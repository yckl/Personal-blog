<template>
  <div v-if="article" class="article-page">
    <!-- ===== Floating Toolbar (appears at 30% scroll) ===== -->
    <Transition name="slide-in">
      <div class="floating-toolbar glass" v-show="showToolbar">
        <button class="toolbar-btn" :class="{ active: liked }" @click="handleLike" title="Like">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
          <span v-if="article.likeCount">{{ article.likeCount }}</span>
        </button>
        <button class="toolbar-btn" @click="shareArticle('copy')" title="Copy Link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"/></svg>
        </button>
        <button class="toolbar-btn" @click="shareArticle('twitter')" title="Share on X">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
        </button>
        <div class="toolbar-divider" />
        <button class="toolbar-btn" @click="scrollToTop" title="回到顶部">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 19V5M5 12l7-7 7 7"/></svg>
        </button>
        <div class="toolbar-timer" v-if="readingSeconds > 0">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
          {{ formatTimer(readingSeconds) }}
        </div>
      </div>
    </Transition>

    <!-- ===== Article Layout ===== -->
    <div class="article-layout">
      <!-- Main Content Column -->
      <article class="article-main fade-in">
        <!-- Header -->
        <header class="article-header">
          <div class="article-tags">
            <router-link v-for="tag in (article.tags||[])" :key="tag.id" :to="`/tag/${tag.id}`" class="tag"
              :style="{ '--tag-color': tag.color||'#a855f7' }">{{ tag.name }}</router-link>
            <span v-if="article.categoryName" class="category-badge">{{ article.categoryName }}</span>
          </div>
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <img v-if="article.authorAvatar" :src="article.authorAvatar" class="author-avatar" />
            <span class="author-name">{{ article.authorName }}</span>
            <span class="meta-dot">·</span>
            <span>{{ formatDate(article.publishedAt || article.createdAt) }}</span>
            <span class="meta-dot">·</span>
            <span>{{ article.readingTime || Math.max(1, Math.round((article.wordCount||0)/200)) }} 分钟阅读</span>
            <span class="meta-dot">·</span>
            <span>{{ article.viewCount }} 阅读</span>
          </div>
        </header>

        <!-- Cover -->
        <div v-if="article.coverImage" class="article-cover">
          <img :src="article.coverImage" :alt="article.title" @click="openLightbox(article.coverImage)" />
        </div>

        <!-- AI Summary Card -->
        <div class="ai-summary-card glass-card" v-if="article.contentHtml || article.contentMd">
          <div class="ai-summary-header">
            <span class="ai-badge">🤖 AI 摘要</span>
            <button v-if="!aiSummary && !aiLoading" class="ai-gen-btn" @click="generateAiSummary">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/></svg>
              生成摘要
            </button>
          </div>
          <div v-if="aiLoading" class="ai-loading">
            <div class="ai-dots"><span /><span /><span /></div>
            <span>AI 正在总结...</span>
          </div>
          <p v-else-if="aiSummary" class="ai-summary-text">{{ aiSummary }}</p>
          <p v-else-if="aiError" class="ai-error">{{ aiError }}</p>
        </div>

        <!-- Content -->
        <div ref="contentRef" class="article-content" v-html="article.contentHtml || article.contentMd" />

        <!-- Text Selection Share -->
        <div v-if="selectionTooltip.visible" class="selection-share-tooltip glass"
             :style="{ top: selectionTooltip.y + 'px', left: selectionTooltip.x + 'px' }">
          <button @click="shareSelectedText('twitter')" title="Share on X">𝕏</button>
          <button @click="shareSelectedText('copy')" title="Copy quote">📋</button>
        </div>

        <!-- Prev / Next -->
        <div class="article-nav" v-if="nav.prev || nav.next">
          <router-link v-if="nav.prev" :to="`/article/${nav.prev.slug}`" class="nav-card glass-card prev">
            <span class="nav-label">← 上一篇</span>
            <span class="nav-title">{{ nav.prev.title }}</span>
          </router-link>
          <div v-else />
          <router-link v-if="nav.next" :to="`/article/${nav.next.slug}`" class="nav-card glass-card next">
            <span class="nav-label">下一篇 →</span>
            <span class="nav-title">{{ nav.next.title }}</span>
          </router-link>
        </div>

        <!-- Related Articles — Horizontal Snap Scroll -->
        <section class="related-section" v-if="relatedArticles.length">
          <h2>相关文章</h2>
          <div class="related-scroll">
            <router-link v-for="r in relatedArticles" :key="r.id" :to="`/article/${r.slug}`" class="related-card glass-card">
              <div class="related-cover" :style="{ backgroundImage: r.coverImage ? `url(${r.coverImage})` : 'var(--gradient-1)' }" />
              <div class="related-body">
                <h4>{{ r.title }}</h4>
                <span class="card-meta">{{ formatDate(r.publishedAt) }} · {{ r.viewCount }} 阅读</span>
              </div>
            </router-link>
          </div>
        </section>

        <!-- Subscribe CTA -->
        <section class="subscribe-cta glass-card fade-in">
          <h3 class="gradient-text">喜欢这篇文章？</h3>
          <p>订阅以获取新文章通知，无垃圾邮件，随时退订。</p>
          <form class="cta-form" @submit.prevent="handleSubscribe">
            <input v-model="subEmail" type="email" placeholder="你的邮箱地址" required />
            <button type="submit" class="btn btn-primary btn-glow" :disabled="subscribing">{{ subscribing ? '订阅中...' : '立即订阅' }}</button>
          </form>
          <p v-if="subscribeSuccess" class="success-msg">✅ 订阅成功！</p>
        </section>

        <!-- Comments -->
        <CommentSection v-if="article.allowComment !== false" :articleId="article.id" />
      </article>

      <!-- ===== TOC Sidebar — Glass Fixed Panel ===== -->
      <aside class="toc-sidebar" v-if="tocItems.length">
        <div class="toc-card glass">
          <div class="toc-header">
            <span>目录</span>
          </div>
          <nav class="toc-nav">
            <a v-for="(item, idx) in tocItems" :key="idx" :href="'#' + item.id"
              :class="{ active: activeTocId === item.id }"
              :style="{ paddingLeft: (item.level - 1) * 16 + 16 + 'px' }"
              @click.prevent="scrollToHeading(item.id)">
              <span class="toc-dot" />
              {{ item.text }}
            </a>
          </nav>
        </div>
      </aside>
    </div>

    <!-- Lightbox -->
    <Transition name="fade">
      <div v-if="lightboxSrc" class="lightbox" @click="lightboxSrc = ''">
        <img :src="lightboxSrc" />
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleBySlug, recordVisit, subscribe, likeArticle, getLikeStatus } from '../api'
import request from '../utils/request'
import CommentSection from '../components/CommentSection.vue'

