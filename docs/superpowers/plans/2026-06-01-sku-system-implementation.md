# SKU 规格组合系统 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将平铺的 `product_spec` 规格系统升级为标准的 SKU 组合模型，支持多维度规格、每个组合独立价格/库存/图片、非法组合禁用。

**Architecture:** 三张新表（`product_spec_group`、`product_spec_value`、`product_sku`）替代旧 `product_spec`。新增后端服务层和 API。管理端新增 SKU 网格编辑器。PDP 通过 `spec_value_ids` 匹配 SKU 显示精确价格/库存。

**Tech Stack:** Spring Boot 3.2 / MyBatis-Plus / MySQL / Vue 3 / Element Plus

---

## 文件清单

### 后端 - 新建
- `backend/product-service/src/main/java/com/mall/product/entity/SpecGroup.java`
- `backend/product-service/src/main/java/com/mall/product/entity/SpecValue.java`
- `backend/product-service/src/main/java/com/mall/product/entity/ProductSku.java`
- `backend/product-service/src/main/java/com/mall/product/mapper/SpecGroupMapper.java`
- `backend/product-service/src/main/java/com/mall/product/mapper/SpecValueMapper.java`
- `backend/product-service/src/main/java/com/mall/product/mapper/ProductSkuMapper.java`
- `backend/product-service/src/main/java/com/mall/product/service/SkuService.java`
- `backend/product-service/src/main/java/com/mall/product/controller/SkuController.java`

### 后端 - 修改
- `backend/product-service/.../entity/Product.java` - 替换 specList 为 specGroups + skuList
- `backend/product-service/.../service/ProductService.java` - 更新 detail()、add()、update()
- `backend/product-service/.../controller/ProductController.java` - 更新 detail 接口
- `backend/order-service/.../entity/Cart.java` - 新增 skuId 字段
- `backend/order-service/.../entity/OrderItem.java` - 新增 skuId 字段
- `backend/order-service/.../controller/CartController.java` - AddRequest 新增 skuId
- `backend/order-service/.../service/CartService.java` - 处理 skuId
- `backend/order-service/.../dto/CartVO.java` - 新增 skuId

### 前端 - 客户
- `frontend/src/api/product.ts` - 新增 specGroups/skuList 类型
- `frontend/src/views/ProductDetailView.vue` - SKU 匹配逻辑重写
- `frontend/src/api/cart.ts` - AddCartRequest 新增 skuId

### 前端 - 管理
- `admin-frontend/src/api/product.ts` - 新增 spec-groups API
- `admin-frontend/src/views/ProductListView.vue` - 规格编辑器区块重写

---

## Task 1: 数据库迁移

**Files:**
- Create: `backend/product-service/src/main/resources/sql/2026-06-01-sku-migration.sql`

```sql
-- 1. 建新表
CREATE TABLE IF NOT EXISTS product_spec_group (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id  BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,
    sort        INT DEFAULT 0,
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_spec_value (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id    BIGINT NOT NULL,
    value       VARCHAR(100) NOT NULL,
    sort        INT DEFAULT 0,
    KEY idx_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_sku (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id      BIGINT NOT NULL,
    spec_value_ids  JSON NOT NULL,
    price           DECIMAL(10,2) DEFAULT NULL,
    stock           INT DEFAULT 0,
    image           VARCHAR(500) DEFAULT NULL,
    enabled         TINYINT(1) DEFAULT 1,
    sort            INT DEFAULT 0,
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. 迁移旧 product_spec 数据
-- 按 product_id + spec_name 分组创建 group
INSERT INTO product_spec_group (product_id, name, sort)
SELECT DISTINCT s.product_id, s.spec_name, 0
FROM product_spec s;

-- 每个 group 下创建 value
INSERT INTO product_spec_value (group_id, value, sort)
SELECT g.id, s.spec_value, s.sort
FROM product_spec s
JOIN product_spec_group g ON g.product_id = s.product_id AND g.name = s.spec_name;

-- 生成 SKU（笛卡尔积每个 product 下的所有 group×value）
-- 单维度：每个 value 一个 SKU，取该 spec 的 extra_price 和 stock
-- 多维度：组合生成，price=null，stock=product.stock
INSERT IGNORE INTO product_sku (product_id, spec_value_ids, price, stock, enabled)
SELECT
    p.id,
    JSON_ARRAY(v.id),
    s.extra_price,
    s.stock,
    1
FROM product_spec s
JOIN product p ON p.id = s.product_id
JOIN product_spec_value v ON v.value = s.spec_value
JOIN product_spec_group g ON g.id = v.group_id AND g.product_id = s.product_id AND g.name = s.spec_name
WHERE (SELECT COUNT(DISTINCT spec_name) FROM product_spec WHERE product_id = p.id) = 1;

-- 多维度商品：不迁移特定 stock/price，使用 product 基准值
INSERT IGNORE INTO product_sku (product_id, spec_value_ids, stock, enabled)
SELECT
    p.id,
    JSON_ARRAY(v1.id, v2.id),
    p.stock,
    1
FROM product p
JOIN product_spec_group g1 ON g1.product_id = p.id
JOIN product_spec_value v1 ON v1.group_id = g1.id
JOIN product_spec_group g2 ON g2.product_id = p.id AND g2.id > g1.id
JOIN product_spec_value v2 ON v2.group_id = g2.id
WHERE (SELECT COUNT(DISTINCT spec_name) FROM product_spec WHERE product_id = p.id) >= 2;

-- 3. order_service 库加 sku_id 列（手动执行）
-- ALTER TABLE mall_order.cart ADD COLUMN sku_id BIGINT DEFAULT NULL;
-- ALTER TABLE mall_order.order_item ADD COLUMN sku_id BIGINT DEFAULT NULL;

-- 4. 迁移完成后删除旧表（手动确认）
-- DROP TABLE IF EXISTS product_spec;
```

