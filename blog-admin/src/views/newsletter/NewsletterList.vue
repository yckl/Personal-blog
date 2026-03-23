<template>
  <div>
    <h3 style="margin-bottom: 20px;">Newsletter Management</h3>
    <el-tabs v-model="activeTab">
      <!-- Tasks -->
      <el-tab-pane label="Newsletter Tasks" name="tasks">
        <el-button type="primary" style="margin-bottom:16px" @click="showCreateDialog=true">Create Newsletter</el-button>
        <el-table :data="tasks" stripe v-loading="loadingTasks">
          <el-table-column prop="subject" label="Subject" min-width="200" />
          <el-table-column prop="audienceType" label="Audience" width="120">
            <template #default="{ row }"><el-tag size="small">{{ row.audienceType }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="status" label="Status" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status==='SENT'?'success':row.status==='SENDING'?'warning':'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="Created" width="180" />
          <el-table-column label="Actions" width="200">
            <template #default="{ row }">
              <el-button v-if="row.status==='PENDING'" type="primary" size="small" text @click="sendNewsletter(row.id)">Send</el-button>
              <el-button type="info" size="small" text @click="viewStats(row.id)">Stats</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="taskPage" :total="taskTotal" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadTasks" />
      </el-tab-pane>

      <!-- Stats -->
      <el-tab-pane label="Send Stats" name="stats">
        <div v-if="selectedStats">
          <el-descriptions :column="4" border>
            <el-descriptions-item label="Subject">{{ selectedStats.subject }}</el-descriptions-item>
            <el-descriptions-item label="Total Sent">{{ selectedStats.totalSent }}</el-descriptions-item>
            <el-descriptions-item label="Opened">{{ selectedStats.opened }}</el-descriptions-item>
            <el-descriptions-item label="Clicked">{{ selectedStats.clicked }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <el-empty v-else description="Select a newsletter to view stats" />
      </el-tab-pane>
    </el-tabs>

    <!-- Create Dialog -->
    <el-dialog v-model="showCreateDialog" title="Create Newsletter" width="600">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Subject"><el-input v-model="form.subject" /></el-form-item>
        <el-form-item label="Content"><el-input v-model="form.contentHtml" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="Audience">
          <el-select v-model="form.audienceType">
            <el-option label="All Subscribers" value="ALL" />
            <el-option label="Free Only" value="FREE" />
            <el-option label="Premium Only" value="PREMIUM" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog=false">Cancel</el-button>
        <el-button type="primary" @click="handleCreate">Create</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
const activeTab = ref('tasks')
const tasks = ref<any[]>([]); const loadingTasks = ref(false); const taskPage = ref(1); const taskTotal = ref(0)
const showCreateDialog = ref(false); const selectedStats = ref<any>(null)
const form = ref({ subject: '', contentHtml: '', audienceType: 'ALL' })

async function loadTasks() {
  loadingTasks.value = true
  try { const r: any = await request.get('/api/admin/newsletter/page', { params: { page: taskPage.value, size: 20 } }); tasks.value = r.data?.records || r.data || [] }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loadingTasks.value = false }
}
async function handleCreate() {
  await request.post('/api/admin/newsletter/create', form.value); ElMessage.success('Created')
  showCreateDialog.value = false; form.value = { subject: '', contentHtml: '', audienceType: 'ALL' }; loadTasks()
}
async function sendNewsletter(id: number) { await request.post(`/api/admin/newsletter/send/${id}`); ElMessage.success('Sending...'); loadTasks() }
async function viewStats(id: number) {
  try { const r: any = await request.get(`/api/admin/newsletter/stats/${id}`); selectedStats.value = r.data; activeTab.value = 'stats' }
  catch { selectedStats.value = null }
}
onMounted(loadTasks)
</script>
