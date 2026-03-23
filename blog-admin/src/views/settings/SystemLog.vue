<template>
  <div class="log-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">📋 系统日志</h3>
      <div class="toolbar-right">
        <el-button :type="viewMode === 'table' ? 'primary' : 'default'" size="small" @click="viewMode = 'table'">📋 表格</el-button>
        <el-button :type="viewMode === 'timeline' ? 'primary' : 'default'" size="small" @click="viewMode = 'timeline'">🕐 时间轴</el-button>
      </div>
    </div>

    <!-- Filters -->
    <div class="filter-bar glass-admin">
      <el-select v-model="filter.action" placeholder="操作类型" clearable style="width:150px;" @change="loadData">
        <el-option label="登录" value="LOGIN" /><el-option label="创建" value="CREATE" />
        <el-option label="更新" value="UPDATE" /><el-option label="删除" value="DELETE" />
        <el-option label="发布" value="PUBLISH" /><el-option label="导出" value="EXPORT" />
        <el-option label="导入" value="IMPORT" />
      </el-select>
      <el-select v-model="filter.resource" placeholder="资源" clearable style="width:150px;" @change="loadData">
        <el-option label="文章" value="article" /><el-option label="评论" value="comment" />
        <el-option label="媒体" value="media" /><el-option label="订阅者" value="subscriber" />
        <el-option label="邮件" value="newsletter" /><el-option label="设置" value="settings" />
        <el-option label="用户" value="user" />
      </el-select>
      <el-date-picker v-model="filter.dateRange" type="daterange" range-separator="to"
        start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:260px;" />
      <el-button type="primary" @click="loadData">搜索</el-button>
    </div>

    <!-- Table View -->
    <div v-if="viewMode === 'table'" class="table-wrap glass-admin">
      <el-table :data="logs" v-loading="loading"
                :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                :row-style="{ height: '56px' }"
                :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="action" label="操作类型" width="110">
          <template #default="{ row }">
            <span :class="['action-badge', actionClass(row.action)]">{{ row.action }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="resource" label="资源" width="120" />
        <el-table-column prop="resourceId" label="资源ID" width="80" />
        <el-table-column prop="detail" label="详情" min-width="250" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户" width="70" />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }"><span class="time-text">{{ formatTime(row.createdAt) }}</span></template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Timeline View -->
    <div v-else class="timeline-wrap glass-admin">
      <div v-for="log in logs" :key="log.id" class="timeline-item">
        <div :class="['timeline-dot', actionClass(log.action)]" />
        <div class="timeline-content">
          <div class="timeline-header">
            <span :class="['action-badge', actionClass(log.action)]">{{ log.action }}</span>
            <span class="timeline-resource">{{ log.resource }} #{{ log.resourceId }}</span>
            <span class="time-text">{{ formatTime(log.createdAt) }}</span>
          </div>
          <p class="timeline-detail">{{ log.detail }}</p>
          <span class="timeline-meta">User {{ log.userId }} · {{ log.ipAddress }}</span>
        </div>
      </div>
    </div>

    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next"
                   style="margin-top: 20px; justify-content: flex-end;" @current-change="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const logs = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const viewMode = ref<'table' | 'timeline'>('table')
const filter = ref<any>({ action: '', resource: '', dateRange: null })

function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }

function actionClass(action: string): string {
  const map: Record<string, string> = { CREATE: 'success', DELETE: 'danger', UPDATE: 'warning', LOGIN: 'info', PUBLISH: 'success', EXPORT: 'cyan', IMPORT: 'cyan' }
  return map[action] || 'info'
}

async function loadData() {
  loading.value = true
  const params: any = { page: page.value, size: 30 }
  if (filter.value.action) params.action = filter.value.action
  if (filter.value.resource) params.resource = filter.value.resource
  if (filter.value.dateRange?.[0]) { params.startDate = filter.value.dateRange[0]; params.endDate = filter.value.dateRange[1] }
  try { const r: any = await request.get('/api/admin/audit-logs', { params }); logs.value = r.data?.records || r.data || []; total.value = r.data?.total || 0 }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}
onMounted(loadData)
</script>

<style scoped>
.log-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.toolbar-right { display: flex; gap: 8px; }
.filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 20px; flex-wrap: wrap; }
.table-wrap { padding: 0; overflow: hidden; }
.time-text { font-size: 13px; color: #475569; }

/* Action badges */
.action-badge {
  display: inline-block; padding: 3px 10px; border-radius: 999px;
  font-size: 11px; font-weight: 700; text-transform: uppercase;
}
.action-badge.success { background: rgba(16,185,129,0.12); color: #10b981; }
.action-badge.danger { background: rgba(244,63,94,0.12); color: #f43f5e; }
.action-badge.warning { background: rgba(245,158,11,0.12); color: #f59e0b; }
.action-badge.info { background: rgba(168,85,247,0.12); color: #a855f7; }
.action-badge.cyan { background: rgba(34,211,238,0.12); color: #22d3ee; }

/* Timeline */
.timeline-wrap { padding: 0 0 0 20px; }
.timeline-item {
  display: flex; gap: 16px; padding: 16px 0;
  border-left: 2px solid rgba(168,85,247,0.15); padding-left: 20px;
  position: relative;
}
.timeline-dot {
  position: absolute; left: -7px; top: 22px; width: 12px; height: 12px; border-radius: 50%;
  border: 2px solid rgba(15,23,42,0.6);
}
.timeline-dot.success { background: #10b981; }
.timeline-dot.danger { background: #f43f5e; }
.timeline-dot.warning { background: #f59e0b; }
.timeline-dot.info { background: #a855f7; }
.timeline-dot.cyan { background: #22d3ee; }
.timeline-content { flex: 1; }
.timeline-header { display: flex; align-items: center; gap: 10px; margin-bottom: 6px; }
.timeline-resource { font-size: 13px; color: #94a3b8; font-weight: 500; }
.timeline-detail { font-size: 13px; color: #64748b; line-height: 1.5; margin: 0 0 4px; }
.timeline-meta { font-size: 12px; color: #475569; }

/* Dark table */
:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.04) !important; }
</style>
