# API 说明

本文说明 MUXIN AI SCRM 的接口约定、认证方式、业务接口与 AI 中台接口。服务启动后可访问：

- Swagger UI：<http://localhost:8001/swagger-ui/index.html>
- OpenAPI JSON：<http://localhost:8001/v3/api-docs>

接口定义以代码为准；本文用于说明调用方式和业务语义，不替代 OpenAPI。

## 1. 基础约定

### 地址与协议

| 环境 | 前端地址 | 代理前缀 | 实际后端 |
| --- | --- | --- | --- |
| 开发 | `http://localhost:3001` | `/dev-api` | `http://localhost:8001` |
| 生产 | 静态站点 | `/prod-api` | 由 Nginx 反代到后端 |

- 接口前缀：`/api/v1`
- 数据格式：`application/json;charset=utf-8`
- 鉴权头：`Authorization: Bearer <accessToken>`
- 时区：`Asia/Shanghai`

### 统一响应

```json
{
  "code": "00000",
  "msg": "成功",
  "data": {}
}
```

分页响应的 `data`：

```json
{
  "code": "00000",
  "msg": "成功",
  "data": {
    "list": [],
    "total": 0
  }
}
```

### 分页参数

| 参数 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `pageNum` | integer | `1` | 页码 |
| `pageSize` | integer | `10` | 每页条数 |
| `sortBy` | string | — | 仅允许 `create_time`、`update_time` |
| `order` | string | — | `ASC` 或 `DESC` |

### 数据类型约定

- `bigint` 主键统一序列化为字符串，前端必须先 `Number(...)` 再参与运算。
- 金额为 `decimal`，前端展示前同样转数值。
- 日期为 `yyyy-MM-dd`，日期时间为 ISO 格式。
- 逻辑删除由后端处理，业务响应不返回 `is_deleted`。

### 常用错误码

| 错误码 | 含义 |
| --- | --- |
| `00000` | 成功 |
| `A0210` | 用户名或密码错误 |
| `A0230` | 访问令牌无效或已过期 |
| `A0240` | 验证码错误或已过期 |
| `A0301` | 访问未授权 |
| `A0410` | 请求必填参数为空 |
| `B0001` | 系统执行出错 |
| `C0300` | 数据库服务出错 |

HTTP 状态码在业务失败时可能仍为 200，前端应以响应体 `code` 为准。Axios 拦截器已经统一处理提示与令牌续期。

## 2. 认证接口

### 获取验证码

```http
GET /api/v1/auth/captcha
```

```json
{
  "code": "00000",
  "msg": "成功",
  "data": {
    "captchaId": "b6b6...",
    "captchaBase64": "data:image/png;base64,..."
  }
}
```

验证码存放在 Redis，登录时一次性校验。

