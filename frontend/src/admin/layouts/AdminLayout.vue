<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const menuItems = [
  {
    path: '/dashboard',
    icon: 'Odometer',
    label: '控制台'
  },
  {
    path: '/users',
    icon: 'User',
    label: '用户管理'
  },
  {
    label: '商品管理',
    icon: 'Goods',
    children: [
      { path: '/products', label: '商品列表' },
      { path: '/categories', label: '分类管理' }
    ]
  },
  {
    path: '/orders',
    icon: 'List',
    label: '订单管理'
  },
  {
    label: '内容管理',
    icon: 'Document',
    children: [
      { path: '/banners', label: '轮播管理' },
      { path: '/notices', label: '公告管理' }
    ]
  },
  {
    label: '系统',
    icon: 'Setting',
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
    <!-- Sidebar -->
    <aside class="admin-layout__sidebar">
      <div class="sidebar-brand">
        <span>◆</span> Warmart Admin
      </div>

      <el-menu
        :default-active="activeMenu"
        :router="false"
        class="sidebar-menu"
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

    <!-- Main area -->
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

<style scoped>
.admin-layout__header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--wa-text-primary);
}

.admin-layout__header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.admin-layout__header-user {
  font-size: 13px;
  color: var(--wa-text-regular);
}
</style>
