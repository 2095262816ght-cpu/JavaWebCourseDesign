<template>
  <div class="dash">
    <!-- KPI -->
    <div class="kpi-grid">
      <article class="kpi surface-hover" v-reveal>
        <div class="kpi-top">
          <span class="kpi-label">今日销售额</span>
          <span class="kpi-tag mono">TODAY</span>
        </div>
        <p class="kpi-value mono">
          <template v-if="ordersError">—</template>
          <template v-else-if="todaySales !== null">
            <span class="kpi-symbol">¥</span>{{ formatMoney(todaySales) }}
          </template>
          <template v-else>加载中</template>
        </p>
        <p class="kpi-foot" :class="trendClass">{{ salesFooter }}</p>
        <span class="kpi-glow"></span>
      </article>

      <article class="kpi kpi-link surface-hover" v-reveal="60" @click="$router.push('/admin/orders')">
        <div class="kpi-top">
          <span class="kpi-label">订单总量</span>
          <span class="kpi-tag mono">ORDERS</span>
        </div>
        <p class="kpi-value mono">
          <template v-if="ordersError">—</template>
          <template v-else-if="orderCount !== null">{{ orderCount }}</template>
          <template v-else>加载中</template>
        </p>
        <p class="kpi-foot">点击前往订单管理 →</p>
        <span class="kpi-glow"></span>
      </article>

      <article class="kpi kpi-link surface-hover" v-reveal="120" @click="$router.push('/admin/products')">
        <div class="kpi-top">
          <span class="kpi-label">在售商品</span>
          <span class="kpi-tag mono">CATALOG</span>
        </div>
        <p class="kpi-value mono">
          <template v-if="productsError">—</template>
          <template v-else-if="onSaleCount !== null">{{ onSaleCount }}</template>
          <template v-else>加载中</template>
        </p>
        <p class="kpi-foot">{{ stockAlertFooter }}</p>
        <span class="kpi-glow"></span>
      </article>

      <article class="kpi kpi-link surface-hover" v-reveal="180" @click="$router.push('/admin/users')">
        <div class="kpi-top">
          <span class="kpi-label">注册用户</span>
          <span class="kpi-tag mono">USERS</span>
        </div>
        <p class="kpi-value mono">
          <template v-if="usersError">—</template>
          <template v-else-if="userCount !== null">{{ userCount }}</template>
          <template v-else>加载中</template>
        </p>
        <p class="kpi-foot">点击前往用户管理 →</p>
        <span class="kpi-glow"></span>
      </article>
    </div>

    <!-- 图表区 -->
    <div class="chart-row">
      <section class="panel chart-panel" v-reveal>
        <header class="panel-head">
          <div>
            <p class="panel-kicker mono">REVENUE / 7 DAYS</p>
            <h3 class="panel-title">近七日成交额</h3>
          </div>
          <button class="btn btn--ghost btn--sm" type="button" :disabled="loading" @click="handleManualRefresh">
            {{ loading ? '刷新中…' : '刷新数据' }}
          </button>
        </header>

        <div class="chart">
          <div class="chart-grid">
            <span v-for="n in 4" :key="n"></span>
          </div>
          <div class="bars">
            <div v-for="point in salesSeries" :key="point.key" class="bar-col">
              <span class="bar-value mono">{{ point.value > 0 ? formatMoney(point.value) : '—' }}</span>
              <div class="bar-track">
                <div
                  class="bar"
                  :class="{ 'is-empty': point.value === 0 }"
                  :style="{ height: `${point.height}%` }"
                ></div>
              </div>
              <span class="bar-label mono">{{ point.label }}</span>
            </div>
          </div>
        </div>

        <footer class="panel-foot">
          <span>近七日累计 <strong class="mono">¥ {{ formatMoney(weekTotal) }}</strong></span>
          <span class="mono">PAID / SHIPPED / COMPLETED</span>
        </footer>
      </section>

      <section class="panel status-panel" v-reveal="80">
        <header class="panel-head">
          <div>
            <p class="panel-kicker mono">ORDER STATUS</p>
            <h3 class="panel-title">订单状态分布</h3>
          </div>
        </header>

        <div class="status-list">
          <div v-for="row in statusRows" :key="row.key" class="status-row">
            <div class="status-row-top">
              <span class="status-name">
                <i class="status-dot" :class="row.class"></i>
                {{ row.label }}
              </span>
              <span class="status-num mono">{{ row.count }}</span>
            </div>
            <div class="status-track">
              <div class="status-fill" :class="row.class" :style="{ width: `${row.percent}%` }"></div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 库存 + 系统 -->
    <div class="bottom-row">
      <section class="panel" v-reveal>
        <header class="panel-head">
          <div>
            <p class="panel-kicker mono">STOCK ALERT</p>
            <h3 class="panel-title">库存预警</h3>
          </div>
          <router-link to="/admin/products" class="btn btn--ghost btn--sm">去补货</router-link>
        </header>

        <div v-if="lowStockList.length === 0" class="panel-empty">
          暂无低库存商品，库存状态健康
        </div>
        <ul v-else class="stock-list">
          <li v-for="item in lowStockList" :key="item.id" class="stock-item">
            <el-image :src="item.mainImage" class="stock-img" fit="cover" />
            <div class="stock-info">
              <p class="stock-name">{{ item.name }}</p>
              <p class="stock-meta mono">{{ item.brandName }} · ¥{{ Number(item.price).toFixed(2) }}</p>
            </div>
            <span class="stock-badge" :class="item.stock <= 0 ? 'is-out' : 'is-low'">
              {{ item.stock <= 0 ? '缺货' : `剩 ${item.stock}` }}
            </span>
          </li>
        </ul>
      </section>

      <section class="panel" v-reveal="80">
        <header class="panel-head">
          <div>
            <p class="panel-kicker mono">SYSTEM</p>
            <h3 class="panel-title">运行环境</h3>
          </div>
          <span class="system-live"><i></i> ONLINE</span>
        </header>

        <dl class="sys-list">
          <div class="sys-row">
            <dt>后端运行时</dt>
            <dd>Spring Boot 3.3.4 / Java 21</dd>
          </div>
          <div class="sys-row">
            <dt>鉴权引擎</dt>
            <dd>Sa-Token 1.39.0 · JWT</dd>
          </div>
          <div class="sys-row">
            <dt>持久层</dt>
            <dd>MyBatis-Plus 3.5.7</dd>
          </div>
          <div class="sys-row">
            <dt>关系数据库</dt>
            <dd>MySQL 8.4 · :3306</dd>
          </div>
          <div class="sys-row">
            <dt>缓存</dt>
            <dd>Redis 7.2 · :6379</dd>
          </div>
          <div class="sys-row">
            <dt>接口文档</dt>
            <dd>OpenAPI 3 / Swagger UI</dd>
          </div>
        </dl>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminUsersApi, type PageResult } from '@/api/user'
