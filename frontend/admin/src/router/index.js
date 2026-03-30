import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import { hasRoutePermission } from '@/utils/permission'

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
    redirect: () => {
      const loginType = localStorage.getItem('admin-login-type') || 'admin'
      if (loginType === 'staff') {
        try {
          const adminUser = JSON.parse(localStorage.getItem('admin-user') || '{}')
          return adminUser.staffRole === 'DM' ? '/staff/my-schedule' : '/reservation/list'
        } catch (e) {
          return '/reservation/list'
        }
      }
      return '/dashboard'
    },
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
    redirect: (to) => {
      const loginType = localStorage.getItem('admin-login-type')
      return loginType === 'store' ? '/store/operation-board' : '/store/list'
    },
    meta: { title: '门店管理', icon: 'Shop', roles: ['admin', 'store', 'staff'], permissionCodes: ['employee:manage', 'report:view'] },
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
        meta: { title: '经营看板', icon: 'TrendCharts', roles: ['admin', 'store', 'staff'], permissionCodes: ['report:view'] }
      },
      {
        path: 'room',
        name: 'StoreRoom',
        component: () => import('@/views/store/room.vue'),
        meta: { title: '房间管理', icon: 'Operation', roles: ['admin'] }
      },
      {
        path: 'dm',
        name: 'StoreDm',
        component: () => import('@/views/store/dm.vue'),
        meta: { title: 'DM管理', icon: 'Avatar', roles: ['admin', 'store'] }
      },
      {
        path: 'employee',
        name: 'StoreEmployee',
        component: () => import('@/views/store/employee.vue'),
        meta: { title: '员工管理', icon: 'UserFilled', roles: ['admin', 'store', 'staff'], permissionCodes: ['employee:manage'] }
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
      },
      {
        path: 'daily-report',
        name: 'StoreDailyReport',
        component: () => import('@/views/store/daily-report.vue'),
        meta: { title: '营收日报', icon: 'TrendCharts', roles: ['admin', 'store', 'staff'], permissionCodes: ['report:view'] }
      },
    ]
  },
  {
    path: '/script',
    component: Layout,
    redirect: (to) => {
      const loginType = localStorage.getItem('admin-login-type')
      return loginType === 'store' ? '/script/schedule' : '/script/list'
    },
    meta: { title: '剧本管理', icon: 'Reading', roles: ['admin', 'store'] },
    children: [
      {
        path: 'list',
        name: 'ScriptList',
        component: () => import('@/views/script/list.vue'),
        meta: { title: '剧本列表', icon: 'List', roles: ['admin'] }
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
        meta: { title: '剧本评价', icon: 'Comment', roles: ['admin'] }
      },
      {
        path: 'schedule',
        name: 'ScriptSchedule',
        component: () => import('@/views/script/schedule.vue'),
        meta: { title: '排期管理', icon: 'Calendar', roles: ['admin', 'store'] }
      },
      {
        path: 'ai-schedule',
        name: 'AiDmSchedule',
        component: () => import('@/views/script/ai-schedule.vue'),
        meta: { title: 'AI智能分配DM', icon: 'MagicStick', roles: ['admin', 'store'] }
      },
    ]
  },
  {
    path: '/reservation',
    component: Layout,
    redirect: '/reservation/list',
    meta: { title: '预约管理', icon: 'Calendar', roles: ['admin', 'store', 'staff'] },
    children: [
      {
        path: 'list',
        name: 'ReservationList',
        component: () => import('@/views/reservation/list.vue'),
        meta: { title: '预约列表', icon: 'List', roles: ['admin', 'store', 'staff'], permissionCodes: ['reservation:view'] }
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
        meta: { title: '预约详情', activeMenu: '/reservation/list', roles: ['admin', 'store', 'staff'], permissionCodes: ['reservation:view'] },
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
        meta: { title: '退款管理', icon: 'RefreshLeft', roles: ['admin', 'store', 'staff'], permissionCodes: ['refund:process'] }
      }
    ]
  },
  {
    path: '/staff',
    component: Layout,
    redirect: '/staff/my-schedule',
    meta: { title: '我的工作', icon: 'Calendar', roles: ['staff'], staffRoles: ['DM'] },
    children: [
      {
        path: 'my-schedule',
        name: 'StaffMySchedule',
        component: () => import('@/views/staff/my-schedule.vue'),
        meta: { title: '我的场次', icon: 'Calendar', roles: ['staff'], staffRoles: ['DM'] }
      },
      {
        path: 'my-reservation',
        name: 'StaffMyReservation',
        component: () => import('@/views/staff/my-reservation.vue'),
        meta: { title: '我的预约', icon: 'Tickets', roles: ['staff'], staffRoles: ['DM'] }
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
    meta: { title: '优惠券管理', icon: 'Ticket', roles: ['admin', 'store'] },
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
        meta: { title: '用户优惠券', icon: 'Tickets', roles: ['admin', 'store'] }
      }
    ]
  },
  {
    path: '/notification',
    component: Layout,
    redirect: '/notification/index',
    meta: { title: '通知中心', icon: 'Bell', roles: ['admin', 'store', 'staff'], permissionCodes: ['notification:view'] },
    children: [
      {
        path: 'index',
        name: 'Notification',
        component: () => import('@/views/notification/index.vue'),
        meta: { title: '通知中心', icon: 'Bell', roles: ['admin', 'store', 'staff'], permissionCodes: ['notification:view'] }
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
  },
  {
    path: '/service',
    component: Layout,
    redirect: '/service/index',
    meta: { title: '客服中心', icon: 'Service', roles: ['admin'] },
    children: [
      {
        path: 'index',
        name: 'ServiceCenter',
        component: () => import('@/views/service/index.vue'),
        meta: { title: '客服中心', icon: 'Service', roles: ['admin'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const hasPermission = (route) => hasRoutePermission(route)

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
  const currentRole = loginType === 'store' ? 'store' : (loginType === 'staff' ? 'staff' : 'admin')

  if (hasPermission(to)) {
    next()
  } else {
    console.warn('无权限访问:', to.path)
    if (currentRole === 'staff') {
      try {
        const adminUser = JSON.parse(localStorage.getItem('admin-user') || '{}')
        next(adminUser.staffRole === 'DM' ? '/staff/my-schedule' : '/reservation/list')
      } catch (e) {
        next('/reservation/list')
      }
      return
    }
    next('/dashboard')
  }
})

export default router
