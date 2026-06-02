<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, addComment, getCommentList } from '@/api/product'
import { uploadCommentImage } from '@/api/comment'
import { addToCart } from '@/api/cart'
import { checkAllCart } from '@/api/cart'
import type { Product, CommentItem, CommentPageResult } from '@/api/product'
import { showToast } from '@/utils/toast'
import { useUserStore } from '@/stores/user'
import { useIntersectionObserver } from '@vueuse/core'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const product = ref<Product | null>(null)
const loading = ref(true)
const activeTab = ref('detail')

// Spec selection state
const selectedSpecs = ref<Record<string, string>>({})

const matchedSku = computed(() => {
  if (!product.value?.skuList?.length || !product.value?.specGroups?.length) return null
  const allSelected = product.value.specGroups.every(g => selectedSpecs.value[g.name])
  if (!allSelected) return null
  const selectedIds = product.value.specGroups.map(g => {
    const val = g.values.find(v => v.value === selectedSpecs.value[g.name])
    return val?.id
  }).filter((id): id is number => id != null)
  return product.value.skuList.find(sku =>
    sku.enabled &&
    selectedIds.length > 0 &&
    selectedIds.every(id => sku.specValueIdList?.includes(id))
  ) || null
})

const allSpecsSelected = computed(() => {
  if (!product.value?.specGroups?.length) return true
  return product.value.specGroups.every(g => selectedSpecs.value[g.name])
})

const quantity = ref(1)

// Gallery
const activeImageIndex = ref(0)
const thumbList = ref<string[]>([])

// Comments
const comments = ref<CommentItem[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentLoading = ref(false)
const commentEnd = ref(false)

// Cart interaction state
const cartAdding = ref(false)

// Add comment
const showCommentForm = ref(false)
const newComment = ref('')
const newRating = ref(5)
const submittingComment = ref(false)

// Review image upload
const commentImages = ref<string[]>([])
const uploadingImage = ref(false)

async function handleUploadImage(file: File): Promise<boolean> {
  if (commentImages.value.length >= 3) {
    showToast('最多上传 3 张图片', 'warning')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('图片大小不能超过 10MB', 'warning')
    return false
  }
  uploadingImage.value = true
  try {
    const url = await uploadCommentImage(file) as unknown as string
    commentImages.value.push(url)
    return true
  } catch {
    showToast('图片上传失败，请重试', 'error')
    return false
  } finally {
    uploadingImage.value = false
  }
}

function handleRemoveImage(index: number) {
  commentImages.value.splice(index, 1)
}

// Intersection for scroll animations
const infoRef = ref<HTMLElement | null>(null)
const infoVisible = ref(false)

// Computed price
const displayPrice = computed(() => {
  if (!product.value) return 0
  if (matchedSku.value?.price != null) return matchedSku.value.price
  return product.value.price
})

const displayStock = computed(() => {
  if (!product.value) return 0
  if (matchedSku.value) return matchedSku.value.stock
  if (!product.value.specGroups?.length && product.value.specList?.length) return 0
  if (!product.value.specGroups?.length) return product.value.stock
  return 0
})

// Price highlight on SKU change
const priceHighlight = ref(false)
watch(displayPrice, () => {
  priceHighlight.value = true
  setTimeout(() => { priceHighlight.value = false }, 600)
})

const averageRating = computed(() => {
  if (!comments.value.length) return 0
  const sum = comments.value.reduce((s, c) => s + c.rating, 0)
  return (sum / comments.value.length).toFixed(1)
})

const ratingDistribution = computed(() => {
  const dist = [0, 0, 0, 0, 0]
  for (const c of comments.value) {
    if (c.rating >= 1 && c.rating <= 5) dist[c.rating - 1]++
  }
  return dist
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    router.replace('/product/list')
    return
  }
  try {
    // Load product first — show page ASAP
    product.value = await getProductDetail(id)
    buildSpecGroups()
    buildThumbList()
    loading.value = false

    // Load comments in background, don't block rendering
    await loadComments(true)

    if (route.query.orderId) {
      activeTab.value = 'comments'
      showCommentForm.value = true
    }
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
})

function buildSpecGroups() {
  if (!product.value?.specGroups) return
  for (const group of product.value.specGroups) {
    selectedSpecs.value[group.name] = group.values[0]?.value ?? ''
  }
  quantity.value = 1
}

function buildThumbList() {
  if (!product.value) return
  thumbList.value = [product.value.mainImage]
  if (product.value.imageList?.length) {
    for (const img of product.value.imageList) {
      const url = typeof img === 'string' ? img : (img as any).url
      if (url && !thumbList.value.includes(url)) thumbList.value.push(url)
    }
  }
}

function selectSpec(groupName: string, specValue: string) {
  selectedSpecs.value[groupName] = specValue
  quantity.value = 1
  if (matchedSku.value?.image) {
    const idx = thumbList.value.findIndex(t => t === matchedSku.value!.image)
    if (idx >= 0) activeImageIndex.value = idx
  }
}

function getSpecInfo(): string {
  if (!product.value?.specGroups) return ''
  return product.value.specGroups.map(g => `${g.name}: ${selectedSpecs.value[g.name] || ''}`).join('; ')
}

async function handleAddToCart() {
  if (!product.value || !allSpecsSelected.value || cartAdding.value) return
  cartAdding.value = true
  try {
    await addToCart({
      productId: product.value.id,
      quantity: quantity.value,
      specInfo: getSpecInfo(),
      skuId: matchedSku.value?.id
    })
    showToast('已加入购物车', 'success')
    // Brief added state
    await new Promise(r => setTimeout(r, 400))
  } catch (e: any) {
    showToast(e?.message || '加入购物车失败', 'error')
  }
  finally { cartAdding.value = false }
}

async function handleBuyNow() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await checkAllCart(0)
    await addToCart({ productId: product.value.id, quantity: quantity.value, specInfo: getSpecInfo(), skuId: matchedSku.value?.id })
    router.push('/order/create')
  } catch (e: any) {
    showToast(e?.message || '创建订单失败', 'error')
  }
}

