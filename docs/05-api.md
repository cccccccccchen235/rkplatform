# 05 · 接口设计

> 接口风格：REST + JSON，路径前缀 `/api/v1`。
> 本文档的接口路径、字段名是前后端唯一契约；`06/07` 必须与本文件一致。

## 5.1 通用约定

| 项 | 约定 |
| --- | --- |
| 路径前缀 | `/api/v1`，管理端统一置于 `/api/v1/admin/**` |
| 请求编码 | `Content-Type: application/json; charset=utf-8`（文件上传除外） |
| 鉴权 | 请求头 `Authorization: Bearer <token>`（Sa-Token 的 `token-name` 配置为 `Authorization`，`token-prefix` 配置为 `Bearer`） |
| 分页请求 | `pageNum`（从 1 开始，默认 1）、`pageSize`（默认 10，**上限 100**，超出按 100 处理） |
| 分页响应 | `{ "list": [...], "total": 123, "pageNum": 1, "pageSize": 10 }` |
| 时间格式 | 字符串 `yyyy-MM-dd HH:mm:ss`（服务端时区 `Asia/Shanghai`） |
| ID 类型 | 字符串传输（避免 JS 精度丢失），服务端用 `Long` |
| 枚举 | 一律传大写字符串，取值见 [02-domain-model.md](02-domain-model.md) 与 [04-database.md](04-database.md) |
| 幂等 | 答题保存、交卷等接口允许重复调用；交卷为幂等操作（已 `SCORED` 时直接返回结果） |

### 5.1.1 统一响应体

```json
{
  "code": 0,
  "message": "ok",
  "data": { },
  "traceId": "8f2c1a9b4d7e4f10"
}
```

- `code = 0` 表示成功，非 0 为业务错误码；HTTP 状态码**始终为 200**（除鉴权失败返回 401、无权限返回 403，便于前端拦截器统一处理）。
- **上一条的作用域**：以上约定**只适用于业务应答**——即由后端业务代码决定结果、并走统一响应体的那些响应。**不套本响应体的有两类**：① 鉴权失败（401）与无权限（403），前端拦截器专门处理；② **守卫类响应**——文档页守卫（`RK_DOC_ENABLED=false` 时 `/doc.html`、`/v3/api-docs` 返回**真 404**，不是 `200 + 1003`）、以及将来的 Nginx 层限流与拒绝。理由：前端拦截器只需要区分"业务错误"与"根本没到业务层"两件事；把守卫的 404 压成统一响应体，会让"这个 URL 不存在"与"这个资源业务上查不到"变成同一个信号，前端和监控都无从分辨。注意路径不存在**而请求进入了 Spring MVC** 时，仍按 §5.1.2 返回 `200 + 1003`（`NoResourceFoundException` 由全局异常处理器接住）。
- `traceId` 用于日志定位，前端错误提示中展示，便于用户反馈问题时排查。

### 5.1.2 错误码表

> 台账中的分段为「1xxx 通用 / 2xxx 题库 / 3xxx 考试 / 4xxx 统计」。实现时补充了"认证与用户"分段，最终分段如下（**以此表为准**）：
> **1xxx 通用 · 2xxx 认证与用户 · 3xxx 题库与内容 · 4xxx 考试与判分 · 5xxx 统计**

| code | 含义 | HTTP |
| --- | --- | --- |
| 0 | 成功 | 200 |
| 1000 | 系统内部错误 | 200 |
| 1001 | 参数校验失败（`data` 中返回字段级错误列表） | 200 |
| 1002 | 请求过于频繁，请稍后再试 | 200 |
| 1003 | 资源不存在 | 200 |
| 1004 | 无权限访问该资源 | 403 |
| 1005 | 当前状态不允许该操作 | 200 |
| 2001 | 用户名或密码错误 | 200 |
| 2002 | 账号已被禁用 | 200 |
| 2003 | 用户名已存在 | 200 |
| 2004 | 登录失败次数过多，请 15 分钟后再试 | 200 |
| 2005 | 未登录或登录已过期 | 401 |
| 2006 | 原密码错误 | 200 |
| 3001 | 科目不存在或已停用 | 200 |
| 3002 | 题目不存在 | 200 |
| 3003 | 题目状态不允许此操作 | 200 |
| 3004 | 题目内容不完整（如客观题缺少正确答案或选项） | 200 |
| 3005 | 检测到高度相似的重复题目 | 200 |
| 4001 | 该科目尚未配置试卷模板 | 200 |
| 4002 | 该科目卷面没有可作答的分节 | 200 |
| 4003 | 存在未完成的模拟考，请先交卷或放弃 | 200 |
| 4004 | 模拟考会话不存在 | 200 |
| 4005 | 会话已结束，不可再作答 | 200 |
| 4006 | 题目不属于该会话 | 200 |
| 4007 | 该分节在当前平台不可作答 | 200 |
| 5001 | 暂无统计数据 | 200 |

## 5.2 公共数据结构

以下 VO 被多个接口复用，字段即契约。

### `SubjectVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 科目 ID |
| `level` | string | `JUNIOR`/`MIDDLE`/`SENIOR` |
| `code` | string | 科目代码 |
| `name` | string | 科目名 |
| `paperType` | string | `JUNIOR_MID`/`SENIOR` |
| `examPeriod` | string | `BOTH`/`FIRST_HALF`/`SECOND_HALF` |
| `outlineStatus` | string | `FULL`/`SKELETON` |
| `contentStatus` | string | `SAMPLE`/`BULK` |
| `questionCount` | int | 已发布题量 |
| `intro` | string? | 简介 |
| `paperSummary` | object? | 试卷结构摘要（场次层级，对应评审 A5）：`{totalDuration, totalMaxScore, autoScoredMaxScore, sittings:[{sittingNo, sittingName, totalDuration, totalMaxScore, autoScoredMaxScore, sections:[{sectionNo, sectionName, sectionType, questionCount, supportStatus}]}]}`。`totalDuration` 为各场次之和。**列表接口（§5.4 第 8 条）恒为 `null`**，只有详情接口 `GET /subjects/{id}` 才填充——列表接口的价值是"有哪些科目"，卷面结构是"这个科目要考什么"，后者只在用户点进某个科目时才需要，逐科目拼装只会让首屏变慢 |

### `AdminSubjectVO`

