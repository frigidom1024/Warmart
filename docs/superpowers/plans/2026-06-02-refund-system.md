# 退款系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `refund_application` 表，实现完整的退款申请和审核流程

**Architecture:** 后端新增实体/Mapper/Service/Controller，管理端新增独立退款管理页面，客户端改进退款申请表单

**Tech Stack:** Spring Boot + MyBatis-Plus + MySQL + Vue 3 + Element Plus

---

### Task 1: 数据库 — refund_application 建表

**Files:**
- Modify: `deploy/mysql/init.sql`

- [ ] **追加 refund_application 建表语句**

在 `deploy/mysql/init.sql` 文件末尾 `USE mall_order;` 区域内，`order_item` 表之后追加：

```sql
CREATE TABLE IF NOT EXISTS refund_application (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    reason       VARCHAR(500) NOT NULL,
    amount       DECIMAL(10,2) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_reply  VARCHAR(500),
    handled_time DATETIME,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **提交**

```bash
git add deploy/mysql/init.sql
git commit -m "feat: 新增 refund_application 退款申请表"
```

---

### Task 2: 后端 — RefundApplication 实体类

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/entity/RefundApplication.java`

- [ ] **创建实体类**

```java
package com.mall.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("refund_application")
public class RefundApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long userId;
    private String reason;
    private BigDecimal amount;
    private String status; // PENDING, APPROVED, REJECTED
    private String adminReply;
    private LocalDateTime handledTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private String receiverName;
}
```

- [ ] **创建 Mapper 接口**

```java
package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.entity.RefundApplication;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RefundApplicationMapper extends BaseMapper<RefundApplication> {
}
```

- [ ] **提交**

```bash
git add backend/order-service/src/main/java/com/mall/order/entity/RefundApplication.java backend/order-service/src/main/java/com/mall/order/mapper/RefundApplicationMapper.java
git commit -m "feat: 新增 RefundApplication 实体和 Mapper"
```

---

### Task 3: 后端 — RefundService 业务逻辑

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/service/RefundService.java`

- [ ] **创建 RefundService**

```java
package com.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.order.entity.Order;
import com.mall.order.entity.RefundApplication;
import com.mall.order.mapper.OrderMapper;
import com.mall.order.mapper.RefundApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundApplicationMapper refundApplicationMapper;
    private final OrderMapper orderMapper;

    @Transactional
    public RefundApplication create(Long orderId, Long userId, String reason) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("Order not found");
        }
        if (order.getStatus() != 2 && order.getStatus() != 3) {
            throw new RuntimeException("Order cannot be refunded");
        }

        // Create refund application
        RefundApplication app = new RefundApplication();
        app.setOrderId(orderId);
        app.setUserId(userId);
        app.setReason(reason);
        app.setAmount(order.getTotalAmount());
        app.setStatus("PENDING");
        app.setCreatedTime(LocalDateTime.now());
        app.setUpdatedTime(LocalDateTime.now());
        refundApplicationMapper.insert(app);

        // Update order status to refunding
        order.setStatus(5);
        order.setUpdatedTime(LocalDateTime.now());
        orderMapper.updateById(order);

        return app;
    }

    public IPage<RefundApplication> list(String status, int page, int size) {
        LambdaQueryWrapper<RefundApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(RefundApplication::getStatus, status);
        }
        wrapper.orderByDesc(RefundApplication::getCreatedTime);
        IPage<RefundApplication> appPage = refundApplicationMapper.selectPage(new Page<>(page, size), wrapper);

        // Populate order info
        for (RefundApplication app : appPage.getRecords()) {
            Order order = orderMapper.selectById(app.getOrderId());
            if (order != null) {
                app.setOrderNo(order.getOrderNo());
                app.setReceiverName(order.getReceiverName());
            }
        }

        return appPage;
    }

    @Transactional
    public void approve(Long id, String adminReply, Long adminId) {
        RefundApplication app = refundApplicationMapper.selectById(id);
        if (app == null || !"PENDING".equals(app.getStatus())) {
            throw new RuntimeException("Refund application not found or already processed");
        }

        app.setStatus("APPROVED");
        app.setAdminReply(adminReply);
        app.setHandledTime(LocalDateTime.now());
        app.setUpdatedTime(LocalDateTime.now());
        refundApplicationMapper.updateById(app);

        // Set order to cancelled (refunded)
        Order order = orderMapper.selectById(app.getOrderId());
        if (order != null) {
            order.setStatus(4);
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }

    @Transactional
    public void reject(Long id, String adminReply, Long adminId) {
        RefundApplication app = refundApplicationMapper.selectById(id);
        if (app == null || !"PENDING".equals(app.getStatus())) {
            throw new RuntimeException("Refund application not found or already processed");
        }

        app.setStatus("REJECTED");
        app.setAdminReply(adminReply);
        app.setHandledTime(LocalDateTime.now());
        app.setUpdatedTime(LocalDateTime.now());
        refundApplicationMapper.updateById(app);

        // Restore order to completed
        Order order = orderMapper.selectById(app.getOrderId());
        if (order != null) {
            order.setStatus(3);
            order.setUpdatedTime(LocalDateTime.now());
            orderMapper.updateById(order);
        }
    }
}
```

- [ ] **提交**

```bash
git add backend/order-service/src/main/java/com/mall/order/service/RefundService.java
git commit -m "feat: 新增 RefundService 退款业务逻辑"
```

---

### Task 4: 后端 — RefundAdminController 管理端接口

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/controller/RefundAdminController.java`

