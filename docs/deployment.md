# 部署指南

本文覆盖 MUXIN AI SCRM 从本地启动到单机生产部署的完整流程。示例以 Linux + Nginx 为准，Windows 差异单独标注。

> 公网部署前请先阅读 [SECURITY.md](../SECURITY.md) 与 [DISCLAIMER.md](../DISCLAIMER.md)，并完成第 5 节的上线检查。

## 1. 本地开发

### 1.1 环境要求

| 依赖 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 17 或更高 | 推荐 21 |
| Maven | 3.9+ | 后端构建 |
| Node.js | `^20.19.0` 或 `>=22.12.0` | Vite 8 要求 |
| pnpm | 9+ | 前端包管理 |
| Docker | 可选 | 运行 MySQL 与 Redis |

### 1.2 初始化环境变量

```bash
cp .env.example .env
```

至少修改以下值：

```dotenv
MYSQL_ROOT_PASSWORD=请替换
REDIS_PASSWORD=请替换
DB_PASSWORD=请替换          # 必须与 MYSQL_ROOT_PASSWORD 一致
JWT_SECRET=请替换为至少32位随机字符串
AI_ENCRYPT_KEY=请替换为随机字符串
```

生成随机密钥：

```bash
openssl rand -base64 48
```

在当前终端导入变量。后端启动时依赖这些变量，未导入会因占位符无法解析而启动失败：

```bash
set -a && . ./.env && set +a
```

Windows PowerShell：

```powershell
Get-Content .env | Where-Object { $_ -match '^[^#].+=' } | ForEach-Object {
  $name, $value = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($name, $value, "Process")
}
```

### 1.3 启动 MySQL 与 Redis

```bash
docker compose up -d
docker compose ps
```

默认映射：

| 服务 | 容器 | 宿主机端口 |
| --- | --- | --- |
| MySQL 8 | `muxin-scrm-mysql` | 3308 |
| Redis 7 | `muxin-scrm-redis` | 6380 |

端口可通过 `.env` 中的 `MYSQL_PORT`、`REDIS_PORT` 调整。首次启动会自动执行：

- `sql/muxin_scrm.sql`：创建 `muxin_scrm` 数据库、系统表、业务表、演示账号与虚构数据；
- `sql/muxin_ai.sql`：创建 AI 中台表、权限点、模型与 Prompt 演示数据。

初始化脚本**只在数据卷为空时执行一次**。修改 SQL 后若想重新初始化，需要先备份，再删除 `volumes/mysql` 后重新 `docker compose up -d`。这是破坏性操作，不要在有真实数据的环境执行。

如果已有数据卷、只缺少 AI 表，可单独导入：

```bash
mysql -h127.0.0.1 -P3308 -uroot -p muxin_scrm < sql/muxin_ai.sql
```

### 1.4 启动后端

```bash
cd backend
mvn spring-boot:run
```

- 端口：<http://localhost:8001>
- 默认 profile：`dev`
- 接口文档：<http://localhost:8001/swagger-ui/index.html>

开发配置从环境变量读取：

| 变量 | 默认值 |
| --- | --- |
| `DB_HOST` | `localhost` |
| `DB_PORT` | `3308` |
| `DB_NAME` | `muxin_scrm` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | 无默认值，必须提供 |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6380` |
| `REDIS_DATABASE` | `0` |
| `REDIS_PASSWORD` | 无默认值，必须提供 |
| `JWT_SECRET` | 无默认值，必须提供 |
| `AI_ENCRYPT_KEY` | 无默认值，必须提供 |
| `AI_FALLBACK_MOCK` | `true` |

### 1.5 启动前端

另开终端：

```bash
cd frontend
pnpm install
pnpm dev
```

打开 <http://localhost:3001>。开发代理把 `/dev-api` 转发到 `http://localhost:8001`，配置见 `frontend/.env.development`。

### 1.6 默认账号

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 系统管理员 |
| `dept_manager` | `123456` | 部门主管 |
| `dept_member` | `123456` | 部门成员（仅本人数据） |

演示账号仅用于本地体验，公网环境必须修改或删除。

## 2. 生产构建

### 2.1 后端

```bash
cd backend
mvn clean package -DskipTests
```

产物：`backend/target/muxin-scrm.jar`。

