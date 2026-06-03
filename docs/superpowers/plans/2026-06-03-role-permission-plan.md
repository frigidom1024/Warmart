# 角色权限管理 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现简单角色控制，分 SUPER_ADMIN（超级管理员）和 ADMIN（普通管理员），控制管理后台菜单可见性

**Architecture:** 用户实体已有 role 字段，后端新增角色更新接口，gateway 过滤器中增加管理端接口的角色校验，前端根据角色动态渲染菜单

**Tech Stack:** Java Spring Boot + Spring Cloud Gateway + Vue 3 + Element Plus + Pinia

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `backend/auth-service/.../controller/AdminUserController.java` | 修改 | 新增角色更新和获取接口 |
| `backend/gateway/.../AuthGlobalFilter.java` | 修改 | 增加 admin 端点角色校验 |
| `admin-frontend/src/api/user.ts` | 修改 | 新增角色更新 API |
| `admin-frontend/src/stores/user.ts` | 修改 | 存储当前用户角色 |
| `admin-frontend/src/views/UserListView.vue` | 修改 | 增加角色列和编辑 |
| `admin-frontend/src/layouts/AdminLayout.vue` | 修改 | 按角色控制菜单显示 |
| `admin-frontend/src/views/LoginView.vue` | 修改 | 登录后保存角色信息 |

---

### Task 1: 后端角色更新接口

**Files:**
- Modify: `backend/auth-service/src/main/java/com/mall/auth/controller/AdminUserController.java`

- [ ] **Step 1: 新增角色更新和获取用户角色接口**

```java
// 在类中新增方法

@PutMapping("/{id}/role")
public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
    String role = body.get("role");
    if (role == null || (!role.equals("ADMIN") && !role.equals("USER") && !role.equals("SUPER_ADMIN"))) {
        return Result.error(400, "无效的角色值");
    }
    User user = userMapper.selectById(id);
    if (user == null) {
        return Result.error(404, "用户不存在");
    }
    user.setRole(role);
    userMapper.updateById(user);
    return Result.success(null);
}
```

- [ ] **Step 2: 提交**

```bash
cd d:/project/webwork && git add backend/auth-service/src/main/java/com/mall/auth/controller/AdminUserController.java && git commit -m "feat: 新增用户角色更新接口"
```

---

### Task 2: Gateway 角色校验

**Files:**
- Modify: `backend/gateway/src/main/java/com/mall/gateway/AuthGlobalFilter.java`

- [ ] **Step 1: 增加 admin 端点的角色校验**

找到 `AuthGlobalFilter` 中检查 JWT 的部分，在 token 校验逻辑后添加角色检查：

```java
// 在已存在的 token 解析逻辑之后，添加：
// 校验管理端 API 的角色权限
String path = exchange.getRequest().getURI().getPath();
if (path.contains("/admin/")) {
    String role = claims.get("role", String.class);
    if (role == null || (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role))) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }
}
```

注意：需要将角色信息放入 JWT 的 claims 中。检查 auth-service 的 `AuthService.java` 中 `login` 方法生成 token 时是否已包含 role：

```java
// 确保 JWT claims 中包含 role
// 在 AuthService.login 或 token 生成处：
Set<String> scopes = new java.util.HashSet<>();
scopes.add(user.getRole() != null ? user.getRole() : "USER");
// ... 已有 scopes 逻辑
```

如果已有 scopes 逻辑包含 role，则 gateway 中从 scopes 集合判断即可。

- [ ] **Step 2: 提交**

```bash
cd d:/project/webwork && git add backend/gateway/src/main/java/com/mall/gateway/AuthGlobalFilter.java && git commit -m "feat: Gateway 增加管理端接口角色校验"
```

---

### Task 3: 前端角色管理和菜单控制

**Files:**
- Modify: `admin-frontend/src/stores/user.ts`
- Modify: `admin-frontend/src/api/user.ts`
- Modify: `admin-frontend/src/views/LoginView.vue`
- Modify: `admin-frontend/src/layouts/AdminLayout.vue`
- Modify: `admin-frontend/src/views/UserListView.vue`

- [ ] **Step 1: 完善 user store 存储角色信息**

