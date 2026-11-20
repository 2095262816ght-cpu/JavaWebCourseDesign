<template>
  <div class="detail-page">
    <div class="page-container" v-loading="loading">
      <template v-if="product">
        <!-- 面包屑 -->
        <nav class="crumbs" aria-label="面包屑">
          <router-link to="/" class="crumb">首页</router-link>
          <span class="crumb-sep">/</span>
          <router-link to="/products" class="crumb">商品中心</router-link>
          <span class="crumb-sep">/</span>
          <router-link
            :to="{ path: '/products', query: { categoryId: product.categoryId } }"
            class="crumb"
          >
            {{ product.categoryName }}
          </router-link>
          <span class="crumb-sep">/</span>
          <span class="crumb is-current">{{ product.name }}</span>
        </nav>

        <!-- 主展示 -->
        <div class="showcase">
          <!-- 图集 -->
          <div class="gallery">
            <div class="main-frame">
              <span class="corner tl" aria-hidden="true"></span>
              <span class="corner tr" aria-hidden="true"></span>
              <span class="corner bl" aria-hidden="true"></span>
              <span class="corner br" aria-hidden="true"></span>
              <el-image
                :src="activeImage"
                fit="contain"
                class="main-img"
                :preview-src-list="galleryImages"
                :initial-index="Math.max(0, galleryImages.indexOf(activeImage))"
                preview-teleported
              >
                <template #error>
                  <div class="gallery-fallback"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <span class="zoom-hint mono">CLICK TO ZOOM</span>
            </div>

            <div v-if="galleryImages.length > 1" class="thumbs">
              <button
                v-for="(img, idx) in galleryImages"
                :key="idx"
                type="button"
                class="thumb"
                :class="{ 'is-active': img === activeImage }"
                @mouseenter="activeImage = img"
                @click="activeImage = img"
              >
                <el-image :src="img" fit="cover" class="thumb-img" />
              </button>
            </div>
          </div>

          <!-- 信息 -->
          <div class="info">
            <router-link
              :to="{ path: '/products', query: { brandId: product.brandId } }"
              class="brand-line"
            >
              <span class="brand-avatar">
                <el-image v-if="product.brandLogo" :src="product.brandLogo" fit="cover" class="brand-avatar-img">
                  <template #error>{{ product.brandName.slice(0, 2) }}</template>
                </el-image>
                <template v-else>{{ product.brandName.slice(0, 2) }}</template>
              </span>
              <span class="brand-name">{{ product.brandName }}</span>
              <span class="brand-tag mono">OFFICIAL</span>
              <span v-if="product.brandOrigin" class="brand-origin">产地 · {{ product.brandOrigin }}</span>
            </router-link>

            <h1 class="product-title">{{ product.name }}</h1>
            <p v-if="product.subtitle" class="product-sub">{{ product.subtitle }}</p>

            <!-- 价格面板 -->
            <div class="price-panel">
              <div class="price-main">
                <span class="price-label">售价</span>
                <span class="price">
                  <span class="symbol">¥</span>
                  <span class="amount">{{ Number(product.price).toFixed(2) }}</span>
                </span>
                <span
                  v-if="product.originalPrice && product.originalPrice > product.price"
                  class="price-original"
                >
                  ¥{{ Number(product.originalPrice).toFixed(2) }}
                </span>
                <span
                  v-if="product.originalPrice && product.originalPrice > product.price"
                  class="save-badge"
                >
                  立省 ¥{{ (product.originalPrice - product.price).toFixed(2) }}
                </span>
              </div>

              <div class="service-line">
                <span class="service-item"><el-icon><Select /></el-icon> 官方正品</span>
                <span class="service-item"><el-icon><Van /></el-icon> 顺丰包邮</span>
                <span class="service-item"><el-icon><RefreshLeft /></el-icon> 7 天无理由</span>
                <span class="service-item"><el-icon><Timer /></el-icon> 48H 发货</span>
              </div>
            </div>

            <!-- 规格 -->
            <dl class="spec-list">
              <div class="spec-row">
                <dt>商品品类</dt>
                <dd>{{ product.categoryName }}</dd>
              </div>
              <div class="spec-row" v-if="product.brandOrigin">
                <dt>品牌产地</dt>
                <dd>{{ product.brandOrigin }}</dd>
              </div>
              <div class="spec-row">
                <dt>库存状态</dt>
                <dd :class="product.stock > 0 ? 'is-ok' : 'is-bad'">
                  {{ product.stock > 0 ? `现货 ${product.stock} 件` : '已售罄' }}
                </dd>
              </div>
            </dl>

            <!-- 购买 -->
            <div v-if="product.status === 1 && product.stock > 0" class="buy-block">
              <div class="qty-line">
                <span class="qty-label">选购数量</span>
                <el-input-number v-model="quantity" :min="1" :max="product.stock" size="large" />
                <span class="qty-tip">库存 {{ product.stock }} 件</span>
              </div>

              <div class="actions">
                <button class="btn btn--ghost action-ghost" type="button" :disabled="submitting" @click="handleAddToCart">
                  <el-icon><ShoppingCart /></el-icon>
                  加入购物车
                </button>
                <button class="btn btn--primary action-main" type="button" :disabled="submitting" @click="handleBuyNow">
                  立即购买
                  <el-icon><Right /></el-icon>
                </button>
              </div>
            </div>

            <div v-else class="sold-out">
              <span class="sold-out-dot"></span>
              该商品暂时无法购买，请移步同类商品
            </div>
          </div>
        </div>

        <!-- 详情 -->
        <section class="detail-section" v-reveal>
          <div class="tabs">
            <button
              type="button"
              class="tab"
              :class="{ 'is-active': activeTab === 'details' }"
              @click="activeTab = 'details'"
            >
              商品详情
            </button>
            <button
              type="button"
              class="tab"
              :class="{ 'is-active': activeTab === 'service' }"
              @click="activeTab = 'service'"
            >
              服务与售后
            </button>
          </div>

          <div v-if="activeTab === 'details'" class="tab-body">
            <div v-if="safeDetailHtml" class="rich-html" v-html="safeDetailHtml"></div>
            <div v-else class="tab-empty">
              商家暂未录入富文本详情，如有疑问请咨询官方客服。
            </div>
          </div>

          <div v-else class="tab-body service-body">
            <div class="service-card">
              <span class="service-no mono">01</span>
              <h4>100% 正品保证</h4>
              <p>全场商品均来自正规渠道与官方授权，支持假一赔十。</p>
            </div>
            <div class="service-card">
              <span class="service-no mono">02</span>
              <h4>7 天无理由退换</h4>
              <p>签收后 7 天内，商品及包装完好即可申请退换货服务。</p>
            </div>
            <div class="service-card">
              <span class="service-no mono">03</span>
              <h4>闪电配送</h4>
              <p>现货订单工作日 16:00 前支付当天发出，支持全程物流追踪。</p>
            </div>
          </div>
        </section>

        <!-- 同类推荐 -->
        <section v-if="relatedProducts.length > 0" class="related-section">
          <div class="section-head">
            <div>
              <span class="eyebrow">YOU MAY ALSO LIKE</span>
              <h2 class="section-title" style="margin-top: 10px">同类好物</h2>
            </div>
            <router-link
              :to="{ path: '/products', query: { categoryId: product.categoryId } }"
              class="btn btn--ghost btn--sm"
            >
              查看全部
              <el-icon><Right /></el-icon>
            </router-link>
          </div>
          <div class="related-grid">
            <ProductCard
              v-for="(item, idx) in relatedProducts"
              :key="item.id"
              :product="item"
              :index="idx"
              v-reveal="idx * 60"
            />
          </div>
        </section>
      </template>

      <!-- 未找到 -->
      <div v-else-if="!loading" class="not-found">
        <span class="not-found-code mono">404</span>
        <h2>商品不存在或已下架</h2>
        <p>它可能已被管理员移除，去看看别的商品吧</p>
        <button class="btn btn--primary btn--sm" type="button" @click="$router.push('/products')">
          返回商品中心
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ShoppingCart,
  Right,
  Picture,
  Select,
  Van,
  RefreshLeft,
  Timer
} from '@element-plus/icons-vue'
import {
  getProductDetailApi,
  searchProductsApi,
  type ProductDetail,
  type ProductCard as ProductCardModel
} from '@/api/product'
import DOMPurify from 'dompurify'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const loading = ref(false)
const product = ref<ProductDetail | null>(null)
const activeImage = ref('')
const quantity = ref(1)
const activeTab = ref<'details' | 'service'>('details')
const relatedProducts = ref<ProductCardModel[]>([])
const submitting = ref(false)

