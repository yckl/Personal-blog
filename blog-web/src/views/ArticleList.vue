<template>
  <div class="article-list-page">
    <div class="page-header fade-in">
      <h1 class="page-title gradient-text">所有文章</h1>
      <p class="page-desc">浏览所有文章，按标签筛选，发现精彩内容。</p>
    </div>

    <!-- ===== Top Filter Bar (Glass) ===== -->
    <div class="top-filter-bar glass-panel fade-in">
      <div class="filter-left">
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input v-model="searchKeyword" placeholder="搜索文章标题或内容..." @keyup.enter="onSearch" />
        </div>
        <div class="sort-tabs">
          <button v-for="tab in sortTabs" :key="tab.value"
                  :class="['sort-tab', { active: sortBy === tab.value }]"
                  :title="tab.desc"
                  @click="sortBy = tab.value; onSearch()">
            {{ tab.label }}
          </button>
        </div>
      </div>
      <div class="filter-right" v-if="selectedTagId || searchKeyword">
        <div class="active-filter" v-if="selectedTagId">
          <span class="neon-badge tag-badge">标签: {{ getTagName(selectedTagId) }}</span>
          <button class="clear-btn" @click="selectedTagId = null; onSearch()">✕</button>
        </div>
        <button v-if="searchKeyword || selectedTagId" class="clear-all-btn" @click="clearAllFilters">
          清除全部
        </button>
      </div>
    </div>

    <div class="page-layout">
      <!-- ===== Article Grid (Main) ===== -->
      <div class="articles-main">
        <div class="results-bar" v-if="articles.length">
          <span class="results-count">共 {{ totalCount }} 篇文章</span>
        </div>

        <div class="articles-grid">
          <router-link v-for="article in articles" :key="article.id"
                       :to="`/article/${article.slug}`" class="article-card glass-card fade-in">
            <!-- Thumbnail 16:9 -->
            <div class="card-cover-wrapper">
              <div class="card-cover" v-if="article.coverImage" :style="{ backgroundImage: `url(${article.coverImage})` }" />
              <div class="card-cover card-cover-fallback" v-else>
                <div class="fallback-glow"></div>
              </div>
              <div class="card-cover-overlay"></div>
            </div>

            <!-- Body -->
            <div class="card-body">
              <div class="card-tags">
                <span v-for="tag in (article.tags || []).slice(0,3)" :key="tag.id"
                      class="neon-badge" :style="{ '--badge-color': tag.color || '#a855f7' }">
                  {{ tag.name }}
                </span>
                <span v-if="article.categoryName" class="neon-badge category-badge">{{ article.categoryName }}</span>
              </div>
              <h3 class="card-title">{{ article.title }}</h3>
              <p class="card-excerpt">{{ article.excerpt || '点击阅读更多...' }}</p>
            </div>

            <!-- Footer -->
            <div class="card-footer">
              <span class="footer-time">{{ formatDate(article.publishedAt || article.createdAt) }}</span>
              <div class="footer-stats">
                <span title="真实浏览量">👁 {{ article.viewCount }}</span>
                <span title="预估阅读时间">⏱ {{ article.readingTime || Math.max(1, Math.ceil((article.wordCount || 0) / 200)) }} 分钟</span>
              </div>
            </div>
          </router-link>
        </div>

        <!-- Empty State -->
        <div class="empty-state glass-card" v-if="!loading && articles.length === 0">
          <div class="empty-icon">✨</div>
          <h3>暂无匹配文章</h3>
          <p>试试其他关键词或清除标签筛选吧。</p>
          <button class="neon-btn" @click="clearAllFilters">重置筛选</button>
        </div>

        <!-- Infinite Scroll Loader -->
        <div ref="loadMoreRef" class="load-more" v-if="hasMore">
          <div class="loader-dots">
            <span /><span /><span />
          </div>
          <p>正在加载更多...</p>
        </div>
        <div class="end-of-list" v-if="!hasMore && articles.length > 0">
          <p>— 已经到底啦 —</p>
        </div>
      </div>

      <!-- ===== Right Sidebar: Hot Tags ===== -->
      <aside class="tags-sidebar glass-panel" :class="{ collapsed: isSidebarCollapsed }">
        <div class="sidebar-header" @click="isSidebarCollapsed = !isSidebarCollapsed">
          <h3 class="gradient-text">热门标签</h3>
          <button class="collapse-btn">
            {{ isSidebarCollapsed ? '◀' : '▼' }}
          </button>
        </div>
        <div class="sidebar-content" :class="{ hidden: isSidebarCollapsed }">
          <div class="tag-cloud">
            <button v-for="tag in allTags" :key="tag.id"
                    :class="['tag-cloud-item', { active: selectedTagId === tag.id }]"
                    :style="{ '--tag-color': tag.color || '#22d3ee' }"
                    @click="toggleTag(tag.id)">
              <span class="tag-name">{{ tag.name }}</span>
              <span class="tag-count" v-if="tag.articleCount">{{ tag.articleCount }}</span>
            </button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getArticles, getTags } from '../api'

