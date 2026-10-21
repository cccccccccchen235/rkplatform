# 22 · 基建轮（`21 §5.5` 的第 1~3 步）

> 本文档承接 [21](21-p2-audit-round.md)（P2 审查轮）。**旧文档一律不回改**：本轮 `01`~`21` 全部保持原样，改动只落在**新建**的 `scripts/verify/`、`.github/workflows/` 与本文档。
>
> **边界**：本轮**未改 `server/` 与 `web/` 的任何代码**，也**未改 `docs/07`、`.impeccable/`、种子数据**（后三者的原因见 §3）。P2 内容结构本体未开始。
>
> **编号说明（对 `21:300` 的一处有意偏离）**：[21 §7](21-p2-audit-round.md) 结尾写"下一轮（P2 内容结构）完成后另出 `22`"。但委托方本轮选定的是 [`21 §5.5`](21-p2-audit-round.md) 排在第 1~3 位的**基建与文档裁决**，不是 P2 内容结构本体。故本轮占用 `22`，**P2 内容结构轮顺延为 `23`**。理由：若跳过 `22` 直接把它留给 P2 轮，则本轮的迁移与基建交付将没有留痕处，而 `21:300` 的"下一轮"本就以"按 `§5.5` 顺序推进"为前提。

---

## 1. 本轮范围与委托方的三条裁决

`21 §5.5` 给出的顺序是 1) H1 归属 + 首次提交 → 2) 四条文档裁决 → 3) `scripts/` + CI → 4) P2 本体 → 5) 顺手补测。开工前就三条会改变工作内容的岔路征询了委托方，答复如下（**这是本轮的授权边界，§6 据此划定"没做什么"**）：

| # | 问题 | 委托方裁决 | 本轮据此 |
| :-: | --- | --- | --- |
| 1 | 本轮做到哪一步 | **先做第 1~3 步** | 做 H1 归属、四条裁决的**登记**、`scripts/`、CI；P2 本体不动 |
| 2 | `07` 属"不回改"保护范围，四条裁决是否现在执行 | **只登记不改文档** | **不碰** `07`、`.impeccable/`、种子数据；结论全部登记在本文档 §3，回填时机由委托方定 |
| 3 | 是否授权首次 `git commit`、怎么修 H1 | **暂不提交** | 本轮**不做任何 `git commit`**；H1 的"归属"半边照做（§2），"进版本控制"半边留待提交 |

> 裁决 2 与 [18:7](18-p2-frontend-deviations.md) 的规则完全一致——那句写的是"需要回填的地方只标注'**待回填**'，**实际回填由你决定时机**"。本轮把"标注"这一步做全（§3），把"回填"原样留给委托方。

---

## 2. H1 处置：验证仪器搬出被忽略的 `logs/`

### 2.1 事实复核（与 `21 §3 H1` 一致）

`.gitignore` 第 50~51 行忽略 `*.log` 与 `logs/`，而 `logs/` 里同时住着**验证仪器**（12 个脚本/对照）、**运行期日志**（`ruankao-server.log` 等）与 **API 取样 JSON**。后果：`git clean -xdf` 或一次例行清日志 = 销毁验证仪器，而 `20` 把三页回测、`F10` 首屏数字、检测器结论全挂在这批脚本上。

### 2.2 处置：迁移到 `scripts/verify/`（`21 §3 H1` 建议 1）

选建议 1 而非建议 2（补 `.gitignore` 否定项）的理由：否定项只让脚本"勉强能提交"，**不解决"日志与仪器同目录"这个根**——清日志的手仍在同一个目录里。`scripts/` 是 [README §3](../README.md) 与 [10 §10.3](10-roadmap.md) 已声明的离线工具落点，仪器本就该住在这里。

迁移映射（**14 个引用点**按本文档 §3 的 H2 口径，逐文件列出）：

