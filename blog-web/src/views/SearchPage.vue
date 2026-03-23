<template>
  <div class="search-page">
    <!-- Header -->
    <div class="page-header fade-in">
      <h1 class="page-title gradient-text">搜索</h1>
      <p class="page-desc">按标题、内容、标签或分类查找文章</p>
    </div>

    <!-- ===== Large Glass Search Box ===== -->
    <div class="search-box-wrap fade-in">
      <div class="search-box glass" :class="{ focused: inputFocused }">
        <svg class="search-icon" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
        <input ref="inputRef" v-model="keyword" placeholder="搜索文章..."
               @input="onInput" @keyup.enter="search"
               @focus="inputFocused = true; showDropdown = true"
               @blur="onBlur" />
        <button v-if="keyword" class="clear-btn" @click="clearSearch">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
        </button>
        <button class="search-btn btn btn-primary" @click="search" :disabled="searching">
          {{ searching ? '搜索中...' : '搜索' }}
        </button>
      </div>

      <!-- Dropdown: Suggestions + Search History -->
      <Transition name="dropdown">
        <div v-if="showDropdown && (suggestions.length || searchHistory.length)" class="dropdown glass">
          <!-- Search History -->
          <div v-if="!keyword && searchHistory.length" class="dropdown-section">
            <div class="dropdown-header">
              <span>🕒 搜索历史</span>
              <button class="clear-history-btn" @click.stop="clearHistory">清空</button>
            </div>
            <div v-for="(h, idx) in searchHistory" :key="idx" class="dropdown-item history-item"
                 @mousedown.prevent="keyword = h; search()">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>
              <span>{{ h }}</span>
              <button class="remove-item-btn" @click.stop="removeHistoryItem(idx)">×</button>
            </div>
          </div>
          <!-- Suggestions -->
          <div v-if="keyword && suggestions.length" class="dropdown-section">
            <div class="dropdown-header"><span>🔍 搜索建议</span></div>
            <router-link v-for="s in suggestions" :key="s.id" :to="`/article/${s.slug}`"
                         class="dropdown-item" @click="showDropdown = false">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
              <span v-html="highlightText(s.title)" />
            </router-link>
          </div>
        </div>
      </Transition>
    </div>

    <!-- ===== Filters ===== -->
    <div class="filters fade-in">
      <div class="filter-group">
        <select v-model="filterCategoryId" @change="search" class="filter-select">
          <option :value="null">全部分类</option>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>
      <div class="filter-group">
        <select v-model="filterTagId" @change="search" class="filter-select">
          <option :value="null">全部标签</option>
          <option v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>
    </div>

    <!-- ===== Popular Keywords ===== -->
    <div v-if="!searched && popularKeywords.length" class="popular-keywords fade-in">
      <span class="pk-label">🔥 热门搜索：</span>
      <button v-for="pk in popularKeywords" :key="pk" class="pk-chip" @click="keyword = pk; search()">{{ pk }}</button>
    </div>

    <!-- ===== Results Header ===== -->
    <div v-if="searched" class="results-header fade-in">
      <span>找到 {{ total }} 条关于 "<strong>{{ searchedKeyword }}</strong>" 的结果</span>
    </div>

    <!-- ===== Results Grid ===== -->
    <div class="results-grid" v-if="articles.length">
      <router-link v-for="a in articles" :key="a.id" :to="`/article/${a.slug}`" class="result-card glass-card fade-in">
        <div v-if="a.coverImage" class="result-cover" :style="{ backgroundImage: `url(${a.coverImage})` }" />
        <div class="result-body">
          <div class="result-badges">
            <span v-if="a.categoryName" class="category-badge">{{ a.categoryName }}</span>
            <span v-if="a.matchType === 'content'" class="match-badge">内容匹配</span>
            <span v-for="tag in (a.tags || []).slice(0,2)" :key="tag.id" class="tag"
                  :style="{ '--tag-color': tag.color || '#a855f7' }">{{ tag.name }}</span>
          </div>
          <h3 v-html="highlightText(a.title)" />
          <p v-html="highlightText(a.excerpt || '')" />
          <div class="result-meta">
            <span>{{ formatDate(a.publishedAt) }}</span>
            <span class="meta-dot">·</span>
            <span>{{ a.wordCount || 0 }} 字</span>
            <span class="meta-dot">·</span>
            <span>{{ a.viewCount }} 阅读</span>
          </div>
        </div>
      </router-link>
    </div>

    <!-- ===== Empty State ===== -->
    <div v-if="searched && !articles.length" class="empty-state glass-card fade-in">
      <div class="empty-illustration">
        <div class="empty-circle" />
        <div class="empty-line l1" />
        <div class="empty-line l2" />
        <div class="empty-line l3" />
      </div>
      <h3>未找到结果</h3>
      <p>没有找到与 "<strong>{{ searchedKeyword }}</strong>" 相关的内容</p>
      <div class="empty-tips">
        <span>换个关键词试试</span>
        <span class="tip-dot">·</span>
        <span>移除筛选条件</span>
        <span class="tip-dot">·</span>
        <span>使用更简短的词</span>
      </div>
      <div class="empty-links">
        <router-link to="/articles" class="btn btn-ghost">浏览全部文章</router-link>
        <router-link to="/archive" class="btn btn-ghost">查看归档</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'
