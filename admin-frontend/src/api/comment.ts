import request from './request'

export interface CommentItem {
  id: number
  productId: number
  productName: string
  userId: number
  username: string
  content: string
  rating: number
  imageUrls: string[]
  createdTime: string
}

export function getAdminCommentList(params: Record<string, any>) {
  return request.get<any, CommentItem[]>('/product/admin/comment/list', { params })
}

export function deleteAdminComment(id: number) {
  return request.delete<void>(`/product/admin/comment/${id}`)
}
