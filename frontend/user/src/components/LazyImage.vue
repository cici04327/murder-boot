<template>
  <div ref="containerRef" class="lazy-image-container" :style="containerStyle">
    <transition name="fade">
      <img
        v-if="loaded"
        :src="currentSrc"
        :alt="alt"
        :class="['lazy-image', imageClass, { 'loaded': imageLoaded }]"
        @load="onImageLoad"
        @error="onImageError"
      />
    </transition>
    
    <!-- 加载中占位符 -->
    <div v-if="!loaded || loading" class="lazy-image-placeholder" :style="placeholderStyle">
      <div v-if="showLoadingAnimation" class="loading-animation">
        <div class="loading-spinner"></div>
      </div>
      <el-icon v-else class="placeholder-icon"><Picture /></el-icon>
    </div>
    
    <!-- 加载失败占位符 -->
    <div v-if="error" class="lazy-image-error">
      <el-icon class="error-icon"><PictureFilled /></el-icon>
      <span class="error-text">图片加载失败</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { Picture, PictureFilled } from '@element-plus/icons-vue'

const props = defineProps({
  // 图片源地址
  src: {
    type: String,
    required: true
  },
  // 占位图地址
  placeholder: {
    type: String,
    default: ''
  },
  // 图片alt属性
  alt: {
    type: String,
    default: ''
  },
  // 图片宽度
  width: {
    type: [String, Number],
    default: '100%'
  },
  // 图片高度
  height: {
    type: [String, Number],
    default: 'auto'
  },
  // 自定义类名
  imageClass: {
    type: String,
    default: ''
  },
  // 占位符背景色
  placeholderBg: {
    type: String,
    default: '#f5f5f5'
  },
  // 是否显示加载动画
  showLoadingAnimation: {
    type: Boolean,
    default: true
  },
  // 预加载阈值（像素）
  threshold: {
    type: Number,
    default: 100
  },
  // 是否立即加载（不使用懒加载）
  immediate: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['load', 'error'])

// 状态
const loaded = ref(false)
const loading = ref(false)
const imageLoaded = ref(false)
const error = ref(false)
const currentSrc = ref(props.placeholder || '')
const containerRef = ref(null)
const observer = ref(null)

// 计算样式
const containerStyle = computed(() => {
  const width = typeof props.width === 'number' ? `${props.width}px` : props.width
  const height = typeof props.height === 'number' ? `${props.height}px` : props.height
  return {
    width,
    height,
    minHeight: height === 'auto' ? '200px' : height
  }
})

const placeholderStyle = computed(() => ({
  backgroundColor: props.placeholderBg
}))

// 图片加载完成
const onImageLoad = () => {
  loading.value = false
  imageLoaded.value = true
  error.value = false
  emit('load')
}

// 图片加载失败
const onImageError = () => {
  loading.value = false
  imageLoaded.value = false
  error.value = true
  
  // 如果有占位图，显示占位图
  if (props.placeholder && currentSrc.value !== props.placeholder) {
    currentSrc.value = props.placeholder
    error.value = false
  } else {
    emit('error')
  }
}

// 加载图片
const loadImage = () => {
  if (loaded.value || loading.value || !props.src) return
  
  loading.value = true
  currentSrc.value = props.src
  loaded.value = true
}

// 创建 Intersection Observer
const createObserver = (element) => {
  const options = {
    root: null,
    rootMargin: `${props.threshold}px`,
    threshold: 0.01
  }
  
  observer.value = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        loadImage()
        // 加载后停止观察
        if (observer.value) {
          observer.value.unobserve(element)
        }
      }
    })
  }, options)
  
  observer.value.observe(element)
}

// 监听src变化
watch(() => props.src, (newSrc) => {
  if (newSrc && props.immediate) {
    loadImage()
  }
})

onMounted(() => {
  // 如果设置了立即加载或者不支持 Intersection Observer
  if (props.immediate || !('IntersectionObserver' in window)) {
    loadImage()
  } else if (containerRef.value) {
    // 使用懒加载
    createObserver(containerRef.value)
  } else {
    // 如果没有容器元素，则立即加载
    loadImage()
  }
})

onBeforeUnmount(() => {
  // 清理 observer
  if (observer.value) {
    observer.value.disconnect()
    observer.value = null
  }
})

// 暴露方法
defineExpose({
  reload: loadImage
})
</script>

<style scoped>
.lazy-image-container {
  position: relative;
  overflow: hidden;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.lazy-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.lazy-image.loaded {
  opacity: 1;
}

.lazy-image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.placeholder-icon {
  font-size: 48px;
  color: #c0c4cc;
}

.loading-animation {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.lazy-image-error {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
}

.error-icon {
  font-size: 48px;
  color: #f56c6c;
  margin-bottom: 8px;
}

.error-text {
  font-size: 14px;
  color: #909399;
}

/* 淡入动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .lazy-image-container {
    background-color: #2a2a2a;
  }
  
  .lazy-image-placeholder,
  .lazy-image-error {
    background-color: #2a2a2a;
  }
  
  .placeholder-icon {
    color: #606266;
  }
  
  .error-text {
    color: #909399;
  }
}
</style>

