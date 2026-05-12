<template>
  <div class="user-info-page">
    <div class="page-header">
      <h2>个人信息</h2>
    </div>

    <div class="info-cards" v-loading="loading">
      <!-- Profile Card -->
      <el-card class="info-card" shadow="hover">
        <template #header>
          <span class="card-title">基本资料</span>
        </template>

        <el-form :model="userForm" label-width="100px" class="user-form">
          <el-form-item label="头像">
            <div class="avatar-wrapper">
              <el-avatar :size="80" :src="avatarUrl">
                <el-icon :size="36"><UserFilled /></el-icon>
              </el-avatar>
              <el-button size="small" class="upload-btn" @click="triggerUpload">
                更换头像
              </el-button>
              <input
                ref="fileInputRef"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleAvatarChange"
              />
            </div>
          </el-form-item>

          <el-form-item label="用户名">
            <el-input v-model="userForm.username" disabled style="max-width: 360px" />
          </el-form-item>

          <el-form-item label="昵称">
            <el-input v-model="userForm.nickname" placeholder="请输入昵称" style="max-width: 360px" />
          </el-form-item>

          <el-form-item label="邮箱">
            <el-input v-model="userForm.email" disabled style="max-width: 360px" />
          </el-form-item>

          <el-form-item label="手机号">
            <el-input v-model="userForm.phone" placeholder="请输入手机号" style="max-width: 360px" />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleUpdateInfo" :loading="saving">
              保存
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Password Card -->
      <el-card class="info-card" shadow="hover" style="margin-top: 20px">
        <template #header>
          <span class="card-title">修改密码</span>
        </template>

        <el-form :model="passwordForm" label-width="120px" class="user-form">
          <el-form-item label="原密码">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入原密码"
              style="max-width: 360px"
            />
          </el-form-item>

          <el-form-item label="新密码">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码"
              style="max-width: 360px"
            />
          </el-form-item>

          <el-form-item label="确认新密码">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
              style="max-width: 360px"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleUpdatePassword" :loading="savingPassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { getUserInfo, updateUserInfo, updatePassword } from '@/api/user'

const loading = ref(false)
const saving = ref(false)
const savingPassword = ref(false)
const fileInputRef = ref(null)

const userForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarUrl = computed(() => {
  return userForm.avatar || ''
})

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await getUserInfo()
    const data = res.data || res
    userForm.username = data.username || ''
    userForm.nickname = data.nickname || ''
    userForm.email = data.email || ''
    userForm.phone = data.phone || ''
    userForm.avatar = data.avatar || ''
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const triggerUpload = () => {
  fileInputRef.value?.click()
}

const handleAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  // Preview locally first
  const reader = new FileReader()
  reader.onload = (ev) => {
    userForm.avatar = ev.target.result
  }
  reader.readAsDataURL(file)
}

const handleUpdateInfo = async () => {
  saving.value = true
  try {
    const payload = {
      nickname: userForm.nickname,
      phone: userForm.phone,
      avatar: userForm.avatar
    }
    const res = await updateUserInfo(payload)
    if (res.code === 200) {
      ElMessage.success('个人信息已更新')
    }
  } catch {
    // Error handled by interceptor
  } finally {
    saving.value = false
  }
}

const handleUpdatePassword = async () => {
  const { oldPassword, newPassword, confirmPassword } = passwordForm

  if (!oldPassword || !newPassword || !confirmPassword) {
    ElMessage.warning('请填写所有密码字段')
    return
  }

  if (newPassword !== confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }

  if (newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于6位')
    return
  }

  savingPassword.value = true
  try {
    const res = await updatePassword({
      oldPassword,
      newPassword
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }
  } catch {
    // Error handled by interceptor
  } finally {
    savingPassword.value = false
  }
}

onMounted(() => {
  fetchUserInfo()
})
</script>

<style scoped>
.user-info-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.info-card {
  border-radius: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-form {
  margin-top: 8px;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 16px;
}

.upload-btn {
  flex-shrink: 0;
}
</style>
