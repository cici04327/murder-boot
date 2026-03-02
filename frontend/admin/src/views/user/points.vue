<template>
  <div class="points-container">
    <el-page-header @back="goBack" title="返回" content="积分管理" />
    
    <el-card style="margin-top: 20px;">
      <el-descriptions title="用户信息" :column="2" border>
        <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
        <el-descriptions-item label="当前积分">
          <el-tag type="warning" size="large">{{ user.points }} 分</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="会员等级">
          <el-tag :type="getMemberLevelType(user.memberLevel)">
            {{ getMemberLevelText(user.memberLevel) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>积分记录</span>
      </template>
      
      <el-table :data="records" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ row.type === 1 ? '增加' : '减少' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.type === 1 ? '#67C23A' : '#F56C6C', fontWeight: 'bold' }">
              {{ row.type === 1 ? '+' : '-' }}{{ row.points }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="120">
          <template #default="{ row }">
            {{ getSourceText(row.source) }}
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="createTime" label="时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userId = route.params.id

const user = ref({})
const records = ref([])
const loading = ref(false)

// 加载用户信息
const loadUser = async () => {
  try {
    const res = await request.get(`/user/${userId}`)
    user.value = res.data
  } catch (error) {
    console.error('加载用户信息失败:', error)
    // 使用模拟数据
    user.value = {
      username: 'test_user',
      nickname: '张三',
      points: 150,
      memberLevel: 1
    }
  }
}

// 加载积分记录
const loadRecords = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/points/page', {
      params: { userId: userId, page: 1, pageSize: 100 }
    })
    records.value = res.data?.records || []
  } catch (error) {
    console.error('加载积分记录失败:', error)
    // 使用模拟数据
    records.value = [
      { id: 1, type: 1, points: 100, source: 1, description: '注册赠送积分', createTime: '2024-01-01 10:00:00' },
      { id: 2, type: 1, points: 50, source: 3, description: '每日签到', createTime: '2024-01-02 09:00:00' },
      { id: 3, type: 2, points: 20, source: 2, description: '消费抵扣', createTime: '2024-01-03 15:00:00' },
      { id: 4, type: 1, points: 30, source: 4, description: '活动奖励', createTime: '2024-01-04 12:00:00' }
    ]
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.back()
}

const getMemberLevelType = (level) => {
  const types = { 1: '', 2: 'success', 3: 'warning', 4: 'danger' }
  return types[level] || ''
}

const getMemberLevelText = (level) => {
  const texts = { 1: '普通会员', 2: '银卡会员', 3: '金卡会员', 4: '钻石会员' }
  return texts[level] || '未知'
}

const getSourceText = (source) => {
  const texts = { 1: '注册赠送', 2: '消费获得', 3: '签到奖励', 4: '活动奖励' }
  return texts[source] || '未知'
}

onMounted(() => {
  loadUser()
  loadRecords()
})
</script>

<style scoped>
.points-container {
  padding: 20px;
}
</style>
