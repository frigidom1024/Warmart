import request from '@/api/request'

export const getUsers = (params) => request.get('/auth/admin/users', { params })
export const toggleUserStatus = (id) => request.put(`/auth/admin/user/status/${id}`)
export const getDashboardStats = () => request.get('/auth/admin/dashboard')
