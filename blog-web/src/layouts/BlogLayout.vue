<template>
  <div class="blog-layout">
    <!-- Mouse Glow -->
    <MouseGlow />

    <!-- Reading Progress -->
    <ReadingProgress />

    <!-- Skip-to-content link for keyboard users -->
    <a href="#main-content" class="skip-link">{{ $t('nav.skipToContent') }}</a>

    <!-- Header -->
    <header class="header glass" role="banner" v-show="!readingStore.readingMode">
      <div class="container header-inner">
        <router-link to="/" class="logo" aria-label="Home">
          <img v-if="siteConfig.site_logo" :src="siteConfig.site_logo" class="site-logo-img" alt="Logo" />
          <span v-else class="logo-icon" aria-hidden="true">✍️</span>
          <span class="logo-text">{{ siteConfig.site_title || 'My Blog' }}</span>
        </router-link>
        <nav class="nav" role="navigation" aria-label="Main navigation">
          <router-link to="/" class="nav-link">{{ $t('nav.home') }}</router-link>
          <router-link to="/articles" class="nav-link">{{ $t('nav.articles') }}</router-link>
          <router-link to="/archive" class="nav-link">{{ $t('nav.archive') }}</router-link>
          <router-link to="/about" class="nav-link">{{ $t('nav.about') }}</router-link>
          <router-link to="/products" class="nav-link">数字工坊</router-link>
          <router-link to="/contact" class="nav-link">{{ $t('nav.contact') }}</router-link>
          <router-link to="/search" class="nav-link search-btn" :aria-label="$t('nav.search')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          </router-link>
          
          <button class="nav-link theme-toggle" @click="toggleTheme" aria-label="Toggle Theme">
            <svg v-if="isLight" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
          </button>

          <!-- Auth: Not logged in -->
          <template v-if="!authStore.isLoggedIn">
            <button class="auth-btn login-btn" @click="authStore.openLogin()">登录</button>
            <button class="auth-btn register-btn" @click="authStore.openLogin()">注册</button>
          </template>

          <!-- Auth: Logged in -->
          <div v-else class="user-menu">
            <div class="user-avatar" @click.stop="showUserMenu = !showUserMenu">
              {{ (authStore.user?.nickname || authStore.user?.email || '?')[0] }}
            </div>
            <Transition name="dropdown">
              <div v-if="showUserMenu" class="user-dropdown glass" @click.stop>
                <div class="dropdown-user-info">
                  <span class="dropdown-name">{{ authStore.user?.nickname || '用户' }}</span>
                  <span class="dropdown-email">{{ authStore.user?.email }}</span>
                </div>
                <div class="dropdown-divider" />
                <router-link to="/member" class="dropdown-item" @click="showUserMenu = false">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
                  个人中心
                </router-link>
                <button class="dropdown-item logout-item" @click="handleLogout">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
                  退出登录
                </button>
              </div>
            </Transition>
          </div>
        </nav>
      </div>
    </header>

    <!-- Login Modal -->
    <LoginModal />

    <!-- Main Content -->
    <main class="main" id="main-content" role="main">
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" :key="$route.path" />
        </transition>
      </router-view>
    </main>

    <!-- Footer -->
    <footer class="footer" v-show="!readingStore.readingMode">
      <div class="container footer-inner">
        <div class="footer-brand">
          <span class="footer-logo gradient-text">{{ siteConfig.site_title || 'My Blog' }}</span>
          <p>{{ siteConfig.site_footer || $t('footer.defaultCopyright') }}</p>
        </div>
        <div class="footer-links">
          <a v-if="siteConfig.social_github" :href="siteConfig.social_github" target="_blank">{{ $t('footer.github') }}</a>
          <a v-if="siteConfig.social_twitter" :href="siteConfig.social_twitter" target="_blank">{{ $t('footer.twitter') }}</a>
          <a v-if="siteConfig.social_email" :href="'mailto:' + siteConfig.social_email">{{ $t('footer.email') }}</a>
        </div>
      </div>
    </footer>

    <!-- Reading Toolbar -->
    <ReadingToolbar />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getSiteConfig } from '../api'
import { useReadingStore } from '../stores/reading'
import { useAuthStore } from '../stores/auth'
import ReadingToolbar from '../components/ReadingToolbar.vue'
import MouseGlow from '../components/MouseGlow.vue'
import ReadingProgress from '../components/ReadingProgress.vue'
import LoginModal from '../components/LoginModal.vue'

const siteConfig = ref<any>({})
const readingStore = useReadingStore()
const authStore = useAuthStore()
const router = useRouter()
const showUserMenu = ref(false)
const isLight = ref(false)

function handleLogout() {
  authStore.logout()
  showUserMenu.value = false
  router.push('/')
}

