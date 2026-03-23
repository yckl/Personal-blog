<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px;">
      <h3>Tags</h3>
      <el-button type="primary" @click="showDialog()"><el-icon><Plus /></el-icon> Add Tag</el-button>
    </div>
    <div style="display: flex; flex-wrap: wrap; gap: 12px; margin-bottom: 24px;">
      <el-tag v-for="tag in tags" :key="tag.id" :color="tag.color" size="large" closable
              @close="handleDelete(tag.id)" @click="showDialog(tag)" style="cursor: pointer; font-size:14px;">
        {{ tag.name }} <span v-if="tag.articleCount" style="opacity:0.7;">({{ tag.articleCount }})</span>
      </el-tag>
    </div>
    <el-table :data="tags" stripe v-loading="loading">
      <el-table-column prop="name" label="Name" />
      <el-table-column prop="slug" label="Slug" />
      <el-table-column prop="color" label="Color" width="120">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:8px;">
            <div :style="{ width:'20px',height:'20px',borderRadius:'4px',background: row.color || '#ddd' }" />
            {{ row.color }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="Actions" width="150">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="showDialog(row)">Edit</el-button>
          <el-popconfirm title="Delete?" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" size="small" text>Delete</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? 'Edit Tag' : 'Add Tag'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="Name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="Color"><el-color-picker v-model="form.color" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSave">Save</el-button>
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
const form = reactive({ name: '', slug: '', color: '#409eff' })

async function loadData() {
  loading.value = true
  try {
    const res: any = await getTags()
    // Enrich with article counts
    const pubRes: any = await request.get('/api/public/tags/all')
    const countMap: Record<number, number> = {}
    for (const t of (pubRes.data || [])) countMap[t.id] = t.articleCount || 0
    tags.value = (res.data || []).map((t: any) => ({ ...t, articleCount: countMap[t.id] || 0 }))
  }
  catch(e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function showDialog(row?: any) {
  if (row) { editingId.value = row.id; Object.assign(form, { name: row.name, slug: row.slug, color: row.color || '#409eff' }) }
  else { editingId.value = null; Object.assign(form, { name: '', slug: '', color: '#409eff' }) }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('Name is required'); return }
  if (!form.slug.trim()) form.slug = form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-')
  try {
    if (editingId.value) { await updateTag(editingId.value, form) }
    else { await createTag(form) }
    ElMessage.success('Saved'); dialogVisible.value = false; loadData()
  } catch(e: any) { ElMessage.error(e.message) }
}

async function handleDelete(id: number) { await deleteTag(id); ElMessage.success('Deleted'); loadData() }
onMounted(loadData)
</script>
