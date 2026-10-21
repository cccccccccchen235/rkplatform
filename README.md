# 软考在线学习平台（ruankao-online）

> 基于 Vue 3 + Spring Boot 的软考（计算机技术与软件专业技术资格）在线学习平台。
> 全科目结构性覆盖、模板驱动的机考仿真、AI 辅助题库生产与人工审核闭环。
>
> **当前状态：设计已定稿（`docs/01`~`10`），P1 后端骨架已落地并经一轮独立审查（[16](docs/16-p1-code-audit.md)）与整改（[17](docs/17-changes-p1-audit.md)）**——六个 Maven 模块、`GET /api/v1/subjects` 垂直切片、AC-1 环境变量启动校验、`--rk.password-gen` 哈希生成器、27 张表的建库与初始化脚本均已实测通过；本地开发的 `deploy/docker-compose.infra.yml` 与 `deploy/.env.example` 已交付。**前端 `web/` 未开始**；**生产** compose、Nginx 与前端 Dockerfile 未交付（见 [15](docs/15-implementation-deviations.md) 的 L8/L9）。

---

## 1. 项目定位

为软考考生提供一个"刷题 → 错题复盘 → 机考仿真 → 数据反馈"的闭环学习工具，同时以**题库生产流水线 + 审核工作台**解决"题库从哪来、质量谁保证、版权怎么规避"这三个真实问题。

本项目是一个**工程实践型项目**（vibe-coding 尝试），设计目标是：用一套泛化到全部 20 个软考科目的数据模型，支撑"每一科目的客观题刷题、主观题对照、机考模拟"，而不追求内容体量上的全覆盖。

### 范围一句话

| 做 | 不做 |
| --- | --- |
| 20 个科目全部建立"级别 → 科目 → 大纲章节 → 知识点"结构 | 不追求每科目海量题库（3 个热门科目做量，其余做样例） |
| 客观题全流程自动判分 | 主观题（应用技术 / 案例分析 / 论文）**不判分**，仅给参考答案 |
| 模板驱动的机考仿真（卷面可后台配置） | 不实现 Office 操作题（技术上不可行） |
| AI 按「考点 + 题型」生成题目 + 人工审核入库 | 不在产品内提供 AI 答疑 / 智能批改等在线 AI 功能 |
| 错题本、章节与知识点双维度学习统计 | 不做社区、付费课程、打卡、排行榜 |

---

## 2. 技术栈

| 层 | 选型 |
| --- | --- |
| 前端 | Vue 3.5 · Vite · TypeScript · Pinia · Vue Router · Element Plus · ECharts · Axios · KaTeX |
| 后端 | JDK 21 · Spring Boot 3.3 · MyBatis-Plus · Sa-Token · Knife4j · Hutool · Jakarta Validation |
| 存储 | MySQL 8（主） · Redis（会话 / 缓存 / 限流 / 统计） |
| 工程 | Maven 多模块 · pnpm · GitHub Actions · JUnit 5 + Mockito |
| 部署 | Docker · Docker Compose · Nginx · 部署目标无关（假设单机 VPS + 域名） |

