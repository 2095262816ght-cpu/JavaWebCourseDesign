<template>
  <div class="cart-page page-container">
    <!-- 页头 -->
    <header class="cart-head">
      <div>
        <span class="eyebrow">SHOPPING CART</span>
        <h1 class="display-lg cart-title">购物车</h1>
        <p class="cart-sub">
          <template v-if="cartStore.cartCount > 0">
            共 <span class="mono accent">{{ cartStore.cartCount }}</span> 件商品待结算
          </template>
          <template v-else>还没有挑选任何商品</template>
        </p>
      </div>
      <div class="head-actions">
        <button class="btn btn--ghost btn--sm" type="button" @click="$router.push('/products')">
          <el-icon><ArrowLeft /></el-icon>
          继续挑选
        </button>
        <button
          v-if="cartStore.items.length > 0"
          class="btn btn--ghost btn--sm is-danger"
          type="button"
          @click="handleClearCart"
        >
          <el-icon><Delete /></el-icon>
          清空购物车
        </button>
      </div>
    </header>

    <!-- 空态 -->
    <div v-if="cartStore.items.length === 0" class="cart-empty">
      <span class="empty-orb"></span>
      <h2 class="empty-title">购物车空空如也</h2>
      <p class="empty-desc">去商品中心逛逛，把心仪的好物放进来</p>
      <button class="btn btn--primary btn--sm" type="button" @click="$router.push('/products')">
        前往商品中心
      </button>
    </div>

    <!-- 列表 -->
    <template v-else>
      <div class="cart-panel">
        <div class="panel-head">
          <el-checkbox :model-value="cartStore.isAllChecked" @change="handleCheckAll">
            全选
          </el-checkbox>
          <span class="head-label">商品信息</span>
          <span class="head-label is-center">单价</span>
          <span class="head-label is-center">数量</span>
          <span class="head-label is-right">小计</span>
          <span></span>
        </div>

        <div class="cart-rows">
          <div
            v-for="item in cartStore.items"
            :key="item.id"
            class="cart-row"
            :class="{ 'is-invalid': item.productStatus === 0 || (item.stock ?? 0) <= 0 }"
          >
            <div class="row-check">
              <el-checkbox
                :disabled="!item.checked && (item.productStatus === 0 || (item.stock ?? 0) <= 0 || (item.stock !== undefined && item.quantity > item.stock))"
                :model-value="item.checked"
                @change="onToggleItem(item.id, $event)"
              />
            </div>

            <div class="row-product">
              <button class="thumb-btn" type="button" @click="goToProduct(item.productId)">
                <el-image :src="item.productImage" fit="cover" class="item-img">
                  <template #error>
                    <span class="img-fallback"><el-icon><Picture /></el-icon></span>
                  </template>
                </el-image>
              </button>

              <div class="item-meta">
                <button class="item-name" type="button" @click="goToProduct(item.productId)">
                  {{ item.productName }}
                </button>
                <div class="item-tags">
                  <span v-if="item.productStatus === 0" class="status-chip is-danger">已下架</span>
                  <span v-else-if="(item.stock ?? 0) <= 0" class="status-chip is-danger">已售罄</span>
                  <span
                    v-else-if="item.stock !== undefined && item.quantity > item.stock"
                    class="status-chip is-warn"
                  >
                    库存不足 · 仅剩 {{ item.stock }}
                  </span>
                  <span v-else-if="item.stock <= 5" class="status-chip is-warn">仅剩 {{ item.stock }} 件</span>
                  <span v-else class="status-chip">库存 {{ item.stock }}</span>
                </div>
              </div>
            </div>

            <div class="row-price mono">
              <span class="unit">¥{{ Number(item.price ?? 0).toFixed(2) }}</span>
            </div>

            <div class="row-qty">
              <el-input-number
                :model-value="item.quantity"
                :min="1"
                :max="Math.max(1, item.stock ?? 0)"
                :disabled="item.productStatus === 0 || (item.stock ?? 0) <= 0 || updatingItemIds.has(item.id)"
                size="small"
                @change="onQuantityChange(item.id, $event)"
              />
            </div>

            <div class="row-subtotal price">
              <span class="symbol">¥</span>
              <span class="amount">{{ Number(item.subtotal ?? 0).toFixed(2) }}</span>
            </div>

            <div class="row-action">
              <el-popconfirm
                title="确认从购物车移除该商品吗？"
                confirm-button-text="确定"
                cancel-button-text="取消"
                @confirm="handleRemoveItem(item.id)"
              >
                <template #reference>
                  <button class="remove-btn" type="button" aria-label="移除">
                    <el-icon><Delete /></el-icon>
                  </button>
                </template>
              </el-popconfirm>
            </div>
          </div>
        </div>
      </div>

      <!-- 结算条 -->
      <div class="checkout-bar">
        <div class="bar-left">
          <el-checkbox :model-value="cartStore.isAllChecked" @change="handleCheckAll">
            全选
          </el-checkbox>
          <span class="bar-divider"></span>
          <span class="selected-text">
            已选 <span class="mono accent">{{ cartStore.checkedCount }}</span> 件
          </span>
        </div>

        <div class="bar-right">
          <div class="total-wrap">
            <span class="total-label">合计</span>
            <span class="price">
              <span class="symbol">¥</span>
              <span class="amount">{{ Number(cartStore.totalAmount ?? 0).toFixed(2) }}</span>
            </span>
          </div>
          <button
            class="btn btn--primary checkout-btn"
            type="button"
            :disabled="cartStore.checkedCount === 0"
            @click="openCheckoutDialog"
          >
            去结算 ({{ cartStore.checkedCount }})
            <el-icon><Right /></el-icon>
          </button>
        </div>
      </div>
    </template>

    <!-- 结算弹窗 -->
    <el-dialog v-model="checkoutVisible" title="确认订单并结算" width="620px" destroy-on-close>
      <div class="dialog-block">
        <p class="dialog-label mono">ITEMS / {{ cartStore.checkedCount }}</p>
        <div class="preview-list">
          <div v-for="item in checkedItems" :key="item.id" class="preview-item">
            <el-image :src="item.productImage" class="preview-img" fit="cover" />
            <div class="preview-info">
              <p class="preview-name">{{ item.productName }}</p>
              <p class="preview-meta mono">¥{{ Number(item.price ?? 0).toFixed(2) }} × {{ item.quantity }}</p>
            </div>
            <span class="preview-price mono">¥{{ Number(item.subtotal ?? 0).toFixed(2) }}</span>
          </div>
        </div>
      </div>

      <div class="dialog-block">
        <p class="dialog-label mono">SHIPPING</p>
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
                <el-input v-model="checkoutForm.receiverName" placeholder="例如：张同学" maxlength="32" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="receiverPhone">
                <el-input v-model="checkoutForm.receiverPhone" placeholder="例如：13800138000" maxlength="20" clearable />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="详细送货地址" prop="receiverAddress">
            <el-input
              v-model="checkoutForm.receiverAddress"
              type="textarea"
              :rows="2"
              placeholder="例如：北京市海淀区中关村南大街 1 号 3 号楼 402 室"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>

      <div class="dialog-total">
        <span>应付总额</span>
        <span class="price">
          <span class="symbol">¥</span>
          <span class="amount">{{ Number(cartStore.totalAmount ?? 0).toFixed(2) }}</span>
        </span>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <button class="btn btn--ghost btn--sm" type="button" @click="checkoutVisible = false">取消</button>
          <button class="btn btn--primary btn--sm" type="button" :disabled="submitting" @click="handleSubmitOrder">
            {{ submitting ? '正在提交…' : '提交订单并前往支付' }}
          </button>
        </div>
      </template>
    </el-dialog>

    <!-- 支付弹窗 -->
    <el-dialog
      v-model="payVisible"
      title="收银台"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="pay-box">
        <p class="pay-label">支付金额</p>
        <p class="pay-money mono">
          <span class="symbol">¥</span>{{ Number(currentOrder?.totalAmount ?? 0).toFixed(2) }}
        </p>
        <p class="pay-no mono">订单号 {{ currentOrder?.orderNo }}</p>

        <div class="pay-method">
          <span class="method-icon"><el-icon><WalletFilled /></el-icon></span>
          <div class="method-info">
            <p class="method-name">在线支付</p>
            <p class="method-desc">支持微信 / 支付宝 / 银行卡</p>
          </div>
          <span class="method-badge">推荐</span>
        </div>

        <p class="pay-tip">
          支付完成后我们将尽快为你安排发货，可在「我的订单」中查看物流进度。
        </p>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <button class="btn btn--ghost btn--sm" type="button" @click="handleLaterPay">稍后支付</button>
          <button class="btn btn--primary btn--sm" type="button" :disabled="paying" @click="handleConfirmPay">
            {{ paying ? '支付中…' : `确认支付 ¥${Number(currentOrder?.totalAmount ?? 0).toFixed(2)}` }}
          </button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft, Delete, Picture, Right, WalletFilled } from '@element-plus/icons-vue'
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
// 记录正在提交数量修改的购物车项，避免连点或快速改数导致重复请求
const updatingItemIds = ref<Set<number>>(new Set())

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
  return cartStore.items.filter(
    (item) => item.checked && item.productStatus === 1 && (item.stock ?? 0) > 0
  )
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
    console.error('全选操作失败', err)
  }
}

