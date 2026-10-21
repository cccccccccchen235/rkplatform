# 20 · P2 补测轮记录（Redis 就位后的端到端验证 + `F14` 主色处置 + 度量仪器更正）

> 本文档承接 [18](18-p2-frontend-deviations.md)（P2 出入记录）与 [19](19-p2-frontend-open-items.md)（上一轮留下的待办清单）。
>
> **规则不变**：**旧文档一律不回改**。`18` 是活文档、`19` 明确要求同步它，故这两份**已就地更新**（改了什么、为什么改，见 §1）；`15`~`17` 与 `01`~`14` 一律不动。本文档只记录**本轮新增的事实**。
>
> **编号约定**：本文档不新增 `F` 编号。`F14` 是上一轮已登记的条目（`18` §2 已有行、本轮补完了正文）。
>
> **[19](19-p2-frontend-open-items.md) 不回改**：它是上一轮留下的**输入清单**，保持原样。它的每一项现在落在哪里、做完没做完，见本文档 §1 与 §5 的对照表。

---

## 1. 本轮做了什么

起因是 [19](19-p2-frontend-open-items.md) 列的待办，以及一句环境变化：**本机 Redis 已安装**。做完的事分四块：

| # | 事项 | 落点 |
| :-: | --- | --- |
| 1 | **同步 `docs/18`**：补 `F14` 正文、更正 §5 的检测器取证、更正 `F10` 的度量方式与数字、更新未实测表与自查表 | [18](18-p2-frontend-deviations.md)（就地改） |
| 2 | **`F14` 主色按委托方裁决落地**（方案 B：只留一个蓝） | `web/src/styles/*`、`PortalLayout.vue`、`home.vue`、`subject-list.vue` |
| 3 | **Redis 就位后的端到端补测**：登录/注册/登出、`F1` 时间格式、四个错误码 | `logs/auth-chain.js`（新增）、`logs/probe-register-token.js`（新增） |
| 4 | **度量仪器更正**：`F10` 换测量口径、检测器 dev/prod 差异查清并留永久阳性对照 | `logs/firstload.js`、`logs/probe.js`（修）、`logs/detector-control*.html`（新增） |

### 1.1 `docs/18` 改了哪些地方（逐条）

| 位置 | 改前 | 改后 | 为什么 |
| --- | --- | --- | --- |
| §2 表 | 无 `F14` 行（但有 5 处源码引用指向它） | 补 `F14` 行 | 悬空引用 |
| §3 | 无 `F14` 正文 | 补完七节正文（冲突、两次尝试、死代码发现、悬停分析、落点、代价、复测） | 同上 |
| §5 实跑表 | 3 条 detector `layout-transition` 记为"**误报**" | 改为 **0 条**（并说明 `[]` 的含义与 dev/prod 差异） | 见 §4.2 |
| §5 实跑表 | `F10` 数字 369.6/371.59 KB | **379.85 KB**（换测量方式） | 见 §4.1 |
| §5 实跑表 | 无 Redis / 鉴权回路条目 | 新增 4 条（Redis、`F1` 端到端、全链路、错误码、`F14` 三条） | 本轮补测 |
| §5 未实测表 | 5 项（4 项因无 Redis） | 收窄为 4 项（`2002`、浏览器内 2005 跳转、store 守卫、三页设计观看） | 4 项已补测 |
| §5 检测器取证 | "误报"的四项取证（含"规则 0 条"） | "死规则"的取证 + 仪器 bug 说明 + dev/prod 差异 | 见 §4.2 |
| §6 待裁决 | `F10` 数字 369.6；无 `F14` 回填项；无数据矛盾项 | 更新 `F10`；新增"回填 07 §7.9"与"种子数据自相矛盾"两项 | 本轮发现 |
| §6 前置条件 | "Redis 未安装" | "**本轮已解除**" | 本轮变化 |
| §7 自查 | `F10` 依据是"构建报告相加"；detector 那条是"误报" | 改为 `firstload.js`；改为"死规则"并新增 5 行 | 同 §5 |

