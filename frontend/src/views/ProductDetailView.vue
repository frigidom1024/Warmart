<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, addComment, getCommentList } from '@/api/product'
import { addToCart } from '@/api/cart'
import { checkAllCart } from '@/api/cart'
import type { Product, ProductSpec, CommentItem, CommentPageResult } from '@/api/product'
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
const specGroups = ref<{ name: string; values: ProductSpec[] }[]>([])
const selectedSpecs = ref<Record<string, string>>({})
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

// Add comment
const showCommentForm = ref(false)
const newComment = ref('')
const newRating = ref(5)
const submittingComment = ref(false)

// Intersection for scroll animations
const infoRef = ref<HTMLElement | null>(null)
const infoVisible = ref(false)

// Computed price
const displayPrice = computed(() => {
  if (!product.value) return 0
  let extra = 0
  for (const group of specGroups.value) {
    const selectedValue = selectedSpecs.value[group.name]
    if (selectedValue) {
      const spec = group.values.find(v => v.specValue === selectedValue)
      if (spec) extra += spec.extraPrice
    }
  }
  return product.value.price + extra
})

const displayStock = computed(() => {
  if (!product.value) return 0
  if (!specGroups.value.length) return product.value.stock
  let minStock = Infinity
  for (const group of specGroups.value) {
    const selectedValue = selectedSpecs.value[group.name]
    if (!selectedValue) return 0
    const spec = group.values.find(v => v.specValue === selectedValue)
    if (spec && spec.stock < minStock) minStock = spec.stock
  }
  return minStock === Infinity ? 0 : minStock
})

const allSpecsSelected = computed(() => {
  return specGroups.value.every(g => selectedSpecs.value[g.name])
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
    product.value = await getProductDetail(id)
    buildSpecGroups()
    buildThumbList()
    await loadComments(true)
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
})

function buildSpecGroups() {
  if (!product.value?.specList) return
  const groups: Record<string, ProductSpec[]> = {}
  for (const spec of product.value.specList) {
    if (!groups[spec.specName]) groups[spec.specName] = []
    groups[spec.specName].push(spec)
  }
  specGroups.value = Object.entries(groups).map(([name, values]) => ({ name, values }))
  for (const group of specGroups.value) {
    selectedSpecs.value[group.name] = group.values[0].specValue
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
}

function getSpecInfo(): string {
  return specGroups.value.map(g => `${g.name}: ${selectedSpecs.value[g.name] || ''}`).join('; ')
}

async function handleAddToCart() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await addToCart({ productId: product.value.id, quantity: quantity.value, specInfo: getSpecInfo() })
    showToast('已加入购物车', 'success')
  } catch { /* handled */ }
}

async function handleBuyNow() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await checkAllCart(0)
    await addToCart({ productId: product.value.id, quantity: quantity.value, specInfo: getSpecInfo() })
    router.push('/order/create')
  } catch { /* handled */ }
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
  const productId = Number(route.params.id)
  if (!productId) return
  submittingComment.value = true
  try {
    await addComment({ productId, content: newComment.value.trim(), rating: newRating.value })
    showToast('评论成功', 'success')
    newComment.value = ''
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

            <div class="pdp__price-block">
              <span class="pdp__price-current">¥{{ displayPrice }}</span>
              <span v-if="product.originalPrice" class="pdp__price-original">¥{{ product.originalPrice }}</span>
              <span v-if="product.tag" class="pdp__tag">{{ product.tag }}</span>
            </div>

            <div class="pdp__meta">
              <div class="pdp__meta-item">
                <span class="pdp__meta-num">{{ product.sales > 999 ? (product.sales / 1000).toFixed(1) + 'k' : product.sales }}</span>
                <span class="pdp__meta-label">月销</span>
              </div>
              <div class="pdp__meta-item">
                <span class="pdp__meta-num">{{ displayStock }}</span>
                <span class="pdp__meta-label">库存</span>
              </div>
              <div v-if="comments.length" class="pdp__meta-item">
                <span class="pdp__meta-num">{{ averageRating }}</span>
                <span class="pdp__meta-label">评分</span>
              </div>
            </div>

            <p class="pdp__desc">{{ product.description }}</p>

            <!-- Specs -->
            <div v-if="specGroups.length" class="pdp__specs">
              <div v-for="group in specGroups" :key="group.name" class="pdp__spec-row">
                <span class="pdp__spec-label">{{ group.name }}</span>
                <div class="pdp__spec-options">
                  <button
                    v-for="spec in group.values"
                    :key="spec.id"
                    class="pdp__spec-option"
                    :class="{ 'pdp__spec-option--active': selectedSpecs[group.name] === spec.specValue }"
                    @click="selectSpec(group.name, spec.specValue)"
                  >
                    {{ spec.specValue }}
                    <span v-if="spec.extraPrice > 0" class="pdp__spec-extra">+¥{{ spec.extraPrice }}</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- Quantity -->
            <div class="pdp__qty-row">
              <span class="pdp__qty-label">数量</span>
              <div class="pdp__qty-control">
                <button class="pdp__qty-btn" :disabled="quantity <= 1" @click="quantity > 1 && quantity--">−</button>
                <span class="pdp__qty-value">{{ quantity }}</span>
                <button class="pdp__qty-btn" :disabled="quantity >= displayStock" @click="quantity < displayStock && quantity++">+</button>
              </div>
              <span class="pdp__qty-hint">库存 {{ displayStock }} 件</span>
            </div>

            <!-- Actions -->
            <div class="pdp__actions">
              <button class="pdp__cart-btn" :disabled="!allSpecsSelected" @click="handleAddToCart">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
                加入购物车
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
              <div class="pdp__comments-form-actions">
                <span class="pdp__comments-form-count">{{ newComment.length }}/500</span>
                <div class="pdp__comments-form-btns">
                  <button class="pdp__comments-form-cancel" @click="showCommentForm = false">取消</button>
                  <button class="pdp__comments-form-submit" :disabled="submittingComment || !newComment.trim()" @click="handleSubmitComment">
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
                <img v-for="(url, i) in comment.imageUrls.split(',')" :key="i" :src="url" @click="activeImageIndex = i">
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
  margin-bottom: 20px;
  letter-spacing: 0.3px;
}
.pdp__breadcrumb span { cursor: pointer; transition: color 0.2s; }
.pdp__breadcrumb span:hover { color: var(--wz-orange); }
.pdp__breadcrumb-sep { margin: 0 8px; color: var(--wz-border); cursor: default; }
.pdp__breadcrumb-current { color: var(--wz-text-muted); cursor: default; }
.pdp__breadcrumb-current:hover { color: var(--wz-text-muted) !important; }

