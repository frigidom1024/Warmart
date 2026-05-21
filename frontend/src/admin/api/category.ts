import request from './request'

export interface Category {
  id: number
  name: string
  parentId: number | null
  sort: number
  imageUrl: string | null
  status: number | null
  children?: Category[]
}

export function getCategoryList() {
  return request.get<any, Category[]>('/product/category/admin/list')
}

export function addCategory(data: Partial<Category>) {
  return request.post<void>('/product/category/admin/add', data)
}

export function updateCategory(data: Partial<Category>) {
  return request.put<void>('/product/category/admin/update', data)
}

export function deleteCategory(id: number) {
  return request.delete<void>('/product/category/admin/delete/' + id)
}
