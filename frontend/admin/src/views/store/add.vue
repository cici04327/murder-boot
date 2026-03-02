<template>
  <div class="store-add">
    <el-card>
      <template #header>新增门店</template>
      <el-form label-width="100px" :model="form" :rules="rules" ref="formRef" @submit.prevent>
        <el-form-item label="门店名称">
          <el-input v-model="form.name" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" />
        </el-form-item>
        <el-form-item label="联系方式">
          <el-input v-model="form.phone" placeholder="请输入联系方式" />
        </el-form-item>
        
        <el-form-item label="门店图片">
          <div style="display: flex; gap: 10px; align-items: flex-start; flex-direction: column;">
            <div style="display: flex; gap: 10px;">
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
              <el-input v-model="form.images" placeholder="多张图片URL用逗号分隔，或直接上传" style="width: 400px" />
            </div>
            <div v-if="imageList.length > 0" style="display: flex; gap: 10px; flex-wrap: wrap;">
              <div v-for="(img, index) in imageList" :key="index" style="position: relative;">
                <el-image :src="img" style="width: 100px; height: 100px; border-radius: 4px;" fit="cover">
                  <template #error>
                    <div style="width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: #f5f5f5;">
                      <span style="color: #999; font-size: 12px;">加载失败</span>
                    </div>
                  </template>
                </el-image>
                <el-button 
                  type="danger" 
                  size="small" 
                  :icon="Delete" 
                  circle
                  @click="removeImage(index)"
                  style="position: absolute; top: -8px; right: -8px;"
                />
              </div>
            </div>
            <div style="color: #999; font-size: 12px;">
              建议尺寸：800x600px 或 16:9 比例
            </div>
          </div>
        </el-form-item>
        
        <el-form-item label="营业时间">
          <el-time-picker v-model="form.openTime" placeholder="开门时间" format="HH:mm:ss" value-format="HH:mm:ss" />
          <span style="margin: 0 8px">-</span>
          <el-time-picker v-model="form.closeTime" placeholder="关门时间" format="HH:mm:ss" value-format="HH:mm:ss" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, watch } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Upload, Delete } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const submitting = ref(false)
const uploading = ref(false)
const formRef = ref(null)

// 上传配置
const uploadUrl = '/api/admin/file/upload'
const uploadHeaders = {
  token: localStorage.getItem('admin-token') || '',
  'X-Client-Type': 'admin'
}

const form = reactive({
  name: '',
  address: '',
  phone: '',
  description: '',
  images: '',
  openTime: '09:00:00',
  closeTime: '22:00:00',
  status: 1
})

// 图片列表（用于预览）
const imageList = computed(() => {
  if (!form.images) return []
  return form.images.split(',').filter(img => img.trim())
})

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
    // 追加图片URL
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

const rules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系方式', trigger: 'blur' },
    { pattern: /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})$/, message: '请输入合法的手机号或座机号', trigger: 'blur' }
  ],
  openTime: [{ required: true, message: '请选择开门时间', trigger: 'change' }],
  closeTime: [{ required: true, message: '请选择关门时间', trigger: 'change' }]
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    if (form.openTime && form.closeTime && form.openTime >= form.closeTime) {
      ElMessage.error('开门时间必须早于关门时间')
      return
    }
    submitting.value = true
    await request.post('/store', form)
    ElMessage.success('门店新增成功')
    router.push('/store/list')
  } catch (e) {
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.store-add { width: 100%; }
</style>
