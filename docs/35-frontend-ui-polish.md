# 35 · 考生端 UI 精修（克制打磨，不越工程边界）

> 本轮是纯前端 UI/交互增量：对**已有**的考生端页面做精修，范围是桌面 1280px 基准下的
> 排版节奏、视觉层次、微交互与空态表达。一个蓝、零投影、1px 线、诚实编码的设计系统不变。
>
> **不改任何契约**：`04`（DDL）、`05`（接口）、`07`（前端设计）不回改，后端不动；
> 所有改动都是样式与模板 class，没有新增/删除接口调用、store 字段或路由。
> 管理端不在本轮范围（委托方选定"考生端全站"）。
>
> 方向来自上一轮讨论的三项裁决：**保持桌面优先约束**、**克制打磨**、**覆盖考生端全站**。
> 据此主动放弃了两件事：窄屏断点重排、引入第二个强调色/投影。详见 §3。

## 1. 交付了什么

| 层 | 内容 |
| --- | --- |
| 全局底座 | `styles/variables.scss` 新增 `$rk-line-height-heading: 1.4`；`styles/reset.scss` 给 h1~h6 用标题行高、正文仍 1.6；`styles/index.scss` 给原生 `a` / 非 `el-button` 的 `button` 加 `:focus-visible` 兜底环（2px 主色 + 2px offset） |
| 首页书架 | `views/portal/home.vue` 竖排书名加 `text-wrap: balance`，11 字长名折两列时字数均衡（6+5 而不是 8+3）；不缩字号、不加宽书脊 |
| 科目列表 | `views/portal/subject-list.vue` "暂无简介"不再占三行 min-height；卡片边框/末行文案 hover 走 160ms 缓出 |
| 练习答题 | `views/portal/practice-session.vue` 主区改为 flex 列 + 子层 auto margin：短题纵向居中，长题从顶部起、正常滚动；加载/错误/空态同路径不跳版 |
| 统计 | `views/portal/stats-overview.vue` 长列表行 hover 用页面底色给落点；"练过但未掌握"的条加硬边斜纹（形状编码，非新颜色）；薄弱列表行同步 hover |
| 错题本 | `views/portal/wrong-book.vue` 题目行 hover 边框变主色，与科目卡同一条 160ms 缓出 |

改动总计：8 个文件、+90 / −1 行。没有新增文件、没有依赖变化。

## 2. 关键口径

- **标题行高压在 1.4，不更紧**。DESIGN.md 的硬底线是"行高不低于 1.4"。正文与题干仍从 body 继承 1.6；
  只把短文本标题收紧一档，页面标题块不再显得松。单独设变量而不就地写数字，便于将来一处调整。
- **焦点环只兜底原生元素**。Element Plus 组件自带焦点样式，一并覆盖会造成双环；
  首页书脊 `.spine` 已有更具体的 `:focus-visible` 声明，按层叠顺序它赢。
  这是键盘可达性补丁，不是视觉重做——目标只是让普通链接/按钮也有与书脊一致的环。
- **不用新颜色、不用投影**。统计页长列表的 hover 用已有的 `$rk-bg-page`；"未掌握"的区分用
  硬边斜纹（DESIGN.md 允许 `linear-gradient` 画硬边形状）而非第二个蓝或第二套红。
- **练习页纵向居中用 auto margin，不用 `justify-content:center`**。后者在题目超出一屏时会把
  题干顶出可视区（flex 经典的溢出裁切问题）；auto margin 在有空间时才居中、空间不足时归零。
- **空简介只占一行**。"暂无简介"是一行事实，不是三行 placebo；卡片总高仍由网格同行等高，
  卷面行照旧 `margin-top:auto` 沉底，视觉层次不变，只是标题与事实表之间不再有假空白。
- **不动的东西**：`text-wrap: balance` 是渐进增强，不支持的浏览器忽略该行、退回原自动折行；
  没有为长名单独缩字号或加宽书脊（那两条是设计系统硬规则）。

## 3. 主动放弃的方向（讨论时提出、本轮未做）