管理端科目列表（05 §5.10 第 37 条）用。与 `SubjectVO` 的唯一差别是**多一个 `status`**——考生端接口只返回 `ENABLED`，那个字段恒为一个常量，放进 `SubjectVO` 是噪音；而管理端必须能看出哪一行已停用。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` / `level` / `code` / `name` / `paperType` / `examPeriod` / `outlineStatus` / `contentStatus` | string | 同 `SubjectVO` |
| `questionCount` | int | 已发布题量 |
| `paperTotalScore` | int? | 卷面满分；`null` = 无启用中的模板 |
| `intro` | string? | 简介 |
| `sortNo` | int | 同级排序号（新增时取同级 `max + 1`） |
| `status` | string | `ENABLED`/`DISABLED` |

> 管理端列表**不带 `paperSummary`**：列表页只做"有哪些科目、哪个停用了"，卷面结构在详情接口里取（与考生端的分工一致，05 §5.2 `SubjectVO` 的说明）。

### `OutlineNodeVO`（树形递归）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 节点 ID |
| `parentId` | string | 父节点，根为 `"0"` |
| `level` | int | 1/2/3 |
| `name` | string | 章节名 |
| `sortNo` | int | 排序 |
| `questionCount` | int | 本节点（含子节点）已发布题量 |
| `children` | OutlineNodeVO[] | 子节点 |

> 树形返回时**根节点数组的 `parentId` 恒为 `"0"`**（不是 `null`、不是省略）——与 DDL 的 `rk_outline_node.parent_id NOT NULL DEFAULT 0` 是同一条约定（04 §4.5）。前端按 `parentId === "0"` 找根，服务端不得返回 `null`。

### `KnowledgePointVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 知识点 ID |
| `subjectId` | string | 所属科目 |
| `name` | string | 标签名（同科目内唯一，`uk_subject_name`） |
| `aliases` | string? | 同义词，**逗号分隔的单个字符串**（不是数组）——库里就是一列，拆成数组会在写回时多一次序列化/反序列化，而调用方（检索、管理端列表）都只做 `LIKE` 或整串展示 |
| `questionCount` | int | 该标签下的已发布题量 |
| `status` | string | `ENABLED`/`DISABLED` |

> `KnowledgePointVO` 被公开接口 11 与管理接口 46/47/48 复用，故列在此处。管理端列表接口额外返回 `DISABLED` 项，公开接口只返回 `ENABLED`。

### `QuestionVO`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 题目 ID |
| `subjectId` | string | |
| `outlineId` | string | 所属叶子章节 |
| `outlinePath` | string[] | 章节路径名，如 `["计算机系统基础知识","操作系统","进程管理"]` |
| `questionType` | string | 见 02 文档题型枚举 |
| `stem` | string | Markdown + KaTeX |
| `images` | object[] | `[{url, alt}]` |
| `difficulty` | int | 1~5 |
| `options` | object[]? | `[{key, content}]`，仅客观题 |
| `knowledgePoints` | object[] | `[{id, name}]` |
| `isFavorited` | bool | 当前用户是否收藏；**恒为 `boolean`，游客为 `false`**（非 `null`、非省略，省掉前端三态分支，对应评审 C6） |

> **不含答案字段**。答案与解析是否返回由具体接口决定，见 5.4 与 5.6 的差异说明。

### `QuestionDetailVO`（在 `QuestionVO` 基础上增加）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `correctAnswer` | string? | 客观题答案 key |
| `referenceAnswer` | string? | 主观题/论文参考答案 |
| `scorePoints` | string[]? | 主观题评分要点 |
| `analysis` | string? | 解析 |
| `isCorrect` | bool? | 本次作答是否正确（主观题为 null） |
| `userAnswer` | string? | 本次作答内容 |

## 5.3 认证与用户

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | :-: | --- | --- |
| 1 | POST | `/auth/register` | 公开 | 注册 |
| 2 | POST | `/auth/login` | 公开 | 登录 |
| 3 | POST | `/auth/logout` | USER | 登出（服务端使 token 失效） |
| 4 | GET | `/users/me` | USER | 当前用户信息 |
| 5 | PUT | `/users/me` | USER | 修改昵称 |
| 6 | PUT | `/users/me/password` | USER | 修改密码 |
| 7 | PUT | `/users/me/current-subject` | USER | 设置当前备考科目 |

**1) POST `/auth/register`**

| 请求 | 类型 | 校验 |
| --- | --- | --- |
| `username` | string | 4~32 位，`^[a-zA-Z0-9_]{4,32}$`，唯一 |
| `password` | string | 8~32 位，必须同时含字母与数字 |
| `nickname` | string? | 1~32 位；缺省时取 `username` |

响应 `data`：`{ "token": "...", "expireAt": "2026-09-26 12:00:00", "user": UserVO }`
错误码：`1001`、`2003`

**2) POST `/auth/login`**

请求：`{ "username": string, "password": string }`
响应 `data`：同注册。
错误码：`2001`、`2002`、`2004`
> 登录失败按 `rk:login:fail:{username}` 计数（Redis，TTL 15 分钟），达 5 次返回 `2004`；登录成功清零。

**4) GET `/users/me`** → `data`: `UserVO`

| `UserVO` 字段 | 类型 |
| --- | --- |
| `id` / `username` / `nickname` / `role` / `status` | string |
| `currentSubjectId` | string? |
| `mustChangePassword` | boolean |
| `lastLoginAt` / `createdAt` | string |

> `mustChangePassword`：管理员首次登录（`init-data.sql` 插入的占位密码账号）时为 `true`，**改密成功后服务端置为 `false`**；前端凭该字段强制跳转改密页。**不新增专用错误码**（对应评审 C2）。

## 5.4 科目与大纲（公开）

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | :-: | --- | --- |
| 8 | GET | `/subjects` | 公开 | 科目列表，按 `level` 分组返回 |
| 9 | GET | `/subjects/{id}` | 公开 | 科目详情（含 `paperSummary`） |
| 10 | GET | `/subjects/{id}/outline` | 公开 | 大纲章节树（`OutlineNodeVO[]`，根节点数组） |
| 11 | GET | `/subjects/{id}/knowledge-points` | 公开 | 知识点标签列表 |

**8) GET `/subjects`** 请求参数：`level`（可选，过滤级别）；`onlyConfigured`（可选，默认 `false`，仅返回已配置卷面模板的科目）。
响应 `data`：`{ "JUNIOR": SubjectVO[], "MIDDLE": SubjectVO[], "SENIOR": SubjectVO[] }`

## 5.5 练习（含样例题）

**核心差异（重要）**

| 模式 | 是否返回答案 | 判分方 | 目的 |
| --- | :-: | --- | --- |
| 练习（`/practice/**`） | **返回** | 前端即时判分 | 练习模式本就"答完即看解析"，先给答案能省掉每题一次往返；服务端在上报记录时**仍会重新判分校验**，防止前端篡改 |
| 模拟考（`/exams/**`） | **绝不返回** | 服务端 | 考试必须防止答案泄漏 |

> 决策与理由
> 练习模式选择"下发答案 + 前端判分 + 上报时服务端复核"，而不是"每题一问服务端"。理由：练习时每题都要立即出对错和解析，走服务端会让一次作答变成两次网络往返，且服务端仍需保存"临时练习会话"，复杂度显著上升；而练习成绩不参与任何严肃判定，答案泄漏无风险。服务端复核保证统计数据的正确性不受前端影响。

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | :-: | --- | --- |
| 12 | POST | `/practice/papers` | USER（`@SaIgnore` + 方法内校验，见下） | 生成练习题目列表 |
| 13 | POST | `/practice/records` | USER | 批量上报答题结果 |
| 14 | GET | `/practice/progress` | USER | 查询练习进度 |
| 15 | PUT | `/practice/progress` | USER | 更新顺序练习游标 |

