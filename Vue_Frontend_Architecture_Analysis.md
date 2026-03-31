# Vue3+Vite 前端项目结构分析

## 项目概览
- **项目名称**: 剧本杀在线预约平台（Murder Mystery Booking System）
- **技术栈**: Vue3 + Vite + Pinia + Element Plus + Axios
- **两套独立前端应用**:
  - **Admin Frontend** (port 3000): 管理后台 - 供管理员、门店、员工使用
  - **User Frontend** (port 3001): C端应用 - 供普通用户使用
- **后端**: Spring Boot 单体应用 (port 8080，/api 统一前缀)

---

## 一、Admin 前端架构

### 1. 页面域（Views）划分

| 模块 | 路由前缀 | 主要功能 | 关键文件 |
|------|--------|--------|--------|
| **门店管理** | `/store` | 门店列表、员工、房间、DM、统计、营收日报 | `store/list.vue`, `operation-board.vue`, `daily-report.vue` |
| **剧本管理** | `/script` | 剧本列表、分类、角色、排期、评价 | `script/list.vue`, `category.vue`, `schedule.vue` |
| **预约管理** | `/reservation` | 预约列表、新建、编辑、详情、退款 | `reservation/list.vue`, `add.vue`, `refund.vue` |
| **员工工作** | `/staff` | DM专用 - 我的场次、我的预约 | `staff/my-schedule.vue`, `my-reservation.vue` |
| **用户管理** | `/user` | 用户列表、详情、积分、地址 | `user/list.vue`, `detail.vue`, `points.vue` |
| **VIP管理** | `/vip` | VIP套餐配置 | `vip/packages.vue` |
| **优惠券管理** | `/coupon` | 优惠券列表、用户优惠券 | `coupon/list.vue`, `user-coupon.vue` |
| **通知中心** | `/notification` | 通知列表、发送 | `notification/index.vue` |
| **留言管理** | `/feedback` | 反馈列表、回复 | `feedback/list.vue` |
| **客服中心** | `/service` | 客服会话管理 | `service/index.vue` |
| **工作台** | `/dashboard` | 首页数据聚合 | `dashboard/index.vue` |

### 2. 路由组织结构

**关键特点**:
- 三级角色支持: `admin`(管理员) / `store`(门店经理) / `staff`(员工如DM)
- 权限粒度: 路由级 + 权限码级
  - 路由级: `meta.roles` 和 `meta.staffRoles` 控制可访问角色
  - 权限码级: `meta.permissionCodes` 如 `'employee:manage'`, `'report:view'`, `'refund:process'`
- 重定向逻辑: 根据 `admin-login-type` (localStorage) 智能导向首页
  - 管理员 → `/dashboard`
  - 门店 → `/store/operation-board`
  - DM员工 → `/staff/my-schedule`
- 隐藏路由: 详情/编辑页通过 `hidden: true` + `activeMenu` 管理面包屑

**核心文件**: `frontend/admin/src/router/index.js` (384行)

### 3. API 分层

**API 模块划分** (`frontend/admin/src/api/`):
- `coupon.js`: 优惠券 CRUD + 统计
- `dm.js`: DM 管理
- `feedback.js`: 留言反馈
- `notification.js`: 通知
- `schedule.js`: 排期 (含冲突检查、批量添加、生成、日报)
- `store-employee.js`: 员工管理
- `service.js`: 客服会话
- `vip.js`: VIP 套餐 + 用户 VIP 权限

**HTTP 客户端层**:
- 基础实例: `service` (axios)
- 专用实例: `userService`, `reservationService` (响应拦截分开处理)
- 所有实例统一配置:
  - `baseURL: '/api'`
  - `timeout: 10000`
  - 重试机制: 网络错误最多重试 3 次，延迟 1000ms

**请求/响应拦截**:
- 请求头: 
  - `token` (Spring Boot 期望的鉴权头)
  - `X-Client-Type: 'admin'` (标记管理端)
  - `X-Store-Id` (门店登录时携带门店ID)
