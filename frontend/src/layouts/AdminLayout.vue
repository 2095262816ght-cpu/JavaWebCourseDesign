<template>
  <div class="admin-layout">
    <el-container class="admin-container">
      <!-- Sidebar -->
      <el-aside width="220px" class="admin-aside">
        <div class="admin-logo">
          <el-icon><Platform /></el-icon>
          <span>Shop 控制台</span>
        </div>

        <el-menu
          default-active="/admin"
          router
          class="admin-menu"
          background-color="#1f2937"
          text-color="#9ca3af"
          active-text-color="#ffffff"
        >
          <el-menu-item index="/admin">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘概览</span>
          </el-menu-item>
          <el-menu-item index="/admin/users" disabled>
            <el-icon><User /></el-icon>
            <span>用户管理 (T0002)</span>
          </el-menu-item>
          <el-menu-item index="/admin/brands" disabled>
            <el-icon><CollectionTag /></el-icon>
            <span>品牌管理 (T0003)</span>
          </el-menu-item>
          <el-menu-item index="/admin/categories" disabled>
            <el-icon><Menu /></el-icon>
            <span>分类管理 (T0003)</span>
          </el-menu-item>
          <el-menu-item index="/admin/products" disabled>
            <el-icon><Goods /></el-icon>
            <span>商品管理 (T0004)</span>
          </el-menu-item>
          <el-menu-item index="/admin/orders" disabled>
            <el-icon><List /></el-icon>
            <span>订单管理 (T0005)</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- Main Layout -->
      <el-container>
        <!-- Header -->
        <el-header height="60px" class="admin-header">
          <div class="header-left">
            <span class="system-tag">运营管理系统</span>
          </div>

          <div class="header-right">
            <el-button link @click="$router.push('/')">返回前台商城</el-button>
            <el-divider direction="vertical" />
            <el-avatar size="small" src="https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100" />
            <span class="admin-name">管理员 (admin)</span>
          </div>
        </el-header>

        <!-- Content Body -->
        <el-main class="admin-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import {
  Platform,
  Odometer,
  User,
  CollectionTag,
  Menu,
  Goods,
  List
} from '@element-plus/icons-vue'
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: flex;
}

.admin-container {
  height: 100vh;
  width: 100%;
}

.admin-aside {
  background-color: #1f2937;
  display: flex;
  flex-direction: column;
}

.admin-logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  background-color: #111827;
}

.admin-menu {
  border-right: none;
  flex: 1;
}

.admin-header {
  background-color: #ffffff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
}

.system-tag {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-name {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
}

.admin-main {
  background-color: #f3f4f6;
  padding: 24px;
  overflow-y: auto;
}
</style>
