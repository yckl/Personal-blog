<template>
  <div>
    <h3 style="margin-bottom: 20px;">System Log</h3>
    <el-form :inline="true" style="margin-bottom:16px;">
      <el-form-item>
        <el-select v-model="filter.action" placeholder="Action" clearable style="width:160px;">
          <el-option label="LOGIN" value="LOGIN" />
          <el-option label="CREATE" value="CREATE" />
          <el-option label="UPDATE" value="UPDATE" />
          <el-option label="DELETE" value="DELETE" />
          <el-option label="PUBLISH" value="PUBLISH" />
          <el-option label="EXPORT" value="EXPORT" />
          <el-option label="IMPORT" value="IMPORT" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="filter.resource" placeholder="Resource" clearable style="width:160px;">
          <el-option label="article" value="article" />
          <el-option label="comment" value="comment" />
          <el-option label="media" value="media" />
          <el-option label="subscriber" value="subscriber" />
          <el-option label="newsletter" value="newsletter" />
          <el-option label="settings" value="settings" />
          <el-option label="user" value="user" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-date-picker v-model="filter.dateRange" type="daterange" range-separator="to" start-placeholder="Start" end-placeholder="End" value-format="YYYY-MM-DD" style="width:280px;" />
      </el-form-item>
      <el-form-item><el-button type="primary" @click="loadData">Search</el-button></el-form-item>
    </el-form>

    <el-table :data="logs" stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="action" label="Action" width="100">
        <template #default="{ row }">
          <el-tag :type="actionType(row.action)" size="small">{{ row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resource" label="Resource" width="120" />
      <el-table-column prop="resourceId" label="Resource ID" width="100" />
      <el-table-column prop="detail" label="Detail" min-width="250" show-overflow-tooltip />
      <el-table-column prop="userId" label="User ID" width="80" />
      <el-table-column prop="ipAddress" label="IP" width="140" />
      <el-table-column prop="createdAt" label="Time" width="180" />
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadData" />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
const logs = ref<any[]>([]); const loading = ref(false); const page = ref(1); const total = ref(0)
const filter = ref<any>({ action: '', resource: '', dateRange: null })

function actionType(action: string) {
  const map: Record<string, string> = { CREATE: 'success', DELETE: 'danger', UPDATE: 'warning', LOGIN: '', PUBLISH: 'success', EXPORT: 'info', IMPORT: 'info' }
  return map[action] || 'info'
}

async function loadData() {
  loading.value = true
  const params: any = { page: page.value, size: 30 }
  if (filter.value.action) params.action = filter.value.action
  if (filter.value.resource) params.resource = filter.value.resource
  if (filter.value.dateRange?.[0]) { params.startDate = filter.value.dateRange[0]; params.endDate = filter.value.dateRange[1] }
  try { const r: any = await request.get('/api/admin/audit-logs', { params }); logs.value = r.data?.records || r.data || []; total.value = r.data?.total || 0 }
  catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}
onMounted(loadData)
</script>
