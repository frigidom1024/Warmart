<template>
  <div class="feedback-page">
    <div class="page-header">
      <h2>意见反馈</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">
        新建反馈
      </el-button>
    </div>

    <div v-loading="loading">
      <div v-if="feedbackList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无反馈记录" />
      </div>

      <el-card
        v-for="item in feedbackList"
        :key="item.id"
        class="feedback-card"
        shadow="hover"
      >
        <div class="feedback-header">
          <el-tag :type="feedbackTypeTag(item.type)" size="small">
            {{ feedbackTypeLabel(item.type) }}
          </el-tag>
          <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small" effect="plain">
            {{ item.status === 1 ? '已处理' : '待处理' }}
          </el-tag>
          <span class="feedback-time">{{ formatTime(item.createdAt) }}</span>
        </div>
        <div class="feedback-content">
          {{ item.content }}
        </div>
        <div v-if="item.reply" class="feedback-reply">
          <span class="reply-label">回复：</span>
          {{ item.reply }}
        </div>
      </el-card>
    </div>

    <!-- Add Feedback Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="新建反馈"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="feedbackForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="反馈类型" prop="type">
          <el-select v-model="feedbackForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="建议" value="suggestion" />
            <el-option label="投诉" value="complaint" />
            <el-option label="咨询" value="question" />
          </el-select>
        </el-form-item>

        <el-form-item label="反馈内容" prop="content">
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="5"
            placeholder="请详细描述您的反馈意见"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="联系方式">
          <el-input
            v-model="feedbackForm.contact"
            placeholder="手机号或邮箱（选填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getFeedbackList, addFeedback } from '@/api/user'

const loading = ref(false)
const submitting = ref(false)
const feedbackList = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const feedbackForm = reactive({
  type: 'suggestion',
  content: '',
  contact: ''
})

const formRules = {
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    { min: 5, message: '反馈内容至少5个字符', trigger: 'blur' }
  ]
}

const feedbackTypeMap = {
  suggestion: { label: '建议', tag: '' },
  complaint: { label: '投诉', tag: 'danger' },
  question: { label: '咨询', tag: 'primary' }
}

const feedbackTypeLabel = (type) => feedbackTypeMap[type]?.label || type
const feedbackTypeTag = (type) => feedbackTypeMap[type]?.tag || ''

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
}

const fetchFeedbackList = async () => {
  loading.value = true
  try {
    const res = await getFeedbackList()
    const data = res.data || res
    feedbackList.value = Array.isArray(data) ? data : (data.records || [])
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  feedbackForm.type = 'suggestion'
  feedbackForm.content = ''
  feedbackForm.contact = ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      type: feedbackForm.type,
      content: feedbackForm.content
    }
    if (feedbackForm.contact) {
      payload.contact = feedbackForm.contact
    }

    const res = await addFeedback(payload)
    if (res.code === 200) {
      ElMessage.success('反馈已提交，感谢您的意见')
      dialogVisible.value = false
      await fetchFeedbackList()
    }
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchFeedbackList()
})
</script>

<style scoped>
.feedback-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.feedback-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.feedback-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.feedback-time {
  font-size: 13px;
  color: #999;
  margin-left: auto;
}

.feedback-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  padding: 8px 0;
}

.feedback-reply {
  font-size: 14px;
  color: #606266;
  background: #f5f7fa;
  padding: 10px 12px;
  border-radius: 6px;
  margin-top: 8px;
  line-height: 1.5;
}

.reply-label {
  color: #409eff;
  font-weight: 500;
}

.empty-state {
  padding: 60px 0;
}
</style>
