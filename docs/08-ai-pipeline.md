# 08 · AI 造题与审核流水线

> 本文档定义题库的**生产方式**：AI 离线造题 → 人工审核 → 发布入库。
> 上游依据：[02-domain-model.md](02-domain-model.md#26-版权与数据来源策略)（版权策略，本文档的纲领）、[04-database.md](04-database.md#44-题库域)（字段唯一事实来源）、[05-api.md](05-api.md#511-管理端--题库与审核)（接口唯一契约）。
> 本文档只描述**离线工具链与审核闭环**，不描述任何在线 AI 能力（在线 AI 属于范围外，见 OOS-03）。

## 8.1 目标与边界

### 8.1.1 要解决的三个问题

| 问题 | 本文档的回答 | 落地章节 |
| --- | --- | --- |
| 题库从哪来 | 20 个软考科目、∞ 题量：由离线脚本按「大纲考点 + 题型结构」批量命制，不依赖任何外部题库 | 8.3 / 8.4 |
| 质量谁保证 | AI 只产出 `DRAFT`，必须经管理员在审核工作台裁决；**没有任何自动发布路径** | 8.6 / 8.8 |
| 版权怎么规避 | 输入侧禁止真题原文（GR-1）、全链路来源留痕（GR-2）、可投诉下架（GR-3） | 8.2 |

### 8.1.2 不做什么（显式边界）

| 不做 | 依据 | 原因 |
| --- | --- | --- |
| 在线 AI 答疑 / RAG 问答 / 智能推荐 | 01 · OOS-03 | 产品定位为练习工具，在线 AI 会引入运行期外部依赖与内容合规风险 |
| 主观题自动判分 / AI 批改 | 01 · OOS-02 | 判分准确性无法保证；`SUBJECTIVE`/`ESSAY` 一律"参考答案 + 用户自评" |
| **批量生成后自动发布** | 01 · M9 · UC-A02 | 见下方决策块 |
| 引用真题原文、真题 PDF、官方教材正文、开源题库的题目内容 | 01 · OOS-11、02 · 2.6.2 | 版权；仓库 LICENSE 无权覆盖其中的真题/教材内容 |
| 生成 `MULTI_CHOICE` / `TRUE_FALSE` 题型 | 02 · 2.2.1 | 官方客观题全为单选，生成非官方题型会污染题库可信度 |
| 用 AI 生成题干插图 | 02 · 2.2.2 | 离线脚本不产出图片；`images` 由人工在审核台后补 |

> 决策与理由
> **为什么不做"批量生成后自动发布"**：AI 输出的错误分布是"长尾 + 不可预测"的（答案错、难度错、知识点飘、偶发复述训练语料中的真题片段）。自动发布把审核成本从"管理员几十秒/题"转嫁为"用户发现错题 → 口碑损失 → 事后回滚 + 重新审核"，且一旦有题命中真题特征且已公开，GR-1 的技术隔离就失去了实际意义。**审核环节是合规护栏的执行点，不是可优化掉的流程步骤。**
>
> **为什么审核粒度默认是"单题"而不是"批次"**：批量的最大风险是"一键放行错误题目"，所以批量通过仅对"零存疑标记"的题目开放（见 8.8.3）。

### 8.1.3 与需求基线的映射

| 需求编号 | 落点 |
| --- | --- |
| 约束 C1（来源类型可追溯、不可为空） | 8.2.3 来源留痕 |
| 约束 C2（输入只能是考点与题型模板） | 8.2.2 输入隔离 |
| M8 题库管理 F-801~806 | 8.7 产出与导入、8.8 审核工作台 |
| M9 审核工作台 F-901~905 | 8.8 |
| UC-A03 触发与追踪生成批次 | 8.3 / 8.9 |
| UC-A08 反馈处理 | 8.2.4、8.8.5 |

## 8.2 合规护栏（本文档最重要的一节）

> 02 · 2.6 的三条护栏 GR-1/GR-2/GR-3 在此展开为**可执行规则**。任何绕过护栏的实现视为严重缺陷（02 · 2.6 原文）。

### 8.2.1 护栏总览与执行点

| 编号 | 护栏 | 执行点（代码/流程） | 违反后果 |
| --- | --- | --- | --- |
| GR-1 | 输入隔离 | 脚本 CLI 参数白名单 → 运行目录纯净性检查 → **调用 LLM 前的 Prompt 特征词断言**（三种拦截 + 单测 + CI 扫描） | 进程以非 0 退出码中止，不调用 LLM，批次置 `FAILED` |
| GR-2 | 来源留痕 | 导入时强制写入 `source_type` / `source_ref` / `source_license` / `generation_batch_id`；每次流转写 `rk_question_review_log` | 导入接口返回 `3004`（内容不完整）并拒绝该行 |
| GR-3 | 非商业 + 投诉下架 | 页脚声明 + `/feedback` 投诉入口；`COPYRIGHT` 反馈最高优先级 → 一键下架 | 反馈积压告警；下架动作写 `sys_operation_log` |

### 8.2.2 GR-1 输入隔离（可执行规则）

**规则 A · 脚本入参白名单**

生成入口只接受以下参数，不存在任何"追加资料文件"的入口：

```bash
python -m scripts.generate.main \
  --batch-id       41 \
  --outline-ids    102,103,104 \
  --question-type  SINGLE_CHOICE \
  --count          30 \
  --difficulty     3 \
  --prompt-version p1.2.0 \
  --out            data/batches/EX20260925000183.jsonl
```

- 参数解析用 `argparse.parse_args()`，**不使用 `parse_known_args()`**，未知参数直接报错退出（`SystemExit(2)`）。
- 脚本不接收 `--source`、`--corpus`、`--reference`、`--file`、`--url` 等任何指向外部文本内容的参数。
- 单元测试：`test_unknown_arg_rejected` 断言 `--source-file x.pdf` 会以非 0 码退出。

**规则 B · 运行目录纯净性**

- 脚本**不读取任何本地文档**。考点信息（`rk_outline_node.name` 短语、`rk_knowledge_point.name`）通过管理端只读接口获取，不落任何题文文件。
- `scripts/generate/` 目录下禁止存在 `.pdf` / `.docx` / `.doc` / `.txt` / `.csv` 数据文件；CI 增加一步 `check-no-corpus-files`：命中即构建失败。
- 生成脚本运行于**干净容器**（无历史资料挂载），容器内不配置任何真题资料盘。

> 决策与理由
> **为什么输入隔离靠"文件不在运行目录"，而不是靠"代码里不读文件"**：`if (path.endsWith(".pdf")) skip` 这类写法是**纪律约束**，它的失效方式有三种且都很难被发现——后续维护者加一行"顺手读取参考答案做 few-shot"、路径被参数化绕过、依赖库（如某些文档解析包）在初始化时静默读了目录。而"文件根本不存在 + 无网络抓取能力"是**能力最小化**：无论代码怎么写，读到真题原文在物理上都不可能发生。合规护栏必须建立在"不可能"而非"不应该"之上。

**规则 C · Prompt 特征词断言（调用 LLM 前）**

断言对象是**拼接完成的完整 Prompt 字符串**（system + user，即实际发给模型的字节），而非某个中间变量。

| 检测通道 | 策略 | 命中动作 |
| --- | --- | --- |
| ① 词表子串匹配 | 对黑名单词（见 8.4.4）做全角/半角归一 + 大小写归一后的子串扫描 | 中止 |
| ② 结构正则 | `(20\d{2})\s*年[上下]半年`、`第\s*\d+\s*(题|问)`、`(上午|下午)试题`、`试卷分值`、`考试时间\s*\d+\s*分钟` | 中止 |
| ③ 长段落异常 | Prompt 中除模板固定文案外，若出现 **连续中文 ≥ 80 字且不含占位符** 的段落 → 说明变量里混入了正文 | 中止 |

```python
# scripts/generate/guard.py
class SourceLeakSuspect(Exception):
    """GR-1 断言失败：Prompt 中疑似出现真题/教材内容。"""

BLACKLIST = ["历年真题", "真题", "原题", "考试试题", "参考答案与解析",
             "官方教材", "本题选自", "摘自《", "试卷原文", "往届试题"]

STRUCT_PATTERNS = [
    r"20\d{2}\s*年[上下]半年",
    r"第\s*\d+\s*(题|问)",
    r"(上午|下午)试题",
    r"分值\s*[:：]?\s*\d+\s*分",
]

def assert_prompt_clean(prompt: str) -> None:
    """发送给 LLM 前必须调用。命中任一通道即抛异常，调用方不得吞掉。"""
    norm = normalize(prompt)                 # 全角→半角、去零宽字符、统一大小写
    for w in BLACKLIST:
        if w in norm:
            raise SourceLeakSuspect(f"命中黑名单词：{w}")
    for p in STRUCT_PATTERNS:
        if re.search(p, norm):
            raise SourceLeakSuspect(f"命中结构特征：{p}")
    for para in split_paragraphs(prompt):
        if len(para) >= 80 and "{" not in para:
            raise SourceLeakSuspect("出现超长非模板段落，疑似正文混入")
```

- **必须有单元测试覆盖**，至少 3 个断言用例：`test_blocklist_word_aborts`（注入"历年真题"→ 抛异常）、`test_struct_pattern_aborts`（注入"2023 年上半年试题"→ 抛异常）、`test_clean_prompt_passes`（正常模板 → 不抛异常）。该测试计入 M9 的验收清单（见 8.12）。
- 断言失败时：**不调用 LLM**，批次置 `FAILED` 并写入 `fail_reason`，退出码非 0，日志打一级告警（`level=ERROR, event=SOURCE_LEAK_SUSPECT`）。

### 8.2.3 GR-2 来源留痕（字段填写规则）

导入时逐行强制校验，任一字段不合规该行拒绝入库（返回 `3004`）。

| 字段（04 列名） | JSON（05/导入） | 填写规则 | 为空时 |
| --- | --- | --- | --- |
| `source_type` | `sourceType` | AI 造题固定 `AI_GENERATED`；人工原创 `ORIGINAL`；已获授权 `LICENSED` | 拒绝（C1 强制） |
| `source_ref` | `sourceRef` | 记录**生成输入范围**，格式 `批次号 + 考点 ID 列表`，如 `EX20260925000183;outline=102,103,104`；人工题填编写人 | 拒绝 |
| `source_license` | `sourceLicense` | `AI_GENERATED` / `ORIGINAL` 固定 `SELF`；`LICENSED` 填许可标识 | 拒绝 |
| `generation_batch_id` | `generationBatchId` | AI 生成题必须指向真实存在的 `rk_generation_batch.id` | 拒绝 |
| `created_by` | —（服务端） | 服务端取导入操作人（`operator_id` / 管理员 id） | 服务端填充 |
| `status` | —（服务端） | 导入后固定 `DRAFT`，**不接受客户端指定** | 服务端填充 |

**"必填"由谁强制（对应评审 B7）**：上表四字段（`source_type` / `source_ref` / `source_license` / `generation_batch_id`）的"非空"约束**在应用层强制、DDL 不设防**，分两处：

1. **写入时（新增 / 导入）校验**：任一字段为空即拒绝该行，返回 `3004`（上表"为空时"列）。
2. **发布前四字段必须非空**（即 `PUBLISHED` 之前）：发布动作除状态守卫（须为 `APPROVED`）外，还校验 `source_type` / `source_ref` / `source_license` / `generation_batch_id` 四个字段非空，任一为空即拒绝（与 05 发布接口的校验对应）。这样即使有历史数据在写入校验生效前落库，也不可能进入组卷池（组卷只取 `status='PUBLISHED'`）。

> 决策与理由
> **为什么 DDL 里这三列可为 NULL（04 文档）**：`source_type` 各取值所需的字段本就不同——人工原创题（`ORIGINAL`）不需要指向生成批次的 `generation_batch_id`，`LICENSED` 的 `source_ref` 语义也与 `AI_GENERATED` 不同。把它们写成 `NOT NULL` 只会强迫所有来源类型填一个无意义的值。**这是有意的留白，不是 DDL 写漏了约束**；非空要求由上述两处应用层校验承担。

**流转留痕**：每一次状态变化写 `rk_question_review_log`：

| 列 | 填写 |
| --- | --- |
| `from_status` / `to_status` | 流转前后状态（创建时为 `NULL` → `DRAFT`） |
| `action` | `CREATE` / `SUBMIT` / `APPROVE` / `REJECT` / `REGENERATE` / `PUBLISH` / `OFFLINE` / `EDIT` |
| `remark` | 驳回原因前缀 + 说明（见 8.8.4）；存疑标记 `[SUSPECT:...]`；待建标签 `[KP_PENDING:...]`。**注意：只有驳回原因前缀在 `action=REJECT` 的记录上才参与 8.9.2 第 3 项的统计，`[SUSPECT:...]` / `[KP_PENDING:...]` 不参与该统计**（对应评审 B3） |
| `operator_id` | 操作人 `sys_user.id`（脚本导入为管理员 id） |

### 8.2.4 GR-3 非商业与投诉下架

- 全站不出现付费、会员、广告入口（01 · OOS-05）；页脚固定声明数据来源与版权立场（README · 6）。
- `/feedback` 入口的数据落在 `rk_question_feedback`，`type` 取值 `TYPO` / `WRONG_ANSWER` / `UNCLEAR_ANALYSIS` / `COPYRIGHT` / `OTHER`。
- **`COPYRIGHT` 反馈的处理优先级**：

| 项 | 规则 |
| --- | --- |
| 队列排序 | `COPYRIGHT` 永远排在最前（`ORDER BY FIELD(type,'COPYRIGHT',...) , created_at`），列表页标红并置顶 |
| 响应时限 | 目标：24 小时内处理（运营纪律，非代码强制） |
| 处理动作 | 必选"下架题目"：`POST /api/v1/admin/questions/{id}/offline`，即 `PUBLISHED → OFFLINE` |
| 处理记录 | `POST /api/v1/admin/feedback/{id}/handle`，请求体 `{status, handleRemark, offlineQuestion}`；`offlineQuestion=true` 时服务端在**同一事务**内执行下架并写 `sys_operation_log`（module=`QUESTION`, action=`OFFLINE`） |
| 下架后 | 该题不再进入任何新组卷；历史 `exam_session_item` 快照保留（见 8.11.2） |

### 8.2.5 常见误区与反例

| 误区 | 为什么错误 |
| --- | --- |
| "把真题改写一下（换数字、换主语、调语序）就不算抄" | 著作权中的**改编权**独立于复制权。非独创性改写属于演绎作品，仍需原权利人许可；改写后入库等于把侵权内容包装成"本站练习题"，风险与直接复制同级。02 · 2.6.2 明确把"派生"列入禁止项。 |
| "GitHub/Gitee 上的开源题库标了 MIT，就能直接用题目" | MIT 只覆盖**仓库作者拥有版权的那部分内容**（代码、结构、排版工具）。真题与官方教材内容的权利人是官方考试机构，仓库作者无权转授。用其题目内容等于从一个无权利人处取得许可，许可无效。允许的只有"使用其代码/结构"（02 · 2.6.2）。 |
| "模型是离线跑的，模型不可能记住真题" | 无法证明。模型对高频公开语料的记忆是概率性的、不可审计的，因此不能作为免责依据。护栏的设计前提正是"**假定模型可能复述真题**"，所以要在输入侧隔离（GR-1）与输出侧扫描（8.6.3）双向设防。 |
| "只要不写'真题'两个字就没问题" | 文案措辞只影响"是否宣称"，不影响"是否复制"。禁用"真题"字样是为了避免误导性宣称，不能替代内容层面的独立命制要求。 |
| "相似度阈值调低一点可以少丢几道题" | 阈值越低，越容易放过"实质相同"的题。库内高度相似题会让用户产生"这题我刚做过"的疲劳感，且会放大单点错误（同一错误在多题重复出现）。阈值取 0.9 与 05 · 55/56 的 `3005` 判据一致，属于产品与合规共同要求的红线。 |

## 8.3 流水线总体流程

```
 ┌─ 离线侧（人工触发，运行期不参与请求链路）─────────────────────────────────────┐
 │                                                                             │
 │  [管理端] 创建批次                                                           │
 │  POST /api/v1/admin/batches  {subjectId, outlineIds[], questionType,        │
 │        targetCount, difficulty, model, promptVersion}                        │
 │      → rk_generation_batch.status = PENDING                                  │
 │                 │  ✗失败：参数校验 1001 / 考点不属于该科目 3001  可重试：是    │
 │                 ▼                                                            │
 │  [离线脚本] 领取批次                                                          │
 │  GET  /api/v1/admin/batches?status=PENDING                                   │
 │  PUT  /api/v1/admin/batches/{id}/status {status:"GENERATING"}                │
 │                 │  ✗失败：已被其他执行者领取（状态非 PENDING）3003  可重试：否 │
 │                 ▼                                                            │
 │  组装 Prompt（仅 outline.name + 知识点清单 + question_type + difficulty）      │
 │                 │  ✗失败：无                                            　      │
 │                 ▼                                                            │
 │  ★ 断言 A：assert_prompt_clean(prompt)   ← GR-1 唯一强制性拦截点              │
 │  命中 → 中止；PUT /status {status:"FAILED", failReason:"SOURCE_LEAK_SUSPECT"} │
 │                 │  ✗失败：疑似涉真题  可重试：**否**（须人工排查输入源）       │
 │                 ▼                                                            │
 │  调用 LLM（model，温度 0.7，单题最多 3 次重试）                                │
 │                 │  ✗失败：网络/429/超时 → 指数退避重试；超限舍弃该题            │
 │                 ▼                                                            │
 │  逐题 Schema 校验（JSON 可解析 + 必填 + 类型 + 选项 A~D）                      │
 │                 │  ✗失败：解析失败 → 重试该题 1 次；仍失败则丢弃并计数          │
 │                 ▼                                                            │
 │  逐题质量校验（内容层 + 业务层，见 8.6）                                       │
 │                 │  ✗失败：丢弃 / 标记存疑 / 进待建标签队列                     │
 │                 ▼                                                            │
 │  产出 JSONL：data/batches/EX20260925000183.jsonl                             │
 │                 │  ★ 断言 B：落盘前对每行文案二次黑名单扫描（同 8.4.4）         │
 │                 ▼                                                            │
 │  [管理端] 导入                                                                │
 │  POST /api/v1/admin/questions/import  (text/plain, JSONL, ≤500 行)          │
 │      → 全部 status = DRAFT；回写 produced_count                              │
 │      PUT /api/v1/admin/batches/{id}/status {status:"GENERATED", producedCount}│
 │                 │  ✗失败：行级校验 3004 / 重复 3005 → errors[{line,reason}]    │
 └─────────────────┬───────────────────────────────────────────────────────────┘
                   ▼
 ┌─ 审核侧（在线，管理端）───────────────────────────────────────────────────────┐
 │  审核工作台：GET /api/v1/admin/questions?batchId=41&status=DRAFT             │
 │      → PUT /status {status:"REVIEWING"}（首次裁决时）                         │
 │                   │                                                         │
 │        ┌──────────┼───────────────┬──────────────┐                          │
 │        ▼          ▼               ▼              ▼                          │
 │     APPROVE     REJECT        REGENERATE      EDIT+APPROVE                  │
 │     POST .../{id}/review      POST .../{id}/regenerate   PUT .../{id}       │
 │        │          │               │              │                          │
 │        ▼          ▼               ▼              ▼                          │
 │    APPROVED    REJECTED        DRAFT         APPROVED                       │
 │        │                                                                    │
 │        ▼  POST /api/v1/admin/questions/{id}/publish                          │
 │    PUBLISHED  ← 唯一可被组卷的状态（rk_question.status）                      │
 │        │  全部裁决完毕 → rk_generation_batch.status = COMPLETED               │
 │        ▼                                                                    │
 │  [异常] POST .../{id}/offline  PUBLISHED → OFFLINE（含 COPYRIGHT 投诉下架）    │
 └─────────────────────────────────────────────────────────────────────────────┘
```

**每步失败处理与可重试性**

| 步骤 | 幂等 | 失败处理 | 可重试性 |
| --- | --- | --- | --- |
| 创建批次 | 否 | 返回 `1001`/`3001`，不落库 | 修正参数后重试 |
| 领取批次 | 是（状态守卫） | 状态非 `PENDING` 返回 `3003` | 不重试（换一个批次） |
| 组装 Prompt | 是（纯函数） | — | — |
| 断言 A | 是 | 中止进程，批次 `FAILED` | **不自动重试**，必须人工排查 |
| 调用 LLM | 否 | 网络/`429`/超时 → 指数退避，单题最多 3 次 | 可重试（同一题） |
| Schema 校验 | 是 | 重试该题 1 次；仍失败丢弃并计入 `failed` | 有限重试 |
| 质量校验 | 是 | 丢弃 / 存疑 / 待建标签（见 8.6.4） | 内容类失败不重试（同一 Prompt 会复现） |
| 落盘 JSONL | 是（覆盖批次文件） | 断言 B 命中 → 该行剔除并记日志 | 是 |
| 导入 | 是（见 8.7.3） | 行级错误进 `errors[]`，不阻断其他行 | 是（修正后可重传） |
| 批次状态回写 | 是（绝对值） | `PUT /status` 用**绝对值**而非增量，重复调用结果一致 | 是 |
| 审核裁决 | 是（状态守卫） | 状态非 `PENDING_REVIEW` 返回 `3003` | 不重试 |
| 发布 | 是 | 状态非 `APPROVED` 返回 `3003` | 不重试 |

## 8.4 Prompt 契约

### 8.4.1 变量与版本

| 变量 | 来源 | 说明 |
| --- | --- | --- |
| `{prompt_version}` | 批次 `rk_generation_batch.prompt_version` | 如 `p1.2.0`；模板文件 `scripts/generate/prompts/p1.2.0.md`，改模板必须升版本 |
| `{subject_name}` / `{level}` | `rk_subject.name` / `level` | 如 `软件设计师` / `MIDDLE` |
| `{outline_path}` | `rk_outline_node`（自根到叶拼接 `name`） | 如 `计算机系统基础知识 / 操作系统 / 进程管理`。**只取章节标题短语**，不含大纲正文 |
| `{outline_id}` | `rk_outline_node.id` | 写入产出行的 `outlineId` |
| `{kp_hint}` | `rk_knowledge_point.name`（该科目已有标签，逗号分隔，可空） | 供模型优先复用已有标签 |
| `{question_type}` | 批次 `question_type` | 生成阶段固定 `SINGLE_CHOICE` |
| `{count}` | 批次 `target_count` | 本轮要求产出的题数 |
| `{difficulty}` | 批次 `difficulty` | 1~5；为空则允许 1~5 自行分布 |

### 8.4.2 System Prompt 模板

```text
你是计算机技术与软件专业技术资格（软考）命题组的独立命题专家。你的任务是为考试准备练习题。

【最重要的约束 · 必须严格遵守】
1. 你只能依据下方给出的「考试科目」「大纲考点路径」与「题型要求」，从零独立命制题目。
2. 严禁引用、复述、改写、影射任何历年试题、官方试卷、官方教材与辅导书的内容。你不得在题干、选项、解析中提及任何年份、考次、"真题""原题""试题"等字样。
3. 不得在输出中出现"本题选自""摘自某书""参考答案与解析"之类的字样。
4. 你的输出必须是严格合法的 JSON，不要输出 Markdown 代码围栏，不要输出任何解释性前言或后记。

【命题要求】
- 每题必须只有一个明确正确的答案，其余 3 个选项必须是"合理的干扰项"：来源于同一知识点的常见误解（如概念混淆、公式代错、量纲错误、参数顺序颠倒），不得使用明显荒谬、与题干无关或纯凑数的选项。
- 4 个选项的文本长度应大致相当（最长项不超过最短项的 3 倍），避免正确答案因表述最长/最专业而被"猜"出来。
- 解析必须给出可复核的推理过程：写出依据的定义/公式、代入过程与结论，而非仅说"故选 X"。解析长度不少于 30 个汉字。
- 题干必须自包含（不依赖"如上图""见教材第 X 页"之类的外部引用），且不以"第 X 题"开头。

【文本格式要求】
- 题干、选项、解析均为 Markdown 文本。
- 数学公式一律用 KaTeX 语法：行内公式用 $...$（如 $O(n\log n)$、$\log_2 1024 = 10$），独立成行的块级公式用 $$...$$。
- 公式中的反斜杠在 JSON 字符串中必须转义为两个反斜杠（例如 KaTeX 的 \times 在 JSON 中写作 \\times）。
- 不要输出 HTML 标签，不要输出图片（不许出现 ![]() 语法）。

【难度语义】（difficulty，整数 1~5，1 最易、5 最难）
- 1：单一概念的识记题，读过考点即可作答。
- 2：单一步骤的直接应用（代一次公式、判一次定义）。
- 3：需要两步推理或对两个概念做对比区分。
- 4：多步骤计算或跨小节综合，需排除 2 个以上强干扰项。
- 5：跨章节综合或需要较深理解，通常是失分率高的题。

【输出结构】严格输出如下 JSON（不要额外字段，不要省略字段）：
{output_schema}
```

### 8.4.3 User Prompt 模板

```text
【考试科目】{subject_name}（{level}）
【大纲考点路径】{outline_path}
【考点 ID】{outline_id}
【本考点已有知识点标签（可优先复用，也可新增）】{kp_hint}
【题型要求】{question_type}（单选题，恰好 4 个选项，key 依次为 A、B、C、D）
【难度要求】{difficulty}
【本次需产出题数】{count}

请就上述考点独立命制 {count} 道 {question_type} 题。
要求：
- 每题 coverage 到该考点，不要命制与本考点无关的题；
- 同一批题之间不得考查同一个细节点（避免同质化）；
- 每题标注 1~5 个知识点标签（knowledgePoints[].name），优先使用上面给出的已有标签原文；
- 严格按 System 中给定的 JSON 结构输出一个 JSON 数组，数组长度必须等于 {count}。
只输出 JSON 数组本身。
```

> 决策与理由
> **为什么把「黑名单」同时写进 Prompt 与代码校验**：写进 Prompt 是"降低发生概率"，代码校验是"发生了一定拦得住"。模型对否定式指令（"不要提及 X"）的遵循率并不高，尤其在需要引用语境时反而更容易提到 X（ironic process）。因此 Prompt 中的禁令只是第一层，真正的保障是 8.6.3 的输出侧黑名单扫描。
>
> **为什么难度用 5 级而非 3 级**：组卷需要按难度配比（如 3:4:3），3 级的分辨率不足以做出接近真实试卷的难度曲线；5 级与 04 · `rk_question.difficulty TINYINT` 的容量一致，且模型对 5 级语义的把握尚可（对 10 级则明显失真）。

### 8.4.4 禁止出现的表述黑名单（唯一来源）

此表同时被三处引用：GR-1 输入断言（8.2.2）、生成侧 Prompt 禁令（8.4.2）、输出侧内容层校验（8.6.3）。

| 类别 | 词/片段 | 命中处置 |
| --- | --- | --- |
| 考试溯源类 | `历年真题`、`真题`、`原题`、`考试试题`、`往届试题`、`试卷原文` | 丢弃该题（输出侧）/ 中止（输入侧） |
| 考次年份类 | `20XX 年上半年`、`20XX 年下半年`、`上半年试题`、`下午试题`、`上午试题`（正则 `20\d{2}\s*年[上下]半年`） | 同上 |
| 教材出处类 | `官方教材`、`本题选自`、`摘自《`、`参考教材第` | 同上 |
| 结构复刻类 | `参考答案与解析`（作为引用标题出现）、`分值：`、`第 X 题`（正则 `第\s*\d+\s*(题|问)`） | 同上 |
| 答案口吻类 | `答案选 X 因为`、`显然选`、`正确答案是第` （正则 `答案选\s*[A-D]\s*因为`） | 同上 |
| 免责/仿冒类 | `侵权必究`、`版权所有`（模型若回抄官方教材页脚，说明它在复述教材） | 丢弃 + 告警 |

> 注意：黑名单禁止的是**溯源与复刻特征**，不是"解析"这类正常词汇本身。所以判定用精确短语/结构正则，而不是宽泛的"包含'解析'就拦"——过宽的拦截会让正常题目被判为涉真题，反而促使人工逐步关掉护栏。

## 8.5 输出 Schema

<a id="84-输出-schema"></a>

JSONL 单行即一条题目，字段与 `rk_question` / `rk_question_option` / `rk_question_kp` 一一对应（JSON 用 camelCase，入库转 snake_case）。

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://ruankao-online.local/schema/ai-question-line.json",
  "title": "AI 造题 JSONL 单行（/admin/questions/import 契约）",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "subjectId", "outlineId", "questionType", "stem", "stemFormat",
    "options", "correctAnswer", "analysis", "difficulty",
    "knowledgePoints", "sourceType", "sourceLicense", "sourceRef",
    "generationBatchId", "clientKey"
  ],
  "properties": {
    "subjectId":   { "type": "string", "pattern": "^[1-9][0-9]{0,18}$" },
    "outlineId":   { "type": "string", "pattern": "^[1-9][0-9]{0,18}$" },
    "questionType":{ "const": "SINGLE_CHOICE" },
    "stem":        { "type": "string", "minLength": 10, "maxLength": 600 },
    "stemFormat":  { "const": "MARKDOWN" },
    "images": {
      "type": "array",
      "maxItems": 0,
      "items": {
        "type": "object", "additionalProperties": false,
        "required": ["url", "alt"],
        "properties": { "url": { "type": "string" }, "alt": { "type": "string" } }
      }
    },
    "options": {
      "type": "array",
      "minItems": 4, "maxItems": 4,
      "items": {
        "type": "object", "additionalProperties": false,
        "required": ["key", "content"],
        "properties": {
          "key":     { "enum": ["A", "B", "C", "D"] },
          "content": { "type": "string", "minLength": 1, "maxLength": 160 }
        }
      }
    },
    "correctAnswer": { "type": "string", "pattern": "^[A-D]$" },
    "analysis":      { "type": "string", "minLength": 30, "maxLength": 2000 },
    "difficulty":    { "type": "integer", "minimum": 1, "maximum": 5 },
    "knowledgePoints": {
      "type": "array",
      "minItems": 0, "maxItems": 5,
      "items": {
        "type": "object", "additionalProperties": false,
        "required": ["name"],
        "properties": { "name": { "type": "string", "minLength": 2, "maxLength": 64 } }
      }
    },
    "sourceType":        { "const": "AI_GENERATED" },
    "sourceLicense":     { "const": "SELF" },
    "sourceRef":         { "type": "string", "minLength": 1, "maxLength": 255 },
    "generationBatchId": { "type": "string", "pattern": "^[1-9][0-9]{0,18}$" },
    "clientKey":         { "type": "string", "pattern": "^EX[0-9]{14}#[0-9]{4}$" }
  }
}
```

**字段校验规则与入库映射**

| JSON 字段 | 入库落点 | 校验规则 |
| --- | --- | --- |
| `subjectId` | `rk_question.subject_id` | 必填、十进制字符串；须存在且 `rk_subject.status='ENABLED'`，否则 `3001` |
| `outlineId` | `rk_question.outline_id` | 必填；必须是 `level=3`（或 leaf）且属于 `subjectId` 的节点，否则 `3001` |
| `questionType` | `rk_question.question_type` | 生成阶段只允许 `SINGLE_CHOICE`（02 · 2.2.1） |
| `stem` | `rk_question.stem` | 长度 10~600（按去 Markdown 标记后的可见字符计） |
| `stemFormat` | `rk_question.stem_format` | 常量 `MARKDOWN` |
| `images` | `rk_question.images` | AI 阶段必须为空数组或缺省（`maxItems: 0`）；图片由人工在审核台后补 |
| `options[].key` | `rk_question_option.option_key` | 恰好 4 项，取值 A~D，**互不重复**（同 key 重复由代码校验，JSON Schema 无法表达） |
| `options[].content` | `rk_question_option.content` | 1~160 字符；`sort_no` 按 A/B/C/D 顺序取 0/1/2/3 |
| `correctAnswer` | `rk_question.correct_answer` | 单字符且必须 ∈ `options[].key`；单选不允许多字符（多选才按字典序拼接） |
| `analysis` | `rk_question.analysis` | ≥30 字符 |
| `difficulty` | `rk_question.difficulty` | 整数 1~5（1 最易，见下方决策块） |
| `knowledgePoints[].name` | 经 `rk_knowledge_point` 解析后写 `rk_question_kp` | **0~5 项**；入库时按 `uk_subject_name(subject_id, name)` 匹配，含 `aliases` 模糊匹配；匹配不上进"待建标签"队列。**导入允许 0 个挂载**（只挂已匹配标签），**"至少 1 个"是发布动作的校验**（见 8.8.2 发布行），与 05 的 `kpIds` 0~5 口径一致（对应评审 B4） |
| `sourceType` | `rk_question.source_type` | 常量 `AI_GENERATED`（人工题走 8.5.1 的普通新增接口） |
| `sourceLicense` | `rk_question.source_license` | 常量 `SELF` |
| `sourceRef` | `rk_question.source_ref` | 必填；格式 `批次号;outline=<id列表>` |
| `generationBatchId` | `rk_question.generation_batch_id` | 必填且批次须存在；同一请求内所有行必须同一批次 |
| `clientKey` | 不入库（幂等用） | 格式 `批次号#行序号`；服务端据此实现行级幂等（见 8.7.3） |

