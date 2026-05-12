<template>
  <div class="forgot-container">
    <el-card class="forgot-card" shadow="always">
      <template #header>
        <h2 class="forgot-title">忘记密码</h2>
      </template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @keyup.enter="handleSubmit"
      >
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入注册时使用的邮箱"
            :prefix-icon="Message"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            style="width: 100%"
            @click="handleSubmit"
          >
            发送重置链接
          </el-button>
        </el-form-item>
      </el-form>
      <div class="forgot-links">
        <router-link to="/auth/login">返回登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Message } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { forgotPassword } from '@/api/auth'

const formRef = ref(null)
const loading = ref(false)

const form = ref({
  email: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await forgotPassword(form.value.email)
    ElMessage.success('重置链接已发送，请查收邮件')
  } catch {
    // Error is already handled by request interceptor (ElMessage.error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.forgot-card {
  width: 400px;
}

.forgot-title {
  text-align: center;
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.forgot-links {
  text-align: center;
  font-size: 14px;
}

.forgot-links a {
  color: #409eff;
  text-decoration: none;
}

.forgot-links a:hover {
  text-decoration: underline;
}
</style>