**12) POST `/practice/papers`**

| 请求 | 类型 | 说明 |
| --- | --- | --- |
| `subjectId` | string | 必填 |
| `mode` | string | `CHAPTER`/`KP`/`SEQUENCE`/`FAVORITE`/`WRONG_REDO`/`SAMPLE` |
| `refId` | string? | `CHAPTER` 传章节 ID；`KP` 传知识点 ID |
| `count` | int | 题量，默认 10，上限 50 |
| `difficulty` | int? | 难度过滤 |
| `onlyUnmastered` | bool? | 是否只取未掌握（掌握度 < 0.6）的题 |
| `fromSequence` | int? | `SEQUENCE` 模式续做起点 |

响应 `data`：`{ "questions": QuestionDetailVO[], "total": int, "nextSequence": int }`
错误码：`3001`、`1001`、`2005`
> `SAMPLE` 模式允许游客访问且强制 `count ≤ 5`，返回科目下随机已发布题。
>
> **游客放行机制（对应评审 B6）**：本接口标注 `@SaIgnore`，但**游客资格在方法体内校验** —— 仅当 `mode = SAMPLE` 且 `count ≤ 5` 时放行，否则抛 `2005`。**不得把本路径加入 `AuthWhitelist`**：白名单是路径级常量数组，只能表达"整条路径放行"，一旦写入，游客即可用 `mode = FULL` 绕过题量限制（安全洞）。白名单处仅留注释指向本方法内校验。
> 测试要求：未登录 + `mode = FULL` → 返回 `2005`（并入 ADR-02 的鉴权单元测试）。
> 已删除原末尾一句"`SCORED` 以外的状态会话无权访问本接口"（本接口无"会话"概念，系从考试节误抄，对应评审 D6）。

**13) POST `/practice/records`**

| 请求 | 类型 | 说明 |
| --- | --- | --- |
| `items` | object[] | `[{questionId, userAnswer, durationMs}]`，单次上限 50 条 |
| `subjectId` | string | |
| `source` | string | `PRACTICE_CHAPTER`/`PRACTICE_KP`/`PRACTICE_SEQUENCE`/`PRACTICE_FAVORITE`/`WRONG_REDO` |

响应 `data`：`{ "accepted": int, "correctCount": int }`（`correctCount` 为**服务端复核后**的正确数）
> 服务端行为：重新判分 → 写 `user_answer_record` → 更新 `user_wrong_question` → 增量更新 `stat_*`（见 06 文档）。同一 `questionId` 在 5 秒内重复上报视为重放，忽略。

**14) GET `/practice/progress`**（对应评审 C1/C8，补齐字段表）

参数：`subjectId`（必填）、`mode?`（`SEQUENCE`/`CHAPTER`/`KP`/`FAVORITE`/`WRONG_REDO`，缺省返回该科目全部模式的游标）。

响应 `data`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `subjectId` | string | 科目 ID |
| `mode` | string | 练习类型（`SEQUENCE`/`CHAPTER`/`KP`/`FAVORITE`/`WRONG_REDO`） |
| `refId` | string? | `CHAPTER` 传章节 ID；`KP` 传知识点 ID；其余为 `null` |
| `lastSequence` | int | 已做到的题序（对应 `fromSequence` 的续做起点） |
| `lastQuestionId` | string? | 最后作答题目 ID，供"继续上次练习"跳转 |
| `totalCount` | int | 该模式题目总数 |
| `answeredCount` | int | 该模式已作答题数 |
| `updatedAt` | string | 游标最后更新时间 |

错误码：`3001`

**15) PUT `/practice/progress`**（对应评审 C1/C8，补齐字段表）

| 请求 | 类型 | 说明 |
| --- | --- | --- |
| `subjectId` | string | 必填 |
| `mode` | string | 练习类型，取值同 `GET` |
| `refId` | string? | 章节 / 知识点 ID |
| `lastSequence` | int | 必填，当前题序 |
| `lastQuestionId` | string? | 当前题目 ID |

响应 `data`：`null`。

> 落点在 `user_practice_progress`（用户 × 科目 × 练习类型），幂等 upsert，见 02 §2.5.4 与 04。错误码：`3001`、`1001`。

## 5.6 模拟考

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | :-: | --- | --- |
| 16 | POST | `/exams` | USER | 发起模拟考（组卷） |
| 17 | GET | `/exams` | USER | 历史成绩列表 |
| 18 | GET | `/exams/{id}` | USER | 会话详情（断点续答） |
| 19 | PUT | `/exams/{id}/answers` | USER | 保存作答（不判分） |
| 20 | POST | `/exams/{id}/sittings/{sittingNo}/start` | USER | **启动第 N 场次计时**（场次二论文由用户主动开始） |
| 21 | POST | `/exams/{id}/sections/{sectionNo}/advance` | USER | 离开当前分节，进入同场次下一节 |
| 22 | POST | `/exams/{id}/sittings/{sittingNo}/finish` | USER | 提前结束整个场次 |
| 23 | POST | `/exams/{id}/submit` | USER | 交卷 |
| 24 | GET | `/exams/{id}/result` | USER | 成绩详情（此时才返回答案与解析） |
| 25 | POST | `/exams/{id}/abandon` | USER | 放弃 |

> **场次（sitting）与分节（section）两层结构**：高级科目含两个场次（场次一 = 综合知识 + 案例分析连考，场次二 = 论文独立），**场次间各有一条独立倒计时，不自动衔接**（论文在下午独立时段，中间隔午休）。服务端始终按**当前 `IN_PROGRESS` 场次**计时。业务规则见 [02-domain-model.md](02-domain-model.md) §2.4 与 §2.5.5（对应评审 A5）。

**16) POST `/exams`**

请求：`{ "subjectId": string }`
响应 `data`：`ExamSessionVO`

