<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminProductList, addProduct, updateProduct, deleteProduct } from '../api/product'
import { getCategoryList } from '../api/category'
import type { Product } from '../api/product'
import type { Category } from '../api/category'

const products = ref<Product[]>([])
const categories = ref<Category[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({ categoryId: undefined as number | undefined, status: undefined as number | undefined, page: 1, size: 10 })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = ref<Partial<Product>>({
  name: '', categoryId: 0, price: 0, originalPrice: 0, stock: 0,
  tag: '', description: '', mainImage: '', status: 1, isRecommend: 0,
  hasSpec: 0, sales: 0
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
  } catch {} finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getCategoryList()
  } catch {}
}

function handleSearch() {
  query.value.page = 1
  loadProducts()
}

function handleReset() {
  query.value.categoryId = undefined
  query.value.status = undefined
  query.value.page = 1
  loadProducts()
}

function openAddDialog() {
  isEdit.value = false
  form.value = { name: '', categoryId: 0, price: 0, originalPrice: 0, stock: 0, tag: '', description: '', mainImage: '', status: 1, isRecommend: 0, hasSpec: 0, sales: 0 }
  dialogVisible.value = true
}

function openEditDialog(item: Product) {
  isEdit.value = true
  form.value = { ...item }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.name) {
    ElMessage.warning('请输入商品名称')
    return
  }
  try {
    if (isEdit.value) {
      await updateProduct(form.value)
      ElMessage.success('更新成功')
    } else {
      await addProduct(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadProducts()
  } catch {}
}

async function handleDelete(item: Product) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${item.name}」吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await deleteProduct(item.id)
    ElMessage.success('删除成功')
    await loadProducts()
  } catch {}
}

function handlePageChange(page: number) {
  query.value.page = page
  loadProducts()
}

const categoryMap = computed(() => {
  const map: Record<number, string> = {}
  categories.value.forEach(c => { map[c.id] = c.name })
  return map
})
onMounted(() => {
  loadCategories()
  loadProducts()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h2>商品管理</h2>
    </div>
    <el-card>
      <div class="search-bar">
        <el-select v-model="query.categoryId" placeholder="商品分类" clearable class="search-select" @change="handleSearch">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="query.status" placeholder="上下架状态" clearable class="search-select" @change="handleSearch">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" @click="openAddDialog">+ 新增商品</el-button>
      </div>
      <el-table :data="products" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="图片" width="70">
          <template #default="{ row }">
            <el-image :src="row.mainImage" fit="cover" style="width: 40px; height: 40px; border-radius: 4px;">
              <template #error><div class="img-error">无</div></template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">{{ categoryMap[row.categoryId] || '-' }}</template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="90">
          <template #default="{ row }">¥{{ row.price }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="70" />
        <el-table-column prop="sales" label="销量" width="70" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="650px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="商品分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="价格">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="原价">
              <el-input-number v-model="form.originalPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="库存">
              <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="标签">
          <el-input v-model="form.tag" placeholder="如：新品、热销" style="width: 240px" />
        </el-form-item>
        <el-form-item label="主图URL">
          <el-input v-model="form.mainImage" placeholder="请填写图片URL" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="推荐">
          <el-radio-group v-model="form.isRecommend">
            <el-radio :value="1">推荐</el-radio>
            <el-radio :value="0">不推荐</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: var(--wa-text-primary, #303133); }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.search-select { width: 160px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
.img-error { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; font-size: 11px; color: #999; border-radius: 4px; }
</style>