| 原路径（`21` 及更早文档里写的） | 现路径 |
| --- | --- |
| `logs/auth-chain.js` | [`scripts/verify/auth-chain.js`](../scripts/verify/auth-chain.js) |
| `logs/capture.js` | [`scripts/verify/capture.js`](../scripts/verify/capture.js) |
| `logs/contrast.js` | [`scripts/verify/contrast.js`](../scripts/verify/contrast.js) |
| `logs/firstload.js` | [`scripts/verify/firstload.js`](../scripts/verify/firstload.js) |
| `logs/inspect-pages.js` | [`scripts/verify/inspect-pages.js`](../scripts/verify/inspect-pages.js) |
| `logs/measure.js` | [`scripts/verify/measure.js`](../scripts/verify/measure.js) |
| `logs/measure-interaction.js` | [`scripts/verify/measure-interaction.js`](../scripts/verify/measure-interaction.js) |
| `logs/probe.js` | [`scripts/verify/probe.js`](../scripts/verify/probe.js) |
| `logs/probe-register-token.js` | [`scripts/verify/probe-register-token.js`](../scripts/verify/probe-register-token.js) |
| `logs/detector-control.html` | [`scripts/verify/detector-control.html`](../scripts/verify/detector-control.html) |
| `logs/detector-control-linked.html` | [`scripts/verify/detector-control-linked.html`](../scripts/verify/detector-control-linked.html) |
| `logs/detector-control-linked.css` | [`scripts/verify/detector-control-linked.css`](../scripts/verify/detector-control-linked.css) |

**`logs/` 迁后只剩运行期输出**：11 个 `.log`（含 1 个 `.gz` 归档）与 3 个 API 取样 JSON（`me.json` / `proxy.json` / `subj.json`）。这三份 JSON 是运行期取样、不是仪器，留在原处。

迁移后修掉了一处**脚本内的自引用**：[`detector-control-linked.html:23`](../scripts/verify/detector-control-linked.html) 原文写"`logs/probe.js` 去读 `document.styleSheets`"，已改为 `scripts/verify/probe.js`。其余 11 个文件**无任何路径依赖**（实测 `grep -n "logs/\|\.\./\|__dirname\|process.cwd" scripts/verify/*` 零命中），所以搬动是纯粹的移动；唯一的文件间引用是 `detector-control-linked.html` 相对链接 `detector-control-linked.css`，两者同时搬运故仍解析。新增 [`scripts/verify/README.md`](../scripts/verify/README.md) 记录每个脚本测什么、前置条件、以及"那两组对照不要删"。

### 2.3 显式作废 `19 §5` 的"可删"授权（`21 §3 H1` 建议 3）

[19 §5](19-p2-frontend-open-items.md) 原话把 `logs/` 下的脚本称为"**非产物、可删**"。**该授权是错的，自本文档起作废**：

> **作废声明**：`19 §5` 关于"`logs/` 下的可复现脚本非产物、可删"的表述**不再有效**。那批脚本是**验证仪器**，已迁至 `scripts/verify/`，**不得删除**；其中 `detector-control.html` / `detector-control-linked.{html,css}` 是 `impeccable detect` 的**永久阳性对照**（[20 §4.3](20-p2-verification-round.md)），删除即失去"生产预览上的 `[]` 不等于规则不存在"这条结论的唯一证据。

`19` 本身按约定不回改，故作废声明落在这里。

### 2.4 尚未补齐的一半

H1 的完整修复含两步：**① 搬出被忽略的目录**（本轮已做）+ **② 进版本控制**（未做——委托方裁决 3 暂不提交）。所以现状是：仪器已不在会被 `git clean -xdf` 扫掉的目录里，但**仍未进版本**，换机器/重新 clone 依然为零。`21 §3 H1` 的"建议 4：立即做首次 `git commit`"因此**仍然待办**。

---

## 3. `21 §5.1` 四条裁决：**已登记，未执行**

按委托方裁决 2，本节只做"标注待回填"，**未改 `07`、未改 `.impeccable/`、未改种子数据**。四条逐条如下，回填时机由委托方定。

### 3.1 第 1 条 · 重写 `07 §7.9` 的主题定制段

