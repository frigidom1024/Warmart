<template>
  <div class="order-detail-container" v-loading="loading">
    <h2>订单详情</h2>

    <!-- Order Progress -->
    <el-card class="section-card">
      <el-steps :active="stepActive" align-center>
        <el-step title="提交订单" />
        <el-step title="支付" />
        <el-step title="发货" />
        <el-step title="收货" />
        <el-step title="完成" />
      </el-steps>
      <div v-if="order.status === 4" class="status-info">
        <el-tag type="info">订单已取消</el-tag>
      </div>
      <div v-if="order.status === 5" class="status-info">
        <el-tag type="danger">退款处理中</el-tag>
      </div>
    </el-card>

    <!-- Shipping Address -->
    <el-card class="section-card">
      <template #header>
        <span>收货信息</span>
      </template>
      <div class="info-row">
        <span class="info-label">收货人：</span>
        <span>{{ order.receiverName }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">联系电话：</span>
        <span>{{ order.receiverPhone }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">收货地址：</span>
        <span>{{ order.receiverAddress }}</span>
      </div>
    </el-card>

    <!-- Order Items -->
    <el-card class="section-card">
      <template #header>
        <span>商品信息</span>
      </template>
      <el-table :data="order.items || []" style="width: 100%">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                :src="row.productImage"
                class="product-img"
                fit="cover"
              />
              <span>{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="center">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">
            {{ row.quantity }}
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="center">
          <template #default="{ row }">
            ¥{{ (row.price * row.quantity).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Payment Info -->
    <el-card class="section-card">
      <template #header>
        <span>支付信息</span>
      </template>
      <div class="info-row">
        <span class="info-label">支付方式：</span>
        <span>{{ paymentMethodLabel }}</span>
      </div>
      <div class="info-row">
        <span class="info-label">支付状态：</span>
        <el-tag :type="order.paymentStatus === 1 ? 'success' : 'danger'">
          {{ order.paymentStatus === 1 ? '已支付' : '未支付' }}
        </el-tag>
      </div>
      <div class="info-row" v-if="order.paymentTime">
        <span class="info-label">支付时间：</span>
        <span>{{ order.paymentTime }}</span>
      </div>
    </el-card>

    <!-- Price Breakdown -->
    <el-card class="section-card">
      <div class="total-row">
        <span>订单总金额：</span>
        <strong class="total-amount">&yen;{{ order.totalAmount }}</strong>
      </div>
    </el-card>

    <!-- Actions -->
    <div class="detail-actions">
      <el-button @click="router.push('/order/list')">返回列表</el-button>
      <el-button
        v-if="order.status === 0"
        type="primary"
        @click="handlePay"
      >
        支付
      </el-button>
      <el-button v-if="order.status === 0" @click="handleCancel">
        取消订单
      </el-button>
      <el-button
        v-if="order.status === 2"
        type="success"
        @click="handleConfirm"
      >
        确认收货
      </el-button>
      <el-button
        v-if="order.status === 3 || order.status === 5"
        @click="handleRefund"
      >
        申请退款
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  getOrderDetail,
  cancelOrder,
  confirmOrder,
  applyRefund,
  payOrder
} from '@/api/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref({})

const paymentMethods = {
  alipay: '支付宝',
  wechat: '微信支付',
  card: '银行卡'
}

const paymentMethodLabel = computed(() => {
  if (!order.value.paymentMethod) return '未选择'
  return paymentMethods[order.value.paymentMethod] || order.value.paymentMethod
})

const stepActive = computed(() => {
  const status = order.value.status
  if (status === undefined || status === null) return 0
  if (status === 4) return 0
  return Math.min(status + 1, 4)
})

onMounted(() => {
  const id = route.params.id
  if (id) {
    fetchOrder(id)
  }
})

const fetchOrder = async (id) => {
  loading.value = true
  try {
    const res = await getOrderDetail(id)
    if (res.code === 200) {
      order.value = res.data || {}
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handlePay = async () => {
  try {
    const res = await payOrder({ orderId: order.value.id })
    if (res.code === 200) {
      ElMessage.success('支付成功')
      await fetchOrder(order.value.id)
    }
  } catch {
    // Error handled by interceptor
  }
}

const handleCancel = async () => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await cancelOrder(order.value.id)
    if (res.code === 200) {
      ElMessage.success('订单已取消')
      await fetchOrder(order.value.id)
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}

const handleConfirm = async () => {
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await confirmOrder(order.value.id)
    if (res.code === 200) {
      ElMessage.success('已确认收货')
      await fetchOrder(order.value.id)
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}

const handleRefund = async () => {
  try {
    await ElMessageBox.confirm('确定要申请退款吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await applyRefund(order.value.id)
    if (res.code === 200) {
      ElMessage.success('退款申请已提交')
      await fetchOrder(order.value.id)
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}
</script>

<style scoped>
.order-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.order-detail-container h2 {
  margin-bottom: 20px;
  font-size: 20px;
}

.section-card {
  margin-bottom: 20px;
}

.info-row {
  padding: 6px 0;
  font-size: 14px;
}

.info-label {
  color: #666;
  display: inline-block;
  width: 100px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  flex-shrink: 0;
  background: #f5f5f5;
}

.total-row {
  text-align: right;
  font-size: 16px;
}

.total-amount {
  font-size: 24px;
  color: #e4393c;
}

.detail-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.status-info {
  text-align: center;
  margin-top: 16px;
}
</style>
