<template>
  <div class="login-page">
    <!-- Particle Background -->
    <canvas ref="particleCanvas" class="particle-bg" />

    <!-- Glass Card -->
    <div class="login-card">
      <div class="card-glow" />
      <div class="login-header">
        <div class="brand-icon">✍️</div>
        <h1>博客管理员</h1>
        <p>{{ step === 'credentials' ? '登录以管理您的博客' : '两步验证' }}</p>
      </div>

      <!-- Step 1: Credentials -->
      <el-form v-if="step === 'credentials'" ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名或邮箱" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>

        <div class="form-extras">
          <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
        </div>

        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" native-type="submit">
            登入
          </el-button>
        </el-form-item>

        <div class="oauth-divider">
          <span>或继续使用</span>
        </div>
        <button type="button" class="oauth-btn" @click="handleGitHubLogin">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
          GitHub
        </button>
      </el-form>

      <!-- Step 2: 2FA -->
      <el-form v-else ref="totpFormRef" :model="totpForm" :rules="totpRules" @submit.prevent="handleVerify2fa">
        <div class="twofa-info">
          <div class="twofa-icon">🔐</div>
          <p>请输入验证器应用中的6位验证码</p>
        </div>
        <el-form-item prop="code">
          <el-input v-model="totpForm.code" placeholder="000000" size="large" maxlength="6" class="totp-input" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" native-type="submit">验证</el-button>
        </el-form-item>
        <el-form-item>
          <el-button size="large" class="login-btn back-btn" @click="backToCredentials">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const totpFormRef = ref()
const loading = ref(false)
const rememberMe = ref(false)
const step = ref<'credentials' | 'totp'>('credentials')
const particleCanvas = ref<HTMLCanvasElement>()

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const totpForm = reactive({ code: '' })
const totpRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码必须是 6 位数字', trigger: 'blur' }
  ]
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const needs2fa = await userStore.login(form.username, form.password)
    if (needs2fa) { step.value = 'totp'; ElMessage.info('请输入两步验证码') }
    else { ElMessage.success('登录成功'); router.push('/dashboard') }
  } catch (e: any) { ElMessage.error(e.message || '登录失败') }
  finally { loading.value = false }
}

async function handleVerify2fa() {
  await totpFormRef.value?.validate()
  loading.value = true
  try { await userStore.verify2fa(totpForm.code); ElMessage.success('验证成功'); router.push('/dashboard') }
  catch (e: any) { ElMessage.error(e.message || '验证失败') }
  finally { loading.value = false }
}

function backToCredentials() { step.value = 'credentials'; totpForm.code = '' }
function handleGitHubLogin() { ElMessage.info('GitHub OAuth 即将上线') }

