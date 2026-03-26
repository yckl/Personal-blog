<template>
  <div class="yt-comment" :class="{ 'is-reply': isReply, 'is-pinned': comment.isPinned }">
    <!-- Avatar -->
    <div class="yt-avatar">
      <img v-if="comment.authorAvatar" :src="comment.authorAvatar" :alt="comment.authorName" />
      <span v-else class="yt-avatar-fallback" :style="{ background: avatarColor }">{{ initial }}</span>
    </div>

    <!-- Body -->
    <div class="yt-body">
      <div class="yt-header">
        <span class="yt-author">
          <a v-if="comment.authorUrl" :href="comment.authorUrl" target="_blank" rel="noopener">{{ comment.authorName }}</a>
          <template v-else>{{ comment.authorName }}</template>
        </span>
        <span v-if="comment.isPinned" class="yt-badge pinned">📌 置顶</span>
        <span v-if="comment.isPending" class="yt-badge pending">⏳ 待审核</span>
        <span class="yt-time">{{ timeAgo(comment.createdAt) }}</span>
      </div>

      <div class="yt-content">
        <span v-if="comment.replyToUserName" class="yt-mention">@{{ comment.replyToUserName }}</span>
        {{ comment.content }}
      </div>

      <div class="yt-actions">
        <button class="yt-action-btn like-btn" :class="{ pulsing: isLiking, liked: isLiked }" @click="handleLike" title="点赞">
          <svg width="16" height="16" viewBox="0 0 24 24" :fill="isLiked ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
            <path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-2.3H14z"/>
            <path d="M7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/>
          </svg>
          <span v-if="comment.likeCount">{{ comment.likeCount }}</span>
        </button>
        <button class="yt-action-btn dislike-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 15V19a3 3 0 0 0 3 3l4-9V2H5.72a2 2 0 0 0-2 1.7l-1.38 9a2 2 0 0 0 2 2.3H10z"/>
            <path d="M17 2h2.67A2.31 2.31 0 0 1 22 4v7a2.31 2.31 0 0 1-2.33 2H17"/>
          </svg>
        </button>
        <button class="yt-action-btn reply-btn" @click="$emit('reply', comment)" v-if="!comment.isPending">
          回复
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  comment: any
  articleId: number
  isReply?: boolean
  likedIds?: Set<number>
}>()

const emit = defineEmits<{
  (e: 'reply', comment: any): void
  (e: 'like', id: number): void
}>()

const isLiking = ref(false)
const isLiked = computed(() => props.likedIds?.has(props.comment.id) ?? false)

function handleLike() {
  if (isLiking.value) return
  isLiking.value = true
  emit('like', props.comment.id)
  setTimeout(() => isLiking.value = false, 400)
}

const initial = computed(() => {
  const name = props.comment.authorName || '?'
  return name.charAt(0).toUpperCase()
})

const avatarColor = computed(() => {
  const colors = ['#ef4444','#f97316','#eab308','#22c55e','#06b6d4','#3b82f6','#8b5cf6','#ec4899']
  const name = props.comment.authorName || ''
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
})

function timeAgo(dateStr: string) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  const diffHr = Math.floor(diffMin / 60)
  if (diffHr < 24) return `${diffHr} 小时前`
  const diffDay = Math.floor(diffHr / 24)
  if (diffDay < 30) return `${diffDay} 天前`
  const diffMonth = Math.floor(diffDay / 30)
  if (diffMonth < 12) return `${diffMonth} 个月前`
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.yt-comment {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  position: relative;
}
.yt-comment.is-pinned {
  background: rgba(168,85,247,0.04);
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px solid rgba(168,85,247,0.12);
}

/* Avatar */
.yt-avatar { flex-shrink: 0; }
.yt-avatar img {
  width: 40px; height: 40px; border-radius: 50%;
  object-fit: cover; cursor: pointer;
}
.yt-avatar-fallback {
  width: 40px; height: 40px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700; color: #fff;
  user-select: none;
}
.is-reply .yt-avatar img,
.is-reply .yt-avatar-fallback {
  width: 28px; height: 28px; font-size: 12px;
}

/* Body */
.yt-body { flex: 1; min-width: 0; }

.yt-header {
  display: flex; align-items: center; gap: 8px;
  margin-bottom: 4px; flex-wrap: wrap;
}
.yt-author {
  font-size: 13px; font-weight: 600;
  color: var(--text-heading);
}
.yt-author a {
  color: var(--primary-light); text-decoration: none;
}
.yt-author a:hover { text-decoration: underline; }
.yt-time {
  font-size: 12px; color: var(--text-dim);
}

.yt-badge {
  font-size: 11px; padding: 1px 8px; border-radius: 4px; font-weight: 600;
}
.yt-badge.pinned { color: var(--primary-light); background: rgba(168,85,247,0.12); }
.yt-badge.pending { color: #f59e0b; background: rgba(245,158,11,0.12); }

/* Content */
.yt-content {
  font-size: 14px; line-height: 1.7;
  color: var(--text-primary);
  word-break: break-word;
  margin-bottom: 4px;
}
.yt-mention {
  color: #3b82f6; font-weight: 600;
  cursor: pointer; margin-right: 4px;
}
.yt-mention:hover { text-decoration: underline; }

/* Actions */
.yt-actions {
  display: flex; align-items: center; gap: 4px;
  margin-top: 2px; margin-left: -8px;
}
.yt-action-btn {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 10px; border-radius: 999px;
  border: none; background: transparent;
  font-size: 12px; font-weight: 500;
  color: var(--text-dim); cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}
.yt-action-btn:hover {
  background: var(--bg-hover);
  color: var(--text-muted);
}
.yt-action-btn.pulsing svg {
  animation: likePulse 0.4s ease;
}
@keyframes likePulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

.reply-btn { font-weight: 600; }
.yt-action-btn.liked { color: #3b82f6; }
.yt-action-btn:disabled { opacity: 0.8; cursor: default; }
</style>
