import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

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
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