async function loadComments(reset = false) {
  const productId = Number(route.params.id)
  if (!productId || commentLoading.value) return
  const page = reset ? 1 : commentPage.value + 1
  commentLoading.value = true
  try {
    const res = await getCommentList(productId, page, 10) as unknown as CommentPageResult
    if (reset) {
      comments.value = res.records
    } else {
      comments.value = [...comments.value, ...res.records]
    }
    commentTotal.value = res.total
    commentPage.value = page
    commentEnd.value = comments.value.length >= res.total
  } catch { /* handled */ }
  finally { commentLoading.value = false }
}

function loadMore() {
  if (!commentEnd.value && !commentLoading.value) loadComments()
}

async function handleSubmitComment() {
  if (!newComment.value.trim()) {
    showToast('请输入评论内容', 'warning')
    return
  }
  if (uploadingImage.value) {
    showToast('请等待图片上传完成', 'warning')
    return
  }
  const productId = Number(route.params.id)
  if (!productId) return
  submittingComment.value = true
  try {
    await addComment({
      productId,
      content: newComment.value.trim(),
      rating: newRating.value,
      imageUrls: commentImages.value.length > 0 ? commentImages.value.join(',') : undefined
    })
    showToast('评论成功', 'success')
    newComment.value = ''
    commentImages.value = []
    newRating.value = 5
    showCommentForm.value = false
    await loadComments(true)
  } catch { /* handled */ }
  finally { submittingComment.value = false }
}

function renderStars(rating: number) {
  const full = '★'.repeat(rating)
  const empty = '☆'.repeat(5 - rating)
  return full + empty
}
</script>

