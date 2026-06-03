import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('admin-user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref<Record<string, any> | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const isSuperAdmin = computed(() => role.value === 'SUPER_ADMIN')
  const isAdmin = computed(() => role.value === 'ADMIN' || role.value === 'SUPER_ADMIN')

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

  return { token, userInfo, isLoggedIn, role, isSuperAdmin, isAdmin, setToken, setUserInfo, logout }
})
