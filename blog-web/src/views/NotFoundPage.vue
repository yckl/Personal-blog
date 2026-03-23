<template>
  <div class="not-found-page fade-in">
    <!-- Particle Background -->
    <div class="particles" aria-hidden="true">
      <div class="particle p1" />
      <div class="particle p2" />
      <div class="particle p3" />
      <div class="particle p4" />
      <div class="particle p5" />
      <div class="particle p6" />
    </div>

    <!-- ===== Hero ===== -->
    <div class="nf-hero">
      <div class="nf-code">
        <span class="digit">4</span>
        <span class="digit zero">0</span>
        <span class="digit">4</span>
      </div>
      <h1 class="nf-title">迷失在代码星空中</h1>
      <p class="nf-desc">服务器响应说："我尽力了，但这可能是通往平行宇宙的链接。"<br>启动紧急跃迁程序，带你返回安全地带。</p>

      <div class="nf-actions">
        <!-- Neon Back Button -->
        <button class="btn-neon" @click="$router.push('/')">
          <span class="neon-text">← 启动跃迁 (返回首页)</span>
        </button>
        <router-link to="/articles" class="btn btn-ghost-neon">探索星系 (文章列表)</router-link>
      </div>
    </div>

    <!-- ===== Search ===== -->
    <div class="nf-search">
      <form @submit.prevent="goSearch" class="search-form glass">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
        <input v-model="query" type="text" placeholder="扫描其他区域..." />
        <button type="submit" class="btn btn-primary submit-btn">开始扫描</button>
      </form>
    </div>

    <!-- ===== Random Recommended ===== -->
    <section v-if="recommended.length" class="nf-recommended">
      <h2>最近发现的生命信号 (推荐阅读)</h2>
      <div class="rec-grid">
        <router-link v-for="a in recommended" :key="a.id" :to="`/article/${a.slug}`" class="rec-card glass-card">
          <div class="rec-topbar" />
          <div class="rec-body">
            <h3>{{ a.title }}</h3>
            <span class="rec-meta">{{ a.viewCount || 0 }} 次探测</span>
          </div>
        </router-link>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const query = ref('')
const recommended = ref<any[]>([])

function goSearch() {
  if (query.value.trim()) {
    router.push({ path: '/search', query: { q: query.value.trim() } })
  }
}

onMounted(async () => {
  try {
    const res: any = await request.get('/api/public/homepage')
    const all = [...(res.data?.popularArticles || []), ...(res.data?.latestArticles || [])]
    // Shuffle and pick 4
    const shuffled = all.sort(() => Math.random() - 0.5)
    const unique = new Map<number, any>()
    for (const a of shuffled) { if (!unique.has(a.id)) unique.set(a.id, a) }
    recommended.value = Array.from(unique.values()).slice(0, 4)
  } catch {}
})
</script>

<style scoped>
.not-found-page {
  position: relative; max-width: 900px; margin: 0 auto;
  padding: 0 24px 80px; text-align: center; overflow: hidden;
}

