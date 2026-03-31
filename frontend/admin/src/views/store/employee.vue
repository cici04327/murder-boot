<template>
  <div class="employee-manage">
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item v-if="!isStoreScoped" label="门店">
          <el-select v-model="searchForm.storeId" placeholder="全部门店" clearable style="width: 180px" @change="fetchData">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="员工姓名">
          <el-input v-model="searchForm.keyword" placeholder="请输入员工姓名" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="openDialog()">新增员工</el-button>
          <el-button plain @click="openLogDialog()">操作日志</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="tableData" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="name" label="员工姓名" min-width="120" />
        <el-table-column prop="storeName" label="所属门店" min-width="140" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="职位" width="110">
          <template #default="{ row }">
            {{ row.positionName || getPositionLabel(row.position) }}
          </template>
        </el-table-column>
        <el-table-column label="后台角色" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="getStaffRoleTag(row.staffRole)">
              {{ getStaffRoleLabel(row.staffRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="loginAccount" label="登录账号" min-width="140" />
        <el-table-column label="权限" min-width="200">
          <template #default="{ row }">
            <el-tag
              v-for="code in splitPermissionCodes(row.permissionCodes)"
              :key="code"
              size="small"
              style="margin: 2px"
            >
              {{ getPermissionLabel(code) }}
            </el-tag>
            <span v-if="!splitPermissionCodes(row.permissionCodes).length" class="text-gray">未配置</span>
          </template>
        </el-table-column>
        <el-table-column label="绑定DM" width="120">
          <template #default="{ row }">
            {{ row.dmName || (row.dmId ? `#${row.dmId}` : '-') }}
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="170">
          <template #default="{ row }">
            {{ row.lastLoginTime || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="340" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button size="small" @click="openResetPasswordDialog(row)">重置密码</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑员工' : '新增员工'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item v-if="!isStoreScoped" label="所属门店" prop="storeId">
          <el-select v-model="form.storeId" placeholder="请选择门店" style="width:100%">
            <el-option v-for="store in storeOptions" :key="store.id" :label="store.name" :value="store.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="员工姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入员工姓名" maxlength="30" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" maxlength="20" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-select v-model="form.position" placeholder="请选择职位" style="width:100%">
            <el-option label="店长" :value="1" />
            <el-option label="副店长" :value="2" />
            <el-option label="主持人" :value="3" />
            <el-option label="服务员" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="后台角色" prop="staffRole">
          <el-select v-model="form.staffRole" placeholder="请选择后台角色" style="width:100%" @change="handleStaffRoleChange">
            <el-option label="店长" value="MANAGER" />
            <el-option label="店员" value="CLERK" />
            <el-option label="DM" value="DM" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录账号" prop="loginAccount">
          <el-input v-model="form.loginAccount" placeholder="不填则默认取手机号" maxlength="64" />
        </el-form-item>
        <el-form-item label="登录密码" prop="loginPassword">
          <el-input v-model="form.loginPassword" type="password" show-password :placeholder="editingId ? '留空则不修改密码' : '不填默认123456'" />
        </el-form-item>
        <el-form-item label="权限" prop="permissionCodes">
          <div class="permission-tip">勾选后保存即生效；下次重新登录对应账号后按当前勾选权限生效。不同岗位仅能勾选其权限上限内的项目。</div>
          <el-checkbox-group v-model="selectedPermissionCodes">
            <el-checkbox
              v-for="item in permissionOptions"
              :key="item.code"
              :label="item.code"
              :disabled="!isPermissionAllowed(item.code)"
            >
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-if="form.staffRole === 'DM'" label="绑定DM">
          <el-select v-model="form.dmId" placeholder="请选择对应DM" style="width:100%" clearable filterable>
            <el-option v-for="dm in dmOptions" :key="dm.id" :label="dm.name" :value="dm.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPasswordVisible" title="重置员工密码" width="420px">
      <el-form label-width="100px">
        <el-form-item label="员工姓名">
          <el-input :model-value="currentRow.name || '-'" disabled />
        </el-form-item>
        <el-form-item label="登录账号">
          <el-input :model-value="currentRow.loginAccount || '-'" disabled />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            show-password
            maxlength="32"
            placeholder="请输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordVisible = false">取消</el-button>
        <el-button type="primary" :loading="resettingPassword" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logDialogVisible" title="员工操作日志" width="920px">
      <div class="log-toolbar">
        <el-select v-model="logQuery.actionType" clearable placeholder="全部动作" style="width: 180px">
          <el-option label="员工新增" value="EMPLOYEE_CREATE" />
          <el-option label="员工更新" value="EMPLOYEE_UPDATE" />
          <el-option label="账号启停" value="EMPLOYEE_STATUS" />
          <el-option label="重置密码" value="EMPLOYEE_RESET_PASSWORD" />
          <el-option label="预约核销" value="RESERVATION_CHECKIN" />
          <el-option label="完成预约" value="RESERVATION_COMPLETE" />
          <el-option label="分配DM" value="RESERVATION_ASSIGN_DM" />
          <el-option label="同意退款" value="REFUND_APPROVE" />
          <el-option label="拒绝退款" value="REFUND_REJECT" />
        </el-select>
        <el-button type="primary" @click="loadLogData">查询</el-button>
      </div>
      <el-table :data="logTableData" v-loading="logLoading" border stripe height="420">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="actionType" label="动作" width="160">
          <template #default="{ row }">
            {{ getLogActionLabel(row.actionType) }}
          </template>
        </el-table-column>
        <el-table-column prop="targetName" label="目标" width="180" />
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="logPagination.page"
        v-model:page-size="logPagination.pageSize"
        :total="logPagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @current-change="loadLogData"
        @size-change="loadLogData"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { getDmList } from '@/api/dm'
import {
  addStoreEmployee,
  deleteStoreEmployee,
  getStoreEmployeeLogPage,
  getStoreEmployeePage,
  resetStoreEmployeePassword,
  updateStoreEmployee,
  updateStoreEmployeeStatus
} from '@/api/store-employee'

const loginType = localStorage.getItem('admin-login-type') || 'admin'
const adminUser = JSON.parse(localStorage.getItem('admin-user') || '{}')
const isStoreScoped = loginType === 'store' || loginType === 'staff'
const fixedStoreId = Number(localStorage.getItem('admin-store-id') || 0) || null

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const tableData = ref([])
const storeOptions = ref([])
const dmOptions = ref([])
const selectedPermissionCodes = ref([])
const resetPasswordVisible = ref(false)
const resettingPassword = ref(false)
const currentRow = ref({})
const logDialogVisible = ref(false)
const logLoading = ref(false)
const logTableData = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const logPagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const searchForm = reactive({
  storeId: fixedStoreId,
  keyword: '',
  status: null
})

const logQuery = reactive({
  actionType: ''
})

const form = reactive({
  id: null,
  storeId: fixedStoreId,
  name: '',
  phone: '',
  position: 4,
  staffRole: 'CLERK',
  loginAccount: '',
  loginPassword: '',
  permissionCodes: '',
  dmId: null,
  status: 1
})

const resetPasswordForm = reactive({
  newPassword: '123456'
})

const rules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  name: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  position: [{ required: true, message: '请选择职位', trigger: 'change' }],
  staffRole: [{ required: true, message: '请选择后台角色', trigger: 'change' }]
}

const rolePermissionLimitMap = {
  MANAGER: ['reservation:view', 'reservation:checkin', 'reservation:complete', 'reservation:assign_dm', 'refund:process', 'employee:manage', 'report:view', 'notification:view'],
  CLERK: ['reservation:view', 'reservation:checkin', 'notification:view'],
  DM: ['reservation:view', 'reservation:complete', 'notification:view']
}

const defaultPermissionMap = {
  MANAGER: [...rolePermissionLimitMap.MANAGER],
  CLERK: [...rolePermissionLimitMap.CLERK],
  DM: [...rolePermissionLimitMap.DM]
}

const permissionOptions = [
  { code: 'reservation:view', label: '查看预约' },
  { code: 'reservation:checkin', label: '预约核销' },
  { code: 'reservation:complete', label: '完成预约' },
  { code: 'reservation:assign_dm', label: '分配DM' },
  { code: 'refund:process', label: '处理退款' },
  { code: 'employee:manage', label: '员工管理' },
  { code: 'report:view', label: '经营数据查看' },
  { code: 'notification:view', label: '通知中心' }
]

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getStoreEmployeePage({
      storeId: isStoreScoped ? fixedStoreId : searchForm.storeId,
      page: pagination.page,
      pageSize: pagination.pageSize
    })
    let records = res.data?.records || []
    if (searchForm.keyword) {
      const keyword = searchForm.keyword.trim()
      records = records.filter(item => item.name?.includes(keyword) || item.phone?.includes(keyword) || item.loginAccount?.includes(keyword))
    }
    if (searchForm.status !== null && searchForm.status !== undefined) {
      records = records.filter(item => item.status === searchForm.status)
    }
    tableData.value = records
    pagination.total = res.data?.total || records.length
  } catch (error) {
    ElMessage.error(error.message || '加载员工列表失败')
  } finally {
    loading.value = false
  }
}

const fetchStores = async () => {
  if (isStoreScoped) {
    return
  }
  try {
    const res = await request.get('/store/list')
    storeOptions.value = res.data || []
  } catch (e) {
    storeOptions.value = []
  }
}

const fetchDmOptions = async (storeId) => {
  if (!storeId) {
    dmOptions.value = []
    return
  }
  try {
    const res = await getDmList(storeId)
    dmOptions.value = res.data || []
  } catch (error) {
    dmOptions.value = []
  }
}

const resetSearch = () => {
  searchForm.storeId = fixedStoreId
  searchForm.keyword = ''
  searchForm.status = null
  pagination.page = 1
  fetchData()
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    storeId: fixedStoreId,
    name: '',
    phone: '',
    position: 4,
    staffRole: 'CLERK',
    loginAccount: '',
    loginPassword: '',
    permissionCodes: '',
    dmId: null,
    status: 1
  })
  selectedPermissionCodes.value = [...defaultPermissionMap.CLERK]
  dmOptions.value = []
}

