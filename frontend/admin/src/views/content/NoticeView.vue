<template>
  <div class="notice-view">
    <div class="page-header">
      <h3>公告管理</h3>
      <el-button type="primary" @click="handleAdd">添加公告</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagType(row.type)" size="small">
              {{ row.type === 'system' ? '系统' : row.type === 'promotion' ? '促销' : '活动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'published' ? 'success' : 'warning'" size="small">
              {{ row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="170" align="center">
          <template #default="{ row }">{{ row.updatedAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            <el-button
              :type="row.status === 'published' ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 'published' ? '下架' : '发布' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑公告' : '添加公告'"
      width="640px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
        label-position="left"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="系统" value="system" />
            <el-option label="促销" value="promotion" />
            <el-option label="活动" value="activity" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            active-value="published"
            inactive-value="draft"
            active-text="发布"
            inactive-text="草稿"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const formRef = ref(null)

const defaultForm = {
  title: '',
  type: 'system',
  content: '',
  status: 'draft'
}

const form = reactive({ ...defaultForm })

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

const typeTagType = (type) => {
  const map = { system: 'primary', promotion: 'danger', activity: 'success' }
  return map[type] || 'info'
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/notice/list')
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, defaultForm)
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    title: row.title,
    type: row.type,
    content: row.content,
    status: row.status
  })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put('/user/notice/update', form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/user/notice/add', form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await fetchList()
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该公告吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/user/notice/delete/${row.id}`)
      ElMessage.success('删除成功')
      await fetchList()
    } catch {
      // Error handled by interceptor
    }
  }).catch(() => {})
}

const handleToggleStatus = async (row) => {
  const newStatus = row.status === 'published' ? 'draft' : 'published'
  const actionText = newStatus === 'published' ? '发布' : '下架'
  try {
    await request.put('/user/notice/update', { id: row.id, status: newStatus })
    ElMessage.success(`${actionText}成功`)
    await fetchList()
  } catch {
    // Error handled by interceptor
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
</style>
