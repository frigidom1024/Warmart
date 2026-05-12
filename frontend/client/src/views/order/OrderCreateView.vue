<template>
  <div class="order-create-container">
    <h2>确认订单</h2>

    <el-card class="section-card" v-loading="loadingItems">
      <template #header>
        <span>收货地址</span>
      </template>
      <el-form :model="addressForm" label-width="100px">
        <el-form-item label="收货人" required>
          <el-input v-model="addressForm.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="addressForm.receiverPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="收货地址" required>
          <el-input
            v-model="addressForm.receiverAddress"
            type="textarea"
            :rows="2"
            placeholder="请输入详细地址"
          />
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="section-card" v-loading="loadingItems">
      <template #header>
        <span>商品信息</span>
      </template>
      <el-table :data="cartItems" style="width: 100%">
        <el-table-column label="商品" min-width="300">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                :src="row.productImage"
                class="product-img"
                fit="cover"
              />
              <span>{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="center">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">
            {{ row.quantity }}
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="center">
          <template #default="{ row }">
            ¥{{ (row.price * row.quantity).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="section-card">
      <template #header>
        <span>支付方式</span>
      </template>
      <el-radio-group v-model="paymentMethod">
        <el-radio value="alipay">支付宝</el-radio>
        <el-radio value="wechat">微信支付</el-radio>
        <el-radio value="card">银行卡</el-radio>
      </el-radio-group>
    </el-card>

    <div class="order-footer">
      <div class="total-area">
        <span>订单总计：</span>
        <strong class="total-amount">&yen;{{ totalAmount }}</strong>
      </div>
      <el-button
        type="primary"
        size="large"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交订单
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCartList } from '@/api/cart'
import { createOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()
const loadingItems = ref(false)
const submitting = ref(false)
const cartItems = ref([])
const paymentMethod = ref('alipay')

const addressForm = ref({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const cartItemIds = computed(() => {
  const ids = route.query.cartItemIds
  if (!ids) return []
  return String(ids)
    .split(',')
    .map(Number)
    .filter((id) => !isNaN(id))
})

onMounted(async () => {
  if (cartItemIds.value.length === 0) {
    ElMessage.warning('没有选择商品')
    router.push('/cart')
    return
  }
  await fetchCartItems()
})

const fetchCartItems = async () => {
  loadingItems.value = true
  try {
    const res = await getCartList()
    if (res.code === 200) {
      const allItems = res.data || []
      cartItems.value = allItems.filter((item) =>
        cartItemIds.value.includes(item.id)
      )
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loadingItems.value = false
  }
}

const totalAmount = computed(() =>
  cartItems.value
    .reduce((sum, item) => sum + item.price * item.quantity, 0)
    .toFixed(2)
)

const handleSubmit = async () => {
  if (!addressForm.value.receiverName.trim()) {
    ElMessage.warning('请填写收货人姓名')
    return
  }
  if (!addressForm.value.receiverPhone.trim()) {
    ElMessage.warning('请填写联系电话')
    return
  }
  if (!addressForm.value.receiverAddress.trim()) {
    ElMessage.warning('请填写收货地址')
    return
  }

  submitting.value = true
  try {
    const res = await createOrder({
      receiverName: addressForm.value.receiverName,
      receiverPhone: addressForm.value.receiverPhone,
      receiverAddress: addressForm.value.receiverAddress,
      paymentMethod: paymentMethod.value,
      cartItemIds: cartItemIds.value
    })
    if (res.code === 200) {
      const orderId =
        typeof res.data === 'object' ? res.data.id : res.data
      ElMessage.success('订单提交成功')
      router.push(`/order/detail/${orderId}`)
    }
  } catch {
    // Error handled by interceptor
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.order-create-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.order-create-container h2 {
  margin-bottom: 20px;
  font-size: 20px;
}

.section-card {
  margin-bottom: 20px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  flex-shrink: 0;
  background: #f5f5f5;
}

.order-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 20px;
  padding: 16px 24px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.total-area {
  font-size: 16px;
}

.total-amount {
  font-size: 24px;
  color: #e4393c;
}
</style>
