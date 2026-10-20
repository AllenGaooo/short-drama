<template>
  <div class="login-page">
    <el-card class="login-panel">
      <template #header>
        <div class="panel-head">
          <span class="logo">🐰</span>
          <span class="brand">兔兔短剧</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" class="tabs">
        <!-- ========== 密码登录 ========== -->
        <el-tab-pane label="密码登录" name="password">
          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="0"
            size="large"
          >
            <el-form-item prop="phone">
              <el-input
                v-model="pwdForm.phone"
                placeholder="请输入手机号"
                :prefix-icon="Iphone"
                maxlength="11"
                clearable
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="pwdForm.password"
                type="password"
                placeholder="请输入密码"
                :prefix-icon="Lock"
                show-password
                @keyup.enter="onPasswordLogin"
              />
            </el-form-item>
            <el-button
              type="warning"
              class="submit"
              :loading="loading"
              @click="onPasswordLogin"
            >
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <!-- ========== 验证码登录 ========== -->
        <el-tab-pane label="验证码登录" name="code">
          <el-form
            ref="codeFormRef"
            :model="codeForm"
            :rules="codeRules"
            label-width="0"
            size="large"
          >
            <el-form-item prop="phone">
              <el-input
                v-model="codeForm.phone"
                placeholder="请输入手机号"
                :prefix-icon="Iphone"
                maxlength="11"
                clearable
              />
            </el-form-item>
            <el-form-item prop="code">
              <div class="code-row">
                <el-input
                  v-model="codeForm.code"
                  placeholder="请输入验证码"
                  :prefix-icon="Message"
                  maxlength="6"
                  @keyup.enter="onCodeLogin"
                />
                <el-button
                  :disabled="countdown > 0"
                  :loading="codeSending"
                  class="code-btn"
                  @click="onSendCode"
                >
                  {{ countdown > 0 ? countdown + 's 后重发' : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-button
              type="warning"
              class="submit"
              :loading="loading"
              @click="onCodeLogin"
            >
              登录 / 注册
            </el-button>
            <div class="tip">未注册的手机号，验证码登录时将自动注册</div>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock, Message } from '@element-plus/icons-vue'
import { loginByPassword, loginByCode, sendSmsCode } from '@/api/user.js'
import { setToken } from '@/utils/request.js'

const router = useRouter()
const route = useRoute()

const activeTab = ref('password')
const loading = ref(false)
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer = null

// ---------- 表单 ----------
const pwdFormRef = ref(null)
const pwdForm = reactive({ phone: '', password: '' })
const pwdRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6~20 位', trigger: 'blur' }
  ]
}

const codeFormRef = ref(null)
const codeForm = reactive({ phone: '', code: '' })
const codeRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ]
}

// ---------- 登录成功处理 ----------
const handleSuccess = (res, msg) => {
  const data = res && res.data
  const token = data && data.token
  if (!token) {
    ElMessage.error('登录失败：未获取到 token')
    return
  }
  setToken(token)
  ElMessage.success(msg || '登录成功')
  // 跳回来源页，没有则回首页
  const redirect = route.query.redirect
  router.push(redirect ? String(redirect) : '/')
}

// ---------- 密码登录 ----------
const onPasswordLogin = async () => {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return // 校验不通过
  }

  loading.value = true
  try {
    const res = await loginByPassword(pwdForm.phone, pwdForm.password)
    handleSuccess(res, '登录成功')
  } catch (e) {
    ElMessage.error(e.message || '登录失败，请检查手机号或密码')
  } finally {
    loading.value = false
  }
}

// ---------- 验证码登录 ----------
const onCodeLogin = async () => {
  if (!codeFormRef.value) return
  try {
    await codeFormRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    const res = await loginByCode(codeForm.phone, codeForm.code)
    handleSuccess(res, '登录成功')
  } catch (e) {
    ElMessage.error(e.message || '登录失败，请检查验证码')
  } finally {
    loading.value = false
  }
}

// ---------- 发送验证码（60 秒倒计时） ----------
const onSendCode = async () => {
  const phone = codeForm.phone
  if (!/^1\d{10}$/.test(phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  codeSending.value = true
  try {
    await sendSmsCode(phone)
    ElMessage.success('验证码已发送')
    startCountdown(60)
  } catch (e) {
    ElMessage.error(e.message || '验证码发送失败')
  } finally {
    codeSending.value = false
  }
}

const startCountdown = (seconds) => {
  countdown.value = seconds
  clearInterval(countdownTimer)
  countdownTimer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

// 离开页面清理定时器，避免内存泄漏
onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fff5ec 0%, #ffe9d6 100%);
}
.login-panel {
  width: 92%;
  max-width: 400px;
  border-radius: 14px;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.logo { font-size: 26px; }
.brand { font-size: 19px; font-weight: 700; color: #333; }
.tabs :deep(.el-tabs__item) { font-size: 15px; }
.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
}
.code-row .el-input { flex: 1; }
.code-btn { width: 116px; flex-shrink: 0; }
.submit {
  width: 100%;
  margin-top: 4px;
  font-size: 16px;
  letter-spacing: 2px;
}
.tip {
  margin-top: 12px;
  text-align: center;
  color: #999;
  font-size: 12px;
}
</style>