- 请求参数注入: GET 请求自动在 query 添加 `storeId`；POST/PUT 自动在 body 添加 `storeId`
- 响应判断: `code === 1 || code === 200` 为成功
- 错误处理: 401 清空认证信息并跳转登录

### 4. 状态管理/鉴权方式

**状态管理**: 
- 无 Pinia store (admin 端) - 全量使用 localStorage
- 状态存储位置:
  - `admin-token`: JWT token
  - `admin-login-type`: 登录类型 ('admin'/'store'/'staff')
  - `admin-user`: 用户信息对象 (包含 `staffRole`, `permissionCodes`)
  - `admin-store-id`: 门店ID
  - `admin-current-store-id`: 当前选中门店ID

**鉴权机制**:
- 核心函数: `frontend/admin/src/utils/permission.js`
  - `getCurrentRole()`: 从 localStorage 获取当前角色
  - `getStaffRole()`: 获取员工特定角色 (如 'DM')
  - `getPermissionCodes()`: 解析权限码集合
  - `hasRoutePermission(route)`: 路由守卫检查 (角色 + staffRole + 权限码)
  
- 路由守卫 (beforeEach):
  ```javascript
  - /login 白名单
  - 无 token → 重定向到登录
  - 有 token 但权限不足 → 基于角色重定向 (DM→/staff/my-schedule, 其他→/dashboard)
  ```

**登录流程**:
1. 填表登录 (SliderCaptcha 滑块验证)
2. 后端返回 `{ code, data: { token, username, permissionCodes, staffRole } }`
3. 存入 localStorage + 路由守卫验证 + 跳转相应首页

---

## 二、User 前端架构

### 1. 页面域（Views）划分

| 模块 | 路由前缀 | 主要功能 | 关键文件 |
|------|--------|--------|--------|
| **首页/发现** | `/home` | 轮播、热门剧本、推荐、快捷入口 | `home/index.vue` |
| **剧本大厅** | `/script` | 剧本列表、详情、收藏、评价 | `script/list.vue`, `detail.vue`, `detail-enhanced.vue` |
| **门店列表** | `/store` | 门店列表、门店详情 | `store/list.vue`, `detail.vue` |
| **排期大厅** | `/schedule/list` | 场次展示、过滤 | `schedule/list.vue` |
| **预约流程** | `/reservation/*` | 选择场次、创建预约、支付、详情、退款、评价 | `reservation/create.vue`, `confirm.vue`, `detail.vue`, `refund.vue`, `review.vue`, `schedule-select.vue` |
| **支付** | `/payment/*` | 支付页、支付结果 | `payment/index.vue`, `result.vue` |
| **个人中心** | `/user/*` | 资料、预约、优惠券、地址、积分、消息、收藏、历史、设置、留言、数据统计 | `user/profile-enhanced.vue`, `reservations.vue`, `coupons.vue`, `statistics.vue` |
| **VIP中心** | `/vip` | VIP 套餐购买 | `vip/index.vue` |
| **拼单** | `/group` | 拼单列表、详情、创建 | `group/list.vue`, `detail.vue` |
| **内容** | `/article`, `/recommend`, `/search` | 资讯、智能推荐、全局搜索 | `article/list-enhanced.vue`, `recommend/enhanced.vue`, `search/index.vue` |
| **信息** | `/about`, `/contact`, `/help`, `/agreement` | 关于、联系、帮助、协议 | `info/about.vue` 等 |

### 2. 路由组织结构

**关键特点**:
- 单一 Layout 容器，所有页面都在 `/` 下的 children
- 鉴权字段: `meta.requireAuth` (布尔值)
  - `true`: 必须登录才能访问 (用户中心、预约等)
  - `false` / 无: 无需登录 (首页、剧本详情等)
- 路由守卫逻辑:
  ```javascript
  if (to.meta.requireAuth && !userStore.isLoggedIn) {
    // 拦截到登录页，携带 redirect 参数
    next({ path: '/login', query: { redirect: to.fullPath } })
  }
  ```
