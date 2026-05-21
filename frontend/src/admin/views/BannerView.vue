<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBannerList, addBanner, updateBanner, deleteBanner } from '../api/banner'
import type { Banner } from '../api/banner'

const banners = ref<Banner[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const form = ref<Partial<Banner>>({
  title: '', subtitle: '', description: '', imageUrl: '', linkUrl: '',
  btnText: '', align: 'left', sort: 0, status: 1
})

async function loadData() {
  loading.value = true
  try {
    banners.value = await getBannerList()
  } catch {} finally {
    loading.value = false
  }
}

function openAddDialog() {
  isEdit.value = false
  form.value = { title: '', subtitle: '', description: '', imageUrl: '', linkUrl: '', btnText: '', align: 'left', sort: 0, status: 1 }
  dialogVisible.value = true
}

function openEditDialog(item: Banner) {
  isEdit.value = true
  form.value = { ...item }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.title || !form.value.imageUrl) {
    ElMessage.warning('请填写标题和图片URL')
    return
  }
  try {
    if (isEdit.value) {
      await updateBanner(form.value)
      ElMessage.success('更新成功')
    } else {
      await addBanner(form.value)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch {}
}

async function handleDelete(item: Banner) {
  try {
    await ElMessageBox.confirm(`确定删除轮播「${item.title}」吗？`, '确认删除', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await deleteBanner(item.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch {}
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>轮播管理</h2>
    </div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>首页轮播图</span>
          <el-button type="primary" size="small" @click="openAddDialog()">添加轮播</el-button>
        </div>
      </template>
      <el-table :data="banners" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="图片" width="100">
          <template #default="{ row }">
            <el-image
              :src="row.imageUrl" fit="cover"
              style="width: 60px; height: 40px; border-radius: 4px;"
              @error="true"
            >
              <template #error>
                <div class="img-error">无图</div>
              </template>
            </el-image>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="subtitle" label="副标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column prop="align" label="对齐" width="80">
          <template #default="{ row }">
            <el-tag size="small">{{ row.align }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
            <el-button type="danger" size="small" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑轮播' : '添加轮播'" width="600px">
      <el-form ref="formRef" :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="主标题" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="选填" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
        <el-form-item label="图片URL" required>
          <el-input v-model="form.imageUrl" placeholder="请填写图片URL" />
        </el-form-item>
        <el-form-item label="图片预览" v-if="form.imageUrl">
          <el-image :src="form.imageUrl" fit="contain" style="width: 200px; height: 100px; border-radius: 4px; border: 1px solid #eee;">
            <template #error><span style="color:#999">加载失败</span></template>
          </el-image>
        </el-form-item>
        <el-form-item label="链接地址">
          <el-input v-model="form.linkUrl" placeholder="点击跳转链接，选填" />
        </el-form-item>
        <el-form-item label="按钮文字">
          <el-input v-model="form.btnText" placeholder="选填" style="width: 160px" />
        </el-form-item>
        <el-form-item label="对齐方式">
          <el-select v-model="form.align" style="width: 120px">
            <el-option label="左对齐" value="left" />
            <el-option label="居中" value="center" />
            <el-option label="右对齐" value="right" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: var(--wa-text-primary, #303133); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.img-error { width: 60px; height: 40px; display: flex; align-items: center; justify-content: center; background: #f5f7fa; font-size: 12px; color: #999; border-radius: 4px; }
</style>
