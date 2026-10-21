# 09 · 部署上线设计

> 目标：**部署目标无关**（假设 A2）——全部组件容器化，换服务器只改 `deploy/.env`，不改代码、不改镜像（约束 AC-1）。
> 本文档的容器名、服务名、端口、环境变量名是运维唯一事实来源；与其他文档冲突时以本文件为准。

## 9.1 部署形态与前提

| 项 | 取值 |
| --- | --- |
| 服务器 | 1 台境外/香港 VPS，2C4G，40GB SSD（假设 A1） |
| 域名 | 1 个（如 `exam.example.com`），A 记录指向 VPS 公网 IP |
| 形态 | 单机 Docker Compose：`nginx` + `server` + `mysql` + `redis`（03 文档 3.5） |
| 协议 | 全程 HTTPS（Let's Encrypt），HTTP 301 跳转 |
| 备案 | **不做 ICP 备案** |
| 可用性 | 月度 ≥ 99%（单机无冗余，接受计划内维护窗口，01 文档 1.5） |

### 9.1.1 备案前提与代价

- **境外/香港服务器不需要 ICP 备案**：备案是境内接入服务的行政许可，服务器在境外时接入商不要求、也无力代办。
- **但域名仍须在注册商处完成实名/邮箱验证**并开启 WHOIS 隐私保护；未实名的域名会被注册商 `clientHold`（解析全部失效）。用 `dig +short exam.example.com` 确认解析是否被挂起。
- 代价一：**将来若迁到境内服务器，必须先完成 ICP 备案**才能解析到境内 IP。备案需域名注册满 3 个月、负责人核验、接入商初审 + 管局审核（约 7~20 个工作日）。
- 代价二：本站属**教育/培训类**内容，多数省份该类备案要求**企业主体**，个人主体常被驳回或需补充资质。因此"迁回境内"不是纯技术决定，而是可能**卡死**的合规决定。
- 代价三：不做备案意味着无法使用国内 CDN、国内对象存储、微信生态与短信验证码（与 01 文档 OOS-07 一致）。
- 代价四：用户数据存储于境外，需在隐私政策中明示存储位置与跨境事实（本站不采集手机号与实名信息，风险面已最小化）。

### 9.1.2 单机方案的固有代价（显式接受）

| 代价 | 表现 | 缓解 | 接受 |
| --- | --- | --- | --- |
| 单点故障 | 机器/机房故障 → 全站不可用 | 每日备份 + 按 9.11 恢复 | 是 |
| 无冗余 | MySQL 单实例，无主从、无自动故障转移 | 备份 + 磁盘快照 | 是 |
| 无弹性扩容 | 2C4G 上限约 50 并发，超限只能升配重启 | 升配不动代码 | 是 |
| 计划内停机 | 发版需重建容器，5~20s 不可用 | 选低峰发版 | 是 |
| 跨境延迟 | 大陆访问香港 30~60ms | 优先香港节点 | 是 |

> 决策与理由
> 备选方案是"云 RDS + 云 Redis + 容器托管"可消除单点。放弃原因：本项目无商业收入（OOS-05），托管费用远高于一台 VPS，而可用性要求仅 99%（每月允许 7.2 小时不可用）。**若未来有付费用户，第一件该做的事是把 MySQL 换成 RDS，而不是加服务器。**

## 9.2 服务器准备

### 9.2.1 配置、系统与端口

| 项 | 最低 | 推荐 | 说明 |
| --- | --- | --- | --- |
| CPU / 内存 | 2 vCPU / 2GB | **2 vCPU / 4GB** | JVM 1G + MySQL 1G + Redis 256M + 系统/Nginx（03 文档 3.7） |
| 磁盘 | 30GB SSD | **40GB SSD** | 数据库 + 镜像 + 日志 + 上传 |
| 系统 | Ubuntu 22.04 LTS | **Ubuntu 24.04 LTS** | 本文命令按 Ubuntu 编写 |
| swap | 1GB | **2GB** | 防止 OOM Killer 杀 MySQL |
| 区域 | 香港 | 香港 | 大陆访问延迟最低 |

| 端口 | 用途 | 对公网开放 |
| --- | --- | --- |
| 22/tcp | SSH | 开放（建议限制来源 IP） |
| 80/tcp | HTTP（仅 ACME 校验 + 301 跳转） | 开放 |
| 443/tcp | HTTPS | 开放 |
| 3306/tcp | MySQL | **不开放**（compose 内不映射宿主端口） |
| 6379/tcp | Redis | **不开放**（compose 内不映射宿主端口） |
| 8080/tcp | 后端（仅绑 `127.0.0.1`） | **不开放** |

> 决策与理由
> **MySQL / Redis 不映射宿主机端口**：① 这两个中间件暴露公网后被打包扫描几乎必然（Redis 未授权访问是最高频入口）；② 端口不映射 = 即使 ufw 规则写错、即使 Docker 绕过 ufw（见 9.13.2），攻击面依然为零；③ 后端通过服务名 `mysql:3306` 访问，压根不需要宿主端口。

### 9.2.2 初始化命令

```bash
# 1) 运维账号（不要长期用 root 操作）
adduser --gecos "" deploy && usermod -aG sudo deploy

# 2) 时区：MySQL / JVM / Nginx / 日志全部跟随宿主机
timedatectl set-timezone Asia/Shanghai && timedatectl status

# 3) 系统更新与常用工具（宿主机不需要 JDK/Maven/Node，全部在镜像内构建）
apt-get update && apt-get upgrade -y
apt-get install -y curl unzip jq ca-certificates gnupg lsb-release

# 4) swap（4G 内存机器必做）
fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
sysctl -w vm.swappiness=10 && echo 'vm.swappiness=10' >> /etc/sysctl.conf
free -h

# 5) Docker 与 Compose（官方 apt 源）
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
usermod -aG docker deploy && systemctl enable --now docker
docker version && docker compose version      # Compose 需 v2.20+
```

在**云厂商控制台安全组**中只放行 22/80/443，并用 ufw 做宿主侧兜底（9.13.2）。

## 9.3 容器编排

宿主目录约定（`/opt/ruankao/ruankao-online` 为 git 仓库）：

```
/opt/ruankao/
├── ruankao-online/deploy/
│   ├── .env                     # 唯一环境差异来源（不进 Git）
│   ├── docker-compose.yml  server.Dockerfile  web.Dockerfile
│   ├── nginx/conf.d/default.conf
│   ├── sql/                     # schema.sql / init-data.sql / outline-*.sql
│   └── data/{uploads,logs,mysql-log,certbot/{conf,www}}   # 运行时数据（不进 Git）
└── backup/                      # 备份（仓库目录之外，不污染 git status）
```

### 9.3.1 `deploy/docker-compose.yml`

```yaml
# 单机部署：nginx + server + mysql + redis
# 用法：cd /opt/ruankao/ruankao-online/deploy && docker compose up -d
# 硬约定：MySQL(3306) / Redis(6379) 不映射宿主机端口，仅 rk-net 内网可达
name: ruankao

x-logging: &default-logging
  driver: json-file
  options:
    max-size: "10m"
    max-file: "5"

services:

  # ---------------- MySQL ----------------
  mysql:
    image: mysql:8.0
    container_name: rk-mysql
    restart: always
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      - --default-time-zone=+08:00
      - --explicit_defaults_for_timestamp=1
      - --innodb-buffer-pool-size=1024M      # 4G 机器给 MySQL 1G（03 文档 3.7）
      - --innodb-flush-log-at-trx-commit=2   # 单机非金融场景，牺牲 1s 持久性换写入吞吐
      - --max-connections=300
      - --max-allowed-packet=64M
      - --performance-schema=0               # 省下约 200~400MB
      - --slow-query-log=1
      - --long-query-time=0.5                # 与"慢 SQL >500ms"要求一致
      - --slow-query-log-file=/var/log/mysql/slow.log
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD 未设置}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-ruankao}
      MYSQL_USER: ${MYSQL_USERNAME:-ruankao}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:?MYSQL_PASSWORD 未设置}
      TZ: ${TZ:-Asia/Shanghai}
    volumes:
      - mysql-data:/var/lib/mysql
      - ./data/mysql-log:/var/log/mysql
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p\"$$MYSQL_ROOT_PASSWORD\" --silent"]
      interval: 10s
      timeout: 5s
      retries: 12
      start_period: 90s
    logging: *default-logging
    networks: [rk-net]

  # ---------------- Redis ----------------
  redis:
    image: redis:7-alpine
    container_name: rk-redis
    restart: always
    command:
      - redis-server
      - --port
      - "6379"
      - --requirepass
      - ${REDIS_PASSWORD:?REDIS_PASSWORD 未设置}
      - --appendonly
      - "yes"
      - --appendfsync
      - everysec
      - --maxmemory
      - 256mb
      - --maxmemory-policy
      - volatile-lru
      - --save
      - "3600 1 300 100"
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD:?REDIS_PASSWORD 未设置}   # 供 healthcheck 使用
      TZ: ${TZ:-Asia/Shanghai}
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD-SHELL", "redis-cli -a \"$$REDIS_PASSWORD\" --no-auth-warning ping | grep -q PONG"]
      interval: 10s
      timeout: 3s
      retries: 10
      start_period: 20s
    logging: *default-logging
    networks: [rk-net]

  # ---------------- 后端 ----------------
  server:
    build:
      context: ..
      dockerfile: deploy/server.Dockerfile
    image: ${REGISTRY:-ghcr.io/your-org}/ruankao-server:${IMAGE_TAG:-latest}
    container_name: rk-server
    restart: always
    init: true
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod
      TZ: ${TZ:-Asia/Shanghai}
      JAVA_OPTS: ${JAVA_OPTS:--Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10M}
      MYSQL_HOST: mysql
      MYSQL_PORT: "3306"
      MYSQL_DATABASE: ${MYSQL_DATABASE:-ruankao}
      MYSQL_USERNAME: ${MYSQL_USERNAME:-ruankao}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:?MYSQL_PASSWORD 未设置}
      REDIS_HOST: redis
      REDIS_PORT: "6379"
      REDIS_PASSWORD: ${REDIS_PASSWORD:?REDIS_PASSWORD 未设置}
      RK_TOKEN_TIMEOUT: ${RK_TOKEN_TIMEOUT:-2592000}
      RK_SERVER_PORT: "8080"
      RK_MGMT_PORT: "8081"
      RK_LOG_DIR: /app/logs
      RK_LOG_LEVEL: ${RK_LOG_LEVEL:-info}
      RK_UPLOAD_DIR: /data/uploads
      RK_CORS_ORIGINS: "${RK_CORS_ORIGINS:-}"
      RK_DOC_ENABLED: ${RK_DOC_ENABLED:-false}
      RK_ACTUATOR_ENABLED: ${RK_ACTUATOR_ENABLED:-true}
      LOGGING_LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE: 10MB
      LOGGING_LOGBACK_ROLLINGPOLICY_MAX_HISTORY: 30
      LOGGING_LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP: 1GB
      SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: 5MB
      SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE: 10MB
      # 池上限 20：取值理由与配套参数（minimum-idle / connection-timeout）见 06 §6.4.1，
      # 那里是这个数字的唯一定义处。此处留成环境变量是为了上线后可不重建镜像调整，
      # 值必须与 06 §6.4.1 保持一致（改一处就要改另一处，并复核 09 §9.12.2 的阈值）。
      SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "20"
    volumes:
      - ./data/uploads:/data/uploads
      - ./data/logs:/app/logs
    ports:
      - "127.0.0.1:8080:8080"      # 仅回环，便于宿主机 curl 调试；不对公网暴露
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://127.0.0.1:8081/actuator/health | grep -q '\"status\":\"UP\"'"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 90s
    logging: *default-logging
    networks: [rk-net]

  # ---------------- Nginx + 前端静态产物 ----------------
  nginx:
    build:
      context: ..
      dockerfile: deploy/web.Dockerfile
    image: ${REGISTRY:-ghcr.io/your-org}/ruankao-web:${IMAGE_TAG:-latest}
    container_name: rk-nginx
    restart: always
    depends_on:
      server:
        condition: service_healthy
    environment:
      TZ: ${TZ:-Asia/Shanghai}
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./data/uploads:/data/uploads:ro           # 与 RK_UPLOAD_DIR 同一宿主目录
      - ./data/certbot/conf:/etc/letsencrypt:ro
      - ./data/certbot/www:/var/www/certbot:ro
    logging: *default-logging
    networks: [rk-net]

volumes:
  mysql-data:
  redis-data:

networks:
  rk-net:
    driver: bridge
```

