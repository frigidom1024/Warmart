<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Van } from '@element-plus/icons-vue'
import { getAdminOrderList } from '@/api/order'
import type { Order } from '@/api/order'
import { getLogisticsTracks, updateLogisticsTrack, deleteLogisticsTrack, addLogisticsTrack } from '@/api/logistics'
import type { LogisticsTrack } from '@/api/logistics'
import { regionData, codeToText } from 'element-china-area-data'

const route = useRoute()
const router = useRouter()

// Build reverse map: Chinese text → numeric code (for parsing stored location back to cascader values)
const textToCode: Record<string, string> = {}
for (const [code, text] of Object.entries(codeToText)) {
  textToCode[text] = code
}

function parseLocationToCodes(location: string): string[] {
  if (!location) return []
  const codes: string[] = []
  let remaining = location
  // Sort by longest text first to match multi-char names before substrings
  const names = Object.keys(textToCode).sort((a, b) => b.length - a.length)
  for (const name of names) {
    if (remaining.startsWith(name)) {
      codes.push(textToCode[name])
      remaining = remaining.slice(name.length)
      if (!remaining) break
    }
  }
  return codes
}

// --- Order list state ---
const orders = ref<Order[]>([])
const total = ref(0)
const loading = ref(false)
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

// --- Timeline dialog ---
const timelineDialogVisible = ref(false)
const timelineOrder = ref<Order | null>(null)
const tracks = ref<LogisticsTrack[]>([])
const tracksLoading = ref(false)

// --- Add/Edit track dialog ---
const trackDialogVisible = ref(false)
const trackDialogTitle = ref('')
const editingTrackId = ref<number | null>(null)
const locationCascader = ref<string[]>([])
const trackForm = ref({
  orderId: 0,
  status: 'IN_TRANSIT',
  message: '',
  location: '',
  trackTime: ''
})

function onLocationChange() {
  trackForm.value.location = locationCascader.value
    .map(code => codeToText[code])
    .filter(Boolean)
    .join('')
}

// --- Status maps ---
const statusMap: Record<number, { type: string; text: string }> = {
  0: { type: 'warning', text: '待付款' },
  1: { type: 'primary', text: '待发货' },
  2: { type: '', text: '待收货' },
  3: { type: 'success', text: '已完成' },
  4: { type: 'info', text: '已取消' },
  5: { type: 'danger', text: '退款中' }
}

const trackStatusMap: Record<string, { type: string; text: string }> = {
  ORDERED: { type: 'info', text: '已下单' },
  WAREHOUSE: { type: 'primary', text: '仓库处理' },
  IN_TRANSIT: { type: 'warning', text: '运输中' },
  PICKUP: { type: 'primary', text: '待取件' },
  DELIVERED: { type: 'success', text: '已签收' }
}

// --- Data loading ---
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
    orders.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

function pushQueryToUrl() {
  const params: Record<string, string> = {}
  if (query.value.status !== undefined) params.status = String(query.value.status)
  if (query.value.orderNo) params.orderNo = query.value.orderNo
  if (query.value.receiverName) params.receiverName = query.value.receiverName
  if (query.value.receiverPhone) params.receiverPhone = query.value.receiverPhone
  params.page = String(query.value.page)
  router.replace({ query: params })
}

function handleSearch() { query.value.page = 1; pushQueryToUrl(); loadData() }

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

function handlePageChange(page: number) { query.value.page = page; pushQueryToUrl(); loadData() }

// --- Timeline ---
async function openTimeline(order: Order) {
  timelineOrder.value = order
  tracksLoading.value = true
  timelineDialogVisible.value = true
  try {
    tracks.value = await getLogisticsTracks(order.id)
  } catch { tracks.value = [] } finally { tracksLoading.value = false }
}

function closeTimeline() {
  timelineDialogVisible.value = false
  timelineOrder.value = null
  tracks.value = []
}

// --- Add track ---
function openAddTrack(order: Order) {
  trackDialogTitle.value = '添加物流信息'
  editingTrackId.value = null
  trackForm.value = {
    orderId: order.id,
    status: 'IN_TRANSIT',
    message: '',
    location: '',
    trackTime: ''
  }
  locationCascader.value = []
  trackDialogVisible.value = true
}

// --- Edit track ---
function openEditTrack(track: LogisticsTrack) {
  trackDialogTitle.value = '编辑物流信息'
  editingTrackId.value = track.id
  trackForm.value = {
    orderId: track.orderId,
    status: track.status,
    message: track.message || '',
    location: track.location || '',
    trackTime: track.trackTime || ''
  }
  locationCascader.value = parseLocationToCodes(track.location || '')
  trackDialogVisible.value = true
}

// --- Save track (add or edit) ---
async function handleSaveTrack() {
  try {
    if (editingTrackId.value) {
      const data: any = { status: trackForm.value.status }
      if (trackForm.value.message) data.message = trackForm.value.message
      if (trackForm.value.location) data.location = trackForm.value.location
      if (trackForm.value.trackTime) data.trackTime = trackForm.value.trackTime
      await updateLogisticsTrack(editingTrackId.value, data)
      ElMessage.success('物流信息已更新')
    } else {
      await addLogisticsTrack({
        orderId: trackForm.value.orderId,
        status: trackForm.value.status,
        message: trackForm.value.message || undefined,
        location: trackForm.value.location || undefined,
        trackTime: trackForm.value.trackTime || undefined
      })
      ElMessage.success('物流信息已添加')
    }
    trackDialogVisible.value = false
    if (timelineOrder.value) {
      await openTimeline(timelineOrder.value)
    }
  } catch {}
}

