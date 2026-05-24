<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getFeedbackList, replyFeedback } from '@/api/feedback'
import type { Feedback } from '@/api/feedback'

const feedbacks = ref<Feedback[]>([])
const loading = ref(false)
const replyDialogVisible = ref(false)
const currentItem = ref<Feedback | null>(null)
const replyForm = ref({ replyContent: '' })

const typeLabels: Record<string, string> = { suggestion: '建议', complaint: '投诉', question: '咨询', other: '其他' }
const typeColors: Record<string, string> = { suggestion: '', complaint: 'danger', question: 'primary', other: 'info' }

async function loadData() {
  loading.value = true
  try { feedbacks.value = await getFeedbackList() } catch {} finally { loading.value = false }
}
function openReplyDialog(item: Feedback) { currentItem.value = item; replyForm.value.replyContent = item.reply || ''; replyDialogVisible.value = true }
async function submitReply() {
  if (!currentItem.value || !replyForm.value.replyContent) { ElMessage.warning('请输入回复内容'); return }
  try {
    await replyFeedback(currentItem.value.id, replyForm.value.replyContent)
    ElMessage.success('回复成功'); replyDialogVisible.value = false; await loadData()
  } catch {}
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header"><h2>反馈管理</h2></div>
    <el-card>
      <el-table :data="feedbacks" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="type" label="类型" width="100"><template #default="{ row }"><el-tag :type="typeColors[row.type] || ''" size="small">{{ typeLabels[row.type] || row.type }}</el-tag></template></el-table-column>
        <el-table-column prop="content" label="反馈内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="reply" label="回复" min-width="200" show-overflow-tooltip><template #default="{ row }"><span v-if="row.reply" class="reply-text">{{ row.reply }}</span><el-tag v-else size="small" type="warning">未回复</el-tag></template></el-table-column>
        <el-table-column prop="createdTime" label="提交时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button type="primary" size="small" :disabled="row.status === 1" @click="openReplyDialog(row)">回复</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="replyDialogVisible" title="回复反馈" width="550px">
      <div v-if="currentItem" class="reply-dialog-content">
        <div class="original-feedback"><label>用户反馈：</label><p>{{ currentItem.content }}</p></div>
        <el-form><el-form-item label="回复内容"><el-input v-model="replyForm.replyContent" type="textarea" :rows="4" /></el-form-item></el-form>
      </div>
      <template #footer><el-button @click="replyDialogVisible = false">取消</el-button><el-button type="primary" @click="submitReply">确认回复</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.reply-dialog-content { padding: 0 8px; }
.original-feedback { background: #f5f7fa; padding: 12px 16px; border-radius: 6px; margin-bottom: 20px; }
.original-feedback label { font-size: 13px; color: #909399; }
.original-feedback p { margin: 6px 0 0; font-size: 14px; color: #303133; }
.reply-text { font-size: 13px; color: #606266; }
</style>
