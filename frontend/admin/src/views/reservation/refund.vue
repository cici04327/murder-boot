<template>
  <div class="refund-management">
    <el-card>
      <template #header>
        <div class="header">
          <h2>退款管理</h2>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待处理" name="1" />
        <el-tab-pane label="已通过" name="2" />
        <el-tab-pane label="已拒绝" name="3" />
        <el-tab-pane label="全部" name="all" />
      </el-tabs>

      <el-table :data="refundList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="订单ID" width="80" />
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column label="用户信息" width="150">
          <template #default="{ row }">
            <div>{{ row.contactName }}</div>
            <div style="color: #999; font-size: 12px;">{{ row.contactPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="scriptName" label="剧本" width="150" />
        <el-table-column prop="storeName" label="门店" width="150" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <div>总价: ¥{{ row.totalPrice }}</div>
            <div style="color: #f56c6c; font-weight: bold;">
              实付: ¥{{ row.actualAmount }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="退款信息" width="200">
          <template #default="{ row }">
            <div style="margin-bottom: 5px;">
              <el-tag :type="getRefundStatusType(row.refundStatus)" size="small">
                {{ getRefundStatusText(row.refundStatus) }}
              </el-tag>
            </div>
            <div style="font-size: 12px; color: #666;">
              申请时间: {{ formatTime(row.refundApplyTime) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="refundReason" label="退款原因" width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.refundStatus === 1"
              type="success"
              size="small"
              @click="handleProcess(row, 1)"
            >
              同意
            </el-button>
            <el-button
              v-if="row.refundStatus === 1"
              type="danger"
              size="small"
              @click="handleProcess(row, 0)"
            >
              拒绝
            </el-button>
            <el-button
              v-if="row.refundStatus !== 1"
              size="small"
              @click="handleViewDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 处理退款对话框 -->
    <el-dialog v-model="showProcessDialog" :title="processTitle" width="500px">
      <el-form :model="processForm" label-width="100px">
        <el-form-item label="订单编号">
          <el-input v-model="currentItem.orderNo" disabled />
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input v-model="currentItem.actualAmount" disabled>
            <template #suffix>元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="退款原因">
          <el-input
            v-model="currentItem.refundReason"
            type="textarea"
            :rows="3"
            disabled
          />
        </el-form-item>
        <el-form-item label="管理员备注">
          <el-input
            v-model="processForm.adminRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showProcessDialog = false">取消</el-button>
        <el-button
          :type="processForm.approved === 1 ? 'success' : 'danger'"
          @click="confirmProcess"
          :loading="processing"
        >
          {{ processForm.approved === 1 ? '确认同意' : '确认拒绝' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog v-model="showDetailDialog" title="退款详情" width="600px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号" :span="2">
          {{ currentItem.orderNo }}
        </el-descriptions-item>
        <el-descriptions-item label="退款状态">
          <el-tag :type="getRefundStatusType(currentItem.refundStatus)">
            {{ getRefundStatusText(currentItem.refundStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="退款金额">
          <span style="color: #f56c6c; font-weight: bold;">
            ¥{{ currentItem.actualAmount }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间" :span="2">
          {{ formatTime(currentItem.refundApplyTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="处理时间" :span="2">
          {{ formatTime(currentItem.refundProcessTime) || '未处理' }}
        </el-descriptions-item>
        <el-descriptions-item label="退款原因" :span="2">
          {{ currentItem.refundReason }}
        </el-descriptions-item>
        <el-descriptions-item label="管理员备注" :span="2">
          {{ currentItem.adminRemark || '无' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reservationService } from '@/utils/request'

const loading = ref(false)
const processing = ref(false)
const activeTab = ref('1')
const refundList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const showProcessDialog = ref(false)
const showDetailDialog = ref(false)
const currentItem = ref({})
const processTitle = ref('')

const processForm = reactive({
  approved: 1,
  adminRemark: ''
})

// 获取退款状态类型
const getRefundStatusType = (status) => {
  const types = {
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return types[status] || 'info'
}

// 获取退款状态文本
const getRefundStatusText = (status) => {
  const texts = {
    1: '待处理',
    2: '已通过',
    3: '已拒绝'
  }
  return texts[status] || '未知'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

// 加载退款列表
const loadRefundList = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value
    }
    
    if (activeTab.value !== 'all') {
      params.refundStatus = parseInt(activeTab.value)
    } else {
      params.hasRefund = true // 只查询有退款申请的订单
    }

    const res = await reservationService.get('/reservation/page', { params })
    
    console.log('退款列表响应:', res)
    console.log('退款列表数据:', res.data.records)
    
    if (res.code === 1 || res.code === 200) {
      refundList.value = res.data.records || res.data.list || []
      total.value = res.data.total || 0
      
      // 调试：打印每条记录的refundStatus
      if (refundList.value.length > 0) {
        console.log('第一条记录的退款状态:', refundList.value[0].refundStatus)
        console.log('第一条完整数据:', refundList.value[0])
      }
    }
  } catch (error) {
    console.error('加载退款列表失败:', error)
    ElMessage.error('加载退款列表失败')
  } finally {
    loading.value = false
  }
}

// 切换标签
const handleTabChange = () => {
  page.value = 1
  loadRefundList()
}

// 分页
const handlePageChange = () => {
  loadRefundList()
}

const handleSizeChange = () => {
  page.value = 1
  loadRefundList()
}

// 处理退款
const handleProcess = (row, approved) => {
  currentItem.value = row
  processForm.approved = approved
  processForm.adminRemark = ''
  processTitle.value = approved === 1 ? '同意退款' : '拒绝退款'
  showProcessDialog.value = true
}

// 确认处理
const confirmProcess = async () => {
  const action = processForm.approved === 1 ? '同意' : '拒绝'
  
  try {
    await ElMessageBox.confirm(
      `确认${action}该退款申请吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    processing.value = true

    const res = await reservationService.post('/reservation/payment/refund/process', null, {
      params: {
        reservationId: currentItem.value.id,
        approved: processForm.approved,
        adminRemark: processForm.adminRemark
      }
    })

    if (res.code === 1 || res.code === 200) {
      ElMessage.success(`${action}退款成功`)
      showProcessDialog.value = false
      loadRefundList()
    } else {
      ElMessage.error(res.msg || `${action}退款失败`)
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('处理退款失败:', error)
      ElMessage.error('操作失败，请稍后重试')
    }
  } finally {
    processing.value = false
  }
}

// 查看详情
const handleViewDetail = (row) => {
  currentItem.value = row
  showDetailDialog.value = true
}

onMounted(() => {
  loadRefundList()
})
</script>

<style scoped>
.refund-management {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header h2 {
  margin: 0;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
