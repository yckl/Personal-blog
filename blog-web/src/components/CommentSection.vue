<template>
  <section class="yt-comments">
    <!-- Header -->
    <div class="yt-comments-header">
      <h2>{{ totalCount }} 条评论</h2>
      <div class="yt-sort" v-if="comments.length">
        <button v-for="s in sortOptions" :key="s.key"
          :class="{ active: sortBy === s.key }" @click="sortBy = s.key">
          {{ s.label }}
        </button>
      </div>
    </div>

    <!-- Post Comment (top-level) -->
    <div class="yt-post-box">
      <div class="yt-post-avatar">
        <span class="yt-avatar-fallback" style="background: #6366f1">
          {{ form.authorName ? form.authorName.charAt(0).toUpperCase() : '?' }}
        </span>
      </div>
      <div class="yt-post-form" :class="{ focused: formFocused }">
        <div class="yt-post-inputs" v-show="formFocused">
          <input v-model="form.authorName" placeholder="你的名字 *" maxlength="30" />
          <input v-model="form.authorEmail" placeholder="邮箱（选填，用于 Gravatar 头像）" type="email" maxlength="100" />
        </div>
        <textarea
          ref="mainTextarea"
          v-model="form.content"
          placeholder="添加评论..."
          rows="1"
          @focus="formFocused = true"
          @input="autoResize($event)"
        />
        <div class="yt-post-actions" v-show="formFocused">
          <span v-if="submitError" class="yt-error">{{ submitError }}</span>
          <span v-if="submitSuccess" class="yt-success">✅ 评论已提交，等待审核</span>
          <div class="yt-post-btns">
            <button class="yt-btn cancel" @click="cancelMainForm">取消</button>
            <button class="yt-btn submit" :disabled="!canSubmitMain || submitting" @click="submitComment(null)">
              {{ submitting ? '提交中...' : '评论' }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Comment List -->
    <div class="yt-list">
      <div v-for="c in sortedComments" :key="c.id" class="yt-thread">
        <!-- Root comment -->
        <CommentItem :comment="c" :articleId="articleId" :likedIds="likedIds"
          @reply="openReply" @like="likeComment" />

        <!-- Reply toggle button + guide line container -->
        <div class="yt-replies-zone" v-if="c.children && c.children.length"
             :class="{ 'is-expanded': expandedThreads[c.id], 'is-hovering': hoveringToggle === c.id }">

          <!-- Vertical guide stem (::before on this container) -->
          <button class="yt-toggle-btn"
            @click="toggleReplies(c)"
            @mouseenter="hoveringToggle = c.id"
            @mouseleave="hoveringToggle = null">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <path v-if="!expandedThreads[c.id]" d="M7 10l5 5 5-5z"/>
              <path v-else d="M7 14l5-5 5 5z"/>
            </svg>
            {{ expandedThreads[c.id] ? '隐藏回复' : `${c.children.length} 条回复` }}
          </button>

          <!-- Expanded replies (PC: inline, Mobile: BottomSheet) -->
          <Transition name="yt-expand">
            <div v-if="expandedThreads[c.id] && !isMobile" class="yt-replies-list">
              <div v-for="(reply, idx) in c.children" :key="reply.id"
                   class="yt-reply-wrapper"
                   :class="{ 'is-last': idx === c.children.length - 1 }">
                <CommentItem :comment="reply" :articleId="articleId" :isReply="true" :likedIds="likedIds"
                  @reply="openReply" @like="likeComment" />
              </div>
            </div>
          </Transition>
        </div>

        <!-- Inline reply form -->
        <div v-if="replyingTo?.rootId === c.id || (replyingTo?.id === c.id && !c.parentId)"
             class="yt-reply-form-wrapper">
          <div class="yt-reply-form">
            <div class="yt-post-avatar small">
              <span class="yt-avatar-fallback small" style="background: #8b5cf6">
                {{ form.authorName ? form.authorName.charAt(0).toUpperCase() : '?' }}
              </span>
            </div>
            <div class="yt-post-form focused">
              <textarea
                ref="replyTextarea"
                v-model="replyContent"
                :placeholder="`回复 @${replyingTo?.authorName}...`"
                rows="1"
                @input="autoResize($event)"
              />
              <div class="yt-post-actions">
                <div class="yt-post-btns">
                  <button class="yt-btn cancel" @click="replyingTo = null; replyContent = ''">取消</button>
                  <button class="yt-btn submit" :disabled="!replyContent.trim() || submitting"
                    @click="submitComment(replyingTo!.id)">
                    回复
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty state -->
    <div v-if="!comments.length && !loading" class="yt-empty">
      <div class="yt-empty-icon">💬</div>
      <p>还没有评论，来发表第一条吧！</p>
    </div>

    <!-- Mobile BottomSheet -->
    <Teleport to="body">
      <Transition name="yt-sheet">
        <div v-if="sheetThread && isMobile" class="yt-sheet-overlay" @click.self="sheetThread = null">
          <div class="yt-sheet" ref="sheetRef">
            <div class="yt-sheet-handle" />
            <div class="yt-sheet-header">
              <h3>回复</h3>
              <button class="yt-sheet-close" @click="sheetThread = null">✕</button>
            </div>
            <div class="yt-sheet-body">
              <!-- Root comment in sheet -->
              <CommentItem :comment="sheetThread" :articleId="articleId" :likedIds="likedIds"
                @reply="openReply" @like="likeComment" />
              <!-- All replies -->
              <div class="yt-sheet-replies">
                <div v-for="reply in sheetThread.children" :key="reply.id" class="yt-reply-wrapper">
                  <CommentItem :comment="reply" :articleId="articleId" :isReply="true" :likedIds="likedIds"
                    @reply="openReply" @like="likeComment" />
                </div>
              </div>
              <!-- Reply form in sheet -->
              <div v-if="replyingTo" class="yt-reply-form">
                <div class="yt-post-avatar small">
                  <span class="yt-avatar-fallback small" style="background: #8b5cf6">
                    {{ form.authorName ? form.authorName.charAt(0).toUpperCase() : '?' }}
                  </span>
                </div>
                <div class="yt-post-form focused">
                  <textarea v-model="replyContent"
                    :placeholder="`回复 @${replyingTo.authorName}...`"
                    rows="1" @input="autoResize($event)" />
                  <div class="yt-post-actions">
                    <div class="yt-post-btns">
                      <button class="yt-btn cancel" @click="replyingTo = null; replyContent = ''">取消</button>
                      <button class="yt-btn submit" :disabled="!replyContent.trim() || submitting"
                        @click="submitComment(replyingTo!.id)">回复</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import request from '../utils/request'
import CommentItem from './CommentItem.vue'

const props = defineProps<{ articleId: number }>()

const comments = ref<any[]>([])
const loading = ref(true)
const sortBy = ref('newest')
const formFocused = ref(false)
const form = ref({ authorName: '', authorEmail: '', content: '' })
const replyingTo = ref<any>(null)
const replyContent = ref('')
const submitting = ref(false)
const submitError = ref('')
const submitSuccess = ref(false)
const expandedThreads = ref<Record<number, boolean>>({})
const hoveringToggle = ref<number | null>(null)
const sheetThread = ref<any>(null)
const isMobile = ref(false)
const mainTextarea = ref<HTMLTextAreaElement>()
const replyTextarea = ref<HTMLTextAreaElement>()
const likedIds = ref<Set<number>>(new Set())

const sortOptions = [
  { key: 'newest', label: '最新' },
  { key: 'oldest', label: '最早' },
  { key: 'popular', label: '最热' },
]

const canSubmitMain = computed(() => form.value.authorName.trim() && form.value.content.trim())

const totalCount = computed(() => {
  let count = 0
  for (const c of comments.value) {
    count += 1 + (c.children?.length || 0)
  }
  return count
})

const sortedComments = computed(() => {
  const list = [...comments.value]
  const pinned = list.filter((c: any) => c.isPinned)
  const unpinned = list.filter((c: any) => !c.isPinned)
  switch (sortBy.value) {
    case 'oldest':
      unpinned.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
      break
    case 'popular':
      unpinned.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0))
      break
    default:
      unpinned.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }
  return [...pinned, ...unpinned]
})

