<template>
  <div class="uc-orders">
    <h2 class="uc-section-title gradient-text">订单记录</h2>
    <div v-if="loading" class="uc-loading">加载中...</div>
    <div v-else-if="!orders.length" class="uc-empty">
      <p>暂无订单记录</p>
    </div>
    <div v-else class="orders-list">
      <div v-for="o in orders" :key="o.id" class="order-item">
        <div class="order-info">
          <span class="order-no">{{ o.orderNo }}</span>
          <span :class="['order-status', o.status?.toLowerCase()]">{{ statusLabel(o.status) }}</span>
        </div>
        <div class="order-detail">
          <span>¥{{ (o.amountCents / 100).toFixed(2) }}</span>
          <span class="order-time">{{ formatDate(o.createdAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'

const authStore = useAuthStore()
const orders = ref<any[]>([])
const loading = ref(false)

function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待支付', PAID: '已支付', REFUNDED: '已退款', EXPIRED: '已过期' }
  return map[s] || s
}

onMounted(async () => {
  if (!authStore.user) return
  loading.value = true
  try {
    const res: any = await request.get(`/api/member/${authStore.user.id}/orders`)
    orders.value = res.data || []
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
.uc-orders { display: flex; flex-direction: column; height: 100%; }
.uc-section-title {
  font-family: var(--font-heading); font-size: 28px; font-weight: 800;
  margin-bottom: 24px; letter-spacing: -0.02em;
}
.uc-loading, .uc-empty { text-align: center; padding: 48px; color: var(--text-dim); font-size: 16px; flex: 1; display: flex; align-items: center; justify-content: center; }
.orders-list { display: flex; flex-direction: column; gap: 12px; flex: 1; }
.order-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 24px; border-radius: 14px;
  background: var(--bg-card, rgba(15,23,42,0.4));
  border: 1px solid var(--border, rgba(255,255,255,0.06));
}
.order-info { display: flex; align-items: center; gap: 12px; }
.order-no { font-family: var(--font-mono, monospace); font-size: 14px; color: var(--text-heading); }
.order-status {
  padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 600;
}
.order-status.paid { background: rgba(16,185,129,0.12); color: #10b981; }
.order-status.pending { background: rgba(245,158,11,0.12); color: #f59e0b; }
.order-status.refunded { background: rgba(244,63,94,0.12); color: #f43f5e; }
.order-detail { font-size: 14px; color: var(--text-muted); display: flex; gap: 16px; align-items: center; }
.order-time { font-size: 13px; color: var(--text-dim); }
</style>
