<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { initExternalAnalytics } from './utils/analytics'

onMounted(() => {
  initExternalAnalytics()
})
</script>
