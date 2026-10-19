<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <el-icon :size="40" color="#409eff"><VideoCamera /></el-icon>
        <h2>兔兔短剧 · 注册</h2>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11">
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            show-password
            @keyup.enter="handleRegister"
          >
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleRegister">
          注 册
        </el-button>
      </el-form>
      <div class="auth-footer">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  phone: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

const handleRegister = async () => {
  await formRef.value.validate()
  loading.value = true
  try {
    const res = await register(form.phone, form.password)
    userStore.setLogin(res.data)
    ElMessage.success('注册成功')
    router.push('/drama')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.auth-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}
.auth-header {
  text-align: center;
  margin-bottom: 28px;
}
.auth-header h2 {
  margin-top: 12px;
  color: #1f2937;
}
.submit-btn {
  width: 100%;
  margin-top: 8px;
}
.auth-footer {
  margin-top: 20px;
  text-align: center;
  color: #6b7280;
  font-size: 14px;
}
.auth-footer a {
  color: #409eff;
}
</style>
