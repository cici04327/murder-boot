<template>
  <div class="settings-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>账号设置</h1>
      <p>管理您的个人信息、安全设置和偏好</p>
    </div>

    <!-- 设置导航和内容 -->
    <el-row :gutter="20">
      <!-- 左侧导航 -->
      <el-col :xs="24" :sm="6">
        <el-card class="settings-nav" shadow="never">
          <el-menu
            :default-active="activeTab"
            @select="handleTabChange"
            class="nav-menu"
          >
            <el-menu-item index="basic">
              <el-icon><User /></el-icon>
              <span>基本信息</span>
            </el-menu-item>
            <el-menu-item index="security">
              <el-icon><Lock /></el-icon>
              <span>账号安全</span>
            </el-menu-item>
            <el-menu-item index="privacy">
              <el-icon><View /></el-icon>
              <span>隐私设置</span>
            </el-menu-item>
            <el-menu-item index="notification">
              <el-icon><Bell /></el-icon>
              <span>通知设置</span>
            </el-menu-item>
            <el-menu-item index="preference">
              <el-icon><Setting /></el-icon>
              <span>偏好设置</span>
            </el-menu-item>
            <el-menu-item index="account">
              <el-icon><UserFilled /></el-icon>
              <span>账号管理</span>
            </el-menu-item>
          </el-menu>
        </el-card>
      </el-col>

      <!-- 右侧内容 -->
      <el-col :xs="24" :sm="18">
        <!-- 基本信息 -->
        <el-card v-show="activeTab === 'basic'" class="settings-content animate-fade-in">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-button type="primary" size="small" @click="handleSaveBasic" :loading="saving">
                保存修改
              </el-button>
            </div>
          </template>

          <el-form :model="basicForm" :rules="basicRules" ref="basicFormRef" label-width="100px">
            <!-- 头像 -->
            <el-form-item label="头像">
              <div class="avatar-upload-section">
                <el-avatar :size="100" :src="basicForm.avatar">
                  <el-icon size="50"><User /></el-icon>
                </el-avatar>
                <el-upload
                  class="avatar-uploader"
                  action="/api/user/upload/avatar"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :before-upload="beforeAvatarUpload"
                  accept="image/*"
                >
                  <el-button size="small" type="primary">
                    <el-icon><Camera /></el-icon>
                    更换头像
                  </el-button>
                </el-upload>
                <div class="avatar-tips">
                  <el-text size="small" type="info">建议尺寸：200x200像素，支持JPG、PNG格式，不超过2MB</el-text>
                </div>
              </div>
            </el-form-item>

            <!-- 用户名 -->
            <el-form-item label="用户名">
              <el-input v-model="basicForm.username" disabled>
                <template #append>
                  <el-tag type="info">不可修改</el-tag>
                </template>
              </el-input>
            </el-form-item>

            <!-- 昵称 -->
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="basicForm.nickname" placeholder="请输入昵称" maxlength="20" show-word-limit />
            </el-form-item>

            <!-- 性别 -->
            <el-form-item label="性别">
              <el-radio-group v-model="basicForm.gender">
                <el-radio :label="1">男</el-radio>
                <el-radio :label="2">女</el-radio>
                <el-radio :label="0">保密</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 生日 -->
            <el-form-item label="生日">
              <el-date-picker
                v-model="basicForm.birthday"
                type="date"
                placeholder="选择生日"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :disabled-date="disabledDate"
              />
            </el-form-item>

            <!-- 个人简介 -->
            <el-form-item label="个人简介">
              <el-input
                v-model="basicForm.bio"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己吧~"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 账号安全 -->
        <el-card v-show="activeTab === 'security'" class="settings-content animate-fade-in">
          <template #header>
            <span>账号安全</span>
          </template>

          <!-- 安全等级 -->
          <div class="security-level">
            <el-alert
              :title="`当前安全等级：${securityLevel.label}`"
              :type="securityLevel.type"
              :closable="false"
            >
              <template #default>
                <div class="security-progress">
                  <el-progress :percentage="securityLevel.score" :color="securityLevel.color" />
                  <p>{{ securityLevel.tip }}</p>
                </div>
              </template>
            </el-alert>
          </div>

          <!-- 安全项列表 -->
          <div class="security-items">
            <!-- 登录密码 -->
            <div class="security-item">
              <div class="item-info">
                <div class="item-icon">
                  <el-icon size="24" color="#409EFF"><Lock /></el-icon>
                </div>
                <div class="item-content">
                  <h4>登录密码</h4>
                  <p class="item-desc">安全性高的密码可以使账号更安全</p>
                  <el-tag v-if="securityInfo.hasPassword" type="success" size="small">已设置</el-tag>
                  <el-tag v-else type="danger" size="small">未设置</el-tag>
                </div>
              </div>
              <el-button @click="showPasswordDialog = true">修改</el-button>
            </div>

            <!-- 手机号 -->
            <div class="security-item">
              <div class="item-info">
                <div class="item-icon">
                  <el-icon size="24" color="#67C23A"><Phone /></el-icon>
                </div>
                <div class="item-content">
                  <h4>手机号</h4>
                  <p class="item-desc">{{ securityInfo.phone ? `已绑定：${maskPhone(securityInfo.phone)}` : '未绑定' }}</p>
                </div>
              </div>
              <el-button @click="showPhoneDialog = true">{{ securityInfo.phone ? '更换' : '绑定' }}</el-button>
            </div>

            <!-- 邮箱 -->
            <div class="security-item">
              <div class="item-info">
                <div class="item-icon">
                  <el-icon size="24" color="#E6A23C"><Message /></el-icon>
                </div>
                <div class="item-content">
                  <h4>邮箱</h4>
                  <p class="item-desc">{{ securityInfo.email ? `已绑定：${maskEmail(securityInfo.email)}` : '未绑定' }}</p>
                </div>
              </div>
              <el-button @click="showEmailDialog = true">{{ securityInfo.email ? '更换' : '绑定' }}</el-button>
            </div>

            <!-- 实名认证 -->
            <div class="security-item">
              <div class="item-info">
                <div class="item-icon">
                  <el-icon size="24" color="#F56C6C"><CreditCard /></el-icon>
                </div>
                <div class="item-content">
                  <h4>实名认证</h4>
                  <p class="item-desc">{{ securityInfo.realNameVerified ? '已认证' : '未认证，认证后享受更多特权' }}</p>
                </div>
              </div>
              <el-button :disabled="securityInfo.realNameVerified" @click="showRealNameDialog = true">
                {{ securityInfo.realNameVerified ? '已认证' : '去认证' }}
              </el-button>
            </div>

            <!-- 登录日志 -->
            <div class="security-item">
              <div class="item-info">
                <div class="item-icon">
                  <el-icon size="24" color="#909399"><Document /></el-icon>
                </div>
                <div class="item-content">
                  <h4>登录日志</h4>
                  <p class="item-desc">查看最近的登录记录</p>
                </div>
              </div>
              <el-button @click="showLoginLogDialog = true">查看</el-button>
            </div>
          </div>
        </el-card>

        <!-- 隐私设置 -->
        <el-card v-show="activeTab === 'privacy'" class="settings-content animate-fade-in">
          <template #header>
            <span>隐私设置</span>
          </template>

          <div class="privacy-settings">
            <div class="privacy-item">
              <div class="privacy-info">
                <h4>公开我的收藏</h4>
                <p>允许其他用户查看我收藏的剧本</p>
              </div>
              <el-switch v-model="privacySettings.showFavorites" @change="handlePrivacyChange" />
            </div>

            <div class="privacy-item">
              <div class="privacy-info">
                <h4>公开我的预约</h4>
                <p>允许其他用户查看我的预约记录</p>
              </div>
              <el-switch v-model="privacySettings.showReservations" @change="handlePrivacyChange" />
            </div>

            <div class="privacy-item">
              <div class="privacy-info">
                <h4>允许陌生人查看主页</h4>
                <p>关闭后只有好友可以访问你的主页</p>
              </div>
              <el-switch v-model="privacySettings.allowStrangerVisit" @change="handlePrivacyChange" />
            </div>

            <div class="privacy-item">
              <div class="privacy-info">
                <h4>搜索引擎收录</h4>
                <p>允许搜索引擎收录我的公开信息</p>
              </div>
              <el-switch v-model="privacySettings.allowSearchEngine" @change="handlePrivacyChange" />
            </div>

            <div class="privacy-item">
              <div class="privacy-info">
                <h4>个性化推荐</h4>
                <p>根据我的浏览和收藏记录推荐内容</p>
              </div>
              <el-switch v-model="privacySettings.allowRecommendation" @change="handlePrivacyChange" />
            </div>
          </div>
        </el-card>

        <!-- 通知设置 -->
        <el-card v-show="activeTab === 'notification'" class="settings-content animate-fade-in">
          <template #header>
            <span>通知设置</span>
          </template>

          <div class="notification-settings">
            <el-alert
              title="消息通知设置"
              type="info"
              :closable="false"
              style="margin-bottom: 20px;"
            >
              选择您希望接收的通知类型
            </el-alert>

            <!-- 系统消息 -->
            <div class="notification-group">
              <h4>系统消息</h4>
              <div class="notification-item">
                <div class="notification-info">
                  <span>预约提醒</span>
                  <p>预约即将开始时提醒您</p>
                </div>
                <el-switch v-model="notificationSettings.reservationReminder" @change="handleNotificationChange" />
              </div>
              <div class="notification-item">
                <div class="notification-info">
                  <span>优惠券通知</span>
                  <p>新优惠券到账和即将过期提醒</p>
                </div>
                <el-switch v-model="notificationSettings.couponNotice" @change="handleNotificationChange" />
              </div>
              <div class="notification-item">
                <div class="notification-info">
                  <span>积分变动</span>
                  <p>积分增加或减少时通知您</p>
                </div>
                <el-switch v-model="notificationSettings.pointsChange" @change="handleNotificationChange" />
              </div>
            </div>

            <!-- 营销消息 -->
            <div class="notification-group">
              <h4>营销消息</h4>
              <div class="notification-item">
                <div class="notification-info">
                  <span>活动推广</span>
                  <p>接收平台活动和优惠信息</p>
                </div>
                <el-switch v-model="notificationSettings.promotionActivity" @change="handleNotificationChange" />
              </div>
              <div class="notification-item">
                <div class="notification-info">
                  <span>新剧本推荐</span>
                  <p>新上线的剧本推荐</p>
                </div>
                <el-switch v-model="notificationSettings.newScriptRecommend" @change="handleNotificationChange" />
              </div>
            </div>

            <!-- 通知方式 -->
            <div class="notification-group">
              <h4>通知方式</h4>
              <el-checkbox-group v-model="notificationSettings.methods" @change="handleNotificationChange">
                <el-checkbox label="站内信">站内信</el-checkbox>
                <el-checkbox label="短信">短信</el-checkbox>
                <el-checkbox label="邮件">邮件</el-checkbox>
                <el-checkbox label="微信">微信</el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-card>

        <!-- 偏好设置 -->
        <el-card v-show="activeTab === 'preference'" class="settings-content animate-fade-in">
          <template #header>
            <span>偏好设置</span>
          </template>

          <el-form label-width="120px">
            <!-- 语言设置 -->
            <el-form-item label="语言">
              <el-select v-model="preferenceSettings.language" @change="handlePreferenceChange">
                <el-option label="简体中文" value="zh-CN" />
                <el-option label="繁體中文" value="zh-TW" />
                <el-option label="English" value="en-US" />
              </el-select>
            </el-form-item>

            <!-- 主题设置 -->
            <el-form-item label="主题模式">
              <el-radio-group v-model="preferenceSettings.theme" @change="handlePreferenceChange">
                <el-radio label="light">明亮模式</el-radio>
                <el-radio label="dark">深色模式</el-radio>
                <el-radio label="auto">跟随系统</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 默认页面 -->
            <el-form-item label="默认首页">
              <el-select v-model="preferenceSettings.defaultPage" @change="handlePreferenceChange">
                <el-option label="推荐首页" value="/home" />
                <el-option label="剧本大厅" value="/script" />
                <el-option label="门店列表" value="/store" />
                <el-option label="我的预约" value="/user/reservations" />
              </el-select>
            </el-form-item>

            <!-- 列表显示 -->
            <el-form-item label="列表显示">
              <el-radio-group v-model="preferenceSettings.listView" @change="handlePreferenceChange">
                <el-radio label="grid">网格视图</el-radio>
                <el-radio label="list">列表视图</el-radio>
              </el-radio-group>
            </el-form-item>

            <!-- 每页数量 -->
            <el-form-item label="每页显示">
              <el-select v-model="preferenceSettings.pageSize" @change="handlePreferenceChange">
                <el-option :label="`12条/页`" :value="12" />
                <el-option :label="`24条/页`" :value="24" />
                <el-option :label="`48条/页`" :value="48" />
              </el-select>
            </el-form-item>

            <!-- 自动播放 -->
            <el-form-item label="视频自动播放">
              <el-switch v-model="preferenceSettings.autoPlayVideo" @change="handlePreferenceChange" />
              <el-text size="small" type="info" style="margin-left: 10px;">
                在WiFi环境下自动播放视频
              </el-text>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 账号管理 -->
        <el-card v-show="activeTab === 'account'" class="settings-content animate-fade-in">
          <template #header>
            <span>账号管理</span>
          </template>

          <div class="account-management">
            <!-- 账号信息 -->
            <div class="account-info-section">
              <h4>账号信息</h4>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="用户ID">{{ accountInfo.userId }}</el-descriptions-item>
                <el-descriptions-item label="注册时间">{{ accountInfo.createTime }}</el-descriptions-item>
                <el-descriptions-item label="账号状态">
                  <el-tag :type="accountInfo.status === '正常' ? 'success' : 'danger'">
                    {{ accountInfo.status }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="VIP等级">
                  <el-tag v-if="accountInfo.vipLevel > 0" type="warning">
                    VIP{{ accountInfo.vipLevel }}
                  </el-tag>
                  <el-text v-else type="info">普通用户</el-text>
                </el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 数据统计 -->
            <div class="account-stats-section">
              <h4>数据统计</h4>
              <el-row :gutter="20">
                <el-col :span="6">
                  <el-statistic title="累计预约" :value="accountStats.totalReservations" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="累计消费" :value="accountStats.totalSpent" prefix="¥" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="累计积分" :value="accountStats.totalPoints" />
                </el-col>
                <el-col :span="6">
                  <el-statistic title="好友数量" :value="accountStats.friendCount" />
                </el-col>
              </el-row>
            </div>

            <!-- 危险操作 -->
            <div class="danger-zone">
              <h4>危险操作</h4>
              <el-alert
                title="以下操作不可逆，请谨慎操作"
                type="warning"
                :closable="false"
                style="margin-bottom: 20px;"
              />
              
              <div class="danger-actions">
                <div class="danger-item">
                  <div class="danger-info">
                    <h5>清空浏览历史</h5>
                    <p>清空所有浏览记录和缓存</p>
                  </div>
                  <el-button type="warning" plain @click="handleClearHistory">清空历史</el-button>
                </div>

                <div class="danger-item">
                  <div class="danger-info">
                    <h5>注销账号</h5>
                    <p>注销后账号将被永久删除，所有数据将无法恢复</p>
                  </div>
                  <el-button type="danger" plain @click="handleDeactivateAccount">注销账号</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 修改密码对话框 -->
    <el-dialog v-model="showPasswordDialog" title="修改密码" width="500px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
          <div class="password-strength">
            <span>密码强度：</span>
            <el-progress
              :percentage="passwordStrength.percentage"
              :color="passwordStrength.color"
              :show-text="false"
              style="width: 200px;"
            />
            <el-text :type="passwordStrength.type" size="small">{{ passwordStrength.label }}</el-text>
          </div>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword" :loading="saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定/更换手机号对话框 -->
    <el-dialog v-model="showPhoneDialog" :title="securityInfo.phone ? '更换手机号' : '绑定手机号'" width="500px">
      <el-form :model="phoneForm" :rules="phoneRules" ref="phoneFormRef" label-width="100px">
        <el-form-item v-if="securityInfo.phone" label="原手机号">
          <el-text>{{ maskPhone(securityInfo.phone) }}</el-text>
        </el-form-item>
        <el-form-item label="新手机号" prop="phone">
          <el-input v-model="phoneForm.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="phoneForm.code" placeholder="请输入验证码" maxlength="6">
            <template #append>
              <el-button :disabled="countdown > 0" @click="handleSendPhoneCode">
                {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPhoneDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePhone" :loading="saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定/更换邮箱对话框 -->
    <el-dialog v-model="showEmailDialog" :title="securityInfo.email ? '更换邮箱' : '绑定邮箱'" width="500px">
      <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-width="100px">
        <el-form-item v-if="securityInfo.email" label="原邮箱">
          <el-text>{{ maskEmail(securityInfo.email) }}</el-text>
        </el-form-item>
        <el-form-item label="新邮箱" prop="email">
          <el-input v-model="emailForm.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <el-input v-model="emailForm.code" placeholder="请输入验证码" maxlength="6">
            <template #append>
              <el-button :disabled="countdown > 0" @click="handleSendEmailCode">
                {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateEmail" :loading="saving">确定</el-button>
      </template>
    </el-dialog>

    <!-- 实名认证对话框 -->
    <el-dialog v-model="showRealNameDialog" title="实名认证" width="500px">
      <el-form :model="realNameForm" :rules="realNameRules" ref="realNameFormRef" label-width="100px">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="realNameForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="realNameForm.idCard" placeholder="请输入身份证号" maxlength="18" />
        </el-form-item>
        <el-alert
          title="温馨提示"
          type="info"
          :closable="false"
          style="margin-top: 20px;"
        >
          <ul style="margin: 10px 0; padding-left: 20px;">
            <li>实名信息仅用于身份验证，不会公开</li>
            <li>认证后无法修改，请确保信息准确</li>
            <li>认证成功后可享受更多特权</li>
          </ul>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="showRealNameDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRealNameVerify" :loading="saving">提交认证</el-button>
      </template>
    </el-dialog>

    <!-- 登录日志对话框 -->
    <el-dialog v-model="showLoginLogDialog" title="登录日志" width="800px">
      <el-table :data="loginLogs" style="width: 100%">
        <el-table-column prop="time" label="登录时间" width="180" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="location" label="登录地点" />
        <el-table-column prop="device" label="设备" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '成功' ? 'success' : 'danger'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-if="loginLogTotal > 10"
        v-model:current-page="loginLogPage"
        v-model:page-size="loginLogPageSize"
        :total="loginLogTotal"
        layout="prev, pager, next"
        style="margin-top: 20px; justify-content: center;"
        @current-change="loadLoginLogs"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, Lock, View, Bell, Setting, UserFilled, Camera, Phone,
  Message, CreditCard, Document
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 当前激活的标签页
const activeTab = ref('basic')
const saving = ref(false)

// ========== 基本信息 ==========
const basicFormRef = ref(null)
const basicForm = reactive({
  avatar: '',
  username: '',
  nickname: '',
  gender: 0,
  birthday: '',
  bio: ''
})

const basicRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在2-20个字符', trigger: 'blur' }
  ]
}

// ========== 账号安全 ==========
const securityInfo = ref({
  hasPassword: true,
  phone: '',
  email: '',
  realNameVerified: false
})

// 安全等级计算
const securityLevel = computed(() => {
  let score = 0
  if (securityInfo.value.hasPassword) score += 25
  if (securityInfo.value.phone) score += 25
  if (securityInfo.value.email) score += 25
  if (securityInfo.value.realNameVerified) score += 25

  if (score >= 75) {
    return { score, label: '高', type: 'success', color: '#67C23A', tip: '您的账号安全等级很高' }
  } else if (score >= 50) {
    return { score, label: '中', type: 'warning', color: '#E6A23C', tip: '建议完善更多安全设置' }
  } else {
    return { score, label: '低', type: 'danger', color: '#F56C6C', tip: '您的账号安全等级较低，请尽快完善' }
  }
})

// 修改密码
const showPasswordDialog = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6-20个字符', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{6,}$/,
      message: '密码必须包含大小写字母和数字',
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 密码强度
const passwordStrength = computed(() => {
  const password = passwordForm.newPassword
  if (!password) return { percentage: 0, color: '#909399', type: 'info', label: '无' }

  let strength = 0
  if (password.length >= 6) strength += 20
  if (password.length >= 8) strength += 20
  if (/[a-z]/.test(password)) strength += 20
  if (/[A-Z]/.test(password)) strength += 20
  if (/\d/.test(password)) strength += 20

  if (strength >= 80) {
    return { percentage: strength, color: '#67C23A', type: 'success', label: '强' }
  } else if (strength >= 60) {
    return { percentage: strength, color: '#E6A23C', type: 'warning', label: '中' }
  } else {
    return { percentage: strength, color: '#F56C6C', type: 'danger', label: '弱' }
  }
})

// 绑定/更换手机号
const showPhoneDialog = ref(false)
const phoneFormRef = ref(null)
const phoneForm = reactive({
  phone: '',
  code: ''
})

const phoneRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 绑定/更换邮箱
const showEmailDialog = ref(false)
const emailFormRef = ref(null)
const emailForm = reactive({
  email: '',
  code: ''
})

const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

// 实名认证
const showRealNameDialog = ref(false)
const realNameFormRef = ref(null)
const realNameForm = reactive({
  realName: '',
  idCard: ''
})

const realNameRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在2-20个字符', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    {
      pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/,
      message: '请输入正确的身份证号',
      trigger: 'blur'
    }
  ]
}

