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

      <!-- ── Header ── -->
      <div class="od-header">
        <div class="od-header__breadcrumb" @click="router.push('/order/list')">我的订单</div>
        <div class="od-header__row">
          <h1 class="od-header__title">订单详情</h1>
          <span class="od-header__badge" :class="`od-header__badge--${order.status}`">
            {{ statusLabels[order.status] || '未知' }}
          </span>
        </div>
        <div class="od-header__meta">
          <span>{{ order.orderNo }}</span>
          <span class="od-header__dot">·</span>
          <span>{{ formatTime(order.createdTime) }}</span>
          <span class="od-header__dot">·</span>
          <span>实付 <strong>¥{{ order.totalAmount }}</strong></span>
        </div>
      </div>

      <!-- ── Progress ── -->
      <section v-if="order.status !== 4" class="od-progress">
        <div class="od-progress__steps">
          <template v-for="(step, i) in stepConfig" :key="step.key">
            <div
              v-if="order.status !== 5 || (i <= 3 && (i !== 2 || order.deliveryTime))"
              class="od-progress__step"
              :class="[`od-progress__step--${stepStatus(i)}`]"
            >
              <div class="od-progress__dot"></div>
              <div class="od-progress__info">
                <p class="od-progress__label">{{ order.status === 5 && i === 3 ? '退款处理中' : step.label }}</p>
                <p class="od-progress__time" v-if="stepStatus(i)">{{ step.timeField && (order as any)[step.timeField] || '——' }}</p>
              </div>
            </div>
            <div v-if="i < stepConfig.length - 1 && (order.status !== 5 || (order.deliveryTime ? i < 3 : i < 1))" class="od-progress__connector"></div>
          </template>
        </div>
        <div v-if="order.status === 5" class="od-progress__refund od-progress__refund--pending">
          <span class="od-progress__pulse"></span>
          <span>退款申请已提交，等待商家处理</span>
        </div>
      </section>

      <!-- Refund result banners -->
      <div v-if="refundInfo?.status === 'REJECTED'" class="od-progress__refund od-progress__refund--rejected">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        <div>
          <p class="od-progress__refund-title">退款申请已被拒绝</p>
          <p v-if="refundInfo.adminReply" class="od-progress__refund-reply">{{ refundInfo.adminReply }}</p>
        </div>
      </div>
      <div v-if="refundInfo?.status === 'APPROVED'" class="od-progress__refund od-progress__refund--approved">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
        <span>退款已完成</span>
      </div>

      <!-- ── Content (single card) ── -->
      <div class="od-content">

        <!-- Receiver -->
        <div class="od-content__block">
          <div class="od-content__label">收货信息</div>
          <div class="od-content__grid">
            <div class="od-content__cell">
              <span class="od-content__hint">收货人</span>
              <span>{{ order.receiverName }}</span>
            </div>
            <div class="od-content__cell">
              <span class="od-content__hint">联系电话</span>
              <span>{{ order.receiverPhone }}</span>
            </div>
            <div class="od-content__cell od-content__cell--wide">
              <span class="od-content__hint">收货地址</span>
              <span>{{ order.receiverAddress }}</span>
            </div>
          </div>
        </div>

        <div class="od-content__divider"></div>

        <!-- Logistics -->
        <div v-if="order.logisticsCompany" class="od-content__block">
          <div class="od-content__label">物流信息</div>
          <div class="od-content__grid">
            <div class="od-content__cell">
              <span class="od-content__hint">物流公司</span>
              <span>{{ order.logisticsCompany }}</span>
            </div>
            <div class="od-content__cell">
              <span class="od-content__hint">运单编号</span>
              <span>
                {{ order.logisticsNo }}
                <span class="od-content__copy" @click="copyLogisticsNo">复制</span>
              </span>
            </div>
            <div class="od-content__cell" v-if="order.deliveryTime">
              <span class="od-content__hint">发货时间</span>
              <span>{{ formatTime(order.deliveryTime) }}</span>
            </div>
          </div>
        </div>

        <div v-if="order.logisticsCompany && logisticsTracks.length" class="od-content__divider"></div>

        <!-- Logistics Timeline (compact) -->
        <div v-if="logisticsTracks.length" class="od-content__block">
          <div class="od-content__label">物流跟踪</div>
          <div class="od-timeline">
            <div
              v-for="(track, i) in logisticsTracks.slice(0, 4)"
              :key="track.id"
              class="od-timeline__item"
            >
              <div class="od-timeline__dot"
                :style="{ background: i === 0 ? logisticsStatusMap[track.status]?.color : 'var(--wz-text-muted)' }">
              </div>
              <div v-if="i < Math.min(logisticsTracks.length, 4) - 1" class="od-timeline__line"></div>
              <div class="od-timeline__content">
                <p class="od-timeline__status"
                  :style="{ color: i === 0 ? logisticsStatusMap[track.status]?.color : '' }">
                  {{ logisticsStatusMap[track.status]?.label || track.status }}
                </p>
                <p class="od-timeline__msg">{{ track.message || '' }}</p>
                <p class="od-timeline__time">{{ formatTime(track.trackTime) }}</p>
              </div>
            </div>
            <div v-if="logisticsTracks.length > 4" class="od-timeline__more">
              <span @click="$router.push('/logistics/' + order?.id)">查看完整物流</span>
            </div>
          </div>
        </div>

        <div class="od-content__divider"></div>

        <!-- Items -->
        <div class="od-content__block">
          <div class="od-content__label">商品清单</div>
          <div
            v-for="item in (order.items || [])"
            :key="item.id"
            class="od-item"
          >
            <img v-if="item.productImage" :src="item.productImage" class="od-item__img">
            <div v-else class="od-item__img od-item__img--empty"></div>
            <div class="od-item__info">
              <p class="od-item__name">{{ item.productName }}</p>
              <p v-if="item.specInfo" class="od-item__spec">{{ item.specInfo }}</p>
            </div>
            <div class="od-item__pricing">
              <span class="od-item__price">¥{{ item.price }}</span>
              <span class="od-item__qty">x{{ item.quantity }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Footer ── -->
      <div class="od-footer">
        <div class="od-footer__total">
          <span>共 {{ order.items?.length || 0 }} 件商品</span>
          <span class="od-footer__amount">实付 <strong>¥{{ order.totalAmount }}</strong></span>
        </div>
        <div class="od-footer__actions">
          <button
            v-if="order.status === 0"
            class="od-btn od-btn--primary"
            @click="handlePay"
          >去支付</button>
          <button
            v-if="order.status === 0"
            class="od-btn od-btn--ghost"
            @click="handleCancel"
          >取消订单</button>
          <button
            v-if="order.status === 2"
            class="od-btn od-btn--primary"
            @click="handleConfirm"
          >确认收货</button>
          <button
            v-if="order.status === 3"
            class="od-btn od-btn--primary"
            @click="goToComment"
          >去评价</button>
          <button
            v-if="order.status === 1 || order.status === 2 || order.status === 3"
            class="od-btn od-btn--danger"
            @click="handleRefund"
          >申请退款</button>
          <button
            v-if="order.status === 5"
            class="od-btn od-btn--ghost"
            @click="handleCancelRefund"
          >取消退款</button>
          <button
            v-if="order.logisticsCompany"
            class="od-btn od-btn--ghost"
            @click="$router.push('/logistics/' + order?.id)"
          >查看物流</button>
        </div>
      </div>
    </div>

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
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.order-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 28px 24px 80px;
}
.od-loading {
  max-width: 800px;
  margin: 0 auto;
  padding: 80px 24px;
  text-align: center;
  color: var(--wz-text-muted);
}
.od-header { margin-bottom: 28px; }
.od-header__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-bottom: 12px;
  cursor: pointer;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}
