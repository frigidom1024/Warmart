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
      <div class="order-detail__header">
        <div class="order-detail__back" @click="router.push('/order/list')">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
          我的订单
        </div>
        <h1 class="order-detail__title">
          订单详情
          <span class="order-detail__badge" :class="`order-detail__badge--s${order.status}`">{{ statusLabels[order.status] || '未知' }}</span>
        </h1>
      </div>

      <!-- Steps + Refund -->
      <div v-if="order.status !== 4" class="order-detail__steps-wrap">
        <div class="order-detail__steps">
          <template v-for="(step, i) in stepConfig" :key="step.key">
            <div
              v-if="order.status !== 5 || (i <= 3 && (i !== 2 || order.deliveryTime))"
              class="order-detail__step"
              :class="[`order-detail__step--${stepStatus(i)}`]"
            >
              <div class="order-detail__step-dot"></div>
              <div class="order-detail__step-info">
                <p class="order-detail__step-label">{{ order.status === 5 && i === 3 ? '退款处理中' : step.label }}</p>
                <p class="order-detail__step-time" v-if="stepStatus(i)">{{ step.timeField && (order as any)[step.timeField] ? ((order as any)[step.timeField] as string).replace('T', ' ').substring(0, 16) : '——' }}</p>
              </div>
            </div>
            <div v-if="i < stepConfig.length - 1 && (order.status !== 5 || (order.deliveryTime ? i < 3 : i < 1))" class="order-detail__step-line"></div>
          </template>
        </div>
        <div v-if="order.status === 5" class="order-detail__refund order-detail__refund--pending">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          <span>退款申请已提交，等待商家处理</span>
        </div>
      </div>

      <!-- Refund results -->
      <div v-if="refundInfo?.status === 'REJECTED'" class="order-detail__refund order-detail__refund--rejected">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <div>
          <span class="order-detail__refund-title">退款申请已被拒绝</span>
          <p v-if="refundInfo.adminReply" class="order-detail__refund-reply">{{ refundInfo.adminReply }}</p>
        </div>
      </div>
      <div v-if="refundInfo?.status === 'APPROVED'" class="order-detail__refund order-detail__refund--approved">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
        <span>退款已完成</span>
      </div>

      <!-- Body: two-column on wide, stacked on narrow -->
      <div class="order-detail__body">

        <!-- Left / main column -->
        <div class="order-detail__main">

          <!-- Items -->
          <section class="order-detail__items">
            <div v-for="item in (order.items || [])" :key="item.id" class="order-detail__item">
              <img v-if="item.productImage" :src="item.productImage" alt="" class="order-detail__item-img">
              <div v-else class="order-detail__item-img order-detail__item-img--empty"></div>
              <div class="order-detail__item-body">
                <p class="order-detail__item-name">{{ item.productName }}</p>
                <p v-if="item.specInfo" class="order-detail__item-spec">{{ item.specInfo }}</p>
              </div>
              <div class="order-detail__item-meta">
                <span class="order-detail__item-price">¥{{ item.price }}</span>
                <span class="order-detail__item-qty">×{{ item.quantity }}</span>
              </div>
            </div>
          </section>

          <!-- Logistics -->
          <section v-if="order.logisticsCompany" class="order-detail__logistics">
            <div class="order-detail__logistics-header">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"/><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"/><circle cx="5.5" cy="18.5" r="2.5"/><circle cx="18.5" cy="18.5" r="2.5"/></svg>
              <span>{{ order.logisticsCompany }}</span>
              <span class="order-detail__logistics-no">{{ order.logisticsNo }}</span>
              <span class="order-detail__copy-btn" @click="copyLogisticsNo">复制</span>
            </div>
            <div v-if="logisticsTracks.length" class="order-detail__logistics-tracks">
              <div
                v-for="(track, i) in logisticsTracks.slice(0, 4)"
                :key="track.id"
                class="order-detail__track-row"
              >
                <div class="order-detail__track-dot" :style="{ background: i === 0 ? logisticsStatusMap[track.status]?.color : '' }"></div>
                <div class="order-detail__track-body">
                  <span class="order-detail__track-status" :style="{ color: i === 0 ? logisticsStatusMap[track.status]?.color : '' }">{{ logisticsStatusMap[track.status]?.label || track.status }}</span>
                  <span class="order-detail__track-msg">{{ track.message }}</span>
                </div>
                <span class="order-detail__track-time">{{ formatTime(track.trackTime) }}</span>
              </div>
              <div v-if="logisticsTracks.length > 4" class="order-detail__track-more">
                <a @click="$router.push('/logistics/' + order.id)">查看完整物流</a>
              </div>
            </div>
          </section>

        </div>

        <!-- Right / sidebar column -->
        <div class="order-detail__side">

          <!-- Receiver -->
          <section class="order-detail__info">
            <h3 class="order-detail__info-title">收货信息</h3>
            <div class="order-detail__info-row">
              <span class="order-detail__info-label">收货人</span>
              <span>{{ order.receiverName }}</span>
            </div>
            <div class="order-detail__info-row">
              <span class="order-detail__info-label">联系电话</span>
              <span>{{ order.receiverPhone }}</span>
            </div>
            <div class="order-detail__info-row">
              <span class="order-detail__info-label">收货地址</span>
              <span class="order-detail__info-address">{{ order.receiverAddress }}</span>
            </div>
          </section>

          <!-- Summary -->
          <section class="order-detail__info">
            <div class="order-detail__info-row">
              <span class="order-detail__info-label">订单编号</span>
              <span class="order-detail__info-mono">{{ order.orderNo }}</span>
            </div>
            <div class="order-detail__info-row">
              <span class="order-detail__info-label">下单时间</span>
              <span>{{ formatTime(order.createdTime) }}</span>
            </div>
            <div class="order-detail__info-row" v-if="order.paymentTime">
              <span class="order-detail__info-label">付款时间</span>
              <span>{{ formatTime(order.paymentTime) }}</span>
            </div>
            <div class="order-detail__info-divider"></div>
            <div class="order-detail__info-row order-detail__info-total">
              <span>{{ order.items?.length || 0 }} 件商品</span>
              <span>合计：<strong>¥{{ order.totalAmount }}</strong></span>
            </div>
          </section>

          <!-- Actions -->
          <div class="order-detail__actions">
            <button
              v-if="order.status === 0"
              class="order-detail__action order-detail__action--primary"
              @click="handlePay"
            >去支付</button>
            <button
              v-if="order.status === 0"
              class="order-detail__action order-detail__action--ghost"
              @click="handleCancel"
            >取消订单</button>
            <button
              v-if="order.status === 2"
              class="order-detail__action order-detail__action--primary"
              @click="handleConfirm"
            >确认收货</button>
            <button
              v-if="order.status === 3"
              class="order-detail__action order-detail__action--primary"
              @click="goToComment"
            >去评价</button>
            <button
              v-if="order.status === 1 || order.status === 2 || order.status === 3"
              class="order-detail__action order-detail__action--danger"
              @click="handleRefund"
            >申请退款</button>
            <button
              v-if="order.status === 5"
              class="order-detail__action order-detail__action--ghost"
              @click="handleCancelRefund"
            >取消退款</button>
          </div>

        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="order-detail__loading">加载中...</div>
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
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}

