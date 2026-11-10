<template>
  <div class="product-detail-container" v-loading="loading">
    <template v-if="product">
      <!-- Breadcrumb -->
      <div class="detail-breadcrumb">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item :to="{ path: '/products' }">商品中心</el-breadcrumb-item>
          <el-breadcrumb-item
            :to="{ path: '/products', query: { categoryId: product.categoryId } }"
          >
            {{ product.categoryName }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ product.name }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <!-- Main Showcase Section -->
      <div class="detail-hero-card">
        <!-- Left: Image Gallery & Thumbnail Switcher -->
        <div class="gallery-column">
          <div class="main-preview-box">
            <el-image
              :src="activeImage"
              fit="contain"
              class="main-preview-img"
              :preview-src-list="galleryImages"
            >
              <template #error>
                <div class="gallery-placeholder">暂无主图</div>
              </template>
            </el-image>
            <div class="zoom-hint">点击查看高清大图</div>
          </div>

          <!-- Thumbnails Strip -->
          <div class="thumbnails-strip" v-if="galleryImages.length > 1">
            <div
              v-for="(img, idx) in galleryImages"
              :key="idx"
              class="thumb-item"
              :class="{ active: img === activeImage }"
              @mouseenter="activeImage = img"
              @click="activeImage = img"
            >
              <el-image :src="img" fit="cover" class="thumb-img" />
            </div>
          </div>
        </div>

        <!-- Right: Meta, Pricing, Stock & Actions -->
        <div class="meta-column">
          <!-- Brand Pill -->
          <div class="brand-pill" @click="$router.push({ path: '/products', query: { brandId: product.brandId } })">
            <el-avatar
              v-if="product.brandLogo"
              :size="24"
              :src="product.brandLogo"
              class="brand-mini-logo"
            />
            <span class="brand-name">{{ product.brandName }}</span>
            <el-tag size="small" type="primary" round effect="light">品牌官方直营</el-tag>
            <el-tag v-if="product.brandOrigin" size="small" type="info" round effect="light">产地: {{ product.brandOrigin }}</el-tag>
          </div>

          <h1 class="product-title">{{ product.name }}</h1>
          <p class="product-tagline" v-if="product.subtitle">{{ product.subtitle }}</p>

          <!-- Price Panel -->
          <div class="price-panel">
            <div class="price-row">
              <span class="price-label">售价</span>
              <span class="currency-symbol">¥</span>
              <span class="current-price">{{ Number(product.price).toFixed(2) }}</span>
              <span
                v-if="product.originalPrice && product.originalPrice > product.price"
                class="original-price"
              >
                ¥{{ Number(product.originalPrice).toFixed(2) }}
              </span>
              <el-tag
                v-if="product.originalPrice && product.originalPrice > product.price"
                type="danger"
                size="small"
                effect="dark"
                round
              >
                优惠 ¥{{ (product.originalPrice - product.price).toFixed(2) }}
              </el-tag>
            </div>

            <div class="service-row">
              <span class="service-label">保障</span>
              <div class="service-tags">
                <span class="s-tag"><el-icon><Check /></el-icon> 官方正品</span>
                <span class="s-tag"><el-icon><Van /></el-icon> 顺丰包邮</span>
                <span class="s-tag"><el-icon><Service /></el-icon> 7天无理由退换</span>
                <span class="s-tag"><el-icon><Finished /></el-icon> 极速发货</span>
              </div>
            </div>
          </div>

          <!-- Stock & Category Info -->
          <div class="props-list">
            <div class="prop-item">
              <span class="prop-label">商品品类</span>
              <span class="prop-value">{{ product.categoryName }}</span>
            </div>
            <div class="prop-item" v-if="product.brandOrigin">
              <span class="prop-label">品牌产地</span>
              <span class="prop-value">{{ product.brandOrigin }}</span>
            </div>
            <div class="prop-item">
              <span class="prop-label">库存状态</span>
              <span
                class="prop-value"
                :class="{ 'in-stock': product.stock > 0, 'out-of-stock': product.stock <= 0 }"
              >
                {{ product.stock > 0 ? `现货充足 (${product.stock} 件)` : '已售罄缺货' }}
              </span>
            </div>
            <div class="prop-item">
              <span class="prop-label">上架状态</span>
              <span class="prop-value">
                <el-tag :type="product.status === 1 ? 'success' : 'info'" size="small">
                  {{ product.status === 1 ? '正常销售中' : '已下架' }}
                </el-tag>
              </span>
            </div>
          </div>

          <!-- Purchase Quantity Stepper -->
          <div class="quantity-row" v-if="product.status === 1 && product.stock > 0">
            <span class="qty-label">选购数量</span>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="product.stock"
              size="large"
            />
            <span class="qty-stock-tip">（最多可选购 {{ product.stock }} 件）</span>
          </div>

          <!-- Action Buttons -->
          <div class="actions-row">
            <el-button
              type="warning"
              size="large"
              :icon="ShoppingCart"
              class="action-btn cart-btn"
              :disabled="product.status !== 1 || product.stock <= 0"
              @click="handleAddToCart"
            >
              加入购物车
            </el-button>
            <el-button
              type="danger"
              size="large"
              class="action-btn buy-btn"
              :disabled="product.status !== 1 || product.stock <= 0"
              @click="handleBuyNow"
            >
              立即购买
            </el-button>
          </div>
        </div>
      </div>

      <!-- Detail Rich Text Section -->
      <div class="detail-body-card">
        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane label="商品详情规格" name="details">
            <div class="rich-html-content" v-if="product.detailHtml" v-html="product.detailHtml"></div>
            <div v-else class="empty-rich">
              <p>商家暂未录入富文本详情，如有疑问请咨询官方客服。</p>
            </div>
          </el-tab-pane>
          <el-tab-pane label="服务与售后" name="service">
            <div class="service-content">
              <h3>官方服务承诺</h3>
              <p>1. <strong>100% 正品保证</strong>：全场商品均来自正规渠道与官方授权，假一赔十。</p>
              <p>2. <strong>7天无理由退换</strong>：在签收货物后7天内，商品及包装完好无损可申请退换货服务。</p>
              <p>3. <strong>闪电配送</strong>：现货订单在工作日 16:00 前完成支付的当天发出，支持全程顺丰物流追踪。</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>

    <div v-else-if="!loading" class="not-found-card">
      <el-empty description="抱歉，该商品不存在或已被管理员下架">
        <el-button type="primary" @click="$router.push('/products')">返回商品中心</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ShoppingCart,
  Check,
  Van,
  Service,
  Finished
} from '@element-plus/icons-vue'
import { getProductDetailApi, type ProductDetail } from '@/api/product'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const loading = ref(false)
const product = ref<ProductDetail | null>(null)
const activeImage = ref('')
const quantity = ref(1)
const activeTab = ref('details')

