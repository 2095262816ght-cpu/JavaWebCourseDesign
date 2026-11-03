import { get, post, put } from './request'

export interface UserProfile {
  id: number
  username: string
  nickname: string
  avatar: string
  role: 'ROLE_ADMIN' | 'ROLE_CUSTOMER'
  status: number
  createdAt: string
}

export interface AuthTokenVO {
  tokenName: string
  tokenValue: string
  user: UserProfile
}

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  nickname?: string
  avatar?: string
}

export interface UpdateProfileParams {
  nickname?: string
  avatar?: string
  oldPassword?: string
  newPassword?: string
}

/**
 * 账号登录
 */
export function loginApi(data: LoginParams): Promise<AuthTokenVO> {
  return post<AuthTokenVO>('/api/auth/login', data)
}

/**
 * 顾客新账号注册
 */
export function registerApi(data: RegisterParams): Promise<UserProfile> {
  return post<UserProfile>('/api/auth/register', data)
}

/**
 * 获取当前登录用户资料
 */
export function getProfileApi(): Promise<UserProfile> {
  return get<UserProfile>('/api/auth/profile')
}

/**
 * 更新用户个人资料
 */
export function updateProfileApi(data: UpdateProfileParams): Promise<UserProfile> {
  return put<UserProfile>('/api/auth/profile', data)
}

/**
 * 注销当前登录会话
 */
export function logoutApi(): Promise<void> {
  return post<void>('/api/auth/logout')
}