// 富文本详情来自管理端录入，渲染前做白名单净化，避免脚本/事件属性在访客浏览器执行
const safeDetailHtml = computed(() =>
  product.value?.detailHtml ? DOMPurify.sanitize(product.value.detailHtml) : ''
)

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

// 商品切换时详情与推荐请求可能乱序返回，只采纳最后一次请求的结果
let detailRequestSeq = 0
let relatedRequestSeq = 0

async function fetchRelated(categoryId: number, excludeId: number) {
  const seq = ++relatedRequestSeq
  relatedProducts.value = []
  try {
    const res = await searchProductsApi({ categoryId, pageNum: 1, pageSize: 5 })
    if (seq !== relatedRequestSeq) return
    relatedProducts.value = (res?.records || [])
      .filter((item) => item.id !== excludeId)
      .slice(0, 4)
  } catch (err) {
    console.error('加载同类推荐失败', err)
  }
}

async function fetchDetail() {
  const seq = ++detailRequestSeq
  // 商品切换时同时作废上一件商品尚未返回的同类推荐
  relatedRequestSeq += 1
  const id = Number(route.params.id)
  if (!id || Number.isNaN(id)) {
    product.value = null
    activeImage.value = ''
    relatedProducts.value = []
    loading.value = false
    return
  }

  loading.value = true
  product.value = null
  activeImage.value = ''
  relatedProducts.value = []
  try {
    const res = await getProductDetailApi(id)
    if (seq !== detailRequestSeq) return
    if (res) {
      product.value = res
      activeImage.value = res.mainImage || (res.subImages?.[0] ?? '')
      fetchRelated(res.categoryId, res.id)
    }
  } catch (err) {
    console.error('获取商品详情失败', err)
  } finally {
    if (seq === detailRequestSeq) {
      loading.value = false
    }
  }
}

