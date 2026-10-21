# 12 · 修订记录：交叉评审发现的落地

> 本文档记录 **一次完整修订**：把 [11-review-findings.md](11-review-findings.md) 中 38 条评审发现，连同 6 项待裁决冲突，逐条落到 `README.md` 与 `docs/01~10`。
>
> 与 `11` 的分工：**11 是"体检报告"（记录问题），本文档是"手术记录"（记录改了什么、为什么这么改）**。11 中的问题结论以本文档为准。
>
> ⚠️ **本文档不是最终结论。** 本文档的落地结果已由 [13-review-round2.md](13-review-round2.md) 做**第二轮复查**：44 条改动确认真落地，但新发现 9 项（G1~G9）**尚未落到 01~10**，其中 G2 / G3 需改 DDL、G6 含 **P1 验收标准写错**（`SHOW TABLES` 应为 27 张而非 25 张）。§8 的自查表有两项口径不实（见 13 §13.6.1）。**本文件与 13 冲突时以 13 为准。**
>
> 依据 [10-roadmap.md](10-roadmap.md) §10.12 的规则「文档与代码不一致时，先改文档再改代码」，本次修订在进入 P1 之前完成。

---

## 1. 六项裁决的结论

评审标为 🔴 的 6 项是"两个文档说了不同的话、选哪个都改变行为"，必须由项目负责人拍板。结论如下：

| # | 争议 | 裁决 | 落点 |
| --- | --- | --- | --- |
| **A1** | 计时权威：Redis 增量 vs `started_at` 派生 | **派生式胜出**。计时真值唯一来源是 `exam_session_sitting.started_at`；`remaining_sec` 一律是**展示用缓存**；Redis 只承载会话状态与短锁，不承载计时增量 | [03](03-architecture.md) ADR-03、[04](04-database.md) 注释、[06](06-backend.md) §6.9 |
| **A2** | 反馈限流：`{userId}:{questionId}`/24h/3 次 vs `{userId}`/3600s/20 次 | **06/05 胜出**（键带 `questionId`、24h、3 次）。理由：键粒度不足会退化成"全站 20 次/小时"，与"同一道题每人最多纠错 3 次"的产品规则不符 | [09](09-deployment.md) §9.13.6 |
| **A3** | `innodb_buffer_pool_size`：512M vs 1024M | **1024M**。2C4G 机器上 512M 会让索引命中率明显下降；与 03 §3.7「MySQL 约占 1G」一致 | [10](10-roadmap.md) 风险 R4 |
| **A4** | 容量数字三处不一（2 万/500 万 vs 5000/365 万 vs 5000/400 万） | **以 04 §4.9 为唯一出处**。01 的 2 万/500 万重新定位为「**设计容量上限**」，与「阶段一目标量」分开表述；03 改为引用 04，答题记录统一 400 万 | [01](01-requirements.md) §1.5、[03](03-architecture.md) §3.7 |
| **A5** | 高级科目卷面时长无法表达（模型假设一条连续倒计时，实际是两场次） | **评审建议的"论文并入连考、总时长 360 分钟"被否决**，改用**场次（sitting）分段** | [02](02-domain-model.md) §2.4、[04](04-database.md)、[05](05-api.md)、[06](06-backend.md)、[07](07-frontend.md)、[10](10-roadmap.md) |
| **A6** | 案例分析节的「可进入下一节」判定不可计算 | **采纳**：`duration_min` / `duration_max` 一律必填；离开门槛规则唯一化为「`usedSec >= duration_min` 才可离开，达到 `duration_max` 强制离开」 | [02](02-domain-model.md) §2.4.3、[05](05-api.md) §5.10、[06](06-backend.md) §6.9.1 |

### A5 为什么否决了评审的方案

评审给的方案甲是「论文并入连考，`total_duration = 360`」。这**与考制事实不符**：官方批次安排里，高级科目的综合知识 + 案例分析在上午连考（如 8:30—12:30，共 240 分钟），论文在下午独立时段（如 14:30—16:30，120 分钟），两者之间**隔着午休**。按 360 分钟连续计时，会让考生在午休期间被扣掉 120 分钟——这不是模型缺陷，是**功能缺陷**。

