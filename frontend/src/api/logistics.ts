import request from './request'

export interface LogisticsTrack {
  id: number
  orderId: number
  status: 'ORDERED' | 'WAREHOUSE' | 'IN_TRANSIT' | 'PICKUP' | 'DELIVERED'
  message: string | null
  location: string | null
  trackTime: string
  createdTime: string
}

export function getLogisticsTracks(orderId: number) {
  return request.get<LogisticsTrack[]>('/order/logistics/tracks/' + orderId)
}
