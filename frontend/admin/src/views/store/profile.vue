<template>
  <div class="store-profile">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>🏪 门店信息管理</span>
          <el-tag type="success" v-if="store.status === 1">营业中</el-tag>
          <el-tag type="info" v-else>已打烊</el-tag>
        </div>
      </template>
      
      <div v-loading="loading">
        <el-form 
          :model="form" 
          :rules="rules" 
          ref="formRef" 
          label-width="120px"
          @submit.prevent
        >
          <!-- 基本信息 -->
          <div class="section-title">基本信息</div>
          
          <el-form-item label="门店名称">
            <el-input v-model="form.name" disabled>
              <template #prefix>🏠</template>
            </el-input>
            <div class="form-tip">门店名称需联系总部修改</div>
          </el-form-item>
          
          <el-form-item label="门店地址">
            <el-input v-model="form.address" disabled>
              <template #prefix>📍</template>
            </el-input>
            <div class="form-tip">门店地址需联系总部修改</div>
          </el-form-item>
          
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入联系电话">
              <template #prefix>📞</template>
            </el-input>
          </el-form-item>
          
          <el-form-item label="门店简介" prop="description">
            <el-input 
              v-model="form.description" 
              type="textarea" 
              :rows="3"
              placeholder="请输入门店简介，向顾客介绍您的门店特色"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          
          <!-- 营业时间 -->
          <div class="section-title">营业时间</div>
          
          <el-form-item label="营业时间" prop="openTime">
            <div class="time-range">
              <el-time-select
                v-model="form.openTime"
                placeholder="开门时间"
                start="06:00"
                step="00:30"
                end="23:30"
              />
              <span class="time-separator">至</span>
              <el-time-select
                v-model="form.closeTime"
                placeholder="关门时间"
                start="06:00"
                step="00:30"
                end="23:30"
              />
            </div>
          </el-form-item>
          
          <el-form-item label="营业状态">
            <el-switch 
              v-model="form.status" 
              :active-value="1" 
              :inactive-value="0"
              active-text="营业中"
              inactive-text="已打烊"
            />
          </el-form-item>
          
          <!-- 门店图片 -->
          <div class="section-title">门店图片</div>
          
          <el-form-item label="门店图片">
            <div class="image-upload-area">
              <el-upload
                class="image-uploader"
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleImageSuccess"
                :before-upload="beforeUpload"
                accept="image/*"
              >
                <el-button type="primary" :loading="uploading">
                  <el-icon v-if="!uploading"><Upload /></el-icon>
                  {{ uploading ? '上传中...' : '上传图片' }}
                </el-button>
              </el-upload>
              
              <div v-if="imageList.length > 0" class="image-preview-list">
                <div v-for="(img, index) in imageList" :key="index" class="image-preview-item">
                  <el-image :src="img" fit="cover">
                    <template #error>
                      <div class="image-error">加载失败</div>
                    </template>
                  </el-image>
                  <el-button 
                    type="danger" 
                    size="small" 
                    :icon="Delete" 
                    circle
                    class="delete-btn"
                    @click="removeImage(index)"
                  />
                </div>
              </div>
              
              <div class="form-tip">建议尺寸：800x600px，支持jpg/png格式，最大5MB</div>
            </div>
          </el-form-item>
          
          <!-- 提交按钮 -->
          <el-form-item>
            <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
              保存修改
            </el-button>
            <el-button size="large" @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Upload, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref(null)

// 获取门店ID
const storeId = localStorage.getItem('admin-store-id')

// 原始门店数据
const store = reactive({
  status: 1
})

// 上传配置
const uploadUrl = '/api/admin/file/upload'
const uploadHeaders = {
  token: localStorage.getItem('admin-token') || '',
  'X-Client-Type': 'admin'
}

const form = reactive({
  id: null,
  name: '',
  address: '',
  phone: '',
  description: '',
  images: '',
  openTime: '09:00:00',
  closeTime: '22:00:00',
  status: 1
})

// 图片列表
const imageList = computed(() => {
  if (!form.images) return []
  return form.images.split(',').filter(img => img.trim())
})

// 表单验证规则
const rules = {
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/, message: '请输入合法的手机号或座机号', trigger: 'blur' }
  ],
  openTime: [{ required: true, message: '请选择开门时间', trigger: 'change' }],
  closeTime: [{ required: true, message: '请选择关门时间', trigger: 'change' }]
}

// 加载门店信息
const loadStoreInfo = async () => {
  if (!storeId) {
    ElMessage.error('无法获取门店信息')
    return
  }
  
  loading.value = true
  try {
    const res = await request.get(`/store/${storeId}`)
    if (res.code === 1 || res.code === 200) {
      const data = res.data
      Object.assign(store, data)
      // el-time-select 需要 "HH:mm" 格式，后端返回 "HH:mm:ss"，截取前5位
      const toHHmm = (t) => t ? String(t).substring(0, 5) : ''
      Object.assign(form, {
        id: data.id,
        name: data.name || '',
        address: data.address || '',
        phone: data.phone || '',
        description: data.description || '',
        images: data.images || '',
        openTime: toHHmm(data.openTime) || '09:00',
        closeTime: toHHmm(data.closeTime) || '22:00',
        status: data.status ?? 1
      })
    }
  } catch (error) {
    console.error('加载门店信息失败', error)
    ElMessage.error('加载门店信息失败')
  } finally {
    loading.value = false
  }
}

// 上传前校验
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  uploading.value = true
  return true
}

// 上传成功回调
const handleImageSuccess = (response) => {
  uploading.value = false
  if (response.code === 200) {
    if (form.images) {
      form.images += ',' + response.data
    } else {
      form.images = response.data
    }
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 删除图片
const removeImage = (index) => {
  const images = form.images.split(',').filter(img => img.trim())
  images.splice(index, 1)
  form.images = images.join(',')
}

// 重置表单
const resetForm = () => {
  loadStoreInfo()
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    // 表单验证失败，直接返回
    return
  }

  // 验证营业时间
  const openStr = typeof form.openTime === 'string' ? form.openTime : ''
  const closeStr = typeof form.closeTime === 'string' ? form.closeTime : ''
  if (openStr && closeStr && openStr >= closeStr) {
    ElMessage.error('开门时间必须早于关门时间')
    return
  }

  submitting.value = true
  try {
    // el-time-select 返回 "HH:mm"，后端需要 "HH:mm:ss"
    const toHHmmss = (t) => t && t.length === 5 ? t + ':00' : (t || '')
    const res = await request.put('/store', {
      id: form.id,
      phone: form.phone,
      description: form.description,
      images: form.images,
      openTime: toHHmmss(form.openTime),
      closeTime: toHHmmss(form.closeTime),
      status: form.status
    })

    if (res.code === 1 || res.code === 200) {
      ElMessage.success('门店信息更新成功')
    } else {
      ElMessage.error(res.msg || '更新失败')
    }
  } catch (error) {
    console.error('提交失败', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadStoreInfo()
})
</script>

<style scoped>
.store-profile {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 24px 0 16px;
  padding-left: 10px;
  border-left: 4px solid #8B0000;
}

.section-title:first-child {
  margin-top: 0;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.time-range {
  display: flex;
  align-items: center;
  gap: 12px;
}

.time-separator {
  color: #606266;
}

.image-upload-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.image-preview-item {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.image-preview-item .el-image {
  width: 100%;
  height: 100%;
}

.image-preview-item .delete-btn {
  position: absolute;
  top: 4px;
  right: 4px;
}

.image-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  color: #999;
  font-size: 12px;
}
</style>
