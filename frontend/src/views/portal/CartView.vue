<template>
  <div class="cart-container">
    <div class="cart-header-wrapper">
      <div class="cart-header-inner">
        <div class="title-cell">
          <el-icon class="title-icon"><ShoppingCart /></el-icon>
          <h2>我的购物车</h2>
          <span class="item-count-badge" v-if="cartStore.cartCount > 0">
            共 {{ cartStore.cartCount }} 件商品
          </span>
        </div>
        <div class="actions-cell">
          <el-button link type="primary" @click="$router.push('/products')">
            <el-icon><ArrowLeft /></el-icon> 继续挑选商品
          </el-button>
          <el-button
            v-if="cartStore.items.length > 0"
            link
            type="danger"
            @click="handleClearCart"
          >
            <el-icon><Delete /></el-icon> 清空购物车
          </el-button>
        </div>
      </div>
    </div>

    <!-- Main Content Area -->
    <div class="cart-body">
      <!-- Empty State -->
      <el-card v-if="cartStore.items.length === 0" shadow="never" class="empty-card">
        <el-empty
          description="您的购物车空空如也，快去挑选心仪的商品吧！"
          :image-size="160"
        >
          <el-button type="primary" size="large" round @click="$router.push('/products')">
            前往商品中心逛逛
          </el-button>
        </el-empty>
      </el-card>

      <!-- Cart Item List -->
      <template v-else>
        <el-card shadow="never" class="cart-table-card">
          <div class="cart-table-header">
            <div class="col col-check">
              <el-checkbox
                :model-value="cartStore.isAllChecked"
                @change="handleCheckAll"
              >
                全选
              </el-checkbox>
            </div>
            <div class="col col-info">商品信息</div>
            <div class="col col-price">单价</div>
            <div class="col col-qty">数量</div>
            <div class="col col-subtotal">小计</div>
            <div class="col col-action">操作</div>
          </div>

          <div class="cart-item-rows">
            <div
              v-for="item in cartStore.items"
              :key="item.id"
              class="cart-row"
              :class="{ 'is-off-shelf': item.productStatus === 0 }"
            >
              <!-- Checkbox -->
              <div class="col col-check">
                <el-checkbox
                  :disabled="item.productStatus === 0"
                  :model-value="item.checked"
                  @change="onToggleItem(item.id, $event)"
                />
              </div>

              <!-- Product Info -->
              <div class="col col-info">
                <el-image
                  :src="item.productImage"
                  class="item-img"
                  fit="cover"
                  @click="goToProduct(item.productId)"
                >
                  <template #error>
                    <div class="image-fallback">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="item-meta">
                  <div class="item-name" @click="goToProduct(item.productId)">
                    {{ item.productName }}
                  </div>
                  <div class="item-tags">
                    <el-tag
                      v-if="item.productStatus === 0"
                      type="danger"
                      size="small"
                      effect="dark"
                    >
                      已下架
                    </el-tag>
                    <el-tag
                      v-else-if="item.stock <= 5"
                      type="warning"
                      size="small"
                    >
                      仅剩 {{ item.stock }} 件
                    </el-tag>
                    <span v-else class="stock-text">库存充足 ({{ item.stock }})</span>
                  </div>
                </div>
              </div>

              <!-- Unit Price -->
              <div class="col col-price">
                <span class="price-symbol">¥</span>
                <span class="price-val">{{ item.price.toFixed(2) }}</span>
              </div>

              <!-- Stepper Quantity -->
              <div class="col col-qty">
                <el-input-number
                  :model-value="item.quantity"
                  :min="1"
                  :max="item.stock || 99"
                  :disabled="item.productStatus === 0"
                  size="small"
                  @change="onQuantityChange(item.id, $event)"
                />
              </div>

              <!-- Subtotal -->
              <div class="col col-subtotal">
                <span class="subtotal-symbol">¥</span>
                <span class="subtotal-val">{{ item.subtotal.toFixed(2) }}</span>
              </div>

              <!-- Action -->
              <div class="col col-action">
                <el-popconfirm
                  title="确认从购物车移除该商品吗？"
                  confirm-button-text="确定"
                  cancel-button-text="取消"
                  @confirm="handleRemoveItem(item.id)"
                >
                  <template #reference>
                    <el-button link type="danger">
                      <el-icon><Delete /></el-icon> 移除
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </el-card>

        <!-- Sticky Checkout Bar -->
        <div class="checkout-bar">
          <div class="bar-left">
            <el-checkbox
              :model-value="cartStore.isAllChecked"
              @change="handleCheckAll"
            >
              全选
            </el-checkbox>
            <span class="divider">|</span>
            <span class="selected-text">
              已选 <strong class="highlight-count">{{ cartStore.checkedCount }}</strong> 件商品
            </span>
          </div>

          <div class="bar-right">
            <div class="total-wrap">
              <span class="total-label">合计金额：</span>
              <span class="currency-symbol">¥</span>
              <span class="total-amount">{{ cartStore.totalAmount.toFixed(2) }}</span>
            </div>
            <el-button
              type="primary"
              size="large"
              class="checkout-btn"
              :disabled="cartStore.checkedCount === 0"
              @click="openCheckoutDialog"
            >
              去结算 ({{ cartStore.checkedCount }})
            </el-button>
          </div>
        </div>
      </template>
    </div>

    <!-- Checkout Modal Dialog -->
    <el-dialog
      v-model="checkoutVisible"
      title="确认订单并结算"
      width="600px"
      destroy-on-close
      class="checkout-dialog"
    >
      <div class="checkout-dialog-content">
        <!-- Order Items Preview -->
        <div class="section-title">结算商品清单（共 {{ cartStore.checkedCount }} 件）</div>
        <div class="checkout-items-preview">
          <div
            v-for="item in checkedItems"
            :key="item.id"
            class="preview-item"
          >
            <el-image :src="item.productImage" class="preview-img" fit="cover" />
            <div class="preview-info">
              <div class="preview-name">{{ item.productName }}</div>
              <div class="preview-meta">
                ¥ {{ item.price.toFixed(2) }} × {{ item.quantity }}
              </div>
            </div>
            <div class="preview-price">
              ¥ {{ item.subtotal.toFixed(2) }}
            </div>
          </div>
        </div>

        <el-divider />

        <!-- Shipping Address Form -->
        <div class="section-title">收件人及送货地址</div>
        <el-form
          ref="checkoutFormRef"
          :model="checkoutForm"
          :rules="checkoutRules"
          label-position="top"
          class="checkout-form"
        >
          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="收货人姓名" prop="receiverName">
                <el-input
                  v-model="checkoutForm.receiverName"
                  placeholder="例如：张同学"
                  maxlength="32"
                  clearable
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="receiverPhone">
                <el-input
                  v-model="checkoutForm.receiverPhone"
                  placeholder="例如：13800138000"
                  maxlength="20"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="详细送货地址" prop="receiverAddress">
            <el-input
              v-model="checkoutForm.receiverAddress"
              type="textarea"
              :rows="2"
              placeholder="例如：北京市海淀区中关村南大街1号 XX教学楼 302"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>

        <div class="checkout-summary-box">
          <span class="box-label">应付总额：</span>
          <span class="box-price">¥ {{ cartStore.totalAmount.toFixed(2) }}</span>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="checkoutVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="submitting"
            @click="handleSubmitOrder"
          >
            提交订单并前往支付
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- Simulated Payment Dialog -->
    <el-dialog
      v-model="payVisible"
      title="收银台 · 模拟在线支付"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
      class="pay-dialog"
    >
      <div class="pay-dialog-content">
        <div class="pay-amount-box">
          <div class="pay-label">支付金额</div>
          <div class="pay-money">
            <span class="symbol">¥</span>
            <span class="val">{{ currentOrder?.totalAmount?.toFixed(2) }}</span>
          </div>
          <div class="pay-order-no">
            订单编号：<strong>{{ currentOrder?.orderNo }}</strong>
          </div>
        </div>

        <div class="pay-methods">
          <div class="method-card is-active">
            <el-icon class="method-icon"><WalletFilled /></el-icon>
            <div class="method-name">模拟快捷支付通道</div>
            <el-tag type="success" size="small">极速免密</el-tag>
          </div>
        </div>

        <div class="pay-tip">
          提示：本项目为课程作业演示系统，采用教学模拟支付流转机制，点击下方按钮即可完成真实状态流转。
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleLaterPay">稍后支付</el-button>
          <el-button
            type="success"
            size="large"
            :loading="paying"
            @click="handleConfirmPay"
          >
            确认模拟支付 ¥ {{ currentOrder?.totalAmount?.toFixed(2) }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  ShoppingCart,
  ArrowLeft,
  Delete,
  Picture,
  WalletFilled
} from '@element-plus/icons-vue'
import { useCartStore } from '@/stores/cart'
import { useUserStore } from '@/stores/user'
import { payOrderApi, type OrderCheckoutResult } from '@/api/order'

