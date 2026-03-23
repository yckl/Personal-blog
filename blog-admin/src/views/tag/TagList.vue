<template>
  <div class="tag-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">🏷️ 标签管理</h3>
      <div class="toolbar-right">
        <el-button :type="viewMode === 'cloud' ? 'primary' : 'default'" size="small" @click="viewMode = 'cloud'">☁️ 云视图</el-button>
        <el-button :type="viewMode === 'table' ? 'primary' : 'default'" size="small" @click="viewMode = 'table'">📋 表格</el-button>
        <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 添加标签</el-button>
      </div>
    </div>

    <!-- ===== Tag Cloud ===== -->
    <div v-if="viewMode === 'cloud'" class="cloud-wrap glass-admin">
      <div class="tag-cloud">
        <span v-for="tag in tags" :key="tag.id"
              class="cloud-tag"
              :style="{
                fontSize: getTagSize(tag.articleCount) + 'px',
                '--tag-color': tag.color || '#a855f7'
              }"
              @click="showDialog(tag)">
          {{ tag.name }}
          <span class="cloud-count">{{ tag.articleCount || 0 }}</span>
        </span>
      </div>
    </div>

    <!-- ===== Tag Table ===== -->
    <div v-else class="table-wrap glass-admin">
      <el-table :data="tags" v-loading="loading"
                :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                :row-style="{ height: '56px' }"
                :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
        <el-table-column prop="name" label="名称" min-width="200">
          <template #default="{ row }">
            <div class="tag-name-cell">
              <span class="tag-dot" :style="{ background: row.color || '#a855f7' }" />
              <span class="tag-label">{{ row.name }}</span>
              <span class="tag-article-count">{{ row.articleCount || 0 }} articles</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="slug" label="别名" width="180" />
        <el-table-column prop="color" label="颜色" width="140">
          <template #default="{ row }">
            <div class="color-cell">
              <div class="color-swatch" :style="{ background: row.color || '#a855f7' }" />
              <span>{{ row.color || '#a855f7' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="showDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button type="danger" size="small" text>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑标签' : '添加标签'" width="440px">
      <el-form :model="form" label-width="80px" class="form-spaced">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="别名"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="颜色"><el-color-picker v-model="form.color" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getTags, createTag, updateTag, deleteTag } from '../../api/modules'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const tags = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', slug: '', color: '#a855f7' })
const viewMode = ref<'cloud' | 'table'>('cloud')

function getTagSize(count: number): number {
  const maxCount = Math.max(...tags.value.map(t => t.articleCount || 0), 1)
  return 14 + (count / maxCount) * 14 // 14px-28px
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getTags()
    const pubRes: any = await request.get('/api/public/tags/all')
    const countMap: Record<number, number> = {}
    for (const t of (pubRes.data || [])) countMap[t.id] = t.articleCount || 0
    tags.value = (res.data || []).map((t: any) => ({ ...t, articleCount: countMap[t.id] || 0 }))
  } catch(e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function showDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, { name: row.name, slug: row.slug, color: row.color || '#a855f7' }) }
  else { editingId.value = null; Object.assign(form, { name: '', slug: '', color: '#a855f7' }) }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('名称不能为空'); return }
  if (!form.slug.trim()) form.slug = form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  try {
    if (editingId.value) { await updateTag(editingId.value, form) }
    else { await createTag(form) }
    ElMessage.success('已保存'); dialogVisible.value = false; loadData()
  } catch(e: any) { ElMessage.error(e.message) }
}

async function handleDelete(id: number) { await deleteTag(id); ElMessage.success('已删除'); loadData() }
onMounted(loadData)
</script>

<style scoped>
.tag-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.toolbar-right { display: flex; gap: 8px; align-items: center; }

/* ===== Tag Cloud ===== */
.cloud-wrap { margin-bottom: 24px; min-height: 200px; }
.tag-cloud {
  display: flex; flex-wrap: wrap; gap: 16px; align-items: center;
  justify-content: center; padding: 24px;
}
.cloud-tag {
  display: inline-flex; align-items: baseline; gap: 4px;
  cursor: pointer; color: var(--tag-color);
  font-weight: 600; transition: all 0.25s;
  padding: 6px 14px; border-radius: 999px;
  background: rgba(255,255,255,0.03);
}
.cloud-tag:hover {
  transform: scale(1.15);
  background: rgba(168,85,247,0.1);
  box-shadow: 0 0 20px color-mix(in srgb, var(--tag-color) 30%, transparent);
}
.cloud-count { font-size: 11px; opacity: 0.5; font-weight: 400; }

/* ===== Tag Table ===== */
.table-wrap { padding: 0; overflow: hidden; }
.tag-name-cell { display: flex; align-items: center; gap: 10px; }
.tag-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.tag-label { font-weight: 600; color: #f8fafc; }
.tag-article-count { font-size: 12px; color: #475569; }
.color-cell { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #64748b; }
.color-swatch { width: 20px; height: 20px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.1); }
.form-spaced :deep(.el-form-item) { margin-bottom: 24px; }

:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.04) !important; }
</style>
