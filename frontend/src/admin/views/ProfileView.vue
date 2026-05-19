<script setup lang="ts">
</script>

<template>
  <div>
    <div class="page-header">
      <h2>个人信息</h2>
    </div>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card class="profile-card">
          <div class="avatar-section">
            <div class="avatar-placeholder">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <p class="profile-name">管理员</p>
            <p class="profile-role">超级管理员</p>
          </div>
          <el-divider />
          <div class="profile-meta">
            <div class="meta-item">
              <span class="meta-label">部门</span>
              <span class="meta-value">技术部</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">邮箱</span>
              <span class="meta-value">admin@warmart.com</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">手机</span>
              <span class="meta-value">138-0000-0000</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>编辑资料</span>
          </template>
          <div class="form-placeholder">
            <el-empty description="个人信息表单" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--wa-text-primary, #303133);
}

.profile-card {
  text-align: center;
}

.avatar-section {
  padding: 20px 0 8px;
}

.avatar-placeholder {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--wa-color-primary-light-9, #ecf5ff);
  color: var(--wa-color-primary, #409eff);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
}

.profile-name {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: var(--wa-text-primary, #303133);
}

.profile-role {
  margin: 0;
  font-size: 13px;
  color: var(--wa-text-secondary, #909399);
}

.profile-meta {
  text-align: left;
  padding: 0 8px 16px;
}

.meta-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #f2f2f2;
}

.meta-item:last-child {
  border-bottom: none;
}

.meta-label {
  font-size: 13px;
  color: var(--wa-text-secondary, #909399);
}

.meta-value {
  font-size: 14px;
  color: var(--wa-text-primary, #303133);
}

.form-placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
