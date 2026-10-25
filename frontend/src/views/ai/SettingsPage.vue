<template>
  <div class="ym-page">
    <div class="ym-page-title">AI 设置</div>
    <p class="ym-subtab-desc">
      统一管理模型接入、用量与 Prompt 模板。默认 Mock 演示模式无需密钥即可体验全部功能；配置 OpenAI 兼容接口后自动切换真实模型。
    </p>

    <div class="ym-subtabs">
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

    <div v-if="activeTab === 'model'" class="ym-panel" style="margin-top: 0">
      <div class="ym-panel-header">
        <div class="ym-panel-title">模型配置</div>
        <el-tag v-if="config?.provider === 'mock'" type="warning" size="small" effect="plain">
          Mock 演示模式
        </el-tag>
        <el-tag v-else type="success" size="small" effect="plain">真实模型</el-tag>
      </div>

      <div class="ym-info-alert">
        <el-icon><InfoFilled /></el-icon>
        <p>
          支持 OpenAI 协议兼容的模型服务（DeepSeek / 通义千问 / 豆包等），API 地址填写到 <code>/v1</code> 层即可。
          未配置密钥或真实模型调用失败时，将自动回落 Mock，保证页面流程可用。
        </p>
      </div>

      <el-form :model="configForm" label-width="110px" class="ai-settings-form" v-loading="configLoading">
        <el-form-item label="配置名称" required>
          <el-input v-model="configForm.name" maxlength="64" />
        </el-form-item>
        <el-form-item label="模型提供方" required>
          <el-radio-group v-model="configForm.provider">
            <el-radio value="mock">Mock 演示</el-radio>
            <el-radio value="openai">OpenAI 兼容</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="API 地址">
          <el-input v-model="configForm.baseUrl" placeholder="https://api.deepseek.com/v1" />
        </el-form-item>
        <el-form-item label="API Key">
          <div class="ai-key-field">
            <el-input
              v-model="configForm.apiKey"
              type="password"
              show-password
              :placeholder="apiKeyPlaceholder"
            />
            <el-button v-if="config?.hasApiKey" link type="danger" @click="clearApiKey">清空密钥</el-button>
          </div>
          <div class="ym-muted ai-form-tip">留空表示保持已保存的密钥不变；密钥使用 AES 加密存储。</div>
        </el-form-item>
        <el-form-item label="模型名称" required>
          <el-input v-model="configForm.model" placeholder="如 deepseek-chat、qwen-plus" />
        </el-form-item>
        <el-form-item label="温度">
          <el-input-number v-model="configForm.temperature" :min="0" :max="2" :step="0.1" :precision="1" />
          <span class="ym-muted ai-form-tip" style="margin-left: 12px">数值越低输出越稳定</span>
        </el-form-item>
        <el-form-item label="最大输出">
          <el-input-number v-model="configForm.maxTokens" :min="128" :max="8192" :step="128" controls-position="right" />
          <span class="ym-muted ai-form-tip" style="margin-left: 12px">tokens</span>
        </el-form-item>
        <el-form-item label="启用配置">
          <el-switch v-model="configForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="configForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
          <el-button plain :loading="testing" @click="handleTest">测试连通性</el-button>
          <span v-if="config?.updateTime" class="ym-muted ai-form-tip" style="margin-left: 12px">
            最近更新：{{ config.updateTime }}
          </span>
        </el-form-item>
      </el-form>

      <div v-if="testResult" class="ai-test-result">
        <div class="ai-test-result__head">
          <span>连通性测试</span>
          <el-tag :type="testResult.mocked ? 'warning' : 'success'" size="small" effect="plain">
            {{ testResult.mocked ? "Mock 回退" : "真实模型" }}
          </el-tag>
        </div>
        <p>{{ testResult.message }}</p>
        <div class="ym-muted">
          {{ testResult.provider }} / {{ testResult.model }} · {{ testResult.latencyMs }} ms
        </div>
      </div>
    </div>

    <div v-else-if="activeTab === 'usage'" class="ym-panel" style="margin-top: 0">
      <div class="ym-panel-header">
        <div class="ym-panel-title">用量统计</div>
        <el-button plain :loading="usageLoading" @click="loadUsage">刷新</el-button>
      </div>

      <div class="ym-stat-grid ai-usage-grid">
        <div class="ym-stat-card">
          <div class="ym-stat-card__label">调用次数</div>
          <div class="ym-stat-card__value">{{ usage?.totalCalls ?? 0 }}</div>
          <div class="ym-stat-card__foot">累计 AI 请求</div>
        </div>
        <div class="ym-stat-card">
          <div class="ym-stat-card__label">Token 消耗</div>
          <div class="ym-stat-card__value">{{ usage?.totalTokens ?? 0 }}</div>
          <div class="ym-stat-card__foot">输入与输出合计</div>
        </div>
        <div class="ym-stat-card">
          <div class="ym-stat-card__label">成功率</div>
          <div class="ym-stat-card__value">
            {{ usage?.successRate ?? 0 }}<span class="ym-stat-card__suffix">%</span>
          </div>
          <div class="ym-stat-card__foot">成功 {{ usage?.successCalls ?? 0 }} 次</div>
        </div>
        <div class="ym-stat-card">
          <div class="ym-stat-card__label">平均耗时</div>
          <div class="ym-stat-card__value">
            {{ usage?.avgLatency ?? 0 }}<span class="ym-stat-card__suffix">ms</span>
          </div>
          <div class="ym-stat-card__foot">单次调用平均耗时</div>
        </div>
      </div>

      <el-table v-loading="usageLoading" :data="usage?.features ?? []" border class="ai-usage-table">
        <el-table-column label="功能" min-width="160">
          <template #default="{ row }">{{ featureLabel(row.feature) }}</template>
        </el-table-column>
        <el-table-column prop="calls" label="调用次数" width="120" align="center" />
        <el-table-column prop="successCalls" label="成功次数" width="120" align="center" />
        <el-table-column prop="tokens" label="Token" width="120" align="center" />
        <el-table-column label="平均耗时" width="130" align="center">
          <template #default="{ row }">{{ row.avgLatency }} ms</template>
        </el-table-column>
        <template #empty>
          <div class="ym-empty ym-empty--gray">暂无调用记录</div>
        </template>
      </el-table>
    </div>

    <div v-else class="ym-panel" style="margin-top: 0">
      <div class="ym-panel-header">
        <div class="ym-panel-title">Prompt 模板</div>
        <span class="ym-muted">修改后自动递增版本号，仅影响后续分析</span>
      </div>

      <div v-loading="promptLoading" class="ai-prompt-list">
        <div v-for="item in prompts" :key="item.code" class="ai-prompt-card">
          <div class="ai-prompt-head">
            <div>
              <span class="ai-prompt-name">{{ item.name }}</span>
              <el-tag size="small" effect="plain" class="ai-prompt-code">{{ item.code }}</el-tag>
              <span class="ym-muted">v{{ item.version ?? 1 }}</span>
            </div>
            <span v-if="item.updateTime" class="ym-muted">{{ item.updateTime }}</span>
          </div>
          <el-form label-width="110px">
            <el-form-item label="System Prompt">
              <el-input v-model="item.systemPrompt" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="User Prompt">
              <el-input v-model="item.userTemplate" type="textarea" :rows="6" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="promptSaving === item.code" @click="handleSavePrompt(item)">
                保存模板
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        <div v-if="!promptLoading && !prompts.length" class="ym-empty ym-empty--gray">暂无 Prompt 模板</div>
      </div>
      <p class="ym-muted ai-form-tip">
        摘要模板占位符：{customer}、{conversation}；质检模板占位符：{rules}、{conversation}。请保留占位符以免分析失败。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "AiSettings" });

