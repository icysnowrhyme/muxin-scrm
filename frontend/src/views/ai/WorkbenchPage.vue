<template>
  <div class="ym-page">
    <div class="ym-page-title">AI 会话工作台</div>
    <p class="ym-subtab-desc">
      导入企微会话后，AI 自动生成会话摘要、客户需求、意向判断、标签建议与下一步动作；确认后可一键写入跟进记录与客户标签。
    </p>

    <div class="ym-filter-card">
      <div class="ym-filter-row">
        <div class="ym-filter-field ym-filter-field--wide">
          <span class="ym-filter-label">关键字</span>
          <el-input
            v-model="query.keyword"
            clearable
            placeholder="会话标题 / 客户 / 员工"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="ym-filter-field">
          <span class="ym-filter-label">分析状态</span>
          <el-select v-model="query.status" clearable placeholder="全部会话">
            <el-option :value="0" label="未分析" />
            <el-option :value="1" label="已分析" />
          </el-select>
        </div>
        <div class="ym-filter-actions">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" plain @click="openImport">导入会话</el-button>
        </div>
      </div>
    </div>

    <div class="ai-workbench">
      <aside class="ai-conversation-panel">
        <div class="ai-conversation-head">
          <span>会话列表</span>
          <span class="ym-muted">共 {{ total }} 条</span>
        </div>
        <div v-loading="loading" class="ai-conversation-list">
          <button
            v-for="item in conversations"
            :key="item.id"
            type="button"
            :class="['ai-conversation', { active: isCurrent(item) }]"
            @click="selectConversation(item)"
          >
            <div class="ai-conversation__top">
              <span class="ai-conversation__title">{{ item.title || "未命名会话" }}</span>
              <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small" effect="plain">
                {{ item.status === 1 ? "已分析" : "未分析" }}
              </el-tag>
            </div>
            <div class="ai-conversation__meta">
              <span>{{ item.customerName || "未关联客户" }}</span>
              <span v-if="item.employeeName">· {{ item.employeeName }}</span>
            </div>
            <div class="ai-conversation__foot">
              <span>{{ item.messageCount || 0 }} 条消息</span>
              <span>{{ item.startedAt || item.createTime || "-" }}</span>
            </div>
          </button>
          <div v-if="!loading && !conversations.length" class="ym-empty ym-empty--gray">
            暂无会话，请先导入企微聊天记录
          </div>
        </div>
        <div v-if="total > query.pageSize" class="ai-conversation-pager">
          <el-pagination
            v-model:current-page="query.pageNum"
            :page-size="query.pageSize"
            :total="total"
            layout="prev, pager, next"
            small
            @current-change="loadConversations()"
          />
        </div>
      </aside>

      <section class="ym-panel ai-detail-panel">
        <template v-if="current">
          <div class="ym-panel-header ai-detail-head">
            <div>
              <div class="ym-panel-title">{{ current.title || "会话详情" }}</div>
              <div class="ai-detail-meta">
                <span>客户：{{ current.customerName || "-" }}</span>
                <span>员工：{{ current.employeeName || "-" }}</span>
                <span>{{ current.startedAt || current.createTime || "-" }}</span>
                <span>来源：{{ sourceLabel(current.source) }}</span>
              </div>
            </div>
            <div class="ym-toolbar">
              <el-button type="primary" plain :loading="analyzing" @click="handleAnalyze">
                {{ analysis ? "重新分析" : "生成 AI 摘要" }}
              </el-button>
              <el-button link type="danger" @click="handleDelete">删除会话</el-button>
            </div>
          </div>

          <el-tabs v-model="detailTab" class="ai-detail-tabs">
            <el-tab-pane label="会话原文" name="content">
              <div v-loading="detailLoading" class="ai-chat">
                <div
                  v-for="(line, index) in chatLines"
                  :key="index"
                  :class="['ai-chat-row', line.role === 'staff' ? 'is-staff' : 'is-customer']"
                >
                  <div class="ai-chat-avatar">{{ line.role === "staff" ? "员" : "客" }}</div>
                  <div class="ai-chat-bubble">
                    <div class="ai-chat-role">
                      {{ line.role === "staff" ? current.employeeName || "员工" : current.customerName || "客户" }}
                    </div>
                    <div class="ai-chat-text">{{ line.text }}</div>
                  </div>
                </div>
                <div v-if="!chatLines.length" class="ym-empty ym-empty--gray">暂无会话内容</div>
              </div>
            </el-tab-pane>

            <el-tab-pane name="analysis">
              <template #label>
                <span class="ai-tab-label">AI 洞察<span v-if="analysis" class="ai-tab-dot" /></span>
              </template>
              <div v-loading="analysisLoading" class="ai-analysis">
                <template v-if="analysis">
                  <div class="ai-analysis-block ai-analysis-summary">
                    <div class="ai-analysis-label">会话摘要</div>
                    <p>{{ analysis.summary || "暂无摘要" }}</p>
                  </div>

                  <div class="ai-analysis-grid">
                    <div class="ai-analysis-block">
                      <div class="ai-analysis-label">客户需求</div>
                      <div class="ai-chip-list">
                        <el-tag v-for="item in analysis.demands" :key="item" effect="plain">{{ item }}</el-tag>
                        <span v-if="!analysis.demands?.length" class="ym-muted">未识别</span>
                      </div>
                    </div>
                    <div class="ai-analysis-block">
                      <div class="ai-analysis-label">客户异议</div>
                      <div class="ai-chip-list">
                        <el-tag v-for="item in analysis.objections" :key="item" type="warning" effect="plain">
                          {{ item }}
                        </el-tag>
                        <span v-if="!analysis.objections?.length" class="ym-muted">未识别</span>
                      </div>
                    </div>
                    <div class="ai-analysis-block">
                      <div class="ai-analysis-label">已承诺事项</div>
                      <div class="ai-chip-list">
                        <el-tag v-for="item in analysis.commitments" :key="item" type="success" effect="plain">
                          {{ item }}
                        </el-tag>
                        <span v-if="!analysis.commitments?.length" class="ym-muted">未识别</span>
                      </div>
                    </div>
                  </div>

                  <div class="ai-analysis-facts">
                    <div class="ai-fact">
                      <span class="ai-fact-label">预算</span>
                      <span>{{ analysis.budget || "未提及" }}</span>
                    </div>
                    <div class="ai-fact">
                      <span class="ai-fact-label">决策人</span>
                      <span>{{ analysis.decisionMaker || "未提及" }}</span>
                    </div>
                    <div class="ai-fact">
                      <span class="ai-fact-label">情绪</span>
                      <span>{{ sentimentLabel(analysis.sentiment) }}</span>
                    </div>
                    <div class="ai-fact">
                      <span class="ai-fact-label">意向等级</span>
                      <el-tag :type="intentTag(analysis.intentLevel)" size="small" effect="plain">
                        {{ intentLabel(analysis.intentLevel) }}
                      </el-tag>
                    </div>
                  </div>

                  <div class="ai-analysis-block ai-next-action">
                    <div class="ai-analysis-label">建议下一步</div>
                    <p>{{ analysis.nextAction || "暂无建议" }}</p>
                  </div>

                  <div class="ai-analysis-block">
                    <div class="ai-analysis-label">标签建议</div>
                    <div class="ai-suggestion-list">
                      <div v-for="item in analysis.suggestions" :key="item.id" class="ai-suggestion">
                        <div class="ai-suggestion__main">
                          <el-tag size="small" effect="plain">{{ item.tagName }}</el-tag>
                          <span class="ym-muted">置信度 {{ item.confidence ?? "-" }}%</span>
                          <span v-if="item.evidence" class="ai-suggestion__evidence" :title="item.evidence">
                            {{ item.evidence }}
                          </span>
                        </div>
                        <div class="ai-suggestion__action">
                          <template v-if="item.status === 0">
                            <el-button size="small" type="primary" plain @click="handleAcceptTag(item)">采纳</el-button>
                            <el-button size="small" @click="handleRejectTag(item)">忽略</el-button>
                          </template>
                          <el-tag v-else size="small" :type="item.status === 1 ? 'success' : 'info'" effect="plain">
                            {{ item.status === 1 ? "已采纳" : "已忽略" }}
                          </el-tag>
                        </div>
                      </div>
                      <span v-if="!analysis.suggestions?.length" class="ym-muted">暂无标签建议</span>
                    </div>
                  </div>

                  <div class="ai-analysis-actions">
                    <el-button type="primary" :disabled="!analysis.customerId" @click="openFollowUp">
                      采纳为跟进记录
                    </el-button>
                    <span v-if="!analysis.customerId" class="ym-muted">未关联客户，无法写入跟进记录</span>
                    <span v-else-if="analysis.followUpAccepted === 1" class="ym-muted">
                      已写入跟进记录{{ analysis.followUpId ? ` #${analysis.followUpId}` : "" }}
                    </span>
                  </div>

                  <div class="ai-analysis-foot">
                    <span>{{ analysis.provider || "-" }} / {{ analysis.model || "-" }}</span>
                    <span>{{ analysis.totalTokens || 0 }} tokens · {{ analysis.latencyMs || 0 }} ms</span>
                    <span>{{ analysis.createTime || "-" }}</span>
                  </div>
                </template>
                <div v-else-if="!analysisLoading" class="ai-analysis-empty">
                  <div class="ym-empty ym-empty--gray">该会话尚未分析</div>
                  <el-button type="primary" :loading="analyzing" @click="handleAnalyze">立即生成 AI 摘要</el-button>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </template>

        <div v-else class="ym-empty ym-empty--gray ai-detail-empty">选择左侧会话查看原文与 AI 洞察</div>
      </section>
    </div>

    <el-dialog v-model="importVisible" title="导入企微会话" width="640px" destroy-on-close>
      <el-form :model="importForm" label-width="92px">
        <el-form-item label="关联客户">
          <el-select
            v-model="importForm.customerId"
            clearable
            filterable
            placeholder="请选择客户"
            style="width: 100%"
          >
            <el-option
              v-for="item in customerOptions"
              :key="item.id"
              :label="item.company ? `${item.name}（${item.company}）` : item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="会话标题">
          <el-input v-model="importForm.title" maxlength="64" placeholder="未填写时自动生成" />
        </el-form-item>
        <el-form-item label="员工姓名">
          <el-input v-model="importForm.employeeName" maxlength="32" placeholder="参与会话的员工" />
        </el-form-item>
        <el-form-item label="会话时间">
          <el-date-picker
            v-model="importForm.startedAt"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择会话开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="会话内容" required>
          <el-input
            v-model="importForm.content"
            type="textarea"
            :rows="10"
            placeholder="粘贴企微聊天记录，建议每行一条，以「客户：」「员工：」开头"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="handleImport">导入并分析</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followUpVisible" title="采纳为跟进记录" width="560px">
      <el-form :model="followUpForm" label-width="92px">
        <el-form-item label="跟进内容">
          <el-input v-model="followUpForm.content" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="跟进结果">
          <el-input v-model="followUpForm.result" maxlength="64" />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker
            v-model="followUpForm.nextFollowTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="可选"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followUpVisible = false">取消</el-button>
        <el-button type="primary" :loading="followingUp" @click="handleFollowUp">确认写入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "AiWorkbench" });

