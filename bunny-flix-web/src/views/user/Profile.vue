<template>
  <div class="page-card">
    <el-descriptions title="我的信息" :column="2" border>
      <el-descriptions-item label="用户ID">{{ userStore.userId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="手机号">{{ userStore.phone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用户类型">{{ userStore.userType || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider content-position="left">修改密码</el-divider>
    <el-form :model="pwdForm" label-width="100px" style="max-width: 420px">
      <el-form-item label="原密码">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleChangePassword">确认修改</el-button>
      </el-form-item>
    </el-form>

    <el-divider content-position="left">关注管理</el-divider>
    <div class="toolbar">
      <el-input v-model="followUserId" placeholder="输入要关注的用户ID" style="width: 220px" />
      <el-button type="success" @click="handleFollow">关注</el-button>
      <el-button type="danger" @click="handleUnfollow">取关</el-button>
    </div>
    <el-empty description="在此管理关注关系" />
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword, follow, unfollow } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const saving = ref(false)
const followUserId = ref('')
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

const handleChangePassword = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  saving.value = true
  try {
    await changePassword(pwdForm.oldPassword, pwdForm.newPassword)
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
  } finally {
    saving.value = false
  }
}

const handleFollow = async () => {
  if (!followUserId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }
  await follow(followUserId.value)
  ElMessage.success('关注成功')
}

const handleUnfollow = async () => {
  if (!followUserId.value) {
    ElMessage.warning('请输入用户ID')
    return
  }
  await unfollow(followUserId.value)
  ElMessage.success('已取关')
}
</script>
