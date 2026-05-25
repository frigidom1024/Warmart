<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCategoryList } from '@/api/product'
import type { Category } from '@/api/product'

const router = useRouter()
const userStore = useUserStore()

const scrolled = ref(false)
const hidden = ref(false)
let lastScrollY = 0
const searchKeyword = ref('')
const cartCount = ref(0)
const showSuggestions = ref(false)

const allCategories = ref<Category[]>([])
const firstLevelCategories = computed(() => allCategories.value.filter(c => !c.parentId))
const selectedCategoryId = ref<number | ''>('')

const hotSearches = ref<string[]>(['连衣裙', '蓝牙耳机', '防晒霜', '运动鞋', '破壁机', '面膜', '台灯', '投影仪'])

function handleScroll() {
  const sy = window.scrollY
  scrolled.value = sy > 40

  if (sy > 600) {
    hidden.value = sy > lastScrollY
  } else {
    hidden.value = false
  }
  lastScrollY = sy
}

function doSearch(keyword?: string) {
  const q = (keyword || searchKeyword.value).trim()
  if (q) {
    searchKeyword.value = q
    showSuggestions.value = false
    router.push({
      path: '/product/list',
      query: {
        keyword: q,
        ...(selectedCategoryId.value ? { categoryId: selectedCategoryId.value } : {})
      }
    })
  }
}

function onSearchFocus() {
  showSuggestions.value = true
}

function onSearchBlur() {
  setTimeout(() => { showSuggestions.value = false }, 200)
}

function selectSuggestion(keyword: string) {
  doSearch(keyword)
}

function goHome() { router.push('/') }
function goCart() { router.push('/cart') }
function goLogin() { router.push('/auth') }
function goRegister() { router.push('/auth?mode=register') }
function goRoute(name: string) { router.push({ name }) }

function handleLogout() {
  userStore.logout()
  router.push('/')
}

onMounted(async () => {
  window.addEventListener('scroll', handleScroll)
  try { allCategories.value = await getCategoryList() } catch {}
})
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<template>
  <header :class="['header-nav', { 'header-nav--scrolled': scrolled, 'header-nav--hidden': hidden }]">
    <div class="header-nav__inner">
      <!-- Logo -->
      <div class="header-nav__brand" @click="goHome">
        <span class="header-nav__logo">暖</span>
        <span class="header-nav__name">Warmart</span>
      </div>

      <!-- Center: Search bar -->
      <div class="header-nav__search-wrapper">
        <div class="header-nav__search">
          <el-dropdown trigger="click" @command="(v: string) => selectedCategoryId.value = v ? Number(v) : ''">
            <button class="header-nav__search-cat">
              <span>{{ selectedCategoryId.value ? (firstLevelCategories.find(c => c.id === selectedCategoryId.value)?.name || '分类') : '全部分类' }}</span>
              <svg width="8" height="6" viewBox="0 0 8 6" fill="none" class="header-nav__search-arrow">
                <path d="M1 1.5L4 4.5L7 1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="''" :class="{ 'is-active': selectedCategoryId.value === '' }">全部分类</el-dropdown-item>
                <el-dropdown-item
                  v-for="cat in firstLevelCategories"
                  :key="cat.id"
                  :command="String(cat.id)"
                  :class="{ 'is-active': cat.id === selectedCategoryId.value }"
                >
                  {{ cat.name }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <span class="header-nav__search-divider" />

          <input
            v-model="searchKeyword"
            type="text"
            class="header-nav__search-input"
            placeholder="搜搜你喜欢的商品…"
            @keyup.enter="doSearch()"
            @focus="onSearchFocus"
            @blur="onSearchBlur"
          />

          <button v-if="searchKeyword" class="header-nav__search-clear" @click="searchKeyword = ''">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>

          <button class="header-nav__search-btn" @click="doSearch()">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
            </svg>
          </button>
        </div>

        <!-- Suggestions dropdown -->
        <div v-if="showSuggestions" class="header-nav__suggestions">
          <div class="header-nav__suggestions-header">热门搜索</div>
          <div class="header-nav__suggestions-tags">
            <span
              v-for="word in hotSearches"
              :key="word"
              class="header-nav__suggestion-tag"
              @mousedown.prevent="selectSuggestion(word)"
            >{{ word }}</span>
          </div>
        </div>
      </div>

      <!-- Actions -->
      <div class="header-nav__actions">
        <!-- Cart -->
        <button class="header-nav__icon-btn header-nav__cart-btn" @click="goCart">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
          </svg>
          <span v-if="cartCount > 0" class="header-nav__cart-badge">{{ cartCount }}</span>
        </button>

        <span class="header-nav__divider" />

        <!-- User -->
        <template v-if="userStore.isLoggedIn">
          <el-dropdown trigger="click">
            <span class="header-nav__user">
              <span class="header-nav__avatar">{{ (userStore.userInfo?.nickname || userStore.userInfo?.username || '用')[0] }}</span>
              <span class="header-nav__username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
              <svg width="8" height="6" viewBox="0 0 8 6" fill="none" class="header-nav__link-arrow">
                <path d="M1 1.5L4 4.5L7 1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="goRoute('UserInfo')">个人信息</el-dropdown-item>
                <el-dropdown-item @click="goRoute('OrderList')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="goRoute('UserFavorites')">我的收藏</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>

        <template v-else>
          <button class="header-nav__text-link" @click="goLogin">登录</button>
          <button class="header-nav__cta" @click="goRegister">注册</button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;700&display=swap');

.header-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  background: var(--wz-bg);
  border-bottom: 1px solid var(--wz-border);
  transition: border-color var(--wz-duration-normal) var(--wz-ease-out),
              transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.header-nav--scrolled {
  border-color: rgba(255, 255, 255, 0.08);
}

.header-nav--hidden {
  transform: translateY(-100%);
}

.header-nav__inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 var(--wz-space-xl);
  height: 60px;
  display: flex;
  align-items: center;
  gap: var(--wz-space-xl);
}

