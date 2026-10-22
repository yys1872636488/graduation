<template>
  <div class="login-page">
    <!-- 左侧：品牌区 -->
    <aside class="login-brand">
      <div class="brand-content">
        <div class="brand-mark">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect x="4" y="4" width="40" height="40" rx="4" stroke="rgba(255,255,255,0.6)" stroke-width="1.5"/>
            <path d="M16 18h4v12h-4zM22 14h4v16h-4zM28 18h4v12h-4z" fill="rgba(255,255,255,0.7)" opacity="0.6"/>
            <path d="M14 30l20-12" stroke="rgba(255,255,255,0.35)" stroke-width="1"/>
          </svg>
        </div>
        <h1 class="brand-title">服装追溯系统</h1>
        <p class="brand-desc">原材料到成品的全链路数字化追溯</p>
        <div class="brand-line"></div>
        <p class="brand-sub">集成机器视觉的布料瑕疵检测与批次追溯管理平台</p>
      </div>
    </aside>

    <!-- 右侧：登录表单 -->
    <main class="login-form-area">
      <div class="login-card">
        <h2 class="form-title">登录系统</h2>
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              :prefix-icon="UserFilled"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item style="margin-top: 8px">
            <el-button
              type="primary"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { UserFilled, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
}

/* 左侧品牌区 */
.login-brand {
  width: 42%;
  background: var(--color-sidebar);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      0deg,
      transparent,
      transparent 2px,
      rgba(255,255,255,0.015) 2px,
      rgba(255,255,255,0.015) 4px
    ),
    repeating-linear-gradient(
      90deg,
      transparent,
      transparent 2px,
      rgba(255,255,255,0.015) 2px,
      rgba(255,255,255,0.015) 4px
    );
  pointer-events: none;
}

.brand-content {
  text-align: center;
  color: #fff;
  padding: 48px;
  position: relative;
  z-index: 1;
}

.brand-mark {
  margin-bottom: 32px;
  opacity: 0.9;
}

.brand-title {
  font-size: 32px;
  font-weight: 600;
  margin: 0 0 12px;
  letter-spacing: 0.04em;
  color: rgba(255,255,255,0.95);
}

.brand-desc {
  font-size: 15px;
  color: rgba(255,255,255,0.6);
  margin: 0 0 32px;
  line-height: 1.6;
}

.brand-line {
  width: 40px;
  height: 1px;
  background: rgba(255,255,255,0.3);
  margin: 0 auto 32px;
}

.brand-sub {
  font-size: 13px;
  color: rgba(255,255,255,0.4);
  margin: 0;
  line-height: 1.6;
}

/* 右侧表单区 */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
}

.login-card {
  width: 380px;
  padding: 48px 40px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 32px;
  letter-spacing: -0.01em;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 0.04em;
}

</style>