- [ ] **Step 1: 在 product 库执行建表和迁移**

```bash
docker exec -i mall-mysql mysql -uroot -proot123 mall_product < backend/product-service/src/main/resources/sql/2026-06-01-sku-migration.sql
```

- [ ] **Step 2: 在 order 库加 sku_id 列**

```bash
docker exec mall-mysql sh -c 'mysql -uroot -proot123 mall_order -e "ALTER TABLE cart ADD COLUMN sku_id BIGINT DEFAULT NULL; ALTER TABLE order_item ADD COLUMN sku_id BIGINT DEFAULT NULL;"'
```

- [ ] **Step 3: 验证迁移**

```bash
docker exec mall-mysql sh -c 'mysql -uroot -proot123 mall_product -e "SELECT COUNT(*) FROM product_sku; SELECT COUNT(*) FROM product_spec_group; SELECT COUNT(*) FROM product_spec_value;"'
```

---

## Task 2: 后端新实体 + Mapper

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/entity/SpecGroup.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/SpecValue.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/ProductSku.java`
- Create: `backend/product-service/src/main/java/com/mall/product/mapper/SpecGroupMapper.java`
- Create: `backend/product-service/src/main/java/com/mall/product/mapper/SpecValueMapper.java`
- Create: `backend/product-service/src/main/java/com/mall/product/mapper/ProductSkuMapper.java`

- [ ] **Step 1: 创建 SpecGroup.java**

```java
package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.List;

@Data
@TableName("product_spec_group")
public class SpecGroup {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String name;
    private Integer sort;
    @TableField(exist = false)
    private List<SpecValue> values;
}
```

- [ ] **Step 2: 创建 SpecValue.java**

```java
package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("product_spec_value")
public class SpecValue {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long groupId;
    private String value;
    private Integer sort;
}
```

- [ ] **Step 3: 创建 ProductSku.java**

```java
package com.mall.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@TableName("product_sku")
public class ProductSku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String specValueIds;  // JSON array string in DB, e.g. "[1,3]"

    @TableField(exist = false)
    private List<Long> specValueIdList;  // Parsed for API responses

    private BigDecimal price;
    private Integer stock;
    private String image;
    private Boolean enabled;
    private Integer sort;

    /** Parse specValueIds JSON string to list. Call after query. */
    public void parseSpecValueIds() {
        if (specValueIds != null && specValueIdList == null) {
            try {
                specValueIdList = new ObjectMapper().readValue(specValueIds,
                        new TypeReference<List<Long>>() {});
            } catch (JsonProcessingException e) {
                specValueIdList = List.of();
            }
        }
    }
}
```

- [ ] **Step 4: 创建三个 Mapper**

```java
// SpecGroupMapper.java
package com.mall.product.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.SpecGroup;
@org.apache.ibatis.annotations.Mapper
public interface SpecGroupMapper extends BaseMapper<SpecGroup> {}

// SpecValueMapper.java
package com.mall.product.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.SpecValue;
@org.apache.ibatis.annotations.Mapper
public interface SpecValueMapper extends BaseMapper<SpecValue> {}

// ProductSkuMapper.java
package com.mall.product.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.product.entity.ProductSku;
@org.apache.ibatis.annotations.Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {}
```

---

## Task 3: 后端 Service + Controller

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/service/SkuService.java`
- Create: `backend/product-service/src/main/java/com/mall/product/controller/SkuController.java`

- [ ] **Step 1: 创建 SkuService.java**