> 决策与理由
> **不用 `docker-entrypoint-initdb.d` 自动执行 SQL**（mysql 官方镜像支持）：① 该机制按文件名字母序执行，`init-data.sql` 会早于 `schema.sql` 而必然失败；② 管理员密码是**占位哈希**（04 文档 4.8.3），无法写成静态 SQL；③ 首次初始化只有一次机会，失败必须删卷重来，风险高于手工按序执行（见 9.9）。

> 决策与理由
> **Redis `maxmemory-policy` 选 `volatile-lru`**，而非 `noeviction` 或 `allkeys-lru`。`noeviction` 在内存打满时**写命令直接报错**，而登录会话写 Redis（ADR-03 规定会话不可降级）→ 全站无法登录，故障面最大；`allkeys-lru` 则可能淘汰正在使用的会话。`volatile-lru` 只淘汰**带 TTL 的键**（缓存、限流计数、`rk:login:fail:*`、Sa-Token 会话键全部带 TTL），语义最贴近"先牺牲可重建的数据"。同时必须配 `used_memory` 告警（9.12）把淘汰概率压到接近零。

### 9.3.2 `deploy/docker-compose.infra.yml`（本地开发用，对应评审 C4）

`README` 的快速开始与 10 文档 §P1 均要求「`docker compose -f deploy/docker-compose.infra.yml up -d` 后 MySQL 与 Redis 均 healthy」，但本文档此前未给出该文件的设计，导致 P1「本地能跑起来」无定义。它是**与生产 `docker-compose.yml` 并列的独立文件**：本地只需 MySQL + Redis，后端在 IDE 中 `spring-boot:run`、前端在 `pnpm dev`（Vite）中运行，两者都直连宿主端口。

```yaml
# deploy/docker-compose.infra.yml   —— 仅本地开发用（对应 10 文档 P1）
# 用法：cd deploy && docker compose -f docker-compose.infra.yml up -d
# 只起 MySQL + Redis；后端/前端在本地运行，不再容器化
name: ruankao-infra          # 独立 project name：命名卷不与生产 ruankao 项目的卷冲突

services:

  # ---------------- MySQL ----------------
  mysql:
    image: mysql:8.0
    container_name: rk-dev-mysql
    # 刻意映射到宿主机 3306：本地工具链（IDEA/DBeaver/mysql 客户端）要直连，理由见下方决策块
    ports:
      - "3306:3306"
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_0900_ai_ci
      - --default-time-zone=+08:00
      - --explicit_defaults_for_timestamp=1
      - --innodb-buffer-pool-size=256M      # 本地数据量小，不必照抄生产的 1024M
      - --max-connections=100
      - --max-allowed-packet=64M
      - --slow-query-log=0                  # 本地不需要慢日志
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-dev_root_pwd}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-ruankao}
      MYSQL_USER: ${MYSQL_USERNAME:-ruankao}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-dev_app_pwd}
      TZ: ${TZ:-Asia/Shanghai}
    volumes:
      - infra-mysql-data:/var/lib/mysql     # 本地独立命名卷，不与生产 mysql-data 混用
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p\"$$MYSQL_ROOT_PASSWORD\" --silent"]
      interval: 5s
      timeout: 5s
      retries: 20
      start_period: 30s
    networks: [rk-dev-net]

  # ---------------- Redis ----------------
  redis:
    image: redis:7-alpine
    container_name: rk-dev-redis
    # 刻意映射到宿主机 6379：理由同 MySQL
    ports:
      - "6379:6379"
    command:
      - redis-server
      - --port
      - "6379"
      - --requirepass
      - ${REDIS_PASSWORD:-dev_redis_pwd}
      - --appendonly
      - "yes"
      - --appendfsync
      - everysec
      - --maxmemory
      - 128mb
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD:-dev_redis_pwd}   # 供 healthcheck 使用
      TZ: ${TZ:-Asia/Shanghai}
    volumes:
      - infra-redis-data:/data              # 本地独立命名卷
    healthcheck:
      test: ["CMD-SHELL", "redis-cli -a \"$$REDIS_PASSWORD\" --no-auth-warning ping | grep -q PONG"]
      interval: 5s
      timeout: 3s
      retries: 10
    networks: [rk-dev-net]

volumes:
  infra-mysql-data:            # project name 为 ruankao-infra，故实际卷名为 ruankao-infra_infra-mysql-data
  infra-redis-data:

networks:
  rk-dev-net:
    driver: bridge
```

本地起库后建表与初始化数据（与生产 9.9 步骤 5 等价，直连宿主端口）：

```bash
cd /path/to/repo/deploy
docker compose -f docker-compose.infra.yml up -d
docker compose -f docker-compose.infra.yml ps          # 期望 mysql / redis 均 healthy

# 直连宿主 3306 建库建表（本地 root 口令默认 dev_root_pwd）
mysql -h 127.0.0.1 -P 3306 -uroot -pdev_root_pwd ruankao < sql/schema.sql
mysql -h 127.0.0.1 -P 3306 -uroot -pdev_root_pwd ruankao < sql/init-data.sql
for f in sql/outline-*.sql; do [ -e "$f" ] || continue; mysql -h 127.0.0.1 -P 3306 -uroot -pdev_root_pwd ruankao < "$f"; done
redis-cli -h 127.0.0.1 -p 6379 -a dev_redis_pwd --no-auth-warning ping   # 期望 PONG
```

> 决策与理由
> **本地 `docker-compose.infra.yml` 故意把 `3306:3306` 与 `6379:6379` 映射到宿主机，与生产 9.2.1「不映射宿主端口」相反**：两者判定条件不同，不是同一条规则的两个取值。① 本地开发的工具链（IDE 数据源、DBeaver、`mysql` / `redis-cli`、RedisInsight）就是**要直连这两个端口**，不映射则本地开发无法调试数据库；② 开发机位于 NAT/防火墙之后，端口只在**本机 loopback 与局域网**可达，暴露对象是"自己的机器"，不是公网；③ 生产不映射是为了**不把数据库暴露到公网**（Redis 未授权访问是最高频入口，见 9.2.1）。**因此这是"本地起得来、生产也安全"的做法，不构成对 9.2.1 端口最小化原则的违反。**若开发机直连公网（如云上开发机），必须先加 ufw 限制来源 IP 再启用本文件。
>
> **数据卷用独立命名卷（`infra-mysql-data` / `infra-redis-data`）、`restart` 策略可省**：① compose 的 `name:` 设为 `ruankao-infra`，卷名前缀与生产的 `ruankao` 不同，**不会互相覆盖数据**，也不会让本地调试误连生产快照；② 本地不需要 `restart: always`——开发机重启后手动 `up -d` 更利于观察启动报错，也避免后台常驻占用资源。若开发机要与生产的 `docker-compose.yml` 同盘运行（少见），命名卷隔离即为此兜底。

## 9.4 环境变量清单

`deploy/.env` 是与服务器唯一相关的差异来源，**存放位置固定为 `deploy/.env`**（与 compose 同目录，`docker compose` 自动加载）。

