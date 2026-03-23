<template>
  <div class="about-page fade-in">
    <!-- ===== Hero — Avatar + Info ===== -->
    <section class="about-hero">
      <div class="hero-left">
        <div class="avatar-wrap">
          <div class="avatar-glow" />
          <div class="avatar-ring">
            <img v-if="siteConfig.site_avatar" :src="siteConfig.site_avatar" alt="Author" />
            <span v-else class="avatar-fallback">👨‍💻</span>
          </div>
        </div>
        <!-- Social links below avatar -->
        <div class="social-links">
          <a v-if="siteConfig.social_github" :href="siteConfig.social_github" target="_blank" class="social-btn glass" title="GitHub">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/></svg>
          </a>
          <a v-if="siteConfig.social_twitter" :href="siteConfig.social_twitter" target="_blank" class="social-btn glass" title="Twitter">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"/></svg>
          </a>
          <a v-if="siteConfig.social_email" :href="'mailto:'+siteConfig.social_email" class="social-btn glass" title="Email">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
          </a>
        </div>
      </div>

      <div class="hero-right">
        <h1 class="hero-name gradient-text">{{ siteConfig.site_author || '博客作者' }}</h1>
        <p class="hero-tagline">{{ siteConfig.site_description || '一名充满热情的开发者与写作者。' }}</p>

        <!-- Stats Row -->
        <div class="stats-row" v-if="stats.totalArticles">
          <div class="stat glass">
            <span class="stat-num">{{ stats.totalArticles }}</span>
            <span class="stat-label">文章</span>
          </div>
          <div class="stat glass">
            <span class="stat-num">{{ formatNum(stats.totalViews) }}</span>
            <span class="stat-label">阅读</span>
          </div>
          <div class="stat glass">
            <span class="stat-num">{{ stats.totalCategories || 0 }}</span>
            <span class="stat-label">分类</span>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 4 Glass Cards ===== -->
    <section class="info-grid">
      <!-- Experience -->
      <div class="info-card glass-card">
        <div class="card-icon">💼</div>
        <h3>经历</h3>
        <div class="card-content" v-if="aboutHtml" v-html="aboutHtml" />
        <div class="card-content" v-else>
          <p>全栈开发者，拥有多年 Web 应用和系统开发经验。热衷于干净的代码、可扩展的架构和卓越的用户体验。</p>
        </div>
      </div>

      <!-- Skills (Radar Chart) -->
      <div class="info-card glass-card">
        <div class="card-icon">🎯</div>
        <h3>核心能力</h3>
        <div class="radar-wrap">
          <svg viewBox="0 0 200 200" class="radar-svg">
            <!-- Grid lines -->
            <polygon points="100,30 160,65 160,135 100,170 40,135 40,65" fill="none" stroke="var(--border)" stroke-width="1"/>
            <polygon points="100,50 143,75 143,125 100,150 57,125 57,75" fill="none" stroke="var(--border)" stroke-width="1"/>
            <polygon points="100,70 126,85 126,115 100,130 74,115 74,85" fill="none" stroke="var(--border)" stroke-width="1"/>
            <!-- Axes -->
            <line x1="100" y1="100" x2="100" y2="30" stroke="var(--border)"/>
            <line x1="100" y1="100" x2="160" y2="65" stroke="var(--border)"/>
            <line x1="100" y1="100" x2="160" y2="135" stroke="var(--border)"/>
            <line x1="100" y1="100" x2="100" y2="170" stroke="var(--border)"/>
            <line x1="100" y1="100" x2="40" y2="135" stroke="var(--border)"/>
            <line x1="100" y1="100" x2="40" y2="65" stroke="var(--border)"/>
            <!-- Data polygon -->
            <polygon points="100,45 150,70 145,120 100,160 65,115 55,60" fill="rgba(168,85,247,0.3)" stroke="#a855f7" stroke-width="2"/>
            <circle cx="100" cy="45" r="3" fill="#22d3ee" style="filter: drop-shadow(0 0 4px #22d3ee)" />
            <circle cx="150" cy="70" r="3" fill="#22d3ee" style="filter: drop-shadow(0 0 4px #22d3ee)" />
            <circle cx="145" cy="120" r="3" fill="#22d3ee" style="filter: drop-shadow(0 0 4px #22d3ee)" />
            <circle cx="100" cy="160" r="3" fill="#22d3ee" style="filter: drop-shadow(0 0 4px #22d3ee)" />
            <circle cx="65" cy="115" r="3" fill="#22d3ee" style="filter: drop-shadow(0 0 4px #22d3ee)" />
            <circle cx="55" cy="60" r="3" fill="#22d3ee" style="filter: drop-shadow(0 0 4px #22d3ee)" />
            <!-- Labels -->
            <text x="100" y="20" fill="var(--text-primary)" font-size="11" font-weight="600" text-anchor="middle" font-family="var(--font-heading)">前端架构</text>
            <text x="180" y="65" fill="var(--text-primary)" font-size="11" font-weight="600" text-anchor="middle" font-family="var(--font-heading)">后端设计</text>
            <text x="180" y="145" fill="var(--text-primary)" font-size="11" font-weight="600" text-anchor="middle" font-family="var(--font-heading)">数据库</text>
            <text x="100" y="190" fill="var(--text-primary)" font-size="11" font-weight="600" text-anchor="middle" font-family="var(--font-heading)">UI设计</text>
            <text x="20" y="145" fill="var(--text-primary)" font-size="11" font-weight="600" text-anchor="middle" font-family="var(--font-heading)">DevOps</text>
            <text x="20" y="65" fill="var(--text-primary)" font-size="11" font-weight="600" text-anchor="middle" font-family="var(--font-heading)">系统架构</text>
          </svg>
        </div>
      </div>

      <!-- Values -->
      <div class="info-card glass-card">
        <div class="card-icon">💡</div>
        <h3>理念</h3>
        <div class="card-content">
          <ul class="values-list">
            <li><strong>质量驱动：</strong> 质量永远优于数量</li>
            <li><strong>持续进化：</strong> 持续学习与个人技术栈迭代</li>
            <li><strong>用户之上：</strong> 打造卓越体验的产品</li>
            <li><strong>开源精神：</strong> 与社区分享知识与工具</li>
          </ul>
        </div>
      </div>

      <!-- Contact -->
      <div class="info-card glass-card">
        <div class="card-icon">📬</div>
        <h3>联系我</h3>
        <div class="card-content">
          <p>欢迎技术交流、项目合作或随便聊聊！我通常会在 24 小时内回复。</p>
          <router-link to="/contact" class="btn btn-primary contact-btn">发送消息 →</router-link>
        </div>
      </div>
    </section>

    <!-- ===== Timeline ===== -->
    <section class="timeline-section">
      <h2 class="section-title"><span class="title-icon">⏳</span> 旅程时间线</h2>
      <div class="timeline">
        <div v-for="(event, idx) in timeline" :key="idx" class="timeline-item fade-in">
          <div class="tl-dot" />
          <div class="tl-card glass-card">
            <span class="tl-year">{{ event.year }}</span>
            <h4>{{ event.title }}</h4>
            <p>{{ event.description }}</p>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSiteConfig } from '../api'
