# 网上商城系统 - 设计文档

## 1. 概述

基于 Spring Cloud 微服务架构和 VUE3 前端技术栈，开发一套功能完整的网上商城系统。本系统覆盖商品浏览、购物车、订单管理、用户中心等客户侧功能，以及商品管理、订单管理、用户管理等后台管理功能，并扩展了缓存优化、服务拆分、容器化部署及 CI/CD 持续集成能力。

## 2. 项目结构

### 仓库布局 (Monorepo)

```
webwork/
├── frontend/                      # VUE3 前端
│   ├── client/                   # 客户前端（商城用户使用）
│   │   ├── src/
│   │   │   ├── views/
│   │   │   │   ├── home/        # 首页（轮播图、推荐商品、公告）
│   │   │   │   ├── auth/        # 登录、注册、找回密码
│   │   │   │   ├── product/     # 商品列表、商品详情、评论、咨询
│   │   │   │   ├── cart/        # 购物车
│   │   │   │   ├── order/       # 订单确认、支付、我的订单、售后
│   │   │   │   ├── user/        # 个人中心、地址管理、收藏、反馈
│   │   │   │   └── service/     # 客服咨询、通知列表
│   │   │   ├── router/          # vue-router 路由配置
│   │   │   ├── stores/          # Pinia 状态管理
│   │   │   ├── api/             # Axios 请求封装
│   │   │   └── components/      # 通用组件
│   │   └── package.json
│   │
│   └── admin/                    # 管理后台（管理员使用）
│       └── src/
│           ├── views/
│           │   ├── dashboard/   # 数据统计看板
│           │   ├── user/        # 用户管理
│           │   ├── product/     # 商品管理、分类管理、评论管理
│           │   ├── order/       # 订单管理、售后处理
│           │   ├── content/     # 轮播管理、公告管理
│           │   └── system/      # 反馈管理、咨询管理、个人信息
│           ├── router/
│           ├── stores/
│           ├── api/
│           ├── layouts/         # 后台布局（侧边栏+顶栏）
│           └── components/      # 通用组件
│
├── backend/
│   ├── gateway/                  # Spring Cloud Gateway
│   │   ├── src/main/java/.../gateway/
│   │   │   ├── config/         # CORS、路由配置
│   │   │   └── filter/         # OAuth2 资源服务器过滤器
│   │   └── pom.xml
│   │
│   ├── auth-service/             # 认证服务 (Spring Authorization Server)
│   │   ├── src/main/java/.../auth/
│   │   │   ├── controller/     # OAuth2 端点
│   │   │   ├── service/        # OAuth2 业务逻辑
│   │   │   ├── config/         # Authorization Server 配置
│   │   │   │   ├── AuthorizationServerConfig  # OAuth2 服务器
│   │   │   │   ├── SecurityConfig             # Spring Security
│   │   │   │   └── RegisteredClientConfig     # 客户端注册
│   │   │   └── mapper/         # 用户查询
│   │   └── pom.xml
│   │
│   ├── user-service/             # 用户服务
│   │   ├── src/main/java/.../user/
│   │   │   ├── controller/     # 地址管理、个人信息 API
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── src/main/resources/
│   │   │   └── application.yml
│   │   └── pom.xml
│   │
│   ├── product-service/          # 商品服务
│   │   ├── src/main/java/.../product/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── mapper/
│   │   │   └── entity/
│   │   └── pom.xml
│   │
│   └── order-service/            # 订单服务
│       ├── src/main/java/.../order/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── mapper/
│       │   └── entity/
│       └── pom.xml
│
├── deploy/                       # 部署配置
│   ├── docker-compose.yml       # 全服务编排
│   ├── mysql/                   # 初始化 SQL 脚本
│   └── nginx/                   # Nginx 配置
│
└── .github/workflows/           # GitHub Actions CI/CD
    └── ci.yml
```

## 3. 技术栈

| 层面 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + Element Plus | Vue 3.4+ |
| 前端状态管理 | Pinia | - |
| 前端构建 | Vite | - |
| 后端框架 | Spring Boot 3.x + Spring Cloud | 3.x |
| ORM | MyBatis-Plus | - |
| 注册中心 | Nacos | - |
| 数据库 | MySQL | 8.x |
| 缓存 | Redis | 7.x |
| 认证 | OAuth2 (Spring Authorization Server) | - |
| 部署 | Docker + Docker Compose | - |
| CI/CD | GitHub Actions | - |

## 4. 数据库模型

### 4.1 用户服务 (user_service schema)

