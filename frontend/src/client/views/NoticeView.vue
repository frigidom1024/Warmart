<script setup lang="ts">
const pageTitle = '系统公告'
const noticeTypes = ['全部', '平台公告', '活动通知', '系统维护']
</script>

<template>
  <div class="page-container">
    <div class="notice">
      <h1 class="notice__title">{{ pageTitle }}</h1>

      <!-- Filter Placeholder -->
      <div class="notice__filter">
        <span
          v-for="type in noticeTypes"
          :key="type"
          class="notice__filter-item"
          :class="{ 'notice__filter-item--active': type === '全部' }"
        >
          {{ type }}
        </span>
      </div>

      <!-- Notice Cards Placeholder -->
      <div class="notice__list">
        <div v-for="i in 4" :key="i" class="notice__card">
          <div class="notice__card-icon">📢</div>
          <div class="notice__card-content">
            <div class="notice__card-top">
              <h3 class="notice__card-title">系统公告标题 {{ i }}</h3>
              <span class="notice__card-tag" v-if="i === 1">最新</span>
            </div>
            <p class="notice__card-summary">
              这是公告 {{ i }} 的简要内容描述，点击可查看完整公告详情…
            </p>
            <div class="notice__card-meta">
              <span class="notice__card-type">平台公告</span>
              <span class="notice__card-date">2026-05-1{{ i }}</span>
              <span class="notice__card-views">阅读 {{ i * 100 }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="notice__pagination">
        <span class="notice__page-item notice__page-item--active">1</span>
        <span class="notice__page-item">2</span>
        <span class="notice__page-item">3</span>
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
.notice {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.notice__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.notice__filter {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.notice__filter-item {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid var(--wz-border);
  color: var(--wz-text);
  cursor: pointer;
  transition: all 0.2s;
}
.notice__filter-item:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.notice__filter-item--active {
  background: var(--wz-orange);
  color: #fff;
  border-color: var(--wz-orange);
}
.notice__list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 32px;
}
.notice__card {
  display: flex;
  gap: 16px;
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.notice__card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}
.notice__card-icon {
  font-size: 28px;
  flex-shrink: 0;
  line-height: 1;
}
.notice__card-content {
  flex: 1;
  min-width: 0;
}
.notice__card-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.notice__card-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--wz-text);
  transition: color 0.2s;
}
.notice__card:hover .notice__card-title {
  color: var(--wz-orange);
}
.notice__card-tag {
  font-size: 11px;
  padding: 1px 8px;
  border-radius: 8px;
  background: var(--wz-orange);
  color: #fff;
  font-weight: 500;
}
.notice__card-summary {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notice__card-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--wz-text-soft);
}
.notice__card-type {
  color: var(--wz-orange);
}
.notice__pagination {
  display: flex;
  justify-content: center;
  gap: 8px;
}
.notice__page-item {
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
.notice__page-item:hover {
  color: var(--wz-orange);
}
.notice__page-item--active {
  background: var(--wz-orange);
  color: #fff;
}
</style>
