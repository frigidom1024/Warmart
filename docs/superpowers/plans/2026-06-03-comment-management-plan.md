# 评价管理后台 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为管理后台添加评价管理页面，支持查看所有评价并删除违规评价

**Architecture:** 在 product-service 中新增管理端评价接口（复用现有 Comment 实体和服务），admin-frontend 新增评价管理视图页面

**Tech Stack:** Java Spring Boot + MyBatis-Plus + Vue 3 + Element Plus

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `backend/product-service/.../controller/CommentController.java` | 修改 | 新增 admin 列表和删除接口 |
| `admin-frontend/src/api/comment.ts` | 修改 | 新增 admin 评价相关 API |
| `admin-frontend/src/views/CommentManageView.vue` | 新建 | 评价管理页面 |
| `admin-frontend/src/router/index.ts` | 修改 | 添加评价管理路由 |
| `admin-frontend/src/layouts/AdminLayout.vue` | 修改 | 添加评价管理菜单项 |

---

### Task 1: 后端评价管理接口

**Files:**
- Modify: `backend/product-service/src/main/java/com/mall/product/controller/CommentController.java`

- [ ] **Step 1: 在 CommentController 新增 admin 评价列表接口**

```java
// 在文件末尾新增

@GetMapping("/admin/comment/list")
public Result<IPage<CommentVO>> adminList(
        @RequestParam(required = false) String productName,
        @RequestParam(required = false) String username,
        @RequestParam(required = false) Integer rating,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    return Result.success(commentService.adminList(productName, username, rating, page, size));
}

@DeleteMapping("/admin/comment/{id}")
public Result<Void> adminDelete(@PathVariable Long id) {
    commentService.removeById(id);
    return Result.success(null);
}
```

- [ ] **Step 2: 在 CommentService 新增 adminList 方法**

修改 `backend/product-service/src/main/java/com/mall/product/service/CommentService.java`：

```java
// 在 listByProductId 方法之后新增

public IPage<CommentVO> adminList(String productName, String username, Integer rating, int page, int size) {
    Page<Comment> p = new Page<>(page, size);
    LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
            .orderByDesc(Comment::getCreatedTime);
    
    if (productName != null && !productName.isEmpty()) {
        wrapper.like(Comment::getProductName, productName);
    }
    if (username != null && !username.isEmpty()) {
        wrapper.like(Comment::getUsername, username);
    }
    if (rating != null && rating >= 1 && rating <= 5) {
        wrapper.eq(Comment::getRating, rating);
    }
    
    Page<Comment> commentPage = commentMapper.selectPage(p, wrapper);
    List<CommentVO> voList = commentPage.getRecords().stream().map(c -> {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(c, vo);
        return vo;
    }).collect(Collectors.toList());
    
    Page<CommentVO> voPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
    voPage.setRecords(voList);
    return voPage;
}
```

- [ ] **Step 3: 确认 CommentService 已有必要的 import**

检查 `CommentService.java` 开头是否缺少以下 import，如有则添加：

```java
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
```

- [ ] **Step 4: 提交**

```bash
cd d:/project/webwork && git add backend/product-service/src/main/java/com/mall/product/controller/CommentController.java backend/product-service/src/main/java/com/mall/product/service/CommentService.java && git commit -m "feat: 新增评价管理后台接口
- 管理员分页查询所有评价，支持按商品名/用户名/评分筛选
- 管理员删除评价接口"
```

---

### Task 2: 前端评价管理 API

**Files:**
- Modify: `admin-frontend/src/api/comment.ts`

- [ ] **Step 1: 新增评价管理 API**

```typescript
// 在文件末尾新增

export interface CommentItem {
  id: number
  productId: number
  productName: string
  userId: number
  username: string
  content: string
  rating: number
  imageUrls: string
  createdTime: string
}

export function getAdminCommentList(params: {
  productName?: string
  username?: string
  rating?: number
  page?: number
  size?: number
}) {
  return request.get<any, { records: CommentItem[]; total: number }>('/product/admin/comment/list', { params })
}

export function deleteAdminComment(id: number) {
  return request.delete<void>('/product/admin/comment/' + id)
}
```

