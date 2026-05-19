<script setup lang="ts">
</script>

<template>
  <div>
    <div class="page-header">
      <h2>轮播管理</h2>
    </div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>首页轮播图</span>
          <el-button type="primary" size="small">添加轮播</el-button>
        </div>
      </template>
      <div class="table-placeholder">
        <el-empty description="轮播列表" />
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