function checkMobile() { isMobile.value = window.innerWidth < 768 }

onMounted(() => {
  form.value.authorName = localStorage.getItem('comment-name') || ''
  form.value.authorEmail = localStorage.getItem('comment-email') || ''
  // Restore liked IDs from localStorage
  try {
    const saved = JSON.parse(localStorage.getItem('comment-liked') || '[]')
    likedIds.value = new Set(saved)
  } catch { likedIds.value = new Set() }
  loadComments()
  checkMobile()
  window.addEventListener('resize', checkMobile)
})
onUnmounted(() => { window.removeEventListener('resize', checkMobile) })

async function loadComments() {
  loading.value = true
  try {
    const res: any = await request.get(`/api/comments/article/${props.articleId}`)
    comments.value = res.data || []
  } catch {}
  finally { loading.value = false }
}

function toggleReplies(thread: any) {
  if (isMobile.value) {
    sheetThread.value = thread
  } else {
    expandedThreads.value[thread.id] = !expandedThreads.value[thread.id]
  }
}

function openReply(comment: any) {
  // Determine the root thread for positioning the reply form
  const rootId = comment.rootId || comment.parentId || comment.id
  const resolvedRootId = comment.parentId ? rootId : comment.id
  replyingTo.value = { ...comment, rootId: resolvedRootId }
  replyContent.value = ''
  // Auto-expand the thread so the reply form is visible
  expandedThreads.value[resolvedRootId] = true
  nextTick(() => {
    replyTextarea.value?.focus()
  })
}