.order-detail {
  max-width: 960px;
  margin: 0 auto;
  padding: 28px 24px 80px;
}

.order-detail__loading {
  text-align: center;
  padding: 80px 24px;
  color: var(--wz-text-muted);
  font-size: 14px;
}

/* ── Header ── */
.order-detail__header {
  margin-bottom: 32px;
}

.order-detail__back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--wz-text-muted);
  cursor: pointer;
  margin-bottom: 8px;
  transition: color 0.2s;
}

.order-detail__back:hover {
  color: var(--wz-text-soft);
}

.order-detail__title {
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-text);
  letter-spacing: -0.02em;
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 0;
}

.order-detail__badge {
  display: inline-block;
  font-size: 13px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 20px;
  line-height: 1.4;
}

.order-detail__badge--s0 { background: rgba(255, 159, 10, 0.15); color: #ff9f0a; }
.order-detail__badge--s1 { background: rgba(64, 158, 255, 0.15); color: #409eff; }
.order-detail__badge--s2 { background: rgba(255, 159, 10, 0.15); color: #ff9f0a; }
.order-detail__badge--s3 { background: rgba(52, 199, 89, 0.15); color: #34c759; }
.order-detail__badge--s4 { background: rgba(107, 108, 114, 0.2); color: #6b6c72; }
.order-detail__badge--s5 { background: rgba(255, 69, 58, 0.15); color: #ff453a; }

/* ── Steps ── */
.order-detail__steps-wrap {
  margin-bottom: 28px;
}

.order-detail__steps {
  display: flex;
  align-items: flex-start;
  padding: 24px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  overflow-x: auto;
}

.order-detail__step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-shrink: 0;
}

.order-detail__step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--wz-text-muted);
  margin-top: 5px;
  flex-shrink: 0;
  transition: background 0.3s, box-shadow 0.3s;
}

.order-detail__step--completed .order-detail__step-dot {
  background: var(--wz-orange);
  box-shadow: 0 0 0 3px var(--wz-orange-muted);
}

.order-detail__step--active .order-detail__step-dot {
  background: var(--wz-orange);
  box-shadow: 0 0 0 5px var(--wz-orange-muted);
}

.order-detail__step-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text);
  line-height: 1.3;
  white-space: nowrap;
}

.order-detail__step--active .order-detail__step-label {
  color: var(--wz-orange);
}

.order-detail__step-time {
  font-size: 11px;
  color: var(--wz-text-muted);
  margin-top: 2px;
  line-height: 1.3;
}

.order-detail__step-line {
  width: 40px;
  height: 1px;
  background: var(--wz-border);
  margin: 10px 6px 0;
  flex-shrink: 0;
}

.order-detail__step--completed + .order-detail__step-line {
  background: var(--wz-orange-muted);
  height: 2px;
  margin-top: 9.5px;
}

/* ── Refund alerts ── */
.order-detail__refund {
  margin-top: 12px;
  padding: 10px 16px;
  border-radius: var(--wz-radius-sm);
  font-size: 13px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.4;
}

.order-detail__refund--pending {
  background: rgba(255, 159, 10, 0.08);
  border: 1px solid rgba(255, 159, 10, 0.2);
  color: var(--wz-warning);
}

.order-detail__refund--rejected {
  background: rgba(255, 69, 58, 0.08);
  border: 1px solid rgba(255, 69, 58, 0.2);
  color: var(--wz-danger);
}

.order-detail__refund--approved {
  background: rgba(52, 199, 89, 0.08);
  border: 1px solid rgba(52, 199, 89, 0.2);
  color: var(--wz-success);
}

.order-detail__refund-title {
  font-weight: 500;
}

.order-detail__refund-reply {
  font-size: 12px;
  opacity: 0.8;
  margin: 2px 0 0;
}

/* ── Body grid ── */
.order-detail__body {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 28px;
  align-items: start;
}

.order-detail__main {
  min-width: 0;
}

.order-detail__side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── Items ── */
.order-detail__items {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  overflow: hidden;
}

.order-detail__item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
}