> 决策与理由
> **难度方向：`difficulty` 1 = 最易、5 = 最难。** 该方向以 02 · 2.2.2 的字段定义为唯一来源。**注意这里曾出现方向冲突**：04 的 DDL 注释最初写作 `'1~5，1 最难'`，与 02 相反；现已把 04 的注释与 06 的组卷难度分档同步为同一语义。之所以要在此处显式记录，是因为组卷难度配比、统计口径与 Prompt 难度语义**全部依赖这个方向**，一旦实现时反向，会造成"难题被当作易题"且**不报错**的静默错误——这类错误在验收时很难被发现。
>
> **为什么产出行里也带 `subjectId`/`outlineId`（批次已知道这些）**：批次记录的是"本次覆盖的多个考点"，单题归属到哪一个 leaf 只有产出行自己知道。把归属写在行上，导入时就不需要按序号猜测，也让 JSONL 文件可以脱离批次上下文单独校验（验收标准中"3 行非法数据定位到行号"依赖这一点）。

## 8.6 校验规则（三层）

校验在**两个位置执行同一套规则**：离线脚本落盘前（早发现、省导入往返）、导入接口服务端（权威校验，脚本不可信）。层与层是**短路**关系：Schema 层不过则不进入内容层。

### 8.6.1 Schema 层