/* ── Info Panel ── */
.pdp__info-panel {
  padding-top: 12px;
}
.pdp__name {
  font-family: 'Noto Serif SC', 'Source Han Serif SC', serif;
  font-size: 28px;
  font-weight: 600;
  color: var(--wz-text);
  line-height: 1.35;
  margin-bottom: 20px;
  letter-spacing: 1px;
}

/* ── Price ── */
.pdp__price-block {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: var(--wz-orange-muted);
  border-radius: 12px;
}
.pdp__price-current {
  font-size: 36px;
  font-weight: 700;
  color: var(--wz-orange);
  letter-spacing: -0.5px;
}
.pdp__price-original {
  font-size: 15px;
  color: var(--wz-text-muted);
  text-decoration: line-through;
}
.pdp__tag {
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--wz-orange);
  padding: 3px 10px;
  border-radius: 99px;
  letter-spacing: 0.5px;
}

/* ── Meta ── */
.pdp__meta {
  display: flex;
  gap: 32px;
  margin-bottom: 20px;
  padding: 14px 0;
  border-top: 1px solid var(--wz-border);
  border-bottom: 1px solid var(--wz-border);
}
.pdp__meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pdp__meta-num {
  font-size: 17px;
  font-weight: 600;
  color: var(--wz-text);
}
.pdp__meta-label {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* ── Description ── */
.pdp__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.8;
  margin-bottom: 24px;
  padding: 14px 0;
  border-top: 1px solid var(--wz-border);
  border-bottom: 1px solid var(--wz-border);
}

/* ── Specs ── */
.pdp__specs { margin-bottom: 28px; }
.pdp__spec-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 14px;
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
.pdp__spec-option {
  font-size: 13px;
  padding: 7px 18px;
  border-radius: 8px;
  border: 1px solid var(--wz-border);
  color: var(--wz-text-soft);
  background: transparent;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__spec-option:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.pdp__spec-option--active {
  border-color: var(--wz-orange);
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
}
.pdp__spec-extra {
  font-size: 11px;
  color: var(--wz-orange);
  margin-left: 3px;
}

/* ── Quantity ── */
.pdp__qty-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
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
}
.pdp__qty-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: var(--wz-text-soft);
  transition: background 0.2s;
}
.pdp__qty-btn:hover:not(:disabled) { background: var(--wz-bg-elevated); }
.pdp__qty-btn:disabled { opacity: 0.25; cursor: not-allowed; }
.pdp__qty-value {
  width: 44px;
  text-align: center;
  font-size: 15px;
  color: var(--wz-text);
  border-left: 1px solid var(--wz-border);
  border-right: 1px solid var(--wz-border);
  line-height: 36px;
}
.pdp__qty-hint { font-size: 12px; color: var(--wz-text-muted); }

/* ── Actions ── */
.pdp__actions {
  display: flex;
  gap: 12px;
}
.pdp__cart-btn, .pdp__buy-btn {
  flex: 1;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 25px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.25s;
}
.pdp__cart-btn {
  border: 1px solid var(--wz-orange);
  color: var(--wz-orange);
  background: transparent;
}
.pdp__cart-btn:hover:not(:disabled) {
  background: var(--wz-orange-muted);
}
.pdp__buy-btn {
  background: var(--wz-orange);
  color: #fff;
  font-weight: 600;
}
.pdp__buy-btn:hover:not(:disabled) {
  background: var(--wz-orange-dark);
  transform: translateY(-1px);
  box-shadow: var(--wz-shadow-glow);
}
.pdp__cart-btn:disabled, .pdp__buy-btn:disabled {
  opacity: 0.35;
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

/* Summary */
.pdp__comments-summary {
  display: flex;
  gap: 40px;
  padding: 24px 28px;
  background: var(--wz-bg-card);
  border-radius: 16px;
  margin-bottom: 28px;
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
  height: 6px;
  background: var(--wz-bg-elevated);
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
  margin-bottom: 28px;
}
.pdp__comments-add-btn {
  width: 100%;
  height: 48px;
  border: 1px dashed var(--wz-border);
  border-radius: 12px;
  color: var(--wz-text-soft);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__comments-add-btn:hover:not(:disabled) {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.pdp__comments-add-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.pdp__comments-form {
  background: var(--wz-bg-card);
  border-radius: 16px;
  padding: 24px;
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

/* Comment List */
.pdp__comments-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.pdp__comment-card {
  background: var(--wz-bg-card);
  border-radius: 14px;
  padding: 20px 24px;
  transition: background 0.2s;
}
.pdp__comment-card:hover {
  background: var(--wz-bg-elevated);
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
.pdp__comment-images img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.2s;
}
.pdp__comment-images img:hover { transform: scale(1.05); }
.pdp__comment-time {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* Load More & End */
.pdp__comments-more {
  text-align: center;
  padding: 20px 0;
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
