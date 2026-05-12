# 网上商城系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete online mall system with Spring Cloud microservices backend + VUE3 frontend, covering customer shopping flow and admin management, with Docker deployment and CI/CD.

**Architecture:** 4 microservices (user, product, order) + auth-service (Spring Authorization Server) + gateway (Spring Cloud Gateway). Frontend split into client (customer) and admin (management) VUE3 apps. Nacos for service discovery, MySQL per service, Redis for caching.

**Tech Stack:** Spring Boot 3.x, Spring Cloud, MyBatis-Plus, Spring Authorization Server, MySQL 8.x, Redis 7.x, Vue 3 + Element Plus + Pinia, Vite, Docker, GitHub Actions

---

## Phase 1: Project Scaffolding & Infrastructure

### Task 1: Create Monorepo Directory Structure and Root POM

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/gateway/pom.xml`
- Create: `backend/auth-service/pom.xml`
- Create: `backend/user-service/pom.xml`
- Create: `backend/product-service/pom.xml`
- Create: `backend/order-service/pom.xml`
- Create: `.gitignore`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p backend/{gateway,auth-service,user-service,product-service,order-service}/src/{main/{java/com/mall/{gateway,auth,user,product,order},resources},test/java/com/mall}
mkdir -p frontend/{client,admin}
mkdir -p deploy/{mysql,nginx}
mkdir -p .github/workflows
```

- [ ] **Step 2: Create root pom.xml**

Root POM at `backend/pom.xml` as `<packaging>pom</packaging>` with modules: gateway, auth-service, user-service, product-service, order-service.

Use Spring Boot 3.2.x parent, Spring Cloud 2023.0.x. Properties: java version 17, spring-cloud version, mybatis-plus-spring-boot3 version, mysql-connector-j version.

Dependency management (`<dependencyManagement>`): spring-cloud-dependencies (BOM), mybatis-plus-spring-boot3-starter, mysql-connector-j, spring-boot-starter-oauth2-resource-server, spring-boot-starter-security, spring-boot-starter-web, spring-boot-starter-data-redis, lombok, spring-boot-starter-test.

- [ ] **Step 3: Create .gitignore**

```
target/
node_modules/
.DS_Store
*.iml
.idea/
*.log
.env
.superpowers/
```

- [ ] **Step 4: Commit**

```bash
git init
git add -A
git commit -m "feat: init monorepo structure with root POM"
```

---

### Task 2: Initialize Nacos and Common Backend Config

**Files:**
- Create: `deploy/docker-compose.yml` (base, without service application images yet)
- Create: `deploy/mysql/init.sql` (placeholder)

- [ ] **Step 1: Create base docker-compose.yml with infrastructure services**

`deploy/docker-compose.yml`:
```yaml
services:
  nacos:
    image: nacos/nacos-server:v2.3.2
    environment:
      MODE: standalone
    ports:
      - "8848:8848"

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
    ports:
      - "3306:3306"
    volumes:
      - ./mysql/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
```

- [ ] **Step 2: Create MySQL init.sql**

`deploy/mysql/init.sql` creates 4 databases:
```sql
CREATE DATABASE IF NOT EXISTS `mall_user` DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `mall_product` DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `mall_order` DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `mall_auth` DEFAULT CHARSET utf8mb4;
```

- [ ] **Step 3: Commit**

```bash
git add deploy/
git commit -m "feat: add docker-compose for infrastructure and mysql init"
```

---

## Phase 2: Auth Service

### Task 3: Auth Service - Entity, Security Config, OAuth2 Server

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/AuthApplication.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/entity/User.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/mapper/UserMapper.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/config/SecurityConfig.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/config/AuthorizationServerConfig.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/config/RegisteredClientConfig.java`
- Create: `backend/auth-service/src/main/resources/application.yml`

- [ ] **Step 1: Create AuthApplication.java**

```java
package com.mall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```

- [ ] **Step 2: Create User entity**

User entity in `auth-service` (maps `mall_auth.user` table): fields id (Long), username, password (bcrypt), email, role (String), status (Integer). Annotate with `@Data`, `@TableName("user")`.

- [ ] **Step 3: Create UserMapper**

```java
package com.mall.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

- [ ] **Step 4: Create SecurityConfig.java**

Security config using `@Configuration`, `@EnableWebSecurity`. Define `SecurityFilterChain` for OAuth2 endpoints. Configure:
- `/api/auth/register`, `/api/auth/login`, `/api/auth/token`, `/api/auth/refresh`, `/api/auth/forgot-password` permitAll
- All other requests authenticated
- CORS enabled
- `BCryptPasswordEncoder` bean
- `AuthenticationManager` bean

- [ ] **Step 5: Create AuthorizationServerConfig.java**

