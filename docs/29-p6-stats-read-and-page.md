# 29 · P6 读取接口与统计页

> 本轮把 28 §6 的三点"下一步"里的前两点做完：**P6 读取接口**（05 §5.8 第 32~35 条）
> 与**考生端统计页**（07 §7.3.1 的 `/stats`）。
> 第三点（把 `ExamService.submit` 接到 `incrementCounters`）**未完成**——它依赖尚未实现的
> P5 模拟机考，见 §5。

## 1. 交付了什么

| 层 | 内容 |
| --- | --- |
| 接口 | `GET /api/v1/stats/overview`、`/trend`、`/outline`、`/weak-kp`（全部 `@SaCheckLogin`） |
| 服务 | `StatisticsQueryService(+Impl)`；读取前一律先 `refreshIfStale`，保证展示的是最新画像 |
| 纯函数 | `StatOutlineTreeBuilder`：叶子统计行沿树向上滚成父节点掌握度（子节点题量加权） |
| 前端 | `/stats` 页（`views/portal/stats-overview.vue`）+ `components/stats/TrendChart.vue`（ECharts 按需注册、动态 import）+ `api/stats.ts` + `types/api.d.ts` 四个类型 + 顶栏「统计」入口 |
| 单测 | `StatOutlineTreeBuilderTest`（3 条：加权、先验、空树） |

## 2. 口径与边界

- **总览**：科目维度（做题量/正确率/掌握度/章节覆盖/模考）与**用户维度**（学习天数/连续天数/今日）两组口径并存——连续学习天数不随科目切换而变（06 §6.10.3）。
- **趋势**：按科目逐日**事件**计数，只统计客观题；缺失日期补 0（10 §10.8 验收项）。`days` 默认 30、上限 90。
- **章节掌握度**：叶子用自己的统计行；父节点 `Σ(子 mastery × 子题量)/Σ题量`；没练过/没题的节点取先验 0.5。`questionCount` 由 `rk_question` 现算，不取 `rk_outline_node.question_count` 冗余列（权重与题量必须同源）。
- **薄弱知识点**：`mastery < maxMastery`（默认 0.6）升序，`limit` 默认 10、上限 30，带 `kpName` 与「去练习」跳转。
- **无科目**：`subjectId` 缺省取 `currentSubjectId`；两者都没有 → `5001`，前端渲染引导空态（10 §10.8 的"暂无统计"）。

## 3. 偏离登记

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | **趋势从 `user_answer_record` 现算，不读 `stat_daily`** | 04 §4.6 说 `stat_daily` 供趋势图，但该表**没有 `subject_id` 列**，而 05 §5.8 的 trend 带 `subjectId?`。要按科目过滤只能回到流水分组 | `stat_daily` 仍供总览的学习天数/连续天数/今日；趋势与它同为事件口径，但不再共用那张表 |
| 2 | **章节掌握度用「缩进 + 横向条」而非纯柱状图** | 10 §10.8 验收项要求"树形展示、父节点为子节点题量加权"，07 §7.5.1 写的是"柱状"。缩进保留树形层级、条长表达数值 | 同时满足两处；若以 07 为准需改成仅叶子的条形图，会丢掉父节点加权这一验收项 |
| 3 | **新增 `StatOutlineNodeVO`，不扩公开的 `OutlineNodeVO`** | 05 §5.8 说 outline 返回"`OutlineNodeVO[]` 每个节点额外含"三个统计字段。若真给 `OutlineNodeVO` 加三个可空字段，公开接口 `GET /subjects/{id}/outline` 会多出三个恒 `null` 的键（Jackson 配置为 always） | 统计与公开大纲各自成形；字段名与 05 的字段表一致 |
| 4 | **参数越界取 clamp，不报 1001** | `days > 90`、`limit > 30`、`maxMastery` 越界时夹到合法区间 | 前端拿不到"参数非法"，但也不会因为多传一个数就整页报错 |
| 5 | **统计页默认科目取"第一个有题的科目"** | `currentSubjectId` 的写接口（05 §5.3 第 7 条）未实现，库里常为空；再回落到科目列表第一个（初级、多为空库）会让整页显示 0——看起来像页面坏了 | 若用户显式选了科目或 URL 带了 `subjectId`，以它为准 |

## 4. 验证（本轮实跑）

| 步骤 | 结果 |
| --- | --- |
| 后端全量构建 | `mvn -q clean package` 退出 0；**60** 个单测全绿（新增 `StatOutlineTreeBuilderTest` 3 条） |
| 前端三件套 | `pnpm lint` / `type-check` / `build` 均退出 0；产物里 `stats-overview`、`TrendChart` 与 `vendor-echarts` 各自成 chunk（07 §7.10 的按需分包生效） |
| `GET /stats/overview?subjectId=6` | `answeredCount=7, correctCount=6, accuracy=0.8571, mastery=0.5100, studyDays=1, continuousDays=1, outlineCoverage=0.0364, todayAnswered=7` |
| `GET /stats/trend?subjectId=6&days=7` | 7 个点，末日 `2026-10-05 / 6`，其余日期补 0 |
| `GET /stats/outline?subjectId=6` | 13 个根节点；根 id=1（计算机系统基础知识，子树 20 题）掌握度 **0.65**，是子节点按题量加权的加权平均，不是简单平均 |
| `GET /stats/weak-kp?subjectId=6`（答错一题后） | 返回 2 条（`IEEE 754 浮点数`、`浮点数格式`，mastery=0.0000）——证明过滤与排序生效 |
| 渲染取证 | CDP 截图 `.impeccable/review/p6-stats.png`：7 张总览卡、1 个 canvas（趋势）、66 行章节树、2 条薄弱点，无横向滚动、无错误面板；未练过的章节显示「未练习」、练过未达标才显示「未掌握」 |

> 测试手段与 28 同源：登录 `negctl_81217` 走真实 HTTP。测试用户在库里留下了第 7 条流水与派生统计行，生产前用 `deploy/sql` 重建即可。

## 5. 未完成：第 3 点（模拟考接入统计）

`28 §6` 的第 3 点是"**P5 模拟考落地后**，把 `ExamService.submit` 接到 `incrementCounters`（05 §5.6 的 source=EXAM）"。
它**不是**能独立完成的一项：P5（模拟机考）整个阶段尚未实现，仓库里没有 `ExamService`、
没有会话创建/计时/交卷，`stat_user_subject.exam_count` / `best_score` 因此恒为 0 / null。
统计侧已经就绪——`incrementCounters` 与 `refreshIfStale` 与来源无关，P5 落地后在其
交卷事务里按本节口径调用即可（`source=EXAM` 的记录也会自然进入趋势、章节与知识点聚合）。

## 6. 下一步

1. **P5 模拟机考**（`10 §10.11` 标为风险最高）——落地时一并完成 §5 的统计接入。
2. `stat_user_subject.exam_count` / `best_score` 的写入（属 P5）。
3. 三处文档回填 + 本地首次提交 + 清理测试数据（`27 §2.3` / §2.4 的既有欠账）。
