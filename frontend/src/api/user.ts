import request from './request'

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string
  phone: string | null
  avatar: string | null
  role: string
}

export interface Address {
  id: number
  userId: number
  receiverName: string
  receiverPhone: string
  province: string | null
  city: string | null
  district: string | null
  detailAddress: string
  isDefault: number
  createdTime: string
  updatedTime: string
}

export interface UserInfoRequest {
  nickname?: string
  email?: string
  phone?: string
}

export interface PasswordRequest {
  oldPassword: string
  newPassword: string
}

export function getUserInfo() {
  return request.get<UserInfo>('/user/info')
}

export function updateUserInfo(data: UserInfoRequest) {
  return request.put<void>('/user/info', data)
}

export function updatePassword(data: PasswordRequest) {
  return request.put<void>('/user/password', data)
}

export function getAddressList() {
  return request.get<Address[]>('/user/address/list')
}

export function addAddress(data: Partial<Address>) {
  return request.post<void>('/user/address/add', data)
}

export function updateAddress(data: Partial<Address>) {
  return request.put<void>('/user/address/update', data)
}

export function deleteAddress(id: number) {
  return request.delete<void>('/user/address/delete/' + id)
}

export function setDefaultAddress(id: number) {
  return request.put<void>('/user/address/default/' + id)
}
