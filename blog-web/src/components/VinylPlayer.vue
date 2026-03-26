<template>
  <div
    ref="playerRef"
    class="vinyl-player"
    :class="{ playing: isPlaying, dragging: isDragging }"
    :style="positionStyle"
    @mouseenter="hover = true"
    @mouseleave="hover = false"
  >
    <!-- Drag Handle (the disc itself acts as drag handle) -->
    
    <!-- Info Panel Slide Out -->
    <div class="info-panel glass-card" :class="{ 'panel-visible': hover || isPlaying }">
      <div class="song-info">
        <div class="title">{{ currentTrack?.title || '寻音之旅' }}</div>
        <div class="author">{{ currentTrack?.author || '正在为您捕捉电波...' }}</div>
      </div>
      <div class="controls">
        <button class="btn-icon" @click.stop="prevTrack" title="上一首">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="19 20 9 12 19 4 19 20"></polygon><line x1="5" y1="19" x2="5" y2="5"></line></svg>
        </button>
        <button class="btn-icon play-btn" @click.stop="togglePlay" :title="isPlaying ? '暂停' : '播放'">
          <svg v-if="!isPlaying" width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
          <svg v-else width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="6" y="4" width="4" height="16"></rect><rect x="14" y="4" width="4" height="16"></rect></svg>
        </button>
        <button class="btn-icon" @click.stop="nextTrack" title="下一首">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="5 4 15 12 5 20 5 4"></polygon><line x1="19" y1="5" x2="19" y2="19"></line></svg>
        </button>
      </div>
    </div>

    <!-- The Vinyl Disc (also the drag handle) -->
    <div
      class="vinyl-disc"
      @mousedown.prevent="startDrag"
      @touchstart.prevent="startDragTouch"
    >
      <div class="vinyl-texture"></div>
      <div class="vinyl-track-lines"></div>
      <img v-if="currentTrack?.pic" :src="currentTrack.pic" class="album-cover" />
      <div v-else class="album-cover placeholder-cover"></div>
      <div class="vinyl-hole"></div>

      <!-- Tonearm Stylus -->
      <div class="tonearm" :class="{ 'tonearm-playing': isPlaying }"></div>
    </div>
    
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'

const STORAGE_KEY = 'vinyl-player-pos'

const hover = ref(false)
const isPlaying = ref(false)
const isDragging = ref(false)
const playlist = ref<any[]>([])
const currentIndex = ref(0)
const playerRef = ref<HTMLElement | null>(null)
let audio = new Audio()

// --- Position state (use right/bottom for initial, switch to left/top on drag) ---
const posX = ref<number | null>(null)  // left px
const posY = ref<number | null>(null)  // top px

const positionStyle = computed(() => {
  if (posX.value !== null && posY.value !== null) {
    return {
      left: posX.value + 'px',
      top: posY.value + 'px',
      right: 'auto',
      bottom: 'auto',
    }
  }
  // default position: bottom-right, raised up to avoid overlap
  return {
    right: '30px',
    bottom: '100px',
    left: 'auto',
    top: 'auto',
  }
})

// --- Drag logic ---
let dragStartX = 0
let dragStartY = 0
let elemStartX = 0
let elemStartY = 0
let hasMoved = false

function getElemPos() {
  if (!playerRef.value) return { x: 0, y: 0 }
  const rect = playerRef.value.getBoundingClientRect()
  return { x: rect.left, y: rect.top }
}

