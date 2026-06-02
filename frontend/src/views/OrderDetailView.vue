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

function copyNo(text: string) {
  navigator.clipboard.writeText(text)
  showToast('已复制', 'success')
}

function stepClass(i: number) {
  if (!order.value) return ''
  const s = order.value.status
  if (s === 4) return ''
  if (i === 0) return 'od__step--done'
  if (i === 1) return s >= 1 ? 'od__step--done' : ''
  if (i === 2) return s >= 2 ? 'od__step--done' : ''
  if (i === 3) return s === 2 ? 'od__step--active' : s > 2 ? 'od__step--done' : ''
  if (i === 4) return s >= 3 ? (s === 3 ? 'od__step--active' : 'od__step--done') : ''
  return ''
}

function statusColor(status: string) {
  const map: Record<string, string> = {
    ORDERED: 'var(--wz-text-muted)',
    WAREHOUSE: 'var(--wz-text-muted)',
    IN_TRANSIT: '#409eff',
    PICKUP: '#ff9f0a',
    DELIVERED: '#34c759'
  }
  return map[status] || 'var(--wz-text-muted)'
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
    <div class="od" v-if="order">
      <div class="od__top">
        <div class="od__breadcrumb" @click="router.push('/order/list')">← 我的订单</div>
        <div class="od__status" v-if="order.status === 0">待付款</div>
        <div class="od__status od__status--ship" v-else-if="order.status === 1">待发货</div>
        <div class="od__status od__status--ship" v-else-if="order.status === 2">待收货</div>
        <div class="od__status od__status--done" v-else-if="order.status === 3">已完成</div>
        <div class="od__status od__status--muted" v-else-if="order.status === 4">已取消</div>
        <div class="od__status od__status--warn" v-else-if="order.status === 5">退款中</div>
      </div>

      <div v-if="order.status !== 4" class="od__steps">
        <div v-for="i in 5" :key="i" class="od__step" :class="[stepClass(i-1)]">
          <div class="od__step-dot"></div>
        </div>
      </div>

      <div v-if="order.status === 5" class="od__banner od__banner--pending">
        <span>退款申请已提交，等待商家处理</span>
      </div>
      <div v-if="refundInfo?.status === 'REJECTED'" class="od__banner od__banner--rejected">
        <span>退款申请已被拒绝</span>
        <span v-if="refundInfo.adminReply" class="od__banner-reply">{{ refundInfo.adminReply }}</span>
      </div>
      <div v-if="refundInfo?.status === 'APPROVED'" class="od__banner od__banner--approved">
        <span>退款已完成</span>
      </div>

      <div class="od__surface">
        <div class="od__section">
          <div class="od__field">
            <span class="od__field-label">收货人</span>
            <span class="od__field-value">{{ order.receiverName }}</span>
          </div>
          <div class="od__field">
            <span class="od__field-label">联系电话</span>
            <span class="od__field-value">{{ order.receiverPhone }}</span>
          </div>
          <div class="od__field od__field--addr">
            <span class="od__field-label">收货地址</span>
            <span class="od__field-value">{{ order.receiverAddress }}</span>
          </div>
        </div>
        <div class="od__divider"></div>
        <div class="od__section od__section--items">
          <div v-for="item in (order.items || [])" :key="item.id" class="od__item">
            <img v-if="item.productImage" :src="item.productImage" class="od__item-img">
            <div v-else class="od__item-img od__item-img--empty"></div>
            <div class="od__item-info">
              <p class="od__item-name">{{ item.productName }}</p>
              <p v-if="item.specInfo" class="od__item-spec">{{ item.specInfo }}</p>
            </div>
            <div class="od__item-right">
              <span class="od__item-price">¥{{ item.price }}</span>
              <span class="od__item-qty">×{{ item.quantity }}</span>
            </div>
          </div>
        </div>
        <div class="od__divider"></div>
        <div class="od__section od__section--total">
          <div class="od__total-row">
            <span>共 {{ order.items?.length || 0 }} 件商品</span>
            <span class="od__total-amount">合计 <strong>¥{{ order.totalAmount }}</strong></span>
          </div>
          <div class="od__meta-row">
            <span>订单编号：{{ order.orderNo }}</span>
            <span class="od__copy" @click="copyNo(order.orderNo)">复制</span>
          </div>
          <div class="od__meta-row" v-if="order.createdTime">
            <span>下单时间：{{ order.createdTime.replace('T', ' ') }}</span>
          </div>
        </div>
      </div>

      <div v-if="order.logisticsCompany" class="od__surface od__surface--logistics">
        <div class="od__section">
          <div class="od__logi-row">
            <span>{{ order.logisticsCompany }}</span>
            <span class="od__logi-divider">·</span>
            <span>{{ order.logisticsNo }}</span>
            <span class="od__copy" @click="copyLogisticsNo">复制</span>
          </div>
          <div v-if="logisticsTracks.length" class="od__logi-tracks">
            <div v-for="(track, i) in logisticsTracks.slice(0, 4)" :key="track.id" class="od__logi-track">
              <span class="od__logi-dot" :style="{ background: statusColor(track.status) }"></span>
              <span class="od__logi-text">{{ track.message || formatTime(track.trackTime) }}</span>
            </div>
            <div v-if="logisticsTracks.length > 4" class="od__logi-more" @click="$router.push('/logistics/' + order?.id)">查看完整物流 →</div>
          </div>
        </div>
      </div>

      <div class="od__actions">
        <button v-if="order.status === 0" class="od__btn od__btn--primary" @click="handlePay">去支付</button>
        <button v-if="order.status === 0" class="od__btn od__btn--ghost" @click="handleCancel">取消订单</button>
        <button v-if="order.status === 1 || order.status === 2 || order.status === 3" class="od__btn od__btn--ghost od__btn--danger" @click="handleRefund">申请退款</button>
        <button v-if="order.status === 2" class="od__btn od__btn--primary" @click="handleConfirm">确认收货</button>
        <button v-if="order.status === 3" class="od__btn od__btn--primary" @click="goToComment">去评价</button>
        <button v-if="order.status === 5" class="od__btn od__btn--ghost" @click="handleCancelRefund">取消退款</button>
      </div>
    </div>
    <div v-else-if="loading" class="od__loading">加载中...</div>

    <el-dialog v-model="refundDialogVisible" title="申请退款" width="440px" class="refund-dialog">
      <div class="refund-dialog__body">
        <div class="refund-dialog__amount">
          <span class="refund-dialog__amount-label">退款金额</span>
          <span class="refund-dialog__amount-value">¥{{ order?.totalAmount }}</span>
        </div>
        <div class="refund-dialog__section">
          <p class="refund-dialog__section-title">退款原因</p>
          <div class="refund-dialog__presets">
            <span v-for="reason in refundPresets" :key="reason" class="refund-dialog__preset" :class="{ 'refund-dialog__preset--active': refundReason === reason }" @click="selectPreset(reason)">{{ reason }}</span>
          </div>
          <div class="refund-dialog__textarea-wrap">
            <textarea v-model="refundReason" class="refund-dialog__textarea" :rows="3" placeholder="请详细描述退款原因，这将帮助商家更快处理您的申请" maxlength="500"></textarea>
            <span class="refund-dialog__textarea-count">{{ refundReason.length }}/500</span>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="refund-dialog__footer">
          <button class="refund-dialog__btn refund-dialog__btn--cancel" @click="refundDialogVisible = false">取消</button>
          <button class="refund-dialog__btn refund-dialog__btn--submit" :disabled="!refundReason.trim() || refundSubmitting" @click="submitRefund">{{ refundSubmitting ? '提交中...' : '提交申请' }}</button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.od {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px 24px 60px;
}
.od__loading {
  text-align: center;
  padding: 80px;
  color: var(--wz-text-muted);
  font-size: 14px;
}

/* Top bar */
.od__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.od__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-muted);
  cursor: pointer;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}