**user 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 用户ID |
| username | VARCHAR(50) UNIQUE | 用户名 |
| password | VARCHAR(255) | 密码(bcrypt) |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(255) | 头像URL |
| role | VARCHAR(20) | 角色：USER/ADMIN/SUPER_ADMIN |
| status | TINYINT | 状态：0正常/1禁用 |
| created_time | DATETIME | 创建时间 |
| updated_time | DATETIME | 更新时间 |

**user_address 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 地址ID |
| user_id | BIGINT FK | 用户ID |
| receiver_name | VARCHAR(50) | 收件人 |
| receiver_phone | VARCHAR(20) | 手机号 |
| province | VARCHAR(50) | 省 |
| city | VARCHAR(50) | 市 |
| district | VARCHAR(50) | 区 |
| detail_address | VARCHAR(255) | 详细地址 |
| is_default | TINYINT | 是否默认 |
| created_time | DATETIME | 创建时间 |

**notice 表**（系统公告）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 公告ID |
| title | VARCHAR(100) | 公告标题 |
| content | TEXT | 公告内容 |
| type | VARCHAR(20) | 类型：system/activity |
| status | TINYINT | 状态：0发布/1草稿 |
| created_time | DATETIME | 创建时间 |

**user_feedback 表**（用户反馈）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 反馈ID |
| user_id | BIGINT | 用户ID |
| type | VARCHAR(20) | 类型：complaint/suggestion |
| content | TEXT | 反馈内容 |
| reply | TEXT | 管理员回复 |
| reply_time | DATETIME | 回复时间 |
| status | TINYINT | 状态：0待处理/1已回复 |
| created_time | DATETIME | 创建时间 |

**consultation 表**（客服咨询）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 咨询ID |
| user_id | BIGINT | 用户ID |
| product_id | BIGINT | 商品ID（关联商品） |
| question | TEXT | 咨询内容 |
| answer | TEXT | 客服回复 |
| status | TINYINT | 状态：0待回复/1已回复 |
| created_time | DATETIME | 咨询时间 |

### 4.2 商品服务 (product_service schema)

**banner 表**（首页轮播图）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 轮播ID |
| title | VARCHAR(100) | 标题 |
| image_url | VARCHAR(255) | 轮播图URL |
| link_url | VARCHAR(255) | 跳转链接 |
| sort | INT | 排序 |
| status | TINYINT | 状态：0启用/1停用 |
| created_time | DATETIME | 创建时间 |

**category 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 分类ID |
| name | VARCHAR(50) | 分类名称 |
| parent_id | BIGINT | 父分类ID（0为顶级） |
| sort | INT | 排序 |
| image_url | VARCHAR(255) | 分类图URL |
| status | TINYINT | 状态 |
| created_time | DATETIME | 创建时间 |

**product 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 商品ID |
| category_id | BIGINT FK | 分类ID |
| name | VARCHAR(100) | 商品名称 |
| description | TEXT | 商品描述 |
| price | DECIMAL(10,2) | 价格 |
| stock | INT | 库存 |
| sales | INT | 销量 |
| main_image | VARCHAR(255) | 主图URL |
| status | TINYINT | 状态：0上架/1下架 |
| is_recommend | TINYINT | 是否推荐 |
| has_spec | TINYINT | 是否有规格(颜色/尺寸) |
| created_time | DATETIME | 创建时间 |
| updated_time | DATETIME | 更新时间 |

**product_spec 表**（商品规格：颜色、尺寸等多规格）
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 规格ID |
| product_id | BIGINT FK | 商品ID |
| spec_name | VARCHAR(50) | 规格名（如"颜色"） |
| spec_value | VARCHAR(50) | 规格值（如"红色"） |
| extra_price | DECIMAL(10,2) | 加价金额 |
| stock | INT | 规格独立库存 |
| image | VARCHAR(255) | 规格图片（颜色可配图） |
| sort | INT | 排序 |

**product_image 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 图片ID |
| product_id | BIGINT FK | 商品ID |
| url | VARCHAR(255) | 图片URL |
| sort | INT | 排序 |

**product_comment 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 评论ID |
| product_id | BIGINT FK | 商品ID |
| user_id | BIGINT | 用户ID |
| content | TEXT | 评论内容 |
| rating | TINYINT | 评分(1-5) |
| image_urls | VARCHAR(500) | 评论图片 |
| created_time | DATETIME | 评论时间 |

**user_favorite 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 收藏ID |
| user_id | BIGINT | 用户ID |
| product_id | BIGINT FK | 商品ID |
| created_time | DATETIME | 收藏时间 |

