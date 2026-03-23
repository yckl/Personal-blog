<template>
  <div>
    <h3 style="margin-bottom: 20px;">Users &amp; Permissions</h3>
    <el-tabs v-model="activeTab">
      <!-- Admin Users -->
      <el-tab-pane label="Admin Users" name="admins">
        <el-table :data="admins" stripe v-loading="loadingAdmins">
          <el-table-column prop="username" label="Username" min-width="120" />
          <el-table-column prop="nickname" label="Nickname" min-width="120" />
          <el-table-column prop="email" label="Email" min-width="180" />
          <el-table-column label="Roles" min-width="150">
            <template #default="{ row }">
              <el-tag v-for="r in (row.roles||[])" :key="r" size="small" style="margin-right:4px;">{{ r }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="Status" width="100">
            <template #default="{ row }"><el-tag :type="row.status===1?'success':'danger'" size="small">{{ row.status===1?'Active':'Disabled' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="Created" width="180" />
        </el-table>
      </el-tab-pane>

      <!-- Members (Front-end) -->
      <el-tab-pane label="Members" name="members">
        <el-table :data="members" stripe v-loading="loadingMembers">
          <el-table-column prop="email" label="Email" min-width="200" />
          <el-table-column prop="nickname" label="Nickname" min-width="120" />
          <el-table-column prop="tier" label="Tier" width="120">
            <template #default="{ row }"><el-tag :type="row.tier==='PREMIUM'?'warning':'info'" size="small">{{ row.tier }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="tierExpiresAt" label="Expires" width="180" />
          <el-table-column prop="status" label="Status" width="100">
            <template #default="{ row }"><el-tag :type="row.status==='ACTIVE'?'success':'danger'" size="small">{{ row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="lastLoginAt" label="Last Login" width="180" />
          <el-table-column prop="createdAt" label="Joined" width="180" />
        </el-table>
        <el-pagination v-model:current-page="memberPage" :total="memberTotal" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadMembers" />
      </el-tab-pane>

      <!-- Roles -->
      <el-tab-pane label="Roles" name="roles">
        <el-table :data="roles" stripe v-loading="loadingRoles">
          <el-table-column prop="name" label="Role Name" min-width="150" />
          <el-table-column prop="description" label="Description" min-width="250" />
          <el-table-column label="Permissions" min-width="300">
            <template #default="{ row }">
              <el-tag v-for="p in (row.permissions||[]).slice(0,5)" :key="p" size="small" style="margin:2px;">{{ p }}</el-tag>
              <span v-if="(row.permissions||[]).length>5" style="color:#999;font-size:12px;"> +{{ row.permissions.length-5 }} more</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
const activeTab = ref('admins')
const admins = ref<any[]>([]); const loadingAdmins = ref(false)
const members = ref<any[]>([]); const loadingMembers = ref(false); const memberPage = ref(1); const memberTotal = ref(0)
const roles = ref<any[]>([]); const loadingRoles = ref(false)

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
