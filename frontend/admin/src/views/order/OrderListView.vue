<template>
  <div class="order-list-view">
    <el-card shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 140px">
            <el-option label="全部" value="" />
            <el-option label="待付款" value="待付款" />
            <el-option label="待发货" value="待发货" />
            <el-option label="待收货" value="待收货" />
            <el-option label="已完成" value="已完成" />
            <el-option label="已取消" value="已取消" />
            <el-option label="退款中" value="退款中" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table
        :data="orders"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="userId" label="用户ID" width="80" align="center" />
        <el-table-column prop="receiverName" label="收货人" width="100" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">
            <span>¥{{ row.totalAmount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="paymentMethod" label="支付方式" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" style="margin-top: 20px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchOrders"
          @current-change="fetchOrders"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="800px"
      :close-on-click-modal="false"
    >
      <template v-if="orderDetail">
        <el-descriptions title="订单信息" :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="订单号">{{ orderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(orderDetail.status)" size="small">{{ orderDetail.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ orderDetail.createTime }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="收货人信息" :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="姓名">{{ orderDetail.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ orderDetail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ orderDetail.receiverAddress }}</el-descriptions-item>
        </el-descriptions>

        <div style="margin-bottom: 20px">
          <h4 style="margin-bottom: 8px">商品信息</h4>
          <el-table :data="orderDetail.items || []" border size="small">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div style="display: flex; align-items: center; gap: 8px">
                  <el-image
                    :src="row.productImage || ''"
                    style="width: 40px; height: 40px; flex-shrink: 0"
                    fit="cover"
                  >
                    <template #error>
                      <div style="width: 40px; height: 40px; background: #f5f5f5; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #999">无图</div>
                    </template>
                  </el-image>
                  <span>{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="price" label="单价" width="80" align="center">
              <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="60" align="center" />
            <el-table-column label="小计" width="90" align="center">
              <template #default="{ row }">¥{{ row.subtotal?.toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <el-descriptions title="支付信息" :column="2" border style="margin-bottom: 20px">
          <el-descriptions-item label="支付方式">{{ orderDetail.paymentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ orderDetail.paymentStatus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单金额">
            <span style="color: #f56c6c; font-weight: bold; font-size: 16px">
              ¥{{ orderDetail.totalAmount?.toFixed(2) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span style="color: #f56c6c; font-weight: bold; font-size: 16px">
              ¥{{ orderDetail.payAmount?.toFixed(2) }}
            </span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="actionButtons.length > 0" style="text-align: center; padding-top: 8px; border-top: 1px solid #ebeef5">
          <el-button
            v-for="(btn, index) in actionButtons"
            :key="index"
            :type="btn.type"
            :loading="btn.loading"
            @click="btn.handler"
          >
            {{ btn.label }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog
      v-model="shipDialogVisible"
      title="发货"
      width="400px"
      :close-on-click-modal="false"
    >
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="80px">
        <el-form-item label="快递公司" prop="courierCompany">
          <el-input v-model="shipForm.courierCompany" placeholder="请输入快递公司" />
        </el-form-item>
        <el-form-item label="快递单号" prop="trackingNo">
          <el-input v-model="shipForm.trackingNo" placeholder="请输入快递单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipLoading" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const orders = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const orderDetail = ref(null)
const shipDialogVisible = ref(false)
const shipLoading = ref(false)
const shipFormRef = ref(null)
const shippingOrderId = ref(null)

const searchForm = reactive({
  status: ''
})
const dateRange = ref(null)

const shipForm = reactive({
  courierCompany: '',
  trackingNo: ''
})

const shipRules = {
  courierCompany: [{ required: true, message: '请输入快递公司', trigger: 'blur' }],
  trackingNo: [{ required: true, message: '请输入快递单号', trigger: 'blur' }]
}

const getOrders = (params) => request.get('/order/list', { params })
const getOrderDetail = (id) => request.get(`/order/detail/${id}`)
const updateOrderStatus = (id, status) => request.put(`/order/admin/status/${id}`, { status })

function getStatusType(status) {
  const map = {
    '待付款': 'warning',
    '待发货': 'primary',
    '待收货': 'info',
    '已完成': 'success',
    '已取消': 'info',
    '退款中': 'danger',
    '退款完成': 'success'
  }
  return map[status] || 'info'
}

const actionButtons = computed(() => {
  if (!orderDetail.value) return []
  const status = orderDetail.value.status
  const id = orderDetail.value.id
  const buttons = []

  if (status === '待付款') {
    buttons.push({
      label: '标记已付款',
      type: 'success',
      loading: false,
      handler: () => handleUpdateStatus(id, '待发货')
    })
  }

  if (status === '待发货') {
    buttons.push({
      label: '发货',
      type: 'primary',
      loading: false,
      handler: () => openShipDialog(id)
    })
  }

  if (status === '待收货') {
    buttons.push({
      label: '标记已收货',
      type: 'success',
      loading: false,
      handler: () => handleUpdateStatus(id, '已完成')
    })
  }

  if (status === '退款中') {
    buttons.push({
      label: '同意退款',
      type: 'danger',
      loading: false,
      handler: () => handleRefund(id, '同意')
    })
    buttons.push({
      label: '拒绝退款',
      type: 'info',
      loading: false,
      handler: () => handleRefund(id, '拒绝')
    })
  }

  return buttons
})

async function fetchOrders() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.status) params.status = searchForm.status
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }

    const res = await getOrders(params)
    if (res.code === 200) {
      orders.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取订单列表失败')
    }
  } catch {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchOrders()
}

function handleReset() {
  searchForm.status = ''
  dateRange.value = null
  currentPage.value = 1
  fetchOrders()
}

async function handleViewDetail(row) {
  detailVisible.value = true
  orderDetail.value = null
  try {
    const res = await getOrderDetail(row.id)
    if (res.code === 200) {
      orderDetail.value = res.data
    } else {
      ElMessage.error(res.message || '获取订单详情失败')
    }
  } catch {
    ElMessage.error('获取订单详情失败')
  }
}

async function handleUpdateStatus(id, newStatus) {
  try {
    const res = await updateOrderStatus(id, newStatus)
    if (res.code === 200) {
      ElMessage.success('操作成功')
      detailVisible.value = false
      fetchOrders()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

function openShipDialog(id) {
  shipForm.courierCompany = ''
  shipForm.trackingNo = ''
  shippingOrderId.value = id
  shipDialogVisible.value = true
}

async function handleShip() {
  const valid = await shipFormRef.value.validate().catch(() => false)
  if (!valid) return

  shipLoading.value = true
  try {
    const res = await request.put(`/order/admin/ship/${shippingOrderId.value}`, {
      courierCompany: shipForm.courierCompany,
      trackingNo: shipForm.trackingNo
    })
    if (res.code === 200) {
      ElMessage.success('发货成功')
      shipDialogVisible.value = false
      detailVisible.value = false
      fetchOrders()
    } else {
      ElMessage.error(res.message || '发货失败')
    }
  } catch {
    ElMessage.error('发货失败')
  } finally {
    shipLoading.value = false
  }
}

async function handleRefund(id, action) {
  const confirmText = action === '同意' ? '确认同意退款？' : '确认拒绝退款？'
  try {
    await ElMessageBox.confirm(confirmText, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const status = action === '同意' ? '退款完成' : '已取消'
    const res = await updateOrderStatus(id, status)
    if (res.code === 200) {
      ElMessage.success(`${action}退款成功`)
      detailVisible.value = false
      fetchOrders()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchOrders()
})
</script>
