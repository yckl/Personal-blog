<template>
  <div class="uc-plans">
    <h2 class="uc-section-title gradient-text">会员计划</h2>
    <div v-if="loading" class="uc-loading">加载中...</div>
    <div class="plans-grid" v-else>
      <div v-for="plan in plans" :key="plan.id" :class="['plan-card', { current: plan.id === authStore.user?.currentPlanId }]">
        <div class="plan-header">
          <h3>{{ plan.name }}</h3>
          <span class="plan-price">
            <span class="price-currency">¥</span>
            <span class="price-amount">{{ (plan.priceCents / 100).toFixed(0) }}</span>
            <span class="price-period">/{{ plan.durationMonths }}个月</span>
          </span>
        </div>
        <p class="plan-desc">{{ plan.description || '享受高级功能和独家内容' }}</p>
        <ul class="plan-features">
          <li>✓ 无限阅读所有文章</li>
          <li>✓ 会员专属内容</li>
          <li v-if="plan.tier === 'PREMIUM'">✓ 优先客服支持</li>
          <li v-if="plan.tier === 'PREMIUM'">✓ 下载原始资源</li>
        </ul>
        <!-- 当前计划 -->
        <span v-if="plan.id === authStore.user?.currentPlanId" class="current-badge">✓ 当前计划</span>
        <!-- 已包含在更高级别中 -->
        <span v-else-if="authStore.user?.currentPlanId && plan.priceCents <= currentPlanPrice" class="included-badge">已包含在当前计划中</span>
        <!-- 可升级 -->
        <button v-else
          class="btn btn-primary btn-glow plan-btn" @click="handleUpgrade(plan)">
          {{ authStore.user?.tier === 'FREE' ? '立即开通' : '升级到此计划' }}
        </button>
      </div>
    </div>

    <!-- 支付弹窗 -->
    <Teleport to="body">
      <Transition name="pay-modal">
        <div v-if="showPayment" class="pay-overlay" @click.self="showPayment = false">
          <div class="pay-dialog">
            <div class="pay-header">
              <h3>订单结算</h3>
              <button class="pay-close" @click="showPayment = false">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                  <path d="M18 6L6 18M6 6l12 12"/>
                </svg>
              </button>
            </div>
            <div class="pay-body">
              <p class="pay-product-name">{{ pendingPlan?.name }}</p>
              <div class="pay-price-display">
                <span class="pay-currency">¥</span>
                <span class="pay-amount">{{ pendingPlan ? (pendingPlan.priceCents / 100).toFixed(2) : '0.00' }}</span>
              </div>
              <div class="pay-qr">
                <img v-if="pendingPlan" :src="`https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=mock_upgrade_${pendingPlan.id}`" alt="支付二维码" />
              </div>
              <p class="pay-hint">测试环境 — 请使用任意扫码工具模拟支付</p>
            </div>
            <div class="pay-footer">
              <button class="pay-btn cancel" @click="showPayment = false">取消支付</button>
              <button class="pay-btn confirm" @click="confirmUpgrade">已完成支付</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'

const authStore = useAuthStore()
const plans = ref<any[]>([])
const loading = ref(false)
const showPayment = ref(false)
const pendingPlan = ref<any>(null)

// 当前计划的价格，用于判断哪些计划已包含
const currentPlanPrice = computed(() => {
  if (!authStore.user?.currentPlanId) return 0
  const cp = plans.value.find((p: any) => p.id === authStore.user?.currentPlanId)
  return cp ? cp.priceCents : 0
})

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await request.get('/api/membership/plans')
    plans.value = res.data || []
  } catch {} finally { loading.value = false }
})

function handleUpgrade(plan: any) {
  if (plan.priceCents === 0) { alert('您已是该计划会员'); return }
  pendingPlan.value = plan
  showPayment.value = true
}

async function confirmUpgrade() {
  showPayment.value = false
  try {
    const res: any = await request.post('/api/membership/upgrade', { planId: pendingPlan.value.id })
    const data = res.data
    if (authStore.user && data) {
      authStore.user.tier = data.tier
      authStore.user.tierExpiresAt = data.tierExpiresAt
      authStore.user.currentPlanId = data.currentPlanId
      localStorage.setItem('member_user', JSON.stringify(authStore.user))
    }
    alert('🎉 升级成功！')
  } catch { alert('升级失败，请稍后重试') }
}
</script>

