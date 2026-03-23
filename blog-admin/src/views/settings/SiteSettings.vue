<template>
  <div class="site-settings">
    <el-tabs v-model="activeTab" type="border-card" style="border-radius: 12px;">

      <!-- Tab 1: General -->
      <el-tab-pane label="🏠 基本设置" name="general">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="站点标题"><el-input v-model="config.site_title" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="config.site_description" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="关键词"><el-input v-model="config.site_keywords" /></el-form-item>
          <el-form-item label="作者"><el-input v-model="config.site_author" /></el-form-item>
          <el-form-item label="ICP备案号"><el-input v-model="config.site_icp" /></el-form-item>
          <el-form-item label="评论审核"><el-switch v-model="commentReview" /></el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab 2: Brand & Theme -->
      <el-tab-pane label="🎨 品牌与主题" name="brand">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="Logo 地址"><el-input v-model="config.site_logo" placeholder="https://..." /></el-form-item>
          <el-form-item label="Favicon 地址"><el-input v-model="config.site_favicon" placeholder="https://..." /></el-form-item>
          <el-divider>颜色</el-divider>
          <el-form-item label="主色调">
            <div style="display: flex; align-items: center; gap: 12px;">
              <el-color-picker v-model="config.theme_primary_color" show-alpha />
              <el-input v-model="config.theme_primary_color" style="width: 160px;" />
            </div>
          </el-form-item>
          <el-form-item label="强调色">
            <div style="display: flex; align-items: center; gap: 12px;">
              <el-color-picker v-model="config.theme_accent_color" show-alpha />
              <el-input v-model="config.theme_accent_color" style="width: 160px;" />
            </div>
          </el-form-item>
          <el-form-item label="背景色">
            <div style="display: flex; align-items: center; gap: 12px;">
              <el-color-picker v-model="config.theme_bg_color" />
              <el-input v-model="config.theme_bg_color" style="width: 160px;" />
            </div>
          </el-form-item>
          <el-divider>字体</el-divider>
          <el-form-item label="标题字体">
            <el-select v-model="config.theme_heading_font" placeholder="选择字体" style="width: 100%;">
              <el-option label="Inter" value="Inter" />
              <el-option label="Outfit" value="Outfit" />
              <el-option label="Roboto" value="Roboto" />
              <el-option label="Noto Sans SC" value="Noto Sans SC" />
              <el-option label="Merriweather" value="Merriweather" />
              <el-option label="Playfair Display" value="Playfair Display" />
              <el-option label="系统默认" value="system-ui" />
            </el-select>
          </el-form-item>
          <el-form-item label="正文字体">
            <el-select v-model="config.theme_body_font" placeholder="选择字体" style="width: 100%;">
              <el-option label="Inter" value="Inter" />
              <el-option label="Roboto" value="Roboto" />
              <el-option label="Noto Sans SC" value="Noto Sans SC" />
              <el-option label="Lora" value="Lora" />
              <el-option label="系统默认" value="system-ui" />
            </el-select>
          </el-form-item>
          <el-divider>深色模式</el-divider>
          <el-form-item label="深色模式">
            <el-select v-model="config.theme_dark_mode" style="width: 200px;">
              <el-option label="自动（跟随系统）" value="auto" />
              <el-option label="始终浅色" value="light" />
              <el-option label="始终深色" value="dark" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab 3: Hero Section -->
      <el-tab-pane label="🦸 首页横幅" name="hero">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="启用横幅"><el-switch v-model="heroEnabled" /></el-form-item>
          <el-form-item label="横幅标题"><el-input v-model="config.hero_title" /></el-form-item>
          <el-form-item label="横幅副标题"><el-input v-model="config.hero_subtitle" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="横幅背景"><el-input v-model="config.hero_bg_image" placeholder="图片链接或渐变CSS" /></el-form-item>
          <el-form-item label="CTA 按钮文字"><el-input v-model="config.hero_cta_text" placeholder="例：阅读博客" /></el-form-item>
          <el-form-item label="CTA 按钮链接"><el-input v-model="config.hero_cta_link" placeholder="/articles" /></el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab 4: Homepage Modules -->
      <el-tab-pane label="📦 首页模块" name="modules">
        <p style="color: #999; font-size: 13px; margin-bottom: 16px;">切换并重新排序博客首页的各个模块，拖拽调整顺序。</p>
        <div class="module-list">
          <div v-for="(mod, idx) in homeModules" :key="mod.key" class="module-item" draggable="true"
               @dragstart="dragStart(idx)" @dragover.prevent @drop="dragDrop(idx)">
            <div class="module-handle">☰</div>
            <div class="module-info">
              <span class="module-icon">{{ mod.icon }}</span>
              <span class="module-name">{{ mod.label }}</span>
            </div>
            <el-switch v-model="mod.enabled" />
          </div>
        </div>
      </el-tab-pane>

      <!-- Tab 5: Navigation Menu -->
      <el-tab-pane label="📋 导航菜单" name="menu">
        <div style="display: flex; justify-content: space-between; margin-bottom: 16px;">
          <span style="font-weight: 600;">菜单项</span>
          <el-button type="primary" size="small" @click="addMenu">
            <el-icon><Plus /></el-icon> 添加菜单项
          </el-button>
        </div>
        <el-table :data="menus" stripe style="width: 100%;" row-key="id">
          <el-table-column label="☰" width="50">
            <template #default="{ $index }">
              <div style="cursor: grab; color: #c0c4cc; font-size: 18px;" draggable="true"
                   @dragstart="menuDragStart($index)" @dragover.prevent @drop="menuDragDrop($index)">☰</div>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="标题" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="url" label="路径" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.url" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="目标" width="100">
            <template #default="{ row }">
              <el-select v-model="row.target" size="small" style="width: 80px;">
                <el-option label="_self" value="_self" />
                <el-option label="_blank" value="_blank" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="可见" width="80">
            <template #default="{ row }"><el-switch v-model="row.visible" /></template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-popconfirm title="确定删除？" @confirm="removeMenu(row.id)">
                <template #reference><el-button type="danger" size="small" text><el-icon><Delete /></el-icon></el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" style="margin-top: 12px;" @click="saveMenus" :loading="menuSaving">保存菜单顺序</el-button>
      </el-tab-pane>

      <!-- Tab 6: Footer & Social -->
      <el-tab-pane label="🔗 页脚与社交" name="footer">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="页脚文本"><el-input v-model="config.site_footer" type="textarea" :rows="2" /></el-form-item>
          <el-divider>社交链接</el-divider>
          <el-form-item label="GitHub"><el-input v-model="config.social_github" /></el-form-item>
          <el-form-item label="Twitter / X"><el-input v-model="config.social_twitter" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="config.social_email" /></el-form-item>
          <el-form-item label="微信"><el-input v-model="config.social_wechat" /></el-form-item>
          <el-form-item label="微博"><el-input v-model="config.social_weibo" /></el-form-item>
          <el-form-item label="Bilibili"><el-input v-model="config.social_bilibili" /></el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- Global Save -->
    <div class="save-bar">
      <el-button type="primary" size="large" :loading="saving" @click="handleSave">
        💾 保存全部设置
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getSiteConfig, updateSiteConfig,
  getMenus, createMenu, updateMenu, deleteMenu, batchSortMenus
} from '../../api/modules'
import { ElMessage } from 'element-plus'

