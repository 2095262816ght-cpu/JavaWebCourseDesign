/**
 * 订单状态在商城前台与管理后台的统一文案与标签配色
 */
export type OrderStatusTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

const ORDER_STATUS_TEXT: Record<string, string> = {
  UNPAID: '待付款',
  PAID: '已付款',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const ORDER_STATUS_TAG_TYPE: Record<string, OrderStatusTagType> = {
  UNPAID: 'warning',
  PAID: 'success',
  SHIPPED: 'primary',
  COMPLETED: 'info',
  CANCELLED: 'danger'
}

export function getOrderStatusText(status: string): string {
  return ORDER_STATUS_TEXT[status] ?? status
}

export function getOrderStatusTagType(status: string): OrderStatusTagType {
  return ORDER_STATUS_TAG_TYPE[status] ?? 'info'
}
