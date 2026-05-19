<script setup lang="ts">
const pageTitle = '创建订单'
</script>

<template>
  <div class="page-container">
    <div class="order-create">
      <h1 class="order-create__title">{{ pageTitle }}</h1>

      <!-- Address Section Placeholder -->
      <section class="order-create__section">
        <h2 class="order-create__section-title">收货地址</h2>
        <div class="order-create__address-card">
          <div class="order-create__address-placeholder">
            <span class="order-create__add-icon">+</span>
            <span>添加收货地址</span>
          </div>
        </div>
      </section>

      <!-- Order Items Placeholder -->
      <section class="order-create__section">
        <h2 class="order-create__section-title">商品信息</h2>
        <div class="order-create__items">
          <div v-for="i in 2" :key="i" class="order-create__item">
            <div class="order-create__item-image"></div>
            <div class="order-create__item-info">
              <p class="order-create__item-name">示例商品 {{ i }}</p>
              <p class="order-create__item-spec">规格：默认</p>
            </div>
            <div class="order-create__item-price">¥ 99.00</div>
            <div class="order-create__item-qty">x 1</div>
          </div>
        </div>
      </section>

      <!-- Payment Placeholder -->
      <section class="order-create__section">
        <h2 class="order-create__section-title">支付方式</h2>
        <div class="order-create__payment-options">
          <div class="order-create__payment-option order-create__payment-option--active">
            <span class="order-create__payment-icon">💳</span>
            <span>微信支付</span>
          </div>
          <div class="order-create__payment-option">
            <span class="order-create__payment-icon">💳</span>
            <span>支付宝</span>
          </div>
        </div>
      </section>

      <!-- Summary & Submit -->
      <div class="order-create__summary">
        <div class="order-create__summary-row">
          <span>商品小计</span>
          <span>¥ 198.00</span>
        </div>
        <div class="order-create__summary-row">
          <span>运费</span>
          <span>¥ 0.00</span>
        </div>
        <div class="order-create__summary-row order-create__summary-row--total">
          <span>合计</span>
          <span>¥ 198.00</span>
        </div>
        <div class="order-create__submit-btn">提交订单</div>
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
.order-create {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.order-create__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 28px;
}
.order-create__section {
  margin-bottom: 24px;
}
.order-create__section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 12px;
}
.order-create__address-card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-create__address-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px;
  border: 2px dashed var(--wz-border);
  border-radius: 8px;
  font-size: 14px;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: border-color 0.2s;
}
.order-create__address-placeholder:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.order-create__add-icon {
  font-size: 20px;
  font-weight: 300;
}
.order-create__items {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-create__item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0ebe6;
}
.order-create__item:last-child {
  border-bottom: none;
}
.order-create__item-image {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  border-radius: 8px;
  flex-shrink: 0;
}
.order-create__item-info {
  flex: 1;
}
.order-create__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 4px;
}
.order-create__item-spec {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.order-create__item-price {
  font-size: 15px;
  color: var(--wz-text);
  font-weight: 500;
}
.order-create__item-qty {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.order-create__payment-options {
  display: flex;
  gap: 12px;
}
.order-create__payment-option {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: var(--wz-bg-card, #fff);
  border: 1px solid var(--wz-border);
  border-radius: 12px;
  font-size: 14px;
  color: var(--wz-text);
  cursor: pointer;
  transition: all 0.2s;
}
.order-create__payment-option:hover {
  border-color: var(--wz-orange);
}
.order-create__payment-option--active {
  border-color: var(--wz-orange);
  background: rgba(255, 107, 53, 0.04);
}
.order-create__payment-icon {
  font-size: 20px;
}
.order-create__summary {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-create__summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 10px;
}
.order-create__summary-row--total {
  font-size: 18px;
  font-weight: 600;
  color: var(--wz-orange);
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0ebe6;
  margin-bottom: 20px;
}
.order-create__submit-btn {
  width: 100%;
  height: 48px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  letter-spacing: 2px;
  transition: background 0.2s;
}
.order-create__submit-btn:hover {
  background: var(--wz-orange-dark);
}
</style>
