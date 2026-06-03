# 邮箱验证码与找回密码 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为注册流程增加邮箱验证码认证，并实现基于验证码的密码重置功能

**Architecture:** 在 auth-service 中新增 `EmailSender` 接口抽象邮件发送，阿里云 DirectMail（`SingleSendMail` API）为生产实现，Mock 为本地开发实现。验证码存储在 Redis 中，带 TTL 自动过期。业务层 `AuthService` 只依赖 `EmailSender` 接口。

**Tech Stack:** Spring Boot 3.2.5, MyBatis-Plus, Redis, Alibaba Cloud DirectMail SDK, Vue 3 + Element Plus

---

### Task 1: 新增阿里云 SDK 依赖和配置类

**Files:**
- Modify: `backend/auth-service/pom.xml`
- Modify: `backend/auth-service/src/main/resources/application.yml`
- Modify: `deploy/.env`
- Modify: `deploy/docker-compose.yml`

- [ ] **Step 1: pom.xml 新增阿里云 SDK 依赖**

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

在 `backend/auth-service/pom.xml` 的 `<dependencies>` 末尾（`springdoc` 之后）插入上述依赖。

- [ ] **Step 2: application.yml 新增阿里云 DM 配置和验证码配置**

在 `backend/auth-service/src/main/resources/application.yml` 末尾追加：

```yaml
# ============================================
# 阿里云 DirectMail 配置
# ============================================
email:
  sender-type: aliyun

aliyun:
  dm:
    access-key-id: ${ALIYUN_DM_ACCESS_KEY_ID}
    access-key-secret: ${ALIYUN_DM_ACCESS_KEY_SECRET}
    account-name: ${ALIYUN_DM_ACCOUNT_NAME}
    address-type: 1
    reply-to-address: true

# ============================================
# 验证码配置
# ============================================
auth:
  code:
    length: 6
    expiration: 300
    resend-interval: 60
    daily-limit: 10
```

- [ ] **Step 3: deploy/.env 新增环境变量**

在 `deploy/.env` 末尾追加：
```
# ============================================
# 阿里云 DirectMail (邮件推送)
# ============================================
ALIYUN_DM_ACCESS_KEY_ID=your-access-key-id
ALIYUN_DM_ACCESS_KEY_SECRET=your-access-key-secret
ALIYUN_DM_ACCOUNT_NAME=noreply@example.net
```

- [ ] **Step 4: docker-compose.yml 新增环境变量传递**

在 `deploy/docker-compose.yml` 中 `auth-service` 的 `environment` 下追加：
```yaml
      ALIYUN_DM_ACCESS_KEY_ID: ${ALIYUN_DM_ACCESS_KEY_ID}
      ALIYUN_DM_ACCESS_KEY_SECRET: ${ALIYUN_DM_ACCESS_KEY_SECRET}
      ALIYUN_DM_ACCOUNT_NAME: ${ALIYUN_DM_ACCOUNT_NAME}
```

- [ ] **Step 5: 验证编译无报错**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS (无错误输出)

---

### Task 2: User 实体新增 emailVerified 字段

**Files:**
- Modify: `backend/auth-service/src/main/java/com/mall/auth/entity/User.java`

- [ ] **Step 1: User.java 新增 emailVerified 字段**

在 `phone` 字段后插入：
```java
    @TableField("email_verified")
    private Boolean emailVerified;
```

完整上下文——修改后字段排列：
```java
    private String email;
    private String phone;
    @TableField("email_verified")
    private Boolean emailVerified;
    private String avatar;
```

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 3: 邮件发送抽象层 — EmailSender 接口

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/service/email/EmailSender.java`

- [ ] **Step 1: 创建 EmailSender 接口**

```java
package com.mall.auth.service.email;

/**
 * 邮件发送抽象接口。
 * 业务层通过此接口发送邮件，与具体供应商解耦。
 * 切换供应商只需新增实现类并修改 email.sender-type 配置。
 */
public interface EmailSender {

    /**
     * 发送邮件
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文（纯文本）
     */
    void send(String to, String subject, String content);
}
```

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 4: 阿里云 DirectMail 发送实现

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/service/email/AliyunEmailSender.java`

- [ ] **Step 1: 创建 AliyunEmailSender 实现类**

