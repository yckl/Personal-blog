<template>
  <div class="menu-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">📋 菜单管理</h3>
      <div class="toolbar-right">
        <el-button type="primary" @click="showDialog=true"><el-icon><Plus /></el-icon> Add Item</el-button>
        <el-button @click="handleSaveOrder" :loading="saving">💾 保存排序</el-button>
      </div>
    </div>

    <el-row :gutter="24">
      <!-- Left: Draggable Menu List -->
      <el-col :span="14">
        <div class="menu-list glass-admin" v-loading="loading">
          <div v-for="(item, idx) in menus" :key="item.id || idx" class="menu-item"
               draggable="true" @dragstart="dragStart(idx)" @dragover.prevent @drop="dragDrop(idx)">
            <span class="drag-handle">⠿</span>
            <span v-if="item.icon" class="menu-icon">{{ item.icon }}</span>
            <div class="menu-info">
              <span class="menu-label-text">{{ item.name }}</span>
              <span class="menu-url">{{ item.url }}</span>
            </div>
            <el-tag size="small" class="target-tag">{{ item.target || '_self' }}</el-tag>
            <el-switch v-model="item.visible" size="small" @change="handleUpdate(item)" />
            <div class="menu-actions">
              <el-button type="primary" size="small" text @click="editItem(item)">编辑</el-button>
              <el-popconfirm title="确定删除？" @confirm="handleDelete(item.id)">
                <template #reference><el-button type="danger" size="small" text>删</el-button></template>
              </el-popconfirm>
            </div>
          </div>
          <div v-if="menus.length === 0" class="empty-hint">暂无菜单项。</div>
        </div>
      </el-col>

      <!-- Right: Phone Preview -->
      <el-col :span="10">
        <div class="preview-wrap glass-admin">
          <h4 class="preview-title">📱 预览</h4>
          <div class="phone-shell">
            <div class="phone-notch" />
            <div class="phone-nav">
              <div v-for="item in menus.filter(m => m.visible !== false)" :key="item.id" class="nav-item">
                <span v-if="item.icon" class="nav-icon">{{ item.icon }}</span>
                <span>{{ item.name }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="showDialog" :title="editingId ? '编辑菜单项' : '添加菜单项'" width="500px">
      <el-form :model="form" label-width="110px" class="form-spaced">
        <el-form-item label="标签名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="链接"><el-input v-model="form.url" placeholder="/about 或 https://..." /></el-form-item>
        <el-form-item label="目标">
          <el-select v-model="form.target"><el-option label="_self" value="_self" /><el-option label="_blank" value="_blank" /></el-select>
        </el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="表情或图标类名" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" @click="handleSave">{{ editingId ? '更新' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMenus, createMenu, updateMenu, deleteMenu, batchSortMenus } from '../../api/modules'
import { ElMessage } from 'element-plus'

const menus = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ name: '', url: '', target: '_self', sortOrder: 0, icon: '' })

let dragIdx = -1
function dragStart(idx: number) { dragIdx = idx }
function dragDrop(idx: number) {
  if (dragIdx < 0 || dragIdx === idx) return
  const item = menus.value.splice(dragIdx, 1)[0]
  menus.value.splice(idx, 0, item)
  dragIdx = -1
}

async function loadData() {
  loading.value = true
  try { const r: any = await getMenus(); menus.value = r.data || [] }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function editItem(row: any) {
  editingId.value = row.id
  form.value = { name: row.name, url: row.url, target: row.target || '_self', sortOrder: row.sortOrder || 0, icon: row.icon || '' }
  showDialog.value = true
}

async function handleSave() {
  if (editingId.value) { await updateMenu(editingId.value, form.value) }
  else { await createMenu(form.value) }
  ElMessage.success(editingId.value ? '已更新' : '已创建')
  showDialog.value = false; editingId.value = null
  form.value = { name: '', url: '', target: '_self', sortOrder: 0, icon: '' }
  loadData()
}

async function handleSaveOrder() {
  saving.value = true
  try {
    const sortItems = menus.value.map((m, i) => ({ id: m.id, sortOrder: i }))
    await batchSortMenus(sortItems)
    ElMessage.success('排序已保存')
  } catch (e: any) { ElMessage.error(e.message) }
  finally { saving.value = false }
}

async function handleUpdate(row: any) { await updateMenu(row.id, row); ElMessage.success('已更新') }
async function handleDelete(id: number) { await deleteMenu(id); ElMessage.success('已删除'); loadData() }
onMounted(loadData)
</script>

<style scoped>
.menu-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.toolbar-right { display: flex; gap: 8px; }
.form-spaced :deep(.el-form-item) { margin-bottom: 24px; }

/* Menu items */
.menu-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; border-radius: 12px; margin-bottom: 8px; cursor: grab;
  background: rgba(15,23,42,0.4); border: 1px solid rgba(255,255,255,0.05);
  transition: all 0.25s;
}
.menu-item:hover { border-color: rgba(168,85,247,0.15); background: rgba(168,85,247,0.06); }
.drag-handle { color: #475569; font-size: 16px; user-select: none; letter-spacing: -2px; }
.drag-handle:hover { color: #a855f7; }
.menu-icon { font-size: 18px; }
.menu-info { flex: 1; min-width: 0; }
.menu-label-text { font-weight: 600; color: #f8fafc; font-size: 14px; display: block; }
.menu-url { font-size: 12px; color: #475569; }
.target-tag { font-size: 10px; }
.menu-actions { display: flex; gap: 2px; }
.empty-hint { text-align: center; color: #475569; padding: 40px; }

/* Phone preview */
.preview-wrap { text-align: center; }
.preview-title { font-size: 15px; font-weight: 700; color: #f8fafc; margin: 0 0 16px; }
.phone-shell {
  width: 260px; margin: 0 auto; border-radius: 28px;
  background: rgba(15,23,42,0.8); border: 2px solid rgba(168,85,247,0.15);
  padding: 32px 16px 24px; min-height: 380px;
  box-shadow: 0 0 40px rgba(168,85,247,0.08);
}
.phone-notch {
  width: 80px; height: 6px; border-radius: 3px; margin: 0 auto 20px;
  background: rgba(255,255,255,0.1);
}
.phone-nav { display: flex; flex-direction: column; gap: 4px; }
.nav-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: 10px; font-size: 13px;
  color: #94a3b8; transition: all 0.2s; cursor: default;
}
.nav-item:hover { background: rgba(168,85,247,0.08); color: #c084fc; }
.nav-icon { font-size: 15px; }
</style>
