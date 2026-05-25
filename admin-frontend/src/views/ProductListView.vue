<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminProductList, addProduct, updateProduct, deleteProduct } from '@/api/product'
import { getCategoryList } from '@/api/category'
import type { Product, ProductSpec, ProductImage } from '@/api/product'
import type { Category } from '@/api/category'
import { useUserStore } from '@/stores/user'

const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({ categoryId: undefined as number | undefined, status: undefined as number | undefined, page: 1, size: 10 })
const dialogVisible = ref(false)
const isEdit = ref(false)
const dialogMounted = ref(0) // increment to restart animations

const userStore = useUserStore()
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${userStore.token}` }))

const form = ref<Partial<Product>>({
  name: '', categoryId: 0, price: 0, originalPrice: 0, stock: 0,
  tag: '', description: '', mainImage: '', status: 1, isRecommend: 0, sales: 0,
  specList: [], imageList: []
})

async function loadProducts() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.categoryId) params.categoryId = query.value.categoryId
    if (query.value.status !== undefined) params.status = query.value.status
    const res = await getAdminProductList(params)
    products.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

async function loadCategories() {
  try { categories.value = await getCategoryList() } catch {}
}

function handleSearch() { query.value.page = 1; loadProducts() }
function handleReset() { query.value.categoryId = undefined; query.value.status = undefined; query.value.page = 1; loadProducts() }

function openAddDialog() {
  isEdit.value = false
  form.value = { name: '', categoryId: 0, price: 0, originalPrice: 0, stock: 0, tag: '', description: '', mainImage: '', status: 1, isRecommend: 0, sales: 0, specList: [], imageList: [] }
  dialogMounted.value++
  dialogVisible.value = true
}

function openEditDialog(item: Product) {
  isEdit.value = true
  form.value = { ...item, specList: item.specList ? JSON.parse(JSON.stringify(item.specList)) : [], imageList: item.imageList ? JSON.parse(JSON.stringify(item.imageList)) : [] }
  dialogMounted.value++
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.name) { ElMessage.warning('请输入商品名称'); return }
  try {
    if (isEdit.value) { await updateProduct(form.value); ElMessage.success('更新成功') }
    else { await addProduct(form.value); ElMessage.success('添加成功') }
    dialogVisible.value = false; await loadProducts()
  } catch {}
}

async function handleDelete(item: Product) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${item.name}」吗？`, '确认删除', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' })
    await deleteProduct(item.id); ElMessage.success('删除成功'); await loadProducts()
  } catch {}
}

function handlePageChange(page: number) { query.value.page = page; loadProducts() }

const categoryMap = computed(() => {
  const map: Record<number, string> = {}
  categories.value.forEach(c => { map[c.id] = c.name })
  return map
})

const categoryTreeData = computed(() => {
  const map = new Map<number, any>()
  const roots: any[] = []
  const sorted = [...categories.value].sort((a, b) => (a.parentId || 0) - (b.parentId || 0) || a.sort - b.sort)
  sorted.forEach(c => {
    const node = { value: c.id, label: c.name, children: [] as any[] }
    map.set(c.id, node)
    if (!c.parentId || c.parentId === 0) { roots.push(node) }
    else { const parent = map.get(c.parentId); if (parent) parent.children.push(node) }
  })
  return roots
})

// ─── Image upload handlers ───