```java
package com.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.*;
import com.mall.product.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuService {

    private final SpecGroupMapper specGroupMapper;
    private final SpecValueMapper specValueMapper;
    private final ProductSkuMapper productSkuMapper;

    /** 获取商品完整规格结构（分组 + SKU） */
    public List<SpecGroup> getGroups(Long productId) {
        List<SpecGroup> groups = specGroupMapper.selectList(
                new LambdaQueryWrapper<SpecGroup>()
                        .eq(SpecGroup::getProductId, productId)
                        .orderByAsc(SpecGroup::getSort));
        for (SpecGroup g : groups) {
            List<SpecValue> values = specValueMapper.selectList(
                    new LambdaQueryWrapper<SpecValue>()
                            .eq(SpecValue::getGroupId, g.getId())
                            .orderByAsc(SpecValue::getSort));
            g.setValues(values);
        }
        return groups;
    }

    public List<ProductSku> getSkus(Long productId) {
        List<ProductSku> skus = productSkuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .orderByAsc(ProductSku::getSort));
        skus.forEach(ProductSku::parseSpecValueIds);
        return skus;
    }

    /** 全量保存规格定义（先删旧数据，再批量插入） */
    @Transactional
    public void saveGroupsAndSkus(Long productId,
                                   List<Map<String, Object>> groupsInput,
                                   List<Map<String, Object>> skusInput) {
        // 1. 删除旧数据
        productSkuMapper.delete(new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId));
        List<SpecGroup> oldGroups = specGroupMapper.selectList(
                new LambdaQueryWrapper<SpecGroup>()
                        .eq(SpecGroup::getProductId, productId));
        for (SpecGroup g : oldGroups) {
            specValueMapper.delete(new LambdaQueryWrapper<SpecValue>()
                    .eq(SpecValue::getGroupId, g.getId()));
        }
        specGroupMapper.delete(new LambdaQueryWrapper<SpecGroup>()
                .eq(SpecGroup::getProductId, productId));

        // 2. 插入新 group + value
        Map<String, Long> valueIdMap = new HashMap<>(); // "groupName:value" -> new id
        for (int gi = 0; gi < groupsInput.size(); gi++) {
            Map<String, Object> gInput = groupsInput.get(gi);
            SpecGroup group = new SpecGroup();
            group.setProductId(productId);
            group.setName((String) gInput.get("name"));
            group.setSort(gi);
            specGroupMapper.insert(group);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> values = (List<Map<String, Object>>) gInput.get("values");
            for (int vi = 0; vi < values.size(); vi++) {
                SpecValue sv = new SpecValue();
                sv.setGroupId(group.getId());
                sv.setValue((String) values.get(vi).get("value"));
                sv.setSort(vi);
                specValueMapper.insert(sv);
                valueIdMap.put(group.getName() + ":" + sv.getValue(), sv.getId());
            }
        }

        // 3. 插入 SKU
        for (int si = 0; si < skusInput.size(); si++) {
            Map<String, Object> skuInput = skusInput.get(si);
            @SuppressWarnings("unchecked")
            List<String> specValueRefs = (List<String>) skuInput.get("specValueRefs");
            List<Long> ids = new ArrayList<>();
            for (String ref : specValueRefs) {
                String[] parts = ref.split(":", 2);
                Long id = valueIdMap.get(ref);
                if (id != null) ids.add(id);
            }
            if (ids.isEmpty()) continue;

            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSpecValueIds(ids.stream().map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]")));
            sku.setPrice(skuInput.get("price") != null
                    ? new java.math.BigDecimal(skuInput.get("price").toString()) : null);
            sku.setStock(skuInput.get("stock") != null
                    ? ((Number) skuInput.get("stock")).intValue() : 0);
            sku.setImage((String) skuInput.get("image"));
            sku.setEnabled(skuInput.get("enabled") != null
                    ? (Boolean) skuInput.get("enabled") : true);
            sku.setSort(si);
            productSkuMapper.insert(sku);
        }
    }
}
```

- [ ] **Step 2: 创建 SkuController.java**

```java
package com.mall.product.controller;

import com.mall.product.common.Result;
import com.mall.product.entity.*;
import com.mall.product.service.SkuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class SkuController {

    private final SkuService skuService;

    @GetMapping("/sku/list/{productId}")
    public Result<List<ProductSku>> skuList(@PathVariable Long productId) {
        return Result.success(skuService.getSkus(productId));
    }

    @PutMapping("/admin/spec-groups")
    public Result<Void> saveSpecGroups(@RequestBody Map<String, Object> body) {
        Long productId = Long.valueOf(body.get("productId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) body.get("groups");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> skus = (List<Map<String, Object>>) body.get("skus");
        skuService.saveGroupsAndSkus(productId, groups, skus);
        return Result.success(null);
    }
}
```

---

## Task 4: 修改 Product 实体和 ProductService

**Files:**
- Modify: `backend/product-service/.../entity/Product.java`
- Modify: `backend/product-service/.../service/ProductService.java`
- Modify: `backend/product-service/.../controller/ProductController.java`

- [ ] **Step 1: 修改 Product.java** — 替换 specList 为 specGroups + skuList

```java
// 替换:
// @TableField(exist = false)
// private List<ProductSpec> specList;

// 改为:
@TableField(exist = false)
private List<SpecGroup> specGroups;

@TableField(exist = false)
private List<ProductSku> skuList;
```

- [ ] **Step 2: 修改 ProductService.detail()** — 加载新结构

找到 `detail()` 方法，替换 specs 加载部分：

