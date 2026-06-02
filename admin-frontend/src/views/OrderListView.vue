<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Van } from '@element-plus/icons-vue'
import { getAdminOrderList, updateOrderStatus, shipOrder } from '@/api/order'
import type { Order } from '@/api/order'

const orders = ref<Order[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({ status: undefined as number | undefined, page: 1, size: 10 })
const expandedRows = ref<number[]>([])
const shipDialogVisible = ref(false)
const shipTarget = ref<Order | null>(null)
const shipForm = ref({ logisticsCompany: '', logisticsNo: '' })

const statusMap: Record<number, { type: string; text: string }> = {
  0: { type: 'warning', text: '待付款' },
  1: { type: 'primary', text: '待发货' },
  2: { type: '', text: '待收货' },
  3: { type: 'success', text: '已完成' },
  4: { type: 'info', text: '已取消' },
  5: { type: 'danger', text: '退款中' }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.status !== undefined) params.status = query.value.status
    const res = await getAdminOrderList(params)
    orders.value = res.records; total.value = res.total
  } catch {} finally { loading.value = false }
}
function handleSearch() { query.value.page = 1; loadData() }
function handleReset() { query.value.status = undefined; query.value.page = 1; loadData() }

async function handleUpdateStatus(order: Order, newStatus: number, label: string) {
  try {
    await ElMessageBox.confirm(`确定将订单 ${order.orderNo} 设为「${label}」吗？`, '确认操作', { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' })
    await updateOrderStatus(order.id, newStatus); ElMessage.success('操作成功'); await loadData()
  } catch {}
}

function openShipDialog(order: Order) {
  shipTarget.value = order
  shipForm.value = { logisticsCompany: '', logisticsNo: '' }
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
    shipDialogVisible.value = false
    await loadData()
  } catch {}
}

function handlePageChange(page: number) { query.value.page = page; loadData() }
function toggleExpand(row: Order) {
  const idx = expandedRows.value.indexOf(row.id)
  if (idx >= 0) expandedRows.value.splice(idx, 1); else expandedRows.value.push(row.id)
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header"><h2>订单管理</h2></div>
    <el-card>
      <div class="search-bar">
        <el-select v-model="query.status" placeholder="订单状态" clearable class="search-select" @change="handleSearch">
          <el-option label="全部" :value="undefined" /><el-option label="待付款" :value="0" /><el-option label="待发货" :value="1" /><el-option label="待收货" :value="2" /><el-option label="已完成" :value="3" /><el-option label="已取消" :value="4" /><el-option label="退款中" :value="5" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-table :data="orders" v-loading="loading" stripe style="width:100%" @expand-change="toggleExpand">
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="toggleExpand(row)">详情</el-button>
            <el-button v-if="row.status === 1" size="small" type="primary" @click="openShipDialog(row)">发货</el-button>
            <el-button v-if="row.status === 2" size="small" type="success" @click="handleUpdateStatus(row, 3, '已完成')">确认收货</el-button>
            <el-tag v-if="row.logisticsCompany" size="small" type="info" style="margin-left:4px">已发货</el-tag>
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
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
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
</style>
