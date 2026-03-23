<template>
  <div class="dashboard">
    <!-- Period Selector -->
    <div class="period-selector">
      <el-radio-group v-model="period" size="default" @change="loadData">
        <el-radio-button value="today">今天</el-radio-button>
        <el-radio-button value="7d">7天</el-radio-button>
        <el-radio-button value="30d">30天</el-radio-button>
      </el-radio-group>
    </div>

    <!-- ===== 4 Glass Metric Cards ===== -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card glass-admin">
        <div class="stat-icon-wrap" :style="{ '--accent': card.color }">
          <el-icon :size="24"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">
            <span class="counter" :data-target="card.value">{{ animatedValues[card.label] ?? 0 }}</span>
          </div>
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-sub" v-if="card.sub">{{ card.sub }}</div>
        </div>
      </div>
    </div>

    <!-- ===== Charts Row ===== -->
    <div class="row-flex">
      <div class="col-main">
        <div class="chart-card glass-admin">
          <div class="card-header">
            <span class="card-title">📈 流量趋势</span>
            <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
              <el-radio-button :value="7">7D</el-radio-button>
              <el-radio-button :value="14">14D</el-radio-button>
              <el-radio-button :value="30">30D</el-radio-button>
            </el-radio-group>
          </div>
          <v-chart :option="trendChartOption" :style="{ height: '320px' }" autoresize />
        </div>
      </div>
      <div class="col-side">
        <div class="hot-card glass-admin">
          <div class="card-header">
            <span class="card-title">🔥 热门文章</span>
          </div>
          <div class="hot-list">
            <div v-for="(a, idx) in hotArticles.slice(0, 6)" :key="a.id" class="hot-item">
              <span class="hot-rank">{{ String(idx + 1).padStart(2, '0') }}</span>
              <div class="hot-info">
                <div class="hot-title">{{ a.title }}</div>
                <div class="hot-bar-wrap">
                  <div class="hot-bar" :style="{ width: (a.viewCount / Math.max(...hotArticles.map((x: any) => x.viewCount || 1)) * 100) + '%' }" />
                </div>
              </div>
              <span class="hot-views">{{ a.viewCount || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== Bottom Row ===== -->
    <div class="row-flex">
      <!-- Recent 评论管理 -->
      <div class="col-main">
        <div class="list-card glass-admin full-height-card">
          <div class="card-header">
            <span class="card-title">💬 最近评论</span>
            <el-button type="primary" text @click="$router.push('/comments')">查看全部</el-button>
          </div>
          <div v-if="recentComments.length === 0" class="empty-hint">暂无评论</div>
          <div v-for="comment in recentComments" :key="comment.id" class="comment-item">
            <el-avatar :size="36" :src="comment.authorAvatar" class="comment-avatar">
              {{ (comment.authorName || '?')[0] }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-meta">
                <span class="comment-author">{{ comment.authorName }}</span>
                <span class="comment-on">评论了</span>
                <span class="comment-article" @click="$router.push(`/articles/edit/${comment.articleId}`)">
                  {{ comment.articleTitle || '无标题' }}
                </span>
                <el-tag :type="comment.status === 'APPROVED' ? 'success' : comment.status === 'PENDING' ? 'warning' : 'danger'" size="small" style="margin-left: 8px;">
                  {{ comment.status }}
                </el-tag>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- Pending + Quick Actions -->
      <div class="col-side">
        <div class="pending-card glass-admin" style="margin-bottom: 24px;">
          <div class="card-header"><span class="card-title">⚡ 待处理项</span></div>
          <div v-if="!summary?.pendingItems?.length" class="empty-hint" style="text-align:center; padding: 20px;">
            ✅ 全部已完成！
          </div>
          <div v-for="item in summary?.pendingItems" :key="item.type" class="pending-item" @click="$router.push(item.link)">
            <div class="pending-info">
              <el-badge :value="item.count" :max="99" type="danger">
                <span class="pending-label">{{ item.label }}</span>
              </el-badge>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>

        <div class="list-card glass-admin split-card">
          <div class="card-header"><span class="card-title">🚀 快捷操作</span></div>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/articles/create')">
              <el-icon><EditPen /></el-icon> 写文章
            </el-button>
            <el-button size="large" @click="$router.push('/media')">
              <el-icon><Picture /></el-icon> 媒体库
            </el-button>
            <el-button size="large" @click="$router.push('/comments')">
              <el-icon><ChatDotRound /></el-icon> 评论管理
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, markRaw } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent,
  LegendComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { View, ChatDotRound, Message, TrendCharts, ArrowRight, EditPen, Picture } from '@element-plus/icons-vue'
import { getDashboardStats, getDashboardTrend, getHotArticles, getRecentComments } from '../api/modules'

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const period = ref('7d')
const trendDays = ref(7)
const summary = ref<any>(null)
const trendData = ref<any[]>([])
const hotArticles = ref<any[]>([])
const recentComments = ref<any[]>([])

const statCards = computed(() => {
  const s = summary.value || {}
  const viewsValue = period.value === 'today' ? s.todayViews : period.value === '7d' ? s.weekViews : s.monthViews
  const viewsLabel = period.value === 'today' ? '今日浏览' : period.value === '7d' ? '本周浏览' : '本月浏览'
  return [
    { label: viewsLabel, value: viewsValue || 0, icon: markRaw(View), color: '#a855f7', sub: `UV: ${s.todayUniqueVisitors || 0}` },
    { label: '订阅者', value: s.totalSubscribers || 0, icon: markRaw(Message), color: '#f43f5e', sub: `+${s.newSubscribers || 0} 本周` },
    { label: '文章', value: s.totalArticles || 0, icon: markRaw(TrendCharts), color: '#22d3ee', sub: `${s.publishedArticles || 0} 已发布` },
    { label: '评论', value: s.totalComments || 0, icon: markRaw(ChatDotRound), color: '#f59e0b', sub: `${s.pendingComments || 0} 待审核` }
  ]
})

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis', backgroundColor: 'rgba(15,23,42,0.9)', borderColor: 'rgba(168,85,247,0.2)', textStyle: { color: '#f8fafc' } },
  legend: { data: ['页面浏览', '独立访客'], bottom: 0, textStyle: { color: '#94a3b8' } },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '5%', containLabel: true },
  xAxis: { type: 'category', data: trendData.value.map((d: any) => d.date?.substring(5) || ''), axisLabel: { fontSize: 11, color: '#64748b' }, axisLine: { lineStyle: { color: '#1e293b' } } },
  yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#1e293b' } } },
  series: [
    { name: '页面浏览', type: 'line', smooth: true, data: trendData.value.map((d: any) => d.pageViews || 0), areaStyle: { color: 'rgba(168,85,247,0.15)' }, lineStyle: { color: '#a855f7', width: 2 }, itemStyle: { color: '#a855f7' } },
    { name: '独立访客', type: 'line', smooth: true, data: trendData.value.map((d: any) => d.uniqueVisitors || 0), areaStyle: { color: 'rgba(34,211,238,0.15)' }, lineStyle: { color: '#22d3ee', width: 2 }, itemStyle: { color: '#22d3ee' } }
  ]
}))



