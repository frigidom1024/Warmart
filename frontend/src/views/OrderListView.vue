<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, cancelOrder, confirmOrder } from '@/api/order'
import type { Order, OrderItem } from '@/api/order'

const router = useRouter()
const orders = ref<Order[]>([])
const activeTab = ref('')
const loading = ref(true)

const orderTabs = [
  { label: '全部', value: '' },
  { label: '待付款', value: '0' },
  { label: '待发货', value: '1' },
  { label: '待收货', value: '2' },
  { label: '已完成', value: '3' },
  { label: '已取消', value: '4' },
]

const statusLabels: Record<number, string> = {
  0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消'
}

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  loading.value = true
  try {
    const status = activeTab.value ? Number(activeTab.value) : undefined
    const res = await getOrderList({ status, size: 50 })
    orders.value = (res as any).records || (res as Order[]) || []
  } finally {
    loading.value = false
  }
}

function switchTab(value: string) {
  activeTab.value = value
  loadOrders()
}

async function handleCancel(id: number) {
  try {
    await cancelOrder(id)
    loadOrders()
  } catch { /* handled */ }
}

async function handleConfirm(id: number) {
  try {
    await confirmOrder(id)
    loadOrders()
  } catch { /* handled */ }
}

function goToComment(order: Order) {
  if (order.items?.length) {
    router.push(`/product/detail/${order.items[0].productId}?orderId=${order.id}`)
  }
}

const orderStatus = (status: number) => statusLabels[status] || '未知'
</script>

<template>
  <div class="page-container">
    <div class="order-list">
      <h1 class="order-list__title">我的订单</h1>

      <!-- Tabs -->
      <div class="order-list__tabs">
        <span
          v-for="tab in orderTabs"
          :key="tab.value"
          class="order-list__tab"
          :class="{ 'order-list__tab--active': activeTab === tab.value }"
          @click="switchTab(tab.value)"
        >{{ tab.label }}</span>
      </div>

      <!-- Order Cards -->
      <div v-if="orders.length" class="order-list__cards">
        <div v-for="order in orders" :key="order.id" class="order-list__card">
          <div class="order-list__card-header" @click="router.push('/order/detail/' + order.id)">
            <span class="order-list__order-id">订单号：{{ order.orderNo }}</span>
            <span class="order-list__order-status">{{ orderStatus(order.status) }}</span>
          </div>
          <div class="order-list__card-body">
            <div
              v-for="item in (order.items || [])"
              :key="item.id"
              class="order-list__item"
              @click="router.push('/product/detail/' + item.productId)"
            >
              <img v-if="item.productImage" :src="item.productImage" class="order-list__item-image">
              <div v-else class="order-list__item-image"></div>
              <div class="order-list__item-info">
                <p class="order-list__item-name">{{ item.productName }}</p>
                <p v-if="item.specInfo" class="order-list__item-spec">{{ item.specInfo }}</p>
              </div>
              <div class="order-list__item-price">¥{{ item.price }}</div>
              <div class="order-list__item-qty">x{{ item.quantity }}</div>
            </div>
          </div>
          <div class="order-list__card-footer">
            <span class="order-list__total">
              共 {{ order.items?.length || 0 }} 件商品 合计：<strong>¥{{ order.totalAmount }}</strong>
            </span>
            <div class="order-list__actions">
              <span
                class="order-list__action order-list__action--secondary"
                @click="router.push('/order/detail/' + order.id)"
              >查看详情</span>
              <span
                v-if="order.status === 0"
                class="order-list__action order-list__action--danger"
                @click="handleCancel(order.id)"
              >取消订单</span>
              <span
                v-if="order.status === 2"
                class="order-list__action order-list__action--primary"
                @click="handleConfirm(order.id)"
              >确认收货</span>
              <span
                v-if="order.status === 3"
                class="order-list__action order-list__action--primary"
                @click="goToComment(order)"
              >去评价</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else-if="!loading" class="order-list__empty">
        <p class="order-list__empty-text">暂无订单</p>
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
.order-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
}
.order-list__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.order-list__tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  padding: 4px;
  background: var(--wz-bg-card, #fff);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-list__tab {
  flex: 1;
  text-align: center;
  padding: 10px 8px;
  font-size: 13px;
  color: var(--wz-text-soft);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.order-list__tab:hover {
  color: var(--wz-orange);
}
.order-list__tab--active {
  background: var(--wz-orange);
  color: #fff;
}
.order-list__cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.order-list__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-list__card-header {
  display: flex;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0ebe6;
  cursor: pointer;
}
.order-list__card-header:hover {
  background: var(--wz-bg-hover);
}
.order-list__order-id {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.order-list__order-status {
  font-size: 13px;
  color: var(--wz-orange);
  font-weight: 500;
}
.order-list__card-body {
  padding: 16px 20px;
}
.order-list__item {
  display: flex;
  align-items: center;
  gap: 14px;
}
.order-list__item-image {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  flex-shrink: 0;
  object-fit: cover;
  background: var(--wz-bg);
}
.order-list__item-qty {
  font-size: 14px;
  color: var(--wz-text-soft);
  margin-left: 8px;
}
.order-list__item-info {
  flex: 1;
}
.order-list__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 4px;
}
.order-list__item-spec {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.order-list__item-price {
  font-size: 15px;
  color: var(--wz-text);
  font-weight: 500;
}
.order-list__card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: var(--wz-bg);
}
.order-list__total {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.order-list__total strong {
  color: var(--wz-orange);
  font-size: 15px;
}
.order-list__actions {
  display: flex;
  gap: 10px;
}
.order-list__action {
  padding: 6px 16px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.order-list__action--secondary {
  border: 1px solid var(--wz-border);
  color: var(--wz-text);
}
.order-list__action--secondary:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.order-list__action--primary {
  background: var(--wz-orange);
  color: #fff;
}
.order-list__action--primary:hover {
  background: var(--wz-orange-dark);
}
.order-list__action--danger {
  border: 1px solid var(--wz-danger, #e74c3c);
  color: var(--wz-danger, #e74c3c);
}
.order-list__action--danger:hover {
  background: rgba(231, 76, 60, 0.06);
}
.order-list__empty {
  text-align: center;
  padding: 80px 24px;
}
.order-list__empty-text {
  font-size: 16px;
  color: var(--wz-text-soft);
}
</style>
