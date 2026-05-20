<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, updateUserInfo, updatePassword, getAddressList, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api/user'
import type { UserInfo, Address } from '@/api/user'
import { getFavoriteList } from '@/api/favorite'
import { getOrderList } from '@/api/order'
import { showToast } from '@/utils/toast'

const router = useRouter()
const userInfo = ref<UserInfo | null>(null)
const loading = ref(true)
const saving = ref(false)

// ─── Tab ───
type Tab = 'profile' | 'address' | 'security'
const activeTab = ref<Tab>('profile')

const tabs = [
  { id: 'profile' as Tab, label: '个人信息', icon: 'profile' },
  { id: 'address' as Tab, label: '收货地址', icon: 'address' },
  { id: 'security' as Tab, label: '安全设置', icon: 'security' },
]

// ─── Profile Form ───
const form = reactive({ nickname: '', email: '', phone: '' })

// ─── Address ───
const addressList = ref<Address[]>([])
const addressLoading = ref(false)
const addressFormVisible = ref(false)
const editingAddressId = ref<number | null>(null)
const addressForm = reactive({ receiverName: '', receiverPhone: '', detailAddress: '', isDefault: 0 })

// ─── Password ───
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

// ─── Stats ───
const stats = reactive({ favorites: 0, orders: 0 })

onMounted(async () => {
  try {
    const info = await getUserInfo()
    userInfo.value = info as UserInfo
    form.nickname = (info as UserInfo).nickname || ''
    form.email = (info as UserInfo).email || ''
    form.phone = (info as UserInfo).phone || ''
  } catch { /* handled */ }

  try {
    const [favRes, orderRes] = await Promise.all([
      getFavoriteList({ size: 1 }),
      getOrderList({ size: 1 })
    ])
    stats.favorites = (favRes as any).total || 0
    stats.orders = (orderRes as any).total || 0
  } catch { /* ignore */ }

  loading.value = false
})

