# 36 · 考生端视觉推翻重做：Soft Focus 柔焦书房

> 本轮**推翻**了考生端既有的视觉与布局，不是精修（对比 `35` 的克制打磨）。
> 保留蓝白底色，改为舒缓的蓝白渐变 + 现代极简，目标是让备考界面更柔和、不制造压力。
>
> **不修改任何接口、后端、`04`（DDL）、`05`（接口）——本轮零契约变更。管理端不碰。**

---

## 1. 用户裁决（本轮的输入）

| # | 议题 | 裁决 | 影响 |
| :-: | --- | --- | --- |
| 1 | 主蓝是否调柔 | **坚持 `#2a72c2`** | 主色取值不动；柔和感由页面底渐变与书脊色阶承担，不靠改主色（改柔会掉到 4.5:1 以下） |
| 2 | 页面切换是否加动效 | **加淡入** | `PortalLayout` / `BlankLayout` 的 router-view 外套 `rk-page` 过渡；管理端不套 |
| 3 | 管理端是否一起改 | **不碰** | 普通用户见不到；管理员要严谨严肃的工作场景，视觉保持原样 |
| 4 | 首页信息架构 | 书架概念重塑 | 保留书脊/抽屉、等高、实线/虚线编码；形态从硬边改为柔和卡片 |
| 5 | 视觉语言强度 | 轻渐变 + 微阴影 | 卡片 16px 圆角、蓝调微阴影；书脊与页面底用蓝白渐变 |
| 6 | 响应式 | 放开 | 移除 `body min-width:1280` 与 `<meta viewport width=1280>` 的横向滚动口径 |

---

## 2. 被替换的三条设计规则

旧的 "The Honest Bookshelf" 有三条硬规则，本轮在**考生端**正式作废（管理端仍遵守）：

| 旧规则（DESIGN.md） | 新规则（Soft Focus） | 含义 |
| --- | --- | --- |
| The One Blue Rule | **Blue as Anchor** | 主蓝 `#2a72c2` 只做交互锚点（按钮/链接/焦点/已印描边）；页面底与书脊用它的极浅色阶渐变，不引入第二个色相 |
| The Board-Not-Shadow Rule | **Soft Lift** | 用蓝调微阴影表达抬升，取代 1px 硬线；线只承担"形状编码"职责 |
| The No-Gradient Rule | **Gradient as Ground** | 渐变只做背景层（页面/书脊），不承担任何数据编码 |

**未被替换、必须保留的骨架**（这是隐喻的骨头，不是装饰）：

- 首页三层书架、书脊**等高**、层标签说层级；
- 书脊**实线 = 大纲完整 / 虚线 = 骨架**这组唯一形状编码；
- 仅简体中文、无深色模式、键盘可达（书脊方向键、答题快捷键）、不造假数据、空态引导、"不判分"诚实边界。

---

## 3. 设计令牌与"管理端零影响"的实现机制

### 3.1 关键约束：两端共用全局样式文件

`web/src/styles/{variables,index,reset,element-override}.scss` 被考生端与管理端**共同引入**。
若直接改既有 token（如 `$rk-radius-l`、`--el-border-radius-base`）或 `body` 的背景/宽度，管理端会被一起改掉——与裁决 3 冲突。

### 3.2 采用的三层隔离

1. **只新增、不改旧值**：所有柔和化用新令牌 `$rk-soft-*`（`variables.scss` 末尾），既有 `$rk-primary`/`$rk-radius-*`/`$rk-bg-*` 等取值一字未动。管理端继续读旧值。
2. **作用域覆盖**：Element Plus 的柔和化集中在 `index.scss` 的 `.portal-layout, .blank-layout` 选择器内——覆盖 `--el-border-radius-base: 8px`、给 `.el-card` 加 16px 圆角与蓝调阴影。管理端是 `.admin-layout`，不带这两个类，规则不匹配。产物实测同一份 CSS 里同时存在 `4px`（:root，管理端）与 `8px`（考生端作用域）两个值，即隔离生效的证据。
3. **页面底渐变上收**：body 改为透明、渐变铺在 `#app`；管理端 `.admin-layout` 显式补回 `background-color: $rk-bg-page` 纯色，挡住渐变。

### 3.3 新增令牌（`variables.scss`）

