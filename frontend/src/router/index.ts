import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import PortalLayout from '@/layouts/PortalLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import HomeView from '@/views/portal/HomeView.vue'
import DashboardView from '@/views/admin/DashboardView.vue'
import UserManagementView from '@/views/admin/UserManagementView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: PortalLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: HomeView
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAdmin: true },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: DashboardView
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: UserManagementView
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由导航守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore()

  // 1. 如果跳转到登录页且已登录，直接引导回对应主页
  if (to.path === '/login' && userStore.isLoggedIn) {
    if (userStore.isAdmin) {
      return next('/admin')
    }
    return next('/')
  }

  // 2. 管理后台权限防护 (/admin/**)
  if (to.matched.some((record) => record.meta.requiresAdmin)) {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录管理员账号后再进入控制台')
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }

    if (!userStore.isAdmin) {
      ElMessage.error('权限不足：当前账号不是超级管理员')
      return next('/')
    }
  }

  next()
})

export default router
