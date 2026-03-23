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
    <button
      class="paper-mode-toggle"
      type="button"
      :aria-pressed="paperMode"
      :title="paperMode ? '恢复项目默认深色主题' : '切换为论文截图浅色主题'"
      @click="togglePaperMode"
    >
      <span class="toggle-icon">{{ paperMode ? '🌙' : '📄' }}</span>
      <span class="toggle-text">{{ paperMode ? '恢复深色' : '论文浅色' }}</span>
    </button>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const transitionName = ref('fade-slide')
const paperMode = ref(false)
const PAPER_MODE_KEY = 'paper-screenshot-mode'

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

const applyPaperMode = (enabled) => {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.classList.toggle('paper-mode', enabled)
  document.body.classList.toggle('paper-mode', enabled)
  if (enabled) {
    document.documentElement.setAttribute('data-theme', 'paper')
  } else {
    document.documentElement.removeAttribute('data-theme')
  }
  paperMode.value = enabled
}

const togglePaperMode = () => {
  const nextValue = !paperMode.value
  applyPaperMode(nextValue)
  localStorage.setItem(PAPER_MODE_KEY, nextValue ? '1' : '0')
}

onMounted(() => {
  // 从本地存储恢复用户信息
  userStore.loadUserFromStorage()
  applyPaperMode(localStorage.getItem(PAPER_MODE_KEY) === '1')
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

.paper-mode-toggle {
  position: fixed;
  right: 20px;
  bottom: 24px;
  z-index: 4000;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 11px 16px;
  border: 1px solid rgba(192, 57, 43, 0.22);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.94) 0%, rgba(22, 33, 62, 0.92) 100%);
  color: rgba(255, 255, 255, 0.92);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(14px);
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;
}

.paper-mode-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.28);
}

.paper-mode .paper-mode-toggle {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 247, 251, 0.94) 100%);
  color: #253043;
  border-color: rgba(192, 57, 43, 0.18);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.12);
}

.toggle-icon {
  font-size: 15px;
}

.toggle-text {
  line-height: 1;
}

@media (max-width: 768px) {
  .paper-mode-toggle {
    right: 14px;
    bottom: 18px;
    padding: 10px 14px;
    font-size: 12px;
  }
}

@media print {
  .paper-mode-toggle {
    display: none !important;
  }
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
