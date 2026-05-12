<template>
  <div class="product-detail-page" v-loading="loading">
    <!-- Product Detail -->
    <div class="detail-container" v-if="product">
      <!-- Left: Image Gallery -->
      <div class="image-gallery">
        <div class="main-image">
          <el-image :src="currentImage" fit="contain">
            <template #error>
              <div class="image-placeholder">
                <el-icon :size="48"><Picture /></el-icon>
              </div>
            </template>
          </el-image>
        </div>
        <div class="thumbnail-list" v-if="imageList.length > 1">
          <div
            v-for="(img, index) in imageList"
            :key="index"
            class="thumbnail-item"
            :class="{ active: currentImage === img }"
            @click="currentImage = img"
          >
            <el-image :src="img" fit="cover" />
          </div>
        </div>
      </div>

      <!-- Right: Product Info -->
      <div class="product-info">
        <h1 class="product-name">{{ product.name }}</h1>

        <!-- Price -->
        <div class="price-section">
          <div class="current-price">
            <span class="symbol">¥</span>
            <span class="value">{{ formatPrice(currentPrice) }}</span>
          </div>
          <div class="original-price" v-if="originalPrice > 0 && originalPrice !== currentPrice">
            <s>¥{{ formatPrice(originalPrice) }}</s>
          </div>
        </div>

        <!-- Description -->
        <p class="product-desc" v-if="product.description || product.intro">
          {{ product.description || product.intro }}
        </p>

        <!-- Stock -->
        <div class="stock-info">
          <span class="label">库存：</span>
          <span :class="product.stock > 0 ? 'stock-available' : 'stock-empty'">
            {{ product.stock > 0 ? product.stock + ' 件' : '暂时无货' }}
          </span>
        </div>

        <!-- Spec Selection -->
        <div class="spec-section" v-if="specGroups.length > 0">
          <div class="spec-group" v-for="group in specGroups" :key="group.name">
            <span class="spec-label">{{ group.name }}：</span>
            <div class="spec-options">
              <el-button
                v-for="spec in group.items"
                :key="spec.value || spec.specValue"
                :type="isSpecSelected(group.name, spec) ? 'primary' : 'default'"
                size="small"
                :class="{ 'spec-selected': isSpecSelected(group.name, spec) }"
                @click="toggleSpec(group.name, spec)"
              >
                {{ spec.value || spec.specValue }}
              </el-button>
            </div>
          </div>
        </div>

        <!-- Quantity -->
        <div class="quantity-section">
          <span class="qty-label">数量：</span>
          <el-input-number
            v-model="quantity"
            :min="1"
            :max="maxQuantity"
            :disabled="!product.stock || product.stock <= 0"
          />
          <span class="stock-hint" v-if="product.stock > 0">(库存 {{ product.stock }} 件)</span>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <el-button
            type="danger"
            size="large"
            :disabled="!product.stock || product.stock <= 0"
            @click="addToCart"
          >
            加入购物车
          </el-button>
        </div>
      </div>
    </div>

    <!-- Bottom Tabs -->
    <div class="detail-tabs" v-if="product">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 商品详情 Tab -->
        <el-tab-pane label="商品详情" name="detail">
          <div class="tab-content detail-content">
            <div
              v-if="product.description || product.detail"
              class="rich-text"
              v-html="product.description || product.detail"
            ></div>
            <el-empty v-else description="暂无详情" />
          </div>
        </el-tab-pane>

        <!-- 评论 Tab -->
        <el-tab-pane label="评论" name="comments">
          <div class="tab-content comments-content" v-loading="commentsLoading">
            <template v-if="comments.length > 0">
              <div class="comment-item" v-for="comment in comments" :key="comment.id || comment._id">
                <div class="comment-header">
                  <el-avatar :size="36">
                    {{ (comment.userName || comment.nickname || '?')[0] }}
                  </el-avatar>
                  <div class="comment-user">
                    <span class="user-name">{{ comment.userName || comment.nickname || '匿名用户' }}</span>
                    <el-rate
                      :model-value="Number(comment.rating || comment.score || 0)"
                      disabled
                      size="small"
                    />
                  </div>
                  <span class="comment-time">{{ comment.createdAt || comment.createTime }}</span>
                </div>
                <p class="comment-content" v-if="comment.content">{{ comment.content }}</p>
                <div class="comment-images" v-if="comment.images && parsedCommentImages(comment.images).length > 0">
                  <el-image
                    v-for="(img, idx) in parsedCommentImages(comment.images)"
                    :key="idx"
                    :src="img"
                    fit="cover"
                    class="comment-image"
                    :preview-src-list="parsedCommentImages(comment.images)"
                  />
                </div>
              </div>

              <!-- Comment Pagination -->
              <div class="comment-pagination" v-if="commentsTotal > 10">
                <el-pagination
                  v-model:current-page="commentsPage"
                  :page-size="10"
                  :total="commentsTotal"
                  layout="total, prev, pager, next"
                  small
                  @current-change="handleCommentsPageChange"
                />
              </div>
            </template>
            <el-empty v-else description="暂无评论" />
          </div>
        </el-tab-pane>

        <!-- 咨询 Tab -->
        <el-tab-pane label="咨询" name="consultations">
          <div class="tab-content consultations-content" v-loading="consultationsLoading">
            <template v-if="consultations.length > 0">
              <div class="consultation-item" v-for="item in consultations" :key="item.id || item._id">
                <div class="consultation-q">
                  <el-icon :size="18" color="#409eff"><QuestionFilled /></el-icon>
                  <span class="q-text">{{ item.question || item.content }}</span>
                </div>
                <div class="consultation-a" v-if="item.reply || item.answer">
                  <el-icon :size="18" color="#67c23a"><ChatDotSquare /></el-icon>
                  <span class="a-text">{{ item.reply || item.answer }}</span>
                </div>
                <div class="consultation-meta">
                  <span class="meta-user">{{ item.userName || item.nickname || '用户' }}</span>
                  <span class="meta-time">{{ item.createdAt || item.createTime }}</span>
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无咨询" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, getComments, getConsultations } from '@/api/product'
import request from '@/api/request'
import { ElMessage } from 'element-plus'
import { Picture, QuestionFilled, ChatDotSquare } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// State
const product = ref(null)
const loading = ref(false)
const currentImage = ref('')
const quantity = ref(1)
const selectedSpecs = ref({})
const activeTab = ref('detail')

