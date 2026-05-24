# 商品购买流程改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构商品详情页购买逻辑，引入交互式规格选择、购物车勾选结算机制、修复「立即购买」断裂

**Architecture:** 前端 Vue 3 + TypeScript，后端 Spring Boot 微服务（Java 17），DB MySQL。
后端修改集中在 order-service（Cart / Order / Payment），前端修改在 frontend/src/views。
规格数据平铺存储在 `product_spec` 表，购物车和订单项通过 `spec_info` 字段传递规格选择。

**Tech Stack:** Vue 3 + TypeScript, Java 17 + Spring Boot + MyBatis-Plus, MySQL

---

### Task 1: DB + Entity — Cart 表加 spec_info 字段 + OrderItem 实体加 specInfo

**Files:**
- Modify: `deploy/mysql/init.sql`
- Modify: `backend/order-service/src/main/java/com/mall/order/entity/Cart.java`
- Modify: `backend/order-service/src/main/java/com/mall/order/entity/OrderItem.java`
- Modify: `backend/order-service/src/main/java/com/mall/order/dto/CartVO.java`

- [ ] **Step 1: Cart 表加 spec_info 列**

`deploy/mysql/init.sql` 中 `CREATE TABLE IF NOT EXISTS cart` 加一列：

```sql
spec_info VARCHAR(500) DEFAULT NULL,
```

放在 `checked INT DEFAULT 1,` 和 `created_time DATETIME NOT NULL,` 之间。

- [ ] **Step 2: Cart Java Entity 加 specInfo**

`Cart.java` 新增字段：

```java
private String specInfo;
```

- [ ] **Step 3: OrderItem Java Entity 加 specInfo**

`OrderItem.java` 新增字段：

```java
private String specInfo;
```

（数据库已有 `spec_info` 列，仅缺实体字段映射）

- [ ] **Step 4: CartVO DTO 加 specInfo**

`CartVO.java` 新增字段：

```java
private String specInfo;
```

- [ ] **Step 5: 前端 CartItem 接口加 specInfo**

`frontend/src/api/cart.ts` 的 `CartItem` 接口新增：

```typescript
specInfo: string | null
```

- [ ] **Step 6: Commit**

```bash
git add deploy/mysql/init.sql \
  backend/order-service/src/main/java/com/mall/order/entity/Cart.java \
  backend/order-service/src/main/java/com/mall/order/entity/OrderItem.java \
  backend/order-service/src/main/java/com/mall/order/dto/CartVO.java \
  frontend/src/api/cart.ts
git commit -m "feat: add specInfo field to cart and order entities"
```

---

### Task 2: 后端 — CartService 支持 specInfo + 自动调整勾选状态

**Files:**
- Modify: `backend/order-service/src/main/java/com/mall/order/service/CartService.java`
- Modify: `backend/order-service/src/main/java/com/mall/order/controller/CartController.java` (AddRequest 加 specInfo)

- [ ] **Step 1: CartController.AddRequest 加 specInfo 字段**

在 `AddRequest` 类中新增：

```java
private String specInfo;
```

- [ ] **Step 2: CartService.add() 支持 specInfo + 自动勾选逻辑**

`CartService.add()` 方法改成：

```java
@Transactional
public void add(Long userId, Long productId, Integer quantity, String specInfo) {
    // Check if product already in cart (same product + same spec)
    LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<Cart>()
            .eq(Cart::getUserId, userId)
            .eq(Cart::getProductId, productId);
    if (specInfo != null) {
        wrapper.eq(Cart::getSpecInfo, specInfo);
    } else {
        wrapper.isNull(Cart::getSpecInfo);
    }
    Cart existing = cartMapper.selectOne(wrapper);

    if (existing != null) {
        existing.setQuantity(existing.getQuantity() + quantity);
        existing.setUpdatedTime(LocalDateTime.now());
        cartMapper.updateById(existing);
        // Uncheck all, then check this one
        checkAll(userId, 0);
        existing.setChecked(1);
        cartMapper.updateById(existing);
    } else {
        // Uncheck all first
        checkAll(userId, 0);

        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setProductId(productId);
        cart.setQuantity(quantity);
        cart.setSpecInfo(specInfo);
        cart.setChecked(1);
        cart.setCreatedTime(LocalDateTime.now());
        cart.setUpdatedTime(LocalDateTime.now());
        cartMapper.insert(cart);
    }
}
```

同时更新 `CartController.add()` 调用：

```java
cartService.add(userId, request.getProductId(), request.getQuantity(), request.getSpecInfo());
```

