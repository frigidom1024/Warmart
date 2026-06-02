<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductList, getCategoryList, searchProducts } from '@/api/product'
import { addFavorite } from '@/api/favorite'
import { showToast } from '@/utils/toast'
import type { Product, Category } from '@/api/product'

const route = useRoute()
const router = useRouter()

const products = ref<Product[]>([])
const categories = ref<{ label: string; value: number | '' }[]>([{ label: '全部', value: '' }])
const loading = ref(false)
const totalCount = ref(0)
const totalPages = ref(1)

const activeCategory = ref<number | ''>((route.query.categoryId ? Number(route.query.categoryId) : '') as number | '')
const keyword = ref((route.query.keyword as string) || '')
const sortBy = ref('default')
const currentPage = ref(1)
const minPrice = ref('')
const maxPrice = ref('')
const pageSize = 12

const isSearching = computed(() => !!keyword.value || !!minPrice.value || !!maxPrice.value)

const sortMap: Record<string, string | undefined> = {
  default: undefined,
  sales: 'sales_desc',
  'price-asc': 'price_asc',
  'price-desc': 'price_desc',
  newest: undefined,
}

function buildParams() {
  const params: Record<string, any> = { page: currentPage.value, size: pageSize }

  if (activeCategory.value !== '') {
    params.categoryId = activeCategory.value
  }
  const sortValue = sortMap[sortBy.value]
  if (sortValue) {
    params.sortBy = sortValue
  }

  if (keyword.value) params.keyword = keyword.value
  if (minPrice.value) params.minPrice = parseFloat(minPrice.value)
  if (maxPrice.value) params.maxPrice = parseFloat(maxPrice.value)

  return params
}