async function handleToggleItem(itemId: number, checked: boolean) {
  try {
    await cartStore.toggleCheck(itemId, checked)
  } catch (err: any) {
    console.error('切换勾选失败', err)
  }
}

async function handleQuantityChange(itemId: number, quantity: number) {
  if (!quantity || quantity < 1 || updatingItemIds.value.has(itemId)) return
  updatingItemIds.value.add(itemId)
  try {
    await cartStore.updateQuantity(itemId, quantity)
  } catch (err: any) {
    console.error('修改数量失败', err)
  } finally {
    updatingItemIds.value.delete(itemId)
  }
}

async function handleRemoveItem(itemId: number) {
  try {
    await cartStore.removeItem(itemId)
    ElMessage.success('已从购物车移除该商品')
  } catch (err: any) {
    console.error('移除失败', err)
  }
}

function handleClearCart() {
  ElMessageBox.confirm('确定要清空购物车中的所有商品吗？', '提示', {
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(async () => {
      try {
        await cartStore.clearCart()
        ElMessage.success('购物车已清空')
      } catch (err: any) {
        console.error('清空失败', err)
      }
    })
    .catch(() => {})
}

function openCheckoutDialog() {
  const hasInvalidChecked = cartStore.items.some(
    (item) => item.checked && (item.productStatus !== 1 || (item.stock ?? 0) <= 0)
  )
  if (hasInvalidChecked) {
    ElMessage.warning('存在已下架或已售罄的勾选商品，请先取消勾选或移除后再结算')
    return
  }
  const hasOverStockChecked = cartStore.items.some(
    (item) => item.checked && item.stock !== undefined && item.quantity > item.stock
  )
  if (hasOverStockChecked) {
    ElMessage.warning('存在加购数量超过当前库存的商品，请先调减数量或取消勾选后再结算')
    return
  }
  if (cartStore.checkedCount === 0) {
    ElMessage.warning('请至少勾选一件在售商品进行结算')
    return
  }
  checkoutVisible.value = true
}

async function handleSubmitOrder() {
  if (!checkoutFormRef.value) return
  try {
    const valid = await checkoutFormRef.value.validate()
    if (!valid) return
  } catch {
    return
  }

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
    console.error('订单提交失败', err)
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
    console.error('支付失败', err)
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
.cart-page {
  padding-top: 28px;
  padding-bottom: 40px;
}

/* 页头 */
.cart-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 12px 0 34px;
  flex-wrap: wrap;
}

.cart-title {
  margin-top: 12px;
}

.cart-sub {
  margin-top: 10px;
  font-size: 13px;
  color: var(--text-3);
}

.accent {
  color: var(--cyan);
  font-weight: 700;
}

.head-actions {
  display: flex;
  gap: 10px;
}

.is-danger:hover {
  border-color: rgba(255, 92, 122, 0.5);
  color: #ff9db0;
}

/* 空态 */
.cart-empty {
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 110px 24px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-xl);
  text-align: center;
}

.empty-orb {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: radial-gradient(circle at 40% 35%, rgba(124, 92, 255, 0.6), rgba(34, 211, 238, 0.1) 62%, transparent 74%);
  filter: blur(8px);
  margin-bottom: 8px;
}

.empty-title {
  font-size: 22px;
  font-weight: 700;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-3);
  margin-bottom: 10px;
}

