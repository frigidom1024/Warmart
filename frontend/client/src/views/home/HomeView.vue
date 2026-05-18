<template>
  <div class="wz-home">
    <HeaderNav />

    <!-- ============ HERO ============ -->
    <section class="wz-hero">
      <div class="wz-hero__bg">
        <div class="wz-hero__shape wz-hero__shape--1" />
        <div class="wz-hero__shape wz-hero__shape--2" />
        <div class="wz-hero__shape wz-hero__shape--3" />
      </div>
      <div class="wz-hero__inner">
        <div class="wz-hero__content">
          <span class="wz-hero__badge">新品上市</span>
          <h1 class="wz-hero__title">
            <span class="wz-hero__title-line">发现生活的</span>
            <span class="wz-hero__title-line wz-hero__title-line--accent">温暖质感</span>
          </h1>
          <p class="wz-hero__desc">精选好物，用心生活 — 每一件都值得拥有</p>
          <div class="wz-hero__actions">
            <router-link to="/product/list" class="wz-hero__btn wz-hero__btn--primary">
              立即探索
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-left:6px">
                <line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/>
              </svg>
            </router-link>
            <router-link to="/service/notice" class="wz-hero__btn wz-hero__btn--ghost">
              了解我们
            </router-link>
          </div>
        </div>
        <div class="wz-hero__visual">
          <div class="wz-hero__carousel-wrapper">
            <div v-if="!pageLoading && banners.length > 0" class="wz-hero__carousel">
              <el-carousel
                height="460px"
                :interval="4000"
                indicator-position="none"
                arrow="never"
                :autoplay="true"
              >
                <el-carousel-item v-for="banner in banners" :key="banner.id">
                  <div class="wz-hero__slide" @click="handleBannerClick(banner)">
                    <div class="wz-hero__slide-img-wrapper">
                      <img
                        :src="banner.imageUrl || banner.imgUrl || banner.image"
                        :alt="banner.title || 'banner'"
                        class="wz-hero__slide-img"
                      />
                    </div>
                  </div>
                </el-carousel-item>
              </el-carousel>
            </div>
            <div v-else class="wz-hero__carousel-placeholder">
              <div class="wz-hero__placeholder-deco" />
              <div class="wz-hero__placeholder-text">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" color="#d4a373">
                  <rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/>
                </svg>
                <span>精彩即将呈现</span>
              </div>
            </div>
            <div class="wz-hero__carousel-indicators">
              <span
                v-for="(b, i) in banners"
                :key="b.id"
                class="wz-hero__dot"
                :class="{ 'is-active': i === heroSlideIndex }"
                @click="heroSlideIndex = i"
              />
            </div>
          </div>
          <div class="wz-hero__deco-card" />
        </div>
      </div>
    </section>

    <!-- ============ CATEGORIES ============ -->
    <section class="wz-section" ref="categorySection">
      <div class="wz-section__header">
        <h2 class="wz-section__title">全部品类</h2>
        <router-link to="/product/list" class="wz-section__link">
          查看全部 <span aria-hidden="true">→</span>
        </router-link>
      </div>
      <div v-if="pageLoading" class="wz-grid-skeleton">
        <div v-for="n in 8" :key="n" class="wz-skeleton-card" />
      </div>
      <div v-else-if="topCategories.length > 0" class="wz-category-grid">
        <router-link
          v-for="(cat, idx) in topCategories"
          :key="cat.id"
          :to="{ path: '/product/list', query: { categoryId: cat.id } }"
          class="wz-category-card fade-in-up"
          :class="[`stagger-${idx + 1}`, { 'visible': visibleCategories }]"
          :style="getCategoryStyle(idx)"
        >
          <span class="wz-category-card__icon">{{ categoryIcons[idx % categoryIcons.length] }}</span>
          <span class="wz-category-card__name">{{ cat.name }}</span>
          <span class="wz-category-card__count">{{ cat.count || '' }}</span>
        </router-link>
      </div>
      <el-empty v-else description="暂无分类" />
    </section>

    <!-- ============ FEATURED PRODUCTS ============ -->
    <section class="wz-section wz-section--alt" ref="productSection">
      <div class="wz-section__header">
        <h2 class="wz-section__title">推荐好物</h2>
        <router-link to="/product/list" class="wz-section__link">
          浏览更多 <span aria-hidden="true">→</span>
        </router-link>
      </div>

      <div v-if="pageLoading" class="wz-grid-skeleton">
        <div v-for="n in 8" :key="n" class="wz-skeleton-card" />
      </div>

      <!-- Featured hero product -->
      <div v-else-if="products.length > 0" class="wz-product-showcase">
        <router-link
          :to="`/product/detail/${products[0].id}`"
          class="wz-product-hero fade-in-up"
          :class="{ visible: visibleProducts }"
        >
          <div class="wz-product-hero__image">
            <img
              :src="productImage(products[0]) || '/placeholder.svg'"
              :alt="products[0].name"
            />
          </div>
          <div class="wz-product-hero__info">
            <span class="wz-product-hero__tag">精选推荐</span>
            <h3 class="wz-product-hero__name">{{ products[0].name }}</h3>
            <p class="wz-product-hero__desc">{{ products[0].description || '匠心之作，品质之选' }}</p>
            <span class="wz-product-hero__price">
              ¥{{ formatPrice(products[0].price) }}
            </span>
          </div>
        </router-link>

        <!-- Product grid -->
        <div class="wz-product-grid">
          <router-link
            v-for="(product, idx) in products.slice(1)"
            :key="product.id"
            :to="`/product/detail/${product.id}`"
            class="wz-product-card fade-in-up"
            :class="[`stagger-${idx + 1}`, { visible: visibleProducts }]"
          >
            <div class="wz-product-card__image">
              <img
                :src="productImage(product) || '/placeholder.svg'"
                :alt="product.name"
                loading="lazy"
              />
              <div class="wz-product-card__overlay">
                <span>查看详情</span>
              </div>
            </div>
            <div class="wz-product-card__body">
              <h3 class="wz-product-card__name">{{ product.name }}</h3>
              <div class="wz-product-card__footer">
                <span class="wz-product-card__price">¥{{ formatPrice(product.price) }}</span>
                <span v-if="product.sales !== undefined" class="wz-product-card__sales">
                  已售 {{ product.sales }}
                </span>
              </div>
            </div>
          </router-link>
        </div>
      </div>
      <el-empty v-else description="暂无推荐商品" />
    </section>

    <!-- ============ FEATURES STRIP ============ -->
    <section class="wz-features">
      <div class="wz-features__inner">
        <div v-for="(item, idx) in features" :key="idx" class="wz-features__item">
          <div class="wz-features__icon" v-html="item.icon" />
          <div class="wz-features__text">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ FOOTER ============ -->
    <footer class="wz-footer">
      <div class="wz-footer__inner">
        <div class="wz-footer__brand">
          <span class="wz-footer__logo">暖</span>
          <span class="wz-footer__name">Warmart 网上商城</span>
        </div>
        <div class="wz-footer__links">
          <router-link to="/">首页</router-link>
          <router-link to="/product/list">商品</router-link>
          <router-link to="/service/notice">公告</router-link>
          <router-link to="/service/consultation">咨询</router-link>
        </div>
        <div class="wz-footer__copy">© 2026 Warmart. All rights reserved.</div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getBanners, getCategories, getProducts } from '@/api/product'
