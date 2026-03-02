<template>
  <div class="skeleton" :class="[type, { animated }]" :style="customStyle">
    <div class="skeleton-shimmer"></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'rect', // rect, circle, text
    validator: (value) => ['rect', 'circle', 'text'].includes(value)
  },
  width: {
    type: [String, Number],
    default: '100%'
  },
  height: {
    type: [String, Number],
    default: '16px'
  },
  animated: {
    type: Boolean,
    default: true
  },
  borderRadius: {
    type: [String, Number],
    default: '4px'
  }
})

const customStyle = computed(() => {
  const width = typeof props.width === 'number' ? `${props.width}px` : props.width
  const height = typeof props.height === 'number' ? `${props.height}px` : props.height
  const borderRadius = typeof props.borderRadius === 'number' ? `${props.borderRadius}px` : props.borderRadius
  
  return {
    width,
    height,
    borderRadius: props.type === 'circle' ? '50%' : borderRadius
  }
})
</script>

<style scoped>
.skeleton {
  position: relative;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 37%, #f0f0f0 63%);
  overflow: hidden;
}

.skeleton.animated {
  background-size: 400% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}

.skeleton.text {
  border-radius: 4px;
}

.skeleton.circle {
  border-radius: 50%;
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .skeleton {
    background: linear-gradient(90deg, #2a2a2a 25%, #3a3a3a 37%, #2a2a2a 63%);
  }
}
</style>