function startDrag(e: MouseEvent) {
  hasMoved = false
  isDragging.value = true
  dragStartX = e.clientX
  dragStartY = e.clientY
  const pos = getElemPos()
  elemStartX = pos.x
  elemStartY = pos.y

  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

function startDragTouch(e: TouchEvent) {
  if (!e.touches.length) return
  hasMoved = false
  isDragging.value = true
  dragStartX = e.touches[0].clientX
  dragStartY = e.touches[0].clientY
  const pos = getElemPos()
  elemStartX = pos.x
  elemStartY = pos.y

  document.addEventListener('touchmove', onDragTouch, { passive: false })
  document.addEventListener('touchend', stopDragTouch)
}

function onDrag(e: MouseEvent) {
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  applyDrag(dx, dy)
}

function onDragTouch(e: TouchEvent) {
  if (!e.touches.length) return
  e.preventDefault()
  const dx = e.touches[0].clientX - dragStartX
  const dy = e.touches[0].clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  applyDrag(dx, dy)
}

function applyDrag(dx: number, dy: number) {
  let newX = elemStartX + dx
  let newY = elemStartY + dy

  // Clamp within viewport
  const vw = window.innerWidth
  const vh = window.innerHeight
  const elW = playerRef.value?.offsetWidth || 90
  const elH = playerRef.value?.offsetHeight || 90

  newX = Math.max(0, Math.min(newX, vw - elW))
  newY = Math.max(0, Math.min(newY, vh - elH))

  posX.value = newX
  posY.value = newY
}

function stopDrag() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  savePosition()

  // If didn't move, treat as click → toggle play
  if (!hasMoved) {
    togglePlay()
  }
}

function stopDragTouch() {
  isDragging.value = false
  document.removeEventListener('touchmove', onDragTouch)
  document.removeEventListener('touchend', stopDragTouch)
  savePosition()

  if (!hasMoved) {
    togglePlay()
  }
}

function savePosition() {
  if (posX.value !== null && posY.value !== null) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ x: posX.value, y: posY.value }))
  }
}

function restorePosition() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const { x, y } = JSON.parse(raw)
      // Validate within viewport
      const vw = window.innerWidth
      const vh = window.innerHeight
      if (x >= 0 && x < vw - 50 && y >= 0 && y < vh - 50) {
        posX.value = x
        posY.value = y
      }
    }
  } catch { /* ignore */ }
}

const currentTrack = computed(() => playlist.value[currentIndex.value])

async function loadPlaylist() {
  try {
    const res = await fetch('https://api.i-meto.com/meting/api?server=netease&type=playlist&id=7459495471')
    playlist.value = await res.json()
    if (playlist.value.length > 0) {
      audio.src = playlist.value[0].url
    }
  } catch (e) {
    console.error('Failed to load playlist', e)
  }
}

function togglePlay() {
  if (!playlist.value.length) return
  if (isPlaying.value) {
    audio.pause()
  } else {
    audio.play()
  }
}

function nextTrack() {
  if (!playlist.value.length) return
  currentIndex.value = (currentIndex.value + 1) % playlist.value.length
  playNewTrack()
}

function prevTrack() {
  if (!playlist.value.length) return
  currentIndex.value = (currentIndex.value - 1 + playlist.value.length) % playlist.value.length
  playNewTrack()
}

function playNewTrack() {
  audio.src = currentTrack.value.url
  if (isPlaying.value) {
    audio.play()
  }
}

onMounted(() => {
  restorePosition()
  loadPlaylist()
  audio.volume = 0.6

  audio.addEventListener('play', () => isPlaying.value = true)
  audio.addEventListener('pause', () => isPlaying.value = false)
  audio.addEventListener('ended', nextTrack)
  audio.addEventListener('error', nextTrack)
})

onUnmounted(() => {
  audio.pause()
  audio.src = ''
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
  document.removeEventListener('touchmove', onDragTouch)
  document.removeEventListener('touchend', stopDragTouch)
})
</script>

<style scoped>
.vinyl-player {
  position: fixed;
  bottom: 100px;
  right: 30px;
  z-index: 999999;
  display: flex;
  align-items: center;
  font-family: var(--font-body, system-ui, sans-serif);
  user-select: none;
  touch-action: none;
}

.vinyl-player.dragging {
  cursor: grabbing;
  transition: none !important;
}

.vinyl-player:not(.dragging) {
  transition: box-shadow 0.3s ease;
}

