<script setup lang="ts">
const pageTitle = '创建账号'
</script>

<template>
  <div class="page-container">
    <div class="register-wrapper">
      <el-card class="register-card" shadow="never">
        <div class="register-card__header">
          <h1 class="register-card__title">{{ pageTitle }}</h1>
          <p class="register-card__desc">加入 Warmart，开启温暖购物之旅</p>
        </div>
        <div class="register-card__body">
          <div v-for="field in ['邮箱', '手机号', '密码', '确认密码']" :key="field" class="register-card__field">
            <span class="register-card__label">{{ field }}</span>
            <div class="register-card__input-placeholder"></div>
          </div>
          <div class="register-card__actions">
            <div class="register-card__btn-placeholder">注册</div>
          </div>
          <p class="register-card__footer">
            已有账号？<span class="register-card__link">立即登录</span>
          </p>
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
.register-wrapper {
  width: 100%;
  max-width: 420px;
  padding: 24px;
}
.register-card {
  border-radius: 16px;
  border: 1px solid rgba(255, 107, 53, 0.08);
}
.register-card__header {
  text-align: center;
  padding: 32px 24px 24px;
}
.register-card__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 28px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.register-card__desc {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.register-card__body {
  padding: 0 24px 32px;
}
.register-card__field {
  margin-bottom: 18px;
}
.register-card__label {
  display: block;
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 6px;
  font-weight: 500;
}
.register-card__input-placeholder {
  height: 42px;
  background: var(--wz-bg);
  border-radius: 8px;
  border: 1px solid var(--wz-border);
}
.register-card__actions {
  margin-top: 24px;
}
.register-card__btn-placeholder {
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
.register-card__footer {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: var(--wz-text-soft);
}
.register-card__link {
  color: var(--wz-orange);
  cursor: pointer;
}
</style>