const router = useRouter()
const cartStore = useCartStore()
const userStore = useUserStore()

const checkoutVisible = ref(false)
const payVisible = ref(false)
const submitting = ref(false)
const paying = ref(false)
const checkoutFormRef = ref<FormInstance>()
const currentOrder = ref<OrderCheckoutResult | null>(null)

const checkoutForm = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: ''
})

const checkoutRules: FormRules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
    { min: 2, max: 32, message: '姓名长度需在 2 到 32 个字符之间', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { min: 7, max: 20, message: '请输入有效的电话号码', trigger: 'blur' }
  ],
  receiverAddress: [
    { required: true, message: '请输入详细送货地址', trigger: 'blur' },
    { min: 5, max: 200, message: '地址描述至少 5 个字符', trigger: 'blur' }
  ]
}

const checkedItems = computed(() => {
  return cartStore.items.filter((item) => item.checked && item.productStatus === 1)
})

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录账号后再查看购物车')
    router.push('/login?redirect=/cart')
    return
  }
  await cartStore.fetchCart()

  // 默认填充用户昵称与常用地址示例
  if (userStore.user) {
    checkoutForm.receiverName = userStore.user.nickname || userStore.user.username || ''
    checkoutForm.receiverPhone = '13800138000'
    checkoutForm.receiverAddress = '北京市海淀区中关村南大街1号 信息科学实验楼 A座 402'
  }
})

