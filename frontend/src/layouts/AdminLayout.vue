<template>
  <div class="admin-shell">
    <!-- 侧边栏 -->
    <aside class="side">
      <div class="side-brand">
        <span class="brand-mark" aria-hidden="true">
          <img src="/brand/logo-mark.png" alt="" />
        </span>
        <div class="brand-text">
          <span class="brand-main">NOIR</span>
          <span class="brand-sub mono">CONTROL PANEL</span>
        </div>
      </div>

      <nav class="side-nav" aria-label="后台导航">
        <p class="nav-group mono">OVERVIEW</p>
        <router-link to="/admin" class="nav-item" exact-active-class="is-active">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘概览</span>
        </router-link>

        <p class="nav-group mono">CATALOG</p>
        <router-link to="/admin/products" class="nav-item" active-class="is-active">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </router-link>
        <router-link to="/admin/brands" class="nav-item" active-class="is-active">
          <el-icon><CollectionTag /></el-icon>
          <span>品牌管理</span>
        </router-link>
        <router-link to="/admin/categories" class="nav-item" active-class="is-active">
          <el-icon><Menu /></el-icon>
          <span>分类管理</span>
        </router-link>

        <p class="nav-group mono">OPERATIONS</p>
        <router-link to="/admin/orders" class="nav-item" active-class="is-active">
          <el-icon><Tickets /></el-icon>
          <span>订单管理</span>
        </router-link>
        <router-link to="/admin/users" class="nav-item" active-class="is-active">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </router-link>
      </nav>

      <div class="side-foot">
        <router-link to="/" class="back-link">
          <el-icon><Shop /></el-icon>
          <span>返回前台商城</span>
          <el-icon class="back-arrow"><Right /></el-icon>
        </router-link>
      </div>
    </aside>

    <!-- 主区 -->
    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <button class="icon-btn mobile-menu" type="button" aria-label="菜单" @click="drawerOpen = true">
            <el-icon><Expand /></el-icon>
          </button>
          <div>
            <p class="topbar-kicker mono">ADMIN / {{ currentKey }}</p>
            <h1 class="topbar-title">{{ currentTitle }}</h1>
          </div>
        </div>

        <div class="topbar-right">
          <span class="status-dot"><i></i> 系统运行中</span>
          <el-dropdown trigger="click">
            <button class="admin-user" type="button">
              <el-avatar :size="30" :src="userStore.userAvatar" />
              <span class="admin-name">{{ userStore.displayName }}</span>
              <el-icon class="caret"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/')">
                  <el-icon><Shop /></el-icon>
                  <span>商城前台</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="passwordDialogOpen = true">
                  <el-icon><Lock /></el-icon>
                  <span>修改密码</span>
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  <span>退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>

    <!-- 移动端导航 -->
    <el-drawer v-model="drawerOpen" direction="ltr" size="76%" :with-header="false">
      <div class="drawer-body">
        <p class="side-brand" style="padding: 0 0 18px">
          <span class="brand-mark" aria-hidden="true">
            <img src="/brand/logo-mark.png" alt="" />
          </span>
          <span class="brand-text">
            <span class="brand-main">NOIR</span>
            <span class="brand-sub mono">CONTROL PANEL</span>
          </span>
        </p>

        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="drawer-link"
          @click="drawerOpen = false"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>

        <router-link to="/" class="drawer-link" @click="drawerOpen = false">
          <el-icon><Shop /></el-icon>
          <span>返回前台商城</span>
        </router-link>
        <button
          class="drawer-link drawer-action"
          type="button"
          @click="drawerOpen = false; passwordDialogOpen = true"
        >
          <el-icon><Lock /></el-icon>
          <span>修改密码</span>
        </button>
      </div>
    </el-drawer>

    <ChangePasswordDialog v-model="passwordDialogOpen" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Odometer,
  Goods,
  CollectionTag,
  Menu,
  Tickets,
  User,
  Shop,
  SwitchButton,
  ArrowDown,
  Right,
  Expand,
  Lock
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const drawerOpen = ref(false)
const passwordDialogOpen = ref(false)

const navItems = [
  { path: '/admin', label: '仪表盘概览', icon: Odometer },
  { path: '/admin/products', label: '商品管理', icon: Goods },
  { path: '/admin/brands', label: '品牌管理', icon: CollectionTag },
  { path: '/admin/categories', label: '分类管理', icon: Menu },
  { path: '/admin/orders', label: '订单管理', icon: Tickets },
  { path: '/admin/users', label: '用户管理', icon: User }
]

