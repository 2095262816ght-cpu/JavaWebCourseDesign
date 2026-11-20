<template>
  <div class="orders-page page-container">
    <!-- 页头 -->
    <header class="orders-head">
      <div>
        <span class="eyebrow">ORDER HISTORY</span>
        <h1 class="display-lg orders-title">我的订单</h1>
        <p class="orders-sub">
          共 <span class="mono accent">{{ total }}</span> 笔订单记录
        </p>
      </div>
      <button class="btn btn--ghost btn--sm" type="button" @click="$router.push('/products')">
        <el-icon><ArrowLeft /></el-icon>
        返回商城
      </button>
    </header>

    <!-- 状态筛选 -->
    <div class="status-tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.value"
        type="button"
        class="status-tab"
        :class="{ 'is-active': activeStatus === tab.value }"
        @click="changeStatus(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- 内容 -->
    <div v-loading="loading" class="orders-body">
      <div v-if="orders.length === 0 && !loading" class="orders-empty">
        <span class="empty-orb"></span>
        <h2 class="empty-title">暂无订单记录</h2>
        <p class="empty-desc">挑选商品并完成结算后，订单会出现在这里</p>
        <button class="btn btn--primary btn--sm" type="button" @click="$router.push('/products')">
          去挑选商品
        </button>
      </div>

      <template v-else>
        <article v-for="order in orders" :key="order.id" class="order-card" v-reveal>
          <!-- 卡片头 -->
          <header class="order-head">
            <div class="order-head-left">
              <span class="order-no mono">{{ order.orderNo }}</span>
              <span class="order-time">{{ formatDateTime(order.createdAt) }}</span>
            </div>
            <span class="status-chip" :class="statusClass(order.status)">
              {{ getOrderStatusText(order.status) }}
            </span>
          </header>

          <!-- 商品行 -->
          <div class="order-items">
            <div v-for="item in order.items" :key="item.id" class="order-item">
              <el-image :src="item.productImage" class="item-img" fit="cover">
                <template #error>
                  <span class="item-fallback"><el-icon><Picture /></el-icon></span>
                </template>
              </el-image>
              <div class="item-info">
                <p class="item-title">{{ item.productName }}</p>
                <p class="item-meta mono">
                  ¥{{ Number(item.unitPrice ?? 0).toFixed(2) }} × {{ item.quantity }}
                </p>
              </div>
              <span class="item-subtotal mono">¥{{ Number(item.totalPrice ?? 0).toFixed(2) }}</span>
            </div>
          </div>

          <!-- 卡片脚 -->
          <footer class="order-foot">
            <p class="ship-info">
              <el-icon><Location /></el-icon>
              <span>{{ order.receiverName }} · {{ order.receiverPhone }} · {{ order.receiverAddress }}</span>
            </p>

            <div class="foot-right">
              <div class="order-total">
                <span class="total-label">{{ order.itemCount }} 件商品 · 实付</span>
                <span class="price">
                  <span class="symbol">¥</span>
                  <span class="amount">{{ Number(order.totalAmount ?? 0).toFixed(2) }}</span>
                </span>
              </div>

              <div class="btn-group">
                <button
                  v-if="order.status === 'UNPAID'"
                  class="btn btn--primary btn--sm"
                  type="button"
                  @click="openPayModal(order)"
                >
                  立即支付
                </button>
                <button
                  v-if="order.status === 'SHIPPED'"
                  class="btn btn--primary btn--sm"
                  type="button"
                  @click="handleConfirmReceipt(order)"
                >
                  确认收货
                </button>
                <button
                  v-if="order.status === 'UNPAID' || order.status === 'PAID'"
                  class="btn btn--ghost btn--sm"
                  type="button"
                  @click="handleCancelOrder(order)"
                >
                  取消订单
                </button>
                <button class="btn btn--ghost btn--sm" type="button" @click="openDetailModal(order.id)">
                  订单详情
                </button>
              </div>
            </div>
          </footer>
        </article>

        <div class="pagination-bar">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="handleSizeChange"
            @current-change="fetchOrders"
          />
        </div>
      </template>
    </div>

    <!-- 订单详情 -->
    <el-dialog v-model="detailVisible" title="订单详情快照" width="680px" destroy-on-close>
      <div v-if="currentDetail" class="detail-body">
        <div class="detail-grid">
          <div class="detail-cell">
            <span class="cell-label">订单编号</span>
            <span class="cell-value mono">{{ currentDetail.orderNo }}</span>
          </div>
          <div class="detail-cell">
            <span class="cell-label">当前状态</span>
            <span class="status-chip" :class="statusClass(currentDetail.status)">
              {{ getOrderStatusText(currentDetail.status) }}
            </span>
          </div>
          <div class="detail-cell">
            <span class="cell-label">下单时间</span>
            <span class="cell-value">{{ formatDateTime(currentDetail.createdAt) }}</span>
          </div>
          <div class="detail-cell">
            <span class="cell-label">支付时间</span>
            <span class="cell-value">
              {{ currentDetail.paymentTime ? formatDateTime(currentDetail.paymentTime) : '尚未支付' }}
            </span>
          </div>
          <div class="detail-cell is-wide">
            <span class="cell-label">收件信息</span>
            <span class="cell-value">
              {{ currentDetail.receiverName }} · {{ currentDetail.receiverPhone }}
            </span>
          </div>
          <div class="detail-cell is-wide">
            <span class="cell-label">配送地址</span>
            <span class="cell-value">{{ currentDetail.receiverAddress }}</span>
          </div>
        </div>

        <p class="detail-label mono">ITEM SNAPSHOTS</p>
        <div class="detail-items">
          <div v-for="row in currentDetail.items" :key="row.id" class="detail-item">
            <el-image :src="row.productImage" class="detail-img" fit="cover" />
            <div class="detail-item-info">
              <p class="detail-item-name">{{ row.productName }}</p>
              <p class="detail-item-meta mono">
                ¥{{ Number(row.unitPrice ?? 0).toFixed(2) }} × {{ row.quantity }}
              </p>
            </div>
            <span class="detail-item-price mono">¥{{ Number(row.totalPrice ?? 0).toFixed(2) }}</span>
          </div>
        </div>

        <div class="detail-total">
          <span>订单总金额</span>
          <span class="price">
            <span class="symbol">¥</span>
            <span class="amount">{{ Number(currentDetail.totalAmount ?? 0).toFixed(2) }}</span>
          </span>
        </div>
      </div>
    </el-dialog>

    <!-- 支付 -->
    <el-dialog
      v-model="payVisible"
      title="订单收银台"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="pay-box">
        <p class="pay-label">待支付金额</p>
        <p class="pay-money mono">
          <span class="symbol">¥</span>{{ Number(payingOrder?.totalAmount ?? 0).toFixed(2) }}
        </p>
        <p class="pay-no mono">订单号 {{ payingOrder?.orderNo }}</p>

        <div class="pay-method">
          <span class="method-icon"><el-icon><WalletFilled /></el-icon></span>
          <div class="method-info">
            <p class="method-name">在线支付</p>
            <p class="method-desc">支持微信 / 支付宝 / 银行卡</p>
          </div>
          <span class="method-badge">推荐</span>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <button class="btn btn--ghost btn--sm" type="button" @click="payVisible = false">取消</button>
          <button class="btn btn--primary btn--sm" type="button" :disabled="paying" @click="handleExecutePay">
            {{ paying ? '支付中…' : `确认支付 ¥${Number(payingOrder?.totalAmount ?? 0).toFixed(2)}` }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Location, Picture, WalletFilled } from '@element-plus/icons-vue'
