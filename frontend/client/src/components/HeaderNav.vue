<template>
  <header :class="['wz-header', { 'wz-header--scrolled': isScrolled }]">
    <div class="wz-header__inner">
      <!-- Logo -->
      <router-link to="/" class="wz-header__logo">
        <span class="wz-header__logo-mark">暖</span>
        <span class="wz-header__logo-text">Warmart</span>
      </router-link>

      <!-- Navigation -->
      <nav class="wz-header__nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="wz-header__nav-link"
          :class="{ 'is-active': isActive(item.path) }"
        >
          {{ item.label }}
        </router-link>
        <div class="wz-header__nav-divider" />
        <div class="wz-header__nav-categories" @mouseenter="showCategories = true" @mouseleave="showCategories = false">
          <span class="wz-header__nav-link wz-header__nav-link--dropdown">
            分类
            <span class="arrow">▼</span>
          </span>
          <transition name="fade-drop">
            <div v-if="showCategories && categories.length > 0" class="wz-header__dropdown">
              <button
                v-for="cat in categories"
                :key="cat.id"
                class="wz-header__dropdown-item"
                @click="goCategory(cat.id)"
              >
                {{ cat.name }}
              </button>
            </div>
          </transition>
        </div>
      </nav>

      <!-- Search -->
      <div class="wz-header__search">
        <transition name="search-expand">
          <div v-if="searchOpen" class="wz-header__search-box">
            <input
              ref="searchInput"
              v-model="searchKeyword"
              type="text"
              placeholder="搜索商品…"
              class="wz-header__search-input"
              @keyup.enter="handleSearch"
              @blur="handleSearchBlur"
            />
            <button class="wz-header__search-submit" @click="handleSearch">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
              </svg>
            </button>
          </div>
        </transition>
        <button v-if="!searchOpen" class="wz-header__icon-btn" @click="openSearch" aria-label="搜索">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
        </button>
      </div>

      <!-- Cart -->
      <router-link to="/cart" class="wz-header__icon-btn wz-header__cart" aria-label="购物车">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
          <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/>
          <line x1="3" y1="6" x2="21" y2="6"/>
          <path d="M16 10a4 4 0 0 1-8 0"/>
        </svg>
        <span v-if="cartCount > 0" class="wz-header__cart-badge">{{ cartCount > 99 ? '99+' : cartCount }}</span>
      </router-link>

      <!-- User -->
      <div v-if="store.token" class="wz-header__user">
        <div class="wz-header__user-trigger" @click="userMenuOpen = !userMenuOpen" v-click-outside="() => userMenuOpen = false">
          <div class="wz-header__avatar">
            {{ displayName.charAt(0).toUpperCase() }}
          </div>
          <span class="wz-header__username">{{ displayName }}</span>
        </div>
        <transition name="fade-drop">
          <div v-if="userMenuOpen" class="wz-header__dropdown wz-header__dropdown--right">
            <router-link to="/user/info" class="wz-header__dropdown-item" @click="userMenuOpen = false">个人信息</router-link>
            <router-link to="/order/list" class="wz-header__dropdown-item" @click="userMenuOpen = false">我的订单</router-link>
            <router-link to="/user/favorites" class="wz-header__dropdown-item" @click="userMenuOpen = false">我的收藏</router-link>
            <div class="wz-header__dropdown-divider" />
            <button class="wz-header__dropdown-item wz-header__dropdown-item--danger" @click="handleLogout">退出登录</button>
          </div>
        </transition>
      </div>
      <div v-else class="wz-header__auth">
        <router-link to="/auth/login" class="wz-header__auth-link">登录</router-link>
        <router-link to="/auth/register" class="wz-header__auth-btn">注册</router-link>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCategories } from '@/api/product'
import { getUserInfo } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const isScrolled = ref(false)
const searchOpen = ref(false)
const searchKeyword = ref('')
const categories = ref([])
const showCategories = ref(false)
const userMenuOpen = ref(false)
const cartCount = ref(0)
const searchInput = ref(null)