/* ===== Info Panel ===== */
.info-panel {
  display: flex;
  align-items: center;
  gap: 16px;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  padding: 12px 40px 12px 24px;
  border-radius: 99px 0 0 99px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-right: none;
  
  transform: translateX(50px);
  opacity: 0;
  pointer-events: none;
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  margin-right: -30px; /* pull right to slip under the vinyl */
  box-shadow: -10px 10px 30px rgba(0,0,0,0.3);
}

.panel-visible {
  transform: translateX(0);
  opacity: 1;
  pointer-events: auto;
}

.song-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  text-align: right;
  min-width: 120px;
}
.title { 
  font-size: 14px; font-weight: 800; color: #fff; 
  max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.author { 
  font-size: 11px; color: #94a3b8; margin-top: 4px;
  max-width: 140px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.btn-icon {
  background: none; border: none; color: #fff; cursor: pointer; 
  padding: 8px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.btn-icon:hover { background: rgba(255, 255, 255, 0.1); transform: scale(1.1); }
.play-btn { 
  background: linear-gradient(135deg, var(--primary-light, #c084fc), var(--primary, #a855f7)); 
  box-shadow: 0 4px 12px rgba(168, 85, 247, 0.4); 
  color: #fff; 
}
.play-btn:hover {
  box-shadow: 0 6px 16px rgba(168, 85, 247, 0.6);
  transform: scale(1.15);
}

/* ===== Vinyl Disc ===== */
.vinyl-disc {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: #000;
  box-shadow: 0 15px 35px rgba(0,0,0,0.5), inset 0 0 0 4px rgba(255,255,255,0.05);
  position: relative;
  cursor: grab;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.dragging .vinyl-disc { cursor: grabbing; }
.vinyl-disc:hover { transform: scale(1.05); box-shadow: 0 20px 40px rgba(0,0,0,0.6), inset 0 0 0 4px rgba(255,255,255,0.1); }

/* Texture grooving rings */
.vinyl-texture {
  position: absolute; inset: 4px; border-radius: 50%;
  background: repeating-radial-gradient(#111 0%, #111 2%, #1a1a1a 3%, #111 4%);
  z-index: 1;
}
.vinyl-track-lines {
  position: absolute; inset: 0; border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(255,255,255,0.05) 0deg, transparent 45deg, rgba(255,255,255,0.05) 90deg, transparent 135deg, rgba(255,255,255,0.05) 180deg, transparent 225deg, rgba(255,255,255,0.05) 270deg, transparent 315deg);
  z-index: 2;
}

.album-cover {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  z-index: 3;
  object-fit: cover;
  box-shadow: 0 0 0 2px #0f172a, 0 0 8px rgba(0,0,0,0.8);
  animation: spin 6s linear infinite;
  animation-play-state: paused;
}
.placeholder-cover {
  background: var(--gradient-1, linear-gradient(135deg, #a855f7, #ec4899));
}

.playing .album-cover {
  animation-play-state: running;
}

.vinyl-hole {
  position: absolute;
  width: 6px;
  height: 6px;
  background: #fff;
  border-radius: 50%;
  z-index: 4;
}

/* ===== Tonearm ===== */
.tonearm {
  position: absolute;
  top: -10px;
  right: -5px;
  width: 14px;
  height: 36px;
  background: transparent;
  border-left: 3px solid #cbd5e1;
  border-bottom: 3px solid #cbd5e1;
  border-radius: 0 0 0 6px;
  z-index: 5;
  transform-origin: top right;
  transform: rotate(-30deg);
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  filter: drop-shadow(2px 4px 4px rgba(0,0,0,0.5));
}
.tonearm::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: -2px;
  width: 6px;
  height: 10px;
  background: #f43f5e;
  border-radius: 2px;
}
.tonearm::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 10px;
  height: 10px;
  background: #94a3b8;
  border-radius: 50%;
  transform: translate(50%, -50%);
}

.tonearm-playing {
  transform: rotate(15deg);
}

@keyframes spin { 100% { transform: rotate(360deg); } }
</style>