import { ElMessage } from "element-plus";
import AiAPI, {
  type AiModelConfig,
  type AiPromptTemplate,
  type AiTestResult,
  type AiUsageSummary,
} from "@/api/ai";

const tabs = [
  { key: "model", label: "模型配置" },
  { key: "usage", label: "用量统计" },
  { key: "prompt", label: "Prompt 模板" },
];

const activeTab = ref("model");

const config = ref<AiModelConfig | null>(null);
const configLoading = ref(false);
const saving = ref(false);
const testing = ref(false);
const testResult = ref<AiTestResult | null>(null);

const usage = ref<AiUsageSummary | null>(null);
const usageLoading = ref(false);

const prompts = ref<AiPromptTemplate[]>([]);
const promptLoading = ref(false);
const promptSaving = ref<string | null>(null);

const configForm = reactive({
  name: "",
  provider: "mock",
  baseUrl: "",
  apiKey: "",
  model: "",
  temperature: 0.3,
  maxTokens: 1200,
  enabled: 1,
  isDefault: 1,
});

const apiKeyPlaceholder = computed(() => {
  if (configForm.apiKey === "__CLEAR__") return "保存后将清空已配置的密钥";
  return config.value?.hasApiKey
    ? `已配置：${config.value.apiKeyMasked || "****"}，留空保持不变`
    : "未配置 API Key";
});

