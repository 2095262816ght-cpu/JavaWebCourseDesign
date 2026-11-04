import { get, post, put, del } from './request'

export interface CartItem {
  id: number
  productId: number
  productName: string
  productImage: string
  price: number
  quantity: number
  subtotal: number
  stock: number
  checked: boolean
  productStatus: number
}

export interface CartSummary {
  items: CartItem[]
  totalItems: number
  checkedCount: number
  totalAmount: number
}

export interface AddCartCommand {
  productId: number
  quantity: number
}

export interface UpdateCartItemCommand {
  quantity?: number
  checked?: boolean
}

/**
 * 获取当前登录用户的购物车明细与小计
 */
export function getCartApi(): Promise<CartSummary> {
  return get<CartSummary>('/api/cart')
}

/**
 * 添加商品到购物车
 */
export function addCartItemApi(data: AddCartCommand): Promise<void> {
  return post<void>('/api/cart/items', data)
}

/**
 * 修改购物车项数量或勾选状态
 */
export function updateCartItemApi(id: number, data: UpdateCartItemCommand): Promise<void> {
  return put<void>(`/api/cart/items/${id}`, data)
}

/**
 * 移除指定购物车行项
 */
export function removeCartItemApi(id: number): Promise<void> {
  return del<void>(`/api/cart/items/${id}`)
}

/**
 * 清空当前用户购物车
 */
export function clearCartApi(): Promise<void> {
  return del<void>('/api/cart')
}
