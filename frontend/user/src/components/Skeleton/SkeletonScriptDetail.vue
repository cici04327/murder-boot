<template>
  <div class="skeleton-script-detail">
    <!-- 顶部封面图 -->
    <div class="skeleton-cover">
      <SkeletonBase type="rect" height="400px" :border-radius="0" />
    </div>
    
    <div class="skeleton-detail-content">
      <el-row :gutter="40">
        <!-- 左侧信息 -->
        <el-col :xs="24" :md="16">
          <!-- 标题 -->
          <SkeletonBase 
            type="text" 
            height="36px" 
            width="60%" 
            style="margin-bottom: 20px;"
          />
          
          <!-- 标签和评分 -->
          <div class="skeleton-meta" style="margin-bottom: 30px;">
            <div style="display: flex; gap: 10px; margin-bottom: 15px;">
              <SkeletonBase 
                v-for="i in 4" 
                :key="i"
                type="rect" 
                width="80px" 
                height="28px" 
                :border-radius="14"
              />
            </div>
            <SkeletonBase type="rect" width="150px" height="24px" />
          </div>
          
          <!-- 剧本介绍 -->
          <div class="skeleton-description" style="margin-bottom: 30px;">
            <SkeletonBase 
              type="text" 
              height="20px" 
              width="100%" 
              style="margin-bottom: 10px;"
            />
            <SkeletonBase 
              type="text" 
              height="20px" 
              width="95%" 
              style="margin-bottom: 10px;"
            />
            <SkeletonBase 
              type="text" 
              height="20px" 
              width="90%" 
              style="margin-bottom: 10px;"
            />
            <SkeletonBase 
              type="text" 
              height="20px" 
              width="85%" 
            />
          </div>
          
          <!-- 角色信息 -->
          <div class="skeleton-roles">
            <SkeletonBase 
              type="text" 
              height="28px" 
              width="120px" 
              style="margin-bottom: 20px;"
            />
            <el-row :gutter="15">
              <el-col 
                v-for="i in 6" 
                :key="i"
                :xs="12" 
                :sm="8" 
                :md="6"
              >
                <div class="skeleton-role-card">
                  <SkeletonBase type="circle" :width="60" :height="60" style="margin: 0 auto 10px;" />
                  <SkeletonBase type="text" height="16px" width="80%" style="margin: 0 auto;" />
                </div>
              </el-col>
            </el-row>
          </div>
        </el-col>
        
        <!-- 右侧预约卡片 -->
        <el-col :xs="24" :md="8">
          <div class="skeleton-booking-card">
            <!-- 价格 -->
            <SkeletonBase 
              type="text" 
              height="48px" 
              width="150px" 
              style="margin-bottom: 20px;"
            />
            
            <!-- 信息项 -->
            <div v-for="i in 4" :key="i" style="margin-bottom: 15px;">
              <SkeletonBase type="text" height="20px" width="100%" />
            </div>
            
            <!-- 按钮 -->
            <SkeletonBase 
              type="rect" 
              height="44px" 
              width="100%" 
              :border-radius="22"
              style="margin-top: 20px;"
            />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import SkeletonBase from './SkeletonBase.vue'
</script>

<style scoped>
.skeleton-script-detail {
  background: #fff;
}

.skeleton-cover {
  width: 100%;
  overflow: hidden;
}

.skeleton-detail-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.skeleton-booking-card {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 24px;
  position: sticky;
  top: 80px;
}

.skeleton-role-card {
  text-align: center;
  padding: 15px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 15px;
}

@media (max-width: 768px) {
  .skeleton-detail-content {
    padding: 20px 15px;
  }
  
  .skeleton-booking-card {
    position: static;
    margin-top: 30px;
  }
}
</style>

