import request from './request'

export interface CommentItem {
  id: number
  productId: number
  productName: string
  userId: number
  username: string
  content: string
  rating: number
  imageUrls: string
  createdTime: string
}

export function getAdminCommentList(params: {
  productName?: string
  username?: string
  rating?: number
  page?: number
  size?: number
}) {
  return request.get<any, { records: CommentItem[]; total: number }>('/product/admin/comment/list', { params })
}

export function deleteAdminComment(id: number) {
  return request.delete<void>(`/product/admin/comment/${id}`)
}
