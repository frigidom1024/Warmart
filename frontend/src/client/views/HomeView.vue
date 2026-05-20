<script setup lang="ts">
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategoryList, getProductList } from '@/api/product'
import { addFavorite } from '@/api/favorite'
import { showToast } from '@/utils/toast'
import type { Category as ApiCategory, Product } from '@/api/product'

// Categories from API
const categoryTree = ref<ApiCategory[]>([])
const loading = ref(true)

const catIconMap: Record<string, string> = {
  '服饰鞋包': 'shirt', '数码家电': 'laptop', '家居生活': 'home',
  '美妆个护': 'sparkles', '运动户外': 'activity',
  '食品饮料': 'food', '母婴用品': 'baby', '图书文具': 'books',
  '宠物生活': 'pets', '汽车用品': 'auto', '医药健康': 'health',
  '珠宝配饰': 'jewelry',
}

const categories = computed(() =>
  categoryTree.value.map(c => ({
    label: c.name,
    icon: catIconMap[c.name] || 'default',
    children: c.children?.map(ch => ch.name) || []
  }))
)

const catIcons: Record<string, string> = {
  default: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>',
  shirt: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M6 5 3 8l4 4 2-2"/><path d="M18 5l3 3-4 4-2-2"/><path d="M6 5h4v2a2 2 0 0 0 4 0V5h4"/><path d="M9 19V9"/><path d="M15 19V9"/></svg>',
  sparkles: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3 13.5 7.5 18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5Z"/><path d="M5 16 6 16.5 6.5 17.5 7 16.5 8 16 7 15.5 6.5 14.5 6 15.5Z"/><path d="M18 14 19 14.5 19.5 15.5 20 14.5 21 14 20 13.5 19.5 12.5 19 13.5Z"/></svg>',
  laptop: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="13" rx="2"/><path d="M2 20h20"/><path d="M10 16v4"/><path d="M14 16v4"/></svg>',
  home: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12 12 3 21 12"/><path d="M5 10v10a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1v-4a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v4a1 1 0 0 0 1 1h3a1 1 0 0 0 1-1V10"/></svg>',
  cookie: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><circle cx="8" cy="9" r="1"/><circle cx="16" cy="8" r="1"/><circle cx="13" cy="14" r="1"/><circle cx="9" cy="16" r="1"/><circle cx="15" cy="13" r="1"/></svg>',
  activity: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="5" r="2.5"/><path d="M10 21 13 15 16 18 20 9"/><path d="M7 13 10 10 13 13"/></svg>',
  food: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8a6 6 0 0 1-6 6"/><path d="M6 2v4a6 6 0 0 0 12 0V2"/><path d="M2 22h20"/><path d="M8 22v-8"/><path d="M16 22v-8"/></svg>',
  baby: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6z"/><path d="M7 15c0-2.8 2.2-5 5-5s5 2.2 5 5"/><path d="M3 22c2-3 5-5 9-5s7 2 9 5"/></svg>',
  books: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1 0-5H20"/><path d="M12 6v7"/><path d="M9 9h6"/></svg>',
  pets: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 17a5 5 0 0 0 5-5c0-2-1-3.5-2.5-4.5"/><path d="M8 12a3 3 0 0 0 3-3c0-1.2-.6-2-1.5-2.5"/><circle cx="4" cy="8" r="1"/><circle cx="20" cy="8" r="1"/><path d="M2 14c0 5 4.5 8 10 8s10-3 10-8"/></svg>',
  auto: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M5 17a2 2 0 0 1-2-2V9l2-5h14l2 5v6a2 2 0 0 1-2 2"/><path d="M5 17h14"/><circle cx="7" cy="17" r="2"/><circle cx="17" cy="17" r="2"/></svg>',
  health: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>',
  jewelry: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L9 7l3 4 3-4-3-5z"/><path d="M9 7H5l-1 5 8 10"/><path d="M15 7h4l1 5-8 10"/></svg>',
}

const hoveredCat = ref(-1)
let hideTimer: ReturnType<typeof setTimeout> | null = null

function onCatEnter(i: number) {
  hoveredCat.value = i
  if (hideTimer) { clearTimeout(hideTimer); hideTimer = null }
}

function onBrowseLeave() {
  hideTimer = setTimeout(() => { hoveredCat.value = 0 }, 300)
}

function cancelHide() {
  if (hideTimer) { clearTimeout(hideTimer); hideTimer = null }
}

