<template>
  <div class="catalog">
    <!-- 页头 -->
    <header class="catalog-head page-container">
      <div>
        <span class="eyebrow">PRODUCT CATALOG</span>
        <h1 class="display-lg catalog-title">商品中心</h1>
        <p class="catalog-sub">
          共 <span class="mono catalog-count">{{ total }}</span> 件商品
          <template v-if="selectedCategoryName"> · {{ selectedCategoryName }}</template>
          <template v-if="selectedBrandName"> · {{ selectedBrandName }}</template>
        </p>
      </div>
      <div class="head-actions">
        <button class="btn btn--ghost btn--sm filter-toggle" type="button" @click="filtersOpen = !filtersOpen">
          <el-icon><Operation /></el-icon>
          {{ filtersOpen ? '收起筛选' : '展开筛选' }}
        </button>
        <button class="btn btn--ghost btn--sm" type="button" @click="resetFilters">
          重置条件
        </button>
      </div>
    </header>

    <div class="catalog-body page-container">
      <!-- 筛选侧栏 -->
      <aside class="filter-rail" :class="{ 'is-open': filtersOpen }">
        <div class="filter-panel">
          <div class="filter-head">
            <span class="mono filter-head-label">FILTERS</span>
            <span class="filter-head-hint">多维组合筛选</span>
          </div>

          <div class="filter-group">
            <label class="filter-label">关键字</label>
            <div class="search-field">
              <el-icon class="search-icon"><Search /></el-icon>
              <input
                v-model="queryParams.keyword"
                class="field-input"
                type="search"
                placeholder="搜索商品名称、型号"
                @keyup.enter="handleSearch"
              />
              <button v-if="queryParams.keyword" class="field-clear" type="button" @click="clearKeyword">
                <el-icon><Close /></el-icon>
              </button>
            </div>
          </div>

          <div class="filter-group">
            <label class="filter-label">所属分类</label>
            <el-select
              v-model="queryParams.categoryId"
              placeholder="全部分类"
              clearable
              class="w-full"
              popper-class="neon-select"
              @change="handleSearch"
            >
              <el-option label="全部分类" :value="undefined" />
              <template v-for="cat in flatCategories" :key="cat.id">
                <el-option
                  :label="cat.depth > 0 ? '　'.repeat(cat.depth) + '└ ' + cat.name : cat.name"
                  :value="cat.id"
                />
              </template>
            </el-select>
          </div>

          <div class="filter-group">
            <label class="filter-label">品牌</label>
            <el-select
              v-model="queryParams.brandId"
              placeholder="全部品牌"
              clearable
              filterable
              class="w-full"
              popper-class="neon-select"
              @change="handleSearch"
            >
              <el-option label="全部品牌" :value="undefined" />
              <el-option v-for="brand in brandList" :key="brand.id" :label="brand.name" :value="brand.id" />
            </el-select>
          </div>

          <div class="filter-group">
            <label class="filter-label">价格区间 (¥)</label>
            <div class="price-range">
              <el-input-number
                v-model="queryParams.minPrice"
                :min="0"
                :controls="false"
                placeholder="最低"
                class="price-input"
              />
              <span class="range-line"></span>
              <el-input-number
                v-model="queryParams.maxPrice"
                :min="0"
                :controls="false"
                placeholder="最高"
                class="price-input"
              />
            </div>
            <button class="btn btn--primary btn--sm apply-btn" type="button" @click="handleSearch">
              应用价格筛选
            </button>
          </div>
        </div>
      </aside>

      <!-- 商品区 -->
      <main class="catalog-main">
        <div class="toolbar">
          <div class="sort-pills">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              type="button"
              class="sort-pill"
              :class="{ 'is-active': queryParams.sortBy === option.value }"
              @click="changeSort(option.value)"
            >
              {{ option.label }}
            </button>
          </div>

          <div class="active-chips">
            <button v-if="queryParams.keyword" class="chip" type="button" @click="clearKeyword">
              {{ queryParams.keyword }}
              <el-icon><Close /></el-icon>
            </button>
            <button v-if="selectedCategoryName" class="chip" type="button" @click="clearCategory">
              {{ selectedCategoryName }}
              <el-icon><Close /></el-icon>
            </button>
            <button v-if="selectedBrandName" class="chip" type="button" @click="clearBrand">
              {{ selectedBrandName }}
              <el-icon><Close /></el-icon>
            </button>
            <button
              v-if="queryParams.minPrice != null || queryParams.maxPrice != null"
              class="chip"
              type="button"
              @click="clearPrice"
            >
              ¥{{ queryParams.minPrice ?? 0 }} - ¥{{ queryParams.maxPrice ?? '∞' }}
              <el-icon><Close /></el-icon>
            </button>
          </div>
        </div>

        <div v-loading="loading" class="grid-wrap">
          <div v-if="productList.length > 0" class="product-grid">
            <ProductCard
              v-for="(item, idx) in productList"
              :key="item.id"
              :product="item"
              :index="idx"
              v-reveal="(idx % 3) * 60"
            />
          </div>

          <div v-else-if="!loading" class="empty-state">
            <span class="empty-orb"></span>
            <h3 class="empty-title">没有符合条件的商品</h3>
            <p class="empty-desc">试着放宽价格区间，或清空当前筛选条件</p>
            <button class="btn btn--primary btn--sm" type="button" @click="resetFilters">
              清空筛选条件
            </button>
          </div>
        </div>

        <div v-if="total > 0" class="pagination-bar">
          <el-pagination
            background
            layout="prev, pager, next"
            :current-page="queryParams.pageNum"
            :page-size="queryParams.pageSize"
            :total="total"
            @current-change="handlePageChange"
          />
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, Close, Operation } from '@element-plus/icons-vue'
import { searchProductsApi, type ProductCard as ProductCardModel, type ProductQuery } from '@/api/product'
import { getPortalCategoryTreeApi, type CategoryNode } from '@/api/category'
import { getPortalBrandsApi, type Brand } from '@/api/brand'
import ProductCard from '@/components/ProductCard.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const productList = ref<ProductCardModel[]>([])
const brandList = ref<Brand[]>([])
const categoryTree = ref<CategoryNode[]>([])
const filtersOpen = ref(false)