```java
package com.mall.auth.service.email;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dm.model.v20151123.SingleSendMailRequest;
import com.aliyuncs.dm.model.v20151123.SingleSendMailResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "email.sender-type", havingValue = "aliyun")
public class AliyunEmailSender implements EmailSender {

    private final IAcsClient client;
    private final String accountName;
    private final Integer addressType;
    private final Boolean replyToAddress;

    public AliyunEmailSender(
            @Value("${aliyun.dm.access-key-id}") String accessKeyId,
            @Value("${aliyun.dm.access-key-secret}") String accessKeySecret,
            @Value("${aliyun.dm.account-name}") String accountName,
            @Value("${aliyun.dm.address-type:1}") Integer addressType,
            @Value("${aliyun.dm.reply-to-address:true}") Boolean replyToAddress
    ) {
        this.accountName = accountName;
        this.addressType = addressType;
        this.replyToAddress = replyToAddress;
        DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
        this.client = new DefaultAcsClient(profile);
    }

    @Override
    public void send(String to, String subject, String content) {
        SingleSendMailRequest request = new SingleSendMailRequest();
        request.setAccountName(accountName);
        request.setAddressType(addressType);
        request.setReplyToAddress(replyToAddress);
        request.setToAddress(to);
        request.setSubject(subject);
        request.setTextBody(content);

        try {
            SingleSendMailResponse response = client.getAcsResponse(request);
            log.info("邮件发送成功 -> to={}, subject={}, envId={}", to, subject, response.getEnvId());
        } catch (ClientException e) {
            log.error("邮件发送失败 -> to={}, subject={}, errCode={}, errMsg={}",
                    to, subject, e.getErrCode(), e.getErrMsg());
            throw new RuntimeException("邮件发送失败: " + e.getErrMsg(), e);
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 5: Mock 邮件发送实现（本地开发用）

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/service/email/MockEmailSender.java`

- [ ] **Step 1: 创建 MockEmailSender 实现类**

```java
package com.mall.auth.service.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "email.sender-type", havingValue = "mock", matchIfMissing = true)
public class MockEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String content) {
        log.info("[Mock邮件] ==============================");
        log.info("[Mock邮件] 收件人: {}", to);
        log.info("[Mock邮件] 主题: {}", subject);
        log.info("[Mock邮件] 内容: {}", content);
        log.info("[Mock邮件] ==============================");
    }
}
```

注意：`matchIfMissing = true` 表示如果没有配置 `email.sender-type`，默认使用 Mock。

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 6: 新增请求 DTO

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/dto/SendCodeRequest.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/dto/VerifyCodeRequest.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/dto/ResetPasswordRequest.java`
- Modify: `backend/auth-service/src/main/java/com/mall/auth/dto/RegisterRequest.java`

- [ ] **Step 1: 创建 SendCodeRequest.java**

```java
package com.mall.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

- [ ] **Step 2: 创建 VerifyCodeRequest.java**

```java
package com.mall.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    private String code;

    @NotBlank(message = "验证码类型不能为空")
    private String type; // "register" 或 "reset"
}
```

- [ ] **Step 3: 创建 ResetPasswordRequest.java**

```java
package com.mall.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    private String code;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度6-100")
    private String newPassword;
}
```

- [ ] **Step 4: RegisterRequest.java 新增 code 字段**

在 `backend/auth-service/src/main/java/com/mall/auth/dto/RegisterRequest.java` 中 `email` 字段后面追加：

```java
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    private String code;
```

修改后完整字段：
```java
    private String email;
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为6位数字")
    private String code;
    // 原文件到此结束（无尾部字段了）
```

- [ ] **Step 5: 验证编译**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 7: AuthService 新增验证码业务逻辑

**Files:**
- Modify: `backend/auth-service/src/main/java/com/mall/auth/service/AuthService.java`

- [ ] **Step 1: AuthService 新增依赖注入和方法**

在 `AuthService` 类中新增依赖：
```java
    private final EmailSender emailSender;
```

在 `register()` 方法中，在创建 User 之前插入验证码校验逻辑（在邮箱重复性检查之后、`User user = new User();` 之前）：

```java
        // 验证注册验证码
        String codeKey = "auth:code:register:" + req.getEmail();
        String cachedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.error(410, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(req.getCode())) {
            return Result.error(400, "验证码不正确");
        }
        redisTemplate.delete(codeKey);
```

