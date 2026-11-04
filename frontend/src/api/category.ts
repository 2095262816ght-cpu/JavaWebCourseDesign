import { get, post, put, del } from './request'

export interface CategoryNode {
  id: number
  parentId: number
  name: string
  icon: string | null
  sort: number
  status: number
  children: CategoryNode[]
}

export interface CategoryCommand {
  id?: number
  parentId: number
  name: string
  icon?: string | null
  sort?: number
  status: number
}

/**
 * 前台获取分类层级树 (带 Redis 缓存)
 */
export function getPortalCategoryTreeApi(): Promise<CategoryNode[]> {
  return get<CategoryNode[]>('/api/portal/categories/tree')
}

/**
 * 管理后台获取分类层级树
 */
export function getAdminCategoryTreeApi(): Promise<CategoryNode[]> {
  return get<CategoryNode[]>('/api/admin/categories/tree')
}

/**
 * 管理员创建新分类
 */
export function createCategoryApi(data: CategoryCommand): Promise<void> {
  return post<void>('/api/admin/categories', data)
}

/**
 * 管理员更新分类信息
 */
export function updateCategoryApi(id: number, data: CategoryCommand): Promise<void> {
  return put<void>(`/api/admin/categories/${id}`, data)
}

/**
 * 管理员删除分类 (若存在子分类将被后端安全拦截)
 */
export function deleteCategoryApi(id: number): Promise<void> {
  return del<void>(`/api/admin/categories/${id}`)
}