```java
// 替换:
// List<ProductSpec> specs = productSpecMapper.selectList(...)
// product.setSpecList(specs);

// 改为:
// Load spec groups + values
List<SpecGroup> specGroups = new ArrayList<>();
List<com.mall.product.entity.SpecGroup> rawGroups = specGroupMapper.selectList(
        new LambdaQueryWrapper<com.mall.product.entity.SpecGroup>()
                .eq(com.mall.product.entity.SpecGroup::getProductId, id)
                .orderByAsc(com.mall.product.entity.SpecGroup::getSort));
for (com.mall.product.entity.SpecGroup g : rawGroups) {
    SpecGroup sg = new SpecGroup();
    sg.setId(g.getId());
    sg.setProductId(g.getProductId());
    sg.setName(g.getName());
    sg.setSort(g.getSort());
    List<SpecValue> values = specValueMapper.selectList(
            new LambdaQueryWrapper<SpecValue>()
                    .eq(SpecValue::getGroupId, g.getId())
                    .orderByAsc(SpecValue::getSort));
    sg.setValues(values);
    specGroups.add(sg);
}
product.setSpecGroups(specGroups);

// Load SKUs
List<ProductSku> skus = productSkuMapper.selectList(
        new LambdaQueryWrapper<ProductSku>()
                .eq(ProductSku::getProductId, id)
                .orderByAsc(ProductSku::getSort));
skus.forEach(ProductSku::parseSpecValueIds);
product.setSkuList(skus);
```

同时需要在 ProductService 里注入 `SpecGroupMapper`、`SpecValueMapper`、`ProductSkuMapper`：

```java
// 在字段区追加:
private final SpecGroupMapper specGroupMapper;
private final SpecValueMapper specValueMapper;
private final ProductSkuMapper productSkuMapper;
```

- [ ] **Step 3: 修改 ProductService.add()** — 添加新结构处理

`add()` 方法中替换旧的 specList 插入逻辑：

```java
// 替换:
// if (product.getSpecList() != null) {
//     for (ProductSpec spec : product.getSpecList()) {
//         spec.setProductId(product.getId());
//         productSpecMapper.insert(spec);
//     }
// }

// 改为:
if (product.getSpecGroups() != null && !product.getSpecGroups().isEmpty()) {
    for (SpecGroup g : product.getSpecGroups()) {
        g.setProductId(product.getId());
        specGroupMapper.insert(g);
        if (g.getValues() != null) {
            for (SpecValue v : g.getValues()) {
                v.setGroupId(g.getId());
                specValueMapper.insert(v);
            }
        }
    }
}
if (product.getSkuList() != null && !product.getSkuList().isEmpty()) {
    for (ProductSku s : product.getSkuList()) {
        s.setProductId(product.getId());
        productSkuMapper.insert(s);
    }
}
```

- [ ] **Step 4: 修改 ProductService.update()** — 替换 specList 更新逻辑

找到 `update()` 方法，移除旧 specList 处理代码：

```java
// 删除这段：
// if (product.getSpecList() != null) {
//     productSpecMapper.delete(new LambdaQueryWrapper<ProductSpec>()
//             .eq(ProductSpec::getProductId, product.getId()));
//     for (ProductSpec spec : product.getSpecList()) {
//         spec.setId(null);
//         spec.setProductId(product.getId());
//         productSpecMapper.insert(spec);
//     }
// }
// SKU 的更新通过 /api/product/admin/spec-groups 接口单独处理
```

- [ ] **Step 5: 编译验证**

```bash
cd "d:\project\webwork\backend" && mvn compile -pl product-service -am -q 2>&1 | tail -5
```

---

## Task 5: 修改 Order 服务 — 添加 skuId

**Files:**
- Modify: `backend/order-service/src/main/java/com/mall/order/entity/Cart.java`
- Modify: `backend/order-service/src/main/java/com/mall/order/entity/OrderItem.java`
- Modify: `backend/order-service/.../controller/CartController.java`
- Modify: `backend/order-service/.../service/CartService.java`
- Modify: `backend/order-service/.../dto/CartVO.java`

- [ ] **Step 1: Cart.java 追加 skuId**

```java
// 在 quantity 字段之后追加:
private Long skuId;
```

- [ ] **Step 2: OrderItem.java 追加 skuId**

```java
// 在 productImage 字段之后追加:
private Long skuId;
```

- [ ] **Step 3: CartVO.java 追加 skuId**

```java
// 在 specInfo 字段之后追加:
private Long skuId;
```

- [ ] **Step 4: CartController.java — AddRequest 追加 skuId**

```java
// 在 AddRequest 类中追加:
private Long skuId;
```

- [ ] **Step 5: CartService.add() — 接收 skuId 参数**

找到 `add(Long userId, Long productId, Integer quantity, String specInfo)` 方法签名，改为：

