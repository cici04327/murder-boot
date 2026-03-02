<template>
  <div class="empty-state-container" :class="[`type-${type}`, customClass]">
    <!-- 图标区域 -->
    <div class="empty-icon">
      <component :is="iconComponent" class="icon" />
    </div>
    
    <!-- 文本区域 -->
    <div class="empty-content">
      <h3 class="empty-title">{{ title }}</h3>
      <p class="empty-description">{{ description }}</p>
    </div>
    
    <!-- 操作区域 -->
    <div class="empty-actions" v-if="showAction">
      <slot name="action">
        <el-button 
          :type="actionType" 
          :size="actionSize"
          @click="handleAction"
        >
          <el-icon v-if="actionIcon"><component :is="actionIcon" /></el-icon>
          {{ actionText }}
        </el-button>
      </slot>
    </div>
    
    <!-- 额外内容插槽 -->
    <div class="empty-extra" v-if="$slots.extra">
      <slot name="extra"></slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import {
  Star,
  Calendar,
  Ticket,
  Search,
  ShoppingCart,
  Document,
  Bell,
  Location,
  User,
  Box,
  Files,
  Plus,
  ArrowRight
} from '@element-plus/icons-vue'

const props = defineProps({
  // 空状态类型
  type: {
    type: String,
    default: 'default',
    validator: (value) => [
      'default',
      'no-favorite',      // 无收藏
      'no-reservation',   // 无预约
      'no-coupon',        // 无优惠券
      'no-search-result', // 无搜索结果
      'no-data',          // 无数据
      'no-notification',  // 无通知
      'no-address',       // 无地址
      'network-error',    // 网络错误
      'permission-denied' // 权限不足
    ].includes(value)
  },
  // 标题
  title: {
    type: String,
    default: ''
  },
  // 描述
  description: {
    type: String,
    default: ''
  },
  // 自定义图标
  icon: {
    type: [String, Object],
    default: null
  },
  // 是否显示操作按钮
  showAction: {
    type: Boolean,
    default: true
  },
  // 操作按钮文字
  actionText: {
    type: String,
    default: ''
  },
  // 操作按钮类型
  actionType: {
    type: String,
    default: 'primary'
  },
  // 操作按钮大小
  actionSize: {
    type: String,
    default: 'default'
  },
  // 操作按钮图标
  actionIcon: {
    type: Object,
    default: null
  },
  // 自定义类名
  customClass: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['action'])

// 预定义的配置
const typeConfig = {
  'no-favorite': {
    icon: Star,
    title: '还没有收藏的剧本',
    description: '快去发现喜欢的剧本吧',
    actionText: '浏览剧本',
    actionIcon: ArrowRight
  },
  'no-reservation': {
    icon: Calendar,
    title: '还没有预约记录',
    description: '赶快预约一场精彩的剧本杀体验吧',
    actionText: '立即预约',
    actionIcon: Plus
  },
  'no-coupon': {
    icon: Ticket,
    title: '暂无优惠券',
    description: '去领取优惠券，享受更多优惠',
    actionText: '领取优惠券',
    actionIcon: Plus
  },
  'no-search-result': {
    icon: Search,
    title: '没有找到相关结果',
    description: '换个关键词试试吧',
    actionText: '清空搜索',
    actionIcon: null
  },
  'no-data': {
    icon: Box,
    title: '暂无数据',
    description: '当前没有可显示的内容',
    actionText: '刷新',
    actionIcon: null
  },
  'no-notification': {
    icon: Bell,
    title: '暂无消息通知',
    description: '有新消息时会在这里显示',
    actionText: null,
    actionIcon: null
  },
  'no-address': {
    icon: Location,
    title: '还没有收货地址',
    description: '添加地址后可以更便捷地预约',
    actionText: '添加地址',
    actionIcon: Plus
  },
  'network-error': {
    icon: Document,
    title: '网络连接失败',
    description: '请检查网络设置后重试',
    actionText: '重新加载',
    actionIcon: null
  },
  'permission-denied': {
    icon: User,
    title: '没有访问权限',
    description: '请先登录后再访问',
    actionText: '去登录',
    actionIcon: ArrowRight
  },
  'default': {
    icon: Files,
    title: '暂无内容',
    description: '这里还没有内容哦',
    actionText: null,
    actionIcon: null
  }
}

// 计算图标组件
const iconComponent = computed(() => {
  if (props.icon) {
    return props.icon
  }
  return typeConfig[props.type]?.icon || Files
})

// 计算标题
const computedTitle = computed(() => {
  return props.title || typeConfig[props.type]?.title || '暂无内容'
})

// 计算描述
const computedDescription = computed(() => {
  return props.description || typeConfig[props.type]?.description || ''
})

// 计算操作按钮文字
const computedActionText = computed(() => {
  return props.actionText || typeConfig[props.type]?.actionText || '确定'
})

// 计算操作按钮图标
const computedActionIcon = computed(() => {
  return props.actionIcon || typeConfig[props.type]?.actionIcon
})

// 使用计算属性替换直接引用
const title = computedTitle
const description = computedDescription
const actionText = computedActionText
const actionIcon = computedActionIcon

// 处理操作按钮点击
const handleAction = () => {
  emit('action')
}
</script>

<style scoped>
.empty-state-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  min-height: 400px;
}

.empty-icon {
  margin-bottom: 24px;
}

.empty-icon .icon {
  font-size: 120px;
  color: #dcdfe6;
  opacity: 0.6;
}

.empty-content {
  margin-bottom: 32px;
  max-width: 500px;
}

.empty-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 12px 0;
}

.empty-description {
  font-size: 14px;
  color: #909399;
  margin: 0;
  line-height: 1.6;
}

.empty-actions {
  margin-bottom: 20px;
}

.empty-extra {
  margin-top: 20px;
}

/* 不同类型的样式 */
.type-no-favorite .icon {
  color: #f59e0b;
}

.type-no-reservation .icon {
  color: #3b82f6;
}

.type-no-coupon .icon {
  color: #ef4444;
}

.type-no-search-result .icon {
  color: #6b7280;
}

.type-network-error .icon {
  color: #f56c6c;
}

.type-permission-denied .icon {
  color: #f59e0b;
}

/* 响应式 */
@media (max-width: 768px) {
  .empty-state-container {
    padding: 40px 20px;
    min-height: 300px;
  }
  
  .empty-icon .icon {
    font-size: 80px;
  }
  
  .empty-title {
    font-size: 18px;
  }
  
  .empty-description {
    font-size: 13px;
  }
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .empty-title {
    color: #e0e0e0;
  }
  
  .empty-description {
    color: #a0a0a0;
  }
  
  .empty-icon .icon {
    color: #606266;
  }
}

/* 动画效果 */
.empty-state-container {
  animation: fadeIn 0.4s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