// Comments
const comments = ref([])
const commentsTotal = ref(0)
const commentsPage = ref(1)
const commentsLoading = ref(false)
const commentsLoaded = ref(false)

// Consultations
const consultations = ref([])
const consultationsLoading = ref(false)
const consultationsLoaded = ref(false)

// --- Image Parsing ---

const parseProductImages = (prod) => {
  if (!prod) return []

  // Try imageList (array of objects with url property)
  if (prod.imageList && Array.isArray(prod.imageList)) {
    const urls = prod.imageList.map((img) => {
      if (typeof img === 'string') return img
      return img.url || img.imageUrl || img.imgUrl || ''
    }).filter(Boolean)
    if (urls.length > 0) return urls
  }

  // Try images as array of strings
  if (Array.isArray(prod.images)) {
    const urls = prod.images.filter(Boolean)
    if (urls.length > 0) return urls
  }

  // Try images as JSON string
  if (typeof prod.images === 'string' && prod.images) {
    try {
      const parsed = JSON.parse(prod.images)
      if (Array.isArray(parsed)) {
        return parsed.filter(Boolean)
      }
    } catch (e) {
      // Not JSON, ignore
    }
  }

  // Fall back to single image field
  const single = prod.image || prod.coverImage || prod.cover_img
  if (single) return [single]

  return []
}

const imageList = computed(() => {
  return parseProductImages(product.value)
})

// Parsed comment images (may be JSON string or array)
const parsedCommentImages = (images) => {
  if (!images) return []
  if (Array.isArray(images)) return images.filter(Boolean)
  if (typeof images === 'string') {
    try {
      const parsed = JSON.parse(images)
      return Array.isArray(parsed) ? parsed.filter(Boolean) : []
    } catch (e) {
      return [images]
    }
  }
  return []
}

// --- Spec Handling ---

