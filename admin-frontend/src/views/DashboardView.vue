<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getAdminOrderList } from '@/api/order'
import { getAdminDashboardStats, getAdminUserCount } from '@/api/dashboard'
import type { Order } from '@/api/order'
import type { DashboardStats } from '@/api/dashboard'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { ElEmpty } from 'element-plus'

use([CanvasRenderer, LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent])

// ── Data ──
const stats = ref<DashboardStats | null>(null)
const totalUsers = ref(0)
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

const statusColorMap: Record<number, string> = {
  0: '#e6a23c',
  1: '#409eff',
  2: '#909399',
  3: '#67c23a',
  4: '#909399',
  5: '#f56c6c'
}

const statusLabelMap: Record<number, string> = {
  0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消', 5: '退款中'
}

// ── Load ──
async function loadDashboard() {
  loading.value = true
  try {
    const [statsData, userCount, orderRes] = await Promise.all([
      getAdminDashboardStats(),
      getAdminUserCount(),
      getAdminOrderList({ page: 1, size: 5 })
    ])
    stats.value = { ...statsData, totalUsers: userCount }
    totalUsers.value = userCount
    recentOrders.value = (orderRes as any).records || []
  } catch {
    // Fallback: load individually
    try {
      const statsData = await getAdminDashboardStats()
      const orderRes = await getAdminOrderList({ page: 1, size: 5 })
      stats.value = { ...statsData, totalUsers: 0 }
      recentOrders.value = (orderRes as any).records || []
    } catch {
      // Silent fail
    }
    try {
      totalUsers.value = await getAdminUserCount()
    } catch {}
  }
  loading.value = false
}

onMounted(loadDashboard)

// ── Format helpers ──
function formatSales(val: number): string {
  if (val >= 10000) return (val / 10000).toFixed(1) + '万'
  return val.toLocaleString()
}

// ── ECharts Option helpers ──

/** 销量趋势 - 折线图 */
const salesTrendOption = computed(() => {
  const data = stats.value?.salesTrend || []
  if (!data.length) return null
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { fontSize: 12 },
      formatter: (params: any) => {
        const p = params[0]
        const item = data[p.dataIndex]
        if (!item) return ''
        const date = item.date || ''
        const count = item.order_count || 0
        const amount = item.sales_amount || 0
        return '<strong>' + date + '</strong><br/>订单数: ' + count + '<br/>销售额: ¥' + formatSales(amount)
      }
    },
    grid: { left: 50, right: 20, top: 30, bottom: 25 },
    xAxis: {
      type: 'category',
      data: data.map((d: any) => (d.date || '').slice(5)),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { color: '#909399', fontSize: 11 }
    },
    series: [{
      type: 'line',
      data: data.map((d: any) => d.order_count || 0),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color: '#409eff', width: 2.5 },
      itemStyle: { color: '#409eff' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64,158,255,0.25)' },
            { offset: 1, color: 'rgba(64,158,255,0.02)' }
          ]
        }
      }
    }]
  }
})

/** 订单状态统计 - 环形图 */
const orderStatusOption = computed(() => {
  const data = stats.value?.orderStatusStats || []
  if (!data.length) return null
  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: '#fff',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { fontSize: 12 },
      formatter: (p: any) => `<strong>${statusLabelMap[p.data.status] || '未知'}</strong><br/>数量: ${p.value}<br/>占比: ${p.percent}%`
    },
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 12, color: '#606266' },
      formatter: (name: string) => {
        const item = data.find(d => statusLabelMap[d.status] === name)
        return `${name}  ${item?.count || 0}`
      }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 600 },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.15)' }
      },
      data: data.map((d: any) => ({
        value: d.count,
        name: statusLabelMap[d.status] || '未知',
        status: d.status,
        itemStyle: { color: statusColorMap[d.status] || '#909399' }
      }))
    }]
  }
})

/** 热销商品排行 - 柱状图 */
const hotProductOption = computed(() => {
  const data = stats.value?.hotProducts || []
  if (!data.length) return null
  const top = data.slice(0, 8)
  return {
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#fff',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { fontSize: 12 },
      formatter: (params: any) => {
        const p = params[0]
        const item = top[p.dataIndex]
        if (!item) return ''
        const name = item.productName || '未命名商品'
        const sales = item.sales || 0
        const amount = item.amount || 0
        return '<strong>' + name + '</strong><br/>销量: ' + sales + '<br/>销售额: ¥' + formatSales(amount)
      }
    },
    grid: { left: 120, right: 50, top: 15, bottom: 20 },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
      axisLabel: { color: '#909399', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: top.map(d => (d.productName || '未命名商品').length > 10 ? (d.productName || '未命名商品').slice(0, 10) + '…' : (d.productName || '未命名商品')),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#606266', fontSize: 12 }
    },
    series: [{
      type: 'bar',
      data: top.map((d, i) => ({
        value: d.sales || 0,
        itemStyle: {
          color: i === 0 ? '#f56c6c' : i === 1 ? '#e6a23c' : i === 2 ? '#409eff' : '#a0cfff',
          borderRadius: [0, 4, 4, 0]
        }
      })),
      barWidth: 20,
      label: {
        show: true,
        position: 'right',
        formatter: (p: any) => formatSales(top[p.dataIndex]?.sales || 0),
        fontSize: 11,
        color: '#606266'
      }
    }]
  }
})

