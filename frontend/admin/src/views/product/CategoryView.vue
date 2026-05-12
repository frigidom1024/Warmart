<template>
  <div class="category-view">
    <el-card shadow="never">
      <div class="table-header">
        <el-button type="primary" @click="handleAdd">添加分类</el-button>
      </div>

      <el-table
        :data="categoryTree"
        v-loading="loading"
        border
        stripe
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        style="width: 100%; margin-top: 16px"
      >
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="图标" width="80" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.icon"
              :src="row.icon"
              style="width: 28px; height: 28px"
              fit="contain"
            >
              <template #error>
                <el-icon :size="20"><Picture /></el-icon>
              </template>
            </el-image>
            <el-icon v-else :size="20" color="#ccc"><Picture /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '添加分类'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="formData.parentId" placeholder="选择上级分类（顶级分类选0）" style="width: 100%">
            <el-option :value="0" label="顶级分类" />
            <el-option
              v-for="cat in flatCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
              :disabled="isEdit && cat.id === editingId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图标URL" prop="icon">
          <el-input v-model="formData.icon" placeholder="请输入图标URL" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'

const loading = ref(false)
const submitLoading = ref(false)
const categories = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const getCategories = () => request.get('/product/category/list')
const addCategory = (data) => request.post('/product/category/add', data)
const updateCategory = (data) => request.put('/product/category/update', data)
const deleteCategory = (id) => request.delete(`/product/category/delete/${id}`)

const flatCategories = computed(() => {
  const flatten = (list) => {
    const result = []
    for (const item of list) {
      result.push(item)
      if (item.children && item.children.length > 0) {
        result.push(...flatten(item.children))
      }
    }
    return result
  }
  return flatten(categories.value)
})

const categoryTree = computed(() => {
  return buildTree(categories.value)
})

function buildTree(flatList) {
  if (!flatList || flatList.length === 0) return []
  const map = {}
  const tree = []

  flatList.forEach(item => {
    map[item.id] = { ...item, children: [] }
  })

  flatList.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(map[item.id])
    } else if (!item.parentId || item.parentId === 0) {
      tree.push(map[item.id])
    }
  })

  return tree
}

const defaultForm = () => ({
  name: '',
  parentId: 0,
  sort: 0,
  icon: ''
})

const formData = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

async function fetchCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    if (res.code === 200) {
      const data = res.data || []
      if (data.length > 0 && data[0].children !== undefined) {
        categories.value = flattenTree(data)
      } else {
        categories.value = data
      }
    } else {
      ElMessage.error(res.message || '获取分类列表失败')
    }
  } catch {
    ElMessage.error('获取分类列表失败')
  } finally {
    loading.value = false
  }
}

function flattenTree(tree) {
  const result = []
  const walk = (nodes) => {
    for (const node of nodes) {
      result.push({ id: node.id, name: node.name, parentId: node.parentId || 0, sort: node.sort, icon: node.icon, createTime: node.createTime })
      if (node.children && node.children.length > 0) {
        walk(node.children)
      }
    }
  }
  walk(tree)
  return result
}

function resetForm() {
  Object.assign(formData, defaultForm())
  editingId.value = null
  isEdit.value = false
}

function handleAdd() {
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  formData.name = row.name
  formData.parentId = row.parentId || 0
  formData.sort = row.sort ?? 0
  formData.icon = row.icon || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      name: formData.name,
      parentId: formData.parentId,
      sort: formData.sort,
      icon: formData.icon
    }

    if (isEdit.value) {
      payload.id = editingId.value
    }

    const api = isEdit.value ? updateCategory : addCategory
    const res = await api(payload)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
      dialogVisible.value = false
      fetchCategories()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除分类「${row.name}」？`,
      '警告',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'error'
      }
    )

    const res = await deleteCategory(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchCategories()
    } else if (res.code === 400 || (res.message && res.message.includes('子分类'))) {
      ElMessage.warning(res.message || '该分类存在子分类，无法删除')
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.table-header {
  display: flex;
  justify-content: flex-end;
}
</style>