- [ ] **Step 3: CartService.listByUserId() 返回 specInfo**

无需额外改动 — `CartVO` 已有 `specInfo` 字段，`Cart` 实体也已有，MyBatis-Plus 自动映射。

- [ ] **Step 4: Commit**

```bash
git add backend/order-service/src/main/java/com/mall/order/service/CartService.java \
  backend/order-service/src/main/java/com/mall/order/controller/CartController.java
git commit -m "feat: cart supports specInfo and auto-check on add"
```

---

### Task 3: 后端 — OrderService.create() 使用 cartItemIds + 传递 specInfo

**Files:**
- Modify: `backend/order-service/src/main/java/com/mall/order/service/OrderService.java`

- [ ] **Step 1: 修复 create() 使用 cartItemIds 参数 + 传递 specInfo**

`OrderService.create()` 方法修改：

```java
@Transactional
public Order create(Long userId, String receiverName, String receiverPhone,
                    String receiverAddress, String paymentMethod, List<Long> cartItemIds) {
    // Get cart items by IDs (instead of getCheckedItems)
    if (cartItemIds == null || cartItemIds.isEmpty()) {
        throw new RuntimeException("No items selected");
    }
    List<Cart> cartItems = cartMapper.selectList(
            new LambdaQueryWrapper<Cart>()
                    .in(Cart::getId, cartItemIds)
                    .eq(Cart::getUserId, userId));
    if (cartItems.isEmpty()) {
        throw new RuntimeException("No items selected");
    }

    // Generate order no
    String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            + String.format("%08d", new Random().nextInt(100000000));

    // Calculate total and fetch product details
    List<Long> productIds = cartItems.stream()
            .map(Cart::getProductId).collect(Collectors.toList());

    Map<Long, Map<String, Object>> productMap = fetchProductMap(productIds);

    BigDecimal total = BigDecimal.ZERO;
    for (var cartItem : cartItems) {
        Map<String, Object> p = productMap.get(cartItem.getProductId());
        BigDecimal price = p != null ? new BigDecimal(p.get("price").toString()) : BigDecimal.valueOf(100);
        total = total.add(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
    }

    // Create order
    Order order = new Order();
    order.setUserId(userId);
    order.setOrderNo(orderNo);
    order.setTotalAmount(total);
    order.setStatus(0);
    order.setPaymentMethod(paymentMethod);
    order.setReceiverName(receiverName);
    order.setReceiverPhone(receiverPhone);
    order.setReceiverAddress(receiverAddress);
    order.setCreatedTime(LocalDateTime.now());
    order.setUpdatedTime(LocalDateTime.now());
    orderMapper.insert(order);

    // Create order items
    for (var cartItem : cartItems) {
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(cartItem.getProductId());

        Map<String, Object> p = productMap.get(cartItem.getProductId());
        if (p != null) {
            item.setProductName((String) p.getOrDefault("name", "商品"));
            item.setProductImage((String) p.getOrDefault("mainImage", null));
            BigDecimal price = new BigDecimal(p.get("price").toString());
            item.setPrice(price);
            item.setSubtotal(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        } else {
            item.setProductName("Product " + cartItem.getProductId());
            item.setPrice(BigDecimal.valueOf(100));
            item.setSubtotal(BigDecimal.valueOf(100).multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        item.setQuantity(cartItem.getQuantity());
        item.setSpecInfo(cartItem.getSpecInfo());  // Pass specInfo through
        orderItemMapper.insert(item);

        // Remove from cart
        cartService.delete(cartItem.getId(), userId);
    }

    return order;
}
```

注意：需要新增 `CartMapper` 注入（和已有的 `orderMapper`, `orderItemMapper` 同级注入），或者通过 CartService 做查询。我会通过注入 CartMapper 直接查。

在 `OrderService` 中新增注入：

```java
private final CartMapper cartMapper;
```

（Lombok `@RequiredArgsConstructor` 自动注入）

- [ ] **Step 2: Commit**

```bash
git add backend/order-service/src/main/java/com/mall/order/service/OrderService.java
git commit -m "fix: OrderService.create uses cartItemIds param and passes specInfo"
```

---

### Task 4: 前端 — ProductDetailView 规格选择 + 数量选择器

**Files:**
- Modify: `frontend/src/views/ProductDetailView.vue`

这是改动最大的任务。需要把商品详情页的 `<script setup>` 和 `<template>` 重写规格部分。

