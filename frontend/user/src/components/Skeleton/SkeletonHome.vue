<template>
  <div class="skeleton-home">
    <!-- 轮播图骨架 -->
    <div class="skeleton-banner">
      <SkeletonBase type="rect" height="450px" :border-radius="0" />
    </div>
    
    <!-- 统计数据骨架 -->
    <div class="skeleton-statistics">
      <el-row :gutter="20">
        <el-col v-for="i in 4" :key="i" :xs="12" :sm="6">
          <div class="skeleton-stat-card">
            <SkeletonBase type="circle" :width="50" :height="50" style="margin-bottom: 10px;" />
            <SkeletonBase type="text" height="32px" width="80px" style="margin-bottom: 8px;" />
            <SkeletonBase type="text" height="16px" width="100px" />
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 快速入口骨架 -->
    <div class="skeleton-quick-entry">
      <el-row :gutter="20">
        <el-col v-for="i in 4" :key="i" :xs="12" :sm="6">
          <div class="skeleton-entry-card">
            <SkeletonBase type="circle" :width="60" :height="60" style="margin-bottom: 15px;" />
            <SkeletonBase type="text" height="20px" width="80px" style="margin-bottom: 8px;" />
            <SkeletonBase type="text" height="16px" width="100px" />
          </div>
        </el-col>
      </el-row>
    </div>
    
    <!-- 热门剧本骨架 -->
    <div class="skeleton-section">
      <div class="skeleton-section-header">
        <SkeletonBase type="text" height="32px" width="150px" />
      </div>
      <div class="skeleton-horizontal-scroll">
        <div 
          v-for="i in 4" 
          :key="i" 
          class="skeleton-horizontal-card"
        >
          <SkeletonBase type="rect" height="200px" :border-radius="8" style="margin-bottom: 12px;" />
          <SkeletonBase type="text" height="20px" width="90%" style="margin-bottom: 8px;" />
          <SkeletonBase type="text" height="16px" width="70%" />
        </div>
      </div>
    </div>
    
    <!-- 推荐剧本骨架 -->
    <div class="skeleton-section">
      <div class="skeleton-section-header">
        <SkeletonBase type="text" height="32px" width="150px" />
      </div>
      <el-row :gutter="20">
        <el-col v-for="i in 4" :key="i" :xs="24" :sm="12" :md="6">
          <SkeletonScriptCard :image-height="220" />
        </el-col>
      </el-row>
    </div>
    
    <!-- 推荐门店骨架 -->
    <div class="skeleton-section">
      <div class="skeleton-section-header">
        <SkeletonBase type="text" height="32px" width="150px" />
      </div>
      <el-row :gutter="20">
        <el-col v-for="i in 3" :key="i" :xs="24" :sm="12" :md="8">
          <SkeletonStoreCard />
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import SkeletonBase from './SkeletonBase.vue'
import SkeletonScriptCard from './SkeletonScriptCard.vue'
import SkeletonStoreCard from './SkeletonStoreCard.vue'
</script>

<style scoped>
.skeleton-home {
  min-height: 100vh;
  background: #f5f5f5;
}

.skeleton-banner {
  width: 100%;
  margin-bottom: 20px;
}

.skeleton-statistics {
  max-width: 1200px;
  margin: -60px auto 40px;
  padding: 0 20px;
  position: relative;
  z-index: 2;
}

.skeleton-stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.skeleton-quick-entry {
  max-width: 1200px;
  margin: 0 auto 40px;
  padding: 0 20px;
}

.skeleton-entry-card {
  background: #fff;
  border-radius: 12px;
  padding: 30px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
}

.skeleton-section {
  max-width: 1200px;
  margin: 0 auto 60px;
  padding: 0 20px;
}

.skeleton-section-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.skeleton-horizontal-scroll {
  display: flex;
  gap: 20px;
  overflow-x: auto;
  padding-bottom: 10px;
}

.skeleton-horizontal-card {
  flex-shrink: 0;
  width: 280px;
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}

/* 隐藏滚动条但保持可滚动 */
.skeleton-horizontal-scroll::-webkit-scrollbar {
  height: 6px;
}

.skeleton-horizontal-scroll::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

@media (max-width: 768px) {
  .skeleton-statistics {
    margin-top: 20px;
  }
  
  .skeleton-stat-card {
    padding: 16px;
  }
  
  .skeleton-entry-card {
    padding: 20px 15px;
  }
  
  .skeleton-section {
    margin-bottom: 40px;
  }
}
</style>

