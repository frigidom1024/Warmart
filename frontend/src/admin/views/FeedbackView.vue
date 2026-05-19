<script setup lang="ts">
</script>

<template>
  <div>
    <div class="page-header">
      <h2>反馈管理</h2>
    </div>
    <el-card>
      <div class="table-placeholder">
        <el-empty description="用户反馈列表" />
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

.table-placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
