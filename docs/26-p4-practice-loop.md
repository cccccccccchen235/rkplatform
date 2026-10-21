# 26 · P4 练习闭环（后端 + 前端）+ 首页去噪

> 承接 [25](25-progress-import-and-fixes.md)。**旧文档一律不回改**：`01`~`25` 保持原样。
>
> 本文档记录 `25` 之后发生的事。口径沿用本项目规矩：**没跑过的写"未验证"，不写"应该没问题"**；
> 每条结论附可复现的取证方式；**对本轮偏离 05/06 的地方逐条登记**（§7）。

---

## 0. 一句话

**P4 练习闭环已跑通：6 种组卷模式、服务端复核判分、幂等上报、错题本、收藏、题目反馈、顺序练习游标全部落地，并有三层取证（46 条接口断言 + MySQL 独立复核 + 前端截图）。** 同时按委托方裁决移除了首页的两处图形编码（脊高、顶端色带）。

---

## 1. 本轮做了什么（按时间顺序）

| # | 事项 | 落点 | 关键证据 |
| :-: | --- | --- | --- |
| 1 | 首页去噪：移除脊高换算与顶端色带 | `web/src/views/portal/home.vue` | `measure.js` 断言脊高集合 `[160]`、脊宽 `[88]`、横向溢出 0；截图见 `.impeccable/review/desktop.png` |
| 2 | 同步活文档（DESIGN.md + surface brief） | `DESIGN.md`、`.impeccable/surfaces/…home-vue.md` | 新增 `Equal-Height Rule` / `One-Encoding-Per-Axis Rule` |
| 3 | P4 领域层：8 实体 + 5 枚举 | `server/rk-domain` | `mvn -pl rk-domain -am compile` exit 0 |
| 4 | **判分引擎**（AC-3 的落点） | `service/practice/{JudgeService,JudgeResult}` | `JudgeServiceTest` **11 条**全绿，且**做过变异对照**（见 §5.1） |
| 5 | 持久层：7 个 Mapper + XML + 3 个 Redis DAO | `server/rk-repository` | 13 个 mapper XML 全部通过 XML 解析；后端启动无 MyBatis 报错 |
| 6 | 契约层：5 DTO + 9 VO | `server/rk-domain/{dto,vo}` | 编译通过 |
| 7 | 服务层：组卷 / 上报 / 错题本 / 收藏 / 反馈 / 进度 | `service/practice/**` | 46 条接口断言 |
| 8 | 接口层：4 个控制器（11 个端点） | `server/rk-api/controller/portal` | `AuthAnnotationTest` 通过（注解与白名单必居其一） |
| 9 | 前端：3 页 + 2 组件 + store + 4 个 api 模块 | `web/src/**` | `pnpm lint && type-check && build` exit 0 |
| 10 | 验收脚本（可复跑） | `scripts/verify/p4-acceptance.js` | 46/46 |

---

## 2. 后端：端点与关键设计

### 2.1 落地的 11 个端点（05 §5.5 / §5.7 / §5.9）

| # | 方法 | 路径 | 权限 |
| :-: | --- | --- | --- |
| 12 | POST | `/practice/papers` | `@SaIgnore` + **方法体内**校验游客资格 |
| 13 | POST | `/practice/records` | USER |
| 14 | GET | `/practice/progress` | USER |
| 15 | PUT | `/practice/progress` | USER |
| 26 | GET | `/wrong-questions` | USER |
| 27 | DELETE | `/wrong-questions/{questionId}` | USER |
| 28 | POST | `/wrong-questions/{questionId}/restore` | USER |
| 29 | POST | `/favorites` | USER |
| 30 | DELETE | `/favorites/{questionId}` | USER |
| 31 | GET | `/favorites` | USER |
| 36 | POST | `/questions/{id}/feedback` | USER |

### 2.2 四处必须写死的口径

**① 服务端复核（AC-3）。** `PracticeRecordRequest` 里**没有 `isCorrect` 字段**——把这个字段从契约上删掉，比"记得忽略它"可靠。上报时按 `questionId` 从 `rk_question` 重新取题，再用 `JudgeService` 判一遍，落库与错题本只认这个结果。

**② 5 秒重放闸。** `ReplayGuardDao` 用 `SET key 1 NX EX 5` 一条命令完成"判重 + 占位"（不用"先 GET 再 SET"，那之间有竞态）。**不可降级**：Redis 不可用时让上报失败——闸门坏了还放行，等于把幂等悄悄关掉。

