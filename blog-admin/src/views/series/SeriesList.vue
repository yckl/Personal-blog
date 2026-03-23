<template>
  <div class="series-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">📚 系列管理</h3>
      <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 添加系列</el-button>
    </div>

    <!-- ===== Card Grid ===== -->
    <div class="series-grid" v-loading="loading">
      <div v-for="s in series" :key="s.id" class="series-card glass-admin" @click="showDialog(s)">
        <div class="card-cover">
          <img v-if="s.coverImage" :src="s.coverImage" />
          <div v-else class="cover-placeholder">📚</div>
          <div class="cover-gradient" />
          <span class="article-count-badge">{{ s.articleCount || 0 }} articles</span>
        </div>
        <div class="card-body">
          <h4 class="card-title">{{ s.name }}</h4>
          <p class="card-desc">{{ s.description || 'No description' }}</p>
          <!-- Progress bar -->
          <div class="progress-wrap">
            <div class="progress-bar" :style="{ width: getProgress(s) + '%' }" />
            <span class="progress-text">{{ getProgress(s) }}% complete</span>
          </div>
        </div>
        <div class="card-actions" @click.stop>
          <el-button type="primary" size="small" text @click="showDialog(s)">编辑</el-button>
          <el-button type="info" size="small" text @click="manageArticles(s)">Articles</el-button>
          <el-popconfirm title="Delete series?" @confirm="handleDelete(s.id)">
            <template #reference><el-button type="danger" size="small" text>删除</el-button></template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑系列' : '添加系列'" width="520px">
      <el-form :model="form" label-width="120px" class="form-spaced">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="别名"><el-input v-model="form.slug" placeholder="自动生成" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="封面图片"><el-input v-model="form.coverImage" placeholder="图片链接" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- Manage Articles Drawer -->
    <el-drawer v-model="articlesDrawerVisible" :title="'系列文章：' + currentSeries?.name" size="520px">
      <div v-if="seriesArticles.length === 0" class="empty-drawer">
        此系列暂无文章，请在文章编辑器中添加。
      </div>
      <div v-for="(a, idx) in seriesArticles" :key="a.articleId" class="series-article-item">
        <span class="series-order">{{ String(idx + 1).padStart(2, '0') }}</span>
        <span class="series-article-title">{{ a.title || 'Article #' + a.articleId }}</span>
        <span class="series-sort">Sort: {{ a.sortOrder }}</span>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSeries, createSeries, updateSeries, deleteSeries } from '../../api/modules'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const series = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', slug: '', description: '', coverImage: '', sortOrder: 0 })

const articlesDrawerVisible = ref(false)
const currentSeries = ref<any>(null)
const seriesArticles = ref<any[]>([])

function getProgress(s: any): number {
  // Placeholder: assume 0% unless custom logic
  return Math.min(100, Math.round((s.articleCount || 0) * 15))
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getSeries()
    const pubRes: any = await request.get('/api/public/series')
    const countMap: Record<number, number> = {}
    for (const s of (pubRes.data || [])) countMap[s.id] = s.articleCount || 0
    series.value = (res.data || []).map((s: any) => ({ ...s, articleCount: countMap[s.id] || 0 }))
  } catch(e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function showDialog(row?: any) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, { name: row.name, slug: row.slug, description: row.description || '', coverImage: row.coverImage || '', sortOrder: row.sortOrder || 0 })
  } else {
    editingId.value = null
    Object.assign(form, { name: '', slug: '', description: '', coverImage: '', sortOrder: 0 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('名称不能为空'); return }
  if (!form.slug.trim()) form.slug = form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  try {
    if (editingId.value) { await updateSeries(editingId.value, form) }
    else { await createSeries(form) }
    ElMessage.success('已保存'); dialogVisible.value = false; loadData()
  } catch(e: any) { ElMessage.error(e.message) }
}

async function handleDelete(id: number) { await deleteSeries(id); ElMessage.success('已删除'); loadData() }

async function manageArticles(s: any) {
  currentSeries.value = s
  try {
    const res: any = await request.get(`/api/series/${s.id}/articles`)
    seriesArticles.value = res.data || []
  } catch { seriesArticles.value = [] }
  articlesDrawerVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.series-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
  transition: all 0.3s;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.form-spaced :deep(.el-form-item) { margin-bottom: 24px; }

/* ===== Card Grid ===== */
.series-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.series-card { padding: 0; overflow: hidden; cursor: pointer; }
.series-card:hover { transform: translateY(-4px); border-color: rgba(168,85,247,0.25); box-shadow: 0 12px 40px rgba(0,0,0,0.3); }

.card-cover { position: relative; height: 140px; background: rgba(15,23,42,0.4); overflow: hidden; }
.card-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  font-size: 40px; background: linear-gradient(135deg, rgba(168,85,247,0.1), rgba(34,211,238,0.1));
}
.cover-gradient {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(15,23,42,0.8) 0%, transparent 60%);
}
.article-count-badge {
  position: absolute; bottom: 8px; left: 12px;
  padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 600;
  background: rgba(168,85,247,0.3); color: #c084fc; backdrop-filter: blur(8px);
}

.card-body { padding: 16px; }
.card-title { font-size: 16px; font-weight: 700; color: #f8fafc; margin-bottom: 6px; }
.card-desc { font-size: 13px; color: #64748b; line-height: 1.5; min-height: 38px; margin-bottom: 12px;
  overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }

/* Progress bar */
.progress-wrap { position: relative; }
.progress-bar {
  height: 4px; border-radius: 2px;
  background: linear-gradient(90deg, #a855f7, #22d3ee);
  transition: width 0.5s;
}
.progress-wrap::before {
  content: ''; display: block; width: 100%; height: 4px;
  background: rgba(255,255,255,0.05); border-radius: 2px;
  position: absolute; top: 0;
}
.progress-text { font-size: 11px; color: #475569; margin-top: 4px; display: block; }

.card-actions { padding: 0 16px 14px; display: flex; gap: 4px; }

/* Drawer articles */
.empty-drawer { text-align: center; color: #475569; padding: 40px; }
.series-article-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-bottom: 1px solid rgba(255,255,255,0.05);
}
.series-order {
  min-width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
  background: linear-gradient(135deg, #a855f7, #22d3ee); color: #fff;
}
.series-article-title { flex: 1; font-size: 14px; color: #f8fafc; }
.series-sort { color: #475569; font-size: 12px; }

/* Responsive */
@media (max-width: 1024px) { .series-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .series-grid { grid-template-columns: 1fr; } }
</style>
