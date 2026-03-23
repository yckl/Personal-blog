<template>
  <div>
    <h3 style="margin-bottom: 20px;">SEO Settings</h3>
    <el-tabs v-model="activeTab">
      <!-- Global SEO Config -->
      <el-tab-pane label="Global Config" name="config">
        <el-form :model="config" label-width="200px" style="max-width:700px;" v-loading="loading">
          <el-form-item label="Site Title"><el-input v-model="config.site_title" /></el-form-item>
          <el-form-item label="Meta Description"><el-input v-model="config.meta_description" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="Default OG Image"><el-input v-model="config.default_og_image" /></el-form-item>
          <el-form-item label="Robots.txt Rules"><el-input v-model="config.robots_custom_rules" type="textarea" :rows="4" /></el-form-item>
          <el-divider>Third-Party Analytics</el-divider>
          <el-form-item label="GA4 Measurement ID"><el-input v-model="config.ga4_measurement_id" placeholder="G-XXXXXXXXXX" /></el-form-item>
          <el-form-item label="Search Console Verification"><el-input v-model="config.google_search_console_verification" /></el-form-item>
          <el-form-item label="Plausible Domain"><el-input v-model="config.plausible_domain" placeholder="myblog.com" /></el-form-item>
          <el-form-item label="Custom Head Scripts"><el-input v-model="config.custom_head_scripts" type="textarea" :rows="4" placeholder="<script>...</script>" /></el-form-item>
          <el-form-item><el-button type="primary" @click="saveConfig">Save All</el-button></el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Redirects -->
      <el-tab-pane label="301 Redirects" name="redirects">
        <el-button type="primary" style="margin-bottom:16px" @click="showRedirectDialog=true">Add Redirect</el-button>
        <el-table :data="redirects" stripe>
          <el-table-column prop="oldPath" label="Old Path" min-width="200" />
          <el-table-column prop="newPath" label="New Path" min-width="200" />
          <el-table-column prop="hitCount" label="Hits" width="80" />
          <el-table-column prop="isActive" label="Active" width="80">
            <template #default="{ row }"><el-tag :type="row.isActive?'success':'info'" size="small">{{ row.isActive ? 'Yes' : 'No' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="Actions" width="120">
            <template #default="{ row }">
              <el-popconfirm title="Delete redirect?" @confirm="deleteRedirect(row.id)">
                <template #reference><el-button type="danger" size="small" text>Delete</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showRedirectDialog" title="Add Redirect" width="500">
      <el-form :model="redirectForm" label-width="100px">
        <el-form-item label="Old Path"><el-input v-model="redirectForm.oldPath" placeholder="/old-article-slug" /></el-form-item>
        <el-form-item label="New Path"><el-input v-model="redirectForm.newPath" placeholder="/new-article-slug" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRedirectDialog=false">Cancel</el-button>
        <el-button type="primary" @click="addRedirect">Add</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
const activeTab = ref('config'); const loading = ref(false)
const config = ref<any>({})
const redirects = ref<any[]>([]); const showRedirectDialog = ref(false)
const redirectForm = ref({ oldPath: '', newPath: '' })

async function loadConfig() {
  loading.value = true
  try { const r: any = await request.get('/api/site/seo'); config.value = r.data || {} }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}
async function saveConfig() {
  await request.put('/api/admin/seo/config', config.value); ElMessage.success('Saved')
}
async function loadRedirects() {
  try { const r: any = await request.get('/api/admin/redirects'); redirects.value = r.data || [] }
  catch { redirects.value = [] }
}
async function addRedirect() {
  await request.post('/api/admin/redirect', redirectForm.value); ElMessage.success('Added')
  showRedirectDialog.value = false; redirectForm.value = { oldPath: '', newPath: '' }; loadRedirects()
}
async function deleteRedirect(id: number) { await request.delete(`/api/admin/redirect/${id}`); ElMessage.success('Deleted'); loadRedirects() }
onMounted(() => { loadConfig(); loadRedirects() })
</script>