import { getCategories, getTags } from '../api'

const HISTORY_KEY = 'blog_search_history'
const MAX_HISTORY = 10

const route = useRoute()
const inputRef = ref<HTMLInputElement>()
const keyword = ref('')
const searchedKeyword = ref('')
const articles = ref<any[]>([])
const total = ref(0)
const searched = ref(false)
const searching = ref(false)
const inputFocused = ref(false)
const filterCategoryId = ref<number | null>(null)
const filterTagId = ref<number | null>(null)
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const suggestions = ref<any[]>([])
const showDropdown = ref(false)
const popularKeywords = ref<string[]>([])
const searchHistory = ref<string[]>([])

let suggestTimer: ReturnType<typeof setTimeout>

// ============ Search History ============

function loadHistory() {
  try {
    const saved = localStorage.getItem(HISTORY_KEY)
    searchHistory.value = saved ? JSON.parse(saved) : []
  } catch { searchHistory.value = [] }
}

function saveToHistory(kw: string) {
  if (!kw) return
  // Remove duplicates, add to front, limit size
  const history = searchHistory.value.filter(h => h !== kw)
  history.unshift(kw)
  if (history.length > MAX_HISTORY) history.length = MAX_HISTORY
  searchHistory.value = history
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history))
}

function removeHistoryItem(idx: number) {
  searchHistory.value.splice(idx, 1)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(searchHistory.value))
}

function clearHistory() {
  searchHistory.value = []
  localStorage.removeItem(HISTORY_KEY)
}

