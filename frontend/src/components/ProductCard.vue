<template>
  <router-link :to="`/product/${product.id}`" class="p-card surface-hover">
    <div class="p-media">
      <el-image :src="product.mainImage" fit="cover" class="p-img" loading="lazy">
        <template #error>
          <div class="p-img-fallback">
            <el-icon><Picture /></el-icon>
          </div>
        </template>
      </el-image>
      <span class="p-brand">{{ product.brandName }}</span>
      <span v-if="product.stock <= 0" class="p-stock is-out">SOLD OUT</span>
      <span v-else-if="product.stock < 10" class="p-stock">仅剩 {{ product.stock }}</span>
    </div>

    <div class="p-body">
      <div class="p-meta">
        <span class="p-cat">{{ product.categoryName }}</span>
        <span class="p-index mono">{{ indexLabel }}</span>
      </div>
      <h3 class="p-name" :title="product.name">{{ product.name }}</h3>
      <p class="p-sub" :title="product.subtitle">{{ product.subtitle || '官方正品 · 闪电发货' }}</p>

      <div class="p-foot">
        <span class="price">
          <span class="symbol">¥</span>
          <span class="amount">{{ Number(product.price).toFixed(2) }}</span>
          <span
            v-if="product.originalPrice && product.originalPrice > product.price"
            class="original"
          >
            ¥{{ Number(product.originalPrice).toFixed(2) }}
          </span>
        </span>
        <span class="p-arrow" aria-hidden="true">
          <el-icon><Right /></el-icon>
        </span>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Picture, Right } from '@element-plus/icons-vue'
import type { ProductCard as ProductCardModel } from '@/api/product'

const props = withDefaults(
  defineProps<{
    product: ProductCardModel
    index?: number
  }>(),
  { index: 0 }
)

const indexLabel = computed(() => String(props.index + 1).padStart(2, '0'))
</script>

<style scoped>
.p-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--surface);
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  transition: transform 0.5s var(--ease), border-color 0.4s var(--ease),
    box-shadow 0.5s var(--ease);
}

.p-card:hover {
  transform: translateY(-8px);
  border-color: rgba(124, 92, 255, 0.45);
  box-shadow: var(--shadow-card), 0 0 0 1px rgba(124, 92, 255, 0.2),
    0 36px 80px -44px rgba(124, 92, 255, 0.9);
}

/* 图片 */
.p-media {
  position: relative;
  aspect-ratio: 4 / 3.2;
  overflow: hidden;
  background: radial-gradient(circle at 50% 40%, #151a2b 0%, #0b0e18 100%);
}

.p-img {
  width: 100%;
  height: 100%;
  transition: transform 0.9s var(--ease), opacity 0.4s var(--ease);
}

.p-card:hover .p-img {
  transform: scale(1.07);
}

.p-img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-4);
  font-size: 28px;
}

.p-media::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(5, 6, 10, 0) 45%, rgba(5, 6, 10, 0.75) 100%);
  pointer-events: none;
}

.p-brand {
  position: absolute;
  top: 12px;
  left: 12px;
  z-index: 2;
  padding: 5px 11px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 999px;
  background: rgba(8, 10, 17, 0.62);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text);
}

.p-stock {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  padding: 4px 9px;
  border-radius: 999px;
  background: rgba(251, 191, 36, 0.16);
  border: 1px solid rgba(251, 191, 36, 0.35);
  color: #fcd77a;
  font-size: 10px;
  font-weight: 600;
}

.p-stock.is-out {
  background: rgba(255, 92, 122, 0.16);
  border-color: rgba(255, 92, 122, 0.4);
  color: #ff9db0;
}

/* 内容 */
.p-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 18px 18px 16px;
  gap: 4px;
}

.p-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
}

.p-cat {
  font-size: 11px;
  color: var(--text-3);
  letter-spacing: 0.04em;
}

.p-index {
  font-size: 10px;
  color: var(--text-4);
  letter-spacing: 0.1em;
}

.p-name {
  font-size: 15.5px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.3s var(--ease);
}

.p-card:hover .p-name {
  color: #fff;
}

.p-sub {
  font-size: 12px;
  color: var(--text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 14px;
}

.p-foot {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.p-arrow {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  border: 1px solid var(--line);
  color: var(--text-2);
  font-size: 13px;
  transition: all 0.35s var(--ease);
}

.p-card:hover .p-arrow {
  border-color: transparent;
  background: var(--grad);
  color: #07080d;
  transform: rotate(-45deg);
}
</style>
