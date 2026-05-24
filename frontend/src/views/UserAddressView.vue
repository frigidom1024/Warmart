<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/user'
import type { Address } from '@/api/user'

import { showToast } from '@/utils/toast'

const router = useRouter()
const addressList = ref<Address[]>([])
const loading = ref(true)
const showForm = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ receiverName: '', receiverPhone: '', detailAddress: '', isDefault: 0 })

onMounted(async () => {
  await loadAddresses()
})

async function loadAddresses() {
  loading.value = true
  try {
    addressList.value = await getAddressList() as Address[]
  } catch { /* handled */ } finally {
    loading.value = false
  }
}

function openAdd() {
  editingId.value = null
  form.value = { receiverName: '', receiverPhone: '', detailAddress: '', isDefault: 0 }
  showForm.value = true
}

function openEdit(addr: Address) {
  editingId.value = addr.id
  form.value = {
    receiverName: addr.receiverName,
    receiverPhone: addr.receiverPhone,
    detailAddress: addr.detailAddress,
    isDefault: addr.isDefault
  }
  showForm.value = true
}

async function handleSave() {
  if (!form.value.receiverName || !form.value.receiverPhone || !form.value.detailAddress) {
    showToast('请填写完整信息', 'warning')
    return
  }
  try {
    if (editingId.value) {
      await updateAddress({ id: editingId.value, ...form.value })
    } else {
      await addAddress(form.value)
    }
    showForm.value = false
    await loadAddresses()
  } catch { /* handled */ }
}

async function handleDelete(id: number) {
  try {
    await deleteAddress(id)
    await loadAddresses()
  } catch { /* handled */ }
}

async function handleSetDefault(id: number) {
  try {
    await setDefaultAddress(id)
    await loadAddresses()
  } catch { /* handled */ }
}
</script>

<template>
  <div class="page-container">
    <div class="user-address">
      <div class="user-address__header">
        <h1 class="user-address__title">收货地址</h1>
        <div class="user-address__add-btn" @click="openAdd">+ 新增地址</div>
      </div>

      <!-- Address List -->
      <div v-if="addressList.length" class="user-address__list">
        <div v-for="addr in addressList" :key="addr.id" class="user-address__card">
          <div class="user-address__card-top">
            <span class="user-address__name">{{ addr.receiverName }}</span>
            <span class="user-address__phone">{{ addr.receiverPhone }}</span>
            <span v-if="addr.isDefault" class="user-address__default-badge">默认</span>
          </div>
          <p class="user-address__detail">{{ addr.detailAddress }}</p>
          <div class="user-address__card-actions">
            <span class="user-address__action" @click="openEdit(addr)">编辑</span>
            <span class="user-address__action user-address__action--danger" @click="handleDelete(addr.id)">删除</span>
            <span v-if="!addr.isDefault" class="user-address__action" @click="handleSetDefault(addr.id)">设为默认</span>
          </div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="!loading" class="user-address__empty">
        <p class="user-address__empty-text">还没有添加收货地址</p>
        <p class="user-address__empty-hint">添加地址以便下单配送</p>
      </div>

      <!-- Add/Edit Dialog -->
      <Teleport to="body">
        <div v-if="showForm" class="dialog-overlay" @click.self="showForm = false">
          <div class="dialog">
            <h3 class="dialog__title">{{ editingId ? '编辑地址' : '新增地址' }}</h3>
            <div class="dialog__field">
              <label>收货人</label>
              <input v-model="form.receiverName" class="dialog__input" placeholder="请输入收货人姓名">
            </div>
            <div class="dialog__field">
              <label>联系电话</label>
              <input v-model="form.receiverPhone" class="dialog__input" placeholder="请输入联系电话">
            </div>
            <div class="dialog__field">
              <label>详细地址</label>
              <input v-model="form.detailAddress" class="dialog__input" placeholder="请输入详细地址">
            </div>
            <div class="dialog__field">
              <label class="dialog__checkbox">
                <input v-model="form.isDefault" type="checkbox" true-value="1" false-value="0" />
                <span>设为默认地址</span>
              </label>
            </div>
            <div class="dialog__actions">
              <button class="dialog__btn dialog__btn--cancel" @click="showForm = false">取消</button>
              <button class="dialog__btn dialog__btn--confirm" @click="handleSave">保存</button>
            </div>
          </div>
        </div>
      </Teleport>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  min-height: 100vh;
  padding-top: 64px;
  background: var(--wz-bg);
}
.user-address {
  max-width: 700px;
  margin: 0 auto;
  padding: 32px 24px 80px;
}
.user-address__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.user-address__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 24px;
  font-weight: 600;
  color: var(--wz-text);
}
.user-address__add-btn {
  padding: 8px 20px;
  background: var(--wz-orange);
  color: #fff;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.user-address__add-btn:hover {
  background: var(--wz-orange-dark);
}
.user-address__list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.user-address__card {
  background: var(--wz-bg-card, #fff);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}
.user-address__card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}
.user-address__card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.user-address__name {
  font-size: 16px;
  font-weight: 500;
  color: var(--wz-text);
}
.user-address__phone {
  font-size: 14px;
  color: var(--wz-text-soft);
}
.user-address__default-badge {
  font-size: 11px;
  padding: 2px 10px;
  border-radius: 10px;
  background: rgba(255, 107, 53, 0.1);
  color: var(--wz-orange);
  font-weight: 500;
}
.user-address__detail {
  font-size: 14px;
  color: var(--wz-text);
  line-height: 1.6;
  margin-bottom: 14px;
}
.user-address__card-actions {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #f5f0eb;
}
.user-address__action {
  font-size: 13px;
  color: var(--wz-text-soft);
  cursor: pointer;
  transition: color 0.2s;
}
.user-address__action:hover {
  color: var(--wz-orange);
}
.user-address__action--danger:hover {
  color: #e74c3c;
}
.user-address__empty {
  text-align: center;
  padding: 80px 24px;
}
.user-address__empty-text {
  font-size: 16px;
  color: var(--wz-text);
  margin-bottom: 8px;
}
.user-address__empty-hint {
  font-size: 14px;
  color: var(--wz-text-soft);
}
</style>

<style>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
}
.dialog {
  background: var(--wz-bg-card);
  border-radius: 16px;
  padding: 28px 24px;
  width: 380px;
  max-width: 90vw;
}
.dialog__title {
  font-family: var(--wz-font-display, 'Noto Serif SC', serif);
  font-size: 18px;
  font-weight: 600;
  color: var(--wz-text);
  margin-bottom: 20px;
}
.dialog__field {
  margin-bottom: 16px;
}
.dialog__field label {
  display: block;
  font-size: 13px;
  color: var(--wz-text-soft);
  margin-bottom: 6px;
}
.dialog__input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  background: var(--wz-bg);
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  outline: none;
  box-sizing: border-box;
}
.dialog__input:focus {
  border-color: var(--wz-orange);
}
.dialog__checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--wz-text);
  cursor: pointer;
}
.dialog__checkbox input {
  width: 16px;
  height: 16px;
  accent-color: var(--wz-orange);
}
.dialog__actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}
.dialog__btn {
  flex: 1;
  height: 42px;
  border-radius: 21px;
  font-size: 14px;
  cursor: pointer;
  font-family: var(--wz-font-body);
}
.dialog__btn--cancel {
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  color: var(--wz-text);
}
.dialog__btn--confirm {
  background: var(--wz-orange);
  border: none;
  color: #fff;
}
</style>
