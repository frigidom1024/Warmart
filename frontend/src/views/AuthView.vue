<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login as loginApi, register as registerApi, sendRegisterCode } from '@/api/auth'
import { getUserInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { showToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const mode = computed(() => (route.query.mode === 'register' ? 'register' : 'login'))
const isLogin = computed(() => mode.value === 'login')

const loginForm = ref({ account: '', password: '', remember: false })
const registerForm = ref({ email: '', password: '', confirm: '', code: '' })
const showPassword = ref(false)
const loading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

async function handleSendRegisterCode() {
  if (!registerForm.value.email) {
    showToast('请先输入邮箱', 'warning')
    return
  }
  codeSending.value = true
  try {
    await sendRegisterCode(registerForm.value.email)
    showToast('验证码已发送', 'success')
    startCountdown()
  } catch {
    // handled by interceptor
  } finally {
    codeSending.value = false
  }
}

function startCountdown() {
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value--
    if (codeCountdown.value <= 0) {
      if (countdownTimer) clearInterval(countdownTimer)
    }
  }, 1000)
}

function switchMode(m: 'login' | 'register') {
  router.replace({ query: { mode: m === 'login' ? undefined : 'register' } })
}

async function handleLogin() {
  if (!loginForm.value.account || !loginForm.value.password) return
  loading.value = true
  try {
    const res = await loginApi({
      username: loginForm.value.account,
      password: loginForm.value.password
    })
    userStore.setToken(res.accessToken, res.refreshToken)
    const userInfo = await getUserInfo()
    userStore.setUserInfo(userInfo)
    router.push((route.query.redirect as string) || '/')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.value.email || !registerForm.value.password) return
  if (registerForm.value.password !== registerForm.value.confirm) {
    showToast('两次输入的密码不一致', 'warning')
    return
  }
  loading.value = true
  try {
    await registerApi({
      username: registerForm.value.email,
      password: registerForm.value.password,
      nickname: registerForm.value.email.split('@')[0],
      email: registerForm.value.email,
      code: registerForm.value.code
    })
    showToast('注册成功，请登录', 'success')
    switchMode('login')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleSubmit() {
  if (isLogin.value) handleLogin()
  else handleRegister()
}
</script>

<template>
  <div class="page-container">
    <div class="auth-wrap">
      <div class="auth-inner">
        <div class="auth-split">
          <!-- Left: Brand Panel -->
          <div class="auth-brand">
            <img
              src="https://picsum.photos/id/42/700/800"
              alt=""
              class="auth-brand__img"
            />
            <div class="auth-brand__overlay" />
            <div class="auth-brand__content">
              <div class="auth-brand__logo">
                <span class="auth-brand__logo-icon">暖</span>
                <span class="auth-brand__logo-name">Warmart</span>
              </div>
              <h2 class="auth-brand__headline">温暖你的生活</h2>
              <p class="auth-brand__desc">
                精选全球好物，一站式轻松购齐，让每一次购物都成为温暖的体验
              </p>

              <div class="auth-brand__features">
                <div
                  v-for="(feat, i) in [
                    { icon: 'check-badge', label: '正品直供·品质保障', desc: '全球严选，100%正品承诺' },
                    { icon: 'truck', label: '极速发货·准时送达', desc: '下单即发，次日达覆盖全国' },
                    { icon: 'heart', label: '7天退换·无忧购物', desc: '不满意随时退，运费我们承担' },
                    { icon: 'star', label: '会员专享·积分加倍', desc: '开通会员享专属价+双倍积分' },
                  ]"
                  :key="i"
                  class="auth-brand__feature"
                >
                  <div class="auth-brand__feature-icon">
                    <svg v-if="feat.icon === 'check-badge'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s-8-4.5-8-11.8A8 8 0 0 1 12 2a8 8 0 0 1 8 8.2c0 7.3-8 11.8-8 11.8z"/><path d="m9 12 2 2 4-4"/></svg>
                    <svg v-else-if="feat.icon === 'truck'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/><polyline points="17 6 23 6 23 12"/></svg>
                    <svg v-else-if="feat.icon === 'heart'" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                    <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 1 0 10 10h-10V2z"/><path d="M22 12h-5.5A4.5 4.5 0 0 1 12 7.5V2"/></svg>
                  </div>
                  <div>
                    <div class="auth-brand__feature-title">{{ feat.label }}</div>
                    <div class="auth-brand__feature-desc">{{ feat.desc }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Right: Auth Form -->
          <div class="auth-form">
            <!-- Tabs -->
            <div class="auth-form__tabs">
              <button
                class="auth-form__tab"
                :class="{ 'auth-form__tab--active': isLogin }"
                @click="switchMode('login')"
              >登录</button>
              <button
                class="auth-form__tab"
                :class="{ 'auth-form__tab--active': !isLogin }"
                @click="switchMode('register')"
              >注册</button>
            </div>

            <!-- Login -->
            <form v-if="isLogin" class="auth-form__body" @submit.prevent="handleSubmit">
              <h2 class="auth-form__title">欢迎回来</h2>
              <p class="auth-form__desc">登录你的 Warmart 账号，继续温暖之旅</p>

              <div class="auth-form__field">
                <label class="auth-form__label">邮箱</label>
                <div class="auth-form__input-wrap">
                  <svg class="auth-form__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
                  <input
                    v-model="loginForm.account"
                    type="text"
                    class="auth-form__input"
                    placeholder="请输入邮箱"
                  />
                </div>
              </div>

              <div class="auth-form__field">
                <label class="auth-form__label">密码</label>
                <div class="auth-form__input-wrap">
                  <svg class="auth-form__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  <input
                    v-model="loginForm.password"
                    :type="showPassword ? 'text' : 'password'"
                    class="auth-form__input"
                    placeholder="请输入密码"
                  />
                  <button
                    type="button"
                    class="auth-form__input-toggle"
                    @click="showPassword = !showPassword"
                    tabindex="-1"
                  >
                    <svg v-if="showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  </button>
                </div>
              </div>

              <div class="auth-form__row">
                <label class="auth-form__checkbox">
                  <input v-model="loginForm.remember" type="checkbox" />
                  <span>记住我</span>
                </label>
                <router-link to="/auth/forgot-password" class="auth-form__link">忘记密码？</router-link>
              </div>

              <button type="submit" class="auth-form__btn" :disabled="loading">
                <span v-if="loading" class="auth-form__btn-loading">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="auth-form__spinner"><circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-dashoffset="32"/></svg>
                  登录中...
                </span>
                <span v-else>登录</span>
              </button>

              <div class="auth-form__divider">
                <span>其他方式</span>
              </div>

              <div class="auth-form__social">
                <button type="button" class="auth-form__social-btn">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18z"/></svg>
                  微信
                </button>
                <button type="button" class="auth-form__social-btn">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor"><path d="M16.076,13.732c0.862-1.494,1.55-3.196,2.003-5.045h-4.731V6.988h5.795V6.04h-5.795V3.207h-2.365 c-0.415,0-0.415,0.408-0.415,0.408V6.04H4.707v0.948h5.861v1.699H5.729v0.948h9.386c-0.343,1.18-0.805,2.288-1.352,3.294 c-3.045-1.002-6.295-1.814-8.337-1.314c-1.305,0.321-2.146,0.893-2.641,1.493c-2.267,2.751-0.641,6.929,4.147,6.929 c2.831,0,5.559-1.574,7.673-4.168C17.758,17.381,24,19.976,24,19.976v0.157c0,2.117-1.719,3.833-3.843,3.833H3.845 C1.72,23.966,0,22.249,0,20.132V3.868C0,1.75,1.72,0.034,3.845,0.034h16.312C22.281,0.034,24,1.75,24,3.868v12.409 c0,0-0.784-0.062-4.24-1.216C18.8,14.74,17.511,14.25,16.076,13.732z M5.834,13.034c-0.6,0.059-1.725,0.324-2.341,0.866 c-1.845,1.604-0.741,4.537,2.993,4.537c2.17,0,4.339-1.384,6.042-3.599C10.104,13.659,8.052,12.816,5.834,13.034z"/></svg>
                  支付宝
                </button>
              </div>

              <p class="auth-form__footnote">
                还没有账号？
                <span class="auth-form__link" @click="switchMode('register')">立即注册</span>
              </p>
            </form>

            <!-- Register -->
            <form v-else class="auth-form__body" @submit.prevent="handleSubmit">
              <h2 class="auth-form__title">创建账号</h2>
              <p class="auth-form__desc">加入 Warmart，开启温暖购物之旅</p>

              <div class="auth-form__field">
                <label class="auth-form__label">邮箱</label>
                <div class="auth-form__input-wrap">
                  <svg class="auth-form__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
                  <input v-model="registerForm.email" type="email" class="auth-form__input" placeholder="请输入邮箱" />
                </div>
              </div>
              <div class="auth-form__field">
                <label class="auth-form__label">密码</label>
                <div class="auth-form__input-wrap">
                  <svg class="auth-form__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  <input v-model="registerForm.password" :type="showPassword ? 'text' : 'password'" class="auth-form__input" placeholder="请设置密码" />
                  <button type="button" class="auth-form__input-toggle" @click="showPassword = !showPassword" tabindex="-1">
                    <svg v-if="showPassword" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                    <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                  </button>
                </div>
              </div>
              <div class="auth-form__field">
                <label class="auth-form__label">确认密码</label>
                <div class="auth-form__input-wrap">
                  <svg class="auth-form__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
                  <input v-model="registerForm.confirm" :type="showPassword ? 'text' : 'password'" class="auth-form__input" placeholder="请再次输入密码" />
                </div>
              </div>

              <div class="auth-form__field">
                <label class="auth-form__label">验证码</label>
                <div class="auth-form__code-wrap">
                  <div class="auth-form__input-wrap" style="flex:1">
                    <svg class="auth-form__input-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/><path d="M9 21V9"/></svg>
                    <input v-model="registerForm.code" type="text" class="auth-form__input" placeholder="请输入验证码" maxlength="6" />
                  </div>
                  <button
                    type="button"
                    class="auth-form__code-btn"
                    :disabled="codeSending || codeCountdown > 0"
                    @click="handleSendRegisterCode"
                  >{{ codeCountdown > 0 ? `${codeCountdown}s` : '获取验证码' }}</button>
                </div>
              </div>

              <button type="submit" class="auth-form__btn" :disabled="loading">
                <span v-if="loading" class="auth-form__btn-loading">
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" class="auth-form__spinner"><circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-dashoffset="32"/></svg>
                  注册中...
                </span>
                <span v-else>注册</span>
              </button>

              <p class="auth-form__footnote">
                已有账号？
                <span class="auth-form__link" @click="switchMode('login')">立即登录</span>
              </p>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-bg);
}

