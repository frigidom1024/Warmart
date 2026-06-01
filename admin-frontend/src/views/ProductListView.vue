<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminProductList, addProduct, updateProduct, deleteProduct, getProductDetail, saveSpecGroups } from '@/api/product'
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
  specGroupsInput.value = []
  skuGrid.value = []
}

async function openEditDialog(item: Product) {
  isEdit.value = true
  dialogMounted.value++
  dialogVisible.value = true
  // Load full detail with specs and images
  try {
    const detail = await getProductDetail(item.id)
    // 加载规格数据
    if ((detail as any).specGroups?.length) {
      const groups = (detail as any).specGroups as any[]
      specGroupsInput.value = groups.map((g: any) => ({
        name: g.name,
        values: g.values.map((v: any) => v.value)
      }))
      regenerateSkuGrid()
      // 回填已有 SKU
      const skus = (detail as any).skuList || []
      if (skus.length) {
        const allGroups = groups
        for (const sku of skus) {
          const refs: string[] = []
          for (const g of allGroups) {
            const val = g.values.find((v: any) => sku.specValueIdList?.includes(v.id))
            if (val) refs.push(`${g.name}:${val.value}`)
          }
          const existing = skuGrid.value.find(s =>
            JSON.stringify(s.specValueRefs) === JSON.stringify(refs)
          )
          if (existing) {
            existing.price = sku.price
            existing.stock = sku.stock
            existing.image = sku.image || ''
            existing.enabled = sku.enabled !== false
          }
        }
      }
    } else {
      specGroupsInput.value = []
      skuGrid.value = []
    }
    form.value = { ...detail, specList: detail.specList || [], imageList: detail.imageList || [] }
  } catch {
    form.value = { ...item, specList: item.specList || [], imageList: item.imageList || [] }
  }
}

