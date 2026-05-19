<script setup lang="ts">
</script>

<template>
  <div>
    <div class="page-header">
      <h2>分类管理</h2>
    </div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品分类</span>
          <el-button type="primary" size="small">新增分类</el-button>
        </div>
      </template>
      <div class="tree-table-placeholder">
        <el-empty description="分类树形表格" />
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

.tree-table-placeholder {
  min-height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
