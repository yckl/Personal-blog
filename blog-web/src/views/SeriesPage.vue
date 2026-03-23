<template>
  <div class="series-page" v-if="series">
    <!-- ===== Series Banner ===== -->
    <section class="series-banner fade-in"
             :style="{ backgroundImage: series.coverImage ? `url(${series.coverImage})` : 'none' }">
      <div class="banner-overlay">
        <div class="banner-content">
          <span class="banner-badge glass">📚 Series</span>
          <h1 class="banner-title">{{ series.name }}</h1>
          <p v-if="series.description" class="banner-desc">{{ series.description }}</p>
          <div class="banner-stats">
            <span>{{ articles.length }} articles</span>
            <span class="stat-dot">·</span>
            <span>{{ totalWords }} words total</span>
            <span class="stat-dot">·</span>
            <span>{{ totalReadTime }} min read</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== Progress Bar ===== -->
    <div class="progress-section fade-in">
      <div class="progress-bar-wrap glass">
        <div class="progress-info">
          <span class="progress-label">Series Progress</span>
          <span class="progress-value">{{ readCount }} / {{ articles.length }} articles read</span>
        </div>
        <div class="progress-track">
          <div class="progress-fill" :style="{ width: progressPercent + '%' }" />
        </div>
      </div>
    </div>

    <!-- ===== Article List ===== -->
    <div class="series-list">
      <div v-for="(article, idx) in articles" :key="article.id" class="series-item fade-in">
        <!-- Sequence Number -->
        <div class="seq-number">
          <span class="seq-text gradient-text">{{ String(idx + 1).padStart(2, '0') }}</span>
          <div class="seq-line" v-if="idx < articles.length - 1" />
        </div>

        <!-- Article Card -->
        <router-link :to="`/article/${article.slug}`"
                     :class="['series-card glass-card', { read: isRead(article.id) }]">
          <div class="card-content">
            <div class="card-head">
              <h3>{{ article.title }}</h3>
              <span v-if="isRead(article.id)" class="read-badge">✓ Read</span>
            </div>
            <p class="card-excerpt">{{ article.excerpt || 'Read this article...' }}</p>
            <div class="card-meta">
              <span>{{ formatDate(article.publishedAt) }}</span>
              <span class="meta-dot">·</span>
              <span>{{ article.readingTime || Math.max(1, Math.round((article.wordCount||0)/200)) }} min</span>
              <span class="meta-dot">·</span>
              <span>{{ article.viewCount || 0 }} views</span>
            </div>
          </div>
          <div v-if="article.coverImage" class="card-cover"
               :style="{ backgroundImage: `url(${article.coverImage})` }" />
        </router-link>
      </div>
    </div>

    <!-- Empty -->
    <p v-if="!articles.length" class="empty-state">No articles in this series yet.</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const series = ref<any>(null)
const articles = ref<any[]>([])

// Track read articles from localStorage
const readArticles = ref<Set<number>>(new Set())

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

function isRead(articleId: number): boolean {
  return readArticles.value.has(articleId)
}

const totalWords = computed(() => {
  const total = articles.value.reduce((s, a) => s + (a.wordCount || 0), 0)
  if (total >= 1000) return (total / 1000).toFixed(1) + 'K'
  return total.toString()
})

const totalReadTime = computed(() => {
  return articles.value.reduce((s, a) => s + (a.readingTime || Math.max(1, Math.round((a.wordCount || 0) / 200))), 0)
})

const readCount = computed(() => {
  return articles.value.filter(a => readArticles.value.has(a.id)).length
})

const progressPercent = computed(() => {
  if (!articles.value.length) return 0
  return Math.round((readCount.value / articles.value.length) * 100)
})

onMounted(async () => {
  // Load read history
  try {
    const stored = localStorage.getItem('read_articles')
    if (stored) readArticles.value = new Set(JSON.parse(stored))
  } catch {}

  try {
    const slug = route.params.slug as string
    const res: any = await request.get(`/api/public/series/${slug}`)
    series.value = res.data
    articles.value = res.data?.articles || []
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.series-page { max-width: 1000px; margin: 0 auto; padding: 0 24px 80px; }

/* ===== Banner ===== */
.series-banner {
  min-height: 360px; border-radius: 24px; overflow: hidden;
  position: relative;
  background-size: cover; background-position: center;
  margin-bottom: 48px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.4);
}
.banner-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(15,23,42,0.95) 10%, rgba(15,23,42,0.4) 100%);
  display: flex; align-items: flex-end;
  padding: 56px 48px;
}
.series-banner:not([style*="url"]) .banner-overlay {
  background: linear-gradient(135deg, rgba(168,85,247,0.8), rgba(34,211,238,0.8));
}
.series-banner:not([style*="url"]) .banner-overlay::after {
  content: ''; position: absolute; inset: 0; background: rgba(15,23,42,0.5);
}
.banner-content { position: relative; z-index: 1; }
.banner-badge {
  display: inline-block; padding: 6px 16px;
  font-size: 13px; font-weight: 700; color: #fff;
  border-radius: 999px; margin-bottom: 20px;
  background: rgba(255,255,255,0.1); backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,0.2); letter-spacing: 0.05em; text-transform: uppercase;
}
.banner-title {
  font-family: var(--font-heading);
  font-size: 48px; font-weight: 900; color: #fff;
  letter-spacing: -0.02em; margin-bottom: 16px;
  line-height: 1.15; text-shadow: 0 4px 20px rgba(0,0,0,0.5);
}
.banner-desc { color: rgba(255,255,255,0.8); font-size: 17px; max-width: 600px; margin-bottom: 20px; line-height: 1.7; text-shadow: 0 2px 10px rgba(0,0,0,0.5); }
.banner-stats { display: flex; gap: 12px; color: rgba(255,255,255,0.6); font-size: 14px; font-weight: 500; }
.stat-dot { opacity: 0.4; }