async function submitForm() {
  if (!form.value.name) { ElMessage.warning('请输入商品名称'); return }
  try {
    let productId = form.value.id || 0
    if (isEdit.value) { await updateProduct(form.value); productId = form.value.id!; ElMessage.success('更新成功') }
    else { const res = await addProduct(form.value); productId = (res as any)?.data || productId; ElMessage.success('添加成功') }
    // 保存规格数据
    if (specGroupsInput.value.length > 0 && specGroupsInput.value.some(d => d.name)) {
      const groups = specGroupsInput.value.map(g => ({
        name: g.name,
        values: g.values.map(v => ({ value: v }))
      }))
      const skus = skuGrid.value.map(s => ({
        specValueRefs: s.specValueRefs,
        price: s.price,
        stock: s.stock,
        image: s.image || null,
        enabled: s.enabled
      }))
      await saveSpecGroups(productId, groups, skus)
    }
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

// ─── Spec group editor ───
const specGroupsInput = ref<{ name: string; values: string[] }[]>([])
const skuGrid = ref<{ specValueRefs: string[]; price: number | null; stock: number; image: string; enabled: boolean }[]>([])
const batchPrice = ref<number | null>(null)
const batchStock = ref<number>(0)
const specPresets = ['颜色', '尺寸', '材质', '规格', '口味', '容量']

function addDimension() {
  specGroupsInput.value.push({ name: '', values: [] })
}

function removeDimension(idx: number) {
  specGroupsInput.value.splice(idx, 1)
  regenerateSkuGrid()
}

function addValue(groupIdx: number, value: string) {
  if (!value.trim()) return
  const v = value.trim()
  if (specGroupsInput.value[groupIdx].values.includes(v)) return
  specGroupsInput.value[groupIdx].values.push(v)
  regenerateSkuGrid()
}

function removeValue(groupIdx: number, valIdx: number) {
  specGroupsInput.value[groupIdx].values.splice(valIdx, 1)
  regenerateSkuGrid()
}

function regenerateSkuGrid() {
  const dims = specGroupsInput.value.filter(d => d.name && d.values.length > 0)
  if (dims.length === 0) { skuGrid.value = []; return }

  const combinations = cartesian(dims.map(d => d.values.map(v => `${d.name}:${v}`)))
  skuGrid.value = combinations.map(refs => {
    const existing = skuGrid.value.find(s =>
      JSON.stringify(s.specValueRefs) === JSON.stringify(refs)
    )
    return existing || { specValueRefs: refs, price: null, stock: 0, image: '', enabled: true }
  })
}

function cartesian<T>(arrays: T[][]): T[][] {
  if (arrays.length === 0) return [[]]
  const [first, ...rest] = arrays
  const restProduct = cartesian(rest)
  return first.flatMap(f => restProduct.map(r => [f, ...r]))
}

function setAllPrice(price: number | null) {
  skuGrid.value.forEach(s => { if (s.enabled) s.price = price })
}

function setAllStock(stock: number) {
  skuGrid.value.forEach(s => { if (s.enabled) s.stock = stock })
}

function enableAll() {
  skuGrid.value.forEach(s => { s.enabled = true })
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
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/></svg>
              <span>规格参数</span>
            </div>
            <div class="form-section-body">
              <!-- Dimensions -->
              <div v-for="(dim, di) in specGroupsInput" :key="di" style="margin-bottom:10px">
                <div style="display:flex;align-items:center;gap:6px;flex-wrap:wrap">
                  <el-input v-model="dim.name" placeholder="维度名称" size="small" style="width:100px" @input="regenerateSkuGrid" />
                  <el-tag v-for="(val, vi) in dim.values" :key="vi" closable size="small" @close="removeValue(di, vi)">{{ val }}</el-tag>
                  <input type="text" size="small" class="spec-value-input" placeholder="输入值后回车"
                    @keyup.enter="(e: any) => { if (e.target.value) { addValue(di, e.target.value); e.target.value = '' } }" />
                  <button style="border:none;background:none;color:#ef4444;cursor:pointer;font-size:16px;padding:0 4px" @click="removeDimension(di)" title="删除维度">×</button>
                </div>
              </div>
              <el-button size="small" @click="addDimension">+ 添加维度</el-button>

              <!-- SKU Grid -->
              <div v-if="skuGrid.length > 0" style="margin-top:12px;border:1px solid #eef0f2;border-radius:8px;overflow:hidden">
                <div style="display:flex;align-items:center;gap:6px;padding:8px 12px;background:#f8f9fb;border-bottom:1px solid #eef0f2;flex-wrap:wrap">
                  <span style="font-size:12px;color:#64748b;margin-right:8px">共 {{ skuGrid.filter(s => s.enabled).length }} 个组合</span>
                  <el-button size="small" @click="enableAll">全部启用</el-button>
                  <span style="margin-left:8px;font-size:12px;color:#909399">统一加价:</span>
                  <el-input-number v-model="batchPrice" :min="0" :precision="2" size="small" style="width:120px" />
                  <el-button size="small" @click="setAllPrice(batchPrice)">应用</el-button>
                  <span style="margin-left:8px;font-size:12px;color:#909399">统一库存:</span>
                  <el-input-number v-model="batchStock" :min="0" size="small" style="width:100px" />
                  <el-button size="small" @click="setAllStock(batchStock)">应用</el-button>
                </div>
                <div style="display:flex;flex-direction:column">
                  <div style="display:flex;align-items:center;padding:6px 12px;background:#f1f3f5;font-size:12px;font-weight:600;color:#64748b;border-bottom:1px solid #eef0f2">
                    <span style="flex:2;padding:2px 4px">组合</span>
                    <span style="flex:1;padding:2px 4px">加价(¥)</span>
                    <span style="flex:1;padding:2px 4px">库存</span>
                    <span style="flex:1;padding:2px 4px">启用</span>
                  </div>
                  <div v-for="(sku, si) in skuGrid" :key="si" style="display:flex;align-items:center;padding:6px 12px;border-bottom:1px solid #f8f9fb;transition:background 0.15s" :style="{ opacity: sku.enabled ? 1 : 0.5, background: sku.enabled ? '' : '#f8f9fb' }">
                    <span style="flex:2;padding:2px 4px;font-size:13px;font-weight:500;color:#334155">{{ sku.specValueRefs.join(' / ') }}</span>
                    <span style="flex:1;padding:2px 4px"><el-input-number v-model="sku.price" :min="0" :precision="2" size="small" style="width:100px" placeholder="基准价" /></span>
                    <span style="flex:1;padding:2px 4px"><el-input-number v-model="sku.stock" :min="0" size="small" style="width:80px" /></span>
                    <span style="flex:1;padding:2px 4px"><el-switch v-model="sku.enabled" size="small" /></span>
                  </div>
                </div>
              </div>
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

.spec-value-input {
  width: 120px;
  height: 24px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}
.spec-value-input:focus { border-color: #409eff; }

/* Status radio */
.status-radio-group :deep(.el-radio-button__inner) { padding: 8px 20px; font-size: 13px; }

/* Recommend switch */
.recommend-switch :deep(.el-switch__core) { width: 44px !important; }
</style>