- [ ] **创建 RefundAdminController**

```java
package com.mall.order.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mall.order.common.Result;
import com.mall.order.entity.RefundApplication;
import com.mall.order.service.RefundService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order/admin/refund")
@RequiredArgsConstructor
public class RefundAdminController {

    private final RefundService refundService;

    @GetMapping("/list")
    public Result<IPage<RefundApplication>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(refundService.list(status, page, size));
    }

    @PostMapping("/approve")
    public Result<Void> approve(@RequestBody RefundActionRequest request) {
        refundService.approve(request.getId(), request.getAdminReply(), 0L);
        return Result.success(null);
    }

    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody RefundActionRequest request) {
        refundService.reject(request.getId(), request.getAdminReply(), 0L);
        return Result.success(null);
    }

    @Data
    public static class RefundActionRequest {
        private Long id;
        private String adminReply;
    }
}
```

- [ ] **提交**

```bash
git add backend/order-service/src/main/java/com/mall/order/controller/RefundAdminController.java
git commit -m "feat: 新增管理端退款审批接口"
```

---

### Task 5: 后端 — 用户申请退款接口增加 reason 参数

**Files:**
- Modify: `backend/order-service/src/main/java/com/mall/order/controller/OrderController.java`
- Modify: `backend/order-service/src/main/java/com/mall/order/service/OrderService.java`

- [ ] **修改 OrderController.refund 方法接收 reason 参数**

将：
```java
@PostMapping("/refund/{id}")
public Result<Void> refund(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    Long userId = Long.valueOf(jwt.getSubject());
    orderService.applyRefund(id, userId);
    return Result.success(null);
}
```

改为：
```java
@PostMapping("/refund/{id}")
public Result<Void> refund(@PathVariable Long id,
                           @RequestParam String reason,
                           @AuthenticationPrincipal Jwt jwt) {
    Long userId = Long.valueOf(jwt.getSubject());
    orderService.applyRefund(id, userId, reason);
    return Result.success(null);
}
```

- [ ] **修改 OrderService**

注入 `RefundService` 并修改 `applyRefund` 方法：

在 `OrderService` 顶部增加注入：
```java
private final RefundService refundService;
```

修改 `applyRefund` 方法：
```java
@Transactional
public void applyRefund(Long orderId, Long userId, String reason) {
    refundService.create(orderId, userId, reason);
}
```

- [ ] **提交**

```bash
git add backend/order-service/src/main/java/com/mall/order/controller/OrderController.java backend/order-service/src/main/java/com/mall/order/service/OrderService.java
git commit -m "feat: 用户退款接口增加 reason 参数并关联 RefundService"
```

---

### Task 6: 管理端前端 — API 层

**Files:**
- Create: `admin-frontend/src/api/refund.ts`

- [ ] **创建 refund.ts**

```typescript
import request from './request'

export interface RefundApplication {
  id: number
  orderId: number
  userId: number
  reason: string
  amount: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  adminReply: string | null
  handledTime: string | null
  createdTime: string
  updatedTime: string
  orderNo: string
  receiverName: string
}

export function getRefundList(params: {
  status?: string
  page?: number
  size?: number
}) {
  return request.get<any, import('./product').PageResult<RefundApplication>>('/order/admin/refund/list', { params })
}

export function approveRefund(id: number, adminReply?: string) {
  return request.post<void>('/order/admin/refund/approve', { id, adminReply })
}

export function rejectRefund(id: number, adminReply?: string) {
  return request.post<void>('/order/admin/refund/reject', { id, adminReply })
}
```

- [ ] **提交**

```bash
git add admin-frontend/src/api/refund.ts
git commit -m "feat: 管理端退款 API 层"
```

---

### Task 7: 管理端前端 — 退款管理页面

**Files:**
- Create: `admin-frontend/src/views/RefundManageView.vue`

- [ ] **创建 RefundManageView.vue**

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRefundList, approveRefund, rejectRefund } from '@/api/refund'
import type { RefundApplication } from '@/api/refund'

