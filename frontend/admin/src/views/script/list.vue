<template>
  <div class="script-list">
    <el-card>
      <el-form :inline="true" :model="queryForm" class="query-form">
        <el-form-item label="剧本名称">
          <el-input v-model="queryForm.name" placeholder="请输入剧本名称" clearable />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryForm.category" placeholder="请选择分类" clearable style="width: 160px">
            <el-option v-for="opt in categoryOptions" :key="opt.id" :label="opt.name" :value="opt.name" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增剧本</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <el-image 
              v-if="row.cover" 
              :src="row.cover" 
              fit="cover" 
              style="width: 60px; height: 80px; border-radius: 4px; cursor: pointer;"
              :preview-src-list="[row.cover]"
            />
            <div v-else style="width: 60px; height: 80px; background: #f5f5f5; display: flex; align-items: center; justify-content: center; border-radius: 4px;">
              <span style="color: #ccc; font-size: 12px;">无封面</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="剧本名称" width="170">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 5px;">
              <span>{{ row.name }}</span>
              <el-tag v-if="row.isExclusive === 1" type="danger" size="small">独家</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="标签" width="180">
          <template #default="{ row }">
            <div v-if="row.tags" style="display: flex; flex-wrap: wrap; gap: 4px;">
              <el-tag v-for="tag in getTagArray(row.tags)" :key="tag" size="small" type="info">{{ tag }}</el-tag>
            </div>
            <span v-else style="color: #ccc; font-size: 12px;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="130">
          <template #default="{ row }">
            <el-rate v-model="row.rating" disabled show-score text-color="#ff9900" :max="5" />
          </template>
        </el-table-column>
        <el-table-column prop="playerCount" label="人数" width="70" />
        <el-table-column prop="duration" label="时长(分)" width="90" />
        <el-table-column label="难度" width="120">
          <template #default="{ row }">
            <el-rate v-model="row.difficulty" disabled :max="5" />
          </template>
        </el-table-column>
        <el-table-column prop="price" label="价格" width="90" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="queryForm.page"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const rawData = ref([])
const tableData = computed(() => {
  let data = rawData.value
  if (queryForm.name) {
    data = data.filter(item => item.name.includes(queryForm.name))
  }
  if (queryForm.category) {
    data = data.filter(item => item.category === queryForm.category)
  }
  return data
})

const route = useRoute()
const router = useRouter()

const categoryOptions = ref([])
const total = ref(0)
const queryForm = reactive({
  name: '',
  category: '',
  page: 1,
  pageSize: 10
})

const handleQuery = async () => {
  queryForm.page = 1 // 查询时重置到第一页
  await fetchList()
}

const handleReset = () => {
  queryForm.name = ''
  queryForm.category = ''
  queryForm.page = 1
  handleQuery()
}

const handleSizeChange = async (newSize) => {
  queryForm.pageSize = newSize
  queryForm.page = 1
  await fetchList()
}

const handlePageChange = async (newPage) => {
  queryForm.page = newPage
  await fetchList()
}

const handleAdd = () => {
  router.push('/script/add')
}

const handleEdit = (row) => {
  // 由于路由中没有单独的编辑页面，这里可以跳转到add页面并传递ID
  // 或者显示一个对话框进行编辑
  router.push({
    path: '/script/add',
    query: { id: row.id }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除剧本"${row.name}"吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    await request.delete(`/script/${row.id}`)
    ElMessage.success('删除成功')
    await fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  } finally {
    loading.value = false
  }
}

const getTagArray = (tags) => {
  if (!tags) return []
  return tags.split(',').map(t => t.trim()).filter(t => t)
}

const fetchList = async () => {
  try {
    const params = { page: queryForm.page, pageSize: queryForm.pageSize, name: queryForm.name }
    const sel = categoryOptions.value.find(c => c.name === queryForm.category)
    if (sel) params.categoryId = sel.id
    const res = await request.get('/script/page', { params })
    total.value = res.data?.total || 0
    const list = res.data?.records || []
    
    // 创建分类ID到名称的映射
    const categoryMap = {}
    categoryOptions.value.forEach(cat => {
      categoryMap[cat.id] = cat.name
    })
    
    rawData.value = list.map(it => ({
      id: it.id,
      name: it.name,
      // 优先使用categoryName，其次使用categoryId映射，最后使用category字段
      category: it.categoryName || categoryMap[it.categoryId] || it.category || '未分类',
      categoryId: it.categoryId, // 保留categoryId用于编辑
      cover: it.cover || '',
      tags: it.tags || '',
      rating: it.rating || 0,
      isExclusive: it.isExclusive || 0,
      playerCount: it.playerCount,
      duration: it.duration,
      difficulty: it.difficulty || 3,
      price: it.price || 0
    }))
  } catch (e) {
    // 回退到演示数据
    rawData.value = [
      { id: 1, name: '午夜铃声', category: '恐怖惊悚', playerCount: 6, duration: 240, difficulty: 3, price: 168 },
      { id: 2, name: '雾都迷案', category: '推理悬疑', playerCount: 7, duration: 300, difficulty: 4, price: 198 },
      { id: 3, name: '岁月回声', category: '情感治愈', playerCount: 6, duration: 180, difficulty: 5, price: 228 },
      { id: 4, name: '周末派对', category: '欢乐搞笑', playerCount: 6, duration: 150, difficulty: 2, price: 128 },
      { id: 5, name: '棋局', category: '机制硬核', playerCount: 6, duration: 210, difficulty: 5, price: 218 },
      { id: 6, name: '浮生录', category: '还原沉浸', playerCount: 7, duration: 240, difficulty: 3, price: 188 },
      { id: 7, name: '初来乍到', category: '新手友好', playerCount: 6, duration: 120, difficulty: 1, price: 99 }
    ]
  }
}

const fetchCategories = async () => {
  try {
    const res = await request.get('/script/category')
    categoryOptions.value = (res.data || []).map(c => ({ id: c.id, name: c.name }))
  } catch (e) {
    categoryOptions.value = [
      '恐怖惊悚','推理悬疑','情感治愈','欢乐搞笑','机制硬核','还原沉浸','新手友好'
    ].map((n, i) => ({ id: i + 1, name: n }))
  }
}

onMounted(async () => {
  // 从首页图表跳转的分类筛选
  if (route.query && route.query.category) {
    queryForm.category = String(route.query.category)
  }

  await Promise.all([fetchCategories(), fetchList()])
})
</script>

<style scoped>
.script-list {
  width: 100%;
}

.query-form {
  margin-bottom: 20px;
}
</style>
