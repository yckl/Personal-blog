import request from '../utils/request'

// Categories
export function getCategories() {
  return request.get('/api/categories')
}
export function createCategory(data: any) {
  return request.post('/api/categories', data)
}
export function updateCategory(id: number, data: any) {
  return request.put(`/api/categories/${id}`, data)
}
export function deleteCategory(id: number) {
  return request.delete(`/api/categories/${id}`)
}

// Tags
export function getTags() {
  return request.get('/api/tags')
}
export function createTag(data: any) {
  return request.post('/api/tags', data)
}
export function updateTag(id: number, data: any) {
  return request.put(`/api/tags/${id}`, data)
}
export function deleteTag(id: number) {
  return request.delete(`/api/tags/${id}`)
}

// Series
export function getSeries() {
  return request.get('/api/series')
}
export function createSeries(data: any) {
  return request.post('/api/series', data)
}
export function updateSeries(id: number, data: any) {
  return request.put(`/api/series/${id}`, data)
}
export function deleteSeries(id: number) {
  return request.delete(`/api/series/${id}`)
}

// Comments
export function getComments(params: any) {
  return request.get('/api/admin/comments', { params })
}
export function approveComment(id: number) {
  return request.put(`/api/admin/comments/${id}/approve`)
}
export function rejectComment(id: number) {
  return request.put(`/api/admin/comments/${id}/reject`)
}
export function deleteComment(id: number) {
  return request.delete(`/api/admin/comments/${id}`)
}

// Media
export function getMediaFiles(params: any) {
  return request.get('/api/admin/media/page', { params })
}
export function getMediaFile(id: number) {
  return request.get(`/api/admin/media/${id}`)
}
export function uploadMedia(file: File, onProgress?: (percent: number) => void) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/admin/media/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e: any) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
    }
  })
}
export function updateMediaAlt(id: number, altText: string) {
  return request.put(`/api/admin/media/${id}/alt`, { altText })
}
export function deleteMedia(id: number) {
  return request.delete(`/api/admin/media/${id}`)
}

// Subscribers
export function getSubscribers(params: any) {
  return request.get('/api/admin/subscribers', { params })
}
export function deleteSubscriber(id: number) {
  return request.delete(`/api/admin/subscribers/${id}`)
}

// Site Config
export function getSiteConfig() {
  return request.get('/api/admin/site-config')
}
export function updateSiteConfig(data: any) {
  return request.put('/api/admin/site-config', data)
}

// Menus
export function getMenus() {
  return request.get('/api/admin/menu')
}
export function createMenu(data: any) {
  return request.post('/api/admin/menu', data)
}
export function updateMenu(id: number, data: any) {
  return request.put(`/api/admin/menu/${id}`, data)
}
export function deleteMenu(id: number) {
  return request.delete(`/api/admin/menu/${id}`)
}
export function batchSortMenus(items: { id: number; sortOrder: number }[]) {
  return request.put('/api/admin/menu/sort', items)
}

// Stats / Dashboard
export function getDashboardStats() {
  return request.get('/api/admin/dashboard/summary')
}
export function getDashboardTrend(days: number = 7) {
  return request.get('/api/admin/dashboard/trend', { params: { days } })
}
export function getHotArticles(limit: number = 10) {
  return request.get('/api/admin/dashboard/hot-articles', { params: { limit } })
}
export function getRecentComments(limit: number = 10) {
  return request.get('/api/admin/dashboard/recent-comments', { params: { limit } })
}

