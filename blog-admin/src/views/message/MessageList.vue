<template>
  <div class="message-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">💌 消息留言</h3>
    </div>

    <!-- Filters -->
    <div class="filter-bar glass-admin">
      <el-input v-model="filter.keyword" placeholder="搜索姓名/邮箱/内容" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData" />
      <el-select v-model="filter.status" placeholder="状态" clearable style="width:120px" @change="loadData">
        <el-option label="待回复" value="PENDING" />
        <el-option label="已回复" value="REPLIED" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
    </div>

    <!-- Table -->
    <div class="table-wrap glass-admin">
      <el-table :data="messages" v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="email" label="邮箱" width="180" />
        <el-table-column prop="message" label="留言内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'" size="small">
              {{ row.status === 'PENDING' ? '待回复' : '已回复' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="160">
          <template #default="{ row }"><span class="time-text">{{ formatTime(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">回复</el-button>
            <el-popconfirm title="确定删除吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next" style="margin-top: 20px; justify-content: flex-end;" @current-change="loadData" />
    </div>

    <!-- Reply Dialog -->
    <el-dialog v-model="replyVisible" title="回复消息" width="500px">
      <div v-if="currentMsg" class="msg-detail">
        <p><strong>From:</strong> {{ currentMsg.name }} ({{ currentMsg.email }})</p>
        <p><strong>Message:</strong></p>
        <div class="msg-box">{{ currentMsg.message }}</div>
      </div>
      <el-form :model="replyForm" style="margin-top:20px;">
        <el-form-item label="回复内容" required>
          <el-input type="textarea" v-model="replyForm.replyContent" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const messages = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const filter = ref({ keyword: '', status: '' })

const replyVisible = ref(false)
const submitting = ref(false)
const currentMsg = ref<any>(null)
const replyForm = ref({ replyContent: '' })

function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }

async function loadData() {
  loading.value = true
  const params: any = { page: page.value, size: 10 }
  if (filter.value.keyword) params.keyword = filter.value.keyword
  if (filter.value.status) params.status = filter.value.status
  try {
    const r: any = await request.get('/api/admin/messages', { params })
    messages.value = r.data.records; total.value = r.data.total
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function openReply(row: any) {
  currentMsg.value = row
  replyForm.value.replyContent = row.replyContent || ''
  replyVisible.value = true
}

async function submitReply() {
  if (!replyForm.value.replyContent) return ElMessage.warning('内容不能为空')
  submitting.value = true
  try {
    await request.post(`/api/admin/messages/${currentMsg.value.id}/reply`, { replyContent: replyForm.value.replyContent })
    ElMessage.success('回复成功')
    replyVisible.value = false
    loadData()
  } catch (e: any) { ElMessage.error(e.message) }
  finally { submitting.value = false }
}

async function handleDelete(row: any) {
  try {
    await request.delete(`/api/admin/messages/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) { ElMessage.error(e.message) }
}

onMounted(loadData)
</script>

<style scoped>
.message-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 20px; flex-wrap: wrap; }
.table-wrap { padding: 0; overflow: hidden; }
.time-text { font-size: 13px; color: #475569; }

/* Dark table inherited from AdminLayout global styles */

.msg-detail p { margin-bottom: 8px; color: #94a3b8; }
.msg-detail p strong { color: #f8fafc; }
.msg-box { background: rgba(255,255,255,0.05); padding: 12px; border-radius: 8px; border: 1px solid rgba(255,255,255,0.05); line-height: 1.6; color: #e2e8f0; }
</style>