<template>
  <div class="pdp">
    <!-- Loading -->
    <div v-if="loading" class="pdp__loading">
      <div class="pdp__loading-pulse"></div>
    </div>

    <template v-else-if="product">
      <!-- Hero Section -->
      <div class="pdp__hero">
        <div class="pdp__hero-bg"></div>
        <div class="pdp__hero-inner">
          <!-- Gallery -->
          <div class="pdp__gallery">
            <div class="pdp__gallery-main" @click="router.push('/product/list')">
              <img :src="thumbList[activeImageIndex] || product.mainImage" :alt="product.name">
              <div class="pdp__gallery-nav">
                <span v-if="activeImageIndex > 0" class="pdp__gallery-arrow pdp__gallery-arrow--prev" @click.stop="activeImageIndex--">←</span>
                <span v-if="activeImageIndex < thumbList.length - 1" class="pdp__gallery-arrow pdp__gallery-arrow--next" @click.stop="activeImageIndex++">→</span>
              </div>
            </div>
            <div v-if="thumbList.length > 1" class="pdp__gallery-thumbs">
              <div
                v-for="(img, i) in thumbList"
                :key="i"
                class="pdp__thumb"
                :class="{ 'pdp__thumb--active': activeImageIndex === i }"
                @click="activeImageIndex = i"
              >
                <img :src="img" :alt="product.name + ' ' + i">
              </div>
            </div>
          </div>

          <!-- Info Panel -->
          <div ref="infoRef" class="pdp__info-panel">
            <nav class="pdp__breadcrumb">
              <span @click="router.push('/')">首页</span>
              <span class="pdp__breadcrumb-sep">/</span>
              <span @click="router.push('/product/list')">商品列表</span>
              <span class="pdp__breadcrumb-sep">/</span>
              <span class="pdp__breadcrumb-current">{{ product.name }}</span>
            </nav>

            <h1 class="pdp__name">{{ product.name }}</h1>

            <!-- Price Section -->
            <div class="pdp__price-section">
              <div class="pdp__price-main">
                <span class="pdp__price-current" :class="{ 'pdp__price-current--highlight': priceHighlight }">¥{{ displayPrice }}</span>
                <span v-if="product.originalPrice" class="pdp__price-original">¥{{ product.originalPrice }}</span>
              </div>
              <span v-if="product.tag" class="pdp__price-tag">{{ product.tag }}</span>
            </div>

            <!-- Trust Signals -->
            <div class="pdp__trust">
              <div class="pdp__trust-item">
                <svg class="pdp__trust-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/></svg>
                <span class="pdp__trust-label">月销</span>
                <span class="pdp__trust-value">{{ product.sales > 999 ? (product.sales / 1000).toFixed(1) + 'k' : product.sales }}</span>
              </div>
              <div class="pdp__trust-divider"></div>
              <div class="pdp__trust-item">
                <svg class="pdp__trust-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
                <span class="pdp__trust-label">评分</span>
                <span class="pdp__trust-value">{{ averageRating || '-' }}</span>
              </div>
              <div class="pdp__trust-divider"></div>
              <div class="pdp__trust-item">
                <svg class="pdp__trust-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                <span class="pdp__trust-label">库存</span>
                <span class="pdp__trust-value" :class="{ 'pdp__trust-value--low': displayStock < 10 }">{{ displayStock }}</span>
              </div>
            </div>

            <!-- Section Divider -->
            <div class="pdp__divider"></div>

            <!-- Description -->
            <p v-if="product.description" class="pdp__desc">{{ product.description }}</p>

            <!-- Specs -->
            <div v-if="product?.specGroups?.length" class="pdp__specs">
              <div v-for="group in product.specGroups" :key="group.name" class="pdp__spec-row">
                <span class="pdp__spec-label">{{ group.name }}</span>
                <div class="pdp__spec-options">
                  <button
                    v-for="val in group.values"
                    :key="val.id"
                    class="pdp__spec-option"
                    :class="{ 'pdp__spec-option--active': selectedSpecs[group.name] === val.value }"
                    @click="selectSpec(group.name, val.value)"
                  >
                    {{ val.value }}
                  </button>
                </div>
              </div>

              <!-- Selected spec summary -->
              <transition name="pdp-spec-summary">
                <div v-if="allSpecsSelected" class="pdp__spec-summary">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
                  <span>已选: {{ product.specGroups.map(g => `${g.name}: ${selectedSpecs[g.name]}`).join('，') }}</span>
                </div>
              </transition>
            </div>

            <!-- Quantity -->
            <div class="pdp__qty-row">
              <span class="pdp__qty-label">数量</span>
              <div class="pdp__qty-control">
                <button class="pdp__qty-btn" :disabled="quantity <= 1" @click="quantity > 1 && quantity--">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </button>
                <input class="pdp__qty-input" type="text" v-model.number="quantity" @blur="quantity = Math.max(1, Math.min(displayStock || 999, quantity || 1))" />
                <button class="pdp__qty-btn" :disabled="quantity >= displayStock" @click="quantity < displayStock && quantity++">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="5" y1="12" x2="19" y2="12"/><line x1="12" y1="5" x2="12" y2="19"/></svg>
                </button>
              </div>
              <span class="pdp__qty-hint">库存 {{ displayStock }} 件</span>
            </div>

            <!-- Divider before actions -->
            <div class="pdp__divider"></div>

            <!-- Actions -->
            <div class="pdp__actions">
              <button class="pdp__cart-btn" :class="{ 'pdp__cart-btn--adding': cartAdding }" :disabled="!allSpecsSelected || cartAdding" @click="handleAddToCart">
                <svg v-if="!cartAdding" class="pdp__cart-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
                <svg v-else class="pdp__cart-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                <span>{{ cartAdding ? '已加入' : '加入购物车' }}</span>
              </button>
              <button class="pdp__buy-btn" :disabled="!allSpecsSelected" @click="handleBuyNow">立即购买</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Details + Comments -->
      <div class="pdp__content">
        <!-- Tab Bar -->
        <div class="pdp__tabs">
          <button class="pdp__tab" :class="{ 'pdp__tab--active': activeTab === 'detail' }" @click="activeTab = 'detail'">商品详情</button>
          <button class="pdp__tab" :class="{ 'pdp__tab--active': activeTab === 'comments' }" @click="activeTab = 'comments'">
            商品评价
            <span v-if="commentTotal" class="pdp__tab-count">{{ commentTotal }}</span>
          </button>
        </div>

        <!-- Detail Tab -->
        <transition name="pdp-fade" mode="out-in">
          <div v-show="activeTab === 'detail'" class="pdp__detail" key="detail">
            <div class="pdp__detail-text">{{ product.description }}</div>
            <div v-if="product.imageList?.length" class="pdp__detail-images">
              <img v-for="(img, i) in product.imageList" :key="i" :src="typeof img === 'string' ? img : img.url" :alt="product.name">
            </div>
          </div>
        </transition>

        <!-- Comments Tab -->
        <transition name="pdp-fade" mode="out-in">
          <div v-show="activeTab === 'comments'" class="pdp__comments" key="comments">
          <!-- Summary -->
          <div v-if="comments.length" class="pdp__comments-summary">
            <div class="pdp__comments-score">
              <span class="pdp__comments-score-num">{{ averageRating }}</span>
              <span class="pdp__comments-score-label">综合评分</span>
              <div class="pdp__comments-stars">{{ renderStars(Math.round(+averageRating)) }}</div>
            </div>
            <div class="pdp__comments-bars">
              <div v-for="i in 5" :key="i" class="pdp__comments-bar-row">
                <span class="pdp__comments-bar-label">{{ 6 - i }}星</span>
                <div class="pdp__comments-bar-track">
                  <div class="pdp__comments-bar-fill" :style="{ width: commentTotal ? (ratingDistribution[5 - i] / commentTotal * 100) + '%' : '0%' }"></div>
                </div>
                <span class="pdp__comments-bar-num">{{ ratingDistribution[5 - i] }}</span>
              </div>
            </div>
          </div>

          <!-- Add Comment -->
          <div class="pdp__comments-add">
            <button v-if="!showCommentForm" class="pdp__comments-add-btn" @click="showCommentForm = true" :disabled="!userStore.isLoggedIn">
              {{ userStore.isLoggedIn ? '写评价' : '登录后评价' }}
            </button>
            <div v-else class="pdp__comments-form">
              <div class="pdp__comments-form-rating">
                <span class="pdp__comments-form-label">评分</span>
                <div class="pdp__comments-star-select">
                  <span v-for="i in 5" :key="i" class="pdp__comments-star-option" :class="{ 'pdp__comments-star-option--active': i <= newRating }" @click="newRating = i">{{ i <= newRating ? '★' : '☆' }}</span>
                </div>
              </div>
              <textarea v-model="newComment" class="pdp__comments-textarea" placeholder="分享使用体验..." rows="4" maxlength="500"></textarea>
              <!-- Image Upload -->
              <div class="pdp__comments-form-images">
                <div class="pdp__comments-form-image-list">
                  <div v-for="(url, i) in commentImages" :key="i" class="pdp__comments-form-image-item">
                    <img :src="url" class="pdp__comments-form-image-preview">
                    <button class="pdp__comments-form-image-remove" @click="handleRemoveImage(i)">×</button>
                  </div>
                  <label v-if="commentImages.length < 3" class="pdp__comments-form-image-upload"
                         :class="{ 'pdp__comments-form-image-upload--disabled': uploadingImage }">
                    <input type="file" accept="image/png,image/jpeg,image/webp"
                           :disabled="uploadingImage"
                           @change="async (e) => {
                             const file = (e.target as HTMLInputElement).files?.[0]
                             if (file) {
                               await handleUploadImage(file)
                               ;(e.target as HTMLInputElement).value = ''
                             }
                           }">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                      <circle cx="8.5" cy="8.5" r="1.5"/>
                      <polyline points="21 15 16 10 5 21"/>
                    </svg>
                    <span>{{ uploadingImage ? '上传中...' : '上传图片' }}</span>
                  </label>
                </div>
                <span class="pdp__comments-form-image-hint">{{ commentImages.length }}/3 张</span>
              </div>
              <div class="pdp__comments-form-actions">
                <span class="pdp__comments-form-count">{{ newComment.length }}/500</span>
                <div class="pdp__comments-form-btns">
                  <button class="pdp__comments-form-cancel" @click="showCommentForm = false; commentImages = []">取消</button>
                  <button class="pdp__comments-form-submit" :disabled="submittingComment || uploadingImage || !newComment.trim()" @click="handleSubmitComment">
                    {{ submittingComment ? '提交中...' : '提交评价' }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Comment List -->
          <div v-if="comments.length" class="pdp__comments-list">
            <div v-for="comment in comments" :key="comment.id" class="pdp__comment-card">
              <div class="pdp__comment-header">
                <div class="pdp__comment-user">
                  <div v-if="comment.userAvatar" class="pdp__comment-avatar">
                    <img :src="comment.userAvatar" :alt="comment.userNickname">
                  </div>
                  <div v-else class="pdp__comment-avatar pdp__comment-avatar--empty">
                    {{ comment.userNickname.charAt(0) }}
                  </div>
                  <span class="pdp__comment-nickname">{{ comment.userNickname }}</span>
                </div>
                <div class="pdp__comment-stars">{{ renderStars(comment.rating) }}</div>
              </div>
              <p class="pdp__comment-content">{{ comment.content }}</p>
              <div v-if="comment.imageUrls" class="pdp__comment-images">
                <el-image
                  v-for="(url, i) in comment.imageUrls.split(',')"
                  :key="i"
                  :src="url"
                  :preview-src-list="comment.imageUrls!.split(',')"
                  :initial-index="i"
                  class="pdp__comment-image-el"
                  fit="cover"
                />
              </div>
              <span class="pdp__comment-time">{{ comment.createdTime?.slice(0, 10) }}</span>
            </div>

            <!-- Load More -->
            <div v-if="!commentEnd" class="pdp__comments-more">
              <button :disabled="commentLoading" @click="loadMore">
                {{ commentLoading ? '加载中...' : '加载更多' }}
              </button>
            </div>
            <div v-else class="pdp__comments-end">— 全部 {{ commentTotal }} 条评价 —</div>
          </div>

          <div v-else-if="!commentLoading" class="pdp__comments-empty">
            <p>暂无评价</p>
          </div>
        </div>
      </transition>
    </div>
    </template>
  </div>
</template>

<style scoped>
/* ── Reset ── */
.pdp button {
  border: none;
  background: none;
  cursor: pointer;
  font: inherit;
  color: inherit;
  padding: 0;
}

/* ── Page Container ── */
.pdp {
  min-height: 100vh;
  background: var(--wz-bg);
  color: var(--wz-text);
}

/* ── Loading ── */
.pdp__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.pdp__loading-pulse {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--wz-orange);
  animation: pulse 1.2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 0.3; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.1); }
}