- [ ] **Step 2: 提交**

```bash
cd d:/project/webwork && git add admin-frontend/src/api/comment.ts && git commit -m "feat: 新增前端评价管理 API"
```

---

### Task 3: 评价管理页面

**Files:**
- Create: `admin-frontend/src/views/CommentManageView.vue`
- Modify: `admin-frontend/src/router/index.ts`
- Modify: `admin-frontend/src/layouts/AdminLayout.vue`

- [ ] **Step 1: 新建 CommentManageView.vue**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import { getAdminCommentList, deleteAdminComment } from '@/api/comment'
import type { CommentItem } from '@/api/comment'

const comments = ref<CommentItem[]>([])
const total = ref(0)
const loading = ref(false)

const query = ref({
  productName: '',
  username: '',
  rating: undefined as number | undefined,
  page: 1,
  size: 10
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminCommentList(query.value)
    comments.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.value.page = 1
  loadData()
}

function onReset() {
  query.value.productName = ''
  query.value.username = ''
  query.value.rating = undefined
  query.value.page = 1
  loadData()
}

function onPageChange(p: number) {
  query.value.page = p
  loadData()
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该评价？', '确认删除', { type: 'warning' })
    await deleteAdminComment(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>评价管理</h2>
    </div>

    <el-card class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="商品名称">
          <el-input v-model="query.productName" placeholder="输入商品名称" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="输入用户名" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="query.rating" placeholder="全部" clearable style="width:120px">
            <el-option label="5星" :value="5" />
            <el-option label="4星" :value="4" />
            <el-option label="3星" :value="3" />
            <el-option label="2星" :value="2" />
            <el-option label="1星" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="comments" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column label="评分" width="140">
          <template #default="{ row }">
            <el-icon v-for="i in row.rating" :key="i" color="#f7ba2a"><StarFilled /></el-icon>
            <el-icon v-for="i in (5 - row.rating)" :key="'e'+i" color="#ccc"><StarFilled /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="imageUrls" label="图片" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrls"
              :src="row.imageUrls.split(',')[0]"
              style="width:50px;height:50px"
              fit="cover"
              :preview-src-list="row.imageUrls.split(',')"
              preview-teleported
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.search-card { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
```

- [ ] **Step 2: 添加路由**

在 `admin-frontend/src/router/index.ts` 中 `/logistics` 路由之后添加：

```typescript
{
  path: '/comments',
  name: 'CommentManage',
  component: () => import('@/views/CommentManageView.vue'),
  meta: { title: '评价管理' }
},
```

- [ ] **Step 3: 添加菜单项**

在 `admin-frontend/src/layouts/AdminLayout.vue` 的侧边栏菜单中（在物流管理之后或退款管理之后）添加：

```vue
<el-menu-item index="/comments">
  <el-icon><ChatDotSquare /></el-icon>
  <span>评价管理</span>
</el-menu-item>
```

并在 script 中的图标导入添加 `ChatDotSquare`（如果尚未导入）。

- [ ] **Step 4: 提交**

```bash
cd d:/project/webwork && git add admin-frontend/src/views/CommentManageView.vue admin-frontend/src/router/index.ts admin-frontend/src/layouts/AdminLayout.vue && git commit -m "feat: 新增评价管理页面
- 新增 CommentManageView 评价管理视图
- 配置路由和侧边栏菜单
- 支持按商品名称/用户名/评分搜索
- 支持删除评价"
```

---

### 评价管理 — 验证清单

- [ ] 管理员登录后可在侧边栏看到「评价管理」菜单
- [ ] 进入页面后可看到所有评价列表
- [ ] 可按商品名称/用户名/评分搜索
- [ ] 点击删除可删除评价，刷新后消失
- [ ] 分页功能正常