onMounted(async () => {
  try {
    const [catRes, productRes] = await Promise.all([
      getCategoryList(),
      getProductList({ size: 50 })
    ])
    categoryTree.value = catRes as ApiCategory[]

    const products = (productRes as any).records || (productRes as Product[]) || []

    // Fisher-Yates shuffle
    function shuffleArray<T>(arr: T[]): T[] {
      const a = [...arr]
      for (let i = a.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [a[i], a[j]] = [a[j], a[i]]
      }
      return a
    }

    // Build category id → zone index (include all descendant IDs)
    const catToZone: Record<number, number> = {}
    function walk(cats: ApiCategory[], zoneIdx: number) {
      for (const cat of cats) {
        catToZone[cat.id] = zoneIdx
        if (cat.children?.length) walk(cat.children, zoneIdx)
      }
    }
    categoryTree.value.forEach((cat, i) => {
      if (i < 12) walk([cat], i)
    })

    // Max products per zone
    const zoneLimits: Record<string, number> = {
      clothing: 8, digital: 6, home: 6,
      beauty: 6, sports: 6, food: 6,
      baby: 6, books: 6, pets: 6,
      auto: 6, health: 6, jewelry: 6,
      hotsale: 8,
    }

    // Group products into zones, randomize & limit
    const buckets: Zone[] = zoneNames.map((id, i) => ({
      id, title: zoneTitles[id], size: zoneSizes[id], products: []
    }))

    for (const p of (Array.isArray(products) ? products : [])) {
      const pi = p as Product
      const zoneIdx = catToZone[pi.categoryId]
      const zp: ZoneProduct = {
        id: pi.id, title: pi.name, price: pi.price,
        old: pi.originalPrice, tag: pi.tag, img: pi.mainImage
      }
      if (zoneIdx !== undefined && zoneIdx < 12) {
        buckets[zoneIdx].products.push(zp)
      }
    }

    // Randomize each zone and limit count
    for (const zone of buckets) {
      zone.products = shuffleArray(zone.products).slice(0, zoneLimits[zone.id] || 8)
    }

    // Hotsale: random selection across all products
    const allZp = (Array.isArray(products) ? products : []).map((pi: Product) => ({
      id: pi.id, title: pi.name, price: pi.price,
      old: pi.originalPrice, tag: pi.tag, img: pi.mainImage
    }))
    buckets[zoneNames.indexOf('hotsale')].products = shuffleArray(allZp).slice(0, 8)

    zones.value = buckets
  } catch {
    // keep empty
  } finally {
    loading.value = false
  }
})
onUnmounted(() => {
  if (hideTimer) clearTimeout(hideTimer)
})

const catZoneMap: Record<string, string> = {
  '服饰鞋包': 'clothing', '数码家电': 'digital', '家居生活': 'home',
  '美妆个护': 'beauty', '运动户外': 'sports',
  '食品饮料': 'food', '母婴用品': 'baby', '图书文具': 'books',
  '宠物生活': 'pets', '汽车用品': 'auto', '医药健康': 'health',
  '珠宝配饰': 'jewelry',
}

const hoveredCatProducts = computed(() => {
  const cat = categories.value[hoveredCat.value]
  if (!cat) return []
  const zoneId = catZoneMap[cat.label]
  if (!zoneId) return []
  const zone = zones.value.find(z => z.id === zoneId)
  return zone ? zone.products.slice(0, 3) : []
})

const router = useRouter()

const slides = [
  {
    label: '严选好物 · 品质生活',
    title: '精选全球好物<br>一站式轻松购齐',
    desc: '正品直供极速发货，严选轻奢穿搭、家居日用、美妆护肤、数码潮品全品类覆盖',
    btn: '即刻选购',
    btn2: '查看爆款',
    img: 'https://picsum.photos/id/26/700/600',
    align: 'left'
  },
  {
    label: '新品首发 · 限时特惠',
    title: '夏季新品全面上架<br>低至 5 折起',
    desc: '每周上新数百款爆品，限时秒杀专区全场包邮，潮流尖货抢先入手',
    btn: '抢购新品',
    btn2: '查看活动',
    img: 'https://picsum.photos/id/1/700/600',
    align: 'center'
  },
  {
    label: '数码狂欢 · 潮品直降',
    title: '数码家电超级品类日<br>大牌爆款直降千元',
    desc: 'Apple、华为、小米等大牌齐聚，以旧换新增值补贴，最高享 24 期免息',
    btn: '立即抢购',
    btn2: '查看活动',
    img: 'https://picsum.photos/id/0/700/600',
    align: 'right'
  },
  {
    label: '会员专享 · 积分加倍',
    title: '开通年度会员<br>立享双倍积分',
    desc: '会员专属价、生日礼包、免邮特权、专属客服四大权益，一年省回本',
    btn: '开通会员',
    btn2: '了解更多',
    img: 'https://picsum.photos/id/42/700/600',
    align: 'center'
  }
]
const currentSlide = ref(0)
let slideTimer: ReturnType<typeof setInterval> | null = null

