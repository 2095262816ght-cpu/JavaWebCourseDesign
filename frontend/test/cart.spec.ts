import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCartStore } from '@/stores/cart'

vi.mock('@/api/cart', () => ({
  getCartApi: vi.fn(),
  addCartItemApi: vi.fn(),
  updateCartItemApi: vi.fn(),
  removeCartItemApi: vi.fn(),
  clearCartApi: vi.fn()
}))

vi.mock('@/api/order', () => ({
  checkoutOrderApi: vi.fn()
}))

describe('Pinia Cart Store & Reactive Computations', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('initializes with default empty cart state', () => {
    const store = useCartStore()
    expect(store.items).toEqual([])
    expect(store.cartCount).toBe(0)
    expect(store.checkedCount).toBe(0)
    expect(store.totalAmount).toBe(0)
    expect(store.isAllChecked).toBe(false)
  })

  it('reactively computes cartCount, checkedCount, totalAmount and isAllChecked', () => {
    const store = useCartStore()

    // Populate cart summary
    store.cartSummary = {
      items: [
        {
          id: 1,
          productId: 101,
          productName: 'iPhone 15 Pro',
          productImage: 'https://img.test/iphone.jpg',
          price: 7999,
          quantity: 2,
          checked: true,
          subtotal: 15998
        },
        {
          id: 2,
          productId: 102,
          productName: 'AirPods Pro 2',
          productImage: 'https://img.test/airpods.jpg',
          price: 1899,
          quantity: 1,
          checked: false,
          subtotal: 1899
        }
      ],
      totalItems: 2,
      checkedCount: 1,
      totalAmount: 15998
    }

    // cartCount is sum of quantities (2 + 1 = 3)
    expect(store.cartCount).toBe(3)
    expect(store.checkedCount).toBe(1)
    expect(store.totalAmount).toBe(15998)
    expect(store.isAllChecked).toBe(false)

    // Check second item -> isAllChecked should reactively become true
    store.cartSummary.items[1].checked = true
    expect(store.isAllChecked).toBe(true)

    // Updating item quantity should reactively update cartCount
    store.cartSummary.items[0].quantity = 5
    expect(store.cartCount).toBe(6)
  })

  it('returns isAllChecked false when cart is empty', () => {
    const store = useCartStore()
    store.cartSummary = {
      items: [],
      totalItems: 0,
      checkedCount: 0,
      totalAmount: 0
    }
    expect(store.isAllChecked).toBe(false)
  })

  it('persists and restores cartSummary from localStorage cache', () => {
    const mockSummary = {
      items: [
        {
          id: 3,
          productId: 200,
          productName: 'MacBook Air',
          productImage: 'https://img.test/macbook.jpg',
          price: 8999,
          quantity: 1,
          checked: true,
          subtotal: 8999
        }
      ],
      totalItems: 1,
      checkedCount: 1,
      totalAmount: 8999
    }
    localStorage.setItem('shop_cart_cache', JSON.stringify(mockSummary))

    const store = useCartStore()
    expect(store.items.length).toBe(1)
    expect(store.cartCount).toBe(1)
    expect(store.totalAmount).toBe(8999)
    expect(store.isAllChecked).toBe(true)
  })
})
