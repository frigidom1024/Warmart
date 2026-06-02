import request from './request'

export interface Comment {
  id: number
  productId: number
  userId: number
  content: string
  rating: number
  imageUrls: string | null
  createdTime: string
  userNickname: string | null
  userAvatar: string | null
}

export function getCommentList(productId: number, params: { page?: number; size?: number }) {
  return request.get<import('./product').PageResult<Comment>>('/product/comment/list/' + productId, { params })
}

export function addComment(data: { productId: number; content: string; rating: number; imageUrls?: string }) {
  return request.post<void>('/product/comment/add', data)
}

export function uploadCommentImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/product/comment/upload-image', formData)
}
