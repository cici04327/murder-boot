<template>
  <transition name="fade">
    <div
      v-show="visible"
      class="back-to-top"
      @click="scrollToTop"
    >
      <el-tooltip content="返回顶部" placement="left">
        <el-button circle type="primary" size="large">
          <el-icon :size="24"><Top /></el-icon>
        </el-button>
      </el-tooltip>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { Top } from '@element-plus/icons-vue'

const visible = ref(false)

// 滚动监听
const handleScroll = () => {
  visible.value = window.scrollY > 300
}

// 滚动到顶部
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.back-to-top {
  position: fixed;
  right: 50px;
  bottom: 50px;
  z-index: 999;
  cursor: pointer;
}

.back-to-top .el-button {
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
  transition: all 0.3s;
}

.back-to-top .el-button:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 16px rgba(64, 158, 255, 0.5);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