import { ElMessage, ElMessageBox } from "element-plus";
import AiAPI, { type AiAnalysis, type AiConversation, type AiTagSuggestion } from "@/api/ai";
import { CustomerAPI } from "@/api/scrm";
import type { CustomerOption } from "@/api/scrm/types";

const loading = ref(false);
const detailLoading = ref(false);
const analysisLoading = ref(false);
const analyzing = ref(false);
const importing = ref(false);
const followingUp = ref(false);

const conversations = ref<AiConversation[]>([]);
const total = ref(0);
const current = ref<AiConversation | null>(null);
const analysis = ref<AiAnalysis | null>(null);
const detailTab = ref("content");

const customerOptions = ref<CustomerOption[]>([]);
const importVisible = ref(false);
const followUpVisible = ref(false);

const query = reactive<{ pageNum: number; pageSize: number; keyword: string; status?: number }>({
  pageNum: 1,
  pageSize: 20,
  keyword: "",
  status: undefined,
});

const importForm = reactive({
  customerId: undefined as string | number | undefined,
  title: "",
  employeeName: "",
  startedAt: "",
  content: "",
});

const followUpForm = reactive({
  content: "",
  result: "AI 会话摘要跟进",
  nextFollowTime: "",
});

const chatLines = computed(() => {
  const content = current.value?.content ?? "";
  return content
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      if (line.startsWith("客户：") || line.startsWith("客户:")) {
        return { role: "customer", text: line.slice(3) };
      }
      if (line.startsWith("员工：") || line.startsWith("员工:")) {
        return { role: "staff", text: line.slice(3) };
      }
      return { role: "customer", text: line };
    });
});

