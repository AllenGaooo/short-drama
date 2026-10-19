import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
console.log("===== vite config loaded =====")
export default defineConfig({
  plugins: [vue()],
  //配置根路径，这个不配置会导致部署之后访问不到
  base: './',
  //  构建
  build: {
    outDir: 'dist', //指定打包输出路径
    assetsDir: 'assets', //指定静态资源存放路径
    cssCodeSplit: true, //css代码拆分,禁用则所有样式保存在一个css里面
    sourcemap: false, //是否构建source map 文件

    // 生产环境取消 console
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    },

    //会打包出 css js 等文件夹 目录显得清晰
    rollupOptions: {
      output: {
        chunkFileNames: 'js/[name]-[hash].js',
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: '[ext]/[name]-[hash].[ext]'
      }
    }
  },
  resolve: {
    alias: {
      //别名，给./src目录，起个别名@，在文件中就可以使用@替换src了
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 本地服务
  server: {
    host: "0.0.0.0", // ip
    port: 5174,  // 端口号
    open: false,  // 是否自动在浏览器打开
    https: false, // 是否开启 https
    cors: true, // 允许跨域
    allowedHosts: true,
    // 接口代理：/api 转发到网关，由网关路由到各微服务
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
