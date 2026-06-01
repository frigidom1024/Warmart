# SKU 规格组合系统设计

## 背景

当前 `product_spec` 表以平铺方式存储规格值，每个规格值独立持有 `extra_price` 和 `stock`。PDP 对多维度组合（如"红色+S"）的库存只能取各维度最小值，无法精确反映每个组合的真实库存。购物车/订单以纯文本 `spec_info` 记录规格选择，缺乏结构化关联。

## 目标

引入标准 SKU 模型，支持：

- 多维度规格组合（颜色、尺寸、材质等），无维度数量限制
- 每个组合 (SKU) 独立管理价格、库存、图片
- 非法组合可禁用（特定组合不存在）
- 购物车/订单通过 SKU ID 关联精确规格

## 数据模型

### 新建表

```sql
-- 规格维度（如"颜色""尺寸"）
product_spec_group (
    id          BIGINT PK AUTO_INCREMENT,
    product_id  BIGINT NOT NULL,
    name        VARCHAR(100) NOT NULL,  -- "颜色"
    sort        INT DEFAULT 0,
    INDEX idx_product (product_id)
)

-- 规格值（如"红色""蓝色"）
product_spec_value (
    id          BIGINT PK AUTO_INCREMENT,
    group_id    BIGINT NOT NULL,       -- FK → product_spec_group.id
    value       VARCHAR(100) NOT NULL, -- "红色"
    sort        INT DEFAULT 0,
    INDEX idx_group (group_id)
)

-- SKU（组合）
product_sku (
    id              BIGINT PK AUTO_INCREMENT,
    product_id      BIGINT NOT NULL,
    spec_value_ids  JSON NOT NULL,          -- [1, 3] 对应两个维度的 value_id
    price           DECIMAL(10,2) DEFAULT NULL,  -- NULL = 沿用商品基准价
    stock           INT DEFAULT 0,
    image           VARCHAR(500) DEFAULT NULL,
    enabled         TINYINT(1) DEFAULT 1,  -- 0=禁用（非法组合）
    sort            INT DEFAULT 0,
    INDEX idx_product (product_id)
)
```

### 现有表追加字段

```sql
ALTER TABLE cart_item ADD COLUMN sku_id BIGINT DEFAULT NULL;
ALTER TABLE order_item ADD COLUMN sku_id BIGINT DEFAULT NULL;
```

### 数据关系

```
Product 1 ──→ N product_spec_group
product_spec_group 1 ──→ N product_spec_value
Product 1 ──→ N product_sku
product_sku.spec_value_ids → [product_spec_value.id, ...]
cart_item.sku_id → product_sku.id
order_item.sku_id → product_sku.id
```

### 存量迁移

旧 `product_spec` 表迁移至新结构：

1. 按 `product_id + spec_name` 分组 → 创建 `product_spec_group`
2. 按 `spec_value` 去重 → 创建 `product_spec_value`（group_id 关联）
3. 同商品下所有 group 做笛卡尔积 → 生成 `product_sku`
   - 单维度：每个 value 一个 SKU，`price = extra_price`，`stock` 照搬
   - 多维度：每组合一个 SKU，`price = NULL`（基准价），`stock = product.stock`
4. 所有 SKU `enabled = true`
5. 删除旧 `product_spec` 表

## 后端 API

### 新增

| 方法 | 路径 | 说明 |
|------|------|------|
| `PUT` | `/api/product/admin/spec-groups` | 全量保存商品规格定义。接收完整结构（groups + values + skus），后端先删旧数据再批量插入。事务保证原子性。 |
| `GET` | `/api/product/sku/list/{productId}` | 公开接口。返回商品所有 SKU（含 enabled、specValueIds、price、stock、image）。 |

### 调整

| 接口 | 改动 |
|------|------|
| `GET /api/product/detail/{id}` | 响应增加 `specGroups`（分组结构）和 `skuList`（SKU 列表）字段 |
| `POST /api/cart/add` | 请求体新增 `skuId`（可选），指定具体 SKU |
| `POST /api/order/create` | 从 cartItem 读取 `skuId`，写入 orderItem |
| `PUT /api/product/admin/update` | 移除 `specList` 字段处理，交给 `/spec-groups` 接口 |