| 项 | 内容 |
| --- | --- |
| 依据 | [18 §6](18-p2-frontend-deviations.md) 待裁决表 + `21` 的 **H3** |
| 现状（本轮复核，三处均未回填） | [07:448](07-frontend.md) 主色仍写 `#409eff` 且理由写"与 Element Plus 默认主色一致，减少主题定制面"；[07:461](07-frontend.md) 仍写"方式：SCSS 变量覆盖 —— `@forward … with ($colors: …)`，由 `element-override.scss` 承载"；[07:475](07-frontend.md) 预算式子仍列不存在的 chunk |
| 推荐 | **a（现在就补）** |
| **H3 的范围修正** | `18 §6:603` 只写"`$rk-primary` 回填为 `#2a72c2`"。**这偏窄**：`07:461` 那段 `@forward` 机制正是 `F14` 证明**从未生效**的死代码（`ElementPlusResolver()` 默认 `importStyle: 'css'`，SCSS 编译期变量对预编译 CSS 无效）。只改颜色 = 把**错方法**留在文档里，下一个人会照着重写一次，再花一轮查"为什么颜色没变"。故应把该条从"回填颜色"扩为"**重写 `07 §7.9` 的主题定制段**"：颜色 → `#2a72c2`；机制 → 写明 `@forward` 在 `importStyle: 'css'` 下是死代码、实际生效的是 `:root` 运行期变量覆盖（`web/dist` 的加载顺序即 `F14` 的单点依赖，见 `21 §2.3`），派生档按 Element Plus 的混合公式现算 |

### 3.2 第 2 条 · 回填方向契约（落点清单按 **H2** 修正）

| 项 | 内容 |
| --- | --- |
| 依据 | [18 §6:604](18-p2-frontend-deviations.md) + `21` 的 **H2** |
| 推荐 | **a**；且与第 1 条**一次改完，不分两次**（同一件事的两处落点） |

**H2 的修正**：`18 §6` 与 `20 §5.1` 把漂移落点写成「`.impeccable/surfaces/*.md` 与 `.impeccable/brief-body-home.md`」。实测 `409eff` 在 `.impeccable/` 下的分布是：

| 文件 | 出现次数 | 是否已在登记清单里 |
| --- | --: | :-: |
| `.impeccable/payloads/surface-home.json` | **10** | ❌ 未登记 |
| `.impeccable/brief-body-home.md` | 2 | ✅ |
| `.impeccable/surfaces/web-src-views-portal-home-vue.md` | 2 | ✅ |
| `.impeccable/design.json` | 2 | 非漂移（是"记录已替换"的叙事） |

真实漂移 = **3 文件 14 处**，原清单只覆盖 4 处（29%）。按原清单执行会漏 71%。`payloads/surface-home.json` 是"掷牌阶段"的输入（五张候选牌共用一套色板），回填时必须一起改，否则下一轮做设计评审读到的候选方案仍是 `#409eff`。

### 3.3 第 3 条 · 改写 `07 §7.10` 的预算式子

| 项 | 内容 |
| --- | --- |
| 依据 | [18:270](18-p2-frontend-deviations.md) |
| 现状 | [07:475](07-frontend.md) 的首屏预算式子里仍列 `vendor-katex` / `vendor-markdown` 两个 **`dist` 里不存在**的 chunk（`21 §2.5` 已核：`dist/assets` 共 14 个文件，无此二者） |
| 推荐 | **a**。去掉不存在的 chunk 名，改用"首屏**同源传输字节**"表述——即 `scripts/verify/firstload.js` 的口径（CDP `encodedDataLength`、冷缓存、`Document`+`Script`+`Stylesheet` 三类计和）。当前实测 **379.85 KB**，超 350 KB 预算 29.85 KB（[18](18-p2-frontend-deviations.md) `F10`） |

### 3.4 第 4 条 · 种子数据的 `contentStatus` 自相矛盾

