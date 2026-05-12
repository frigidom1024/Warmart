import request from '@/api/request'

export const getCartList = () => request.get('/order/cart/list')
export const addToCart = (data) => request.post('/order/cart/add', data)
export const updateCart = (data) => request.put('/order/cart/update', data)
export const deleteCartItem = (id) => request.delete(`/order/cart/delete/${id}`)
export const checkCartItem = (data) => request.put('/order/cart/check', data)
export const checkAllCart = (data) => request.put('/order/cart/checkAll', data)