.auth-wrap {
  width: 100%;
  max-width: 960px;
  padding: 20px;
}

.auth-inner {
  background: var(--wz-bg-card);
  border-radius: 40px;
  overflow: hidden;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--wz-border);
}

.auth-split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 680px;
}

/* ==================== Brand Side ==================== */
.auth-brand {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  margin: 12px;
  border-radius: 28px;
  background: var(--wz-bg);
}

.auth-brand__img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.auth-brand__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    145deg,
    rgba(15, 15, 17, 0.92) 0%,
    rgba(15, 15, 17, 0.55) 50%,
    rgba(15, 15, 17, 0.15) 100%
  );
}

.auth-brand__content {
  position: relative;
  z-index: 2;
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
}

.auth-brand__logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.auth-brand__logo-icon {
  font-family: 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-orange);
  line-height: 1;
}

.auth-brand__logo-name {
  font-family: 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.05em;
}

.auth-brand__headline {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  line-height: 1.25;
  margin-bottom: 10px;
}

.auth-brand__desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.6;
  margin-bottom: 32px;
  max-width: 320px;
}

.auth-brand__features {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.auth-brand__feature {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.auth-brand__feature-icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: rgba(255, 107, 53, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--wz-orange);
}

.auth-brand__feature-title {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  line-height: 1.3;
  margin-bottom: 2px;
}

.auth-brand__feature-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  line-height: 1.4;
}

