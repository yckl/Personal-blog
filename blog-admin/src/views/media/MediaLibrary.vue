<template>
  <div class="media-library">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="Search files..." clearable style="width: 220px" @clear="loadData" @keyup.enter="loadData">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="typeFilter" placeholder="File type" clearable style="width: 140px" @change="loadData">
          <el-option label="Images" value="image" />
          <el-option label="Videos" value="video" />
          <el-option label="Audio" value="audio" />
          <el-option label="Documents" value="document" />
        </el-select>
      </div>
      <el-upload
        :show-file-list="false"
        :http-request="handleUpload"
        :accept="acceptTypes"
        multiple
        drag
        class="upload-trigger"
      >
        <el-button type="primary"><el-icon><Upload /></el-icon> Upload</el-button>
      </el-upload>
    </div>

    <!-- Upload progress -->
    <el-progress v-if="uploading" :percentage="uploadPercent" :stroke-width="4" style="margin-bottom: 16px;" />

    <!-- Drop zone overlay -->
    <div
      class="drop-zone"
      :class="{ active: isDragging }"
      @dragover.prevent="isDragging = true"
      @dragleave="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <!-- Card Grid -->
      <div class="media-grid" v-loading="loading">
        <div v-for="file in files" :key="file.id" class="media-card" @click="openDetail(file)">
          <div class="media-preview">
            <img v-if="file.fileType === 'image'" :src="getUrl(file.thumbnailUrl || file.fileUrl)" loading="lazy" />
            <div v-else-if="file.fileType === 'video'" class="type-icon video">
              <el-icon :size="36"><VideoCamera /></el-icon>
            </div>
            <div v-else-if="file.fileType === 'audio'" class="type-icon audio">
              <el-icon :size="36"><Headset /></el-icon>
            </div>
            <div v-else class="type-icon doc">
              <el-icon :size="36"><Document /></el-icon>
            </div>
            <div class="media-overlay">
              <el-button circle size="small" @click.stop="copyUrl(file)"><el-icon><CopyDocument /></el-icon></el-button>
              <el-button circle size="small" type="danger" @click.stop="handleDelete(file)"><el-icon><Delete /></el-icon></el-button>
            </div>
          </div>
          <div class="media-info">
            <div class="media-name" :title="file.originalName">{{ file.originalName }}</div>
            <div class="media-meta">
              {{ formatSize(file.fileSize) }}
              <span v-if="file.width">· {{ file.width }}×{{ file.height }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty drop hint -->
      <div v-if="isDragging" class="drop-hint">
        <el-icon :size="48"><Upload /></el-icon>
        <p>Drop files here to upload</p>
      </div>
    </div>

    <!-- Pagination -->
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      :page-sizes="[20, 40, 60]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end;"
      @current-change="loadData"
      @size-change="loadData"
    />

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="File Details" width="520px" destroy-on-close>
      <div v-if="selectedFile" class="detail-content">
        <div class="detail-preview">
          <img v-if="selectedFile.fileType === 'image'" :src="getUrl(selectedFile.fileUrl)" style="max-width: 100%; border-radius: 8px;" />
          <div v-else class="type-icon doc" style="width: 100%; height: 200px;">
            <el-icon :size="64"><Document /></el-icon>
          </div>
        </div>
        <el-descriptions :column="1" border style="margin-top: 16px;">
          <el-descriptions-item label="Name">{{ selectedFile.originalName }}</el-descriptions-item>
          <el-descriptions-item label="Type">{{ selectedFile.mimeType }}</el-descriptions-item>
          <el-descriptions-item label="Size">{{ formatSize(selectedFile.fileSize) }}</el-descriptions-item>
          <el-descriptions-item v-if="selectedFile.width" label="Dimensions">{{ selectedFile.width }}×{{ selectedFile.height }}</el-descriptions-item>
          <el-descriptions-item label="URL">
            <el-input :model-value="getUrl(selectedFile.fileUrl)" readonly size="small">
              <template #append>
                <el-button @click="copyUrl(selectedFile)"><el-icon><CopyDocument /></el-icon></el-button>
              </template>
            </el-input>
          </el-descriptions-item>
          <el-descriptions-item v-if="selectedFile.webpUrl" label="WebP URL">
            <el-link type="primary" :href="getUrl(selectedFile.webpUrl)" target="_blank">View WebP</el-link>
          </el-descriptions-item>
        </el-descriptions>
        <div style="margin-top: 16px;">
          <label style="font-weight: 600; font-size: 13px; display: block; margin-bottom: 6px;">Alt Text</label>
          <el-input v-model="altTextEdit" placeholder="Describe this image for accessibility..." />
          <el-button type="primary" size="small" style="margin-top: 8px;" @click="saveAlt">Save Alt Text</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getMediaFiles, uploadMedia, deleteMedia, updateMediaAlt } from '../../api/modules'
