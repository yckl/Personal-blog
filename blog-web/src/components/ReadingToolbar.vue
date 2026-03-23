<template>
  <div class="reading-toolbar" :class="{ collapsed: isCollapsed }">
    <button class="toolbar-toggle" @click="isCollapsed = !isCollapsed" :title="isCollapsed ? 'Show toolbar' : 'Hide toolbar'">
      {{ isCollapsed ? '⚙️' : '✕' }}
    </button>
    <div v-show="!isCollapsed" class="toolbar-controls">
      <!-- Theme Toggle -->
      <button class="toolbar-btn" @click="store.toggleTheme()" :title="store.theme === 'dark' ? 'Switch to Light' : 'Switch to Dark'">
        {{ store.theme === 'dark' ? '☀️' : '🌙' }}
      </button>

      <!-- Font Size -->
      <div class="toolbar-group">
        <button class="toolbar-btn small" @click="store.decreaseFontSize()" title="Decrease font">A-</button>
        <span class="toolbar-label">{{ store.fontSize }}</span>
        <button class="toolbar-btn small" @click="store.increaseFontSize()" title="Increase font">A+</button>
      </div>

      <!-- Line Width -->
      <button class="toolbar-btn" @click="store.cycleLineWidth()" :title="'Width: ' + store.lineWidth">
        {{ store.lineWidth === 'narrow' ? '↔' : store.lineWidth === 'wide' ? '⟷' : '↹' }}
      </button>

      <!-- Reading Mode -->
      <button class="toolbar-btn" :class="{ active: store.readingMode }" @click="store.toggleReadingMode()" title="Distraction-free">
        📖
      </button>

      <!-- Print -->
      <button class="toolbar-btn" @click="printPage" title="Print">
        🖨️
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useReadingStore } from '../stores/reading'

const store = useReadingStore()
const isCollapsed = ref(true)

function printPage() {
  window.print()
}
</script>

<style scoped>
.reading-toolbar {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.toolbar-toggle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text);
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(0,0,0,0.3);
}
.toolbar-toggle:hover {
  background: var(--bg-hover);
  transform: scale(1.05);
}

.toolbar-controls {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.4);
  backdrop-filter: blur(12px);
  animation: slideUp 0.2s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.toolbar-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--text);
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}
.toolbar-btn:hover {
  background: var(--bg-hover);
  border-color: var(--border);
}
.toolbar-btn.active {
  background: rgba(99,102,241,0.15);
  border-color: var(--primary);
  color: var(--primary-light);
}
.toolbar-btn.small {
  width: 30px;
  height: 30px;
  font-size: 12px;
  font-weight: 700;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px;
  background: var(--bg-hover);
  border-radius: 8px;
}

.toolbar-label {
  font-size: 12px;
  color: var(--text-muted);
  min-width: 20px;
  text-align: center;
  font-weight: 600;
}

@media print {
  .reading-toolbar { display: none !important; }
}
</style>
