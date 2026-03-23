<template>
  <div class="subscribe-page fade-in">
    <div class="confirm-card glass-card">
      <!-- Loading -->
      <template v-if="loading">
        <div class="icon-wrap">
          <div class="spinner-ring" />
        </div>
        <p class="status-text">正在确认你的跃迁坐标...</p>
      </template>

      <!-- Success -->
      <template v-else-if="success">
        <div class="icon-wrap success">
          <div class="success-anim-container">
            <div class="sa-ring" />
            <div class="sa-ring" />
            <div class="sa-circle glass">
              <svg class="sa-check" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 6L9 17l-5-5"/>
              </svg>
            </div>
          </div>
        </div>
        <h1 class="gradient-text success-title">订阅通道已建立！</h1>
        <p class="success-desc">欢迎加入星际网络！最新的知识与信号将直接投递到你的终端。</p>
        <div v-if="email" class="email-display glass">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
          {{ email }}
        </div>
        <div>
          <router-link to="/" class="btn btn-primary btn-glow btn-large">← 返回主航线</router-link>
        </div>
      </template>

      <!-- Error -->
      <template v-else>
        <div class="icon-wrap error">
          <div class="error-circle">!</div>
        </div>
        <h1 class="error-title">{{ errorTitle }}</h1>
        <p class="error-desc">{{ errorMessage }}</p>
        <router-link to="/" class="btn btn-primary btn-glow btn-large">← 返回主航线</router-link>
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
const email = ref('')
const errorTitle = ref('链接失效')
const errorMessage = ref('此跃迁锚点已过期或破损。')

onMounted(async () => {
  const token = route.query.token as string
  if (!token) { loading.value = false; return }
  try {
    const res: any = await request.get('/api/subscribe/confirm', { params: { token } })
    success.value = true
    email.value = res.data?.email || ''
  } catch (e: any) {
    errorTitle.value = '坐标确认失败'
    errorMessage.value = e?.response?.data?.message || '空间扭曲导致信号中断，请稍后再试。'
  } finally { loading.value = false }
})
</script>

<style scoped>
.subscribe-page {
  min-height: 85vh; display: flex; align-items: center; justify-content: center;
  padding: 24px; position: relative; overflow: hidden;
}
.subscribe-page::before {
  content: ''; position: absolute; top: -20%; left: -10%; right: -10%; bottom: -20%;
  background: radial-gradient(circle at center, rgba(16,185,129,0.05) 0%, transparent 60%);
  z-index: -1; pointer-events: none;
}
.confirm-card {
  text-align: center; max-width: 560px; width: 100%;
  padding: 80px 48px; border-radius: 32px;
  background: rgba(15,23,42,0.4); backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.08); box-shadow: 0 30px 60px rgba(0,0,0,0.4);
}

/* Base Icon Wrap */
.icon-wrap { margin-bottom: 40px; display: flex; justify-content: center; }

/* Giant CSS Success Animation */
.success-anim-container {
  width: 140px; height: 140px; position: relative;
  display: flex; align-items: center; justify-content: center;
}
.sa-ring {
  position: absolute; inset: 0; border-radius: 50%;
  border: 4px solid rgba(16,185,129,0.15);
  animation: scaleFade 2s infinite cubic-bezier(0.2, 0.8, 0.2, 1);
}
.sa-ring:nth-child(2) { animation-delay: 1s; }
@keyframes scaleFade {
  0% { transform: scale(0.8); opacity: 1; }
  100% { transform: scale(2); opacity: 0; }
}
.sa-circle {
  position: relative; width: 130px; height: 130px; border-radius: 50%;
  background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(16,185,129,0.05));
  border: 2px solid rgba(16,185,129,0.4);
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 0 40px rgba(16,185,129,0.3), inset 0 0 20px rgba(16,185,129,0.2);
  animation: popScale 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.sa-check {
  color: #10b981; filter: drop-shadow(0 0 10px rgba(16,185,129,0.6));
  stroke-dasharray: 100; stroke-dashoffset: 100;
  animation: drawCheck 0.6s 0.4s cubic-bezier(0.4, 0, 0.2, 1) forwards;
}
@keyframes drawCheck { to { stroke-dashoffset: 0; } }
@keyframes popScale { 0% { transform: scale(0); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }

/* Error state */
.error-circle {
  width: 130px; height: 130px; border-radius: 50%;
  background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(239,68,68,0.05));
  border: 2px solid rgba(239,68,68,0.4);
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-heading); font-size: 80px; font-weight: 900; color: #ef4444;
  box-shadow: 0 0 40px rgba(239,68,68,0.3), inset 0 0 20px rgba(239,68,68,0.2);
  text-shadow: 0 0 20px rgba(239,68,68,0.6);
  animation: popScale 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}

/* Spinner */
.spinner-ring {
  width: 64px; height: 64px;
  border: 4px solid rgba(255,255,255,0.05);
  border-top-color: #22d3ee; border-radius: 50%;
  animation: spin 0.8s linear infinite; box-shadow: 0 0 20px rgba(34,211,238,0.3);
}
@keyframes spin { to { transform: rotate(360deg); } }

/* Typography */
.success-title {
  font-family: var(--font-heading); font-size: 36px; font-weight: 900;
  background: linear-gradient(135deg, #10b981, #34d399); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
  margin-bottom: 20px; letter-spacing: -0.02em; filter: drop-shadow(0 2px 4px rgba(16,185,129,0.2));
}
.success-desc { color: var(--text-muted); font-size: 18px; line-height: 1.7; margin-bottom: 32px; padding: 0 24px; }
.error-title { font-family: var(--font-heading); font-size: 36px; font-weight: 800; color: #ef4444; margin-bottom: 20px; }
.error-desc { color: var(--text-dim); font-size: 16px; margin-bottom: 40px; }

.status-text { color: var(--text-dim); font-size: 18px; font-weight: 600; letter-spacing: 0.05em; }

.email-display {
  display: inline-flex; align-items: center; gap: 10px;
  padding: 14px 28px; border-radius: 99px;
  background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.3);
  font-size: 16px; font-weight: 600; color: #34d399; margin-bottom: 40px;
  box-shadow: 0 0 20px rgba(16,185,129,0.1);
}

.btn-large {
  padding: 16px 40px; font-size: 16px; font-weight: 800;
  background: linear-gradient(90deg, #10b981, #059669); border: none; box-shadow: 0 10px 30px rgba(16,185,129,0.3);
}
.btn-large:hover { box-shadow: 0 16px 40px rgba(16,185,129,0.4); transform: translateY(-2px); }

@media (max-width: 768px) {
  .confirm-card { padding: 56px 32px; }
  .sa-circle, .error-circle { width: 100px; height: 100px; }
  .success-title, .error-title { font-size: 28px; }
  .success-desc { font-size: 16px; padding: 0; }
  .email-display { font-size: 15px; padding: 12px 24px; }
}
</style>
