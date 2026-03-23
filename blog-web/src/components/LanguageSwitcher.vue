<template>
  <button class="lang-switch" @click="toggleLocale" :aria-label="ariaLabel">
    {{ currentLabel }}
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

const currentLabel = computed(() => locale.value === 'zh-CN' ? 'EN' : '中文')
const ariaLabel = computed(() => locale.value === 'zh-CN' ? 'Switch to English' : '切换至中文')

function toggleLocale() {
  const next = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN'
  locale.value = next
  localStorage.setItem('locale', next)
}
</script>

<style scoped>
.lang-switch {
  background: transparent;
  border: 1px solid var(--border, rgba(255,255,255,0.15));
  color: var(--text-muted, #a0a0b0);
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 0.3px;
}
.lang-switch:hover {
  color: #fff;
  border-color: var(--primary-light, #818cf8);
  background: rgba(129, 140, 248, 0.1);
}
</style>
