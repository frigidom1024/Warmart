<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { getAdminUserList, updateUserStatus, getUserDetail } from '@/api/user'
import type { User } from '@/api/user'

const route = useRoute()
const router = useRouter()

// --- Table state ---
const users = ref<User[]>([])
const total = ref(0)
const loading = ref(false)

// --- Search state (synced with URL query params) ---
const query = ref({
  username: '',
  phone: '',
  nickname: '',
  status: undefined as number | undefined,
  page: 1,
  size: 10
})

// --- Detail dialog ---
const detailDialogVisible = ref(false)
const detailUser = ref<User | null>(null)

// --- Role display config ---
const roleMap: Record<string, { type: string; text: string }> = {
  USER: { type: 'info', text: '用户' },
  ADMIN: { type: 'warning', text: '管理员' },
  SUPER_ADMIN: { type: 'danger', text: '超级管理员' }
}

// --- URL sync ---
function syncQueryFromRoute() {
  const q = route.query
  query.value.username = (q.username as string) || ''
  query.value.phone = (q.phone as string) || ''
  query.value.nickname = (q.nickname as string) || ''
  query.value.status = q.status ? Number(q.status) : undefined
  query.value.page = q.page ? Number(q.page) : 1
}

function pushQueryToUrl() {
  const params: Record<string, string> = {}
  if (query.value.username) params.username = query.value.username
  if (query.value.phone) params.phone = query.value.phone
  if (query.value.nickname) params.nickname = query.value.nickname
  if (query.value.status !== undefined) params.status = String(query.value.status)
  params.page = String(query.value.page)
  router.replace({ query: params })
}

// --- Data loading ---
async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.username) params.username = query.value.username
    if (query.value.phone) params.phone = query.value.phone
    if (query.value.nickname) params.nickname = query.value.nickname
    if (query.value.status !== undefined) params.status = query.value.status
    const res = await getAdminUserList(params)
    users.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

// --- Search / Reset ---
function handleSearch() {
  query.value.page = 1
  pushQueryToUrl()
  loadData()
}

function handleReset() {
  query.value.username = ''
  query.value.phone = ''
  query.value.nickname = ''
  query.value.status = undefined
  query.value.page = 1
  router.replace({ query: {} })
  loadData()
}

// --- Pagination ---
function handlePageChange(page: number) {
  query.value.page = page
  pushQueryToUrl()
  loadData()
}

// --- Toggle status ---
async function handleToggleStatus(user: User) {
  const newStatus = user.status === 0 ? 1 : 0
  const actionText = newStatus === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定${actionText}用户「${user.nickname || user.username}」吗？`,
      '确认操作',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
    await updateUserStatus(user.id, newStatus)
    ElMessage.success(`用户已${actionText}`)
    await loadData()
  } catch {}
}

// --- Detail ---
async function openDetail(user: User) {
  try {
    const detail = await getUserDetail(user.id)
    detailUser.value = detail
    detailDialogVisible.value = true
  } catch {}
}

onMounted(() => {
  syncQueryFromRoute()
  loadData()
})
</script>

<template>
  <div>
    <div class="page-header"><h2>用户管理</h2></div>
    <el-card>
      <!-- Search -->
      <div class="search-bar">
        <el-input v-model="query.username" placeholder="用户名" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-input v-model="query.phone" placeholder="手机号" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-input v-model="query.nickname" placeholder="昵称" clearable class="search-input" @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="状态" clearable class="search-select" @change="handleSearch">
          <el-option label="全部" :value="undefined" />
          <el-option label="正常" :value="0" />
          <el-option label="已禁用" :value="1" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <!-- Table -->
      <el-table :data="users" v-loading="loading" stripe style="width:100%" row-key="id">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="140" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="头像" width="60" align="center">
          <template #default="{ row }">
            <el-avatar :src="row.avatar" :size="32">
              <el-icon><UserFilled /></el-icon>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="roleMap[row.role]?.type" size="small">
              {{ roleMap[row.role]?.text || row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'danger'" size="small">
              {{ row.status === 0 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              size="small"
              :type="row.status === 0 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 0 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="500px">
      <el-descriptions v-if="detailUser" :column="2" border>
        <el-descriptions-item label="ID" span="2">{{ detailUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ detailUser.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detailUser.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detailUser.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detailUser.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="roleMap[detailUser.role]?.type" size="small">
            {{ roleMap[detailUser.role]?.text || detailUser.role }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailUser.status === 0 ? 'success' : 'danger'" size="small">
            {{ detailUser.status === 0 ? '正常' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间" span="2">{{ detailUser.createdTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" span="2">{{ detailUser.updatedTime }}</el-descriptions-item>
        <el-descriptions-item label="头像" span="2">
          <el-avatar :src="detailUser.avatar" :size="60" v-if="detailUser.avatar" />
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.search-bar { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; align-items: center; }
.search-input { width: 160px; }
.search-select { width: 140px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
