import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/auth/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/auth/register',
    name: 'Register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/auth/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/ForgotPasswordView.vue'),
    meta: { title: '忘记密码' }
  },
  {
    path: '/product/list',
    name: 'ProductList',
    component: () => import('@/views/ProductListView.vue'),
    meta: { title: '商品列表' }
  },
  {
    path: '/product/detail/:id',
    name: 'ProductDetail',
    component: () => import('@/views/ProductDetailView.vue'),
    meta: { title: '商品详情' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/CartView.vue'),
    meta: { title: '购物车', requiresAuth: true }
  },
  {
    path: '/order/create',
    name: 'OrderCreate',
    component: () => import('@/views/OrderCreateView.vue'),
    meta: { title: '创建订单', requiresAuth: true }
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('@/views/OrderListView.vue'),
    meta: { title: '我的订单', requiresAuth: true }
  },
  {
    path: '/order/detail/:id',
    name: 'OrderDetail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单详情', requiresAuth: true }
  },
  {
    path: '/user/info',
    name: 'UserInfo',
    component: () => import('@/views/UserInfoView.vue'),
    meta: { title: '个人信息', requiresAuth: true }
  },
  {
    path: '/user/address',
    name: 'UserAddress',
    component: () => import('@/views/UserAddressView.vue'),
    meta: { title: '收货地址', requiresAuth: true }
  },
  {
    path: '/user/favorites',
    name: 'UserFavorites',
    component: () => import('@/views/UserFavoritesView.vue'),
    meta: { title: '我的收藏', requiresAuth: true }
  },
  {
    path: '/user/feedback',
    name: 'UserFeedback',
    component: () => import('@/views/UserFeedbackView.vue'),
    meta: { title: '意见反馈', requiresAuth: true }
  },
  {
    path: '/service/notice',
    name: 'Notice',
    component: () => import('@/views/NoticeView.vue'),
    meta: { title: '系统公告' }
  },
  {
    path: '/service/consultation',
    name: 'Consultation',
    component: () => import('@/views/ConsultationView.vue'),
    meta: { title: '售前咨询', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()
    if (!userStore.token) {
      return { name: 'Login', query: { redirect: to.fullPath } }
    }
  }
})

export default router
