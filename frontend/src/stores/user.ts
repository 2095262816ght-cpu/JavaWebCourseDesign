import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  loginApi,
  registerApi,
  getProfileApi,
  updateProfileApi,
  logoutApi,
  type UserProfile,
  type LoginParams,
  type RegisterParams,
  type UpdateProfileParams
} from '@/api/auth'

function loadCachedUser(): UserProfile | null {
  const saved = localStorage.getItem('user')
  if (saved) {
    try {
      return JSON.parse(saved) as UserProfile
    } catch {
      return null
    }
  }
  return null
}

export const useUserStore = defineStore('user', () => {
  // Token持久化存储
  const token = ref<string>(localStorage.getItem('token') || '')

  // 用户资料响应式缓存
  const user = ref<UserProfile | null>(loadCachedUser())

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ROLE_ADMIN')
  const displayName = computed(() => user.value?.nickname || user.value?.username || '用户')
  const userAvatar = computed(
    () => user.value?.avatar || '/uploads/avatar-default.jpg'
  )

  /**
   * 登录
   */
  async function login(credentials: LoginParams) {
    const res = await loginApi(credentials)
    token.value = res.tokenValue
    user.value = res.user
    localStorage.setItem('token', res.tokenValue)
    localStorage.setItem('user', JSON.stringify(res.user))
    return res
  }

  /**
   * 注册
   */
  async function register(form: RegisterParams) {
    const profile = await registerApi(form)
    return profile
  }

  /**
   * 刷新拉取最新个人资料
   */
  async function fetchProfile() {
    if (!token.value) return null
    try {
      const profile = await getProfileApi()
      user.value = profile
      localStorage.setItem('user', JSON.stringify(profile))
      return profile
    } catch (err) {
      // 若 Token 已经失效，清理本地缓存
      clearAuth()
      throw err
    }
  }

  /**
   * 更新个人资料
   */
  async function updateProfile(data: UpdateProfileParams) {
    const profile = await updateProfileApi(data)
    user.value = profile
    localStorage.setItem('user', JSON.stringify(profile))
    return profile
  }

  /**
   * 注销登出
   */
  async function logout() {
    try {
      if (token.value) {
        await logoutApi()
      }
    } catch {
      // 忽略服务端注销可能的网络异常，确保本地状态清空
    } finally {
      clearAuth()
    }
  }

  /**
   * 内部清理鉴权状态
   */
  function clearAuth() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('shop_cart_cache')
    if (typeof window !== 'undefined') {
      window.dispatchEvent(new CustomEvent('cart:reset'))
    }
  }

  // 与请求层联动：401 时清空内存中的登录态，避免守卫继续放行失效会话
  if (typeof window !== 'undefined') {
    window.addEventListener('auth:expired', clearAuth)
  }

  return {
    token,
    user,
    isLoggedIn,
    isAdmin,
    displayName,
    userAvatar,
    login,
    register,
    fetchProfile,
    updateProfile,
    logout,
    clearAuth
  }
})
