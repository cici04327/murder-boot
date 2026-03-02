<template>
  <div class="recommendation-section">
    <div class="section-header">
      <h3 class="section-title">{{ title }}</h3>
      <div class="section-subtitle" v-if="subtitle">{{ subtitle }}</div>
    </div>

    <div v-if="scripts && scripts.length > 0" class="script-list">
      <div
        v-for="(script, index) in scripts"
        :key="script.id"
        class="script-item"
        @click="handleClick(script)"
      >
        <div class="rank-badge" v-if="showRank">{{ index + 1 }}</div>
        
        <div class="script-cover">
          <el-image
            :src="script.cover || '/default-script.jpg'"
            fit="cover"
            class="cover-image"
            lazy
            loading="lazy"
          >
            <template #placeholder>
              <div class="image-loading">
                <div class="loading-spinner"></div>
              </div>
            </template>
            <template #error>
              <div class="image-error">
                <i class="el-icon-picture-outline"></i>
                <p>加载失败</p>
              </div>
            </template>
          </el-image>
          
          <!-- 推荐标签 -->
          <div class="recommend-tag" v-if="script.recommendReason">
            {{ script.recommendReason }}
          </div>
          
          <!-- 推荐分数 -->
          <div class="recommend-score" v-if="script.recommendScore">
            <i class="el-icon-star-on"></i>
            {{ script.recommendScore }}
          </div>
        </div>

        <div class="script-info">
          <h4 class="script-name" :title="script.name">{{ script.name }}</h4>
          
          <div class="script-meta">
            <el-tag size="small" v-if="script.categoryName">{{ script.categoryName }}</el-tag>
            <el-tag size="small" type="info">{{ script.playerCount }}人</el-tag>
            <el-tag size="small" type="warning" v-if="script.difficulty">
              {{ getDifficultyText(script.difficulty) }}
            </el-tag>
          </div>

          <div class="script-tags" v-if="script.tags && script.tags.length > 0">
            <span v-for="tag in script.tags.slice(0, 3)" :key="tag" class="tag-item">
              #{{ tag }}
            </span>
          </div>

          <div class="script-bottom">
            <div class="script-rating">
              <el-rate
                v-model="script.rating"
                disabled
                show-score
                text-color="#ff9900"
                score-template="{value}"
              />
            </div>
            <div class="script-price">¥{{ script.price }}</div>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-else description="暂无推荐" :image-size="100" />
  </div>
</template>

<script>
export default {
  name: 'RecommendationSection',
  props: {
    title: {
      type: String,
      required: true
    },
    subtitle: {
      type: String,
      default: ''
    },
    scripts: {
      type: Array,
      default: () => []
    },
    showRank: {
      type: Boolean,
      default: false
    }
  },
  emits: ['script-click'],
  methods: {
    handleClick(script) {
      this.$emit('script-click', script)
    },
    getDifficultyText(difficulty) {
      const map = {
        1: '简单',
        2: '普通',
        3: '困难',
        4: '硬核'
      }
      return map[difficulty] || '普通'
    }
  }
}
</script>

<style scoped>
.recommendation-section {
  margin-bottom: 30px;
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 5px;
}

.section-subtitle {
  font-size: 14px;
  color: #909399;
}

.script-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

@media (min-width: 1400px) {
  .script-list {
    grid-template-columns: repeat(5, 1fr);
  }
}

@media (min-width: 1200px) and (max-width: 1399px) {
  .script-list {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (min-width: 768px) and (max-width: 1199px) {
  .script-list {
    grid-template-columns: repeat(3, 1fr);
  }
}

.script-item {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: relative;
}

.script-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.rank-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  width: 32px;
  height: 32px;
  background: #8B0000;
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 16px;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.script-cover {
  width: 100%;
  height: 160px;
  position: relative;
  overflow: hidden;
  background: #f0f0f0;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.script-item:hover .cover-image {
  transform: scale(1.05);
}

.image-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 36px;
  gap: 8px;
}

.image-error p {
  font-size: 12px;
  margin: 0;
}

.image-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f0f0f0;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(139, 0, 0, 0.3);
  border-top-color: #8B0000;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.recommend-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 87, 51, 0.9);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.recommend-score {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.6);
  color: #ffd700;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 4px;
}

.script-info {
  padding: 15px;
}

.script-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.script-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.script-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.tag-item {
  font-size: 12px;
  color: #909399;
  background: #f4f4f5;
  padding: 2px 8px;
  border-radius: 4px;
}

.script-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.script-rating {
  font-size: 14px;
}

.script-price {
  font-size: 18px;
  font-weight: bold;
  color: #ff5733;
}

@media (max-width: 767px) {
  .script-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .script-cover {
    height: 140px;
  }

  .script-info {
    padding: 8px;
  }

  .script-name {
    font-size: 13px;
    margin-bottom: 6px;
  }

  .script-meta {
    gap: 4px;
  }

  .script-tags {
    gap: 4px;
  }

  .tag-item {
    font-size: 11px;
    padding: 1px 6px;
  }

  .script-price {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .script-list {
    gap: 8px;
  }

  .script-cover {
    height: 120px;
  }

  .script-info {
    padding: 6px;
  }

  .script-name {
    font-size: 12px;
  }

  .script-bottom {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>