import HeaderNav from '@/components/HeaderNav.vue'

const router = useRouter()

const banners = ref([])
const topCategories = ref([])
const products = ref([])
const pageLoading = ref(true)
const heroSlideIndex = ref(0)

const visibleCategories = ref(false)
const visibleProducts = ref(false)

const categoryIcons = ['🛋', '📱', '👗', '⌚', '🏃', '📚', '🎧', '🎮', '🧸', '💻', '👟', '🧥']

const features = [
  { icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>', title: '品质保障', desc: '严选好物，用心把关' },
  { icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>', title: '极速发货', desc: '下单后24小时内发出' },
  { icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/></svg>', title: '无忧退换', desc: '7天无理由退货' },
  { icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>', title: '贴心服务', desc: '专属客服在线答疑' },
]

let observerCategories = null
let observerProducts = null
let carouselTimer = null

onMounted(async () => {
  try {
    const [bannerRes, categoryRes, productRes] = await Promise.all([
      getBanners(),
      getCategories(),
      getProducts({ isRecommended: 1 })
    ])

    const bannerData = bannerRes.data || bannerRes
    banners.value = Array.isArray(bannerData) ? bannerData : []

    const categoryData = categoryRes.data || categoryRes
    if (Array.isArray(categoryData)) {
      topCategories.value = categoryData.filter(c => {
        const pid = c.parentId ?? c.parent_id ?? c.pid
        return pid === 0 || pid === null || pid === undefined
      }).slice(0, 12)
    }

    const productData = productRes.data || productRes
    if (Array.isArray(productData)) {
      products.value = productData
    } else if (productData && typeof productData === 'object') {
      products.value = productData.records || productData.list || productData.rows || []
    }
  } catch (e) {
    console.error('Home load error:', e)
  } finally {
    pageLoading.value = false
  }

  // Intersection Observers for scroll reveal
  const sectionEl = document.querySelector('.wz-section')
  if (sectionEl) {
    observerCategories = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) { visibleCategories.value = true; observerCategories?.disconnect() } },
      { threshold: 0.1 }
    )
    observerCategories.observe(sectionEl)
  }

  const altSection = document.querySelector('.wz-section--alt')
  if (altSection) {
    observerProducts = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) { visibleProducts.value = true; observerProducts?.disconnect() } },
      { threshold: 0.08 }
    )
    observerProducts.observe(altSection)
  }
})