function startAutoplay() {
  stopAutoplay()
  slideTimer = setInterval(() => {
    currentSlide.value = (currentSlide.value + 1) % slides.length
  }, 5000)
}
function stopAutoplay() {
  if (slideTimer) { clearInterval(slideTimer); slideTimer = null }
}
function goSlide(i: number) {
  currentSlide.value = i
  stopAutoplay(); startAutoplay()
}
function prevSlide() { goSlide((currentSlide.value - 1 + slides.length) % slides.length) }
function nextSlide() { goSlide((currentSlide.value + 1) % slides.length) }

onMounted(startAutoplay)

const stats = [
  { value: '100W+', label: '忠实用户' },
  { value: '5000+', label: '入驻品牌' },
  { value: '99%', label: '好评率' }
]

interface ZoneProduct {
  id: number
  title: string
  price: number
  old: number | null
  tag: string | null
  img: string
}

interface Zone {
  id: string
  title: string
  size: string
  products: ZoneProduct[]
}

const zoneNames = ['clothing', 'digital', 'home', 'beauty', 'sports', 'food', 'baby', 'books', 'pets', 'auto', 'health', 'jewelry', 'hotsale']
const zoneTitles: Record<string, string> = {
  clothing: '潮流服饰', digital: '数码家电', home: '家居日用',
  beauty: '美妆护肤', sports: '运动户外', food: '食品饮料',
  baby: '母婴用品', books: '图书文具', pets: '宠物生活',
  auto: '汽车用品', health: '医药健康', jewelry: '珠宝配饰',
  hotsale: '热销爆款',
}
const zoneSizes: Record<string, string> = {
  clothing: '4x4', digital: '2x1', home: '2x1',
  beauty: '2x1', sports: '2x1', food: '2x1',
  baby: '2x1', books: '2x1', pets: '2x1',
  auto: '2x1', health: '2x1', jewelry: '2x1',
  hotsale: '4x4',
}

const zones = ref<Zone[]>([])

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

const sections = computed(() => {
  const result: any[] = []
  for (let i = 0; i < zones.value.length; i++) {
    result.push({ ...zones.value[i], _type: 'zone' })
    if (i === 2) result.push({ _type: 'banner', ...banners[0] })
    if (i === 4) result.push({ _type: 'banner', ...banners[1] })
  }
  return result
})

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

function goProductList() {
  router.push('/product/list')
}

function goProductDetail(id: number) {
  window.open('/product/detail/' + id, '_blank')
}
</script>

