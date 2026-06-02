<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, cancelOrder, confirmOrder, payOrder, applyRefund } from '@/api/order'
import type { Order } from '@/api/order'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const order = ref<Order | null>(null)
const loading = ref(true)
const refundDialogVisible = ref(false)
const refundReason = ref('')
const refundSubmitting = ref(false)

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

async function handleRefund() {
  if (!order.value) return
  refundReason.value = ''
  refundDialogVisible.value = true
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
    // 退款中：显示到"已发货"，最后一步显示"退款处理中"
    if (stepIndex === 0) return 'completed'
    if (stepIndex === 1) return 'completed'
    if (stepIndex === 2) return 'completed'
    if (stepIndex === 3) return 'active'
    return ''
  }
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
            <div v-if="order.status !== 5 || i <= 3"
              class="order-detail__step"
              :class="[`order-detail__step--${stepStatus(i)}`]"
            >
              <div class="order-detail__step-dot"></div>
              <div class="order-detail__step-info">
                <p class="order-detail__step-label">{{ order.status === 5 && i === 3 ? '退款处理中' : step.label }}</p>
                <p class="order-detail__step-time">{{ step.timeField && (order as any)[step.timeField] || '——' }}</p>
              </div>
            </div>
            <div v-if="i < stepConfig.length - 1 && (order.status !== 5 || i < 3)" class="order-detail__step-connector"></div>
          </template>
        </div>
        <!-- Refund info banner -->
        <div v-if="order.status === 5" class="order-detail__refund-banner">
          <span class="order-detail__refund-dot"></span>
          <span>退款申请已提交，等待商家处理</span>
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

      <!-- Logistics Tracking -->
      <section v-if="order.logisticsCompany" class="order-detail__section">
        <h2 class="order-detail__section-title">物流信息</h2>
        <div class="order-detail__card">
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">物流公司</span>
            <span class="order-detail__card-value">{{ order.logisticsCompany }}</span>
          </div>
          <div class="order-detail__card-row">
            <span class="order-detail__card-label">运单编号</span>
            <span class="order-detail__card-value">
              {{ order.logisticsNo }}
              <span
                class="order-detail__copy-btn"
                @click="copyLogisticsNo"
              >复制</span>
            </span>
          </div>
          <div class="order-detail__card-row" v-if="order.deliveryTime">
            <span class="order-detail__card-label">发货时间</span>
            <span class="order-detail__card-value">{{ order.deliveryTime }}</span>
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
        <div class="order-detail__actions">
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
          <span
            v-if="order.status === 3"
            class="order-detail__action order-detail__action--primary"
            @click="goToComment"
          >去评价</span>
          <span
            v-if="order.status === 1 || order.status === 2 || order.status === 3"
            class="order-detail__action order-detail__action--danger"
            @click="handleRefund"
          >申请退款</span>
          <span
            v-if="order.logisticsCompany"
            class="order-detail__action order-detail__action--secondary"
          >查看物流</span>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="order-detail order-detail--loading">加载中...</div>
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

.order-detail__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-bottom: 16px;
  cursor: pointer;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}
.order-detail__breadcrumb:hover {
  color: var(--wz-text-soft);
}

.order-detail--loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--wz-text-muted);
}

.order-detail__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--wz-text);
  letter-spacing: 0.02em;
  margin-bottom: 32px;
}

.order-detail__section {
  margin-bottom: 28px;
}

.order-detail__section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--wz-text-soft);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 12px;
  padding-left: 2px;
}

/* ── Steps ── */
.order-detail__steps {
  display: flex;
  align-items: flex-start;
  padding: 28px 24px;
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
  transition: background var(--wz-duration-normal) var(--wz-ease-out),
              box-shadow var(--wz-duration-normal) var(--wz-ease-out);
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

.order-detail__step-connector {
  width: 48px;
  height: 1px;
  background: var(--wz-border);
  margin: 10px 8px 0;
  flex-shrink: 0;
}

.order-detail__step--completed + .order-detail__step-connector {
  background: var(--wz-orange-muted);
  height: 2px;
}

/* ── Refund banner ── */
.order-detail__refund-banner {
  margin-top: 12px;
  padding: 10px 16px;
  background: rgba(255, 159, 10, 0.08);
  border: 1px solid rgba(255, 159, 10, 0.2);
  border-radius: var(--wz-radius-sm);
  font-size: 13px;
  color: var(--wz-warning);
  display: flex;
  align-items: center;
  gap: 8px;
}
.order-detail__refund-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--wz-warning);
  flex-shrink: 0;
  animation: refund-pulse 2s ease-in-out infinite;
}
@keyframes refund-pulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* ── Cards (dark surface) ── */
.order-detail__card {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 20px;
  transition: border-color var(--wz-duration-normal) var(--wz-ease-out);
}

.order-detail__card-row {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 1.5;
}
.order-detail__card-row:last-child {
  margin-bottom: 0;
}

.order-detail__card-label {
  color: var(--wz-text-muted);
  min-width: 68px;
  flex-shrink: 0;
  font-size: 13px;
}

.order-detail__card-value {
  color: var(--wz-text-soft);
  word-break: break-all;
}

