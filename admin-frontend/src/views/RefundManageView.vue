<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRefundList, approveRefund, rejectRefund } from '@/api/refund'
import type { RefundApplication } from '@/api/refund'

const list = ref<RefundApplication[]>([])
const total = ref(0)
const loading = ref(false)
const query = ref({ status: '' as string | undefined, page: 1, size: 20 })
const actionDialogVisible = ref(false)
const actionTarget = ref<RefundApplication | null>(null)
const actionType = ref<'approve' | 'reject'>('approve')
const actionForm = ref({ adminReply: '' })

const statusMap: Record<string, { type: string; text: string }> = {
  PENDING: { type: 'warning', text: '待处理' },
  APPROVED: { type: 'success', text: '已通过' },
  REJECTED: { type: 'danger', text: '已拒绝' },
  CANCELLED: { type: 'info', text: '已取消' }
}

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: query.value.page, size: query.value.size }
    if (query.value.status) params.status = query.value.status
    const res = await getRefundList(params)
    list.value = res.records
    total.value = res.total
  } catch {} finally { loading.value = false }
}

function handleSearch() { query.value.page = 1; loadData() }

function openActionDialog(row: RefundApplication, type: 'approve' | 'reject') {
  actionTarget.value = row
  actionType.value = type
  actionForm.value = { adminReply: '' }
  actionDialogVisible.value = true
}

async function handleAction() {
  if (!actionTarget.value) return
  const target = actionTarget.value
  try {
    if (actionType.value === 'approve') {
      await ElMessageBox.confirm(`确定同意订单 ${target.orderNo} 的退款申请？`, '确认操作')
      await approveRefund(target.id, actionForm.value.adminReply)
      ElMessage.success('退款已通过')
    } else {
      if (!actionForm.value.adminReply) {
        ElMessage.warning('拒绝退款请填写理由')
        return
      }
      await ElMessageBox.confirm(`确定拒绝订单 ${target.orderNo} 的退款申请？`, '确认操作')
      await rejectRefund(target.id, actionForm.value.adminReply)
      ElMessage.success('已拒绝退款')
    }
    actionDialogVisible.value = false
    await loadData()
  } catch {}
}

onMounted(loadData)
</script>

<template>
  <div>
    <div class="page-header"><h2>退款管理</h2></div>
    <el-card>
      <div class="search-bar">
        <el-select v-model="query.status" placeholder="退款状态" clearable class="search-select" @change="handleSearch">
          <el-option label="全部" :value="undefined" />
          <el-option label="待处理" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe style="width:100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="receiverName" label="收货人" width="100" />
        <el-table-column label="退款金额" width="100"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="adminReply" label="管理员回复" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button size="small" type="success" @click="openActionDialog(row, 'approve')">通过</el-button>
              <el-button size="small" type="danger" @click="openActionDialog(row, 'reject')">拒绝</el-button>
            </template>
            <el-tag v-else size="small" :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination v-model:current-page="query.page" :page-size="query.size" :total="total" layout="prev, pager, next, total" @current-change="handleSearch" />
      </div>
    </el-card>

    <el-dialog v-model="actionDialogVisible" :title="actionType === 'approve' ? '通过退款' : '拒绝退款'" width="450px">
      <el-form label-width="80px">
        <el-form-item label="订单号">
          <span>{{ actionTarget?.orderNo }}</span>
        </el-form-item>
        <el-form-item label="退款原因">
          <span>{{ actionTarget?.reason }}</span>
        </el-form-item>
        <el-form-item label="退款金额">
          <span>¥{{ actionTarget?.amount }}</span>
        </el-form-item>
        <el-form-item :label="actionType === 'approve' ? '回复说明' : '拒绝理由'" required>
          <el-input v-model="actionForm.adminReply" type="textarea" :rows="3" :placeholder="actionType === 'reject' ? '请填写拒绝理由（必填）' : '选填回复说明'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAction">{{ actionType === 'approve' ? '确认通过' : '确认拒绝' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; color: #303133; }
.search-bar { display: flex; gap: 12px; margin-bottom: 20px; }
.search-select { width: 160px; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