| 变量 | 示例值 | 必填 | 说明 |
| --- | --- | --- | --- |
| `MYSQL_ROOT_PASSWORD` | `Rk_ro0t_9f3c` | 是 | MySQL root 密码，仅初始化与备份使用，不进后端 |
| `MYSQL_DATABASE` | `ruankao` | 是 | 库名（04 文档固定 `ruankao`，utf8mb4） |
| `MYSQL_USERNAME` | `ruankao` | 是 | 业务库账号，权限限于 `ruankao` 库 |
| `MYSQL_PASSWORD` | `Rk_app_7b21` | 是 | 业务库账号密码 |
| `REDIS_PASSWORD` | `Rk_rd_4d8a` | 是 | Redis 口令（`--requirepass`） |
| `REDIS_HOST` | `redis` | 是 | compose 服务名；本地开发为 `127.0.0.1` |
| `REDIS_PORT` | `6379` | 是 | Redis 端口 |
| `RK_TOKEN_TIMEOUT` | `2592000` | 是 | 登录态有效期（秒），默认 30 天。**Sa-Token 采用不透明令牌（`token-style: uuid`），登录态存 Redis，因此不存在需要轮换的令牌签名密钥**，强制下线靠删除会话实现（见 06 文档 6.5） |
| `RK_SERVER_PORT` | `8080` | 是 | 后端端口（容器内） |
| `RK_MGMT_PORT` | `8081` | 否 | 管理端点端口（容器内，不对外映射），承载 `/actuator/health` |
| `RK_LOG_LEVEL` | `info` | 是 | 日志级别，故障期临时调 `debug` |
| `RK_LOG_DIR` | `/app/logs` | 否 | 日志目录（容器内），对应宿主 `deploy/data/logs` |
| `RK_UPLOAD_DIR` | `/data/uploads` | 是 | 图片存储目录（05 文档 5.10-51），对应宿主 `deploy/data/uploads` |
| `RK_CORS_ORIGINS` | 空 | 否 | 允许的跨域来源，逗号分隔；同源部署留空即关闭 CORS |
| `RK_DOC_ENABLED` | `false` | 否 | Knife4j（`/doc.html`）开关，**生产必须 false** |
| `RK_ACTUATOR_ENABLED` | `true` | 否 | 管理端点开关；仅绑内部 `RK_MGMT_PORT`，公网不可达 |
| `TZ` | `Asia/Shanghai` | 是 | 所有容器时区，影响日志时间与 MySQL `NOW()` |
| `JAVA_OPTS` | `-Xmx1024m -Xms512m -XX:+UseG1GC` | 是 | JVM 参数，`-Xmx` 与 03 文档 3.7 一致 |
| `REGISTRY` | `ghcr.io/your-org` | 否 | 镜像仓库前缀（CI 推送与 `compose pull` 使用） |
| `IMAGE_TAG` | `latest` / `sha-8f2c1a9` | 否 | 镜像标签，回滚时改回上一个 sha |

### 9.4.1 生成 `deploy/.env`（含随机密钥）

```bash
cd /opt/ruankao/ruankao-online/deploy
cat > .env <<'EOF'
MYSQL_ROOT_PASSWORD=CHANGE_ME_ROOT
MYSQL_DATABASE=ruankao
MYSQL_USERNAME=ruankao
MYSQL_PASSWORD=CHANGE_ME_APP
REDIS_PASSWORD=CHANGE_ME_REDIS
REDIS_HOST=redis
REDIS_PORT=6379
RK_TOKEN_TIMEOUT=2592000
RK_SERVER_PORT=8080
RK_MGMT_PORT=8081
RK_LOG_LEVEL=info
RK_LOG_DIR=/app/logs
RK_UPLOAD_DIR=/data/uploads
RK_CORS_ORIGINS=
RK_DOC_ENABLED=false
RK_ACTUATOR_ENABLED=true
TZ=Asia/Shanghai
JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m -Xlog:gc*:file=/app/logs/gc.log:time,uptime,level,tags:filecount=5,filesize=10m
REGISTRY=ghcr.io/your-org
IMAGE_TAG=latest
EOF

k() { openssl rand -base64 24 | tr -d '/+='; }
sed -i "s/^MYSQL_ROOT_PASSWORD=.*/MYSQL_ROOT_PASSWORD=$(k)/" .env
sed -i "s/^MYSQL_PASSWORD=.*/MYSQL_PASSWORD=$(k)/" .env
sed -i "s/^REDIS_PASSWORD=.*/REDIS_PASSWORD=$(k)/" .env

chmod 600 .env && ls -l .env
```

### 9.4.2 不得提交到 Git

仓库根 `.gitignore` 必须包含（约束 AC-1）：

```gitignore
deploy/.env
deploy/.env.*
!deploy/.env.example
deploy/data/
backup/
*.hprof
*.log
```

> 决策与理由
> 仓库内只保留 `deploy/.env.example`（占位值），真实 `.env` 只存在于服务器。Git 历史一旦写入密钥即永久可查（删除也需重写历史），而本项目是公开仓库，泄露概率极高。**必须在第一次 `git add` 之前就写好这条规则。**

## 9.5 Nginx 配置

`deploy/nginx/conf.d/default.conf`（挂载到容器 `/etc/nginx/conf.d/default.conf`）。`exam.example.com` 是**唯一需要替换的两处之一**，另一处是 DNS。

```nginx
# 挂载位置：/etc/nginx/conf.d/default.conf（:ro）
limit_req_zone  $binary_remote_addr zone=api_rps:10m  rate=20r/s;
limit_conn_zone $binary_remote_addr zone=api_conn:10m;

# ---------- HTTP：仅 ACME 校验 + 301 跳转 ----------
server {
    listen      80;
    listen      [::]:80;
    server_name exam.example.com;

    location ^~ /.well-known/acme-challenge/ {
        root         /var/www/certbot;
        default_type "text/plain";
        access_log   off;
    }
    location / { return 301 https://$host$request_uri; }
}

# ---------- HTTPS ----------
server {
    listen      443 ssl;
    listen      [::]:443 ssl;
    http2       on;
    server_name exam.example.com;

    # TLS
    ssl_certificate         /etc/letsencrypt/live/exam.example.com/fullchain.pem;
    ssl_certificate_key     /etc/letsencrypt/live/exam.example.com/privkey.pem;
    ssl_trusted_certificate /etc/letsencrypt/live/exam.example.com/chain.pem;
    ssl_protocols           TLSv1.2 TLSv1.3;
    ssl_ciphers             ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache       shared:SSL:10m;
    ssl_session_timeout     1d;
    ssl_session_tickets     off;
    ssl_stapling            on;
    ssl_stapling_verify     on;
    resolver                223.5.5.5 8.8.8.8 valid=300s;
    resolver_timeout        5s;

    # gzip
    gzip            on;
    gzip_comp_level 5;
    gzip_min_length 1024;
    gzip_vary       on;
    gzip_proxied    any;
    gzip_disable    "msie6";
    gzip_types      text/plain text/css text/xml text/javascript application/javascript
                    application/json application/xml application/rss+xml image/svg+xml font/ttf font/otf;

    # 安全响应头（server 级默认值；带 add_header 的 location 需重复声明）
    add_header X-Content-Type-Options     "nosniff" always;
    add_header X-Frame-Options            "SAMEORIGIN" always;
    add_header Referrer-Policy            "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security  "max-age=31536000; includeSubDomains" always;

    root                  /usr/share/nginx/html;
    index                 index.html;
    charset               utf-8;
    server_tokens         off;
    client_max_body_size  10m;      # 图片单文件 ≤5MB（05 文档 5.10-51），留 2 倍余量
    access_log /var/log/nginx/access.log;
    error_log  /var/log/nginx/error.log warn;

    # 生产屏蔽接口文档与监控端点（纵深防御，见 9.10）
    location ~ ^/(doc\.html|webjars/|v3/api-docs|swagger-resources/|swagger-ui|actuator/) { return 404; }
    # 隐藏文件（.env/.git 等）永不可达
    location ~ /\. { return 404; }

    # 前端 SPA（Vue history 模式）
    location / { try_files $uri $uri/ /index.html; }

    # index.html 不缓存，保证发版立即生效
    location = /index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        expires -1;
    }

    # Vite 带内容 hash 的资源，可长缓存
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, max-age=31536000, immutable" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        access_log off;
    }

    # 上传图片静态托管（映射 RK_UPLOAD_DIR 挂载卷）
    location /uploads/ {
        alias /data/uploads/;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header X-Frame-Options "SAMEORIGIN" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        autoindex off;
        access_log off;
    }

    # 后端 API（前缀 /api/v1，见 05 文档 5.1）
    location /api/ {
        limit_req  zone=api_rps burst=40 nodelay;
        limit_conn api_conn 20;

        proxy_pass         http://server:8080;
        proxy_http_version 1.1;
        proxy_set_header   Host              $host;
        proxy_set_header   X-Real-IP         $remote_addr;
        proxy_set_header   X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto $scheme;
        proxy_set_header   Connection        "";
        proxy_connect_timeout 5s;
        proxy_send_timeout    60s;
        proxy_read_timeout    60s;        # 组卷红线 800ms，60s 仅作兜底
        client_max_body_size  10m;
        proxy_buffering         on;
        proxy_buffer_size       16k;
        proxy_buffers           8 16k;
        proxy_busy_buffers_size 32k;
    }
}
```

> 决策与理由
> **`proxy_pass http://server:8080;` 不带路径后缀**：`/api/v1/auth/login` 原样透传，前后端契约（05 文档）与本地开发（Vite proxy）完全一致。在这里做路径重写，就会复现"本地能跑、线上 404"。

> 决策与理由
> **Brotli（`br`）暂不启用**：官方 `nginx` 镜像不含 `ngx_brotli` 模块，写 `brotli on;` 会导致 **Nginx 启动失败**（unknown directive）。启用需自建带 `--add-module=ngx_brotli` 的镜像，收益是 JS/CSS 再降 15~20%；本项目产物 gzip 后首屏 300~500KB、更新频率低，不值得引入自建镜像的维护成本。若要启用：把 gzip 段换成 `brotli on; brotli_comp_level 5; brotli_types ...`，并把 `web.Dockerfile` 的基础镜像换成自建 brotli 版，其余不变。

> 决策与理由
> **`add_header` 必须逐个 location 重复声明**：Nginx 的 `add_header` 是"就近覆盖"而非继承累加——某 location 内一旦出现任一 `add_header`，server 级所有 `add_header` 在该 location 内**全部失效**。因此 `/assets/`、`/uploads/`、`= /index.html` 三个写了 `Cache-Control` 的 location 都重复了安全头。这是最容易被漏掉的一类安全配置错误。

## 9.6 HTTPS 证书（Let's Encrypt + certbot）

### 9.6.1 首次申请（webroot）

签发需要 80 端口可达，而完整配置里的 443 server 引用了尚不存在的证书、会导致 Nginx 启动失败，因此**先用 HTTP-only 临时配置起 Nginx**：

