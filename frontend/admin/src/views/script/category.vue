<template>
  <div class="category-list">
    <el-card>
      <el-button type="success" @click="handleAdd" style="margin-bottom: 20px">新增分类</el-button>
      
      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else style="color: #ccc;">暂无描述</span>
          </template>
        </el-table-column>
        <el-table-column prop="scriptCount" label="剧本数量" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="dialogTitle" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="3"
            placeholder="请输入分类描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const form = ref({
  id: null,
  name: '',
  description: ''
})

const fetchList = async () => {
  loading.value = true
  try {
    // 并行获取分类和剧本列表
    const [categoryRes, scriptRes] = await Promise.all([
      request.get('/script/category'),
      request.get('/script/page', { params: { page: 1, pageSize: 1000 } })
    ])
    
    const categories = categoryRes.data || []
    const scripts = scriptRes.data?.records || []
    
    // 统计每个分类下的剧本数量
    const categoryCountMap = {}
    scripts.forEach(script => {
      const categoryId = script.categoryId
      if (categoryId) {
        categoryCountMap[categoryId] = (categoryCountMap[categoryId] || 0) + 1
      }
    })
    
    // 合并分类信息和统计数量
    tableData.value = categories.map(item => ({
      id: item.id,
      name: item.name,
      description: item.description || item.desc || '', // 兼容desc字段
      scriptCount: categoryCountMap[item.id] || 0
    }))
    
    console.log('分类数据加载完成:', tableData.value)
  } catch (e) {
    console.error('加载分类失败:', e)
    // 回退到演示数据
    tableData.value = [
      { id: 1, name: '恐怖惊悚', description: '恐怖惊悚类剧本', scriptCount: 5 },
      { id: 2, name: '推理悬疑', description: '推理悬疑类剧本', scriptCount: 8 },
      { id: 3, name: '情感治愈', description: '情感治愈类剧本', scriptCount: 6 },
      { id: 4, name: '欢乐搞笑', description: '欢乐搞笑类剧本', scriptCount: 4 },
      { id: 5, name: '机制硬核', description: '机制硬核类剧本', scriptCount: 3 },
      { id: 6, name: '还原沉浸', description: '还原沉浸类剧本', scriptCount: 7 },
      { id: 7, name: '新手友好', description: '新手友好类剧本', scriptCount: 2 }
    ]
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增分类'
  form.value = {
    id: null,
    name: '',
    description: ''
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑分类'
  form.value = {
    id: row.id,
    name: row.name,
    description: row.description
  }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类"${row.name}"吗？${row.scriptCount > 0 ? '注意：该分类下有 ' + row.scriptCount + ' 个剧本！' : ''}`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    loading.value = true
    await request.delete(`/script/category/${row.id}`)
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

const handleSubmit = async () => {
  if (!form.value.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  
  try {
    loading.value = true
    if (form.value.id) {
      // 编辑
      await request.put(`/script/category/${form.value.id}`, {
        name: form.value.name,
        description: form.value.description
      })
      ElMessage.success('更新成功')
    } else {
      // 新增
      await request.post('/script/category', {
        name: form.value.name,
        description: form.value.description
      })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await fetchList()
  } catch (e) {
    ElMessage.error(form.value.id ? '更新失败' : '添加失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.category-list {
  width: 100%;
}
</style>
