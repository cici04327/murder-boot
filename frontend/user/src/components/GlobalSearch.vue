<template>
  <div class="global-search" :class="{ 'is-focused': isFocused }">
    <!-- 搜索输入框 -->
    <el-popover
      v-model:visible="showPanel"
      :width="650"
      placement="bottom-start"
      trigger="manual"
      popper-class="search-popover"
      :show-arrow="false"
    >
      <template #reference>
        <div class="search-input-wrapper" @click="handleFocus">
          <el-icon class="search-icon"><Search /></el-icon>
          <input
            ref="searchInputRef"
            v-model="searchKeyword"
            :placeholder="placeholderText"
            @focus="handleFocus"
            @blur="handleBlur"
            @input="handleInput"
            @keyup.enter="handleSearch"
            @keyup.up.prevent="navigateResult(-1)"
            @keyup.down.prevent="navigateResult(1)"
            @keyup.esc="handleEsc"
            class="search-input"
          />
          <!-- 语音搜索按钮 -->
          <el-tooltip content="语音搜索" placement="bottom" v-if="speechSupported">
            <el-button 
              text 
              circle 
              size="small" 
              class="voice-btn"
              :class="{ 'is-recording': isRecording }"
              @click.stop="toggleVoiceSearch"
            >
              <el-icon><Microphone /></el-icon>
            </el-button>
          </el-tooltip>
          <!-- 清除按钮 -->
          <el-button 
            v-if="searchKeyword" 
            text 
            circle 
            size="small" 
            class="clear-btn"
            @click.stop="clearSearch"
          >
            <el-icon><Close /></el-icon>
          </el-button>
          <!-- 快捷键提示 -->
          <span class="shortcut-hint" v-if="!isFocused && !searchKeyword">
            <kbd>Ctrl</kbd><kbd>K</kbd>
          </span>
        </div>
      </template>

      <!-- 搜索面板：mousedown 时标记正在点击，防止 blur 误关闭面板 -->
      <div class="search-panel" :class="{ 'has-results': hasResults }"
        @mousedown="isPanelClicking = true"
        @mouseup="isPanelClicking = false"
        @mouseleave="isPanelClicking = false"
      >
        <!-- 分类筛选标签 -->
        <div class="filter-tabs" v-if="searchKeyword">
          <div 
            class="filter-tab" 
            :class="{ active: activeFilter === 'all' }"
            @click="setFilter('all')"
          >
            <el-icon><Grid /></el-icon>
            全部
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: activeFilter === 'script' }"
            @click="setFilter('script')"
          >
            <el-icon><Document /></el-icon>
            剧本
            <span class="filter-count" v-if="scriptResults.length">({{ scriptResults.length }})</span>
          </div>
          <div 
            class="filter-tab" 
            :class="{ active: activeFilter === 'store' }"
            @click="setFilter('store')"
          >
            <el-icon><Shop /></el-icon>
            门店
            <span class="filter-count" v-if="storeResults.length">({{ storeResults.length }})</span>
          </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>搜索中...</span>
        </div>

        <!-- 搜索建议（无关键词时显示） -->
        <div v-else-if="!searchKeyword" class="search-suggestions">
          <!-- 搜索历史 -->
          <div v-if="searchHistory.length > 0" class="suggestion-section">
            <div class="section-header">
              <span class="section-title">
                <el-icon><Clock /></el-icon>
                搜索历史
              </span>
              <el-button text size="small" @click="handleClearHistory">
                <el-icon><Delete /></el-icon>
                清空
              </el-button>
            </div>
            <div class="history-tags">
              <transition-group name="tag-fade">
                <el-tag
                  v-for="(item, index) in searchHistory"
                  :key="item"
                  closable
                  effect="plain"
                  @close="handleRemoveHistory(item)"
                  @click="handleClickHistory(item)"
                  class="history-tag"
                  :class="{ 'selected': selectedIndex === index && !searchKeyword }"
                >
                  {{ item }}
                </el-tag>
              </transition-group>
            </div>
          </div>

          <!-- 热门搜索 -->
          <div v-if="hotSearches.length > 0" class="suggestion-section">
            <div class="section-header">
              <span class="section-title">
                <el-icon><TrendCharts /></el-icon>
                热门搜索
              </span>
              <el-button text size="small" @click="refreshHotSearches">
                <el-icon :class="{ 'is-loading': refreshingHot }"><Refresh /></el-icon>
                换一批
              </el-button>
            </div>
            <div class="hot-searches">
              <div
                v-for="(item, index) in displayedHotSearches"
                :key="item.keyword || item"
                class="hot-item"
                :class="{ 
                  'selected': selectedIndex === searchHistory.length + index && !searchKeyword,
                  'is-hot': item.isHot,
                  'is-new': item.isNew
                }"
                @click="handleClickHistory(item.keyword || item)"
              >
                <span class="hot-rank" :class="getRankClass(index)">{{ index + 1 }}</span>
                <span class="hot-keyword">{{ item.keyword || item }}</span>
                <el-tag v-if="item.isHot" size="small" type="danger" effect="dark" class="hot-badge">热</el-tag>
                <el-tag v-else-if="item.isNew" size="small" type="success" effect="dark" class="hot-badge">新</el-tag>
                <span v-if="item.count" class="hot-count">{{ formatCount(item.count) }}次搜索</span>
              </div>
            </div>
          </div>
          
          <!-- 快速入口 -->
          <div class="quick-entries">
            <div class="section-header">
              <span class="section-title">
                <el-icon><Compass /></el-icon>
                快速入口
              </span>
            </div>
            <div class="entry-grid">
              <div class="entry-item" @click="goTo('/script?sort=hot')">
                <div class="entry-icon hot">🔥</div>
                <span>热门剧本</span>
              </div>
              <div class="entry-item" @click="goTo('/script?sort=new')">
                <div class="entry-icon new">✨</div>
                <span>最新上架</span>
              </div>
              <div class="entry-item" @click="goTo('/store')">
                <div class="entry-icon store">🏪</div>
                <span>附近门店</span>
              </div>
              <div class="entry-item" @click="goTo('/group')">
                <div class="entry-icon group">👥</div>
                <span>拼车组队</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 搜索结果 -->
        <div v-else class="search-results">
          <!-- 智能建议 -->
          <div v-if="suggestions.length > 0 && !hasResults" class="suggestions-list">
            <div class="section-header">
              <span class="section-title">
                <el-icon><MagicStick /></el-icon>
                猜你想搜
              </span>
            </div>
            <div 
              v-for="(suggestion, index) in suggestions" 
              :key="index"
              class="suggestion-item"
              :class="{ 'selected': selectedIndex === index }"
              @click="handleClickSuggestion(suggestion)"
            >
              <el-icon><Search /></el-icon>
              <span v-html="highlightKeyword(suggestion)"></span>
            </div>
          </div>

          <!-- 剧本结果 -->
          <div v-if="scriptResults.length > 0 && (activeFilter === 'all' || activeFilter === 'script')" class="result-section">
            <div class="section-header">
              <span class="section-title">
                <el-icon><Document /></el-icon>
                剧本
                <span class="result-count">{{ scriptResults.length }}个结果</span>
              </span>
              <el-button text size="small" @click="handleViewMore('script')">
                查看全部 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
            <div class="result-list">
              <transition-group name="result-fade">
                <div
                  v-for="(script, index) in scriptResults"
                  :key="script.id"
                  class="result-item"
                  :class="{ 'selected': getResultIndex('script', index) === selectedIndex }"
                  @click="handleClickScript(script)"
                  @mouseenter="selectedIndex = getResultIndex('script', index)"
                >
                  <el-image
                    :src="script.cover || PLACEHOLDERS.SCRIPT"
                    class="result-image"
                    fit="cover"
                    lazy
                  >
                    <template #error>
                      <div class="image-placeholder">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div class="result-info">
                    <div class="result-title" v-html="highlightKeyword(script.name)"></div>
                    <div class="result-tags">
                      <el-tag size="small" type="info">{{ script.categoryName || '剧本杀' }}</el-tag>
                      <el-tag size="small" effect="plain">{{ script.playerCount || '6-8' }}人</el-tag>
                      <el-tag size="small" effect="plain">{{ script.duration || 240 }}分钟</el-tag>
                      <el-tag v-if="script.difficulty" size="small" :type="getDifficultyType(script.difficulty)">
                        {{ script.difficulty }}
                      </el-tag>
                    </div>
                    <div class="result-footer">
                      <span class="result-price">¥{{ script.price || 128 }}</span>
                      <span class="result-rating" v-if="script.rating">
                        <el-icon><Star /></el-icon>
                        {{ script.rating }}
                      </span>
                    </div>
                  </div>
                </div>
              </transition-group>
            </div>
          </div>

          <!-- 门店结果 -->
          <div v-if="storeResults.length > 0 && (activeFilter === 'all' || activeFilter === 'store')" class="result-section">
            <div class="section-header">
              <span class="section-title">
                <el-icon><Shop /></el-icon>
                门店
                <span class="result-count">{{ storeResults.length }}个结果</span>
              </span>
              <el-button text size="small" @click="handleViewMore('store')">
                查看全部 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
            <div class="result-list">
              <transition-group name="result-fade">
                <div
                  v-for="(store, index) in storeResults"
                  :key="store.id"
                  class="result-item store-item"
                  :class="{ 'selected': getResultIndex('store', index) === selectedIndex }"
                  @click="handleClickStore(store)"
                  @mouseenter="selectedIndex = getResultIndex('store', index)"
                >
                  <el-image
                    :src="store.image || store.cover || PLACEHOLDERS.STORE"
                    class="result-image"
                    fit="cover"
                    lazy
                  >
                    <template #error>
                      <div class="image-placeholder">
                        <el-icon><Shop /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div class="result-info">
                    <div class="result-title" v-html="highlightKeyword(store.name)"></div>
                    <div class="result-meta-row">
                      <el-rate
                        v-model="store.rating"
                        disabled
                        size="small"
                        :show-score="true"
                        score-template="{value}"
                      />
                      <span class="store-scripts">{{ store.scriptCount || 0 }}个剧本</span>
                    </div>
                    <div class="result-address">
                      <el-icon><Location /></el-icon>
                      {{ store.address || '暂无地址信息' }}
                    </div>
                  </div>
                </div>
              </transition-group>
            </div>
          </div>

          <!-- 无结果 -->
          <div v-if="!loading && !hasResults" class="no-results">
            <el-empty description="未找到相关内容" :image-size="100">
              <template #description>
                <p>未找到与 "<strong>{{ searchKeyword }}</strong>" 相关的内容</p>
                <p class="no-results-tips">试试其他关键词，或浏览热门内容</p>
              </template>
            </el-empty>
          </div>
        </div>

        <!-- 底部提示 -->
        <div class="panel-footer" v-if="hasResults">
          <span class="footer-tip">
            <kbd>↑</kbd><kbd>↓</kbd> 选择
            <kbd>Enter</kbd> 确认
            <kbd>Esc</kbd> 关闭
          </span>
          <el-button text size="small" type="primary" @click="handleSearch">
            查看全部结果 <el-icon><ArrowRight /></el-icon>
          </el-button>
        </div>
      </div>
    </el-popover>
    
    <!-- 语音识别提示 -->
    <transition name="fade">
      <div v-if="isRecording" class="voice-overlay" @click="stopVoiceSearch">
        <div class="voice-modal">
          <div class="voice-animation">
            <span></span><span></span><span></span><span></span><span></span>
          </div>
          <p class="voice-text">{{ voiceText || '请说出您想搜索的内容...' }}</p>
          <el-button type="danger" round @click.stop="stopVoiceSearch">
            <el-icon><Close /></el-icon>
            停止录音
          </el-button>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Clock, TrendCharts, Document, Shop, ArrowRight,
  Microphone, Close, Delete, Refresh, Grid, Loading,
  Compass, MagicStick, Picture, Star, Location
} from '@element-plus/icons-vue'
import {
  searchScripts,
  searchStores,
  saveSearchHistory,
  getSearchHistory,
  clearSearchHistory,
  removeSearchHistory
} from '@/api/search'
import { PLACEHOLDERS } from '@/assets/placeholders'

