<template>
  <div class="container">
    <div class="page-hero fade-in">
      <h1>📅 Archive</h1>
      <p>All articles organized by time</p>
      <span class="article-count">{{ totalCount }} article{{ totalCount !== 1 ? 's' : '' }} total</span>
    </div>

    <!-- Timeline -->
    <div class="timeline">
      <div v-for="yearGroup in archiveData" :key="yearGroup.year" class="timeline-year fade-in">
        <h2 class="year-label">
          <span class="year-dot" />
          {{ yearGroup.year }}
          <span class="year-count">({{ yearGroup.totalCount }})</span>
        </h2>
        <div v-for="monthGroup in yearGroup.months" :key="monthGroup.month" class="timeline-month">
          <h3 class="month-label" @click="toggleMonth(yearGroup.year, monthGroup.month)">
            {{ getMonthName(monthGroup.month) }}
            <span class="month-count">{{ monthGroup.count }} articles</span>
            <span class="month-toggle">{{ isExpanded(yearGroup.year, monthGroup.month) ? '▼' : '▶' }}</span>
          </h3>
          <div v-show="isExpanded(yearGroup.year, monthGroup.month)" class="month-articles">
            <router-link v-for="a in monthGroup.articles" :key="a.id" :to="`/article/${a.slug}`" class="archive-item">
              <span class="archive-date">{{ formatDay(a.publishedAt) }}</span>
              <span class="archive-title">{{ a.title }}</span>
              <span class="archive-views">👁 {{ a.viewCount || 0 }}</span>
            </router-link>
          </div>
        </div>
      </div>
    </div>
    <p v-if="!archiveData.length && !loading" style="text-align:center;color:var(--text-dim);padding:48px;">No articles yet.</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '../utils/request'

interface MonthGroup { month: number; count: number; articles: any[] }
interface YearGroup { year: number; totalCount: number; months: MonthGroup[] }

const archiveData = ref<YearGroup[]>([])
const expandedKeys = ref<Set<string>>(new Set())
const loading = ref(true)

const totalCount = computed(() => archiveData.value.reduce((s, y) => s + y.totalCount, 0))

function getMonthName(m: number) {
  return ['January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'][m - 1]
}
function formatDay(d: string) {
  if (!d) return ''
  const dt = new Date(d)
  return `${(dt.getMonth() + 1).toString().padStart(2, '0')}-${dt.getDate().toString().padStart(2, '0')}`
}

function toggleMonth(year: number, month: number) {
  const key = `${year}-${month}`
  if (expandedKeys.value.has(key)) {
    expandedKeys.value.delete(key)
  } else {
    expandedKeys.value.add(key)
    // Lazy load articles for this month
    loadMonthArticles(year, month)
  }
}

function isExpanded(year: number, month: number) {
  return expandedKeys.value.has(`${year}-${month}`)
}

async function loadMonthArticles(year: number, month: number) {
  const yg = archiveData.value.find(y => y.year === year)
  if (!yg) return
  const mg = yg.months.find(m => m.month === month)
  if (!mg || mg.articles.length) return // already loaded
  try {
    const res: any = await request.get(`/api/public/archive/${year}/${month}`)
    mg.articles = res.data || []
  } catch (e) { console.error(e) }
}

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/public/archive')
    const data = res.data || []
    // group by year
    const yearMap: Record<number, MonthGroup[]> = {}
    for (const item of data) {
      if (!yearMap[item.year]) yearMap[item.year] = []
      yearMap[item.year].push({ month: item.month, count: item.count, articles: [] })
    }
    archiveData.value = Object.entries(yearMap)
      .map(([y, months]) => ({
        year: Number(y),
        totalCount: months.reduce((s, m) => s + m.count, 0),
        months: months.sort((a, b) => b.month - a.month)
      }))
      .sort((a, b) => b.year - a.year)
    // Auto-expand latest month
    if (archiveData.value.length && archiveData.value[0].months.length) {
      const latest = archiveData.value[0]
      toggleMonth(latest.year, latest.months[0].month)
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-hero { text-align: center; padding: 40px 0 32px; }
.page-hero h1 { font-size: 32px; font-weight: 800; color: var(--text-heading); margin-bottom: 8px; }
.page-hero p { color: var(--text-muted); font-size: 16px; margin-bottom: 4px; }
.article-count { font-size: 13px; color: var(--text-dim); }

.timeline { border-left: 2px solid var(--border); margin-left: 24px; padding-left: 24px; }

.timeline-year { margin-bottom: 32px; }
.year-label { display: flex; align-items: center; gap: 12px; font-size: 24px; font-weight: 700; color: var(--primary-light); margin-bottom: 16px; position: relative; }
.year-dot { position: absolute; left: -33px; width: 12px; height: 12px; background: var(--primary); border-radius: 50%; border: 2px solid var(--bg); }
.year-count { font-size: 14px; font-weight: 400; color: var(--text-dim); }

.timeline-month { margin-bottom: 12px; }
.month-label { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 600; color: var(--text-muted); cursor: pointer; padding: 8px 12px; border-radius: 8px; transition: background 0.2s; }
.month-label:hover { background: var(--bg-hover); }
.month-count { font-size: 13px; font-weight: 400; color: var(--text-dim); }
.month-toggle { font-size: 12px; color: var(--text-dim); margin-left: auto; }

.month-articles { padding-left: 12px; }
.archive-item { display: flex; gap: 16px; align-items: center; padding: 8px 16px; border-radius: 6px; transition: background .2s; text-decoration: none; color: inherit; }
.archive-item:hover { background: var(--bg-hover); }
.archive-date { color: var(--text-dim); font-size: 14px; font-family: 'JetBrains Mono', monospace; min-width: 50px; }
.archive-title { color: var(--text); font-size: 15px; flex: 1; }
.archive-views { font-size: 12px; color: var(--text-dim); }
</style>