```java
public void add(Long userId, Long productId, Integer quantity, String specInfo, Long skuId) {
    LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, userId)
            .eq(Cart::getProductId, productId);
    if (specInfo != null) {
        wrapper.eq(Cart::getSpecInfo, specInfo);
    } else {
        wrapper.isNull(Cart::getSpecInfo);
    }
    Cart existing = cartMapper.selectOne(wrapper);
    if (existing != null) {
        existing.setQuantity(existing.getQuantity() + quantity);
        existing.setSkuId(skuId != null ? skuId : existing.getSkuId());
        cartMapper.updateById(existing);
    } else {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setQuantity(quantity);
        cart.setSpecInfo(specInfo);
        cart.setSkuId(skuId);
        cart.setChecked(1);
        cart.setCreatedTime(LocalDateTime.now());
        cart.setUpdatedTime(LocalDateTime.now());
        cartMapper.insert(cart);
    }
}
```

同时更新 CartController.add() 方法的调用：

```java
// 改为:
cartService.add(userId, request.getProductId(), request.getQuantity(),
        request.getSpecInfo(), request.getSkuId());
```

以及 CartService 中其他调用 `add` 的地方（如果有）。

- [ ] **Step 6: 编译验证**

```bash
cd "d:\project\webwork\backend" && mvn compile -pl order-service -am -q 2>&1 | tail -5
```

---

## Task 6: 客户前端 — PDP SKU 匹配

**Files:**
- Modify: `frontend/src/api/product.ts` — 新增类型
- Modify: `frontend/src/views/ProductDetailView.vue` — 重写 specs 逻辑
- Modify: `frontend/src/api/cart.ts` — 新增 skuId

- [ ] **Step 1: 更新 product.ts 类型**

在 `Product` interface 中替换 specList 为 specGroups + skuList：

```ts
// 删掉: specList: ProductSpec[] | null
// 改为:
specGroups?: SpecGroup[]
skuList?: ProductSku[]
```

在文件末尾新增类型：

```ts
export interface SpecGroup {
  id: number
  productId: number
  name: string
  sort: number
  values: SpecValue[]
}

export interface SpecValue {
  id: number
  groupId: number
  value: string
  sort: number
}

export interface ProductSku {
  id: number
  productId: number
  specValueIds: string | null
  specValueIdList: number[]
  price: number | null
  stock: number
  image: string | null
  enabled: boolean
  sort: number
}
```

- [ ] **Step 2: 更新 cart.ts 类型**

```ts
// 修改 AddCartRequest:
export interface AddCartRequest {
  productId: number
  quantity: number
  specInfo?: string
  skuId?: number
}
```

- [ ] **Step 3: 重写 ProductDetailView.vue 的 specs 部分**

替换以下几个部分：

**a) 替换模板中的 spec 渲染（约 L283-L298）：**

```vue
<!-- Specs -->
<div v-if="product?.specGroups?.length" class="pdp__specs">
  <div v-for="group in product.specGroups" :key="group.name" class="pdp__spec-row">
    <span class="pdp__spec-label">{{ group.name }}</span>
    <div class="pdp__spec-options">
      <button
        v-for="val in group.values"
        :key="val.id"
        class="pdp__spec-option"
        :class="{
          'pdp__spec-option--active': selectedSpecs[group.name] === val.value,
          'pdp__spec-option--disabled': !isSkuAvailable(group.name, val.value)
        }"
        @click="selectSpec(group.name, val.value)"
      >
        {{ val.value }}
        <span v-if="getSpecExtraPrice(group.name, val.value) > 0" class="pdp__spec-extra">
          +¥{{ getSpecExtraPrice(group.name, val.value) }}
        </span>
      </button>
    </div>
  </div>
</div>
```

**b) 替换 script 中的变量和函数：**

