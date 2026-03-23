<template>
  <div class="editor-page">
    <el-row :gutter="20">
      <!-- Editor Area -->
      <el-col :span="16">
        <el-card shadow="hover" class="editor-card">
          <el-input v-model="form.title" placeholder="Article Title" size="large"
            style="margin-bottom: 12px; font-size: 20px; font-weight: 600;" />

          <!-- Stats bar -->
          <div class="stats-bar">
            <span>📝 {{ wordCount }} words</span>
            <span>⏱️ ~{{ readingTime }} min read</span>
            <span v-if="autoSaveStatus" :class="'auto-save ' + autoSaveStatus">
              {{ autoSaveStatus === 'saving' ? '💾 Saving...' : autoSaveStatus === 'saved' ? '✅ Saved' : '⚠️ Error' }}
            </span>
          </div>

          <MdEditor
            v-model="form.contentMd"
            :style="{ height: '560px' }"
            :toolbars="toolbars as any"
            @onHtmlChanged="handleHtmlChange"
            @onChange="handleContentChange"
            @onUploadImg="handleUploadImg"
            language="en-US"
            :preview="true"
            :showCodeRowNumber="true"
          />
        </el-card>
      </el-col>

      <!-- Sidebar Settings -->
      <el-col :span="8">
        <!-- Publish -->
        <el-card shadow="hover" class="side-card">
          <template #header><span class="card-title">Publish</span></template>
          <el-form label-position="top" :model="form">
            <el-form-item label="Status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="Draft" value="DRAFT" />
                <el-option label="Published" value="PUBLISHED" />
                <el-option label="Scheduled" value="SCHEDULED" />
                <el-option label="Private" value="PRIVATE" />
                <el-option label="Member Only" value="MEMBER_ONLY" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="form.status === 'SCHEDULED'" label="Scheduled At">
              <el-date-picker v-model="form.scheduledAt" type="datetime" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="Slug">
              <el-input v-model="form.slug" placeholder="auto-generated-from-title" />
            </el-form-item>
            <div style="display: flex; gap: 10px; flex-wrap: wrap;">
              <el-button type="primary" style="flex: 1" :loading="saving" @click="handleSave">
                {{ isEdit ? 'Update' : 'Save' }}
              </el-button>
              <el-button v-if="isEdit && form.status !== 'PUBLISHED'" type="success" :loading="saving" @click="showPublishDialog = true">
                Publish
              </el-button>
              <el-button v-if="isEdit" style="flex-basis: 100%;" @click="handlePreview">
                🔗 Preview Link
              </el-button>
            </div>
          </el-form>
        </el-card>

        <!-- Metadata -->
        <el-card shadow="hover" class="side-card">
          <template #header><span class="card-title">Metadata</span></template>
          <el-form label-position="top" :model="form">
            <el-form-item label="Category">
              <el-select v-model="form.categoryId" placeholder="Select category" clearable style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="Tags">
              <el-select v-model="form.tagIds" multiple placeholder="Select tags" style="width: 100%">
                <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="Excerpt">
              <el-input v-model="form.excerpt" type="textarea" :rows="3" placeholder="Brief summary..." />
            </el-form-item>
            <el-form-item label="Cover Image URL">
              <el-input v-model="form.coverImage" placeholder="https://..." />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="form.isTop">Pin to top</el-checkbox>
              <el-checkbox v-model="form.isFeatured">Featured</el-checkbox>
              <el-checkbox v-model="form.allowComment">Allow comments</el-checkbox>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- TOC -->
        <el-card shadow="hover" class="side-card" v-if="tocItems.length > 0">
          <template #header><span class="card-title">📑 Table of Contents</span></template>
          <div class="toc-list">
            <div v-for="(item, idx) in tocItems" :key="idx" class="toc-item"
              :style="{ paddingLeft: (item.level - 1) * 16 + 'px' }">
              <span class="toc-dot" :class="'level-' + item.level"></span>
              {{ item.text }}
            </div>
          </div>
        </el-card>

        <!-- Version History & Export -->
        <el-card v-if="isEdit" shadow="hover" class="side-card">
          <template #header><span class="card-title">📜 Version History</span></template>
          <el-button style="width: 100%; margin-bottom: 8px;" @click="openVersionDrawer">
            <el-icon><Clock /></el-icon> View Versions ({{ versions.length }})
          </el-button>
          <el-button style="width: 100%;" @click="handleExportMd">
            <el-icon><Download /></el-icon> Export Markdown
          </el-button>
        </el-card>

        <!-- AI Assistant -->
        <el-card v-if="isEdit" shadow="hover" class="side-card">
          <template #header><span class="card-title">🤖 AI Assistant</span></template>
          <div class="ai-actions">
            <el-button size="small" style="width: 100%; margin-bottom: 6px;" :loading="aiLoading === 'title'" @click="aiGenerate('title')">✨ Generate Titles</el-button>
            <el-button size="small" style="width: 100%; margin-bottom: 6px;" :loading="aiLoading === 'summary'" @click="aiGenerate('summary')">📝 Generate Summary</el-button>
            <el-button size="small" style="width: 100%; margin-bottom: 6px;" :loading="aiLoading === 'seo_title'" @click="aiGenerate('seo_title')">🔍 SEO Title</el-button>
            <el-button size="small" style="width: 100%; margin-bottom: 6px;" :loading="aiLoading === 'meta_description'" @click="aiGenerate('meta_description')">📋 Meta Description</el-button>
            <el-button size="small" style="width: 100%; margin-bottom: 6px;" :loading="aiLoading === 'outline'" @click="aiGenerate('outline')">📑 Generate Outline</el-button>
            <el-button size="small" style="width: 100%; margin-bottom: 6px;" :loading="aiLoading === 'tags'" @click="aiGenerate('tags')">🏷️ Recommend Tags</el-button>
            <el-button size="small" style="width: 100%; margin-bottom: 6px;" :loading="aiLoading === 'newsletter'" @click="aiGenerate('newsletter')">📬 Newsletter Summary</el-button>
            <el-button size="small" style="width: 100%;" :loading="aiLoading === 'internal_links'" @click="aiGenerate('internal_links')">🔗 Internal Links</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- AI Result Dialog -->
    <el-dialog v-model="aiResultVisible" :title="'AI Result: ' + aiResultType" width="600px" destroy-on-close>
      <div class="ai-result">
        <pre v-if="aiResultType === 'outline'" style="white-space: pre-wrap; font-family: monospace; background: #f5f7fa; padding: 16px; border-radius: 8px;">{{ aiResult }}</pre>
        <template v-else-if="aiResultParsed.length > 0">
          <div v-for="(item, idx) in aiResultParsed" :key="idx" class="ai-option" @click="applyAiResult(item)">
            <span>{{ typeof item === 'string' ? item : JSON.stringify(item) }}</span>
            <el-button size="small" type="primary" text>Apply</el-button>
          </div>
        </template>
        <div v-else style="background: #f5f7fa; padding: 16px; border-radius: 8px;">{{ aiResult }}</div>
      </div>
      <template #footer>
        <el-button v-if="aiResultType === 'summary' || aiResultType === 'meta_description' || aiResultType === 'newsletter'" type="primary" @click="applyAiResult(aiResult)">Apply to Field</el-button>
      </template>
    </el-dialog>

    <!-- Version History Drawer -->
    <el-drawer v-model="versionDrawerVisible" title="Version History" size="520px" direction="rtl">
      <div v-if="versions.length === 0" style="text-align: center; color: #c0c4cc; padding: 40px;">
        No versions saved yet
      </div>
      <div v-for="v in versions" :key="v.id" class="version-item"
        :class="{ active: selectedVersion?.id === v.id }" @click="selectVersion(v)">
        <div class="version-header">
          <el-tag size="small" type="info">v{{ v.versionNum }}</el-tag>
          <span class="version-time">{{ formatTime(v.createdAt) }}</span>
        </div>
        <div class="version-title">{{ v.title }}</div>
        <div class="version-actions">
          <el-button size="small" text type="primary" @click.stop="previewVersion(v)">Preview</el-button>
          <el-popconfirm title="Rollback to this version?" @confirm="handleRollback(v.id)">
            <template #reference>
              <el-button size="small" text type="warning" @click.stop>Rollback</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
      <el-dialog v-model="previewVisible" title="Version Preview" width="700px" destroy-on-close append-to-body>
        <div v-if="previewContent" style="max-height: 500px; overflow-y: auto;">
          <el-descriptions :column="1" border style="margin-bottom: 16px;">
            <el-descriptions-item label="Version">v{{ previewContent.versionNum }}</el-descriptions-item>
            <el-descriptions-item label="Title">{{ previewContent.title }}</el-descriptions-item>
            <el-descriptions-item label="Date">{{ formatTime(previewContent.createdAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-divider>Content</el-divider>
          <pre class="version-content-preview">{{ previewContent.contentMd }}</pre>
        </div>
      </el-dialog>
    </el-drawer>

    <!-- Publish Dialog -->
    <el-dialog v-model="showPublishDialog" title="🚀 Publish Article" width="480px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="Visibility">
          <el-radio-group v-model="publishOptions.visibility">
            <el-radio-button value="PUBLISHED">Public</el-radio-button>
            <el-radio-button value="MEMBER_ONLY">Members Only</el-radio-button>
            <el-radio-button value="PRIVATE">Private</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="When to publish">
          <el-radio-group v-model="publishOptions.mode">
            <el-radio value="now">Publish Now</el-radio>
            <el-radio value="schedule">Schedule</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="publishOptions.mode === 'schedule'" label="Scheduled Date & Time">
          <el-date-picker v-model="publishOptions.scheduledAt" type="datetime" style="width: 100%;" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="publishOptions.sendNewsletter">📬 Send Newsletter to subscribers</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">Cancel</el-button>
        <el-button type="success" :loading="saving" @click="handlePublish">Confirm Publish</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import {
  createArticle, updateArticle, getArticle, publishArticle,
  getArticleVersions, rollbackVersion, exportArticleMd,
  scheduleArticle, generatePreviewLink
} from '../../api/article'
import { getCategories, getTags, uploadMedia } from '../../api/modules'
import { aiGenerateTitle, aiGenerateSummary, aiGenerateSeoTitle, aiGenerateMetaDescription, aiGenerateOutline, aiRecommendTags, aiGenerateNewsletter, aiRecommendInternalLinks } from '../../api/ai'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const categories = ref<any[]>([])
const tags = ref<any[]>([])

const isEdit = computed(() => !!route.params.id)
const showPublishDialog = ref(false)
const publishOptions = reactive({
  visibility: 'PUBLISHED',
  mode: 'now' as 'now' | 'schedule',
  scheduledAt: null as Date | null,
  sendNewsletter: false
})

const form = reactive({
  title: '',
  slug: '',
  excerpt: '',
  coverImage: '',
  categoryId: null as number | null,
  tagIds: [] as number[],
  contentMd: '',
  contentHtml: '',
  status: 'DRAFT',
  scheduledAt: null as Date | null,
  isTop: false,
  isFeatured: false,
  allowComment: true
})

// Toolbar config — all md-editor-v3 built-in toolbars
const toolbars = [
  'bold', 'underline', 'italic', 'strikeThrough', '-',
  'title', 'sub', 'sup', 'quote', 'unorderedList', 'orderedList', 'task', '-',
  'codeRow', 'code', 'link', 'image', 'table', 'mermaid', 'katex', '-',
  'revoke', 'next', 'save', '=',
  'pageFullscreen', 'fullscreen', 'preview', 'previewOnly', 'htmlPreview', 'catalog'
]

function handleHtmlChange(html: string) {
  form.contentHtml = html
}

// ---- Word count / reading time (client-side for real-time) ----

const wordCount = ref(0)
const readingTime = ref(0)
const tocItems = ref<{ level: number; text: string; anchor: string }[]>([])

function computeStats(md: string) {
  if (!md) { wordCount.value = 0; readingTime.value = 0; tocItems.value = []; return }
  // Strip code blocks and markdown chars
  const cleaned = md.replace(/```[\s\S]*?```/g, '').replace(/[#*`\[\]()>!\-]/g, '').trim()
  const chinese = (cleaned.match(/[\u4e00-\u9fa5]/g) || []).length
  const eng = cleaned.replace(/[\u4e00-\u9fa5]/g, '').trim()
  const engWords = eng ? eng.split(/\s+/).length : 0
  wordCount.value = chinese + engWords
  readingTime.value = Math.max(1, Math.ceil(chinese / 350 + engWords / 225))

  // TOC
  const headingRegex = /^(#{1,6})\s+(.+)$/gm
  const items: { level: number; text: string; anchor: string }[] = []
  let m
  while ((m = headingRegex.exec(md)) !== null) {
    const text = m[2].replace(/[*_`~]/g, '')
    items.push({
      level: m[1].length,
      text,
      anchor: text.toLowerCase().replace(/[^a-z0-9\u4e00-\u9fa5\s-]/g, '').replace(/\s+/g, '-')
    })
  }
  tocItems.value = items
}

function handleContentChange(md: string) {
  computeStats(md)
  scheduleAutoSave()
}

// ---- Auto-save ----

const autoSaveStatus = ref<'saving' | 'saved' | 'error' | ''>('')
let autoSaveTimer: ReturnType<typeof setTimeout> | null = null

function scheduleAutoSave() {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  autoSaveTimer = setTimeout(autoSave, 5000) // 5 seconds after last edit
}

async function autoSave() {
  if (!form.contentMd.trim() && !form.title.trim()) return
  autoSaveStatus.value = 'saving'
  try {
    const res: any = await request.post('/api/admin/editor/save-draft', {
      articleId: isEdit.value ? Number(route.params.id) : null,
      title: form.title || 'Untitled Draft',
      contentMd: form.contentMd,
      contentHtml: form.contentHtml
    })
    autoSaveStatus.value = 'saved'
    // If new article, update URL
    if (!isEdit.value && res.data) {
      router.replace(`/articles/edit/${res.data}`)
    }
    setTimeout(() => { if (autoSaveStatus.value === 'saved') autoSaveStatus.value = '' }, 3000)
  } catch {
    autoSaveStatus.value = 'error'
  }
}

onUnmounted(() => { if (autoSaveTimer) clearTimeout(autoSaveTimer) })

// ---- Image upload handler (integrates with media library) ----

async function handleUploadImg(files: File[], callback: (urls: string[]) => void) {
  const urls: string[] = []
  for (const file of files) {
    try {
      const res: any = await uploadMedia(file)
      urls.push('http://localhost:8088' + res.data.fileUrl)
    } catch (e: any) {
      ElMessage.error('Image upload failed: ' + (e.message || ''))
    }
  }
  callback(urls)
}

// ---- Save / Publish ----

async function handleSave() {
  if (!form.title.trim()) { ElMessage.warning('Title is required'); return }
  saving.value = true
  try {
    if (isEdit.value) {
      await updateArticle(Number(route.params.id), form)
      ElMessage.success('Article updated')
      loadVersions()
    } else {
      const res: any = await createArticle(form)
      ElMessage.success('Article created')
      router.replace(`/articles/edit/${res.data}`)
    }
  } catch (e: any) { ElMessage.error(e.message) }
  finally { saving.value = false }
}

async function handlePublish() {
  saving.value = true
  try {
    if (publishOptions.mode === 'schedule' && publishOptions.scheduledAt) {
      // Schedule
      const scheduledStr = new Date(publishOptions.scheduledAt).toISOString().replace('Z', '')
      await scheduleArticle(Number(route.params.id), {
        scheduledAt: scheduledStr,
        sendNewsletter: publishOptions.sendNewsletter,
        visibility: publishOptions.visibility
      })
      ElMessage.success('Article scheduled!')
    } else {
      // Publish now
      await updateArticle(Number(route.params.id), form)
      await publishArticle(Number(route.params.id), {
        sendNewsletter: publishOptions.sendNewsletter,
        visibility: publishOptions.visibility
      })
      ElMessage.success('Published!' + (publishOptions.sendNewsletter ? ' Newsletter sending...' : ''))
    }
    showPublishDialog.value = false
    form.status = publishOptions.mode === 'schedule' ? 'SCHEDULED' : publishOptions.visibility
    loadVersions()
  } catch (e: any) { ElMessage.error(e.message) }
  finally { saving.value = false }
}

async function handlePreview() {
  try {
    const res: any = await generatePreviewLink(Number(route.params.id))
    const url = res.data?.previewUrl
    if (url) {
      await navigator.clipboard.writeText(url)
      ElMessage.success('Preview link copied! Valid for 24 hours.')
    }
  } catch (e: any) { ElMessage.error(e.message) }
}

// ---- Version history ----

const versions = ref<any[]>([])
const versionDrawerVisible = ref(false)
const selectedVersion = ref<any>(null)
const previewVisible = ref(false)
const previewContent = ref<any>(null)

async function loadVersions() {
  if (!isEdit.value) return
  try {
    const res: any = await getArticleVersions(Number(route.params.id))
    versions.value = res.data || []
  } catch { /* ignore */ }
}

function openVersionDrawer() { loadVersions(); versionDrawerVisible.value = true }
function selectVersion(v: any) { selectedVersion.value = v }
function previewVersion(v: any) { previewContent.value = v; previewVisible.value = true }

async function handleRollback(versionId: number) {
  try {
    await rollbackVersion(Number(route.params.id), versionId)
    ElMessage.success('Rolled back successfully')
    versionDrawerVisible.value = false
    const res: any = await getArticle(Number(route.params.id))
    const a = res.data
    Object.assign(form, {
      title: a.title, slug: a.slug, excerpt: a.excerpt, coverImage: a.coverImage,
      categoryId: a.categoryId, tagIds: (a.tags || []).map((t: any) => t.id),
      contentMd: a.contentMd || '', contentHtml: a.contentHtml || '',
      status: a.status, isTop: a.isTop, isFeatured: a.isFeatured, allowComment: a.allowComment
    })
    computeStats(form.contentMd)
  } catch (e: any) { ElMessage.error(e.message) }
}

// ---- Export ----

async function handleExportMd() {
  try {
    const res: any = await exportArticleMd(Number(route.params.id))
    const blob = new Blob([res], { type: 'text/markdown' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a'); a.href = url
    a.download = (form.title || 'article') + '.md'; a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('Exported!')
  } catch (e: any) { ElMessage.error(e.message) }
}

// ---- AI Assistant ----

const aiLoading = ref('')
const aiResult = ref('')
const aiResultType = ref('')
const aiResultVisible = ref(false)
const aiResultParsed = ref<any[]>([])

const aiApiFns: Record<string, Function> = {
  title: (c: string, id?: number) => aiGenerateTitle(c, id),
  summary: (c: string, id?: number) => aiGenerateSummary(c, id),
  seo_title: (c: string, id?: number) => aiGenerateSeoTitle(c, form.title, id),
  meta_description: (c: string, id?: number) => aiGenerateMetaDescription(c, id),
  outline: (c: string, id?: number) => aiGenerateOutline(c, id),
  tags: (c: string, id?: number) => aiRecommendTags(c, id),
  newsletter: (c: string, id?: number) => aiGenerateNewsletter(c, id),
  internal_links: (c: string, id?: number) => aiRecommendInternalLinks(c, id)
}

async function aiGenerate(type: string) {
  if (!form.contentMd.trim() && type !== 'outline') {
    ElMessage.warning('Write some content first'); return
  }
  aiLoading.value = type
  try {
    const fn = aiApiFns[type]
    const artId = isEdit.value ? Number(route.params.id) : undefined
    const res: any = await fn(form.contentMd || form.title, artId)
    aiResult.value = res.data || ''
    aiResultType.value = type
    // Try parse JSON array
    try { aiResultParsed.value = JSON.parse(aiResult.value) } catch { aiResultParsed.value = [] }
    aiResultVisible.value = true
  } catch (e: any) { ElMessage.error('AI generation failed: ' + (e.message || '')) }
  finally { aiLoading.value = '' }
}

function applyAiResult(value: any) {
  const text = typeof value === 'string' ? value : value.title || JSON.stringify(value)
  switch (aiResultType.value) {
    case 'title': case 'seo_title': form.title = text; break
    case 'summary': case 'newsletter': form.excerpt = text; break
    case 'meta_description': form.excerpt = text; break
    case 'outline':
      form.contentMd = form.contentMd ? form.contentMd + '\n\n' + text : text; break
    case 'tags': ElMessage.info('Tag: ' + text + ' (add manually from metadata)'); break
    default: break
  }
  aiResultVisible.value = false
  ElMessage.success('Applied!')
}

function formatTime(t: string): string {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

// ---- Init ----

onMounted(async () => {
  const [catRes, tagRes]: any = await Promise.all([getCategories(), getTags()])
  categories.value = catRes.data || []
  tags.value = tagRes.data || []

  if (isEdit.value) {
    const res: any = await getArticle(Number(route.params.id))
    const a = res.data
    Object.assign(form, {
      title: a.title, slug: a.slug, excerpt: a.excerpt, coverImage: a.coverImage,
      categoryId: a.categoryId, tagIds: (a.tags || []).map((t: any) => t.id),
      contentMd: a.contentMd || '', contentHtml: a.contentHtml || '',
      status: a.status, isTop: a.isTop, isFeatured: a.isFeatured, allowComment: a.allowComment
    })
    computeStats(form.contentMd)
    loadVersions()
  }
})
</script>

<style scoped>
.editor-page { padding: 4px; }
.editor-card { border-radius: 12px; }
.side-card { border-radius: 12px; margin-bottom: 16px; }
.card-title { font-weight: 600; }

.stats-bar {
  display: flex; gap: 16px; align-items: center;
  padding: 8px 12px; margin-bottom: 12px;
  background: #f9fafb; border-radius: 8px;
  font-size: 13px; color: #666;
}
.auto-save { font-weight: 500; }
.auto-save.saving { color: #e6a23c; }
.auto-save.saved { color: #67c23a; }
.auto-save.error { color: #f56c6c; }

/* TOC */
.toc-list { max-height: 240px; overflow-y: auto; }
.toc-item {
  padding: 4px 0; font-size: 13px; color: #555;
  display: flex; align-items: center; gap: 6px;
  cursor: default;
}
.toc-dot {
  width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0;
}
.toc-dot.level-1 { background: #409eff; width: 8px; height: 8px; }
.toc-dot.level-2 { background: #67c23a; }
.toc-dot.level-3 { background: #e6a23c; }
.toc-dot.level-4, .toc-dot.level-5, .toc-dot.level-6 { background: #c0c4cc; }

/* Versions */
.version-item {
  padding: 14px 16px; border: 1px solid #eee; border-radius: 8px;
  margin-bottom: 10px; cursor: pointer; transition: all 0.2s;
}
.version-item:hover { border-color: #409eff; background: #f5f8ff; }
.version-item.active { border-color: #409eff; background: #ecf5ff; }
.version-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.version-time { font-size: 12px; color: #999; }
.version-title { font-size: 14px; font-weight: 500; color: #333; }
.version-actions { margin-top: 8px; display: flex; gap: 4px; }
.version-content-preview {
  background: #f5f7fa; padding: 16px; border-radius: 8px;
  font-size: 13px; white-space: pre-wrap; word-break: break-word;
  max-height: 400px; overflow-y: auto; font-family: monospace;
}

/* AI */
.ai-actions { display: flex; flex-direction: column; }
.ai-result { max-height: 400px; overflow-y: auto; }
.ai-option {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border: 1px solid #eee; border-radius: 8px;
  margin-bottom: 8px; cursor: pointer; transition: all 0.2s;
}
.ai-option:hover { border-color: #409eff; background: #f5f8ff; }
</style>