| 检查 | 判据 | 不通过处置 |
| --- | --- | --- |
| JSON 可解析 | 每行 `json.loads` 成功；失败行不影响其他行 | 丢弃该行，计入 `failed`，`errors[].reason="JSON 解析失败"` |
| 必填字段 | `required` 全部存在且非 null | 丢弃，返回 `3004` |
| 类型/常量 | `questionType`、`stemFormat`、`sourceType`、`sourceLicense` 为常量值 | 丢弃，返回 `3004` |
| `options` 结构 | 恰好 4 项，`key ∈ {A,B,C,D}` 且**唯一** | 丢弃，返回 `3004` |
| `correctAnswer` ∈ 选项 | `correctAnswer` 必须出现在 `options[].key` 中 | 丢弃，返回 `3004` |
| ID 合法性 | `subjectId` / `outlineId` / `generationBatchId` 存在且归属正确 | 丢弃，返回 `3001` |

### 8.6.2 内容层

| 检查 | 判据（阈值） | 处置 |
| --- | --- | --- |
| 题干长度 | 去 Markdown 标记后可见字符 ∈ [10, 600]；< 20 字且 `difficulty ≥ 4` → 存疑 | 超界丢弃；存疑标记 |
| 公式成对 | `stem` / `analysis` / `options[].content` 中 `$` 出现次数为偶数；`$$` 出现次数为偶数 | 不成对丢弃（渲染必然错乱） |
| 选项长度分布 | `maxLen ≤ 3 × minLen + 20`；4 项内容两两不相同 | 超出丢弃；有 2 项完全相同丢弃 |
| 干扰项合理性 | 不得出现 ≥2 项与题干完全无关（启发式：选项与题干关键词无共现）；不得全部为"以上都对/都不对"类 | 命中丢弃 |
| 解析长度下限 | `len(analysis) ≥ 30` 且需包含至少一个推理连接词（`因为/由于/根据/推导/代入/故/因此/计算`） | 不足则存疑 |
| 难度-长度相关性 | `difficulty ≤ 2` 但题干含块级公式 `$$` 或长度 > 200 → 存疑；`difficulty = 5` 但题干 < 20 字 → 存疑 | 存疑标记 |
| 黑名单扫描 | 命中 8.4.4 任一词/正则 | **丢弃**（不得仅存疑） |