const router = useRouter()

// 搜索输入框引用
const searchInputRef = ref(null)

// 搜索状态
const searchKeyword = ref('')
const showPanel = ref(false)
const loading = ref(false)
const isFocused = ref(false)
const activeFilter = ref('all')
const selectedIndex = ref(-1)

// 占位符文字轮播
const placeholders = [
  '搜索剧本、门店...',
  '试试搜索"年轮"',
  '搜索附近的门店',
  '查找推理类剧本',
  '搜索热门剧本'
]
const placeholderIndex = ref(0)
const placeholderText = computed(() => placeholders[placeholderIndex.value])

// 搜索历史和热门搜索
const searchHistory = ref([])
const hotSearches = ref([
  { keyword: '年轮', count: 12580, isHot: true },
  { keyword: '东方快车谋杀案', count: 9820, isHot: true },
  { keyword: '无人生还', count: 8650 },
  { keyword: '红蝶', count: 7230, isNew: true },
  { keyword: '机械迷城', count: 6890 },
  { keyword: '长安', count: 5670 },
  { keyword: '迷失庄园', count: 4520 },
  { keyword: '时空旅行', count: 3890, isNew: true },
  { keyword: '黑暗森林', count: 3210 },
  { keyword: '寂静岭', count: 2980 }
])
const hotSearchPage = ref(0)
const refreshingHot = ref(false)

