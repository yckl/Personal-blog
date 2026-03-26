<template>
  <div class="uc-profile">
    <h2 class="uc-section-title gradient-text">个人资料</h2>
    <div class="profile-rows">
      <div class="profile-row">
        <span class="row-label">昵称</span>
        <span class="row-value">{{ authStore.user?.nickname || '—' }}</span>
      </div>
      <div class="profile-row">
        <span class="row-label">邮箱</span>
        <span class="row-value">{{ authStore.user?.email }}</span>
      </div>
      <div class="profile-row">
        <span class="row-label">会员等级</span>
        <span :class="['row-value', 'tier-text', authStore.user?.tier?.toLowerCase()]">
          {{ authStore.user?.tier === 'PREMIUM' ? '高级会员' : '免费会员' }}
        </span>
      </div>
      <div class="profile-row" v-if="authStore.user?.tierExpiresAt">
        <span class="row-label">到期时间</span>
        <span class="row-value">{{ formatDate(authStore.user.tierExpiresAt) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '../../stores/auth'
const authStore = useAuthStore()

function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}
</script>

<style scoped>
.uc-profile { display: flex; flex-direction: column; height: 100%; }
.uc-section-title {
  font-family: var(--font-heading); font-size: 28px; font-weight: 800;
  margin-bottom: 24px; letter-spacing: -0.02em;
}
.profile-rows { flex: 1; }
.profile-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 0; border-bottom: 1px solid var(--border, rgba(255,255,255,0.06));
}
.profile-row:last-child { border-bottom: none; }
.row-label { color: var(--text-dim, #64748b); font-size: 14px; }
.row-value { color: var(--text-heading, #f1f5f9); font-weight: 500; }
.tier-text.premium { color: #f59e0b; }
.tier-text.free { color: #94a3b8; }
</style>
