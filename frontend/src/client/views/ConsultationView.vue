<script setup lang="ts">
const pageTitle = '售前咨询'
</script>

<template>
  <div class="page-container">
    <div class="consultation">
      <h1 class="consultation__title">{{ pageTitle }}</h1>

      <!-- Conversation List Placeholder -->
      <div class="consultation__list">
        <div v-for="i in 4" :key="i" class="consultation__item">
          <div class="consultation__avatar"></div>
          <div class="consultation__content">
            <div class="consultation__top">
              <span class="consultation__name">客服 {{ i }}</span>
              <span class="consultation__time">{{ i }} 小时前</span>
            </div>
            <p class="consultation__message">
              {{ i === 1 ? '您好，请问有什么可以帮助您的？' : '关于商品的问题，您可以随时咨询我们。' }}
            </p>
            <div class="consultation__badge" v-if="i === 1">
              <span class="consultation__badge-dot"></span>
              未读消息
            </div>
          </div>
        </div>
      </div>

      <!-- Start New Consultation -->
      <div class="consultation__start">
        <div class="consultation__start-btn">开始新的咨询</div>
      </div>

      <!-- Empty State -->
      <div class="consultation__empty" style="display: none;">
        <p class="consultation__empty-text">暂无咨询记录</p>
        <p class="consultation__empty-hint">联系客服，获取帮助</p>
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
.consultation {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.consultation__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.consultation__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--wz-bg-card, #fff);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}
.consultation__item {
  display: flex;
  gap: 14px;
  padding: 18px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f8f5f2;
}
.consultation__item:last-child {
  border-bottom: none;
}
.consultation__item:hover {
  background: rgba(255, 107, 53, 0.03);
}
.consultation__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  flex-shrink: 0;
}
.consultation__content {
  flex: 1;
  min-width: 0;
}
.consultation__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.consultation__name {
  font-size: 15px;
  font-weight: 500;
  color: var(--wz-text);
}
.consultation__time {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.consultation__message {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
}
.consultation__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--wz-orange);
  font-weight: 500;
}
.consultation__badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--wz-orange);
}
.consultation__start {
  text-align: center;
}
.consultation__start-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  padding: 0 40px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  letter-spacing: 1px;
  transition: background 0.2s;
}
.consultation__start-btn:hover {
  background: var(--wz-orange-dark);
}
.consultation__empty {
  text-align: center;
  padding: 80px 24px;
}
.consultation__empty-text {
  font-size: 16px;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.consultation__empty-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
}
</style>