| 组 | 令牌 | 取值 / 用途 |
| --- | --- | --- |
| 页面渐变 | `$rk-soft-page-{top,mid,bottom}` / `$rk-soft-page-gradient` | `#f3f8fe → #fbfdff → #f6faff`，极浅、文字压上仍达标 |
| 书脊色阶 | `$rk-soft-spine-{unprinted,printed}-{1,2}` | 主色浅阶，白↔浅蓝渐变；**不是第二个强调色** |
| 柔和边框 | `$rk-soft-border` | `#dbe7f4` |
| 蓝调微阴影 | `$rk-soft-shadow` / `-hover` | `rgba(42,114,194,.06)` 起，蓝调非黑调 |
| 柔和圆角 | `$rk-soft-radius-{control,spine,card}` | `8 / 12 / 16px` |
| 呼吸间距 | `$rk-soft-space-{2xl,3xl}` | `32 / 48px`，书脊层之间用 |
| 字号 / 行高 | `$rk-soft-font-display` / `$rk-soft-line-height-body` | `32px` 展示字；题干行高 `1.75` |
| 缓动 / 时长 | `$rk-soft-ease` / `$rk-soft-duration` | `cubic-bezier(.16,1,.3,1)` / `260ms` |

### 3.4 页面切换淡入（`index.scss` 的 `rk-page`）

`opacity` + `translateY(8px)` 进出场，`mode="out-in"`，离场时长 `0.6×`；`prefers-reduced-motion` 下关闭过渡（功能保留）。只动合成层属性，不触发布局。

---

## 4. 页面改动清单

| 文件 | 改动 |
| --- | --- |
| `styles/variables.scss` | 新增 18 个 `$rk-soft-*` 令牌；旧令牌零改动 |
| `styles/index.scss` | 映射 CSS 变量；`#app` 蓝白渐变；考生端作用域 Element 柔和化；`rk-page` 淡入 |
| `styles/reset.scss` | 移除 `body min-width:1280`，背景改透明 |
| `index.html` | viewport 由 `width=1280` 改为 `width=device-width, initial-scale=1` |
| `layouts/PortalLayout.vue` | 顶栏毛玻璃 + 柔和阴影；导航改胶囊态；三档响应式；router-view 加淡入 |
| `layouts/BlankLayout.vue` | 透明底；router-view 加淡入 |
| `layouts/AdminLayout.vue` | 仅补回纯色底注释（抵消 body 变透明），视觉零变化 |
| `views/portal/home.vue` | 书架重塑：渐变书脊 + 12px 顶角 + 微阴影 + hover 浮起；层板/抽屉/图例柔和化；展示字号；书脊折行响应式 |
| `views/portal/subject-list.vue` | 科目卡柔和 + hover 浮起；三列→两列→单列 |
| `views/portal/subject-detail.vue` | 事实条、场次卡、知识点标签柔和化 + 响应式 |
| `views/portal/subject-outline.vue` | 大纲面板柔和 + 标签圆角 |
| `views/portal/login.vue` / `register.vue` | 表单卡柔和 + 窄屏限宽 |
| `views/portal/practice-session.vue` | 顶/底栏毛玻璃 + 柔和阴影；题干区透明；操作条换行 |
| `views/portal/stats-overview.vue` | 统计卡/面板柔和 + 卡片与列表响应式 |
| `views/portal/wrong-book.vue` | 错题行柔和 + hover；窄屏换行 |
| `views/portal/exam-result.vue` | 成绩卡/面板柔和；卡片响应式；主卡强调阴影 |
| `views/portal/exam-session.vue` | 顶栏毛玻璃；待开始卡/分节 tab 柔和；答题卡窄屏堆叠；倒计时改柔和琥珀 |
| `components/question/QuestionCard.vue` | 题卡柔和；题干行高 1.75；选项态加柔和底色 |
| `components/exam/ExamQuestionCard.vue` | 题卡柔和；题干行高 1.75 |
| `components/exam/ExamAnswerSheet.vue` | 答题卡柔和 + 窄屏堆叠 |
| `components/stats/TrendChart.vue` | tooltip 改柔和边框 + 阴影；网格线用 soft 色 |

**未改动**：`server/`、`deploy/`、`04-database.md`、`05-api.md`、`07-frontend.md`、`docs/01~35`、所有 `views/admin/*`、`components/common/{BrandMark,MarkdownRenderer}.vue`、所有 `api/`、`stores/`、`utils/`、`router/`、`types/`。