### 请求/响应格式

**PUT /api/product/admin/spec-groups**

```json
{
  "productId": 1,
  "groups": [
    {
      "name": "颜色",
      "sort": 1,
      "values": [
        {"value": "红色", "sort": 1},
        {"value": "蓝色", "sort": 2}
      ]
    },
    {
      "name": "尺寸",
      "sort": 2,
      "values": [
        {"value": "S", "sort": 1},
        {"value": "M", "sort": 2},
        {"value": "L", "sort": 3}
      ]
    }
  ],
  "skus": [
    {"specValueIds": [1, 3], "price": null, "stock": 20, "image": "red-s.jpg", "enabled": true},
    {"specValueIds": [1, 4], "price": 109.99, "stock": 15, "image": null, "enabled": true},
    {"specValueIds": [1, 5], "price": null, "stock": 0, "image": null, "enabled": false},
    {"specValueIds": [2, 3], "price": 104.99, "stock": 25, "image": "blue-s.jpg", "enabled": true}
  ]
}
```

**GET /api/product/detail/{id} 附加字段**

```json
{
  "...": "...",
  "specGroups": [
    {"name": "颜色", "values": [{"id": 1, "value": "红色", "sort": 1}, {"id": 2, "value": "蓝色", "sort": 2}]},
    {"name": "尺寸", "values": [{"id": 3, "value": "S", "sort": 1}, {"id": 4, "value": "M", "sort": 2}]}
  ],
  "skuList": [
    {"id": 1, "specValueIds": [1, 3], "price": null, "stock": 20, "image": "red-s.jpg", "enabled": true},
    {"id": 2, "specValueIds": [1, 4], "price": 109.99, "stock": 15, "image": null, "enabled": true}
  ]
}
```

## 管理端 UI

### 三步交互

**Step 1: 定义维度**

产品编辑弹窗的"规格参数"区块顶部增加维度编辑器。管理员添加维度名称（支持预置下拉：颜色、尺寸、材质、规格），每个维度内按回车添加值。支持删除维度和单个值。

**Step 2: SKU 网格**

维度定义后自动生成笛卡尔积网格：
- 行标题 = 第一维度的值，列标题 = 第二维度的值
- 三维以上用表格形式展示所有组合
- 每个格子展示：加价/价格、库存、图片缩略图、启用开关
- 禁用（非法）的组合用灰色标注，不出现在 PDP 选择中
- 提供批量操作：统一加价/库存/全部启用

**Step 3: 保存**

提交时全量覆盖，与商品基本信息一起保存。每次保存先删除旧数据再批量插入新数据（事务内）。

## PDP 前端

### SKU 匹配逻辑

用户选择规格时，按 `spec_value_ids` 精确匹配 SKU：

```
选中 红色(1) + S(3) → specValueIds=[1,3] → 匹配 sku
→ displayPrice = sku.price ?? product.price
→ displayStock = sku.stock
→ displayImage = sku.image ?? product.mainImage
```

- 匹配失败或 `enabled=false` → 组合不可用，选择按钮禁选
- 所有维度选完且匹配到有效 SKU → 才允许加入购物车

### 购物车/下单

```json
POST /api/cart/add
{
  "productId": 1,
  "skuId": 1,
  "quantity": 2,
  "specInfo": "颜色: 红色; 尺寸: S"
}
```

`specInfo` 保留作为订单历史快照，`skuId` 作为结构化关联。

## 实施顺序

1. 数据库迁移 → 新建表 + 迁移脚本 + 删除旧表
2. 后端模型 + 接口 → `SpecGroupController`、SKU CRUD、detail 接口改造
3. 管理端 UI → 维度编辑器 + SKU 网格组件
4. PDP 前端 → SKU 匹配 + 价格/库存/图片联动
5. 购物车/订单 → skuId 字段接入