**③ 错题本状态机。** 答错 → `INSERT … ON DUPLICATE KEY UPDATE`（`wrong_count+1`、连续数归零、状态回 `ACTIVE`）；答对 → **只 UPDATE、绝不 INSERT**（从没答错过的题不该因为答对被拉进本子），且**永不触碰 `REMOVED` 行**。`MASTERED` 的门槛是 `consecutive_correct ≥ threshold`（`sys_config`，默认 2）。
> ⚠️ 这里有一个 MySQL 陷阱：`SET` 子句**从左到右求值，后面的赋值会读到前面已更新的值**。若把 `status` 的赋值排在 `consecutive_correct` 之后，`consecutive_correct + 1` 会读到自增后的数，阈值提前一次触发。XML 里已把 `status` 排在前面并注明。

**④ 游客边界（评审 B6）。** `mode = SAMPLE` 且 `count ≤ 5` 才放行，否则 `2005`；`SAMPLE` 超量是 `1001`。为此 `PracticePaperRequest.mode` **刻意不加 `@Pattern`**：若注解先把未知 mode 拦成 1001，"未登录 + mode=FULL → 2005" 这条测试要求就永远拿不到 2005，而那正是"安全判断在方法体内"的证据。`/practice/papers` **不在 `AuthWhitelist`**，由 `AuthAnnotationTest` 断言。

---

## 3. 前端：3 个页面

| 路由 | 文件 | 要点 |
| --- | --- | --- |
| `/practice/:mode` | `practice-session.vue` | `BlankLayout` + `keepAlive`；本地即时判分、`←/→`/`1-9`/`A-F`/`Enter` 快捷键（输入框内不触发）；答案防抖批量上报；`SEQUENCE` 走 `GET/PUT /practice/progress` 续做 |
| `/wrong-book` | `wrong-book.vue` | 科目切换 + `ACTIVE/MASTERED/REMOVED` 三 tab + 分页；「去重做」「移出/恢复」 |
| `/favorites` | `favorites.vue` | 分页 `QuestionCard`；取消收藏后可**撤销**（8 秒常驻条，撤销即重新 `POST /favorites`） |

组件：`MarkdownRenderer.vue`（markdown-it + 自写 KaTeX 插件，**不开启 `html: true`**）、`QuestionCard.vue`、`QuestionFeedbackDialog.vue`。

### 3.1 本轮修掉的两个前端缺陷（都是"渲染/链路"层，不是样式偏好）

| # | 缺陷 | 根因 | 处置 |
| :-: | --- | --- | --- |
| 1 | **选项里的 `$169$` 原样显示** | 题干走 `MarkdownRenderer`，选项走 `{{ }}` 插值。库里选项内容确实带 `$…$` 定界符 | 给 `MarkdownRenderer` 加 `inline` 模式（`renderInline`，不产生 `<p>` 包裹），选项改用它。**这条直接对应验收项「公式与图片正确渲染」** |
| 2 | `?subjectId=` 在错题本/收藏页被忽略 | 两页只读 `user.currentSubjectId`，于是无法被"链接进来" | 改为"查询参数 → 当前备考科目 → 列表第一个"三级回落 |

---

## 4. 验收（对照 10 §10.6 的 10 条）

脚本：`node scripts/verify/p4-acceptance.js`（**46 条断言，46 通过 / 0 失败**，exit 0）。

| 10 §10.6 的验收项 | 对应断言 |
| --- | --- |
| 五种模式均可用 | 6 种模式组卷 `code=0` |
| 即时显示对错与解析；公式正确渲染 | 取卷带 `correctAnswer`/`options`/`analysis`；截图见 `.impeccable/review/p4-practice-chapter.png` |
| 答题记录落库，行数与实际答题数一致 | 3 条上报 → `accepted=3`；MySQL `answer_rows` 复核 |
| **服务端复核生效** | 请求体塞 `isCorrect:true` + 错误答案 → 响应 `correctCount=0`，**MySQL 里 `is_correct=0`** |
| 答错进错题本；连对 2 次转 `MASTERED` | 答错 → `ACTIVE`；连对 2 次 → `MASTERED`（MySQL 复核 `consecutive_correct=2`） |
| **未作答的题不入错题本** | 空作答 `accepted=1`（流水仍记录）但不在错题本 |
| 断网补传 + 5 秒重复提交只生效一次 | 连发两次 → 第 2 次 `accepted=0` |
| 顺序练习"继续上次练习" | `PUT` 后 `GET` 回读 `lastSequence=42`；按 `fromSequence=42` 取到的题 **id 全部 > 42** |
| 游客 `SAMPLE count=5` 可用；`count=10` → `1001` | 两条断言 |
| 反馈第 4 次 → `1002` | 前 3 次 `code=0`，第 4 次 `code=1002` |

