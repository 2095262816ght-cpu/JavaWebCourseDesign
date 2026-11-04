<template>
  <div class="home-container">
    <!-- Hero Showcase Carousel (From single /portal/home payload) -->
    <div class="hero-section" v-loading="loading">
      <el-carousel height="420px" indicator-position="outside" :interval="5000">
        <el-carousel-item v-for="item in carousels" :key="item.id">
          <div
            class="banner-card"
            :style="{ backgroundImage: `url(${item.image})` }"
            @click="handleBannerClick(item)"
          >
            <div class="banner-overlay">
              <div class="banner-content">
                <span class="banner-tag">旗舰精选 · 官方直营</span>
                <h2 class="banner-title">{{ item.title }}</h2>
                <p class="banner-subtitle">{{ item.subtitle }}</p>
                <div class="banner-actions">
                  <el-button type="primary" size="large" round>立即选购</el-button>
                  <el-button
                    type="info"
                    size="large"
                    round
                    plain
                    @click.stop="$router.push('/products')"
                  >
                    全部商品
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- Quick Features Value Proposition -->
    <div class="features-bar">
      <div class="feature-item">
        <el-icon class="feature-icon"><Van /></el-icon>
        <div>
          <h4>官方正品保证</h4>
          <p>严选官方一手货源 假一赔十</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon class="feature-icon"><Lightning /></el-icon>
        <div>
          <h4>极速物流配送</h4>
          <p>顺丰速运 智能仓储 次日必达</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon class="feature-icon"><Service /></el-icon>
        <div>
          <h4>七天无理由退换</h4>
          <p>上门取件 极速退款 售后无忧</p>
        </div>
      </div>
      <div class="feature-item">
        <el-icon class="feature-icon"><Medal /></el-icon>
        <div>
          <h4>专属会员权益</h4>
          <p>尊享折上折与新品优先购</p>
        </div>
      </div>
    </div>

    <!-- Hot Recommendation Products Section (T0004) -->
    <div class="section-container" v-if="hotProducts.length > 0">
      <div class="section-header">
        <div class="header-left">
          <div class="section-icon-wrap hot-gradient">
            <el-icon><HotWater /></el-icon>
          </div>
          <div>
            <h3 class="section-title">热销爆款甄选</h3>
            <span class="section-subtitle">人气口碑推荐 · 限时优惠特价</span>
          </div>
        </div>
        <el-button link type="primary" @click="$router.push('/products')">
          查看更多商品 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="product-grid">
        <div
          v-for="product in hotProducts"
          :key="product.id"
          class="product-card"
          @click="viewProduct(product.id)"
        >
          <div class="product-img-wrap">
            <el-image
              :src="product.mainImage"
              fit="cover"
              class="product-img"
              loading="lazy"
            >
              <template #error>
                <div class="img-fallback">暂无图片</div>
              </template>
            </el-image>
            <div class="badge-brand">{{ product.brandName }}</div>
          </div>

          <div class="product-info">
            <div class="product-meta">
              <el-tag size="small" type="info" effect="plain">{{ product.categoryName }}</el-tag>
              <span class="stock-status" :class="{ 'low-stock': product.stock < 10 }">
                库存 {{ product.stock }}
              </span>
            </div>
            <h4 class="product-name" :title="product.name">{{ product.name }}</h4>
            <p class="product-sub" :title="product.subtitle">{{ product.subtitle || '品质保证 尊享现货' }}</p>

            <div class="price-action-row">
              <div class="price-box">
                <span class="currency">¥</span>
                <span class="price-val">{{ Number(product.price).toFixed(2) }}</span>
                <span
                  class="original-price"
                  v-if="product.originalPrice && product.originalPrice > product.price"
                >
                  ¥{{ Number(product.originalPrice).toFixed(2) }}
                </span>
              </div>
              <el-button type="primary" size="small" round @click.stop="viewProduct(product.id)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Quick Brand Wall (T0004) -->
    <div class="section-container" v-if="featuredBrands.length > 0">
      <div class="section-header">
        <div class="header-left">
          <div class="section-icon-wrap brand-gradient">
            <el-icon><CollectionTag /></el-icon>
          </div>
          <div>
            <h3 class="section-title">官方入驻品牌</h3>
            <span class="section-subtitle">知名大牌联合旗舰店 · 品质之选</span>
          </div>
        </div>
        <el-button link type="primary" @click="$router.push('/products')">
          按品牌选购 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="brand-grid">
        <div
          v-for="brand in featuredBrands"
          :key="brand.id"
          class="brand-card"
          @click="$router.push({ path: '/products', query: { brandId: brand.id } })"
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
            <p class="brand-desc">{{ brand.description || '官方直营旗舰店' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- New Arrivals Section (T0004) -->
    <div class="section-container" v-if="newProducts.length > 0">
      <div class="section-header">
        <div class="header-left">
          <div class="section-icon-wrap new-gradient">
            <el-icon><Present /></el-icon>
          </div>
          <div>
            <h3 class="section-title">新品首发上架</h3>
            <span class="section-subtitle">第一时间抢鲜体验 · 先锋潮品</span>
          </div>
        </div>
        <el-button link type="primary" @click="$router.push({ path: '/products', query: { sortBy: 'newest' } })">
          浏览最新商品 <el-icon class="el-icon--right"><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="product-grid">
        <div
          v-for="product in newProducts"
          :key="product.id"
          class="product-card"
          @click="viewProduct(product.id)"
        >
          <div class="product-img-wrap">
            <el-image
              :src="product.mainImage"
              fit="cover"
              class="product-img"
              loading="lazy"
            >
              <template #error>
                <div class="img-fallback">暂无图片</div>
              </template>
            </el-image>
            <div class="badge-brand">{{ product.brandName }}</div>
          </div>

          <div class="product-info">
            <div class="product-meta">
              <el-tag size="small" type="success" effect="plain">{{ product.categoryName }}</el-tag>
              <span class="stock-status">现货库存 {{ product.stock }}</span>
            </div>
            <h4 class="product-name" :title="product.name">{{ product.name }}</h4>
            <p class="product-sub" :title="product.subtitle">{{ product.subtitle || '先锋体验 · 官方首发' }}</p>

            <div class="price-action-row">
              <div class="price-box">
                <span class="currency">¥</span>
                <span class="price-val">{{ Number(product.price).toFixed(2) }}</span>
                <span
                  class="original-price"
                  v-if="product.originalPrice && product.originalPrice > product.price"
                >
                  ¥{{ Number(product.originalPrice).toFixed(2) }}
                </span>
              </div>
              <el-button type="primary" size="small" round @click.stop="viewProduct(product.id)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Category Taxonomy Grid -->
    <div class="section-container" v-if="categories.length > 0">
      <div class="section-header">
        <div class="header-left">
          <div class="section-icon-wrap cat-gradient">
            <el-icon><Menu /></el-icon>
          </div>
          <div>
            <h3 class="section-title">全部分类导航</h3>
            <span class="section-subtitle">多级层级树架构 · Redis 缓存加速</span>
          </div>
        </div>
      </div>

      <div class="category-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="category-card"
        >
          <div class="category-card-header" @click="$router.push({ path: '/products', query: { categoryId: cat.id } })">
            <span class="cat-name">{{ cat.name }}</span>
            <el-tag size="small" type="primary" round>{{ cat.children?.length || 0 }} 个子分类</el-tag>
          </div>
          <div class="sub-category-list" v-if="cat.children && cat.children.length > 0">
            <span
              v-for="sub in cat.children"
              :key="sub.id"
              class="sub-cat-pill"
              @click="$router.push({ path: '/products', query: { categoryId: sub.id } })"
            >
              {{ sub.name }}
            </span>
          </div>
          <div v-else class="empty-sub">暂无子分类</div>
        </div>
      </div>
    </div>

    <!-- Development Status Card -->
    <el-card shadow="never" class="welcome-box">
      <template #header>
        <div class="card-header">
          <span class="title">商城平台架构进度</span>
          <div class="tags">
            <el-tag type="success">Ticket 0001 脚手架</el-tag>
            <el-tag type="success">Ticket 0002 认证与用户</el-tag>
            <el-tag type="success">Ticket 0003 品牌与分类</el-tag>
            <el-tag type="primary">Ticket 0004 商品与前台橱窗</el-tag>
          </div>
        </div>
      </template>
      <div class="status-content">
        <p><strong>Ticket 0004 商品与前台聚合展示切片已落地：</strong></p>
        <p>• 后端：Deep ProductCatalogService（单请求聚合首页数据消除瀑布流网络开销、多维动态组合检索、商品详情及副图画廊解析）+ ProductAdminService（品牌分类存在性硬校验、上下架快速切换、Redis 双写淘汰缓存）。</p>
        <p>• 前端：商城首页零瀑布流聚合橱窗、商品中心多维过滤与排序、商品详情画廊切换、管理后台商品增删改查与图片上传。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Van,
  Lightning,
  Service,
  Medal,
  Menu,
  CollectionTag,
  ArrowRight,
  HotWater,
  Present
} from '@element-plus/icons-vue'
import {
  getHomeShowcaseApi,
  type CarouselSlide,
  type ProductCard
} from '@/api/product'
import type { CategoryNode } from '@/api/category'
import type { Brand } from '@/api/brand'

const router = useRouter()
const loading = ref(false)

const carousels = ref<CarouselSlide[]>([])
const categories = ref<CategoryNode[]>([])
const featuredBrands = ref<Brand[]>([])
const hotProducts = ref<ProductCard[]>([])
const newProducts = ref<ProductCard[]>([])

async function loadHomeShowcase() {
  loading.value = true
  try {
    const data = await getHomeShowcaseApi()
    if (data) {
      carousels.value = data.carousels || []
      categories.value = data.categories || []
      featuredBrands.value = data.featuredBrands || []
      hotProducts.value = data.hotProducts || []
      newProducts.value = data.newProducts || []
    }
  } catch (err) {
    console.error('加载首页聚合橱窗失败', err)
  } finally {
    loading.value = false
  }
}

function handleBannerClick(item: CarouselSlide) {
  if (item.productId) {
    router.push(`/product/${item.productId}`)
  } else if (item.targetUrl) {
    router.push(item.targetUrl)
  } else {
    router.push('/products')
  }
}

function viewProduct(id: number) {
  router.push(`/product/${id}`)
}

onMounted(() => {
  loadHomeShowcase()
})
</script>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.hero-section {
  border-radius: 16px;
  overflow: hidden;
  margin-bottom: 24px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
}

.banner-card {
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
  cursor: pointer;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(15, 23, 42, 0.88) 0%, rgba(15, 23, 42, 0.45) 55%, transparent 100%);
  display: flex;
  align-items: center;
  padding: 0 60px;
}