.order-detail__item + .order-detail__item {
  border-top: 1px solid var(--wz-border-light);
}

.order-detail__item-img {
  width: 64px;
  height: 64px;
  border-radius: var(--wz-radius-sm);
  object-fit: cover;
  flex-shrink: 0;
  background: var(--wz-bg);
}

.order-detail__item-img--empty {
  background: var(--wz-bg-elevated);
}

.order-detail__item-body {
  flex: 1;
  min-width: 0;
}

.order-detail__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin: 0 0 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-detail__item-spec {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin: 0;
}

.order-detail__item-meta {
  text-align: right;
  flex-shrink: 0;
}

.order-detail__item-price {
  font-size: 15px;
  font-weight: 600;
  color: var(--wz-text);
  display: block;
}

.order-detail__item-qty {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* ── Logistics ── */
.order-detail__logistics {
  margin-top: 16px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 16px 20px;
}

.order-detail__logistics-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--wz-text);
  flex-wrap: wrap;
}

.order-detail__logistics-header svg {
  color: var(--wz-text-muted);
  flex-shrink: 0;
}

.order-detail__logistics-no {
  color: var(--wz-text-soft);
  font-family: var(--wz-font-mono);
  font-size: 12px;
}

.order-detail__copy-btn {
  display: inline-block;
  padding: 1px 8px;
  font-size: 11px;
  color: var(--wz-orange);
  border: 1px solid var(--wz-orange);
  border-radius: 4px;
  cursor: pointer;
  line-height: 18px;
  transition: background 0.2s, color 0.2s;
}

