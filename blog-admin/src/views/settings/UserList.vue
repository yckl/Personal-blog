<template>
  <div class="user-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">👥 用户与权限</h3>
    </div>

    <el-tabs v-model="activeTab" class="dark-tabs">
      <!-- Admin Users -->
      <el-tab-pane label="管理员用户" name="admins">
        <div class="section-toolbar">
          <el-button type="primary" @click="openCreateUser" size="small">
            <el-icon><Plus /></el-icon> 新建用户
          </el-button>
        </div>
        <div class="table-wrap glass-admin">
          <el-table :data="admins" v-loading="loadingAdmins"
                    :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                    :row-style="{ height: '64px' }"
                    :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
            <el-table-column label="用户" min-width="200">
              <template #default="{ row }">
                <div class="user-cell">
                  <div class="avatar-ring">
                    <el-avatar :size="36" :src="row.avatar">{{ (row.nickname || row.username || '?')[0] }}</el-avatar>
                  </div>
                  <div>
                    <span class="user-name">{{ row.nickname || row.username }}</span>
                    <span class="user-email">{{ row.email }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="角色" min-width="180">
              <template #default="{ row }">
                <span v-for="r in (row.roles || [])" :key="r" :class="['role-badge', roleClass(r)]">{{ r }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <span :class="['status-badge', row.status === 1 ? 'active' : 'disabled']">
                  <span class="status-dot" /> {{ row.status === 1 ? '正常' : '禁用' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="170">
              <template #default="{ row }"><span class="time-text">{{ formatTime(row.createdAt) }}</span></template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openEditUser(row)">编辑</el-button>
                <el-popconfirm title="确定删除该用户？" @confirm="deleteUser(row.id)">
                  <template #reference>
                    <el-button type="danger" link size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Members -->
      <el-tab-pane label="会员" name="members">
        <div class="table-wrap glass-admin">
          <el-table :data="members" v-loading="loadingMembers"
                    :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                    :row-style="{ height: '56px' }"
                    :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
            <el-table-column label="会员" min-width="220">
              <template #default="{ row }">
                <div class="user-cell">
                  <div class="avatar-ring small">
                    <el-avatar :size="30">{{ (row.nickname || row.email || '?')[0] }}</el-avatar>
                  </div>
                  <div>
                    <span class="user-name">{{ row.nickname || row.email }}</span>
                    <span class="user-email">{{ row.email }}</span>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="tier" label="等级" width="120">
              <template #default="{ row }">
                <span :class="['tier-badge', row.tier?.toLowerCase()]">{{ row.tier }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <span :class="['status-badge', row.status?.toLowerCase()]">
                  <span class="status-dot" /> {{ row.status }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="最后登录" width="170">
              <template #default="{ row }"><span class="time-text">{{ formatTime(row.lastLoginAt) }}</span></template>
            </el-table-column>
            <el-table-column label="加入时间" width="170">
              <template #default="{ row }"><span class="time-text">{{ formatTime(row.createdAt) }}</span></template>
            </el-table-column>
          </el-table>
        </div>
        <el-pagination v-model:current-page="memberPage" :total="memberTotal" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadMembers" />
      </el-tab-pane>

      <!-- Roles -->
      <el-tab-pane label="角色" name="roles">
        <div class="section-toolbar">
          <el-button type="primary" @click="openCreateRole" size="small">
            <el-icon><Plus /></el-icon> 新建角色
          </el-button>
        </div>
        <div class="table-wrap glass-admin">
          <el-table :data="roles" v-loading="loadingRoles"
                    :header-cell-style="{ background: 'rgba(168,85,247,0.06)', color: '#94a3b8', borderBottom: '1px solid rgba(255,255,255,0.05)', height: '52px' }"
                    :row-style="{ height: '56px' }"
                    :cell-style="{ borderBottom: '1px solid rgba(255,255,255,0.04)' }">
            <el-table-column prop="name" label="角色名称" min-width="150">
              <template #default="{ row }">
                <span :class="['role-badge', roleClass(row.name)]">{{ row.name }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="roleKey" label="角色标识" width="150" />
            <el-table-column prop="description" label="描述" min-width="250" />
            <el-table-column label="权限" min-width="300">
              <template #default="{ row }">
                <span v-for="p in (row.permissions || []).slice(0, 5)" :key="p" class="perm-tag">{{ p }}</span>
                <span v-if="(row.permissions || []).length > 5" class="more-tag">+{{ row.permissions.length - 5 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openEditRole(row)">编辑</el-button>
                <el-popconfirm title="确定删除该角色？" @confirm="deleteRole(row.id)">
                  <template #reference>
                    <el-button type="danger" link size="small">删除</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- ===== Create / Edit User Dialog ===== -->
    <el-dialog v-model="userDialogVisible" :title="editingUser ? '编辑用户' : '新建用户'" width="480px"
               class="dark-dialog" :close-on-click-modal="false">
      <el-form :model="userForm" label-width="80px" label-position="left">
        <el-form-item label="用户名" required>
          <el-input v-model="userForm.username" placeholder="输入用户名" :disabled="!!editingUser" />
        </el-form-item>
        <el-form-item label="密码" :required="!editingUser">
          <el-input v-model="userForm.password" type="password" show-password
                    :placeholder="editingUser ? '留空则不修改' : '输入密码'" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="userForm.nickname" placeholder="显示昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="邮箱地址" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="userForm.status" style="width:100%">
            <el-option :value="1" label="正常" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="userForm.roleIds">
            <el-checkbox v-for="r in roles" :key="r.id" :value="r.id">{{ r.name }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== Create / Edit Role Dialog ===== -->
    <el-dialog v-model="roleDialogVisible" :title="editingRole ? '编辑角色' : '新建角色'" width="480px"
               class="dark-dialog" :close-on-click-modal="false">
      <el-form :model="roleForm" label-width="80px" label-position="left">
        <el-form-item label="角色名称" required>
          <el-input v-model="roleForm.roleName" placeholder="如：EDITOR" />
        </el-form-item>
        <el-form-item label="角色标识" required>
          <el-input v-model="roleForm.roleKey" placeholder="如：editor" :disabled="!!editingRole" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="roleForm.description" type="textarea" :rows="2" placeholder="角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const activeTab = ref('admins')
const admins = ref<any[]>([]); const loadingAdmins = ref(false)
const members = ref<any[]>([]); const loadingMembers = ref(false); const memberPage = ref(1); const memberTotal = ref(0)
const roles = ref<any[]>([]); const loadingRoles = ref(false)
const saving = ref(false)

// ---- User Dialog ----
const userDialogVisible = ref(false)
const editingUser = ref<any>(null)
const userForm = ref({ username: '', password: '', nickname: '', email: '', status: 1, roleIds: [] as number[] })

function openCreateUser() {
  editingUser.value = null
  userForm.value = { username: '', password: '', nickname: '', email: '', status: 1, roleIds: [] }
  userDialogVisible.value = true
}

function openEditUser(row: any) {
  editingUser.value = row
  // Map role names back to IDs
  const roleIds = roles.value
    .filter(r => (row.roles || []).includes(r.name))
    .map(r => r.id)
  userForm.value = {
    username: row.username,
    password: '',
    nickname: row.nickname || '',
    email: row.email || '',
    status: row.status ?? 1,
    roleIds
  }
  userDialogVisible.value = true
}

async function saveUser() {
  const form = userForm.value
  if (!form.username) { ElMessage.warning('请输入用户名'); return }
  if (!editingUser.value && !form.password) { ElMessage.warning('请输入密码'); return }
  saving.value = true
  try {
    if (editingUser.value) {
      await request.put(`/api/admin/users/${editingUser.value.id}`, {
        password: form.password || undefined,
        nickname: form.nickname,
        email: form.email,
        status: form.status,
        roleIds: form.roleIds
      })
      ElMessage.success('用户已更新')
    } else {
      await request.post('/api/admin/users', form)
      ElMessage.success('用户已创建')
    }
    userDialogVisible.value = false
    loadAdmins()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
  finally { saving.value = false }
}

async function deleteUser(id: number) {
  try {
    await request.delete(`/api/admin/users/${id}`)
    ElMessage.success('用户已删除')
    loadAdmins()
  } catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

// ---- Role Dialog ----
const roleDialogVisible = ref(false)
const editingRole = ref<any>(null)
const roleForm = ref({ roleName: '', roleKey: '', description: '' })

function openCreateRole() {
  editingRole.value = null
  roleForm.value = { roleName: '', roleKey: '', description: '' }
  roleDialogVisible.value = true
}

function openEditRole(row: any) {
  editingRole.value = row
  roleForm.value = {
    roleName: row.name || '',
    roleKey: row.roleKey || '',
    description: row.description || ''
  }
  roleDialogVisible.value = true
}

async function saveRole() {
  const form = roleForm.value
  if (!form.roleName || !form.roleKey) { ElMessage.warning('请填写角色名称和标识'); return }
  saving.value = true
  try {
    if (editingRole.value) {
      await request.put(`/api/admin/roles/${editingRole.value.id}`, form)
      ElMessage.success('角色已更新')
    } else {
      await request.post('/api/admin/roles', form)
      ElMessage.success('角色已创建')
    }
    roleDialogVisible.value = false
    loadRoles()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
  finally { saving.value = false }
}

async function deleteRole(id: number) {
  try {
    await request.delete(`/api/admin/roles/${id}`)
    ElMessage.success('角色已删除')
    loadRoles()
  } catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

// ---- Utilities ----

function formatTime(t: string): string { return t ? t.replace('T', ' ').substring(0, 16) : '' }

function roleClass(role: string): string {
  const map: Record<string, string> = { SUPER_ADMIN: 'super', ADMIN: 'admin', EDITOR: 'editor', AUTHOR: 'author' }
  return map[role] || 'default'
}

// ---- Data Loading ----

async function loadAdmins() {
  loadingAdmins.value = true
  try { const r: any = await request.get('/api/admin/users'); admins.value = r.data || [] }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loadingAdmins.value = false }
}
async function loadMembers() {
  loadingMembers.value = true
  try { const r: any = await request.get('/api/admin/members', { params: { page: memberPage.value, size: 20 } }); members.value = r.data?.records || r.data || []; memberTotal.value = r.data?.total || 0 }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loadingMembers.value = false }
}
async function loadRoles() {
  loadingRoles.value = true
  try { const r: any = await request.get('/api/admin/roles'); roles.value = r.data || [] }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loadingRoles.value = false }
}
onMounted(() => { loadAdmins(); loadMembers(); loadRoles() })
</script>

<style scoped>
.user-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.section-toolbar { display: flex; justify-content: flex-end; margin-bottom: 12px; }
.table-wrap { padding: 0; overflow: hidden; }
.time-text { font-size: 13px; color: #475569; }

/* User cell */
.user-cell { display: flex; align-items: center; gap: 12px; }
.avatar-ring {
  position: relative; padding: 2px; border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #22d3ee);
}
.avatar-ring.small { padding: 1px; }
.avatar-ring :deep(.el-avatar) { border: 2px solid rgba(15,23,42,0.8); }
.user-name { font-weight: 600; color: #f8fafc; font-size: 14px; display: block; }
.user-email { font-size: 12px; color: #475569; }

/* Role badges */
.role-badge {
  display: inline-block; padding: 3px 10px; border-radius: 999px;
  font-size: 11px; font-weight: 700; margin-right: 4px; text-transform: uppercase;
}
.role-badge.super { background: rgba(244,63,94,0.12); color: #f43f5e; }
.role-badge.admin { background: rgba(245,158,11,0.12); color: #f59e0b; }
.role-badge.editor { background: rgba(16,185,129,0.12); color: #10b981; }
.role-badge.author { background: rgba(168,85,247,0.12); color: #a855f7; }
.role-badge.default { background: rgba(148,163,184,0.12); color: #94a3b8; }

/* Status */
.status-badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-badge.active { background: rgba(16,185,129,0.1); color: #10b981; }
.status-badge.active .status-dot { background: #10b981; }
.status-badge.disabled { background: rgba(244,63,94,0.1); color: #f43f5e; }
.status-badge.disabled .status-dot { background: #f43f5e; }

/* Tier */
.tier-badge {
  padding: 3px 10px; border-radius: 999px; font-size: 11px; font-weight: 600;
}
.tier-badge.premium { background: rgba(245,158,11,0.12); color: #f59e0b; }
.tier-badge.free { background: rgba(148,163,184,0.12); color: #94a3b8; }

/* Permission tags */
.perm-tag {
  display: inline-block; padding: 2px 8px; border-radius: 6px; margin: 2px;
  background: rgba(168,85,247,0.08); color: #c084fc; font-size: 11px;
}
.more-tag { color: #475569; font-size: 12px; margin-left: 4px; }

/* Dark tabs */
.dark-tabs :deep(.el-tabs__header) { border-bottom-color: rgba(255,255,255,0.05) !important; }
.dark-tabs :deep(.el-tabs__item) { color: #64748b !important; }
.dark-tabs :deep(.el-tabs__item.is-active) { color: #c084fc !important; }
.dark-tabs :deep(.el-tabs__active-bar) { background: linear-gradient(90deg, #a855f7, #22d3ee) !important; }

/* Dark table */
:deep(.el-table) { background: transparent; --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: transparent; --el-table-border-color: rgba(255,255,255,0.04); color: #94a3b8; }
:deep(.el-table th) { background: rgba(168,85,247,0.04) !important; }
:deep(.el-table td) { border-bottom-color: rgba(255,255,255,0.04) !important; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background: rgba(168,85,247,0.04) !important; }

/* Dark dialog */
:deep(.el-dialog) {
  background: rgba(15,23,42,0.95) !important; backdrop-filter: blur(24px);
  border: 1px solid rgba(168,85,247,0.15); border-radius: 16px;
}
:deep(.el-dialog__header) { border-bottom: 1px solid rgba(255,255,255,0.05); }
:deep(.el-dialog__title) { color: #f8fafc; font-weight: 600; }
:deep(.el-dialog__body) { color: #94a3b8; }
:deep(.el-form-item__label) { color: #94a3b8 !important; }
:deep(.el-input__wrapper) {
  background: rgba(30,41,59,0.8) !important;
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: none !important;
}
:deep(.el-input__inner) { color: #f8fafc !important; }
:deep(.el-textarea__inner) {
  background: rgba(30,41,59,0.8) !important; color: #f8fafc !important;
  border: 1px solid rgba(255,255,255,0.08); box-shadow: none !important;
}
:deep(.el-select .el-input__wrapper) { background: rgba(30,41,59,0.8) !important; }
:deep(.el-checkbox__label) { color: #94a3b8 !important; }
</style>

<style>
/* Global dark dialog styles for UserList which appends to body */
.dark-dialog.el-dialog {
  background: rgba(15,23,42,0.95) !important; backdrop-filter: blur(24px);
  border: 1px solid rgba(168,85,247,0.15); border-radius: 16px;
}
.dark-dialog .el-dialog__header { border-bottom: 1px solid rgba(255,255,255,0.05); }
.dark-dialog .el-dialog__title { color: #f8fafc; font-weight: 600; }
.dark-dialog .el-dialog__body { color: #94a3b8; }
.dark-dialog .el-form-item__label { color: #94a3b8 !important; }
.dark-dialog .el-input__wrapper {
  background: rgba(30,41,59,0.8) !important;
  border: 1px solid rgba(255,255,255,0.08);
  box-shadow: none !important;
}
.dark-dialog .el-input__inner { color: #f8fafc !important; }
.dark-dialog .el-textarea__inner {
  background: rgba(30,41,59,0.8) !important; color: #f8fafc !important;
  border: 1px solid rgba(255,255,255,0.08); box-shadow: none !important;
}
.dark-dialog .el-select .el-input__wrapper { background: rgba(30,41,59,0.8) !important; }
.dark-dialog .el-checkbox__label { color: #94a3b8 !important; }
</style>
