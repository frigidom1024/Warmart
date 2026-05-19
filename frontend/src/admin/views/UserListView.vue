<script setup lang="ts">
</script>

<template>
  <div>
    <div class="page-header">
      <h2>用户管理</h2>
    </div>
    <el-card>
      <div class="search-bar">
        <el-input
          placeholder="搜索用户名、手机号"
          clearable
          class="search-input"
        />
        <el-button type="primary">搜索</el-button>
        <el-button>重置</el-button>
      </div>
      <div class="table-placeholder">
        <el-empty description="用户列表" />
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
}

.search-input {
  width: 300px;
}

.table-placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