function featureLabel(feature?: string) {
  const labels: Record<string, string> = {
    summary: "会话摘要",
    "summary-fallback": "会话摘要（Mock 回退）",
    quality: "智能质检",
    "quality-fallback": "智能质检（Mock 回退）",
    test: "连通性测试",
    "test-fallback": "连通性测试（Mock 回退）",
  };
  return feature ? labels[feature] ?? feature : "-";
}

async function loadConfig() {
  configLoading.value = true;
  try {
    const data = await AiAPI.modelConfig();
    config.value = data;
    Object.assign(configForm, {
      name: data.name,
      provider: data.provider,
      baseUrl: data.baseUrl ?? "",
      apiKey: "",
      model: data.model,
      temperature: Number(data.temperature ?? 0.3),
      maxTokens: data.maxTokens ?? 1200,
      enabled: data.enabled ?? 1,
      isDefault: data.isDefault ?? 1,
    });
  } finally {
    configLoading.value = false;
  }
}

async function loadUsage() {
  usageLoading.value = true;
  try {
    usage.value = await AiAPI.usageSummary();
  } finally {
    usageLoading.value = false;
  }
}

async function loadPrompts() {
  promptLoading.value = true;
  try {
    prompts.value = await AiAPI.prompts();
  } finally {
    promptLoading.value = false;
  }
}

function clearApiKey() {
  configForm.apiKey = "__CLEAR__";
}

async function handleSave() {
  if (!configForm.name.trim() || !configForm.model.trim()) {
    ElMessage.warning("请填写配置名称与模型名称");
    return;
  }
  if (configForm.provider === "openai" && !configForm.baseUrl.trim()) {
    ElMessage.warning("OpenAI 兼容模式需要填写 API 地址");
    return;
  }
  saving.value = true;
  try {
    await AiAPI.saveModelConfig({
      name: configForm.name,
      provider: configForm.provider,
      baseUrl: configForm.baseUrl || undefined,
      apiKey: configForm.apiKey || undefined,
      model: configForm.model,
      temperature: configForm.temperature,
      maxTokens: configForm.maxTokens,
      enabled: configForm.enabled,
      isDefault: configForm.isDefault,
    });
    ElMessage.success("模型配置已保存");
    await loadConfig();
  } finally {
    saving.value = false;
  }
}

async function handleTest() {
  testing.value = true;
  testResult.value = null;
  try {
    testResult.value = await AiAPI.testModel();
    if (testResult.value.mocked) {
      ElMessage.warning("当前走 Mock 演示通道，请检查提供方与密钥配置");
    } else {
      ElMessage.success("模型连通正常");
    }
  } finally {
    testing.value = false;
  }
}

async function handleSavePrompt(item: AiPromptTemplate) {
  if (!item.systemPrompt.trim() || !item.userTemplate.trim()) {
    ElMessage.warning("System Prompt 与 User Prompt 不能为空");
    return;
  }
  promptSaving.value = item.code;
  try {
    const saved = await AiAPI.savePrompt(item.code, {
      name: item.name,
      systemPrompt: item.systemPrompt,
      userTemplate: item.userTemplate,
      enabled: item.enabled ?? 1,
    });
    Object.assign(item, saved);
    ElMessage.success(`模板已保存，当前版本 v${saved.version ?? item.version ?? 1}`);
  } finally {
    promptSaving.value = null;
  }
}

watch(activeTab, (value) => {
  if (value === "usage") loadUsage();
  if (value === "prompt") loadPrompts();
});

onMounted(() => {
  loadConfig();
  loadUsage();
  loadPrompts();
});
</script>

<style lang="scss" scoped>
.ai-settings-form {
  max-width: 760px;
}

.ai-key-field {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;

  .el-input {
    flex: 1;
  }
}

.ai-form-tip {
  font-size: 12px;
  line-height: 20px;
}

.ai-test-result {
  max-width: 760px;
  padding: 14px 16px;
  margin-top: 8px;
  background: #f7f9fc;
  border: 1px solid #e7ecf5;
  border-radius: 4px;

  &__head {
    display: flex;
    gap: 10px;
    align-items: center;
    font-size: 13px;
    font-weight: 500;
    color: #303133;
  }

  p {
    margin: 8px 0 6px;
    font-size: 14px;
    line-height: 22px;
    color: #303133;
  }
}

.ai-usage-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 16px;
}

.ai-usage-table {
  margin-top: 4px;
}

.ai-prompt-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-prompt-card {
  padding: 16px;
  background: #fafbfd;
  border: 1px solid #eef0f4;
  border-radius: 4px;
}

.ai-prompt-head {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  font-size: 13px;
}

.ai-prompt-name {
  margin-right: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #222;
}

.ai-prompt-code {
  margin-right: 8px;
}

@media (width <= 1400px) {
  .ai-usage-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
