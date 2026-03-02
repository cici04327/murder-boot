<template>
  <div class="user-addresses">
    <el-card class="page-header">
      <div class="header-content">
        <div>
          <h2>我的地址</h2>
          <p class="subtitle">管理您的收货地址</p>
        </div>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增地址
        </el-button>
      </div>
    </el-card>

    <!-- 地址列表 -->
    <el-card class="address-list-card">
      <div v-loading="loading" class="address-list">
        <div v-if="addresses.length === 0" class="empty-state">
          <el-empty description="暂无地址">
            <el-button type="primary" @click="handleAdd">添加地址</el-button>
          </el-empty>
        </div>

        <el-row :gutter="20" v-else>
          <el-col :span="8" v-for="address in addresses" :key="address.id">
            <div class="address-item" :class="{ 'default': address.isDefault }">
              <div class="address-header">
                <el-tag v-if="address.isDefault" type="danger" size="small">默认</el-tag>
                <el-tag v-else type="info" size="small">备用</el-tag>
              </div>
              <div class="address-content">
                <h3 class="contact-name">
                  {{ address.receiverName }}
                  <span class="contact-phone">{{ address.receiverPhone }}</span>
                </h3>
                <p class="address-detail">
                  {{ address.province }} {{ address.city }} {{ address.district }}
                  {{ address.detail }}
                </p>
              </div>
              <div class="address-actions">
                <el-button 
                  v-if="!address.isDefault" 
                  type="primary" 
                  size="small" 
                  link
                  @click="handleSetDefault(address.id)"
                >
                  设为默认
                </el-button>
                <el-button 
                  type="primary" 
                  size="small" 
                  link
                  @click="handleEdit(address)"
                >
                  编辑
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  link
                  @click="handleDelete(address.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 添加/编辑地址对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="formData.receiverName" placeholder="请输入收货人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="receiverPhone">
          <el-input v-model="formData.receiverPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="所在地区" prop="region">
          <el-cascader
            v-model="formData.region"
            :options="regionOptions"
            placeholder="请选择省/市/区"
            style="width: 100%"
            @change="handleRegionChange"
          />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input
            v-model="formData.detail"
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址，如街道、小区、楼栋号等"
          />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="formData.isDefault" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { 
  getUserAddresses, 
  addUserAddress, 
  updateUserAddress, 
  deleteUserAddress,
  setDefaultAddress 
} from '@/api/user'

// 状态
const loading = ref(false)
const addresses = ref([])
const showDialog = ref(false)
const dialogTitle = ref('新增地址')
const formRef = ref(null)
const submitting = ref(false)
const editingId = ref(null)

