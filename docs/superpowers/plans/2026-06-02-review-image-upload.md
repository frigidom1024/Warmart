# 商品评价图片上传功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 支持用户在提交商品评价时上传最多 3 张图片，并在评价列表中展示可点击预览的图片。

**Architecture:** 后端在 CommentController 新增评价图片上传接口（复用 ProductController.uploadImage 的现有文件上传模式），前端在 ProductDetailView 的评价表单中增加 Element Plus 图片上传组件，提交时图片 URL 以逗号分隔传入现有 imageUrls 字段。

**Tech Stack:** Java 17 + Spring Boot 3 + Spring Security OAuth2 Resource Server + Vue 3 + TypeScript + Element Plus

---

### Task 1: 后端 — 新增评价图片上传接口

**Files:**
- Modify: `backend/product-service/src/main/java/com/mall/product/controller/CommentController.java`
- No config changes needed（ResourceServerConfig 默认对所有非 permitAll 路径要求登录认证，新接口自动受保护）

- [ ] **Step 1: 在 CommentController 新增 uploadImage 方法**

在 `CommentController.java` 的 `add` 方法之后添加以下代码：

```java
    @Value("${comment.upload.path:./static/images/comments}")
    private String commentUploadPath;

    @PostMapping("/comment/upload-image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            return Result.error(400, "文件名无效");
        }
        String ext = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx > 0) {
            ext = originalName.substring(dotIdx).toLowerCase();
        }
        if (!ext.equals(".png") && !ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".webp")) {
            return Result.error(400, "仅支持 PNG、JPG、WebP 格式");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path dir = Paths.get(commentUploadPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path dest = dir.resolve(filename);
            file.transferTo(dest.toFile());
            String url = "/images/comments/" + filename;
            return Result.success(url);
        } catch (IOException e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
```

需要添加的 import（如果 IDE 未自动补全）：
```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
```

- [ ] **Step 2: 编译验证**

Run:
```bash
cd d:/project/webwork/backend/product-service
mvn compile -q
```
Expected: BUILD SUCCESS（无编译错误）

- [ ] **Step 3: Commit**

```bash
cd d:/project/webwork
git add backend/product-service/src/main/java/com/mall/product/controller/CommentController.java
git commit -m "feat: 新增评价图片上传接口"
```

---

### Task 2: 前端 — 新增评价图片上传 API 方法

**Files:**
- Modify: `frontend/src/api/comment.ts`

- [ ] **Step 1: 在 comment.ts 添加 uploadCommentImage 方法**

更新 `frontend/src/api/comment.ts`，在现有代码末尾添加：

```typescript
export function uploadCommentImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/product/comment/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => res.data)
}
```

- [ ] **Step 2: Commit**

```bash
cd d:/project/webwork
git add frontend/src/api/comment.ts
git commit -m "feat: 新增评价图片上传 API"
```

---

### Task 3: 前端 — 评价表单集成图片上传

**Files:**
- Modify: `frontend/src/views/ProductDetailView.vue`

- [ ] **Step 1: 添加图片上传相关的状态和逻辑（Script 部分）**

在 `ProductDetailView.vue` 的 `<script setup>` 中，在现有 `submittingComment` ref 之后添加：

```typescript
// Review image upload
const commentImages = ref<string[]>([])
const uploadingImage = ref(false)

async function handleUploadImage(file: File): Promise<boolean> {
  if (commentImages.value.length >= 3) {
    showToast('最多上传 3 张图片', 'warning')
    return false
  }
  uploadingImage.value = true
  try {
    const url = await uploadCommentImage(file)
    commentImages.value.push(url)
    return true
  } catch {
    showToast('图片上传失败，请重试', 'error')
    return false
  } finally {
    uploadingImage.value = false
  }
}

function handleRemoveImage(index: number) {
  commentImages.value.splice(index, 1)
}
```

同时更新 import，将 `uploadCommentImage` 从 `@/api/comment` 导入（或者从已有的 `addComment` 同一来源导入）。当前 ProductDetailView.vue 第 4 行已有：
```typescript
import { getProductDetail, addComment, getCommentList } from '@/api/product'
```

需要改为：
```typescript
import { getProductDetail, addComment, getCommentList } from '@/api/product'
import { uploadCommentImage } from '@/api/comment'
```

