<template>
  <div class="portal-layout">
    <!-- Top Bar -->
    <header class="portal-header">
      <div class="header-inner">
        <div class="brand" @click="$router.push('/')">
          <el-icon class="brand-icon"><Shop /></el-icon>
          <span class="brand-title">SHOP 商城</span>
        </div>

        <nav class="nav-links">
          <router-link to="/" class="nav-item">商城首页</router-link>
          <span class="nav-item disabled">商品中心</span>
          <span class="nav-item disabled">品牌分类</span>
          <router-link to="/admin" class="nav-item admin-link">管理后台</router-link>
        </nav>

        <div class="header-actions">
          <el-badge :value="0" class="cart-badge">
            <el-button circle :icon="ShoppingCart" />
          </el-badge>
          <el-button type="primary" size="default" round>登录 / 注册</el-button>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="portal-main">
      <router-view />
    </main>

    <!-- Footer -->
    <footer class="portal-footer">
      <div class="footer-inner">
        <p>Shop Platform © 2026 基于现代化 Java Web 架构（Spring Boot 3 + Sa-Token + MyBatis-Plus + Vue 3）构建</p>
        <p class="sub">课程大作业示范项目 · 高内聚深模块工程实践</p>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { Shop, ShoppingCart } from '@element-plus/icons-vue'
</script>

<style scoped>
.portal-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f9fafb;
}

.portal-header {
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.brand-icon {
  font-size: 26px;
  color: #409eff;
}

.brand-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.nav-links {
  display: flex;
  gap: 28px;
}

.nav-item {
  font-size: 15px;
  color: #4b5563;
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-item:hover {
  color: #409eff;
}

.nav-item.admin-link {
  color: #e6a23c;
}

.nav-item.disabled {
  color: #9ca3af;
  cursor: not-allowed;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.portal-main {
  flex: 1;
}

.portal-footer {
  background: #ffffff;
  border-top: 1px solid #e5e7eb;
  padding: 24px 0;
  margin-top: 48px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}

.footer-inner .sub {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 4px;
}
</style>