```ts
// 修改 specGroups 相关响应式变量
const selectedSpecs = ref<Record<string, string>>({})

// 移除旧的 specGroups 和 buildSpecGroups 函数，替换为：
const matchedSku = computed(() => {
  if (!product.value?.skuList?.length || !product.value?.specGroups?.length) return null
  const allSelected = product.value.specGroups.every(g => selectedSpecs.value[g.name])
  if (!allSelected) return null
  const selectedIds = product.value.specGroups.map(g => {
    const val = g.values.find(v => v.value === selectedSpecs.value[g.name])
    return val?.id
  })
  return product.value.skuList.find(sku =>
    sku.enabled &&
    selectedIds.every(id => sku.specValueIdList?.includes(id))
  ) || null
})

const displayPrice = computed(() => {
  if (!product.value) return 0
  if (matchedSku.value?.price != null) return matchedSku.value.price
  return product.value.price
})

const displayStock = computed(() => {
  if (!product.value) return 0
  if (matchedSku.value) return matchedSku.value.stock
  if (!product.value.specGroups?.length) return product.value.stock
  return 0
})

const allSpecsSelected = computed(() => {
  if (!product.value?.specGroups?.length) return true
  return product.value.specGroups.every(g => selectedSpecs.value[g.name])
})

function selectSpec(groupName: string, value: string) {
  selectedSpecs.value[groupName] = value
  quantity.value = 1
  // 自动更新图片
  if (matchedSku.value?.image) {
    activeImageIndex.value = thumbList.value.findIndex(t => t === matchedSku.value!.image)
  }
}

function isSkuAvailable(groupName: string, value: string): boolean {
  if (!product.value?.skuList?.length) return true
  // 检查选了这个值后，是否存在至少一个 enabled 的 SKU
  const testSelected = { ...selectedSpecs.value, [groupName]: value }
  const allSelected = product.value.specGroups!.every(g => testSelected[g.name])
  if (!allSelected) return true // 还没选完，所有选项可点
  const group = product.value.specGroups!.find(g => g.name === groupName)
  if (!group) return true
  const valObj = group.values.find(v => v.value === value)
  if (!valObj) return true
  const selectedIds = product.value.specGroups!.map(g => {
    const v = g.values.find(vv => vv.value === testSelected[g.name])
    return v?.id
  })
  return product.value.skuList.some(sku =>
    sku.enabled && selectedIds.every(id => sku.specValueIdList?.includesid!))
  )
}

function getSpecExtraPrice(groupName: string, value: string): number {
  if (!product.value?.skuList?.length) return 0
  // 当一个规格维度只有一个值被选中时，检查该组合的加价
  if (!product.value.specGroups?.some(g => selectedSpecs.value[g.name])) return 0
  const testSelected = { ...selectedSpecs.value }
  testSelected[groupName] = value
  const group = product.value.specGroups!.find(g => g.name === groupName)
  if (!group) return 0
  const valObj = group.values.find(v => v.value === value)
  if (!valObj) return 0
  const selectedIds = product.value.specGroups!.map(g => {
    const v = g.values.find(vv => vv.value === testSelected[g.name])
    return v?.id
  })
  // 匹配到 SKU 则显示其加价
  const sku = product.value.skuList.find(s =>
    s.enabled && selectedIds.every(id => s.specValueIdList?.includesid!))
  )
  if (sku?.price != null && product.value.price != null) {
    return sku.price - product.value.price
  }
  return 0
}
```

**c) 替换 `handleAddToCart` 中的调用：**

```ts
async function handleAddToCart() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await addToCart({
      productId: product.value.id,
      quantity: quantity.value,
      specInfo: getSpecInfo(),
      skuId: matchedSku.value?.id
    })
    showToast('已加入购物车', 'success')
  } catch { /* handled */ }
}
```

- [ ] **Step 4: 移除旧的 `buildSpecGroups` 和 `specGroups` ref**

删除以下代码：
```ts
const specGroups = ref<{ name: string; values: ProductSpec[] }[]>([])
// 以及 onMounted 中的 buildSpecGroups() 调用
// 以及 buildSpecGroups() 函数定义
```

在 `onMounted` 中，确认规格初始值在 `product.value` 加载后设置：

```ts
// 在 onMounted 中替换 buildSpecGroups() 为:
if (product.value?.specGroups) {
  for (const group of product.value.specGroups) {
    selectedSpecs.value[group.name] = group.values[0]?.value
  }
}
```

---

## Task 7: 管理前端 — SKU 网格编辑

**Files:**
- Modify: `admin-frontend/src/api/product.ts` — 新增 saveSpecGroups API
- Modify: `admin-frontend/src/views/ProductListView.vue` — 重写规格区块

- [ ] **Step 1: product.ts 新增 API**

```ts
export function saveSpecGroups(productId: number, groups: any[], skus: any[]) {
  return request.put<void>('/product/admin/spec-groups', { productId, groups, skus })
}
```

- [ ] **Step 2: 重写 ProductListView.vue 的规格参数区块**

将原"规格参数" section（L316-L351）替换为新的维度编辑器 + SKU 网格组件。

在 script 中追加：

```ts
// ─── Spec group editor ───
const specGroupsInput = ref<{ name: string; values: string[] }[]>([])
const skuGrid = ref<{ specValueRefs: string[]; price: number | null; stock: number; image: string; enabled: boolean }[]>([])
const specPresets = ['颜色', '尺寸', '材质', '规格', '口味', '容量']

function addDimension() {
  specGroupsInput.value.push({ name: '', values: [] })
}

function removeDimension(idx: number) {
  specGroupsInput.value.splice(idx, 1)
  regenerateSkuGrid()
}

function addValue(groupIdx: number, value: string) {
  if (!value.trim()) return
  if (specGroupsInput.value[groupIdx].values.includes(value.trim())) return
  specGroupsInput.value[groupIdx].values.push(value.trim())
  regenerateSkuGrid()
}

function removeValue(groupIdx: number, valIdx: number) {
  specGroupsInput.value[groupIdx].values.splice(valIdx, 1)
  regenerateSkuGrid()
}

function regenerateSkuGrid() {
  const dims = specGroupsInput.value.filter(d => d.name && d.values.length > 0)
  if (dims.length === 0) { skuGrid.value = []; return }
  
  const combinations = cartesian(dims.map(d => d.values.map(v => `${d.name}:${v}`)))
  skuGrid.value = combinations.map(refs => {
    const existing = skuGrid.value.find(s =>
      JSON.stringify(s.specValueRefs) === JSON.stringify(refs)
    )
    return existing || { specValueRefs: refs, price: null, stock: 0, image: '', enabled: true }
  })
}

function cartesian<T>(arrays: T[][]): T[][] {
  if (arrays.length === 0) return [[]]
  const [first, ...rest] = arrays
  const restProduct = cartesian(rest)
  return first.flatMap(f => restProduct.map(r => [f, ...r]))
}

function setAllPrice(price: number | null) {
  skuGrid.value.forEach(s => { if (s.enabled) s.price = price })
}

function setAllStock(stock: number) {
  skuGrid.value.forEach(s => { if (s.enabled) s.stock = stock })
}

function enableAll() {
  skuGrid.value.forEach(s => { s.enabled = true })
}

function loadSpecData(productId: number) {
  // 从 product detail 加载已有规格数据
  // 这个方法在 openEditDialog 中被调用
}
```

