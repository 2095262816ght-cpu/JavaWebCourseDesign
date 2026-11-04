<template>
  <div class="orders-container">
    <div class="orders-header">
      <div class="title-wrap">
        <el-icon class="title-icon"><List /></el-icon>
        <h2>我的订单</h2>
      </div>
      <el-button link type="primary" @click="$router.push('/products')">
        <el-icon><ArrowLeft /></el-icon> 返回商城浏览
      </el-button>
    </div>

    <!-- Status Tabs -->
    <el-tabs v-model="activeStatus" class="orders-tabs" @tab-change="handleStatusChange">
      <el-tab-pane label="全部订单" name="" />
      <el-tab-pane label="待付款" name="UNPAID" />
      <el-tab-pane label="已支付" name="PAID" />
      <el-tab-pane label="已发货" name="SHIPPED" />
    </el-tabs>

    <!-- Orders Content -->
    <div v-loading="loading" class="orders-content">
      <!-- Empty State -->
      <el-card v-if="orders.length === 0" shadow="never" class="empty-card">
        <el-empty description="暂无符合条件的订单记录">
          <el-button type="primary" round @click="$router.push('/products')">
            去挑选商品下单
          </el-button>
        </el-empty>
      </el-card>

      <!-- Order Cards -->
      <div v-else class="order-card-list">
        <div v-for="order in orders" :key="order.id" class="order-card">
          <!-- Card Header -->
          <div class="card-header">
            <div class="header-left">
              <span class="order-no-label">订单编号：</span>
              <span class="order-no-val">{{ order.orderNo }}</span>
              <span class="header-time">{{ order.createdAt }}</span>
            </div>
            <div class="header-right">
              <el-tag :type="getStatusTagType(order.status)" effect="light" size="default">
                {{ getStatusText(order.status) }}
              </el-tag>
            </div>
          </div>

          <!-- Card Body: Item Snapshot List -->
          <div class="card-items">
            <div
              v-for="item in order.items"
              :key="item.id"
              class="order-item-row"
            >
              <el-image
                :src="item.productImage"
                class="item-img"
                fit="cover"
              />
              <div class="item-info">
                <div class="item-title">{{ item.productName }}</div>
                <div class="item-price-meta">
                  ¥ {{ item.unitPrice.toFixed(2) }} × {{ item.quantity }} 件
                </div>
              </div>
              <div class="item-subtotal">
                ¥ {{ item.totalPrice.toFixed(2) }}
              </div>
            </div>
          </div>

          <!-- Card Footer -->
          <div class="card-footer">
            <div class="shipping-info">
              <el-icon><Location /></el-icon>
              <span>{{ order.receiverName }}（{{ order.receiverPhone }}）{{ order.receiverAddress }}</span>
            </div>

            <div class="footer-actions">
              <div class="order-total">
                <span class="total-label">共 {{ order.itemCount }} 件商品，实付：</span>
                <span class="currency">¥</span>
                <span class="amount">{{ order.totalAmount.toFixed(2) }}</span>
              </div>

              <div class="btn-group">
                <el-button
                  v-if="order.status === 'UNPAID'"
                  type="danger"
                  size="default"
                  @click="openPayModal(order)"
                >
                  立即支付
                </el-button>

                <el-button
                  type="default"
                  size="default"
                  @click="openDetailModal(order.id)"
                >
                  订单详情
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- Pagination -->
        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="fetchOrders"
            @current-change="fetchOrders"
          />
        </div>
      </div>
    </div>

    <!-- Order Detail Drawer / Dialog -->
    <el-dialog
      v-model="detailVisible"
      title="订单详情快照"
      width="640px"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-dialog-body">
        <el-descriptions :column="2" border class="mb-4">
          <el-descriptions-item label="订单编号">{{ currentDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="getStatusTagType(currentDetail.status)">
              {{ getStatusText(currentDetail.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentDetail.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ currentDetail.paymentTime || '尚未支付' }}</el-descriptions-item>
          <el-descriptions-item label="收件人" :span="2">
            {{ currentDetail.receiverName }}（{{ currentDetail.receiverPhone }}）
          </el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">
            {{ currentDetail.receiverAddress }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-items-title">购买商品行项（历史价格快照归档）</div>
        <el-table :data="currentDetail.items" border style="width: 100%">
          <el-table-column label="商品图" width="70">
            <template #default="{ row }">
              <el-image :src="row.productImage" style="width: 44px; height: 44px; border-radius: 4px;" fit="cover" />
            </template>
          </el-table-column>
          <el-table-column prop="productName" label="商品名称" min-width="160" />
          <el-table-column prop="unitPrice" label="购买单价" width="110">
            <template #default="{ row }">¥ {{ row.unitPrice.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="totalPrice" label="小计" width="110" align="right">
            <template #default="{ row }">
              <strong style="color: #dc2626;">¥ {{ row.totalPrice.toFixed(2) }}</strong>
            </template>
          </el-table-column>
        </el-table>

        <div class="detail-total-box">
          <span>订单总金额：</span>
          <strong class="total-money">¥ {{ currentDetail.totalAmount.toFixed(2) }}</strong>
        </div>
      </div>
    </el-dialog>

    <!-- Simulated Payment Dialog -->
    <el-dialog
      v-model="payVisible"
      title="订单收银台 · 模拟在线支付"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="pay-body">
        <div class="pay-amount-box">
          <div class="pay-label">待支付金额</div>
          <div class="pay-val">¥ {{ payingOrder?.totalAmount?.toFixed(2) }}</div>
          <div class="pay-sub">订单号：{{ payingOrder?.orderNo }}</div>
        </div>

        <div class="method-card">
          <el-icon class="method-icon"><WalletFilled /></el-icon>
          <div class="method-name">模拟快捷支付通道</div>
          <el-tag type="success" size="small">极速免密</el-tag>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="payVisible = false">取消</el-button>
          <el-button
            type="success"
            :loading="paying"
            @click="handleExecutePay"
          >
            确认支付 ¥ {{ payingOrder?.totalAmount?.toFixed(2) }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  List,
  ArrowLeft,
  Location,
  WalletFilled
} from '@element-plus/icons-vue'
import {
  getMyOrdersApi,
  getOrderDetailApi,
  payOrderApi,
  type OrderSummary,
  type OrderDetail
} from '@/api/order'
import { useUserStore } from '@/stores/user'

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
      pageSize: pageSize.value
    })
    if (res) {
      let filtered = res.records || []
      if (activeStatus.value) {
        filtered = filtered.filter((o) => o.status === activeStatus.value)
      }
      orders.value = filtered
      total.value = res.total || 0
    }
  } catch (err: any) {
    ElMessage.error(err.message || '获取订单列表失败')
  } finally {
    loading.value = false
  }
}

