<script setup lang="ts">
const pageTitle = '我的订单'
const orderTabs = ['全部', '待付款', '待发货', '待收货', '已完成', '已取消']
</script>

<template>
  <div class="page-container">
    <div class="order-list">
      <h1 class="order-list__title">{{ pageTitle }}</h1>

      <!-- Tab Filters Placeholder -->
      <div class="order-list__tabs">
        <span
          v-for="tab in orderTabs"
          :key="tab"
          class="order-list__tab"
          :class="{ 'order-list__tab--active': tab === '全部' }"
        >
          {{ tab }}
        </span>
      </div>

      <!-- Order Cards Placeholder -->
      <div class="order-list__cards">
        <div v-for="i in 3" :key="i" class="order-list__card">
          <div class="order-list__card-header">
            <span class="order-list__order-id">订单号：WM2026051900{{ i }}</span>
            <span class="order-list__order-status">待发货</span>
          </div>
          <div class="order-list__card-body">
            <div class="order-list__item">
              <div class="order-list__item-image"></div>
              <div class="order-list__item-info">
                <p class="order-list__item-name">示例商品 {{ i }}</p>
                <p class="order-list__item-spec">规格：默认</p>
              </div>
              <div class="order-list__item-price">¥ 99.00</div>
            </div>
          </div>
          <div class="order-list__card-footer">
            <span class="order-list__total">共 1 件商品 合计：<strong>¥ 99.00</strong></span>
            <div class="order-list__actions">
              <span class="order-list__action order-list__action--secondary">查看详情</span>
              <span class="order-list__action order-list__action--primary">确认收货</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div class="order-list__empty" style="display: none;">
        <p class="order-list__empty-text">暂无订单</p>
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
.order-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
}
.order-list__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.order-list__tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  padding: 4px;
  background: var(--wz-bg-card, #fff);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-list__tab {
  flex: 1;
  text-align: center;
  padding: 10px 8px;
  font-size: 13px;
  color: var(--wz-text-soft);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.order-list__tab:hover {
  color: var(--wz-orange);
}
.order-list__tab--active {
  background: var(--wz-orange);
  color: #fff;
}
.order-list__cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.order-list__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-list__card-header {
  display: flex;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0ebe6;
}
.order-list__order-id {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.order-list__order-status {
  font-size: 13px;
  color: var(--wz-orange);
  font-weight: 500;
}
.order-list__card-body {
  padding: 16px 20px;
}
.order-list__item {
  display: flex;
  align-items: center;
  gap: 14px;
}
.order-list__item-image {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  border-radius: 8px;
  flex-shrink: 0;
}
.order-list__item-info {
  flex: 1;
}
.order-list__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 4px;
}
.order-list__item-spec {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.order-list__item-price {
  font-size: 15px;
  color: var(--wz-text);
  font-weight: 500;
}
.order-list__card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: var(--wz-bg);
}
.order-list__total {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.order-list__total strong {
  color: var(--wz-orange);
  font-size: 15px;
}
.order-list__actions {
  display: flex;
  gap: 10px;
}
.order-list__action {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.order-list__action--secondary {
  border: 1px solid var(--wz-border);
  color: var(--wz-text);
}
.order-list__action--secondary:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.order-list__action--primary {
  background: var(--wz-orange);
  color: #fff;
}
.order-list__action--primary:hover {
  background: var(--wz-orange-dark);
}
.order-list__empty {
  text-align: center;
  padding: 80px 24px;
}
.order-list__empty-text {
  font-size: 16px;
  color: var(--wz-text-soft);
}
</style>
