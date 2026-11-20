import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  // 首次请求可能撞上后端/数据库冷启动，预留充足等待时间
  timeout: 30000
})

// Request interceptor: attach Sa-Token auth header
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['satoken'] = token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor: handle unified Result<T> structure
service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse

    // If binary or direct download response
    if (response.config.responseType === 'blob') {
      return response.data
    }

    if (res.code === 200) {
      return res.data
    }

    // 登录接口的 401 表示账号或密码错误，直接透传后端提示，不触发全局登录失效流程
    if (res.code === 401 && (response.config.url || '').includes('/api/auth/login')) {
      ElMessage.error(res.message || '用户名或密码错误')
      return Promise.reject(new Error(res.message || 'Unauthorized'))
    }

    // 登录态失效：清理本地凭证并通知 Pinia 同步退出
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('shop_cart_cache')
      window.dispatchEvent(new CustomEvent('auth:expired'))
      ElMessage.error('登录状态已失效，请重新登录')
      return Promise.reject(new Error(res.message || 'Unauthorized'))
    }

    ElMessage.error(res.message || '请求处理失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    let msg = '网络请求异常，请稍后重试'
    if (error.code === 'ECONNABORTED') {
      msg = '请求超时，请确认后端服务已启动后重试'
    } else if (!error.response) {
      msg = '网络连接异常，请确认后端服务已启动'
    } else if (error.response.status >= 500) {
      msg = '服务暂时不可用，请确认后端服务已启动后重试'
    } else {
      msg = error.response?.data?.message || error.message || msg
    }
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service

export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, { params, ...config }) as unknown as Promise<T>
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as unknown as Promise<T>
}

export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as unknown as Promise<T>
}

export function patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
  return service.patch(url, data, config) as unknown as Promise<T>
}

export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config) as unknown as Promise<T>
}