function handleStatusChange() {
  fetchOrders()
}

function getStatusTagType(status: string) {
  switch (status) {
    case 'UNPAID':
      return 'warning'
    case 'PAID':
      return 'success'
    case 'SHIPPED':
      return 'primary'
    case 'COMPLETED':
      return 'info'
    case 'CANCELLED':
      return 'danger'
    default:
      return 'info'
  }
}

function getStatusText(status: string) {
  switch (status) {
    case 'UNPAID':
      return '待付款'
    case 'PAID':
      return '已付款'
    case 'SHIPPED':
      return '已发货'
    case 'COMPLETED':
      return '已完成'
    case 'CANCELLED':
      return '已取消'
    default:
      return status
  }
}

async function openDetailModal(orderId: number) {
  try {
    const detail = await getOrderDetailApi(orderId)
    currentDetail.value = detail
    detailVisible.value = true
  } catch (err: any) {
    ElMessage.error(err.message || '获取订单详情失败')
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
    ElMessage.error(err.message || '支付失败')
  } finally {
    paying.value = false
  }
}
</script>

<style scoped>
.orders-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

.orders-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 26px;
  color: #2563eb;
}

.title-wrap h2 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.orders-tabs {
  margin-bottom: 20px;
}

.empty-card {
  padding: 60px 0;
  border-radius: 12px;
}

.order-card-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.order-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background-color: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.order-no-label {
  color: #6b7280;
}

.order-no-val {
  font-weight: 600;
  color: #1f2937;
}

.header-time {
  color: #9ca3af;
}

.card-items {
  padding: 16px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-img {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  border: 1px solid #f3f4f6;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
}

.item-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.item-price-meta {
  font-size: 13px;
  color: #6b7280;
  margin-top: 4px;
}

.item-subtotal {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background-color: #fafbfc;
  border-top: 1px solid #f3f4f6;
  font-size: 13px;
}

.shipping-info {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #6b7280;
  max-width: 50%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footer-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.order-total {
  display: flex;
  align-items: baseline;
}

.total-label {
  color: #4b5563;
}

.currency {
  font-size: 14px;
  font-weight: 700;
  color: #dc2626;
  margin-left: 4px;
}

.amount {
  font-size: 20px;
  font-weight: 800;
  color: #dc2626;
}

.btn-group {
  display: flex;
  gap: 10px;
}

.pagination-wrapper {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}

/* Pay Dialog */
.pay-amount-box {
  text-align: center;
  padding: 16px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.pay-label {
  font-size: 13px;
  color: #6b7280;
}

.pay-val {
  font-size: 32px;
  font-weight: 800;
  color: #059669;
  margin: 6px 0;
}

.pay-sub {
  font-size: 13px;
  color: #4b5563;
}

.method-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 8px;
  border: 1px solid #10b981;
  background-color: #ecfdf5;
  margin-top: 16px;
}

.method-icon {
  font-size: 24px;
  color: #059669;
}

.method-name {
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: #065f46;
}

.detail-items-title {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
  margin: 16px 0 8px;
}

.detail-total-box {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  margin-top: 16px;
  font-size: 15px;
}

.total-money {
  font-size: 22px;
  color: #dc2626;
  margin-left: 6px;
}
</style>
