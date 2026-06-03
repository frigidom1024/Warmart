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
  code: string
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
  return request.post<void>('/auth/forgot-password', { email })
}

export function refreshToken(refreshToken: string) {
  return request.post<AuthResponse>('/auth/refresh', null, { params: { refreshToken } })
}

export function sendRegisterCode(email: string) {
  return request.post<void>('/auth/code/send-register', { email })
}

export function sendResetCode(email: string) {
  return request.post<void>('/auth/code/send-reset', { email })
}

export function verifyCode(email: string, code: string, type: string) {
  return request.post<void>('/auth/code/verify', { email, code, type })
}

export function resetPassword(email: string, code: string, newPassword: string) {
  return request.post<void>('/auth/reset-password', { email, code, newPassword })
}

export function checkEmailRegistered(email: string) {
  return request.get<boolean>('/auth/check-email', { params: { email } })
}