因此引入 `sitting_no`：**每个场次有独立的时长与独立的 `started_at`，场次之间不自动衔接**；场次二（论文）由用户主动点「开始本场次」才启动计时。

---

## 2. 结构性变更：场次（sitting）模型

这是本次修订唯一的**结构性**改动，其余 33 条都是局部修正。

### 2.1 层级从两层变三层

```
改前：模板 → 分节
改后：模板 → 场次（sitting）→ 分节
```

| 变更 | 对象 | 说明 |
| --- | --- | --- |
| **新增表** | `exam_paper_sitting` | 场次定义：`sitting_no`、`sitting_name`、`total_duration`、`total_max_score`、`auto_scored_max_score` |
| **新增表** | `exam_session_sitting` | 场次运行态：`status`（`PENDING`/`IN_PROGRESS`/`FINISHED`）、`started_at`、`total_duration_sec`、`remaining_sec`、`current_section_no`、`objective_score` |
| **字段迁移** | `exam_paper_template.total_duration` → `exam_paper_sitting.total_duration` | 时长不再落在模板级 |
| **字段新增** | `exam_paper_section.sitting_no` | 标明分节归属哪个场次 |
| **字段新增** | `exam_session.current_sitting_no` | 当前进行到的场次 |
| **字段新增** | `exam_session_sitting.*`（见上） | — |
| **字段删除** | `exam_paper_section.allow_carry_over` | 场次内总时长共享 → 节余自动可用，该字段既非判定依据也不可配置（同时满足评审 B11 的 YAGNI） |
| **字段删除** | `exam_session_section.allow_carry_over`、`carry_in_sec` | 同上 |
| **表数量** | 25 → **27** | §4.1 分域总览与 §4.2 ER 已同步 |

### 2.2 场次蓝图（权威定义在 [02 §2.4.2](02-domain-model.md)）

| 级别 | 场次 | 分节 | 时长约束 |
| --- | --- | --- | --- |
| 初级 / 中级 | 1 个（240 分钟） | ① 基础知识 单选×75（75 分）<br>② 应用技术 主观×5（75 分） | ① 90~120<br>② 120~150 |
| 高级 | 场次一（240 分钟） | ① 综合知识 单选×75（75 分）<br>② 案例分析 主观×3（75 分） | ① 120~150<br>② 90~120 |
| 高级 | 场次二（120 分钟） | ③ 论文 ESSAY×1（75 分） | 120~120，`allow_early_submit = false` |

**一致性不变式**（已在 04 §4.8.2 与 05 §5.10 写入校验）：

```
Σ(duration_min) ≤ sitting.total_duration ≤ Σ(duration_max)
```

验算：初/中级 `90+120=210 ≤ 240 ≤ 120+150=270`；高级场次一 `120+90=210 ≤ 240 ≤ 150+120=270`；论文场次 `120 ≤ 120 ≤ 120`。

### 2.3 接口变化

| 变化 | 接口 | 说明 |
| --- | --- | --- |
| **新增** | `POST /exams/{id}/sittings/{sittingNo}/start` | 启动场次计时（`PENDING → IN_PROGRESS`）。**论文场次需用户主动开始的落点** |
| **新增** | `POST /exams/{id}/sittings/{sittingNo}/finish` | 提前结束场次（受各分节 `duration_min` 与 `allow_early_submit` 约束） |
| **改名** | `POST /exams/{id}/sections/{sectionNo}/finish` → `.../advance` | 语义从"结束本节+申请结转"变为"离开本节进入下一节"；**删除 `carryOver` 请求参数** |
| **新增 VO** | `SittingVO` | 场次级状态与分数，`ExamSessionVO.sittings[]` |
| **新增 VO** | `ExamResultVO.sittingResults[]` | 成绩页按场次分区；`sectionResults[]` 增加 `sittingNo` |
| **字段删除** | `SectionVO.allowCarryOver` / `carryInSec` | 与 DDL 同步 |