const galleryImages = computed(() => {
  if (!product.value) return []
  const list: string[] = []
  if (product.value.mainImage) {
    list.push(product.value.mainImage)
  }
  if (product.value.subImages && product.value.subImages.length > 0) {
    for (const img of product.value.subImages) {
      if (img && !list.includes(img)) {
        list.push(img)
      }
    }
  }
  return list
})

async function fetchDetail() {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res = await getProductDetailApi(id)
    if (res) {
      product.value = res
      activeImage.value = res.mainImage || (res.subImages?.[0] ?? '')
    }
  } catch (err) {
    console.error('获取商品详情失败', err)
  } finally {
    loading.value = false
  }
}

async function handleAddToCart() {
  if (!product.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录账号后再加购商品')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    ElMessage.success(`已成功将 ${quantity.value} 件【${product.value.name}】加入购物车`)
  } catch (err: any) {
    ElMessage.error(err.message || '加入购物车失败')
  }
}

async function handleBuyNow() {
  if (!product.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录账号后再结算商品')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    router.push('/cart')
  } catch (err: any) {
    ElMessage.error(err.message || '立即购买失败')
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.product-detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.detail-breadcrumb {
  margin-bottom: 20px;
}

.detail-hero-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
  display: flex;
  gap: 40px;
  margin-bottom: 32px;
}

/* Gallery Column */
.gallery-column {
  width: 460px;
  flex-shrink: 0;
}

.main-preview-box {
  width: 100%;
  height: 420px;
  border-radius: 12px;
  background: #f8fafc;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e2e8f0;
}

.main-preview-img {
  width: 100%;
  height: 100%;
  cursor: zoom-in;
}

.gallery-placeholder {
  color: #94a3b8;
  font-size: 14px;
}

.zoom-hint {
  position: absolute;
  bottom: 12px;
  right: 12px;
  background: rgba(15, 23, 42, 0.6);
  color: #fff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  pointer-events: none;
}

.thumbnails-strip {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.thumb-item {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  border: 2px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.thumb-item.active {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(64, 158, 255, 0.2);
}

.thumb-img {
  width: 100%;
  height: 100%;
}

/* Meta Column */
.meta-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.brand-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f8fafc;
  padding: 4px 12px 4px 6px;
  border-radius: 20px;
  width: fit-content;
  cursor: pointer;
  margin-bottom: 14px;
  border: 1px solid #e2e8f0;
  transition: all 0.2s;
}

.brand-pill:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.brand-name {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.product-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.35;
  margin-bottom: 10px;
}

.product-tagline {
  font-size: 14px;
  color: #64748b;
  line-height: 1.5;
  margin-bottom: 20px;
}

/* Price Panel */
.price-panel {
  background: #f8fafc;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border-left: 4px solid #ef4444;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 16px;
}

.price-label {
  font-size: 13px;
  color: #64748b;
}

.currency-symbol {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

.current-price {
  font-size: 32px;
  font-weight: 800;
  color: #ef4444;
}

.original-price {
  font-size: 14px;
  color: #94a3b8;
  text-decoration: line-through;
  margin-left: 6px;
}

.service-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
}

.service-label {
  color: #64748b;
  font-weight: 500;
}

.service-tags {
  display: flex;
  gap: 16px;
  color: #475569;
}

.s-tag {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Props List */
.props-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
  padding: 0 4px;
}

.prop-item {
  display: flex;
  font-size: 13px;
}

.prop-label {
  width: 80px;
  color: #64748b;
}

.prop-value {
  color: #1e293b;
  font-weight: 500;
}

.in-stock {
  color: #10b981;
}

.out-of-stock {
  color: #ef4444;
}

/* Quantity Row */
.quantity-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  padding: 0 4px;
}

.qty-label {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.qty-stock-tip {
  font-size: 12px;
  color: #94a3b8;
}

/* Action Buttons */
.actions-row {
  display: flex;
  gap: 16px;
  margin-top: auto;
}

.action-btn {
  flex: 1;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 24px;
}

/* Detail Body Tabs */
.detail-body-card {
  background: #fff;
  border-radius: 16px;
  padding: 28px 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.rich-html-content {
  line-height: 1.8;
  color: #334155;
  font-size: 15px;
}

.rich-html-content :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.empty-rich {
  padding: 40px 0;
  text-align: center;
  color: #94a3b8;
}

.service-content {
  line-height: 2;
  color: #475569;
}

.service-content h3 {
  font-size: 16px;
  color: #1e293b;
  margin-bottom: 12px;
}

.not-found-card {
  padding: 80px 0;
  background: #fff;
  border-radius: 16px;
}
</style>
