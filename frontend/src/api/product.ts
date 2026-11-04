import request from './request'
import type { CategoryNode } from './category'
import type { Brand } from './brand'

export interface CarouselSlide {
  id: number
  title: string
  subtitle: string
  image: string
  targetUrl: string
  productId?: number
}

export interface ProductCard {
  id: number
  name: string
  subtitle: string
  mainImage: string
  price: number
  originalPrice: number
  brandId: number
  brandName: string
  categoryId: number
  categoryName: string
  stock: number
  status: number
}

export interface ProductDetail {
  id: number
  brandId: number
  brandName: string
  brandLogo?: string
  categoryId: number
  categoryName: string
  name: string
  subtitle: string
  mainImage: string
  subImages: string[]
  price: number
  originalPrice: number
  stock: number
  status: number
  detailHtml?: string
  createdAt: string
}

export interface HomeShowcase {
  carousels: CarouselSlide[]
  featuredBrands: Brand[]
  categories: CategoryNode[]
  hotProducts: ProductCard[]
  newProducts: ProductCard[]
}

export interface ProductQuery {
  keyword?: string
  categoryId?: number
  brandId?: number
  minPrice?: number
  maxPrice?: number
  sortBy?: string
  pageNum?: number
  pageSize?: number
}

export interface ProductAdminVO {
  id: number
  brandId: number
  brandName: string
  categoryId: number
  categoryName: string
  name: string
  subtitle: string
  mainImage: string
  subImages: string[]
  price: number
  originalPrice: number
  stock: number
  status: number
  detailHtml?: string
  createdAt: string
  updatedAt: string
}

export interface AdminProductQuery {
  name?: string
  brandId?: number
  categoryId?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface ProductCommand {
  id?: number
  name: string
  subtitle?: string
  brandId: number
  categoryId: number
  mainImage: string
  subImages?: string[]
  price: number
  originalPrice?: number
  stock: number
  status?: number
  detailHtml?: string
}

export interface PageResult<T> {
  current: number
  size: number
  total: number
  pages: number
  records: T[]
}

// ----------------- Storefront Public APIs -----------------

/**
 * 首页聚合橱窗接口 (单次网络调用返回轮播、分类、品牌、新品及热卖)
 */
export function getHomeShowcaseApi() {
  return request.get<any, HomeShowcase>('/portal/home')
}

/**
 * 前台商品多维组合检索
 */
export function searchProductsApi(params: ProductQuery) {
  return request.get<any, PageResult<ProductCard>>('/portal/products', { params })
}

/**
 * 前台商品详情查询
 */
export function getProductDetailApi(id: number) {
  return request.get<any, ProductDetail>(`/portal/products/${id}`)
}

// ----------------- Backoffice Admin APIs -----------------

/**
 * 管理后台多条件分页查询商品
 */
export function listAdminProductsApi(params: AdminProductQuery) {
  return request.get<any, PageResult<ProductAdminVO>>('/admin/products', { params })
}

/**
 * 管理后台查询商品详情
 */
export function getAdminProductApi(id: number) {
  return request.get<any, ProductAdminVO>(`/admin/products/${id}`)
}

/**
 * 管理后台新建商品
 */
export function createProductApi(data: ProductCommand) {
  return request.post<any, number>('/admin/products', data)
}

/**
 * 管理后台修改商品
 */
export function updateProductApi(id: number, data: ProductCommand) {
  return request.put<any, void>(`/admin/products/${id}`, data)
}

/**
 * 管理后台快速修改上下架状态
 */
export function updateProductStatusApi(id: number, status: number) {
  return request.patch<any, void>(`/admin/products/${id}/status`, null, {
    params: { status }
  })
}

/**
 * 管理后台删除商品
 */
export function deleteProductApi(id: number) {
  return request.delete<any, void>(`/admin/products/${id}`)
}