/* ── Hero ── */
.pdp__hero {
  position: relative;
  overflow: hidden;
}
.pdp__hero-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse 80% 60% at 30% 40%, var(--wz-bg-card) 0%, var(--wz-bg) 70%);
  pointer-events: none;
}
.pdp__hero-inner {
  position: relative;
  max-width: 1280px;
  margin: 0 auto;
  padding: 80px 40px 60px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 60px;
  align-items: start;
}

/* ── Gallery ── */
.pdp__gallery {
  position: sticky;
  top: 88px;
}
.pdp__gallery-main {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 5;
  border-radius: 20px;
  overflow: hidden;
  background: var(--wz-bg-card);
  box-shadow: var(--wz-shadow-xl);
}
.pdp__gallery-main img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: opacity 0.4s ease;
}
.pdp__gallery-nav {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  pointer-events: none;
}
.pdp__gallery-arrow {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--wz-bg-hover);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--wz-text);
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.25s;
}
.pdp__gallery-arrow:hover {
  background: var(--wz-bg-hover);
  opacity: 0.9;
  transform: scale(1.08);
}
.pdp__gallery-thumbs {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  overflow-x: auto;
  padding-bottom: 4px;
}
.pdp__gallery-thumbs::-webkit-scrollbar {
  height: 3px;
}
.pdp__gallery-thumbs::-webkit-scrollbar-thumb {
  background: var(--wz-orange);
  border-radius: 99px;
}
.pdp__thumb {
  flex-shrink: 0;
  width: 64px;
  height: 64px;
  border-radius: 10px;
  overflow: hidden;
  border: 2px solid transparent;
  cursor: pointer;
  opacity: 0.5;
  transition: all 0.25s;
}
.pdp__thumb:hover { opacity: 0.8; }
.pdp__thumb--active {
  opacity: 1;
  border-color: var(--wz-orange);
}
.pdp__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ── Breadcrumb ── */
.pdp__breadcrumb {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin-bottom: 16px;
  letter-spacing: 0.3px;
}
.pdp__breadcrumb span { cursor: pointer; transition: color 0.2s var(--wz-ease-out); }
.pdp__breadcrumb span:hover { color: var(--wz-orange); }
.pdp__breadcrumb-sep { margin: 0 8px; color: var(--wz-border); cursor: default; }
.pdp__breadcrumb-current { color: var(--wz-text-muted); cursor: default; }