### 账号密码登录

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "username": "admin",
  "password": "123456",
  "captchaId": "b6b6...",
  "captchaCode": "9"
}
```

```json
{
  "code": "00000",
  "msg": "成功",
  "data": {
    "tokenType": "Bearer",
    "accessToken": "xxx.yyy.zzz",
    "refreshToken": "xxx.yyy.zzz",
    "expiresIn": 7200
  }
}
```

登录必须使用 JSON 请求体，并携带 `captchaId` 与 `captchaCode`；缺少任一字段会被验证码过滤器拒绝。

### 刷新令牌

```http
POST /api/v1/auth/refresh-token?refreshToken=<refreshToken>
```

### 退出登录

```http
DELETE /api/v1/auth/logout
Authorization: Bearer <accessToken>
```

## 3. 员工绩效接口

```http
GET /api/v1/reports/performance?startDate=2026-10-14&endDate=2026-10-20
Authorization: Bearer <accessToken>
```

| 参数 | 必填 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `startDate` | 否 | `endDate - 6 天` | 统计开始日期 |
| `endDate` | 否 | 今天 | 统计结束日期（含当天） |

```json
{
  "code": "00000",
  "msg": "成功",
  "data": [
    {
      "userId": "2",
      "userName": "张明",
      "deptName": "销售一部",
      "customerCount": 8,
      "followCount": 12,
      "orderCount": 3,
      "orderAmount": 86000.00,
      "winRate": 33
    }
  ]
}
```

| 字段 | 说明 |
| --- | --- |
| `userId` | 用户 ID（字符串） |
| `userName` | 昵称，缺省时回退到用户名 |
| `deptName` | 部门名称 |
| `customerCount` | 名下客户总数，不限日期 |
| `followCount` | 日期区间内新增跟进数 |
| `orderCount` | 日期区间内新建订单数 |
| `orderAmount` | 日期区间内订单金额合计 |
| `winRate` | 区间内已完成订单数 / 区间订单数，整数百分比 |

口径说明：

- 只统计 `sys_user` 中未删除且启用的用户。
- 结果按订单金额倒序。
- 部门/员工筛选由前端在结果集上完成，接口本身不接收 `deptId`、`userId`。
- 该接口仅要求登录，没有额外的报表权限码。

「员工绩效」页面（`/report/meritsrecord`）复用上述接口，并在前端派生签收绩效与线索转化指标；这些派生公式仅用于界面展示，不是服务端指标。

## 4. 客户与线索接口

基础路径：`/api/v1/scrm/customers`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/` | `scrm:customer:list` | 客户分页列表 |
| GET | `/pool` | `scrm:customer:list` | 公海分页列表 |
| GET | `/options` | 已登录 | 客户下拉选项 |
| GET | `/{id}` | `scrm:customer:list` | 客户详情（含标签与最近跟进） |
| POST | `/` | `scrm:customer:create` | 新增客户/线索 |
| PUT | `/{id}` | `scrm:customer:update` | 修改 |
| DELETE | `/{ids}` | `scrm:customer:delete` | 批量删除，ID 逗号分隔 |
| PUT | `/assign` | `scrm:customer:assign` | 分配负责人 |
| PUT | `/{id}/claim` | `scrm:customer:claim` | 领取公海客户 |

### 列表筛选参数

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `keyword` | string | 姓名、公司、手机号 |
| `level` | string | `A` / `B` / `C` / `D` |
| `status` | integer | 单个客户状态 |
| `statuses` | string | 逗号分隔的多个状态，例如 `2,3` |
| `source` | integer | 来源：1 官网、2 转介绍、3 活动、4 广告、5 主动开发、6 其他 |
| `ownerId` | long | 负责人 |
| `tagId` | long | 标签 |
| `pool` | boolean | 是否公海 |
| `startDate` / `endDate` | date | 创建日期范围 |

界面映射：

```text
线索列表   -> statuses=1
客户列表   -> statuses=2,3
待办线索   -> statuses=1 + 前端按 nextFollowTime 过滤
公海线索   -> /pool
```

### 新增/修改请求体

```json
{
  "name": "王女士",
  "phone": "13800000000",
  "wechat": "wang_demo",
  "company": "示例科技有限公司",
  "position": "采购经理",
  "industry": "零售",
  "level": "A",
  "source": 1,
  "status": 1,
  "ownerId": "2",
  "address": "杭州市",
  "remark": "演示数据",
  "nextFollowTime": "2026-10-22T10:00:00",
  "tagIds": ["1", "3"]
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `name` | 是 | 姓名/联系人 |
| `phone` | 否 | 手机号 |
| `level` | 否 | 意向级别 |
| `source` | 否 | 来源枚举 |
| `status` | 否 | 1 线索、2 跟进中、3 已成交、4 已流失 |
| `ownerId` | 否 | 负责人；不传时由后端按当前用户处理 |
| `tagIds` | 否 | 标签 ID 数组，保存时整体覆盖 |

把线索转为客户，本质是把 `status` 从 `1` 更新为 `2`，再调用修改接口。

`CustomerDetailVO` 除客户字段外，还包含 `tags`、`followUps`、`opportunities`、`contracts`，前端详情抽屉按这些分区展示。

## 5. 跟进记录

基础路径：`/api/v1/scrm/follow-ups`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/` | `scrm:follow-up:list` | 分页列表 |
| GET | `/customer/{customerId}` | `scrm:follow-up:list` | 某客户的跟进 |
| POST | `/` | `scrm:follow-up:create` | 新增跟进 |
| PUT | `/{id}` | `scrm:follow-up:update` | 修改 |
| DELETE | `/{ids}` | `scrm:follow-up:delete` | 批量删除 |

筛选参数：`customerId`、`keyword`、`type`、`creatorId`、`startDate`、`endDate`。

