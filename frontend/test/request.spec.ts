import { describe, it, expect, beforeEach, vi } from 'vitest'
import service from '@/api/request'
import { AxiosHeaders } from 'axios'

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn(),
    info: vi.fn()
  }
}))

describe('Axios Request Interceptor & Token Injection', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('injects satoken header when token exists in localStorage', async () => {
    const fakeToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.sig'
    localStorage.setItem('token', fakeToken)

    const interceptor = (service.interceptors.request as any).handlers[0].fulfilled
    const config = {
      headers: new AxiosHeaders()
    }

    const modifiedConfig = await interceptor(config)
    expect(modifiedConfig.headers['satoken']).toBe(fakeToken)
  })

  it('does not inject satoken header when localStorage has no token', async () => {
    const interceptor = (service.interceptors.request as any).handlers[0].fulfilled
    const config = {
      headers: new AxiosHeaders()
    }

    const modifiedConfig = await interceptor(config)
    expect(modifiedConfig.headers['satoken']).toBeUndefined()
  })

  it('handles response 200 by extracting data', async () => {
    const responseInterceptor = (service.interceptors.response as any).handlers[0].fulfilled
    const mockResponse = {
      config: {},
      data: {
        code: 200,
        message: 'success',
        data: { id: 100, name: 'Sample' }
      }
    }

    const extracted = responseInterceptor(mockResponse)
    expect(extracted).toEqual({ id: 100, name: 'Sample' })
  })

  it('clears token from localStorage on 401 unauthenticated response', async () => {
    localStorage.setItem('token', 'expired-token')
    localStorage.setItem('user', JSON.stringify({ id: 1 }))

    const responseInterceptor = (service.interceptors.response as any).handlers[0].fulfilled
    const mockResponse = {
      config: {},
      data: {
        code: 401,
        message: '请先登录'
      }
    }

    await expect(responseInterceptor(mockResponse)).rejects.toThrow('请先登录')
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('user')).toBeNull()
  })
})
