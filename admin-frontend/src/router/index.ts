import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'AdminLogin',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '控制台' }
      },
      {
        path: 'users',
        name: 'UserList',
        component: () => import('@/views/UserListView.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'products',
        name: 'AdminProductList',
        component: () => import('@/views/ProductListView.vue'),
        meta: { title: '商品管理' }
      },
      {
        path: 'categories',
        name: 'CategoryList',
        component: () => import('@/views/CategoryView.vue'),
        meta: { title: '分类管理' }
      },
      {
        path: 'orders',
        name: 'AdminOrderList',
        component: () => import('@/views/OrderListView.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'logistics',
        name: 'LogisticsManage',
        component: () => import('@/views/LogisticsView.vue'),
        meta: { title: '物流管理' }
      },
      {
        path: 'refunds',
        name: 'RefundManage',
        component: () => import('@/views/RefundManageView.vue'),
        meta: { title: '退款管理' }
      },
      {
        path: 'comments',
        name: 'CommentManage',
        component: () => import('@/views/CommentManageView.vue'),
        meta: { title: '评价管理' }
      },
      {
        path: 'banners',
        name: 'BannerList',
        component: () => import('@/views/BannerView.vue'),
        meta: { title: '轮播管理' }
      },
      {
        path: 'notices',
        name: 'AdminNoticeList',
        component: () => import('@/views/NoticeView.vue'),
        meta: { title: '公告管理' }
      },
      {
        path: 'feedbacks',
        name: 'FeedbackList',
        component: () => import('@/views/FeedbackView.vue'),
        meta: { title: '反馈管理' }
      },
      {
        path: 'consultations',
        name: 'AdminConsultationList',
        component: () => import('@/views/ConsultationView.vue'),
        meta: { title: '咨询管理' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '个人信息' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && userStore.token) {
    return { path: '/dashboard' }
  }
})

export default router
