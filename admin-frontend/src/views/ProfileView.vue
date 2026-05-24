<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const userInfo = ref<any>(null)
const loading = ref(false)
const editForm = ref({ nickname: '', phone: '', email: '' })

async function loadUserInfo() {
  loading.value = true
  try {
    const res = await request.get<any, any>('/user/info')
    userInfo.value = res
    editForm.value = { nickname: res.nickname || res.username || '', phone: res.phone || '', email: res.email || '' }
  } catch {} finally { loading.value = false }
}
async function saveProfile() {
  try {
    await request.put<any, any>('/user/info', editForm.value)
    ElMessage.success('保存成功'); await loadUserInfo()
  } catch {}
}

onMounted(loadUserInfo)
</script>

<template>
  <div>
    <div class="page-header"><h2>个人信息</h2></div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card">
          <div class="avatar-section">
            <div class="avatar-placeholder"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg></div>
            <p class="profile-name">{{ userInfo?.nickname || userInfo?.username || '管理员' }}</p>
            <p class="profile-role">超级管理员</p>
          </div>
          <el-divider />
          <div class="profile-meta">
            <div class="meta-item"><span class="meta-label">用户名</span><span class="meta-value">{{ userInfo?.username || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">邮箱</span><span class="meta-value">{{ userInfo?.email || '-' }}</span></div>
            <div class="meta-item"><span class="meta-label">手机</span><span class="meta-value">{{ userInfo?.phone || '-' }}</span></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header><span>编辑资料</span></template>
          <el-form :model="editForm" label-width="80px" class="profile-form">
            <el-form-item label="昵称"><el-input v-model="editForm.nickname" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item>
            <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
            <el-form-item><el-button type="primary" @click="saveProfile" :loading="loading">保存修改</el-button></el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.profile-card { text-align: center; }
.avatar-section { padding: 20px 0 8px; }
.avatar-placeholder { width: 80px; height: 80px; border-radius: 50%; background: #ecf5ff; color: #409eff; display: flex; align-items: center; justify-content: center; margin: 0 auto 12px; }
.profile-name { margin: 0 0 4px; font-size: 18px; font-weight: 600; color: #303133; }
.profile-role { margin: 0; font-size: 13px; color: #909399; }
.profile-meta { text-align: left; padding: 0 8px 16px; }
.meta-item { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f2f2f2; }
.meta-item:last-child { border-bottom: none; }
.meta-label { font-size: 13px; color: #909399; }
.meta-value { font-size: 14px; color: #303133; }
.profile-form { max-width: 400px; margin-top: 16px; }
</style>
