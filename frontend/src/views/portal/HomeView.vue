<template>
  <div class="home">
    <!-- ================= HERO ================= -->
    <section class="hero page-container">
      <div class="hero-copy">
        <span class="eyebrow">CURATED COMMERCE / 2026</span>
        <h1 class="display-xl hero-title">
          把每一次<br />
          <span class="text-gradient">挑选</span>都变成仪式
        </h1>
        <p class="hero-desc">
          严选官方正品，覆盖数码、生活与潮流品类。<br />
          全场顺丰包邮，48 小时内极速发货。
        </p>

        <div class="hero-actions">
          <button class="btn btn--primary" type="button" @click="$router.push('/products')">
            开始探索
            <el-icon><Right /></el-icon>
          </button>
          <button class="btn btn--ghost" type="button" @click="scrollToSection('hot')">
            查看热销
          </button>
        </div>

        <div class="hero-stats">
          <div class="stat">
            <span class="stat-value mono">{{ totalProducts }}</span>
            <span class="stat-label">精选在售</span>
          </div>
          <span class="stat-divider"></span>
          <div class="stat">
            <span class="stat-value mono">{{ featuredBrands.length }}</span>
            <span class="stat-label">官方品牌</span>
          </div>
          <span class="stat-divider"></span>
          <div class="stat">
            <span class="stat-value mono">{{ categories.length }}</span>
            <span class="stat-label">品类导航</span>
          </div>
        </div>
      </div>

      <!-- 展示舞台 -->
      <div class="hero-stage">
        <div v-if="loadError" class="stage-empty">
          <el-icon class="stage-empty-icon"><WarningFilled /></el-icon>
          <p>橱窗数据加载失败</p>
          <button class="btn btn--sm btn--ghost" type="button" @click="loadHomeShowcase">重新加载</button>
        </div>

        <template v-else-if="currentSlide">
          <div
            class="stage-frame"
            role="button"
            tabindex="0"
            @click="handleBannerClick(currentSlide)"
            @keydown.enter="handleBannerClick(currentSlide)"
          >
            <img :key="currentSlide.id" :src="currentSlide.image" :alt="currentSlide.title" class="stage-img" />
            <span class="stage-scanline" aria-hidden="true"></span>
            <div class="stage-overlay">
              <span class="stage-tag mono">FEATURED / {{ String(activeSlide + 1).padStart(2, '0') }}</span>
              <h2 class="stage-title">{{ currentSlide.title }}</h2>
              <p class="stage-sub">{{ currentSlide.subtitle }}</p>
              <span class="stage-cta">
                立即查看
                <el-icon><Right /></el-icon>
              </span>
            </div>
          </div>

          <div class="stage-dots" v-if="carousels.length > 1">
            <button
              v-for="(slide, idx) in carousels"
              :key="slide.id"
              class="stage-dot"
              :class="{ 'is-active': idx === activeSlide }"
              type="button"
              :aria-label="`切换到 ${slide.title}`"
              @click="activeSlide = idx"
            >
              <span class="dot-line"></span>
              <span class="dot-label mono">{{ String(idx + 1).padStart(2, '0') }}</span>
            </button>
          </div>
        </template>

        <div v-else class="stage-empty is-loading">
          <span class="stage-loader"></span>
          <p>正在加载商城橱窗…</p>
        </div>
      </div>
    </section>

    <!-- ================= 跑马灯 ================= -->
    <div class="marquee" aria-hidden="true">
      <div class="marquee-track">
        <span v-for="n in 2" :key="n" class="marquee-group">
          <template v-for="item in marqueeItems" :key="item.title">
            <span class="marquee-item">
              <span class="mi-title">{{ item.title }}</span>
              <span class="mi-en mono">{{ item.en }}</span>
            </span>
            <i class="mi-sep"></i>
          </template>
        </span>
      </div>
    </div>

    <!-- ================= 热销 ================= -->
    <section id="hot" class="section page-container" v-if="hotProducts.length > 0">
      <div class="section-head">
        <div>
          <span class="eyebrow">BEST SELLERS</span>
          <h2 class="section-title" style="margin-top: 10px">热销爆款甄选</h2>
          <p class="section-desc">人气口碑与真实成交共同筛出的头部单品</p>
        </div>
        <router-link to="/products" class="btn btn--ghost btn--sm">
          全部商品
          <el-icon><Right /></el-icon>
        </router-link>
      </div>

      <div class="hot-layout">
        <!-- 主推大卡 -->
        <router-link
          :to="`/product/${hotProducts[0].id}`"
          class="feature-card"
          v-reveal
        >
          <el-image :src="hotProducts[0].mainImage" fit="cover" class="feature-img">
            <template #error>
              <div class="feature-img-fallback"><el-icon><Picture /></el-icon></div>
            </template>
          </el-image>
          <div class="feature-shade"></div>
          <span class="feature-badge mono">TOP 01</span>
          <div class="feature-info">
            <span class="feature-brand">{{ hotProducts[0].brandName }}</span>
            <h3 class="feature-name">{{ hotProducts[0].name }}</h3>
            <p class="feature-sub">{{ hotProducts[0].subtitle || '编辑部年度推荐单品' }}</p>
            <div class="feature-bottom">
              <span class="price">
                <span class="symbol">¥</span>
                <span class="amount">{{ Number(hotProducts[0].price).toFixed(2) }}</span>
              </span>
              <span class="feature-arrow"><el-icon><Right /></el-icon></span>
            </div>
          </div>
        </router-link>

        <!-- 次级商品 -->
        <div class="hot-grid">
          <ProductCard
            v-for="(product, idx) in hotProducts.slice(1, 5)"
            :key="product.id"
            :product="product"
            :index="idx + 1"
            v-reveal="idx * 90"
          />
        </div>
      </div>
    </section>

    <!-- ================= 品牌墙 ================= -->
    <section class="section page-container" v-if="featuredBrands.length > 0">
      <div class="section-head">
        <div>
          <span class="eyebrow">OFFICIAL BRANDS</span>
          <h2 class="section-title" style="margin-top: 10px">官方入驻品牌</h2>
          <p class="section-desc">只与官方渠道合作，正品可溯源</p>
        </div>
        <router-link to="/products" class="btn btn--ghost btn--sm">
          按品牌选购
          <el-icon><Right /></el-icon>
        </router-link>
      </div>

      <div class="brand-wall">
        <router-link
          v-for="(brand, idx) in featuredBrands.slice(0, 6)"
          :key="brand.id"
          :to="{ path: '/products', query: { brandId: brand.id } }"
          class="brand-tile"
          v-reveal="idx * 55"
        >
          <div class="brand-logo">
            <el-image v-if="brand.logo" :src="brand.logo" fit="cover" class="brand-logo-img">
              <template #error>
                <span class="brand-fallback">{{ brand.name.slice(0, 2) }}</span>
              </template>
            </el-image>
            <span v-else class="brand-fallback">{{ brand.name.slice(0, 2) }}</span>
          </div>
          <div class="brand-meta">
            <div class="brand-title-row">
              <h4 class="brand-name">{{ brand.name }}</h4>
              <span v-if="brand.origin" class="brand-origin">{{ brand.origin }}</span>
            </div>
            <p class="brand-desc">{{ brand.description || '官方直营旗舰' }}</p>
          </div>
          <span class="brand-arrow"><el-icon><Right /></el-icon></span>
        </router-link>
      </div>
    </section>

    <!-- ================= 分类导航 ================= -->
    <section class="section page-container" v-if="categories.length > 0">
      <div class="section-head">
        <div>
          <span class="eyebrow">CATEGORY INDEX</span>
          <h2 class="section-title" style="margin-top: 10px">全品类索引</h2>
          <p class="section-desc">像翻阅目录一样，快速定位目标品类</p>
        </div>
      </div>

      <div class="cat-index">
        <div
          v-for="(cat, idx) in categories"
          :key="cat.id"
          class="cat-row"
          @click="$router.push({ path: '/products', query: { categoryId: cat.id } })"
        >
          <span class="cat-no mono">{{ String(idx + 1).padStart(2, '0') }}</span>
          <div class="cat-main">
            <h3 class="cat-name">{{ cat.name }}</h3>
            <div class="cat-subs" v-if="cat.children && cat.children.length > 0">
              <button
                v-for="sub in cat.children"
                :key="sub.id"
                type="button"
                class="cat-sub"
                @click.stop="$router.push({ path: '/products', query: { categoryId: sub.id } })"
              >
                {{ sub.name }}
              </button>
            </div>
            <p v-else class="cat-sub is-empty">暂无子分类</p>
          </div>
          <span class="cat-count mono">{{ cat.children?.length || 0 }} SUB</span>
          <span class="cat-arrow"><el-icon><Right /></el-icon></span>
        </div>
      </div>
    </section>

    <!-- ================= 新品 ================= -->
    <section class="section page-container" v-if="newProducts.length > 0">
      <div class="section-head">
        <div>
          <span class="eyebrow">NEW ARRIVALS</span>
          <h2 class="section-title" style="margin-top: 10px">新品首发上架</h2>
          <p class="section-desc">第一时间抢鲜体验的最新货品</p>
        </div>
        <router-link :to="{ path: '/products', query: { sortBy: 'newest' } }" class="btn btn--ghost btn--sm">
          浏览最新
          <el-icon><Right /></el-icon>
        </router-link>
      </div>

      <div class="new-rail">
        <div
          v-for="(product, idx) in newProducts"
          :key="product.id"
          class="new-item"
          v-reveal="idx * 70"
        >
          <ProductCard :product="product" :index="idx" />
        </div>
      </div>
    </section>

    <!-- ================= 收尾 CTA ================= -->
    <section class="page-container">
      <div class="closing-band" v-reveal>
        <div class="closing-copy">
          <span class="eyebrow">READY TO SHOP</span>
          <h2 class="display-lg" style="margin-top: 12px">
            挑一件喜欢的，<br />剩下的交给我们。
          </h2>
        </div>
        <div class="closing-actions">
          <button class="btn btn--primary" type="button" @click="$router.push('/products')">
            进入商品中心
            <el-icon><Right /></el-icon>
          </button>
          <button
            v-if="!userStore.isLoggedIn"
            class="btn btn--ghost"
            type="button"
            @click="$router.push('/login')"
          >
            登录账号
          </button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Right, Picture, WarningFilled } from '@element-plus/icons-vue'
