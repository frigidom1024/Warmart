# 退款系统设计文档

日期: 2026-06-02

## 概述

为商城系统增加完善的退款申请流程，支持用户提交退款申请、管理员审核处理，并记录完整的退款档案。

## 当前问题

- 退款仅通过 `order.status = 5` 一个字段标记，无独立退款记录
- 用户申请退款时无法填写退款原因
- 管理员操作后无历史记录留底
- 退款流程仅"申请→处理"两步，无法追踪处理进度

## 数据库设计

新建 `refund_application` 表：

```sql
CREATE TABLE refund_application (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT NOT NULL,         -- 关联订单
    user_id      BIGINT NOT NULL,         -- 申请用户
    reason       VARCHAR(500) NOT NULL,   -- 退款原因（用户填写）
    amount       DECIMAL(10,2) NOT NULL,  -- 退款金额（默认订单全额）
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                 -- PENDING=待处理 | APPROVED=已通过 | REJECTED=已拒绝
    admin_reply  VARCHAR(500),            -- 管理员回复/拒绝理由
    handled_time DATETIME,                -- 处理时间
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 退款全流程

```
用户申请退款 → 填写退款原因 → 创建退款申请(status=PENDING)
                                    ↓
                         管理员在退款管理页查看
                                    ↓
                     ┌────────────────┴────────────────┐
                     ↓                                 ↓
          同意退款(APPROVED)                   拒绝退款(REJECTED)
          订单状态→已取消(4)                   订单状态→已完成(3)
          需填写回复说明                       需填写拒绝理由
```

## 状态及流转规则

### 退款申请状态

| 状态值 | 含义 | 说明 |
|--------|------|------|
| PENDING | 待处理 | 用户已提交，管理员尚未处理 |
| APPROVED | 已通过 | 管理员同意退款，订单标记为已取消 |
| REJECTED | 已拒绝 | 管理员驳回申请，订单恢复已完成 |

### 订单状态流转

| 当前订单状态 | 操作 | 目标订单状态 |
|-------------|------|-------------|
| 待收货(2) / 已完成(3) | 用户申请退款 | 退款中(5)，创建退款申请记录(PENDING) |
| 退款中(5) | 管理员同意退款 | 已取消(4) |
| 退款中(5) | 管理员拒绝退款 | 已完成(3) |

## 后端改动

### 新增文件

| 文件 | 说明 |
|------|------|
| `entity/RefundApplication.java` | 退款申请实体类 |
| `mapper/RefundApplicationMapper.java` | MyBatis-Plus Mapper |
| `service/RefundService.java` | 退款业务逻辑 |
| `controller/RefundAdminController.java` | 管理员退款管理接口 |

### API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/order/admin/refund/list` | 分页查询退款申请列表，支持按状态筛选 |
| POST | `/api/order/admin/refund/approve` | 同意退款（参数：id, adminReply） |
| POST | `/api/order/admin/refund/reject` | 拒绝退款（参数：id, adminReply） |

### 用户端现有接口不变

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/order/refund/{id}` | 用户申请退款（增加 reason 参数） |

## 前端改动

### 新增管理端页面

`admin-frontend/src/views/RefundManageView.vue`

- 独立的退款管理页面
- 列表展示所有退款申请，支持按状态(PENDING/APPROVED/REJECTED)筛选
- 每行显示：订单号、用户、退款金额、退款原因、申请时间、状态
- 点击展开或弹窗查看订单详情和退款详情
- PENDING 状态的申请显示"通过"和"拒绝"操作按钮
- 通过/拒绝时弹出对话框填写管理员回复

### 路由和菜单

- 新增路由 `/refunds` → `RefundManageView.vue`
- 侧边栏新增"退款管理"菜单项（位于"订单管理"下方）
- 订单管理页：退款中的订单增加"查看退款"快捷跳转链接

### 用户端改动

- 申请退款时弹出表单填写退款原因（必填）
- 退款横幅增加状态文字：“待处理”/“已通过”/“已拒绝”
- 退款被拒绝时显示管理员回复

## 涉及文件清单

### 新建文件
1. `deploy/mysql/init.sql` — 追加 refund_application 建表语句
2. `backend/order-service/.../entity/RefundApplication.java`
3. `backend/order-service/.../mapper/RefundApplicationMapper.java`
4. `backend/order-service/.../service/RefundService.java`
5. `backend/order-service/.../controller/RefundAdminController.java`
6. `admin-frontend/src/views/RefundManageView.vue`
7. `admin-frontend/src/api/refund.ts`

### 修改文件
1. `admin-frontend/src/layouts/AdminLayout.vue` — 新增菜单项
2. `admin-frontend/src/router/index.ts` — 新增路由
3. `admin-frontend/src/views/OrderListView.vue` — 增加快捷跳转
4. `frontend/src/api/order.ts` — applyRefund 增加 reason 参数
5. `frontend/src/views/OrderDetailView.vue` — 退款表单和状态展示
6. `frontend/src/views/OrderListView.vue` — 退款申请弹窗
7. `backend/order-service/.../controller/OrderController.java` — refund 接口接收 reason
8. `backend/order-service/.../service/OrderService.java` — 关联 RefundService