```bash
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8001
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_NAME=muxin_scrm
export DB_USERNAME=muxin
export DB_PASSWORD='<强密码>'
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_DATABASE=0
export REDIS_PASSWORD='<强密码>'
export JWT_SECRET='<至少32位随机字符串>'
export AI_ENCRYPT_KEY='<随机字符串>'

java -jar target/muxin-scrm.jar
```

生产配置说明：

- `application-prod.yml` 的数据库默认端口是 3306、Redis 默认 6379，与 Docker 开发端口不同，部署时请显式传入。
- SQL 日志默认开启（`StdOutImpl`）。高流量环境建议关闭，避免日志膨胀。
- Swagger UI / OpenAPI 默认关闭；如确需开放，应加 IP 白名单或 Basic Auth。
- Actuator 仅暴露 `health` 与 `info`，且不返回详细信息。
- AI Key 使用 `AI_ENCRYPT_KEY` 加密后入库；轮换该值会导致旧 Key 无法解密，需要重新录入。

建议使用 systemd 管理进程：

```ini
# /etc/systemd/system/muxin-scrm.service
[Unit]
Description=MUXIN AI SCRM Backend
After=network.target mysql.service redis.service

[Service]
User=muxin
WorkingDirectory=/opt/muxin-scrm
EnvironmentFile=/etc/muxin-scrm.env
ExecStart=/usr/bin/java -Xms512m -Xmx1g -jar /opt/muxin-scrm/muxin-scrm.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now muxin-scrm
sudo journalctl -u muxin-scrm -f
```

`/etc/muxin-scrm.env` 权限应为 `600`，不要提交到仓库。

### 2.2 前端

```bash
cd frontend
pnpm install --frozen-lockfile
pnpm build
```

产物在 `frontend/dist`。生产环境请求前缀为 `/prod-api`，由 Nginx 去掉前缀后转发到后端。

## 3. Nginx 单机示例

```nginx
server {
    listen 80;
    server_name scrm.example.com;

    root /var/www/muxin-scrm;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /prod-api/ {
        proxy_pass http://127.0.0.1:8001/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        client_max_body_size 20m;
        proxy_read_timeout 60s;
    }

    location ~* \.(js|css|png|jpg|jpeg|svg|woff2?)$ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

注意：

- `proxy_pass` 末尾的 `/` 会去掉 `/prod-api` 前缀；若省略，后端会收到 `/prod-api/api/v1/...` 并返回 404。
- 前端使用 Hash 路由，`try_files` 的回退主要用于根路径和静态资源。
- 启用 HTTPS 后，把 `listen 80` 换成 443 并配置证书；建议用 Certbot 或云厂商证书。

```bash
sudo nginx -t
sudo systemctl reload nginx
```

## 4. 数据库与缓存

### 4.1 初始化

- 开发：`sql/muxin_scrm.sql` 与 `sql/muxin_ai.sql` 由 Docker 首次启动自动导入。
- 生产：用 root 或有 DDL 权限的账号手工导入：

```bash
mysql -h 127.0.0.1 -u root -p < sql/muxin_scrm.sql
mysql -h 127.0.0.1 -u root -p muxin_scrm < sql/muxin_ai.sql
```

### 4.2 最小权限账号

应用账号不应使用 root：

```sql
CREATE DATABASE IF NOT EXISTS muxin_scrm
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER 'muxin'@'%' IDENTIFIED BY '<强密码>';
GRANT SELECT, INSERT, UPDATE, DELETE ON muxin_scrm.* TO 'muxin'@'%';
FLUSH PRIVILEGES;
```

如果应用需要执行代码生成等 DDL 操作，再单独授予 `CREATE`、`ALTER`、`INDEX`，不要直接给 `ALL`。

### 4.3 Redis

- 必须设置密码，禁止无认证端口暴露公网。
- 生产建议使用独立实例或独立 DB。
- 内存策略建议 `noeviction` 或 `allkeys-lru`，并配置监控；验证码丢失只影响登录，权限缓存丢失会触发重新加载。

### 4.4 备份

```bash
mysqldump -h 127.0.0.1 -u root -p \
  --single-transaction --routines --triggers \
  muxin_scrm > backup_muxin_scrm_$(date +%F).sql

