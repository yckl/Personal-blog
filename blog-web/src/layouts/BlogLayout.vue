<template>
  <div class="blog-layout">
    <!-- I2: Skip-to-content link for keyboard users -->
    <a href="#main-content" class="skip-link">{{ $t('nav.skipToContent') }}</a>

    <!-- Header -->
    <header class="header" role="banner" v-show="!readingStore.readingMode">
      <div class="container header-inner">
        <router-link to="/" class="logo" aria-label="Home">
          <span class="logo-icon" aria-hidden="true">✍️</span>
          <span class="logo-text">{{ siteConfig.site_title || 'My Blog' }}</span>
        </router-link>
        <nav class="nav" role="navigation" aria-label="Main navigation">
          <router-link to="/" class="nav-link">{{ $t('nav.home') }}</router-link>
          <router-link to="/articles" class="nav-link">{{ $t('nav.articles') }}</router-link>
          <router-link to="/archive" class="nav-link">{{ $t('nav.archive') }}</router-link>
          <router-link to="/about" class="nav-link">{{ $t('nav.about') }}</router-link>
          <router-link to="/contact" class="nav-link">{{ $t('nav.contact') }}</router-link>
          <router-link to="/search" class="nav-link search-btn" :aria-label="$t('nav.search')">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
          </router-link>
          <LanguageSwitcher />
        </nav>
      </div>
    </header>

    <!-- Main Content -->
    <main class="main" id="main-content" role="main">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="footer" v-show="!readingStore.readingMode">
      <div class="container footer-inner">
        <p>{{ siteConfig.site_footer || $t('footer.defaultCopyright') }}</p>
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
import { getSiteConfig } from '../api'
import { useReadingStore } from '../stores/reading'
import ReadingToolbar from '../components/ReadingToolbar.vue'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'

const siteConfig = ref<any>({})
const readingStore = useReadingStore()

onMounted(async () => {
  // Init reading preferences (apply theme, font, width from localStorage)
  readingStore.init()

  try {
    const res: any = await getSiteConfig()
    siteConfig.value = res.data || {}
    if (siteConfig.value.site_title) {
      document.title = siteConfig.value.site_title
    }
  } catch (e) { console.error(e) }
})

// Toggle reading mode body class
watch(() => readingStore.readingMode, (v) => {
  document.body.classList.toggle('reading-mode', v)
})
</script>

<style scoped>
/* I2: Skip-to-content (visible only on focus for keyboard users) */
.skip-link {
  position: absolute; left: -9999px; top: auto;
  padding: 8px 16px; background: var(--primary); color: #fff;
  font-weight: 600; z-index: 9999; border-radius: 0 0 8px 0;
}
.skip-link:focus { left: 0; top: 0; }

/* I2: Focus visible for keyboard navigation */
.nav-link:focus-visible, .logo:focus-visible { outline: 2px solid var(--primary-light); outline-offset: 2px; border-radius: 4px; }

.blog-layout { min-height: 100vh; display: flex; flex-direction: column; }

.header {
  position: sticky; top: 0; z-index: 100;
  background: var(--header-bg); backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border);
}
.header-inner { display: flex; justify-content: space-between; align-items: center; height: 64px; }
.logo { display: flex; align-items: center; gap: 10px; text-decoration: none; }
.logo-icon { font-size: 24px; }
.logo-text { font-size: 20px; font-weight: 700; color: var(--text-heading); letter-spacing: -0.5px; }
.nav { display: flex; align-items: center; gap: 8px; }
.nav-link {
  padding: 6px 14px; border-radius: 8px; font-size: 14px; font-weight: 500;
  color: var(--text-muted); transition: all 0.2s;
}
.nav-link:hover, .nav-link.router-link-active { color: var(--text-heading); background: var(--bg-hover); }
.search-btn { display: flex; align-items: center; padding: 8px; }

.main { flex: 1; padding: 40px 0; }

.footer { border-top: 1px solid var(--border); padding: 32px 0; }
.footer-inner { display: flex; justify-content: space-between; align-items: center; }
.footer-inner p { color: var(--text-dim); font-size: 14px; }
.footer-links { display: flex; gap: 16px; }
.footer-links a { color: var(--text-muted); font-size: 14px; }
.footer-links a:hover { color: var(--primary-light); }
</style>
