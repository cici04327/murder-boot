<template>
  <div class="store-list">
    <el-card>
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="门店名称">
          <el-input v-model="queryForm.name" placeholder="请输入门店名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增门店</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <el-image
              v-if="row.images"
              :src="row.images.split(',')[0]"
              :preview-src-list="row.images.split(',')"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 4px; cursor: pointer;"
            />
            <span v-else style="color: #ccc; font-size: 12px;">无封面</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="门店名称" width="180" />
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="营业时间" width="180">
          <template #default="{ row }">
            <span v-if="row.openTime && row.closeTime">
              {{ row.openTime }} - {{ row.closeTime }}
            </span>
            <span v-else style="color: #ccc;">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="130">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" :max="5" v-if="row.rating" />
            <span v-else style="color: #ccc;">暂无评分</span>
          </template>
        </el-table-column>
        <el-table-column label="登录账号" width="130">
          <template #default="{ row }">
            <span v-if="row.loginAccount" class="login-account">{{ row.loginAccount }}</span>
            <el-tag v-else type="warning" size="small">未设置</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getBusinessStatusType(row)" size="small">
              {{ getBusinessStatusText(row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" size="small" @click="handleAccountManage(row)">账号管理</el-button>
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="门店名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入门店名称" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="门店地址" prop="address">
          <el-input v-model="formData.address" placeholder="请输入门店地址" />
        </el-form-item>
        <el-form-item label="开始时间" prop="openTime">
          <el-time-picker
            v-model="formData.openTime"
            format="HH:mm:ss"
            value-format="HH:mm:ss"
            placeholder="选择营业开始时间"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="closeTime">
          <el-time-picker
            v-model="formData.closeTime"
            format="HH:mm:ss"
            value-format="HH:mm:ss"
            placeholder="选择营业结束时间"
          />
        </el-form-item>
        <el-form-item label="门店描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入门店描述"
          />
        </el-form-item>
        <el-form-item label="门店封面">
          <div style="display: flex; gap: 10px; align-items: flex-start; flex-direction: column;">
            <el-upload
              class="cover-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-button type="primary" :loading="uploading">
                <el-icon v-if="!uploading"><Upload /></el-icon>
                {{ uploading ? '上传中...' : '上传封面' }}
              </el-button>
            </el-upload>
            <div v-if="formData.images" style="display: flex; gap: 10px; flex-wrap: wrap;">
              <div v-for="(img, index) in formData.images.split(',')" :key="index" style="position: relative;">
                <el-image
                  :src="img"
                  fit="cover"
                  style="width: 100px; height: 100px; border-radius: 4px;"
                  :preview-src-list="formData.images.split(',')"
                  :initial-index="index"
                />
                <el-button
                  type="danger"
                  size="small"
                  circle
                  @click="removeImage(index)"
                  style="position: absolute; top: -8px; right: -8px;"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
              </div>
            </div>
            <span style="color: #999; font-size: 12px;">建议尺寸：800x600，支持多张图片</span>
          </div>
        </el-form-item>
        <el-form-item label="门店评分">
          <el-rate v-model="formData.rating" :max="5" show-score />
          <span style="margin-left: 10px; color: #999; font-size: 12px;">用于展示门店服务质量</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :label="1">营业中</el-radio>
            <el-radio :label="0">已停业</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 账号管理对话框 -->
    <el-dialog
      v-model="accountDialogVisible"
      title="门店账号管理"
      width="500px"
      @close="handleAccountDialogClose"
    >
      <div class="account-info">
        <div class="store-name">{{ currentStore.name }}</div>
        <div class="store-id">门店ID: {{ currentStore.id }}</div>
      </div>
      
      <el-form :model="accountForm" :rules="accountRules" ref="accountFormRef" label-width="100px">
        <el-form-item label="登录账号" prop="loginAccount">
          <el-input v-model="accountForm.loginAccount" placeholder="请输入登录账号">
            <template #prepend>🔑</template>
          </el-input>
          <div class="form-tip">账号用于门店管理员登录，建议使用易记的名称</div>
        </el-form-item>
        <el-form-item label="登录密码" prop="loginPassword">
          <el-input 
            v-model="accountForm.loginPassword" 
            type="password" 
            placeholder="留空则不修改密码"
            show-password
          >
            <template #prepend>🔐</template>
          </el-input>
          <div class="form-tip">设置新密码，留空表示保留原密码</div>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword" v-if="accountForm.loginPassword">
          <el-input 
            v-model="accountForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="accountDialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleResetPassword">重置为默认密码</el-button>
        <el-button type="primary" @click="handleSaveAccount">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Close } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const uploading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('新增门店')
const formRef = ref(null)

// 账号管理相关
const accountDialogVisible = ref(false)
const accountFormRef = ref(null)
const currentStore = reactive({
  id: null,
  name: ''
})
const accountForm = reactive({
  loginAccount: '',
  loginPassword: '',
  confirmPassword: ''
})

// 账号表单验证规则
const accountRules = {
  loginAccount: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 3, max: 30, message: '账号长度应在3-30个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (accountForm.loginPassword && value !== accountForm.loginPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 上传配置
const uploadUrl = '/api/admin/file/upload/store-cover'
const uploadHeaders = {
  token: localStorage.getItem('admin-token') || '',
  'X-Client-Type': 'admin'
}

const queryForm = reactive({
  name: '',
  page: 1,
  pageSize: 10
})

const formData = reactive({
  id: null,
  name: '',
  phone: '',
  address: '',
  openTime: '09:00:00',
  closeTime: '22:00:00',
  description: '',
  images: '',
  rating: 0,
  status: 1
})

// 判断营业状态
const getBusinessStatusType = (row) => {
  if (row.status === 0) return 'danger' // 已停业
  
  if (!row.openTime || !row.closeTime) return 'warning' // 未设置营业时间
  
  // 判断当前时间是否在营业时间内
  const now = new Date()
  const currentTime = now.getHours() * 60 + now.getMinutes()
  
  const [openHour, openMin] = row.openTime.split(':').map(Number)
  const [closeHour, closeMin] = row.closeTime.split(':').map(Number)
  
  const openMinutes = openHour * 60 + openMin
  const closeMinutes = closeHour * 60 + closeMin
  
  if (currentTime >= openMinutes && currentTime <= closeMinutes) {
    return 'success' // 营业中
  }
  return 'info' // 休息中
}

const getBusinessStatusText = (row) => {
  if (row.status === 0) return '已停业'
  
  if (!row.openTime || !row.closeTime) return '未设置'
  
  const now = new Date()
  const currentTime = now.getHours() * 60 + now.getMinutes()
  
  const [openHour, openMin] = row.openTime.split(':').map(Number)
  const [closeHour, closeMin] = row.closeTime.split(':').map(Number)
  
  const openMinutes = openHour * 60 + openMin
  const closeMinutes = closeHour * 60 + closeMin
  
  if (currentTime >= openMinutes && currentTime <= closeMinutes) {
    return '营业中'
  }
  return '休息中'
}

const formRules = {
  name: [{ required: true, message: '请输入门店名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入门店地址', trigger: 'blur' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    console.log('加载门店列表，参数:', queryForm)
    const res = await request.get('/store/page', { params: queryForm })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (newPage) => {
  console.log('门店列表页码变化:', newPage)
  queryForm.page = newPage
  fetchData()
}

const handleSizeChange = (newSize) => {
  console.log('门店列表每页大小变化:', newSize)
  queryForm.pageSize = newSize
  queryForm.page = 1
  fetchData()
}

const handleQuery = () => {
  queryForm.page = 1
  fetchData()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.page = 1
  fetchData()
}

const handleAdd = () => {
  dialogTitle.value = '新增门店'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑门店'
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该门店吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.delete(`/store/${row.id}`)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (formData.id) {
      await request.put('/store', formData)
      ElMessage.success('更新成功')
    } else {
      await request.post('/store', formData)
      ElMessage.success('新增成功')
    }
    
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: null,
    name: '',
    phone: '',
    address: '',
    openTime: '09:00:00',
    closeTime: '22:00:00',
    description: '',
    images: '',
    rating: 0,
    status: 1
  })
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
const handleCoverSuccess = (response) => {
  uploading.value = false
  if (response.code === 200) {
    // 如果已有图片，追加新图片
    if (formData.images) {
      formData.images = formData.images + ',' + response.data
    } else {
      formData.images = response.data
    }
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 删除图片
const removeImage = (index) => {
  const images = formData.images.split(',')
  images.splice(index, 1)
  formData.images = images.join(',')
}

// ========== 账号管理相关方法 ==========

// 打开账号管理对话框
const handleAccountManage = (row) => {
  currentStore.id = row.id
  currentStore.name = row.name
  accountForm.loginAccount = row.loginAccount || `store_${row.id}`
  accountForm.loginPassword = ''
  accountForm.confirmPassword = ''
  accountDialogVisible.value = true
}

// 关闭账号管理对话框
const handleAccountDialogClose = () => {
  accountFormRef.value?.resetFields()
  accountForm.loginAccount = ''
  accountForm.loginPassword = ''
  accountForm.confirmPassword = ''
}

// 保存账号信息
const handleSaveAccount = async () => {
  try {
    await accountFormRef.value.validate()
    
    // 如果设置了密码，检查确认密码
    if (accountForm.loginPassword && accountForm.loginPassword !== accountForm.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    
    const data = {
      id: currentStore.id,
      loginAccount: accountForm.loginAccount
    }
    
    // 如果设置了新密码，添加到请求中
    if (accountForm.loginPassword) {
      data.loginPassword = accountForm.loginPassword
    }
    
    const res = await request.put('/store/account', data)
    if (res.code === 200 || res.code === 1) {
      ElMessage.success('账号信息保存成功')
      accountDialogVisible.value = false
      fetchData() // 刷新列表
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (error) {
    console.error('保存账号失败:', error)
    ElMessage.error('保存账号信息失败')
  }
}

// 重置为默认密码
const handleResetPassword = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要将门店【${currentStore.name}】的密码重置为默认密码(123456)吗？`,
      '重置密码确认',
      {
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const res = await request.put('/store/account/reset-password', {
      id: currentStore.id
    })
    
    if (res.code === 200 || res.code === 1) {
      ElMessage.success('密码已重置为: 123456')
      accountDialogVisible.value = false
    } else {
      ElMessage.error(res.msg || '重置失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
      ElMessage.error('重置密码失败')
    }
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.store-list {
  width: 100%;
}

.query-form {
  margin-bottom: 20px;
}

/* 样式已移至全局主题文件 murder-theme.css */
</style>
