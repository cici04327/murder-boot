<template>
  <div class="user-list-container">
    <el-card>
      <!-- 搜索区域 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="searchForm.memberLevel" placeholder="请选择" clearable>
            <el-option label="普通会员" :value="1" />
            <el-option label="银卡会员" :value="2" />
            <el-option label="金卡会员" :value="3" />
            <el-option label="钻石会员" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮 -->
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
        <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="selectedRows.length === 0">
          批量删除
        </el-button>
        <el-button type="success" :icon="Download" @click="handleExport" :loading="exporting">
          导出Excel
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        v-loading="loading"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="头像" width="80">
          <template #default="{ row }">
            <el-avatar 
              :size="50" 
              :src="row.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
              fit="cover"
            />
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="{ row }">
            <el-tag :type="row.gender === 1 ? 'primary' : 'danger'" size="small">
              {{ row.gender === 1 ? '男' : '女' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memberLevel" label="会员等级" width="100">
          <template #default="{ row }">
            <el-tag :type="getMemberLevelType(row.memberLevel)" size="small">
              {{ getMemberLevelText(row.memberLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100" sortable />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleView(row)">查看</el-button>
            <el-button type="success" link :icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" link :icon="Location" @click="handleAddress(row)">地址</el-button>
            <el-button type="warning" link :icon="Coin" @click="handlePoints(row)">积分</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Delete, Download, View, Edit, Coin, Location } from '@element-plus/icons-vue'
import request from '@/utils/request'
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'

const router = useRouter()

const searchForm = ref({
  username: '',
  phone: '',
  memberLevel: null
})

const tableData = ref([])
const loading = ref(false)
const exporting = ref(false)
const selectedRows = ref([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  total: 0
})

// 加载用户列表
const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/page', { 
      params: {
        page: pagination.value.page,
        pageSize: pagination.value.pageSize,
        username: searchForm.value.username || undefined,
        phone: searchForm.value.phone || undefined,
        memberLevel: searchForm.value.memberLevel || undefined
      }
    })
    
    tableData.value = res.data.records || []
    pagination.value.total = res.data.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载数据失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.value.page = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.value = {
    username: '',
    phone: '',
    memberLevel: null
  }
  handleSearch()
}

// 新增
const handleAdd = () => {
  router.push('/user/add')
}

// 查看详情
const handleView = (row) => {
  router.push(`/user/detail/${row.id}`)
}

// 编辑
const handleEdit = (row) => {
  router.push(`/user/edit/${row.id}`)
}

// 地址管理
const handleAddress = (row) => {
  router.push(`/user/address/${row.id}`)
}

// 积分管理
const handlePoints = (row) => {
  router.push(`/user/points/${row.id}`)
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    await request.delete(`/user/${row.id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的用户')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个用户吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    
    const ids = selectedRows.value.map(row => row.id)
    // 逐个删除
    for (const id of ids) {
      await request.delete(`/user/${id}`)
    }
    
    ElMessage.success('批量删除成功')
    selectedRows.value = []
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败: ' + (error.message || '未知错误'))
    }
  }
}

// 导出Excel
const handleExport = async () => {
  exporting.value = true
  try {
    // 获取所有数据（不分页）
    const res = await request.get('/user/page', {
      params: {
        page: 1,
        pageSize: 10000,
        username: searchForm.value.username || undefined,
        phone: searchForm.value.phone || undefined,
        memberLevel: searchForm.value.memberLevel || undefined
      }
    })

    const data = res.data.records || []
    
    if (data.length === 0) {
      ElMessage.warning('没有数据可导出')
      return
    }

    // 转换数据格式
    const exportData = data.map(item => ({
      'ID': item.id,
      '用户名': item.username,
      '昵称': item.nickname,
      '手机号': item.phone,
      '性别': item.gender === 1 ? '男' : '女',
      '会员等级': getMemberLevelText(item.memberLevel),
      '积分': item.points,
      '状态': item.status === 1 ? '启用' : '禁用',
      '注册时间': item.createTime
    }))

    // 创建工作簿
    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, '用户列表')

    // 设置列宽
    const colWidths = [
      { wch: 8 },  // ID
      { wch: 15 }, // 用户名
      { wch: 15 }, // 昵称
      { wch: 15 }, // 手机号
      { wch: 8 },  // 性别
      { wch: 12 }, // 会员等级
      { wch: 10 }, // 积分
      { wch: 8 },  // 状态
      { wch: 20 }  // 注册时间
    ]
    worksheet['!cols'] = colWidths

    // 生成Excel文件
    const excelBuffer = XLSX.write(workbook, { bookType: 'xlsx', type: 'array' })
    const blob = new Blob([excelBuffer], { type: 'application/octet-stream' })
    
    // 下载文件
    const fileName = `用户列表_${new Date().getTime()}.xlsx`
    saveAs(blob, fileName)
    
    ElMessage.success(`成功导出 ${data.length} 条数据`)
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败: ' + (error.message || '未知错误'))
  } finally {
    exporting.value = false
  }
}

// 状态切换
const handleStatusChange = async (row) => {
  const oldStatus = row.status
  try {
    await request.put(`/user/status/${row.id}`, { status: row.status })
    ElMessage.success('状态更新成功')
    loadData()
  } catch (error) {
    console.error('状态更新失败:', error)
    row.status = oldStatus
    ElMessage.error('状态更新失败: ' + (error.message || '未知错误'))
  }
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 分页
const handleSizeChange = () => {
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

// 会员等级
const getMemberLevelType = (level) => {
  const types = { 1: '', 2: 'success', 3: 'warning', 4: 'danger' }
  return types[level] || ''
}

const getMemberLevelText = (level) => {
  const texts = { 1: '普通', 2: '银卡', 3: '金卡', 4: '钻石' }
  return texts[level] || '未知'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-list-container {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.toolbar {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
