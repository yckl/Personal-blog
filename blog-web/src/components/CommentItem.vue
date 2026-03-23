<template>
  <div class="comment-item" :class="{ 'is-reply': isReply, 'is-pinned': comment.isPinned }">
    <div class="comment-avatar">
      <img v-if="comment.authorAvatar" :src="comment.authorAvatar" alt="" />
      <span v-else class="avatar-placeholder">{{ initial }}</span>
    </div>
    <div class="comment-body">
      <div class="comment-header">
        <span class="author-name">
          <a v-if="comment.authorUrl" :href="comment.authorUrl" target="_blank">{{ comment.authorName }}</a>
          <template v-else>{{ comment.authorName }}</template>
        </span>
        <span v-if="comment.isPinned" class="pin-badge">📌 Pinned</span>
        <span class="comment-date">{{ timeAgo(comment.createdAt) }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
      <div class="comment-actions">
        <button class="action-btn" @click="$emit('like', comment.id)" :title="'Like'">
          ❤️ {{ comment.likeCount || 0 }}
        </button>
        <button class="action-btn" @click="$emit('reply', comment.id)">
          💬 Reply
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  comment: any
  articleId: number
  isReply?: boolean
}>()

defineEmits<{
  (e: 'reply', id: number): void
  (e: 'like', id: number): void
}>()

const initial = computed(() => {
  const name = props.comment.authorName || '?'
  return name.charAt(0).toUpperCase()
})

function timeAgo(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return 'Just now'
  if (diffMin < 60) return `${diffMin}m ago`
  const diffHr = Math.floor(diffMin / 60)
  if (diffHr < 24) return `${diffHr}h ago`
  const diffDay = Math.floor(diffHr / 24)
  if (diffDay < 30) return `${diffDay}d ago`
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
</script>

<style scoped>
.comment-item { display: flex; gap: 12px; padding: 16px; border-radius: 10px; transition: background 0.2s; }
.comment-item:hover { background: var(--bg-hover); }
.comment-item.is-pinned { background: rgba(99,102,241,0.05); border: 1px solid rgba(99,102,241,0.2); }
.comment-item.is-reply { padding: 12px; }

.comment-avatar { flex-shrink: 0; }
.comment-avatar img { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; }
.avatar-placeholder { width: 40px; height: 40px; border-radius: 50%; background: var(--gradient-1); display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; color: #fff; }
.is-reply .comment-avatar img, .is-reply .avatar-placeholder { width: 32px; height: 32px; font-size: 14px; }

.comment-body { flex: 1; min-width: 0; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; flex-wrap: wrap; }
.author-name { font-size: 14px; font-weight: 600; color: var(--text-heading); }
.author-name a { color: var(--primary-light); text-decoration: none; }
.author-name a:hover { text-decoration: underline; }
.pin-badge { font-size: 11px; color: var(--primary-light); background: rgba(99,102,241,0.15); padding: 2px 8px; border-radius: 4px; }
.comment-date { font-size: 12px; color: var(--text-dim); }
.comment-content { font-size: 14px; line-height: 1.6; color: var(--text-muted); word-break: break-word; }
.comment-actions { display: flex; gap: 12px; margin-top: 8px; }
.action-btn { background: none; border: none; font-size: 13px; color: var(--text-dim); cursor: pointer; padding: 4px 8px; border-radius: 6px; transition: all 0.2s; }
.action-btn:hover { background: var(--bg-card); color: var(--text-muted); }
</style>