/* ===== Particles ===== */
.particles { position: absolute; inset: 0; z-index: -1; pointer-events: none; overflow: visible; }
.particle { position: absolute; border-radius: 50%; background: #22d3ee; filter: blur(4px); box-shadow: 0 0 10px #22d3ee; animation: float 10s infinite linear; opacity: 0.3; }
.p1 { width: 80px; height: 80px; top: -20px; left: 10%; background: #a855f7; box-shadow: 0 0 20px #a855f7; animation-duration: 12s; }
.p2 { width: 40px; height: 40px; top: 30%; left: 85%; animation-duration: 8s; animation-direction: reverse; }
.p3 { width: 120px; height: 120px; top: 60%; left: -10%; background: #a855f7; box-shadow: 0 0 30px #a855f7; filter: blur(8px); animation-duration: 15s; }
.p4 { width: 20px; height: 20px; top: 80%; left: 70%; animation-duration: 6s; }
.p5 { width: 60px; height: 60px; top: 10%; left: 60%; background: #a855f7; animation-duration: 9s; filter: blur(6px); }
.p6 { width: 30px; height: 30px; top: 50%; left: 40%; animation-duration: 7s; }

@keyframes float {
  0% { transform: translateY(0) scale(1) rotate(0deg); opacity: 0.2; }
  33% { transform: translateY(-30px) scale(1.1) rotate(120deg); opacity: 0.4; }
  66% { transform: translateY(20px) scale(0.9) rotate(240deg); opacity: 0.3; }
  100% { transform: translateY(0) scale(1) rotate(360deg); opacity: 0.2; }
}

/* ===== Hero ===== */
.nf-hero { padding: 64px 0 56px; position: relative; z-index: 10; }
.nf-code { display: flex; justify-content: center; gap: 16px; margin-bottom: 32px; filter: drop-shadow(0 0 20px rgba(168,85,247,0.4)); }
.digit {
  font-family: var(--font-heading);
  font-size: 160px; font-weight: 900; line-height: 1;
  background: linear-gradient(135deg, #a855f7 0%, #22d3ee 100%);
  background-size: 200% 200%;
  animation: gradientShift 6s ease infinite;
  -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent;
  letter-spacing: -0.05em; text-shadow: 0 10px 30px rgba(0,0,0,0.5);
}
.digit.zero {
  animation: floatBounce 4s ease-in-out infinite, gradientShift 6s ease infinite;
  display: inline-block; transform-origin: center bottom;
}
@keyframes floatBounce {
  0%, 100% { transform: translateY(0) scaleY(1); }
  50% { transform: translateY(-24px) scaleY(1.05); }
}

.nf-title { font-family: var(--font-heading); font-size: 36px; font-weight: 900; color: #f8fafc; margin-bottom: 16px; letter-spacing: -0.02em; }
.nf-desc { color: var(--text-muted); font-size: 18px; line-height: 1.8; margin-bottom: 48px; max-width: 600px; margin-left: auto; margin-right: auto; }

/* Actions */
.nf-actions { display: flex; gap: 24px; justify-content: center; align-items: center; }

/* Neon Button */
.btn-neon {
  position: relative; display: inline-flex; align-items: center; justify-content: center;
  padding: 18px 40px; border-radius: 999px; border: none;
  background: transparent; color: #22d3ee; font-size: 16px; font-weight: 800; cursor: pointer;
  letter-spacing: 0.05em; text-transform: uppercase;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid #22d3ee; box-shadow: inset 0 0 10px rgba(34,211,238,0.4), 0 0 20px rgba(34,211,238,0.4);
  font-family: var(--font-body);
}
.btn-neon:hover {
  background: #22d3ee; color: #0f172a;
  box-shadow: 0 0 30px #22d3ee, 0 0 60px rgba(34,211,238,0.6);
  transform: scale(1.05);
}

.btn-ghost-neon {
  padding: 18px 40px; border-radius: 999px;
  background: rgba(168,85,247,0.1); color: #a855f7; font-size: 16px; font-weight: 700;
  border: 1px solid rgba(168,85,247,0.3); text-decoration: none;
  transition: all 0.4s; letter-spacing: 0.05em;
}
.btn-ghost-neon:hover {
  background: rgba(168,85,247,0.25); border-color: #a855f7; color: #f8fafc;
  box-shadow: 0 0 20px rgba(168,85,247,0.4); transform: scale(1.05);
}

/* ===== Search ===== */
.nf-search { margin-bottom: 64px; position: relative; z-index: 10; }
.search-form {
  display: flex; align-items: center; gap: 12px;
  max-width: 560px; margin: 0 auto;
  padding: 8px 10px 8px 20px;
  border-radius: 99px; background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(255,255,255,0.08); box-shadow: 0 10px 40px rgba(0,0,0,0.3);
  transition: all 0.3s;
}
.search-form:focus-within { border-color: rgba(34,211,238,0.5); box-shadow: 0 10px 40px rgba(0,0,0,0.4), 0 0 20px rgba(34,211,238,0.2); transform: translateY(-2px); }
.search-form svg { color: var(--text-dim); flex-shrink: 0; transition: color 0.3s; }
.search-form:focus-within svg { color: #22d3ee; }
.search-form input {
  flex: 1; padding: 14px 0; background: transparent; border: none;
  color: #f8fafc; font-size: 16px; outline: none; font-weight: 500;
  font-family: var(--font-body);
}
.search-form input::placeholder { color: var(--text-muted); }
.submit-btn { padding: 12px 28px; border-radius: 999px; font-size: 15px; font-weight: 700; box-shadow: 0 4px 16px rgba(168,85,247,0.4); }

/* ===== Recommended ===== */
.nf-recommended { text-align: left; position: relative; z-index: 10; margin-top: 24px; }
.nf-recommended h2 {
  font-family: var(--font-heading);
  font-size: 24px; font-weight: 800; color: #f8fafc;
  margin-bottom: 24px; text-align: center; letter-spacing: -0.01em;
}
.rec-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 24px; }
.rec-card {
  text-decoration: none; color: inherit; border-radius: 16px;
  overflow: hidden; background: rgba(15,23,42,0.4); backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.05); box-shadow: 0 10px 30px rgba(0,0,0,0.2);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex; flex-direction: column;
}
.rec-card:hover { transform: translateY(-6px); border-color: rgba(34,211,238,0.3); box-shadow: 0 16px 40px rgba(0,0,0,0.3), 0 0 20px rgba(34,211,238,0.15); }
.rec-topbar { height: 4px; background: linear-gradient(90deg, #a855f7, #22d3ee); width: 100%; transition: opacity 0.3s; opacity: 0.7; }
.rec-card:hover .rec-topbar { opacity: 1; }
.rec-body { padding: 24px; flex: 1; display: flex; flex-direction: column; justify-content: center; }
.rec-body h3 {
  font-family: var(--font-heading); font-size: 18px; font-weight: 700;
  color: #f8fafc; margin-bottom: 12px; line-height: 1.4;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.rec-meta { font-size: 13px; font-weight: 600; color: var(--text-dim); }

/* Responsive */
@media (max-width: 768px) {
  .digit { font-size: 110px; }
  .nf-title { font-size: 28px; }
  .nf-actions { flex-direction: column; align-items: stretch; }
  .btn-neon, .btn-ghost-neon { text-align: center; }
  .rec-grid { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .digit { font-size: 80px; }
}
</style>
