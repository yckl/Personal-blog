<template>
  <div class="container">
    <!-- Hero Section -->
    <section class="hero fade-in">
      <div class="hero-badge">{{ hero.badge }}</div>
      <h1 class="hero-title" v-html="heroTitle"></h1>
      <p class="hero-desc">{{ hero.subtitle }}</p>
      <div class="hero-actions">
        <router-link :to="hero.ctaLink || '/articles'" class="btn btn-primary">{{ hero.ctaText || 'Read Articles →' }}</router-link>
        <router-link to="/about" class="btn btn-ghost">About Me</router-link>
      </div>
    </section>

    <!-- Top / Pinned Posts -->
    <section class="section" v-if="topArticles.length">
      <h2 class="section-title">📌 Top Picks</h2>
      <div class="popular-grid">
        <router-link v-for="article in topArticles" :key="article.id"
                     :to="`/article/${article.slug}`" class="popular-card fade-in top-card">
          <span class="popular-rank">TOP</span>
          <div class="popular-info">
            <h4>{{ article.title }}</h4>
            <div class="card-meta">
              <span v-if="article.categoryName">{{ article.categoryName }}</span>
              <span>·</span>
              <span>👁 {{ article.viewCount }}</span>
              <span>·</span>
              <span>💬 {{ article.commentCount || 0 }}</span>
            </div>
          </div>
        </router-link>
      </div>
    </section>

    <!-- Featured Posts -->
    <section class="section" v-if="featuredArticles.length">
      <h2 class="section-title">⭐ Featured</h2>
      <div class="featured-grid">
        <router-link v-for="article in featuredArticles" :key="article.id"
                     :to="`/article/${article.slug}`" class="featured-card fade-in">
          <div class="featured-cover" :style="{ backgroundImage: article.coverImage ? `url(${article.coverImage})` : 'var(--gradient-1)' }">
            <span v-if="article.categoryName" class="card-badge">{{ article.categoryName }}</span>
          </div>
          <div class="featured-body">
            <h3>{{ article.title }}</h3>
            <p>{{ article.excerpt || 'Click to read more...' }}</p>
            <div class="card-meta">
              <span>{{ formatDate(article.publishedAt) }}</span>
              <span>·</span>
              <span>👁 {{ article.viewCount }}</span>
            </div>
          </div>
        </router-link>
      </div>
    </section>

    <!-- Recent Posts -->
    <section class="section" v-if="latestArticles.length">
      <h2 class="section-title">📝 Recent Posts</h2>
      <div class="posts-list">
        <router-link v-for="article in latestArticles" :key="article.id"
                     :to="`/article/${article.slug}`" class="post-card fade-in">
          <div class="post-info">
            <div class="post-tags">
              <span v-for="tag in (article.tags || []).slice(0,3)" :key="tag.id" class="tag"
                    :style="{ '--tag-color': tag.color || '#6366f1' }">{{ tag.name }}</span>
            </div>
            <h3>{{ article.title }}</h3>
            <p>{{ article.excerpt || 'Click to read the full article...' }}</p>
            <div class="card-meta">
              <span>{{ formatDate(article.publishedAt) }}</span>
              <span>·</span>
              <span>{{ article.wordCount || 0 }} words</span>
              <span>·</span>
              <span>👁 {{ article.viewCount }}</span>
            </div>
          </div>
          <div v-if="article.coverImage" class="post-cover" :style="{ backgroundImage: `url(${article.coverImage})` }" />
        </router-link>
      </div>
    </section>

    <!-- Popular Posts -->
    <section class="section" v-if="popularArticles.length">
      <h2 class="section-title">🔥 Popular</h2>
      <div class="popular-grid">
        <router-link v-for="(article, idx) in popularArticles" :key="article.id"
                     :to="`/article/${article.slug}`" class="popular-card fade-in">
          <span class="popular-rank">{{ idx + 1 }}</span>
          <div class="popular-info">
            <h4>{{ article.title }}</h4>
            <div class="card-meta">
              <span>👁 {{ article.viewCount }}</span>
              <span>·</span>
              <span>❤ {{ article.likeCount || 0 }}</span>
              <span>·</span>
              <span>💬 {{ article.commentCount || 0 }}</span>
            </div>
          </div>
        </router-link>
      </div>
    </section>

    <!-- Series -->
    <section class="section" v-if="seriesList.length">
      <h2 class="section-title">📚 Series</h2>
      <div class="series-grid">
        <router-link v-for="s in seriesList" :key="s.id"
                     :to="`/series/${s.slug || s.id}`" class="series-card fade-in">
          <div class="series-cover" :style="{ backgroundImage: s.coverImage ? `url(${s.coverImage})` : 'var(--gradient-2)' }">
            <span class="series-count">{{ s.articleCount }} articles</span>
          </div>
          <div class="series-body">
            <h3>{{ s.name }}</h3>
            <p>{{ s.description || 'Explore this series...' }}</p>
          </div>
        </router-link>
      </div>
    </section>

    <!-- Newsletter -->
    <section class="section newsletter-section fade-in">
      <div class="newsletter-card">
        <h2>📬 Subscribe to Newsletter</h2>
        <p>Get notified when I publish new articles. No spam, unsubscribe anytime.</p>
        <form class="newsletter-form" @submit.prevent="handleSubscribe">
          <input v-model="email" type="email" placeholder="your@email.com" required />
          <button type="submit" class="btn btn-primary" :disabled="subscribing">{{ subscribing ? 'Subscribing...' : 'Subscribe' }}</button>
        </form>
        <p v-if="subscribed" style="color: var(--accent-green); margin-top: 8px;">✅ Subscribed successfully!</p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getHomepageData, subscribe } from '../api'

