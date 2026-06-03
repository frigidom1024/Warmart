<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import { getAdminCommentList, deleteAdminComment } from '@/api/comment'
import type { CommentItem } from '@/api/comment'

const comments = ref<CommentItem[]>([])
const total = ref(0)
const loading = ref(false)

const query = ref({
  productName: '',
  username: '',
  rating: undefined as number | undefined,
  page: 1,
  size: 10
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminCommentList(query.value)
    comments.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.value.page = 1
  loadData()
}

function onReset() {
  query.value.productName = ''
  query.value.username = ''
  query.value.rating = undefined
  query.value.page = 1
  loadData()
}

function onPageChange(p: number) {
  query.value.page = p
  loadData()
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除该评价？', '确认删除', { type: 'warning' })
    await deleteAdminComment(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>评价管理</h2>
    </div>

    <el-card class="search-card">
      <el-form :model="query" inline>
        <el-form-item label="商品名称">
          <el-input v-model="query.productName" placeholder="输入商品名称" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="输入用户名" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="评分">
          <el-select v-model="query.rating" placeholder="全部" clearable style="width:120px">
            <el-option label="5星" :value="5" />
            <el-option label="4星" :value="4" />
            <el-option label="3星" :value="3" />
            <el-option label="2星" :value="2" />
            <el-option label="1星" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="comments" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="productName" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column label="评分" width="140">
          <template #default="{ row }">
            <el-icon v-for="i in row.rating" :key="i" color="#f7ba2a"><StarFilled /></el-icon>
            <el-icon v-for="i in (5 - row.rating)" :key="'e'+i" color="#ccc"><StarFilled /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="imageUrls" label="图片" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrls"
              :src="row.imageUrls.split(',')[0]"
              style="width:50px;height:50px"
              fit="cover"
              :preview-src-list="row.imageUrls.split(',')"
              preview-teleported
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="评价时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0; font-size: 20px; }
.search-card { margin-bottom: 16px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
