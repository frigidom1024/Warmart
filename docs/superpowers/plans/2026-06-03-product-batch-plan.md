# 商品批量导入导出 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 管理后台支持通过 Excel 批量导入和导出商品数据

**Architecture:** 在 product-service 中集成 EasyExcel 实现 Excel 读写，导出时查询商品列表写入响应流，导入时解析上传文件并批量写入数据库

**Tech Stack:** Java Spring Boot + EasyExcel + Vue 3 + Element Plus

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `backend/product-service/pom.xml` | 修改 | 添加 EasyExcel 依赖 |
| `backend/product-service/.../entity/ProductExportVO.java` | 新建 | Excel 导出 VO |
| `backend/product-service/.../controller/ProductController.java` | 修改 | 新增导入导出接口 |
| `admin-frontend/src/api/product.ts` | 修改 | 新增导入导出 API |
| `admin-frontend/src/views/ProductListView.vue` | 修改 | 新增导入导出按钮 |

---

### Task 1: 后端 EasyExcel 依赖

**Files:**
- Modify: `backend/product-service/pom.xml`

- [ ] **Step 1: 在 pom.xml 中添加 EasyExcel 依赖**

```xml
<!-- 在 </dependencies> 之前添加 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>4.0.3</version>
</dependency>
```

- [ ] **Step 2: 提交**

```bash
cd d:/project/webwork && git add backend/product-service/pom.xml && git commit -m "chore: 添加 EasyExcel 依赖用于商品导入导出"
```

---