Using Spring Authorization Server (`@EnableAuthorizationServer` isn't a thing in Spring Auth Server 3.x - use `@Import(OAuth2AuthorizationServerConfiguration.class)` approach instead). Configure:
- `RegisteredClientRepository` bean (from RegisteredClientConfig)
- `OAuth2TokenGenerator` bean (JWT with Nimbus)
- `OAuth2TokenEndpointFilter` customizer for `/api/auth/token` endpoint

Key config:
```java
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        return http.build();
    }
}
```

- [ ] **Step 6: Create RegisteredClientConfig.java**

Define two pre-registered OAuth2 clients using `RegisteredClientRepository` (in-memory for simplicity):
- `mall-client` (client-secret: `client123`): authorization_code + refresh_token grants, redirect_uri: `http://localhost:5173/login/oauth2/code/mall-client`, scopes: openid, profile, roles
- `mall-admin` (client-secret: `admin123`): authorization_code + refresh_token grants, redirect_uri: `http://localhost:5174/login/oauth2/code/mall-admin`, scopes: openid, profile, roles, admin

Both with access token TTL 30 minutes, refresh token TTL 7 days.

- [ ] **Step 7: Create application.yml for auth-service**

```yaml
server:
  port: 8081
spring:
  application:
    name: auth-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
  datasource:
    url: jdbc:mysql://localhost:3306/mall_auth?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.mall.auth.entity
```

- [ ] **Step 8: Commit**

```bash
git add backend/auth-service/
git commit -m "feat: add auth-service with OAuth2 Authorization Server config"
```

---

### Task 4: Auth Service - Auth Controller and Service

**Files:**
- Create: `backend/auth-service/src/main/java/com/mall/auth/service/AuthService.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/controller/AuthController.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/dto/LoginRequest.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/dto/RegisterRequest.java`
- Create: `backend/auth-service/src/main/java/com/mall/auth/dto/AuthResponse.java`

- [ ] **Step 1: Create DTOs**

LoginRequest: username (String), password (String) with `@NotBlank`.
RegisterRequest: username, password, email, nickname with `@NotBlank`, `@Email`.
AuthResponse: accessToken, refreshToken, expiresIn, tokenType = "Bearer".

- [ ] **Step 2: Create AuthService.java**

Methods:
- `register(RegisterRequest)`: check username/email uniqueness → hash password with BCrypt → insert user → return success
- `login(LoginRequest)`: find user by username → verify password → generate OAuth2 access_token + refresh_token → return AuthResponse
- `logout(String refreshToken)`: add refresh_token to Redis blacklist
- `forgotPassword(String email)`: validate email exists → generate reset code → (simulate sending email) return success
- `refreshToken(String refreshToken)`: validate refresh_token (not blacklisted) → generate new access_token + refresh_token → return AuthResponse

- [ ] **Step 3: Create AuthController.java**

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterRequest req) { ... }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginRequest req) { ... }

    @PostMapping("/refresh")
    public Result refresh(@RequestParam String refreshToken) { ... }

    @PostMapping("/logout")
    public Result logout(@RequestHeader("Authorization") String authHeader) { ... }

    @PostMapping("/forgot-password")
    public Result forgotPassword(@RequestParam String email) { ... }
}
```

All return `Result` wrapper: `{ code: 200, message: "success", data: ... }`. Error codes: 400 (bad request), 401 (unauthorized), 500 (server error).

- [ ] **Step 4: Create Result wrapper class**

```java
@Data
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/auth-service/src/main/java/com/mall/auth/service/
git add backend/auth-service/src/main/java/com/mall/auth/controller/
git add backend/auth-service/src/main/java/com/mall/auth/dto/
git commit -m "feat: add auth controller and service with register/login/refresh/logout"
```

---

## Phase 3: User Service

### Task 5: User Service - Entity, Mapper, Config

**Files:**
- Create: `backend/user-service/src/main/java/com/mall/user/UserApplication.java`
- Create: `backend/user-service/src/main/java/com/mall/user/entity/UserAddress.java`
- Create: `backend/user-service/src/main/java/com/mall/user/entity/Notice.java`
- Create: `backend/user-service/src/main/java/com/mall/user/entity/UserFeedback.java`
- Create: `backend/user-service/src/main/java/com/mall/user/entity/Consultation.java`
- Create: `backend/user-service/src/main/java/com/mall/user/mapper/UserAddressMapper.java`
- Create: `backend/user-service/src/main/java/com/mall/user/mapper/NoticeMapper.java`
- Create: `backend/user-service/src/main/java/com/mall/user/mapper/UserFeedbackMapper.java`
- Create: `backend/user-service/src/main/java/com/mall/user/mapper/ConsultationMapper.java`
- Create: `backend/user-service/src/main/java/com/mall/user/config/ResourceServerConfig.java`
- Create: `backend/user-service/src/main/resources/application.yml`

- [ ] **Step 1: Create UserApplication.java**

```java
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.mall.user.mapper")
public class UserApplication { ... }
```

- [ ] **Step 2: Create entities**

UserAddress: id, userId, receiverName, receiverPhone, province, city, district, detailAddress, isDefault (Integer), createdTime, updatedTime. `@TableName("user_address")`.

Notice: id, title, content, type ("system"/"activity"), status (0=published/1=draft), createdTime. `@TableName("notice")`.

UserFeedback: id, userId, type ("complaint"/"suggestion"), content, reply, replyTime, status (0=pending/1=replied), createdTime. `@TableName("user_feedback")`.

Consultation: id, userId, productId, question, answer, status (0=pending/1=replied), createdTime. `@TableName("consultation")`.

- [ ] **Step 3: Create ResourceServerConfig.java**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

- [ ] **Step 4: Create application.yml**

```yaml
server:
  port: 8082
