/**
 * 地理位置相关工具函数
 */

const LOCATION_STORAGE_KEY = 'user_location'
const ACTIVE_LOCATION_CACHE_MS = 5 * 60 * 1000
const PASSIVE_LOCATION_CACHE_MS = 2 * 60 * 60 * 1000

function readCachedLocation(maxAgeMs = ACTIVE_LOCATION_CACHE_MS) {
  const cached = localStorage.getItem(LOCATION_STORAGE_KEY)
  if (!cached) {
    return null
  }

  try {
    const { location, timestamp } = JSON.parse(cached)
    if (!location || typeof timestamp !== 'number') {
      return null
    }

    const age = Date.now() - timestamp
    if (age > maxAgeMs) {
      return null
    }

    return location
  } catch (error) {
    console.error('解析缓存位置信息失败:', error)
    return null
  }
}

/**
 * 获取用户当前位置
 * @returns {Promise<{latitude: number, longitude: number}>}
 */
export function getUserLocation(forceRequest = false) {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持地理定位'))
      return
    }

    // 如果强制请求，跳过缓存检查
    if (!forceRequest) {
      const cachedLocation = readCachedLocation()
      if (cachedLocation) {
        console.log('使用缓存的位置信息')
        resolve(cachedLocation)
        return
      }
    }

    console.log('请求浏览器位置权限...')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const location = {
          latitude: position.coords.latitude,
          longitude: position.coords.longitude
        }
        
        // 缓存位置信息
        localStorage.setItem(LOCATION_STORAGE_KEY, JSON.stringify({
          location,
          timestamp: Date.now()
        }))
        
        console.log('位置获取成功:', location)
        resolve(location)
      },
      (error) => {
        console.error('获取位置失败:', error)
        reject(error)
      },
      {
        enableHighAccuracy: false,
        timeout: 10000,
        maximumAge: 0 // 不使用浏览器缓存，强制获取新位置
      }
    )
  })
}

/**
 * 读取最近缓存的位置，不主动触发浏览器定位。
 * 适合首页、列表页这类只想稳定显示距离的场景。
 */
export function getCachedUserLocation(maxAgeMs = PASSIVE_LOCATION_CACHE_MS) {
  return readCachedLocation(maxAgeMs)
}

/**
 * 计算两个坐标点之间的距离（Haversine公式）
 * @param {number} lat1 - 起点纬度
 * @param {number} lon1 - 起点经度
 * @param {number} lat2 - 终点纬度
 * @param {number} lon2 - 终点经度
 * @returns {number} 距离（单位：公里）
 */
export function calculateDistance(lat1, lon1, lat2, lon2) {
  const R = 6371 // 地球半径（公里）
  const dLat = toRadians(lat2 - lat1)
  const dLon = toRadians(lon2 - lon1)
  
  const a = 
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) * Math.cos(toRadians(lat2)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2)
  
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  const distance = R * c
  
  return distance
}

/**
 * 角度转弧度
 */
function toRadians(degrees) {
  return degrees * (Math.PI / 180)
}

/**
 * 格式化距离显示
 * @param {number} distance - 距离（公里）
 * @returns {string} 格式化后的距离字符串
 */
export function formatDistance(distance) {
  if (!distance || distance < 0) {
    return '未知'
  }
  
  if (distance < 1) {
    // 小于1公里，显示米
    return `${Math.round(distance * 1000)}m`
  } else if (distance < 10) {
    // 1-10公里，保留1位小数
    return `${distance.toFixed(1)}km`
  } else {
    // 大于10公里，取整
    return `${Math.round(distance)}km`
  }
}

/**
 * 根据地址解析经纬度（使用高德地图API）
 * 注意：实际使用时需要申请高德地图API密钥
 * @param {string} address - 地址
 * @returns {Promise<{latitude: number, longitude: number}>}
 */
export async function geocodeAddress(address) {
  // 这里可以集成高德地图、百度地图等地图API
  // 示例：使用高德地图Web服务API
  const AMAP_KEY = 'your_amap_key_here' // 需要替换为实际的API密钥
  
  try {
    const response = await fetch(
      `https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(address)}&key=${AMAP_KEY}`
    )
    const data = await response.json()
    
    if (data.status === '1' && data.geocodes.length > 0) {
      const [lon, lat] = data.geocodes[0].location.split(',')
      return {
        latitude: parseFloat(lat),
        longitude: parseFloat(lon)
      }
    }
    
    throw new Error('地址解析失败')
  } catch (error) {
    console.error('地址解析失败:', error)
    // 返回默认坐标（北京天安门）
    return {
      latitude: 39.908815,
      longitude: 116.397529
    }
  }
}

/**
 * 检查浏览器是否支持地理定位
 * @returns {boolean}
 */
export function isGeolocationSupported() {
  return 'geolocation' in navigator
}

/**
 * 请求地理位置权限
 * @returns {Promise<string>} 权限状态：'granted', 'denied', 'prompt'
 */
export async function requestLocationPermission() {
  if (!navigator.permissions) {
    return 'prompt'
  }
  
  try {
    const result = await navigator.permissions.query({ name: 'geolocation' })
    return result.state
  } catch (error) {
    console.error('查询位置权限失败:', error)
    return 'prompt'
  }
}

/**
 * 计算距离并格式化显示
 * @param {Object} userLocation - 用户位置 {latitude, longitude}
 * @param {Object} targetLocation - 目标位置 {latitude, longitude}
 * @returns {string} 格式化的距离字符串
 */
export function getDistanceText(userLocation, targetLocation) {
  if (!userLocation || !targetLocation) {
    return '位置未知'
  }
  
  const distance = calculateDistance(
    userLocation.latitude,
    userLocation.longitude,
    targetLocation.latitude,
    targetLocation.longitude
  )
  
  return formatDistance(distance)
}

