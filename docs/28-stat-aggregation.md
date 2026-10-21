# 28 · 统计写入层（`stat_*` 增量更新 + 掌握度重算）

> 本轮做的是 27 §7 的第 2 项：**P6 的 `stat_*` 增量更新**，也就是 26 §7 偏离 1 欠下的那笔债。
> 目标只有一个：让 `onlyUnmastered` 从"空操作"变成真的过滤器，并把 P6 读取接口要用到的
> `stat_*` 表维护成"随时可读"的状态。
>
> 本轮**不**包含 P6 的读取接口（05 §5.8 的四条）与前端页面；那部分仍未开始。

---

## 1. 交付了什么

| 层 | 内容 |
| --- | --- |
| 增量计数 | `PracticeService.report` 在**同一事务**内累加 `stat_daily`（answered/correct/duration）与 `stat_user_subject`（answered/correct/accuracy/last_answered_at） |
| 脏标记 | 上报提交后置 `rk:stat:stale:{userId}:{subjectId}`（TTL 7 天，06 §6.11） |
| 掌握度重算 | `StatisticsService.refreshIfStale` / `recompute`：由 `user_answer_record` 重算 `stat_user_outline`、`stat_user_kp`，再按章节题量加权出 `stat_user_subject.mastery` |
| 触发点 | 练习组卷开 `onlyUnmastered` 时（当前唯一读 `stat_user_kp.mastery` 的地方）；P6 读取接口落地后复用同一入口 |
| 夜间兜底 | `StatisticsScheduler`：每日 03:00 全量重算近 90 天活跃用户的 (user, subject)（06 §6.13，`task.enabled.stat.rebuild` 开关） |
| 单测 | `StatAggregatorTest`：06 §6.10.1 的四种场景 + 去重口径两个方向 + 同秒裁决 + 时间衰减 + 科目加权，共 10 条 |

## 2. 口径（唯一实现：`StatAggregator`）

`mastery = coverage × recency + (1 - coverage) × prior`（06 §6.10.1）

- `a` = 去重后已作答题数；`n` = 该维度已发布题量；`coverage = min(1, a/n)`。
- `recency = Σ(w_i×c_i)/Σ(w_i)`，`w_i = 0.5^(Δd_i/H)`，`H` 取 `mastery.half.life`（默认 30 天）。
- `prior` 取 `mastery.prior`（默认 0.5）。
- 科目掌握度 = `Σ(章节 mastery × 章节题量)/Σ题量`；**没有作答的章节用先验补**，否则"只练一章"会拿到接近满分的科目掌握度。

**章节 / 知识点**用"最近一次作答"去重口径（同一题重复作答只算最后一次，裁决 `(answered_at, id)`）；
**`stat_daily` 与 `stat_user_subject` 的计数**用事件累加口径（做两次算两次）。

## 3. 偏离登记（对 05/06 的偏离，逐条给理由）

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | **去重从 SQL 窗口函数移到 Java**（06 §6.10.2 示例用 `ROW_NUMBER()`） | 06 §6.10.2 自己要求"一题先对后错，`accuracy` 必须为 0"有单测覆盖；SQL 里的窗口函数只有连库集成测试才能覆盖，而本项目测试是纯单测（不连 MySQL）。语义与文档一致，"唯一实现"从 SQL 移到 `StatAggregator` | 单用户单科目流水全部读回内存去重；已在 mapper 加 20000 行上界（AC-4） |
| 2 | **重算触发点是"练习组卷 `onlyUnmastered`"，不是"读取统计接口"** | 06 §6.10.2 的触发点写的是统计读取接口，但 P6 读取接口未落地；当前唯一消费者是 `onlyUnmastered` 的组卷 SQL。保留 `rk:stat:stale` 标记，P6 读取接口落地后调用同一个 `refreshIfStale` | 不做统计读取的用户，`stat_*` 表只在夜间全量重算时刷新，够用 |
| 3 | **`stat_user_subject` 的计数用事件口径，`stat_user_outline/kp` 用去重口径** | 06 §6.4 / §6.10.2 明确要求 `stat_user_subject` 增量更新；§6.10.2 那段"必须走窗口函数口径"紧跟在章节重算 SQL 之后，作用域是 outline/kp | 两处 `accuracy` 语义不同：科目页是"累计正确率"，章节/知识点是"最近一次正确率"。若 P6 要求两者一致，需另开一轮改口径 |
| 4 | **06 §6.10.2 疑似笔误** | 原文写"一题先错后对，`accuracy` 应为 0"；按它自己定义的"取最近一次"，应为 1，写 0 的应是"先对后错" | 实现在单测里把两个方向都钉死（`laterWrongAnswerWins` / `laterCorrectAnswerWins`），无论笔误指向哪边都不会漂 |