.od-header__breadcrumb:hover { color: var(--wz-text-soft); }
.od-header__row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}
.od-header__title {
  font-family: var(--wz-font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--wz-text);
  letter-spacing: 0.02em;
  margin: 0;
}
.od-header__badge {
  display: inline-block;
  padding: 3px 12px;
  font-size: 12px;
  font-weight: 600;
  border-radius: 999px;
  line-height: 1.4;
}
.od-header__badge--0 { background: var(--wz-orange-muted); color: var(--wz-orange); }
.od-header__badge--1 { background: rgba(64,158,255,0.12); color: #409eff; }
.od-header__badge--2 { background: var(--wz-bg-elevated); color: var(--wz-text-soft); }
.od-header__badge--3 { background: rgba(52,199,89,0.12); color: var(--wz-success); }
.od-header__badge--4 { background: var(--wz-bg-elevated); color: var(--wz-text-muted); }
.od-header__badge--5 { background: rgba(255,159,10,0.12); color: var(--wz-warning); }
.od-header__meta {
  font-size: 13px;
  color: var(--wz-text-muted);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.od-header__meta strong { color: var(--wz-orange); font-weight: 600; }
.od-header__dot { color: var(--wz-border); }

.od-progress {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 24px 20px;
  margin-bottom: 20px;
}
.od-progress__steps { display: flex; align-items: flex-start; overflow-x: auto; }
.od-progress__step { display: flex; align-items: flex-start; gap: 10px; flex-shrink: 0; }
.od-progress__dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--wz-text-muted);
  margin-top: 5px; flex-shrink: 0;
  transition: background var(--wz-duration-normal) var(--wz-ease-out), box-shadow var(--wz-duration-normal) var(--wz-ease-out);
}
.od-progress__step--completed .od-progress__dot {
  background: var(--wz-orange);
  box-shadow: 0 0 0 3px var(--wz-orange-muted);
}
.od-progress__step--active .od-progress__dot {
  background: var(--wz-orange);
  box-shadow: 0 0 0 5px var(--wz-orange-muted);
}
.od-progress__label {
  font-size: 13px; font-weight: 500;
  color: var(--wz-text);
  line-height: 1.3; white-space: nowrap; margin: 0;
}
.od-progress__step--active .od-progress__label { color: var(--wz-orange); }
.od-progress__time { font-size: 11px; color: var(--wz-text-muted); margin: 0; }
.od-progress__connector {
  width: 48px; height: 1px;
  background: var(--wz-border);
  margin: 10px 8px 0;
  flex-shrink: 0;
}
.od-progress__step--completed + .od-progress__connector {
  background: var(--wz-orange-muted); height: 2px;
}
.od-progress__refund {
  margin-top: 14px; padding: 10px 14px;
  border-radius: var(--wz-radius-sm);
  font-size: 13px;
  display: flex; align-items: flex-start; gap: 8px;
  line-height: 1.4;
}
.od-progress__refund--pending {
  background: rgba(255,159,10,0.08);
  border: 1px solid rgba(255,159,10,0.2);
  color: var(--wz-warning);
}
.od-progress__refund--rejected {
  background: rgba(255,69,58,0.08);
  border: 1px solid rgba(255,69,58,0.2);
  color: var(--wz-danger);
  margin-bottom: 20px;
}
.od-progress__refund--approved {
  background: rgba(52,199,89,0.08);
  border: 1px solid rgba(52,199,89,0.2);
  color: var(--wz-success);
  margin-bottom: 20px;
}
.od-progress__refund-title { font-weight: 500; margin: 0; }
.od-progress__refund-reply { font-size: 12px; opacity: 0.8; margin: 2px 0 0; }
.od-progress__pulse {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--wz-warning);
  flex-shrink: 0;
  animation: od-pulse 2s ease-in-out infinite;
}
@keyframes od-pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.od-content {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  margin-bottom: 20px;
  overflow: hidden;
}
.od-content__block { padding: 20px; }
.od-content__label {
  font-size: 13px; font-weight: 600;
  color: var(--wz-text-soft);
  margin-bottom: 14px;
  letter-spacing: 0.04em;
}
.od-content__grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.od-content__cell {
  font-size: 14px; color: var(--wz-text);
  line-height: 1.5;
  display: flex; flex-direction: column; gap: 2px;
}
.od-content__cell--wide { grid-column: 1 / -1; }
.od-content__hint { font-size: 12px; color: var(--wz-text-muted); }
.od-content__divider {
  height: 1px; background: var(--wz-border-light);
  margin: 0 20px;
}
.od-content__copy {
  display: inline-block; margin-left: 6px;
  padding: 0 7px; font-size: 11px;
  color: var(--wz-orange);
  border: 1px solid var(--wz-orange); border-radius: 4px;
  cursor: pointer; line-height: 18px; vertical-align: middle;
  transition: background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out);
}
.od-content__copy:hover { background: var(--wz-orange); color: #fff; }

.od-timeline { position: relative; }
.od-timeline__item {
  display: flex; align-items: flex-start; gap: 12px;
  position: relative; padding-bottom: 16px;
}
.od-timeline__item:last-child { padding-bottom: 0; }
.od-timeline__dot {
  width: 10px; height: 10px; border-radius: 50%;
  flex-shrink: 0; margin-top: 4px;
  position: relative; z-index: 1;
}
.od-timeline__line {
  position: absolute; left: 4px; top: 16px;
  width: 2px; bottom: 0; background: var(--wz-border);
}
.od-timeline__content { flex: 1; min-width: 0; }
.od-timeline__status {
  font-size: 14px; font-weight: 500;
  margin: 0; color: var(--wz-text-soft);
}
.od-timeline__msg { font-size: 12px; color: var(--wz-text-muted); margin: 2px 0; }
.od-timeline__time { font-size: 11px; color: var(--wz-text-muted); margin: 0; }
.od-timeline__more { text-align: center; padding-top: 10px; border-top: 1px solid var(--wz-border-light); margin-top: 10px; }
.od-timeline__more span { font-size: 13px; color: var(--wz-orange); cursor: pointer; }
.od-timeline__more span:hover { color: var(--wz-orange-dark); }

.od-item {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid var(--wz-border-light);
}
.od-item:last-child { border-bottom: none; padding-bottom: 0; }
.od-item:first-child { padding-top: 0; }
.od-item__img {
  width: 60px; height: 60px;
  border-radius: var(--wz-radius-sm);
  flex-shrink: 0; object-fit: cover;
  background: var(--wz-bg);
}
.od-item__info { flex: 1; min-width: 0; }
.od-item__name {
  font-size: 14px; font-weight: 500;
  color: var(--wz-text);
  margin: 0 0 2px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.od-item__spec { font-size: 12px; color: var(--wz-text-muted); margin: 0; }
.od-item__pricing {
  display: flex; flex-direction: column;
  align-items: flex-end; gap: 2px; white-space: nowrap;
}
.od-item__price { font-size: 14px; font-weight: 500; color: var(--wz-text); }
.od-item__qty { font-size: 13px; color: var(--wz-text-muted); }

.od-footer {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 20px;
}
.od-footer__total {
  display: flex; justify-content: space-between;
  align-items: center;
  font-size: 13px; color: var(--wz-text-soft);
  margin-bottom: 16px;
}
.od-footer__amount { font-size: 16px; }
.od-footer__amount strong { color: var(--wz-orange); font-weight: 600; }
.od-footer__actions { display: flex; gap: 10px; flex-wrap: wrap; }
.od-btn {
  flex: 1; min-width: 0; height: 42px;
  display: flex; align-items: center; justify-content: center;
  border-radius: 21px;
  font-size: 13px; font-weight: 600;
  cursor: pointer; border: none;
  font-family: inherit; padding: 0 20px;
  transition: background var(--wz-duration-fast) var(--wz-ease-out), border-color var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out), box-shadow var(--wz-duration-fast) var(--wz-ease-out);
  user-select: none;
}
.od-btn--primary { background: var(--wz-orange); color: #fff; }
.od-btn--primary:hover { background: var(--wz-orange-dark); box-shadow: 0 0 24px rgba(255,107,53,0.25); }
.od-btn--ghost { background: transparent; color: var(--wz-text-soft); border: 1px solid var(--wz-border); }
.od-btn--ghost:hover { border-color: var(--wz-text-muted); color: var(--wz-text); }
.od-btn--danger { background: transparent; color: var(--wz-danger); border: 1px solid var(--wz-danger); }
.od-btn--danger:hover { background: rgba(255,69,58,0.08); }

@media (max-width: 640px) {
  .order-detail { padding: 20px 16px 80px; }
  .od-header__title { font-size: 22px; }
  .od-content__grid { grid-template-columns: 1fr; }
  .od-footer__actions { flex-direction: column; }
  .od-btn { flex: none; width: 100%; }
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

