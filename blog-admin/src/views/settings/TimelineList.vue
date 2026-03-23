<template>
  <div class="timeline-page">
    <div class="page-toolbar glass-admin">
      <h3 class="toolbar-title">⏳ 旅程时间线</h3>
      <el-button type="primary" @click="openForm()">➕ 新增事件</el-button>
    </div>

    <div class="table-wrap glass-admin">
      <el-table :data="timelines" v-loading="loading">
        <el-table-column prop="year" label="年份" width="100">
          <template #default="{ row }">
            <span class="year-badge">{{ row.year }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" width="200" />
        <el-table-column prop="description" label="详细内容" min-width="300" />
        <el-table-column prop="isVisible" label="显示状态" width="100">
          <template #default="{ row }">
            <el-switch v-model="row.isVisible" @change="toggleVisible(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确定删除吗？" @confirm="handleDelete(row)">
              <template #reference><el-button link type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑事件' : '新增事件'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="年份" prop="year">
          <el-input v-model="form.year" placeholder="例如: 2024" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="主要事件标题" />
        </el-form-item>
        <el-form-item label="内容说明">
          <el-input type="textarea" v-model="form.description" :rows="4" />
        </el-form-item>
        <el-form-item label="排序权重" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'

const timelines = ref<any[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive<any>({ id: null, year: '', title: '', description: '', sortOrder: 0, isVisible: true })
const rules = {
  year: [{ required: true, message: '请输入年份', trigger: 'blur' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const r: any = await request.get('/api/admin/timeline')
    timelines.value = r.data
  } catch (e: any) { ElMessage.error(e.message) }
  finally { loading.value = false }
}

function openForm(row?: any) {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: null, year: '', title: '', description: '', sortOrder: timelines.value.length * 10, isVisible: true })
  }
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return
    submitting.value = true
    try {
      if (form.id) {
        await request.put(`/api/admin/timeline/${form.id}`, form)
        ElMessage.success('更新成功')
      } else {
        await request.post('/api/admin/timeline', form)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e: any) { ElMessage.error(e.message) }
    finally { submitting.value = false }
  })
}

async function handleDelete(row: any) {
  try {
    await request.delete(`/api/admin/timeline/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (e: any) { ElMessage.error(e.message) }
}

async function toggleVisible(row: any) {
  try {
    await request.put(`/api/admin/timeline/${row.id}`, row)
    ElMessage.success('状态已更新')
  } catch (e: any) {
    row.isVisible = !row.isVisible
    ElMessage.error(e.message)
  }
}

onMounted(loadData)
</script>

<style scoped>
.timeline-page { padding: 4px; }
.glass-admin {
  background: rgba(15,23,42,0.6); backdrop-filter: blur(16px);
  border: 1px solid rgba(168,85,247,0.1); border-radius: 16px; padding: 20px;
}
.page-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.toolbar-title { font-size: 18px; font-weight: 700; color: #f8fafc; margin: 0; }
.table-wrap { padding: 0; overflow: hidden; }

.year-badge {
  display: inline-block; padding: 4px 10px; border-radius: 6px;
  background: rgba(34,211,238,0.15); color: #22d3ee;
  font-family: 'Inter', sans-serif; font-size: 13px; font-weight: 700;
}
</style>
