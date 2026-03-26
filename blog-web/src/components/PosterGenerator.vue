<template>
  <div class="poster-generator">
    <button class="btn btn-outline btn-glow poster-btn" @click="generatePoster" :disabled="generating">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
        <circle cx="8.5" cy="8.5" r="1.5"/>
        <polyline points="21 15 16 10 5 21"/>
      </svg>
      {{ generating ? '生成海报中...' : '生成专属海报' }}
    </button>

    <!-- Hidden Poster Template -->
    <div class="poster-offscreen-container">
      <div ref="posterNode" class="poster-canvas glass-card" v-show="true" :style="{ backgroundImage: article.coverImage ? `linear-gradient(to bottom, rgba(15, 23, 42, 0.7), rgba(15, 23, 42, 0.95)), url(${article.coverImage})` : 'linear-gradient(135deg, #1e293b, #0f172a)' }">
        
        <div class="poster-header">
          <div class="poster-brand">
            <span class="brand-y">Y</span>CK's Blog
          </div>
          <div class="poster-date">{{ formatDate(article.publishedAt || article.createdAt) }}</div>
        </div>

        <div class="poster-body">
          <h1 class="poster-title">{{ article.title }}</h1>
          <p class="poster-excerpt">{{ article.excerpt || truncate(article.contentMd || '', 100) }}</p>
          <div class="poster-meta">
            <img v-if="article.authorAvatar" :src="article.authorAvatar" class="avatar" />
            <span>{{ article.authorName }}</span>
            <span class="dot">·</span>
            <span class="tag" v-if="article.categoryName">{{ article.categoryName }}</span>
          </div>
        </div>

        <div class="poster-footer glass">
          <div class="qr-code-box">
            <qrcode-vue :value="pageUrl" :size="70" level="M" :margin="2" />
          </div>
          <div class="scan-hint">
            <p>长按扫码，阅读全文</p>
            <p class="url-text">{{ domain }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Generated Image Modal -->
    <Transition name="fade">
      <div v-if="posterImageUrl" class="poster-modal" @click.self="posterImageUrl = ''">
        <div class="poster-preview glass-card">
          <button class="close-btn" @click="posterImageUrl = ''">✕</button>
          <img :src="posterImageUrl" alt="Article Poster" />
          <div class="poster-actions">
             <p class="hint">长按或右键保存图片</p>
             <a :href="posterImageUrl" :download="`poster_${article.id}.png`" class="btn btn-primary btn-glow">下载原图</a>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import html2canvas from 'html2canvas'
import QrcodeVue from 'qrcode.vue'
import type { Article } from '../types'

const props = defineProps<{
  article: Article
}>()

const generating = ref(false)
const posterImageUrl = ref('')
const posterNode = ref<HTMLElement | null>(null)
const pageUrl = computed(() => typeof window !== 'undefined' ? window.location.href : '')
const domain = computed(() => {
  if (typeof window === 'undefined') return ''
  return window.location.hostname
})

function truncate(str: string, max: number) {
  if (!str) return '阅读全文了解更多细节与详细代码实现，探索技术的美感。'
  const text = str.replace(/[#*`\\[\\]()>!\\-\\_]/g, '').trim()
  return text.length > max ? text.substring(0, max) + '...' : text
}

function formatDate(d?: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

async function generatePoster() {
  if (!posterNode.value || generating.value) return
  generating.value = true
  
  try {
    // Give QR code time to mount just in case
    await new Promise(r => setTimeout(r, 100))
    const canvas = await html2canvas(posterNode.value, {
      scale: 2, // High DPI
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#0f172a',
      windowWidth: 700
    })
    posterImageUrl.value = canvas.toDataURL('image/png')
  } catch (err) {
    console.error('Failed to generate poster', err)
    alert('海报生成失败，请稍后重试')
  } finally {
    generating.value = false
  }
}
</script>

<style scoped>
.poster-generator {
  margin-top: 2rem;
  display: flex;
  justify-content: center;
}

.poster-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border-radius: 9999px;
  padding: 0.75rem 2rem;
}

/* Offscreen rendering container */
.poster-offscreen-container {
  position: fixed;
  left: 0;
  top: 0;
  z-index: -9999;
  opacity: 0;
  pointer-events: none;
}

.poster-canvas {
  width: 480px;
  height: 800px;
  background-size: cover;
  background-position: center;
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
  border-radius: 1.5rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
  overflow: hidden;
}

.poster-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-family: 'Inter', sans-serif;
}

.poster-brand {
  font-size: 1.2rem;
  font-weight: 700;
  letter-spacing: 1px;
}
.brand-y {
  color: var(--primary-color);
}
.poster-date {
  font-size: 0.9rem;
  color: var(--text-muted);
}

.poster-body {
  margin-top: auto;
  margin-bottom: 3rem;
}

.poster-title {
  font-size: 2.2rem;
  font-weight: 800;
  line-height: 1.3;
  margin-bottom: 1.5rem;
  text-shadow: 0 4px 12px rgba(0,0,0,0.5);
}

.poster-excerpt {
  font-size: 1.1rem;
  line-height: 1.7;
  color: rgba(255,255,255,0.85);
  margin-bottom: 2rem;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.poster-meta {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1rem;
}
.avatar {
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  border: 2px solid var(--primary-color);
}
.tag {
  background: rgba(168, 85, 247, 0.2);
  color: #d1d5db;
  padding: 0.2rem 0.6rem;
  border-radius: 0.25rem;
  font-size: 0.85rem;
}

.poster-footer {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.25rem;
  border-radius: 1rem;
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.qr-code-box {
  background: #fff;
  padding: 4px;
  border-radius: 0.5rem;
  display: flex;
}

.scan-hint {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
}

.scan-hint p {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 500;
  letter-spacing: 1px;
}
.scan-hint .url-text {
  font-size: 0.85rem;
  color: var(--text-muted);
  font-family: monospace;
}

/* Modal */
.poster-modal {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0,0,0,0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}

.poster-preview {
  position: relative;
  max-width: 480px;
  width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  padding: 1rem;
  background: transparent;
  border: none;
  box-shadow: none;
}

.poster-preview img {
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
  border-radius: 1rem;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.6);
}

.close-btn {
  position: absolute;
  top: -2rem;
  right: -2rem;
  background: none;
  border: none;
  color: #fff;
  font-size: 2rem;
  cursor: pointer;
  transition: transform 0.2s;
}
.close-btn:hover {
  transform: scale(1.1);
}

.poster-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
}
.hint {
  color: #94a3b8;
  font-size: 0.9rem;
}
</style>