import {
  getMyOrdersApi,
  getOrderDetailApi,
  payOrderApi,
  confirmReceiptApi,
  cancelOrderApi,
  type OrderSummary,
  type OrderDetail
} from '@/api/order'
import { useUserStore } from '@/stores/user'
import { formatDateTime } from '@/utils/format'
import { getOrderStatusText } from '@/utils/order'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const paying = ref(false)
const activeStatus = ref('')
const orders = ref<OrderSummary[]>([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const currentDetail = ref<OrderDetail | null>(null)

const payVisible = ref(false)
const payingOrder = ref<OrderSummary | null>(null)

const statusTabs = [
  { value: '', label: '全部订单' },
  { value: 'UNPAID', label: '待付款' },
  { value: 'PAID', label: '已支付' },
  { value: 'SHIPPED', label: '已发货' },
  { value: 'COMPLETED', label: '已完成' }
]

function statusClass(status: string) {
  switch (status) {
    case 'UNPAID':
      return 'is-warn'
    case 'PAID':
      return 'is-info'
    case 'SHIPPED':
      return 'is-violet'
    case 'COMPLETED':
      return 'is-ok'
    case 'CANCELLED':
      return 'is-muted'
    default:
      return ''
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录账号后再查看订单列表')
    router.push('/login?redirect=/orders')
    return
  }
  await fetchOrders()
})

async function fetchOrders() {
  loading.value = true
  try {
    const res = await getMyOrdersApi({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      status: activeStatus.value || undefined
    })
    if (res) {
      orders.value = res.records || []
      total.value = res.total || 0
    }
  } catch (err: any) {
    console.error('获取订单列表失败', err)
  } finally {
    loading.value = false
  }
}

function changeStatus(status: string) {
  activeStatus.value = status
  pageNum.value = 1
  fetchOrders()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  pageNum.value = 1
  fetchOrders()
}

async function openDetailModal(orderId: number) {
  try {
    const detail = await getOrderDetailApi(orderId)
    currentDetail.value = detail
    detailVisible.value = true
  } catch (err: any) {
    console.error('获取订单详情失败', err)
  }
}

function openPayModal(order: OrderSummary) {
  payingOrder.value = order
  payVisible.value = true
}

async function handleExecutePay() {
  if (!payingOrder.value) return
  paying.value = true
  try {
    await payOrderApi(payingOrder.value.orderNo)
    ElMessage.success('支付成功！')
    payVisible.value = false
    await fetchOrders()
  } catch (err: any) {
    console.error('支付失败', err)
  } finally {
    paying.value = false
  }
}

async function handleCancelOrder(order: OrderSummary) {
  try {
    await ElMessageBox.confirm(
      `确定取消订单 ${order.orderNo} 吗？取消后商品库存将自动归还。`,
      '取消订单确认',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }
  try {
    await cancelOrderApi(order.id)
    ElMessage.success('订单已取消，库存已归还')
    await fetchOrders()
  } catch (err: any) {
    console.error('取消订单失败', err)
  }
}

async function handleConfirmReceipt(order: OrderSummary) {
  try {
    await ElMessageBox.confirm(
      `请确认已收到订单 ${order.orderNo} 的商品，确认后订单将标记为已完成。`,
      '确认收货',
      { type: 'info', confirmButtonText: '确认已收到', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await confirmReceiptApi(order.id)
    ElMessage.success('确认收货成功')
    await fetchOrders()
  } catch (err: any) {
    console.error('确认收货失败', err)
  }
}
</script>

<style scoped>
.orders-page {
  padding-top: 28px;
  padding-bottom: 40px;
}

/* 页头 */
.orders-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 12px 0 30px;
  flex-wrap: wrap;
}

.orders-title {
  margin-top: 12px;
}

.orders-sub {
  margin-top: 10px;
  font-size: 13px;
  color: var(--text-3);
}

.accent {
  color: var(--cyan);
  font-weight: 700;
}

/* 状态筛选 */
.status-tabs {
  display: flex;
  gap: 6px;
  padding: 5px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.03);
  width: fit-content;
  margin-bottom: 26px;
  max-width: 100%;
  overflow-x: auto;
}

.status-tab {
  padding: 8px 18px;
  border: none;
  border-radius: 999px;
  background: transparent;
  color: var(--text-3);
  font-family: var(--font-body);
  font-size: 12.5px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s var(--ease);
}

.status-tab:hover {
  color: var(--text);
}

.status-tab.is-active {
  background: var(--grad);
  color: #07080d;
  font-weight: 600;
}

/* 空态 */
.orders-empty {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 100px 24px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-xl);
  text-align: center;
}

.empty-orb {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, rgba(124, 92, 255, 0.6), rgba(34, 211, 238, 0.1) 62%, transparent 74%);
  filter: blur(8px);
  margin-bottom: 8px;
}