| 项 | 内容 |
| --- | --- |
| 依据 | [19 §2(3)](19-p2-frontend-open-items.md)、[18 §6](18-p2-frontend-deviations.md)、语义出处 [04:172](04-database.md) 与 [02:48](02-domain-model.md) |
| 事实 | `contentStatus = BULK` 有 **3** 门（`SAD` / `IPMP` / `SD`），而全部 **20** 门的 `questionCount` 都是 **0** |
| 推荐 | **改数据、不改语义**。`contentStatus` 的定义是**当前题量规模**（`SAMPLE` 样例 / `BULK` 200~300 题），不是"计划规模"，故 `questionCount = 0` 时标 `BULK` 是自我矛盾。下一轮把 3 门改回 `SAMPLE`，等 P3 真题量到位再翻 `BULK`。**不要**反向改语义（那要连动 `02`/`04`/`05` 三份文档，并推翻 `18:474` 已做的判断）。改动需连 `deploy/sql/init-data.sql` 一起做（[`init-data.sql:33-37`](../deploy/sql/init-data.sql) 那三行） |
| 附带发现 | 现有数据里 `contentStatus` 与 `outlineStatus` **完全冗余**——3 门 `FULL` 恰好对应 3 门 `BULK`，17 门 `SKELETON` 对应 17 门 `SAMPLE`。这不构成问题（两个字段语义不同、将来会解耦），但**当前数据无法区分二者**，所以只看数据会以为改一个就够 |

---

## 4. 工程基建：已交付

### 4.1 `scripts/`（`15 L8` 的遗留之一）

`21` 与 `15` 都指出"`scripts/` 目录不存在"（[README §3](../README.md) 与 [10 §10.3](10-roadmap.md) 交付物 1 都声明了它）。本轮借 H1 的迁移把它建起来：

```
scripts/
└── verify/                    # 验证仪器（原 logs/ 下的 12 个文件）
    ├── README.md              # 每个脚本测什么、前置、对照的用法
    ├── check-numeric-consistency.sh
    ├── check-no-corpus-files.sh
    └── 12 个迁移来的脚本/对照
```

注意 README §3 原措辞是"离线工具：AI 造题、数据导入、SQL 校验"——那是 `scripts/generate/` 一类**业务**工具。验证仪器是**第二类**用途，故 README §3 的该行已补注（§4.3）。

### 4.2 `.github/workflows/`（`15 L7` 的遗留之一）

新增 [`.github/workflows/consistency.yml`](../.github/workflows/consistency.yml)，两个 job，**都不需要数据库、网络或任何凭据**（AC-1）：

| job | 断言 | 脚本 |
| --- | --- | --- |
| `numeric-consistency` | 文档与 SQL 里的"表数量 / 科目数"复写点与产物一致 | [`check-numeric-consistency.sh`](../scripts/verify/check-numeric-consistency.sh) |
| `no-corpus-files` | `scripts/generate/` 下无真题语料（GR-1，[08 §8.4.4](08-ai-pipeline.md)） | [`check-no-corpus-files.sh`](../scripts/verify/check-no-corpus-files.sh) |

**为什么这样写**（对应 [10 §10.2](10-roadmap.md) 的"复写不可避免的加 CI 检索断言"）：期望值**从产物反推，不在脚本里另写一份常量**——

- 表数量 ← `deploy/sql/schema.sql` 的 `CREATE TABLE` 条数
- 科目数 ← `deploy/sql/init-data.sql` 的 `rk_subject` INSERT 值元组数

这样真源仍只有两处；脚本不再制造第三份数字（正是 `10 §10.2` 批评的"同一个数字被两个文档各写了一遍"）。

**为什么不并进 [09 §9.8](09-deployment.md) 的 `ci.yml`**：那条流水线负责 server/web 构建与镜像（要 secrets、packages 权限、MySQL service）。本文件只跑两个纯文本断言，拆开后可在任何 PR/fork 上无条件运行，且 `docs/`、`deploy/sql/` 的改动不必等构建。

### 4.3 README 的索引与结构行

- §4 文档索引新增 `22` 一行——否则 `22` 成孤儿文件，且 [10 §10.2](10-roadmap.md) 的"README 索引中列出的每个文件都存在"会失效。
- §3 仓库结构新增 `.github/workflows/`，并把 `scripts/` 一行补注了验证仪器这一类。

