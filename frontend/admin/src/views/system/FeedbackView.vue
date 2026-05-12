<template>
  <div class="feedback-view">
    <div class="page-header">
      <h3>反馈管理</h3>
    </div>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="userId" label="用户 ID" width="90" align="center" />
        <el-table-column label="类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small">
              {{ row.type || '通用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ truncate(row.content, 60) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'pending' ? 'warning' : 'success'" size="small">
              {{ row.status === 'pending' ? '待处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              link
              size="small"
              @click="handleView(row)"
            >
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail / Reply Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="反馈详情"
      width="600px"
      :close-on-click-modal="false"
    >
      <template v-if="currentItem">
        <div class="detail-section">
          <div class="detail-row">
            <span class="detail-label">用户 ID：</span>
            <span>{{ currentItem.userId }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">类型：</span>
            <el-tag :type="typeTagType(currentItem.type)" size="small">
              {{ currentItem.type || '通用' }}
            </el-tag>
          </div>
          <div class="detail-row">
            <span class="detail-label">内容：</span>
            <div class="detail-content">{{ currentItem.content }}</div>
          </div>
          <div class="detail-row" v-if="currentItem.contact">
            <span class="detail-label">联系方式：</span>
            <span>{{ currentItem.contact }}</span>
          </div>
          <div class="detail-row" v-if="currentItem.status !== 'pending'">
            <span class="detail-label">回复内容：</span>
            <div class="detail-content">{{ currentItem.reply }}</div>
          </div>
          <div class="detail-row" v-if="currentItem.repliedAt">
            <span class="detail-label">回复时间：</span>
            <span>{{ currentItem.repliedAt }}</span>
          </div>
        </div>

        <el-divider />

        <div class="reply-section" v-if="currentItem.status === 'pending'">
          <h4>回复反馈</h4>
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="4"
            placeholder="请输入回复内容"
          />
          <el-button
            type="primary"
            style="margin-top: 12px"
            :loading="replying"
            @click="handleReply"
          >
            提交回复
          </el-button>
        </div>
        <el-alert
          v-else
          type="success"
          :closable="false"
          show-icon
          title="该反馈已处理"
        />
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

const typeTagType = (type) => {
  const map = { 投诉: 'danger', 建议: 'primary', 咨询: 'info', 售后: 'warning' }
  return map[type] || 'info'
}

const truncate = (text, len) => {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '...' : text
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/feedback/list')
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

const handleView = (row) => {
  currentItem.value = row
  replyContent.value = ''
  dialogVisible.value = true
}

const handleReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  replying.value = true
  try {
    await request.put(`/user/feedback/reply/${currentItem.value.id}`, {
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

.detail-section {
  padding: 0 4px;
}

.detail-row {
  margin-bottom: 14px;
  line-height: 1.6;
}

.detail-label {
  font-weight: 600;
  color: #606266;
}

.detail-content {
  margin-top: 4px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  white-space: pre-wrap;
  line-height: 1.6;
}

.reply-section h4 {
  margin: 0 0 10px;
  font-size: 15px;
  font-weight: 600;
}
</style>
