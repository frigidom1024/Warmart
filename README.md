# 🛒 暖 Warmart — 网上商城系统

基于 **Spring Cloud Alibaba 微服务架构**的电商平台，配套 **Vue 3 + Element Plus** 客户端和管理后台。

---

## 📋 功能概述

### 客户端（消费者端）

| 模块 | 功能 |
|------|------|
| **首页** | 轮播广告、分类导航、热销爆款、新品推荐、无限滚动「为你推荐」 |
| **商品** | 分类浏览、模糊搜索、多维度排序、商品详情（图片画廊 + 规格选择 + SKU）、收藏 |
| **购物车** | 添加商品（选规格/数量）、列表展示、勾选/全选、数量调整、结算跳转 |
| **订单** | 确认订单、模拟支付、订单列表（多状态筛选）、取消/确认收货/申请退款/查看物流 |
| **用户** | 邮箱注册（验证码）、登录（记住密码）、找回密码、个人信息、收货地址管理 |
| **其他** | 系统公告、售前咨询、意见反馈 |

### 管理后台（管理员端）

| 模块 | 功能 |
|------|------|
| **数据看板** | 用户数/订单数/销售额统计、ECharts 图表（趋势/状态/排行） |
| **用户管理** | 用户列表、搜索、禁用/启用、角色修改 |
| **商品管理** | 分类树 CRUD、商品列表/搜索、新增/编辑（图片上传 + SKU 规格）、导入/导出 Excel |
| **订单管理** | 订单筛选、详情、发货、取消订单、退款处理、导出 Excel |
| **系统管理** | 轮播图、公告、反馈、咨询、评价、物流管理 |

---

## 🏗️ 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端框架** | Vue 3 + Vite | ^3.5.x / ^8.0.x |
| **UI 组件库** | Element Plus | ^2.14.x |
| **状态管理** | Pinia | ^3.0.x |
| **路由** | Vue Router | ^5.0.x |
| **后端框架** | Spring Boot / Spring Cloud | 3.2.x / 2023.0.x |
| **服务注册** | Nacos | 2.3.2 |
| **ORM** | MyBatis-Plus | 3.5.x |
| **数据库** | MySQL | 8.0 |
| **缓存** | Redis | 7-alpine |
| **对象存储** | MinIO | 2024.12 |
| **容器化** | Docker / Docker Compose | - |
| **CI/CD** | GitHub Actions → GHCR | - |

---

## 📁 项目结构

```
├── backend/                        # 后端微服务 (Spring Cloud)
│   ├── gateway/                    # API 网关 (Spring Cloud Gateway)
│   ├── auth-service/               # 认证授权服务 (端口 8081)
│   ├── user-service/               # 用户服务 (端口 8082)
│   ├── product-service/            # 商品服务 (端口 8083)
│   └── order-service/              # 订单服务 (端口 8084)
├── frontend/                       # 客户端前端 (Vue 3 + Vite)
│   ├── src/
│   │   ├── api/                    # API 请求层
│   │   ├── views/                  # 页面组件
│   │   ├── layouts/                # 布局组件
│   │   ├── stores/                 # Pinia 状态管理
│   │   └── router/                 # 路由配置
│   └── package.json
├── admin-frontend/                 # 管理端前端 (Vue 3 + ECharts)
│   ├── src/
│   │   ├── api/
│   │   ├── views/
│   │   └── router/
│   └── package.json
└── deploy/                         # 部署配置
    ├── docker-compose.yml          # 本地开发（含 build）
    ├── docker-compose.prod.yml     # 生产部署（从 GHCR 拉取）
    ├── .env                        # 环境变量
    ├── mysql/                      # 数据库初始化脚本
    └── start.sh                    # 一键启动脚本
```

---

## 🚀 快速启动

### 前置要求

- JDK 17+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 20+

### 方式一：一键启动（推荐）

```bash
bash deploy/start.sh
```

### 方式二：分步启动

**1. 启动基础设施**

```bash
docker compose -f deploy/docker-compose.yml up -d nacos mysql redis minio
```

**2. 构建后端**

```bash
cd backend
mvn clean package -DskipTests
```

**3. 启动全部服务**

```bash
docker compose -f deploy/docker-compose.yml up -d
```

**4. 启动前端**

```bash
# 用户端
cd frontend
npm install
npm run dev

# 管理后台
cd admin-frontend
npm install
npm run dev
```

### 方式三：生产环境部署（从 GHCR 拉取）

```bash
# 部署最新版本
docker compose -f deploy/docker-compose.prod.yml up -d

# 部署指定版本
IMAGE_TAG=abc1234f docker compose -f deploy/docker-compose.prod.yml up -d
```

---

## 🔧 端口与配置

所有端口通过 `deploy/.env` 文件配置：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `NACOS_PORT` | 8848 | Nacos 服务 |
| `MYSQL_PORT` | 3306 | MySQL 数据库 |
| `REDIS_PORT` | 6379 | Redis 缓存 |
| `GATEWAY_PORT` | 8080 | API 网关 |
| `AUTH_SERVICE_PORT` | 8081 | 认证服务 |
| `USER_SERVICE_PORT` | 8082 | 用户服务 |
| `PRODUCT_SERVICE_PORT` | 8083 | 商品服务 |
| `ORDER_SERVICE_PORT` | 8084 | 订单服务 |

### 服务地址

| 服务 | 地址 |
|------|------|
| 用户端 | http://localhost:5173 |
| 管理后台 | http://localhost:5174 |
| API 网关 | http://localhost:8080 |
| Nacos 控制台 | http://localhost:8848/nacos |
| MinIO 控制台 | <http://localhost:9001> |

---

## 🌐 API 路由

网关统一前缀 `/api`：

| 路径 | 目标服务 |
|------|----------|
| `/api/auth/**` | Auth Service (8081) |
| `/api/user/**` | User Service (8082) |
| `/api/product/**` | Product Service (8083) |
| `/api/order/**` | Order Service (8084) |

---

## 🤖 CI/CD

每次推送 `main` 分支自动触发 GitHub Actions 工作流：

1. Maven 打包所有后端服务
2. npm install + build 构建两个前端项目
3. Docker 构建 5 个微服务镜像
4. 推送镜像到 GitHub Container Registry

镜像地址：`ghcr.io/frigidom1024/warmart-{service}:{tag}`（支持 `latest` 和 commit SHA 标签）

---

## 📊 未实现的功能

以下功能不在当前实现范围内，标注以供参考：

| 功能 | 说明 |
|------|------|
| 优惠券 | 无前后端实现 |
| 秒杀/促销活动 | 首页仅有文案装饰 |
| 实时在线客服 | 咨询为留言-回复模式，非 WebSocket 实时 |
| 第三方社交登录 | 仅有前端 UI 入口 |
| 真实支付网关 | 订单支付为模拟实现 |

---

## 📝 许可证

MIT License
