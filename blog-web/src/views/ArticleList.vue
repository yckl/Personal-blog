<template>
  <div class="container">
    <h1 class="page-title">All Articles</h1>
    <div class="posts-list">
      <router-link v-for="article in articles" :key="article.id" :to="`/article/${article.slug}`" class="post-card fade-in">
        <div class="post-info">
          <div class="post-tags">
            <span v-for="tag in (article.tags||[]).slice(0,3)" :key="tag.id" class="tag" :style="{ '--tag-color': tag.color || '#6366f1' }">{{ tag.name }}</span>
          </div>
          <h3>{{ article.title }}</h3>
          <p>{{ article.excerpt || '' }}</p>
          <div class="card-meta">
            <span>{{ formatDate(article.publishedAt || article.createdAt) }}</span>
            <span>·</span><span>{{ article.viewCount }} views</span>
            <span>·</span><span>{{ article.wordCount }} words</span>
          </div>
        </div>
        <div v-if="article.coverImage" class="post-cover" :style="{ backgroundImage: `url(${article.coverImage})` }" />
      </router-link>
    </div>
    <div v-if="totalPages > 1" class="pagination">
      <button :disabled="page <= 1" @click="page--; loadData()" class="btn btn-ghost">← Prev</button>
      <span style="color:var(--text-muted);">Page {{ page }} / {{ totalPages }}</span>
      <button :disabled="page >= totalPages" @click="page++; loadData()" class="btn btn-ghost">Next →</button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getArticles } from '../api'
const articles = ref<any[]>([]); const page = ref(1); const totalPages = ref(1)
function formatDate(d:string){if(!d)return '';return new Date(d).toLocaleDateString('en-US',{year:'numeric',month:'short',day:'numeric'})}
async function loadData(){try{const r:any=await getArticles({page:page.value,size:10});articles.value=r.data?.records||[];totalPages.value=r.data?.pages||1}catch(e){console.error(e)}}
onMounted(loadData)
</script>
<style scoped>
.page-title{font-size:32px;font-weight:700;color: var(--text-heading);margin-bottom:32px;}
.posts-list{display:flex;flex-direction:column;gap:16px;}
.post-card{display:flex;gap:20px;background:var(--bg-card);border-radius:var(--radius);padding:24px;border:1px solid var(--border);transition:all .3s;text-decoration:none;color:inherit;}
.post-card:hover{border-color:var(--primary);transform:translateX(4px);}
.post-info{flex:1;}.post-info h3{font-size:18px;color: var(--text-heading);margin-bottom:8px;}.post-info p{font-size:14px;color:var(--text-muted);display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;}
.post-cover{width:160px;min-height:120px;border-radius:8px;background-size:cover;background-position:center;flex-shrink:0;}
.post-tags{display:flex;gap:6px;margin-bottom:8px;}.tag{padding:2px 10px;border-radius:4px;font-size:12px;font-weight:500;background:color-mix(in srgb,var(--tag-color)15%,transparent);color:var(--tag-color);}
.card-meta{display:flex;gap:8px;font-size:13px;color:var(--text-dim);margin-top:12px;}
.pagination{display:flex;align-items:center;justify-content:center;gap:16px;margin-top:32px;}
.btn{padding:10px 20px;border-radius:8px;font-size:14px;font-weight:600;cursor:pointer;border:none;transition:all .2s;}
.btn-ghost{background:transparent;color:var(--text-muted);border:1px solid var(--border);}
.btn-ghost:hover{border-color:var(--primary-light);color:var(--primary-light);}
.btn:disabled{opacity:.4;cursor:not-allowed;}
</style>
