import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '../utils/request'

export interface MemberInfo {
  id: number
  email: string
  nickname: string
  avatar: string | null
  tier: string
  tierExpiresAt: string | null
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('member_token'))
  const user = ref<MemberInfo | null>(null)
  const showLoginModal = ref(false)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isPremium = computed(() => user.value?.tier === 'PREMIUM')

  // Restore user from localStorage on init
  function init() {
    const saved = localStorage.getItem('member_user')
    if (saved) {
      try { user.value = JSON.parse(saved) } catch { user.value = null }
    }
    if (token.value) {
      // Set default auth header
      request.defaults.headers.common['Authorization'] = `Bearer ${token.value}`
    }
  }

  async function login(email: string, password: string) {
    const res: any = await request.post('/api/member/login', { email, password })
    const data = res.data
    token.value = data.token
    user.value = {
      id: data.id,
      email: data.email,
      nickname: data.nickname,
      avatar: data.avatar,
      tier: data.tier,
      tierExpiresAt: data.tierExpiresAt
    }
    localStorage.setItem('member_token', data.token)
    localStorage.setItem('member_user', JSON.stringify(user.value))
    request.defaults.headers.common['Authorization'] = `Bearer ${data.token}`
  }

  async function register(email: string, password: string, nickname?: string) {
    const res: any = await request.post('/api/member/register', { email, password, nickname })
    return res.data
  }

  function logout() {
    token.value = null
    user.value = null
    localStorage.removeItem('member_token')
    localStorage.removeItem('member_user')
    delete request.defaults.headers.common['Authorization']
  }

  async function loadProfile() {
    if (!user.value) return
    try {
      const res: any = await request.get(`/api/member/${user.value.id}/profile`)
      const data = res.data
      user.value = {
        id: data.id,
        email: data.email,
        nickname: data.nickname,
        avatar: data.avatar,
        tier: data.tier,
        tierExpiresAt: data.tierExpiresAt
      }
      localStorage.setItem('member_user', JSON.stringify(user.value))
    } catch {
      // If 401, force logout
      logout()
    }
  }

  function openLogin() { showLoginModal.value = true }
  function closeLogin() { showLoginModal.value = false }

  return {
    token, user, showLoginModal,
    isLoggedIn, isPremium,
    init, login, register, logout, loadProfile,
    openLogin, closeLogin
  }
})
