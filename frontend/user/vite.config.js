import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3001,
    host: '0.0.0.0',
    hmr: {
      protocol: 'ws',
      host: 'localhost',
      port: 3001,
      clientPort: 3001,
      overlay: false  // 禁用错误遮罩
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // murder-boot 应用端口
        changeOrigin: true,
        ws: true
      },
      // 上传/图片等静态资源由后端 /upload/** 提供，开发环境需要同样代理
      '/upload': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    },
    watch: {
      ignored: [
        '**/node_modules/**',
        '**/dist/**',
        '**/.git/**',
        '**/public/sw.js',  // 忽略 Service Worker 文件
        '**/*.log'
      ]
    },
    cors: true
  }
})
