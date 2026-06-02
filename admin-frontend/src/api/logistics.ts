import request from './request'

export interface LogisticsTrack {
  id: number
  orderId: number
  status: string
  message: string | null
  location: string | null
  trackTime: string
  createdTime: string
}

export interface AddTrackRequest {
  orderId: number
  status: string
  message?: string
  location?: string
  trackTime?: string
}

export interface UpdateTrackRequest {
  status?: string
  message?: string
  location?: string
  trackTime?: string
}

export function addLogisticsTrack(data: AddTrackRequest) {
  return request.post<void>('/order/admin/logistics/track', data)
}

export function getLogisticsTracks(orderId: number) {
  return request.get<any, LogisticsTrack[]>(`/order/admin/logistics/tracks/${orderId}`)
}

export function updateLogisticsTrack(id: number, data: UpdateTrackRequest) {
  return request.put<void>(`/order/admin/logistics/track/${id}`, data)
}

export function deleteLogisticsTrack(id: number) {
  return request.delete<void>(`/order/admin/logistics/track/${id}`)
}
