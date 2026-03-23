<template>
  <div>
    <h3 style="margin-bottom: 20px;">Menu Settings</h3>
    <el-button type="primary" style="margin-bottom:16px" @click="showDialog=true">Add Menu Item</el-button>
    <el-table :data="menus" stripe row-key="id" v-loading="loading">
      <el-table-column prop="label" label="Label" min-width="150" />
      <el-table-column prop="url" label="URL" min-width="200" />
      <el-table-column prop="target" label="Target" width="100">
        <template #default="{ row }"><el-tag size="small">{{ row.target || '_self' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="Order" width="80" />
      <el-table-column prop="isVisible" label="Visible" width="80">
        <template #default="{ row }"><el-switch v-model="row.isVisible" @change="handleUpdate(row)" /></template>
      </el-table-column>
      <el-table-column label="Actions" width="180">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="editItem(row)">Edit</el-button>
          <el-popconfirm title="Delete menu item?" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" size="small" text>Delete</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editingId?'Edit Menu Item':'Add Menu Item'" width="500">
      <el-form :model="form" label-width="100px">
        <el-form-item label="Label"><el-input v-model="form.label" /></el-form-item>
        <el-form-item label="URL"><el-input v-model="form.url" placeholder="/about or https://..." /></el-form-item>
        <el-form-item label="Target">
          <el-select v-model="form.target"><el-option label="_self" value="_self" /><el-option label="_blank" value="_blank" /></el-select>
        </el-form-item>
        <el-form-item label="Sort Order"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="Icon (optional)"><el-input v-model="form.icon" placeholder="emoji or icon class" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">Cancel</el-button>
        <el-button type="primary" @click="handleSave">{{ editingId ? 'Update' : 'Create' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMenus, createMenu, updateMenu, deleteMenu } from '../../api/modules'
import { ElMessage } from 'element-plus'
const menus = ref<any[]>([]); const loading = ref(false); const showDialog = ref(false); const editingId = ref<number|null>(null)
const form = ref({ label: '', url: '', target: '_self', sortOrder: 0, icon: '' })

async function loadData() { loading.value=true; try{const r:any=await getMenus();menus.value=r.data||[]}catch(e:any){ElMessage.error(e.message)}finally{loading.value=false} }
function editItem(row: any) { editingId.value=row.id; form.value={label:row.label,url:row.url,target:row.target||'_self',sortOrder:row.sortOrder||0,icon:row.icon||''}; showDialog.value=true }
async function handleSave() {
  if(editingId.value){await updateMenu(editingId.value,form.value)}else{await createMenu(form.value)}
  ElMessage.success(editingId.value?'Updated':'Created'); showDialog.value=false; editingId.value=null
  form.value={label:'',url:'',target:'_self',sortOrder:0,icon:''}; loadData()
}
async function handleUpdate(row:any) { await updateMenu(row.id,row); ElMessage.success('Updated') }
async function handleDelete(id:number) { await deleteMenu(id); ElMessage.success('Deleted'); loadData() }
onMounted(loadData)
</script>