// 登录日志
const showLoginLogDialog = ref(false)
const loginLogs = ref([])
const loginLogPage = ref(1)
const loginLogPageSize = ref(10)
const loginLogTotal = ref(0)

// 验证码倒计时
const countdown = ref(0)

// ========== 隐私设置 ==========
const privacySettings = reactive({
  showFavorites: true,
  showReservations: true,
  allowStrangerVisit: true,
  allowSearchEngine: true,
  allowRecommendation: true
})

// ========== 通知设置 ==========
const notificationSettings = reactive({
  reservationReminder: true,
  couponNotice: true,
  pointsChange: true,
  promotionActivity: false,
  newScriptRecommend: true,
  methods: ['站内信', '短信']
})

// ========== 偏好设置 ==========
const preferenceSettings = reactive({
  language: 'zh-CN',
  theme: 'light',
  defaultPage: '/home',
  listView: 'grid',
  pageSize: 12,
  autoPlayVideo: false
})

// ========== 账号管理 ==========
const accountInfo = ref({
  userId: '',
  createTime: '',
  status: '正常',
  vipLevel: 0
})

const accountStats = ref({
  totalReservations: 0,
  totalSpent: 0,
  totalPoints: 0,
  friendCount: 0
})

// ========== 方法 ==========

// 标签页切换
const handleTabChange = (key) => {
  activeTab.value = key
}

