<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTable } from 'element-plus'
import { Van } from '@element-plus/icons-vue'
import { getAdminOrderList, updateOrderStatus, shipOrder } from '@/api/order'
import type { Order } from '@/api/order'
import { addLogisticsTrack } from '@/api/logistics'
import { regionData, codeToText } from 'element-china-area-data'

const router = useRouter()
const route = useRoute()

const tableRef = ref<InstanceType<typeof ElTable>>()
const orders = ref<Order[]>([])
const total = ref(0)
const loading = ref(false)

// Search state (synced with URL query params)
const query = ref({
  status: undefined as number | undefined,
  orderNo: '',
  receiverName: '',
  receiverPhone: '',
  startTime: '',
  endTime: '',
  page: 1,
  size: 10
})
const dateRange = ref<[string, string] | null>(null)
const shipDialogVisible = ref(false)
const shipTarget = ref<Order | null>(null)
const shipForm = ref({ logisticsCompany: '', logisticsNo: '' })
const shipTrack = ref({ status: 'WAREHOUSE', message: '', location: '' })
const shipLocationCascader = ref<string[]>([])

// --- Status modify dialog ---
const statusDialogVisible = ref(false)
const statusTarget = ref<Order | null>(null)
const statusForm = ref({ status: 0 })

const statusOptions = [
  { value: 0, label: '待付款' },
  { value: 1, label: '待发货' },
  { value: 2, label: '待收货' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '退款中' }
]

function openStatusDialog(order: Order) {
  statusTarget.value = order
  statusForm.value = { status: order.status }
  statusDialogVisible.value = true
}

async function handleStatusChange() {
  if (!statusTarget.value) return
  const label = statusOptions.find(o => o.value === statusForm.value.status)?.label || '未知'
  try {
    await ElMessageBox.confirm(
      `确定将订单 ${statusTarget.value.orderNo} 设为「${label}」吗？`,
      '确认操作', { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
    await updateOrderStatus(statusTarget.value.id, statusForm.value.status)
    ElMessage.success('状态已更新')
    statusDialogVisible.value = false
    await loadData()
  } catch {}
}

function onShipLocationChange() {
  shipTrack.value.location = shipLocationCascader.value
    .map(code => codeToText[code])
    .filter(Boolean)
    .join('')
}

const statusMap: Record<number, { type: string; text: string }> = {
  0: { type: 'warning', text: '待付款' },
  1: { type: 'primary', text: '待发货' },
  2: { type: '', text: '待收货' },
  3: { type: 'success', text: '已完成' },
  4: { type: 'info', text: '已取消' },
  5: { type: 'danger', text: '退款中' }
}

function syncQueryFromRoute() {
  const q = route.query
  query.value.status = q.status ? Number(q.status) : undefined
  query.value.orderNo = (q.orderNo as string) || ''
  query.value.receiverName = (q.receiverName as string) || ''
  query.value.receiverPhone = (q.receiverPhone as string) || ''
  query.value.page = q.page ? Number(q.page) : 1
  query.value.startTime = (q.startTime as string) || ''
  query.value.endTime = (q.endTime as string) || ''
  if (query.value.startTime && query.value.endTime) {
    dateRange.value = [query.value.startTime, query.value.endTime]
  } else {
    dateRange.value = null
  }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.status !== undefined) params.status = query.value.status
    if (query.value.orderNo) params.orderNo = query.value.orderNo
    if (query.value.receiverName) params.receiverName = query.value.receiverName
    if (query.value.receiverPhone) params.receiverPhone = query.value.receiverPhone
    if (query.value.startTime) params.startTime = query.value.startTime
    if (query.value.endTime) params.endTime = query.value.endTime
    const res = await getAdminOrderList(params)
    orders.value = res.records; total.value = res.total
  } catch {} finally { loading.value = false }
}

function pushQueryToUrl() {
  const params: Record<string, string> = {}
  if (query.value.status !== undefined) params.status = String(query.value.status)
  if (query.value.orderNo) params.orderNo = query.value.orderNo
  if (query.value.receiverName) params.receiverName = query.value.receiverName
  if (query.value.receiverPhone) params.receiverPhone = query.value.receiverPhone
  if (query.value.startTime) params.startTime = query.value.startTime
  if (query.value.endTime) params.endTime = query.value.endTime
  params.page = String(query.value.page)
  router.replace({ query: params })
}

