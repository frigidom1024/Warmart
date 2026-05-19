<script setup lang="ts">
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
              <p class="stat-value">128</p>
              <p class="stat-trend up">较昨日 +12%</p>
            </div>
            <div class="stat-icon order-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 01-8 0"/></svg>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">成交金额</p>
              <p class="stat-value">¥23,456</p>
              <p class="stat-trend up">较昨日 +8.5%</p>
            </div>
            <div class="stat-icon revenue-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></svg>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">注册用户</p>
              <p class="stat-value">1,024</p>
              <p class="stat-trend up">较昨日 +5.3%</p>
            </div>
            <div class="stat-icon user-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">待处理</p>
              <p class="stat-value">16</p>
              <p class="stat-trend down">较昨日 -3</p>
            </div>
            <div class="stat-icon pending-icon">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>订单趋势</span>
          </template>
          <div class="chart-placeholder">
            <el-empty description="订单趋势图表" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>热销商品</span>
          </template>
          <div class="chart-placeholder">
            <el-empty description="热销商品排行" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="table-card">
      <template #header>
        <span>最近订单</span>
      </template>
      <div class="table-placeholder">
        <el-empty description="最近订单列表" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--wa-text-primary, #303133);
}

.page-desc {
  font-size: 13px;
  color: var(--wa-text-secondary, #909399);
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.stat-info {
  flex: 1;
}

.stat-label {
  margin: 0 0 4px;
  font-size: 13px;
  color: var(--wa-text-secondary, #909399);
}

.stat-value {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: var(--wa-text-primary, #303133);
}

.stat-trend {
  margin: 0;
  font-size: 12px;
}

.stat-trend.up {
  color: #67c23a;
}

.stat-trend.down {
  color: #f56c6c;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.order-icon {
  background: #ecf5ff;
  color: var(--wa-color-primary, #409eff);
}

.revenue-icon {
  background: #f0f9eb;
  color: #67c23a;
}

.user-icon {
  background: #fdf6ec;
  color: #e6a23c;
}

.pending-icon {
  background: #fef0f0;
  color: #f56c6c;
}

.chart-row {
  margin-bottom: 20px;
}

.chart-placeholder {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.table-card {
  border-radius: 8px;
}

.table-placeholder {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
