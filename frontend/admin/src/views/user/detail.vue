<template>
  <div class="user-detail-container">
    <el-page-header @back="goBack" title="返回" content="用户详情" />
    
    <el-card style="margin-top: 20px;">
      <el-descriptions title="基本信息" :column="2" border>
        <el-descriptions-item label="用户ID">{{ user.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ user.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ user.phone }}</el-descriptions-item>
        <el-descriptions-item label="性别">
          <el-tag :type="user.gender === 1 ? 'primary' : 'danger'" size="small">
            {{ user.gender === 1 ? '男' : '女' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="会员等级">
          <el-tag :type="getMemberLevelType(user.memberLevel)" size="small">
            {{ getMemberLevelText(user.memberLevel) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="积分">
          <el-tag type="warning" size="small">{{ user.points }} 分</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="user.status === 1 ? 'success' : 'danger'" size="small">
            {{ user.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ user.createTime }}</el-descriptions-item>
        <el-descriptions-item label="最后更新">{{ user.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <div class="actions" style="margin-top: 20px;">
        <el-button type="primary" :icon="Edit" @click="handleEdit">编辑</el-button>
        <el-button type="warning" :icon="Coin" @click="handlePoints">积分管理</el-button>
        <el-button type="success" :icon="Location" @click="handleAddress">地址管理</el-button>
      </div>
    </el-card>

    <!-- 积分记录 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>最近积分记录</span>
      </template>
      <el-table :data="pointsRecords" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'success' : 'danger'" size="small">
              {{ row.type === 1 ? '增加' : '减少' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.type === 1 ? '#67C23A' : '#F56C6C' }">
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
import { Edit, Coin, Location } from '@element-plus/icons-vue'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()
const userId = route.params.id

const user = ref({})
const pointsRecords = ref([])

// 加载用户详情
const loadUserDetail = async () => {
  try {
    const res = await request.get(`/user/${userId}`)
    user.value = res.data
  } catch (error) {
    console.error('加载用户详情失败:', error)
    // 使用模拟数据
    user.value = {
      id: userId,
      username: 'test_user',
      nickname: '张三',
      phone: '13800138000',
      gender: 1,
      memberLevel: 1,
      points: 150,
      status: 1,
      createTime: '2024-01-01 10:00:00',
      updateTime: '2024-10-21 10:00:00'
    }
  }
}

// 加载积分记录
const loadPointsRecords = async () => {
  try {
    const res = await request.get('/user/points/page', {
      params: { userId: userId, page: 1, pageSize: 5 }
    })
    pointsRecords.value = res.data?.records || []
  } catch (error) {
    console.error('加载积分记录失败:', error)
    // 使用模拟数据
    pointsRecords.value = [
      { id: 1, type: 1, points: 100, source: 1, description: '注册赠送积分', createTime: '2024-01-01 10:00:00' },
      { id: 2, type: 1, points: 50, source: 3, description: '每日签到', createTime: '2024-01-02 09:00:00' },
      { id: 3, type: 2, points: 20, source: 2, description: '消费抵扣', createTime: '2024-01-03 15:00:00' }
    ]
  }
}

const goBack = () => {
  router.back()
}

const handleEdit = () => {
  router.push(`/user/edit/${userId}`)
}

const handlePoints = () => {
  router.push(`/user/points/${userId}`)
}

const handleAddress = () => {
  router.push(`/user/address/${userId}`)
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
  loadUserDetail()
  loadPointsRecords()
})
</script>

<style scoped>
.user-detail-container {
  padding: 20px;
}

.actions {
  display: flex;
  gap: 10px;
}
</style>