// ─── Profile ───
async function handleSaveProfile() {
  saving.value = true
  try {
    await updateUserInfo({ nickname: form.nickname })
    showToast('保存成功', 'success')
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

// ─── Address ───
async function loadAddresses() {
  addressLoading.value = true
  try {
    addressList.value = await getAddressList() as Address[]
  } catch { /* handled */ } finally {
    addressLoading.value = false
  }
}

function openAddAddress() {
  editingAddressId.value = null
  addressForm.receiverName = ''
  addressForm.receiverPhone = ''
  addressForm.detailAddress = ''
  addressForm.isDefault = 0
  addressFormVisible.value = true
}

function openEditAddress(addr: Address) {
  editingAddressId.value = addr.id
  addressForm.receiverName = addr.receiverName
  addressForm.receiverPhone = addr.receiverPhone
  addressForm.detailAddress = addr.detailAddress
  addressForm.isDefault = addr.isDefault
  addressFormVisible.value = true
}

async function handleSaveAddress() {
  if (!addressForm.receiverName || !addressForm.receiverPhone || !addressForm.detailAddress) {
    showToast('请填写完整信息', 'warning')
    return
  }
  try {
    if (editingAddressId.value) {
      await updateAddress({ id: editingAddressId.value, ...addressForm })
    } else {
      await addAddress(addressForm)
    }
    addressFormVisible.value = false
    await loadAddresses()
  } catch { /* handled */ }
}

async function handleDeleteAddress(id: number) {
  try {
    await deleteAddress(id)
    await loadAddresses()
  } catch { /* handled */ }
}

async function handleSetDefaultAddress(id: number) {
  try {
    await setDefaultAddress(id)
    await loadAddresses()
  } catch { /* handled */ }
}

// ─── Password ───
async function handleChangePassword() {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirm) {
    showToast('请填写完整信息', 'warning')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirm) {
    showToast('两次密码不一致', 'warning')
    return
  }
  saving.value = true
  try {
    await updatePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    showToast('密码修改成功', 'success')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirm = ''
  } catch { /* handled */ } finally {
    saving.value = false
  }
}

function switchTab(tab: Tab) {
  activeTab.value = tab
  if (tab === 'address' && !addressList.value.length) {
    loadAddresses()
  }
}
</script>

<template>
  <div class="page-container">
    <div class="profile-layout">
      <!-- ═══ SIDEBAR ═══ -->
      <aside class="sidebar">
        <div class="sidebar__card">
          <!-- Avatar -->
          <div class="sidebar__avatar-row">
            <img v-if="userInfo?.avatar" :src="userInfo.avatar" class="sidebar__avatar" />
            <div v-else class="sidebar__avatar sidebar__avatar--empty">
              <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </div>
            <div class="sidebar__name-group">
              <p class="sidebar__name">{{ userInfo?.nickname || userInfo?.username || '用户' }}</p>
              <p class="sidebar__sub">{{ userInfo?.email || '未设置邮箱' }}</p>
            </div>
          </div>

          <!-- Stats -->
          <div class="sidebar__stats">
            <div class="sidebar__stat" @click="router.push('/order/list')">
              <span class="sidebar__stat-value">{{ stats.orders }}</span>
              <span class="sidebar__stat-label">订单</span>
            </div>
            <div class="sidebar__stat" @click="router.push('/user/favorites')">
              <span class="sidebar__stat-value">{{ stats.favorites }}</span>
              <span class="sidebar__stat-label">收藏</span>
            </div>
          </div>
        </div>

        <!-- Navigation -->
        <nav class="sidebar__nav">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            class="sidebar__nav-btn"
            :class="{ 'sidebar__nav-btn--active': activeTab === tab.id }"
            @click="switchTab(tab.id)"
          >
            <span class="sidebar__nav-icon">
              <svg v-if="tab.icon === 'profile'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              <svg v-else-if="tab.icon === 'address'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
              <svg v-else-if="tab.icon === 'security'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </span>
            <span class="sidebar__nav-label">{{ tab.label }}</span>
            <svg class="sidebar__nav-arrow" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M9 18l6-6-6-6"/></svg>
          </button>
        </nav>

        <!-- Extra links -->
        <div class="sidebar__extras">
          <button class="sidebar__extra-btn" @click="router.push('/user/feedback')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
            <span>意见反馈</span>
          </button>
          <button class="sidebar__extra-btn" @click="router.push('/service/notice')">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 0 1-3.46 0"/></svg>
            <span>系统公告</span>
          </button>
        </div>
      </aside>

      <!-- ═══ CONTENT ═══ -->
      <main class="content">
        <!-- ─── Profile ─── -->
        <div v-if="activeTab === 'profile'" class="content__panel">
          <div class="content__panel-header">
            <h2 class="content__title">个人信息</h2>
            <p class="content__desc">管理您的基本资料和联系方式</p>
          </div>

          <div class="info-card">
            <div class="info-card__row">
              <span class="info-card__label">用户名</span>
              <span class="info-card__value">{{ userInfo?.username || '-' }}</span>
            </div>
            <div class="info-card__row">
              <span class="info-card__label">昵称</span>
              <input v-model="form.nickname" class="info-card__input" placeholder="设置您的昵称" />
            </div>
            <div class="info-card__row">
              <span class="info-card__label">邮箱</span>
              <span class="info-card__value">{{ userInfo?.email || '未设置' }}</span>
            </div>
            <div class="info-card__row">
              <span class="info-card__label">手机号</span>
              <span class="info-card__value">{{ userInfo?.phone || '未设置' }}</span>
            </div>
          </div>

          <button class="content__save-btn" :class="{ 'content__save-btn--busy': saving }" @click="handleSaveProfile">
            {{ saving ? '保存中…' : '保存修改' }}
          </button>
        </div>

        <!-- ─── Address ─── -->
        <div v-if="activeTab === 'address'" class="content__panel">
          <div class="content__panel-header">
            <div>
              <h2 class="content__title">收货地址</h2>
              <p class="content__desc">管理您的收货地址，下单时快速选择</p>
            </div>
            <button class="content__add-btn" @click="openAddAddress">+ 新增地址</button>
          </div>

          <div v-if="addressLoading" class="content__loading">
            <div class="content__loading-spinner" />
            <span>加载中…</span>
          </div>

          <template v-else-if="addressList.length">
            <div
              v-for="addr in addressList"
              :key="addr.id"
              class="addr-card"
              :class="{ 'addr-card--default': addr.isDefault }"
            >
              <div class="addr-card__badge" v-if="addr.isDefault">默认</div>
              <div class="addr-card__top">
                <span class="addr-card__name">{{ addr.receiverName }}</span>
                <span class="addr-card__phone">{{ addr.receiverPhone }}</span>
              </div>
              <p class="addr-card__detail">{{ addr.detailAddress }}</p>
              <div class="addr-card__actions">
                <button class="addr-card__action" @click="openEditAddress(addr)">编辑</button>
                <button v-if="!addr.isDefault" class="addr-card__action" @click="handleSetDefaultAddress(addr.id)">设为默认</button>
                <button class="addr-card__action addr-card__action--danger" @click="handleDeleteAddress(addr.id)">删除</button>
              </div>
            </div>
          </template>

          <div v-else class="content__empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" class="content__empty-icon"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/></svg>
            <p class="content__empty-text">还没有添加收货地址</p>
            <p class="content__empty-hint">添加地址以便下单配送</p>
            <button class="content__add-btn content__add-btn--center" @click="openAddAddress">+ 新增地址</button>
          </div>
        </div>

        <!-- ─── Security ─── -->
        <div v-if="activeTab === 'security'" class="content__panel">
          <div class="content__panel-header">
            <h2 class="content__title">安全设置</h2>
            <p class="content__desc">修改您的登录密码，保障账户安全</p>
          </div>

          <div class="info-card">
            <div class="info-card__row">
              <span class="info-card__label">旧密码</span>
              <input v-model="passwordForm.oldPassword" type="password" class="info-card__input" placeholder="请输入当前密码" />
            </div>
            <div class="info-card__row">
              <span class="info-card__label">新密码</span>
              <input v-model="passwordForm.newPassword" type="password" class="info-card__input" placeholder="请输入新密码（至少6位）" />
            </div>
            <div class="info-card__row">
              <span class="info-card__label">确认密码</span>
              <input v-model="passwordForm.confirm" type="password" class="info-card__input" placeholder="请再次输入新密码" />
            </div>
          </div>

          <button class="content__save-btn" :class="{ 'content__save-btn--busy': saving }" @click="handleChangePassword">
            {{ saving ? '修改中…' : '修改密码' }}
          </button>
        </div>
      </main>
    </div>

    <!-- ═══ ADDRESS DIALOG ═══ -->
    <Teleport to="body">
      <div v-if="addressFormVisible" class="dialog-overlay" @click.self="addressFormVisible = false">
        <div class="dialog">
          <h3 class="dialog__title">{{ editingAddressId ? '编辑地址' : '新增地址' }}</h3>
          <div class="dialog__field">
            <label>收货人</label>
            <input v-model="addressForm.receiverName" class="dialog__input" placeholder="请输入收货人姓名">
          </div>
          <div class="dialog__field">
            <label>联系电话</label>
            <input v-model="addressForm.receiverPhone" class="dialog__input" placeholder="请输入联系电话">
          </div>
          <div class="dialog__field">
            <label>详细地址</label>
            <input v-model="addressForm.detailAddress" class="dialog__input" placeholder="请输入详细地址">
          </div>
          <div class="dialog__field">
            <label class="dialog__checkbox">
              <input v-model="addressForm.isDefault" type="checkbox" true-value="1" false-value="0" />
              <span>设为默认地址</span>
            </label>
          </div>
          <div class="dialog__actions">
            <button class="dialog__btn dialog__btn--cancel" @click="addressFormVisible = false">取消</button>
            <button class="dialog__btn dialog__btn--confirm" @click="handleSaveAddress">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* =============================================
   User Profile — Left-Right Layout
   ============================================= */

.page-container {
  min-height: 100vh;
  padding-top: 60px;
  background: var(--wz-bg);
}

.profile-layout {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px 80px;
  display: flex;
  gap: 32px;
  align-items: flex-start;
}

/* ═══ Sidebar ═══ */
.sidebar {
  width: 260px;
  flex-shrink: 0;
  position: sticky;
  top: 92px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.sidebar__card {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 14px;
  padding: 24px 20px 20px;
}

.sidebar__avatar-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.sidebar__avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}

.sidebar__avatar--empty {
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
  display: flex;
  align-items: center;
  justify-content: center;
}

.sidebar__name-group {
  min-width: 0;
}

.sidebar__name {
  font-family: var(--wz-font-display);
  font-size: 16px;
  font-weight: 600;
  color: var(--wz-text);
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__sub {
  font-size: 12px;
  color: var(--wz-text-muted);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar__stats {
  display: flex;
  gap: 0;
  border-top: 1px solid var(--wz-border);
  padding-top: 16px;
}

.sidebar__stat {
  flex: 1;
  text-align: center;
  cursor: pointer;
  padding: 4px 0;
  border-radius: 8px;
  transition: background var(--wz-duration-fast) var(--wz-ease-out);
}

.sidebar__stat:hover {
  background: var(--wz-bg-hover);
}

.sidebar__stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--wz-orange);
  line-height: 1.2;
}

.sidebar__stat-label {
  display: block;
  font-size: 12px;
  color: var(--wz-text-muted);
  margin-top: 2px;
}

/* Navigation */
.sidebar__nav {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 14px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar__nav-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 14px;
  background: none;
  border: none;
  border-radius: 10px;
  font-family: var(--wz-font-body);
  font-size: 14px;
  font-weight: 500;
  color: var(--wz-text-soft);
  cursor: pointer;
  text-align: left;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.sidebar__nav-btn:hover {
  background: var(--wz-bg-hover);
  color: var(--wz-text);
}

.sidebar__nav-btn--active {
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
}

.sidebar__nav-icon {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.sidebar__nav-label {
  flex: 1;
}

.sidebar__nav-arrow {
  opacity: 0;
  transition: opacity var(--wz-duration-fast) var(--wz-ease-out),
              transform var(--wz-duration-fast) var(--wz-ease-out);
}

.sidebar__nav-btn--active .sidebar__nav-arrow {
  opacity: 1;
  transform: translateX(2px);
}

/* Extra links */
.sidebar__extras {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 14px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar__extra-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  background: none;
  border: none;
  border-radius: 8px;
  font-family: var(--wz-font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-muted);
  cursor: pointer;
  text-align: left;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out);
}

.sidebar__extra-btn:hover {
  background: var(--wz-bg-hover);
  color: var(--wz-text);
}

/* ═══ Content ═══ */
.content {
  flex: 1;
  min-width: 0;
}

.content__panel {
  animation: panelIn 0.25s var(--wz-ease-out);
}

@keyframes panelIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.content__panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.content__title {
  font-family: var(--wz-font-display);
  font-size: 22px;
  font-weight: 700;
  color: var(--wz-text);
  line-height: 1.3;
}

.content__desc {
  font-size: 13px;
  color: var(--wz-text-muted);
  margin-top: 4px;
}

.content__add-btn {
  flex-shrink: 0;
  height: 38px;
  padding: 0 22px;
  background: var(--wz-orange);
  border: none;
  border-radius: 19px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.content__add-btn:hover {
  background: var(--wz-orange-dark);
  box-shadow: 0 4px 16px rgba(255, 107, 53, 0.3);
}

.content__add-btn--center {
  margin-top: 16px;
}

.content__save-btn {
  width: 100%;
  height: 46px;
  margin-top: 20px;
  background: var(--wz-orange);
  border: none;
  border-radius: 23px;
  color: #fff;
  font-family: var(--wz-font-body);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 1px;
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.content__save-btn:hover {
  background: var(--wz-orange-dark);
  box-shadow: 0 6px 20px rgba(255, 107, 53, 0.3);
}

.content__save-btn--busy {
  opacity: 0.7;
  cursor: not-allowed;
}

/* Info card (profile + security form) */
.info-card {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 14px;
  padding: 6px 0;
}

.info-card__row {
  display: flex;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid var(--wz-border);
  gap: 16px;
}

.info-card__row:last-child {
  border-bottom: none;
}

.info-card__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--wz-text-muted);
  min-width: 64px;
  flex-shrink: 0;
}

.info-card__value {
  font-size: 14px;
  color: var(--wz-text);
  flex: 1;
}

.info-card__input {
  flex: 1;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  background: var(--wz-bg);
  font-family: var(--wz-font-body);
  font-size: 14px;
  color: var(--wz-text);
  outline: none;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
  box-sizing: border-box;
}

.info-card__input:focus {
  border-color: var(--wz-orange);
}

.info-card__input::placeholder {
  color: var(--wz-text-muted);
}

/* Address card */
.addr-card {
  position: relative;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 12px;
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.addr-card:hover {
  border-color: var(--wz-orange-muted);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.addr-card--default {
  border-color: rgba(255, 107, 53, 0.25);
}

.addr-card__badge {
  position: absolute;
  top: 12px;
  right: 16px;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 8px;
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
  letter-spacing: 0.04em;
}

.addr-card__top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.addr-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--wz-text);
}

.addr-card__phone {
  font-size: 13px;
  color: var(--wz-text-soft);
}

.addr-card__detail {
  font-size: 13px;
  color: var(--wz-text);
  line-height: 1.5;
  margin-bottom: 14px;
}

.addr-card__actions {
  display: flex;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--wz-border);
}

.addr-card__action {
  padding: 4px 14px;
  font-size: 12px;
  font-weight: 500;
  color: var(--wz-text-soft);
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  border-radius: 8px;
  cursor: pointer;
  font-family: var(--wz-font-body);
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              color var(--wz-duration-fast) var(--wz-ease-out),
              border-color var(--wz-duration-fast) var(--wz-ease-out);
}

.addr-card__action:hover {
  background: var(--wz-orange-muted);
  color: var(--wz-orange);
  border-color: var(--wz-orange-muted);
}

.addr-card__action--danger:hover {
  background: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  border-color: rgba(231, 76, 60, 0.2);
}

/* Loading & Empty */
.content__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: var(--wz-text-muted);
  font-size: 14px;
}