const openResetPasswordDialog = (row) => {
  currentRow.value = row || {}
  resetPasswordForm.newPassword = '123456'
  resetPasswordVisible.value = true
}

const handleResetPassword = async () => {
  if (!currentRow.value?.id) return
  if (!resetPasswordForm.newPassword || resetPasswordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  resettingPassword.value = true
  try {
    await resetStoreEmployeePassword(currentRow.value.id, resetPasswordForm.newPassword)
    ElMessage.success('密码重置成功')
    resetPasswordVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '密码重置失败')
  } finally {
    resettingPassword.value = false
  }
}

const openLogDialog = () => {
  logDialogVisible.value = true
  logPagination.page = 1
  loadLogData()
}

const loadLogData = async () => {
  logLoading.value = true
  try {
    const res = await getStoreEmployeeLogPage({
      storeId: isStoreScoped ? fixedStoreId : searchForm.storeId,
      actionType: logQuery.actionType,
      page: logPagination.page,
      pageSize: logPagination.pageSize
    })
    logTableData.value = res.data?.records || []
    logPagination.total = res.data?.total || 0
  } catch (error) {
    ElMessage.error(error.message || '加载操作日志失败')
  } finally {
    logLoading.value = false
  }
}

const openDialog = async (row = null) => {
  resetForm()
  editingId.value = row?.id || null
  if (row) {
    Object.assign(form, {
      id: row.id,
      storeId: row.storeId,
      name: row.name,
      phone: row.phone,
      position: row.position,
      staffRole: row.staffRole || inferStaffRole(row.position),
      loginAccount: row.loginAccount || '',
      loginPassword: '',
      permissionCodes: row.permissionCodes || '',
      dmId: row.dmId || null,
      status: row.status
    })
    selectedPermissionCodes.value = normalizePermissionCodesByRole(form.staffRole, splitPermissionCodes(row.permissionCodes))
  }
  await fetchDmOptions(form.storeId)
  dialogVisible.value = true
}