### 8.6.3 业务层

| 检查 | 判据 | 处置 |
| --- | --- | --- |
| 库内相似度 | 题干归一化（去空白/标点、转小写、KaTeX 命令降级为纯文本）后，与**同科目 + 同章节**、`status ∈ {DRAFT, PENDING_REVIEW, APPROVED, PUBLISHED}` 的既有题做相似度比对；**阈值 0.9**（与 05 · `3005` 一致）；完全相同也命中 | 命中丢弃，计入 `duplicates`（人工可在 55/56 用 `force=true` 覆盖，导入路径不提供覆盖） |
| 知识点匹配 | `knowledgePoints[].name` 按 `uk_subject_name(subject_id, name)` 精确匹配，再按 `rk_knowledge_point.aliases` 模糊匹配 | 匹配上的写 `rk_question_kp`；匹配不上的进**"待建标签"队列**，同时在 `rk_question_review_log`（`action=CREATE`）的 `remark` 写入 `[KP_PENDING:标签名]` 供审核台提示 |
| 黑名单二次 | 同 8.6.2 | 丢弃 |

**"待建标签"队列的落地**：不新增表。未匹配标签的题目**照常导入为 `DRAFT`**（只挂已匹配标签，**允许 0 个暂挂**），未匹配名字写入该题创建时的 `review_log.remark`。审核台按 `[KP_PENDING:` 过滤出这批题，管理员在知识点维护页建好标签后再挂载（走 `PUT /api/v1/admin/questions/{id}` 编辑）。`review_log` 的 `remark` 是 VARCHAR(500)，够放 5 个标签名。

