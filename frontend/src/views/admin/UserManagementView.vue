<template>
  <div class="user-mgmt-container">
    <div class="page-header">
      <div class="header-info">
        <h2 class="page-title">平台用户管理</h2>
        <p class="page-desc">查询并管理全平台买家与管理员账户，控制账号启停状态及权限分配</p>
      </div>
    </div>

    <!-- Search & Filter Card -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="关键字">
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索用户名或昵称"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            style="width: 220px;"
          />
        </el-form-item>

        <el-form-item label="用户角色">
          <el-select v-model="queryForm.role" placeholder="全部角色" clearable style="width: 160px;">
            <el-option label="全部角色" value="" />
            <el-option label="超级管理员" value="ROLE_ADMIN" />
            <el-option label="顾客买家" value="ROLE_CUSTOMER" />
          </el-select>
        </el-form-item>

        <el-form-item label="账号状态">
          <el-select v-model="queryForm.status" placeholder="全部状态" clearable style="width: 140px;">
            <el-option label="全部状态" :value="undefined" />
            <el-option label="正常启用" :value="1" />
            <el-option label="已封禁" :value="0" />
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
        :data="userList"
        stripe
        style="width: 100%"
        row-key="id"
      >
        <el-table-column prop="id" label="用户ID" width="100" align="center" />

        <el-table-column label="用户资料" min-width="220">
          <template #default="{ row }">
            <div class="user-profile-cell">
              <el-avatar :size="40" :src="row.avatar" />
              <div class="user-text">
                <span class="user-nickname">{{ row.nickname || '未设置昵称' }}</span>
                <span class="user-username">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="身份角色" width="150" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'ROLE_ADMIN'" type="danger" effect="light" round>
              超级管理员
            </el-tag>
            <el-tag v-else type="primary" effect="light" round>
              顾客买家
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="160" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <el-switch
                :model-value="row.status === 1"
                :loading="row.statusLoading"
                :disabled="isCurrentAdmin(row)"
                active-color="#10b981"
                inactive-color="#ef4444"
                inline-prompt
                active-text="正常"
                inactive-text="禁用"
                @change="(val: boolean) => toggleStatus(row, val)"
              />
              <span v-if="isCurrentAdmin(row)" class="self-tag">(当前登录)</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="注册时间" width="200" align="center">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="用户详细资料" width="500px" destroy-on-close>
      <div v-if="selectedUser" class="detail-body">
        <div class="detail-avatar-box">
          <el-avatar :size="72" :src="selectedUser.avatar" />
          <h3 class="detail-name">{{ selectedUser.nickname || selectedUser.username }}</h3>
          <el-tag :type="selectedUser.role === 'ROLE_ADMIN' ? 'danger' : 'primary'">
            {{ selectedUser.role === 'ROLE_ADMIN' ? '超级管理员' : '顾客买家' }}
          </el-tag>
        </div>

        <el-descriptions :column="1" border class="detail-desc">
          <el-descriptions-item label="用户唯一ID">{{ selectedUser.id }}</el-descriptions-item>
          <el-descriptions-item label="登录用户名">{{ selectedUser.username }}</el-descriptions-item>
          <el-descriptions-item label="展示昵称">{{ selectedUser.nickname || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="账号状态">
            <el-tag :type="selectedUser.status === 1 ? 'success' : 'danger'" size="small">
              {{ selectedUser.status === 1 ? '正常活跃' : '已被封禁' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ formatDateTime(selectedUser.createdAt) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUsersApi, updateUserStatusApi } from '@/api/user'
import type { UserProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

interface UserRow extends UserProfile {
  statusLoading?: boolean
}

const loading = ref(false)
const userList = ref<UserRow[]>([])

const queryForm = reactive({
  keyword: '',
  role: '',
  status: undefined as number | undefined
})

const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

const detailVisible = ref(false)
const selectedUser = ref<UserProfile | null>(null)

function isCurrentAdmin(row: UserProfile) {
  return userStore.user?.id === row.id
}

function formatDateTime(val?: string) {
  if (!val) return '-'
  return val.replace('T', ' ').substring(0, 19)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminUsersApi({
      page: pagination.current,
      size: pagination.size,
      keyword: queryForm.keyword ? queryForm.keyword.trim() : undefined,
      role: queryForm.role || undefined,
      status: queryForm.status
    })
    userList.value = res.records.map((item) => ({ ...item, statusLoading: false }))
    pagination.total = res.total
  } catch {
    // 错误处理由 request 拦截器完成
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  loadData()
}

function handleReset() {
  queryForm.keyword = ''
  queryForm.role = ''
  queryForm.status = undefined
  pagination.current = 1
  loadData()
}

async function toggleStatus(row: UserRow, newActive: boolean) {
  const targetStatus = newActive ? 1 : 0
  const actionText = newActive ? '启用' : '禁用'

  try {
    await ElMessageBox.confirm(
      `确定要【${actionText}】用户 "${row.nickname || row.username}" 吗？${
        !newActive ? '禁用后该用户将立即强制下线且无法再次登录。' : ''
      }`,
      `确认${actionText}`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: newActive ? 'info' : 'warning'
      }
    )

    row.statusLoading = true
    await updateUserStatusApi(row.id, targetStatus)
    row.status = targetStatus
    ElMessage.success(`用户已成功${actionText}`)
  } catch (err: any) {
    if (err !== 'cancel') {
      // 失败时还原表格开关状态
      loadData()
    }
  } finally {
    row.statusLoading = false
  }
}

function viewDetail(row: UserProfile) {
  selectedUser.value = row
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-mgmt-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0;
}

.page-desc {
  font-size: 13px;
  color: #6b7280;
  margin: 4px 0 0 0;
}

.filter-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.filter-form {
  margin-bottom: -18px;
}

.table-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.user-profile-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-text {
  display: flex;
  flex-direction: column;
}

.user-nickname {
  font-weight: 600;
  color: #1f2937;
  font-size: 14px;
}

.user-username {
  font-size: 12px;
  color: #9ca3af;
}

.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.self-tag {
  font-size: 11px;
  color: #9ca3af;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.detail-avatar-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.detail-name {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.detail-desc {
  width: 100%;
}
</style>
