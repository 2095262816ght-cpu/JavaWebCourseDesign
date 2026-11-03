import { createRouter, createWebHistory } from 'vue-router'
import PortalLayout from '@/layouts/PortalLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import HomeView from '@/views/portal/HomeView.vue'
import DashboardView from '@/views/admin/DashboardView.vue'

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
    path: '/admin',
    component: AdminLayout,
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: DashboardView
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