function formatNumber(n: number): string {
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return String(n || 0)
}

// Animated counters
const animatedValues = ref<Record<string, string>>({})

function animateCounters() {
  for (const card of statCards.value) {
    const target = card.value
    const label = card.label
    const duration = 1200
    const start = performance.now()
    function step(now: number) {
      const progress = Math.min((now - start) / duration, 1)
      const eased = 1 - Math.pow(1 - progress, 3) // easeOutCubic
      const current = Math.round(target * eased)
      animatedValues.value[label] = formatNumber(current)
      if (progress < 1) requestAnimationFrame(step)
    }
    requestAnimationFrame(step)
  }
}
function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }

async function loadData() {
  try {
    const [summaryRes, trendRes, hotRes, commentsRes]: any = await Promise.all([
      getDashboardStats(), getDashboardTrend(trendDays.value), getHotArticles(10), getRecentComments(8)
    ])
    summary.value = summaryRes.data; trendData.value = trendRes.data || []
    hotArticles.value = hotRes.data || []; recentComments.value = commentsRes.data || []
    // Trigger animated counters after data loads
    setTimeout(animateCounters, 100)
  } catch (e) { console.error('Dashboard load error', e) }
}
async function loadTrend() {
  try { const res: any = await getDashboardTrend(trendDays.value); trendData.value = res.data || [] } catch (e) { console.error(e) }
}
onMounted(loadData)
</script>

<style scoped>
.dashboard { padding: 4px; }
.period-selector { margin-bottom: 24px; }

