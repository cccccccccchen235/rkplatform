# 11 · 文档评审发现记录

> ⚠️ **本文档是"体检报告"，记录的是修订前的问题。** 全部 38 条发现与 6 项裁决的**处理结论**见 [12-changes-applied.md](12-changes-applied.md)；两者冲突时以 `12` 为准。本文档正文保持原样，作为评审历史留痕。
>
> 📌 **后续**：12 的落地结果已由 [13-review-round2.md](13-review-round2.md) 做第二轮复查——44 条确认真落地，新发现 9 项（G1~G9）**尚未落地**。**三份文档冲突时以 `13` 为准。**
>
> 本文档是一次系统交叉评审的产物，覆盖 `README.md` 与 `docs/01~10`。
> 评审方法：对每条可验证的事实（数值、字段名、缓存键、错误码、状态机迁移、SQL 语义）在文档间做双向核对，不凭印象下结论。
> 本文档本身**不修改任何既有文档**——它是修改动作的清单与验收依据，与 [10-roadmap.md](10-roadmap.md) §10.12「文档与代码不一致时，先改文档再改代码」的回填责任挂钩。

---

## 0. 阅读约定

每条发现按统一格式记录：

- **位置**：冲突双方各自的文件与章节/行号。
- **现状**：两边各写了什么。
- **影响**：照现状实现会发生什么。
- **建议**：推荐的收敛方向（**裁决项**需由项目负责人确认，其余可直接采纳）。

发现分级：

| 标记 | 含义 |
| --- | --- |
| 🔴 | 需裁决的硬冲突：两个文档说了不同的话，选哪个都改变行为 |
| 🟠 | 逻辑缺陷：会让实现出错或验收失败 |
| 🟡 | 契约空缺：写代码前必须补进 05，否则卡住前端 |
| 🔵 | 编辑级：不影响正确性，但会误导实现者 |

---

## 1. 总体评价

这套文档的完成度明显高于同类项目。规范层面做到了自洽且有出处——错误码分段、ADR 带「代价」栏、OOS 明确列出不做什么、GR-1/2/3 从原则下沉到可执行断言、08 甚至把一次「难度方向写反」的历史冲突记录在案。方向性的东西（鉴权选型、模板驱动卷面、主观题不判分、AI 离线化）理由都站得住，不需要推翻。

问题集中在三类接缝处：

1. **同一事实被两个文档各写一遍**（数值、时限、键名）——没有单一出处，于是漂移。这是占比最高的成因。
2. **多文档各自演进后的边界情形**——高级论文的计时、案例分析节的退出条件、题目从 REJECTED 复活后的计数，都属于「主流程写得很细，分支流程漏了」。
3. **05 作为接口契约不够完整**——有接口有路径无字段表，而 07 §7.11 又强制 `types/api.d.ts` 与 05 严格一致，等于把缺口直接传导到前端实现。

---

## 2. 🔴 需裁决的硬冲突（6 项）

### A1 计时权威：Redis 增量 vs `started_at` 派生 — **阻塞**

