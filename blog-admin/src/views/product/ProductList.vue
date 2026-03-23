<template>
  <div>
    <h3 style="margin-bottom: 20px;">Digital Products</h3>
    <el-button type="primary" style="margin-bottom:16px;" @click="showDialog=true; editingId=null">Add Product</el-button>
    <el-table :data="products" stripe v-loading="loading">
      <el-table-column label="Cover" width="80">
        <template #default="{ row }"><img v-if="row.coverImage" :src="row.coverImage" style="width:50px;height:50px;object-fit:cover;border-radius:4px;" /><span v-else>—</span></template>
      </el-table-column>
      <el-table-column prop="title" label="Title" min-width="200" />
      <el-table-column prop="productType" label="Type" width="130">
        <template #default="{ row }">
          <el-tag :type="row.productType==='EBOOK'?'':row.productType==='COURSE'?'warning':'success'" size="small">{{ row.productType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Price" width="100">
        <template #default="{ row }">{{ row.priceCents===0?'Free': `$${(row.priceCents/100).toFixed(2)}` }}</template>
      </el-table-column>
      <el-table-column prop="status" label="Status" width="110">
        <template #default="{ row }"><el-tag :type="row.status==='PUBLISHED'?'success':row.status==='DRAFT'?'info':'warning'" size="small">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="downloadCount" label="Downloads" width="100" />
      <el-table-column prop="visibility" label="Visibility" width="120">
        <template #default="{ row }"><el-tag size="small" type="info">{{ row.visibility }}</el-tag></template>
      </el-table-column>
      <el-table-column label="Actions" width="180">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="editProduct(row)">Edit</el-button>
          <el-popconfirm title="Delete this product?" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" size="small" text>Delete</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadData" />

    <el-dialog v-model="showDialog" :title="editingId?'Edit Product':'Add Product'" width="650">
      <el-form :model="form" label-width="120px">
        <el-form-item label="Title"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="Slug"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="Type">
          <el-select v-model="form.productType">
            <el-option label="eBook" value="EBOOK" /><el-option label="Course" value="COURSE" /><el-option label="Resource Pack" value="RESOURCE_PACK" />
          </el-select>
        </el-form-item>
        <el-form-item label="Description"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="Cover Image"><el-input v-model="form.coverImage" placeholder="URL" /></el-form-item>
        <el-form-item label="File URL"><el-input v-model="form.fileUrl" placeholder="Download URL" /></el-form-item>
        <el-form-item label="Preview URL"><el-input v-model="form.previewUrl" placeholder="Preview/Sample URL" /></el-form-item>
        <el-form-item label="Price (cents)"><el-input-number v-model="form.priceCents" :min="0" :step="100" /> <span style="margin-left:8px;color:#999;">{{ form.priceCents===0?'Free': `$${(form.priceCents/100).toFixed(2)}` }}</span></el-form-item>
        <el-form-item label="Status">
          <el-select v-model="form.status"><el-option label="Draft" value="DRAFT" /><el-option label="Published" value="PUBLISHED" /><el-option label="Archived" value="ARCHIVED" /></el-select>
        </el-form-item>
        <el-form-item label="Visibility">
          <el-select v-model="form.visibility"><el-option label="Public" value="PUBLIC" /><el-option label="Member Only" value="MEMBER_ONLY" /><el-option label="Premium Only" value="PREMIUM_ONLY" /></el-select>
        </el-form-item>
        <el-form-item label="Sort Order"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
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
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
const products = ref<any[]>([]); const loading = ref(false); const page = ref(1); const total = ref(0)
const showDialog = ref(false); const editingId = ref<number|null>(null)
const emptyForm = () => ({ title:'',slug:'',productType:'EBOOK',description:'',coverImage:'',fileUrl:'',previewUrl:'',priceCents:0,status:'DRAFT',visibility:'PUBLIC',sortOrder:0 })
const form = ref<any>(emptyForm())

async function loadData() {
  loading.value = true
  try { const r: any = await request.get('/api/admin/products', { params: { page: page.value, size: 20 } }); products.value = r.data?.records || r.data || []; total.value = r.data?.total || 0 }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}
function editProduct(row: any) { editingId.value = row.id; form.value = { ...row }; showDialog.value = true }
async function handleSave() {
  if (editingId.value) { await request.put(`/api/admin/products/${editingId.value}`, form.value) }
  else { await request.post('/api/admin/products', form.value) }
  ElMessage.success(editingId.value ? 'Updated' : 'Created')
  showDialog.value = false; editingId.value = null; form.value = emptyForm(); loadData()
}
async function handleDelete(id: number) { await request.delete(`/api/admin/products/${id}`); ElMessage.success('Deleted'); loadData() }
onMounted(loadData)
</script>
