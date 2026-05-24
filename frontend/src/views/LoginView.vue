<script setup lang="ts">
const pageTitle = '登录'
</script>

<template>
  <div class="page-container">
    <div class="login-wrapper">
      <!-- Decorative background note -->
      <div class="login-decoration">
        <div class="login-decoration__circle"></div>
        <div class="login-decoration__circle login-decoration__circle--small"></div>
      </div>

      <el-card class="login-card" shadow="never">
        <div class="login-card__header">
          <h1 class="login-card__title">{{ pageTitle }}</h1>
          <p class="login-card__desc">欢迎回到 Warmart，继续你的温暖之旅</p>
        </div>
        <div class="login-card__body">
          <div class="login-card__field">
            <span class="login-card__label">邮箱 / 手机号</span>
            <div class="login-card__input-placeholder"></div>
          </div>
          <div class="login-card__field">
            <span class="login-card__label">密码</span>
            <div class="login-card__input-placeholder"></div>
          </div>
          <div class="login-card__actions">
            <div class="login-card__btn-placeholder">登录</div>
          </div>
          <div class="login-card__links">
            <span class="login-card__link">忘记密码？</span>
            <span class="login-card__link">注册账号</span>
          </div>
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
  position: relative;
  overflow: hidden;
}
.login-wrapper {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 24px;
}
.login-decoration {
  position: absolute;
  top: -80px;
  right: -80px;
  z-index: 0;
}
.login-decoration__circle {
  width: 240px;
  height: 240px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 107, 53, 0.08) 0%, transparent 70%);
}
.login-decoration__circle--small {
  width: 120px;
  height: 120px;
  top: 60px;
  right: 60px;
  position: absolute;
}
.login-card {
  position: relative;
  z-index: 1;
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 53, 0.08);
}
.login-card__header {
  text-align: center;
  padding: 32px 24px 24px;
}
.login-card__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 28px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.login-card__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.login-card__body {
  padding: 0 24px 32px;
}
.login-card__field {
  margin-bottom: 20px;
}
.login-card__label {
  display: block;
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 8px;
  font-weight: 500;
}
.login-card__input-placeholder {
  height: 42px;
  background: var(--wz-bg);
  border-radius: 8px;
  border: 1px solid var(--wz-border);
}
.login-card__actions {
  margin-top: 28px;
}
.login-card__btn-placeholder {
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
  letter-spacing: 2px;
}
.login-card__links {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}
.login-card__link {
  font-size: 13px;
  color: var(--wz-orange);
  cursor: pointer;
}
</style>
