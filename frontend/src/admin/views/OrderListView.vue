<script setup lang="ts">
</script>

<template>
  <div>
    <div class="page-header">
      <h2>订单管理</h2>
    </div>
    <el-card>
      <div class="search-bar">
        <el-input
          placeholder="搜索订单号"
          clearable
          class="search-input"
        />
        <el-select placeholder="订单状态" clearable class="search-select" />
        <el-date-picker
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="search-date"
        />
        <el-button type="primary">搜索</el-button>
        <el-button>重置</el-button>
      </div>
      <div class="table-placeholder">
        <el-empty description="订单列表" />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--wa-text-primary, #303133);
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-input {
  width: 240px;
}

.search-select {
  width: 160px;
}

.search-date {
  width: 260px;
}

.table-placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