const navItems = [
  { path: '/', label: '首页' },
  { path: '/product/list', label: '商品' },
]

const displayName = computed(() => store.userInfo?.nickname || store.userInfo?.username || '用户')

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

function openSearch() {
  searchOpen.value = true
  nextTick(() => searchInput.value?.focus())
}

function handleSearchBlur() {
  if (!searchKeyword.value.trim()) {
    setTimeout(() => { searchOpen.value = false }, 200)
  }
}

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (kw) {
    router.push({ path: '/product/list', query: { keyword: kw } })
    searchOpen.value = false
    searchKeyword.value = ''
  }
}

function goCategory(id) {
  router.push({ path: '/product/list', query: { categoryId: id } })
  showCategories.value = false
}

function handleLogout() {
  store.logout()
  userMenuOpen.value = false
  router.push('/')
}

let scrollHandler = null
onMounted(async () => {
  scrollHandler = () => { isScrolled.value = window.scrollY > 40 }
  window.addEventListener('scroll', scrollHandler, { passive: true })

  try {
    const res = await getCategories()
    const list = res.data || res
    if (Array.isArray(list)) {
      categories.value = list.filter(c => {
        const pid = c.parentId ?? c.parent_id ?? c.pid
        return pid === 0 || pid === null || pid === undefined
      })
    }
  } catch (_) {}

  if (store.token && !store.userInfo) {
    try {
      const res = await getUserInfo()
      store.userInfo = res.data || res
    } catch (_) {}
  }
})

onBeforeUnmount(() => {
  if (scrollHandler) window.removeEventListener('scroll', scrollHandler)
})

// Click outside directive
const vClickOutside = {
  mounted(el, binding) {
    el.__clickOutside = (e) => {
      if (!el.contains(e.target)) binding.value()
    }
    document.addEventListener('click', el.__clickOutside)
  },
  unmounted(el) {
    document.removeEventListener('click', el.__clickOutside)
  }
}
</script>

<style scoped>
/* === Header === */
.wz-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  padding: 0 24px;
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  background: transparent;
  height: 72px;
}
.wz-header--scrolled {
  background: rgba(247, 243, 238, 0.92);
  backdrop-filter: blur(20px) saturate(1.2);
  -webkit-backdrop-filter: blur(20px) saturate(1.2);
  height: 64px;
  box-shadow: 0 1px 0 var(--wz-border-light);
}

.wz-header__inner {
  max-width: var(--wz-container-wide);
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 100%;
  gap: 32px;
}

/* Logo */
.wz-header__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}
.wz-header__logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: var(--wz-accent);
  color: white;
  border-radius: 10px;
  font-size: 18px;
  font-weight: 700;
  font-family: var(--wz-font-display);
}
.wz-header__logo-text {
  font-size: 20px;
  font-weight: 600;
  color: var(--wz-text);
  letter-spacing: -0.3px;
}

/* Navigation */
.wz-header__nav {
  display: flex;
  align-items: center;
  gap: 4px;
}
.wz-header__nav-link {
  position: relative;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--wz-text-secondary);
  text-decoration: none;
  border-radius: var(--wz-radius-sm);
  transition: all 0.25s ease;
  cursor: pointer;
  background: none;
  border: none;
  font-family: inherit;
}
.wz-header__nav-link:hover,
.wz-header__nav-link--dropdown:hover {
  color: var(--wz-accent);
  background: var(--wz-accent-light);
}
.wz-header__nav-link.is-active {
  color: var(--wz-accent);
  font-weight: 600;
}
.wz-header__nav-link.is-active::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 16px;
  height: 2px;
  background: var(--wz-accent);
  border-radius: 1px;
}
.wz-header__nav-link .arrow {
  font-size: 8px;
  margin-left: 4px;
  display: inline-block;
}
.wz-header__nav-divider {
  width: 1px;
  height: 20px;
  background: var(--wz-border);
  margin: 0 8px;
}
.wz-header__nav-categories {
  position: relative;
}