---

## 2. Redis 就位后的端到端补测

### 2.1 先修 Redis 自身：口令没生效

第一次跑鉴权链路，**21 项断言全挂**，后端日志里是：

```
io.lettuce.core.RedisCommandExecutionException: ERR Client sent AUTH, but no password is set
```

即客户端发了 `AUTH`，而服务端**没设口令**。这里有一个坑值得记下来：之前用 `redis-cli -a <口令> ping` 得到过 `PONG`，据此以为"口令没问题"——**那个 PONG 是假阳性**：旧版 `redis-cli` 在服务端不需要认证时不会因为多给了一个口令而报错，于是"带口令能连"这件事在"服务端根本没设口令"时同样成立，区分不出来。

**处置**：`redis-cli config set requirepass <口令>` + `config rewrite`（写回服务配置，重启后仍生效）。验证方式改成**不带口令**去连：

```
redis-cli ping        →  NOAUTH Authentication required.   ← 这才说明口令真的生效了
redis-cli -a <口令> ping  →  PONG
```

**给下一个人的判据**：判断"口令是否生效"，看**不带口令的那一次**是否被拒；不要拿"带口令能连"当证据。

> 与 [19](19-p2-frontend-open-items.md) §3 的出入：那里记的启动方式是命令行 `redis-server.exe --port 6379 --requirepass <口令> --bind 127.0.0.1`。本机实际跑的是**Windows 服务**（服务名 `Redis`，配置 `redis.windows-service.conf`），所以口令不是从命令行给的，而是写进服务配置并 `config rewrite` 持久化的。两种方式等效于应用，但**服务方式在重启后仍然生效**，命令行方式不会——所以这里选服务方式。

### 2.2 链路本身：又撞出一个测试端的契约错误

修好 Redis 后，注册能拿到 token 了，但拿这个 token 打 `/users/me` 仍然 `2005`（未登录）。查下来**不是应用的问题，是我的测试脚本发错了**：

- Sa-Token 配置里 `token-name: Authorization`、**`token-prefix: Bearer`**；
- 前端 [request.ts](../../web/src/utils/request.ts) 早就是 `Authorization: Bearer <token>`（注释里写明依据）；
- 我的脚本发的是**裸 token**。

**处置**：脚本改发 `Bearer <token>`。同时补了一个当时缺的断言——原脚本事后只验证"拿到了 token"，**没验证这个 token 能用**；正是这个缺口让上面的错误藏了一轮。现在链路里每一步拿到的 token 都会真的拿去打一次受保护接口。

> 顺带一个**设计事实**（不是缺陷）：注册后立刻查 `/users/me`，`lastLoginAt` 是 `null`。因为 `UserServiceImpl` 只在 `verifyLogin` 里更新 `lastLoginAt`，注册不走登录。已读源码确认（`UserServiceImpl` 第 98 行），故断言按"注册后为 null、登录后被填上"来写。`F1` 的端到端时间格式证据因此挂在**登录**路径上，更自然。

### 2.3 最终结果

`node logs/auth-chain.js`（真 HTTP，非 mock）**全部通过**，覆盖：

| 场景 | 断言要点 |
| --- | --- |
| 匿名访问 `/users/me` | HTTP 401 + `code 2005` + 带回 `traceId` |
| 注册 | `code 0`、拿到 token、`expireAt`/`createdAt` 为 `yyyy-MM-dd HH:mm:ss`、昵称默认取用户名、`role=USER`、`mustChangePassword=false`、**响应里不含任何口令字段** |
| 注册后用该 token 打 `/users/me` | 通过（token 真的可用） |
| 登录 | 拿到 token、`expireAt` 格式化、`lastLoginAt` **已填上且格式化** |
| 登录后用该 token 打 `/users/me` | 通过 |
| 错误码 | `2001` 口令错、`2003` 用户已存在、`2005` 未登录/token 失效、`1001` 字段错误列表 |
| 连续错口令 | 5 次错口令得到 `2001,2001,2001,2001,2004`（第 5 次升级为锁定类错误），**此后正确口令仍为 `2004`** |
| 登出 | `code 0`；旧 token 立刻 `401`+`2005`；重新登录正常 |

