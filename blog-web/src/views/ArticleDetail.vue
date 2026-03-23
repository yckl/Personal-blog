<template>
  <div v-if="article">
    <!-- Reading Progress Bar -->
    <div class="reading-progress" :style="{ width: readingProgress + '%' }" />

    <div class="container article-detail">
      <div class="article-layout">
        <!-- Main Content -->
        <article class="article-main fade-in">
          <!-- Header -->
          <header class="article-header">
            <div class="article-tags">
              <router-link v-for="tag in (article.tags||[])" :key="tag.id" :to="`/tag/${tag.id}`" class="tag"
                :style="{ '--tag-color': tag.color||'#6366f1' }">{{ tag.name }}</router-link>
              <span v-if="article.categoryName" class="category-badge">{{ article.categoryName }}</span>
            </div>
            <h1>{{ article.title }}</h1>
            <div class="article-meta">
              <img v-if="article.authorAvatar" :src="article.authorAvatar" class="author-avatar" />
              <span>{{ article.authorName }}</span>
              <span>·</span>
              <span>{{ formatDate(article.publishedAt || article.createdAt) }}</span>
              <span v-if="article.updatedAt && article.updatedAt !== article.publishedAt">· Updated {{ formatDate(article.updatedAt) }}</span>
              <span>·</span>
              <span>🕐 {{ article.readingTime || Math.max(1, Math.round((article.wordCount||0)/200)) }} min read</span>
              <span>·</span>
              <span>👁 {{ article.viewCount }}</span>
            </div>
          </header>

          <!-- Cover -->
          <div v-if="article.coverImage" class="article-cover">
            <img :src="article.coverImage" :alt="article.title" @click="openLightbox(article.coverImage)" />
          </div>

          <!-- Content -->
          <div ref="contentRef" class="article-content" v-html="article.contentHtml || article.contentMd" />

          <!-- Text Selection Share Tooltip -->
          <div v-if="selectionTooltip.visible" class="selection-share-tooltip" :style="{ top: selectionTooltip.y + 'px', left: selectionTooltip.x + 'px' }">
            <button @click="shareSelectedText('twitter')" title="Share on X">𝕏</button>
            <button @click="shareSelectedText('copy')" title="Copy quote">📋</button>
          </div>

          <ArticleActions v-if="article"
            :articleId="article.id"
            :articleTitle="article.title"
            :articleUrl="currentUrl" />

          <!-- Prev / Next -->
          <div class="article-nav" v-if="nav.prev || nav.next">
            <router-link v-if="nav.prev" :to="`/article/${nav.prev.slug}`" class="nav-card prev">
              <span class="nav-label">← Previous</span>
              <span class="nav-title">{{ nav.prev.title }}</span>
            </router-link>
            <div v-else />
            <router-link v-if="nav.next" :to="`/article/${nav.next.slug}`" class="nav-card next">
              <span class="nav-label">Next →</span>
              <span class="nav-title">{{ nav.next.title }}</span>
            </router-link>
          </div>

          <!-- Related Articles -->
          <section class="related-section" v-if="relatedArticles.length">
            <h2>📖 Related Articles</h2>
            <div class="related-grid">
              <router-link v-for="r in relatedArticles" :key="r.id" :to="`/article/${r.slug}`" class="related-card">
                <div class="related-cover" :style="{ backgroundImage: r.coverImage ? `url(${r.coverImage})` : 'var(--gradient-1)' }" />
                <div class="related-body">
                  <h4>{{ r.title }}</h4>
                  <span class="card-meta">{{ formatDate(r.publishedAt) }} · 👁 {{ r.viewCount }}</span>
                </div>
              </router-link>
            </div>
          </section>

          <!-- Subscribe CTA -->
          <section class="subscribe-cta fade-in">
            <h3>📬 Enjoyed this article?</h3>
            <p>Subscribe to get notified about new posts. No spam, unsubscribe anytime.</p>
            <form class="cta-form" @submit.prevent="handleSubscribe">
              <input v-model="subEmail" type="email" placeholder="your@email.com" required />
              <button type="submit" class="btn btn-primary" :disabled="subscribing">{{ subscribing ? 'Subscribing...' : 'Subscribe' }}</button>
            </form>
            <p v-if="subscribeSuccess" style="color: var(--accent-green); margin-top: 8px;">✅ Subscribed!<//p>
          </section>

          <!-- Comments -->
          <CommentSection v-if="article.allowComment !== false" :articleId="article.id" />
        </article>

        <!-- TOC Sidebar -->
        <aside class="toc-sidebar" v-if="tocItems.length">
          <div class="toc-card" :class="{ 'toc-collapsed': tocCollapsed }">
            <div class="toc-header" @click="tocCollapsed = !tocCollapsed">
              <span>📑 Table of Contents</span>
              <span class="toc-toggle">{{ tocCollapsed ? '▼' : '▶' }}</span>
            </div>
            <nav v-show="!tocCollapsed" class="toc-nav">
              <a v-for="(item, idx) in tocItems" :key="idx" :href="'#' + item.id"
                :class="{ active: activeTocId === item.id }"
                :style="{ paddingLeft: (item.level - 1) * 16 + 12 + 'px' }"
                @click.prevent="scrollToHeading(item.id)">
                {{ item.text }}
              </a>
            </nav>
          </div>
        </aside>
      </div>
    </div>

    <!-- Lightbox -->
    <div v-if="lightboxSrc" class="lightbox" @click="lightboxSrc = ''">
      <img :src="lightboxSrc" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getArticleBySlug, recordVisit, subscribe } from '../api'
