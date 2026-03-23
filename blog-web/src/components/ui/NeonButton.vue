<template>
  <button
    :class="['neon-button', variant, size, { block, loading: isLoading }]"
    :disabled="disabled || isLoading"
    @click="$emit('click', $event)"
  >
    <span v-if="isLoading" class="neon-spinner" />
    <span class="neon-content">
      <slot />
    </span>
    <span class="neon-glow" />
  </button>
</template>

<script setup lang="ts">
defineProps<{
  variant?: 'primary' | 'ghost' | 'danger' | 'cyan'
  size?: 'sm' | 'md' | 'lg'
  block?: boolean
  disabled?: boolean
  isLoading?: boolean
}>()

defineEmits<{ click: [e: MouseEvent] }>()
</script>

<style scoped>
.neon-button {
  position: relative;
  padding: 12px 28px;
  border-radius: var(--radius-sm);
  font-family: var(--font-heading);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: -0.01em;
  cursor: pointer;
  border: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.neon-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none !important;
}

/* Sizes */
.sm { padding: 8px 18px; font-size: 13px; border-radius: var(--radius-xs); }
.lg { padding: 16px 36px; font-size: 17px; }
.block { width: 100%; }

/* Primary */
.primary {
  background: var(--gradient-1);
  color: #fff;
  box-shadow: 0 4px 20px rgba(168, 85, 247, 0.3);
}
.primary:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.03);
  box-shadow: var(--glow-purple);
}
.primary:active:not(:disabled) { transform: translateY(0) scale(0.98); }

/* Cyan */
.cyan {
  background: linear-gradient(135deg, #22d3ee 0%, #06b6d4 100%);
  color: #0f172a;
  box-shadow: 0 4px 20px rgba(34, 211, 238, 0.25);
}
.cyan:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.03);
  box-shadow: var(--glow-cyan);
}

/* Ghost */
.ghost {
  background: transparent;
  color: var(--text-muted);
  border: 1px solid var(--border);
}
.ghost:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary-light);
  background: rgba(168, 85, 247, 0.08);
  transform: translateY(-1px);
}

/* Danger */
.danger {
  background: linear-gradient(135deg, #f43f5e 0%, #e11d48 100%);
  color: #fff;
  box-shadow: 0 4px 20px rgba(244, 63, 94, 0.25);
}
.danger:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.03);
  box-shadow: 0 0 40px rgba(244, 63, 94, 0.3);
}

/* Neon glow trail */
.neon-glow {
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  opacity: 0;
  transition: opacity 0.4s;
}
.primary .neon-glow { background: var(--gradient-1); filter: blur(12px); }
.cyan .neon-glow { background: linear-gradient(135deg, #22d3ee, #06b6d4); filter: blur(12px); }
.neon-button:hover .neon-glow { opacity: 0.3; }

/* Content z-index above glow */
.neon-content { position: relative; z-index: 1; display: flex; align-items: center; gap: 8px; }

/* Loading spinner */
.neon-spinner {
  width: 16px; height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: neon-spin 0.6s linear infinite;
  position: relative; z-index: 1;
}
@keyframes neon-spin { to { transform: rotate(360deg); } }
</style>
