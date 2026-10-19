<template>
  <div class="welcome">
    <!-- 管理员信息卡 -->
    <el-card class="profile-card" shadow="hover">
      <div class="profile">
        <el-avatar :size="56" src="https://cdn.bunnyflix.com/default-avatar.png">
          <el-icon><User /></el-icon>
        </el-avatar>
        <div class="profile-info">
          <div class="profile-name">
            欢迎回来，管理员
            <el-tag v-if="adminRole" size="small" type="warning" effect="plain">{{ adminRole }}</el-tag>
          </div>
          <div class="profile-sub">管理员 ID：{{ userId }} · 当前拥有 {{ menuCount }} 个菜单权限</div>
        </div>
        <el-button text :icon="Refresh" @click="loadMenus" :loading="loading">刷新菜单</el-button>
      </div>
    </el-card>

    <!-- 加载态 -->
    <el-card v-if="loading" shadow="never">
      <el-skeleton :rows="4" animated />
    </el-card>

    <!-- 空态 -->
    <el-empty v-else-if="!tree.length" description="暂无菜单数据，请联系超级管理员分配权限">
      <el-button type="primary" @click="loadMenus">重新查询</el-button>
    </el-empty>

    <!-- 动态菜单栏（来自数据库 admin_menu，按当前管理员下发） -->
    <template v-else>
      <el-card
        v-for="cat in tree"
        :key="cat.id"
        class="menu-card"
        shadow="hover"
        :body-style="{ padding: '12px 16px' }"
      >
        <template #header>
          <div class="cat-header">
            <el-icon class="cat-icon"><component :is="iconOf(cat.icon)" /></el-icon>
            <span class="cat-title">{{ cat.name }}</span>
            <el-tag size="small" type="info" effect="plain">{{ cat.children?.length || 0 }} 项</el-tag>
          </div>
        </template>

        <div class="menu-grid">
          <div
            v-for="child in (cat.children || [])"
            :key="child.id"
            class="menu-item"
            :class="{ disabled: child.type === 3 }"
            @click="onMenuClick(child)"
          >
            <el-icon v-if="child.icon" class="item-icon"><component :is="iconOf(child.icon)" /></el-icon>
            <span class="item-name">{{ child.name }}</span>
            <el-tag
              v-if="child.type === 3"
              size="small"
              type="success"
              effect="plain"
              class="item-tag"
            >按钮</el-tag>
            <el-tag
              v-else-if="child.type === 2"
              size="small"
              effect="plain"
              class="item-tag"
            >页面</el-tag>
          </div>
        </div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Refresh, Document, Platform, List, Present, Ticket, Menu as MenuIcon } from '@element-plus/icons-vue'
import userStore from '@/store/userStore.js'
import { getMenus } from '@/api/user.js'
import { buildMenuTree } from '@/utils/menu.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userId = computed(() => userStore().userId)

// 从数据库查询到的扁平菜单列表 + 树
const rawList = ref([])
const tree = computed(() => buildMenuTree(rawList.value))
const loading = ref(false)
const menuCount = computed(() => rawList.value.length)
const adminRole = ref('')

// icon 字段 -> Element Plus 图标组件
const iconMap = {
  User, Platform, Present, List, Ticket, Document, Menu: MenuIcon
}
const iconOf = (icon) => iconMap[icon] || Document

// 进入页面后从数据库动态查询当前管理员的菜单
const loadMenus = () => {
  loading.value = true
  return getMenus()
    .then((res) => {
      const list = res.data || []
      rawList.value = list
      // 同步回 store，保证左侧 MainLayout 菜单栏也用同一份最新数据
      userStore().menus = buildMenuTree(list)
    })
    .catch((err) => {
      ElMessage.error((err && err.message) || '菜单查询失败')
    })
    .finally(() => {
      loading.value = false
    })
}

// 点击菜单：type=2 页面跳转，type=3 按钮仅提示
const onMenuClick = (node) => {
  if (node.type === 3) {
    ElMessage.info(`「${node.name}」为按钮权限（${node.permission || '无标识'}）`)
    return
  }
  if (node.path) {
    router.push(node.path)
  } else {
    ElMessage.warning(`「${node.name}」未配置路由路径`)
  }
}

onMounted(() => {
  // 优先复用登录时已拉取的菜单；为空时再从数据库查询
  const stored = userStore().menus
  if (stored && stored.length) {
    rawList.value = flatten(stored)
  } else {
    loadMenus()
  }
})

// 把树拍平成列表（用于 menuCount / 复用）
const flatten = (nodes) => {
  const out = []
  const walk = (list) => list.forEach((n) => {
    out.push(n)
    if (n.children && n.children.length) walk(n.children)
  })
  walk(nodes)
  return out
}
</script>

<style scoped>
.welcome { display: flex; flex-direction: column; gap: 16px; }
.profile-card .profile { display: flex; align-items: center; gap: 14px; }
.profile-info { flex: 1; }
.profile-name { font-size: 16px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
.profile-sub { font-size: 13px; color: #909399; margin-top: 4px; }

.cat-header { display: flex; align-items: center; gap: 8px; }
.cat-icon { color: #626aef; }
.cat-title { font-weight: 600; font-size: 15px; }

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}
.menu-item:hover { border-color: #626aef; background: #f0f2ff; transform: translateY(-1px); }
.menu-item.disabled { cursor: default; opacity: 0.75; }
.menu-item.disabled:hover { border-color: #ebeef5; background: #fafafa; transform: none; }
.item-icon { color: #626aef; }
.item-name { flex: 1; font-size: 14px; }
.item-tag { margin-left: auto; }
</style>