/* ===== Progress Bar ===== */
.progress-section { margin-bottom: 48px; }
.progress-bar-wrap {
  padding: 24px 32px; border-radius: 20px;
  background: rgba(15,23,42,0.4); backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: 0 10px 30px rgba(0,0,0,0.3);
}
.progress-info { display: flex; justify-content: space-between; margin-bottom: 12px; }
.progress-label { font-family: var(--font-heading); font-size: 16px; font-weight: 700; color: #f8fafc; letter-spacing: 0.02em; }
.progress-value { font-size: 14px; color: var(--text-dim); font-weight: 500; }
.progress-track {
  height: 8px; border-radius: 4px;
  background: rgba(0,0,0,0.3);
  overflow: hidden; inset: inset 0 1px 3px rgba(0,0,0,0.2);
}
.progress-fill {
  height: 100%; border-radius: 4px;
  background: linear-gradient(90deg, #a855f7, #22d3ee);
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 0 16px rgba(168,85,247,0.6);
}

/* ===== Series List ===== */
.series-list { display: flex; flex-direction: column; gap: 0; }
.series-item { display: flex; gap: 32px; align-items: stretch; }

/* Sequence Number */
.seq-number {
  display: flex; flex-direction: column; align-items: center;
  width: 72px; flex-shrink: 0; padding-top: 32px;
}
.seq-text {
  font-family: var(--font-heading);
  font-size: 36px; font-weight: 900;
  letter-spacing: -0.04em;
  background: linear-gradient(180deg, #f8fafc, rgba(248,250,252,0.2));
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.seq-line {
  flex: 1; width: 2px; margin-top: 12px;
  background: linear-gradient(180deg, rgba(168,85,247,0.5), transparent);
}

/* Article Card */
.series-card {
  flex: 1; display: flex; gap: 24px;
  padding: 32px; margin-bottom: 24px;
  text-decoration: none; color: inherit;
  min-height: 180px; border-radius: 20px;
  background: rgba(15,23,42,0.4); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.05);
  box-shadow: 0 4px 20px rgba(0,0,0,0.2);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.series-card:hover {
  transform: translateX(8px) translateY(-2px);
  border-color: rgba(168,85,247,0.3);
  background: rgba(15,23,42,0.6);
  box-shadow: 0 12px 30px rgba(0,0,0,0.3), 0 0 20px rgba(168,85,247,0.15);
}
.series-card.read { border-color: rgba(16,185,129,0.3); background: rgba(15,23,42,0.2); }
.card-content { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.card-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 12px; }
.card-head h3 {
  font-family: var(--font-heading);
  font-size: 22px; font-weight: 800; color: #f8fafc;
  letter-spacing: -0.01em; line-height: 1.35;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.read-badge {
  font-size: 11px; font-weight: 700; color: #10b981;
  background: rgba(16,185,129,0.15); padding: 4px 12px;
  border-radius: 999px; flex-shrink: 0;
  border: 1px solid rgba(16,185,129,0.3); text-transform: uppercase; letter-spacing: 0.05em;
}
.card-excerpt {
  font-size: 15px; color: var(--text-muted); flex: 1;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  line-height: 1.7;
}
.card-meta {
  display: flex; gap: 10px; font-size: 13px; color: var(--text-dim); margin-top: 16px; font-weight: 500;
}
.meta-dot { opacity: 0.4; }
.card-cover {
  width: 240px; border-radius: 12px; flex-shrink: 0;
  background-size: cover; background-position: center;
}

/* Empty */
.empty-state { text-align: center; color: var(--text-dim); padding: 64px; font-size: 16px; }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .banner-title { font-size: 28px; }
  .banner-overlay { padding: 32px 24px; }
  .series-banner { min-height: 240px; }
  .seq-number { width: 48px; }
  .seq-text { font-size: 24px; }
  .series-card { flex-direction: column; padding: 20px; }
  .card-cover { width: 100%; min-height: 160px; }
}
@media (max-width: 480px) {
  .banner-title { font-size: 24px; }
  .seq-number { width: 40px; }
  .seq-text { font-size: 20px; }
}
</style>