import request from '../utils/request'

const siteConfig = ref<any>({})
const aboutHtml = ref('')
const stats = ref<any>({})

const timeline = ref<any[]>([])

function formatNum(n: number) {
  if (!n) return '0'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'K'
  return n.toString()
}

onMounted(async () => {
  try {
    const [configRes, homeRes, tlRes]: any = await Promise.all([
      getSiteConfig(),
      request.get('/api/public/homepage'),
      request.get('/api/public/timeline')
    ])
    siteConfig.value = configRes.data || {}
    aboutHtml.value = siteConfig.value.about_content || ''
    stats.value = homeRes.data?.stats || {}
    timeline.value = tlRes.data || []
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.about-page { max-width: 1100px; margin: 0 auto; padding: 0 24px 80px; }

/* ===== Hero — Two Column ===== */
.about-hero {
  display: flex; gap: 64px; align-items: center;
  padding: 80px 0;
}
.hero-left { display: flex; flex-direction: column; align-items: center; gap: 32px; flex-shrink: 0; }

/* Avatar with glow */
.avatar-wrap { position: relative; width: 260px; height: 260px; }
.avatar-glow {
  position: absolute; inset: -16px;
  border-radius: 50%;
  background: conic-gradient(from 0deg, #a855f7, #22d3ee, #a855f7);
  animation: glowSpin 5s linear infinite;
  opacity: 0.5;
  filter: blur(24px);
}
@keyframes glowSpin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
.avatar-ring {
  position: relative; width: 260px; height: 260px;
  border-radius: 50%; overflow: hidden;
  border: 4px solid var(--card-bg);
  box-shadow: inset 0 0 40px rgba(0,0,0,0.1), var(--shadow);
  background: var(--card-bg); backdrop-filter: blur(16px);
}
.avatar-ring img { width: 100%; height: 100%; object-fit: cover; }
.avatar-fallback {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 100%; font-size: 100px;
  background: var(--gradient-1);
}

.social-links { display: flex; gap: 16px; }
.social-btn {
  width: 52px; height: 52px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--text-muted); transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--border); box-shadow: 0 4px 16px rgba(0,0,0,0.1);
}
.social-btn:hover {
  color: var(--text-primary);
  background: rgba(168,85,247,0.15);
  box-shadow: 0 10px 24px rgba(168,85,247,0.2);
  transform: translateY(-4px); border-color: rgba(168,85,247,0.3);
}

.hero-right { flex: 1; }
.hero-name {
  font-family: var(--font-heading);
  font-size: 56px; font-weight: 900;
  margin-bottom: 16px; letter-spacing: -0.02em;
  background: linear-gradient(135deg, var(--text-primary), var(--text-muted) 40%, var(--primary));
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.hero-tagline { font-size: 20px; color: var(--text-dim); line-height: 1.7; margin-bottom: 36px; max-width: 560px; }

.stats-row { display: flex; gap: 20px; }
.stat {
  padding: 20px 24px; text-align: center; border-radius: 20px;
  min-width: 110px; flex: 1;
  background: var(--card-bg); backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  box-shadow: var(--shadow); transition: all 0.4s;
}
.stat:hover { transform: translateY(-4px); border-color: rgba(168,85,247,0.3); background: var(--bg-elevated); }
.stat-num {
  display: block; font-family: var(--font-heading);
  font-size: 32px; font-weight: 900; margin-bottom: 4px;
  background: linear-gradient(90deg, #a855f7, #22d3ee); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
}
.stat-label { font-size: 13px; color: var(--text-dim); text-transform: uppercase; letter-spacing: 0.1em; font-weight: 600; }

/* ===== 4 Info Cards ===== */
.info-grid {
  display: grid; grid-template-columns: 1fr 1fr;
  gap: 32px; margin-bottom: 80px;
}
.info-card {
  padding: 40px; border-radius: 24px;
  background: var(--card-bg); backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.info-card:hover {
  transform: translateY(-8px);
  border-color: rgba(168,85,247,0.3); background: var(--bg-elevated);
  box-shadow: 0 20px 50px rgba(0,0,0,0.15), 0 0 30px rgba(168,85,247,0.15);
}
.card-icon { font-size: 40px; margin-bottom: 24px; }
.info-card h3 {
  font-family: var(--font-heading); font-size: 24px; font-weight: 800;
  color: var(--text-primary); margin-bottom: 20px; letter-spacing: -0.01em;
}
.card-content { color: var(--text-dim); font-size: 16px; line-height: 1.8; flex: 1; }
.card-content p { margin-bottom: 16px; }
.card-content a { color: var(--primary-light); font-weight: 600; }

/* Radar Chart */
.radar-wrap { width: 100%; aspect-ratio: 1; position: relative; padding: 20px; opacity: 0.9; transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1); }
.info-card:hover .radar-wrap { opacity: 1; transform: scale(1.05); }
.radar-svg { width: 100%; height: 100%; overflow: visible; }

/* Values */
.values-list { list-style: none; padding: 0; }
.values-list li {
  position: relative; padding-left: 24px; margin-bottom: 12px;
  color: var(--text-dim); font-size: 16px;
}
.values-list li strong { color: var(--text-primary); }
.values-list li::before {
  content: '';
  position: absolute; left: 0; top: 10px;
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--gradient-1); box-shadow: 0 0 10px rgba(168,85,247,0.6);
}

/* Contact */
.contact-btn { margin-top: 16px; padding: 14px 32px; font-size: 16px; font-weight: 700; border-radius: 99px; box-shadow: 0 8px 20px rgba(168,85,247,0.3); }

/* ===== Timeline ===== */
.timeline-section { margin-bottom: 80px; }
.section-title {
  font-family: var(--font-heading); font-size: 36px; font-weight: 800;
  color: var(--text-primary); margin-bottom: 48px;
  display: flex; align-items: center; justify-content: center; gap: 16px; letter-spacing: -0.02em;
}
.title-icon { font-size: 32px; filter: drop-shadow(0 0 10px rgba(34,211,238,0.5)); }
.timeline {
  margin-left: max(50% - 360px, 20px); max-width: 720px; padding-left: 40px;
  border-left: 2px solid var(--border); position: relative;
}
.timeline::before {
  content: ''; position: absolute; left: -2px; top: 0; bottom: 0; width: 2px;
  background: linear-gradient(180deg, var(--primary), #22d3ee, transparent);
}
.timeline-item { margin-bottom: 40px; position: relative; }
.tl-dot {
  position: absolute; left: -49px; top: 32px;
  width: 16px; height: 16px; border-radius: 50%;
  background: var(--bg); border: 4px solid #22d3ee;
  box-shadow: 0 0 16px rgba(34,211,238,0.6); z-index: 2;
}
.tl-card {
  padding: 32px; border-radius: 20px;
  background: var(--card-bg); backdrop-filter: blur(12px);
  border: 1px solid var(--border); box-shadow: 0 10px 30px rgba(0,0,0,0.1);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.tl-card:hover { transform: translateX(8px); border-color: rgba(34,211,238,0.3); background: var(--bg-elevated); box-shadow: 0 16px 40px rgba(0,0,0,0.15), 0 0 24px rgba(34,211,238,0.15); }
.tl-year {
  display: inline-block; padding: 4px 12px; border-radius: 8px; font-family: var(--font-heading);
  font-size: 14px; font-weight: 800; background: rgba(34,211,238,0.15); color: #22d3ee;
  margin-bottom: 12px; letter-spacing: 0.1em;
}
.tl-card h4 { font-family: var(--font-heading); font-size: 24px; font-weight: 800; color: var(--text-primary); margin-bottom: 12px; }
.tl-card p { font-size: 16px; color: var(--text-dim); line-height: 1.8; }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .about-hero { flex-direction: column; text-align: center; gap: 40px; padding: 48px 0; }
  .hero-right { text-align: center; }
  .stats-row { justify-content: center; flex-wrap: wrap; }
  .avatar-wrap { width: 200px; height: 200px; }
  .avatar-ring { width: 200px; height: 200px; }
  .hero-name { font-size: 40px; }
  .info-grid { grid-template-columns: 1fr; gap: 24px; }
  .timeline { margin-left: 20px; padding-left: 32px; }
  .tl-dot { left: -41px; }
}
</style>