const normalizePermissionCodesByRole = (role, permissionCodes = []) => {
  const allowedCodes = new Set(rolePermissionLimitMap[role] || [])
  return Array.from(new Set(permissionCodes.filter(code => allowedCodes.has(code))))
}

const isPermissionAllowed = (permissionCode) => {
  return (rolePermissionLimitMap[form.staffRole] || []).includes(permissionCode)
}

const handleStaffRoleChange = async (role) => {
  const normalizedCodes = normalizePermissionCodesByRole(role, selectedPermissionCodes.value)
  selectedPermissionCodes.value = normalizedCodes.length > 0 ? normalizedCodes : [...(defaultPermissionMap[role] || [])]
  if (role !== 'DM') {
    form.dmId = null
  } else {
    await fetchDmOptions(form.storeId)
  }
}

const handleSubmit = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    selectedPermissionCodes.value = normalizePermissionCodesByRole(form.staffRole, selectedPermissionCodes.value)
    form.permissionCodes = selectedPermissionCodes.value.join(',')
    const payload = {
      ...form,
      storeId: isStoreScoped ? fixedStoreId : form.storeId
    }
    if (!payload.loginPassword) {
      delete payload.loginPassword
    }
    if (editingId.value) {
      await updateStoreEmployee(payload)
    } else {
      await addStoreEmployee(payload)
    }
    ElMessage.success(`${editingId.value ? '员工更新成功' : '员工新增成功'}，如修改了权限，请让对应账号退出后重新登录生效`)
    dialogVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error(error.message || '保存员工失败')
  } finally {
    submitting.value = false
  }
}

