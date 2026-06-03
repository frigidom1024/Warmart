import request from './request'

export interface DashboardStats {
  totalOrders: number
  totalSales: number
  todayOrders: number
  todaySales: number
  totalUsers: number
  orderStatusStats: { status: number; count: number }[]
  salesTrend: { date: string; order_count: number; sales_amount: number }[]
  hotProducts: { productId: number; productName: string; productImage: string; sales: number; amount: number }[]
}

export function getAdminDashboardStats() {
  return request.get<any, DashboardStats>('/order/admin/stats')
}

export function getAdminUserCount() {
  return request.get<any, number>('/auth/admin/users/count')
}