| `ExamSessionVO` 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sessionId` / `sessionNo` | string | |
| `subjectId` / `subjectName` | string | |
| `status` | string | `CREATED`/`IN_PROGRESS`/`SUBMITTED`/`SCORED`/`ABANDONED` |
| `currentSittingNo` | int | 当前场次；无 `IN_PROGRESS` 场次时为下一个待开始的 `PENDING` 场次 |
| `totalDurationSec` | int | **全场次**总时长之和（展示用） |
| `remainingSec` | int | **当前 `IN_PROGRESS` 场次**的剩余秒数；无进行中场次时为 `0`（须先调 `20` 启动） |
| `currentSectionNo` | int | **当前场次**的当前分节 |
| `questionCount` | int | 总题数 |
| `autoScoredMaxScore` / `totalMaxScore` | int | 分数口径（各场次之和） |
| `sittings` | SittingVO[] | 场次列表，见下 |
| `sections` | SectionVO[] | 全场次分节的扁平列表（含 `sittingNo`），见下 |
| `items` | ExamItemVO[] | **不含 `correctAnswer`/`referenceAnswer`/`analysis`** |

| `SittingVO` 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sittingNo` | int | 场次序号，从 1 起 |
| `sittingName` | string | 场次名，如 `上午连考`/`下午论文` |
| `totalDurationSec` | int | 本场次总时长（秒） |
| `remainingSec` | int | 本场次剩余秒数；`PENDING` 时等于 `totalDurationSec`，`FINISHED` 时冻结为 `0` |
| `status` | string | `PENDING`/`IN_PROGRESS`/`FINISHED`（见 02 §2.5.5） |
| `currentSectionNo` | int | 本场次当前分节 |
| `totalMaxScore` | int | 本场次卷面满分（`NOT_SUPPORTED` 分节不计） |
| `autoScoredMaxScore` | int | 本场次可判分满分（仅 `OBJECTIVE` 分节） |
| `objectiveScore` | decimal | 本场次客观题实际得分 |
| `startedAt` | string? | 场次开始时间；`PENDING` 时为 `null` |
| `finishedAt` | string? | 场次结束时间；未结束为 `null` |
| `sections` | SectionVO[] | 本场次的分节 |

| `SectionVO` 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sittingNo` | int | 所属场次 |
| `sectionNo` / `sectionName` / `sectionType` | int/string/string | `sectionType`：`OBJECTIVE`/`SUBJECTIVE`/`ESSAY` |
| `questionCount` / `maxScore` / `perQuestionScore` | int/decimal | |
| `durationMin` / `durationMax` | int | 本节最早/最晚可离开时间点（分钟），**均必填**（对应评审 A6） |
| `allowEarlySubmit` | bool | 论文节为 `false` |
| `usedSec` | int | 本节已用时长 |
| `supportStatus` | string | `NOT_SUPPORTED` 时分节仅展示提示，不可作答 |
| `status` | string | `PENDING`/`IN_PROGRESS`/`FINISHED`/`SKIPPED` |

> 已删除 `SectionVO` 的**结转字段**（DDL 中 `allow_carry_over` / `carry_in_sec` 已删除，对应评审 B11：节余时长在"场次内总时长共享"模型下自动可用，无需结转字段）。

| `ExamItemVO` 字段 | 类型 | 说明 |
| --- | --- | --- |
| `itemId` / `questionId` / `sequence` / `sectionNo` | string/int | |
| `questionType` / `stem` / `images` / `options` / `score` | — | 题干快照（不含答案） |
| `userAnswer` | string? | 已保存的作答 |
| `answeredAt` / `durationMs` | string/int? | |

错误码：`3001`、`4001`、`4002`、`4003`

**19) PUT `/exams/{id}/answers`**

请求：`{ "items": [{ "itemId": string, "userAnswer": string, "durationMs": int }] }`（单次上限 100 条）
响应 `data`：`{ "remainingSec": int, "currentSittingNo": int, "currentSectionNo": int }`
错误码：`4004`、`4005`、`4006`、`1005`
> **服务端按当前 `IN_PROGRESS` 场次计时**（约束 C4：以自身时间为准）。若请求携带的客户端时间与剩余时长冲突，一律以服务端为准。若当前场次剩余时长为 0 或已超时，自动结束该场次并返回 `4005`。
> **若当前无 `IN_PROGRESS` 场次则返回 `1005`**：需先调用 `20` 启动场次计时；`PENDING` 场次不接受作答。
> **`4006`**：当 `items` 中任一 `itemId` 不属于该会话（`sessionId`）时返回（对应评审 B8）。
>
> **`items` 允许为空数组**：此时本接口退化为**心跳与计时校准**请求 —— 服务端不写入任何作答，仅执行超时检测并返回最新 `remainingSec` 与 `currentSittingNo`/`currentSectionNo`。前端在用户长时间停顿（如阅读长题干、撰写论文）时以 60 秒间隔调用一次，用于：① 让服务端有机会触发超时结算；② 校准本地倒计时。心跳请求不写 `user_answer_record`。

**20) POST `/exams/{id}/sittings/{sittingNo}/start`**

请求：`{}`
响应 `data`：`{ "sitting": SittingVO, "remainingSec": int, "currentSectionNo": int }`（返回该场次最新的 `SittingVO`）
错误码：`4004`、`1005`
> 启动该场次计时，写入 `started_at`。**仅 `PENDING → IN_PROGRESS` 合法**；已是 `IN_PROGRESS`/`FINISHED` 返回 `1005`。
> **这是"论文场次需用户主动开始"的落点**：场次一结束时不会自动启动场次二，必须由用户点"开始论文"（对应评审 A5）。

**21) POST `/exams/{id}/sections/{sectionNo}/advance`**

请求：`{}`（**无请求体参数**）
响应 `data`：`{ "currentSectionNo": int, "remainingSec": int, "sections": SectionVO[] }`
错误码：`1005`、`4005`、`4007`（`NOT_SUPPORTED` 分节不允许发起离开动作）
> 离开当前分节、进入**同场次**的下一分节；**若当前节是本场次的最后一节，则等价于结束本场次**（等价于调用 `22`）。
> **校验**：必须 `usedSec >= durationMin`（分钟 × 60）才允许离开，否则返回 `1005`，并在 `data` 中给出最早可离开的剩余秒数（`{ "canLeaveInSec": int }`）。
> **已删除原 `finish` 接口的结转请求参数**（对应评审 B11）：节余时长在"场次内总时长共享"模型下自动可用，无需结转参数，本接口**无请求体**。

**22) POST `/exams/{id}/sittings/{sittingNo}/finish`**

请求：`{}`
响应 `data`：`{ "currentSittingNo": int, "remainingSec": int, "sittings": SittingVO[] }`
错误码：`4004`、`1005`
> 提前结束整个场次，置该场次为 `FINISHED`、`remaining_sec` 冻结为 `0`。
> **校验**：本场次所有分节的 `duration_min` 均已满足；**若本场次含 `allowEarlySubmit = false` 的分节（论文）则禁止**，返回 `1005`。

**23) POST `/exams/{id}/submit`**
请求：`{}`（无需传答案，答案已在 `19` 中保存）
响应 `data`：`ExamResultVO` 摘要（`objectiveScore`、`autoScoredMaxScore`、`accuracy`、`usedSec`、`sectionScores`）
> **语义澄清（对应评审 A5）**：全部场次均 `FINISHED` 后才可交卷；若仍有 `PENDING` 场次（用户放弃论文），则交卷时将该场次一并置 `FINISHED`（不生成作答），成绩页对应分区标注"**未作答**"。

**24) GET `/exams/{id}/result`** → `data`: `ExamResultVO`

| `ExamResultVO` 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sessionId` / `sessionNo` / `subjectName` | string | |
| `objectiveScore` | decimal | 客观题得分（**唯一参与判定的分数**） |
| `autoScoredMaxScore` | int | 可判分满分（各场次之和，仅 `OBJECTIVE` 分节） |
| `totalMaxScore` | int | 卷面满分（含主观题节；`NOT_SUPPORTED` 分节不计入） |
| `accuracy` | decimal | 客观题正确率 |
| `usedSec` | int | 实际用时（各场次之和） |
| `submittedAt` / `scoredAt` | string | |
| `sittingResults` | object[] | 场次级汇总：`[{sittingNo, sittingName, totalDurationSec, usedSec, objectiveScore, autoScoredMaxScore, totalMaxScore, status, answered: bool}]`。`answered = false` 表示该场次未作答（用户放弃论文），成绩页按其分区标注"未作答" |
| `sectionResults` | object[] | 分节级明细（含 `sittingNo`）：`[{sittingNo, sectionNo, sectionName, sectionType, maxScore, objectiveScore, supportStatus, scored: bool}]` |
| `items` | object[] | `QuestionDetailVO[]` 形式，**含答案与解析**；主观题含 `referenceAnswer`、`scorePoints` |

