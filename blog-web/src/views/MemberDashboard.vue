<template>
  <div class="member-page">
    <div class="member-layout">
      <!-- Sidebar -->
      <aside class="member-sidebar glass">
        <div class="sidebar-profile">
          <div class="profile-avatar">
            <div class="avatar-ring-lg">
              {{ (authStore.user?.nickname || authStore.user?.email || '?')[0] }}
            </div>
          </div>
          <h3 class="profile-name">{{ authStore.user?.nickname || '用户' }}</h3>
          <span class="profile-email">{{ authStore.user?.email }}</span>
          <span :class="['tier-badge', authStore.user?.tier?.toLowerCase()]">
            {{ authStore.user?.tier === 'PREMIUM' ? '✨ 高级会员' : '免费会员' }}
          </span>
        </div>

        <nav class="sidebar-nav">
          <button :class="['nav-item', { active: currentTab === 'profile' }]" @click="currentTab = 'profile'">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            个人资料
          </button>
          <button :class="['nav-item', { active: currentTab === 'plans' }]" @click="currentTab = 'plans'">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            会员计划
          </button>
          <button :class="['nav-item', { active: currentTab === 'orders' }]" @click="currentTab = 'orders'">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
            订单记录
          </button>
          <button :class="['nav-item', { active: currentTab === 'assets' }]" @click="currentTab = 'assets'">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 22h14a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v4"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M3 15h6v2H3z"/><path d="M3 19h6v2H3z"/></svg>
            数字资产
          </button>
          <div class="nav-divider" />
          <button class="nav-item logout" @click="handleLogout">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            退出登录
          </button>
        </nav>
      </aside>

      <!-- Main Content -->
      <main class="member-main">
        <!-- Profile Tab -->
        <section v-if="currentTab === 'profile'" class="tab-content fade-in">
          <h2 class="section-title gradient-text">个人资料</h2>
          <div class="profile-card glass">
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
        </section>

        <!-- Plans Tab -->
        <section v-if="currentTab === 'plans'" class="tab-content fade-in">
          <h2 class="section-title gradient-text">会员计划</h2>
          <div v-if="loadingPlans" class="loading-text">加载中...</div>
          <div class="plans-grid" v-else>
            <div v-for="plan in plans" :key="plan.id" :class="['plan-card', 'glass', { current: plan.tier === authStore.user?.tier }]">
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
              <button v-if="plan.tier !== authStore.user?.tier" class="btn btn-primary btn-glow plan-btn" @click="handleUpgrade(plan)">
                升级到 {{ plan.name }}
              </button>
              <span v-else class="current-badge">当前计划</span>
            </div>
          </div>
        </section>

        <!-- Orders Tab -->
        <section v-if="currentTab === 'orders'" class="tab-content fade-in">
          <h2 class="section-title gradient-text">订单记录</h2>
          <div v-if="loadingOrders" class="loading-text">加载中...</div>
          <div v-else-if="!orders.length" class="empty-state">
            <p>暂无订单记录</p>
          </div>
          <div v-else class="orders-list">
            <div v-for="o in orders" :key="o.id" class="order-item glass">
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
        </section>

        <!-- Digital Assets Tab -->
        <section v-if="currentTab === 'assets'" class="tab-content fade-in">
          <h2 class="section-title gradient-text">我的数字资产</h2>
          <div v-if="loadingAssets" class="loading-text">加载中...</div>
          <div v-else-if="!assets.length" class="empty-state">
            <div class="empty-icon glass-icon" style="font-size: 48px; width: 100px; height: 100px; line-height: 100px; border-radius: 50%; margin: 0 auto 24px; background: rgba(15,23,42,0.4);">📦</div>
            <p>您还没有领取或购买任何数字产品</p>
            <button class="btn btn-primary" style="margin-top: 16px;" @click="$router.push('/products')">去发现好物</button>
          </div>
          <div v-else class="assets-grid">
            <div v-for="item in assets" :key="item.purchase.id" class="asset-card glass">
              <div class="asset-cover">
                <img v-if="item.product.coverImage" :src="item.product.coverImage" />
                <div v-else class="cover-placeholder"></div>
              </div>
              <div class="asset-info">
                <h3>{{ item.product.title }}</h3>
                <p>获取时间：{{ formatDate(item.purchase.createdAt) }}</p>
                <div class="asset-actions">
                  <button class="btn btn-primary btn-sm" @click="handleDownload(item.purchase.downloadToken)">
                    {{ item.product.productType === 'COURSE' ? '前往学习' : '下载文件' }}
                  </button>
                  <span class="download-count" v-if="item.purchase.maxDownloads > 0">
                    还可下载 {{ Math.max(0, item.purchase.maxDownloads - item.purchase.downloadCount) }} 次
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>

    <!-- Mock Payment Modal (outside layout for proper overlay) -->
    <Transition name="fade">
      <div v-if="showPaymentModal" class="mock-modal-overlay" @click.self="showPaymentModal = false">
        <div class="mock-modal-content glass-card">
          <h3 class="modal-title gradient-text">扫码升级会员</h3>
          <p class="modal-warn">测试环境 - 请勿真实扫码打款</p>
          <div class="qr-box">
            <img v-if="pendingPlan" :src="`https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=mock_upgrade_${pendingPlan.id}_${pendingPlan.priceCents}`" alt="QR" />
          </div>
          <p class="modal-price">需支付: <strong>¥{{ pendingPlan ? (pendingPlan.priceCents / 100).toFixed(2) : '0.00' }}</strong></p>
          <div class="modal-actions">
            <button class="btn btn-outline" @click="showPaymentModal = false">取消支付</button>
            <button class="btn btn-primary btn-glow" @click="confirmUpgrade">已完成支付</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'

