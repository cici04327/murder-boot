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
  background: white;
  padding: 20px 15px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  max-width: 200px;
  z-index: 100;
}

.anchor-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409EFF;
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
  color: #606266;
}

.anchor-item:hover {
  background: #ecf5ff;
  color: #409EFF;
}

.anchor-item.active {
  background: #409EFF;
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
  background: #dcdfe6;
  border-radius: 2px;
}

.anchor-list::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}
</style>