const route = useRoute()
const article = ref<any>(null)
const contentRef = ref<HTMLElement>()

// TOC
const tocItems = ref<{ id: string; text: string; level: number }[]>([])
const activeTocId = ref('')

// Progress & Toolbar
const readingProgress = ref(0)
const showToolbar = ref(false)
const liked = ref(false)
const liking = ref(false)

// Nav
const nav = ref<any>({})
const relatedArticles = ref<any[]>([])

// Lightbox
const lightboxSrc = ref('')

// Text selection share
const selectionTooltip = reactive({ visible: false, x: 0, y: 0, text: '' })

// Subscribe
const subEmail = ref('')
const subscribing = ref(false)
const subscribeSuccess = ref(false)
const currentUrl = ref('')

// AI Summary
const aiSummary = ref('')
const aiLoading = ref(false)
const aiError = ref('')

// Live Reading Timer
const readingSeconds = ref(0)
let readingTimer: ReturnType<typeof setInterval> | null = null

function formatTimer(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s < 10 ? '0' : ''}${s}秒` : `${s}秒`
}

function startReadingTimer() {
  if (readingTimer) return
  readingTimer = setInterval(() => { readingSeconds.value++ }, 1000)
}

function stopReadingTimer() {
  if (readingTimer) { clearInterval(readingTimer); readingTimer = null }
}

async function generateAiSummary() {
  if (!article.value) return
  aiLoading.value = true
  aiError.value = ''
  try {
    // Extract plain text from HTML content
    const tmp = document.createElement('div')
    tmp.innerHTML = article.value.contentHtml || article.value.contentMd || ''
    const plainText = tmp.textContent?.substring(0, 3000) || ''
    
    const res: any = await request.post('/api/public/ai/summarize', {
      title: article.value.title,
      content: plainText
    })
    aiSummary.value = res.data?.summary || res.data || '暂无法生成摘要'
  } catch (e: any) {
    aiError.value = '生成失败，请稍后重试'
  } finally {
    aiLoading.value = false
  }
}

function handleTextSelection() {
  const sel = window.getSelection()
  const text = sel?.toString().trim() || ''
  if (text.length > 10 && contentRef.value?.contains(sel?.anchorNode as Node)) {
    const range = sel!.getRangeAt(0)
    const rect = range.getBoundingClientRect()
    selectionTooltip.visible = true
    selectionTooltip.x = rect.left + rect.width / 2 - 40
    selectionTooltip.y = rect.top + window.scrollY - 45
    selectionTooltip.text = text
  } else {
    selectionTooltip.visible = false
  }
}

function shareSelectedText(platform: string) {
  const text = selectionTooltip.text
  if (platform === 'twitter') {
    const url = encodeURIComponent(window.location.href)
    const quote = encodeURIComponent('"' + text.substring(0, 200) + '"')
    window.open(`https://twitter.com/intent/tweet?text=${quote}&url=${url}`, '_blank')
  } else if (platform === 'copy') {
    navigator.clipboard.writeText(`"${text}" — ${window.location.href}`)
  }
  selectionTooltip.visible = false
}