spring:
  application:
    name: user-service
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
  datasource:
    url: jdbc:mysql://localhost:3306/mall_user?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root123
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8081
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.mall.user.entity
```

- [ ] **Step 5: Create Mapper interfaces (extend BaseMapper)**

UserAddressMapper, NoticeMapper, UserFeedbackMapper, ConsultationMapper — each extends `BaseMapper<Entity>` with `@Mapper`.

- [ ] **Step 6: Commit**

```bash
git add backend/user-service/
git commit -m "feat: add user-service entities, mappers, and resource server config"
```

---

### Task 6: User Service - Service Layer

**Files:**
- Create: `backend/user-service/src/main/java/com/mall/user/service/UserAddressService.java`
- Create: `backend/user-service/src/main/java/com/mall/user/service/NoticeService.java`
- Create: `backend/user-service/src/main/java/com/mall/user/service/UserFeedbackService.java`
- Create: `backend/user-service/src/main/java/com/mall/user/service/ConsultationService.java`

- [ ] **Step 1: UserAddressService.java**

Methods: listByUserId(Long userId), add(addr), update(addr), delete(Long id), setDefault(Long id, Long userId).

Logic: When setDefault, first set all user's addresses `is_default=0`, then set this one to `is_default=1`.

- [ ] **Step 2: NoticeService.java**

Methods: listPublished(String type) — query notices with status=0. If type provided, filter by type. Order by createdTime desc.

- [ ] **Step 3: UserFeedbackService.java**

Methods: add(Long userId, type, content), listByUserId(Long userId).

- [ ] **Step 4: ConsultationService.java**

Methods: add(userId, productId, question), listByProductId(Long productId) — return answered consultations only.

- [ ] **Step 5: Commit**

```bash
git add backend/user-service/src/main/java/com/mall/user/service/
git commit -m "feat: add user-service service layer"
```

---

### Task 7: User Service - Controller

**Files:**
- Create: `backend/user-service/src/main/java/com/mall/user/controller/UserController.java`
- Create: `backend/user-service/src/main/java/com/mall/user/controller/AddressController.java`
- Create: `backend/user-service/src/main/java/com/mall/user/controller/NoticeController.java`
- Create: `backend/user-service/src/main/java/com/mall/user/controller/FeedbackController.java`
- Create: `backend/user-service/src/main/java/com/mall/user/dto/AddressRequest.java`

- [ ] **Step 1: UserController.java**

```java
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/info")
    public Result getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        // fetch from auth-service via OpenFeign (or JWT claim)
    }

    @PutMapping("/info")
    public Result updateUserInfo(@AuthenticationPrincipal Jwt jwt, @RequestBody ...) { }

    @PutMapping("/password")
    public Result updatePassword(@AuthenticationPrincipal Jwt jwt, ...) { }
}
```

Note: For user info, the JWT should contain userId as `sub` claim. For simplicity in monorepo, the user-service can read user info by making the user-service's datasource also point to `mall_auth` for the `user` table, or have a `sys_user` sync table. Simpler approach: put userId in JWT sub claim, and user-service has a UserReadMapper that reads from `mall_auth.user` (read-only).

- [ ] **Step 2: AddressController.java**

```java
@RestController
@RequestMapping("/api/user/address")
public class AddressController {
    @GetMapping("/list")        → listByUserId(userId)
    @PostMapping("/add")        → add(req, userId)
    @PutMapping("/update")      → update(req)
    @DeleteMapping("/delete/{id}") → delete(id, userId)
    @PutMapping("/default/{id}") → setDefault(id, userId)
}
```

- [ ] **Step 3: NoticeController.java**

```java
@RestController
@RequestMapping("/api/user/notice")
public class NoticeController {
    @GetMapping("/list")
    public Result list(@RequestParam(required=false) String type) {
        return Result.success(noticeService.listPublished(type));
    }
}
```

- [ ] **Step 4: FeedbackController.java**

```java
@RestController
@RequestMapping("/api/user/feedback")
public class FeedbackController {
    @PostMapping("/add")
    public Result add(@AuthenticationPrincipal Jwt jwt, @RequestBody FeedbackRequest req) { }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/user-service/src/main/java/com/mall/user/controller/
git add backend/user-service/src/main/java/com/mall/user/dto/
git commit -m "feat: add user-service controllers for address, notice, feedback"
```

---

## Phase 4: Product Service

### Task 8: Product Service - Entity, Mapper, Config

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/ProductApplication.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/Category.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/Product.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/ProductSpec.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/ProductImage.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/Comment.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/Favorite.java`
- Create: `backend/product-service/src/main/java/com/mall/product/entity/Banner.java`
- Create: `backend/product-service/src/main/java/com/mall/product/mapper/*Mapper.java` (8 mappers)
- Create: `backend/product-service/src/main/java/com/mall/product/config/ResourceServerConfig.java`
- Create: `backend/product-service/src/main/resources/application.yml`

- [ ] **Step 1: Create ProductApplication + ResourceServerConfig**

Same pattern as user-service. Port: 8083. Datasource: `mall_product`. MyBatis-Plus alias scan.

- [ ] **Step 2: Create all entities**

Map to database tables as defined in spec Section 4.2. All `@Data` + `@TableName`.

Key fields to remember:
- Product: has_spec (Integer, 0/1), with `@TableField(exist = false)` for transient fields like `specList`, `imageList`, `commentList`
- ProductSpec: extra_price (BigDecimal), stock (Integer)

- [ ] **Step 3: Create all Mapper interfaces**

CategoryMapper, ProductMapper, ProductSpecMapper, ProductImageMapper, CommentMapper, FavoriteMapper, BannerMapper — all extend BaseMapper.

- [ ] **Step 4: Create application.yml**

Port 8083, datasource mall_product, resource server pointing to auth-service on localhost:8081.

- [ ] **Step 5: Commit**

```bash
git add backend/product-service/
git commit -m "feat: add product-service entities, mappers, config"
```

---

### Task 9: Product Service - Service Layer (Part 1)

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/service/CategoryService.java`
- Create: `backend/product-service/src/main/java/com/mall/product/service/ProductService.java`
- Create: `backend/product-service/src/main/java/com/mall/product/service/BannerService.java`

- [ ] **Step 1: CategoryService.java**

Methods: listAll() — returns all categories tree (parent_id=0 as roots, children nested). Use MyBatis-Plus `selectList()` and assemble tree in Java. Add() / update() / delete().

- [ ] **Step 2: ProductService.java**

Methods:
- `list(categoryId, sortBy, page, size)` — paginated product list. sortBy: "price_asc", "price_desc", "sales_desc", "new". Filter by categoryId (including subcategories). Return Page<Product>.
- `detail(Long id)` — product + specs + images + comments count. MyBatis-Plus selectById + manual joins.
- `search(keyword, categoryId, minPrice, maxPrice, sortBy, page, size)` — like '%keyword%' search with filters.
- `add(Product)` / `update(Product)` / `delete(Long id)` — CRUD with image/spec batch save.
- `listByCategoryId(Long categoryId)` — for category page.

- [ ] **Step 3: BannerService.java**

Methods: listActive() — banners with status=0, ordered by sort asc.

- [ ] **Step 4: Commit**

```bash
git add backend/product-service/src/main/java/com/mall/product/service/
git commit -m "feat: add product-service category, product, banner services"
```

---

### Task 10: Product Service - Service Layer (Part 2)

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/service/CommentService.java`
- Create: `backend/product-service/src/main/java/com/mall/product/service/FavoriteService.java`
- Create: `backend/product-service/src/main/java/com/mall/product/service/ConsultationService.java`

- [ ] **Step 1: CommentService.java**

Methods: add(productId, userId, content, rating, imageUrls), listByProductId(productId, page, size). Rating validation 1-5.

- [ ] **Step 2: FavoriteService.java**

Methods: add(userId, productId), cancel(userId, productId), listByUserId(userId, page, size), isFavorited(userId, productId).

- [ ] **Step 3: ProductConsultationService.java**

Methods: Already handled in user-service for the data. Here in product-service, just expose `listByProductId(productId)`.

- [ ] **Step 4: Commit**

```bash
git add backend/product-service/src/main/java/com/mall/product/service/
git commit -m "feat: add product-service comment, favorite, consultation services"
```

---

### Task 11: Product Service - Controllers

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/controller/ProductController.java`
- Create: `backend/product-service/src/main/java/com/mall/product/controller/CategoryController.java`
- Create: `backend/product-service/src/main/java/com/mall/product/controller/BannerController.java`
- Create: `backend/product-service/src/main/java/com/mall/product/controller/CommentController.java`
- Create: `backend/product-service/src/main/java/com/mall/product/controller/FavoriteController.java`

- [ ] **Step 1: Create ProductController.java**

```java
@RestController
@RequestMapping("/api/product")
public class ProductController {
    @GetMapping("/list")
    public Result list(@RequestParam(required=false) Long categoryId,
                       @RequestParam(defaultValue="new") String sortBy,
                       @RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="20") int size) { }

    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) { }

    @PostMapping("/search")
    public Result search(@RequestBody SearchRequest req) { }

    @GetMapping("/spec/list/{productId}")
    public Result specs(@PathVariable Long productId) { }

    @GetMapping("/consultation/list/{productId}")
    public Result consultations(@PathVariable Long productId) { }

    @PostMapping("/consultation/add")
    public Result addConsultation(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam Long productId,
                                  @RequestParam String question) { }
}
```

- [ ] **Step 2: Create CategoryController.java**

```java
@RestController
@RequestMapping("/api/product")
public class CategoryController {
    @GetMapping("/category/list")
    public Result list() { }
}
```

- [ ] **Step 3: Create BannerController.java**

```java
@RestController
@RequestMapping("/api/product")
public class BannerController {
    @GetMapping("/banner/list")
    public Result list() { }
}
```

- [ ] **Step 4: Create CommentController.java**

```java
@RestController
@RequestMapping("/api/product")
public class CommentController {
    @GetMapping("/comment/list/{productId}")
    public Result list(@PathVariable Long productId,
                       @RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="10") int size) { }

    @PostMapping("/comment/add")
    public Result add(@AuthenticationPrincipal Jwt jwt, @RequestBody CommentRequest req) { }
}
```

- [ ] **Step 5: Create FavoriteController.java**

```java
@RestController
@RequestMapping("/api/product")
public class FavoriteController {
    @PostMapping("/favorite/add")
    public Result add(@AuthenticationPrincipal Jwt jwt, @RequestParam Long productId) { }

