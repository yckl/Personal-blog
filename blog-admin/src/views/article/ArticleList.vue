<template>
  <div class="article-list">
    <!-- Toolbar -->
    <div class="toolbar glass-admin">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索文章..." clearable style="width: 220px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px" @change="loadData">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="定时发布" value="SCHEDULED" />
          <el-option label="已归档" value="ARCHIVED" />
          <el-option label="私密" value="PRIVATE" />
          <el-option label="仅会员" value="MEMBER_ONLY" />
        </el-select>
        <el-select v-model="categoryFilter" placeholder="分类" clearable style="width: 140px" @change="loadData">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="tagFilter" placeholder="标签" clearable style="width: 140px" @change="loadData">
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </div>
      <el-button type="primary" @click="$router.push('/articles/create')">
        <el-icon><Plus /></el-icon> 新建文章
      </el-button>
    </div>

    <!-- Batch Bar -->
    <transition name="el-fade-in">
      <div v-if="selectedIds.length > 0" class="batch-bar glass-admin">
        <span class="batch-count">已选 {{ selectedIds.length }} 篇</span>
        <el-button size="small" @click="handleBatch('publish')">
          <el-icon><CircleCheckFilled /></el-icon> 发布
        </el-button>
        <el-button size="small" @click="handleBatch('unpublish')">
          <el-icon><RemoveFilled /></el-icon> 取消发布
        </el-button>
        <el-button size="small" @click="handleBatch('top')">
          <el-icon><Top /></el-icon> 置顶
        </el-button>
        <el-button size="small" @click="handleBatch('archive')">
          <el-icon><FolderChecked /></el-icon> 归档
        </el-button>
        <el-popconfirm title="确定删除选中的文章？" @confirm="handleBatch('delete')">
          <template #reference>
            <el-button size="small" type="danger"><el-icon><Delete /></el-icon> 删除</el-button>
          </template>
        </el-popconfirm>
      </div>
    </transition>

    <!-- ===== Glass Table ===== -->
    <div class="table-wrap glass-admin">
      <el-table :data="articles" v-loading="loading" style="width: 100%" row-key="id"
                @selection-change="handleSelectionChange"
                :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                :row-style="{ height: '72px' }"
                :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag v-if="row.isTop" type="danger" size="small" effect="dark" style="margin-right: 6px; flex-shrink: 0;">TOP</el-tag>
              <el-link type="primary" @click="$router.push(`/articles/edit/${row.id}`)" class="title-link">{{ row.title }}</el-link>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <span :class="['status-badge', row.status?.toLowerCase()]">
              <span class="status-dot" />
              {{ statusMap[row.status] || row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="90" show-overflow-tooltip />
        <el-table-column label="标签" width="140">
          <template #default="{ row }">
            <el-tag v-for="tag in (row.tags || []).slice(0, 2)" :key="tag.id" :color="tag.color" size="small" style="margin-right: 4px; color: #fff;" effect="dark">
              {{ tag.name }}
            </el-tag>
            <span v-if="(row.tags || []).length > 2" style="font-size: 12px; color: #64748b;">+{{ row.tags.length - 2 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="70" sortable />
        <el-table-column prop="commentCount" label="评论" width="70" />
        <el-table-column label="发布时间" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.publishedAt ? formatTime(row.publishedAt) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" size="small" text @click="$router.push(`/articles/edit/${row.id}`)">编辑</el-button>
              <el-button v-if="row.slug" type="info" size="small" text @click="previewArticle(row.slug)">预览</el-button>
              <el-button v-if="row.status !== 'PUBLISHED'" type="success" size="small" text @click="handlePublish(row.id)">发布</el-button>
              <el-button v-else type="warning" size="small" text @click="handleUnpublish(row.id)">取消发布</el-button>
              <el-dropdown trigger="click" @command="(cmd: string) => handleRowAction(cmd, row)">
                <el-button size="small" text><el-icon><MoreFilled /></el-icon></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="top">{{ row.isTop ? '取消置顶' : '置顶' }}</el-dropdown-item>
                    <el-dropdown-item command="archive">归档</el-dropdown-item>
                    <el-dropdown-item command="schedule">定时发布</el-dropdown-item>
                    <el-dropdown-item command="delete" divided style="color: #f43f5e;">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Pagination -->
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px; justify-content: flex-end;"
      @current-change="loadData"
      @size-change="loadData"
    />

    <!-- 定时发布 Dialog -->
    <el-dialog v-model="scheduleDialogVisible" title="定时发布" width="420px">
      <el-form>
        <el-form-item label="发布时间">
          <el-date-picker v-model="scheduledTime" type="datetime" placeholder="选择日期和时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSchedule">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  getArticles, deleteArticle, publishArticle, unpublishArticle, scheduleArticle,
  batchDeleteArticles, batchTopArticles, batchArchiveArticles, batchPublishArticles, batchUnpublishArticles
} from '../../api/article'
import { getCategories, getTags } from '../../api/modules'
import { ElMessage, ElMessageBox } from 'element-plus'

const statusMap: Record<string, string> = { DRAFT: '草稿', PUBLISHED: '已发布', SCHEDULED: '定时发布', ARCHIVED: '已归档', PRIVATE: '私密', MEMBER_ONLY: '仅会员' }

const articles = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const statusFilter = ref('')
const categoryFilter = ref<number | undefined>(undefined)
const tagFilter = ref<number | undefined>(undefined)
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const selectedIds = ref<number[]>([])
const scheduleDialogVisible = ref(false)
const scheduledTime = ref<Date | null>(null)
const scheduleTargetId = ref<number | null>(null)

function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }

function previewArticle(slug: string) {
  window.open(`/article/${slug}`, '_blank')
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getArticles({
      page: page.value, size: size.value,
      status: statusFilter.value || undefined,
      categoryId: categoryFilter.value || undefined,
      tagId: tagFilter.value || undefined,
      keyword: keyword.value || undefined
    })
    articles.value = res.data.records || []
    total.value = res.data.total
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

async function loadFilters() {
  try {
    const [catRes, tagRes]: any = await Promise.all([getCategories(), getTags()])
    categories.value = catRes.data || []
    tags.value = tagRes.data || []
  } catch {}
}

function handleSelectionChange(rows: any[]) { selectedIds.value = rows.map((r: any) => r.id) }
async function handlePublish(id: number) { await publishArticle(id); ElMessage.success('已发布'); loadData() }
async function handleUnpublish(id: number) { await unpublishArticle(id); ElMessage.success('已取消发布'); loadData() }

async function handleRowAction(cmd: string, row: any) {
  if (cmd === 'delete') {
    await ElMessageBox.confirm('确认删除这篇文章吗？', '提示')
    await deleteArticle(row.id); ElMessage.success('已删除'); loadData()
  } else if (cmd === 'top') {
    await batchTopArticles([row.id])
    ElMessage.success(row.isTop ? '已取消置顶' : '已置顶'); loadData()
  } else if (cmd === 'archive') {
    await batchArchiveArticles([row.id]); ElMessage.success('已归档'); loadData()
  } else if (cmd === 'schedule') {
    scheduleTargetId.value = row.id; scheduledTime.value = null; scheduleDialogVisible.value = true
  }
}

async function confirmSchedule() {
  if (!scheduledTime.value || !scheduleTargetId.value) { ElMessage.warning('请选择日期和时间'); return }
  const isoStr = scheduledTime.value.toISOString().substring(0, 19)
  await scheduleArticle(scheduleTargetId.value, { scheduledAt: isoStr })
  ElMessage.success('文章已定时发布'); scheduleDialogVisible.value = false; loadData()
}

async function handleBatch(action: string) {
  const ids = selectedIds.value
  if (!ids.length) return
  try {
    if (action === 'delete') await batchDeleteArticles(ids)
    else if (action === 'top') await batchTopArticles(ids)
    else if (action === 'archive') await batchArchiveArticles(ids)
    else if (action === 'publish') await batchPublishArticles(ids)
    else if (action === 'unpublish') await batchUnpublishArticles(ids)
    ElMessage.success(`Batch ${action} successful`); loadData()
  } catch (e: any) { ElMessage.error(e.message) }
}

onMounted(() => { loadData(); loadFilters() })
</script>

<style scoped>
.article-list { padding: 4px; }

.glass-admin {
  background: rgba(15,23,42,0.6);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1);
  border-radius: 16px;
  padding: 20px;
}

/* Toolbar */
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.toolbar-left { display: flex; gap: 10px; flex-wrap: wrap; }

/* Batch Bar */
.batch-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
.batch-count { font-size: 13px; font-weight: 600; color: #a855f7; margin-right: 8px; }

/* Table */
.table-wrap { padding: 0; overflow: hidden; }
.title-cell { display: flex; align-items: center; min-width: 0; }
.title-link { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* Status Badge */
.status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-badge.published { background: rgba(16,185,129,0.1); color: #10b981; }
.status-badge.published .status-dot { background: #10b981; }
.status-badge.draft { background: rgba(148,163,184,0.1); color: #94a3b8; }
.status-badge.draft .status-dot { background: #94a3b8; }
.status-badge.scheduled { background: rgba(245,158,11,0.1); color: #f59e0b; }
.status-badge.scheduled .status-dot { background: #f59e0b; }
.status-badge.archived { background: rgba(244,63,94,0.1); color: #f43f5e; }
.status-badge.archived .status-dot { background: #f43f5e; }
.status-badge.private { background: rgba(168,85,247,0.1); color: #a855f7; }
.status-badge.private .status-dot { background: #a855f7; }
.status-badge.member_only { background: rgba(34,211,238,0.1); color: #22d3ee; }
.status-badge.member_only .status-dot { background: #22d3ee; }

.action-btns { display: flex; gap: 2px; align-items: center; }

/* Dark overrides for el-table */
:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table .el-table__cell) { color: #94a3b8; }
</style>
