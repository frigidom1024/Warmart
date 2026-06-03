import request from './request'
import type { PageResult } from './product'

export interface User {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  role: string
  status: number
  createdTime: string
  updatedTime: string
}

export interface UserQuery {
  username?: string
  phone?: string
  nickname?: string
  status?: number
  page?: number
  size?: number
}

export function getAdminUserList(params: UserQuery) {
  return request.get<any, PageResult<User>>('/auth/admin/users/list', { params })
}

export function getUserDetail(id: number) {
  return request.get<any, User>('/auth/admin/users/' + id)
}

export function updateUserStatus(id: number, status: number) {
  return request.put<void>('/auth/admin/users/' + id + '/status', null, { params: { status } })
}

export function updateUserRole(id: number, role: string) {
  return request.put<void>('/auth/admin/users/' + id + '/role', { role })
}
