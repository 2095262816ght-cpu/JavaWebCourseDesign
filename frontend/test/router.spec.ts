import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import router from '@/router'
import { useUserStore } from '@/stores/user'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn(),
    info: vi.fn()
  }
}))

describe('Router Guards Smoke Tests', () => {
  beforeEach(async () => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.clearAllMocks()
    await router.push('/')
  })

  it('allows access to public pages without login', async () => {
    await router.push('/')
    expect(router.currentRoute.value.path).toBe('/')

    await router.push('/products')
    expect(router.currentRoute.value.path).toBe('/products')
  })

  it('redirects unauthenticated user from protected page (/cart) to /login with redirect query', async () => {
    const userStore = useUserStore()
    userStore.clearAuth()

    await router.push('/cart')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/cart')
  })

  it('redirects unauthenticated user from /admin to /login', async () => {
    const userStore = useUserStore()
    userStore.clearAuth()

    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/login')
    expect(router.currentRoute.value.query.redirect).toBe('/admin')
  })

  it('blocks customer (ROLE_CUSTOMER) from accessing /admin and redirects to home /', async () => {
    const userStore = useUserStore()
    userStore.token = 'customer-jwt-token'
    userStore.user = {
      id: 2,
      username: 'customer',
      nickname: '普通顾客',
      role: 'ROLE_CUSTOMER',
      email: 'customer@test.com',
      phone: '13800000002',
      avatar: null,
      status: 1,
      createdAt: '2026-01-01'
    }

    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('allows admin (ROLE_ADMIN) to access /admin', async () => {
    const userStore = useUserStore()
    userStore.token = 'admin-jwt-token'
    userStore.user = {
      id: 1,
      username: 'admin',
      nickname: '系统管理员',
      role: 'ROLE_ADMIN',
      email: 'admin@test.com',
      phone: '13800000001',
      avatar: null,
      status: 1,
      createdAt: '2026-01-01'
    }

    await router.push('/admin')
    expect(router.currentRoute.value.path).toBe('/admin')
  })

  it('redirects logged-in customer away from /login to home /', async () => {
    const userStore = useUserStore()
    userStore.token = 'customer-jwt-token'
    userStore.user = {
      id: 2,
      username: 'customer',
      nickname: '普通顾客',
      role: 'ROLE_CUSTOMER',
      email: null,
      phone: null,
      avatar: null,
      status: 1,
      createdAt: '2026-01-01'
    }

    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('redirects logged-in admin away from /login to /admin', async () => {
    const userStore = useUserStore()
    userStore.token = 'admin-jwt-token'
    userStore.user = {
      id: 1,
      username: 'admin',
      nickname: '系统管理员',
      role: 'ROLE_ADMIN',
      email: null,
      phone: null,
      avatar: null,
      status: 1,
      createdAt: '2026-01-01'
    }

    await router.push('/login')
    expect(router.currentRoute.value.path).toBe('/admin')
  })
})