并在创建用户后设置 `emailVerified = true`：
```java
        user.setEmailVerified(true);
```

修改后 `register()` 方法完整代码（注意在邮箱重复性检查之后插入验证码逻辑）：

```java
    @Transactional
    public Result<Void> register(RegisterRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername())) > 0) {
            return Result.error(400, "用户名已存在");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail())) > 0) {
            return Result.error(400, "邮箱已被注册");
        }

        // 验证注册验证码
        String codeKey = "auth:code:register:" + req.getEmail();
        String cachedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.error(410, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(req.getCode())) {
            return Result.error(400, "验证码不正确");
        }
        redisTemplate.delete(codeKey);

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setEmail(req.getEmail());
        user.setEmailVerified(true);
        user.setRole("USER");
        user.setStatus(0);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success(null);
    }
```

- [ ] **Step 2: 新增发送验证码方法**

在 `AuthService` 类中（`register()` 之后）插入：

```java
    public Result<Void> sendRegisterCode(SendCodeRequest req) {
        return sendCode(req.getEmail(), "register");
    }

    public Result<Void> sendResetCode(SendCodeRequest req) {
        // 先检查邮箱是否已注册
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
        if (user == null) {
            return Result.error(404, "该邮箱未注册");
        }
        return sendCode(req.getEmail(), "reset");
    }

    private Result<Void> sendCode(String email, String type) {
        // 检查频率限制（同一邮箱60秒内不可重复发送）
        String limitKey = "auth:code:limit:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(limitKey))) {
            return Result.error(429, "发送过于频繁，请60秒后重试");
        }

        // 检查每日发送上限
        String dailyKey = "auth:code:daily:" + email;
        Integer dailyCount = (Integer) redisTemplate.opsForValue().get(dailyKey);
        if (dailyCount != null && dailyCount >= 10) {
            return Result.error(429, "今日验证码发送已达上限");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // 存入 Redis（5分钟过期）
        String codeKey = "auth:code:" + type + ":" + email;
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofSeconds(300));

        // 设置频率限制（60秒过期）
        redisTemplate.opsForValue().set(limitKey, "1", Duration.ofSeconds(60));

        // 更新每日计数
        if (dailyCount == null) {
            redisTemplate.opsForValue().set(dailyKey, 1, Duration.ofDays(1));
        } else {
            redisTemplate.opsForValue().increment(dailyKey);
        }

        // 发送邮件
        String subject = type.equals("register") ? "注册验证码" : "密码重置验证码";
        String content = String.format(
                "您的%s为：%s，有效期5分钟，请勿泄露给他人。",
                type.equals("register") ? "注册验证码" : "密码重置验证码", code);
        emailSender.send(email, subject, content);

        return Result.success(null);
    }
```

- [ ] **Step 3: 新增验证验证码方法**

```java
    public Result<Void> verifyCode(VerifyCodeRequest req) {
        String codeKey = "auth:code:" + req.getType() + ":" + req.getEmail();
        String cachedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.error(410, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(req.getCode())) {
            return Result.error(400, "验证码不正确");
        }
        // 验证通过后删除（一次性使用）
        redisTemplate.delete(codeKey);
        return Result.success(null);
    }
```

- [ ] **Step 4: 新增重置密码方法**

```java
    public Result<Void> resetPassword(ResetPasswordRequest req) {
        // 先验证验证码
        String codeKey = "auth:code:reset:" + req.getEmail();
        String cachedCode = (String) redisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null) {
            return Result.error(410, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equals(req.getCode())) {
            return Result.error(400, "验证码不正确");
        }
        redisTemplate.delete(codeKey);

        // 查找用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, req.getEmail()));
        if (user == null) {
            return Result.error(404, "该邮箱未注册");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(user);

        // 清除该用户的所有活跃刷新令牌（将其加入黑名单）
        // 注意：这里无法枚举所有令牌，所以清除策略是让用户重新登录
        // 实际业务中可通过记录用户登录的token列表来精确清除

        return Result.success(null);
    }
```

- [ ] **Step 5: 更新 import 语句**

在 `AuthService.java` 顶部新增 import：
```java
import com.mall.auth.dto.*;
import com.mall.auth.service.email.EmailSender;
```