const importCustomerName = computed(() => {
  const customer = customerOptions.value.find((item) => String(item.id) === String(importForm.customerId));
  return customer?.name ?? "";
});

watch(
  () => importForm.customerId,
  (value) => {
    const customer = customerOptions.value.find((item) => String(item.id) === String(value));
    if (customer && !importForm.title) {
      importForm.title = `${customer.name}会话沟通`;
    }
  }
);

function isCurrent(item: AiConversation) {
  return current.value != null && String(current.value.id) === String(item.id);
}

function sourceLabel(source?: string) {
  const labels: Record<string, string> = { wecom: "企业微信", import: "手动导入", demo: "演示数据" };
  return source ? labels[source] ?? source : "-";
}

function sentimentLabel(sentiment?: string) {
  const labels: Record<string, string> = {
    positive: "积极",
    neutral: "中性",
    negative: "消极",
    POSITIVE: "积极",
    NEUTRAL: "中性",
    NEGATIVE: "消极",
  };
  return sentiment ? labels[sentiment] ?? sentiment : "未识别";
}

function intentLabel(level?: string) {
  const labels: Record<string, string> = {
    high: "高意向",
    medium: "中意向",
    low: "低意向",
    HIGH: "高意向",
    MEDIUM: "中意向",
    LOW: "低意向",
  };
  return level ? labels[level] ?? level : "未知";
}

