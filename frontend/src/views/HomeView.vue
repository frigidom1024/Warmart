<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryList, getProductList } from '@/api/product'
import { addFavorite } from '@/api/favorite'
import { showToast } from '@/utils/toast'
import type { Category as ApiCategory, Product } from '@/api/product'

// ─── Config ───
const ZONE_COUNT = 12        // max category zones to display
const HOT_ZONE_LIMIT = 8     // products in hotsale zone
const HIGH_PRI_COUNT = 2     // first N zones get 4x4 size
const HIGH_PRI_LIMIT = 8     // products in high-priority zones
const LOW_PRI_LIMIT = 4  // product limit for lower-priority zones

// ─── Data ───
const categoryTree = ref<ApiCategory[]>([])
const loading = ref(true)
const loadError = ref(false)
const zones = ref<Zone[]>([])

interface ZoneProduct {
  id: number
  title: string
  price: number
  old: number | null
  tag: string | null
  img: string
}

interface Zone {
  id: number | string
  title: string
  size: string
  products: ZoneProduct[]
}

// ─── Category display (from API, sorted by `sort`) ───
const categories = computed(() => {
  const sorted = [...categoryTree.value].sort((a, b) => a.sort - b.sort)
  return sorted.map(c => ({
    id: c.id,
    label: c.name,
    imageUrl: c.imageUrl,
    children: c.children?.map(ch => ch.name) || []
  }))
})

// ─── Flyout panel hover ───
const hoveredCat = ref(-1)
let hideTimer: ReturnType<typeof setTimeout> | null = null

function onCatEnter(i: number) {
  hoveredCat.value = i
  if (hideTimer) { clearTimeout(hideTimer); hideTimer = null }
}

function onCatClick(i: number) {
  // On touch: first tap shows flyout, second tap navigates
  if (hoveredCat.value === i) {
    goProductList()
  } else {
    hoveredCat.value = i
  }
}

function closeFlyout() {
  hoveredCat.value = -1
}

function onBrowseLeave() {
  hideTimer = setTimeout(() => { hoveredCat.value = -1 }, 300)
}

function cancelHide() {
  if (hideTimer) { clearTimeout(hideTimer); hideTimer = null }
}

// ─── Flyout recommended products ───
const hoveredCatProducts = computed(() => {
  const cat = categories.value[hoveredCat.value]
  if (!cat) return []
  const zone = zones.value.find(z => z.id === cat.id)
  return zone ? zone.products.slice(0, 3) : []
})

// ─── Build zones from API data ───
onMounted(async () => {
  try {
    const [catRes, productRes] = await Promise.all([
      getCategoryList(),
      getProductList({ size: 200 })
    ])
    categoryTree.value = catRes as ApiCategory[]

    const products = (productRes as any).records || (productRes as Product[]) || []
    const allZoneProducts: ZoneProduct[] = []

    // Build category id → first-level parent id (for routing products to zones)
    const parentMap = new Map<number, ApiCategory>()
    function walk(parent: ApiCategory) {
      if (parent.children?.length) {
        for (const ch of parent.children) {
          parentMap.set(ch.id, parent)
          walk(ch)
        }
      }
    }
    for (const cat of categoryTree.value) {
      parentMap.set(cat.id, cat)
      walk(cat)
    }

    // Sort first-level categories by sort asc, take top ZONE_COUNT
    const sortedCats = [...categoryTree.value]
      .sort((a, b) => a.sort - b.sort)
      .slice(0, ZONE_COUNT)

    // Build product lookup per first-level category
    const catProducts = new Map<number, ZoneProduct[]>()
    for (const p of (Array.isArray(products) ? products : [])) {
      const pi = p as Product
      const parent = parentMap.get(pi.categoryId)
      const catId = parent?.id
      if (catId === undefined) continue
      if (!catProducts.has(catId)) catProducts.set(catId, [])
      const zp: ZoneProduct = {
        id: pi.id, title: pi.name, price: pi.price,
        old: pi.originalPrice, tag: pi.tag, img: pi.mainImage
      }
      catProducts.get(catId)!.push(zp)
      allZoneProducts.push(zp)
    }

    // Build zones — only categories that have products
    let priCount = 0
    const result: Zone[] = []

    for (let i = 0; i < sortedCats.length; i++) {
      const cat = sortedCats[i]
      const zpList = catProducts.get(cat.id) || []
      if (zpList.length === 0) continue  // skip empty zones

      const isHighPri = priCount < HIGH_PRI_COUNT
      const limit = isHighPri ? HIGH_PRI_LIMIT : LOW_PRI_LIMIT
      const size = isHighPri ? '4x4' : '2x1'

      result.push({
        id: cat.id,
        title: cat.name,
        size,
        // stable sort: newest first
        products: [...zpList].sort((a, b) => b.id - a.id).slice(0, limit)
      })

      priCount++
    }

    zones.value = result

    // Derive hotsale products from the same fetch, no duplicate request
    hotsaleProducts.value = [...allZoneProducts]
      .sort((a, b) => b.id - a.id)
      .slice(0, HOT_ZONE_LIMIT)
  } catch {
      loadError.value = true
      showToast('加载失败，请检查网络后重试', 'error')
  } finally {
    loading.value = false
  }
})
onUnmounted(() => {
  if (hideTimer) clearTimeout(hideTimer)
  stopAutoplay()
  window.removeEventListener('scroll', scrollTick)
  if (feedObserver.value) {
    feedObserver.value.disconnect()
    feedObserver.value = null
  }
})

// ─── Hotsale zone (always first) ───
const hotsaleProducts = ref<ZoneProduct[]>([])

// ─── Sections: insert banners after zone 2 and 4 ───
const banners = [
  {
    id: 'banner-sale',
    title: '限时秒杀',
    subtitle: '每日精选 低至 5 折',
    desc: '全场满99包邮 · 正品保障 · 闪电发货',
    btn: '立即抢购',
    img: 'https://picsum.photos/id/20/1200/500',
    align: 'left' as const
  },
  {
    id: 'banner-brand',
    title: '品质生活 温暖相伴',
    subtitle: 'Warmart 精选全球好物',
    desc: '每一件商品都经过严格筛选，只为给你最好的',
    btn: '探索更多',
    img: 'https://picsum.photos/id/24/1200/500',
    align: 'right' as const
  }
]

const zoneRows = computed(() => {
  const rows: any[] = []
  let pending2x1: any[] = []

  function flushPair() {
    if (pending2x1.length === 1) {
      rows.push({ _type: 'row', cols: [{ ...pending2x1[0], _colSpan: 'half', _soloHalf: true }] })
    } else if (pending2x1.length >= 2) {
      rows.push({ _type: 'row', cols: pending2x1.slice(0, 2).map((z: any) => ({ ...z, _colSpan: 'half' })) })
      pending2x1 = pending2x1.slice(2)
      flushPair()
    }
  }

  // Hotsale first
  if (hotsaleProducts.value.length) {
    rows.push({ _type: 'row', cols: [{ id: 'hotsale', title: '热销爆款', size: '4x4', products: hotsaleProducts.value, _colSpan: 'full', _type: 'zone' }] })
  }

  let bannerIdx = 0
  for (let i = 0; i < zones.value.length; i++) {
    const z = zones.value[i]

    if (z.size === '4x4') {
      flushPair()
      rows.push({ _type: 'row', cols: [{ ...z, _colSpan: 'full', _type: 'zone' }] })
    } else {
      pending2x1.push(z)
    }

    if (i === 1 && bannerIdx < banners.length) {
      flushPair()
      rows.push({ _type: 'row', cols: [{ _type: 'banner', ...banners[bannerIdx++] }] })
    }
    if (i === 3 && bannerIdx < banners.length) {
      flushPair()
      rows.push({ _type: 'row', cols: [{ _type: 'banner', ...banners[bannerIdx++] }] })
    }
  }
  flushPair()
  return rows
})

