import request from './request'

export interface RefundApplication {
  id: number
  orderId: number
  userId: number
  reason: string
  amount: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  adminReply: string | null
  handledTime: string | null
  createdTime: string
  updatedTime: string
  orderNo: string
  receiverName: string
}

export function getRefundList(params: {
  status?: string
  page?: number
  size?: number
}) {
  return request.get<any, import('./product').PageResult<RefundApplication>>('/order/admin/refund/list', { params })
}

export function approveRefund(id: number, adminReply?: string) {
  return request.post<void>('/order/admin/refund/approve', { id, adminReply })
}

export function rejectRefund(id: number, adminReply?: string) {
  return request.post<void>('/order/admin/refund/reject', { id, adminReply })
}
