# 前端重构设计需求文档

> 基于现有代码解析提取，适用于完整重构参考。

---

## 1. 概述

### 1.1 技术栈

| 项目 | 客户端 | 管理端 |
|------|--------|--------|
| 框架 | Vue 3 (Composition API + `<script setup>`) | Vue 3 |
| 构建 | Vite 5 | Vite 5 |
| 路由 | Vue Router 4 (createWebHistory) | Vue Router 4 |
| 状态管理 | Pinia | Pinia |
| UI 库 | Element Plus | Element Plus |
| 图标 | @element-plus/icons-vue | @element-plus/icons-vue |
| HTTP | Axios | Axios |

### 1.2 架构设计要点

- 两个独立 SPA：`client/`（C端用户）和 `admin/`（管理后台）
- 共用同一个后端 API（`/api` 代理到 `localhost:8080`）
- 客户端用 `@` 路径别名指向 `src/`
- 客户端端口 5173，管理端端口 5174

---

## 2. 客户端（C端商城）

### 2.1 路由与页面清单

| 路由 | 页面 | 说明 | 需登录 |
|------|------|------|--------|
| `/` | HomeView | 首页（Hero + 分类 + 推荐商品） | 否 |
| `/auth/login` | LoginView | 登录（记住用户名 + 重定向） | 否 |
| `/auth/register` | RegisterView | 注册（用户名/邮箱/昵称/密码） | 否 |
| `/auth/forgot-password` | ForgotPasswordView | 忘记密码（邮箱重置） | 否 |
| `/product/list` | ProductListView | 商品列表（分类+排序+分页） | 否 |
| `/product/detail/:id` | ProductDetailView | 商品详情（图片/规格/评论/咨询） | 否 |
| `/cart` | CartView | 购物车（勾选/数量/删除/结算） | 是 |
| `/order/create` | OrderCreateView | 创建订单（地址+商品+支付方式） | 是 |
| `/order/list` | OrderListView | 订单列表（状态标签筛选） | 是 |
| `/order/detail/:id` | OrderDetailView | 订单详情（进度+信息+操作） | 是 |
| `/user/info` | UserInfoView | 个人信息（资料+密码修改） | 是 |
| `/user/address` | UserAddressView | 收货地址管理（CRUD） | 是 |
| `/user/favorites` | UserFavoritesView | 我的收藏（网格展示） | 是 |
| `/user/feedback` | UserFeedbackView | 意见反馈（列表+新建） | 是 |
| `/service/notice` | NoticeView | 系统公告（分类筛选+展开） | 否 |
| `/service/consultation` | ConsultationView | 售前咨询（列表+新建） | 是 |

### 2.2 页面详细需求

#### 2.2.1 HeaderNav（全局导航栏）
- Logo + 品牌名（"暖 Warmart"）
- 导航链接：首页、商品
- 分类下拉菜单（hover 展示顶级分类列表）
- 搜索栏（展开式动画，回车搜索跳转 `/product/list?keyword=xxx`）
- 购物车图标（角标显示数量）
- 用户菜单：未登录 → 登录/注册按钮；已登录 → 头像+昵称下拉（个人信息/我的订单/我的收藏/退出）
- 滚动监听：透明背景 → 滚动后毛玻璃+阴影效果
- 响应式：768px 以下隐藏导航文字

#### 2.2.2 HomeView（首页）
- **Hero 区域**：全屏背景（模糊圆形渐变装饰）+ 左侧文案+CTA 按钮 + 右侧轮播图（Banner 列表，带占位态）
- **全部品类**：网格展示顶级分类卡片（最多12个，带图标和颜色），IntersectionObserver 触发的滚动渐入动画
- **推荐好物**：第一个商品作为大图展示 + 其余网格排列，骨架屏加载态，无数据时的 `<el-empty>` 空态
- **特色条**：品质保障/极速发货/无忧退换/贴心服务 4 个 icon 项
- **页脚**：品牌信息 + 链接 + 版权

#### 2.2.3 LoginView（登录页）
- 居中卡片式布局 + 背景装饰
- 用户名+密码表单（自定义 SVG 图标前缀）
- "记住密码"（存 localStorage rememberedUsername）
- 表单验证（用户名必填，密码必填+最少6位）
- 登录后按 `redirect` 参数跳转，默认回首页
- Element Plus Message 反馈
- 底部链接：去注册、忘记密码