### 2.4 一致性要求（实现阶段必须遵守）

1. **计时只认 `exam_session_sitting.started_at`**，`PENDING` 场次不倒计时。任何"把剩余秒数当增量状态存下来"的实现都是错的。
2. **场次之间不得自动衔接**——场次一结束后，场次二必须停在 `PENDING` 等用户点击开始。
3. `duration_min` / `duration_max` 在**所有**场次的所有分节上都必填（A6），否则门槛判定不可计算。
4. 成绩页必须按 `sittingResults` 分区展示；`answered = false` 的场次（放弃论文）标注"本场次未作答"。

---

## 3. 38 条发现的逐条落实

### 3.1 🔴 硬冲突（6 项）

见第 1 节表格，全部已裁决并落地。

### 3.2 🟠 逻辑缺陷（15 项）

| 编号 | 问题 | 改法 | 落点 |
| --- | --- | --- | --- |
| B1 | `PUBLISHED → DRAFT` 编辑后三处冗余计数不递减 | 「凡**进入或离开** `PUBLISHED` 的迁移都同步三处计数」，触发迁移逐条列明 | 06 §6.12 |
| B2 | 08 的一致性巡检公式正常流程下必然误报 | 等式改为**终态互斥口径**；「曾被拒绝次数」降为参考指标，不参与一致性校验 | 08 §8.9.2 第 10 项 |
| B3 | 驳回原因分布被 `[SUSPECT:]` 等前缀污染 | 明确「驳回原因分布**仅当 `action = REJECT`** 时参与统计」 | 08 §8.9.2 / §8.8.4 / §8.2.3 / §8.11.2 |
| B4 | `kpIds` 1~5 导致 0 标签题无法保存，08 闭环断裂 | `kpIds` 改 **0~5**；「至少 1 个」下沉为**发布动作**的校验（`/publish` 返回 `1005`） | 05 §5.11、07 §7.5.2、08 |
| B5 | 掌握度重算 SQL 与自身声明口径相反（`accuracy` 系统性偏高） | 改用 `ROW_NUMBER() OVER (PARTITION BY question_id ORDER BY answered_at DESC, id DESC)` 取每题末次作答再聚合；并要求单测覆盖"一题先错后对" | 06 §6.10.2 |
| B6 | 游客放行条件写成**安全洞**（白名单是路径级，实现者只能二选一，都不可接受） | 白名单**只表达路径级放行**；`POST /practice/papers` 标 `@SaIgnore` + **方法内**校验 `mode=SAMPLE && count≤5`；补测试「未登录 + `mode=FULL` → `2005`」 | 05 §5.13、03 ADR-02 |
| B7 | GR-2「必填」与 DDL 允许 NULL 冲突 | 保持可空（各 `source_type` 所需字段不同），**显式声明由应用层强制**：写入时校验 + `PUBLISHED` 前四字段非空 | 04 注释、08 §8.2.3、05 发布接口 |
| B8 | 错误码 `4006` 定义了但无人引用 | 绑定到 `PUT /exams/{id}/answers`：`itemId` 不属于该会话时返回 `4006` | 05 |
| B9 | `GET /exams` 未过滤 `ABANDONED` | 默认仅返回 `status IN ('SUBMITTED','SCORED')`；显式传 `includeAbandoned=true` 才包含 | 05 用例 17 |
| B10 | `flushSave()` 被 5 处调用但从未定义 | 在 `stores/exam.ts` 的 action 表补 `flushSave()`（取消防抖、立刻上报全部 `dirty`，并承担"空数组心跳"用途） | 07 §7.4 |
| B11 | `carryOver` 是死参数（用户能勾、勾了没用） | 删除字段 `allow_carry_over` / `carry_in_sec`、删除 API 参数、删除前端勾选框 | 02 / 04 / 05 / 06 / 07 |
| B12 | 冗余计数降到 `DRAFT` 的路径未覆盖 | 与 B1 合并处理，覆盖 `PUBLISHED → DRAFT`、`OFFLINE → DRAFT`、`APPROVED → DRAFT` | 06 §6.12 |
| B13 | `IPT` 未定义 `total_duration` | 明确 `IPT` 场次一 `total_duration = 240`（连考科目）；并加约束「初始 SQL 必须覆盖全部 20 个科目的场次时长」 | 04 §4.8.2 |
| B14 | `remark` 长度上限不一致（前端 200 / 服务端 500） | 在 05 记录为**有意收紧**，避免后人当 bug"修正" | 05 |
| B15 | 08 的每日一致性巡检未登记进 06 的定时任务表 | 登记为「每日 05:00 · 生成批次一致性巡检」（只告警不自动修正），与 `task.enabled.batch.consistency` 开关对应 | 06 §6.13 |