### 4.3 订单服务 (order_service schema)

**cart 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 购物车ID |
| user_id | BIGINT | 用户ID |
| product_id | BIGINT | 商品ID |
| quantity | INT | 数量 |
| checked | TINYINT | 是否选中 |
| created_time | DATETIME | 添加时间 |
| updated_time | DATETIME | 更新时间 |

**order 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 订单ID |
| user_id | BIGINT | 用户ID |
| order_no | VARCHAR(50) UNIQUE | 订单号 |
| total_amount | DECIMAL(10,2) | 总金额 |
| status | TINYINT | 状态：0待支付/1待发货/2待收货/3已完成/4已取消/5售后 |
| payment_method | VARCHAR(20) | 支付方式：余额/微信/支付宝 |
| payment_time | DATETIME | 支付时间 |
| delivery_time | DATETIME | 发货时间 |
| receive_time | DATETIME | 收货时间 |
| receiver_name | VARCHAR(50) | 收件人 |
| receiver_phone | VARCHAR(20) | 收件人手机 |
| receiver_address | VARCHAR(255) | 收货地址 |
| created_time | DATETIME | 创建时间 |
| updated_time | DATETIME | 更新时间 |

**order_item 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 明细ID |
| order_id | BIGINT FK | 订单ID |
| product_id | BIGINT | 商品ID |
| product_name | VARCHAR(100) | 商品名 |
| product_image | VARCHAR(255) | 商品图片 |
| price | DECIMAL(10,2) | 单价 |
| quantity | INT | 数量 |
| subtotal | DECIMAL(10,2) | 小计 |

**payment 表**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 支付记录ID |
| order_id | BIGINT FK | 订单ID |
| order_no | VARCHAR(50) | 订单号 |
| amount | DECIMAL(10,2) | 金额 |
| method | VARCHAR(20) | 支付方式 |
| status | TINYINT | 状态：0待支付/1已支付/2已退款 |
| pay_time | DATETIME | 支付时间 |

## 5. API 设计

### 5.1 路由规则

所有请求通过 Gateway 统一入口 `http://localhost:8080`，Gateway 根据路径前缀路由到对应服务。

### 5.2 认证服务 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录（获取 token） | 否 |
| POST | /api/auth/token | OAuth2 令牌端点 | 否 |
| POST | /api/auth/refresh | 刷新 token | 否 |
| POST | /api/auth/logout | 退出登录（作废 token） | 是 |
| POST | /api/auth/forgot-password | 找回密码 | 否 |

### 5.3 用户服务 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/user/info | 获取用户信息 | 是 |
| PUT | /api/user/info | 修改用户信息 | 是 |
| PUT | /api/user/password | 修改密码 | 是 |
| GET | /api/user/address/list | 地址列表 | 是 |
| POST | /api/user/address/add | 新增地址 | 是 |
| PUT | /api/user/address/update | 修改地址 | 是 |
| DELETE | /api/user/address/delete/{id} | 删除地址 | 是 |
| PUT | /api/user/address/default/{id} | 设为默认 | 是 |
| POST | /api/user/feedback/add | 提交反馈 | 是 |
| GET | /api/user/notice/list | 公告列表（系统/活动） | 否 |

### 5.4 商品服务 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/product/list | 商品分页查询 | 否 |
| GET | /api/product/detail/{id} | 商品详情 | 否 |
| POST | /api/product/search | 商品搜索 | 否 |
| GET | /api/product/category/list | 分类列表 | 否 |
| POST | /api/product/favorite/add | 收藏商品 | 是 |
| DELETE | /api/product/favorite/cancel/{productId} | 取消收藏 | 是 |
| GET | /api/product/favorite/list | 收藏列表 | 是 |
| POST | /api/product/comment/add | 提交评论 | 是 |
| GET | /api/product/comment/list/{productId} | 查看评论 | 否 |
| GET | /api/product/banner/list | 首页轮播图列表 | 否 |
| GET | /api/product/spec/list/{productId} | 商品规格列表 | 否 |
| GET | /api/product/consultation/list/{productId} | 查看商品咨询 | 否 |
| POST | /api/product/consultation/add | 提交商品咨询 | 是 |