function toggleTheme() {
  isLight.value = !isLight.value
  if (isLight.value) {
    document.documentElement.setAttribute('data-theme', 'light')
    localStorage.setItem('theme', 'light')
  } else {
    document.documentElement.removeAttribute('data-theme')
    localStorage.setItem('theme', 'dark')
  }
}

onMounted(async () => {
  readingStore.init()
  authStore.init()
  try {
    const res: any = await getSiteConfig()
    siteConfig.value = res.data || {}
    if (siteConfig.value.site_title) document.title = siteConfig.value.site_title
    
    // Inject Theme Variables
    const root = document.documentElement
    if (siteConfig.value.theme_primary_color) root.style.setProperty('--primary', siteConfig.value.theme_primary_color)
    if (siteConfig.value.theme_accent_color) root.style.setProperty('--accent-cyan', siteConfig.value.theme_accent_color)
    if (siteConfig.value.theme_bg_color) {
      root.style.setProperty('--bg-default', siteConfig.value.theme_bg_color)
      document.body.style.backgroundColor = siteConfig.value.theme_bg_color
    }
    if (siteConfig.value.theme_heading_font) root.style.setProperty('--font-heading', `"${siteConfig.value.theme_heading_font}", sans-serif`)
    if (siteConfig.value.theme_body_font) root.style.setProperty('--font-body', `"${siteConfig.value.theme_body_font}", sans-serif`)

    // Inject SEO Meta Tags
    if (siteConfig.value.site_description) {
      let metaDesc = document.querySelector('meta[name="description"]')
      if (!metaDesc) { metaDesc = document.createElement('meta'); metaDesc.setAttribute('name', 'description'); document.head.appendChild(metaDesc) }
      metaDesc.setAttribute('content', siteConfig.value.site_description)
    }
    if (siteConfig.value.site_keywords) {
      let metaKw = document.querySelector('meta[name="keywords"]')
      if (!metaKw) { metaKw = document.createElement('meta'); metaKw.setAttribute('name', 'keywords'); document.head.appendChild(metaKw) }
      metaKw.setAttribute('content', siteConfig.value.site_keywords)
    }
    if (siteConfig.value.site_favicon) {
      let link: any = document.querySelector("link[rel~='icon']")
      if (!link) { link = document.createElement('link'); link.rel = 'icon'; document.head.appendChild(link) }
      link.href = siteConfig.value.site_favicon
    }
  } catch (e) { console.error(e) }

  // Restore theme
  if (localStorage.getItem('theme') === 'light') {
    isLight.value = true
    document.documentElement.setAttribute('data-theme', 'light')
  }

  // Close user dropdown on outside click
  document.addEventListener('click', () => { showUserMenu.value = false })
})

watch(() => readingStore.readingMode, (v) => {
  document.body.classList.toggle('reading-mode', v)
})
</script>

<style scoped>
/* Skip-to-content */
.skip-link {
  position: absolute; left: -9999px; top: auto;
  padding: 8px 16px; background: var(--gradient-1); color: #fff;
  font-weight: 600; z-index: 9999; border-radius: 0 0 var(--radius-xs) 0;
}
.skip-link:focus { left: 0; top: 0; }

/* Focus visible */
.nav-link:focus-visible, .logo:focus-visible {
  outline: 2px solid var(--primary-light);
  outline-offset: 2px;
  border-radius: 4px;
}

/* Layout */
.blog-layout { min-height: 100vh; display: flex; flex-direction: column; position: relative; }

/* Header */
.header {
  position: sticky; top: 0; z-index: 100;
  border-radius: 0;
  border-top: none;
  border-left: none;
  border-right: none;
}
.header-inner {
  display: flex; justify-content: space-between; align-items: center; height: 72px;
}
.logo {
  display: flex; align-items: center; gap: 12px;
  text-decoration: none;
  transition: transform 0.3s ease;
  position: relative;
}
.logo::before {
  content: ''; position: absolute; inset: -10px; border-radius: 50%;
  background: radial-gradient(circle, rgba(168,85,247,0.2) 0%, transparent 70%);
  opacity: 0; transition: opacity 0.3s; pointer-events: none;
}
.logo:hover::before { opacity: 1; }
.logo:hover { transform: scale(1.03); filter: drop-shadow(0 0 10px rgba(34,211,238,0.3)); }
.logo-icon { font-size: 26px; }
.logo-text {
  font-family: var(--font-heading);
  font-size: 22px; font-weight: 700;
  color: var(--text-heading);
  letter-spacing: -0.03em;
}
.site-logo-img {
  width: 32px; height: 32px; object-fit: contain;
  border-radius: 4px;
}

/* Nav */
.nav { display: flex; align-items: center; gap: 4px; }
.nav-link {
  padding: 8px 16px; border-radius: var(--radius-xs);
  font-family: var(--font-heading);
  font-size: 14px; font-weight: 500;
  color: var(--text-muted);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}
