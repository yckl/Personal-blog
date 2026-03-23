<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 20px;">
      <h3>Comments</h3>
      <el-select v-model="statusFilter" placeholder="Filter by status" clearable style="width:160px" @change="loadData">
        <el-option label="Pending" value="PENDING" />
        <el-option label="Approved" value="APPROVED" />
        <el-option label="Rejected" value="REJECTED" />
      </el-select>
    </div>
    <el-table :data="comments" stripe v-loading="loading">
      <el-table-column prop="authorName" label="Author" width="140" />
      <el-table-column prop="content" label="Content" min-width="300">
        <template #default="{ row }"><div style="max-height:60px;overflow:hidden;">{{ row.content }}</div></template>
      </el-table-column>
      <el-table-column prop="status" label="Status" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status==='APPROVED'?'success':row.status==='PENDING'?'warning':'danger'" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="Created" width="180" />
      <el-table-column label="Actions" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status!=='APPROVED'" type="success" size="small" text @click="handleApprove(row.id)">Approve</el-button>
          <el-button v-if="row.status!=='REJECTED'" type="warning" size="small" text @click="handleReject(row.id)">Reject</el-button>
          <el-popconfirm title="Delete?" @confirm="handleDelete(row.id)">
            <template #reference><el-button type="danger" size="small" text>Delete</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :total="total" layout="total,prev,pager,next" style="margin-top:20px;justify-content:flex-end;" @current-change="loadData" />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getComments, approveComment, rejectComment, deleteComment } from '../../api/modules'
import { ElMessage } from 'element-plus'
const comments = ref<any[]>([]); const loading = ref(false); const page = ref(1); const total = ref(0); const statusFilter = ref('')
async function loadData() { loading.value=true; try{const r:any=await getComments({page:page.value,size:20,status:statusFilter.value||undefined});comments.value=r.data.records||[];total.value=r.data.total}catch(e:any){ElMessage.error(e.message)}finally{loading.value=false} }
async function handleApprove(id:number){await approveComment(id);ElMessage.success('Approved');loadData()}
async function handleReject(id:number){await rejectComment(id);ElMessage.success('Rejected');loadData()}
async function handleDelete(id:number){await deleteComment(id);ElMessage.success('Deleted');loadData()}
onMounted(loadData)
</script>