/* ===== Glass Admin Card ===== */
.glass-admin {
  background: rgba(15,23,42,0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1);
  border-radius: 20px;
  padding: 24px;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 4px 24px rgba(0,0,0,0.2);
}
.glass-admin:hover {
  border-color: rgba(168,85,247,0.25);
  box-shadow: 0 8px 40px rgba(168,85,247,0.12), 0 0 0 1px rgba(168,85,247,0.1);
}

/* ===== Stat Grid ===== */
.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 28px; margin-bottom: 28px;
}
.stat-card { display: flex; align-items: center; gap: 16px; }
.stat-card:hover {
  transform: translateY(-4px) scale(1.01);
  box-shadow: 0 12px 40px rgba(168,85,247,0.15), 0 0 24px rgba(34,211,238,0.06);
}
.stat-icon-wrap {
  width: 52px; height: 52px; border-radius: 14px;
  background: color-mix(in srgb, var(--accent) 15%, transparent);
  display: flex; align-items: center; justify-content: center;
  color: var(--accent); flex-shrink: 0;
}
.stat-value { font-size: 32px; font-weight: 800; color: #f8fafc; line-height: 1.2; font-family: 'Inter', sans-serif; }
.stat-label { font-size: 13px; color: #94a3b8; }
.stat-sub { font-size: 12px; color: #475569; margin-top: 2px; }

/* ===== Cards ===== */
.chart-card, .list-card, .pending-card { margin-bottom: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-weight: 700; font-size: 15px; color: #f8fafc; }

/* 评论管理 */
.comment-item { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px solid rgba(255,255,255,0.05); }
.comment-item:last-child { border-bottom: none; }
.comment-body { flex: 1; min-width: 0; }
.comment-meta { font-size: 13px; margin-bottom: 4px; }
.comment-author { font-weight: 600; color: #f8fafc; }
.comment-on { color: #475569; margin: 0 4px; }
.comment-article { color: #a855f7; cursor: pointer; }
.comment-article:hover { text-decoration: underline; }
.comment-content { font-size: 13px; color: #94a3b8; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.comment-time { font-size: 12px; color: #475569; margin-top: 4px; }

/* Pending */
.pending-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 4px; cursor: pointer; border-bottom: 1px solid rgba(255,255,255,0.05); transition: background 0.2s; color: #94a3b8; }
.pending-item:hover { background: rgba(168,85,247,0.06); }
.pending-item:last-child { border-bottom: none; }
.pending-label { font-size: 14px; color: #f8fafc; }

/* Quick Actions */
.quick-actions { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.quick-actions .el-button { margin: 0; }
.empty-hint { color: #475569; font-size: 13px; padding: 16px 0; }

/* Responsive */
/* Hot Articles */
.hot-list { display: flex; flex-direction: column; gap: 10px; }
.hot-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: 12px;
  transition: all 0.25s; cursor: default;
}
.hot-item:hover { background: rgba(168,85,247,0.06); }
.hot-rank {
  font-size: 16px; font-weight: 800; color: #475569;
  min-width: 24px; font-family: 'Inter', sans-serif;
}
.hot-item:nth-child(1) .hot-rank,
.hot-item:nth-child(2) .hot-rank,
.hot-item:nth-child(3) .hot-rank { background: linear-gradient(135deg, #a855f7, #22d3ee); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.hot-info { flex: 1; min-width: 0; }
.hot-title { font-size: 13px; color: #f8fafc; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4px; }
.hot-bar-wrap { height: 3px; background: rgba(255,255,255,0.05); border-radius: 2px; }
.hot-bar { height: 100%; border-radius: 2px; background: linear-gradient(90deg, #a855f7, #22d3ee); transition: width 0.5s; }
.hot-views { font-size: 12px; color: #64748b; min-width: 36px; text-align: right; }

/* ===== Flex Row Layout ===== */
.row-flex {
  display: flex; gap: 24px; margin-bottom: 24px; align-items: stretch;
}
.col-main { flex: 2; min-width: 0; display: flex; flex-direction: column; }
.col-side { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.full-height-card { flex: 1; display: flex; flex-direction: column; margin-bottom: 0 !important; }
.split-card { flex: 1; margin-bottom: 0 !important; }

/* ===== Responsive ===== */
@media (max-width: 1200px) { .stat-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 900px) {
  .row-flex { flex-direction: column; }
  .stat-grid { grid-template-columns: repeat(2, 1fr); gap: 16px; }
  .stat-value { font-size: 24px; }
}
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: 1fr; }
}
</style>