function handleSearch() {
  query.value.page = 1
  pushQueryToUrl()
  loadData()
}

function handleReset() {
  query.value.status = undefined
  query.value.orderNo = ''
  query.value.receiverName = ''
  query.value.receiverPhone = ''
  query.value.startTime = ''
  query.value.endTime = ''
  query.value.page = 1
  dateRange.value = null
  router.replace({ query: {} })
  loadData()
}

function onDateRangeChange(value: [Date, Date] | null) {
  if (value) {
    const fmt = (d: Date) => {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}T00:00:00`
    }
    query.value.startTime = fmt(value[0])
    query.value.endTime = fmt(value[1])
  } else {
    query.value.startTime = ''
    query.value.endTime = ''
  }
}

async function handleUpdateStatus(order: Order, newStatus: number, label: string) {
  try {
    await ElMessageBox.confirm(`确定将订单 ${order.orderNo} 设为「${label}」吗？`, '确认操作', { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' })
    await updateOrderStatus(order.id, newStatus); ElMessage.success('操作成功'); await loadData()
  } catch {}
}

function openShipDialog(order: Order) {
  shipTarget.value = order
  shipForm.value = { logisticsCompany: '', logisticsNo: '' }
  shipTrack.value = { status: 'WAREHOUSE', message: '', location: '' }
  shipLocationCascader.value = []
  shipDialogVisible.value = true
}

async function handleShip() {
  if (!shipTarget.value) return
  if (!shipForm.value.logisticsCompany || !shipForm.value.logisticsNo) {
    ElMessage.warning('请填写物流公司和运单号')
    return
  }
  try {
    await shipOrder(shipTarget.value.id, shipForm.value.logisticsCompany, shipForm.value.logisticsNo)
    ElMessage.success('发货成功')
    await addLogisticsTrack({
      orderId: shipTarget.value.id,
      status: shipTrack.value.status,
      message: shipTrack.value.message || undefined,
      location: shipTrack.value.location || undefined
    }).catch(() => {})
    shipDialogVisible.value = false
    await loadData()
  } catch {}
}

function handlePageChange(page: number) { query.value.page = page; pushQueryToUrl(); loadData() }
function toggleExpand(row: Order) {
  tableRef.value?.toggleRowExpansion(row)
}

onMounted(() => {
  syncQueryFromRoute()
  loadData()
})
</script>

<template>
  <div>
    <div class="page-header"><h2>订单管理</h2></div>
    <el-card>
      <div class="search-bar">
        <div class="search-row">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch" />
          <el-input v-model="query.receiverName" placeholder="收货人" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch" />
          <el-input v-model="query.receiverPhone" placeholder="手机号" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch" />
          <el-select v-model="query.status" placeholder="订单状态" clearable class="search-select" @change="handleSearch">
            <el-option label="全部" :value="undefined" />
            <el-option label="待付款" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="待收货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
            <el-option label="退款中" :value="5" />
          </el-select>
        </div>
        <div class="search-row">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="下单开始"
            end-placeholder="下单结束"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:260px"
            @change="onDateRangeChange"
          />
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
      <el-table ref="tableRef" :data="orders" v-loading="loading" stripe style="width:100%" row-key="id">
        <el-table-column type="expand" width="40">
          <template #default="{ row }">
            <div class="order-items">
              <div v-for="item in row.items" :key="item.id" class="order-item">
                <el-image :src="item.productImage" fit="cover" style="width:50px;height:50px;border-radius:4px" />
                <div class="item-info"><p class="item-name">{{ item.productName }}</p><p class="item-spec" v-if="item.specInfo">{{ item.specInfo }}</p></div>
                <div class="item-price">¥{{ item.price }}</div><div class="item-qty">x{{ item.quantity }}</div><div class="item-subtotal">¥{{ item.subtotal }}</div>
              </div>
              <div v-if="row.logisticsCompany" class="order-logistics">
                <el-icon><Van /></el-icon>
                物流：{{ row.logisticsCompany }} · {{ row.logisticsNo }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="receiverName" label="收货人" width="100" />
        <el-table-column prop="totalAmount" label="金额" width="90"><template #default="{ row }">¥{{ row.totalAmount }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{ row }"><el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.text || '未知' }}</el-tag></template></el-table-column>
        <el-table-column prop="createdTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="310" fixed="right">
          <template #default="{ row }">
            <div class="action-cell">
              <el-button size="small" @click="toggleExpand(row)">详情</el-button>
              <el-button size="small" @click="openStatusDialog(row)">改状态</el-button>
              <el-button v-if="row.status === 1" size="small" type="primary" @click="openShipDialog(row)">发货</el-button>
              <el-button v-if="row.status === 2" size="small" type="success" @click="handleUpdateStatus(row, 3, '已完成')">确认收货</el-button>
              <el-button
                v-if="row.logisticsCompany"
                size="small" type="info"
                @click="router.push({ path: '/logistics', query: { orderId: row.id, orderNo: row.orderNo } })"
              >物流</el-button>
              <el-button v-if="row.status === 5" size="small" @click="router.push('/refunds')">退款</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="prev, pager, next, total" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- Ship Dialog -->
    <el-dialog v-model="shipDialogVisible" title="发货" width="400px">
      <el-form v-if="shipTarget" label-width="80px">
        <el-form-item label="订单号">
          <span>{{ shipTarget.orderNo }}</span>
        </el-form-item>
        <el-form-item label="物流公司" required>
          <el-select v-model="shipForm.logisticsCompany" placeholder="选择物流公司" style="width:100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="京东物流" value="京东物流" />
            <el-option label="EMS" value="EMS" />
            <el-option label="其它" value="其它" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单编号" required>
          <el-input v-model="shipForm.logisticsNo" placeholder="请输入运单编号" />
        </el-form-item>
        <el-divider style="margin:12px 0" />
        <p style="font-size:13px;font-weight:600;color:var(--wz-text);margin:0 0 12px">初始物流状态</p>
        <el-form-item label="物流状态">
          <el-select v-model="shipTrack.status" placeholder="选择物流状态" style="width:100%">
            <el-option label="仓库处理中" value="WAREHOUSE" />
            <el-option label="运输中" value="IN_TRANSIT" />
            <el-option label="待取件" value="PICKUP" />
            <el-option label="已签收" value="DELIVERED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注信息">
          <el-input v-model="shipTrack.message" placeholder="如: 快件已到达北京分拨中心" />
        </el-form-item>
        <el-form-item label="当前位置">
          <el-cascader
            v-model="shipLocationCascader"
            :options="regionData"
            :props="{ expandTrigger: 'hover', label: 'label', value: 'value', children: 'children' }"
            placeholder="选择省/市/区"
            style="width:100%"
            clearable
            @change="onShipLocationChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- Status Modify Dialog -->
    <el-dialog v-model="statusDialogVisible" title="修改订单状态" width="380px">
      <el-form v-if="statusTarget" label-width="80px">
        <el-form-item label="订单号">
          <span>{{ statusTarget.orderNo }}</span>
        </el-form-item>
        <el-form-item label="当前状态">
          <el-tag :type="statusMap[statusTarget.status]?.type" size="small">
            {{ statusMap[statusTarget.status]?.text || '未知' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="新状态" required>
          <el-select v-model="statusForm.status" placeholder="选择新状态" style="width:100%">
            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleStatusChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.search-bar { display: flex; flex-direction: column; gap: 8px; margin-bottom: 20px; }
.search-row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.search-input { width: 160px; }
.search-select { width: 160px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.order-items { padding: 8px 0; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 8px 0; border-bottom: 1px solid #f2f2f2; }
.order-item:last-child { border-bottom: none; }
.item-info { flex: 1; min-width: 0; }
.item-name { margin: 0 0 4px; font-size: 14px; color: #303133; }
.item-spec { margin: 0; font-size: 12px; color: #909399; }
.item-price, .item-qty, .item-subtotal { font-size: 14px; color: #606266; white-space: nowrap; }
.order-logistics { margin-top: 8px; padding-top: 8px; border-top: 1px dashed #e4e7ed; font-size: 13px; color: #909399; display: flex; align-items: center; gap: 4px; }
.action-cell { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
