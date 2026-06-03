# 管理员仪表盘增强设计

## 概述

增强管理员控制台（Dashboard）页面，在现有基础上增加核心数据统计指标和可视化图表，提供更全面的运营数据概览。

## 当前状态

现有控制台页面位于 `admin-frontend/src/views/DashboardView.vue`，包含：
- 4 个统计卡片：今日订单、成交金额、商品数量、待处理
- 最近订单表格（5 条）
- 所有数据通过前端调用现有 API 实时计算

## 改造目标

### 1. 核心数据统计（5 个指标卡片）

| 指标 | 数据来源 | 说明 |
|------|----------|------|
| 总用户数 | auth-service | 注册用户总量 |
| 总订单数 | order-service | 所有订单总量 |
| 总销售额 | order-service | 已完成订单金额总和 |
| 今日订单 | order-service | 今日创建订单数 |
| 今日销售额 | order-service | 今日已完成订单金额总和 |

### 2. 数据图表

| 图表 | 类型 | 说明 |
|------|------|------|
| 销量趋势 | 折线图（ECharts） | 近 7 天每日订单数 |
| 热销商品排行 | 横向柱状图（ECharts） | 按销量排序前 10 商品（含销售额） |
| 订单状态统计 | 饼图/环形图（ECharts） | 各状态订单数量分布 |

### 3. 保留现有功能

- 最近订单表格
- 页面布局风格保持一致

## 架构设计

### 后端新增 API

#### order-service

在 `OrderController` 中新增管理端仪表盘统计接口：

```
GET /api/order/admin/stats
```

返回数据结构：

```json
{
  "code": 200,
  "data": {
    "totalOrders": 1000,
    "totalSales": 285000.00,
    "todayOrders": 45,
    "todaySales": 12345.00,
    "orderStatusStats": [
      { "status": 0, "count": 120 },
      { "status": 1, "count": 85 },
      { "status": 2, "count": 60 },
      { "status": 3, "count": 650 },
      { "status": 4, "count": 50 },
      { "status": 5, "count": 35 }
    ],
    "salesTrend": [
      { "date": "2026-05-27", "orderCount": 12, "salesAmount": 3600.00 },
      { "date": "2026-05-28", "orderCount": 18, "salesAmount": 5200.00 },
      ...
    ],
    "hotProducts": [
      { "productName": "iPhone 15", "sales": 328, "amount": 987000.00 },
      ...
    ]
  }
}
```

实现方式：在 `OrderService` 中通过 MyBatis Plus 查询和聚合，hotProducts 通过 `OrderItemMapper` 按 productId 分组统计。

#### auth-service

在 `AdminUserController` 中新增用户计数接口：

```
GET /api/auth/admin/users/count
```

返回：`{ "code": 200, "data": 12345 }`

实现方式：`userMapper.selectCount(null)`

### 数据流

```
DashboardView.vue
  → OrderService.getAdminDashboardStats()  → GET /api/order/admin/stats
  → UserService.getAdminUserCount()        → GET /api/auth/admin/users/count
  → 渲染统计数据卡片、ECharts 图表、最近订单表格
```

### 前端组件结构

```
DashboardView.vue
├── StatCard × 5（总用户数、总订单数、总销售额、今日订单、今日销售额）
├── ECharts: SalesTrendChart（销量趋势折线图）
├── ECharts: OrderStatusChart（订单状态环形图）
├── ECharts: HotProductsChart（热销商品柱状图）
└── RecentOrdersTable（最近订单表格，保留现有）
```

## 技术选型

- **图表库**: ECharts（`echarts` + `vue-echarts`）
- **API 层**: 在 `admin-frontend/src/api/` 中新增统计接口封装
- **样式**: 保持现有 Element Plus 主题风格，scoped CSS

## 实现步骤

1. **后端 order-service**: 新增 dashboard stats API 和 Service 方法
2. **后端 auth-service**: 新增用户计数 API
3. **前端依赖**: 安装 echarts / vue-echarts
4. **前端 API 层**: 新增统计接口封装
5. **前端 DashboardView**: 重构页面，整合新指标和图表
