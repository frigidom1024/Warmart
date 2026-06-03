# 管理后台功能补全设计

## 概述

在现有电商平台基础上，补全三个管理后台缺失功能：评价管理、商品批量导入导出、角色权限管理。

---

## 一、评价管理后台

### 后端

在 `product-service` 的 `CommentController` 中新增管理端接口：

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/product/admin/comment/list` | 分页查询所有评价 |
| DELETE | `/api/product/admin/comment/{id}` | 删除评价 |

**列表接口参数**：
- `productName` — 按商品名称模糊搜索
- `username` — 按用户名搜索
- `rating` — 按评分筛选（1-5）
- `page` / `size` — 分页

**已有实体复用**：直接使用 `Comment` 实体和 `CommentService`，无需新增 Entity/Service。

### 前端

- 新增 `admin-frontend/src/views/CommentManageView.vue`
- el-table 展示：商品名称、用户名、评分（星级展示）、评价内容、图片缩略图、评价时间
- 操作列：删除按钮（二次确认弹窗）
- 搜索区：商品名称、用户名
- 路由：`/admin/comments` → `CommentManageView`
- 侧边栏菜单新增「评价管理」入口（仅 SUPER_ADMIN 和 ADMIN 可见）

---

## 二、商品批量导入导出

### 后端

在 `product-service` 中添加 EasyExcel 依赖，`ProductController` 新增接口：

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/product/admin/export` | 导出商品 Excel，支持筛选条件 |
| POST | `/api/product/admin/import` | 上传 Excel 导入商品 |

**导出格式**（列头）：

| 商品名称 | 分类 | 价格 | 原价 | 库存 | 商品描述 | 主图URL | 标签 | 状态 | 是否推荐 |
|----------|------|------|------|------|----------|---------|------|------|---------|

**导入逻辑**：
1. 解析 Excel，按行读取
2. 按商品名称去重：已存在则更新，不存在则新增
3. 分类按名称匹配到已有分类
4. 返回导入结果（成功条数、失败条数、失败原因列表）

### 前端

在 `admin-frontend/src/views/ProductListView.vue` 中：
- 工具栏新增「导入」按钮 → 文件选择对话框（仅 .xlsx）
- 工具栏新增「导出」按钮 → 下载当前筛选条件的 Excel
- 导入完成后弹出结果提示

---

## 三、角色权限管理

### 后端

**角色更新接口**：
| 方法 | 路径 | 功能 |
|------|------|------|
| PUT | `/api/auth/admin/users/{id}/role` | 修改用户角色 |

请求体：`{ "role": "ADMIN" | "SUPER_ADMIN" | "USER" }`

**权限校验**：
- 在 Gateway 的 `AuthGlobalFilter` 中，对 `/api/*/admin/**` 路径的请求校验 JWT 中的 `role` claim
- 只有 `SUPER_ADMIN` 和 `ADMIN` 角色可访问管理端 API
- 部分敏感接口（如修改角色）仅允许 `SUPER_ADMIN`

### 前端

**角色分级**：
- `SUPER_ADMIN`（超级管理员）：所有菜单可见
- `ADMIN`（普通管理员）：隐藏「用户管理」「管理员设置」等菜单

**改动文件**：
- `admin-frontend/src/layouts/AdminLayout.vue` — 菜单根据角色动态渲染
- `admin-frontend/src/views/UserListView.vue` — 增加「角色」列和编辑功能
- `admin-frontend/src/api/user.ts` — 新增角色更新 API
- `admin-frontend/src/stores/user.ts` — 存储当前用户角色信息

---

## 四、涉及文件清单

### 评价管理
| 文件 | 操作 |
|------|------|
| `backend/product-service/.../controller/CommentController.java` | 新增 admin 列表和删除接口 |
| `admin-frontend/src/views/CommentManageView.vue` | 新建 |
| `admin-frontend/src/router/index.ts` | 新增路由 |
| `admin-frontend/src/layouts/AdminLayout.vue` | 新增菜单项 |
| `admin-frontend/src/api/comment.ts` | 新增 admin API |

### 商品批量导入导出
| 文件 | 操作 |
|------|------|
| `backend/product-service/pom.xml` | 添加 EasyExcel 依赖 |
| `backend/product-service/.../controller/ProductController.java` | 新增导入导出接口 |
| `backend/product-service/.../entity/ProductExportVO.java` | 新建导出 VO |
| `admin-frontend/src/views/ProductListView.vue` | 新增导入导出按钮 |
| `admin-frontend/src/api/product.ts` | 新增导入导出 API |

### 角色权限管理
| 文件 | 操作 |
|------|------|
| `backend/auth-service/.../controller/AdminUserController.java` | 新增角色更新接口 |
| `backend/gateway/.../AuthGlobalFilter.java` | 增加 admin 接口角色校验 |
| `admin-frontend/src/layouts/AdminLayout.vue` | 菜单按角色控制 |
| `admin-frontend/src/views/UserListView.vue` | 增加角色编辑 |
| `admin-frontend/src/stores/user.ts` | 存储角色信息 |
| `admin-frontend/src/api/user.ts` | 新增角色更新 API |