/* ── Info Panel ── */
.pdp__info-panel {
  padding-top: 12px;
}
.pdp__name {
  font-family: var(--wz-font-display);
  font-size: 28px;
  font-weight: 600;
  color: var(--wz-text);
  line-height: 1.35;
  margin-bottom: 24px;
  letter-spacing: 0.02em;
}

/* ── Price Section ── */
.pdp__price-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.pdp__price-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.pdp__price-current {
  font-size: 36px;
  font-weight: 700;
  color: var(--wz-orange);
  letter-spacing: -0.5px;
  line-height: 1;
  transition: transform 0.3s var(--wz-ease-out);
}
.pdp__price-current--highlight {
  animation: price-pulse 0.6s var(--wz-ease-out);
}
@keyframes price-pulse {
  0% { transform: scale(1); }
  25% { transform: scale(1.06); }
  60% { transform: scale(0.98); }
  100% { transform: scale(1); }
}
.pdp__price-original {
  font-size: 14px;
  color: var(--wz-text-muted);
  text-decoration: line-through;
}
.pdp__price-tag {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--wz-orange);
  padding: 3px 10px;
  border-radius: 99px;
  letter-spacing: 0.5px;
  flex-shrink: 0;
}

/* ── Trust Signals ── */
.pdp__trust {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 24px;
  background: var(--wz-bg-card);
  border-radius: 12px;
  padding: 14px 0;
}
.pdp__trust-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.pdp__trust-icon {
  color: var(--wz-text-muted);
  margin-bottom: 2px;
}
.pdp__trust-label {
  font-size: 11px;
  color: var(--wz-text-muted);
  letter-spacing: 0.5px;
}
.pdp__trust-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-text);
}
.pdp__trust-value--low {
  color: var(--wz-danger);
}
.pdp__trust-divider {
  width: 1px;
  height: 32px;
  background: var(--wz-border);
  flex-shrink: 0;
}

