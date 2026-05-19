<script setup lang="ts">
const pageTitle = '购物车'
</script>

<template>
  <div class="page-container">
    <div class="cart">
      <div class="cart__header">
        <h1 class="cart__title">{{ pageTitle }}</h1>
        <span class="cart__count">共 0 件商品</span>
      </div>

      <!-- Table placeholder -->
      <div class="cart__table">
        <div class="cart__table-header">
          <span class="cart__col cart__col--info">商品信息</span>
          <span class="cart__col cart__col--price">单价</span>
          <span class="cart__col cart__col--qty">数量</span>
          <span class="cart__col cart__col--total">小计</span>
          <span class="cart__col cart__col--action">操作</span>
        </div>
        <div v-for="i in 3" :key="i" class="cart__row">
          <div class="cart__col cart__col--info">
            <div class="cart__product">
              <div class="cart__product-image"></div>
              <div class="cart__product-detail">
                <p class="cart__product-name">示例商品 {{ i }}</p>
                <p class="cart__product-spec">规格：默认</p>
              </div>
            </div>
          </div>
          <div class="cart__col cart__col--price">¥ 99.00</div>
          <div class="cart__col cart__col--qty">
            <div class="cart__qty-control">
              <span class="cart__qty-btn">-</span>
              <span class="cart__qty-value">1</span>
              <span class="cart__qty-btn">+</span>
            </div>
          </div>
          <div class="cart__col cart__col--total">¥ 99.00</div>
          <div class="cart__col cart__col--action">
            <span class="cart__delete">删除</span>
          </div>
        </div>
      </div>

      <!-- Summary Placeholder -->
      <div class="cart__footer">
        <div class="cart__summary">
          <span class="cart__summary-text">合计：</span>
          <span class="cart__summary-price">¥ 0.00</span>
        </div>
        <div class="cart__checkout-btn">去结算</div>
      </div>

      <!-- Empty state (hidden) -->
      <div class="cart__empty" style="display: none;">
        <p class="cart__empty-text">购物车是空的</p>
        <p class="cart__empty-hint">快去挑选心仪的商品吧</p>
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
}
.cart__product-image {
  width: 72px;
  height: 72px;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  border-radius: 8px;
  flex-shrink: 0;
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
}

@media (max-width: 640px) {
  .cart__table-header { display: none; }
  .cart__row { flex-wrap: wrap; gap: 12px; }
  .cart__col--info { flex: 1 1 100%; }
}
</style>
