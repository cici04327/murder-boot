<template>
  <div class="skeleton-script-list">
    <el-row :gutter="20">
      <el-col
        v-for="i in count"
        :key="i"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <SkeletonScriptCard :image-height="imageHeight" />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import SkeletonScriptCard from './SkeletonScriptCard.vue'

defineProps({
  count: {
    type: Number,
    default: 12
  },
  imageHeight: {
    type: [String, Number],
    default: 200
  }
})
</script>

<style scoped>
.skeleton-script-list {
  padding: 20px 0;
}

.skeleton-script-list .el-col {
  margin-bottom: 20px;
}
</style>

