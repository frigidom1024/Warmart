# 物流跟踪功能设计文档

日期: 2026-06-02

## 概述

为商城系统增加物流轨迹跟踪功能。订单详情页展示最新几条进度，另设独立的物流详情页展示完整轨迹时间线。

## 物流状态流程

```
已下单 → 仓库处理中 → 运输中 → 待取件 → 已签收
                        ↓
                  （含多个运输节点列表）
```

| 状态 | 含义 | 说明 |
|------|------|------|
| ORDERED | 已下单 | 订单创建后自动生成 |
| WAREHOUSE | 仓库处理中 | 商品打包中，等待快递揽收 |
| IN_TRANSIT | 运输中 | 快件在运输途中，可包含多个中转节点 |
| PICKUP | 待取件 | 快件到达自提点/驿站，等待取件 |
| DELIVERED | 已签收 | 用户已签收 |

## 数据库设计

新建 `logistics_track` 表：

```sql
CREATE TABLE logistics_track (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
         -- ORDERED | WAREHOUSE | IN_TRANSIT | PICKUP | DELIVERED
    message VARCHAR(500),
         -- 描述文字，如"快件已到达北京分拨中心"
    location VARCHAR(200),
         -- 地点，如"北京市通州区"
    track_time DATETIME NOT NULL,
         -- 物流事件发生时间
    created_time DATETIME NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_track_time (track_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

运输中(IN_TRANSIT)可以有多条记录，表示不同的中转节点。

## 后端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/order/logistics/tracks/{orderId}` | 获取订单物流轨迹（全部，按track_time倒序） |
| POST | `/api/order/admin/logistics/track` | 管理员添加物流轨迹节点 |

发货时后端自动生成 ORDERED → WAREHOUSE 两条初始记录。

## 前端页面

### 1. 订单详情页（精简版）

在现有物流信息卡片下方，展示**最近 3-4 条**进度的精简时间线：

```
┌─ 物流跟踪 ────────────────────┐
│  🟢 已签收                     │
│     您的快件已签收  06-04 15:30│
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│  🟡 待取件                     │
│     已到达XX驿站  06-04 08:00  │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│  🔵 运输中                     │
│     已到达北京分拨中心  06-03  │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│    查看完整物流 →              │
└────────────────────────────────┘
```

点击"查看完整物流"跳转到 `/logistics/:orderId`。

### 2. 物流详情页（完整版）

独立页面 `frontend/src/views/LogisticsView.vue`，路由 `/logistics/:orderId`。

- 顶部：物流公司、运单号（可复制）
- 完整时间线：

```
已签收 🟢
  您的快件已签收
  2026-06-04 15:30
────────────
待取件 🟡
  快件已到达XX驿站，请凭取件码取件
  2026-06-04 08:00
────────────
运输中 🔵
  ├ 已到达北京朝阳区站点    2026-06-03 22:00
  ├ 已到达北京分拨中心      2026-06-03 14:00
  └ 已从杭州发出            2026-06-02 18:00
────────────
仓库处理中 ⚪
  商品已打包完成，等待快递揽收
  2026-06-02 14:00
────────────
已下单 ⚪
  订单已确认
  2026-06-02 10:00
```

- 运输中的多个节点以缩进列表形式展示在同一阶段下
- 时间线从最新到最旧排列

## 新增文件

1. `deploy/mysql/init.sql` — 追加 logistics_track 建表语句
2. `backend/order-service/.../entity/LogisticsTrack.java`
3. `backend/order-service/.../mapper/LogisticsTrackMapper.java`
4. `backend/order-service/.../service/LogisticsService.java`
5. `backend/order-service/.../controller/LogisticsController.java`
6. `frontend/src/api/logistics.ts`
7. `frontend/src/views/LogisticsView.vue`

## 修改文件

1. `backend/order-service/.../service/OrderService.java` — 发货时自动生成初始记录
2. `frontend/src/views/OrderDetailView.vue` — 嵌入精简物流时间线，查看完整物流按钮
3. `admin-frontend/src/views/OrderListView.vue` — 发货弹窗增加轨迹录入
4. `frontend/src/router/index.ts` — 新增 /logistics/:id 路由
