<template>
  <div class="notice-page">
    <div class="page-header">
      <h2>系统公告</h2>
    </div>

    <!-- Filter tabs -->
    <div class="filter-bar">
      <el-radio-group v-model="activeFilter" @change="handleFilterChange">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="system">系统通知</el-radio-button>
        <el-radio-button value="promotion">促销活动</el-radio-button>
      </el-radio-group>
    </div>

    <div v-loading="loading">
      <div v-if="noticeList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无公告" />
      </div>

      <el-card
        v-for="notice in noticeList"
        :key="notice.id"
        class="notice-card"
        shadow="hover"
      >
        <div class="notice-header">
          <div class="notice-title-row">
            <el-tag v-if="notice.type" :type="noticeTypeTag(notice.type)" size="small" effect="dark">
              {{ noticeTypeLabel(notice.type) }}
            </el-tag>
            <span class="notice-title">{{ notice.title }}</span>
          </div>
          <span class="notice-date">{{ formatDate(notice.createdAt) }}</span>
        </div>

        <div class="notice-content-wrapper">
          <div
            :class="['notice-content', { expanded: notice._expanded }]"
            ref="contentRefs"
          >
            {{ notice.content }}
          </div>
          <el-button
            v-if="notice.content && notice.content.length > 150"
            type="primary"
            link
            size="small"
            @click="toggleExpand(notice)"
          >
            {{ notice._expanded ? '收起' : '展开' }}
            <el-icon :size="12" :class="{ expanded: notice._expanded }">
              <ArrowDown />
            </el-icon>
          </el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { getNoticeList } from '@/api/user'

const loading = ref(false)
const noticeList = ref([])
const activeFilter = ref('')

const noticeTypeMap = {
  system: { label: '系统通知', tag: 'primary' },
  promotion: { label: '促销活动', tag: 'danger' }
}

const noticeTypeLabel = (type) => noticeTypeMap[type]?.label || type
const noticeTypeTag = (type) => noticeTypeMap[type]?.tag || ''

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const fetchNotices = async () => {
  loading.value = true
  try {
    const params = {}
    if (activeFilter.value) {
      params.type = activeFilter.value
    }
    const res = await getNoticeList(params)
    const data = res.data || res
    const list = Array.isArray(data) ? data : (data.records || [])

    // Add expand state to each notice
    noticeList.value = list.map((item) => ({
      ...item,
      _expanded: false
    }))
  } catch {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  fetchNotices()
}

const toggleExpand = (notice) => {
  notice._expanded = !notice._expanded
}

onMounted(() => {
  fetchNotices()
})
</script>

<style scoped>
.notice-page {
  max-width: 900px;
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

.filter-bar {
  margin-bottom: 20px;
}

.notice-card {
  margin-bottom: 12px;
  border-radius: 8px;
}

.notice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.notice-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.notice-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-date {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  flex-shrink: 0;
}

.notice-content-wrapper {
  padding-left: 4px;
}

.notice-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  word-break: break-word;
}

.notice-content.expanded {
  max-height: none;
}

.notice-content:not(.expanded) {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.empty-state {
  padding: 60px 0;
}
</style>
