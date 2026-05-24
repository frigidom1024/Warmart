import request from './request'

export interface Consultation {
  id: number
  userId: number
  productId: number
  question: string
  answer: string | null
  status: number
  createdTime: string
}

export function getConsultationList() {
  return request.get<any, Consultation[]>('/user/consultation/admin/list')
}

export function replyConsultation(id: number, answer: string) {
  return request.put<void>('/user/consultation/admin/reply', null, { params: { id, answer } })
}