// ─── Infinite scroll product feed ───
const feedProducts = ref<ZoneProduct[]>([])
const feedPage = ref(1)
const feedLoading = ref(false)
const feedFinished = ref(false)
const feedLoadError = ref(false)
const feedObserver = ref<IntersectionObserver | null>(null)
const feedSentinel = ref<HTMLElement | null>(null)

// ─── Scroll-to-top ───
const showScrollTop = ref(false)
let scrollListener: (() => void) | null = null
function onScrollTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function loadMore() {
  if (feedLoading.value || feedFinished.value) return
  feedLoading.value = true
  try {
    const res = await getProductList({ page: feedPage.value, size: 12 })
    const records = (res as any).records || (res as Product[]) || []
    if (!records.length) {
      feedFinished.value = true
      return
    }
    feedProducts.value.push(...records.map((pi: Product) => ({
      id: pi.id, title: pi.name, price: pi.price,
      old: pi.originalPrice, tag: pi.tag, img: pi.mainImage
    })))
    feedPage.value++
  } catch {
      feedLoadError.value = true
      showToast('加载更多失败，请重试', 'error')
  } finally {
    feedLoading.value = false
  }
}

function retryLoadMore() {
  if (feedLoading.value) return
  feedLoadError.value = false
  loadMore()
}

onMounted(() => {
  loadMore()
  feedObserver.value = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting) loadMore()
  }, { rootMargin: '200px' })
})

function onFeedSentinel(el: HTMLElement | null) {
  if (feedSentinel.value && feedObserver.value) {
    feedObserver.value.unobserve(feedSentinel.value)
  }
  feedSentinel.value = el
  if (el && feedObserver.value) {
    feedObserver.value.observe(el)
  }
}

// ─── Favorite ───
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

// ─── Navigation ───
const router = useRouter()
function goProductList() { router.push('/product/list') }
function goProductDetail(id: number) { window.open('/product/detail/' + id, '_blank') }

function retryLoad() {
  window.location.reload()
}

function onImgError(e: Event) {
  const img = e.target as HTMLImageElement
  if (img.dataset.fallback) return
  img.dataset.fallback = '1'
  img.style.display = 'none'
}

// ─── Hero carousel ───
const slides = [
  {
    label: '严选好物 · 品质生活',
    title: '精选全球好物<br>一站式轻松购齐',
    desc: '正品直供极速发货，严选轻奢穿搭、家居日用、美妆护肤、数码潮品全品类覆盖',
    btn: '即刻选购', btn2: '查看爆款',
    img: 'https://picsum.photos/id/26/700/600', align: 'left'
  },
  {
    label: '新品首发 · 限时特惠',
    title: '夏季新品全面上架<br>低至 5 折起',
    desc: '每周上新数百款爆品，限时秒杀专区全场包邮，潮流尖货抢先入手',
    btn: '抢购新品', btn2: '查看活动',
    img: 'https://picsum.photos/id/1/700/600', align: 'center'
  },
  {
    label: '数码狂欢 · 潮品直降',
    title: '数码家电超级品类日<br>大牌爆款直降千元',
    desc: 'Apple、华为、小米等大牌齐聚，以旧换新增值补贴，最高享 24 期免息',
    btn: '立即抢购', btn2: '查看活动',
    img: 'https://picsum.photos/id/0/700/600', align: 'right'
  },
  {
    label: '会员专享 · 积分加倍',
    title: '开通年度会员<br>立享双倍积分',
    desc: '会员专属价、生日礼包、免邮特权、专属客服四大权益，一年省回本',
    btn: '开通会员', btn2: '了解更多',
    img: 'https://picsum.photos/id/42/700/600', align: 'center'
  }
]
const currentSlide = ref(0)
let slideTimer: ReturnType<typeof setInterval> | null = null
function startAutoplay() { stopAutoplay(); slideTimer = setInterval(() => { currentSlide.value = (currentSlide.value + 1) % slides.length }, 5000) }
function stopAutoplay() { if (slideTimer) { clearInterval(slideTimer); slideTimer = null } }
function goSlide(i: number) { currentSlide.value = i; stopAutoplay(); startAutoplay() }
function prevSlide() { goSlide((currentSlide.value - 1 + slides.length) % slides.length) }
function nextSlide() { goSlide((currentSlide.value + 1) % slides.length) }
onMounted(startAutoplay)

const scrollTick = () => { showScrollTop.value = window.scrollY > 800 }
window.addEventListener('scroll', scrollTick, { passive: true })

const stats = [
  { value: '100W+', label: '忠实用户' },
  { value: '5000+', label: '入驻品牌' },
  { value: '99%', label: '好评率' }
]
</script>

<template>
  <div class="home">
    <!-- ============ HERO CAROUSEL ============ -->
    <section class="hero" @mouseenter="stopAutoplay" @mouseleave="startAutoplay">
      <div class="hero__track" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
        <div v-for="(slide, i) in slides" :key="i" class="hero__slide" :class="[`hero__slide--${slide.align}`]">
          <div class="hero__bg">
            <img :src="slide.img" alt="" class="hero__bg-img" @error="onImgError">
            <div class="hero__bg-overlay" />
          </div>
          <div class="hero__container">
            <div class="hero__content">
              <span class="hero__label">{{ slide.label }}</span>
              <h1 class="hero__title" v-html="slide.title" />
              <p class="hero__desc">{{ slide.desc }}</p>
              <div class="hero__btns">
                <button class="hero__btn-primary" @click="goProductList">{{ slide.btn }}</button>
                <button class="hero__btn-ghost" @click="goProductList">{{ slide.btn2 }}</button>
              </div>
              <div v-if="i === 0" class="hero__stats">
                <div v-for="s in stats" :key="s.label" class="hero__stat">
                  <p class="hero__stat-value">{{ s.value }}</p>
                  <p class="hero__stat-label">{{ s.label }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <button class="hero__arrow hero__arrow--prev" @click="prevSlide">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
      </button>
      <button class="hero__arrow hero__arrow--next" @click="nextSlide">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
      </button>
      <div class="hero__dots">
        <button v-for="(_, i) in slides" :key="i" class="hero__dot" :class="{ 'hero__dot--active': i === currentSlide }" @click="goSlide(i)" />
      </div>
    </section>

<!-- ============ LOADING SKELETON ============ -->
    <template v-if="loading">
      <section class="skeleton-section">
        <div class="section__inner">
          <div class="skeleton-loading-msg">正在为你精心挑选好物...</div>
          <div class="skeleton-header">
            <div class="shimmer skeleton-header__ornament" />
            <div class="shimmer skeleton-header__slogan" />
            <div class="shimmer skeleton-header__ornament" />
          </div>
          <div class="skeleton-cat-grid">
            <div v-for="i in 12" :key="i" class="shimmer skeleton-cat-tile" />
          </div>
          <div class="skeleton-zone">
            <div class="shimmer skeleton-zone-title" />
            <div class="skeleton-zone-prods">
              <div v-for="i in 4" :key="i" class="shimmer skeleton-zone-prod" />
            </div>
          </div>
          <div class="skeleton-zone">
            <div class="shimmer skeleton-zone-title" />
            <div class="skeleton-zone-prods skeleton-zone-prods--half">
              <div v-for="i in 4" :key="i" class="shimmer skeleton-zone-prod" />
            </div>
          </div>
        </div>
      </section>
    </template>

    <!-- ============ ERROR STATE ============ -->
    <template v-else-if="loadError">
      <section class="error-section">
        <div class="section__inner">
          <div class="error-state">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--wz-orange)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            <h3 class="error-state__title">加载失败</h3>
            <p class="error-state__desc">网络开小差了，请检查后重试</p>
            <button class="error-state__btn" @click="retryLoad">重新加载</button>
          </div>
        </div>
      </section>
    </template>

    <!-- ============ CATEGORY BROWSE ============ -->
    <section v-else class="cat-browse">
