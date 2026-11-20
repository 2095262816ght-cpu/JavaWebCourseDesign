<template>
  <div class="auth-page">
    <div class="auth-shell">
      <!-- 左侧视觉区 -->
      <aside class="auth-visual">
        <div class="visual-inner">
          <button class="visual-brand" type="button" @click="$router.push('/')">
            <span class="brand-mark" aria-hidden="true">
              <img src="/brand/logo-mark.png" alt="" />
            </span>
            <span class="visual-brand-text">
              <span class="brand-main">SHOP NOIR</span>
              <span class="brand-sub mono">CURATED STORE</span>
            </span>
          </button>

          <div class="visual-copy">
            <span class="eyebrow">MEMBER ACCESS</span>
            <h1 class="visual-title">
              欢迎回到<br />
              <span class="text-gradient">SHOP NOIR</span>
            </h1>
            <p class="visual-desc">
              登录后可同步购物车与订单进度，<br />
              并享受会员专属价格与优先发货。
            </p>

            <ul class="visual-list">
              <li><span class="dot"></span> 官方正品 · 假一赔十</li>
              <li><span class="dot"></span> 顺丰包邮 · 48 小时发货</li>
              <li><span class="dot"></span> 七天无理由退换</li>
            </ul>
          </div>

          <div class="visual-foot">
            <span class="mono">客服热线 400-800-1234</span>
            <span class="mono">© 2026 SHOP NOIR</span>
          </div>
        </div>

        <span class="visual-ring ring-a" aria-hidden="true"></span>
        <span class="visual-ring ring-b" aria-hidden="true"></span>
        <span class="visual-glow" aria-hidden="true"></span>
      </aside>

      <!-- 右侧表单区 -->
      <main class="auth-panel">
        <div class="auth-card">
          <header class="card-head">
            <h2 class="card-title">{{ activeTab === 'login' ? '账号登录' : '注册新账号' }}</h2>
            <p class="card-sub">
              {{ activeTab === 'login' ? '输入账号密码，继续你的购物旅程' : '创建账号后即可加购与下单' }}
            </p>
          </header>

          <!-- 切换 -->
          <div class="auth-switcher" role="tablist" aria-label="认证方式切换">
            <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'login'"
              class="switcher-btn"
              :class="{ 'is-active': activeTab === 'login' }"
              @click="switchTab('login')"
            >
              登录
            </button>
            <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'register'"
              class="switcher-btn"
              :class="{ 'is-active': activeTab === 'register' }"
              @click="switchTab('register')"
            >
              注册
            </button>
          </div>

          <!-- 登录 -->
          <el-form
            v-show="activeTab === 'login'"
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
            class="auth-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                size="large"
                placeholder="请输入用户名"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>

            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="loginForm.password"
                size="large"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <button class="btn btn--primary submit-btn" type="button" :disabled="loading" @click="handleLogin">
              <span v-if="loading" class="btn-loader"></span>
              {{ loading ? '正在验证…' : '立即登录' }}
              <el-icon v-if="!loading"><Right /></el-icon>
            </button>
          </el-form>

          <!-- 注册 -->
          <el-form
            v-show="activeTab === 'register'"
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
            class="auth-form"
            @keyup.enter="handleRegister"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                size="large"
                placeholder="3-32 位字母、数字或下划线"
                :prefix-icon="User"
                clearable
              />
            </el-form-item>

            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="registerForm.password"
                size="large"
                type="password"
                placeholder="6-32 位密码字符"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                size="large"
                type="password"
                placeholder="请再次输入密码"
                :prefix-icon="Lock"
                show-password
                clearable
              />
            </el-form-item>

            <el-form-item label="显示昵称（可选）" prop="nickname">
              <el-input
                v-model="registerForm.nickname"
                size="large"
                placeholder="展示给商城的个性昵称"
                :prefix-icon="Edit"
                clearable
              />
            </el-form-item>

            <button class="btn btn--primary submit-btn" type="button" :disabled="loading" @click="handleRegister">
              <span v-if="loading" class="btn-loader"></span>
              {{ loading ? '正在创建…' : '创建账号' }}
              <el-icon v-if="!loading"><Right /></el-icon>
            </button>
          </el-form>

          <div class="login-note">
            <button class="text-link" type="button" @click="handleForgotPassword">忘记密码？</button>
            <span class="note-terms">登录即代表你同意《用户协议》与《隐私政策》</span>
          </div>

          <div class="card-foot">
            <router-link to="/" class="back-home">
              <el-icon><ArrowLeft /></el-icon>
              返回商城首页
            </router-link>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock, Edit, ArrowLeft, Right } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)

const loginFormRef = ref<FormInstance>()
const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: ''
})

const validateConfirmPass = (_rule: unknown, value: string, callback: (err?: Error) => void) => {
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
  confirmPassword: [{ required: true, validator: validateConfirmPass, trigger: 'blur' }]
}

function switchTab(tab: 'login' | 'register') {
  activeTab.value = tab
}

function handleForgotPassword() {
  ElMessage.info('请联系客服 400-800-1234 重置登录密码')
}

async function handleLogin() {
  // 回车提交不受按钮 disabled 约束，这里显式拦截重复提交
  if (loading.value) return
  if (!loginFormRef.value) return
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await userStore.login({
        username: loginForm.username,
        password: loginForm.password
      })
      await cartStore.fetchCart().catch(() => {})
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
  padding: 40px 24px;
}