    @DeleteMapping("/favorite/cancel/{productId}")
    public Result cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) { }

    @GetMapping("/favorite/list")
    public Result list(@AuthenticationPrincipal Jwt jwt,
                       @RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="20") int size) { }
}
```

- [ ] **Step 6: Commit**

```bash
git add backend/product-service/src/main/java/com/mall/product/controller/
git commit -m "feat: add product-service controllers"
```

---

## Phase 5: Order Service

### Task 12: Order Service - Entity, Mapper, Config

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/OrderApplication.java`
- Create: `backend/order-service/src/main/java/com/mall/order/entity/Cart.java`
- Create: `backend/order-service/src/main/java/com/mall/order/entity/Order.java`
- Create: `backend/order-service/src/main/java/com/mall/order/entity/OrderItem.java`
- Create: `backend/order-service/src/main/java/com/mall/order/entity/Payment.java`
- Create: `backend/order-service/src/main/java/com/mall/order/mapper/*Mapper.java` (4 mappers)
- Create: `backend/order-service/src/main/java/com/mall/order/config/ResourceServerConfig.java`
- Create: `backend/order-service/src/main/resources/application.yml`

- [ ] **Step 1: OrderApplication + ResourceServerConfig + application.yml**

Port 8084. Datasource mall_order. Resource server to auth-service.

- [ ] **Step 2: Create entities**

Cart: id, userId, productId, quantity, checked, createdTime, updatedTime.
Order: id, userId, orderNo (unique), totalAmount, status, paymentMethod, paymentTime, deliveryTime, receiveTime, receiverName, receiverPhone, receiverAddress, createdTime, updatedTime.
OrderItem: id, orderId, productId, productName, productImage, price, quantity, subtotal.
Payment: id, orderId, orderNo, amount, method, status, payTime.

Order status constants: 0=pending_payment, 1=pending_delivery, 2=pending_receipt, 3=completed, 4=cancelled, 5=refunding.

- [ ] **Step 3: Create Mappers**: CartMapper, OrderMapper, OrderItemMapper, PaymentMapper.

- [ ] **Step 4: Commit**

```bash
git add backend/order-service/
git commit -m "feat: add order-service entities, mappers, config"
```

---

### Task 13: Order Service - Service Layer

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/service/CartService.java`
- Create: `backend/order-service/src/main/java/com/mall/order/service/OrderService.java`
- Create: `backend/order-service/src/main/java/com/mall/order/service/PaymentService.java`

- [ ] **Step 1: CartService.java**

Methods:
- listByUserId(Long userId) — returns cart items with joined product info (price, name, image). Since cross-service join: fetch from product-service via OpenFeign or store redundant data.
- add(userId, productId, quantity) — if exists, increment quantity; else insert.
- updateQuantity(Long cartId, int quantity)
- delete(Long cartId)
- check(Long cartId, Integer checked) — 0=uncheck, 1=check
- checkAll(Long userId, Integer checked)
- getCheckedItems(Long userId) — returns all checked items for order creation

- [ ] **Step 2: OrderService.java**

Methods:
- create(userId, addressId, paymentMethod, cartItemIds) — steps: 1. get checked cart items 2. calculate total 3. generate order no (timestamp+random) 4. create order + order items 5. clear cart items 6. return order
- listByUserId(userId, status, page, size) — filtered orders
- detail(Long orderId)
- cancel(Long orderId, Long userId) — only pending_payment orders
- confirm(Long orderId, Long userId) — set status=completed, set receiveTime
- applyRefund(Long orderId, Long userId) — set status=refunding
- adminList(status, page, size)
- adminDeliver(Long orderId) — set status=pending_receipt, set deliveryTime

Order number generation: `yyyyMMdd + 8-digit-random`.

- [ ] **Step 3: PaymentService.java**

Methods:
- pay(Long orderId, String method) — simulate payment. Set order status=pending_delivery, create payment record with status=paid.
- refund(Long orderId) — set payment status=refunded, order status=cancelled.

- [ ] **Step 4: Commit**

```bash
git add backend/order-service/src/main/java/com/mall/order/service/
git commit -m "feat: add order-service services (cart, order, payment)"
```

---

### Task 14: Order Service - Controllers

**Files:**
- Create: `backend/order-service/src/main/java/com/mall/order/controller/CartController.java`
- Create: `backend/order-service/src/main/java/com/mall/order/controller/OrderController.java`
- Create: `backend/order-service/src/main/java/com/mall/order/controller/PaymentController.java`

- [ ] **Step 1: CartController.java**

All endpoints use `@AuthenticationPrincipal Jwt jwt` to get userId. Implement cart APIs from spec 5.5.

- [ ] **Step 2: OrderController.java**

Implement: create, list, detail, cancel, confirm, refund.

- [ ] **Step 3: PaymentController.java**

Implement: pay.

- [ ] **Step 4: Commit**

```bash
git add backend/order-service/src/main/java/com/mall/order/controller/
git commit -m "feat: add order-service controllers"
```

---

## Phase 6: Gateway

### Task 15: Gateway - Route Config, OAuth2 Resource Server, CORS

**Files:**
- Create: `backend/gateway/src/main/java/com/mall/gateway/GatewayApplication.java`
- Create: `backend/gateway/src/main/java/com/mall/gateway/config/CorsConfig.java`
- Create: `backend/gateway/src/main/java/com/mall/gateway/config/RouteConfig.java`
- Create: `backend/gateway/src/main/java/com/mall/gateway/filter/AuthGlobalFilter.java`
- Create: `backend/gateway/src/main/resources/application.yml`

- [ ] **Step 1: GatewayApplication.java**

```java
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication { }
```

- [ ] **Step 2: application.yml**

```yaml
server:
  port: 8080