const specGroups = computed(() => {
  if (!product.value || !product.value.specList || !Array.isArray(product.value.specList)) {
    return []
  }
  const groups = {}
  product.value.specList.forEach((spec) => {
    const name = spec.name || spec.specName
    if (!name) return
    if (!groups[name]) groups[name] = []
    groups[name].push(spec)
  })
  return Object.entries(groups).map(([name, items]) => ({ name, items }))
})

const isSpecSelected = (groupName, spec) => {
  const current = selectedSpecs.value[groupName]
  if (!current) return false
  return (current.value || current.specValue) === (spec.value || spec.specValue)
}

const toggleSpec = (groupName, spec) => {
  const current = selectedSpecs.value[groupName]
  if (current && (current.value || current.specValue) === (spec.value || spec.specValue)) {
    // Deselect
    delete selectedSpecs.value[groupName]
  } else {
    selectedSpecs.value[groupName] = spec
  }
}

// --- Price Calculation ---

const originalPrice = computed(() => {
  const prod = product.value
  if (!prod) return 0
  return Number(prod.originalPrice ?? prod.original_price ?? prod.marketPrice ?? prod.market_price ?? 0)
})

const currentPrice = computed(() => {
  if (!product.value) return 0
  let price = Number(product.value.price) || 0
  // Add extra_price from selected specs
  Object.values(selectedSpecs.value).forEach((spec) => {
    const extra = Number(spec.extra_price || spec.extraPrice || 0)
    if (extra > 0) {
      price += extra
    }
  })
  return price
})

const maxQuantity = computed(() => {
  return Math.max(1, product.value?.stock || 99)
})

// --- Helpers ---

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

// --- Data Fetching ---

const fetchProductDetail = async () => {
  const id = route.params.id
  if (!id) return

  loading.value = true
  try {
    const res = await getProductDetail(id)
    const data = res.data || res
    product.value = data

    // Set initial image
    const images = parseProductImages(data)
    if (images.length > 0) {
      currentImage.value = images[0]
    }

    // Reset spec selection and quantity
    selectedSpecs.value = {}
    quantity.value = 1
  } catch (e) {
    console.error('Failed to load product detail:', e)
    ElMessage.error('加载商品信息失败')
  } finally {
    loading.value = false
  }
}

const fetchComments = async (page = 1) => {
  const id = route.params.id
  if (!id) return

  commentsLoading.value = true
  try {
    const res = await getComments(id, { page, pageSize: 10 })
    const data = res.data || res
    if (data && data.records) {
      comments.value = data.records
      commentsTotal.value = data.total
    } else if (Array.isArray(data)) {
      comments.value = data
      commentsTotal.value = data.length
    } else {
      comments.value = []
      commentsTotal.value = 0
    }
    commentsPage.value = page
    commentsLoaded.value = true
  } catch (e) {
    console.error('Failed to load comments:', e)
    comments.value = []
    commentsTotal.value = 0
  } finally {
    commentsLoading.value = false
  }
}

const fetchConsultations = async () => {
  const id = route.params.id
  if (!id) return

  consultationsLoading.value = true
  try {
    const res = await getConsultations(id)
    const data = res.data || res
    consultations.value = Array.isArray(data) ? data : []
    consultationsLoaded.value = true
  } catch (e) {
    console.error('Failed to load consultations:', e)
    consultations.value = []
  } finally {
    consultationsLoading.value = false
  }
}

// --- Handlers ---

const addToCart = async () => {
  const id = route.params.id
  if (!id) return

  try {
    await request.post('/order/cart/add', {
      productId: id,
      quantity: quantity.value,
    })
    ElMessage.success('已添加到购物车')
  } catch (e) {
    // Error handling is done by the request interceptor
    console.error('Failed to add to cart:', e)
  }
}

const handleTabChange = (tab) => {
  if (tab === 'comments' && !commentsLoaded.value) {
    fetchComments()
  } else if (tab === 'consultations' && !consultationsLoaded.value) {
    fetchConsultations()
  }
}

const handleCommentsPageChange = (page) => {
  fetchComments(page)
}

// --- Lifecycle ---

onMounted(async () => {
  await fetchProductDetail()

  // If comments data came with the product, use it
  if (product.value && product.value.comments) {
    const data = product.value.comments
    if (Array.isArray(data)) {
      comments.value = data
      commentsTotal.value = data.length
      commentsLoaded.value = true
    } else if (data.records) {
      comments.value = data.records
      commentsTotal.value = data.total
      commentsLoaded.value = true
    }
  }

  // If consultations came with the product, use it
  if (product.value && product.value.consultations) {
    const data = product.value.consultations
    if (Array.isArray(data)) {
      consultations.value = data
      consultationsLoaded.value = true
    }
  }
})
</script>