async function handleAddToCart() {
  if (!product.value || submitting.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录账号后再加购商品')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  submitting.value = true
  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    ElMessage.success(`已成功将 ${quantity.value} 件【${product.value.name}】加入购物车`)
  } catch (err: any) {
    console.error('加入购物车失败', err)
  } finally {
    submitting.value = false
  }
}

async function handleBuyNow() {
  if (!product.value || submitting.value) return
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录账号后再结算商品')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  submitting.value = true
  try {
    await cartStore.addToCart(product.value.id, quantity.value)
    router.push('/cart')
  } catch (err: any) {
    console.error('立即购买失败', err)
  } finally {
    submitting.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    quantity.value = 1
    activeTab.value = 'details'
    fetchDetail()
  }
)

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.detail-page {
  padding-top: 24px;
}

/* 面包屑 */
.crumbs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 10px 0 28px;
  font-size: 12px;
  color: var(--text-3);
}

.crumb {
  transition: color 0.25s var(--ease);
}

.crumb:hover {
  color: var(--cyan);
}

.crumb.is-current {
  color: var(--text-2);
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.crumb-sep {
  color: var(--text-4);
  opacity: 0.5;
}

/* 展示 */
.showcase {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(0, 1fr);
  gap: clamp(28px, 4vw, 56px);
  align-items: start;
}

/* 图集 */
.gallery {
  position: sticky;
  top: 96px;
}

.main-frame {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-xl);
  border: 1px solid var(--line);
  background: radial-gradient(circle at 50% 40%, #141a2c 0%, #080a12 100%);
  overflow: hidden;
  box-shadow: var(--shadow-card), inset 0 0 90px rgba(124, 92, 255, 0.08);
}

.main-frame::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 70% 15%, rgba(34, 211, 238, 0.1), transparent 55%);
  pointer-events: none;
}