#### 2.2.4 RegisterView（注册页）
- Element Plus Card 布局
- 字段：用户名、邮箱、昵称、密码、确认密码
- 验证：邮箱格式、密码一致性、密码长度 >= 6
- 注册成功跳转登录页

#### 2.2.5 ForgotPasswordView（忘记密码）
- Card 布局，输入邮箱，调用发送重置链接 API
- 邮箱格式验证

#### 2.2.6 ProductListView（商品列表）
- **搜索栏**：关键词输入 + 搜索按钮
- **分类侧边栏**：`el-tree` 组件展示分类树，点击筛选
- **排序栏**：综合/价格升序/价格降序/销量/最新
- **商品网格**：`el-row` + `el-col` 响应式，商品卡片带图片、名称、价格
- **分页**：`el-pagination`（总数+页码跳转）
- 状态：loading、空数据

#### 2.2.7 ProductDetailView（商品详情）
- **图片画廊**：主图 + 缩略图列表，多种图片字段兼容（imageList/images/image）
- **商品信息**：名称、价格（支持原价+划线价）、描述、库存
- **规格选择**：动态 specGroups（名称+选项多选），选中后累加 extra_price
- **数量选择**：`el-input-number`，上限 = stock
- **加入购物车**：调用 `/order/cart/add`
- **底部 Tabs**：
  - 商品详情（v-html 富文本）
  - 评论（用户头像+评分+内容+图片+分页）
  - 咨询（Q&A 形式，已回复/未回复）
- 懒加载：评论和咨询数据按需切换 Tab 时拉取

#### 2.2.8 CartView（购物车）
- `el-table` 展示：勾选 → 商品图+名称 → 单价 → 数量（InputNumber） → 小计 → 删除按钮
- 全选/反选（支持不确定态 indeterminate）
- 底部汇总：选中数量总金额，结算按钮跳转 `/order/create?cartItemIds=xxx`

#### 2.2.9 OrderCreateView（创建订单）
- 收货地址表单（手动填写：收件人/电话/地址）
- 选中商品列表（只读展示）
- 支付方式选择：支付宝/微信支付/银行卡
- 提交订单 → 跳转订单详情

#### 2.2.10 OrderListView（订单列表）
- Tab 筛选：全部/待付款(0)/待发货(1)/待收货(2)/已完成(3)/已取消(4)/退款中(5)
- 订单卡片：订单号+状态Tag + 商品列表 + 合计金额
- 操作按钮：支付/取消/确认收货/申请退款/详情（按状态条件显示）
- 分页

#### 2.2.11 OrderDetailView（订单详情）
- `el-steps` 进度条：提交→支付→发货→收货→完成
- 收货信息/商品信息/支付信息 三块卡片
- 操作按钮（同列表页的按状态）
- 特殊状态 Tag：已取消、退款中

#### 2.2.12 UserInfoView（个人信息）
- 基本资料 Card：头像（file input + base64 预览）、用户名(disabled)、昵称、邮箱(disabled)、手机号
- 修改密码 Card：原密码/新密码/确认新密码
- 前端验证 + 保存按钮

#### 2.2.13 UserAddressView（收货地址管理）
- 地址列表卡片：收件人+电话+省市区+详细地址+默认Tag
- 操作：设为默认/编辑/删除
- Dialog CRUD：收件人/电话/省市区（三个 input）/详细地址/默认开关
- 验证：手机号格式 `^1\d{10}$`

#### 2.2.14 UserFavoritesView（我的收藏）
- 网格布局展示收藏商品（图+名+价）
- 悬停效果，点击跳转商品详情
- 取消收藏（确认对话框）

#### 2.2.15 UserFeedbackView（意见反馈）
- 反馈列表卡片：类型Tag + 状态Tag + 时间 + 内容 + 回复内容
- Dialog 新建：类型选择（建议/投诉/咨询）+ 内容 textarea（maxlength=500 + 字数统计）+ 联系方式（选填）
- 验证：内容至少5字符

#### 2.2.16 NoticeView（系统公告）
- 筛选 RadioButton：全部/系统通知/促销活动
- 公告卡片列表：类型Tag + 标题 + 日期 + 内容（超过150字可展开/收起）

