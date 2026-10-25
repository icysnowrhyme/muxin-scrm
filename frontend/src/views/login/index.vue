<template>
  <div class="scrm-login">
    <section class="scrm-login-brand">
      <div class="scrm-login-brand__inner">
        <div class="scrm-login-logo">
          <span class="scrm-login-logo__mark">木心</span>
          <span class="scrm-login-logo__dot">·</span>
          <span class="scrm-login-logo__text">MUXIN</span>
        </div>
        <h1>私域客户增长管理平台</h1>
        <p>从线索进线、客户跟进，到订单成交与业绩分析，一站式完成团队协作与客户经营。</p>
        <div class="scrm-login-features">
          <div class="scrm-login-feature">
            <span class="scrm-login-feature__icon">
              <el-icon :size="18"><Search /></el-icon>
            </span>
            <div>
              <strong>线索全流程管理</strong>
              <small>进线、分配、跟进、转化全程可追溯</small>
            </div>
          </div>
          <div class="scrm-login-feature">
            <span class="scrm-login-feature__icon">
              <el-icon :size="18"><DataAnalysis /></el-icon>
            </span>
            <div>
              <strong>业绩实时统计</strong>
              <small>员工绩效、渠道分析与订单报表</small>
            </div>
          </div>
          <div class="scrm-login-feature">
            <span class="scrm-login-feature__icon">
              <el-icon :size="18"><Lock /></el-icon>
            </span>
            <div>
              <strong>数据权限隔离</strong>
              <small>按角色与部门控制可见范围</small>
            </div>
          </div>
        </div>
      </div>
      <span class="scrm-login-orb scrm-login-orb--one" />
      <span class="scrm-login-orb scrm-login-orb--two" />
    </section>

    <section class="scrm-login-panel">
      <div class="scrm-login-card">
        <div class="scrm-login-card__head">
          <h2>账号登录</h2>
          <p>欢迎使用木心 MUXIN</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginFormData"
          :rules="loginRules"
          size="large"
          :validate-on-rule-change="false"
          @keyup.enter="handleLoginSubmit"
        >
          <el-form-item prop="username">
            <el-input v-model.trim="loginFormData.username" placeholder="请输入用户名" :prefix-icon="UserIcon" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model.trim="loginFormData.password"
              placeholder="请输入密码"
              type="password"
              show-password
              :prefix-icon="LockIcon"
            />
          </el-form-item>
          <el-form-item prop="captchaCode">
            <div class="scrm-login-captcha">
              <el-input
                v-model.trim="loginFormData.captchaCode"
                placeholder="请输入验证码"
                :prefix-icon="KeyIcon"
              />
              <button class="scrm-login-captcha__image" type="button" @click="getCaptcha">
                <el-icon v-if="codeLoading" class="is-loading" :size="16"><Loading /></el-icon>
                <img v-else-if="captchaBase64" :src="captchaBase64" alt="验证码" />
                <el-icon v-else :size="16"><Refresh /></el-icon>
              </button>
            </div>
          </el-form-item>

          <div class="scrm-login-options">
            <el-checkbox v-model="loginFormData.rememberMe">记住我</el-checkbox>
            <span class="scrm-login-forget" @click="handleForget">忘记密码？</span>
          </div>

          <el-button class="scrm-login-submit" type="primary" size="large" :loading="loading" @click="handleLoginSubmit">
            登录
          </el-button>
        </el-form>

        <div class="scrm-login-demo">
          <span>演示账号</span>
          <button type="button" @click="fillDemo('admin')">admin / 123456</button>
          <button type="button" @click="fillDemo('dept_member')">dept_member / 123456</button>
        </div>

        <div class="scrm-login-footer">© 木心 MUXIN · 版本 1.8.6.1</div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: "LoginPage" });

import { DataAnalysis, Key, Loading, Lock, Refresh, Search, User } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import type { FormInstance } from "element-plus";
import AuthAPI from "@/api/auth";
import type { LoginRequest } from "@/api/auth";
import router from "@/router";
import { useUserStore } from "@/stores";
import { AuthStorage } from "@/utils/auth";
import { useRoute } from "vue-router";

const userStore = useUserStore();
const route = useRoute();

const loginFormRef = ref<FormInstance>();
const loading = ref(false);
const codeLoading = ref(false);
const captchaBase64 = ref<string>();

const UserIcon = markRaw(User);
const LockIcon = markRaw(Lock);
const KeyIcon = markRaw(Key);

const loginFormData = ref<LoginRequest>({
  username: "admin",
  password: "123456",
  captchaId: "",
  captchaCode: "",
  rememberMe: AuthStorage.getRememberMe(),
});

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入用户名" }],
  password: [
    { required: true, trigger: "blur", message: "请输入密码" },
    { min: 6, trigger: "blur", message: "密码不能少于 6 位" },
  ],
  captchaCode: [{ required: true, trigger: "blur", message: "请输入验证码" }],
};

function getCaptcha() {
  codeLoading.value = true;
  AuthAPI.getCaptcha()
    .then((data) => {
      loginFormData.value.captchaId = data.captchaId;
      captchaBase64.value = data.captchaBase64;
    })
    .finally(() => {
      codeLoading.value = false;
    });
}