- 滚动行为: 
  - 保留浏览器后退位置
  - 支持 hash 平滑滚动
  - 默认滚动到顶部

**核心文件**: `frontend/user/src/router/index.js` (327行)

### 3. API 分层

**API 模块划分** (`frontend/user/src/api/`):
- `user.js`: 登录、注册、信息、地址、积分、邀请、通知、设置、实名认证等 (404行，最全)
- `reservation.js`: 创建、详情、支付、改期、取消、完成、退款等
- `script.js`: 剧本列表、详情、收藏、评价、推荐
- `store.js`: 门店列表、详情
- `article.js`: 资讯列表、详情、评论
- `group.js`: 拼单模块
- `search.js`: 全局搜索
- `statistics.js`: 用户统计数据
- `history.js`: 浏览历史
- `recommendation.js`: 推荐算法
- `coupon.js`: 优惠券
- `feedback.js`: 留言反馈
- `vip.js`: VIP 相关
- `ai.js`: AI 客服对话
- `service.js`: 客服会话 (user 端也有)

**HTTP 客户端层** (`frontend/user/src/utils/request.js`):
- 单实例: `axios` 
- 配置: 
  - `baseURL: '/api'`
  - `timeout: 30000` (考虑 AI 对话耗时)
- 请求头: `token` (user 端使用 userStore 的 token)
- 静默错误白名单: 某些接口 (评价列表为空、收藏检查等) 只输出 console.warn，不 toast
- 重试机制: GET 请求网络错误最多重试 3 次，延迟逐步增加 (500ms, 1000ms, 1500ms)

**响应判断**: `code === 1 || code === 200` 为成功

### 4. 状态管理/鉴权方式

**状态管理**: Pinia + localStorage
- Store 名: `useUserStore` (`frontend/user/src/store/user.js`)
- 状态字段:
  - `token` (ref)
  - `userInfo` (ref: { id, username, nickname, avatar, ... })
  - `isLoggedIn` (computed: !!token)
  - `userId`, `username`, `nickname`, `avatar` (computed)

- 核心方法:
  - `login(loginForm)`: 调用 user API，保存 token + userInfo 到 localStorage
  - `register(registerForm)`: 注册新用户
  - `logout()`: 清空 token + userInfo，删除 localStorage
  - `loadUserFromStorage()`: 应用启动时从 localStorage 恢复状态
  - `loadUserInfo()`: 从服务器同步最新用户信息
  - `updateUserInfo(newInfo)`: 本地快速更新

**鉴权机制**:
- 路由守卫在 `router.beforeEach()` 中检查 `userStore.isLoggedIn`
- 响应拦截器: 401 错误调用 `userStore.logout()` 并重定向到登录
- 登录页支持跳转回源: `query.redirect` 参数

**登录流程**:
1. 用户输入用户名/邮箱/手机号 + 密码
2. 通过 SliderCaptcha 滑块验证
3. 调用 `/user/login` API，后端返回 `{ code, data: { token, ...user } }`
4. userStore 保存 token 和 userInfo 到 localStorage
5. 路由守卫验证通过，重定向到目标页面或首页

---

## 三、典型用户路径

### User 端用户路径

**场景 1: 未登录用户浏览剧本并预约**
```
首页 (/home)
  ↓ 点击"浏览剧本"
剧本列表 (/script)
  ↓ 点击剧本卡片
剧本详情 (/script/:id 或 /script/:id/enhanced)
  ↓ 点击"立即预约"
登录页 (/login) [路由守卫拦截]
  ↓ 登录成功
预约页-步骤1: 选择剧本 (/reservation/create)
  ↓ 已预填剧本信息
预约页-步骤2: 选择门店和场次
  ↓ 场次选择页或来自 /schedule/list
预约页-步骤3: 确认信息和人数
  ↓ 检查房间可用性
预约确认页 (/reservation/confirm/:id)
  ↓ 点击"支付"
支付页 (/payment/:id)
  ↓ 支付成功回调
支付结果页 (/payment/result)
  ↓ 跳转到预约详情或个人中心
我的预约 (/user/reservations)
```

