<template>
  <div class="portal-layout">
    <header class="site-header" :class="{ 'is-scrolled': scrolled }">
      <div class="nav-bar">
        <button class="brand" type="button" @click="$router.push('/')">
          <span class="brand-mark" aria-hidden="true">
            <img src="/brand/logo-mark.png" alt="" />
          </span>
          <span class="brand-text">
            <span class="brand-main">SHOP NOIR</span>
            <span class="brand-sub mono">CURATED STORE</span>
          </span>
        </button>

        <nav class="nav-links" aria-label="主导航">
          <router-link to="/" class="nav-item" exact-active-class="is-active">首页</router-link>
          <router-link to="/products" class="nav-item" active-class="is-active">商品中心</router-link>
          <router-link v-if="userStore.isLoggedIn" to="/orders" class="nav-item" active-class="is-active">
            我的订单
          </router-link>
          <router-link v-if="userStore.isAdmin" to="/admin" class="nav-item nav-item--admin" active-class="is-active">
            管理后台
          </router-link>
        </nav>

        <div class="nav-actions">
          <!-- 搜索 -->
          <div class="search-box" :class="{ 'is-open': searchOpen }">
            <input
              ref="searchInputRef"
              v-model="keyword"
              class="search-input"
              type="search"
              placeholder="搜索商品 / 品牌"
              @keydown.enter="submitSearch"
              @blur="handleSearchBlur"
            />
            <button class="icon-btn" type="button" aria-label="搜索" @click="toggleSearch">
              <el-icon><Search /></el-icon>
            </button>
          </div>

          <!-- 购物车 -->
          <button class="icon-btn cart-btn" type="button" aria-label="购物车" @click="$router.push('/cart')">
            <el-icon><ShoppingCart /></el-icon>
            <span v-if="cartStore.cartCount > 0" class="cart-count mono">{{ cartStore.cartCount }}</span>
          </button>

          <!-- 已登录 -->
          <el-dropdown v-if="userStore.isLoggedIn" trigger="click" popper-class="user-dropdown">
            <button class="user-chip" type="button">
              <el-avatar :size="28" :src="userStore.userAvatar" class="user-avatar" />
              <span class="user-name">{{ userStore.displayName }}</span>
              <el-icon class="user-caret"><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/cart')">
                  <el-icon><ShoppingCart /></el-icon>
                  <span>购物车 · {{ cartStore.cartCount }}</span>
                </el-dropdown-item>
                <el-dropdown-item @click="$router.push('/orders')">
                  <el-icon><Tickets /></el-icon>
                  <span>我的订单</span>
                </el-dropdown-item>
                <el-dropdown-item v-if="userStore.isAdmin" divided @click="$router.push('/admin')">
                  <el-icon><Monitor /></el-icon>
                  <span>管理控制台</span>
                </el-dropdown-item>
                <el-dropdown-item :divided="!userStore.isAdmin" @click="passwordDialogOpen = true">
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

          <button v-else class="btn btn--primary btn--sm login-btn" type="button" @click="$router.push('/login')">
            登录 / 注册
          </button>

          <button class="icon-btn menu-btn" type="button" aria-label="菜单" @click="drawerOpen = true">
            <el-icon><Menu /></el-icon>
          </button>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view />
    </main>

    <footer class="site-footer">
      <div class="page-container footer-inner">
        <div class="footer-brand">
          <span class="brand-mark footer-mark" aria-hidden="true">
            <img src="/brand/logo-mark.png" alt="" />
          </span>
          <div>
            <p class="footer-logo">SHOP NOIR</p>
            <p class="footer-tagline">官方正品 · 顺丰包邮 · 七天无理由</p>
          </div>
        </div>

        <div class="footer-links">
          <div class="footer-col">
            <p class="footer-col-title mono">NAVIGATE</p>
            <router-link to="/" class="footer-link">商城首页</router-link>
            <router-link to="/products" class="footer-link">全部商品</router-link>
            <router-link to="/cart" class="footer-link">购物车</router-link>
            <router-link to="/orders" class="footer-link">我的订单</router-link>
          </div>
          <div class="footer-col">
            <p class="footer-col-title mono">CONTACT</p>
            <span class="footer-link is-static">客服热线 400-800-1234</span>
            <span class="footer-link is-static">服务时间 09:00 - 21:00</span>
            <span class="footer-link is-static">邮箱 care@shopnoir.com</span>
            <span class="footer-link is-static">采购专线 400-800-5678</span>
          </div>
          <div class="footer-col">
            <p class="footer-col-title mono">SERVICE</p>
            <span class="footer-link is-static">正品保障</span>
            <span class="footer-link is-static">七天无理由</span>
            <span class="footer-link is-static">顺丰包邮</span>
            <span class="footer-link is-static">48 小时发货</span>
          </div>
        </div>
      </div>

      <div class="page-container footer-bottom">
        <span>© 2026 SHOP NOIR 保留所有权利</span>
        <span class="mono">隐私政策 · 服务条款</span>
      </div>
    </footer>

    <el-drawer v-model="drawerOpen" direction="rtl" size="78%" :with-header="false" class="mobile-drawer">
      <div class="drawer-body">
        <div class="drawer-head">
          <span class="footer-logo">SHOP NOIR</span>
          <button class="icon-btn" type="button" aria-label="关闭" @click="drawerOpen = false">
            <el-icon><Close /></el-icon>
          </button>
        </div>
        <form class="drawer-search" role="search" @submit.prevent="submitSearch">
          <input
            v-model="keyword"
            class="drawer-search-input"
            type="search"
            placeholder="搜索商品 / 品牌"
            aria-label="搜索商品或品牌"
          />
          <button class="btn btn--primary btn--sm" type="submit">搜索</button>
        </form>
        <router-link to="/" class="drawer-link" @click="drawerOpen = false">商城首页</router-link>
        <router-link to="/products" class="drawer-link" @click="drawerOpen = false">商品中心</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/orders" class="drawer-link" @click="drawerOpen = false">我的订单</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/cart" class="drawer-link" @click="drawerOpen = false">购物车</router-link>
        <router-link v-if="userStore.isAdmin" to="/admin" class="drawer-link" @click="drawerOpen = false">管理后台</router-link>
        <button
          v-if="userStore.isLoggedIn"
          class="drawer-link drawer-action"
          type="button"
          @click="drawerOpen = false; passwordDialogOpen = true"
        >
          修改密码
        </button>
        <button v-if="!userStore.isLoggedIn" class="btn btn--primary drawer-cta" type="button" @click="$router.push('/login'); drawerOpen = false">
          登录 / 注册
        </button>
      </div>
    </el-drawer>

    <ChangePasswordDialog v-model="passwordDialogOpen" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Search,
  ShoppingCart,
  ArrowDown,
  SwitchButton,
  Tickets,
  Monitor,
  Menu,
  Close,
  Lock
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'

