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
  return request.get<Consultation[]>('/user/consultation/list')
}

export function addConsultation(data: { productId: number; question: string }) {
  return request.post<void>('/user/consultation/add', data)
}
