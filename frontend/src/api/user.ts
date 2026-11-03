import { get, put } from './request'
import type { UserProfile } from './auth'

export interface PageResult<T> {
  current: number
  size: number
  total: number
  pages: number
  records: T[]
}

export interface UserQueryParams {
  page?: number
  size?: number
  keyword?: string
  role?: string
  status?: number
}

/**
 * 管理员分页查询用户列表
 */
export function getAdminUsersApi(params?: UserQueryParams): Promise<PageResult<UserProfile>> {
  return get<PageResult<UserProfile>>('/api/admin/users', params)
}

/**
 * 管理员更新用户启用/禁用状态
 */
export function updateUserStatusApi(id: number, status: number): Promise<void> {
  return put<void>(`/api/admin/users/${id}/status`, { status })
}

/**
 * 管理员查询用户详情
 */
export function getUserDetailApi(id: number): Promise<UserProfile> {
  return get<UserProfile>(`/api/admin/users/${id}`)
}
