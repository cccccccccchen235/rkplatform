# 31 · P5 模拟机考（考生端三页）

> 本轮做 [30-p5-exam-backend.md](30-p5-exam-backend.md) §6 的**第 1 项**：考生端三页
> （模拟考中心 `/exam`、答题页 `/exam/{id}`、成绩详情 `/exam/{id}/result`）与支撑它所需的
> 前端 store / 类型 / 组件 / 导航入口。
>
> **未包含**：§6 第 2 项（管理端卷面模板配置，05 §5.10 第 51/52 条）与第 3 项
> （`images_snapshot`、`repeatedCount`——都要求先改 04/05，见 30 的偏离 4/7）。

## 1. 交付了什么

| 层 | 内容 |
| --- | --- |
| 路由 | `/exam`（模拟考中心）、`/exam/{sessionId}`（答题页，`BlankLayout`）、`/exam/{sessionId}/result`（成绩详情）；三条均 `requiresAuth` |
| 页面 | `views/portal/exam-center.vue`、`exam-session.vue`、`exam-result.vue` |
| store | `stores/exam.ts`：发起 / 断点续答 / 作答保存（5s 防抖 + 60s 心跳 + 分批 ≤100）/ 服务端计时校准 / 场次与分节切换 / 交卷 / 放弃 |
| 组件 | `components/exam/` 四个：`ExamQuestionCard`（答题，无答案）、`EssayEditor`（论文/主观题，草稿 + 字数 + 预览 + 导出）、`ExamAnswerSheet`（答题卡）、`ExamSectionTip`（`NOT_SUPPORTED` / 论文节提示） |
| 接口 | `api/exam.ts`：05 §5.6 的十个端点（16~25） |
| 类型 | `types/api.d.ts` 增补 `ExamSessionVO` / `SittingVO` / `SectionVO` / `ExamItemVO` / `ExamResultVO` / `ExamHistoryVO` 等 13 个类型 |
| 复用改造 | `QuestionCard.vue` 新增 `mode='result'`（成绩回顾展示答案与解析、隐藏收藏/反馈）；`PortalLayout` 顶栏加「模拟考」；`subject-detail` 卷面结构加「发起模拟考」 |

## 2. 关键口径

- **计时以服务端为准（C4）**：本地 `setInterval(1000)` 只推进 `tickAt`，`displaySec` 由 getter
  现算；`remainingSec` 只被 `GET` / `PUT answers` / `start` / `finish` / `advance` 的响应覆盖。
  剩余归零**不本地交卷**，只发一次保存，由服务端 `4005` 决定落点（07 §7.4.4 第 5 条）。
- **PENDING 场次不倒计时（A5）**：高级科目下午论文场次显示「开始本场次」按钮，午休不扣时。
- **心跳（B10）**：无变更时每 60s 用空 `items` 调 `PUT /exams/{id}/answers`，刷新 `remainingSec`
  并给服务端一次超时结算机会；切回前台（`visibilitychange`）再补一次。
- **保存状态常驻**：`保存中… / 已保存 hh:mm:ss / 保存失败，点击重试`；失败保留 `dirty`，
  连续 3 次失败只提示不阻断本地作答。
- **答题卡**：只列当前场次的分节；已完成分节可点进只读查看，未开始分节与 `NOT_SUPPORTED`
  整组禁用；「结束本节」只出现在服务端当前分节且 `allowEarlySubmit = true` 时（论文节无此按钮）。

## 3. 偏离登记

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | `POST /exams` 返回 `4003` 时**不带 `sessionId`**（05 §5.6 第 16 条只定义了错误码，`BizException` 无 data 载荷），而 `IN_PROGRESS` 会话又不在历史列表里（第 17 条）。前端改为在发起成功时把会话记进 `localStorage`（`rk:exam:active`），4003 时读出来弹「继续 / 放弃」对话框 | 07 §7.4.4 要求对话框显示 `sessionId` 并支持两个动作；在「01~26 不回改」的前提下，本地记录是唯一可行的找回路径 | 同一浏览器可正常找回；**换设备/清缓存后拿不到**，此时对话框如实说明只能在 6 小时窗口期后等自动结算。若要让服务端直接给出 `sessionId`，需先改 05 §5.6 第 16 条的 4003 定义 |
| 2 | 考试期间主观题/论文**不给参考答案**，只给写作框；参考答案只在成绩页出现 | `ExamItemVO` 本就不含 `referenceAnswer`，且考试中下发答案等于送卷（05 §5.6 第 18 条的设计意图） | 与后端契约一致；论文写作框带 localStorage 草稿（`rk:essay:{sessionId}:{itemId}`），刷新/崩溃不丢未提交内容 |
| 3 | 成绩页逐题回顾是**顺序列表**，不按分节分组 | `ExamResultVO.items` 是 `QuestionDetailVO[]`，其中**没有 `sectionNo`**，无法从数据上恢复分节边界；分节维度的信息由 `sectionResults` 表格承担 | 逐题回顾能看到题干/作答/答案/解析，但不显示"这题属于哪一节"；若要分组，需给结果项加 `sectionNo`（改 05） |
| 4 | 30 的偏离 4（`exam_session_item` 无 `images` 快照列）在答题页同样成立：`ExamItemVO.images` 恒为空数组 | 未改 DDL | 一旦题库出现带图题，考试答题页会丢图；`images_snapshot` 已登记为下一步前置 |