gzip backup_muxin_scrm_*.sql
```

建议：

- 每日全量 + binlog 增量；
- 备份文件加密后存到另一台机器或对象存储；
- 每季度做一次恢复演练。

恢复：

```bash
gunzip -c backup_muxin_scrm_2026-10-20.sql.gz | mysql -h 127.0.0.1 -u root -p muxin_scrm
```

## 5. 上线检查清单

### 配置

- [ ] `.env` 未提交，权限为 `600`
- [ ] `JWT_SECRET`、`AI_ENCRYPT_KEY` 已替换为随机值
- [ ] 数据库与 Redis 使用强密码，不对公网开放
- [ ] 应用使用最小权限数据库账号
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] Swagger UI、Actuator 未暴露公网
- [ ] 演示账号已修改密码或禁用
- [ ] AI 模型 Key 已通过设置页录入，未写入代码或镜像

### 网络与证书

- [ ] 全站 HTTPS，HTTP 自动跳转
- [ ] 安全响应头（HSTS、X-Content-Type-Options、Referrer-Policy）
- [ ] 反向代理超时与上传大小符合业务
- [ ] 防火墙只开放 80/443

### 数据

- [ ] 已配置定时备份并验证可恢复
- [ ] 已确认演示数据已清理，或明确标注为演示环境
- [ ] 接入真实客户数据前完成隐私与合规评估

### 可观测性

- [ ] 后端日志落盘并轮转
- [ ] 监控 8001 健康检查、MySQL、Redis
- [ ] 配置磁盘与内存告警
- [ ] 监控 AI 调用失败率与 Token 消耗

## 6. 常见问题

### 后端启动报 `Could not resolve placeholder 'DB_PASSWORD'`

环境变量未导入。执行 `set -a && . ./.env && set +a` 后重启，或检查 systemd 的 `EnvironmentFile`。

### `docker compose up` 提示缺少 `MYSQL_ROOT_PASSWORD`

没有 `.env`，或变量为空。先 `cp .env.example .env` 并填写。

### 数据库表不存在 / 登录报错

初始化脚本只在空数据卷执行。检查 `volumes/mysql` 是否已有旧数据，或手工导入 SQL。

### AI 页面提示未配置模型 / 调用失败

- 确认 `AI_ENCRYPT_KEY` 与保存 Key 时一致；
- 在「AI 设置」执行连通性测试；
- `AI_FALLBACK_MOCK=true` 时会回退 Mock 通道，检查后端日志确认是否发生回退。

### 前端 404 或接口打到错误地址

- 开发：确认 `VITE_PROXY_TARGET=http://localhost:8001`。
- 生产：确认 Nginx 的 `proxy_pass` 末尾有 `/`，并且前端构建时 `VITE_API_BASE=/prod-api`。

### 登录提示验证码错误

- 验证码一次性使用，刷新页面会重新获取；
- Redis 未连接或密码错误时，验证码无法写入，查看后端日志；
- 系统时间偏差过大会影响 JWT 校验。

### 接口返回 401 / 403

- 401：令牌缺失、过期或签名不一致；
- 403：当前账号缺少接口权限码，检查角色与菜单授权。

### 表格中的 ID 或金额计算错误

后端把 `Long` 序列化为字符串。前端参与运算前使用 `Number(value)`，否则会出现字符串拼接或 `NaN`。

### 中文 CSV 在 Excel 中乱码

项目已写入 UTF-8 BOM。若自行扩展导出逻辑，保留 `\uFEFF` 前缀。

## 7. 升级与回滚

升级：

```bash
# 1. 备份数据库
# 2. 拉取代码并构建
cd frontend && pnpm install --frozen-lockfile && pnpm build
cd ../backend && mvn clean package -DskipTests
# 3. 执行增量 SQL（如有）
# 4. 重启后端，替换前端静态文件
```

回滚：

- 保留上一版本的 jar 与 `dist` 目录，回滚时替换并重启；
- 数据库变更需提前准备反向 SQL；
- 回滚后清理浏览器缓存或调整静态资源版本号。

## 8. 公开演示部署的额外提醒

1. 修改所有默认密码，禁用不必要的账号；
2. 使用独立演示数据库，禁止导入真实客户数据；
3. 关闭写操作或定期重置数据，避免被恶意灌入内容；
4. 在页面显著位置标注「演示环境」；
5. 遵守 [DISCLAIMER.md](../DISCLAIMER.md) 中的使用约定。
