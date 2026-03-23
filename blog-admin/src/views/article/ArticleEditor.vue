<template>
  <div class="editor-shell">
    <!-- ===== LEFT: TOC Outline ===== -->
    <aside class="panel-left glass-panel" :class="{ collapsed: tocCollapsed }">
      <div class="panel-header">
        <span v-if="!tocCollapsed" class="panel-title">📑 目录</span>
        <button class="collapse-btn" @click="tocCollapsed = !tocCollapsed" :title="tocCollapsed ? '展开' : '折叠'">
          {{ tocCollapsed ? '▶' : '◀' }}
        </button>
      </div>
      <div v-if="!tocCollapsed" class="toc-list">
        <div v-for="(item, idx) in tocItems" :key="idx" class="toc-item"
          :style="{ paddingLeft: (item.level - 1) * 14 + 'px' }">
          <span class="toc-dot" :class="'level-' + item.level"></span>
          <span class="toc-text">{{ item.text }}</span>
        </div>
        <div v-if="tocItems.length === 0" class="toc-empty">开始编写后自动生成…</div>
      </div>
    </aside>

    <!-- Divider 1 -->
    <div class="divider"></div>

    <!-- ===== CENTER: Editor ===== -->
    <main class="panel-center">
      <!-- Title Input -->
      <div class="title-area">
        <input v-model="form.title" placeholder="输入文章标题…" class="title-input" />
        <div class="stats-bar">
          <span class="status-pill" :class="form.status.toLowerCase()">{{ form.status }}</span>
          <span>📝 {{ wordCount }} 字</span>
          <span>⏱️ ~{{ readingTime }} 分钟</span>
          <span v-if="autoSaveStatus" :class="'auto-save ' + autoSaveStatus">
            {{ autoSaveStatus === 'saving' ? '💾 保存中…' : autoSaveStatus === 'saved' ? '✅ 已保存' : '⚠️ 错误' }}
          </span>
        </div>
      </div>

      <!-- Markdown Editor -->
      <div class="editor-wrap">
        <MdEditor
          v-model="form.contentMd"
          :style="{ height: '100%' }"
          :toolbars="toolbars as any"
          @onHtmlChanged="handleHtmlChange"
          @onChange="handleContentChange"
          @onUploadImg="handleUploadImg"
          language="zh-CN"
          :preview="true"
          :showCodeRowNumber="true"
        />
      </div>
    </main>

    <!-- Divider 2 -->
    <div class="divider"></div>

    <!-- ===== RIGHT: Properties ===== -->
    <aside class="panel-right glass-panel">
      <!-- Tab Header -->
      <div class="tab-header">
        <button v-for="tab in rightTabs" :key="tab.key"
          class="tab-btn" :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key">
          {{ tab.icon }} {{ tab.label }}
        </button>
      </div>

      <!-- Tab Content -->
      <div class="tab-content">
        <!-- 发布 Tab -->
        <div v-if="activeTab === 'publish'" class="tab-panel">
          <el-form label-position="top" :model="form">
            <el-form-item label="状态">
              <el-select v-model="form.status" style="width: 100%">
                <el-option label="草稿" value="DRAFT" />
                <el-option label="已发布" value="PUBLISHED" />
                <el-option label="定时发布" value="SCHEDULED" />
                <el-option label="私密" value="PRIVATE" />
                <el-option label="仅会员" value="MEMBER_ONLY" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="form.status === 'SCHEDULED'" label="定时时间">
              <el-date-picker v-model="form.scheduledAt" type="datetime" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="别名">
              <el-input v-model="form.slug" placeholder="自动生成" />
            </el-form-item>
            <el-form-item label="封面图片">
              <el-input v-model="form.coverImage" placeholder="https://..." />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="form.isTop">置顶</el-checkbox>
              <el-checkbox v-model="form.isFeatured">精选</el-checkbox>
              <el-checkbox v-model="form.allowComment">允许评论</el-checkbox>
            </el-form-item>
            <el-button v-if="isEdit" style="width: 100%;" @click="handlePreview">🔗 预览链接</el-button>
          </el-form>
        </div>

        <!-- 元数据 Tab -->
        <div v-if="activeTab === 'meta'" class="tab-panel">
          <el-form label-position="top" :model="form">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="标签">
              <el-select v-model="form.tagIds" multiple placeholder="选择标签" style="width: 100%">
                <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="摘要">
              <el-input v-model="form.excerpt" type="textarea" :rows="4" placeholder="简要摘要..." />
            </el-form-item>
          </el-form>
        </div>

        <!-- AI 助手 Tab -->
        <div v-if="activeTab === 'ai'" class="tab-panel">
          <div class="ai-grid">
            <button v-for="item in aiButtons" :key="item.type" class="ai-btn"
              :disabled="!!aiLoading" @click="aiGenerate(item.type)">
              <span class="ai-icon">{{ item.icon }}</span>
              <span>{{ item.label }}</span>
              <span v-if="aiLoading === item.type" class="ai-spinner">⏳</span>
            </button>
          </div>
        </div>

        <!-- 版本历史 Tab -->
        <div v-if="activeTab === 'version'" class="tab-panel">
          <el-button style="width: 100%; margin: 0 0 10px 0; justify-content: center;" @click="openVersionDrawer">
            <el-icon><Clock /></el-icon> 查看版本 ({{ versions.length }})
          </el-button>
          <el-button style="width: 100%; margin: 0; justify-content: center;" @click="handleExportMd">
            <el-icon><Download /></el-icon> 导出 Markdown
          </el-button>
        </div>
      </div>
    </aside>

    <!-- ===== Floating Bottom Bar ===== -->
    <div class="bottom-bar">
      <div class="bar-left">
        <span class="save-hint" v-if="autoSaveStatus === 'saved'">✅ 所有更改已保存</span>
        <span class="save-hint" v-else-if="autoSaveStatus === 'saving'">💾 保存中...</span>
      </div>
      <div class="bar-actions">
        <button class="bar-btn save" :disabled="saving" @click="handleSave">
          💾 {{ isEdit ? '更新' : '保存草稿' }}
        </button>
        <button v-if="isEdit && form.status !== 'PUBLISHED'" class="bar-btn publish" :disabled="saving" @click="showPublishDialog = true">
          🚀 发布
        </button>
      </div>
    </div>

    <!-- ===== Dialogs (kept from original) ===== -->
    <!-- AI Result Dialog -->
    <el-dialog v-model="aiResultVisible" :title="aiDialogTitle" width="620px" destroy-on-close class="ai-dialog">
      <div class="ai-result">
        <div v-if="!aiResult" class="ai-empty"><p>🤖 未获取到结果，请稍后重试</p></div>
        <pre v-else-if="aiResultType === 'outline'" class="ai-outline-block">{{ aiResult }}</pre>
        <div v-else-if="aiResultType === 'newsletter'" class="ai-newsletter-preview">
          <div class="newsletter-label">📬 生成的邮件摘要：</div>
          <div class="newsletter-text">{{ aiResult }}</div>
        </div>
        <div v-else-if="aiResultType === 'internal_links' && aiResultParsed.length > 0">
          <div class="ai-links-label">🔗 推荐的内部链接：</div>
          <div v-for="(link, idx) in aiResultParsed" :key="idx" class="ai-link-card">
            <div class="link-title">{{ link.title || link.slug }}</div>
            <div v-if="link.slug" class="link-slug">/article/{{ link.slug }}</div>
            <div v-if="link.reason" class="link-reason">💡 {{ link.reason }}</div>
            <el-button size="small" type="primary" text @click="copyLinkMarkdown(link)">复制链接</el-button>
          </div>
        </div>
        <div v-else-if="aiResultType === 'internal_links' && aiResultParsed.length === 0" class="ai-empty">
          <p>暂无推荐的内部链接</p>
        </div>
        <template v-else-if="aiResultParsed.length > 0">
          <div v-for="(item, idx) in aiResultParsed" :key="idx" class="ai-option" @click="applyAiResult(item)">
            <span>{{ typeof item === 'string' ? item : (item.title || JSON.stringify(item)) }}</span>
            <el-button size="small" type="primary" text>应用</el-button>
          </div>
        </template>
        <div v-else class="ai-plain-result">{{ aiResult }}</div>
      </div>
      <template #footer>
        <el-button @click="aiResultVisible = false">关闭</el-button>
        <el-button v-if="['summary', 'meta_description', 'newsletter'].includes(aiResultType)" type="primary" @click="applyAiResult(aiResult)">应用到摘要字段</el-button>
        <el-button v-if="aiResultType === 'outline'" type="primary" @click="applyAiResult(aiResult)">插入到正文</el-button>
      </template>
    </el-dialog>

    <!-- Version History Drawer -->
    <el-drawer v-model="versionDrawerVisible" title="版本历史" size="520px" direction="rtl">
      <div v-if="versions.length === 0" style="text-align: center; color: #c0c4cc; padding: 40px;">暂无历史版本</div>
      <div v-for="v in versions" :key="v.id" class="version-item"
        :class="{ active: selectedVersion?.id === v.id }" @click="selectVersion(v)">
        <div class="version-header">
          <el-tag size="small" type="info">v{{ v.versionNum }}</el-tag>
          <span class="version-time">{{ formatTime(v.createdAt) }}</span>
        </div>
        <div class="version-title">{{ v.title }}</div>
        <div class="version-actions">
          <el-button size="small" text type="primary" @click.stop="previewVersion(v)">预览</el-button>
          <el-popconfirm title="确定回滚到此版本？" @confirm="handleRollback(v.id)">
            <template #reference>
              <el-button size="small" text type="warning" @click.stop>回滚</el-button>
            </template>
          </el-popconfirm>
        </div>
      </div>
      <el-dialog v-model="previewVisible" title="版本预览" width="700px" destroy-on-close append-to-body>
        <div v-if="previewContent" style="max-height: 500px; overflow-y: auto;">
          <el-descriptions :column="1" border style="margin-bottom: 16px;">
            <el-descriptions-item label="版本">v{{ previewContent.versionNum }}</el-descriptions-item>
            <el-descriptions-item label="标题">{{ previewContent.title }}</el-descriptions-item>
            <el-descriptions-item label="日期">{{ formatTime(previewContent.createdAt) }}</el-descriptions-item>
          </el-descriptions>
          <el-divider>内容</el-divider>
          <pre class="version-content-preview">{{ previewContent.contentMd }}</pre>
        </div>
      </el-dialog>
    </el-drawer>

    <!-- Publish Dialog -->
    <el-dialog v-model="showPublishDialog" title="🚀 发布文章" width="480px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="可见性">
          <el-radio-group v-model="publishOptions.visibility">
            <el-radio-button value="PUBLISHED">公开</el-radio-button>
            <el-radio-button value="MEMBER_ONLY">仅会员</el-radio-button>
            <el-radio-button value="PRIVATE">私密</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-radio-group v-model="publishOptions.mode">
            <el-radio value="now">立即发布</el-radio>
            <el-radio value="schedule">定时发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="publishOptions.mode === 'schedule'" label="定时日期与时间">
          <el-date-picker v-model="publishOptions.scheduledAt" type="datetime" style="width: 100%;" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="publishOptions.sendNewsletter">📬 发送邮件通知给订阅者</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="success" :loading="saving" @click="handlePublish">确认发布</el-button>
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

