<template>
  <div class="category-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">📂 分类管理</h3>
      <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> 添加分类</el-button>
    </div>

    <div class="table-wrap glass-admin">
      <el-table :data="categoryTree" row-key="id" v-loading="loading" default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
        :row-style="{ height: '56px' }"
        :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
        <el-table-column label="⠿" width="50">
          <template #default>
            <span class="drag-handle">⠿</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="200">
          <template #default="{ row }">
            <span :style="{ fontWeight: row.parentId ? 'normal' : '600', color: row.parentId ? '#94a3b8' : '#f8fafc' }">{{ row.name }}</span>
            <el-tag v-if="row.articleCount" size="small" type="info" style="margin-left: 8px;">{{ row.articleCount }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="slug" label="别名" width="160" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" text @click="showDialog(row)">编辑</el-button>
            <el-button type="success" size="small" text @click="showDialog(null, row.id)">+ 子分类</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference><el-button type="danger" size="small" text>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '添加分类'" width="520px">
      <el-form :model="form" label-width="120px" class="form-spaced">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="别名"><el-input v-model="form.slug" placeholder="自动生成" /></el-form-item>
        <el-form-item label="Parent">
          <el-select v-model="form.parentId" clearable placeholder="无（顶级分类）" style="width: 100%">
            <el-option v-for="c in flat分类管理" :key="c.id" :label="c.name" :value="c.id" :disabled="c.id === editingId" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../../api/modules'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const categories = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({ name: '', slug: '', description: '', sortOrder: 0, parentId: null as number | null })

const categoryTree = computed(() => {
  const map = new Map<number, any>()
  const roots: any[] = []
  for (const c of categories.value) { map.set(c.id, { ...c, children: [] }) }
  for (const c of categories.value) {
    const node = map.get(c.id)
    if (c.parentId && map.has(c.parentId)) { map.get(c.parentId).children.push(node) }
    else { roots.push(node) }
  }
  return roots
})
const flat分类管理 = computed(() => categories.value)

async function loadData() {
  loading.value = true
  try {
    const [res, publicRes]: any = await Promise.all([getCategories(), request.get('/api/public/categories/tree')])
    const countMap: Record<number, number> = {}
    const walk = (nodes: any[]) => { for (const n of nodes || []) { countMap[n.id] = n.articleCount || 0; if (n.children?.length) walk(n.children) } }
    walk(publicRes.data || [])
    categories.value = (res.data || []).map((c: any) => ({ ...c, articleCount: countMap[c.id] || 0 }))
  } catch(e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function showDialog(row?: any, parentId?: number) {
  if (row) {
    editingId.value = row.id
    Object.assign(form, { name: row.name, slug: row.slug, description: row.description || '', sortOrder: row.sortOrder || 0, parentId: row.parentId || null })
  } else {
    editingId.value = null
    Object.assign(form, { name: '', slug: '', description: '', sortOrder: 0, parentId: parentId || null })
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('名称不能为空'); return }
  if (!form.slug.trim()) form.slug = form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  try {
    if (editingId.value) { await updateCategory(editingId.value, form) }
    else { await createCategory(form) }
    ElMessage.success('已保存'); dialogVisible.value = false; loadData()
  } catch(e: any) { ElMessage.error(e.message) }
}

async function handleDelete(id: number) { await deleteCategory(id); ElMessage.success('已删除'); loadData() }
onMounted(loadData)
</script>

<style scoped>
.category-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.table-wrap { padding: 0; overflow: hidden; }
.drag-handle { cursor: grab; font-size: 16px; color: #475569; user-select: none; letter-spacing: -2px; }
.drag-handle:hover { color: #a855f7; }
.form-spaced :deep(.el-form-item) { margin-bottom: 24px; }

:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.04) !important; }
</style>
