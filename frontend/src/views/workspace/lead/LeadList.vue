<template>
  <div class="scrm-page">
    <div class="scrm-page-title">{{ pageTitle }}</div>

    <div class="scrm-filter-card">
      <div class="scrm-filter-row">
        <div class="scrm-filter-field scrm-filter-field--wide">
          <span class="scrm-filter-label">关键字</span>
          <el-input v-model="query.keyword" clearable placeholder="请输入姓名、手机号、线索编码" @keyup.enter="handleSearch" />
        </div>
        <div class="scrm-filter-field">
          <span class="scrm-filter-label">线索类型</span>
          <el-select v-model="query.level" clearable placeholder="请选择线索类型">
            <el-option v-for="item in SCRM_LEVELS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="scrm-filter-field">
          <span class="scrm-filter-label">转化目标</span>
          <el-select v-model="query.status" clearable placeholder="请选择转化目标">
            <el-option v-for="item in SCRM_CUSTOMER_STATUSES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="scrm-filter-actions">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <button class="scrm-advanced" type="button" @click="advanced = !advanced">
            高级搜索
            <el-icon :class="{ 'is-open': advanced }"><ArrowDown /></el-icon>
          </button>
        </div>
      </div>

      <div v-if="advanced" class="scrm-filter-row" style="margin-top: 14px">
        <div class="scrm-filter-field">
          <span class="scrm-filter-label">线索来源</span>
          <el-select v-model="query.source" clearable placeholder="请选择来源">
            <el-option v-for="item in SCRM_SOURCES" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
        <div class="scrm-filter-field">
          <span class="scrm-filter-label">创建时间</span>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="-"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </div>
      </div>
    </div>

    <div class="scrm-panel">
      <div class="scrm-panel-header">
        <div class="scrm-panel-title">线索列表</div>
        <div class="scrm-toolbar">
          <el-button type="primary" @click="openCreate">新建线索</el-button>
          <el-button plain @click="ElMessage.info('自定义列演示：当前展示全部字段')">自定义列</el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="rows" border row-key="id" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="44" />
        <el-table-column label="编码" width="110">
          <template #default="{ row }">{{ customerCode(row.id) }}</template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="110" show-overflow-tooltip />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="最近跟进" width="160">
          <template #default="{ row }">{{ row.lastFollowTime || "-" }}</template>
        </el-table-column>
        <el-table-column label="线索类型" width="120">
          <template #default="{ row }">{{ leadTypeLabel(row.level) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="100">
          <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
        </el-table-column>
        <el-table-column label="意向" width="110">
          <template #default="{ row }">
            <el-tag :type="levelTag(row.level)" size="small" effect="plain">
              {{ intentLabel(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="150">
          <template #default="{ row }">
            <template v-if="row.tags?.length">
              <el-tag
                v-for="tag in row.tags.slice(0, 2)"
                :key="tag.id"
                size="small"
                effect="plain"
                :color="`${tag.color || '#035cff'}14`"
                :style="{ color: tag.color || '#035cff', borderColor: `${tag.color || '#035cff'}55` }"
                class="mr4"
              >
                {{ tag.name }}
              </el-tag>
              <span v-if="row.tags.length > 2" class="scrm-muted">+{{ row.tags.length - 2 }}</span>
            </template>
            <span v-else class="scrm-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || "-" }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ row.createTime || "-" }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="isPool" link type="primary" @click="handleClaim(row)">领取</el-button>
            <el-button v-else-if="row.status !== 3" link type="primary" @click="handleConvert(row)">转客户</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="scrm-empty scrm-empty--gray">暂无数据</div>
        </template>
      </el-table>

      <div class="scrm-list-footer">
        <span class="scrm-selected">已选 {{ selection.length }} 条</span>
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="load"
          @current-change="load"
        />
      </div>
    </div>

    <CustomerFormDialog
      v-model="formVisible"
      mode="lead"
      :record="editing"
      @saved="handleSaved"
    />

    <el-drawer v-model="detailVisible" title="线索详情" size="460px">
      <el-descriptions v-if="detail" :column="1" border label-width="110px">
        <el-descriptions-item label="线索编码">{{ customerCode(detail.id) }}</el-descriptions-item>
        <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || "-" }}</el-descriptions-item>
        <el-descriptions-item label="微信号">{{ detail.wechat || "-" }}</el-descriptions-item>
        <el-descriptions-item label="线索类型">{{ leadTypeLabel(detail.level) }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ sourceLabel(detail.source) }}</el-descriptions-item>
        <el-descriptions-item label="意向">
          <el-tag :type="levelTag(detail.level)" size="small">{{ intentLabel(detail.level) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <template v-if="detail.tags?.length">
            <el-tag v-for="tag in detail.tags" :key="tag.id" size="small" class="mr4" effect="plain">
              {{ tag.name }}
            </el-tag>
          </template>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="最近跟进">{{ detail.lastFollowTime || "-" }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || "-" }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime || "-" }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "LeadList" });