<style scoped>
.uc-plans { display: flex; flex-direction: column; height: 100%; }
.uc-section-title {
  font-family: var(--font-heading); font-size: 28px; font-weight: 800;
  margin-bottom: 24px; letter-spacing: -0.02em;
}
.uc-loading { text-align: center; padding: 32px; color: var(--text-dim); }
.plans-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; flex: 1; }
.plan-card {
  padding: 28px; border-radius: 16px; position: relative;
  background: var(--bg-card, rgba(15,23,42,0.4));
  border: 1px solid var(--border, rgba(255,255,255,0.06));
  transition: all 0.3s;
}
.plan-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(168,85,247,0.15); }
.plan-card.current { border-color: var(--primary, #8b5cf6); }
.plan-header { margin-bottom: 16px; }
.plan-header h3 {
  font-family: var(--font-heading); font-size: 20px; font-weight: 700;
  color: var(--text-heading); margin-bottom: 10px;
}
.plan-price { display: flex; align-items: baseline; gap: 2px; }
.price-currency { font-size: 18px; color: var(--primary-light, #a78bfa); font-weight: 600; }
.price-amount { font-size: 36px; font-weight: 800; color: var(--text-heading); line-height: 1; }
.price-period { font-size: 14px; color: var(--text-dim); margin-left: 4px; }
.plan-desc { font-size: 14px; color: var(--text-muted, #94a3b8); margin-bottom: 16px; }
.plan-features { list-style: none; padding: 0; margin-bottom: 20px; }
.plan-features li { padding: 6px 0; font-size: 14px; color: var(--text-muted); }
.plan-btn { width: 100%; padding: 12px; border-radius: 12px; }
.current-badge {
  display: block; text-align: center; padding: 12px;
  color: var(--accent-green, #10b981); font-weight: 600; font-size: 14px;
}
.included-badge {
  display: block; text-align: center; padding: 12px;
  color: var(--text-dim, #64748b); font-weight: 500; font-size: 13px;
}
</style>

<!-- 支付弹窗样式（非 scoped，因为 Teleport 到 body） -->
<style>
.pay-overlay {
  position: fixed; inset: 0; z-index: 10000;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.55); backdrop-filter: blur(6px);
}
.pay-dialog {
  width: 380px; max-width: 92vw;
  background: var(--bg-card, #1e293b); border-radius: 20px;
  box-shadow: 0 32px 64px rgba(0,0,0,0.35), 0 0 0 1px rgba(255,255,255,0.08);
  overflow: hidden; animation: payIn 0.3s cubic-bezier(0.34,1.56,0.64,1);
}
@keyframes payIn {
  from { opacity: 0; transform: scale(0.9) translateY(20px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.pay-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 16px; border-bottom: 1px solid var(--border, rgba(255,255,255,0.06));
}
.pay-header h3 { font-size: 17px; font-weight: 700; color: var(--text-heading); margin: 0; }
.pay-close {
  width: 32px; height: 32px; border-radius: 50%; border: none;
  background: var(--bg-hover, rgba(255,255,255,0.06)); color: var(--text-dim);
  cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s;
}
.pay-close:hover { background: rgba(239,68,68,0.1); color: #ef4444; }
.pay-body { padding: 24px; text-align: center; }
.pay-product-name { font-size: 14px; color: var(--text-dim); margin-bottom: 8px; }
.pay-price-display { display: flex; align-items: baseline; justify-content: center; gap: 2px; margin-bottom: 24px; }
.pay-price-display .pay-currency { font-size: 18px; font-weight: 700; color: var(--text-heading); }
.pay-price-display .pay-amount { font-size: 36px; font-weight: 800; color: var(--text-heading); letter-spacing: -0.02em; }
.pay-qr {
  display: inline-flex; padding: 12px; background: #fff;
  border-radius: 16px; margin-bottom: 16px;
}
.pay-qr img { width: 180px; height: 180px; border-radius: 8px; }
.pay-hint { font-size: 12px; color: var(--text-dim, #9ca3af); margin: 0; }
.pay-footer { display: flex; gap: 12px; padding: 16px 24px 24px; }
.pay-btn {
  flex: 1; padding: 12px 0; border-radius: 12px; font-size: 15px;
  font-weight: 700; cursor: pointer; border: none; transition: all 0.2s;
}
.pay-btn.cancel {
  background: var(--bg-hover, rgba(255,255,255,0.06)); color: var(--text-muted);
  border: 1px solid var(--border, rgba(255,255,255,0.08));
}
.pay-btn.cancel:hover { background: rgba(255,255,255,0.1); }
.pay-btn.confirm {
  background: linear-gradient(135deg, #8b5cf6, #6366f1); color: #fff;
  box-shadow: 0 4px 16px rgba(139,92,246,0.35);
}
.pay-btn.confirm:hover { box-shadow: 0 6px 24px rgba(139,92,246,0.5); transform: translateY(-1px); }
.pay-modal-enter-active { transition: opacity 0.25s; }
.pay-modal-leave-active { transition: opacity 0.2s; }
.pay-modal-enter-from, .pay-modal-leave-to { opacity: 0; }
</style>
