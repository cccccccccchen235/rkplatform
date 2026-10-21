# 32 · P5 管理端卷面模板配置

> 本轮做 [30 §6](30-p5-exam-backend.md) 第 2 项 = [31 §6](31-p5-exam-frontend.md) 第 1 项：
> 05 §5.10 第 **51/52** 条的读写接口（`GET` / `PUT /admin/paper-templates/{subjectId}`）
> 与管理端页面 `/admin/paper-templates`。
> 这是 [10 §10.7](10-roadmap.md) 前两条验收项（"满分口径不符即保存失败"、"IPT 应用技术 NOT_SUPPORTED 不计入满分"）的前置。
>
> **未包含**：`images_snapshot`（30 偏离 4）与 `repeatedCount` 可观测性（30 偏离 7）——两者都要求先改 04/05；
> 以及三页 + 本页的浏览器渲染取证（本机策略仍禁止启动 Chrome，同 `31`）。

## 1. 交付了什么

| 层 | 内容 |
| --- | --- |
| 接口 | `GET /api/v1/admin/paper-templates/{subjectId}`（全字段读取）、`PUT`（**整体覆盖式**保存，返回 `insufficientQuestions` 警告） |
| 后端 | DTO `PaperTemplateSaveRequest`（场次 → 分节两层）；VO `PaperTemplateDetailVO` / `PaperTemplateSaveResultVO`；`PaperTemplateAdminService(+Impl)`；`AdminPaperTemplateController`；`PaperTemplateMapper` 补 7 个读写方法 |
| 校验 | 05 §5.10 第 52 条的五条规则**全部服务端强制**（序号连续 / 时长自洽 / 满分口径一致）；题量不足只警告不阻断 |
| 前端 | `views/admin/paper-template-config.vue`（科目选择 + 两层编辑 + 序号与满分只读派生 + 蓝图填充 + 即时校验 + 警告展示）；`api/admin/paper-template.ts`；路由 `/admin/paper-templates`（管理端菜单第 4 项）；`types/api.d.ts` 增补 6 个类型 |
| 验证 | `scripts/verify/p5-paper-template-admin.js`：24 条断言，含 3 个否定用例、IPT 反证、幂等回填，**结束时按快照还原**被改的模板 |

## 2. 关键口径

- **满分是"比对"而不是"重算"**：提交的 `sitting.totalMaxScore` /
  `sitting.autoScoredMaxScore` / 模板级两个合计，都要与"按分节分值算出来的值"相等，
  不等即 `1001`——10 §10.7 第一条验收项要的就是"保存失败"，静默改写等于把校验关掉。
- **`NOT_SUPPORTED` 在满分口径里按 0 计，在时长口径里照常参与**：
  它的 `questionCount`/`perQuestionScore` 不进入任何满分（02 §2.4.4），
  但 `durationMin/durationMax` 仍参与 `Σ(min) ≤ sitting.totalDuration ≤ Σ(max)`
  （种子数据里 IPT 的「应用技术」就是时长 120~150、分值 0，见 04 §4.8.2）。
- **`GET` 的响应可直接作为 `PUT` 的请求体**（多出的 `subjectId` / `totalDuration` 是只读派生字段，
  被服务端忽略）；覆盖式写入因此是幂等的：连续两次提交同一份数据，回读逐字节一致，
  场次/分节行数不累积（脚本用"两次写入后数量不变"钉住这一点）。
- **无模板则创建**：科目没有模板时 `PUT` 走 `insertTemplate` 分支（`status = ENABLED`），
  有则整体替换；`GET` 读不到模板返回 `4001`，页面据此进入"按蓝图新建"引导而不是报错。
- **写接口同时清科目列表缓存**：`/subjects` 列表里的 `paperTotalScore` 来自模板表，
  不清缓存会出现"管理端改完，考生端最长一小时看不到"（与本项目其它写接口同规矩）。
- **审计**：`module = SUBJECT`、`action = UPDATE`、`targetType = PAPER_TEMPLATE`（AC-2）。
  模板配置不是"内容维护"三件套，但同属影响考生可见数据的高危写操作。

