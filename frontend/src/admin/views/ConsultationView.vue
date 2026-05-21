<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConsultationList, replyConsultation } from '../api/consultation'
import type { Consultation } from '../api/consultation'

const consultations = ref<Consultation[]>([])
const loading = ref(false)
const replyDialogVisible = ref(false)
const currentItem = ref<Consultation | null>(null)
const replyForm = ref({ answer: '' })

async function loadData() {
  loading.value = true
  try {
    consultations.value = await getConsultationList()
  } catch {} finally {
    loading.value = false
  }
}

function openReplyDialog(item: Consultation) {
  currentItem.value = item
  replyForm.value.answer = item.answer || ''
  replyDialogVisible.value = true
}

async function submitReply() {
  if (!currentItem.value || !replyForm.value.answer) {
    ElMessage.warning('请输入回复内容')
    return
  }
  try {
    await replyConsultation(currentItem.value.id, replyForm.value.answer)
    ElMessage.success('回复成功')
    replyDialogVisible.value = false
    await loadData()
  } catch {}
}

function getStatusTag(status: number) {
  return status === 0 ? { type: 'warning', text: '待回复' }
    : status === 1 ? { type: 'success', text: '已回复' }
    : { type: 'info', text: '已关闭' }
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>咨询管理</h2>
    </div>
    <el-card>
      <el-table :data="consultations" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="80" />
        <el-table-column prop="question" label="咨询问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="answer" label="回复内容" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.answer" class="reply-text">{{ row.answer }}</span>
            <el-tag v-else size="small" type="warning">未回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTag(row.status).type" size="small">
              {{ getStatusTag(row.status).text }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="咨询时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary" size="small"
              :disabled="row.status !== 0"
              @click="openReplyDialog(row)"
            >
              回复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="replyDialogVisible" title="回复咨询" width="550px">
      <div v-if="currentItem" class="reply-dialog-content">
        <div class="original-question">
          <label>用户问题：</label>
          <p>{{ currentItem.question }}</p>
        </div>
        <el-form>
          <el-form-item label="回复内容">
            <el-input
              v-model="replyForm.answer"
              type="textarea"
              :rows="4"
              placeholder="请输入回复内容"
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">确认回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: var(--wa-text-primary, #303133); }
.reply-dialog-content { padding: 0 8px; }
.original-question { background: #f5f7fa; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px; }
.original-question label { font-size: 13px; color: #909399; }
.original-question p { margin: 6px 0 0; font-size: 14px; color: #303133; line-height: 1.6; }
.reply-text { font-size: 13px; color: #606266; }
</style>