import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute } from "vue-router";
import { CustomerAPI } from "@/api/scrm";
import type { CustomerDetail, CustomerItem, CustomerQuery } from "@/api/scrm/types";
import {
  SCRM_CUSTOMER_STATUSES,
  SCRM_LEVELS,
  SCRM_SOURCES,
  customerCode,
  intentLabel,
  leadTypeLabel,
  levelTag,
  sourceLabel,
} from "@/navigation/constants";
import CustomerFormDialog from "../components/CustomerFormDialog.vue";

const route = useRoute();

const pageTitle = computed(() => (route.meta.title as string) || "管理线索");
const isPool = computed(() => route.meta.scope === "pool");

const loading = ref(false);
const rows = ref<CustomerItem[]>([]);
const total = ref(0);
const selection = ref<CustomerItem[]>([]);
const advanced = ref(false);
const dateRange = ref<[string, string] | null>(null);

const query = reactive<CustomerQuery & { status?: number }>({
  pageNum: 1,
  pageSize: 10,
  keyword: "",
  level: "",
  source: undefined,
  status: undefined,
});

const formVisible = ref(false);
const editing = ref<CustomerItem | null>(null);
const detailVisible = ref(false);
const detail = ref<CustomerDetail | null>(null);

function statusLabel(status?: number): string {
  return SCRM_CUSTOMER_STATUSES.find((item) => item.value === status)?.label ?? "未知";
}

function statusTag(status?: number): "primary" | "success" | "warning" | "info" | "danger" {
  return (SCRM_CUSTOMER_STATUSES.find((item) => item.value === status)?.type as never) ?? "info";
}

async function load() {
  loading.value = true;
  try {
    const params: CustomerQuery = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword || undefined,
      level: query.level || undefined,
      source: query.source,
      status: query.status,
    };
    if (dateRange.value?.length === 2) {
      params.startDate = dateRange.value[0];
      params.endDate = dateRange.value[1];
    }

    if (isPool.value) {
      const result = await CustomerAPI.getPoolPage(params);
      rows.value = result.list;
      total.value = result.total;
    } else {
      if (route.meta.scope === "todo") {
        params.status = 1;
        params.statuses = undefined;
      } else if (route.meta.scope === "manage") {
        params.status = undefined;
        params.statuses = "1,2";
      }
      const result = await CustomerAPI.getPage(params);
      rows.value = result.list;
      total.value = result.total;
    }
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  load();
}

function handleReset() {
  query.keyword = "";
  query.level = "";
  query.source = undefined;
  query.status = undefined;
  query.pageNum = 1;
  dateRange.value = null;
  load();
}

function handleSelectionChange(selectionRows: CustomerItem[]) {
  selection.value = selectionRows;
}

function openCreate() {
  editing.value = null;
  formVisible.value = true;
}

function openEdit(row: any) {
  editing.value = row;
  formVisible.value = true;
}

async function openDetail(row: any) {
  detail.value = null;
  detailVisible.value = true;
  detail.value = await CustomerAPI.getDetail(row.id);
}

function handleSaved() {
  load();
}

async function handleClaim(row: any) {
  await ElMessageBox.confirm(`确定领取线索「${row.name}」吗？`, "领取线索", {
    type: "warning",
    confirmButtonText: "确认领取",
    cancelButtonText: "取消",
  });
  await CustomerAPI.claim(row.id);
  ElMessage.success("领取成功");
  load();
}

async function handleConvert(row: any) {
  await ElMessageBox.confirm(`确定将线索「${row.name}」转化为客户吗？`, "转化客户", {
    type: "warning",
    confirmButtonText: "确认转化",
    cancelButtonText: "取消",
  });
  const data = await CustomerAPI.getDetail(row.id);
  await CustomerAPI.update(row.id, {
    name: data.name,
    phone: data.phone,
    wechat: data.wechat,
    gender: data.gender,
    level: data.level,
    source: data.source,
    status: 2,
    remark: data.remark,
    tagIds: (data.tags ?? []).map((tag) => tag.id),
  });
  ElMessage.success("转化成功，已进入客户列表");
  load();
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除线索「${row.name}」吗？`, "删除确认", {
    type: "warning",
    confirmButtonText: "删除",
    cancelButtonText: "取消",
  });
  await CustomerAPI.remove(String(row.id));
  ElMessage.success("删除成功");
  load();
}

watch(
  () => route.fullPath,
  () => {
    query.pageNum = 1;
    load();
    if (route.meta.createMode) {
      nextTick(() => openCreate());
    }
  }
);

onMounted(() => {
  load();
  if (route.meta.createMode) {
    nextTick(() => openCreate());
  }
});
</script>

<style scoped>
.scrm-advanced {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 0;
  font-size: 14px;
  color: #035cff;
  cursor: pointer;
  background: transparent;
  border: 0;
}

.scrm-advanced .el-icon {
  transition: transform 0.2s;
}

.scrm-advanced .el-icon.is-open {
  transform: rotate(180deg);
}

.scrm-list-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.scrm-selected {
  font-size: 13px;
  color: #909399;
}

.mr4 {
  margin-right: 4px;
}
</style>