const authStore = useAuthStore()
const router = useRouter()
const currentTab = ref('profile')
const plans = ref<any[]>([])
const orders = ref<any[]>([])
const assets = ref<any[]>([])
const loadingPlans = ref(false)
const loadingOrders = ref(false)
const loadingAssets = ref(false)

function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function statusLabel(s: string) {
  const map: Record<string, string> = { PENDING: '待支付', PAID: '已支付', REFUNDED: '已退款', EXPIRED: '已过期' }
  return map[s] || s
}

function handleLogout() {
  authStore.logout()
  router.push('/')
}

const showPaymentModal = ref(false)
const pendingPlan = ref<any>(null)

async function handleUpgrade(plan: any) {
  if (!authStore.isLoggedIn) {
    authStore.openLogin()
    return
  }
  
  if (plan.priceCents === 0) {
    alert('您已是该计划会员')
    return
  }
  
  pendingPlan.value = plan
  showPaymentModal.value = true
}

async function confirmUpgrade() {
  showPaymentModal.value = false
  try {
    const res: any = await request.post('/api/membership/upgrade', { planId: pendingPlan.value.id })
    const data = res.data
    // Update local auth store immediately — no need to re-login
    if (authStore.user && data) {
      authStore.user.tier = data.tier
      authStore.user.tierExpiresAt = data.tierExpiresAt
      localStorage.setItem('member_user', JSON.stringify(authStore.user))
    }
    alert('🎉 升级成功！您已成为高级会员！')
  } catch {
    alert('升级失败，请稍后重试')
  }
}

async function handleDownload(token: string) {
  if (!token) { alert('无效的下载凭证'); return }
  try {
    const res: any = await request.get(`/api/products/download/${token}`)
    if (res.data?.fileUrl) {
      window.open('http://localhost:8088' + res.data.fileUrl, '_blank')
    } else {
      alert('无法获取文件链接')
    }
  } catch (e: any) {
    alert(e.message || '下载失败')
  }
}

onMounted(async () => {
  if (!authStore.isLoggedIn) {
    authStore.openLogin()
    router.push('/')
    return
  }
  // Load plans
  loadingPlans.value = true
  try {
    const res: any = await request.get('/api/membership/plans')
    plans.value = res.data || []
  } catch {}
  finally { loadingPlans.value = false }

  // Load orders & assets
  if (authStore.user) {
    loadingOrders.value = true
    loadingAssets.value = true
    try {
      const [orderRes, assetRes]: any = await Promise.all([
        request.get(`/api/member/${authStore.user.id}/orders`),
        request.get(`/api/member/${authStore.user.id}/products`)
      ])
      orders.value = orderRes.data || []
      assets.value = assetRes.data || []
    } catch {}
    finally { 
      loadingOrders.value = false 
      loadingAssets.value = false 
    }
  }
})
</script>

<style scoped>
.member-page { max-width: 1200px; margin: 0 auto; padding: 0 24px 80px; }

.member-layout {
  display: grid; grid-template-columns: 280px 1fr; gap: 32px;
  align-items: start;
}

