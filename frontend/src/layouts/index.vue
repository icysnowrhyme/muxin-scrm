<template>
  <div class="scrm-layout">
    <header class="scrm-header">
      <button class="scrm-logo" type="button" @click="go(section.path)">
        <span class="scrm-logo-mark">木心</span>
        <span class="scrm-logo-dot">·</span>
        <span class="scrm-logo-name">MUXIN</span>
      </button>

      <nav class="scrm-top-nav">
        <button
          v-for="item in navSections"
          :key="item.key"
          type="button"
          :class="['scrm-top-tab', { active: item.key === section.key }]"
          @click="go(item.path)"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </button>
      </nav>

      <div class="scrm-header-right">
        <el-tooltip content="帮助中心" placement="bottom">
          <button class="scrm-icon-btn" type="button" @click="handleHelp">
            <el-icon :size="19"><QuestionFilled /></el-icon>
          </button>
        </el-tooltip>
        <el-tooltip content="系统通知" placement="bottom">
          <button class="scrm-icon-btn scrm-bell" type="button" @click="handleNotice">
            <el-icon :size="19"><Bell /></el-icon>
            <span class="scrm-bell-dot" />
          </button>
        </el-tooltip>
        <el-dropdown trigger="click" @command="handleCommand">
          <button class="scrm-user" type="button">
            <span>{{ userName }}</span>
            <el-icon :size="12"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="subscribe">消息订阅</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <aside class="scrm-sidebar">
      <div class="scrm-side-list">
        <button
          v-for="item in sectionItems"
          :key="item.path"
          type="button"
          :class="['scrm-side-item', { active: isActive(item.path) }]"
          @click="go(item.path)"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </button>
      </div>
      <div class="scrm-side-footer">版本 1.0.0</div>
    </aside>

    <main class="scrm-main">
      <div v-if="showNotice" class="scrm-notice">
        <div class="scrm-notice-label">系统公告</div>
        <div class="scrm-notice-track">{{ currentNotice }}</div>
        <div class="scrm-notice-dots">
          <span
            v-for="(_, index) in announcements"
            :key="index"
            :class="['scrm-notice-dot', { active: index === noticeIndex }]"
            @click="noticeIndex = index"
          />
        </div>
        <button class="scrm-notice-close" type="button" @click="showNotice = false">
          <el-icon :size="15"><Close /></el-icon>
        </button>
      </div>

      <router-view v-slot="{ Component }">
        <transition name="scrm-fade" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "MainLayout" });

import { ElMessage, ElMessageBox } from "element-plus";
import { useUserStore } from "@/stores";
import { useRoute, useRouter } from "vue-router";
import {
  findSectionByPath,
  announcements,
  navSections,
} from "@/navigation/menu";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const section = computed(() => findSectionByPath(route.path));
const isAdmin = computed(() =>
  (userStore.userInfo?.roles ?? []).some((role) => ["ROOT", "ADMIN"].includes(role))
);
const sectionItems = computed(() =>
  section.value.items.filter((item) => item.path !== "/ai/settings" || isAdmin.value)
);
const userName = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || "管理员"
);

const showNotice = ref(true);
const noticeIndex = ref(0);
let noticeTimer: ReturnType<typeof setInterval> | undefined;

const currentNotice = computed(() => announcements[noticeIndex.value] ?? "");

function isActive(path: string): boolean {
  if (route.path === path) return true;
  if (path === "/crm/clues/create") return false;
  return route.path.startsWith(`${path}/`);
}

function go(path: string): void {
  if (route.path !== path) {
    router.push(path);
  }
}

function handleHelp() {
  ElMessage.info("帮助文档正在整理中，可联系系统管理员获取操作手册。");
}

function handleNotice() {
  ElMessage.success("暂无新的系统通知");
}

async function handleCommand(command: string) {
  if (command === "subscribe") {
    ElMessage.info("消息订阅功能演示：已默认订阅系统公告与待办提醒。");
    return;
  }
  if (command === "password") {
    ElMessage.warning("请前往系统设置 - 员工管理修改密码（演示版暂未开放）。");
    return;
  }
  if (command === "logout") {
    await ElMessageBox.confirm("确定要退出登录吗？", "提示", {
      type: "warning",
      confirmButtonText: "退出",
      cancelButtonText: "取消",
    }).catch(() => false);
    userStore.resetAllState();
    router.push("/login");
  }
}

onMounted(() => {
  noticeTimer = setInterval(() => {
    noticeIndex.value = (noticeIndex.value + 1) % announcements.length;
  }, 4500);
});

onBeforeUnmount(() => {
  if (noticeTimer) clearInterval(noticeTimer);
});
</script>

