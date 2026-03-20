<template>
  <div class="page-anchor" v-if="anchors.length > 0">
    <div class="anchor-title">快速导航</div>
    <div class="anchor-list">
      <div
        v-for="(anchor, index) in anchors"
        :key="index"
        class="anchor-item"
        :class="{ active: activeAnchor === index }"
        @click="scrollToSection(index)"
      >
        <div class="anchor-dot"></div>
        <span>{{ anchor }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  anchors: {
    type: Array,
    default: () => []
  }
})

const activeAnchor = ref(0)

const scrollToSection = (index) => {
  activeAnchor.value = index
  const sections = document.querySelectorAll('.agreement-section')
  if (sections[index]) {
    // 获取目标元素的位置
    const targetElement = sections[index]
    const targetPosition = targetElement.getBoundingClientRect().top + window.pageYOffset
    
    // 计算偏移量（考虑固定头部高度 + 额外边距）
    const headerHeight = 80  // 导航栏高度
    const extraOffset = 100  // 额外向下偏移，让内容显示在更舒适的位置
    const offsetPosition = targetPosition - headerHeight - extraOffset
    
    // 平滑滚动到计算后的位置
    window.scrollTo({
      top: offsetPosition,
      behavior: 'smooth'
    })
  }
}
</script>

<style scoped>
.page-anchor {
  position: fixed;
  right: 30px;
  top: 50%;
  transform: translateY(-50%);
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.96) 0%, rgba(22, 33, 62, 0.94) 100%);
  padding: 20px 15px;
  border-radius: 12px;
  border: 1px solid rgba(192, 57, 43, 0.2);
  box-shadow: 0 14px 30px rgba(0, 0, 0, 0.24);
  max-width: 200px;
  z-index: 100;
}

.anchor-title {
  font-size: 14px;
  font-weight: bold;
  color: rgba(255, 255, 255, 0.88);
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid rgba(224, 90, 71, 0.8);
}

.anchor-list {
  max-height: 400px;
  overflow-y: auto;
}

.anchor-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  margin-bottom: 5px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.3s;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.anchor-item:hover {
  background: rgba(224, 90, 71, 0.14);
  color: #ffffff;
}

.anchor-item.active {
  background: linear-gradient(135deg, #16213e 0%, #0f3460 100%);
  color: white;
}

.anchor-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.anchor-item span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 滚动条样式 */
.anchor-list::-webkit-scrollbar {
  width: 4px;
}

.anchor-list::-webkit-scrollbar-thumb {
  background: rgba(224, 90, 71, 0.26);
  border-radius: 2px;
}

.anchor-list::-webkit-scrollbar-thumb:hover {
  background: rgba(224, 90, 71, 0.38);
}
</style>
