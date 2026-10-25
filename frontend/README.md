# MUXIN AI SCRM · Frontend

木心 MUXIN 私域智能运营系统的 Web 前端，基于 Vue 3 + TypeScript + Vite + Element Plus 构建。

## 环境要求

- Node.js `^20.19.0` 或 `>=22.12.0`
- pnpm 9+

## 本地开发

```bash
pnpm install
pnpm dev
```

默认访问 `http://localhost:3001`，开发代理指向 `http://localhost:8001`。

## 常用命令

| 命令 | 说明 |
| --- | --- |
| `pnpm dev` | 启动开发服务器 |
| `pnpm type-check` | TypeScript 类型检查 |
| `pnpm build` | 类型检查并构建生产包 |
| `pnpm preview` | 预览构建产物 |
| `pnpm lint` | ESLint / Prettier / Stylelint |

## 环境变量

开发环境配置见 `.env.development`，生产构建配置见 `.env.production`：

- `VITE_APP_TITLE`：站点标题
- `VITE_API_BASE`：接口代理前缀
- `VITE_PROXY_TARGET`：开发代理目标（默认后端 8001）

## 目录结构

```text
src/
├── api/                # 接口封装
│   ├── ai/             # AI 工作台、质检、设置
│   ├── analytics/      # 经营报表与组织数据
│   └── scrm/           # 客户、标签、合同
├── layouts/            # 主框架布局
├── navigation/         # 导航、公告与业务常量
├── router/             # 静态路由与访问守卫
├── stores/             # Pinia 状态
├── styles/             # 设计令牌与全局样式
└── views/
    ├── ai/             # AI 能力页面
    ├── workspace/      # 线索、客户、订单、报表与组织页面
    └── system/         # 系统管理页面
```

完整架构、启动方式与许可证说明见仓库根目录 [README.md](../README.md)。