import { getAdminOrdersApi, type OrderSummary } from '@/api/order'
import { listAdminProductsApi, type ProductAdminVO } from '@/api/product'
import { getOrderStatusText } from '@/utils/order'

const loading = ref(false)
const usersError = ref(false)
const ordersError = ref(false)
const productsError = ref(false)
const userCount = ref<number | null>(null)
const orderCount = ref<number | null>(null)
const todaySales = ref<number | null>(null)
const yesterdaySales = ref<number>(0)
const todayPaidCount = ref<number>(0)
const totalPaidSales = ref<number>(0)

const onSaleCount = ref<number | null>(null)
const outOfStockCount = ref<number>(0)
const lowStockCount = ref<number>(0)

const allOrders = ref<OrderSummary[]>([])
const allProducts = ref<ProductAdminVO[]>([])

const paidStatuses = new Set(['PAID', 'SHIPPED', 'COMPLETED'])

const stockAlertFooter = computed(() => {
  if (productsError.value) return '商品库存统计获取失败，请刷新重试'
  if (onSaleCount.value === null) return '正在检测商品库存...'
  if (outOfStockCount.value > 0) return `缺货 ${outOfStockCount.value} 件 · 低库存 ${lowStockCount.value} 件`
  if (lowStockCount.value > 0) return `低库存预警 ${lowStockCount.value} 件（库存 ≤ 10）`
  return '库存状态健康'
})

