import request from './request'

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
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  createdTime: string
  updatedTime: string
  items: OrderItem[] | null
}

export interface CreateOrderRequest {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  paymentMethod: string
  cartItemIds: number[]
}

export function createOrder(data: CreateOrderRequest) {
  return request.post<Order>('/order/create', data)
}

export function getOrderList(params: {
  status?: number
  page?: number
  size?: number
}) {
  return request.get<import('./product').PageResult<Order>>('/order/list', { params })
}

export function getOrderDetail(id: number) {
  return request.get<Order>('/order/detail/' + id)
}

export function cancelOrder(id: number) {
  return request.post<void>('/order/cancel/' + id)
}

export function confirmOrder(id: number) {
  return request.post<void>('/order/confirm/' + id)
}

export function applyRefund(id: number) {
  return request.post<void>('/order/refund/' + id)
}

export function payOrder(data: { orderId: number; method: string }) {
  return request.post<void>('/order/payment/pay', data)
}
