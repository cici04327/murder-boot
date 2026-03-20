<template>
  <div class="skeleton-script-card">
    <!-- 封面图片骨架 -->
    <SkeletonBase 
      type="rect" 
      :height="imageHeight" 
      :border-radius="8"
    />
    
    <!-- 内容骨架 -->
    <div class="skeleton-content">
      <!-- 标题 -->
      <SkeletonBase 
        type="text" 
        height="24px" 
        width="80%" 
        :border-radius="4"
        style="margin-bottom: 12px;"
      />
      
      <!-- 标签组 -->
      <div class="skeleton-tags">
        <SkeletonBase 
          v-for="i in 3" 
          :key="i"
          type="rect" 
          width="60px" 
          height="24px" 
          :border-radius="12"
          style="margin-right: 8px;"
        />
      </div>
      
      <!-- 评分 -->
      <div class="skeleton-rating" style="margin: 12px 0;">
        <SkeletonBase 
          type="rect" 
          width="120px" 
          height="20px" 
          :border-radius="4"
        />
      </div>
      
      <!-- 价格 -->
      <SkeletonBase 
        type="text" 
        width="100px" 
        height="28px" 
        :border-radius="4"
      />
    </div>
  </div>
</template>

<script setup>
import SkeletonBase from './SkeletonBase.vue'

defineProps({
  imageHeight: {
    type: [String, Number],
    default: 200
  }
})
</script>

<style scoped>
.skeleton-script-card {
  border: 1px solid rgba(192, 57, 43, 0.2);
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.96) 0%, rgba(22, 33, 62, 0.94) 100%);
  transition: all 0.3s ease;
}

.skeleton-content {
  padding: 16px;
}

.skeleton-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skeleton-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 深色模式 */
@media (prefers-color-scheme: dark) {
  .skeleton-script-card {
    background: linear-gradient(135deg, rgba(26, 26, 46, 0.96) 0%, rgba(22, 33, 62, 0.94) 100%);
    border-color: rgba(192, 57, 43, 0.2);
  }
}
</style>