spring:
  application:
    name: gateway
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
    gateway:
      routes:
        - id: auth-service
          uri: lb://auth-service
          predicates:
            - Path=/api/auth/**
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
        - id: product-service
          uri: lb://product-service
          predicates:
            - Path=/api/product/**
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
```

- [ ] **Step 3: CorsConfig.java**

Allow origins: http://localhost:5173 (client), http://localhost:5174 (admin). Allow all methods, headers. Expose Authorization header.

- [ ] **Step 4: AuthGlobalFilter.java**

Global filter that extracts JWT from Authorization header, validates, and passes userId/role via headers to downstream services:
```
X-User-Id: {userId}
X-User-Role: {role}
```

- [ ] **Step 5: Commit**

```bash
git add backend/gateway/
git commit -m "feat: add gateway with routing, CORS, auth filter"
```

---

## Phase 7: Database DDL

### Task 16: Create Full SQL Init Script

**Files:**
- Modify: `deploy/mysql/init.sql`

- [ ] **Step 1: Write complete init.sql**

```sql
-- mall_auth
CREATE TABLE mall_auth.`user` ( ... ) ENGINE=InnoDB;

-- mall_user
CREATE TABLE mall_user.`user_address` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_user.`notice` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_user.`user_feedback` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_user.`consultation` ( ... ) ENGINE=InnoDB;

-- mall_product
CREATE TABLE mall_product.`category` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_product.`banner` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_product.`product` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_product.`product_spec` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_product.`product_image` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_product.`product_comment` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_product.`user_favorite` ( ... ) ENGINE=InnoDB;

-- mall_order
CREATE TABLE mall_order.`cart` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_order.`order` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_order.`order_item` ( ... ) ENGINE=InnoDB;
CREATE TABLE mall_order.`payment` ( ... ) ENGINE=InnoDB;

-- Insert sample data: admin user, categories, sample products, banners
INSERT INTO mall_auth.`user` VALUES (1, 'admin', '$2a$10$...', 'Admin', 'admin@mall.com', '', 'SUPER_ADMIN', 0, NOW(), NOW());
INSERT INTO mall_auth.`user` VALUES (2, 'user', '$2a$10$...', 'TestUser', 'user@mall.com', '', 'USER', 0, NOW(), NOW());
```

Use BCrypt hash for password "123456". Generate with Java or use a known hash.

- [ ] **Step 2: Commit**

```bash
git add deploy/mysql/init.sql
git commit -m "feat: add complete database DDL with sample data"
```

---

## Phase 8: Frontend - Client App

### Task 17: Scaffold Client VUE3 Project

**Files:**
- Create: `frontend/client/package.json`
- Create: `frontend/client/vite.config.js`
- Create: `frontend/client/index.html`
- Create: `frontend/client/src/main.js`
- Create: `frontend/client/src/App.vue`
- Create: `frontend/client/src/api/request.js` (Axios instance)
- Create: `frontend/client/src/stores/user.js` (Pinia)
- Create: `frontend/client/src/router/index.js`

- [ ] **Step 1: Initialize project**

Run: `npm create vite@latest frontend/client -- --template vue` OR manually create files.

Dependencies in package.json: vue, vue-router, pinia, axios, element-plus, @element-plus/icons-vue.

- [ ] **Step 2: vite.config.js**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

- [ ] **Step 3: Create Axios request.js**

```js
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const request = axios.create({ baseURL: '/api' })

request.interceptors.request.use(config => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = `Bearer ${store.token}`
  }
  return config
})

request.interceptors.response.use(
  res => res.data,
  error => {
    if (error.response?.status === 401) {
      const store = useUserStore()
      store.logout()
      window.location.href = '/auth/login'
    }
    ElMessage.error(error.response?.data?.message || 'Request failed')
    return Promise.reject(error)
  }
)

export default request
```

- [ ] **Step 4: Create user Pinia store**

```js
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    userInfo: null
  }),
  actions: {
    setToken(token, refreshToken) {
      this.token = token
      this.refreshToken = refreshToken
      localStorage.setItem('token', token)
      localStorage.setItem('refreshToken', refreshToken)
    },
    logout() {
      this.token = ''
      this.refreshToken = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
    }
  }
})
```

- [ ] **Step 5: Create router with route guard**

Routes: /auth/login, /auth/register, /, /product/list, /product/detail/:id, /cart, /order/create, /order/list, /order/detail/:orderNo, /user/info, /user/address, /user/favorites, /user/feedback, /service/consultation, /service/notice.

Route guard: check userStore token for protected routes, redirect to /auth/login if missing.

- [ ] **Step 6: main.js - import and use router, pinia, element-plus**

- [ ] **Step 7: Commit**

```bash
git add frontend/client/
git commit -m "feat: scaffold client frontend with Vite, Vue Router, Pinia, Axios"
```

---

### Task 18: Client Frontend - Auth Pages

**Files:**
- Create: `frontend/client/src/views/auth/LoginView.vue`
- Create: `frontend/client/src/views/auth/RegisterView.vue`
- Create: `frontend/client/src/views/auth/ForgotPasswordView.vue`
- Create: `frontend/client/src/api/auth.js`

- [ ] **Step 1: Auth API module**

```js
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const forgotPassword = (email) => request.post('/auth/forgot-password', null, { params: { email } })
```

- [ ] **Step 2: LoginView.vue**

Element Plus card centered. ElForm with username + password. "记住密码" checkbox. Submit → call login API → store token → router.push('/'). Links to register and forgot password.

- [ ] **Step 3: RegisterView.vue**

Register form: username, email, nickname, password, confirm password. Validation: email format, password length >= 6. Submit → call register → success message → redirect to login.

- [ ] **Step 4: ForgotPasswordView.vue**

Email input → submit → show "reset link sent" message.

- [ ] **Step 5: Commit**

```bash
git add frontend/client/src/views/auth/
git add frontend/client/src/api/auth.js
git commit -m "feat: add client auth pages (login, register, forgot password)"
```

---

### Task 19: Client Frontend - Home Page

**Files:**
- Create: `frontend/client/src/views/home/HomeView.vue`
- Create: `frontend/client/src/components/HeaderNav.vue`
- Create: `frontend/client/src/api/product.js`

- [ ] **Step 1: product.js API**

```js
export const getBanners = () => request.get('/product/banner/list')
export const getCategories = () => request.get('/product/category/list')
export const getProducts = (params) => request.get('/product/list', { params })
export const getProductDetail = (id) => request.get(`/product/detail/${id}`)
export const searchProducts = (data) => request.post('/product/search', data)
```

- [ ] **Step 2: HeaderNav.vue**

Top navigation bar: Logo (left), Category dropdown (middle) also navigation items: 首页, 商品, 购物车(with badge), 个人中心, 登录/用户信息.

- [ ] **Step 3: HomeView.vue**

Layout:
1. Element Plus ElCarousel for banners
2. Category navigation cards (一级分类 displayed, click to filter)
3. Recommended products grid (ElRow + ElCol, each product card with image, name, price)
4. Search bar in header

- [ ] **Step 4: Commit**

```bash
git add frontend/client/src/views/home/
git add frontend/client/src/components/HeaderNav.vue
git add frontend/client/src/api/product.js
git commit -m "feat: add client home page with banners, categories, recommended products"
```

---

### Task 20: Client Frontend - Product Pages

**Files:**
- Create: `frontend/client/src/views/product/ProductList.vue`
- Create: `frontend/client/src/views/product/ProductDetail.vue`

- [ ] **Step 1: ProductList.vue**

Product grid with filters: sidebar category tree, sort by (price asc/desc, sales, newest), pagination. Each card = image + name + price.

- [ ] **Step 2: ProductDetail.vue**

Layout: left = main image + thumbnail list; right = name, price, stock, description, spec selection (if has_spec), quantity selector, "Add to Cart" button.
Below: tab panel with "商品详情" (description), "评论" (comments list with rating), "咨询" (consultations).

- [ ] **Step 3: Commit**

```bash
git add frontend/client/src/views/product/
git commit -m "feat: add client product list and detail pages"
```

---

### Task 21: Client Frontend - Cart and Order Pages

**Files:**
- Create: `frontend/client/src/views/cart/CartView.vue`
- Create: `frontend/client/src/views/order/CreateOrderView.vue`
- Create: `frontend/client/src/views/order/OrderList.vue`
- Create: `frontend/client/src/views/order/OrderDetail.vue`
- Create: `frontend/client/src/api/order.js`

- [ ] **Step 1: cart.js API**

```js
export const getCartList = () => request.get('/order/cart/list')
export const addToCart = (data) => request.post('/order/cart/add', data)
export const updateCart = (data) => request.put('/order/cart/update', data)
export const deleteCartItem = (id) => request.delete(`/order/cart/delete/${id}`)
export const checkCart = (data) => request.post('/order/cart/check', data)
export const checkAllCart = (data) => request.post('/order/cart/checkAll', data)
```

- [ ] **Step 2: order.js API**

```js
export const createOrder = (data) => request.post('/order/create', data)
export const getOrders = (params) => request.get('/order/list', { params })
export const getOrderDetail = (orderNo) => request.get(`/order/detail/${orderNo}`)
export const payOrder = (data) => request.post('/order/pay', data)
export const cancelOrder = (orderNo) => request.post(`/order/cancel/${orderNo}`)
export const confirmOrder = (orderNo) => request.post(`/order/confirm/${orderNo}`)
export const applyRefund = (data) => request.post('/order/refund/apply', data)
```

- [ ] **Step 3: CartView.vue**

ElTable: checkbox, image, name, unit price, quantity (ElInputNumber), subtotal, delete. Below: select all checkbox, total display, "结算" button.

- [ ] **Step 4: CreateOrderView.vue**

Address selection (radio group), order items list (read-only), total display. "提交订单" button. After submit → redirect to order detail with payment prompt.

- [ ] **Step 5: OrderList.vue**

Tabs: 全部/待付款/待发货/待收货/待评价/退款. Each tab shows orders with status badge + product list + total + action buttons (pay, cancel, confirm receipt, apply refund).

- [ ] **Step 6: OrderDetail.vue**

Order status progress (ElSteps), shipping address, order items, payment info, price breakdown. Action buttons based on status.

- [ ] **Step 7: Commit**

```bash
git add frontend/client/src/views/cart/ frontend/client/src/views/order/
git add frontend/client/src/api/order.js
git commit -m "feat: add client cart and order pages"
```

---

### Task 22: Client Frontend - User Pages and Service Pages

**Files:**
- Create: `frontend/client/src/views/user/UserInfo.vue`
- Create: `frontend/client/src/views/user/AddressList.vue`
- Create: `frontend/client/src/views/user/Favorites.vue`
- Create: `frontend/client/src/views/user/FeedbackList.vue`
- Create: `frontend/client/src/views/service/Consultation.vue`
- Create: `frontend/client/src/views/service/NoticeList.vue`

- [ ] **Step 1: UserInfo.vue**

Profile card: avatar (upload), nickname, email. Password change form: old password, new password, confirm.

- [ ] **Step 2: AddressList.vue**

List of address cards. Each card: receiver, phone, address, tags (default). Buttons: edit, delete, set default. "Add address" form dialog.

- [ ] **Step 3: Favorites.vue**

Product grid of favorited items. Click to go to product detail. Heart icon to unfavorite.

- [ ] **Step 4: FeedbackList.vue**

List of submitted feedbacks. "New feedback" button with form (type: complaint/suggestion, content textarea). Display replies.

- [ ] **Step 5: Consultation.vue**

Product consultation list. User can select product and ask question.

- [ ] **Step 6: NoticeList.vue**

List of system/activity notices from `/api/user/notice/list`. Click to expand detail.

- [ ] **Step 7: Commit**

```bash
git add frontend/client/src/views/user/ frontend/client/src/views/service/
git commit -m "feat: add client user center and service pages"
```

---

## Phase 9: Frontend - Admin App

### Task 23: Scaffold Admin VUE3 Project

**Files:**
- Create: `frontend/admin/package.json`
- Create: `frontend/admin/vite.config.js`
- Create: `frontend/admin/index.html`
- Create: `frontend/admin/src/main.js`
- Create: `frontend/admin/src/App.vue`
- Create: `frontend/admin/src/api/request.js`
- Create: `frontend/admin/src/stores/user.js`
- Create: `frontend/admin/src/router/index.js`
- Create: `frontend/admin/src/layouts/AdminLayout.vue`

- [ ] **Step 1: Create project files**

Same pattern as client but port 5174. Vite proxy same target localhost:8080.

- [ ] **Step 2: AdminLayout.vue**

Element Plus container layout: `el-container` > `el-aside` (sidebar menu) + `el-container` > `el-header` (top bar with admin info + logout) + `el-main` (router-view).

Sidebar menu items: Dashboard, User Management, Product Management, Order Management, Content (Banner, Notice), System (Feedback, Consultation, Profile).

- [ ] **Step 3: router + route guard**

All routes require ADMIN role. Guard reads role from JWT (stored in userStore after login).

- [ ] **Step 4: Commit**

```bash
git add frontend/admin/
git commit -m "feat: scaffold admin frontend with layout and routing"
```

---

### Task 24: Admin Frontend - Dashboard and User Management

**Files:**
- Create: `frontend/admin/src/views/dashboard/DashboardView.vue`
- Create: `frontend/admin/src/views/user/UserList.vue`
- Create: `frontend/admin/src/api/admin.js`

- [ ] **Step 1: DashboardView.vue**

Statistics cards: total users, total orders, total sales, today orders, today sales (use ElStatistic or custom cards).
Charts (optional): order trend line chart (use simple ECharts or chart.js), product sales ranking.

- [ ] **Step 2: UserList.vue**

ElTable with columns: ID, username, nickname, email, role, status, created time. Search bar: username/phone/nickname. Actions: toggle disable/enable. Pagination.

- [ ] **Step 3: admin.js API**

```js
export const getDashboard = () => request.get('/admin/dashboard/statistics')
export const getUsers = (params) => request.get('/admin/user/list', { params })
export const disableUser = (id) => request.put(`/admin/user/disable/${id}`)
```

- [ ] **Step 4: Commit**

```bash
git add frontend/admin/src/views/dashboard/ frontend/admin/src/views/user/
git commit -m "feat: add admin dashboard and user management pages"
```

---

### Task 25: Admin Frontend - Product and Order Management

**Files:**
- Create: `frontend/admin/src/views/product/ProductList.vue`
- Create: `frontend/admin/src/views/product/ProductForm.vue`
- Create: `frontend/admin/src/views/product/CategoryList.vue`
- Create: `frontend/admin/src/views/product/CommentList.vue`
- Create: `frontend/admin/src/views/order/OrderList.vue`
- Create: `frontend/admin/src/views/order/OrderDetail.vue`

- [ ] **Step 1: ProductList.vue**

Table: ID, image, name, category, price, stock, sales, status (上架/下架), created. Search: name, category. Actions: add, edit, delete (confirm dialog), toggle status.

- [ ] **Step 2: ProductForm.vue**

Dialog form: name, category (select), price, stock, description (rich text or textarea), main image (ElUpload), additional images. Spec management: dynamic rows for spec_name/spec_value/extra_price.

- [ ] **Step 3: CategoryList.vue**

Tree table for hierarchical categories. Add/edit/delete buttons.

- [ ] **Step 4: CommentList.vue**

Table: product name, user, rating, content, images, created time. Delete button for violating comments.

- [ ] **Step 5: OrderList.vue**

Table: order no, user, total, status (tag), payment method, created. Tabs for status filter. Actions: view detail, deliver (for pending_delivery), refund (for refunding).

- [ ] **Step 6: OrderDetail.vue**

Order info, address info, product items table, payment info, action buttons.

- [ ] **Step 7: Commit**

```bash
git add frontend/admin/src/views/product/ frontend/admin/src/views/order/
git commit -m "feat: add admin product and order management pages"
```

---

### Task 26: Admin Frontend - Content and System Management

**Files:**
- Create: `frontend/admin/src/views/content/BannerList.vue`
- Create: `frontend/admin/src/views/content/NoticeList.vue`
- Create: `frontend/admin/src/views/system/FeedbackList.vue`
- Create: `frontend/admin/src/views/system/ConsultationList.vue`
- Create: `frontend/admin/src/views/system/ProfileView.vue`

- [ ] **Step 1: BannerList.vue**

Table: image (thumbnail), title, link, sort, status, created. Add/edit dialog: image upload, title, link URL, sort order, status toggle. Delete with confirmation.

- [ ] **Step 2: NoticeList.vue**

Table: title, type (tag), status, created time. Add/edit dialog: title, type (radio: system/activity), content (textarea), status. Delete button.

- [ ] **Step 3: FeedbackList.vue**

Table: user, type, content, status (pending/replied), created. Click to view detail + reply dialog. Mark as replied.

- [ ] **Step 4: ConsultationList.vue**

Table: user, product (link), question, status, created. Click to view + answer textarea + reply button.

- [ ] **Step 5: ProfileView.vue**

Admin profile edit: nickname, email, password change.

- [ ] **Step 6: Create admin API methods**

Add all admin API calls: /api/admin/banner/*, /api/admin/notice/*, /api/admin/feedback/*, /api/admin/consultation/*.

- [ ] **Step 7: Commit**

```bash
git add frontend/admin/src/views/content/ frontend/admin/src/views/system/
git commit -m "feat: add admin content and system management pages"
```

---

## Phase 10: Deployment & CI/CD

### Task 27: Dockerfiles for Each Service

**Files:**
- Create: `backend/gateway/Dockerfile`
- Create: `backend/auth-service/Dockerfile`
- Create: `backend/user-service/Dockerfile`
- Create: `backend/product-service/Dockerfile`
- Create: `backend/order-service/Dockerfile`

- [ ] **Step 1: Create multi-stage Dockerfiles**

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY gateway/pom.xml gateway/
RUN mvn dependency:go-offline -pl gateway -am
COPY . .
RUN mvn clean package -pl gateway -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Create same pattern for all 5 services. Adjust port, service module name, jar path.

- [ ] **Step 2: Update docker-compose.yml**

Add all service images (build context = ./backend, dockerfile = ./{service}/Dockerfile). Define depends_on: nacos, mysql, redis for all services.

- [ ] **Step 3: Create deploy/nginx/nginx.conf**

Nginx config: serve frontend static files, proxy /api/* to gateway:8080.

- [ ] **Step 4: Commit**

```bash
git add backend/*/Dockerfile deploy/
git commit -m "feat: add Dockerfiles and update docker-compose with all services"
```

---

### Task 28: GitHub Actions CI/CD

**Files:**
- Create: `.github/workflows/ci.yml`

- [ ] **Step 1: Create CI workflow**

```yaml
name: CI

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean package -DskipTests
        working-directory: ./backend
      - name: Build Docker images
        run: docker compose build
        working-directory: ./backend
      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: Push images
        run: docker compose push
        working-directory: ./backend
