<script setup lang="ts">
const pageTitle = '个人信息'
</script>

<template>
  <div class="page-container">
    <div class="user-info">
      <h1 class="user-info__title">{{ pageTitle }}</h1>

      <!-- Profile Card -->
      <div class="user-info__profile-card">
        <div class="user-info__avatar-section">
          <div class="user-info__avatar"></div>
          <p class="user-info__avatar-hint">点击更换头像</p>
        </div>
        <div class="user-info__fields">
          <div v-for="field in ['昵称', '邮箱', '手机号', '性别', '生日']" :key="field" class="user-info__field">
            <span class="user-info__field-label">{{ field }}</span>
            <div class="user-info__field-value">
              <span>{{ field === '性别' ? '未设置' : (field === '昵称' ? 'Warmart用户' : '-') }}</span>
              <span class="user-info__edit-icon">✎</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Stats Card -->
      <div class="user-info__stats">
        <div class="user-info__stat-item">
          <span class="user-info__stat-value">0</span>
          <span class="user-info__stat-label">收藏</span>
        </div>
        <div class="user-info__stat-item">
          <span class="user-info__stat-value">0</span>
          <span class="user-info__stat-label">订单</span>
        </div>
        <div class="user-info__stat-item">
          <span class="user-info__stat-value">0</span>
          <span class="user-info__stat-label">优惠券</span>
        </div>
        <div class="user-info__stat-item">
          <span class="user-info__stat-value">0</span>
          <span class="user-info__stat-label">积分</span>
        </div>
      </div>

      <div class="user-info__save-btn">保存修改</div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.user-info {
  max-width: 600px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.user-info__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.user-info__profile-card {
  background: var(--wz-bg-card, #fff);
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 20px;
}
.user-info__avatar-section {
  text-align: center;
  margin-bottom: 28px;
}
.user-info__avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  margin: 0 auto 10px;
  cursor: pointer;
  transition: opacity 0.2s;
}
.user-info__avatar:hover {
  opacity: 0.8;
}
.user-info__avatar-hint {
  font-size: 13px;
  color: var(--wz-text-soft);
}
.user-info__fields {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.user-info__field {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f0eb;
}
.user-info__field:last-child {
  border-bottom: none;
}
.user-info__field-label {
  font-size: 14px;
  color: var(--wz-text-soft);
  min-width: 64px;
}
.user-info__field-value {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--wz-text);
}
.user-info__edit-icon {
  font-size: 16px;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: color 0.2s;
}
.user-info__edit-icon:hover {
  color: var(--wz-orange);
}
.user-info__stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 28px;
}
.user-info__stat-item {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px 12px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.user-info__stat-value {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-orange);
  margin-bottom: 6px;
}
.user-info__stat-label {
  display: block;
  font-size: 13px;
  color: var(--wz-text-soft);
}
.user-info__save-btn {
  width: 100%;
  height: 48px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  letter-spacing: 2px;
  transition: background 0.2s;
}
.user-info__save-btn:hover {
  background: var(--wz-orange-dark);
}
</style>
