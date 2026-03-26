<template>
  <div class="uc-assets">
    <h2 class="uc-section-title gradient-text">我的数字资产</h2>
    <div v-if="loading" class="uc-loading">加载中...</div>
    <div v-else-if="!assets.length" class="uc-empty">
      <div class="empty-icon">📦</div>
      <p>您还没有领取或购买任何数字产品</p>
      <button class="btn btn-primary" style="margin-top: 16px;" @click="$router.push('/products')">去发现好物</button>
    </div>
    <div v-else class="assets-grid">
      <div v-for="item in assets" :key="item.purchase.id" class="asset-card">
        <div class="asset-cover">
          <img v-if="item.product.coverImage" :src="item.product.coverImage" />
          <div v-else class="cover-placeholder" />
        </div>
        <div class="asset-info">
          <h3>{{ item.product.title }}</h3>
          <p>获取时间：{{ formatDate(item.purchase.createdAt) }}</p>
          <div class="asset-actions">
            <button class="btn btn-primary btn-sm" @click="handleDownload(item.purchase.downloadToken)">
              {{ item.product.productType === 'COURSE' ? '前往学习' : '下载文件' }}
            </button>
            <span class="download-count" v-if="item.purchase.maxDownloads > 0">
              还可下载 {{ Math.max(0, item.purchase.maxDownloads - item.purchase.downloadCount) }} 次
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import request from '../../utils/request'

const authStore = useAuthStore()
const assets = ref<any[]>([])
const loading = ref(false)

function formatDate(d: string) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

async function handleDownload(token: string) {
  if (!token) { alert('无效的下载凭证'); return }
  try {
    const res: any = await request.get(`/api/products/download/${token}`)
    if (res.data?.fileUrl) {
      window.open('http://localhost:8088' + res.data.fileUrl, '_blank')
    } else { alert('无法获取文件链接') }
  } catch (e: any) { alert(e.message || '下载失败') }
}

onMounted(async () => {
  if (!authStore.user) return
  loading.value = true
  try {
    const res: any = await request.get(`/api/member/${authStore.user.id}/products`)
    assets.value = res.data || []
  } catch {} finally { loading.value = false }
})
</script>

<style scoped>
.uc-assets { display: flex; flex-direction: column; height: 100%; }
.uc-section-title {
  font-family: var(--font-heading); font-size: 28px; font-weight: 800;
  margin-bottom: 24px; letter-spacing: -0.02em;
}
.uc-loading, .uc-empty {
  text-align: center; padding: 48px; color: var(--text-dim); font-size: 16px;
  flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.empty-icon {
  font-size: 48px; width: 100px; height: 100px; line-height: 100px;
  border-radius: 50%; margin-bottom: 24px;
  background: rgba(15,23,42,0.4);
}
.assets-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px; flex: 1; }
.asset-card {
  border-radius: 16px; overflow: hidden; display: flex; flex-direction: column;
  background: var(--bg-card, rgba(15,23,42,0.4));
  border: 1px solid var(--border, rgba(255,255,255,0.06));
}
.asset-cover { height: 160px; background: var(--border); }
.asset-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder { width: 100%; height: 100%; background: linear-gradient(135deg, rgba(168,85,247,0.1), rgba(34,211,238,0.1)); }
.asset-info { padding: 20px; flex: 1; display: flex; flex-direction: column; }
.asset-info h3 { font-size: 16px; font-weight: 700; color: var(--text-heading); margin-bottom: 8px; }
.asset-info p { font-size: 13px; color: var(--text-dim); margin-bottom: 20px; }
.asset-actions { margin-top: auto; display: flex; flex-direction: column; gap: 8px; }
.btn-sm {
  padding: 8px 16px; font-size: 13px; border-radius: 8px; border: none;
  cursor: pointer; background: var(--primary, #8b5cf6); color: #fff; transition: all 0.2s;
}
.btn-sm:hover { background: var(--primary-light, #a78bfa); transform: translateY(-1px); }
.download-count { font-size: 12px; color: var(--text-dim); text-align: center; }
</style>
