<template>
  <div class="tag-page">
    <!-- Header -->
    <div class="page-header fade-in">
      <h1 class="page-title gradient-text">标签</h1>
      <p class="page-desc">按标签浏览文章</p>
    </div>

    <!-- ===== Interactive Bubble Tag Cloud ===== -->
    <section class="tag-cloud-section glass-card fade-in">
      <div class="bubble-cloud">
        <router-link v-for="t in allTags" :key="t.id"
                     :to="`/tag/${t.slug || t.id}`"
                     :class="['bubble', { active: t.slug === currentSlug }]"
                     :style="{
                       fontSize: getTagSize(t.articleCount) + 'px',
                       '--bubble-color': t.color || '#a855f7'
                     }">
          {{ t.name }}
          <span class="bubble-count">{{ t.articleCount || 0 }}</span>
        </router-link>
      </div>
    </section>

    <!-- ===== Selected Tag Info ===== -->
    <div class="active-tag-bar fade-in" v-if="tag.name">
      <div class="tag-indicator">
        <span class="tag-dot" :style="{ background: tag.color || 'var(--primary)' }" />
        <h2>{{ tag.name }}</h2>
        <span class="tag-article-count">{{ articles.length }} 篇文章</span>
      </div>
    </div>

    <!-- ===== Article Grid ===== -->
    <div class="articles-grid" v-if="articles.length">
      <router-link v-for="a in articles" :key="a.id"
                   :to="`/article/${a.slug}`" class="article-card glass-card fade-in">
        <div class="card-topbar" :style="{ background: tag.color || 'var(--gradient-1)' }" />
        <div class="card-cover" v-if="a.coverImage"
             :style="{ backgroundImage: `url(${a.coverImage})` }" />
        <div class="card-cover card-cover-fallback" v-else />
        <div class="card-body">
          <h3 class="card-title">{{ a.title }}</h3>
          <p class="card-excerpt">{{ a.excerpt || '查看更多...' }}</p>
        </div>
        <div class="card-footer">
          <span>{{ formatDate(a.publishedAt) }}</span>
          <span class="footer-dot">·</span>
          <span>{{ a.viewCount || 0 }} 阅读</span>
        </div>
      </router-link>
    </div>

    <p v-if="!articles.length && !loading && tag.name" class="empty-state">该标签下暂无文章</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const tag = ref<any>({})
const articles = ref<any[]>([])
const allTags = ref<any[]>([])
const loading = ref(true)
const currentSlug = computed(() => route.params.slug as string)

// Dynamic font size: 14-32px based on article count
const maxCount = computed(() => Math.max(...allTags.value.map(t => t.articleCount || 0), 1))
function getTagSize(count: number): number {
  const min = 14, max = 32
  const ratio = (count || 0) / maxCount.value
  return Math.round(min + ratio * (max - min))
}

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

