---
name: 软考在线学习平台
description: Element Plus 变量体系下的桌面优先软考学习平台：一个蓝、一面书架、零投影。
colors:
  primary: "#2a72c2"
  primary-dark: "#225b9b"
  success: "#67c23a"
  warning: "#e6a23c"
  danger: "#f56c6c"
  info: "#909399"
  text-primary: "#303133"
  text-regular: "#606266"
  text-muted: "#6b6e76"
  border: "#dcdfe6"
  surface-page: "#f5f7fa"
  surface-card: "#ffffff"
typography:
  title:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.6
    letterSpacing: "normal"
  subtitle:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
    fontSize: "16px"
    fontWeight: 600
    lineHeight: 1.6
    letterSpacing: "normal"
  body:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: "normal"
  label:
    fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.6
    letterSpacing: "normal"
rounded:
  s: "2px"
  m: "4px"
  l: "8px"
spacing:
  xs: "4px"
  s: "8px"
  m: "12px"
  l: "16px"
  xl: "24px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.surface-card}"
    rounded: "{rounded.m}"
    padding: "8px 15px"
    height: "32px"
  button-primary-hover:
    backgroundColor: "{colors.primary-dark}"
  button-primary-active:
    backgroundColor: "{colors.primary-dark}"
  button-text:
    backgroundColor: transparent
    textColor: "{colors.text-regular}"
    rounded: "{rounded.m}"
    padding: "8px 15px"
    height: "32px"
  card:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.l}"
    padding: "16px"
  input:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.m}"
  nav-link:
    backgroundColor: transparent
    textColor: "{colors.text-regular}"
    rounded: "0"
    padding: "4px 0"
  nav-link-active:
    textColor: "{colors.primary}"
---

# Design System: 软考在线学习平台

## Overview

**Creative North Star: "The Honest Bookshelf"（一面诚实的书架）**

这是一套没有装饰层的系统。它的世界不是被设计出来的，而是从 Element Plus 的变量体系里长出来的：一个蓝、一套 2/4/8 的圆角、一条 1px 的层板线，剩下的靠版式本身说话。页面不靠渐变、投影或纹样制造"质感"，因为这里要回答的问题只有"这个站有什么、哪门课现在真的能练"——任何为了好看而加的东西都会稀释这个答案。

系统的性格集中在首页：20 个科目被立成 20 根书脊，**等高**立在三条层板上，**20 根里只有 3 根是蓝框**（大纲与题库已完备的那 3 门）。这里**只用一处图形编码**：实线 / 虚线（大纲是否完整）——它回答的是首页唯一要紧的问题"哪门课现在真的能练"。满分与考期不再用图形表达，而是以文字给出：书脚一行是"科目编号 + 卷面满分"，考期在取下来的抽屉里。

> **2026-10-02 移除了两处图形编码**（依据是渲染实测，见 `docs/26`）。脊高原本绑卷面满分，但每层内部高度本已相同（高级 5 本全 225、中级与初级 9 本全 150），脊高实际只重复了"层级"——而层级已由三层书架与层标签说出；全页 20 根里唯一由它产生的独立信息是 IPT（75 分）矮一截，而那本的书名会被压成三列错行。顶端 6px 色带原本用"整条 / 左半 / 右半"三种形状编码考期，但在这个尺寸上读不出来，且与"已印"的蓝框同色同位相撞。**冗余的编码不是信息，是噪音**：同一条事实已由文字说清时，再用一个读不出的图形重复它，只会让页面更难读，并不会更好看。

配色上，系统拒绝了两条看起来更省事的路。第一条是沿用文档钉死的 Element Plus 默认蓝 `#409eff`——它白底只有 2.78:1，连图形线都过不了，且正是被产品明令避开的"培训机构促销感"；第二条是"填充一个蓝、文字另一个蓝"的双蓝方案——它会让同一屏并排出现两个蓝，却没换到任何一处达标。所以只留**一个**蓝 `#2a72c2`。深度上同样克制：全站没有一处 `box-shadow`，实体感只来自层板那条 1px 实线。

