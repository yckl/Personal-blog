<template>
  <div :class="['glowing-icon', color, size, { pulse: animated }]">
    <slot>
      <span class="icon-char">{{ icon }}</span>
    </slot>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  icon?: string
  color?: 'purple' | 'cyan' | 'green' | 'amber' | 'rose'
  size?: 'sm' | 'md' | 'lg' | 'xl'
  animated?: boolean
}>()
</script>

<style scoped>
.glowing-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  position: relative;
  flex-shrink: 0;
}

/* Sizes */
.sm  { width: 32px; height: 32px; font-size: 14px; }
.md  { width: 44px; height: 44px; font-size: 18px; }
.lg  { width: 56px; height: 56px; font-size: 24px; }
.xl  { width: 72px; height: 72px; font-size: 32px; }
.glowing-icon:not(.sm):not(.md):not(.lg):not(.xl) { width: 44px; height: 44px; font-size: 18px; }

/* Color variants */
.purple {
  background: rgba(168, 85, 247, 0.12);
  color: #c084fc;
  box-shadow: 0 0 20px rgba(168, 85, 247, 0.15);
}
.cyan {
  background: rgba(34, 211, 238, 0.12);
  color: #22d3ee;
  box-shadow: 0 0 20px rgba(34, 211, 238, 0.15);
}
.green {
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.15);
}
.amber {
  background: rgba(245, 158, 11, 0.12);
  color: #f59e0b;
  box-shadow: 0 0 20px rgba(245, 158, 11, 0.15);
}
.rose {
  background: rgba(244, 63, 94, 0.12);
  color: #f43f5e;
  box-shadow: 0 0 20px rgba(244, 63, 94, 0.15);
}

/* Default */
.glowing-icon:not(.purple):not(.cyan):not(.green):not(.amber):not(.rose) {
  background: rgba(168, 85, 247, 0.12);
  color: #c084fc;
  box-shadow: 0 0 20px rgba(168, 85, 247, 0.15);
}

.icon-char { font-style: normal; line-height: 1; }

/* Pulse animation */
.pulse {
  animation: icon-pulse 2.5s ease-in-out infinite;
}
@keyframes icon-pulse {
  0%, 100% { box-shadow: 0 0 20px rgba(168, 85, 247, 0.15); transform: scale(1); }
  50% { box-shadow: 0 0 32px rgba(168, 85, 247, 0.3), 0 0 48px rgba(34, 211, 238, 0.1); transform: scale(1.05); }
}
.cyan.pulse {
  animation-name: icon-pulse-cyan;
}
@keyframes icon-pulse-cyan {
  0%, 100% { box-shadow: 0 0 20px rgba(34, 211, 238, 0.15); transform: scale(1); }
  50% { box-shadow: 0 0 32px rgba(34, 211, 238, 0.3), 0 0 48px rgba(168, 85, 247, 0.1); transform: scale(1.05); }
}
</style>