import {
  getHomeShowcaseApi,
  type CarouselSlide,
  type ProductCard as ProductCardModel
} from '@/api/product'
import type { CategoryNode } from '@/api/category'
import type { Brand } from '@/api/brand'
import { useUserStore } from '@/stores/user'
import ProductCard from '@/components/ProductCard.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref(false)

const carousels = ref<CarouselSlide[]>([])
const categories = ref<CategoryNode[]>([])
const featuredBrands = ref<Brand[]>([])
const hotProducts = ref<ProductCardModel[]>([])
const newProducts = ref<ProductCardModel[]>([])

const activeSlide = ref(0)
let slideTimer: number | undefined

const currentSlide = computed(() => carousels.value[activeSlide.value] ?? null)
/** 热销与新品会有重叠，按商品 id 去重后统计在售数量 */
const totalProducts = computed(() => {
  const ids = new Set<number>()
  for (const item of [...hotProducts.value, ...newProducts.value]) ids.add(item.id)
  return ids.size
})

const marqueeItems = [
  { title: '官方正品', en: 'Authentic' },
  { title: '顺丰包邮', en: 'Free Shipping' },
  { title: '7 天无理由', en: 'Easy Return' },
  { title: '极速发货', en: 'Fast Dispatch' },
  { title: '会员专享价', en: 'Member Price' },
  { title: '官方售后', en: 'Official Support' }
]