const list = ref<RefundApplication[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({ status: '' as string | undefined, page: 1, size: 20 })
const actionDialogVisible = ref(false)
const actionTarget = ref<RefundApplication | null>(null)
const actionType = ref<'approve' | 'reject'>('approve')
const actionForm = ref({ adminReply: '' })

const statusMap: Record<string, { type: string; text: string }> = {
  PENDING: { type: 'warning', text: '待处理' },
  APPROVED: { type: 'success', text: '已通过' },
  REJECTED: { type: 'danger', text: '已拒绝' }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.status) params.status = query.value.status
    const res = await getRefundList(params)
    list.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

function handleSearch() { query.value.page = 1; loadData() }

function openActionDialog(row: RefundApplication, type: 'approve' | 'reject') {
  actionTarget.value = row
  actionType.value = type
  actionForm.value = { adminReply: '' }
  actionDialogVisible.value = true
}

async function handleAction() {
  if (!actionTarget.value) return
  const target = actionTarget.value
  try {
    if (actionType.value === 'approve') {
      await ElMessageBox.confirm(`确定同意订单 ${target.orderNo} 的退款申请？`, '确认操作')
      await approveRefund(target.id, actionForm.value.adminReply)
      ElMessage.success('退款已通过')
    } else {
      if (!actionForm.value.adminReply) {
        ElMessage.warning('拒绝退款请填写理由')
        return
      }
      await ElMessageBox.confirm(`确定拒绝订单 ${target.orderNo} 的退款申请？`, '确认操作')
      await rejectRefund(target.id, actionForm.value.adminReply)
      ElMessage.success('已拒绝退款')
    }
    actionDialogVisible.value = false
    await loadData()
  } catch {}
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header"><h2>退款管理</h2></div>
    <el-card>
      <div class="search-bar">
        <el-select v-model="query.status" placeholder="退款状态" clearable class="search-select" @change="handleSearch">
          <el-option label="全部" :value="undefined" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="receiverName" label="收货人" width="100" />
        <el-table-column label="退款金额" width="100"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="adminReply" label="管理员回复" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" @click="openActionDialog(row, 'approve')">通过</el-button>
              <el-button size="small" type="danger" @click="openActionDialog(row, 'reject')">拒绝</el-button>
            </template>
            <el-tag v-else size="small" :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="prev, pager, next, total" @current-change="handleSearch" />
      </div>
    </el-card>

    <el-dialog v-model="actionDialogVisible" :title="actionType === 'approve' ? '通过退款' : '拒绝退款'" width="450px">
      <el-form label-width="80px">
        <el-form-item label="订单号">
          <span>{{ actionTarget?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款原因">
          <span>{{ actionTarget?.reason }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span>¥{{ actionTarget?.amount }}</span>
        </el-form-item>
        <el-form-item :label="actionType === 'approve' ? '回复说明' : '拒绝理由'" required>
          <el-input v-model="actionForm.adminReply" type="textarea" :rows="3" :placeholder="actionType === 'reject' ? '请填写拒绝理由（必填）' : '选填回复说明'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAction">{{ actionType === 'approve' ? '确认通过' : '确认拒绝' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.search-select { width: 160px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
```

- [ ] **提交**

```bash
git add admin-frontend/src/views/RefundManageView.vue
git commit -m "feat: 新增管理端退款管理页面"
```

---

### Task 8: 管理端前端 — 路由和侧边栏菜单

**Files:**
- Modify: `admin-frontend/src/router/index.ts`
- Modify: `admin-frontend/src/layouts/AdminLayout.vue`

- [ ] **添加路由**

在 `admin-frontend/src/router/index.ts` 的 `children` 数组中 `orders` 路由之后追加：

```typescript
{
  path: 'refunds',
  name: 'RefundManage',
  component: () => import('@/views/RefundManageView.vue'),
  meta: { title: '退款管理' }
},
```

- [ ] **添加侧边栏菜单**

在 `admin-frontend/src/layouts/AdminLayout.vue` 的 `menuItems` 数组中 `{ path: '/orders', ... }` 之后追加：

```typescript
{ path: '/refunds', icon: 'Coin', label: '退款管理' },
```

- [ ] **提交**

```bash
git add admin-frontend/src/router/index.ts admin-frontend/src/layouts/AdminLayout.vue
git commit -m "feat: 新增退款管理路由和菜单"
```

---

### Task 9: 管理端前端 — 订单管理页增加退款跳转链接

**Files:**
- Modify: `admin-frontend/src/views/OrderListView.vue`

- [ ] **在订单管理页的退款中订单操作区增加跳转链接**

找到 `admin-frontend/src/views/OrderListView.vue` 中 status === 5 的按钮区块：

```html
<template v-if="row.status === 5">
  <el-button size="small" type="danger" @click="handleUpdateStatus(row, 4, '已退款')">同意退款</el-button>
  <el-button size="small" @click="handleUpdateStatus(row, 3, '已完成')">拒绝退款</el-button>
</template>
```

改为：

```html
<template v-if="row.status === 5">
  <el-button size="small" type="primary" @click="$router.push('/refunds')">查看退款</el-button>
</template>
```

在 `<script>` 中确认有 `useRouter` 引入，若没有则添加。当前模板中使用了 `$router`，Vue 模板中可通过 `$router` 直接访问。

- [ ] **提交**

```bash
git add admin-frontend/src/views/OrderListView.vue
git commit -m "feat: 订单管理页增加退款跳转链接"
```

---

### Task 10: 客户端前端 — API 增加 reason 参数

**Files:**
- Modify: `frontend/src/api/order.ts`

- [ ] **修改 applyRefund 函数**

将：
```typescript
export function applyRefund(id: number) {
  return request.post<void>('/order/refund/' + id)
}
```

改为：
```typescript
export function applyRefund(id: number, reason: string) {
  return request.post<void>('/order/refund/' + id + '?reason=' + encodeURIComponent(reason))
}
```

- [ ] **提交**

```bash
git add frontend/src/api/order.ts
git commit -m "feat: 客户端退款API增加 reason 参数"
```

---

### Task 11: 客户端前端 — 订单详情页退款表单

**Files:**
- Modify: `frontend/src/views/OrderDetailView.vue`
- Modify: `frontend/src/views/OrderListView.vue`

- [ ] **修改 OrderDetailView.vue 退款逻辑**

将简单的 confirm 改为弹窗表单：

替换 `handleRefund` 函数：
```typescript
import { ref } from 'vue'
// 页面顶部已有 import

const refundDialogVisible = ref(false)
const refundReason = ref('')
const refundSubmitting = ref(false)

async function handleRefund() {
  if (!order.value) return
  refundReason.value = ''
  refundDialogVisible.value = true
}

async function submitRefund() {
  if (!order.value || !refundReason.value.trim()) return
  refundSubmitting.value = true
  try {
    await applyRefund(order.value.id, refundReason.value.trim())
    order.value.status = 5
    refundDialogVisible.value = false
    showToast('退款申请已提交', 'success')
  } catch { /* handled */ }
  finally { refundSubmitting.value = false }
}
```

在模板中找到申请退款的元素，改为触发 `handleRefund`（已经是）。

在模板末尾（`</template>` 之前）添加退款弹窗：

```html
<!-- Refund Dialog -->
<el-dialog v-model="refundDialogVisible" title="申请退款" width="420px">
  <el-form label-width="70px">
    <el-form-item label="订单金额">
      <span style="font-size:16px;font-weight:600;color:#ff6b35">¥{{ order?.totalAmount }}</span>
    </el-form-item>
    <el-form-item label="退款原因" required>
      <el-input v-model="refundReason" type="textarea" :rows="4" placeholder="请详细描述退款原因，这将帮助商家更快处理您的申请" maxlength="500" show-word-limit />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="refundDialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">提交申请</el-button>
  </template>
</el-dialog>
```

还需要更新状态显示部分。找到退款横幅区域（当前显示"退款申请已提交，等待商家处理"），改为根据退款申请状态动态显示：

```html
<div v-if="order.status === 5" class="order-detail__refund-banner">
  <span class="order-detail__refund-dot"></span>
  <span>退款申请已提交，等待商家处理</span>
</div>
```

由于订单详情页没有加载退款申请记录的能力，这个横幅暂时保持现有文案。更详细的退款状态显示可以在后续优化中补充。

- [ ] **修改 OrderListView.vue 客户端列表页的退款确认**

将：
```typescript
async function handleRefund(id: number) {
  if (!window.confirm('确定要申请退款吗？')) return
  try {
    await applyRefund(id)
    loadOrders()
  } catch { /* handled */ }
}
```

改为弹出表单：

```typescript
import { ElMessageBox, ElMessage } from 'element-plus'

async function handleRefund(id: number) {
  try {
    const { value } = await ElMessageBox.prompt('请填写退款原因', '申请退款', {
      inputType: 'textarea',
      inputPlaceholder: '请详细描述退款原因',
      inputValidator: (v: string) => !!v.trim() || '退款原因不能为空',
      confirmButtonText: '提交申请',
      cancelButtonText: '取消'
    })
    await applyRefund(id, value.trim())
    ElMessage.success('退款申请已提交')
    loadOrders()
  } catch { /* handled (cancel or error) */ }
}
```

注意需要添加 `import { ElMessageBox, ElMessage } from 'element-plus'`，并移除原来的 `window.confirm`。

- [ ] **提交**

```bash
git add frontend/src/views/OrderDetailView.vue frontend/src/views/OrderListView.vue
git commit -m "feat: 客户端退款申请改用表单填写原因"
```