const articles = ref<any[]>([])
const page = ref(1)
const totalPages = ref(1)
const totalCount = ref(0)
const loading = ref(false)
const allTags = ref<any[]>([])
const selectedTagId = ref<number | null>(null)
const searchKeyword = ref('')
const loadMoreRef = ref<HTMLElement>()

const isSidebarCollapsed = ref(false)

const sortTabs = [
  { label: '最新发布', value: 'latest', desc: '按文章的发布日期从新到旧排列' },
  { label: '最多阅读', value: 'hot', desc: '按历史总阅读量(真实数据)从高到低排列' },
  { label: '站长推荐', value: 'recommended', desc: '按后台管理员设置的“标记为推荐”优先排列' },
]
const sortBy = ref('latest')

const hasMore = computed(() => page.value < totalPages.value)

function getTagName(id: number) {
  const t = allTags.value.find(t => t.id === id)
  return t ? t.name : 'Unknown'
}

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function onSearch() {
  page.value = 1
  articles.value = []
  loadData()
}

function toggleTag(tagId: number) {
  selectedTagId.value = selectedTagId.value === tagId ? null : tagId
  onSearch()
}

function clearAllFilters() {
  selectedTagId.value = null
  searchKeyword.value = ''
  onSearch()
}

let currentRequestId = 0

async function loadData(append = false) {
  const reqId = ++currentRequestId
  loading.value = true
  try {
    const params: any = { page: page.value, size: 12, sort: sortBy.value }
    if (selectedTagId.value) params.tagId = selectedTagId.value
    if (searchKeyword.value.trim()) params.keyword = searchKeyword.value.trim()
    
    const r: any = await getArticles(params)
    if (reqId !== currentRequestId) return // Prevent race conditions
    
    const newArticles = r.data?.records || []
    if (append) {
      articles.value.push(...newArticles)
    } else {
      articles.value = newArticles
    }
    totalPages.value = r.data?.pages || 1
    totalCount.value = r.data?.total || 0
  } catch (e) { 
    if (reqId === currentRequestId) console.error(e) 
  } finally { 
    if (reqId === currentRequestId) loading.value = false 
  }
}

// Infinite scroll
let observer: IntersectionObserver | null = null

function setupInfiniteScroll() {
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && hasMore.value && !loading.value) {
      page.value++
      loadData(true)
    }
  }, { rootMargin: '200px' })

  if (loadMoreRef.value) observer.observe(loadMoreRef.value)
}

onMounted(async () => {
  try {
    const tagRes: any = await getTags()
    // Optional: Sort tags by count to make them "Hot"
    allTags.value = (tagRes.data || []).sort((a: any, b: any) => (b.articleCount || 0) - (a.articleCount || 0))
  } catch {}

  await loadData()
  setupInfiniteScroll()
})

onUnmounted(() => { observer?.disconnect() })
</script>

<style scoped>
/* ===== Core Layout ===== */
.article-list-page { max-width: 1400px; margin: 0 auto; padding: 0 24px 80px; }
.page-header { text-align: center; padding: 40px 0 32px; }
.page-title {
  font-family: var(--font-heading);
  font-size: 56px; font-weight: 900;
  margin-bottom: 16px; letter-spacing: -0.02em;
}
.page-desc { font-size: 18px; color: var(--text-dim); max-width: 500px; margin: 0 auto; line-height: 1.6; }

.glass-panel {
  background: var(--card-bg); backdrop-filter: blur(20px);
  border: 1px solid var(--border);
  box-shadow: 0 8px 32px rgba(0,0,0,0.1);
}

