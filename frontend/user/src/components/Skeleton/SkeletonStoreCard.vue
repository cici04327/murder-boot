<template>
  <div class="skeleton-store-card">
    <el-row :gutter="20">
      <!-- 左侧图片 -->
      <el-col :xs="24" :sm="8">
        <SkeletonBase 
          type="rect" 
          height="180px" 
          :border-radius="8"
        />
      </el-col>
      
      <!-- 右侧信息 -->
      <el-col :xs="24" :sm="16">
        <div class="skeleton-store-info">
          <!-- 标题 -->
          <SkeletonBase 
            type="text" 
            height="28px" 
            width="60%" 
            style="margin-bottom: 15px;"
          />
          
          <!-- 标签 -->
          <div class="skeleton-tags" style="margin-bottom: 15px;">
            <SkeletonBase 
              v-for="i in 3" 
              :key="i"
              type="rect" 
              width="70px" 
              height="24px" 
              :border-radius="12"
              style="margin-right: 8px;"
            />
          </div>
          
          <!-- 地址 -->
          <SkeletonBase 
            type="text" 
            height="20px" 
            width="80%" 
            style="margin-bottom: 10px;"
          />
          
          <!-- 评分和其他信息 -->
          <div style="display: flex; gap: 20px;">
            <SkeletonBase type="rect" width="100px" height="20px" />
            <SkeletonBase type="rect" width="80px" height="20px" />
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import SkeletonBase from './SkeletonBase.vue'
</script>

<style scoped>
.skeleton-store-card {
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.skeleton-store-info {
  padding: 10px 0;
}

.skeleton-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 576px) {
  .skeleton-store-card {
    padding: 15px;
  }
  
  .skeleton-store-info {
    padding-top: 15px;
  }
}
</style>