**场景 2: 已登录用户快速查看推荐并拼单**
```
首页 (/home) [自动加载推荐、拼单信息]
  ↓ 点击"为你推荐" 或 "拼单大厅"
推荐页 (/recommend 或 /recommend/enhanced)
或 拼单列表 (/group)
  ↓ 点击商品卡片
拼单详情 (/group/:id) 或 推荐详情
  ↓ 点击"参与拼单"或"预约此场次"
排期选择 (/reservation/schedule-select)
  ↓ 选择具体场次
预约创建 (/reservation/create?scheduleId=xxx) [排期驱动模式]
  ↓ 简化步骤，跳过"选剧本"和"选门店"
预约确认 (/reservation/confirm/:id)
  ↓ 支付 → 完成
```

**场景 3: 用户售后流程**
```
我的预约 (/user/reservations)
  ↓ 点击预约卡片
预约详情 (/reservation/detail/:id)
  ↓ 点击"申请退款"或"评价订单"
退款申请 (/reservation/refund)
或 评价页 (/reservation/review/:id)
  ↓ 提交申请/评价
完成，返回预约列表
```

**场景 4: 用户个人中心自服务**
```
个人中心 (/user/profile)
  ↓ 可选操作：
    - 修改资料 (updateUserInfo)
    - 管理地址 (/user/addresses)
    - 查看积分 (/user/points, 含每日签到、兑换优惠券)
    - 我的优惠券 (/user/coupons)
    - 消息通知 (/user/notifications, WebSocket 实时推送)
    - 我的收藏 (/user/favorites-enhanced)
    - 浏览历史 (/user/history)
    - 账号设置 (/user/settings, 绑定手机/邮箱、实名认证、隐私设置)
    - 数据统计 (/user/statistics)
```

### Admin 端用户路径

**场景 1: 超级管理员的工作流**
```
登录页 (/login) 
  ↓ 以 admin 身份登录
工作台 (/dashboard) [自动重定向]
  ↓ 查看今日汇总卡片 (预约数、退款数、评价数、通知数)
快速入口或菜单点击
  ↓ 点击"预约管理"
预约列表 (/reservation/list)
  ↓ 过滤、搜索、分页
  ↓ 点击预约卡片
预约详情 (/reservation/detail/:id)
  ↓ 可编辑字段、查看评价、处理退款
或 点击"门店管理"
门店列表 (/store/list)
  ↓ 点击门店
门店详情/编辑 (通过 add.vue 或 edit.vue)
  ↓ 配置基本信息、房间、员工等
或 点击"剧本管理"
剧本列表 (/script/list)
  ↓ 点击剧本或编辑分类、角色、排期
```

**场景 2: 门店经理的工作流**
```
登录页 (/login)
  ↓ 以 store 身份登录 (loginType=store)
自动重定向到经营看板 (/store/operation-board)
  ↓ 查看今日营收、房间使用率、预约数
  ↓ 点击菜单或快捷入口
员工管理 (/store/employee)
  ↓ 添加/编辑员工，分配权限
或 排期管理 (/script/schedule)
  ↓ 批量添加/生成排期
或 预约列表 (/reservation/list)
  ↓ 查看自己门店的预约 [自动过滤 storeId]
或 营收日报 (/store/daily-report)
  ↓ 查看营收统计
或 房间管理 (/store/room)
  ↓ 管理房间信息、容纳人数等
```

**场景 3: DM 员工的工作流**
```
登录页 (/login)
  ↓ 以 staff 身份登录 (loginType=staff, staffRole=DM)
自动重定向到我的场次 (/staff/my-schedule)
  ↓ 查看今日及后续要主持的场次
  ↓ 点击场次查看预约人员
或 我的预约 (/staff/my-reservation)
  ↓ 查看对应的预约信息
或 预约管理 (/reservation/list)
  ↓ 仅能查看已分配的预约 [permissionCode: reservation:view]
或 通知中心 (/notification/index)
  ↓ 查看系统通知和分配任务
或 经营看板 (/store/operation-board) [permissionCode: report:view]
  ↓ 查看门店报表 (某些权限)
```

