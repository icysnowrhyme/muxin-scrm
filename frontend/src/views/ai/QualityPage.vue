<template>
  <div class="ym-page">
    <div class="ym-page-title">AI 智能质检</div>
    <p class="ym-subtab-desc">
      基于大模型对企微会话做合规与服务规范检查，命中问题进入人工复核，复核结论沉淀为质检数据。
    </p>

    <div class="ym-stat-grid">
      <div v-for="card in statCards" :key="card.label" class="ym-stat-card">
        <div class="ym-stat-card__label">{{ card.label }}</div>
        <div class="ym-stat-card__value">{{ card.value }}</div>
        <div class="ym-stat-card__foot">{{ card.foot }}</div>
      </div>
    </div>

    <div class="ym-subtabs" style="margin-top: 16px">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        :class="['ym-subtab', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>

    <div v-if="activeTab === 'tasks'" class="ym-panel" style="margin-top: 0">
      <div class="ym-panel-header">
        <div class="ym-panel-title">质检任务</div>
        <div class="ym-toolbar">
          <el-input
            v-model="taskQuery.keyword"
            clearable
            placeholder="任务名称"
            style="width: 220px"
            @keyup.enter="handleTaskSearch"
          />
          <el-select v-model="taskQuery.status" clearable placeholder="全部状态" style="width: 130px">
            <el-option v-for="item in TASK_STATUS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button type="primary" @click="handleTaskSearch">查询</el-button>
          <el-button @click="loadTasks()">刷新</el-button>
          <el-button type="primary" plain @click="openTaskDialog">新建质检任务</el-button>
        </div>
      </div>

      <el-table v-loading="taskLoading" :data="tasks" border>
        <el-table-column prop="name" label="任务名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="质检进度" min-width="180">
          <template #default="{ row }">
            <div class="ai-task-progress">
              <el-progress :percentage="taskPercent(row)" :stroke-width="8" :show-text="false" />
              <span>{{ row.processedCount || 0 }}/{{ row.totalCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="hitCount" label="命中问题" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="taskStatusTag(row.status)" size="small" effect="plain">
              {{ taskStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="失败原因" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.errorMessage || "-" }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTaskResults(row)">查看结果</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="ym-empty ym-empty--gray">暂无质检任务</div>
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="taskQuery.pageNum"
        v-model:page-size="taskQuery.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="taskTotal"
        layout="total, sizes, prev, pager, next"
        @size-change="loadTasks()"
        @current-change="loadTasks()"
      />
    </div>

    <div v-else-if="activeTab === 'results'" class="ym-panel" style="margin-top: 0">
      <div class="ym-panel-header">
        <div class="ym-panel-title">质检结果</div>
        <div class="ym-toolbar">
          <el-select v-model="resultQuery.taskId" clearable placeholder="全部任务" style="width: 210px">
            <el-option v-for="item in taskOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-select v-model="resultQuery.severity" clearable placeholder="严重程度" style="width: 130px">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
          <el-select v-model="resultQuery.reviewStatus" clearable placeholder="复核状态" style="width: 130px">
            <el-option label="待复核" :value="0" />
            <el-option label="确认违规" :value="1" />
            <el-option label="误报" :value="2" />
          </el-select>
          <el-checkbox v-model="resultQuery.hitOnly">只看命中</el-checkbox>
          <el-button type="primary" @click="handleResultSearch">查询</el-button>
          <el-button @click="handleResultReset">重置</el-button>
        </div>
      </div>

      <el-table v-loading="resultLoading" :data="results" border>
        <el-table-column label="会话" min-width="170">
          <template #default="{ row }">
            <div>{{ row.customerName || "未关联客户" }}</div>
            <div class="ym-muted">会话 #{{ row.conversationId }}</div>
          </template>
        </el-table-column>
        <el-table-column label="质检规则" min-width="160">
          <template #default="{ row }">
            <div>{{ row.ruleName || "-" }}</div>
            <div class="ym-muted">{{ row.ruleCode }}</div>
          </template>
        </el-table-column>
        <el-table-column label="严重程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" size="small" effect="plain">
              {{ severityLabel(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="命中" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.hit === 1 ? 'danger' : 'info'" size="small" effect="plain">
              {{ row.hit === 1 ? "是" : "否" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="80" align="center" />
        <el-table-column prop="evidence" label="证据原文" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.evidence || "-" }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="判定说明" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reason || "-" }}</template>
        </el-table-column>
        <el-table-column label="复核" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="reviewTag(row.reviewStatus)" size="small" effect="plain">
              {{ reviewLabel(row.reviewStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="复核人" width="100">
          <template #default="{ row }">{{ row.reviewerName || "-" }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.hit === 1 && row.reviewStatus === 0 && canReview"
              link
              type="primary"
              @click="openReview(row)"
            >
              复核
            </el-button>
            <span v-else class="ym-muted">-</span>
          </template>
        </el-table-column>
        <template #empty>
          <div class="ym-empty ym-empty--gray">暂无质检结果</div>
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="resultQuery.pageNum"
        v-model:page-size="resultQuery.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="resultTotal"
        layout="total, sizes, prev, pager, next"
        @size-change="loadResults()"
        @current-change="loadResults()"
      />
    </div>

    <div v-else class="ym-panel" style="margin-top: 0">
      <div class="ym-panel-header">
        <div class="ym-panel-title">质检规则</div>
        <div class="ym-toolbar">
          <el-input
            v-model="ruleKeyword"
            clearable
            placeholder="规则名称 / 编码"
            style="width: 220px"
            @keyup.enter="loadRules"
          />
          <el-select v-model="ruleEnabled" clearable placeholder="全部状态" style="width: 130px">
            <el-option label="已启用" :value="1" />
            <el-option label="已停用" :value="0" />
          </el-select>
          <el-button type="primary" @click="loadRules">查询</el-button>
          <el-button v-if="canManageRules" type="primary" plain @click="openRuleDialog()">新增规则</el-button>
        </div>
      </div>

      <el-table v-loading="ruleLoading" :data="rules" border>
        <el-table-column prop="code" label="规则编码" width="140" />
        <el-table-column prop="name" label="规则名称" min-width="130" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">{{ row.category || "-" }}</template>
        </el-table-column>
        <el-table-column label="严重程度" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="severityTag(row.severity)" size="small" effect="plain">
              {{ severityLabel(row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="instruction" label="判定说明" min-width="260" show-overflow-tooltip />
        <el-table-column label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === 1" :disabled="!canManageRules" @change="(value: string | number | boolean) => toggleRule(row, Boolean(value))" />
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManageRules" link type="primary" @click="openRuleDialog(row)">编辑</el-button>
            <el-button v-if="canManageRules" link type="danger" @click="handleDeleteRule(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="ym-empty ym-empty--gray">暂无质检规则</div>
        </template>
      </el-table>
    </div>

    <el-dialog v-model="taskVisible" title="新建质检任务" width="620px" destroy-on-close>
      <el-form :model="taskForm" label-width="92px">
        <el-form-item label="任务名称" required>
          <el-input v-model="taskForm.name" maxlength="64" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="质检会话" required>
          <el-select
            v-model="taskForm.conversationIds"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            placeholder="请选择要质检的会话"
            style="width: 100%"
          >
            <el-option
              v-for="item in conversationOptions"
              :key="item.id"
              :label="`${item.customerName || '未关联客户'} · ${item.title || `会话 #${item.id}`}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="质检规则">
          <el-select
            v-model="taskForm.ruleIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="留空则使用全部启用规则"
            style="width: 100%"
          >
            <el-option v-for="item in enabledRules" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <div class="ym-muted ai-form-tip">默认勾选全部启用规则，可按需调整。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="taskVisible = false">取消</el-button>
        <el-button type="primary" :loading="taskSubmitting" @click="handleCreateTask">开始质检</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="ruleVisible" :title="ruleForm.id ? '编辑质检规则' : '新增质检规则'" width="600px" destroy-on-close>
      <el-form :model="ruleForm" label-width="92px">
        <el-form-item label="规则编码" required>
          <el-input v-model="ruleForm.code" maxlength="64" :disabled="!!ruleForm.id" placeholder="如 PROMISE" />
        </el-form-item>
        <el-form-item label="规则名称" required>
          <el-input v-model="ruleForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="ruleForm.category" maxlength="64" placeholder="如 合规风险" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="ruleForm.severity" style="width: 100%">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="ruleForm.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="规则描述">
          <el-input v-model="ruleForm.description" maxlength="200" />
        </el-form-item>
        <el-form-item label="判定说明" required>
          <el-input
            v-model="ruleForm.instruction"
            type="textarea"
            :rows="4"
            placeholder="描述 AI 需要识别的问题，例如：禁止承诺收益、使用绝对化表述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSubmitting" @click="handleSaveRule">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="reviewVisible" title="复核质检结果" width="560px">
      <el-descriptions v-if="reviewing" :column="1" border label-width="92px" class="ai-review-desc">
        <el-descriptions-item label="会话">{{ reviewing.customerName || `会话 #${reviewing.conversationId}` }}</el-descriptions-item>
        <el-descriptions-item label="质检规则">{{ reviewing.ruleName }}（{{ reviewing.ruleCode }}）</el-descriptions-item>
        <el-descriptions-item label="证据原文">{{ reviewing.evidence || "-" }}</el-descriptions-item>
        <el-descriptions-item label="判定说明">{{ reviewing.reason || "-" }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="reviewForm" label-width="92px" style="margin-top: 16px">
        <el-form-item label="复核结论">
          <el-radio-group v-model="reviewForm.reviewStatus">
            <el-radio :value="1">确认违规</el-radio>
            <el-radio :value="2">误报</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="reviewForm.reviewNote" type="textarea" :rows="3" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="handleReview">提交复核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "AiQuality" });

import { ElMessage, ElMessageBox } from "element-plus";
import AiAPI, {
  type AiConversation,
  type AiId,
  type AiQualityResult,
  type AiQualityRule,
  type AiQualityStats,
  type AiQualityTask,
} from "@/api/ai";
import { useUserStore } from "@/stores";

const TASK_STATUS = [
  { label: "等待中", value: 0 },
  { label: "运行中", value: 1 },
  { label: "已完成", value: 2 },
  { label: "失败", value: 3 },
];

const tabs = [
  { key: "tasks", label: "质检任务" },
  { key: "results", label: "质检结果" },
  { key: "rules", label: "质检规则" },
];

const activeTab = ref("tasks");
const stats = ref<AiQualityStats | null>(null);

const taskLoading = ref(false);
const tasks = ref<AiQualityTask[]>([]);
const taskTotal = ref(0);
const taskOptions = ref<AiQualityTask[]>([]);
const taskQuery = reactive<{ pageNum: number; pageSize: number; keyword: string; status?: number }>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  status: undefined,
});

const resultLoading = ref(false);
const results = ref<AiQualityResult[]>([]);
const resultTotal = ref(0);
const resultQuery = reactive<{
  pageNum: number;
  pageSize: number;
  taskId?: AiId;
  severity?: string;
  reviewStatus?: number;
  hitOnly: boolean;
}>({
  pageNum: 1,
  pageSize: 10,
  taskId: undefined,
  severity: undefined,
  reviewStatus: undefined,
  hitOnly: true,
});

const ruleLoading = ref(false);
const rules = ref<AiQualityRule[]>([]);
const ruleKeyword = ref("");
const ruleEnabled = ref<number | undefined>(undefined);
const enabledRules = computed(() => rules.value.filter((item) => item.enabled === 1));

const userStore = useUserStore();
const canReview = computed(() =>
  (userStore.userInfo.roles ?? []).some((role) => ["ROOT", "ADMIN", "DEPT_MANAGER"].includes(role))
);
const canManageRules = canReview;

const taskVisible = ref(false);
const taskSubmitting = ref(false);
const conversationOptions = ref<AiConversation[]>([]);
const taskForm = reactive<{ name: string; conversationIds: AiId[]; ruleIds: AiId[] }>({
  name: "",
  conversationIds: [],
  ruleIds: [],
});

const ruleVisible = ref(false);
const ruleSubmitting = ref(false);
const ruleForm = reactive({
  id: undefined as AiId | undefined,
  code: "",
  name: "",
  category: "",
  description: "",
  instruction: "",
  severity: "medium",
  enabled: 1,
  sort: 0,
});

const reviewVisible = ref(false);
const reviewSubmitting = ref(false);
const reviewing = ref<AiQualityResult | null>(null);
const reviewForm = reactive({ reviewStatus: 1, reviewNote: "" });

let pollTimer: ReturnType<typeof setInterval> | undefined;

const statCards = computed(() => [
  {
    label: "质检规则",
    value: stats.value?.ruleCount ?? 0,
    foot: `${stats.value?.enabledRuleCount ?? 0} 条已启用`,
  },
  {
    label: "质检任务",
    value: stats.value?.taskCount ?? 0,
    foot: `${stats.value?.runningTaskCount ?? 0} 个运行中`,
  },
  { label: "命中问题", value: stats.value?.hitCount ?? 0, foot: "累计命中违规项" },
  { label: "待复核", value: stats.value?.pendingReview ?? 0, foot: "等待主管确认" },
  {
    label: "已确认 / 误报",
    value: `${stats.value?.confirmedCount ?? 0} / ${stats.value?.falsePositiveCount ?? 0}`,
    foot: "复核结论统计",
  },
]);

function taskPercent(row: any) {
  if (!row.totalCount) return 0;
  return Math.min(100, Math.round(((row.processedCount || 0) / row.totalCount) * 100));
}

function taskStatusLabel(status?: number) {
  return TASK_STATUS.find((item) => item.value === status)?.label ?? "未知";
}

function taskStatusTag(status?: number) {
  if (status === 2) return "success";
  if (status === 1) return "warning";
  if (status === 3) return "danger";
  return "info";
}

function severityLabel(severity?: string) {
  return severity === "high" ? "高" : severity === "medium" ? "中" : severity === "low" ? "低" : "-";
}

function severityTag(severity?: string) {
  if (severity === "high") return "danger";
  if (severity === "medium") return "warning";
  return "info";
}

function reviewLabel(status?: number) {
  if (status === 1) return "确认违规";
  if (status === 2) return "误报";
  return "待复核";
}

function reviewTag(status?: number) {
  if (status === 1) return "danger";
  if (status === 2) return "success";
  return "info";
}

async function loadStats() {
  stats.value = await AiAPI.qualityStats();
}

async function loadTasks(silent = false) {
  if (!silent) taskLoading.value = true;
  try {
    const result = await AiAPI.qualityTasks({
      pageNum: taskQuery.pageNum,
      pageSize: taskQuery.pageSize,
      keyword: taskQuery.keyword || undefined,
      status: taskQuery.status,
    });
    tasks.value = result.list;
    taskTotal.value = result.total;
    syncPolling();
  } finally {
    if (!silent) taskLoading.value = false;
  }
}

function handleTaskSearch() {
  taskQuery.pageNum = 1;
  loadTasks();
}

function syncPolling() {
  const running = activeTab.value === "tasks" && tasks.value.some((item) => item.status === 0 || item.status === 1);
  if (running && !pollTimer) {
    pollTimer = setInterval(async () => {
      await Promise.all([loadTasks(true), loadStats()]);
    }, 3000);
  } else if (!running && pollTimer) {
    clearInterval(pollTimer);
    pollTimer = undefined;
  }
}

async function loadResultTasks() {
  const result = await AiAPI.qualityTasks({ pageNum: 1, pageSize: 100 });
  taskOptions.value = result.list;
}

async function loadResults() {
  resultLoading.value = true;
  try {
    const result = await AiAPI.qualityResults({
      pageNum: resultQuery.pageNum,
      pageSize: resultQuery.pageSize,
      taskId: resultQuery.taskId,
      severity: resultQuery.severity,
      reviewStatus: resultQuery.reviewStatus,
      hitOnly: resultQuery.hitOnly,
    });
    results.value = result.list;
    resultTotal.value = result.total;
  } finally {
    resultLoading.value = false;
  }
}

function handleResultSearch() {
  resultQuery.pageNum = 1;
  loadResults();
}

function handleResultReset() {
  resultQuery.taskId = undefined;
  resultQuery.severity = undefined;
  resultQuery.reviewStatus = undefined;
  resultQuery.hitOnly = true;
  resultQuery.pageNum = 1;
  loadResults();
}

async function openTaskResults(row: any) {
  resultQuery.taskId = row.id;
  resultQuery.pageNum = 1;
  activeTab.value = "results";
}

async function loadRules() {
  ruleLoading.value = true;
  try {
    rules.value = await AiAPI.qualityRules({
      keyword: ruleKeyword.value || undefined,
      enabled: ruleEnabled.value,
    });
  } finally {
    ruleLoading.value = false;
  }
}

async function openTaskDialog() {
  taskForm.name = defaultTaskName();
  taskForm.conversationIds = [];
  taskForm.ruleIds = [];
  taskVisible.value = true;
  try {
    const [conversationResult, ruleList] = await Promise.all([
      AiAPI.conversations({ pageNum: 1, pageSize: 100 }),
      AiAPI.qualityRules({ enabled: 1 }),
    ]);
    conversationOptions.value = conversationResult.list;
    rules.value = ruleList;
    taskForm.ruleIds = enabledRules.value.map((item) => item.id);
  } catch {
    taskVisible.value = false;
  }
}

function defaultTaskName() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `质检任务 ${now.getMonth() + 1}-${now.getDate()} ${pad(now.getHours())}:${pad(now.getMinutes())}`;
}

async function handleCreateTask() {
  if (!taskForm.name.trim()) {
    ElMessage.warning("请输入任务名称");
    return;
  }
  if (!taskForm.conversationIds.length) {
    ElMessage.warning("请选择要质检的会话");
    return;
  }
  taskSubmitting.value = true;
  try {
    await AiAPI.createQualityTask({
      name: taskForm.name,
      conversationIds: taskForm.conversationIds,
      ruleIds: taskForm.ruleIds,
    });
    taskVisible.value = false;
    activeTab.value = "tasks";
    taskQuery.pageNum = 1;
    ElMessage.success("质检任务已启动");
    await Promise.all([loadTasks(), loadStats()]);
  } finally {
    taskSubmitting.value = false;
  }
}

function openRuleDialog(row?: any) {
  ruleForm.id = row?.id;
  ruleForm.code = row?.code ?? "";
  ruleForm.name = row?.name ?? "";
  ruleForm.category = row?.category ?? "";
  ruleForm.description = row?.description ?? "";
  ruleForm.instruction = row?.instruction ?? "";
  ruleForm.severity = row?.severity ?? "medium";
  ruleForm.enabled = row?.enabled ?? 1;
  ruleForm.sort = row?.sort ?? 0;
  ruleVisible.value = true;
}

async function handleSaveRule() {
  if (!ruleForm.code.trim() || !ruleForm.name.trim() || !ruleForm.instruction.trim()) {
    ElMessage.warning("请填写规则编码、名称与判定说明");
    return;
  }
  ruleSubmitting.value = true;
  try {
    const payload = {
      code: ruleForm.code.trim().toUpperCase(),
      name: ruleForm.name,
      category: ruleForm.category || undefined,
      description: ruleForm.description || undefined,
      instruction: ruleForm.instruction,
      severity: ruleForm.severity,
      enabled: ruleForm.enabled,
      sort: ruleForm.sort,
    };
    if (ruleForm.id) {
      await AiAPI.updateQualityRule(ruleForm.id, payload);
    } else {
      await AiAPI.createQualityRule(payload);
    }
    ruleVisible.value = false;
    ElMessage.success("规则已保存");
    await Promise.all([loadRules(), loadStats()]);
  } finally {
    ruleSubmitting.value = false;
  }
}

async function toggleRule(row: any, enabled: boolean) {
  await AiAPI.updateQualityRule(row.id, {
    code: row.code,
    name: row.name,
    category: row.category,
    description: row.description,
    instruction: row.instruction,
    severity: row.severity,
    enabled: enabled ? 1 : 0,
    sort: row.sort,
  });
  row.enabled = enabled ? 1 : 0;
  ElMessage.success(enabled ? "规则已启用" : "规则已停用");
  loadStats();
}

async function handleDeleteRule(row: any) {
  await ElMessageBox.confirm(`确定删除质检规则「${row.name}」吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消",
  });
  await AiAPI.deleteQualityRules(String(row.id));
  ElMessage.success("规则已删除");
  await Promise.all([loadRules(), loadStats()]);
}

function openReview(row: any) {
  reviewing.value = row;
  reviewForm.reviewStatus = 1;
  reviewForm.reviewNote = "";
  reviewVisible.value = true;
}

async function handleReview() {
  if (!reviewing.value) return;
  reviewSubmitting.value = true;
  try {
    await AiAPI.reviewQualityResult(reviewing.value.id, {
      reviewStatus: reviewForm.reviewStatus,
      reviewNote: reviewForm.reviewNote || undefined,
    });
    reviewVisible.value = false;
    ElMessage.success("复核结论已保存");
    await Promise.all([loadResults(), loadStats()]);
  } finally {
    reviewSubmitting.value = false;
  }
}

watch(activeTab, (value) => {
  if (value === "tasks") {
    loadTasks();
  } else {
    if (pollTimer) {
      clearInterval(pollTimer);
      pollTimer = undefined;
    }
    if (value === "results") {
      loadResultTasks();
      loadResults();
    }
    if (value === "rules") {
      loadRules();
    }
  }
});

onMounted(async () => {
  await Promise.all([loadStats(), loadTasks(), loadRules()]);
});

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer);
});
</script>

<style lang="scss" scoped>
.ai-task-progress {
  display: flex;
  gap: 10px;
  align-items: center;

  .el-progress {
    flex: 1;
  }

  span {
    flex: none;
    font-size: 12px;
    color: #909399;
  }
}

.ai-form-tip {
  margin-top: 4px;
  font-size: 12px;
  line-height: 18px;
}

.ai-review-desc :deep(.el-descriptions__cell) {
  font-size: 13px;
}
</style>