## 4. 验证

| 项 | 结果 |
| --- | --- |
| `pnpm -C web type-check` | 通过（0 错误） |
| `pnpm -C web lint` | 通过 |
| `pnpm -C web build` | 通过（仅有既有的 chunk > 500KB 提示与 Sass 弃用提示，非本轮引入） |
| 真 HTTP 端到端（真后端 8080 + MySQL + Redis） | 用测试用户跑通：`POST /exams` → `POST /sittings/1/start` → `PUT /answers`（1 条作答）→ `PUT /answers`（空 items 心跳）→ `GET /exams/{id}`（断点续答读回 `userAnswer=A`）→ `POST /submit` → `GET /result`；逐字段核对了与 `types/api.d.ts` 的一致性 |
| 关键返回值核对 | `start`：`{sitting, remainingSec:14401, currentSectionNo:1}`；`save`/心跳：`{remainingSec:14400, currentSittingNo:1, currentSectionNo:1}`；`result`：`objectiveScore:1.0 / autoScoredMaxScore:75 / totalMaxScore:75 / accuracy:0.0133`；`sittingResults[0].answered:true`；`sectionResults` 中 `OBJECTIVE scored:true`、`SUBJECTIVE scored:false`；`items[0]` 含 `correctAnswer:A / isCorrect:true / userAnswer:A / outlinePath:[]` |
| **未做** | 三页的 CDP 截图取证。本轮环境的策略**禁止启动 Chrome**（`Start-Process chrome.exe` 被拦），所以「人眼过一遍」这一条没做——type-check / build / 真 HTTP 只能证明代码可编译、契约对齐，**不能证明渲染无异常**。这是本轮最大的未验证项，待能起浏览器时补 `capture.js` / `shot-pages.js` |

> 副作用：测试用户 `negctl_81217` 新增 1 个 `SCORED` 会话（id=6，用于成绩页字段取证）与其派生流水；
> CDP 脚本未跑成，故未新增临时 `IN_PROGRESS`/`ABANDONED` 会话。生产前用 `deploy/sql` 重建即可。

## 5. 文件

新增：`web/src/api/exam.ts`、`web/src/stores/exam.ts`、
`web/src/components/exam/{ExamQuestionCard,EssayEditor,ExamAnswerSheet,ExamSectionTip}.vue`、
`web/src/views/portal/{exam-center,exam-session,exam-result}.vue`。

修改：`types/api.d.ts`（模拟考类型）、`types/labels.ts`（场次/分节状态文案）、
`utils/storage.ts`（`exam:active` key）、`components/question/QuestionCard.vue`（`mode='result'`）、
`router/portal.routes.ts`（三条路由）、`layouts/PortalLayout.vue`（导航）、
`views/portal/subject-detail.vue`（发起入口）。

## 6. 下一步

1. **管理端卷面模板配置**（05 §5.10 第 51/52 条）：后端目前**没有** `/admin/paper-templates/{subjectId}`
   的读写接口（只有 `PaperTemplateMapper` 被组卷复用），需要补 Controller + Service + 校验
   （`autoScoredMaxScore` 与客观题分节之和不符即保存失败；IPT「应用技术」`NOT_SUPPORTED`），
   再加前端页面与路由 `/admin/paper-templates`。这是 10 §10.7 前两条验收项的前置。
2. 三页的浏览器渲染取证（需能启动 Chrome 的环境）。
3. `images_snapshot`（30 偏离 4）与 `repeatedCount` 可观测性（30 偏离 7）——都要先改 04/05。