.main-img {
  width: 100%;
  height: 100%;
  cursor: zoom-in;
}

.gallery-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-4);
  font-size: 34px;
}

/* 四角标记 */
.corner {
  position: absolute;
  width: 16px;
  height: 16px;
  z-index: 2;
  border-color: rgba(34, 211, 238, 0.7);
  border-style: solid;
  border-width: 0;
  pointer-events: none;
}

.corner.tl {
  top: 14px;
  left: 14px;
  border-top-width: 1px;
  border-left-width: 1px;
}

.corner.tr {
  top: 14px;
  right: 14px;
  border-top-width: 1px;
  border-right-width: 1px;
}

.corner.bl {
  bottom: 14px;
  left: 14px;
  border-bottom-width: 1px;
  border-left-width: 1px;
}

.corner.br {
  bottom: 14px;
  right: 14px;
  border-bottom-width: 1px;
  border-right-width: 1px;
}

.zoom-hint {
  position: absolute;
  right: 18px;
  bottom: 16px;
  z-index: 2;
  font-size: 9px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

.thumbs {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  overflow-x: auto;
  padding-bottom: 4px;
}

.thumb {
  width: 76px;
  height: 76px;
  flex-shrink: 0;
  padding: 0;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  background: #0b0e18;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s var(--ease);
}

.thumb:hover {
  border-color: rgba(255, 255, 255, 0.28);
}

.thumb.is-active {
  border-color: rgba(124, 92, 255, 0.9);
  box-shadow: 0 0 0 3px rgba(124, 92, 255, 0.18);
}

.thumb-img {
  width: 100%;
  height: 100%;
}

/* 信息 */
.info {
  padding-top: 6px;
}

.brand-line {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 5px 14px 5px 5px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.04);
  transition: all 0.3s var(--ease);
}

.brand-line:hover {
  border-color: rgba(124, 92, 255, 0.5);
  background: rgba(124, 92, 255, 0.1);
}

.brand-avatar {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  overflow: hidden;
  background: rgba(124, 92, 255, 0.2);
  color: #c4b8ff;
  font-size: 10px;
  font-weight: 700;
}

.brand-avatar-img {
  width: 100%;
  height: 100%;
}

.brand-name {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text);
}

.brand-tag {
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--cyan);
}

.brand-origin {
  padding-left: 10px;
  border-left: 1px solid var(--line);
  font-size: 11px;
  color: var(--text-3);
}

.product-title {
  margin-top: 22px;
  font-size: clamp(26px, 3vw, 38px);
  font-weight: 700;
  line-height: 1.22;
  letter-spacing: -0.03em;
}

.product-sub {
  margin-top: 12px;
  font-size: 14px;
  color: var(--text-2);
  line-height: 1.7;
}

/* 价格 */
.price-panel {
  margin-top: 28px;
  padding: 22px 24px;
  border: 1px solid rgba(124, 92, 255, 0.24);
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, rgba(124, 92, 255, 0.12), rgba(34, 211, 238, 0.04));
}

.price-main {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.price-label {
  font-size: 12px;
  color: var(--text-3);
}

.price-main .price .amount {
  font-size: 40px;
  line-height: 1;
}

.price-original {
  font-size: 13px;
  color: var(--text-4);
  text-decoration: line-through;
}

.save-badge {
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(255, 92, 122, 0.14);
  border: 1px solid rgba(255, 92, 122, 0.35);
  color: #ff9db0;
  font-size: 11px;
  font-weight: 600;
}

.service-line {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.service-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-2);
}