/* ==================== Form Side ==================== */
.auth-form {
  display: flex;
  flex-direction: column;
}

.auth-form__tabs {
  display: flex;
  border-bottom: 1px solid var(--wz-border);
}

.auth-form__tab {
  flex: 1;
  height: 52px;
  background: none;
  border: none;
  font-family: var(--wz-font-body);
  font-size: 15px;
  font-weight: 500;
  color: var(--wz-text-soft);
  cursor: pointer;
  position: relative;
  transition: color var(--wz-duration-fast) var(--wz-ease-out);
}

.auth-form__tab:hover { color: var(--wz-text); }

.auth-form__tab--active {
  color: var(--wz-text);
  font-weight: 600;
}

.auth-form__tab--active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 36px;
  height: 2.5px;
  background: var(--wz-orange);
  border-radius: 2px;
}

.auth-form__body {
  flex: 1;
  padding: 36px 40px 32px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-form__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 22px;
  font-weight: 700;
  color: var(--wz-text);
  margin-bottom: 4px;
}

.auth-form__desc {
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 24px;
}

.auth-form__field {
  margin-bottom: 18px;
}

.auth-form__label {
  display: block;
  font-size: 13px;
  color: var(--wz-text);
  margin-bottom: 6px;
  font-weight: 500;
}