// --- Delete track ---
async function handleDeleteTrack(track: LogisticsTrack) {
  try {
    await ElMessageBox.confirm('确定要删除此物流记录吗？', '确认删除', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    await deleteLogisticsTrack(track.id)
    ElMessage.success('已删除')
    if (timelineOrder.value) {
      await openTimeline(timelineOrder.value)
    }
  } catch {}
}

onMounted(() => {
  syncQueryFromRoute()
  loadData().then(() => {
    // If navigated from order list with an orderId, auto-open its timeline
    const orderId = route.query.orderId
    const orderNo = route.query.orderNo as string | undefined
    if (orderId) {
      const id = Number(orderId)
      // Try to find the order in loaded data
      const found = orders.value.find(o => o.id === id)
      const order = found || { id, orderNo: orderNo || `#${id}` } as Order
      openTimeline(order)
    }
  })
})
</script>

<template>
  <div>
    <div class="page-header"><h2>物流管理</h2></div>
    <el-card>
      <!-- Search -->
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

      <!-- Orders table -->
      <el-table :data="orders" v-loading="loading" stripe style="width:100%" row-key="id">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="receiverName" label="收货人" width="100" />
        <el-table-column label="物流信息" width="220">
          <template #default="{ row }">
            <span v-if="row.logisticsCompany" class="logistics-cell">
              <el-icon><Van /></el-icon>
              {{ row.logisticsCompany }} · {{ row.logisticsNo }}
            </span>
            <span v-else class="no-logistics">未发货</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small">
              {{ statusMap[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openTimeline(row)" :disabled="!row.logisticsCompany">
              查看物流
            </el-button>
            <el-button size="small" @click="openAddTrack(row)" :disabled="!row.logisticsCompany">
              添加轨迹
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Timeline Dialog -->
    <el-dialog
      v-model="timelineDialogVisible"
      :title="`物流详情 — ${timelineOrder?.orderNo || ''}`"
      width="560px"
      @close="closeTimeline"
    >
      <div v-if="tracksLoading" style="text-align:center;padding:40px 0">
        <span style="color:#909399">加载中...</span>
      </div>
      <div v-else-if="tracks.length === 0" style="text-align:center;padding:40px 0">
        <span style="color:#909399">暂无物流记录</span>
      </div>
      <el-timeline v-else>
        <el-timeline-item
          v-for="track in tracks"
          :key="track.id"
          :timestamp="track.trackTime"
          placement="top"
        >
          <div class="track-item">
            <div class="track-item-header">
              <el-tag :type="trackStatusMap[track.status]?.type" size="small">
                {{ trackStatusMap[track.status]?.text || track.status }}
              </el-tag>
              <span class="track-item-message">{{ track.message || '' }}</span>
            </div>
            <div class="track-item-meta">
              <span v-if="track.location">{{ track.location }}</span>
              <span class="track-item-actions">
                <el-button size="small" text @click="openEditTrack(track)">编辑</el-button>
                <el-button size="small" text type="danger" @click="handleDeleteTrack(track)">删除</el-button>
              </span>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>

    <!-- Add/Edit Track Dialog -->
    <el-dialog v-model="trackDialogVisible" :title="trackDialogTitle" width="420px">
      <el-form label-width="80px">
        <el-form-item label="物流状态" required>
          <el-select v-model="trackForm.status" placeholder="选择物流状态" style="width:100%">
            <el-option label="已下单" value="ORDERED" />
            <el-option label="仓库处理中" value="WAREHOUSE" />
            <el-option label="运输中" value="IN_TRANSIT" />
            <el-option label="待取件" value="PICKUP" />
            <el-option label="已签收" value="DELIVERED" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述信息">
          <el-input v-model="trackForm.message" placeholder="如: 快件已到达北京分拨中心" />
        </el-form-item>
        <el-form-item label="当前位置">
          <el-cascader
            v-model="locationCascader"
            :options="regionData"
            :props="{ expandTrigger: 'hover', label: 'label', value: 'value', children: 'children' }"
            placeholder="选择省/市/区"
            style="width:100%"
            clearable
            @change="onLocationChange"
          />
        </el-form-item>
        <el-form-item label="更新时间">
          <el-date-picker
            v-model="trackForm.trackTime"
            type="datetime"
            placeholder="选择时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width:100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="trackDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveTrack">确认</el-button>
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
.logistics-cell { display: inline-flex; align-items: center; gap: 4px; font-size: 13px; color: #606266; }
.no-logistics { font-size: 13px; color: #c0c4cc; }

.track-item { padding: 4px 0; }
.track-item-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.track-item-message { font-size: 14px; color: #303133; }
.track-item-meta { display: flex; align-items: center; justify-content: space-between; font-size: 12px; color: #909399; margin-top: 2px; }
.track-item-actions { display: flex; gap: 4px; }
</style>
