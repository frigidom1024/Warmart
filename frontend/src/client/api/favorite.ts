import request from './request'

export interface FavoriteItem {
  id: number
  userId: number
  productId: number
  createdTime: string
  productName: string | null
  productPrice: number | null
  productOldPrice: number | null
  productImage: string | null
  productTag: string | null
  productSales: number | null
}

export function getFavoriteList(params: { page?: number; size?: number }) {
  return request.get<import('./product').PageResult<FavoriteItem>>('/product/favorite/list', { params })
}

export function addFavorite(productId: number) {
  return request.post<void>('/product/favorite/add', null, { params: { productId } })
}

export function cancelFavorite(productId: number) {
  return request.delete<void>('/product/favorite/cancel/' + productId)
}