function goToProduct(productId: number) {
  router.push(`/product/${productId}`)
}

function onToggleItem(itemId: number, val: any) {
  handleToggleItem(itemId, Boolean(val))
}

function onQuantityChange(itemId: number, val: any) {
  handleQuantityChange(itemId, Number(val))
}

async function handleCheckAll(val: any) {
  const checked = Boolean(val)
  try {
    await cartStore.toggleCheckAll(checked)
  } catch (err: any) {
    ElMessage.error(err.message || '全选操作失败')
  }
}

async function handleToggleItem(itemId: number, checked: boolean) {
  try {
    await cartStore.toggleCheck(itemId, checked)
  } catch (err: any) {
    ElMessage.error(err.message || '切换勾选失败')
  }
}

async function handleQuantityChange(itemId: number, quantity: number) {
  if (!quantity || quantity < 1) return
  try {
    await cartStore.updateQuantity(itemId, quantity)
  } catch (err: any) {
    ElMessage.error(err.message || '修改数量失败')
  }
}

async function handleRemoveItem(itemId: number) {
  try {
    await cartStore.removeItem(itemId)
    ElMessage.success('已从购物车移除该商品')
  } catch (err: any) {
    ElMessage.error(err.message || '移除失败')
  }
}

function handleClearCart() {
  ElMessageBox.confirm('确定要清空购物车中的所有商品吗？', '提示', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await cartStore.clearCart()
      ElMessage.success('购物车已清空')
    } catch (err: any) {
      ElMessage.error(err.message || '清空失败')
    }
  }).catch(() => {})
}

function openCheckoutDialog() {
  if (cartStore.checkedCount === 0) {
    ElMessage.warning('请至少勾选一件在售商品进行结算')
    return
  }
  checkoutVisible.value = true
}

async function handleSubmitOrder() {
  if (!checkoutFormRef.value) return
  const valid = await checkoutFormRef.value.validate()
  if (!valid) return

  submitting.value = true
  try {
    const result = await cartStore.checkout({
      receiverName: checkoutForm.receiverName,
      receiverPhone: checkoutForm.receiverPhone,
      receiverAddress: checkoutForm.receiverAddress
    })
    ElMessage.success('订单生成成功，正在进入收银台...')
    checkoutVisible.value = false
    currentOrder.value = result
    payVisible.value = true
  } catch (err: any) {
    ElMessage.error(err.message || '订单提交失败')
  } finally {
    submitting.value = false
  }
}

async function handleConfirmPay() {
  if (!currentOrder.value) return
  paying.value = true
  try {
    await payOrderApi(currentOrder.value.orderNo)
    ElMessage.success('支付成功！商品将尽快安排发货')
    payVisible.value = false
    router.push('/orders')
  } catch (err: any) {
    ElMessage.error(err.message || '支付失败')
  } finally {
    paying.value = false
  }
}

function handleLaterPay() {
  payVisible.value = false
  ElMessage.info('订单已保存至您的“我的订单”中，可随时完成支付')
  router.push('/orders')
}
</script>

<style scoped>
.cart-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

.cart-header-wrapper {
  margin-bottom: 20px;
}

.cart-header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  font-size: 26px;
  color: #2563eb;
}

