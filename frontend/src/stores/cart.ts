import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  getCartApi,
  addCartItemApi,
  updateCartItemApi,
  removeCartItemApi,
  clearCartApi,
  type CartSummary,
  type CartItem
} from '@/api/cart'
import {
  checkoutOrderApi,
  type CheckoutCommand,
  type OrderCheckoutResult
} from '@/api/order'

const CART_CACHE_KEY = 'shop_cart_cache'
const EMPTY_CART: CartSummary = {
  items: [],
  totalItems: 0,
  checkedCount: 0,
  totalAmount: 0
}

function loadCachedCart(): CartSummary {
  const cached = localStorage.getItem(CART_CACHE_KEY)
  if (cached) {
    try {
      return JSON.parse(cached) as CartSummary
    } catch {
      // ignore
    }
  }
  return { ...EMPTY_CART }
}

function isCheckableItem(item: CartItem): boolean {
  return item.productStatus !== 0 && (item.stock === undefined || (item.stock > 0 && item.quantity <= item.stock))
}

export const useCartStore = defineStore('cart', () => {
  const cartSummary = ref<CartSummary>(loadCachedCart())
  const loading = ref<boolean>(false)

  // 计算属性
  const items = computed<CartItem[]>(() => cartSummary.value.items || [])
  const cartCount = computed<number>(() => {
    return items.value.reduce((sum, item) => sum + (item.quantity || 0), 0)
  })
  const checkedCount = computed<number>(() => cartSummary.value.checkedCount || 0)
  const totalAmount = computed<number>(() => cartSummary.value.totalAmount || 0)
  const isAllChecked = computed<boolean>(() => {
    const checkable = items.value.filter(isCheckableItem)
    return checkable.length > 0 && checkable.every((item) => item.checked)
  })

  function saveCache(data: CartSummary) {
    cartSummary.value = data
    try {
      localStorage.setItem(CART_CACHE_KEY, JSON.stringify(data))
    } catch {
      // ignore
    }
  }

  /**
   * 同步拉取远端购物车数据
   */
  async function fetchCart(): Promise<CartSummary> {
    const token = localStorage.getItem('token')
    if (!token) {
      resetCart()
      return { ...EMPTY_CART }
    }

    loading.value = true
    try {
      const res = await getCartApi()
      saveCache(res || { ...EMPTY_CART })
      return cartSummary.value
    } finally {
      loading.value = false
    }
  }

  /**
   * 添加商品至购物车并同步
   */
  async function addToCart(productId: number, quantity: number = 1): Promise<void> {
    await addCartItemApi({ productId, quantity })
    await fetchCart()
  }

  /**
   * 修改购物车某项购买数量
   */
  async function updateQuantity(cartItemId: number, quantity: number): Promise<void> {
    await updateCartItemApi(cartItemId, { quantity })
    await fetchCart()
  }

  /**
   * 切换单个条目勾选状态
   */
  async function toggleCheck(cartItemId: number, checked: boolean): Promise<void> {
    await updateCartItemApi(cartItemId, { checked })
    await fetchCart()
  }

  /**
   * 全选或全不选
   */
  async function toggleCheckAll(checked: boolean): Promise<void> {
    const targetItems = items.value.filter((item) => {
      if (item.checked === checked) return false
      if (checked && !isCheckableItem(item)) return false
      return true
    })
    if (targetItems.length === 0) return
    const promises = targetItems.map((item) => updateCartItemApi(item.id, { checked }))
    await Promise.all(promises)
    await fetchCart()
  }

  /**
   * 移除指定购物车行项
   */
  async function removeItem(cartItemId: number): Promise<void> {
    await removeCartItemApi(cartItemId)
    await fetchCart()
  }

  /**
   * 清空购物车全部内容
   */
  async function clearCart(): Promise<void> {
    await clearCartApi()
    saveCache({ ...EMPTY_CART })
  }

  /**
   * 提交已勾选条目结算下单并更新本地购物车状态
   */
  async function checkout(cmd: CheckoutCommand): Promise<OrderCheckoutResult> {
    const result = await checkoutOrderApi(cmd)
    await fetchCart()
    return result
  }

  function resetCart() {
    cartSummary.value = { ...EMPTY_CART }
    try {
      localStorage.removeItem(CART_CACHE_KEY)
    } catch {
      // ignore
    }
  }

  // 与请求层和用户状态联动：登录态失效或退出登录时同步清空购物车缓存
  if (typeof window !== 'undefined') {
    window.addEventListener('auth:expired', resetCart)
    window.addEventListener('cart:reset', resetCart)
  }

  return {
    cartSummary,
    loading,
    items,
    cartCount,
    checkedCount,
    totalAmount,
    isAllChecked,
    fetchCart,
    addToCart,
    updateQuantity,
    toggleCheck,
    toggleCheckAll,
    removeItem,
    clearCart,
    checkout,
    resetCart
  }
})