#### 2.2.17 ConsultationView（售前咨询）
- 咨询列表：商品名 + 问题 + 回复/待回复
- Dialog 新建：输入商品ID + 咨询内容
- 验证：商品ID必填数字>=1，内容至少5字符

### 2.3 API 接口清单

| 模块 | 接口 | 方法 |
|------|------|------|
| Auth | `/auth/login` | POST |
| Auth | `/auth/register` | POST |
| Auth | `/auth/forgot-password` | POST |
| User | `/user/info` | GET |
| Product | `/product/banner/list` | GET |
| Product | `/product/category/list` | GET |
| Product | `/product/list` | GET |
| Product | `/product/detail/:id` | GET |
| Product | `/product/search` | POST |
| Product | `/product/comment/list/:productId` | GET |
| Product | `/product/comment/add` | POST |
| Product | `/product/consultation/list/:productId` | GET |
| Product | `/product/favorite/check/:productId` | GET |
| Product | `/product/favorite/add` | POST |
| Product | `/product/favorite/cancel/:productId` | DELETE |
| Cart | `/order/cart/list` | GET |
| Cart | `/order/cart/add` | POST |
| Cart | `/order/cart/update` | PUT |
| Cart | `/order/cart/delete/:id` | DELETE |
| Cart | `/order/cart/check` | PUT |
| Cart | `/order/cart/checkAll` | PUT |
| Order | `/order/create` | POST |
| Order | `/order/list` | GET |
| Order | `/order/detail/:id` | GET |
| Order | `/order/cancel/:id` | POST |
| Order | `/order/confirm/:id` | POST |
| Order | `/order/refund/:id` | POST |
| Order | `/order/payment/pay` | POST |
| User | `/user/info` | GET/PUT |
| User | `/user/password` | PUT |
| User | `/user/address/list` | GET |
| User | `/user/address/add` | POST |
| User | `/user/address/update` | PUT |
| User | `/user/address/delete/:id` | DELETE |
| User | `/user/address/default/:id` | PUT |
| User | `/product/favorite/list` | GET |
| User | `/user/feedback/list` | GET |
| User | `/user/feedback/add` | POST |
| User | `/user/notice/list` | GET |
| User | `/user/consultation/list` | GET |
| User | `/user/consultation/add` | POST |

### 2.4 数据流

- **认证流程**：登录 → 后端返回 token + refreshToken → Pinia store + localStorage → Axios 拦截器自动注入 `Authorization: Bearer <token>` → 401 响应触发 logout + 重定向
- **通用 API 响应**：Axios 拦截器统一提取 `res.data`，错误统一 `ElMessage.error`
- **数据获取**：页面 `onMounted` 拉取，部分页面按需（如评论 Tab 切换）

### 2.5 状态管理

**User Store：**
```javascript
state: {
  token: localStorage.getItem('token') || '',
  refreshToken: localStorage.getItem('refreshToken') || '',
  userInfo: null
}
actions: setToken, logout
```

### 2.6 响应式断点

- 768px 以下：导航简化、布局切换
- 1024px 以下：双列转单列
- 640px 以下：网格缩减、内边距缩小

---

## 3. 管理端

### 3.1 路由与页面清单

| 路由 | 页面 | 说明 |
|------|------|------|
| `/login` | LoginView | 管理员登录 |
| `/` | AdminLayout | 侧边栏+顶栏布局 |
| `/dashboard` | DashboardView | 控制台（统计卡片+最近订单） |
| `/users` | UserListView | 用户管理（搜索+分页+状态切换） |
| `/products` | ProductListView | 商品管理（CRUD+规格+图片） |
| `/categories` | CategoryView | 分类管理（树形+CRUD） |
| `/orders` | OrderListView | 订单管理（搜索+详情+发货+退款） |
| `/banners` | BannerView | 轮播管理（CRUD+排序） |
| `/notices` | NoticeView | 公告管理（CRUD+发布/草稿） |
| `/feedbacks` | FeedbackView | 反馈管理（列表+回复） |
| `/consultations` | ConsultationView | 咨询管理（列表+回复） |
| `/profile` | ProfileView | 个人信息+密码修改 |

