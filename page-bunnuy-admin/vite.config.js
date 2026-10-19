import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: './',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    open: false,
    cors: true,
    proxy: {
      // 后台登录请求走网关
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