**知识点数量的两级口径（对应评审 B4）**：**保存（新增 / 导入 / `PUT` 编辑）允许 0~5 个**，与 05 校验规则 3 改后的 `kpIds` 0~5 一致；**"至少 1 个"下沉为发布动作的校验**——`PUBLISHED` 前该题必须已挂 ≥1 个知识点（见 8.8.2 发布行）。这样"标签匹配不上 → 0 个暂挂"的题在管理端能正常打开并保存，管理员补挂标签后即可发布，闭环不断裂。

> 决策与理由
> **为什么相似度阈值取 0.9，而不是 0.8 或 0.95**：0.95 太高，无法拦住"换数字但结构完全一致"的题（这类题归一化后相似度常在 0.92~0.95），会放过实质重复；0.8 太低，会把"考同一考点但不同细节点"的正常题判为重复，导致大批合规题目被丢弃、审核台被"重复"告警淹没。0.9 是"能拦住改写型重复、又不误伤同考点异题型"的平衡点，且与 05 已定的 `3005` 判据统一，避免两套阈值导致前端提示不一致。
>
> **为什么业务层失败一律丢弃而非存疑**：相似度与黑名单命中的题，若仍以 `DRAFT` 入库，会在审核台长期占用人工注意力，且"存疑"的语义会被稀释。存疑标记只留给**内容写法可疑但仍可能正确**的情况（长度、难度相关性），涉重复/涉真题属于硬失败。

### 8.6.4 处置矩阵（汇总）

| 处置 | 适用 | 实现 |
| --- | --- | --- |
| **丢弃** | JSON 不可解析、必填缺失、选项/答案结构错、黑名单命中、相似度 ≥0.9、公式不成对 | 不写入 JSONL / 计入 `errors[]`；脚本侧计入 `failed`；批次 `produced_count` 不含它 |
| **标记存疑交人工** | 题干过短却标难题、解析缺推理词、难度与长度不相关、选项长度分布异常但未超界 | 正常导入为 `DRAFT`，写 `review_log.remark` 前缀 `[SUSPECT:原因]`；审核台高亮并**禁止参与批量通过** |
| **自动重试一次** | 仅限**非内容类**失败：网络/`429`/超时、LLM 返回非法 JSON | 同 Prompt 重试 1 次（网络类走退避最多 3 次）；内容类失败不重试 |

## 8.7 产出与导入

### 8.7.1 JSONL 行格式（真实样例）

每行一个 JSON 对象，UTF-8，行尾 `\n`，**不换行**。以下是一行完整样例（`\\times`、`\\log_2` 为 JSON 中对 KaTeX 反斜杠的转义）：

```json
{"subjectId":"6","outlineId":"102","questionType":"SINGLE_CHOICE","stem":"在某分页存储管理系统中，页面大小为 $4\\text{KB}$，页表项占 $4$ 字节。若采用一级页表，则逻辑地址中的页内偏移量应为（　）位。","stemFormat":"MARKDOWN","options":[{"key":"A","content":"$4$"},{"key":"B","content":"$10$"},{"key":"C","content":"$12$"},{"key":"D","content":"$32$"}],"correctAnswer":"C","analysis":"页面大小为 $4\\text{KB}=2^{12}$ 字节。逻辑地址由「页号 + 页内偏移」两部分组成，页内偏移用于定位页内的字节，其位数由页面大小决定：$\\log_2 2^{12}=12$，因此页内偏移量应为 $12$ 位，故选 C。页表项占 $4$ 字节只影响页表本身的空间占用，与页内偏移位数无关。","difficulty":2,"knowledgePoints":[{"name":"分页存储管理"},{"name":"逻辑地址结构"}],"sourceType":"AI_GENERATED","sourceLicense":"SELF","sourceRef":"EX20260925000183;outline=102","generationBatchId":"41","clientKey":"EX20260925000183#0007"}
```

### 8.7.2 导入接口调用

- 路径：`POST /api/v1/admin/questions/import`
- `Content-Type: text/plain; charset=utf-8`（**不是** `application/json`，请求体本身是 JSONL 文本）
- 单次 ≤ 500 行（超额返回 `1001`）
- 需 `role = ADMIN`（`/api/v1/admin/**` 全部要求，且写 `sys_operation_log`）

```bash
curl -X POST 'https://<host>/api/v1/admin/questions/import' \
  -H 'Authorization: Bearer <ADMIN_TOKEN>' \
  -H 'Content-Type: text/plain; charset=utf-8' \
  --data-binary '@data/batches/EX20260925000183.jsonl'
```

成功响应（部分行失败时 `code` 仍为 0，逐行错误在 `data.errors`）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "imported": 97,
    "skipped": 0,
    "errors": [
      { "line": 12, "reason": "options 数量必须为 4" },
      { "line": 45, "reason": "correctAnswer 不在 options.key 中" },
      { "line": 88, "reason": "命中黑名单词：历年真题" },
      { "line": 90, "reason": "与既有题目高度相似（≥0.9）" }
    ]
  },
  "traceId": "8f2c1a9b4d7e4f10"
}
```

- `line` 为**请求体内的行号**（从 1 开始），便于直接定位素材文件中的那一行。
- 导入成功后全行 `status = DRAFT`；管理端页面或脚本随后调用 `PUT /api/v1/admin/batches/{id}/status`，请求体 `{status:"GENERATED", producedCount:97}` 回写批次。

### 8.7.3 重复导入的幂等策略

| 层级 | 规则 | 效果 |
| --- | --- | --- |
| 批次级 | 服务端取请求内所有行的 `generationBatchId`，必须唯一；随后校验该批次 `status`：`GENERATING` 才接受导入；若已是 `GENERATED` 或 `COMPLETED` → 返回 `1005`，`errors[0].reason="该批次已导入，如需追加请新建批次"` | 拦住"整个文件重传一次" |
| 行级 | 用 `clientKey = 批次号#行序号` + `normalized(stem)` 双重判重：同 `generation_batch_id` 且 `outline_id` 相同、归一化题干相同 → 计入 `skipped`，不重复入库 | 拦住脚本重跑 / 断点续传产生重复行 |
| 跨批次 | 仍受 `3005` 相似度检测约束（同科目同章节 ≥0.9 即拒），`skipped` 与 `errors` 均可能命中 | 拦住"换批次重造同一题" |
| 状态回写 | `PUT /api/v1/admin/batches/{id}/status` 只接受**绝对值**（`producedCount` 是总数），不接受增量 | 重复回写不会把计数累加成 2 倍 |

