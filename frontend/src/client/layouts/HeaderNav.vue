<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const scrolled = ref(false)
const searchKeyword = ref('')
const cartCount = ref(0)

const categories = [
  { label: '全部', value: '' },
  { label: '潮流服饰', value: 'clothing' },
  { label: '美妆护肤', value: 'beauty' },
  { label: '数码家电', value: 'digital' },
  { label: '家居软装', value: 'home' },
  { label: '休闲零食', value: 'food' },
  { label: '运动户外', value: 'sports' }
]
const selectedCategory = ref(categories[0])

function handleScroll() {
  scrolled.value = window.scrollY > 40
}

function doSearch() {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/product/list',
      query: {
        keyword: searchKeyword.value.trim(),
        ...(selectedCategory.value.value ? { category: selectedCategory.value.value } : {})
      }
    })
  }
}

function goHome() { router.push('/') }
function goCart() { router.push('/cart') }
function goLogin() { router.push('/auth/login') }
function goRegister() { router.push('/auth/register') }
function goRoute(name: string) { router.push({ name }) }

function handleLogout() {
  userStore.logout()
  router.push('/')
}

onMounted(() => window.addEventListener('scroll', handleScroll))
onUnmounted(() => window.removeEventListener('scroll', handleScroll))
</script>

<template>
  <header :class="['header-nav', { 'header-nav--scrolled': scrolled }]">
    <div class="header-nav__inner">
      <!-- Logo -->
      <div class="header-nav__brand" @click="goHome">
        <span class="header-nav__logo">暖</span>
        <span class="header-nav__name">Warmart</span>
      </div>

      <!-- Center: Search bar -->
      <div class="header-nav__search">
        <el-dropdown trigger="click" @command="(v: string) => selectedCategory = categories.find(c => c.value === v) || categories[0]">
          <button class="header-nav__search-cat">
            <span>{{ selectedCategory.label }}</span>
            <svg width="8" height="6" viewBox="0 0 8 6" fill="none" class="header-nav__search-arrow">
              <path d="M1 1.5L4 4.5L7 1.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="cat in categories"
                :key="cat.value"
                :command="cat.value"
                :class="{ 'is-active': cat.value === selectedCategory.value }"
              >
                {{ cat.label }}
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
          @keyup.enter="doSearch"
        />

        <button v-if="searchKeyword" class="header-nav__search-clear" @click="searchKeyword = ''">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
        </button>

        <button class="header-nav__search-btn" @click="doSearch">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
        </button>
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
  transition: border-color var(--wz-duration-normal) var(--wz-ease-out);
}

.header-nav--scrolled {
  border-color: rgba(255, 255, 255, 0.08);
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

/* ---- Center Search Bar ---- */
.header-nav__search {
  flex: 1;
  max-width: 560px;
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

  .header-nav__search {
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
