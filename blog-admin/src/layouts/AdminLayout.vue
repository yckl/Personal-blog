<template>
  <el-container style="height: 100vh">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '220px'" style="transition: width 0.3s; background: #1d1e2c;">
      <div class="logo-area" @click="$router.push('/dashboard')">
        <el-icon :size="24" color="#409eff"><EditPen /></el-icon>
        <span v-if="!isCollapse" class="logo-text">{{ $t('admin.title') }}</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        background-color="#1d1e2c"
        text-color="#a3a6b4"
        active-text-color="#409eff"
        router
        :collapse-transition="false"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>{{ $t('admin.menu.dashboard') }}</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:view')" index="/articles">
          <el-icon><Document /></el-icon>
          <template #title>{{ $t('admin.menu.articles') }}</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:edit')" index="/categories">
          <el-icon><FolderOpened /></el-icon>
          <template #title>{{ $t('admin.menu.categories') }}</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:edit')" index="/tags">
          <el-icon><PriceTag /></el-icon>
          <template #title>{{ $t('admin.menu.tags') }}</template>
        </el-menu-item>
        <el-menu-item v-if="can('article:edit')" index="/series">
          <el-icon><Collection /></el-icon>
          <template #title>{{ $t('admin.menu.series') }}</template>
        </el-menu-item>
        <el-menu-item v-if="can('comment:review')" index="/comments">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>{{ $t('admin.menu.comments') }}</template>
        </el-menu-item>
        <el-menu-item v-if="can('media:upload')" index="/media">
          <el-icon><Picture /></el-icon>
          <template #title>{{ $t('admin.menu.media') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/subscribers">
          <el-icon><Message /></el-icon>
          <template #title>{{ $t('admin.menu.subscribers') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/newsletter">
          <el-icon><Promotion /></el-icon>
          <template #title>{{ $t('admin.menu.newsletter') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/products">
          <el-icon><Goods /></el-icon>
          <template #title>{{ $t('admin.menu.products') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/seo">
          <el-icon><Search /></el-icon>
          <template #title>{{ $t('admin.menu.seo') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/menus">
          <el-icon><Menu /></el-icon>
          <template #title>{{ $t('admin.menu.menus') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/settings">
          <el-icon><Setting /></el-icon>
          <template #title>{{ $t('admin.menu.settings') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/users">
          <el-icon><User /></el-icon>
          <template #title>{{ $t('admin.menu.users') }}</template>
        </el-menu-item>
        <el-menu-item v-if="isAdminUser" index="/system-log">
          <el-icon><List /></el-icon>
          <template #title>{{ $t('admin.menu.systemLog') }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- Main Content -->
    <el-container>
      <el-header style="background: #fff; border-bottom: 1px solid #eee; display: flex; align-items: center; justify-content: space-between; padding: 0 20px;">
        <div style="display: flex; align-items: center; gap: 12px;">
          <el-icon :size="20" style="cursor: pointer" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">{{ $t('admin.header.home') }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.name }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div style="display: flex; align-items: center; gap: 16px;">
          <LanguageSwitcher />
          <el-tag v-if="primaryRole" size="small" :type="roleTagType" effect="plain">{{ primaryRole }}</el-tag>
          <span style="font-size: 14px; color: #666;">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
          <el-dropdown @command="handleCommand">
            <el-avatar :size="32" :src="userStore.userInfo?.avatar">
              {{ (userStore.userInfo?.nickname || 'A')[0] }}
            </el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">{{ $t('admin.header.logout') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main style="padding: 20px; overflow-y: auto;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '../stores/user'
import LanguageSwitcher from '../components/LanguageSwitcher.vue'

const { t } = useI18n()
const userStore = useUserStore()
const router = useRouter()
const isCollapse = ref(false)

const isAdminUser = computed(() => userStore.isAdmin())

const primaryRole = computed(() => {
  if (userStore.hasRole('SUPER_ADMIN')) return t('admin.role.superAdmin')
  if (userStore.hasRole('ADMIN')) return t('admin.role.admin')
  if (userStore.hasRole('EDITOR')) return t('admin.role.editor')
  if (userStore.hasRole('AUTHOR')) return t('admin.role.author')
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
  try {
    await userStore.fetchUser()
  } catch {
    router.push('/login')
  }
})

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.05);
}
.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}
</style>
