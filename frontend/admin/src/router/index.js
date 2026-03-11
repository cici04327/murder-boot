import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled', roles: ['admin', 'store'] }
      }
    ]
  },
  {
    path: '/store',
    component: Layout,
    redirect: '/store/list',
    meta: { title: '门店管理', icon: 'Shop', roles: ['admin'] },
    children: [
      {
        path: 'list',
        name: 'StoreList',
        component: () => import('@/views/store/list.vue'),
        meta: { title: '门店列表', icon: 'List', roles: ['admin'] }
      },
      {
        path: 'add',
        name: 'StoreAdd',
        component: () => import('@/views/store/add.vue'),
        meta: { title: '新增门店', activeMenu: '/store/list', roles: ['admin'] },
        hidden: true
      },
      {
        path: 'statistics',
        name: 'StoreStatistics',
        component: () => import('@/views/store/statistics.vue'),
        meta: { title: '门店统计', icon: 'DataAnalysis', roles: ['admin'] }
      },
      {
        path: 'operation-board',
        name: 'OperationBoard',
        component: () => import('@/views/store/operation-board.vue'),
        meta: { title: '经营看板', icon: 'TrendCharts', roles: ['admin', 'store'] }
      },
      {
        path: 'room',
        name: 'StoreRoom',
        component: () => import('@/views/store/room.vue'),
        meta: { title: '房间管理', icon: 'Operation', roles: ['admin', 'store'] }
      },
      {
        path: 'review',
        name: 'StoreReview',
        component: () => import('@/views/store/review.vue'),
        meta: { title: '评价管理', icon: 'Comment', roles: ['admin', 'store'] }
      },
      {
        path: 'profile',
        name: 'StoreProfile',
        component: () => import('@/views/store/profile.vue'),
        meta: { title: '门店信息', icon: 'Setting', roles: ['store'] }
      }
    ]
  },
  {
    path: '/script',
    component: Layout,
    redirect: '/script/list',
    meta: { title: '剧本管理', icon: 'Reading', roles: ['admin', 'store'] },
    children: [
      {
        path: 'list',
        name: 'ScriptList',
        component: () => import('@/views/script/list.vue'),
        meta: { title: '剧本列表', icon: 'List', roles: ['admin', 'store'] }
      },
      {
        path: 'add',
        name: 'ScriptAdd',
        component: () => import('@/views/script/add.vue'),
        meta: { title: '新增剧本', activeMenu: '/script/list', roles: ['admin'] },
        hidden: true
      },
      {
        path: 'category',
        name: 'ScriptCategory',
        component: () => import('@/views/script/category.vue'),
        meta: { title: '剧本分类', icon: 'Folder', roles: ['admin'] }
      },
      {
        path: 'role',
        name: 'ScriptRole',
        component: () => import('@/views/script/role.vue'),
        meta: { title: '角色管理', icon: 'Avatar', roles: ['admin'] }
      },
      {
        path: 'review',
        name: 'ScriptReview',
        component: () => import('@/views/script/review.vue'),
        meta: { title: '剧本评价', icon: 'Comment', roles: ['admin', 'store'] }
      },
      {
        path: 'schedule',
        name: 'ScriptSchedule',
        component: () => import('@/views/script/schedule.vue'),
        meta: { title: '排期管理', icon: 'Calendar', roles: ['admin', 'store'] }
      }
    ]
  },
  {
    path: '/reservation',
    component: Layout,
    redirect: '/reservation/list',
    meta: { title: '预约管理', icon: 'Calendar', roles: ['admin', 'store'] },
    children: [
      {
        path: 'list',
        name: 'ReservationList',
        component: () => import('@/views/reservation/list.vue'),
        meta: { title: '预约列表', icon: 'List', roles: ['admin', 'store'] }
      },
      {
        path: 'add',
        name: 'ReservationAdd',
        component: () => import('@/views/reservation/add.vue'),
        meta: { title: '新建预约', activeMenu: '/reservation/list', roles: ['admin', 'store'] },
        hidden: true
      },
      {
        path: 'detail/:id',
        name: 'ReservationDetail',
        component: () => import('@/views/reservation/detail.vue'),
        meta: { title: '预约详情', activeMenu: '/reservation/list', roles: ['admin', 'store'] },
        hidden: true
      },
      {
        path: 'edit/:id',
        name: 'ReservationEdit',
        component: () => import('@/views/reservation/edit.vue'),
        meta: { title: '编辑预约', activeMenu: '/reservation/list', roles: ['admin', 'store'] },
        hidden: true
      },
      {
        path: 'refund',
        name: 'ReservationRefund',
        component: () => import('@/views/reservation/refund.vue'),
        meta: { title: '退款管理', icon: 'RefreshLeft', roles: ['admin', 'store'] }
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    redirect: '/user/list',
    meta: { title: '用户管理', icon: 'User', roles: ['admin'] },
    children: [
      {
        path: 'list',
        name: 'UserList',
        component: () => import('@/views/user/list.vue'),
        meta: { title: '用户列表', icon: 'List' }
      },
      {
        path: 'detail/:id',
        name: 'UserDetail',
        component: () => import('@/views/user/detail.vue'),
        meta: { title: '用户详情' },
        hidden: true
      },
      {
        path: 'add',
        name: 'UserAdd',
        component: () => import('@/views/user/edit.vue'),
        meta: { title: '新增用户' },
        hidden: true
      },
      {
        path: 'edit/:id',
        name: 'UserEdit',
        component: () => import('@/views/user/edit.vue'),
        meta: { title: '编辑用户' },
        hidden: true
      },
      {
        path: 'points/:id',
        name: 'UserPoints',
        component: () => import('@/views/user/points.vue'),
        meta: { title: '积分管理' },
        hidden: true
      },
      {
        path: 'address/:id',
        name: 'UserAddress',
        component: () => import('@/views/user/address.vue'),
        meta: { title: '地址管理' },
        hidden: true
      }
    ]
  },
  {
    path: '/vip',
    component: Layout,
    redirect: '/vip/packages',
    meta: { title: 'VIP管理', icon: 'Medal', roles: ['admin'] },
    children: [
      {
        path: 'packages',
        name: 'VipPackages',
        component: () => import('@/views/vip/packages.vue'),
        meta: { title: 'VIP套餐', icon: 'ShoppingBag', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/coupon',
    component: Layout,
    redirect: '/coupon/list',
    meta: { title: '优惠券管理', icon: 'Ticket', roles: ['admin'] },
    children: [
      {
        path: 'list',
        name: 'CouponList',
        component: () => import('@/views/coupon/list.vue'),
        meta: { title: '优惠券列表', icon: 'List', roles: ['admin'] }
      },
      {
        path: 'user-coupon',
        name: 'UserCoupon',
        component: () => import('@/views/coupon/user-coupon.vue'),
        meta: { title: '用户优惠券', icon: 'Tickets', roles: ['admin'] }
      }
    ]
  },
  {
    path: '/notification',
    component: Layout,
    redirect: '/notification/index',
    meta: { title: '通知中心', icon: 'Bell', roles: ['admin', 'store'] },
    children: [
      {
        path: 'index',
        name: 'Notification',
        component: () => import('@/views/notification/index.vue'),
        meta: { title: '通知中心', icon: 'Bell', roles: ['admin', 'store'] }
      }
    ]
  },
  {
    path: '/feedback',
    component: Layout,
    redirect: '/feedback/list',
    meta: { title: '留言管理', icon: 'ChatDotSquare', roles: ['admin'] },
    children: [
      {
        path: 'list',
        name: 'FeedbackList',
        component: () => import('@/views/feedback/list.vue'),
        meta: { title: '留言列表', icon: 'List', roles: ['admin'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const hasPermission = (route, role) => {
  if (!route.meta?.roles) return true
  return route.meta.roles.includes(role)
}

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin-token')

  if (to.path === '/login') {
    next()
    return
  }

  if (!token) {
    next('/login')
    return
  }

  const loginType = localStorage.getItem('admin-login-type') || 'admin'
  const currentRole = loginType === 'store' ? 'store' : 'admin'

  if (hasPermission(to, currentRole)) {
    next()
  } else {
    console.warn('无权限访问:', to.path)
    next('/dashboard')
  }
})

export default router