<template>
  <div class="home">
    <!-- ============ HERO CAROUSEL ============ -->
    <section class="hero" @mouseenter="stopAutoplay" @mouseleave="startAutoplay">
      <div class="hero__track" :style="{ transform: `translateX(-${currentSlide * 100}%)` }">
        <div
          v-for="(slide, i) in slides"
          :key="i"
          class="hero__slide"
          :class="[`hero__slide--${slide.align}`]"
        >
          <div class="hero__bg">
            <img :src="slide.img" alt="" class="hero__bg-img">
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

      <!-- Arrows -->
      <button class="hero__arrow hero__arrow--prev" @click="prevSlide">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
      </button>
      <button class="hero__arrow hero__arrow--next" @click="nextSlide">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
      </button>

      <!-- Dots -->
      <div class="hero__dots">
        <button
          v-for="(_, i) in slides"
          :key="i"
          class="hero__dot"
          :class="{ 'hero__dot--active': i === currentSlide }"
          @click="goSlide(i)"
        />
      </div>
    </section>

    <!-- ============ CATEGORY BROWSE ============ -->
    <section class="cat-browse">
      <!-- Decorative header -->
      <div class="cat-browse__header">
        <div class="section__inner">
          <div class="cat-browse__header-inner">
            <span class="cat-browse__ornament"></span>
            <span class="cat-browse__slogan">每一件好物，都值得被认真挑选</span>
            <span class="cat-browse__ornament"></span>
          </div>
        </div>
      </div>

      <div class="cat-browse__body"
        @mouseleave="onBrowseLeave"
      >
        <div class="section__inner">
          <div class="cat-browse__grid">
            <button
              v-for="(cat, i) in categories"
              :key="cat.label"
              class="cat-browse__tile"
              :class="{ 'cat-browse__tile--active': hoveredCat === i }"
              @mouseenter="onCatEnter(i)"
              @click="goProductList"
            >
              <span class="cat-browse__tile-icon" v-html="catIcons[cat.icon]" />
              <span class="cat-browse__tile-label">{{ cat.label }}</span>
            </button>
          </div>
        </div>

        <!-- Flyout panel (reuses trigger panel styles) -->
        <div
          class="cat-trigger__panel"
          :class="{ 'cat-trigger__panel--visible': hoveredCat > 0 }"
          @mouseenter="cancelHide"
        >
          <div class="section__inner">
            <div class="cat-trigger__panel-inner">
              <div class="cat-trigger__left">
                <button
                  v-for="(cat, i) in categories"
                  :key="cat.label"
                  class="cat-trigger__cat-btn"
                  :class="{ 'cat-trigger__cat-btn--active': hoveredCat === i }"
                  @mouseenter="onCatEnter(i)"
                >
                  <span class="cat-trigger__cat-icon" v-html="catIcons[cat.icon]" />
                  <span class="cat-trigger__cat-label">{{ cat.label }}</span>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="cat-trigger__cat-arrow"><path d="M9 18l6-6-6-6"/></svg>
                </button>
              </div>
              <div class="cat-trigger__right">
                <div class="cat-trigger__subcats">
                  <span
                    v-for="child in categories[hoveredCat]?.children"
                    :key="child"
                    class="cat-trigger__subcat-tag"
                    @click="goProductList"
                  >{{ child }}</span>
                </div>
                <div v-if="hoveredCatProducts.length" class="cat-trigger__products">
                  <div
                    v-for="p in hoveredCatProducts"
                    :key="p.id"
                    class="cat-trigger__product"
                    @click="goProductDetail(p.id)"
                  >
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

    <!-- ============ ZONE GRID ============ -->
    <section class="section">
      <div class="section__inner">
        <div class="zone-grid">
          <template v-for="item in sections" :key="item._type === 'banner' ? item.id : item.id">
            <div v-if="item._type === 'banner'" class="zone-banner" :class="`zone-banner--${item.align}`">
              <div class="zone-banner__bg">
                <img :src="item.img" alt="" class="zone-banner__bg-img">
                <div class="zone-banner__overlay" />
              </div>
              <div class="zone-banner__content">
                <span class="zone-banner__label">{{ item.subtitle }}</span>
                <h3 class="zone-banner__title">{{ item.title }}</h3>
                <p class="zone-banner__desc">{{ item.desc }}</p>
                <button class="zone-banner__btn" @click="goProductList">{{ item.btn }}</button>
              </div>
            </div>
            <div v-else :class="['zone', `zone--${item.size}`]">
              <div class="zone__header" @click="goProductList">
                <h3 class="zone__title">{{ item.title }}</h3>
              </div>
              <div class="zone__products">
                <div
                  v-for="p in item.products"
                  :key="p.id"
                  class="zone__product"
                  @click="goProductDetail(p.id)"
                >
                  <div class="zone__product-img">
                    <img :src="p.img" :alt="p.title">
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
    </section>

    <!-- ============ FOOTER ============ -->
    <footer class="footer">
      <div class="section__inner">
        <div class="footer__grid">
          <div class="footer__brand">
            <h3><span class="footer__logo">暖</span> Warmart</h3>
            <p class="footer__desc">专注品质好物，打造轻松便捷高品质网购平台</p>
            <div class="footer__social">
              <a href="#"><i class="fa-brands fa-weixin"></i></a>
              <a href="#"><i class="fa-brands fa-weibo"></i></a>
              <a href="#"><i class="fa-brands fa-tiktok"></i></a>
            </div>
          </div>
          <div class="footer__col">
            <h4>购物指南</h4>
            <ul>
              <li><a href="#">注册登录</a></li>
              <li><a href="#">下单流程</a></li>
              <li><a href="#">支付方式</a></li>
              <li><a href="#">常见问题</a></li>
            </ul>
          </div>
          <div class="footer__col">
            <h4>售后服务</h4>
            <ul>
              <li><a href="#">退换货政策</a></li>
              <li><a href="#">物流查询</a></li>
              <li><a href="#">售后投诉</a></li>
              <li><a href="#">正品保障</a></li>
            </ul>
          </div>
          <div class="footer__col">
            <h4>联系我们</h4>
            <ul>
              <li><i class="fa-solid fa-phone mr-2"></i>400-888-9999</li>
              <li><i class="fa-solid fa-envelope mr-2"></i>service@warmart.com</li>
              <li><i class="fa-solid fa-location-dot mr-2"></i>杭州市电商产业园</li>
            </ul>
          </div>
        </div>
        <div class="footer__bottom">
          <p>© 2026 暖 Warmart 版权所有 浙ICP备12345678号</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
/* =============================================
   Home Page — Warmart Dark E-commerce
   ============================================= */

.home {
  padding-top: 60px;
  background: var(--wz-bg);
}

/* ---- Hero Carousel ---- */
.hero {
  position: relative;
  overflow: hidden;
  background: var(--wz-bg);
  height: 580px;
}

@media (max-width: 768px) {
  .hero { height: 460px; }
}

/* Track — horizontal slide */
.hero__track {
  display: flex;
  height: 100%;
  transition: transform 0.7s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

/* Each slide */
.hero__slide {
  position: relative;
  min-width: 100%;
  display: flex;
  align-items: center;
  overflow: hidden;
}

/* Background image */
.hero__bg {
  position: absolute;
  inset: 0;
}

.hero__bg-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero__bg-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(15, 15, 17, 0.92) 0%,
    rgba(15, 15, 17, 0.60) 40%,
    rgba(15, 15, 17, 0.30) 100%
  );
}