const router = useRouter()
const userStore = useUserStore()
const cartStore = useCartStore()

const scrolled = ref(false)
const searchOpen = ref(false)
const keyword = ref('')
const searchInputRef = ref<HTMLInputElement>()
const drawerOpen = ref(false)
const passwordDialogOpen = ref(false)

function handleScroll() {
  scrolled.value = window.scrollY > 12
}

function toggleSearch() {
  if (!searchOpen.value) {
    searchOpen.value = true
    setTimeout(() => searchInputRef.value?.focus(), 60)
    return
  }
  submitSearch()
}

function handleSearchBlur() {
  if (!keyword.value) {
    searchOpen.value = false
  }
}

function submitSearch() {
  const value = keyword.value.trim()
  if (!value) return
  router.push({ path: '/products', query: { keyword: value } })
  searchOpen.value = false
  drawerOpen.value = false
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
  if (userStore.isLoggedIn) {
    cartStore.fetchCart().catch(() => {})
  }
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

async function handleLogout() {
  await userStore.logout()
  cartStore.resetCart()
  ElMessage.success('已安全退出登录')
  router.push('/')
}
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ---------- 顶部导航 ---------- */
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 18px 24px 0;
  transition: padding 0.4s var(--ease);
}

.site-header.is-scrolled {
  padding-top: 10px;
}

.nav-bar {
  max-width: 1280px;
  margin: 0 auto;
  height: 62px;
  padding: 0 10px 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(9, 11, 18, 0.6);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  box-shadow: 0 20px 50px -32px rgba(0, 0, 0, 0.95);
  transition: background 0.4s var(--ease), border-color 0.4s var(--ease);
}

.is-scrolled .nav-bar {
  background: rgba(7, 9, 15, 0.86);
  border-color: rgba(255, 255, 255, 0.12);
}

/* 品牌 */
.brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 0;
  border: none;
  background: none;
  color: var(--text);
  cursor: pointer;
  flex-shrink: 0;
}

.brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: var(--cyan);
  border-radius: 12px;
  background: linear-gradient(150deg, rgba(124, 92, 255, 0.24), rgba(34, 211, 238, 0.1));
  border: 1px solid rgba(124, 92, 255, 0.35);
  box-shadow: 0 0 24px -6px rgba(124, 92, 255, 0.7);
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  line-height: 1;
  text-align: left;
}

.brand-main {
  font-family: var(--font-display);
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text);
  white-space: nowrap;
}

.brand-sub {
  font-size: 8px;
  letter-spacing: 0.24em;
  color: var(--text-4);
  white-space: nowrap;
}

/* 导航 */
.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  position: relative;
  padding: 8px 16px;
  border-radius: 999px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-2);
  transition: color 0.25s var(--ease), background 0.25s var(--ease);
}

.nav-item::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 4px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--cyan);
  transform: translateX(-50%) scale(0);
  transition: transform 0.3s var(--ease);
}

.nav-item:hover {
  color: var(--text);
  background: rgba(255, 255, 255, 0.05);
}

.nav-item.is-active {
  color: var(--text);
}

