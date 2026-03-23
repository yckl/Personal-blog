<template>
  <div class="site-settings">
    <el-tabs v-model="activeTab" type="border-card" style="border-radius: 12px;">

      <!-- Tab 1: General -->
      <el-tab-pane label="🏠 General" name="general">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="Site Title"><el-input v-model="config.site_title" /></el-form-item>
          <el-form-item label="Description"><el-input v-model="config.site_description" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="Keywords"><el-input v-model="config.site_keywords" /></el-form-item>
          <el-form-item label="Author"><el-input v-model="config.site_author" /></el-form-item>
          <el-form-item label="ICP备案号"><el-input v-model="config.site_icp" /></el-form-item>
          <el-form-item label="Review Comments"><el-switch v-model="commentReview" /></el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab 2: Brand & Theme -->
      <el-tab-pane label="🎨 Brand & Theme" name="brand">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="Logo URL"><el-input v-model="config.site_logo" placeholder="https://..." /></el-form-item>
          <el-form-item label="Favicon URL"><el-input v-model="config.site_favicon" placeholder="https://..." /></el-form-item>
          <el-divider>Colors</el-divider>
          <el-form-item label="Primary Color">
            <div style="display: flex; align-items: center; gap: 12px;">
              <el-color-picker v-model="config.theme_primary_color" show-alpha />
              <el-input v-model="config.theme_primary_color" style="width: 160px;" />
            </div>
          </el-form-item>
          <el-form-item label="Accent Color">
            <div style="display: flex; align-items: center; gap: 12px;">
              <el-color-picker v-model="config.theme_accent_color" show-alpha />
              <el-input v-model="config.theme_accent_color" style="width: 160px;" />
            </div>
          </el-form-item>
          <el-form-item label="Background Color">
            <div style="display: flex; align-items: center; gap: 12px;">
              <el-color-picker v-model="config.theme_bg_color" />
              <el-input v-model="config.theme_bg_color" style="width: 160px;" />
            </div>
          </el-form-item>
          <el-divider>Typography</el-divider>
          <el-form-item label="Heading Font">
            <el-select v-model="config.theme_heading_font" placeholder="Select font" style="width: 100%;">
              <el-option label="Inter" value="Inter" />
              <el-option label="Outfit" value="Outfit" />
              <el-option label="Roboto" value="Roboto" />
              <el-option label="Noto Sans SC" value="Noto Sans SC" />
              <el-option label="Merriweather" value="Merriweather" />
              <el-option label="Playfair Display" value="Playfair Display" />
              <el-option label="System Default" value="system-ui" />
            </el-select>
          </el-form-item>
          <el-form-item label="Body Font">
            <el-select v-model="config.theme_body_font" placeholder="Select font" style="width: 100%;">
              <el-option label="Inter" value="Inter" />
              <el-option label="Roboto" value="Roboto" />
              <el-option label="Noto Sans SC" value="Noto Sans SC" />
              <el-option label="Lora" value="Lora" />
              <el-option label="System Default" value="system-ui" />
            </el-select>
          </el-form-item>
          <el-divider>Dark Mode</el-divider>
          <el-form-item label="Dark Mode">
            <el-select v-model="config.theme_dark_mode" style="width: 200px;">
              <el-option label="Auto (follow system)" value="auto" />
              <el-option label="Always Light" value="light" />
              <el-option label="Always Dark" value="dark" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab 3: Hero Section -->
      <el-tab-pane label="🦸 Hero Section" name="hero">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="Enable Hero"><el-switch v-model="heroEnabled" /></el-form-item>
          <el-form-item label="Hero Title"><el-input v-model="config.hero_title" /></el-form-item>
          <el-form-item label="Hero Subtitle"><el-input v-model="config.hero_subtitle" type="textarea" :rows="2" /></el-form-item>
          <el-form-item label="Hero Background"><el-input v-model="config.hero_bg_image" placeholder="Image URL or gradient CSS" /></el-form-item>
          <el-form-item label="CTA Button Text"><el-input v-model="config.hero_cta_text" placeholder="e.g. Read Blog" /></el-form-item>
          <el-form-item label="CTA Button Link"><el-input v-model="config.hero_cta_link" placeholder="/articles" /></el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- Tab 4: Homepage Modules -->
      <el-tab-pane label="📦 Homepage Modules" name="modules">
        <p style="color: #999; font-size: 13px; margin-bottom: 16px;">Toggle and reorder the sections on your blog homepage. Drag to rearrange.</p>
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
      <el-tab-pane label="📋 Navigation" name="menu">
        <div style="display: flex; justify-content: space-between; margin-bottom: 16px;">
          <span style="font-weight: 600;">Menu Items</span>
          <el-button type="primary" size="small" @click="addMenu">
            <el-icon><Plus /></el-icon> Add Item
          </el-button>
        </div>
        <el-table :data="menus" stripe style="width: 100%;" row-key="id">
          <el-table-column label="☰" width="50">
            <template #default="{ $index }">
              <div style="cursor: grab; color: #c0c4cc; font-size: 18px;" draggable="true"
                   @dragstart="menuDragStart($index)" @dragover.prevent @drop="menuDragDrop($index)">☰</div>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="Title" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.name" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="url" label="Path" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.url" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="Target" width="100">
            <template #default="{ row }">
              <el-select v-model="row.target" size="small" style="width: 80px;">
                <el-option label="_self" value="_self" />
                <el-option label="_blank" value="_blank" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="Visible" width="80">
            <template #default="{ row }"><el-switch v-model="row.visible" /></template>
          </el-table-column>
          <el-table-column label="Actions" width="80">
            <template #default="{ row }">
              <el-popconfirm title="Delete?" @confirm="removeMenu(row.id)">
                <template #reference><el-button type="danger" size="small" text><el-icon><Delete /></el-icon></el-button></template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
        <el-button type="primary" style="margin-top: 12px;" @click="saveMenus" :loading="menuSaving">Save Menu Order</el-button>
      </el-tab-pane>

      <!-- Tab 6: Footer & Social -->
      <el-tab-pane label="🔗 Footer & Social" name="footer">
        <el-form :model="config" label-width="160px" style="max-width: 650px;">
          <el-form-item label="Footer Text"><el-input v-model="config.site_footer" type="textarea" :rows="2" /></el-form-item>
          <el-divider>Social Links</el-divider>
          <el-form-item label="GitHub"><el-input v-model="config.social_github" /></el-form-item>
          <el-form-item label="Twitter / X"><el-input v-model="config.social_twitter" /></el-form-item>
          <el-form-item label="Email"><el-input v-model="config.social_email" /></el-form-item>
          <el-form-item label="WeChat"><el-input v-model="config.social_wechat" /></el-form-item>
          <el-form-item label="Weibo"><el-input v-model="config.social_weibo" /></el-form-item>
          <el-form-item label="Bilibili"><el-input v-model="config.social_bilibili" /></el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>

    <!-- Global Save -->
    <div class="save-bar">
      <el-button type="primary" size="large" :loading="saving" @click="handleSave">
        💾 Save All Settings
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
  { key: 'featured', label: 'Featured Articles', icon: '⭐', enabled: true },
  { key: 'recent', label: 'Recent Posts', icon: '📝', enabled: true },
  { key: 'categories', label: 'Categories', icon: '📁', enabled: true },
  { key: 'tags', label: 'Tag Cloud', icon: '🏷️', enabled: true },
  { key: 'newsletter', label: 'Newsletter', icon: '📬', enabled: true },
  { key: 'about', label: 'About Me', icon: '👤', enabled: false }
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
    ElMessage.success('Deleted')
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
.save-bar {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
.module-list {
  max-width: 500px;
}
.module-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 16px; background: #fff; border: 1px solid #eee;
  border-radius: 8px; margin-bottom: 8px; cursor: grab;
  transition: box-shadow 0.2s;
}
.module-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.module-handle { color: #c0c4cc; font-size: 18px; user-select: none; }
.module-info { flex: 1; display: flex; align-items: center; gap: 8px; }
.module-icon { font-size: 18px; }
.module-name { font-size: 14px; font-weight: 500; }
</style>