function shareArticle(platform: string) {
  const url = window.location.href
  const title = article.value?.title || ''
  if (platform === 'twitter') {
    window.open(`https://twitter.com/intent/tweet?text=${encodeURIComponent(title)}&url=${encodeURIComponent(url)}`, '_blank')
  } else if (platform === 'copy') {
    navigator.clipboard.writeText(url)
  }
}

async function handleLike() {
  if (!article.value || liking.value) return
  liking.value = true
  try {
    const res: any = await likeArticle(article.value.id)
    liked.value = res.data.liked
    article.value.likeCount = res.data.likeCount
  } catch {}
  finally { liking.value = false }
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// SEO
function setMeta(name: string, content: string) {
  const attr = name.startsWith('og:') || name.startsWith('twitter:') ? 'property' : 'name'
  let el = document.querySelector(`meta[${attr}="${name}"]`) as HTMLMetaElement
  if (!el) { el = document.createElement('meta'); el.setAttribute(attr, name); document.head.appendChild(el) }
  el.content = content
}

function injectJsonLd(data: Record<string, any>) {
  let script = document.querySelector('script#json-ld-article') as HTMLScriptElement
  if (!script) { script = document.createElement('script'); script.id = 'json-ld-article'; script.type = 'application/ld+json'; document.head.appendChild(script) }
  script.textContent = JSON.stringify(data)
}

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

async function loadArticle(slug: string) {
  const res: any = await getArticleBySlug(slug)
  article.value = res.data
  currentUrl.value = window.location.href

  const a = res.data
  const seoTitle = a.seoTitle || a.title
  document.title = seoTitle + ' - Blog'
  setMeta('description', a.metaDescription || a.excerpt || '')
  setMeta('og:title', seoTitle)
  setMeta('og:description', a.metaDescription || a.excerpt || '')
  setMeta('og:image', a.ogImage || a.coverImage || '')
  setMeta('og:url', window.location.href)
  setMeta('og:type', 'article')
  setMeta('twitter:card', 'summary_large_image')
  setMeta('twitter:title', seoTitle)
  setMeta('twitter:description', a.metaDescription || a.excerpt || '')
  setMeta('twitter:image', a.ogImage || a.coverImage || '')
  if (a.canonicalUrl) {
    let link = document.querySelector('link[rel="canonical"]') as HTMLLinkElement
    if (!link) { link = document.createElement('link'); link.rel = 'canonical'; document.head.appendChild(link) }
    link.href = a.canonicalUrl
  }

  injectJsonLd({
    '@context': 'https://schema.org', '@type': 'BlogPosting',
    headline: a.title, description: a.metaDescription || a.excerpt || '',
    image: a.ogImage || a.coverImage || '',
    datePublished: a.publishedAt, dateModified: a.updatedAt || a.publishedAt,
    author: { '@type': 'Person', name: a.authorName || 'Blog Author' },
    url: window.location.href
  })

  const visitorId = localStorage.getItem('visitor_id') || (() => {
    const id = crypto.randomUUID(); localStorage.setItem('visitor_id', id); return id
  })()
  recordVisit({ articleId: res.data.id, pageUrl: window.location.href, visitorId, pageType: 'article' }).catch(() => {})

  try { const nRes: any = await request.get(`/api/public/articles/${res.data.id}/nav`); nav.value = nRes.data || {} } catch { nav.value = {} }
  try {
    const rRes: any = await request.get(`/api/public/recommend/articles/${res.data.id}`, { params: { scene: 'DETAIL', size: 6 } })
    relatedArticles.value = rRes.data || []
    for (const r of relatedArticles.value) {
      request.post('/api/public/recommend/exposure', { articleId: res.data.id, recommendedArticleId: r.id, scene: 'DETAIL' }).catch(() => {})
    }
  } catch { relatedArticles.value = [] }

  await nextTick()
  parseToc()
  addCodeCopyButtons()
  addImageClickHandlers()

  // Load like status
  try {
    const likeRes: any = await getLikeStatus(res.data.id)
    liked.value = likeRes.data.liked
  } catch {}

  if (contentRef.value) {
    contentRef.value.querySelectorAll('img').forEach((img: HTMLImageElement) => {
      img.loading = 'lazy'
      if (!img.alt) img.alt = 'Article image'
    })
  }
}

function parseToc() {
  if (!contentRef.value) return
  const headings = contentRef.value.querySelectorAll('h1, h2, h3, h4')
  if (!headings.length) return

  // If server provided tocJson, use it and assign IDs to DOM headings by matching order
  if (article.value?.tocJson) {
    try {
      const serverToc = typeof article.value.tocJson === 'string'
        ? JSON.parse(article.value.tocJson)
        : article.value.tocJson
      if (Array.isArray(serverToc) && serverToc.length > 0) {
        // Match server TOC entries to DOM headings by order
        const domH2H3 = Array.from(headings).filter(h => {
          const level = parseInt(h.tagName.substring(1))
          return level >= 2 && level <= 4
        })
        serverToc.forEach((item: any, idx: number) => {
          if (idx < domH2H3.length) {
            domH2H3[idx].id = item.id
          }
        })
        tocItems.value = serverToc.map((item: any) => ({
          id: item.id,
          text: item.text,
          level: item.level || 2
        }))
        setupScrollSpy()
        return
      }
    } catch {}
  }

  // Fallback: generate TOC from DOM headings
  const items: { id: string; text: string; level: number }[] = []
  headings.forEach((h, idx) => {
    const level = parseInt(h.tagName.substring(1))
    if (level < 2 || level > 4) return  // Skip h1
    const text = h.textContent?.trim() || ''
    if (!text) return
    // Generate slug-style ID
    const id = h.id || 'heading-' + idx
    h.id = id
    items.push({ id, text, level })
  })
  tocItems.value = items
  setupScrollSpy()
}

function addCodeCopyButtons() {
  if (!contentRef.value) return
  contentRef.value.querySelectorAll('pre').forEach(pre => {
    if (pre.querySelector('.copy-btn')) return
    const btn = document.createElement('button')
    btn.className = 'copy-btn'
    btn.textContent = 'Copy'
    btn.onclick = () => {
      const code = pre.querySelector('code')?.textContent || pre.textContent || ''
      navigator.clipboard.writeText(code)
      btn.textContent = 'Copied!'
      btn.classList.add('copied')
      setTimeout(() => { btn.textContent = 'Copy'; btn.classList.remove('copied') }, 2000)
    }
    pre.style.position = 'relative'
    pre.appendChild(btn)
  })
}

function addImageClickHandlers() {
  if (!contentRef.value) return
  contentRef.value.querySelectorAll('img').forEach(img => {
    img.style.cursor = 'zoom-in'
    img.onclick = () => openLightbox((img as HTMLImageElement).src)
  })
}

function openLightbox(src: string) { lightboxSrc.value = src }

let tocObserver: IntersectionObserver | null = null

function setupScrollSpy() {
  // Clean up previous observer
  if (tocObserver) { tocObserver.disconnect(); tocObserver = null }
  if (!tocItems.value.length) return

  // Use IntersectionObserver for scroll spy
  const visibleHeadings = new Set<string>()
  tocObserver = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        visibleHeadings.add(entry.target.id)
      } else {
        visibleHeadings.delete(entry.target.id)
      }
    })
    // Find the first visible heading in document order
    for (const item of tocItems.value) {
      if (visibleHeadings.has(item.id)) {
        activeTocId.value = item.id
        return
      }
    }
    // If no heading visible, find the last one that's above viewport
    if (!visibleHeadings.size) {
      let lastAbove = ''
      for (const item of tocItems.value) {
        const el = document.getElementById(item.id)
        if (el && el.getBoundingClientRect().top < 120) {
          lastAbove = item.id
        }
      }
      if (lastAbove) activeTocId.value = lastAbove
    }
  }, {
    rootMargin: '-80px 0px -60% 0px',
    threshold: 0
  })

  // Observe all heading elements
  tocItems.value.forEach(item => {
    const el = document.getElementById(item.id)
    if (el) tocObserver!.observe(el)
  })
}