修改 `admin-frontend/src/stores/user.ts`：

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const username = ref(localStorage.getItem('admin_username') || '')
  const role = ref(localStorage.getItem('admin_role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')
  const isAdmin = computed(() => role.value === 'ADMIN' || role.value === 'SUPER_ADMIN')

  function setToken(t: string) {
    token.value = t
    localStorage.setItem('admin_token', t)
  }

  function setUserInfo(u: string, r: string) {
    username.value = u
    role.value = r
    localStorage.setItem('admin_username', u)
    localStorage.setItem('admin_role', r)
  }

  function logout() {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_username')
    localStorage.removeItem('admin_role')
  }

  return { token, username, role, isLoggedIn, isSuperAdmin, isAdmin, setToken, setUserInfo, logout }
})
```

- [ ] **Step 2: 登录时保存角色信息**

修改 `admin-frontend/src/views/LoginView.vue`，登录成功后调用 `setUserInfo`：

```typescript
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

async function handleLogin() {
  try {
    const res = await login(form.value)
    userStore.setToken(res.accessToken)
    // 从 JWT payload 中解析 role（或从登录响应中获取）
    const payload = JSON.parse(atob(res.accessToken.split('.')[1]))
    userStore.setUserInfo(form.value.username, payload.role || payload.scopes?.[0] || 'ADMIN')
    await router.push('/dashboard')
  } catch { /* 已有错误处理 */ }
}
```

- [ ] **Step 3: 在 UserListView.vue 增加角色列和编辑功能**

在 `admin-frontend/src/views/UserListView.vue` 的用户表格中添加角色列：

```vue
<el-table-column label="角色" width="130">
  <template #default="{ row }">
    <el-tag v-if="row.role === 'SUPER_ADMIN'" type="danger">超级管理员</el-tag>
    <el-tag v-else-if="row.role === 'ADMIN'" type="warning">管理员</el-tag>
    <el-tag v-else type="info">用户</el-tag>
  </template>
</el-table-column>
<el-table-column label="操作" width="150" fixed="right">
  <!-- 在已有操作之后添加角色修改 -->
  <template #default="{ row }">
    <!-- 已有编辑/禁用按钮 -->
    <el-button 
      v-if="row.role !== 'SUPER_ADMIN' && userStore.isSuperAdmin" 
      type="primary" size="small" 
      @click="handleRoleChange(row)"
    >修改角色</el-button>
  </template>
</el-table-column>
```

添加角色修改弹窗逻辑：

```typescript
import { useUserStore } from '@/stores/user'
import { updateUserRole } from '@/api/user'

const userStore = useUserStore()
const roleDialogVisible = ref(false)
const roleForm = ref({ userId: 0, username: '', role: 'USER' })

async function handleRoleChange(row: any) {
  roleForm.value = { userId: row.id, username: row.username, role: row.role || 'USER' }
  roleDialogVisible.value = true
}

async function submitRoleChange() {
  try {
    await updateUserRole(roleForm.value.userId, roleForm.value.role)
    ElMessage.success('角色更新成功')
    roleDialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error('角色更新失败')
  }
}
```

- [ ] **Step 4: 新增角色更新 API**

在 `admin-frontend/src/api/user.ts` 末尾添加：

```typescript
export function updateUserRole(id: number, role: string) {
  return request.put<void>(`/auth/admin/users/${id}/role`, { role })
}
```

- [ ] **Step 5: AdminLayout 根据角色控制菜单**

修改 `admin-frontend/src/layouts/AdminLayout.vue`：

```typescript
<script setup lang="ts">
import { useUserStore } from '@/stores/user'
const userStore = useUserStore()
</script>
```

在需要根据角色隐藏的菜单项上使用 `v-if`：

```vue
<!-- 仅 SUPER_ADMIN 可见的菜单 -->
<el-menu-item index="/users" v-if="userStore.isSuperAdmin">
  <el-icon><User /></el-icon>
  <span>用户管理</span>
</el-menu-item>

<!-- 所有管理员都可见的菜单保持不变 -->
```

- [ ] **Step 6: 提交**

```bash
cd d:/project/webwork && git add \
  admin-frontend/src/stores/user.ts \
  admin-frontend/src/views/LoginView.vue \
  admin-frontend/src/views/UserListView.vue \
  admin-frontend/src/api/user.ts \
  admin-frontend/src/layouts/AdminLayout.vue \
  && git commit -m "feat: 实现角色权限管理
- Pinia store 存储用户角色
- 登录时保存角色到 localStorage
- 用户管理增加角色列和修改功能
- 侧边栏菜单按角色动态显示
- 新增角色更新 API"
```

---

### 验证清单

- [ ] 超级管理员登录后可看到所有菜单
- [ ] 新建普通管理员账号并登录后，用户管理菜单不可见
- [ ] 超级管理员可在用户管理页面修改其他用户的角色
- [ ] 修改角色后重新登录生效
- [ ] 普通管理员访问 `/users` 路由被重定向或菜单隐藏