/* Dropdown */
.wz-header__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 160px;
  background: var(--wz-bg-card);
  border-radius: var(--wz-radius);
  box-shadow: var(--wz-shadow-md);
  padding: 8px;
  z-index: 100;
}
.wz-header__dropdown--right {
  left: auto;
  right: 0;
}
.wz-header__dropdown-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  font-size: 14px;
  color: var(--wz-text);
  text-decoration: none;
  border-radius: 6px;
  transition: all 0.2s ease;
  cursor: pointer;
  background: none;
  border: none;
  font-family: inherit;
  text-align: left;
}
.wz-header__dropdown-item:hover {
  background: var(--wz-accent-light);
  color: var(--wz-accent);
}
.wz-header__dropdown-item--danger:hover {
  background: rgba(192, 102, 74, 0.08);
  color: var(--wz-accent);
}
.wz-header__dropdown-divider {
  height: 1px;
  background: var(--wz-border-light);
  margin: 4px 0;
}

/* Search */
.wz-header__search {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  max-width: 320px;
  margin-left: auto;
}
.wz-header__search-box {
  display: flex;
  align-items: center;
  width: 100%;
  background: var(--wz-bg-alt);
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid var(--wz-border-light);
  transition: all 0.3s ease;
}
.wz-header__search-box:focus-within {
  border-color: var(--wz-accent);
  background: var(--wz-bg-card);
  box-shadow: 0 0 0 3px var(--wz-accent-light);
}
.wz-header__search-input {
  flex: 1;
  padding: 8px 16px;
  border: none;
  background: transparent;
  font-size: 14px;
  font-family: inherit;
  color: var(--wz-text);
  outline: none;
}
.wz-header__search-input::placeholder {
  color: var(--wz-text-muted);
}
.wz-header__search-submit {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  color: var(--wz-text-secondary);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s;
}
.wz-header__search-submit:hover {
  color: var(--wz-accent);
}

/* Icon buttons */
.wz-header__icon-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: var(--wz-text-secondary);
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.25s ease;
  text-decoration: none;
}
.wz-header__icon-btn:hover {
  background: var(--wz-accent-light);
  color: var(--wz-accent);
}

/* Cart */
.wz-header__cart-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: var(--wz-accent);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

/* User */
.wz-header__user {
  position: relative;
}
.wz-header__user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.wz-header__user-trigger:hover {
  background: var(--wz-accent-light);
}
.wz-header__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--wz-accent);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}
.wz-header__username {
  font-size: 14px;
  color: var(--wz-text);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Auth buttons */
.wz-header__auth {
  display: flex;
  align-items: center;
  gap: 8px;
}
.wz-header__auth-link {
  padding: 8px 16px;
  font-size: 14px;
  color: var(--wz-text-secondary);
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.25s;
}
.wz-header__auth-link:hover {
  color: var(--wz-accent);
  background: var(--wz-accent-light);
}
.wz-header__auth-btn {
  padding: 8px 20px;
  font-size: 14px;
  color: white;
  background: var(--wz-accent);
  text-decoration: none;
  border-radius: 20px;
  font-weight: 500;
  transition: all 0.25s;
}
.wz-header__auth-btn:hover {
  background: var(--wz-accent-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(192, 102, 74, 0.3);
}

/* Transitions */
.fade-drop-enter-active, .fade-drop-leave-active {
  transition: all 0.2s ease;
}
.fade-drop-enter-from, .fade-drop-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.search-expand-enter-active, .search-expand-leave-active {
  transition: all 0.3s ease;
}
.search-expand-enter-from, .search-expand-leave-to {
  opacity: 0;
  transform: scaleX(0.8);
  transform-origin: right;
}

/* Responsive */
@media (max-width: 768px) {
  .wz-header__nav { display: none; }
  .wz-header__search { max-width: 200px; }
  .wz-header__username { display: none; }
}
</style>