async function loadProducts() {
  loading.value = true
  try {
    const baseParams = buildParams()
    let allRecords: Product[] = []

    if (keyword.value) {
      // Fetch exact matches first, then fuzzy matches, merge with exact first
      const exactRes = await searchProducts({ ...baseParams, exactMatch: true })
      const exactIds = new Set(exactRes.records.map(p => p.id))
      allRecords = [...exactRes.records]

      const fuzzyRes = await searchProducts({ ...baseParams, exactMatch: false })
      allRecords.push(...fuzzyRes.records.filter(p => !exactIds.has(p.id)))

      totalCount.value = exactRes.total + fuzzyRes.records.filter(p => !exactIds.has(p.id)).length
    } else {
      const res = await getProductList(baseParams)
      allRecords = res.records || []
      totalCount.value = res.total || 0
    }

    products.value = allRecords
    totalPages.value = Math.max(1, Math.ceil(totalCount.value / pageSize))
  } catch {
    products.value = []
    totalCount.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

function setCategory(value: number | '') {
  activeCategory.value = value
  currentPage.value = 1
  syncStateToUrl()
  loadProducts()
}

function setSort(value: string) {
  sortBy.value = value
  currentPage.value = 1
  syncStateToUrl()
  loadProducts()
}

function goProduct(id: number) {
  router.push({ name: 'ProductDetail', params: { id } })
}

function syncStateToUrl() {
  const q: Record<string, string> = {}
  if (keyword.value) q.keyword = keyword.value
  if (activeCategory.value !== '') q.categoryId = String(activeCategory.value)
  if (sortBy.value !== 'default') q.sortBy = sortBy.value
  if (minPrice.value) q.minPrice = minPrice.value
  if (maxPrice.value) q.maxPrice = maxPrice.value
  if (currentPage.value > 1) q.page = String(currentPage.value)
  router.replace({ query: q })
}

function handleSearch() {
  currentPage.value = 1
  syncStateToUrl()
  loadProducts()
}

function handleReset() {
  keyword.value = ''
  activeCategory.value = ''
  sortBy.value = 'default'
  minPrice.value = ''
  maxPrice.value = ''
  currentPage.value = 1
  router.replace({ query: {} })
  loadProducts()
}

const animatingIds = reactive(new Set<number>())
const favoritedIds = reactive(new Set<number>())

async function handleFavorite(id: number) {
  if (animatingIds.has(id)) return
  animatingIds.add(id)
  try {
    await addFavorite(id)
    favoritedIds.add(id)
    showToast('已收藏', 'success')
  } catch { /* handled */ }
  setTimeout(() => animatingIds.delete(id), 700)
}

function goPage(p: number) {
  if (p < 1 || p > totalPages.value) return
  currentPage.value = p
  syncStateToUrl()
  loadProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function clearKeyword() {
  keyword.value = ''
  currentPage.value = 1
  router.replace({ query: { ...route.query, keyword: undefined } })
  loadProducts()
}

function clearPrice() {
  minPrice.value = ''
  maxPrice.value = ''
  currentPage.value = 1
  loadProducts()
}

function applyPrice() {
  currentPage.value = 1
  syncStateToUrl()
  loadProducts()
}

watch(() => route.query, () => {
  keyword.value = (route.query.keyword as string) || ''
  activeCategory.value = (route.query.categoryId ? Number(route.query.categoryId) : '') as number | ''
  sortBy.value = (route.query.sortBy as string) || 'default'
  minPrice.value = (route.query.minPrice as string) || ''
  maxPrice.value = (route.query.maxPrice as string) || ''
  currentPage.value = route.query.page ? Number(route.query.page) : 1
  loadProducts()
})

const categoryLabel = computed(() => {
  const c = categories.value.find(c => c.value === activeCategory.value)
  return c ? c.label : ''
})

const sortOptions = [
  { label: '综合', value: 'default' },
  { label: '销量优先', value: 'sales' },
  { label: '价格 ↑', value: 'price-asc' },
  { label: '价格 ↓', value: 'price-desc' },
  { label: '最新', value: 'newest' },
]

const activeFilters = computed(() => {
  const filters: { label: string; onRemove: () => void }[] = []
  if (keyword.value) filters.push({ label: `搜索：${keyword.value}`, onRemove: clearKeyword })
  if (activeCategory.value !== '') {
    filters.push({ label: `分类：${categoryLabel.value}`, onRemove: () => setCategory('') })
  }
  if (minPrice.value || maxPrice.value) {
    filters.push({ label: `价格 ${minPrice.value || '0'} ~ ${maxPrice.value || '不限'}`, onRemove: clearPrice })
  }
  return filters
})

onMounted(async () => {
  try {
    const catRes = await getCategoryList()
    const cats = catRes as Category[]
    categories.value = [
      { label: '全部', value: '' },
      ...cats.map(c => ({ label: c.name, value: c.id }))
    ]
  } catch {
    // use default
  }
  loadProducts()
})
</script>

<template>
  <div class="page-container">
    <div class="plp">
      <!-- Breadcrumb -->
      <div class="plp__breadcrumb">
        <span class="plp__crumb" @click="router.push('/')">首页</span>
        <span class="plp__crumb-sep">/</span>
        <span class="plp__crumb">商品列表</span>
        <span v-if="categoryLabel" class="plp__crumb-sep">/</span>
        <span v-if="categoryLabel" class="plp__crumb">{{ categoryLabel }}</span>
      </div>

      <!-- ====== In-page search bar ====== -->
      <div class="plp__search-bar">
        <div class="plp__search-input-wrap">
          <input
            v-model="keyword"
            type="text"
            class="plp__search-input"
            placeholder="搜索商品名称…"
            @keyup.enter="handleSearch"
          />
          <button class="plp__search-btn" @click="handleSearch">搜索</button>
          <button class="plp__search-reset" @click="handleReset">重置</button>
        </div>
      </div>

      <div class="plp__layout">
        <!-- ====== SIDEBAR ====== -->
        <aside class="plp__sidebar">
          <!-- Category -->
          <div class="plp__filter-section">
            <h3 class="plp__filter-title">分类</h3>
            <div class="plp__cat-list">
              <button
                v-for="cat in categories"
                :key="cat.value"
                class="plp__cat-btn"
                :class="{ 'plp__cat-btn--active': activeCategory === cat.value }"
                @click="setCategory(cat.value)"
              >{{ cat.label }}</button>
            </div>
          </div>

          <!-- Price Range -->
          <div class="plp__filter-section">
            <h3 class="plp__filter-title">价格区间</h3>
            <div class="plp__price-range">
              <input v-model="minPrice" type="number" placeholder="最低" class="plp__price-input" min="0">
              <span class="plp__price-dash">—</span>
              <input v-model="maxPrice" type="number" placeholder="最高" class="plp__price-input" min="0">
            </div>
            <button class="plp__price-apply" @click="applyPrice">应用</button>
          </div>
        </aside>

        <!-- ====== CONTENT ====== -->
        <div class="plp__content">
          <!-- Toolbar -->
          <div class="plp__toolbar">
            <span class="plp__count">共 <strong>{{ totalCount }}</strong> 件商品</span>
            <div class="plp__sort-group">
              <button
                v-for="opt in sortOptions"
                :key="opt.value"
                class="plp__sort-btn"
                :class="{ 'plp__sort-btn--active': sortBy === opt.value }"
                @click="setSort(opt.value)"
              >{{ opt.label }}</button>
            </div>
          </div>

          <!-- Active filters -->
          <div v-if="activeFilters.length" class="plp__active-filters">
            <span
              v-for="(f, i) in activeFilters"
              :key="i"
              class="plp__filter-tag"
              @click="f.onRemove"
            >{{ f.label }} <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M18 6 6 18M6 6l12 12"/></svg></span>
          </div>

          <!-- Product Grid -->
          <div v-if="products.length && !loading" class="plp__grid">
            <div
              v-for="p in products"
              :key="p.id"
              class="plp__card"
              @click="goProduct(p.id)"
            >
              <div class="plp__card-img">
                <img :src="p.mainImage" :alt="p.name" loading="lazy">
                <span v-if="p.tag" class="plp__card-badge">{{ p.tag }}</span>
                <button
                  class="plp__card-fav"
                  :class="{ 'plp__card-fav--active': favoritedIds.has(p.id) || animatingIds.has(p.id) }"
                  @click.stop="handleFavorite(p.id)"
                  title="收藏"
                >
                  <svg class="plp__card-fav-heart" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                  <span class="plp__card-fav-ring"></span>
                </button>
              </div>
              <div class="plp__card-body">
                <p class="plp__card-name">{{ p.name }}</p>
                <div class="plp__card-prices">
                  <span class="plp__card-price">¥{{ p.price }}</span>
                  <span v-if="p.originalPrice" class="plp__card-old">¥{{ p.originalPrice }}</span>
                </div>
                <div class="plp__card-footer">
                  <span class="plp__card-sales">已售 {{ p.sales > 999 ? (p.sales / 1000).toFixed(1) + 'k' : p.sales }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Empty State -->
          <div v-else class="plp__empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/><path d="M8 11h6"/>
            </svg>
            <p class="plp__empty-title">未找到相关商品</p>
            <p class="plp__empty-desc">试试调整筛选条件或搜索其他关键词</p>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="plp__pagination">
            <button
              class="plp__page-btn"
              :disabled="currentPage <= 1"
              @click="goPage(currentPage - 1)"
            >‹</button>
            <button
              v-for="p in totalPages"
              :key="p"
              class="plp__page-btn"
              :class="{ 'plp__page-btn--active': p === currentPage }"
              @click="goPage(p)"
            >{{ p }}</button>
            <button
              class="plp__page-btn"
              :disabled="currentPage >= totalPages"
              @click="goPage(currentPage + 1)"
            >›</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 60px;
  background: var(--wz-bg);
}

.plp {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 32px 80px;
}

/* ---- Breadcrumb ---- */
.plp__breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-bottom: 24px;
}

.plp__crumb {
  cursor: pointer;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__crumb:hover {
  color: var(--wz-orange);
}

.plp__crumb-sep {
  color: var(--wz-border);
}

/* ---- Search Bar ---- */
.plp__search-bar {
  margin-bottom: 20px;
}

.plp__search-input-wrap {
  display: flex;
  gap: 8px;
  max-width: 520px;
}

.plp__search-input {
  flex: 1;
  height: 40px;
  padding: 0 14px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  outline: none;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__search-input::placeholder {
  color: var(--wz-text-muted);
}

.plp__search-input:focus {
  border-color: var(--wz-orange);
}

.plp__search-btn {
  height: 40px;
  padding: 0 20px;
  background: var(--wz-orange);
  border: none;
  border-radius: 8px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__search-btn:hover {
  background: var(--wz-orange-dark);
}

.plp__search-reset {
  height: 40px;
  padding: 0 16px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  color: var(--wz-text-soft);
  font-family: var(--wz-font-body);
  font-size: 14px;
  cursor: pointer;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__search-reset:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}

/* ---- Layout ---- */
.plp__layout {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

/* ---- Sidebar ---- */
.plp__sidebar {
  width: 220px;
  flex-shrink: 0;
  position: sticky;
  top: 80px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.plp__sidebar::-webkit-scrollbar {
  width: 4px;
}

.plp__sidebar::-webkit-scrollbar-thumb {
  background: var(--wz-border);
  border-radius: 2px;
}

.plp__filter-section {
  margin-bottom: 28px;
}

.plp__filter-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 12px;
  letter-spacing: 0.04em;
}

.plp__cat-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.plp__cat-btn {
  display: block;
  width: 100%;
  text-align: left;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-soft);
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
  font-family: var(--wz-font-body);
}

.plp__cat-btn:hover {
  background: var(--wz-bg-hover);
  color: var(--wz-text);
}

.plp__cat-btn--active {
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
  font-weight: 600;
}

/* Price range */
.plp__price-range {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 10px;
}

.plp__price-input {
  width: 80px;
  padding: 7px 10px;
  font-size: 13px;
  color: var(--wz-text);
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 6px;
  outline: none;
  font-family: var(--wz-font-body);
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__price-input:focus {
  border-color: var(--wz-orange);
}

.plp__price-input::placeholder {
  color: var(--wz-text-muted);
}

.plp__price-dash {
  color: var(--wz-text-muted);
  font-size: 13px;
}

.plp__price-apply {
  width: 100%;
  padding: 7px 0;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  background: var(--wz-orange);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-family: var(--wz-font-body);
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__price-apply:hover {
  background: var(--wz-orange-dark);
}

/* ---- Content ---- */
.plp__content {
  flex: 1;
  min-width: 0;
}

/* Toolbar */
.plp__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: var(--wz-bg-card);
  border-radius: 10px;
  margin-bottom: 16px;
}

.plp__count {
  font-size: 13px;
  color: var(--wz-text-soft);
}

.plp__count strong {
  color: var(--wz-orange);
  font-weight: 600;
}

.plp__sort-group {
  display: flex;
  gap: 4px;
}

.plp__sort-btn {
  padding: 5px 14px;
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-soft);
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-family: var(--wz-font-body);
  transition: color var(--wz-duration-fast) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__sort-btn:hover {
  color: var(--wz-text);
  background: var(--wz-bg-hover);
}

.plp__sort-btn--active {
  color: var(--wz-orange);
  background: var(--wz-orange-muted);
}

/* Active filter tags */
.plp__active-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.plp__filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 500;
  color: var(--wz-text);
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 999px;
  cursor: pointer;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__filter-tag:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}

/* Product Grid */
.plp__grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.plp__card {
  background: var(--wz-bg-card);
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s var(--wz-ease-out),
              box-shadow 0.3s var(--wz-ease-out);
}

.plp__card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.3);
}

.plp__card-img {
  position: relative;
  aspect-ratio: 4 / 3;
  overflow: hidden;
  background: var(--wz-bg);
}

.plp__card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s var(--wz-ease-out);
}

.plp__card:hover .plp__card-img img {
  transform: scale(1.06);
}

.plp__card-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--wz-orange);
  border-radius: 999px;
  line-height: 1.3;
}