```json
{
  "customerId": "1",
  "type": 1,
  "content": "电话沟通了采购预算",
  "result": "有明确意向，下周寄样",
  "nextFollowTime": "2026-10-25T09:30:00"
}
```

新增跟进后，后端会更新客户的最近跟进时间与下次跟进时间。

## 6. 商机与合同

### 6.1 商机

基础路径：`/api/v1/scrm/opportunities`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/` | `scrm:opportunity:list` |
| GET | `/{id}` | `scrm:opportunity:list` |
| GET | `/customer/{customerId}` | `scrm:opportunity:list` |
| POST | `/` | `scrm:opportunity:create` |
| PUT | `/{id}` | `scrm:opportunity:update` |
| DELETE | `/{ids}` | `scrm:opportunity:delete` |

请求体字段：`customerId`、`name`、`amount`、`stage`、`probability`、`expectedCloseDate`、`ownerId`、`remark`。

### 6.2 合同（订单）

基础路径：`/api/v1/scrm/contracts`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/` | `scrm:contract:list` | 分页列表 |
| GET | `/{id}` | `scrm:contract:list` | 详情 |
| GET | `/customer/{customerId}` | 已登录 | 客户合同 |
| POST | `/` | `scrm:contract:create` | 新增 |
| PUT | `/{id}` | `scrm:contract:update` | 修改 |
| DELETE | `/{ids}` | `scrm:contract:delete` | 批量删除 |

筛选参数：`keyword`、`customerId`、`status`、`ownerId`。

```json
{
  "contractNo": "SO20261020001",
  "customerId": "1",
  "name": "年度服务订单",
  "amount": 12800.00,
  "signDate": "2026-10-20",
  "startDate": "2026-11-01",
  "endDate": "2027-10-31",
  "status": 1,
  "ownerId": "2",
  "remark": "演示订单"
}
```

订单状态：`1 待审核`、`2 已确认`、`3 已完成`、`4 已取消`。审核页把 `status` 从 `1` 改为 `2`（通过）或 `4`（驳回/取消）。

## 7. 标签、活动与看板

### 7.1 客户标签

基础路径：`/api/v1/scrm/tags`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/` | `scrm:tag:list` |
| POST | `/` | `scrm:tag:create` |
| PUT | `/{id}` | `scrm:tag:update` |
| DELETE | `/{ids}` | `scrm:tag:delete` |

字段：`name`、`color`、`sort`。

### 7.2 营销活动

基础路径：`/api/v1/scrm/campaigns`

| 方法 | 路径 | 权限码 |
| --- | --- | --- |
| GET | `/` | `scrm:campaign:list` |
| GET | `/{id}` | `scrm:campaign:list` |
| POST | `/` | `scrm:campaign:create` |
| PUT | `/{id}` | `scrm:campaign:update` |
| DELETE | `/{ids}` | `scrm:campaign:delete` |

当前导航未开放营销活动页面，接口保留供扩展。

### 7.3 工作台看板

```http
GET /api/v1/scrm/dashboard?days=30
```

返回客户、跟进、商机、合同等汇总数据，保留供扩展。

## 8. AI 中台接口

### 8.1 会话与分析

基础路径：`/api/v1/ai`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/conversations` | 会话分页列表 |
| GET | `/conversations/{id}` | 会话详情 |
| POST | `/conversations` | 导入会话 |
| DELETE | `/conversations/{ids}` | 删除会话 |
| POST | `/conversations/{id}/analyze` | 生成摘要、需求、异议与标签建议 |
| GET | `/conversations/{id}/analysis` | 获取已保存的分析结果 |
| POST | `/analyses/{id}/accept-follow-up` | 采纳分析结论为跟进记录 |
| POST | `/tag-suggestions/{id}/accept` | 采纳标签建议并写入客户标签 |
| POST | `/tag-suggestions/{id}/reject` | 忽略标签建议 |

调用 `analyze` 后，模型返回的结构化结果会写入 `ai_analysis`、`ai_tag_suggestion`，并记录 `ai_usage_log`。采纳标签会写入 `biz_customer_tag` / `rel_customer_tag`；采纳跟进会生成 `biz_follow_up`。

### 8.2 智能质检

