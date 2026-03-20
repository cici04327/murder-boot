<template>
  <div class="search-page">
    <!-- 搜索头部 -->
    <div class="search-header">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索剧本、门店..."
        :prefix-icon="Search"
        clearable
        size="large"
        @keyup.enter="handleSearch"
        @focus="showHistoryPanel = true"
        class="search-input"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      
      <!-- 搜索历史面板 -->
      <div v-if="showHistoryPanel && !hasSearched" class="history-panel">
        <SearchHistory @search="handleHistorySearch" />
      </div>
      
      <el-tabs v-model="activeTab" class="search-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="全部" name="all"></el-tab-pane>
        <el-tab-pane name="script">
          <template #label>
            剧本 <el-badge :value="scriptTotal" :hidden="scriptTotal === 0" />
          </template>
        </el-tab-pane>
        <el-tab-pane name="store">
          <template #label>
            门店 <el-badge :value="storeTotal" :hidden="storeTotal === 0" />
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 搜索结果 -->
    <div class="search-content" v-loading="loading">
      <!-- 全部结果 -->
      <div v-if="activeTab === 'all'">
        <!-- 剧本结果 -->
        <div v-if="scriptList.length > 0" class="result-section">
          <div class="section-header">
            <h3>剧本结果</h3>
            <el-button text @click="activeTab = 'script'">
              查看全部 {{ scriptTotal }} 个剧本 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <el-row :gutter="20">
            <el-col
              v-for="script in scriptList.slice(0, 4)"
              :key="script.id"
              :span="6"
            >
              <div class="script-card" @click="goToScriptDetail(script.id)">
                <el-image :src="script.cover || '/default-script.jpg'" fit="cover" class="card-image" />
                <div class="card-content">
                  <h4>{{ script.name }}</h4>
                  <p>{{ script.categoryName }} | {{ script.playerCount }}人</p>
                  <div class="card-price">¥{{ script.price }}</div>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 门店结果 -->
        <div v-if="storeList.length > 0" class="result-section">
          <div class="section-header">
            <h3>门店结果</h3>
            <el-button text @click="activeTab = 'store'">
              查看全部 {{ storeTotal }} 个门店 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
          <el-row :gutter="20">
            <el-col
              v-for="store in storeList.slice(0, 4)"
              :key="store.id"
              :span="6"
            >
              <div class="store-card" @click="goToStoreDetail(store.id)">
                <el-image :src="store.image || '/default-store.jpg'" fit="cover" class="card-image" />
                <div class="card-content">
                  <h4>{{ store.name }}</h4>
                  <p>{{ store.address }}</p>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 无结果 -->
        <EmptyState
          v-if="!loading && scriptList.length === 0 && storeList.length === 0"
          type="no-search-result"
          @action="clearSearch"
        >
          <template #extra>
            <SearchHistory :show-history="false" @search="handleHistorySearch" />
          </template>
        </EmptyState>
      </div>

      <!-- 剧本结果 -->
      <div v-else-if="activeTab === 'script'">
        <el-row :gutter="20" v-if="scriptList.length > 0">
          <el-col
            v-for="script in scriptList"
            :key="script.id"
            :span="6"
          >
            <div class="script-card" @click="goToScriptDetail(script.id)">
              <el-image :src="script.cover || '/default-script.jpg'" fit="cover" class="card-image" />
              <div class="card-content">
                <h4>{{ script.name }}</h4>
                <p>{{ script.categoryName }} | {{ script.playerCount }}人</p>
                <div class="card-price">¥{{ script.price }}</div>
              </div>
            </div>
          </el-col>
        </el-row>
        
        <EmptyState
          v-else
          type="no-search-result"
          @action="clearSearch"
        />
        
        <!-- 分页 -->
        <el-pagination
          v-if="scriptTotal > pageSize"
          v-model:current-page="scriptPage"
          v-model:page-size="pageSize"
          :total="scriptTotal"
          layout="prev, pager, next, jumper"
          @current-change="handleScriptPageChange"
          class="pagination"
        />
      </div>

      <!-- 门店结果 -->
      <div v-else-if="activeTab === 'store'">
        <el-row :gutter="20" v-if="storeList.length > 0">
          <el-col
            v-for="store in storeList"
            :key="store.id"
            :span="6"
          >
            <div class="store-card" @click="goToStoreDetail(store.id)">
              <el-image :src="store.image || '/default-store.jpg'" fit="cover" class="card-image" />
              <div class="card-content">
                <h4>{{ store.name }}</h4>
                <p>{{ store.address }}</p>
              </div>
            </div>
          </el-col>
        </el-row>
        
        <EmptyState
          v-else
          type="no-search-result"
          @action="clearSearch"
        />
        
        <!-- 分页 -->
        <el-pagination
          v-if="storeTotal > pageSize"
          v-model:current-page="storePage"
          v-model:page-size="pageSize"
          :total="storeTotal"
          layout="prev, pager, next, jumper"
          @current-change="handleStorePageChange"
          class="pagination"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { PLACEHOLDERS } from '@/assets/placeholders'