### 3.2 布局（AdminLayout）
- 左侧固定侧边栏（220px，深色 #304156）
- `el-menu` 分组：控制台/用户管理/商品管理(子菜单:商品+分类)/订单管理/内容管理(子菜单:轮播+公告)/系统(子菜单:反馈+咨询+个人信息)
- 顶部 Header：显示用户名 + 退出按钮

### 3.3 页面详细需求

#### 3.3.1 LoginView
- 渐变背景（purple-blue gradient）
- Card 居中，图标+标题，用户名+密码表单
- 登录成功 → setToken → 跳转 `/dashboard`

#### 3.3.2 DashboardView
- 4 个统计卡片：用户总数/订单总数/销售总额/今日订单（渐变色背景）
- 最近订单表格（ID/订单号/金额/状态）
- 欢迎卡片

#### 3.3.3 UserListView
- 搜索栏：关键词（用户名/昵称）
- 表格：ID/用户名/昵称/邮箱/角色(USER/ADMIN/SUPER_ADMIN)/状态(正常/禁用)/创建时间
- 操作：切换启用/禁用（确认对话框）
- 分页（支持 pageSizes）

#### 3.3.4 ProductListView
- 搜索栏：关键词 + 分类筛选
- 表格：ID/图片/名称/分类/价格/库存/销量/状态(上架/下架)/创建时间
- 操作：编辑/上架下架/删除
- Dialog CRUD：
  - 基础字段：名称/描述/价格/原价/库存/分类/图片URL/推荐开关
  - 动态规格：多组（规格名/规格值/加价/库存）
  - 动态相册：多张图片 URL
  - 验证：名称/价格/分类/库存必填

#### 3.3.5 CategoryView
- 树形表格展示（支持 children 展开）
- 列：名称/排序/图标/创建时间
- Dialog CRUD：名称/上级分类/排序/图标URL
- 验证：名称必填，支持顶级分类(parentId=0)

#### 3.3.6 OrderListView
- 搜索栏：订单状态 + 日期范围选择
- 表格：订单号/用户ID/收货人/金额/状态/支付方式/创建时间
- 操作：查看详情
- **详情 Dialog**：订单信息/收货人信息/商品信息/支付信息
- **操作按钮**（按状态渲染）：
  - 待付款 → 标记已付款
  - 待发货 → 发货（弹出 ShipDialog：快递公司+快递单号）
  - 待收货 → 标记已收货
  - 退款中 → 同意退款/拒绝退款
- **发货 Dialog**：快递公司+快递单号（必填验证）

#### 3.3.7 BannerView
- 表格：ID/标题/图片/链接URL/排序/状态(显示/隐藏)/创建时间
- 操作：编辑/删除/上移/下移
- Dialog CRUD：标题/图片地址/链接地址/排序/状态开关

#### 3.3.8 NoticeView
- 表格：ID/标题/类型(system/promotion/activity)/状态(published/draft)/创建时间/更新时间
- 操作：编辑/删除/发布(切换published/draft)
- Dialog CRUD：标题/类型选择/内容(textarea)/状态开关

#### 3.3.9 FeedbackView
- 表格：ID/用户ID/类型/内容(截断)/状态(pending/resolved)/创建时间
- 操作：查看/处理
- 详情 Dialog：展示完整内容 + 联系方式 + 已有回复
- 处理：textarea 输入回复内容，提交

#### 3.3.10 ConsultationView
- 表格：ID/用户ID/商品ID/内容(截断)/状态(pending/resolved)/创建时间
- 操作：回复
- 回复 Dialog：展示用户问题 + textarea 回复 + 提交

#### 3.3.11 ProfileView
- 基本信息 Card：用户名(disabled)/邮箱/手机号/头像URL + 预览
- 修改密码 Card：旧密码/新密码/确认密码
- 验证：邮箱格式、手机号 `^1[3-9]\d{9}$`

### 3.4 管理端 API

