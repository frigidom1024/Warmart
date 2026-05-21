<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminNoticePage, addNotice, updateNotice, deleteNotice } from '../api/notice'
import type { Notice } from '../api/notice'

const notices = ref<Notice[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({ type: '', page: 1, size: 10 })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = ref<Partial<Notice>>({ title: '', content: '', type: 'notice', status: 1 })

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.type) params.type = query.value.type
    const res = await getAdminNoticePage(params)
    notices.value = res.records
    total.value = res.total
  } catch {} finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  form.value = { title: '', content: '', type: 'notice', status: 1 }
  dialogVisible.value = true
}

function openEditDialog(item: Notice) {
  isEdit.value = true
  form.value = { ...item }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.title) {
    ElMessage.warning('请输入公告标题')
    return
  }
  try {
    if (isEdit.value) {
      await updateNotice(form.value)
      ElMessage.success('更新成功')
    } else {
      await addNotice(form.value)
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch {}
}

async function handleDelete(item: Notice) {
  try {
    await ElMessageBox.confirm(`确定删除公告「${item.title}」吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await deleteNotice(item.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch {}
}

function handlePageChange(page: number) {
  query.value.page = page
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>公告管理</h2>
    </div>
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>系统公告</span>
            <el-select v-model="query.type" placeholder="公告类型" clearable size="small" style="width: 120px; margin-left: 16px;" @change="loadData">
              <el-option label="系统公告" value="notice" />
              <el-option label="活动公告" value="activity" />
              <el-option label="更新日志" value="update" />
            </el-select>
          </div>
          <el-button type="primary" size="small" @click="openAddDialog()">发布公告</el-button>
        </div>
      </template>
      <el-table :data="notices" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="row.type === 'notice' ? '' : row.type === 'activity' ? 'warning' : 'info'" size="small">
              {{ row.type === 'notice' ? '系统公告' : row.type === 'activity' ? '活动公告' : '更新日志' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="发布时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '发布公告'" width="650px">
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 160px">
            <el-option label="系统公告" value="notice" />
            <el-option label="活动公告" value="activity" />
            <el-option label="更新日志" value="update" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入公告内容（支持HTML）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">草稿</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: var(--wa-text-primary, #303133); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
