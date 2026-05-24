import request from './request'

export interface Feedback {
  id: number
  userId: number
  type: string
  content: string
  contactInfo: string | null
  status: number
  replyContent: string | null
  replyTime: string | null
  createdTime: string
}

export function addFeedback(data: { type: string; content: string }) {
  return request.post<void>('/user/feedback/add', data)
}
