<template>
  <div class="container search-page">
    <div class="page-hero fade-in">
      <h1>🔍 Search</h1>
      <p>Find articles by title, content, tags, or categories</p>
    </div>

    <!-- Search Box -->
    <div class="search-box fade-in">
      <div class="search-input-wrap">
        <svg class="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
        <input ref="inputRef" v-model="keyword" placeholder="Search articles..."
          @input="onInput" @keyup.enter="search" @focus="showSuggestions = true" />
        <button v-if="keyword" class="clear-btn" @click="keyword=''; articles=[]; searched=false">✕</button>
      </div>

      <!-- Suggestions Dropdown -->
      <div v-if="showSuggestions && suggestions.length" class="suggestions-dropdown">
        <router-link v-for="s in suggestions" :key="s.id" :to="`/article/${s.slug}`" class="suggestion-item"
          @click="showSuggestions = false">
          {{ s.title }}
        </router-link>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters fade-in">
      <div class="filter-group">
        <label>Category</label>
        <select v-model="filterCategoryId" @change="search">
          <option :value="null">All Categories</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div class="filter-group">
        <label>Tag</label>
        <select v-model="filterTagId" @change="search">
          <option :value="null">All Tags</option>
          <option v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>
      <button class="btn btn-primary" @click="search" :disabled="searching">
        {{ searching ? 'Searching...' : 'Search' }}
      </button>
    </div>

    <!-- Popular Keywords -->
    <div v-if="!searched && popularKeywords.length" class="popular-keywords fade-in">
      <span class="pk-label">🔥 Popular:</span>
      <button v-for="pk in popularKeywords" :key="pk" class="pk-chip" @click="keyword = pk; search()">{{ pk }}</button>
    </div>

    <!-- Results -->
    <div v-if="searched" class="results-header">
      <span>{{ total }} result{{ total !== 1 ? 's' : '' }} for "<strong>{{ searchedKeyword }}</strong>"</span>
    </div>

    <div class="posts-list" v-if="articles.length">
      <router-link v-for="a in articles" :key="a.id" :to="`/article/${a.slug}`" class="post-card fade-in">
        <div class="post-info">
          <div class="result-badges">
            <span v-if="a.categoryName" class="category-badge">{{ a.categoryName }}</span>
            <span v-if="a.matchType === 'content'" class="match-badge">Content match</span>
            <span v-for="tag in (a.tags || []).slice(0,3)" :key="tag.id" class="tag"
              :style="{ '--tag-color': '#6366f1' }">{{ tag.name }}</span>
          </div>
          <h3 v-html="highlightText(a.title)" />
          <p v-html="highlightText(a.excerpt || '')" />
          <div class="card-meta">
            <span>{{ formatDate(a.publishedAt) }}</span>
            <span>·</span>
            <span>{{ a.wordCount || 0 }} words</span>
            <span>·</span>
            <span>👁 {{ a.viewCount }}</span>
          </div>
        </div>
        <div v-if="a.coverImage" class="post-cover" :style="{ backgroundImage: `url(${a.coverImage})` }" />
      </router-link>
    </div>

    <!-- No Results -->
    <div v-if="searched && !articles.length" class="no-results fade-in">
      <p class="no-results-text">No results found for "<strong>{{ searchedKeyword }}</strong>"</p>
      <p class="no-results-tips">Try:</p>
      <ul>
        <li>Using different keywords</li>
        <li>Removing filters</li>
        <li>Using shorter or more general terms</li>
      </ul>
      <div class="no-results-links">
        <router-link to="/articles" class="nf-link">📝 Browse All Articles</router-link>
        <router-link to="/archive" class="nf-link">📅 Archive</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import { getCategories, getTags } from '../api'

const route = useRoute()
const inputRef = ref<HTMLInputElement>()
const keyword = ref('')
const searchedKeyword = ref('')
const articles = ref<any[]>([])
const total = ref(0)
const searched = ref(false)
const searching = ref(false)
const filterCategoryId = ref<number | null>(null)
const filterTagId = ref<number | null>(null)
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const suggestions = ref<any[]>([])
const showSuggestions = ref(false)
const popularKeywords = ref<string[]>([])

let suggestTimer: ReturnType<typeof setTimeout>

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

function highlightText(text: string) {
  if (!searchedKeyword.value || !text) return text
  const escaped = searchedKeyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}

function onInput() {
  clearTimeout(suggestTimer)
  if (keyword.value.trim().length < 2) { suggestions.value = []; return }
  suggestTimer = setTimeout(async () => {
    try {
      const res: any = await request.get('/api/public/search/suggest', { params: { keyword: keyword.value } })
      suggestions.value = res.data || []
      showSuggestions.value = true
    } catch { suggestions.value = [] }
  }, 300)
}

async function search() {
  const kw = keyword.value.trim()
  if (!kw) return
  searching.value = true
  searched.value = true
  searchedKeyword.value = kw
  showSuggestions.value = false
  try {
    const params: any = { keyword: kw, size: 50 }
    if (filterCategoryId.value) params.categoryId = filterCategoryId.value
    if (filterTagId.value) params.tagId = filterTagId.value
    const res: any = await request.get('/api/public/search', { params })
    articles.value = res.data?.results || []
    total.value = res.data?.total || 0
  } catch (e) { console.error(e); articles.value = [] }
  finally { searching.value = false }
}

