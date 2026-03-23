<template>
  <div class="container contact-page fade-in">
    <div class="page-hero">
      <h1>📮 Get in Touch</h1>
      <p>Have a question, suggestion, or just want to say hello? I'd love to hear from you.</p>
    </div>

    <div class="contact-grid">
      <!-- Contact Form -->
      <div class="contact-form-card">
        <h2>Send a Message</h2>
        <form @submit.prevent="handleSubmit">
          <div class="form-group">
            <label>Name *</label>
            <input v-model="form.name" type="text" placeholder="Your name" required />
          </div>
          <div class="form-group">
            <label>Email *</label>
            <input v-model="form.email" type="email" placeholder="your@email.com" required />
          </div>
          <div class="form-group">
            <label>Subject</label>
            <input v-model="form.subject" type="text" placeholder="What's this about?" />
          </div>
          <div class="form-group">
            <label>Message *</label>
            <textarea v-model="form.message" placeholder="Your message..." rows="5" required />
          </div>
          <button type="submit" class="btn btn-primary" :disabled="sending">
            {{ sending ? 'Sending...' : '✉️ Send Message' }}
          </button>
          <p v-if="sent" class="success-msg">✅ Message sent! I'll get back to you soon.</p>
        </form>
      </div>

      <!-- Contact Info -->
      <div class="contact-info">
        <div class="info-card" v-if="siteConfig.social_email">
          <span class="info-icon">📧</span>
          <h3>Email</h3>
          <a :href="'mailto:' + siteConfig.social_email">{{ siteConfig.social_email }}</a>
        </div>
        <div class="info-card" v-if="siteConfig.social_github">
          <span class="info-icon">💻</span>
          <h3>GitHub</h3>
          <a :href="siteConfig.social_github" target="_blank">{{ extractUsername(siteConfig.social_github) }}</a>
        </div>
        <div class="info-card" v-if="siteConfig.social_twitter">
          <span class="info-icon">🐦</span>
          <h3>Twitter / X</h3>
          <a :href="siteConfig.social_twitter" target="_blank">{{ extractUsername(siteConfig.social_twitter) }}</a>
        </div>
        <div class="info-card">
          <span class="info-icon">💡</span>
          <h3>Response Time</h3>
          <p>Usually within 24-48 hours</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getSiteConfig, subscribe } from '../api'

const siteConfig = ref<any>({})
const form = reactive({ name: '', email: '', subject: '', message: '' })
const sending = ref(false)
const sent = ref(false)

function extractUsername(url: string) {
  if (!url) return ''
  return '@' + url.replace(/\/$/, '').split('/').pop()
}

async function handleSubmit() {
  sending.value = true
  try {
    // Use subscribe endpoint as a contact mechanism
    await subscribe({ email: form.email, name: `[CONTACT] ${form.name}: ${form.subject || 'No subject'} — ${form.message}` })
    sent.value = true
    form.name = ''; form.email = ''; form.subject = ''; form.message = ''
  } catch { alert('Failed to send. Please try emailing directly.') }
  finally { sending.value = false }
}

onMounted(async () => {
  try { const r: any = await getSiteConfig(); siteConfig.value = r.data || {} }
  catch (e) { console.error(e) }
})
</script>

<style scoped>
.contact-page { max-width: 900px; margin: 0 auto; }
.page-hero { text-align: center; padding: 40px 0 32px; }
.page-hero h1 { font-size: 32px; font-weight: 800; color: var(--text-heading); margin-bottom: 8px; }
.page-hero p { color: var(--text-muted); font-size: 16px; }

.contact-grid { display: grid; grid-template-columns: 1fr 300px; gap: 24px; }

.contact-form-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 32px; }
.contact-form-card h2 { font-size: 20px; font-weight: 700; color: var(--text-heading); margin-bottom: 24px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }
.form-group input, .form-group textarea {
  width: 100%; padding: 12px 16px; border-radius: 8px;
  border: 1px solid var(--border); background: var(--bg);
  color: var(--text); font-size: 14px; outline: none; font-family: inherit;
}
.form-group input:focus, .form-group textarea:focus { border-color: var(--primary); }
.form-group textarea { resize: vertical; }

.btn { padding: 12px 24px; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-dark); transform: translateY(-1px); }
.success-msg { color: var(--accent-green); margin-top: 12px; font-size: 14px; }

.contact-info { display: flex; flex-direction: column; gap: 16px; }
.info-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 20px; }
.info-icon { font-size: 24px; display: block; margin-bottom: 8px; }
.info-card h3 { font-size: 14px; font-weight: 600; color: var(--text-heading); margin-bottom: 4px; }
.info-card a { color: var(--primary-light); font-size: 14px; }
.info-card p { color: var(--text-muted); font-size: 14px; }

@media (max-width: 768px) {
  .contact-grid { grid-template-columns: 1fr; }
}
</style>
