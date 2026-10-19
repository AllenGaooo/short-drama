import { createWebHistory, createRouter } from 'vue-router'
import userStore from '@/store/userStore.js'
import { buildRoutes } from '@/utils/menu.js'

// 静态路由：登录、注册、主框架（含欢迎页）
const routes = [
  { path: '/login', component: () => import('@/views/loginPage.vue') },
  { path: '/register', component: () => import('@/views/registerPage.vue') },
  {
    path: '/main',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/main/welcome',
    children: [
      { path: 'welcome', name: 'welcome', component: () => import('@/views/main/welcome.vue'), meta: { title: '欢迎页' } },
    ],
  },
  { path: '/', redirect: '/login' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 菜单动态子路由（仅注册一次），供登录、刷新、注册后复用
export function ensureMenuRoutes() {
  const store = userStore()
  const tree = store.menus || []
  const dynamic = buildRoutes(tree)
  dynamic.forEach((r) => {
    if (!router.hasRoute(r.name)) {
      router.addRoute('main', r)
    }
  })
}

// 全局前置守卫：刷新 /main 后，若 store 有菜单则重注册动态子路由
router.beforeEach((to, from, next) => {
  if (to.path.startsWith('/main') && !to.matched.length) {
    // 路由未匹配：尝试用持久化的菜单重建
    const store = userStore()
    if (store.menus && store.menus.length) {
      ensureMenuRoutes()
      // 重建后重新解析目标路由
      next({ ...to, replace: true })
      return
    }
  }
  next()
})

export default router
