<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { sendResetCode, resetPassword, checkEmailRegistered } from '@/api/auth'
import { showToast } from '@/utils/toast'

const router = useRouter()
const step = ref(1)
const email = ref('')
const code = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const codeSending = ref(false)
const codeCountdown = ref(0)
const emailError = ref('')
const emailChecking = ref(false)
const emailRegistered = ref<boolean | null>(null)
const codeError = ref('')
const passwordError = ref('')
const confirmError = ref('')
const resetDone = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const maskedEmail = computed(() => {
  const [name, domain] = email.value.split('@')
  if (!domain) return email.value
  return name.slice(0, 2) + '***@' + domain
})

const canSendCode = computed(() => {
  return email.value.trim().length > 0 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())
})

const canSubmit = computed(() => {
  return code.value.length === 6 && newPassword.value.length >= 6 && newPassword.value === confirmPassword.value
})

onUnmounted(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

function validateEmail() {
  if (!email.value.trim()) {
    emailError.value = '请输入邮箱地址'
    return false
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    emailError.value = '邮箱格式不正确'
    return false
  }
  emailError.value = ''
  return true
}

async function checkEmail() {
  if (!validateEmail()) return
  emailChecking.value = true
  emailRegistered.value = null
  try {
    const registered = await checkEmailRegistered(email.value.trim())
    emailRegistered.value = registered
    if (!registered) {
      emailError.value = '该邮箱尚未注册'
    } else {
      emailError.value = ''
    }
  } catch {
    emailRegistered.value = null
  } finally {
    emailChecking.value = false
  }
}

async function handleSendCode() {
  if (!validateEmail()) return
  codeSending.value = true
  try {
    await sendResetCode(email.value.trim())
    startCountdown()
    step.value = 2
  } catch {
    emailError.value = '该邮箱未注册'
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

async function handleResendCode() {
  if (codeCountdown.value > 0) return
  codeSending.value = true
  try {
    await sendResetCode(email.value.trim())
    startCountdown()
    showToast('验证码已重新发送', 'success')
  } catch { /* handled */ } finally {
    codeSending.value = false
  }
}

function handleBack() {
  if (step.value === 1) {
    router.push('/auth')
  } else {
    step.value = 1
  }
}

async function handleReset() {
  let valid = true
  if (code.value.length !== 6) {
    codeError.value = '请输入6位验证码'
    valid = false
  } else {
    codeError.value = ''
  }
  if (newPassword.value.length < 6) {
    passwordError.value = '密码长度不少于6位'
    valid = false
  } else {
    passwordError.value = ''
  }
  if (newPassword.value !== confirmPassword.value) {
    confirmError.value = '两次输入的密码不一致'
    valid = false
  } else {
    confirmError.value = ''
  }
  if (!valid) return

  loading.value = true
  try {
    await resetPassword(email.value.trim(), code.value, newPassword.value)
    resetDone.value = true
  } catch { /* handled */ } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="reset-root">
    <div class="reset-frame">
      <!-- Step indicator -->
      <div class="reset-steps">
        <div class="reset-step" :class="{ 'reset-step--done': step > 1, 'reset-step--active': step === 1 }">
          <div class="reset-step__circle">
            <svg v-if="step > 1" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            <span v-else>1</span>
          </div>
          <span class="reset-step__label">验证邮箱</span>
        </div>
        <div class="reset-step__connector" :class="{ 'reset-step__connector--active': step > 1 }" />
        <div class="reset-step" :class="{ 'reset-step--active': step === 2, 'reset-step--done': resetDone }">
          <div class="reset-step__circle">
            <svg v-if="resetDone" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            <span v-else>2</span>
          </div>
          <span class="reset-step__label">设置密码</span>
        </div>
      </div>

      <!-- Step 1: Email -->
      <transition name="fade-slide" mode="out-in">
        <div v-if="step === 1" key="step1" class="reset-body">
          <div class="reset-brand">
            <span class="reset-brand__mark">暖</span>
            <span class="reset-brand__name">Warmart</span>
          </div>
          <h1 class="reset-heading">找回密码</h1>
          <p class="reset-desc">输入注册时使用的邮箱地址，我们将发送验证码协助您重置密码。</p>

          <div class="reset-field">
            <label class="reset-field__label">邮箱地址</label>
            <div class="reset-field__input-wrap" :class="{ 'reset-field__input-wrap--error': emailError }">
              <svg class="reset-field__icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg>
              <input
                v-model="email"
                type="email"
                class="reset-field__input"
                placeholder="请输入邮箱地址"
                autocomplete="email"
                @blur="checkEmail"
                @input="emailError = ''; emailRegistered = null"
              />
              <span v-if="emailChecking" class="reset-field__suffix">
                <svg class="reset-field__spinner" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-dashoffset="32"/></svg>
              </span>
              <span v-else-if="emailRegistered === true" class="reset-field__suffix reset-field__suffix--ok">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
              </span>
              <span v-else-if="emailRegistered === false" class="reset-field__suffix reset-field__suffix--err">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </span>
            </div>
            <p v-if="emailError" class="reset-field__error">{{ emailError }}</p>
            <p v-else-if="emailRegistered === true" class="reset-field__hint reset-field__hint--ok">邮箱已验证，可发送验证码</p>
          </div>

          <button
            class="reset-btn reset-btn--primary"
            :disabled="!canSendCode || codeSending"
            @click="handleSendCode"
          >
            <span v-if="codeSending" class="reset-btn__loading">
              <svg class="reset-btn__spinner" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-dashoffset="32"/></svg>
              发送中...
            </span>
            <span v-else>发送验证码</span>
          </button>

          <button class="reset-back" @click="handleBack">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
            返回登录
          </button>
        </div>

        <!-- Step 2: Code + Password -->
        <div v-else-if="step === 2 && !resetDone" key="step2" class="reset-body">
          <div class="reset-brand">
            <span class="reset-brand__mark">暖</span>
            <span class="reset-brand__name">Warmart</span>
          </div>
          <h1 class="reset-heading">设置新密码</h1>
          <p class="reset-desc">
            验证码已发送至 <strong class="reset-desc__email">{{ maskedEmail }}</strong>，
            请输入验证码并设置新密码。
          </p>

          <div class="reset-field">
            <label class="reset-field__label">验证码</label>
            <div class="reset-field__code-wrap" :class="{ 'reset-field__input-wrap--error': codeError }">
              <input
                v-model="code"
                type="text"
                class="reset-field__input reset-field__input--code"
                placeholder="6位验证码"
                maxlength="6"
                autocomplete="one-time-code"
                @input="codeError = ''"
              />
              <button
                class="reset-field__code-btn"
                :disabled="codeCountdown > 0 || codeSending"
                @click="handleResendCode"
              >
                <template v-if="codeSending">发送中...</template>
                <template v-else-if="codeCountdown > 0">{{ codeCountdown }}s</template>
                <template v-else>重新发送</template>
              </button>
            </div>
            <p v-if="codeError" class="reset-field__error">{{ codeError }}</p>
          </div>

          <div class="reset-field">
            <label class="reset-field__label">新密码</label>
            <div class="reset-field__input-wrap" :class="{ 'reset-field__input-wrap--error': passwordError }">
              <svg class="reset-field__icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input
                v-model="newPassword"
                type="password"
                class="reset-field__input"
                placeholder="至少6位密码"
                autocomplete="new-password"
                @input="passwordError = ''"
              />
            </div>
            <p v-if="passwordError" class="reset-field__error">{{ passwordError }}</p>
          </div>

          <div class="reset-field">
            <label class="reset-field__label">确认密码</label>
            <div class="reset-field__input-wrap" :class="{ 'reset-field__input-wrap--error': confirmError }">
              <svg class="reset-field__icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
              <input
                v-model="confirmPassword"
                type="password"
                class="reset-field__input"
                placeholder="请再次输入新密码"
                autocomplete="new-password"
                @input="confirmError = ''"
              />
            </div>
            <p v-if="confirmError" class="reset-field__error">{{ confirmError }}</p>
          </div>

          <button
            class="reset-btn reset-btn--primary"
            :disabled="!canSubmit || loading"
            @click="handleReset"
          >
            <span v-if="loading" class="reset-btn__loading">
              <svg class="reset-btn__spinner" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-dashoffset="32"/></svg>
              重置中...
            </span>
            <span v-else>重置密码</span>
          </button>

          <button class="reset-back" @click="handleBack">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
            返回上一步
          </button>
        </div>

        <!-- Success state -->
        <div v-else key="success" class="reset-body reset-body--centered">
          <div class="reset-success-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          </div>
          <h1 class="reset-heading">密码已重置</h1>
          <p class="reset-desc">密码已成功修改，请使用新密码登录。</p>
          <button class="reset-btn reset-btn--primary" @click="router.push('/auth')">
            立即登录
          </button>
        </div>
      </transition>
    </div>
  </div>
</template>

<style scoped>
/* ============================
   Root & Frame
   ============================ */
.reset-root {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--wz-bg, #0f0f11);
  padding: 100px 24px 48px;
}

.reset-frame {
  width: 100%;
  max-width: 420px;
  background: var(--wz-bg-card, #1a1b1f);
  border: 1px solid var(--wz-border, #2a2b30);
  border-radius: 28px;
  padding: 40px 36px 32px;
  transition: box-shadow 250ms ease-out;
}

/* ============================
   Step Indicator
   ============================ */
.reset-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 36px;
}

.reset-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reset-step__circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: var(--wz-bg-elevated, #222327);
  color: var(--wz-text-muted, #6b6c72);
  transition: all 250ms ease-out;
}

.reset-step--active .reset-step__circle {
  background: var(--wz-orange, #ff6b35);
  color: #fff;
}

.reset-step--done .reset-step__circle {
  background: #34c759;
  color: #fff;
}

.reset-step__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-muted, #6b6c72);
  transition: color 250ms ease-out;
  display: none;
}

.reset-step--active .reset-step__label,
.reset-step--done .reset-step__label {
  color: var(--wz-text-soft, #b0b1b6);
  display: inline;
}

.reset-step__connector {
  width: 40px;
  height: 2px;
  background: var(--wz-bg-elevated, #222327);
  margin: 0 10px;
  border-radius: 1px;
  transition: background 300ms ease-out;
}

.reset-step__connector--active {
  background: var(--wz-orange, #ff6b35);
}

@media (min-width: 480px) {
  .reset-step__label {
    display: inline;
  }
  .reset-step__connector {
    width: 56px;
    margin: 0 12px;
  }
}

/* ============================
   Body
   ============================ */
.reset-body {
  animation: fadeIn 300ms ease-out;
}

.reset-body--centered {
  text-align: center;
  padding: 20px 0;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ============================
   Brand
   ============================ */
.reset-brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 24px;
}

.reset-brand__mark {
  font-family: 'Noto Serif SC', Georgia, serif;
  font-size: 28px;
  font-weight: 700;
  color: var(--wz-orange, #ff6b35);
  line-height: 1;
}

.reset-brand__name {
  font-family: 'Noto Serif SC', Georgia, serif;
  font-size: 20px;
  font-weight: 700;
  color: var(--wz-text, #ffffff);
  letter-spacing: 0.05em;
  line-height: 1;
}

/* ============================
   Text
   ============================ */
.reset-heading {
  font-family: 'Noto Serif SC', Georgia, serif;
  font-size: 22px;
  font-weight: 600;
  color: var(--wz-text, #ffffff);
  text-align: center;
  margin: 0 0 8px;
  line-height: 1.3;
}

.reset-desc {
  font-size: 14px;
  color: var(--wz-text-soft, #b0b1b6);
  text-align: center;
  margin: 0 0 28px;
  line-height: 1.5;
}

.reset-desc__email {
  color: var(--wz-text, #ffffff);
  font-weight: 500;
}

/* ============================
   Fields
   ============================ */
.reset-field {
  margin-bottom: 20px;
}

.reset-field__label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text, #ffffff);
  margin-bottom: 6px;
}

.reset-field__input-wrap {
  position: relative;
  display: flex;
  align-items: center;
  background: var(--wz-bg, #0f0f11);
  border: 1px solid var(--wz-border, #2a2b30);
  border-radius: 10px;
  transition: border-color 200ms ease-out, box-shadow 200ms ease-out;
}

.reset-field__input-wrap:focus-within {
  border-color: var(--wz-orange, #ff6b35);
  box-shadow: 0 0 0 3px rgba(255, 107, 53, 0.1);
}

.reset-field__input-wrap--error {
  border-color: #ff453a;
}

.reset-field__input-wrap--error:focus-within {
  box-shadow: 0 0 0 3px rgba(255, 69, 58, 0.1);
}

.reset-field__icon {
  position: absolute;
  left: 13px;
  color: var(--wz-text-muted, #6b6c72);
  pointer-events: none;
  flex-shrink: 0;
}

.reset-field__input {
  width: 100%;
  height: 44px;
  padding: 0 14px 0 40px;
  background: transparent;
  border: none;
  outline: none;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  font-size: 14px;
  color: var(--wz-text, #ffffff);
}

.reset-field__input::placeholder {
  color: var(--wz-text-muted, #6b6c72);
}

.reset-field__input--code {
  padding: 0 14px;
  text-align: center;
  letter-spacing: 4px;
  font-size: 16px;
  font-weight: 600;
}

.reset-field__code-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
}

.reset-field__code-wrap .reset-field__input-wrap--error {
  flex: 1;
}

.reset-field__code-btn {
  height: 44px;
  padding: 0 14px;
  background: var(--wz-orange, #ff6b35);
  border: none;
  border-radius: 10px;
  color: #fff;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: opacity 200ms ease-out;
}

.reset-field__code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reset-field__code-btn:hover:not(:disabled) {
  opacity: 0.85;
}

.reset-field__error {
  margin: 4px 0 0;
  font-size: 12px;
  color: #ff453a;
  line-height: 1.4;
}

.reset-field__hint {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.4;
}

.reset-field__hint--ok {
  color: #34c759;
}

.reset-field__suffix {
  position: absolute;
  right: 12px;
  display: flex;
  align-items: center;
  pointer-events: none;
}

.reset-field__suffix--ok {
  color: #34c759;
}

.reset-field__suffix--err {
  color: #ff453a;
}

.reset-field__spinner {
  animation: spin 0.8s linear infinite;
}

/* ============================
   Buttons
   ============================ */
.reset-btn {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 23px;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  letter-spacing: 0.5px;
  transition: box-shadow 250ms ease-out, transform 250ms ease-out, opacity 200ms ease-out;
}

.reset-btn--primary {
  background: linear-gradient(135deg, var(--wz-orange, #ff6b35) 0%, #e55a2b 100%);
  color: #fff;
  margin-top: 4px;
}

.reset-btn--primary:hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(255, 107, 53, 0.35);
  transform: translateY(-1px);
}

.reset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reset-btn__loading {
  display: flex;
  align-items: center;
  gap: 8px;
}

.reset-btn__spinner {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ============================
   Back
   ============================ */
.reset-back {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 16px;
  padding: 10px;
  background: none;
  border: none;
  font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
  color: var(--wz-text-soft, #b0b1b6);
  cursor: pointer;
  transition: color 200ms ease-out;
}

.reset-back:hover {
  color: var(--wz-orange, #ff6b35);
}

/* ============================
   Success
   ============================ */
.reset-success-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: rgba(52, 199, 89, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  color: #34c759;
  animation: popIn 400ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes popIn {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}

.reset-body--centered .reset-desc {
  margin-bottom: 32px;
}

/* ============================
   Transition
   ============================ */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 250ms ease-out;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
</style>