async function likeComment(id: number) {
  const alreadyLiked = likedIds.value.has(id)
  try {
    if (alreadyLiked) {
      await request.post(`/api/comments/${id}/unlike`)
      likedIds.value.delete(id)
    } else {
      await request.post(`/api/comments/${id}/like`)
      likedIds.value.add(id)
    }
    localStorage.setItem('comment-liked', JSON.stringify([...likedIds.value]))
    const updateLike = (list: any[]) => {
      for (const c of list) {
        if (c.id === id) {
          c.likeCount = Math.max(0, (c.likeCount || 0) + (alreadyLiked ? -1 : 1))
          return true
        }
        if (c.children && updateLike(c.children)) return true
      }
      return false
    }
    updateLike(comments.value)
  } catch {}
}

async function submitComment(parentId: number | null) {
  submitError.value = ''
  submitSuccess.value = false
  const content = parentId ? replyContent.value : form.value.content
  const name = form.value.authorName
  if (!name.trim() || !content.trim()) return

  submitting.value = true
  try {
    await request.post('/api/comments', {
      articleId: props.articleId,
      parentId,
      authorName: name.trim(),
      authorEmail: form.value.authorEmail.trim() || null,
      content: content.trim()
    })
    localStorage.setItem('comment-name', name.trim())
    if (form.value.authorEmail) localStorage.setItem('comment-email', form.value.authorEmail.trim())

    const newComment: any = {
      id: Date.now(),
      authorName: name.trim(),
      content: content.trim(),
      createdAt: new Date().toISOString(),
      likeCount: 0,
      isPending: true
    }

    if (parentId) {
      // Find root thread and add reply
      newComment.replyToUserName = replyingTo.value?.authorName
      newComment.parentId = parentId
      for (const c of comments.value) {
        if (c.id === parentId || (c.children?.some((r: any) => r.id === parentId))) {
          if (!c.children) c.children = []
          c.children.push(newComment)
          // Auto-expand this thread
          expandedThreads.value[c.id] = true
          break
        }
      }
      replyContent.value = ''
      replyingTo.value = null
    } else {
      newComment.children = []
      comments.value.unshift(newComment)
      form.value.content = ''
      formFocused.value = false
    }
    submitSuccess.value = true
    setTimeout(() => submitSuccess.value = false, 3000)
  } catch (e: any) {
    submitError.value = e?.response?.data?.message || '评论发送失败'
    setTimeout(() => submitError.value = '', 4000)
  } finally { submitting.value = false }
}

function cancelMainForm() {
  formFocused.value = false
  form.value.content = ''
}

function autoResize(e: Event) {
  const el = e.target as HTMLTextAreaElement
  el.style.height = 'auto'
  el.style.height = el.scrollHeight + 'px'
}
</script>

<style scoped>
/* ===== Root ===== */
.yt-comments { margin-top: 48px; }

/* ===== Header ===== */
.yt-comments-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px; gap: 16px; flex-wrap: wrap;
}
.yt-comments-header h2 {
  font-size: 20px; font-weight: 700;
  color: var(--text-heading); margin: 0;
  font-family: var(--font-heading);
}
.yt-sort { display: flex; gap: 4px; }
.yt-sort button {
  padding: 6px 14px; border-radius: 999px; font-size: 13px;
  border: none; background: transparent;
  color: var(--text-dim); cursor: pointer;
  font-weight: 500; font-family: inherit;
  transition: all 0.2s;
}
.yt-sort button:hover { background: var(--bg-hover); }
.yt-sort button.active {
  background: var(--text-heading); color: var(--bg);
}