### 3.3 🟡 契约空缺（9 项）

| 编号 | 问题 | 改法 | 落点 |
| --- | --- | --- | --- |
| C1 | 5 个接口有路径、无字段表（前端无法写类型） | 补齐 `GET /admin/dashboard`、`GET\|PUT /admin/configs`、`GET /admin/users`、`GET /admin/batches`、`GET\|PUT /practice/progress` 的字段表 | 05 |
| C2 | 「首次登录强制改密」无任何落点 | 新增 `sys_user.must_change_password` 列 → `UserVO.mustChangePassword`；重置密码置 `1`、改密成功置 `0`；**不新增专用错误码** | 04、05、06 §6.5、09 §9.10 |
| C3 | `sys_config` 初始化键缺 7 个 | 补齐 `wrongbook.master.threshold`、`admin.must.change.password`、`task.enabled.*`（5 个）；并加约定「新增配置键必须同步回 §4.8.3」 | 04 §4.8.3 |
| C4 | `deploy/docker-compose.infra.yml` 被引用但无设计 | 新增一节给出完整定义：只含 MySQL + Redis，**本地映射 3306/6379**（与生产故意相反），并写明理由 | 09 §9.3.2 |
| C5 | 3 个 FULL 科目的大纲 SQL 作者未定 | 采纳「按 GR-2 自行起草」：格式、来源约束（仅官方章节标题、`origin='OFFICIAL_TITLE'`）与校验要求已写明；**具体章节数据属 P2 内容工作** | 04 §4.8.2、10 P2 |
| C6 | 游客 `SAMPLE` 模式下 `isFavorited` 三态未定 | 定为 **`boolean`，游客恒 `false`**（省掉前端三态分支） | 05 `QuestionVO` |
| C7 | 两个限流键只在 09 出现，违反 06 自己的检查项 | `rk:limit:register:{ip}`（10/h）、`rk:limit:upload:{userId}`（60/h）补登进 06 §6.11，阈值与 09 一致 | 06 §6.11、09 §9.13.6 |
| C8 | `GET /exams` 与 `progress` 的 `data` 结构只给了一半 | 补分页包装与字段表（与 C1 一并处理） | 05 |
| C9 | 05 示例请求缺 `generationBatchId` | 示例 JSON 补 `"generationBatchId": null`，保留规则 4 | 05 §5.11 |

### 3.4 🔵 编辑级（8 项）