<style scoped>
.product-detail-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-top: 80px;
  padding-bottom: 60px;
}

.detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  display: flex;
  gap: 40px;
  align-items: flex-start;
}

/* Image Gallery */
.image-gallery {
  width: 440px;
  flex-shrink: 0;
}

.main-image {
  width: 440px;
  height: 440px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.main-image .el-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f5f5;
  width: 100%;
  height: 100%;
}

.thumbnail-list {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.thumbnail-item {
  width: 64px;
  height: 64px;
  border: 2px solid transparent;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s;
  background: #fff;
}

.thumbnail-item:hover {
  border-color: #409eff;
}

.thumbnail-item.active {
  border-color: #409eff;
}

.thumbnail-item .el-image {
  width: 100%;
  height: 100%;
}

/* Product Info */
.product-info {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.product-name {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
  line-height: 1.4;
}

/* Price Section */
.price-section {
  display: flex;
  align-items: baseline;
  gap: 12px;
  padding: 16px;
  background: #fef0f0;
  border-radius: 6px;
  margin-bottom: 16px;
}

.current-price {
  color: #f56c6c;
  font-weight: 700;
}

.current-price .symbol {
  font-size: 16px;
  margin-right: 2px;
}

.current-price .value {
  font-size: 28px;
}

.original-price s {
  font-size: 14px;
  color: #909399;
  text-decoration: line-through;
}

/* Description */
.product-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 16px;
}

/* Stock */
.stock-info {
  font-size: 14px;
  margin-bottom: 16px;
}

.stock-info .label {
  color: #909399;
}

.stock-available {
  color: #67c23a;
  font-weight: 500;
}

.stock-empty {
  color: #f56c6c;
  font-weight: 500;
}

/* Spec Section */
.spec-section {
  margin-bottom: 16px;
}

.spec-group {
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
}

.spec-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
  line-height: 32px;
  min-width: 60px;
}

.spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spec-selected {
  border-color: #409eff;
}

/* Quantity */
.quantity-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.qty-label {
  font-size: 14px;
  color: #606266;
}

.stock-hint {
  font-size: 12px;
  color: #909399;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 12px;
}

.action-buttons .el-button {
  min-width: 180px;
}

/* Bottom Tabs */
.detail-tabs {
  max-width: 1200px;
  margin: 30px auto 0;
  padding: 0 20px;
}

.detail-tabs :deep(.el-tabs__item) {
  font-size: 16px;
}

.tab-content {
  background: #fff;
  border-radius: 0 0 8px 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  min-height: 200px;
}

/* Rich Text */
.rich-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
}

.rich-text :deep(img) {
  max-width: 100%;
  height: auto;
}

/* Comment Item */
.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.comment-user {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.comment-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}

.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 10px;
}

.comment-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.comment-image {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #ebeef5;
}

.comment-pagination {
  display: flex;
  justify-content: center;
  padding-top: 16px;
}

/* Consultation Item */
.consultation-item {
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.consultation-item:last-child {
  border-bottom: none;
}

.consultation-q,
.consultation-a {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.q-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
}

.a-text {
  font-size: 14px;
  color: #67c23a;
  line-height: 1.5;
}

.consultation-meta {
  display: flex;
  gap: 16px;
  margin-left: 26px;
  font-size: 12px;
  color: #909399;
}

/* Responsive */
@media (max-width: 960px) {
  .detail-container {
    flex-direction: column;
    align-items: center;
  }

  .image-gallery {
    width: 100%;
    max-width: 440px;
  }

  .main-image {
    width: 100%;
    height: auto;
    aspect-ratio: 1;
  }

  .product-info {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .product-detail-page {
    padding-top: 70px;
  }

  .product-info {
    padding: 20px;
  }

  .product-name {
    font-size: 18px;
  }

  .current-price .value {
    font-size: 24px;
  }

  .action-buttons .el-button {
    min-width: 140px;
  }
}
</style>
