<template>
  <div class="address-container">
    <el-page-header @back="goBack" title="返回" content="地址管理" />
    
    <el-card style="margin-top: 20px;">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">新增地址</el-button>
      </div>

      <el-table :data="addresses" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="contactName" label="联系人" width="120" />
        <el-table-column label="联系电话" width="130">
          <template #default="{ row }">
            {{ row.phone || row.contactPhone }}
          </template>
        </el-table-column>
        <el-table-column label="地址">
          <template #default="{ row }">
            {{ row.province }} {{ row.city }} {{ row.district }} {{ row.detailAddress || row.detail }}
          </template>
        </el-table-column>
        <el-table-column prop="isDefault" label="默认地址" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isDefault === 1 ? 'success' : 'info'" size="small">
              {{ row.isDefault === 1 ? '默认' : '非默认' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button 
              v-if="row.isDefault !== 1"
              type="success" 
              link 
              @click="handleSetDefault(row)"
            >
              设为默认
            </el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑地址对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="600px"
      @close="addressFormRef?.resetFields()"
    >
      <el-form 
        ref="addressFormRef" 
        :model="addressForm" 
        :rules="addressRules" 
        label-width="100px"
      >
        <el-form-item label="联系人" prop="contactName">
          <el-input 
            v-model="addressForm.contactName" 
            placeholder="请输入联系人姓名"
            maxlength="20"
          />
        </el-form-item>

        <el-form-item label="联系电话" prop="contactPhone">
          <el-input 
            v-model="addressForm.contactPhone" 
            placeholder="请输入联系电话"
            maxlength="11"
          />
        </el-form-item>

        <el-form-item label="省份" prop="province">
          <el-select 
            v-model="addressForm.province" 
            placeholder="请选择省份"
            @change="handleProvinceChange"
            style="width: 100%"
            filterable
          >
            <el-option 
              v-for="item in provinceOptions" 
              :key="item.value" 
              :label="item.label" 
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="城市" prop="city">
          <el-select 
            v-model="addressForm.city" 
            placeholder="请选择城市"
            @change="handleCityChange"
            style="width: 100%"
            filterable
            :disabled="!cityOptions.length"
          >
            <el-option 
              v-for="item in cityOptions" 
              :key="item.value" 
              :label="item.label" 
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="区县" prop="district">
          <el-select 
            v-model="addressForm.district" 
            placeholder="请选择区县"
            style="width: 100%"
            filterable
            :disabled="!districtOptions.length"
          >
            <el-option 
              v-for="item in districtOptions" 
              :key="item.value" 
              :label="item.label" 
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="详细地址" prop="detail">
          <el-input 
            v-model="addressForm.detail" 
            type="textarea"
            :rows="3"
            placeholder="请输入详细地址（街道、门牌号等）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="默认地址">
          <el-switch 
            v-model="addressForm.isDefault" 
            :active-value="1" 
            :inactive-value="0"
          />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            设为默认地址后，其他地址将自动取消默认
          </span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userId = route.params.id

const addresses = ref([])
const loading = ref(false)

// 加载地址列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get(`/user/address/list/${userId}`)
    addresses.value = res.data || []
  } catch (error) {
    console.error('加载地址列表失败:', error)
    ElMessage.error('加载地址列表失败')
    addresses.value = []
  } finally {
    loading.value = false
  }
}

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增地址')
const addressForm = ref({
  id: null,
  contactName: '',
  contactPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0
})
const addressFormRef = ref(null)

// 省市区数据
const provinceOptions = ref([
  { value: '北京市', label: '北京市' },
  { value: '上海市', label: '上海市' },
  { value: '天津市', label: '天津市' },
  { value: '重庆市', label: '重庆市' },
  { value: '河北省', label: '河北省' },
  { value: '山西省', label: '山西省' },
  { value: '辽宁省', label: '辽宁省' },
  { value: '吉林省', label: '吉林省' },
  { value: '黑龙江省', label: '黑龙江省' },
  { value: '江苏省', label: '江苏省' },
  { value: '浙江省', label: '浙江省' },
  { value: '安徽省', label: '安徽省' },
  { value: '福建省', label: '福建省' },
  { value: '江西省', label: '江西省' },
  { value: '山东省', label: '山东省' },
  { value: '河南省', label: '河南省' },
  { value: '湖北省', label: '湖北省' },
  { value: '湖南省', label: '湖南省' },
  { value: '广东省', label: '广东省' },
  { value: '海南省', label: '海南省' },
  { value: '四川省', label: '四川省' },
  { value: '贵州省', label: '贵州省' },
  { value: '云南省', label: '云南省' },
  { value: '陕西省', label: '陕西省' },
  { value: '甘肃省', label: '甘肃省' },
  { value: '青海省', label: '青海省' },
  { value: '台湾省', label: '台湾省' },
  { value: '内蒙古自治区', label: '内蒙古自治区' },
  { value: '广西壮族自治区', label: '广西壮族自治区' },
  { value: '西藏自治区', label: '西藏自治区' },
  { value: '宁夏回族自治区', label: '宁夏回族自治区' },
  { value: '新疆维吾尔自治区', label: '新疆维吾尔自治区' },
  { value: '香港特别行政区', label: '香港特别行政区' },
  { value: '澳门特别行政区', label: '澳门特别行政区' }
])

const cityOptions = ref([])
const districtOptions = ref([])