更新 `handleSubmitComment` 方法，在第 226 行的 `addComment` 调用中传入 `imageUrls`：

```typescript
await addComment({
  productId,
  content: newComment.value.trim(),
  rating: newRating.value,
  imageUrls: commentImages.value.length > 0 ? commentImages.value.join(',') : undefined
})
```

在成功提交后，清空 `commentImages.value`（在 `newComment.value = ''` 之后添加）：

```typescript
commentImages.value = []
```

- [ ] **Step 2: 添加图片上传 UI（Template 部分）**

在评论表单的文本域和操作按钮之间（第 436 行 `</textarea>` 之后、第 437 行 `pdp__comments-form-actions` 之前），添加图片上传区域：

```html
<!-- Image Upload -->
<div class="pdp__comments-form-images">
  <div class="pdp__comments-form-image-list">
    <div v-for="(url, i) in commentImages" :key="i" class="pdp__comments-form-image-item">
      <img :src="url" class="pdp__comments-form-image-preview">
      <button class="pdp__comments-form-image-remove" @click="handleRemoveImage(i)">×</button>
    </div>
    <label v-if="commentImages.length < 3" class="pdp__comments-form-image-upload"
           :class="{ 'pdp__comments-form-image-upload--disabled': uploadingImage }">
      <input type="file" accept="image/png,image/jpeg,image/webp"
             :disabled="uploadingImage"
             @change="async (e) => {
               const file = (e.target as HTMLInputElement).files?.[0]
               if (file) {
                 await handleUploadImage(file)
                 ;(e.target as HTMLInputElement).value = ''
               }
             }">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        <circle cx="8.5" cy="8.5" r="1.5"/>
        <polyline points="21 15 16 10 5 21"/>
      </svg>
      <span>{{ uploadingImage ? '上传中...' : '上传图片' }}</span>
    </label>
  </div>
  <span class="pdp__comments-form-image-hint">{{ commentImages.length }}/3 张</span>
</div>
```

- [ ] **Step 3: 添加图片上传区域的 CSS（Style 部分）**

在 `<style scoped>` 中，在 `.pdp__comments-form-submit:disabled` 规则之后（第 1224 行后）添加：

```css
/* Image Upload */
.pdp__comments-form-images {
  margin-top: 12px;
}
.pdp__comments-form-image-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.pdp__comments-form-image-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--wz-border);
}
.pdp__comments-form-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pdp__comments-form-image-remove {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
  cursor: pointer;
  border: none;
  padding: 0;
}
.pdp__comments-form-image-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 1px dashed var(--wz-border);
  border-radius: 8px;
  cursor: pointer;
  color: var(--wz-text-muted);
  font-size: 11px;
  gap: 4px;
  transition: all 0.2s;
}
.pdp__comments-form-image-upload:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.pdp__comments-form-image-upload--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.pdp__comments-form-image-upload input {
  display: none;
}
.pdp__comments-form-image-hint {
  display: block;
  font-size: 12px;
  color: var(--wz-text-muted);
  margin-top: 6px;
}
```

- [ ] **Step 4: 优化评价列表图片展示**

当前第 465-467 行已有图片展示代码，但点击预览没有大图效果。将其替换为使用 `<el-image>` 组件支持点击预览：

```html
<div v-if="comment.imageUrls" class="pdp__comment-images">
  <el-image
    v-for="(url, i) in comment.imageUrls.split(',')"
    :key="i"
    :src="url"
    :preview-src-list="comment.imageUrls!.split(',')"
    :initial-index="i"
    class="pdp__comment-image-el"
    fit="cover"
  />
</div>
```

在 Style 中添加：
```css
.pdp__comment-image-el {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  cursor: pointer;
}
```

注意：`el-image` 的 `preview-src-list` 需要数组，非空断言 `!` 安全因为外层已有 `v-if` 判断。

- [ ] **Step 5: Commit**

```bash
cd d:/project/webwork
git add frontend/src/views/ProductDetailView.vue
git commit -m "feat: 评价表单集成图片上传和预览"
```

---

### Task 4: 验证

- [ ] **Step 1: 前端编译验证**

```bash
cd d:/project/webwork/frontend
npx vue-tsc --noEmit
```
Expected: 无类型错误

- [ ] **Step 2: 后端编译验证**

```bash
cd d:/project/webwork/backend/product-service
mvn compile -q
```
Expected: BUILD SUCCESS
