import request from './request'

export function addLogisticsTrack(data: {
  orderId: number
  status: string
  message?: string
  location?: string
  trackTime?: string
}) {
  return request.post<void>('/order/admin/logistics/track', data)
}
