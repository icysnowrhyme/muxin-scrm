import request from "@/utils/request";
import type { PageResult } from "@/api/common";

export type AiId = string | number;

export interface AiConversation {
  id: AiId;
  title: string;
  customerId?: AiId;
  customerName?: string;
  employeeName?: string;
  ownerId?: AiId;
  source?: string;
  content?: string;
  messageCount?: number;
  startedAt?: string;
  status?: number;
  createTime?: string;
}

export interface AiTagSuggestion {
  id: AiId;
  conversationId: AiId;
  analysisId?: AiId;
  customerId?: AiId;
  tagId?: AiId;
  tagName: string;
  confidence?: number;
  evidence?: string;
  status?: number;
}

export interface AiAnalysis {
  id: AiId;
  conversationId: AiId;
  customerId?: AiId;
  summary?: string;
  demands?: string[];
  objections?: string[];
  budget?: string;
  decisionMaker?: string;
  commitments?: string[];
  nextAction?: string;
  sentiment?: string;
  intentLevel?: string;
  tags?: string[];
  provider?: string;
  model?: string;
  totalTokens?: number;
  latencyMs?: number;
  followUpAccepted?: number;
  followUpId?: AiId;
  createTime?: string;
  suggestions?: AiTagSuggestion[];
}

export interface AiModelConfig {
  id: AiId;
  name: string;
  provider: string;
  baseUrl?: string;
  model: string;
  temperature?: number;
  maxTokens?: number;
  enabled?: number;
  isDefault?: number;
  hasApiKey?: boolean;
  apiKeyMasked?: string;
  updateTime?: string;
}

export interface AiTestResult {
  provider: string;
  model: string;
  mocked: boolean;
  latencyMs: number;
  message: string;
}

export interface AiUsageFeature {
  feature: string;
  calls: number;
  tokens: number;
  avgLatency: number;
  successCalls: number;
}

export interface AiUsageSummary {
  totalCalls: number;
  totalTokens: number;
  successCalls: number;
  successRate: number;
  avgLatency: number;
  features: AiUsageFeature[];
}

export interface AiPromptTemplate {
  id: AiId;
  code: string;
  name: string;
  systemPrompt: string;
  userTemplate: string;
  version?: number;
  enabled?: number;
  updateTime?: string;
}

export interface AiQualityRule {
  id: AiId;
  code: string;
  name: string;
  category?: string;
  description?: string;
  instruction: string;
  severity?: string;
  enabled?: number;
  sort?: number;
  updateTime?: string;
}

export interface AiQualityTask {
  id: AiId;
  name: string;
  conversationIds?: string;
  ruleIds?: string;
  totalCount: number;
  processedCount: number;
  hitCount: number;
  status: number;
  errorMessage?: string;
  createTime?: string;
  updateTime?: string;
}

export interface AiQualityResult {
  id: AiId;
  taskId: AiId;
  conversationId: AiId;
  customerName?: string;
  ruleId?: AiId;
  ruleCode?: string;
  ruleName?: string;
  severity?: string;
  hit?: number;
  evidence?: string;
  reason?: string;
  score?: number;
  reviewStatus?: number;
  reviewNote?: string;
  reviewerId?: AiId;
  reviewerName?: string;
  reviewTime?: string;
  createTime?: string;
}

export interface AiQualityStats {
  ruleCount: number;
  enabledRuleCount: number;
  taskCount: number;
  runningTaskCount: number;
  hitCount: number;
  pendingReview: number;
  confirmedCount: number;
  falsePositiveCount: number;
}

const AI = "/api/v1/ai";

