<template>
  <div class="role-management">
    <el-card>
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>剧本角色管理</span>
          <el-button type="primary" @click="handleAdd">新增角色</el-button>
        </div>
      </template>

      <!-- 剧本选择 -->
      <el-form :inline="true">
        <el-form-item label="选择剧本">
          <el-select 
            v-model="selectedScriptId" 
            placeholder="请选择剧本" 
            filterable
            @change="loadRoles"
            style="width: 300px"
          >
            <el-option 
              v-for="script in scripts" 
              :key="script.id" 
              :label="script.name" 
              :value="script.id" 
            />
          </el-select>
        </el-form-item>
      </el-form>

      <!-- 角色列表 -->
      <el-table 
        :data="roles" 
        v-loading="loading"
        border
        stripe
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="100">
          <template #default="{ row }">
            <el-image 
              v-if="row.avatar"
              :src="row.avatar" 
              style="width: 60px; height: 60px; border-radius: 50%;"
              fit="cover"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
            <span v-else style="color: #999;">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色名称" width="120" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">
            <el-tag :type="getGenderType(row.gender)">
              {{ getGenderText(row.gender) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ageRange" label="年龄" width="100" />
        <el-table-column label="难度" width="120">
          <template #default="{ row }">
            <el-rate v-model="row.difficulty" disabled :max="3" show-score />
          </template>
        </el-table-column>
        <el-table-column prop="description" label="角色描述" show-overflow-tooltip />
        <el-table-column label="立绘" width="100">
          <template #default="{ row }">
            <el-button 
              v-if="row.characterImage" 
              type="primary" 
              link 
              @click="previewImage(row.characterImage)"
            >
              查看
            </el-button>
            <span v-else style="color: #999;">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-popconfirm 
              title="确定删除此角色吗？" 
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="700px"
      @close="resetForm"
    >
      <el-form 
        :model="form" 
        :rules="rules" 
        ref="formRef" 
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
        </el-form-item>

        <el-form-item label="角色头像">
          <div style="display: flex; gap: 10px; align-items: flex-start;">
            <el-upload
              class="avatar-uploader"
              :action="uploadAvatarUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button type="primary" :loading="uploadingAvatar">
                <el-icon v-if="!uploadingAvatar"><Upload /></el-icon>
                {{ uploadingAvatar ? '上传中...' : '上传头像' }}
              </el-button>
            </el-upload>
            <el-input 
              v-model="form.avatar" 
              placeholder="或手动输入头像URL" 
              style="width: 300px" 
            />
          </div>
          <div v-if="form.avatar" style="margin-top: 10px;">
            <el-image 
              :src="form.avatar" 
              style="width: 80px; height: 80px; border-radius: 50%;" 
              fit="cover"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          <div style="margin-top: 5px; color: #999; font-size: 12px;">
            建议尺寸：正方形，如 200x200px
          </div>
        </el-form-item>

        <el-form-item label="角色立绘">
          <div style="display: flex; gap: 10px; align-items: flex-start;">
            <el-upload
              class="character-uploader"
              :action="uploadCharacterUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleCharacterSuccess"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button type="primary" :loading="uploadingCharacter">
                <el-icon v-if="!uploadingCharacter"><Upload /></el-icon>
                {{ uploadingCharacter ? '上传中...' : '上传立绘' }}
              </el-button>
            </el-upload>
            <el-input 
              v-model="form.characterImage" 
              placeholder="或手动输入立绘URL" 
              style="width: 300px" 
            />
          </div>
          <div v-if="form.characterImage" style="margin-top: 10px;">
            <el-image 
              :src="form.characterImage" 
              style="width: 100px; height: auto;" 
              fit="contain"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><Picture /></el-icon>
                </div>
              </template>
            </el-image>
          </div>
          <div style="margin-top: 5px; color: #999; font-size: 12px;">
            建议尺寸：竖版图片，如 500x800px
          </div>
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
            <el-radio :label="3">不限</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="年龄范围" prop="ageRange">
          <el-input v-model="form.ageRange" placeholder="如：20-30岁" />
        </el-form-item>

        <el-form-item label="角色难度" prop="difficulty">
          <el-rate v-model="form.difficulty" :max="3" show-text />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">
            1-简单 2-普通 3-困难
          </span>
        </el-form-item>

        <el-form-item label="角色描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入角色背景、性格、特点等描述"
          />
        </el-form-item>

        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">
            数字越小越靠前
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog v-model="previewVisible" title="立绘预览" width="600px">
      <el-image :src="previewImageUrl" fit="contain" style="width: 100%;" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload, Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const dialogVisible = ref(false)
const previewVisible = ref(false)
const previewImageUrl = ref('')
const isEdit = ref(false)
const submitting = ref(false)
const uploadingAvatar = ref(false)
const uploadingCharacter = ref(false)

const scripts = ref([])
const selectedScriptId = ref(null)
const roles = ref([])
const formRef = ref(null)

// 上传配置
const uploadAvatarUrl = '/api/admin/file/upload/role-avatar'
const uploadCharacterUrl = '/api/admin/file/upload/role-character'
const uploadHeaders = {
  token: localStorage.getItem('admin-token') || '',
  'X-Client-Type': 'admin'
}

const form = reactive({
  id: null,
  scriptId: null,
  roleName: '',
  avatar: '',
  characterImage: '',
  gender: 3,
  ageRange: '',
  description: '',
  difficulty: 2,
  sort: 0
})

const rules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

// 加载剧本列表
const loadScripts = async () => {
  try {
    const res = await request.get('/script/page', {
      params: { page: 1, pageSize: 1000 }
    })
    scripts.value = res.data.records || []
  } catch (error) {
    console.error('加载剧本列表失败:', error)
    ElMessage.error('加载剧本列表失败')
  }
}

// 加载角色列表
const loadRoles = async () => {
  if (!selectedScriptId.value) {
    roles.value = []
    return
  }

  loading.value = true
  try {
    const res = await request.get('/script/role/list', {
      params: { scriptId: selectedScriptId.value }
    })
    roles.value = res.data || []
  } catch (error) {
    console.error('加载角色列表失败:', error)
    ElMessage.error('加载角色列表失败')
  } finally {
    loading.value = false
  }
}

// 性别文本映射
const getGenderText = (gender) => {
  const map = { 1: '男', 2: '女', 3: '不限' }
  return map[gender] || '未知'
}

// 性别标签类型
const getGenderType = (gender) => {
  const map = { 1: 'primary', 2: 'danger', 3: 'info' }
  return map[gender] || 'info'
}

// 新增角色
const handleAdd = () => {
  if (!selectedScriptId.value) {
    ElMessage.warning('请先选择剧本')
    return
  }
  isEdit.value = false
  resetForm()
  form.scriptId = selectedScriptId.value
  dialogVisible.value = true
}

// 编辑角色
const handleEdit = (row) => {
  isEdit.value = true
  form.id = row.id
  form.scriptId = row.scriptId
  form.roleName = row.roleName
  form.avatar = row.avatar || ''
  form.characterImage = row.characterImage || ''
  form.gender = row.gender
  form.ageRange = row.ageRange || ''
  form.description = row.description || ''
  form.difficulty = row.difficulty || 2
  form.sort = row.sort || 0
  dialogVisible.value = true
}

// 删除角色
const handleDelete = async (id) => {
  try {
    await request.delete(`/script/role/${id}`)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    submitting.value = true

    if (isEdit.value) {
      await request.put('/script/role', form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/script/role', form)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    loadRoles()
  } catch (error) {
    console.error('提交失败:', error)
    if (error !== 'cancel') {
      ElMessage.error('提交失败')
    }
  } finally {
    submitting.value = false
  }
}

// 重置表单
const resetForm = () => {
  form.id = null
  form.scriptId = null
  form.roleName = ''
  form.avatar = ''
  form.characterImage = ''
  form.gender = 3
  form.ageRange = ''
  form.description = ''
  form.difficulty = 2
  form.sort = 0
  if (formRef.value) {
    formRef.value.clearValidate()
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
  return true
}

// 头像上传成功
const handleAvatarSuccess = (response) => {
  uploadingAvatar.value = false
  if (response.code === 200) {
    form.avatar = response.data
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 立绘上传成功
const handleCharacterSuccess = (response) => {
  uploadingCharacter.value = false
  if (response.code === 200) {
    form.characterImage = response.data
    ElMessage.success('立绘上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 预览图片
const previewImage = (url) => {
  previewImageUrl.value = url
  previewVisible.value = true
}

onMounted(() => {
  loadScripts()
})
</script>

<style scoped>
.role-management {
  padding: 20px;
}

.image-slot {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #909399;
  font-size: 24px;
}
</style>