- [ ] **Step 1: 改造 script setup — 规格分组、状态管理、联动计算**

在 `<script setup lang="ts">` 中（保留现有的 import 和生命周期，替换/新增以下内容）：

```typescript
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { addToCart } from '@/api/cart'
import type { Product, ProductSpec } from '@/api/product'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const product = ref<Product | null>(null)
const loading = ref(true)
const activeTab = ref('detail')

// Spec selection state
const specGroups = ref<{ name: string; values: ProductSpec[] }[]>([])
const selectedSpecs = ref<Record<string, string>>({})  // specName → specValue
const quantity = ref(1)

// Computed price: basePrice + sum of extraPrice of selected specs
const displayPrice = computed(() => {
  if (!product.value) return 0
  let extra = 0
  for (const group of specGroups.value) {
    const selectedValue = selectedSpecs.value[group.name]
    if (selectedValue) {
      const spec = group.values.find(v => v.specValue === selectedValue)
      if (spec) extra += spec.extraPrice
    }
  }
  return product.value.price + extra
})

// Computed stock: min stock of selected specs
const displayStock = computed(() => {
  if (!product.value) return 0
  if (!specGroups.value.length) return product.value.stock
  let minStock = Infinity
  for (const group of specGroups.value) {
    const selectedValue = selectedSpecs.value[group.name]
    if (!selectedValue) return 0  // not all selected
    const spec = group.values.find(v => v.specValue === selectedValue)
    if (spec && spec.stock < minStock) minStock = spec.stock
  }
  return minStock === Infinity ? 0 : minStock
})

// All specs selected?
const allSpecsSelected = computed(() => {
  return specGroups.value.every(g => selectedSpecs.value[g.name])
})

// Build spec groups from product specList
function buildSpecGroups() {
  if (!product.value?.specList) return
  const groups: Record<string, ProductSpec[]> = {}
  for (const spec of product.value.specList) {
    if (!groups[spec.specName]) groups[spec.specName] = []
    groups[spec.specName].push(spec)
  }
  specGroups.value = Object.entries(groups).map(([name, values]) => ({ name, values }))
  // Auto-select first value of each group
  for (const group of specGroups.value) {
    selectedSpecs.value[group.name] = group.values[0].specValue
  }
  quantity.value = 1
}

function selectSpec(groupName: string, specValue: string) {
  selectedSpecs.value[groupName] = specValue
  quantity.value = 1  // reset quantity when spec changes
}

// Build specInfo string for cart/order
function getSpecInfo(): string {
  return specGroups.value.map(g => `${g.name}: ${selectedSpecs.value[g.name] || ''}`).join('; ')
}

async function handleAddToCart() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await addToCart({
      productId: product.value.id,
      quantity: quantity.value,
      specInfo: getSpecInfo()
    })
    showToast('已加入购物车', 'success')
  } catch {
    // handled by interceptor
  }
}

async function handleBuyNow() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await addToCart({
      productId: product.value.id,
      quantity: quantity.value,
      specInfo: getSpecInfo()
    })
    router.push('/order/create')
  } catch {
    // handled by interceptor
  }
}
```

- [ ] **Step 2: 改造 template — 规格部分、数量选择器、按钮状态**

模板中替换原有的 spec 展示和 actions 部分：

```html
<!-- Spec Selection -->
<div v-if="specGroups.length" class="pdp__specs">
  <div v-for="group in specGroups" :key="group.name" class="pdp__spec-row">
    <span class="pdp__spec-label">{{ group.name }}</span>
    <div class="pdp__spec-options">
      <span
        v-for="spec in group.values"
        :key="spec.id"
        class="pdp__spec-option"
        :class="{ 'pdp__spec-option--active': selectedSpecs[group.name] === spec.specValue }"
        @click="selectSpec(group.name, spec.specValue)"
      >
        {{ spec.specValue }}
        <span v-if="spec.extraPrice > 0" class="pdp__spec-extra">+¥{{ spec.extraPrice }}</span>
      </span>
    </div>
  </div>
</div>

<!-- Quantity -->
<div class="pdp__qty-row">
  <span class="pdp__qty-label">数量</span>
  <div class="pdp__qty-control">
    <span class="pdp__qty-btn" :class="{ 'pdp__qty-btn--disabled': quantity <= 1 }" @click="quantity > 1 && quantity--">-</span>
    <span class="pdp__qty-value">{{ quantity }}</span>
    <span class="pdp__qty-btn" :class="{ 'pdp__qty-btn--disabled': quantity >= displayStock }" @click="quantity < displayStock && quantity++">+</span>
  </div>
  <span class="pdp__qty-hint">库存 {{ displayStock }} 件</span>
</div>

<!-- Actions -->
<div class="pdp__actions">
  <div
    class="pdp__cart-btn"
    :class="{ 'pdp__btn--disabled': !allSpecsSelected }"
    @click="handleAddToCart"
  >加入购物车</div>
  <div
    class="pdp__buy-btn"
    :class="{ 'pdp__btn--disabled': !allSpecsSelected }"
    @click="handleBuyNow"
  >立即购买</div>
</div>
```

