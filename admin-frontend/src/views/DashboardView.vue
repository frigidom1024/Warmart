<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminOrderList } from '@/api/order'
import { getAdminProductList } from '@/api/product'
import type { Order } from '@/api/order'

const todayOrderCount = ref(0)
const totalRevenue = ref(0)
const productCount = ref(0)
const pendingCount = ref(0)
const recentOrders = ref<Order[]>([])
const loading = ref(false)

const statusMap: Record<number, { type: string; text: string }> = {
  0: { type: 'warning', text: '待付款' },
  1: { type: 'primary', text: '待发货' },
  2: { type: '', text: '待收货' },
  3: { type: 'success', text: '已完成' },
  4: { type: 'info', text: '已取消' },
  5: { type: 'danger', text: '退款中' }
}

async function loadDashboard() {
  loading.value = true
  try {
    const orderRes = await getAdminOrderList({ page: 1, size: 1000 })
    const allOrders = orderRes.records || []
    const today = new Date().toISOString().slice(0, 10)
    todayOrderCount.value = allOrders.filter((o: Order) => o.createdTime?.startsWith(today)).length
    totalRevenue.value = allOrders.filter((o: Order) => o.status === 3).reduce((s: number, o: Order) => s + o.totalAmount, 0)
    pendingCount.value = allOrders.filter((o: Order) => o.status === 0 || o.status === 1).length
    recentOrders.value = allOrders.slice(0, 5)
  } catch {}
  try {
    const productRes = await getAdminProductList({ page: 1, size: 1 })
    productCount.value = productRes.total || 0
  } catch {}
  loading.value = false
}

onMounted(loadDashboard)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>控制台</h2>
      <span class="page-desc">欢迎回来，这是今天的运营概览</span>
    </div>
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">今日订单</p>
              <p class="stat-value">{{ todayOrderCount }}</p>
            </div>
            <div class="stat-icon order-icon"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">成交金额</p>
              <p class="stat-value">¥{{ totalRevenue }}</p>
            </div>
            <div class="stat-icon revenue-icon"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></svg></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">商品数量</p>
              <p class="stat-value">{{ productCount }}</p>
            </div>
            <div class="stat-icon user-icon"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg></div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">待处理</p>
              <p class="stat-value">{{ pendingCount }}</p>
            </div>
            <div class="stat-icon pending-icon"><svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg></div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="table-card">
      <template #header><span>最近订单</span></template>
      <el-table :data="recentOrders" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="receiverName" label="收货人" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="100"><template #default="{ row }">¥{{ row.totalAmount }}</template></el-table-column>
        <el-table-column prop="status" label="状态" width="100"><template #default="{ row }"><el-tag :type="statusMap[row.status]?.type || ''" size="small">{{ statusMap[row.status]?.text || '未知' }}</el-tag></template></el-table-column>
        <el-table-column prop="createdTime" label="下单时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-header { display: flex; align-items: baseline; gap: 12px; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.page-desc { font-size: 13px; color: #909399; }
.stat-row { margin-bottom: 20px; }
.stat-card { border-radius: 8px; transition: transform 0.2s; }
.stat-card:hover { transform: translateY(-2px); }
.stat-content { display: flex; justify-content: space-between; align-items: flex-start; }
.stat-info { flex: 1; }
.stat-label { margin: 0 0 4px; font-size: 13px; color: #909399; }
.stat-value { margin: 0 0 6px; font-size: 26px; font-weight: 700; color: #303133; }
.stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.order-icon { background: #ecf5ff; color: #409eff; }
.revenue-icon { background: #f0f9eb; color: #67c23a; }
.user-icon { background: #fdf6ec; color: #e6a23c; }
.pending-icon { background: #fef0f0; color: #f56c6c; }
.table-card { border-radius: 8px; }
</style>