// 显示的热门搜索（分页显示）
const displayedHotSearches = computed(() => {
  const pageSize = 5
  const start = hotSearchPage.value * pageSize
  return hotSearches.value.slice(start, start + pageSize)
})

// 搜索建议
const suggestions = ref([])

// 搜索结果
const scriptResults = ref([])
const storeResults = ref([])

// 是否有搜索结果
const hasResults = computed(() => {
  return scriptResults.value.length > 0 || storeResults.value.length > 0
})

// 语音搜索相关
const speechSupported = ref(false)
const isRecording = ref(false)
const voiceText = ref('')
let recognition = null

// 防抖定时器
let debounceTimer = null
let placeholderTimer = null
// 标记用户是否正在点击面板内部（mousedown 先于 blur 触发）
let isPanelClicking = false

// ========== 初始化 ==========
const initSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (SpeechRecognition) {
    speechSupported.value = true
    recognition = new SpeechRecognition()
    recognition.continuous = false
    recognition.interimResults = true
    recognition.lang = 'zh-CN'
    
    recognition.onresult = (event) => {
      const result = event.results[event.results.length - 1]
      voiceText.value = result[0].transcript
      if (result.isFinal) {
        searchKeyword.value = voiceText.value
        stopVoiceSearch()
        performSearch()
      }
    }
    
    recognition.onerror = (event) => {
      console.error('语音识别错误:', event.error)
      ElMessage.error('语音识别失败，请重试')
      stopVoiceSearch()
    }
    
    recognition.onend = () => {
      if (isRecording.value) {
        stopVoiceSearch()
      }
    }
  }
}

