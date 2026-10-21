# 19 · P2 前端本轮未完成事项（交接）

> 这份文档只记录**本轮停下时还没做完的事**，供下一轮直接接手。
> 已落地的出入与取证在 [18](18-p2-frontend-deviations.md)，本文件不重复。
> 记录时间：2026-09-27。停下的原因是用户要求中止本轮。

---

## 1. 本轮结束时的状态：代码是绿的

| 项 | 命令 | 结果 |
| --- | --- | --- |
| lint | `pnpm lint` | `LINT_EXIT=0`，无输出 |
| 类型检查 | `pnpm type-check` | `TC_EXIT=0`，无输出 |
| 构建 | `pnpm build` | `BUILD_EXIT=0`，4.53s |

首页三项实测（生产预览 `pnpm preview --port 4173` + CDP，脚本在 [`logs/`](../logs/)）：

- 书脊 20 根，脊高 `200 / 133 / 67`（对应卷面满分 225 / 150 / 75），**全部 `overflowX=0`**；
- 色带已改为形状编码：`dy=1`（紧贴 1px 描边）、`86x6`、`BOTH` 实心 `rgb(42,114,194)`、`FIRST_HALF` 与 `SECOND_HALF` 为左右半边渐变；
- 900 宽不重排：`docScrollW=1280 > innerWidth=900`，20 根书脊宽度仍全为 `88px`。

---

## 2. 未完成：三件必须先做完的事

### （1）`docs/18` 的正文还没同步

以下内容**已经量出来了，但还没写进 [`docs/18`](18-p2-frontend-deviations.md)**：

1. **F14 条目本身还没有**（§2 表格的行、§3 的正文都缺）。但源码注释里已经引用它了——
   `grep -rn "F14" src/` 实测 **8 处，分布在 5 个文件**：`PortalLayout.vue` 2、`home.vue` 3、
   `reset.scss` 1、`variables.scss` 1、`subject-list.vue` 1，全都写着「见 docs/18 的 F14」。
   **现在是一组悬空引用，必须在下一轮补上条目，或改掉这 8 处注释。**
   （`variables.scss` 第 23 行还留了一张 `$rk-primary` 的候选对比度表，其中 `#2f79c4` 白底 4.51:1，
   注释里已写明「0.01 的余量对下一个微调的人是陷阱」，所以没采用——这条判断要一并写进 F14。）
2. **§5 的检测器取证是错的，要改。** 旧记录写「已加载样式表 0 条规则」「element-plus 0 处」，实测**两条都不成立**：

   | 新取证 | 结果 |
   | --- | --- |
   | 规则原文 | `.el-collapse-transition-leave-active,.el-collapse-transition-enter-active{transition:…max-height…, …padding-top…, …padding-bottom…}`（一条规则，两个选择器） |
   | 声明长度 | 213 字符（选择器+声明整体 215） |
   | 生产产物 | **存在**，在 `dist/assets/vendor-element-*.css` 里 |
   | dev 页面 | **存在**，但 Vite 以**内联 `<style>`** 注入（16 张表 / 430 条规则） |
   | 页面元素 | **0 个**元素带这个类 → 是**死规则** |
   | 旧记录为什么错 | 我上一版的 grep 上界是 `[^;}]{0,80}`，而 `padding-bottom` 出现在第 ~145 字符处。用同一个上界复跑，命中 `padding-bottom` 的 **0 条**——上界太短，不是规则不存在 |

   → 结论从「无从复现的误报」改成「**依赖带进来的死规则**」。检测器扫的是样式表规则而非渲染元素，所以它在 `/register` 这种没有书架的页面也报。
3. **F10 的首屏体积要换数**。旧记录 369.6 / 371.59 KB 的相加口径没有留痕，无法复现。改用真实传输字节（新脚本 [`logs/firstload.js`](../logs/firstload.js)，CDP Network + 冷缓存 + 生产预览）：

   **379.86 KB**（Document + Script + Stylesheet），预算 350 KB → **超 29.86 KB（+8.5%）**。构成：

   | 资源 | KB | 占比 |
   | --- | --: | --: |
   | `vendor-element-*.js` | 248.82 | 65.5% |
   | `vendor-*.js` | 106.54 | 28.0% |
   | `vendor-element-*.css` | 11.12 | 2.9% |
   | `index-*.js` | 5.93 | 1.6% |
   | `home-*.js` / `home-*.css` | 3.35 / 1.59 | 1.3% |
   | `index-*.css` / HTML | 1.50 / 0.86 | 0.6% |

   （另有运行时数据 `GET /api/v1/subjects` 5.04 KB，不计入。）
   **附带一条对裁决有用的实测**：按需引入是**真的生效**的——抽查 11 个未使用的组件类名（`el-tree` / `el-transfer` / `el-carousel` / `el-cascader` / `el-date-picker` / `el-upload` / `el-tabs` / `el-menu` / `el-dialog` / `el-backtop` / `el-timeline`）在产物 CSS 里**全部为 0**。所以超预算不是配置错了，是当前用量的真实成本。

### （2）主色这一个决定，卡在半路（**本轮最大的未决项**）