onBeforeUnmount(() => {
  observerCategories?.disconnect()
  observerProducts?.disconnect()
  if (carouselTimer) clearInterval(carouselTimer)
})

const productImage = (product) => {
  return product.image || product.mainImage || product.coverImage || (product.images && product.images[0]) || ''
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

const bgColors = ['#f0ebe5', '#e8e0d8', '#f5e6d8', '#dce5e8', '#e8dce8', '#d8e8e0', '#f0e0d8', '#e0e8f0']
const getCategoryStyle = (idx) => ({
  backgroundColor: bgColors[idx % bgColors.length],
})
</script>

<style scoped>
/* ============ HOME ============ */
.wz-home {
  min-height: 100vh;
  background: var(--wz-bg);
  overflow-x: hidden;
}

/* ============ HERO ============ */
.wz-hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  padding: 0 24px;
  overflow: hidden;
}
.wz-hero__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.wz-hero__shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}
.wz-hero__shape--1 {
  width: 600px; height: 600px;
  background: rgba(192, 102, 74, 0.15);
  top: -200px; right: -100px;
  animation: float-slow 20s ease-in-out infinite alternate;
}
.wz-hero__shape--2 {
  width: 400px; height: 400px;
  background: rgba(212, 163, 115, 0.12);
  bottom: -100px; left: -100px;
  animation: float-slow 25s ease-in-out infinite alternate-reverse;
}
.wz-hero__shape--3 {
  width: 300px; height: 300px;
  background: rgba(61, 90, 109, 0.08);
  top: 40%; left: 30%;
  animation: float-slow 18s ease-in-out infinite alternate;
}

@keyframes float-slow {
  0% { transform: translate(0, 0) scale(1); }
  100% { transform: translate(60px, -40px) scale(1.15); }
}

.wz-hero__inner {
  max-width: var(--wz-container-wide);
  margin: 0 auto;
  width: 100%;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 64px;
  align-items: center;
  padding-top: 72px;
}

/* Hero Content */
.wz-hero__content {
  animation: hero-enter 1s ease-out;
}
@keyframes hero-enter {
  from { opacity: 0; transform: translateY(32px); }
  to { opacity: 1; transform: translateY(0); }
}
.wz-hero__badge {
  display: inline-block;
  padding: 6px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--wz-accent);
  background: var(--wz-accent-light);
  border-radius: 20px;
  letter-spacing: 1px;
  margin-bottom: 24px;
  text-transform: uppercase;
}
.wz-hero__title {
  font-family: var(--wz-font-display);
  font-size: clamp(36px, 5vw, 64px);
  line-height: 1.15;
  margin: 0 0 20px;
  color: var(--wz-text);
}
.wz-hero__title-line {
  display: block;
}
.wz-hero__title-line--accent {
  color: var(--wz-accent);
}
.wz-hero__desc {
  font-size: 18px;
  color: var(--wz-text-secondary);
  margin: 0 0 36px;
  line-height: 1.6;
}
.wz-hero__actions {
  display: flex;
  gap: 16px;
  align-items: center;
}
.wz-hero__btn {
  display: inline-flex;
  align-items: center;
  padding: 14px 32px;
  font-size: 15px;
  font-weight: 500;
  border-radius: 30px;
  text-decoration: none;
  transition: all 0.3s ease;
  cursor: pointer;
}
.wz-hero__btn--primary {
  background: var(--wz-accent);
  color: white;
  box-shadow: 0 8px 24px rgba(192, 102, 74, 0.3);
}
.wz-hero__btn--primary:hover {
  background: var(--wz-accent-hover);
  transform: translateY(-2px);
  box-shadow: 0 12px 32px rgba(192, 102, 74, 0.4);
}
.wz-hero__btn--ghost {
  color: var(--wz-text-secondary);
  background: transparent;
  border: 1.5px solid var(--wz-border);
}
.wz-hero__btn--ghost:hover {
  border-color: var(--wz-accent);
  color: var(--wz-accent);
}

