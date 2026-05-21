<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const users = ref<any[]>([])

// Placeholder - user admin list API not yet implemented on backend
onMounted(() => {
  loading.value = false
})
</script>

<template>
  <div>
    <div class="page-header">
      <h2>用户管理</h2>
    </div>
    <el-card>
      <div class="search-bar">
        <el-input placeholder="搜索用户名、手机号" clearable class="search-input" />
        <el-button type="primary">搜索</el-button>
        <el-button>重置</el-button>
      </div>
      <el-empty description="用户管理功能开发中" />
    </el-card>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: var(--wa-text-primary, #303133); }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.search-input { width: 300px; }
</style>
