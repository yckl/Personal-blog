import request from '../utils/request'

export function login(data: { username: string; password: string }) {
  return request.post('/api/admin/auth/login', data)
}

export function verify2fa(data: { tempToken: string; code: string }) {
  return request.post('/api/admin/auth/verify-2fa', data)
}

export function refreshToken(data: { refreshToken: string }) {
  return request.post('/api/admin/auth/refresh', data)
}

export function logout() {
  return request.post('/api/admin/auth/logout')
}

export function register(data: { username: string; password: string; nickname?: string; email?: string }) {
  return request.post('/api/admin/auth/register', data)
}

export function getCurrentUser() {
  return request.get('/api/admin/auth/me')
}