/* ===== Sidebar ===== */
.member-sidebar { padding: 32px 24px; border-radius: 20px; position: sticky; top: 100px; }
.sidebar-profile { text-align: center; margin-bottom: 28px; padding-bottom: 24px; border-bottom: 1px solid var(--border); }
.avatar-ring-lg {
  width: 72px; height: 72px; margin: 0 auto 12px;
  border-radius: 50%;
  background: var(--gradient-1);
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700; color: #fff;
  box-shadow: 0 0 24px rgba(168,85,247,0.3);
}
.profile-name {
  font-family: var(--font-heading); font-size: 18px; font-weight: 700;
  color: var(--text-heading); margin-bottom: 4px;
}
.profile-email { font-size: 13px; color: var(--text-dim); display: block; margin-bottom: 10px; }
.tier-badge {
  display: inline-block; padding: 4px 14px; border-radius: 999px;
  font-size: 12px; font-weight: 600;
}
.tier-badge.premium { background: rgba(245,158,11,0.12); color: #f59e0b; }
.tier-badge.free { background: rgba(148,163,184,0.12); color: #94a3b8; }

.sidebar-nav { display: flex; flex-direction: column; gap: 4px; }
.nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-radius: 12px;
  border: none; background: transparent;
  color: var(--text-muted); font-size: 15px;
  font-family: var(--font-body); cursor: pointer;
  transition: all 0.25s;
}
.nav-item:hover { background: rgba(168,85,247,0.06); color: var(--text-heading); }
.nav-item.active {
  background: rgba(168,85,247,0.1); color: var(--primary-light);
  font-weight: 600;
}
.nav-item.logout { color: var(--accent-rose); }
.nav-item.logout:hover { background: rgba(244,63,94,0.06); }
.nav-divider { height: 1px; background: var(--border); margin: 8px 0; }

/* ===== Main Content ===== */
.member-main { min-height: 400px; min-width: 0; }
.section-title {
  font-family: var(--font-heading); font-size: 28px; font-weight: 800;
  margin-bottom: 24px; letter-spacing: -0.02em;
}

/* Profile */
.profile-card { padding: 28px; border-radius: 16px; }
.profile-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 0; border-bottom: 1px solid var(--border);
}
.profile-row:last-child { border-bottom: none; }
.row-label { color: var(--text-dim); font-size: 14px; }
.row-value { color: var(--text-heading); font-weight: 500; }
.tier-text.premium { color: #f59e0b; }
.tier-text.free { color: #94a3b8; }

/* Plans */
.plans-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.plan-card {
  padding: 28px; border-radius: 16px; position: relative;
  transition: all 0.3s;
}
.plan-card:hover { transform: translateY(-4px); box-shadow: var(--glow-purple); }
.plan-card.current { border-color: var(--primary); }
.plan-header { margin-bottom: 16px; }
.plan-header h3 {
  font-family: var(--font-heading); font-size: 20px; font-weight: 700;
  color: var(--text-heading); margin-bottom: 10px;
}
.plan-price { display: flex; align-items: baseline; gap: 2px; }
.price-currency { font-size: 18px; color: var(--primary-light); font-weight: 600; }
.price-amount { font-size: 36px; font-weight: 800; color: var(--text-heading); line-height: 1; }
.price-period { font-size: 14px; color: var(--text-dim); margin-left: 4px; }
.plan-desc { font-size: 14px; color: var(--text-muted); margin-bottom: 16px; }
.plan-features { list-style: none; padding: 0; margin-bottom: 20px; }
.plan-features li { padding: 6px 0; font-size: 14px; color: var(--text-muted); }
.plan-btn { width: 100%; padding: 12px; border-radius: 12px; }
.current-badge {
  display: block; text-align: center; padding: 12px;
  color: var(--accent-green); font-weight: 600; font-size: 14px;
}

/* Orders */
.orders-list { display: flex; flex-direction: column; gap: 12px; }
.order-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 18px 24px; border-radius: 14px;
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

.empty-state { text-align: center; padding: 48px; color: var(--text-dim); font-size: 16px; }
.loading-text { text-align: center; padding: 32px; color: var(--text-dim); }

/* Assets */
.assets-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.asset-card { border-radius: 16px; overflow: hidden; display: flex; flex-direction: column; }
.asset-cover { height: 160px; background: var(--border); }
.asset-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { width: 100%; height: 100%; background: linear-gradient(135deg, rgba(168,85,247,0.1), rgba(34,211,238,0.1)); }
.asset-info { padding: 20px; flex: 1; display: flex; flex-direction: column; }
.asset-info h3 { font-size: 16px; font-weight: 700; color: var(--text-heading); margin-bottom: 8px; font-family: var(--font-heading); }
.asset-info p { font-size: 13px; color: var(--text-dim); margin-bottom: 20px; }
.asset-actions { margin-top: auto; display: flex; flex-direction: column; gap: 8px; }
.btn-sm { padding: 8px 16px; font-size: 13px; border-radius: 8px; border: none; cursor: pointer; background: var(--primary); color: #fff; transition: all 0.2s; }
.btn-sm:hover { background: var(--primary-light); transform: translateY(-1px); }
.download-count { font-size: 12px; color: var(--text-dim); text-align: center; }

/* Responsive */
@media (max-width: 768px) {
  .member-layout { grid-template-columns: 1fr; }
  .member-sidebar { position: static; }
}
</style>