.plp__card:nth-child(3n+2) .plp__card-badge {
  background: var(--wz-success);
}

.plp__card:nth-child(3n+3) .plp__card-badge {
  background: #af52de;
}

.plp__card-fav {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 3;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  border: none;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity var(--wz-duration-normal) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out);
  backdrop-filter: blur(4px);
}

.plp__card:hover .plp__card-fav {
  opacity: 1;
}

.plp__card-fav:hover {
  background: rgba(255, 107, 53, 0.85);
  color: #fff;
  transform: scale(1.1);
}

/* Favorite button animation */
.plp__card-fav--active {
  background: rgba(255, 107, 53, 0.85);
  color: #fff;
}

.plp__card-fav-heart {
  position: relative;
  z-index: 1;
  transition: transform 0.3s var(--wz-ease-out);
}

.plp__card-fav--active .plp__card-fav-heart {
  fill: currentColor;
  animation: plp-fav-beat 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

.plp__card-fav-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid var(--wz-orange);
  opacity: 0;
  pointer-events: none;
}

.plp__card-fav--active .plp__card-fav-ring {
  animation: plp-fav-ripple 0.6s ease-out forwards;
}

@keyframes plp-fav-beat {
  0% { transform: scale(1); }
  30% { transform: scale(1.5); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1.2); }
}