// Three-panel layout state
const tocCollapsed = ref(false)
const activeTab = ref('publish')
const rightTabs = [
  { key: 'publish', icon: '📤', label: '发布' },
  { key: 'meta', icon: '🏷️', label: '元数据' },
  { key: 'ai', icon: '🤖', label: 'AI助手' },
  { key: 'version', icon: '📜', label: '版本' }
]
const aiButtons = [
  { type: 'title', icon: '✨', label: '生成标题' },
  { type: 'summary', icon: '📝', label: '生成摘要' },
  { type: 'seo_title', icon: '🔍', label: 'SEO 标题' },
  { type: 'meta_description', icon: '📋', label: 'Meta 描述' },
  { type: 'outline', icon: '📑', label: '生成大纲' },
  { type: 'tags', icon: '🏷️', label: '推荐标签' },
  { type: 'newsletter', icon: '📬', label: '邮件摘要' },
  { type: 'internal_links', icon: '🔗', label: '内部链接' }
]

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
const isDirty = ref(false)

function scheduleAutoSave() {
  isDirty.value = true
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
    isDirty.value = false
    // If new article, update URL
    if (!isEdit.value && res.data) {
      router.replace(`/articles/edit/${res.data}`)
    }
    setTimeout(() => { if (autoSaveStatus.value === 'saved') autoSaveStatus.value = '' }, 3000)
  } catch {
    autoSaveStatus.value = 'error'
  }
}