在 `openEditDialog` 中追加规格加载逻辑：

```ts
async function openEditDialog(item: Product) {
  isEdit.value = true
  dialogMounted.value++
  dialogVisible.value = true
  try {
    const detail = await getProductDetail(item.id)
    form.value = { ...detail, imageList: detail.imageList || [] }
    // 加载规格数据
    if (detail.specGroups?.length) {
      specGroupsInput.value = detail.specGroups.map(g => ({
        name: g.name,
        values: g.values.map(v => v.value)
      }))
      regenerateSkuGrid()
      // 回填已有 SKU
      if (detail.skuList?.length) {
        for (const sku of detail.skuList) {
          const refs: string[] = []
          for (const g of detail.specGroups) {
            const val = g.values.find(v => sku.specValueIdList?.includesv.id))
            if (val) refs.push(`${g.name}:${val.value}`)
          }
          const existing = skuGrid.value.find(s =>
            JSON.stringify(s.specValueRefs) === JSON.stringify(refs)
          )
          if (existing) {
            existing.price = sku.price
            existing.stock = sku.stock
            existing.image = sku.image || ''
            existing.enabled = sku.enabled
          }
        }
      }
    } else {
      specGroupsInput.value = []
      skuGrid.value = []
    }
  } catch {
    form.value = { ...item, imageList: item.imageList || [] }
  }
}
```

在 `openAddDialog` 中重置：

```ts
function openAddDialog() {
  isEdit.value = false
  form.value = { name: '', categoryId: 0, price: 0, /* ...其他字段 */ }
  specGroupsInput.value = []
  skuGrid.value = []
  // ...
}
```

保存时追加 SKU 数据提交，在 `submitForm` 中商品基本保存成功后追加：

```ts
async function submitForm() {
  if (!form.value.name) { ElMessage.warning('请输入商品名称'); return }
  try {
    let productId: number
    if (isEdit.value) {
      await updateProduct(form.value)
      productId = form.value.id!
    } else {
      await addProduct(form.value)
      productId = form.value.id!
    }
    // 保存规格数据
    if (specGroupsInput.value.length > 0) {
      const groups = specGroupsInput.value.map(g => ({
        name: g.name,
        values: g.values.map(v => ({ value: v }))
      }))
      const skus = skuGrid.value.map(s => ({
        specValueRefs: s.specValueRefs,
        price: s.price,
        stock: s.stock,
        image: s.image || null,
        enabled: s.enabled
      }))
      await saveSpecGroups(productId!, groups, skus)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
    dialogVisible.value = false
    await loadProducts()
  } catch {}
}
```

模板替换规格区块（原 L316-L351）：