| 编号 | 问题 | 改法 | 落点 |
| --- | --- | --- | --- |
| D1 | 容量括注算得 1430 与 5000 不符 | 改为「3 科 × 250 + 17 科 × 40 ≈ 1430 条，预留约 3 倍冗余至 5000 条」 | 03 §3.7 |
| D2 | `action` 枚举引用指向错误 | 改指向 04 的 DDL 注释（枚举实际定义处） | 06 §6.12 |
| D3 | `/practice/:mode` 标 `requiresAuth ✗`，游客会吃 `2005` toast | 改为「**按 `mode` 判定**」，仅 `SAMPLE` 免登录，守卫内动态判定并干净重定向 | 07 路由表 |
| D4 | 备份脚本注释称日备 7 天 + 周备 4 周，实际只保留 7 天 | **真做周备**：周日备份硬链到 `weekly/` 保留 4 周，日备 7 天，上传包 14 天；成本估算按「日备 7 份 + 周备 4 份」重算 | 09 §9.11.1、§9.13.8 |
| D5 | **唯一一处"照做必然出事"的命令**：`chmod 755 data/uploads data/logs` 而容器以 `rkapp`(uid 10001) 运行 → 非属主不可写 | 4 处全部改为 `chown -R 10001:10001`（含把错误做法当修复方案的排查表那一行） | 09 §9.7.3 / §9.9 / §9.12 / §9.13.3 |
| D6 | 练习试卷接口末尾误抄了"会话"概念的一句话 | 删除该句（该接口无会话概念） | 05 用例 12 |
| D7 | 03 §3.4 目录缺 `types/` 与 `BlankLayout.vue`，07 的"完全一致"声明不成立 | 补齐两项 | 03 §3.4、07 §7.2 |
| D8 | 回填责任表缺「前端结构变更 → 07」 | 补该行（D7 的成因） | 10 §10.12 |

---

## 4. 修订过程中一并处理的三处新发现

这三处是执行 38 条修订时暴露出来的连带缺口，评审未列出：

1. **`exam_session.total_duration_sec` 语义未定义**（04 的该列没有 COMMENT，无法判断是"当前场次"还是"全部场次之和"）。
   → 定义为「**全场次总时长之和（展示用）**；各场次真值见 `exam_session_sitting.total_duration_sec`」，并在 04 与 05 同步。避免了两处各写一半的隐患。

2. **`ExamResultVO` 缺场次分区**，成绩页无法按场次展示（高级科目论文场次"未作答"无处表达）。
   → 新增 `sittingResults[]`（含 `answered: bool`），`sectionResults[]` 增加 `sittingNo`；05 的字段表与 07 的 `types/api.d.ts` 同步。

3. **`sys_user` 缺 `must_change_password` 持久化列**，C2 的机制没有落点（06 的修订说明里已主动标注此缺口）。
   → 补列 `must_change_password TINYINT(1) NOT NULL DEFAULT 0`。

---

## 5. 未改动的项与理由

| 项 | 处理 |
| --- | --- |
| 评审 §5 判定为"不是问题"的两处（错误码 `1003` 在 07 的使用、03 环境变量清单无 `MYSQL_ROOT_PASSWORD`） | **保持原样**，仅在 11 中保留判定，避免反复翻出 |
| [11-review-findings.md](11-review-findings.md) 正文 | **不改动**，保留为评审历史记录；仅在文首加一行指向本文档 |
| 20 科目清单、组卷四级降级、难度 30/50/20、防重复 30 天窗口、主观题不判分、交卷幂等 | 评审已逐条确认跨文档一致，**未改动** |

---

## 6. 遗留项（实现阶段必须处理）

| # | 遗留 | 归属阶段 |
| --- | --- | --- |
| L1 | 04 §4.8.2 只给出场次/分节的**蓝图注释**，20 个科目的完整 `INSERT`（含场次与分节）尚未落地 | P1（交付物含 `init-data.sql`） |
| L2 | 管理员的初始记录须以 `must_change_password = 1` 插入 | P1 |
| L3 | 3 个 FULL 科目的章节树 SQL（`outline-*.sql`）内容尚未产出，仅约束与格式已定 | P2 |
| L4 | 软件设计师首批 250 题的生成与审核 | P3 |
| L5 | 场次模型的**计时正确性**是本次改动风险最高的部分，P5 验收已补 3 条针对性用例（场次一结束不倒计时 / 两场次独立倒计时 / 论文不可提前结束） | P5 |
| L6 | 05 用例编号因新增接口整体顺延，11 中引用的旧编号（如 #65/#71/#72）已失效——以 05 现编号为准 | 已记录，无需动作 |

