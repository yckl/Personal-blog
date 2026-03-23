<template>
  <div class="home-page">
    <!-- ===== Hero: 60vh + Particle Canvas ===== -->
    <section class="hero">
      <div class="hero-bg-overlay" />
      <canvas ref="particleCanvas" class="hero-particles" />
      <div class="hero-content fade-in">
        <h1 class="hero-title gradient-text">
          {{ hero.title || 'Bob 的思考空间' }}
        </h1>
        <p class="hero-desc">
          {{ hero.subtitle || '关注技术、产品、设计与长期主义，记录构建有价值事物的旅程。' }}
        </p>
        <div class="hero-actions">
          <router-link :to="hero.ctaLink || '/articles'" class="neon-btn btn-large">
            开始阅读
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M5 12h14M12 5l7 7-7 7"/></svg>
          </router-link>
        </div>
        <div class="scroll-down-hint">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="m6 9 6 6 6-6"/></svg>
        </div>
      </div>
    </section>

    <div class="main-content">
      <!-- ===== Featured Posts (编辑推荐) ===== -->
      <section class="section" v-if="featuredArticles.length">
        <div class="section-header">
          <h2 class="section-title"><span class="title-icon">⭐</span> 编辑推荐</h2>
        </div>
        <div class="featured-grid">
          <router-link v-for="article in featuredArticles" :key="article.id"
                       :to="`/article/${article.slug}`" class="featured-card-link">
            <GlassCard class="featured-card fade-in">
              <div class="featured-cover" :style="{ backgroundImage: article.coverImage ? `url(${article.coverImage})` : 'var(--gradient-1)' }">
                <span v-if="article.categoryName" class="card-badge">{{ article.categoryName }}</span>
              </div>
              <div class="featured-body">
                <h3 class="card-feature-title">{{ article.title }}</h3>
                <p class="card-feature-excerpt">{{ article.excerpt || '点击阅读全文...' }}</p>
                <div class="card-meta">
                  <span>{{ formatDate(article.publishedAt) }}</span>
                  <span class="meta-dot">·</span>
                  <span>{{ article.viewCount }} 阅读</span>
                </div>
              </div>
            </GlassCard>
          </router-link>
        </div>
      </section>

      <!-- ===== Latest Articles (最新文章 - Masonry) ===== -->
      <section class="section" v-if="latestArticles.length">
        <div class="section-header">
          <h2 class="section-title"><span class="title-icon">📝</span> 最新文章</h2>
          <router-link to="/articles" class="neon-text-btn">查看更多 →</router-link>
        </div>
        <div class="masonry-grid">
          <router-link v-for="article in latestArticles" :key="article.id"
                       :to="`/article/${article.slug}`" class="masonry-card-link">
            <GlassCard class="masonry-card fade-in">
              <div v-if="article.coverImage" class="masonry-cover"
                   :style="{ backgroundImage: `url(${article.coverImage})` }" />
              <div class="masonry-body">
                <div class="post-tags">
                  <span v-for="tag in (article.tags || []).slice(0,3)" :key="tag.id" class="neon-label"
                        :style="{ '--tag-color': tag.color || '#a855f7' }">{{ tag.name }}</span>
                </div>
                <h3 class="masonry-title">{{ article.title }}</h3>
                <p class="masonry-excerpt">{{ article.excerpt || '点击阅读全文...' }}</p>
                <div class="card-meta">
                  <span>{{ formatDate(article.publishedAt) }}</span>
                  <span class="meta-dot">·</span>
                  <span>{{ article.wordCount || 0 }} 字</span>
                </div>
              </div>
            </GlassCard>
          </router-link>
        </div>
      </section>

      <!-- ===== Hot Articles (热门文章 - 优雅编号列表) ===== -->
      <section class="section" v-if="popularArticles.length">
        <div class="section-header">
          <h2 class="section-title"><span class="title-icon">🔥</span> 热门文章</h2>
        </div>
        <div class="hot-list">
          <router-link v-for="(article, idx) in popularArticles" :key="article.id"
                       :to="`/article/${article.slug}`" class="hot-list-item fade-in">
            <div class="hot-rank-box" :class="'rank-' + (idx + 1)">
              <span class="hot-rank-num">{{ idx + 1 }}</span>
              <div class="rank-glow"></div>
            </div>
            <div class="hot-info">
              <h3 class="hot-title">{{ article.title }}</h3>
              <p class="hot-meta">
                <span>{{ article.viewCount }} 次阅读</span> • 
                <span>{{ article.likeCount || 0 }} 人喜欢</span>
              </p>
            </div>
            <div class="hot-arrow">→</div>
          </router-link>
        </div>
      </section>

      <!-- ===== Series (系列集 - 横向滑动卡片) ===== -->
      <section class="section" v-if="seriesList.length">
        <div class="section-header">
          <h2 class="section-title"><span class="title-icon">📚</span> 系列集</h2>
          <router-link to="/series" class="neon-text-btn">探索全部系列 →</router-link>
        </div>
        <div class="series-scroll-container">
          <router-link v-for="s in seriesList" :key="s.id"
                       :to="`/series/${s.slug || s.id}`" class="series-card-link">
            <GlassCard class="series-card fade-in">
              <div class="series-cover" :style="{ backgroundImage: s.coverImage ? `url(${s.coverImage})` : 'var(--gradient-2)' }">
                <div class="series-progress-badge">共 {{ s.articleCount }} 篇</div>
              </div>
              <div class="series-body">
                <h3>{{ s.name }}</h3>
                <p>{{ s.description || '探索这个有趣的系列内容。' }}</p>
              </div>
            </GlassCard>
          </router-link>
        </div>
      </section>

      <!-- ===== Newsletter (订阅) ===== -->
      <section class="section newsletter-section fade-in">
        <GlassCard class="newsletter-card">
          <div class="newsletter-glow" />
          <h2 class="gradient-text newsletter-title">订阅 Newsletter</h2>
          <p class="newsletter-desc">新文章发布时第一时间通知你。无垃圾邮件，随时退订。</p>
          <form class="newsletter-form" @submit.prevent="handleSubscribe">
            <input v-model="email" type="email" placeholder="你的邮箱地址" required />
            <button type="submit" class="neon-btn btn-small" :disabled="subscribing">
              {{ subscribing ? '订阅中...' : '立即订阅' }}
            </button>
          </form>
          <p v-if="subscribed" class="success-msg">✅ 订阅成功！</p>
        </GlassCard>
      </section>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getHomepageData, subscribe } from '../api'
