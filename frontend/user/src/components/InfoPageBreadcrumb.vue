<template>
  <div class="breadcrumb-container">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/' }">
        <el-icon><HomeFilled /></el-icon>
        首页
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ currentPage }}</el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { HomeFilled } from '@element-plus/icons-vue'

const route = useRoute()

const currentPage = computed(() => {
  return route.meta.title || '信息页面'
})
</script>

<style scoped>
.breadcrumb-container {
  padding: 15px 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.95) 0%, rgba(22, 33, 62, 0.95) 100%);
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid rgba(139, 0, 0, 0.2);
}

.el-breadcrumb {
  font-size: 14px;
}

.el-breadcrumb-item {
  display: flex;
  align-items: center;
  gap: 5px;
}

:deep(.el-breadcrumb__inner) {
  color: rgba(255, 255, 255, 0.7) !important;
}

:deep(.el-breadcrumb__inner a),
:deep(.el-breadcrumb__inner.is-link) {
  color: rgba(255, 255, 255, 0.7) !important;
}

:deep(.el-breadcrumb__inner a:hover),
:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #ff6b6b !important;
}

:deep(.el-breadcrumb__separator) {
  color: rgba(255, 255, 255, 0.4) !important;
}

:deep(.el-icon) {
  color: rgba(255, 255, 255, 0.7) !important;
}
</style>
