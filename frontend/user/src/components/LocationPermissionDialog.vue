<template>
  <el-dialog
    v-model="visible"
    title=""
    width="400px"
    :show-close="false"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    class="location-dialog"
    center
  >
    <div class="dialog-content">
      <!-- 图标 -->
      <div class="icon-wrapper">
        <div class="icon-bg">
          <el-icon class="location-icon"><Location /></el-icon>
        </div>
        <div class="pulse-ring"></div>
      </div>

      <!-- 标题 -->
      <h3 class="dialog-title">获取您的位置</h3>

      <!-- 说明文字 -->
      <p class="dialog-desc">
        为了显示门店与您的距离，我们需要获取您的位置信息
      </p>

      <!-- 功能说明 -->
      <div class="feature-list">
        <div class="feature-item">
          <el-icon class="feature-icon"><CircleCheck /></el-icon>
          <span>查看门店离您的距离</span>
        </div>
        <div class="feature-item">
          <el-icon class="feature-icon"><CircleCheck /></el-icon>
          <span>按距离远近排序门店</span>
        </div>
        <div class="feature-item">
          <el-icon class="feature-icon"><CircleCheck /></el-icon>
          <span>发现附近的优质门店</span>
        </div>
      </div>

      <!-- 隐私说明 -->
      <div class="privacy-note">
        <el-icon><InfoFilled /></el-icon>
        <span>您的位置信息仅用于计算距离，不会被上传或存储</span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleDeny" size="large">
          暂不需要
        </el-button>
        <el-button type="primary" @click="handleAllow" size="large" :loading="loading">
          <el-icon v-if="!loading"><Location /></el-icon>
          {{ loading ? '获取中...' : '允许获取位置' }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Location, CircleCheck, InfoFilled } from '@element-plus/icons-vue'
import { getUserLocation, requestLocationPermission } from '@/utils/location'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success', 'deny'])

const visible = ref(false)
const loading = ref(false)

// 同步显示状态
watch(() => props.modelValue, (val) => {
  visible.value = val
})

watch(visible, (val) => {
  emit('update:modelValue', val)
})

// 允许获取位置
const handleAllow = async () => {
  loading.value = true
  
  try {
    // 调用获取位置
    const location = await getUserLocation(true)
    
    // 保存用户已授权的标记
    localStorage.setItem('location_permission_granted', 'true')
    
    ElMessage.success('位置获取成功！')
    emit('success', location)
    visible.value = false
  } catch (error) {
    console.error('获取位置失败:', error)
    
    let errorMsg = '获取位置失败'
    if (error.code === 1) {
      errorMsg = '您拒绝了位置权限，可在浏览器设置中重新开启'
    } else if (error.code === 2) {
      errorMsg = '无法获取位置信息，请检查设备定位是否开启'
    } else if (error.code === 3) {
      errorMsg = '获取位置超时，请稍后重试'
    }
    
    ElMessage.warning(errorMsg)
    emit('deny', error)
    visible.value = false
  } finally {
    loading.value = false
  }
}

// 拒绝获取位置
const handleDeny = () => {
  // 保存用户选择不获取位置
  localStorage.setItem('location_permission_denied', 'true')
  emit('deny', null)
  visible.value = false
}

// 检查是否需要显示弹窗
const checkShouldShow = async () => {
  // 如果用户之前已经授权
  if (localStorage.getItem('location_permission_granted') === 'true') {
    return false
  }
  
  // 如果用户之前已经拒绝（本次会话不再提示）
  if (sessionStorage.getItem('location_permission_asked') === 'true') {
    return false
  }
  
  // 检查浏览器权限状态
  const permissionState = await requestLocationPermission()
  
  // 如果已经授权或已经拒绝，不显示弹窗
  if (permissionState === 'granted' || permissionState === 'denied') {
    return false
  }
  
  return true
}

// 暴露方法
defineExpose({
  checkShouldShow
})
</script>

<style scoped>
.location-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.location-dialog :deep(.el-dialog__header) {
  display: none;
}

.location-dialog :deep(.el-dialog__body) {
  padding: 30px 30px 20px;
}

.dialog-content {
  text-align: center;
}

.icon-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 20px;
}

.icon-bg {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #16213e 0%, #0f3460 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
}

.location-icon {
  font-size: 40px;
  color: #fff;
}

.pulse-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 3px solid rgba(224, 90, 71, 0.72);
  animation: pulse 2s ease-out infinite;
  z-index: 1;
}

@keyframes pulse {
  0% {
    width: 80px;
    height: 80px;
    opacity: 0.8;
  }
  100% {
    width: 120px;
    height: 120px;
    opacity: 0;
  }
}

.dialog-title {
  font-size: 22px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.88);
  margin-bottom: 12px;
}

.dialog-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.66);
  margin-bottom: 24px;
  line-height: 1.6;
}

.feature-list {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.96) 0%, rgba(22, 33, 62, 0.94) 100%);
  border: 1px solid rgba(192, 57, 43, 0.18);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 16px;
  text-align: left;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.82);
}

.feature-icon {
  color: #67c23a;
  font-size: 18px;
}

.privacy-note {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.54);
  padding: 10px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 8px;
}

.privacy-note .el-icon {
  font-size: 14px;
}

.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.dialog-footer .el-button {
  min-width: 140px;
  border-radius: 8px;
}

.dialog-footer .el-button--primary {
  background: linear-gradient(135deg, #16213e 0%, #0f3460 100%);
  border: none;
}

.dialog-footer .el-button--primary:hover {
  background: linear-gradient(135deg, #55141d 0%, #7a1d1d 100%);
}
</style>