/* ── Section Divider ── */
.pdp__divider {
  height: 1px;
  background: var(--wz-border);
  margin: 20px 0;
}

/* ── Description ── */
.pdp__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.8;
}

/* ── Specs ── */
.pdp__specs {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.pdp__spec-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}
.pdp__spec-label {
  font-size: 13px;
  color: var(--wz-text-soft);
  min-width: 48px;
  line-height: 36px;
}
.pdp__spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pdp .pdp__spec-option {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  font-size: 13px;
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--wz-border);
  color: var(--wz-text-soft);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s var(--wz-ease-out);
}
.pdp .pdp__spec-option:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
  background: rgba(255, 107, 53, 0.06);
}
.pdp .pdp__spec-option--active {
  border-color: var(--wz-orange);
  background: var(--wz-orange);
  color: #fff;
  font-weight: 600;
}
.pdp .pdp__spec-option--active:hover {
  background: var(--wz-orange-dark);
  border-color: var(--wz-orange-dark);
}

/* Spec Summary */
.pdp__spec-summary {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--wz-orange);
  padding: 8px 14px;
  background: var(--wz-orange-muted);
  border-radius: 8px;
  line-height: 1.4;
}
.pdp-spec-summary-enter-active {
  transition: all 0.25s var(--wz-ease-out);
}
.pdp-spec-summary-leave-active {
  transition: all 0.15s ease-in;
}
.pdp-spec-summary-enter-from,
.pdp-spec-summary-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* ── Quantity ── */
.pdp__qty-row {
  display: flex;
  align-items: center;
  gap: 16px;
}
.pdp__qty-label {
  font-size: 13px;
  color: var(--wz-text-soft);
  min-width: 48px;
}
.pdp__qty-control {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--wz-bg-card);
}
.pdp__qty-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wz-text-soft);
  transition: background 0.2s, color 0.2s;
}
.pdp__qty-btn:hover:not(:disabled) {
  background: var(--wz-bg-elevated);
  color: var(--wz-orange);
}
.pdp__qty-btn:disabled { opacity: 0.25; cursor: not-allowed; }
.pdp__qty-input {
  width: 44px;
  text-align: center;
  font-size: 15px;
  color: var(--wz-text);
  background: transparent;
  border: none;
  border-left: 1px solid var(--wz-border);
  border-right: 1px solid var(--wz-border);
  line-height: 36px;
  height: 36px;
  font-family: inherit;
  outline: none;
  -moz-appearance: textfield;
  appearance: textfield;
}
.pdp__qty-input::-webkit-outer-spin-button,
.pdp__qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}
.pdp__qty-input:focus {
  color: var(--wz-orange);
}
.pdp__qty-hint { font-size: 12px; color: var(--wz-text-muted); }

