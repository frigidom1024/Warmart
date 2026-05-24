<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, cancelOrder, confirmOrder, payOrder } from '@/api/order'
import type { Order } from '@/api/order'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const loading = ref(true)

const stepConfig = [
  { label: '下单成功', key: 'created', timeField: 'createdTime' },
  { label: '已付款', key: 'paid', timeField: 'paymentTime' },
  { label: '已发货', key: 'shipped', timeField: 'deliveryTime' },
  { label: '待收货', key: 'received' },
  { label: '已完成', key: 'done', timeField: 'receiveTime' },
]

const statusLabels: Record<number, string> = {
  0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消'
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    router.replace('/order/list')
    return
  }
  try {
    order.value = await getOrderDetail(id)
  } catch {
    // handled
  } finally {
    loading.value = false
  }
})

async function handleCancel() {
  if (!order.value) return
  try {
    await cancelOrder(order.value.id)
    order.value.status = 4
  } catch { /* handled */ }
}

async function handleConfirm() {
  if (!order.value) return
  try {
    await confirmOrder(order.value.id)
    order.value.status = 3
  } catch { /* handled */ }
}

async function handlePay() {
  if (!order.value) return
  try {
    await payOrder({ orderId: order.value.id, method: order.value.paymentMethod || 'wechat' })
    order.value.status = 1
    showToast('支付成功', 'success')
  } catch { /* handled */ }
}

function stepStatus(stepIndex: number) {
  if (!order.value) return ''
  const s = order.value.status
  if (s === 4) return '' // cancelled
  if (stepIndex === 0) return 'completed'
  if (stepIndex === 1) return s >= 1 ? 'completed' : ''
  if (stepIndex === 2) return s >= 2 ? 'completed' : ''
  if (stepIndex === 3) return s >= 3 ? (s === 3 ? 'active' : 'completed') : ''
  if (stepIndex === 4) return s >= 3 ? 'completed' : ''
  return ''
}
</script>

