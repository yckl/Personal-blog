<template>
  <div class="comment-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">💬 评论管理</h3>
      <el-select v-model="statusFilter" placeholder="按状态筛选" clearable style="width:160px" @change="loadData">
        <el-option label="Pending" value="PENDING" />
        <el-option label="Approved" value="APPROVED" />
        <el-option label="Rejected" value="REJECTED" />
      </el-select>
    </div>

    <div class="table-wrap glass-admin">
      <el-table :data="comments" v-loading="loading"
                :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                :row-style="{ height: '80px' }"
                :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
        <el-table-column prop="authorName" label="作者" width="140">
          <template #default="{ row }">
            <div class="author-cell">
              <el-avatar :size="28" :src="row.authorAvatar" class="author-avatar">
                {{ (row.authorName || '?')[0] }}
              </el-avatar>
              <span class="author-name">{{ row.authorName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="文章" width="180">
          <template #default="{ row }">
            <span class="article-link" @click="$router.push(`/articles/edit/${row.articleId}`)">
              {{ row.articleTitle || 'Untitled' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="400">
          <template #default="{ row }">
            <div class="comment-content" :class="{ expanded: expandedIds.has(row.id) }">
              {{ row.content }}
            </div>
            <el-button v-if="row.content && row.content.length > 80" type="primary" size="small" text
                       @click="toggleExpand(row.id)">
              {{ expandedIds.has(row.id) ? 'Collapse' : 'Expand' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
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
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="row.status !== 'APPROVED'" type="success" size="small" text @click="handleApprove(row.id)">✓ Approve</el-button>
              <el-button v-if="row.status !== 'REJECTED'" type="warning" size="small" text @click="handleReject(row.id)">✗ Reject</el-button>
              <el-button type="info" size="small" text @click="openReplyDialog(row)">↩ Reply</el-button>
              <el-popconfirm title="Delete this comment?" @confirm="handleDelete(row.id)">
                <template #reference><el-button type="danger" size="small" text>删除</el-button></template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next"
                   style="margin-top: 20px; justify-content: flex-end;" @current-change="loadData" />

    <!-- Reply Dialog -->
    <el-dialog v-model="replyDialogVisible" title="回复评论" width="480px">
      <div v-if="replyTarget" class="reply-quote">
        <strong>{{ replyTarget.authorName }}</strong>: "{{ replyTarget.content }}"
      </div>
      <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="写你的回复..." />
      <template #footer>
        <el-button @click="replyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="handleReply">发送回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getComments, approveComment, rejectComment, deleteComment } from '../../api/modules'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const comments = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const statusFilter = ref('')
const expandedIds = ref(new Set<number>())
const replyDialogVisible = ref(false)
const replyTarget = ref<any>(null)
const replyContent = ref('')
const replying = ref(false)

function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }
function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) expandedIds.value.delete(id)
  else expandedIds.value.add(id)
}

async function loadData() {
  loading.value = true
  try {
    const r: any = await getComments({ page: page.value, size: 20, status: statusFilter.value || undefined })
    comments.value = r.data.records || []
    total.value = r.data.total
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

async function handleApprove(id: number) { await approveComment(id); ElMessage.success('Approved'); loadData() }
async function handleReject(id: number) { await rejectComment(id); ElMessage.success('Rejected'); loadData() }
async function handleDelete(id: number) { await deleteComment(id); ElMessage.success('已删除'); loadData() }

function openReplyDialog(comment: any) {
  replyTarget.value = comment
  replyContent.value = ''
  replyDialogVisible.value = true
}

async function handleReply() {
  if (!replyContent.value.trim()) { ElMessage.warning('回复不能为空'); return }
  replying.value = true
  try {
    await request.post(`/api/admin/comments/${replyTarget.value.id}/reply`, { content: replyContent.value })
    ElMessage.success('回复已发送')
    replyDialogVisible.value = false
    loadData()
  } catch (e: any) {
    ElMessage.error(e.message || '回复失败')
  } finally { replying.value = false }
}

onMounted(loadData)
</script>

<style scoped>
.comment-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.table-wrap { padding: 0; overflow: hidden; }

/* Author */
.author-cell { display: flex; align-items: center; gap: 8px; }
.author-name { font-weight: 500; color: #f8fafc; font-size: 13px; }

/* Article link */
.article-link { color: #a855f7; cursor: pointer; font-size: 13px; }
.article-link:hover { text-decoration: underline; }

/* Content */
.comment-content {
  font-size: 13px; color: #94a3b8; line-height: 1.6;
  max-height: 60px; overflow: hidden; transition: max-height 0.3s;
}
.comment-content.expanded { max-height: 400px; }

/* Status */
.status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-badge.approved { background: rgba(16,185,129,0.1); color: #10b981; }
.status-badge.approved .status-dot { background: #10b981; }
.status-badge.pending { background: rgba(245,158,11,0.1); color: #f59e0b; }
.status-badge.pending .status-dot { background: #f59e0b; }
.status-badge.rejected { background: rgba(244,63,94,0.1); color: #f43f5e; }
.status-badge.rejected .status-dot { background: #f43f5e; }

.time-text { font-size: 13px; color: #475569; }
.action-btns { display: flex; gap: 2px; align-items: center; flex-wrap: wrap; }

/* Reply */
.reply-quote {
  padding: 12px 16px; border-radius: 10px; margin-bottom: 16px;
  background: rgba(168,85,247,0.06); border: 1px solid rgba(168,85,247,0.1);
  font-size: 13px; color: #94a3b8; line-height: 1.6;
}
.reply-quote strong { color: #f8fafc; }

/* Dark table */
:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.06) !important; }

/* Gradient action buttons */
:deep(.el-button--success) { background: linear-gradient(135deg, #10b981, #22d3ee) !important; border: none !important; color: #fff !important; }
:deep(.el-button--warning) { background: linear-gradient(135deg, #f59e0b, #f43f5e) !important; border: none !important; color: #fff !important; }
:deep(.el-button--success.is-text), :deep(.el-button--warning.is-text) { background: transparent !important; }
:deep(.el-button--success.is-text:hover) { color: #10b981 !important; background: rgba(16,185,129,0.1) !important; }
:deep(.el-button--warning.is-text:hover) { color: #f59e0b !important; background: rgba(245,158,11,0.1) !important; }
</style>
