<template>
  <div class="statistics-dashboard">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-cards">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #409eff;">
              <el-icon :size="32"><Shop /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalStores || 0 }}</div>
              <div class="stat-label">门店总数</div>
              <div class="stat-detail">
                <span class="success">营业: {{ statistics.openStores || 0 }}</span>
                <span class="danger">停业: {{ statistics.closedStores || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #67c23a;">
              <el-icon :size="32"><Reading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalRooms || 0 }}</div>
              <div class="stat-label">房间总数</div>
              <div class="stat-detail">
                <span class="success">可用: {{ statistics.availableRooms || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #e6a23c;">
              <el-icon :size="32"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.totalReviews || 0 }}</div>
              <div class="stat-label">评价总数</div>
              <div class="stat-detail">
                <span class="success">好评: {{ statistics.goodReviews || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" style="background: #f56c6c;">
              <el-icon :size="32"><Star /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ statistics.averageRating || 0 }}</div>
              <div class="stat-label">平均评分</div>
              <div class="stat-detail">
                <el-rate 
                  v-model="ratingValue" 
                  disabled 
                  show-score 
                  text-color="#ff9900"
                  score-template="{value}"
                />
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 门店列表 -->
    <el-card class="store-list-card" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>门店详情</span>
          <el-button type="primary" @click="refreshData">刷新数据</el-button>
        </div>
      </template>

      <el-table :data="storeList" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="门店名称" width="200" />
        <el-table-column prop="address" label="地址" min-width="250" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="营业时间" width="150">
          <template #default="{ row }">
            {{ row.openTime }} - {{ row.closeTime }}
          </template>
        </el-table-column>
        <el-table-column prop="rating" label="评分" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getRatingType(row.rating)">
              {{ row.rating || 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '营业中' : '已停业' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="viewDetail(row.id)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 门店详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="门店详细信息"
      width="900px"
    >
      <div v-loading="detailLoading" class="store-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="门店名称">{{ storeDetail.name }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ storeDetail.phone }}</el-descriptions-item>
          <el-descriptions-item label="门店地址" :span="2">{{ storeDetail.address }}</el-descriptions-item>
          <el-descriptions-item label="营业时间">
            {{ storeDetail.openTime }} - {{ storeDetail.closeTime }}
          </el-descriptions-item>
          <el-descriptions-item label="评分">
            <el-rate v-model="detailRating" disabled show-score />
          </el-descriptions-item>
          <el-descriptions-item label="评价数量">{{ storeDetail.reviewCount || 0 }} 条</el-descriptions-item>
          <el-descriptions-item label="门店简介" :span="2">
            {{ storeDetail.description || '暂无简介' }}
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">房间列表</el-divider>
        <el-table :data="storeDetail.rooms" style="width: 100%">
          <el-table-column prop="name" label="房间名称" width="150" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              {{ getRoomTypeText(row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="capacity" label="容纳人数" width="100" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '可用' : '不可用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Shop, Reading, User, Star } from '@element-plus/icons-vue'
import request from '@/utils/request'

const loading = ref(false)
const detailLoading = ref(false)
const detailDialogVisible = ref(false)
const storeList = ref([])
const storeDetail = ref({})

const statistics = reactive({
  totalStores: 0,
  openStores: 0,
  closedStores: 0,
  totalRooms: 0,
  availableRooms: 0,
  averageRating: 0,
  totalReviews: 0,
  goodReviews: 0
})

const ratingValue = computed(() => {
  return parseFloat(statistics.averageRating) || 0
})

const detailRating = computed(() => {
  return parseFloat(storeDetail.value.rating) || 0
})

const fetchStatistics = async () => {
  try {
    const res = await request.get('/store/statistics')
    Object.assign(statistics, res.data)
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  }
}

const fetchStoreList = async () => {
  loading.value = true
  try {
    const res = await request.get('/store/list')
    storeList.value = res.data
  } catch (error) {
    console.error('获取门店列表失败:', error)
    ElMessage.error('获取门店列表失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = async (id) => {
  detailDialogVisible.value = true
  detailLoading.value = true
  try {
    const res = await request.get(`/store/detail/${id}`)
    storeDetail.value = res.data
  } catch (error) {
    console.error('获取门店详情失败:', error)
    ElMessage.error('获取门店详情失败')
  } finally {
    detailLoading.value = false
  }
}

const getRatingType = (rating) => {
  if (rating >= 4.5) return 'success'
  if (rating >= 4.0) return 'warning'
  return 'danger'
}

const getRoomTypeText = (type) => {
  const map = { 1: '小房', 2: '中房', 3: '大房' }
  return map[type] || '未知'
}

const refreshData = () => {
  fetchStatistics()
  fetchStoreList()
  ElMessage.success('数据已刷新')
}

onMounted(() => {
  fetchStatistics()
  fetchStoreList()
})
</script>

<style scoped>
.statistics-dashboard {
  width: 100%;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
  line-height: 1;
  margin-bottom: 8px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.stat-detail {
  font-size: 12px;
  color: #606266;
}

.stat-detail .success {
  color: #67c23a;
  margin-right: 12px;
}

.stat-detail .danger {
  color: #f56c6c;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.store-detail {
  padding: 12px;
}
</style>
