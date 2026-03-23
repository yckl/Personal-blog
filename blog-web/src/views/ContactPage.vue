<template>
  <div class="contact-page fade-in">
    <div class="page-header">
      <h1 class="page-title gradient-text">联系我</h1>
      <p class="page-desc">有问题、建议，或只是想打个招呼？</p>
    </div>

    <div class="contact-layout">
      <!-- ===== Left: Social 3D Cards ===== -->
      <div class="social-cards">
        <a v-if="siteConfig.social_email" :href="'mailto:' + siteConfig.social_email"
           class="social-card glass-card" @click.prevent="copyEmail">
          <div class="card-icon email-icon">📧</div>
          <h3>邮箱</h3>
          <p class="card-value">{{ siteConfig.social_email }}</p>
          <span class="card-action">点击复制</span>
        </a>

        <a v-if="siteConfig.social_github" :href="siteConfig.social_github" target="_blank"
           class="social-card glass-card">
          <div class="card-icon github-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
          </div>
          <h3>GitHub</h3>
          <p class="card-value">{{ extractUsername(siteConfig.social_github) }}</p>
          <span class="card-action">访问主页 →</span>
        </a>

        <a v-if="siteConfig.social_twitter" :href="siteConfig.social_twitter" target="_blank"
           class="social-card glass-card">
          <div class="card-icon twitter-icon">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
          </div>
          <h3>Twitter / X</h3>
          <p class="card-value">{{ extractUsername(siteConfig.social_twitter) }}</p>
          <span class="card-action">关注 →</span>
        </a>

        <div class="social-card glass-card response-card">
          <div class="card-icon">⚡</div>
          <h3>响应时间</h3>
          <p class="card-value">通常 24-48 小时内回复</p>
        </div>
      </div>

      <!-- ===== Right: Contact Form ===== -->
      <div class="form-section">
        <div class="form-card glass-card">
          <h2 class="form-title">发送消息</h2>
          <form @submit.prevent="handleSubmit">
            <div class="form-group">
              <label>姓名</label>
              <input v-model="form.name" type="text" placeholder="你的名字" required />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input v-model="form.email" type="email" placeholder="your@email.com" required />
            </div>
            <div class="form-group">
              <label>消息</label>
              <textarea v-model="form.message" placeholder="想说什么？" rows="5" required />
            </div>
            <button type="submit" class="btn btn-primary btn-glow submit-btn" :disabled="sending">
              {{ sending ? '发送中...' : '发送消息 →' }}
            </button>
            <p v-if="sent" class="success-msg">✅ 消息已发送！我会尽快回复你。</p>
          </form>
        </div>
      </div>
    </div>

    <!-- Toast -->
    <Transition name="toast">
      <div v-if="showToast" class="toast glass">{{ toastMsg }}</div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSiteConfig } from '../api'
import request from '../utils/request'

const siteConfig = ref<any>({})
const form = reactive({ name: '', email: '', message: '' })
const sending = ref(false)
const sent = ref(false)
const showToast = ref(false)
const toastMsg = ref('')

function extractUsername(url: string) {
  if (!url) return ''
  return '@' + url.replace(/\/$/, '').split('/').pop()
}

function toast(msg: string) {
  toastMsg.value = msg
  showToast.value = true
  setTimeout(() => { showToast.value = false }, 2500)
}

function copyEmail() {
  if (siteConfig.value.social_email) {
    navigator.clipboard.writeText(siteConfig.value.social_email)
    toast('📋 邮箱已复制！')
  }
}

async function handleSubmit() {
  sending.value = true
  try {
    await request.post('/api/public/messages', { email: form.email, name: form.name, message: form.message })
    sent.value = true
    form.name = ''; form.email = ''; form.message = ''
  } catch { alert('发送失败，请尝试直接发邮件。') }
  finally { sending.value = false }
}

onMounted(async () => {
  try { const r: any = await getSiteConfig(); siteConfig.value = r.data || {} }
  catch (e) { console.error(e) }
})
</script>

<style scoped>
.contact-page { max-width: 1100px; margin: 0 auto; padding: 0 24px 80px; }

/* Header */
.page-header { text-align: center; padding: 40px 0 48px; }
.page-title { font-family: var(--font-heading); font-size: 48px; font-weight: 900; margin-bottom: 8px; letter-spacing: -0.03em; }
.page-desc { font-size: 17px; color: var(--text-muted); }

/* ===== Layout ===== */
.contact-layout { display: flex; gap: 48px; align-items: stretch; justify-content: center; max-width: 1000px; margin: 0 auto; }

