<template>
  <div class="cart-container">
    <h2>购物车</h2>

    <div v-if="!loading && cartList.length === 0" class="empty-cart">
      <el-empty description="购物车是空的">
        <el-button type="primary" @click="$router.push('/product/list')">去购物</el-button>
      </el-empty>
    </div>

    <template v-else>
      <el-table :data="cartList" v-loading="loading" style="width: 100%">
        <el-table-column width="50">
          <template #default="{ row }">
            <el-checkbox
              :model-value="row.checked === 1"
              @change="() => handleCheck(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="商品信息" min-width="300">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image
                :src="row.productImage"
                class="product-img"
                fit="cover"
              />
              <span class="product-name">{{ row.productName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="单价" width="100" align="center">
          <template #default="{ row }">
            ¥{{ row.price }}
          </template>
        </el-table-column>
        <el-table-column label="数量" width="180" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.quantity"
              :min="1"
              :max="999"
              size="small"
              @change="(newVal, oldVal) => handleQuantityChange(row, newVal, oldVal)"
            />
          </template>
        </el-table-column>
        <el-table-column label="小计" width="120" align="center">
          <template #default="{ row }">
            ¥{{ (row.price * row.quantity).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="cart-footer">
        <div class="footer-left">
          <el-checkbox
            :model-value="isAllChecked"
            :indeterminate="isIndeterminate"
            @change="handleCheckAll"
          >
            全选
          </el-checkbox>
        </div>
        <div class="footer-right">
          <span class="total-text">
            总计：<strong class="total-amount">&yen;{{ totalAmount }}</strong>
          </span>
          <el-button
            type="primary"
            size="large"
            :disabled="checkedCount === 0"
            @click="handleCheckout"
          >
            结算
          </el-button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getCartList, updateCart, deleteCartItem, checkCartItem, checkAllCart } from '@/api/cart'

const router = useRouter()
const loading = ref(false)
const cartList = ref([])

onMounted(() => {
  fetchCartList()
})

const fetchCartList = async () => {
  loading.value = true
  try {
    const res = await getCartList()
    if (res.code === 200) {
      cartList.value = res.data || []
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const checkedItems = computed(() => cartList.value.filter((item) => item.checked === 1))
const checkedCount = computed(() => checkedItems.value.length)
const isAllChecked = computed(() => cartList.value.length > 0 && checkedCount.value === cartList.value.length)
const isIndeterminate = computed(() => checkedCount.value > 0 && checkedCount.value < cartList.value.length)
const totalAmount = computed(() => checkedItems.value.reduce((sum, item) => sum + item.price * item.quantity, 0).toFixed(2))

const handleCheck = async (row) => {
  const newChecked = row.checked === 1 ? 0 : 1
  const oldChecked = row.checked
  try {
    await checkCartItem({ id: row.id, checked: newChecked })
    row.checked = newChecked
  } catch {
    row.checked = oldChecked
  }
}

const handleCheckAll = async (checked) => {
  const newChecked = checked ? 1 : 0
  try {
    await checkAllCart({ checked: newChecked })
    cartList.value.forEach((item) => {
      item.checked = newChecked
    })
  } catch {
    await fetchCartList()
  }
}

const handleQuantityChange = async (row, newQuantity, oldQuantity) => {
  try {
    await updateCart({ id: row.id, quantity: newQuantity })
  } catch {
    row.quantity = oldQuantity
    ElMessage.error('更新数量失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCartItem(row.id)
    cartList.value = cartList.value.filter((item) => item.id !== row.id)
    ElMessage.success('删除成功')
  } catch {
    // Cancelled or error handled by interceptor
  }
}

const handleCheckout = () => {
  const ids = checkedItems.value.map((item) => item.id).join(',')
  router.push({ path: '/order/create', query: { cartItemIds: ids } })
}
</script>

<style scoped>
.cart-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.cart-container h2 {
  margin-bottom: 20px;
  font-size: 20px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  flex-shrink: 0;
  background: #f5f5f5;
}

.product-name {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-height: 1.4;
}

.cart-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #fff;
  margin-top: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.total-text {
  font-size: 16px;
}

.total-amount {
  font-size: 22px;
  color: #e4393c;
}

.empty-cart {
  padding: 60px 0;
}
</style>