> 决策与理由
> **为什么幂等键不用新增 `client_key` 列**：04 未定义该列，且本题库的"重复"本质是**内容重复**而非"文件行重复"——即使换批次、换行号重传同一道题，也应被拦（跨批次那一条）。既然内容判重（归一化题干）是必须做的，就没有必要为了 idempotency 再加一列：`clientKey` 只在导入请求内用于**定位行号与生成可读错误**，不落库。
>
> **为什么导入路径不提供 `force=true`**：`force` 的存在意义是"人工确认过这题虽相似但确实要留"，那是编辑场景（05 · 55/56）的判断；批量导入是机器批量灌入，任何绕过相似度检测的开关都等于把 0.9 阈值变成可选项。

## 8.8 审核工作台

### 8.8.1 界面要素

三栏布局（管理端 `web/src/views/admin/`，桌面优先，最小宽度 1280px）：

| 区域 | 要素 |
| --- | --- |
| 左：待审队列 | 按批次/科目分组；筛选项 `subjectId`/`outlineId`/`batchId`/`status`/`sourceType`；显示 `[SUSPECT]`、`[KP_PENDING]` 徽标；顶部计数 |
| 中：题目预览 | 题干（**KaTeX 渲染** + 图片按 `rk_question.images` 渲染）、4 个选项（标出正确答案）、解析（渲染公式与插图）、当前难度与知识点标签 |
| 右：来源与操作 | 来源信息（`sourceType` / `sourceRef` / `batch_no` / `prompt_version` / `model`）、章节归属、难度选择器、知识点标签编辑（可合并同义）、相似度告警、`rk_question_review_log` 时间线、操作按钮组 |

### 8.8.2 操作与状态流转表

| 动作 | 接口 | `action`（review_log） | from → to | 备注 |
| --- | --- | --- | --- | --- |
| 创建（导入/人工新增） | `POST /api/v1/admin/questions`、`/import` | `CREATE` | `NULL` → `DRAFT` | 导入为批量创建 |
| 提交审核 | `POST /api/v1/admin/questions/{id}/submit-review` | `SUBMIT` | `DRAFT` → `PENDING_REVIEW` | 状态非 `DRAFT` 返回 `3003` |
| 通过 | `POST /api/v1/admin/questions/{id}/review` `{action:"APPROVE"}` | `APPROVE` | `PENDING_REVIEW` → `APPROVED` | 写 `reviewed_by`、`reviewed_at`；批次 `approved_count +1` |
| 驳回 | `POST .../{id}/review` `{action:"REJECT", remark}` | `REJECT` | `PENDING_REVIEW` → `REJECTED` | 批次 `rejected_count +1` |
| 退回重生成 | `POST /api/v1/admin/questions/{id}/regenerate` | `REGENERATE` | `REJECTED` → `DRAFT` | 见 8.8.5 |
| 编辑 | `PUT /api/v1/admin/questions/{id}` | `EDIT` | 状态不变 | `version +1` |
| 编辑后通过 | `PUT` + `POST .../review` | `EDIT` 然后 `APPROVE` | `PENDING_REVIEW` → `APPROVED` | 两条日志，顺序固定 |
| 批量通过 | `POST /api/v1/admin/questions/batch-review` `{ids[], action, remark}` | 每题一条 `APPROVE` | 逐题 `PENDING_REVIEW` → `APPROVED` | 上限 100；**跳过带 `[SUSPECT]` 的题**并返回跳过名单 |
| 发布 | `POST /api/v1/admin/questions/{id}/publish` | `PUBLISH` | `APPROVED` → `PUBLISHED` | 写 `published_at`；同步 `rk_subject.question_count`、`rk_outline_node.question_count`、`rk_knowledge_point.question_count`。**发布前校验（除状态守卫外）：已挂知识点 ≥1 个；`source_type`/`source_ref`/`source_license`/`generation_batch_id` 四字段非空**，任一不满足即拒绝（对应评审 B4、B7，与 05 发布接口校验一致） |
| 下架 | `POST /api/v1/admin/questions/{id}/offline` | `OFFLINE` | `PUBLISHED` → `OFFLINE` | 同步递减上述计数；写 `sys_operation_log` |

> 注意：`rk_question_review_log.action` 的枚举只有 `SUBMIT/APPROVE/REJECT/REGENERATE/PUBLISH/OFFLINE/EDIT/CREATE`（04 · DDL 注释）。因此"驳回原因"与"存疑标记"**不新增 action 值**，而是编码进 `remark` 前缀（见 8.8.4），避免与 04 的枚举冲突。

### 8.8.3 键盘快捷键建议

| 键 | 动作 | 说明 |
| --- | --- | --- |
| `J` / `K` | 下一条 / 上一条 | 焦点在题目列表，不改变状态 |
| `A` | 通过 | 若题带 `[SUSPECT]`，弹出确认框并要求填写 `remark` |
| `R` | 驳回 | 弹出原因下拉（见 8.8.4）+ 备注（备注必填） |
| `E` | 编辑 | 打开编辑抽屉（`PUT` 后 `version +1`） |
| `G` | 退回重生成 | 仅 `REJECTED` 可用 |
| `P` | 发布 | 仅 `APPROVED` 可用 |
| `Enter` | 确认当前对话框 | — |
| `Esc` | 关闭对话框 / 取消 | — |

快捷键必须是**显式操作**：不提供"全选全部未存疑题并一键通过"之外的任何隐式批量行为（避免误操作跨批次放行）。

### 8.8.4 驳回原因分类（编码进 `remark` 前缀）

| 原因分类 | `remark` 前缀 | 典型场景 |
| --- | --- | --- |
| 内容错误 | `[CONTENT_ERROR]` | 题干事实错误、公式错误、选项与题干矛盾 |
| 答案错误 | `[ANSWER_ERROR]` | `correct_answer` 与解析推导结论不符 |
| 难度不符 | `[DIFFICULTY_MISMATCH]` | 标注 2 实为 4（与 8.6.2 的存疑信号相互印证） |
| 表述不清 | `[UNCLEAR]` | 题干歧义、选项语义重叠、"最合适"无唯一解 |
| 重复 | `[DUP]` | 与库内既有题实质重复（人工判定，可能相似度 <0.9） |

统一格式：`[ANSWER_ERROR] 解析推出 A，标注为 B，已回退重生成`。统计时按 `remark` 的 `[...]` 前缀分组（见 8.9.2 第 3 项）。

**这些前缀只在 `action = REJECT` 的记录上参与统计**（对应评审 B3）：同一 `remark` 字段还承载 `[SUSPECT:...]` / `[KP_PENDING:...]`（8.2.3）与 `[COPYRIGHT]`（8.11.2），它们**不是驳回原因，不参与本分布**。

### 8.8.5 "退回重生成"如何回写批次计数

`POST /api/v1/admin/questions/{id}/regenerate` 的语义是"**这道题不要了，让脚本换一题重造**"，因此：

1. **状态**：`REJECTED → DRAFT`，并写 `review_log`（`action=REGENERATE`，`remark` 记录原因）。
2. **计数**：`rejected_count` 在 `REJECT` 时已经 `+1`，`REGENERATE` **不再改动** `approved_count` / `rejected_count`（否则同一道题会被计两次，通过率失真）。
3. **补造**：管理员在批次详情页把该批次 `status` 由 `REVIEWING` 改回 `GENERATING`，并把 `target_count` 设为"原目标 + 需补数量"，通过 `PUT /api/v1/admin/batches/{id}/status` 提交（绝对值，幂等）。脚本下一次领取该批次时，按 `target_count - produced_count` 计算需补题数。
4. **待删的旧题**：回退后的 `DRAFT` 题由管理员决定"编辑后复用"或"删除"（`DELETE /api/v1/admin/questions/{id}`，仅 `DRAFT`/`REJECTED` 可删）。

## 8.9 批次与统计

### 8.9.1 `rk_generation_batch` 字段写入时机

| 字段 | 写入时机 | 写入方 | 说明 |
| --- | --- | --- | --- |
| `batch_no` | 创建时 | 服务端 | `EX` + `yyyyMMdd` + 6 位随机，如 `EX20260925000183`（`uk_batch_no`） |
| `subject_id` / `outline_ids` / `question_type` / `target_count` / `difficulty` / `model` / `prompt_version` / `operator_id` | 创建时（`PENDING`） | 服务端 | `outline_ids` 是"输入不含真题"的证据链（04 · 注释原文） |
| `status` | 全生命周期 | 脚本 + 服务端 | `PENDING`(创建) → `GENERATING`(脚本领取) → `GENERATED`(导入完成) → `REVIEWING`(首次裁决) → `COMPLETED`(全部裁决)；异常 → `FAILED` |
| `produced_count` | 导入成功时 | 服务端 | 成功的行数（不含 `errors` 与 `skipped`）；绝对值回写 |
| `approved_count` | 每次 `APPROVE` | 服务端（事务内） | 含 `batch-review` |
| `rejected_count` | 每次 `REJECT` | 服务端（事务内） | `REGENERATE` 不重复计数 |
| `fail_reason` | `FAILED` 时 | 脚本（`PUT /status`） | 如 `SOURCE_LEAK_SUSPECT`、`LLM_TIMEOUT`、`RATE_LIMITED` |
| `finished_at` | `COMPLETED` 或 `FAILED` 时 | 服务端/脚本 | 唯一性：只在首次进入终态时写 |
| `created_at` | 创建时 | DB 默认值 | — |

