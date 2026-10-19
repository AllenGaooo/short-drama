import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/drama',
    children: [
      {
        path: 'drama',
        name: 'Drama',
        component: () => import('@/views/drama/DramaList.vue'),
        meta: { title: '短剧管理' }
      },
      {
        path: 'drama/detail/:id',
        name: 'DramaDetail',
        component: () => import('@/views/drama/DramaDetail.vue'),
        meta: { title: '短剧详情' }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/SearchIndex.vue'),
        meta: { title: '短剧搜索' }
      },
      {
        path: 'video',
        name: 'Video',
        component: () => import('@/views/video/VideoList.vue'),
        meta: { title: '视频管理' }
      },
      {
        path: 'user',
        name: 'UserList',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'marketing',
        name: 'Marketing',
        component: () => import('@/views/marketing/Marketing.vue'),
        meta: { title: '营销中心' }
      },
      {
        path: 'recommend',
        name: 'Recommend',
        component: () => import('@/views/recommend/Recommend.vue'),
        meta: { title: '推荐管理' }
      },
      {
        path: 'member',
        name: 'Member',
        component: () => import('@/views/member/Member.vue'),
        meta: { title: '会员管理' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人中心' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/drama'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '兔兔短剧'
  const userStore = useUserStore()
  const publicPages = ['/login', '/register']
  if (!publicPages.includes(to.path) && !userStore.isLogin) {
    next('/login')
  } else {
    next()
  }
})

export default router
