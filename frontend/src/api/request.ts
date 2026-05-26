import axios from 'axios'
import { showToast } from '@/utils/toast'
import { useUserStore } from '@/stores/user'
import { refreshToken as refreshTokenApi } from './auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

const publicApis = [
  '/product/list', '/product/detail/', '/product/search',
  '/product/category/', '/product/banner/', '/product/spec/',
  '/product/comment/list/', '/product/consultation/list/', '/product/inner/',
  '/auth/'
]

function isPublicApi(url: string = '') {
  return publicApis.some(prefix => url.startsWith(prefix))
}

let isRefreshing = false
let pendingQueue: Array<{ resolve: (token: string) => void; reject: () => void }> = []

function onRefreshed(token: string) {
  pendingQueue.forEach(p => p.resolve(token))
  pendingQueue = []
}

function onRefreshFailed() {
  pendingQueue.forEach(p => p.reject())
  pendingQueue = []
}

request.interceptors.request.use((config) => {
  if (isPublicApi(config.url)) return config
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200 || res.code === undefined) {
      return res.data !== undefined ? res.data : res
    }
    const errMsg = res.msg || res.message || '请求失败'
    showToast(errMsg, 'error')
    return Promise.reject(new Error(errMsg))
  },
  async (error) => {
    const originalRequest = error.config
    if (error.response?.status === 401 && !originalRequest._retry) {
      const userStore = useUserStore()
      if (!userStore.refreshToken) {
        userStore.logout()
        window.location.href = '/auth/login'
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          pendingQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(request(originalRequest))
            },
            reject: () => {
              reject(error)
            }
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await refreshTokenApi(userStore.refreshToken) as any
        const newToken = res.accessToken
        const newRefreshToken = res.refreshToken
        userStore.setToken(newToken, newRefreshToken)
        onRefreshed(newToken)
        originalRequest.headers.Authorization = `Bearer ${newToken}`
        return request(originalRequest)
      } catch {
        onRefreshFailed()
        userStore.logout()
        window.location.href = '/auth/login'
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response?.status !== 401) {
      showToast(error.message || '网络错误', 'error')
    }
    return Promise.reject(error)
  }
)

export default request
