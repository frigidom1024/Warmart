<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuItems = [
  { path: '/dashboard', icon: 'Odometer', label: '控制台' },
  { path: '/users', icon: 'User', label: '用户管理' },
  {
    label: '商品管理', icon: 'Goods',
    children: [
      { path: '/products', label: '商品列表' },
      { path: '/categories', label: '分类管理' }
    ]
  },
  {
    label: '订单管理', icon: 'List',
    children: [
      { path: '/orders', label: '订单列表' },
      { path: '/logistics', label: '物流管理' },
      { path: '/refunds', label: '退款管理' }
    ]
  },
  {
    label: '内容管理', icon: 'Document',
    children: [
      { path: '/banners', label: '轮播管理' },
      { path: '/comments', label: '评价管理' },
      { path: '/notices', label: '公告管理' }
    ]
  },
  {
    label: '系统', icon: 'Setting',
    children: [
      { path: '/feedbacks', label: '反馈管理' },
      { path: '/consultations', label: '咨询管理' },
      { path: '/profile', label: '个人信息' }
    ]
  }
]

const activeMenu = computed(() => route.path)

function handleSelect(path: string) {
  router.push(path)
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-layout__sidebar">
      <div class="sidebar-brand">Warmart Admin</div>
      <el-menu
        :default-active="activeMenu"
        :router="false"
        background-color="#1a2332"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <template v-for="item in menuItems" :key="item.label">
          <el-sub-menu v-if="item.children" :index="item.label">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="child.path"
              @click="handleSelect(child.path)"
            >
              {{ child.label }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path!" @click="handleSelect(item.path!)">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.label }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </aside>
    <div class="admin-layout__main">
      <header class="admin-layout__header">
        <div class="admin-layout__header-title">{{ route.meta.title || 'Warmart Admin' }}</div>
        <div class="admin-layout__header-actions">
          <span class="admin-layout__header-user">
            {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '管理员' }}
          </span>
          <el-button size="small" type="danger" plain @click="handleLogout">退出</el-button>
        </div>
      </header>
      <div class="admin-layout__content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body, #app { height: 100%; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }

.admin-layout { display: flex; height: 100vh; }
.admin-layout__sidebar { width: 220px; background: #1a2332; flex-shrink: 0; overflow-y: auto; }
.sidebar-brand { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 16px; font-weight: 600; letter-spacing: 1px; background: rgba(0,0,0,0.1); }
.admin-layout__main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.admin-layout__header { height: 56px; flex-shrink: 0; display: flex; align-items: center; justify-content: space-between; padding: 0 24px; background: #fff; border-bottom: 1px solid #e4e7ed; }
.admin-layout__header-title { font-size: 16px; font-weight: 600; color: #303133; }
.admin-layout__header-actions { display: flex; align-items: center; gap: 16px; }
.admin-layout__header-user { font-size: 13px; color: #606266; }
.admin-layout__content { flex: 1; padding: 20px; overflow-y: auto; background: #f5f7fa; }
</style>
