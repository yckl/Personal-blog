<template>
  <div class="category-page">
    <!-- ===== Category Hero ===== -->
    <section class="category-hero fade-in" v-if="category.name" :style="heroGradientStyle">
      <div class="hero-overlay">
        <div class="hero-icon glass">
          <span>{{ categoryEmoji }}</span>
        </div>
        <h1 class="hero-title">{{ category.name }}</h1>
        <p v-if="category.description" class="hero-desc">{{ category.description }}</p>
        <span class="hero-count">{{ totalCount }} article{{ totalCount !== 1 ? 's' : '' }}</span>
      </div>
    </section>

    <div class="category-content">
      <!-- ===== Subcategory Filter ===== -->
      <div v-if="subcategories.length" class="subcat-bar fade-in">
        <button :class="['subcat-chip', { active: !activeSubcatId }]"
                @click="activeSubcatId = null; filterArticles()">
          All
        </button>
        <button v-for="sub in subcategories" :key="sub.id"
                :class="['subcat-chip', { active: activeSubcatId === sub.id }]"
                @click="activeSubcatId = sub.id; filterArticles()">
          {{ sub.name }}
          <span class="chip-count" v-if="sub.articleCount">{{ sub.articleCount }}</span>
        </button>
      </div>

      <!-- ===== Article Grid ===== -->
      <div class="articles-grid">
        <router-link v-for="a in displayArticles" :key="a.id"
                     :to="`/article/${a.slug}`" class="article-card glass-card fade-in">
          <div class="card-topbar" :style="{ background: heroGradient }" />
          <div class="card-cover" v-if="a.coverImage"
               :style="{ backgroundImage: `url(${a.coverImage})` }" />
          <div class="card-cover card-cover-fallback" v-else :style="{ background: heroGradient }" />
          <div class="card-body">
            <div class="card-tags">
              <span v-for="tag in (a.tags || []).slice(0,2)" :key="tag.id"
                    class="tag" :style="{ '--tag-color': tag.color || '#a855f7' }">{{ tag.name }}</span>
            </div>
            <h3 class="card-title">{{ a.title }}</h3>
            <p class="card-excerpt">{{ a.excerpt || 'Read more...' }}</p>
          </div>
          <div class="card-footer">
            <span>{{ formatDate(a.publishedAt) }}</span>
            <span class="footer-dot">·</span>
            <span>{{ a.viewCount || 0 }} views</span>
          </div>
        </router-link>
      </div>

      <!-- Empty -->
      <EmptyState v-if="!displayArticles.length && !loading"
                  title="分类下还没有文章哦"
                  message="博主正在努力构思中，去探索一下其他领域吧。" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import EmptyState from '../components/ui/EmptyState.vue'

const route = useRoute()
const category = ref<any>({})
const articles = ref<any[]>([])
const subcategories = ref<any[]>([])
const loading = ref(true)
const activeSubcatId = ref<number | null>(null)

// Color palette for category gradients
const gradients = [
  'linear-gradient(135deg, #a855f7 0%, #22d3ee 100%)',
  'linear-gradient(135deg, #f43f5e 0%, #f59e0b 100%)',
  'linear-gradient(135deg, #10b981 0%, #22d3ee 100%)',
  'linear-gradient(135deg, #6366f1 0%, #ec4899 100%)',
  'linear-gradient(135deg, #f97316 0%, #ef4444 100%)',
  'linear-gradient(135deg, #06b6d4 0%, #8b5cf6 100%)',
  'linear-gradient(135deg, #84cc16 0%, #22c55e 100%)',
  'linear-gradient(135deg, #e879f9 0%, #6366f1 100%)',
]
const categoryEmojis = ['📂', '💻', '🎨', '📖', '🔧', '🌍', '🧠', '🚀', '📊', '🔬']

const heroGradient = computed(() => {
  const idx = (category.value?.id || 0) % gradients.length
  return gradients[idx]
})
const heroGradientStyle = computed(() => ({ '--cat-gradient': heroGradient.value }))
const categoryEmoji = computed(() => {
  const idx = (category.value?.id || 0) % categoryEmojis.length
  return categoryEmojis[idx]
})
const totalCount = computed(() => articles.value.length)
const displayArticles = computed(() => {
  if (!activeSubcatId.value) return articles.value
  return articles.value.filter((a: any) => a.categoryId === activeSubcatId.value)
})

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

function filterArticles() {
  // reactive via computed displayArticles
}