/* Container inside slide */
.hero__container {
  position: relative;
  z-index: 2;
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 48px;
}

/* Content positioning */
.hero__content {
  max-width: 560px;
}

.hero__slide--center .hero__container {
  text-align: center;
}
.hero__slide--center .hero__content {
  margin: 0 auto;
}
.hero__slide--center .hero__desc {
  margin-left: auto;
  margin-right: auto;
}
.hero__slide--center .hero__btns {
  justify-content: center;
}
.hero__slide--center .hero__stats {
  justify-content: center;
}

.hero__slide--right .hero__container {
  display: flex;
  justify-content: flex-end;
}

/* Label */
.hero__label {
  display: inline-block;
  font-size: 12px;
  font-weight: 500;
  color: var(--wz-orange);
  letter-spacing: 0.18em;
  text-transform: uppercase;
  background: rgba(255, 107, 53, 0.12);
  padding: 4px 16px;
  border-radius: 999px;
  margin-bottom: 16px;
}

/* Title */
.hero__title {
  font-family: var(--wz-font-display);
  font-size: 44px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--wz-text);
  margin: 0 0 16px;
}

@media (max-width: 768px) {
  .hero__title { font-size: 30px; }
}

/* Description */
.hero__desc {
  font-size: 16px;
  color: var(--wz-text-soft);
  line-height: 1.7;
  max-width: 460px;
  margin-bottom: 28px;
}

/* Buttons */
.hero__btns {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero__btn-primary {
  padding: 14px 34px;
  background: var(--wz-orange);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out),
              transform var(--wz-duration-normal) var(--wz-ease-out);
}

.hero__btn-primary:hover {
  box-shadow: 0 8px 28px rgba(255, 107, 53, 0.35);
  transform: translateY(-2px);
}

.hero__btn-ghost {
  padding: 14px 34px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 10px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.hero__btn-ghost:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}

/* Stats (first slide only) */
.hero__stats {
  display: flex;
  gap: 40px;
  margin-top: 40px;
}

.hero__stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--wz-text);
  line-height: 1.2;
}

.hero__stat-label {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-top: 4px;
}

/* ---- Arrow Navigation ---- */
.hero__arrow {
  position: absolute;
  top: 50%;
  z-index: 5;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: opacity var(--wz-duration-normal) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out);
  transform: translateY(-50%) scale(0.9);
}

.hero:hover .hero__arrow {
  opacity: 1;
  transform: translateY(-50%) scale(1);
}

.hero__arrow:hover {
  background: rgba(255, 255, 255, 0.12);
}

.hero__arrow--prev { left: 24px; }
.hero__arrow--next { right: 24px; }

/* ---- Dots ---- */
.hero__dots {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 5;
  display: flex;
  gap: 10px;
}

.hero__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  border: none;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out),
              width var(--wz-duration-fast) var(--wz-ease-out);
  padding: 0;
}

.hero__dot--active {
  width: 28px;
  border-radius: 4px;
  background: var(--wz-orange);
}

.hero__dot:hover:not(.hero__dot--active) {
  background: rgba(255, 255, 255, 0.5);
}

/* ---- Category Browse (merged transition + category grid) ---- */
.cat-browse {
  background: var(--wz-bg-elevated);
}

.cat-browse__header {
  padding: 40px 16px;
  background: var(--wz-orange);
}

.cat-browse__header-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.cat-browse__ornament {
  width: 60px;
  height: 1px;
  background: rgba(255, 255, 255, 0.5);
}

.cat-browse__slogan {
  font-family: var(--wz-font-display);
  font-size: 20px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.3em;
  white-space: nowrap;
}

/* Body wraps grid + flyout */
.cat-browse__body {
  position: relative;
}

/* Category tile grid */
.cat-browse__grid {
  display: grid;
  grid-template-columns: repeat(12, 1fr);
  gap: 8px;
  padding: 20px 0;
}

.cat-browse__tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px 14px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 10px;
  cursor: pointer;
  font-family: var(--wz-font-body);
  color: var(--wz-text-soft);
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              border-color var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.cat-browse__tile:hover {
  background: var(--wz-bg-hover);
  border-color: var(--wz-orange-muted);
  color: var(--wz-text);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}

.cat-browse__tile--active {
  background: var(--wz-orange-muted);
  border-color: var(--wz-orange);
  color: var(--wz-orange);
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.15);
}

.cat-browse__tile-icon {
  display: flex;
  align-items: center;
  opacity: 0.6;
  transition: opacity var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out);
}

.cat-browse__tile:hover .cat-browse__tile-icon {
  opacity: 1;
  transform: scale(1.1);
}

.cat-browse__tile--active .cat-browse__tile-icon {
  opacity: 1;
  transform: scale(1.1);
}

