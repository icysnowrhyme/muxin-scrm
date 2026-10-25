# 贡献指南

感谢你愿意改进 MUXIN AI SCRM。提交前请确认改动符合项目定位，并且没有引入真实业务数据、密钥或第三方受版权保护的素材。

## 可以贡献的内容

- 修复已有页面的交互、样式、类型或接口错误；
- 增加通用组件、可访问性改进与前端性能优化；
- 完善 AI 中台能力：模型适配、Prompt 设计、质检规则、评测集；
- 改进文档、启动流程、测试与部署脚本；
- 用独立实现完善占位页面。

## 不接受的内容

- 真实客户、员工、订单、会话或个人数据；
- 绕过登录、绕过授权或抓取第三方系统数据的脚本；
- 明文密钥、数据库密码、JWT 密钥、模型 API Key 或云平台凭据；
- 来源不明的图片、字体、图标包或受版权保护的素材；
- 用于冒充官方、商业误导或侵犯他人权益的内容。

## 开发流程

1. 从 `main` 创建分支：`feat/xxx`、`fix/xxx`、`docs/xxx`。
2. 按 `.env.example` 配置本地环境并完成启动。
3. 修改后运行：

   ```bash
   cd frontend
   pnpm type-check
   pnpm build

   cd ../backend
   mvn -q -DskipTests package
   ```

4. 在 Pull Request 中描述改动、验证步骤和截图（如需要）。截图必须来自本地演示数据并完成脱敏。

## 代码约定

### 前端

- 业务页面放在 `frontend/src/views/workspace/`，AI 页面放在 `frontend/src/views/ai/`，复用 `styles/theme.scss` 中的设计令牌。
- 静态路由集中在 `frontend/src/router/index.ts`；新增导航时同步更新 `frontend/src/navigation/menu.ts`。
- API 封装集中在 `frontend/src/api/` 下的 `analytics/`、`scrm/`、`ai/`。
- 筛选、表格、分页和详情抽屉遵循现有页面模式，避免为单个页面复制大段样式。
- 后端返回的 `Long` 字段可能是字符串，参与计算前使用 `Number()`。
- 提交前通过 `pnpm type-check` 与 `pnpm build`。

### 后端

- 业务模块放在 `com.muxin.scrm.module.*`；AI 能力统一放在 `module/ai`。
- 保持统一响应结构，分页使用 `PageResult`。
- 涉及聚合查询时显式处理空数据、除零和时区边界。
- 显式更新 `update_time` 的场景在 Service 内写入，避免 `strictUpdateFill` 保留旧值。
- 新增 SQL 变更时同步更新 `sql/muxin_scrm.sql` 或 `sql/muxin_ai.sql`，并说明升级路径。
- 提交前通过 `mvn -q -DskipTests package`。

## 提交信息

```text
feat(order): add batch audit confirmation
fix(report): guard division by zero in win rate
feat(ai): persist quality task failure reason
docs(design): document navigation tokens
```

## 安全

请勿公开披露未修复的漏洞。报告方式见 [SECURITY.md](SECURITY.md)。