async function load() {
  loading.value = true
  const slug = route.params.slug as string
  try {
    const treeRes: any = await request.get('/api/public/categories/tree')
    const allCats = flattenTree(treeRes.data || [])
    const cat = allCats.find((c: any) => c.slug === slug)
    category.value = cat || { name: slug }
    subcategories.value = allCats.filter((c: any) => cat && c.parentId === cat.id)
    const artRes: any = await request.get(`/api/public/categories/${slug}/articles`)
    articles.value = artRes.data || []
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}

function flattenTree(nodes: any[]): any[] {
  const result: any[] = []
  for (const n of nodes) {
    result.push(n)
    if (n.children) result.push(...flattenTree(n.children))
  }
  return result
}

onMounted(load)
watch(() => route.params.slug, () => { activeSubcatId.value = null; load() })
</script>

<style scoped>
.category-page { max-width: 1280px; margin: 0 auto; padding: 0 24px 80px; }

/* ===== Hero ===== */
.category-hero {
  position: relative; overflow: hidden;
  border-radius: var(--radius);
  margin-bottom: 40px;
  min-height: 280px;
}
.hero-overlay {
  position: relative;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 56px 32px;
  text-align: center;
  background: var(--cat-gradient);
}
.hero-overlay::after {
  content: '';
  position: absolute; inset: 0;
  background: rgba(10,10,10,0.4);
  backdrop-filter: blur(2px);
}
.hero-overlay > * { position: relative; z-index: 1; }

.hero-icon {
  width: 96px; height: 96px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 44px;
  margin-bottom: 20px;
  background: rgba(255,255,255,0.1);
  border-color: rgba(255,255,255,0.2);
  box-shadow: 0 8px 32px rgba(0,0,0,0.3);
}
.hero-title {
  font-family: var(--font-heading);
  font-size: 36px; font-weight: 900; color: #fff;
  letter-spacing: -0.03em; margin-bottom: 10px;
}
.hero-desc { color: rgba(255,255,255,0.8); font-size: 16px; max-width: 480px; margin-bottom: 12px; }
.hero-count {
  font-size: 13px; color: rgba(255,255,255,0.6);
  padding: 4px 16px; background: rgba(255,255,255,0.1);
  border-radius: 999px; backdrop-filter: blur(8px);
}

/* ===== Subcategory Filter ===== */
.subcat-bar { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-bottom: 32px; }
.subcat-chip {
  padding: 8px 20px; border-radius: 999px; font-size: 14px; font-weight: 500;
  background: var(--glass-bg); border: 1px solid var(--glass-border);
  color: var(--text-muted); cursor: pointer;
  transition: all 0.25s; font-family: var(--font-body);
  display: inline-flex; align-items: center; gap: 6px;
  backdrop-filter: blur(12px);
}
.subcat-chip:hover { border-color: var(--primary); color: var(--primary-light); }
.subcat-chip.active {
  background: rgba(168,85,247,0.15); border-color: var(--primary);
  color: var(--primary-light); font-weight: 600;
}
.chip-count { font-size: 11px; opacity: 0.7; }

/* ===== Content ===== */
.category-content { max-width: 1200px; margin: 0 auto; }

/* ===== Article Grid (Same structure as ArticleList) ===== */
.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 32px;
}
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
.card-cover {
  width: 100%; aspect-ratio: 16 / 9; flex-shrink: 0;
  background-size: cover; background-position: center; border-radius: 16px 16px 0 0;
}
.card-cover-fallback { background: linear-gradient(135deg, rgba(168,85,247,0.12) 0%, rgba(34,211,238,0.08) 100%); }
.card-body { flex: 1; padding: 24px; display: flex; flex-direction: column; }
.card-tags { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.tag {
  padding: 4px 12px; border-radius: 6px; font-size: 11px; font-weight: 600;
  background: color-mix(in srgb, var(--tag-color) 12%, transparent);
  color: var(--tag-color);
  border: 1px solid color-mix(in srgb, var(--tag-color) 20%, transparent);
  letter-spacing: 0.05em; text-transform: uppercase;
}
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

.empty-state { text-align: center; color: var(--text-dim); padding: 64px; font-size: 16px; }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .articles-grid { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); }
}
@media (max-width: 640px) {
  .articles-grid { grid-template-columns: 1fr; gap: 24px; }
  .hero-title { font-size: 32px; }
  .hero-icon { width: 80px; height: 80px; font-size: 36px; }
}
@media (max-width: 480px) {
  .category-hero { min-height: 220px; }
  .hero-title { font-size: 28px; }
}

</style>
