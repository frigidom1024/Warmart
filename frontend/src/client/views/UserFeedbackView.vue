<script setup lang="ts">
const pageTitle = '意见反馈'
</script>

<template>
  <div class="page-container">
    <div class="user-feedback">
      <h1 class="user-feedback__title">{{ pageTitle }}</h1>

      <!-- Feedback Form Placeholder -->
      <div class="user-feedback__form">
        <div class="user-feedback__field">
          <span class="user-feedback__label">反馈类型</span>
          <div class="user-feedback__type-selector">
            <span class="user-feedback__type user-feedback__type--active">功能建议</span>
            <span class="user-feedback__type">商品问题</span>
            <span class="user-feedback__type">配送问题</span>
            <span class="user-feedback__type">其他</span>
          </div>
        </div>
        <div class="user-feedback__field">
          <span class="user-feedback__label">反馈内容</span>
          <div class="user-feedback__textarea-placeholder">
            <span class="user-feedback__textarea-hint">请详细描述你的问题或建议…</span>
          </div>
        </div>
        <div class="user-feedback__field">
          <span class="user-feedback__label">联系方式</span>
          <div class="user-feedback__input-placeholder"></div>
        </div>
        <div class="user-feedback__submit-btn">提交反馈</div>
      </div>

      <!-- Previous Feedback List Placeholder -->
      <div class="user-feedback__history">
        <h2 class="user-feedback__history-title">历史反馈</h2>
        <div class="user-feedback__list">
          <div v-for="i in 2" :key="i" class="user-feedback__item">
            <div class="user-feedback__item-top">
              <span class="user-feedback__item-type">功能建议</span>
              <span class="user-feedback__item-status user-feedback__item-status--done">已处理</span>
            </div>
            <p class="user-feedback__item-content">这是第 {{ i }} 条历史反馈的内容摘要，仅作占位展示…</p>
            <span class="user-feedback__item-time">2026-05-1{{ i }}</span>
          </div>
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
.user-feedback {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.user-feedback__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.user-feedback__form {
  background: var(--wz-bg-card, #fff);
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 32px;
}
.user-feedback__field {
  margin-bottom: 20px;
}
.user-feedback__label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 10px;
}
.user-feedback__type-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.user-feedback__type {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid var(--wz-border);
  color: var(--wz-text);
  cursor: pointer;
  transition: all 0.2s;
}
.user-feedback__type:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.user-feedback__type--active {
  background: var(--wz-orange);
  color: #fff;
  border-color: var(--wz-orange);
}
.user-feedback__textarea-placeholder {
  min-height: 120px;
  background: var(--wz-bg);
  border-radius: 10px;
  border: 1px solid var(--wz-border);
  padding: 14px;
  display: flex;
  align-items: flex-start;
}
.user-feedback__textarea-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.user-feedback__input-placeholder {
  height: 42px;
  background: var(--wz-bg);
  border-radius: 8px;
  border: 1px solid var(--wz-border);
}
.user-feedback__submit-btn {
  width: 100%;
  height: 46px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 23px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  letter-spacing: 1px;
  transition: background 0.2s;
  margin-top: 24px;
}
.user-feedback__submit-btn:hover {
  background: var(--wz-orange-dark);
}
.user-feedback__history-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 16px;
}
.user-feedback__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.user-feedback__item {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.user-feedback__item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.user-feedback__item-type {
  font-size: 13px;
  color: var(--wz-orange);
  font-weight: 500;
}
.user-feedback__item-status {
  font-size: 12px;
  padding: 2px 12px;
  border-radius: 10px;
}
.user-feedback__item-status--done {
  background: rgba(76, 175, 80, 0.1);
  color: #4caf50;
}
.user-feedback__item-content {
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 8px;
  line-height: 1.5;
}
.user-feedback__item-time {
  font-size: 12px;
  color: var(--wz-text-soft);
}
</style>