> 成绩页展示规则：按 `sittingResults` **分场次分区**；`sectionType != OBJECTIVE` 的分区标题旁标注"本节不判分，仅提供参考答案"且不显示任何分数；`answered = false` 的场次标注"本场次未作答"。

**17) GET `/exams`**（对应评审 B9、C8）

参数：`subjectId?`、`includeAbandoned?`（默认 `false`）、`pageNum`、`pageSize`。

> **默认仅返回 `status IN ('SUBMITTED','SCORED')`**；显式传 `includeAbandoned=true` 才包含 `ABANDONED`（与 10 的验收项"`ABANDONED` 不出现在列表中"对齐，对应评审 B9）。
>
> **`IN_PROGRESS` 在任何取值下都不返回，这是有意设计**（对应评审 G1）：高级科目考生考完上午连考后若不再回来，会话会停留在 `IN_PROGRESS`（场次二 `PENDING`）——该会话由 [06-backend.md](06-backend.md) §6.13 的「会话级滞留结算」在 6 小时内自动交卷，随后即以 `SUBMITTED`/`SCORED` 出现在本列表；在窗口期内用户通过 `POST /exams` 返回的 `4003` 提示框进入（见 [07-frontend.md](07-frontend.md) §7.4），不需要列表兜底。因此**不要为了"找回未完成考试"而把 `IN_PROGRESS` 加进本接口**，否则列表会被大量废弃会话污染。

响应 `data`（分页包装，与 5.1 一致）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `list` | object[] | 见下 |
| `total` | int | 总条数 |
| `pageNum` / `pageSize` | int | 与请求一致 |

`list` 项字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `sessionId` | string | 会话 ID |
| `sessionNo` | string | 会话编号 |
| `subjectId` / `subjectName` | string | |
| `status` | string | `SUBMITTED`/`SCORED`（默认口径）；`includeAbandoned=true` 时可能为 `ABANDONED` |
| `objectiveScore` | decimal | 客观题得分 |
| `autoScoredMaxScore` | int | 可判分满分 |
| `accuracy` | decimal | 客观题正确率 |
| `usedSec` | int | 实际用时 |
| `submittedAt` | string? | 交卷时间；`ABANDONED` 为 `null` |

错误码：`3001`

**25) POST `/exams/{id}/abandon`** → 置 `ABANDONED`，**不计入** `stat_user_subject.exam_count`。

## 5.7 错题本与收藏

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | :-: | --- | --- |
| 26 | GET | `/wrong-questions` | USER | 错题列表 |
| 27 | DELETE | `/wrong-questions/{questionId}` | USER | 移出错题本（置 `REMOVED`） |
| 28 | POST | `/wrong-questions/{questionId}/restore` | USER | 恢复到错题本 |
| 29 | POST | `/favorites` | USER | 收藏 |
| 30 | DELETE | `/favorites/{questionId}` | USER | 取消收藏 |
| 31 | GET | `/favorites` | USER | 收藏列表 |

**26) GET `/wrong-questions`** 参数：`subjectId`（必填）、`status`（默认 `ACTIVE`，可选 `MASTERED`/`REMOVED`）、`outlineId?`、`kpId?`、`sortBy`（`wrongCount`/`lastAnsweredAt`，默认 `lastAnsweredAt`）、分页。
`data` 分页项字段：`{questionId, stem摘要(stemSummary), outlinePath, wrongCount, correctCount, consecutiveCorrect, lastAnsweredAt, status}`

**31) GET `/favorites`** 参数：`subjectId`、分页；`data` 为 `QuestionVO` 分页。

## 5.8 学习统计

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | :-: | --- | --- |
| 32 | GET | `/stats/overview` | USER | 总览 |
| 33 | GET | `/stats/trend` | USER | 每日趋势 |
| 34 | GET | `/stats/outline` | USER | 章节掌握度 |
| 35 | GET | `/stats/weak-kp` | USER | 薄弱知识点 |

**32) GET `/stats/overview`** 参数：`subjectId?`（缺省用 `currentSubjectId`）
`data`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `answeredCount` / `correctCount` / `accuracy` | int/int/decimal | 累计口径 |
| `studyDays` | int | 累计有作答的天数 |
| `continuousDays` | int | 连续学习天数（含今天或昨天为最近端点） |
| `mastery` | decimal | 科目掌握度 |
| `examCount` / `bestScore` | int/decimal? | 模考次数与最高分 |
| `outlineCoverage` | decimal | 已练习章节数 / 总叶子章节数 |
| `todayAnswered` / `todayAccuracy` | int/decimal | 今日数据 |

**33) GET `/stats/trend`** 参数：`subjectId?`、`days`（默认 30，上限 90）
`data`：`[{ "date": "2026-09-25", "answeredCount": 30, "correctCount": 24, "accuracy": 0.8, "durationSec": 1800 }]`（缺失日期补 0）

**34) GET `/stats/outline`** → `data`：`OutlineNodeVO[]`，每个节点额外含 `answeredCount`、`accuracy`、`mastery`。

**35) GET `/stats/weak-kp`** 参数：`subjectId?`、`limit`（默认 10，上限 30）、`maxMastery`（默认 0.6）
`data`：`[{ "kpId", "kpName", "answeredCount", "accuracy", "mastery" }]`，按 `mastery` 升序。

## 5.9 题目反馈

| # | 方法 | 路径 | 权限 | 说明 |
| --- | --- | :-: | --- | --- |
| 36 | POST | `/questions/{id}/feedback` | USER | 提交反馈 |

请求：`{ "type": string, "content": string }`（`content` 5~1000 字；`type` 见 02 文档 2.6.3）
响应：`data: null`。同一用户对同一题 24 小时内最多提交 3 次（Redis 限流，超限返回 `1002`）。

## 5.10 管理端 · 内容维护

| # | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 37 | GET | `/admin/subjects` | 科目列表（含 `DISABLED`） |
| 38 | POST | `/admin/subjects` | 新增科目 |
| 39 | PUT | `/admin/subjects/{id}` | 修改科目 |
| 40 | PATCH | `/admin/subjects/{id}/status` | 启用/停用 |
| 41 | GET | `/admin/outline` | 章节树（`subjectId` 必填） |
| 42 | POST | `/admin/outline` | 新增章节 `{subjectId, parentId, name, sortNo, origin}` |
| 43 | PUT | `/admin/outline/{id}` | 修改章节 |
| 44 | DELETE | `/admin/outline/{id}` | 删除章节（有题目引用时返回 `1005`） |
| 45 | PUT | `/admin/outline/sort` | 批量排序 `[{id, parentId, sortNo}]` |
| 46 | GET | `/admin/knowledge-points` | 知识点列表 |
| 47 | POST | `/admin/knowledge-points` | 新增 |
| 48 | PUT | `/admin/knowledge-points/{id}` | 修改（含 `aliases`） |
| 49 | DELETE | `/admin/knowledge-points/{id}` | 删除（有题目引用时返回 `1005`） |
| 50 | POST | `/admin/knowledge-points/merge` | 合并标签 `{sourceIds[], targetId}` |
| 51 | GET | `/admin/paper-templates/{subjectId}` | 读取卷面模板（含场次与分节） |
| 52 | PUT | `/admin/paper-templates/{subjectId}` | 更新模板（含场次与分节，整体覆盖式提交） |
| 53 | POST | `/admin/files` | 上传图片（multipart，字段名 `file`） |
| 54 | GET | `/admin/configs` / PUT | 系统配置读写 |

**38) POST / 39) PUT `/admin/subjects[/{id}]`** 请求体（字段与 `SubjectVO` 同源，`id` 由路径给出）：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | :-: | --- |
| `level` | string | ✔ | `JUNIOR`/`MIDDLE`/`SENIOR`，创建后**不可改**（科目级别决定卷面类型，改它会架空已有模板） |
| `code` | string | ✔ | 科目代码，全局唯一；重名返回 `1001` |
| `name` | string | ✔ | 科目名 |
| `paperType` | string | ✔ | `JUNIOR_MID`/`SENIOR` |
| `examPeriod` | string | ✔ | `BOTH`/`FIRST_HALF`/`SECOND_HALF` |
| `outlineStatus` | string | ✔ | `FULL`/`SKELETON` |
| `contentStatus` | string | ✔ | `SAMPLE`/`BULK` |
| `intro` | string? | | 简介 |

响应 `data`：`SubjectVO`；错误码 `1001`、`1004`。写 `sys_operation_log`（`module = SUBJECT`，`CREATE`/`UPDATE`）。

**40) PATCH `/admin/subjects/{id}/status`** 请求体：`{ "status": "ENABLED" | "DISABLED" }`。

> **这是 P2 引入的第一个 AC-2 高危操作**：`DISABLED` 即"下架"，考生端 `GET /subjects` 与详情随即不可见，已有模拟考会话的引用不受影响。必须写 `sys_operation_log`（`module = SUBJECT`，`action = DISABLE`/`ENABLE`，`targetId` = 科目 ID）。响应 `data`：`null`；错误码 `1001`、`1004`、`3001`。

**44) DELETE `/admin/outline/{id}` / 49) DELETE `/admin/knowledge-points/{id}`**：**有题目引用时返回 `1005`**（`STATUS_NOT_ALLOWED`）——不级联删除、不静默置空题目上的 `outlineId`。理由：题目一旦挂了章节，删除章节就等于让题目的"章节路径"（`QuestionVO.outlinePath`）指向不存在的位置，这是数据损坏而非校验失败，所以宁可直接拒绝并让管理员先迁移题目。同样写 `sys_operation_log`（`module = SUBJECT`，`DELETE`）。

**53) POST `/admin/files`**
- 限制：`jpg/jpeg/png/gif/webp`，单文件 ≤ 5MB。
- 存储：服务器本地目录（由环境变量 `RK_UPLOAD_DIR` 指定，默认 `deploy/data/uploads`），Nginx 以 `/uploads/` 路径静态托管（见 09 文档）。
- 响应：`{ "url": "/uploads/2026/09/25/8f2c1a9b.png", "size": 204800 }`

> 决策与理由
> 不使用对象存储（OSS/S3）。理由：单机部署下，本地磁盘 + Nginx 静态托管零成本、零外部依赖、无网络延迟，且图片总量估算 < 500MB。缺点是无法多机共享，与"单机 VPS"假设一致。若将来扩容到多机，把 `RK_UPLOAD_DIR` 挂载到共享存储或改为 OSS 即可，接口契约不变。

**52) PUT `/admin/paper-templates/{subjectId}`**

> 请求体为**场次（sitting）→ 分节（section）**两层结构（对应评审 A5）。模板级不再有 `totalDuration`（已随 `exam_paper_template.total_duration` 从 DDL 删除，总时长下沉至各场次的 `totalDuration`）。

示例一：**软件设计师（初级/中级模板，1 个场次）**

```json
{
  "name": "软件设计师·模拟卷",
  "paperType": "JUNIOR_MID",
  "totalMaxScore": 150,
  "autoScoredMaxScore": 75,
  "sittings": [
    {
      "sittingNo": 1,
      "sittingName": "上午连考",
      "totalDuration": 240,
      "totalMaxScore": 150,
      "autoScoredMaxScore": 75,
      "sections": [
        { "sectionNo": 1, "sectionName": "基础知识", "sectionType": "OBJECTIVE", "questionType": "SINGLE_CHOICE",
          "questionCount": 75, "perQuestionScore": 1, "durationMin": 90, "durationMax": 120,
          "allowEarlySubmit": true, "supportStatus": "SUPPORTED", "sortNo": 1 },
        { "sectionNo": 2, "sectionName": "应用技术", "sectionType": "SUBJECTIVE", "questionType": "SUBJECTIVE",
          "questionCount": 5, "perQuestionScore": 15, "durationMin": 120, "durationMax": 150,
          "allowEarlySubmit": true, "supportStatus": "SUPPORTED", "sortNo": 2 }
      ]
    }
  ]
}
```

示例二：**系统架构设计师（高级，2 个场次；论文在下午独立场次）**

