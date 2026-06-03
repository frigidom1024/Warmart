<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
const title = (route.meta.title as string) || ''
</script>

<template>
  <div class="static-page">
    <div class="static-page__card">
      <h1 class="static-page__title">{{ title }}</h1>
      <div class="static-page__divider" />
      <p class="static-page__placeholder">该页面正在建设中，敬请期待…</p>
    </div>
  </div>
</template>

<style scoped>
.static-page {
  min-height: 100vh;
  padding-top: 80px;
  background: var(--wz-bg);
  display: flex;
  justify-content: center;
}

.static-page__card {
  max-width: 600px;
  width: 100%;
  margin: 60px 24px;
  padding: 48px 40px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 14px;
  text-align: center;
}

.static-page__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
}

.static-page__divider {
  width: 40px;
  height: 2px;
  background: var(--wz-orange);
  margin: 16px auto 24px;
  border-radius: 1px;
}

.static-page__placeholder {
  font-size: 15px;
  color: var(--wz-text-muted);
  line-height: 1.6;
}
</style>