const salesFooter = computed(() => {
  if (ordersError.value) return '订单销售额统计获取失败'
  if (todaySales.value === null) return '正在统计订单销售额...'
  if (yesterdaySales.value > 0) {
    const pct = ((todaySales.value - yesterdaySales.value) / yesterdaySales.value) * 100
    return `较昨日 ${pct >= 0 ? '+' : ''}${pct.toFixed(1)}%`
  }
  return `今日已支付 ${todayPaidCount.value} 笔 · 累计 ¥${totalPaidSales.value.toFixed(2)}`
})

const trendClass = computed(() => {
  if (todaySales.value === null || yesterdaySales.value <= 0) return ''
  return todaySales.value >= yesterdaySales.value ? 'is-up' : 'is-down'
})

interface SalesPoint {
  key: string
  label: string
  value: number
  height: number
}

const salesSeries = computed<SalesPoint[]>(() => {
  const days: { key: string; label: string; sum: number }[] = []
  const now = new Date()
  for (let i = 6; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(now.getDate() - i)
    const key = formatLocalDate(d)
    days.push({
      key,
      label: `${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`,
      sum: 0
    })
  }
  const map = new Map(days.map((d) => [d.key, d]))
  for (const order of allOrders.value) {
    if (!paidStatuses.has(order.status)) continue
    const bizDate = (order.paymentTime || order.createdAt || '').slice(0, 10)
    const target = map.get(bizDate)
    if (target) target.sum += Number(order.totalAmount || 0)
  }
  const max = Math.max(...days.map((d) => d.sum), 1)
  return days.map((d) => ({
    key: d.key,
    label: d.label,
    value: d.sum,
    height: d.sum === 0 ? 4 : Math.max(10, Math.round((d.sum / max) * 100))
  }))
})

const weekTotal = computed(() => salesSeries.value.reduce((sum, p) => sum + p.value, 0))

const statusRows = computed(() => {
  const statuses = [
    { key: 'UNPAID', class: 'is-warn' },
    { key: 'PAID', class: 'is-info' },
    { key: 'SHIPPED', class: 'is-violet' },
    { key: 'COMPLETED', class: 'is-ok' },
    { key: 'CANCELLED', class: 'is-muted' }
  ]
  const counts = new Map<string, number>()
  for (const order of allOrders.value) {
    counts.set(order.status, (counts.get(order.status) || 0) + 1)
  }
  const total = allOrders.value.length || 1
  return statuses.map((s) => {
    const count = counts.get(s.key) || 0
    return {
      ...s,
      label: getOrderStatusText(s.key),
      count,
      percent: Math.round((count / total) * 100)
    }
  })
})

const lowStockList = computed(() =>
  allProducts.value
    .filter((p) => p.status === 1 && (p.stock ?? 0) <= 10)
    .sort((a, b) => (a.stock ?? 0) - (b.stock ?? 0))
    .slice(0, 6)
)

