import axios from 'axios'

let isRefreshing = false
let pendingRequests: Array<(token: string) => void> = []

const request = axios.create({
  baseURL: 'http://localhost:8088',
  timeout: 15000,
})

// Request interceptor - add JWT token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor - handle errors + auto-refresh
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        return handle401(response.config)
      }
      return Promise.reject(new Error(res.message || 'Request failed'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      return handle401(error.config)
    }
    return Promise.reject(error)
  }
)

/**
 * Handle 401 by attempting a token refresh.
 * If refresh fails, redirect to login.
 */
async function handle401(originalConfig: any) {
  // Don't try to refresh if the failing request IS the refresh request
  if (originalConfig.url?.includes('/auth/refresh')) {
    clearAndRedirect()
    return Promise.reject(new Error('Session expired'))
  }

  if (isRefreshing) {
    // Queue the request until the refresh is done
    return new Promise((resolve) => {
      pendingRequests.push((newToken: string) => {
        originalConfig.headers.Authorization = `Bearer ${newToken}`
        resolve(request(originalConfig))
      })
    })
  }

  isRefreshing = true
  const refreshToken = localStorage.getItem('refreshToken')

  if (!refreshToken) {
    clearAndRedirect()
    return Promise.reject(new Error('No refresh token'))
  }

  try {
    // Call refresh endpoint directly to avoid interceptor loops
    const res = await axios.post('http://localhost:8088/api/admin/auth/refresh', {
      refreshToken
    })

    if (res.data.code === 200) {
      const newToken = res.data.data.token
      const newRefreshToken = res.data.data.refreshToken
      localStorage.setItem('token', newToken)
      localStorage.setItem('refreshToken', newRefreshToken)

      // Retry all pending requests
      pendingRequests.forEach(cb => cb(newToken))
      pendingRequests = []

      // Retry the original request
      originalConfig.headers.Authorization = `Bearer ${newToken}`
      return request(originalConfig)
    } else {
      clearAndRedirect()
      return Promise.reject(new Error('Refresh failed'))
    }
  } catch {
    clearAndRedirect()
    return Promise.reject(new Error('Refresh failed'))
  } finally {
    isRefreshing = false
  }
}

function clearAndRedirect() {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  pendingRequests = []
  window.location.href = '/login'
}

export default request