function scrollToSection(id: string) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function startSlideTimer() {
  stopSlideTimer()
  if (carousels.value.length > 1) {
    slideTimer = window.setInterval(() => {
      activeSlide.value = (activeSlide.value + 1) % carousels.value.length
    }, 5200)
  }
}

function stopSlideTimer() {
  if (slideTimer) {
    window.clearInterval(slideTimer)
    slideTimer = undefined
  }
}

async function loadHomeShowcase() {
  loading.value = true
  loadError.value = false
  try {
    const data = await getHomeShowcaseApi()
    if (data) {
      carousels.value = data.carousels || []
      categories.value = data.categories || []
      featuredBrands.value = data.featuredBrands || []
      hotProducts.value = data.hotProducts || []
      newProducts.value = data.newProducts || []
      activeSlide.value = 0
      startSlideTimer()
    }
  } catch (err) {
    console.error('加载首页聚合橱窗失败', err)
    loadError.value = true
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

watch(
  () => carousels.value.length,
  () => startSlideTimer()
)

onMounted(() => {
  loadHomeShowcase()
})

onUnmounted(() => {
  stopSlideTimer()
})
</script>

<style scoped>
.home {
  padding-top: 40px;
}

/* ================= HERO ================= */
.hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.02fr);
  gap: clamp(32px, 5vw, 72px);
  align-items: center;
  min-height: min(74vh, 660px);
  padding-top: 28px;
  padding-bottom: 64px;
}