### 4.1 三层取证（不止"接口说成功"）

1. **接口层**：46 条断言，按 `code` 判定（不是"没抛异常"）。
2. **数据库层**：`user_answer_record` 的 `is_correct`、`user_wrong_question` 的 `consecutive_correct/status` 用 MySQL 直接读，独立复现 AC-3 与错题本状态机。
3. **渲染层**：CDP 截图 4 张（章节练习已判分态、游客样例、错题本、收藏）。

### 4.2 三个门禁 + 单测

| 门禁 | 结果 |
| --- | --- |
| `mvn -pl rk-api -am test` | **47 条**全绿（新增 `JudgeServiceTest` 11 条） |
| `pnpm lint && type-check && build` | 三者 exit 0 |
| `check-numeric-consistency.sh` / `check-no-corpus-files.sh` | 13+1 全绿 |

---

## 5. 本轮记录的错误与我怎么证明"没栽"

### 5.1 变异对照：判分测试确实能失败

`docs/22 §5` 的规矩是"任何断言都必须先用一次人为制造漂移证明它会失败"。本轮对判分测试做了这件事：把多选的集合比较**故意改回**顺序敏感（`normalizedAnswer.equals(expected)`），重跑 → `multiChoiceOrderInsensitive`、`multiChoiceIgnoresInnerSpaces` **两条如期失败**；改回后 11/11 恢复全绿。**否则"11 条全绿"只能证明测试跑了，不能证明它在看。**

### 5.2 验收脚本第一版：3 条假失败

首跑 42/45，失败 3 条，全部集中在"连对 2 次转 MASTERED"与"5 秒重放"。根因**不在产品**：我的脚本在只有 5 道题的章节池里复用了同一道题，而那道题的上一次上报还在 5 秒重放窗口内——于是"第 1 次答对"被合规地丢掉，后续断言连锁失败。
**处置**：脚本改用 50 题的顺序卷作为题库池，各用例取独立题目，并在跨用例复用同一题处显式 `sleep(6s)` 越过窗口。
**这条本身是判据的实证**：如果脚本按"调用没抛异常"判定，这 3 条会被打成 ✅。

### 5.3 前端：`eslint-disable` 之下的坑（未发生，但记下判据）

`vue-tsc` 的 `components.d.ts` 由 unplugin 生成：新组件（`el-checkbox-group` 等）**首次 type-check 时声明还不存在**，类型错误要等 `build` 生成后才暴露。因此"三件套只跑 type-check"会漏；必须三项都跑。

---

## 6. 当前总体进度（对照 10 §10.1）

| 阶段 | 状态 | 说明 |
| :-: | :-: | --- |
| P0 设计定稿 | ✅ | `docs/` 26 篇 |
| P1 骨架跑通 | ✅ | |
| P2 内容结构 | 代码/数据 ✅ · **验收 2/8** | `24 §3.2` 的 6 条阻塞项**仍未重跑**（管理端已可用） |
| P3 题库生产 | 方案 B 完成 | 完整流水线未开始 |
| **P4 练习闭环** | **✅ 本轮完成** | 6 模式 / 判分 / 错题本 / 收藏 / 反馈 / 进度 |
| P5 模拟机考 | ⬜ | 防重复窗口的读取方在这里 |
| P6 学习统计 | ⬜ | **依赖 P4 的流水（已就绪）**；`stat_*` 增量更新在这里落地（见 §7 偏离 2） |
| P7 公网部署 / P8 收尾 | ⬜ | |

**现场（可复现）**

| 项 | 值 |
| --- | --- |
| 后端 | `java -jar .verify-run/rk-boot.jar`（前缀注入 `deploy/.env`），`http://127.0.0.1:8080/api/v1` |
| 前端 | `pnpm -C web dev` → **`http://localhost:5174`**（用主机名，勿用 IP） |
| 验收 | `node scripts/verify/p4-acceptance.js`（**跑前需清 `rk:limit:feedback:*`**） |
| 题库 | 250 题，全部 `PUBLISHED`；测试用户 `negctl_81217` |

