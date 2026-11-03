<template>
  <div class="home-container">
    <!-- Hero Banner Skeleton -->
    <div class="hero-section">
      <el-carousel height="380px" indicator-position="outside">
        <el-carousel-item v-for="(item, index) in banners" :key="index">
          <div class="banner-card" :style="{ backgroundImage: `url(${item.image})` }">
            <div class="banner-overlay">
              <h2>{{ item.title }}</h2>
              <p>{{ item.subtitle }}</p>
              <el-button type="primary" size="large" round>立即选购</el-button>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- Quick Features -->
    <div class="features-bar">
      <div class="feature-item">
        <el-icon><Van /></el-icon>
        <div>
          <h4>官方正品保证</h4>
          <p>全场正品 假一赔十</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon><Lightning /></el-icon>
        <div>
          <h4>极速物流配送</h4>
          <p>顺丰速运 次日必达</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon><Service /></el-icon>
        <div>
          <h4>七天无理由退换</h4>
          <p>贴心售后 无忧购物</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon><Medal /></el-icon>
        <div>
          <h4>专属会员特权</h4>
          <p>积分抵现 专享优惠</p>
        </div>
      </div>
    </div>

    <!-- Welcome Content Box -->
    <el-card shadow="never" class="welcome-box">
      <template #header>
        <div class="card-header">
          <span class="title">商城平台已就绪</span>
          <el-tag type="success">Ticket 0001 已交付</el-tag>
        </div>
      </template>
      <div class="status-content">
        <p>前后端统一架构已就绪：Spring Boot 3.3 + Sa-Token + MyBatis-Plus + Vue 3 + Pinia + Element Plus。</p>
        <p>本地 MySQL 8.4 数据库及 Docker Redis 缓存已连接成功，数据表结构已完成初始化。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Van, Lightning, Service, Medal } from '@element-plus/icons-vue'

const banners = ref([
  {
    title: 'iPhone 16 Pro 钛金属系列',
    subtitle: '强悍 A18 Pro 芯片，超长续航与卓越影像',
    image: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=1600&h=600&fit=crop'
  },
  {
    title: '华为 Mate 60 Pro 旗舰归来',
    subtitle: '遥遥领先玄武架构，双向卫星通信',
    image: 'https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=1600&h=600&fit=crop'
  },
  {
    title: 'MacBook Air 15 英寸 M3 芯片',
    subtitle: '轻薄出众，动力澎湃，18小时强悍续航',
    image: 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=1600&h=600&fit=crop'
  }
])
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.hero-section {
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 24px;
}

.banner-card {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
  display: flex;
  align-items: center;
}

.banner-overlay {
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.75) 0%, rgba(0, 0, 0, 0.2) 70%, transparent 100%);
  width: 100%;
  height: 100%;
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  color: #fff;
}

.banner-overlay h2 {
  font-size: 32px;
  margin: 0 0 12px 0;
  font-weight: 700;
}

.banner-overlay p {
  font-size: 16px;
  margin: 0 0 24px 0;
  color: #e5e7eb;
}

.features-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  margin-bottom: 32px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.feature-item .el-icon {
  font-size: 32px;
  color: #409eff;
}

.feature-item h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  color: #1f2937;
}

.feature-item p {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.welcome-box {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 18px;
  font-weight: 600;
}

.status-content p {
  margin: 8px 0;
  color: #4b5563;
  line-height: 1.6;
}
</style>
