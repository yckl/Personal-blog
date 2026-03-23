<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1>✍️ Blog Admin</h1>
        <p>{{ step === 'credentials' ? 'Sign in to manage your blog' : 'Two-factor authentication' }}</p>
      </div>

      <!-- Step 1: Username & Password -->
      <el-form
        v-if="step === 'credentials'"
        ref="formRef"
        :model="form"
        :rules="rules"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="Username or Email" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="Password" :prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" native-type="submit">
            Sign In
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Step 2: 2FA Code -->
      <el-form
        v-else
        ref="totpFormRef"
        :model="totpForm"
        :rules="totpRules"
        @submit.prevent="handleVerify2fa"
      >
        <div class="twofa-info">
          <el-icon :size="48" color="#409eff"><Key /></el-icon>
          <p>Enter the 6-digit code from your authenticator app</p>
        </div>
        <el-form-item prop="code">
          <el-input
            v-model="totpForm.code"
            placeholder="000000"
            size="large"
            maxlength="6"
            style="text-align: center; font-size: 24px; letter-spacing: 8px;"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" native-type="submit">
            Verify
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button size="large" style="width: 100%" @click="backToCredentials">
            Back
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Key } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const totpFormRef = ref()
const loading = ref(false)
const step = ref<'credentials' | 'totp'>('credentials')

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: 'Please enter username', trigger: 'blur' }],
  password: [{ required: true, message: 'Please enter password', trigger: 'blur' }]
}

const totpForm = reactive({ code: '' })
const totpRules = {
  code: [
    { required: true, message: 'Please enter verification code', trigger: 'blur' },
    { len: 6, message: 'Code must be 6 digits', trigger: 'blur' }
  ]
}

async function handleLogin() {
  await formRef.value?.validate()
  loading.value = true
  try {
    const needs2fa = await userStore.login(form.username, form.password)
    if (needs2fa) {
      step.value = 'totp'
      ElMessage.info('Please enter your 2FA code')
    } else {
      ElMessage.success('Login successful')
      router.push('/dashboard')
    }
  } catch (e: any) {
    ElMessage.error(e.message || 'Login failed')
  } finally {
    loading.value = false
  }
}

async function handleVerify2fa() {
  await totpFormRef.value?.validate()
  loading.value = true
  try {
    await userStore.verify2fa(totpForm.code)
    ElMessage.success('Verification successful')
    router.push('/dashboard')
  } catch (e: any) {
    ElMessage.error(e.message || 'Verification failed')
  } finally {
    loading.value = false
  }
}

function backToCredentials() {
  step.value = 'credentials'
  totpForm.code = ''
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header h1 {
  font-size: 28px;
  color: #1d1e2c;
  margin-bottom: 8px;
}
.login-header p {
  color: #999;
  font-size: 14px;
}
.twofa-info {
  text-align: center;
  margin-bottom: 24px;
}
.twofa-info p {
  margin-top: 12px;
  color: #666;
  font-size: 14px;
}
</style>