/* ---- Brand ---- */
.header-nav__brand {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.header-nav__logo {
  font-family: 'Noto Serif SC', serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--wz-orange);
  line-height: 1;
}

.header-nav__name {
  font-family: 'Noto Serif SC', serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--wz-text);
  letter-spacing: 0.05em;
}

/* ---- Search Wrapper (for dropdown positioning) ---- */
.header-nav__search-wrapper {
  flex: 1;
  max-width: 560px;
  position: relative;
}

/* ---- Center Search Bar ---- */
.header-nav__search {
  display: flex;
  align-items: center;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: var(--wz-radius-full);
  padding: 0 4px 0 0;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__search:focus-within {
  border-color: var(--wz-orange);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.12);
}

/* Category dropdown trigger */
.header-nav__search-cat {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 16px 0 18px;
  height: 36px;
  background: none;
  border: none;
  font-family: var(--wz-font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-soft);
  cursor: pointer;
  white-space: nowrap;
  border-radius: var(--wz-radius-full) 0 0 var(--wz-radius-full);
  transition: color var(--wz-duration-fast) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__search-cat:hover {
  color: var(--wz-text);
  background: var(--wz-bg-hover);
}

.header-nav__search-arrow {
  transition: transform var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__search-cat:hover .header-nav__search-arrow {
  transform: rotate(-180deg);
}

.header-nav__search-divider {
  width: 1px;
  height: 18px;
  background: var(--wz-border);
  flex-shrink: 0;
}

.header-nav__search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: none;
  font-family: var(--wz-font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  padding: 9px 10px;
  letter-spacing: 0.02em;
}

.header-nav__search-input::placeholder {
  color: var(--wz-text-muted);
}

.header-nav__search-clear {
  flex-shrink: 0;
  background: none;
  border: none;
  color: var(--wz-text-muted);
  cursor: pointer;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__search-clear:hover {
  color: var(--wz-text);
}

.header-nav__search-btn {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-orange);
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__search-btn:hover {
  background: var(--wz-orange-dark);
  box-shadow: 0 0 12px rgba(255, 107, 53, 0.35);
}

/* ---- Search Suggestions Dropdown ---- */
.header-nav__suggestions {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 12px;
  padding: 16px;
  z-index: 200;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.25);
  animation: suggest-fade-in 0.15s var(--wz-ease-out);
}

@keyframes suggest-fade-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.header-nav__suggestions-header {
  font-size: 12px;
  font-weight: 600;
  color: var(--wz-text-soft);
  letter-spacing: 0.08em;
  margin-bottom: 12px;
}

.header-nav__suggestions-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.header-nav__suggestion-tag {
  display: inline-block;
  padding: 5px 14px;
  font-size: 13px;
  color: var(--wz-text);
  background: var(--wz-bg);
  border-radius: 999px;
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__suggestion-tag:hover {
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
}

/* ---- Actions ---- */
.header-nav__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: auto;
}

.header-nav__icon-btn {
  position: relative;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: var(--wz-radius-sm);
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: color var(--wz-duration-fast) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__icon-btn:hover {
  color: var(--wz-orange);
  background: var(--wz-orange-muted);
}

.header-nav__cart-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--wz-orange);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  line-height: 16px;
  text-align: center;
  border-radius: 8px;
}

.header-nav__divider {
  width: 1px;
  height: 20px;
  background: var(--wz-border);
  margin: 0 6px;
}

/* User */
.header-nav__user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--wz-radius-sm);
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__user:hover {
  background: var(--wz-orange-muted);
}

.header-nav__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.header-nav__username {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-soft);
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Text link */
.header-nav__text-link {
  background: none;
  border: none;
  font-family: var(--wz-font-body);
  font-size: 13.5px;
  font-weight: 500;
  color: var(--wz-text-soft);
  padding: 6px 12px;
  cursor: pointer;
  border-radius: var(--wz-radius-sm);
  transition: color var(--wz-duration-fast) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__text-link:hover {
  color: var(--wz-orange);
  background: var(--wz-orange-muted);
}

/* CTA */
.header-nav__cta {
  font-family: var(--wz-font-body);
  font-size: 13.5px;
  font-weight: 600;
  color: #fff;
  background: var(--wz-orange);
  border: none;
  padding: 6px 18px;
  border-radius: var(--wz-radius-full);
  cursor: pointer;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.header-nav__cta:hover {
  background: var(--wz-orange-dark);
  transform: translateY(-1px);
  box-shadow: var(--wz-shadow-glow);
}

/* Dropdown active state */
.header-nav__search :deep(.is-active) {
  color: var(--wz-orange) !important;
  font-weight: 500;
}

/* ---- Responsive ---- */
@media (max-width: 900px) {
  .header-nav__search {
    max-width: 300px;
  }
}

@media (max-width: 768px) {
  .header-nav__inner {
    padding: 0 var(--wz-space-md);
    gap: var(--wz-space-sm);
  }

  .header-nav__name {
    display: none;
  }

  .header-nav__search-wrapper {
    max-width: none;
  }

  .header-nav__search-cat {
    display: none;
  }

  .header-nav__search-divider {
    display: none;
  }

  .header-nav__username {
    display: none;
  }

  .header-nav__cta {
    padding: 6px 14px;
    font-size: 12px;
  }
}
</style>