.cat-browse__tile-label {
  font-size: 12px;
  font-weight: 500;
  text-align: center;
  line-height: 1.3;
}

@media (max-width: 1024px) {
  .cat-browse__grid { grid-template-columns: repeat(6, 1fr); }
}

@media (max-width: 640px) {
  .cat-browse__header { padding: 24px 16px; }
  .cat-browse__slogan { font-size: 16px; letter-spacing: 0.15em; }
  .cat-browse__ornament { width: 32px; }
  .cat-browse__grid { grid-template-columns: repeat(4, 1fr); gap: 6px; padding: 12px 0; }
  .cat-browse__tile { padding: 12px 6px; }
}

/* Flyout panel (shared with cat-trigger styles) */
.cat-trigger__panel {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 50;
  padding: 24px 0 28px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-top: none;
  border-radius: 0 0 14px 14px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.4);
  opacity: 0;
  transform: translateY(-6px);
  pointer-events: none;
  transition: opacity 0.18s var(--wz-ease-out), transform 0.18s var(--wz-ease-out);
}

.cat-trigger__panel--visible {
  opacity: 1;
  transform: translateY(0);
  pointer-events: auto;
}

.cat-trigger__panel-inner {
  display: flex;
  gap: 32px;
  min-height: 440px;
}

/* Left: category list */
.cat-trigger__left {
  width: 170px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.cat-trigger__cat-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: none;
  border: none;
  border-radius: 8px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text-soft);
  cursor: pointer;
  text-align: left;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.cat-trigger__cat-btn:hover {
  background: var(--wz-bg-hover);
  color: var(--wz-text);
}

.cat-trigger__cat-btn--active {
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
  font-weight: 600;
}

.cat-trigger__cat-icon {
  display: flex;
  align-items: center;
  opacity: 0.6;
  flex-shrink: 0;
}

.cat-trigger__cat-btn--active .cat-trigger__cat-icon {
  opacity: 1;
}

.cat-trigger__cat-label {
  flex: 1;
}

.cat-trigger__cat-arrow {
  opacity: 0;
  transition: opacity var(--wz-duration-fast) var(--wz-ease-out);
  flex-shrink: 0;
}

.cat-trigger__cat-btn--active .cat-trigger__cat-arrow {
  opacity: 0.6;
}

/* Right: subcategories + products */
.cat-trigger__right {
  flex: 1;
  min-width: 0;
}

.cat-trigger__subcats {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--wz-border);
}

.cat-trigger__subcat-tag {
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text);
  background: var(--wz-bg);
  border-radius: 999px;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out);
}

.cat-trigger__subcat-tag:hover {
  background: var(--wz-orange);
  color: #fff;
  transform: translateY(-1px);
}

/* Recommended products */
.cat-trigger__products {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.cat-trigger__product {
  cursor: pointer;
}

.cat-trigger__product-img {
  position: relative;
  aspect-ratio: 3 / 2;
  border-radius: 6px;
  overflow: hidden;
  background: var(--wz-bg);
}

.cat-trigger__product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s var(--wz-ease-out);
}

.cat-trigger__product:hover .cat-trigger__product-img img {
  transform: scale(1.06);
}

.cat-trigger__product-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16px 8px 7px;
  background: linear-gradient(transparent 20%, rgba(0, 0, 0, 0.8) 60%, rgba(0, 0, 0, 0.92));
  border-radius: 0 0 6px 6px;
}

.cat-trigger__product-name {
  font-size: 12px;
  color: #fff;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 3px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

.cat-trigger__product-price {
  font-size: 14px;
  font-weight: 700;
  color: var(--wz-orange);
}


@media (max-width: 768px) {
  .cat-trigger__panel {
    padding: 16px 0 20px;
  }
  .cat-trigger__panel-inner {
    flex-direction: column;
    gap: 16px;
    min-height: 0;
  }
  .cat-trigger__left {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 4px;
  }
  .cat-trigger__cat-btn {
    padding: 6px 12px;
    font-size: 13px;
    flex: 0 0 auto;
  }
  .cat-trigger__cat-arrow {
    display: none;
  }
  .cat-trigger__products {
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }
}

/* ---- Sections ---- */
.section {
  padding: 40px 16px 80px;
}

/* ---- Sections ---- */
.section {
  padding: 40px 16px 80px;
}

.section--alt {
  background: var(--wz-bg);
  border-top: 1px solid var(--wz-border);
  border-bottom: 1px solid var(--wz-border);
}

.section__inner {
  max-width: 1400px;
  margin: 0 auto;
}

.section__inner--narrow {
  max-width: 640px;
}

.section__header {
  text-align: center;
  margin-bottom: 48px;
}

.section__header--row {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  text-align: left;
}

.section__overline {
  font-size: 12px;
  color: var(--wz-orange);
  letter-spacing: 0.1em;
  font-weight: 500;
}

.section__title {
  font-family: var(--wz-font-display);
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-text);
  margin-bottom: 8px;
}