.od__breadcrumb:hover { color: var(--wz-text-soft); }
.od__status {
  padding: 4px 14px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
}
.od__status--ship { color: #409eff; background: rgba(64,158,255,0.12); }
.od__status--done { color: var(--wz-success); background: rgba(52,199,89,0.12); }
.od__status--muted { color: var(--wz-text-muted); background: var(--wz-bg-card); border: 1px solid var(--wz-border); }
.od__status--warn { color: var(--wz-warning); background: rgba(255,159,10,0.12); }

/* Steps */
.od__steps {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
}
.od__step {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
}
.od__step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--wz-text-muted);
  transition: all var(--wz-duration-normal) var(--wz-ease-out);
}
.od__step--done .od__step-dot { background: var(--wz-orange); box-shadow: 0 0 0 3px var(--wz-orange-muted); }
.od__step--active .od__step-dot { background: var(--wz-orange); box-shadow: 0 0 0 4px var(--wz-orange-muted); }

/* Banners */
.od__banner {
  padding: 10px 16px;
  border-radius: var(--wz-radius-sm);
  font-size: 13px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.od__banner--pending { background: rgba(255,159,10,0.08); border: 1px solid rgba(255,159,10,0.2); color: var(--wz-warning); }
.od__banner--rejected { background: rgba(255,69,58,0.08); border: 1px solid rgba(255,69,58,0.2); color: var(--wz-danger); }
.od__banner--approved { background: rgba(52,199,89,0.08); border: 1px solid rgba(52,199,89,0.2); color: var(--wz-success); }
.od__banner-reply { font-size: 12px; opacity: 0.8; }

/* Unified surface */
.od__surface {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  margin-bottom: 12px;
  overflow: hidden;
}
.od__surface--logistics { margin-bottom: 20px; }
.od__section { padding: 16px 20px; }
.od__section--items { padding: 8px 20px; }
.od__section--total { padding: 14px 20px; }
.od__divider { height: 1px; background: var(--wz-border-light); margin: 0; }

/* Fields (receiver) */
.od__field {
  display: flex;
  gap: 12px;
  padding: 4px 0;
  font-size: 14px;
  line-height: 1.5;
}
.od__field--addr { align-items: flex-start; }
.od__field-label {
  color: var(--wz-text-muted);
  min-width: 64px;
  flex-shrink: 0;
  font-size: 13px;
}
.od__field-value {
  color: var(--wz-text-soft);
  word-break: break-all;
}

/* Items */
.od__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--wz-border-light);
}
.od__item:last-child { border-bottom: none; }
.od__item-img {
  width: 52px;
  height: 52px;
  border-radius: var(--wz-radius-sm);
  object-fit: cover;
  flex-shrink: 0;
  background: var(--wz-bg);
}
.od__item-img--empty { background: var(--wz-bg); }
.od__item-info { flex: 1; min-width: 0; }
.od__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}
.od__item-spec {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin: 0;
}
.od__item-right {
  text-align: right;
  flex-shrink: 0;
}
.od__item-price {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  display: block;
}
.od__item-qty {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* Totals */
.od__total-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 10px;
}
.od__total-amount strong {
  font-size: 17px;
  color: var(--wz-orange);
  margin-left: 4px;
}
.od__meta-row {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin-top: 4px;
}
.od__copy {
  display: inline-block;
  margin-left: 6px;
  padding: 0 6px;
  font-size: 11px;
  color: var(--wz-orange);
  border: 1px solid var(--wz-orange);
  border-radius: 4px;
  cursor: pointer;
  line-height: 17px;
  vertical-align: middle;
}
.od__copy:hover {
  background: var(--wz-orange);
  color: #fff;
}