// Prevent accidental close/refresh with unsaved changes
function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (isDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  if (autoSaveTimer) clearTimeout(autoSaveTimer)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

// Prevent in-app navigation with unsaved changes
import { onBeforeRouteLeave } from 'vue-router'
onBeforeRouteLeave((_to, _from, next) => {
  if (isDirty.value) {
    const answer = window.confirm('您有未保存的更改，确认离开吗？')
    next(answer)
  } else {
    next()
  }
})

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
    ElMessage.success('导出成功！')
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

const aiDialogTitleMap: Record<string, string> = {
  title: '✨ AI 生成标题',
  summary: '📝 AI 生成摘要',
  seo_title: '🔍 AI SEO 标题',
  meta_description: '📋 AI Meta 描述',
  outline: '📑 AI 生成大纲',
  tags: '🏷️ AI 推荐标签',
  newsletter: '📬 AI 邮件摘要',
  internal_links: '🔗 AI 内链推荐'
}
const aiDialogTitle = computed(() => aiDialogTitleMap[aiResultType.value] || 'AI 结果')

async function aiGenerate(type: string) {
  if (!form.contentMd.trim() && type !== 'outline') {
    ElMessage.warning('请先输入一些文章内容'); return
  }
  if (aiLoading.value) return // 防重
  aiLoading.value = type
  try {
    const fn = aiApiFns[type]
    const artId = isEdit.value ? Number(route.params.id) : undefined
    const res: any = await fn(form.contentMd || form.title, artId)
    aiResult.value = res.data || ''
    aiResultType.value = type
    // Try parse JSON array
    try {
      let cleanJson = aiResult.value.trim()
      const match = cleanJson.match(/\[([\s\S]*)\]/)
      if (match) cleanJson = match[0]
      aiResultParsed.value = JSON.parse(cleanJson)
    } catch { aiResultParsed.value = [] }
    aiResultVisible.value = true
    ElMessage.success('生成完成！')
  } catch (e: any) {
    ElMessage.error('生成失败，请稍后重试：' + (e.message || '未知错误'))
  } finally { aiLoading.value = '' }
}

function applyAiResult(value: any) {
  const text = typeof value === 'string' ? value : value.title || JSON.stringify(value)
  switch (aiResultType.value) {
    case 'title': case 'seo_title': form.title = text; break
    case 'summary': case 'newsletter': form.excerpt = text; break
    case 'meta_description': form.excerpt = text; break
    case 'outline':
      form.contentMd = form.contentMd ? form.contentMd + '\n\n' + text : text; break
    case 'tags': ElMessage.info('标签：' + text + '（请在元数据面板手动添加）'); break
    default: break
  }
  aiResultVisible.value = false
  ElMessage.success('已应用！')
}

function copyLinkMarkdown(link: any) {
  const md = `[${link.title}](/article/${link.slug})`
  navigator.clipboard.writeText(md)
  ElMessage.success('已复制 Markdown 链接！')
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
/* ===== Shell: Full-height three-panel flex ===== */
.editor-shell {
  display: flex; height: calc(100vh - 60px);
  background: #0a0a0a; overflow: hidden; position: relative;
}

/* ===== Glass Panel Base ===== */
.glass-panel {
  background: rgba(15,23,42,0.65);
  backdrop-filter: blur(20px);
  display: flex; flex-direction: column; overflow: hidden;
}

/* ===== LEFT: TOC Panel ===== */
.panel-left {
  width: 180px; flex-shrink: 0;
  border-right: 1px solid rgba(168,85,247,0.15);
  transition: width 0.3s ease;
}
.panel-left.collapsed { width: 48px; }
.panel-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 12px; border-bottom: 1px solid rgba(255,255,255,0.05);
}
.panel-title { font-size: 14px; font-weight: 700; color: #f8fafc; }
.collapse-btn {
  background: rgba(168,85,247,0.1); border: 1px solid rgba(168,85,247,0.2);
  color: #a855f7; border-radius: 6px; width: 24px; height: 24px;
  cursor: pointer; font-size: 10px; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.collapse-btn:hover { background: rgba(168,85,247,0.25); transform: scale(1.1); }
.toc-list { flex: 1; overflow-y: auto; padding: 10px 12px; }
.toc-item {
  display: flex; align-items: center; gap: 6px;
  padding: 5px 0; font-size: 12px; color: #94a3b8; cursor: default; transition: color 0.2s;
}
.toc-item:hover { color: #f8fafc; }
.toc-text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.toc-dot { width: 6px; height: 6px; border-radius: 50%; flex-shrink: 0; }
.toc-dot.level-1 { background: #a855f7; width: 8px; height: 8px; }
.toc-dot.level-2 { background: #22d3ee; }
.toc-dot.level-3 { background: #f59e0b; }
.toc-dot.level-4, .toc-dot.level-5, .toc-dot.level-6 { background: #475569; }
.toc-empty { color: #475569; font-size: 12px; padding: 20px 0; text-align: center; }

/* ===== DIVIDERS ===== */
.divider {
  width: 1px; flex-shrink: 0;
  background: linear-gradient(180deg, rgba(168,85,247,0.3), rgba(34,211,238,0.3));
  transition: all 0.2s; cursor: col-resize;
}
.divider:hover {
  width: 3px;
  box-shadow: 0 0 12px rgba(168,85,247,0.4), 0 0 12px rgba(34,211,238,0.3);
}

/* ===== CENTER: Editor Panel ===== */
.panel-center {
  flex: 1; display: flex; flex-direction: column; min-width: 0; overflow: hidden;
}
.title-area { padding: 16px 24px 0; }
.title-input {
  width: 100%; font-size: 28px; font-weight: 800; line-height: 1.3;
  background: transparent; border: none; outline: none;
  color: #f8fafc; letter-spacing: -0.02em;
  border-bottom: 2px solid rgba(168,85,247,0.15);
  padding-bottom: 12px; margin-bottom: 10px; transition: border-color 0.3s;
}
.title-input:focus { border-image: linear-gradient(90deg, #a855f7, #22d3ee) 1; }
.title-input::placeholder { color: #475569; }
.stats-bar {
  display: flex; gap: 12px; align-items: center; flex-wrap: wrap;
  padding: 8px 0 12px; font-size: 12px; color: #64748b;
}
.editor-wrap { flex: 1; overflow: hidden; }

/* Status pills */
.status-pill {
  padding: 2px 10px; border-radius: 999px; font-size: 11px; font-weight: 700; text-transform: uppercase;
}
.status-pill.draft { background: rgba(148,163,184,0.15); color: #94a3b8; }
.status-pill.published { background: rgba(16,185,129,0.15); color: #10b981; }
.status-pill.scheduled { background: rgba(245,158,11,0.15); color: #f59e0b; }
.status-pill.private { background: rgba(168,85,247,0.15); color: #a855f7; }
.status-pill.member_only { background: rgba(34,211,238,0.15); color: #22d3ee; }

.auto-save { font-weight: 500; }
.auto-save.saving { color: #f59e0b; }
.auto-save.saved { color: #10b981; }
.auto-save.error { color: #f43f5e; }

/* ===== RIGHT: Property Panel ===== */
.panel-right {
  width: 280px; flex-shrink: 0;
  border-left: 1px solid rgba(168,85,247,0.15);
}
.tab-header {
  display: flex; border-bottom: 1px solid rgba(255,255,255,0.06);
  flex-shrink: 0;
}
.tab-btn {
  flex: 1; padding: 12px 4px; font-size: 12px; font-weight: 600;
  color: #64748b; background: transparent; border: none;
  cursor: pointer; transition: all 0.25s; position: relative; white-space: nowrap;
}
.tab-btn:hover { color: #f8fafc; background: rgba(168,85,247,0.06); }
.tab-btn.active { color: #f8fafc; background: rgba(168,85,247,0.08); }
.tab-btn.active::after {
  content: ''; position: absolute; bottom: 0; left: 15%; right: 15%;
  height: 2px; background: linear-gradient(90deg, #a855f7, #22d3ee); border-radius: 2px;
}
.tab-content { flex: 1; overflow-y: auto; }
.tab-panel { padding: 16px; }

/* ===== AI Grid ===== */
.ai-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.ai-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 10px; border-radius: 10px; font-size: 12px; font-weight: 500;
  background: rgba(15,23,42,0.5); color: #94a3b8;
  border: 1px solid rgba(255,255,255,0.06); cursor: pointer; transition: all 0.2s;
}
.ai-btn:hover:not(:disabled) {
  border-color: rgba(168,85,247,0.3); color: #f8fafc;
  background: rgba(168,85,247,0.08); box-shadow: 0 0 12px rgba(168,85,247,0.15);
}
.ai-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.ai-icon { font-size: 14px; }

/* ===== Bottom Bar ===== */
.bottom-bar {
  position: fixed; bottom: 0; left: 240px; right: 0; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 24px; height: 56px;
  background: rgba(15,23,42,0.9); backdrop-filter: blur(20px);
  border-top: 1px solid rgba(168,85,247,0.12);
}
.bar-left { display: flex; align-items: center; gap: 12px; }
.save-hint { font-size: 13px; color: #64748b; }
.bar-actions { display: flex; gap: 10px; }
.bar-btn {
  padding: 10px 24px; border-radius: 12px; font-size: 14px; font-weight: 600;
  border: none; cursor: pointer; transition: all 0.25s; color: #fff;
}
.bar-btn.save { background: linear-gradient(135deg, #a855f7, #7c3aed); }
.bar-btn.save:hover { transform: translateY(-3px); box-shadow: 0 8px 28px rgba(168,85,247,0.35); }
.bar-btn.publish { background: linear-gradient(135deg, #10b981, #059669); }
.bar-btn.publish:hover { transform: translateY(-3px); box-shadow: 0 8px 28px rgba(16,185,129,0.35); }
.bar-btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none !important; }

/* ===== MdEditor dark overrides ===== */
:deep(.md-editor) { background: transparent !important; border: none !important; }
:deep(.md-editor-toolbar-wrapper) { background: rgba(15,23,42,0.6) !important; border-color: rgba(255,255,255,0.05) !important; }
:deep(.el-form-item__label) { color: #94a3b8 !important; font-size: 13px; }

/* ===== Dialog: AI results ===== */
.ai-result { max-height: 400px; overflow-y: auto; }
.ai-option {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; border: 1px solid rgba(255,255,255,0.06); border-radius: 10px;
  margin-bottom: 8px; cursor: pointer; transition: all 0.2s; background: rgba(15,23,42,0.4);
}
.ai-option:hover { border-color: rgba(168,85,247,0.3); background: rgba(168,85,247,0.06); }
.ai-newsletter-preview { padding: 0; }
.newsletter-label { font-size: 13px; color: #94a3b8; font-weight: 600; margin-bottom: 10px; }
.newsletter-text {
  background: rgba(168,85,247,0.06); border: 1px solid rgba(168,85,247,0.15);
  padding: 18px; border-radius: 12px; font-size: 14px; color: #f1f5f9; line-height: 1.8; white-space: pre-wrap;
}
.ai-links-label { font-size: 13px; color: #94a3b8; font-weight: 600; margin-bottom: 12px; }
.ai-link-card {
  padding: 14px 18px; border: 1px solid rgba(255,255,255,0.06);
  border-radius: 12px; margin-bottom: 10px; background: rgba(15,23,42,0.4); transition: all 0.2s;
}
.ai-link-card:hover { border-color: rgba(168,85,247,0.3); background: rgba(168,85,247,0.06); }
.link-title { font-weight: 600; color: #f8fafc; font-size: 14px; margin-bottom: 4px; }
.link-slug { font-size: 12px; color: #22d3ee; font-family: monospace; margin-bottom: 6px; }
.link-reason { font-size: 13px; color: #94a3b8; margin-bottom: 8px; }
.ai-empty { text-align: center; padding: 40px; color: #64748b; font-size: 15px; }
.ai-outline-block {
  white-space: pre-wrap; font-family: monospace;
  background: rgba(15,23,42,0.6); padding: 18px; border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.06); color: #94a3b8; font-size: 13px; line-height: 1.8;
}
.ai-plain-result {
  background: rgba(15,23,42,0.6); padding: 18px; border-radius: 12px;
  border: 1px solid rgba(255,255,255,0.06); color: #f1f5f9; font-size: 14px; line-height: 1.7;
}

/* ===== Version History ===== */
.version-item {
  padding: 14px 16px; border: 1px solid rgba(255,255,255,0.06); border-radius: 12px;
  margin-bottom: 10px; cursor: pointer; transition: all 0.2s; background: rgba(15,23,42,0.4);
}
.version-item:hover { border-color: rgba(168,85,247,0.3); background: rgba(168,85,247,0.06); }
.version-item.active { border-color: #a855f7; background: rgba(168,85,247,0.1); }
.version-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.version-time { font-size: 12px; color: #475569; }
.version-title { font-size: 14px; font-weight: 500; color: #f8fafc; }
.version-actions { margin-top: 8px; display: flex; gap: 4px; }
.version-content-preview {
  background: rgba(15,23,42,0.6); padding: 16px; border-radius: 10px;
  border: 1px solid rgba(255,255,255,0.05);
  font-size: 13px; white-space: pre-wrap; word-break: break-word;
  max-height: 400px; overflow-y: auto; font-family: monospace; color: #94a3b8;
}

/* ===== Responsive ===== */
@media (max-width: 1100px) {
  .editor-shell { flex-direction: column; height: auto; min-height: 100vh; }
  .panel-left { display: none; }
  .divider { display: none; }
  .panel-right { width: 100%; border-left: none; border-top: 1px solid rgba(168,85,247,0.15); }
  .panel-center { min-height: 60vh; }
  .bottom-bar { left: 0; }
}
</style>
