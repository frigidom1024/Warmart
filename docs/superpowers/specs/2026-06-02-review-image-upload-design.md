# 商品评价图片上传功能设计

## 概述

商品评价需要支持用户上传评价图片。每个评价最多 3 张图片，存储到本地服务器，沿用项目现有的文件上传模式。

## 目标

- 用户在提交商品评价时可上传最多 3 张图片
- 图片上传后可在评价列表中展示并点击预览
- 与现有评价流程无缝集成

## 非目标

- 拖拽排序图片（按上传顺序展示即可）
- 图片裁剪/压缩/格式转换
- 云存储 OSS 集成
- 评价图片的管理后台功能（删除/审核等）

## 后端设计

### 新增上传接口

| 项目 | 内容 |
|---|---|
| 方法 | `POST` |
| 路径 | `/api/product/comment/upload-image` |
| Content-Type | `multipart/form-data` |
| 鉴权 | 需要 JWT 登录 |
| 参数 | `file` (MultipartFile) |
| 响应 | `{"code": 200, "msg": "success", "data": "/images/comments/uuid.ext"}` |

### 实现逻辑

评价图片上传接口位于 `CommentController.java`，遵循 `ProductController.uploadImage` 的现有模式：

```java
@PostMapping("/upload-image")
public Result uploadImage(@RequestParam("file") MultipartFile file) {
    // 1. 校验文件是否为空
    // 2. 校验文件格式（PNG、JPG、JPEG、WebP）
    // 3. UUID 重命名文件
    // 4. 保存到 ${upload.path:./static/images}/comments/ 目录
    // 5. 返回 /images/comments/uuid.ext
}
```

### 文件存储

- 目录：`./static/images/comments/`
- 文件名：`UUID + 原始扩展名`
- 大小限制：沿用 `spring.servlet.multipart.max-file-size: 2MB`
- 允许格式：`image/png`、`image/jpeg`、`image/webp`

### 静态资源映射

已有 `StaticResourceConfig.java` 将 `/images/**` 映射到 `file:./static/images/`，新目录自动被覆盖。

### 数据库

已有 `product_comment.imageUrls` 字段（VARCHAR 类型，逗号分隔 URL），无需表结构变更。

## 前端设计

### 新增 API

**`frontend/src/api/comment.ts`**：

```typescript
export function uploadCommentImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/product/comment/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => res.data)
}
```

### 评价表单（ProductDetailView.vue）

在现有评价表单区域（星级评分 + 文本输入框）下方增加图片上传组件：

- 使用 Element Plus `<el-upload>` 组件
- 上传方式：选择文件 → 调用 `uploadCommentImage` → 返回 URL → 存入 `imageList: string[]`
- 预览：已上传图片可点击查看大图
- 删除：每张图右上角删除按钮，移除 URL
- 上限：最多 3 张，达到上限时隐藏上传按钮，显示提示"最多 3 张"
- 提交时：`imageList.join(',')` 作为 `imageUrls` 参数

### 评价列表

保持现有的 `comment.imageUrls` 渲染逻辑，确保：
- 解析逗号分隔的 URL 为多张图片
- 使用 `<el-image>` 组件支持点击预览大图

### 状态变更

| 组件 | 变更 |
|---|---|
| `CommentDialog.vue` (如有) | 新增图片上传区域 |
| `ProductDetailView.vue` | 评价表单增加上传组件 |
| `CommentItem` 类型 | 无需变更，已有 `imageUrls: string \| null` |

## 数据流

```
用户选择图片 → 前端预览
     ↓
逐张上传 → multipart/form-data + JWT
     ↓
后端保存到 ./static/images/comments/ → 返回 URL
     ↓
前端将 URL 加入 imageList[]
     ↓
提交评价 → addComment({ ..., imageUrls: "url1,url2,url3" })
     ↓
后端存入 DB → 列表页解析展示
```

## 错误处理

| 场景 | 前端提示 | 后端行为 |
|---|---|---|
| 文件 > 2MB | "图片大小不能超过 2MB" | 返回 400 错误 |
| 格式不合法 | "仅支持 PNG/JPG/WebP 格式" | 返回 400 错误 |
| 文件为空 | "请选择要上传的图片" | 返回 400 错误 |
| 上传网络异常 | "上传失败，请重试" | - |
| 超过 3 张 | 上传按钮禁用 + "最多上传 3 张" | - |

## 权限

- 上传接口：需 JWT 认证
- 评价提交：已有鉴权，不变
- 评价列表查看：无需登录（permitAll，不变）

## 涉及文件清单

### 后端

| 文件 | 操作 |
|---|---|
| `backend/product-service/.../controller/CommentController.java` | 新增 `uploadImage` 方法 |
| `backend/product-service/.../config/ResourceServerConfig.java` | 确认新接口被登录保护 |
| `backend/product-service/src/main/resources/application.yml` | 无需变更（上传限制已全局配置） |

### 前端

| 文件 | 操作 |
|---|---|
| `frontend/src/api/comment.ts` | 新增 `uploadCommentImage` 方法 |
| `frontend/src/views/ProductDetailView.vue` | 评价表单增加图片上传组件 + 评价列表完善图片展示 |
