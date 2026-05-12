<template>
  <div v-loading="loading" class="dashboard-container">
    <!-- Statistics Cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col :xs="24" :sm="12" :md="6" v-for="item in statCards" :key="item.label">
        <el-card :style="{ background: item.bg, color: '#fff', border: 'none' }" shadow="hover" body-style="padding: 20px">
          <div class="stat-inner">
            <div class="stat-icon-wrapper">
              <el-icon :size="36">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="stat-details">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Content Section -->
    <el-row :gutter="20" class="content-row">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span class="section-title">最近订单</span>
          </template>
          <el-table :data="recentOrders" style="width: 100%" empty-text="暂无数据" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="orderNo" label="订单号" min-width="140" />
            <el-table-column prop="totalAmount" label="金额（元）" width="100" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'PAID' ? 'success' : row.status === 'PENDING' ? 'warning' : 'info'">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover" class="welcome-card">
          <div class="welcome-content">
            <el-icon :size="64" color="#409eff">
              <Monitor />
            </el-icon>
            <h2 class="welcome-title">欢迎使用商城管理系统</h2>
            <p class="welcome-desc">请使用左侧菜单导航管理您的商城</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { User, ShoppingCart, Coin, Clock, Monitor } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/admin'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const recentOrders = ref([])

const defaultStats = [
  { label: '用户总数', value: '0', icon: User, bg: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { label: '订单总数', value: '0', icon: ShoppingCart, bg: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { label: '销售总额', value: '¥0.00', icon: Coin, bg: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)' },
  { label: '今日订单', value: '0', icon: Clock, bg: 'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)' }
]

const statCards = ref(defaultStats.map((s) => ({ ...s })))

const formatCurrency = (val) => {
  if (val == null) return '¥0.00'
  return `¥${Number(val).toFixed(2)}`
}

const fetchStats = async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    if (res) {
      statCards.value = [
        { ...defaultStats[0], value: res.userCount ?? '0' },
        { ...defaultStats[1], value: res.orderCount ?? '0' },
        { ...defaultStats[2], value: formatCurrency(res.salesTotal) },
        { ...defaultStats[3], value: res.todayOrders ?? '0' }
      ]
      recentOrders.value = res.recentOrders || []
    }
  } catch (err) {
    ElMessage.error('获取统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
}

.stat-details {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  margin-top: 4px;
  opacity: 0.85;
}

.content-row {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.welcome-card {
  height: 100%;
}

.welcome-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 260px;
  text-align: center;
}

.welcome-title {
  margin-top: 20px;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.welcome-desc {
  margin-top: 10px;
  font-size: 14px;
  color: #909399;
}
</style>
