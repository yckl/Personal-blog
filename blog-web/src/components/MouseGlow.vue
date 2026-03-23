<template>
  <div class="mouse-glow" :style="{ left: x + 'px', top: y + 'px' }" />
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const x = ref(-300)
const y = ref(-300)

function handleMove(e: MouseEvent) {
  x.value = e.clientX
  y.value = e.clientY
}

onMounted(() => document.addEventListener('mousemove', handleMove, { passive: true }))
onUnmounted(() => document.removeEventListener('mousemove', handleMove))
</script>
