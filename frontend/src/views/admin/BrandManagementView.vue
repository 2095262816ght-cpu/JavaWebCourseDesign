<template>
  <div class="brand-mgmt-container">
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">商品品牌管理</h2>
        <p class="page-desc">维护商城商品品牌目录，上传品牌徽标 Logo，并自动同步更新前台首页推荐与商品筛选</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">
          新建品牌
        </el-button>
      </div>
    </div>

    <!-- Filter Card -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="关键字">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索品牌名称、产地或描述"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            style="width: 220px;"
          />
        </el-form-item>

        <el-form-item label="品牌状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 140px;">
            <el-option label="全部状态" :value="undefined" />
            <el-option label="正常启用" :value="1" />
            <el-option label="已下架/禁用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Data Table Card -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="brandList"
        stripe
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="id" label="品牌ID" width="100" align="center" />

        <el-table-column label="品牌徽标" width="120" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.logo"
              :src="row.logo"
              :preview-src-list="[row.logo]"
              preview-teleported
              fit="cover"
              class="brand-logo-img"
            >
              <template #error>
                <div class="image-fallback">Logo</div>
              </template>
            </el-image>
            <div v-else class="image-fallback">无Logo</div>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="品牌名称" width="160">
          <template #default="{ row }">
            <span class="brand-name-text">{{ row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="origin" label="品牌产地" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.origin" type="info" size="small">{{ row.origin }}</el-tag>
            <span v-else style="color: var(--text-4); font-size: 12px;">未设置</span>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="品牌描述" min-width="260">
          <template #default="{ row }">
            <span class="brand-desc-text">{{ row.description || '暂无品牌描述' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="排序权重" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.sort }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建商品品牌' : '编辑商品品牌'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        status-icon
      >
        <el-form-item label="品牌名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入品牌名称，如 Apple" maxlength="64" />
        </el-form-item>

        <el-form-item label="品牌产地" prop="origin">
          <el-input v-model="formData.origin" placeholder="请输入品牌原产国/产地，如 美国加利福尼亚" maxlength="64" clearable />
        </el-form-item>

        <el-form-item label="品牌徽标" prop="logo">
          <div class="logo-uploader-wrapper">
            <el-upload
              class="logo-uploader"
              :action="uploadActionUrl"
              :headers="uploadHeaders"
              name="file"
              accept=".jpg,.jpeg,.png,.gif,.webp,.bmp,.ico"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeImageUpload"
            >
              <img v-if="formData.logo" :src="formData.logo" class="uploaded-logo" />
              <el-icon v-else class="uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="uploader-tip">
              <el-input
                v-model="formData.logo"
                placeholder="或直接输入图片 URL"
                size="small"
                clearable
                style="margin-top: 8px;"
              />
              <span class="hint-text">支持 PNG/JPG/WebP 格式图片，建议尺寸 200x200</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="品牌描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="简要介绍品牌理念、核心产品线或生产制造商"
            maxlength="512"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="排序序号" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="9999" controls-position="right" />
          <span class="form-item-tip">数值越小排在越前面</span>
        </el-form-item>

        <el-form-item label="启用状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常启用</el-radio>
            <el-radio :value="0">下架禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitForm">
            保存提交
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { Plus, Search, Refresh, Edit, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAdminBrandsApi,
  createBrandApi,
  updateBrandApi,
  deleteBrandApi,
  type Brand,
  type BrandCommand,
  type BrandQueryParams
} from '@/api/brand'
import { beforeImageUpload } from '@/utils/image'

const loading = ref(false)
const brandList = ref<Brand[]>([])

const queryForm = reactive<BrandQueryParams>({
  keyword: '',
  status: undefined
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// Dialog state
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentEditId = ref<number | null>(null)

const formData = reactive<BrandCommand>({
  name: '',
  origin: '',
  logo: '',
  description: '',
  sort: 1,
  status: 1
})

const formRules: FormRules = {
  name: [
    { required: true, message: '品牌名称为必填项', trigger: 'blur' },
    { max: 64, message: '名称最多64个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择启用状态', trigger: 'change' }
  ]
}

// Upload configuration
const uploadActionUrl = computed(() => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  return `${baseUrl}/api/common/upload`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { satoken: token } : {}
})

const handleUploadSuccess = (response: any) => {
  if (typeof response === 'string') {
    formData.logo = response
    ElMessage.success('Logo 上传成功')
  } else if (response && response.code === 200 && response.data) {
    formData.logo = response.data
    ElMessage.success('Logo 上传成功')
  } else {
    ElMessage.error(response?.message || 'Logo 上传失败，请重试')
  }
}

const handleUploadError = () => {
  ElMessage.error('Logo 上传失败，请检查登录状态及图片格式')
}

// Fetch list
const fetchBrands = async () => {
  loading.value = true
  try {
    const res = await getAdminBrandsApi({
      page: pagination.current,
      size: pagination.size,
      keyword: queryForm.keyword?.trim() || undefined,
      status: queryForm.status
    })
    brandList.value = res.records
    pagination.total = res.total
  } catch (err) {
    console.error('加载品牌列表失败', err)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  fetchBrands()
}

const handleReset = () => {
  queryForm.keyword = ''
  queryForm.status = undefined
  pagination.current = 1
  fetchBrands()
}

const handleSizeChange = (val: number) => {
  pagination.size = val
  pagination.current = 1
  fetchBrands()
}

const handleCurrentChange = (val: number) => {
  pagination.current = val
  fetchBrands()
}

// Dialog actions
const openCreateDialog = () => {
  dialogMode.value = 'create'
  currentEditId.value = null
  formData.name = ''
  formData.origin = ''
  formData.logo = ''
  formData.description = ''
  formData.sort = 1
  formData.status = 1
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const openEditDialog = (row: Brand) => {
  dialogMode.value = 'edit'
  currentEditId.value = row.id
  formData.name = row.name
  formData.origin = row.origin || ''
  formData.logo = row.logo || ''
  formData.description = row.description || ''
  formData.sort = row.sort
  formData.status = row.status
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (dialogMode.value === 'create') {
        await createBrandApi(formData)
        ElMessage.success('品牌创建成功')
      } else if (currentEditId.value) {
        await updateBrandApi(currentEditId.value, formData)
        ElMessage.success('品牌更新成功')
      }
      dialogVisible.value = false
      fetchBrands()
    } catch {
      // 错误提示由请求拦截器统一处理
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row: Brand) => {
  ElMessageBox.confirm(
    `确定要删除品牌 “${row.name}” 吗？删除后该品牌信息将不再展示。`,
    '安全删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteBrandApi(row.id)
      ElMessage.success('品牌删除成功')
      if (brandList.value.length === 1 && pagination.current > 1) {
        pagination.current -= 1
      }
      fetchBrands()
    } catch {
      // Handled by global interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchBrands()
})
</script>

<style scoped>
.brand-mgmt-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--glass-bg);
  padding: 20px 24px;
  border-radius: 16px;
  border: 1px solid var(--glass-border);
  box-shadow: var(--glass-shadow);
  backdrop-filter: blur(var(--glass-blur)) saturate(165%);
  -webkit-backdrop-filter: blur(var(--glass-blur)) saturate(165%);
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: var(--text-3);
}

.filter-card {
  border-radius: 16px;
  border: 1px solid var(--glass-border);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.filter-form .el-form-item {
  margin-bottom: 0;
  margin-right: 18px;
}

.table-card {
  border-radius: 8px;
  border: 1px solid var(--line);
}

.brand-logo-img {
  width: 54px;
  height: 54px;
  border-radius: 6px;
  border: 1px solid var(--line);
  object-fit: cover;
}

.image-fallback {
  width: 54px;
  height: 54px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.05);
  color: var(--text-4);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
}

.brand-name-text {
  font-weight: 600;
  color: var(--text);
}

.brand-desc-text {
  color: var(--text-2);
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

/* Upload styles */
.logo-uploader-wrapper {
  display: flex;
  flex-direction: column;
}

.logo-uploader {
  border: 1px dashed var(--line-strong);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.2s;
}

.logo-uploader:hover {
  border-color: var(--violet);
}

.uploader-icon {
  font-size: 28px;
  color: var(--text-3);
}

.uploaded-logo {
  width: 88px;
  height: 88px;
  object-fit: cover;
}

.hint-text {
  font-size: 12px;
  color: var(--text-4);
  margin-top: 4px;
  display: inline-block;
}

.form-item-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-4);
}
</style>