### 8.9.2 质量指标统计口径（10 条细节）

1. **通过率** = `approved_count / produced_count`。分母是"已入库为 `DRAFT` 的题数"，**不是** `target_count`（后者混入了"生成阶段就丢弃的题"，会把模型错误算成审核结果）。`produced_count = 0` 时该指标为 `NULL`，不记 0（避免"通过率 0%"的错误结论）。
2. **生成达成率** = `produced_count / target_count`，与通过率**分开统计**。它衡量的是脚本/模型产出能力，与人工裁决质量无关；两率相乘才是"从目标到可用题"的总效率。
3. **驳回原因分布**：**仅当 `action = REJECT` 的记录参与统计**，再按 `rk_question_review_log.remark` 的 `[...]` 前缀分组计数（8.8.4 的 5 类）。一题可被多次驳回时，**以题为单位只保留最后一次驳回原因**，否则同一道反复驳回的题会放大某一类别的占比。（对应评审 B3：`remark` 同一字段还承载 `[SUSPECT:...]`（8.6.4）、`[KP_PENDING:...]`（8.6.3）、`[COPYRIGHT]`（8.11.2）三类杂项，若不限定 `action`，分布会混入它们。）
4. **首次通过率** = 从未产生过 `REJECT` 日志的题数 / `produced_count`。这是最接近"模型质量"的指标；`APPROVE` 总数会被"驳回-修改-再通过"的迭代次数放大。
5. **平均审核时长** = 从该题 `CREATE` 日志到 `APPROVE` 日志的时间差，取**中位数**而非均值（存在"放着不管几周"的长尾，均值无意义）。
6. **重复丢弃率** = 生成阶段 `duplicates` 丢弃数 /（产出数 + 丢弃数）。它反映"模型在给定考点上产同质题"的程度，是该考点是否需要拆分更细粒度的信号。
7. **存疑率** = 带 `[SUSPECT:]` 标记的题数 / `produced_count`。存疑率突升通常意味着 Prompt 版本变更后质量回退，可作为回滚 `prompt_version` 的触发条件。
8. **知识点未匹配率** = 带 `[KP_PENDING:]` 标记的题数 / `produced_count`。该值持续偏高说明 `rk_knowledge_point` 标签体系覆盖不足，应优先补标签而非继续加题。
9. **统计的时间归属**：以 `rk_generation_batch.created_at` 归入统计日（跨天执行的批次不拆分），避免同一批次的数据在两天里各算一半。
10. **一致性巡检**：每日任务按 `generation_batch_id` 分组，以**当前状态**做互斥统计，校验下方等式；不等式即说明计数被重复回写或有题目脱离批次，需人工介入。该巡检只告警不自动修正。

    **一致性等式（终态互斥口径，对应评审 B2）**：

    ```sql
    -- 按 generation_batch_id 分组：命中任一 HAVING 条件即为告警行
    SELECT b.id,
           b.produced_count,
           COUNT(q.id)                      AS actual_total,   -- 该批次当前全部产出题
           SUM(q.status = 'DRAFT')          AS draft_cnt,
           SUM(q.status = 'PENDING_REVIEW') AS pending_cnt,
           SUM(q.status = 'APPROVED')       AS approved_cnt,
           SUM(q.status = 'PUBLISHED')      AS published_cnt,
           SUM(q.status = 'OFFLINE')        AS offline_cnt,
           SUM(q.status = 'REJECTED')       AS rejected_cnt
    FROM rk_generation_batch b
    LEFT JOIN rk_question q ON q.generation_batch_id = b.id
    GROUP BY b.id, b.produced_count
    HAVING actual_total <> b.produced_count
        OR draft_cnt + pending_cnt + approved_cnt + published_cnt + offline_cnt + rejected_cnt
           <> b.produced_count;
    ```

    **语义说明**：
    - 等式只认**当前状态**：`count(该批次全部产出题) == produced_count`，且**各状态计数之和也等于 produced_count**。同一时刻一题只可能处于一种状态，因此左边是**互斥**的，不会重复计数。
    - `approved_count` / `rejected_count` 是批次上的**累计写入值**（每次 `APPROVE` / `REJECT` 各 `+1`），而 §8.8.5 明文写出的 `REJECTED → REGENERATE → DRAFT → … → APPROVED` 是**正常流程**——这类题会**同时**落进 `rejected_count` 与 `approved_count`，两者相加必然大于题数。因此 `rejected_count` **降级为参考指标**（"曾被拒绝次数"，用于分析返工量），`approved_count` 同理，**二者都不参与一致性等式**，否则巡检每天报假警。
    - `produced_count` 作为"本批次应有多少题"的基线，独立于状态，仍是等式右端。

## 8.10 模型与成本

### 8.10.1 模型可配置性

| 项 | 规则 |
| --- | --- |
| `model` | `rk_generation_batch.model VARCHAR(64)`，**由创建批次时指定**，如 `deepseek-chat` / `qwen-plus`。代码中不硬编码默认模型 |
| 调用形式 | 统一走 OpenAI 兼容的 `/chat/completions` 协议，供应商切换只改 `base_url` + `api_key` + `model` 三个配置项 |
| 推荐 | **国产对话模型 API**（国产模型对中文软考术语与 KaTeX 输出的稳定性更好，且成本可控、无需跨境链路） |
| 不在运行期使用 | 模型调用只发生在离线脚本中；后端服务不持有任何模型密钥（01 · 假设 A4） |
| `prompt_version` | 与模板文件一一对应（`scripts/generate/prompts/{prompt_version}.md`），便于"这一批题是用哪版 Prompt 造的"可复现 |

### 8.10.2 成本估算方式（公式，不含报价）

```text
# 单个批次
input_tokens  = target_count × (system_prompt_tokens + user_prompt_tokens)
                 其中 system_prompt_tokens ≈ 600~900（模板固定，可命中 prompt caching）
                      user_prompt_tokens   ≈ 100~200（考点路径 + 要求）
output_tokens = target_count × output_tokens_per_question
                 其中 output_tokens_per_question ≈ 300~600（中文 token ≈ 1.5 字/token）
                                                  题干+4 选项+解析，含 KaTeX 转义

批次成本 ≈ (input_tokens  ÷ 1e6) × price_in_per_mtok
        + (output_tokens ÷ 1e6) × price_out_per_mtok

# 重试放大系数：网络类重试率 r 时，实际成本 ≈ 批次成本 × (1 + r)，按 r ≈ 0.05~0.10 预留
```

- 估算时**按最坏情况取上界**（`output_tokens_per_question = 600`、`system_prompt_tokens = 900`）来做预算审批；实际按 token 用量统计回填。
- 单题成本与"难度"弱相关（难题解析更长），与 `target_count` 强线性相关，因此预算公式以批次的 `target_count` 为主变量。
- 成本优化**只能**通过"缩短 System Prompt / 命中 prompt caching / 降低无效重试"实现；**不允许**通过"把真题塞进 Prompt 做 few-shot"来提升质量（违反 GR-1）。

### 8.10.3 失败重试与限流（离线脚本）

| 场景 | 策略 |
| --- | --- |
| 并发 | 默认 2~4 并发（受供应商单账号 QPS/RPM 限制）；可通过 `--concurrency` 调整，上限 4（单机 + 限流保护） |
| 网络错误/`5xx` | 指数退避：`base=2s`，`delay = min(2^attempt × 2s, 60s)`，最多 3 次，加 ±20% 抖动避免同时重试 |
| `429` | 优先读 `Retry-After` 头；无该头则退避 30s 起，最多 5 次；连续 2 次 `429` 自动把并发降到 1 |
| 超时 | 单请求 60s；超时按可重试错误处理 |
| 非法 JSON 输出 | 与"内容错"区分对待：重试该题 1 次（附加强指令"只输出 JSON 数组"），仍失败则丢弃 |
| **`SOURCE_LEAK_SUSPECT`** | **不重试、不降级、直接中止整个批次**，批次置 `FAILED`，必须人工排查输入源（可能是运营误把资料挂进了生成容器） |
| 断点续跑 | 成功行即刻追加写入 JSONL；重跑时按 `clientKey`（`批次号#行序号`）跳过已成功行，只补未完成的题 |
| 限速保护 | 脚本内置令牌桶（默认 3 QPS）；命中 QPS 上限时排队而非丢弃 |

## 8.11 运营动作

### 8.11.1 扩充题量的标准流程（唯一允许路径）