const toggleStatus = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(`确认将员工【${row.name}】设为${nextStatus === 1 ? '在职' : '离职'}？`, '提示', { type: 'warning' })
    await updateStoreEmployeeStatus(row.id, nextStatus)
    ElMessage.success('状态更新成功')
    fetchData()
  } catch (error) {
    // ignore cancel
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除员工【${row.name}】？`, '提示', { type: 'warning' })
    await deleteStoreEmployee(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch (error) {
    // ignore cancel
  }
}

const inferStaffRole = (position) => {
  if (position === 1 || position === 2) return 'MANAGER'
  if (position === 3) return 'DM'
  return 'CLERK'
}

const splitPermissionCodes = (permissionCodes) =>
  String(permissionCodes || '')
    .split(',')
    .map(item => item.trim())
    .filter(Boolean)

const getPermissionLabel = (code) => {
  const map = {
    'reservation:view': '查看预约',
    'reservation:checkin': '预约核销',
    'reservation:complete': '完成预约',
    'reservation:assign_dm': '分配DM',
    'refund:process': '处理退款',
    'employee:manage': '员工管理',
    'report:view': '经营数据查看',
    'notification:view': '通知中心'
  }
  return map[code] || code
}

const getPositionLabel = (position) => {
  const map = { 1: '店长', 2: '副店长', 3: '主持人', 4: '服务员' }
  return map[position] || '未知'
}

const getStaffRoleLabel = (staffRole) => {
  const map = { MANAGER: '店长', CLERK: '店员', DM: 'DM' }
  return map[staffRole] || staffRole || '未设置'
}

const getStaffRoleTag = (staffRole) => {
  const map = { MANAGER: 'warning', CLERK: 'info', DM: 'success' }
  return map[staffRole] || 'info'
}

const getLogActionLabel = (actionType) => {
  const map = {
    EMPLOYEE_CREATE: '员工新增',
    EMPLOYEE_UPDATE: '员工更新',
    EMPLOYEE_STATUS: '账号启停',
    EMPLOYEE_RESET_PASSWORD: '重置密码',
    RESERVATION_CHECKIN: '预约核销',
    RESERVATION_COMPLETE: '完成预约',
    RESERVATION_ASSIGN_DM: '分配DM',
    REFUND_APPROVE: '同意退款',
    REFUND_REJECT: '拒绝退款'
  }
  return map[actionType] || actionType || '-'
}

onMounted(async () => {
  await fetchStores()
  await fetchDmOptions(isStoreScoped ? fixedStoreId : null)
  await fetchData()
})
</script>

<style scoped>
.employee-manage {
  padding: 16px;
}

.search-card {
  margin-bottom: 0;
}

.text-gray {
  color: #909399;
  font-size: 12px;
}

.permission-tip {
  margin-bottom: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 1.5;
}

.log-toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
</style>
