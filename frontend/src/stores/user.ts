import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const refreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<Record<string, any> | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string, newRefreshToken: string) {
    token.value = newToken
    refreshToken.value = newRefreshToken
    localStorage.setItem('token', newToken)
    localStorage.setItem('refreshToken', newRefreshToken)
  }

  function setUserInfo(info: Record<string, any>) {
    userInfo.value = info
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  return { token, refreshToken, userInfo, isLoggedIn, setToken, setUserInfo, logout }
})