const activeTab = ref('general')
const loading = ref(false)
const saving = ref(false)
const menuSaving = ref(false)

const config = reactive<Record<string, string>>({})
const menus = ref<any[]>([])

const commentReview = computed({
  get: () => config.comment_review === 'true',
  set: (v: boolean) => { config.comment_review = String(v) }
})
const heroEnabled = computed({
  get: () => config.hero_enabled === 'true',
  set: (v: boolean) => { config.hero_enabled = String(v) }
})

// ---- Homepage modules ----
const defaultModules = [
  { key: 'hero', label: 'Hero Banner', icon: '🦸', enabled: true },
  { key: 'featured', label: '精选文章', icon: '⭐', enabled: true },
  { key: 'recent', label: 'Recent Posts', icon: '📝', enabled: true },
  { key: 'categories', label: '分类管理', icon: '📁', enabled: true },
  { key: 'tags', label: '标签云', icon: '🏷️', enabled: true },
  { key: 'newsletter', label: '邮件订阅', icon: '📬', enabled: true },
  { key: 'about', label: '关于我', icon: '👤', enabled: false }
]
const homeModules = ref([...defaultModules])

let dragIdx = -1
function dragStart(idx: number) { dragIdx = idx }
function dragDrop(idx: number) {
  if (dragIdx < 0 || dragIdx === idx) return
  const item = homeModules.value.splice(dragIdx, 1)[0]
  homeModules.value.splice(idx, 0, item)
  dragIdx = -1
}