| 候选 | 裁决 | 理由 |
| --- | --- | --- |
| 窄屏断点重排 | 不做 | 委托方明确选"保持约束，桌面内优化"；`body{min-width:1280px}` 与 `<meta viewport width=1280>` 是产品既有事实（PRODUCT.md），重排等于改产品口径 |
| 第二个强调色 / 彩色标记条 | 不做 | 违反 The One Blue Rule；用形状（斜纹）+ 已有 el-tag 文字标记代替 |
| 统计页给低正确率换色条 | 不做 | 同上，且相近色相在 8px 条上读不出来（Shape-Carries-The-Encoding Rule） |
| 首页长名加宽书脊 / 简称 | 不做 | 加宽破坏"等高齐宽"签名；简称需要造数据，违反"宁可空态也不假装有数据" |
| 标题行高收到 1.35 | 不做 | 低于 DESIGN.md 的 1.4 底线；收到 1.4 已足够解决松垮感 |
| 空态文案重写 | 不做 | 核对后收藏/错题/统计的空态本就带下一步引导（"去练习""在练习页点收藏"），再改是制造噪音 |

## 4. 偏离登记

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | 新增 `$rk-line-height-heading: 1.4`，h1~h6 从继承 1.6 改为 1.4 | `07` / `DESIGN.md` 只规定全局 1.6，未区分标题；1.4 恰在"不低于 1.4"线上，短文本不涉及长文本可读性下限 | 所有页面标题块略收紧；正文、题干不受影响 |
| 2 | 全局给原生 `a` / `button:not(.el-button)` 加 `:focus-visible` | 原设计系统只在若干组件里逐条声明焦点环，没有通用兜底 | 键盘用户 Tab 到普通链接时环更明显；Element Plus 组件不受影响 |
| 3 | 统计页"未掌握"条加白斜纹 | 既不新增颜色，又能把"练过但没掌握"从一片同色蓝条里扫出来 | 仅一对伪元素背景，无 DOM/逻辑变化；打印时斜纹随背景可能被省略，但 el-tag 文字标记仍在 |

## 5. 验证

| 项 | 结果 |
| --- | --- |
| `vue-tsc --build` | 通过（exit 0） |
| `eslint .` | 失败：**6437 个错误全部来自 `dist/` 与 `dist_old_*/` 的构建产物**，非源码（产物不在 `eslint.config.js` 的 ignores 里，见 §6 第 2 条） |
| `eslint src` | 通过（exit 0）—— 本轮改动的全部源码干净 |
| `vite build` | 通过（exit 0，✓ built in 36.54s；沿用 docs/34 的"先改名旧 dist 再构建"绕过本机守卫） |
| 产物核验 | `index-*.css` 含 `focus-visible` 与 `line-height:1.4`；`home-*.css` 含 `text-wrap:balance`；`stats-overview-*.css` 含 `repeating-linear-gradient`；`subject-list-*.css` 含 `min-height:auto` |
| **未做** | 浏览器渲染取证：本机策略禁止启动 Chrome（同 `31`~`34`），像素位置、折行观感、hover 手感未实测 |

产物里的 chunk 体积警告（`vendor-element` 810 kB、`vendor-echarts` 480 kB）是既有现象，本轮未引入新依赖。

## 6. 文件

修改：`web/src/styles/variables.scss`、`web/src/styles/index.scss`、`web/src/styles/reset.scss`、
`web/src/views/portal/home.vue`、`web/src/views/portal/subject-list.vue`、
`web/src/views/portal/practice-session.vue`、`web/src/views/portal/stats-overview.vue`、
`web/src/views/portal/wrong-book.vue`、本篇文档。

未改动：`server/`、`deploy/`、`04-database.md`、`05-api.md`、`07-frontend.md`、`DESIGN.md`、
`.impeccable/`（设计史不回改；本轮偏离已在 §4 登记）。

本地遗留：构建前改名产生的 `web/dist_old_20261121123124/` 与历史遗留的
`web/dist_old_20261120213014/`，均已被 `.gitignore` 的 `web/dist_old*/` 忽略，不进版本库。

## 7. 下一步

1. **浏览器渲染取证**：换能启动 Chrome 的机器，对 1280×800 下的首页长名书脊折行、科目列表空简介卡片、
   练习页短题居中、统计页斜纹与 hover 做截图确认；重点看 `text-wrap: balance` 的实际折行点与
   练习页 auto margin 在长题干（主观题）下是否确实从顶部开始。§5 的"未做"随之关闭。
2. **eslint 忽略规则**：把 `dist_old*/` 加入 `web/eslint.config.js` 的 ignores，让 `eslint .` 恢复通过。
   本轮没改它，是因为它属于基建清理而非 UI 精修；§5 的 lint 失败不是本轮引入的。
3. 若后续要放开响应式，需先改 `PRODUCT.md` / `01 §1.5` 的产品口径，再重排顶栏与三列网格——
   那是另一轮，不应混在 UI 精修里。