.content__loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid var(--wz-border);
  border-top-color: var(--wz-orange);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.content__empty {
  text-align: center;
  padding: 60px 24px;
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 14px;
}

.content__empty-icon {
  color: var(--wz-text-muted);
  margin-bottom: 16px;
  opacity: 0.5;
}

.content__empty-text {
  font-size: 15px;
  color: var(--wz-text);
  margin-bottom: 6px;
}

.content__empty-hint {
  font-size: 13px;
  color: var(--wz-text-muted);
}

/* ═══ Dialog ═══ */
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.15s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.dialog {
  background: var(--wz-bg-card);
  border: 1px solid var(--wz-border);
  border-radius: 16px;
  padding: 28px 24px;
  width: 380px;
  max-width: 90vw;
}

.dialog__title {
  font-family: var(--wz-font-display);
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
  font-weight: 500;
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
  transition: border-color var(--wz-duration-fast) var(--wz-ease-out);
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
  font-weight: 500;
  cursor: pointer;
  font-family: var(--wz-font-body);
  transition: background var(--wz-duration-fast) var(--wz-ease-out),
              box-shadow var(--wz-duration-fast) var(--wz-ease-out);
}

.dialog__btn--cancel {
  background: var(--wz-bg);
  border: 1px solid var(--wz-border);
  color: var(--wz-text);
}

