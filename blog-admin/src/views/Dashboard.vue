<template>
  <div class="dashboard">
    <!-- Period Selector -->
    <div class="period-selector">
      <el-radio-group v-model="period" size="default" @change="loadData">
        <el-radio-button value="today">Today</el-radio-button>
        <el-radio-button value="7d">7 Days</el-radio-button>
        <el-radio-button value="30d">30 Days</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Stat Cards -->
    <el-row :gutter="20" style="margin-bottom: 24px;">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card-inner">
            <div class="stat-icon" :style="{ background: card.bg }">
              <el-icon :size="24" :color="card.color"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatNumber(card.value) }}</div>
              <div class="stat-label">{{ card.label }}</div>
              <div v-if="card.sub" class="stat-sub">{{ card.sub }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts Row -->
    <el-row :gutter="20" style="margin-bottom: 24px;">
      <!-- Trend Chart -->
      <el-col :span="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">📈 Traffic Trend</span>
              <el-radio-group v-model="trendDays" size="small" @change="loadTrend">
                <el-radio-button :value="7">7D</el-radio-button>
                <el-radio-button :value="14">14D</el-radio-button>
                <el-radio-button :value="30">30D</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <v-chart :option="trendChartOption" :style="{ height: '320px' }" autoresize />
        </el-card>
      </el-col>

      <!-- Hot Articles Bar Chart -->
      <el-col :span="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">🔥 Hot Articles</span>
            </div>
          </template>
          <v-chart :option="hotArticlesChartOption" :style="{ height: '320px' }" autoresize />
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom Row -->
    <el-row :gutter="20">
      <!-- Recent Comments -->
      <el-col :span="16">
        <el-card shadow="hover" class="list-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">💬 Recent Comments</span>
              <el-button type="primary" text @click="$router.push('/comments')">View All</el-button>
            </div>
          </template>
          <div v-if="recentComments.length === 0" class="empty-hint">No comments yet</div>
          <div v-for="comment in recentComments" :key="comment.id" class="comment-item">
            <el-avatar :size="36" :src="comment.authorAvatar" class="comment-avatar">
              {{ (comment.authorName || '?')[0] }}
            </el-avatar>
            <div class="comment-body">
              <div class="comment-meta">
                <span class="comment-author">{{ comment.authorName }}</span>
                <span class="comment-on">on</span>
                <span class="comment-article" @click="$router.push(`/articles/edit/${comment.articleId}`)">
                  {{ comment.articleTitle || 'Untitled' }}
                </span>
                <el-tag :type="comment.status === 'APPROVED' ? 'success' : comment.status === 'PENDING' ? 'warning' : 'danger'" size="small" style="margin-left: 8px;">
                  {{ comment.status }}
                </el-tag>
              </div>
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-time">{{ formatTime(comment.createdAt) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Pending Items + Quick Actions -->
      <el-col :span="8">
        <!-- Pending Items -->
        <el-card shadow="hover" class="pending-card" style="margin-bottom: 20px;">
          <template #header>
            <span class="card-title">⚡ Pending Items</span>
          </template>
          <div v-if="!summary?.pendingItems?.length" class="empty-hint" style="text-align:center; padding: 20px;">
            ✅ All clear! Nothing to do.
          </div>
          <div v-for="item in summary?.pendingItems" :key="item.type" class="pending-item" @click="$router.push(item.link)">
            <div class="pending-info">
              <el-badge :value="item.count" :max="99" type="danger">
                <span class="pending-label">{{ item.label }}</span>
              </el-badge>
            </div>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </el-card>

        <!-- Quick Actions -->
        <el-card shadow="hover" class="list-card">
          <template #header><span class="card-title">🚀 Quick Actions</span></template>
          <div class="quick-actions">
            <el-button type="primary" size="large" @click="$router.push('/articles/create')">
              <el-icon><EditPen /></el-icon> Write Article
            </el-button>
            <el-button size="large" @click="$router.push('/media')">
              <el-icon><Picture /></el-icon> Media Library
            </el-button>
            <el-button size="large" @click="$router.push('/comments')">
              <el-icon><ChatDotRound /></el-icon> Comments
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
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
import { View, ChatDotRound, Message, TrendCharts, ArrowRight } from '@element-plus/icons-vue'
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
  const viewsLabel = period.value === 'today' ? 'Today Views' : period.value === '7d' ? 'Week Views' : 'Month Views'
  return [
    { label: viewsLabel, value: viewsValue || 0, icon: markRaw(View), color: '#409eff', bg: '#ecf5ff', sub: `UV: ${s.todayUniqueVisitors || 0}` },
    { label: 'Subscribers', value: s.totalSubscribers || 0, icon: markRaw(Message), color: '#f56c6c', bg: '#fef0f0', sub: `+${s.newSubscribers || 0} this week` },
    { label: 'Articles', value: s.totalArticles || 0, icon: markRaw(TrendCharts), color: '#67c23a', bg: '#f0f9eb', sub: `${s.publishedArticles || 0} published` },
    { label: 'Comments', value: s.totalComments || 0, icon: markRaw(ChatDotRound), color: '#e6a23c', bg: '#fdf6ec', sub: `${s.pendingComments || 0} pending` }
  ]
})

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['Page Views', 'Unique Visitors'], bottom: 0 },
  grid: { left: '3%', right: '4%', bottom: '15%', top: '5%', containLabel: true },
  xAxis: {
    type: 'category',
    data: trendData.value.map((d: any) => d.date?.substring(5) || ''),
    axisLabel: { fontSize: 11 }
  },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: 'Page Views', type: 'line', smooth: true,
      data: trendData.value.map((d: any) => d.pageViews || 0),
      areaStyle: { color: 'rgba(64, 158, 255, 0.15)' },
      lineStyle: { color: '#409eff', width: 2 },
      itemStyle: { color: '#409eff' }
    },
    {
      name: 'Unique Visitors', type: 'line', smooth: true,
      data: trendData.value.map((d: any) => d.uniqueVisitors || 0),
      areaStyle: { color: 'rgba(103, 194, 58, 0.15)' },
      lineStyle: { color: '#67c23a', width: 2 },
      itemStyle: { color: '#67c23a' }
    }
  ]
}))