/* 列表面板 */
.cart-panel {
  border: 1px solid var(--line);
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.025);
  overflow: hidden;
}

.panel-head {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 120px 160px 140px 60px;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.025);
}

.head-label {
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-4);
}

.head-label.is-center {
  text-align: center;
}

.head-label.is-right {
  text-align: right;
}

.cart-row {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) 120px 160px 140px 60px;
  align-items: center;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line);
  transition: background 0.3s var(--ease);
}

.cart-row:last-child {
  border-bottom: none;
}

.cart-row:hover {
  background: rgba(255, 255, 255, 0.03);
}

.cart-row.is-invalid {
  opacity: 0.6;
}

.row-check {
  display: flex;
  align-items: center;
}

.row-product {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.thumb-btn {
  padding: 0;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: #0b0e18;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  transition: border-color 0.3s var(--ease);
}

.thumb-btn:hover {
  border-color: rgba(124, 92, 255, 0.6);
}

.item-img {
  width: 74px;
  height: 74px;
  display: block;
}

.img-fallback {
  width: 100%;
  height: 100%;
  display: grid;
  place-items: center;
  color: var(--text-4);
}

.item-meta {
  min-width: 0;
}

.item-name {
  display: block;
  padding: 0;
  border: none;
  background: none;
  text-align: left;
  font-family: var(--font-body);
  font-size: 14.5px;
  font-weight: 600;
  color: var(--text);
  cursor: pointer;
  line-height: 1.45;
  transition: color 0.25s var(--ease);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

.item-name:hover {
  color: var(--cyan);
}

.item-tags {
  margin-top: 8px;
}

.status-chip {
  display: inline-block;
  padding: 2px 9px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.05);
  font-size: 10.5px;
  color: var(--text-3);
}

.status-chip.is-warn {
  border-color: rgba(251, 191, 36, 0.35);
  background: rgba(251, 191, 36, 0.1);
  color: #fcd77a;
}

.status-chip.is-danger {
  border-color: rgba(255, 92, 122, 0.35);
  background: rgba(255, 92, 122, 0.1);
  color: #ff9db0;
}

.row-price {
  text-align: right;
  font-size: 13px;
  color: var(--text-2);
}

.row-qty {
  display: flex;
  justify-content: center;
}

.row-subtotal {
  justify-content: flex-end;
}

.row-subtotal .amount {
  font-size: 20px;
}

.row-action {
  display: flex;
  justify-content: flex-end;
}

.remove-btn {
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: transparent;
  color: var(--text-4);
  cursor: pointer;
  transition: all 0.28s var(--ease);
}

.remove-btn:hover {
  border-color: rgba(255, 92, 122, 0.5);
  background: rgba(255, 92, 122, 0.1);
  color: #ff9db0;
}

/* 结算条 */
.checkout-bar {
  position: sticky;
  bottom: 20px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-top: 22px;
  padding: 14px 14px 14px 24px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 999px;
  background: rgba(10, 12, 20, 0.86);
  backdrop-filter: blur(26px) saturate(180%);
  -webkit-backdrop-filter: blur(26px) saturate(180%);
  box-shadow: 0 34px 70px -34px rgba(0, 0, 0, 1), 0 0 0 1px rgba(124, 92, 255, 0.1);
}

.bar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 13px;
  color: var(--text-2);
}

