<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail } from '@/api/order'
import { getLogisticsTracks } from '@/api/logistics'
import type { LogisticsTrack } from '@/api/logistics'
import type { Order } from '@/api/order'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const tracks = ref<LogisticsTrack[]>([])
const loading = ref(true)

const statusConfig: Record<string, { label: string; color: string }> = {
  ORDERED: { label: '已下单', color: '#6b6c72' },
  WAREHOUSE: { label: '仓库处理中', color: '#6b6c72' },
  IN_TRANSIT: { label: '运输中', color: '#409eff' },
  PICKUP: { label: '待取件', color: '#ff9f0a' },
  DELIVERED: { label: '已签收', color: '#34c759' }
}

const displayGroups = computed(() => {
  const groups: { status: string; label: string; color: string; items: LogisticsTrack[] }[] = []
  const statusOrder = ['DELIVERED', 'PICKUP', 'IN_TRANSIT', 'WAREHOUSE', 'ORDERED']
  const seenStatuses = new Set<string>()
  for (const track of tracks.value) {
    if (!seenStatuses.has(track.status)) {
      seenStatuses.add(track.status)
    }
  }
  for (const status of statusOrder) {
    if (seenStatuses.has(status)) {
      const items = tracks.value.filter(t => t.status === status)
      groups.push({
        status,
        label: statusConfig[status]?.label || status,
        color: statusConfig[status]?.color || '#6b6c72',
        items
      })
    }
  }
  return groups
})

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

function copyNo(text: string) {
  navigator.clipboard.writeText(text)
  showToast('运单号已复制', 'success')
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) { router.replace('/order/list'); return }
  try {
    const [o, res]: any = await Promise.all([
      getOrderDetail(id),
      getLogisticsTracks(id).catch(() => ({}))
    ])
    order.value = o
    tracks.value = res?.data || res || []
  } catch {} finally { loading.value = false }
})
</script>

<template>
  <div class="page-container">
    <div class="logistics" v-if="order">
      <div class="logistics__breadcrumb" @click="router.push('/order/detail/' + order.id)">订单详情 / 物流跟踪</div>
      <h1 class="logistics__title">物流跟踪</h1>

      <div class="logistics__header">
        <div class="logistics__header-row">
          <span class="logistics__header-label">物流公司</span>
          <span class="logistics__header-value">{{ order.logisticsCompany }}</span>
        </div>
        <div class="logistics__header-row">
          <span class="logistics__header-label">运单编号</span>
          <span class="logistics__header-value">
            {{ order.logisticsNo }}
            <span class="logistics__copy-btn" @click="copyNo(order.logisticsNo || '')">复制</span>
          </span>
        </div>
      </div>

      <div class="logistics__timeline" v-if="displayGroups.length">
        <template v-for="(group, gi) in displayGroups" :key="group.status">
          <div class="logistics__group">
            <div class="logistics__group-header">
              <span class="logistics__group-dot" :style="{ background: gi === 0 ? group.color : '' }"></span>
              <span class="logistics__group-status" :style="{ color: gi === 0 ? group.color : '' }">{{ group.label }}</span>
            </div>
            <div class="logistics__group-items">
              <div v-for="(item, ii) in group.items" :key="item.id" class="logistics__track-item">
                <div class="logistics__track-content">
                  <p class="logistics__track-msg">{{ item.message }}</p>
                  <p class="logistics__track-location" v-if="item.location">{{ item.location }}</p>
                  <p class="logistics__track-time">{{ formatTime(item.trackTime) }}</p>
                </div>
              </div>
            </div>
          </div>
          <div v-if="gi < displayGroups.length - 1" class="logistics__group-connector"></div>
        </template>
      </div>
      <div v-else class="logistics__empty">暂无物流跟踪信息</div>
    </div>
    <div v-else-if="loading" class="logistics__loading">加载中...</div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.logistics {
  max-width: 640px;
  margin: 0 auto;
  padding: 28px 24px 80px;
}
.logistics__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-bottom: 16px;
  cursor: pointer;
}
.logistics__breadcrumb:hover { color: var(--wz-text-soft); }
.logistics__title {
  font-family: var(--wz-font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 28px;
}
.logistics__header {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 20px;
  margin-bottom: 24px;
}
.logistics__header-row {
  display: flex;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}
.logistics__header-row:last-child { margin-bottom: 0; }
.logistics__header-label {
  color: var(--wz-text-muted);
  min-width: 68px;
  flex-shrink: 0;
  font-size: 13px;
}
.logistics__header-value {
  color: var(--wz-text-soft);
  word-break: break-all;
}
.logistics__copy-btn {
  display: inline-block;
  margin-left: 6px;
  padding: 0 7px;
  font-size: 11px;
  color: var(--wz-orange);
  border: 1px solid var(--wz-orange);
  border-radius: 4px;
  cursor: pointer;
  line-height: 18px;
  vertical-align: middle;
}
.logistics__copy-btn:hover {
  background: var(--wz-orange);
  color: #fff;
}
/* Timeline */
.logistics__timeline {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 24px 20px;
}
.logistics__group { position: relative; }
.logistics__group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.logistics__group-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--wz-text-muted);
  flex-shrink: 0;
}
.logistics__group-status {
  font-size: 15px;
  font-weight: 600;
  color: var(--wz-text);
}
.logistics__group-items {
  padding-left: 22px;
}
.logistics__track-item {
  padding: 6px 0 6px 16px;
  border-left: 2px solid var(--wz-border-light);
}
.logistics__track-item:first-child { padding-top: 0; }
.logistics__track-item:last-child { padding-bottom: 0; }
.logistics__track-msg {
  font-size: 13px;
  color: var(--wz-text-soft);
  margin: 0;
}
.logistics__track-location {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin: 2px 0;
}
.logistics__track-time {
  font-size: 11px;
  color: var(--wz-text-muted);
  margin: 2px 0 0;
}
.logistics__group-connector {
  height: 16px;
  margin-left: 5px;
  border-left: 2px dashed var(--wz-border);
}
.logistics__empty {
  text-align: center;
  padding: 40px;
  color: var(--wz-text-muted);
  font-size: 14px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
}
.logistics__loading {
  text-align: center;
  padding: 80px;
  color: var(--wz-text-muted);
}
@media (max-width: 640px) {
  .logistics { padding: 20px 16px 80px; }
}
</style>
