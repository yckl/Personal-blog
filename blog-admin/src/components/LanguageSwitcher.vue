<template>
  <el-dropdown @command="switchLocale" trigger="click">
    <span class="lang-toggle">
      <el-icon :size="16"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg></el-icon>
      <span>{{ currentLabel }}</span>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="zh-CN" :disabled="locale === 'zh-CN'">中文</el-dropdown-item>
        <el-dropdown-item command="en-US" :disabled="locale === 'en-US'">English</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const { locale } = useI18n()

const currentLabel = computed(() => locale.value === 'zh-CN' ? '中文' : 'EN')

function switchLocale(lang: string) {
  locale.value = lang
  localStorage.setItem('locale', lang)
}
</script>

<style scoped>
.lang-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #666;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.2s;
}
.lang-toggle:hover {
  color: #409eff;
  background: rgba(64, 158, 255, 0.06);
}
</style>
