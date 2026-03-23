<template>
  <div class="backup-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">📦 备份与恢复</h3>
    </div>

    <div class="backup-grid">
      <!-- Export Card -->
      <div class="backup-card glass-admin">
        <div class="card-icon-wrap export-icon">📤</div>
        <h4 class="card-label">Export</h4>
        <div class="action-list">
          <div class="action-item">
            <div>
              <div class="action-title">📄 Markdown ZIP</div>
              <div class="action-desc">All articles as .md files with YAML front matter</div>
            </div>
            <el-button class="backup-btn" :loading="exporting === 'zip'" @click="handleExportZip">下载</el-button>
          </div>
          <div class="action-item">
            <div>
              <div class="action-title">🗂️ 完整 JSON 备份</div>
              <div class="action-desc">Articles, tags, categories, config, menus</div>
            </div>
            <el-button class="backup-btn" :loading="exporting === 'json'" @click="handleExportJson">下载</el-button>
          </div>
        </div>
        <!-- Progress -->
        <div v-if="exporting" class="progress-area">
          <div class="progress-track"><div class="progress-fill" :style="{ width: exportProgress + '%' }" /></div>
          <span class="progress-text">{{ exportProgress }}%</span>
        </div>
      </div>

      <!-- Import Card -->
      <div class="backup-card glass-admin">
        <div class="card-icon-wrap import-icon">📥</div>
        <h4 class="card-label">Import</h4>
        <div class="action-list">
          <div class="action-item">
            <div>
              <div class="action-title">📄 导入 Markdown ZIP</div>
              <div class="action-desc">上传 .md 文件导入为草稿文章</div>
            </div>
            <el-upload :show-file-list="false" accept=".zip" :http-request="handleImportMd">
              <el-button :loading="importing === 'md'">上传 ZIP</el-button>
            </el-upload>
          </div>
          <div class="action-item">
            <div>
              <div class="action-title">🗂️ 导入 JSON 备份</div>
              <div class="action-desc">从完整站点 JSON 导出恢复</div>
            </div>
            <el-upload :show-file-list="false" accept=".json" :http-request="handleImportJson">
              <el-button :loading="importing === 'json'">上传 JSON</el-button>
            </el-upload>
          </div>
        </div>
        <div v-if="importing" class="progress-area">
          <div class="progress-track"><div class="progress-fill importing" :style="{ width: importProgress + '%' }" /></div>
          <span class="progress-text">{{ importProgress }}%</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { exportSiteZip, exportSiteJson, importSiteJson, importMarkdownZip } from '../../api/article'
import { ElMessage } from 'element-plus'

const exporting = ref('')
const importing = ref('')
const exportProgress = ref(0)
const importProgress = ref(0)

function downloadBlob(data: any, filename: string) {
  const blob = new Blob([data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  URL.revokeObjectURL(url)
}

function simulateProgress(ref: { value: number }, done: () => void) {
  ref.value = 0
  const iv = setInterval(() => {
    ref.value = Math.min(ref.value + Math.random() * 20, 95)
    if (ref.value >= 95) { clearInterval(iv); done() }
  }, 300)
  return () => { clearInterval(iv); ref.value = 100 }
}

async function handleExportZip() {
  exporting.value = 'zip'
  const finish = simulateProgress(exportProgress, () => {})
  try { const res: any = await exportSiteZip(); finish(); downloadBlob(res, 'blog-export.zip'); ElMessage.success('导出成功！') }
  catch (e: any) { finish(); ElMessage.error(e.message) }
  finally { setTimeout(() => { exporting.value = ''; exportProgress.value = 0 }, 600) }
}

async function handleExportJson() {
  exporting.value = 'json'
  const finish = simulateProgress(exportProgress, () => {})
  try { const res: any = await exportSiteJson(); finish(); downloadBlob(res, 'blog-export.json'); ElMessage.success('导出成功！') }
  catch (e: any) { finish(); ElMessage.error(e.message) }
  finally { setTimeout(() => { exporting.value = ''; exportProgress.value = 0 }, 600) }
}

async function handleImportMd(options: any) {
  importing.value = 'md'
  const finish = simulateProgress(importProgress, () => {})
  try { await importMarkdownZip(options.file); finish(); ElMessage.success('导入成功！') }
  catch (e: any) { finish(); ElMessage.error(e.message) }
  finally { setTimeout(() => { importing.value = ''; importProgress.value = 0 }, 600) }
}

async function handleImportJson(options: any) {
  importing.value = 'json'
  const finish = simulateProgress(importProgress, () => {})
  try { await importSiteJson(options.file); finish(); ElMessage.success('导入成功！') }
  catch (e: any) { finish(); ElMessage.error(e.message) }
  finally { setTimeout(() => { importing.value = ''; importProgress.value = 0 }, 600) }
}
</script>

<style scoped>
.backup-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }

.backup-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; }
.backup-card { text-align: center; padding: 28px; }
.card-icon-wrap { font-size: 40px; margin-bottom: 12px; }
.card-label { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0 0 20px; }
.action-list { display: flex; flex-direction: column; gap: 16px; text-align: left; }
.action-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px; border-radius: 12px;
  background: rgba(15,23,42,0.4); border: 1px solid rgba(255,255,255,0.05);
}
.action-item:hover { border-color: rgba(168,85,247,0.12); }
.action-title { font-weight: 600; font-size: 14px; color: #f8fafc; margin-bottom: 4px; }
.action-desc { font-size: 12px; color: #475569; }

.backup-btn {
  background: linear-gradient(135deg, #a855f7, #22d3ee) !important;
  border: none !important; color: #fff !important; font-weight: 600;
}
.backup-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(168,85,247,0.25); }

/* Progress */
.progress-area { margin-top: 20px; display: flex; align-items: center; gap: 12px; }
.progress-track {
  flex: 1; height: 12px; border-radius: 6px;
  background: rgba(255,255,255,0.06); overflow: hidden;
}
.progress-fill {
  height: 100%; border-radius: 6px; transition: width 0.3s;
  background: linear-gradient(90deg, #a855f7, #22d3ee);
}
.progress-fill.importing { background: linear-gradient(90deg, #22d3ee, #a855f7); }
.progress-text { font-size: 13px; color: #94a3b8; font-weight: 600; min-width: 40px; }

@media (max-width: 1024px) { .backup-grid { grid-template-columns: 1fr; } }
</style>
