import request from './request'
import type { PageResult } from './product'

export interface Notice {
  id: number
  title: string
  content: string | null
  type: string
  status: number
  createdTime: string
}

export function getAdminNoticePage(params: {
  type?: string
  page?: number
  size?: number
}) {
  return request.get<any, PageResult<Notice>>('/user/notice/admin/page', { params })
}

export function addNotice(data: Partial<Notice>) {
  return request.post<void>('/user/notice/admin/add', data)
}

export function updateNotice(data: Partial<Notice>) {
  return request.put<void>('/user/notice/admin/update', data)
}

export function deleteNotice(id: number) {
  return request.delete<void>('/user/notice/admin/delete/' + id)
}