// 城市数据（简化版，实际项目中应该使用完整的省市区数据）
const cityData = {
  '上海市': ['黄浦区', '徐汇区', '长宁区', '静安区', '普陀区', '虹口区', '杨浦区', '闵行区', '宝山区', '嘉定区', '浦东新区', '金山区', '松江区', '青浦区', '奉贤区', '崇明区'],
  '北京市': ['东城区', '西城区', '朝阳区', '丰台区', '石景山区', '海淀区', '门头沟区', '房山区', '通州区', '顺义区', '昌平区', '大兴区', '怀柔区', '平谷区', '密云区', '延庆区'],
  '广东省': ['广州市', '深圳市', '珠海市', '汕头市', '佛山市', '韶关市', '湛江市', '肇庆市', '江门市', '茂名市', '惠州市', '梅州市', '汕尾市', '河源市', '阳江市', '清远市', '东莞市', '中山市', '潮州市', '揭阳市', '云浮市'],
  '江苏省': ['南京市', '无锡市', '徐州市', '常州市', '苏州市', '南通市', '连云港市', '淮安市', '盐城市', '扬州市', '镇江市', '泰州市', '宿迁市'],
  '浙江省': ['杭州市', '宁波市', '温州市', '嘉兴市', '湖州市', '绍兴市', '金华市', '衢州市', '舟山市', '台州市', '丽水市']
}

const districtData = {
  '广州市': ['天河区', '越秀区', '荔湾区', '海珠区', '白云区', '黄埔区', '番禺区', '花都区', '南沙区', '从化区', '增城区'],
  '深圳市': ['罗湖区', '福田区', '南山区', '宝安区', '龙岗区', '盐田区', '龙华区', '坪山区', '光明区', '大鹏新区'],
  '南京市': ['玄武区', '秦淮区', '建邺区', '鼓楼区', '浦口区', '栖霞区', '雨花台区', '江宁区', '六合区', '溧水区', '高淳区'],
  '杭州市': ['上城区', '下城区', '江干区', '拱墅区', '西湖区', '滨江区', '萧山区', '余杭区', '富阳区', '临安区', '桐庐县', '淳安县', '建德市'],
  '苏州市': ['姑苏区', '虎丘区', '吴中区', '相城区', '吴江区', '昆山市', '太仓市', '常熟市', '张家港市']
}

// 省份改变
const handleProvinceChange = (value) => {
  addressForm.value.city = ''
  addressForm.value.district = ''
  cityOptions.value = []
  districtOptions.value = []
  
  if (cityData[value]) {
    // 直辖市：城市即区县
    if (['北京市', '上海市', '天津市', '重庆市'].includes(value)) {
      addressForm.value.city = value
      cityOptions.value = [{ value: value, label: value }]
      districtOptions.value = cityData[value].map(d => ({ value: d, label: d }))
    } else {
      // 省份：显示城市列表
      cityOptions.value = cityData[value].map(c => ({ value: c, label: c }))
    }
  }
}

// 城市改变
const handleCityChange = (value) => {
  addressForm.value.district = ''
  districtOptions.value = []
  
  if (districtData[value]) {
    districtOptions.value = districtData[value].map(d => ({ value: d, label: d }))
  }
}

// 表单验证规则
const addressRules = {
  contactName: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  province: [
    { required: true, message: '请选择省份', trigger: 'change' }
  ],
  city: [
    { required: true, message: '请选择城市', trigger: 'change' }
  ],
  district: [
    { required: true, message: '请选择区县', trigger: 'change' }
  ],
  detail: [
    { required: true, message: '请输入详细地址', trigger: 'blur' }
  ]
}

const handleAdd = () => {
  dialogTitle.value = '新增地址'
  addressForm.value = {
    id: null,
    contactName: '',
    contactPhone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: 0
  }
  cityOptions.value = []
  districtOptions.value = []
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑地址'
  addressForm.value = {
    id: row.id,
    contactName: row.contactName,
    contactPhone: row.phone || row.contactPhone,
    province: row.province,
    city: row.city,
    district: row.district,
    detail: row.detailAddress || row.detail,
    isDefault: row.isDefault
  }
  
  // 初始化级联选择器
  if (row.province) {
    handleProvinceChange(row.province)
    if (row.city) {
      handleCityChange(row.city)
    }
  }
  
  dialogVisible.value = true
}

// 保存地址
const handleSaveAddress = async () => {
  try {
    await addressFormRef.value.validate()
    
    const data = {
      userId: userId,
      contactName: addressForm.value.contactName,
      contactPhone: addressForm.value.contactPhone,
      province: addressForm.value.province,
      city: addressForm.value.city,
      district: addressForm.value.district,
      detail: addressForm.value.detail,
      isDefault: addressForm.value.isDefault
    }
    
    if (addressForm.value.id) {
      // 编辑
      data.id = addressForm.value.id
      await request.put('/user/address', data)
      ElMessage.success('更新成功')
    } else {
      // 新增
      await request.post('/user/address', data)
      ElMessage.success('添加成功')
    }
    
    dialogVisible.value = false
    loadData()
  } catch (error) {
    if (error !== false) {
      console.error('保存失败:', error)
      ElMessage.error('保存失败')
    }
  }
}

const handleSetDefault = async (row) => {
  try {
    await request.put(`/user/address/${row.id}/default?userId=${userId}`)
    ElMessage.success('设置成功')
    loadData()
  } catch (error) {
    console.error('设置失败:', error)
    ElMessage.error('设置失败')
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', {
      type: 'warning'
    })
    await request.delete(`/user/address/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.address-container {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
}
</style>
