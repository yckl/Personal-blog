import request from '../utils/request'

// ---- Admin article API ----

export function getArticles(params: any) {
  return request.get('/api/admin/article/page', { params })
}

export function getArticle(id: number) {
  return request.get(`/api/admin/article/${id}`)
}

export function createArticle(data: any) {
  return request.post('/api/admin/article/create', data)
}

export function updateArticle(id: number, data: any) {
  return request.put(`/api/admin/article/update/${id}`, data)
}

export function deleteArticle(id: number) {
  return request.delete(`/api/admin/article/${id}`)
}

export function publishArticle(id: number, options?: { sendNewsletter?: boolean; visibility?: string }) {
  return request.post(`/api/admin/article/publish/${id}`, options || {})
}

export function scheduleArticle(id: number, data: { scheduledAt: string; sendNewsletter?: boolean; visibility?: string }) {
  return request.post(`/api/admin/article/schedule/${id}`, data)
}

export function generatePreviewLink(id: number) {
  return request.post(`/api/admin/article/preview/${id}`)
}

export function unpublishArticle(id: number) {
  return request.put(`/api/admin/article/${id}/draft`)
}



// ---- Batch operations ----

export function batchDeleteArticles(ids: number[]) {
  return request.post('/api/admin/article/batch-delete', { ids })
}

export function batchTopArticles(ids: number[]) {
  return request.post('/api/admin/article/batch-top', { ids })
}

export function batchArchiveArticles(ids: number[]) {
  return request.post('/api/admin/article/batch-archive', { ids })
}

export function batchPublishArticles(ids: number[]) {
  return request.post('/api/admin/article/batch-publish', { ids })
}

export function batchUnpublishArticles(ids: number[]) {
  return request.post('/api/admin/article/batch-unpublish', { ids })
}

// ---- Version history ----

export function getArticleVersions(articleId: number) {
  return request.get(`/api/admin/article/${articleId}/versions`)
}

export function getArticleVersion(versionId: number) {
  return request.get(`/api/admin/article/version/${versionId}`)
}

export function rollbackVersion(articleId: number, versionId: number) {
  return request.post(`/api/admin/article/${articleId}/rollback/${versionId}`)
}

// ---- Export ----

export function exportArticleMd(id: number) {
  return request.get(`/api/admin/export/article/${id}`, { responseType: 'blob' })
}

export function exportSiteZip() {
  return request.get('/api/admin/export/site', { responseType: 'blob' })
}

export function exportSiteJson() {
  return request.get('/api/admin/export/site-json', { responseType: 'blob' })
}

// ---- Import ----

export function importSiteJson(file: File) {
  const fd = new FormData(); fd.append('file', file)
  return request.post('/api/admin/import/site', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function importMarkdownZip(file: File) {
  const fd = new FormData(); fd.append('file', file)
  return request.post('/api/admin/import/markdown', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
