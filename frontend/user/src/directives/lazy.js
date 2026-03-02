/**
 * v-lazy 图片懒加载指令
 * 使用 Intersection Observer API 实现
 */

// 默认占位图
const DEFAULT_PLACEHOLDER = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgZmlsbD0iI2Y1ZjVmNSIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjE4IiBmaWxsPSIjY2NjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkb21pbmFudC1iYXNlbGluZT0ibWlkZGxlIj7liqDovb3kuK08L3RleHQ+PC9zdmc+'

// 创建观察器映射
const observerMap = new WeakMap()

// 加载图片
const loadImage = (el, binding) => {
  const imageSrc = binding.value
  
  if (!imageSrc) return
  
  // 创建临时图片对象预加载
  const img = new Image()
  
  img.onload = () => {
    // 加载成功，替换src
    el.src = imageSrc
    el.classList.add('lazy-loaded')
    el.classList.remove('lazy-loading')
  }
  
  img.onerror = () => {
    // 加载失败
    el.classList.add('lazy-error')
    el.classList.remove('lazy-loading')
    
    // 触发自定义错误事件
    el.dispatchEvent(new CustomEvent('lazy-error', {
      detail: { src: imageSrc }
    }))
  }
  
  // 添加加载中类名
  el.classList.add('lazy-loading')
  
  // 开始加载
  img.src = imageSrc
}

// 创建 Intersection Observer
const createObserver = (el, binding) => {
  const options = {
    root: null,
    rootMargin: '100px', // 提前100px开始加载
    threshold: 0.01
  }
  
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        // 元素进入视口，开始加载图片
        loadImage(el, binding)
        
        // 加载后停止观察
        observer.unobserve(el)
        observerMap.delete(el)
      }
    })
  }, options)
  
  // 开始观察
  observer.observe(el)
  
  // 保存observer引用
  observerMap.set(el, observer)
}

// 懒加载指令
export const lazy = {
  // 指令挂载时
  mounted(el, binding) {
    // 检查是否为img元素
    if (el.tagName !== 'IMG') {
      console.warn('[v-lazy] 指令只能用于 <img> 元素')
      return
    }
    
    // 设置占位图
    const placeholder = binding.arg || DEFAULT_PLACEHOLDER
    el.src = placeholder
    
    // 添加懒加载类名
    el.classList.add('lazy-image')
    
    // 如果浏览器支持 Intersection Observer
    if ('IntersectionObserver' in window) {
      createObserver(el, binding)
    } else {
      // 不支持则直接加载
      loadImage(el, binding)
    }
  },
  
  // 指令更新时
  updated(el, binding) {
    // 如果图片地址变化，重新加载
    if (binding.value !== binding.oldValue) {
      // 停止旧的观察
      const oldObserver = observerMap.get(el)
      if (oldObserver) {
        oldObserver.unobserve(el)
        observerMap.delete(el)
      }
      
      // 重置状态
      el.classList.remove('lazy-loaded', 'lazy-error', 'lazy-loading')
      
      // 重新创建观察器
      if ('IntersectionObserver' in window) {
        createObserver(el, binding)
      } else {
        loadImage(el, binding)
      }
    }
  },
  
  // 指令卸载时
  beforeUnmount(el) {
    // 清理观察器
    const observer = observerMap.get(el)
    if (observer) {
      observer.unobserve(el)
      observerMap.delete(el)
    }
  }
}

// 全局注册辅助函数
export const registerLazyDirective = (app) => {
  app.directive('lazy', lazy)
}

export default lazy

