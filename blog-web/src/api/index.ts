import request from '../utils/request'

export function getHomepageData() {
  return request.get('/api/public/homepage')
}

export function getArticles(params: any) {
  return request.get('/api/public/articles', { params })
}

export function getArticleBySlug(slug: string) {
  return request.get(`/api/public/articles/${slug}`)
}

export function getCategories() {
  return request.get('/api/public/categories')
}

export function getTags() {
  return request.get('/api/public/tags/all')
}

export function getSiteConfig() {
  return request.get('/api/public/config/site')
}

export function getMenus() {
  return request.get('/api/public/config/menus')
}

export function getCommentsByArticle(articleId: number) {
  return request.get(`/api/comments/article/${articleId}`)
}

export function postComment(data: any) {
  return request.post('/api/comments', data)
}

export function recordVisit(data: any) {
  return request.post('/api/visits', data)
}

export function likeArticle(id: number) {
  return request.post(`/api/articles/${id}/like`)
}

export function subscribe(data: { email: string; name?: string; source?: string; tags?: string }) {
  return request.post('/api/subscribe', data)
}