```
1. 选考点   管理端确定"哪个科目的哪些叶子章节题量不足"
            （依据：rk_outline_node.question_count 与卷面模板所需题量对比）
2. 建批次   POST /api/v1/admin/batches
            {subjectId, outlineIds[], questionType:"SINGLE_CHOICE",
             targetCount, difficulty, model, promptVersion}
            → status = PENDING
3. 生成     离线脚本领取 → 组装 Prompt → 断言 A → 调用 LLM
            → Schema/内容/业务三层校验 → JSONL
            → POST /api/v1/admin/questions/import → status = GENERATED
4. 审核     审核工作台逐题裁决（通过 / 驳回 / 退回重生成）
            → APPROVED 的题执行 publish → PUBLISHED
            → 全部裁决后 status = COMPLETED（或改回 GENERATING 补造）
5. 发布     发布后自动更新 rk_subject / rk_outline_node / rk_knowledge_point
            的 question_count；题目进入组卷池
```

- 单批次建议 `targetCount ≤ 50`：批次越小，审核可更快闭环、Prompt 版本迭代反馈越快、单次 `SOURCE_LEAK_SUSPECT` 的损失越小。
- 每个批次必须记录 `prompt_version`，形成"Prompt 版本 → 质量指标"的对照表（8.9.2 的第 7 条）。

### 8.11.2 题目被下架后的数据影响

| 对象 | 影响 |
| --- | --- |
| `rk_question.status` | `PUBLISHED → OFFLINE`；不再进入任何**新**组卷（组卷条件 `status='PUBLISHED'`） |
| `rk_subject.question_count` / `rk_outline_node.question_count` / `rk_knowledge_point.question_count` | 同步递减（05 · 63 原文） |
| `user_answer_record` | **不受影响**（历史答题流水保留，统计可复现） |
| `user_wrong_question` / `user_favorite` | 保留；列表查询按 `rk_question.status` 过滤，不再展示已下架题 |
| `exam_session_item` 快照 | **不受影响**：快照已固化了 `stem_snapshot` / `options_snapshot` / `correct_answer`，历史成绩与试卷可完整复现（04 · 注释、02 · 2.4.3） |
| `rk_question_review_log` | 追加一条 `action=OFFLINE` 记录，`remark` 记录下架原因（如 `[COPYRIGHT] 用户投诉`）。**`[COPYRIGHT]` 不是驳回原因，不参与 8.9.2 第 3 项的驳回原因分布统计**（对应评审 B3） |
| 批次统计 | 不留痕：下架发生在发布之后，不计入 `rejected_count`（否则通过率会被历史下架污染） |

> 决策与理由
> **为什么下架不做物理删除**：物理删除会破坏 `user_answer_record` / `exam_session_item` 的引用完整性，且一旦误删，被投诉题目的"曾经存在过"这一事实也一并消失——这在版权争议中是**反向不利**的证据状态。保留 `OFFLINE` 状态并留下带原因的 `review_log`，反而构成"发现即处理"的合规证据链。
>
> **为什么下架要同步递减 `question_count`**：该字段是组卷配额的输入。若不递减，组卷引擎会以为该章节还有题，导致"抽不够题"或"抽到已下架题"。递减是唯一能让冗余统计与真实可组卷量一致的做法。

## 8.12 验收标准

以下清单为 M8/M9 的验收项，每一项都必须是**可执行、可判定**的：

- [ ] **GR-1 输入白名单**：`scripts/generate` 入口对未知参数（如 `--source-file`）以非 0 码退出；存在对应单元测试。
- [ ] **GR-1 目录纯净**：CI 的 `check-no-corpus-files` 步骤在 `scripts/generate/` 出现 `.pdf`/`.docx`/`.txt` 数据文件时构建失败。
- [ ] **GR-1 断言行测试（必做）**：`assert_prompt_clean` 单测通过——注入"历年真题"抛 `SourceLeakSuspect`；注入"2023 年上半年试题"抛 `SourceLeakSuspect`；正常组装 Prompt 不抛异常。
- [ ] **GR-1 断言阻断**：构造含黑名单词的批次，运行脚本，断言**未发生任何 LLM 调用**（用 mock 计数为 0），批次 `status=FAILED` 且 `fail_reason=SOURCE_LEAK_SUSPECT`。
- [ ] **GR-2 留痕完整**：导入任一行后，`rk_question` 的 `source_type`/`source_ref`/`source_license`/`generation_batch_id` 四字段非空；手动置空任一字段的 JSONL 行被拒并返回 `3004`。
- [ ] **GR-2 流转日志**：一次"导入 → 提交 → 驳回 → 退回重生成 → 编辑 → 提交 → 通过 → 发布"的完整链路，`rk_question_review_log` 恰好 8 条记录，`action` 序列为 `CREATE/SUBMIT/REJECT/REGENERATE/EDIT/SUBMIT/APPROVE/PUBLISH`。
- [ ] **GR-3 投诉下架**：提交 `type=COPYRIGHT` 的反馈，管理端列表中排在最前；调用 `POST /api/v1/admin/feedback/{id}/handle` 且 `offlineQuestion=true` 后，题目 `status=OFFLINE`，并存在一条 `sys_operation_log`（module=`QUESTION`, action=`OFFLINE`）。
- [ ] **Schema 层**：`correctAnswer` 不在 `options.key` 中的行被拒，`errors[].reason` 明确指出原因。
- [ ] **Schema 层**：`options` 只有 3 项、或两个 `key` 重复（如 A/A/B/C）的行被拒。
- [ ] **内容层**：`$` 不成对的题干（如 `$O(n` 缺右定界符）被拒。
- [ ] **业务层相似度**：把一道已发布题目原样再导入一次，被拒且 `errors[].reason` 提示高度相似（≥0.9）。
- [ ] **业务层待建标签**：导入一道 `knowledgePoints[].name` 为该科目不存在的标签（该行 `knowledgePoints` 可为空数组），**该题仍以 0 个知识点成功导入为 `DRAFT`**；导入成功后该题的创建 `review_log.remark` 含 `[KP_PENDING:标签名]`，且审核台可按该前缀筛出。管理端打开该题的编辑页可正常保存（`kpIds` 允许 0~5）。
- [ ] **知识点数量口径（对应评审 B4）**：`kpIds` 传 0 个时，`PUT /admin/questions/{id}` 保存成功（不再报校验错）；对 0 个知识点的 `APPROVED` 题执行 `POST .../{id}/publish` 被拒（发布前要求 ≥1 个），补挂 1 个标签后发布成功。
- [ ] **导入错误定位（必做）**：构造 100 行 JSONL，其中**第 12 行选项数不为 4、第 45 行 `correctAnswer` 不在选项中、第 88 行含黑名单词"历年真题"**；调用 `/api/v1/admin/questions/import` 后断言 `data.imported=97`、`data.errors` 长度 3 且 `line` 依次为 12/45/88。
- [ ] **导入幂等**：同一份 JSONL 连传两次，第二次返回 `1005`（批次已导入），数据库题目数不变；再次传入同一批次但改写 `clientKey`（模拟断点续传），重复行计入 `skipped` 而非新增。
- [ ] **批次计数**：一批 30 题经 20 通过 / 6 驳回 / 4 退回重生成后，`produced_count=30`、`approved_count=20`、`rejected_count=6`；"退回重生成"未使 `rejected_count` 变成 10。
- [ ] **批次状态机**：批次状态依次经过 `PENDING → GENERATING → GENERATED → REVIEWING → COMPLETED`；中途注入 `SOURCE_LEAK_SUSPECT` 时转为 `FAILED` 且 `finished_at` 非空。
- [ ] **发布侧联动**：发布 1 题后 `rk_subject.question_count`、`rk_outline_node.question_count`、`rk_knowledge_point.question_count` 各 `+1`；下架后各 `-1`。
- [ ] **发布互斥**：`status=PENDING_REVIEW` 的题调用 `POST .../{id}/publish` 返回 `3003`；`published` 唯一的组卷可见性由"非 `PUBLISHED` 题目不出现在 `/practice/papers` 结果中"验证。
- [ ] **发布前校验（对应评审 B7）**：手工把某 `APPROVED` 题的 `source_ref`（或 `source_license` / `generation_batch_id` / `source_type`）置空后调用 `POST .../{id}/publish` 被拒（返回 `3004`，不得进入 `PUBLISHED`）；补回字段后发布成功。
- [ ] **批量通过护栏**：对含 3 道 `[SUSPECT]` 标记题的 10 道题执行 `batch-review`，断言只有 7 道变为 `APPROVED`，响应中返回被跳过的 3 个 id。
- [ ] **统计一致性**：跑 8.9.2 第 10 条的巡检任务，在正常情况下 0 告警；**构造一道"驳回 → 退回重生成 → 通过"的题（该题同时进入 `rejected_count` 与 `approved_count`），巡检仍 0 告警**（验证已改为终态互斥口径，对应评审 B2）；手工把某题 `generation_batch_id` 置空后巡检告警。
- [ ] **成本可审计**：脚本日志记录每批次的 `input_tokens` / `output_tokens` / 重试次数，可回填 8.10.2 的估算公式。