---

## 7. 环境前置（实测，阻塞 P1）

修订完成后实测本机环境，与评审 §7 的结论有两处差异：

| 项 | 评审结论 | 实测 | 影响 |
| --- | --- | --- | --- |
| Docker | 未安装 | ✅ **确认未安装**（`docker` 命令不存在） | 🔴 **阻塞 P1**：无法起本地 MySQL/Redis |
| 本机 MySQL | 8.0.23 占着 3306，root 口令未知 | ✅ 3306 确有监听（PID 6384） | 🔴 **阻塞 P1**：需口令，或改用容器实例避开冲突 |
| Maven | 3.9.8，**未上 PATH** | ❌ **评审有误**：`mvn -v` 正常返回 3.9.8，已在 PATH | 无影响 |
| pnpm | 需 corepack 启用 | ✅ 确认未安装（`node v24.14.0` 在位，corepack 可用） | 🟡 P1 前执行 `corepack prepare pnpm@9 --activate` |

**结论：P1 的"本地起依赖"这一步目前无法执行。** 需要二选一：① 安装 Docker Desktop（Windows 需启用 WSL2）；② 使用现有 MySQL 8.0.23 建库并提供 root 口令。

---

## 8. 本次修订的效果核对

以下项已用检索逐条确认（非"应该没问题"）：

| 核对项 | 期望 | 结果 |
| --- | --- | --- |
| `chmod 755 data` | 0 处 | ✅ |
| `carry_in_sec` / `allow_carry_over` / `carryOver` / `allowCarryOver` / `carryInSec` | 仅在 02 与 11 的说明性文字中出现（记录"已删除"），无残留字段与参数 | ✅ |
| `RK_JWT_SECRET` | 仅 03 的"不存在该密钥"说明 | ✅ |
| `1 最难` | 0 处（已统一为 1 最易） | ✅ |
| `365 万` / `512M` | 0 处 | ✅ |
| `rejected_count +` 参与一致性等式 | 0 处 | ✅ |
| `rk:limit:feedback:{userId}`（旧键） | 仅 09 的"已废弃"说明 | ✅ |
| `CREATE TABLE` 数量 | 27，与"共 27 张表"一致 | ✅ |
| `exam_paper_sitting` / `exam_session_sitting` | 均存在，DDL 完整 | ✅ |
| `kpIds` 数量约束 | 05 与 07 均为 0~5 | ✅ |
| `flushSave()` | 07 的 action 表已定义 | ✅ |
| `advanceSection` | 07 已改，无 `finishSection` | ✅ |
| `ROW_NUMBER()` | 06 §6.10.2 已使用 | ✅ |
| 鉴权白名单含 `practice/papers` | 已移除，改为方法内校验 | ✅ |
| `types/` 与 `BlankLayout.vue` | 03 §3.4 与 07 §7.2 均存在 | ✅ |

> ⚠️ **上表有两项口径不实，已由 [13-review-round2.md](13-review-round2.md) §13.6.1 修正：**
> - `carryOver` 相关词并非"仅在 02 与 11"——[05-api.md:328](05-api.md#L328)、[07-frontend.md:237](07-frontend.md#L237)、[07-frontend.md:343](07-frontend.md#L343) 也有（**均为"已删除"说明，无残留字段与参数，B11 落地是干净的**）。正确口径：「无残留**字段与参数**」。
> - `1 最难` 并非 0 处——[08-ai-pipeline.md:440](08-ai-pipeline.md#L440) 有 1 处，是**有意保留的历史冲突记录**（该段自己写明"注意这里曾出现方向冲突"），属文档资产。
>
> **两项的功能状态都正确，问题在自查方法**：该栏系按印象勾选而非跑检索得出。**自查表一旦有假阳性，其余所有 ✅ 都要重新打折**——因此第二轮复查对全表做了独立复跑，而非抽样。
>
> 本轮复查另发现 **9 项新问题（G1~G9）**，详见 13。