@keyframes plp-fav-ripple {
  0% { transform: scale(0.8); opacity: 0.6; }
  100% { transform: scale(2); opacity: 0; }
}

.plp__card-body {
  padding: 12px 14px 14px;
}

.plp__card-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}

.plp__card-prices {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.plp__card-price {
  font-size: 16px;
  font-weight: 700;
  color: var(--wz-orange);
  line-height: 1;
}

.plp__card-old {
  font-size: 12px;
  color: var(--wz-text-muted);
  text-decoration: line-through;
  line-height: 1;
}

.plp__card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.plp__card-sales {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* Empty state */
.plp__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--wz-text-muted);
}

.plp__empty svg {
  margin-bottom: 16px;
  opacity: 0.5;
}

.plp__empty-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--wz-text-soft);
  margin-bottom: 8px;
}

.plp__empty-desc {
  font-size: 13px;
  color: var(--wz-text-muted);
}

/* Pagination */
.plp__pagination {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.plp__page-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text-soft);
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  cursor: pointer;
  font-family: var(--wz-font-body);
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out);
}

.plp__page-btn:hover:not(:disabled):not(.plp__page-btn--active) {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}

.plp__page-btn--active {
  background: var(--wz-orange);
  border-color: var(--wz-orange);
  color: #fff;
}

.plp__page-btn:disabled {
  opacity: 0.3;
  cursor: default;
}

/* ---- Responsive ---- */
@media (max-width: 1200px) {
  .plp__grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 900px) {
  .plp {
    padding: 20px 16px 60px;
  }
  .plp__sidebar {
    display: none;
  }
  .plp__layout {
    gap: 0;
  }
  .plp__grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .plp__toolbar {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 16px;
  }
}

@media (max-width: 480px) {
  .plp__grid {
    grid-template-columns: 1fr;
  }
  .plp__sort-btn {
    font-size: 12px;
    padding: 4px 10px;
  }
}
</style>
