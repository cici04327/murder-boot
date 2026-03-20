<template>
  <div class="search-history" v-if="showHistory || showHotSearch">
    <!-- 搜索历史 -->
    <div class="history-section" v-if="showHistory && searchHistory.length > 0">
      <div class="section-header">
        <div class="section-title">
          <el-icon><Clock /></el-icon>
          <span>历史搜索</span>
        </div>
        <el-button text @click="handleClearHistory">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
      </div>
      <div class="history-tags">
        <el-tag
          v-for="(item, index) in searchHistory"
          :key="index"
          closable
          @click="handleHistoryClick(item)"
          @close="handleRemoveHistory(item)"
          class="history-tag"
        >
          {{ item }}
        </el-tag>
      </div>
    </div>
    
    <!-- 热门搜索 -->
    <div class="hot-search-section" v-if="showHotSearch">
      <div class="section-header">
        <div class="section-title">
          <el-icon><TrendCharts /></el-icon>
          <span>热门搜索</span>
        </div>
      </div>
      <div class="hot-search-list">
        <div
          v-for="(item, index) in hotSearches"
          :key="index"
          class="hot-search-item"
          @click="handleHotSearchClick(item.keyword)"
        >
          <span class="rank" :class="{'top-rank': index < 3}">{{ index + 1 }}</span>
          <span class="keyword">{{ item.keyword }}</span>
          <el-icon
            v-if="item.trend === 'up'"
            class="trend trend-up"
          >
            <CaretTop />
          </el-icon>
          <el-icon
            v-else-if="item.trend === 'down'"
            class="trend trend-down"
          >
            <CaretBottom />
          </el-icon>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Clock, Delete, TrendCharts, CaretTop, CaretBottom } from '@element-plus/icons-vue'
import {
  getSearchHistory,
  removeSearchHistory,
  clearSearchHistory,
  getHotSearches
} from '@/utils/searchHistory'

const props = defineProps({
  showHistory: {
    type: Boolean,
    default: true
  },
  showHotSearch: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['search'])

const searchHistory = ref([])
const hotSearches = ref([])

// 加载数据
const loadData = () => {
  searchHistory.value = getSearchHistory()
  hotSearches.value = getHotSearches()
}

// 点击历史记录
const handleHistoryClick = (keyword) => {
  emit('search', keyword)
}

// 删除单条历史
const handleRemoveHistory = (keyword) => {
  removeSearchHistory(keyword)
  loadData()
}

// 清空历史
const handleClearHistory = () => {
  ElMessageBox.confirm('确定要清空所有搜索历史吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    clearSearchHistory()
    loadData()
  }).catch(() => {
    // 取消操作
  })
}

// 点击热门搜索
const handleHotSearchClick = (keyword) => {
  emit('search', keyword)
}

onMounted(() => {
  loadData()
})

// 暴露方法供父组件调用
defineExpose({
  refresh: loadData
})
</script>

<style scoped>
.search-history {
  --history-text: #f4e8eb;
  --history-muted: rgba(244, 232, 235, 0.68);
  --history-soft: rgba(255, 234, 239, 0.07);
  --history-border: rgba(255, 234, 239, 0.1);
  padding: 20px;
  background: linear-gradient(135deg, rgba(33, 24, 39, 0.96) 0%, rgba(21, 16, 28, 0.96) 100%);
  border-radius: 16px;
  border: 1px solid var(--history-border);
  box-shadow: 0 20px 44px rgba(9, 7, 15, 0.32);
  backdrop-filter: blur(18px);
}

.history-section,
.hot-search-section {
  margin-bottom: 24px;
}

.history-section:last-child,
.hot-search-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: var(--history-text);
}

.section-title .el-icon {
  font-size: 18px;
  color: #d9899a;
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.3s;
  background: var(--history-soft);
  border-color: var(--history-border);
  color: var(--history-text);
}

.history-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 24px rgba(12, 9, 18, 0.22);
  border-color: rgba(216, 137, 154, 0.35);
}

.hot-search-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hot-search-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--history-soft);
  border: 1px solid var(--history-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.hot-search-item:hover {
  background: rgba(193, 101, 124, 0.12);
  transform: translateX(4px);
}

.rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  font-size: 14px;
  font-weight: 500;
  color: var(--history-muted);
  background: rgba(255, 234, 239, 0.08);
  border-radius: 4px;
  flex-shrink: 0;
}

.rank.top-rank {
  color: #fff;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
}

.keyword {
  flex: 1;
  font-size: 14px;
  color: var(--history-text);
}

.trend {
  font-size: 16px;
  flex-shrink: 0;
}

.trend-up {
  color: #ff8ca3;
}

.trend-down {
  color: #81d5a1;
}

.search-history :deep(.el-button.is-text),
.search-history :deep(.el-button--text) {
  color: var(--history-muted);
}

.search-history :deep(.el-button.is-text:hover),
.search-history :deep(.el-button--text:hover) {
  color: #fff1f4;
  background: rgba(255, 234, 239, 0.06);
}

.search-history :deep(.el-tag) {
  --el-tag-bg-color: rgba(255, 234, 239, 0.08);
  --el-tag-border-color: rgba(255, 234, 239, 0.1);
  --el-tag-text-color: #f4e8eb;
}

/* 响应式 */
@media (max-width: 768px) {
  .search-history {
    padding: 15px;
  }
  
  .section-title {
    font-size: 14px;
  }
  
  .hot-search-item {
    padding: 10px;
  }
}

</style>