**未覆盖**：`2002`（需要"账号被禁用"的数据，本轮没构造）；`2005` 的**浏览器内**拦截器跳转（HTTP 层已验证，浏览器层未跑）。

---

## 3. `F14`：主色处置

完整正文在 [18](18-p2-frontend-deviations.md) §3 的 `F14`。这里只留**要点与结论**，避免两份文档各说一套：

- `#409eff`（07 §7.9 钉死）白底 **2.78:1**、页面底 **2.59:1**，连"图形 ≥3:1"都过不了；它同时正是被列为禁区的"培训机构促销感"里最典型的默认蓝。
- 委托方裁决 **B（只留一个蓝）**：`$rk-primary` 本身改为 **`#2a72c2`**（白底 4.90 / 页面底 4.57）。
- **连带发现**：`element-override.scss` 里那块 `@forward 'element-plus/theme-chalk/…' with ($colors: …)` **是死代码**——按需引入的 `importStyle` 默认 `'css'`，拿到的是预编译 CSS，我们配的 SCSS 变量从未参与编译。证据：改掉 base 后重新构建，产物里仍是 `--el-color-primary:#409eff`。已改为 `:root` 运行期变量覆盖，派生色档按 Element Plus 自己的混合公式**现算**。
- **主按钮悬停**：Element Plus 默认 hover 指向 `light-3`（往浅走），在**两种主色下都不达标**（`#409eff` 上 2.03、`#2a72c2` 上 2.87）。只有 `#2a72c2` 有一个达标档可用：`dark-2` = **6.91:1**。故 hover 指向 `dark-2`；active 本来就是 `dark-2`，于是**按下与悬停同色**（本条目唯一代价）。

**改后复测**（全部实跑）：三件套 exit 0；产物颜色与 `logs/contrast.js` 实算逐位相同；`measure.js` 复量书架几何不变（20 根书脊 `overflowX=0`）；`inspect-pages.js` 打 5 个路由、355 个文字节点 **0 处不达标**、0 横向溢出、0 控制台报错。

---

## 4. 度量仪器的两处更正（本轮最值得记住的一节）

两处"实测"结论其实是**仪器坏了**，不是被测对象的问题。两处都已更正，因为错的仪器会持续产出看起来可信的错数字。

### 4.1 `F10` 的首屏体积：换口径

**旧口径**（构建报告里挑几个 chunk 的 gzip 值相加）有三个问题：

1. **相加的成员是人选的**——换个人会得到另一个数；
2. 07 §7.10 的预算式子里列着 `vendor-katex` / `vendor-markdown`，而它们**当前根本不存在**（katex 与 markdown 尚未被任何页面引入），照那个式子加减等于在和空气对账；
3. 各 chunk 的 gzip 值**不含**响应头，也不是浏览器真正下载的字节。

**新口径**：`node logs/firstload.js` —— 用 CDP 的 `Network` 域读 `/` 上**真实传输的字节**（`encodedDataLength`），冷缓存、只算同源、按资源类型拆开，把**代码体积**与**运行时数据**（`/api/v1/subjects`）分开报。

**结果**：首屏 **379.85 KB**（JS 364.64 + CSS 14.35 + 文档 0.86），预算 350 KB，**超 29.85 KB（8.5%）**。结论不变（仍等页面齐全再决定压不压），但**数字的出处从此是唯一的**。

### 4.2 检测器那条 `layout-transition`：从"误报"更正为"死规则"

**上一版的结论**：这条是**误报**，依据是"遍历已加载样式表的全部规则 → **0 条**"以及"搜 Element Plus 产物 → **0 处**"。

**两个 0 来自两个各自独立的仪器问题**（都在本轮查清）：

**(a) `logs/probe.js` 的遍历有 bug。** 它写成了：

```js
if (r.cssRules) { walk(r.cssRules); continue }   // ← 错
```