这两处是**索引与结构的完整性维护**，不是 §3 那四条裁决的回填，故不受裁决 2 限制。

---

## 5. 取证（不按印象勾选）

| 结论 | 取证方式 | 结果 |
| --- | --- | --- |
| 迁移后 `logs/` 只剩运行期输出 | `ls logs/` → 11 个 `.log`/`.gz` + 3 个 JSON，无 `.js`/`.html`/`.css` | ✅ |
| 迁移的 12 个文件可运行（无路径依赖） | `grep -n "logs/\|\.\./\|__dirname\|process.cwd" scripts/verify/*` → 零命中；唯一自引用 `detector-control-linked.html:23` 已改 | ✅ |
| 一致性断言通过 | `bash scripts/verify/check-numeric-consistency.sh` → **13 通过 / 0 失败**，exit 0 | ✅ |
| 一致性断言**能失败**（负向对照，防"永不报警的仪器"） | 在 `/tmp` 的副本上：①未改→通过；②`docs/10` 的 `27 张表`→`28 张表` → **1 失败非零退出**；③`init-data.sql` 删一行科目 → 科目数反推 19，**10 处复写点全部报错**，exit 1 | ✅ 两个方向都咬得住 |
| 反推真源与**实际库**一致 | 只读查询 `ruankao`：tables 27 / subjects 20 / templates 20 / sittings 25 / sections 45 / sys_config 15 | ✅ 与 `verify.sql` 的期望值全部吻合 |
| GR-1 断言通过 | `bash scripts/verify/check-no-corpus-files.sh` → exit 0（`scripts/generate/` 尚未交付） | ✅ |
| workflow YAML 合法 | `js-yaml` 解析：`name=consistency`，2 jobs，triggers `push/pull_request/workflow_dispatch`，各 2 step | ✅ |
| `07` / `.impeccable/` / 种子数据**确未改动** | 本轮只新建 `scripts/verify/*`、`.github/workflows/*`、`docs/22`、`scripts/verify/README.md`，并改 `README.md` 两行 | ✅ |
| `server/` 与 `web/` 源码确未改动 | `find server web -newermt "2026-10-02 13:15"`（本轮开始时刻）→ **零命中**。**带对照**：把界限放宽到 `00:00` 时能命中午前那轮（`13:01~13:03`）改的 `PortalLayout.vue`、`styles/*.scss`、`home.vue`、`subject-list.vue` 等，证明这个过滤条件本身有效，不是"永远返回空" | ✅ |
| 迁移是纯移动、未改坏脚本 | 从新位置直接运行：`probe.js` → 判词仍为"全部是死规则（0 个元素）"、`#909399` 计数 0；`firstload.js` → 首屏 **379.85 KB**、`XHR 5.04 KB /api/v1/subjects`。**与迁移前的数逐位相同** | ✅ |
| 未做任何 `git commit` | 全程未调用 `git add` / `git commit` | ✅ |
| `H2` 的 14 处分布 | 逐文件计数 `409eff`：`payloads/surface-home.json` 10 / `brief-body-home.md` 2 / `surfaces/web-src-views-portal-home-vue.md` 2（`design.json` 的 2 处为叙事性引用，不计） | ✅ |

---

## 6. 本轮**没做**的事（边界明示）

| 没做 | 依据 |
| --- | --- |
| 任何 `git commit` | 委托方裁决 3；`21 §5.5` 第 1 步的另一半，待办 |
| 回填 `07 §7.9` / §7.10、`.impeccable/` 方向契约、种子数据 `contentStatus` | 委托方裁决 2；结论登记在 §3，回填待授权 |
| P2 内容结构本体（后端三组 CRUD、管理端页面、考生端详情/大纲树、3 科章节树数据） | 委托方裁决 1；按 [`21 §5.2`](21-p2-audit-round.md) 整体移入 `23` |
| [`21 §5.3`](21-p2-audit-round.md) 的四项未测（`2002`、浏览器内 `2005` 跳转、`stores/user.ts` 持久化+守卫、三页设计观看） | 同上；且它们依附在 P2 本体的页面上，`21 §5.5` 本就排在第 5 步 |
| 首屏超预算 29.85 KB 的优化 | `21 §5.6` 明令不做（等页面齐全再决定） |
| `comps` / `F13` | `21 §5.6` 明令不动（已披露的流程状态，非缺陷） |