/* ===== Left: Social 3D Cards ===== */
.social-cards { display: flex; flex-direction: column; gap: 20px; width: 320px; flex-shrink: 0; perspective: 1000px; }
.social-card {
  flex: 1; display: flex; flex-direction: column; justify-content: center;
  padding: 32px; text-decoration: none; color: inherit; border-radius: 24px;
  background: var(--card-bg); backdrop-filter: blur(16px);
  border: 1px solid var(--border); box-shadow: var(--card-shadow);
  transition: all 0.5s cubic-bezier(0.3, 0.8, 0.2, 1);
  cursor: pointer; position: relative; overflow: hidden;
}
.social-card::before { content: ''; position: absolute; inset: 0; background: linear-gradient(135deg, rgba(255,255,255,0.05), transparent); opacity: 0; transition: opacity 0.5s; }
.social-card:hover {
  transform: rotateX(8deg) rotateY(-12deg) translateY(-8px) scale(1.02);
  border-color: rgba(168,85,247,0.4); background: var(--bg-elevated);
  box-shadow: 20px 30px 50px rgba(0,0,0,0.15), -10px -10px 30px rgba(168,85,247,0.1);
}
.social-card:hover::before { opacity: 1; }
.card-icon { font-size: 40px; margin-bottom: 16px; color: var(--text-muted); transition: transform 0.4s; }
.social-card:hover .card-icon { transform: scale(1.1) rotate(-5deg); filter: drop-shadow(0 0 10px currentColor); }
.email-icon { color: #22d3ee; }
.github-icon { color: #f8fafc; }
.twitter-icon { color: #f8fafc; }
.social-card h3 { font-family: var(--font-heading); font-size: 20px; font-weight: 800; color: var(--text-primary); margin-bottom: 8px; letter-spacing: -0.01em; }
.card-value { font-size: 15px; color: var(--text-dim); margin-bottom: 12px; font-weight: 500; }
.card-action { font-size: 13px; color: #a855f7; font-weight: 700; opacity: 0; transition: all 0.3s; transform: translateX(-8px); letter-spacing: 0.05em; text-transform: uppercase; }
.social-card:hover .card-action { opacity: 1; transform: translateX(0); }

.response-card { cursor: default; }
.response-card:hover { transform: translateY(-4px); border-color: rgba(34,211,238,0.3); box-shadow: 0 20px 40px rgba(0,0,0,0.3), 0 0 20px rgba(34,211,238,0.1); }

/* ===== Right: Contact Form ===== */
.form-section { flex: 1; max-width: 600px; display: flex; flex-direction: column; }
.form-card {
  flex: 1; display: flex; flex-direction: column; justify-content: center;
  padding: 48px; border-radius: 32px;
  background: var(--card-bg); backdrop-filter: blur(24px);
  border: 1px solid var(--border); box-shadow: var(--card-shadow);
}
.form-title { font-family: var(--font-heading); font-size: 32px; font-weight: 800; color: var(--text-primary); margin-bottom: 36px; letter-spacing: -0.02em; }
.form-group { margin-bottom: 24px; }
.form-group label { display: block; font-size: 13px; font-weight: 700; color: var(--text-dim); margin-bottom: 10px; text-transform: uppercase; letter-spacing: 0.08em; }
.form-group input, .form-group textarea {
  width: 100%; padding: 16px 20px; border-radius: 16px;
  border: 1px solid var(--border); background: var(--bg-hover); color: var(--text-primary);
  font-size: 16px; outline: none; font-family: var(--font-body); font-weight: 500; transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.form-group input:focus, .form-group textarea:focus { border-color: rgba(34,211,238,0.5); background: var(--bg-elevated); box-shadow: 0 0 24px rgba(34,211,238,0.15); transform: translateY(-2px); }
.form-group textarea { resize: vertical; min-height: 140px; }
.submit-btn { width: 100%; padding: 18px; font-size: 16px; font-weight: 800; border-radius: 16px; letter-spacing: 0.05em; text-transform: uppercase; box-shadow: 0 10px 30px rgba(168,85,247,0.3); }
.submit-btn:hover { transform: translateY(-2px); box-shadow: 0 16px 40px rgba(168,85,247,0.4); }
.success-msg { color: #10b981; margin-top: 20px; text-align: center; font-size: 15px; font-weight: 600; padding: 12px; border-radius: 12px; background: rgba(16,185,129,0.15); border: 1px solid rgba(16,185,129,0.3); }

/* ===== Toast ===== */
.toast {
  position: fixed; bottom: 32px; left: 50%;
  transform: translateX(-50%);
  padding: 14px 28px; border-radius: 999px;
  font-size: 14px; font-weight: 600;
  color: #f8fafc; background: rgba(15,23,42,0.8); backdrop-filter: blur(12px); border: 1px solid rgba(255,255,255,0.1);
  z-index: 9999;
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
}
.toast-enter-active, .toast-leave-active { transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateX(-50%) translateY(16px); }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .contact-layout { flex-direction: column-reverse; }
  .social-cards { width: 100%; flex-direction: row; flex-wrap: wrap; }
  .social-card { flex: 1; min-width: 140px; padding: 20px; }
  .page-title { font-size: 36px; }
}
</style>
