<script setup lang="ts">
const pageTitle = '收货地址'
</script>

<template>
  <div class="page-container">
    <div class="user-address">
      <div class="user-address__header">
        <h1 class="user-address__title">{{ pageTitle }}</h1>
        <div class="user-address__add-btn">+ 新增地址</div>
      </div>

      <!-- Address Cards Placeholder -->
      <div class="user-address__list">
        <div v-for="i in 2" :key="i" class="user-address__card">
          <div class="user-address__card-top">
            <span class="user-address__name">张三</span>
            <span class="user-address__phone">138****000{{ i }}</span>
            <span v-if="i === 1" class="user-address__default-badge">默认</span>
          </div>
          <p class="user-address__detail">
            浙江省杭州市西湖区示例街道 {{ i }} 号小区 {{ i }} 栋 {{ i * 100 }} 室
          </p>
          <div class="user-address__card-actions">
            <span class="user-address__action">编辑</span>
            <span class="user-address__action user-address__action--danger">删除</span>
            <span v-if="i !== 1" class="user-address__action">设为默认</span>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div class="user-address__empty" style="display: none;">
        <p class="user-address__empty-text">还没有添加收货地址</p>
        <p class="user-address__empty-hint">添加地址以便下单配送</p>
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
.user-address {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.user-address__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.user-address__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
}
.user-address__add-btn {
  padding: 8px 20px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.user-address__add-btn:hover {
  background: var(--wz-orange-dark);
}
.user-address__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.user-address__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}
.user-address__card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.user-address__card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.user-address__name {
  font-size: 16px;
  font-weight: 500;
  color: var(--wz-text);
}
.user-address__phone {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.user-address__default-badge {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(255, 107, 53, 0.1);
  color: var(--wz-orange);
  font-weight: 500;
}
.user-address__detail {
  font-size: 14px;
  color: var(--wz-text);
  line-height: 1.6;
  margin-bottom: 14px;
}
.user-address__card-actions {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #f5f0eb;
}
.user-address__action {
  font-size: 13px;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: color 0.2s;
}
.user-address__action:hover {
  color: var(--wz-orange);
}
.user-address__action--danger:hover {
  color: #e74c3c;
}
.user-address__empty {
  text-align: center;
  padding: 80px 24px;
}
.user-address__empty-text {
  font-size: 16px;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.user-address__empty-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
}
</style>
