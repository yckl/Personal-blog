import axios from 'axios'

const request = axios.create({
  baseURL: import.meta.env.DEV ? '' : 'http://localhost:8088',
  timeout: 15000,
})

// Request interceptor: attach auth token
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('member_token')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: handle errors + 401 token refresh
let isRefreshing = false
let failedQueue: { resolve: (v: any) => void; reject: (e: any) => void; config: any }[] = []

function processQueue(error: any, token: string | null = null) {
  for (const prom of failedQueue) {
    if (token) {
      prom.config.headers.Authorization = `Bearer ${token}`
      prom.resolve(request(prom.config))
    } else {
      prom.reject(error)
    }
  }
  failedQueue = []
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  async (error) => {
    const originalRequest = error.config

    // 401 — attempt token refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        // Queue this request until refresh completes
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject, config: originalRequest })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      const refreshToken = localStorage.getItem('member_refresh_token')
      if (refreshToken) {
        try {
          const res = await axios.post(
            (import.meta.env.DEV ? '' : 'http://localhost:8088') + '/api/member/auth/refresh',
            { refreshToken }
          )
          const newToken = res.data?.data?.token
          if (newToken) {
            localStorage.setItem('member_token', newToken)
            originalRequest.headers.Authorization = `Bearer ${newToken}`
            processQueue(null, newToken)
            return request(originalRequest)
          }
        } catch {
          processQueue(error, null)
          localStorage.removeItem('member_token')
          localStorage.removeItem('member_refresh_token')
        } finally {
          isRefreshing = false
        }
      } else {
        isRefreshing = false
        // No refresh token — clear auth silently (don't redirect, stay on page)
        localStorage.removeItem('member_token')
      }
    }

    return Promise.reject(error)
  }
)

export default request