.section__sub {
  font-size: 15px;
  color: var(--wz-text-muted);
}

.section__more {
  font-size: 14px;
  color: var(--wz-text-soft);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
  flex-shrink: 0;
}

.section__more:hover {
  color: var(--wz-orange);
}

/* ---- Zone Grid (waterfall) ---- */
.zone-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 36px;
  align-items: start;
  grid-auto-flow: dense;
}

.zone {
  /* no card background — transparent on page */
}

/* Zone sizes for waterfall packing */
.zone--4x4 {
  grid-column: span 4;
}

.zone--2x1 {
  grid-column: span 2;
}

.zone__header {
  padding: 0 0 12px;
  cursor: pointer;
}

.zone__title {
  transition: transform 0.3s var(--wz-ease-out);
  transform-origin: left;
  font-family: var(--wz-font-display);
  font-size: 20px;
  font-weight: 700;
  color: var(--wz-text);
  line-height: 1.3;
  position: relative;
  padding-left: 14px;
}

.zone__title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 2px;
  background: var(--wz-orange);
  transition: width 0.3s var(--wz-ease-out), box-shadow 0.3s var(--wz-ease-out);
}

.zone__header:hover .zone__title {
  transform: translateX(4px) scale(1.04);
}

.zone__header:hover .zone__title::before {
  width: 4px;
  top: 2px;
  bottom: 2px;
  background: var(--wz-orange);
  box-shadow: 0 0 14px rgba(255, 107, 53, 0.6);
}

.zone__title::after {
  content: '›';
  display: inline-block;
  margin-left: 4px;
  font-family: var(--wz-font-body);
  font-size: 22px;
  font-weight: 400;
  color: var(--wz-text-muted);
  transition: transform 0.3s var(--wz-ease-out), color 0.3s var(--wz-ease-out);
  line-height: 1;
}

.zone__header:hover .zone__title::after {
  transform: translateX(4px);
  color: var(--wz-orange);
}

/* Product container */
.zone__products {
  padding: 0;
}

/* 4×4 zone: 4-column product grid */
.zone--4x4 .zone__products {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

/* 2×1 zone: 2 products side by side */
.zone--2x1 .zone__products {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.zone__product {
  cursor: pointer;
}

.zone__product-img {
  position: relative;
  overflow: hidden;
  aspect-ratio: 1;
  border-radius: 8px;
  background: var(--wz-bg-card);
}

.zone__product-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s var(--wz-ease-out);
}

.zone__product:hover .zone__product-img img {
  transform: scale(1.06);
}

.zone__product-info {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 32px 10px 10px;
  background: linear-gradient(transparent 20%, rgba(0, 0, 0, 0.8) 60%, rgba(0, 0, 0, 0.92));
  border-radius: 0 0 8px 8px;
}

.zone__product-name {
  font-size: 12px;
  color: #fff;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 4px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

/* Product badges */
.zone__product-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  z-index: 2;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 999px;
  color: #fff;
  background: var(--wz-orange);
  line-height: 1.3;
  letter-spacing: 0.04em;
}

.zone__product:nth-child(2) .zone__product-badge {
  background: var(--wz-success);
}

.zone__product:nth-child(3) .zone__product-badge {
  background: #af52de;
}

.zone__product:nth-child(4) .zone__product-badge {
  background: #007aff;
}

/* Favorite button */
.zone__product-fav {
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

.zone__product:hover .zone__product-fav {
  opacity: 1;
}

.zone__product-fav:hover {
  background: rgba(255, 107, 53, 0.85);
  color: #fff;
  transform: scale(1.1);
}

/* Favorite button animation */
.zone__product-fav--active {
  background: rgba(255, 107, 53, 0.85);
  color: #fff;
}

.zone__product-fav-heart {
  position: relative;
  z-index: 1;
  transition: transform 0.3s var(--wz-ease-out);
}

.zone__product-fav--active .zone__product-fav-heart {
  fill: currentColor;
  animation: fav-beat 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
}

.zone__product-fav-ring {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 2px solid var(--wz-orange);
  opacity: 0;
  pointer-events: none;
}

.zone__product-fav--active .zone__product-fav-ring {
  animation: fav-ripple 0.6s ease-out forwards;
}

@keyframes fav-beat {
  0% { transform: scale(1); }
  30% { transform: scale(1.5); }
  60% { transform: scale(0.9); }
  100% { transform: scale(1.2); }
}

@keyframes fav-ripple {
  0% { transform: scale(0.8); opacity: 0.6; }
  100% { transform: scale(2); opacity: 0; }
}

/* Prices row */
.zone__product-prices {
  display: flex;
  align-items: center;
  gap: 6px;
}

.zone__product-price {
  font-size: 14px;
  font-weight: 700;
  color: var(--wz-orange);
  line-height: 1;
}

.zone__product-old {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  text-decoration: line-through;
  line-height: 1;
}

/* Hot sale zone: more emphasis on prices */
.zone:last-child .zone__product-price {
  font-size: 15px;
}

.zone:last-child .zone__product-badge {
  background: var(--wz-danger);
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* ---- Zone Banners ---- */
.zone-banner {
  position: relative;
  grid-column: span 4;
  border-radius: 10px;
  overflow: hidden;
  height: 220px;
  cursor: pointer;
}

.zone-banner__bg {
  position: absolute;
  inset: 0;
}

.zone-banner__bg-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s var(--wz-ease-out);
}

.zone-banner:hover .zone-banner__bg-img {
  transform: scale(1.05);
}

.zone-banner__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    135deg,
    rgba(15, 15, 17, 0.88) 0%,
    rgba(15, 15, 17, 0.45) 50%,
    rgba(15, 15, 17, 0.20) 100%
  );
}