/* Hero Visual / Carousel */
.wz-hero__visual {
  position: relative;
  animation: hero-enter 1s ease-out 0.2s both;
}
.wz-hero__carousel-wrapper {
  position: relative;
  z-index: 2;
}
.wz-hero__carousel {
  border-radius: var(--wz-radius-lg);
  overflow: hidden;
  box-shadow: var(--wz-shadow-lg);
  background: var(--wz-bg-card);
}
.wz-hero__slide {
  width: 100%;
  height: 100%;
  cursor: pointer;
  overflow: hidden;
}
.wz-hero__slide-img-wrapper {
  width: 100%;
  height: 100%;
}
.wz-hero__slide-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}
.wz-hero__slide:hover .wz-hero__slide-img {
  transform: scale(1.04);
}
.wz-hero__carousel-placeholder {
  width: 100%;
  height: 460px;
  border-radius: var(--wz-radius-lg);
  background: var(--wz-bg-alt);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}
.wz-hero__placeholder-deco {
  position: absolute;
  width: 200px; height: 200px;
  border-radius: 50%;
  background: rgba(212, 163, 115, 0.1);
  top: -50px; right: -50px;
}
.wz-hero__placeholder-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--wz-text-muted);
  font-size: 14px;
}
.wz-hero__carousel-indicators {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}
.wz-hero__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--wz-border);
  cursor: pointer;
  transition: all 0.3s ease;
}
.wz-hero__dot.is-active {
  width: 24px;
  border-radius: 4px;
  background: var(--wz-accent);
}
.wz-hero__deco-card {
  position: absolute;
  width: 100%;
  height: 100%;
  border: 2px solid var(--wz-border);
  border-radius: var(--wz-radius-lg);
  top: 16px;
  left: 16px;
  z-index: 0;
  opacity: 0.5;
}

/* ============ SECTIONS ============ */
.wz-section {
  max-width: var(--wz-container-wide);
  margin: 0 auto;
  padding: 80px 24px;
}
.wz-section--alt {
  padding-top: 40px;
}
.wz-section__header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 40px;
}
.wz-section__title {
  font-family: var(--wz-font-display);
  font-size: 28px;
  font-weight: 600;
  color: var(--wz-text);
  margin: 0;
  letter-spacing: 1px;
}
.wz-section__link {
  font-size: 14px;
  color: var(--wz-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.wz-section__link:hover {
  color: var(--wz-accent);
}

/* ============ CATEGORIES ============ */
.wz-category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 16px;
}
.wz-category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 28px 16px;
  border-radius: var(--wz-radius);
  text-decoration: none;
  transition: all 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
}
.wz-category-card::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: var(--wz-radius);
  border: 1px solid transparent;
  transition: border-color 0.3s;
}
.wz-category-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--wz-shadow-md);
}
.wz-category-card:hover::after {
  border-color: rgba(192, 102, 74, 0.15);
}
.wz-category-card__icon {
  font-size: 32px;
  line-height: 1;
}
.wz-category-card__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  text-align: center;
}
.wz-category-card__count {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* ============ PRODUCTS ============ */
.wz-product-showcase {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Featured hero product */
.wz-product-hero {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  background: var(--wz-bg-card);
  border-radius: var(--wz-radius-lg);
  overflow: hidden;
  box-shadow: var(--wz-shadow-sm);
  text-decoration: none;
  transition: all 0.4s ease;
}
.wz-product-hero:hover {
  box-shadow: var(--wz-shadow-md);
  transform: translateY(-4px);
}
.wz-product-hero__image {
  height: 400px;
  overflow: hidden;
  background: var(--wz-bg-alt);
}
.wz-product-hero__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}
.wz-product-hero:hover .wz-product-hero__image img {
  transform: scale(1.05);
}
.wz-product-hero__info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 48px 48px 0;
}
.wz-product-hero__tag {
  display: inline-block;
  padding: 4px 12px;
  font-size: 11px;
  font-weight: 600;
  color: var(--wz-accent-gold);
  background: rgba(212, 163, 115, 0.1);
  border-radius: 4px;
  letter-spacing: 2px;
  text-transform: uppercase;
  margin-bottom: 16px;
  width: fit-content;
}
.wz-product-hero__name {
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin: 0 0 12px;
  line-height: 1.3;
}
.wz-product-hero__desc {
  font-size: 15px;
  color: var(--wz-text-secondary);
  margin: 0 0 24px;
  line-height: 1.6;
}
.wz-product-hero__price {
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-accent);
}

