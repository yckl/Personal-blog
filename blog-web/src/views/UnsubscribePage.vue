<template>
  <div class="unsub-page fade-in">
    <div class="unsub-card glass-card">
      <!-- Loading -->
      <template v-if="loading">
        <div class="icon-wrap">
          <div class="spinner-ring" />
        </div>
        <p class="status-text">正在处理你的请求...</p>
      </template>

      <!-- Confirmation Step -->
      <template v-else-if="!submitted && !success">
        <div class="icon-wrap">
          <div class="unsub-circle">📭</div>
        </div>
        <h1>取消订阅</h1>
        <p>很遗憾你要离开。在你取消之前，能告诉我们原因吗？</p>

        <div class="reason-list">
          <label v-for="reason in reasons" :key="reason.value" class="reason-item"
                 :class="{ selected: selectedReason === reason.value }">
            <input type="radio" v-model="selectedReason" :value="reason.value" />
            <span class="reason-dot" />
            <span>{{ reason.label }}</span>
          </label>
        </div>

        <button class="btn-danger-neon" @click="confirmUnsub" :disabled="processing">
          {{ processing ? '取消中...' : '确认取消订阅' }}
        </button>
        <router-link to="/" class="keep-link">算了，继续保持订阅 →</router-link>
      </template>

      <!-- Success -->
      <template v-else-if="success">
        <div class="icon-wrap">
          <div class="unsub-circle done">👋</div>
        </div>
        <h1>已取消订阅</h1>
        <p>你将不会再收到我们的邮件。如果改变主意，可以随时在首页重新订阅。</p>
        <router-link to="/" class="btn btn-primary btn-glow">← 返回首页</router-link>
      </template>

      <!-- Error -->
      <template v-else>
        <div class="icon-wrap">
          <div class="unsub-circle error">😕</div>
        </div>
        <h1>{{ errorTitle }}</h1>
        <p>{{ errorMessage }}</p>
        <router-link to="/" class="btn btn-primary btn-glow">← 返回首页</router-link>
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
const submitted = ref(false)
const success = ref(false)
const processing = ref(false)
const token = ref('')
const selectedReason = ref('too_many')
const errorTitle = ref('链接无效')
const errorMessage = ref('此取消订阅链接无效或已过期。')

const reasons = [
  { value: 'too_many', label: '邮件太多了' },
  { value: 'not_relevant', label: '内容与我无关' },
  { value: 'never_subscribed', label: '我从未订阅' },
  { value: 'other', label: '其他原因' },
]

async function confirmUnsub() {
  processing.value = true
  try {
    await request.get('/api/unsubscribe', { params: { token: token.value, reason: selectedReason.value } })
    success.value = true
    submitted.value = true
  } catch (e: any) {
    errorTitle.value = '取消失败'
    errorMessage.value = e?.response?.data?.message || '出了点问题，请稍后再试。'
    submitted.value = true
  } finally { processing.value = false }
}

onMounted(() => {
  token.value = route.query.token as string || ''
  if (!token.value) {
    loading.value = false
    submitted.value = true // show error
    return
  }
  loading.value = false
})
</script>

<style scoped>
.unsub-page {
  min-height: 85vh; display: flex; align-items: center; justify-content: center;
  padding: 24px; position: relative; overflow: hidden;
}
.unsub-page::before {
  content: ''; position: absolute; top: -20%; left: -10%; right: -10%; bottom: -20%;
  background: radial-gradient(circle at center, rgba(168,85,247,0.05) 0%, transparent 60%);
  z-index: -1; pointer-events: none;
}
.unsub-card {
  text-align: center; max-width: 560px; width: 100%;
  padding: 80px 48px; border-radius: 32px;
  background: rgba(15,23,42,0.4); backdrop-filter: blur(24px);
  border: 1px solid rgba(255,255,255,0.08); box-shadow: 0 30px 60px rgba(0,0,0,0.4);
}