.nav-item.is-active::after {
  transform: translateX(-50%) scale(1);
}

.nav-item--admin {
  color: #d9c8ff;
}

/* 右侧操作 */
.nav-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.icon-btn {
  position: relative;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 1px solid transparent;
  border-radius: 50%;
  background: transparent;
  color: var(--text-2);
  font-size: 17px;
  cursor: pointer;
  transition: all 0.28s var(--ease);
}

.icon-btn:hover {
  color: var(--text);
  background: rgba(255, 255, 255, 0.07);
  border-color: var(--line);
}

.cart-count {
  position: absolute;
  top: 1px;
  right: 0;
  min-width: 17px;
  height: 17px;
  padding: 0 4px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: var(--grad-hot);
  color: #fff;
  font-size: 9px;
  font-weight: 700;
  box-shadow: 0 0 12px rgba(255, 77, 141, 0.8);
}

/* 搜索展开 */
.search-box {
  display: flex;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 999px;
  transition: all 0.4s var(--ease);
  overflow: hidden;
}

.search-box.is-open {
  width: 232px;
  padding-left: 16px;
  border-color: var(--line);
  background: rgba(255, 255, 255, 0.05);
}

.search-input {
  width: 0;
  opacity: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text);
  font-family: var(--font-body);
  font-size: 13px;
  transition: opacity 0.25s var(--ease);
}

.search-box.is-open .search-input {
  width: 168px;
  opacity: 1;
}

.search-input::placeholder {
  color: var(--text-4);
}

/* 用户 */
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 10px 0 4px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  color: var(--text-2);
  cursor: pointer;
  transition: all 0.28s var(--ease);
}

.user-chip:hover {
  border-color: rgba(124, 92, 255, 0.45);
  background: rgba(124, 92, 255, 0.1);
  color: var(--text);
}

.user-avatar {
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-caret {
  font-size: 11px;
  opacity: 0.6;
}

.login-btn {
  margin-left: 4px;
}

.menu-btn {
  display: none;
}

/* 主内容 */
.portal-main {
  flex: 1;
}

/* ---------- 页脚 ---------- */
.site-footer {
  margin-top: 96px;
  border-top: 1px solid var(--line);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent);
}

.footer-inner {
  display: flex;
  justify-content: space-between;
  gap: 48px;
  padding-top: 56px;
  padding-bottom: 40px;
}

.footer-brand {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  max-width: 300px;
}

.footer-mark {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.footer-logo {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text);
}

.footer-tagline {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-3);
  line-height: 1.6;
}

.footer-links {
  display: flex;
  gap: 72px;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.footer-col-title {
  font-size: 10px;
  letter-spacing: 0.24em;
  color: var(--text-4);
  margin-bottom: 2px;
}

.footer-link {
  font-size: 13px;
  color: var(--text-2);
  transition: color 0.25s var(--ease);
  width: fit-content;
}

.footer-link:hover {
  color: var(--cyan);
}

.footer-link.is-static:hover {
  color: var(--text-2);
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 22px;
  padding-bottom: 32px;
  border-top: 1px solid var(--line);
  font-size: 12px;
  color: var(--text-4);
}

/* 抽屉 */
.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 4px;
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.drawer-link {
  padding: 15px 12px;
  border-radius: 12px;
  font-family: var(--font-display);
  font-size: 19px;
  font-weight: 600;
  color: var(--text-2);
  border-bottom: 1px solid var(--line);
}

.drawer-action {
  width: 100%;
  border-top: none;
  border-left: none;
  border-right: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

/* 移动端抽屉搜索：窄屏隐藏顶栏搜索框时的替代入口 */
.drawer-search {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.drawer-search-input {
  flex: 1;
  min-width: 0;
  height: 42px;
  padding: 0 14px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.05);
  color: var(--text);
  font-family: var(--font-body);
  font-size: 14px;
  outline: none;
}

.drawer-search-input::placeholder {
  color: var(--text-4);
}

.drawer-search-input:focus {
  border-color: var(--line-strong);
}

.drawer-link.router-link-exact-active,
.drawer-link.router-link-active {
  color: var(--cyan);
}

.drawer-cta {
  margin-top: 22px;
  width: 100%;
}

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .nav-links {
    display: none;
  }

  .menu-btn {
    display: grid;
  }

  .search-box {
    display: none;
  }

  .site-header {
    padding: 10px 14px 0;
  }

  .footer-inner {
    flex-direction: column;
    gap: 36px;
  }

  .footer-links {
    gap: 40px;
    flex-wrap: wrap;
  }
}

@media (max-width: 600px) {
  .user-name,
  .user-caret {
    display: none;
  }

  .login-btn {
    display: none;
  }

  .brand-main {
    font-size: 15px;
  }

  .nav-bar {
    padding-left: 16px;
  }

  .footer-bottom {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
  }
}
</style>

<style>
.mobile-drawer .el-drawer__body {
  background: #080a11;
  padding: 20px;
}
</style>
