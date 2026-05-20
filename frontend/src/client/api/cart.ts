import request from './request'

export interface CartItem {
  id: number
  userId: number
  productId: number
  quantity: number
  checked: number
  createdTime: string
  updatedTime: string
  productName: string | null
  productPrice: number | null
  productOldPrice: number | null
  productImage: string | null
  productTag: string | null
}

export interface AddCartRequest {
  productId: number
  quantity: number
}

export interface UpdateQuantityRequest {
  id: number
  quantity: number
}

export interface CheckRequest {
  id: number
  checked: number
}

export function getCartList() {
  return request.get<CartItem[]>('/order/cart/list')
}

export function addToCart(data: AddCartRequest) {
  return request.post<void>('/order/cart/add', data)
}

export function updateCartQuantity(data: UpdateQuantityRequest) {
  return request.put<void>('/order/cart/update', data)
}

export function deleteCartItem(id: number) {
  return request.delete<void>('/order/cart/delete/' + id)
}

export function checkCartItem(data: CheckRequest) {
  return request.put<void>('/order/cart/check', data)
}

export function checkAllCart(checked: number) {
  return request.put<void>('/order/cart/checkAll', { checked })
}
