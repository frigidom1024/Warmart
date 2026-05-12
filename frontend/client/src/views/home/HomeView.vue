<template>
  <div class="home-page">
    <HeaderNav />

    <div v-loading="pageLoading" class="home-content">
      <!-- Banner Carousel -->
      <div class="banner-section" v-if="banners.length > 0">
        <el-carousel
          height="400px"
          :interval="4000"
          indicator-position="inside"
          arrow="hover"
          type=""
          class="banner-carousel"
        >
          <el-carousel-item v-for="banner in banners" :key="banner.id">
            <div class="banner-item" @click="handleBannerClick(banner)">
              <img
                :src="banner.imageUrl || banner.imgUrl || banner.image"
                :alt="banner.title || 'banner'"
                class="banner-img"
              />
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <!-- Category Section -->
      <div class="section category-section">
        <h2 class="section-title">商品分类</h2>
        <el-row :gutter="16">
          <el-col
            v-for="cat in topCategories"
            :key="cat.id"
            :xs="8"
            :sm="6"
            :md="4"
            :lg="4"
            :xl="4"
            class="category-col"
          >
            <div class="category-card" @click="goCategoryList(cat.id)">
              <div class="category-icon-wrapper">
                <el-icon :size="28" v-if="cat.icon">
                  <component :is="cat.icon" />
                </el-icon>
                <el-icon :size="28" v-else>
                  <FolderOpened />
                </el-icon>
              </div>
              <span class="category-name">{{ cat.name }}</span>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- Recommended Products Section -->
      <div class="section products-section">
        <h2 class="section-title">推荐商品</h2>
        <el-row :gutter="20">
          <el-col
            v-for="product in products"
            :key="product.id"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="6"
            :xl="6"
            class="product-col"
          >
            <div class="product-card" @click="goProductDetail(product.id)">
              <div class="product-image-wrapper">
                <el-image
                  :src="productImage(product)"
                  :alt="product.name"
                  class="product-image"
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
                <h3 class="product-name" :title="product.name">
                  {{ product.name }}
                </h3>
                <div class="product-meta">
                  <span class="product-price">
                    <span class="price-symbol">¥</span>
                    <span class="price-value">{{ formatPrice(product.price) }}</span>
                  </span>
                  <span class="product-sales" v-if="product.sales !== undefined">
                    已售 {{ product.sales }}
                  </span>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>

        <!-- Empty state -->
        <el-empty v-if="!pageLoading && products.length === 0" description="暂无推荐商品" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBanners, getCategories, getProducts } from '@/api/product'
import HeaderNav from '@/components/HeaderNav.vue'

const router = useRouter()

// Data
const banners = ref([])
const topCategories = ref([])
const products = ref([])
const pageLoading = ref(true)

// Fetch all home page data
onMounted(async () => {
  try {
    const [bannerRes, categoryRes, productRes] = await Promise.all([
      getBanners(),
      getCategories(),
      getProducts({ isRecommended: 1 })
    ])

    // Banners
    const bannerData = bannerRes.data || bannerRes
    banners.value = Array.isArray(bannerData) ? bannerData : []

    // Categories - filter top-level (parent_id === 0)
    const categoryData = categoryRes.data || categoryRes
    if (Array.isArray(categoryData)) {
      topCategories.value = categoryData.filter(c => {
        const pid = c.parentId ?? c.parent_id ?? c.pid
        return pid === 0 || pid === null || pid === undefined
      })
    }

    // Products
    const productData = productRes.data || productRes
    if (Array.isArray(productData)) {
      products.value = productData
    } else if (productData && typeof productData === 'object') {
      // Handle paginated responses: { records: [...], total: ... }
      products.value = productData.records || productData.list || productData.rows || []
    }
  } catch (e) {
    console.error('Failed to load home page data:', e)
  } finally {
    pageLoading.value = false
  }
})

// Helpers
const productImage = (product) => {
  return product.image || product.coverImage || product.cover_img || (product.images && product.images[0]) || ''
}

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const handleBannerClick = (banner) => {
  const url = banner.linkUrl || banner.link_url || banner.link
  if (url) {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      window.open(url, '_blank')
    } else {
      router.push(url)
    }
  }
}

const goCategoryList = (id) => {
  router.push({ path: '/product/list', query: { categoryId: id } })
}

const goProductDetail = (id) => {
  router.push(`/product/detail/${id}`)
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.home-content {
  padding-top: 60px; /* offset for fixed header */
}

/* Banner */
.banner-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 20px 0;
}

.banner-carousel {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.banner-item {
  width: 100%;
  height: 100%;
  cursor: pointer;
  overflow: hidden;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.banner-item:hover .banner-img {
  transform: scale(1.04);
}

/* Sections */
.section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
  padding-left: 12px;
  border-left: 4px solid #409eff;
  line-height: 1.4;
}

/* Category Cards */
.category-section {
  margin-top: 10px;
}

.category-col {
  margin-bottom: 16px;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 12px;
  background: #fff;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(64, 158, 255, 0.15);
}

.category-icon-wrapper {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
  border-radius: 50%;
  margin-bottom: 10px;
  color: #409eff;
}

.category-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  text-align: center;
  line-height: 1.3;
}

/* Product Cards */
.products-section {
  padding-bottom: 40px;
}

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
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.10);
}

.product-image-wrapper {
  width: 100%;
  padding-top: 100%; /* 1:1 aspect ratio */
  position: relative;
  background: #f5f5f5;
  overflow: hidden;
}

.product-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .product-image {
  transform: scale(1.06);
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
  text-overflow: ellipsis;
  min-height: 39px;
}

.product-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.product-sales {
  font-size: 12px;
  color: #909399;
}

/* Responsive */
@media (max-width: 768px) {
  .banner-carousel {
    height: 200px !important;
  }

  .section-title {
    font-size: 18px;
  }

  .category-card {
    padding: 14px 8px;
  }

  .category-icon-wrapper {
    width: 40px;
    height: 40px;
  }

  .category-name {
    font-size: 12px;
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