function scrollToHeading(id: string) {
  const el = document.getElementById(id)
  if (!el) return
  // Offset for sticky header (approx 80px)
  const y = el.getBoundingClientRect().top + window.scrollY - 90
  window.scrollTo({ top: y, behavior: 'smooth' })
  activeTocId.value = id
}

function handleScroll() {
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  readingProgress.value = docHeight > 0 ? Math.min(100, (scrollTop / docHeight) * 100) : 0
  // Show floating toolbar at 30%
  showToolbar.value = readingProgress.value > 30
}

async function handleSubscribe() {
  subscribing.value = true
  try { await subscribe({ email: subEmail.value }); subscribeSuccess.value = true; subEmail.value = '' }
  catch { alert('Subscribe failed') }
  finally { subscribing.value = false }
}

onMounted(() => {
  loadArticle(route.params.slug as string)
  window.addEventListener('scroll', handleScroll, { passive: true })
  document.addEventListener('mouseup', handleTextSelection)
  document.addEventListener('mousedown', () => { selectionTooltip.visible = false })
  startReadingTimer()
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('mouseup', handleTextSelection)
  if (tocObserver) { tocObserver.disconnect(); tocObserver = null }
  stopReadingTimer()
  const jsonLd = document.querySelector('script#json-ld-article')
  if (jsonLd) jsonLd.remove()
})
watch(() => route.params.slug, (slug) => { if (slug) loadArticle(slug as string) })
</script>