.hero-copy {
  min-width: 0;
  animation: hero-in 0.9s var(--ease) both;
}

@keyframes hero-in {
  from {
    opacity: 0;
    transform: translateY(28px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.hero-title {
  margin-top: 22px;
}

.hero-desc {
  margin-top: 22px;
  max-width: 420px;
  font-size: 15px;
  line-height: 1.85;
  color: var(--text-2);
}

.hero-actions {
  display: flex;
  gap: 14px;
  margin-top: 34px;
  flex-wrap: wrap;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 52px;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--text);
}

.stat-label {
  font-size: 11px;
  letter-spacing: 0.12em;
  color: var(--text-4);
}

.stat-divider {
  width: 1px;
  height: 30px;
  background: var(--line);
}

/* 舞台 */
.hero-stage {
  position: relative;
  display: flex;
  gap: 18px;
  align-items: stretch;
  min-width: 0;
  animation: stage-in 1s var(--ease) 0.1s both;
}

@keyframes stage-in {
  from {
    opacity: 0;
    transform: translateY(36px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

.stage-frame {
  position: relative;
  flex: 1;
  min-width: 0;
  min-height: 480px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: #0b0e18;
  cursor: pointer;
  box-shadow: 0 50px 110px -50px rgba(0, 0, 0, 1), 0 0 0 1px rgba(124, 92, 255, 0.12);
  outline: none;
}

.stage-frame::before {
  content: '';
  position: absolute;
  inset: -1px;
  border-radius: inherit;
  padding: 1px;
  background: linear-gradient(150deg, rgba(124, 92, 255, 0.7), transparent 40%, rgba(34, 211, 238, 0.55));
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
  z-index: 3;
}

.stage-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  animation: stage-fade 0.9s var(--ease);
  transition: transform 1.2s var(--ease);
}

.stage-frame:hover .stage-img {
  transform: scale(1.04);
}

@keyframes stage-fade {
  from {
    opacity: 0;
    transform: scale(1.05);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.stage-scanline {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(180deg, transparent 0%, rgba(34, 211, 238, 0.06) 50%, transparent 100%);
  background-size: 100% 6px;
  opacity: 0.5;
  pointer-events: none;
}

.stage-overlay {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 36px;
  background: linear-gradient(180deg, rgba(5, 6, 10, 0.1) 0%, rgba(5, 6, 10, 0.35) 55%, rgba(5, 6, 10, 0.92) 100%);
}

.stage-tag {
  font-size: 10px;
  letter-spacing: 0.22em;
  color: var(--cyan);
}

.stage-title {
  margin-top: 10px;
  font-size: clamp(24px, 2.6vw, 34px);
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.02em;
  text-wrap: balance;
  overflow-wrap: anywhere;
}

.stage-sub {
  margin-top: 8px;
  max-width: 88%;
  font-size: 13.5px;
  color: var(--text-2);
  line-height: 1.7;
}

.stage-cta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.stage-cta .el-icon {
  transition: transform 0.35s var(--ease);
}

.stage-frame:hover .stage-cta .el-icon {
  transform: translateX(5px);
}

/* 轮播点 */
.stage-dots {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  width: 34px;
}

.stage-dot {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  color: var(--text-4);
}

.dot-line {
  width: 2px;
  height: 26px;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.16);
  transition: all 0.4s var(--ease);
}

.dot-label {
  font-size: 10px;
  letter-spacing: 0.1em;
  opacity: 0;
  transition: opacity 0.3s var(--ease);
}

.stage-dot:hover .dot-label {
  opacity: 0.7;
}

.stage-dot.is-active .dot-line {
  height: 44px;
  background: var(--grad);
  box-shadow: 0 0 14px rgba(124, 92, 255, 0.9);
}

.stage-dot.is-active .dot-label {
  opacity: 1;
  color: var(--cyan);
}

/* 占位 */
.stage-empty {
  flex: 1;
  min-height: 480px;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 14px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-xl);
  color: var(--text-3);
  font-size: 13px;
  background: var(--surface);
  text-align: center;
}

.stage-empty-icon {
  font-size: 36px;
  color: var(--amber);
}

.stage-loader {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: 2px solid var(--line);
  border-top-color: var(--violet);
  animation: spin-slow 0.9s linear infinite;
}

/* ================= 跑马灯 ================= */
.marquee {
  position: relative;
  margin: 12px 0;
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
  background: linear-gradient(90deg, rgba(124, 92, 255, 0.07), rgba(34, 211, 238, 0.05));
  overflow: hidden;
  mask-image: linear-gradient(90deg, transparent 0, #000 6%, #000 94%, transparent 100%);
  -webkit-mask-image: linear-gradient(90deg, transparent 0, #000 6%, #000 94%, transparent 100%);
}

.marquee-track {
  display: flex;
  width: max-content;
  animation: marquee 46s linear infinite;
}

.marquee:hover .marquee-track {
  animation-play-state: paused;
}

.marquee-group {
  display: flex;
  align-items: center;
  gap: 34px;
  padding: 15px 34px 15px 0;
}

.marquee-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
  white-space: nowrap;
}

.mi-title {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--text-2);
}

.mi-en {
  font-size: 8.5px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--text-4);
}

.mi-sep {
  width: 5px;
  height: 5px;
  flex-shrink: 0;
  transform: rotate(45deg);
  background: linear-gradient(135deg, var(--violet), var(--cyan));
  opacity: 0.7;
  box-shadow: 0 0 10px rgba(124, 92, 255, 0.7);
}

@keyframes marquee {
  from {
    transform: translateX(0);
  }
  to {
    transform: translateX(-50%);
  }
}

/* ================= 热销 ================= */
.hot-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.5fr);
  gap: 24px;
  align-items: stretch;
}

.feature-card {
  position: relative;
  display: block;
  min-height: 520px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  border: 1px solid var(--line);
  box-shadow: var(--shadow-card);
  transition: border-color 0.4s var(--ease), transform 0.5s var(--ease);
}

.feature-card:hover {
  transform: translateY(-6px);
  border-color: rgba(124, 92, 255, 0.45);
}

.feature-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  transition: transform 1.1s var(--ease);
}

