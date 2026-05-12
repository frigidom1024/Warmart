<template>
  <div class="banner-view">
    <div class="page-header">
      <h3>轮播管理</h3>
      <el-button type="primary" @click="handleAdd">添加 Banner</el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="title" label="标题" min-width="150" />
        <el-table-column label="图片" width="120" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              style="width: 60px; height: 40px"
              fit="cover"
              :preview-src-list="[row.imageUrl]"
              preview-teleported
            />
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="linkUrl" label="链接 URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '显示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ row.createdAt }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row, $index }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            <el-button link size="small" :disabled="$index === 0" @click="handleMove(row, 'up')">上移</el-button>
            <el-button link size="small" :disabled="$index === list.length - 1" @click="handleMove(row, 'down')">下移</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑 Banner' : '添加 Banner'"
      width="520px"
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
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="请输入图片 URL" />
        </el-form-item>
        <el-form-item label="链接地址" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="请输入跳转链接" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="显示"
            inactive-text="隐藏"
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
  imageUrl: '',
  linkUrl: '',
  sort: 0,
  status: 1
}

const form = reactive({ ...defaultForm })

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请输入图片地址', trigger: 'blur' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await request.get('/product/banner/list')
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
    imageUrl: row.imageUrl,
    linkUrl: row.linkUrl,
    sort: row.sort,
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
      await request.put('/product/banner/update', form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/product/banner/add', form)
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
  ElMessageBox.confirm('确定要删除该 Banner 吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await request.delete(`/product/banner/delete/${row.id}`)
      ElMessage.success('删除成功')
      await fetchList()
    } catch {
      // Error handled by interceptor
    }
  }).catch(() => {})
}

const handleMove = async (row, direction) => {
  const index = list.value.findIndex(item => item.id === row.id)
  if (direction === 'up' && index > 0) {
    const temp = list.value[index - 1]
    list.value[index - 1] = list.value[index]
    list.value[index] = temp
  } else if (direction === 'down' && index < list.value.length - 1) {
    const temp = list.value[index + 1]
    list.value[index + 1] = list.value[index]
    list.value[index] = temp
  }
  // Persist sort order after move
  try {
    const sortedIds = list.value.map((item, i) => ({ id: item.id, sort: i }))
    for (const item of sortedIds) {
      await request.put('/product/banner/update', { id: item.id, sort: item.sort })
    }
    ElMessage.success('排序已更新')
  } catch {
    await fetchList()
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
