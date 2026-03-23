<template>
  <div class="container">
    <section class="page-header fade-in" v-if="series">
      <div class="series-hero" :style="{ backgroundImage: series.coverImage ? `url(${series.coverImage})` : 'var(--gradient-2)' }">
        <div class="series-hero-overlay">
          <h1>{{ series.name }}</h1>
          <p>{{ series.description }}</p>
          <span class="series-meta">{{ articles.length }} articles in series</span>
        </div>
      </div>
    </section>

    <section class="series-articles" v-if="articles.length">
      <div v-for="(article, idx) in articles" :key="article.id" class="series-item fade-in">
        <span class="order-badge">{{ idx + 1 }}</span>
        <router-link :to="`/article/${article.slug}`" class="series-article-card">
          <h3>{{ article.title }}</h3>
          <p>{{ article.excerpt || 'Read this article...' }}</p>
          <div class="card-meta">
            <span>{{ formatDate(article.publishedAt) }}</span>
            <span>·</span>
            <span>👁 {{ article.viewCount || 0 }}</span>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const series = ref<any>(null)
const articles = ref<any[]>([])

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

onMounted(async () => {
  try {
    const slug = route.params.slug as string
    const res: any = await request.get(`/api/public/series/${slug}`)
    series.value = res.data
    articles.value = res.data?.articles || []
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.page-header { margin-bottom: 32px; }
.series-hero { min-height: 240px; background-size: cover; background-position: center; border-radius: var(--radius); overflow: hidden; position: relative; }
.series-hero-overlay { position: absolute; inset: 0; background: linear-gradient(to top, rgba(0,0,0,0.8), rgba(0,0,0,0.3)); display: flex; flex-direction: column; justify-content: flex-end; padding: 32px; }
.series-hero-overlay h1 { font-size: 32px; color: var(--text-heading); margin-bottom: 8px; }
.series-hero-overlay p { color: rgba(255,255,255,0.8); font-size: 16px; margin-bottom: 8px; }
.series-meta { color: rgba(255,255,255,0.6); font-size: 13px; }

.series-articles { display: flex; flex-direction: column; gap: 12px; }
.series-item { display: flex; gap: 16px; align-items: flex-start; }
.order-badge { width: 36px; height: 36px; background: var(--gradient-1); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 15px; flex-shrink: 0; margin-top: 16px; }
.series-article-card { flex: 1; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 20px; transition: all 0.3s; text-decoration: none; color: inherit; }
.series-article-card:hover { border-color: var(--primary); transform: translateX(4px); }
.series-article-card h3 { font-size: 17px; color: var(--text-heading); margin-bottom: 6px; }
.series-article-card p { font-size: 14px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-meta { display: flex; gap: 8px; font-size: 13px; color: var(--text-dim); margin-top: 10px; }
</style>
