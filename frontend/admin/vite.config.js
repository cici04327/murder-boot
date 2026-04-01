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
    port: 3000,
    host: '0.0.0.0',
    hmr: {
      protocol: 'ws',
      host: 'localhost',
      port: 3000,
      clientPort: 3000,
      overlay: false  // 禁用错误遮罩层
    },
    proxy: {
      // 所有API请求统一通过Spring Boot应用转发
      '/api': {
        target: 'http://localhost:8080',  // murder-boot 应用端口
        changeOrigin: true,
        ws: true  // 支持WebSocket
      },
      // 上传/图片等静态资源由后端 /upload/** 提供，开发环境需要同样代理
      '/upload': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
