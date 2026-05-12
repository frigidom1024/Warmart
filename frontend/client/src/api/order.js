import request from '@/api/request'

export const createOrder = (data) => request.post('/order/create', data)
export const getOrders = (params) => request.get('/order/list', { params })
export const getOrderDetail = (id) => request.get(`/order/detail/${id}`)
export const cancelOrder = (id) => request.post(`/order/cancel/${id}`)
export const confirmOrder = (id) => request.post(`/order/confirm/${id}`)
export const applyRefund = (id) => request.post(`/order/refund/${id}`)
export const payOrder = (data) => request.post('/order/payment/pay', data)
