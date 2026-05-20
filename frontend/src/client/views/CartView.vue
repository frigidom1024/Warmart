<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCartList, updateCartQuantity, deleteCartItem } from '@/api/cart'
import type { CartItem } from '@/api/cart'

const router = useRouter()
const cartItems = ref<CartItem[]>([])
const loading = ref(true)

const totalCount = computed(() => cartItems.value.length)
const totalPrice = computed(() =>
  cartItems.value.reduce((sum, item) => sum + (item.productPrice || 0) * item.quantity, 0)
)

onMounted(async () => {
  try {
    cartItems.value = await getCartList()
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
})

async function handleQuantity(item: CartItem, delta: number) {
  const newQty = item.quantity + delta
  if (newQty < 1) return
  try {
    await updateCartQuantity({ id: item.id, quantity: newQty })
    item.quantity = newQty
  } catch {
    // handled by interceptor
  }
}

async function handleDelete(id: number) {
  try {
    await deleteCartItem(id)
    cartItems.value = cartItems.value.filter(i => i.id !== id)
  } catch {
    // handled by interceptor
  }
}

function goCheckout() {
  if (!cartItems.value.length) return
  router.push('/order/create')
}
</script>

<template>
  <div class="page-container">
    <div class="cart">
      <div class="cart__header">
        <h1 class="cart__title">购物车</h1>
        <span class="cart__count">共 {{ totalCount }} 件商品</span>
      </div>

      <!-- Cart Table -->
      <div v-if="cartItems.length" class="cart__table">
        <div class="cart__table-header">
          <span class="cart__col cart__col--info">商品信息</span>
          <span class="cart__col cart__col--price">单价</span>
          <span class="cart__col cart__col--qty">数量</span>
          <span class="cart__col cart__col--total">小计</span>
          <span class="cart__col cart__col--action">操作</span>
        </div>
        <div v-for="item in cartItems" :key="item.id" class="cart__row">
          <div class="cart__col cart__col--info">
            <div class="cart__product" @click="router.push('/product/detail/' + item.productId)">
              <img v-if="item.productImage" :src="item.productImage" class="cart__product-image">
              <div v-else class="cart__product-image"></div>
              <div class="cart__product-detail">
                <p class="cart__product-name">{{ item.productName }}</p>
                <span v-if="item.productTag" class="cart__product-tag">{{ item.productTag }}</span>
              </div>
            </div>
          </div>
          <div class="cart__col cart__col--price">
            <span class="cart__price">¥{{ item.productPrice }}</span>
            <span v-if="item.productOldPrice" class="cart__old-price">¥{{ item.productOldPrice }}</span>
          </div>
          <div class="cart__col cart__col--qty">
            <div class="cart__qty-control">
              <span class="cart__qty-btn" @click="handleQuantity(item, -1)">-</span>
              <span class="cart__qty-value">{{ item.quantity }}</span>
              <span class="cart__qty-btn" @click="handleQuantity(item, 1)">+</span>
            </div>
          </div>
          <div class="cart__col cart__col--total">¥{{ ((item.productPrice || 0) * item.quantity).toFixed(2) }}</div>
          <div class="cart__col cart__col--action">
            <span class="cart__delete" @click="handleDelete(item.id)">删除</span>
          </div>
        </div>
      </div>

      <!-- Summary -->
      <div v-if="cartItems.length" class="cart__footer">
        <div class="cart__summary">
          <span class="cart__summary-text">合计：</span>
          <span class="cart__summary-price">¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        <div class="cart__checkout-btn" @click="goCheckout">去结算</div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!loading" class="cart__empty">
        <p class="cart__empty-text">购物车是空的</p>
        <p class="cart__empty-hint">快去挑选心仪的商品吧</p>
        <div class="cart__empty-btn" @click="router.push('/product/list')">去逛逛</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.cart {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px;
}
.cart__header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
}
.cart__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
}
.cart__count {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.cart__table {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}
.cart__table-header {
  display: flex;
  padding: 14px 20px;
  background: var(--wz-bg);
  font-size: 13px;
  color: var(--wz-text-soft);
  font-weight: 500;
}
.cart__row {
  display: flex;
  padding: 20px;
  border-bottom: 1px solid #f0ebe6;
  align-items: center;
}
.cart__row:last-child {
  border-bottom: none;
}
.cart__col--info { flex: 3; }
.cart__col--price { flex: 1; text-align: center; }
.cart__col--qty { flex: 1; text-align: center; }
.cart__col--total { flex: 1; text-align: center; }
.cart__col--action { flex: 0.8; text-align: center; }
.cart__col {
  font-size: 14px;
  color: var(--wz-text);
}
.cart__product {
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
}
.cart__price {
  font-weight: 600;
}
.cart__old-price {
  font-size: 12px;
  color: var(--wz-text-muted);
  text-decoration: line-through;
  margin-left: 6px;
}
.cart__product-image {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  flex-shrink: 0;
  object-fit: cover;
  background: var(--wz-bg);
}
.cart__product-tag {
  display: inline-block;
  font-size: 11px;
  font-weight: 600;
  color: var(--wz-orange);
  background: var(--wz-orange-muted);
  padding: 1px 8px;
  border-radius: 4px;
}
.cart__product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 4px;
}
.cart__product-spec {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.cart__qty-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--wz-border);
  border-radius: 6px;
  overflow: hidden;
}
.cart__qty-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  transition: background 0.2s;
}
.cart__qty-btn:hover { background: var(--wz-bg); }
.cart__qty-value {
  width: 40px;
  text-align: center;
  font-size: 14px;
  border-left: 1px solid var(--wz-border);
  border-right: 1px solid var(--wz-border);
  line-height: 30px;
}
.cart__delete {
  font-size: 13px;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: color 0.2s;
}
.cart__delete:hover {
  color: var(--wz-orange);
}
.cart__footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 24px;
  padding: 20px 24px;
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.cart__summary-text {
  font-size: 15px;
  color: var(--wz-text);
}
.cart__summary-price {
  font-size: 24px;
  font-weight: 700;
  color: var(--wz-orange);
}
.cart__checkout-btn {
  height: 44px;
  padding: 0 36px;
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
  transition: background 0.2s;
}
.cart__checkout-btn:hover {
  background: var(--wz-orange-dark);
}
.cart__empty {
  text-align: center;
  padding: 80px 24px;
}
.cart__empty-text {
  font-size: 18px;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.cart__empty-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
  margin-bottom: 20px;
}
.cart__empty-btn {
  display: inline-flex;
  padding: 10px 32px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 999px;
  font-size: 14px;
  cursor: pointer;
  transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out);
}
.cart__empty-btn:hover {
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.35);
}

@media (max-width: 640px) {
  .cart__table-header { display: none; }
  .cart__row { flex-wrap: wrap; gap: 12px; }
  .cart__col--info { flex: 1 1 100%; }
}
</style>