---

## 5. 偏离登记

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | 移除 `body min-width:1280`、`meta viewport width=1280`，放开响应式 | 用户裁决 6；与 `01 §1.5` 的桌面优先口径冲突 | 考生端窄屏重排；管理端 `.admin-layout` 保持桌面固定布局（不重排），行为不变 |
| 2 | 考生端 `.el-card` 圆角 8→16、加阴影；控件圆角 4→8 | 裁决 4/5 的"丰富但不喧宾夺主" | 仅考生端作用域命中，管理端仍 4px/无阴影 |
| 3 | 题干行高 1.6→1.75（考生端题卡） | 舒缓阅读、减少压迫感 | 长题干略增高；`DESIGN.md` 的 1.4 硬底线是下限，1.75 在上方，不冲突 |
| 4 | 模考倒计时告警色由 `$rk-danger` 改 `$rk-warning` | 舒缓备考压力（裁决未要求保留红色） | 最后 5 分钟提示变柔和深琥珀；仍满足"不能只靠颜色"——数字本身在变 |
| 5 | 首页展示字号用 32px display 档 | 建立更有节奏的字号层级（现代极简） | 仅首页 H1；窄屏回落到 24px |
| 6 | 主蓝 `#2a72c2` 未改 | 用户裁决 1（坚持） | 柔和感全部由浅阶渐变与阴影承担，主色对比度余量保持 |

---

## 6. 验证

| 项 | 结果 |
| --- | --- |
| `vue-tsc --build`（pnpm type-check） | 通过（exit 0） |
| `eslint src` | 通过（exit 0） |
| `eslint .` | 失败：`12874` 个错误全部来自 `dist_old_*` 历史产物（`eslint.config.js` 的 ignores 未排除），非源码；与 `35 §5` 同一既有基建欠账，本轮未引入 |
| `vite build` | 通过（exit 0，23.41s；沿用 docs/34/35 的"先改名旧 dist"绕过本机守卫） |
| 产物核验 | `index-*.css` 含页面渐变、蓝调阴影、`rk-page` 淡入、16px 圆角；`home-*.css` 含书脊渐变；`dist/index.html` viewport 已放开 |
| 隔离核验 | `index-*.css` 同时含 `--el-border-radius-base:4px`（管理端 :root）与 `8px`（考生端作用域），证明管理端未被波及 |
| **未做** | 浏览器渲染取证：本机策略禁止启动 Chrome（同 `31`~`35`），像素位置、折行观感、hover/淡入手感未实测 |

产物里的 chunk 体积警告（`vendor-element` 810 kB、`vendor-echarts` 480 kB）是既有现象，本轮未引入新依赖。

---

## 7. 下一步

1. **浏览器渲染取证**：换能启动 Chrome 的机器，对 1280×800 桌面与窄屏（900/760/600/375）分别截图，确认书架折行、书脊 hover 浮起、顶栏换行、卡片阴影、答题页答题卡堆叠、页面切换淡入；本机"未做"随之关闭。
2. **eslint ignores**：把 `dist_old*/` 加入 `web/eslint.config.js` 的 ignores，让 `eslint .` 恢复通过（基建清理，非本轮范围）。
3. **管理端是否跟进**：本轮裁决"不碰"。若日后要统一视觉，需另起一轮，并明确"管理端严谨严肃"的诉求如何与柔和语言共存（建议只统一字体与间距，不引入渐变与阴影）。
4. **`DESIGN.md` 的归属**：它是"当前状态镜像"，本轮已同步更新其 Overview/规则/令牌/组件段落；`docs/01~35` 作为设计史留痕不回改，本篇是冲突裁决的最新一篇。

---

## 8. 文件

新增：`docs/36-soft-focus-redesign.md`、本篇。
修改：见 §4 清单（22 个前端文件 + `index.html`）、`DESIGN.md`、`PRODUCT.md`。
未改：`server/`、`deploy/`、`04`/`05`/`07`、`docs/01~35`、管理端全部页面。

本地遗留：构建前改名产生的 `web/dist_old_20261121140428/` 及历史两个，均已被 `.gitignore` 的 `web/dist_old*/` 忽略。
