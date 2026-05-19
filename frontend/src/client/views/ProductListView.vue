<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

interface Product {
  id: number
  title: string
  price: number
  old?: number
  tag?: string
  img: string
  category: string
  sales: number
}

const route = useRoute()
const router = useRouter()

const allProducts: Product[] = [
  { id: 101, title: '轻奢百搭手提包', price: 199, old: 399, tag: '热卖', img: 'https://picsum.photos/id/1/400/300', category: 'clothing', sales: 2834 },
  { id: 102, title: '纯棉宽松卫衣', price: 159, img: 'https://picsum.photos/id/5/400/300', category: 'clothing', sales: 1542 },
  { id: 103, title: '法式碎花连衣裙', price: 239, old: 359, tag: '新品', img: 'https://picsum.photos/id/9/400/300', category: 'clothing', sales: 982 },
  { id: 104, title: '韩版宽松牛仔外套', price: 189, img: 'https://picsum.photos/id/10/400/300', category: 'clothing', sales: 671 },
  { id: 105, title: '复古英伦风衣', price: 299, old: 499, tag: '推荐', img: 'https://picsum.photos/id/19/400/300', category: 'clothing', sales: 445 },
  { id: 106, title: '高腰直筒牛仔裤', price: 139, img: 'https://picsum.photos/id/21/400/300', category: 'clothing', sales: 2103 },
  { id: 107, title: '桑蚕丝印花衬衫', price: 189, old: 289, img: 'https://picsum.photos/id/22/400/300', category: 'clothing', sales: 756 },
  { id: 108, title: '纯色针织开衫', price: 169, tag: '新品', img: 'https://picsum.photos/id/40/400/300', category: 'clothing', sales: 534 },
  { id: 201, title: '持妆粉底液', price: 89, tag: '爆款', img: 'https://picsum.photos/id/3/400/300', category: 'beauty', sales: 4521 },
  { id: 202, title: '天然植物精华液', price: 139, old: 269, img: 'https://picsum.photos/id/7/400/300', category: 'beauty', sales: 3210 },
  { id: 203, title: '补水面膜套装', price: 59, tag: '热卖', img: 'https://picsum.photos/id/36/400/300', category: 'beauty', sales: 6789 },
  { id: 204, title: '防晒霜SPF50', price: 79, img: 'https://picsum.photos/id/37/400/300', category: 'beauty', sales: 5432 },
  { id: 301, title: '无线蓝牙耳机', price: 129, old: 229, tag: '6折', img: 'https://picsum.photos/id/4/400/300', category: 'digital', sales: 8765 },
  { id: 302, title: '智能扫地机器人', price: 1299, img: 'https://picsum.photos/id/6/400/300', category: 'digital', sales: 1234 },
  { id: 303, title: '4K超清投影仪', price: 2399, old: 2999, tag: '直降', img: 'https://picsum.photos/id/23/400/300', category: 'digital', sales: 876 },
  { id: 304, title: '机械键盘青轴', price: 159, img: 'https://picsum.photos/id/24/400/300', category: 'digital', sales: 3456 },
  { id: 401, title: '北欧ins风台灯', price: 79, tag: '新品', img: 'https://picsum.photos/id/11/400/300', category: 'home', sales: 2345 },
  { id: 402, title: '日式懒人沙发', price: 299, old: 429, img: 'https://picsum.photos/id/12/400/300', category: 'home', sales: 1567 },
  { id: 403, title: '简约落地衣架', price: 89, img: 'https://picsum.photos/id/25/400/300', category: 'home', sales: 3210 },
  { id: 404, title: '纯棉四件套', price: 199, old: 299, tag: '热卖', img: 'https://picsum.photos/id/26/400/300', category: 'home', sales: 2890 },
  { id: 501, title: '混合坚果礼盒', price: 59, img: 'https://picsum.photos/id/31/400/300', category: 'food', sales: 5678 },
  { id: 502, title: '抹茶生巧', price: 39, tag: '爆款', img: 'https://picsum.photos/id/32/400/300', category: 'food', sales: 7890 },
  { id: 503, title: '手撕牛肉干', price: 49, img: 'https://picsum.photos/id/33/400/300', category: 'food', sales: 4567 },
  { id: 601, title: '专业跑步鞋', price: 259, old: 359, tag: '推荐', img: 'https://picsum.photos/id/13/400/300', category: 'sports', sales: 1876 },
  { id: 602, title: '户外背包大容量', price: 179, img: 'https://picsum.photos/id/8/400/300', category: 'sports', sales: 1245 },
  { id: 603, title: '加厚防滑瑜伽垫', price: 69, old: 129, img: 'https://picsum.photos/id/14/400/300', category: 'sports', sales: 3456 },
]