.empty-title {
  font-size: 21px;
  font-weight: 700;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-3);
  margin-bottom: 10px;
}

/* 订单卡片 */
.orders-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 220px;
}

.order-card {
  border: 1px solid var(--line);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.025);
  overflow: hidden;
  transition: border-color 0.4s var(--ease), transform 0.4s var(--ease);
}

.order-card:hover {
  border-color: rgba(124, 92, 255, 0.3);
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.02);
}

.order-head-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.order-no {
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--text);
}

.order-time {
  font-size: 11.5px;
  color: var(--text-4);
}

/* 状态标签 */
.status-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.05);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-2);
  white-space: nowrap;
}

.status-chip.is-warn {
  border-color: rgba(251, 191, 36, 0.4);
  background: rgba(251, 191, 36, 0.1);
  color: #fcd77a;
}

.status-chip.is-info {
  border-color: rgba(34, 211, 238, 0.4);
  background: rgba(34, 211, 238, 0.1);
  color: #7ce8f7;
}

.status-chip.is-violet {
  border-color: rgba(124, 92, 255, 0.45);
  background: rgba(124, 92, 255, 0.14);
  color: #c4b8ff;
}

.status-chip.is-ok {
  border-color: rgba(52, 211, 153, 0.4);
  background: rgba(52, 211, 153, 0.1);
  color: #7cf0c4;
}

