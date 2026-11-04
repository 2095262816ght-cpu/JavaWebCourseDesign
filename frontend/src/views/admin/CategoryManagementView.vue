<template>
  <div class="category-mgmt-container">
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">商品分类管理</h2>
        <p class="page-desc">维护商品分类的多级树形层级结构，支持无级递归扩展与缓存自动清理</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Refresh" @click="fetchCategories">
          刷新
        </el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog(0)">
          新建一级分类
        </el-button>
      </div>
    </div>

    <!-- Tree Table Card -->
    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="categoryTree"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        style="width: 100%"
        stripe
      >
        <el-table-column prop="name" label="分类名称" min-width="260">
          <template #default="{ row }">
            <div class="category-name-cell">
              <el-tag v-if="row.parentId === 0" type="primary" size="small" effect="plain" class="level-tag">
                一级
              </el-tag>
              <el-tag v-else type="success" size="small" effect="plain" class="level-tag">
                子分类
              </el-tag>
              <span class="category-icon-badge" v-if="row.icon">
                <el-icon><Menu /></el-icon>
              </span>
              <span class="category-name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="分类ID" width="100" align="center" />

        <el-table-column prop="icon" label="图标标识" width="160" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.icon" type="info" size="small" round>
              {{ row.icon }}
            </el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <el-table-column prop="sort" label="排序权重" width="120" align="center">
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

        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Plus" @click="openCreateDialog(row.id)">
              添加子分类
            </el-button>
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(row)">
              编辑
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        status-icon
      >
        <el-form-item label="上级分类" prop="parentId">
          <el-cascader
            v-model="formData.parentId"
            :options="cascaderOptions"
            :props="{ checkStrictly: true, emitPath: false, value: 'id', label: 'name' }"
            placeholder="选择父级分类（留空或选顶级为一级分类）"
            clearable
            style="width: 100%;"
            :disabled="dialogMode === 'create' && parentLocked"
          />
        </el-form-item>

        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称，如 智能手机" maxlength="64" />
        </el-form-item>

        <el-form-item label="分类图标" prop="icon">
          <el-input v-model="formData.icon" placeholder="输入图标名称或样式，如 el-icon-mobile" clearable />
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
import { ref, reactive, onMounted, computed } from 'vue'
import { Plus, Refresh, Edit, Delete, Menu } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getAdminCategoryTreeApi,
  createCategoryApi,
  updateCategoryApi,
  deleteCategoryApi,
  type CategoryNode,
  type CategoryCommand
} from '@/api/category'

const loading = ref(false)
const categoryTree = ref<CategoryNode[]>([])

// Dialog state
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const parentLocked = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const currentEditId = ref<number | null>(null)

const formData = reactive<CategoryCommand>({
  parentId: 0,
  name: '',
  icon: '',
  sort: 1,
  status: 1
})

const formRules: FormRules = {
  name: [
    { required: true, message: '分类名称为必填项', trigger: 'blur' },
    { max: 64, message: '名称最多64个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择启用状态', trigger: 'change' }
  ]
}

const dialogTitle = computed(() => {
  if (dialogMode.value === 'edit') {
    return '编辑分类节点'
  }
  return formData.parentId === 0 ? '新建顶级一级分类' : '新建子级分类'
})

// Cascader options with Root node
const cascaderOptions = computed(() => {
  const rootOption = {
    id: 0,
    name: '【顶级根分类】',
    children: [] as CategoryNode[]
  }

  // Remove current node from selectable parents when in edit mode to avoid cycles
  const filterChildren = (nodes: CategoryNode[]): any[] => {
    return nodes
      .filter((node) => node.id !== currentEditId.value)
      .map((node) => ({
        id: node.id,
        name: node.name,
        children: node.children && node.children.length > 0 ? filterChildren(node.children) : undefined
      }))
  }

  return [rootOption, ...filterChildren(categoryTree.value)]
})

// Fetch tree
const fetchCategories = async () => {
  loading.value = true
  try {
    const res = await getAdminCategoryTreeApi()
    categoryTree.value = res
  } finally {
    loading.value = false
  }
}

// Dialog openers
const openCreateDialog = (parentId: number = 0) => {
  dialogMode.value = 'create'
  currentEditId.value = null
  parentLocked.value = parentId > 0
  formData.parentId = parentId
  formData.name = ''
  formData.icon = ''
  formData.sort = 1
  formData.status = 1
  dialogVisible.value = true
}

const openEditDialog = (row: CategoryNode) => {
  dialogMode.value = 'edit'
  currentEditId.value = row.id
  parentLocked.value = false
  formData.parentId = row.parentId
  formData.name = row.name
  formData.icon = row.icon || ''
  formData.sort = row.sort
  formData.status = row.status
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload: CategoryCommand = {
        parentId: formData.parentId || 0,
        name: formData.name.trim(),
        icon: formData.icon?.trim() || null,
        sort: formData.sort ?? 0,
        status: formData.status
      }

      if (dialogMode.value === 'create') {
        await createCategoryApi(payload)
        ElMessage.success('分类创建成功')
      } else if (currentEditId.value) {
        await updateCategoryApi(currentEditId.value, payload)
        ElMessage.success('分类更新成功')
      }
      dialogVisible.value = false
      fetchCategories()
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row: CategoryNode) => {
  if (row.children && row.children.length > 0) {
    ElMessageBox.alert(
      `分类 “${row.name}” 下存在 ${row.children.length} 个子分类，请先清理或转移子分类后再行删除。`,
      '删除拦截提示',
      {
        confirmButtonText: '我知道了',
        type: 'warning'
      }
    )
    return
  }

  ElMessageBox.confirm(
    `确定要删除分类 “${row.name}” 吗？删除后前台目录将立即移除该节点。`,
    '安全删除确认',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteCategoryApi(row.id)
      ElMessage.success('分类删除成功')
      fetchCategories()
    } catch {
      // Handled by global interceptor
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchCategories()
})
</script>

<style scoped>
.category-mgmt-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  padding: 20px 24px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.page-title {
  margin: 0 0 6px 0;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
}

.page-desc {
  margin: 0;
  font-size: 13px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.table-card {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.category-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.level-tag {
  font-size: 11px;
}

.category-icon-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #4b5563;
}

.category-name-text {
  font-weight: 600;
  color: #1f2937;
}

.text-muted {
  color: #9ca3af;
}

.form-item-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
