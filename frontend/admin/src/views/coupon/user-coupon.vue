<template>
  <div class="user-coupon">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="用户ID">
          <el-input v-model="searchForm.userId" placeholder="请输入用户ID" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="未使用" :value="1" />
            <el-option label="已使用" :value="2" />
            <el-option label="已过期" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="couponName" label="优惠券名称" min-width="150" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.type)">{{ row.typeName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="150">
          <template #default="{ row }">
            <span v-if="row.type === 1">满{{ row.minAmount }}减{{ row.discountValue }}</span>
            <span v-else-if="row.type === 2">{{ (row.discountValue * 10).toFixed(1) }}折</span>
            <span v-else>代金券{{ row.discountValue }}元</span>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单编号" min-width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.statusName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="receiveTime" label="领取时间" width="180" />
        <el-table-column prop="useTime" label="使用时间" width="180" />
        <el-table-column prop="expireTime" label="过期时间" width="180" />
      </el-table>

      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const searchForm = reactive({
  userId: '',
  status: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const tableData = ref([])

// 移除自动加载，用户需要输入ID后点击查询按钮
// onMounted(() => {
//   loadData()
// })

const loadData = async () => {
  if (!searchForm.userId) {
    ElMessage.warning('请输入用户ID')
    return
  }

  try {
    const { data } = await request.get(`/coupon/user/${searchForm.userId}`, {
      params: {
        page: pagination.page,
        pageSize: pagination.pageSize,
        status: searchForm.status
      }
    })
    tableData.value = data.records
    pagination.total = data.total
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    userId: '',
    status: null
  })
  tableData.value = []
  pagination.total = 0
}

const getTypeTagType = (type) => {
  const types = { 1: 'success', 2: 'warning', 3: 'danger' }
  return types[type] || ''
}

const getStatusTagType = (status) => {
  const types = { 1: 'success', 2: 'info', 3: 'danger' }
  return types[status] || ''
}
</script>

<style scoped>
.user-coupon {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.el-pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
