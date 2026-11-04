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

function loadCachedCart(): CartSummary {
  const cached = localStorage.getItem(CART_CACHE_KEY)
  if (cached) {
    try {
      return JSON.parse(cached) as CartSummary
    } catch {
      // ignore
    }
  }
  return {
    items: [],
    totalItems: 0,
    checkedCount: 0,
    totalAmount: 0
  }
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
    return items.value.length > 0 && items.value.every((item) => item.checked)
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
      const empty: CartSummary = { items: [], totalItems: 0, checkedCount: 0, totalAmount: 0 }
      saveCache(empty)
      return empty
    }

    loading.value = true
    try {
      const res = await getCartApi()
      saveCache(res || { items: [], totalItems: 0, checkedCount: 0, totalAmount: 0 })
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
    const promises = items.value
      .filter((item) => item.checked !== checked)
      .map((item) => updateCartItemApi(item.id, { checked }))
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
    const empty: CartSummary = { items: [], totalItems: 0, checkedCount: 0, totalAmount: 0 }
    saveCache(empty)
  }

  /**
   * 提交已勾选条目结算下单并更新本地购物车状态
   */
  async function checkout(cmd: CheckoutCommand): Promise<OrderCheckoutResult> {
    const result = await checkoutOrderApi(cmd)
    await fetchCart()
    return result
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
    checkout
  }
})
