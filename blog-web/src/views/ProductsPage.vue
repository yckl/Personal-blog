<template>
  <div class="products-page">
    <!-- Hero Section -->
    <header class="hero-section">
      <div class="hero-content">
        <h1 class="page-title gradient-text">数字工坊</h1>
        <p class="page-subtitle">探索我精心打磨的电子书、代码资源与专属教程，助力你的成长旅程。</p>
        
        <div class="filter-tabs">
          <button 
            v-for="f in filters" :key="f.value"
            class="filter-btn" :class="{ active: typeFilter === f.value }"
            @click="typeFilter = f.value; loadProducts()"
          >
            {{ f.label }}
          </button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="container">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在加载数字资产...</p>
      </div>
      
      <div v-else-if="products.length === 0" class="empty-state">
        <div class="empty-icon glass-icon">📦</div>
        <p>这里暂时是空的</p>
      </div>

      <div v-else class="products-grid">
        <div v-for="product in products" :key="product.id" class="product-card glass-card">
          <div class="card-cover">
            <img v-if="product.coverImage" :src="product.coverImage" :alt="product.title" />
            <div v-else class="cover-placeholder"></div>
            <div class="type-badge">{{ formatType(product.productType) }}</div>
          </div>
          
          <div class="card-body">
            <h3 class="product-title">{{ product.title }}</h3>
            <p class="product-desc">{{ product.description }}</p>
            
            <div class="card-footer">
              <div class="product-price">
                <span v-if="product.priceCents > 0">
                  <span class="currency">¥</span>
                  <span class="amount">{{ (product.priceCents / 100).toFixed(2) }}</span>
                </span>
                <span v-else class="price-free gradient-text">限时免费</span>
              </div>
              
              <button class="buy-btn" @click="handlePurchase(product)">
                {{ product.priceCents > 0 ? '立即获取' : '免费领取' }}
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Pagination -->
      <div v-if="total > size" class="pagination-wrapper">
        <button 
          class="page-btn" :disabled="page === 1"
          @click="page--; loadProducts()"
        >上一页</button>
        <span class="page-info">{{ page }} / {{ Math.ceil(total / size) }}</span>
        <button 
          class="page-btn" :disabled="page >= Math.ceil(total / size)"
          @click="page++; loadProducts()"
        >下一页</button>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const products = ref<any[]>([])
const page = ref(1)
const size = ref(12)
const total = ref(0)
const typeFilter = ref('')

const filters = [
  { label: '全部作品', value: '' },
  { label: '📚 电子书', value: 'EBOOK' },
  { label: '🎓 课程', value: 'COURSE' },
  { label: '🧰 资源包', value: 'RESOURCE' }
]

function formatType(t: string) {
  const map: Record<string, string> = { EBOOK: '电子书', COURSE: '在线课程', RESOURCE: '资源包' }
  return map[t] || t
}

async function loadProducts() {
  loading.value = true
  try {
    const res: any = await request.get('/api/products', {
      params: { page: page.value, size: size.value, type: typeFilter.value || undefined }
    })
    products.value = res.data || []
    total.value = res.total || 0 // Assuming backend returns total or we just rely on infinite scroll logic
  } catch (e) {
    console.error('Failed to load products', e)
  } finally {
    loading.value = false
  }
}

async function handlePurchase(product: any) {
  if (!authStore.isLoggedIn) {
    authStore.openLogin()
    return
  }
  
  if (product.priceCents === 0) {
    // Free purchase logic
    try {
      const res: any = await request.post(`/api/products/${product.id}/purchase`, {
        memberId: authStore.user?.id
      })
      alert('领取成功！数字产品将在您的会员中心展示或发送至您的邮箱。')
      router.push('/member')
    } catch (e: any) {
      alert(e.message || '领取失败')
    }
  } else {
    // Route to checkout / member center
    alert('暂未开启在线支付功能。')
  }
}

onMounted(() => {
  loadProducts()
})
</script>

<style scoped>
.products-page {
  min-height: 100vh;
  padding-bottom: 80px;
}

/* ===== Hero Section ===== */
.hero-section {
  padding: 80px 24px 60px;
  text-align: center;
  position: relative;
  overflow: hidden;
}
.hero-section::before {
  content: ''; position: absolute; top: -50%; left: 50%; transform: translateX(-50%);
  width: 80vw; height: 80vw;
  background: radial-gradient(circle, rgba(168,85,247,0.15) 0%, transparent 60%);
  z-index: -1; pointer-events: none;
}
.page-title {
  font-family: var(--font-heading); font-size: 48px; font-weight: 800;
  letter-spacing: -0.03em; margin-bottom: 16px;
}
.page-subtitle {
  font-size: 18px; color: var(--text-dim); max-width: 600px;
  margin: 0 auto 40px; line-height: 1.6;
}

