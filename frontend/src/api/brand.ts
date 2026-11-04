import { get, post, put, del } from './request'
import type { PageResult } from './user'

export interface Brand {
  id: number
  name: string
  logo: string | null
  description: string | null
  sort: number
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface BrandCommand {
  id?: number
  name: string
  logo?: string | null
  description?: string | null
  sort?: number
  status: number
}

export interface BrandQueryParams {
  page?: number
  size?: number
  keyword?: string
  status?: number
}

/**
 * 前台获取推荐品牌 (带 Redis 缓存)
 */
export function getFeaturedBrandsApi(limit: number = 6): Promise<Brand[]> {
  return get<Brand[]>('/api/portal/brands/featured', { limit })
}

/**
 * 管理后台分页查询品牌列表
 */
export function getAdminBrandsApi(params?: BrandQueryParams): Promise<PageResult<Brand>> {
  return get<PageResult<Brand>>('/api/admin/brands', params)
}

/**
 * 管理员获取品牌详情
 */
export function getBrandDetailApi(id: number): Promise<Brand> {
  return get<Brand>(`/api/admin/brands/${id}`)
}

/**
 * 管理员新建品牌
 */
export function createBrandApi(data: BrandCommand): Promise<void> {
  return post<void>('/api/admin/brands', data)
}

/**
 * 管理员修改品牌
 */
export function updateBrandApi(id: number, data: BrandCommand): Promise<void> {
  return put<void>(`/api/admin/brands/${id}`, data)
}

/**
 * 管理员删除品牌
 */
export function deleteBrandApi(id: number): Promise<void> {
  return del<void>(`/api/admin/brands/${id}`)
}