- [ ] **Step 3: 新增样式 — 规格选中样式、数量选择器样式**

在 `<style scoped>` 末尾追加：

```css
.pdp__spec-option--active {
  border-color: var(--wz-orange);
  background: rgba(255, 107, 53, 0.06);
  color: var(--wz-orange);
}
.pdp__spec-extra {
  font-size: 11px;
  color: var(--wz-orange);
  margin-left: 2px;
}
.pdp__qty-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  padding-top: 12px;
  border-top: 1px solid var(--wz-border);
}
.pdp__qty-label {
  font-size: 13px;
  color: var(--wz-text-soft);
  min-width: 48px;
}
.pdp__qty-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--wz-border);
  border-radius: 6px;
  overflow: hidden;
}
.pdp__qty-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}
.pdp__qty-btn:hover { background: var(--wz-bg); }
.pdp__qty-btn--disabled { opacity: 0.3; cursor: not-allowed; }
.pdp__qty-value {
  width: 44px;
  text-align: center;
  font-size: 14px;
  border-left: 1px solid var(--wz-border);
  border-right: 1px solid var(--wz-border);
  line-height: 32px;
}
.pdp__qty-hint {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.pdp__btn--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/ProductDetailView.vue
git commit -m "feat: add interactive spec selection and quantity control to PDP"
```

---

### Task 5: 前端 — CartView 勾选功能

**Files:**
- Modify: `frontend/src/views/CartView.vue`

- [ ] **Step 1: 改造 script — 勾选状态管理**

在 `<script setup>` 中新增：

```typescript
import { checkCartItem, checkAllCart } from '@/api/cart'

// Computed
const checkedItems = computed(() => cartItems.value.filter(i => i.checked))
const totalPrice = computed(() =>
  checkedItems.value.reduce((sum, item) => sum + (item.productPrice || 0) * item.quantity, 0)
)
const allChecked = computed(() =>
  cartItems.value.length > 0 && cartItems.value.every(i => i.checked)
)

// Methods
async function handleCheck(item: CartItem) {
  const newChecked = item.checked ? 0 : 1
  try {
    await checkCartItem({ id: item.id, checked: newChecked })
    item.checked = newChecked
  } catch { /* handled */ }
}

async function handleCheckAll() {
  const newChecked = allChecked.value ? 0 : 1
  try {
    await checkAllCart(newChecked)
    cartItems.value.forEach(i => i.checked = newChecked)
  } catch { /* handled */ }
}

function goCheckout() {
  if (!checkedItems.value.length) return
  router.push('/order/create')
}
```

- [ ] **Step 2: 改造 template — 勾选 UI**

替换购物车表格中的 header 和 row：

```html
<!-- Table Header -->
<div class="cart__table-header">
  <span class="cart__col cart__col--check">
    <span class="cart__checkbox" :class="{ 'cart__checkbox--checked': allChecked }" @click="handleCheckAll">
      <span v-if="allChecked">✓</span>
    </span>
  </span>
  <span class="cart__col cart__col--info">商品信息</span>
  <span class="cart__col cart__col--price">单价</span>
  <span class="cart__col cart__col--qty">数量</span>
  <span class="cart__col cart__col--total">小计</span>
  <span class="cart__col cart__col--action">操作</span>
</div>

<!-- Row -->
<div v-for="item in cartItems" :key="item.id" class="cart__row">
  <div class="cart__col cart__col--check">
    <span class="cart__checkbox" :class="{ 'cart__checkbox--checked': item.checked }" @click="handleCheck(item)">
      <span v-if="item.checked">✓</span>
    </span>
  </div>
  <!-- ... rest of columns unchanged ... -->
</div>
```

- [ ] **Step 3: 改造结算栏 — 显示已选件数**

