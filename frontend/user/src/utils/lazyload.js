/**
 * 图片懒加载工具函数
 */

/**
 * 预加载图片
 * @param {string} src - 图片地址
 * @returns {Promise} - 返回Promise
 */
export function preloadImage(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

/**
 * 批量预加载图片
 * @param {Array<string>} srcs - 图片地址数组
 * @returns {Promise<Array>} - 返回Promise数组
 */
export function preloadImages(srcs) {
  return Promise.all(srcs.map(src => preloadImage(src)))
}

/**
 * 获取图片的真实尺寸
 * @param {string} src - 图片地址
 * @returns {Promise<{width: number, height: number}>}
 */
export function getImageSize(src) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => {
      resolve({
        width: img.naturalWidth,
        height: img.naturalHeight
      })
    }
    img.onerror = reject
    img.src = src
  })
}

/**
 * 生成占位图
 * @param {number} width - 宽度
 * @param {number} height - 高度
 * @param {string} bgColor - 背景色
 * @param {string} text - 文字
 * @returns {string} - Base64 SVG
 */
export function generatePlaceholder(width = 100, height = 100, bgColor = '#f5f5f5', text = '加载中...') {
  const svg = `
    <svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
      <rect width="${width}" height="${height}" fill="${bgColor}"/>
      <text x="50%" y="50%" font-size="14" fill="#ccc" text-anchor="middle" dominant-baseline="middle">${text}</text>
    </svg>
  `
  return `data:image/svg+xml;base64,${btoa(unescape(encodeURIComponent(svg)))}`
}

/**
 * 生成缩略图URL（适用于支持图片处理的CDN）
 * @param {string} url - 原始图片URL
 * @param {number} width - 目标宽度
 * @param {number} height - 目标高度
 * @param {string} mode - 缩放模式 (cover/contain/fill)
 * @returns {string}
 */
export function getThumbnailUrl(url, width, height, mode = 'cover') {
  if (!url) return ''
  
  // 如果是Unsplash图片，添加尺寸参数
  if (url.includes('unsplash.com')) {
    const separator = url.includes('?') ? '&' : '?'
    return `${url}${separator}w=${width}&h=${height}&fit=${mode}&auto=format`
  }
  
  // 如果是本地图片或其他CDN，返回原URL
  // 这里可以根据实际使用的CDN添加相应的参数
  return url
}

/**
 * 响应式图片URL生成器
 * @param {string} url - 原始图片URL
 * @returns {Object} - 包含不同尺寸的图片URL
 */
export function getResponsiveImageUrls(url) {
  return {
    small: getThumbnailUrl(url, 400, 300),
    medium: getThumbnailUrl(url, 800, 600),
    large: getThumbnailUrl(url, 1200, 900),
    original: url
  }
}

/**
 * 创建图片懒加载实例
 * @param {Object} options - 配置项
 * @returns {IntersectionObserver}
 */
export function createLazyLoader(options = {}) {
  const {
    threshold = 100, // 提前加载阈值（px）
    onLoad = () => {}, // 加载成功回调
    onError = () => {} // 加载失败回调
  } = options
  
  const observerOptions = {
    root: null,
    rootMargin: `${threshold}px`,
    threshold: 0.01
  }
  
  const loadImage = (img) => {
    const src = img.dataset.src
    if (!src) return
    
    const tempImg = new Image()
    
    tempImg.onload = () => {
      img.src = src
      img.classList.add('loaded')
      onLoad(img)
    }
    
    tempImg.onerror = () => {
      img.classList.add('error')
      onError(img)
    }
    
    tempImg.src = src
  }
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        const img = entry.target
        loadImage(img)
        observer.unobserve(img)
      }
    })
  }, observerOptions)
  
  return observer
}

/**
 * 检查图片是否在视口内
 * @param {HTMLElement} element - 元素
 * @param {number} threshold - 阈值
 * @returns {boolean}
 */
export function isInViewport(element, threshold = 0) {
  const rect = element.getBoundingClientRect()
  const windowHeight = window.innerHeight || document.documentElement.clientHeight
  const windowWidth = window.innerWidth || document.documentElement.clientWidth
  
  return (
    rect.top >= -threshold &&
    rect.left >= -threshold &&
    rect.bottom <= windowHeight + threshold &&
    rect.right <= windowWidth + threshold
  )
}

/**
 * 获取图片的模糊占位符（使用 BlurHash 或类似技术）
 * 这里返回一个简单的灰色占位符，实际项目中可以使用 BlurHash 库
 * @param {number} width - 宽度
 * @param {number} height - 高度
 * @returns {string}
 */
export function getBlurPlaceholder(width = 100, height = 100) {
  return generatePlaceholder(width, height, '#f0f0f0', '')
}

/**
 * 图片加载性能监控
 */
export class ImageLoadMonitor {
  constructor() {
    this.stats = {
      total: 0,
      loaded: 0,
      failed: 0,
      totalLoadTime: 0
    }
  }
  
  onLoadStart() {
    this.stats.total++
    return Date.now()
  }
  
  onLoadSuccess(startTime) {
    this.stats.loaded++
    const loadTime = Date.now() - startTime
    this.stats.totalLoadTime += loadTime
    return loadTime
  }
  
  onLoadError() {
    this.stats.failed++
  }
  
  getStats() {
    return {
      ...this.stats,
      averageLoadTime: this.stats.loaded > 0 
        ? Math.round(this.stats.totalLoadTime / this.stats.loaded)
        : 0,
      successRate: this.stats.total > 0
        ? Math.round((this.stats.loaded / this.stats.total) * 100)
        : 0
    }
  }
  
  reset() {
    this.stats = {
      total: 0,
      loaded: 0,
      failed: 0,
      totalLoadTime: 0
    }
  }
}

// 全局监控实例
export const imageMonitor = new ImageLoadMonitor()

export default {
  preloadImage,
  preloadImages,
  getImageSize,
  generatePlaceholder,
  getThumbnailUrl,
  getResponsiveImageUrls,
  createLazyLoader,
  isInViewport,
  getBlurPlaceholder,
  ImageLoadMonitor,
  imageMonitor
}