```json
{
  "name": "系统架构设计师·模拟卷",
  "paperType": "SENIOR",
  "totalMaxScore": 225,
  "autoScoredMaxScore": 75,
  "sittings": [
    {
      "sittingNo": 1,
      "sittingName": "上午连考",
      "totalDuration": 240,
      "totalMaxScore": 150,
      "autoScoredMaxScore": 75,
      "sections": [
        { "sectionNo": 1, "sectionName": "综合知识", "sectionType": "OBJECTIVE", "questionType": "SINGLE_CHOICE",
          "questionCount": 75, "perQuestionScore": 1, "durationMin": 120, "durationMax": 150,
          "allowEarlySubmit": true, "supportStatus": "SUPPORTED", "sortNo": 1 },
        { "sectionNo": 2, "sectionName": "案例分析", "sectionType": "SUBJECTIVE", "questionType": "SUBJECTIVE",
          "questionCount": 3, "perQuestionScore": 25, "durationMin": 90, "durationMax": 120,
          "allowEarlySubmit": true, "supportStatus": "SUPPORTED", "sortNo": 2 }
      ]
    },
    {
      "sittingNo": 2,
      "sittingName": "下午论文",
      "totalDuration": 120,
      "totalMaxScore": 75,
      "autoScoredMaxScore": 0,
      "sections": [
        { "sectionNo": 3, "sectionName": "论文", "sectionType": "ESSAY", "questionType": "ESSAY",
          "questionCount": 1, "perQuestionScore": 75, "durationMin": 120, "durationMax": 120,
          "allowEarlySubmit": false, "supportStatus": "SUPPORTED", "sortNo": 1 }
      ]
    }
  ]
}
```

校验规则（服务端强制，违反返回 `1001`）：
1. `sittings` 的 `sittingNo` 从 1 连续递增；每个 sitting 内 `sections` 的 `sectionNo` **全局连续递增**（跨 sitting 连续，如高级为 `1/2/3`）。
2. `durationMin`、`durationMax` **均必填**，且 `durationMin ≤ durationMax`（对应评审 A6）。
3. **`Σ(durationMin) ≤ sitting.totalDuration ≤ Σ(durationMax)`**（对应评审 A6）—— 场次内总时长共享，分节时长上下限只约束"最早/最晚可离开"。
4. `sitting.autoScoredMaxScore` = 该场次内 `sectionType = OBJECTIVE` 分节分值之和。
5. 模板级 `autoScoredMaxScore` = Σ 各场次 `autoScoredMaxScore`；模板级 `totalMaxScore` = Σ 各场次 `totalMaxScore`；`NOT_SUPPORTED` 分节**不计入任何满分口径**。
6. 同一模板中 `questionType` 为 `SINGLE_CHOICE` 的分节，题目数不得超过该科目已发布单选题量（否则保存时给出警告字段 `insufficientQuestions`，但不阻断保存）。

**54) GET|PUT `/admin/configs`**（对应评审 C1，补齐字段表）

`GET` 无参数，响应 `data`：`sys_config` 全量键值。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `configKey` | string | 配置键（见 04 §4.8.3） |
| `configValue` | string | 配置值（**统一以字符串传输**，前端按 `remark` 语义解析） |
| `remark` | string? | 说明 |
| `updatedAt` | string | 最后更新时间 |

`PUT` 请求：`{ "items": [{ "configKey": string, "configValue": string }] }`（**仅允许更新白名单内的键**，未知键返回 `1001`）。

响应 `data`：`null`。错误码：`1001`、`1004`。
> `PUT` 为高危操作，必须写 `sys_operation_log`（`module = CONFIG`，约束 AC-2）。

## 5.11 管理端 · 题库与审核

| # | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 55 | GET | `/admin/questions` | 列表筛选 |
| 56 | GET | `/admin/questions/{id}` | 详情（全字段，含答案与解析） |
| 57 | POST | `/admin/questions` | 新增题目 |
| 58 | PUT | `/admin/questions/{id}` | 编辑（`version` +1） |
| 59 | DELETE | `/admin/questions/{id}` | 删除（仅 `DRAFT`/`REJECTED` 可删） |
| 60 | POST | `/admin/questions/{id}/submit-review` | 提交审核 `DRAFT → PENDING_REVIEW` |
| 61 | POST | `/admin/questions/{id}/review` | 裁决 `{action: APPROVE\|REJECT, remark}` |
| 62 | POST | `/admin/questions/batch-review` | 批量裁决 `{ids[], action, remark}`（上限 100） |
| 63 | POST | `/admin/questions/{id}/regenerate` | 退回重生成 `REJECTED → DRAFT` |
| 64 | POST | `/admin/questions/{id}/publish` | 发布 `APPROVED → PUBLISHED` |
| 65 | POST | `/admin/questions/{id}/offline` | 下架 `PUBLISHED → OFFLINE` |
| 66 | POST | `/admin/questions/import` | 批量导入 JSONL 产出 |
| 67 | GET | `/admin/batches` | 生成批次列表 |
| 68 | GET | `/admin/batches/{id}` | 批次详情（含产出题目统计） |
| 69 | POST | `/admin/batches` | 创建批次（状态 `PENDING`，供离线脚本领取） |
| 70 | PUT | `/admin/batches/{id}/status` | 脚本回写状态与计数 |
| 71 | GET | `/admin/feedback` | 反馈列表 |
| 72 | POST | `/admin/feedback/{id}/handle` | 处理反馈 `{status, handleRemark, offlineQuestion: bool}` |
| 73 | GET | `/admin/dashboard` | 数据概览 |

**55) GET `/admin/questions`** 参数：`subjectId?`、`status?`、`questionType?`、`outlineId?`、`kpId?`、`sourceType?`、`batchId?`、`keyword?`（题干模糊）、`pageNum`、`pageSize`。
`data` 分页项：`{id, subjectName, outlinePath, questionType, stemSummary, difficulty, status, sourceType, batchNo, updatedAt}`

**57/58) 请求体（新增/编辑）**

```json
{
  "subjectId": "6",
  "outlineId": "102",
  "questionType": "SINGLE_CHOICE",
  "stem": "在操作系统中，…… 正确的是 $O(1)$",
  "images": [{ "url": "/uploads/2026/09/25/a.png", "alt": "图示" }],
  "options": [{ "key": "A", "content": "..." }, { "key": "B", "content": "..." }],
  "correctAnswer": "B",
  "referenceAnswer": null,
  "analysis": "...",
  "scorePoints": null,
  "difficulty": 3,
  "kpIds": ["11", "12"],
  "sourceType": "ORIGINAL",
  "sourceRef": "人工编写",
  "sourceLicense": "SELF",
  "generationBatchId": null
}
```

服务端校验：
1. `questionType` 为客观题时，`options` 至少 2 项、`correctAnswer` 必须存在于 `options.key` 中；`MULTI_CHOICE` 的 `correctAnswer` 按字典序排列（服务端自动规范化）。
2. `questionType` 为 `SUBJECTIVE`/`ESSAY` 时，`correctAnswer` 必须为 `null`，`referenceAnswer` 必须非空。
3. `kpIds` 数量 **0~5**（**保存/编辑时不强制至少 1 个**，见下方"发布前校验"，对应评审 B4）。
4. `sourceType` 必填（约束 C1）；为 `AI_GENERATED` 时必须提供 `generationBatchId`（示例中 `generationBatchId` 字段以 `null` 出现，对应评审 C9）。
5. 重复检测：题干归一化（去空白、去标点、转小写）后与同科目同章节已有题目比对，完全相同返回 `3005`；相似度 ≥ 0.9 返回 `3005` 并附 `data.similarQuestionId`（可带 `force=true` 覆盖）。

