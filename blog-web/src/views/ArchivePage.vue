<template>
  <div class="archive-page">
    <!-- Header -->
    <div class="page-header fade-in">
      <h1 class="page-title gradient-text">归档</h1>
      <p class="page-desc">共 {{ totalCount }} 篇文章</p>
    </div>

    <div class="archive-layout">
      <!-- ===== Main Timeline ===== -->
      <div class="timeline-main">
        <div v-for="yearGroup in archiveData" :key="yearGroup.year" class="timeline-year fade-in">
          <!-- Year Title -->
          <div class="year-header" :id="'year-' + yearGroup.year">
            <div class="year-dot-wrap">
              <span class="year-dot" />
            </div>
            <h2 class="year-title">{{ yearGroup.year }}</h2>
            <span class="year-count">{{ yearGroup.totalCount }} 篇</span>
          </div>

          <!-- Months -->
          <div v-for="monthGroup in yearGroup.months" :key="monthGroup.month" class="timeline-month">
            <div class="month-header" @click="toggleMonth(yearGroup.year, monthGroup.month)">
              <span class="month-name">{{ getMonthName(monthGroup.month) }}</span>
              <span class="month-count">{{ monthGroup.count }}</span>
              <svg class="month-chevron" :class="{ expanded: isExpanded(yearGroup.year, monthGroup.month) }"
                   width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="m6 9 6 6 6-6"/>
              </svg>
            </div>

            <!-- Articles (expandable) -->
            <Transition name="expand">
              <div v-show="isExpanded(yearGroup.year, monthGroup.month)" class="month-articles">
                <router-link v-for="a in monthGroup.articles" :key="a.id"
                             :to="`/article/${a.slug}`" class="archive-item">
                  <span class="item-line" />
                  <div class="item-content">
                    <h4 class="item-title">{{ a.title }}</h4>
                    <div class="item-meta">
                      <span class="item-date">{{ formatDay(a.publishedAt) }}</span>
                      <span v-if="a.readingTime || a.wordCount" class="item-reading">
                        {{ a.readingTime || Math.max(1, Math.round((a.wordCount||0)/200)) }} 分钟
                      </span>
                      <span v-for="tag in (a.tags||[]).slice(0,2)" :key="tag.id" class="item-tag"
                            :style="{ '--tag-color': tag.color || '#a855f7' }">{{ tag.name }}</span>
                      <span class="item-views">{{ a.viewCount || 0 }} 阅读</span>
                    </div>
                  </div>
                </router-link>
              </div>
            </Transition>
          </div>
        </div>

        <p v-if="!archiveData.length && !loading" class="empty-state">暂无文章。</p>
      </div>

      <!-- ===== Year Nav — Sticky Right ===== -->
      <aside class="year-nav" v-if="archiveData.length">
        <div class="year-nav-card glass">
          <a v-for="yg in archiveData" :key="yg.year"
             :href="'#year-' + yg.year"
             :class="['year-nav-item', { active: activeYear === yg.year }]"
             @click.prevent="scrollToYear(yg.year)">
            <span class="year-nav-label">{{ yg.year }}</span>
            <span class="year-nav-count">{{ yg.totalCount }}</span>
          </a>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import request from '../utils/request'

interface MonthGroup { month: number; count: number; articles: any[] }
interface YearGroup { year: number; totalCount: number; months: MonthGroup[] }

const archiveData = ref<YearGroup[]>([])
const expandedKeys = ref<Set<string>>(new Set())
const loading = ref(true)
const activeYear = ref(0)

const totalCount = computed(() => archiveData.value.reduce((s, y) => s + y.totalCount, 0))

function getMonthName(m: number) {
  return ['一月', '二月', '三月', '四月', '五月', '六月',
    '七月', '八月', '九月', '十月', '十一月', '十二月'][m - 1]
}

