<template>
  <div class="backup-page">
    <h3 style="margin-bottom: 20px;">📦 Backup & Export</h3>

    <el-row :gutter="20">
      <!-- Export -->
      <el-col :span="12">
        <el-card shadow="hover" style="border-radius: 12px;">
          <template #header><span style="font-weight: 600;">Export</span></template>
          <div class="action-list">
            <div class="action-item">
              <div>
                <div class="action-title">📄 Export as Markdown ZIP</div>
                <div class="action-desc">Download all articles as a ZIP of .md files with YAML front matter</div>
              </div>
              <el-button type="primary" :loading="exporting === 'zip'" @click="handleExportZip">Download</el-button>
            </div>
            <div class="action-item">
              <div>
                <div class="action-title">🗂️ Export as JSON</div>
                <div class="action-desc">Full site backup: articles, tags, categories, config, menus</div>
              </div>
              <el-button type="primary" :loading="exporting === 'json'" @click="handleExportJson">Download</el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Import -->
      <el-col :span="12">
        <el-card shadow="hover" style="border-radius: 12px;">
          <template #header><span style="font-weight: 600;">Import</span></template>
          <div class="action-list">
            <div class="action-item">
              <div>
                <div class="action-title">📄 Import Markdown ZIP</div>
                <div class="action-desc">Upload a ZIP of .md files to import as draft articles</div>
              </div>
              <el-upload :show-file-list="false" accept=".zip" :http-request="handleImportMd">
                <el-button :loading="importing === 'md'">Upload ZIP</el-button>
              </el-upload>
            </div>
            <div class="action-item">
              <div>
                <div class="action-title">🗂️ Import JSON Backup</div>
                <div class="action-desc">Restore from a full site JSON export</div>
              </div>
              <el-upload :show-file-list="false" accept=".json" :http-request="handleImportJson">
                <el-button :loading="importing === 'json'">Upload JSON</el-button>
              </el-upload>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { exportSiteZip, exportSiteJson, importSiteJson, importMarkdownZip } from '../../api/article'
import { ElMessage } from 'element-plus'

const exporting = ref('')
const importing = ref('')

function downloadBlob(data: any, filename: string) {
  const blob = new Blob([data])
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename; a.click()
  URL.revokeObjectURL(url)
}

async function handleExportZip() {
  exporting.value = 'zip'
  try {
    const res: any = await exportSiteZip()
    downloadBlob(res, 'blog-export.zip')
    ElMessage.success('Exported!')
  } catch (e: any) { ElMessage.error(e.message) }
  finally { exporting.value = '' }
}

async function handleExportJson() {
  exporting.value = 'json'
  try {
    const res: any = await exportSiteJson()
    downloadBlob(res, 'blog-export.json')
    ElMessage.success('Exported!')
  } catch (e: any) { ElMessage.error(e.message) }
  finally { exporting.value = '' }
}

async function handleImportMd(options: any) {
  importing.value = 'md'
  try {
    await importMarkdownZip(options.file)
    ElMessage.success('Imported Markdown files successfully!')
  } catch (e: any) { ElMessage.error(e.message) }
  finally { importing.value = '' }
}

async function handleImportJson(options: any) {
  importing.value = 'json'
  try {
    await importSiteJson(options.file)
    ElMessage.success('Imported site data successfully!')
  } catch (e: any) { ElMessage.error(e.message) }
  finally { importing.value = '' }
}
</script>

<style scoped>
.backup-page { padding: 4px; }
.action-list { display: flex; flex-direction: column; gap: 20px; }
.action-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px; background: #f9fafb; border-radius: 8px;
}
.action-title { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.action-desc { font-size: 12px; color: #999; }
</style>
