# 物流跟踪功能设计文档

日期: 2026-06-02

## 概述

为商城系统增加物流轨迹跟踪功能，在订单详情页嵌入物流时间线，支持管理员录入物流进度节点。

## 数据库设计

新建 `logistics_track` 表：

```sql
CREATE TABLE logistics_track (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    message VARCHAR(500),
    location VARCHAR(200),
    track_time DATETIME NOT NULL,
    created_time DATETIME NOT NULL,
    INDEX idx_order_id (order_id),
    INDEX idx_track_time (track_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### status 枚举值

| 值 | 含义 | 显示颜色 |
|----|------|---------|
| PICKED_UP | 已揽件 | 橙色 |
| IN_TRANSIT | 运输中 | 蓝色 |
| DELIVERING | 派送中 | 蓝色 |
| DELIVERED | 已签收 | 绿色 |
| FAILED | 配送异常 | 红色 |

## 后端 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/order/logistics/tracks/{orderId}` | 获取订单物流轨迹列表 |
| POST | `/api/order/admin/logistics/track` | 管理员添加物流轨迹节点 |

### GET /api/order/logistics/tracks/{orderId}

返回该订单的所有物流轨迹，按 `track_time` 倒序排列。

### POST /api/order/admin/logistics/track

请求体：
```json
{
  "orderId": 1,
  "status": "IN_TRANSIT",
  "message": "快件已到达北京分拨中心",
  "location": "北京市通州区",
  "trackTime": "2026-06-02T14:00:00"
}
```

发货时由后端自动生成第一条 "已揽件" 记录。

## 前端改动

### OrderDetailView.vue

在现有的物流信息卡片下方，新增 **物流轨迹时间线**：

```
┌─ 物流信息 ─────────────────────┐
│  物流公司：顺丰速运            │
│  运单编号：SF1234567890 [复制]  │
│  发货时间：2026-06-02 10:00    │
├────────────────────────────────┤
│  🟢 已签收                     │
│     您的快件已签收              │
│     2026-06-04 15:30           │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│  🔵 派送中                     │
│     快件已到达北京市朝阳区      │
│     2026-06-04 08:00           │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│  🔵 运输中                     │
│     快件已到达北京分拨中心      │
│     2026-06-03 22:00           │
│  ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─  │
│  🟠 已揽件                     │
│     您的快件已被快递员揽收      │
│     2026-06-02 14:00           │
└────────────────────────────────┘
```

- 无轨迹数据时显示 "暂无物流跟踪信息"
- 时间线样式使用纯 CSS 实现（竖线 + 圆点 + 内容区）

### 管理端

在发货成功后弹窗底部增加 "新增物流轨迹" 的入口，或在订单详情中追加物流进度。

## 新增文件

1. `deploy/mysql/init.sql` — 追加 logistics_track 建表语句
2. `backend/order-service/.../entity/LogisticsTrack.java`
3. `backend/order-service/.../mapper/LogisticsTrackMapper.java`
4. `backend/order-service/.../service/LogisticsService.java`
5. `backend/order-service/.../controller/LogisticsController.java`
6. `frontend/src/api/logistics.ts`
7. `admin-frontend/src/api/logistics.ts`

## 修改文件

1. `backend/order-service/.../service/OrderService.java` — 发货时自动生成揽件记录
2. `frontend/src/views/OrderDetailView.vue` — 嵌入物流时间线
3. `frontend/src/api/order.ts` — 添加 getLogisticsTracks 引用
4. `admin-frontend/src/views/OrderListView.vue` — 发货弹窗增加轨迹录入