onMounted(async () => {
  // Load filters
  try {
    const [cRes, tRes]: any = await Promise.all([getCategories(), getTags()])
    categories.value = cRes.data || []
    tags.value = tRes.data || []
  } catch {}

  // Load popular keywords
  try {
    const pkRes: any = await request.get('/api/public/search/popular')
    popularKeywords.value = pkRes.data || []
  } catch {}

  // Auto-search from query param
  if (route.query.q) {
    keyword.value = route.query.q as string
    search()
  }

  // Focus input
  inputRef.value?.focus()

  // Close suggestions on click outside
  document.addEventListener('click', () => { showSuggestions.value = false })
})
</script>

<style scoped>
.search-page { max-width: 800px; margin: 0 auto; }

.page-hero { text-align: center; padding: 40px 0 24px; }
.page-hero h1 { font-size: 32px; font-weight: 800; color: var(--text-heading); margin-bottom: 8px; }
.page-hero p { color: var(--text-muted); font-size: 16px; }

/* Search Box */
.search-box { position: relative; margin-bottom: 16px; }
.search-input-wrap {
  display: flex; align-items: center; gap: 12px;
  background: var(--bg-card); border: 2px solid var(--border); border-radius: 14px;
  padding: 4px 16px; transition: border-color 0.2s;
}
.search-input-wrap:focus-within { border-color: var(--primary); }
.search-icon { color: var(--text-dim); flex-shrink: 0; }
.search-input-wrap input {
  flex: 1; padding: 14px 0; background: transparent; border: none;
  color: var(--text); font-size: 17px; outline: none;
}
.clear-btn {
  background: none; border: none; color: var(--text-dim); font-size: 16px;
  cursor: pointer; padding: 4px;
}
.clear-btn:hover { color: var(--text); }

/* Suggestions */
.suggestions-dropdown {
  position: absolute; top: 100%; left: 0; right: 0; z-index: 50;
  background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px;
  margin-top: 4px; overflow: hidden; box-shadow: 0 8px 32px rgba(0,0,0,0.3);
}
.suggestion-item {
  display: block; padding: 12px 20px; font-size: 15px; color: var(--text);
  text-decoration: none; transition: background 0.15s;
}
.suggestion-item:hover { background: var(--bg-hover); }

/* Filters */
.filters { display: flex; gap: 12px; margin-bottom: 24px; align-items: flex-end; }
.filter-group { flex: 1; }
.filter-group label { display: block; font-size: 12px; font-weight: 600; color: var(--text-dim); margin-bottom: 4px; text-transform: uppercase; }
.filter-group select {
  width: 100%; padding: 10px 12px; border-radius: 8px;
  border: 1px solid var(--border); background: var(--bg-card);
  color: var(--text); font-size: 14px; outline: none; cursor: pointer;
}
.filter-group select:focus { border-color: var(--primary); }
.btn { padding: 10px 24px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; border: none; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-dark); }

/* Popular Keywords */
.popular-keywords { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 32px; }
.pk-label { font-size: 13px; color: var(--text-dim); }
.pk-chip {
  padding: 4px 14px; border-radius: 20px; font-size: 13px;
  background: var(--bg-card); border: 1px solid var(--border);
  color: var(--text-muted); cursor: pointer; transition: all 0.2s;
}
.pk-chip:hover { border-color: var(--primary); color: var(--primary-light); }

/* Results */
.results-header { font-size: 14px; color: var(--text-muted); margin-bottom: 16px; }
.results-header strong { color: var(--primary-light); }

.posts-list { display: flex; flex-direction: column; gap: 16px; }
.post-card { display: flex; gap: 20px; background: var(--bg-card); border-radius: var(--radius); padding: 24px; border: 1px solid var(--border); transition: all .3s; text-decoration: none; color: inherit; }
.post-card:hover { border-color: var(--primary); transform: translateX(4px); }
.post-info { flex: 1; }
.post-info h3 { font-size: 18px; color: var(--text-heading); margin-bottom: 8px; }
.post-info p { font-size: 14px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.post-cover { width: 140px; min-height: 100px; border-radius: 8px; background-size: cover; background-position: center; flex-shrink: 0; }
.card-meta { display: flex; gap: 8px; font-size: 13px; color: var(--text-dim); margin-top: 12px; }

.result-badges { display: flex; gap: 6px; margin-bottom: 8px; flex-wrap: wrap; }
.category-badge { padding: 2px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; background: rgba(16,185,129,0.15); color: var(--accent-green); }
.match-badge { padding: 2px 10px; border-radius: 4px; font-size: 11px; font-weight: 600; background: rgba(245,158,11,0.15); color: var(--accent-amber); }
.tag { padding: 2px 10px; border-radius: 4px; font-size: 11px; font-weight: 500; background: color-mix(in srgb, var(--tag-color) 15%, transparent); color: var(--tag-color); }

:deep(mark) { background: rgba(99,102,241,0.3); color: var(--primary-light); padding: 1px 2px; border-radius: 2px; }

/* No Results */
.no-results { text-align: center; padding: 48px 24px; }
.no-results-text { font-size: 18px; color: var(--text-muted); margin-bottom: 12px; }
.no-results-text strong { color: var(--primary-light); }
.no-results-tips { color: var(--text-dim); font-size: 14px; margin-bottom: 8px; }
.no-results ul { color: var(--text-dim); font-size: 14px; list-style: none; margin-bottom: 24px; }
.no-results ul li::before { content: "→"; color: var(--primary); }
.no-results-links { display: flex; gap: 12px; justify-content: center; }
.nf-link { padding: 10px 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 10px; color: var(--text-muted); font-size: 14px; text-decoration: none; transition: all 0.2s; }
.nf-link:hover { border-color: var(--primary); color: var(--primary-light); }

@media (max-width: 768px) {
  .filters { flex-direction: column; }
  .post-card { flex-direction: column; }
  .post-cover { width: 100%; min-height: 160px; }
}
</style>
