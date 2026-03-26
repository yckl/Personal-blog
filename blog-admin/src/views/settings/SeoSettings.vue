<template>
  <div class="seo-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">🔍 搜索引擎优化</h3>
      <el-button type="primary" @click="saveConfig"><el-icon><Check /></el-icon> 保存全部</el-button>
    </div>

    <!-- SEO Card Groups -->
    <div class="seo-grid" v-loading="loading">
      <div class="seo-card glass-admin">
        <h4 class="card-label">📝 Meta 标签管理</h4>
        <el-form :model="config" label-width="160px">
          <el-form-item label="站点标题"><el-input v-model="config.site_title" /></el-form-item>
          <el-form-item label="Meta 描述"><el-input v-model="config.meta_description" type="textarea" :rows="3" /></el-form-item>
          <el-form-item label="默认 OG 图片"><el-input v-model="config.default_og_image" /></el-form-item>
        </el-form>
      </div>

      <div class="seo-card glass-admin">
        <h4 class="card-label">🤖 爬虫与索引</h4>
        <el-form :model="config" label-width="160px">
          <el-form-item label="Robots.txt 规则"><el-input v-model="config.robots_custom_rules" type="textarea" :rows="4" /></el-form-item>
          <el-form-item label="站点地图">
            <el-button class="sitemap-btn" @click="generateSitemap" :loading="generating">🗺️ 生成站点地图</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="seo-card glass-admin">
        <h4 class="card-label">📊 分析统计</h4>
        <el-form :model="config" label-width="200px">
          <el-form-item label="GA4 衡量 ID"><el-input v-model="config.ga4_measurement_id" placeholder="G-XXXXXXXXXX" /></el-form-item>
          <el-form-item label="Search Console 密钥"><el-input v-model="config.google_search_console_verification" /></el-form-item>
          <el-form-item label="Plausible 域名"><el-input v-model="config.plausible_domain" placeholder="myblog.com" /></el-form-item>
        </el-form>
      </div>

      <div class="seo-card glass-admin">
        <h4 class="card-label">💻 自定义脚本</h4>
        <el-form :model="config" label-width="160px">
          <el-form-item label="头部脚本"><el-input v-model="config.custom_head_scripts" type="textarea" :rows="4" :placeholder="'<script>...</script>'" /></el-form-item>
        </el-form>
      </div>
    </div>

    <!-- Redirects -->
    <div class="seo-card glass-admin" style="margin-top: 24px;">
      <div class="card-header-bar">
        <h4 class="card-label">↪️ 301 重定向</h4>
        <el-button type="primary" size="small" @click="showRedirectDialog=true">+ 添加重定向</el-button>
      </div>
      <div class="table-wrap">
        <el-table :data="redirects"
                  :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '48px' }"
                  :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
          <el-table-column prop="oldPath" label="旧路径" min-width="200" />
          <el-table-column prop="newPath" label="新路径" min-width="200" />
          <el-table-column prop="hitCount" label="命中" width="80" />
          <el-table-column prop="isActive" label="活跃" width="80">
            <template #default="{ row }">
              <span :class="['status-dot-sm', row.isActive ? 'active' : 'inactive']" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-popconfirm title="确定删除？" @confirm="deleteRedirect(row.id)">
                <template #reference><el-button type="danger" size="small" text>删除</el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="showRedirectDialog" title="添加重定向" width="500px">
      <el-form :model="redirectForm" label-width="100px">
        <el-form-item label="旧路径"><el-input v-model="redirectForm.oldPath" placeholder="/旧路径" /></el-form-item>
        <el-form-item label="新路径"><el-input v-model="redirectForm.newPath" placeholder="/新路径" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRedirectDialog=false">取消</el-button>
        <el-button type="primary" @click="addRedirect">添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const generating = ref(false)
const config = ref<any>({})
const redirects = ref<any[]>([])
const showRedirectDialog = ref(false)
const redirectForm = ref({ oldPath: '', newPath: '' })

async function loadConfig() {
  loading.value = true
  try { const r: any = await request.get('/api/site/seo'); config.value = r.data || {} }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}
async function saveConfig() {
  await request.put('/api/admin/seo/config', config.value); ElMessage.success('已保存')
}
async function generateSitemap() {
  generating.value = true
  try { await request.post('/api/admin/seo/sitemap'); ElMessage.success('站点地图已生成！') }
  catch (e: any) { ElMessage.error(e.message || '生成失败') }
  finally { generating.value = false }
}
async function loadRedirects() {
  try { const r: any = await request.get('/api/admin/redirects'); redirects.value = r.data || [] }
  catch { redirects.value = [] }
}
async function addRedirect() {
  await request.post('/api/admin/redirect', redirectForm.value); ElMessage.success('已添加')
  showRedirectDialog.value = false; redirectForm.value = { oldPath: '', newPath: '' }; loadRedirects()
}
async function deleteRedirect(id: number) { await request.delete(`/api/admin/redirect/${id}`); ElMessage.success('已删除'); loadRedirects() }
onMounted(() => { loadConfig(); loadRedirects() })
</script>

<style scoped>
.seo-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.seo-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.card-label { font-size: 15px; font-weight: 700; color: #f8fafc; margin: 0 0 16px; }
.card-header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.table-wrap { overflow: hidden; }
.sitemap-btn {
  background: linear-gradient(135deg, #a855f7, #22d3ee) !important;
  border: none !important; color: #fff !important; font-weight: 600;
  box-shadow: 0 0 16px rgba(168,85,247,0.2);
}
.sitemap-btn:hover { transform: translateY(-2px); box-shadow: 0 6px 24px rgba(168,85,247,0.35); }
.status-dot-sm {
  display: inline-block; width: 8px; height: 8px; border-radius: 50%;
}
.status-dot-sm.active { background: #10b981; box-shadow: 0 0 6px rgba(16,185,129,0.4); }
.status-dot-sm.inactive { background: #475569; }

:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.04) !important; }
@media (max-width: 1024px) { .seo-grid { grid-template-columns: 1fr; } }
</style>