.title-cell h2 {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.item-count-badge {
  font-size: 13px;
  color: #6b7280;
  background-color: #f3f4f6;
  padding: 2px 10px;
  border-radius: 9999px;
}

.actions-cell {
  display: flex;
  align-items: center;
  gap: 16px;
}

.empty-card {
  border-radius: 12px;
  padding: 60px 0;
  text-align: center;
}

.cart-table-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 0;
  overflow: hidden;
}

.cart-table-header {
  display: flex;
  align-items: center;
  background-color: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 20px;
  font-size: 14px;
  color: #6b7280;
  font-weight: 600;
}

.col-check {
  width: 70px;
}

.col-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 16px;
}

.col-price {
  width: 140px;
  text-align: right;
  font-size: 14px;
}

.col-qty {
  width: 160px;
  text-align: center;
}

.col-subtotal {
  width: 150px;
  text-align: right;
  font-size: 15px;
}

.col-action {
  width: 100px;
  text-align: right;
}

.cart-item-rows {
  divide-y: 1px solid #f3f4f6;
}

.cart-row {
  display: flex;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f3f4f6;
  transition: background-color 0.15s ease;
}

.cart-row:hover {
  background-color: #fafbfc;
}

.cart-row.is-off-shelf {
  opacity: 0.6;
  background-color: #fef2f2;
}

.item-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  cursor: pointer;
  flex-shrink: 0;
}

.image-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f3f4f6;
  color: #9ca3af;
  font-size: 24px;
}

.item-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item-name {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  cursor: pointer;
  transition: color 0.2s;
  line-height: 1.4;
}

.item-name:hover {
  color: #2563eb;
}

.item-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stock-text {
  font-size: 12px;
  color: #9ca3af;
}

.price-symbol,
.subtotal-symbol {
  font-size: 12px;
  color: #4b5563;
  margin-right: 2px;
}

.price-val {
  font-weight: 500;
  color: #374151;
}

.subtotal-val {
  font-weight: 700;
  color: #dc2626;
  font-size: 16px;
}

/* Sticky Checkout Bar */
.checkout-bar {
  position: sticky;
  bottom: 20px;
  margin-top: 24px;
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
  z-index: 10;
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  color: #4b5563;
}

.divider {
  color: #d1d5db;
}

.highlight-count {
  color: #2563eb;
  font-size: 16px;
}

.bar-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.total-wrap {
  display: flex;
  align-items: baseline;
}

.total-label {
  font-size: 14px;
  color: #4b5563;
}

.currency-symbol {
  font-size: 16px;
  font-weight: 700;
  color: #dc2626;
  margin-left: 6px;
}

.total-amount {
  font-size: 26px;
  font-weight: 800;
  color: #dc2626;
  letter-spacing: -0.5px;
}

.checkout-btn {
  padding: 0 36px;
  font-size: 16px;
  font-weight: 600;
  height: 48px;
  border-radius: 8px;
}

/* Checkout Dialog */
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 12px;
}

.checkout-items-preview {
  max-height: 200px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-right: 6px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background-color: #f9fafb;
  padding: 8px 12px;
  border-radius: 8px;
}

.preview-img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  flex-shrink: 0;
}

.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-meta {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.preview-price {
  font-size: 14px;
  font-weight: 600;
  color: #dc2626;
}

.checkout-summary-box {
  background-color: #fef2f2;
  border: 1px solid #fee2e2;
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  margin-top: 16px;
}

.box-label {
  font-size: 14px;
  color: #991b1b;
}

.box-price {
  font-size: 20px;
  font-weight: 800;
  color: #dc2626;
  margin-left: 8px;
}

/* Pay Dialog */
.pay-amount-box {
  text-align: center;
  padding: 16px 0;
  border-bottom: 1px dashed #e5e7eb;
}

.pay-label {
  font-size: 14px;
  color: #6b7280;
}

.pay-money {
  margin: 8px 0;
  color: #059669;
}

.pay-money .symbol {
  font-size: 20px;
  font-weight: 700;
}

.pay-money .val {
  font-size: 36px;
  font-weight: 800;
}

.pay-order-no {
  font-size: 13px;
  color: #4b5563;
}

.pay-methods {
  margin: 20px 0;
}

.method-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: 10px;
  border: 2px solid #10b981;
  background-color: #ecfdf5;
}

.method-icon {
  font-size: 28px;
  color: #059669;
}

.method-name {
  flex: 1;
  font-size: 15px;
  font-weight: 600;
  color: #065f46;
}

.pay-tip {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
  background-color: #f9fafb;
  padding: 10px 14px;
  border-radius: 6px;
}
</style>