/* ── Actions ── */
.pdp__actions {
  display: flex;
  gap: 12px;
}
.pdp__cart-btn, .pdp__buy-btn {
  flex: 1;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 26px;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.25s var(--wz-ease-out);
  cursor: pointer;
}
.pdp .pdp__cart-btn {
  border: 1.5px solid var(--wz-border);
  color: var(--wz-text-soft);
  background: transparent;
}
.pdp .pdp__cart-btn:hover:not(:disabled) {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
  background: var(--wz-orange-muted);
}
.pdp .pdp__cart-btn--adding {
  border-color: var(--wz-success) !important;
  color: var(--wz-success) !important;
  background: rgba(52, 199, 89, 0.08) !important;
  pointer-events: none;
}
.pdp__cart-icon,
.pdp__cart-check {
  transition: transform 0.3s var(--wz-ease-out);
}
.pdp__cart-btn--adding .pdp__cart-check {
  animation: cart-check-bounce 0.4s var(--wz-ease-out);
}
@keyframes cart-check-bounce {
  0% { transform: scale(0); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}
.pdp .pdp__buy-btn {
  background: var(--wz-orange);
  color: #fff;
  border: 1.5px solid var(--wz-orange);
}
.pdp .pdp__buy-btn:hover:not(:disabled) {
  background: var(--wz-orange-dark);
  border-color: var(--wz-orange-dark);
  transform: translateY(-1px);
  box-shadow: var(--wz-shadow-glow);
}
.pdp .pdp__cart-btn:disabled, .pdp .pdp__buy-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

/* ── Content ── */
.pdp__content {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 40px 80px;
}

/* ── Tabs ── */
.pdp__tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--wz-border);
  margin-bottom: 32px;
  padding: 0 4px;
}
.pdp__tab {
  position: relative;
  padding: 12px 28px;
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-muted);
  cursor: pointer;
  transition: all 0.25s ease;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px 8px 0 0;
  letter-spacing: 0.5px;
}
.pdp__tab:hover {
  color: var(--wz-text-soft);
  background: var(--wz-bg-hover);
}
.pdp__tab--active {
  color: var(--wz-orange);
  background: var(--wz-orange-muted);
}
.pdp__tab--active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--wz-orange);
  border-radius: 2px 2px 0 0;
  box-shadow: var(--wz-shadow-glow);
}
.pdp__tab-count {
  font-size: 11px;
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
  padding: 1px 7px;
  border-radius: 99px;
}

/* ── Tab Fade Transition ── */
.pdp-fade-enter-active,
.pdp-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.pdp-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.pdp-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ── Detail ── */
.pdp__detail {
  min-height: 200px;
}
.pdp__detail-text {
  font-size: 15px;
  color: var(--wz-text-soft);
  line-height: 1.9;
  margin-bottom: 32px;
}
.pdp__detail-images {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.pdp__detail-images img {
  width: 100%;
  border-radius: 12px;
}

/* ── Comments ── */
.pdp__comments {
  min-height: 200px;
}

/* Summary - no card, just a compact header */
.pdp__comments-summary {
  display: flex;
  gap: 32px;
  padding-bottom: 20px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--wz-border);
}
.pdp__comments-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 100px;
}
.pdp__comments-score-num {
  font-size: 42px;
  font-weight: 700;
  color: var(--wz-orange);
  line-height: 1;
}
.pdp__comments-score-label {
  font-size: 12px;
  color: var(--wz-text-muted);
}
.pdp__comments-stars {
  font-size: 16px;
  color: var(--wz-orange);
  letter-spacing: 2px;
}
.pdp__comments-bars { flex: 1; display: flex; flex-direction: column; gap: 6px; justify-content: center; }
.pdp__comments-bar-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pdp__comments-bar-label {
  font-size: 12px;
  color: var(--wz-text-soft);
  min-width: 28px;
}
.pdp__comments-bar-track {
  flex: 1;
  height: 4px;
  background: var(--wz-border);
  border-radius: 99px;
  overflow: hidden;
}
.pdp__comments-bar-fill {
  height: 100%;
  background: var(--wz-orange);
  border-radius: 99px;
  transition: width 0.6s ease;
}
.pdp__comments-bar-num {
  font-size: 12px;
  color: var(--wz-text-muted);
  min-width: 20px;
  text-align: right;
}

