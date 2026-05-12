<template>
  <div class="consultation-page">
    <div class="page-header">
      <h2>售前咨询</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">
        新咨询
      </el-button>
    </div>

    <div v-loading="loading">
      <div v-if="consultationList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无咨询记录" />
      </div>

      <el-card
        v-for="item in consultationList"
        :key="item.id"
        class="consultation-card"
        shadow="hover"
      >
        <div class="consultation-header">
          <span class="consultation-product">
            商品：{{ item.productName || '未知商品' }}
          </span>
          <span class="consultation-date">{{ formatDate(item.createdAt) }}</span>
        </div>

        <div class="consultation-question">
          <span class="q-label">咨询内容：</span>
          {{ item.content || item.question }}
        </div>

        <div v-if="item.reply" class="consultation-answer">
          <span class="a-label">回复：</span>
          {{ item.reply }}
        </div>

        <div v-else class="consultation-pending">
          暂无回复，请耐心等待
        </div>
      </el-card>
    </div>

    <!-- Add Consultation Dialog -->
    <el-dialog
      v-model="dialogVisible"
      title="新咨询"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="consultationForm"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="商品ID" prop="productId">
          <el-input-number
            v-model="consultationForm.productId"
            :min="1"
            placeholder="请输入商品ID"
            style="width: 100%"
          />
          <div class="form-tip">请输入您要咨询的商品ID</div>
        </el-form-item>

        <el-form-item label="咨询内容" prop="content">
          <el-input
            v-model="consultationForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入您的咨询内容"
            maxlength="500"
            show-word-limit
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
import { getMyConsultations, addConsultation } from '@/api/user'

const loading = ref(false)
const submitting = ref(false)
const consultationList = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const consultationForm = reactive({
  productId: undefined,
  content: ''
})

const formRules = {
  productId: [
    { required: true, message: '请输入商品ID', trigger: 'blur' },
    { type: 'number', min: 1, message: '请输入有效的商品ID', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入咨询内容', trigger: 'blur' },
    { min: 5, message: '咨询内容至少5个字符', trigger: 'blur' }
  ]
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

const fetchConsultations = async () => {
  loading.value = true
  try {
    const res = await getMyConsultations()
    const data = res.data || res
    consultationList.value = Array.isArray(data) ? data : (data.records || [])
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  consultationForm.productId = undefined
  consultationForm.content = ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await addConsultation({
      productId: consultationForm.productId,
      content: consultationForm.content
    })
    if (res.code === 200) {
      ElMessage.success('咨询已提交')
      dialogVisible.value = false
      await fetchConsultations()
    }
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchConsultations()
})
</script>

<style scoped>
.consultation-page {
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

.consultation-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.consultation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.consultation-product {
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}

.consultation-date {
  font-size: 13px;
  color: #999;
}

.consultation-question {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  padding: 8px 0;
}

.q-label {
  font-weight: 600;
  color: #606266;
}

.consultation-answer {
  font-size: 14px;
  color: #606266;
  background: #f0f9eb;
  padding: 10px 12px;
  border-radius: 6px;
  margin-top: 8px;
  line-height: 1.5;
}

.a-label {
  color: #67c23a;
  font-weight: 500;
}

.consultation-pending {
  font-size: 13px;
  color: #909399;
  margin-top: 8px;
  padding: 8px 0;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.empty-state {
  padding: 60px 0;
}
</style>
