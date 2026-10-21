# 07 · 前端开发设计

> 技术栈：Vue 3.5 · Vite 5 · TypeScript 5 · Pinia 2 · Vue Router 4 · Element Plus 2 · ECharts 5 · Axios · KaTeX 0.16（见 [03-architecture.md](03-architecture.md#32-技术选型与理由)）。
> 接口路径、字段名、枚举值唯一来源为 [05-api.md](05-api.md)；业务规则与题型枚举唯一来源为 [02-domain-model.md](02-domain-model.md)。本文档不自创任何字段名。
> 目录结构沿用 [03-architecture.md](03-architecture.md#34-前端工程结构) 3.4 节，不得改动目录名。

## 7.1 技术选型与理由

| 依赖 | 解决的问题 | 为什么不用替代方案 |
| --- | --- | --- |
| **Element Plus 2.x** | 管理端 12 个页面几乎全是「表格 + 筛选表单 + 树 + 分页 + 对话框」；考生端需要表单校验、消息提示、确认框 | 备选 Naive UI：树与 `el-table` 的插槽组合、表单校验触发方式与本项目批量录入/审核场景契合度更低；Ant Design Vue：跟随 React 版设计语言，与 Vue 生态更新节奏不同。自研组件在管理端会吃掉大量工期。 |
| **Pinia 2.x** | 跨页面共享登录态、当前科目、进行中的练习/考试会话 | 备选 Vuex：需要 `mutations`/`actions` 双写，TS 推导在 `commit` 处丢失；本项目的 `exam` store 要承载倒计时与自动保存，直接改 state 更直接。 |
| **KaTeX 0.16** | 题干/选项/解析中的行内公式 `$...$` 与块级公式 `$$...$$`（02 文档 2.2.2，软考题含大量计算式） | 备选 MathJax：体积更大、需运行时异步加载字体与配置脚本，与「题干同步渲染」目标冲突；KaTeX 可随包整体引入、字体本地化、渲染同步完成。 |
| **ECharts 5.x** | M6 统计：做题量趋势（折线）、章节课时分布（柱状）、知识点薄弱点（雷达/横向柱） | 备选 Chart.js：雷达与大数据量折线配置成本更高、中文文档弱；自研 SVG 图表在交互（tooltip、缩放）上不划算。 |
| **Axios** | 统一拦截器（注入 token、解包 `data`、错误码分流） | 备选 `fetch`：需自行封装超时、进度、取消，等于重写一份 Axios。 |
| **markdown-it** | 题干/解析的 Markdown 渲染，与 KaTeX 组合 | 备选 `marked`：扩展点（自定义 KaTeX 规则）不如 markdown-it 清晰。 |

**不引入**：VueUse 全家桶（仅按需引入个别工具函数）、i18n（01 文档 1.5：仅简体中文）、Tailwind（与 Element Plus 变量体系冲突）、状态持久化插件（只手动持久化 token）。

## 7.2 工程结构

```
web/
├── index.html
├── vite.config.ts               # 见 7.2.1
├── package.json                 # pnpm，scripts: dev / build / preview / lint / type-check
├── tsconfig.json                # strict: true，paths: @/* → src/*
├── .env.development / .env.production    # VITE_API_BASE=/api/v1（两环境一致，路径前缀由 05 文档固定）
├── eslint.config.js / .prettierrc
└── src/
    ├── api/                     # 只放「一函数一接口」映射，禁止放机制代码
    │   ├── auth.ts              # /auth/**、/users/me        subject.ts   # /subjects/**
    │   ├── practice.ts          # /practice/**               exam.ts      # /exams/**
    │   ├── wrong-book.ts        # /wrong-questions/**        favorite.ts  # /favorites/**
    │   ├── stats.ts             # /stats/**                  feedback.ts  # /questions/{id}/feedback
    │   └── admin/               # question.ts(/admin/questions|batches)、content.ts(/admin/subjects|outline|
    │                            #   knowledge-points|paper-templates)、file.ts(/admin/files)、
    │                            #   feedback.ts(/admin/feedback|dashboard)、user.ts(/admin/users)
    ├── assets/                  # 图标、logo（业务图片一律走后端 /uploads）
    ├── components/
    │   ├── base/                # 无业务：MarkdownRenderer、EmptyState、PaginationBar、ErrorBlock
    │   └── question/            # 业务：QuestionCard、AnswerSheet、EssayEditor、SectionTip、QuestionFeedbackDialog
    ├── layouts/                 # PortalLayout.vue（顶栏+科目切换）/ AdminLayout.vue（侧栏+面包屑）
    │                            # BlankLayout.vue（登录注册、模拟考答题页，无导航防误触）
    ├── router/                  # index.ts（createRouter+全局守卫）；portal.routes.ts；admin.routes.ts（全懒加载）
    ├── stores/                  # user.ts / subject.ts / practice.ts / exam.ts
    ├── styles/                  # variables.scss（设计变量，见 7.9）/ element-override.scss / reset.scss / index.scss
    ├── types/                   # api.d.ts（手写接口类型，见 7.11）/ domain.d.ts（枚举联合类型）
    ├── utils/                   # request.ts（★ axios 封装）/ upload.ts / format.ts（时间、hh:mm:ss、百分比）
    │                            # storage.ts（token 与草稿，统一 key 前缀 rk:）/ katex-markdown.ts
    ├── views/                   # portal/ 与 admin/，见 7.5 页面清单
    ├── App.vue
    └── main.ts
```

> 决策与理由
> `request.ts` 放在 `utils/` 而非 `api/`：`api/` 只放「一函数一接口」的纯映射，`utils/` 放不依赖业务的机制代码。这样 `api/*.ts` 可以整文件被删除/替换而不影响机制层，也让「接口路径变更只需要改 api/」成为可执行的约定。
> 目录与 03 文档 3.4 节完全一致，**唯一新增**的是 `types/`（接口类型手写维护位置，见 7.11）；未改动、未重命名任何既有目录。
> **接口路径书写约定**：`api/*.ts` 中的路径为相对路径（不含 `/api/v1`，由 `baseURL` 拼上）；本文 7.4 的「接口」列与 7.7 的错误码表写完整路径 `/api/v1/...`，7.5 页面表沿用 05 文档的相对写法（如 `/exams/{id}/answers`），两者等价。

### 7.2.1 vite.config.ts 关键配置点

| 配置 | 内容 | 目的 |
| --- | --- | --- |
| `server.port` | `5173` | 与 README 快速开始一致 |
| `server.proxy['/api']` | `{ target: 'http://localhost:8080', changeOrigin: true }` | 开发期免 CORS |
| `server.proxy['/uploads']` | 同上 | 题干图片由后端静态托管（05 文档 5.10 第 51 项），不代理则 Markdown 图片 404 |
| `build.rollupOptions.output.manualChunks` | 见下 | 第三方库分家，控制考生端首屏 |
| `css.preprocessorOptions.scss.additionalData` | `@use "@/styles/variables.scss" as *;` | 全量可用设计变量 |
| `resolve.alias` | `@` → `src` | |
| `build.assetsInlineLimit` | KaTeX 字体不做 dataURL 内联（保持独立文件） | 字体文件过大，内联会撑爆 JS |
| 插件 | `unplugin-auto-import` + `unplugin-vue-components`（`ElementPlusResolver`）、`vite-plugin-eslint` | Element Plus 与常用 API 按需引入 |

`manualChunks` 策略：

```ts
manualChunks(id) {
  if (id.includes('echarts') || id.includes('zrender')) return 'vendor-echarts'
  if (id.includes('katex')) return 'vendor-katex'
  if (id.includes('element-plus')) return 'vendor-element'
  if (id.includes('markdown-it')) return 'vendor-markdown'
  if (id.includes('node_modules')) return 'vendor'
}
```

> **`if (id.includes('/src/views/admin/')) return 'admin'` 这一条在本轮（P2）被删除**，原来那句注释"管理端独立分包，考生端首屏不加载"是**反的**：手工 chunk 的语义是"这些模块必须放在一起"，rollup 会把该 chunk 依赖的公共模块一并并进来——`utils/request.ts`、`utils/storage.ts` 被并进 `admin` 之后，`index-*.js`（路由与 store 也 import 它们）就必须**静态** import `admin-*.js`，于是 `index.html` 里出现 `admin` 的 `modulepreload`，首屏反而多下载 **11.53 KB**，`§7.12`「管理端代码不进考生端首屏」当场为假。删掉后由 rollup 按 `() => import()` 自然分包，三个管理页各自成 chunk，首屏的 `modulepreload` 只剩 `vendor` / `vendor-element` 两条。
> **一般规则：想让一段代码不进首屏，靠的是"它只被动态 import 引用"，不是"给它起个 chunk 名"。** 实测与前后 chunk 清单见 `24`。
>
> 决策与理由
> KaTeX 字体：`katex.min.css` 引用的 woff2 由 Vite 作为 asset 输出到 `dist/assets/`，`base` 保持默认相对路径，无需 CDN。**不使用官方 CDN 方案**，因为部署目标为单机 + 可能无外网（假设 A2），外链字体失败会导致公式回退成乱码文本。

## 7.3 路由设计

统一 meta 约定：

```ts
interface RouteMeta {            // 全部字段可选，未声明即按 false 处理
  title: string                  // document.title
  requiresAuth?: boolean         // 需登录；requiresAdmin?: boolean 需 ADMIN（仅体验层拦截）
  layout?: 'portal' | 'admin' | 'blank'
  keepAlive?: boolean            // 练习/统计页开启缓存；activeMenu?: string 详情页高亮父菜单
  menuShort?: string             // 管理端菜单折叠（64px）时显示的 1~2 字缩写
  menuOrder?: number             // 管理端菜单排序；不填则菜单不收录该路由
}
```

> **管理端菜单的来源**：菜单项由 `router.getRoutes()` 过滤 `path` 以 `/admin/` 开头、且带 `menuShort` 的路由生成（7.8 的「菜单由 `admin.routes.ts` 生成」）。所以 `menuOrder` 不是可有可无的装饰——`getRoutes()` 返回的是**扁平化后的匹配器顺序**，嵌套子路由的先后不保证与声明顺序一致，靠数组下标排菜单会让它在某次重构后悄悄变序。**未填 `menuShort` 的路由不进菜单**（如 `:id/edit` 这类详情页），这就是"哪些页面出现在侧栏"的判据。

### 7.3.1 考生端路由（`portal.routes.ts`）

| path | name | 组件 | 需登录 | 需 ADMIN | meta 备注 |
| --- | --- | --- | :-: | :-: | --- |
| `/` | `home` | `portal/home.vue` | ✗ | ✗ | `title: 首页`；游客显示科目入口与样例题（`mode=SAMPLE`） |
| `/subjects` | `subjects` | `portal/subject-list.vue` | ✗ | ✗ | 公开（`GET /api/v1/subjects`） |
| `/subjects/:id` | `subject-detail` | `portal/subject-detail.vue` | ✗ | ✗ | `activeMenu: subjects`，含 `paperSummary`（场次 / 分节结构）与知识点标签 |
| `/subjects/:id/outline` | `subject-outline` | `portal/subject-outline.vue` | ✗ | ✗ | `activeMenu: subjects`，3 层大纲树（节点显示题量） |
| `/practice/:mode` | `practice` | `portal/practice-session.vue` | **按 `mode` 判定** | ✗ | `mode ∈ CHAPTER/KP/SEQUENCE/FAVORITE/WRONG_REDO/SAMPLE`；**仅 `mode = SAMPLE`（游客样例题）免登录**，其余 mode 均需登录 —— 守卫在 `beforeEach` 中按 `to.params.mode` 动态判定，未登录时干净重定向到 `/login?redirect=...`，**不应让请求打到后端去吃 `2005` toast**（对应评审 D3）；`layout: blank`；`keepAlive: true` |
| `/exam` | `exam-center` | `portal/exam-center.vue` | ✓ | ✗ | 发起模拟考 + 历史成绩列表（`GET /api/v1/exams`） |
| `/exam/:sessionId` | `exam-session` | `portal/exam-session.vue` | ✓ | ✗ | `layout: blank`；答题页，刷新可恢复 |
| `/exam/:sessionId/result` | `exam-result` | `portal/exam-result.vue` | ✓ | ✗ | 成绩详情（`GET /api/v1/exams/{id}/result`） |
| `/wrong-book` | `wrong-book` | `portal/wrong-book.vue` | ✓ | ✗ | 错题本（`GET /api/v1/wrong-questions`，`subjectId` 必填） |
| `/favorites` | `favorites` | `portal/favorites.vue` | ✓ | ✗ | 收藏列表 |
| `/stats` | `stats` | `portal/stats-overview.vue` | ✓ | ✗ | `keepAlive: true`，ECharts 图区 |
| `/profile` | `profile` | `portal/profile.vue` | ✓ | ✗ | 昵称/密码/当前科目/退出（M1） |
| `/login` | `login` | `portal/login.vue` | ✗ | ✗ | `layout: blank` |
| `/register` | `register` | `portal/register.vue` | ✗ | ✗ | `layout: blank` |
| `/:pathMatch(.*)*` | `not-found` | `portal/not-found.vue` | ✗ | ✗ | 404 |

### 7.3.2 管理端路由（`admin.routes.ts`，全部 `() => import()`）

| path | name | 组件（懒加载） | 需登录 | 需 ADMIN | 功能模块 |
| --- | --- | --- | :-: | :-: | --- |
| `/admin/dashboard` | `admin-dashboard` | `admin/dashboard.vue` | ✓ | ✓ | M10 / UC-A09（`GET /api/v1/admin/dashboard`） |
| `/admin/questions` | `admin-questions` | `admin/question-list.vue` | ✓ | ✓ | M8 / UC-A01（`GET /api/v1/admin/questions`） |
| `/admin/questions/:id/edit` | `admin-question-edit` | `admin/question-edit.vue` | ✓ | ✓ | `activeMenu: admin-questions`；`:id` 为 `new` 时走新增 |
| `/admin/review` | `admin-review` | `admin/review-workbench.vue` | ✓ | ✓ | M9 / UC-A02 |
| `/admin/batches` | `admin-batches` | `admin/batch-list.vue` | ✓ | ✓ | UC-A03（`GET /api/v1/admin/batches`） |
| `/admin/subjects` | `admin-subjects` | `admin/subject-manage.vue` | ✓ | ✓ | UC-A04 |
| `/admin/outline` | `admin-outline` | `admin/outline-manage.vue` | ✓ | ✓ | UC-A04（`subjectId` 必填） |
| `/admin/knowledge-points` | `admin-kps` | `admin/knowledge-point-manage.vue` | ✓ | ✓ | UC-A05 |
| `/admin/paper-templates` | `admin-paper-templates` | `admin/paper-template-config.vue` | ✓ | ✓ | UC-A06 |
| `/admin/feedback` | `admin-feedback` | `admin/feedback-list.vue` | ✓ | ✓ | UC-A08 |
| `/admin/users` | `admin-users` | `admin/user-manage.vue` | ✓ | ✓ | UC-A07 |
| `/admin/configs` | `admin-configs` | `admin/system-config.vue` | ✓ | ✓ | 系统配置读写（`GET/PUT /api/v1/admin/configs`） |

管理端统一挂在 `AdminLayout` 下（子路由），考生端挂在 `PortalLayout` / `BlankLayout` 下。

### 7.3.3 全局守卫逻辑（`router/index.ts`）

```ts
router.beforeEach(async (to) => {
  document.title = to.meta.title ? `${to.meta.title} · 软考在线学习平台` : '软考在线学习平台'
  const user = useUserStore()
  // 1) 有 token 但无用户信息（刷新页面）→ 取 /users/me；失败即视为未登录
  if (user.token && !user.userInfo) { try { await user.fetchMe() } catch { user.clearAuth() } }
  // 2) 未登录 → 跳登录并携带 redirect
  if (to.meta.requiresAuth && !user.isLogin) return { name: 'login', query: { redirect: to.fullPath } }
  // 3) 已登录访问登录/注册页 → 回 redirect 或首页（只接受站内相对路径）
  if (user.isLogin && (to.name === 'login' || to.name === 'register')) {
    const redirect = (to.query.redirect as string) || '/'
    return redirect.startsWith('/') ? redirect : '/'
  }
  // 4) 角色不够（仅体验层拦截，真正校验在服务端）
  if (to.meta.requiresAdmin && !user.isAdmin) { ElMessage.error('无权限访问该页面'); return { name: 'home' } }
  return true
})
```

`afterEach`：结束顶部进度条；`onError`：路由 chunk 加载失败 → 提示并刷新（部署后旧 chunk 失效场景）。

## 7.4 状态管理（Pinia）

### 7.4.1 `stores/user.ts`

| 类别 | 名称 | 类型/说明 | 接口 |
| --- | --- | --- | --- |
| state | `token` | `string`（初始化自 `storage.get('rk:token')`） | — |
| state | `userInfo` | `UserVO \| null`（`id/username/nickname/role/status/currentSubjectId/lastLoginAt/createdAt`） | — |
| state | `permissionsLoading` | `boolean`（守卫取用户信息期间） | — |
| getter | `isLogin` / `isAdmin` / `currentSubjectId` | `!!token` / `userInfo?.role === 'ADMIN'` / `userInfo?.currentSubjectId ?? null` | — |
| action | `register(payload)` / `login(username, password)` | 写 token + `userInfo` 并持久化 token | `POST /api/v1/auth/register`、`POST /api/v1/auth/login` |
| action | `fetchMe()` | 刷新用户信息（守卫与刷新页面时调用） | `GET /api/v1/users/me` |
| action | `updateNickname(nickname)` / `setCurrentSubject(subjectId)` | 回写 `userInfo.nickname` / `userInfo.currentSubjectId`（UC-U01） | `PUT /api/v1/users/me`、`PUT /api/v1/users/me/current-subject` |
| action | `updatePassword(oldPassword, newPassword)` | 成功后清 token 并跳登录 | `PUT /api/v1/users/me/password` |
| action | `logout()` / `clearAuth()` | 调后端置 token 失效后清本地（失败也清）；`clearAuth()` 只清 token + `userInfo` + 本地缓存 | `POST /api/v1/auth/logout` |

### 7.4.2 `stores/subject.ts`

| 类别 | 名称 | 说明 | 接口 |
| --- | --- | --- | --- |
| state | `grouped` | `Record<'JUNIOR'\|'MIDDLE'\|'SENIOR', SubjectVO[]>` | — |
| state | `flatList` | `SubjectVO[]`（由 `grouped` 展平，供下拉框） | — |
| state | `currentSubject` | `SubjectVO \| null`（详情页，含 `paperSummary`） | — |
| state | `outlineTree` | `OutlineNodeVO[]` | — |
| state | `knowledgePoints` | `{id, name}[]` | — |
| getter | `configuredSubjects` | `flatList.filter(s => s.paperSummary != null)`（可发起模拟考） | — |
| action | `fetchSubjects(params?)` | 缓存 5 分钟，`?level=&onlyConfigured=true` | `GET /api/v1/subjects` |
| action | `fetchSubjectDetail(id)` | | `GET /api/v1/subjects/{id}` |
| action | `fetchOutline(id)` | 返回根节点数组 | `GET /api/v1/subjects/{id}/outline` |
| action | `fetchKnowledgePoints(id)` | | `GET /api/v1/subjects/{id}/knowledge-points` |

### 7.4.3 `stores/practice.ts`

| 类别 | 名称 | 说明 | 接口 |
| --- | --- | --- | --- |
| state | `subjectId` / `mode` / `refId` | 科目；`CHAPTER/KP/SEQUENCE/FAVORITE/WRONG_REDO/SAMPLE`；章节 ID 或知识点 ID | — |
| state | `questions` | `QuestionDetailVO[]`（**含答案与解析**） | — |
| state | `answers` / `judged` | `Record<questionId, string>`；`Record<questionId, boolean \| null>`（本地判分，主观题为 `null`） | — |
| state | `currentIndex` / `total` / `nextSequence` | 当前下标；试卷总题数；顺序练习续做游标 | — |
| state | `pendingRecords` | 未上报的 `{questionId,userAnswer,durationMs}[]` | — |
| getter | `currentQuestion` / `answeredCount` / `accuracy` | `questions[currentIndex]`；本地统计（仅展示，权威值以上报返回为准） | — |
| action | `fetchPaper(payload)` | `{subjectId, mode, refId?, count, difficulty?, onlyUnmastered?, fromSequence?}` | `POST /api/v1/practice/papers` |
| action | `judgeLocally(questionId, userAnswer)` | 见下 | — |
| action | `flushRecords(source)` | 批量上报（单次 ≤50 条），成功后清 `pendingRecords` 并用 `correctCount` 覆盖本地正确数 | `POST /api/v1/practice/records` |
| action | `fetchProgress(subjectId)` / `saveProgress(subjectId, seq)` | 顺序练习游标读取与推进 | `GET`/`PUT /api/v1/practice/progress` |
| action | `reset()` | 清空会话状态 | — |

本地判分规则（严格对齐 02 文档 2.2.1，服务端在上报时复核）：

| `questionType` | 判分 | `judged` 值 |
| --- | --- | --- |
| `SINGLE_CHOICE` / `TRUE_FALSE` | 与 `correctAnswer` 严格相等 | `boolean` |
| `MULTI_CHOICE` | 与 `correctAnswer` **集合完全相等**，不给部分分 | `boolean` |
| `SUBJECTIVE` / `ESSAY` | **不判分** | `null`（UI 只显示参考答案与 `scorePoints`） |

`source` 映射：`CHAPTER → PRACTICE_CHAPTER`、`KP → PRACTICE_KP`、`SEQUENCE → PRACTICE_SEQUENCE`、`FAVORITE → PRACTICE_FAVORITE`、`WRONG_REDO → WRONG_REDO`；`SAMPLE` 模式（游客）**不上报**。

### 7.4.4 `stores/exam.ts`

| 类别 | 名称 | 说明 | 接口 |
| --- | --- | --- | --- |
| state | `sessionId` / `session` | 会话 ID；`ExamSessionVO \| null`（`status/totalDurationSec/remainingSec/currentSittingNo/currentSectionNo/questionCount/autoScoredMaxScore/totalMaxScore/sittings/sections/items`） | — |
| state | `items` / `answers` / `dirty` | `ExamItemVO[]`（**不含答案**）；`Record<itemId, string>`；`Set<itemId>` 待保存作答 | — |
| state | `currentItemId` / `submitting` | 当前题；交卷进行中（防重复点击） | — |
| state | `remainingSec` / `syncedAt` / `tickAt` | **当前场次**服务端最近一次下发的剩余秒数；本地收到该值的 `Date.now()`；1 秒定时器推进的时间戳（驱动展示倒计时重算） | — |
| getter | `displaySec` | `Math.max(0, remainingSec - Math.floor((tickAt - syncedAt) / 1000))`；**仅当前场次 `IN_PROGRESS` 时驱动倒计时**，`PENDING` 场次不跑本地计时 | — |
| getter | `currentSitting` / `currentSection` / `answeredItemIds` / `unansweredCount` | 当前场次（按 `currentSittingNo` 取）；当前分节；答题卡状态来源与未答数 | — |
| action | `createExam(subjectId)` / `fetchSession(id)` | 发起考试并载入 `ExamSessionVO`；断点续答恢复 | `POST /api/v1/exams`、`GET /api/v1/exams/{id}` |
| action | `setAnswer(itemId, value)` | 写 `answers` + `dirty`，并触发 `scheduleSave()` | — |
| action | `scheduleSave()` | 节流上报 `dirty` 项（单次 ≤100 条，超出分批）；响应的 `remainingSec` 用于校准 | `PUT /api/v1/exams/{id}/answers` |
| action | `flushSave()` | **取消防抖、立刻上报全部 `dirty` 项**；无 `dirty` 时仍按"空 `items` 数组心跳"调用同一接口刷新 `remainingSec`（对应评审 B10） | `PUT /api/v1/exams/{id}/answers` |
| action | `startSitting(sittingNo)` | 开始指定场次（`PENDING → IN_PROGRESS`），用返回的 `remainingSec` 启动本地倒计时 | `POST /api/v1/exams/{id}/sittings/{sittingNo}/start` |
| action | `finishSitting(sittingNo)` | 结束场次；若返回的下一场次为 `PENDING` 则停止本地倒计时并展示「开始本场次」 | `POST /api/v1/exams/{id}/sittings/{sittingNo}/finish` |
| action | `advanceSection(sectionNo)` | 离开当前分节进入下一节（**无 `carryOver` 参数**）；用返回的 `remainingSec`/`currentSectionNo`/`sections` 就地更新 | `POST /api/v1/exams/{id}/sections/{sectionNo}/advance` |
| action | `submit()` | 幂等；返回 `ExamResultVO` 摘要后跳成绩页 | `POST /api/v1/exams/{id}/submit` |
| action | `fetchResult(id)` / `abandon(id)` | 结果只读（不进 store 主状态）；二次确认后置 `ABANDONED` | `GET /api/v1/exams/{id}/result`、`POST /api/v1/exams/{id}/abandon` |
| action | `startTick()` / `stopTick()` | 启动/停止 1 秒定时器 | — |

**服务端权威计时（约束 C4）**

1. `remainingSec` **只会**被服务端响应覆盖：来源是 `fetchSession` / `saveAnswers` / `startSitting` / `finishSitting` / `advanceSection` 的返回值。每次覆盖同时执行 `syncedAt = now; tickAt = now`。
2. **本地倒计时与场次绑定**：只有当前 `IN_PROGRESS` 场次才跑本地倒计时；`PENDING` 场次（如高级科目的论文场次，见 02 文档 §2.4.1）**不启动计时**，改为显示「开始本场次」按钮与规则说明文案（提示"上午连考场次结束后，论文场次需点击开始，中间不计时"）。`startSitting()` 成功后才 `startTick()`（对应评审 A5）。
3. 本地 `setInterval(1000)` 只把 `tickAt` 推到当前时间，`displaySec` 由 getter 计算 —— **本地定时器不写回 `remainingSec`，不参与任何判定**，只做视觉递减。
4. 自动保存即校准：作答变更后 debounce 5s 触发 `scheduleSave()`，把 `dirty` 项（单次 ≤100 条，超出分批）作为 `items` 提交；响应中的 `remainingSec` 用来纠偏（页面切后台、标签页休眠导致的本地快进都会被纠正）。无变更时每 60s 触发一次心跳保存（提交空 `items` 数组）以刷新 `remainingSec`。
5. `displaySec === 0` 时：**不本地强制交卷**。仅调用一次 `flushSave()` → 若服务端判定本场次超时，会返回 `4005`；若此时已无后续场次则按「已自动交卷」处理并跳 `/exam/:sessionId/result`，若仍有 `PENDING` 场次则进入「开始本场次」状态；若返回正常（说明本地时钟偏快），用返回的 `remainingSec` 校准继续答题。
6. 页面 `visibilitychange` 回到前台时立即触发一次保存校准。

**断点续答恢复流程**

```text
进入 /exam/:sessionId → fetchSession(id)                    // GET /api/v1/exams/{id}
  status = SUBMITTED | SCORED  → 直接 replace 到 result 页
  status = ABANDONED           → 提示会话已放弃，引导去 /exam
  status = CREATED | IN_PROGRESS → items/answers/sittings/sections/currentSittingNo/currentSectionNo/remainingSec 全量重建
       → 按 currentSittingNo 定位当前场次；若该场次 status = PENDING → 不自动开始计时，展示「开始本场次」按钮等用户点击
       → 若该场次 status = IN_PROGRESS → currentItemId = 该场次第一道 userAnswer == null 的题；startTick()
       → 论文/主观题分区：读 localStorage 草稿 rk:essay:{sessionId}:{itemId}，
         若服务端 userAnswer 为空且草稿非空 → 提示「检测到未提交草稿，是否恢复」
  路由离开 → flushSave() 后 stopTick()
```

**发起新考试的冲突处理**：`POST /api/v1/exams` 返回 `4003`（存在未完成的模拟考）时，前端弹出对话框，提供「继续未完成的考试」（跳该 `sessionId`）与「放弃并重新开始」（调 `abandon` 后重试）两个动作。对话框**必须显示 `sessionId` 与当前场次**，因为这是用户在该窗口期内唯一的入口。

> 对应评审 G1：高级科目考生若考完上午连考后不再回来，会话会停在 `IN_PROGRESS`（场次一 `FINISHED`、场次二 `PENDING`），而此时该会话**不在历史列表里**（默认口径只返回 `SUBMITTED`/`SCORED`，`includeAbandoned` 也不含 `IN_PROGRESS`）。因此上面的 `4003` 对话框是唯一的找回路径，不能省。窗口期结束后（默认 6 小时）由 [06-backend.md](06-backend.md) §6.13 的「会话级滞留结算」自动交卷，会话随即出现在历史成绩中，**前端无需为此额外做任何事**。

## 7.5 页面清单

### 7.5.1 考生端

| 页面 | 路径 | 核心区块 | 调用接口 | 关键交互与边界状态 |
| --- | --- | --- | --- | --- |
| 首页 | `/` | 当前科目卡片、快捷入口（继续练习/错题本/模拟考）、今日数据 | `GET /users/me`、`GET /stats/overview`、`GET /subjects` | 游客态：显示科目入口 + 样例题按钮（`mode=SAMPLE`，≤5 题）；未设当前科目时显示引导选择；统计失败 `5001` → 显示空态而非报错 |
| 科目列表 | `/subjects` | 按 `level` 三级分组卡片，展示 `examPeriod`/`outlineStatus`/`contentStatus`/`questionCount`、`paperSummary` 摘要 | `GET /subjects` | loading 骨架屏；空态（`onlyConfigured` 过滤后为空）；图片/简介缺失时占位 |
| 科目详情 | `/subjects/:id` | 卷面摘要（`totalDuration`/`totalMaxScore`/`autoScoredMaxScore`，**按场次分区**）、分节表（含 `supportStatus` 与 `sectionType` 文案）、知识点标签、大纲入口 | `GET /subjects/{id}`、`GET /subjects/{id}/knowledge-points` | `3001` → 空态「科目不存在或已停用」；`paperSummary = null` → 空态「尚未配置卷面模板」；`outlineStatus = SKELETON` → 提示「该科目暂无完整章节树」；`NOT_SUPPORTED` 分节旁提示去官方机考系统 |
| 大纲树 | `/subjects/:id/outline` | 3 层章节树，每个节点显示本节点（含子节点）的已发布题量；根节点按 `parentId === "0"` 取 | `GET /subjects/{id}/outline` | `3001` → 空态「科目不存在或已停用」；返回 `[]` → 空态「该科目还没有章节」（与"取不到"是两件事，文案不得混） |
| 练习答题页 | `/practice/:mode` | 顶部（科目/模式/进度 `x/total`）、题干区、`QuestionCard`、底部操作条（上一题/下一题/收藏/反馈/结束练习） | `POST /practice/papers`、`POST /practice/records`、`POST /favorites`、`DELETE /favorites/{questionId}`、`GET /practice/progress`、`PUT /practice/progress` | 见下方专项说明 |
| 模拟考中心 | `/exam` | 科目选择（仅 `paperSummary != null`）、发起按钮、历史成绩表（分页） | `GET /subjects?onlyConfigured=true`、`POST /exams`、`GET /exams` | `4001` 无模板 → 禁用发起并说明；`4002` 无可作答分节 → 提示；`4003` → 继续/放弃对话框；历史为空 → 空态 |
| 模拟考答题页 | `/exam/:sessionId` | 顶部场次/分节进度与倒计时、`SectionTip`、`QuestionCard`（无答案）、`AnswerSheet`（右侧抽屉/侧栏，按场次分区）、底部交卷条 | `GET /exams/{id}`、`PUT /exams/{id}/answers`、`POST /exams/{id}/sittings/{sittingNo}/start`、`POST /exams/{id}/sittings/{sittingNo}/finish`、`POST /exams/{id}/sections/{sectionNo}/advance`、`POST /exams/{id}/submit`、`POST /exams/{id}/abandon` | 见下方专项说明 |
| 成绩详情页 | `/exam/:sessionId/result` | 总分卡（`objectiveScore`/`autoScoredMaxScore`/`totalMaxScore`/`accuracy`/`usedSec`）、**按场次分区的**分节结果表、逐题回顾（含答案与解析） | `GET /exams/{id}/result` | 见下方专项说明；`4004` → 「会话不存在」空态；未交卷时先引导去答题页 |
| 错题本 | `/wrong-book` | 科目切换、筛选（章节/知识点/`sortBy`/`status`）、列表（`stemSummary`/`wrongCount`/`consecutiveCorrect`/`lastAnsweredAt`）、分页 | `GET /wrong-questions`、`DELETE /wrong-questions/{questionId}`、`POST /wrong-questions/{questionId}/restore` | `subjectId` 必填，未选科目时不发请求；操作后局部刷新该行（乐观更新 + 失败回滚）；空态区分「无错题」与「无 `REMOVED` 记录」；「重做」→ 跳 `/practice/WRONG_REDO` |
| 收藏 | `/favorites` | 科目切换、题目卡片列表、分页 | `GET /favorites`、`DELETE /favorites/{questionId}` | 取消收藏后从列表移除并提示可撤销（撤销即重新 `POST /favorites`）；空态 |
| 学习统计 | `/stats` | 总览卡（`answeredCount`/`accuracy`/`continuousDays`/`mastery`/`outlineCoverage`/`todayAnswered`）、趋势折线、章节掌握度柱状、薄弱知识点列表 | `GET /stats/overview`、`GET /stats/trend`、`GET /stats/outline`、`GET /stats/weak-kp` | `5001` 暂无统计 → 空态引导去练习；`days` 默认 30；图表 `resize` 监听；缺数据日期由服务端补 0，直接渲染 |
| 个人中心 | `/profile` | 基本信息（`username`/`nickname`/`lastLoginAt`/`createdAt`）、改昵称、改密码、当前科目 | `GET /users/me`、`PUT /users/me`、`PUT /users/me/password`、`PUT /users/me/current-subject`、`POST /auth/logout` | `2006` 原密码错误就地提示；改密成功后强制登出并跳登录；退出登录二次确认 |
| 登录 | `/login` | 用户名/密码表单、跳注册 | `POST /auth/login` | `2001` 用户名或密码错误、`2002` 账号禁用、`2004` 登录失败次数过多 —— 均就地 `el-alert`；成功后按 `?redirect=` 跳转 |
| 注册 | `/register` | 用户名（`^[a-zA-Z0-9_]{4,32}$`）、密码（8~32 位含字母与数字）、昵称（可空，缺省取用户名） | `POST /auth/register` | `2003` 用户名已存在；`1001` 展示字段级错误（`data` 为字段错误列表）；成功后自动登录并跳首页 |

#### 练习答题页（专项）

- **加载/作答**：`POST /practice/papers` 一次性取回含答案的题目列表（整页骨架 loading）；选中即判分（`judgeLocally`），客观题立即高亮对错并展开 `analysis`（`MarkdownRenderer` 渲染），主观题/论文显示「本节不判分，以下为参考答案」+ `referenceAnswer` + `scorePoints`。
- **切题**：上一题/下一题；键盘 `←/→` 切换，`1-9`/`A-F` 选选项，`Enter` 下一题。**收藏**：乐观更新 `isFavorited`，失败回滚并提示。
- **上报**：每答满 10 题或离开题目调用 `flushRecords(source)`；`onBeforeUnmount`/路由守卫兜底调用一次；`1002` 限流时保留 `pendingRecords` 延后重试。
- **顺序练习**：进入时 `GET /practice/progress` 取 `nextSequence` 并作为 `fromSequence` 传入；每次推进后 `PUT /practice/progress`（debounce 3s）。
- **边界**：`SAMPLE` 且未登录 → 顶部提示「样例题共 5 题，注册后可继续练习」，答满后引导注册；`total = 0` → 空态「该范围暂无题目」+「换范围」按钮；`onlyUnmastered` 取不到题 → 空态说明。
- **错误态**：`3001` 科目停用 → 整页空态；`1001` → 提示参数问题并返回科目详情。

#### 模拟考答题页（专项）

- **布局**：左侧题干区，右侧固定 `AnswerSheet`；顶部为倒计时 + 分节 Tab + 已答计数；答题页用 `BlankLayout`，隐藏导航菜单以降低误操作。
- **倒计时**：显示 `displaySec`（`hh:mm:ss`）；剩余 ≤5 分钟标红并提示；不做任何本地跳转（见 7.4.4）。
- **场次与分节切换**：场次 Tab 仅允许进入已结束/当前场次；`PENDING` 场次不跑倒计时，只显示「开始本场次」按钮（高级科目论文场次的落点）。分节 Tab 仅允许进入已完成/当前分节；「结束本节」**无结转勾选框**（节余时长在"场次内总时长共享"下自动可用，对应评审 B11），仅做二次确认；`allowEarlySubmit = false` 的分节（论文）不显示结束按钮，仅提示「本分节须待本场次时长达标后整体交卷」。
- **`NOT_SUPPORTED` 分节**：Tab 置灰不可进入，`AnswerSheet` 中该分节题目置灰不可点；若当前分节为 `NOT_SUPPORTED`，只渲染 `SectionTip`（文案：本平台无法模拟 Office 操作题，请前往官方机考模拟练习平台），无作答区。
- **保存**：切题即 `setAnswer`；顶部保存状态指示（保存中/已保存 `hh:mm:ss`/保存失败可点击重试）；保存失败保留 `dirty` 并节流重试，连续失败 3 次提示「网络异常，请检查连接后重试」，不阻断本地作答。
- **交卷确认**：弹对话框列出 `unansweredCount` 与按分节统计的已答/未答（`SectionVO.questionCount`），二次确认；期间 `submitting = true` 禁用按钮；先 `flushSave()` 再 `submit()`；返回摘要后 `replace` 到成绩页；若 `4005`（超时已自动交卷）→ 提示后同样跳成绩页（`GET /exams/{id}/result` 幂等可读）。
- **离开拦截**：`onBeforeRouteLeave` 弹「离开后计时仍继续，是否确认离开？」，确认后 `flushSave()`。
- **无权限/异常**：`1004`/`403` → 整页「无权访问该会话」；`4004` → 「模拟考会话不存在」。

#### 成绩详情页（专项）

- **客观题得分区**：三个数字卡 —— `objectiveScore` / `autoScoredMaxScore` / `totalMaxScore`，明确口径文案「客观题得分（唯一参与判定的分数）」「可判分满分」「卷面满分」，展示 `accuracy` 与 `usedSec`（格式化为 `xh ym`）。
- **主观题分区**：遍历 `sectionResults`，当 `sectionType !== 'OBJECTIVE'` 时，分区标题右侧固定标注「本节不判分，仅提供参考答案」，且**不渲染任何分数**（`scored: false` 时连得分列都不出现）；`supportStatus = NOT_SUPPORTED` 的分节显示 `SectionTip` 提示文案。
- **逐题回顾**：`items` 为 `QuestionDetailVO[]`，展示 `userAnswer`、`correctAnswer`（客观题）、`referenceAnswer` + `scorePoints`（主观题/论文）、`analysis`；客观题按 `isCorrect` 展示对/错标记，主观题 `isCorrect` 为 `null` → 只显示「已作答/未作答」。
- **补充提示**：页脚固定提示「本次为模拟，官方须一次性通过全部科目」（02 文档 2.1.1）。
- **边界**：`items` 为空 → 提示数据异常并提供重新加载；`GET /exams/{id}/result` 在 `SUBMITTED` 但未 `SCORED` 时按后端约定轮询一次（间隔 1s，最多 3 次），仍失败则提示稍后查看；`4004` → 空态。

### 7.5.2 管理端

| 页面 | 路径 | 核心区块 | 调用接口 | 关键交互与边界状态 |
| --- | --- | --- | --- | --- |
| 数据概览 | `/admin/dashboard` | 指标卡（题目总量/待审核量/用户数/近 7 天答题量）、趋势图 | `GET /admin/dashboard` | 无权限 → `403` 提示并跳首页 |
| 题目列表 | `/admin/questions` | 筛选栏（`subjectId/status/questionType/outlineId/kpId/sourceType/batchId/keyword`）、表格（`stemSummary`/`difficulty`/`status`/`sourceType`/`batchNo`/`updatedAt`）、批量操作、分页 | `GET /admin/questions`、`DELETE /admin/questions/{id}`、`POST /admin/questions/{id}/submit-review`（或 `publish` / `offline`）、`POST /admin/questions/import`、`GET /admin/batches`、`GET /admin/subjects`、`GET /admin/outline`、`GET /admin/knowledge-points` | 状态列用 Tag 配色（`DRAFT/PENDING_REVIEW/APPROVED/REJECTED/PUBLISHED/OFFLINE`）；删除仅 `DRAFT`/`REJECTED` 可见，否则 `3003` 提示；`offline`/`publish` 二次确认；导入为 `text/plain` JSONL（≤500 行）文本域 + 结果表（`imported/skipped/errors[{line,reason}]`）；空态区分「无数据」与「筛选无结果」 |
| 题目编辑 | `/admin/questions/:id/edit` | 基本信息（`subjectId`/`outlineId`/`questionType`/`difficulty`/`kpIds`/`sourceType`/`sourceRef`/`sourceLicense`）、题干（Markdown + 公式 + 图片）、选项与答案、解析 | `GET /admin/questions/{id}`、`POST /admin/questions`、`PUT /admin/questions/{id}`、`POST /admin/files` | 题型驱动表单：客观题显示 `options`/`correctAnswer`（`MULTI_CHOICE` 多选、提交前按字典序规范化），`SUBJECTIVE`/`ESSAY` 隐藏 `correctAnswer` 并强制 `referenceAnswer` + `scorePoints`；`kpIds` 1~5 个校验；`sourceType = AI_GENERATED` 时必填批次；`3004` 提示题目内容不完整；`3005` 重复检测 → 对话框展示 `data.similarQuestionId` 并允许「强制保存」（`force=true`）；图片上传走 `POST /admin/files`（`file` 字段，jpg/jpeg/png/gif/webp ≤5MB），成功插入 `/uploads/...` 到题干 |
| 审核工作台 | `/admin/review` | 左：待审队列（按批次/科目筛选）；中：题目预览（题干 + 选项 + 答案 + 解析，全字段）；右：知识点校正、驳回原因输入；底部操作条 | `GET /admin/questions?status=PENDING_REVIEW`、`GET /admin/questions/{id}`、`POST /admin/questions/{id}/review`、`POST /admin/questions/batch-review`、`POST /admin/questions/{id}/regenerate`、`GET /admin/batches`、`GET /admin/knowledge-points` | 见下方专项说明 |
| 生成批次 | `/admin/batches` | 批次列表（目标/模型/Prompt 版本/产出与审核计数/状态）、批次详情抽屉 | `GET /admin/batches`、`GET /admin/batches/{id}`、`POST /admin/batches` | 状态 `PENDING/GENERATING/GENERATED/REVIEWING/COMPLETED/FAILED` 用 Tag 展示；创建批次后提示「等待离线脚本领取」；`FAILED` 展示失败原因 |
| 科目维护 | `/admin/subjects` | 表格（含 `DISABLED`）、新增/编辑抽屉、启用/停用 | `GET /admin/subjects`、`POST /admin/subjects`、`PUT /admin/subjects/{id}`、`PATCH /admin/subjects/{id}/status` | 停用需二次确认；`code` 唯一性由服务端校验（`1001` 展示字段错误） |
| 大纲维护 | `/admin/outline` | 科目选择 + 树（`level` ≤3）、新增/编辑节点、拖拽排序 | `GET /admin/outline?subjectId=`、`POST /admin/outline`、`PUT /admin/outline/{id}`、`DELETE /admin/outline/{id}`、`PUT /admin/outline/sort` | 未选科目时不加载；拖拽结束批量提交 `[{id,parentId,sortNo}]`；删除时 `1005`「有题目引用」→ 提示不可删；`origin` 字段可编辑（`OFFICIAL_TITLE`/`MANUAL`） |
| 知识点维护 | `/admin/knowledge-points` | 表格（含 `aliases`）、新增/编辑、合并 | `GET /admin/knowledge-points`、`POST /admin/knowledge-points`、`PUT /admin/knowledge-points/{id}`、`DELETE /admin/knowledge-points/{id}`、`POST /admin/knowledge-points/merge` | 合并选择多个 `sourceIds` + 一个 `targetId`，二次确认；删除 `1005` 提示被引用 |
| 卷面模板配置 | `/admin/paper-templates` | 模板头（`name`/`totalDuration`/`totalMaxScore`/`autoScoredMaxScore`）+ 分节表格（可增删行、上下移动） | `GET /admin/paper-templates/{subjectId}`、`PUT /admin/paper-templates/{subjectId}` | 见下方专项说明 |
| 反馈处理 | `/admin/feedback` | 列表（类型/状态/题目摘要）、处理对话框（`status`/`handleRemark`/`offlineQuestion`）、跳转题目编辑 | `GET /admin/feedback`、`POST /admin/feedback/{id}/handle` | `COPYRIGHT` 类型置顶并标红（02 文档 2.6.3）；`offlineQuestion = true` 时二次确认（高危操作）；处理后跳 `/admin/questions/:id/edit` 复核 |
| 用户管理 | `/admin/users` | 列表（`keyword` 匹配用户名/昵称）、状态、重置密码 | `GET /admin/users`、`PATCH /admin/users/{id}/status`、`POST /admin/users/{id}/reset-password` | 禁用与重置密码均二次确认；重置后弹出一次性明文密码（仅展示一次，需用户确认已复制） |
| 系统配置 | `/admin/configs` | 配置项表单（键值） | `GET /admin/configs`、`PUT /admin/configs` | 保存前展示 diff 确认；未知配置项原样透传，前端不硬编码配置键语义 |

#### 审核工作台（专项）

- **队列与导航**：左栏只列 `status = PENDING_REVIEW` 的题目（`stemSummary` + 科目 + 批次 + 题型），支持按 `batchId`/`subjectId` 过滤，当前项高亮。
- **逐题预览**：中栏用 `QuestionCard` 的 `mode="review"` 渲染全字段（题干 Markdown + KaTeX + 图片、选项、`correctAnswer`、`analysis`、`difficulty`、`kpIds`、`sourceType`/`sourceRef`），并提供「编辑」入口跳 `/admin/questions/:id/edit`。
- **通过**：`POST /admin/questions/{id}/review`，`{ action: 'APPROVE' }` → 队列自动跳下一题。**驳回**：`remark` 非空校验（≤200 字）后 `{ action: 'REJECT', remark }`；驳回项显示「退回重生成」按钮 → `POST /admin/questions/{id}/regenerate`（`REJECTED → DRAFT`，二次确认并提示「题目将回到 DRAFT，可重新编辑或由脚本重新生成」）。**批量通过**：勾选多项 → `POST /admin/questions/batch-review`，`{ ids, action: 'APPROVE' }`，`ids` 上限 100（超出前端分批）。
- **知识点校正**：右栏可增删 `kpIds`（**0~5 个**），保存走 `PUT /admin/questions/{id}`，提示「校正知识点会新建题目版本（`version` +1）」。0 个只允许作为**暂存态**（AI 造题时标签匹配不上的题就是这样导入的）；**发布时**必须至少 1 个，由服务端在 `POST /admin/questions/{id}/publish` 校验并返回 `1005`，前端在"通过并发布"前给出明确提示（对应评审 B4）。
- **键盘快捷键**（队列获焦时生效，输入框内不触发）：`A`/`Enter` 通过 · `R` 聚焦驳回原因 · `Ctrl/Cmd + Enter` 提交驳回 · `J`/`↓` 下一题 · `K`/`↑` 上一题 · `E` 打开编辑页 · `?` 快捷键速查浮层。
- **边界状态**：队列空 → 空态「当前没有待审核题目」；`3003`（状态非 `PENDING_REVIEW`，多为并发审核）→ 提示后刷新队列；保存知识点时也可能触发 `3005`；批量通过失败时列出失败 `id` 并保留勾选。

#### 卷面模板配置（专项）

- **两层编辑结构**：先按场次（`sittingNo`）分块，块内再编辑分节。场次行字段：`sittingNo`（自动从 1 连续自增，只读）、`sittingName`、`totalDuration`（分钟）、`totalMaxScore`、`autoScoredMaxScore`、`sortNo`。分节行字段：`sectionNo`（自动连续自增，只读）、`sectionName`、`sectionType`（`OBJECTIVE`/`SUBJECTIVE`/`ESSAY`）、`questionType`、`questionCount`、`perQuestionScore`、`durationMin`、`durationMax`、`allowEarlySubmit`、`supportStatus`（`SUPPORTED`/`NOT_SUPPORTED`）、`sortNo`。**没有 `allowCarryOver`**（字段已从 DDL 与接口删除，对应评审 B11）。
- 前端即时校验（与 05 文档 5.10 校验规则一致，服务端强制）：① 场次 `sittingNo` 与分节 `sectionNo` 均连续；② **`durationMin`、`durationMax` 均为必填**（对应评审 A6）；③ **`Σ(durationMin) ≤ 该场次 totalDuration ≤ Σ(durationMax)`**（对应评审 A6，需在表单内实时显示校验提示）；④ 场次 `autoScoredMaxScore` = 该场次内 `OBJECTIVE` 分节分值之和；⑤ 模板级 `autoScoredMaxScore`/`totalMaxScore` = 各场次之和（`NOT_SUPPORTED` 分节不计入任何满分口径）；
- 提供「按蓝图一键填充」按钮（初级/中级 1 场次、高级 2 场次），取值取自 02 文档 §2.4.2，减少手填出错。
- `questionCount` 超过该科目已发布单选题量时，服务端返回 `insufficientQuestions` 警告字段 → 前端以 `el-alert`（warning）提示「题量不足，保存后按现有题量组卷」，**不阻断保存**；整体提交为覆盖式 `PUT /admin/paper-templates/{subjectId}`。
- `NOT_SUPPORTED` 行的 `perQuestionScore` 置灰不计分。

## 7.6 通用组件设计

| 组件 | 位置 | Props | Events / Slots | 说明 |
| --- | --- | --- | --- | --- |
| `MarkdownRenderer` | `base/` | `content: string`、`allowImage?: boolean`（默认 `true`） | slot `default`（无 content 时） | markdown-it（`html: false`）+ KaTeX 渲染，`katex-markdown.ts` 单例实例；支持行内 `$...$` 与块级 `$$...$$`；图片用 `el-image` 带 `preview-src-list` 点击放大，`loading="lazy"`；渲染结果 `v-html` |
| `EmptyState` | `base/` | `description?: string`、`image?: 'default'\|'search'\|'error'`、`actionText?: string` | `@action` | 统一空态（练习无题、错题本为空、统计无数据、审核队列为空） |
| `PaginationBar` | `base/` | `total`、`pageNum`、`pageSize`、`pageSizes?: number[]`（默认 `[10,20,50,100]`）、`loading?: boolean` | `@change({pageNum,pageSize})` | 封装 `el-pagination`，`pageSize` 上限 100（05 文档 5.1） |
| `ErrorBlock` | `base/` | `code?: number`、`message?: string`、`traceId?: string` | `@retry` | 错误态展示业务 `code` 与 `traceId`，便于用户反馈 |
| `QuestionCard` | `question/` | `question: QuestionVO \| QuestionDetailVO`、`itemId?: string`（考试场景）、`modelValue?: string`、`mode: 'practice'\|'exam'\|'review'\|'result'`、`judged?: boolean \| null`、`showAnswer?: boolean`、`disabled?: boolean` | `@update:modelValue`、`@favorite-change`、`@feedback` | 按 `questionType` 分支渲染；`mode='exam'` 时**永不渲染答案相关字段**；`mode='result'` 时主观题只展示参考答案与 `scorePoints`，不出现分数 |
| `AnswerSheet` | `question/` | `items: ExamItemVO[]`、`sections: SectionVO[]`、`answers: Record<string,string>`、`currentItemId?: string`、`mode?: 'exam'\|'review'` | `@jump(itemId)` | 按 `sectionNo` 分组；图例：已答/未答/当前；`supportStatus = NOT_SUPPORTED` 的分节整组置灰且不可点击；每分节显示 `已答 x / questionCount` |
| `SectionTip` | `question/` | `section: SectionVO`、`variant?: 'not-supported'\|'no-early-submit'` | — | `NOT_SUPPORTED`：显示分节名、题型说明与「本平台无法模拟 Office 操作题，请前往官方机考模拟练习平台」；`no-early-submit`：提示「本分节须待考试总时长达标后整体交卷」 |
| `EssayEditor` | `question/` | `modelValue: string`、`itemId: string`、`sessionId?: string`、`remainingLabel?: string`、`readonly?: boolean` | `@update:modelValue`、`@draft-saved` | 论文/主观题写作框：① 顶部展示计时（透传 `remainingLabel`，由 store 提供，组件不自行计时）；② 实时字数统计（去除空白字符，与「不少于 N 字」提示联动）；③ localStorage 草稿自动保存（见下）；④ 支持 Markdown 预览切换与纯文本导出（`Blob` + `a[download]`） |
| `QuestionFeedbackDialog` | `question/` | `questionId: string`、`visible: boolean` | `@update:visible` | `type ∈ TYPO/WRONG_ANSWER/UNCLEAR_ANALYSIS/COPYRIGHT/OTHER`，`content` 5~1000 字；`1002` 超限提示「24 小时内最多提交 3 次」 |

**`EssayEditor` 草稿策略**

- 键：`rk:essay:{sessionId}:{itemId}`（由 `utils/storage.ts` 统一拼接前缀 `rk:`）；考试结束或交卷成功后清理该会话全部草稿。
- 写入：`input` 事件 debounce 2s；此外 `blur`、路由离开、`beforeunload` 立即写入。
- 恢复：进入卡片时若服务端 `userAnswer` 为空且本地草稿非空 → 弹提示「检测到未提交的草稿（保存于 hh:mm:ss），是否恢复」。
- 与模拟考自动保存的关系：localStorage 只防「未提交内容因刷新/崩溃丢失」，**不参与服务端 `PUT /exams/{id}/answers`**；两者互不替代。

**`QuestionCard` 题型分支**

| `questionType` | 作答控件 | 判分展示（`mode='practice'` / `'result'`） |
| --- | --- | --- |
| `SINGLE_CHOICE` | `el-radio-group`（`options[].key` A~F） | 正确选项标绿、错选标红，展示 `correctAnswer` |
| `MULTI_CHOICE` | `el-checkbox-group` | 同上，提示「须全部选对才得分」 |
| `TRUE_FALSE` | `el-radio-group`（正确/错误） | 同上；选项由前端按题型生成，不依赖 `options` |
| `SUBJECTIVE` | 多行 Markdown 编辑框 + 预览 | **不判分**：显示「本节不判分，以下为参考答案」+ `referenceAnswer` + `scorePoints` |
| `ESSAY` | `EssayEditor` | 同上 + 计时与字数统计 |

## 7.7 请求封装（`utils/request.ts`）

```ts
const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE, // '/api/v1'
  timeout: 15000,
})
```

**请求拦截器**：从 `userStore.token` 注入 `Authorization: Bearer <token>`（Sa-Token 的 `token-name = Authorization`、`token-prefix = Bearer`，05 文档 5.1）；为每个请求生成 `configKey` 用于去重；支持 `config.silent = true`（调用方自行处理错误，拦截器不弹提示）。

**响应拦截器（核心契约）**

| 阶段 | 行为 |
| --- | --- |
| 解包 | 成功时**直接返回 `data`**（即业务数据），调用方拿到的就是 `SubjectVO[]`、`ExamSessionVO` 等，不再写 `res.data.data` |
| `code === 0` | 返回 `data`（`data` 为 `null` 时返回 `null`） |
| `code === 1001` | 展示字段级错误：`data` 为字段错误列表 → 用 `el-message` 拼接首条或交给表单 `setFields`；通过 `reject` 抛出携带 `code`/`data` 的错误 |
| `code === 1002` | 提示「请求过于频繁，请稍后再试」 |
| `code === 1003` / `3002` / `4004` | 提示资源不存在/题目不存在/会话不存在，交由页面进入空态 |
| `code === 1004`（HTTP 403） | 提示「无权限访问该资源」；**不跳转**（可能是局部操作越权） |
| `code === 2005`（HTTP 401） | 清 token → `ElMessage.warning('登录已过期，请重新登录')` → 跳 `/login?redirect=<当前 fullPath>`；若是登录接口本身则只提示 |
| `code === 3005` | 抛出错误并把 `message` 交给页面（重复题对话框） |
| `code === 4003` / `4005` | 默认 `silent` 处理：不弹通用提示，由 `exam` store 按 7.4.4 逻辑接管 |
| 其他非 0 | `ElMessage.error(message + '（traceId: ' + traceId + '）')` —— 统一展示 `traceId`，便于用户反馈时定位日志 |
| 网络/超时/5xx | 提示「网络异常，请稍后重试」；超时与 `ECONNABORTED` 单独文案 |

统一抛出的错误对象结构：`{ code: number, message: string, data: any, traceId?: string, isBusiness: boolean }`，供页面 `try/catch` 分支处理。

**并发与重复请求处理**

- 去重（仅 `GET`）：`pendingMap: Map<string, Promise<any>>`，key = `method + url + JSON.stringify(params)`，命中则复用同一 Promise（结束后删除）。POST/PUT/PATCH 一律不合并（作答保存、交卷必须逐次到达服务端）。
- 取消：全局维护 `Map<configKey, AbortController>`；路由切换（`router.afterEach` / 组件 `onBeforeUnmount`）调用 `cancelAll()`，避免「旧页数据渲染到新页」。
- 串行：作答保存、反馈提交等写操作内部走 `Promise` 队列，避免同 `itemId` 乱序覆盖。

**文件上传封装（`utils/upload.ts`）**

```ts
export async function uploadImage(file: File): Promise<{ url: string; size: number }>
// 1) 前置校验：扩展名 ∈ jpg/jpeg/png/gif/webp，size ≤ 5MB（与 05 文档 5.10 第 51 项一致）
// 2) FormData 追加字段名 'file'（不得改名为 image/file[] 等）
// 3) POST /api/v1/admin/files，Content-Type 由浏览器自动带 boundary
// 4) 返回 { url: '/uploads/2026/09/25/xxx.png', size }，前端以 url 拼接写入题干 Markdown
```

> 决策与理由
> 不封装「分片上传 / 断点续传」：图片单文件 ≤5MB，且题库高效期上传发生在管理端人工操作，分片带来的复杂度（合并接口、临时文件清理）远超收益。上传失败直接重试即可（接口幂等，重复上传只产生孤儿文件，由部署侧清理任务兜底）。

## 7.8 权限与安全

- **前端权限仅用于体验层**：隐藏无权限菜单、路由守卫拦截、按钮禁用。真正的鉴权与越权校验**全部在服务端**：`/api/v1/admin/**` 要求 `role = ADMIN`（05 文档 5.13），用户数据访问由服务端强制携带当前登录用户 ID（03 文档 AC-3）。**前端任何"权限判断"都不得被视为安全边界。**
- 管理端菜单由 `admin.routes.ts` 生成，仅当 `userStore.isAdmin` 时渲染；`requiresAdmin` 守卫在角色不足时提示并回首页。
- 越权请求的权威响应是 `403 → 1004`（01 文档 1.5：越权访问必须返回 403 而非 404），前端只负责提示。

> 决策与理由
> **token 存放于 `localStorage`（key `rk:token`）**，不使用 Cookie。
> 理由：① 后端 Sa-Token 从 `Authorization` 请求头读取 token，不使用 Cookie，因此无需 `withCredentials`、无 CSRF 面、无跨域 Cookie 的 `SameSite`/`Secure` 配置成本；② 本项目为纯前端 SPA，无 SSR，不存在「必须在服务端读 token」的场景；③ 刷新页面需要保持登录态，`sessionStorage` 在标签页复制/新开时丢失，体验更差。
> 风险与缓解：`localStorage` 可被 XSS 读取。缓解手段是**三处同时收紧**——题干/解析等富文本由服务端白名单过滤（01 文档 1.5）；前端 `markdown-it` 关闭 `html: true`，且不渲染来自用户的任意 HTML；全站无第三方脚本注入点。**已知残留风险**：若前端出现 XSS 漏洞，token 可被窃取；彻底消除需 httpOnly Cookie，但会与「token 走 Header」的既定契约（05 文档 5.1）冲突，故接受当前方案。
> token 失效处理：`2005`/401 → 清 token、清 `userInfo`、跳登录并携带 `redirect`；不做静默刷新（后端无 refresh token 机制）。

## 7.9 样式与适配

**设计变量（`styles/variables.scss`，同时映射为 CSS 变量供运行时使用）**

| 类别 | 变量 | 取值 |
| --- | --- | --- |
| 主色 | `$rk-primary` | `#2a72c2`（**不是** Element Plus 默认的 `#409eff`：后者在白底只有 2.78:1、页面底 2.59:1，连 craft floor 的"图形 ≥3:1"都过不了，且正是用户列为禁区的"培训机构促销感"默认蓝。`#2a72c2` 白底 4.90:1 / 页面底 4.57:1，刻意留余量而非取"刚好过线"的 `#2f79c4`(4.51:1)。取舍与实测见 `18` 的 `F11`/`F14`）；`$rk-success #67c23a`、`$rk-warning #e6a23c`、`$rk-danger #f56c6c`、`$rk-info #909399` |
| 文本 | `$rk-text-1/2/3` | `#303133` / `#606266` / `#909399` |
| 边框/背景 | `$rk-border` / `$rk-bg-page` / `$rk-bg-card` | `#dcdfe6` / `#f5f7fa` / `#ffffff` |
| 间距 | `$rk-space-xs/s/m/l/xl` | `4 / 8 / 12 / 16 / 24`（px） |
| 圆角 | `$rk-radius-s/m/l` | `2 / 4 / 8` |
| 字号 | `$rk-font-xs/s/m/l/xl/xxl` | `12 / 14 / 16 / 20 / 24 / 30` |
| 行高 | `$rk-line-height` | `1.6`（题干长文本可读性） |
| 布局 | `$rk-page-min-width` | `1280px`；内容区最大宽度 `1200px`；管理端侧栏 `220px`（折叠 `64px`）；答题页右栏 `280px` |

**布局宽度策略**：桌面优先，`body { min-width: 1280px }`，低于该宽度时保留横向滚动条而不做重排（01 文档 1.5）。移动端**保证可用**（不遮挡、可滚动、按钮可点），不做断点适配、不做移动端专属布局。

**Element Plus 主题定制**

- **方式：只覆盖运行期 CSS 变量**（`web/src/styles/element-override.scss` 在 `:root` 给出 `--el-color-primary` 及其派生档、`--el-text-color-*`、`--el-border-radius-base` 等），**不碰 Element Plus 的 SCSS**。
- ⚠️ **不要**用 `@forward 'element-plus/theme-chalk/src/common/var.scss' with ($colors: …)` 做主题定制——在本项目里那是**死代码**。`ElementPlusResolver()` 默认 `importStyle: 'css'`，按需引入拿到的是 Element Plus 的**预编译 CSS**（其中 `--el-color-primary` 已经是字面量 `#409eff`），这里配置的 SCSS 变量从头到尾没有参与那次编译。实测证据：把 `@forward` 的 base 从 `#409eff` 改成 `#2a72c2` 后重新构建，产物里仍是 `--el-color-primary:#409eff`（`18` 的 `F14`）。**`element-override.scss` 的上一版正是这么写的**，本条即是对它的纠正——只改颜色而不改机制，等于把**错方法**留在文档里。
- 派生档（`--el-color-primary-light-3/5/7/8/9`、`-dark-2`）按 Element Plus 的混合公式**现算**：`light-i` = 白与主色按 `i*10%` 混合、`dark-i` = 黑与主色按 `i*10%` 混合。主色再变时它们自动跟随，不抄一份字面量。
- 生效依赖加载顺序：`index.html` 里 `vendor-element` 的 `<link>` 排在本项目 index CSS **之前**，同为 `:root` 特异度时后出现的那条胜出（`21 §2.3`）。
- 个别运行期变量直接覆盖 CSS 变量（如 `--el-font-size-base`），不重复走 SCSS 编译。
- 不改 Element Plus 组件内部结构选择器（`.el-xxx__body`），只使用其暴露的变量与 `class`/`slot`，避免升级断裂。

## 7.10 性能与体验

| 措施 | 具体做法 |
| --- | --- |
| 路由懒加载 | 所有页面路由使用 `() => import()`；`admin.routes.ts` 全部懒加载，保证管理端代码不进考生端首屏（03 文档 3.4） |
| 分包 | `manualChunks` 见 7.2.1；`echarts`/`katex`/`admin` 各自成 chunk，仅相关页面加载 |
| 组件按需引入 | `unplugin-vue-components` + `ElementPlusResolver` 自动按需引入；不整包 `import ElementPlus` |
| ECharts 按需注册 | `echarts/core` + 显式 `use([LineChart, BarChart, RadarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])`；仅在 `stats` 页动态 import 图表封装组件 |
| 长列表策略 | 错题本、收藏、题目列表、反馈列表**一律分页**，不引入虚拟滚动 |
| 图片 | `el-image` 懒加载 + 点击预览；题干图片走 `/uploads` 静态托管 |
| 首屏体积目标 | 考生端首屏**同源传输字节** ≤ **350KB**。口径以 `scripts/verify/firstload.js` 为准：CDP `encodedDataLength`、冷缓存、`Document` + `Script` + `Stylesheet` 三类计和——**不是**"gzip 后 JS 之和"，故此处不列 chunk 名（旧版式子列了 `vendor-katex`/`vendor-markdown`，而 `dist/assets` 里根本没有这两个 chunk：`manualChunks` 的规则虽已声明，但模块尚未被 import 时不会产出产物）。**当前实测 379.85 KB，超预算 29.85 KB**，优化推迟到页面齐全后再做（`21 §5.6`；数据见 `18` 的 `F10`）。`admin` chunk 不参与首屏统计，gzip 单独 ≤ 300KB；`vendor-echarts` 仅在 `/stats` 与 `/admin/dashboard` 加载 |
| 交互反馈 | 列表 loading 用骨架屏/表格 `v-loading`；按钮提交态禁用；保存状态常驻指示（模拟考） |
| 缓存 | `subject` store 内存缓存 5 分钟（科目/大纲/知识点几乎不变）；答题页 `keepAlive` 仅在练习页开启 |

> 决策与理由
> **错题本/题目列表用分页而非虚拟滚动。** 理由：① 后端所有列表接口强制分页（03 文档 AC-4），`pageSize` 上限 100，单页数据量本身有界；② 单用户单科目错题量在几百量级、管理端题目列表单页 ≤100 行，虚拟滚动的收益（渲染万行）在此规模下不存在；③ Element Plus 的 `el-table` 不做虚拟滚动，强行引入 `el-table-v2` 或第三方虚拟列表需重写表格（定高、插槽、固定列、选中态全部要自己实现），维护成本高；④ 分页天然解决「用户需要看到总数与翻页定位」的需求，而虚拟滚动要额外实现滚动位置恢复。
> **例外**：模拟考答题卡一次渲染 75~85 个小方块（`AnswerSheet`），用普通渲染 + CSS Grid，不做虚拟化。

## 7.11 前端开发约定

**命名规范**

| 对象 | 规范 | 示例 |
| --- | --- | --- |
| Vue 组件文件 | PascalCase | `MarkdownRenderer.vue`、`QuestionCard.vue` |
| 页面文件 | kebab-case | `practice-session.vue`、`review-workbench.vue` |
| 其余文件 | kebab-case | `wrong-book.ts`、`katex-markdown.ts` |
| 组件内事件/props | camelCase | `modelValue`、`@favorite-change` |
| Pinia store | `useXxxStore` | `useUserStore`、`useExamStore` |
| api 函数 | `动词 + 名词`，camelCase | `fetchSubjects`、`createExam`、`saveExamAnswers`、`submitReview` |
| 路由 name | kebab-case | `exam-session`、`admin-question-edit` |
| 常量/枚举 | 全大写下划线 | `QUESTION_STATUS.PENDING_REVIEW` |

**`api/` 与 `views/` 的对应关系**：一个业务域一个 api 文件、一个目录；页面只允许从 `api/` 导入函数，禁止在 `.vue` 里直接 `axios` 或拼 URL。`api/admin/*` 一一对应 `views/admin/*`，便于按页面替换后端实现。

**TypeScript 类型放置（`web/src/types/api.d.ts`）**

```ts
// 手写维护，字段与 docs/05-api.md 完全一致
export interface ApiResult<T = unknown> { code: number; message: string; data: T; traceId: string }
export interface PageResult<T> { list: T[]; total: number; pageNum: number; pageSize: number }

export type QuestionType = 'SINGLE_CHOICE' | 'MULTI_CHOICE' | 'TRUE_FALSE' | 'SUBJECTIVE' | 'ESSAY'
export type QuestionStatus = 'DRAFT' | 'PENDING_REVIEW' | 'APPROVED' | 'REJECTED' | 'PUBLISHED' | 'OFFLINE'
export type ExamStatus = 'CREATED' | 'IN_PROGRESS' | 'SUBMITTED' | 'SCORED' | 'ABANDONED'
export type SectionType = 'OBJECTIVE' | 'SUBJECTIVE' | 'ESSAY'
export type SupportStatus = 'SUPPORTED' | 'NOT_SUPPORTED'

export interface UserVO { id: string; username: string; nickname: string; role: 'USER' | 'ADMIN'; status: string
  currentSubjectId?: string; lastLoginAt: string; createdAt: string }
export interface PaperSummary { totalDuration: number; totalMaxScore: number; autoScoredMaxScore: number
  sections: { sectionNo: number; sectionName: string; sectionType: SectionType; questionCount: number; supportStatus: SupportStatus }[] }
export interface SubjectVO { id: string; level: 'JUNIOR' | 'MIDDLE' | 'SENIOR'; code: string; name: string
  paperType: 'JUNIOR_MID' | 'SENIOR'; examPeriod: 'BOTH' | 'FIRST_HALF' | 'SECOND_HALF'; outlineStatus: 'FULL' | 'SKELETON'
  contentStatus: 'SAMPLE' | 'BULK'; questionCount: number; intro?: string; paperSummary?: PaperSummary }
export interface OutlineNodeVO { id: string; parentId: string; level: number; name: string; sortNo: number
  questionCount: number; children: OutlineNodeVO[] }
export interface QuestionVO { id: string; subjectId: string; outlineId: string; outlinePath: string[]; questionType: QuestionType
  stem: string; images: { url: string; alt: string }[]; difficulty: number; options?: { key: string; content: string }[]
  knowledgePoints: { id: string; name: string }[]; isFavorited: boolean }          // 不含答案字段
export interface QuestionDetailVO extends QuestionVO { correctAnswer?: string; referenceAnswer?: string
  scorePoints?: string[]; analysis?: string; isCorrect?: boolean | null; userAnswer?: string }
export type SittingStatus = 'PENDING' | 'IN_PROGRESS' | 'FINISHED'
export interface SectionVO { sittingNo: number; sectionNo: number; sectionName: string; sectionType: SectionType; questionCount: number
  maxScore: number; perQuestionScore: number; durationMin: number; durationMax: number; allowEarlySubmit: boolean
  usedSec: number; supportStatus: SupportStatus
  status: 'PENDING' | 'IN_PROGRESS' | 'FINISHED' | 'SKIPPED' }        // 无结转字段（对应评审 B11）
export interface SittingVO { sittingNo: number; sittingName: string; totalDurationSec: number; remainingSec: number
  status: SittingStatus; currentSectionNo: number; totalMaxScore: number; autoScoredMaxScore: number
  objectiveScore: number; startedAt: string | null; finishedAt: string | null; sections: SectionVO[] }
export interface ExamItemVO { itemId: string; questionId: string; sequence: string; sectionNo: number; questionType: QuestionType
  stem: string; images: { url: string; alt: string }[]; options?: { key: string; content: string }[]; score: number
  userAnswer?: string; answeredAt?: string; durationMs?: number }
export interface ExamSessionVO { sessionId: string; sessionNo: string; subjectId: string; subjectName: string; status: ExamStatus
  totalDurationSec: number; currentSittingNo: number; remainingSec: number; currentSectionNo: number; questionCount: number
  autoScoredMaxScore: number; totalMaxScore: number; sittings: SittingVO[]; items: ExamItemVO[] }  // items 不含答案
// 会话级 totalDurationSec = 各场次之和（展示用）；remainingSec / currentSectionNo 均为「当前场次」的值，真值以 sittings[] 为准
export interface ExamResultVO { sessionId: string; sessionNo: string; subjectName: string; objectiveScore: number
  autoScoredMaxScore: number; totalMaxScore: number; accuracy: number; usedSec: number; submittedAt: string; scoredAt: string
  sittingResults: { sittingNo: number; sittingName: string; totalDurationSec: number; usedSec: number
    objectiveScore: number; autoScoredMaxScore: number; totalMaxScore: number; status: SittingStatus; answered: boolean }[]
  sectionResults: { sittingNo: number; sectionNo: number; sectionName: string; sectionType: SectionType; maxScore: number
    objectiveScore: number; supportStatus: SupportStatus; scored: boolean }[]; items: QuestionDetailVO[] }
// sittingResults[].answered = false 表示该场次未作答（论文放弃时），成绩页按其分区标注「未作答」
// 其余接口（错题本项、统计项、批次、模板、反馈、用户管理）按同风格补齐；所有 ID 类型为 string
```

> 决策与理由
> **不自动生成接口类型**（如 openapi-typescript 从 Knife4j 的 `/v3/api-docs` 生成）。
> 理由：① 生成链要求在构建期运行后端或拉取远端 JSON，会引入「前端构建依赖后端可访问」的耦合，与 CI 中前后端独立构建冲突；② 生成出的类型是 Java 视角（`Long` → `number` 会丢精度，本项目 ID 一律以 string 传输，见 05 文档 5.1），需要再写一层覆盖规则，规则本身又要维护；③ 本项目接口约 70 个、VO 约 20 个，手写总量可控且能附中文注释；④ 05 文档是**唯一契约来源**，手写 d.ts 与文档一一对照更易审查。
> 代价与强制措施：`05-api.md` 变更时必须同步改 `types/api.d.ts`（列为提交前检查项，见 7.11 代码质量）。若将来接口数突破 200 个，再评估引入生成工具。

**代码质量**

- ESLint：`eslint-plugin-vue`（`vue3-recommended`）+ `@typescript-eslint/recommended` + `eslint-plugin-import`（禁止 `views/` 直接 import `axios`）；`prettier` 处理格式（2 空格、单引号、无分号、`printWidth: 120`）。
- 类型检查：`pnpm type-check`（`vue-tsc --build`）必须零错误。
  > **命令在本轮由 `vue-tsc --noEmit` 改为 `vue-tsc --build`**（P2 回填）。原因：`tsconfig.json` 是 solution 形式（`files: []` + `references` 指向 `tsconfig.app.json` / `tsconfig.node.json`），而 `vue-tsc --noEmit` 不带 `-p` 或 `--build` 时**一个文件都不检查**，直接以退出码 0 结束——此前各轮记录的"类型检查通过"因此什么都没验证过。`--build` 会沿 `references` 依次检查两个工程（`src/**` 与 `vite.config.ts`），`noEmit: true` 由 `@vue/tsconfig` 基座给定，不会写出任何产物。改为 `--build` 后立即暴露并修掉了 3 个真实错误（`utils/request.ts` 的 axios 泛型、管理端两处），详见 `24`。
- 提交前检查项：`pnpm lint` → `pnpm type-check` → `pnpm build`；确认 `grep "axios" web/src/views` 无结果；确认新接口已在 `types/api.d.ts` 与 `05-api.md` 同步；确认新增页面已登记到路由表与页面清单（本文 7.3/7.5）。

## 7.12 验收标准（自检清单）

**鉴权与权限**

- [ ] 未登录访问 `/wrong-book`、`/stats`、`/admin/**` 被重定向到 `/login?redirect=<原路径>`，登录后回到原路径；已登录访问 `/login` 被重定向到 `/`（或 `?redirect=` 目标）。
- [ ] 未登录访问 `/admin/dashboard` 时管理端代码未加载；普通用户直接输入 `/admin/questions` 被拦截并提示无权限。
  > 判据本轮改为**首屏 `index.html` 的 `modulepreload` 只有 `vendor-*.js` / `vendor-element-*.js` 两条**（旧表述"Network 中无 `admin` chunk 请求"依赖一个已删除的手工 chunk 名，见 §7.2.1；且它说反了——那样命名恰恰会把管理端拉进首屏）。管理端每个页面各自成 chunk，加载与否看 Network 里有没有对应的 `subject-manage-*.js` 之类。
- [ ] 刷新任意受保护页面后登录态与角色判定仍正确（`GET /users/me` 恢复）；服务端返回 `403`/`1004` 时前端只提示，不误清 token。

**练习**

- [ ] 章节练习 `/practice/CHAPTER` 能取到含答案的题目并即时判分；`MULTI_CHOICE` 少选不得分；主观题只展示「不判分」+ 参考答案 + 评分要点，不出现分数。
- [ ] 顺序练习离开后重新进入从上次游标继续（`GET/PUT /practice/progress`）；游客 `SAMPLE` 模式题量 ≤5 且不产生答题记录上报。

**模拟考（M5）**

- [ ] 未配置卷面模板的科目无法发起模拟考（`4001`）；存在未完成会话时发起新考试被拦截，可选择继续或放弃（`4003`）。
- [ ] **模拟考刷新页面后仍能恢复作答内容与剩余时间**（`GET /exams/{id}` 重建，`remainingSec` 来自服务端）；页面切后台/休眠后不出现「本地先到 0」的错误交卷，回到前台剩余时间被服务端值纠正。
- [ ] 答题卡显示已答/未答且点击可跳转，`NOT_SUPPORTED` 分节不可进入且仅显示 `SectionTip`；论文分节不显示「结束本节」按钮，只能整体交卷。
- [ ] 交卷前先保存作答，重复点击交卷不产生重复判分（幂等）；超时自动交卷后前端经 `4005` 流程进入成绩页而非卡死。

**成绩**

- [ ] **主观题分区不显示分数**，标题旁标注「本节不判分，仅提供参考答案」；`NOT_SUPPORTED` 分节显示提示文案且不计分。
- [ ] 客观题得分区同时展示 `objectiveScore` / `autoScoredMaxScore` / `totalMaxScore` 三个口径。

**管理端**

- [ ] 审核工作台可用键盘完成「通过 / 驳回 / 切题」，输入框内不触发快捷键；批量通过 `ids` 超过 100 时前端自动分批。
- [ ] 题目编辑页按题型切换表单（客观题有答案、主观题只有参考答案），`3005` 重复题可强制保存。
- [ ] 卷面模板保存前本地校验四条规则，题量不足时提示 `insufficientQuestions` 但仍可保存；删除被引用的章节/知识点时正确提示 `1005`；重置密码明文只展示一次并提示已复制。

**工程**

- [ ] 所有接口路径与 `05-api.md` 完全一致（抽查 `PUT /api/v1/exams/{id}/answers`、`POST /api/v1/admin/questions/batch-review`）；`types/api.d.ts` 无自造字段（ID 为 `string`）。
- [ ] 业务错误提示包含 `traceId`；`pnpm lint`、`pnpm type-check`、`pnpm build` 全部通过，考生端首屏 gzip JS ≤ 350KB。
- [ ] 1280px 宽度下全站无横向滚动错位；更窄宽度下内容可横向滚动查看（不做移动端适配）。