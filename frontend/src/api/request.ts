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
  timeout: 10000
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
    } else {
      ElMessage.error(res.message || '请求处理失败')

      // Handle unauthenticated state
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('user')
      }

      return Promise.reject(new Error(res.message || 'Error'))
    }
  },
  (error) => {
    const msg = error.response?.data?.message || error.message || '网络请求超时或异常'
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