<style scoped>
/* ===== Page ===== */
.article-page { position: relative; }

/* ===== AI Summary Card ===== */
.ai-summary-card {
  margin-bottom: 32px; padding: 24px;
  border: 1px solid rgba(168,85,247,0.15);
  background: rgba(168,85,247,0.04);
}
.ai-summary-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px;
}
.ai-badge {
  font-size: 13px; font-weight: 700;
  color: var(--primary);
  display: flex; align-items: center; gap: 6px;
}
.ai-gen-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 8px;
  background: rgba(168,85,247,0.1);
  border: 1px solid rgba(168,85,247,0.2);
  color: var(--primary);
  font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.25s;
}
.ai-gen-btn:hover {
  background: rgba(168,85,247,0.2);
  border-color: rgba(168,85,247,0.35);
  transform: scale(1.03);
}
.ai-loading {
  display: flex; align-items: center; gap: 12px;
  color: var(--text-dim); font-size: 14px;
}
.ai-dots { display: flex; gap: 4px; }
.ai-dots span {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--primary);
  animation: aiPulse 1.2s ease-in-out infinite;
}
.ai-dots span:nth-child(2) { animation-delay: 0.2s; }
.ai-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes aiPulse { 0%, 100% { opacity: 0.2; transform: scale(0.8); } 50% { opacity: 1; transform: scale(1.2); } }
.ai-summary-text {
  font-size: 15px; line-height: 1.8;
  color: var(--text-muted);
  border-left: 3px solid var(--primary);
  padding-left: 16px; margin: 0;
}
.ai-error {
  font-size: 14px; color: var(--accent-rose);
  margin: 0;
}

