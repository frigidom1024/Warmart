<template>
  <div class="header-nav">
    <div class="header-inner">
      <!-- Logo -->
      <div class="logo" @click="router.push('/')">
        网上商城
      </div>

      <!-- Navigation Menu -->
      <el-menu
        :default-active="activeIndex"
        mode="horizontal"
        :ellipsis="false"
        @select="handleSelect"
        class="nav-menu"
      >
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/product/list">商品</el-menu-item>
        <el-menu-item index="/cart">
          <el-badge :value="cartCount" :max="99" :hidden="cartCount === 0">
            <span>购物车</span>
          </el-badge>
        </el-menu-item>
        <el-sub-menu index="categories">
          <template #title>
            <span>分类</span>
          </template>
          <el-menu-item
            v-for="cat in categories"
            :key="cat.id"
            :index="`cat-${cat.id}`"
          >
            {{ cat.name }}
          </el-menu-item>
        </el-sub-menu>
      </el-menu>

      <!-- Search -->
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索商品"
          size="default"
          class="search-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">
          搜索
        </el-button>
      </div>

      <!-- User Area -->
      <div class="user-area">
        <template v-if="store.token">
          <el-dropdown trigger="click">
            <span class="user-dropdown-trigger">
              <el-avatar :size="32" :src="userAvatar">
                {{ userAvatarFallback }}
              </el-avatar>
              <span class="username">{{ displayName }}</span>
              <el-icon class="dropdown-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/user/info')">
                  <el-icon><User /></el-icon>个人信息
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/order/list')">
                  <el-icon><List /></el-icon>我的订单
                </el-dropdown-item>
                <el-dropdown-item @click="router.push('/user/favorites')">
                  <el-icon><Star /></el-icon>我的收藏
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button class="login-btn" @click="router.push('/auth/login')">登录</el-button>
          <el-button type="primary" @click="router.push('/auth/register')">注册</el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCategories } from '@/api/product'
import { getUserInfo } from '@/api/auth'
import { Search } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const searchKeyword = ref('')
const categories = ref([])
const cartCount = ref(0)

// Compute the active menu index based on current route
const activeIndex = computed(() => {
  const path = route.path
  if (path.startsWith('/product')) return '/product/list'
  if (path.startsWith('/cart')) return '/cart'
  return path
})

// User display helpers
const displayName = computed(() => store.userInfo?.nickname || store.userInfo?.username || '用户')
const userAvatar = computed(() => store.userInfo?.avatar || '')
const userAvatarFallback = computed(() => displayName.value.charAt(0).toUpperCase())

// Fetch categories for dropdown
onMounted(async () => {
  try {
    const res = await getCategories()
    const list = res.data || res
    // Filter top-level categories (parent_id === 0)
    if (Array.isArray(list)) {
      categories.value = list.filter(c => {
        const pid = c.parentId ?? c.parent_id ?? c.pid
        return pid === 0 || pid === null || pid === undefined
      })
    }
  } catch (e) {
    console.error('Failed to load categories:', e)
  }

  // Fetch user profile if logged in but info not yet loaded
  if (store.token && !store.userInfo) {
    try {
      const res = await getUserInfo()
      store.userInfo = res.data || res
    } catch (e) {
      console.error('Failed to load user info:', e)
    }
  }
})

const handleSelect = (index) => {
  if (index.startsWith('/')) {
    router.push(index)
  } else if (index.startsWith('cat-')) {
    const id = index.replace('cat-', '')
    router.push({ path: '/product/list', query: { categoryId: id } })
  }
}

const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    router.push({ path: '/product/list', query: { keyword } })
  }
}

const handleLogout = () => {
  store.logout()
  router.push('/')
}
</script>

<style scoped>
.header-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-inner {
  display: flex;
  align-items: center;
  max-width: 1200px;
  height: 60px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
  margin-right: 10px;
  user-select: none;
}

.logo:hover {
  opacity: 0.8;
}

.nav-menu {
  flex-shrink: 0;
  border-bottom: none !important;
}

.nav-menu .el-menu-item,
.nav-menu .el-sub-menu {
  height: 60px;
  line-height: 60px;
}

.search-box {
  display: flex;
  align-items: center;
  flex: 1;
  max-width: 360px;
  margin: 0 20px;
}

.search-input {
  flex: 1;
}

.search-input :deep(.el-input__wrapper) {
  border-radius: 4px 0 0 4px;
}

.search-box .el-button {
  border-radius: 0 4px 4px 0;
  margin-left: 0;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  flex-shrink: 0;
}

.user-dropdown-trigger {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.user-dropdown-trigger:hover {
  background-color: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #333;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-arrow {
  font-size: 12px;
  color: #999;
}

.login-btn {
  margin-right: 0;
}

/* Override ElMenu hover styles for horizontal mode */
.nav-menu :deep(.el-menu-item:hover),
.nav-menu :deep(.el-sub-menu__title:hover) {
  background-color: transparent !important;
  color: #409eff !important;
}

.nav-menu :deep(.el-menu-item.is-active) {
  color: #409eff !important;
  border-bottom-color: #409eff !important;
}
</style>
