<script setup lang="ts">
const pageTitle = '商品列表'
</script>

<template>
  <div class="page-container">
    <div class="plp">
      <!-- Page Header -->
      <div class="plp__header">
        <h1 class="plp__title">{{ pageTitle }}</h1>
        <p class="plp__count">共 0 件商品</p>
      </div>

      <!-- Filter / Sort Bar Placeholder -->
      <div class="plp__toolbar">
        <div class="plp__filter-group">
          <span class="plp__filter-chip">全部</span>
          <span class="plp__filter-chip">新品</span>
          <span class="plp__filter-chip">热销</span>
          <span class="plp__filter-chip">促销</span>
        </div>
        <div class="plp__sort-group">
          <span class="plp__sort-option">综合</span>
          <span class="plp__sort-option">销量</span>
          <span class="plp__sort-option plp__sort-option--active">价格</span>
          <span class="plp__sort-option">最新</span>
        </div>
      </div>

      <!-- Product Grid Placeholder -->
      <div class="plp__grid">
        <div v-for="i in 8" :key="i" class="plp__product-card">
          <div class="plp__product-image">商品图片</div>
          <div class="plp__product-info">
            <h3 class="plp__product-name">示例商品 {{ i }}</h3>
            <p class="plp__product-price">¥ {{ (Math.random() * 500 + 10).toFixed(2) }}</p>
          </div>
        </div>
      </div>

      <!-- Pagination Placeholder -->
      <div class="plp__pagination">
        <span class="plp__page-item plp__page-item--active">1</span>
        <span class="plp__page-item">2</span>
        <span class="plp__page-item">3</span>
        <span class="plp__page-item">...</span>
        <span class="plp__page-item">10</span>
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
.plp {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
}
.plp__header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
}
.plp__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
}
.plp__count {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.plp__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 28px;
  padding: 16px 20px;
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.plp__filter-group,
.plp__sort-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.plp__filter-chip {
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 20px;
  background: var(--wz-bg);
  color: var(--wz-text);
  cursor: pointer;
  transition: all 0.2s;
}
.plp__filter-chip:hover {
  background: rgba(255, 107, 53, 0.1);
  color: var(--wz-orange);
}
.plp__sort-option {
  font-size: 13px;
  padding: 6px 12px;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: color 0.2s;
}
.plp__sort-option:hover,
.plp__sort-option--active {
  color: var(--wz-orange);
  font-weight: 500;
}
.plp__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}
.plp__product-card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.plp__product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.plp__product-image {
  height: 180px;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--wz-text-soft);
}
.plp__product-info {
  padding: 14px;
}
.plp__product-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.plp__product-price {
  font-size: 17px;
  font-weight: 600;
  color: var(--wz-orange);
}
.plp__pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
}
.plp__page-item {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 14px;
  color: var(--wz-text-soft);
  background: var(--wz-bg-card, #fff);
  cursor: pointer;
  transition: all 0.2s;
}
.plp__page-item:hover {
  color: var(--wz-orange);
}
.plp__page-item--active {
  background: var(--wz-orange);
  color: #fff;
}
</style>
