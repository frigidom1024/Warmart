import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', name: 'Home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '首页' } },
  { path: '/auth/login', name: 'Login', component: () => import('@/views/auth/LoginView.vue'), meta: { title: '登录' } },
  { path: '/auth/register', name: 'Register', component: () => import('@/views/auth/RegisterView.vue'), meta: { title: '注册' } },
  { path: '/auth/forgot-password', name: 'ForgotPassword', component: () => import('@/views/auth/ForgotPasswordView.vue'), meta: { title: '忘记密码' } },
  { path: '/product/list', name: 'ProductList', component: () => import('@/views/product/ProductListView.vue'), meta: { title: '商品列表' } },
  { path: '/product/detail/:id', name: 'ProductDetail', component: () => import('@/views/product/ProductDetailView.vue'), meta: { title: '商品详情' } },
  { path: '/cart', name: 'Cart', component: () => import('@/views/cart/CartView.vue'), meta: { title: '购物车', requiresAuth: true } },
  { path: '/order/create', name: 'OrderCreate', component: () => import('@/views/order/OrderCreateView.vue'), meta: { title: '确认订单', requiresAuth: true } },
  { path: '/order/list', name: 'OrderList', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '我的订单', requiresAuth: true } },
  { path: '/order/detail/:id', name: 'OrderDetail', component: () => import('@/views/order/OrderDetailView.vue'), meta: { title: '订单详情', requiresAuth: true } },
  { path: '/user/info', name: 'UserInfo', component: () => import('@/views/user/UserInfoView.vue'), meta: { title: '个人信息', requiresAuth: true } },
  { path: '/user/address', name: 'UserAddress', component: () => import('@/views/user/UserAddressView.vue'), meta: { title: '收货地址', requiresAuth: true } },
  { path: '/user/favorites', name: 'UserFavorites', component: () => import('@/views/user/UserFavoritesView.vue'), meta: { title: '我的收藏', requiresAuth: true } },
  { path: '/user/feedback', name: 'UserFeedback', component: () => import('@/views/user/UserFeedbackView.vue'), meta: { title: '意见反馈', requiresAuth: true } },
  { path: '/service/notice', name: 'Notice', component: () => import('@/views/service/NoticeView.vue'), meta: { title: '系统公告' } },
  { path: '/service/consultation', name: 'Consultation', component: () => import('@/views/service/ConsultationView.vue'), meta: { title: '售前咨询', requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 网上商城` : '网上商城'
  if (to.meta.requiresAuth) {
    const store = useUserStore()
    if (!store.token) {
      next({ path: '/auth/login', query: { redirect: to.fullPath } })
      return
    }
  }
  next()
})

export default router