/* ===== Post Box ===== */
.yt-post-box {
  display: flex; gap: 16px; margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border);
}
.yt-post-avatar { flex-shrink: 0; }
.yt-avatar-fallback {
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: #fff;
  user-select: none;
}
.yt-avatar-fallback.small { width: 28px; height: 28px; font-size: 12px; }
.yt-post-avatar.small { flex-shrink: 0; }

.yt-post-form {
  flex: 1; min-width: 0;
}
.yt-post-inputs {
  display: flex; gap: 12px; margin-bottom: 8px;
}
.yt-post-inputs input {
  flex: 1; padding: 8px 0; border: none;
  border-bottom: 1px solid var(--border);
  background: transparent; color: var(--text-primary);
  font-size: 14px; outline: none;
  font-family: inherit;
  transition: border-color 0.2s;
}
.yt-post-inputs input:focus { border-bottom-color: var(--text-heading); }
.yt-post-inputs input::placeholder { color: var(--text-dim); }

.yt-post-form textarea {
  width: 100%; padding: 8px 0; border: none;
  border-bottom: 1px solid var(--border);
  background: transparent; color: var(--text-primary);
  font-size: 14px; outline: none; resize: none;
  font-family: inherit; min-height: 28px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.yt-post-form textarea:focus { border-bottom-color: var(--text-heading); }
.yt-post-form textarea::placeholder { color: var(--text-dim); }

.yt-post-actions {
  display: flex; align-items: center; justify-content: space-between;
  margin-top: 8px; gap: 8px; flex-wrap: wrap;
}
.yt-post-btns {
  display: flex; gap: 8px; margin-left: auto;
}
.yt-error { font-size: 13px; color: #ef4444; }
.yt-success { font-size: 13px; color: #10b981; }

.yt-btn {
  padding: 8px 16px; border-radius: 999px;
  font-size: 14px; font-weight: 600;
  border: none; cursor: pointer;
  font-family: inherit; transition: all 0.2s;
}
.yt-btn.cancel {
  background: transparent; color: var(--text-muted);
}
.yt-btn.cancel:hover { background: var(--bg-hover); }
.yt-btn.submit {
  background: #3b82f6; color: #fff;
}
.yt-btn.submit:hover { background: #2563eb; }
.yt-btn.submit:disabled {
  opacity: 0.5; cursor: not-allowed;
  background: var(--bg-hover); color: var(--text-dim);
}

/* ===== Comment List ===== */
.yt-list { display: flex; flex-direction: column; }
.yt-thread { position: relative; }

/* ===== Replies Zone — Guide Lines ===== */
.yt-replies-zone {
  position: relative;
  margin-left: 20px;  /* center under root avatar (40px / 2) */
  padding-left: 36px; /* space for guide line + branch */
}

/* Vertical stem line */
.yt-replies-zone::before {
  content: '';
  position: absolute;
  left: 0;
  top: -8px;
  bottom: 0;
  border-left: 2px solid var(--border);
  transition: border-color 0.25s;
}
/* When not expanded, stem only goes to the toggle button */
.yt-replies-zone:not(.is-expanded)::before {
  bottom: calc(100% - 36px);
  top: -8px;
}
/* Hover highlight */
.yt-replies-zone.is-hovering::before {
  border-left-color: var(--text-dim);
}

/* Toggle button */
.yt-toggle-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 16px; border-radius: 999px;
  border: none; background: transparent;
  color: #3b82f6; font-size: 14px; font-weight: 600;
  cursor: pointer; font-family: inherit;
  transition: all 0.2s;
  position: relative;
}
.yt-toggle-btn::before {
  /* branch line from stem to button */
  content: '';
  position: absolute;
  left: -36px; top: 50%;
  width: 24px; height: 16px;
  border-left: 2px solid var(--border);
  border-bottom: 2px solid var(--border);
  border-bottom-left-radius: 12px;
  border-right: none; border-top: none;
  transition: border-color 0.25s;
}
.yt-toggle-btn:hover {
  background: rgba(59,130,246,0.08);
}
.yt-replies-zone.is-hovering .yt-toggle-btn::before {
  border-color: var(--text-dim);
}

/* ===== Replies List ===== */
.yt-replies-list { padding-top: 4px; }

.yt-reply-wrapper {
  position: relative;
  padding-left: 0;
}
/* L-shaped branch for each reply */
.yt-reply-wrapper::before {
  content: '';
  position: absolute;
  left: -36px;
  top: 0;
  width: 24px;
  height: 32px;
  border-left: 2px solid var(--border);
  border-bottom: 2px solid var(--border);
  border-bottom-left-radius: 12px;
  border-right: none; border-top: none;
  transition: border-color 0.25s;
}
/* Vertical continuation line between replies */
.yt-reply-wrapper:not(.is-last)::after {
  content: '';
  position: absolute;
  left: -36px;
  top: 32px;
  bottom: 0;
  border-left: 2px solid var(--border);
  transition: border-color 0.25s;
}
/* Hover highlight on all lines */
.yt-replies-zone.is-hovering .yt-reply-wrapper::before,
.yt-replies-zone.is-hovering .yt-reply-wrapper::after {
  border-color: var(--text-dim);
}

/* ===== Reply form wrapper ===== */
.yt-reply-form-wrapper {
  margin-left: 56px; /* align with root body */
  padding: 8px 0;
}
.yt-reply-form {
  display: flex; gap: 12px;
}

/* ===== Empty ===== */
.yt-empty {
  text-align: center; padding: 48px 24px;
  color: var(--text-dim);
}
.yt-empty-icon { font-size: 48px; margin-bottom: 12px; }
.yt-empty p { font-size: 15px; }

/* ===== Expand Transition ===== */
.yt-expand-enter-active { transition: all 0.3s ease-out; overflow: hidden; }
.yt-expand-leave-active { transition: all 0.2s ease-in; overflow: hidden; }
.yt-expand-enter-from { opacity: 0; max-height: 0; transform: translateY(-8px); }
.yt-expand-enter-to { opacity: 1; max-height: 2000px; transform: translateY(0); }
.yt-expand-leave-from { opacity: 1; max-height: 2000px; }
.yt-expand-leave-to { opacity: 0; max-height: 0; }

/* ===== Mobile BottomSheet ===== */
.yt-sheet-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 10000;
  display: flex; align-items: flex-end;
  backdrop-filter: blur(4px);
}
.yt-sheet {
  background: var(--bg); width: 100%;
  max-height: 85vh;
  border-radius: 16px 16px 0 0;
  display: flex; flex-direction: column;
  overflow: hidden;
  animation: sheetUp 0.3s ease-out;
}
@keyframes sheetUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}
.yt-sheet-handle {
  width: 36px; height: 4px;
  background: var(--text-dim); border-radius: 2px;
  margin: 12px auto 0;
  opacity: 0.4;
}
.yt-sheet-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px 12px; border-bottom: 1px solid var(--border);
}
.yt-sheet-header h3 {
  font-size: 16px; font-weight: 700;
  color: var(--text-heading); margin: 0;
}
.yt-sheet-close {
  width: 32px; height: 32px; border-radius: 50%;
  border: none; background: var(--bg-hover);
  color: var(--text-muted); font-size: 16px;
  cursor: pointer; display: flex;
  align-items: center; justify-content: center;
}
.yt-sheet-body {
  flex: 1; overflow-y: auto;
  padding: 16px 20px; -webkit-overflow-scrolling: touch;
}
.yt-sheet-replies {
  margin-left: 24px; padding-top: 8px;
  border-left: 2px solid var(--border);
  padding-left: 16px;
}

/* Sheet transitions */
.yt-sheet-enter-active { transition: opacity 0.3s; }
.yt-sheet-leave-active { transition: opacity 0.2s; }
.yt-sheet-enter-from, .yt-sheet-leave-to { opacity: 0; }
.yt-sheet-enter-from .yt-sheet { transform: translateY(100%); }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .yt-post-inputs { flex-direction: column; gap: 0; }
  .yt-replies-zone { margin-left: 16px; padding-left: 28px; }
  .yt-toggle-btn::before { left: -28px; width: 18px; }
  .yt-reply-wrapper::before { left: -28px; width: 18px; }
  .yt-reply-wrapper::after { left: -28px; }
  .yt-reply-form-wrapper { margin-left: 40px; }
  .yt-comments-header { flex-direction: column; align-items: flex-start; }
}
</style>