function formatMoney(value: number) {
  return value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatLocalDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function fetchAllPages<T>(
  loader: (pageNum: number, pageSize: number) => Promise<PageResult<T>>
): Promise<{ total: number; records: T[] }> {
  const pageSize = 100
  const firstPage = await loader(1, pageSize)
  const records = [...(firstPage.records || [])]
  const totalPages = firstPage.pages || 1
  for (let page = 2; page <= totalPages; page++) {
    try {
      const nextPage = await loader(page, pageSize)
      if (nextPage.records?.length) {
        records.push(...nextPage.records)
      }
    } catch (err) {
      // 首页已成功时保留已获取数据，避免单页失败导致整块统计退化为“—”
      console.warn(`加载第 ${page} 页失败，使用已获取的 ${records.length} 条记录`, err)
      break
    }
  }
  return { total: firstPage.total || records.length, records }
}

async function fetchStats() {
  loading.value = true
  try {
    const [usersResult, ordersResult, productsResult] = await Promise.allSettled([
      getAdminUsersApi({ page: 1, size: 1 }),
      fetchAllPages<OrderSummary>((pageNum, pageSize) => getAdminOrdersApi({ pageNum, pageSize })),
      fetchAllPages<ProductAdminVO>((pageNum, pageSize) => listAdminProductsApi({ pageNum, pageSize }))
    ])

    if (usersResult.status === 'fulfilled') {
      usersError.value = false
      userCount.value = usersResult.value.total
    } else {
      usersError.value = true
      userCount.value = null
    }

    if (ordersResult.status === 'fulfilled') {
      ordersError.value = false
      const { total, records: orders } = ordersResult.value
      orderCount.value = total
      allOrders.value = orders

      const now = new Date()
      const todayStr = formatLocalDate(now)
      const yesterday = new Date(now)
      yesterday.setDate(now.getDate() - 1)
      const yesterdayStr = formatLocalDate(yesterday)

      let todaySum = 0
      let yesterdaySum = 0
      let totalSum = 0
      let todayCount = 0

      for (const order of orders) {
        if (!paidStatuses.has(order.status)) continue
        const amount = Number(order.totalAmount || 0)
        totalSum += amount
        const bizDate = (order.paymentTime || order.createdAt || '').slice(0, 10)
        if (bizDate === todayStr) {
          todaySum += amount
          todayCount += 1
        } else if (bizDate === yesterdayStr) {
          yesterdaySum += amount
        }
      }

      todaySales.value = todaySum
      yesterdaySales.value = yesterdaySum
      todayPaidCount.value = todayCount
      totalPaidSales.value = totalSum
    } else {
      ordersError.value = true
      orderCount.value = null
      todaySales.value = null
      yesterdaySales.value = 0
      todayPaidCount.value = 0
      totalPaidSales.value = 0
      allOrders.value = []
    }

    if (productsResult.status === 'fulfilled') {
      productsError.value = false
      const records = productsResult.value.records
      allProducts.value = records
      const onSale = records.filter((p) => p.status === 1)
      onSaleCount.value = onSale.length
      outOfStockCount.value = onSale.filter((p) => (p.stock ?? 0) <= 0).length
      lowStockCount.value = onSale.filter((p) => (p.stock ?? 0) > 0 && p.stock <= 10).length
    } else {
      productsError.value = true
      onSaleCount.value = null
      outOfStockCount.value = 0
      lowStockCount.value = 0
      allProducts.value = []
    }
  } finally {
    loading.value = false
  }
}

async function handleManualRefresh() {
  await fetchStats()
  if (!usersError.value && !ordersError.value && !productsError.value) {
    ElMessage.success('运营数据已刷新')
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* KPI */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.kpi {
  position: relative;
  padding: 20px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.02));
  overflow: hidden;
  cursor: default;
}

.kpi-link {
  cursor: pointer;
}

.kpi-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi-label {
  font-size: 12px;
  color: var(--text-3);
}

.kpi-tag {
  font-size: 8px;
  letter-spacing: 0.18em;
  color: var(--text-4);
}

.kpi-value {
  margin: 14px 0 8px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--text);
  line-height: 1.1;
}

.kpi-symbol {
  font-size: 16px;
  margin-right: 2px;
  color: var(--text-3);
}

.kpi-foot {
  font-size: 11px;
  color: var(--text-4);
}

.kpi-foot.is-up {
  color: var(--green);
}

.kpi-foot.is-down {
  color: var(--price);
}

.kpi-glow {
  position: absolute;
  right: -40px;
  top: -40px;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.25), transparent 70%);
  filter: blur(14px);
  pointer-events: none;
}

/* 通用面板 */
.panel {
  position: relative;
  padding: 22px;
  border: 1px solid var(--line);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.025);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.panel-kicker {
  font-size: 9px;
  letter-spacing: 0.2em;
  color: var(--text-4);
}

.panel-title {
  margin-top: 6px;
  font-size: 17px;
  font-weight: 700;
}

.panel-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
  font-size: 11px;
  color: var(--text-4);
  flex-wrap: wrap;
}

.panel-foot strong {
  color: var(--text);
}

/* 图表 */
.chart-row {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 16px;
}

