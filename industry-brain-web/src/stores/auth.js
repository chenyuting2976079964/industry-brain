import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authService } from '@/services/auth.service'

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const token = ref(sessionStorage.getItem('token') || '')
  const region = ref(sessionStorage.getItem('region') || '马鞍山市')

  const isLoggedIn = computed(() => !!token.value)

  /**
   * A01 — 登录
   * 后端返回 data: { token: "...", user: { id, username, displayName, role, avatar, phone, email } }
   */
  async function login(username, password, selectedRegion) {
    const res = await authService.login(username, password)
    // api.js 已解包 AjaxResult.data, 所以 res = { token, user }
    token.value = res.token
    user.value = res.user
    region.value = selectedRegion
    sessionStorage.setItem('token', res.token)
    sessionStorage.setItem('loggedIn', '1')
    sessionStorage.setItem('region', selectedRegion)
    return res
  }

  /**
   * A03 — 获取当前用户信息
   * 后端返回 data: { id, username, displayName, role, avatar, phone, email }
   */
  async function fetchCurrentUser() {
    try {
      const res = await authService.getCurrentUser()
      // api.js 已解包 AjaxResult.data, 所以 res = user 对象
      user.value = res
      return res
    } catch (e) {
      // Token expired, force re-login
      token.value = ''
      user.value = null
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('loggedIn')
      throw e
    }
  }

  /**
   * A02 — 登出
   */
  async function logout() {
    try { await authService.logout() } catch (e) { /* ignore */ }
    token.value = ''
    user.value = null
    sessionStorage.removeItem('token')
    sessionStorage.removeItem('loggedIn')
    sessionStorage.removeItem('region')
    sessionStorage.removeItem('navContext')
  }

  return { user, token, region, isLoggedIn, login, fetchCurrentUser, logout }
})