选型理由与取舍见 [03-architecture.md](docs/03-architecture.md#32-技术选型与理由)。

---

## 3. 仓库结构

```
ruankao-online/
├── README.md                 # 本文件：索引 + 快速开始
├── docs/                     # 文档集（设计 01~10 + 评审/手术/实现期记录，完整索引见 §4）
├── server/                   # 后端：Maven 多模块单体
│   ├── rk-common/            # 通用：响应体、异常、工具、常量
│   ├── rk-domain/            # 领域模型：实体、枚举、DTO、VO
│   ├── rk-repository/        # 持久层：Mapper、Redis DAO
│   ├── rk-service/           # 业务：题库、考试、统计、审核
│   ├── rk-api/               # 对外接口：Controller、鉴权、Knife4j
│   └── rk-boot/              # 启动模块：配置、装配、打包
├── web/                      # 前端：单 Vue 工程，portal / admin 目录隔离
│   └── src/
│       ├── views/portal/     # 考生端
│       ├── views/admin/      # 管理端（/admin/* 懒加载）
│       ├── api/              # 接口封装
│       ├── stores/           # Pinia
│       └── router/           # 路由与守卫
├── scripts/                  # 离线工具：AI 造题、数据导入（generate/）；验证仪器（verify/）
├── .github/workflows/        # CI：数值一致性断言、GR-1 语料目录纯净
└── deploy/                   # 部署：compose、Nginx、环境变量模板、初始化 SQL
```

---

## 4. 文档索引

| 文档 | 内容 | 何时读 |
| --- | --- | --- |
| [01-requirements.md](docs/01-requirements.md) | 角色、用例、功能清单、非功能需求、**范围外清单** | 想确认"做什么、不做什么" |
| [02-domain-model.md](docs/02-domain-model.md) | 20 科目体系、题型分类、大纲与知识点、卷面模型、状态机、**版权策略** | 理解业务规则与数据来源约束 |
| [03-architecture.md](docs/03-architecture.md) | 总体架构、技术选型理由、模块划分、**ADR 决策记录** | 想改架构或质疑选型 |
| [04-database.md](docs/04-database.md) | ER 概览、表清单、**完整 DDL**、初始化数据、容量估算 | 建库、改表 |
| [05-api.md](docs/05-api.md) | 接口约定、错误码表、**字段级接口清单** | 写前后端接口 |
| [06-backend.md](docs/06-backend.md) | 分层与包结构、**组卷算法**、判分、掌握度计算、计时补偿、缓存规范 | 写后端业务 |
| [07-frontend.md](docs/07-frontend.md) | 工程结构、路由与权限、页面清单、状态管理、组件约定 | 写前端页面 |
| [08-ai-pipeline.md](docs/08-ai-pipeline.md) | 造题流水线、Prompt 契约、输出 Schema、校验规则、**版权护栏** | 造题与审核 |
| [09-deployment.md](docs/09-deployment.md) | 容器化、Compose、Nginx、环境变量、上线检查单、备份 | 部署上线 |
| [10-roadmap.md](docs/10-roadmap.md) | 分阶段里程碑与**验收标准** | 排期与自检 |
| [11-review-findings.md](docs/11-review-findings.md) | 独立交叉评审的 38 条发现（**体检报告**，记录修订前的问题） | 了解曾经存在的问题 |
| [12-changes-applied.md](docs/12-changes-applied.md) | 第一轮修订的手术记录：6 项裁决结论、场次模型重构、38 条逐条落实、遗留项 | 想确认"**第一轮**某个问题改了没" |
| [13-review-round2.md](docs/13-review-round2.md) | 第二轮复查（**体检报告**）：44 条改动复跑确认，新发现 9 项（G1~G9） | 了解第二轮复查的意见 |
| [14-changes-round2.md](docs/14-changes-round2.md) | 第二轮修订的手术记录：G1~G9 落地、一处对 13 字面要求的有意偏离及理由 | 想确认"**第二轮**某个问题改了没"；文档内部冲突时 `14` > `13` > `12` > `11` |
| [15-implementation-deviations.md](docs/15-implementation-deviations.md) | P1 实现与设计文档的**出入记录**：11 条（文档写错的 / 数字打架的 / 文档没写而实现自行决定的） | 写代码或部署前，想确认"**文档与代码**当前哪里对不上"（这些条目 `15` 优先） |
| [16-p1-code-audit.md](docs/16-p1-code-audit.md) | 对 P1 代码的**独立审查** + 四项待裁决的处置方案；新增 8 条发现（E1~E8），其中一条测试缺陷的假阳性会把人推向真漏洞 | **动 `server/` 的代码之前先看这篇**（该轮未改任何代码） |
| [17-changes-p1-audit.md](docs/17-changes-p1-audit.md) | 16 那份方案的**落地记录**：D/E 逐条落到哪个文件、7 条执行期新发现（N1~N7）、实测证据与**未实测清单** | 想知道"16 说的那些**到底改了没、怎么验的**" |
| [18-p2-frontend-deviations.md](docs/18-p2-frontend-deviations.md) | P2 前端实现与设计的**出入记录**（`F` 系列，含 `F14` 主色与死代码发现） | 写前端页面前，想确认设计与实现的出入（活文档） |
| [19-p2-frontend-open-items.md](docs/19-p2-frontend-open-items.md) | P2 前端**停下时还没做完的事**（交接清单） | 了解某一轮中断时还剩什么（其 §5 的"脚本可删"授权已被 `21` 的 H1 判定为错，并由 `22 §2.3` **正式作废**） |
| [20-p2-verification-round.md](docs/20-p2-verification-round.md) | P2 **补测轮**：Redis 就位后的端到端鉴权验证、`F14` 主色落地、两处度量仪器更正 | 想知道"`F10` 的首屏数字、检测器结论从哪来" |
| [21-p2-audit-round.md](docs/21-p2-audit-round.md) | 对 `20` 的**独立复核**（12 组主张全部复现）+ 4 条新发现（H1~H4）+ **下一轮工作的范围与顺序** | **开工下一轮之前先看这篇**——它同时指出"P2 前端"与 `10 §10.4` 的 P2 不是同一件事 |
| [22-infra-round.md](docs/22-infra-round.md) | 基建轮：验证仪器搬出被忽略的 `logs/`（H1）、四条文档裁决的**登记**、`scripts/verify/` 与 CI 交付 | 想知道"`07` 那些待回填到底登记在哪、验证脚本现在放哪、CI 断言是什么" |
| [23-next-round-plan.md](docs/23-next-round-plan.md) | **下一轮任务安排**：P2 内容结构本体（`10 §10.4`）+ 四项未测；并按"**本地完成、暂不上传仓库**"重排了 `21`/`22` 的三处安排 | **开工前先读这篇**——它给出两个必须先解决的前置（`bash` 不在 PATH、四条裁决未授权）与完整的验收判据 |
| [24-execution-record.md](docs/24-execution-record.md) | `23` 那份计划的**执行记录**：四条裁决回填、三科大纲树数据、一键门禁全绿、`§3.6` 八条验收（2 通过 / 6 被同一根因阻塞）、四项未测 | 想知道"`23` 的计划**做到哪一步、卡在哪**"（其 §9.1 的归因已由 `25 §3.1` 修正） |
| [25-progress-import-and-fixes.md](docs/25-progress-import-and-fixes.md) | **题库导入（方案 B）落地** + 服务启动 + 两处后端修复（`StpInterface` 漏项、客户端异常家族）+ **当前总体进度与待办** | 想知道"**现在到底能跑到什么程度**、下一步该做什么" |
| [26-p4-practice-loop.md](docs/26-p4-practice-loop.md) | **P4 练习闭环**（6 种组卷模式 / 服务端复核判分 AC-3 / 幂等上报 / 错题本 / 收藏 / 反馈 / 练习进度）+ 首页去噪 + 对 05/06 的**偏离登记** | 想知道"练习闭环做到哪、哪些地方与设计文档刻意不同、怎么复跑验收" |
| [27-handoff.md](docs/27-handoff.md) | **交接文档**：当前现场、启动与验证步骤、阶段进度、欠账清单、踩坑台账、"下一步做什么" | **接手这个仓库的人先读这一篇**（它是唯一允许被更新的文档） |

---

## 5. 快速开始

### 前置依赖

JDK 21、Maven 3.9+、Node 20+、pnpm 9+、Docker（或本地 MySQL 8 + Redis 7）。

> **装完工具链后必须注销重登或重启**（含完全退出编辑器再打开）。新开的终端/集成终端
> 继承的是父进程启动时的环境快照，否则会出现"PATH 配好了但 `mvn` 命令找不到"的假故障。

### 本地启动

```bash
# 0) 准备环境变量（AC-1：不在代码里写默认值，缺任一项会拒绝启动并一次列全缺失键）
cp deploy/.env.example deploy/.env
$EDITOR deploy/.env        # 至少填 MYSQL_PASSWORD / REDIS_PASSWORD / RK_TOKEN_TIMEOUT / RK_UPLOAD_DIR

# 1) 起依赖（MySQL + Redis）
docker compose -f deploy/docker-compose.infra.yml up -d

# 2) 建库 + 初始化 + 校验（校验脚本"违规即出行为空表"，比打印 OK 更难自欺）
mysql -h127.0.0.1 -uroot -p < deploy/sql/schema.sql
mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/init-data.sql
mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/verify.sql

# 3) 产出并回填管理员密码哈希（不做这步 admin 登录必然失败：库里是占位串）
cd server && mvn -q -pl rk-boot -am package -DskipTests && cd ..
java -jar server/rk-boot/target/rk-boot.jar --rk.password-gen='你的口令'
mysql -h127.0.0.1 -uroot -p ruankao -e \
  "UPDATE sys_user SET password_hash='<上一步输出的 60 字符哈希>' WHERE username='admin';"

# 4) 起后端（默认 8080；需先导入环境变量）
set -a && source deploy/.env && set +a
cd server && mvn -pl rk-boot -am spring-boot:run

# 5) 起前端（P1 未交付，见 docs/15 的 L9）
# cd web && pnpm install && pnpm dev
```

> - 接口文档默认**关闭**（`RK_DOC_ENABLED=false`，生产要求如此，且关掉后 `/doc.html` 返回真 404）。
>   本地要看文档：启动前 `export RK_DOC_ENABLED=true`，再访问 `http://127.0.0.1:8080/doc.html`。
> - **Windows 上用 PowerShell 而非 bash**（`set -a && source` 是 bash 语法）：
>   ```powershell
>   Get-Content deploy/.env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
>       $kv = $_ -split '=', 2; Set-Item "env:$($kv[0].Trim())" $kv[1].Trim() }
>   ```
>   或者干脆在 WSL 里跑后端（Docker 也在那儿）。

---

## 6. 合规声明（摘要）

本项目采取以下数据合规策略，详细规则见 [02-domain-model.md](docs/02-domain-model.md#26-版权与数据来源策略)：**本平台非商业性运营，不提供付费与广告；题库全部由 AI 依据公开的「考点 + 题型」结构独立命制，不复制、不改编任何真题原文；题目文案不出现"真题"字样；公开页提供侵权投诉下架入口。**

> 本文档与代码均为学习/研究用途，不构成法律意见。

---

## 7. 文档约定

- **`docs/01`~`26` 是"设计史"：一个阶段一篇、旧篇永不回改、冲突时新篇优先**（裁决顺序 `26 > 25 > 24 > … > 11`）。漏做的事由新篇登记，而不是回头改旧篇。
- **[27-handoff.md](docs/27-handoff.md) 是唯一允许被更新的文档**——它记录的是"当前现场"（怎么跑、做到哪、欠着什么、哪里有坑），性质与设计史不同。换了机器、改了端口、修完某条欠账之后请回来改它。`DESIGN.md`、`PRODUCT.md`、`.impeccable/` 同理：它们是**当前状态的镜像**，不属于设计史链。
- 文档内出现的**表名、字段名、枚举值、接口路径**为跨文档唯一事实来源，以 `04-database.md` 与 `05-api.md` 为准；改动这两份文档时必须同步检查 `06`、`07`、`08`。
- 关键取舍在 [03-architecture.md](docs/03-architecture.md#36-adr-决策记录) 以 ADR 形式记录，并在相关段落标注 `> 决策与理由`。
- 所有"暂不实现"的项必须在 [01-requirements.md](docs/01-requirements.md) 的范围外清单中登记，不允许在实现阶段被临时加入。