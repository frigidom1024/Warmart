import request from './request'

export interface Feedback {
  id: number
  userId: number
  type: string
  content: string
  reply: string | null
  replyTime: string | null
  status: number
  createdTime: string
}

export function getFeedbackList() {
  return request.get<any, Feedback[]>('/user/feedback/admin/list')
}

export function replyFeedback(id: number, replyContent: string) {
  return request.put<void>('/user/feedback/admin/reply', null, { params: { id, replyContent } })
}
