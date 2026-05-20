<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNoticePage } from '@/api/notice'
import type { Notice } from '@/api/notice'

const router = useRouter()
const notices = ref<Notice[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 10
const activeType = ref('')
const loading = ref(true)

const typeOptions = [
  { label: '全部', value: '' },
  { label: '平台公告', value: '平台公告' },
  { label: '活动通知', value: '活动通知' },
  { label: '系统维护', value: '系统维护' },
]

onMounted(async () => {
  await loadNotices()
})

async function loadNotices() {
  loading.value = true
  try {
    const res = await getNoticePage({
      type: activeType.value || undefined,
      page: currentPage.value,
      size: pageSize
    })
    const pageResult = res as any
    notices.value = pageResult.records || []
    total.value = pageResult.total || 0
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function switchType(value: string) {
  activeType.value = value
  currentPage.value = 1
  loadNotices()
}

function goPage(p: number) {
  currentPage.value = p
  loadNotices()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const selectedNotice = ref<Notice | null>(null)

function toggleDetail(item: Notice) {
  selectedNotice.value = selectedNotice.value?.id === item.id ? null : item
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

</script>

<template>
  <div class="page-container">
    <div class="notice">
      <h1 class="notice__title">系统公告</h1>

      <!-- Filter -->
      <div class="notice__filter">
        <span
          v-for="opt in typeOptions"
          :key="opt.value"
          class="notice__filter-item"
          :class="{ 'notice__filter-item--active': activeType === opt.value }"
          @click="switchType(opt.value)"
        >{{ opt.label }}</span>
      </div>

      <!-- List -->
      <div v-if="notices.length" class="notice__list">
        <div
          v-for="item in notices"
          :key="item.id"
          class="notice__card"
          @click="toggleDetail(item)"
        >
          <div class="notice__card-content">
            <div class="notice__card-top">
              <h3 class="notice__card-title">{{ item.title }}</h3>
              <span v-if="item.type" class="notice__card-tag">{{ item.type }}</span>
            </div>
            <p class="notice__card-summary">{{ item.content }}</p>
            <div
              v-if="selectedNotice?.id === item.id"
              class="notice__card-full"
            >{{ item.content }}</div>
            <div class="notice__card-meta">
              <span class="notice__card-date">{{ item.createdTime }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="!loading" class="notice__empty">
        <p class="notice__empty-text">暂无公告</p>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="notice__pagination">
        <button
          class="notice__page-item"
          :disabled="currentPage <= 1"
          @click="goPage(currentPage - 1)"
        >‹</button>
        <button
          v-for="p in totalPages"
          :key="p"
          class="notice__page-item"
          :class="{ 'notice__page-item--active': p === currentPage }"
          @click="goPage(p)"
        >{{ p }}</button>
        <button
          class="notice__page-item"
          :disabled="currentPage >= totalPages"
          @click="goPage(currentPage + 1)"
        >›</button>
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
.notice__card-full {
  margin-top: 8px;
  padding: 12px;
  background: var(--wz-bg);
  border-radius: 8px;
  font-size: 14px;
  color: var(--wz-text);
  line-height: 1.7;
  white-space: pre-wrap;
}
.notice__empty {
  text-align: center;
  padding: 80px 24px;
  color: var(--wz-text-soft);
}
.notice__empty-text {
  font-size: 16px;
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
  border: 1px solid transparent;
  cursor: pointer;
  font-family: var(--wz-font-body);
  transition: all 0.2s;
}
.notice__page-item:hover:not(:disabled):not(.notice__page-item--active) {
  color: var(--wz-orange);
  border-color: var(--wz-orange);
}
.notice__page-item--active {
  background: var(--wz-orange);
  color: #fff;
}
.notice__page-item:disabled {
  opacity: 0.3;
  cursor: default;
}
</style>
