# 商品购买流程改造设计

## 概述

重构商品详情页购买逻辑，引入交互式规格选择、购物车勾选结算机制，使购买流程符合现代电商平台模式。

## 现状问题

1. **「立即购买」断裂** — `OrderCreateView` 只从购物车加载商品，忽略了详情页传入的 `productId/quantity` 参数
2. **无规格选择** — `specList` 仅做静态展示，用户无法交互式选择规格
3. **无数量选择** — 加入购物车和立即购买都硬编码数量为 1
4. **购物车无勾选机制** — `checked` 字段后端已支持，前端未使用，结算时提交全部商品
5. **后端 `create()` 未使用 `cartItemIds` 参数** — 方法签名接收了参数但内部直接查全部已勾选商品

## 数据模型

`product_spec` 表采用平铺规格模式：

| 字段 | 说明 |
|------|------|
| `spec_name` | 规格组名（颜色、尺码、存储等） |
| `spec_value` | 规格值（白色、XL、8+256G 等） |
| `extra_price` | 该规格值的加价金额 |
| `stock` | 该规格值的独立库存 |
| `image` | 选中该规格值时的商品图片（可选） |

价格计算：`basePrice + Σ(选中规格的 extraPrice)`
库存计算：`min(选中规格的 stock)`

## 完整流程

### 商品详情页

```
规格展示：将 specList 按 specName 分组，每组为一行选项
- 规格组行：颜色 → [白色] [黑色] [灰色]
- 规格组行：尺码 → [M] [L] [XL]

选择交互：
- 点击规格选项 → 高亮选中样式
- 价格实时更新：basePrice + Σ(extraPrice)
- 库存实时更新：min(选中规格的 stock)
- 主图切换：若选中规格有 image 字段则切换
- 所有规格组必须选完 → 按钮才可点击

数量选择器：
- 步进器 [- 1 +]
- 最小值 1，最大值 = 当前规格组合的库存

[加入购物车] → 调用 POST /order/cart/add，携带 productId + specInfo + quantity
                 Toast 提示成功 → 停留在详情页

[立即购买]   → 调用 POST /order/cart/add
                 后端将该用户购物车全部 uncheck (checked=0)
                 将当前商品 check=1
                 若商品已在购物车中，更新其数量为当前选择
                 跳转 /order/create
```

### 购物车页

```
新增功能：
- 每行商品增加复选框（复用后端 checked 字段）
- 顶部「全选/取消全选」按钮
- 底部结算栏展示「已选 X 件，合计 ¥XXX」
- 仅勾选的商品参与结算

现有功能保持不变：数量调整、删除、跳转商品详情
```

### 订单创建页

```
页面逻辑调整：
- onMounted 加载购物车列表，过滤 checked === 1 的商品
- 商品列表只展示勾选的商品
- 合计金额基于实际展示的商品计算
- 提交时 cartItemIds 只传勾选商品 ID

现有功能保持不变：
- 收货地址展示（默认地址）
- 支付方式选择（微信/支付宝）
- 运费计算（当前免运费）
```

### 订单详情页

```
订单创建成功后跳转订单详情页：
- 展示订单状态、商品列表、收货信息
- 待支付订单显示「去支付」按钮
- 模拟支付：调用 POST /order/pay/{id}，将状态从 0→1
```

## 后端改动

### 必要修复

1. **`OrderService.create()` 使用传入的 `cartItemIds`**
   - 当前：方法内部调 `getCheckedItems(userId)` 忽略参数
   - 修复：用传入的 `cartItemIds` 查询并过滤购物车商品

2. **加购时调整勾选状态**（方案A：后端自动处理）
   - 修改 `CartService.add()`：加购成功后，将该用户购物车全部 uncheck，仅当前商品 check=1
   - 若商品已在购物车中，更新其数量为当前选择
   - 前端无需额外请求

3. **购物车全选/勾选 API 接入**
   - 前端接入已有的 `checkCartItem` 和 `checkAllCart` API

4. **模拟支付接口**
   - 在 `PaymentController` 中新增 `POST /order/pay/{id}` 接口
   - 逻辑：将订单状态从 0 更新为 1，记录支付时间
   - 前端订单详情页展示「模拟支付」按钮

1. **`OrderService.create()` 使用传入的 `cartItemIds`**
   - 当前：方法内部调 `getCheckedItems(userId)` 忽略参数
   - 修复：用传入的 `cartItemIds` 查询并过滤购物车商品

2. **加购时调整勾选状态**
   - 新增 `POST /order/cart/selectForPurchase` 接口
   - 逻辑：用户购物车全部 uncheck → 指定商品 check=1 → 若商品已存在则更新数量
   - 或者复用 `checkAll` + `check` 接口组合实现

3. **购物车全选/勾选 API 接入**
   - 前端接入已有的 `checkCartItem` 和 `checkAllCart` API

### 库存校验（可选增强）

- 下单时校验 product.stock 或 spec.stock 是否充足
- 库存不足返回明确错误码和提示

## 订单状态

| 状态码 | 含义 | 说明 |
|--------|------|------|
| 0 | 待支付 | 订单创建后默认状态，展示「去支付」按钮 |
| 1 | 待发货 | 模拟支付后进入，用户等待商家发货 |
| 2 | 待收货 | 商家发货后，用户可确认收货 |
| 3 | 已完成 | 用户确认收货 |
| 4 | 已取消 | 用户取消订单（仅待支付状态可取消） |
| 5 | 退款中 | 用户申请退款 |

## 涉及文件

### 前端

| 文件 | 改动 |
|------|------|
| `frontend/src/views/ProductDetailView.vue` | 交互式规格选择、数量选择器、价格联动 |
| `frontend/src/views/CartView.vue` | 勾选/全选 UI、结算栏联动 |
| `frontend/src/views/OrderCreateView.vue` | 过滤勾选商品、修复立即购买流程 |
| `frontend/src/api/cart.ts` | 接入 check/checkAll API |
| `frontend/src/api/order.ts` | 新增模拟支付接口 |

### 后端

| 文件 | 改动 |
|------|------|
| `order-service/.../service/OrderService.java` | 修复 create() 使用 cartItemIds 参数 |
| `order-service/.../controller/CartController.java` | 调整现有逻辑（无需新接口） |
| `order-service/.../service/CartService.java` | 加购自动调整勾选状态 |
| `order-service/.../controller/PaymentController.java` | 新增模拟支付接口 |
| `order-service/.../service/PaymentService.java` | 实现模拟支付逻辑 |