import request from '../utils/request'
import CommentSection from '../components/CommentSection.vue'
// @ts-ignore
import ArticleActions from '../components/ArticleActions.vue'

const route = useRoute()
const article = ref<any>(null)

const contentRef = ref<HTMLElement>()

// TOC
const tocItems = ref<{ id: string; text: string; level: number }[]>([])
const activeTocId = ref('')
const tocCollapsed = ref(false)

// Progress
const readingProgress = ref(0)

// Nav
const nav = ref<any>({})
const relatedArticles = ref<any[]>([])

// Lightbox
const lightboxSrc = ref('')

// Text selection share
const selectionTooltip = reactive({ visible: false, x: 0, y: 0, text: '' })

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

// Subscribe
const subEmail = ref('')
const subscribing = ref(false)
const subscribeSuccess = ref(false)
const currentUrl = ref('')

// SEO helpers
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
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

async function loadArticle(slug: string) {
  const res: any = await getArticleBySlug(slug)
  article.value = res.data
  currentUrl.value = window.location.href

  // SEO meta injection
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

  // JSON-LD Structured Data (BlogPosting)
  injectJsonLd({
    '@context': 'https://schema.org',
    '@type': 'BlogPosting',
    headline: a.title,
    description: a.metaDescription || a.excerpt || '',
    image: a.ogImage || a.coverImage || '',
    datePublished: a.publishedAt,
    dateModified: a.updatedAt || a.publishedAt,
    author: { '@type': 'Person', name: a.authorName || 'Blog Author' },
    url: window.location.href
  })

  // Record visit
  // Record visit with visitor ID for UV tracking
  const visitorId = localStorage.getItem('visitor_id') || (() => {
    const id = crypto.randomUUID(); localStorage.setItem('visitor_id', id); return id
  })()
  recordVisit({ articleId: res.data.id, pageUrl: window.location.href, visitorId, pageType: 'article' }).catch(() => {})


  // Load nav
  try {
    const nRes: any = await request.get(`/api/public/articles/${res.data.id}/nav`)
    nav.value = nRes.data || {}
  } catch { nav.value = {} }

  // Load related (advanced recommendation)
  try {
    const rRes: any = await request.get(`/api/public/recommend/articles/${res.data.id}`, { params: { scene: 'DETAIL', size: 6 } })
    relatedArticles.value = rRes.data || []
    // Track exposure
    for (const r of relatedArticles.value) {
      request.post('/api/public/recommend/exposure', {
        articleId: res.data.id, recommendedArticleId: r.id, scene: 'DETAIL'
      }).catch(() => {})
    }
  } catch { relatedArticles.value = [] }

  // Parse TOC
  await nextTick()
  parseToc()
  addCodeCopyButtons()
  addImageClickHandlers()

  // I1: Image lazy loading + I2: alt text accessibility
  if (contentRef.value) {
    contentRef.value.querySelectorAll('img').forEach((img: HTMLImageElement) => {
      img.loading = 'lazy'
      if (!img.alt) img.alt = 'Article image'
    })
  }
}

function parseToc() {
  // Try from tocJson first
  if (article.value?.tocJson) {
    try {
      tocItems.value = JSON.parse(article.value.tocJson)
      return
    } catch {}
  }
  // Fallback: parse from DOM
  if (!contentRef.value) return
  const headings = contentRef.value.querySelectorAll('h1, h2, h3, h4')
  const items: { id: string; text: string; level: number }[] = []
  headings.forEach((h, idx) => {
    const id = h.id || 'heading-' + idx
    if (!h.id) h.id = id
    items.push({ id, text: h.textContent || '', level: parseInt(h.tagName.substring(1)) })
  })
  tocItems.value = items
}

