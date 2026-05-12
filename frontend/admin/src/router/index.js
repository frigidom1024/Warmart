import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '管理员登录' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '控制台' } },
      { path: 'users', name: 'Users', component: () => import('@/views/user/UserListView.vue'), meta: { title: '用户管理' } },
      { path: 'products', name: 'Products', component: () => import('@/views/product/ProductListView.vue'), meta: { title: '商品管理' } },
      { path: 'categories', name: 'Categories', component: () => import('@/views/product/CategoryView.vue'), meta: { title: '分类管理' } },
      { path: 'orders', name: 'Orders', component: () => import('@/views/order/OrderListView.vue'), meta: { title: '订单管理' } },
      { path: 'banners', name: 'Banners', component: () => import('@/views/content/BannerView.vue'), meta: { title: '轮播管理' } },
      { path: 'notices', name: 'Notices', component: () => import('@/views/content/NoticeView.vue'), meta: { title: '公告管理' } },
      { path: 'feedbacks', name: 'Feedbacks', component: () => import('@/views/system/FeedbackView.vue'), meta: { title: '反馈管理' } },
      { path: 'consultations', name: 'Consultations', component: () => import('@/views/system/ConsultationView.vue'), meta: { title: '咨询管理' } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/system/ProfileView.vue'), meta: { title: '个人信息' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 商城管理系统` : '商城管理系统'
  if (to.path !== '/login') {
    const store = useUserStore()
    if (!store.token) {
      next('/login')
      return
    }
  }
  next()
})

export default router