async function load() {
  loading.value = true
  const slug = route.params.slug as string
  try {
    const [tagsRes, artRes]: any = await Promise.all([
      request.get('/api/public/tags/all'),
      request.get(`/api/public/tags/${slug}/articles`)
    ])
    allTags.value = tagsRes.data || []
    const t = allTags.value.find((t: any) => t.slug === slug)
    tag.value = t || { name: slug }
    articles.value = artRes.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

onMounted(load)
watch(() => route.params.slug, load)
</script>

<style scoped>
.tag-page { max-width: 1280px; margin: 0 auto; padding: 0 24px 80px; }

/* Header */
.page-header { text-align: center; padding: 40px 0 32px; }
.page-title { font-family: var(--font-heading); font-size: 48px; font-weight: 900; margin-bottom: 8px; letter-spacing: -0.03em; }
.page-desc { font-size: 17px; color: var(--text-muted); }

/* ===== Bubble Cloud ===== */
.tag-cloud-section { padding: 40px; margin-bottom: 40px; text-align: center; }
.bubble-cloud {
  display: flex; flex-wrap: wrap; gap: 12px;
  justify-content: center; align-items: center;
}
.bubble {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 20px; border-radius: 999px;
  background: color-mix(in srgb, var(--bubble-color) 8%, transparent);
  color: var(--bubble-color);
  border: 1px solid color-mix(in srgb, var(--bubble-color) 15%, transparent);
  text-decoration: none;
  font-family: var(--font-heading); font-weight: 600;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
}
.bubble:hover {
  transform: scale(1.15);
  background: color-mix(in srgb, var(--bubble-color) 18%, transparent);
  border-color: var(--bubble-color);
  box-shadow: 0 0 24px color-mix(in srgb, var(--bubble-color) 30%, transparent),
              0 0 48px color-mix(in srgb, var(--bubble-color) 10%, transparent);
}
.bubble.active {
  background: color-mix(in srgb, var(--bubble-color) 25%, transparent);
  border-color: var(--bubble-color);
  box-shadow: 0 0 20px color-mix(in srgb, var(--bubble-color) 35%, transparent);
  transform: scale(1.1);
}
.bubble-count {
  font-size: 0.7em; opacity: 0.6;
  background: rgba(255,255,255,0.08);
  padding: 1px 7px; border-radius: 999px;
}

/* ===== Active Tag ===== */
.active-tag-bar { margin-bottom: 32px; display: flex; justify-content: center; }
.tag-indicator { display: flex; align-items: center; gap: 12px; background: rgba(15,23,42,0.4); padding: 12px 24px; border-radius: 99px; backdrop-filter: blur(8px); border: 1px solid rgba(255,255,255,0.05); box-shadow: 0 4px 20px rgba(0,0,0,0.2); }
.tag-dot { width: 14px; height: 14px; border-radius: 50%; box-shadow: 0 0 15px currentColor; }
.tag-indicator h2 { font-family: var(--font-heading); font-size: 28px; font-weight: 800; color: #f8fafc; }
.tag-article-count { font-size: 14px; color: var(--text-dim); margin-left: 8px; font-weight: 500; }

/* ===== Article Grid (Same structure as ArticleList) ===== */
.articles-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 32px; }
.article-card {
  display: flex; flex-direction: column; overflow: hidden;
  text-decoration: none; color: inherit; border-radius: 16px;
  background: rgba(15,23,42,0.4); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.05);
  box-shadow: 0 4px 20px rgba(0,0,0,0.2);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}
.article-card:hover {
  transform: translateY(-8px) scale(1.02);
  border-color: rgba(168,85,247,0.3);
  box-shadow: 0 20px 40px rgba(0,0,0,0.3), 0 0 30px rgba(168,85,247,0.15);
}
.card-topbar { height: 4px; flex-shrink: 0; }
.card-cover { width: 100%; aspect-ratio: 16 / 9; flex-shrink: 0; background-size: cover; background-position: center; border-radius: 16px 16px 0 0; }
.card-cover-fallback { background: linear-gradient(135deg, rgba(168,85,247,0.12) 0%, rgba(34,211,238,0.08) 100%); }
.card-body { flex: 1; padding: 24px; display: flex; flex-direction: column; }
.card-title {
  font-family: var(--font-heading); font-size: 24px; font-weight: 800;
  color: #f8fafc; margin-bottom: 12px; line-height: 1.3;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.card-excerpt {
  font-size: 15px; color: var(--text-muted); line-height: 1.7; flex: 1;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.card-footer {
  display: flex; align-items: center; gap: 8px;
  padding: 16px 24px; border-top: 1px solid rgba(255,255,255,0.05);
  font-size: 13px; color: var(--text-dim); font-weight: 500;
}
.footer-dot { opacity: 0.5; }

.empty-state { text-align: center; color: var(--text-dim); padding: 48px; font-size: 16px; }

/* Responsive */
@media (max-width: 1024px) {
  .articles-grid { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); }
}
@media (max-width: 640px) {
  .articles-grid { grid-template-columns: 1fr; gap: 24px; }
  .page-title { font-size: 36px; }
  .tag-cloud-section { padding: 24px; }
}

</style>
