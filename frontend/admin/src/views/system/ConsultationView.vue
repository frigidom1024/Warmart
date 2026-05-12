<template>
  <div class="consultation-view">
    <div class="page-header">
      <h3>咨询管理</h3>
    </div>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="userId" label="用户 ID" width="90" align="center" />
        <el-table-column prop="productId" label="商品 ID" width="90" align="center" />
        <el-table-column label="内容" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ truncate(row.content, 60) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'pending' ? 'warning' : 'success'" size="small">
              {{ row.status === 'pending' ? '未回复' : '已回复' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleReply(row)">回复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Reply Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="回复咨询"
      width="560px"
      :close-on-click-modal="false"
    >
      <template v-if="currentItem">
        <div class="question-section">
          <h4>用户问题</h4>
          <div class="question-content">{{ currentItem.content }}</div>
        </div>

        <el-divider />

        <div class="reply-section">
          <h4>回复</h4>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="5"
            placeholder="请输入回复内容"
          />
          <el-button
            type="primary"
            style="margin-top: 14px"
            :loading="replying"
            @click="handleSubmit"
          >
            提交回复
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const currentItem = ref(null)
const replyContent = ref('')
const replying = ref(false)

const truncate = (text, len) => {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '...' : text
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/consultation/list')
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

const handleReply = (row) => {
  currentItem.value = row
  replyContent.value = row.reply || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  replying.value = true
  try {
    await request.put(`/user/consultation/reply/${currentItem.value.id}`, {
      reply: replyContent.value
    })
    ElMessage.success('回复成功')
    dialogVisible.value = false
    await fetchList()
  } catch {
    // Error handled by interceptor
  } finally {
    replying.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.question-section,
.reply-section {
  padding: 0 4px;
}

.question-section h4,
.reply-section h4 {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
}

.question-content {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>