- **位置**：[04-database.md](04-database.md#L391) 字段注释；[03-architecture.md](03-architecture.md) ADR-03；[06-backend.md](06-backend.md) §6.9.1；[10-roadmap.md](10-roadmap.md#L175) 风险 R6。
- **现状**：
  - 04：`remaining_sec` 字段注释「计时权威在服务端：remaining_sec 由 Redis 维护并定期回写」。
  - 03 ADR-03：模拟考答题会话与剩余秒数「高频读写，不能每次打 MySQL」。
  - 06 §6.9.1：`remainingSec = max(0, totalDurationSec - (now - session.startedAt))`，纯派生。
  - 10 R6：「计时全部由 `started_at` 派生、**不存增量状态**」。
- **影响**：Redis 增量与派生两种模型实现互斥。增量式一旦 Redis 丢键即永久错位；派生式每次重算，抗时钟漂移与丢包。
- **建议**：**06 + R6 胜出，改 04 与 03。**
  - 04 字段注释改为「展示用缓存，真值由 `started_at` 派生」。
  - 03 ADR-03 第 ② 条改为「会话状态（需 TTL 与强制下线）」。

### A2 反馈限流键：24h/3次 vs 3600s/20次

- **位置**：[06-backend.md](06-backend.md) §6.11；[09-deployment.md](09-deployment.md) §9.13.6。
- **现状**：

  | 出处 | 键 | TTL | 阈值 |
  | --- | --- | --- | --- |
  | 06 §6.11 | `rk:limit:feedback:{userId}:{questionId}` | 24h | 同题同用户 3 次（与 05、10.6 一致） |
  | 09 §9.13.6 | `rk:limit:feedback:{userId}` | 3600s | 20 次/小时 |

- **影响**：键粒度（带不带 `questionId`）与阈值双冲突，实现必然二选一。
- **建议**：**06/05 胜出。** 顺带把 09 §9.13.6 的措辞统一为「题目纠错反馈」（当前读起来像「反馈题目有错」）。

### A3 `innodb_buffer_pool_size`：512M vs 1024M

- **位置**：[10-roadmap.md](10-roadmap.md) R4；[09-deployment.md](09-deployment.md) §9.3.1；[03-architecture.md](03-architecture.md) §3.7。
- **现状**：10 R4 写 512M；09 §9.3.1 用 `--innodb-buffer-pool-size=1024M`（注释标「03 文档 3.7」）；03 §3.7 写「MySQL 约占 1G」。
- **影响**：2C4G 机器上 512M 会让索引命中率明显下降。
- **建议**：**1G 为准，改 R4。** 若项目决定压到 2G 内存部署，则应反过来改 09——因此列入裁决。

### A4 容量数字：三个文档三个值

- **位置**：[01-requirements.md](01-requirements.md) §1.5；[03-architecture.md](03-architecture.md#L204-L206) §3.7；[04-database.md](04-database.md) §4.9。
- **现状**：

  | 出处 | 题目量 | 答题记录 |
  | --- | --- | --- |
  | 01 §1.5 | ≤ 2 万条 | ≤ 500 万条/年 |
  | 03 §3.7 | ≤ 5000 条 | 365 万条/年 |
  | 04 §4.9 | ≤ 5000 条 | ≤ 400 万 |

- **影响**：需求上限（01）与估算（03/04）混为一谈；03↔04 的 365 万↔400 万纯冲突，且 09 §9.13.8 的成本估算引用了「04 文档 4.9 的 400 万」。
- **建议**：**以 04 为唯一出处**，03 引用之；01 保留 2 万/500 万作为**设计容量上限**，另起一行写「阶段一目标量见 03 §3.7」，把「上限」与「目标」分开。

### A5 论文节的时长模型缺失 — **阻塞 P5**

- **位置**：[02-domain-model.md](02-domain-model.md#L137)；[04-database.md](04-database.md#L356)。
- **现状**：高级试卷 = 综合知识 + 案例分析（连考 240 min）+ 论文独立 120 min = 360 min；但 `total_duration` 被两处写死为「240 或 120」，且 `exam_session.total_duration_sec` 是**单个**值，表达不了「240 连考 + 120 独立」。
- **影响**：模型中高级考试的时长无法表达，P5 跑不通。
- **建议**（需确认真实考制）：
  - **方案甲**：论文并入连考，`total_duration=360`，论文节 `duration_min=120`。
  - **方案乙**：会话拆两段（改状态机，成本高）。
  - 倾向**方案甲**。02 声称已核实 2026 考制，但此处与自身约束矛盾，请以真实考制为准裁决。

### A6 案例分析节的「可进入下一节」判定不可计算

- **位置**：[06-backend.md](06-backend.md) §6.9.1；[05-api.md](05-api.md) §5.10 规则 4。
- **现状**：06 §6.9.1 要求 `usedSec >= section.durationMin`，且若 `allowCarryOver = false` 则要求 `usedSec >= durationMax`；但 05 §5.10 规则 4 只在 `allowCarryOver = true` 时**要求**填 `durationMax`。
- **影响**：案例分析节（`allowCarryOver=false`）不填 `durationMax` → 门槛值缺失，无法计算何时能进论文节。
- **建议**：05 规则 4 改为「`durationMin` 必填；`durationMax` 在 `allowCarryOver` 任一取值下必填」，并在 02 写清两者语义（最短/最长占用）。

---

## 3. 🟠 逻辑缺陷（15 项）

### B1 `PUBLISHED → DRAFT` 编辑后三处冗余计数不递减

- **位置**：[06-backend.md](06-backend.md) §6.12 步骤 ④；`TRANSITIONS` 表。
- **现状**：④ 只写「发布/下架时更新三处冗余计数」，但 `PUBLISHED` 有 `EDIT → DRAFT` 迁移，编辑已发布题后题目变 `DRAFT`，三处 `question_count` 仍计着它。
- **影响**：计数偏高一整个周期；§6.13「每周一 04:00 冗余计数校验」会持续 WARN 一周。
- **建议**：④ 改为「**凡进入/离开 `PUBLISHED` 的迁移**都更新三处计数」，并列出触发迁移清单（`OFFLINE → DRAFT`、`APPROVED → DRAFT` 同源，一并覆盖，见 B12）。

### B2 08 的一致性巡检公式在正常流程下必然误报

- **位置**：[08-ai-pipeline.md](08-ai-pipeline.md) §8.9.2 第 10 项。
- **现状**：`approved_count + rejected_count + count(status ∈ {DRAFT,PENDING_REVIEW,APPROVED}) == produced_count`。而 `REJECTED → REGENERATE → DRAFT → … → APPROVED` 是 §8.8.5 明文写出的正常流程，这类题同时落进 `rejected_count` 与 `approved_count`，等式左边多计一次。
- **影响**：巡检每天报假警。
- **建议**：改为终态互斥口径，把「曾被拒绝次数」降为参考指标，不参与一致性等式。

### B3 08 的驳回原因分布被 `remark` 前缀污染

- **位置**：[08-ai-pipeline.md](08-ai-pipeline.md) §8.9.2 第 3 项；§8.2.3；§8.8.4；§8.11.2。
- **现状**：第 3 项按 `[...]` 前缀聚合 5 类驳回原因，但同一字段 `remark` 还承载 `[SUSPECT:...]`、`[KP_PENDING:...]`、`[COPYRIGHT]`。
- **影响**：驳回原因分布混入三类杂项，统计失真。
- **建议**：明确「仅当 `action=REJECT` 时参与统计」（最干净），或规定前缀优先级/取最后一个 `[...]`。

### B4 `kpIds` 允许 0 个 vs 强制 1~5 个 — 管理端改不了题

- **位置**：[05-api.md](05-api.md#L479) 校验规则 3；[05-api.md](05-api.md) `PUT /admin/questions/{id}` 用例 7；[07-frontend.md](07-frontend.md) §7.5.2；[08-ai-pipeline.md](08-ai-pipeline.md#L471)。
- **现状**：05 校验规则 3 要求 `kpIds` 1~5；08 却允许「0 个暂挂」导入为 `DRAFT`，且要求管理员「建好标签后再挂载（走 `PUT`）」。
- **影响**：0 个标签的题一旦打开编辑页就保存不了，08 设计的闭环断裂。
- **建议**：改为「0~5 个」，把「发布前 ≥1 个」作为**发布动作**的校验而非保存动作的校验。

### B5 掌握度重算 SQL 与它自己声明的口径相反

- **位置**：[06-backend.md](06-backend.md) §6.10.2。
- **现状**：注释写「按**去重后的最近一次作答**计」，但 SQL 里 `COUNT(DISTINCT r.question_id)` 去重了，`SUM(CASE WHEN r.is_correct = 1 THEN 1 ELSE 0 END)` 没有去重。
- **影响**：一题先错后对 → `correct_count` 有值而末次实际是错的，`accuracy` 系统性偏高，掌握度 `recency` 权重错。
- **建议**：用 `ROW_NUMBER() OVER (PARTITION BY question_id ORDER BY created_at DESC)` 取 `rn=1` 子集再聚合；补单测。

### B6 游客条件放行 vs ADR-02 的反射扫描测试 — 机制冲突

- **位置**：[05-api.md](05-api.md#L508) 白名单；[03-architecture.md](03-architecture.md) ADR-02；[06-backend.md](06-backend.md) §6.5。
- **现状**：`POST /api/v1/practice/papers`「仅 `mode=SAMPLE` 且 `count≤5` 时放行游客」，但白名单是路径常量数组，表达不了「同路径按参数放行」。实现者要么把路径加进白名单（游客可 `mode=FULL` 绕开限制，安全洞），要么改测试（违反 ADR-02）。
- **影响**：两条路都不可接受。
- **建议**：该路径标 `@SaIgnore` + **方法内**校验 `mode=SAMPLE && count≤5`，否则抛 `2005`；从 `AuthWhitelist` 常量移除，留注释指向方法内校验；补测试 `mode=FULL` 未登录 → `2005`。

### B7 GR-2「必填」vs DDL 允许 NULL

- **位置**：[04-database.md](04-database.md#L236-L238)；[08-ai-pipeline.md](08-ai-pipeline.md) §8.2.3。
- **现状**：`source_ref` / `source_license` / `generation_batch_id` 在 DDL 中全为 `NULL`；08 却要求三者「为空时拒绝（3004）」。
- **影响**：契约仅靠应用层守，DDL 不设防，绕过 import 接口的写入可落库。
- **建议**：① 08 显式声明该约束在**应用层**强制（因 `source_type` 各取值所需字段不同，DDL 保持 NULL）；② 补一条**发布前**校验（`PUBLISHED` 时四字段非空）。

### B8 错误码 `4006` 定义了但无人引用

- **位置**：[05-api.md](05-api.md) 错误码表。
- **影响**：无绑定接口的错误码会在实现时被随意占用。
- **建议**：写清它绑哪个接口，或删除。

### B9 `GET /exams` 未过滤 `ABANDONED`

- **位置**：[05-api.md](05-api.md) 用例 17；[10-roadmap.md](10-roadmap.md#L120)。
- **现状**：05 只给了 `subjectId?` 与分页；10 验收要求 `ABANDONED` 不出现在列表中。
- **影响**：实现易漏。
- **建议**：05 补「默认仅返回 `status IN ('SUBMITTED','SCORED')`」。

### B10 `flushSave()` 被当既有 API 用，但从未定义

- **位置**：[07-frontend.md](07-frontend.md#L244)、§7.4.4、§7.5.1、§7.5.2、`onBeforeRouteLeave` 共 5 处调用；`stores/exam.ts` action 表。
- **现状**：5 处调用 `flushSave()`，action 表只定义了 `scheduleSave()`。
- **影响**：悬空引用。
- **建议**：在 action 表补 `flushSave()`（取消防抖、立刻上报全部 dirty + 空数组心跳两种模式），或将 5 处统一改为 `await scheduleSave({immediate:true})`。倾向前者。

### B11 `carryOver` 请求参数是死参数

- **位置**：[06-backend.md](06-backend.md) §6.9.1；[05-api.md](05-api.md) 用例 20；[07-frontend.md](07-frontend.md) §7.5.1。
- **现状**：06 把 `carry_in_sec` / `allow_carry_over` 降级为「仅作展示」，但 05 仍接受 `{carryOver}`，07 仍渲染勾选框。
- **影响**：用户能勾，勾了没用。
- **建议**：先删参数与 UI（YAGNI），有需求再加。

### B12 冗余计数降到 `DRAFT` 的路径未覆盖

- **位置**：[06-backend.md](06-backend.md) §6.12 步骤 ④。
- **现状**：`OFFLINE → DRAFT`、`APPROVED → DRAFT` 同样离开可见集，但 ④ 未覆盖。
- **影响**：与 B1 同源，计数不准。
- **建议**：与 B1 合并处理，覆盖所有「进入/离开 `PUBLISHED`」的迁移。

### B13 `IPT` 未定义 `total_duration`

- **位置**：[04-database.md](04-database.md) §4.8.2。
- **现状**：`code='IPT'` 的 `total_max_score=75`，但未给 `total_duration`。
- **影响**：一次 75 题、时长为空/默认 240 分钟的模拟考。
- **建议**：补齐 20 个科目的 `total_duration`，或明确「`total_duration` NOT NULL → 初始 SQL 必须全覆盖」，并一并检查其余子科。

### B14 `remark` 长度上限不一致

- **位置**：[07-frontend.md](07-frontend.md) §7.5.2；[04-database.md](04-database.md)。
- **现状**：07 表单限 200 字，04 `VARCHAR(500)`。
- **影响**：非冲突，但需在 05 写明是有意收紧，否则被当 bug 改掉。
- **建议**：统一到 500，或在 05 记录「服务端 500、前端 200」为有意设计。

### B15 08 的「每日一致性巡检」未登记进 06 §6.13

- **位置**：[08-ai-pipeline.md](08-ai-pipeline.md) §8.9.2 第 10 项；[06-backend.md](06-backend.md) §6.13。
- **现状**：08 引入每日巡检任务，但 06 §6.13 定时任务表只有 4 条。
- **影响**：该任务无实现落点。
- **建议**：并入 06 §6.13 现有表，或与「每周一 04:00 冗余计数校验」合并为一条。

---

## 4. 🟡 契约空缺（9 项，写代码前必须补进 05）

按 [07-frontend.md](07-frontend.md) §7.11 的强制要求（`types/api.d.ts` 必须与 05 严格一致、无自造字段），下列空缺会直接卡住前端。

### C1 5 个接口有路径、无字段表

- **位置**：`GET /admin/dashboard`(#71)、`GET|PUT /admin/configs`、`GET /admin/users`(#72)、`GET /admin/batches`(#65)、`GET|PUT /practice/progress`。
- **影响**：前端无法写类型，只能自造字段，违反 7.11 提交前检查项。
- **建议**：补齐字段表，优先 `dashboard`（首页）与 `progress`（考生端核心页）。

### C2 「首次登录强制改密」无任何落点

- **位置**：[06-backend.md](06-backend.md) §6.5；[09-deployment.md](09-deployment.md) §9.10；05 无字段/无错误码。
- **影响**：09 的上线检查项「该拦截生效」无法通过。
- **建议**：`UserVO` 加 `mustChangePassword: boolean`，改密成功后清零。倾向不加专用错误码，前端凭字段跳转即可。

### C3 `sys_config` 初始化键缺 3 个

- **位置**：[06-backend.md](06-backend.md) §6.8/§6.13/§6.5；[04-database.md](04-database.md) §4.8.3。
- **现状**：06 引用 `wrongbook.master.threshold`、`task.enabled.*`、`admin.must.change.password`，但 04 §4.8.3 只 seed 了 6 个键。
- **影响**：实现时读不到配置只能硬编码。
- **建议**：04 §4.8.3 补齐（含默认值与 remark），并加约定「06/08 新增配置键必须同步回 04 §4.8.3」。

### C4 `deploy/docker-compose.infra.yml` 被引用但无设计

- **位置**：[README.md](README.md) 快速开始；[10-roadmap.md](10-roadmap.md) P1 交付物。
- **影响**：P1「本地能跑起来」验收无定义。
- **建议**：在 09 补一节（如 §9.14）给出 infra compose 的端口映射；本地需映射 3306/6379，与生产**故意相反**，必须写明理由，避免被误认为违反 9.2.1。

### C5 `deploy/sql/outline-*.sql`：3 个 FULL 科目的作者未定

- **位置**：软件设计师 / 系统架构设计师 / 信息系统项目管理师的大纲树（各 200~300 题）需要 `outline-*.sql`。
- **影响**：这是版权边界上的内容工作（只能用官方章节标题原文，`origin='OFFICIAL_TITLE'`），谁产出、什么格式、如何核对均未写，直接影响 P2/P6 排期。
- **建议**（需裁决）：(a) 我按 GR-2 起草；(b) 你提供权威来源我结构化；(c) 先放占位、P6 再补。

### C6 游客 `SAMPLE` 模式下 `QuestionVO.isFavorited` 未定义

- **位置**：[05-api.md](05-api.md) `QuestionVO`。
- **影响**：游客无登录用户，`isFavorited` 返回 `false`/`null`/省略三选一未定，前端类型无法确定可空性。
- **建议**：`boolean`，游客恒 `false`（省掉前端三态分支）。

### C7 缓存键 `rk:limit:register:{ip}` / `rk:limit:upload:{userId}` 只在 09

- **位置**：[09-deployment.md](09-deployment.md) §9.13.6；[06-backend.md](06-backend.md) §6.11。
- **影响**：两键未在 06 §6.11 登记，违反 06 §6.15 自己的检查清单「新增缓存 Key 已在 6.11 登记」；阈值（注册 10/h、上传 60/h）在 05 中完全没有。
- **建议**：两键补进 06 §6.11；阈值写进 05 对应接口说明。

### C8 `GET /exams` / `PUT /practice/progress` 的 `data` 结构只给了一半

- **位置**：[05-api.md](05-api.md) 用例 17 与 `progress`。
- **影响**：用例 17 给了列表项字段但没给分页包装；`progress` 连字段表都没有，考生端主路径受影响。
- **建议**：与 C1 同源，优先补齐。

### C9 05 用例 55/56 示例请求缺 `generationBatchId`

- **位置**：[05-api.md](05-api.md) 规则 4 与示例 JSON。
- **影响**：规则 4 要求 `sourceType='AI_GENERATED'` 时提供 `generationBatchId`，但示例里该字段完全没出现，实现者不知道字段名与位置。
- **建议**：示例补 `"generationBatchId": null` 并保留规则 4。

---

## 5. 🔵 编辑级（8 项）

| # | 位置 | 问题 |
| --- | --- | --- |
| D1 | [03-architecture.md](03-architecture.md#L204) §3.7 | 「≤ 5000 条（3 科 × 250 + 17 科 × 40 + 增量）」括注算得 1430，与 5000 不符 |
| D2 | [06-backend.md](06-backend.md) §6.12 ③ | 写「`action` 取值与 05 文档一致」，但 action 枚举实际在 04 的 DDL 注释里，应改指向 04 |
| D3 | [07-frontend.md](07-frontend.md) 路由表 | `/practice/:mode` 标 `requiresAuth ✗`，但 `CHAPTER/KP/SEQUENCE/FAVORITE/WRONG_REDO` 都需登录，游客会拿 2005 toast 而非干净守卫重定向，应标「按 mode 判定」 |
| D4 | [09-deployment.md](09-deployment.md) §9.11.1 | 注释称「日备 7 天、周备（周日）4 周、上限 30 天」，但脚本无周备逻辑：`-mtime +7 -delete` 已删掉 7 天以上全部文件，`-mtime +30 -delete` 永远匹配不到，**实际保留 = 7 天**。§9.13.8 成本估算按 30 天算的 1.1GB 连带失真。建议真做周备，或把注释与成本改成 7 天口径 |
| D5 | [09-deployment.md](09-deployment.md#L680)、[877](09-deployment.md#L877)、[1179](09-deployment.md#L1179) | `chmod 755 data/uploads data/logs` 而 `server` 容器以 `rkapp`(uid 10001) 运行，755 下非属主不可写 → 图片上传与日志写入失败，且 §9.12 排查表把这个错误做法写成了修复方案。建议 `chown -R 10001:10001 data/uploads data/logs`（或 775 + 属组）。**文档里唯一一处「照做必然出事」的命令** |
| D6 | [05-api.md](05-api.md#L212) | 练习试卷接口末尾多一句「`SCORED` 以外的状态会话无权访问本接口」，该接口无会话概念，是从考试节复制的，建议删 |
| D7 | [03-architecture.md](03-architecture.md) §3.4 | 目录清单缺 `types/` 与 `BlankLayout.vue`，07 §7.2 声明的「与 03 完全一致」不成立 |
| D8 | [10-roadmap.md](10-roadmap.md) §10.12 | 回填责任表缺「前端结构变更 → 07」一行（D7 的成因） |

**判定为「不是问题」的两处（记录以免反复翻出）：**

- 05 定义的错误码 `1003` 在 07 §7.7 有 `code === 1003` 的判断，**不是缺陷**。
- 03 §3.5 环境变量清单无 `MYSQL_ROOT_PASSWORD`（只在 09 有），03 已显式声明「完整清单见 09」，**可接受**。

---

## 6. 跨文档一致的正面确认

以下逐条核过，没有问题，实现时可直接依赖：

- 20 个科目表（02 §2.1.2）与 04 §4.8.1 的 SQL **逐行一致**。
- 错误码分段（1xxx~5xxx）在 05 定义、06/07/08 引用，无越界、无重号。
- 组卷 4 级回退（06 §6.6）、难度 30/50/20、最大余数法配额 —— 06 与 10 的验收口径一致。
- 防重复窗口 `rk:repeat:*` 30 天 —— 02 / 04 / 06 / 10 四处一致。
- 交卷幂等（`UPDATE ... WHERE status IN (...)` 判影响行数）—— 06 §6.9 与 02 状态机、10 验收一致。
- 错题本 upsert 语义（`ON DUPLICATE KEY UPDATE`、连续答对 2 次移出）—— 02 / 04 / 06 / 07 一致。
- 主观题不判分（`is_correct = NULL`）—— ADR-06 / OOS-02 / 02 §2.4.3 / 06 §6.7 / 04 DDL 注释五处一致。
- GR-1 的「运行目录无真题语料 + CI `check-no-corpus-files`」—— 02 §2.6 / 08 §8.4.4 / 09 CI 配置一致。
- 部署硬约束（MySQL/Redis 不映射端口、`.env` 不进 Git、GHCR + `workflow_dispatch` 审批）—— 03 AC-1 / 09 多处 / 10 P8 一致。
- 07 的 `manualChunks`（admin 独立分包）与 03 ADR 的「管理端不进首屏」目标一致。

---

## 7. 环境现状（不阻塞文档评审，动工前逐步提醒）

1. **Docker 未安装**（无 WSL）→ 影响 P1 的本地 MySQL/Redis。Windows 上建议装 Docker Desktop（需 WSL2），或退一步用 Memurai/原生 Redis。
2. **本机 MySQL 8.0.23 在 3306 运行中，`root` 口令未知**（空口令 → `ERROR 1045`）。需提供口令，或另起容器实例避免冲突。
3. **Maven 3.9.8 在 `D:\apache-maven`（未上 PATH）、corepack 0.34.6 可用** → `corepack prepare pnpm@9` + 加 PATH 即可。

---

## 8. 收敛顺序建议

1. 先裁决第 2 节 6 项（尤其 **A5 高级计时模型**、**C5 大纲 SQL 作者**），其余 32 项均有明确建议，认可即可照建议改。
2. 按 [10-roadmap.md](10-roadmap.md) §10.12「文档与代码不一致时，先改文档再改代码」，逐份回改 01~10，每处标注来源。
3. 改完再进 P0。