```

- [ ] **Step 2: Commit**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions CI workflow"
```

---

## Phase 11: Nacos Config & Redis Caching

### Task 29: Redis Cache Integration

**Files:**
- Create: `backend/product-service/src/main/java/com/mall/product/config/RedisConfig.java`
- Modify: `backend/product-service/src/main/java/com/mall/product/service/ProductService.java`
- Modify: `backend/product-service/src/main/java/com/mall/product/service/CategoryService.java`
- Modify: `backend/auth-service/src/main/java/com/mall/auth/config/RedisConfig.java`
- Modify: `backend/auth-service/src/main/java/com/mall/auth/service/AuthService.java`

- [ ] **Step 1: Create RedisConfig (product-service)**

```java
@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 2: Cache product detail**

In ProductService.detail(): check Redis key `product:detail:{id}` first → if hit, return cached; else query DB, cache with TTL 30min, return.

- [ ] **Step 3: Cache category list**

In CategoryService.listAll(): cache key `product:category:list` with TTL 1h. Invalidate on add/update/delete.

- [ ] **Step 4: Token blacklist in auth-service**

In AuthService.logout(): store refreshToken in Redis with key `token:blacklist:{refreshToken}` and TTL = remaining token lifetime.

In AuthService.refreshToken(): check Redis blacklist before refreshing.

- [ ] **Step 5: Commit**

```bash
git add backend/product-service/src/main/java/com/mall/product/config/
git add backend/auth-service/src/main/java/com/mall/auth/config/
git commit -m "feat: add Redis caching for product detail, categories, and token blacklist"
```

---

### Task 30: Add Spring Boot Configuration for Nacos Config (Optional - use bootstrap.yml or application.yml)

**Files:**
- Modify: `backend/auth-service/src/main/resources/application.yml`
- Modify: `backend/gateway/src/main/resources/application.yml` (if needed)

- [ ] **Step 1: Add Nacos Config dependency to each service POM**

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
```