| 接口 | 方法 | 用途 |
|------|------|------|
| `/auth/login` | POST | 管理员登录 |
| `/auth/admin/users` | GET | 用户列表 |
| `/auth/admin/user/status/:id` | PUT | 切换用户状态 |
| `/auth/admin/dashboard` | GET | 控制台统计 |
| `/product/list` | GET | 商品列表 |
| `/product/detail/:id` | GET | 商品详情 |
| `/product/add` | POST | 添加商品 |
| `/product/update` | PUT | 更新商品 |
| `/product/delete/:id` | DELETE | 删除商品 |
| `/product/status/:id` | PUT | 上架/下架 |
| `/product/category/list` | GET | 分类列表 |
| `/product/category/add` | POST | 添加分类 |
| `/product/category/update` | PUT | 更新分类 |
| `/product/category/delete/:id` | DELETE | 删除分类 |
| `/product/banner/list` | GET | Banner 列表 |
| `/product/banner/add` | POST | 添加 Banner |
| `/product/banner/update` | PUT | 更新 Banner |
| `/product/banner/delete/:id` | DELETE | 删除 Banner |
| `/order/list` | GET | 订单列表 |
| `/order/detail/:id` | GET | 订单详情 |
| `/order/admin/status/:id` | PUT | 更新订单状态 |
| `/order/admin/ship/:id` | PUT | 发货 |
| `/user/notice/list` | GET | 公告列表 |
| `/user/notice/add` | POST | 添加公告 |
| `/user/notice/update` | PUT | 更新公告 |
| `/user/notice/delete/:id` | DELETE | 删除公告 |
| `/user/feedback/list` | GET | 反馈列表 |
| `/user/feedback/reply/:id` | PUT | 回复反馈 |
| `/user/consultation/list` | GET | 咨询列表 |
| `/user/consultation/reply/:id` | PUT | 回复咨询 |
| `/user/profile/update` | PUT | 更新管理员信息 |
| `/user/profile` | GET | 获取管理员信息 |
| `/user/password` | PUT | 修改密码 |

### 3.5 管理端状态管理

**User Store：**
```javascript
state: {
  token: localStorage.getItem('admin_token') || '',
  userInfo: null
}
getters: isLoggedIn
actions: setToken, setUserInfo, logout
```

---

## 4. 可复用的设计系统 & UX 模式

### 4.1 设计视觉风格（客户端 Warm·Zen）

- 暖色调品牌：主题色 `#c0664a`（accent），背景 `#f7f3ee`（warm beige）
- 圆角体系：8px/16px/24px/32px
- 阴影层次：sm/md/lg
- 字体：`Noto Serif SC` 用于展示标题，系统字体用于正文
- 品牌标识："暖" 字 logo + "Warmart"
- 毛玻璃效果（导航栏滚动后 backdrop-filter）
- 模糊圆形装饰背景（hero, login）
- 骨架屏 shimmer 加载动画
- IntersectionObserver 驱动的滚动渐入动画
- 自定义 CSS 变量体系（`--wz-*`）

### 4.2 通用交互模式

- 确认对话框（ElMessageBox.confirm）用于删除等危险操作
- 操作反馈统一使用 ElMessage.success/error
- 错误处理：API 拦截器全局处理，组件内只做兜底
- Loading 状态：`v-loading` + 数据空态 `<el-empty>`
- 表单验证：提交前 validate，错误提示

### 4.3 认证/授权模式

- 前端路由守卫：`requiresAuth` meta 标记 → 无 token 重定向到登录页
- Token 存储：localStorage + Pinia store
- Axios 拦截器自动注入 Authorization header
- 401 响应自动清除 token 并重定向

### 4.4 当前代码存在的问题（重构时需注意）

1. **风格不一致**：客户端部分页面使用了自定义 Warm·Zen 设计系统（Home/Login），部分页面使用 Element Plus 默认样式（Register/Product/Order/Cart 等），新旧风格混杂
2. **重复代码**：多个页面有相同的数据兼容逻辑（如 `productImage()`, `formatPrice()`）
3. **API 响应解析不一致**：有 `res.code === 200` 和 `res.data.records` 两种模式混用
4. **Admin 商品 CRUD Dialog 过长**：单个文件 488 行，混合大量逻辑
5. **无 TypeScript**：所有文件使用 JS，无类型定义
6. **无单元测试**：无任何测试文件
7. **无国际化**：所有文本硬编码中文
8. **文件上传**：头像上传仅做了 base64 预览，未实现真正的上传逻辑
9. **省市区选择**：使用普通 input 输入，未级联选择器
10. **购物车 badge 数量**：HeaderNav 中 cartCount 声明但未初始化（始终为 0）
