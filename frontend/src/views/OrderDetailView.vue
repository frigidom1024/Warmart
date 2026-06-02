<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, cancelOrder, confirmOrder, payOrder, applyRefund, cancelRefund, getRefundInfo } from '@/api/order'
import type { Order } from '@/api/order'
import { getLogisticsTracks } from '@/api/logistics'
import type { LogisticsTrack } from '@/api/logistics'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const loading = ref(true)
const refundDialogVisible = ref(false)
const refundReason = ref('')
const refundSubmitting = ref(false)
const refundInfo = ref<{ status: string; adminReply: string | null } | null>(null)
const logisticsTracks = ref<LogisticsTrack[]>([])

const refundPresets = [
  '商品与描述不符',
  '质量问题',
  '不想要了',
  '发错货/漏发',
  '其他原因'
]

function selectPreset(reason: string) {
  refundReason.value = reason
}

const stepConfig = [
  { label: '下单成功', key: 'created', timeField: 'createdTime' },
  { label: '已付款', key: 'paid', timeField: 'paymentTime' },
  { label: '已发货', key: 'shipped', timeField: 'deliveryTime' },
  { label: '待收货', key: 'received' },
  { label: '已完成', key: 'done', timeField: 'receiveTime' },
]

const statusLabels: Record<number, string> = {
  0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中'
}

