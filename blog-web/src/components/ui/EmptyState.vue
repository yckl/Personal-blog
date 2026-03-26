<template>
  <div class="empty-state">
    <div class="empty-icon">
      <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="11" cy="11" r="8"/>
        <path d="m21 21-4.3-4.3"/>
        <line x1="8" y1="11" x2="14" y2="11"/>
      </svg>
    </div>
    <h3 class="empty-title">{{ title || '似乎到了知识的荒原' }}</h3>
    <p class="empty-desc">{{ message || '博主正在努力码字中，不如先看看别的内容吧。' }}</p>
    <div class="empty-actions" v-if="showAction">
      <router-link to="/" class="btn btn-primary">返回大本营</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
  message?: string
  showAction?: boolean
}>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
  background: var(--bg-card);
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  margin: 32px 0;
}
.empty-icon {
  color: var(--text-dim);
  margin-bottom: 24px;
  animation: float 6s ease-in-out infinite;
}
.empty-title {
  font-size: 1.25em;
  color: var(--text-heading);
  margin-bottom: 12px;
}
.empty-desc {
  color: var(--text-muted);
  font-size: 0.95em;
  max-width: 400px;
  margin-bottom: 24px;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
</style>
