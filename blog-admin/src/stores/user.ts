import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, verify2fa as verify2faApi, refreshToken as refreshTokenApi, logout as logoutApi, getCurrentUser } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const storedRefreshToken = ref(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<any>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  // 2FA flow state
  const tempToken = ref('')
  const requireTwoFactor = ref(false)

  const isLoggedIn = computed(() => !!token.value)

  /**
   * Step 1: Login with credentials.
   * Returns true if 2FA is required, false if login is complete.
   */
  async function login(username: string, password: string): Promise<boolean> {
    const res: any = await loginApi({ username, password })
    const data = res.data

    if (data.requireTwoFactor) {
      tempToken.value = data.tempToken
      requireTwoFactor.value = true
      return true // 2FA needed
    }

    setAuthData(data)
    return false // Login complete
  }

  /**
   * Step 2: Verify 2FA code.
   */
  async function verify2fa(code: string) {
    const res: any = await verify2faApi({ tempToken: tempToken.value, code })
    setAuthData(res.data)
    tempToken.value = ''
    requireTwoFactor.value = false
  }

  /**
   * Refresh the access token using the stored refresh token.
   */
  async function refreshAccessToken(): Promise<boolean> {
    if (!storedRefreshToken.value) return false
    try {
      const res: any = await refreshTokenApi({ refreshToken: storedRefreshToken.value })
      setAuthData(res.data)
      return true
    } catch {
      clearAuth()
      return false
    }
  }

  async function fetchUser() {
    const res: any = await getCurrentUser()
    userInfo.value = res.data
    // Populate roles & permissions so sidebar visibility works after page refresh
    if (res.data?.roles) {
      roles.value = res.data.roles
      localStorage.setItem('roles', JSON.stringify(res.data.roles))
    }
    if (res.data?.permissions) {
      permissions.value = res.data.permissions
      localStorage.setItem('permissions', JSON.stringify(res.data.permissions))
    }
  }

  async function logout() {
    try {
      await logoutApi()
    } catch {
      // Ignore errors — clear local state regardless
    }
    clearAuth()
  }

  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function hasPermission(perm: string): boolean {
    return permissions.value.includes(perm)
  }

  function isAdmin(): boolean {
    return hasRole('SUPER_ADMIN') || hasRole('ADMIN')
  }

  // ---- Private helpers ----

  function setAuthData(data: any) {
    token.value = data.token
    storedRefreshToken.value = data.refreshToken
    userInfo.value = data
    roles.value = data.roles || []
    permissions.value = data.permissions || []
    localStorage.setItem('token', data.token)
    localStorage.setItem('refreshToken', data.refreshToken)
    localStorage.setItem('roles', JSON.stringify(roles.value))
    localStorage.setItem('permissions', JSON.stringify(permissions.value))
  }

  function clearAuth() {
    token.value = ''
    storedRefreshToken.value = ''
    userInfo.value = null
    roles.value = []
    permissions.value = []
    tempToken.value = ''
    requireTwoFactor.value = false
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('roles')
    localStorage.removeItem('permissions')
  }

  return {
    token, storedRefreshToken, userInfo, roles, permissions,
    tempToken, requireTwoFactor, isLoggedIn,
    login, verify2fa, refreshAccessToken, fetchUser, logout,
    hasRole, hasPermission, isAdmin
  }
})
