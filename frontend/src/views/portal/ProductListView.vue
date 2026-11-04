<template>
  <div class="product-list-container">
    <!-- Breadcrumb & Top Bar -->
    <div class="list-breadcrumb">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item>商品中心</el-breadcrumb-item>
        <el-breadcrumb-item v-if="selectedCategoryName">{{ selectedCategoryName }}</el-breadcrumb-item>
        <el-breadcrumb-item v-if="selectedBrandName">{{ selectedBrandName }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="list-layout">
      <!-- Left Filter Sidebar -->
      <aside class="filter-sidebar">
        <div class="filter-card">
          <div class="filter-card-header">
            <span class="filter-title">多维商品筛选</span>
            <el-button link type="primary" size="small" @click="resetFilters">重置全部</el-button>
          </div>

          <!-- Keyword Search -->
          <div class="filter-group">
            <label class="filter-label">关键字检索</label>
            <el-input
              v-model="queryParams.keyword"
              placeholder="搜索商品名称、型号..."
              clearable
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            />
          </div>

          <!-- Category Filter -->
          <div class="filter-group">
            <label class="filter-label">所属分类</label>
            <el-select
              v-model="queryParams.categoryId"
              placeholder="全部分类"
              clearable
              class="w-full"
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

          <!-- Brand Filter -->
          <div class="filter-group">
            <label class="filter-label">品牌筛选</label>
            <el-select
              v-model="queryParams.brandId"
              placeholder="全部品牌"
              clearable
              filterable
              class="w-full"
              @change="handleSearch"
            >
              <el-option label="全部品牌" :value="undefined" />
              <el-option
                v-for="brand in brandList"
                :key="brand.id"
                :label="brand.name"
                :value="brand.id"
              />
            </el-select>
          </div>

          <!-- Price Range Filter -->
          <div class="filter-group">
            <label class="filter-label">价格区间 (¥)</label>
            <div class="price-range-inputs">
              <el-input-number
                v-model="queryParams.minPrice"
                :min="0"
                :controls="false"
                placeholder="最低价"
                class="price-input"
              />
              <span class="range-divider">-</span>
              <el-input-number
                v-model="queryParams.maxPrice"
                :min="0"
                :controls="false"
                placeholder="最高价"
                class="price-input"
              />
            </div>
            <el-button
              type="primary"
              plain
              size="small"
              class="apply-price-btn"
              @click="handleSearch"
            >
              应用价格筛选
            </el-button>
          </div>
        </div>
      </aside>

      <!-- Right Main Content Area -->
      <main class="list-main">
        <!-- Sort and Active Filters Header -->
        <div class="main-toolbar">
          <div class="toolbar-left">
            <span class="result-count">
              共找到 <strong>{{ total }}</strong> 件商品
            </span>
            <div class="active-tags">
              <el-tag
                v-if="queryParams.keyword"
                closable
                size="small"
                @close="clearKeyword"
              >
                关键字: {{ queryParams.keyword }}
              </el-tag>
              <el-tag
                v-if="selectedCategoryName"
                closable
                size="small"
                type="success"
                @close="clearCategory"
              >
                分类: {{ selectedCategoryName }}
              </el-tag>
              <el-tag
                v-if="selectedBrandName"
                closable
                size="small"
                type="warning"
                @close="clearBrand"
              >
                品牌: {{ selectedBrandName }}
              </el-tag>
              <el-tag
                v-if="queryParams.minPrice || queryParams.maxPrice"
                closable
                size="small"
                type="info"
                @close="clearPrice"
              >
                价格: ¥{{ queryParams.minPrice || 0 }} - ¥{{ queryParams.maxPrice || '∞' }}
              </el-tag>
            </div>
          </div>

          <div class="toolbar-right">
            <span class="sort-label">排序方式：</span>
            <el-radio-group v-model="queryParams.sortBy" size="default" @change="handleSearch">
              <el-radio-button label="">综合</el-radio-button>
              <el-radio-button label="newest">新品优先</el-radio-button>
              <el-radio-button label="price_asc">价格从低到高</el-radio-button>
              <el-radio-button label="price_desc">价格从高到低</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- Product Cards Grid -->
        <div class="product-cards-container" v-loading="loading">
          <template v-if="productList.length > 0">
            <div class="products-grid">
              <div
                v-for="item in productList"
                :key="item.id"
                class="catalog-product-card"
                @click="goToDetail(item.id)"
              >
                <div class="card-thumb-wrap">
                  <el-image
                    :src="item.mainImage"
                    fit="cover"
                    class="card-img"
                    loading="lazy"
                  >
                    <template #error>
                      <div class="img-placeholder">暂无封面</div>
                    </template>
                  </el-image>
                  <span class="card-brand-badge">{{ item.brandName }}</span>
                </div>

                <div class="card-details">
                  <div class="card-cat-badge">
                    <el-tag size="small" type="info" effect="plain">{{ item.categoryName }}</el-tag>
                    <span class="stock-pill" :class="{ 'danger-stock': item.stock < 10 }">
                      库存: {{ item.stock }}
                    </span>
                  </div>

                  <h3 class="card-name" :title="item.name">{{ item.name }}</h3>
                  <p class="card-sub" :title="item.subtitle">{{ item.subtitle || '官方现货正品 · 闪电发货' }}</p>

                  <div class="card-footer">
                    <div class="price-container">
                      <span class="rmb">¥</span>
                      <span class="main-price">{{ Number(item.price).toFixed(2) }}</span>
                      <span
                        v-if="item.originalPrice && item.originalPrice > item.price"
                        class="strike-price"
                      >
                        ¥{{ Number(item.originalPrice).toFixed(2) }}
                      </span>
                    </div>

                    <el-button
                      type="primary"
                      size="small"
                      round
                      @click.stop="goToDetail(item.id)"
                    >
                      查看详情
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- Pagination Bar -->
            <div class="pagination-bar">
              <el-pagination
                background
                layout="total, prev, pager, next, jumper"
                :current-page="queryParams.pageNum"
                :page-size="queryParams.pageSize"
                :total="total"
                @current-change="handlePageChange"
              />
            </div>
          </template>

          <!-- Empty State -->
          <div v-else-if="!loading" class="empty-wrap">
            <el-empty description="未找到符合筛选条件的商品">
              <el-button type="primary" @click="resetFilters">清空筛选条件</el-button>
            </el-empty>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { searchProductsApi, type ProductCard, type ProductQuery } from '@/api/product'
import { getPortalCategoryTreeApi, type CategoryNode } from '@/api/category'
import { getFeaturedBrandsApi, type Brand } from '@/api/brand'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const productList = ref<ProductCard[]>([])
const brandList = ref<Brand[]>([])
const categoryTree = ref<CategoryNode[]>([])

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

// Flatten category tree for select dropdown with depth indentation
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
  const target = flatCategories.value.find(c => c.id === queryParams.categoryId)
  return target ? target.name : ''
})