.zone-banner--right .zone-banner__overlay {
  background: linear-gradient(
    225deg,
    rgba(15, 15, 17, 0.88) 0%,
    rgba(15, 15, 17, 0.45) 50%,
    rgba(15, 15, 17, 0.20) 100%
  );
}

.zone-banner__content {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 48px;
  max-width: 420px;
}

.zone-banner--right .zone-banner__content {
  margin-left: auto;
  text-align: right;
  align-items: flex-end;
}

.zone-banner__label {
  display: inline-block;
  font-size: 11px;
  font-weight: 500;
  color: var(--wz-orange);
  letter-spacing: 0.15em;
  background: rgba(255, 107, 53, 0.15);
  padding: 3px 14px;
  border-radius: 999px;
  margin-bottom: 12px;
  align-self: flex-start;
}

.zone-banner--right .zone-banner__label {
  align-self: flex-end;
}

.zone-banner__title {
  font-family: var(--wz-font-display);
  font-size: 26px;
  font-weight: 700;
  color: var(--wz-text);
  margin-bottom: 8px;
  line-height: 1.2;
}

.zone-banner__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.5;
  margin-bottom: 18px;
}

.zone-banner__btn {
  align-self: flex-start;
  padding: 10px 28px;
  background: var(--wz-orange);
  border: none;
  border-radius: 999px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out),
              transform var(--wz-duration-normal) var(--wz-ease-out);
}

.zone-banner--right .zone-banner__btn {
  align-self: flex-end;
}

.zone-banner__btn:hover {
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.35);
  transform: translateY(-1px);
}

@media (max-width: 768px) {
  .zone-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }
  .zone--4x4 { grid-column: span 2; }
  .zone--2x1 { grid-column: span 2; }

  .zone--4x4 .zone__products {
    grid-template-columns: repeat(2, 1fr);
  }

  .zone__title { font-size: 17px; }
  .zone--4x4 .zone__products { gap: 10px; }
  .zone--2x1 .zone__products { gap: 10px; }
  .zone__product-info { padding: 16px 8px 8px; }
  .zone-banner { grid-column: span 2; height: 180px; }
  .zone-banner__content { padding: 0 24px; max-width: 280px; }
  .zone-banner__title { font-size: 20px; }
  .zone-banner__desc { font-size: 12px; }
}
/* ---- Footer ---- */
.footer {
  padding: 64px 16px 32px;
  border-top: 1px solid var(--wz-border);
}

.footer__grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 40px;
  padding-bottom: 40px;
  border-bottom: 1px solid var(--wz-border);
}

@media (min-width: 768px) {
  .footer__grid { grid-template-columns: 2fr 1fr 1fr 1fr; }
}

.footer__logo {
  font-family: 'Noto Serif SC', serif;
  font-size: 22px;
  font-weight: 700;
  color: var(--wz-orange);
}

.footer__brand h3 {
  font-size: 20px;
  font-weight: 700;
  color: var(--wz-text);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}

.footer__desc {
  font-size: 14px;
  color: var(--wz-text-muted);
  line-height: 1.6;
  margin-bottom: 16px;
  max-width: 280px;
}

.footer__social {
  display: flex;
  gap: 10px;
}

.footer__social a {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--wz-bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--wz-text-soft);
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.footer__social a:hover {
  background: var(--wz-orange);
  color: #fff;
}

.footer__col h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 16px;
}

.footer__col ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer__col a {
  font-size: 14px;
  color: var(--wz-text-muted);
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}

.footer__col a:hover {
  color: var(--wz-orange);
}

.footer__col li {
  font-size: 14px;
  color: var(--wz-text-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}

.footer__bottom {
  text-align: center;
  padding-top: 24px;
  font-size: 13px;
  color: var(--wz-text-muted);
}
</style>
