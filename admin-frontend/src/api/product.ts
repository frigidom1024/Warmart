import request from './request'

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface Product {
  id: number
  categoryId: number
  name: string
  description: string
  price: number
  originalPrice: number | null
  tag: string | null
  stock: number
  sales: number
  mainImage: string
  status: number
  isRecommend: number
  hasSpec: number | null
  createdTime: string
  updatedTime: string
  specList: any[] | null
  imageList: any[] | null
}

export function getAdminProductList(params: {
  categoryId?: number
  status?: number
  page?: number
  size?: number
}) {
  return request.get<any, PageResult<Product>>('/product/admin/list', { params })
}

export function addProduct(data: Partial<Product>) {
  return request.post<void>('/product/admin/add', data)
}

export function updateProduct(data: Partial<Product>) {
  return request.put<void>('/product/admin/update', data)
}

export function deleteProduct(id: number) {
  return request.delete<void>('/product/admin/delete/' + id)
}