// 表单数据
const formData = reactive({
  receiverName: '',
  receiverPhone: '',
  region: [],
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

// 地区选项（简化版，实际应该从接口获取）
const regionOptions = ref([
  {
    value: '北京市',
    label: '北京市',
    children: [
      {
        value: '北京市',
        label: '北京市',
        children: [
          { value: '东城区', label: '东城区' },
          { value: '西城区', label: '西城区' },
          { value: '朝阳区', label: '朝阳区' },
          { value: '海淀区', label: '海淀区' }
        ]
      }
    ]
  },
  {
    value: '上海市',
    label: '上海市',
    children: [
      {
        value: '上海市',
        label: '上海市',
        children: [
          { value: '黄浦区', label: '黄浦区' },
          { value: '徐汇区', label: '徐汇区' },
          { value: '长宁区', label: '长宁区' },
          { value: '静安区', label: '静安区' }
        ]
      }
    ]
  },
  {
    value: '广东省',
    label: '广东省',
    children: [
      {
        value: '广州市',
        label: '广州市',
        children: [
          { value: '天河区', label: '天河区' },
          { value: '越秀区', label: '越秀区' },
          { value: '海珠区', label: '海珠区' }
        ]
      },
      {
        value: '深圳市',
        label: '深圳市',
        children: [
          { value: '福田区', label: '福田区' },
          { value: '南山区', label: '南山区' },
          { value: '宝安区', label: '宝安区' }
        ]
      }
    ]
  }
])

// 表单验证规则
const rules = {
  receiverName: [
    { required: true, message: '请输入收货人姓名', trigger: 'blur' }
  ],
  receiverPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  region: [
    { required: true, message: '请选择所在地区', trigger: 'change' }
  ],
  detail: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ]
}

// 加载地址列表
const loadAddresses = async () => {
  loading.value = true
  try {
    const res = await getUserAddresses()
    if (res.code === 1) {
      addresses.value = res.data || []
    }
  } catch (error) {
    console.error('加载地址失败:', error)
    ElMessage.error('加载地址失败')
  } finally {
    loading.value = false
  }
}

// 处理地区选择
const handleRegionChange = (value) => {
  if (value && value.length === 3) {
    formData.province = value[0]
    formData.city = value[1]
    formData.district = value[2]
  }
}

// 新增地址
const handleAdd = () => {
  dialogTitle.value = '新增地址'
  editingId.value = null
  resetForm()
  showDialog.value = true
}

// 编辑地址
const handleEdit = (address) => {
  dialogTitle.value = '编辑地址'
  editingId.value = address.id
  
  formData.receiverName = address.receiverName
  formData.receiverPhone = address.receiverPhone
  formData.region = [address.province, address.city, address.district]
  formData.province = address.province
  formData.city = address.city
  formData.district = address.district
  formData.detail = address.detail
  formData.isDefault = address.isDefault === 1
  
  showDialog.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const data = {
          receiverName: formData.receiverName,
          receiverPhone: formData.receiverPhone,
          province: formData.province,
          city: formData.city,
          district: formData.district,
          detail: formData.detail,
          isDefault: formData.isDefault ? 1 : 0
        }
        
        let res
        if (editingId.value) {
          res = await updateUserAddress(editingId.value, data)
        } else {
          res = await addUserAddress(data)
        }
        
        if (res.code === 1) {
          ElMessage.success(editingId.value ? '更新成功' : '添加成功')
          showDialog.value = false
          loadAddresses()
        }
      } catch (error) {
        console.error('保存地址失败:', error)
      } finally {
        submitting.value = false
      }
    }
  })
}

// 设为默认地址
const handleSetDefault = async (id) => {
  try {
    const res = await setDefaultAddress(id)
    if (res.code === 1) {
      ElMessage.success('已设为默认地址')
      loadAddresses()
    }
  } catch (error) {
    console.error('设置默认地址失败:', error)
  }
}

// 删除地址
const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除这个地址吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const res = await deleteUserAddress(id)
    if (res.code === 1) {
      ElMessage.success('删除成功')
      loadAddresses()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除地址失败:', error)
    }
  }
}

// 重置表单
const resetForm = () => {
  formData.receiverName = ''
  formData.receiverPhone = ''
  formData.region = []
  formData.province = ''
  formData.city = ''
  formData.district = ''
  formData.detail = ''
  formData.isDefault = false
  
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

// 初始化
onMounted(() => {
  loadAddresses()
})
</script>

<style scoped>
.user-addresses {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

/* 地址列表 */
.address-list {
  min-height: 400px;
}

.address-item {
  border: 2px solid #DCDFE6;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.address-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.address-item.default {
  border-color: #F56C6C;
  background-color: #FEF0F0;
}

.address-header {
  margin-bottom: 15px;
}

.address-content {
  margin-bottom: 15px;
}

.contact-name {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: #303133;
}

.contact-phone {
  margin-left: 10px;
  font-size: 14px;
  color: #909399;
  font-weight: normal;
}

.address-detail {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.address-actions {
  display: flex;
  gap: 15px;
  padding-top: 15px;
  border-top: 1px solid #EBEEF5;
}

/* 空状态 */
.empty-state {
  padding: 60px 0;
  text-align: center;
}
</style>
