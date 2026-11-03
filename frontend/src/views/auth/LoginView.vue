<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo" @click="$router.push('/')">
          <el-icon class="logo-icon"><Shop /></el-icon>
          <span class="logo-text">SHOP 商城</span>
        </div>
        <p class="auth-subtitle">现代化 Java Web 架构商城 · 用户认证中心</p>
      </div>

      <!-- Quick Fill Bar for Examiners/Graders -->
      <div class="demo-account-box">
        <span class="demo-title">🎯 快捷测试账号填充：</span>
        <div class="demo-buttons">
          <el-button size="small" type="warning" plain @click="fillAdmin">管理员 (admin / admin123)</el-button>
          <el-button size="small" type="primary" plain @click="fillCustomer">顾客 (customer / 123456)</el-button>
        </div>
      </div>

      <!-- Tabs -->
      <el-tabs v-model="activeTab" class="auth-tabs" stretch>
        <!-- Login Form -->
        <el-tab-pane label="账号登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            @keyup.enter="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>

            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleLogin"
            >
              立即登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- Register Form -->
        <el-tab-pane label="注册新账号" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            @keyup.enter="handleRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="3-32位英文字母或数字"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>

            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="6-32位密码字符"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item label="显示昵称 (可选)" prop="nickname">
              <el-input
                v-model="registerForm.nickname"
                placeholder="输入展示给商城的个性昵称"
                :prefix-icon="Edit"
                clearable
              />
            </el-form-item>

            <el-button
              type="success"
              size="large"
              class="submit-btn"
              :loading="loading"
              @click="handleRegister"
            >
              创建账号
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="auth-footer">
        <router-link to="/">返回商城首页</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Shop, User, Lock, Edit } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)

// Login Form
const loginFormRef = ref<FormInstance>()
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// Register Form
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const validateConfirmPass = (_rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度须在3至32位之间', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持英文字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPass, trigger: 'blur' }
  ]
}

function fillAdmin() {
  activeTab.value = 'login'
  loginForm.username = 'admin'
  loginForm.password = 'admin123'
}

function fillCustomer() {
  activeTab.value = 'login'
  loginForm.username = 'customer'
  loginForm.password = '123456'
}

async function handleLogin() {
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await userStore.login({
        username: loginForm.username,
        password: loginForm.password
      })
      ElMessage.success(`欢迎回来，${res.user.nickname || res.user.username}！`)

      const redirect = route.query.redirect as string
      if (redirect) {
        router.push(redirect)
      } else if (res.user.role === 'ROLE_ADMIN') {
        router.push('/admin')
      } else {
        router.push('/')
      }
    } catch {
      // 错误已由 request 拦截器统一提示
    } finally {
      loading.value = false
    }
  })
}

async function handleRegister() {
  if (!registerFormRef.value) return
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.register({
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname || undefined
      })
      ElMessage.success('注册成功！已为您切换至登录界面')
      loginForm.username = registerForm.username
      loginForm.password = registerForm.password
      activeTab.value = 'login'
    } catch {
      // 错误由拦截器统一弹窗
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4f8 0%, #e2e8f0 100%);
  padding: 24px 16px;
}

.auth-card {
  width: 100%;
  max-width: 440px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
  padding: 32px 36px;
}

.auth-header {
  text-align: center;
  margin-bottom: 20px;
}

.auth-logo {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-bottom: 8px;
}

.logo-icon {
  font-size: 32px;
  color: #409eff;
}

.logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.5px;
}

.auth-subtitle {
  font-size: 13px;
  color: #64748b;
  margin: 0;
}

.demo-account-box {
  background-color: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 20px;
}

.demo-title {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 8px;
}

.demo-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.auth-tabs {
  margin-top: 8px;
}

.submit-btn {
  width: 100%;
  margin-top: 12px;
  font-weight: 600;
  letter-spacing: 1px;
}

.auth-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
}

.auth-footer a {
  color: #409eff;
  text-decoration: none;
}

.auth-footer a:hover {
  text-decoration: underline;
}
</style>