// 禁用未来日期
const disabledDate = (time) => {
  return time.getTime() > Date.now()
}

// 保存基本信息
const handleSaveBasic = async () => {
  try {
    await basicFormRef.value.validate()
    saving.value = true

    // TODO: 调用API保存基本信息
    await new Promise(resolve => setTimeout(resolve, 1000))

    ElMessage.success('保存成功')
  } catch (error) {
    console.error('保存失败:', error)
  } finally {
    saving.value = false
  }
}

// 头像上传前验证
const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB!')
    return false
  }
  return true
}

// 头像上传成功
const handleAvatarSuccess = (response) => {
  if (response.code === 200) {
    basicForm.avatar = response.data.url
    ElMessage.success('头像上传成功')
  }
}

// 修改密码
const handleUpdatePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    saving.value = true

    // TODO: 调用API修改密码
    await new Promise(resolve => setTimeout(resolve, 1000))

    ElMessage.success('密码修改成功，请重新登录')
    showPasswordDialog.value = false
    
    // 重置表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''

    // 3秒后退出登录
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 3000)
  } catch (error) {
    console.error('修改密码失败:', error)
  } finally {
    saving.value = false
  }
}

// 发送手机验证码
const handleSendPhoneCode = async () => {
  if (!phoneForm.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  if (!/^1[3-9]\d{9}$/.test(phoneForm.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }

  try {
    // TODO: 调用API发送验证码
    await new Promise(resolve => setTimeout(resolve, 500))
    
    ElMessage.success('验证码已发送')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    ElMessage.error('发送失败，请稍后重试')
  }
}

// 发送邮箱验证码
const handleSendEmailCode = async () => {
  if (!emailForm.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailForm.email)) {
    ElMessage.warning('请输入正确的邮箱地址')
    return
  }

  try {
    // TODO: 调用API发送验证码
    await new Promise(resolve => setTimeout(resolve, 500))
    
    ElMessage.success('验证码已发送到您的邮箱')
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
      }
    }, 1000)
  } catch (error) {
    ElMessage.error('发送失败，请稍后重试')
  }
}

