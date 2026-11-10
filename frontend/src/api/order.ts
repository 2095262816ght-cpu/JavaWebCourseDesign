import { get, post, patch } from './request'
import type { PageResult } from './user'

export interface OrderItem {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage: string
  unitPrice: number
  quantity: number
  totalPrice: number
}

export interface DeliveryAddress {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
}

export interface OrderSummary {
  id: number
  orderNo: string
  customerId: number
  userId?: number
  totalAmount: number
  status: 'UNPAID' | 'PAID' | 'SHIPPED' | 'COMPLETED' | 'CANCELLED'
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  deliveryAddress?: DeliveryAddress
  paymentTime: string | null
  createdAt: string
  itemCount: number
  items: OrderItem[]
}

export interface OrderDetail extends OrderSummary {
  updatedAt: string
}

export interface CheckoutCommand {
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  deliveryAddress?: DeliveryAddress
}

export interface OrderCheckoutResult {
  orderId: number
  orderNo: string
  totalAmount: number
  status: string
}

export interface AdminOrderQuery {
  orderNo?: string
  status?: string
  receiverPhone?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 顾客提交购物车订单结算
 */
export function checkoutOrderApi(data: CheckoutCommand): Promise<OrderCheckoutResult> {
  return post<OrderCheckoutResult>('/api/orders/checkout', data)
}

/**
 * 模拟完成订单在线支付
 */
export function payOrderApi(orderNo: string): Promise<void> {
  return post<void>(`/api/orders/${orderNo}/pay`)
}

/**
 * 分页查询当前顾客订单历史
 */
export function getMyOrdersApi(params?: { pageNum?: number; pageSize?: number }): Promise<PageResult<OrderSummary>> {
  return get<PageResult<OrderSummary>>('/api/orders/my', params)
}

/**
 * 查询指定订单详情与快照行项
 */
export function getOrderDetailApi(id: number): Promise<OrderDetail> {
  return get<OrderDetail>(`/api/orders/${id}`)
}

/**
 * 管理后台多条件分页检索全量订单
 */
export function getAdminOrdersApi(params?: AdminOrderQuery): Promise<PageResult<OrderSummary>> {
  return get<PageResult<OrderSummary>>('/api/admin/orders', params)
}

/**
 * 管理员执行订单标记发货流转
 */
export function shipAdminOrderApi(id: number): Promise<void> {
  return patch<void>(`/api/admin/orders/${id}/ship`)
}