### 5.5 订单服务 API

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/order/cart/list | 购物车列表 | 是 |
| POST | /api/order/cart/add | 加入购物车 | 是 |
| PUT | /api/order/cart/update | 修改数量 | 是 |
| DELETE | /api/order/cart/delete/{id} | 删除购物车项 | 是 |
| POST | /api/order/cart/check | 选中/取消选中 | 是 |
| POST | /api/order/cart/checkAll | 全选/取消全选 | 是 |
| POST | /api/order/create | 创建订单 | 是 |
| GET | /api/order/list | 我的订单 | 是 |
| GET | /api/order/detail/{orderNo} | 订单详情 | 是 |
| POST | /api/order/pay | 支付订单 | 是 |
| POST | /api/order/cancel/{orderNo} | 取消订单 | 是 |
| POST | /api/order/confirm/{orderNo} | 确认收货 | 是 |
| POST | /api/order/refund/apply | 申请退款/退货 | 是 |

### 5.6 管理后台 API

| 方法 | 路径 | 说明 | 角色 |
|------|------|------|------|
| GET | /api/admin/dashboard/statistics | 数据统计 | ADMIN |
| GET | /api/admin/user/list | 用户列表 | ADMIN |
| PUT | /api/admin/user/disable/{id} | 禁用用户 | ADMIN |
| POST | /api/admin/product/add | 新增商品 | ADMIN |
| PUT | /api/admin/product/update | 修改商品 | ADMIN |
| DELETE | /api/admin/product/delete/{id} | 删除商品 | ADMIN |
| POST | /api/admin/category/add | 新增分类 | ADMIN |
| GET | /api/admin/order/list | 订单列表 | ADMIN |
| POST | /api/admin/order/deliver | 发货 | ADMIN |
| POST | /api/admin/order/refund | 退款 | ADMIN |
| GET | /api/admin/order/export | 导出订单Excel | ADMIN |
| GET | /api/admin/consultation/list | 咨询列表 | ADMIN |
| POST | /api/admin/consultation/reply | 回复咨询 | ADMIN |
| GET | /api/admin/banner/list | 轮播图列表 | ADMIN |
| POST | /api/admin/banner/add | 新增轮播图 | ADMIN |
| PUT | /api/admin/banner/update | 编辑轮播图 | ADMIN |
| DELETE | /api/admin/banner/delete/{id} | 删除轮播图 | ADMIN |
| GET | /api/admin/notice/list | 公告列表 | ADMIN |
| POST | /api/admin/notice/add | 发布公告 | SUPER_ADMIN |
| PUT | /api/admin/notice/update | 编辑公告 | SUPER_ADMIN |
| DELETE | /api/admin/notice/delete/{id} | 删除公告 | SUPER_ADMIN |
| GET | /api/admin/feedback/list | 反馈列表 | ADMIN |
| POST | /api/admin/feedback/reply | 回复反馈 | ADMIN |

### 5.7 OAuth2 认证流程

采用 Spring Authorization Server 实现 OAuth2 授权框架，使用授权码模式（Authorization Code）配合 PKCE 增强安全性：

**架构角色：**
- **授权服务器（Authorization Server）**：`auth-service`，负责颁发和验证 OAuth2 令牌
- **资源服务器（Resource Server）**：各微服务（user-service、product-service、order-service），通过配置 OAuth2 Resource Server 校验令牌
- **客户端（Client）**：前端应用（client、admin），通过 OAuth2 客户端流程获取令牌

**认证流程：**
```
1. 用户在前端登录页输入凭据
2. 前端发送 POST /api/auth/login 到 auth-service
3. auth-service 验证凭据，返回 access_token + refresh_token
4. 前端将 token 存入 Pinia store 及 localStorage（或 httpOnly cookie）
5. 后续请求：Authorization: Bearer <access_token>
6. Gateway 作为资源服务器校验 token 签名及 scope，将用户信息通过 Header 透传
7. token 过期时，前端使用 refresh_token 自动刷新
8. 各微服务通过 @PreAuthorize 注解和 security.oauth2.resourceserver 配置控制权限
```

**OAuth2 客户端注册（系统预置）：**
| 客户端 | grant_type | scope | 说明 |
|--------|-----------|-------|------|
| mall-client | authorization_code, refresh_token | openid, profile, roles | 客户前端 |
| mall-admin | authorization_code, refresh_token | openid, profile, roles, admin | 管理后台 |

## 6. 前端关键设计

### 6.1 项目构建
- 使用 Vite 构建，client 和 admin 各自独立 `package.json`
- 共享组件抽离到 `frontend/common/` 目录
- 开发环境通过 Vite proxy 将 `/api` 请求转发到 `http://localhost:8080`（Gateway）

### 6.2 状态管理
- Pinia stores：`useUserStore`（用户信息/access_token/refresh_token）、`useCartStore`（购物车数量）
- 购物车数据在用户登录后从后端获取，数量同步到导航栏徽标
- Axios 拦截器自动附加 `Authorization: Bearer <access_token>`；401 响应时尝试用 refresh_token 静默刷新，刷新失败跳转登录页