import { useRouter, useRoute } from 'vue-router'
import { Search, ArrowRight } from '@element-plus/icons-vue'
import { searchScripts, searchStores, saveSearchHistory } from '@/api/search'
import EmptyState from '@/components/EmptyState.vue'
import SearchHistory from '@/components/SearchHistory.vue'
import { addSearchHistory } from '@/utils/searchHistory'

const router = useRouter()
const route = useRoute()

const searchKeyword = ref('')
const activeTab = ref('all')
const loading = ref(false)
const showHistoryPanel = ref(false)
const hasSearched = ref(false)

const scriptList = ref([])
const scriptPage = ref(1)
const scriptTotal = ref(0)

const storeList = ref([])
const storePage = ref(1)
const storeTotal = ref(0)

const pageSize = ref(12)

const initSearch = () => {
  searchKeyword.value = route.query.keyword || ''
  activeTab.value = route.query.type || 'all'
  
  if (searchKeyword.value) {
    performSearch()
  }
}

const performSearch = async () => {
  if (!searchKeyword.value.trim()) return
  
  loading.value = true
  
  try {
    if (activeTab.value === 'all' || activeTab.value === 'script') {
      await searchScriptData()
    }
    
    if (activeTab.value === 'all' || activeTab.value === 'store') {
      await searchStoreData()
    }
    
    saveSearchHistory(searchKeyword.value)
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    loading.value = false
  }
}

const searchScriptData = async () => {
  const res = await searchScripts(searchKeyword.value, {
    page: scriptPage.value,
    pageSize: pageSize.value
  })
  
  if (res.code === 200) {
    scriptList.value = res.data.records || []
    scriptTotal.value = res.data.total || 0
  }
}

const searchStoreData = async () => {
  const res = await searchStores(searchKeyword.value, {
    page: storePage.value,
    pageSize: pageSize.value
  })
  
  if (res.code === 200) {
    storeList.value = res.data.records || []
    storeTotal.value = res.data.total || 0
  }
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  
  // 添加到搜索历史
  addSearchHistory(searchKeyword.value)
  
  // 隐藏历史面板
  showHistoryPanel.value = false
  hasSearched.value = true
  
  scriptPage.value = 1
  storePage.value = 1
  
  router.push({
    path: '/search',
    query: { 
      keyword: searchKeyword.value,
      type: activeTab.value
    }
  })
  
  performSearch()
}

// 从历史记录搜索
const handleHistorySearch = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

// 清空搜索
const clearSearch = () => {
  searchKeyword.value = ''
  hasSearched.value = false
  scriptList.value = []
  storeList.value = []
  scriptTotal.value = 0
  storeTotal.value = 0
}

const handleTabChange = (tab) => {
  router.push({
    path: '/search',
    query: { 
      keyword: searchKeyword.value,
      type: tab
    }
  })
  
  performSearch()
}

