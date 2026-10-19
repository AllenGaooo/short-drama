<template>
  <div class="login-container">
    <el-card class="loginPanel" style="max-width: 420px">
      <template #header>
        <div class="login-header">
          <el-icon size="28" color="#626aef"><Platform /></el-icon>
          <span class="title">Bunny Admin 管理后台</span>
        </div>
      </template>

      <el-form
        ref="loginFormRef"
        hide-required-asterisk
        :model="loginForm"
        :rules="rules"
        label-width="70"
        style="max-width: 400px"
      >
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="loginForm.phone" :prefix-icon="Iphone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            type="password"
            show-password
            v-model="loginForm.password"
            :prefix-icon="Lock"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="onSubmit">登录</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
        <el-form-item>
          <el-link type="primary" :underline="false" @click="goRegister">没有账号？去注册</el-link>
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
import { loginByPassword, getMenus } from '@/api/user.js'
import { buildMenuTree } from '@/utils/menu.js'

import { ensureMenuRoutes } from '@/router'

// 登录成功后：拉取分级菜单 -> 构建动态路由 -> 进入后台
const enterAdmin = () => {
  return getMenus().then((res) => {
    const list = res.data || []
    const tree = buildMenuTree(list)
    userStore().menus = tree
    // /main 与 welcome 已是静态路由，这里只注册菜单动态子路由
    ensureMenuRoutes()
  })
}

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = ref({
  phone: '',
  password: ''
})

const rules = reactive({
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
})

const onSubmit = () => {
  loginFormRef.value.validate((valid) => {
    if (!valid) return
    loading.value = true
    loginByPassword(loginForm.value.phone, loginForm.value.password)
      .then((res) => {
        const data = res.data || {}
        ElMessage.success(res.msg || '登录成功')
        // 存储 token 与 userId
        userStore().token = data.token
        userStore().userId = data.userId
        // 拉取分级菜单并构建路由后进入后台
        enterAdmin()
          .then(() => router.push('/main'))
          .catch(() => router.push('/main')) // 菜单拉取失败也允许进入（左侧为空）
      })
      .catch((err) => {
        ElMessage.error((err && err.message) || '登录失败')
      })
      .finally(() => {
        loading.value = false
      })
  })
}

const onReset = () => {
  loginFormRef.value.resetFields()
}

const goRegister = () => {
  router.push('/register')
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
