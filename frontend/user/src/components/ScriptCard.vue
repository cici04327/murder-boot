<template>
  <div class="script-card" @click="handleClick">
    <div class="card-cover">
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

      <!-- 独家标签 -->
      <div class="exclusive-badge" v-if="script.isExclusive">独家</div>

      <!-- 评分 -->
      <div class="rating-badge" v-if="script.rating">
        <i class="el-icon-star-on"></i>
        {{ script.rating }}
      </div>
    </div>

    <div class="card-content">
      <h4 class="script-title" :title="script.name">{{ script.name }}</h4>
      
      <div class="script-info">
        <span class="info-item">
          <i class="el-icon-user"></i>
          {{ script.playerCount }}人
        </span>
        <span class="info-item">
          <i class="el-icon-time"></i>
          {{ script.duration }}h
        </span>
      </div>

      <div class="script-price">¥{{ script.price }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ScriptCard',
  props: {
    script: {
      type: Object,
      required: true
    }
  },
  methods: {
    handleClick() {
      this.$emit('click', this.script)
    }
  }
}
</script>

<style scoped>
.script-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.script-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.card-cover {
  width: 100%;
  height: 150px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.script-card:hover .cover-image {
  transform: scale(1.08);
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
  font-size: 32px;
  gap: 6px;
}

.image-error p {
  font-size: 11px;
  margin: 0;
  color: #909399;
}

.image-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.exclusive-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.rating-badge {
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

.card-content {
  padding: 10px;
}

.script-title {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.4;
}

.script-info {
  display: flex;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 3px;
}

.script-price {
  font-size: 16px;
  font-weight: bold;
  color: #ff5733;
}

@media (max-width: 767px) {
  .card-cover {
    height: 130px;
  }

  .card-content {
    padding: 8px;
  }

  .script-title {
    font-size: 13px;
    margin-bottom: 4px;
  }

  .script-info {
    font-size: 11px;
    gap: 8px;
  }

  .script-price {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .card-cover {
    height: 110px;
  }

  .card-content {
    padding: 6px;
  }

  .script-title {
    font-size: 12px;
  }

  .script-info {
    font-size: 10px;
    gap: 6px;
  }

  .script-price {
    font-size: 13px;
  }
}
</style>
