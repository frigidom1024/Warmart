# Mall - 网上商城系统

基于 Spring Cloud Alibaba 微服务架构的网上商城系统，配套 Vue 3 前台和后台管理。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| 微服务 | Spring Cloud | 2023.0.3 |
| 服务注册 | Nacos | 2.3.2 |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7 |
| 前端 | Vue 3 + Vite | 5 |
| UI 框架 | Element Plus | 2.6 |
| 状态管理 | Pinia | 2.1 |
| HTTP 客户端 | Axios | 1.6 |

## 项目结构

```
├── backend                      # 后端微服务
│   ├── gateway                  # API 网关 (Spring Cloud Gateway)
│   ├── auth-service             # 认证授权服务
│   ├── user-service             # 用户服务
│   ├── product-service          # 商品服务
│   └── order-service            # 订单服务
├── frontend
│   ├── client                   # 前台用户端 (Vue 3)
│   └── admin                    # 后台管理端 (Vue 3)
└── deploy                       # 部署配置
    ├── docker-compose.yml       # Docker Compose 编排
    ├── .env                     # 端口/密码配置
    ├── mysql/                   # 数据库初始化脚本
    └── nginx/                   # Nginx 配置
```

## 快速启动

### 前置要求

- JDK 17+
- Maven 3.9+
- Docker & Docker Compose
- Node.js 18+

### 一键启动

```bash
bash deploy/start.sh
```

### 分步启动

**1. 启动基础设施**

```bash
docker compose -f deploy/docker-compose.yml up -d nacos mysql redis
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
cd frontend/client
npm install
npm run dev

# 管理后台
cd frontend/admin
npm install
npm run dev
```

### 端口配置

所有基础设施和服务的端口通过 `deploy/.env` 文件配置，支持自定义：

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

## API 路由

网关统一前缀 `/api`：

| 路径 | 目标服务 |
|------|----------|
| `/api/auth/**` | Auth Service |
| `/api/user/**` | User Service |
| `/api/product/**` | Product Service |
| `/api/order/**` | Order Service |
