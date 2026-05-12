<template>
  <div class="address-page">
    <div class="page-header">
      <h2>收货地址</h2>
      <el-button type="primary" :icon="Plus" @click="openAddDialog">
        添加地址
      </el-button>
    </div>

    <div v-loading="loading">
      <div v-if="addressList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无收货地址，请添加" />
      </div>

      <el-card
        v-for="addr in addressList"
        :key="addr.id"
        class="address-card"
        shadow="hover"
      >
        <div class="address-main">
          <div class="address-info">
            <div class="address-name-row">
              <span class="receiver-name">{{ addr.receiverName }}</span>
              <span class="receiver-phone">{{ addr.receiverPhone }}</span>
              <el-tag v-if="addr.isDefault" size="small" type="danger">默认</el-tag>
            </div>
            <div class="address-detail">
              {{ addr.province }}{{ addr.city }}{{ addr.district }}
              {{ addr.detailAddress }}
            </div>
          </div>
          <div class="address-actions">
            <el-button
              v-if="!addr.isDefault"
              size="small"
              @click="handleSetDefault(addr.id)"
            >
              设为默认
            </el-button>
            <el-button size="small" @click="openEditDialog(addr)">
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(addr.id)">
              删除
            </el-button>
          </div>
        </div>
      </el-card>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEditing ? '编辑地址' : '添加地址'"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="addressForm"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>

        <el-form-item label="手机号码" prop="receiverPhone">
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入手机号码" maxlength="11" />
        </el-form-item>

        <el-form-item label="省/市/区" prop="province" required>
          <el-row :gutter="12">
            <el-col :span="8">
              <el-input v-model="addressForm.province" placeholder="省" />
            </el-col>
            <el-col :span="8">
              <el-input v-model="addressForm.city" placeholder="市" />
            </el-col>
            <el-col :span="8">
              <el-input v-model="addressForm.district" placeholder="区" />
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="详细地址" prop="detailAddress">
          <el-input
            v-model="addressForm.detailAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch v-model="addressForm.isDefault" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getAddressList,
  addAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from '@/api/user'

const loading = ref(false)
const submitting = ref(false)
const addressList = ref([])
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const defaultForm = () => ({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const addressForm = reactive(defaultForm())

const formRules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  detailAddress: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ]
}

const fetchAddressList = async () => {
  loading.value = true
  try {
    const res = await getAddressList()
    const data = res.data || res
    addressList.value = Array.isArray(data) ? data : (data.records || [])
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(addressForm, defaultForm())
  isEditing.value = false
  editingId.value = null
}

const openAddDialog = () => {
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (addr) => {
  isEditing.value = true
  editingId.value = addr.id
  addressForm.receiverName = addr.receiverName || ''
  addressForm.receiverPhone = addr.receiverPhone || ''
  addressForm.province = addr.province || ''
  addressForm.city = addr.city || ''
  addressForm.district = addr.district || ''
  addressForm.detailAddress = addr.detailAddress || ''
  addressForm.isDefault = addr.isDefault || false
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      receiverName: addressForm.receiverName,
      receiverPhone: addressForm.receiverPhone,
      province: addressForm.province,
      city: addressForm.city,
      district: addressForm.district,
      detailAddress: addressForm.detailAddress,
      isDefault: addressForm.isDefault
    }

    let res
    if (isEditing.value) {
      payload.id = editingId.value
      res = await updateAddress(payload)
    } else {
      res = await addAddress(payload)
    }

    if (res.code === 200) {
      ElMessage.success(isEditing.value ? '地址已更新' : '地址已添加')
      dialogVisible.value = false
      await fetchAddressList()
    }
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}

const handleSetDefault = async (id) => {
  try {
    const res = await setDefaultAddress(id)
    if (res.code === 200) {
      ElMessage.success('已设为默认地址')
      await fetchAddressList()
    }
  } catch {
    // Error handled by interceptor
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await deleteAddress(id)
    if (res.code === 200) {
      ElMessage.success('地址已删除')
      await fetchAddressList()
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}

onMounted(() => {
  fetchAddressList()
})
</script>

<style scoped>
.address-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.address-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.address-main {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.address-info {
  flex: 1;
  min-width: 0;
}

.address-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.receiver-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.receiver-phone {
  font-size: 14px;
  color: #666;
}

.address-detail {
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.empty-state {
  padding: 60px 0;
}

@media (max-width: 640px) {
  .address-main {
    flex-direction: column;
  }

  .address-actions {
    align-self: flex-end;
  }
}
</style>