/* ===== Top Filter Bar ===== */
.top-filter-bar {
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px;
  padding: 16px 24px; border-radius: 20px; margin-bottom: 32px;
  position: sticky; top: 80px; z-index: 40;
}
.filter-left { display: flex; align-items: center; flex-wrap: wrap; gap: 24px; }
.search-box {
  display: flex; align-items: center; gap: 10px;
  background: var(--bg-hover); border: 1px solid var(--border);
  padding: 8px 16px; border-radius: 99px; width: 280px; transition: all 0.3s;
}
.search-box:focus-within { border-color: #a855f7; box-shadow: 0 0 12px rgba(168,85,247,0.2); background: var(--card-bg); }
.search-icon { color: var(--text-dim); font-size: 14px; }
.search-box input {
  background: transparent; border: none; outline: none; color: var(--text-primary); width: 100%; font-size: 14px;
}
.search-box input::placeholder { color: var(--text-muted); }

.sort-tabs { display: flex; gap: 8px; background: var(--bg-hover); padding: 4px; border-radius: 99px; }
.sort-tab {
  padding: 8px 16px; border: none; background: transparent; color: var(--text-muted);
  font-size: 14px; font-weight: 600; cursor: pointer; border-radius: 99px; transition: all 0.3s;
}
.sort-tab:hover { color: var(--text-primary); }
.sort-tab.active { background: rgba(168,85,247,0.15); color: var(--primary); font-weight: 700; box-shadow: 0 2px 8px rgba(168,85,247,0.2); }

.filter-right { display: flex; align-items: center; gap: 12px; }
.active-filter { display: flex; align-items: center; gap: 8px; }
.clear-btn { background: transparent; border: none; color: var(--text-dim); cursor: pointer; border-radius: 50%; width: 24px; height: 24px; transition: all 0.2s; }
.clear-btn:hover { background: rgba(244,63,94,0.15); color: #f43f5e; }
.clear-all-btn { background: var(--bg-hover); border: 1px solid var(--border); color: var(--text-primary); padding: 6px 14px; border-radius: 99px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.clear-all-btn:hover { background: var(--border); }

/* ===== Main Layout ===== */
.page-layout { display: flex; gap: 32px; align-items: flex-start; }
.articles-main { flex: 1; min-width: 0; }

.results-bar { margin-bottom: 24px; padding-left: 8px; }
.results-count { font-size: 14px; color: var(--text-dim); font-weight: 600; letter-spacing: 0.05em; }

/* ===== Article Cards Grid ===== */
.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 32px;
}
.article-card {
  display: flex; flex-direction: column; overflow: hidden;
  text-decoration: none; color: inherit; height: 520px;
  border-radius: 16px; transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  background: var(--card-bg); border: 1px solid var(--border);
}
.article-card:hover {
  transform: translateY(-12px);
  border-color: rgba(168,85,247,0.4);
  box-shadow: 0 20px 40px rgba(0,0,0,0.4), 0 0 20px rgba(168,85,247,0.15), 0 0 40px rgba(34,211,238,0.1);
}

/* 16:9 Cover */
.card-cover-wrapper { position: relative; width: 100%; padding-top: 56.25%; /* 16:9 Aspect Ratio */ overflow: hidden; }
.card-cover { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background-size: cover; background-position: center; transition: transform 0.6s; }
.article-card:hover .card-cover { transform: scale(1.05); }
.card-cover-overlay { position: absolute; inset: 0; background: linear-gradient(180deg, transparent 50%, rgba(15,23,42,0.8) 100%); pointer-events: none; }
.card-cover-fallback { background: #0f172a; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.fallback-glow { width: 150px; height: 150px; background: #a855f7; filter: blur(80px); opacity: 0.5; border-radius: 50%; animation: pulse 4s infinite alternate; }

/* Neon Badges */
.card-tags { display: flex; gap: 8px; margin-bottom: 12px; position: absolute; top: -16px; left: 24px; z-index: 10; flex-wrap: wrap; width: calc(100% - 48px); }
.neon-badge {
  padding: 4px 12px; border-radius: 99px; font-size: 11px; font-weight: 700;
  background: rgba(15,23,42,0.8); backdrop-filter: blur(8px);
  color: var(--badge-color, #fff); border: 1px solid color-mix(in srgb, var(--badge-color, #fff) 30%, transparent);
  box-shadow: 0 0 10px color-mix(in srgb, var(--badge-color, #fff) 20%, transparent);
}
.category-badge { --badge-color: #22d3ee; }

/* Body Typography */
.card-body { flex: 1; padding: 32px 24px 20px; display: flex; flex-direction: column; position: relative; }
.card-title {
  font-family: var(--font-heading); font-size: 24px; font-weight: 700; color: var(--text-primary);
  margin-bottom: 12px; line-height: 1.4; letter-spacing: -0.01em;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  transition: color 0.3s;
}
.article-card:hover .card-title { background: linear-gradient(90deg, var(--text-primary), #a855f7); background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.card-excerpt {
  font-size: 15px; color: var(--text-muted); line-height: 1.7; flex: 1;
  display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}

/* Footer */
.card-footer { display: flex; align-items: center; justify-content: space-between; padding: 16px 24px; border-top: 1px solid var(--border); background: var(--bg-hover); flex-shrink: 0; }
.footer-time { font-size: 13px; color: var(--text-dim); font-weight: 500; }
.footer-stats { display: flex; gap: 16px; font-size: 13px; color: var(--text-dim); }

/* ===== Right Sidebar ===== */
.tags-sidebar { width: 300px; flex-shrink: 0; border-radius: 20px; position: sticky; top: 168px; padding: 24px; transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1); overflow: hidden; }
.tags-sidebar.collapsed { width: 80px; padding: 24px 16px; cursor: pointer; }
.tags-sidebar.collapsed:hover { background: var(--card-bg); }
.sidebar-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; cursor: pointer; }
.sidebar-header h3 { font-family: var(--font-heading); font-size: 18px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.1em; transition: opacity 0.3s; }
.tags-sidebar.collapsed h3 { opacity: 0; width: 0; }
.collapse-btn { background: var(--bg-hover); border: none; color: var(--text-primary); border-radius: 50%; width: 32px; height: 32px; cursor: pointer; transition: all 0.3s; }
.collapse-btn:hover { background: rgba(168,85,247,0.15); color: #a855f7; }

.sidebar-content { transition: opacity 0.3s; }
.sidebar-content.hidden { opacity: 0; pointer-events: none; height: 0; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: 10px; }
.tag-cloud-item {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 8px 16px; border-radius: 12px; font-size: 13px; font-weight: 500;
  background: var(--bg-hover); color: var(--text-muted); border: 1px solid var(--border);
  cursor: pointer; transition: all 0.3s;
}
.tag-cloud-item:hover { transform: translateY(-2px); border-color: var(--primary); color: var(--text-primary); }
.tag-cloud-item.active { background: color-mix(in srgb, var(--tag-color) 15%, transparent); border-color: var(--tag-color); color: var(--tag-color); box-shadow: 0 4px 16px color-mix(in srgb, var(--tag-color) 20%, transparent); font-weight: 700; }
.tag-cloud-item .tag-count { font-size: 11px; background: var(--border); padding: 2px 6px; border-radius: 6px; color: var(--text-primary); }

/* ===== Empty & Loading States ===== */
.empty-state { padding: 80px 24px; text-align: center; border-radius: 24px; margin-top: 32px; }
.empty-icon { font-size: 64px; margin-bottom: 24px; animation: float 3s ease-in-out infinite; }
.empty-state h3 { font-family: var(--font-heading); font-size: 28px; margin-bottom: 12px; }
.neon-btn { margin-top: 24px; background: linear-gradient(135deg, #a855f7, #22d3ee); border: none; padding: 12px 32px; border-radius: 99px; color: #fff; font-weight: 700; cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; box-shadow: 0 8px 24px rgba(168,85,247,0.3); }
.neon-btn:hover { transform: translateY(-2px); box-shadow: 0 12px 32px rgba(168,85,247,0.5); }

.load-more { text-align: center; padding: 60px 0; color: var(--text-dim); font-size: 15px; font-weight: 500; }
.end-of-list { text-align: center; padding: 40px 0; color: var(--text-dim); font-size: 14px; font-weight: 600; letter-spacing: 0.1em; opacity: 0.5; }

@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }

/* ===== Responsive ===== */
@media (max-width: 1200px) {
  .articles-grid { grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); }
  .tags-sidebar { width: 260px; }
}
@media (max-width: 900px) {
  .page-layout { flex-direction: column; }
  .tags-sidebar { width: 100% !important; position: static; }
  .tags-sidebar.collapsed { height: 80px; overflow: hidden; }
  .top-filter-bar { flex-direction: column; align-items: stretch; border-radius: 16px; top: 60px; }
  .filter-left { flex-direction: column; align-items: stretch; }
  .search-box { width: 100%; }
}
@media (max-width: 600px) {
  .articles-grid { grid-template-columns: 1fr; }
  .page-title { font-size: 40px; }
}
</style>