const AiAPI = {
  conversations(params: {
    pageNum: number;
    pageSize: number;
    keyword?: string;
    status?: number;
    customerId?: AiId;
  }) {
    return request<unknown, PageResult<AiConversation>>({
      url: `${AI}/conversations`,
      method: "get",
      params,
    });
  },
  conversation(id: AiId) {
    return request<unknown, AiConversation>({ url: `${AI}/conversations/${id}`, method: "get" });
  },
  createConversation(data: Record<string, unknown>) {
    return request<unknown, AiConversation>({ url: `${AI}/conversations`, method: "post", data });
  },
  deleteConversations(ids: string) {
    return request({ url: `${AI}/conversations/${ids}`, method: "delete" });
  },
  analyze(id: AiId) {
    return request<unknown, AiAnalysis>({
      url: `${AI}/conversations/${id}/analyze`,
      method: "post",
    });
  },
  analysis(id: AiId) {
    return request<unknown, AiAnalysis | null>({
      url: `${AI}/conversations/${id}/analysis`,
      method: "get",
    });
  },
  acceptFollowUp(id: AiId, data: Record<string, unknown>) {
    return request<unknown, AiId>({
      url: `${AI}/analyses/${id}/accept-follow-up`,
      method: "post",
      data,
    });
  },
  acceptTag(id: AiId) {
    return request({ url: `${AI}/tag-suggestions/${id}/accept`, method: "post" });
  },
  rejectTag(id: AiId) {
    return request({ url: `${AI}/tag-suggestions/${id}/reject`, method: "post" });
  },
  modelConfig() {
    return request<unknown, AiModelConfig>({ url: `${AI}/config`, method: "get" });
  },
  saveModelConfig(data: Record<string, unknown>) {
    return request<unknown, AiModelConfig>({ url: `${AI}/config`, method: "put", data });
  },
  testModel() {
    return request<unknown, AiTestResult>({ url: `${AI}/config/test`, method: "post" });
  },
  usageSummary() {
    return request<unknown, AiUsageSummary>({ url: `${AI}/usage/summary`, method: "get" });
  },
  prompts() {
    return request<unknown, AiPromptTemplate[]>({ url: `${AI}/prompts`, method: "get" });
  },
  savePrompt(code: string, data: Record<string, unknown>) {
    return request<unknown, AiPromptTemplate>({
      url: `${AI}/prompts/${code}`,
      method: "put",
      data,
    });
  },
  qualityStats() {
    return request<unknown, AiQualityStats>({ url: `${AI}/quality/stats`, method: "get" });
  },
  qualityRules(params?: { keyword?: string; enabled?: number }) {
    return request<unknown, AiQualityRule[]>({
      url: `${AI}/quality/rules`,
      method: "get",
      params,
    });
  },
  createQualityRule(data: Record<string, unknown>) {
    return request({ url: `${AI}/quality/rules`, method: "post", data });
  },
  updateQualityRule(id: AiId, data: Record<string, unknown>) {
    return request({ url: `${AI}/quality/rules/${id}`, method: "put", data });
  },
  deleteQualityRules(ids: string) {
    return request({ url: `${AI}/quality/rules/${ids}`, method: "delete" });
  },
  qualityTasks(params: { pageNum: number; pageSize: number; keyword?: string; status?: number }) {
    return request<unknown, PageResult<AiQualityTask>>({
      url: `${AI}/quality/tasks`,
      method: "get",
      params,
    });
  },
  createQualityTask(data: Record<string, unknown>) {
    return request<unknown, AiQualityTask>({ url: `${AI}/quality/tasks`, method: "post", data });
  },
  qualityResults(params: {
    pageNum: number;
    pageSize: number;
    taskId?: AiId;
    conversationId?: AiId;
    severity?: string;
    reviewStatus?: number;
    hitOnly?: boolean;
  }) {
    return request<unknown, PageResult<AiQualityResult>>({
      url: `${AI}/quality/results`,
      method: "get",
      params,
    });
  },
  reviewQualityResult(id: AiId, data: Record<string, unknown>) {
    return request({ url: `${AI}/quality/results/${id}/review`, method: "put", data });
  },
};

export default AiAPI;
