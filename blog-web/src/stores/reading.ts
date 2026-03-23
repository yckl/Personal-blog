import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export const useReadingStore = defineStore('reading', () => {
  // Theme: 'dark' | 'light'
  const theme = ref(localStorage.getItem('blog-theme') || 'dark')

  // Font size: 14 - 22
  const fontSize = ref(parseInt(localStorage.getItem('blog-font-size') || '16'))

  // Line width: 'normal' | 'narrow' | 'wide'
  const lineWidth = ref(localStorage.getItem('blog-line-width') || 'normal')

  // Reading mode (distraction-free)
  const readingMode = ref(false)

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  function increaseFontSize() {
    if (fontSize.value < 22) fontSize.value += 1
  }

  function decreaseFontSize() {
    if (fontSize.value > 14) fontSize.value -= 1
  }

  function cycleLineWidth() {
    const widths = ['narrow', 'normal', 'wide']
    const idx = widths.indexOf(lineWidth.value)
    lineWidth.value = widths[(idx + 1) % widths.length]
  }

  function toggleReadingMode() {
    readingMode.value = !readingMode.value
  }

  // Persist to localStorage
  watch(theme, (v) => {
    localStorage.setItem('blog-theme', v)
    applyTheme(v)
  })
  watch(fontSize, (v) => {
    localStorage.setItem('blog-font-size', String(v))
    document.documentElement.style.setProperty('--reading-font-size', v + 'px')
  })
  watch(lineWidth, (v) => {
    localStorage.setItem('blog-line-width', v)
    applyLineWidth(v)
  })

  function applyTheme(t: string) {
    document.documentElement.setAttribute('data-theme', t)
  }

  function applyLineWidth(w: string) {
    const widthMap: Record<string, string> = { narrow: '720px', normal: '1100px', wide: '1400px' }
    document.documentElement.style.setProperty('--max-width', widthMap[w] || '1100px')
  }

  // Initialize on mount
  function init() {
    applyTheme(theme.value)
    document.documentElement.style.setProperty('--reading-font-size', fontSize.value + 'px')
    applyLineWidth(lineWidth.value)
  }

  return {
    theme, fontSize, lineWidth, readingMode,
    toggleTheme, increaseFontSize, decreaseFontSize,
    cycleLineWidth, toggleReadingMode, init
  }
})