<template>
  <div class="page-container">
    <div class="order-detail" v-if="order">
      <div class="order-detail__breadcrumb" @click="router.push('/order/list')">我的订单 / 订单详情</div>

      <h1 class="order-detail__title">订单详情</h1>

      <!-- Steps -->
      <section v-if="order.status !== 4" class="order-detail__section">
        <div class="order-detail__steps">
          <template v-for="(step, i) in stepConfig" :key="step.key">
            <div
              class="order-detail__step"
              :class="[`order-detail__step--${stepStatus(i)}`]"
            >
              <div class="order-detail__step-dot"></div>
              <div class="order-detail__step-info">
                <p class="order-detail__step-label">{{ step.label }}</p>
                <p class="order-detail__step-time">{{ step.timeField && (order as any)[step.timeField] || '——' }}</p>
              </div>
            </div>
            <div v-if="i < stepConfig.length - 1" class="order-detail__step-connector"></div>
          </template>
        </div>
      </section>

      <!-- Receiver Info -->
      <section class="order-detail__section">
        <h2 class="order-detail__section-title">收货信息</h2>
        <div class="order-detail__card">
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">收货人</span>
            <span class="order-detail__card-value">{{ order.receiverName }}</span>
          </div>
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">联系电话</span>
            <span class="order-detail__card-value">{{ order.receiverPhone }}</span>
          </div>
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">收货地址</span>
            <span class="order-detail__card-value">{{ order.receiverAddress }}</span>
          </div>
        </div>
      </section>

      <!-- Order Items -->
      <section class="order-detail__section">
        <h2 class="order-detail__section-title">商品信息</h2>
        <div class="order-detail__card">
          <div v-for="item in (order.items || [])" :key="item.id" class="order-detail__item">
            <img v-if="item.productImage" :src="item.productImage" class="order-detail__item-image">
            <div v-else class="order-detail__item-image"></div>
            <div class="order-detail__item-info">
              <p class="order-detail__item-name">{{ item.productName }}</p>
              <p v-if="item.specInfo" class="order-detail__item-spec">{{ item.specInfo }}</p>
            </div>
            <div class="order-detail__item-price">¥{{ item.price }}</div>
            <div class="order-detail__item-qty">x{{ item.quantity }}</div>
          </div>
        </div>
      </section>

      <!-- Summary -->
      <div class="order-detail__summary">
        <div class="order-detail__summary-row">
          <span>订单编号：</span>
          <span>{{ order.orderNo }}</span>
        </div>
        <div class="order-detail__summary-row">
          <span>下单时间：</span>
          <span>{{ order.createdTime }}</span>
        </div>
        <div class="order-detail__summary-row">
          <span>订单状态：</span>
          <span>{{ statusLabels[order.status] || '未知' }}</span>
        </div>
        <div class="order-detail__summary-row order-detail__summary-row--total">
          <span>实付金额：</span>
          <span>¥{{ order.totalAmount }}</span>
        </div>
        <div v-if="order.status === 0 || order.status === 2" class="order-detail__actions">
          <span
            v-if="order.status === 0"
            class="order-detail__action order-detail__action--primary"
            @click="handlePay"
          >去支付</span>
          <span
            v-if="order.status === 0"
            class="order-detail__action order-detail__action--danger"
            @click="handleCancel"
          >取消订单</span>
          <span
            v-if="order.status === 2"
            class="order-detail__action order-detail__action--primary"
            @click="handleConfirm"
          >确认收货</span>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="order-detail order-detail--loading">加载中...</div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.order-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.order-detail__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 12px;
  cursor: pointer;
}
.order-detail__breadcrumb:hover {
  color: var(--wz-orange);
}
.order-detail--loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--wz-text-muted);
}
.order-detail__actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
.order-detail__action {
  flex: 1;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22px;
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
}
.order-detail__action--primary {
  background: var(--wz-orange);
  color: #fff;
}
.order-detail__action--primary:hover {
  background: var(--wz-orange-dark);
}
.order-detail__action--danger {
  border: 1px solid var(--wz-danger, #e74c3c);
  color: var(--wz-danger, #e74c3c);
}
.order-detail__action--danger:hover {
  background: rgba(231, 76, 60, 0.06);
}
.order-detail__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 28px;
}
.order-detail__section {
  margin-bottom: 24px;
}
.order-detail__section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 12px;
}
.order-detail__steps {
  display: flex;
  align-items: flex-start;
  padding: 24px;
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  overflow-x: auto;
}
.order-detail__step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-shrink: 0;
}
.order-detail__step-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e0d8d0;
  margin-top: 4px;
  flex-shrink: 0;
}
.order-detail__step--completed .order-detail__step-dot {
  background: var(--wz-orange);
}
.order-detail__step--active .order-detail__step-dot {
  background: var(--wz-orange);
  box-shadow: 0 0 0 4px rgba(255, 107, 53, 0.2);
}
.order-detail__step-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text);
  white-space: nowrap;
}
.order-detail__step-time {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.order-detail__step-connector {
  width: 40px;
  height: 2px;
  background: #e0d8d0;
  margin: 10px 12px 0;
  flex-shrink: 0;
}
.order-detail__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-detail__card-row {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 14px;
}
.order-detail__card-row:last-child {
  margin-bottom: 0;
}
.order-detail__card-label {
  color: var(--wz-text-soft);
  min-width: 72px;
  flex-shrink: 0;
}
.order-detail__card-value {
  color: var(--wz-text);
}
.order-detail__item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  border-bottom: 1px solid #f0ebe6;
}
.order-detail__item:last-child {
  border-bottom: none;
}
.order-detail__item-image {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  flex-shrink: 0;
  object-fit: cover;
  background: var(--wz-bg);
}
.order-detail__item-qty {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.order-detail__item-info {
  flex: 1;
}
.order-detail__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 4px;
}
.order-detail__item-spec {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.order-detail__item-price {
  font-size: 15px;
  font-weight: 500;
  color: var(--wz-text);
}
.order-detail__item-qty {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.order-detail__summary {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.order-detail__summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 10px;
}
.order-detail__summary-row--total {
  font-size: 17px;
  font-weight: 600;
  color: var(--wz-orange);
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0ebe6;
}
</style>