const sortOptions = [
  { value: '', label: '综合排序' },
  { value: 'newest', label: '新品优先' },
  { value: 'price_asc', label: '价格 ↑' },
  { value: 'price_desc', label: '价格 ↓' }
]

const queryParams = reactive<ProductQuery>({
  keyword: '',
  categoryId: undefined,
  brandId: undefined,
  minPrice: undefined,
  maxPrice: undefined,
  sortBy: '',
  pageNum: 1,
  pageSize: 12
})

interface FlatCategory {
  id: number
  name: string
  depth: number
}

const flatCategories = computed(() => {
  const result: FlatCategory[] = []
  function traverse(nodes: CategoryNode[], depth = 0) {
    for (const node of nodes) {
      result.push({ id: node.id, name: node.name, depth })
      if (node.children && node.children.length > 0) {
        traverse(node.children, depth + 1)
      }
    }
  }
  traverse(categoryTree.value)
  return result
})

const selectedCategoryName = computed(() => {
  if (!queryParams.categoryId) return ''
  const target = flatCategories.value.find((c) => c.id === queryParams.categoryId)
  return target ? target.name : ''
})

const selectedBrandName = computed(() => {
  if (!queryParams.brandId) return ''
  const target = brandList.value.find((b) => b.id === queryParams.brandId)
  return target ? target.name : ''
})

async function loadFilterMetadata() {
  try {
    const [cats, brands] = await Promise.all([getPortalCategoryTreeApi(), getPortalBrandsApi()])
    categoryTree.value = cats || []
    brandList.value = brands || []
  } catch (err) {
    console.error('加载筛选元数据失败', err)
  }
}

// 快速切换筛选/排序时，只采纳最后一次请求的响应，避免旧结果覆盖新列表
let productRequestSeq = 0

