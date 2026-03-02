/**
 * 主题切换 Composable
 * 用于管理用户端背景主题
 */

import { ref, onMounted } from 'vue'

const THEME_KEY = 'user-theme'

// 可用主题列表
export const THEMES = [
  { value: 'default', label: '默认彩虹', icon: '🌈' },
  { value: 'dark', label: '神秘暗夜', icon: '🌃' },
  { value: 'purple', label: '浪漫紫色', icon: '💜' },
  { value: 'tech', label: '科技蓝', icon: '🔷' },
  { value: 'warm', label: '温暖橙', icon: '🧡' },
  { value: 'nature', label: '清新绿', icon: '🍃' },
  { value: 'aurora', label: '极光', icon: '✨' },
  { value: 'mystery', label: '悬疑深红', icon: '🔴' },
  { value: 'light', label: '简约浅色', icon: '☁️' },
  { value: 'starry', label: '星空', icon: '⭐' }
]

export function useTheme() {
  const currentTheme = ref('default')

  // 加载保存的主题
  const loadTheme = () => {
    const saved = localStorage.getItem(THEME_KEY)
    if (saved && THEMES.some(t => t.value === saved)) {
      currentTheme.value = saved
    }
    applyTheme(currentTheme.value)
  }

  // 应用主题
  const applyTheme = (theme) => {
    // 移除所有主题类
    document.body.classList.remove(...THEMES.map(t => `theme-${t.value}`))
    
    // 添加新主题类
    document.body.classList.add(`theme-${theme}`)
    currentTheme.value = theme
  }

  // 切换主题
  const setTheme = (theme) => {
    if (THEMES.some(t => t.value === theme)) {
      applyTheme(theme)
      localStorage.setItem(THEME_KEY, theme)
    }
  }

  // 获取主题信息
  const getThemeInfo = (value) => {
    return THEMES.find(t => t.value === value)
  }

  onMounted(() => {
    loadTheme()
  })

  return {
    currentTheme,
    themes: THEMES,
    setTheme,
    getThemeInfo,
    loadTheme
  }
}