```bash
cd /opt/ruankao/ruankao-online/deploy
cp nginx/conf.d/default.conf nginx/conf.d/default.conf.bak

cat > nginx/conf.d/default.conf <<'EOF'
server {
    listen 80;
    server_name exam.example.com;
    location ^~ /.well-known/acme-challenge/ { root /var/www/certbot; }
    location / { return 200 "bootstrap\n"; }
}
EOF

docker compose up -d nginx
curl -I http://exam.example.com/        # 期望 200

docker run --rm \
  -v /opt/ruankao/ruankao-online/deploy/data/certbot/conf:/etc/letsencrypt \
  -v /opt/ruankao/ruankao-online/deploy/data/certbot/www:/var/www/certbot \
  certbot/certbot certonly --webroot -w /var/www/certbot \
    -d exam.example.com --email you@example.com \
    --agree-tos --no-eff-email --non-interactive

ls -l data/certbot/conf/live/exam.example.com/

mv nginx/conf.d/default.conf.bak nginx/conf.d/default.conf
docker compose exec -T nginx nginx -t && docker compose exec -T nginx nginx -s reload
```

### 9.6.2 自动续期（宿主 cron）+ 续期后 reload

证书 90 天有效，certbot 在剩余 <30 天时才会真正续期。**certbot 容器内没有 docker CLI，无法自己 reload Nginx，因此续期与 reload 必须写在同一个宿主脚本里**：

```bash
cat > /opt/ruankao/ruankao-online/deploy/renew-cert.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
DEPLOY_DIR=/opt/ruankao/ruankao-online/deploy
DOMAIN=exam.example.com
LOG=/opt/ruankao/backup/cert-renew.log
cd "$DEPLOY_DIR"
echo "[$(date '+%F %T')] start renew" >> "$LOG"

docker run --rm \
  -v "$DEPLOY_DIR/data/certbot/conf:/etc/letsencrypt" \
  -v "$DEPLOY_DIR/data/certbot/www:/var/www/certbot" \
  certbot/certbot renew --webroot -w /var/www/certbot --quiet >> "$LOG" 2>&1

# 续期成功后 reload，让 Nginx 重新读取证书（不中断现有连接）
docker compose exec -T nginx nginx -s reload >> "$LOG" 2>&1

# 剩余有效期 <15 天说明续期持续失败，提前暴露
LEFT=$(docker run --rm -v "$DEPLOY_DIR/data/certbot/conf:/etc/letsencrypt" \
  certbot/certbot certificates -d "$DOMAIN" 2>/dev/null | grep -oP 'VALID: \K[0-9]+' | head -1 || echo 999)
[ "${LEFT:-999}" -lt 15 ] && echo "[$(date '+%F %T')] WARN: cert expires in $LEFT days" >> "$LOG"
exit 0
EOF
chmod +x /opt/ruankao/ruankao-online/deploy/renew-cert.sh

cat > /etc/cron.d/ruankao-cert-renew <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
7 3 * * * root /opt/ruankao/ruankao-online/deploy/renew-cert.sh
EOF
chmod 644 /etc/cron.d/ruankao-cert-renew

# 演练一次（--dry-run 不消耗签发配额）
docker run --rm \
  -v /opt/ruankao/ruankao-online/deploy/data/certbot/conf:/etc/letsencrypt \
  -v /opt/ruankao/ruankao-online/deploy/data/certbot/www:/var/www/certbot \
  certbot/certbot renew --webroot -w /var/www/certbot --dry-run
```

> 决策与理由
> 不用 systemd timer（能力等价但路径/SELinux 坑更多），也不用 certbot 容器内置定时（该镜像是**一次性命令容器**，无守护进程，且无 `docker` CLI）。**关键是"续期"与"reload"必须串行在一个脚本里**，否则会出现"证书已更新、Nginx 仍在用旧证书"的静默故障（浏览器仍显示旧到期日）。

## 9.7 构建与发布流程

### 9.7.1 后端：Maven 多模块 → 多阶段 Dockerfile

前提：`server/rk-boot/pom.xml` 必须设置 `<finalName>rk-boot</finalName>`，产物固定为 `rk-boot/target/rk-boot.jar`（可执行 fat jar；同时会产出 `rk-boot.jar.original`，不复制它）。

```dockerfile
# deploy/server.Dockerfile   构建上下文：仓库根目录（context: ..）
# docker build -f deploy/server.Dockerfile -t ruankao-server:dev .

# ---------------- 构建阶段 ----------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# 先只复制 POM，命中依赖缓存层（改代码不重下依赖）
COPY server/pom.xml                    /build/server/
COPY server/rk-common/pom.xml          /build/server/rk-common/
COPY server/rk-domain/pom.xml          /build/server/rk-domain/
COPY server/rk-repository/pom.xml      /build/server/rk-repository/
COPY server/rk-service/pom.xml         /build/server/rk-service/
COPY server/rk-api/pom.xml             /build/server/rk-api/
COPY server/rk-boot/pom.xml            /build/server/rk-boot/
RUN mvn -f /build/server/pom.xml -B -q -pl rk-boot -am dependency:go-offline
COPY server/ /build/server/
RUN mvn -f /build/server/pom.xml -B -pl rk-boot -am -DskipTests clean package \
 && ls -l /build/server/rk-boot/target/rk-boot.jar

# ---------------- 运行阶段 ----------------
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r -g 10001 rkapp \
 && useradd -r -u 10001 -g rkapp -m -d /home/rkapp rkapp \
 && mkdir -p /app/logs /data/uploads \
 && chown -R rkapp:rkapp /app /data/uploads
WORKDIR /app
COPY --from=build /build/server/rk-boot/target/rk-boot.jar /app/app.jar
RUN chown rkapp:rkapp /app/app.jar
USER rkapp                                  # 非 root 运行（9.13.3）
ENV TZ=Asia/Shanghai RK_SERVER_PORT=8080 RK_MGMT_PORT=8081 \
    JAVA_OPTS="-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m"
EXPOSE 8080 8081
# JAVA_OPTS 必须经 sh -c 展开（exec 形式不做变量替换）
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
```

> 决策与理由
> 运行镜像用 `eclipse-temurin:21-jre-jammy` 而非 alpine。alpine 是 musl libc，JVM 在时区文件、字体/本地库上有隐性差异；本项目 2C4G 上多出的约 120MB 磁盘完全可接受，换取"与开发环境行为一致"更划算。**镜像体积不是瓶颈，排障时间才是。**

### 9.7.2 前端：pnpm 产物进 Nginx 容器

```dockerfile
# deploy/web.Dockerfile   构建上下文：仓库根目录（context: ..）

# ---------------- 构建阶段 ----------------
FROM node:20-alpine AS build
RUN corepack enable && corepack prepare pnpm@9.15.0 --activate
WORKDIR /build
COPY web/package.json web/pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile
COPY web/ ./
RUN pnpm build && ls -l dist/index.html

# ---------------- 运行阶段 ----------------
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
RUN rm -f /etc/nginx/conf.d/default.conf      # 配置由 compose 只读挂载，换域名不改镜像
EXPOSE 80 443
```

> 决策与理由
> **推荐"多阶段构建把 `dist` 打进 Nginx 镜像"，而非"构建后拷进挂载卷"。** 挂载卷方案的三个具体代价：① 宿主目录必须先有文件，否则启动时挂载出空目录、页面 404，首次上线顺序脆弱；② 拷贝过程存在数百毫秒到数秒的**半成品窗口**（旧 `index.html` 引用已删除的旧 hash 资源 → 白屏）；③ 拷入文件属主是宿主用户，与 Nginx 容器内 `nginx`（uid 101）不一致，易 403。镜像方案下发版 = `pull + up -d`，`index.html` 与 `assets/` **原子切换**，回滚 = 改 `IMAGE_TAG`。代价是前端改动需重建镜像（约 1~2 分钟），单机项目可接受。

> 决策与理由
> **不在构建期注入 `VITE_API_BASE`**：前端统一使用相对路径 `/api/v1`（同源由 Nginx 反代），因此不存在"不同服务器需要不同构建产物"的问题——这正是"部署目标无关"（假设 A2）成立的原因。

### 9.7.3 手工发布命令序列

```bash
cd /opt/ruankao/ruankao-online/deploy
# 首次：准备运行时目录（MySQL 容器内 uid=999，慢日志目录必须可写）
mkdir -p data/uploads data/logs data/mysql-log data/certbot/conf data/certbot/www
chown -R 999:999 data/mysql-log && chown -R 10001:10001 data/uploads data/logs
# 10001 = server 容器内 rkapp 的 uid（9.7.1 USER rkapp）。绝不能用 `chmod 755`：755 下非属主只有 r-x、没有写权限，会导致图片上传与日志写入失败（对应评审 D5）

cd /opt/ruankao/ruankao-online && git fetch --all --tags && git checkout main && git pull --ff-only

cd deploy
docker compose pull                                  # 拉取 CI 已构建的镜像
# docker compose build --no-cache server nginx       # 服务器本地构建时使用

docker compose up -d --no-build mysql redis          # 幂等
docker compose up -d --no-build server
docker compose up -d --no-build nginx

docker compose ps
curl -sS -o /dev/null -w '%{http_code}\n' https://exam.example.com/
curl -sS -o /dev/null -w '%{http_code}\n' https://exam.example.com/api/v1/subjects
docker compose logs --since 2m server | tail -30
docker image prune -f

# 回滚：改回上一个成功的 sha 后重跑
# sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=<上一个成功的 sha>/' .env
# docker compose pull server nginx && docker compose up -d --no-build server nginx
```

> 注意：不要在 ARM 机器上构建后部署到 x86 VPS——`docker build` 产出宿主架构镜像，`arm64` 镜像在 `amd64` 上会 `exec format error`。本项目开发机为 x86_64 且 CI 为 `ubuntu-latest`，两侧一致。

## 9.8 CI/CD（GitHub Actions）