- [ ] **Step 2: Commit**

```bash
git add backend/*/pom.xml backend/*/src/main/resources/
git commit -m "feat: add nacos config dependency for centralized configuration"
```

---

## Phase 12: Verification Script and Final Polish

### Task 31: Add Quick-Start Script

**Files:**
- Create: `deploy/start.sh`

- [ ] **Step 1: Create start script**

```bash
#!/bin/bash
# Start infrastructure
docker compose -f deploy/docker-compose.yml up -d nacos mysql redis

# Wait for services
echo "Waiting for MySQL and Nacos to start..."
sleep 30

# Start backend services (in order)
cd backend

echo "Building all services..."
mvn clean package -DskipTests

echo "Starting services with Docker Compose..."
docker compose -f ../deploy/docker-compose.yml up -d

# Start frontend
cd ../frontend/client && npm install && npm run dev &
cd ../frontend/admin && npm install && npm run dev &

echo "System started!"
echo "Client: http://localhost:5173"
echo "Admin: http://localhost:5174"
echo "Gateway: http://localhost:8080"
```

- [ ] **Step 2: Commit**

```bash
git add deploy/start.sh
chmod +x deploy/start.sh
git commit -m "chore: add quick-start script"
```

---

## Self-Review Verification

Checklist against spec:

| Spec Requirement | Covered In |
|-----------------|------------|
| User register/login/logout/forgot password | Task 4 (auth controller/service) |
| User info CRUD, password change | Task 7 (UserController) |
| Address CRUD + default | Task 7 (AddressController) |
| Product categories (tree) | Task 9 (CategoryService) |
| Product CRUD with images | Task 9 (ProductService) |
| Product spec (color/size) | Task 9 (Product entity + ProductSpec) |
| Product search/sort | Task 9 (ProductService.search) |
| Product favorites | Task 10 (FavoriteService) |
| Product comments with images | Task 10 (CommentService) |
| Banners display + admin CRUD | Task 9 (BannerService) + Task 26 |
| Cart add/list/update/delete/check | Task 13 (CartService) |
| Order create/pay/cancel/confirm/refund | Task 13 (OrderService) |
| Order list with status filter | Task 13 (OrderService.listByUserId) |
| Admin dashboard statistics | Task 24 (DashboardView) |
| Admin user management | Task 24 (UserList) |
| Admin product/category/comment mgmt | Task 25 |
| Admin order management + export | Task 25 |
| Admin notice/banner CRUD | Task 26 |
| Admin feedback reply | Task 26 |
| Admin consultation reply | Task 26 |
| Notices public display | Task 6 (NoticeService) + Task 7 (NoticeController) |
| User feedback submit | Task 6 + Task 7 |
| Customer consultation | Task 6 + Task 11 (Consultation) |
| OAuth2 auth flow | Task 3 + Task 4 |
| Service discovery (Nacos) | All service application.yml |
| Redis caching | Task 29 |
| Docker containerization | Task 27 |
| GitHub Actions | Task 28 |
| Responsive design | Element Plus responsive |
| Image upload | ElUpload in forms |
| Pagination | MyBatis-Plus Page + ElPagination |
