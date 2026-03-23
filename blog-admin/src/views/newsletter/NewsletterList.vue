<template>
  <div class="newsletter-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">📬 邮件通讯</h3>
      <div class="toolbar-right">
        <el-button size="small" @click="sendTestEmail">📧 发送测试邮件</el-button>
        <el-button type="primary" @click="showCreateDialog=true">+ 创建邮件</el-button>
      </div>
    </div>

    <!-- ===== Quick Send Form ===== -->
    <div class="send-form glass-admin" style="margin-bottom: 24px;">
      <div class="form-header">
        <span class="card-title">✉️ 撰写邮件</span>
        <el-button class="test-email-btn" @click="sendTestEmail">🧪 发送测试邮件</el-button>
      </div>
      <el-form :model="form" label-width="100px" style="margin-top: 16px;">
        <el-form-item label="主题"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.contentHtml" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="受众">
          <el-select v-model="form.audienceType" style="width: 200px;">
            <el-option label="All 订阅者管理" value="ALL" />
            <el-option label="Free Only" value="FREE" />
            <el-option label="Premium Only" value="PREMIUM" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="handleCreate">🚀 创建并排队发送</el-button></el-form-item>
      </el-form>
    </div>

    <!-- ===== History Table ===== -->
    <el-tabs v-model="activeTab" class="dark-tabs">
      <!-- Tasks -->
      <el-tab-pane label="Newsletter Tasks" name="tasks">
        <div class="table-wrap glass-admin">
          <el-table :data="tasks" v-loading="loadingTasks"
                    :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                    :row-style="{ height: '56px' }"
                    :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
            <el-table-column prop="subject" label="主题" min-width="200" />
            <el-table-column prop="audienceType" label="受众" width="120">
              <template #default="{ row }">
                <span class="audience-badge">{{ row.audienceType }}</span>
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
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }">
                <span class="time-text">{{ formatTime(row.createdAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status==='PENDING'" type="primary" size="small" text @click="sendNewsletter(row.id)">🚀 Send</el-button>
                <el-button type="info" size="small" text @click="viewStats(row.id)">📊 Stats</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-pagination v-model:current-page="taskPage" :total="taskTotal" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadTasks" />
      </el-tab-pane>

      <!-- Stats -->
      <el-tab-pane label="Send Stats" name="stats">
        <div v-if="selectedStats" class="stats-grid">
          <div class="stat-card glass-admin" v-for="stat in statsCards" :key="stat.label">
            <div class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
        <div v-else class="empty-state glass-admin">
          <span style="font-size: 48px;">📊</span>
          <p>Select a newsletter to view stats</p>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建邮件" width="600">
      <el-form :model="form" label-width="100px">
        <el-form-item label="主题"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.contentHtml" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="受众">
          <el-select v-model="form.audienceType">
            <el-option label="All 订阅者管理" value="ALL" />
            <el-option label="Free Only" value="FREE" />
            <el-option label="Premium Only" value="PREMIUM" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const activeTab = ref('tasks')
const tasks = ref<any[]>([])
const loadingTasks = ref(false)
const taskPage = ref(1)
const taskTotal = ref(0)
const showCreateDialog = ref(false)
const selectedStats = ref<any>(null)
const form = ref({ subject: '', contentHtml: '', audienceType: 'ALL' })

function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }

const statsCards = computed(() => {
  if (!selectedStats.value) return []
  const s = selectedStats.value
  return [
    { label: 'Subject', value: s.subject || '—', color: '#f8fafc' },
    { label: 'Total Sent', value: s.totalSent || 0, color: '#a855f7' },
    { label: 'Opened', value: s.opened || 0, color: '#22d3ee' },
    { label: 'Clicked', value: s.clicked || 0, color: '#10b981' },
  ]
})

async function loadTasks() {
  loadingTasks.value = true
  try {
    const r: any = await request.get('/api/admin/newsletter/page', { params: { page: taskPage.value, size: 20 } })
    tasks.value = r.data?.records || r.data || []
    taskTotal.value = r.data?.total || 0
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loadingTasks.value = false }
}

async function handleCreate() {
  await request.post('/api/admin/newsletter/create', form.value)
  ElMessage.success('已创建')
  showCreateDialog.value = false
  form.value = { subject: '', contentHtml: '', audienceType: 'ALL' }
  loadTasks()
}

async function sendNewsletter(id: number) {
  await request.post(`/api/admin/newsletter/send/${id}`)
  ElMessage.success('发送中...')
  loadTasks()
}

async function sendTestEmail() {
  try {
    await request.post('/api/admin/newsletter/send-test')
    ElMessage.success('测试邮件已发送！')
  } catch (e: any) { ElMessage.error(e.message || '发送测试邮件失败') }
}

async function viewStats(id: number) {
  try {
    const r: any = await request.get(`/api/admin/newsletter/stats/${id}`)
    selectedStats.value = r.data
    activeTab.value = 'stats'
  } catch { selectedStats.value = null }
}

onMounted(loadTasks)
</script>

<style scoped>
.newsletter-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.toolbar-right { display: flex; gap: 8px; }
.table-wrap { padding: 0; overflow: hidden; }

/* Audience badge */
.audience-badge {
  padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600;
  background: rgba(168,85,247,0.1); color: #a855f7;
}

/* Status */
.status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-badge.sent { background: rgba(16,185,129,0.1); color: #10b981; }
.status-badge.sent .status-dot { background: #10b981; }
.status-badge.sending { background: rgba(245,158,11,0.1); color: #f59e0b; }
.status-badge.sending .status-dot { background: #f59e0b; }
.status-badge.pending { background: rgba(148,163,184,0.1); color: #94a3b8; }
.status-badge.pending .status-dot { background: #94a3b8; }
.time-text { font-size: 13px; color: #475569; }

/* Stats grid */
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: 800; margin-bottom: 4px; }
.stat-label { font-size: 13px; color: #94a3b8; }
.empty-state { text-align: center; padding: 48px; }
.empty-state p { color: #475569; margin-top: 12px; }

/* Dark table */
:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
/* Test email glow button */
.test-email-btn {
  background: linear-gradient(135deg, #a855f7, #22d3ee) !important;
  border: none !important; color: #fff !important; font-weight: 600;
  box-shadow: 0 0 20px rgba(168,85,247,0.2);
  transition: all 0.25s;
}
.test-email-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(168,85,247,0.4); }
.form-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 16px; font-weight: 700; color: #f8fafc; }
</style>