### Task 2: 后端导出 VO 实体

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/entity/ProductExportVO.java`

- [ ] **Step 1: 新建 ProductExportVO.java**

```java
package com.mall.product.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class ProductExportVO {

    @ExcelProperty("商品名称")
    @ColumnWidth(30)
    private String name;

    @ExcelProperty("分类")
    @ColumnWidth(16)
    private String categoryName;

    @ExcelProperty("价格")
    @ColumnWidth(12)
    private Double price;

    @ExcelProperty("原价")
    @ColumnWidth(12)
    private Double originalPrice;

    @ExcelProperty("库存")
    @ColumnWidth(10)
    private Integer stock;

    @ExcelProperty("销量")
    @ColumnWidth(10)
    private Integer sales;

    @ExcelProperty("商品描述")
    @ColumnWidth(40)
    private String description;

    @ExcelProperty("主图URL")
    @ColumnWidth(40)
    private String mainImage;

    @ExcelProperty("标签")
    @ColumnWidth(10)
    private String tag;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String statusText;

    @ExcelProperty("是否推荐")
    @ColumnWidth(12)
    private String recommendText;
}
```

- [ ] **Step 2: 提交**

```bash
cd d:/project/webwork && git add 'backend/product-service/src/main/java/com/mall/product/entity/ProductExportVO.java' && git commit -m "feat: 新增商品导出 VO"
```

---

### Task 3: 后端导入导出接口

**Files:**
- Modify: `backend/product-service/src/main/java/com/mall/product/controller/ProductController.java`

- [ ] **Step 1: 在 ProductController 新增导出接口**

在 `ProductController` 中添加以下 import 和代码：

```java
import com.alibaba.excel.EasyExcel;
import com.mall.product.entity.ProductExportVO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;
```

然后在类中新增方法：

```java
@GetMapping("/admin/export")
public void export(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String keyword,
        HttpServletResponse response) throws IOException {
    // 查询商品
    LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
            .orderByDesc(Product::getCreatedTime);
    if (categoryId != null) wrapper.eq(Product::getCategoryId, categoryId);
    if (status != null) wrapper.eq(Product::getStatus, status);
    if (keyword != null && !keyword.isEmpty()) {
        wrapper.like(Product::getName, keyword);
    }
    List<Product> products = productService.list(wrapper);

    // 构建导出数据
    List<ProductExportVO> list = products.stream().map(p -> {
        ProductExportVO vo = new ProductExportVO();
        vo.setName(p.getName());
        vo.setCategoryName(categoryService.getById(p.getCategoryId()) != null
                ? categoryService.getById(p.getCategoryId()).getName() : "");
        vo.setPrice(p.getPrice() != null ? p.getPrice().doubleValue() : 0);
        vo.setOriginalPrice(p.getOriginalPrice() != null ? p.getOriginalPrice().doubleValue() : null);
        vo.setStock(p.getStock());
        vo.setSales(p.getSales());
        vo.setDescription(p.getDescription());
        vo.setMainImage(p.getMainImage());
        vo.setTag(p.getTag());
        vo.setStatusText(p.getStatus() == 0 ? "上架" : "下架");
        vo.setRecommendText(p.getIsRecommend() == 1 ? "是" : "否");
        return vo;
    }).collect(Collectors.toList());

    // 写 Excel
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader("Content-Disposition", "attachment; filename="
            + URLEncoder.encode("商品数据", StandardCharsets.UTF_8) + ".xlsx");
    EasyExcel.write(response.getOutputStream(), ProductExportVO.class).sheet("商品数据").doWrite(list);
}
```

- [ ] **Step 2: 在 ProductController 新增导入接口**

```java
@PostMapping("/admin/import")
public Result<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) throws IOException {
    if (file.isEmpty()) {
        return Result.error(400, "文件不能为空");
    }
    if (!file.getOriginalFilename().endsWith(".xlsx")) {
        return Result.error(400, "仅支持 .xlsx 格式");
    }

    List<ProductImportRow> rows = EasyExcel.read(file.getInputStream())
            .head(ProductImportRow.class)
            .sheet()
            .doReadSync();

    int success = 0;
    List<String> errors = new ArrayList<>();
    int lineNum = 1; // 表头占一行

    for (ProductImportRow row : rows) {
        lineNum++;
        if (row.getName() == null || row.getName().trim().isEmpty()) {
            errors.add("第" + lineNum + "行：商品名称为空");
            continue;
        }
        if (row.getPrice() == null) {
            errors.add("第" + lineNum + "行（" + row.getName() + "）：价格为空");
            continue;
        }
        if (row.getStock() == null) {
            errors.add("第" + lineNum + "行（" + row.getName() + "）：库存为空");
            continue;
        }

        // 按名称查找已有商品
        Product existing = productService.lambdaQuery()
                .eq(Product::getName, row.getName().trim())
                .one();

        if (existing != null) {
            // 更新
            existing.setPrice(BigDecimal.valueOf(row.getPrice()));
            if (row.getOriginalPrice() != null) existing.setOriginalPrice(BigDecimal.valueOf(row.getOriginalPrice()));
            existing.setStock(row.getStock());
            if (row.getCategoryName() != null && !row.getCategoryName().isEmpty()) {
                Category cat = categoryService.lambdaQuery()
                        .eq(Category::getName, row.getCategoryName().trim()).one();
                if (cat != null) existing.setCategoryId(cat.getId());
            }
            if (row.getDescription() != null) existing.setDescription(row.getDescription());
            if (row.getMainImage() != null) existing.setMainImage(row.getMainImage());
            if (row.getTag() != null) existing.setTag(row.getTag());
            if (row.getStatus() != null) existing.setStatus(row.getStatus());
            if (row.getIsRecommend() != null) existing.setIsRecommend(row.getIsRecommend());
            productService.updateById(existing);
        } else {
            // 新增
            Product p = new Product();
            p.setName(row.getName().trim());
            p.setPrice(BigDecimal.valueOf(row.getPrice()));
            if (row.getOriginalPrice() != null) p.setOriginalPrice(BigDecimal.valueOf(row.getOriginalPrice()));
            p.setStock(row.getStock());
            p.setSales(0);
            if (row.getCategoryName() != null && !row.getCategoryName().isEmpty()) {
                Category cat = categoryService.lambdaQuery()
                        .eq(Category::getName, row.getCategoryName().trim()).one();
                if (cat != null) p.setCategoryId(cat.getId());
            }
            p.setDescription(row.getDescription() != null ? row.getDescription() : "");
            p.setMainImage(row.getMainImage() != null ? row.getMainImage() : "");
            p.setTag(row.getTag() != null ? row.getTag() : "");
            p.setStatus(row.getStatus() != null ? row.getStatus() : 0);
            p.setIsRecommend(row.getIsRecommend() != null ? row.getIsRecommend() : 0);
            p.setHasSpec(0);
            p.setCreatedTime(LocalDateTime.now());
            p.setUpdatedTime(LocalDateTime.now());
            productService.save(p);
        }
        success++;
    }

    Map<String, Object> result = new java.util.HashMap<>();
    result.put("success", success);
    result.put("total", rows.size());
    result.put("errors", errors);
    return Result.success(result);
}
```

- [ ] **Step 3: 新建 ProductImportRow 内部类**

在 `ProductController` 类内部（或在单独文件）添加导入行 VO：

```java
@Data
public static class ProductImportRow {
    @ExcelProperty("商品名称")
    private String name;

    @ExcelProperty("分类")
    private String categoryName;

    @ExcelProperty("价格")
    private Double price;

    @ExcelProperty("原价")
    private Double originalPrice;

