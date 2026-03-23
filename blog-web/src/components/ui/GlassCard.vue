<template>
  <div class="glass-card-component transition-all">
    <slot />
  </div>
</template>

<style scoped>
.glass-card-component {
  background: var(--card-bg, rgba(15, 23, 42, 0.85));
  box-shadow: var(--card-shadow, 0 25px 50px -12px rgba(168, 85, 247, 0.25));
  border: 1px solid var(--border, rgba(165, 85, 247, 0.2));
  border-radius: var(--radius, 24px);
  backdrop-filter: blur(var(--glass-blur, 24px));
  -webkit-backdrop-filter: blur(var(--glass-blur, 24px));
  color: var(--text-primary, #f8fafc);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.glass-card-component:hover {
  border-color: var(--border-hover, rgba(168, 85, 247, 0.45));
  box-shadow: var(--glow-purple, 0 0 40px rgba(168, 85, 247, 0.3));
  transform: translateY(-4px) scale(1.01);
}
</style>
