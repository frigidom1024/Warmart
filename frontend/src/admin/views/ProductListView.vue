<script setup lang="ts">
</script>

<template>
  <div>
    <div class="page-header">
      <h2>商品管理</h2>
    </div>
    <el-card>
      <div class="search-bar">
        <el-input
          placeholder="搜索商品名称、编号"
          clearable
          class="search-input"
        />
        <el-select placeholder="商品分类" clearable class="search-select" />
        <el-select placeholder="上下架状态" clearable class="search-select" />
        <el-button type="primary">搜索</el-button>
        <el-button>重置</el-button>
      </div>
      <div class="table-placeholder">
        <el-empty description="商品列表" />
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

.table-placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
