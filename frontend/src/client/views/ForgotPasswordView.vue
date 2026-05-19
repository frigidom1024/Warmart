<script setup lang="ts">
const pageTitle = '重置密码'
</script>

<template>
  <div class="page-container">
    <div class="reset-wrapper">
      <el-card class="reset-card" shadow="never">
        <div class="reset-card__header">
          <h1 class="reset-card__title">{{ pageTitle }}</h1>
          <p class="reset-card__desc">输入你的邮箱，我们将发送重置链接</p>
        </div>
        <div class="reset-card__body">
          <div class="reset-card__field">
            <span class="reset-card__label">邮箱地址</span>
            <div class="reset-card__input-placeholder"></div>
          </div>
          <div class="reset-card__actions">
            <div class="reset-card__btn-placeholder">发送重置链接</div>
          </div>
          <p class="reset-card__back">返回登录</p>
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
.reset-card__input-placeholder {
  height: 42px;
  background: var(--wz-bg);
  border-radius: 8px;
  border: 1px solid var(--wz-border);
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
</style>