const handleScriptPageChange = () => {
  searchScriptData()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const handleStorePageChange = () => {
  searchStoreData()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const goToScriptDetail = (id) => {
  router.push(`/script/${id}`)
}

const goToStoreDetail = (id) => {
  router.push(`/store/${id}`)
}

watch(() => route.query, () => {
  if (route.path === '/search') {
    initSearch()
  }
}, { deep: true })

onMounted(() => {
  initSearch()
})
</script>

<style scoped>
.search-page {
  --search-page-bg: linear-gradient(180deg, #14101d 0%, #1c1421 42%, #1a1320 100%);
  --search-panel-bg: linear-gradient(135deg, rgba(37, 27, 41, 0.96) 0%, rgba(23, 17, 29, 0.96) 100%);
  --search-panel-soft: rgba(255, 233, 238, 0.06);
  --search-panel-soft-strong: rgba(255, 233, 238, 0.1);
  --search-panel-border: rgba(255, 233, 238, 0.1);
  --search-page-text: #f6e9ec;
  --search-page-muted: rgba(246, 233, 236, 0.68);
  --search-page-accent: #dd8da1;
  max-width: 1200px;
  margin: 0 auto;
  min-height: calc(100vh - 64px - 100px);
  padding: 24px 20px 36px;
  background: var(--search-page-bg);
  border-radius: 28px;
  border: 1px solid rgba(255, 233, 238, 0.08);
  box-shadow: 0 28px 64px rgba(8, 7, 14, 0.34);
  position: relative;
  overflow: hidden;
}

.search-page::before,
.search-page::after {
  content: '';
  position: absolute;
  inset: auto;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(8px);
}

.search-page::before {
  width: 280px;
  height: 280px;
  top: -120px;
  right: -60px;
  background: radial-gradient(circle, rgba(183, 90, 114, 0.26) 0%, rgba(183, 90, 114, 0) 72%);
}

.search-page::after {
  width: 220px;
  height: 220px;
  left: -90px;
  bottom: 30px;
  background: radial-gradient(circle, rgba(94, 48, 69, 0.22) 0%, rgba(94, 48, 69, 0) 72%);
}

.search-header {
  margin-bottom: 30px;
  position: relative;
  padding: 20px;
  border-radius: 24px;
  background: var(--search-panel-bg);
  border: 1px solid var(--search-panel-border);
  box-shadow: 0 20px 48px rgba(8, 7, 14, 0.26);
  backdrop-filter: blur(18px);
}

.search-input {
  margin-bottom: 20px;
}

.history-panel {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 10px;
  background: transparent;
  border-radius: 18px;
  box-shadow: 0 18px 48px rgba(8, 7, 14, 0.28);
  z-index: 100;
  overflow: hidden;
}

.search-content {
  min-height: 400px;
}

.result-section {
  margin-bottom: 40px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 0 6px 12px;
  border-bottom: 1px solid rgba(255, 233, 238, 0.08);
}

.section-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--search-page-text);
}

.script-card,
.store-card {
  cursor: pointer;
  border-radius: 18px;
  overflow: hidden;
  transition: all 0.3s;
  margin-bottom: 20px;
  background: var(--search-panel-bg);
  border: 1px solid var(--search-panel-border);
  box-shadow: 0 16px 36px rgba(8, 7, 14, 0.22);
}

.script-card:hover,
.store-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 22px 46px rgba(8, 7, 14, 0.3);
  border-color: rgba(221, 141, 161, 0.22);
}

.card-image {
  width: 100%;
  height: 200px;
}

.card-content {
  padding: 15px;
}

.card-content h4 {
  margin: 0 0 10px 0;
  font-size: 16px;
  color: var(--search-page-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-content p {
  margin: 0 0 10px 0;
  font-size: 13px;
  color: var(--search-page-muted);
}

.card-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff9cb0;
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.search-page :deep(.el-input__wrapper) {
  background: rgba(255, 233, 238, 0.06);
  border: 1px solid rgba(255, 233, 238, 0.1);
  box-shadow: none;
}

.search-page :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px rgba(221, 141, 161, 0.45) inset;
}

.search-page :deep(.el-input__inner) {
  color: var(--search-page-text);
}

.search-page :deep(.el-input__inner::placeholder) {
  color: rgba(246, 233, 236, 0.46);
}

.search-page :deep(.el-input-group__append) {
  background: rgba(255, 233, 238, 0.08);
  border-color: rgba(255, 233, 238, 0.1);
  color: var(--search-page-text);
}

.search-page :deep(.el-input-group__append .el-button) {
  color: var(--search-page-text);
}

.search-page :deep(.el-input__prefix),
.search-page :deep(.el-input__suffix),
.search-page :deep(.el-input__icon) {
  color: var(--search-page-muted);
}

.search-page :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 233, 238, 0.08);
}

.search-page :deep(.el-tabs__item) {
  color: var(--search-page-muted);
}

.search-page :deep(.el-tabs__item.is-active),
.search-page :deep(.el-tabs__item:hover) {
  color: var(--search-page-text);
}

.search-page :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #e7a0b1 0%, #b55974 100%);
}

.search-page :deep(.el-badge__content) {
  border: none;
  background: #b55974;
}

.search-page :deep(.el-button.is-text),
.search-page :deep(.el-button--text) {
  color: var(--search-page-muted);
}

.search-page :deep(.el-button.is-text:hover),
.search-page :deep(.el-button--text:hover) {
  color: #fff0f3;
  background: rgba(255, 233, 238, 0.06);
}

.search-page :deep(.el-pagination) {
  --el-pagination-bg-color: rgba(255, 233, 238, 0.04);
  --el-pagination-text-color: var(--search-page-muted);
  --el-pagination-button-color: var(--search-page-muted);
  --el-pagination-button-bg-color: rgba(255, 233, 238, 0.04);
  --el-pagination-button-disabled-bg-color: rgba(255, 233, 238, 0.03);
  --el-pagination-hover-color: #fff0f3;
}

.search-page :deep(.el-pager li),
.search-page :deep(.btn-prev),
.search-page :deep(.btn-next) {
  border-radius: 10px;
}

.search-page :deep(.el-empty__description),
.search-page :deep(.el-empty__description p) {
  color: var(--search-page-muted);
}
</style>
