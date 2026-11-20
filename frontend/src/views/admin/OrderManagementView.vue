<template>
  <div class="admin-orders-view">
    <div class="page-header">
      <div class="header-left">
        <h2>订单管理</h2>
        <span class="subtitle">全平台客户订单流转监控、快照归档与发货履约</span>
      </div>
      <div class="header-right">
        <el-button :icon="Refresh" @click="fetchOrders">刷新列表</el-button>
      </div>
    </div>

    <!-- Filter Card -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="订单编号">
          <el-input
            v-model="queryForm.orderNo"
            placeholder="输入订单号模糊检索"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="订单状态">
          <el-select
            v-model="queryForm.status"
            placeholder="全部状态"
            clearable
            style="width: 150px"
          >
            <el-option label="全部状态" value="" />
            <el-option label="待付款 (UNPAID)" value="UNPAID" />
            <el-option label="已付款 (PAID)" value="PAID" />
            <el-option label="已发货 (SHIPPED)" value="SHIPPED" />
            <el-option label="已完成 (COMPLETED)" value="COMPLETED" />
            <el-option label="已取消 (CANCELLED)" value="CANCELLED" />
          </el-select>
        </el-form-item>

        <el-form-item label="收件人电话">
          <el-input
            v-model="queryForm.receiverPhone"
            placeholder="手机号检索"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshRight" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table Card -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="orders"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column prop="orderNo" label="业务订单号" min-width="190">
          <template #default="{ row }">
            <span class="mono-order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="customerId" label="顾客ID" width="85" align="center" />

        <el-table-column prop="totalAmount" label="订单金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount-val">¥ {{ Number(row.totalAmount ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="收货人信息" min-width="180">
          <template #default="{ row }">
            <div class="receiver-cell">
              <div><strong>{{ row.receiverName }}</strong> ({{ row.receiverPhone }})</div>
              <div class="address-sub" :title="row.receiverAddress">{{ row.receiverAddress }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="getOrderStatusTagType(row.status)">
              {{ getOrderStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="下单时间" width="170" align="center">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="250" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="openOrderDetail(row)"
            >
              详情
            </el-button>

            <el-divider direction="vertical" />

            <el-button
              link
              type="success"
              size="small"
              :disabled="row.status !== 'PAID'"
              @click="handleShipOrder(row)"
            >
              标记发货
            </el-button>

            <el-divider direction="vertical" />

            <el-button
              link
              type="danger"
              size="small"
              :disabled="row.status !== 'UNPAID' && row.status !== 'PAID'"
              @click="handleCancelOrder(row)"
            >
              取消订单
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryForm.pageNum"
          v-model:page-size="queryForm.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="fetchOrders"
        />
      </div>
    </el-card>

    <!-- Order Detail Drawer -->
    <el-drawer
      v-model="drawerVisible"
      title="订单全量详情及快照明细"
      size="550px"
      destroy-on-close
    >
      <div v-if="selectedOrder" class="drawer-content">
        <el-descriptions title="基本信息" :column="1" border class="mb-4">
          <el-descriptions-item label="订单编号">{{ selectedOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getOrderStatusTagType(selectedOrder.status)">
              {{ getOrderStatusText(selectedOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="顾客ID">{{ selectedOrder.customerId || selectedOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(selectedOrder.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ selectedOrder.paymentTime ? formatDateTime(selectedOrder.paymentTime) : '未支付' }}</el-descriptions-item>
          <el-descriptions-item label="收件人姓名">{{ selectedOrder.deliveryAddress?.receiverName || selectedOrder.receiverName }}</el-descriptions-item>
          <el-descriptions-item label="收件人电话">{{ selectedOrder.deliveryAddress?.receiverPhone || selectedOrder.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货详细地址">{{ selectedOrder.deliveryAddress?.receiverAddress || selectedOrder.receiverAddress }}</el-descriptions-item>
        </el-descriptions>

        <h4 class="drawer-section-title">订单商品行项快照（{{ selectedOrder.items?.length || 0 }} 款）</h4>
        <div class="snapshot-items-list">
          <div
            v-for="item in selectedOrder.items"
            :key="item.id"
            class="snapshot-item-card"
          >
            <el-image :src="item.productImage" class="snapshot-img" fit="cover" />
            <div class="snapshot-meta">
              <div class="snapshot-title">{{ item.productName }}</div>
              <div class="snapshot-calc">
                ¥ {{ Number(item.unitPrice ?? 0).toFixed(2) }} × {{ item.quantity }} 件
              </div>
            </div>
            <div class="snapshot-total">
              ¥ {{ Number(item.totalPrice ?? 0).toFixed(2) }}
            </div>
          </div>
        </div>

        <div class="drawer-summary-bar">
          <span>实收订单总额：</span>
          <strong class="drawer-total-amount">¥ {{ Number(selectedOrder.totalAmount ?? 0).toFixed(2) }}</strong>
        </div>

        <div v-if="selectedOrder.status === 'PAID'" class="drawer-actions">
          <el-button type="success" size="large" style="width: 100%" @click="handleShipOrder(selectedOrder)">
            立即标记发货
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  RefreshRight,
  Refresh
} from '@element-plus/icons-vue'
import {
  getAdminOrdersApi,
  getAdminOrderDetailApi,
  shipAdminOrderApi,
  cancelAdminOrderApi,
  type OrderSummary,
  type OrderDetail
} from '@/api/order'
import { formatDateTime } from '@/utils/format'
import { getOrderStatusTagType, getOrderStatusText } from '@/utils/order'

const loading = ref(false)
const orders = ref<OrderSummary[]>([])
const total = ref(0)

const queryForm = reactive({
  orderNo: '',
  status: '',
  receiverPhone: '',
  pageNum: 1,
  pageSize: 10
})

const drawerVisible = ref(false)
const selectedOrder = ref<OrderDetail | null>(null)

onMounted(() => {
  fetchOrders()
})

// 翻页、筛选与发货/取消后的刷新可能并发，只采纳最后一次请求的响应
let orderRequestSeq = 0

async function fetchOrders() {
  const seq = ++orderRequestSeq
  loading.value = true
  try {
    const res = await getAdminOrdersApi(queryForm)
    if (seq !== orderRequestSeq) return
    if (res) {
      orders.value = res.records || []
      total.value = res.total || 0
    }
  } catch (err: any) {
    console.error('获取订单列表失败', err)
  } finally {
    if (seq === orderRequestSeq) {
      loading.value = false
    }
  }
}

function handleSizeChange(size: number) {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  fetchOrders()
}

function handleSearch() {
  queryForm.pageNum = 1
  fetchOrders()
}

function handleReset() {
  queryForm.orderNo = ''
  queryForm.status = ''
  queryForm.receiverPhone = ''
  queryForm.pageNum = 1
  fetchOrders()
}

async function openOrderDetail(order: OrderSummary) {
  try {
    const detail = await getAdminOrderDetailApi(order.id)
    selectedOrder.value = detail
    drawerVisible.value = true
  } catch (err: any) {
    console.error('获取订单详情失败', err)
  }
}

function handleShipOrder(order: OrderSummary | OrderDetail) {
  ElMessageBox.confirm(
    `确定要将订单【${order.orderNo}】标记为已发货吗？系统将流转状态并记录履约。`,
    '发货确认',
    {
      confirmButtonText: '确定发货',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await shipAdminOrderApi(order.id)
      ElMessage.success('订单已成功流转为发货状态')
      if (drawerVisible.value && selectedOrder.value?.id === order.id) {
        selectedOrder.value.status = 'SHIPPED'
      }
      await fetchOrders()
    } catch (err: any) {
      console.error('发货操作失败', err)
    }
  }).catch(() => {})
}

function handleCancelOrder(order: OrderSummary | OrderDetail) {
  ElMessageBox.confirm(
    `确定要取消订单【${order.orderNo}】吗？取消后商品库存将自动归还，且不可恢复。`,
    '取消订单确认',
    {
      confirmButtonText: '确定取消',
      cancelButtonText: '再想想',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await cancelAdminOrderApi(order.id)
      ElMessage.success('订单已取消，库存已归还')
      if (drawerVisible.value && selectedOrder.value?.id === order.id) {
        selectedOrder.value.status = 'CANCELLED'
      }
      await fetchOrders()
    } catch (err: any) {
      console.error('取消订单失败', err)
    }
  }).catch(() => {})
}
</script>

<style scoped>
.admin-orders-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}

.subtitle {
  font-size: 13px;
  color: var(--text-3);
  margin-top: 4px;
  display: block;
}

.filter-card {
  border-radius: 16px;
  border: 1px solid var(--glass-border);
}

.filter-form {
  margin-bottom: -18px;
}

.table-card {
  border-radius: 16px;
  border: 1px solid var(--glass-border);
}

.mono-order-no {
  font-family: monospace;
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.amount-val {
  font-weight: 700;
  color: var(--price);
  font-size: 14px;
}

.receiver-cell {
  line-height: 1.4;
  font-size: 13px;
}

.address-sub {
  color: var(--text-3);
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 260px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* Drawer */
.drawer-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drawer-section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text);
  margin: 8px 0 0;
}

.snapshot-items-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.snapshot-item-card {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: rgba(255, 255, 255, 0.04);
  border-radius: 8px;
  padding: 10px 14px;
}

.snapshot-img {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  border: 1px solid var(--line);
  flex-shrink: 0;
}

.snapshot-meta {
  flex: 1;
  min-width: 0;
}

.snapshot-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.snapshot-calc {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
}

.snapshot-total {
  font-size: 14px;
  font-weight: 700;
  color: var(--price);
}

.drawer-summary-bar {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  background-color: rgba(255, 92, 122, 0.07);
  border: 1px solid rgba(255, 92, 122, 0.25);
  border-radius: 8px;
  padding: 12px 16px;
  font-size: 14px;
  color: #ff9db0;
}

.drawer-total-amount {
  font-size: 22px;
  font-weight: 800;
  color: var(--price);
  margin-left: 6px;
}

.drawer-actions {
  margin-top: 12px;
}
</style>
