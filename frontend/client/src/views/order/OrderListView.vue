<template>
  <div class="order-list-container">
    <h2>我的订单</h2>

    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="待付款" name="0" />
      <el-tab-pane label="待发货" name="1" />
      <el-tab-pane label="待收货" name="2" />
      <el-tab-pane label="已完成" name="3" />
      <el-tab-pane label="已取消" name="4" />
      <el-tab-pane label="退款中" name="5" />
    </el-tabs>

    <div v-loading="loading">
      <div v-if="orders.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无订单" />
      </div>

      <el-card
        v-for="order in orders"
        :key="order.id"
        class="order-card"
        shadow="hover"
      >
        <div class="order-header">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusTagType(order.status)">
            {{ statusLabel(order.status) }}
          </el-tag>
        </div>

        <div class="order-items">
          <div
            v-for="item in order.items || []"
            :key="item.id"
            class="order-item"
          >
            <el-image
              :src="item.productImage"
              class="order-item-img"
              fit="cover"
            />
            <span class="order-item-name">{{ item.productName }}</span>
            <span class="order-item-price"
              >&yen;{{ item.price }} &times; {{ item.quantity }}</span
            >
          </div>
        </div>

        <div class="order-footer">
          <span class="order-total"
            >合计：&yen;{{ order.totalAmount }}</span
          >
          <div class="order-actions">
            <el-button
              v-if="order.status === 0"
              type="primary"
              size="small"
              @click="handlePay(order)"
            >
              支付
            </el-button>
            <el-button
              v-if="order.status === 0"
              size="small"
              @click="handleCancel(order)"
            >
              取消
            </el-button>
            <el-button
              v-if="order.status === 2"
              type="success"
              size="small"
              @click="handleConfirm(order)"
            >
              确认收货
            </el-button>
            <el-button
              v-if="order.status === 3 || order.status === 5"
              size="small"
              @click="handleRefund(order)"
            >
              申请退款
            </el-button>
            <el-button
              size="small"
              @click="router.push(`/order/detail/${order.id}`)"
            >
              详情
            </el-button>
          </div>
        </div>
      </el-card>

      <div v-if="total > 0" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import {
  getOrders,
  cancelOrder,
  confirmOrder,
  applyRefund,
  payOrder
} from '@/api/order'

const router = useRouter()
const loading = ref(false)
const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeTab = ref('all')

const statusMap = {
  0: { label: '待付款', type: 'warning' },
  1: { label: '待发货', type: 'primary' },
  2: { label: '待收货', type: 'info' },
  3: { label: '已完成', type: 'success' },
  4: { label: '已取消', type: 'info' },
  5: { label: '退款中', type: 'danger' }
}

const statusLabel = (status) => statusMap[status]?.label || '未知'
const statusTagType = (status) => statusMap[status]?.type || ''

const getStatusParam = () => {
  return activeTab.value === 'all' ? undefined : parseInt(activeTab.value)
}

onMounted(() => {
  fetchOrders()
})

const fetchOrders = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, pageSize: pageSize.value }
    const status = getStatusParam()
    if (status !== undefined) {
      params.status = status
    }
    const res = await getOrders(params)
    if (res.code === 200) {
      orders.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleTabClick = async () => {
  currentPage.value = 1
  await fetchOrders()
}

const handlePageChange = async (page) => {
  currentPage.value = page
  await fetchOrders()
}

const handlePay = async (order) => {
  try {
    const res = await payOrder({ orderId: order.id })
    if (res.code === 200) {
      ElMessage.success('支付成功')
      await fetchOrders()
    }
  } catch {
    // Error handled by interceptor
  }
}

const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await cancelOrder(order.id)
    if (res.code === 200) {
      ElMessage.success('订单已取消')
      await fetchOrders()
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}

const handleConfirm = async (order) => {
  try {
    await ElMessageBox.confirm('确认已收到商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await confirmOrder(order.id)
    if (res.code === 200) {
      ElMessage.success('已确认收货')
      await fetchOrders()
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}

const handleRefund = async (order) => {
  try {
    await ElMessageBox.confirm('确定要申请退款吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await applyRefund(order.id)
    if (res.code === 200) {
      ElMessage.success('退款申请已提交')
      await fetchOrders()
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}
</script>

<style scoped>
.order-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.order-list-container h2 {
  margin-bottom: 20px;
  font-size: 20px;
}

.order-card {
  margin-bottom: 16px;
}

.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.order-no {
  font-size: 14px;
  color: #666;
}

.order-items {
  margin-bottom: 12px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.order-item + .order-item {
  border-top: 1px solid #f5f5f5;
}

.order-item-img {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  flex-shrink: 0;
  background: #f5f5f5;
}

.order-item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-item-price {
  color: #999;
  white-space: nowrap;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.order-total {
  font-size: 14px;
  font-weight: bold;
  color: #e4393c;
}

.order-actions {
  display: flex;
  gap: 8px;
}

.empty-state {
  padding: 60px 0;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
