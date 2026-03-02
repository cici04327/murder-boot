# Murder Boot - 前端项目

本目录包含 Murder Boot 系统的前端项目（用户端和管理端）。

## 📁 目录结构

```
frontend/
├── user/          # 用户前端（端口3001）
│   ├── src/       # 源代码
│   ├── public/    # 静态资源
│   └── package.json
├── admin/         # 管理端前端（端口3000）
│   ├── src/       # 源代码
│   └── package.json
└── README.md      # 本文件
```

## 🚀 快速启动

### 1. 安装依赖

**用户前端：**
```bash
cd frontend/user
npm install
```

**管理端前端：**
```bash
cd frontend/admin
npm install
```

### 2. 启动开发服务器

**用户前端（端口3001）：**
```bash
cd frontend/user
npm run dev
```

**管理端前端（端口3000）：**
```bash
cd frontend/admin
npm run dev
```

### 3. 构建生产版本

**用户前端：**
```bash
cd frontend/user
npm run build
```

**管理端前端：**
```bash
cd frontend/admin
npm run build
```

## 🔗 访问地址

- **用户端开发环境**: http://localhost:3001
- **管理端开发环境**: http://localhost:3000
- **后端API**: http://localhost:8080

## ⚙️ 配置说明

### API代理配置

前端项目已配置为连接到 Spring Boot 单体应用（端口8080）：

**用户前端** (`user/vite.config.js`)：
```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true
  }
}
```

**管理端前端** (`admin/vite.config.js`)：
```javascript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    ws: true  // 支持WebSocket
  }
}
```

## 📦 技术栈

### 用户前端
- Vue 3.4.0
- Element Plus 2.5.0
- Vite 5.0.0
- Pinia 2.1.7
- Vue Router 4.2.5
- Axios 1.6.0
- ECharts 5.4.3

### 管理端前端
- Vue 3.3.4
- Element Plus 2.3.14
- Vite 4.4.9
- Pinia 2.1.6
- Vue Router 4.2.4
- Axios 1.5.0
- ECharts 6.0.0

## 🔧 开发说明

### 1. 后端服务必须先启动

前端项目依赖后端API，启动前端前请确保：
- Spring Boot 应用已启动（端口8080）
- MySQL 数据库已启动
- Redis 已启动

### 2. 环境要求

- Node.js >= 16.0.0
- npm >= 8.0.0

### 3. 常见问题

**问题1：无法连接到后端服务**
- 检查 Spring Boot 应用是否在 8080 端口运行
- 检查后端日志是否有错误

**问题2：跨域问题**
- 前端已配置代理，不应出现跨域问题
- 如果仍有问题，检查后端 CORS 配置

**问题3：WebSocket 连接失败**
- 确保后端 WebSocket 配置正确
- 检查防火墙设置

## 📝 与 Spring Cloud 版本的区别

### Spring Cloud 版本（原项目）
- 用户前端连接网关（8080）
- 管理端前端连接网关（8080），部分功能直连微服务（8082, 8085）
- 需要启动多个微服务

### Spring Boot 版本（murder-boot）
- 用户前端连接单体应用（8080）
- 管理端前端连接单体应用（8080）
- 只需启动一个应用

### 配置简化
- ✅ 移除了微服务直连配置
- ✅ 统一所有请求到 Spring Boot 应用
- ✅ 简化了代理配置
- ✅ 保持原有功能不变

## 🎯 启动顺序

1. 启动 MySQL 数据库
2. 启动 Redis
3. 启动 Spring Boot 后端（`murder-boot`）
4. 启动用户前端（可选）
5. 启动管理端前端（可选）

## 📞 获取帮助

如遇到问题，请查看：
- 后端文档：`../README.md`
- 快速启动指南：`../快速开始.md`
- 迁移说明：`../README-迁移说明.md`

---
**版本**: 1.0.0  
**更新时间**: 2025-01-28