Chrome 支持 CSS 嵌套之后，**每一条** `CSSStyleRule` 都带一个 `cssRules`：类型是 `CSSRuleList`、`length` 为 0，而**空对象在 JS 里是 truthy**。于是 `continue` 把每一条规则都跳过，规则自身的声明从来没被检查过——扫描必然返回 0。

修法：`if (r.cssRules && r.cssRules.length) walk(r.cssRules)`，去掉 `continue`，改读 `getPropertyValue('transition')`。

**(b) 旧记录里那次 grep 的上界太短**（`[^;}]{0,80}`）：`padding-bottom` 出现在规则第 ~145 字符处，超出上界，于是"搜 Element Plus 产物"那一行也报了 0。

两个 0 落在**同一张表的不同行**上，所以修好一处不等于修好另一处——本轮把检测器那一节整体重写了。

修好后同一页面实测（生产预览）：扫到 569 条规则，其中含布局属性的 `transition` 规则 **3 条**，原文为

| 选择器 | `transition` 值 |
| --- | --- |
| `.collapse-transition` | `… height …, … padding-top …` |
| `.el-collapse-transition-leave-active, .el-collapse-transition-enter-active` | `… max-height …, … padding …` |
| `.horizontal-collapse-transition` | `… width …, … padding-left …` |

三条全来自 Element Plus 自带的 collapse 过渡，本项目从未使用 collapse 组件；带这些类的元素 **0 个**。

**所以：检测器报得没错，代码也没错——那是"死规则"。** 上一版把它写成"误报"是双重错误：既错在"规则不存在"，也错在把检测器的正确报警说成误报。

### 4.3 顺带查清：检测器在 dev 与 prod 上的答案不同，且差别只来自构建方式

这是上一版误判的**根因**。用两个**内容等价**的阳性对照实测（`logs/detector-control.html` 用内联 `<style>`、`logs/detector-control-linked.html` + `.css` 用外链 `<link>`）：

| 对照 | `low-contrast` | `layout-transition` |
| --- | :-: | :-: |
| 内联 `<style>` 版 | ✓ | ✓ |
| 外链 `<link>` 版 | ✓ | ✗ |

两个检查**取数方式不同**：`low-contrast` 读渲染后元素的**计算样式**（外链 CSS 一样看得见）；`layout-transition` 读页面里的**内联 `<style>` 声明**（**看不见外链 CSS**）。Vite **dev** 把组件 CSS 以**内联 `<style>`** 注入（报得出来），生产构建发出的是 `<link>`（报不出来）。

**给下一个人的判据**：**生产预览上的 `[]` 不等于"规则不存在"**。要判断规则在不在，用 `logs/probe.js` 读 `document.styleSheets`——那才是浏览器眼里的全部规则。这两个对照文件已作为**永久阳性对照**保留在 `logs/`，不要再删。

---

## 5. 本轮仍未做 / 仍未测

| 项 | 状态 | 说明 |
| --- | --- | --- |
| `2002` 错误码 | 未测 | 需"账号被禁用"的数据前置状态 |
| `2005` 的浏览器内跳转 | 未测 | HTTP 层已验证；拦截器跳转需在浏览器里真的让会话过期 |
| `stores/user.ts` 持久化与路由守卫 | 未测 | 同上，需在浏览器里真的登录一次 |
| 登录/注册/科目列表三页的**设计观看** | 未做 | 已打开并程序化审计（0 异常），但本机截图读回是 `[Unsupported Image]`，看不出"好不好看" |
| `impeccable-documenter`（`DESIGN.md` + `.impeccable/design.json`） | **本轮已完成** | 见 §5.1 |
| `comps` 阶段（`F13`） | 保持 open | code-led 构建留下的流程状态，如实披露 |
| `git commit` | **未授权** | 工作区仍全部未跟踪 |

### 5.1 `impeccable-documenter` 的产物与它报出的漂移

已产出两件（均从**已构建的产物**反推，不是照抄 `07 §7.9`）：

