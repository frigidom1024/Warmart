<script setup lang="ts">
const pageTitle = '订单详情'
</script>

<template>
  <div class="page-container">
    <div class="order-detail">
      <!-- Breadcrumb -->
      <div class="order-detail__breadcrumb">我的订单 / 订单详情</div>

      <h1 class="order-detail__title">{{ pageTitle }}</h1>

      <!-- Order Steps Placeholder -->
      <section class="order-detail__section">
        <div class="order-detail__steps">
          <div class="order-detail__step order-detail__step--completed">
            <div class="order-detail__step-dot"></div>
            <div class="order-detail__step-info">
              <p class="order-detail__step-label">下单成功</p>
              <p class="order-detail__step-time">2026-05-19 10:00</p>
            </div>
          </div>
          <div class="order-detail__step-connector"></div>
          <div class="order-detail__step order-detail__step--completed">
            <div class="order-detail__step-dot"></div>
            <div class="order-detail__step-info">
              <p class="order-detail__step-label">已付款</p>
              <p class="order-detail__step-time">2026-05-19 10:02</p>
            </div>
          </div>
          <div class="order-detail__step-connector"></div>
          <div class="order-detail__step order-detail__step--active">
            <div class="order-detail__step-dot"></div>
            <div class="order-detail__step-info">
              <p class="order-detail__step-label">已发货</p>
              <p class="order-detail__step-time">2026-05-19 14:00</p>
            </div>
          </div>
          <div class="order-detail__step-connector"></div>
          <div class="order-detail__step">
            <div class="order-detail__step-dot"></div>
            <div class="order-detail__step-info">
              <p class="order-detail__step-label">待收货</p>
              <p class="order-detail__step-time">——</p>
            </div>
          </div>
          <div class="order-detail__step-connector"></div>
          <div class="order-detail__step">
            <div class="order-detail__step-dot"></div>
            <div class="order-detail__step-info">
              <p class="order-detail__step-label">已完成</p>
              <p class="order-detail__step-time">——</p>
            </div>
          </div>
        </div>
      </section>

      <!-- Order Info Cards -->
      <section class="order-detail__section">
        <h2 class="order-detail__section-title">收货信息</h2>
        <div class="order-detail__card">
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">收货人</span>
            <span class="order-detail__card-value">张三</span>
          </div>
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">联系电话</span>
            <span class="order-detail__card-value">138****0000</span>
          </div>
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">收货地址</span>
            <span class="order-detail__card-value">浙江省杭州市西湖区示例路 1 号</span>
          </div>
        </div>
      </section>

      <section class="order-detail__section">
        <h2 class="order-detail__section-title">商品信息</h2>
        <div class="order-detail__card">
          <div v-for="i in 2" :key="i" class="order-detail__item">
            <div class="order-detail__item-image"></div>
            <div class="order-detail__item-info">
              <p class="order-detail__item-name">示例商品 {{ i }}</p>
              <p class="order-detail__item-spec">规格：默认</p>
            </div>
            <div class="order-detail__item-price">¥ 99.00</div>
            <div class="order-detail__item-qty">x 1</div>
          </div>
        </div>
      </section>

      <!-- Summary -->
      <div class="order-detail__summary">
        <div class="order-detail__summary-row">
          <span>订单编号：</span>
          <span>WM2026051900001</span>
        </div>
        <div class="order-detail__summary-row">
          <span>下单时间：</span>
          <span>2026-05-19 10:00:00</span>
        </div>
        <div class="order-detail__summary-row">
          <span>支付方式：</span>
          <span>微信支付</span>
        </div>
        <div class="order-detail__summary-row order-detail__summary-row--total">
          <span>实付金额：</span>
          <span>¥ 198.00</span>
        </div>
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
.order-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.order-detail__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 12px;
}
.order-detail__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 28px;
}
.order-detail__section {
  margin-bottom: 24px;
}
.order-detail__section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 12px;
}
.order-detail__steps {
  display: flex;
  align-items: flex-start;
  padding: 24px;
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow-x: auto;
}
.order-detail__step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-shrink: 0;
}
.order-detail__step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e0d8d0;
  margin-top: 4px;
  flex-shrink: 0;
}
.order-detail__step--completed .order-detail__step-dot {
  background: var(--wz-orange);
}
.order-detail__step--active .order-detail__step-dot {
  background: var(--wz-orange);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.2);
}
.order-detail__step-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text);
  white-space: nowrap;
}
.order-detail__step-time {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.order-detail__step-connector {
  width: 40px;
  height: 2px;
  background: #e0d8d0;
  margin: 10px 12px 0;
  flex-shrink: 0;
}
.order-detail__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-detail__card-row {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 14px;
}
.order-detail__card-row:last-child {
  margin-bottom: 0;
}
.order-detail__card-label {
  color: var(--wz-text-soft);
  min-width: 72px;
  flex-shrink: 0;
}
.order-detail__card-value {
  color: var(--wz-text);
}
.order-detail__item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f0ebe6;
}
.order-detail__item:last-child {
  border-bottom: none;
}
.order-detail__item-image {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  border-radius: 8px;
  flex-shrink: 0;
}
.order-detail__item-info {
  flex: 1;
}
.order-detail__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 4px;
}
.order-detail__item-spec {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.order-detail__item-price {
  font-size: 15px;
  font-weight: 500;
  color: var(--wz-text);
}
.order-detail__item-qty {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.order-detail__summary {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-detail__summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 10px;
}
.order-detail__summary-row--total {
  font-size: 17px;
  font-weight: 600;
  color: var(--wz-orange);
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0ebe6;
}
</style>