// Particles
let animFrame = 0
function initParticles() {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')!
  if (!ctx) return
  const resize = () => { canvas.width = window.innerWidth; canvas.height = window.innerHeight }
  resize()
  window.addEventListener('resize', resize)

  const particles: { x: number; y: number; vx: number; vy: number; r: number; color: string; a: number }[] = []
  for (let i = 0; i < 50; i++) {
    particles.push({
      x: Math.random() * canvas.width, y: Math.random() * canvas.height,
      vx: (Math.random() - 0.5) * 0.3, vy: (Math.random() - 0.5) * 0.2,
      r: Math.random() * 1.5 + 0.5,
      color: Math.random() > 0.5 ? '#a855f7' : '#22d3ee',
      a: Math.random() * 0.4 + 0.1
    })
  }
  function draw() {
    ctx.clearRect(0, 0, canvas!.width, canvas!.height)
    for (const p of particles) {
      p.x += p.vx; p.y += p.vy
      if (p.x < 0 || p.x > canvas!.width) p.vx *= -1
      if (p.y < 0 || p.y > canvas!.height) p.vy *= -1
      ctx.beginPath(); ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.color; ctx.globalAlpha = p.a; ctx.fill()
    }
    ctx.globalAlpha = 1
    animFrame = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(initParticles)
onUnmounted(() => cancelAnimationFrame(animFrame))
</script>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: #0a0a0a; position: relative; overflow: hidden;
}
.particle-bg {
  position: absolute; inset: 0; z-index: 0;
}
.login-card {
  position: relative; z-index: 1;
  width: 420px; padding: 48px;
  background: rgba(15,23,42,0.75);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(168,85,247,0.15);

  /* Override ALL Element Plus CSS variables for dark theme */
  --el-input-bg-color: rgba(255,255,255,0.05);
  --el-input-border-color: rgba(255,255,255,0.1);
  --el-input-hover-border-color: rgba(168,85,247,0.3);
  --el-input-focus-border-color: #a855f7;
  --el-input-text-color: #f8fafc;
  --el-input-placeholder-color: #475569;
  --el-input-icon-color: #64748b;
  --el-text-color-regular: #94a3b8;
  --el-bg-color: rgba(15,23,42,0.75);
  --el-fill-color-blank: rgba(255,255,255,0.05);
  --el-border-color: rgba(255,255,255,0.1);
  --el-border-color-hover: rgba(168,85,247,0.3);
  --el-color-primary: #a855f7;
  border-radius: 28px;
  box-shadow: 0 32px 80px rgba(0,0,0,0.6);
}
.card-glow {
  position: absolute; top: -1px; left: 50%; transform: translateX(-50%);
  width: 60%; height: 2px;
  background: linear-gradient(90deg, transparent, #a855f7, #22d3ee, transparent);
}
.login-header { text-align: center; margin-bottom: 36px; }
.brand-icon { font-size: 48px; margin-bottom: 12px; }
.login-header h1 {
  font-family: 'Inter', sans-serif;
  font-size: 26px; font-weight: 800; color: #f8fafc;
  letter-spacing: -0.03em; margin-bottom: 8px;
}
.login-header p { color: #94a3b8; font-size: 14px; }

/* Form Extras */
.form-extras {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}

/* Login Button */
.login-btn {
  width: 100%; height: 48px; font-size: 15px; font-weight: 600;
  border-radius: 12px;
}
.back-btn {
  background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
  color: #94a3b8;
}
.back-btn:hover { background: rgba(255,255,255,0.1); color: #f8fafc; }

/* OAuth */
.oauth-divider {
  text-align: center; margin: 20px 0; position: relative;
  color: #475569; font-size: 13px;
}
.oauth-divider::before, .oauth-divider::after {
  content: ''; position: absolute; top: 50%; width: 35%; height: 1px;
  background: rgba(255,255,255,0.08);
}
.oauth-divider::before { left: 0; }
.oauth-divider::after { right: 0; }
.oauth-btn {
  width: 100%; padding: 12px; border-radius: 12px;
  background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1);
  color: #94a3b8; font-size: 14px; font-weight: 500;
  display: flex; align-items: center; justify-content: center; gap: 8px;
  cursor: pointer; transition: all 0.25s;
}
.oauth-btn:hover { background: rgba(255,255,255,0.1); color: #f8fafc; border-color: rgba(255,255,255,0.2); }

/* 2FA */
.twofa-info { text-align: center; margin-bottom: 24px; }
.twofa-icon { font-size: 48px; margin-bottom: 12px; }
.twofa-info p { color: #94a3b8; font-size: 14px; }
.totp-input :deep(.el-input__inner) {
  text-align: center; font-size: 28px; letter-spacing: 10px; font-weight: 700;
}

/* Override Element Plus for dark theme */
:deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.05) !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
  border-radius: 12px !important; padding: 8px 20px !important;
  box-shadow: none !important;
}
:deep(.el-input__wrapper:hover) { border-color: rgba(168,85,247,0.3) !important; }
:deep(.el-input__wrapper.is-focus) {
  border-color: #a855f7 !important;
  box-shadow: 0 0 16px rgba(168,85,247,0.15) !important;
}
:deep(.el-input__inner) { color: #f8fafc !important; caret-color: #c084fc; }
:deep(.el-input__inner::placeholder) { color: #475569 !important; }
:deep(.el-input__prefix .el-icon) { color: #64748b; }
:deep(.el-checkbox__label) { color: #94a3b8; font-size: 13px; }
:deep(.el-checkbox__inner) { background: rgba(255,255,255,0.05); border-color: rgba(255,255,255,0.15); }
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
  border: none;
}
:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #c084fc 0%, #a855f7 100%);
}
</style>
