<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { addFeedback } from '@/api/feedback'
import { showToast } from '@/utils/toast'

const typeOptions = ['功能建议', '商品问题', '配送问题', '其他']
const form = reactive({ type: '功能建议', content: '', contactInfo: '' })
const submitting = ref(false)
const history = ref<{ id: number; type: string; content: string; status: number; replyContent: string | null; createdTime: string }[]>([])

onMounted(async () => {
  // history not available via API — left empty
})

async function handleSubmit() {
  if (!form.content.trim()) {
    showToast('请输入反馈内容', 'warning')
    return
  }
  submitting.value = true
  try {
    await addFeedback({ type: form.type, content: form.content.trim() })
    showToast('感谢你的反馈！', 'success')
    form.content = ''
    form.contactInfo = ''
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="user-feedback">
      <h1 class="user-feedback__title">意见反馈</h1>

      <!-- Form -->
      <div class="user-feedback__form">
        <div class="user-feedback__field">
          <span class="user-feedback__label">反馈类型</span>
          <div class="user-feedback__type-selector">
            <span
              v-for="t in typeOptions"
              :key="t"
              class="user-feedback__type"
              :class="{ 'user-feedback__type--active': form.type === t }"
              @click="form.type = t"
            >{{ t }}</span>
          </div>
        </div>
        <div class="user-feedback__field">
          <span class="user-feedback__label">反馈内容</span>
          <textarea
            v-model="form.content"
            class="user-feedback__textarea"
            placeholder="请详细描述你的问题或建议…"
            rows="5"
          ></textarea>
        </div>
        <div class="user-feedback__field">
          <span class="user-feedback__label">联系方式</span>
          <input
            v-model="form.contactInfo"
            class="user-feedback__input"
            placeholder="手机号或邮箱（选填）"
          >
        </div>
        <div
          class="user-feedback__submit-btn"
          :class="{ 'user-feedback__submit-btn--disabled': submitting }"
          @click="handleSubmit"
        >{{ submitting ? '提交中...' : '提交反馈' }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.user-feedback {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.user-feedback__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.user-feedback__form {
  background: var(--wz-bg-card, #fff);
  border-radius: 16px;
  padding: 28px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 32px;
}
.user-feedback__field {
  margin-bottom: 20px;
}
.user-feedback__label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 10px;
}
.user-feedback__type-selector {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.user-feedback__type {
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  border: 1px solid var(--wz-border);
  color: var(--wz-text);
  cursor: pointer;
  transition: all 0.2s;
}
.user-feedback__type:hover {
  border-color: var(--wz-orange);
  color: var(--wz-orange);
}
.user-feedback__type--active {
  background: var(--wz-orange);
  color: #fff;
  border-color: var(--wz-orange);
}
.user-feedback__textarea {
  width: 100%;
  min-height: 120px;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 10px;
  padding: 14px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  resize: vertical;
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}
.user-feedback__textarea:focus {
  border-color: var(--wz-orange);
}
.user-feedback__textarea::placeholder {
  color: var(--wz-text-soft);
}
.user-feedback__input {
  width: 100%;
  height: 42px;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  padding: 0 14px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}
.user-feedback__input:focus {
  border-color: var(--wz-orange);
}
.user-feedback__input::placeholder {
  color: var(--wz-text-soft);
}
.user-feedback__submit-btn {
  width: 100%;
  height: 46px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 23px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  letter-spacing: 1px;
  transition: background 0.2s;
  margin-top: 24px;
}
.user-feedback__submit-btn:hover {
  background: var(--wz-orange-dark);
}
.user-feedback__submit-btn--disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.user-feedback__history-title {
  font-size: 18px;
  font-weight: 500;
  color: var(--wz-text);
  margin-bottom: 16px;
}
.user-feedback__list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.user-feedback__item {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 18px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.user-feedback__item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.user-feedback__item-type {
  font-size: 13px;
  color: var(--wz-orange);
  font-weight: 500;
}
.user-feedback__item-status {
  font-size: 12px;
  padding: 2px 12px;
  border-radius: 10px;
}
.user-feedback__item-status--done {
  background: rgba(76, 175, 80, 0.1);
  color: #4caf50;
}
.user-feedback__item-content {
  font-size: 14px;
  color: var(--wz-text);
  margin-bottom: 8px;
  line-height: 1.5;
}
.user-feedback__item-time {
  font-size: 12px;
  color: var(--wz-text-soft);
}
</style>