<!-- Decorative section divider -->
      <div class="section__divider">
        <div class="section__inner">
          <div class="section__divider-inner">
            <span class="section__divider-line"></span>
            <span class="section__divider-ember">◆</span>
            <span class="section__divider-text">每一件好物，都值得被认真挑选</span>
            <span class="section__divider-ember">◆</span>
            <span class="section__divider-line"></span>
          </div>
        </div>
      </div>

      <div class="cat-browse__body" @mouseleave="onBrowseLeave">
        <div class="section__inner">
          <div class="cat-browse__grid">
            <button
              v-for="(cat, i) in categories"
              :key="cat.id"
              class="cat-browse__tile"
              :class="{ 'cat-browse__tile--active': hoveredCat === i }"
              @mouseenter="onCatEnter(i)"
              @click="onCatClick(i)"
            >
              <span class="cat-browse__tile-icon">
                <img v-if="cat.imageUrl" :src="cat.imageUrl" :alt="cat.label" width="22" height="22">
                <span v-else class="cat-browse__tile-fallback">{{ cat.label.charAt(0) }}</span>
              </span>
              <span class="cat-browse__tile-label">{{ cat.label }}</span>
              <span class="cat-browse__tile-hint">点击展开</span>
            </button>
          </div>
        </div>

        <!-- Flyout panel -->
        <div
          class="cat-trigger__panel"
          :class="{ 'cat-trigger__panel--visible': hoveredCat >= 0 }"
          @mouseenter="cancelHide"
          @click.self="closeFlyout"
        >
          <div class="section__inner">
            <div class="cat-trigger__panel-inner">
              <div class="cat-trigger__left">
                <button
                  v-for="(cat, i) in categories"
                  :key="cat.id"
                  class="cat-trigger__cat-btn"
                  :class="{ 'cat-trigger__cat-btn--active': hoveredCat === i }"
                  @mouseenter="onCatEnter(i)"
                >
                  <span class="cat-trigger__cat-icon">
                    <img v-if="cat.imageUrl" :src="cat.imageUrl" :alt="cat.label" width="16" height="16">
                    <span v-else class="cat-trigger__cat-fallback">{{ cat.label.charAt(0) }}</span>
                  </span>
                  <span class="cat-trigger__cat-label">{{ cat.label }}</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="cat-trigger__cat-arrow"><path d="M9 18l6-6-6-6"/></svg>
                </button>
              </div>
              <div class="cat-trigger__right">
                <div class="cat-trigger__subcats">
                  <span v-for="child in categories[hoveredCat]?.children" :key="child" class="cat-trigger__subcat-tag" @click="goProductList">{{ child }}</span>
                </div>
                <div v-if="hoveredCatProducts.length" class="cat-trigger__products">
                  <div v-for="p in hoveredCatProducts" :key="p.id" class="cat-trigger__product" @click="goProductDetail(p.id)">
                    <div class="cat-trigger__product-img">
                      <img :src="p.img" :alt="p.title">
                      <div class="cat-trigger__product-overlay">
                        <p class="cat-trigger__product-name">{{ p.title }}</p>
                        <span class="cat-trigger__product-price">¥{{ p.price }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ ZONE GRID (hotsale first, then category zones) ============ -->
    <section v-if="!loading && !loadError" class="section">
      <div class="section__inner">
        <div class="zone-rows">
          <div
            v-for="(row, ri) in zoneRows"
            :key="ri"
            class="zone-row"
            :class="{ 'zone-row--paired': row.cols.length > 1, 'zone-row--single': row._soloHalf }"
          >
            <template v-for="item in row.cols" :key="item._type === 'banner' ? item.id : item.id">
              <div v-if="item._type === 'banner'" class="zone-banner" :class="`zone-banner--${item.align}`">
                <div class="zone-banner__bg">
                  <img :src="item.img" alt="" class="zone-banner__bg-img" @error="onImgError">
                  <div class="zone-banner__overlay" />
                </div>
                <div class="zone-banner__content">
                  <span class="zone-banner__label">{{ item.subtitle }}</span>
                  <h3 class="zone-banner__title">{{ item.title }}</h3>
                  <p class="zone-banner__desc">{{ item.desc }}</p>
                  <button class="zone-banner__btn" @click="goProductList">{{ item.btn }}</button>
                </div>
              </div>
              <div v-else :class="['zone', `zone--${item._colSpan}`]" :data-zone-id="item.id">
                <div class="zone__header" @click="goProductList">
                  <h3 class="zone__title">{{ item.title }}</h3>
                </div>
                <div class="zone__products">
                  <div v-for="p in item.products" :key="p.id" class="zone__product" @click="goProductDetail(p.id)">


                    <div class="zone__product-img">
                      <img :src="p.img" :alt="p.title" @error="onImgError">
                      <span v-if="p.tag" class="zone__product-badge">{{ p.tag }}</span>
                      <button
                        class="zone__product-fav"
                        :class="{ 'zone__product-fav--active': favoritedIds.has(p.id) || animatingIds.has(p.id) }"
                        @click.stop="handleFavorite(p.id)"
                        title="收藏"
                      >
                        <svg class="zone__product-fav-heart" viewBox="0 0 24 24" width="16" height="16">
                          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" fill="none" stroke="currentColor" stroke-width="2"/>
                        </svg>
                        <span class="zone__product-fav-ring"></span>
                      </button>
                      <div class="zone__product-info">
                        <p class="zone__product-name">{{ p.title }}</p>
                        <div class="zone__product-prices">
                          <span class="zone__product-price">¥{{ p.price }}</span>
                          <span v-if="p.old" class="zone__product-old">¥{{ p.old }}</span>
                        </div>
                      </div>
                    </div>


                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </section>

    <!-- ============ INFINITE SCROLL PRODUCT FEED ============ -->
    <section v-if="!loading && !loadError" class="section section--feed">
      <div class="section__inner">
        <div class="feed__header">
          <h3 class="feed__title">为你推荐</h3>
          <span class="feed__subtitle">发现更多好物</span>
        </div>
        <div class="feed__grid">
          <div v-for="p in feedProducts" :key="p.id" class="feed__card" @click="goProductDetail(p.id)">
            <div class="feed__card-img">
              <img :src="p.img" :alt="p.title" loading="lazy" @error="onImgError">
              <span v-if="p.tag" class="feed__card-badge">{{ p.tag }}</span>
              <button
                class="feed__card-fav"
                :class="{ 'feed__card-fav--active': favoritedIds.has(p.id) || animatingIds.has(p.id) }"
                @click.stop="handleFavorite(p.id)"
                title="收藏"
              >
                <svg class="feed__card-fav-heart" viewBox="0 0 24 24" width="14" height="14">
                  <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" fill="none" stroke="currentColor" stroke-width="2"/>
                </svg>
                <span class="feed__card-fav-ring"></span>
              </button>
            </div>
            <div class="feed__card-body">
              <p class="feed__card-name">{{ p.title }}</p>
              <div class="feed__card-prices">
                <span class="feed__card-price">¥{{ p.price }}</span>
                <span v-if="p.old" class="feed__card-old">¥{{ p.old }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Sentinel for IntersectionObserver -->
<div :ref="onFeedSentinel" class="feed__sentinel">
          <div v-if="feedLoadError" class="feed__error" @click="retryLoadMore">
            <span class="feed__error-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
            </span>
            <span>加载失败，点击重试</span>
          </div>
          <div v-else-if="feedLoading" class="feed__loading">
            <span class="feed__spinner"></span>
            <span>加载中...</span>
          </div>
          <div v-else-if="feedFinished" class="feed__finished">— 已经到底了 —</div>
        </div>
      </div>
    </section>

<!-- ============ SCROLL TO TOP ============ -->
    <Transition name="scroll-top">
      <button v-if="showScrollTop" class="scroll-top" @click="onScrollTop" title="回到顶部">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 15l-6-6-6 6"/></svg>
      </button>
    </Transition>

  </div>
</template>

<style scoped>
.home { padding-top: 60px; background: var(--wz-bg); }

/* ---- Hero Carousel ---- */
.hero { position: relative; overflow: hidden; background: var(--wz-bg); height: 620px; }
@media (max-width: 768px) { .hero { height: 480px; } }
.hero__track { display: flex; height: 100%; transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1); will-change: transform; }
.hero__slide { position: relative; min-width: 100%; display: flex; align-items: center; overflow: hidden; }
.hero__bg { position: absolute; inset: 0; }
.hero__bg-img { width: 100%; height: 100%; object-fit: cover; }
.hero__bg-overlay { position: absolute; inset: 0; }
.hero__slide:nth-child(1) .hero__bg { background: linear-gradient(135deg, #1a0e06 0%, #2d1a0a 40%, #1a0e06 100%); }
.hero__slide:nth-child(2) .hero__bg { background: linear-gradient(135deg, #1a0808 0%, #2d1010 40%, #1a0808 100%); }
.hero__slide:nth-child(3) .hero__bg { background: linear-gradient(135deg, #080e1a 0%, #0a1a2d 40%, #080e1a 100%); }
.hero__slide:nth-child(4) .hero__bg { background: linear-gradient(135deg, #12081a 0%, #1a0a2d 40%, #12081a 100%); }
.hero__bg-overlay { position: absolute; inset: 0; background: linear-gradient(135deg, rgba(15,15,17,0.70) 0%, rgba(15,15,17,0.30) 50%, rgba(15,15,17,0.10) 100%); }
.hero__container { position: relative; z-index: 2; width: 100%; max-width: 1400px; margin: 0 auto; padding: 0 48px; }
.hero__container::before { content: ''; position: absolute; left: -120px; top: 50%; transform: translateY(-50%); width: 520px; height: 520px; border-radius: 50%; background: radial-gradient(circle, rgba(255,107,53,0.10) 0%, rgba(255,107,53,0.04) 40%, transparent 70%); pointer-events: none; }
.hero__slide--center .hero__container::before { left: 50%; transform: translate(-50%, -50%); }
.hero__slide--right .hero__container::before { left: auto; right: -120px; }
.hero__content { max-width: 560px; animation: hero-content-in 0.6s var(--wz-ease-out) both; }
@media (prefers-reduced-motion: reduce) { .hero__content { animation: none; } }

@keyframes hero-content-in {
  from { opacity: 0; transform: translateY(18px); }
  to { opacity: 1; transform: translateY(0); }
}
.hero__slide--center .hero__container { text-align: center; }
.hero__slide--center .hero__content { margin: 0 auto; }
.hero__slide--center .hero__desc { margin-left: auto; margin-right: auto; }
.hero__slide--center .hero__btns { justify-content: center; }
.hero__slide--center .hero__stats { justify-content: center; }
.hero__slide--right .hero__container { display: flex; justify-content: flex-end; }
.hero__label { display: inline-block; font-size: 12px; font-weight: 600; color: var(--wz-orange); letter-spacing: 0.18em; text-transform: uppercase; background: rgba(255,107,53,0.15); padding: 5px 18px; border-radius: 999px; margin-bottom: 18px; box-shadow: 0 0 20px rgba(255,107,53,0.08); }
.hero__title { font-family: var(--wz-font-display); font-size: clamp(32px, 4.5vw, 54px); font-weight: 700; line-height: 1.15; color: var(--wz-text); margin: 0 0 16px; text-shadow: 0 2px 24px rgba(0,0,0,0.3); }
@media (max-width: 768px) { .hero__title { font-size: 32px; } }
.hero__desc { font-size: 17px; color: var(--wz-text-soft); line-height: 1.75; max-width: 480px; margin-bottom: var(--wz-space-lg); text-shadow: 0 1px 12px rgba(0,0,0,0.2); }
.hero__btns { display: flex; gap: 12px; flex-wrap: wrap; }
.hero__btn-primary { padding: 14px 34px; background: var(--wz-orange); border: none; border-radius: 10px; color: #fff; font-family: var(--wz-font-body); font-size: 15px; font-weight: 600; cursor: pointer; transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out), transform var(--wz-duration-normal) var(--wz-ease-out); }
.hero__btn-primary:hover { box-shadow: 0 8px 28px rgba(255,107,53,0.35); transform: translateY(-2px); }
.hero__btn-ghost { padding: 14px 34px; background: transparent; border: 1px solid rgba(255,255,255,0.5); border-radius: 10px; color: #fff; font-family: var(--wz-font-body); font-size: 15px; font-weight: 500; cursor: pointer; transition: border-color var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out); }
.hero__btn-ghost:hover { border-color: var(--wz-orange); color: var(--wz-orange); }
.hero__stats { display: flex; gap: var(--wz-space-xl); margin-top: var(--wz-space-2xl); }
.hero__stat-value { font-size: 28px; font-weight: 700; color: var(--wz-orange); line-height: 1.2; letter-spacing: 0.02em; }
.hero__stat-label { font-size: 13px; color: var(--wz-text-muted); margin-top: 4px; }
.hero__arrow { position: absolute; top: 50%; z-index: 5; width: 44px; height: 44px; border-radius: 50%; background: rgba(255,255,255,0.06); backdrop-filter: blur(8px); border: 1px solid rgba(255,255,255,0.08); color: #fff; display: flex; align-items: center; justify-content: center; cursor: pointer; opacity: 0; transition: opacity var(--wz-duration-normal) var(--wz-ease-out), background var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out); transform: translateY(-50%) scale(0.9); }
.hero:hover .hero__arrow { opacity: 1; transform: translateY(-50%) scale(1); }
.hero__arrow:hover { background: rgba(255,255,255,0.12); }
.hero__arrow--prev { left: 24px; }
.hero__arrow--next { right: 24px; }
.hero__dots { position: absolute; bottom: 24px; left: 50%; transform: translateX(-50%); z-index: 5; display: flex; gap: 10px; }
.hero__dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255,255,255,0.25); border: none; cursor: pointer; transition: background var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out), width var(--wz-duration-fast) var(--wz-ease-out); padding: 0; }
.hero__dot--active { width: 28px; border-radius: 4px; background: var(--wz-orange); }
.hero__dot:hover:not(.hero__dot--active) { background: rgba(255,255,255,0.5); }

/* ---- Category Browse ---- */
.cat-browse { background: var(--wz-bg-elevated); }
/* ---- Section Divider (replaces orange header) ---- */
.section__divider {
  padding: var(--wz-space-xl) var(--wz-space-md);
}
.section__divider-inner {
  display: flex;
  align-items: center;
  gap: var(--wz-space-md);
  justify-content: center;
}
.section__divider-line {
  display: block;
  width: 80px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--wz-border), transparent);
  border-radius: 1px;
}
.section__divider-ember {
  font-size: 10px;
  color: var(--wz-orange);
  opacity: 0.6;
  line-height: 1;
}
.section__divider-text {
  font-family: var(--wz-font-display);
  font-size: 14px;
  font-weight: 600;
  color: var(--wz-text-soft);
  letter-spacing: 0.25em;
  white-space: nowrap;
}
.cat-browse__body { position: relative; }
.cat-browse__grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: 8px; padding: 20px 0; }
.cat-browse__tile { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 16px 8px 14px; background: var(--wz-bg-card); border: 1px solid var(--wz-border); border-radius: 10px; cursor: pointer; font-family: var(--wz-font-body); color: var(--wz-text-soft); transition: background var(--wz-duration-fast) var(--wz-ease-out), border-color var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out), box-shadow var(--wz-duration-fast) var(--wz-ease-out); }
.cat-browse__tile:hover { background: var(--wz-bg-hover); border-color: var(--wz-orange); color: var(--wz-text); transform: translateY(-3px); box-shadow: 0 8px 28px rgba(255,107,53,0.10); }
.cat-browse__tile--active { background: var(--wz-orange-muted); border-color: var(--wz-orange); color: var(--wz-orange); box-shadow: 0 6px 24px rgba(255,107,53,0.20); }
.cat-browse__tile-icon { display: flex; align-items: center; justify-content: center; width: 22px; height: 22px; opacity: 0.6; transition: opacity var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out); }
.cat-browse__tile:hover .cat-browse__tile-icon { opacity: 1; transform: scale(1.1); }
.cat-browse__tile--active .cat-browse__tile-icon { opacity: 1; transform: scale(1.1); }
.cat-browse__tile-fallback { font-size: 13px; font-weight: 600; color: var(--wz-text-soft); line-height: 22px; }
.cat-browse__tile-label { font-size: 12px; font-weight: 500; text-align: center; line-height: 1.3; }
.cat-browse__tile-hint { display: none; font-size: 10px; color: var(--wz-text-muted); letter-spacing: 0.05em; margin-top: auto; }

@media (max-width: 1024px) { .cat-browse__grid { grid-template-columns: repeat(auto-fill, minmax(90px, 1fr)); } }
@media (max-width: 640px) {
  .cat-browse__grid { grid-template-columns: repeat(auto-fill, minmax(70px, 1fr)); gap: 6px; padding: 12px 0; }
  .cat-browse__tile { padding: 12px 6px; }
}

/* Flyout panel */
.cat-trigger__panel { position: absolute; top: 100%; left: 0; right: 0; z-index: 50; padding: 24px 0 28px; background: var(--wz-bg-card); border: 1px solid var(--wz-border); border-top: none; border-radius: 0 0 14px 14px; box-shadow: 0 16px 48px rgba(0,0,0,0.4); opacity: 0; transform: translateY(-6px); pointer-events: none; transition: opacity 0.18s var(--wz-ease-out), transform 0.18s var(--wz-ease-out); }
.cat-trigger__panel--visible { opacity: 1; transform: translateY(0); pointer-events: auto; }
.cat-trigger__panel-inner { display: flex; gap: 32px; min-height: 440px; }
.cat-trigger__left { width: 170px; flex-shrink: 0; display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }
.cat-trigger__cat-btn { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: none; border: none; border-radius: 8px; font-family: var(--wz-font-body); font-size: 14px; font-weight: 500; color: var(--wz-text-soft); cursor: pointer; text-align: left; transition: background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out); }
.cat-trigger__cat-btn:hover { background: var(--wz-bg-hover); color: var(--wz-text); }
.cat-trigger__cat-btn--active { background: var(--wz-orange-muted); color: var(--wz-orange); font-weight: 600; }
.cat-trigger__cat-icon { display: flex; align-items: center; justify-content: center; width: 16px; height: 16px; opacity: 0.6; flex-shrink: 0; }
.cat-trigger__cat-btn--active .cat-trigger__cat-icon { opacity: 1; }
.cat-trigger__cat-fallback { font-size: 11px; font-weight: 600; color: var(--wz-text-soft); line-height: 16px; }
.cat-trigger__cat-label { flex: 1; }
.cat-trigger__cat-arrow { opacity: 0; transition: opacity var(--wz-duration-fast) var(--wz-ease-out); flex-shrink: 0; }
.cat-trigger__cat-btn--active .cat-trigger__cat-arrow { opacity: 0.6; }
.cat-trigger__right { flex: 1; min-width: 0; }
.cat-trigger__subcats { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid var(--wz-border); }
.cat-trigger__subcat-tag { padding: 6px 16px; font-size: 13px; font-weight: 500; color: var(--wz-text); background: var(--wz-bg); border-radius: 999px; cursor: pointer; transition: background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out); }
.cat-trigger__subcat-tag:hover { background: var(--wz-orange); color: #fff; transform: translateY(-1px); }
.cat-trigger__products { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.cat-trigger__product { cursor: pointer; }
.cat-trigger__product-img { position: relative; aspect-ratio: 3 / 2; border-radius: 6px; overflow: hidden; background: var(--wz-bg); }
.cat-trigger__product-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s var(--wz-ease-out); }
.cat-trigger__product:hover .cat-trigger__product-img img { transform: scale(1.06); }
.cat-trigger__product-overlay { position: absolute; left: 0; right: 0; bottom: 0; padding: 16px 8px 7px; background: linear-gradient(transparent 20%, rgba(0,0,0,0.8) 60%, rgba(0,0,0,0.92)); border-radius: 0 0 6px 6px; }
.cat-trigger__product-name { font-size: 12px; color: #fff; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 3px; text-shadow: 0 1px 4px rgba(0,0,0,0.5); }
.cat-trigger__product-price { font-size: 14px; font-weight: 700; color: var(--wz-orange); }

@media (max-width: 768px) {
  .cat-trigger__panel { padding: 16px 0 20px; }
  .cat-trigger__panel-inner { flex-direction: column; gap: 16px; min-height: 0; }
  .cat-trigger__left { width: 100%; flex-direction: row; flex-wrap: wrap; gap: 4px; }
  .cat-trigger__cat-btn { padding: 6px 12px; font-size: 13px; flex: 0 0 auto; }
  .cat-trigger__cat-arrow { display: none; }
  .cat-trigger__products { grid-template-columns: repeat(3, 1fr); gap: 10px; }
}

/* ---- Section ---- */
.section { padding: var(--wz-space-3xl) var(--wz-space-md) var(--wz-space-3xl); }
.section__inner { max-width: 1400px; margin: 0 auto; }

/* Category browse bottom breathing room */
.cat-browse { background: var(--wz-bg-elevated); padding-bottom: var(--wz-space-2xl); }

/* ---- Zone Rows ---- */
.zone-rows { display: flex; flex-direction: column; gap: var(--wz-space-3xl); }
.zone-row { display: flex; width: 100%; gap: var(--wz-space-xl); }
.zone-row--single { justify-content: center; }
.zone--full { width: 100%; }
.zone--half { width: calc(50% - var(--wz-space-xl) / 2); flex: 0 1 auto; min-width: 0; }
.zone-row--paired .zone--half { flex: 1 1 calc(50% - var(--wz-space-xl) / 2); }
.zone-row--single .zone--half { width: calc(50% - var(--wz-space-xl) / 2); max-width: 50%; }
.zone__title { transition: transform 0.3s var(--wz-ease-out); transform-origin: left; font-family: var(--wz-font-display); font-size: 22px; font-weight: 700; color: var(--wz-text); line-height: 1.3; position: relative; display: inline-block; padding-left: 14px; }
.zone__title::before { content: ''; position: absolute; left: 0; top: 0.08em; bottom: 0.08em; width: 3px; border-radius: 2px; background: var(--wz-orange); opacity: 0; transition: opacity 0.3s var(--wz-ease-out), transform 0.3s var(--wz-ease-out); transform: scaleY(0.6); }
.zone__title::after { content: ''; position: absolute; left: 0; right: 0; bottom: -4px; height: 3px; border-radius: 2px; background: var(--wz-orange); transition: width 0.3s var(--wz-ease-out), box-shadow 0.3s var(--wz-ease-out), right 0.3s var(--wz-ease-out); }
.zone__header:hover .zone__title { transform: translateX(6px) scale(1.04); }
.zone__header:hover .zone__title::before { opacity: 0.7; transform: scaleY(1); }
.zone__header:hover .zone__title::after { box-shadow: 0 0 20px rgba(255,107,53,0.5); width: 110%; }
.zone__header { padding: 0 0 16px; cursor: pointer; }
.zone__products { padding: 0; }
.zone--full .zone__products { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--wz-space-md); }
.zone--half .zone__products { display: grid; grid-template-columns: 1fr 1fr; gap: var(--wz-space-md); }
.zone__product { cursor: pointer; animation: card-in 0.5s var(--wz-ease-out) both; }
.zone__product:nth-child(1) { animation-delay: 0s; }
.zone__product:nth-child(2) { animation-delay: 0.06s; }
.zone__product:nth-child(3) { animation-delay: 0.12s; }
.zone__product:nth-child(4) { animation-delay: 0.18s; }
@keyframes card-in {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
@media (prefers-reduced-motion: reduce) { .zone__product { animation: none; } }
.zone__product-img { position: relative; overflow: hidden; aspect-ratio: 1; border-radius: 8px; background: var(--wz-bg-card); }
.zone__product-img:has(img[data-fallback]) { background: linear-gradient(135deg, var(--wz-bg-card) 0%, var(--wz-bg-elevated) 100%); }
.zone__product-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s var(--wz-ease-out); }
.zone__product:hover .zone__product-img img { transform: scale(1.06); }
.zone__product-info { position: absolute; left: 0; right: 0; bottom: 0; padding: 32px 10px 10px; background: linear-gradient(transparent 20%, rgba(0,0,0,0.8) 60%, rgba(0,0,0,0.92)); border-radius: 0 0 8px 8px; }
.zone__product-name { font-size: 12px; color: #fff; line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4px; text-shadow: 0 1px 4px rgba(0,0,0,0.5); }
.zone__product-badge { position: absolute; top: 8px; left: 8px; z-index: 2; font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 999px; color: #fff; background: var(--wz-orange); line-height: 1.3; letter-spacing: 0.04em; }
.zone__product:nth-child(2) .zone__product-badge { background: var(--wz-success); }
.zone__product:nth-child(3) .zone__product-badge { background: #af52de; }
.zone__product:nth-child(4) .zone__product-badge { background: #007aff; }
.zone__product-fav { position: absolute; top: 8px; right: 8px; z-index: 3; width: 30px; height: 30px; border-radius: 50%; background: rgba(0,0,0,0.45); border: none; color: rgba(255,255,255,0.7); cursor: pointer; display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity var(--wz-duration-normal) var(--wz-ease-out), background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out); backdrop-filter: blur(4px); }
.zone__product:hover .zone__product-fav { opacity: 1; }
.zone__product-fav:hover { background: rgba(255,107,53,0.85); color: #fff; transform: scale(1.1); }
.zone__product-fav--active { background: rgba(255,107,53,0.85); color: #fff; }
.zone__product-fav-heart { position: relative; z-index: 1; transition: transform 0.3s var(--wz-ease-out); }
.zone__product-fav--active .zone__product-fav-heart { fill: currentColor; animation: fav-beat 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards; }
.zone__product-fav-ring { position: absolute; inset: -4px; border-radius: 50%; border: 2px solid var(--wz-orange); opacity: 0; pointer-events: none; }
.zone__product-fav--active .zone__product-fav-ring { animation: fav-ripple 0.6s ease-out forwards; }
@keyframes fav-beat { 0% { transform: scale(1); } 30% { transform: scale(1.5); } 60% { transform: scale(0.9); } 100% { transform: scale(1.2); } }
@keyframes fav-ripple { 0% { transform: scale(0.8); opacity: 0.6; } 100% { transform: scale(2); opacity: 0; } }
.zone__product-prices { display: flex; align-items: center; gap: 6px; }
.zone__product-price { font-size: 14px; font-weight: 700; color: var(--wz-orange); line-height: 1; }
.zone__product-old { font-size: 11px; color: rgba(255,255,255,0.5); text-decoration: line-through; line-height: 1; }
.zone[data-zone-id="hotsale"] .zone__product-price { font-size: 15px; }
.zone[data-zone-id="hotsale"] .zone__product-badge { background: var(--wz-danger); animation: badge-pulse 2s ease-in-out infinite; }
.zone[data-zone-id="hotsale"] .zone__title { color: var(--wz-orange); }
.zone[data-zone-id="hotsale"] .zone__title::after { background: var(--wz-orange); box-shadow: 0 0 12px rgba(255,107,53,0.4); }
.zone[data-zone-id="hotsale"] .zone__title::before { background: var(--wz-orange); }
@keyframes badge-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }

/* ---- Skeleton Loading ---- */
.skeleton-section { padding: var(--wz-space-xl) var(--wz-space-md) var(--wz-space-2xl); }
.skeleton-loading-msg { text-align: center; font-size: 14px; color: var(--wz-text-muted); letter-spacing: 0.08em; margin-bottom: var(--wz-space-lg); animation: skeleton-msg-pulse 2s ease-in-out infinite; }
@keyframes skeleton-msg-pulse { 0%, 100% { opacity: 0.5; } 50% { opacity: 1; } }
.skeleton-header { display: flex; align-items: center; justify-content: center; gap: var(--wz-space-lg); margin-bottom: var(--wz-space-lg); }
.skeleton-header__ornament { width: 60px; height: 1px; border-radius: 2px; }
.skeleton-header__slogan { width: 240px; height: 24px; border-radius: 4px; }
.skeleton-cat-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); gap: var(--wz-space-sm); margin-bottom: var(--wz-space-2xl); }
.skeleton-cat-tile { height: 72px; border-radius: 10px; }
.skeleton-zone { margin-bottom: var(--wz-space-2xl); }
.skeleton-zone-title { width: 120px; height: 24px; border-radius: 4px; margin-bottom: 16px; }
.skeleton-zone-prods { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--wz-space-md); }
.skeleton-zone-prods--half { grid-template-columns: repeat(2, 1fr); }
.skeleton-zone-prod { aspect-ratio: 1; border-radius: 8px; }

@media (max-width: 768px) {
  .skeleton-zone-prods { grid-template-columns: repeat(2, 1fr); gap: 10px; }
}

/* ---- Error State ---- */
.error-section { padding: 100px 16px; }
.error-state { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; gap: 16px; }
.error-state__title { font-family: var(--wz-font-display); font-size: 22px; font-weight: 700; color: var(--wz-text); }
.error-state__desc { font-size: 14px; color: var(--wz-text-muted); }
.error-state__btn { padding: 12px 32px; background: var(--wz-orange); border: none; border-radius: 10px; color: #fff; font-family: var(--wz-font-body); font-size: 15px; font-weight: 600; cursor: pointer; transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out), transform var(--wz-duration-normal) var(--wz-ease-out); }
.error-state__btn:hover { box-shadow: 0 8px 28px rgba(255,107,53,0.35); transform: translateY(-2px); }

/* ---- Reduced Motion ---- */
@media (prefers-reduced-motion: reduce) {
  .hero__track { transition: none; }
  .zone__product-img img,
  .feed__card-img img,
  .cat-trigger__product-img img,
  .zone-banner__bg-img,
  .cat-browse__tile,
  .feed__card,
  .hero__btn-primary,
  .hero__btn-ghost,
  .zone__product-fav,
  .feed__card-fav { transition: none; }
  .hero:hover .hero__arrow { opacity: 0; }
  .zone__product:hover .zone__product-img img { transform: none; }
  .feed__card:hover .feed__card-img img { transform: none; }
  .shimmer { animation: none; background: var(--wz-bg-card); }
  .zone__product-fav { opacity: 0.6; }
  .feed__card-fav { opacity: 0.6; }
  .hero__dot--active { width: 8px; border-radius: 50%; }
}

/* ---- Touch devices: no hover, show controls always ---- */
@media (hover: none) and (pointer: coarse) {
  .hero__arrow { opacity: 1; transform: translateY(-50%) scale(1); }
  .zone__product-fav,
  .feed__card-fav { opacity: 0.6; }
  .zone__product-fav:hover,
  .feed__card-fav:hover,
  .cat-browse__tile:hover { transform: none; }
  .cat-trigger__panel { display: none; }
  .cat-trigger__panel--visible { display: block; }
  .cat-browse__tile-hint { display: block; }
  .hero__btn-primary:hover,
  .hero__btn-ghost:hover,
  .error-state__btn:hover { transform: none; box-shadow: none; }
  .zone-banner__btn:hover { transform: none; box-shadow: none; }
  .cat-trigger__subcat-tag:hover { transform: none; }
  .hero__btn-primary:active,
  .error-state__btn:active { opacity: 0.8; }
}

/* ---- Touch-friendly sizing ---- */
@media (max-width: 480px) {
  .hero { height: 380px; }
  .hero__container { padding: 0 20px; }
  .hero__title { font-size: 24px; }
  .hero__desc { font-size: 14px; max-width: none; }
  .hero__btn-primary,
  .hero__btn-ghost { padding: 12px 24px; font-size: 14px; min-height: 44px; }
  .cat-browse__tile { min-height: 44px; }
  .cat-browse__tile-label { font-size: 11px; }
  .zone-banner { height: 150px; }
  .zone-banner__content { padding: 0 16px; max-width: none; }
  .zone-banner__title { font-size: 18px; }
  .zone-banner__btn { min-height: 44px; }
  .cat-trigger__subcat-tag { padding: 8px 16px; min-height: 36px; }
  .feed__card-fav { width: 36px; height: 36px; }
  .zone__product-fav { width: 36px; height: 36px; }
}

@keyframes badge-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }

/* ---- Zone Banners ---- */
.zone-banner { position: relative; width: 100%; flex-shrink: 0; border-radius: 10px; overflow: hidden; height: 220px; cursor: pointer; }
.zone-banner__bg { position: absolute; inset: 0; }
.zone-banner__bg-img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s var(--wz-ease-out); }
.zone-banner:hover .zone-banner__bg-img { transform: scale(1.05); }
.zone-banner__overlay { position: absolute; inset: 0; background: linear-gradient(135deg, rgba(15,15,17,0.90) 0%, rgba(15,15,17,0.50) 40%, rgba(15,15,17,0.15) 100%); }
.zone-banner--right .zone-banner__overlay { background: linear-gradient(225deg, rgba(15,15,17,0.90) 0%, rgba(15,15,17,0.50) 40%, rgba(15,15,17,0.15) 100%); }
.zone-banner::after { content: ''; position: absolute; inset: 0; background: radial-gradient(ellipse at 30% 50%, rgba(255,107,53,0.06) 0%, transparent 60%); pointer-events: none; z-index: 1; }
.zone-banner--right::after { background: radial-gradient(ellipse at 70% 50%, rgba(255,107,53,0.06) 0%, transparent 60%); }
.zone-banner:first-of-type .zone-banner__bg { background: linear-gradient(135deg, #1a0e06 0%, #2d1a0a 100%); }
.zone-banner:last-of-type .zone-banner__bg { background: linear-gradient(135deg, #0e0e1a 0%, #1a0a2d 100%); }
.zone-banner__content { position: relative; z-index: 2; height: 100%; display: flex; flex-direction: column; justify-content: center; padding: 0 48px; max-width: 420px; }
.zone-banner--right .zone-banner__content { margin-left: auto; text-align: right; align-items: flex-end; }
.zone-banner__label { display: inline-block; font-size: 11px; font-weight: 500; color: var(--wz-orange); letter-spacing: 0.15em; background: rgba(255,107,53,0.15); padding: 3px 14px; border-radius: 999px; margin-bottom: 12px; align-self: flex-start; }
.zone-banner--right .zone-banner__label { align-self: flex-end; }
.zone-banner__title { font-family: var(--wz-font-display); font-size: 26px; font-weight: 700; color: var(--wz-text); margin-bottom: 8px; line-height: 1.2; }
.zone-banner__desc { font-size: 14px; color: var(--wz-text-soft); line-height: 1.5; margin-bottom: 18px; }
.zone-banner__btn { align-self: flex-start; padding: 10px 28px; background: var(--wz-orange); border: none; border-radius: 999px; color: #fff; font-family: var(--wz-font-body); font-size: 14px; font-weight: 600; cursor: pointer; transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out), transform var(--wz-duration-normal) var(--wz-ease-out); }
.zone-banner--right .zone-banner__btn { align-self: flex-end; }
.zone-banner__btn:hover { box-shadow: 0 6px 20px rgba(255,107,53,0.35); transform: translateY(-1px); }

/* ---- Infinite Scroll Product Feed ---- */
.section--feed { padding-top: var(--wz-space-lg); }

.feed__header { text-align: center; margin-bottom: var(--wz-space-xl); position: relative; padding-top: 8px; }
.feed__header::before { content: '◆'; display: block; font-size: 10px; color: var(--wz-orange); opacity: 0.4; margin-bottom: 10px; }
.feed__title { font-family: var(--wz-font-display); font-size: 26px; font-weight: 700; color: var(--wz-text); margin-bottom: 6px; letter-spacing: 0.04em; }
.feed__subtitle { font-size: 13px; color: var(--wz-text-muted); letter-spacing: 0.08em; }

.feed__grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--wz-space-md); }

.feed__card { background: var(--wz-bg-card); border-radius: 10px; overflow: hidden; cursor: pointer; transition: transform 0.3s var(--wz-ease-out), box-shadow 0.3s var(--wz-ease-out); animation: card-in 0.5s var(--wz-ease-out) both; }
.feed__card:nth-child(1) { animation-delay: 0s; }
.feed__card:nth-child(2) { animation-delay: 0.04s; }
.feed__card:nth-child(3) { animation-delay: 0.08s; }
.feed__card:nth-child(4) { animation-delay: 0.12s; }
@media (prefers-reduced-motion: reduce) { .feed__card { animation: none; } }
.feed__card:hover { transform: translateY(-3px); box-shadow: 0 8px 28px rgba(0,0,0,0.3); }
.feed__card-img { position: relative; aspect-ratio: 1; overflow: hidden; background: var(--wz-bg); }
.feed__card-img img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.4s var(--wz-ease-out); }
.feed__card:hover .feed__card-img img { transform: scale(1.06); }
.feed__card-badge { position: absolute; top: 6px; left: 6px; z-index: 2; font-size: 10px; font-weight: 700; padding: 2px 8px; border-radius: 999px; color: #fff; background: var(--wz-orange); line-height: 1.3; }
.feed__card-fav { position: absolute; top: 6px; right: 6px; z-index: 3; width: 26px; height: 26px; border-radius: 50%; background: rgba(0,0,0,0.45); border: none; color: rgba(255,255,255,0.7); cursor: pointer; display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity var(--wz-duration-normal) var(--wz-ease-out), background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out); backdrop-filter: blur(4px); }
.feed__card:hover .feed__card-fav { opacity: 1; }
.feed__card-fav:hover { background: rgba(255,107,53,0.85); color: #fff; transform: scale(1.1); }
.feed__card-fav--active { background: rgba(255,107,53,0.85); color: #fff; }
.feed__card-fav-heart { position: relative; z-index: 1; transition: transform 0.3s var(--wz-ease-out); }
.feed__card-fav--active .feed__card-fav-heart { fill: currentColor; animation: feed-fav-beat 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards; }
.feed__card-fav-ring { position: absolute; inset: -3px; border-radius: 50%; border: 2px solid var(--wz-orange); opacity: 0; pointer-events: none; }
.feed__card-fav--active .feed__card-fav-ring { animation: feed-fav-ripple 0.6s ease-out forwards; }
@keyframes feed-fav-beat { 0% { transform: scale(1); } 30% { transform: scale(1.5); } 60% { transform: scale(0.9); } 100% { transform: scale(1.2); } }
@keyframes feed-fav-ripple { 0% { transform: scale(0.8); opacity: 0.6; } 100% { transform: scale(2); opacity: 0; } }
.feed__card-body { padding: 10px 12px 12px; }
.feed__card-name { font-size: 13px; font-weight: 500; color: var(--wz-text); line-height: 1.4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 6px; }
.feed__card-prices { display: flex; align-items: center; gap: 5px; }
.feed__card-price { font-size: 14px; font-weight: 700; color: var(--wz-orange); line-height: 1; }
.feed__card-old { font-size: 11px; color: var(--wz-text-muted); text-decoration: line-through; line-height: 1; }

.feed__sentinel { grid-column: 1 / -1; padding: var(--wz-space-xl) 0; text-align: center; }
.feed__error { display: inline-flex; align-items: center; gap: 8px; font-size: 13px; color: var(--wz-text-muted); cursor: pointer; padding: 8px 20px; border: 1px solid var(--wz-border); border-radius: 999px; transition: color var(--wz-duration-fast) var(--wz-ease-out), border-color var(--wz-duration-fast) var(--wz-ease-out); }
.feed__error:hover { color: var(--wz-orange); border-color: var(--wz-orange); }
.feed__error-icon { display: flex; }
.feed__loading { display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 13px; color: var(--wz-text-muted); }
.feed__spinner { width: 18px; height: 18px; border: 2px solid var(--wz-border); border-top-color: var(--wz-orange); border-radius: 50%; animation: spin 0.7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.feed__finished { font-size: 13px; color: var(--wz-text-muted); letter-spacing: 0.08em; }

@media (max-width: 900px) { .feed__grid { grid-template-columns: repeat(3, 1fr); } }
@media (max-width: 640px) { .feed__grid { grid-template-columns: repeat(2, 1fr); gap: 10px; } }

@media (max-width: 768px) {
  .zone-rows { gap: var(--wz-space-md); }
  .zone-row--paired { flex-direction: column; gap: var(--wz-space-md); }
  .zone--half { width: 100%; }
  .zone--full .zone__products { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .zone--half .zone__products { gap: 10px; }
  .zone__title { font-size: 17px; }
  .zone__product-info { padding: 16px 8px 8px; }
  .zone-banner { height: 180px; }
  .zone-banner__content { padding: 0 24px; max-width: 280px; }
  .zone-banner__title { font-size: 20px; }
  .zone-banner__desc { font-size: 12px; }
}

/* ---- Footer ---- */
.footer__grid { display: grid; grid-template-columns: 1fr; gap: var(--wz-space-xl); padding-bottom: var(--wz-space-xl); border-bottom: 1px solid var(--wz-border); }

.footer__logo { font-family: 'Noto Serif SC', serif; font-size: 22px; font-weight: 700; color: var(--wz-orange); }
.footer__brand h3 { font-size: 20px; font-weight: 700; color: var(--wz-text); display: flex; align-items: center; gap: 6px; margin-bottom: 12px; }
.footer__desc { font-size: 14px; color: var(--wz-text-muted); line-height: 1.6; margin-bottom: 16px; max-width: 280px; }
.footer__social { display: flex; gap: 10px; }
.footer__social a { width: 36px; height: 36px; border-radius: 50%; background: var(--wz-bg-card); display: flex; align-items: center; justify-content: center; color: var(--wz-text-soft); transition: background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out); }
.footer__social a:hover { background: var(--wz-orange); color: #fff; }
.footer__col h4 { font-size: 16px; font-weight: 600; color: var(--wz-text); margin-bottom: 16px; }
.footer__col ul { list-style: none; display: flex; flex-direction: column; gap: 10px; }
.footer__col a { font-size: 14px; color: var(--wz-text-muted); transition: color var(--wz-duration-fast) var(--wz-ease-out); }
.footer__col a:hover { color: var(--wz-orange); }
.footer__col li { font-size: 14px; color: var(--wz-text-muted); display: flex; align-items: center; gap: 8px; }
.footer__bottom { text-align: center; padding-top: 24px; font-size: 13px; color: var(--wz-text-muted); }
.footer__logo { transition: text-shadow var(--wz-duration-normal) var(--wz-ease-out); }
.footer__brand:hover .footer__logo { text-shadow: 0 0 16px rgba(255,107,53,0.30), 0 0 48px rgba(255,107,53,0.10); }

/* ---- Scroll to Top ---- */
.scroll-top { position: fixed; bottom: 32px; right: 32px; z-index: 90; width: 44px; height: 44px; border-radius: 50%; background: var(--wz-bg-card); border: 1px solid var(--wz-border); color: var(--wz-text-soft); display: flex; align-items: center; justify-content: center; cursor: pointer; box-shadow: 0 4px 16px rgba(0,0,0,0.25); transition: background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out), border-color var(--wz-duration-fast) var(--wz-ease-out), box-shadow var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out); }
.scroll-top:hover { background: var(--wz-orange); color: #fff; border-color: var(--wz-orange); transform: translateY(-3px); box-shadow: 0 8px 28px rgba(255,107,53,0.30); }
.scroll-top:active { transform: translateY(-1px); }
.scroll-top-enter-active { animation: scroll-in 0.35s cubic-bezier(0.34, 1.56, 0.64, 1); }
.scroll-top-leave-active { animation: scroll-in 0.2s var(--wz-ease-out) reverse; }
@keyframes scroll-in {
  0% { opacity: 0; transform: scale(0.6) translateY(12px); }
  100% { opacity: 1; transform: scale(1) translateY(0); }
}

@media (max-width: 640px) { .scroll-top { bottom: 20px; right: 20px; width: 40px; height: 40px; } }
@media (prefers-reduced-motion: reduce) { .scroll-top { transition: none; } .scroll-top-enter-active, .scroll-top-leave-active { animation: none; } }
</style>
