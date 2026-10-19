# 兔兔短剧 · 前端 (Vite + Vue 3)

基于你的 Spring Boot 微服务后端生成的现代前端管理后台。

## 技术栈

- **Vite 5** + **Vue 3**（`<script setup>`）
- **Vue Router 4** + **Pinia**（状态管理）
- **Element Plus**（UI 组件库）
- **Axios**（HTTP 请求，自动携带 JWT token）

## 快速开始

```bash
cd bunny-flix-web
npm install
npm run dev
```

启动后访问 `http://localhost:5173`

## 后端对接

Vite 开发服务器已配置代理，将 `/api` 前缀的请求转发到网关 `http://localhost:8080`。

```js
// vite.config.js
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

所以只需保证后端 **gateway(8080)** 及各微服务已启动即可联调。

## 鉴权

- 登录/注册成功后，后端返回 `token`，前端存入 `localStorage`。
- 每次请求通过 axios 拦截器自动添加 `Authorization: Bearer <token>`。
- 401 时自动登出并跳转登录页。

## 功能页面

| 路由 | 说明 | 对应后端服务 |
|---|---|---|
| `/login` `/register` | 登录 / 注册 | service-user |
| `/drama` | 短剧列表 / 增删改查 | service-drama |
| `/drama/detail/:id` | 短剧详情 | service-drama |
| `/search` | 短剧搜索（ES） | service-search |
| `/video` | 视频管理 | service-video |
| `/recommend` | 推荐管理 | service-recommend |
| `/marketing` | 营销中心（优惠券/签到） | service-marketing |
| `/member` | 会员管理 | service-member |
| `/user` | 用户管理 | service-user |
| `/profile` | 个人中心 | service-user |

## 目录结构

```
bunny-flix-web/
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── api/           # 各服务的 API 封装
    ├── layout/        # 主布局（侧边栏 + 顶栏）
    ├── router/        # 路由 + 登录守卫
    ├── stores/        # Pinia（user）
    ├── styles/        # 全局样式
    ├── utils/         # axios 封装
    └── views/         # 页面组件
```

## 生产构建

```bash
npm run build
```

产物在 `dist/` 目录。生产环境请将 `/api` 通过 Nginx 等反向代理到网关 `:8080`。