/* Filter Tabs */
.filter-tabs {
  display: inline-flex; background: rgba(15,23,42,0.4);
  padding: 6px; border-radius: 999px; border: 1px solid rgba(255,255,255,0.05);
  backdrop-filter: blur(12px); gap: 4px;
}
.filter-btn {
  padding: 10px 24px; border-radius: 999px;
  background: transparent; border: none; font-size: 15px; font-weight: 600;
  color: var(--text-muted); cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.filter-btn:hover { color: var(--text-heading); }
.filter-btn.active {
  background: rgba(168,85,247,0.15); color: var(--primary-light);
  box-shadow: 0 4px 12px rgba(168,85,247,0.1);
}

/* ===== Grid Layout ===== */
.products-grid {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 32px; padding: 0 24px; max-width: 1200px; margin: 0 auto;
}

/* Product Card */
.product-card {
  border-radius: 20px; overflow: hidden;
  display: flex; flex-direction: column;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.product-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 24px 48px rgba(168,85,247,0.15), 0 0 0 1px rgba(168,85,247,0.25);
}
.card-cover {
  width: 100%; height: 200px; position: relative; overflow: hidden;
  background: var(--bg-surface);
}
.card-cover img {
  width: 100%; height: 100%; object-fit: cover; transition: transform 0.6s;
}
.product-card:hover .card-cover img { transform: scale(1.05); }
.cover-placeholder { width: 100%; height: 100%; background: linear-gradient(135deg, rgba(168,85,247,0.1), rgba(34,211,238,0.1)); }

.type-badge {
  position: absolute; top: 16px; right: 16px;
  background: rgba(15,23,42,0.7); backdrop-filter: blur(8px);
  color: #fff; padding: 4px 12px; border-radius: 999px;
  font-size: 12px; font-weight: 700; letter-spacing: 0.05em;
  border: 1px solid rgba(255,255,255,0.1);
}

.card-body { padding: 24px; display: flex; flex-direction: column; flex: 1; }
.product-title {
  font-family: var(--font-heading); font-size: 20px; font-weight: 800;
  color: var(--text-heading); margin-bottom: 12px;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
}
.product-desc {
  font-size: 14px; color: var(--text-dim); line-height: 1.6;
  margin-bottom: 24px; flex: 1;
  overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical;
}

.card-footer {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: auto; padding-top: 20px; border-top: 1px solid rgba(255,255,255,0.05);
}
.product-price { display: flex; align-items: baseline; gap: 4px; }
.currency { font-size: 14px; color: var(--primary); font-weight: 600; }
.amount { font-size: 24px; font-weight: 800; color: var(--text-heading); font-family: var(--font-heading); }
.price-free { font-size: 18px; font-weight: 800; }

.buy-btn {
  padding: 10px 20px; border-radius: 12px; font-size: 14px; font-weight: 700;
  background: var(--primary); color: #fff; border: none; cursor: pointer;
  transition: all 0.3s;
}
.buy-btn:hover { background: var(--primary-light); box-shadow: 0 4px 16px rgba(168,85,247,0.4); transform: translateY(-2px); }

/* States */
.loading-state, .empty-state { text-align: center; padding: 100px 20px; color: var(--text-muted); }
.spinner { width: 40px; height: 40px; border: 3px solid rgba(168,85,247,0.2); border-top-color: var(--primary); border-radius: 50%; animation: spin 1s linear infinite; margin: 0 auto 20px; }
@keyframes spin { to { transform: rotate(360deg); } }
.empty-icon { font-size: 48px; width: 100px; height: 100px; line-height: 100px; border-radius: 50%; margin: 0 auto 24px; background: rgba(15,23,42,0.4); }

.pagination-wrapper { display: flex; justify-content: center; align-items: center; gap: 24px; margin-top: 60px; }
.page-btn { padding: 10px 24px; border-radius: 12px; background: rgba(15,23,42,0.4); color: var(--text-heading); border: 1px solid var(--border); transition: all 0.2s; cursor: pointer; font-weight: 600; }
.page-btn:hover:not(:disabled) { background: rgba(168,85,247,0.1); border-color: var(--primary); color: var(--primary-light); }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-info { color: var(--text-dim); font-size: 14px; font-weight: 600; }

@media (max-width: 768px) {
  .hero-section { padding: 60px 20px 40px; }
  .page-title { font-size: 36px; }
  .filter-tabs { flex-wrap: wrap; justify-content: center; border-radius: 20px; }
  .filter-btn { padding: 8px 16px; font-size: 14px; }
  .products-grid { grid-template-columns: 1fr; gap: 24px; }
}
</style>
