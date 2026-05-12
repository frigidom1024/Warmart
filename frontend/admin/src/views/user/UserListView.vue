<template>
  <div class="user-container">
    <!-- Search Bar -->
    <el-card shadow="hover" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名/昵称">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入用户名或昵称"
            clearable
            style="width: 260px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- User Table -->
    <el-card shadow="hover" class="table-card">
      <el-table :data="users" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="role" label="角色" width="130">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.role)">
              {{ roleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'">
              {{ row.status === 0 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              :type="row.status === 0 ? 'warning' : 'success'"
              size="small"
              :loading="togglingId === row.id"
              @click="handleToggle(row)"
            >
              {{ row.status === 0 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers as apiGetUsers, toggleUserStatus } from '@/api/admin'

const loading = ref(false)
const togglingId = ref(null)
const users = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const searchForm = reactive({
  keyword: ''
})

const roleTagType = (role) => {
  const map = {
    USER: '',
    ADMIN: 'warning',
    SUPER_ADMIN: 'danger'
  }
  return map[role] || 'info'
}

const roleLabel = (role) => {
  const map = {
    USER: '普通用户',
    ADMIN: '管理员',
    SUPER_ADMIN: '超级管理员'
  }
  return map[role] || role
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value
    }
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    const res = await apiGetUsers(params)
    if (Array.isArray(res)) {
      users.value = res
      total.value = res.length
    } else if (res && res.records) {
      users.value = res.records
      total.value = res.total || 0
    } else if (res && res.data) {
      users.value = Array.isArray(res.data) ? res.data : []
      total.value = res.total || users.value.length
    } else {
      users.value = []
      total.value = 0
    }
  } catch (err) {
    ElMessage.error('获取用户列表失败')
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  fetchUsers()
}

const handleReset = () => {
  searchForm.keyword = ''
  page.value = 1
  fetchUsers()
}

const handleToggle = async (row) => {
  const action = row.status === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${row.username}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    togglingId.value = row.id
    await toggleUserStatus(row.id)
    ElMessage.success(`${action}成功`)
    fetchUsers()
  } catch (err) {
    // cancelled by user or error — ignore
  } finally {
    togglingId.value = null
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