```yaml
# .github/workflows/ci.yml   monorepo：server/ 与 web/ 独立构建
name: ci

on:
  push:
    branches: [main]
    paths: ['server/**', 'web/**', 'deploy/**', '.github/workflows/ci.yml']
  pull_request:
    paths: ['server/**', 'web/**', 'deploy/**', '.github/workflows/ci.yml']
  workflow_dispatch:
    inputs:
      deploy:
        description: '构建完成后部署到生产服务器'
        type: boolean
        default: false

concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true

env:
  IMAGE_PREFIX: ghcr.io/${{ github.repository_owner }}/ruankao

jobs:
  # 判断 monorepo 哪一侧发生变化（paths 过滤器是 workflow 级的，逐 job 判断需用 paths-filter）
  changes:
    runs-on: ubuntu-latest
    outputs:
      server: ${{ steps.filter.outputs.server }}
      web: ${{ steps.filter.outputs.web }}
    steps:
      - uses: actions/checkout@v4
      - uses: dorny/paths-filter@v3
        id: filter
        with:
          filters: |
            server:
              - 'server/**'
              - 'deploy/server.Dockerfile'
            web:
              - 'web/**'
              - 'deploy/web.Dockerfile'

  # 后端：单测 + 打包 + 镜像
  server:
    needs: changes
    if: needs.changes.outputs.server == 'true'
    runs-on: ubuntu-latest
    permissions: { contents: read, packages: write }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: 单测 + 打包（组卷/判分/掌握度/计时为核心覆盖对象）
        run: mvn -B -f server/pom.xml -T1C clean verify
      - uses: docker/setup-buildx-action@v3
      - name: 登录 GHCR
        if: github.event_name == 'push'
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: 构建并推送后端镜像
        uses: docker/build-push-action@v6
        with:
          context: .
          file: deploy/server.Dockerfile
          push: ${{ github.event_name == 'push' }}
          tags: |
            ${{ env.IMAGE_PREFIX }}-server:latest
            ${{ env.IMAGE_PREFIX }}-server:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # 前端：构建 + 镜像
  web:
    needs: changes
    if: needs.changes.outputs.web == 'true'
    runs-on: ubuntu-latest
    permissions: { contents: read, packages: write }
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with: { version: 9 }
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: pnpm
          cache-dependency-path: web/pnpm-lock.yaml
      - name: 安装依赖
        working-directory: web
        run: pnpm install --frozen-lockfile
      - name: 类型检查 + 单测（脚本不存在时自动跳过）
        working-directory: web
        run: |
          pnpm run --if-present type-check
          pnpm run --if-present test
      - name: 构建
        working-directory: web
        run: pnpm build
      - uses: docker/setup-buildx-action@v3
      - name: 登录 GHCR
        if: github.event_name == 'push'
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - name: 构建并推送前端镜像
        uses: docker/build-push-action@v6
        with:
          context: .
          file: deploy/web.Dockerfile
          push: ${{ github.event_name == 'push' }}
          tags: |
            ${{ env.IMAGE_PREFIX }}-web:latest
            ${{ env.IMAGE_PREFIX }}-web:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # 部署（方案 A：SSH 远程执行 compose）
  deploy:
    needs: [server, web]
    if: github.event_name == 'workflow_dispatch' && inputs.deploy
    runs-on: ubuntu-latest
    environment: production          # 在 GitHub 上配置 Required reviewers，形成人工闸门
    steps:
      - name: SSH 上线
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.PROD_HOST }}
          username: ${{ secrets.PROD_USER }}
          key: ${{ secrets.PROD_SSH_KEY }}
          script: |
            set -euo pipefail
            cd /opt/ruankao/ruankao-online/deploy
            echo "${{ secrets.GHCR_TOKEN }}" | docker login ghcr.io -u ${{ github.actor }} --password-stdin
            sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${{ github.sha }}/" .env
            docker compose pull server nginx
            docker compose up -d --no-build server
            docker compose up -d --no-build nginx
            docker compose ps
            docker image prune -f
```

| 方案 | 做法 | 优点 | 缺点 |
| --- | --- | --- | --- |
| **A（推荐）** | 上面的 `deploy` job：`workflow_dispatch` + environment 审批后由 CI 登录服务器执行 `compose pull/up` | 一次点击完成发版；`up -d` 幂等可重复；回滚=改 `IMAGE_TAG` 重跑 | 需要 SSH 私钥放进 GitHub Secrets；CI 与生产强耦合 |
| B | CI 只推镜像，上线由人执行 9.7.3 的命令 | 生产入口只有人的 SSH，无额外凭据；发版时人能顺手看 `ps` 与日志 | 每次都要人工；易漏步骤（如忘 `nginx -s reload`） |

> 决策与理由
> **推荐方案 A，但触发条件限制为 `workflow_dispatch` 手动触发 + `environment: production` 人工审批**，而不是"push 到 main 自动上线"。单机没有蓝绿环境，一次自动上线失败即全站不可用，而 `main` 上一次不经意的合并会直接打到线上；加一道审批的成本只是"多点一次按钮"，收益是把意外发布彻底挡掉。**方案 B 保留为首次上线与故障期手段**（命令能逐条看清执行结果）；两方案共用同一份 compose 与同一批镜像，随时可切换。

## 9.9 首次上线步骤（从零到可访问）

```bash
# 0) 服务器准备：见 9.2.2（时区/swap/ufw/Docker）、9.2.3（安全组只开 22/80/443）

# 1) 上传代码
mkdir -p /opt/ruankao && cd /opt/ruankao
git clone https://github.com/<your-org>/ruankao-online.git

# 2) 运行时目录 + .env（见 9.4.1）
cd /opt/ruankao/ruankao-online/deploy
mkdir -p data/uploads data/logs data/mysql-log data/certbot/conf data/certbot/www
chown -R 999:999 data/mysql-log && chown -R 10001:10001 data/uploads data/logs
# 10001 = server 容器内 rkapp 的 uid（9.7.1 USER rkapp）。绝不能用 `chmod 755`：755 下非属主只有 r-x、没有写权限，会导致图片上传与日志写入失败（对应评审 D5）
# ... 按 9.4.1 生成 .env（含 openssl 随机密钥），并 chmod 600 .env ...

# 3) 域名解析：A 记录指向本机公网 IP
dig +short exam.example.com

# 4) 起 MySQL 与 Redis，等 healthy
docker compose up -d mysql redis
docker compose ps

# 5) 建库建表 + 初始化数据（schema.sql 首部须含 CREATE DATABASE IF NOT EXISTS ruankao ...; USE ruankao;）
set -a; source .env; set +a
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < sql/schema.sql
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < sql/init-data.sql
for f in sql/outline-*.sql; do
  [ -e "$f" ] || continue
  echo ">>> $f"
  docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$f"
done

# 6) 校验：27 张表 + 20 科目 + 20 卷面模板 + utf8mb4
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "
  SELECT COUNT(*) AS tables_ FROM information_schema.tables WHERE table_schema='$MYSQL_DATABASE';
  SELECT COUNT(*) AS subjects FROM rk_subject;
  SELECT COUNT(*) AS templates FROM exam_paper_template;"
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  SELECT DEFAULT_CHARACTER_SET_NAME AS cs, DEFAULT_COLLATION_NAME AS coll
  FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$MYSQL_DATABASE';"

# 7) 生成管理员 BCrypt 哈希并写库（04 文档 4.8.3 是占位哈希 + 首次登录强制改密）
docker compose run --rm --no-deps --entrypoint java server \
  -jar /app/app.jar --rk.password-gen='ChangeMe@2026'
# 输出形如 $2a$10$N9qo8uLOickgx2ZMRZoMye...（60 字符）
ADMIN_HASH='$2a$10$......'      # 必须用单引号，$ 不能被 shell 展开
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "
  UPDATE sys_user SET password_hash='${ADMIN_HASH}' WHERE username='admin';
  SELECT username, role, status, LENGTH(password_hash) AS hash_len FROM sys_user WHERE username='admin';"
# hash_len 必须是 60（对应 sys_user.password_hash CHAR(60)）

# 8) 起后端并确认健康
docker compose up -d server
docker compose ps
curl -s http://127.0.0.1:8080/api/v1/subjects | head -c 200     # code 应为 0
curl -s http://127.0.0.1:8081/actuator/health                   # 期望 status UP

# 9) 构建并起 Nginx（含前端产物）
docker compose build nginx          # 或 docker compose pull nginx
docker compose up -d nginx

# 10) 申请 HTTPS 证书：见 9.6.1 完整流程（临时 HTTP 配置 → certonly → 恢复 → nginx -t/-s reload）

# 11) 验证
curl -sSI http://exam.example.com/ | head -3                     # 期望 301 → https
curl -sS -o /dev/null -w '%{http_code}\n' https://exam.example.com/
curl -sS -o /dev/null -w '%{http_code}\n' https://exam.example.com/api/v1/subjects
curl -sSI https://exam.example.com/doc.html | head -1            # 期望 404
docker compose ps
```

**管理员一次性密码生成器（已实现，落点如下）**：

分派点在 `rk-boot` 的 `main` 首行：

```java
// server/rk-boot/src/main/java/com/ruankao/boot/RkBootApplication.java
public static void main(String[] args) {
    // 一次性口令生成：打印哈希后直接返回，不启动 Spring，因此不依赖 MySQL/Redis
    if (PasswordGenCli.tryRun(args)) {
        return;
    }
    SpringApplication.run(RkBootApplication.class, args);
}
```

实现分散在两个模块里（**规格的唯一出处是 [06 §6.1](06-backend.md)**，本节只说明"为什么是这个落点"）：

| 片段 | 落点 |
| --- | --- |
| 参数识别与打印（`--rk.password-gen=<明文>`，子串命中即短路） | `rk-api`：`com.ruankao.api.cli.PasswordGenCli` |
| BCrypt 哈希（strength=10，输出 `$2a$` 前缀、60 字符） | `rk-common`：`com.ruankao.common.security.PasswordHasher` |

> 决策与理由
> **不用 `htpasswd -bnBC 10 "" 'pass'`**：`apache2-utils` 产出 `$2y$` 前缀，而 Hutool / Spring Security 的 BCrypt 主要按 `$2a$` 分支解析盐值，`$2y$` 存在校验失败风险（各实现行为不一致）。**用后端自带的实现生成、同一实现校验**，永远不会遇到前缀兼容问题。
> 这也是把生成器**放进后端模块、由 `rk-boot` 的 `main` 分派**、而非交给外部脚本的原因。为什么不放在 `rk-boot` 里"就地写一遍"：`rk-boot` 的分层约束只允许依赖 `rk-api`，`PasswordHasher` 因此落在 `rk-common`，由 `rk-api` 沿合法路径复用它——**生成侧与校验侧共用同一份实现**，正是上面那个前缀哑弹的结构性解法。若把哈希逻辑内联回 `rk-boot`，要么接受"使用未声明的直接依赖（Hutool 靠传递依赖到达）"，要么在 `rk-boot` 里再写一遍 BCrypt 调用、制造第二个将来会漂移的落点。**若为一次性 CLI 放宽启动模块的依赖约束，代价比收益大。**