import { ElMessage, ElMessageBox } from 'element-plus'

const files = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const keyword = ref('')
const typeFilter = ref('')
const uploading = ref(false)
const uploadPercent = ref(0)
const isDragging = ref(false)

const detailVisible = ref(false)
const selectedFile = ref<any>(null)
const altTextEdit = ref('')

const acceptTypes = 'image/*,video/*,audio/*,.pdf,.zip,.doc,.docx'
const baseUrl = 'http://localhost:8088'

function getUrl(path: string) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return baseUrl + path
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(1) + ' MB'
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getMediaFiles({
      page: page.value, size: size.value,
      fileType: typeFilter.value || undefined,
      keyword: keyword.value || undefined
    })
    files.value = res.data.records || []
    total.value = res.data.total
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

async function handleUpload(options: any) {
  uploading.value = true
  uploadPercent.value = 0
  try {
    await uploadMedia(options.file, (p: number) => { uploadPercent.value = p })
    ElMessage.success('Uploaded successfully')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || 'Upload failed') }
  finally { uploading.value = false }
}

async function handleDrop(e: DragEvent) {
  isDragging.value = false
  const droppedFiles = e.dataTransfer?.files
  if (!droppedFiles) return
  for (const file of droppedFiles) {
    await handleUpload({ file })
  }
}

function openDetail(file: any) {
  selectedFile.value = file
  altTextEdit.value = file.altText || ''
  detailVisible.value = true
}

async function saveAlt() {
  if (!selectedFile.value) return
  try {
    await updateMediaAlt(selectedFile.value.id, altTextEdit.value)
    selectedFile.value.altText = altTextEdit.value
    ElMessage.success('Alt text saved')
  } catch (e: any) { ElMessage.error(e.message) }
}

async function handleDelete(file: any) {
  await ElMessageBox.confirm(`Delete "${file.originalName}"?`, 'Confirm')
  await deleteMedia(file.id)
  ElMessage.success('Deleted')
  if (detailVisible.value) detailVisible.value = false
  loadData()
}

function copyUrl(file: any) {
  navigator.clipboard.writeText(getUrl(file.fileUrl))
  ElMessage.success('URL copied!')
}

onMounted(loadData)
</script>

<style scoped>
.media-library { padding: 4px; }
.toolbar {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px; flex-wrap: wrap; gap: 12px;
}
.toolbar-left { display: flex; gap: 10px; }
.upload-trigger :deep(.el-upload-dragger) {
  padding: 0; border: none; background: none;
}
.drop-zone { position: relative; min-height: 300px; }
.drop-zone.active { outline: 3px dashed #409eff; outline-offset: -3px; border-radius: 12px; }
.drop-hint {
  position: absolute; inset: 0; display: flex; flex-direction: column;
  align-items: center; justify-content: center; background: rgba(64,158,255,0.06);
  border-radius: 12px; z-index: 10; pointer-events: none;
  color: #409eff; font-size: 16px; font-weight: 600;
}
.media-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 16px;
}
.media-card {
  border-radius: 12px; overflow: hidden; background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}
.media-card:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(0,0,0,0.1); }
.media-preview {
  height: 160px; background: #f5f7fa; display: flex; align-items: center;
  justify-content: center; overflow: hidden; position: relative;
}
.media-preview img { width: 100%; height: 100%; object-fit: cover; }
.media-overlay {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  gap: 8px; background: rgba(0,0,0,0.4); opacity: 0; transition: opacity 0.2s;
}
.media-card:hover .media-overlay { opacity: 1; }
.type-icon {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 100%; color: #c0c4cc;
}
.type-icon.video { background: #fdf6ec; color: #e6a23c; }
.type-icon.audio { background: #f0f9eb; color: #67c23a; }
.type-icon.doc { background: #ecf5ff; color: #409eff; }
.media-info { padding: 10px 12px; }
.media-name {
  font-size: 13px; color: #333; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap;
}
.media-meta { font-size: 12px; color: #999; margin-top: 4px; }
.detail-preview { text-align: center; }
</style>