    @ExcelProperty("库存")
    private Integer stock;

    @ExcelProperty("商品描述")
    private String description;

    @ExcelProperty("主图URL")
    private String mainImage;

    @ExcelProperty("标签")
    private String tag;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("是否推荐")
    private Integer isRecommend;
}
```

- [ ] **Step 4: 确认必要的 import**

确保 `ProductController.java` 顶部包含以下 import：

```java
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.product.entity.ProductExportVO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
```

- [ ] **Step 5: 注入 CategoryService**

如果 `ProductController` 中还没有注入 `CategoryService`，添加：

```java
private final CategoryService categoryService;
```

并在构造函数参数或 `@RequiredArgsConstructor` 中自动注入。

- [ ] **Step 6: 提交**

```bash
cd d:/project/webwork && git add backend/product-service/src/main/java/com/mall/product/controller/ProductController.java backend/product-service/src/main/java/com/mall/product/entity/ProductExportVO.java && git commit -m "feat: 新增商品导入导出接口
- 导出商品列表为 Excel（支持筛选条件）
- 导入 Excel 批量创建/更新商品（按名称去重）"
```

---

### Task 4: 前端导入导出功能

**Files:**
- Modify: `admin-frontend/src/api/product.ts`
- Modify: `admin-frontend/src/views/ProductListView.vue`

- [ ] **Step 1: 新增导入导出 API**

在 `admin-frontend/src/api/product.ts` 末尾添加：

```typescript
export function exportProducts(params?: {
  categoryId?: number
  status?: number
  keyword?: string
}) {
  const query = new URLSearchParams()
  if (params?.categoryId) query.set('categoryId', String(params.categoryId))
  if (params?.status !== undefined) query.set('status', String(params.status))
  if (params?.keyword) query.set('keyword', params.keyword)
  return `/product/admin/export?${query.toString()}`
}

export function importProducts(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, { success: number; total: number; errors: string[] }>('/product/admin/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

- [ ] **Step 2: 在 ProductListView.vue 添加入导出按钮**

在 `admin-frontend/src/views/ProductListView.vue` 中找到工具栏区域，添加以下按钮：

```vue
<el-button @click="handleExport" :loading="exporting">导出</el-button>
<el-button type="success" @click="handleImportClick">导入</el-button>
```

并在 script 中添加逻辑：

```typescript
import { exportProducts, importProducts } from '@/api/product'
import { ElMessage, ElMessageBox } from 'element-plus'

const exporting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    const url = exportProducts({
      categoryId: query.value.categoryId,
      status: query.value.status,
      keyword: query.value.keyword
    })
    // 使用 download 触发下载
    const token = localStorage.getItem('admin_token')
    const fullUrl = import.meta.env.VITE_API_BASE || 'http://localhost:8080/api'
    const link = document.createElement('a')
    link.href = fullUrl + url + (token ? `?token=${token}` : '')
    link.download = '商品数据.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const fileInput = ref<HTMLInputElement>()

function handleImportClick() {
  if (!fileInput.value) {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = '.xlsx'
    input.onchange = handleImportFile
    fileInput.value = input
  }
  fileInput.value.click()
}

async function handleImportFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  try {
    await ElMessageBox.confirm(`确定导入文件「${file.name}」？`, '确认导入')
    const res = await importProducts(file)
    ElMessage.success(`导入完成：成功 ${res.success}/${res.total} 条`)
    if (res.errors && res.errors.length > 0) {
      console.warn('导入错误：', res.errors)
      ElMessage.warning(`${res.errors.length} 条导入失败，详情见控制台`)
    }
    loadData() // 刷新列表
  } catch {
    // 用户取消或出错
  }
}
```

- [ ] **Step 3: 提交**

```bash
cd d:/project/webwork && git add admin-frontend/src/api/product.ts admin-frontend/src/views/ProductListView.vue && git commit -m "feat: 新增前端商品导入导出功能
- 工具栏添加导入/导出按钮
- 导出按当前筛选条件下载 Excel
- 导入支持选择 .xlsx 文件并上传"
```

---

### 验证清单

- [ ] 商品列表页工具栏有「导出」和「导入」按钮
- [ ] 点击导出可下载包含当前筛选商品的 Excel 文件
- [ ] 下载的 Excel 列头正确、数据完整
- [ ] 点击导入→选择 Excel→确认后可成功导入
- [ ] 导入时已存在的商品按名称匹配更新，不存在的新增
- [ ] 导入完成后提示成功/失败条数
- [ ] 导入异常（空文件、格式错误）有友好提示
