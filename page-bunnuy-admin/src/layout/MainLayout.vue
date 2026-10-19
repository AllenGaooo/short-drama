<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon size="22" color="#fff"><Platform /></el-icon>
        <span>Bunny Admin</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#1f2233"
        text-color="#bfc3d4"
        active-text-color="#ffffff"
        class="menu"
      >
        <template v-for="item in menuItems" :key="item.index">
          <!-- 有子菜单 -->
          <el-sub-menu v-if="item.children && item.children.length" :index="item.index">
            <template #title>
              <el-icon v-if="item.icon"><component :is="iconMap[item.icon] || Document" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children.filter(c => c.type !== 3)"
              :key="child.index"
              :index="child.index"
            >
              <el-icon v-if="child.icon"><component :is="iconMap[child.icon] || Document" /></el-icon>
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <!-- 无子菜单（且非按钮） -->
          <el-menu-item v-else-if="item.type !== 3" :index="item.index">
            <el-icon v-if="item.icon"><component :is="iconMap[item.icon] || Document" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧主体 -->
    <el-container>
      <el-header class="header">
        <span class="header-title">管理后台</span>
        <div class="header-right">
          <el-dropdown @command="onCommand">
            <span class="user-dropdown">
              <el-icon><User /></el-icon>
              <span>管理员 (ID: {{ userId }})</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
import { User, ArrowDown, Platform, Document, ChatDotRound } from '@element-plus/icons-vue'
import userStore from '@/store/userStore.js'
import { toMenuItems } from '@/utils/menu.js'

const route = useRoute()
const router = useRouter()
const userId = computed(() => userStore().userId)

// el-menu 需要的菜单数据（按钮 type=3 不渲染）
const menuItems = computed(() => toMenuItems(userStore().menus || []))

const activeMenu = computed(() => route.path)

// 后端菜单 icon 字段 -> Element Plus 图标组件映射
const iconMap = {
  User: User,
  Video: Platform,
  Present: Document,
  List: Document,
  Ticket: Document,
  Agent: ChatDotRound,
  ChatDotRound: ChatDotRound
}

const onCommand = (cmd) => {
  if (cmd === 'logout') {
    userStore().token = ''
    userStore().userId = ''
    userStore().menus = []
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside {
  background-color: #1f2233;
  overflow: hidden;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}
.menu { border-right: none; }
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.header-title { font-weight: 600; }
.user-dropdown {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
}
.main { background: #f0f2f5; padding: 16px; }
</style>