.status-chip.is-muted {
  color: var(--text-4);
}

/* 商品行 */
.order-items {
  padding: 8px 24px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px dashed var(--line);
}

.order-item:last-child {
  border-bottom: none;
}

.item-img {
  width: 58px;
  height: 58px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  flex-shrink: 0;
}

.item-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-4);
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-meta {
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--text-4);
}

.item-subtotal {
  font-size: 14px;
  color: var(--text-2);
}

/* 卡片脚 */
.order-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 24px;
  border-top: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.02);
  flex-wrap: wrap;
}

.ship-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 200px;
  font-size: 11.5px;
  color: var(--text-4);
  overflow: hidden;
}

.ship-info span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ship-info .el-icon {
  flex-shrink: 0;
  color: var(--text-3);
}

.foot-right {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
}

.order-total {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.total-label {
  font-size: 11.5px;
  color: var(--text-4);
}

.order-total .price .amount {
  font-size: 24px;
}

.btn-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* 详情 */
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1px;
  background: var(--line);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  overflow: hidden;
}

.detail-cell {
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 14px 16px;
  background: #0a0d16;
}

.detail-cell.is-wide {
  grid-column: span 2;
}

.cell-label {
  font-size: 10.5px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.cell-value {
  font-size: 13px;
  color: var(--text-2);
}

.detail-label {
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--cyan);
}

.detail-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.03);
}

.detail-img {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  flex-shrink: 0;
}

.detail-item-info {
  flex: 1;
  min-width: 0;
}

.detail-item-name {
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-item-meta {
  margin-top: 3px;
  font-size: 11px;
  color: var(--text-4);
}

.detail-item-price {
  font-size: 13px;
  color: var(--text-2);
}

.detail-total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(255, 92, 122, 0.24);
  border-radius: var(--radius);
  background: rgba(255, 92, 122, 0.06);
  font-size: 12.5px;
  color: var(--text-2);
}

/* 支付 */
.pay-box {
  text-align: center;
}

.pay-label {
  font-size: 12px;
  color: var(--text-3);
}

.pay-money {
  margin: 8px 0 6px;
  font-size: 42px;
  font-weight: 700;
  letter-spacing: -0.03em;
  color: var(--green);
}

.pay-money .symbol {
  font-size: 22px;
  margin-right: 2px;
}

.pay-no {
  font-size: 11px;
  color: var(--text-4);
}

.pay-method {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 24px;
  padding: 16px;
  border: 1px solid rgba(52, 211, 153, 0.4);
  border-radius: var(--radius);
  background: rgba(52, 211, 153, 0.07);
  text-align: left;
}

.method-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: rgba(52, 211, 153, 0.15);
  color: var(--green);
  font-size: 20px;
}

.method-info {
  flex: 1;
}

.method-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.method-desc {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-3);
}

.method-badge {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--green);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 响应式 */
@media (max-width: 760px) {
  .order-foot {
    flex-direction: column;
    align-items: flex-start;
  }

  .foot-right {
    width: 100%;
    justify-content: space-between;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-cell.is-wide {
    grid-column: span 1;
  }
}

@media (max-width: 520px) {
  .order-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .order-items,
  .order-foot {
    padding-left: 16px;
    padding-right: 16px;
  }

  .btn-group {
    width: 100%;
  }
}
</style>
