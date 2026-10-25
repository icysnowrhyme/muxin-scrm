<!--
 * 基于 ECharts 的 Vue 3 图表组件
 * 原始实现的版权与许可信息见 THIRD_PARTY_NOTICES.md
-->

<template>
  <div ref="chartRef" :style="{ width, height }"></div>
</template>

<script setup lang="ts">
import * as echarts from "echarts/core";
import {
  BarChart,
  EffectScatterChart,
  LineChart,
  LinesChart,
  MapChart,
  PieChart,
} from "echarts/charts";
import {
  GeoComponent,
  GridComponent,
  LegendComponent,
  TooltipComponent,
  VisualMapComponent,
} from "echarts/components";
// 必须引入一个渲染器（CanvasRenderer 或 SVGRenderer），否则图表画不出来
import { CanvasRenderer } from "echarts/renderers";

import { useResizeObserver } from "@vueuse/core";

// 按需注册组件
echarts.use([
  CanvasRenderer,
  BarChart,
  LineChart,
  PieChart,
  MapChart,
  EffectScatterChart,
  LinesChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  VisualMapComponent,
  GeoComponent,
]);

/**
 * ECharts 图表容器：按配置渲染并自适应尺寸
 */
const props = defineProps<{
  options: echarts.EChartsCoreOption;
  width?: string;
  height?: string;
}>();

const chartRef = ref<HTMLDivElement | null>(null);
let chartInstance: echarts.ECharts | null = null;

/**
 * 初始化图表
 */
const initChart = () => {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value);
    if (props.options) {
      chartInstance.setOption(props.options);
    }
  }
};

// 监听尺寸变化，自动调整
useResizeObserver(chartRef, () => {
  chartInstance?.resize();
});

// 监听 options 变化，更新图表
watch(
  () => props.options,
  (newOptions) => {
    if (chartInstance && newOptions) {
      chartInstance.setOption(newOptions);
    }
  },
  { deep: true }
);

onMounted(() => {
  nextTick(() => initChart());
});

onBeforeUnmount(() => {
  chartInstance?.dispose();
});
</script>