**一条观察（不是偏离，未改公式）**：06 §6.10.1 的 `recency` 是**归一化**加权正确率，
因此"很久以前全部答对"仍会得到 `recency = 1`——时间衰减只在同一维度内**混合新旧作答**时改变相对权重，
并不会让"只做过一次且答对、之后再没碰"的掌握度随时间下降。公式按文档原样实现；
若 P6 期望的是绝对衰减（例如超过若干半衰期后掌握度回落到先验附近），需要先改 06 §6.10.1，再改 `StatAggregator`。

## 4. 验证（本轮实跑，判据为业务码 `code` 与库中值）

| 步骤 | 命令/入口 | 观察结果 |
| --- | --- | --- |
| 单测 | `mvn -q -pl rk-service -am test` | `StatAggregatorTest` 10/10 通过；全量 `mvn -q clean package` 退出 0 |
| 单测会看 | 注入 `coverage = 1.0` | 半覆盖用例如期失败（期望 0.7500、实得 1.0000），改回后全绿 |
| 增量计数 | 登录 `negctl_81217` → `POST /practice/records`（5 题全对，各 30000ms） | `stat_daily` = 5/5/150；`stat_user_subject` = 5/5/1.0000，`mastery` = 0.5000（先验） |
| 懒重算 | `POST /practice/papers`（`onlyUnmastered=true`） | `stat_user_outline` 2 行、`stat_user_kp` 12 行；`stat_user_subject.mastery` = **0.5120** |
| 加权正确 | 手工核对 | `(1.0×5 + 0.6×5 + 0.5×240)/250 = 0.512` ✓（0.6 是"1/5 覆盖且全对"） |
| 过滤器生效 | `KP=1`：`onlyUnmastered=false → 1 题`，`true → 0 题` | `onlyUnmastered` 不再是空操作 ✓ |
| 标记生命周期 | 再上报 1 题 → 再读一次 | Redis 标记 `1 → 0`；`stat_daily` 累加到 6/151 ✓ |
| 夜间查询 | 直接执行 `selectActiveUserSubjects` 的 `INTERVAL ? DAY` 预处理语句 | 语法可用，返回 `(24, 6)` ✓ |

> 遗留：本机 MySQL/Redis 常驻，未跑 P2/P4 验收（用户明确要求跳过）；测试用户在库里留下 6 条
> 新流水与派生统计行，与 26 §8 记录的既有测试数据同源，生产前用 `deploy/sql` 重建即可。

## 5. 涉及文件

新增：`rk-domain` 统计实体与投影 6 个；`rk-repository` 五个 Mapper（+5 XML）、`StatStaleDao`；
`rk-service` 的 `StatAggregator`、`StatisticsService(+Impl)`、`StatisticsScheduler`；单测 1 个。
修改：`CacheKeys`、`SysConfigService`、`PracticeServiceImpl`、`RkBootApplication`（`@EnableScheduling`）。

## 6. 下一步

1. **P6 读取接口**（05 §5.8 四条：`/stats/overview|trend|outline|weak-kp`）——`refreshIfStale` 已可复用。
2. P6 前端统计页。
3. P5 模拟考落地后，把 `ExamService.submit` 也接到 `incrementCounters`（05 §5.6 的 source=EXAM）。