async function fetchProducts() {
  const seq = ++productRequestSeq
  loading.value = true
  try {
    const res = await searchProductsApi({
      keyword: queryParams.keyword?.trim() || undefined,
      categoryId: queryParams.categoryId,
      brandId: queryParams.brandId,
      minPrice: queryParams.minPrice,
      maxPrice: queryParams.maxPrice,
      sortBy: queryParams.sortBy || undefined,
      pageNum: queryParams.pageNum,
      pageSize: queryParams.pageSize
    })
    if (seq !== productRequestSeq) return
    if (res) {
      productList.value = res.records || []
      total.value = res.total || 0
    }
  } catch (err) {
    console.error('商品列表加载失败', err)
  } finally {
    if (seq === productRequestSeq) {
      loading.value = false
    }
  }
}

function buildRouteQuery(): Record<string, string> {
  const query: Record<string, string> = {}
  if (queryParams.keyword?.trim()) query.keyword = queryParams.keyword.trim()
  if (queryParams.categoryId != null) query.categoryId = String(queryParams.categoryId)
  if (queryParams.brandId != null) query.brandId = String(queryParams.brandId)
  if (queryParams.minPrice != null) query.minPrice = String(queryParams.minPrice)
  if (queryParams.maxPrice != null) query.maxPrice = String(queryParams.maxPrice)
  if (queryParams.sortBy) query.sortBy = queryParams.sortBy
  return query
}

function getNormalizedCurrentRouteQuery(): Record<string, string> {
  const current: Record<string, string> = {}
  for (const key of ['keyword', 'categoryId', 'brandId', 'minPrice', 'maxPrice', 'sortBy']) {
    const val = route.query[key]
    if (val != null && String(val).trim() !== '') {
      current[key] = String(val).trim()
    }
  }
  return current
}

function applyFiltersAndFetch() {
  queryParams.pageNum = 1
  const nextQuery = buildRouteQuery()
  const currentQuery = getNormalizedCurrentRouteQuery()
  const nextKeys = Object.keys(nextQuery).sort()
  const currentKeys = Object.keys(currentQuery).sort()
  const isSame =
    nextKeys.length === currentKeys.length &&
    nextKeys.every((k, i) => k === currentKeys[i] && nextQuery[k] === currentQuery[k])

  if (!isSame) {
    router.replace({ path: '/products', query: nextQuery })
  } else {
    fetchProducts()
  }
}

function handleSearch() {
  applyFiltersAndFetch()
}

function changeSort(value: string) {
  queryParams.sortBy = value
  handleSearch()
}

function handlePageChange(page: number) {
  queryParams.pageNum = page
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function resetFilters() {
  queryParams.keyword = ''
  queryParams.categoryId = undefined
  queryParams.brandId = undefined
  queryParams.minPrice = undefined
  queryParams.maxPrice = undefined
  queryParams.sortBy = ''
  applyFiltersAndFetch()
}

function clearKeyword() {
  queryParams.keyword = ''
  handleSearch()
}

function clearCategory() {
  queryParams.categoryId = undefined
  handleSearch()
}

function clearBrand() {
  queryParams.brandId = undefined
  handleSearch()
}

function clearPrice() {
  queryParams.minPrice = undefined
  queryParams.maxPrice = undefined
  handleSearch()
}

function parseOptionalNumber(val: unknown): number | undefined {
  if (val == null || String(val).trim() === '') return undefined
  const num = Number(val)
  return Number.isNaN(num) ? undefined : num
}

function initFromRoute() {
  queryParams.keyword = route.query.keyword ? String(route.query.keyword) : ''
  queryParams.categoryId = parseOptionalNumber(route.query.categoryId)
  queryParams.brandId = parseOptionalNumber(route.query.brandId)
  queryParams.minPrice = parseOptionalNumber(route.query.minPrice)
  queryParams.maxPrice = parseOptionalNumber(route.query.maxPrice)
  queryParams.sortBy = route.query.sortBy ? String(route.query.sortBy) : ''
  queryParams.pageNum = 1
}

watch(
  () => route.query,
  () => {
    initFromRoute()
    fetchProducts()
  }
)

onMounted(async () => {
  initFromRoute()
  await loadFilterMetadata()
  fetchProducts()
})
</script>

<style scoped>
.catalog {
  padding-top: 28px;
  padding-bottom: 8px;
}

/* 页头 */
.catalog-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-top: 12px;
  padding-bottom: 36px;
  flex-wrap: wrap;
}