// Menu drag sort
let menuDragIdx = -1
function menuDragStart(idx: number) { menuDragIdx = idx }
function menuDragDrop(idx: number) {
  if (menuDragIdx < 0 || menuDragIdx === idx) return
  const item = menus.value.splice(menuDragIdx, 1)[0]
  menus.value.splice(idx, 0, item)
  menuDragIdx = -1
}

function addMenu() {
  menus.value.push({ id: null, name: '', url: '/', target: '_self', visible: true, sortOrder: menus.value.length, isExternal: false, _isNew: true })
}

async function removeMenu(id: number | null) {
  if (id) {
    await deleteMenu(id)
    ElMessage.success('已删除')
  }
  menus.value = menus.value.filter(m => m.id !== id)
}

async function saveMenus() {
  menuSaving.value = true
  try {
    // Save new items first
    for (const m of menus.value) {
      if (m._isNew) {
        const res: any = await createMenu(m)
        m.id = res.data
        delete m._isNew
      } else {
        await updateMenu(m.id, m)
      }
    }
    // Batch sort
    const sortItems = menus.value.map((m, i) => ({ id: m.id, sortOrder: i }))
    await batchSortMenus(sortItems)
    ElMessage.success('Menu saved')
    await loadMenus()
  } catch (e: any) { ElMessage.error(e.message) }
  finally { menuSaving.value = false }
}

// ---- Load / Save ----

async function loadData() {
  loading.value = true
  try {
    const r: any = await getSiteConfig()
    const list = r.data || []
    if (Array.isArray(list)) {
      list.forEach((c: any) => { config[c.configKey] = c.configValue || '' })
    }
    // Parse homepage modules from config
    if (config.home_modules) {
      try { homeModules.value = JSON.parse(config.home_modules) } catch { /* keep default */ }
    }
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

async function loadMenus() {
  try {
    const r: any = await getMenus()
    menus.value = r.data || []
  } catch { /* ignore */ }
}

async function handleSave() {
  saving.value = true
  try {
    // Serialize home modules into config
    config.home_modules = JSON.stringify(homeModules.value)
    await updateSiteConfig(config)
    ElMessage.success('Settings saved!')
  } catch (e: any) { ElMessage.error(e.message) }
  finally { saving.value = false }
}

onMounted(() => { loadData(); loadMenus() })
</script>

<style scoped>
.site-settings { padding: 4px; }

/* Save bar */
.save-bar { margin-top: 24px; display: flex; justify-content: flex-end; }
.save-bar :deep(.el-button--primary) {
  background: linear-gradient(135deg, #a855f7, #7c3aed) !important;
  border: none !important; font-weight: 700; padding: 12px 32px !important;
  border-radius: 14px !important; font-size: 15px;
  box-shadow: 0 0 24px rgba(168,85,247,0.2);
}
.save-bar :deep(.el-button--primary:hover) {
  transform: translateY(-2px); box-shadow: 0 8px 32px rgba(168,85,247,0.35);
}

/* Glass tabs */
:deep(.el-tabs--border-card) {
  background: rgba(15,23,42,0.6) !important;
  backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1) !important;
  border-radius: 16px !important;
}
:deep(.el-tabs__header) { background: rgba(168,85,247,0.04) !important; border-bottom-color: rgba(255,255,255,0.05) !important; }
:deep(.el-tabs__item) { color: #64748b !important; font-weight: 500; }
:deep(.el-tabs__item.is-active) { color: #c084fc !important; background: rgba(168,85,247,0.08) !important; border-bottom-color: #a855f7 !important; }
:deep(.el-tabs__content) { padding: 24px !important; }

/* Form spacing */
:deep(.el-form-item) { margin-bottom: 28px; }
:deep(.el-divider__text) { color: #94a3b8 !important; background: rgba(15,23,42,0.6) !important; font-weight: 600; }
:deep(.el-divider) { border-color: rgba(255,255,255,0.06); }

/* Module items */
.module-list { max-width: 500px; }
.module-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; border-radius: 12px; margin-bottom: 8px; cursor: grab;
  background: rgba(15,23,42,0.4); border: 1px solid rgba(255,255,255,0.05);
  transition: all 0.25s;
}
.module-item:hover { border-color: rgba(168,85,247,0.15); background: rgba(168,85,247,0.06); }
.module-handle { color: #475569; font-size: 18px; user-select: none; }
.module-handle:hover { color: #a855f7; }
.module-info { flex: 1; display: flex; align-items: center; gap: 8px; }
.module-icon { font-size: 18px; }
.module-name { font-size: 14px; font-weight: 500; color: #f8fafc; }
</style>