.order-detail__copy-btn {
  display: inline-block;
  margin-left: 6px;
  padding: 0 7px;
  font-size: 11px;
  color: var(--wz-orange);
  border: 1px solid var(--wz-orange);
  border-radius: 4px;
  cursor: pointer;
  line-height: 18px;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
  vertical-align: middle;
}
.order-detail__copy-btn:hover {
  background: var(--wz-orange);
  color: #fff;
}

/* ── Order items ── */
.order-detail__item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid var(--wz-border-light);
}
.order-detail__item:last-child {
  border-bottom: none;
}

.order-detail__item-image {
  width: 60px;
  height: 60px;
  border-radius: var(--wz-radius-sm);
  flex-shrink: 0;
  object-fit: cover;
  background: var(--wz-bg);
}

.order-detail__item-info {
  flex: 1;
  min-width: 0;
}

.order-detail__item-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.order-detail__item-spec {
  font-size: 12px;
  color: var(--wz-text-muted);
}

.order-detail__item-price {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  white-space: nowrap;
}

.order-detail__item-qty {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-left: 4px;
}

/* ── Summary ── */
.order-detail__summary {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-md);
  padding: 20px;
}

.order-detail__summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 8px;
  line-height: 1.5;
}

.order-detail__summary-row--total {
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-text);
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--wz-border);
}

.order-detail__summary-row--total span:last-child {
  color: var(--wz-orange);
}

/* ── Actions ── */
.order-detail__actions {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.order-detail__action {
  flex: 1;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
  user-select: none;
}

.order-detail__action--primary {
  background: var(--wz-orange);
  color: #fff;
  border: none;
}
.order-detail__action--primary:hover {
  background: var(--wz-orange-dark);
}

.order-detail__action--secondary {
  background: transparent;
  color: var(--wz-text-soft);
  border: 1px solid var(--wz-border);
}
.order-detail__action--secondary:hover {
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

@media (max-width: 640px) {
  .order-detail {
    padding: 20px 16px 80px;
  }
  .order-detail__title {
    font-size: 22px;
    margin-bottom: 24px;
  }
  .order-detail__steps {
    padding: 20px 16px;
  }
  .order-detail__action {
    font-size: 12px;
    height: 36px;
  }
  .order-detail__item-image {
    width: 48px;
    height: 48px;
  }
}

/* ── Refund Dialog ── */
.refund-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.refund-dialog :deep(.el-dialog__header) {
  padding: 20px 24px 0;
  margin: 0;
}
.refund-dialog :deep(.el-dialog__headerbtn) {
  top: 20px;
  right: 20px;
}
.refund-dialog :deep(.el-dialog__title) {
  font-size: 17px;
  font-weight: 600;
  color: var(--wz-text);
}
.refund-dialog__body {
  padding: 20px 24px;
}
.refund-dialog__amount {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--wz-orange-muted);
  border-radius: 10px;
  margin-bottom: 20px;
}
.refund-dialog__amount-label {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.refund-dialog__amount-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--wz-orange);
  letter-spacing: -0.3px;
}
.refund-dialog__section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--wz-text);
  margin: 0 0 10px;
}
.refund-dialog__presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}
.refund-dialog__preset {
  padding: 6px 14px;
  font-size: 12px;
  color: var(--wz-text-soft);
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s var(--wz-ease-out);
  user-select: none;
}
.refund-dialog__preset:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.refund-dialog__preset--active {
  background: var(--wz-orange);
  border-color: var(--wz-orange);
  color: #fff;
}
.refund-dialog__preset--active:hover {
  background: var(--wz-orange-dark);
  border-color: var(--wz-orange-dark);
  color: #fff;
}
.refund-dialog__textarea-wrap {
  position: relative;
}
.refund-dialog__textarea {
  width: 100%;
  padding: 10px 14px;
  font-size: 13px;
  font-family: inherit;
  color: var(--wz-text);
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  outline: none;
  resize: none;
  line-height: 1.6;
  transition: border-color 0.2s var(--wz-ease-out);
  box-sizing: border-box;
}
.refund-dialog__textarea:focus {
  border-color: var(--wz-orange);
}
.refund-dialog__textarea::placeholder {
  color: var(--wz-text-muted);
}
.refund-dialog__textarea-count {
  position: absolute;
  right: 10px;
  bottom: 8px;
  font-size: 11px;
  color: var(--wz-text-muted);
}
.refund-dialog__footer {
  display: flex;
  gap: 10px;
  padding: 0 24px 20px;
}
.refund-dialog__btn {
  flex: 1;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 21px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s var(--wz-ease-out);
  border: none;
  font-family: inherit;
}
.refund-dialog__btn--cancel {
  background: var(--wz-bg);
  color: var(--wz-text-soft);
  border: 1px solid var(--wz-border);
}
.refund-dialog__btn--cancel:hover {
  border-color: var(--wz-text-muted);
  color: var(--wz-text);
}
.refund-dialog__btn--submit {
  background: var(--wz-orange);
  color: #fff;
}
.refund-dialog__btn--submit:hover:not(:disabled) {
  background: var(--wz-orange-dark);
}
.refund-dialog__btn--submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