const logisticsStatusMap: Record<string, { label: string; color: string }> = {
  ORDERED: { label: '已下单', color: '#6b6c72' },
  WAREHOUSE: { label: '仓库处理中', color: '#6b6c72' },
  IN_TRANSIT: { label: '运输中', color: '#409eff' },
  PICKUP: { label: '待取件', color: '#ff9f0a' },
  DELIVERED: { label: '已签收', color: '#34c759' }
}

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    router.replace('/order/list')
    return
  }
  try {
    order.value = await getOrderDetail(id)
    // 获取物流跟踪信息
    if (order.value?.logisticsCompany) {
      try {
        const res: any = await getLogisticsTracks(id)
        logisticsTracks.value = res?.data || res || []
      } catch {}
    }
    // 获取退款申请信息（如果有）
    try {
      const res: any = await getRefundInfo(id)
      const info = res?.data || res
      if (info?.status) refundInfo.value = { status: info.status, adminReply: info.adminReply || null }
    } catch {}
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

async function handleRefund() {
  if (!order.value) return
  refundReason.value = ''
  refundDialogVisible.value = true
}

async function handleCancelRefund() {
  if (!order.value) return
  try {
    await cancelRefund(order.value.id)
    showToast('退款申请已取消', 'success')
    const updated = await getOrderDetail(order.value.id)
    order.value = updated
  } catch { /* handled */ }
}

async function submitRefund() {
  if (!order.value || !refundReason.value.trim()) return
  refundSubmitting.value = true
  try {
    await applyRefund(order.value.id, refundReason.value.trim())
    order.value.status = 5
    refundDialogVisible.value = false
    showToast('退款申请已提交', 'success')
  } catch { /* handled */ }
  finally { refundSubmitting.value = false }
}

function goToComment() {
  if (order.value?.items?.length) {
    router.push(`/product/detail/${order.value.items[0].productId}?orderId=${order.value.id}`)
  }
}

function copyLogisticsNo() {
  if (!order.value?.logisticsNo) return
  navigator.clipboard.writeText(order.value.logisticsNo)
  showToast('运单号已复制', 'success')
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
  if (s === 5) {
    // 退款中：根据实际发货情况显示步骤
    if (stepIndex === 0) return 'completed'
    if (stepIndex === 1) return 'completed'
    // 只有实际已发货才显示"已发货"已完成
    if (stepIndex === 2) return order.value?.deliveryTime ? 'completed' : ''
    if (stepIndex === 3) return 'active'
    return ''
  }
  if (stepIndex === 0) return 'completed'
  if (stepIndex === 1) return s >= 1 ? 'completed' : ''
  if (stepIndex === 2) return s >= 2 ? 'completed' : ''
  if (stepIndex === 3) return s >= 2 ? (s === 2 ? 'active' : 'completed') : ''
  if (stepIndex === 4) return s >= 3 ? (s === 3 ? 'active' : 'completed') : ''
  return ''
}
</script>

<template>
  <div class="page-container">
    <div class="order-detail" v-if="order">
      <!-- Header -->
      <div class="od-header">
        <button class="od-header__back" @click="router.push('/order/list')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          返回
        </button>
        <div class="od-header__status">
          <span class="od-header__status-badge" :class="`od-header__status-badge--${order.status}`">
            {{ statusLabels[order.status] || '未知' }}
          </span>
          <span class="od-header__order-no">#{{ order.orderNo }}</span>
        </div>
      </div>

      <!-- Steps bar -->
      <div v-if="order.status !== 4" class="od-steps">
        <template v-for="(step, i) in stepConfig" :key="step.key">
          <div
            v-if="order.status !== 5 || (i <= 3 && (i !== 2 || order.deliveryTime))"
            class="od-step"
            :class="[`od-step--${stepStatus(i)}`]"
          >
            <div class="od-step__dot"></div>
            <div class="od-step__info">
              <p class="od-step__label">{{ order.status === 5 && i === 3 ? '退款处理中' : step.label }}</p>
              <p class="od-step__time">{{ stepStatus(i) && step.timeField ? ((order as any)[step.timeField] || '') : '' }}</p>
            </div>
          </div>
          <div v-if="i < stepConfig.length - 1 && (order.status !== 5 || (order.deliveryTime ? i < 3 : i < 1))" class="od-step__connector"></div>
        </template>
      </div>

      <!-- Refund banner -->
      <div v-if="order.status === 5" class="od-refund od-refund--pending">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        <span>退款申请已提交，等待商家处理</span>
      </div>
      <div v-if="refundInfo?.status === 'REJECTED'" class="od-refund od-refund--rejected">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <div>
          <p class="od-refund__title">退款申请已被拒绝</p>
          <p v-if="refundInfo.adminReply" class="od-refund__reply">{{ refundInfo.adminReply }}</p>
        </div>
      </div>
      <div v-if="refundInfo?.status === 'APPROVED'" class="od-refund od-refund--approved">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
        <span>退款已完成</span>
      </div>

      <!-- Shipment panel: receiver + logistics + timeline merged -->
      <div class="od-shipment">
        <div class="od-shipment__main">
          <div class="od-shipment__receiver">
            <p class="od-shipment__name">{{ order.receiverName }}</p>
            <p class="od-shipment__phone">{{ order.receiverPhone }}</p>
            <p class="od-shipment__address">{{ order.receiverAddress }}</p>
          </div>
          <div class="od-shipment__divider"></div>
          <div class="od-shipment__logistics" v-if="order.logisticsCompany">
            <div class="od-shipment__courier" @click="$router.push('/logistics/' + order.id)">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
              <span class="od-shipment__courier-text">{{ order.logisticsCompany }} · {{ order.logisticsNo }}</span>
              <button class="od-shipment__copy" @click.stop="copyLogisticsNo">复制</button>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="od-shipment__arrow"><polyline points="9 18 15 12 9 6"/></svg>
            </div>
            <p class="od-shipment__delivery-time" v-if="order.deliveryTime">发货 {{ formatTime(order.deliveryTime) }}</p>

            <!-- Compact timeline inside shipment -->
            <div class="od-shipment__tracks" v-if="logisticsTracks.length">
              <div
                v-for="(track, i) in logisticsTracks.slice(0, 3)"
                :key="track.id"
                class="od-shipment__track"
              >
                <span class="od-shipment__track-dot" :style="{ background: i === 0 ? logisticsStatusMap[track.status]?.color : '' }"></span>
                <span class="od-shipment__track-label" :style="{ color: i === 0 ? logisticsStatusMap[track.status]?.color : '' }">
                  {{ logisticsStatusMap[track.status]?.label || track.status }}
                </span>
                <span class="od-shipment__track-time">{{ formatTime(track.trackTime) }}</span>
              </div>
              <button v-if="logisticsTracks.length > 3" class="od-shipment__view-all" @click="$router.push('/logistics/' + order.id)">
                查看完整物流
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Order Items -->
      <div class="od-items">
        <div v-for="item in (order.items || [])" :key="item.id" class="od-item">
          <img v-if="item.productImage" :src="item.productImage" class="od-item__image">
          <div v-else class="od-item__image od-item__image--empty"></div>
          <div class="od-item__body">
            <p class="od-item__name">{{ item.productName }}</p>
            <p v-if="item.specInfo" class="od-item__spec">{{ item.specInfo }}</p>
          </div>
          <div class="od-item__pricing">
            <p class="od-item__price">¥{{ item.price }}</p>
            <p class="od-item__qty">x{{ item.quantity }}</p>
            <p class="od-item__subtotal">¥{{ item.subtotal }}</p>
          </div>
        </div>
      </div>

      <!-- Summary + Actions -->
      <div class="od-summary">
        <div class="od-summary__rows">
          <div class="od-summary__row">
            <span>订单编号</span>
            <span>{{ order.orderNo }}</span>
          </div>
          <div class="od-summary__row">
            <span>下单时间</span>
            <span>{{ order.createdTime }}</span>
          </div>
          <div class="od-summary__row">
            <span>订单状态</span>
            <span>{{ statusLabels[order.status] || '未知' }}</span>
          </div>
        </div>
        <div class="od-summary__total">
          <span>实付金额</span>
          <span class="od-summary__amount">¥{{ order.totalAmount }}</span>
        </div>
      </div>

      <div class="od-actions">
        <button v-if="order.status === 0" class="od-btn od-btn--primary" @click="handlePay">去支付</button>
        <button v-if="order.status === 0" class="od-btn od-btn--ghost" @click="handleCancel">取消订单</button>
        <button v-if="order.status === 2" class="od-btn od-btn--primary" @click="handleConfirm">确认收货</button>
        <button v-if="order.status === 3" class="od-btn od-btn--primary" @click="goToComment">去评价</button>
        <button v-if="order.status === 1 || order.status === 2 || order.status === 3" class="od-btn od-btn--danger" @click="handleRefund">申请退款</button>
        <button v-if="order.status === 5" class="od-btn od-btn--ghost" @click="handleCancelRefund">取消退款</button>
      </div>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="od-loading">加载中...</div>
  </div>

  <!-- Refund Dialog -->
  <el-dialog v-model="refundDialogVisible" title="申请退款" width="440px" class="refund-dialog">
    <div class="refund-dialog__body">
      <div class="refund-dialog__amount">
        <span class="refund-dialog__amount-label">退款金额</span>
        <span class="refund-dialog__amount-value">¥{{ order?.totalAmount }}</span>
      </div>
      <div class="refund-dialog__section">
        <p class="refund-dialog__section-title">退款原因</p>
        <div class="refund-dialog__presets">
          <span
            v-for="reason in refundPresets"
            :key="reason"
            class="refund-dialog__preset"
            :class="{ 'refund-dialog__preset--active': refundReason === reason }"
            @click="selectPreset(reason)"
          >{{ reason }}</span>
        </div>
        <div class="refund-dialog__textarea-wrap">
          <textarea
            v-model="refundReason"
            class="refund-dialog__textarea"
            :rows="3"
            placeholder="请详细描述退款原因，这将帮助商家更快处理您的申请"
            maxlength="500"
          ></textarea>
          <span class="refund-dialog__textarea-count">{{ refundReason.length }}/500</span>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="refund-dialog__footer">
        <button class="refund-dialog__btn refund-dialog__btn--cancel" @click="refundDialogVisible = false">取消</button>
        <button class="refund-dialog__btn refund-dialog__btn--submit" :disabled="!refundReason.trim() || refundSubmitting" @click="submitRefund">
          {{ refundSubmitting ? '提交中...' : '提交申请' }}
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.page-container { min-height: 100vh; padding-top: 64px; background: var(--wz-bg); }
.order-detail { max-width: 640px; margin: 0 auto; padding: 24px 20px 80px; }
.od-loading { display: flex; align-items: center; justify-content: center; min-height: 300px; color: var(--wz-text-muted); font-size: 14px; }
.od-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 28px; }
.od-header__back { display: inline-flex; align-items: center; gap: 4px; background: none; border: none; color: var(--wz-text-muted); font-size: 13px; cursor: pointer; padding: 4px 8px; border-radius: 6px; font-family: inherit; transition: color .2s, background .2s; }
.od-header__back:hover { color: var(--wz-text); background: var(--wz-bg-hover); }
.od-header__status { display: flex; align-items: center; gap: 10px; }
.od-header__status-badge { font-size: 12px; font-weight: 600; padding: 4px 12px; border-radius: 99px; }
.od-header__status-badge--0 { background: rgba(255,159,10,0.15); color: var(--wz-warning); }
.od-header__status-badge--1 { background: rgba(64,158,255,0.15); color: #409eff; }
.od-header__status-badge--2 { background: rgba(255,255,255,0.08); color: var(--wz-text-soft); }
.od-header__status-badge--3 { background: rgba(52,199,89,0.15); color: var(--wz-success); }
.od-header__status-badge--4 { background: rgba(107,108,114,0.2); color: var(--wz-text-muted); }
.od-header__status-badge--5 { background: rgba(255,69,58,0.15); color: var(--wz-danger); }
.od-header__order-no { font-size: 13px; color: var(--wz-text-muted); font-family: var(--wz-font-mono,monospace); }
.od-steps { display: flex; align-items: flex-start; padding: 20px; background: var(--wz-bg-card); border: 1px solid var(--wz-border); border-radius: 12px; margin-bottom: 20px; overflow-x: auto; }
.od-step { display: flex; align-items: flex-start; gap: 8px; flex-shrink: 0; }
.od-step__dot { width: 8px; height: 8px; border-radius: 50%; background: var(--wz-text-muted); margin-top: 4px; flex-shrink: 0; transition: background .3s, box-shadow .3s; }
.od-step--completed .od-step__dot { background: var(--wz-orange); box-shadow: 0 0 0 3px rgba(255,107,53,0.15); }
.od-step--active .od-step__dot { background: var(--wz-orange); box-shadow: 0 0 0 5px rgba(255,107,53,0.2); }
.od-step__label { font-size: 12px; font-weight: 500; color: var(--wz-text); line-height: 1.3; white-space: nowrap; }
.od-step--active .od-step__label { color: var(--wz-orange); }
.od-step__time { font-size: 10px; color: var(--wz-text-muted); margin-top: 1px; line-height: 1.2; }
.od-step__connector { width: 32px; height: 1px; background: var(--wz-border); margin: 7px 6px 0; flex-shrink: 0; }
.od-step--completed + .od-step__connector { background: rgba(255,107,53,0.3); height: 2px; }
.od-refund { margin-bottom: 20px; padding: 12px 16px; border-radius: 10px; font-size: 13px; display: flex; align-items: flex-start; gap: 10px; line-height: 1.5; }
.od-refund--pending { background: rgba(255,159,10,0.08); border: 1px solid rgba(255,159,10,0.2); color: var(--wz-warning); }
.od-refund--rejected { background: rgba(255,69,58,0.08); border: 1px solid rgba(255,69,58,0.2); color: var(--wz-danger); }
.od-refund--approved { background: rgba(52,199,89,0.08); border: 1px solid rgba(52,199,89,0.2); color: var(--wz-success); }
.od-refund svg { flex-shrink: 0; margin-top: 2px; }
.od-refund__title { font-weight: 500; margin: 0; }
.od-refund__reply { font-size: 12px; opacity: 0.8; margin: 2px 0 0; }
.od-shipment { background: var(--wz-bg-card); border: 1px solid var(--wz-border); border-radius: 12px; margin-bottom: 20px; overflow: hidden; }
.od-shipment__main { padding: 20px; }
.od-shipment__receiver { display: flex; flex-direction: column; gap: 4px; }
.od-shipment__name { font-size: 15px; font-weight: 600; color: var(--wz-text); margin: 0; }
.od-shipment__phone { font-size: 12px; color: var(--wz-text-soft); margin: 0; }
.od-shipment__address { font-size: 12px; color: var(--wz-text-muted); margin: 0; line-height: 1.5; }
.od-shipment__divider { height: 1px; background: var(--wz-border-light); margin: 14px 0; }
.od-shipment__courier { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--wz-text-soft); margin-bottom: 4px; cursor: pointer; padding: 4px 0; border-radius: 6px; transition: background .2s; }
.od-shipment__courier:hover { background: var(--wz-bg-hover); }
.od-shipment__courier svg { flex-shrink: 0; color: var(--wz-text-muted); }
.od-shipment__courier-text { flex: 1; }
.od-shipment__arrow { color: var(--wz-text-muted); transition: transform .2s; }
.od-shipment__courier:hover .od-shipment__arrow { color: var(--wz-orange); transform: translateX(2px); }
.od-shipment__copy { display: inline-block; padding: 0 8px; font-size: 11px; color: var(--wz-orange); background: none; border: 1px solid var(--wz-orange); border-radius: 4px; cursor: pointer; line-height: 20px; font-family: inherit; transition: background .2s, color .2s; }
.od-shipment__copy:hover { background: var(--wz-orange); color: #fff; }
.od-shipment__delivery-time { font-size: 11px; color: var(--wz-text-muted); margin: 2px 0 12px; }
.od-shipment__tracks { display: flex; flex-direction: column; gap: 6px; padding-top: 10px; border-top: 1px solid var(--wz-border-light); }
.od-shipment__track { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.od-shipment__track-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--wz-border); flex-shrink: 0; }
.od-shipment__track-label { color: var(--wz-text-muted); flex: 1; }
.od-shipment__track-time { color: var(--wz-text-muted); font-size: 11px; flex-shrink: 0; }
.od-shipment__view-all { display: inline-flex; align-items: center; gap: 4px; background: none; border: none; color: var(--wz-orange); font-size: 12px; cursor: pointer; padding: 4px 0; font-family: inherit; margin-top: 2px; }
.od-shipment__view-all:hover { color: var(--wz-orange-dark); }
.od-items { background: var(--wz-bg-card); border: 1px solid var(--wz-border); border-radius: 12px; margin-bottom: 20px; overflow: hidden; }
.od-item { display: flex; align-items: center; gap: 14px; padding: 16px 20px; border-bottom: 1px solid var(--wz-border-light); }
.od-item:last-child { border-bottom: none; }
.od-item__image { width: 64px; height: 64px; border-radius: 8px; flex-shrink: 0; object-fit: cover; background: var(--wz-bg); }
.od-item__image--empty { background: var(--wz-bg-elevated); }
.od-item__body { flex: 1; min-width: 0; }
.od-item__name { font-size: 14px; font-weight: 500; color: var(--wz-text); margin: 0 0 3px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.od-item__spec { font-size: 12px; color: var(--wz-text-muted); margin: 0; }
.od-item__pricing { text-align: right; flex-shrink: 0; }
.od-item__price { font-size: 14px; font-weight: 600; color: var(--wz-text); margin: 0; }
.od-item__qty { font-size: 12px; color: var(--wz-text-muted); margin: 2px 0; }
.od-item__subtotal { font-size: 13px; font-weight: 500; color: var(--wz-text-soft); margin: 0; }
.od-summary { background: var(--wz-bg-card); border: 1px solid var(--wz-border); border-radius: 12px; margin-bottom: 20px; padding: 20px; }
.od-summary__rows { display: flex; flex-direction: column; gap: 6px; }
.od-summary__row { display: flex; justify-content: space-between; font-size: 13px; color: var(--wz-text-muted); line-height: 1.5; }
.od-summary__total { display: flex; justify-content: space-between; align-items: center; margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--wz-border); font-size: 14px; color: var(--wz-text); font-weight: 500; }
.od-summary__amount { font-size: 20px; font-weight: 700; color: var(--wz-orange); letter-spacing: -0.3px; }
.od-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.od-btn { flex: 1; min-width: 100px; height: 42px; display: flex; align-items: center; justify-content: center; border-radius: 21px; font-size: 13px; font-weight: 600; cursor: pointer; border: none; font-family: inherit; transition: background .2s, color .2s, border-color .2s, box-shadow .2s; }
.od-btn--primary { background: var(--wz-orange); color: #fff; }
.od-btn--primary:hover { background: var(--wz-orange-dark); box-shadow: 0 0 20px rgba(255,107,53,0.25); }
.od-btn--ghost { background: transparent; color: var(--wz-text-soft); border: 1px solid var(--wz-border); }
.od-btn--ghost:hover { border-color: var(--wz-text-muted); color: var(--wz-text); }
.od-btn--danger { background: transparent; color: var(--wz-danger); border: 1px solid var(--wz-danger); }
.od-btn--danger:hover { background: rgba(255,69,58,0.08); }
@media (max-width:640px) {
  .order-detail { padding: 16px 14px 80px; }
  .od-header { flex-direction: column; align-items: flex-start; gap: 10px; }
  .od-item { padding: 12px 14px; }
  .od-item__image { width: 52px; height: 52px; }
  .od-btn { font-size: 12px; height: 38px; min-width: 80px; }
}
</style>

<style>
.refund-dialog.el-dialog,
.refund-dialog {
  --el-dialog-bg-color: var(--wz-bg-card);
  --el-dialog-border: 1px solid var(--wz-border);
  --el-dialog-border-radius: var(--wz-radius-md);
  --el-dialog-box-shadow: var(--wz-shadow-xl);
  --el-dialog-title-font-size: 17px;
  --el-dialog-title-font-weight: 600;
  --el-dialog-content-font-size: 14px;
  --el-dialog-padding-primary: 0;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  box-shadow: var(--wz-shadow-xl);
}
.refund-dialog .el-overlay {
  background: rgba(0, 0, 0, 0.6) !important;
}
.refund-dialog .el-dialog__header {
  padding: 20px 24px 0 !important;
  margin: 0 !important;
  border-bottom: none !important;
}
.refund-dialog .el-dialog__headerbtn {
  top: 20px !important;
  right: 20px !important;
}
.refund-dialog .el-dialog__headerbtn .el-dialog__close {
  color: var(--wz-text-muted) !important;
}
.refund-dialog .el-dialog__headerbtn .el-dialog__close:hover {
  color: var(--wz-text) !important;
}
.refund-dialog .el-dialog__title {
  font-size: 17px !important;
  font-weight: 600 !important;
  color: var(--wz-text) !important;
}
</style>