---

## 四、关键技术细节

### 前端构建与部署
- **Build 工具**: Vite 4 (admin) / Vite 5 (user)
- **Vue 版本**: Vue 3.3.4 (admin) / Vue 3.4.0 (user)
- **发开端口**: 
  - Admin: 3000 (proxy → http://localhost:8080/api)
  - User: 3001 (proxy → http://localhost:8080/api)
- **生产构建**: `vite build`
  - 输出目录: `dist/` (从 Dockerfile 可看到 nginx 使用)
  - 静态资源缓存: `/assets/` 目录

### 组件库和 UI 框架
- **Element Plus**: 2.3.14 (admin) / 2.5.0 (user)
- **Icon 库**: @element-plus/icons-vue
- **自定义组件**: 
  - SliderCaptcha (滑块验证码)
  - NotificationBell (通知铃铛)
  - SkeletonHome, SkeletonScriptCard 等 (骨架屏)
  - LazyImage (图片懒加载)
  - ShareButtons, PageAnchor 等 (user 端增强)

### 高级特性
- **PWA 支持**: User 端有 service worker (`public/sw.js`)，但已禁用
- **图片懒加载**: 自定义 `lazy` 指令 (user 端)
- **主题切换**: 论文浅色主题 vs 项目默认深色主题 (user 端)
- **路由过渡动画**: `fade-slide` 动画 (user 端)
- **WebSocket**: 通知、客服实时通讯 (admin + user 都支持)
- **AI 客服**: `AICustomerService.vue` 和 `AICustomerServiceEnhanced.vue` (user 端)

---

## 五、核心文件索引

### Admin 前端
| 目录 | 关键文件 | 功能 |
|-----|--------|------|
| `/router` | `index.js` | 路由定义 (10个一级菜单) |
| `/utils` | `permission.js` | 权限检查 (角色+权限码) |
| `/utils` | `request.js` | 请求/响应拦截 (3个专用实例) |
| `/store` | ❌ 无 | 全量使用 localStorage |
| `/api` | `*.js` (9 个文件) | 各业务域 API 封装 |
| `/layout` | `index.vue` | 侧边栏菜单 + 顶部栏 + 面包屑 |
| `/views` | 各菜单对应目录 | 业务页面 |

### User 前端
| 目录 | 关键文件 | 功能 |
|-----|--------|------|
| `/router` | `index.js` | 路由定义 (单 layout，30+ 页面) |
| `/store` | `user.js` | Pinia 状态管理 (登录、用户信息) |
| `/utils` | `request.js` | 请求/响应拦截 (静默错误、重试) |
| `/api` | `*.js` (14 个文件) | 各业务域 API 封装 |
| `/layout` | `index.vue` | 统一容器 + 路由视图 + 主题切换按钮 |
| `/views` | 各页面对应目录 | 业务页面 |
| `/directives` | `lazy.js` | 图片懒加载指令 |
| `/composables` | `*.js` | `usePagination`, `useScrollReveal`, `useTheme` |
| `/styles` | `*.css` | 主题、动画、element-plus 覆盖 |

---

## 六、总结对比

| 维度 | Admin 后台 | User C端 |
|-----|----------|--------|
| **菜单结构** | 10 个一级菜单，3 级路由深度 | 单 Layout，30+ 平面路由 |
| **权限模型** | 3 层: 角色(admin/store/staff) + 员工类型(DM) + 权限码 | 简单: 登录状态 + requireAuth 标记 |
| **状态管理** | 纯 localStorage，无 Pinia | Pinia store + localStorage 同步 |
| **API 实例** | 多专用实例 (service/userService/reservationService) | 单一实例 |
| **错误处理** | 所有错误 toast | 部分接口静默处理 |
| **核心流程** | 多角色权限检查 → 相应菜单展示 | 登录流程 → 业务模块自服务 |
| **UI 库版本** | Element Plus 2.3.14 | Element Plus 2.5.0 |

