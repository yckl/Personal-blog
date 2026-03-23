<template>
  <div class="container not-found fade-in">
    <div class="nf-hero">
      <span class="nf-code">404</span>
      <h1>Page Not Found</h1>
      <p>Oops! The page you're looking for doesn't exist or has been moved.</p>
    </div>

    <!-- Search -->
    <div class="nf-search">
      <form @submit.prevent="goSearch">
        <input v-model="query" type="text" placeholder="Search articles..." />
        <button type="submit" class="btn btn-primary">Search</button>
      </form>
    </div>

    <!-- Quick Links -->
    <div class="nf-links">
      <router-link to="/" class="nf-link">🏠 Homepage</router-link>
      <router-link to="/articles" class="nf-link">📝 All Articles</router-link>
      <router-link to="/archive" class="nf-link">📅 Archive</router-link>
      <router-link to="/about" class="nf-link">👤 About</router-link>
    </div>

    <!-- Popular Articles -->
    <section v-if="popular.length" class="nf-popular">
      <h2>🔥 Popular Articles</h2>
      <div class="popular-list">
        <router-link v-for="a in popular" :key="a.id" :to="`/article/${a.slug}`" class="popular-item">
          <h3>{{ a.title }}</h3>
          <span class="card-meta">👁 {{ a.viewCount }}</span>
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
const popular = ref<any[]>([])

function goSearch() {
  if (query.value.trim()) {
    router.push({ path: '/search', query: { q: query.value.trim() } })
  }
}

onMounted(async () => {
  try {
    const res: any = await request.get('/api/public/homepage')
    popular.value = (res.data?.popularArticles || []).slice(0, 5)
  } catch {}
})
</script>

<style scoped>
.not-found { max-width: 600px; margin: 0 auto; text-align: center; }

.nf-hero { padding: 60px 0 32px; }
.nf-code {
  font-size: 120px; font-weight: 900; line-height: 1;
  background: var(--gradient-1); -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  display: block; margin-bottom: 16px;
}
.nf-hero h1 { font-size: 28px; font-weight: 700; color: var(--text-heading); margin-bottom: 8px; }
.nf-hero p { color: var(--text-muted); font-size: 16px; }

.nf-search { margin-bottom: 32px; }
.nf-search form { display: flex; gap: 12px; max-width: 440px; margin: 0 auto; }
.nf-search input {
  flex: 1; padding: 12px 16px; border-radius: 10px;
  border: 1px solid var(--border); background: var(--bg-card);
  color: var(--text); font-size: 15px; outline: none;
}
.nf-search input:focus { border-color: var(--primary); }
.btn { padding: 12px 24px; border-radius: 10px; font-size: 15px; font-weight: 600; cursor: pointer; border: none; }
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-dark); }

.nf-links { display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; margin-bottom: 48px; }
.nf-link {
  padding: 10px 20px; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: 10px; color: var(--text-muted); font-size: 14px; font-weight: 500;
  text-decoration: none; transition: all 0.2s;
}
.nf-link:hover { border-color: var(--primary); color: var(--primary-light); }

.nf-popular { text-align: left; }
.nf-popular h2 { font-size: 20px; font-weight: 700; color: var(--text-heading); margin-bottom: 16px; }
.popular-list { display: flex; flex-direction: column; gap: 8px; }
.popular-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px; background: var(--bg-card); border: 1px solid var(--border);
  border-radius: var(--radius); text-decoration: none; color: inherit;
  transition: all 0.2s;
}
.popular-item:hover { border-color: var(--primary); transform: translateX(4px); }
.popular-item h3 { font-size: 15px; color: var(--text-heading); font-weight: 500; }
.card-meta { font-size: 13px; color: var(--text-dim); }
</style>
