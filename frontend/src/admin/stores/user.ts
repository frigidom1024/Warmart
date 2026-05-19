import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('admin-user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref<Record<string, any> | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('admin_token', newToken)
  }

  function setUserInfo(info: Record<string, any>) {
    userInfo.value = info
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
  }

  return { token, userInfo, isLoggedIn, setToken, setUserInfo, logout }
})
