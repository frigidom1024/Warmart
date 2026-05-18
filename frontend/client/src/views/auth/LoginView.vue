<template>
  <div class="wz-login">
    <div class="wz-login__bg">
      <div class="wz-login__shape wz-login__shape--1" />
      <div class="wz-login__shape wz-login__shape--2" />
    </div>
    <router-link to="/" class="wz-login__back">
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><line x1="19" y1="12" x2="5" y2="12"/><polyline points="12 19 5 12 12 5"/></svg>
      返回首页
    </router-link>
    <div class="wz-login__card">
      <div class="wz-login__header">
        <div class="wz-login__logo">
          <span class="wz-login__logo-mark">暖</span>
        </div>
        <h1 class="wz-login__title">欢迎回来</h1>
        <p class="wz-login__subtitle">登录您的账户，继续温暖的购物之旅</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="wz-login__form"
        @keyup.enter="handleLogin"
        size="large"
      >
        <el-form-item prop="username">
          <div class="wz-input-group">
            <svg class="wz-input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
            </svg>
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="null"
            />
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="wz-input-group">
            <svg class="wz-input-icon" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              :prefix-icon="null"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="remember" class="wz-login__checkbox">记住密码</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="wz-login__submit"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="wz-login__footer">
        <router-link to="/auth/register">还没有账户？立即注册</router-link>
        <router-link to="/auth/forgot-password">忘记密码</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref(null)
const loading = ref(false)
const remember = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ]
}

onMounted(() => {
  const saved = localStorage.getItem('rememberedUsername')
  if (saved) {
    form.username = saved
    remember.value = true
  }
})

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({ username: form.username, password: form.password })
    const data = res.data || res
    store.setToken(data.token || data.accessToken, data.refreshToken || '')

    if (remember.value) {
      localStorage.setItem('rememberedUsername', form.username)
    } else {
      localStorage.removeItem('rememberedUsername')
    }

    ElMessage.success({ message: '登录成功', duration: 1500 })
    const redirect = route.query.redirect || '/'
    router.push(redirect)
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wz-login {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-bg);
  padding: 24px;
  position: relative;
  overflow: hidden;
}
.wz-login__bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.wz-login__shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
}
.wz-login__shape--1 {
  width: 500px; height: 500px;
  background: rgba(192, 102, 74, 0.1);
  top: -200px; right: -100px;
}
.wz-login__shape--2 {
  width: 400px; height: 400px;
  background: rgba(212, 163, 115, 0.08);
  bottom: -150px; left: -100px;
}

.wz-login__back {
  position: fixed;
  top: 24px;
  left: 24px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--wz-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
  z-index: 10;
}
.wz-login__back:hover {
  color: var(--wz-accent);
}

.wz-login__card {
  width: 100%;
  max-width: 420px;
  background: var(--wz-bg-card);
  border-radius: var(--wz-radius-lg);
  padding: 48px 40px 40px;
  box-shadow: var(--wz-shadow-lg);
  position: relative;
  z-index: 2;
  animation: card-enter 0.6s ease-out;
}
@keyframes card-enter {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.wz-login__header {
  text-align: center;
  margin-bottom: 36px;
}
.wz-login__logo {
  margin-bottom: 20px;
}
.wz-login__logo-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: var(--wz-accent);
  color: white;
  border-radius: 14px;
  font-size: 24px;
  font-weight: 700;
  font-family: var(--wz-font-display);
}
.wz-login__title {
  font-family: var(--wz-font-display);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin: 0 0 8px;
}
.wz-login__subtitle {
  font-size: 14px;
  color: var(--wz-text-secondary);
  margin: 0;
}

/* Form */
.wz-login__form {
  margin-bottom: 24px;
}
.wz-input-group {
  position: relative;
  width: 100%;
}
.wz-input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--wz-text-muted);
  z-index: 2;
  pointer-events: none;
}
.wz-login__form :deep(.el-input__wrapper) {
  padding-left: 42px;
  border-radius: 12px;
  box-shadow: 0 0 0 1.5px var(--wz-border-light) !important;
  background: var(--wz-bg);
  transition: all 0.25s ease;
}
.wz-login__form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1.5px var(--wz-border) !important;
}
.wz-login__form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1.5px var(--wz-accent), 0 0 0 4px var(--wz-accent-light) !important;
  background: var(--wz-bg-card);
}
.wz-login__form :deep(.el-input__inner) {
  height: 48px;
  font-size: 15px;
}
.wz-login__checkbox {
  font-size: 13px;
  color: var(--wz-text-secondary);
}
.wz-login__checkbox :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background-color: var(--wz-accent);
  border-color: var(--wz-accent);
}
.wz-login__submit {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  background: var(--wz-accent);
  border-color: var(--wz-accent);
  transition: all 0.3s ease;
}
.wz-login__submit:hover {
  background: var(--wz-accent-hover);
  border-color: var(--wz-accent-hover);
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(192, 102, 74, 0.3);
}

.wz-login__footer {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
}
.wz-login__footer a {
  color: var(--wz-text-secondary);
  text-decoration: none;
  transition: color 0.2s;
}
.wz-login__footer a:hover {
  color: var(--wz-accent);
}
</style>
