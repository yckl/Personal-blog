<template>
  <div class="uc-page">
    <div class="uc-container">
      <!-- 左侧导航栏卡片 -->
      <aside class="uc-sidebar">
        <div class="sidebar-profile">
          <div class="uc-avatar">
            {{ (authStore.user?.nickname || authStore.user?.email || '?')[0] }}
          </div>
          <h3 class="uc-name">{{ authStore.user?.nickname || '用户' }}</h3>
          <span class="uc-email">{{ authStore.user?.email }}</span>
          <span :class="['uc-tier', authStore.user?.tier?.toLowerCase()]">
            {{ authStore.user?.tier === 'PREMIUM' ? '✨ 高级会员' : '免费会员' }}
          </span>
        </div>

        <nav class="uc-nav">
          <router-link to="/member/profile" class="uc-nav-item" active-class="active">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            个人资料
          </router-link>
          <router-link to="/member/plans" class="uc-nav-item" active-class="active">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
            会员计划
          </router-link>
          <router-link to="/member/orders" class="uc-nav-item" active-class="active">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="3" width="20" height="14" rx="2"/><path d="M8 21h8M12 17v4"/></svg>
            订单记录
          </router-link>
          <router-link to="/member/assets" class="uc-nav-item" active-class="active">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 22h14a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v4"/><path d="M14 2v4a2 2 0 0 0 2 2h4"/><path d="M3 15h6v2H3z"/><path d="M3 19h6v2H3z"/></svg>
            数字资产
          </router-link>
          <div class="uc-nav-divider" />
          <button class="uc-nav-item logout" @click="handleLogout">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            退出登录
          </button>
        </nav>
      </aside>

      <!-- 右侧内容区卡片 -->
      <main class="uc-content">
        <router-view v-slot="{ Component }">
          <Transition name="uc-fade" mode="out-in">
            <component :is="Component" />
          </Transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const router = useRouter()

function handleLogout() {
  authStore.logout()
  router.push('/')
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    authStore.openLogin()
    router.push('/')
  }
})
</script>

<style scoped>
/* ===== 页面容器 ===== */
.uc-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}

/* ===== 核心等高布局：使用 Flexbox stretch ===== */
.uc-container {
  display: flex;              /* flex 容器 */
  align-items: stretch;       /* 🔑 等高核心：左右两列拉伸到相同高度 */
  gap: 32px;
  min-height: calc(100vh - 200px);  /* 🔑 防塌陷：内容少时也保持规整矩形 */
}

/* ===== 左侧导航栏 ===== */
.uc-sidebar {
  width: 280px;               /* 🔑 固定宽度，不被右侧挤压 */
  flex-shrink: 0;             /* 🔑 禁止收缩 */
  background: var(--bg-card, rgba(15,23,42,0.6));
  border-radius: 16px;        /* 统一圆角 */
  border: 1px solid var(--border, rgba(255,255,255,0.06));
  padding: 32px 24px;
  display: flex;
  flex-direction: column;
}

/* 用户头像区 */
.sidebar-profile {
  text-align: center;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border, rgba(255,255,255,0.06));
}
.uc-avatar {
  width: 72px; height: 72px; margin: 0 auto 12px;
  border-radius: 50%;
  background: var(--gradient-1, linear-gradient(135deg, #8b5cf6, #06b6d4));
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700; color: #fff;
  box-shadow: 0 0 24px rgba(168,85,247,0.3);
}
.uc-name {
  font-family: var(--font-heading); font-size: 18px; font-weight: 700;
  color: var(--text-heading, #f1f5f9); margin-bottom: 4px;
}
.uc-email {
  font-size: 13px; color: var(--text-dim, #64748b);
  display: block; margin-bottom: 10px;
}
.uc-tier {
  display: inline-block; padding: 4px 14px; border-radius: 999px;
  font-size: 12px; font-weight: 600;
}
.uc-tier.premium { background: rgba(245,158,11,0.12); color: #f59e0b; }
.uc-tier.free { background: rgba(148,163,184,0.12); color: #94a3b8; }

/* 导航菜单 */
.uc-nav {
  display: flex; flex-direction: column; gap: 4px;
  flex: 1;    /* 导航区域占满侧边栏剩余空间 */
}
.uc-nav-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; border-radius: 12px;
  border: none; background: transparent;
  color: var(--text-muted, #94a3b8); font-size: 15px;
  font-family: inherit; cursor: pointer;
  text-decoration: none;
  transition: all 0.25s;
}
.uc-nav-item:hover {
  background: rgba(168,85,247,0.06);
  color: var(--text-heading, #f1f5f9);
}
.uc-nav-item.active {
  background: rgba(168,85,247,0.1);
  color: var(--primary-light, #a78bfa);
  font-weight: 600;
}
.uc-nav-item.logout { color: var(--accent-rose, #f43f5e); }
.uc-nav-item.logout:hover { background: rgba(244,63,94,0.06); }
.uc-nav-divider {
  height: 1px; background: var(--border, rgba(255,255,255,0.06));
  margin: 8px 0;
}

/* ===== 右侧内容区 ===== */
.uc-content {
  flex: 1;                    /* 🔑 填满剩余宽度 */
  min-width: 0;               /* 防止内容溢出 */
  background: var(--bg-card, rgba(15,23,42,0.6));
  border-radius: 16px;        /* 统一圆角 */
  border: 1px solid var(--border, rgba(255,255,255,0.06));
  padding: 32px;
  display: flex;
  flex-direction: column;
}

/* 子页面自动填满内容区 */
.uc-content :deep(> *) {
  flex: 1;                    /* 🔑 子路由组件撑满白卡片 */
}

/* ===== 路由切换淡入淡出动画 ===== */
.uc-fade-enter-active { transition: opacity 0.25s ease, transform 0.25s ease; }
.uc-fade-leave-active { transition: opacity 0.15s ease, transform 0.15s ease; }
.uc-fade-enter-from { opacity: 0; transform: translateY(12px); }
.uc-fade-leave-to { opacity: 0; transform: translateY(-8px); }

/* ===== 移动端响应式 ===== */
@media (max-width: 768px) {
  .uc-container {
    flex-direction: column;
    min-height: auto;
  }
  .uc-sidebar {
    width: 100%;
    flex-shrink: 1;
  }
}
</style>
