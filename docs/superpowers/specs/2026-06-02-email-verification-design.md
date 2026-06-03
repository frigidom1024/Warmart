# 邮箱验证码认证与找回密码功能设计

## 概述

为注册流程增加邮箱验证码认证，并为用户提供基于验证码的找回密码功能。利用项目已有的 Redis 基础设施存储验证码，通过阿里云 DirectMail API 发送邮件。

**关键设计原则：** 邮件发送抽象为接口，与具体供应商解耦。切换邮件服务商只需新增一个实现类 + 修改配置，无需改动业务代码。

## 目标

- 注册时用户需通过邮箱验证码确认邮箱真实性
- 支持通过邮箱验证码重置密码
- 验证码安全机制：有效期、频率限制、一次性使用
- **邮件发送抽象化**：通过接口与具体供应商解耦，便于切换

## 非目标

- 短信验证码 / 多因素认证
- 邮件模板的可视化编辑
- 邮件发送的异步队列（后续可按需升级）
- 独立邮件微服务（当前过度设计）

## 数据模型变更

### User 实体（auth-service）

| 字段 | 类型 | 说明 |
|------|------|------|
| `email_verified` | `tinyint(1)` | 邮箱是否已验证，默认 `0`（false） |

```sql
ALTER TABLE `user` ADD COLUMN `email_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证';
```

### Redis Key 设计

| Key | 值 | TTL | 用途 |
|-----|-----|-----|------|
| `auth:code:register:{email}` | 6位数字验证码 | 300s | 注册验证码 |
| `auth:code:reset:{email}` | 6位数字验证码 | 300s | 找回密码验证码 |
| `auth:code:limit:{email}` | `1` | 60s | 发送频率限制 |
| `auth:code:daily:{email}` | 计数 | 86400s | 每日发送上限 |

## 后端设计

### 邮件发送抽象层

业务代码不直接依赖阿里云 SDK，而是面向接口编程：

```text
┌───────────────────────────────────────────┐
│               AuthService                   │
│  (业务逻辑 —— 只依赖 EmailSender 接口)      │
└──────────────┬────────────────────────────┘
               │ 发送验证码邮件
               ▼
┌───────────────────────────────────────────┐
│          EmailSender (接口)                 │
│  + send(email, subject, content)           │
└──────────────┬────────────────────────────┘
               │ 由 Spring 注入具体实现
       ┌───────┴────────┐
       ▼                ▼
┌──────────────┐  ┌──────────────┐
│ AliyunSender │  │ MockSender   │  ← 后续可加 SmtpSender、SendGridSender 等
│ (生产环境)   │  │ (本地开发)    │
└──────────────┘  └──────────────┘
```

**切换方式：** 通过 `application.yml` 中的配置 + `@ConditionalOnProperty` 选择实现类。

### 新增依赖

`auth-service/pom.xml`：
```xml
<!-- 阿里云 DirectMail SDK -->
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-dm</artifactId>
    <version>3.3.1</version>
</dependency>
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>aliyun-java-sdk-core</artifactId>
    <version>4.6.3</version>
</dependency>
```

### 新增配置

`auth-service/src/main/resources/application.yml`：
```yaml
# 邮件发送器选择（支持切换实现）
# aliyun = 阿里云 DirectMail, mock = 本地模拟（仅输出日志）
email:
  sender-type: aliyun             # 切换这里即可换供应商

# 阿里云 DirectMail 配置（仅 sender-type=aliyun 时生效）
aliyun:
  dm:
    access-key-id: ${ALIYUN_DM_ACCESS_KEY_ID}
    access-key-secret: ${ALIYUN_DM_ACCESS_KEY_SECRET}
    account-name: ${ALIYUN_DM_ACCOUNT_NAME}   # 发信地址
    address-type: 1                             # 随机账号(0) 或 发信地址(1)
    reply-to-address: true                      # 启用回信地址

# 验证码配置
auth:
  code:
    length: 6                       # 验证码长度
    expiration: 300                 # 过期时间（秒）
    resend-interval: 60             # 重发间隔（秒）
    daily-limit: 10                 # 每日发送上限