/* ===== Toolbar Timer ===== */
.toolbar-timer {
  display: flex; align-items: center; gap: 4px;
  font-size: 11px; font-weight: 600;
  color: var(--text-dim);
  padding: 4px 0;
  font-family: var(--font-mono);
}

/* ===== Floating Toolbar ===== */
.floating-toolbar {
  position: fixed; bottom: 40px; left: 50%; transform: translateX(-50%);
  display: flex; flex-direction: row; align-items: center; gap: 16px;
  padding: 12px 24px; border-radius: 99px;
  z-index: 100;
  box-shadow: var(--card-shadow);
  background: var(--bg-elevated); backdrop-filter: blur(24px);
  border: 1px solid var(--border);
}
.toolbar-btn {
  display: flex; flex-direction: row; align-items: center; justify-content: center; gap: 8px;
  height: 40px; border: none; background: transparent; padding: 0 12px;
  color: var(--text-dim); cursor: pointer;
  border-radius: 20px; transition: all 0.25s;
  font-size: 13px; font-family: var(--font-body); font-weight: 600;
}
.toolbar-btn:hover { background: rgba(168,85,247,0.15); color: var(--primary); transform: translateY(-2px); }
.toolbar-btn.active { color: var(--accent-rose); background: rgba(244,63,94,0.1); }
.toolbar-btn span { font-size: 13px; }
.toolbar-divider { width: 1px; height: 24px; background: var(--border); margin: 0 4px; }

