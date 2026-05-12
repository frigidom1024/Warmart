<template>
  <div class="favorites-page">
    <div class="page-header">
      <h2>我的收藏</h2>
    </div>

    <div v-loading="loading">
      <template v-if="favoriteList.length > 0">
        <el-row :gutter="20">
          <el-col
            v-for="item in favoriteList"
            :key="item.id || item.productId"
            :xs="12"
            :sm="8"
            :md="6"
            :lg="6"
            :xl="6"
            class="product-col"
          >
            <div class="fav-card" @click="goDetail(item)">
              <div class="fav-image-wrapper">
                <el-image
                  :src="item.image || item.coverImage || item.cover_img || ''"
                  fit="cover"
                  loading="lazy"
                >
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon :size="32"><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
              <div class="fav-info">
                <h3 class="fav-name" :title="item.name || item.productName">
                  {{ item.name || item.productName }}
                </h3>
                <div class="fav-price">
                  <span class="price-symbol">¥</span>
                  <span class="price-value">{{ formatPrice(item.price) }}</span>
                </div>
                <el-button
                  size="small"
                  type="danger"
                  plain
                  class="unfav-btn"
                  @click.stop="handleUnfavorite(item)"
                >
                  取消收藏
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </template>
      <el-empty v-else-if="!loading" description="暂无收藏商品" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { getFavorites, unfavorite } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const favoriteList = ref([])

const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await getFavorites({ page: 1, pageSize: 100 })
    const data = res.data || res
    if (data && data.records) {
      favoriteList.value = data.records
    } else if (Array.isArray(data)) {
      favoriteList.value = data
    } else {
      favoriteList.value = []
    }
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const formatPrice = (price) => {
  if (price === null || price === undefined) return '0.00'
  return Number(price).toFixed(2)
}

const goDetail = (item) => {
  const productId = item.productId || item.id
  if (productId) {
    router.push(`/product/detail/${productId}`)
  }
}

const handleUnfavorite = async (item) => {
  const productId = item.productId || item.id
  try {
    await ElMessageBox.confirm('确定要取消收藏该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    const res = await unfavorite(productId)
    if (res.code === 200) {
      ElMessage.success('已取消收藏')
      await fetchFavorites()
    }
  } catch {
    // Cancelled or error handled by interceptor
  }
}

onMounted(() => {
  fetchFavorites()
})
</script>

<style scoped>
.favorites-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  font-size: 20px;
  color: #303133;
  margin: 0;
}

.product-col {
  margin-bottom: 20px;
}

.fav-card {
  background: #fff;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s ease, box-shadow 0.25s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.fav-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.fav-image-wrapper {
  width: 100%;
  padding-top: 100%;
  position: relative;
  background: #f5f5f5;
  overflow: hidden;
}

.fav-image-wrapper .el-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  background: #f5f5f5;
}

.fav-info {
  padding: 12px 14px 16px;
}

.fav-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 39px;
}

.fav-price {
  color: #f56c6c;
  font-weight: 700;
  margin-bottom: 10px;
}

.price-symbol {
  font-size: 12px;
  margin-right: 1px;
}

.price-value {
  font-size: 18px;
}

.unfav-btn {
  width: 100%;
}

@media (max-width: 768px) {
  .fav-info {
    padding: 8px 10px 12px;
  }

  .fav-name {
    font-size: 13px;
    min-height: auto;
  }

  .price-value {
    font-size: 16px;
  }
}
</style>
