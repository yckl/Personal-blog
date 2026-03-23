<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="authStore.showLoginModal" class="modal-overlay" @click.self="authStore.closeLogin()">
        <div class="modal-card glass">
          <!-- Tab switches -->
          <div class="modal-tabs">
            <button :class="['tab', { active: tab === 'login' }]" @click="tab = 'login'">登录</button>
            <button :class="['tab', { active: tab === 'register' }]" @click="tab = 'register'">注册</button>
          </div>

          <!-- Close button -->
          <button class="close-btn" @click="authStore.closeLogin()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6 6 18M6 6l12 12"/></svg>
          </button>

          <!-- Login Form -->
          <form v-if="tab === 'login'" @submit.prevent="handleLogin" class="form">
            <div class="form-group">
              <label>邮箱</label>
              <input v-model="loginForm.email" type="email" placeholder="your@email.com" required />
            </div>
            <div class="form-group">
              <label>密码</label>
              <input v-model="loginForm.password" type="password" placeholder="输入密码" required />
            </div>
            <p v-if="loginError" class="error-msg">{{ loginError }}</p>
            <button class="submit-btn btn btn-primary btn-glow" type="submit" :disabled="loading">
              {{ loading ? '登录中...' : '登录' }}
            </button>
            <p class="switch-prompt">还没有账号？<a @click="tab = 'register'">立即注册</a></p>
          </form>

          <!-- Register Form -->
          <form v-if="tab === 'register'" @submit.prevent="handleRegister" class="form">
            <div class="form-group">
              <label>昵称 <span class="optional">（可选）</span></label>
              <input v-model="registerForm.nickname" type="text" placeholder="你的昵称" />
            </div>
            <div class="form-group">
              <label>邮箱</label>
              <input v-model="registerForm.email" type="email" placeholder="your@email.com" required />
            </div>
            <div class="form-group">
              <label>密码</label>
              <input v-model="registerForm.password" type="password" placeholder="至少 6 位" required minlength="6" />
            </div>
            <div class="form-group">
              <label>确认密码</label>
              <input v-model="registerForm.confirmPassword" type="password" placeholder="再输入一次" required />
            </div>
            <p v-if="registerError" class="error-msg">{{ registerError }}</p>
            <p v-if="registerSuccess" class="success-msg">✅ 注册成功！请登录</p>
            <button class="submit-btn btn btn-primary btn-glow" type="submit" :disabled="loading">
              {{ loading ? '注册中...' : '注册' }}
            </button>
            <p class="switch-prompt">已有账号？<a @click="tab = 'login'">立即登录</a></p>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const tab = ref<'login' | 'register'>('login')
const loading = ref(false)
const loginError = ref('')
const registerError = ref('')
const registerSuccess = ref(false)

const loginForm = ref({ email: '', password: '' })
const registerForm = ref({ nickname: '', email: '', password: '', confirmPassword: '' })

async function handleLogin() {
  loginError.value = ''
  if (!loginForm.value.email || !loginForm.value.password) {
    loginError.value = '请填写邮箱和密码'; return
  }
  loading.value = true
  try {
    await authStore.login(loginForm.value.email, loginForm.value.password)
    authStore.closeLogin()
    loginForm.value = { email: '', password: '' }
  } catch (e: any) {
    loginError.value = e.message || '登录失败，请检查邮箱和密码'
  } finally { loading.value = false }
}

async function handleRegister() {
  registerError.value = ''
  registerSuccess.value = false
  const f = registerForm.value
  if (!f.email || !f.password) {
    registerError.value = '请填写邮箱和密码'; return
  }
  if (f.password.length < 6) {
    registerError.value = '密码至少 6 位'; return
  }
  if (f.password !== f.confirmPassword) {
    registerError.value = '两次密码不一致'; return
  }
  loading.value = true
  try {
    await authStore.register(f.email, f.password, f.nickname || undefined)
    registerSuccess.value = true
    registerForm.value = { nickname: '', email: '', password: '', confirmPassword: '' }
    // Auto switch to login after 1.5s
    setTimeout(() => { tab.value = 'login'; registerSuccess.value = false }, 1500)
  } catch (e: any) {
    registerError.value = e.message || '注册失败'
  } finally { loading.value = false }
}
</script>

<style scoped>
/* Overlay */
.modal-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0,0,0,0.6); backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}

/* Card */
.modal-card {
  position: relative;
  width: 100%; max-width: 420px;
  padding: 40px 36px 36px;
  border-radius: 20px;
  animation: slide-up 0.35s ease-out;
}
@keyframes slide-up {
  from { opacity: 0; transform: translateY(24px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* Tabs */
.modal-tabs {
  display: flex; gap: 4px; margin-bottom: 28px;
  background: rgba(255,255,255,0.04); border-radius: 12px; padding: 4px;
}
.tab {
  flex: 1; padding: 10px; border-radius: 10px;
  border: none; background: transparent;
  font-family: var(--font-heading); font-size: 15px; font-weight: 600;
  color: var(--text-muted); cursor: pointer;
  transition: all 0.25s;
}
.tab.active {
  background: var(--gradient-1); color: #fff;
  box-shadow: 0 4px 16px rgba(168,85,247,0.3);
}

/* Close */
.close-btn {
  position: absolute; top: 16px; right: 16px;
  background: none; border: none; color: var(--text-dim);
  cursor: pointer; padding: 6px; border-radius: 8px;
  transition: all 0.2s;
}
.close-btn:hover { background: rgba(255,255,255,0.06); color: var(--text); }

/* Form */
.form { display: flex; flex-direction: column; gap: 18px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label {
  font-size: 13px; font-weight: 600; color: var(--text-muted);
}
.optional { font-weight: 400; color: var(--text-dim); }
.form-group input {
  padding: 12px 16px; border-radius: 12px;
  border: 1px solid var(--border);
  background: rgba(255,255,255,0.04);
  color: var(--text); font-size: 15px;
  font-family: var(--font-body);
  outline: none; transition: all 0.25s;
}
.form-group input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 20px rgba(168,85,247,0.15);
}
.form-group input::placeholder { color: var(--text-dim); }

.submit-btn {
  width: 100%; padding: 14px; font-size: 16px; font-weight: 600;
  border-radius: 12px; margin-top: 4px;
}

.error-msg {
  color: var(--accent-rose); font-size: 13px; text-align: center;
  background: rgba(244,63,94,0.08); padding: 8px; border-radius: 8px;
}
.success-msg {
  color: var(--accent-green); font-size: 13px; text-align: center;
  background: rgba(16,185,129,0.08); padding: 8px; border-radius: 8px;
}

.switch-prompt {
  text-align: center; font-size: 13px; color: var(--text-dim);
}
.switch-prompt a {
  color: var(--primary-light); cursor: pointer; font-weight: 600;
  transition: color 0.2s;
}
.switch-prompt a:hover { color: var(--accent-cyan); }

/* Transition */
.modal-enter-active, .modal-leave-active { transition: opacity 0.3s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