// ── Stat cards data ──
const statCards = computed(() => [
  {
    label: '总用户数',
    value: stats.value?.totalUsers?.toLocaleString() || '—',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
    bg: '#ecf5ff', color: '#409eff'
  },
  {
    label: '总订单数',
    value: stats.value?.totalOrders?.toLocaleString() || '—',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>',
    bg: '#f0f9eb', color: '#67c23a'
  },
  {
    label: '总销售额',
    value: stats.value ? '¥' + formatSales(stats.value.totalSales) : '—',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>',
    bg: '#fdf6ec', color: '#e6a23c'
  },
  {
    label: '今日订单',
    value: stats.value?.todayOrders?.toLocaleString() || '—',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>',
    bg: '#fef0f0', color: '#f56c6c'
  },
  {
    label: '今日销售',
    value: stats.value ? '¥' + formatSales(stats.value.todaySales) : '—',
    icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>',
    bg: '#f0f9eb', color: '#67c23a'
  }
])
</script>

<template>
  <div class="dashboard">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h2>控制台</h2>
        <span class="page-desc">欢迎回来，这是今天的运营概览</span>
      </div>
      <el-button size="small" :loading="loading" @click="loadDashboard">
        <template #icon><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg></template>
        刷新
      </el-button>
    </div>

    <!-- Stat Cards -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-card__body">
          <div class="stat-card__info">
            <p class="stat-card__label">{{ card.label }}</p>
            <p class="stat-card__value" :style="{ color: card.color }">{{ card.value }}</p>
          </div>
          <div class="stat-card__icon" :style="{ background: card.bg, color: card.color }" v-html="card.icon" />
        </div>
      </div>
    </div>

    <!-- Charts Row -->
    <div class="charts-row">
      <!-- Sales Trend -->
      <div class="chart-card chart-card--wide">
        <div class="chart-card__header">
          <h3>销量趋势</h3>
          <span class="chart-card__hint">近 14 天</span>
        </div>
        <div v-if="salesTrendOption" class="chart-container">
          <VChart :option="salesTrendOption" autoresize />
        </div>
        <div v-else class="chart-empty">
          <el-empty :image-size="80" description="暂无销量数据" />
        </div>
      </div>

      <!-- Order Status -->
      <div class="chart-card">
        <div class="chart-card__header">
          <h3>订单状态</h3>
        </div>
        <div v-if="orderStatusOption" class="chart-container chart-container--pie">
          <VChart :option="orderStatusOption" autoresize />
        </div>
        <div v-else class="chart-empty">
          <el-empty :image-size="80" description="暂无订单数据" />
        </div>
      </div>
    </div>

    <!-- Hot Products -->
    <div class="chart-card chart-card--full">
      <div class="chart-card__header">
        <h3>热销商品排行</h3>
        <span class="chart-card__hint">按销量排序 Top 10</span>
      </div>
      <div v-if="hotProductOption" class="chart-container chart-container--bar">
        <VChart :option="hotProductOption" autoresize />
      </div>
      <div v-else class="chart-empty">
        <el-empty :image-size="80" description="暂无商品数据" />
      </div>
    </div>

    <!-- Recent Orders -->
    <div class="chart-card chart-card--full">
      <div class="chart-card__header">
        <h3>最近订单</h3>
      </div>
      <el-table :data="recentOrders" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="orderNo" label="订单号" width="190" />
        <el-table-column prop="receiverName" label="收货人" width="110" />
        <el-table-column prop="totalAmount" label="金额" width="90">
          <template #default="{ row }">¥{{ row.totalAmount }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || ''" size="small">
              {{ statusMap[row.status]?.text || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="90">
          <template #default>
            <el-button size="small" @click="$router.push('/orders')">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
/* ── Layout ── */
.dashboard {
  max-width: 1400px;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}
.page-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  display: block;
}

/* ── Stat Cards ── */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
@media (max-width: 1200px) {
  .stat-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #ebeef5;
  transition: transform 0.2s, box-shadow 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.06);
}
.stat-card__body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.stat-card__info { flex: 1; }
.stat-card__label {
  margin: 0 0 6px;
  font-size: 13px;
  color: #909399;
}
.stat-card__value {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}
.stat-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* ── Charts ── */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 960px) {
  .charts-row { grid-template-columns: 1fr; }
}
.chart-card {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  padding: 20px;
  margin-bottom: 16px;
}
.chart-card--full { margin-bottom: 16px; }
.chart-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}
.chart-card__header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.chart-card__hint {
  font-size: 12px;
  color: #c0c4cc;
}
.chart-container {
  width: 100%;
  height: 280px;
}
.chart-container--pie {
  height: 260px;
}
.chart-container--bar {
  height: 340px;
}
.chart-empty {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
