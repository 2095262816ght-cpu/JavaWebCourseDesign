<template>
  <div class="product-mgmt-container">
    <!-- Header Card -->
    <div class="page-header">
      <div>
        <h2 class="title">商品中心管理</h2>
        <p class="subtitle">统一维护平台在售商品规格、品牌分类归属、图片画廊与实时上下架状态</p>
      </div>
      <el-button type="primary" :icon="Plus" @click="openCreateDialog">
        录入新商品
      </el-button>
    </div>

    <!-- Search & Filter Card -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="商品名称">
          <el-input
            v-model="queryForm.name"
            placeholder="输入商品名检索"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
        </el-form-item>

        <el-form-item label="所属品牌">
          <el-select
            v-model="queryForm.brandId"
            placeholder="全部品牌"
            clearable
            filterable
            class="filter-select"
            @change="handleSearch"
          >
            <el-option
              v-for="b in brandList"
              :key="b.id"
              :label="b.name"
              :value="b.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="所属分类">
          <el-select
            v-model="queryForm.categoryId"
            placeholder="全部分类"
            clearable
            class="filter-select"
            @change="handleSearch"
          >
            <el-option
              v-for="c in flatCategories"
              :key="c.id"
              :label="c.depth > 0 ? '　'.repeat(c.depth) + '└ ' + c.name : c.name"
              :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="上下架状态">
          <el-select
            v-model="queryForm.status"
            placeholder="全部状态"
            clearable
            class="status-select"
            @change="handleSearch"
          >
            <el-option label="全部" :value="undefined" />
            <el-option label="在售上架" :value="1" />
            <el-option label="下架禁售" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table Card -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="productList"
        v-loading="loading"
        stripe
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="商品封面" width="100" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.mainImage"
              fit="cover"
              class="table-thumb"
              :preview-src-list="[row.mainImage, ...(row.subImages || [])]"
              preview-teleported
            >
              <template #error>
                <div class="thumb-err">无图</div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column label="商品基本信息" min-width="260">
          <template #default="{ row }">
            <div class="prod-cell">
              <span class="prod-name" :title="row.name">{{ row.name }}</span>
              <span class="prod-sub" :title="row.subtitle">{{ row.subtitle || '暂无副标题' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="品牌与分类" width="180">
          <template #default="{ row }">
            <div class="tag-cell">
              <el-tag size="small" type="primary">{{ row.brandName }}</el-tag>
              <el-tag size="small" type="info">{{ row.categoryName }}</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="价格体系" width="150" align="right">
          <template #default="{ row }">
            <div class="price-cell">
              <span class="price-now">¥{{ Number(row.price).toFixed(2) }}</span>
              <span
                class="price-old"
                v-if="row.originalPrice && row.originalPrice > row.price"
              >
                ¥{{ Number(row.originalPrice).toFixed(2) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="可用库存" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.stock > 10 ? 'success' : 'danger'"
              size="small"
              round
            >
              {{ row.stock }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="上下架状态" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              inline-prompt
              active-text="在售"
              inactive-text="下架"
              @change="() => handleStatusChange(row)"
            />
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="录入时间" width="160" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="管理操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-divider direction="vertical" />
            <el-popconfirm
              title="确定要删除此商品吗？删除后前台将不可见。"
              confirm-button-text="确认删除"
              cancel-button-text="取消"
              confirm-button-type="danger"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="queryForm.pageNum"
          :page-size="queryForm.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑商品档案' : '录入新商品'"
      width="780px"
      top="6vh"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
        class="prod-dialog-form"
      >
        <el-row :gutter="16">
          <el-col :span="24">
            <el-form-item label="商品名称" prop="name">
              <el-input
                v-model="formData.name"
                placeholder="请输入完整商品名称及规格型号"
                maxlength="128"
                show-word-limit
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="副标题" prop="subtitle">
              <el-input
                v-model="formData.subtitle"
                placeholder="请输入卖点简述或促销宣传语"
                maxlength="255"
                show-word-limit
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="所属品牌" prop="brandId">
              <el-select
                v-model="formData.brandId"
                placeholder="选择关联品牌"
                filterable
                class="w-full"
              >
                <el-option
                  v-for="b in brandList"
                  :key="b.id"
                  :label="b.name"
                  :value="b.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select
                v-model="formData.categoryId"
                placeholder="选择关联商品分类"
                class="w-full"
              >
                <el-option
                  v-for="c in flatCategories"
                  :key="c.id"
                  :label="c.depth > 0 ? '　'.repeat(c.depth) + '└ ' + c.name : c.name"
                  :value="c.id"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="销售价格 (¥)" prop="price">
              <el-input-number
                v-model="formData.price"
                :min="0"
                :precision="2"
                :step="10"
                class="w-full"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="划线原价 (¥)" prop="originalPrice">
              <el-input-number
                v-model="formData.originalPrice"
                :min="0"
                :precision="2"
                :step="10"
                class="w-full"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="可用库存" prop="stock">
              <el-input-number
                v-model="formData.stock"
                :min="0"
                :step="1"
                class="w-full"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="初始状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio :label="1">立即上架在售</el-radio>
                <el-radio :label="0">暂存为下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <!-- Main Image Upload with StorageService -->
          <el-col :span="24">
            <el-form-item label="商品主图" prop="mainImage">
              <div class="image-uploader-cell">
                <el-input
                  v-model="formData.mainImage"
                  placeholder="可直接输入图片URL，或点击右侧上传"
                >
                  <template #append>
                    <el-upload
                      :show-file-list="false"
                      :http-request="handleUploadMainImage"
                      accept="image/*"
                    >
                      <el-button :icon="Upload">本地上传</el-button>
                    </el-upload>
                  </template>
                </el-input>
                <div class="image-preview" v-if="formData.mainImage">
                  <el-image :src="formData.mainImage" fit="contain" class="preview-box" />
                  <span class="preview-tip">主图预览</span>
                </div>
              </div>
            </el-form-item>
          </el-col>

          <!-- Sub-Images Gallery -->
          <el-col :span="24">
            <el-form-item label="轮播副图画廊">
              <div class="sub-images-wrap">
                <div
                  v-for="(subImg, index) in subImagesList"
                  :key="index"
                  class="sub-img-item"
                >
                  <el-image :src="subImg" fit="cover" class="sub-img-thumb" />
                  <div class="sub-img-actions">
                    <el-button
                      type="danger"
                      circle
                      size="small"
                      :icon="Delete"
                      @click="removeSubImage(index)"
                    />
                  </div>
                </div>

                <el-upload
                  :show-file-list="false"
                  :http-request="handleUploadSubImage"
                  accept="image/*"
                  class="sub-img-uploader"
                >
                  <div class="upload-trigger-box">
                    <el-icon><Plus /></el-icon>
                    <span>添加副图</span>
                  </div>
                </el-upload>
              </div>
              <div class="gallery-manual-add">
                <el-input
                  v-model="manualSubImgUrl"
                  placeholder="或粘贴副图网络图片URL"
                  size="small"
                >
                  <template #append>
                    <el-button @click="addManualSubImage">添加</el-button>
                  </template>
                </el-input>
              </div>
            </el-form-item>
          </el-col>

          <!-- Detail Rich Text -->
          <el-col :span="24">
            <el-form-item label="商品图文详情" prop="detailHtml">
              <el-input
                v-model="formData.detailHtml"
                type="textarea"
                :rows="6"
                placeholder="支持富文本 HTML，例如 <p>商品特色规格说明</p>"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确认保存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import {
  Plus,
  Search,
  Refresh,
  Upload,
  Delete
} from '@element-plus/icons-vue'
import {
  listAdminProductsApi,
  createProductApi,
  updateProductApi,
  updateProductStatusApi,
  deleteProductApi,
  type ProductAdminVO,
  type AdminProductQuery,
  type ProductCommand
} from '@/api/product'
import { getFeaturedBrandsApi, type Brand } from '@/api/brand'
import { getPortalCategoryTreeApi, type CategoryNode } from '@/api/category'
import request from '@/api/request'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const total = ref(0)

const productList = ref<ProductAdminVO[]>([])
const brandList = ref<Brand[]>([])
const categoryTree = ref<CategoryNode[]>([])

const queryForm = reactive<AdminProductQuery>({
  name: '',
  brandId: undefined,
  categoryId: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const formRef = ref<FormInstance>()
const formData = reactive<ProductCommand>({
  id: undefined,
  name: '',
  subtitle: '',
  brandId: undefined as any,
  categoryId: undefined as any,
  mainImage: '',
  price: 0,
  originalPrice: 0,
  stock: 100,
  status: 1,
  detailHtml: ''
})

const subImagesList = ref<string[]>([])
const manualSubImgUrl = ref('')

const formRules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  brandId: [{ required: true, message: '请选择关联品牌', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择关联分类', trigger: 'change' }],
  mainImage: [{ required: true, message: '请提供商品主图URL或上传图片', trigger: 'blur' }],
  price: [{ required: true, message: '请输入销售价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入可用库存', trigger: 'blur' }]
}

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

async function loadMetadata() {
  try {
    const [brands, cats] = await Promise.all([
      getFeaturedBrandsApi(100),
      getPortalCategoryTreeApi()
    ])
    brandList.value = brands || []
    categoryTree.value = cats || []
  } catch (err) {
    console.error('加载品牌与分类列表失败', err)
  }
}

async function fetchProducts() {
  loading.value = true
  try {
    const res = await listAdminProductsApi(queryForm)
    if (res) {
      productList.value = res.records || []
      total.value = res.total || 0
    }
  } catch (err) {
    console.error('加载后台商品列表异常', err)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryForm.pageNum = 1
  fetchProducts()
}

function handleReset() {
  queryForm.name = ''
  queryForm.brandId = undefined
  queryForm.categoryId = undefined
  queryForm.status = undefined
  queryForm.pageNum = 1
  fetchProducts()
}

function handleSizeChange(size: number) {
  queryForm.pageSize = size
  queryForm.pageNum = 1
  fetchProducts()
}

function handlePageChange(page: number) {
  queryForm.pageNum = page
  fetchProducts()
}

async function handleStatusChange(row: ProductAdminVO) {
  const val = row.status
  try {
    await updateProductStatusApi(row.id, val)
    ElMessage.success(`商品【${row.name}】已${val === 1 ? '上架销售' : '下架禁售'}`)
  } catch (err) {
    // 恢复原有开关状态
    row.status = val === 1 ? 0 : 1
    console.error('切换状态失败', err)
  }
}

async function handleDelete(id: number) {
  try {
    await deleteProductApi(id)
    ElMessage.success('商品已成功删除')
    fetchProducts()
  } catch (err) {
    console.error('删除商品失败', err)
  }
}

function openCreateDialog() {
  isEdit.value = false
  formData.id = undefined
  formData.name = ''
  formData.subtitle = ''
  formData.brandId = undefined as any
  formData.categoryId = undefined as any
  formData.mainImage = ''
  formData.price = 99.00
  formData.originalPrice = 129.00
  formData.stock = 50
  formData.status = 1
  formData.detailHtml = '<p>全新正品旗舰力作，欢迎选购！</p>'
  subImagesList.value = []
  manualSubImgUrl.value = ''
  dialogVisible.value = true
}

function openEditDialog(row: ProductAdminVO) {
  isEdit.value = true
  formData.id = row.id
  formData.name = row.name
  formData.subtitle = row.subtitle
  formData.brandId = row.brandId
  formData.categoryId = row.categoryId
  formData.mainImage = row.mainImage
  formData.price = Number(row.price)
  formData.originalPrice = Number(row.originalPrice || row.price)
  formData.stock = row.stock
  formData.status = row.status
  formData.detailHtml = row.detailHtml || ''
  subImagesList.value = row.subImages ? [...row.subImages] : []
  manualSubImgUrl.value = ''
  dialogVisible.value = true
}

async function handleUploadMainImage(options: UploadRequestOptions) {
  const form = new FormData()
  form.append('file', options.file)
  try {
    const res = await request.post<any, any>('/api/common/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = typeof res === 'string' ? res : (res?.url || res?.data)
    if (url) {
      formData.mainImage = url
      ElMessage.success('主图上传成功')
    }
  } catch (err) {
    ElMessage.error('图片上传失败，请检查文件格式大小')
  }
}

async function handleUploadSubImage(options: UploadRequestOptions) {
  const form = new FormData()
  form.append('file', options.file)
  try {
    const res = await request.post<any, any>('/api/common/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = typeof res === 'string' ? res : (res?.url || res?.data)
    if (url) {
      subImagesList.value.push(url)
      ElMessage.success('副图添加成功')
    }
  } catch (err) {
    ElMessage.error('副图上传失败')
  }
}

function addManualSubImage() {
  if (!manualSubImgUrl.value.trim()) return
  subImagesList.value.push(manualSubImgUrl.value.trim())
  manualSubImgUrl.value = ''
}

function removeSubImage(index: number) {
  subImagesList.value.splice(index, 1)
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const payload: ProductCommand = {
        ...formData,
        subImages: subImagesList.value
      }
      if (isEdit.value && formData.id) {
        await updateProductApi(formData.id, payload)
        ElMessage.success('商品信息更新成功')
      } else {
        await createProductApi(payload)
        ElMessage.success('新商品录入成功')
      }
      dialogVisible.value = false
      fetchProducts()
    } catch (err) {
      console.error('保存商品失败', err)
    } finally {
      submitLoading.value = false
    }
  })
}

function formatDate(dateStr: string) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').substring(0, 19)
}

onMounted(async () => {
  await loadMetadata()
  fetchProducts()
})
</script>

<style scoped>
.product-mgmt-container {
  padding: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 4px;
}

.subtitle {
  font-size: 13px;
  color: #64748b;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-select {
  width: 170px;
}

.status-select {
  width: 130px;
}

.table-card {
  border-radius: 10px;
}

.table-thumb {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.thumb-err {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 11px;
}

.prod-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.prod-name {
  font-weight: 600;
  color: #1e293b;
  font-size: 14px;
}

.prod-sub {
  font-size: 12px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.price-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.price-now {
  color: #ef4444;
  font-weight: 700;
  font-size: 15px;
}

.price-old {
  color: #94a3b8;
  font-size: 11px;
  text-decoration: line-through;
}

.time-text {
  font-size: 12px;
  color: #64748b;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.w-full {
  width: 100%;
}

.image-uploader-cell {
  width: 100%;
}

.image-preview {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.preview-box {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}

.preview-tip {
  font-size: 12px;
  color: #64748b;
}

/* Sub-Images Gallery styling */
.sub-images-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 10px;
}

.sub-img-item {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.sub-img-thumb {
  width: 100%;
  height: 100%;
}

.sub-img-actions {
  position: absolute;
  top: 4px;
  right: 4px;
}

.sub-img-uploader {
  display: inline-block;
}

.upload-trigger-box {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 11px;
}

.upload-trigger-box:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

.gallery-manual-add {
  max-width: 380px;
}
</style>