.dialog__btn--cancel:hover {
  background: var(--wz-bg-hover);
}

.dialog__btn--confirm {
  background: var(--wz-orange);
  border: none;
  color: #fff;
}

.dialog__btn--confirm:hover {
  background: var(--wz-orange-dark);
  box-shadow: 0 4px 14px rgba(255, 107, 53, 0.3);
}

/* ═══ Responsive ═══ */
@media (max-width: 860px) {
  .profile-layout {
    flex-direction: column;
    padding: 20px 16px 60px;
  }
  .sidebar {
    width: 100%;
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 10px;
  }
  .sidebar__card {
    width: 100%;
  }
  .sidebar__nav {
    flex: 1;
    min-width: 0;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 4px;
    padding: 6px;
  }
  .sidebar__nav-btn {
    flex: 0 0 auto;
    padding: 8px 14px;
    font-size: 13px;
  }
  .sidebar__nav-arrow {
    display: none;
  }
  .sidebar__extras {
    display: none;
  }
}

@media (max-width: 480px) {
  .content__panel-header {
    flex-direction: column;
  }
  .info-card__row {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
    padding: 12px 16px;
  }
  .addr-card {
    padding: 16px;
  }
  .addr-card__actions {
    flex-wrap: wrap;
  }
}
</style>