.auth-shell {
  width: 100%;
  max-width: 1080px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.92fr);
  border: 1px solid var(--line);
  border-radius: 32px;
  overflow: hidden;
  background: rgba(8, 10, 17, 0.6);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  box-shadow: 0 60px 140px -60px rgba(0, 0, 0, 1);
}

/* 左侧视觉 */
.auth-visual {
  position: relative;
  padding: 44px;
  overflow: hidden;
  background: radial-gradient(700px 500px at 10% 0%, rgba(124, 92, 255, 0.28), transparent 62%),
    radial-gradient(600px 460px at 90% 100%, rgba(34, 211, 238, 0.16), transparent 62%),
    linear-gradient(160deg, #0a0c16, #06070d);
}

.visual-inner {
  position: relative;
  z-index: 2;
  height: 100%;
  min-height: 520px;
  display: flex;
  flex-direction: column;
}

.visual-brand {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  padding: 0;
  border: none;
  background: none;
  color: var(--text);
  cursor: pointer;
  width: fit-content;
}

.brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  color: var(--cyan);
  border-radius: 12px;
  background: linear-gradient(150deg, rgba(124, 92, 255, 0.24), rgba(34, 211, 238, 0.1));
  border: 1px solid rgba(124, 92, 255, 0.35);
}

.visual-brand-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  line-height: 1;
  text-align: left;
}

.brand-main {
  font-family: var(--font-display);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text);
  white-space: nowrap;
}

.brand-sub {
  font-size: 8px;
  letter-spacing: 0.24em;
  color: var(--text-4);
  white-space: nowrap;
}

.visual-copy {
  margin-top: auto;
  padding-top: 48px;
}

.visual-title {
  margin-top: 18px;
  font-size: clamp(34px, 3.6vw, 48px);
  font-weight: 700;
  line-height: 1.05;
  letter-spacing: -0.035em;
}

.visual-desc {
  margin-top: 18px;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--text-2);
}

.visual-list {
  list-style: none;
  margin: 28px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.visual-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--text-2);
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cyan);
  box-shadow: 0 0 10px rgba(34, 211, 238, 0.9);
}

.visual-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 40px;
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--text-4);
}

/* 装饰环 */
.visual-ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.07);
  pointer-events: none;
}

.ring-a {
  width: 420px;
  height: 420px;
  right: -140px;
  top: -120px;
  box-shadow: 0 0 0 40px rgba(255, 255, 255, 0.015), 0 0 0 90px rgba(255, 255, 255, 0.01);
  animation: float-slow 14s ease-in-out infinite;
}

.ring-b {
  width: 240px;
  height: 240px;
  right: 20px;
  bottom: -90px;
  border-color: rgba(124, 92, 255, 0.18);
  animation: float-slow 18s ease-in-out infinite reverse;
}

.visual-glow {
  position: absolute;
  left: -100px;
  bottom: -140px;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(124, 92, 255, 0.38), transparent 68%);
  filter: blur(60px);
  pointer-events: none;
}

/* 右侧表单 */
.auth-panel {
  display: flex;
  align-items: center;
  padding: 44px 44px 36px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.03), rgba(255, 255, 255, 0.01));
  border-left: 1px solid var(--line);
}

.auth-card {
  width: 100%;
}

.card-title {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.card-sub {
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--text-3);
}

.auth-switcher {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px;
  margin: 24px 0 22px;
  padding: 4px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
}

.switcher-btn {
  padding: 9px 12px;
  border: none;
  border-radius: 9px;
  background: transparent;
  color: var(--text-3);
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s var(--ease);
}

.switcher-btn:hover {
  color: var(--text);
}

.switcher-btn.is-active {
  background: rgba(255, 255, 255, 0.09);
  color: var(--text);
  font-weight: 600;
  box-shadow: 0 6px 16px -10px rgba(0, 0, 0, 0.9);
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.auth-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 12px;
  font-weight: 500;
  color: var(--text-2);
}

.submit-btn {
  width: 100%;
  height: 48px;
  margin-top: 6px;
}

.btn-loader {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid rgba(7, 8, 13, 0.3);
  border-top-color: #07080d;
  animation: spin-slow 0.8s linear infinite;
}

/* 登录提示 */
.login-note {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  text-align: center;
}

.text-link {
  padding: 0;
  border: none;
  background: none;
  font-family: var(--font-body);
  font-size: 12.5px;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.25s var(--ease);
}

.text-link:hover {
  color: var(--cyan);
}

.note-terms {
  font-size: 11px;
  line-height: 1.6;
  color: var(--text-4);
}

.card-foot {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--line);
  text-align: center;
}

.back-home {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-3);
  transition: color 0.25s var(--ease);
}

.back-home:hover {
  color: var(--cyan);
}

/* 响应式 */
@media (max-width: 920px) {
  .auth-shell {
    grid-template-columns: 1fr;
    max-width: 460px;
  }

  .auth-visual {
    display: none;
  }

  .auth-panel {
    border-left: none;
    padding: 36px 28px 30px;
  }
}

@media (max-width: 480px) {
  .auth-page {
    padding: 20px 14px;
  }

  .auth-shell {
    border-radius: 22px;
  }

  .auth-panel {
    padding: 28px 20px 24px;
  }

  .note-terms {
    font-size: 10.5px;
  }
}
</style>