- `DESIGN.md`（项目根）——8 节：Overview / Colors / Typography / Layout / Elevation & Depth / Shapes / Components / Do's and Don'ts。北星写作"The Honest Bookshelf（一面诚实的书架）"。
- `.impeccable/design.json`——`schemaVersion: 2`，JSON 校验通过。

它记录的是**实际发货的值**，并把三处对 `07 §7.9` 的偏离标明出处（`F11` / `F12` / `F14`），同时把"`@forward` 是死代码、实际靠 `:root` 运行期覆盖"这一条记进了系统本身。

**它同时报出一处此前没人提过的漂移**（如实登记，**未擅自修**）：

| 漂移 | 内容 |
| --- | --- |
| 方向契约与发货构建不一致 | `.impeccable/surfaces/*.md`（尤其 `web-src-views-portal-home-vue.md`）与 `.impeccable/brief-body-home.md` 里**仍然写着 `#409eff`**，并且写着"不做主题定制"。而发货构建是 `#2a72c2` + 运行期覆盖 Element Plus 变量。 |

`DESIGN.md` 按"记录已发货的现实"处理，**没有**去改那两份契约文件——按 impeccable 的规矩，**漂移只报告、不顺手修**，除非委托方发话。要不要把方向契约回填成 `#2a72c2`（与 07 §7.9 的回填是同一件事的两处落点），请委托方定。

另外两项它**刻意没有**写进系统规则（避免下一个界面盲目继承）：

- `F12` 的"平台无衬线栈充当展示字体"是构建**背负**的一条 craft-floor 让步，不是要继承的系统规则 → 只记了字体栈与级差，没把"不用外链字体"写成规则；
- `F13` 的 `comps` open 是流程状态、不是设计规则 → 未写进 `DESIGN.md`。

**无法从产物确定的**（它如实标为 unresolved，未编造）：移动端处理（按设计就是没有）、后台/考试面板的页宽（220 / 64 / 280px 是已声明但页面未交付的 token）。

---

## 6. 当前可复现脚本清单（`logs/`）

| 脚本 | 用途 | 依赖 |
| --- | --- | --- |
| `auth-chain.js` | 真 HTTP 端到端鉴权回路（注册/登录/登出/错误码/`F1` 时间格式） | 后端 8080 + Redis |
| `contrast.js` | WCAG 对比度实算表（候选色 + Element Plus 派生档） | 无 |
| `firstload.js` | 首屏**真实传输字节**（CDP `Network`） | 生产预览 4173 + CDP 9222 |
| `inspect-pages.js` | 逐路由审计：标题/`h1`/横向溢出/逐节点对比度/控制台报错 | 预览或 dev + CDP |
| `measure.js` / `measure-interaction.js` / `capture.js` | 书架几何、键盘与抽屉交互、响应式不重排 | 预览 + CDP |
| `probe.js` | 把检测器 finding 落到具体元素/具体规则（**已修**） | CDP |
| `detector-control.html` / `detector-control-linked.{html,css}` | 检测器**永久阳性对照** | 放进受测站点可提供的位置 |

---

## 7. 结论

- **上一轮最大的验证缺口已关闭**：登录/注册/登出全链路、`F1` 的时间格式、`2001`/`2003`/`2004`/`2005`/`1001` 全部实跑通过。Redis 是本轮解锁它的唯一前提。
- **`F14` 已按委托方裁决落地**，并连带修掉一处从未生效的主题定制（死代码）。
- **两处度量仪器的错已更正**，其中一处（`F10`）改变了文档里长期沿用的数字口径。
- **仍有两块空白**：浏览器内的会话过期行为（3 项）与三页的设计观看（1 项）。
- **`impeccable-documenter` 已跑完**：`DESIGN.md` 与 `.impeccable/design.json` 产出，并报出一处方向契约与发货构建不一致（契约里仍写着 `#409eff`）——按"漂移只报告"处置，未擅自修。
- `docs/18` 已按 [19](19-p2-frontend-open-items.md) 的要求同步完毕；`15`~`17` 与 `01`~`14` 一律未动。
