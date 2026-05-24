<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail } from '@/api/product'
import { addToCart } from '@/api/cart'
import type { Product, ProductSpec } from '@/api/product'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const product = ref<Product | null>(null)
const loading = ref(true)
const activeTab = ref('detail')

// Spec selection state
const specGroups = ref<{ name: string; values: ProductSpec[] }[]>([])
const selectedSpecs = ref<Record<string, string>>({})
const quantity = ref(1)

// Computed price: basePrice + sum of extraPrice of selected specs
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

// Computed stock: min stock of selected specs
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

// All specs selected?
const allSpecsSelected = computed(() => {
  return specGroups.value.every(g => selectedSpecs.value[g.name])
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
  } catch {
    // handled by interceptor
  } finally {
    loading.value = false
  }
})

// Build spec groups from product specList
function buildSpecGroups() {
  if (!product.value?.specList) return
  const groups: Record<string, ProductSpec[]> = {}
  for (const spec of product.value.specList) {
    if (!groups[spec.specName]) groups[spec.specName] = []
    groups[spec.specName].push(spec)
  }
  specGroups.value = Object.entries(groups).map(([name, values]) => ({ name, values }))
  // Auto-select first value of each group
  for (const group of specGroups.value) {
    selectedSpecs.value[group.name] = group.values[0].specValue
  }
  quantity.value = 1
}

function selectSpec(groupName: string, specValue: string) {
  selectedSpecs.value[groupName] = specValue
  quantity.value = 1
}

// Build specInfo string for cart/order
function getSpecInfo(): string {
  return specGroups.value.map(g => `${g.name}: ${selectedSpecs.value[g.name] || ''}`).join('; ')
}

async function handleAddToCart() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await addToCart({
      productId: product.value.id,
      quantity: quantity.value,
      specInfo: getSpecInfo()
    })
    showToast('已加入购物车', 'success')
  } catch {
    // handled by interceptor
  }
}

async function handleBuyNow() {
  if (!product.value || !allSpecsSelected.value) return
  try {
    await addToCart({
      productId: product.value.id,
      quantity: quantity.value,
      specInfo: getSpecInfo()
    })
    router.push('/order/create')
  } catch {
    // handled by interceptor
  }
}
</script>

<template>
  <div class="page-container">
    <div class="pdp" v-if="product">
      <!-- Breadcrumb -->
      <div class="pdp__breadcrumb">
        <span @click="router.push('/')">首页</span>
        <span class="pdp__breadcrumb-sep">/</span>
        <span @click="router.push('/product/list')">商品列表</span>
        <span class="pdp__breadcrumb-sep">/</span>
        <span>商品详情</span>
      </div>

      <!-- Gallery + Info -->
      <div class="pdp__main">
        <div class="pdp__gallery">
          <div class="pdp__gallery-main">
            <img :src="product.mainImage" :alt="product.name">
          </div>
          <div class="pdp__gallery-thumbs">
            <div
              v-for="(img, i) in product.imageList || [product.mainImage]"
              :key="i"
              class="pdp__thumb"
            >
              <img :src="img" :alt="product.name">
            </div>
          </div>
        </div>

        <div class="pdp__info">
          <h1 class="pdp__name">{{ product.name }}</h1>
          <div class="pdp__price">
            <span class="pdp__price-current">¥{{ displayPrice }}</span>
            <span v-if="product.originalPrice" class="pdp__price-original">¥{{ product.originalPrice }}</span>
            <span v-if="product.tag" class="pdp__tag">{{ product.tag }}</span>
          </div>
          <div class="pdp__meta">
            <span class="pdp__meta-item">月销 {{ product.sales > 999 ? (product.sales / 1000).toFixed(1) + 'k' : product.sales }}</span>
            <span class="pdp__meta-item">库存 {{ product.stock }}</span>
          </div>
          <div class="pdp__desc">{{ product.description }}</div>
          <!-- Spec Selection -->
          <div v-if="specGroups.length" class="pdp__specs">
            <div v-for="group in specGroups" :key="group.name" class="pdp__spec-row">
              <span class="pdp__spec-label">{{ group.name }}</span>
              <div class="pdp__spec-options">
                <span
                  v-for="spec in group.values"
                  :key="spec.id"
                  class="pdp__spec-option"
                  :class="{ 'pdp__spec-option--active': selectedSpecs[group.name] === spec.specValue }"
                  @click="selectSpec(group.name, spec.specValue)"
                >
                  {{ spec.specValue }}
                  <span v-if="spec.extraPrice > 0" class="pdp__spec-extra">+¥{{ spec.extraPrice }}</span>
                </span>
              </div>
            </div>
          </div>

          <!-- Quantity -->
          <div v-if="specGroups.length" class="pdp__qty-row">
            <span class="pdp__qty-label">数量</span>
            <div class="pdp__qty-control">
              <span class="pdp__qty-btn" :class="{ 'pdp__qty-btn--disabled': quantity <= 1 }" @click="quantity > 1 && quantity--">-</span>
              <span class="pdp__qty-value">{{ quantity }}</span>
              <span class="pdp__qty-btn" :class="{ 'pdp__qty-btn--disabled': quantity >= displayStock }" @click="quantity < displayStock && quantity++">+</span>
            </div>
            <span class="pdp__qty-hint">库存 {{ displayStock }} 件</span>
          </div>

          <div class="pdp__actions">
            <div
              class="pdp__cart-btn"
              :class="{ 'pdp__btn--disabled': !allSpecsSelected }"
              @click="handleAddToCart"
            >加入购物车</div>
            <div
              class="pdp__buy-btn"
              :class="{ 'pdp__btn--disabled': !allSpecsSelected }"
              @click="handleBuyNow"
            >立即购买</div>
          </div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="pdp__tabs">
        <div class="pdp__tabs-header">
          <span
            class="pdp__tab"
            :class="{ 'pdp__tab--active': activeTab === 'detail' }"
            @click="activeTab = 'detail'"
          >商品详情</span>
        </div>
        <div class="pdp__tabs-content">
          <div class="pdp__detail-text">{{ product.description }}</div>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-else-if="loading" class="pdp pdp--loading">
      <div class="pdp__breadcrumb">加载中...</div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.pdp {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}
