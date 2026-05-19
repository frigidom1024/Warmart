<script setup lang="ts">
const pageTitle = '商品详情'
</script>

<template>
  <div class="page-container">
    <div class="pdp">
      <!-- Breadcrumb -->
      <div class="pdp__breadcrumb">首页 / 商品列表 / 商品详情</div>

      <!-- Gallery + Info Row -->
      <div class="pdp__main">
        <!-- Gallery Placeholder -->
        <div class="pdp__gallery">
          <div class="pdp__gallery-main">商品主图</div>
          <div class="pdp__gallery-thumbs">
            <div v-for="i in 4" :key="i" class="pdp__thumb"></div>
          </div>
        </div>

        <!-- Info Placeholder -->
        <div class="pdp__info">
          <h1 class="pdp__name">{{ pageTitle }} — 示例商品名称</h1>
          <div class="pdp__price">
            <span class="pdp__price-current">¥ 128.00</span>
            <span class="pdp__price-original">¥ 199.00</span>
          </div>
          <div class="pdp__meta">
            <span class="pdp__meta-item">月销 1000+</span>
            <span class="pdp__meta-item">库存 999</span>
          </div>
          <div class="pdp__specs">
            <div v-for="i in 3" :key="i" class="pdp__spec-row">
              <span class="pdp__spec-label">规格 {{ i }}</span>
              <div class="pdp__spec-options">
                <span class="pdp__spec-option">选项 A</span>
                <span class="pdp__spec-option">选项 B</span>
                <span class="pdp__spec-option">选项 C</span>
              </div>
            </div>
          </div>
          <div class="pdp__actions">
            <div class="pdp__cart-btn">加入购物车</div>
            <div class="pdp__buy-btn">立即购买</div>
          </div>
        </div>
      </div>

      <!-- Tabs Placeholder -->
      <div class="pdp__tabs">
        <div class="pdp__tabs-header">
          <span class="pdp__tab pdp__tab--active">商品详情</span>
          <span class="pdp__tab">规格参数</span>
          <span class="pdp__tab">用户评价</span>
        </div>
        <div class="pdp__tabs-content">
          <div class="pdp__tabs-placeholder">商品详情内容区域</div>
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
.pdp {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}
.pdp__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 24px;
}
.pdp__main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
}
.pdp__gallery-main {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  color: var(--wz-text-soft);
  margin-bottom: 12px;
}
.pdp__gallery-thumbs {
  display: flex;
  gap: 10px;
}
.pdp__thumb {
  width: 72px;
  height: 72px;
  background: var(--wz-bg);
  border-radius: 8px;
  border: 2px solid var(--wz-border);
  cursor: pointer;
}
.pdp__info {
  padding-top: 8px;
}
.pdp__name {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 16px;
  line-height: 1.4;
}
.pdp__price {
  margin-bottom: 20px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.pdp__price-current {
  font-size: 32px;
  font-weight: 700;
  color: var(--wz-orange);
}
.pdp__price-original {
  font-size: 15px;
  color: var(--wz-text-soft);
  text-decoration: line-through;
}
.pdp__meta {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  padding: 12px 0;
  border-top: 1px solid var(--wz-border);
  border-bottom: 1px solid var(--wz-border);
}
.pdp__meta-item {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.pdp__specs {
  margin-bottom: 28px;
}
.pdp__spec-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}
.pdp__spec-label {
  font-size: 13px;
  color: var(--wz-text-soft);
  min-width: 48px;
  line-height: 32px;
}
.pdp__spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pdp__spec-option {
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid var(--wz-border);
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__spec-option:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.pdp__actions {
  display: flex;
  gap: 12px;
}
.pdp__cart-btn {
  flex: 1;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  border: 1px solid var(--wz-orange);
  color: var(--wz-orange);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__cart-btn:hover {
  background: rgba(255, 107, 53, 0.06);
}
.pdp__buy-btn {
  flex: 1;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  background: var(--wz-orange);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__buy-btn:hover {
  background: var(--wz-orange-dark);
}
.pdp__tabs-header {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--wz-border);
  margin-bottom: 24px;
}
.pdp__tab {
  padding: 12px 24px;
  font-size: 15px;
  color: var(--wz-text-soft);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}
.pdp__tab--active {
  color: var(--wz-orange);
  font-weight: 500;
}
.pdp__tab--active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--wz-orange);
}
.pdp__tabs-content {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 48px;
}
.pdp__tabs-placeholder {
  font-size: 14px;
  color: var(--wz-text-soft);
}

@media (max-width: 768px) {
  .pdp__main {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>