.bar-divider {
  width: 1px;
  height: 16px;
  background: var(--line-strong);
}

.bar-right {
  display: flex;
  align-items: center;
  gap: 22px;
}

.total-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.total-label {
  font-size: 12px;
  color: var(--text-3);
}

.total-wrap .price .amount {
  font-size: 28px;
}

.checkout-btn {
  height: 48px;
  padding: 0 30px;
}

.checkout-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 弹窗 */
.dialog-block {
  margin-bottom: 22px;
}

.dialog-label {
  font-size: 10px;
  letter-spacing: 0.2em;
  color: var(--cyan);
  margin-bottom: 12px;
}

.preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 4px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.03);
}

.preview-img {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  flex-shrink: 0;
}

.preview-info {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-size: 13px;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-meta {
  margin-top: 3px;
  font-size: 11px;
  color: var(--text-4);
}

.preview-price {
  font-size: 13px;
  color: var(--price);
}

.checkout-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.dialog-total {
  display: flex;
  align-items: baseline;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border: 1px solid rgba(255, 92, 122, 0.24);
  border-radius: var(--radius);
  background: rgba(255, 92, 122, 0.06);
  font-size: 13px;
  color: var(--text-2);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
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
  margin: 26px 0 16px;
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

.pay-tip {
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.03);
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-4);
  text-align: left;
}

/* 响应式 */
@media (max-width: 980px) {
  .panel-head {
    display: none;
  }

  .cart-row {
    grid-template-columns: 44px minmax(0, 1fr) auto;
    grid-template-areas:
      'check product product'
      'check price qty'
      'check subtotal action';
    gap: 10px 14px;
    padding: 16px;
  }

  .row-check {
    grid-area: check;
    align-items: flex-start;
    padding-top: 4px;
  }

  .row-product {
    grid-area: product;
  }

  .row-price {
    grid-area: price;
    text-align: left;
  }

  .row-qty {
    grid-area: qty;
    justify-content: flex-end;
  }

  .row-subtotal {
    grid-area: subtotal;
    justify-content: flex-start;
  }

  .row-action {
    grid-area: action;
  }
}

@media (max-width: 600px) {
  .checkout-bar {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
    border-radius: var(--radius-lg);
    padding: 16px;
  }

  .bar-right {
    justify-content: space-between;
  }

  .checkout-btn {
    padding: 0 20px;
  }

  .cart-row {
    grid-template-columns: 40px minmax(0, 1fr);
    grid-template-areas:
      'check product'
      'price qty'
      'subtotal action';
  }

  .row-price {
    padding-left: 0;
  }
}
</style>