async function handleLoginSubmit() {
  const valid = await loginFormRef.value?.validate().then(
    () => true,
    () => false
  );
  if (!valid) return;

  loading.value = true;
  try {
    await userStore.login(loginFormData.value);
    const redirectPath = (route.query.redirect as string) || "/";
    await router.push(decodeURIComponent(redirectPath));
  } catch {
    getCaptcha();
    loginFormData.value.captchaCode = "";
  } finally {
    loading.value = false;
  }
}

function fillDemo(username: string) {
  loginFormData.value.username = username;
  loginFormData.value.password = "123456";
}

function handleForget() {
  ElMessage.info("演示环境暂未开放找回密码，请联系管理员重置。");
}

onMounted(getCaptcha);
</script>

<style lang="scss" scoped>
$primary: #035cff;

.scrm-login {
  display: flex;
  min-height: 100vh;
  background: #f5f7fb;
}

.scrm-login-brand {
  position: relative;
  display: flex;
  flex: 1.18;
  align-items: center;
  justify-content: center;
  padding: 64px;
  overflow: hidden;
  color: #fff;
  background:
    radial-gradient(circle at 82% 16%, rgb(255 255 255 / 18%), transparent 38%),
    linear-gradient(135deg, #0249cc 0%, #035cff 52%, #2f83ff 100%);

  &__inner {
    position: relative;
    z-index: 2;
    max-width: 520px;
  }

  h1 {
    margin: 42px 0 18px;
    font-size: 40px;
    font-weight: 700;
    line-height: 1.32;
    letter-spacing: 1px;
  }

  p {
    max-width: 460px;
    margin: 0;
    font-size: 16px;
    line-height: 1.9;
    color: rgb(255 255 255 / 78%);
  }
}

.scrm-login-logo {
  display: flex;
  align-items: center;

  &__mark {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 54px;
    height: 54px;
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 1px;
    background: #fff;
    border-radius: 6px;
    color: $primary;
  }

  &__dot {
    margin-left: 6px;
    font-size: 26px;
    font-weight: 700;
    color: rgb(255 255 255 / 65%);
  }

  &__text {
    font-size: 30px;
    font-weight: 700;
    letter-spacing: 1px;
  }
}

.scrm-login-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 52px;
}

.scrm-login-feature {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 14px 16px;
  background: rgb(255 255 255 / 10%);
  border: 1px solid rgb(255 255 255 / 16%);
  border-radius: 8px;
  backdrop-filter: blur(4px);

  &__icon {
    display: inline-flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    background: rgb(255 255 255 / 18%);
    border-radius: 8px;
  }

  strong {
    display: block;
    margin-bottom: 4px;
    font-size: 15px;
    font-weight: 600;
  }

  small {
    font-size: 13px;
    color: rgb(255 255 255 / 72%);
  }
}

.scrm-login-orb {
  position: absolute;
  border: 1px solid rgb(255 255 255 / 14%);
  border-radius: 50%;

  &--one {
    top: -140px;
    right: -80px;
    width: 420px;
    height: 420px;
    background: rgb(255 255 255 / 5%);
  }

  &--two {
    bottom: -220px;
    left: -120px;
    width: 520px;
    height: 520px;
    background: rgb(255 255 255 / 4%);
  }
}

.scrm-login-panel {
  display: flex;
  flex: 0.82;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
}

.scrm-login-card {
  width: 100%;
  max-width: 380px;

  &__head {
    margin-bottom: 30px;

    h2 {
      margin: 0 0 8px;
      font-size: 28px;
      font-weight: 600;
      color: #1f1f1f;
    }

    p {
      margin: 0;
      font-size: 14px;
      color: #909399;
    }
  }
}

.scrm-login-captcha {
  display: flex;
  gap: 12px;
  width: 100%;

  .el-input {
    flex: 1;
  }

  &__image {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 112px;
    height: 40px;
    padding: 0;
    overflow: hidden;
    cursor: pointer;
    background: #f5f7fa;
    border: 1px solid #dcdfe6;
    border-radius: 4px;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.scrm-login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 18px;
}

.scrm-login-forget {
  font-size: 14px;
  color: $primary;
  cursor: pointer;
}

.scrm-login-submit {
  width: 100%;
  height: 44px;
  font-size: 16px;
  letter-spacing: 2px;
}

.scrm-login-demo {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 14px 0 0;
  margin-top: 22px;
  font-size: 12px;
  color: #909399;
  border-top: 1px dashed #ebeef5;

  button {
    padding: 3px 8px;
    font-size: 12px;
    color: #606266;
    cursor: pointer;
    background: #f5f7fa;
    border: 0;
    border-radius: 3px;

    &:hover {
      color: $primary;
      background: #e8f0ff;
    }
  }
}

.scrm-login-footer {
  margin-top: 36px;
  font-size: 12px;
  color: #b6bcc7;
  text-align: center;
}

@media (max-width: 1000px) {
  .scrm-login-brand {
    display: none;
  }

  .scrm-login-panel {
    flex: 1;
    padding: 32px 24px;
  }
}
</style>