> **`kpIds` 为何允许 0 个（对应评审 B4）**：[08-ai-pipeline.md](08-ai-pipeline.md) 允许"标签匹配不上时先 0 个暂挂（`DRAFT`），之后再人工补挂"。若保存动作就强制 1~5 个，这批题一打开编辑页就保存不了，08 的闭环断裂。因此把"至少 1 个"**下沉为发布动作的校验**（见 `64`）。

**61) POST `/admin/questions/{id}/review`** → 成功返回 `{id, status, reviewedAt}`；状态非 `PENDING_REVIEW` 返回 `3003`。
> `remark` 长度：**服务端上限 500**（`rk_question_review_log.remark` 为 `VARCHAR(500)`），**前端表单限 200 字**（07 §7.5.2）。这是**有意收紧**（前端更严），非缺陷，勿按"不一致"修正（对应评审 B14）。

**64) publish** → 发布前校验（违反返回 `1005`，对应评审 B4、B7）：
- **`kpIds` 至少 1 个**（0 个标签的题可保存为 `DRAFT`，但不可发布）；
- **`source_type` / `source_ref` / `source_license` / `generation_batch_id` 四字段非空**（对应约束 C1 与评审 B7，DDL 不设防，由应用层守，与 [08-ai-pipeline.md](08-ai-pipeline.md) §8.2.3 的表述一致）。

发布后同步更新 `rk_subject.question_count`、`rk_outline_node.question_count`、`rk_knowledge_point.question_count`。
**65) offline** → 同步递减上述计数；已下架题目不出现在任何新组卷中，但历史 `exam_session_item` 快照不受影响。
**66) import** → 请求体为 JSONL 文本（`Content-Type: text/plain`，单次 ≤ 500 行），每行一条题目 JSON，Schema 见 [08-ai-pipeline.md](08-ai-pipeline.md#84-输出-schema)；导入后全部为 `DRAFT`，返回 `{imported, skipped, errors:[{line, reason}]}`。

**67) GET `/admin/batches`**（对应评审 C1，补齐字段表）

参数：`subjectId?`、`status?`（`PENDING`/`GENERATING`/`GENERATED`/`REVIEWING`/`COMPLETED`/`FAILED`）、`pageNum`、`pageSize`。

响应 `data`（分页包装）：`list` 项字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | string | 批次 ID |
| `batchNo` | string | 批次编号 |
| `subjectId` / `subjectName` | string | |
| `questionType` | string | 目标题型 |
| `targetCount` | int | 目标题量 |
| `difficulty` | int? | 目标难度 |
| `model` / `promptVersion` | string | 模型与 Prompt 版本 |
| `status` | string | 见 02 §2.5.3 |
| `producedCount` / `approvedCount` / `rejectedCount` | int | 产出 / 通过 / 驳回计数 |
| `failReason` | string? | `FAILED` 时的原因 |
| `createdAt` / `finishedAt` | string? | |

错误码：`3001`

`68) GET /admin/batches/{id}` 在以上字段基础上增加 `outlineIds`（`string[]`，生成输入的唯一内容来源，用于 GR-1 证据链）。

**73) GET `/admin/dashboard`**（对应评审 C1，补齐字段表）

无参数，响应 `data`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `totalQuestions` | int | 全站已发布题目总数（`PUBLISHED`） |
| `pendingReviewQuestions` | int | 待审核题目数（`PENDING_REVIEW`） |
| `pendingFeedbacks` | int | 待处理反馈数（含 `COPYRIGHT` 高优先级计数 `copyrightFeedbacks`） |
| `copyrightFeedbacks` | int | 待处理的版权投诉反馈数 |
| `activeBatches` | int | 进行中的生成批次数（`PENDING`/`GENERATING`/`GENERATED`/`REVIEWING`） |
| `totalUsers` | int | 注册用户总数 |
| `dailyActiveUsers` | int | 今日活跃用户数 |
| `dailyAnswerCount` | int | 今日全站作答数 |
| `questionTrend` | object[] | 近 7 天 `[{date, importedCount, publishedCount}]` |
| `subjectDistribution` | object[] | `[{subjectId, subjectName, questionCount}]` |

错误码：`5001`（暂无统计数据）

## 5.12 管理端 · 用户

| # | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 74 | GET | `/admin/users` | 列表（`keyword` 匹配用户名/昵称） |
| 75 | PATCH | `/admin/users/{id}/status` | 禁用/启用 `{status}` |
| 76 | POST | `/admin/users/{id}/reset-password` | 重置为随机密码，响应中返回一次明文 |

**74) GET `/admin/users`**（对应评审 C1，补齐字段表）

参数：`keyword?`（匹配用户名/昵称）、`role?`（`USER`/`ADMIN`）、`status?`（`ACTIVE`/`DISABLED`）、`pageNum`、`pageSize`。

响应 `data`（分页包装）：`list` 项字段即 `UserVO`（见 5.3），另附 `questionAnsweredCount`（该用户累计作答数，用于管理端概览）。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` / `username` / `nickname` / `role` / `status` | string | |
| `currentSubjectId` | string? | |
| `mustChangePassword` | boolean | 见 5.3 |
| `lastLoginAt` / `createdAt` | string | |
| `questionAnsweredCount` | int | 累计作答数 |

错误码：`1001`

> `76` 重置密码后，该用户 `mustChangePassword` 置 `true`（下次登录强制改密）。

> 高危操作（`DELETE`、`offline`、`reset-password`、`DISABLE`）必须写 `sys_operation_log`（约束 AC-2）。

## 5.13 鉴权白名单

以下路径**无需登录**，其余全部需要：

```
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/subjects/**
GET  /api/v1/subjects/{id}/outline
GET  /api/v1/subjects/{id}/knowledge-points
GET  /doc.html、/webjars/**、/v3/api-docs/**   （Knife4j，生产环境需由配置开关关闭）
```

> **`POST /api/v1/practice/papers` 不在本白名单内**（对应评审 B6，安全洞）：该路径标注 `@SaIgnore`，但**游客资格在方法体内校验**（仅 `mode=SAMPLE` 且 `count ≤ 5` 时放行，否则抛 `2005`）。**禁止把它加入白名单常量** —— 白名单只能表达路径级放行，一旦写入，游客即可用 `mode=FULL` 绕过题量限制。详见 `5.5 · 12)`。

`/api/v1/admin/**` 全部要求 `role = ADMIN`。
> 该白名单必须在 `rk-api` 的鉴权单元测试中与代码注解双向核对（见 ADR-02）：白名单外的 Controller 方法若未标注 `@SaCheckLogin`/`@SaCheckRole`，测试失败。
> **测试要求**：未登录 + `POST /api/v1/practice/papers` 且 `mode=FULL` → 返回 `2005`（对应评审 B6）。