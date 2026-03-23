<template>
  <div>
    <h3 style="margin-bottom: 20px;">Subscribers</h3>
    <el-table :data="subscribers" stripe v-loading="loading">
      <el-table-column prop="email" label="Email" />
      <el-table-column prop="name" label="Name" />
      <el-table-column prop="status" label="Status" width="120">
        <template #default="{ row }"><el-tag :type="row.status==='ACTIVE'?'success':'info'" size="small">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="subscribedAt" label="Subscribed At" width="180" />
      <el-table-column label="Actions" width="100">
        <template #default="{ row }">
          <el-popconfirm title="Remove subscriber?" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" size="small" text>Remove</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next" style="margin-top:20px;" @current-change="loadData" />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSubscribers, deleteSubscriber } from '../../api/modules'
import { ElMessage } from 'element-plus'
const subscribers = ref<any[]>([]); const loading = ref(false); const page = ref(1); const total = ref(0)
async function loadData() { loading.value=true; try{const r:any=await getSubscribers({page:page.value,size:20});subscribers.value=r.data.records||[];total.value=r.data.total}catch(e:any){ElMessage.error(e.message)}finally{loading.value=false} }
async function handleDelete(id:number) { await deleteSubscriber(id); ElMessage.success('Removed'); loadData() }
onMounted(loadData)
</script>