.service-item .el-icon {
  color: var(--cyan);
  font-size: 13px;
}

/* 规格 */
.spec-list {
  margin: 26px 0 0;
  padding: 0;
  display: grid;
  gap: 0;
}

.spec-row {
  display: flex;
  align-items: center;
  padding: 13px 4px;
  border-bottom: 1px dashed var(--line);
}

.spec-row dt {
  width: 96px;
  font-size: 12.5px;
  color: var(--text-3);
}

.spec-row dd {
  margin: 0;
  font-size: 13px;
  color: var(--text);
}

.spec-row dd.is-ok {
  color: var(--green);
}

.spec-row dd.is-bad {
  color: var(--price);
}

/* 购买 */
.buy-block {
  margin-top: 28px;
}

.qty-line {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.qty-label {
  font-size: 13px;
  color: var(--text-2);
}

.qty-tip {
  font-size: 11.5px;
  color: var(--text-4);
}

.actions {
  display: flex;
  gap: 14px;
}

.action-ghost,
.action-main {
  flex: 1;
  height: 54px;
  border-radius: 999px;
  font-size: 15px;
}

.sold-out {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
  padding: 18px 20px;
  border: 1px solid rgba(255, 92, 122, 0.3);
  border-radius: var(--radius);
  background: rgba(255, 92, 122, 0.08);
  font-size: 13px;
  color: #ff9db0;
}

.sold-out-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--price);
  box-shadow: 0 0 10px var(--price);
}

/* 详情 */
.detail-section {
  margin-top: 64px;
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.025);
}

.tabs {
  display: flex;
  gap: 6px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 26px;
}

.tab {
  position: relative;
  padding: 12px 20px;
  border: none;
  background: none;
  color: var(--text-3);
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.25s var(--ease);
}

.tab::after {
  content: '';
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: var(--grad);
  transform: scaleX(0);
  transition: transform 0.35s var(--ease);
}

.tab.is-active {
  color: var(--text);
  font-weight: 600;
}

.tab.is-active::after {
  transform: scaleX(1);
}

.tab-body {
  animation: fade-in 0.4s var(--ease);
}

@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.rich-html {
  font-size: 14.5px;
  line-height: 1.9;
  color: var(--text-2);
}

.rich-html :deep(img) {
  max-width: 100%;
  border-radius: var(--radius);
  margin: 16px 0;
}

.rich-html :deep(h1),
.rich-html :deep(h2),
.rich-html :deep(h3) {
  color: var(--text);
  margin: 20px 0 10px;
}

.rich-html :deep(a) {
  color: var(--cyan);
}

.tab-empty {
  padding: 50px 0;
  text-align: center;
  font-size: 13px;
  color: var(--text-4);
}

.service-body {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.service-card {
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.025);
}

.service-no {
  font-size: 11px;
  letter-spacing: 0.18em;
  color: var(--violet);
}

.service-card h4 {
  margin: 10px 0 8px;
  font-size: 15px;
  font-weight: 600;
}

.service-card p {
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-3);
}

/* 推荐 */
.related-section {
  margin-top: 72px;
}

.related-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 22px;
}

/* 404 */
.not-found {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 120px 24px;
  text-align: center;
}

.not-found-code {
  font-size: 12px;
  letter-spacing: 0.4em;
  color: var(--violet);
}

.not-found h2 {
  font-size: 26px;
  font-weight: 700;
}

.not-found p {
  font-size: 13px;
  color: var(--text-3);
  margin-bottom: 10px;
}

/* 响应式 */
@media (max-width: 1080px) {
  .showcase {
    grid-template-columns: 1fr;
  }

  .gallery {
    position: static;
    max-width: 560px;
  }

  .related-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .service-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .actions {
    flex-direction: column;
  }

  .action-ghost {
    order: 2;
  }

  .detail-section {
    padding: 18px;
  }

  .related-grid {
    grid-template-columns: 1fr;
  }
}
</style>
