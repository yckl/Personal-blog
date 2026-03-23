<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px;">
      <h3>📂 Categories</h3>
      <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> Add Category</el-button>
    </div>

    <el-table :data="categoryTree" stripe row-key="id" v-loading="loading" default-expand-all
      :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
      <el-table-column prop="name" label="Name" min-width="200">
        <template #default="{ row }">
          <span :style="{ fontWeight: row.parentId ? 'normal' : '600' }">{{ row.name }}</span>
          <el-tag v-if="row.articleCount" size="small" type="info" style="margin-left: 8px;">{{ row.articleCount }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="slug" label="Slug" width="160" />
      <el-table-column prop="description" label="Description" show-overflow-tooltip />
      <el-table-column prop="sortOrder" label="Sort" width="80" />
      <el-table-column label="Actions" width="180">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="showDialog(row)">Edit</el-button>
          <el-button type="success" size="small" text @click="showDialog(null, row.id)">+ Child</el-button>
          <el-popconfirm title="Delete?" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" size="small" text>Delete</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? 'Edit Category' : 'Add Category'" width="500px">
      <el-form :model="form" label-width="120px">
        <el-form-item label="Name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" placeholder="auto-from-name" /></el-form-item>
        <el-form-item label="Parent">
          <el-select v-model="form.parentId" clearable placeholder="None (top-level)" style="width: 100%">
            <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" :disabled="c.id === editingId" />
          </el-select>
        </el-form-item>
        <el-form-item label="Description"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="Sort Order"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSave">Save</el-button>
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

// Build tree from flat list
const categoryTree = computed(() => {
  const map = new Map<number, any>()
  const roots: any[] = []
  for (const c of categories.value) {
    map.set(c.id, { ...c, children: [] })
  }
  for (const c of categories.value) {
    const node = map.get(c.id)
    if (c.parentId && map.has(c.parentId)) {
      map.get(c.parentId).children.push(node)
    } else {
      roots.push(node)
    }
  }
  return roots
})

const flatCategories = computed(() => categories.value)

async function loadData() {
  loading.value = true
  try {
    const [res, publicRes]: any = await Promise.all([
      getCategories(),
      request.get('/api/public/categories/tree')
    ])
    const countMap: Record<number, number> = {}
    const walk = (nodes: any[]) => {
      for (const n of nodes || []) {
        countMap[n.id] = n.articleCount || 0
        if (n.children?.length) walk(n.children)
      }
    }
    walk(publicRes.data || [])
    categories.value = (res.data || []).map((c: any) => ({ ...c, articleCount: countMap[c.id] || 0 }))
  }
  catch(e: any) { ElMessage.error(e.message) }
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
  if (!form.name.trim()) { ElMessage.warning('Name is required'); return }
  if (!form.slug.trim()) form.slug = form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  try {
    if (editingId.value) { await updateCategory(editingId.value, form) }
    else { await createCategory(form) }
    ElMessage.success('Saved'); dialogVisible.value = false; loadData()
  } catch(e: any) { ElMessage.error(e.message) }
}

async function handleDelete(id: number) {
  await deleteCategory(id); ElMessage.success('Deleted'); loadData()
}

onMounted(loadData)
</script>