```vue
<!-- Section: Specs -->
<div class="form-section">
  <div class="form-section-head">
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/></svg>
    <span>规格参数</span>
  </div>
  <div class="form-section-body">
    <!-- Step 1: Dimensions -->
    <div v-for="(dim, di) in specGroupsInput" :key="di" class="spec-dim-row">
      <div class="spec-dim-header">
        <el-input v-model="dim.name" placeholder="维度名称" size="small" class="spec-dim-name" @input="regenerateSkuGrid">
          <template #prefix>
            <el-select v-model="dim.name" size="small" style="width:0;overflow:hidden" @change="regenerateSkuGrid">
              <el-option v-for="pre in specPresets" :key="pre" :label="pre" :value="pre" />
            </el-select>
          </template>
        </el-input>
        <el-tag
          v-for="(val, vi) in dim.values" :key="vi"
          closable size="small"
          @close="removeValue(di, vi)"
        >{{ val }}</el-tag>
        <el-input
          size="small" class="spec-dim-add-input"
          placeholder="输入值后回车" :ref="(el: any) => {}"
          @keyup.enter="(e: any) => { addValue(di, e.target.value); e.target.value = '' }"
        />
        <button class="spec-dim-remove" @click="removeDimension(di)" title="删除维度">×</button>
      </div>
    </div>
    <el-button size="small" @click="addDimension">+ 添加维度</el-button>

    <!-- Step 2: SKU Grid -->
    <div v-if="skuGrid.length > 0" class="sku-grid-section">
      <div class="sku-grid-toolbar">
        <span class="sku-grid-count">共 {{ skuGrid.filter(s => s.enabled).length }} 个组合</span>
        <el-button size="small" @click="enableAll">全部启用</el-button>
        <span style="margin-left:8px;font-size:12px;color:#909399">统一加价:</span>
        <el-input-number v-model="batchPrice" :min="0" :precision="2" size="small" style="width:120px" />
        <el-button size="small" @click="setAllPrice(batchPrice)">应用</el-button>
        <span style="margin-left:8px;font-size:12px;color:#909399">统一库存:</span>
        <el-input-number v-model="batchStock" :min="0" size="small" style="width:100px" />
        <el-button size="small" @click="setAllStock(batchStock)">应用</el-button>
      </div>
      <div class="sku-grid">
        <div class="sku-grid-header">
          <span class="sku-grid-cell sku-grid-cell--header">组合</span>
          <span class="sku-grid-cell sku-grid-cell--header">加价(¥)</span>
          <span class="sku-grid-cell sku-grid-cell--header">库存</span>
          <span class="sku-grid-cell sku-grid-cell--header">启用</span>
        </div>
        <div v-for="(sku, si) in skuGrid" :key="si" class="sku-grid-row" :class="{ 'sku-grid-row--disabled': !sku.enabled }">
          <span class="sku-grid-cell sku-grid-cell--label">{{ sku.specValueRefs.join(' / ') }}</span>
          <span class="sku-grid-cell">
            <el-input-number v-model="sku.price" :min="0" :precision="2" size="small" style="width:100px" placeholder="基准价" />
          </span>
          <span class="sku-grid-cell">
            <el-input-number v-model="sku.stock" :min="0" size="small" style="width:80px" />
          </span>
          <span class="sku-grid-cell">
            <el-switch v-model="sku.enabled" size="small" />
          </span>
        </div>
      </div>
    </div>
  </div>
</div>
```

在 script 中追加 batchPrice/batchStock 变量：

```ts
const batchPrice = ref<number | null>(null)
const batchStock = ref<number>(0)
```

在 ProductListView 的 `<style scoped>` 中追加 SKU 网格样式：

```css
/* ── Spec dimension editor ── */
.spec-dim-row { margin-bottom: 10px; }
.spec-dim-header { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.spec-dim-name { width: 100px; }
.spec-dim-add-input { width: 130px; }
.spec-dim-remove { border: none; background: none; color: #ef4444; cursor: pointer; font-size: 16px; padding: 0 4px; }

/* ── SKU Grid ── */
.sku-grid-section { margin-top: 12px; border: 1px solid #eef0f2; border-radius: 8px; overflow: hidden; }
.sku-grid-toolbar { display: flex; align-items: center; gap: 6px; padding: 8px 12px; background: #f8f9fb; border-bottom: 1px solid #eef0f2; flex-wrap: wrap; }
.sku-grid-count { font-size: 12px; color: #64748b; margin-right: 8px; }
.sku-grid { display: flex; flex-direction: column; }
.sku-grid-header, .sku-grid-row { display: flex; align-items: center; padding: 6px 12px; }
.sku-grid-header { background: #f1f3f5; font-size: 12px; font-weight: 600; color: #64748b; border-bottom: 1px solid #eef0f2; }
.sku-grid-row { border-bottom: 1px solid #f8f9fb; transition: background 0.15s; }
.sku-grid-row:hover { background: #fafbfc; }
.sku-grid-row--disabled { opacity: 0.5; background: #f8f9fb; }
.sku-grid-cell { flex: 1; padding: 2px 4px; font-size: 13px; }
.sku-grid-cell--header { flex: 1; }
.sku-grid-cell--label { flex: 2; font-weight: 500; color: #334155; }
```

---

## Task 8: 重新构建和部署

- [ ] **Step 1: 构建 product-service**

```bash
cd "d:\project\webwork\backend" && mvn package -pl product-service -am -DskipTests -q
```

- [ ] **Step 2: 构建 order-service**

```bash
cd "d:\project\webwork\backend" && mvn package -pl order-service -am -DskipTests -q
```

- [ ] **Step 3: 部署到 Docker**

```bash
cd "d:\project\webwork\backend" && \
MSYS2_ARG_CONV_EXCL="*" docker cp product-service/target/product-service-1.0.0-SNAPSHOT.jar mall-product-service:/app/app.jar && \
docker restart mall-product-service && \
MSYS2_ARG_CONV_EXCL="*" docker cp order-service/target/order-service-1.0.0-SNAPSHOT.jar mall-order-service:/app/app.jar && \
docker restart mall-order-service
```

- [ ] **Step 4: 验证**

```bash
# 验证 detail 接口返回新字段
sleep 10 && curl -s http://localhost:5173/api/product/detail/1 | python -c "import sys,json; d=json.load(sys.stdin)['data']; print('specGroups:', len(d.get('specGroups',[])), 'skuList:', len(d.get('skuList',[])))"
```