/* Add Comment */
.pdp__comments-add {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--wz-border-light);
}
.pdp__comments-add-btn {
  width: 100%;
  height: 48px;
  border: 1px solid var(--wz-border);
  border-radius: 10px;
  color: var(--wz-text-muted);
  font-size: 14px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
  letter-spacing: 0.3px;
}
.pdp__comments-add-btn:hover:not(:disabled) {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
  background: var(--wz-orange-muted);
}
.pdp__comments-add-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.pdp__comments-form {
  padding: 0;
}
.pdp__comments-form-rating {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.pdp__comments-form-label {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.pdp__comments-star-select {
  display: flex;
  gap: 4px;
}
.pdp__comments-star-option {
  font-size: 24px;
  color: var(--wz-text-muted);
  cursor: pointer;
  transition: color 0.2s, transform 0.15s;
}
.pdp__comments-star-option:hover { transform: scale(1.2); }
.pdp__comments-star-option--active { color: var(--wz-orange); }
.pdp__comments-textarea {
  width: 100%;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 10px;
  padding: 14px;
  color: var(--wz-text);
  font-size: 14px;
  line-height: 1.6;
  resize: vertical;
  font-family: inherit;
  transition: border-color 0.2s;
}
.pdp__comments-textarea:focus {
  outline: none;
  border-color: var(--wz-orange);
}
.pdp__comments-textarea::placeholder { color: var(--wz-text-muted); }
.pdp__comments-form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
.pdp__comments-form-count {
  font-size: 12px;
  color: var(--wz-text-muted);
}
.pdp__comments-form-btns {
  display: flex;
  gap: 10px;
}
.pdp__comments-form-cancel {
  height: 36px;
  padding: 0 20px;
  border-radius: 18px;
  font-size: 13px;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: color 0.2s;
}
.pdp__comments-form-cancel:hover { color: var(--wz-text-soft); }
.pdp__comments-form-submit {
  height: 36px;
  padding: 0 24px;
  border-radius: 18px;
  background: var(--wz-orange);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__comments-form-submit:hover:not(:disabled) {
  background: var(--wz-orange-dark);
}
.pdp__comments-form-submit:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* Image Upload */
.pdp__comments-form-images {
  margin-top: 12px;
}
.pdp__comments-form-image-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.pdp__comments-form-image-item {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--wz-border);
}
.pdp__comments-form-image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pdp__comments-form-image-remove {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 14px;
  line-height: 20px;
  text-align: center;
  cursor: pointer;
  border: none;
  padding: 0;
}
.pdp__comments-form-image-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border: 1px dashed var(--wz-border);
  border-radius: 8px;
  cursor: pointer;
  color: var(--wz-text-muted);
  font-size: 11px;
  gap: 4px;
  transition: all 0.2s;
}
.pdp__comments-form-image-upload:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.pdp__comments-form-image-upload--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.pdp__comments-form-image-upload input {
  display: none;
}
.pdp__comments-form-image-hint {
  display: block;
  font-size: 12px;
  color: var(--wz-text-muted);
  margin-top: 6px;
}

/* Comment List - no cards, border separation */
.pdp__comments-list {
  display: flex;
  flex-direction: column;
}
.pdp__comment-card {
  padding: 20px 0;
  border-bottom: 1px solid var(--wz-border-light);
}
.pdp__comment-card:last-child {
  border-bottom: none;
}
.pdp__comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.pdp__comment-user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.pdp__comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}
.pdp__comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pdp__comment-avatar--empty {
  background: var(--wz-bg-elevated);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: var(--wz-orange);
}
.pdp__comment-nickname {
  font-size: 14px;
  color: var(--wz-text-soft);
  font-weight: 500;
}
.pdp__comment-stars {
  font-size: 14px;
  color: var(--wz-orange);
  letter-spacing: 1px;
}
.pdp__comment-content {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.7;
  margin-bottom: 10px;
}
.pdp__comment-images {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.pdp__comment-image-el {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  cursor: pointer;
}
.pdp__comment-time {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* Load More & End */
.pdp__comments-more {
  text-align: center;
  padding: 24px 0 8px;
}
.pdp__comments-more button {
  height: 42px;
  padding: 0 36px;
  border: 1px solid var(--wz-border);
  border-radius: 21px;
  color: var(--wz-text-soft);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__comments-more button:hover:not(:disabled) {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.pdp__comments-more button:disabled { opacity: 0.4; cursor: not-allowed; }
.pdp__comments-end {
  text-align: center;
  padding: 20px 0;
  font-size: 12px;
  color: var(--wz-text-muted);
  letter-spacing: 2px;
}
.pdp__comments-empty {
  text-align: center;
  padding: 60px 0;
  color: var(--wz-text-muted);
  font-size: 14px;
}

/* ── Responsive ── */
@media (max-width: 900px) {
  .pdp__hero-inner {
    grid-template-columns: 1fr;
    gap: 32px;
    padding: 64px 20px 40px;
  }
  .pdp__gallery { position: static; }
  .pdp__gallery-main { aspect-ratio: 3 / 4; }
  .pdp__name { font-size: 24px; }
  .pdp__price-current { font-size: 30px; }
  .pdp__content { padding: 0 20px 60px; }
  .pdp__comments-summary { flex-direction: column; gap: 16px; align-items: center; }
}
</style>
