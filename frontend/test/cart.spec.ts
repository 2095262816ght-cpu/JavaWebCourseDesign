import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useCartStore } from '@/stores/cart'
import { getCartApi, updateCartItemApi } from '@/api/cart'

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

  it('toggleCheckAll skips off-shelf or zero-stock items when selecting all, but allows unchecking them', async () => {
    localStorage.setItem('token', 'mock-jwt-token')
    vi.mocked(updateCartItemApi).mockResolvedValue(undefined)

    const initialItems = [
      {
        id: 11,
        productId: 101,
        productName: '正常在售商品',
        productImage: '',
        price: 100,
        quantity: 1,
        subtotal: 100,
        stock: 10,
        checked: false,
        productStatus: 1
      },
      {
        id: 12,
        productId: 102,
        productName: '已下架商品',
        productImage: '',
        price: 200,
        quantity: 1,
        subtotal: 200,
        stock: 5,
        checked: false,
        productStatus: 0
      },
      {
        id: 13,
        productId: 103,
        productName: '已售罄商品',
        productImage: '',
        price: 300,
        quantity: 1,
        subtotal: 300,
        stock: 0,
        checked: true,
        productStatus: 1
      }
    ]

    vi.mocked(getCartApi).mockResolvedValue({
      items: initialItems.map((it) => (it.id === 11 ? { ...it, checked: true } : { ...it })),
      totalItems: 3,
      checkedCount: 1,
      totalAmount: 100
    })

    const store = useCartStore()
    store.cartSummary = {
      items: initialItems,
      totalItems: 3,
      checkedCount: 0,
      totalAmount: 0
    }

    vi.mocked(updateCartItemApi).mockClear()
    await store.toggleCheckAll(true)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledTimes(1)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledWith(11, { checked: true })

    vi.mocked(updateCartItemApi).mockClear()
    await store.toggleCheckAll(false)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledTimes(2)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledWith(11, { checked: false })
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledWith(13, { checked: false })
  })

  it('toggleCheckAll excludes items whose quantity exceeds current stock when selecting all, while allowing unchecking them', async () => {
    localStorage.setItem('token', 'mock-jwt-token')
    vi.mocked(updateCartItemApi).mockResolvedValue(undefined)

    const items = [
      {
        id: 21,
        productId: 201,
        productName: '库存充足商品',
        productImage: '',
        price: 100,
        quantity: 2,
        subtotal: 200,
        stock: 5,
        checked: true,
        productStatus: 1
      },
      {
        id: 22,
        productId: 202,
        productName: '超库存商品',
        productImage: '',
        price: 300,
        quantity: 5,
        subtotal: 1500,
        stock: 2,
        checked: false,
        productStatus: 1
      }
    ]

    vi.mocked(getCartApi).mockResolvedValue({
      items: items.map((it) => ({ ...it, checked: true })),
      totalItems: 2,
      checkedCount: 2,
      totalAmount: 1700
    })

    const store = useCartStore()
    store.cartSummary = {
      items,
      totalItems: 2,
      checkedCount: 2,
      totalAmount: 200
    }

    // Only the valid item (id=21) is checkable and checked -> isAllChecked should be true
    expect(store.isAllChecked).toBe(true)

    store.cartSummary.items[0].checked = false
    store.cartSummary.items[1].checked = true
    expect(store.isAllChecked).toBe(false)

    vi.mocked(updateCartItemApi).mockClear()
    await store.toggleCheckAll(true)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledTimes(1)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledWith(21, { checked: true })

    vi.mocked(updateCartItemApi).mockClear()
    await store.toggleCheckAll(false)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledTimes(2)
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledWith(21, { checked: false })
    expect(vi.mocked(updateCartItemApi)).toHaveBeenCalledWith(22, { checked: false })
  })
})