const hero = ref<any>({ badge: '✨ Personal Blog', title: 'Thoughts, Stories & Ideas Worth Sharing', subtitle: 'Exploring technology, creativity, and the art of building things that matter.', ctaText: 'Read Articles →', ctaLink: '/articles' })
const topArticles = ref<any[]>([])
const featuredArticles = ref<any[]>([])
const latestArticles = ref<any[]>([])
const popularArticles = ref<any[]>([])
const seriesList = ref<any[]>([])
const email = ref('')
const subscribing = ref(false)
const subscribed = ref(false)

const heroTitle = computed(() => {
  const t = hero.value.title || ''
  // Split on first comma or line break for gradient effect
  const parts = t.split(/[,\n]/);
  return parts.length > 1
    ? `${parts[0]}<br /><span class="gradient-text">${parts.slice(1).join(', ').trim()}</span>`
    : t
})

function formatDate(d: string) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

async function handleSubscribe() {
  subscribing.value = true
  try { await subscribe({ email: email.value }); subscribed.value = true; email.value = '' }
  catch (e) { alert('Subscribe failed') }
  finally { subscribing.value = false }
}

onMounted(async () => {
  try {
    const res: any = await getHomepageData()
    const data = res.data || {}
    if (data.hero) hero.value = data.hero
    topArticles.value = data.topArticles || []
    featuredArticles.value = data.featuredArticles || []
    latestArticles.value = data.latestArticles || []
    popularArticles.value = data.popularArticles || []
    seriesList.value = data.seriesList || []
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.hero { text-align: center; padding: 80px 0 60px; }
.hero-badge { display: inline-block; padding: 6px 16px; background: rgba(99,102,241,0.15); color: var(--primary-light); border-radius: 20px; font-size: 13px; font-weight: 600; margin-bottom: 24px; }
.hero-title { font-size: 48px; font-weight: 800; line-height: 1.2; color: var(--text-heading); margin-bottom: 16px; }
.gradient-text { background: var(--gradient-1); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.hero-desc { font-size: 18px; color: var(--text-muted); max-width: 500px; margin: 0 auto 32px; }
.hero-actions { display: flex; gap: 12px; justify-content: center; }

.btn { padding: 12px 24px; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.2s; border: none; display: inline-flex; align-items: center; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-dark); transform: translateY(-1px); box-shadow: 0 4px 16px rgba(99,102,241,0.4); }
.btn-ghost { background: transparent; color: var(--text-muted); border: 1px solid var(--border); }
.btn-ghost:hover { border-color: var(--primary-light); color: var(--primary-light); }

.section { margin-bottom: 48px; }
.section-title { font-size: 24px; font-weight: 700; color: var(--text-heading); margin-bottom: 24px; }

/* Featured */
.featured-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px; }
.featured-card { background: var(--bg-card); border-radius: var(--radius); overflow: hidden; transition: all 0.3s; border: 1px solid var(--border); text-decoration: none; color: inherit; }
.featured-card:hover { transform: translateY(-4px); box-shadow: var(--shadow); border-color: var(--primary); }
.featured-cover { height: 180px; background-size: cover; background-position: center; position: relative; }
.card-badge { position: absolute; top: 12px; left: 12px; background: rgba(0,0,0,0.6); color: #fff; padding: 4px 12px; border-radius: 6px; font-size: 12px; font-weight: 600; backdrop-filter: blur(4px); }
.featured-body { padding: 20px; }
.featured-body h3 { font-size: 18px; color: var(--text-heading); margin-bottom: 8px; font-weight: 600; }
.featured-body p { font-size: 14px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

.card-meta { display: flex; gap: 8px; font-size: 13px; color: var(--text-dim); margin-top: 12px; }

/* Recent */
.posts-list { display: flex; flex-direction: column; gap: 16px; }
.post-card { display: flex; gap: 20px; background: var(--bg-card); border-radius: var(--radius); padding: 24px; border: 1px solid var(--border); transition: all 0.3s; text-decoration: none; color: inherit; }
.post-card:hover { border-color: var(--primary); transform: translateX(4px); }
.post-info { flex: 1; }
.post-info h3 { font-size: 18px; color: var(--text-heading); margin-bottom: 8px; }
.post-info p { font-size: 14px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.post-cover { width: 160px; min-height: 120px; border-radius: 8px; background-size: cover; background-position: center; flex-shrink: 0; }
.post-tags { display: flex; gap: 6px; margin-bottom: 8px; flex-wrap: wrap; }
.tag { padding: 2px 10px; border-radius: 4px; font-size: 12px; font-weight: 500; background: color-mix(in srgb, var(--tag-color) 15%, transparent); color: var(--tag-color); }

/* Popular */
.popular-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; }
.popular-card { display: flex; align-items: center; gap: 16px; background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 16px 20px; transition: all 0.3s; text-decoration: none; color: inherit; }
.popular-card:hover { border-color: var(--accent-amber); transform: translateY(-2px); }
.popular-rank { width: 36px; height: 36px; background: linear-gradient(135deg, var(--accent-amber), #ef4444); color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 15px; flex-shrink: 0; }
.popular-info { flex: 1; }
.popular-info h4 { font-size: 15px; color: var(--text-heading); margin-bottom: 4px; font-weight: 600; }
.top-card:hover { border-color: var(--primary); }
.top-card .popular-rank { width: 52px; border-radius: 999px; font-size: 12px; background: linear-gradient(135deg, #6366f1, #8b5cf6); }

/* Series */
.series-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; }
.series-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; transition: all 0.3s; text-decoration: none; color: inherit; }
.series-card:hover { transform: translateY(-4px); box-shadow: var(--shadow); border-color: var(--primary); }
.series-cover { height: 140px; background-size: cover; background-position: center; position: relative; }
.series-count { position: absolute; bottom: 12px; right: 12px; background: rgba(0,0,0,0.7); color: #fff; padding: 4px 12px; border-radius: 6px; font-size: 12px; font-weight: 600; backdrop-filter: blur(4px); }
.series-body { padding: 16px; }
.series-body h3 { font-size: 16px; color: var(--text-heading); margin-bottom: 6px; font-weight: 600; }
.series-body p { font-size: 13px; color: var(--text-muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }

/* Newsletter */
.newsletter-card { background: var(--bg-card); border: 1px solid var(--border); border-radius: var(--radius); padding: 48px; text-align: center; }
.newsletter-card h2 { font-size: 28px; color: var(--text-heading); margin-bottom: 12px; }
.newsletter-card > p { color: var(--text-muted); margin-bottom: 24px; }
.newsletter-form { display: flex; gap: 12px; max-width: 440px; margin: 0 auto; }
.newsletter-form input { flex: 1; padding: 12px 16px; border-radius: 10px; border: 1px solid var(--border); background: var(--bg); color: var(--text); font-size: 15px; outline: none; }
.newsletter-form input:focus { border-color: var(--primary); }

/* Responsive */
@media (max-width: 768px) {
  .hero-title { font-size: 32px; }
  .hero-desc { font-size: 16px; }
  .hero-actions { flex-direction: column; align-items: center; }
  .featured-grid { grid-template-columns: 1fr; }
  .popular-grid { grid-template-columns: 1fr; }
  .series-grid { grid-template-columns: 1fr; }
  .post-card { flex-direction: column; }
  .post-cover { width: 100%; min-height: 160px; }
  .newsletter-form { flex-direction: column; }
}
</style>