// 更新手机号
const handleUpdatePhone = async () => {
  try {
    await phoneFormRef.value.validate()
    saving.value = true

    // TODO: 调用API更新手机号
    await new Promise(resolve => setTimeout(resolve, 1000))

    securityInfo.value.phone = phoneForm.phone
    ElMessage.success('手机号绑定成功')
    showPhoneDialog.value = false
    
    phoneForm.phone = ''
    phoneForm.code = ''
  } catch (error) {
    console.error('绑定失败:', error)
  } finally {
    saving.value = false
  }
}

// 更新邮箱
const handleUpdateEmail = async () => {
  try {
    await emailFormRef.value.validate()
    saving.value = true

    // TODO: 调用API更新邮箱
    await new Promise(resolve => setTimeout(resolve, 1000))

    securityInfo.value.email = emailForm.email
    ElMessage.success('邮箱绑定成功')
    showEmailDialog.value = false
    
    emailForm.email = ''
    emailForm.code = ''
  } catch (error) {
    console.error('绑定失败:', error)
  } finally {
    saving.value = false
  }
}

// 实名认证
const handleRealNameVerify = async () => {
  try {
    await realNameFormRef.value.validate()
    saving.value = true

    // TODO: 调用API提交实名认证
    await new Promise(resolve => setTimeout(resolve, 1500))

    securityInfo.value.realNameVerified = true
    ElMessage.success('实名认证提交成功，请等待审核')
    showRealNameDialog.value = false
    
    realNameForm.realName = ''
    realNameForm.idCard = ''
  } catch (error) {
    console.error('认证失败:', error)
  } finally {
    saving.value = false
  }
}

