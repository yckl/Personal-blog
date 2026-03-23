<template>
  <div class="container about-page fade-in">
    <div class="about-header">
      <div class="about-avatar-wrap">
        <div class="about-avatar" v-if="siteConfig.site_avatar">
          <img :src="siteConfig.site_avatar" alt="Author" />
        </div>
        <div class="about-avatar" v-else>👨‍💻</div>
      </div>
      <h1>{{ siteConfig.site_author || 'Blog Author' }}</h1>
      <p class="about-tagline">{{ siteConfig.site_description || 'A passionate developer & writer.' }}</p>
      <div class="about-social">
        <a v-if="siteConfig.social_github" :href="siteConfig.social_github" target="_blank" class="social-link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
          GitHub
        </a>
        <a v-if="siteConfig.social_twitter" :href="siteConfig.social_twitter" target="_blank" class="social-link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
          Twitter
        </a>
        <a v-if="siteConfig.social_email" :href="'mailto:'+siteConfig.social_email" class="social-link">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
          Email
        </a>
      </div>
    </div>

    <!-- Config-driven content -->
    <div class="about-content" v-if="aboutHtml" v-html="aboutHtml" />
    <div class="about-content" v-else>
      <h2>About This Blog</h2>
      <p>Welcome! This is a space where I share my thoughts on technology, programming, design, and life. I believe in building products that people love, and this blog is part of that journey.</p>
      <p>Feel free to browse around, read some articles, leave comments, or <router-link to="/search">search</router-link> for specific topics.</p>
      <p>If you enjoy my content, consider <strong>subscribing to the newsletter</strong> on the <router-link to="/">homepage</router-link> to get notified about new posts.</p>
    </div>

    <!-- Stats -->
    <div class="about-stats" v-if="stats.totalArticles">
      <div class="stat-card">
        <span class="stat-num">{{ stats.totalArticles }}</span>
        <span class="stat-label">Articles</span>
      </div>
      <div class="stat-card">
        <span class="stat-num">{{ formatNum(stats.totalViews) }}</span>
        <span class="stat-label">Total Views</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSiteConfig } from '../api'
import request from '../utils/request'

const siteConfig = ref<any>({})
const aboutHtml = ref('')
const stats = ref<any>({})

function formatNum(n: number) {
  if (!n) return '0'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return n.toString()
}

onMounted(async () => {
  try {
    const [configRes, homeRes]: any = await Promise.all([
      getSiteConfig(),
      request.get('/api/public/homepage')
    ])
    siteConfig.value = configRes.data || {}
    aboutHtml.value = siteConfig.value.about_content || ''
    stats.value = homeRes.data?.stats || {}
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.about-page { max-width: 700px; margin: 0 auto; }
.about-header { text-align: center; padding: 40px 0 32px; }
.about-avatar-wrap { margin-bottom: 16px; }
.about-avatar { width: 100px; height: 100px; border-radius: 50%; background: var(--gradient-1); display: flex; align-items: center; justify-content: center; margin: 0 auto; font-size: 48px; box-shadow: 0 4px 24px rgba(99,102,241,0.3); overflow: hidden; }
.about-avatar img { width: 100%; height: 100%; object-fit: cover; }
.about-header h1 { font-size: 28px; font-weight: 700; color: var(--text-heading); margin-bottom: 8px; }
.about-tagline { color: var(--text-muted); font-size: 16px; margin-bottom: 20px; }
.about-social { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }
.social-link { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 8px; color: var(--text-muted); font-size: 14px; transition: all .2s; }
.social-link:hover { border-color: var(--primary); color: var(--primary-light); }

.about-content { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 32px; line-height: 1.8; margin-bottom: 24px; }
.about-content h2 { font-size: 22px; color: var(--text-heading); margin-bottom: 16px; }
.about-content p { color: var(--text-muted); margin-bottom: 16px; }
.about-content a { color: var(--primary-light); }

.about-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 16px; }
.stat-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 24px; text-align: center; }
.stat-num { display: block; font-size: 28px; font-weight: 800; color: var(--primary-light); margin-bottom: 4px; }
.stat-label { font-size: 13px; color: var(--text-dim); }
</style>
