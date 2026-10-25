interface ScrmOption {
  label: string;
  value: number | string;
  type?: "primary" | "success" | "warning" | "info" | "danger";
}

export const SCRM_SOURCES: ScrmOption[] = [
  { label: "官网", value: 1 },
  { label: "转介绍", value: 2 },
  { label: "活动", value: 3 },
  { label: "广告", value: 4 },
  { label: "主动开发", value: 5 },
  { label: "其他", value: 6 },
];

export const SCRM_LEVELS: ScrmOption[] = [
  { label: "A 类 · 重点", value: "A", type: "danger" },
  { label: "B 类 · 意向", value: "B", type: "warning" },
  { label: "C 类 · 普通", value: "C", type: "primary" },
  { label: "D 类 · 观察", value: "D", type: "info" },
];

export const SCRM_CUSTOMER_STATUSES: ScrmOption[] = [
  { label: "潜在客户", value: 1, type: "info" },
  { label: "跟进中", value: 2, type: "primary" },
  { label: "已成交", value: 3, type: "success" },
  { label: "已流失", value: 4, type: "danger" },
];

export const SCRM_ORDER_STATUSES: ScrmOption[] = [
  { label: "待审核", value: 1, type: "warning" },
  { label: "已确认", value: 2, type: "primary" },
  { label: "已完成", value: 3, type: "success" },
  { label: "已取消", value: 4, type: "info" },
];

export function sourceLabel(value?: number | null): string {
  return SCRM_SOURCES.find((item) => item.value === value)?.label ?? "其他";
}

export function levelTag(value?: string | null): "danger" | "warning" | "primary" | "info" {
  return (SCRM_LEVELS.find((item) => item.value === value)?.type ?? "info") as
    | "danger"
    | "warning"
    | "primary"
    | "info";
}

export function intentLabel(level?: string | null): string {
  if (level === "A") return "高意向";
  if (level === "B") return "中意向";
  if (level === "C") return "一般";
  return "低意向";
}

export function leadTypeLabel(level?: string | null): string {
  return level ? `${level} 类线索` : "普通线索";
}

export function customerCode(id?: string | number | null): string {
  if (id === undefined || id === null) return "-";
  return `C${String(id).padStart(6, "0")}`;
}

export function orderStatusLabel(value?: number | null): {
  label: string;
  type: "primary" | "success" | "warning" | "info" | "danger";
} {
  const item = SCRM_ORDER_STATUSES.find((option) => option.value === value);
  return { label: item?.label ?? "未知", type: item?.type ?? "info" };
}

export function genderLabel(value?: number | null): string {
  if (value === 1) return "男";
  if (value === 2) return "女";
  return "未知";
}

export function formatMoney(value?: number | string | null): string {
  const num = Number(value ?? 0);
  return num.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