## 3. 偏离登记

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | 05 §5.10 只给了 51/52 的路径与请求体，**没定义错误码**。本实现：科目不存在 `3001`、尚未配置 `4001`、校验失败 `1001` | 复用既有码，不与 `05 §5.1.2` 冲突；`4001` 让页面能区分"新建"与"读取失败" | 若将来 05 补定义且取值不同，改这一处 |
| 2 | `insufficientQuestions` 的**形状** 05 未定义。本实现：`[{sectionNo, sectionName, requiredCount, availableCount}]` | 前端要能指出"是哪个分节、差多少"，只给一个布尔值无法渲染提示 | 与 05 无冲突，但属于本实现自定契约 |
| 3 | 05 规则 4/5 字面只要求"场次 `autoScoredMaxScore` = 客观题节之和""模板级 = Σ 场次"。本实现**一并强制** `场次 totalMaxScore = 其分节分值之和` | 不这样做，IPT 的 `NOT_SUPPORTED` 不计入 `totalMaxScore` 在服务端就落不了地（10 §10.7 第二条验收项） | 场次合计与分节不一致的提交会被拒（这正是想要的） |
| 4 | 07 把 `sortNo` 列为可编辑字段，本实现做成**只读派生**（各层 `(index+1)*10`） | 排序只影响展示（读取按 `sitting_no`/`section_no`）；派生可避免"手改出重复序号"导致读取顺序不稳定 | 与 07 的字面描述不同；行为上更严格 |
| 5 | 模板整体覆盖会把 `status` 置回 `ENABLED` | 51/52 没有单独的模板启停接口，配置完仍是停用会让考生端拿不到模板 | 无（当前无停用模板的数据） |
| 6 | 题量警告**只对 `SUPPORTED` 的 `SINGLE_CHOICE` 分节**做 | `NOT_SUPPORTED` 分节不参与组卷，为它报警告是噪音 | 与 05 规则 6 的字面范围略有收窄 |
| 7 | 管理端页面与 P5 三页的**浏览器渲染取证未做** | 本机策略禁止 `Start-Process chrome.exe`（同 `31`） | type-check / build / 真 HTTP 只能证明可编译与契约对齐，不能证明渲染无异常 |

## 4. 验证

| 项 | 结果 |
| --- | --- |
| `mvn -pl rk-api -am test` | 通过（含 `AuthAnnotationTest` 的类级注解识别、`ParamValidationTest` 的"每个控制器必须标 `@Validated`"） |
| `pnpm -C web type-check` / `lint` / `build` | 通过；`paper-template-config` 独立 chunk 14.89 kB（路由懒加载生效） |
| `node scripts/verify/p5-paper-template-admin.js` | **24 通过 / 0 失败** |
| ↳ 否定用例 | `autoScoredMaxScore` 多 1 → `1001`；场次时长 999 与分节上下限不自洽 → `1001`；分节断号（3→4）→ `1001` |
| ↳ IPT 反证 | 把「应用技术」改成 `SUPPORTED`(5×15) 但仍提交 `totalMaxScore=75` → `1001`；保持 `NOT_SUPPORTED`(0 分) → `code=0` 且回读 `totalMaxScore=75` |
| ↳ 警告 | SAD 基础知识的 `questionCount` 设 999 → `code=0` 且 `insufficientQuestions` 命中分节 1（`requiredCount=999` + `availableCount`） |
| ↳ 幂等 | `GET` 响应回填 `PUT` 一次、两次后都与原快照逐字节一致，场次/分节数量不变 |
| ↳ 边界 | 不存在科目 → `3001`；数据还原后库回到 20 模板 / 25 场次 / 45 分节 |
| 手工补充（建模板分支） | 临时科目：`GET` → `4001`；`PUT` 蓝图 → `code=0`、回读 `totalMaxScore=150 / totalDuration=240 / 1 个场次`；随后 SQL 删除该科目的模板与科目行，清理核对为 0 |
| **未做** | 浏览器渲染取证（Chrome 被策略拦，同 `31`） |

## 5. 文件

后端新增：`rk-domain` 的 `dto/PaperTemplateSaveRequest`、`vo/PaperTemplateDetailVO`、`vo/PaperTemplateSaveResultVO`；
`rk-service` 的 `question/PaperTemplateAdminService`、`question/impl/PaperTemplateAdminServiceImpl`；
`rk-api` 的 `controller/admin/AdminPaperTemplateController`。

后端修改：`rk-repository` 的 `PaperTemplateMapper(+XML)`（补 `selectBySubjectId`、模板/场次/分节的
`insertTemplate`/`updateTemplate`/`deleteSections`/`deleteSittings`/`insertSitting`/`insertSection`）。

前端新增：`src/api/admin/paper-template.ts`、`src/views/admin/paper-template-config.vue`。

前端修改：`src/types/api.d.ts`（卷面模板 6 个类型）、`src/router/admin.routes.ts`（一条路由 + 菜单项）、
`src/api/admin/content.ts`（顶部注释指向新文件）。

工具新增：`scripts/verify/p5-paper-template-admin.js`。

## 6. 下一步

1. **浏览器渲染取证**：P5 三页 + 本页（需能启动 Chrome 的环境；`capture.js` / `shot-pages.js`）。
2. `24 §3.2` 的 6 条 P2 验收 + `2002`（§7 建议的下一步里排第一的欠账）。
3. `images_snapshot`（30 偏离 4）与 `repeatedCount` 可观测性（30 偏离 7）——都要先改 04/05。
