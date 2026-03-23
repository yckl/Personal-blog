<template>
  <div class="container confirm-page fade-in">
    <div class="confirm-card">
      <template v-if="loading">
        <div class="spinner" />
        <p>Confirming your subscription...</p>
      </template>
      <template v-else-if="success">
        <div class="confirm-icon">🎉</div>
        <h1>Subscription Confirmed!</h1>
        <p>Welcome aboard! You'll receive our latest articles directly in your inbox.</p>
        <router-link to="/" class="btn btn-primary">← Back to Home</router-link>
      </template>
      <template v-else>
        <div class="confirm-icon">😕</div>
        <h1>{{ errorTitle }}</h1>
        <p>{{ errorMessage }}</p>
        <router-link to="/" class="btn btn-primary">← Back to Home</router-link>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const loading = ref(true)
const success = ref(false)
const errorTitle = ref('Invalid Link')
const errorMessage = ref('This confirmation link is invalid or has expired.')

onMounted(async () => {
  const token = route.query.token as string
  if (!token) {
    loading.value = false
    return
  }
  try {
    await request.get('/api/subscribe/confirm', { params: { token } })
    success.value = true
  } catch (e: any) {
    errorTitle.value = 'Confirmation Failed'
    errorMessage.value = e?.response?.data?.message || 'Something went wrong. Please try again.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.confirm-page { min-height: 60vh; display: flex; align-items: center; justify-content: center; }
.confirm-card { text-align: center; max-width: 480px; padding: 48px; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); }
.confirm-icon { font-size: 64px; margin-bottom: 16px; }
.confirm-card h1 { font-size: 24px; font-weight: 700; color: var(--text-heading); margin-bottom: 12px; }
.confirm-card p { color: var(--text-muted); line-height: 1.6; margin-bottom: 24px; }
.spinner { width: 40px; height: 40px; border: 3px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.8s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.btn-primary { display: inline-block; padding: 12px 24px; background: var(--primary); color: #fff; border-radius: 8px; text-decoration: none; font-weight: 600; }
</style>
