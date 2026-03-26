<template>
  <el-container style="height: 100vh; background: #0a0a0a;">
    <!-- ===== Glass Sidebar ===== -->
    <el-aside :width="isCollapse ? '68px' : '240px'" class="admin-sidebar">
      <div class="logo-area" @click="$router.push('/dashboard')">
        <div class="logo-icon">✍️</div>
        <span v-if="!isCollapse" class="logo-text">博客后台</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        background-color="transparent"
        text-color="#94a3b8"
        active-text-color="#c084fc"
        router
        :collapse-transition="false"
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:view')" index="/articles">
          <el-icon><Document /></el-icon>
          <template #title>文章管理</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:edit')" index="/categories">
          <el-icon><FolderOpened /></el-icon>
          <template #title>分类管理</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:edit')" index="/tags">
          <el-icon><PriceTag /></el-icon>
          <template #title>标签管理</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:edit')" index="/series">
          <el-icon><Collection /></el-icon>
          <template #title>系列管理</template>
        </el-menu-item>
        <el-menu-item v-if="can('comment:review')" index="/comments">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>评论管理</template>
        </el-menu-item>
        <el-menu-item v-if="can('media:upload')" index="/media">
          <el-icon><Picture /></el-icon>
          <template #title>媒体库</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/subscribers">
          <el-icon><Message /></el-icon>
          <template #title>订阅者</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/newsletter">
          <el-icon><Promotion /></el-icon>
          <template #title>邮件通讯</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/products">
          <el-icon><Goods /></el-icon>
          <template #title>数字产品</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>系统设置</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/messages">
          <el-icon><Message /></el-icon>
          <template #title>消息留言</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/timeline">
          <el-icon><Timer /></el-icon>
          <template #title>旅程时间线</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/system-log">
          <el-icon><List /></el-icon>
          <template #title>系统日志</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- ===== Main Content ===== -->
    <el-container>
      <!-- Glass Top Nav -->
      <el-header class="admin-header">
        <div class="header-left">
          <el-icon :size="20" class="collapse-toggle" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.name }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">

          <el-tag v-if="primaryRole" size="small" :type="roleTagType" effect="plain" class="role-tag">{{ primaryRole }}</el-tag>
          <span class="user-name">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-dropdown @command="handleCommand">
            <div class="avatar-wrap">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar">
                {{ (userStore.userInfo?.nickname || 'A')[0] }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
const userStore = useUserStore()
const router = useRouter()
const isCollapse = ref(false)

const isAdminUser = computed(() => userStore.isAdmin())

const primaryRole = computed(() => {
  if (userStore.hasRole('SUPER_ADMIN')) return '超级管理员'
  if (userStore.hasRole('ADMIN')) return '管理员'
  if (userStore.hasRole('EDITOR')) return '编辑'
  if (userStore.hasRole('AUTHOR')) return '作者'
  return ''
})

const roleTagType = computed(() => {
  if (userStore.hasRole('SUPER_ADMIN')) return 'danger'
  if (userStore.hasRole('ADMIN')) return 'warning'
  if (userStore.hasRole('EDITOR')) return 'success'
  return 'info'
})

function can(permission: string): boolean {
  return userStore.hasPermission(permission)
}

onMounted(async () => {
  try { await userStore.fetchUser() }
  catch { router.push('/login') }
})

function handleCommand(command: string) {
  if (command === 'logout') { userStore.logout(); router.push('/login') }
}
</script>

<style scoped>
/* ===== Sidebar ===== */
.admin-sidebar {
  background: rgba(15,23,42,0.85);
  backdrop-filter: blur(24px);
  border-right: 1px solid rgba(168,85,247,0.08);
  transition: width 0.3s;
  display: flex; flex-direction: column;
  overflow-y: auto; overflow-x: hidden;
}
.logo-area {
  height: 64px; display: flex; align-items: center; justify-content: center; gap: 10px;
  cursor: pointer; border-bottom: 1px solid rgba(255,255,255,0.04);
  flex-shrink: 0;
}
.logo-icon { font-size: 26px; }
.logo-text {
  font-family: 'Inter', sans-serif; font-size: 18px; font-weight: 800;
  background: linear-gradient(135deg, #a855f7, #22d3ee);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  letter-spacing: -0.02em;
}

/* Menu styling */
.sidebar-menu { border: none; flex: 1; }
:deep(.el-menu-item) {
  height: 48px; margin: 2px 8px; border-radius: 12px;
  font-size: 14px; font-weight: 500;
  transition: all 0.25s;
}
:deep(.el-menu-item:hover) {
  background: rgba(168,85,247,0.08) !important;
}
:deep(.el-menu-item.is-active) {
  background: rgba(168,85,247,0.12) !important;
  color: #c084fc !important;
  position: relative;
}
:deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute; left: 0; top: 50%; transform: translateY(-50%);
  width: 3px; height: 20px; border-radius: 0 4px 4px 0;
  background: linear-gradient(180deg, #a855f7, #22d3ee);
}
:deep(.el-menu-item.is-active .el-icon) {
  filter: drop-shadow(0 0 6px rgba(168,85,247,0.5));
}

/* ===== Top Nav ===== */
.admin-header {
  background: rgba(15,23,42,0.7);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255,255,255,0.04);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; height: 64px;
}
.header-left, .header-right {
  display: flex; align-items: center; gap: 14px;
}
.collapse-toggle { cursor: pointer; color: #94a3b8; transition: color 0.2s; }
.collapse-toggle:hover { color: #c084fc; }
:deep(.el-breadcrumb__inner) { color: #64748b !important; font-size: 13px; }
:deep(.el-breadcrumb__inner.is-link) { color: #94a3b8 !important; }

.role-tag { font-size: 11px; }
.user-name { font-size: 14px; color: #94a3b8; font-weight: 500; }
.avatar-wrap {
  position: relative; cursor: pointer;
}
.avatar-wrap::after {
  content: '';
  position: absolute; inset: -3px; border-radius: 50%;
  background: conic-gradient(#a855f7, #22d3ee, #a855f7);
  z-index: -1; opacity: 0.5;
  animation: avatarGlow 4s linear infinite;
}
@keyframes avatarGlow { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
:deep(.el-avatar) { border: 2px solid rgba(15,23,42,0.8); }

/* ===== Main ===== */
.admin-main {
  padding: 24px; overflow-y: auto;
  background: #0a0a0a;

  /* Override Element Plus CSS variables globally for dark theme */
  --el-input-bg-color: rgba(255,255,255,0.05);
  --el-input-border-color: rgba(255,255,255,0.08);
  --el-input-hover-border-color: rgba(168,85,247,0.2);
  --el-input-focus-border-color: #a855f7;
  --el-input-text-color: #f8fafc;
  --el-input-placeholder-color: #475569;
  --el-input-icon-color: #64748b;
  --el-text-color-regular: #94a3b8;
  --el-text-color-primary: #f8fafc;
  --el-text-color-secondary: #64748b;
  --el-text-color-placeholder: #475569;
  --el-bg-color: #0a0a0a;
  --el-bg-color-overlay: rgba(15,23,42,0.95);
  --el-fill-color-blank: rgba(255,255,255,0.05);
  --el-fill-color: rgba(255,255,255,0.05);
  --el-fill-color-light: rgba(255,255,255,0.03);
  --el-border-color: rgba(255,255,255,0.08);
  --el-border-color-light: rgba(255,255,255,0.05);
  --el-border-color-hover: rgba(168,85,247,0.2);
  --el-color-primary: #a855f7;
  --el-color-primary-light-3: rgba(168,85,247,0.3);
  --el-color-primary-light-5: rgba(168,85,247,0.5);
  --el-color-primary-light-7: rgba(168,85,247,0.15);
  --el-color-primary-light-8: rgba(168,85,247,0.1);
  --el-color-primary-light-9: rgba(168,85,247,0.06);
  --el-color-primary-dark-2: #7c3aed;
  --el-disabled-bg-color: rgba(255,255,255,0.02);
  --el-disabled-text-color: #334155;
  --el-disabled-border-color: rgba(255,255,255,0.05);
}

/* ===== Global Dark Overrides for Element Plus ===== */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #a855f7 0%, #7c3aed 100%);
  border: none; transition: all 0.25s;
}
:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #c084fc 0%, #a855f7 100%);
  transform: translateY(-2px); box-shadow: 0 6px 24px rgba(168,85,247,0.3);
}
:deep(.el-input__wrapper) {
  background: rgba(255,255,255,0.05) !important;
  box-shadow: none !important;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px;
}
:deep(.el-input__wrapper:hover) { border-color: rgba(168,85,247,0.2); }
:deep(.el-input__wrapper.is-focus) {
  border-color: #a855f7; box-shadow: 0 0 12px rgba(168,85,247,0.15) !important;
}
:deep(.el-input__inner) { color: #f8fafc; }
:deep(.el-input__inner::placeholder) { color: #475569; }
:deep(.el-select__wrapper) {
  background: rgba(255,255,255,0.05) !important;
  box-shadow: none !important;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px;
}
:deep(.el-dialog) {
  background: rgba(15,23,42,0.95) !important;
  backdrop-filter: blur(24px);
  border: 1px solid rgba(168,85,247,0.1);
  border-radius: 24px !important;
}
:deep(.el-dialog__header) { border-bottom: 1px solid rgba(255,255,255,0.05); }
:deep(.el-dialog__title) { color: #f8fafc !important; font-weight: 700; }
:deep(.el-dialog__footer) { border-top: 1px solid rgba(255,255,255,0.05); }
:deep(.el-card) {
  background: rgba(15,23,42,0.6) !important;
  backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1) !important;
  border-radius: 20px !important;
  box-shadow: none !important;
  color: #94a3b8;
}
:deep(.el-card__header) { border-bottom-color: rgba(255,255,255,0.05) !important; color: #f8fafc; }
:deep(.el-form-item__label) { color: #94a3b8 !important; }
:deep(.el-checkbox__label) { color: #94a3b8; }
:deep(.el-textarea__inner) {
  background: rgba(255,255,255,0.05) !important;
  border: 1px solid rgba(255,255,255,0.08);
  color: #f8fafc; border-radius: 10px;
}
/* ===== Pagination Dark ===== */
:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-button-bg-color: rgba(255,255,255,0.05);
  --el-pagination-button-color: #94a3b8;
  --el-pagination-hover-color: #c084fc;
}
:deep(.el-pagination button) {
  background: rgba(255,255,255,0.05) !important; color: #94a3b8 !important; border: 1px solid rgba(255,255,255,0.08);
}
:deep(.el-pagination button:hover) { background: rgba(168,85,247,0.1) !important; color: #c084fc !important; }
:deep(.el-pagination button:disabled) { background: rgba(255,255,255,0.02) !important; color: #334155 !important; }
:deep(.el-pager li) {
  background: rgba(255,255,255,0.05) !important; color: #94a3b8 !important;
  border: 1px solid rgba(255,255,255,0.08); border-radius: 8px; min-width: 32px;
}
:deep(.el-pager li:hover) { color: #c084fc !important; }
:deep(.el-pager li.is-active) {
  background: linear-gradient(135deg, #a855f7, #7c3aed) !important;
  color: #fff !important; border-color: transparent;
}
:deep(.el-pagination .el-pagination__total) { color: #64748b; }

/* ===== Table Selection & Current Row ===== */
:deep(.el-table__body tr.current-row > td) { background: rgba(168,85,247,0.1) !important; }
:deep(.el-table__body tr.el-table__row--striped td) { background: rgba(255,255,255,0.015) !important; }
:deep(.el-table--striped .el-table__body tr.el-table__row--striped.current-row td) { background: rgba(168,85,247,0.08) !important; }

/* ===== Checkbox Dark ===== */
:deep(.el-checkbox__inner) {
  background: rgba(255,255,255,0.05) !important; border-color: rgba(255,255,255,0.15) !important;
}
:deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: #a855f7 !important; border-color: #a855f7 !important;
}

/* ===== Tag Dark ===== */
:deep(.el-tag) {
  --el-tag-bg-color: rgba(168,85,247,0.1);
  --el-tag-border-color: rgba(168,85,247,0.15);
  --el-tag-text-color: #c084fc;
}
:deep(.el-tag--info) { --el-tag-bg-color: rgba(148,163,184,0.1); --el-tag-border-color: rgba(148,163,184,0.15); --el-tag-text-color: #94a3b8; }
:deep(.el-tag--success) { --el-tag-bg-color: rgba(16,185,129,0.1); --el-tag-border-color: rgba(16,185,129,0.15); --el-tag-text-color: #10b981; }
:deep(.el-tag--warning) { --el-tag-bg-color: rgba(245,158,11,0.1); --el-tag-border-color: rgba(245,158,11,0.15); --el-tag-text-color: #f59e0b; }
:deep(.el-tag--danger) { --el-tag-bg-color: rgba(244,63,94,0.1); --el-tag-border-color: rgba(244,63,94,0.15); --el-tag-text-color: #f43f5e; }

/* ===== Button Default Dark ===== */
:deep(.el-button--default) {
  background: rgba(255,255,255,0.05) !important; color: #94a3b8 !important;
  border: 1px solid rgba(255,255,255,0.1) !important;
}
:deep(.el-button--default:hover) {
  background: rgba(168,85,247,0.08) !important; color: #c084fc !important;
  border-color: rgba(168,85,247,0.2) !important;
}

/* ===== Select Dropdown Dark ===== */
:deep(.el-select__wrapper) {
  background: rgba(255,255,255,0.05) !important;
  box-shadow: none !important;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 10px; color: #f8fafc !important;
}
:deep(.el-select__placeholder) { color: #475569 !important; }
:deep(.el-select__selected-item span) { color: #f8fafc !important; }
:deep(.el-select-dropdown) { background: rgba(15,23,42,0.95) !important; border: 1px solid rgba(168,85,247,0.1) !important; }
:deep(.el-select-dropdown__item) { color: #94a3b8 !important; }
:deep(.el-select-dropdown__item.is-hovering) { background: rgba(168,85,247,0.08) !important; }
:deep(.el-select-dropdown__item.is-selected) { color: #c084fc !important; font-weight: 700; }

/* ===== Drawer Dark ===== */
:deep(.el-drawer) {
  background: rgba(15,23,42,0.95) !important; color: #94a3b8;
}
:deep(.el-drawer__header) { color: #f8fafc !important; border-bottom: 1px solid rgba(255,255,255,0.05); }

/* ===== Tabs Dark ===== */
:deep(.el-tabs__header) { border-bottom-color: rgba(255,255,255,0.05) !important; }
:deep(.el-tabs__item) { color: #64748b !important; }
:deep(.el-tabs__item.is-active) { color: #c084fc !important; }
:deep(.el-tabs__active-bar) { background: linear-gradient(90deg, #a855f7, #22d3ee) !important; }
:deep(.el-tabs__nav-wrap::after) { background: rgba(255,255,255,0.05) !important; }

/* ===== Switch Dark ===== */
:deep(.el-switch) { --el-switch-off-color: rgba(255,255,255,0.1); }

/* ===== Date Picker Dark ===== */
:deep(.el-date-editor .el-input__wrapper) { background: rgba(255,255,255,0.05) !important; }

/* ===== Loading ===== */
:deep(.el-loading-mask) { background: rgba(10,10,10,0.7) !important; }
:deep(.el-descriptions) { --el-descriptions-table-bg-color: transparent; }

/* ===== Popover / Popconfirm Dark ===== */
:deep(.el-popconfirm) { background: rgba(15,23,42,0.95); }
</style>

<!-- Global (unscoped) overrides for teleported popper elements -->
<style>
/* Select dropdown popper (teleports to body) */
.el-select__popper, .el-select-dropdown { background: rgba(15,23,42,0.95) !important; border: 1px solid rgba(168,85,247,0.12) !important; }
.el-select-dropdown__item { color: #94a3b8 !important; }
.el-select-dropdown__item:hover, .el-select-dropdown__item.is-hovering { background: rgba(168,85,247,0.08) !important; }
.el-select-dropdown__item.is-selected { color: #c084fc !important; font-weight: 700; }

/* Popover / Popconfirm popper */
.el-popper, .el-popover { background: rgba(15,23,42,0.95) !important; border-color: rgba(168,85,247,0.1) !important; color: #94a3b8 !important; }
.el-popper__arrow::before { background: rgba(15,23,42,0.95) !important; border-color: rgba(168,85,247,0.1) !important; }
.el-popconfirm__main { color: #f8fafc !important; }

/* Date/Time picker popper */
.el-date-picker, .el-picker__popper { background: rgba(15,23,42,0.95) !important; border-color: rgba(168,85,247,0.1) !important; }
.el-date-table td.available { color: #94a3b8 !important; }
.el-date-table td.today .el-date-table-cell__text { color: #c084fc !important; }
.el-date-table td.current:not(.disabled) .el-date-table-cell__text { background: #a855f7 !important; }
.el-date-range-picker__content { border-color: rgba(255,255,255,0.05) !important; }
.el-date-picker__header-label { color: #f8fafc !important; }
.el-date-picker__header .el-icon { color: #64748b !important; }

/* Dialog overlay */
.el-overlay { background: rgba(0,0,0,0.6) !important; }

/* Message box */
.el-message-box { background: rgba(15,23,42,0.95) !important; border: 1px solid rgba(168,85,247,0.1) !important; }
.el-message-box__title { color: #f8fafc !important; }
.el-message-box__message { color: #94a3b8 !important; }
.el-message-box__header { border-bottom: 1px solid rgba(255,255,255,0.05); }
.el-message-box__btns .el-button--default { background: rgba(255,255,255,0.05) !important; color: #94a3b8 !important; border-color: rgba(255,255,255,0.1) !important; }

/* Color picker */
.el-color-picker__panel { background: rgba(15,23,42,0.95) !important; border-color: rgba(168,85,247,0.1) !important; }
</style>
