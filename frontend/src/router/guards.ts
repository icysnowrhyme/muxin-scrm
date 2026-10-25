import NProgress from "@/plugins/nprogress";
import router from "@/router";
import { useUserStore } from "@/stores";
import { ElMessage } from "element-plus";

export function setupPermissionGuard() {
  const whiteList = ["/login"];

  router.beforeEach(async (to, _from) => {
    NProgress.start();
    const userStore = useUserStore();

    try {
      const title = to.meta?.title as string | undefined;
      document.title = title ? `${title} - 木心 MUXIN` : "木心 MUXIN";

      if (!userStore.isLoggedIn()) {
        if (whiteList.includes(to.path)) {
          return;
        }
        NProgress.done();
        return `/login?redirect=${encodeURIComponent(to.fullPath)}`;
      }

      if (to.path === "/login") {
        return { path: "/" };
      }

      if (!userStore.userInfo?.roles?.length) {
        await userStore.getUserInfo();
      }

      const roles = userStore.userInfo?.roles ?? [];
      if (to.path === "/ai/settings" && !roles.some((role) => ["ROOT", "ADMIN"].includes(role))) {
        ElMessage.warning("仅管理员可访问 AI 设置");
        return { path: "/ai/workbench" };
      }

      if (to.matched.length === 0) {
        return "/404";
      }
    } catch (error) {
      console.error("Route guard error:", error);
      await userStore.resetAllState();
      NProgress.done();
      return "/login";
    }
  });

  router.afterEach(() => {
    NProgress.done();
  });
}