function addCodeCopyButtons() {
  if (!contentRef.value) return
  contentRef.value.querySelectorAll('pre').forEach(pre => {
    if (pre.querySelector('.copy-btn')) return
    const btn = document.createElement('button')
    btn.className = 'copy-btn'
    btn.textContent = '📋 Copy'
    btn.onclick = () => {
      const code = pre.querySelector('code')?.textContent || pre.textContent || ''
      navigator.clipboard.writeText(code)
      btn.textContent = '✅ Copied!'
      setTimeout(() => btn.textContent = '📋 Copy', 2000)
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

function scrollToHeading(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleScroll() {
  // Progress
  const scrollTop = window.scrollY
  const docHeight = document.documentElement.scrollHeight - window.innerHeight
  readingProgress.value = docHeight > 0 ? Math.min(100, (scrollTop / docHeight) * 100) : 0

  // Active TOC
  if (!tocItems.value.length) return
  let activeId = tocItems.value[0]?.id || ''
  for (const item of tocItems.value) {
    const el = document.getElementById(item.id)
    if (el && el.getBoundingClientRect().top <= 100) {
      activeId = item.id
    }
  }
  activeTocId.value = activeId
}





async function handleSubscribe() {
  subscribing.value = true
  try { await subscribe({ email: subEmail.value }); subscribeSuccess.value = true; subEmail.value = '' }
  catch { alert('Subscribe failed') }
  finally { subscribing.value = false }
}

onMounted(() => {
  loadArticle(route.params.slug as string)
  window.addEventListener('scroll', handleScroll)
  document.addEventListener('mouseup', handleTextSelection)
  document.addEventListener('mousedown', () => { selectionTooltip.visible = false })
})
onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  document.removeEventListener('mouseup', handleTextSelection)
  const jsonLd = document.querySelector('script#json-ld-article')
  if (jsonLd) jsonLd.remove()
})
watch(() => route.params.slug, (slug) => { if (slug) loadArticle(slug as string) })
</script>

<style scoped>
/* Selection Share Tooltip */
.selection-share-tooltip { position: absolute; z-index: 100; display: flex; gap: 4px; background: #1e1e3a; border: 1px solid rgba(99,102,241,.4); border-radius: 8px; padding: 6px; box-shadow: 0 4px 12px rgba(0,0,0,.4); }
.selection-share-tooltip button { background: none; border: none; color: #fff; cursor: pointer; font-size: 16px; padding: 4px 8px; border-radius: 4px; }
.selection-share-tooltip button:hover { background: rgba(99,102,241,.3); }
/* Progress Bar */
.reading-progress { position: fixed; top: 0; left: 0; height: 3px; background: var(--gradient-1); z-index: 9999; transition: width 0.1s linear; }

/* Layout */
.article-layout { display: flex; gap: 32px; align-items: flex-start; }
.article-main { flex: 1; min-width: 0; }

/* Header */
.article-header { margin-bottom: 32px; }
.article-header h1 { font-size: 36px; font-weight: 800; color: var(--text-heading); line-height: 1.3; margin: 12px 0 16px; }
.article-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.tag { padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 500; background: color-mix(in srgb, var(--tag-color) 15%, transparent); color: var(--tag-color); text-decoration: none; transition: opacity 0.2s; }
.tag:hover { opacity: 0.8; }
.category-badge { padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 600; background: rgba(16,185,129,0.15); color: var(--accent-green); }
.article-meta { display: flex; align-items: center; gap: 10px; color: var(--text-dim); font-size: 14px; flex-wrap: wrap; }
.author-avatar { width: 28px; height: 28px; border-radius: 50%; }

/* Cover */
.article-cover { margin-bottom: 32px; border-radius: var(--radius); overflow: hidden; }
.article-cover img { width: 100%; display: block; cursor: zoom-in; }

/* Actions */
.article-actions { display: flex; gap: 12px; padding: 24px 0; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); margin: 32px 0; }
.action-btn { padding: 10px 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; color: var(--text); cursor: pointer; font-size: 14px; transition: all .2s; }
.action-btn:hover { border-color: var(--primary); background: var(--bg-hover); }
.action-btn.liked { border-color: var(--accent-rose); color: var(--accent-rose); }

/* Prev/Next Nav */
.article-nav { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 32px 0; }
.nav-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 20px; text-decoration: none; color: inherit; transition: all 0.3s; }
.nav-card:hover { border-color: var(--primary); transform: translateY(-2px); }
.nav-card.next { text-align: right; }
.nav-label { font-size: 12px; color: var(--text-dim); text-transform: uppercase; font-weight: 600; }
.nav-title { display: block; font-size: 15px; color: var(--text-heading); margin-top: 6px; font-weight: 600; }

/* Related */
.related-section { margin: 32px 0; }
.related-section h2 { font-size: 22px; font-weight: 700; color: var(--text-heading); margin-bottom: 16px; }
.related-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.related-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; text-decoration: none; color: inherit; transition: all 0.3s; }
.related-card:hover { transform: translateY(-3px); border-color: var(--primary); }
.related-cover { height: 120px; background-size: cover; background-position: center; }
.related-body { padding: 14px; }
.related-body h4 { font-size: 14px; color: var(--text-heading); margin-bottom: 6px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { font-size: 12px; color: var(--text-dim); }

/* Subscribe CTA */
.subscribe-cta { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 32px; text-align: center; margin: 32px 0; }
.subscribe-cta h3 { font-size: 22px; color: var(--text-heading); margin-bottom: 8px; }
.subscribe-cta > p { color: var(--text-muted); margin-bottom: 16px; }
.cta-form { display: flex; gap: 12px; max-width: 420px; margin: 0 auto; }
.cta-form input { flex: 1; padding: 12px 16px; border-radius: 10px; border: 1px solid var(--border); background: var(--bg); color: var(--text); font-size: 15px; outline: none; }
.cta-form input:focus { border-color: var(--primary); }

/* TOC */
.toc-sidebar { width: 260px; flex-shrink: 0; position: sticky; top: 24px; }
.toc-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
.toc-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; font-size: 14px; font-weight: 600; color: var(--text-heading); cursor: pointer; border-bottom: 1px solid var(--border); }
.toc-toggle { font-size: 12px; color: var(--text-dim); }
.toc-nav { padding: 8px 0; max-height: 60vh; overflow-y: auto; }
.toc-nav a { display: block; padding: 6px 12px; font-size: 13px; color: var(--text-muted); text-decoration: none; transition: all 0.2s; border-left: 2px solid transparent; }
.toc-nav a:hover { color: var(--primary-light); background: rgba(99,102,241,0.05); }
.toc-nav a.active { color: var(--primary); border-left-color: var(--primary); font-weight: 600; }

/* Comments */
.comments-section { margin-top: 48px; }
.comments-section h2 { font-size: 24px; font-weight: 700; color: var(--text-heading); margin-bottom: 24px; }
.comment-form { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 24px; margin-bottom: 32px; }
.comment-form-row { display: flex; gap: 12px; margin-bottom: 12px; }
.comment-form input, .comment-form textarea { width: 100%; padding: 12px; background: var(--bg); border: 1px solid var(--border); border-radius: 8px; color: var(--text); font-size: 14px; outline: none; font-family: inherit; }
.comment-form input:focus, .comment-form textarea:focus { border-color: var(--primary); }
.comment-form textarea { margin-bottom: 12px; resize: vertical; }
.btn { padding: 10px 24px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; border: none; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-dark); }
.comment-list { display: flex; flex-direction: column; gap: 16px; }
.comment-item { display: flex; gap: 12px; }
.comment-avatar { width: 40px; height: 40px; border-radius: 50%; flex-shrink: 0; }
.comment-body { flex: 1; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 16px; }
.comment-meta { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 13px; }
.comment-meta strong { color: var(--text-heading); }
.comment-meta span { color: var(--text-dim); }
.comment-body p { color: var(--text-muted); font-size: 14px; }
.comment-replies { margin-top: 12px; padding-left: 16px; border-left: 2px solid var(--border); }
.reply { margin-top: 8px; }

/* Lightbox */
.lightbox { position: fixed; inset: 0; background: rgba(0,0,0,0.9); z-index: 10000; display: flex; align-items: center; justify-content: center; cursor: zoom-out; }
.lightbox img { max-width: 90vw; max-height: 90vh; border-radius: 8px; }

/* Code copy button */
:deep(.copy-btn) {
  position: absolute; top: 8px; right: 8px;
  padding: 4px 12px; border-radius: 6px; font-size: 12px;
  background: rgba(255,255,255,0.1); color: #fff; border: 1px solid rgba(255,255,255,0.2);
  cursor: pointer; transition: all 0.2s;
}
:deep(.copy-btn:hover) { background: rgba(255,255,255,0.2); }

/* Responsive */
@media (max-width: 1024px) {
  .toc-sidebar { display: none; }
}
@media (max-width: 768px) {
  .article-header h1 { font-size: 28px; }
  .article-nav { grid-template-columns: 1fr; }
  .related-grid { grid-template-columns: 1fr; }
  .cta-form { flex-direction: column; }
  .comment-form-row { flex-direction: column; }
}
</style>