.banner-content {
  max-width: 620px;
  color: #fff;
}

.banner-tag {
  display: inline-block;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(8px);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 14px;
  letter-spacing: 0.5px;
}

.banner-title {
  font-size: 34px;
  font-weight: 800;
  line-height: 1.25;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}

.banner-subtitle {
  font-size: 16px;
  color: #cbd5e1;
  margin-bottom: 28px;
  line-height: 1.5;
}

.banner-actions {
  display: flex;
  gap: 14px;
}

/* Features Bar */
.features-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.feature-icon {
  font-size: 32px;
  color: #409eff;
  background: #ecf5ff;
  padding: 10px;
  border-radius: 12px;
}

.feature-item h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.feature-item p {
  font-size: 12px;
  color: #64748b;
}

/* Section Containers */
.section-container {
  margin-bottom: 36px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.hot-gradient {
  background: linear-gradient(135deg, #f43f5e 0%, #fb7185 100%);
}

.brand-gradient {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
}

.new-gradient {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
}

.cat-gradient {
  background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%);
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.section-subtitle {
  font-size: 13px;
  color: #64748b;
  margin-left: 6px;
}

/* Product Card Grid */
.product-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  transition: all 0.25s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  border: 1px solid #f1f5f9;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: #e2e8f0;
}

.product-img-wrap {
  position: relative;
  width: 100%;
  height: 220px;
  background: #f8fafc;
  overflow: hidden;
}

.product-img {
  width: 100%;
  height: 100%;
  transition: transform 0.3s ease;
}

.product-card:hover .product-img {
  transform: scale(1.05);
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}

.badge-brand {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(4px);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 4px;
}

.product-info {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.stock-status {
  font-size: 12px;
  color: #64748b;
}

.low-stock {
  color: #e11d48;
  font-weight: 600;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-sub {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price-action-row {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.currency {
  font-size: 14px;
  font-weight: 700;
  color: #ef4444;
}

.price-val {
  font-size: 20px;
  font-weight: 800;
  color: #ef4444;
}

.original-price {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
  margin-left: 4px;
}

/* Brand Grid */
.brand-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.brand-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;
  cursor: pointer;
  border: 1px solid #f1f5f9;
}

.brand-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.06);
  border-color: #409eff;
}

.brand-logo-wrap {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  overflow: hidden;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.brand-img {
  width: 100%;
  height: 100%;
}

.brand-fallback {
  font-size: 14px;
  font-weight: 700;
  color: #3b82f6;
  text-align: center;
}

.brand-info {
  overflow: hidden;
}

.brand-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.brand-desc {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Category Grid */
.category-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.category-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.category-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  cursor: pointer;
}

.category-card-header:hover .cat-name {
  color: #409eff;
}

.cat-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  transition: color 0.2s;
}

.sub-category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sub-cat-pill {
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.sub-cat-pill:hover {
  background: #e0f2fe;
  color: #0284c7;
}

.empty-sub {
  font-size: 12px;
  color: #94a3b8;
}

.welcome-box {
  margin-top: 32px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 16px;
  font-weight: 600;
}

.card-header .tags {
  display: flex;
  gap: 8px;
}

.status-content p {
  margin-bottom: 8px;
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}
</style>
