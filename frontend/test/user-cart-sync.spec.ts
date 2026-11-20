import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { getPortalBrandsApi } from '@/api/brand'
import service from '@/api/request'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn(),
    info: vi.fn()
  }
}))

describe('Auth-Cart Sync & Portal Brand API', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  it('clearAuth clears token, user, shop_cart_cache and resets active cartStore', () => {
    localStorage.setItem('token', 'mock-token')
    localStorage.setItem('user', JSON.stringify({ id: 2, username: 'customer', role: 'ROLE_CUSTOMER', nickname: '顾客', status: 1, createdAt: '2026-01-01' }))
    const cachedCart = {
      items: [
        {
          id: 9,
          productId: 1,
          productName: 'iPhone 15 Pro',
          productImage: '/uploads/p1.jpg',
          price: 7999,
          quantity: 2,
          subtotal: 15998,
          checked: true
        }
      ],
      totalItems: 2,
      checkedCount: 2,
      totalAmount: 15998
    }
    localStorage.setItem('shop_cart_cache', JSON.stringify(cachedCart))

    const cartStore = useCartStore()
    const userStore = useUserStore()
    expect(cartStore.cartCount).toBe(2)
    expect(userStore.isLoggedIn).toBe(true)

    userStore.clearAuth()

    expect(userStore.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
    expect(localStorage.getItem('shop_cart_cache')).toBeNull()
    expect(cartStore.items).toEqual([])
    expect(cartStore.cartCount).toBe(0)
    expect(cartStore.totalAmount).toBe(0)
  })

  it('getPortalBrandsApi requests uncapped /api/portal/brands endpoint', async () => {
    const getSpy = vi.spyOn(service, 'get').mockResolvedValue([{ id: 1, name: 'Apple' }] as any)
    const brands = await getPortalBrandsApi()
    expect(getSpy).toHaveBeenCalledWith('/api/portal/brands', { params: undefined })
    expect(brands).toEqual([{ id: 1, name: 'Apple' }])
  })
})
