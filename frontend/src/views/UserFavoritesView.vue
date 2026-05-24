<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getFavoriteList, addFavorite, cancelFavorite } from '@/api/favorite'
import { addToCart } from '@/api/cart'
import type { FavoriteItem } from '@/api/favorite'
import { showToast } from '@/utils/toast'

const router = useRouter()
const favorites = ref<FavoriteItem[]>([])
const total = ref(0)
const loading = ref(true)
const favoritedIds = reactive(new Set<number>())

onMounted(async () => {
  loading.value = true
  try {
    const res = await getFavoriteList({ size: 50 })
    favorites.value = (res as any).records || (res as FavoriteItem[]) || []
    total.value = (res as any).total || favorites.value.length
    favorites.value.forEach(f => favoritedIds.add(f.productId))
  } catch { /* handled */ } finally {
    loading.value = false
  }
})

async function handleToggle(productId: number) {
  if (favoritedIds.has(productId)) {
    try {
      await cancelFavorite(productId)
      favoritedIds.delete(productId)
    } catch { /* handled */ }
  } else {
    try {
      await addFavorite(productId)
      favoritedIds.add(productId)
      showToast('已收藏', 'success')
    } catch { /* handled */ }
  }
}

async function handleAddToCart(productId: number) {
  try {
    await addToCart({ productId, quantity: 1 })
    showToast('已加入购物车', 'success')
  } catch { /* handled */ }
}

function goProduct(id: number) {
  router.push('/product/detail/' + id)
}
</script>

<template>
  <div class="page-container">
    <div class="user-favorites">
      <div class="user-favorites__header">
        <h1 class="user-favorites__title">我的收藏</h1>
        <span class="user-favorites__count">共 {{ total }} 件</span>
      </div>

      <!-- Grid -->
      <div v-if="favorites.length" class="user-favorites__grid">
        <div v-for="item in favorites" :key="item.id" class="user-favorites__card">
          <div class="user-favorites__card-image" @click="goProduct(item.productId)">
            <img
              v-if="item.productImage"
              :src="item.productImage"
              :alt="item.productName || ''"
            >
            <button
              class="user-favorites__heart"
              :class="{ 'user-favorites__heart--active': favoritedIds.has(item.productId) }"
              @click.stop="handleToggle(item.productId)"
              :title="favoritedIds.has(item.productId) ? '取消收藏' : '收藏'"
            >
              <svg class="user-favorites__heart-icon" viewBox="0 0 24 24" width="14" height="14">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" fill="none" stroke="currentColor" stroke-width="2"/>
              </svg>
            </button>
          </div>
          <div class="user-favorites__card-info" @click="goProduct(item.productId)">
            <p class="user-favorites__card-name">{{ item.productName }}</p>
            <p class="user-favorites__card-price">
              ¥{{ item.productPrice }}
              <span v-if="item.productOldPrice" class="user-favorites__card-old">¥{{ item.productOldPrice }}</span>
            </p>
          </div>
          <div class="user-favorites__card-action">
            <span class="user-favorites__cart-add" @click="handleAddToCart(item.productId)">加入购物车</span>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="!loading" class="user-favorites__empty">
        <p class="user-favorites__empty-text">收藏夹是空的</p>
        <p class="user-favorites__empty-hint">去逛逛，发现心仪好物</p>
        <div class="user-favorites__go-btn" @click="router.push('/product/list')">去逛逛</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.user-favorites {
  max-width: 1000px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.user-favorites__header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 24px;
}
.user-favorites__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
}
.user-favorites__count {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.user-favorites__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}
.user-favorites__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s, box-shadow 0.2s;
}
.user-favorites__card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}
.user-favorites__card-image {
  height: 160px;
  background: var(--wz-bg);
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 12px;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}
.user-favorites__card-image img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  pointer-events: none;
}
.user-favorites__heart {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 3;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  border: none;
  color: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--wz-duration-fast) var(--wz-ease-out), color var(--wz-duration-fast) var(--wz-ease-out), transform var(--wz-duration-fast) var(--wz-ease-out);
  backdrop-filter: blur(4px);
}
.user-favorites__heart:hover {
  background: rgba(0, 0, 0, 0.6);
  color: rgba(255, 255, 255, 0.8);
  transform: scale(1.1);
}
.user-favorites__heart--active {
  background: rgba(255, 107, 53, 0.85);
  color: #fff;
}
.user-favorites__heart--active:hover {
  background: var(--wz-orange-dark);
  transform: scale(1.1);
}
.user-favorites__heart-icon {
  position: relative;
  z-index: 1;
}
.user-favorites__card-info {
  padding: 14px;
}
.user-favorites__card-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.user-favorites__card-price {
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-orange);
}
.user-favorites__card-old {
  font-size: 12px;
  font-weight: 400;
  color: var(--wz-text-muted);
  text-decoration: line-through;
  margin-left: 6px;
}
.user-favorites__card-action {
  padding: 0 14px 14px;
}
.user-favorites__cart-add {
  display: block;
  text-align: center;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid var(--wz-orange);
  color: var(--wz-orange);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.user-favorites__cart-add:hover {
  background: var(--wz-orange);
  color: #fff;
}
.user-favorites__empty {
  text-align: center;
  padding: 80px 24px;
}
.user-favorites__empty-text {
  font-size: 16px;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.user-favorites__empty-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
  margin-bottom: 20px;
}
.user-favorites__go-btn {
  display: inline-flex;
  padding: 10px 32px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 999px;
  font-size: 14px;
  cursor: pointer;
}
</style>