.chart {
  position: relative;
  height: 220px;
  padding-top: 18px;
}

.chart-grid {
  position: absolute;
  inset: 18px 0 26px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.chart-grid span {
  height: 1px;
  background: var(--line);
}

.bars {
  position: relative;
  z-index: 1;
  height: 100%;
  display: flex;
  align-items: flex-end;
  gap: clamp(8px, 2vw, 20px);
}

.bar-col {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  height: 100%;
  justify-content: flex-end;
}

.bar-value {
  font-size: 9.5px;
  color: var(--text-4);
  white-space: nowrap;
}

.bar-track {
  width: 100%;
  max-width: 46px;
  height: 100%;
  display: flex;
  align-items: flex-end;
  border-radius: 8px;
  background: transparent;
  overflow: hidden;
}

.bar {
  width: 100%;
  border-radius: 8px 8px 3px 3px;
  background: linear-gradient(180deg, #22d3ee 0%, #7c5cff 100%);
  box-shadow: 0 0 22px -6px rgba(124, 92, 255, 0.9);
  transition: height 0.9s var(--ease);
  min-height: 4px;
}

.bar.is-empty {
  background: rgba(255, 255, 255, 0.09);
  box-shadow: none;
}

.bar-label {
  font-size: 9.5px;
  color: var(--text-4);
}

/* 状态分布 */
.status-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-row-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
}

.status-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-2);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-4);
}

.status-dot.is-warn {
  background: var(--amber);
}

.status-dot.is-info {
  background: var(--cyan);
}

.status-dot.is-violet {
  background: var(--violet);
}

.status-dot.is-ok {
  background: var(--green);
}

.status-dot.is-muted {
  background: var(--text-4);
}

.status-num {
  font-size: 12px;
  color: var(--text-2);
}

.status-track {
  height: 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.status-fill {
  height: 100%;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.2);
  transition: width 0.9s var(--ease);
}

.status-fill.is-warn {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
}

.status-fill.is-info {
  background: linear-gradient(90deg, #22d3ee, #0ea5e9);
}

.status-fill.is-violet {
  background: linear-gradient(90deg, #7c5cff, #a78bfa);
}

.status-fill.is-ok {
  background: linear-gradient(90deg, #34d399, #10b981);
}

.status-fill.is-muted {
  background: rgba(255, 255, 255, 0.25);
}

/* 底部 */
.bottom-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 16px;
}

.panel-empty {
  padding: 36px 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-4);
}

.stock-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stock-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 10px;
  border-radius: var(--radius-sm);
  transition: background 0.25s var(--ease);
}

.stock-item:hover {
  background: rgba(255, 255, 255, 0.04);
}

.stock-img {
  width: 40px;
  height: 40px;
  border-radius: 9px;
  border: 1px solid var(--line);
  flex-shrink: 0;
}

.stock-info {
  flex: 1;
  min-width: 0;
}

.stock-name {
  font-size: 12.5px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stock-meta {
  margin-top: 3px;
  font-size: 10px;
  color: var(--text-4);
}

.stock-badge {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
}

.stock-badge.is-low {
  border: 1px solid rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.1);
  color: #fcd77a;
}

.stock-badge.is-out {
  border: 1px solid rgba(255, 92, 122, 0.35);
  background: rgba(255, 92, 122, 0.1);
  color: #ff9db0;
}

/* 系统 */
.system-live {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--green);
}

.system-live i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--green);
  box-shadow: 0 0 8px var(--green);
}

.sys-list {
  margin: 0;
  display: flex;
  flex-direction: column;
}

.sys-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 11px 0;
  border-bottom: 1px dashed var(--line);
}

.sys-row:last-child {
  border-bottom: none;
}

.sys-row dt {
  font-size: 11.5px;
  color: var(--text-4);
}

.sys-row dd {
  margin: 0;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-2);
  text-align: right;
}

/* 响应式 */
@media (max-width: 1280px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .chart-row,
  .bottom-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .kpi-value {
    font-size: 26px;
  }

  .panel {
    padding: 18px;
  }

  .bars {
    gap: 6px;
  }

  .bar-value {
    display: none;
  }
}
</style>
