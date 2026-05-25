<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '@/api/category'
import type { Category } from '@/api/category'
import { useUserStore } from '@/stores/user'

const categories = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<Partial<Category>>({ name: '', parentId: 0, sort: 0, imageUrl: '', status: 1 })

const userStore = useUserStore()
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${userStore.token}` }))

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

function handleUploadSuccess(response: any) {
  if (response.code === 200) {
    form.value.imageUrl = response.data
    ElMessage.success('图标上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function beforeUpload(file: File) {
  const validExts = ['.svg', '.png', '.jpg', '.jpeg']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!validExts.includes(ext)) {
    ElMessage.error('仅支持 SVG、PNG、JPG 格式')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 2MB')
    return false
  }
  return true
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
        <el-table-column label="图标" width="72">
          <template #default="{ row }">
            <div class="table-icon-cell">
              <el-image v-if="row.imageUrl" :src="row.imageUrl" fit="contain" />
              <span v-else class="table-icon-none">—</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类名称" min-width="240">
          <template #default="{ row }">
            <div class="table-name-cell" :class="{ 'is-child': row.parentId }">
              <span v-if="row.parentId" class="tree-connector">└─</span>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="success" size="small" link @click="openAddDialog(row.id)">+ 子分类</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="500px" class="category-dialog">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="父级分类">
          <el-select v-model="form.parentId" placeholder="顶级分类" clearable>
            <el-option label="顶级分类" :value="0" />
            <el-option v-for="item in categories.filter(c => !c.parentId)" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="图标">
          <div class="icon-upload-wrapper">
            <template v-if="form.imageUrl">
              <div class="icon-upload-has">
                <div class="icon-upload-has-preview">
                  <el-image :src="form.imageUrl" fit="contain" />
                </div>
                <div class="icon-upload-has-actions">
                  <el-upload
                    action="/api/product/category/admin/upload-icon"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :on-success="handleUploadSuccess"
                    :before-upload="beforeUpload"
                    accept=".svg,.png,.jpg,.jpeg"
                    class="icon-upload-replace"
                  >
                    <el-button size="small">替换图标</el-button>
                  </el-upload>
                  <el-button size="small" type="danger" plain @click="form.imageUrl = ''">移除</el-button>
                </div>
              </div>
            </template>
            <el-upload
              v-else
              drag
              action="/api/product/category/admin/upload-icon"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :before-upload="beforeUpload"
              accept=".svg,.png,.jpg,.jpeg"
              class="icon-upload-drag"
            >
              <div class="icon-upload-placeholder">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                <span class="icon-upload-title">拖拽或点击上传图标</span>
                <span class="icon-upload-hint">支持 SVG / PNG / JPG，最大 2MB</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">禁用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="submitForm">确认</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 24px; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 600; color: #1d2129; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-header span { font-size: 15px; font-weight: 600; color: #1d2129; }

.category-dialog .el-dialog__body { padding-top: 16px; }

/* ── Table ── */
.table-icon-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: #d0d0d0;
  border-radius: 6px;
}
.table-icon-cell .el-image {
  width: 22px;
  height: 22px;
}
.table-icon-none {
  color: #c0c4cc;
  font-size: 13px;
}
.table-name-cell {
  display: flex;
  align-items: center;
  gap: 4px;
}
.table-name-cell.is-child {
  padding-left: 24px;
  color: #606266;
  font-size: 13px;
}
.tree-connector {
  color: #c0c4cc;
  font-size: 12px;
  margin-right: 2px;
}

/* ── Icon upload ── */
.icon-upload-wrapper {
  display: flex;
  flex-direction: column;
}
.icon-upload-has {
  display: flex;
  align-items: center;
  gap: 16px;
}
.icon-upload-has-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: #c9c9c9;
  border-radius: 8px;
  border: 1px solid #bbb;
}
.icon-upload-has-preview .el-image {
  width: 36px;
  height: 36px;
}
.icon-upload-has-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.icon-upload-drag .el-upload-dragger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 310px;
  padding: 16px;
  border: 2px dashed #d9d9d9;
  border-radius: 10px;
  background: #fafafa;
  transition: all 0.25s;
}
.icon-upload-drag .el-upload-dragger:hover {
  border-color: #409eff;
  background: #f0f7ff;
}
.icon-upload-drag .el-upload-dragger.is-dragover {
  border-color: #409eff;
  background: #ecf5ff;
}
.icon-upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
}
.icon-upload-placeholder svg {
  color: #c0c4cc;
}
.icon-upload-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}
.icon-upload-hint {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