.auth-form__input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.auth-form__input-icon {
  position: absolute;
  left: 13px;
  color: var(--wz-text-muted);
  pointer-events: none;
  flex-shrink: 0;
}

.auth-form__input {
  width: 100%;
  height: 44px;
  padding: 0 14px 0 40px;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 12px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.auth-form__input::placeholder { color: var(--wz-text-muted); }

.auth-form__input:focus {
  border-color: var(--wz-orange);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.1);
}

.auth-form__input-toggle {
  position: absolute;
  right: 10px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--wz-text-muted);
  cursor: pointer;
  border-radius: 8px;
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.auth-form__input-toggle:hover {
  background: var(--wz-bg-hover);
  color: var(--wz-text-soft);
}

/* Row: remember + forgot */
.auth-form__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.auth-form__checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--wz-text-soft);
  cursor: pointer;
}

.auth-form__checkbox input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: var(--wz-orange);
  cursor: pointer;
}

/* Button */
.auth-form__btn {
  width: 100%;
  height: 46px;
  background: linear-gradient(135deg, var(--wz-orange) 0%, #e85a2a 100%);
  border: none;
  border-radius: 23px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 1px;
  position: relative;
  overflow: hidden;
  transition: box-shadow var(--wz-duration-normal) var(--wz-ease-out),
              transform var(--wz-duration-normal) var(--wz-ease-out);
}

.auth-form__btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.6s;
}

.auth-form__btn:hover::before { left: 100%; }

.auth-form__btn:hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(255, 107, 53, 0.35);
  transform: translateY(-1px);
}

.auth-form__btn:disabled { opacity: 0.7; cursor: not-allowed; }

.auth-form__btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.auth-form__spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Divider */
.auth-form__divider {
  position: relative;
  text-align: center;
  margin: 24px 0 18px;
  font-size: 12px;
  color: var(--wz-text-muted);
}

.auth-form__divider::before,
.auth-form__divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: calc(50% - 40px);
  height: 1px;
  background: var(--wz-border);
}

.auth-form__divider::before { left: 0; }
.auth-form__divider::after { right: 0; }

/* Social */
.auth-form__social {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 24px;
}

.auth-form__social-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 42px;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 12px;
  font-family: var(--wz-font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              background var(--wz-duration-fast) var(--wz-ease-out);
}

.auth-form__social-btn:hover {
  border-color: var(--wz-orange);
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
}

.auth-form__link {
  font-size: 13px;
  color: var(--wz-orange);
  cursor: pointer;
  text-decoration: none;
}

.auth-form__link:hover { text-decoration: underline; }

.auth-form__footnote {
  text-align: center;
  font-size: 13px;
  color: var(--wz-text-soft);
}

/* ==================== Responsive ==================== */
@media (max-width: 860px) {
  .auth-split {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-brand {
    height: 260px;
    order: -1;
    margin: 0;
    border-radius: 0;
  }

  .auth-brand__content {
    padding: 28px 24px;
  }

  .auth-brand__headline { font-size: 24px; }
  .auth-brand__features { display: none; }

  .auth-form__body { padding: 24px 24px 28px; }
}

@media (max-width: 480px) {
  .auth-form__body { padding: 20px 16px 24px; }
  .auth-form__social { grid-template-columns: 1fr; }
}

.auth-form__code-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
}

.auth-form__code-btn {
  height: 44px;
  padding: 0 16px;
  background: var(--wz-orange);
  border: none;
  border-radius: 12px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity var(--wz-duration-fast) var(--wz-ease-out);
  flex-shrink: 0;
}

.auth-form__code-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-form__code-btn:hover:not(:disabled) {
  opacity: 0.85;
}
</style>