```

`deploy/.env` 新增：
```
ALIYUN_DM_ACCESS_KEY_ID=your-access-key-id
ALIYUN_DM_ACCESS_KEY_SECRET=your-access-key-secret
ALIYUN_DM_ACCOUNT_NAME=noreply@example.net
```

### 新增端点

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| `POST` | `/api/auth/code/send-register` | 公开 | 发送注册验证码 |
| `POST` | `/api/auth/code/send-reset` | 公开 | 发送找回密码验证码 |
| `POST` | `/api/auth/code/verify` | 公开 | 验证验证码（通用） |
| `POST` | `/api/auth/reset-password` | 公开 | 验证码重置密码 |

### 修改端点

| 方法 | 路径 | 变更 |
|------|------|------|
| `POST` | `/api/auth/register` | 请求体新增必填字段 `code`，成功后设置 `email_verified=true` |
| `POST` | `/api/auth/forgot-password` | 移除旧空壳实现，改用验证码模式（可保留为向后兼容或废弃） |

### Gateway 路由

`gateway/src/main/resources/application.yml` 新增：
```yaml
- id: auth-code
  uri: lb://auth-service
  predicates:
    - Path=/api/auth/code/**, /api/auth/reset-password
  filters:
    - StripPrefix=1
```

### 核心逻辑

**发送验证码：**
1. 校验邮箱格式
2. 检查邮箱是否已注册（send-register 场景）或未注册（send-reset 场景）
3. 检查频率限制（Redis key `auth:code:limit:{email}`）
4. 检查每日发送上限（Redis key `auth:code:daily:{email}`）
5. 生成 6 位随机数字验证码，存入 Redis（`auth:code:{type}:{email}`，TTL 300s）
6. 设置频率限制 key（TTL 60s）和每日计数 key
7. 通过阿里云 DirectMail SDK（`SingleSendMail`）发送验证码邮件
8. 返回成功

**验证验证码：**
1. 从 Redis 获取 `auth:code:{type}:{email}`
2. 不存在 → 返回"验证码已过期"
3. 不匹配 → 返回"验证码不正确"
4. 匹配 → 删除 Redis key（确保一次性使用），返回成功

**重置密码：**
1. 校验新密码格式
2. 验证验证码（调用通用验证逻辑）
3. 验证通过 → BCrypt 加密新密码 → 更新数据库
4. 清除该用户的所有活跃令牌（Redis 令牌黑名单）
5. 返回成功

**注册（修改）：**
1. 校验用户名/邮箱唯一性（已有逻辑）
2. **新增**：验证验证码 `code` 字段
3. 验证通过 → 创建用户，`email_verified=true`
4. 返回 JWT 令牌

### 错误码

| HTTP 状态码 | 场景 |
|-------------|------|
| 400 | 邮箱格式无效、验证码错误、密码格式不符合要求 |
| 404 | 找回密码时邮箱未注册 |
| 409 | 注册时邮箱已被注册 |
| 429 | 发送过于频繁或已达每日上限 |
| 410 | 验证码已过期 |

## 前端设计

### 注册页（AuthView.vue）

- 表单新增"验证码"输入框和"获取验证码"按钮
- 点击"获取验证码" → POST `/api/auth/code/send-register`
- 发送成功后按钮进入 60 秒倒计时（禁用状态）
- 提交注册时携带 `code` 字段

```
┌─────────────────────────────────────┐
│            注册账号                    │
│                                       │
│  📧 邮箱      [________________]      │
│  🔑 密码      [________________]      │
│  👤 昵称      [________________]      │
│  ✉️ 验证码    [______] [获取验证码]   │
│                                       │
│  [         注册账号          ]         │
│  已有账号？去登录                       │
└─────────────────────────────────────┘
```

### 找回密码页（ForgotPasswordView.vue）

- 重新设计为验证码模式
- 表单包含：邮箱、验证码、新密码、确认密码
- 点击"获取验证码" → POST `/api/auth/code/send-reset`
- 提交 → POST `/api/auth/reset-password`
- 成功 → 提示并跳转到登录页

```
┌─────────────────────────────────────┐
│          找回密码                     │
│                                       │
│  📧 邮箱      [________________]      │
│  ✉️ 验证码    [______] [获取验证码]   │
│  🔑 新密码    [________________]      │
│  🔑 确认密码  [________________]      │
│                                       │
│  [         重置密码          ]         │
│  想起密码了？去登录                     │
└─────────────────────────────────────┘
```

## 安全设计

- 验证码 6 位纯数字，300 秒自动过期（Redis TTL）
- 验证码验证后立即删除 Redis key，防止重复使用
- 同一邮箱 60 秒内不可重复发送验证码
- 每邮箱每日最多发送 10 次验证码
- 注册验证码和重置密码验证码使用独立的 Redis namespace
- 密码重置后清除该用户的所有活跃 JWT 令牌
- 邮件内容不包含密码等敏感信息

## 涉及文件清单

### 后端（auth-service）

| 文件 | 操作 |
|------|------|
| `pom.xml` | 新增 `aliyun-java-sdk-dm`、`aliyun-java-sdk-core` 依赖 |
| `application.yml` | 新增阿里云 DM 和验证码配置 |
| `entity/User.java` | 新增 `emailVerified` 字段 |
| `service/email/EmailSender.java` | **新建** — 邮件发送接口，核心抽象 |
| `service/email/AliyunEmailSender.java` | **新建** — 阿里云 DirectMail 实现（生产用） |
| `service/email/MockEmailSender.java` | **新建** — 本地模拟实现（开发用，仅输出日志） |
| `service/AuthService.java` | 新增验证码业务方法，修改注册/重置密码逻辑 |
| `controller/AuthController.java` | 新增 4 个端点，修改 register 端点 |
| `dto/SendCodeRequest.java` | **新建** — 发送验证码请求体 |
| `dto/VerifyCodeRequest.java` | **新建** — 验证验证码请求体 |
| `dto/ResetPasswordRequest.java` | **新建** — 重置密码请求体 |

### 网关（gateway）

| 文件 | 操作 |
|------|------|
| `application.yml` | 新增 `/api/auth/code/**`、`/api/auth/reset-password` 路由 |

### 部署

| 文件 | 操作 |
|------|------|
| `deploy/.env` | 新增 `ALIYUN_DM_ACCESS_KEY_ID`、`ALIYUN_DM_ACCESS_KEY_SECRET`、`ALIYUN_DM_ACCOUNT_NAME` |

### 前端（frontend）

| 文件 | 操作 |
|------|------|
| `src/api/auth.ts` | 新增验证码相关 API 方法 |
| `src/views/AuthView.vue` | 注册表单增加验证码输入和倒计时 |
| `src/views/ForgotPasswordView.vue` | 重构为验证码重置模式 |
| `src/router/index.ts` | 确认找回密码路由正确 |