const pageMap: Record<string, { title: string; key: string }> = {
  '/admin': { title: '仪表盘概览', key: 'DASHBOARD' },
  '/admin/products': { title: '商品管理', key: 'PRODUCTS' },
  '/admin/brands': { title: '品牌管理', key: 'BRANDS' },
  '/admin/categories': { title: '分类管理', key: 'CATEGORIES' },
  '/admin/orders': { title: '订单管理', key: 'ORDERS' },
  '/admin/users': { title: '用户管理', key: 'USERS' }
}

const currentTitle = computed(() => pageMap[route.path]?.title ?? '运营控制台')
const currentKey = computed(() => pageMap[route.path]?.key ?? 'SYSTEM')

async function handleLogout() {
  await userStore.logout()
  ElMessage.success('已退出管理员控制台')
  router.push('/login')
}
</script>

<style scoped>
.admin-shell {
  display: flex;
  min-height: 100vh;
}

/* ---------- 侧栏 ---------- */
.side {
  width: 246px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 24px 16px;
  border-right: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(12, 15, 25, 0.92), rgba(6, 7, 12, 0.92));
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

.side-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0 8px 22px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 10px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  flex-shrink: 0;
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.05;
}

.brand-main {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.brand-sub {
  font-size: 8px;
  letter-spacing: 0.18em;
  color: var(--text-4);
}

.side-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.nav-group {
  margin: 18px 10px 6px;
  font-size: 8.5px;
  letter-spacing: 0.22em;
  color: var(--text-4);
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 10px 12px;
  border-radius: 11px;
  font-size: 13px;
  color: var(--text-2);
  transition: all 0.28s var(--ease);
}

.nav-item .el-icon {
  font-size: 16px;
  color: var(--text-3);
  transition: color 0.28s var(--ease);
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text);
}

.nav-item.is-active {
  background: linear-gradient(90deg, rgba(124, 92, 255, 0.22), rgba(124, 92, 255, 0.04));
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(124, 92, 255, 0.28);
}

.nav-item.is-active::before {
  content: '';
  position: absolute;
  left: -16px;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 18px;
  border-radius: 0 3px 3px 0;
  background: var(--grad);
  box-shadow: 0 0 14px rgba(124, 92, 255, 0.9);
}

.nav-item.is-active .el-icon {
  color: var(--cyan);
}

.side-foot {
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.back-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 11px;
  font-size: 12.5px;
  color: var(--text-3);
  transition: all 0.28s var(--ease);
}

.back-link:hover {
  background: rgba(255, 255, 255, 0.05);
  color: var(--text);
}

.back-arrow {
  margin-left: auto;
  font-size: 11px;
}

/* ---------- 主区 ---------- */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 32px;
  border-bottom: 1px solid var(--line);
  background: rgba(6, 7, 12, 0.72);
  backdrop-filter: blur(22px) saturate(180%);
  -webkit-backdrop-filter: blur(22px) saturate(180%);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.topbar-kicker {
  font-size: 9px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

.topbar-title {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.status-dot {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: var(--text-3);
}

.status-dot i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 10px var(--green);
  animation: pulse-glow 2.4s ease-in-out infinite;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 40px;
  padding: 0 12px 0 5px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.28s var(--ease);
}

.admin-user:hover {
  border-color: rgba(124, 92, 255, 0.45);
  color: var(--text);
}

.admin-name {
  font-size: 12.5px;
  font-weight: 500;
}

.caret {
  font-size: 11px;
  opacity: 0.6;
}

.content {
  flex: 1;
  padding: 28px 32px 48px;
}

.icon-btn {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-2);
  cursor: pointer;
}

.mobile-menu {
  display: none;
}

/* 移动抽屉 */
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px;
}

.drawer-link {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 12px;
  border-radius: 11px;
  font-size: 14px;
  color: var(--text-2);
}

.drawer-action {
  width: 100%;
  border: none;
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
}

.drawer-link.router-link-active {
  background: rgba(124, 92, 255, 0.16);
  color: #fff;
}

@media (max-width: 1024px) {
  .side {
    display: none;
  }

  .mobile-menu {
    display: grid;
  }

  .content {
    padding: 20px 18px 40px;
  }

  .topbar {
    padding: 14px 18px;
  }
}

@media (max-width: 600px) {
  .status-dot,
  .admin-name {
    display: none;
  }
}
</style>

<style>
.admin-shell + .el-drawer,
.el-drawer {
  background: #080a11;
}

.el-drawer__body {
  padding: 18px;
}
</style>
