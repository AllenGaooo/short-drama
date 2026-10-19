<template>
  <div class="home">
    <!-- 顶部导航 -->
    <header class="topbar">
      <div class="topbar-inner">
        <div class="logo" @click="goHome">
          <span class="logo-mark">🐰</span>
          <span class="logo-text">兔兔短剧</span>
        </div>

        <nav class="nav">
          <span class="nav-item" :class="{ active: current === 'home' }" @click="go('home')">首页</span>
          <span class="nav-item" :class="{ active: current === 'category' }" @click="go('category')">分类</span>
          <span class="nav-item" :class="{ active: current === 'rank' }" @click="go('rank')">榜单</span>
        </nav>

        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索短剧"
            size="small"
            clearable
            @keyup.enter="onSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>

        <div class="user-area">
          <el-button v-if="!isLogin" type="primary" size="small" @click="goLogin">登录</el-button>
          <el-dropdown v-else @command="onUserCommand">
            <span class="user-name">
              <el-icon><User /></el-icon>{{ nickname }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- 内容区 -->
    <main class="container">
      <router-view />
    </main>

    <footer class="footer">© 2026 兔兔短剧 · 仅供学习演示</footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, User, ArrowDown } from '@element-plus/icons-vue'
import { getToken, removeToken } from '@/utils/request.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const isLogin = ref(false)
const nickname = ref('我的')

const current = computed(() => {
  const p = route.path
  if (p.includes('/category')) return 'category'
  if (p.includes('/rank')) return 'rank'
  return 'home'
})

onMounted(() => {
  isLogin.value = !!getToken()
})

const go = (tab) => {
  if (tab === 'home') router.push('/')
  else if (tab === 'category') router.push('/category')
  else if (tab === 'rank') router.push('/rank')
}

const goHome = () => router.push('/')
const goLogin = () => router.push('/login')

const onSearch = () => {
  if (!keyword.value.trim()) return
  router.push({ path: '/', query: { keyword: keyword.value.trim() } })
  // 触发首页重新加载
  window.dispatchEvent(new CustomEvent('drama-search', { detail: keyword.value.trim() }))
}

const onUserCommand = (cmd) => {
  if (cmd === 'logout') {
    removeToken()
    isLogin.value = false
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.home { min-height: 100vh; display: flex; flex-direction: column; background: #f5f6f8; }

.topbar {
  background: linear-gradient(90deg, #2b2f4a 0%, #3a3f63 100%);
  color: #fff;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(0,0,0,.15);
}
.topbar-inner {
  max-width: 1400px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 0 24px;
}
.logo { display: flex; align-items: center; gap: 6px; cursor: pointer; flex-shrink: 0; }
.logo-mark { font-size: 22px; }
.logo-text { font-size: 18px; font-weight: 700; letter-spacing: 1px; }

.nav { display: flex; gap: 24px; flex-shrink: 0; }
.nav-item {
  cursor: pointer;
  font-size: 15px;
  color: #c7cade;
  padding: 6px 2px;
  border-bottom: 2px solid transparent;
  transition: all .2s;
}
.nav-item:hover { color: #fff; }
.nav-item.active { color: #fff; font-weight: 600; border-bottom-color: #ff7a45; }

.search-box { flex: 1; max-width: 320px; }
.user-area { margin-left: auto; flex-shrink: 0; }
.user-name {
  display: flex; align-items: center; gap: 4px;
  cursor: pointer; color: #fff; font-size: 14px; outline: none;
}

.container { flex: 1; max-width: 1400px; width: 100%; margin: 0 auto; padding: 24px; box-sizing: border-box; }
.footer { text-align: center; color: #999; font-size: 12px; padding: 20px 0; }
</style>