> 决策与理由
> **不在 `init-data.sql` 里写死管理员密码哈希**：04 文档 4.8.3 明确其为占位哈希且首次登录强制改密。若写死明文哈希，该哈希会随公开仓库公开，等于把默认管理员密码写在 README 旁边。命令行生成 + 单次 `UPDATE` 让密码只存在于服务器上。

## 9.10 上线检查单

```markdown
### 访问与功能
- [ ] `https://exam.example.com/` 返回 200，页面正常渲染，控制台无 mixed content 告警
- [ ] `http://exam.example.com/` 返回 301，Location 指向 https
- [ ] 游客可浏览科目列表与章节树（UC-G01 / UC-G02）
- [ ] 可注册（UC-G04）：弱密码被拒；重复用户名返回 `2003`
- [ ] **强制改密拦截生效（可验证，对应评审 C2）**：用初始密码登录后，登录接口返回的 `UserVO.mustChangePassword === true`，且前端强制跳转改密页（机制落点：`sys_config.admin.must.change.password` → `UserVO.mustChangePassword`，见 06 文档 6.5 与 05 文档 `UserVO`）
- [ ] 改密成功后该字段变为 `false`（重新拉取用户信息，`mustChangePassword === false`），前端不再跳转改密页
- [ ] 可章节练习并刷题（UC-U02），答错自动进错题本（UC-U07）
- [ ] 可发起模拟考、倒计时可用、可交卷并出客观题分数（UC-U09~UC-U11）
- [ ] 管理端可上传图片（`POST /api/v1/admin/files`），响应中 `/uploads/...` 可直接打开
- [ ] `https://exam.example.com/uploads/<真实路径>` 返回 200 且 Content-Type 为图片

### 端口与网络
- [ ] `nmap -Pn -p 22,80,443,3306,6379,8080 exam.example.com`：**3306 / 6379 未开放**
- [ ] `docker compose ps` 中 `rk-mysql`、`rk-redis` 的 PORTS 列为空（无映射）
- [ ] 后端仅回环可达：`ss -lntp | grep 8080` 显示 `127.0.0.1:8080`

### 文档与监控端点
- [ ] `/doc.html`、`/webjars/**`、`/v3/api-docs/**` 外网返回 **404** 或需访问控制
- [ ] `/actuator/**` 外网返回 **404**（仅监听容器内 8081）
- [ ] 容器内确认 Knife4j 已关：`docker compose exec server sh -c "curl -sI http://127.0.0.1:8080/doc.html | head -1"`

### 日志与轮转
- [ ] `deploy/data/logs/` 下有应用日志与 `gc.log`，且持续增长
- [ ] 日志为结构化 JSON 且含 `traceId`（05 文档 5.1.1）
- [ ] `docker compose logs server --since 1m` 有输出
- [ ] `deploy/data/mysql-log/slow.log` 已生成
- [ ] 轮转生效：单文件上限 10MB、保留 30 天（`LOGGING_LOGBACK_ROLLINGPOLICY_*`）

### 安全与密钥
- [ ] `git check-ignore -v deploy/.env` 有输出，`git status` 干净
- [ ] 仓库历史搜不到真实密钥：`git log -p | grep -nE 'MYSQL_PASSWORD=|REDIS_PASSWORD='` 无真实值
- [ ] `.env` 权限为 `600`
- [ ] 管理员默认密码已修改（`UserVO.mustChangePassword` 已为 `false`，再次登录不再提示改密）
- [ ] MySQL root 与业务账号密码均非默认值

### 备份
- [ ] 备份脚本已装入 cron，且手工执行过一次成功
- [ ] `/opt/ruankao/backup/` 下有当天的 `.sql.gz` 与 uploads 压缩包
- [ ] 已按 9.11.4 完成一次恢复演练与恢复验证
```

## 9.11 备份与恢复

> 决策与理由
> **备份落在宿主机 `/opt/ruankao/backup/`，不放容器内或数据卷里**：① 备份与被备份数据同在机器同一个卷 = 没备份，磁盘损坏时一起丢；② 宿主目录便于 `rsync`/`rclone` 推到异地，不必进容器；③ 该目录在 Git 工作区之外（仓库的兄弟目录），不污染 `git status`。

### 9.11.1 定时备份脚本（数据库 + 上传目录）

```bash
cat > /opt/ruankao/ruankao-online/deploy/backup.sh <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
DEPLOY_DIR=/opt/ruankao/ruankao-online/deploy
BACKUP_DIR=/opt/ruankao/backup
STAMP=$(date '+%Y%m%d-%H%M%S')
cd "$DEPLOY_DIR"
set -a; source .env; set +a
mkdir -p "$BACKUP_DIR/mysql" "$BACKUP_DIR/uploads"

# 1) 数据库：--single-transaction 走 InnoDB 一致性快照，不锁表
docker compose exec -T mysql mysqldump \
  -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF --default-character-set=utf8mb4 \
  "$MYSQL_DATABASE" | gzip -6 > "$BACKUP_DIR/mysql/ruankao-$STAMP.sql.gz"

# 2) 备份完整性自检：gzip 尾部完整且文件非空，否则视为失败
gzip -t "$BACKUP_DIR/mysql/ruankao-$STAMP.sql.gz"
[ -s "$BACKUP_DIR/mysql/ruankao-$STAMP.sql.gz" ]

# 3) 上传目录（图片）
tar czf "$BACKUP_DIR/uploads/uploads-$(date '+%Y%m%d').tar.gz" -C "$DEPLOY_DIR/data" uploads
tar tzf "$BACKUP_DIR/uploads/uploads-$(date '+%Y%m%d').tar.gz" > /dev/null

# 4) 保留策略（对应评审 D4）：日备保留 7 天；周日那份额外作为「周备」保留 4 周；上传包 14 天
#    周备必须落在独立目录：若只在日备目录里用 -mtime +7 / -mtime +30 两级 find，
#    前一条会把 7 天以上的文件全部删掉，后一条永远匹配不到 —— 实际只保留 7 天
mkdir -p "$BACKUP_DIR/mysql/weekly"
if [ "$(date '+%u')" = "7" ]; then
  # 周日：把当天这份备份硬链进 weekly/（硬链不额外占磁盘；日备链接被删后，周备链接仍指向同一 inode，可独立恢复）
  ln -f "$BACKUP_DIR/mysql/ruankao-$STAMP.sql.gz" \
        "$BACKUP_DIR/mysql/weekly/ruankao-$STAMP.sql.gz"
fi
find "$BACKUP_DIR/mysql/weekly" -maxdepth 1 -name 'ruankao-*.sql.gz' -mtime +28 -delete   # 周备：保留 4 周
find "$BACKUP_DIR/mysql"        -maxdepth 1 -name 'ruankao-*.sql.gz' -mtime +7  -delete   # 日备：保留 7 天（-maxdepth 1 不递归进 weekly/）
find "$BACKUP_DIR/uploads"                  -name 'uploads-*.tar.gz'  -mtime +14 -delete

echo "[$(date '+%F %T')] backup ok $(du -sh "$BACKUP_DIR/mysql/ruankao-$STAMP.sql.gz" | cut -f1)" \
  >> "$BACKUP_DIR/backup.log"
EOF
chmod +x /opt/ruankao/ruankao-online/deploy/backup.sh

cat > /etc/cron.d/ruankao-backup <<'EOF'
SHELL=/bin/bash
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
20 3 * * * root /opt/ruankao/ruankao-online/deploy/backup.sh
# 异地同步（rclone 配好 remote:rk-backup 后启用）
# 10 4 * * * root rclone sync /opt/ruankao/backup remote:rk-backup --transfers 4 --log-file /opt/ruankao/backup/rclone.log
EOF
chmod 644 /etc/cron.d/ruankao-backup

/opt/ruankao/ruankao-online/deploy/backup.sh && ls -lh /opt/ruankao/backup/mysql/ /opt/ruankao/backup/uploads/
```

### 9.11.2 恢复步骤

```bash
cd /opt/ruankao/ruankao-online/deploy
set -a; source .env; set +a
BK=/opt/ruankao/backup/mysql/ruankao-20260925-032001.sql.gz
UP=/opt/ruankao/backup/uploads/uploads-20260925.tar.gz

# 0) 恢复前快照（强烈建议，便于二次回退）
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction "$MYSQL_DATABASE" | gzip -6 > /tmp/before-restore.sql.gz

# 1) 停后端，避免恢复期间写入
docker compose stop server

# 2) 删库重建，保证与备份时状态一致（避免残留行）
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`;
  CREATE DATABASE \`$MYSQL_DATABASE\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# 3) 导入数据库与上传目录
gunzip -c "$BK" | docker compose exec -T mysql \
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$MYSQL_DATABASE"
tar xzf "$UP" -C data/

# 4) 起后端
docker compose start server
```

### 9.11.3 恢复验证（必须逐项做完，不能只看"命令没报错"）

```bash
# V1 表数量与关键表行数
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "
  SELECT COUNT(*) AS tables_ FROM information_schema.tables WHERE table_schema='$MYSQL_DATABASE';
  SELECT (SELECT COUNT(*) FROM sys_user) AS users,
         (SELECT COUNT(*) FROM rk_subject) AS subjects,
         (SELECT COUNT(*) FROM rk_question) AS questions,
         (SELECT COUNT(*) FROM user_answer_record) AS answers;"

# V2 字符集/排序规则正确（防中文乱码）
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  SELECT DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA
  WHERE SCHEMA_NAME='$MYSQL_DATABASE';"

# V3 中文未乱码（能查到中文科目名即说明 utf8mb4 全程正确）
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "
  SELECT code, name FROM rk_subject ORDER BY sort_no LIMIT 3;"

# V4 密码哈希长度仍为 60（BCrypt 未被截断）
docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "
  SELECT username, LENGTH(password_hash) FROM sys_user WHERE username='admin';"

# V5 业务可用性（不只是 SQL）：接口 200 / 已存在账号能登录 / 题目图片能显示
curl -sS -o /dev/null -w '%{http_code}\n' https://exam.example.com/api/v1/subjects
ls -l data/uploads | head
```

## 9.12 监控与运维