/* Product grid */
.wz-product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}
.wz-product-card {
  background: var(--wz-bg-card);
  border-radius: var(--wz-radius);
  overflow: hidden;
  text-decoration: none;
  box-shadow: var(--wz-shadow-sm);
  transition: all 0.35s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.wz-product-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--wz-shadow-md);
}
.wz-product-card__image {
  position: relative;
  padding-top: 100%;
  background: var(--wz-bg-alt);
  overflow: hidden;
}
.wz-product-card__image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s ease;
}
.wz-product-card:hover .wz-product-card__image img {
  transform: scale(1.08);
}
.wz-product-card__overlay {
  position: absolute;
  inset: 0;
  background: rgba(44, 36, 32, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}
.wz-product-card:hover .wz-product-card__overlay {
  opacity: 1;
}
.wz-product-card__overlay span {
  color: white;
  font-size: 14px;
  font-weight: 500;
  padding: 8px 20px;
  border: 1.5px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  backdrop-filter: blur(4px);
}
.wz-product-card__body {
  padding: 16px;
}
.wz-product-card__name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin: 0 0 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.wz-product-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.wz-product-card__price {
  font-size: 18px;
  font-weight: 700;
  color: var(--wz-accent);
}
.wz-product-card__sales {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* ============ SKELETON ============ */
.wz-grid-skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 16px;
}
.wz-skeleton-card {
  aspect-ratio: 1;
  border-radius: var(--wz-radius);
  background: linear-gradient(135deg, var(--wz-bg-alt) 25%, var(--wz-border-light) 50%, var(--wz-bg-alt) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ============ FEATURES ============ */
.wz-features {
  background: var(--wz-bg-alt);
  padding: 48px 24px;
}
.wz-features__inner {
  max-width: var(--wz-container-wide);
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 32px;
}
.wz-features__item {
  display: flex;
  align-items: center;
  gap: 16px;
}
.wz-features__icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(192, 102, 74, 0.08);
  border-radius: 12px;
  color: var(--wz-accent);
  flex-shrink: 0;
}
.wz-features__text {
  display: flex;
  flex-direction: column;
}
.wz-features__text strong {
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 2px;
}
.wz-features__text span {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* ============ FOOTER ============ */
.wz-footer {
  padding: 40px 24px;
  background: var(--wz-bg-card);
  border-top: 1px solid var(--wz-border-light);
}
.wz-footer__inner {
  max-width: var(--wz-container-wide);
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.wz-footer__brand {
  display: flex;
  align-items: center;
  gap: 10px;
}
.wz-footer__logo {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-accent);
  color: white;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 700;
  font-family: var(--wz-font-display);
}
.wz-footer__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--wz-text);
}
.wz-footer__links {
  display: flex;
  gap: 24px;
}
.wz-footer__links a {
  font-size: 13px;
  color: var(--wz-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
}
.wz-footer__links a:hover {
  color: var(--wz-accent);
}
.wz-footer__copy {
  font-size: 12px;
  color: var(--wz-text-muted);
}

/* ============ RESPONSIVE ============ */
@media (max-width: 1024px) {
  .wz-hero__inner {
    grid-template-columns: 1fr;
    gap: 40px;
    padding-top: 96px;
  }
  .wz-hero__content {
    text-align: center;
  }
  .wz-hero__actions {
    justify-content: center;
  }
  .wz-product-hero {
    grid-template-columns: 1fr;
  }
  .wz-product-hero__info {
    padding: 0 24px 24px;
  }
  .wz-features__inner {
    grid-template-columns: repeat(2, 1fr);
  }
  .wz-footer__inner {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
}

@media (max-width: 640px) {
  .wz-hero { min-height: auto; padding-top: 72px; }
  .wz-section { padding: 48px 16px; }
  .wz-category-grid { grid-template-columns: repeat(4, 1fr); gap: 12px; }
  .wz-category-card { padding: 20px 8px; }
  .wz-product-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .wz-features__inner { grid-template-columns: 1fr; }
  .wz-footer__links { flex-wrap: wrap; justify-content: center; }
}
</style>
