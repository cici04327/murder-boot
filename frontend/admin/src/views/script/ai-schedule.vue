<template>
  <div class="ai-dm-schedule">
    <div class="page-header">
      <h2>AI智能DM分配</h2>
      <p class="subtitle">基于规则引擎+AI，为排期自动推荐最合适的DM主持人</p>
    </div>

    <el-card class="filter-card">
      <el-form :model="form" inline>
        <el-form-item label="门店" v-if="!isStoreAdmin">
          <el-select v-model="form.storeId" placeholder="请选择门店" style="width:180px">
            <el-option v-for="s in storeList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="仅未分配">
          <el-switch v-model="form.onlyUnassigned" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSuggest">
            <el-icon><MagicStick /></el-icon>&nbsp;AI智能推荐
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result" class="summary-card">
      <div class="summary-header">
        <el-icon color="#409EFF" size="20"><ChatDotRound /></el-icon>
        <span class="summary-title">AI分析报告</span>
        <div class="stats">
          <el-tag type="info">共 {{ result.totalSchedules }} 场</el-tag>
          <el-tag type="success">已推荐 {{ result.assignedCount }} 场</el-tag>
          <el-tag v-if="result.unassignedCount > 0" type="warning">待处理 {{ result.unassignedCount }} 场</el-tag>
        </div>
      </div>
      <p class="summary-text">{{ result.summary }}</p>
    </el-card>

    <el-card v-if="result && result.suggests && result.suggests.length" class="result-card">
      <template #header>
        <div class="card-header">
          <span>分配建议（{{ result.suggests.length }} 条）</span>
          <div>
            <el-button size="small" @click="acceptAll">一键接受全部</el-button>
            <el-button size="small" type="primary" :loading="confirming" @click="handleConfirm">确认并应用</el-button>
          </div>
        </div>
      </template>
      <el-table :data="result.suggests" stripe>
        <el-table-column label="日期" prop="scheduleDate" width="100" />
        <el-table-column label="时间" width="120">
          <template #default="{ row }">
            {{ (row.startTime||'').substring(0,5) }} - {{ (row.endTime||'').substring(0,5) }}
          </template>
        </el-table-column>
        <el-table-column label="剧本" prop="scriptName" min-width="120" />
        <el-table-column label="推荐DM" min-width="180">
          <template #default="{ row }">
            <el-select v-if="row.hasCandidate" v-model="selections[row.scheduleId]" size="small" style="width:160px">
              <el-option v-for="c in row.candidates" :key="c.dmId" :value="c.dmId"
                :label="c.name + ' (' + c.matchScore + '分)'" />
            </el-select>
            <el-tag v-else type="danger" size="small">无可用DM</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="匹配分" width="80" align="center">
          <template #default="{ row }">
            <el-progress v-if="row.hasCandidate" type="circle" :percentage="row.matchScore" :width="40"
              :color="row.matchScore>=80?'#67c23a':row.matchScore>=60?'#e6a23c':'#f56c6c'" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="推荐理由" prop="reason" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <span>任务历史</span>
          <el-button size="small" @click="loadHistory">刷新</el-button>
        </div>
      </template>
      <el-table :data="taskHistory" size="small">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.taskType==='DM_ASSIGN'?'primary':'success'">
              {{ row.taskType === 'DM_ASSIGN' ? 'DM分配' : '员工排班' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="日期范围" width="175">
          <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status==='DONE'?'success':row.status==='FAILED'?'danger':'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">{{ row.successCount }}/{{ row.totalCount }}</template>
        </el-table-column>
        <el-table-column label="触发" width="70" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.triggerType==='AUTO'?'info':''">{{ row.triggerType==='AUTO'?'自动':'手动' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" prop="createTime" min-width="140" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { MagicStick, ChatDotRound } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { suggestDmAssignment, confirmDmAssignment, listScheduleTasks } from "@/api/ai-schedule.js";

const loading = ref(false);
const confirming = ref(false);
const result = ref(null);
const taskHistory = ref([]);
const storeList = ref([]);
const selections = reactive({});

const loginType = localStorage.getItem("admin-login-type") || "admin";
const isStoreAdmin = loginType === "store";
const fixedStoreId = localStorage.getItem("admin-store-id");

const form = reactive({
  storeId: isStoreAdmin ? (fixedStoreId ? Number(fixedStoreId) : null) : null,
  dateRange: [],
  onlyUnassigned: true,
});

onMounted(async () => {
  if (!isStoreAdmin) {
    try {
      const res = await request.get("/store/list");
      storeList.value = res.data || [];
      if (storeList.value.length && !form.storeId) form.storeId = storeList.value[0].id;
    } catch {}
  }
  loadHistory();
});

async function handleSuggest() {
  if (!form.storeId) return ElMessage.warning("请选择门店");
  loading.value = true;
  result.value = null;
  try {
    const [start, end] = form.dateRange || [];
    const res = await suggestDmAssignment({
      storeId: form.storeId,
      startDate: start || null,
      endDate: end || null,
      onlyUnassigned: form.onlyUnassigned,
    });
    result.value = res.data;
    (res.data.suggests || []).forEach((s) => {
      if (s.hasCandidate) selections[s.scheduleId] = s.recommendDmId;
    });
    ElMessage.success("AI推荐完成");
    loadHistory();
  } catch (e) {
    ElMessage.error("推荐失败：" + (e.message || "未知错误"));
  } finally {
    loading.value = false;
  }
}

function acceptAll() {
  (result.value?.suggests || []).forEach((s) => {
    if (s.hasCandidate) selections[s.scheduleId] = s.recommendDmId;
  });
  ElMessage.success("已接受全部AI推荐");
}

async function handleConfirm() {
  const items = Object.entries(selections)
    .filter(([, dmId]) => dmId)
    .map(([scheduleId, dmId]) => ({ scheduleId: Number(scheduleId), dmId }));
  if (!items.length) return ElMessage.warning("没有可确认的分配项");
  await ElMessageBox.confirm(`确认应用 ${items.length} 条DM分配方案？`, "确认", { type: "warning" });
  confirming.value = true;
  try {
    await confirmDmAssignment({ items });
    ElMessage.success("DM分配已应用成功");
    result.value = null;
  } catch (e) {
    ElMessage.error("应用失败：" + (e.message || "未知错误"));
  } finally {
    confirming.value = false;
  }
}

async function loadHistory() {
  if (!form.storeId) return;
  try {
    const res = await listScheduleTasks(form.storeId, 20);
    taskHistory.value = res.data || [];
  } catch {}
}
</script>

<style scoped>
.ai-dm-schedule { padding: 20px; }
.page-header { margin-bottom: 16px; }
.page-header h2 { margin: 0 0 4px; font-size: 22px; }
.subtitle { color: #666; margin: 0; font-size: 13px; }
.filter-card, .summary-card, .result-card, .history-card { margin-bottom: 16px; }
.summary-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.summary-title { font-weight: bold; font-size: 15px; flex: 1; }
.stats { display: flex; gap: 6px; }
.summary-text { color: #444; line-height: 1.7; margin: 0; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