function formatDay(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function toggleMonth(year: number, month: number) {
  const key = `${year}-${month}`
  if (expandedKeys.value.has(key)) {
    expandedKeys.value.delete(key)
  } else {
    expandedKeys.value.add(key)
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
  if (!mg || mg.articles.length) return
  try {
    const res: any = await request.get(`/api/public/archive/${year}/${month}`)
    mg.articles = res.data || []
  } catch (e) { console.error(e) }
}

function scrollToYear(year: number) {
  const el = document.getElementById('year-' + year)
  el?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function handleScroll() {
  // Update active year based on scroll position
  for (const yg of archiveData.value) {
    const el = document.getElementById('year-' + yg.year)
    if (el && el.getBoundingClientRect().top <= 150) {
      activeYear.value = yg.year
    }
  }
}

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/public/archive')
    const data = res.data || []
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
    if (archiveData.value.length) {
      activeYear.value = archiveData.value[0].year
      if (archiveData.value[0].months.length) {
        toggleMonth(archiveData.value[0].year, archiveData.value[0].months[0].month)
      }
    }
  } catch (e) { console.error(e) }
  finally { loading.value = false }

  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.archive-page { max-width: 1200px; margin: 0 auto; padding: 0 24px 80px; }

/* ===== Header ===== */
.page-header { text-align: center; padding: 40px 0 56px; }
.page-title {
  font-family: var(--font-heading);
  font-size: 48px; font-weight: 900;
  margin-bottom: 12px;
  letter-spacing: -0.03em;
}
.page-desc { font-size: 17px; color: var(--text-muted); }

/* ===== Layout ===== */
.archive-layout { display: flex; gap: 40px; align-items: flex-start; }

/* ===== Timeline ===== */
.timeline-main {
  flex: 1; min-width: 0;
  border-left: 2px solid transparent;
  border-image: var(--gradient-1) 1;
  padding-left: 32px;
  margin-left: 16px;
}

/* Year */
.timeline-year { margin-bottom: 48px; }
.year-header {
  display: flex; align-items: center; gap: 16px;
  margin-bottom: 24px; position: relative;
}
.year-dot-wrap {
  position: absolute; left: -41px;
  width: 20px; height: 20px;
  background: var(--bg);
  display: flex; align-items: center; justify-content: center;
}
.year-dot {
  width: 12px; height: 12px; border-radius: 50%;
  background: var(--gradient-1);
  box-shadow: 0 0 12px rgba(168,85,247,0.5), 0 0 24px rgba(34,211,238,0.2);
}
.year-title {
  font-family: var(--font-heading);
  font-size: 64px; font-weight: 900;
  color: var(--text-heading);
  letter-spacing: -0.04em;
  line-height: 1;
}
.year-count {
  font-size: 14px; color: var(--text-dim); font-weight: 500;
  padding: 4px 14px;
  background: var(--glass-bg);
  border: 1px solid var(--border);
  border-radius: 999px;
}

/* Month */
.timeline-month { margin-bottom: 8px; }
.month-header {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px; border-radius: var(--radius-xs);
  cursor: pointer; transition: all 0.25s;
  position: relative;
}
.month-header::before {
  content: '';
  position: absolute; left: -39px;
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--text-dim);
}
.month-header:hover { background: var(--bg-hover); }
.month-name {
  font-family: var(--font-heading);
  font-size: 17px; font-weight: 600; color: var(--text-muted);
}
.month-count {
  font-size: 12px; color: var(--text-dim);
  background: rgba(168,85,247,0.1);
  padding: 2px 10px; border-radius: 999px;
}
.month-chevron {
  margin-left: auto; color: var(--text-dim);
  transition: transform 0.3s;
}
.month-chevron.expanded { transform: rotate(180deg); }

/* Articles */
.month-articles { padding: 8px 0 16px 16px; }
.archive-item {
  display: flex; align-items: flex-start; gap: 16px;
  padding: 20px; border-radius: 16px;
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--border);
  box-shadow: 0 4px 20px rgba(0,0,0,0.1);
  text-decoration: none; color: inherit;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  margin-bottom: 16px;
}
.archive-item:hover {
  background: var(--bg-elevated);
  transform: translateX(8px) translateY(-2px);
  border-color: rgba(168,85,247,0.3);
  box-shadow: 0 12px 30px rgba(0,0,0,0.15), 0 0 20px rgba(168,85,247,0.15);
}
.item-line {
  position: absolute; left: -36px; top: 32px;
  width: 20px; height: 2px;
  background: linear-gradient(90deg, transparent, rgba(168,85,247,0.5));
}
.item-line::after {
  content: ''; position: absolute; right: 0; top: -3px;
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--primary);
  box-shadow: 0 0 12px var(--primary-light);
}
.item-content { flex: 1; min-width: 0; }
.item-title {
  font-family: var(--font-heading);
  font-size: 18px; font-weight: 700; color: var(--text-primary);
  margin-bottom: 8px; line-height: 1.4;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.item-meta {
  display: flex; align-items: center; gap: 12px;
  font-size: 13px; color: var(--text-dim); font-weight: 500;
  flex-wrap: wrap;
}
.item-date { font-family: var(--font-mono); font-size: 12px; color: var(--text-muted); }
.item-reading { color: var(--text-dim); }
.item-tag {
  padding: 2px 10px; border-radius: 6px; font-size: 11px; font-weight: 600;
  background: color-mix(in srgb, var(--tag-color) 15%, transparent);
  color: var(--tag-color); letter-spacing: 0.05em;
  border: 1px solid color-mix(in srgb, var(--tag-color) 25%, transparent);
}
.item-views { margin-left: auto; font-size: 12px; background: var(--bg-hover); padding: 2px 8px; border-radius: 4px; }

/* ===== Year Nav — Sticky ===== */
.year-nav { width: 180px; flex-shrink: 0; position: sticky; top: 100px; }
.year-nav-card {
  padding: 16px; border-radius: 20px;
  background: var(--card-bg); backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  box-shadow: var(--card-shadow);
}
.year-nav-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border-radius: 12px;
  font-size: 15px; color: var(--text-muted);
  text-decoration: none; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-family: var(--font-heading); font-weight: 600;
  position: relative; overflow: hidden;
}
.year-nav-item:not(:last-child) { margin-bottom: 4px; }
.year-nav-item:hover { background: var(--bg-hover); color: var(--text-primary); transform: translateX(4px); }
.year-nav-item.active {
  background: rgba(168,85,247,0.15);
  color: var(--primary-light);
  border: 1px solid rgba(168,85,247,0.2);
}
.year-nav-item.active::before {
  content: '';
  position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 4px; height: 60%;
  background: linear-gradient(180deg, #a855f7, #22d3ee);
  border-radius: 0 4px 4px 0;
  box-shadow: 2px 0 10px rgba(168,85,247,0.5);
}
.year-nav-count {
  font-size: 12px; color: var(--text-dim);
  font-weight: 500; background: var(--border);
  padding: 2px 8px; border-radius: 99px;
}
.year-nav-label { letter-spacing: -0.01em; }

/* Expand transition */
.expand-enter-active, .expand-leave-active { transition: all 0.3s ease; overflow: hidden; }
.expand-enter-from, .expand-leave-to { max-height: 0; opacity: 0; }
.expand-enter-to, .expand-leave-from { max-height: 1000px; opacity: 1; }

/* Empty state */
.empty-state { text-align: center; color: var(--text-dim); padding: 64px; font-size: 16px; }

/* ===== Responsive ===== */
@media (max-width: 1024px) {
  .year-nav { display: none; }
}
@media (max-width: 768px) {
  .year-title { font-size: 48px; }
  .timeline-main { padding-left: 24px; margin-left: 8px; }
  .year-dot-wrap { left: -33px; }
  .month-header::before { left: -31px; }
  .item-line { left: -18px; }
  .page-title { font-size: 36px; }
}
@media (max-width: 480px) {
  .year-title { font-size: 36px; }
  .item-meta { gap: 6px; }
}
</style>