（注意：原有的 `import com.mall.auth.dto.LoginRequest;` 等可以保留，但建议用 `import com.mall.auth.dto.*;` 替换以简化）

- [ ] **Step 6: 验证编译**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 8: AuthController 新增端点

**Files:**
- Modify: `backend/auth-service/src/main/java/com/mall/auth/controller/AuthController.java`

- [ ] **Step 1: AuthController 新增 4 个端点，修改 register 和 forgot-password**

```java
package com.mall.auth.controller;

import com.mall.auth.common.Result;
import com.mall.auth.dto.*;
import com.mall.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody SendCodeRequest req) {
        // 改为发送找回密码验证码（原空壳实现替换）
        return authService.sendResetCode(req);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@RequestParam String refreshToken) {
        return authService.refresh(refreshToken);
    }

    // ---- 新增验证码端点 ----

    @PostMapping("/code/send-register")
    public Result<Void> sendRegisterCode(@Valid @RequestBody SendCodeRequest req) {
        return authService.sendRegisterCode(req);
    }

    @PostMapping("/code/send-reset")
    public Result<Void> sendResetCode(@Valid @RequestBody SendCodeRequest req) {
        return authService.sendResetCode(req);
    }

    @PostMapping("/code/verify")
    public Result<Void> verifyCode(@Valid @RequestBody VerifyCodeRequest req) {
        return authService.verifyCode(req);
    }

    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return authService.resetPassword(req);
    }
}
```

注意：
- `forgot-password` 端点复用 `SendCodeRequest` 请求体（替代原来的 `@RequestParam String email`）
- 所有新端点使用 `@Valid` 校验

- [ ] **Step 2: 验证编译**

```bash
cd backend && mvn compile -pl auth-service -am -q
```
Expected: BUILD SUCCESS

---

### Task 9: 前端 auth.ts 新增 API 方法

**Files:**
- Modify: `frontend/src/api/auth.ts`

- [ ] **Step 1: 新增验证码和重置密码 API 方法**

```typescript
export function sendRegisterCode(email: string) {
  return request.post<void>('/auth/code/send-register', { email })
}

export function sendResetCode(email: string) {
  return request.post<void>('/auth/code/send-reset', { email })
}

export function verifyCode(email: string, code: string, type: string) {
  return request.post<void>('/auth/code/verify', { email, code, type })
}

export function resetPassword(email: string, code: string, newPassword: string) {
  return request.post<void>('/auth/reset-password', { email, code, newPassword })
}
```

同时更新 `RegisterRequest` 接口，新增 `code` 字段：
```typescript
export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email: string
  code: string
}
```

完整修改后的文件：
```typescript
import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email: string
  code: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export function login(data: LoginRequest) {
  return request.post<AuthResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<void>('/auth/register', data)
}

export function forgotPassword(email: string) {
  return request.post<void>('/auth/forgot-password', { email })
}

export function refreshToken(refreshToken: string) {
  return request.post<AuthResponse>('/auth/refresh', null, { params: { refreshToken } })
}

export function sendRegisterCode(email: string) {
  return request.post<void>('/auth/code/send-register', { email })
}

export function sendResetCode(email: string) {
  return request.post<void>('/auth/code/send-reset', { email })
}

export function verifyCode(email: string, code: string, type: string) {
  return request.post<void>('/auth/code/verify', { email, code, type })
}

export function resetPassword(email: string, code: string, newPassword: string) {
  return request.post<void>('/auth/reset-password', { email, code, newPassword })
}
```

- [ ] **Step 2: 确认 TypeScript 编译无报错**

```bash
cd frontend && npx vue-tsc --noEmit 2>&1 | head -20
```
Expected: 无 TypeScript 编译错误（若项目已有错误则忽略已有错误，关注新增内容）

---

### Task 10: 前端注册表单增加验证码输入

**Files:**
- Modify: `frontend/src/views/AuthView.vue`

- [ ] **Step 1: 修改 script 部分，增加验证码状态和方法**

更新 `registerForm` 的定义——增加 `code` 字段：
```typescript
const registerForm = ref({ email: '', password: '', confirm: '', code: '' })
```

