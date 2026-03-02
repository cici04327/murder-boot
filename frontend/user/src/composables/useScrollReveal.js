import { onMounted, onUnmounted } from 'vue'

export function useScrollReveal(options = {}) {
  const {
    threshold = 0.1,
    rootMargin = '0px',
    once = true
  } = options
  
  let observer = null
  
  const initObserver = () => {
    observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
          if (once) {
            observer.unobserve(entry.target)
          }
        } else if (!once) {
          entry.target.classList.remove('is-visible')
        }
      })
    }, {
      threshold,
      rootMargin
    })
    
    // 观察所有 scroll-reveal 元素
    const elements = document.querySelectorAll('.scroll-reveal')
    elements.forEach(el => observer.observe(el))
  }
  
  const cleanup = () => {
    if (observer) {
      observer.disconnect()
    }
  }
  
  onMounted(() => {
    // 延迟初始化，确保DOM已渲染
    setTimeout(initObserver, 100)
  })
  
  onUnmounted(cleanup)
  
  return {
    initObserver,
    cleanup
  }
}
