import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email: string
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
}

export function login(data: LoginRequest) {
  return request.post<AuthResponse>('/auth/login', data)
}

export function register(data: RegisterRequest) {
  return request.post<void>('/auth/register', data)
}

export function forgotPassword(email: string) {
  return request.post<void>('/auth/forgot-password', null, { params: { email } })
}

export function refreshToken(refreshToken: string) {
  return request.post<AuthResponse>('/auth/refresh', null, { params: { refreshToken } })
}
