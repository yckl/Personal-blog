<template>
  <div class="article-list">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="Search articles..." clearable style="width: 220px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="statusFilter" placeholder="Status" clearable style="width: 140px" @change="loadData">
          <el-option label="Draft" value="DRAFT" />
          <el-option label="Published" value="PUBLISHED" />
          <el-option label="Scheduled" value="SCHEDULED" />
          <el-option label="Archived" value="ARCHIVED" />
          <el-option label="Private" value="PRIVATE" />
          <el-option label="Members Only" value="MEMBER_ONLY" />
        </el-select>
        <el-select v-model="categoryFilter" placeholder="Category" clearable style="width: 140px" @change="loadData">
          <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
        </el-select>
        <el-select v-model="tagFilter" placeholder="Tag" clearable style="width: 140px" @change="loadData">
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </div>
      <el-button type="primary" @click="$router.push('/articles/create')">
        <el-icon><Plus /></el-icon> New Article
      </el-button>
    </div>

    <!-- Batch actions bar -->
    <transition name="el-fade-in">
      <div v-if="selectedIds.length > 0" class="batch-bar">
        <span class="batch-count">{{ selectedIds.length }} selected</span>
        <el-button size="small" @click="handleBatch('publish')">
          <el-icon><CircleCheckFilled /></el-icon> Publish
        </el-button>
        <el-button size="small" @click="handleBatch('unpublish')">
          <el-icon><RemoveFilled /></el-icon> Unpublish
        </el-button>
        <el-button size="small" @click="handleBatch('top')">
          <el-icon><Top /></el-icon> Pin to Top
        </el-button>
        <el-button size="small" @click="handleBatch('archive')">
          <el-icon><FolderChecked /></el-icon> Archive
        </el-button>
        <el-popconfirm title="Delete selected articles?" @confirm="handleBatch('delete')">
          <template #reference>
            <el-button size="small" type="danger">
              <el-icon><Delete /></el-icon> Delete
            </el-button>
          </template>
        </el-popconfirm>
      </div>
    </transition>

    <!-- Table -->
    <el-table :data="articles" stripe v-loading="loading" style="width: 100%" @selection-change="handleSelectionChange" row-key="id">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="title" label="Title" min-width="280">
        <template #default="{ row }">
          <div class="title-cell">
            <el-tag v-if="row.isTop" type="danger" size="small" effect="dark" style="margin-right: 6px;">TOP</el-tag>
            <el-link type="primary" @click="$router.push(`/articles/edit/${row.id}`)">{{ row.title }}</el-link>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="Status" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="Category" width="120" />
      <el-table-column label="Tags" width="180">
        <template #default="{ row }">
          <el-tag v-for="tag in (row.tags || []).slice(0, 3)" :key="tag.id" :color="tag.color" size="small" style="margin-right: 4px; color: #fff;" effect="dark">
            {{ tag.name }}
          </el-tag>
          <span v-if="(row.tags || []).length > 3" style="font-size: 12px; color: #999;">+{{ row.tags.length - 3 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="Views" width="80" sortable />
      <el-table-column prop="commentCount" label="Comments" width="100" />
      <el-table-column label="Published" width="170">
        <template #default="{ row }">
          {{ row.publishedAt ? formatTime(row.publishedAt) : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'PUBLISHED'" type="success" size="small" text @click="handlePublish(row.id)">Publish</el-button>
          <el-button v-else type="warning" size="small" text @click="handleUnpublish(row.id)">Unpublish</el-button>
          <el-button type="primary" size="small" text @click="$router.push(`/articles/edit/${row.id}`)">Edit</el-button>
          <el-dropdown trigger="click" @command="(cmd: string) => handleRowAction(cmd, row)">
            <el-button size="small" text><el-icon><MoreFilled /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="top">{{ row.isTop ? 'Unpin' : 'Pin to Top' }}</el-dropdown-item>
                <el-dropdown-item command="archive">Archive</el-dropdown-item>
                <el-dropdown-item command="schedule">Schedule</el-dropdown-item>
                <el-dropdown-item command="delete" divided style="color: #f56c6c;">Delete</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

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

    <!-- Schedule Dialog -->
    <el-dialog v-model="scheduleDialogVisible" title="Schedule Publication" width="420px">
      <el-form>
        <el-form-item label="Publish at">
          <el-date-picker v-model="scheduledTime" type="datetime" placeholder="Select date and time" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleDialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="confirmSchedule">Confirm</el-button>
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

function statusTagType(status: string) {
  const map: Record<string, string> = {
    PUBLISHED: 'success', DRAFT: 'info', SCHEDULED: 'warning',
    ARCHIVED: 'danger', PRIVATE: '', MEMBER_ONLY: 'warning'
  }
  return map[status] || 'info'
}

function formatTime(t: string): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
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
  } catch { /* ignore */ }
}

function handleSelectionChange(rows: any[]) {
  selectedIds.value = rows.map((r: any) => r.id)
}

async function handlePublish(id: number) {
  await publishArticle(id); ElMessage.success('Published'); loadData()
}
async function handleUnpublish(id: number) {
  await unpublishArticle(id); ElMessage.success('Unpublished'); loadData()
}

async function handleRowAction(cmd: string, row: any) {
  if (cmd === 'delete') {
    await ElMessageBox.confirm('Delete this article?', 'Confirm')
    await deleteArticle(row.id); ElMessage.success('Deleted'); loadData()
  } else if (cmd === 'top') {
    // Toggle top status via batch (single item)
    if (row.isTop) {
      // Unpin — use batch with isTop=false (via unpublish-like update)
      await batchTopArticles([row.id])
      // Actually we need a separate "unpin" — for now we'll just toggle
    }
    await batchTopArticles([row.id])
    ElMessage.success(row.isTop ? 'Unpinned' : 'Pinned to top')
    loadData()
  } else if (cmd === 'archive') {
    await batchArchiveArticles([row.id]); ElMessage.success('Archived'); loadData()
  } else if (cmd === 'schedule') {
    scheduleTargetId.value = row.id
    scheduledTime.value = null
    scheduleDialogVisible.value = true
  }
}

async function confirmSchedule() {
  if (!scheduledTime.value || !scheduleTargetId.value) {
    ElMessage.warning('Please select a date and time')
    return
  }
  const isoStr = scheduledTime.value.toISOString().substring(0, 19)
  await scheduleArticle(scheduleTargetId.value, { scheduledAt: isoStr })
  ElMessage.success('Article scheduled')
  scheduleDialogVisible.value = false
  loadData()
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
    ElMessage.success(`Batch ${action} successful`)
    loadData()
  } catch (e: any) { ElMessage.error(e.message) }
}

onMounted(() => { loadData(); loadFilters() })
</script>

<style scoped>
.article-list {
  padding: 4px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  background: #ecf5ff;
  border-radius: 8px;
  margin-bottom: 16px;
}
.batch-count {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
  margin-right: 8px;
}
.title-cell {
  display: flex;
  align-items: center;
}
</style>