<style lang="scss" scoped>
$primary: #035cff;
$sidebar-bg: #001529;

.scrm-layout {
  min-height: 100vh;
  background: #f5f6f9;
  color: #222;
}

.scrm-header {
  position: fixed;
  inset: 0 0 auto 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  height: 48px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.scrm-logo {
  display: flex;
  flex: 0 0 180px;
  gap: 2px;
  align-items: center;
  height: 48px;
  padding: 0 8px;
  cursor: pointer;
  background: #fff;
  border: 0;

  &-mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    font-size: 16px;
    font-weight: 700;
    color: #fff;
    letter-spacing: 1px;
    background: $primary;
    border-radius: 3px;
  }

  &-dot {
    margin-left: 2px;
    font-size: 20px;
    font-weight: 700;
    line-height: 1;
    color: #8c8c8c;
  }

  &-name {
    font-size: 20px;
    font-weight: 700;
    line-height: 1;
    color: #1f1f1f;
    letter-spacing: 0;
  }
}

.scrm-top-nav {
  display: flex;
  align-items: stretch;
  height: 48px;
  overflow: hidden;
}

.scrm-top-tab {
  position: relative;
  display: inline-flex;
  gap: 8px;
  align-items: center;
  height: 48px;
  padding: 0 20px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-bottom: 2px solid transparent;
  transition: color 0.2s;

  &:hover {
    color: $primary;
  }

  &.active {
    color: $primary;
    border-bottom-color: $primary;
  }

  .el-icon {
    color: inherit;
  }
}

.scrm-header-right {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-left: auto;
  padding-right: 20px;
}

.scrm-icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: #333;
  cursor: pointer;
  background: transparent;
  border: 0;
  border-radius: 4px;

  &:hover {
    color: $primary;
    background: #f2f6ff;
  }
}

.scrm-bell {
  position: relative;
}

.scrm-bell-dot {
  position: absolute;
  top: 5px;
  right: 6px;
  width: 6px;
  height: 6px;
  background: #f56c6c;
  border-radius: 50%;
}

.scrm-user {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 0 4px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  background: transparent;
  border: 0;

  &:hover {
    color: $primary;
  }
}

.scrm-sidebar {
  position: fixed;
  top: 48px;
  bottom: 0;
  left: 0;
  z-index: 900;
  display: flex;
  flex-direction: column;
  width: 180px;
  padding-top: 12px;
  background: $sidebar-bg;
  overflow-y: auto;
}

.scrm-side-list {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.scrm-side-item {
  display: flex;
  flex: 0 0 48px;
  gap: 10px;
  align-items: center;
  width: 100%;
  height: 48px;
  padding: 0 0 0 20px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
  text-align: left;
  white-space: nowrap;
  cursor: pointer;
  background: transparent;
  border: 0;

  &:hover {
    color: #fff;
    background: #0a2545;
  }

  &.active {
    color: #fff;
    background: $primary;
  }
}

.scrm-side-footer {
  padding: 14px 20px 18px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

.scrm-main {
  min-height: 100vh;
  padding: 48px 20px 20px 200px;
  background: #f5f6f9;
}

.scrm-notice {
  display: flex;
  align-items: center;
  height: 36px;
  margin-bottom: 8px;
  background: #e7effd;
}

.scrm-notice-label {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  margin-left: 10px;
  font-size: 13px;
  color: #7f8796;
  white-space: nowrap;
  background: rgba(255, 255, 255, 0.65);
}

.scrm-notice-track {
  flex: 1;
  padding: 0 14px;
  overflow: hidden;
  font-size: 14px;
  color: #303133;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scrm-notice-dots {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-right: 12px;
}

.scrm-notice-dot {
  width: 6px;
  height: 6px;
  cursor: pointer;
  background: #c9d6ea;
  border-radius: 50%;

  &.active {
    background: $primary;
  }
}

.scrm-notice-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-right: 8px;
  color: #8c8c8c;
  cursor: pointer;
  background: transparent;
  border: 0;

  &:hover {
    color: #333;
  }
}

.scrm-fade-enter-active,
.scrm-fade-leave-active {
  transition: opacity 0.14s ease;
}

.scrm-fade-enter-from,
.scrm-fade-leave-to {
  opacity: 0;
}

@media (max-width: 1280px) {
  .scrm-top-tab {
    padding: 0 12px;
    font-size: 14px;
  }

  .scrm-logo {
    flex-basis: 160px;
  }

  .scrm-sidebar {
    width: 170px;
  }

  .scrm-main {
    padding-left: 190px;
  }
}
</style>