---

## 7. 偏离登记（本轮对 05/06 的偏离，逐条给出理由）

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | **`stat_*` 增量更新未实现**（06 §6.4 说上报要更新 `stat_daily` 等） | `stat_user_kp.mastery` 的权威口径（06 §6.10.1 的公式 + §6.10.2 的"最近一次作答"窗口函数口径）属于 P6。先按 P6 的口径实现，胜过在这里写一版近似公式再被推翻 | **`onlyUnmastered` 目前是空操作**：SQL 条件正确（`NOT EXISTS … mastery >= 0.6`），但 `stat_user_kp` 还没有行，所以过滤不掉任何题。P6 填表后自动生效 |
| 2 | `PickStrategy` 接口与"≥2000 随机偏移取样"分支**未落地**（06 §6.6.5） | 06 §6.6.5 自己写明"当前阶段先只落 `ORDER BY RAND()` 分支"。题量远小于 2000，建一个永不执行的第二实现属于凭空抽象 | 无（将来题量增长时按 §6.6.5 补） |
| 3 | 防重复窗口**只有写入方**（06 §6.6.4 的读取与回填） | 读取方是模拟考组卷（P5）。练习若也按窗口排除，错题重做与收藏练习会立刻取不到题 | 练习不受影响；P5 按 06 §6.6.4 的 SQL 回填即可 |
| 4 | `GET /practice/progress` **恒返回数组** | 05 §5.5 第 14 条既要"返回单个模式"，又要"缺省返回全部模式"。若按 mode 有无切换对象/数组，前端要写两套解析 | 前端按数组消费 |
| 5 | 进度接口的 mode **允许 `WRONG_REDO`** | 05 §5.5 第 14 条明确列了它；04 的列注释只列了 `CHAPTER/KP/SEQUENCE/FAVORITE`（注释不是约束） | 无 |
| 6 | 错题本 `remove`/`restore` 在**不在本子里时返回 `1003`** | 05 §5.7 未定义该错法的错误码；用"资源不存在"比静默成功更诚实 | 前端需容忍 1003 |
| 7 | 收藏的增/删**幂等、不报错**（与错题本刻意不同） | 前端对收藏做乐观更新，双击/重试会产生重复请求；而错题本的"移出"是一次明确的状态变更 | 无 |
| 8 | `user_practice_progress` 的计数**只对 `SEQUENCE` 累加** | 只有顺序练习有"游标"语义；给随机模式记作答题数回答不了"我练到哪了" | 其余模式的 `answeredCount` 恒为 0 |
| 9 | 上报表里的 `outlineId` 用 `0` 哨兵 | 上报请求不带 `refId`（05 §5.5 第 13 条的字段表确实没有） | 章节维度的进度行由 `PUT /practice/progress` 维护 |

---

## 8. 待办

| 优先 | 事项 | 说明 |
| :-: | --- | --- |
| 高 | **重跑 `24 §3.2` 的 6 条 P2 验收 + `2002`** | 管理端已可用，挡板已移除；本轮未做 |
| 中 | **P6 之前的 `stat_*` 增量更新** | 见 §7 偏离 1；落地时 `onlyUnmastered` 自动生效 |
| 中 | 知识点词表 452 → 受控（`25 §2.4` 选甲） | 影响知识点练习与薄弱点分析 |
| 中 | 删或留测试用户 `negctl_81217` | 本轮验收又用它跑了 46 条断言，库里留下流水/反馈行 |
| 低 | 清理验收残留数据 | 生产前用 `deploy/sql` 重建即可 |
| 低 | 清理 5173 的残留 vite 进程 | 新前端仍在 5174 |

---

## 9. 文档链现状

| 序号 | 文件 | 角色 | 状态 |
| :-: | --- | --- | --- |
| 24 | [24-execution-record.md](24-execution-record.md) | P2 执行记录 | 留痕，不改 |
| 25 | [25-progress-import-and-fixes.md](25-progress-import-and-fixes.md) | 题库导入 + 两处后端修复 | 留痕，不改 |
| **26** | **本文件** | **P4 练习闭环 + 首页去噪** | **本轮交付** |

**冲突裁决顺序**：`26` > `25` > `24` > `23` > `22`；跨阶段沿用 `17` > `16` > `15` > `14` > `13` > `12` > `11`。

**后续约定**：P5 模拟机考完成后另出 `27`，不修改本篇。