.feature-card:hover .feature-img {
  transform: scale(1.05);
}

.feature-img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-4);
  font-size: 32px;
  background: radial-gradient(circle at 50% 40%, #151a2b, #0b0e18);
}

.feature-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(5, 6, 10, 0.15) 0%, rgba(5, 6, 10, 0.5) 55%, #05060a 100%);
}

.feature-badge {
  position: absolute;
  top: 20px;
  left: 20px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(5, 6, 10, 0.55);
  backdrop-filter: blur(10px);
  font-size: 10px;
  letter-spacing: 0.18em;
  color: #fff;
}

.feature-info {
  position: absolute;
  inset: auto 0 0 0;
  padding: 28px;
}

.feature-brand {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--cyan);
}

.feature-name {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.02em;
}

.feature-sub {
  margin-top: 8px;
  font-size: 13px;
  color: var(--text-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.feature-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
}

.feature-arrow {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--line-strong);
  color: var(--text);
  transition: all 0.35s var(--ease);
}

.feature-card:hover .feature-arrow {
  background: var(--grad);
  border-color: transparent;
  color: #07080d;
  transform: rotate(-45deg);
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 24px;
}

/* ================= 品牌墙 ================= */
.brand-wall {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.brand-tile {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: var(--surface);
  overflow: hidden;
  transition: all 0.4s var(--ease);
}

.brand-tile::before {
  content: '';
  position: absolute;
  inset: 0;
  background: var(--grad-soft);
  opacity: 0;
  transition: opacity 0.4s var(--ease);
}

.brand-tile:hover {
  border-color: rgba(124, 92, 255, 0.4);
  transform: translateY(-4px);
}

.brand-tile:hover::before {
  opacity: 1;
}

.brand-logo {
  position: relative;
  z-index: 1;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid var(--line);
  overflow: hidden;
}

.brand-logo-img {
  width: 100%;
  height: 100%;
}

.brand-fallback {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  color: #c4b8ff;
}

.brand-meta {
  position: relative;
  z-index: 1;
  min-width: 0;
}

.brand-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.brand-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  flex-shrink: 0;
}