/* Transitions */
.slide-in-enter-active, .slide-in-leave-active { transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-in-enter-from, .slide-in-leave-to { opacity: 0; transform: translate(-50%, 60px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ===== Layout ===== */
.article-layout {
  display: flex; gap: 40px; align-items: flex-start; justify-content: center;
  max-width: 1400px; margin: 0 auto; padding: 0 24px;
}
.article-main { flex: 1; max-width: 720px; min-width: 0; margin: 0; }

/* ===== Header ===== */
.article-header { margin-bottom: 40px; text-align: center; }
.article-tags { display: flex; gap: 8px; justify-content: center; flex-wrap: wrap; margin-bottom: 24px; }
.tag {
  padding: 4px 14px; border-radius: 6px; font-size: 12px; font-weight: 500;
  background: color-mix(in srgb, var(--tag-color) 12%, transparent);
  color: var(--tag-color);
  border: 1px solid color-mix(in srgb, var(--tag-color) 20%, transparent);
  text-decoration: none; transition: all 0.2s;
}
.tag:hover { background: var(--tag-color); color: #fff; }
.category-badge {
  padding: 4px 14px; border-radius: 6px; font-size: 12px; font-weight: 600;
  background: rgba(16,185,129,0.12); color: var(--accent-green);
  border: 1px solid rgba(16,185,129,0.2);
}
.article-title {
  font-family: var(--font-heading);
  font-size: 56px; font-weight: 900;
  line-height: 1.2; letter-spacing: -0.02em;
  margin-bottom: 24px;
  background: linear-gradient(135deg, var(--text-primary), var(--text-muted) 40%, var(--primary));
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
  color: var(--text-primary);
}
.article-meta {
  display: flex; align-items: center; gap: 10px;
  color: var(--text-dim); font-size: 14px; font-weight: 500;
  flex-wrap: wrap; justify-content: center;
}
.author-avatar { width: 36px; height: 36px; border-radius: 50%; border: 2px solid rgba(168,85,247,0.3); }
.author-name { font-weight: 700; color: var(--text-primary); }
.meta-dot { opacity: 0.3; }

/* ===== Cover ===== */
.article-cover {
  margin-bottom: 48px;
  border-radius: 20px; overflow: hidden;
  box-shadow: 0 20px 60px rgba(0,0,0,0.4);
}
.article-cover img { width: 100%; display: block; cursor: zoom-in; transition: transform 0.5s; }
.article-cover:hover img { transform: scale(1.02); }

/* ===== Content — 720px golden reading width ===== */
.article-content {
  font-size: 17px;
  line-height: 1.9;
  color: #e2e8f0;
}
[data-theme="light"] .article-content { color: #334155; }
.article-content :deep(p) { margin-bottom: 1.8em; }
.article-content :deep(ul), .article-content :deep(ol) { margin-bottom: 1.8em; padding-left: 1.5em; }
.article-content :deep(li) { margin-bottom: 0.5em; }

/* h2/h3 with left purple-cyan line */
.article-content :deep(h2),
.article-content :deep(h3) {
  position: relative;
  padding-left: 20px;
  margin: 2em 0 0.8em;
}
.article-content :deep(h2)::before,
.article-content :deep(h3)::before {
  content: '';
  position: absolute; left: 0; top: 4px; bottom: 4px;
  width: 4px;
  background: linear-gradient(180deg, #a855f7, #22d3ee);
  border-radius: 4px;
  box-shadow: 0 0 12px rgba(168,85,247,0.4);
}
.article-content :deep(h2) {
  font-family: var(--font-heading);
  font-size: 1.8em; font-weight: 800;
  color: var(--text-primary); letter-spacing: -0.01em;
}
.article-content :deep(h3) {
  font-family: var(--font-heading);
  font-size: 1.4em; font-weight: 700;
  color: var(--text-primary);
}

/* Blockquotes */
.article-content :deep(blockquote) {
  border-left: 4px solid #a855f7;
  padding: 16px 24px;
  margin: 2em 0;
  background: rgba(168,85,247,0.06);
  border-radius: 0 16px 16px 0;
  color: var(--text-muted); font-size: 16px;
  font-style: italic; line-height: 1.8;
}

/* Images: 20px radius + shadow */
.article-content :deep(img) {
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.3);
  margin: 2em 0; max-width: 100%;
}

/* Code blocks: glass background */
.article-content :deep(pre) {
  background: var(--card-bg);
  backdrop-filter: blur(12px);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  margin: 2em 0;
  box-shadow: inset 0 0 20px rgba(0,0,0,0.1);
}

/* Copy button */
:deep(.copy-btn) {
  position: absolute; top: 12px; right: 12px;
  padding: 6px 16px; border-radius: 8px; font-size: 12px;
  background: rgba(255,255,255,0.1); color: #fff;
  border: none; cursor: pointer;
  font-weight: 600; transition: all 0.2s;
  backdrop-filter: blur(4px);
}
:deep(.copy-btn:hover) { background: rgba(168,85,247,0.4); }
:deep(.copy-btn.copied) { background: rgba(16,185,129,0.3); color: #10b981; }

/* ===== Selection Share ===== */
.selection-share-tooltip {
  position: absolute; z-index: 100;
  display: flex; gap: 4px; background: var(--bg-elevated);
  padding: 8px; border-radius: 12px; border: 1px solid var(--border);
  box-shadow: var(--shadow);
}
.selection-share-tooltip button {
  background: none; border: none; color: var(--text-primary); cursor: pointer;
  font-size: 16px; padding: 6px 10px; border-radius: 6px; transition: background 0.2s;
}
.selection-share-tooltip button:hover { background: rgba(168,85,247,0.15); color: var(--primary); }

/* ===== Prev/Next Nav ===== */
.article-nav { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 64px 0 48px; }
.nav-card { padding: 32px; text-decoration: none; color: inherit; border-radius: 20px; transition: transform 0.3s, border-color 0.3s; }
.nav-card:hover { transform: translateY(-4px); border-color: rgba(168,85,247,0.3); }
.nav-card.next { text-align: right; }
.nav-label { font-size: 13px; color: var(--text-dim); text-transform: uppercase; font-weight: 700; letter-spacing: 0.1em; }
.nav-title { display: block; font-family: var(--font-heading); font-size: 18px; color: var(--text-primary); margin-top: 10px; font-weight: 700; line-height: 1.4; }

/* ===== Related — Horizontal Snap Scroll ===== */
.related-section { margin: 64px 0 48px; }
.related-section h2 { font-family: var(--font-heading); font-size: 28px; color: var(--text-primary); margin-bottom: 24px; font-weight: 800; }
.related-scroll {
  display: flex; gap: 24px; overflow-x: auto;
  scroll-snap-type: x mandatory; -webkit-overflow-scrolling: touch;
  padding-bottom: 24px; padding-top: 12px;
  scrollbar-width: none; /* Firefox */
}
.related-scroll::-webkit-scrollbar { display: none; }
.related-card {
  min-width: 280px; max-width: 320px; flex-shrink: 0;
  scroll-snap-align: start; overflow: hidden;
  text-decoration: none; color: inherit; border-radius: 20px;
  transition: transform 0.3s;
}
.related-card:hover { transform: translateY(-8px); box-shadow: 0 20px 40px rgba(0,0,0,0.3), 0 0 20px rgba(168,85,247,0.1); border-color: rgba(168,85,247,0.2); }
.related-cover { height: 160px; background-size: cover; background-position: center; }
.related-body { padding: 20px; }
.related-body h4 { font-family: var(--font-heading); font-size: 16px; color: var(--text-primary); margin-bottom: 8px; font-weight: 700; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { font-size: 13px; color: var(--text-dim); font-weight: 500; }

/* ===== Subscribe CTA ===== */
.subscribe-cta { padding: 48px; text-align: center; margin: 48px 0; position: relative; overflow: hidden; }
.subscribe-cta h3 { font-family: var(--font-heading); font-size: 26px; margin-bottom: 10px; }
.subscribe-cta > p { color: var(--text-muted); margin-bottom: 20px; }
.cta-form { display: flex; gap: 12px; max-width: 420px; margin: 0 auto; }
.cta-form input {
  flex: 1; padding: 14px 20px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: var(--bg-hover); color: var(--text-primary);
  font-size: 15px; outline: none; transition: all 0.3s;
}
.cta-form input:focus { border-color: var(--primary); background: var(--bg-elevated); box-shadow: 0 0 20px rgba(168,85,247,0.15); }
.success-msg { color: var(--accent-green); margin-top: 12px; }

/* ===== TOC Sidebar — Glass Fixed Panel ===== */
.toc-sidebar { width: 260px; flex-shrink: 0; position: sticky; top: 88px; }
.toc-card { padding: 0; overflow: hidden; }
.toc-header {
  padding: 16px 20px; font-family: var(--font-heading);
  font-size: 14px; font-weight: 700; color: var(--text-heading);
  border-bottom: 1px solid var(--border);
  letter-spacing: -0.01em;
}
.toc-nav { padding: 8px 0; max-height: 60vh; overflow-y: auto; }
.toc-nav a {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 16px; font-size: 13px; color: var(--text-dim);
  text-decoration: none; transition: all 0.25s;
  border-left: 2px solid transparent;
  position: relative;
}
.toc-nav a:hover { color: var(--primary); background: rgba(168,85,247,0.04); }
.toc-nav a.active {
  color: var(--primary); font-weight: 600;
  border-left-color: var(--primary);
  background: rgba(168,85,247,0.06);
}
.toc-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--text-dim); flex-shrink: 0;
  transition: all 0.3s;
}
.toc-nav a.active .toc-dot {
  background: var(--primary);
  box-shadow: 0 0 8px rgba(168,85,247,0.6), 0 0 16px rgba(34,211,238,0.3);
  width: 8px; height: 8px;
}

/* Transitions */
.slide-in-enter-active, .slide-in-leave-active { transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-in-enter-from, .slide-in-leave-to { opacity: 0; transform: translateX(-50%) translateY(20px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ===== Lightbox ===== */
.lightbox {
  position: fixed; inset: 0; background: rgba(0,0,0,0.92); z-index: 10000;
  display: flex; align-items: center; justify-content: center; cursor: zoom-out;
  backdrop-filter: blur(8px);
}
.lightbox img { max-width: 90vw; max-height: 90vh; border-radius: var(--radius-sm); }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .toc-sidebar { display: none; }
  .article-title { font-size: 36px; }
}
@media (max-width: 768px) {
  .article-layout { padding: 0 16px; }
  .article-title { font-size: 30px; }
  .article-nav { grid-template-columns: 1fr; }
  .cta-form { flex-direction: column; }
  .floating-toolbar { bottom: 16px; }
}
@media (max-width: 480px) {
  .article-title { font-size: 26px; }
}
</style>
