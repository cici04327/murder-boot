<template>
  <div id="app">
    <router-view v-slot="{ Component, route }">
      <transition
        :name="transitionName"
        mode="out-in"
        @before-enter="onBeforeEnter"
        @after-enter="onAfterEnter"
      >
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const transitionName = ref('fade-slide')

// 根据路由层级决定动画方向
watch(
  () => router.currentRoute.value,
  (to, from) => {
    if (from && to) {
      const toDepth = to.path.split('/').length
      const fromDepth = from.path.split('/').length
      
      if (toDepth < fromDepth) {
        transitionName.value = 'slide-right' // 返回
      } else if (toDepth > fromDepth) {
        transitionName.value = 'slide-left' // 前进
      } else {
        transitionName.value = 'fade' // 同级
      }
    }
  }
)

const onBeforeEnter = () => {
  // 动画开始前的处理（可选）
}

const onAfterEnter = () => {
  // 动画完成后的处理（可选）
}

onMounted(() => {
  // 从本地存储恢复用户信息
  userStore.loadUserFromStorage()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'Inter', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  min-height: 100vh;
  position: relative;
  
  /* 简洁灰白背景 - 与0.html一致 (bg-gray-50) */
  background: #f9fafb;
}

body {
  margin: 0;
  padding: 0;
  background: #f9fafb;
  color: #1f2937; /* text-gray-800 */
}

/* ========== 路由过渡动画 ========== */

/* 淡入淡出 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 淡入 + 滑动 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 向左滑动（前进） */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
  position: absolute;
  width: 100%;
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-30%);
}

/* 向右滑动（返回） */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
  position: absolute;
  width: 100%;
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-100%);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(30%);
}

/* 缩放动画 */
.zoom-enter-active,
.zoom-leave-active {
  transition: all 0.3s ease;
}

.zoom-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.zoom-leave-to {
  opacity: 0;
  transform: scale(1.05);
}
</style>
