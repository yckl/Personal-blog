<template>
  <router-view />
  <SpotlightSearch />
  <VinylPlayer />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { initExternalAnalytics } from './utils/analytics'
import SpotlightSearch from './components/SpotlightSearch.vue'
import VinylPlayer from './components/VinylPlayer.vue'

onMounted(() => {
  initExternalAnalytics()
})
</script>