const categories = [
  { label: '全部', value: '' },
  { label: '潮流服饰', value: 'clothing' },
  { label: '美妆护肤', value: 'beauty' },
  { label: '数码家电', value: 'digital' },
  { label: '家居软装', value: 'home' },
  { label: '休闲零食', value: 'food' },
  { label: '运动户外', value: 'sports' },
]

const activeCategory = ref((route.query.category as string) || '')
const keyword = ref((route.query.keyword as string) || '')
const sortBy = ref('default')
const currentPage = ref(1)
const minPrice = ref('')
const maxPrice = ref('')
const pageSize = 12

const filteredProducts = computed(() => {
  let list = [...allProducts]

  if (activeCategory.value) {
    list = list.filter(p => p.category === activeCategory.value)
  }

  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(p => p.title.includes(kw))
  }

  const min = parseFloat(minPrice.value)
  const max = parseFloat(maxPrice.value)
  if (!isNaN(min)) list = list.filter(p => p.price >= min)
  if (!isNaN(max)) list = list.filter(p => p.price <= max)

  switch (sortBy.value) {
    case 'sales':
      list.sort((a, b) => b.sales - a.sales)
      break
    case 'price-asc':
      list.sort((a, b) => a.price - b.price)
      break
    case 'price-desc':
      list.sort((a, b) => b.price - a.price)
      break
    case 'newest':
      list.sort((a, b) => b.id - a.id)
      break
    default:
      break
  }

  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredProducts.value.length / pageSize)))

const pagedProducts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredProducts.value.slice(start, start + pageSize)
})

const totalCount = computed(() => filteredProducts.value.length)

function setCategory(value: string) {
  activeCategory.value = value
  currentPage.value = 1
  router.replace({ query: { ...route.query, category: value || undefined } })
}

function setSort(value: string) {
  sortBy.value = value
  currentPage.value = 1
}

function goProduct(id: number) {
  router.push({ name: 'ProductDetail', params: { id } })
}

function goPage(p: number) {
  if (p >= 1 && p <= totalPages.value) currentPage.value = p
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function clearKeyword() {
  keyword.value = ''
  router.replace({ query: { ...route.query, keyword: undefined } })
}

function clearPrice() {
  minPrice.value = ''
  maxPrice.value = ''
}

watch(keyword, (val) => {
  currentPage.value = 1
  router.replace({ query: { ...route.query, keyword: val || undefined } })
})

const categoryLabel = computed(() => {
  const c = categories.find(c => c.value === activeCategory.value)
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
  if (activeCategory.value) {
    filters.push({ label: `分类：${categoryLabel.value}`, onRemove: () => setCategory('') })
  }
  if (minPrice.value || maxPrice.value) {
    filters.push({ label: `价格 ${minPrice.value || '0'} ~ ${maxPrice.value || '不限'}`, onRemove: clearPrice })
  }
  return filters
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
            <button class="plp__price-apply" @click="currentPage = 1">应用</button>
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
          <div v-if="pagedProducts.length" class="plp__grid">
            <div
              v-for="p in pagedProducts"
              :key="p.id"
              class="plp__card"
              @click="goProduct(p.id)"
            >
              <div class="plp__card-img">
                <img :src="p.img" :alt="p.title" loading="lazy">
                <span v-if="p.tag" class="plp__card-badge">{{ p.tag }}</span>
              </div>
              <div class="plp__card-body">
                <p class="plp__card-name">{{ p.title }}</p>
                <div class="plp__card-prices">
                  <span class="plp__card-price">¥{{ p.price }}</span>
                  <span v-if="p.old" class="plp__card-old">¥{{ p.old }}</span>
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