**Key Characteristics:**
- 单一主色，无第二强调色；主色承担填充、文字、链接、焦点环与已印书脊的描边。
- 桌面优先：`body` 最小宽度 1280px，低于该宽度保留横向滚动而不重排。
- 平面的、基于线与边框的层次；零投影、零拟物。
- 5 级间距（4/8/12/16/24）、3 级圆角（2/4/8）、单一平台无衬线字体栈。
- 信息编码优先用形状与文字，颜色只作第三重保险。

## Colors

一个蓝加一组中性灰；颜色的职责是把结构讲清楚，不是制造气氛。

### Primary
- **Single Working Blue** (#2a72c2, rgb 42 114 194): 全站唯一的主色。用在主按钮填充、正文链接、顶栏导航当前项（含 2px 下划线）、品牌字标、输入焦点环、已印书脊的描边与书脚小字、`::selection` 底色（`rgba(#2a72c2, .25)`）与输入光标。它是**文字色**也是**图形色**：白底 4.90:1、页面底 `#f5f7fa` 上 4.57:1，两类用途都过线，因此不再需要第二个"文字档蓝"。
- **Pressed/Working Blue Deep** (#225b9b, rgb 34 91 155 = `--el-color-primary-dark-2`): 主按钮的 hover 与 active 底色，白字压上去 6.91:1。**hover 与 active 故意同色**——Element Plus 只导出到 `dark-2`，往下浅的 `light-3`（hover 默认）在两种主色下都只有 2.03 / 2.87:1，不可用。

### Neutral
- **Heading Ink** (#303133): 标题、正文主色、抽屉规格条的数值。
- **Secondary Ink** (#606266): 次级说明、卡片简介、顶栏导航默认态。
- **Muted Ink** (#6b6e76): 占位、弱提示、书脊编号与满分、每层计数、图例说明。**不是**文档里的 `#909399`——那个值在页面底只有 2.87:1，低于文字底线；这一槽位实测一屏 40 处，全是用来讲清结构事实的字。
- **Hairline Border** (#dcdfe6): 卡片、抽屉、层板以下的常规描边、未印书脊的虚线框、输入框描边。
- **Page Surface** (#f5f7fa): 页面底、未印书脊的脊身、滚动条轨道。
- **Card Surface** (#ffffff): 卡片、抽屉、已印书脊、顶栏。

语义色 `success #67c23a` / `warning #e6a23c` / `danger #f56c6c` / `info #909399` 与上表一起导出为 CSS 变量与 Element Plus 变量，但目前只有 `warning` / `error`（`el-alert`）在用；`info` 仅供导出，无文字落点。

### Named Rules
**The One Blue Rule.** 全站只有一个蓝。填充、文字、链接、焦点环都用 `#2a72c2`；不引入第二个强调色，也不把主色拆成"填充档 / 文字档"——那会让同屏出现两个蓝，且并不解决任何达标问题。

**The Contrast Floor Rule.** 每个颜色先量它**实际落点**的对比度再上屏：文字在它自己的底色上 ≥4.5:1，线条与图形 ≥3:1。文档钉死的 `#409eff`（2.78:1）与 `#909399`（2.87:1）因此被替换（记录于 `docs/18` 的 F11 / F14），而不是被沿用。改色时留出余量，不接受"刚好过线"的值。

## Typography

**Display Font:** 平台无衬线栈（`-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif`）
**Body Font:** 同一栈
**Label/Mono Font:** 无独立字体；数字靠 `font-variant-numeric: tabular-nums` 等宽对齐

**Character:** 克制、中性、以中文为先的界面字。字号不进主视觉，性格由结构与字号层级承担：整页只有 24 / 16 / 14 / 12 四个实际用到的字号，字重集中在 400 与 600（少数 700 用于品牌字标）。

### Hierarchy
- **Title** (600, 24px, line-height 1.6): 页面 `<h1>`（`.rk-page-title`）与首页标题，如"软考科目""科目"。
- **Subtitle** (600, 16px, line-height 1.6): 卡片科目名、抽屉科目名、分组标题、品牌字标；`el-card` 内部沿用。
- **Body** (400, 14px, line-height 1.6): 全站默认字号（也是 `--el-font-size-base` 与 `el-button` 字号）；正文、导航、表单标签。
- **Label** (400, 12px, line-height 1.6): 元数据小字、书脊编号与满分、每层计数、图例、抽屉 `dt`、时间与数字。数字一律 `tabular-nums`，基线对齐。

`$rk-font-l`（20px）与 `$rk-font-xxl`（30px）虽在变量表里存在，但当前所有已交付页面都未使用。行高固定 `1.6`——是题干长文本可读性的下限，不得为"紧凑"压到 1.4 以下。

### Named Rules
**The Tabular Rule.** 所有计数、满分与编号都必须 `font-variant-numeric: tabular-nums` 并共用一条基线；数字要能在不同书脊、不同行之间直接被比较。**不要**为长名字单独缩字号——那会让同一层书架上的字号参差不齐，长名字改由竖排自动折行。

## Layout

桌面优先的固定栅格。`body`（及 `html/body/#app`）最小宽度 1280px，`<meta name="viewport" content="width=1280">`；窗口窄于 1280px 时出现的是横向滚动，不做断点重排。移动端只保证不遮挡、可滚动、可点。

内容容器 `.rk-container` 宽 100%、最大宽 1200px、水平居中、左右内边距 24px；页面不各自写 `max-width`。顶栏 `.portal-header` 高 60px、吸顶（`position: sticky`），内部就是这条容器。

首页书架是纵向三段（高级 / 中级 / 初级，按 `SUBJECT_LEVEL_ORDER` 反序），每段是"一排书脊 + 一条层板"。书脊定宽 88px、间距 8px、**等高 160px**，靠底对齐（`align-items: flex-end`），层板是段底 1px 实线 + 4px 下内边距。**160px 是按"三层书脊在参考视口 1280×800 内同时可见"定的**（实测 200px 会把初级层挤到折线以下），且总高与改动前的 200+133+133 持平。卡片页（科目列表）用三列网格 `repeat(3, minmax(0, 1fr))`、间距 16px。

间距刻度 `4 / 8 / 12 / 16 / 24`（xs / s / m / l / xl）就是全站节奏；没有第四级以外的自由值。答题页右栏（280px）与管理端侧栏（220px / 折叠 64px）在变量表里已定，但对应页面尚未交付。

### Named Rules
**The Equal-Height Rule.** 同一面书架上的书脊**等高**。高度不编码任何数据——它一旦编码数值，就会与"层级"这条已经存在的轴重复，而重复的编码只会让架子看起来参差。要表达量上的差别，用文字（书脚的满分、抽屉里的数字），不用高度。

## Elevation & Depth

**这个系统不用阴影。** 全站没有一处 `box-shadow`、`text-shadow` 或 `backdrop-filter`。层次由线与边框表达：1px 实线、1px 虚线、以及背景色的两档（页面底 `#f5f7fa` 与卡片白 `#ffffff`）。首页的"实体感"只来自一条东西——层板线（`border-bottom: 1px solid #6b6e76`）；用比常规描边更深的 `text-muted` 而不是 `border`，是为了让"书站在什么上面"一眼可辨。书脊被"取下来"时不投下阴影，而是整根上移 14px、离开层板线本身，并配 `z-index: 1`。

### Named Rules
**The Board-Not-Shadow Rule.** 深度只来自一条 1px 实线。需要表达"抬起来/分层/选中"时，用离开基线、位移或边框变化，不用投影；任何新增的表面都默认平贴在页面底上。

## Shapes

圆角是三档的：**2px**（书脊顶部两角、图例色块的顶部两角）、**4px**（Element Plus 基础圆角 `--el-border-radius-base`，主按钮、输入框）、**8px**（卡片、抽屉、`el-card`）。书脊底部两角是直角（`2px 2px 0 0`），因为它立在层板上。

边框是主要的形式语言，且**线的形态承担信息**：`1px solid` = 常规 / 已印（大纲完整），`1px dashed` = 未印 / 骨架态（`el-empty`、未配置卷面的分隔线同理）。焦点用 `outline: 2px solid #2a72c2; outline-offset: 2px`，不改圆角。滚动条宽 12px、滑块 6px 圆角、3px 底色分隔。

线的形态是这一页唯一的类别编码：`1px solid`（已印）与 `1px dashed`（未印）。它成立的前提是**只此一处**——一旦在同一根书脊上再叠加第二个形状（比如曾用来编码考期的顶端 6px 色带），两个编码就会抢同一个位置、同一种颜色，谁都读不清。

### Named Rules
**The One-Encoding-Per-Axis Rule.** 一根书脊上只放一处图形编码。形状编码本身没错，错的是**两个编码叠在同一个位置、共用一种颜色**——顶端那条色带与"已印"的蓝框正是如此，两者都失效。小尺寸的类别仍应优先用形状而非色相深浅，但必须先确认这个通道没被别的编码占用。

**The No-Dimming Rule.** 不用降低透明度表达"未印 / 次要 / 禁用"。`opacity` 会把一个维度的信息压到另一个通道上——曾用 `opacity: .45` 表达未印，结果那 17 本连考期都读不出来（1.20~1.52:1），已删除。次要信息用达标的灰 `#6b6e76` 与虚线框表达，保住对比度。

## Components

### Buttons
- **Shape:** 圆角 4px（`--el-border-radius-base`），高 32px，内边距 `8px 15px`，字号 14px，`inline-flex` 居中。
- **Primary:** 填充 `#2a72c2`、白字、无独立边框（`el-button--primary`）。用于"注册""登录""在科目页查看"等主操作。
- **Hover / Focus:** hover 与 active 都指向 `dark-2` `#225b9b`（hbg / hborder 被显式覆盖为 `--el-color-primary-dark-2`）；Element Plus 默认的 `all .1s` 过渡保留。**两个状态同色**是已知代价，换来的是不自造 Element Plus 未导出的色档。
- **Text / Ghost:** `el-button text`（无填充、`text-regular` 文字）用于"登录""退出""重试"等次级或破坏性弱的动作。顶栏不用按钮占位放"做什么"，只放"去哪儿"。

### Cards / Containers
- **Corner Style:** 8px。
- **Background:** `#ffffff`，静置于页面底 `#f5f7fa` 之上。
- **Shadow Strategy:** 无（见 Elevation & Depth）。
- **Border:** `1px solid #dcdfe6`；卡片内部分隔（如"卷面"行）用 `1px dashed`。
- **Internal Padding:** 16px（`$rk-space-l`；`el-card` 的 `--el-card-padding` 已从默认 20px 收到 16px）。科目卡为纵向 flex、间距 12px，底部动作行用 `margin-top: auto` 贴底。

### Inputs / Fields
- **Style:** Element Plus 默认输入框，圆角 4px，白底，`1px` 描边 `#dcdfe6`；字号 14px。
- **Focus:** 由 Element Plus 主色变量驱动——描边与焦点环用 `#2a72c2`。光标（`caret-color`）同为主色。
- **Placeholder:** `#6b6e76`（`--el-text-color-placeholder` 与 `--el-text-color-secondary` 都映射到此，避免同屏出现两种弱提示灰）。
- **Error / Disabled:** 表单校验错误由 `el-form-item` 就地标注；`1001` 的字段级错误定位到具体字段。错误色 `#f56c6c`。

### Alerts
- **Style:** `el-alert`，用于登录/注册的失败与锁定（`type: error` / `warning`，`show-icon`，`closable: false`）以及科目加载失败（带内嵌"重试"文字按钮）。登录/注册**不用 toast**：失败信息要能一直看着，直到用户据此决定下一步。

### Navigation
- **Style:** 顶栏 60px 白底，底边 `1px solid #dcdfe6`，吸顶。左侧品牌（字标 `软考` 用主色 700，副题 `在线学习平台` 用 `text-1` 600），中间导航，右侧科目下拉 + 登录/注册或用户/退出。
- **Default / Hover / Active:** 导航项默认 `text-regular`，hover 转主色且不出现下划线；当前项为 `color: #2a72c2` + `border-bottom: 2px solid #2a72c2`（用重色与 2px 细线同时表达，不单靠颜色）。
- **Mobile Treatment:** 无。桌面优先，窄于 1280px 只出现横向滚动。

### Signature Component — 书架书脊与抽屉
首页的核心自定义组件。每根书脊是一个可聚焦的 `<button>`（不是 div）：竖排科目名（`writing-mode: vertical-rl`，长名自动折列，160px 下只有 11 字名折两列）、书脚一行"编号 + 满分"。状态：hover 上移 4px（仅"手指搭上去"，不改信息）；`--out` 上移 14px 表示被取下；`--printed`（大纲完整）白底 + `1px solid #2a72c2` 实线，未印为页面底 + `1px dashed #dcdfe6`。键盘 `←/→` 同层移动、`↑/↓` 换层、`Home/End` 到首尾、`Esc` 插回；每根脊本身可 Tab。点击后书架下方长出抽屉（科目名、编号、考期/大纲/题库/卷面四项描述列表 + 主按钮），必要时滚入视口。

**Motion（唯一的一处动效）：** `transform 220ms cubic-bezier(0.16, 1, 0.3, 1)`（指数缓出）、`border-color 160ms ease-out`；`prefers-reduced-motion: reduce` 时过渡置 `none`、滚动改 `auto`——减少的是动态，不是功能。骨架屏刻意不做闪烁动画。

## Do's and Don'ts

### Do:
- **Do** 把每一个可见数值先对它实际落点量对比度：文字 ≥4.5:1、线/图形 ≥3:1（The Contrast Floor Rule）。
- **Do** 只用 `#2a72c2` 一个蓝；hover / active 用 `#225b9b`（The One Blue Rule）。
- **Do** 让同一面书架上的书脊等高；数值差别用文字表达（The Equal-Height Rule）。
- **Do** 确认一个位置只有一个图形编码（The One-Encoding-Per-Axis Rule）。
- **Do** 用 4/8/12/16/24 的间距刻度与 2/4/8 的圆角刻度，不新增自由值。
- **Do** 保持 `body` 最小宽度 1280px，<meta viewport width=1280>；窄屏出横向滚动，不重排。
- **Do** 计数、满分、编号加 `tabular-nums` 并基线对齐（The Tabular Rule）。
- **Do** 用 `el-button` / `el-card` 暴露的变量与 class / slot 定制外观。

### Don't:
- **Don't** 引入第二个强调色，或把主色拆成"填充档 / 文字档"（One Blue Rule）。
- **Don't** 用 `opacity` 或降对比表达"未印 / 次要 / 禁用"（The No-Dimming Rule）；用达标灰 + 虚线框。
- **Don't** 加 `box-shadow`、投影或 `backdrop-filter` 造层次；深度来自 1px 实线（The Board-Not-Shadow Rule）。
- **Don't** 在同一根书脊上叠加第二个图形编码（顶端色带与蓝框相撞就是这样双双失效的）。
- **Don't** 写 `.el-xxx__body` 这类内部结构选择器——升级时会以"样式莫名失效"断裂（Theming 走运行期 `:root` 变量 + `el-button--primary` / `.el-card` 等公开 class）。
- **Don't** 把主按钮的 hover 留给 Element Plus 默认的 `light-3`（白字 2.87:1）；它指向 `dark-2`。
- **Don't** 把行高压到 1.4 以下，或为长名字单独缩字号。
- **Don't** 把柔和的过渡渐变当装饰。渐变在这个系统里没有落点：需要硬边的形状用边框与色块画，不靠渐变。
