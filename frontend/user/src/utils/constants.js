/**
 * 全局常量定义，避免魔法数字散落各处
 */

// ==================== 预约状态 ====================
export const RESERVATION_STATUS = {
  PENDING_PAYMENT: 0,   // 待支付
  PAID: 1,              // 已支付/待体验
  COMPLETED: 2,         // 已完成
  CANCELLED: 3,         // 已取消
  REFUNDING: 4,         // 退款中
  REFUNDED: 5           // 已退款
}

export const RESERVATION_STATUS_LABEL = {
  [RESERVATION_STATUS.PENDING_PAYMENT]: '待支付',
  [RESERVATION_STATUS.PAID]: '待体验',
  [RESERVATION_STATUS.COMPLETED]: '已完成',
  [RESERVATION_STATUS.CANCELLED]: '已取消',
  [RESERVATION_STATUS.REFUNDING]: '退款中',
  [RESERVATION_STATUS.REFUNDED]: '已退款'
}

export const RESERVATION_STATUS_TYPE = {
  [RESERVATION_STATUS.PENDING_PAYMENT]: 'warning',
  [RESERVATION_STATUS.PAID]: 'primary',
  [RESERVATION_STATUS.COMPLETED]: 'success',
  [RESERVATION_STATUS.CANCELLED]: 'info',
  [RESERVATION_STATUS.REFUNDING]: 'danger',
  [RESERVATION_STATUS.REFUNDED]: 'info'
}

// ==================== 剧本难度 ====================
export const SCRIPT_DIFFICULTY = {
  EASY: 1,
  NORMAL: 2,
  HARD: 3,
  HARDCORE: 4
}

export const SCRIPT_DIFFICULTY_LABEL = {
  [SCRIPT_DIFFICULTY.EASY]: '简单',
  [SCRIPT_DIFFICULTY.NORMAL]: '普通',
  [SCRIPT_DIFFICULTY.HARD]: '困难',
  [SCRIPT_DIFFICULTY.HARDCORE]: '硬核'
}

export const SCRIPT_DIFFICULTY_COLOR = {
  [SCRIPT_DIFFICULTY.EASY]: '#67c23a',
  [SCRIPT_DIFFICULTY.NORMAL]: '#e6a23c',
  [SCRIPT_DIFFICULTY.HARD]: '#f56c6c',
  [SCRIPT_DIFFICULTY.HARDCORE]: '#8B0000'
}

// ==================== 优惠券类型 ====================
export const COUPON_TYPE = {
  REDUCE: 1,    // 满减券
  DISCOUNT: 2,  // 折扣券
  CASH: 3       // 代金券
}

export const COUPON_TYPE_LABEL = {
  [COUPON_TYPE.REDUCE]: '满减券',
  [COUPON_TYPE.DISCOUNT]: '折扣券',
  [COUPON_TYPE.CASH]: '代金券'
}

export const COUPON_STATUS = {
  UNUSED: 1,  // 未使用
  USED: 2,    // 已使用
  EXPIRED: 3  // 已过期
}

// ==================== VIP 等级 ====================
export const VIP_LEVEL = {
  ROOKIE: 1,    // 见习侦探
  SILVER: 2,    // 银章侦探
  GOLD: 3,      // 金章侦探
  LEGEND: 4     // 传奇侦探
}

export const VIP_LEVEL_LABEL = {
  [VIP_LEVEL.ROOKIE]: '见习侦探',
  [VIP_LEVEL.SILVER]: '银章侦探',
  [VIP_LEVEL.GOLD]: '金章侦探',
  [VIP_LEVEL.LEGEND]: '传奇侦探'
}

export const VIP_LEVEL_BADGE = {
  [VIP_LEVEL.ROOKIE]: '🔰',
  [VIP_LEVEL.SILVER]: '🥈',
  [VIP_LEVEL.GOLD]: '🥇',
  [VIP_LEVEL.LEGEND]: '👑'
}

export const VIP_DISCOUNT_RATE = {
  [VIP_LEVEL.ROOKIE]: 0.95,  // 9.5折
  [VIP_LEVEL.SILVER]: 0.90,  // 9折
  [VIP_LEVEL.GOLD]: 0.85,    // 8.5折
  [VIP_LEVEL.LEGEND]: 0.80   // 8折
}

export const VIP_DISCOUNT_LABEL = {
  [VIP_LEVEL.ROOKIE]: '9.5折',
  [VIP_LEVEL.SILVER]: '9折',
  [VIP_LEVEL.GOLD]: '8.5折',
  [VIP_LEVEL.LEGEND]: '8折'
}

// ==================== 门店状态 ====================
export const STORE_STATUS = {
  CLOSED: 0,  // 停业
  OPEN: 1     // 营业中
}

export const STORE_STATUS_LABEL = {
  [STORE_STATUS.CLOSED]: '停业',
  [STORE_STATUS.OPEN]: '营业中'
}

// ==================== 拼单状态 ====================
export const GROUP_STATUS = {
  RECRUITING: 1,  // 招募中
  FULL: 2,        // 已成团
  CLOSED: 3       // 已关闭
}

export const GROUP_STATUS_LABEL = {
  [GROUP_STATUS.RECRUITING]: '招募中',
  [GROUP_STATUS.FULL]: '已成团',
  [GROUP_STATUS.CLOSED]: '已关闭'
}

// ==================== 支付方式 ====================
export const PAYMENT_METHOD = {
  ALIPAY: 'alipay',
  WECHAT: 'wechat',
  MOCK: 'mock'
}

export const PAYMENT_METHOD_LABEL = {
  [PAYMENT_METHOD.ALIPAY]: '支付宝',
  [PAYMENT_METHOD.WECHAT]: '微信支付',
  [PAYMENT_METHOD.MOCK]: '模拟支付'
}

// ==================== 通知类型 ====================
export const NOTIFICATION_TYPE = {
  SYSTEM: 1,       // 系统通知
  RESERVATION: 2,  // 预约通知
  COUPON: 3,       // 优惠券通知
  VIP: 4           // VIP通知
}

export const NOTIFICATION_TYPE_LABEL = {
  [NOTIFICATION_TYPE.SYSTEM]: '系统通知',
  [NOTIFICATION_TYPE.RESERVATION]: '预约通知',
  [NOTIFICATION_TYPE.COUPON]: '优惠券',
  [NOTIFICATION_TYPE.VIP]: 'VIP通知'
}

// ==================== 性别 ====================
export const GENDER = {
  UNKNOWN: 0,
  MALE: 1,
  FEMALE: 2
}

export const GENDER_LABEL = {
  [GENDER.UNKNOWN]: '未设置',
  [GENDER.MALE]: '男',
  [GENDER.FEMALE]: '女'
}

// ==================== localStorage Key ====================
export const STORAGE_KEY = {
  BROWSE_HISTORY: 'browseHistory',
  USER_BEHAVIOR: 'userBehavior',
  SEARCH_HISTORY: 'searchHistory',
  THEME: 'theme'
}

// ==================== 分页默认值 ====================
export const DEFAULT_PAGE_SIZE = 12
export const DEFAULT_PAGE = 1

// ==================== 浏览历史上限 ====================
export const MAX_BROWSE_HISTORY = 30
export const MAX_USER_BEHAVIOR = 100
export const MAX_SEARCH_HISTORY = 20
