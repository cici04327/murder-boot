<template>
  <div class="share-buttons">
    <div class="share-title">
      <el-icon><Share /></el-icon>
      <span>分享此页面</span>
    </div>
    <div class="share-actions">
      <el-tooltip content="复制链接" placement="top">
        <el-button circle @click="copyLink">
          <el-icon><Link /></el-icon>
        </el-button>
      </el-tooltip>
      
      <el-tooltip content="分享到微信" placement="top">
        <el-button circle @click="shareToWeChat">
          <el-icon><ChatDotRound /></el-icon>
        </el-button>
      </el-tooltip>
      
      <el-tooltip content="分享到微博" placement="top">
        <el-button circle @click="shareToWeibo">
          <el-icon><Position /></el-icon>
        </el-button>
      </el-tooltip>
      
      <el-tooltip content="分享到QQ" placement="top">
        <el-button circle @click="shareToQQ">
          <el-icon><ChatLineSquare /></el-icon>
        </el-button>
      </el-tooltip>
    </div>
    
    <!-- 二维码对话框 -->
    <el-dialog
      v-model="showQRCode"
      title="微信扫码分享"
      width="300px"
      center
    >
      <div class="qrcode-dialog">
        <div class="qrcode-placeholder">
          <el-icon :size="80"><Iphone /></el-icon>
        </div>
        <p>请使用微信扫描二维码</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Share,
  Link,
  ChatDotRound,
  Position,
  ChatLineSquare,
  Iphone
} from '@element-plus/icons-vue'

const showQRCode = ref(false)

// 复制链接
const copyLink = () => {
  const url = window.location.href
  
  if (navigator.clipboard) {
    navigator.clipboard.writeText(url).then(() => {
      ElMessage.success('链接已复制到剪贴板')
    }).catch(() => {
      fallbackCopyText(url)
    })
  } else {
    fallbackCopyText(url)
  }
}

// 降级复制方案
const fallbackCopyText = (text) => {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  
  try {
    document.execCommand('copy')
    ElMessage.success('链接已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败，请手动复制')
  }
  
  document.body.removeChild(textarea)
}

// 分享到微信
const shareToWeChat = () => {
  showQRCode.value = true
}

// 分享到微博
const shareToWeibo = () => {
  const url = window.location.href
  const title = document.title
  const weiboUrl = `http://service.weibo.com/share/share.php?url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}`
  window.open(weiboUrl, '_blank')
}

// 分享到QQ
const shareToQQ = () => {
  const url = window.location.href
  const title = document.title
  const qqUrl = `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}`
  window.open(qqUrl, '_blank')
}
</script>

<style scoped>
.share-buttons {
  padding: 20px;
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.98) 0%, rgba(22, 33, 62, 0.98) 100%);
  border-radius: 12px;
  color: white;
  text-align: center;
  border: 1px solid rgba(139, 0, 0, 0.3);
}

.share-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 15px;
  color: #fff;
}

.share-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
}

.share-actions .el-button {
  background: rgba(139, 0, 0, 0.3);
  border: 1px solid rgba(139, 0, 0, 0.4);
  color: #fff;
  transition: all 0.3s;
}

.share-actions .el-button:hover {
  background: #8B0000;
  border-color: #8B0000;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(139, 0, 0, 0.3);
  color: #fff;
}

.qrcode-dialog {
  text-align: center;
}

.qrcode-placeholder {
  width: 200px;
  height: 200px;
  margin: 0 auto 15px;
  background: rgba(35, 35, 60, 0.9);
  border: 2px dashed rgba(139, 0, 0, 0.4);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
}

.qrcode-dialog p {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}
</style>