### 9.12.1 日志位置与查看

| 日志 | 宿主路径 | 容器路径 | 轮转 |
| --- | --- | --- | --- |
| 后端应用日志 | `deploy/data/logs/rk-boot*.log` | `/app/logs/` | 10MB/文件，30 天，总量 1GB |
| GC 日志 | `deploy/data/logs/gc.log` | `/app/logs/gc.log` | 5 文件 × 10MB |
| 堆转储 | `deploy/data/logs/*.hprof` | `/app/logs/` | 不轮转（手动清理） |
| 慢 SQL（>500ms） | `deploy/data/mysql-log/slow.log` | `/var/log/mysql/slow.log` | 手工归档，初期每周看一次 |
| Nginx 访问/错误 | `docker compose logs nginx` | `/var/log/nginx/*.log` | json-file 10MB × 5 |
| 容器 stdout | `docker compose logs <svc>` | — | 同上 |

```bash
cd /opt/ruankao/ruankao-online/deploy
docker compose logs -f --tail=100 server          # 实时
docker compose logs --since 30m --timestamps server
grep -h '8f2c1a9b4d7e4f10' data/logs/*.log        # 按 traceId 串联一次请求
grep -i '"level":"ERROR"' data/logs/rk-boot.log | tail -50
docker compose exec mysql tail -100 /var/log/mysql/slow.log
```

### 9.12.2 资源监控与阈值

```bash
cd /opt/ruankao/ruankao-online/deploy
set -a; source .env; set +a
docker stats --no-stream
df -h / && du -sh /var/lib/docker /opt/ruankao/backup deploy/data/*
docker compose exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
  SHOW STATUS LIKE 'Threads_connected'; SHOW STATUS LIKE 'Max_used_connections';
  SELECT table_schema, ROUND(SUM(data_length+index_length)/1024/1024,1) AS mb
  FROM information_schema.tables GROUP BY table_schema;"
docker compose exec redis redis-cli -a "$REDIS_PASSWORD" --no-auth-warning info memory | head -20
```

| 指标 | 告警阈值 | 处置 |
| --- | --- | --- |
| 根分区使用率 | > 80% | 清旧镜像/旧备份（9.12.3 磁盘写满行） |
| 容器内存 | server > 1.4GB / mysql > 1.5GB | 复核 `-Xmx` 与 `innodb-buffer-pool-size` |
| Redis `used_memory`/maxmemory | > 80% | 查大 key、补缓存 TTL |
| MySQL `Threads_connected` | > 200（上限 300） | 排查连接泄漏（Hikari 池上限见 [06 §6.4.1](06-backend.md)，正常时 `Threads_connected` 应 ≤ 池上限 + 几条管理连接） |
| 可用性探测 | 连续 3 次 5xx/超时 | 见 9.12.3 |
| 证书剩余有效期 | < 15 天 | 手工跑 `renew-cert.sh` 并查 cron 日志 |

> 决策与理由
> **不引入 Prometheus/Grafana**（与 01 文档"不引入 ELK"同源）：单机 2C4G 跑监控栈要多占 300~500MB 内存与 5GB 磁盘，而它监控的正是它自己所在的机器——机器宕机时监控也一起挂，对可用性告警零价值。替代方案：① 用**站外**免费拨测从外网探测 `https://exam.example.com/api/v1/subjects`，这才是机器宕机时真正会告警的手段；② 上面的 `docker stats` / `information_schema` 查询做成每日 cron 输出到日志，人工或脚本比对阈值。

### 9.12.3 常见故障排查表

| 现象 | 排查命令 | 常见原因 | 处置 |
| --- | --- | --- | --- |
| **502 Bad Gateway** | `docker compose ps`；`docker compose logs --since 10m server \| tail -50` | ① server 未启动/未 healthy；② 后端启动失败（必填环境变量被 AC-1 启动校验拦下）；③ 后端 OOM 被杀 | 看日志栈顶异常；变量缺失则补 `.env` 后 `up -d server`；OOM 则下调 `-Xmx` 或加 swap |
| 502 但 server 是 healthy | `curl -sI http://127.0.0.1:8080/api/v1/subjects`；`docker compose exec nginx getent hosts server`；`nginx -t` | `proxy_pass` 服务名解析失败；两容器不在同一 network | 确认两侧都在 `rk-net`；`docker compose exec nginx nginx -s reload` |
| **容器反复重启**（Restarting） | `docker inspect rk-server \| jq '.[0].RestartCount, .[0].State'`；`docker compose logs --tail=200 server` | ① 启动时 MySQL 尚未就绪（`start_period` 太短）；② 必填变量为空导致启动校验失败；③ `JAVA_OPTS` 引号写错 | 看首个 `Caused by`；调大 `healthcheck.start_period`；修正 `.env` 后重启 |
| **磁盘写满** | `df -h`；`du -sh /var/lib/docker/* deploy/data/* /opt/ruankao/backup` | ① 日志未轮转；② 旧镜像堆积（每次发版留 300MB+）；③ 备份未清理；④ MySQL binlog | 依次 `docker image prune -af`、`docker system prune -f`、`find /opt/ruankao/backup -mtime +30 -delete`；长期把 `max-file` 降到 3 并核查 `TOTAL_SIZE_CAP` |
| **MySQL 连接数耗尽**（Too many connections） | `docker compose exec mysql mysql -uroot -p... -e "SHOW PROCESSLIST;"` | ① 连接/长事务未关闭；② 池配置过大或多实例；③ 慢 SQL 长期占连接 | 先 `KILL` 掉 `Time>300` 的异常连接救急；查 `slow.log` 定位慢查询；把 Hikari 池调回 [06 §6.4.1](06-backend.md) 规定的值（**不要就地改成一个新数字**），再排查泄漏 |
| **Redis 内存打满**（OOM command not allowed） | `redis-cli -a ... info memory`；`redis-cli -a ... --bigkeys` | ① 缓存键未设 TTL；② 大 key（整份试卷 JSON 塞一个键）；③ `maxmemory` 小于实际需要 | 临时 `CONFIG SET maxmemory 512mb`（**同时改 compose，否则重启失效**）；根治是给缓存键加 TTL、拆分大 key |
| **证书过期** | `echo \| openssl s_client -servername exam.example.com -connect exam.example.com:443 2>/dev/null \| openssl x509 -noout -dates`；`tail -50 /opt/ruankao/backup/cert-renew.log` | ① cron 未安装/未执行；② webroot 路径错导致校验 404；③ 解析变更后校验失败；④ 80 端口被占用 | 手动跑 `renew-cert.sh` 看日志；用 `--dry-run` 复现；确认 `curl -I http://exam.example.com/.well-known/acme-challenge/t` 落在 `/var/www/certbot` |
| healthy 但接口全 401 | `docker compose logs server \| grep -i 'sa-token\|token'`；`docker compose exec redis redis-cli -a "$REDIS_PASSWORD" DBSIZE` | ① **Redis 被清空**（FLUSHALL / 数据卷重建丢 AOF）—— 登录态存于 Redis，清空即全员下线；② `RK_TOKEN_TIMEOUT` 或 `active-timeout` 被改小 | ① 属预期则无需处理（用户重新登录即可）；确系误操作则从备份恢复；② 改回原值后 `up -d server` |
| 上传图片 404（接口 200） | `ls -l deploy/data/uploads/<路径>`；`docker compose exec nginx ls -l /data/uploads/`；`docker compose exec server ls -l /data/uploads/` | ① `RK_UPLOAD_DIR` 与 Nginx 挂载路径不一致；② 宿主目录属主不是 `rkapp`（uid 10001），后端写不进去 | 两侧都必须指向宿主 `deploy/data/uploads`（容器内均为 `/data/uploads`）；**`chown -R 10001:10001 data/uploads data/logs`**（对应评审 D5：`server` 容器内进程是 `rkapp` uid 10001，**`chmod 755` 下非属主无写权限，反而正是上传失败的原因**，不要用 755 或 777 代替） |
| 中文乱码 | `SHOW VARIABLES LIKE 'character\_set%';` | 库/表/连接字符集不是 utf8mb4 | 确认 `--character-set-server=utf8mb4` 生效；JDBC URL 带 `characterEncoding=utf8&connectionCollation=utf8mb4_0900_ai_ci` |

## 9.13 安全加固清单

### 9.13.1 SSH 加固

```bash
# 1) 开发机生成部署专用密钥（不复用个人密钥）
ssh-keygen -t ed25519 -C "ruankao-deploy" -f ~/.ssh/ruankao_vps
ssh-copy-id -i ~/.ssh/ruankao_vps.pub deploy@exam.example.com

# 2) 确认密钥登录可用后，再关密码登录（顺序不能反，否则会把自己锁在外面）
cat > /etc/ssh/sshd_config.d/99-hardening.conf <<'EOF'
PermitRootLogin no
PasswordAuthentication no
KbdInteractiveAuthentication no
PubkeyAuthentication yes
X11Forwarding no
AllowAgentForwarding no
AllowTcpForwarding no
MaxAuthTries 3
LoginGraceTime 20
ClientAliveInterval 300
ClientAliveCountMax 2
EOF
sshd -t && systemctl restart ssh
# 保持当前会话不关，另开一个终端验证能登录后再关闭旧会话
```

### 9.13.2 防火墙（ufw）

```bash
ufw default deny incoming && ufw default allow outgoing
ufw allow 22/tcp  comment 'SSH'
ufw allow 80/tcp  comment 'HTTP/ACME'
ufw allow 443/tcp comment 'HTTPS'
ufw --force enable && ufw status verbose
```

> 决策与理由
> **Docker 会绕过 ufw 的 INPUT 链**（容器端口映射走 `DOCKER-USER`/`nat` 链，`ufw deny 3306` 挡不住 `-p 3306:3306`）。因此**唯一可靠的做法是在 compose 里不映射端口**（9.3.1 已如此），ufw 只是第二道防线。上线前自检：`grep -nE '3306|6379' deploy/docker-compose.yml` 的命中应出现在 `command`/`healthcheck` 段，而不是 `ports` 段。

### 9.13.3 容器与进程

