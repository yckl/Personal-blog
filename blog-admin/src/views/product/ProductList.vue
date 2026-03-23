<template>
  <div class="product-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">🛍️ 数字产品</h3>
      <el-button type="primary" @click="showDialog=true; editingId=null; form=emptyForm()">+ 添加产品</el-button>
    </div>

    <!-- ===== Product Card Grid ===== -->
    <div class="product-grid" v-loading="loading">
      <div v-for="p in products" :key="p.id" class="product-card glass-admin" @click="editProduct(p)">
        <div class="card-cover">
          <img v-if="p.coverImage" :src="p.coverImage" />
          <div v-else class="cover-placeholder">🎁</div>
          <div class="cover-gradient" />
          <span :class="['type-pill', p.productType?.toLowerCase()]">{{ p.productType }}</span>
        </div>
        <div class="card-body">
          <h4 class="card-title">{{ p.title }}</h4>
          <div class="card-meta">
            <span class="card-price" :class="{ free: p.priceCents === 0 }">
              {{ p.priceCents === 0 ? 'Free' : `$${(p.priceCents / 100).toFixed(2)}` }}
            </span>
            <span :class="['status-badge', p.status?.toLowerCase()]">
              <span class="status-dot" /> {{ p.status }}
            </span>
          </div>
          <div class="card-stats">
            <span>📥 {{ p.downloadCount || 0 }} downloads</span>
            <span class="visibility-tag">{{ p.visibility }}</span>
          </div>
        </div>
        <div class="card-actions">
          <el-button type="primary" size="small" text @click.stop="editProduct(p)">编辑</el-button>
          <el-popconfirm title="Delete this product?" @confirm="handleDelete(p.id)">
            <template #reference><el-button type="danger" size="small" text @click.stop>删除</el-button></template>
          </el-popconfirm>
        </div>
      </div>
    </div>

    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadData" />

    <!-- Dialog -->
    <el-dialog v-model="showDialog" :title="editingId ? '编辑产品' : '添加产品'" width="650">
      <el-form :model="form" label-width="120px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="别名"><el-input v-model="form.slug" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.productType">
            <el-option label="eBook" value="EBOOK" /><el-option label="Course" value="COURSE" /><el-option label="Resource Pack" value="RESOURCE_PACK" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="封面图片"><el-input v-model="form.coverImage" placeholder="链接" /></el-form-item>
        <el-form-item label="文件链接"><el-input v-model="form.fileUrl" placeholder="下载链接" /></el-form-item>
        <el-form-item label="预览链接"><el-input v-model="form.previewUrl" placeholder="预览/样品链接" /></el-form-item>
        <el-form-item label="Price (cents)"><el-input-number v-model="form.priceCents" :min="0" :step="100" /> <span style="margin-left:8px;color:#64748b;">{{ form.priceCents===0?'Free': `$${(form.priceCents/100).toFixed(2)}` }}</span></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status"><el-option label="Draft" value="DRAFT" /><el-option label="发布时间" value="PUBLISHED" /><el-option label="Archived" value="ARCHIVED" /></el-select>
        </el-form-item>
        <el-form-item label="Visibility">
          <el-select v-model="form.visibility"><el-option label="Public" value="PUBLIC" /><el-option label="Member Only" value="MEMBER_ONLY" /><el-option label="Premium Only" value="PREMIUM_ONLY" /></el-select>
        </el-form-item>
        <el-form-item label="排序号"><el-input-number v-model="form.sortOrder" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" @click="handleSave">{{ editingId ? 'Update' : 'Create' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const products = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const showDialog = ref(false)
const editingId = ref<number | null>(null)
const emptyForm = () => ({ title: '', slug: '', productType: 'EBOOK', description: '', coverImage: '', fileUrl: '', previewUrl: '', priceCents: 0, status: 'DRAFT', visibility: 'PUBLIC', sortOrder: 0 })
const form = ref<any>(emptyForm())

async function loadData() {
  loading.value = true
  try {
    const r: any = await request.get('/api/admin/products', { params: { page: page.value, size: 20 } })
    products.value = r.data?.records || r.data || []
    total.value = r.data?.total || 0
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function editProduct(row: any) { editingId.value = row.id; form.value = { ...row }; showDialog.value = true }

async function handleSave() {
  if (editingId.value) { await request.put(`/api/admin/products/${editingId.value}`, form.value) }
  else { await request.post('/api/admin/products', form.value) }
  ElMessage.success(editingId.value ? '已更新' : '已创建')
  showDialog.value = false; editingId.value = null; form.value = emptyForm(); loadData()
}

async function handleDelete(id: number) { await request.delete(`/api/admin/products/${id}`); ElMessage.success('已删除'); loadData() }
onMounted(loadData)
</script>

<style scoped>
.product-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
  transition: all 0.3s;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }

/* Product Grid */
.product-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.product-card { padding: 0; overflow: hidden; cursor: pointer; }
.product-card:hover { transform: translateY(-3px); border-color: rgba(168,85,247,0.25); }

.card-cover { position: relative; height: 160px; background: rgba(15,23,42,0.4); overflow: hidden; }
.card-cover img { width: 100%; height: 100%; object-fit: cover; }
.cover-placeholder {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  font-size: 48px; background: linear-gradient(135deg, rgba(168,85,247,0.15), rgba(34,211,238,0.1));
}
.cover-gradient {
  position: absolute; inset: 0;
  background: linear-gradient(to top, rgba(15,23,42,0.8) 0%, transparent 50%);
}
.type-pill {
  position: absolute; top: 10px; right: 10px;
  padding: 4px 10px; border-radius: 999px; font-size: 11px; font-weight: 700;
  backdrop-filter: blur(8px); text-transform: uppercase;
}
.type-pill.ebook { background: rgba(168,85,247,0.3); color: #c084fc; }
.type-pill.course { background: rgba(245,158,11,0.3); color: #fbbf24; }
.type-pill.resource_pack { background: rgba(34,211,238,0.3); color: #67e8f9; }

.card-body { padding: 16px; }
.card-title { font-size: 15px; font-weight: 600; color: #f8fafc; margin-bottom: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-meta { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.card-price {
  font-size: 18px; font-weight: 800;
  background: linear-gradient(135deg, #a855f7, #22d3ee);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
}
.card-price.free { background: none; -webkit-text-fill-color: #10b981; color: #10b981; }

/* Status badge */
.status-badge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 600;
}
.status-dot { width: 5px; height: 5px; border-radius: 50%; }
.status-badge.published { background: rgba(16,185,129,0.1); color: #10b981; }
.status-badge.published .status-dot { background: #10b981; }
.status-badge.draft { background: rgba(148,163,184,0.1); color: #94a3b8; }
.status-badge.draft .status-dot { background: #94a3b8; }
.status-badge.archived { background: rgba(244,63,94,0.1); color: #f43f5e; }
.status-badge.archived .status-dot { background: #f43f5e; }

.card-stats { display: flex; justify-content: space-between; font-size: 12px; color: #475569; }
.visibility-tag { padding: 2px 8px; border-radius: 999px; background: rgba(255,255,255,0.05); font-size: 11px; }

.card-actions { padding: 0 16px 12px; display: flex; gap: 4px; }

/* Responsive */
@media (max-width: 1024px) { .product-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .product-grid { grid-template-columns: 1fr; } }
</style>