新增验证码相关状态和方法（在 `loading` 之后）：
```typescript
const codeSending = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

async function handleSendRegisterCode() {
  if (!registerForm.value.email) {
    showToast('请先输入邮箱', 'warning')
    return
  }
  codeSending.value = true
  try {
    await sendRegisterCode(registerForm.value.email)
    showToast('验证码已发送', 'success')
    startCountdown()
  } catch {
    // handled by interceptor
  } finally {
    codeSending.value = false
  }
}

function startCountdown() {
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
    }
  }, 1000)
}
```

需要新增 import：
```typescript
import { onUnmounted } from 'vue'
import { sendRegisterCode } from '@/api/auth'
```

- [ ] **Step 2: 注册表单模板增加验证码输入框**

在注册表单的"确认密码"字段后面（`</div>` 后）、注册按钮之前插入：

```html
              <div class="auth-form__field">
                <label class="auth-form__label">验证码</label>
                <div class="auth-form__code-wrap">
                  <div class="auth-form__input-wrap" style="flex:1">
                    <svg class="auth-form__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
                    <input v-model="registerForm.code" type="text" class="auth-form__input" placeholder="请输入验证码" maxlength="6" />
                  </div>
                  <button
                    type="button"
                    class="auth-form__code-btn"
                    :disabled="codeSending || codeCountdown > 0"
                    @click="handleSendRegisterCode"
                  >{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</button>
                </div>
              </div>
```

- [ ] **Step 3: 更新 `handleRegister` 方法**

注册时传入 `code` 字段：
```typescript
    await registerApi({
      username: registerForm.value.email,
      password: registerForm.value.password,
      nickname: registerForm.value.email.split('@')[0],
      email: registerForm.value.email,
      code: registerForm.value.code
    })
```

- [ ] **Step 4: 新增验证码相关样式**

在 style 块末尾追加：
```css
.auth-form__code-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
}

.auth-form__code-btn {
  height: 44px;
  padding: 0 16px;
  background: var(--wz-orange);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity var(--wz-duration-fast) var(--wz-ease-out);
  flex-shrink: 0;
}

.auth-form__code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-form__code-btn:hover:not(:disabled) {
  opacity: 0.85;
}
```

---

### Task 11: 前端找回密码页面重构为验证码模式

**Files:**
- Modify: `frontend/src/views/ForgotPasswordView.vue`

- [ ] **Step 1: 完整重写 ForgotPasswordView.vue**

```vue
<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { sendResetCode, resetPassword } from '@/api/auth'
import { showToast } from '@/utils/toast'

const router = useRouter()
const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

async function handleSendCode() {
  if (!email.value.trim()) {
    showToast('请输入邮箱地址', 'warning')
    return
  }
  codeSending.value = true
  try {
    await sendResetCode(email.value.trim())
    showToast('验证码已发送', 'success')
    startCountdown()
  } catch { /* handled */ } finally {
    codeSending.value = false
  }
}

function startCountdown() {
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
    }
  }, 1000)
}

async function handleReset() {
  if (!email.value.trim() || !code.value || !newPassword.value) {
    showToast('请填写完整信息', 'warning')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    showToast('两次输入的密码不一致', 'warning')
    return
  }
  if (newPassword.value.length < 6) {
    showToast('密码长度不少于6位', 'warning')
    return
  }
  loading.value = true
  try {
    await resetPassword(email.value.trim(), code.value, newPassword.value)
    showToast('密码重置成功，请重新登录', 'success')
    router.push('/auth')
  } catch { /* handled */ } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="reset-wrapper">
      <el-card class="reset-card" shadow="never">
        <div class="reset-card__header">
          <h1 class="reset-card__title">重置密码</h1>
          <p class="reset-card__desc">输入邮箱获取验证码，设置新密码</p>
        </div>
        <div class="reset-card__body">
          <div class="reset-card__field">
            <span class="reset-card__label">邮箱地址</span>
            <input v-model="email" type="email" class="reset-card__input" placeholder="请输入邮箱地址">
          </div>
          <div class="reset-card__field">
            <span class="reset-card__label">验证码</span>
            <div class="reset-card__code-wrap">
              <input v-model="code" type="text" class="reset-card__input" placeholder="请输入验证码" maxlength="6" style="flex:1">
              <button
                class="reset-card__code-btn"
                :disabled="codeSending || codeCountdown > 0"
                @click="handleSendCode"
              >{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</button>
            </div>
          </div>
          <div class="reset-card__field">
            <span class="reset-card__label">新密码</span>
            <input v-model="newPassword" type="password" class="reset-card__input" placeholder="请设置新密码（至少6位）">
          </div>
          <div class="reset-card__field">
            <span class="reset-card__label">确认密码</span>
            <input v-model="confirmPassword" type="password" class="reset-card__input" placeholder="请再次输入新密码">
          </div>
          <div class="reset-card__actions">
            <div
              class="reset-card__btn-placeholder"
              :class="{ 'reset-card__btn-placeholder--disabled': loading }"
              @click="handleReset"
            >{{ loading ? '重置中...' : '重置密码' }}</div>
          </div>
          <p class="reset-card__back" @click="router.push('/auth')">返回登录</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-bg);
}
.reset-wrapper {
  width: 100%;
  max-width: 400px;
  padding: 24px;
}
.reset-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 53, 0.08);
}
.reset-card__header {
  text-align: center;
  padding: 32px 24px 20px;
}
.reset-card__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.reset-card__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.reset-card__body {
  padding: 0 24px 32px;
}
.reset-card__field {
  margin-bottom: 20px;
}
.reset-card__label {
  display: block;
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 8px;
  font-weight: 500;
}
.reset-card__input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}
.reset-card__input:focus {
  border-color: var(--wz-orange);
}
.reset-card__input::placeholder {
  color: var(--wz-text-muted);
}
.reset-card__code-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
}
.reset-card__code-btn {
  height: 42px;
  padding: 0 14px;
  background: var(--wz-orange);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: opacity var(--wz-duration-fast) var(--wz-ease-out);
}
.reset-card__code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.reset-card__code-btn:hover:not(:disabled) {
  opacity: 0.85;
}
.reset-card__actions {
  margin-top: 24px;
}
.reset-card__btn-placeholder {
  height: 44px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  cursor: pointer;
  font-weight: 500;
  letter-spacing: 1px;
}
.reset-card__btn-placeholder--disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.reset-card__back {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--wz-orange);
  cursor: pointer;
}
</style>
```