// 加载登录日志
const loadLoginLogs = async () => {
  try {
    // TODO: 调用API加载登录日志
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 模拟数据
    loginLogs.value = [
      {
        time: '2025-11-03 14:30:25',
        ip: '192.168.1.100',
        location: '北京市朝阳区',
        device: 'Windows Chrome',
        status: '成功'
      },
      {
        time: '2025-11-02 10:15:10',
        ip: '192.168.1.100',
        location: '北京市朝阳区',
        device: 'Windows Chrome',
        status: '成功'
      }
    ]
    loginLogTotal.value = 15
  } catch (error) {
    console.error('加载登录日志失败:', error)
  }
}

// 隐私设置变更
const handlePrivacyChange = () => {
  // TODO: 调用API保存隐私设置
  ElMessage.success('设置已保存')
}

// 通知设置变更
const handleNotificationChange = () => {
  // TODO: 调用API保存通知设置
  ElMessage.success('设置已保存')
}

// 偏好设置变更
const handlePreferenceChange = () => {
  // TODO: 调用API保存偏好设置
  ElMessage.success('设置已保存')
}

// 清空浏览历史
const handleClearHistory = () => {
  ElMessageBox.confirm('确定要清空所有浏览历史吗？此操作不可撤销', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    // TODO: 调用API清空历史
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('浏览历史已清空')
  }).catch(() => {})
}

