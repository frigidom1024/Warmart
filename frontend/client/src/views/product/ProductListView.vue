<template>
  <div class="product-list-page">
    <!-- Search Bar -->
    <div class="search-section">
      <div class="search-inner">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品"
          size="large"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" size="large" :icon="Search" @click="handleSearch">
          搜索
        </el-button>
      </div>
    </div>

    <div class="main-container" v-loading="loading">
      <!-- Category Sidebar -->
      <aside class="category-sidebar" v-if="categoryTree.length > 0">
        <div class="sidebar-title">商品分类</div>
        <el-tree
          :data="categoryTree"
          :props="{ label: 'name', children: 'children' }"
          node-key="id"
          :highlight-current="true"
          :current-node-key="currentCategoryId"
          @node-click="handleCategoryClick"
        />
      </aside>

      <!-- Right Content -->
      <section class="content-area">
        <!-- Sort Bar -->
        <div class="sort-bar">
          <span class="sort-label">排序：</span>
          <el-button
            v-for="option in sortOptions"
            :key="option.value"
            :type="sortBy === option.value ? 'primary' : 'default'"
            size="small"
            @click="handleSortChange(option.value)"
          >
            {{ option.label }}
          </el-button>
        </div>

        <!-- Product Grid -->
        <template v-if="productList.length > 0">
          <el-row :gutter="20">
            <el-col
              v-for="product in productList"
              :key="product.id"
              :xs="12"
              :sm="8"
              :md="6"
              :lg="6"
              :xl="6"
              class="product-col"
            >
              <div class="product-card" @click="goDetail(product.id)">
                <div class="product-image-wrapper">
                  <el-image
                    :src="productImage(product)"
                    :alt="product.name"
                    fit="cover"
                    loading="lazy"
                  >
                    <template #error>
                      <div class="image-placeholder">
                        <el-icon :size="32"><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                </div>
                <div class="product-info">
                  <h3 class="product-name" :title="product.name">{{ product.name }}</h3>
                  <div class="product-price">
                    <span class="price-symbol">¥</span>
                    <span class="price-value">{{ formatPrice(product.price) }}</span>
                  </div>
                </div>
              </div>
            </el-col>
          </el-row>
        </template>
        <el-empty v-else-if="!loading" description="暂无商品" />

        <!-- Pagination -->
        <div class="pagination-wrapper" v-if="total > pageSize">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next, jumper"
            @current-change="handlePageChange"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProducts, getCategories } from '@/api/product'
import { Picture, Search } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// State
const searchKeyword = ref('')
const categoryTree = ref([])
const productList = ref([])
const total = ref(0)
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(12)
const sortBy = ref('')

const sortOptions = [
  { label: '综合', value: '' },
  { label: '价格升序', value: 'price_asc' },
  { label: '价格降序', value: 'price_desc' },
  { label: '销量', value: 'sales' },
  { label: '最新', value: 'newest' },
]

const currentCategoryId = computed(() => {
  const id = route.query.categoryId
  return id ? Number(id) : null
})

// Build a tree from flat category list
const buildCategoryTree = (categories) => {
  const map = {}
  const tree = []

  categories.forEach((c) => {
    const id = c.id ?? c.categoryId
    if (id != null) {
      map[id] = { ...c, id, children: [] }
    }
  })

  categories.forEach((c) => {
    const id = c.id ?? c.categoryId
    const pid = c.parentId ?? c.parent_id ?? c.pid
    if (pid === 0 || pid === null || pid === undefined) {
      if (map[id]) tree.push(map[id])
    } else if (map[pid]) {
      map[pid].children.push(map[id])
    }
  })

  return tree
}

// Load categories and build tree
const loadCategories = async () => {
  try {
    const res = await getCategories()
    const data = res.data || res
    if (Array.isArray(data)) {
      categoryTree.value = buildCategoryTree(data)
    }
  } catch (e) {
    console.error('Failed to load categories:', e)
  }
}

// Fetch products based on current route query
const fetchProducts = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      pageSize: pageSize.value,
    }

    const { keyword, categoryId, sortBy: sort } = route.query
    if (keyword) params.keyword = keyword
    if (categoryId) params.categoryId = categoryId

    if (sort === 'price_asc') {
      params.sort = 'price'
      params.order = 'asc'
    } else if (sort === 'price_desc') {
      params.sort = 'price'
      params.order = 'desc'
    } else if (sort === 'sales') {
      params.sort = 'sales'
      params.order = 'desc'
    } else if (sort === 'newest') {
      params.sort = 'createdAt'
      params.order = 'desc'
    }

    const res = await getProducts(params)
    const data = res.data || res
    if (data && data.records) {
      productList.value = data.records
      total.value = data.total
    } else if (Array.isArray(data)) {
      productList.value = data
      total.value = data.length
    } else {
      productList.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('Failed to load products:', e)
    productList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// Helpers
const productImage = (product) => {
  return product.image || product.coverImage || product.cover_img || ''
}

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

// Handlers
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  const query = { page: 1 }
  if (keyword) {
    query.keyword = keyword
  }
  router.push({ path: '/product/list', query })
}

const handleCategoryClick = (node) => {
  const query = { page: 1, categoryId: node.id }
  router.push({ path: '/product/list', query })
  searchKeyword.value = ''
}

const handleSortChange = (value) => {
  const query = { ...route.query, page: 1 }
  if (value) {
    query.sortBy = value
  } else {
    delete query.sortBy
  }
  router.push({ path: '/product/list', query })
}

const handlePageChange = (page) => {
  const query = { ...route.query, page }
  router.push({ path: '/product/list', query })
}

const goDetail = (id) => {
  router.push(`/product/detail/${id}`)
}

// React to route query changes
watch(
  () => [route.query, route.path],
  () => {
    currentPage.value = Number(route.query.page) || 1
    sortBy.value = route.query.sortBy || ''
    searchKeyword.value = route.query.keyword || ''
    fetchProducts()
  },
  { immediate: true }
)

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.product-list-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-top: 60px;
}

/* Search Bar */
.search-section {
  background: #fff;
  padding: 20px 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

.search-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  padding: 0 20px;
}

.search-input {
  max-width: 500px;
}

/* Main Layout */
.main-container {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

/* Category Sidebar */
.category-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

/* Sort Bar */
.content-area {
  flex: 1;
  min-width: 0;
}

.sort-bar {
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.sort-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

/* Product Cards */
.product-col {
  margin-bottom: 20px;
}

.product-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.product-image-wrapper {
  width: 100%;
  padding-top: 100%;
  position: relative;
  background: #f5f5f5;
  overflow: hidden;
}

.product-image-wrapper .el-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f5f5;
}

.product-info {
  padding: 12px 14px 16px;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 10px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}

.product-price {
  color: #f56c6c;
  font-weight: 700;
}

.price-symbol {
  font-size: 12px;
  margin-right: 1px;
}

.price-value {
  font-size: 18px;
}

/* Pagination */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0 40px;
}

/* Responsive */
@media (max-width: 768px) {
  .main-container {
    flex-direction: column;
  }

  .category-sidebar {
    width: 100%;
  }

  .search-inner {
    flex-direction: column;
  }

  .search-input {
    max-width: 100%;
  }

  .product-info {
    padding: 8px 10px 12px;
  }

  .product-name {
    font-size: 13px;
    min-height: auto;
  }

  .price-value {
    font-size: 16px;
  }
}
</style>
