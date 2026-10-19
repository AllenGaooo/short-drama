<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="28" color="#fff"><VideoCamera /></el-icon>
        <span>兔兔短剧</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1f2937"
        text-color="#cbd5e1"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/drama">
          <el-icon><Film /></el-icon>
          <span>短剧管理</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>短剧搜索</span>
        </el-menu-item>
        <el-menu-item index="/video">
          <el-icon><VideoPlay /></el-icon>
          <span>视频管理</span>
        </el-menu-item>
        <el-menu-item index="/recommend">
          <el-icon><DataAnalysis /></el-icon>
          <span>推荐管理</span>
        </el-menu-item>
        <el-menu-item index="/marketing">
          <el-icon><Present /></el-icon>
          <span>营销中心</span>
        </el-menu-item>
        <el-menu-item index="/member">
          <el-icon><Medal /></el-icon>
          <span>会员管理</span>
        </el-menu-item>
        <el-menu-item index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/profile">
          <el-icon><Setting /></el-icon>
          <span>个人中心</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">{{ pageTitle }}</div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-dropdown">
              <el-icon><UserFilled /></el-icon>
              <span>{{ userStore.phone || '未登录' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/drama')) return '/drama'
  return route.path
})
const pageTitle = computed(() => route.meta.title || '')

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background: #1f2937;
  overflow-x: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}
.header-left {
  font-size: 16px;
  font-weight: 600;
}
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #374151;
  outline: none;
}
.main {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
