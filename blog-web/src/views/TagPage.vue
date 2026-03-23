<template>
  <div class="container">
    <div class="page-hero fade-in">
      <h1>🏷️{{ tag.name || 'Tag' }}</h1>
      <span class="article-count">{{ articles.length }} article{{ articles.length !== 1 ? 's' : '' }}</span>
    </div>

    <div class="posts-list">
      <router-link v-for="a in articles" :key="a.id" :to="`/article/${a.slug}`" class="post-card fade-in">
        <div class="post-info">
          <h3>{{ a.title }}</h3>
          <p>{{ a.excerpt || 'Read more...' }}</p>
          <div class="card-meta">
            <span>{{ formatDate(a.publishedAt) }}</span>
            <span>·</span>
            <span>👁 {{ a.viewCount || 0 }}</span>
          </div>
        </div>
        <div v-if="a.coverImage" class="post-cover" :style="{ backgroundImage: `url(${a.coverImage})` }" />
      </router-link>
    </div>
    <p v-if="!articles.length && !loading" style="text-align:center;color:var(--text-dim);padding:48px;">No articles with this tag yet.</p>

    <!-- All Tags Cloud -->
    <section class="all-tags fade-in" v-if="allTags.length">
      <h2>Browse all tags</h2>
      <div class="tag-cloud">
        <router-link v-for="t in allTags" :key="t.id" :to="`/tag/${t.slug}`"
          class="tag-chip" :class="{ active: t.slug === currentSlug }">
          {{ t.name }} <span v-if="t.articleCount">({{ t.articleCount }})</span>
        </router-link>
      </div>
    </section>
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

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
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
.page-hero { text-align: center; padding: 40px 0 32px; }
.page-hero h1 { font-size: 32px; font-weight: 800; color: var(--text-heading); margin-bottom: 8px; }
.article-count { font-size: 13px; color: var(--text-dim); }

.posts-list { display: flex; flex-direction: column; gap: 16px; margin-bottom: 48px; }
.post-card { display: flex; gap: 20px; background: var(--bg-card); border-radius: var(--radius); padding: 24px; border: 1px solid var(--border); transition: all .3s; text-decoration: none; color: inherit; }
.post-card:hover { border-color: var(--primary); transform: translateX(4px); }
.post-info { flex: 1; }
.post-info h3 { font-size: 18px; color: var(--text-heading); margin-bottom: 8px; }
.post-info p { font-size: 14px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.post-cover { width: 160px; min-height: 120px; border-radius: 8px; background-size: cover; background-position: center; flex-shrink: 0; }
.card-meta { display: flex; gap: 8px; font-size: 13px; color: var(--text-dim); margin-top: 12px; }

.all-tags { margin-top: 16px; }
.all-tags h2 { font-size: 20px; font-weight: 700; color: var(--text-heading); margin-bottom: 16px; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-chip { padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: 500; background: var(--bg-card); border: 1px solid var(--border); color: var(--text-muted); text-decoration: none; transition: all 0.2s; }
.tag-chip:hover { border-color: var(--primary); color: var(--primary-light); }
.tag-chip.active { background: rgba(99,102,241,0.15); border-color: var(--primary); color: var(--primary-light); font-weight: 600; }

@media (max-width: 768px) {
  .post-card { flex-direction: column; }
  .post-cover { width: 100%; min-height: 160px; }
}
</style>