function intentTag(level?: string) {
  const value = (level ?? "").toLowerCase();
  if (value.includes("高") || value === "high") return "danger";
  if (value.includes("中") || value === "medium") return "warning";
  return "info";
}

async function loadConversations(preferId?: string | number) {
  loading.value = true;
  try {
    const result = await AiAPI.conversations({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      status: query.status,
    });
    conversations.value = result.list;
    total.value = result.total;
    const targetId = preferId ?? current.value?.id;
    const target = result.list.find((item) => String(item.id) === String(targetId)) ?? result.list[0];
    if (target) {
      await selectConversation(target);
    } else {
      current.value = null;
      analysis.value = null;
    }
  } finally {
    loading.value = false;
  }
}

async function selectConversation(item: AiConversation) {
  if (isCurrent(item) && analysis.value) return;
  current.value = item;
  detailTab.value = item.status === 1 ? "analysis" : "content";
  analysis.value = null;
  detailLoading.value = true;
  analysisLoading.value = true;
  try {
    current.value = await AiAPI.conversation(item.id);
    analysis.value = await AiAPI.analysis(item.id);
  } finally {
    detailLoading.value = false;
    analysisLoading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadConversations();
}

function handleReset() {
  query.keyword = "";
  query.status = undefined;
  query.pageNum = 1;
  loadConversations();
}

async function handleAnalyze() {
  if (!current.value) return;
  analyzing.value = true;
  analysisLoading.value = true;
  try {
    analysis.value = await AiAPI.analyze(current.value.id);
    current.value.status = 1;
    const item = conversations.value.find((row) => String(row.id) === String(current.value?.id));
    if (item) item.status = 1;
    detailTab.value = "analysis";
    ElMessage.success("AI 摘要已生成");
  } finally {
    analyzing.value = false;
    analysisLoading.value = false;
  }
}

async function handleDelete() {
  if (!current.value) return;
  await ElMessageBox.confirm(`确定删除会话「${current.value.title || "未命名会话"}」及其分析结果吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消",
  });
  await AiAPI.deleteConversations(String(current.value.id));
  ElMessage.success("会话已删除");
  current.value = null;
  analysis.value = null;
  loadConversations();
}

async function handleAcceptTag(item: AiTagSuggestion) {
  await AiAPI.acceptTag(item.id);
  item.status = 1;
  ElMessage.success(`标签「${item.tagName}」已写入客户`);
}

async function handleRejectTag(item: AiTagSuggestion) {
  await AiAPI.rejectTag(item.id);
  item.status = 2;
  ElMessage.success("已忽略该标签建议");
}

async function openImport() {
  if (!customerOptions.value.length) {
    customerOptions.value = await CustomerAPI.getOptions();
  }
  importForm.customerId = undefined;
  importForm.title = "";
  importForm.employeeName = "";
  importForm.startedAt = "";
  importForm.content = "";
  importVisible.value = true;
}

async function handleImport() {
  if (!importForm.content.trim()) {
    ElMessage.warning("请粘贴会话内容");
    return;
  }
  importing.value = true;
  try {
    const created = await AiAPI.createConversation({
      title: importForm.title || `${importCustomerName.value || "客户"}会话沟通`,
      customerId: importForm.customerId,
      customerName: importCustomerName.value || undefined,
      employeeName: importForm.employeeName || undefined,
      startedAt: importForm.startedAt || undefined,
      content: importForm.content,
      source: "import",
    });
    importVisible.value = false;
    ElMessage.success("会话已导入，正在生成 AI 摘要");
    query.pageNum = 1;
    await loadConversations(created.id);
    await handleAnalyze();
  } finally {
    importing.value = false;
  }
}

function openFollowUp() {
  if (!analysis.value) return;
  followUpForm.content = analysis.value.summary || "";
  followUpForm.result = "AI 会话摘要跟进";
  followUpForm.nextFollowTime = "";
  followUpVisible.value = true;
}

async function handleFollowUp() {
  if (!analysis.value) return;
  if (!followUpForm.content.trim()) {
    ElMessage.warning("跟进内容不能为空");
    return;
  }
  followingUp.value = true;
  try {
    const followUpId = await AiAPI.acceptFollowUp(analysis.value.id, {
      content: followUpForm.content,
      result: followUpForm.result || undefined,
      nextFollowTime: followUpForm.nextFollowTime || undefined,
    });
    analysis.value.followUpAccepted = 1;
    analysis.value.followUpId = followUpId;
    followUpVisible.value = false;
    ElMessage.success("已写入客户跟进记录");
  } finally {
    followingUp.value = false;
  }
}

onMounted(() => {
  loadConversations();
});
</script>

<style lang="scss" scoped>
.ai-workbench {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}

.ai-conversation-panel {
  display: flex;
  flex-direction: column;
  min-height: 560px;
  max-height: calc(100vh - 250px);
  background: #fff;
  box-shadow: 0 4px 12px rgb(0 0 0 / 12%);
}

.ai-conversation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  font-size: 14px;
  font-weight: 500;
  color: #222;
  border-bottom: 1px solid #ebeef5;
}

.ai-conversation-list {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
}

.ai-conversation {
  display: block;
  width: 100%;
  padding: 12px;
  margin-bottom: 6px;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 4px;
  transition: background 0.18s;

  &:hover {
    background: #f7f9fc;
  }

  &.active {
    background: #f2f6ff;
    border-color: #c9d9ff;
  }

  &__top {
    display: flex;
    gap: 8px;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    overflow: hidden;
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    gap: 4px;
    margin-top: 6px;
    font-size: 13px;
    color: #606266;
  }

  &__foot {
    display: flex;
    justify-content: space-between;
    margin-top: 6px;
    font-size: 12px;
    color: #a8abb2;
  }
}

.ai-conversation-pager {
  display: flex;
  justify-content: center;
  padding: 8px 0;
  border-top: 1px solid #ebeef5;
}

.ai-detail-panel {
  min-height: 560px;
  margin-top: 0;
}

.ai-detail-head {
  align-items: flex-start;
}

.ai-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.ai-detail-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }
}

.ai-tab-label {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.ai-tab-dot {
  width: 6px;
  height: 6px;
  background: #67c23a;
  border-radius: 50%;
}

.ai-detail-empty {
  min-height: 520px;
}

.ai-chat {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: calc(100vh - 380px);
  padding: 4px 8px;
  overflow-y: auto;
}

.ai-chat-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;

  &.is-staff {
    flex-direction: row-reverse;

    .ai-chat-bubble {
      align-items: flex-end;
      background: #eef4ff;
    }
  }
}

.ai-chat-avatar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: 13px;
  color: #fff;
  background: #9aa4b2;
  border-radius: 50%;
}

.is-staff .ai-chat-avatar {
  background: #035cff;
}

.ai-chat-bubble {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  max-width: 72%;
  padding: 10px 12px;
  background: #f5f6f9;
  border-radius: 6px;
}

.ai-chat-role {
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.ai-chat-text {
  font-size: 14px;
  line-height: 22px;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-analysis {
  min-height: 320px;
}

.ai-analysis-block {
  padding: 14px 16px;
  margin-bottom: 12px;
  background: #fafbfd;
  border: 1px solid #eef0f4;
  border-radius: 4px;

  p {
    margin: 8px 0 0;
    font-size: 14px;
    line-height: 24px;
    color: #303133;
  }
}

.ai-analysis-label {
  font-size: 13px;
  font-weight: 500;
  color: #909399;
}

.ai-analysis-summary {
  background: #f4f8ff;
  border-color: #e3edff;
}

.ai-analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  .ai-analysis-block {
    margin-bottom: 0;
  }
}

.ai-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.ai-analysis-facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 12px 0;
}

.ai-fact {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  font-size: 14px;
  color: #303133;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.ai-fact-label {
  font-size: 12px;
  color: #909399;
}

.ai-next-action {
  background: #fff8f0;
  border-color: #ffe6cc;
}

.ai-suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.ai-suggestion {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;

  &__main {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
    min-width: 0;
    font-size: 12px;
  }

  &__evidence {
    max-width: 320px;
    overflow: hidden;
    color: #a8abb2;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__action {
    display: flex;
    flex: none;
    gap: 6px;
    align-items: center;
  }
}

.ai-analysis-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 4px 0 12px;
  font-size: 13px;
}

.ai-analysis-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  padding-top: 12px;
  font-size: 12px;
  color: #b0b4bd;
  border-top: 1px dashed #ebeef5;
}

.ai-analysis-empty {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-height: 320px;

  .ym-empty {
    min-height: auto;
    padding: 24px 0;
  }
}

@media (width <= 1400px) {
  .ai-workbench {
    grid-template-columns: 300px minmax(0, 1fr);
  }

  .ai-analysis-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .ai-analysis-facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