function handleMainImageUploadSuccess(response: any) {
  if (response.code === 200) {
    form.value.mainImage = response.data
    ElMessage.success('主图上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleGalleryUploadSuccess(response: any) {
  if (response.code === 200) {
    if (!form.value.imageList) form.value.imageList = []
    form.value.imageList.push({ url: response.data, sort: form.value.imageList.length })
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function removeGalleryImage(idx: number) {
  form.value.imageList?.splice(idx, 1)
}

function beforeUpload(file: File) {
  const validExts = ['.png', '.jpg', '.jpeg', '.webp', '.svg']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!validExts.includes(ext)) {
    ElMessage.error('仅支持 PNG、JPG、WebP、SVG 格式')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 5MB')
    return false
  }
  return true
}

// ─── Spec handlers ───

function addSpec() {
  if (!form.value.specList) form.value.specList = []
  form.value.specList.push({ specName: '', specValue: '', extraPrice: 0, stock: 0, sort: form.value.specList.length })
}

function removeSpec(idx: number) {
  form.value.specList?.splice(idx, 1)
}

onMounted(() => { loadCategories(); loadProducts() })
</script>

<template>
  <div>
    <div class="page-header"><h2>商品管理</h2></div>

    <!-- Search & Table -->
    <el-card shadow="never" class="list-card">
      <div class="search-bar">
        <el-select v-model="query.categoryId" placeholder="商品分类" clearable class="search-select" @change="handleSearch">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="上下架状态" clearable class="search-select" @change="handleSearch">
          <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <span class="search-spacer" />
        <el-button type="primary" class="btn-add" @click="openAddDialog">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" style="margin-right:4px"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新增商品
        </el-button>
      </div>
      <el-table :data="products" v-loading="loading" stripe style="width:100%" size="small">
        <el-table-column prop="id" label="ID" width="56" />
        <el-table-column label="图片" width="64">
          <template #default="{ row }">
            <el-image :src="row.mainImage" fit="cover" style="width:36px;height:36px;border-radius:4px">
              <template #error><div class="img-error">无</div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="分类" width="100"><template #default="{ row }">{{ categoryMap[row.categoryId] || '-' }}</template></el-table-column>
        <el-table-column prop="price" label="价格" width="90" align="right"><template #default="{ row }">¥{{ row.price }}</template></el-table-column>
        <el-table-column prop="stock" label="库存" width="70" align="center" />
        <el-table-column prop="sales" label="销量" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center"><template #default="{ row }"><span class="status-dot" :class="row.status === 1 ? 'on' : 'off'" />{{ row.status === 1 ? '上架' : '下架' }}</template></el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="130" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <span class="action-divider">|</span>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="prev, pager, next, total" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- ─── Product Form Dialog ─── -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '新增商品'"
      width="840px"
      class="product-dialog"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="dialog-body-scroll">
        <el-form :model="form" label-width="0">
          <!-- Section: Basic Info -->
          <div class="form-section">
            <div class="form-section-head">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
              <span>基本信息</span>
            </div>
            <div class="form-section-body">
              <div class="form-grid-2">
                <div class="form-field">
                  <label>商品名称</label>
                  <el-input v-model="form.name" placeholder="输入商品名称" />
                </div>
                <div class="form-field">
                  <label>分类</label>
                  <el-tree-select
                    v-model="form.categoryId"
                    :data="categoryTreeData"
                    placeholder="选择分类"
                    clearable
                    check-strictly
                    filterable
                  />
                </div>
              </div>
              <div class="form-grid-3">
                <div class="form-field">
                  <label>价格</label>
                  <el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" placeholder="0.00" />
                </div>
                <div class="form-field">
                  <label>原价 <span class="label-optional">(选填)</span></label>
                  <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width:100%" placeholder="0.00" />
                </div>
                <div class="form-field">
                  <label>库存</label>
                  <el-input-number v-model="form.stock" :min="0" style="width:100%" placeholder="0" />
                </div>
              </div>
              <div class="form-field" style="max-width:260px">
                <label>标签 <span class="label-optional">(选填)</span></label>
                <el-input v-model="form.tag" placeholder="如: 新品、热销" />
              </div>
            </div>
          </div>

          <!-- Section: Images -->
          <div class="form-section">
            <div class="form-section-head">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><polyline points="21 15 16 10 5 21"/></svg>
              <span>商品图片</span>
            </div>
            <div class="form-section-body">
              <div class="form-field">
                <label>封面主图</label>
                <div class="main-image-area">
                  <div v-if="form.mainImage" class="main-image-preview">
                    <el-image :src="form.mainImage" fit="contain" />
                    <button class="main-image-clear" @click="form.mainImage = ''" title="移除图片">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
                  </div>
                  <el-upload
                    action="/api/product/admin/upload-image"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :on-success="handleMainImageUploadSuccess"
                    :before-upload="beforeUpload"
                    accept=".png,.jpg,.jpeg,.webp,.svg"
                    class="main-image-upload"
                  >
                    <div class="upload-zone" :class="{ 'has-image': !!form.mainImage }">
                      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
                      <span>{{ form.mainImage ? '更换图片' : '上传主图' }}</span>
                    </div>
                  </el-upload>
                </div>
              </div>
              <div class="form-field">
                <label>图库 <span class="label-optional">(选填)</span></label>
                <div class="gallery-area">
                  <div v-for="(img, idx) in form.imageList" :key="idx" class="gallery-item">
                    <el-image :src="img.url" fit="cover" />
                    <button class="gallery-item-remove" @click="removeGalleryImage(idx)">
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
                  </div>
                  <el-upload
                    action="/api/product/admin/upload-image"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :on-success="handleGalleryUploadSuccess"
                    :before-upload="beforeUpload"
                    accept=".png,.jpg,.jpeg,.webp,.svg"
                  >
                    <div class="gallery-add-btn">
                      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                    </div>
                  </el-upload>
                </div>
              </div>
            </div>
          </div>

          <!-- Section: Specs -->
          <div class="form-section">
            <div class="form-section-head">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/></svg>
              <span>规格参数</span>
              <span class="label-optional" style="margin-left:4px">(选填)</span>
            </div>
            <div class="form-section-body">
              <div v-if="form.specList && form.specList.length > 0" class="spec-table">
                <div class="spec-table-head">
                  <span style="flex:0 0 100px">规格名</span>
                  <span style="flex:0 0 130px">规格值</span>
                  <span style="flex:0 0 110px">加价</span>
                  <span style="flex:0 0 100px">库存</span>
                  <span style="flex:0 0 36px" />
                </div>
                <div v-for="(spec, idx) in form.specList" :key="idx" class="spec-table-row">
                  <div style="flex:0 0 100px"><el-input v-model="spec.specName" placeholder="如: 颜色" size="small" /></div>
                  <div style="flex:0 0 130px"><el-input v-model="spec.specValue" placeholder="如: 红色" size="small" /></div>
                  <div style="flex:0 0 110px"><el-input-number v-model="spec.extraPrice" :min="0" :precision="2" size="small" style="width:100%" placeholder="0.00" /></div>
                  <div style="flex:0 0 100px"><el-input-number v-model="spec.stock" :min="0" size="small" style="width:100%" placeholder="0" /></div>
                  <div style="flex:0 0 36px;display:flex;justify-content:center">
                    <button class="spec-remove-btn" @click="removeSpec(idx)">
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
                  </div>
                </div>
              </div>
              <div v-else class="spec-empty">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" style="color:#d0d5dd"><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/></svg>
                <span>暂无规格，点击下方按钮添加</span>
              </div>
              <el-button size="small" class="spec-add-btn" @click="addSpec">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                添加规格
              </el-button>
            </div>
          </div>

          <!-- Section: Other -->
          <div class="form-section">
            <div class="form-section-head">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/></svg>
              <span>其他设置</span>
            </div>
            <div class="form-section-body">
              <div class="form-field">
                <label>商品描述 <span class="label-optional">(选填)</span></label>
                <el-input v-model="form.description" type="textarea" :rows="3" placeholder="输入商品描述…" />
              </div>
              <div class="form-grid-3">
                <div class="form-field">
                  <label>上架状态</label>
                  <el-radio-group v-model="form.status" class="status-radio-group">
                    <el-radio-button :value="1">上架</el-radio-button>
                    <el-radio-button :value="0">下架</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="form-field">
                  <label>推荐商品</label>
                  <el-switch v-model="form.isRecommend" :active-value="1" :inactive-value="0" class="recommend-switch" />
                </div>
              </div>
            </div>
          </div>
        </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" style="margin-right:4px"><polyline points="20 6 9 17 4 12"/></svg>
            {{ isEdit ? '保存修改' : '确认添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ═══════════════════════════════════════
   Global / Page
   ═══════════════════════════════════════ */
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 22px; font-weight: 700; color: #0f172a; letter-spacing: -0.3px; }

.list-card { border-radius: 10px; border: 1px solid #eef0f2; }

/* ═══════════════════════════════════════
   Search bar
   ═══════════════════════════════════════ */
.search-bar { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; align-items: center; }
.search-select { width: 150px; }
.search-spacer { flex:1; }
.btn-add { font-weight: 600; display: inline-flex; align-items: center; }

/* ═══════════════════════════════════════
   Table
   ═══════════════════════════════════════ */
.img-error { width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; background: #f1f3f5; font-size: 11px; color: #aaa; border-radius: 4px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.action-divider { color: #e0e2e6; font-size: 12px; }

.status-dot { display: inline-block; width: 6px; height: 6px; border-radius: 50%; margin-right: 5px; vertical-align: middle; }
.status-dot.on { background: #22c55e; box-shadow: 0 0 0 2px rgba(34,197,94,0.2); }
.status-dot.off { background: #ef4444; box-shadow: 0 0 0 2px rgba(239,68,68,0.2); }

/* ═══════════════════════════════════════
   Dialog
   ═══════════════════════════════════════ */
.product-dialog :deep(.el-dialog__header) { padding: 20px 28px 0; }
.product-dialog :deep(.el-dialog__title) { font-size: 17px; font-weight: 700; color: #0f172a; letter-spacing: -0.2px; }
.product-dialog :deep(.el-dialog__body) { padding: 16px 28px 8px; max-height: 70vh; overflow: hidden; }
.product-dialog :deep(.el-dialog__footer) { padding: 12px 28px 20px; border-top: 1px solid #f1f3f5; }

.dialog-body-scroll { max-height: calc(70vh - 100px); overflow-y: auto; padding-right: 4px; }
.dialog-body-scroll::-webkit-scrollbar { width: 4px; }
.dialog-body-scroll::-webkit-scrollbar-thumb { background: #d0d5dd; border-radius: 4px; }

.dialog-footer { display: flex; justify-content: flex-end; gap: 8px; }

/* ═══════════════════════════════════════
   Form Section
   ═══════════════════════════════════════ */
.form-section {
  margin-bottom: 20px;
  border: 1px solid #eef0f2;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
  transition: box-shadow 0.2s;
}
.form-section:hover { box-shadow: 0 1px 4px rgba(0,0,0,0.04); }

.form-section-head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f8f9fb;
  border-bottom: 1px solid #eef0f2;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}
.form-section-head svg { color: #64748b; flex-shrink: 0; }

.form-section-body { padding: 16px; display: flex; flex-direction: column; gap: 14px; }

.form-field { display: flex; flex-direction: column; gap: 6px; }
.form-field > label { font-size: 13px; font-weight: 600; color: #334155; }

.label-optional { font-weight: 400; color: #94a3b8; font-size: 12px; }

.form-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.form-grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 14px; }

/* ═══════════════════════════════════════
   Image upload
   ═══════════════════════════════════════ */
.main-image-area { display: flex; gap: 12px; align-items: flex-start; }

.main-image-preview {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 8px;
  border: 1px solid #e2e6ea;
  overflow: hidden;
  flex-shrink: 0;
  background: #f8f9fb;
}
.main-image-preview .el-image { width: 100%; height: 100%; }

.main-image-clear {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.main-image-preview:hover .main-image-clear { opacity: 1; }

.upload-zone {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border: 2px dashed #d0d5dd;
  border-radius: 8px;
  background: #fafbfc;
  cursor: pointer;
  transition: all 0.2s;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}
.upload-zone:hover { border-color: #2563eb; color: #2563eb; background: #f0f4ff; }
.upload-zone svg { color: #94a3b8; transition: color 0.2s; }
.upload-zone:hover svg { color: #2563eb; }
.upload-zone.has-image { border-color: #2563eb; background: #f0f4ff; color: #2563eb; }

/* Gallery */
.gallery-area { display: flex; flex-wrap: wrap; gap: 8px; }
.gallery-item { position: relative; width: 72px; height: 72px; border-radius: 8px; border: 1px solid #e2e6ea; overflow: hidden; flex-shrink: 0; background: #f8f9fb; transition: transform 0.15s; }
.gallery-item:hover { transform: translateY(-1px); }
.gallery-item .el-image { width: 100%; height: 100%; }

.gallery-item-remove {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.gallery-item:hover .gallery-item-remove { opacity: 1; }

.gallery-add-btn {
  width: 72px;
  height: 72px;
  border: 2px dashed #d0d5dd;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #94a3b8;
  background: #fafbfc;
}
.gallery-add-btn:hover { border-color: #2563eb; color: #2563eb; background: #f0f4ff; }

/* ═══════════════════════════════════════
   Specs
   ═══════════════════════════════════════ */
.spec-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px 0;
  color: #94a3b8;
  font-size: 13px;
}
.spec-table { display: flex; flex-direction: column; border: 1px solid #eef0f2; border-radius: 8px; overflow: hidden; }
.spec-table-head { display: flex; padding: 8px 12px; background: #f8f9fb; border-bottom: 1px solid #eef0f2; font-size: 12px; font-weight: 600; color: #64748b; }
.spec-table-row { display: flex; align-items: center; padding: 8px 12px; border-bottom: 1px solid #f1f3f5; transition: background 0.15s; }
.spec-table-row:last-child { border-bottom: none; }
.spec-table-row:hover { background: #fafbfc; }

.spec-remove-btn {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: none;
  background: transparent;
  color: #94a3b8;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s;
}
.spec-remove-btn:hover { background: #fef2f2; color: #ef4444; }

.spec-add-btn { margin-top: 8px; display: inline-flex; align-items: center; gap: 4px; }

/* Status radio */
.status-radio-group :deep(.el-radio-button__inner) { padding: 8px 20px; font-size: 13px; }

/* Recommend switch */
.recommend-switch :deep(.el-switch__core) { width: 44px !important; }
</style>
