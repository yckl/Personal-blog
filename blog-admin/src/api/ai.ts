import request from '../utils/request'

export function aiGenerateTitle(content: string, articleId?: number) {
  return request.post('/api/admin/ai/generate-title', { content, articleId })
}
export function aiGenerateSummary(content: string, articleId?: number) {
  return request.post('/api/admin/ai/generate-summary', { content, articleId })
}
export function aiGenerateSeoTitle(content: string, currentTitle: string, articleId?: number) {
  return request.post('/api/admin/ai/generate-seo-title', { content, currentTitle, articleId })
}
export function aiGenerateMetaDescription(content: string, articleId?: number) {
  return request.post('/api/admin/ai/generate-meta-description', { content, articleId })
}
export function aiGenerateOutline(content: string, articleId?: number) {
  return request.post('/api/admin/ai/generate-outline', { content, articleId })
}
export function aiRecommendTags(content: string, articleId?: number) {
  return request.post('/api/admin/ai/recommend-tags', { content, articleId })
}
export function aiGenerateNewsletter(content: string, articleId?: number) {
  return request.post('/api/admin/ai/generate-newsletter', { content, articleId })
}
export function aiRecommendInternalLinks(content: string, articleId?: number) {
  return request.post('/api/admin/ai/recommend-internal-links', { content, articleId })
}