import GlassCard from '../components/ui/GlassCard.vue'

const hero = ref<any>({ title: '', subtitle: '', ctaLink: '' })
const featuredArticles = ref<any[]>([])
const latestArticles = ref<any[]>([])
const popularArticles = ref<any[]>([])
const seriesList = ref<any[]>([])
const email = ref('')
const subscribing = ref(false)
const subscribed = ref(false)

// Particle Canvas
const particleCanvas = ref<HTMLCanvasElement>()
let animFrame = 0

function initParticles() {
  const canvas = particleCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')!
  if (!ctx) return

  const resize = () => {
    canvas.width = canvas.offsetWidth * window.devicePixelRatio
    canvas.height = canvas.offsetHeight * window.devicePixelRatio
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio)
  }
  resize()
  window.addEventListener('resize', resize)

  const w = () => canvas.offsetWidth
  const h = () => canvas.offsetHeight
  const particles: { x: number; y: number; vx: number; vy: number; r: number; color: string; alpha: number }[] = []
  const count = 70

  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w(),
      y: Math.random() * h(),
      vx: (Math.random() - 0.5) * 0.4,
      vy: (Math.random() - 0.5) * 0.3,
      r: Math.random() * 2 + 0.5,
      color: Math.random() > 0.5 ? '#a855f7' : '#22d3ee',
      alpha: Math.random() * 0.6 + 0.2
    })
  }

  function draw() {
    ctx.clearRect(0, 0, w(), h())
    for (const p of particles) {
      p.x += p.vx
      p.y += p.vy
      if (p.x < 0 || p.x > w()) p.vx *= -1
      if (p.y < 0 || p.y > h()) p.vy *= -1
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
      ctx.fillStyle = p.color
      ctx.globalAlpha = p.alpha
      ctx.fill()
    }
    // Draw connections
    ctx.globalAlpha = 0.05
    ctx.strokeStyle = '#a855f7'
    ctx.lineWidth = 0.8
    for (let i = 0; i < particles.length; i++) {
      for (let j = i + 1; j < particles.length; j++) {
        const dx = particles[i].x - particles[j].x
        const dy = particles[i].y - particles[j].y
        const dist = Math.sqrt(dx * dx + dy * dy)
        if (dist < 150) {
          ctx.beginPath()
          ctx.moveTo(particles[i].x, particles[i].y)
          ctx.lineTo(particles[j].x, particles[j].y)
          ctx.stroke()
        }
      }
    }
    ctx.globalAlpha = 1
    animFrame = requestAnimationFrame(draw)
  }
  draw()
}

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

