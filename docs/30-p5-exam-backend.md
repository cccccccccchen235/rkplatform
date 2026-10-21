# 30 · P5 模拟机考（后端闭环）

> 本轮做 P5 的后端部分：05 §5.6 的**十个接口**（组卷 / 答题会话 / 服务端计时 / 断点续答 /
> 分节切换 / 交卷判分 / 成绩详情 / 历史成绩 / 放弃）+ 超时兜底 + 接入统计。
> **未包含**：考生端三个页面（模拟考中心 / 答题页 / 成绩页）与管理端卷面模板配置
> （05 §5.10 第 51/52 条）——见 §6。

## 1. 交付了什么

| 层 | 内容 |
| --- | --- |
| 接口 | `POST /exams`、`GET /exams`、`GET /exams/{id}`、`PUT /exams/{id}/answers`、`POST /exams/{id}/sittings/{n}/start`、`POST /exams/{id}/sections/{n}/advance`、`POST /exams/{id}/sittings/{n}/finish`、`POST /exams/{id}/submit`、`GET /exams/{id}/result`、`POST /exams/{id}/abandon` |
| 组卷 | 章节配额（保底 + 最大余数）→ 难度三档（30/50/20）→ L1 补档 → L2 放宽防重复 → L3 跨章节 → L4 缩减；题干/选项/正确答案在组卷时快照 |
| 计时 | `ExamTiming` 纯函数：真值由 `exam_session_sitting.started_at` 派生，`PENDING` 不倒计时（午休不扣时） |
| 交卷 | `ExamScoringService` 判分 + 写流水（`source=EXAM`）+ 错题本 + 防重复窗口 + 统计；幂等 |
| 超时兜底 | 惰性结算（任意入口）+ 定时任务（每 5 分钟超时、每 30 分钟会话级滞留，评审 G1） |
| 统计接入 | `incrementCounters`（作答/正确率）+ `recordExam`（`exam_count` / `best_score`）；`ABANDONED` 不计入 |
| 单测 | `ExamTimingTest` 5 条 + `ExamPaperAssemblerTest` 6 条 |

## 2. 关键口径

- **计时**：`remainingSec = max(0, total_duration_sec - (now - started_at))`，`now` 由服务端注入；
  `exam_session.remaining_sec` 只是当前场次的镜像，任何判定都不读它（06 §6.9.1）。
- **场次衔接**：场次一结束后场次二留在 `PENDING`（`remainingSec` 恒等于总时长），必须用户主动 `start` 才开始计时（评审 A5）。
- **L4 缩减**：题量不足时按 06 §6.6.2 Step C 缩减，`section.maxScore = 每题分值 × 实际题量`，请求量随之缩小（不虚报满分）。

## 3. 偏离登记

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | 配额与降级在 Java 侧完成（06 §6.6.2 给的是逐章 SQL） | 分支多（保底/最大余数/三档/两级补足/L2/L3/L4），SQL 版每条分支都要连库验证；Java 版可纯单测 | 一次取回某科目某题型的全部已发布题（单科目 ≤ 数百），加 5000 行硬上界 |
| 2 | 06 §6.6.2 的决策段与 §6.9.3/10.7 冲突：前者说"客观题总量不足直接 4002"，后两者说"按 L4 缩减并提示" | 以 10 §10.7 验收项为准（验收标准是权威） | `4002` 只在完全没有可判分分节、或组卷结果为 0 题时返回 |
| 3 | 0 题分节不进入作答流程 | L4 可能把某分节缩到 0 题（当前题库无主观题，「应用技术」即为 0）。若仍按 `duration_min` 门禁，用户会被锁在"什么都没有的一节"里 120 分钟 | `durationMin` 校验与 `advance`/`finish` 的下一节选择只作用于有题的分节 |
| 4 | `ExamItemVO.images` 恒为空数组 | 04 §4.7 的 `exam_session_item` 没有 images 快照列；当前题库也没有带图题 | 带图题一旦出现，考试页会丢图 → 需给该表加 `images_snapshot JSON`（本轮未改 DDL，留给下一步） |
| 5 | 成绩回顾的 `outlinePath`/`knowledgePoints`/`isFavorited` 为空 | 快照表没有这些列与关联；题干/选项/正确答案/参考答案/解析/对错/用户作答全部取自快照（可复现） | 成绩页逐题回顾不显示章节路径与标签；已实测"删除原题后成绩仍可复现" |
| 6 | `advance` 的 `canLeaveInSec` 放在错误文案里，不在 `data` | `BizException` 不带 `data` 载荷；为它新增一条全局异常通道的成本大于收益 | 前端需从 `message` 里读秒数，或自行用 `durationMin` 计算 |
| 7 | `repeatedCount` 只记日志 | 05 §5.6 的 `ExamSessionVO` 没有该字段；擅自加字段等于改契约 | 验收项"重复比例可观测"目前靠服务端日志；若要进 API，需先改 05 |

