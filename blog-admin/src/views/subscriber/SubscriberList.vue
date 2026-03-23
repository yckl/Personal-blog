<template>
  <div class="subscriber-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">📧 订阅者管理</h3>
      <div class="toolbar-right">
        <el-input v-model="keyword" placeholder="搜索邮箱..." clearable style="width: 220px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px" @change="loadData">
          <el-option label="活跃" value="ACTIVE" />
          <el-option label="Unsubscribed" value="UNSUBSCRIBED" />
        </el-select>
        <el-button type="primary" @click="handleExportCSV">📥 导出 CSV</el-button>
      </div>
    </div>

    <!-- Batch bar -->
    <div v-if="selectedIds.length > 0" class="batch-bar glass-admin">
      <span class="batch-count">{{ selectedIds.length }} selected</span>
      <el-popconfirm title="Unsubscribe selected?" @confirm="handleBatchUnsubscribe">
        <template #reference><el-button type="warning" size="small">批量退订</el-button></template>
      </el-popconfirm>
      <el-popconfirm title="Remove selected?" @confirm="handleBatchDelete">
        <template #reference><el-button type="danger" size="small">批量删除</el-button></template>
      </el-popconfirm>
    </div>

    <div class="table-wrap glass-admin">
      <el-table :data="subscribers" v-loading="loading" @selection-change="handleSelectionChange"
                :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                :row-style="{ height: '56px' }"
                :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="email" label="邮箱" min-width="240">
          <template #default="{ row }">
            <span class="email-text">{{ row.email }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <span class="name-text">{{ row.name || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <span :class="['status-badge', row.status?.toLowerCase()]">
              <span class="status-dot" />
              {{ row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="订阅时间" width="170">
          <template #default="{ row }">
            <span class="time-text">{{ formatTime(row.subscribedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="Remove subscriber?" @confirm="handleDelete(row.id)">
              <template #reference><el-button type="danger" size="small" text>移除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next"
                   style="margin-top: 20px; justify-content: flex-end;" @current-change="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSubscribers, deleteSubscriber } from '../../api/modules'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const subscribers = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('')
const selectedIds = ref<number[]>([])

function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function loadData() {
  loading.value = true
  try {
    const r: any = await getSubscribers({
      page: page.value, size: 20,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined
    })
    subscribers.value = r.data.records || r.data || []
    total.value = r.data.total || 0
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

async function handleDelete(id: number) { await deleteSubscriber(id); ElMessage.success('Removed'); loadData() }

async function handleBatchUnsubscribe() {
  try {
    await request.post('/api/admin/subscribers/batch-unsubscribe', { ids: selectedIds.value })
    ElMessage.success(`${selectedIds.value.length} unsubscribed`)
    loadData()
  } catch (e: any) { ElMessage.error(e.message) }
}

async function handleBatchDelete() {
  try {
    await request.post('/api/admin/subscribers/batch-delete', { ids: selectedIds.value })
    ElMessage.success(`${selectedIds.value.length} removed`)
    loadData()
  } catch (e: any) { ElMessage.error(e.message) }
}

function handleExportCSV() {
  const header = 'Email,Name,Status,Subscribed At\n'
  const csv = header + subscribers.value.map(s =>
    `${s.email},${s.name || ''},${s.status},${s.subscribedAt || ''}`
  ).join('\n')
  const blob = new Blob([csv], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = 'subscribers.csv'; a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('CSV exported!')
}

onMounted(loadData)
</script>

<style scoped>
.subscriber-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.toolbar-right { display: flex; gap: 8px; align-items: center; }
.table-wrap { padding: 0; overflow: hidden; }

/* Batch bar */
.batch-bar {
  margin-bottom: 16px; display: flex; align-items: center; gap: 12px;
  padding: 12px 20px;
}
.batch-count { font-size: 13px; font-weight: 600; color: #c084fc; }

/* Cells */
.email-text { font-weight: 500; color: #f8fafc; }
.name-text { color: #94a3b8; }
.time-text { font-size: 13px; color: #475569; }

/* Status */
.status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-badge.active { background: rgba(16,185,129,0.1); color: #10b981; }
.status-badge.active .status-dot { background: #10b981; }
.status-badge.unsubscribed { background: rgba(148,163,184,0.1); color: #94a3b8; }
.status-badge.unsubscribed .status-dot { background: #94a3b8; }

/* Dark table */
:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.04) !important; }
</style>
