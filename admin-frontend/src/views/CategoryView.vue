<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '@/api/category'
import type { Category } from '@/api/category'

const categories = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<Partial<Category>>({ name: '', parentId: 0, sort: 0, imageUrl: '', status: 1 })

const treeData = computed(() => {
  const map = new Map<number, Category & { children: Category[] }>()
  const roots: (Category & { children: Category[] })[] = []
  const sorted = [...categories.value].sort((a, b) => (a.parentId || 0) - (b.parentId || 0) || a.sort - b.sort)
  sorted.forEach(item => {
    const node = { ...item, children: [] }
    map.set(node.id, node)
    if (!node.parentId || node.parentId === 0) { roots.push(node) }
    else { const parent = map.get(node.parentId); if (parent) parent.children.push(node) }
  })
  return roots
})

async function loadData() {
  loading.value = true
  try { categories.value = await getCategoryList() } catch {} finally { loading.value = false }
}

function openAddDialog(parentId?: number) {
  isEdit.value = false; form.value = { name: '', parentId: parentId || 0, sort: 0, imageUrl: '', status: 1 }; dialogVisible.value = true
}
function openEditDialog(item: Category) {
  isEdit.value = true; form.value = { ...item }; dialogVisible.value = true
}
async function submitForm() {
  if (!form.value.name) { ElMessage.warning('请输入分类名称'); return }
  try {
    if (isEdit.value) { await updateCategory(form.value); ElMessage.success('更新成功') }
    else { await addCategory(form.value); ElMessage.success('添加成功') }
    dialogVisible.value = false; await loadData()
  } catch {}
}
async function handleDelete(item: Category) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${item.name}」吗？`, '确认删除', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteCategory(item.id); ElMessage.success('删除成功'); await loadData()
  } catch {}
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header"><h2>分类管理</h2></div>
    <el-card>
      <template #header>
        <div class="card-header"><span>商品分类</span><el-button type="primary" size="small" @click="openAddDialog()">新增分类</el-button></div>
      </template>
      <el-table :data="treeData" v-loading="loading" stripe row-key="id" style="width:100%">
        <el-table-column prop="name" label="分类名称" min-width="200">
          <template #default="{ row }">
            <span :style="{ paddingLeft: row.parentId ? '24px' : '0', fontWeight: !row.parentId ? 600 : 400 }">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="primary" size="small" link @click="openAddDialog(row.id)">添加子分类</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="父级分类">
          <el-select v-model="form.parentId" placeholder="顶级分类" clearable>
            <el-option label="顶级分类" :value="0" />
            <el-option v-for="item in categories.filter(c => !c.parentId)" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitForm">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
