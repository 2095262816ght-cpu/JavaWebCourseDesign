<template>
  <div class="home-container">
    <!-- Hero Banner -->
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

    <!-- Category Taxonomy Section (T0003) -->
    <div class="section-container" v-if="categories.length > 0">
      <div class="section-header">
        <div class="header-left">
          <el-icon class="section-icon"><Menu /></el-icon>
          <h3 class="section-title">全部分类导航</h3>
          <span class="section-badge">多级层级树 · Redis 缓存加速</span>
        </div>
      </div>

      <div class="category-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="category-card"
        >
          <div class="category-card-header">
            <span class="cat-name">{{ cat.name }}</span>
            <el-tag size="small" type="primary" round>{{ cat.children?.length || 0 }} 个子分类</el-tag>
          </div>
          <div class="sub-category-list" v-if="cat.children && cat.children.length > 0">
            <span
              v-for="sub in cat.children"
              :key="sub.id"
              class="sub-cat-pill"
            >
              {{ sub.name }}
            </span>
          </div>
          <div v-else class="empty-sub">暂无子分类</div>
        </div>
      </div>
    </div>

    <!-- Featured Brands Section (T0003) -->
    <div class="section-container" v-if="featuredBrands.length > 0">
      <div class="section-header">
        <div class="header-left">
          <el-icon class="section-icon"><CollectionTag /></el-icon>
          <h3 class="section-title">热门甄选品牌</h3>
          <span class="section-badge">官方旗舰店入驻 · Cache-Aside 缓存</span>
        </div>
      </div>

      <div class="brand-grid">
        <div
          v-for="brand in featuredBrands"
          :key="brand.id"
          class="brand-card"
        >
          <div class="brand-logo-wrap">
            <el-image
              v-if="brand.logo"
              :src="brand.logo"
              fit="cover"
              class="brand-img"
            >
              <template #error>
                <div class="brand-fallback">{{ brand.name }}</div>
              </template>
            </el-image>
            <div v-else class="brand-fallback">{{ brand.name }}</div>
          </div>
          <div class="brand-info">
            <h4 class="brand-title">{{ brand.name }}</h4>
            <p class="brand-desc">{{ brand.description || '官方直营正品旗舰店' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Welcome Content Box -->
    <el-card shadow="never" class="welcome-box">
      <template #header>
        <div class="card-header">
          <span class="title">商城平台开发进度</span>
          <div class="tags">
            <el-tag type="success">Ticket 0001 脚手架</el-tag>
            <el-tag type="primary">Ticket 0002 认证与用户</el-tag>
            <el-tag type="warning">Ticket 0003 品牌与分类</el-tag>
          </div>
        </div>
      </template>
      <div class="status-content">
        <p><strong>Ticket 0003 品牌与分类管理已就绪：</strong></p>
        <p>• 后端：Deep CategoryService 纯函数 Stream 递归建树、Redis Cache-Aside 双写失效缓存；BrandService 多条件分页筛选与热门品牌推荐缓存。</p>
        <p>• 前端：管理后台品牌管理（文件上传 Logo / 分页列表 / 增删改查）与分类管理（树形表格 / 无级添加子分类 / 递归删除拦截）。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Van, Lightning, Service, Medal, Menu, CollectionTag } from '@element-plus/icons-vue'
import { getPortalCategoryTreeApi, type CategoryNode } from '@/api/category'
import { getFeaturedBrandsApi, type Brand } from '@/api/brand'

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

const categories = ref<CategoryNode[]>([])
const featuredBrands = ref<Brand[]>([])

async function loadCatalogData() {
  try {
    const [cats, brands] = await Promise.all([
      getPortalCategoryTreeApi(),
      getFeaturedBrandsApi(6)
    ])
    categories.value = cats
    featuredBrands.value = brands
  } catch (err) {
    console.error('加载前台类目与品牌失败', err)
  }
}

onMounted(() => {
  loadCatalogData()
})
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

/* Sections */
.section-container {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 20px;
  color: #409eff;
}

.section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.section-badge {
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: 6px;
}

/* Category Grid */
.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.category-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  transition: all 0.2s ease;
}

.category-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #bfdbfe;
}

.category-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.cat-name {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
}

.sub-category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sub-cat-pill {
  font-size: 12px;
  background: #f3f4f6;
  color: #4b5563;
  padding: 4px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
}

.sub-cat-pill:hover {
  background: #e0f2fe;
  color: #0284c7;
}

.empty-sub {
  font-size: 12px;
  color: #9ca3af;
}

/* Brand Grid */
.brand-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}

.brand-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border-color: #bfdbfe;
}

.brand-logo-wrap {
  width: 72px;
  height: 72px;
  margin-bottom: 12px;
}

.brand-img {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  border: 1px solid #f3f4f6;
}

.brand-fallback {
  width: 100%;
  height: 100%;
  background: #f3f4f6;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #6b7280;
  font-weight: 600;
}

.brand-info {
  width: 100%;
}

.brand-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.brand-desc {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

/* Welcome Box */
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

.tags {
  display: flex;
  gap: 8px;
}

.status-content p {
  margin: 8px 0;
  color: #4b5563;
  line-height: 1.6;
}
</style>