// 注销账号
const handleDeactivateAccount = () => {
  ElMessageBox.prompt('注销账号后所有数据将被永久删除且无法恢复，请输入"确认注销"以继续', '注销账号', {
    confirmButtonText: '确定注销',
    cancelButtonText: '取消',
    type: 'error',
    inputPattern: /^确认注销$/,
    inputErrorMessage: '请输入"确认注销"'
  }).then(async ({ value }) => {
    // TODO: 调用API注销账号
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('账号已注销')
    
    setTimeout(() => {
      userStore.logout()
      router.push('/login')
    }, 1500)
  }).catch(() => {})
}

// 手机号掩码
const maskPhone = (phone) => {
  if (!phone) return ''
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2')
}

// 邮箱掩码
const maskEmail = (email) => {
  if (!email) return ''
  const [name, domain] = email.split('@')
  return `${name.slice(0, 2)}****@${domain}`
}

// 加载用户数据
const loadUserData = () => {
  const user = userStore.userInfo
  if (user) {
    // 基本信息
    basicForm.avatar = user.avatar || ''
    basicForm.username = user.username || user.name || ''
    basicForm.nickname = user.nickname || user.username || ''
    basicForm.gender = user.gender || 0
    basicForm.birthday = user.birthday || ''
    basicForm.bio = user.bio || ''

    // 安全信息
    securityInfo.value.phone = user.phone || ''
    securityInfo.value.email = user.email || ''
    securityInfo.value.realNameVerified = user.realNameVerified || false

    // 账号信息
    accountInfo.value.userId = user.id || ''
    accountInfo.value.createTime = user.createTime ? new Date(user.createTime).toLocaleDateString() : ''
    accountInfo.value.vipLevel = user.vipLevel || 0
  }
}