---

## 7. `H4` 判据登记（给下一个做核实的人）

`21 §3 H4` 记的是它**自己**在复核中得到的假阴性：查 `lastLoginAt` 时用区分大小写的模式 → 零命中 → 一度认定 `20 §2.2` 的引用是编造的，实际引用完全准确（成因：驼峰里的大写 `L` 不在模式里）。

本文档照录该判据，并**在 §5 的负向对照里执行了同一精神**：

> 查"某符号/某常数是否在某文件出现、是否一致"时，模式要么放宽大小写，要么把各种写法都列进去；而**任何断言脚本都必须先用一次"人为制造漂移"证明它会失败**——否则你得到的 `✓` 只证明"脚本跑完了"，不证明"断言有效"。这与 `20 §4.2` 的 `probe.js` 坑、`13` 对"自查按印象勾选"的批评同源。

---

## 8. 文档链现状

| 序号 | 文件 | 角色 | 状态 |
| :-: | --- | --- | --- |
| 11 | [11-review-findings.md](11-review-findings.md) | 第一轮体检（38 条 + 6 项裁决） | 留痕，不改 |
| 12 | [12-changes-applied.md](12-changes-applied.md) | 第一轮手术 | 留痕，不改 |
| 13 | [13-review-round2.md](13-review-round2.md) | 第二轮体检（44 条复跑 + G1~G9） | 留痕，不改 |
| 14 | [14-changes-round2.md](14-changes-round2.md) | 第二轮手术（G1~G9） | 留痕，不改 |
| 15 | [15-implementation-deviations.md](15-implementation-deviations.md) | P1 实现出入（11 条） | 留痕，不改（其 L7/L8 的 `scripts/` 与 CI 缺口**本轮部分收口**） |
| 16 | [16-p1-code-audit.md](16-p1-code-audit.md) | P1 代码独立审查（E1~E8） | 留痕，不改 |
| 17 | [17-changes-p1-audit.md](17-changes-p1-audit.md) | P1 审查的处置记录 | 留痕，不改 |
| 18 | [18-p2-frontend-deviations.md](18-p2-frontend-deviations.md) | P2 前端出入（`F` 系列） | 活文档；本轮**未改**（§3 的登记与之无冲突） |
| 19 | [19-p2-frontend-open-items.md](19-p2-frontend-open-items.md) | P2 前端待办（输入清单） | 不回改；**其 §5 的"可删"授权自本文档起作废**（§2.3） |
| 20 | [20-p2-verification-round.md](20-p2-verification-round.md) | P2 补测轮记录 | 留痕，不改（其 `logs/` 路径已过期，映射见 §2.2） |
| 21 | [21-p2-audit-round.md](21-p2-audit-round.md) | P2 审查轮（对 `20` 的独立复核 + `H1`~`H4` + 下一轮范围） | 留痕，不改（其 `logs/` 路径已过期，映射见 §2.2） |
| **22** | **本文件** | **基建轮：H1 处置 + 四条裁决登记 + `scripts/` 与 CI 交付** | **本轮交付** |
| 23 | *（预留）* | P2 内容结构本体（`21 §5.2`）+ `§5.3` 的四项未测 | 未开始 |

**冲突裁决顺序**：`22` > `21` > `20` > `19` > `18`（同一阶段内，新者优先）；跨阶段沿用 `17` > `16` > `15` > `14` > `13` > `12` > `11`。

**后续约定**：`21 §5.5` 的第 4、5 步（P2 内容结构 + 四项未测）完成后另出 `23`，不修改本篇。