.brand-origin {
  font-size: 10.5px;
  color: var(--text-3);
  padding: 1px 6px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.03);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-desc {
  margin-top: 3px;
  font-size: 11.5px;
  color: var(--text-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.brand-arrow {
  position: relative;
  z-index: 1;
  margin-left: auto;
  font-size: 12px;
  color: var(--text-4);
  transition: all 0.35s var(--ease);
}

.brand-tile:hover .brand-arrow {
  color: var(--cyan);
  transform: translateX(4px);
}

/* ================= 分类索引 ================= */
.cat-index {
  border-top: 1px solid var(--line);
}

.cat-row {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 26px 8px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: background 0.35s var(--ease), padding 0.35s var(--ease);
}

.cat-row:hover {
  background: linear-gradient(90deg, rgba(124, 92, 255, 0.1), transparent 65%);
  padding-left: 20px;
}

.cat-no {
  font-size: 12px;
  color: var(--text-4);
  flex-shrink: 0;
}

.cat-main {
  flex: 1;
  min-width: 0;
}

.cat-name {
  font-size: 21px;
  font-weight: 600;
  letter-spacing: -0.01em;
  transition: color 0.3s var(--ease);
}

.cat-row:hover .cat-name {
  color: var(--cyan);
}

.cat-subs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.cat-sub {
  padding: 4px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
  color: var(--text-3);
  font-family: var(--font-body);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.25s var(--ease);
}

.cat-sub:hover {
  border-color: rgba(34, 211, 238, 0.5);
  color: var(--cyan);
  background: rgba(34, 211, 238, 0.08);
}

.cat-sub.is-empty {
  border: none;
  background: none;
  color: var(--text-4);
  cursor: default;
  padding-left: 0;
}

.cat-count {
  font-size: 10px;
  letter-spacing: 0.16em;
  color: var(--text-4);
  flex-shrink: 0;
}

.cat-arrow {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  border: 1px solid var(--line);
  color: var(--text-3);
  flex-shrink: 0;
  transition: all 0.35s var(--ease);
}

.cat-row:hover .cat-arrow {
  border-color: transparent;
  background: var(--grad);
  color: #07080d;
  transform: rotate(-45deg);
}

/* ================= 新品横滑 ================= */
.new-rail {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(280px, 1fr);
  gap: 24px;
  overflow-x: auto;
  padding-bottom: 12px;
  scroll-snap-type: x mandatory;
}

.new-item {
  scroll-snap-align: start;
}

/* ================= 收尾 ================= */
.closing-band {
  position: relative;
  margin-top: 40px;
  padding: clamp(36px, 5vw, 64px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(124, 92, 255, 0.28);
  background: radial-gradient(900px 400px at 15% 0%, rgba(124, 92, 255, 0.22), transparent 65%),
    radial-gradient(700px 380px at 90% 100%, rgba(34, 211, 238, 0.14), transparent 65%),
    rgba(255, 255, 255, 0.025);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  flex-wrap: wrap;
  overflow: hidden;
}

.closing-band::after {
  content: '';
  position: absolute;
  right: -80px;
  top: -80px;
  width: 280px;
  height: 280px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 0 0 40px rgba(255, 255, 255, 0.02), 0 0 0 80px rgba(255, 255, 255, 0.015);
  pointer-events: none;
}

.closing-actions {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  position: relative;
  z-index: 1;
}

/* ================= 响应式 ================= */
@media (max-width: 1080px) {
  .hero {
    grid-template-columns: 1fr;
    min-height: unset;
    gap: 40px;
  }

  .stage-frame {
    min-height: 420px;
  }

  .hot-layout {
    grid-template-columns: 1fr;
  }

  .feature-card {
    min-height: 420px;
  }

  .brand-wall {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .home {
    padding-top: 20px;
  }

  .hero {
    padding-bottom: 40px;
  }

  .hero-stats {
    margin-top: 36px;
    gap: 18px;
  }

  .stat-value {
    font-size: 21px;
  }

  .stage-frame {
    min-height: 360px;
  }

  .stage-overlay {
    padding: 24px;
  }

  .stage-dots {
    display: none;
  }

  .hot-grid {
    grid-template-columns: 1fr;
  }

  .brand-wall {
    grid-template-columns: 1fr;
  }

  .cat-row {
    gap: 16px;
    padding: 20px 4px;
  }

  .cat-name {
    font-size: 17px;
  }

  .cat-count {
    display: none;
  }

  .closing-band {
    padding: 32px 24px;
  }
}
</style>