async function handleSubscribe() {
  subscribing.value = true
  try { await subscribe({ email: email.value }); subscribed.value = true; email.value = '' }
  catch (e) { alert('订阅失败，请稍后重试') }
  finally { subscribing.value = false }
}

onMounted(async () => {
  try {
    const res: any = await getHomepageData()
    const data = res.data || {}
    if (data.hero) hero.value = data.hero
    featuredArticles.value = data.featuredArticles || []
    latestArticles.value = data.latestArticles || []
    popularArticles.value = data.popularArticles || []
    seriesList.value = data.seriesList || []
  } catch (e) { console.error(e) }

  initParticles()
})

onUnmounted(() => {
  cancelAnimationFrame(animFrame)
})
</script>

<style scoped>
/* ===== Hero: 60vh ===== */
.hero {
  position: relative;
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 120px 24px 80px;
  background-color: transparent;
}
.hero-bg-overlay {
  position: absolute; inset: 0; pointer-events: none; z-index: -2;
  background: radial-gradient(ellipse at center, rgba(168,85,247,0.1) 0%, transparent 60%);
}
.hero-particles {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  pointer-events: none; z-index: -1;
}
.hero-content {
  position: relative; z-index: 1;
  text-align: center; max-width: 900px;
}
.hero-title {
  font-family: var(--font-heading);
  font-size: 56px; font-weight: 900; line-height: 1.15;
  margin-bottom: 24px; letter-spacing: -0.02em;
  text-shadow: 0 10px 40px rgba(0,0,0,0.5);
}
.hero-desc {
  font-size: 20px; color: var(--text-dim);
  max-width: 680px; margin: 0 auto 40px;
  line-height: 1.85;
}
.hero-actions { display: flex; justify-content: center; margin-bottom: 20px; }

.neon-btn {
  display: inline-flex; align-items: center; gap: 12px;
  padding: 16px 40px;
  background: linear-gradient(135deg, #a855f7, #22d3ee);
  color: #fff; border-radius: 999px;
  font-size: 18px; font-weight: 800; font-family: var(--font-heading);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 10px 30px rgba(168,85,247,0.3), inset 0 -2px 0 rgba(0,0,0,0.2);
}
.neon-btn:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 15px 40px rgba(168,85,247,0.5), 0 0 60px rgba(34,211,238,0.2);
}
.btn-small { padding: 12px 24px; font-size: 15px; border: none; cursor: pointer; }

.scroll-down-hint {
  position: absolute; bottom: 30px; left: 50%; transform: translateX(-50%);
  color: var(--primary); animation: bounceHint 2s infinite; opacity: 0.6;
}
@keyframes bounceHint { 0%, 100% { transform: translate(-50%, 0); } 50% { transform: translate(-50%, 10px); } }

/* ===== Main Content layout ===== */
.main-content {
  max-width: 1280px; margin: 0 auto;
  padding: 80px 24px 120px;
}