.order-detail__copy-btn:hover {
  background: var(--wz-orange);
  color: #fff;
}

.order-detail__logistics-tracks {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--wz-border-light);
}

.order-detail__track-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 6px 0;
}

.order-detail__track-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--wz-border);
  flex-shrink: 0;
  margin-top: 4px;
}

.order-detail__track-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.order-detail__track-status {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-soft);
}

.order-detail__track-msg {
  font-size: 12px;
  color: var(--wz-text-muted);
}

.order-detail__track-time {
  font-size: 11px;
  color: var(--wz-text-muted);
  white-space: nowrap;
  flex-shrink: 0;
  margin-top: 3px;
}

.order-detail__track-more {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--wz-border-light);
  text-align: center;
}

.order-detail__track-more a {
  font-size: 13px;
  color: var(--wz-orange);
  cursor: pointer;
  text-decoration: none;
}

.order-detail__track-more a:hover {
  color: var(--wz-orange-dark);
}

/* ── Side panel ── */
.order-detail__info {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 18px 20px;
}

.order-detail__info-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--wz-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin: 0 0 12px;
}

.order-detail__info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: var(--wz-text);
  padding: 5px 0;
  line-height: 1.5;
}

.order-detail__info-label {
  color: var(--wz-text-muted);
  flex-shrink: 0;
}

.order-detail__info-address {
  text-align: right;
  max-width: 200px;
}

.order-detail__info-mono {
  font-family: var(--wz-font-mono);
  font-size: 12px;
  color: var(--wz-text-soft);
}

.order-detail__info-divider {
  height: 1px;
  background: var(--wz-border-light);
  margin: 8px 0;
}

.order-detail__info-total {
  font-weight: 600;
}

.order-detail__info-total strong {
  color: var(--wz-orange);
  font-size: 15px;
}

/* ── Actions ── */
.order-detail__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-detail__action {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  border-radius: 21px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--wz-ease-out);
  border: none;
  font-family: inherit;
  padding: 0 20px;
}

.order-detail__action--primary {
  background: var(--wz-orange);
  color: #fff;
}

.order-detail__action--primary:hover {
  background: var(--wz-orange-dark);
}

.order-detail__action--ghost {
  background: transparent;
  color: var(--wz-text-soft);
  border: 1px solid var(--wz-border);
}

.order-detail__action--ghost:hover {
  border-color: var(--wz-text-muted);
  color: var(--wz-text);
}

.order-detail__action--danger {
  background: transparent;
  color: var(--wz-danger);
  border: 1px solid var(--wz-danger);
}

.order-detail__action--danger:hover {
  background: rgba(255, 69, 58, 0.08);
}

/* ── Responsive ── */
@media (max-width: 768px) {
  .order-detail {
    padding: 20px 16px 80px;
  }

  .order-detail__title {
    font-size: 22px;
  }

  .order-detail__body {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .order-detail__side {
    order: -1;
  }

  .order-detail__actions {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .order-detail__action {
    flex: 1;
    min-width: 120px;
  }
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

