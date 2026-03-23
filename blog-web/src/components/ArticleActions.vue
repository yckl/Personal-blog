<template>
  <div class="article-actions">
    <!-- Like -->
    <button class="action-btn" :class="{ active: liked }" @click="toggleLike">
      <span class="action-icon">{{ liked ? '❤️' : '🤍' }}</span>
      <span class="action-count">{{ likeCount }}</span>
    </button>

    <!-- Favorite -->
    <button class="action-btn" :class="{ active: favorited }" @click="toggleFavorite">
      <span class="action-icon">{{ favorited ? '⭐' : '☆' }}</span>
      <span class="action-label">{{ favorited ? 'Saved' : 'Save' }}</span>
    </button>

    <!-- Share -->
    <div class="share-wrapper">
      <button class="action-btn" @click="shareOpen = !shareOpen">
        <span class="action-icon">🔗</span>
        <span class="action-label">Share</span>
      </button>

      <!-- Share Panel -->
      <div v-if="shareOpen" class="share-panel">
        <button @click="shareTwitter" class="share-btn twitter">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
          Twitter / X
        </button>
        <button @click="shareLinkedIn" class="share-btn linkedin">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433c-1.144 0-2.063-.926-2.063-2.065 0-1.138.92-2.063 2.063-2.063 1.14 0 2.064.925 2.064 2.063 0 1.139-.925 2.065-2.064 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg>
          LinkedIn
        </button>
        <button @click="shareWeChat" class="share-btn wechat">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-7.062-6.122zm-2.18 2.768c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982z"/></svg>
          WeChat
        </button>
        <button @click="copyLink" class="share-btn copy">
          📋 {{ copied ? 'Copied!' : 'Copy Link' }}
        </button>
      </div>
    </div>

    <!-- WeChat QR Modal -->
    <div v-if="showQR" class="qr-overlay" @click="showQR = false">
      <div class="qr-modal" @click.stop>
        <h3>📱 Scan to share on WeChat</h3>
        <div class="qr-placeholder">
          <img v-if="qrUrl" :src="qrUrl" alt="QR Code" />
          <p v-else class="qr-text">{{ articleUrl }}</p>
        </div>
        <p class="qr-hint">Or long-press the QR code to save</p>
        <button class="btn btn-ghost" @click="showQR = false">Close</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../utils/request'

const props = defineProps<{
  articleId: number
  articleTitle: string
  articleUrl: string
}>()

const liked = ref(false)
const likeCount = ref(0)
const favorited = ref(false)
const shareOpen = ref(false)
const copied = ref(false)
const showQR = ref(false)
const qrUrl = ref('')

// Device ID for favorites
function getDeviceId() {
  let id = localStorage.getItem('device-id')
  if (!id) {
    id = 'dev-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
    localStorage.setItem('device-id', id)
  }
  return id
}

onMounted(async () => {
  // Check like status
  try {
    const res: any = await request.get(`/api/public/article/${props.articleId}/like/status`)
    liked.value = res.data?.liked || false
    likeCount.value = res.data?.likeCount || 0
  } catch {}

  // Check favorite status
  try {
    const res: any = await request.get(`/api/public/article/${props.articleId}/favorite/status`, {
      headers: { 'X-Device-Id': getDeviceId() }
    })
    favorited.value = res.data?.favorited || false
  } catch {}

  // Close share panel on click outside
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (!target.closest('.share-wrapper')) shareOpen.value = false
  })
})

async function toggleLike() {
  try {
    const res: any = await request.post(`/api/public/article/${props.articleId}/like`)
    liked.value = res.data?.liked || false
    likeCount.value = res.data?.likeCount || 0
  } catch {}
}

async function toggleFavorite() {
  try {
    const res: any = await request.post(`/api/public/article/${props.articleId}/favorite`, null, {
      headers: { 'X-Device-Id': getDeviceId() }
    })
    favorited.value = res.data?.favorited || false
  } catch {}
}

function shareTwitter() {
  trackShare('twitter')
  window.open(`https://twitter.com/intent/tweet?text=${encodeURIComponent(props.articleTitle)}&url=${encodeURIComponent(props.articleUrl)}`, '_blank')
  shareOpen.value = false
}

function shareLinkedIn() {
  trackShare('linkedin')
  window.open(`https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(props.articleUrl)}`, '_blank')
  shareOpen.value = false
}

function shareWeChat() {
  trackShare('wechat')
  // Generate QR code via public API
  qrUrl.value = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(props.articleUrl)}`
  showQR.value = true
  shareOpen.value = false
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(props.articleUrl)
    copied.value = true
    trackShare('copy_link')
    setTimeout(() => { copied.value = false }, 2000)
  } catch { alert('Failed to copy') }
}

function trackShare(platform: string) {
  request.post(`/api/public/article/${props.articleId}/share?platform=${platform}`).catch(() => {})
}
</script>

<style scoped>
.article-actions { display: flex; gap: 12px; align-items: center; padding: 16px 0; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); margin: 24px 0; }

.action-btn {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 18px; border-radius: 10px;
  border: 1px solid var(--border); background: var(--bg-card);
  color: var(--text-muted); font-size: 14px; cursor: pointer;
  transition: all 0.2s;
}
.action-btn:hover { border-color: var(--primary); color: var(--primary-light); }
.action-btn.active { background: rgba(99,102,241,0.1); border-color: var(--primary); color: var(--primary-light); }
.action-icon { font-size: 18px; }
.action-count { font-weight: 600; }

/* Share dropdown */
.share-wrapper { position: relative; }
.share-panel {
  position: absolute; bottom: 100%; left: 0; z-index: 50;
  background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 12px; padding: 8px; margin-bottom: 8px;
  min-width: 200px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);
  display: flex; flex-direction: column; gap: 4px;
}
.share-btn {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 14px; border-radius: 8px; border: none;
  background: transparent; color: var(--text-muted);
  font-size: 14px; cursor: pointer; transition: all 0.15s;
  text-align: left; width: 100%;
}
.share-btn:hover { background: var(--bg-hover); }
.share-btn.twitter:hover { color: #1DA1F2; }
.share-btn.linkedin:hover { color: #0A66C2; }
.share-btn.wechat:hover { color: #07C160; }

/* QR Modal */
.qr-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); z-index: 100; display: flex; align-items: center; justify-content: center; }
.qr-modal { background: var(--bg-card); border: 1px solid var(--border); border-radius: 16px; padding: 32px; text-align: center; max-width: 320px; }
.qr-modal h3 { font-size: 18px; color: var(--text-heading); margin-bottom: 16px; }
.qr-placeholder { margin: 16px auto; }
.qr-placeholder img { border-radius: 8px; }
.qr-text { word-break: break-all; font-size: 12px; color: var(--text-dim); padding: 16px; background: var(--bg); border-radius: 8px; }
.qr-hint { font-size: 12px; color: var(--text-dim); margin: 12px 0; }
.btn-ghost { background: transparent; border: 1px solid var(--border); color: var(--text-muted); padding: 8px 20px; border-radius: 8px; cursor: pointer; }

@media (max-width: 768px) {
  .action-label { display: none; }
  .share-panel { left: auto; right: 0; }
}
</style>
