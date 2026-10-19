import { createWebHistory, createRouter } from 'vue-router'

const routes = [
  {
    path: '/',
    component: () => import('@/views/HomePage.vue'),
    children: [
      // 首页：热门短剧，每行6个，滚动加载
      { path: '', component: () => import('@/views/DramaList.vue'), props: { mode: 'home', title: '热门推荐' } },
      // 分类
      { path: 'category', component: () => import('@/views/DramaList.vue'), props: { mode: 'category', title: '分类浏览' } },
      // 榜单
      { path: 'rank', component: () => import('@/views/RankPage.vue') },
      // 详情
      { path: 'drama/:id', component: () => import('@/views/DramaDetail.vue') },
      // 播放
      { path: 'play/:episodeId?', component: () => import('@/views/play/PlayPage.vue') }
    ]
  },
  { path: '/login', component: () => import('@/views/loginPage.vue') },
  { path: '/play/:episodeId?', component: () => import('@/views/play/PlayPage.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

export default router
