<template>
  <Transition name="fade-scale">
    <div v-if="isVisible" class="spotlight-overlay" @click.self="close">
      <div class="spotlight-modal glass-card">
        <div class="search-input-wrapper">
          <svg class="search-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input 
            ref="searchInput"
            v-model="keyword"
            @keydown.down.prevent="selectNext"
            @keydown.up.prevent="selectPrev"
            @keydown.enter.prevent="navigate"
            @keydown.esc.prevent="close"
            type="text" 
            placeholder="随时输入任何字符进行全局模糊检索 (Cmd+K) ..." 
            class="search-input" />
          <div class="kbd-hint">ESC</div>
        </div>

        <div class="search-results" v-if="results.length > 0">
          <div 
            v-for="(item, index) in results" 
            :key="item.id" 
            class="result-item"
            :class="{ active: selectedIndex === index }"
            @mouseenter="selectedIndex = index"
            @click="navigate"
          >
            <div class="result-icon">📄</div>
            <div class="result-content">
               <h4 v-html="highlight(item.title)"></h4>
               <p v-html="highlight(item.excerpt || item.title)"></p>
            </div>
          </div>
        </div>

        <div class="search-empty" v-else-if="keyword && !loading">
           <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
           <p>没找到关于 "<span>{{ keyword }}</span>" 的内容</p>
        </div>

        <div class="search-loading" v-else-if="loading">
           <div class="spinner"></div>
           <p>检索中...</p>
        </div>

        <div class="search-footer">
          <div class="footer-tips">
            <span class="tip">键盘控制: <kbd>↑</kbd> <kbd>↓</kbd> 切换选项</span>
            <span class="tip"><kbd>Enter</kbd> 直接进入文章</span>
          </div>
          <div class="brand">YCK's Blog Spotlight</div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { getArticles } from '../api'

const router = useRouter()
const isVisible = ref(false)
const keyword = ref('')
const results = ref<any[]>([])
const loading = ref(false)
const searchInput = ref<HTMLInputElement | null>(null)
const selectedIndex = ref(0)
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// Global Keyboard Listener
const handleGlobalKeydown = (e: KeyboardEvent) => {
  if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
    e.preventDefault()
    openSearch()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleGlobalKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown)
})

const openSearch = () => {
  isVisible.value = true
  keyword.value = ''
  results.value = []
  selectedIndex.value = 0
  nextTick(() => {
    searchInput.value?.focus()
  })
}

const close = () => {
  isVisible.value = false
  keyword.value = ''
}

// Search Logic
watch(keyword, (newVal) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!newVal.trim()) {
    results.value = []
    return
  }
  
  loading.value = true
  debounceTimer = setTimeout(async () => {
    try {
      const res: any = await getArticles({ page: 1, size: 10, keyword: newVal })
      results.value = res.data.records || []
      selectedIndex.value = 0
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  }, 400) // 400ms debounce
})

// Navigation
const selectNext = () => {
  if (results.value.length === 0) return
  selectedIndex.value = (selectedIndex.value + 1) % results.value.length
}

const selectPrev = () => {
  if (results.value.length === 0) return
  selectedIndex.value = (selectedIndex.value - 1 + results.value.length) % results.value.length
}

const navigate = () => {
  if (results.value.length > 0 && results.value[selectedIndex.value]) {
    const item = results.value[selectedIndex.value]
    close()
    router.push(`/article/${item.slug}`)
  }
}

// Helper: Highlight matching keywords
const highlight = (text: string) => {
  if (!text || !keyword.value) return text
  const safeText = text.replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const regex = new RegExp(`(${keyword.value.replace(/[-\\/\\\\^$*+?.()|[\\]{}]/g, '\\\\$&')})`, 'gi')
  return safeText.replace(regex, '<mark>$1</mark>')
}
</script>

<style scoped>
.spotlight-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(16px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 10vh;
}

.spotlight-modal {
  width: 100%;
  max-width: 640px;
  background: rgba(30, 41, 59, 0.85);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 1rem;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255,255,255,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.search-icon {
  color: var(--primary-color);
  margin-right: 1rem;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  color: #f8fafc;
  font-size: 1.25rem;
  outline: none;
  font-family: inherit;
}
.search-input::placeholder {
  color: #64748b;
}

.kbd-hint {
  background: rgba(255,255,255,0.1);
  padding: 0.2rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  color: #94a3b8;
  font-family: monospace;
}

.search-results {
  max-height: 400px;
  overflow-y: auto;
  padding: 1rem;
}
.search-results::-webkit-scrollbar { width: 6px; }
.search-results::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 3px; }

.result-item {
  display: flex;
  align-items: flex-start;
  padding: 1rem;
  border-radius: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
  margin-bottom: 0.5rem;
  border: 1px solid transparent;
}
.result-item.active {
  background: rgba(168, 85, 247, 0.15);
  border-color: rgba(168, 85, 247, 0.3);
  transform: translateX(4px);
}

.result-icon {
  font-size: 1.5rem;
  margin-right: 1rem;
  opacity: 0.8;
}

.result-content h4 {
  margin: 0 0 0.25rem 0;
  color: #f8fafc;
  font-size: 1.05rem;
}
.result-content p {
  margin: 0;
  font-size: 0.85rem;
  color: #94a3b8;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

:deep(mark) {
  background: transparent;
  color: #a855f7;
  font-weight: bold;
  text-decoration: underline;
}

.search-empty, .search-loading {
  padding: 3rem;
  text-align: center;
  color: #64748b;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}
.search-empty span {
  color: #f8fafc;
}

.spinner {
  width: 24px;
  height: 24px;
  border: 2px solid rgba(168, 85, 247, 0.3);
  border-top-color: #a855f7;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin { 100% { transform: rotate(360deg); } }

.search-footer {
  padding: 0.75rem 1.5rem;
  border-top: 1px solid rgba(255,255,255,0.05);
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.75rem;
  color: #64748b;
}

.footer-tips {
  display: flex;
  gap: 1rem;
}
.tip kbd {
  background: rgba(255,255,255,0.1);
  padding: 0.1rem 0.3rem;
  border-radius: 0.2rem;
  font-family: inherit;
  color: #94a3b8;
}

.fade-scale-enter-active, .fade-scale-leave-active { transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1); }
.fade-scale-enter-from, .fade-scale-leave-to { opacity: 0; transform: scale(0.95) translateY(-10px); }
</style>
