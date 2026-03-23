<template>
  <section class="comment-section">
    <h2>💬 Comments <span v-if="comments.length" class="count">({{ totalCount }})</span></h2>

    <!-- Sort -->
    <div class="sort-bar" v-if="comments.length">
      <button :class="{ active: sortBy === 'newest' }" @click="sortBy = 'newest'">Newest</button>
      <button :class="{ active: sortBy === 'oldest' }" @click="sortBy = 'oldest'">Oldest</button>
      <button :class="{ active: sortBy === 'popular' }" @click="sortBy = 'popular'">Most Liked</button>
    </div>

    <!-- Comment Form -->
    <div class="comment-form-card">
      <h3>Leave a Comment</h3>
      <form @submit.prevent="submitComment(null)">
        <div class="form-row">
          <input v-model="form.authorName" placeholder="Name *" required />
          <input v-model="form.authorEmail" placeholder="Email" type="email" />
        </div>
        <textarea v-model="form.content" placeholder="Share your thoughts..." rows="4" required />
        <div class="form-actions">
          <span v-if="submitError" class="error-msg">{{ submitError }}</span>
          <span v-if="submitSuccess" class="success-msg">✅ Comment submitted! Awaiting review.</span>
          <button type="submit" class="btn btn-primary" :disabled="submitting">
            {{ submitting ? 'Submitting...' : '💬 Post Comment' }}
          </button>
        </div>
      </form>
    </div>

    <!-- Comment Tree -->
    <div class="comments-list">
      <div v-for="c in sortedComments" :key="c.id" class="comment-thread">
        <CommentItem :comment="c" :articleId="articleId"
          @reply="openReply" @like="likeComment" />
        <!-- Replies -->
        <div class="replies" v-if="c.children && c.children.length">
          <CommentItem v-for="reply in c.children" :key="reply.id"
            :comment="reply" :articleId="articleId" :isReply="true"
            @reply="openReply" @like="likeComment" />
        </div>
        <!-- Reply Form -->
        <div v-if="replyingTo === c.id" class="reply-form">
          <form @submit.prevent="submitComment(c.id)">
            <textarea v-model="replyContent" :placeholder="`Reply to ${c.authorName}...`" rows="3" required />
            <div class="form-actions">
              <button type="button" class="btn btn-ghost" @click="replyingTo = null">Cancel</button>
              <button type="submit" class="btn btn-primary btn-sm" :disabled="submitting">Reply</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <p v-if="!comments.length && !loading" class="empty-comments">No comments yet. Be the first to share your thoughts!</p>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '../utils/request'
import CommentItem from './CommentItem.vue'

const props = defineProps<{ articleId: number }>()

const comments = ref<any[]>([])
const loading = ref(true)
const sortBy = ref('newest')
const replyingTo = ref<number | null>(null)
const replyContent = ref('')
const submitting = ref(false)
const submitError = ref('')
const submitSuccess = ref(false)
const form = ref({ authorName: '', authorEmail: '', content: '' })

// Load saved name/email from localStorage
onMounted(() => {
  form.value.authorName = localStorage.getItem('comment-name') || ''
  form.value.authorEmail = localStorage.getItem('comment-email') || ''
  loadComments()
})

const totalCount = computed(() => {
  let count = 0
  for (const c of comments.value) {
    count += 1 + (c.children?.length || 0)
  }
  return count
})

const sortedComments = computed(() => {
  const list = [...comments.value]
  // Pinned always first
  const pinned = list.filter((c: any) => c.isPinned)
  const unpinned = list.filter((c: any) => !c.isPinned)

  switch (sortBy.value) {
    case 'oldest':
      unpinned.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
      break
    case 'popular':
      unpinned.sort((a, b) => (b.likeCount || 0) - (a.likeCount || 0))
      break
    default: // newest
      unpinned.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
  }
  return [...pinned, ...unpinned]
})

async function loadComments() {
  loading.value = true
  try {
    const res: any = await request.get(`/api/comments/article/${props.articleId}`)
    comments.value = res.data || []
  } catch {}
  finally { loading.value = false }
}

function openReply(commentId: number) {
  replyingTo.value = replyingTo.value === commentId ? null : commentId
  replyContent.value = ''
}

async function likeComment(id: number) {
  try {
    await request.post(`/api/comments/${id}/like`)
    // Optimistic update
    const updateLike = (list: any[]) => {
      for (const c of list) {
        if (c.id === id) { c.likeCount = (c.likeCount || 0) + 1; return }
        if (c.children) updateLike(c.children)
      }
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
    // Save name/email for next time
    localStorage.setItem('comment-name', name.trim())
    if (form.value.authorEmail) localStorage.setItem('comment-email', form.value.authorEmail.trim())

    if (parentId) { replyContent.value = ''; replyingTo.value = null }
    else { form.value.content = '' }
    submitSuccess.value = true
    setTimeout(() => { submitSuccess.value = false }, 5000)
    // Reload comments (the new one won't show until approved, but just in case)
    await loadComments()
  } catch (e: any) {
    submitError.value = e?.response?.data?.message || 'Failed to post comment'
  } finally { submitting.value = false }
}
</script>

<style scoped>
.comment-section { margin-top: 48px; }
.comment-section h2 { font-size: 24px; font-weight: 700; color: var(--text-heading); margin-bottom: 16px; }
.count { font-size: 16px; color: var(--text-dim); font-weight: 400; }

.sort-bar { display: flex; gap: 8px; margin-bottom: 20px; }
.sort-bar button { padding: 6px 16px; border-radius: 20px; font-size: 13px; border: 1px solid var(--border); background: var(--bg-card); color: var(--text-muted); cursor: pointer; transition: all 0.2s; }
.sort-bar button.active { background: rgba(99,102,241,0.15); border-color: var(--primary); color: var(--primary-light); }

.comment-form-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 24px; margin-bottom: 24px; }
.comment-form-card h3 { font-size: 16px; font-weight: 600; color: var(--text-heading); margin-bottom: 16px; }
.form-row { display: flex; gap: 12px; margin-bottom: 12px; }
.form-row input { flex: 1; padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg); color: var(--text); font-size: 14px; outline: none; }
.form-row input:focus { border-color: var(--primary); }
textarea { width: 100%; padding: 12px 14px; border-radius: 8px; border: 1px solid var(--border); background: var(--bg); color: var(--text); font-size: 14px; outline: none; resize: vertical; font-family: inherit; margin-bottom: 12px; }
textarea:focus { border-color: var(--primary); }
.form-actions { display: flex; align-items: center; justify-content: flex-end; gap: 12px; }
.btn { padding: 10px 20px; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; border: none; transition: all 0.2s; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-dark); }
.btn-ghost { background: transparent; color: var(--text-muted); border: 1px solid var(--border); }
.btn-sm { padding: 8px 16px; font-size: 13px; }
.error-msg { color: var(--accent-red); font-size: 13px; }
.success-msg { color: var(--accent-green); font-size: 13px; }

.comments-list { display: flex; flex-direction: column; gap: 16px; }
.comment-thread { }
.replies { padding-left: 40px; border-left: 2px solid var(--border); margin-top: 8px; display: flex; flex-direction: column; gap: 8px; }
.reply-form { padding-left: 40px; margin-top: 8px; }

.empty-comments { text-align: center; color: var(--text-dim); padding: 40px; font-size: 15px; }

@media (max-width: 768px) {
  .form-row { flex-direction: column; }
  .replies { padding-left: 20px; }
  .reply-form { padding-left: 20px; }
}
</style>