// 开始占位符轮播
const startPlaceholderRotation = () => {
  placeholderTimer = setInterval(() => {
    if (!isFocused.value && !searchKeyword.value) {
      placeholderIndex.value = (placeholderIndex.value + 1) % placeholders.length
    }
  }, 3000)
}

// 加载搜索历史
const loadSearchHistory = () => {
  searchHistory.value = getSearchHistory()
}

// ========== 语音搜索 ==========
const toggleVoiceSearch = () => {
  if (isRecording.value) {
    stopVoiceSearch()
  } else {
    startVoiceSearch()
  }
}

const startVoiceSearch = () => {
  if (!recognition) return
  isRecording.value = true
  voiceText.value = ''
  try {
    recognition.start()
  } catch (e) {
    console.error('启动语音识别失败:', e)
    isRecording.value = false
  }
}

const stopVoiceSearch = () => {
  isRecording.value = false
  if (recognition) {
    try {
      recognition.stop()
    } catch (e) {
      // 忽略
    }
  }
}

// ========== 搜索功能 ==========
const handleFocus = () => {
  isFocused.value = true
  showPanel.value = true
  selectedIndex.value = -1
  nextTick(() => {
    searchInputRef.value?.focus()
  })
}

const handleBlur = () => {
  // 如果用户正在点击面板内部，不关闭（mousedown 比 blur 先触发）
  if (isPanelClicking) return
  setTimeout(() => {
    if (isPanelClicking) return
    isFocused.value = false
    showPanel.value = false
  }, 300)
}

const handleEsc = () => {
  showPanel.value = false
  isFocused.value = false
  searchInputRef.value?.blur()
}

const clearSearch = () => {
  searchKeyword.value = ''
  scriptResults.value = []
  storeResults.value = []
  suggestions.value = []
  selectedIndex.value = -1
  searchInputRef.value?.focus()
}