/* Logistics compact */
.od__logi-row {
  font-size: 14px;
  color: var(--wz-text-soft);
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
.od__logi-divider { color: var(--wz-text-muted); }
.od__logi-tracks {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--wz-border-light);
}
.od__logi-track {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 0;
}
.od__logi-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}
.od__logi-text {
  font-size: 12px;
  color: var(--wz-text-muted);
  line-height: 1.3;
}
.od__logi-more {
  font-size: 12px;
  color: var(--wz-orange);
  cursor: pointer;
  margin-top: 6px;
}

/* Actions */
.od__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.od__btn {
  flex: 1;
  min-width: 100px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 22px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--wz-duration-fast) var(--wz-ease-out);
  border: none;
  font-family: inherit;
}
.od__btn--primary {
  background: var(--wz-orange);
  color: #fff;
}
.od__btn--primary:hover { background: var(--wz-orange-dark); }
.od__btn--ghost {
  background: transparent;
  color: var(--wz-text-soft);
  border: 1px solid var(--wz-border);
}
.od__btn--ghost:hover {
  border-color: var(--wz-text-muted);
  color: var(--wz-text);
}
.od__btn--danger {
  border-color: var(--wz-danger);
  color: var(--wz-danger);
}
.od__btn--danger:hover {
  background: rgba(255,69,58,0.08);
}

@media (max-width: 640px) {
  .od { padding: 16px 16px 60px; }
  .od__step:after {
    content: '';
    position: absolute;
    right: -50%;
    top: 12px;
    width: 100%;
    height: 1px;
    background: var(--wz-border);
    z-index: 0;
  }
  .od__step:last-child:after { display: none; }
}
</style>