### 6.3 路由守卫
- 客户前端：`/order/*`、`/cart/*`、`/user/*` 等页面需登录，未登录跳转到 `/auth/login`
- 管理后台：全部路由需 ADMIN 角色，`/system/*` 路由需 SUPER_ADMIN 角色

## 7. 部署与 CI/CD

### 7.1 Docker Compose 编排

```yaml
services:
  nginx:         # 前端静态资源 + 反向代理到 Gateway
  gateway-svc:   # Spring Cloud Gateway (OAuth2 Resource Server)
  auth-svc:      # 认证服务 (Spring Authorization Server)
  user-svc:      # 用户服务
  product-svc:   # 商品服务
  order-svc:     # 订单服务
  nacos:         # 注册中心
  mysql:         # 数据库
  redis:         # 缓存
```

Nginx 负责提供前端静态资源以及将 `/api/*` 请求反向代理到 Gateway 服务。

### 7.2 GitHub Actions CI/CD Pipeline

```yaml
触发条件: push 到 main / PR 到 main
步骤:
  1. Checkout 代码
  2. Maven 编译各微服务
  3. 运行单元测试
  4. 构建 Docker 镜像（每个服务独立镜像）
  5. 推送镜像到 Docker Hub / GHCR
  6. （可选）SSH 部署到服务器
```

## 8. 扩展要求

### 8.1 Redis 缓存优化
- 缓存商品详情（key: `product:detail:{id}`，TTL: 30min）
- 缓存分类列表（key: `product:category:list`，TTL: 1h）
- 缓存 OAuth2 refresh_token 黑名单（退出登录后阻止刷新）
- 数据变更时及时失效或更新缓存

### 8.2 微服务架构（已纳入核心方案）
- 服务注册与发现：Nacos
- 服务间调用：OpenFeign
- 统一配置中心：Nacos Config
- 负载均衡：Spring Cloud LoadBalancer
- 熔断降级：Sentinel（可选）

### 8.3 Docker 容器化（已纳入核心方案）
- 每个微服务独立 Dockerfile
- docker-compose 一键编排启动
- 多阶段构建优化镜像体积

## 9. 评分标准对应

### 实验成绩 (50分)

| 评分项 | 分值 | 覆盖情况 | 说明 |
|--------|:----:|:--------:|------|
| **1.1 客户端基本功能** | **10分** | **全覆盖** | |
| 1.1.1 商品模块 | 3分 | ✅ | 分类/列表/详情/搜索/收藏/评论 |
| 1.1.2 购物车模块 | 3分 | ✅ | 加购/列表/管理/结算 |
| 1.1.3 订单模块 | 4分 | ✅ | 确认/提交/支付/筛选/售后 |
| **1.2 管理后台** | **10分** | **全覆盖** | |
| 1.2.1 管理员认证 | 2分 | ✅ | OAuth2 登录/角色权限 |
| 1.2.2 后台数据控制 | 2分 | ✅ | 用户/订单/销售额统计 |
| 1.2.3 用户管理 | 2分 | ✅ | 列表/搜索/禁用 |
| 1.2.4 商品管理 | 2分 | ✅ | CRUD/图片/分类/评论管理 |
| 1.2.5 订单管理 | 2分 | ✅ | 筛选/详情/售后/导出Excel |
| **2.1 用户认证** | **4分** | **全覆盖** | 注册/登录/找回密码/信息修改 |
| **2.2 首页模块** | **4分** | **全覆盖** | 轮播图/导航分类/推荐/搜索 |
| **2.3 收货地址** | **4分** | **全覆盖** | 新增/列表/修改/删除/默认 |
| **2.4 商品选配/营销** | **4分** | **部分覆盖** ✅ 规格选择 | 优惠券和秒杀活动可后续迭代 |
| **2.5 客户服务** | **4分** | **全覆盖** | 客服咨询/系统公告/用户反馈 |
| **3.1 Redis缓存优化** | **3分** | ✅ | 商品缓存/分类缓存/token黑名单 |
| **3.2 微服务架构** | **4分** | ✅ | 4服务拆分/Nacos/OpenFeign |
| **3.3 Docker容器化** | **3分** | ✅ | 多阶段构建/Compose编排 |

### 实验报告 (10分)

| 评分项 | 分值 | 说明 |
|--------|:----:|------|
| 实验报告 | 10分 | 独立 .docx 文档提交 |
