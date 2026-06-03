import request from './request'
import type { PageResult } from './product'

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage: string
  specInfo: string | null
  price: number
  quantity: number
  subtotal: number
}

export interface Order {
  id: number
  userId: number
  orderNo: string
  totalAmount: number
  status: number
  paymentMethod: string | null
  paymentTime: string | null
  deliveryTime: string | null
  receiveTime: string | null
  logisticsCompany: string | null
  logisticsNo: string | null
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  createdTime: string
  updatedTime: string
  items: OrderItem[] | null
}

export function getAdminOrderList(params: {
  status?: number
  orderNo?: string
  receiverName?: string
  receiverPhone?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}) {
  return request.get<any, PageResult<Order>>('/order/admin/list', { params })
}

export function updateOrderStatus(id: number, status: number) {
  return request.put<void>('/order/admin/status', null, { params: { id, status } })
}

export function shipOrder(id: number, logisticsCompany: string, logisticsNo: string) {
  return request.put<void>('/order/admin/ship', null, { params: { id, logisticsCompany, logisticsNo } })
}

export function cancelOrder(id: number) {
  return request.put<void>('/order/admin/cancel', null, { params: { id } })
}

export function exportOrders(params: {
  status?: number
  orderNo?: string
  receiverName?: string
  receiverPhone?: string
  startTime?: string
  endTime?: string
}) {
  return request.get('/order/admin/export', { params, responseType: 'blob' })
}
