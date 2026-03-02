<template>
  <div class="theme-switcher">
    <el-dropdown trigger="click" @command="handleThemeChange">
      <el-button circle>
        <el-icon><Brush /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item 
            v-for="theme in themes" 
            :key="theme.value"
            :command="theme.value"
            :class="{ 'is-active': currentTheme === theme.value }"
          >
            <span class="theme-icon">{{ theme.icon }}</span>
            <span class="theme-label">{{ theme.label }}</span>
            <el-icon v-if="currentTheme === theme.value" class="check-icon">
              <Check />
            </el-icon>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup>
import { Brush, Check } from '@element-plus/icons-vue'
import { useTheme } from '@/composables/useTheme'
import { ElMessage } from 'element-plus'

const { currentTheme, themes, setTheme } = useTheme()

const handleThemeChange = (theme) => {
  setTheme(theme)
  const themeInfo = themes.find(t => t.value === theme)
  ElMessage.success(`已切换到 ${themeInfo.icon} ${themeInfo.label} 主题`)
}
</script>

<style scoped>
.theme-switcher {
  display: inline-block;
}

:deep(.el-dropdown-menu__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
}

:deep(.el-dropdown-menu__item.is-active) {
  background-color: #ecf5ff;
  color: #409eff;
}

.theme-icon {
  font-size: 18px;
  width: 24px;
  text-align: center;
}

.theme-label {
  flex: 1;
}

.check-icon {
  color: #409eff;
  font-size: 16px;
}
</style>