| 项 | 做法 | 状态 |
| --- | --- | --- |
| 后端非 root | `server.Dockerfile` 内 `USER rkapp`（uid 10001） | 已落地（9.7.1） |
| 前端非 root | nginx 官方镜像 master 为 root、worker 为 `nginx`；完全非 root 需换 `nginxinc/nginx-unprivileged` 并改监听端口 | 可接受，未改 |
| 禁止提权 | 为 `server`/`nginx` 加 `security_opt: ["no-new-privileges:true"]` | 可选增强 |
| 只读根文件系统 | `read_only: true` + `tmpfs: [/tmp]`（日志卷仍需可写） | 可选增强 |
| 数据卷权限 | `data/uploads`、`data/logs` 属主 `10001:10001`（`rkapp`），`data/mysql-log` 属主 `999:999`；**不用 `chmod 777`，也不能只给 755**（755 下非属主无写权限，见 9.7.3 / 9.9） | 已落地（9.7.3） |
| 镜像来源 | 只用官方镜像：`mysql:8.0`、`redis:7-alpine`、`nginx:1.27-alpine`、`eclipse-temurin`、`maven`、`node`、`certbot/certbot` | 已落地 |
| 时区一致 | 所有容器 `TZ=Asia/Shanghai`；MySQL `--default-time-zone=+08:00` | 已落地（9.3.1） |

### 9.13.4 密钥与密码管理

- `.env` 权限 `600`、属主 `deploy`，且已被 `.gitignore` 覆盖（9.4.2）。
- GitHub Secrets 只存 `PROD_HOST`、`PROD_USER`、`PROD_SSH_KEY`、`GHCR_TOKEN`；SSH 用**独立部署密钥**，不复用个人密钥。

| 凭据 | 轮换周期 | 动作 |
| --- | --- | --- |
| `MYSQL_PASSWORD` | 6~12 个月或怀疑泄露时 | 改 `.env` → `ALTER USER 'ruankao'@'%' IDENTIFIED BY '新密码';` → `up -d server` |
| `MYSQL_ROOT_PASSWORD` | 同上 | 改 `.env` → `ALTER USER 'root'@'%' IDENTIFIED BY '新密码';` → 重启 mysql |
| `REDIS_PASSWORD` | 同上 | 改 `.env` → `up -d redis server`（`--requirepass` 重启即生效） |
| 会话有效期策略 | 按需 | 调整 `RK_TOKEN_TIMEOUT`；**副作用：改小会使超出新有效期的用户被强制下线**，选低峰期执行 |
| 强制全员下线（应急） | 怀疑账号被盗用、凭证泄漏时 | `docker compose exec redis redis-cli -a "$REDIS_PASSWORD" FLUSHDB`；**登录态存于 Redis，清空即全员下线**，用户只能重新登录 |
| 管理员密码 | 每 6 个月 | 管理端改密，或按 9.9 步骤 7 重新生成哈希并 `UPDATE` |

### 9.13.5 定期更新

```bash
# 系统安全补丁（每日自动）
apt-get install -y unattended-upgrades && dpkg-reconfigure -plow unattended-upgrades

# 基础镜像更新（每月一次；先备份再重建，重建属计划内停机 10~30s）
cd /opt/ruankao/ruankao-online/deploy
./backup.sh
docker compose pull mysql redis
docker compose up -d --no-build mysql redis
docker image prune -f && docker compose ps
```

### 9.13.6 接口限流在单机下的落地

| 场景 | 阈值 | 实现位置 | 落点 |
| --- | --- | --- | --- |
| 登录失败 | **5 次 / 15 分钟** | Redis 键 `rk:login:fail:{username}`，TTL 900s（04 文档 `sys_user` 注释已指定） | 超限返回错误码 `2004`；计数放 Redis 靠 TTL 自动解锁，避免两处状态不一致 |
| 注册 | 同 IP 10 次 / 小时 | Redis 键 `rk:limit:register:{ip}`，TTL 3600s | 超限返回 `1002` |
| 题目纠错反馈 | **同题同用户 3 次 / 24 小时**（UC-U15） | Redis 键 `rk:limit:feedback:{userId}:{questionId}`，TTL 86400s（24h） | 超限返回 `1002` |
| 图片上传 | 同管理员 60 次 / 小时 | Redis 键 `rk:limit:upload:{userId}`，TTL 3600s | 超限返回 `1002` |
| 全站兜底 | 单 IP 20 r/s，突发 40，并发 20 | **Nginx `limit_req_zone` / `limit_conn_zone`**（9.5） | 超限返回 503 |

> 契约对齐（对应评审 A2）
> 「题目纠错反馈」限流的**键名、TTL、阈值以 06 文档 6.11 与 05 文档为准**：`rk:limit:feedback:{userId}:{questionId}`、24h（86400s）、同题同用户 3 次。此前的 `rk:limit:feedback:{userId}` / 3600s / 20 次每小时的写法已废弃——键粒度不足会退化成"全站 20 次/小时"，与产品规则（同一道题每人最多纠错 3 次）不一致。

> 登记对齐（对应评审 C7）
> 上表的 `rk:limit:register:{ip}`（同 IP 10 次/小时）与 `rk:limit:upload:{userId}`（同管理员 60 次/小时）**两个键已登记进 06 文档 6.11 的缓存 Key 表**（满足 06 文档 6.15 检查清单「新增缓存 Key 已在 6.11 登记」）。两侧的阈值与 TTL 必须完全一致：注册 **10 次/小时、TTL 3600s**，图片上传 **60 次/小时、TTL 3600s**；本文档与 06 出现差异时以 06 的键表为准并回改本文档。

> 决策与理由
> **业务限流放 Redis（应用层），同时保留 Nginx 限流**：两者语义不同——Nginx 的 `limit_req` 只能按 IP 与速率，无法表达"同一**用户名** 15 分钟内失败 5 次"这类带业务状态的规则；但把纯 IP 限流也放应用层，则每个攻击请求都要走完 Nginx → JVM → Redis，而 JVM 是这台机器上最贵的资源。**IP×速率挡在门口，业务×次数在应用层判定**，两层各管一段。另一层原因：登录锁定计数键本身就在 Redis，**Redis 一旦暴露且无密码，攻击者可直接 `DEL rk:login:fail:*` 绕过锁定并读取全部会话令牌**——这正是 9.3.1 不映射端口与 9.4.1 强随机口令两条要求的分量所在。

## 9.14 成本估算

只给估算公式与必须考虑的项（价格随区域、促销、汇率浮动，写死会过期）。

```text
# 1) VPS
年成本(VPS) = 月单价 × 12 + max(0, 月流量 - 流量包) × 超额单价 × 12
月流量 ≈ 月 PV × 平均页面传输体积 × (1 + 缓存未命中率)
```

代入本项目量级：100 日活 × 20 PV/日 × 约 0.4MB（gzip 后首屏 + 接口 + 图片）≈ 0.8GB/日 ≈ **25GB/月**。因此"按流量还是按带宽"不是决策点——选最便宜那档即可，1TB/月 的包已富余两个数量级。易漏项：独立 IPv4 是否单收费、快照额度（比 `mysqldump` 更强，能救误删库）、基础 DDoS 防护、2C4G → 4C8G 的升配差价。

```text
# 2) 域名
年成本(域名) = 首次注册价（首年常为促销） + 续费价 × (n-1)
```

首年促销价与续费价常差 2~5 倍，**按续费价做预算**。`.com`/`.net` 在境外注册商无需实名，`.cn` 必须实名并与备案体系绑定（本项目不选）。建议加购 WHOIS 隐私保护与域名锁定（`clientTransferProhibited`）。

```text
# 3) 证书
年成本(证书) = 0        # Let's Encrypt 免费，同一域名每周签发上限 50 张，正常使用远达不到
```

隐性成本：**续期失败的人工介入**（9.6.2 的脚本与 15 天预警即为压低此项，单次故障约 30 分钟排查）；若未来需要通配符证书（webroot 不可用，须 DNS-01 + API Token 插件，约 1~2 小时一次性工作）；本项目浏览器基线为最近两个大版本，无需商业证书。

```text
# 4) 备份存储
单份全量体积 ≈ 数据库原始体积 ÷ 压缩比        # mysqldump + gzip 经验压缩比约 5:1 ~ 8:1
备份容量     ≈ 单份体积 × 保留份数 + 上传目录体积 × 保留份数
年成本       ≈ 备份容量 × 对象存储单价 + 恢复时的下行流量费
```

代入 03 文档 3.7 与 04 文档 4.9：1 年后 `user_answer_record` ≤ 400 万行、`rk_question` ≤ 5000 条，数据库原始体积约 300~600MB → 单份 dump + gzip 约 **50~100MB**；按 9.11.1 的保留策略**常驻 = 日备 7 份 + 周备 4 份 = 11 份**（对应评审 D4；周备为硬链，与当日日备共享 inode，不额外占盘），即常驻约 **0.55~1.1GB**；`uploads`（估算 <500MB）14 份增量包约 1~2GB。**异地对象存储年成本在"一杯咖啡"量级。** 须计入：恢复时的下行流量费（比上行贵，演练会真实产生）、是否要异地多副本（容量 × 2）、生命周期规则（超 30/90 天转低频或删除，避免成本随时间线性增长）、**备份加密**（含用户数据，建议 `age`/`gpg` 或服务端加密）。

```text
# 5) 年总成本
年总成本 ≈ VPS 月单价 × 12 + 域名续费价 + 证书(0) + 备份容量 × 对象存储单价
         +（可选）快照 / 流量超额 / 独立 IP
```

| 隐性成本 | 量级 | 说明 |
| --- | --- | --- |
| 备案时间 | 本方案 0；迁境内 7~20 工作日且可能被驳回 | 见 9.1.1 |
| 运维时间 | 每月 1~2 小时 | 镜像更新、备份巡检、日志查看 |
| 无 SLA 风险 | 一次机房级故障 = 数小时停机 | 靠备份 + 换机重建 |
| 跨境延迟 | 大陆用户 +30~60ms（香港） | 影响体验不影响正确性 |

> 决策与理由
> **不为"省服务器钱"牺牲可迁移性**。全部差异收敛到 `deploy/.env`（假设 A2）后，换服务器/换厂商的成本 = 装 Docker + `git clone` + 填 `.env` + 执行 9.9，约 1 小时。**这个约 1 小时的迁移能力本身就抵消了"多花几十块选更贵厂商"的预算压力**——选 VPS 时优先看磁盘 IO、网络质量与是否支持随时快照，而不是绝对最低价。