const handleInput = () => {
  clearTimeout(debounceTimer)
  selectedIndex.value = -1
  
  if (!searchKeyword.value.trim()) {
    scriptResults.value = []
    storeResults.value = []
    suggestions.value = []
    return
  }
  
  // 生成搜索建议
  generateSuggestions()
  
  debounceTimer = setTimeout(() => {
    performSearch()
  }, 300)
}

// 生成搜索建议
const generateSuggestions = () => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  const allKeywords = [
    ...hotSearches.value.map(h => h.keyword),
    '推理本', '情感本', '恐怖本', '机制本', '欢乐本',
    '新手本', '硬核本', '阵营本', '还原本', '沉浸本'
  ]
  suggestions.value = allKeywords
    .filter(k => k.toLowerCase().includes(keyword) && k.toLowerCase() !== keyword)
    .slice(0, 5)
}

const performSearch = async () => {
  if (!searchKeyword.value.trim()) return
  
  loading.value = true
  
  try {
    const [scriptRes, storeRes] = await Promise.all([
      searchScripts(searchKeyword.value, { page: 1, pageSize: 5 }),
      searchStores(searchKeyword.value, { page: 1, pageSize: 5 })
    ])
    
    scriptResults.value = scriptRes.data?.records || []
    storeResults.value = storeRes.data?.records || []
  } catch (error) {
    console.error('搜索失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (!searchKeyword.value.trim()) return
  
  saveSearchHistory(searchKeyword.value)
  loadSearchHistory()
  
  router.push({
    path: '/search',
    query: { keyword: searchKeyword.value, type: activeFilter.value !== 'all' ? activeFilter.value : undefined }
  })
  
  showPanel.value = false
  isFocused.value = false
}

// ========== 键盘导航 ==========
const navigateResult = (direction) => {
  const totalItems = getTotalNavigableItems()
  if (totalItems === 0) return
  
  selectedIndex.value += direction
  if (selectedIndex.value < 0) selectedIndex.value = totalItems - 1
  if (selectedIndex.value >= totalItems) selectedIndex.value = 0
}

const getTotalNavigableItems = () => {
  if (!searchKeyword.value) {
    return searchHistory.value.length + displayedHotSearches.value.length
  }
  if (suggestions.value.length > 0 && !hasResults.value) {
    return suggestions.value.length
  }
  let total = 0
  if (activeFilter.value === 'all' || activeFilter.value === 'script') {
    total += scriptResults.value.length
  }
  if (activeFilter.value === 'all' || activeFilter.value === 'store') {
    total += storeResults.value.length
  }
  return total
}

const getResultIndex = (type, index) => {
  if (type === 'script') return index
  if (type === 'store') {
    const scriptCount = (activeFilter.value === 'all' || activeFilter.value === 'script') ? scriptResults.value.length : 0
    return scriptCount + index
  }
  return index
}

// ========== 事件处理 ==========
const handleClickHistory = (keyword) => {
  searchKeyword.value = keyword
  handleSearch()
}

const handleClickSuggestion = (suggestion) => {
  searchKeyword.value = suggestion
  performSearch()
}

const handleClearHistory = () => {
  ElMessageBox.confirm('确定要清空搜索历史吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    clearSearchHistory()
    loadSearchHistory()
    ElMessage.success('已清空搜索历史')
  }).catch(() => {})
}

const handleRemoveHistory = (keyword) => {
  removeSearchHistory(keyword)
  loadSearchHistory()
}

const refreshHotSearches = () => {
  refreshingHot.value = true
  hotSearchPage.value = (hotSearchPage.value + 1) % Math.ceil(hotSearches.value.length / 5)
  setTimeout(() => {
    refreshingHot.value = false
  }, 500)
}

const setFilter = (filter) => {
  activeFilter.value = filter
  selectedIndex.value = -1
}

const handleViewMore = (type) => {
  if (!searchKeyword.value.trim()) return
  saveSearchHistory(searchKeyword.value)
  router.push({ path: '/search', query: { keyword: searchKeyword.value, type } })
  showPanel.value = false
}

const handleClickScript = (script) => {
  if (searchKeyword.value) saveSearchHistory(searchKeyword.value)
  router.push(`/script/${script.id}`)
  showPanel.value = false
}

const handleClickStore = (store) => {
  if (searchKeyword.value) saveSearchHistory(searchKeyword.value)
  router.push(`/store/${store.id}`)
  showPanel.value = false
}

const goTo = (path) => {
  router.push(path)
  showPanel.value = false
}

// ========== 工具方法 ==========
const highlightKeyword = (text) => {
  if (!searchKeyword.value || !text) return text
  const keyword = searchKeyword.value.trim().replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${keyword})`, 'gi')
  return text.replace(regex, '<span class="highlight">$1</span>')
}

const getRankClass = (index) => {
  if (index === 0) return 'rank-1'
  if (index === 1) return 'rank-2'
  if (index === 2) return 'rank-3'
  return ''
}

const formatCount = (count) => {
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count
}

const getDifficultyType = (difficulty) => {
  const map = { '新手': 'success', '简单': 'info', '中等': 'warning', '困难': 'danger', '硬核': 'danger' }
  return map[difficulty] || 'info'
}

// ========== 快捷键 ==========
const handleKeydown = (e) => {
  // Ctrl+K 打开搜索
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    handleFocus()
  }
}

// ========== 生命周期 ==========
onMounted(() => {
  loadSearchHistory()
  initSpeechRecognition()
  startPlaceholderRotation()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  clearInterval(placeholderTimer)
  clearTimeout(debounceTimer)
  document.removeEventListener('keydown', handleKeydown)
  if (recognition) {
    try { recognition.stop() } catch (e) {}
  }
})
</script>

<style scoped>
.global-search {
  width: 320px;
  transition: all 0.3s ease;
}

.global-search.is-focused {
  width: 380px;
}

/* 搜索输入框 */
.search-input-wrapper {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background: #f5f7fa;
  border-radius: 24px;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  cursor: text;
}

.global-search.is-focused .search-input-wrapper {
  background: #fff;
  border-color: #8B0000;
  box-shadow: 0 4px 20px rgba(139, 0, 0, 0.15);
}

.search-icon {
  font-size: 18px;
  color: #909399;
  margin-right: 8px;
  transition: color 0.3s;
}

.global-search.is-focused .search-icon {
  color: #8B0000;
}

.search-input {
  flex: 1;
  border: none;
  outline: none;
  background: transparent;
  font-size: 14px;
  color: #303133;
}

.search-input::placeholder {
  color: #c0c4cc;
}

.voice-btn, .clear-btn {
  margin-left: 4px;
  color: #909399;
}

.voice-btn:hover, .clear-btn:hover {
  color: #8B0000;
}

.voice-btn.is-recording {
  color: #f56c6c;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.shortcut-hint {
  display: flex;
  gap: 2px;
  margin-left: 8px;
}

.shortcut-hint kbd {
  padding: 2px 6px;
  font-size: 11px;
  background: #e4e7ed;
  border-radius: 4px;
  color: #909399;
}

/* 搜索面板 */
.search-panel {
  max-height: 520px;
  overflow-y: auto;
}

.search-panel::-webkit-scrollbar {
  width: 6px;
}

.search-panel::-webkit-scrollbar-thumb {
  background: #ddd;
  border-radius: 3px;
}

/* 分类筛选 */
.filter-tabs {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  background: #fafafa;
}

.filter-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 16px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.filter-tab:hover {
  background: #f0f0f0;
}

.filter-tab.active {
  background: linear-gradient(135deg, #8B0000 0%, #a01010 100%);
  color: #fff;
}

.filter-count {
  font-size: 12px;
  opacity: 0.8;
}

/* 加载状态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #909399;
}

/* 搜索建议 */
.search-suggestions,
.search-results {
  padding: 12px 0;
}

.suggestion-section,
.result-section {
  margin-bottom: 16px;
}

.suggestion-section:last-child,
.result-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  margin-bottom: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.result-count {
  font-weight: normal;
  color: #909399;
  margin-left: 4px;
}

/* 历史标签 */
.history-tags {
  padding: 0 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  transition: all 0.3s;
  border-color: #ddd;
}

.history-tag:hover,
.history-tag.selected {
  border-color: #8B0000;
  color: #8B0000;
  transform: translateY(-2px);
}

/* 标签动画 */
.tag-fade-enter-active,
.tag-fade-leave-active {
  transition: all 0.3s ease;
}

.tag-fade-enter-from,
.tag-fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

/* 热门搜索 */
.hot-searches {
  padding: 0 8px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.3s;
}

.hot-item:hover,
.hot-item.selected {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.05) 0%, rgba(139, 0, 0, 0.1) 100%);
}

.hot-rank {
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  border-radius: 6px;
  background: #e4e7ed;
  color: #909399;
}

.hot-rank.rank-1 {
  background: linear-gradient(135deg, #f56c6c 0%, #e74c3c 100%);
  color: white;
}

.hot-rank.rank-2 {
  background: linear-gradient(135deg, #e6a23c 0%, #f39c12 100%);
  color: white;
}

.hot-rank.rank-3 {
  background: linear-gradient(135deg, #409eff 0%, #3498db 100%);
  color: white;
}

.hot-keyword {
  flex: 1;
  font-size: 14px;
  color: #303133;
}

.hot-badge {
  font-size: 10px;
  padding: 0 6px;
  height: 18px;
  line-height: 18px;
}

.hot-count {
  font-size: 12px;
  color: #c0c4cc;
}

/* 快速入口 */
.quick-entries {
  margin-top: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  padding: 0 16px;
}

.entry-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px 8px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.entry-item:hover {
  background: #f5f7fa;
  transform: translateY(-2px);
}

.entry-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  border-radius: 12px;
  background: #f5f7fa;
}

.entry-item span {
  font-size: 12px;
  color: #606266;
}

/* 搜索建议列表 */
.suggestions-list {
  margin-bottom: 12px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.suggestion-item:hover,
.suggestion-item.selected {
  background: #f5f7fa;
}

.suggestion-item .el-icon {
  color: #c0c4cc;
}

/* 搜索结果 */
.result-list {
  padding: 0 8px;
}

.result-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s;
  margin-bottom: 4px;
}

.result-item:hover,
.result-item.selected {
  background: linear-gradient(135deg, rgba(139, 0, 0, 0.03) 0%, rgba(139, 0, 0, 0.08) 100%);
}

/* 结果动画 */
.result-fade-enter-active,
.result-fade-leave-active {
  transition: all 0.3s ease;
}

.result-fade-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.result-fade-leave-to {
  opacity: 0;
  transform: translateX(10px);
}

.result-image {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  flex-shrink: 0;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 24px;
}

.result-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
}

.result-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-title :deep(.highlight) {
  color: #8B0000;
  background: rgba(139, 0, 0, 0.1);
  padding: 1px 4px;
  border-radius: 3px;
}

.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 6px 0;
}

.result-tags .el-tag {
  font-size: 11px;
}

.result-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-price {
  font-size: 16px;
  font-weight: bold;
  color: #f56c6c;
}

.result-rating {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: #e6a23c;
}

/* 门店结果 */
.result-meta-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 4px 0;
}

.store-scripts {
  font-size: 12px;
  color: #909399;
}

.result-address {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 无结果 */
.no-results {
  padding: 20px;
  text-align: center;
}

.no-results p {
  margin: 4px 0;
  color: #606266;
}

.no-results-tips {
  font-size: 13px;
  color: #909399;
}

/* 底部提示 */
.panel-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.footer-tip {
  font-size: 12px;
  color: #909399;
}

.footer-tip kbd {
  padding: 2px 6px;
  margin: 0 2px;
  font-size: 11px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 4px;
}

/* 语音识别遮罩 */
.voice-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.voice-modal {
  background: #fff;
  padding: 40px 60px;
  border-radius: 20px;
  text-align: center;
}

.voice-animation {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 60px;
  margin-bottom: 20px;
}

.voice-animation span {
  width: 6px;
  background: linear-gradient(135deg, #8B0000 0%, #f56c6c 100%);
  border-radius: 3px;
  animation: voice-wave 1s ease-in-out infinite;
}

.voice-animation span:nth-child(1) { height: 20px; animation-delay: 0s; }
.voice-animation span:nth-child(2) { height: 35px; animation-delay: 0.1s; }
.voice-animation span:nth-child(3) { height: 50px; animation-delay: 0.2s; }
.voice-animation span:nth-child(4) { height: 35px; animation-delay: 0.3s; }
.voice-animation span:nth-child(5) { height: 20px; animation-delay: 0.4s; }

@keyframes voice-wave {
  0%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(1.5); }
}

.voice-text {
  font-size: 18px;
  color: #303133;
  margin-bottom: 20px;
  min-height: 28px;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
.search-popover {
  padding: 0 !important;
  border-radius: 16px !important;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.12) !important;
  border: none !important;
}
</style>
