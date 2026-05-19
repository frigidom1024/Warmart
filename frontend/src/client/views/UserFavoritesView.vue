<script setup lang="ts">
const pageTitle = '我的收藏'
</script>

<template>
  <div class="page-container">
    <div class="user-favorites">
      <div class="user-favorites__header">
        <h1 class="user-favorites__title">{{ pageTitle }}</h1>
        <span class="user-favorites__count">共 0 件</span>
      </div>

      <!-- Grid Placeholder -->
      <div class="user-favorites__grid">
        <div v-for="i in 6" :key="i" class="user-favorites__card">
          <div class="user-favorites__card-image">
            <span class="user-favorites__heart">♥</span>
          </div>
          <div class="user-favorites__card-info">
            <p class="user-favorites__card-name">收藏商品 {{ i }}</p>
            <p class="user-favorites__card-price">¥ {{ (Math.random() * 300 + 30).toFixed(2) }}</p>
          </div>
          <div class="user-favorites__card-action">
            <span class="user-favorites__cart-add">加入购物车</span>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div class="user-favorites__empty" style="display: none;">
        <p class="user-favorites__empty-text">收藏夹是空的</p>
        <p class="user-favorites__empty-hint">去逛逛，发现心仪好物</p>
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
.user-favorites {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.user-favorites__header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
}
.user-favorites__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
}
.user-favorites__count {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.user-favorites__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}
.user-favorites__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.user-favorites__card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.user-favorites__card-image {
  height: 160px;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 12px;
  position: relative;
}
.user-favorites__heart {
  font-size: 20px;
  color: var(--wz-orange);
  cursor: pointer;
  transition: transform 0.2s;
}
.user-favorites__heart:hover {
  transform: scale(1.2);
}
.user-favorites__card-info {
  padding: 14px;
}
.user-favorites__card-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-favorites__card-price {
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-orange);
}
.user-favorites__card-action {
  padding: 0 14px 14px;
}
.user-favorites__cart-add {
  display: block;
  text-align: center;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid var(--wz-orange);
  color: var(--wz-orange);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.user-favorites__cart-add:hover {
  background: var(--wz-orange);
  color: #fff;
}
.user-favorites__empty {
  text-align: center;
  padding: 80px 24px;
}
.user-favorites__empty-text {
  font-size: 16px;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.user-favorites__empty-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
}
</style>
