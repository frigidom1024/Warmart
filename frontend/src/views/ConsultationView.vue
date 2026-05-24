<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getConsultationList, addConsultation } from '@/api/consultation'
import type { Consultation } from '@/api/consultation'
import { showToast } from '@/utils/toast'

const list = ref<Consultation[]>([])
const loading = ref(true)
const showForm = ref(false)
const form = ref({ productId: 0, question: '' })
const submitting = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    list.value = await getConsultationList() as Consultation[]
  } catch { /* handled */ } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  if (!form.value.question.trim()) {
    showToast('请输入咨询内容', 'warning')
    return
  }
  submitting.value = true
  try {
    await addConsultation({ productId: form.value.productId || 1, question: form.value.question.trim() })
    showToast('咨询已提交', 'success')
    showForm.value = false
    form.value.question = ''
    // reload
    list.value = await getConsultationList() as Consultation[]
  } catch { /* handled */ } finally {
    submitting.value = false
  }
}

const statusLabels: Record<number, string> = {
  0: '待回复', 1: '已回复'
}
</script>

<template>
  <div class="page-container">
    <div class="consultation">
      <h1 class="consultation__title">售前咨询</h1>

      <!-- List -->
      <div v-if="list.length" class="consultation__list">
        <div v-for="item in list" :key="item.id" class="consultation__item">
          <div class="consultation__avatar"></div>
          <div class="consultation__content">
            <div class="consultation__top">
              <span class="consultation__name">我</span>
              <span class="consultation__time">{{ item.createdTime }}</span>
              <span class="consultation__status">{{ statusLabels[item.status] || '待回复' }}</span>
            </div>
            <p class="consultation__message"><strong>问：</strong>{{ item.question }}</p>
            <p v-if="item.answer" class="consultation__answer"><strong>答：</strong>{{ item.answer }}</p>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="!loading" class="consultation__empty">
        <p class="consultation__empty-text">暂无咨询记录</p>
        <p class="consultation__empty-hint">您可向我们咨询商品相关问题</p>
      </div>

      <!-- Start / Form -->
      <div class="consultation__start">
        <div v-if="!showForm" class="consultation__start-btn" @click="showForm = true">开始新的咨询</div>
        <div v-else class="consultation__form-card">
          <textarea
            v-model="form.question"
            class="consultation__textarea"
            placeholder="请输入您要咨询的问题..."
            rows="4"
          ></textarea>
          <div class="consultation__form-actions">
            <button
              class="consultation__cancel-btn"
              @click="showForm = false"
            >取消</button>
            <button
              class="consultation__submit-btn"
              :disabled="submitting"
              @click="handleSubmit"
            >{{ submitting ? '提交中...' : '提交咨询' }}</button>
          </div>
        </div>
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
.consultation {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.consultation__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 24px;
}
.consultation__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--wz-bg-card, #fff);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}
.consultation__item {
  display: flex;
  gap: 14px;
  padding: 18px 20px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f8f5f2;
}
.consultation__item:last-child {
  border-bottom: none;
}
.consultation__item:hover {
  background: rgba(255, 107, 53, 0.03);
}
.consultation__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--wz-bg), var(--wz-orange-muted));
  flex-shrink: 0;
}
.consultation__content {
  flex: 1;
  min-width: 0;
}
.consultation__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.consultation__name {
  font-size: 15px;
  font-weight: 500;
  color: var(--wz-text);
}
.consultation__time {
  font-size: 12px;
  color: var(--wz-text-soft);
}
.consultation__message {
  font-size: 14px;
  color: var(--wz-text-soft);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-bottom: 6px;
}
.consultation__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--wz-orange);
  font-weight: 500;
}
.consultation__badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--wz-orange);
}
.consultation__status {
  font-size: 12px;
  color: var(--wz-orange);
  font-weight: 500;
}
.consultation__answer {
  font-size: 14px;
  color: var(--wz-text);
  line-height: 1.5;
  margin-top: 6px;
  padding: 8px 12px;
  background: var(--wz-bg);
  border-radius: 8px;
}
.consultation__form-card {
  background: var(--wz-bg-card);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.consultation__textarea {
  width: 100%;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  padding: 12px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  resize: vertical;
  outline: none;
  box-sizing: border-box;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
}
.consultation__textarea:focus {
  border-color: var(--wz-orange);
}
.consultation__textarea::placeholder {
  color: var(--wz-text-soft);
}
.consultation__form-actions {
  display: flex;
  gap: 12px;
  margin-top: 14px;
  justify-content: flex-end;
}
.consultation__cancel-btn {
  padding: 8px 24px;
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 20px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  cursor: pointer;
}
.consultation__submit-btn {
  padding: 8px 24px;
  background: var(--wz-orange);
  border: none;
  border-radius: 20px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: #fff;
  cursor: pointer;
}
.consultation__submit-btn:disabled {
  opacity: 0.7;
}
.consultation__start {
  text-align: center;
}
.consultation__start-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  padding: 0 40px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  letter-spacing: 1px;
  transition: background 0.2s;
}
.consultation__start-btn:hover {
  background: var(--wz-orange-dark);
}
.consultation__empty {
  text-align: center;
  padding: 80px 24px;
}
.consultation__empty-text {
  font-size: 16px;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.consultation__empty-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
}
</style>