.pdp__breadcrumb {
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 24px;
  cursor: pointer;
}
.pdp__breadcrumb span:hover {
  color: var(--wz-orange);
}
.pdp__breadcrumb-sep {
  margin: 0 8px;
  color: var(--wz-border);
  cursor: default;
}
.pdp__main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
}
.pdp__gallery-main {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: var(--wz-bg);
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 12px;
}
.pdp__gallery-main img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pdp__gallery-thumbs {
  display: flex;
  gap: 10px;
}
.pdp__thumb {
  width: 72px;
  height: 72px;
  background: var(--wz-bg);
  border-radius: 8px;
  border: 2px solid var(--wz-border);
  cursor: pointer;
  overflow: hidden;
}
.pdp__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pdp__info {
  padding-top: 8px;
}
.pdp__name {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 22px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 16px;
  line-height: 1.4;
}
.pdp__price {
  margin-bottom: 20px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.pdp__price-current {
  font-size: 32px;
  font-weight: 700;
  color: var(--wz-orange);
}
.pdp__price-original {
  font-size: 15px;
  color: var(--wz-text-soft);
  text-decoration: line-through;
}
.pdp__tag {
  display: inline-block;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: var(--wz-orange);
  padding: 3px 10px;
  border-radius: 999px;
  margin-left: 8px;
}
.pdp__meta {
  display: flex;
  gap: 24px;
  margin-bottom: 24px;
  padding: 12px 0;
  border-top: 1px solid var(--wz-border);
  border-bottom: 1px solid var(--wz-border);
}
.pdp__meta-item {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.pdp__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.7;
  margin-bottom: 20px;
  padding: 12px 0;
  border-top: 1px solid var(--wz-border);
  border-bottom: 1px solid var(--wz-border);
}
.pdp--loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: var(--wz-text-muted);
}
.pdp__detail-text {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.8;
}
.pdp__specs {
  margin-bottom: 28px;
}
.pdp__spec-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}
.pdp__spec-label {
  font-size: 13px;
  color: var(--wz-text-soft);
  min-width: 48px;
  line-height: 32px;
}
.pdp__spec-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.pdp__spec-option {
  font-size: 13px;
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid var(--wz-border);
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__spec-option:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.pdp__actions {
  display: flex;
  gap: 12px;
}
.pdp__cart-btn {
  flex: 1;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  border: 1px solid var(--wz-orange);
  color: var(--wz-orange);
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__cart-btn:hover {
  background: rgba(255, 107, 53, 0.06);
}
.pdp__buy-btn {
  flex: 1;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 24px;
  background: var(--wz-orange);
  color: #fff;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
}
.pdp__buy-btn:hover {
  background: var(--wz-orange-dark);
}
.pdp__tabs-header {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--wz-border);
  margin-bottom: 24px;
}
.pdp__tab {
  padding: 12px 24px;
  font-size: 15px;
  color: var(--wz-text-soft);
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}
.pdp__tab--active {
  color: var(--wz-orange);
  font-weight: 500;
}
.pdp__tab--active::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 100%;
  height: 2px;
  background: var(--wz-orange);
}
.pdp__tabs-content {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 48px;
}
.pdp__tabs-placeholder {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.pdp__spec-option--active {
  border-color: var(--wz-orange);
  background: rgba(255, 107, 53, 0.06);
  color: var(--wz-orange);
}
.pdp__spec-extra {
  font-size: 11px;
  color: var(--wz-orange);
  margin-left: 2px;
}
.pdp__qty-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  padding-top: 12px;
  border-top: 1px solid var(--wz-border);
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
  border-radius: 6px;
  overflow: hidden;
}
.pdp__qty-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s;
}
.pdp__qty-btn:hover { background: var(--wz-bg); }
.pdp__qty-btn--disabled { opacity: 0.3; cursor: not-allowed; }
.pdp__qty-value {
  width: 44px;
  text-align: center;
  font-size: 14px;
  border-left: 1px solid var(--wz-border);
  border-right: 1px solid var(--wz-border);
  line-height: 32px;
}
.pdp__qty-hint {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.pdp__btn--disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .pdp__main {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}
</style>