.nav-link::after {
  content: '';
  position: absolute;
  bottom: 2px; left: 50%; right: 50%;
  height: 2px;
  background: var(--gradient-1);
  border-radius: 1px;
  transition: all 0.3s ease;
}
.nav-link:hover::after, .nav-link.router-link-active::after {
  left: 16px; right: 16px;
}
.nav-link:hover { color: var(--text-heading); }
.nav-link.router-link-active {
  color: var(--text-heading);
  background: rgba(168,85,247,0.08);
  box-shadow: 0 4px 12px rgba(168,85,247,0.1);
}
.search-btn, .theme-toggle { display: flex; align-items: center; padding: 10px; cursor: pointer; background: transparent; border: none; }
.theme-toggle { color: var(--text-muted); transition: color 0.3s, transform 0.3s; }
.theme-toggle:hover { color: var(--accent-cyan); transform: rotate(15deg); }

/* Auth buttons */
.auth-btn {
  padding: 7px 18px; border-radius: 10px;
  font-family: var(--font-heading);
  font-size: 13px; font-weight: 600;
  cursor: pointer; transition: all 0.25s; border: none;
}
.login-btn {
  background: transparent; color: var(--text-muted);
  border: 1px solid var(--border);
}
.login-btn:hover { border-color: var(--primary); color: var(--primary-light); }
.register-btn {
  background: var(--gradient-1); color: #fff;
  box-shadow: 0 2px 12px rgba(168,85,247,0.2);
}
.register-btn:hover { box-shadow: 0 4px 20px rgba(168,85,247,0.35); transform: translateY(-1px); }

/* User avatar + dropdown */
.user-menu { position: relative; cursor: pointer; }
.user-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: var(--gradient-1);
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 700; color: #fff;
  transition: box-shadow 0.25s, transform 0.25s;
  box-shadow: 0 0 12px rgba(168,85,247,0.3);
  border: 2px solid transparent; background-clip: padding-box;
}
.user-menu:hover .user-avatar { box-shadow: 0 0 20px rgba(168,85,247,0.6), 0 0 30px rgba(34,211,238,0.4); transform: scale(1.05); }
.user-dropdown {
  position: absolute; top: calc(100% + 10px); right: 0;
  width: 220px; border-radius: 14px;
  box-shadow: 0 12px 48px rgba(0,0,0,0.4);
  overflow: hidden; z-index: 200;
}
.dropdown-user-info { padding: 14px 16px; }
.dropdown-name {
  display: block; font-weight: 600; font-size: 14px;
  color: var(--text-heading); margin-bottom: 2px;
}
.dropdown-email { font-size: 12px; color: var(--text-dim); }
.dropdown-divider { height: 1px; background: var(--border); }
.dropdown-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 16px; font-size: 14px;
  color: var(--text-muted); text-decoration: none;
  transition: all 0.15s; cursor: pointer;
  background: none; border: none; width: 100%;
  font-family: var(--font-body);
}
.dropdown-item:hover { background: rgba(168,85,247,0.06); color: var(--text-heading); }
.logout-item:hover { color: var(--accent-rose); }
.dropdown-enter-active, .dropdown-leave-active { transition: all 0.2s ease; }
.dropdown-enter-from, .dropdown-leave-to { opacity: 0; transform: translateY(-6px); }

/* Main */
.main { flex: 1; padding: 48px 0; position: relative; z-index: 1; }

/* Footer */
.footer {
  border-top: 1px solid var(--border);
  padding: 48px 0;
  background: var(--card-bg);
  backdrop-filter: blur(8px);
}
.footer-inner { display: flex; justify-content: space-between; align-items: center; }
.footer-brand { display: flex; flex-direction: column; gap: 6px; }
.footer-logo {
  font-family: var(--font-heading);
  font-size: 18px; font-weight: 700;
}
.footer-brand p { color: var(--text-dim); font-size: 13px; }
.footer-links { display: flex; gap: 20px; }
.footer-links a {
  color: var(--text-muted); font-size: 14px;
  transition: color 0.25s;
}
.footer-links a:hover { color: var(--accent-cyan); }

/* Responsive */
@media (max-width: 768px) {
  .header-inner { height: 60px; }
  .nav { gap: 0; }
  .nav-link { padding: 6px 10px; font-size: 13px; }
  .footer-inner { flex-direction: column; gap: 16px; text-align: center; }
}
@media (max-width: 480px) {
  .nav-link { padding: 6px 8px; font-size: 12px; }
  .logo-text { font-size: 18px; }
}

/* Page transitions */
.page-fade-enter-active { transition: all 0.2s ease-out; }
.page-fade-leave-active { transition: all 0.15s ease-in; }
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to { opacity: 0; transform: translateY(-4px); }
</style>