基础路径：`/api/v1/ai/quality`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/stats` | 已登录 | 质检概览 |
| GET | `/rules` | 已登录 | 质检规则列表 |
| POST | `/rules` | `ai:quality:rule` | 新增规则 |
| PUT | `/rules/{id}` | `ai:quality:rule` | 修改规则 |
| DELETE | `/rules/{ids}` | `ai:quality:rule` | 删除规则 |
| GET | `/tasks` | 已登录 | 质检任务分页 |
| POST | `/tasks` | 已登录 | 创建并异步执行质检任务 |
| GET | `/results` | 已登录 | 质检结果分页 |
| PUT | `/results/{id}/review` | 已登录 | 复核结果（确认违规 / 误报） |

规则字段包含规则内容、判定说明（`instruction`）与严重级别；`instruction` 为必填，用于告诉模型如何判定。

### 8.3 模型与 Prompt

基础路径：`/api/v1/ai`

| 方法 | 路径 | 权限码 | 说明 |
| --- | --- | --- | --- |
| GET | `/config` | `ai:config:list` | 获取模型配置（Key 为掩码） |
| PUT | `/config` | `ai:config:update` | 保存模型配置 |
| POST | `/config/test` | `ai:config:test` | 测试模型连通性 |
| GET | `/usage/summary` | `ai:config:list` | 调用用量概览 |
| GET | `/prompts` | `ai:config:list` | Prompt 模板列表 |
| PUT | `/prompts/{code}` | `ai:config:update` | 保存模板并递增版本 |

模型配置字段：`provider`、`baseUrl`、`model`、`apiKey`、`temperature`、`maxTokens`、`enabled`、`isDefault`。API Key 使用 AES 加密后入库，读取接口只返回掩码。

## 9. 系统接口

### 9.1 部门

基础路径：`/api/v1/depts`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/` | 已登录 | 部门列表（树） |
| GET | `/options` | 已登录 | 部门下拉 |
| POST | `/` | `sys:dept:create` | 新增 |
| GET | `/{deptId}/form` | 已登录 | 表单回显 |
| PUT | `/{deptId}` | `sys:dept:update` | 修改 |
| DELETE | `/{ids}` | `sys:dept:delete` | 删除 |

### 9.2 用户

基础路径：`/api/v1/users`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/` | 已登录 | 用户分页 |
| GET | `/{userId}/form` | `sys:user:update` | 表单回显 |
| POST | `/` | `sys:user:create` | 新增 |
| PUT | `/{userId}` | `sys:user:update` | 修改 |
| DELETE | `/{ids}` | `sys:user:delete` | 删除 |
| GET | `/export` | `sys:user:export` | 导出 |
| GET | `/options` | 已登录 | 用户下拉 |

### 9.3 角色

基础路径：`/api/v1/roles`

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| GET | `/` | 已登录 | 角色分页 |
| GET | `/options` | 已登录 | 角色下拉 |
| POST | `/` | `sys:role:create` | 新增 |
| PUT | `/{id}` | `sys:role:update` | 修改 |
| DELETE | `/{ids}` | `sys:role:delete` | 删除 |
| PUT | `/{roleId}/status` | `sys:role:update` | 启停 |
| GET | `/{roleId}/menu-ids` | `sys:role:update` | 菜单 ID |
| PUT | `/{roleId}/menus` | `sys:role:assign` | 分配菜单 |

> 权限提示：部分系统查询接口只要求登录，没有细粒度权限注解。前端导航是静态的，权限只会在调用写接口或受保护接口时生效。用于真实环境前应补齐接口权限并做完整审计，详见 [SECURITY.md](../SECURITY.md)。

## 10. 前端调用约定

```ts
// frontend/src/api/analytics/index.ts
const rows = await AnalyticsAPI.performance({
  startDate: "2026-10-14",
  endDate: "2026-10-20",
});

// 后端 Long 为字符串，运算前转换
const amount = Number(row.orderAmount ?? 0);
```

- Axios 已解包业务 `data`，页面不再读取 `res.data.data`。
- 业务失败会统一弹错并 reject，页面只处理成功分支。
- 多 ID 删除使用逗号拼接路径，例如 `/api/v1/scrm/customers/1,2,3`。
- 数组参数使用 `ids=1&ids=2` 形式（Axios `paramsSerializer` 已配置）。
- CSV 导出完全在前端完成，不调用导出接口。
