import request from './request'

export interface Notice {
  id: number
  title: string
  content: string | null
  type: string
  status: number
  createdTime: string
}

export function getNoticeList(type?: string) {
  return request.get<Notice[]>('/user/notice/list', { params: { type } })
}

export function getNoticePage(params: { type?: string; page?: number; size?: number }) {
  return request.get<import('./product').PageResult<Notice>>('/user/notice/page', { params })
}

export function getNoticeDetail(id: number) {
  return request.get<Notice>('/user/notice/detail/' + id)
}
