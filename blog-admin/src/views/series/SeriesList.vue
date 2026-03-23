<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px;">
      <h3>📚 Series</h3>
      <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> Add Series</el-button>
    </div>

    <el-row :gutter="20">
      <el-col v-for="s in series" :key="s.id" :span="8" style="margin-bottom: 20px;">
        <el-card shadow="hover" style="border-radius: 12px;">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <span style="font-weight: 600;">{{ s.name }}</span>
              <el-tag size="small">{{ s.articleCount || 0 }} articles</el-tag>
            </div>
          </template>
          <div v-if="s.coverImage" style="margin-bottom: 12px;">
            <img :src="s.coverImage" style="width: 100%; max-height: 120px; object-fit: cover; border-radius: 8px;" />
          </div>
          <p style="color: #666; font-size: 13px; min-height: 40px;">{{ s.description || 'No description' }}</p>
          <div style="display: flex; gap: 8px; margin-top: 12px;">
            <el-button size="small" text type="primary" @click="showDialog(s)">Edit</el-button>
            <el-button size="small" text type="info" @click="manageArticles(s)">Articles</el-button>
            <el-popconfirm title="Delete series and all relations?" @confirm="handleDelete(s.id)">
              <template #reference><el-button size="small" text type="danger">Delete</el-button></template>
            </el-popconfirm>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Create/Edit Series Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? 'Edit Series' : 'Add Series'" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="Name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="auto-from-name" /></el-form-item>
        <el-form-item label="Description"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="Cover Image"><el-input v-model="form.coverImage" placeholder="Image URL" /></el-form-item>
        <el-form-item label="Sort Order"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSave">Save</el-button>
      </template>
    </el-dialog>

    <!-- Manage Articles in Series Drawer -->
    <el-drawer v-model="articlesDrawerVisible" :title="'Articles in: ' + currentSeries?.name" size="520px">
      <div v-if="seriesArticles.length === 0" style="text-align: center; color: #c0c4cc; padding: 40px;">
        No articles in this series. Add from the article editor.
      </div>
      <div v-for="(a, idx) in seriesArticles" :key="a.articleId" class="series-article-item">
        <span class="series-order">{{ idx + 1 }}</span>
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

async function loadData() {
  loading.value = true
  try {
    const res: any = await getSeries()
    // Also get article counts from public API
    const pubRes: any = await request.get('/api/public/series')
    const countMap: Record<number, number> = {}
    for (const s of (pubRes.data || [])) {
      countMap[s.id] = s.articleCount || 0
    }
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
  if (!form.name.trim()) { ElMessage.warning('Name is required'); return }
  if (!form.slug.trim()) form.slug = form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  try {
    if (editingId.value) { await updateSeries(editingId.value, form) }
    else { await createSeries(form) }
    ElMessage.success('Saved'); dialogVisible.value = false; loadData()
  } catch(e: any) { ElMessage.error(e.message) }
}

async function handleDelete(id: number) {
  await deleteSeries(id); ElMessage.success('Deleted'); loadData()
}

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
.series-article-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-bottom: 1px solid #f0f0f0;
}
.series-order {
  width: 28px; height: 28px; background: #409eff; color: white;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 600; flex-shrink: 0;
}
.series-article-title { flex: 1; font-size: 14px; }
.series-sort { color: #999; font-size: 12px; }
</style>
