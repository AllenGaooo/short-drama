<template>
  <div class="login-container">
    <el-card class="loginPanel" style="max-width: 420px">
      <template #header>
        <div class="login-header">
          <el-icon size="28" color="#626aef"><Platform /></el-icon>
          <span class="title">Bunny Admin 注册</span>
        </div>
      </template>

      <el-form
        ref="registerFormRef"
        hide-required-asterisk
        :model="registerForm"
        :rules="rules"
        label-width="70"
        style="max-width: 400px"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" :prefix-icon="Iphone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            type="password"
            show-password
            v-model="registerForm.password"
            :prefix-icon="Lock"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            type="password"
            show-password
            v-model="registerForm.confirmPassword"
            :prefix-icon="Lock"
            placeholder="请再次输入密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onSubmit">注册</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" :underline="false" @click="goLogin">已有账号？去登录</el-link>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { Iphone, Lock, Platform } from '@element-plus/icons-vue'
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import router from '@/router'
import userStore from '@/store/userStore.js'
import { register, getMenus } from '@/api/user.js'
import { buildMenuTree } from '@/utils/menu.js'
import { ensureMenuRoutes } from '@/router'

// 注册成功后：拉取分级菜单 -> 构建动态路由 -> 进入后台
const enterAdmin = () => {
  return getMenus().then((res) => {
    const list = res.data || []
    const tree = buildMenuTree(list)
    userStore().menus = tree
    // /main 与 welcome 已是静态路由，这里只注册菜单动态子路由
    ensureMenuRoutes()
  })
}

const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = ref({
  phone: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = reactive({
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirm, trigger: 'blur' }
  ]
})

const onSubmit = () => {
  registerFormRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    register(registerForm.value.phone, registerForm.value.password)
      .then((res) => {
        const data = res.data || {}
        ElMessage.success(res.msg || '注册成功')
        // 注册成功后端直接返回 token，自动登录
        userStore().token = data.token
        userStore().userId = data.userId
        // 拉取分级菜单并构建路由后进入后台
        enterAdmin()
          .then(() => router.push('/main'))
          .catch(() => router.push('/main'))
      })
      .catch((err) => {
        ElMessage.error((err && err.message) || '注册失败')
      })
      .finally(() => {
        loading.value = false
      })
  })
}

const onReset = () => {
  registerFormRef.value.resetFields()
}

const goLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #626aef 0%, #8e9bff 100%);
}
.loginPanel {
  background-color: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
}
.login-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.login-header .title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
</style>