const selectedBrandName = computed(() => {
  if (!queryParams.brandId) return ''
  const target = brandList.value.find(b => b.id === queryParams.brandId)
  return target ? target.name : ''
})

async function loadFilterMetadata() {
  try {
    const [cats, brands] = await Promise.all([
      getPortalCategoryTreeApi(),
      getFeaturedBrandsApi(50)
    ])
    categoryTree.value = cats || []
    brandList.value = brands || []
  } catch (err) {
    console.error('加载筛选元数据失败', err)
  }
}

async function fetchProducts() {
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
    if (res) {
      productList.value = res.records || []
      total.value = res.total || 0
    }
  } catch (err) {
    console.error('商品列表加载失败', err)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.pageNum = 1
  fetchProducts()
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
  queryParams.pageNum = 1
  fetchProducts()
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

function goToDetail(id: number) {
  router.push(`/product/${id}`)
}

// Sync route queries
function initFromRoute() {
  if (route.query.keyword) queryParams.keyword = String(route.query.keyword)
  if (route.query.categoryId) queryParams.categoryId = Number(route.query.categoryId)
  if (route.query.brandId) queryParams.brandId = Number(route.query.brandId)
  if (route.query.sortBy) queryParams.sortBy = String(route.query.sortBy)
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
.product-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 16px;
}

.list-breadcrumb {
  margin-bottom: 20px;
  padding: 4px 0;
}

.list-layout {
  display: flex;
  gap: 24px;
}

/* Sidebar */
.filter-sidebar {
  width: 270px;
  flex-shrink: 0;
}

.filter-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.filter-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.filter-title {
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.filter-group {
  margin-bottom: 20px;
}

.filter-label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.w-full {
  width: 100%;
}

.price-range-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.price-input {
  flex: 1;
}

.range-divider {
  color: #94a3b8;
}

.apply-price-btn {
  width: 100%;
}

/* Main Area */
.list-main {
  flex: 1;
  min-width: 0;
}

.main-toolbar {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.result-count {
  font-size: 14px;
  color: #64748b;
}

.result-count strong {
  color: #1e293b;
  font-size: 16px;
}

.active-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sort-label {
  font-size: 13px;
  color: #64748b;
}

/* Products Grid */
.products-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.catalog-product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
  transition: all 0.25s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.catalog-product-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08);
  border-color: #cbd5e1;
}

.card-thumb-wrap {
  position: relative;
  width: 100%;
  height: 240px;
  background: #f8fafc;
  overflow: hidden;
}

.card-img {
  width: 100%;
  height: 100%;
  transition: transform 0.3s ease;
}

.catalog-product-card:hover .card-img {
  transform: scale(1.04);
}

.img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 13px;
}

.card-brand-badge {
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

.card-details {
  padding: 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.card-cat-badge {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.stock-pill {
  font-size: 12px;
  color: #64748b;
}

.danger-stock {
  color: #ef4444;
  font-weight: 600;
}

.card-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-sub {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.price-container {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.rmb {
  font-size: 14px;
  font-weight: 700;
  color: #ef4444;
}

.main-price {
  font-size: 20px;
  font-weight: 800;
  color: #ef4444;
}

.strike-price {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
  margin-left: 4px;
}

.pagination-bar {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

.empty-wrap {
  padding: 60px 0;
  background: #fff;
  border-radius: 12px;
}
</style>
