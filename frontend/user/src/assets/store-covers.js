/**
 * 门店封面图片映射
 * 使用高质量的 Unsplash 图片
 */

// 门店封面图片集合（剧本杀主题）
export const STORE_COVERS = {
  // 按门店名称匹配
  '探案密室': 'https://images.unsplash.com/photo-1593642532454-e138e28a63f4?q=80&w=1200&auto=format&fit=crop', // 神秘的密室
  '时空剧本馆': 'https://images.unsplash.com/photo-1481277542470-605612bd2d61?q=80&w=1200&auto=format&fit=crop', // 书架图书馆
  '沉浸式体验馆': 'https://images.unsplash.com/photo-1524758631624-e2822e304c36?q=80&w=1200&auto=format&fit=crop', // 豪华大厅
  '推理殿堂': 'https://images.unsplash.com/photo-1495364141860-b0d03eccd065?q=80&w=1200&auto=format&fit=crop', // 书本咖啡
  '剧本杀公馆': 'https://images.unsplash.com/photo-1556909212-d5b604d0c90d?q=80&w=1200&auto=format&fit=crop', // 欧式客厅
  '谜境体验店': 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=1200&auto=format&fit=crop', // 餐厅包间
  '万达广场店': 'https://images.unsplash.com/photo-1497366216548-37526070297c?q=80&w=1200&auto=format&fit=crop', // 现代商场
  '大悦城店': 'https://images.unsplash.com/photo-1555636222-cae831e670b3?q=80&w=1200&auto=format&fit=crop', // 现代办公室
  '银泰城店': 'https://images.unsplash.com/photo-1497366811353-6870744d04b2?q=80&w=1200&auto=format&fit=crop', // 开放办公
  '星光天地店': 'https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?q=80&w=1200&auto=format&fit=crop', // 现代建筑
  
  // 按门店类型匹配
  密室: 'https://images.unsplash.com/photo-1593642532454-e138e28a63f4?q=80&w=1200&auto=format&fit=crop',
  剧本馆: 'https://images.unsplash.com/photo-1481277542470-605612bd2d61?q=80&w=1200&auto=format&fit=crop',
  体验馆: 'https://images.unsplash.com/photo-1524758631624-e2822e304c36?q=80&w=1200&auto=format&fit=crop',
  推理馆: 'https://images.unsplash.com/photo-1495364141860-b0d03eccd065?q=80&w=1200&auto=format&fit=crop',
  公馆: 'https://images.unsplash.com/photo-1556909212-d5b604d0c90d?q=80&w=1200&auto=format&fit=crop',
  
  // 默认图片（如果都不匹配）
  'default': 'https://images.unsplash.com/photo-1542744173-8e7e53415bb0?q=80&w=1200&auto=format&fit=crop' // 现代会议室
}

/**
 * 获取门店封面图片
 * @param {string} storeName - 门店名称
 * @returns {string} 封面图片URL
 */
export function getStoreCover(storeName) {
  if (!storeName) {
    return STORE_COVERS['default']
  }
  
  // 1. 精确匹配门店名称
  if (STORE_COVERS[storeName]) {
    return STORE_COVERS[storeName]
  }
  
  // 2. 模糊匹配门店类型
  for (const [key, value] of Object.entries(STORE_COVERS)) {
    if (key !== 'default' && storeName.includes(key)) {
      return value
    }
  }
  
  // 3. 返回默认图片
  return STORE_COVERS['default']
}

/**
 * 门店封面图片集合（6个精选门店）
 * 可以直接在页面中使用
 */
export const FEATURED_STORES_COVERS = [
  {
    id: 1,
    name: '探案密室',
    cover: 'https://images.unsplash.com/photo-1593642532454-e138e28a63f4?q=80&w=1200&auto=format&fit=crop',
    description: '神秘氛围，精致布景'
  },
  {
    id: 2,
    name: '时空剧本馆',
    cover: 'https://images.unsplash.com/photo-1481277542470-605612bd2d61?q=80&w=1200&auto=format&fit=crop',
    description: '剧本丰富，环境优雅'
  },
  {
    id: 3,
    name: '沉浸式体验馆',
    cover: 'https://images.unsplash.com/photo-1524758631624-e2822e304c36?q=80&w=1200&auto=format&fit=crop',
    description: '豪华装修，沉浸体验'
  },
  {
    id: 4,
    name: '推理殿堂',
    cover: 'https://images.unsplash.com/photo-1495364141860-b0d03eccd065?q=80&w=1200&auto=format&fit=crop',
    description: '书香氛围，推理专场'
  },
  {
    id: 5,
    name: '剧本杀公馆',
    cover: 'https://images.unsplash.com/photo-1556909212-d5b604d0c90d?q=80&w=1200&auto=format&fit=crop',
    description: '欧式风格，高端品质'
  },
  {
    id: 6,
    name: '谜境体验店',
    cover: 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?q=80&w=1200&auto=format&fit=crop',
    description: '包间齐全，服务周到'
  }
]

// 导出默认对象
export default {
  STORE_COVERS,
  getStoreCover,
  FEATURED_STORES_COVERS
}

