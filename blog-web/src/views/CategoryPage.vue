<template>
  <div class="container">
    <div class="page-hero fade-in">
      <h1>📂 {{ category.name || 'Category' }}</h1>
      <p v-if="category.description">{{ category.description }}</p>
      <span class="article-count">{{ articles.length }} article{{ articles.length !== 1 ? 's' : '' }}</span>
    </div>

    <!-- Subcategories -->
    <div v-if="subcategories.length" class="subcategory-bar">
      <router-link v-for="sub in subcategories" :key="sub.id"
        :to="`/category/${sub.slug}`" class="subcat-chip">
        {{ sub.name }} <span v-if="sub.articleCount">({{ sub.articleCount }})</span>
      </router-link>
    </div>

    <div class="posts-list">
      <router-link v-for="a in articles" :key="a.id" :to="`/article/${a.slug}`" class="post-card fade-in">
        <div class="post-info">
          <div class="post-tags">
            <span v-for="tag in (a.tags || []).slice(0,3)" :key="tag.id" class="tag"
              :style="{ '--tag-color': tag.color || '#6366f1' }">{{ tag.name }}</span>
          </div>
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
    <p v-if="!articles.length && !loading" style="text-align:center;color:var(--text-dim);padding:48px;">No articles in this category yet.</p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const category = ref<any>({})
const articles = ref<any[]>([])
const subcategories = ref<any[]>([])
const loading = ref(true)

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

async function load() {
  loading.value = true
  const slug = route.params.slug as string
  try {
    // Load category tree to find current category
    const treeRes: any = await request.get('/api/public/categories/tree')
    const allCats = flattenTree(treeRes.data || [])
    const cat = allCats.find((c: any) => c.slug === slug)
    category.value = cat || { name: slug }

    // Load subcategories
    subcategories.value = allCats.filter((c: any) => cat && c.parentId === cat.id)

    // Load articles
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
watch(() => route.params.slug, load)
</script>

<style scoped>
.page-hero { text-align: center; padding: 40px 0 32px; }
.page-hero h1 { font-size: 32px; font-weight: 800; color: var(--text-heading); margin-bottom: 8px; }
.page-hero p { color: var(--text-muted); font-size: 16px; margin-bottom: 8px; }
.article-count { font-size: 13px; color: var(--text-dim); }

.subcategory-bar { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 24px; justify-content: center; }
.subcat-chip { padding: 6px 16px; border-radius: 20px; font-size: 13px; font-weight: 500; background: var(--bg-card); border: 1px solid var(--border); color: var(--text-muted); text-decoration: none; transition: all 0.2s; }
.subcat-chip:hover { border-color: var(--primary); color: var(--primary-light); }

.posts-list { display: flex; flex-direction: column; gap: 16px; }
.post-card { display: flex; gap: 20px; background: var(--bg-card); border-radius: var(--radius); padding: 24px; border: 1px solid var(--border); transition: all .3s; text-decoration: none; color: inherit; }
.post-card:hover { border-color: var(--primary); transform: translateX(4px); }
.post-info { flex: 1; }
.post-info h3 { font-size: 18px; color: var(--text-heading); margin-bottom: 8px; }
.post-info p { font-size: 14px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.post-cover { width: 160px; min-height: 120px; border-radius: 8px; background-size: cover; background-position: center; flex-shrink: 0; }
.post-tags { display: flex; gap: 6px; margin-bottom: 8px; flex-wrap: wrap; }
.tag { padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 500; background: color-mix(in srgb, var(--tag-color) 15%, transparent); color: var(--tag-color); }
.card-meta { display: flex; gap: 8px; font-size: 13px; color: var(--text-dim); margin-top: 12px; }

@media (max-width: 768px) {
  .post-card { flex-direction: column; }
  .post-cover { width: 100%; min-height: 160px; }
}
</style>
