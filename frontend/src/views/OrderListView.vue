<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getOrderList, cancelOrder, confirmOrder, applyRefund } from '@/api/order'
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
  { label: '退款中', value: '5' },
]

const statusLabels: Record<number, string> = {
  0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中'
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

async function handleRefund(id: number) {
  if (!window.confirm('确定要申请退款吗？')) return
  try {
    await applyRefund(id)
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
              <span
                v-if="order.status === 2 || order.status === 3"
                class="order-list__action order-list__action--danger"
                @click="handleRefund(order.id)"
              >申请退款</span>
              <span
                v-if="order.logisticsCompany"
                class="order-list__action order-list__action--secondary"
                @click="router.push('/order/detail/' + order.id)"
              >查看物流</span>
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
  padding: 28px 24px;
}

.order-list__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--wz-text);
  letter-spacing: 0.02em;
  margin-bottom: 28px;
}

/* ── Tabs ── */
.order-list__tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 28px;
  padding: 3px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 10px;
}

.order-list__tab {
  flex: 1;
  text-align: center;
  padding: 8px 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-muted);
  border-radius: 8px;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}
.order-list__tab:hover {
  color: var(--wz-text-soft);
}
.order-list__tab--active {
  background: var(--wz-orange);
  color: #fff;
}
.order-list__tab--active:hover {
  color: #fff;
}

/* ── Cards ── */
.order-list__cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-list__card {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  overflow: hidden;
  transition: border-color var(--wz-duration-normal) var(--wz-ease-out);
}

.order-list__card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--wz-border-light);
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}
.order-list__card-header:hover {
  background: var(--wz-bg-hover);
}

.order-list__order-id {
  font-size: 13px;
  color: var(--wz-text-muted);
}

.order-list__order-status {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-orange);
}

.order-list__card-body {
  padding: 12px 20px;
}

.order-list__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--wz-border-light);
}
.order-list__item:last-child {
  border-bottom: none;
}

.order-list__item-image {
  width: 56px;
  height: 56px;
  border-radius: var(--wz-radius-sm);
  flex-shrink: 0;
  object-fit: cover;
  background: var(--wz-bg);
}

.order-list__item-info {
  flex: 1;
  min-width: 0;
}

.order-list__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-list__item-spec {
  font-size: 12px;
  color: var(--wz-text-muted);
}

.order-list__item-price {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  white-space: nowrap;
}

.order-list__item-qty {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-left: 4px;
}

/* ── Footer ── */
.order-list__card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: rgba(0, 0, 0, 0.15);
}

.order-list__total {
  font-size: 13px;
  color: var(--wz-text-muted);
}
.order-list__total strong {
  color: var(--wz-orange);
  font-size: 14px;
}

.order-list__actions {
  display: flex;
  gap: 8px;
}

.order-list__action {
  padding: 5px 14px;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
  font-weight: 500;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
  user-select: none;
}

.order-list__action--secondary {
  border: 1px solid var(--wz-border);
  color: var(--wz-text-muted);
  background: transparent;
}
.order-list__action--secondary:hover {
  border-color: var(--wz-text-muted);
  color: var(--wz-text-soft);
}

.order-list__action--primary {
  background: var(--wz-orange);
  color: #fff;
  border: none;
}
.order-list__action--primary:hover {
  background: var(--wz-orange-dark);
}

.order-list__action--danger {
  border: 1px solid var(--wz-danger);
  color: var(--wz-danger);
  background: transparent;
}
.order-list__action--danger:hover {
  background: rgba(255, 69, 58, 0.08);
}

.order-list__empty {
  text-align: center;
  padding: 80px 24px;
}

.order-list__empty-text {
  font-size: 15px;
  color: var(--wz-text-muted);
}

@media (max-width: 640px) {
  .order-list {
    padding: 20px 16px;
  }
  .order-list__title {
    font-size: 22px;
    margin-bottom: 20px;
  }
  .order-list__card-header,
  .order-list__card-body,
  .order-list__card-footer {
    padding-left: 16px;
    padding-right: 16px;
  }
  .order-list__item-image {
    width: 48px;
    height: 48px;
  }
}
</style>
