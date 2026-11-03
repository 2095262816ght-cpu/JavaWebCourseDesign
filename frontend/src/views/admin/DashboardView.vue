<template>
  <div class="admin-dashboard">
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>今日销售额</span>
              <el-tag type="success">实时</el-tag>
            </div>
          </template>
          <div class="card-value">¥ 48,920.00</div>
          <div class="card-footer">较昨日 +12.5%</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>订单总量</span>
              <el-tag type="primary">日统计</el-tag>
            </div>
          </template>
          <div class="card-value">128 单</div>
          <div class="card-footer">完成率 94.2%</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>在售商品</span>
              <el-tag type="warning">库存正常</el-tag>
            </div>
          </template>
          <div class="card-value">7 款商品</div>
          <div class="card-footer">缺货预警 0 件</div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="clickable-card" @click="$router.push('/admin/users')">
          <template #header>
            <div class="card-header">
              <span>注册用户</span>
              <el-tag type="info">用户管理</el-tag>
            </div>
          </template>
          <div class="card-value">{{ userCount !== null ? `${userCount} 位` : '加载中...' }}</div>
          <div class="card-footer">点击前往用户管理列表 →</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="system-status-card">
      <template #header>
        <div class="card-header">
          <span>系统环境与连接监控</span>
          <el-button type="primary" size="small" link>刷新状态</el-button>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="后端运行环境">Spring Boot 3.3.4 (Java 21)</el-descriptions-item>
        <el-descriptions-item label="鉴权安全引擎">Sa-Token 1.39.0 (Stateless JWT)</el-descriptions-item>
        <el-descriptions-item label="持久层ORM">MyBatis-Plus 3.5.7</el-descriptions-item>
        <el-descriptions-item label="主关系数据库">MySQL 8.4 Community (Port: 3306)</el-descriptions-item>
        <el-descriptions-item label="高性能缓存">Docker Redis 7.2 (Port: 6379)</el-descriptions-item>
        <el-descriptions-item label="API 文档规范">OpenAPI 3 / Swagger UI 2.6.0</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminUsersApi } from '@/api/user'

const userCount = ref<number | null>(null)

async function fetchStats() {
  try {
    const res = await getAdminUsersApi({ page: 1, size: 1 })
    userCount.value = res.total
  } catch {
    userCount.value = null
  }
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.admin-dashboard {
  padding: 8px 0;
}

.stat-cards {
  margin-bottom: 24px;
}

.clickable-card {
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.clickable-card:hover {
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #4b5563;
}

.card-value {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  margin: 12px 0;
}

.card-footer {
  font-size: 12px;
  color: #6b7280;
}

.system-status-card {
  border-radius: 8px;
}
</style>