/* Icon */
.icon-wrap { margin-bottom: 40px; display: flex; justify-content: center; }
.unsub-circle {
  width: 120px; height: 120px; border-radius: 50%;
  background: linear-gradient(135deg, rgba(168,85,247,0.2), rgba(168,85,247,0.05));
  border: 2px solid rgba(168,85,247,0.4);
  display: flex; align-items: center; justify-content: center;
  font-size: 56px; box-shadow: 0 0 30px rgba(168,85,247,0.3);
  animation: popIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes popIn { 0% { transform: scale(0); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
.unsub-circle.done { background: linear-gradient(135deg, rgba(16,185,129,0.2), rgba(16,185,129,0.05)); border-color: rgba(16,185,129,0.4); box-shadow: 0 0 30px rgba(16,185,129,0.3); }
.unsub-circle.error { background: linear-gradient(135deg, rgba(239,68,68,0.2), rgba(239,68,68,0.05)); border-color: rgba(239,68,68,0.4); box-shadow: 0 0 30px rgba(239,68,68,0.3); }

/* Spinner */
.spinner-ring {
  width: 64px; height: 64px;
  border: 4px solid rgba(255,255,255,0.05);
  border-top-color: #22d3ee; border-radius: 50%;
  animation: spin 0.8s linear infinite; box-shadow: 0 0 20px rgba(34,211,238,0.3);
}
@keyframes spin { to { transform: rotate(360deg); } }

.unsub-card h1 {
  font-family: var(--font-heading); font-size: 36px; font-weight: 900;
  color: #f8fafc; margin-bottom: 16px; letter-spacing: -0.02em;
}
.unsub-card p { color: var(--text-muted); font-size: 18px; line-height: 1.7; margin-bottom: 32px; padding: 0 16px; }
.status-text { color: var(--text-dim); font-size: 18px; font-weight: 600; letter-spacing: 0.05em; }

/* Glass Radio List */
.reason-list { text-align: left; margin-bottom: 48px; display: flex; flex-direction: column; gap: 12px; }
.reason-item {
  display: flex; align-items: center; gap: 16px;
  padding: 16px 20px; border-radius: 16px;
  background: rgba(15,23,42,0.3); border: 1px solid rgba(255,255,255,0.05);
  cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 16px; color: var(--text-muted); font-weight: 500;
}
.reason-item:hover { background: rgba(15,23,42,0.5); border-color: rgba(168,85,247,0.2); transform: translateX(4px); }
.reason-item.selected { 
  background: rgba(168,85,247,0.15); border-color: rgba(168,85,247,0.4);
  color: #f8fafc; box-shadow: 0 8px 24px rgba(168,85,247,0.15); transform: translateX(8px);
}
.reason-item input { display: none; }
.reason-dot {
  width: 20px; height: 20px; border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.2); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
}
.reason-item.selected .reason-dot { border-color: #a855f7; background: rgba(168,85,247,0.2); }
.reason-item.selected .reason-dot::after {
  content: ''; width: 8px; height: 8px; border-radius: 50%;
  background: #22d3ee; box-shadow: 0 0 10px #22d3ee;
}

/* Neon button */
.btn-danger-neon {
  width: 100%; padding: 18px; border-radius: 16px; font-size: 16px; font-weight: 800;
  background: transparent; color: #ef4444; border: 2px solid #ef4444;
  box-shadow: inset 0 0 10px rgba(239,68,68,0.2), 0 0 20px rgba(239,68,68,0.2);
  transition: all 0.4s; cursor: pointer; font-family: var(--font-body); letter-spacing: 0.05em; text-transform: uppercase;
}
.btn-danger-neon:hover:not(:disabled) {
  background: #ef4444; color: #fff; box-shadow: 0 0 30px #ef4444; transform: translateY(-2px);
}
.btn-danger-neon:disabled { opacity: 0.5; cursor: not-allowed; }

.keep-link { display: block; margin-top: 24px; color: var(--text-dim); font-size: 14px; font-weight: 600; transition: color 0.3s; letter-spacing: 0.05em; }
.keep-link:hover { color: #22d3ee; text-shadow: 0 0 10px rgba(34,211,238,0.5); }

@media (max-width: 768px) {
  .unsub-card { padding: 48px 24px; }
  .unsub-circle { width: 90px; height: 90px; font-size: 40px; }
  .unsub-card h1 { font-size: 28px; }
  .unsub-card p { font-size: 16px; padding: 0; }
  .reason-item.selected { transform: translateX(4px); }
  .reason-item:hover { transform: translateX(2px); }
}
</style>