/* ===== Shared Section ===== */
.section { margin-bottom: 80px; }
.section-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 40px; }
.section-title {
  font-family: var(--font-heading); font-size: 32px; font-weight: 800;
  color: var(--text-heading); letter-spacing: -0.02em;
  display: flex; align-items: center; gap: 12px;
}
.title-icon { font-size: 28px; }
.card-meta { display: flex; gap: 8px; font-size: 13px; color: var(--text-dim); margin-top: auto; font-weight: 600; }
.meta-dot { opacity: 0.5; }
.neon-text-btn { font-size: 15px; font-weight: 700; color: var(--primary); transition: color 0.3s; letter-spacing: 0.05em; }
.neon-text-btn:hover { color: var(--accent-cyan); text-shadow: 0 0 15px rgba(34,211,238,0.4); }

/* ===== Featured Posts (编辑推荐) ===== */
.featured-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(380px, 1fr)); gap: 32px; }
.featured-card-link { text-decoration: none; display: block; }
.featured-card {
  padding: 0; overflow: hidden; height: 100%; display: flex; flex-direction: column;
}
.featured-cover { height: 220px; background-size: cover; background-position: center; position: relative; transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1); }
.featured-card-link:hover .featured-cover { transform: scale(1.05); }
.card-badge {
  position: absolute; top: 16px; left: 16px;
  background: rgba(15,23,42,0.8); color: #fff; backdrop-filter: blur(8px);
  padding: 6px 16px; border-radius: 999px;
  font-size: 12px; font-weight: 700; border: 1px solid rgba(168,85,247,0.3);
}
.featured-body { padding: 32px; flex: 1; display: flex; flex-direction: column; background: var(--card-bg); z-index: 1;}
.card-feature-title {
  font-family: var(--font-heading); font-size: 24px; font-weight: 800; color: var(--text-primary);
  margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
  transition: color 0.3s; line-height: 1.4;
}
.featured-card-link:hover .card-feature-title { background: var(--gradient-text); background-clip: text; -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.card-feature-excerpt {
  font-size: 15px; color: var(--text-secondary); line-height: 1.8; margin-bottom: 24px;
  display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}

/* ===== Masonry Latest Articles ===== */
.masonry-grid { columns: 3; column-gap: 32px; }
.masonry-card-link { display: block; break-inside: avoid; margin-bottom: 32px; text-decoration: none; }
.masonry-card { padding: 0; overflow: hidden; }
.masonry-cover { height: 190px; background-size: cover; background-position: center; }
.masonry-body { padding: 28px; background: var(--card-bg); }
.masonry-title { font-family: var(--font-heading); font-size: 22px; font-weight: 800; color: var(--text-primary); margin-bottom: 10px; line-height: 1.4; }
.masonry-excerpt { font-size: 15px; color: var(--text-secondary); line-height: 1.7; margin-bottom: 20px; display: -webkit-box; -webkit-line-clamp: 3; line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }

/* Neon Labels */
.post-tags { display: flex; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
.neon-label {
  padding: 4px 12px; border-radius: 999px; font-size: 11px; font-weight: 700;
  background: color-mix(in srgb, var(--tag-color) 10%, transparent); color: var(--tag-color);
  border: 1px solid color-mix(in srgb, var(--tag-color) 20%, transparent); text-transform: uppercase; letter-spacing: 0.05em;
}

/* ===== Hot Articles (Numbered List) ===== */
.hot-list { display: flex; flex-direction: column; gap: 16px; }
.hot-list-item {
  display: flex; align-items: center; gap: 24px; padding: 20px 32px;
  background: var(--card-bg); border-radius: 20px; border: 1px solid var(--border);
  box-shadow: var(--shadow); transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  text-decoration: none; position: relative; overflow: hidden;
}
.hot-list-item:hover { transform: translateX(8px); border-color: rgba(168,85,247,0.4); background: var(--bg-hover); box-shadow: 0 10px 40px rgba(168,85,247,0.15); }
.hot-rank-box {
  width: 48px; height: 48px; flex-shrink: 0; border-radius: 12px;
  display: flex; align-items: center; justify-content: center; position: relative;
  background: rgba(168,85,247,0.1); border: 1px solid rgba(168,85,247,0.2);
}
.hot-rank-num { font-family: var(--font-heading); font-size: 20px; font-weight: 900; color: var(--primary); }
.rank-1 { background: linear-gradient(135deg, #f59e0b, #ef4444); border: none; }
.rank-1 .hot-rank-num { color: #fff; }
.rank-2 { background: linear-gradient(135deg, #a855f7, #6366f1); border: none; }
.rank-2 .hot-rank-num { color: #fff; }
.rank-3 { background: linear-gradient(135deg, #22d3ee, #3b82f6); border: none; }
.rank-3 .hot-rank-num { color: #fff; }

.hot-info { flex: 1; }
.hot-title { font-family: var(--font-heading); font-size: 18px; font-weight: 700; color: var(--text-primary); margin-bottom: 6px; }
.hot-meta { font-size: 13px; color: var(--text-dim); font-weight: 500; display: flex; gap: 12px; }
.hot-arrow { color: var(--text-muted); font-size: 20px; transition: transform 0.3s, color 0.3s; }
.hot-list-item:hover .hot-arrow { transform: translateX(6px); color: var(--primary); }

/* ===== Series Wrap ===== */
.series-scroll-container {
  display: flex; gap: 32px; overflow-x: auto; padding-bottom: 24px;
  scroll-snap-type: x mandatory; scrollbar-width: none;
}
.series-scroll-container::-webkit-scrollbar { display: none; }
.series-card-link { min-width: 360px; max-width: 400px; flex-shrink: 0; scroll-snap-align: start; text-decoration: none; }
.series-card { padding: 0; overflow: hidden; }
.series-cover { height: 180px; background-size: cover; background-position: center; position: relative; }
.series-progress-badge {
  position: absolute; bottom: 16px; left: 16px;
  background: rgba(10,10,10,0.8); backdrop-filter: blur(8px);
  color: #fff; padding: 6px 16px; border-radius: 999px;
  font-size: 12px; font-weight: 700; border: 1px solid rgba(255,255,255,0.1);
}
.series-body { padding: 24px; background: var(--card-bg); }
.series-body h3 { font-family: var(--font-heading); font-size: 20px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
.series-body p { font-size: 14px; color: var(--text-secondary); line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* ===== Newsletter ===== */
.newsletter-card { padding: 64px; text-align: center; position: relative; overflow: hidden; }
.newsletter-glow {
  position: absolute; inset: 0; pointer-events: none;
  background: radial-gradient(circle at 30% 50%, rgba(168,85,247,0.08) 0%, transparent 50%),
              radial-gradient(circle at 70% 50%, rgba(34,211,238,0.05) 0%, transparent 50%);
}
.newsletter-title { font-family: var(--font-heading); font-size: 32px; font-weight: 800; margin-bottom: 12px; }
.newsletter-desc { font-size: 16px; color: var(--text-secondary); margin-bottom: 32px; }
.newsletter-form { display: flex; gap: 16px; max-width: 500px; margin: 0 auto; position: relative; z-index: 10;}
.newsletter-form input {
  flex: 1; padding: 16px 24px; border-radius: 999px; border: 1px solid var(--border);
  background: var(--bg-hover); color: var(--text-primary); font-size: 16px; outline: none; transition: all 0.3s;
}
.newsletter-form input:focus { border-color: var(--primary); background: var(--bg-elevated); box-shadow: 0 0 0 4px rgba(168,85,247,0.1); }
.success-msg { color: #10b981; margin-top: 16px; font-weight: 600; }

/* Responsive adjustments */
@media (max-width: 1024px) {
  .hero-title { font-size: 48px; }
  .masonry-grid { columns: 2; }
}
@media (max-width: 768px) {
  .hero { min-height: 50vh; padding: 100px 20px 60px; }
  .hero-title { font-size: 36px; }
  .hero-desc { font-size: 16px; }
  .section-title { font-size: 26px; }
  .masonry-grid { columns: 1; }
  .featured-grid { grid-template-columns: 1fr; }
  .hot-list-item { padding: 16px 20px; gap: 16px; }
  .hot-rank-box { width: 40px; height: 40px; }
  .hot-rank-num { font-size: 16px; }
  .series-card-link { min-width: 300px; }
}
</style>