.catalog-title {
  margin-top: 12px;
}

.catalog-sub {
  margin-top: 10px;
  font-size: 13px;
  color: var(--text-3);
}

.catalog-count {
  color: var(--cyan);
  font-size: 15px;
  font-weight: 700;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.filter-toggle {
  display: none;
}

/* 主体 */
.catalog-body {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

/* 侧栏 */
.filter-rail {
  width: 268px;
  flex-shrink: 0;
  position: sticky;
  top: 96px;
}

.filter-panel {
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.025);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.filter-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 16px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--line);
}

.filter-head-label {
  font-size: 10px;
  letter-spacing: 0.24em;
  color: var(--cyan);
}

.filter-head-hint {
  font-size: 11px;
  color: var(--text-4);
}

.filter-group {
  margin-bottom: 22px;
}

.filter-group:last-child {
  margin-bottom: 4px;
}

.filter-label {
  display: block;
  margin-bottom: 9px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-2);
  letter-spacing: 0.02em;
}

.w-full {
  width: 100%;
}

/* 自定义搜索框 */
.search-field {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.045);
  transition: all 0.25s var(--ease);
}

.search-field:focus-within {
  border-color: rgba(124, 92, 255, 0.8);
  box-shadow: 0 0 0 3px rgba(124, 92, 255, 0.14);
}

.search-icon {
  color: var(--text-3);
  font-size: 14px;
}

.field-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text);
  font-family: var(--font-body);
  font-size: 13px;
}

.field-input::placeholder {
  color: var(--text-4);
}

.field-clear {
  display: grid;
  place-items: center;
  border: none;
  background: none;
  color: var(--text-4);
  cursor: pointer;
  font-size: 12px;
}

.field-clear:hover {
  color: var(--text);
}

/* 价格 */
.price-range {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.price-input {
  flex: 1;
  min-width: 0;
}

.price-input :deep(.el-input__wrapper) {
  padding: 0 10px;
}

.range-line {
  width: 10px;
  height: 1px;
  background: var(--line-strong);
  flex-shrink: 0;
}

.apply-btn {
  width: 100%;
}

/* 主区 */
.catalog-main {
  flex: 1;
  min-width: 0;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
  margin-bottom: 22px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line);
}

.sort-pills {
  display: flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
}

.sort-pill {
  padding: 7px 16px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--text-3);
  font-family: var(--font-body);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s var(--ease);
  white-space: nowrap;
}

.sort-pill:hover {
  color: var(--text);
}

.sort-pill.is-active {
  background: var(--grad);
  color: #07080d;
  font-weight: 600;
}

.active-chips {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(124, 92, 255, 0.35);
  border-radius: 999px;
  background: rgba(124, 92, 255, 0.12);
  color: #c9bfff;
  font-family: var(--font-body);
  font-size: 11.5px;
  cursor: pointer;
  transition: all 0.25s var(--ease);
}

.chip:hover {
  border-color: rgba(255, 92, 122, 0.5);
  color: #ff9db0;
  background: rgba(255, 92, 122, 0.1);
}

/* 商品网格 */
.grid-wrap {
  min-height: 320px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22px;
}

/* 空态 */
.empty-state {
  position: relative;
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 90px 24px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-xl);
  text-align: center;
  overflow: hidden;
}

.empty-orb {
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, rgba(124, 92, 255, 0.6), rgba(34, 211, 238, 0.12) 60%, transparent 72%);
  filter: blur(6px);
  margin-bottom: 6px;
}

.empty-title {
  font-size: 19px;
  font-weight: 600;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-3);
  margin-bottom: 8px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 44px;
}

/* 响应式 */
@media (max-width: 1080px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .filter-toggle {
    display: inline-flex;
  }

  .catalog-body {
    flex-direction: column;
  }

  .filter-rail {
    width: 100%;
    position: static;
    display: none;
  }

  .filter-rail.is-open {
    display: block;
  }
}

@media (max-width: 600px) {
  .product-grid {
    grid-template-columns: 1fr;
  }

  .sort-pills {
    width: 100%;
    overflow-x: auto;
  }
}
</style>