const hotArticlesChartOption = computed(() => {
  const articles = (hotArticles.value || []).slice(0, 8).reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: {
      type: 'category',
      data: articles.map((a: any) => a.title?.length > 12 ? a.title.substring(0, 12) + '…' : a.title || ''),
      axisLabel: { fontSize: 11 }
    },
    series: [{
      type: 'bar', barWidth: 16,
      data: articles.map((a: any) => a.viewCount || 0),
      itemStyle: {
        borderRadius: [0, 4, 4, 0],
        color: { type: 'linear', x: 0, y: 0, x2: 1, y2: 0, colorStops: [
          { offset: 0, color: '#409eff' }, { offset: 1, color: '#67c23a' }
        ]}
      }
    }]
  }
})

function formatNumber(n: number): string {
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return String(n || 0)
}

function formatTime(t: string): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

async function loadData() {
  try {
    const [summaryRes, trendRes, hotRes, commentsRes]: any = await Promise.all([
      getDashboardStats(),
      getDashboardTrend(trendDays.value),
      getHotArticles(10),
      getRecentComments(8)
    ])
    summary.value = summaryRes.data
    trendData.value = trendRes.data || []
    hotArticles.value = hotRes.data || []
    recentComments.value = commentsRes.data || []
  } catch (e) {
    console.error('Dashboard load error', e)
  }
}

async function loadTrend() {
  try {
    const res: any = await getDashboardTrend(trendDays.value)
    trendData.value = res.data || []
  } catch (e) {
    console.error(e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.dashboard {
  padding: 4px;
}
.period-selector {
  margin-bottom: 20px;
}
.stat-card {
  border-radius: 12px;
  transition: transform 0.2s;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-card-inner {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  border-radius: 12px;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1d1e2c;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #999;
}
.stat-sub {
  font-size: 12px;
  color: #b0b3c0;
  margin-top: 2px;
}
.chart-card, .list-card, .pending-card {
  border-radius: 12px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-weight: 600;
  font-size: 15px;
}

/* Comments */
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-avatar {
  flex-shrink: 0;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-meta {
  font-size: 13px;
  margin-bottom: 4px;
}
.comment-author {
  font-weight: 600;
  color: #1d1e2c;
}
.comment-on {
  color: #b0b3c0;
  margin: 0 4px;
}
.comment-article {
  color: #409eff;
  cursor: pointer;
}
.comment-article:hover {
  text-decoration: underline;
}
.comment-content {
  font-size: 13px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.comment-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

/* Pending */
.pending-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 4px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s;
}
.pending-item:hover {
  background: #f5f7fa;
}
.pending-item:last-child {
  border-bottom: none;
}
.pending-label {
  font-size: 14px;
  color: #333;
}

/* Quick Actions */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-hint {
  color: #c0c4cc;
  font-size: 13px;
  padding: 16px 0;
}
</style>
