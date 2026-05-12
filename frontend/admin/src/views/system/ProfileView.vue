<template>
  <div class="profile-view">
    <div class="page-header">
      <h3>个人信息</h3>
    </div>

    <!-- User Info Card -->
    <el-card shadow="never" style="margin-bottom: 20px">
      <template #header>
        <span style="font-weight: 600">基本信息</span>
      </template>
      <el-form
        ref="infoFormRef"
        :model="infoForm"
        :rules="infoRules"
        label-width="100px"
        label-position="left"
        style="max-width: 480px"
      >
        <el-form-item label="用户名">
          <el-input v-model="infoForm.username" disabled />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="infoForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="infoForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="头像" prop="avatar">
          <el-input v-model="infoForm.avatar" placeholder="请输入头像 URL" />
          <div v-if="infoForm.avatar" style="margin-top: 8px">
            <el-image
              :src="infoForm.avatar"
              style="width: 80px; height: 80px; border-radius: 4px"
              fit="cover"
              :preview-src-list="[infoForm.avatar]"
              preview-teleported
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingInfo" @click="handleSaveInfo">
            保存信息
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Password Change Card -->
    <el-card shadow="never">
      <template #header>
        <span style="font-weight: 600">修改密码</span>
      </template>
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="120px"
        label-position="left"
        style="max-width: 480px"
      >
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            show-password
            placeholder="请输入旧密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPassword" @click="handleSavePassword">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const userStore = useUserStore()
const infoFormRef = ref(null)
const passwordFormRef = ref(null)
const savingInfo = ref(false)
const savingPassword = ref(false)

const infoForm = reactive({
  username: '',
  email: '',
  phone: '',
  avatar: ''
})

const infoRules = {
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }]
}

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const loadUserInfo = () => {
  const info = userStore.userInfo
  if (info) {
    infoForm.username = info.username || ''
    infoForm.email = info.email || ''
    infoForm.phone = info.phone || ''
    infoForm.avatar = info.avatar || ''
  }
}

const handleSaveInfo = async () => {
  const valid = await infoFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingInfo.value = true
  try {
    await request.put('/user/profile/update', {
      email: infoForm.email,
      phone: infoForm.phone,
      avatar: infoForm.avatar
    })
    ElMessage.success('信息已更新')
    // Refresh user info in store
    const res = await request.get('/user/profile')
    if (res.data) {
      userStore.setUserInfo(res.data)
    }
  } catch {
    // Error handled by interceptor
  } finally {
    savingInfo.value = false
  }
}

const handleSavePassword = async () => {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingPassword.value = true
  try {
    await request.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch {
    // Error handled by interceptor
  } finally {
    savingPassword.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}
</style>