## 4. 验证（实跑，判据为业务码与库中值）

| 验收项 | 结果 |
| --- | --- |
| 可发起模拟考，题量/章节分布符合配额 | SD：`items=75`、`autoScoredMaxScore=75`、`totalMaxScore=75`（主观题节 L4 缩到 0，满分随之降为 75） |
| 题量不足按 L4 缩减并提示 | SAD 临时插入 1 题：`items=1`、`autoMax=1`、sections = `1:OBJECTIVE/1 题`、`2:SUBJECTIVE/0 题`、`3:ESSAY/0 题` |
| 计时以服务端为准 | `ExamTimingTest`：把 now 调快 2 小时，剩余时长照减；`PENDING` 恒为总时长 |
| 高级科目两场次独立计时（A5） | 场次一 `IN_PROGRESS/rem=6600` 时场次二 `PENDING/rem=7200`（满值）；start 场次二后 `IN_PROGRESS/rem=7201`；期间 `sessionRemaining=0` |
| 论文节不可提前结束 | `POST /sittings/2/finish` → `1005` |
| 超时后任意接口访问触发自动交卷 | 把 `started_at` 回拨 20000s（> 总时长+60）后 `GET /exams/{id}` → `status=SCORED`、场次 `FINISHED`、`remaining=0` |
| 交卷幂等 | 连点两次 `submit` → 两次都 `code=0` 且 `objectiveScore` 相同（5.0） |
| 成绩：客观题得分/正确率 | `score=5.0`、`autoMax=75`、`accuracy=0.0667`；分节 `1:OBJECTIVE/75.0/scored=true`、`2:SUBJECTIVE/0.0/scored=false` |
| 历史列表 ABANDONED 过滤 | 默认 `total=2`；`includeAbandoned=true` → `total=3` 且含 `ABANDONED`；`ABANDONED` 不计入 `exam_count` |
| 单题超 10 分钟按 10 分钟入库 | 传 `durationMs=700000` → 流水 `duration_ms=600000` |
| 未作答/未开始的状态码 | 未开始场次保存作答 → `1005`（不是 4005）；不属于会话的 itemId → `4006`；有未完成会话再发起 → `4003` |
| 断点续答 | 开始场次 → 保存一条作答 → `GET /exams/{id}` 返回该 `userAnswer` 与最新 `remainingSec` |
| 快照可复现 | 删除原题后 `GET /exams/{id}/result` 仍返回题干与 `correctAnswer`（来自快照） |
| 防重复窗口 | Redis `rk:repeat:24:6` 含窗口内 13 个题 ID；新卷对未耗尽章节零重复；被复用的 3 题全部来自"整章都做过"的章节（L2 设计行为） |

> 本轮为两场次验证临时插入过 1 道 SAD 题（`source_ref='p5-verify'`），**已删除**；
> 测试用户留下 5 个模拟考会话（4 SCORED + 1 ABANDONED）与派生流水，生产前用 `deploy/sql` 重建即可。

## 5. 文件

新增：`rk-domain` 的 3 个状态枚举（`ExamStatus`/`SittingStatus`/`SectionStatus`）、4 个实体
（`ExamSession`/`ExamSessionSitting`/`ExamSessionSection`/`ExamSessionItem`）、
`ExamCreateRequest`/`ExamAnswerRequest`、8 个 VO 组、`ExamQuestionCandidate`；
`rk-repository` 的 `ExamQuestionMapper` 与四个会话表 Mapper（+5 XML）；
`rk-service` 的 `ExamTiming`、`ExamPaperAssembler`、`ExamConverter`、`ExamService(+Impl)`、
`ExamScoringService(+Impl)`、`ExamTimeoutSettler`、`ExamSessionScheduler`；
`rk-api` 的 `ExamController`；单测 `ExamTimingTest`、`ExamPaperAssemblerTest`。

修改：`RepeatWindowDao`（补读取与回填）、`UserAnswerRecordMapper`（窗口回填查询）、
`StatUserSubjectMapper`（`recordExam`）、`StatisticsService`（`recordExam`）、
`SysConfigService`（`task.enabled.exam.timeout` / `session.stale` 两个开关）。

## 6. 下一步

1. **考生端三页**：模拟考中心（`/exam`）、答题页（`/exam/{id}`，答题卡 + 倒计时 + 分节切换）、
   成绩详情页（`/exam/{id}/result`）。路由与页面专项说明见 07 §7.3.1 / §7.5.1。
2. **管理端卷面模板配置**（05 §5.10 第 51/52 条）——含"`autoScoredMaxScore` 与客观题分节之和不符时保存失败"
   与 IPT「应用技术」`NOT_SUPPORTED` 的验证（10 §10.7 前两条验收项）。
3. `images_snapshot` 列（偏离 4）与 `repeatedCount` 可观测性（偏离 7）——都需要先改 04/05。