- [ ] **Step 2: 确认不再引用旧的 forgotPassword API**

检查是否只有 `sendResetCode` 和 `resetPassword` 被使用，旧的 `forgotPassword` 调用已被移除。

---

### Task 12: 数据库迁移脚本

**Files:**
- Modify: `deploy/mysql/init.sql`（如果存在）或提供 SQL 脚本

- [ ] **Step 1: 提供新增字段的 SQL**

```sql
ALTER TABLE `user` ADD COLUMN `email_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证';
```

注：如果 `deploy/mysql/init.sql` 存在，将上述 SQL 追加到文件末尾。如果不存在，将此 SQL 记录在文档中供手动执行。

---

## 自审检查

### 1. 规约覆盖率检查

| 规约需求 | 对应任务 |
|---------|---------|
| 邮件发送接口 EmailSender | Task 3 |
| 阿里云 DirectMail 实现 | Task 4 |
| Mock 本地实现 | Task 5 |
| User 实体 emailVerified 字段 | Task 2 |
| Redis Key 设计（register/reset/limit/daily） | Task 7 |
| 发送注册验证码端点 | Task 8 |
| 发送找回密码验证码端点 | Task 8 |
| 验证验证码端点 | Task 8 |
| 重置密码端点 | Task 8 |
| 注册端点增加 code 校验 | Task 7 + Task 8 |
| forgot-password 端点改造 | Task 8 |
| 注册前端验证码 UI | Task 10 |
| 找回密码前端验证码 UI | Task 11 |
| 前端 API 方法 | Task 9 |
| Gateway 路由 | 无需新增（已有 `/api/auth/**` 匹配） |
| 阿里云 SDK 依赖 | Task 1 |
| deploy/.env 配置 | Task 1 |

### 2. 占位符检查

所有步骤代码完整，无 "TBD"、"TODO" 或占位符。

### 3. 类型一致性检查

- `RegisterRequest.code`（String, `@Pattern(regexp = "^\\d{6}$")`）— 前后一致
- `ResetPasswordRequest` 字段名与前端 `resetPassword()` 参数对应
- `sendCode()` 方法签名一致：`(SendCodeRequest)` → `sendRegisterCode` / `sendResetCode`
- `EmailSender.send(to, subject, content)` — Task 3 定义，Task 4/5 实现一致
