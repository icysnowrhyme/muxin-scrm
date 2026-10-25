<!-- 图标选择器示例 -->
<template>
  <div class="page-container">
    <IconSelect v-model="iconName" />
  </div>
</template>

<script setup lang="ts">
defineOptions({
  name: "DemoIconSelect",
});

// ❌ 踩坑：页面文件名 icon-select.vue 与模板标签 <icon-select> 同名会触发 Vue 隐式自引用
// （unplugin-vue-components 对同名组件禁用自动导入，运行时解析到页面自身 → 无限递归栈溢出白屏）
// ✅ 显式导入公共组件，优先级高于隐式自引用，杜绝递归
import IconSelect from "@/components/IconSelect/index.vue";

// element-plus 图标格式以 el-icon-开头
const iconName = ref("el-icon-edit");
// 本地 SVG 图标格式为 src/assets/icons 下的文件名，不需要 svg 后缀
</script>