onMounted(() => {
  loadUserData()
})
</script>

<style scoped>
.settings-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h1 {
  margin: 0 0 10px 0;
  font-size: 28px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

/* 导航菜单 */
.settings-nav {
  position: sticky;
  top: 20px;
}

.nav-menu {
  border: none;
}

.nav-menu .el-menu-item {
  border-radius: 8px;
  margin-bottom: 5px;
}

/* 内容卡片 */
.settings-content {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

/* 头像上传 */
.avatar-upload-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 15px;
}

.avatar-tips {
  margin-top: 5px;
}

/* 安全等级 */
.security-level {
  margin-bottom: 30px;
}

.security-progress {
  margin-top: 15px;
}

.security-progress p {
  margin: 10px 0 0 0;
  color: #606266;
}

/* 安全项 */
.security-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.security-item:hover {
  background: #fff;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}

.item-info {
  display: flex;
  align-items: center;
  gap: 15px;
  flex: 1;
}

.item-icon {
  flex-shrink: 0;
}

.item-content h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #303133;
}

.item-desc {
  margin: 5px 0;
  font-size: 13px;
  color: #909399;
}

/* 密码强度 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 13px;
}

/* 隐私设置 */
.privacy-settings,
.notification-settings {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.privacy-item,
.notification-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
}

.privacy-info h4,
.notification-info span {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #303133;
}

.privacy-info p,
.notification-info p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* 通知分组 */
.notification-group {
  margin-bottom: 30px;
}

.notification-group h4 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #303133;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

/* 账号管理 */
.account-management > div {
  margin-bottom: 30px;
}

.account-management h4 {
  margin: 0 0 20px 0;
  font-size: 16px;
  color: #303133;
}

/* 危险区域 */
.danger-zone {
  margin-top: 40px;
  padding: 20px;
  border: 1px solid #f56c6c;
  border-radius: 8px;
  background: #fef0f0;
}

.danger-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.danger-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
}

.danger-info h5 {
  margin: 0 0 5px 0;
  font-size: 15px;
  color: #303133;
}

.danger-info p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* 响应式 */
@media (max-width: 768px) {
  .settings-nav {
    position: static;
    margin-bottom: 20px;
  }

  .security-item,
  .privacy-item,
  .notification-item,
  .danger-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .item-info {
    width: 100%;
  }
}
</style>