```html
<!-- Summary -->
<div v-if="cartItems.length" class="cart__footer">
  <div class="cart__summary">
    <span class="cart__summary-text">已选 {{ checkedItems.length }} 件，合计：</span>
    <span class="cart__summary-price">¥{{ totalPrice.toFixed(2) }}</span>
  </div>
  <div class="cart__checkout-btn" :class="{ 'cart__checkout-btn--disabled': !checkedItems.length }" @click="goCheckout">
    去结算
  </div>
</div>
```

- [ ] **Step 4: 新增样式**

```css
.cart__col--check { flex: 0 0 40px; text-align: center; }
.cart__checkbox {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: 2px solid var(--wz-border);
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  color: #fff;
  transition: all 0.2s;
}
.cart__checkbox--checked {
  background: var(--wz-orange);
  border-color: var(--wz-orange);
}
.cart__checkout-btn--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/CartView.vue
git commit -m "feat: add cart item checkbox and select-all functionality"
```

---

### Task 6: 前端 — OrderCreateView 过滤已勾选商品

**Files:**
- Modify: `frontend/src/views/OrderCreateView.vue`

- [ ] **Step 1: 改造 script — onMounted 过滤 checked 商品**

保留大部分现有代码，只修改 `onMounted` 中的加载逻辑：

```typescript
onMounted(async () => {
  try {
    const [cartData, addrRes] = await Promise.all([
      getCartList(),
      getAddressList()
    ])
    // Only keep checked items
    const allItems = cartData as CartItem[]
    cartItems.value = allItems.filter(i => i.checked === 1)
    addressList.value = addrRes as Address[]
  } catch {
    // handled
  }
})
```

同时删除 `totalAmount` computed 中对 `productPrice` 的可空处理（因为已过滤，全是 checked 的）：

```typescript
const totalAmount = computed(() =>
  cartItems.value.reduce((s, item) => s + (item.productPrice || 0) * item.quantity, 0)
)
```

（实际不需要改，保持兼容即可）

以及 `handleSubmit` 的 `cartItemIds` 只传勾选的：

```typescript
cartItemIds: cartItems.value.map(i => i.id)
```

（这已经是当前代码的行为，无需修改）

- [ ] **Step 2: 在 template 中展示 specInfo**

在商品信息区域，每个商品行增加规格展示（如果 `item.specInfo` 存在）：

```html
<div class="order-create__item-info">
  <p class="order-create__item-name">{{ item.productName }}</p>
  <span v-if="item.productTag" class="order-create__item-tag">{{ item.productTag }}</span>
  <p v-if="item.specInfo" class="order-create__item-spec">{{ item.specInfo }}</p>
</div>
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/OrderCreateView.vue
git commit -m "fix: OrderCreateView filters checked cart items and shows specInfo"
```

---

### Task 7: 前端 — OrderDetailView 支付按钮

**Files:**
- Modify: `frontend/src/views/OrderDetailView.vue`
- Modify: `frontend/src/api/order.ts`

- [ ] **Step 1: Order API 新增 payOrder 方法**

`frontend/src/api/order.ts` 新增：

```typescript
export function payOrder(data: { orderId: number; method: string }) {
  return request.post<void>('/order/payment/pay', data)
}
```

- [ ] **Step 2: 改造 OrderDetailView — 支付按钮**

在 `<script setup>` 中新增：

```typescript
import { payOrder } from '@/api/order'

async function handlePay() {
  if (!order.value) return
  try {
    await payOrder({ orderId: order.value.id, method: order.value.paymentMethod || 'wechat' })
    order.value.status = 1
    showToast('支付成功', 'success')
  } catch { /* handled */ }
}
```

注意还需要在现有 import 中加 `showToast`：

```typescript
import { showToast } from '@/utils/toast'
```

- [ ] **Step 3: 改造 template — 在状态 0 时显示支付按钮**

现在 `order-detail__actions` 中只有 status=0 的取消按钮和 status=2 的确认收货按钮。在取消按钮旁加支付按钮：

```html
<div v-if="order.status === 0 || order.status === 2" class="order-detail__actions">
  <span
    v-if="order.status === 0"
    class="order-detail__action order-detail__action--primary"
    @click="handlePay"
  >去支付</span>
  <span
    v-if="order.status === 0"
    class="order-detail__action order-detail__action--danger"
    @click="handleCancel"
  >取消订单</span>
  <span
    v-if="order.status === 2"
    class="order-detail__action order-detail__action--primary"
    @click="handleConfirm"
  >确认收货</span>
</div>
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/views/OrderDetailView.vue frontend/src/api/order.ts
git commit -m "feat: add pay button on order detail page"
```