// ============ Formatting ============

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function highlightText(text: string) {
  if (!searchedKeyword.value || !text) return text
  const escaped = searchedKeyword.value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${escaped})`, 'gi'), '<mark>$1</mark>')
}

// ============ Suggestions ============

function onInput() {
  clearTimeout(suggestTimer)
  if (keyword.value.trim().length < 1) { suggestions.value = []; return }
  suggestTimer = setTimeout(async () => {
    try {
      const res: any = await request.get('/api/public/search/suggest', { params: { keyword: keyword.value } })
      suggestions.value = res.data || []
      showDropdown.value = true
    } catch { suggestions.value = [] }
  }, 300)
}

function onBlur() {
  inputFocused.value = false
  // Delay to allow clicks on dropdown items
  setTimeout(() => { showDropdown.value = false }, 200)
}

function clearSearch() {
  keyword.value = ''
  articles.value = []
  searched.value = false
  suggestions.value = []
}

// ============ Search ============

async function search() {
  const kw = keyword.value.trim()
  if (!kw) return
  searching.value = true
  searched.value = true
  searchedKeyword.value = kw
  showDropdown.value = false
  saveToHistory(kw)
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

// ============ Global click handler ============

function handleGlobalClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.search-box-wrap')) {
    showDropdown.value = false
  }
}

onMounted(async () => {
  loadHistory()
  try {
    const [cRes, tRes]: any = await Promise.all([getCategories(), getTags()])
    categories.value = cRes.data || []
    tags.value = tRes.data || []
  } catch {}
  try {
    const pkRes: any = await request.get('/api/public/search/popular')
    popularKeywords.value = pkRes.data || []
  } catch {}
  if (route.query.q) { keyword.value = route.query.q as string; search() }
  inputRef.value?.focus()
  document.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleGlobalClick)
})
</script>

<style scoped>
.search-page { max-width: 960px; margin: 0 auto; padding: 0 24px 80px; }

/* Header */
.page-header { text-align: center; padding: 40px 0 32px; }
.page-title { font-family: var(--font-heading); font-size: 48px; font-weight: 900; margin-bottom: 8px; letter-spacing: -0.03em; }
.page-desc { font-size: 17px; color: var(--text-muted); }

/* ===== Search Box ===== */
.search-box-wrap { position: relative; max-width: 720px; margin: 0 auto 32px; }
.search-box {
  display: flex; align-items: center; gap: 16px;
  padding: 8px 12px 8px 24px;
  border-radius: 99px;
  background: rgba(15,23,42,0.4); backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: 0 10px 40px rgba(0,0,0,0.3);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.search-box.focused {
  border-color: rgba(168,85,247,0.5); background: rgba(15,23,42,0.6);
  box-shadow: 0 10px 40px rgba(0,0,0,0.4), 0 0 30px rgba(168,85,247,0.2);
  transform: translateY(-2px);
}
.search-icon { color: var(--text-dim); flex-shrink: 0; transition: color 0.3s; }
.search-box.focused .search-icon { color: var(--primary-light); }
.search-box input {
  flex: 1; padding: 14px 0; background: transparent; border: none;
  color: #f8fafc; font-size: 18px; outline: none;
  font-family: var(--font-body); font-weight: 500;
}
.search-box input::placeholder { color: var(--text-dim); font-weight: 400; }
.clear-btn {
  background: none; border: none; color: var(--text-dim);
  cursor: pointer; padding: 8px; border-radius: 50%;
  transition: all 0.2s; display: flex;
}
.clear-btn:hover { background: rgba(244,63,94,0.1); color: var(--accent-rose); }
.search-btn { border-radius: 99px; padding: 12px 32px; font-size: 15px; font-weight: 700; box-shadow: 0 4px 16px rgba(168,85,247,0.4); letter-spacing: 0.05em; }

/* ===== Dropdown (Suggestions + History) ===== */
.dropdown {
  position: absolute; top: calc(100% + 16px); left: 0; right: 0;
  z-index: 100; border-radius: 20px;
  overflow: hidden; background: rgba(15,23,42,0.85); backdrop-filter: blur(24px);
  box-shadow: 0 20px 60px rgba(0,0,0,0.5); border: 1px solid rgba(255,255,255,0.08);
}
.dropdown-section { border-bottom: 1px solid rgba(255,255,255,0.05); }
.dropdown-section:last-child { border-bottom: none; }
.dropdown-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 24px 8px; font-size: 12px; color: var(--text-dim);
  font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em;
}
.clear-history-btn {
  background: none; border: none; color: var(--text-dim);
  font-size: 12px; cursor: pointer; padding: 2px 8px;
  border-radius: 6px; transition: all 0.2s;
}
.clear-history-btn:hover { color: var(--accent-rose); background: rgba(244,63,94,0.1); }
.dropdown-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 24px; font-size: 15px; color: var(--text-muted);
  text-decoration: none; transition: background 0.2s, color 0.2s;
  cursor: pointer; font-weight: 500;
}
.dropdown-item:hover { background: rgba(255,255,255,0.05); color: #f8fafc; }
.dropdown-item svg { color: var(--text-dim); flex-shrink: 0; }
.history-item { position: relative; }
.remove-item-btn {
  position: absolute; right: 20px;
  background: none; border: none; color: var(--text-dim);
  font-size: 16px; cursor: pointer; padding: 2px 6px;
  border-radius: 4px; opacity: 0; transition: all 0.2s;
}
.history-item:hover .remove-item-btn { opacity: 1; }
.remove-item-btn:hover { color: var(--accent-rose); background: rgba(244,63,94,0.1); }
.dropdown-enter-active, .dropdown-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-12px); }

/* ===== Filters ===== */
.filters { display: flex; gap: 16px; max-width: 720px; margin: 0 auto 32px; }
.filter-group { flex: 1; }
.filter-select {
  width: 100%; padding: 14px 20px; border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.08);
  background-color: rgba(15,23,42,0.4); backdrop-filter: blur(12px);
  color: var(--text-muted); font-weight: 500;
  font-size: 15px; outline: none; cursor: pointer;
  font-family: var(--font-body);
  transition: all 0.3s;
  -webkit-appearance: none; appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat; background-position: right 16px center; padding-right: 40px;
}
.filter-select:focus, .filter-select:hover { border-color: rgba(168,85,247,0.4); color: #f8fafc; }
.filter-select option { background: #0f172a; color: #f8fafc; }

/* Popular */
.popular-keywords { display: flex; align-items: center; justify-content: center; gap: 12px; flex-wrap: wrap; margin-bottom: 40px; }
.pk-label { font-size: 14px; color: var(--text-dim); font-weight: 600; }
.pk-chip {
  padding: 8px 20px; border-radius: 99px; font-size: 14px;
  background: rgba(15,23,42,0.4); border: 1px solid rgba(255,255,255,0.08);
  color: var(--text-muted); cursor: pointer; font-weight: 500;
  font-family: var(--font-body);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); backdrop-filter: blur(12px);
}
.pk-chip:hover { border-color: rgba(168,85,247,0.5); color: var(--primary-light); transform: translateY(-2px); box-shadow: 0 4px 12px rgba(168,85,247,0.2); }

/* Results */
.results-header { font-size: 16px; color: var(--text-muted); margin-bottom: 24px; text-align: center; }
.results-header strong { color: var(--primary-light); font-weight: 700; }

/* ===== Results Grid ===== */
.results-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 32px; }
.result-card {
  display: flex; flex-direction: column; overflow: hidden;
  text-decoration: none; color: inherit; border-radius: 20px;
  background: rgba(15,23,42,0.4); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.05);
  box-shadow: 0 4px 20px rgba(0,0,0,0.2);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}
.result-card:hover { transform: translateY(-8px) scale(1.02); border-color: rgba(168,85,247,0.3); box-shadow: 0 20px 40px rgba(0,0,0,0.3), 0 0 30px rgba(168,85,247,0.15); }
.result-cover { width: 100%; aspect-ratio: 16 / 9; flex-shrink: 0; background-size: cover; background-position: center; border-radius: 20px 20px 0 0; }
.result-body { flex: 1; padding: 24px; display: flex; flex-direction: column; }
.result-badges { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.category-badge { padding: 4px 12px; border-radius: 6px; font-size: 11px; font-weight: 700; background: rgba(16,185,129,0.15); color: #10b981; border: 1px solid rgba(16,185,129,0.3); letter-spacing: 0.05em; text-transform: uppercase; }
.match-badge { padding: 4px 12px; border-radius: 6px; font-size: 11px; font-weight: 700; background: rgba(245,158,11,0.15); color: #f59e0b; border: 1px solid rgba(245,158,11,0.3); letter-spacing: 0.05em; text-transform: uppercase; }
.tag { padding: 4px 12px; border-radius: 6px; font-size: 11px; font-weight: 600; background: color-mix(in srgb, var(--tag-color) 15%, transparent); color: var(--tag-color); border: 1px solid color-mix(in srgb, var(--tag-color) 25%, transparent); letter-spacing: 0.05em; text-transform: uppercase; }
.result-body h3 { font-family: var(--font-heading); font-size: 22px; font-weight: 800; color: #f8fafc; margin-bottom: 12px; line-height: 1.35; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.result-body p { font-size: 15px; color: var(--text-muted); line-height: 1.7; flex: 1; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.result-meta { display: flex; align-items: center; gap: 10px; font-size: 13px; color: var(--text-dim); margin-top: 16px; font-weight: 500; }
.meta-dot { opacity: 0.4; }

:deep(mark) {
  background: rgba(168,85,247,0.3); color: #f8fafc;
  padding: 2px 6px; border-radius: 4px; box-shadow: 0 0 10px rgba(168,85,247,0.4);
}

/* ===== Empty State ===== */
.empty-state { padding: 64px 40px; text-align: center; border-radius: 24px; background: rgba(15,23,42,0.3); border: 1px solid rgba(255,255,255,0.05); }
.empty-illustration {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  margin-bottom: 32px; opacity: 0.5;
}
.empty-circle {
  width: 56px; height: 56px; border-radius: 50%;
  border: 3px solid var(--border);
  position: relative;
}
.empty-circle::after {
  content: '';
  position: absolute; right: -8px; bottom: -8px;
  width: 16px; height: 3px;
  background: var(--border);
  transform: rotate(45deg);
  border-radius: 2px;
}
.empty-line {
  height: 3px; border-radius: 2px; background: var(--border);
}
.empty-line.l1 { width: 140px; }
.empty-line.l2 { width: 100px; }
.empty-line.l3 { width: 60px; }
.empty-state h3 { font-family: var(--font-heading); font-size: 22px; color: var(--text-heading); margin-bottom: 8px; }
.empty-state p { color: var(--text-muted); margin-bottom: 16px; }
.empty-state strong { color: var(--primary-light); }
.empty-tips { display: flex; gap: 8px; justify-content: center; font-size: 13px; color: var(--text-dim); margin-bottom: 24px; }
.tip-dot { opacity: 0.4; }
.empty-links { display: flex; gap: 12px; justify-content: center; }

/* Responsive */
@media (max-width: 768px) {
  .page-title { font-size: 36px; }
  .filters { flex-direction: column; }
  .result-card { flex-direction: column; }
  .result-cover { width: 100%; height: 180px; }
}
</style>