已定：`$rk-primary: #409eff`（[07](07-frontend.md) §7.9 钉死）**不能当文字和细线用**——白底 2.78:1、页面底 2.59:1，craft floor 要求文字 ≥4.5:1。
已做：新增 `$rk-primary-ink: #2a72c2`（白底 4.90:1 / 页面底 4.57:1），铺到品牌字标、导航当前项、链接、焦点环、书脊顶端色带、书脚小字，并加了 `--rk-primary-ink` 与浏览器自带界面的着色（选中色 / 光标 / 滚动条，见 [`index.scss`](../web/src/styles/index.scss)）。

**卡住的地方**：这样会**同屏出现两个蓝**——顶栏里「注册」按钮是 `#409eff`（Element Plus 主色填充），紧邻的「首页」当前项是 `#2a72c2`。而 `el-button--primary` 的白字压在 `#409eff` 上**仍是 2.78:1**，这是真正的文字不达标，目前只是被记为"已知未修"。

**两条路，需要用户裁决**（我倾向 B，但这是设计身份的取舍，不由我定）：

- **A（现状）**：保留 `$rk-primary` 做填充、`$rk-primary-ink` 做文字与细线。代价：两个蓝同屏；主按钮白字 2.78:1 长期挂着。
- **B（推荐）**：**只留一个蓝**，把 `$rk-primary` 本身改成 `#2a72c2`，并在 [`element-override.scss`](../web/src/styles/element-override.scss) 的 `@forward` 里把 `$colors.primary.base` 一起改掉（让 Element Plus 重新生成全部派生色档，而不是事后覆盖）。收益：主按钮白字 4.90:1 达标、全站一个蓝、且 `#409eff` 正是用户列为禁区的「培训机构促销感」里最典型的那个默认蓝。代价：需要回退 `$rk-primary-ink` 的 6 处引用；`$rk-primary` 是对 07 §7.9 的第二处偏离，要按 F11 的先例写进 F14。
  - **改之前必须先量**：Element Plus 的主按钮 hover 底是 `light-3`（主色混 30% 白），白字压在其上对比会掉到 ~2.7:1；深色档 `dark-2` 才够。这件事**当前用 `#409eff` 时本来就更差**，不是 B 引入的回归，但既然要动主色，顺手把 `.el-button--primary` 的 hover/active 底色指向深色档比留在浅色档合理。这两步的对比度数字**还没算**（最后一次计算命令被中止，未执行）。

### （3）两处记录项还没落纸

- `contentStatus: BULK` 有 3 门，而全部 20 门的 `questionCount` 都是 **0**——数据自相矛盾。这是**给用户的数据问题**，不是页面问题，要写进 18 的 §4 或 §6。
- `docs/18` §5 的「未实测」表里，`F1` 的时间格式仍然只有代码级证据（本机无 Redis，能证明它的接口全依赖会话）。Redis 起来后要复跑注册 / 登录 / 登出 / `me` 并按实测改这一行。

---

## 3. 之后才轮到的事

1. 跑 `impeccable-documenter`，产出 `DESIGN.md` 与 `.impeccable/design.json`（本轮**未开始**）。
2. Redis（用户在装）起来后复跑鉴权链路。启动命令已记在 `deploy/.env`：
   `.\redis-server.exe --port 6379 --requirepass 888888 --bind 127.0.0.1`
3. 登录页 / 注册页 / 科目列表页**从未打开看过**——本轮视觉工作全在首页。这三页只有构建 + 类型检查 + 接口连通三件证据。
4. `git commit` **仍未获授权**，本轮没提交任何东西。工作区是 20 个未跟踪文件 / 目录。

---

## 4. 下一轮开工时先做什么

1. **先补 `docs/18` 的 F14 与 §5 更正**——源码里已经有 5 处指向还不存在的条目。
2. 再拿第 2 节（2）的两条路问用户要一个决定，按决定统一主色。
3. 主色定了以后**重跑一遍** `pnpm lint && pnpm type-check && pnpm build` 与 `logs/measure.js` + `logs/capture.js`——色带与书脚的实测值会变，18 里的数字要跟着改。

---

## 5. 本轮新增的可复现脚本（都在 [`logs/`](../logs/)，非产物、可删）

| 脚本 | 做什么 |
| --- | --- |
| [`firstload.js`](../logs/firstload.js) | CDP Network 量**真实首屏传输字节**（不再用构建报告相加）；`TARGET_URL` 指向 `pnpm preview` 的地址 |
| [`measure.js`](../logs/measure.js) | 书脊 / 书名列 / **色带几何与计算色** / 描边 / 抽屉 / 图例的位置与尺寸 |
| [`measure-interaction.js`](../logs/measure-interaction.js) | 点击 → 滚动、开关、键盘走位、Esc；事件必须派发在 `document.activeElement` 上 |
| [`capture.js`](../logs/capture.js) | 三种视口截图 + 每种视口的几何断言 |
| [`probe.js`](../logs/probe.js) | 把检测器的发现归因到具体元素 / 样式表规则 |

两个坑，下一轮别再踩：Vite dev server 只绑 `[::1]`（`F9`）；`impeccable detect` 对 `.vue` / `.css` 返回 `[]` 是**「没扫描」而不是「没问题」**，必须拿渲染后的 URL 作目标。
