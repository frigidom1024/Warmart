<template>
  <div class="product-list-view">
    <el-card class="search-bar" shadow="never">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="商品名称" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="searchForm.categoryId" placeholder="选择分类" clearable style="width: 160px">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never" style="margin-top: 16px">
      <div class="table-header">
        <el-button type="primary" @click="handleAdd">添加商品</el-button>
      </div>

      <el-table
        :data="products"
        v-loading="loading"
        border
        stripe
        style="width: 100%; margin-top: 16px"
      >
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="图片" width="70" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.image || ''"
              style="width: 40px; height: 40px"
              fit="cover"
            >
              <template #error>
                <div class="image-placeholder">无图</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="价格" width="90">
          <template #default="{ row }">
            <span>¥{{ row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" align="center" />
        <el-table-column prop="sales" label="销量" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper" style="margin-top: 20px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchProducts"
          @current-change="fetchProducts"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品' : '添加商品'"
      width="800px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        style="padding-right: 20px"
      >
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入商品描述"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="价格" prop="price">
              <el-input-number
                v-model="formData.price"
                :precision="2"
                :min="0"
                :max="999999.99"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价" prop="originalPrice">
              <el-input-number
                v-model="formData.originalPrice"
                :precision="2"
                :min="0"
                :max="999999.99"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存" prop="stock">
              <el-input-number
                v-model="formData.stock"
                :min="0"
                :max="999999"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="formData.categoryId" placeholder="选择分类" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="商品图片" prop="image">
          <el-input v-model="formData.image" placeholder="请输入图片URL" />
        </el-form-item>
        <el-form-item label="推荐">
          <el-switch v-model="formData.isRecommended" />
        </el-form-item>

        <el-divider content-position="left">商品规格</el-divider>
        <div
          v-for="(spec, index) in formData.specs"
          :key="index"
          style="margin-bottom: 12px; padding: 12px; border: 1px solid #ebeef5; border-radius: 4px; background: #fafafa"
        >
          <el-row :gutter="12" align="middle">
            <el-col :span="6">
              <el-input v-model="spec.specName" placeholder="规格名" size="small" />
            </el-col>
            <el-col :span="6">
              <el-input v-model="spec.specValue" placeholder="规格值" size="small" />
            </el-col>
            <el-col :span="5">
              <el-input-number
                v-model="spec.extraPrice"
                :precision="2"
                :min="0"
                size="small"
                style="width: 100%"
                placeholder="加价"
              />
            </el-col>
            <el-col :span="5">
              <el-input-number
                v-model="spec.stock"
                :min="0"
                size="small"
                style="width: 100%"
                placeholder="库存"
              />
            </el-col>
            <el-col :span="2">
              <el-button type="danger" size="small" @click="removeSpec(index)">删除</el-button>
            </el-col>
          </el-row>
        </div>
        <el-button type="primary" size="small" plain @click="addSpec">+ 添加规格</el-button>

        <el-divider content-position="left">商品相册</el-divider>
        <div v-for="(img, index) in formData.images" :key="index" style="margin-bottom: 8px; display: flex; align-items: center; gap: 8px">
          <el-input v-model="img.url" placeholder="请输入图片URL" style="flex: 1" />
          <el-button type="danger" size="small" @click="formData.images.splice(index, 1)">删除</el-button>
        </div>
        <el-button type="primary" size="small" plain @click="addImage">+ 添加图片</el-button>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const submitLoading = ref(false)
const products = ref([])
const categories = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const searchForm = reactive({
  keyword: '',
  categoryId: null
})

const defaultForm = () => ({
  name: '',
  description: '',
  price: null,
  originalPrice: null,
  categoryId: null,
  stock: null,
  image: '',
  isRecommended: false,
  specs: [],
  images: []
})

const formData = reactive(defaultForm())

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const getCategories = () => request.get('/product/category/list')
const getProducts = (params) => request.get('/product/list', { params })
const getProductDetail = (id) => request.get(`/product/detail/${id}`)
const createProduct = (data) => request.post('/product/add', data)
const updateProduct = (data) => request.put('/product/update', data)
const deleteProduct = (id) => request.delete(`/product/delete/${id}`)

async function fetchProducts() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.categoryId) params.categoryId = searchForm.categoryId

    const res = await getProducts(params)
    if (res.code === 200) {
      products.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.message || '获取商品列表失败')
    }
  } catch (err) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await getCategories()
    if (res.code === 200) {
      categories.value = res.data || []
    }
  } catch {
    // silently fail, categories will be empty
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchProducts()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.categoryId = null
  currentPage.value = 1
  fetchProducts()
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

async function handleEdit(row) {
  isEdit.value = true
  editingId.value = row.id
  dialogVisible.value = true

  try {
    const res = await getProductDetail(row.id)
    if (res.code === 200) {
      const data = res.data
      formData.name = data.name || ''
      formData.description = data.description || ''
      formData.price = data.price ?? null
      formData.originalPrice = data.originalPrice ?? null
      formData.categoryId = data.categoryId ?? null
      formData.stock = data.stock ?? null
      formData.image = data.image || ''
      formData.isRecommended = !!data.isRecommended
      formData.specs = data.specs && data.specs.length > 0
        ? data.specs.map(s => ({
            specName: s.specName || '',
            specValue: s.specValue || '',
            extraPrice: s.extraPrice ?? 0,
            stock: s.stock ?? 0
          }))
        : []
      formData.images = data.images && data.images.length > 0
        ? data.images.map(i => (typeof i === 'string' ? { url: i } : { url: i.url || '' }))
        : []
    } else {
      ElMessage.error(res.message || '获取商品详情失败')
    }
  } catch {
    ElMessage.error('获取商品详情失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      name: formData.name,
      description: formData.description,
      price: formData.price,
      originalPrice: formData.originalPrice,
      categoryId: formData.categoryId,
      stock: formData.stock,
      image: formData.image,
      isRecommended: formData.isRecommended,
      specs: formData.specs,
      images: formData.images
    }

    if (isEdit.value) {
      payload.id = editingId.value
    }

    const api = isEdit.value ? updateProduct : createProduct
    const res = await api(payload)
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      fetchProducts()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (err) {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确认${actionText}该商品？`, '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await request.put(`/product/status/${row.id}`, { status: newStatus })
    if (res.code === 200) {
      ElMessage.success(`${actionText}成功`)
      fetchProducts()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    // cancelled
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除商品「${row.name}」？此操作不可恢复。`, '警告', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    const res = await deleteProduct(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchProducts()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    // cancelled
  }
}

function addSpec() {
  formData.specs.push({ specName: '', specValue: '', extraPrice: 0, stock: 0 })
}

function removeSpec(index) {
  formData.specs.splice(index, 1)
}

function addImage() {
  formData.images.push({ url: '' })
}

onMounted(() => {
  fetchProducts()
  fetchCategories()
})
</script>

<style scoped>
.search-bar {
  margin-bottom: 0;
}

.table-header {
  display: flex;
  justify-content: flex-end;
}

.image-placeholder {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 12px;
}

:deep(.el-divider__text) {
  font-size: 14px;
  font-weight: 600;
}
</style>
