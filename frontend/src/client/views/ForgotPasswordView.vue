<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { forgotPassword } from '@/api/auth'
import { showToast } from '@/utils/toast'

const router = useRouter()
const email = ref('')
const loading = ref(false)
const sent = ref(false)

async function handleSubmit() {
  if (!email.value.trim()) {
    showToast('请输入邮箱地址', 'warning')
    return
  }
  loading.value = true
  try {
    await forgotPassword(email.value.trim())
    sent.value = true
  } catch { /* handled */ } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="reset-wrapper">
      <el-card class="reset-card" shadow="never">
        <div class="reset-card__header">
          <h1 class="reset-card__title">重置密码</h1>
          <p class="reset-card__desc" v-if="!sent">输入你的邮箱，我们将发送重置链接</p>
          <p class="reset-card__desc" v-else>重置链接已发送，请查收邮件</p>
        </div>
        <div class="reset-card__body">
          <template v-if="!sent">
            <div class="reset-card__field">
              <span class="reset-card__label">邮箱地址</span>
              <input v-model="email" type="email" class="reset-card__input" placeholder="请输入邮箱地址">
            </div>
            <div class="reset-card__actions">
              <div
                class="reset-card__btn-placeholder"
                :class="{ 'reset-card__btn-placeholder--disabled': loading }"
                @click="handleSubmit"
              >{{ loading ? '发送中...' : '发送重置链接' }}</div>
            </div>
          </template>
          <p class="reset-card__back" @click="router.push('/auth')">返回登录</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-bg);
}
.reset-wrapper {
  width: 100%;
  max-width: 400px;
  padding: 24px;
}
.reset-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 53, 0.08);
}
.reset-card__header {
  text-align: center;
  padding: 32px 24px 20px;
}
.reset-card__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 26px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.reset-card__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.reset-card__body {
  padding: 0 24px 32px;
}
.reset-card__field {
  margin-bottom: 24px;
}
.reset-card__label {
  display: block;
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 8px;
  font-weight: 500;
}
.reset-card__input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}
.reset-card__input:focus {
  border-color: var(--wz-orange);
}
.reset-card__input::placeholder {
  color: var(--wz-text-muted);
}
.reset-card__actions {
  margin-top: 20px;
}
.reset-card__btn-placeholder {
  height: 44px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  cursor: pointer;
  font-weight: 500;
  letter-spacing: 1px;
}
.reset-card__back {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--wz-orange);
  cursor: pointer;
}
.reset-card__btn-placeholder--disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
</style>
