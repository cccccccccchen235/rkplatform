# 18 · P2 阶段的实现出入记录（前端首建 + 认证切片）

> 本文档承接文档链的同一体裁：[15](15-implementation-deviations.md) / [16](16-p1-code-audit.md) / [17](17-changes-p1-audit.md) 是 P1（后端骨架）的出入、审查与处置记录；**本文档是 P2 阶段**——`web/` 首次落地、后端补齐 05 §5.3 第 1~4 条认证接口、以及为了让前者能跑而必须解决的两处契约冲突。
>
> **编号约定**：15 用 `D`、16 用 `E`、17 用 `N`（执行期新发现）。本文档用 **`F`** 系列，不覆盖任何既有编号。
>
> **规则不变**：**旧文档一律不回改**。出入写在这里，需要回填的地方只标注"待回填 `<文档> §<节>`"，实际回填由你决定时机。
>
> **本阶段的取值范围**：`web/**`（新建）、`server/rk-domain|rk-repository|rk-service|rk-api|rk-boot` 的认证相关新增文件与 `paperTotalScore` 一处改动、`deploy/.env`（本地、未提交）、`server/rk-boot/…/application.yml`（一处注释）、`logs/*.js`（本轮新增的取证脚本）、`.impeccable/**`（方向契约与截图，非交付物）、`DESIGN.md` + `.impeccable/design.json`（本轮由 `impeccable-documenter` 从**已构建产物**反推生成，见 [20](20-p2-verification-round.md) §5.1）。

---

## 1. 范围与结论

| 项 | 内容 |
| --- | --- |
| 基准 | [05](05-api.md)（接口契约唯一来源）、[07](07-frontend.md)（前端唯一规格）、[06](06-backend.md)（分层与安全）、[09](09-deployment.md)（环境变量）、[15](15-implementation-deviations.md)~[17](17-changes-p1-audit.md)（前序出入与裁决） |
| 新增 | `web/` 全部；后端 `SysUser`/`UserVO`/`AuthResultVO`/`RegisterRequest`/`LoginRequest`/`SysUserMapper`(+XML)/`LoginAttemptDao`/`UserService`/`UserConverter`/`UserServiceImpl`/`AuthController`/`UserController`/`JacksonConfig` |
| 取证方式 | **全部为实跑**：`mvn test`（17 项全绿）、`mvn package`、起后端打真接口、`pnpm build` / `type-check` / `lint`、经 vite 代理复打接口、**CDP 在真实浏览器里量几何与交互**（`logs/measure.js`、`logs/measure-interaction.js`、`logs/capture.js`、`logs/probe.js`、`logs/firstload.js`、`logs/inspect-pages.js`）、**真 HTTP 的端到端鉴权回路**（`logs/auth-chain.js`）、**WCAG 对比度实算**（`logs/contrast.js`）、**`impeccable detect` 打渲染后的 URL**。凡未实跑的一律在 §5 标"未实测" |
| 未覆盖 | `2002` 一个错误码（需构造"账号被禁用"数据）、浏览器内的会话过期跳转（`2005` 拦截器）、登录/注册/科目列表三页的**设计观看**（已打开并程序化审计，非视觉核对）、`comps` 阶段（code-led，见 `F13`）。详见 §5"未实测" |

**结论**：`F1`~`F3` 是**契约级**冲突（文档与文档、文档与实现之间），其中 `F1` 已修、`F2` 按"05 优先"处置、**`F3` 已按 c 落地（字段取整数 `paperTotalScore`）**；`F4`~`F6` 是文档指定的做法在本轮工具链下**做不到或不该照做**；`F7`~`F10` 是环境与度量事实；`F11` 与 `F14` 是**文档钉死的取值过不了本项目自己的质量底线**（均已修，`F14` 连带查出 Element Plus 主题定制整块是死代码）；`F12` 是一条取向在本项目前提下按文档让步；`F13` 是 code-led 构建留下的流程状态，如实披露。

> **本轮新增一条贯穿性的教训**（`F10` 与 §5 的检测器取证各撞出一次）：**上一版有两处"实测"结论其实是仪器坏了**——`F10` 的"若干 chunk 相加"是人为选定的口径，§5 的"样式表规则 0 条"是 `probe.js` 的 truthy-空对象 bug。两处都已更正。凡把数字写进文档的，先确认那个数字是**怎么来的**。

**两条值得单独记住的方法论事实**（都是本轮实测撞出来的，不是推断）：

1. `impeccable detect` 对 `.vue` / `.css` 返回 `[]` 是"**没扫描**"而不是"没问题"——同一内容写成 `.html` 立刻报 2 条。`.vue` 项目必须拿**渲染后的 URL** 作目标。
2. **"顺手写对"和"实测过"的差别在这一轮是具体的**：书脊的固定开销、抽屉落在折线以下、`$rk-text-3` 的对比度——三件都不是读代码能看出来的，全是量出来的。

---

## 2. 出入清单

| # | 级别 | 类型 | 文档位置 | 实现位置 | 状态 |
| :-: | :-: | --- | --- | --- | --- |
| **F1** | 高 | 契约冲突：文档要求的时间格式 ≠ Jackson 默认行为 | [05](05-api.md) §5.1 | `rk-boot/…/config/JacksonConfig.java` | **本轮已修** |
| **F2** | 高 | 文档缺字段：07 的 `UserVO` 少一个 05 有的字段 | [07](07-frontend.md) §7.11 ↔ [05](05-api.md) §5.3 | `web/src/types/api.d.ts` | **本轮按 05 处置** |
| **F3** | 中 | 契约冲突：07 的 getter 在 05 的接口设计下**恒为空** | [07](07-frontend.md) §7.4.2 / §7.5.1 ↔ [05](05-api.md) §5.4 | `web/src/stores/subject.ts`、`web/src/views/portal/subject-list.vue`、`rk-domain/…/vo/SubjectVO.java` | **本轮按 c 处置**（见下） |
| **F4** | 中 | 文档指定的插件不支持本轮的工具链 | [07](07-frontend.md) §7.2.1 | `web/vite.config.ts`、`web/eslint.config.js` | **本轮已替换** |
| **F5** | 中 | 文档指定的文件类型放不下它自己要求的内容 | [07](07-frontend.md) §7.11 | `web/src/types/labels.ts` | **本轮已拆分** |
| **F6** | 低 | 文档提到的 UI 元素没有对应的技术栈条目 | [07](07-frontend.md) §7.3.3 ↔ §7.1/§7.2 | `web/src/router/index.ts` | **本轮未实现** |
| **F7** | 中 | 环境变量模板照抄会失败 | [09](09-deployment.md) §9.4 / `deploy/.env.example` | `deploy/.env`（本地） | **本轮已绕开** |
| **F8** | 中 | 无 Redis 时注册"失败"但账号已建 | [06](06-backend.md) §6.5 | `rk-api/…/AuthController.java` | **记录** |
| **F9** | 低 | 快速开始里的地址在本机连不上 | [07](07-frontend.md) §7.2.1 | `web/vite.config.ts` | **记录** |
| **F10** | 低 | 首屏体积预算在当前实现下已超 | [07](07-frontend.md) §7.10 | `web/dist`（实测） | **待裁决** |
| **F11** | 高 | 文档钉死的弱提示色**过不了对比度底线**（实测 2.87:1，要求 4.5:1） | [07](07-frontend.md) §7.9 | `web/src/styles/variables.scss`、`web/src/styles/element-override.scss` | **本轮已修** |
| **F12** | 中 | 展示字体无外部来源，与 craft floor"平台无衬线不作展示字体"的取向冲突 | [07](07-frontend.md) §7.9 / §7.10 ↔ `reset.scss` 的假设 A2 | `web/src/styles/reset.scss`、`home.vue` | **按文档处置并记录** |
| **F13** | 中 | code-led 构建下 `.impeccable/build/state.json` 的 `comps` 阶段保持 open | 流程（impeccable 参考文档） | `.impeccable/build/state.json` | **记录**（见 §4） |
| **F14** | 高 | 文档钉死的主色 `#409eff` **过不了对比度底线**；连带查出 Element Plus 主题定制整块是**死代码** | [07](07-frontend.md) §7.9 | `web/src/styles/variables.scss`、`element-override.scss`、`index.scss`、`reset.scss`、`PortalLayout.vue`、`home.vue`、`subject-list.vue` | **本轮已修** |

---

## 3. 逐条说明

### F1 · 05 §5.1 的时间格式与 Jackson 默认行为冲突（高 · 已修）

**冲突内容**

[05](05-api.md) §5.1 规定时间下发为 `yyyy-MM-dd HH:mm:ss`（形如 `2026-09-27 14:05:03`）。而 `application.yml` 里写的两行是：

```yaml
spring.jackson.serialization.write-dates-as-timestamps: false   # 字符串 vs 时间戳
spring.jackson.time-zone: Asia/Shanghai                          # 时区
```

这两行**只决定"字符串还是数字"与"用哪个时区"**，不决定字符串的格式。Jackson 对 `LocalDateTime` 的默认格式是 ISO-8601，即 `2026-09-27T14:05:03`——**与契约只差一个 `T`**。

**为什么它此前一直没暴露**：在本次改动前，全项目没有任何"会带时间字段的 VO 上线过"。`GET /api/v1/subjects` 的 `SubjectVO` 全是字符串与数字。所以这个差异在代码、文档、测试三处都看不出来，只有第一次有人真的解析那个字段时才会以"日期显示成 `2026-09-27T14:05:03`"的形式出现——而那时的第一反应会是去查前端格式化函数。

**处置**：新增 `rk-boot/…/config/JacksonConfig.java`，用 `Jackson2ObjectMapperBuilderCustomizer` 为 `LocalDateTime` 显式注册 `DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")`；同时把 `application.yml` 里那句 "时间下发 ISO-8601 字符串" 的注释改成如实描述（**这是本轮唯一改动的既有文件**，只改注释、不改配置值）。

**刻意只注册 `LocalDateTime`**：契约目前只定义了"日期时间"一种格式。给 `LocalDate`/`LocalTime` 也各编一个格式，是在发明契约没有的东西。

> ⚠️ **本条未端到端实测**：能证明它的接口（`/users/me`、注册、登录）全部依赖 Redis，本机没有（§5）。目前只有**代码级**证据。等 Redis 就位后必须复测。

---

### F2 · 07 §7.11 的 `UserVO` 少了 `mustChangePassword`（高 · 按 05 处置）

[05](05-api.md) §5.3 第 4 条的 `UserVO` 字段表里有 `mustChangePassword`（`boolean`，**必填**），并说明它的用途：管理员首次登录时为 `true`，前端凭它强制跳转改密页（对应评审 C2）。

[07](07-frontend.md) §7.11 的 `UserVO` 片段没有这个字段：

```ts
export interface UserVO { id: string; username: string; nickname: string; role: 'USER' | 'ADMIN'; status: string
  currentSubjectId?: string; lastLoginAt: string; createdAt: string }
```

**处置依据**：[07](07-frontend.md) 自己的开头就写了"接口路径、字段名、枚举值唯一来源为 05"。所以 `web/src/types/api.d.ts` 按 **05** 写，并把 `mustChangePassword` 标为必填（不是可选）。该文件里留了注释指明这处出入。

> 回填建议：[07](07-frontend.md) §7.11 的片段补上 `mustChangePassword: boolean`。**本文档不回改 07**。

---

### F3 · 07 §7.4.2 的 `configuredSubjects` 在 05 §5.4 下恒为空（中 · 已按 c 落地）

**冲突内容**

[07](07-frontend.md) §7.4.2 要求 subject store 有这样一个 getter：

```ts
configuredSubjects = flatList.filter(s => s.paperSummary != null)   // 可发起模拟考的科目
```

而**实现侧**（`SubjectVO` 的 javadoc，其依据是 05 §5.4 把"含 `paperSummary`"列为详情接口 `GET /subjects/{id}` 的特性）：

> `paperSummary` 卷面结构摘要。**列表接口返回 null**，仅 `GET /subjects/{id}` 填充……列表页只需要科目标题，没必要为 20 个科目各带一棵场次树。

**这两条放在一起，后果是**：`GET /subjects` 返回的每一条 `paperSummary` 都是 `null`，所以那个 getter **恒等于空数组**。而空数组在模拟考页会被渲染成"没有任何科目可以考"——一个听起来像事实、实际是错的结论。

**实测证据**（本轮起后端实打）：

```
JUNIOR 4 / MIDDLE 11 / SENIOR 5   共 20 个科目，paperSummary 全部为 null
```

同时 `deploy/sql/init-data.sql` 里 20 个科目的卷面模板是齐的（此前已核验：20 模板 / 25 场次 / 45 分节）。**所以"null"的含义是"列表接口没带"，不是"没配置"。**

**本轮已做的处置（保守侧）**：

1. `web/src/stores/subject.ts` 先按 `paperTotalScore != null` 判断（见下），原地留注释说明原先那个写法为什么恒空、正确答案在 `GET /subjects?onlyConfigured=true`（[07](07-frontend.md) §7.5.1 的模拟考中心正是这么调的）。
2. `web/src/views/portal/subject-list.vue` 的卷面行改为**只在接口真的给了卷面信息时才渲染**。原先写的是"尚未配置卷面模板，暂不能发起模拟考"——那是页面在宣示一件它无从知道的事，而且恰好是假的。
3. 页面导语同步删掉"卷面是否已配置"这一项。

**最终处置（本轮按 c 落地，已获批准）**：候选的三种处置是——

| 方案 | 做法 | 代价 |
| --- | --- | --- |
| a | 列表页不展示卷面状态；模拟考中心用 `?onlyConfigured=true` 单独问 | 科目列表页少一列信息，用户在这里看不出哪些科目能考 |
| b | 后端 `GET /subjects` 也填充 `paperSummary` | 20 个科目各带一棵场次树；写不好就是 N+1（SubjectVO 的 javadoc 正是为了避开这个） |
| **c** | 列表接口的 `SubjectVO` 增加一个轻量字段 | 要改 05 §5.2 的字段表 + VO + Mapper 查询；但这是"列表页需要知道的那一个 bit"的最省表达 |

**采用的不是原提案里的布尔 `paperConfigured`，而是一个整数 `paperTotalScore`**。原因来自首页书架的视觉契约：那一版把**脊高直接绑到卷面满分**（"书脊的高度是它的卷面满分"），布尔表达不了 225 / 150 / 75 三档。既然要动一次契约，就一次给出能同时满足"能不能考"和"卷面多大"的那一个字段——布尔只是整数的一个投影，加两个字段反而要维护两条同步路径。

落地内容：

| 层 | 改动 |
| --- | --- |
| `rk-domain` | `SubjectVO` 增 `Integer paperTotalScore`；`Subject` 实体增同名只读投影字段 |
| `rk-repository` | `SubjectMapper.xml` 增 `<sql id="paperTotalScoreColumn">`：对 `exam_paper_template` 的相关子查询（`status='ENABLED'`，`LIMIT 1`），并用 `uk_subject` 唯一键保证不重行 |
| `rk-service` | `SubjectConverter` 透传该字段 |
| `web` | `types/api.d.ts` 的 `SubjectVO` 增 `paperTotalScore?: number \| null`；store 的 `configuredSubjects` 改按它判断 |

**为什么挂在 `Subject` 实体上而不是另建投影类型**：MyBatis 的自动映射走 setter，本项目没有 `@TableName`/`BaseMapper`、SQL 全部手写，多一个字段不会被任何自动 SQL 生成器看到；而 `argNameBasedConstructorAutoMapping` 未开启，record 投影的映射不可靠。该字段在实体上标明是"关联出来的只读投影，插入/更新永远不写它"。

**实测结果**（`GET /api/v1/subjects`）：20 个科目**全部非 null**（225 五个 / 150 十四个 / 75 一个，即 IPT）。这个"零 null"本身就是结论：**"是否能考"在当前数据下没有区分度**，所以书架上的"已印 / 待印"改用大纲状态（见 §4），而不是拿卷面来说事。

> 由此 [05](05-api.md) §5.2 的 `SubjectVO` 字段表**少一个字段**；本轮以 `SubjectVO.java` 与本文档为准，回填建议见 §6。

---

### F4 · `vite-plugin-eslint` 不支持 ESLint 9 的 flat config（中 · 已替换）

[07](07-frontend.md) §7.2.1 的插件表写着 `vite-plugin-eslint`。该插件停在 ESLint 8 的配置格式上，读不了本项目的 `eslint.config.js`（ESLint 9 flat config）。本轮改用 **`vite-plugin-eslint2`**——同一作者线的维护中分支，行为一致（dev server 里就地报错、`lintOnStart` 可关）。

**这不影响 §7.2.1 想达到的目的**（"lint 错误在 dev 阶段可见"），只换了实现。走 `pnpm lint` 的那条路径（§7.11 的提交前检查项）不受影响，本来就是独立命令。

> 回填建议：[07](07-frontend.md) §7.2.1 插件表把 `vite-plugin-eslint` 改为 `vite-plugin-eslint2`，并注明原因是 ESLint 9 的 flat config。

---

### F5 · `types/domain.d.ts` 放不下它要放的内容（中 · 已拆分）

[07](07-frontend.md) §7.11 规定 `types/domain.d.ts` 存放"枚举联合类型"。但在实现时，与枚举配套的**中文展示映射**（`Record<SubjectLevel, string>` 这类）是**运行时常量**，而 `.d.ts` 文件按定义不能包含实现。硬塞进去会直接编译失败。

**处置**：拆成两个文件——

| 文件 | 内容 | 依据 |
| --- | --- | --- |
| `web/src/types/domain.d.ts` | 只有类型（`PracticeMode`、`SubjectLevelOrder`） | 07 §7.11 原意 |
| `web/src/types/labels.ts` | 枚举 → 中文文案的常量映射 | 07 未规定落点，本轮新增 |

映射表统一写成 `Record<枚举, string>` 而不是 `Record<string, string>`：后端新增枚举值时前者会**编译报错**，后者只会让页面显示一片空白。

> 回填建议：[07](07-frontend.md) §7.2 的目录树与 §7.11 各补一行 `labels.ts`。

---

### F6 · §7.3.3 的「结束顶部进度条」没有对应的依赖（低 · 未实现）

[07](07-frontend.md) §7.3.3 的守卫说明写着 "`afterEach`：结束顶部进度条"。但 §7.1 的技术选型与 §7.2 的 `package.json` 脚本里都没有进度条库（`nprogress` 之类）。

**处置**：本轮**不引入**。理由：为一个纯装饰元素增加一个未在文档中登记的运行期依赖，与 §7.1 "不引入" 那一段的精神相反。`afterEach` 保留，职责改为 `cancelAll()`（§7.7 要求的取消在飞请求）。

> 回填建议：若确实要进度条，先在 §7.1 登记依赖再实现；否则把 §7.3.3 那句话删掉。

---

### F7 · `.env.example` 的 `JAVA_OPTS` 在 bash 下会执行失败（中 · 已绕开）

按 [09](09-deployment.md) §9.4 的用法 `set -a; . deploy/.env; set +a` 加载环境变量时，`deploy/.env.example` 这一行会报错：

```
JAVA_OPTS=-Xmx1024m -Xms512m -XX:+UseG1GC -XX:MaxMetaspaceSize=256m
→ deploy/.env: line 38: -Xms512m: command not found
```

原因是未加引号的值里有空格，被 shell 拆成了命令。用 docker compose 读同一个文件不会出问题（compose 自己的解析器不执行 shell），所以这是**示例文件里"两种读法只有一种能用"**的坑。本轮本地 `deploy/.env` 里给该值加了引号并注明原因（compose 会剥掉外层引号，两边都能用）。

**同时暴露的另一件事**：`REDIS_PASSWORD` 被 `RequiredEnvValidator` 列为**必填**，而本机惯例是装一个**无口令**的 Redis。这两者不能同时成立——无口令 Redis 配上非空 `REDIS_PASSWORD` 会让 Lettuce 发 `AUTH` 并收到 `ERR Client sent AUTH, but no password is set`，连接照样失败。也就是说本机 Redis **必须带口令启动**（这本身是对的方向：Redis 里放的是会话）。本文档在 `deploy/.env` 里把这个要求写成了可直接复制的启动命令。

> 回填建议：[09](09-deployment.md) §9.4 给 `JAVA_OPTS` 加引号，并在 `REDIS_PASSWORD` 一行补一句"留空会拒绝启动，故本地 Redis 也须 `--requirepass`"。

---

### F8 · 无 Redis 时注册返回失败，但账号已经建好了（中 · 记录）

实跑结果（本机无 Redis）：

| 请求 | 响应 |
| --- | --- |
| `POST /api/v1/auth/register`（合法参数） | `{"code":1000,"message":"系统内部错误"}` |
| `POST /api/v1/auth/login` | `{"code":1000,"message":"系统内部错误"}` |

登录回 `1000` 与 [06](06-backend.md) §6.5 的约定**一致**（会话类数据不可降级，Redis 不可用即失败）——这不是缺陷。

但**注册**这一条要单独说：`UserServiceImpl.register` 是 `@Transactional` 的，它提交后 `AuthController` 才调 `StpUtil.login()` 签发令牌。两者不在同一个事务里，所以 Redis 挂掉时会发生：

1. 用户行已提交到 `sys_user`；
2. `StpUtil.login()` 抛异常 → 全局异常处理回 `1000`；
3. 用户以为没注册成功，再点一次 → 唯一键命中 → **`2003` 用户名已存在**。

**这是可恢复的**（前端在 `2003` 时有"去登录"的引导），也确实只在 Redis 不可用时出现，但"第一次告诉你失败、第二次告诉你已存在"对用户是自相矛盾的。三种可能处置：把令牌签发移进事务（仍非原子，只是窗口更小）／注册前先探一次 Redis 可用性／接受现状并在前端把 `2003` 的引导做实。**本轮不动**，记在这里等下批一起定。

---

### F9 · Vite dev server 默认只绑 `::1`（低 · 记录）

`pnpm dev` 起来后，`http://127.0.0.1:5173/` **拒绝连接**，`http://localhost:5173/` 正常：

```
TCP    [::1]:5173    [::]:0    LISTENING
```

Vite 5 的 `server.host` 默认是 `localhost`，在本机解析到 IPv6 回环。这是 Vite 的默认行为而非本项目配置错误，但"照 README 敲 127.0.0.1 却连不上"是常见困惑点。

> 若要让两种写法都可用，在 `vite.config.ts` 的 `server` 里加 `host: '127.0.0.1'`——**本轮不加**：那会把 dev server 从"仅本机"改成绑定具体 IPv4，而我们并不需要局域网访问，且 §7.2.1 没有规定 `host`。

---

### F10 · 首屏体积实测 379.85 KB，超 §7.10 的 350 KB 预算（低 · 待裁决）

[07](07-frontend.md) §7.10 的预算：考生端首屏（`vendor` + `vendor-element` + `vendor-katex` + `vendor-markdown` + `home`）**gzip 后 JS ≤ 350KB**。

**测量方式已换**（这是本条最重要的更正）：上一版的数字是拿构建报告里"某几个 chunk 的 gzip 值相加"得来的——相加规则是人为选的，换个人会得到另一个数，而 §7.10 的预算式子里还列着 `vendor-katex` / `vendor-markdown` 两个**当前根本不存在的 chunk**，照那个式子加减本身就是错的。现在改由 [`logs/firstload.js`](../logs/firstload.js) 量：用 CDP 的 `Network` 域读**浏览器在 `/` 上真正传输的字节**（`encodedDataLength`，压缩后 + 响应头），冷缓存、只算同源、按类型拆开。可复现、有唯一解释。

本轮实测（**目前只有 5 个页面，且 katex 与 markdown 尚未被任何页面引入**）：

| 请求 | 类型 | 传输 |
| --- | --- | --: |
| `/assets/vendor-element-*.js` | Script | 248.82 KB |
| `/assets/vendor-*.js` | Script | 106.54 KB |
| `/assets/vendor-element-*.css` | Stylesheet | 11.12 KB |
| `/assets/index-*.js` | Script | 5.93 KB |
| `/assets/home-*.js` | Script | 3.35 KB |
| `/assets/index-*.css` | Stylesheet | 1.65 KB |
| `/assets/home-*.css` | Stylesheet | 1.58 KB |
| `/`（文档） | Document | 0.86 KB |
| **首屏合计** | | **379.85 KB** |

拆开看：**JS 364.64 KB**、CSS 14.35 KB、文档 0.86 KB。共超标 29.85 KB（**8.5%**）。

> 数字随首页书架落地而变大（`home` 从 1.06 → 3.35 KB，多出来的是书架、抽屉与键盘导航），随后又因 `F14` 的主色改动略微变化。`docs/19` 当时记的是 **379.86 KB**（改主色前的那次构建），本行是改后重测的 **379.85 KB**——两者是**同一次构建的不同版本**，不是互相矛盾的两组数据。**结论不变**：仍然等页面齐全后再决定压不压。

已核查**按需引入确实生效**：`el-table`、`el-date-picker`、`el-cascader`、`el-transfer`、`el-calendar` 均**不在**产物中；`@element-plus/icons-vue` 未被引入（产物里只有 45 处内联 `viewBox`，不是整套图标）。

**判断**：这个数字现在仍然**不足以判定预算是否可达**——katex 与 markdown 一旦随题干渲染进入首屏（§7.10 的预算式子里明确列了它们），总账会更大；而如果它们只随题目页懒加载，首屏就只有上面这些。**本轮不优化**，等考生端页面齐全后再测一次，那时才有意义；真要压，杠杆是 Element Plus 的引入精度（`ElMessage` 目前解析到 `element-plus/es` 桶文件）与 `lodash-es`。

> 回填建议：§7.10 的预算式子应改成用**首屏同源传输字节**表述，并去掉当前不存在的 `vendor-katex` / `vendor-markdown` 两个 chunk 名——否则下一个照式子核算的人会和上一版一样算出一个无法比对的数据。

---

### F11 · 07 §7.9 钉死的 `$rk-text-3` 过不了对比度底线（高 · 已修）

**冲突内容**

[07](07-frontend.md) §7.9 的文本变量表逐字钉死：`$rk-text-1/2/3` = `#303133` / `#606266` / `#909399`。其中 `$rk-text-3`（占位、弱提示）在页面底色 `$rk-bg-page: #f5f7fa` 上实测 **2.87:1**，在卡片底色 `#ffffff` 上 **3.08:1**。

而本项目的 craft floor 把对比度写成硬底线：**"body and placeholder text ≥4.5:1"**。WCAG AA 对 18px 以下的正文/占位文字的要求同为 4.5:1。`#909399` 在两处都不达标。

**为什么不能按"这部分字不重要"放过**：这一槽位的文字量不小——实测一屏 **40 处**，且全是页面用来讲清结构事实的字：

| 用处 | 实测元素 |
| --- | --- |
| 书架每本书的编号与满分 | `span.spine__code`、`span.spine__score`（12px，一屏 40 处里的大头） |
| 每层"已印 / 总数"的计数 | `span.shelf__tier-count`（12px） |
| 图例说明与抽屉提示 | `.legend__note`、`.drawer__hint` |
| 顶栏科目下拉的占位符 | Element Plus 的 `--el-text-color-placeholder`（经 `element-override.scss` 映射到本变量） |

首页的论点是"把每本书的真实状态摆在明面上"，而承载这个论点的正是这些字。它们不可读，等于论点不可读。

**处置**：把 `$rk-text-3` 改为 `#6b6e76`。取值方式是**从达标值里挑最浅的一个**，把改动压到最小：

| 候选 | 页面底 | 卡片底 | AA 4.5:1 |
| --- | --: | --: | --- |
| `#909399`（文档值） | 2.87 | 3.08 | ✗ |
| `#767a82` | 4.01 | 4.31 | ✗ |
| `#737680` | 4.22 | 4.53 | ✗（页面底不过） |
| **`#6b6e76`（采用）** | **4.75** | **5.10** | **✓** |
| `#606266`（= `$rk-text-2`） | 5.69 | 6.11 | ✓ |

同时把 `--el-text-color-secondary` 一起映射到 `$rk-text-3`：Element Plus 的该变量默认也是 `#909399`，不一起换会在同一屏里留下两种"弱提示灰"，而留下的恰是不过标的那个。

**代价（如实记）**：`$rk-text-3`（4.75:1）与 `$rk-text-2`（5.69:1）的层次差因此变窄——三级文字层次里最下面两级现在靠得更近。这是本次取舍付出的代价；另一条路是保留文档值、改为只把上述每一处单独改色，但那会把这个槽位拆成多个值，且每加一处用法就要再判断一次。

> **本节未径自裁决的部分**：`$rk-info`（同为 `#909399`）**保持不变**——它目前只作为 CSS 变量导出、没有任何文字在用它（已核）。若将来有用它做文字的地方，同样要按本节处理。
>
> 另一处**未改**：`$rk-text-3` 在 `home.vue` 里还有两处非文字用法（层板线 `border-bottom`、书脊悬停描边），它们会跟着变深一点。层板线因此比原来更实——这与"层板要能一眼分辨出书站在什么东西上"的意图同向，故保留；若认为层板变重了，应把这两处改用 `$rk-border` 的加深档而不是回退整个变量。

---

### F12 · 展示字体没有外部来源，与"平台无衬线不作展示字体"的取向冲突（中 · 按文档处置）

craft floor 有一条：不以平台默认无衬线字体作为页面的**展示字体**（display voice）——即页面的性格应当有一部分来自字形本身。

而本项目有两条**已经钉死**的约束与之相抵：

1. [07](07-frontend.md) §7.9 钉死了变量体系，[`reset.scss`](../web/src/styles/reset.scss) 里记了"假设 A2：可能无外网"，因此**不引任何外链字体**；
2. [07](07-frontend.md) §7.10 的首屏预算（`F10`：当前 369.6 KB gzip，已超 350 KB）容不下一个中文字体子集——中文字体的最小可用子集也远超该量级。

**处置：按项目文档处置，即不引字体，页面的性格改由结构承担。** 首页的性格来自"脊高 = 卷面满分"这条**数据驱动的版式规则**（`F3` 的那个整数直接决定书脊高度）、书脊的竖排书名、以及层板/色带/蓝脊三件事同时编码三组事实——而不是来自字形。这与 craft floor 那条取向的意图（页面不该只靠默认字体说话）是一致的，只是实现路径不同。

**这不记为"已修"**：它不是缺陷修复，是一条取向在本项目前提下**按文档让步**并被记录。若将来允许引外链字体（外网可达 + 预算放宽），这一条应当重开。

---

### F13 · code-led 构建下 `comps` 阶段保持 open（中 · 记录）

impeccable 的构建状态机要求 `comps` 阶段有"已批准的 comp"才能推进，`build-phase advance` 会以 `GATE COMPS FAILED` 拦住。

**本轮的实际情况**：本方向由**决策卡**锁定（`{"optionId":"alt-shelf","buildPath":"code"}`，"书脊 = 书架"、**code-led**），且本机**没有图像生成能力**（`generate-image` 报 `OPENAI_API_KEY is not set`，也没有可用的图像工具链）。因此不存在也不可能有 comp。

**处置：不使用 `--force`，不手改 `state.json`。** `--force` 唯一被接受的理由是"用户明确降级了 comp"，本轮不属于这种情况；手改状态文件则是在伪造流程记录。按 [impeccable 的参考文档](../.claude/skills/impeccable/reference/degraded/finish-reviewer.md)：

> a code-led build has no comp round at all

`comps` 阶段**保持 open 并如实披露**，随 `F13` 一起交给 finish reviewer 与 documenter。这条记录的目的是：下一个看状态文件的人不会把它误读成"漏做了一轮"。

---

### F14 · 07 §7.9 钉死的主色过不了对比度底线；连带查出 Element Plus 主题定制是死代码（高 · 已修）

> 这一条与 `F11` 是同一类问题的第二次出现：**文档把取值钉死了，而那个取值过不了本项目自己的质量底线**。`F11` 是弱提示灰，这一条是主色，而主色的面积大得多。

#### 一、冲突内容

[07](07-frontend.md) §7.9 把主色钉死为 Element Plus 的默认蓝：

```
$rk-primary: #409eff   // 与 Element Plus 默认主色一致，减少主题定制面
```

实测（WCAG 相对亮度公式实算，脚本 [`logs/contrast.js`](../logs/contrast.js)）：

| 用途 | 底色 | 对比度 | 底线 | |
| --- | --- | --: | :-: | :-: |
| 白底上的文字 / 细线 | `#ffffff` | **2.78 : 1** | 4.5（文字）/ 3（图形） | ✗ |
| 页面底上的文字 | `#f5f7fa` | **2.59 : 1** | 4.5 / 3 | ✗ |

craft floor 的硬底线是"body and placeholder text ≥4.5:1"、图形 ≥3:1。`#409eff` **连图形线都过不了**，更不用说拿它写链接、写导航当前项、写书脚小字。

**为什么不能按"它只是装饰色"放过**：主色在页面上的落点全是**要读的东西**——品牌字标、导航当前项（含 2px 下划线）、正文里的链接、输入焦点环、书脊顶端色带、书脚小字。这些和 `F11` 那 40 处弱提示灰是同一性质：**它们承载页面的论点，不可读等于论点不可读**。

**还有一条与质量无关、但同样成立的理由**：`#409eff` 正是本文档的委托方列为禁区的"培训机构促销感"里最典型的那个默认蓝。换句话说，这个取值同时踩了两条线。

#### 二、两次尝试

**第一版（被否决）：两个蓝。** 保留 `$rk-primary: #409eff` 做填充面，另加一个 `$rk-primary-ink: #2a72c2`（白底 4.90:1 / 页面底 4.57:1）给文字与细线，铺到 6 处引用。

否决原因是**实测出来的，不是审美偏好**：它会让**同一屏出现两个蓝**——顶栏里「注册」按钮是 `#409eff` 填充，紧邻的「首页」当前项是 `#2a72c2`。而更要紧的是，多这一个变量**并没有换到达标**：`el-button--primary` 的白字压在 `#409eff` 上仍是 2.78:1。付出"两个蓝"的代价却没有解决任何问题。

**第二版（采用，已获委托方裁决）：只留一个蓝。** 把 `$rk-primary` 本身改成 `#2a72c2`，全站一个蓝。

| 候选 | 白底 | 页面底 | 结论 |
| --- | --: | --: | --- |
| `#409eff`（07 §7.9 原值） | 2.78 | 2.59 | ✗ |
| `#2f79c4` | 4.51 | **4.21** | ✗（白底刚过、页面底不过） |
| **`#2a72c2`（采用）** | **4.90** | **4.57** | ✓ |

`#2f79c4` 特意记一笔：它的白底对比度是 **4.51:1**——只比底线高 0.01。这种"刚好过线"的取值对下一个微调的人是陷阱：任何人把它调浅一丝，或者把它放到页面底（4.21，本来就不过），都会以"看不出哪里错了"的方式失效。所以宁可离底线远一点。

#### 三、连带发现：Element Plus 的主题定制整块是死代码

改主色时发现 `element-override.scss` 里那块 `@forward 'element-plus/theme-chalk/src/common/var.scss' with ($colors: ('primary': ('base': #409eff)))` **完全没有生效**。

**实测取证**：把 `@forward` 里的 base 从 `#409eff` 改成 `#2a72c2`，重新构建后产物里仍是

```
--el-color-primary:#409eff
```

**原因**：`ElementPlusResolver()` 的 `importStyle` 默认是 `'css'`，按需引入拿到的是 element-plus 的**预编译 CSS**——里面 `--el-color-primary` 已经算成字面量了。我们配置的 SCSS 变量从头到尾没有参与那次编译。`@forward` 只管得到"由本项目编译 EP 源码"那条路，而那条路根本没走。

**这意味着上一版注释里那句"改主色必须同步 element-override.scss"是错的**——同步了也没用。这类错误很隐蔽：代码看起来是对的，构建也成功，只是那一块从来没有作用。

**处置**：改成**运行期 CSS 变量**——这正是该文件对其它变量（字号、文字色、圆角）一直在用的办法。

```scss
:root {
  --el-color-primary: #{$rk-primary};
  --el-color-primary-rgb: #{rk-el-rgb-triplet($rk-primary)};
  --el-color-primary-light-3: #{rk-el-light(30%)};   // 与 EP 同一条混合公式现算
  --el-color-primary-dark-2:  #{rk-el-dark(20%)};
  ...
}
```

级联上站得住：`index.html` 里 `vendor-element-*.css` 的 `<link>` 排在项目 `index-*.css` **之前**，同为 `:root` 特异度时后出现的那条胜出（产物顺序实测如此）。派生色档用 EP 自己的混合公式（`light-i` = 白与主色混合 `i*10%`）**现算**而不是抄字面量，主色再变时它们自动跟着变。

> 附一条给下一个读产物的人：`--el-color-primary` 在**两个** CSS 文件里各有一份（EP 外链的那份 + 项目的覆盖）。读 `vendor-element-*.css` 里那份会得到 `#409eff`，那是**默认值**；实际生效的是后加载的那一份。要确认到底哪个在生效，读元素的计算值，别读文件。

#### 四、主按钮的悬停底色：两种主色都不达标，必须单独指

`el-button--primary` 的填充面是白字，而 Element Plus 默认把 **hover 指向 `light-3`（主色混 30% 白）**——那是往**浅**走，白字压上去反而更不清楚。

| 色档 | 白字在 `#409eff` 上 | 白字在 `#2a72c2` 上 |
| --- | --: | --: |
| `base`（常态） | 2.78 ✗ | **4.90 ✓** |
| `light-3`（默认 hover） | **2.03 ✗** | **2.87 ✗** |
| `light-5` | 1.63 ✗ | 2.04 ✗ |
| `dark-2`（默认 active） | 4.20 ✗ | **6.91 ✓** |

两件事因此是确定的：

1. **换主色本身救不了 hover**——`light-3` 在两种主色下都不达标。这与是否采用本条目无关，是 Element Plus 的默认取向问题。
2. **只有 `#2a72c2` 有一个可指的达标档**：`dark-2` = 6.91:1。若保留 `#409eff`，最好的档（`dark-2`）也只有 4.20:1，等于只能自造一个更深的色。

**处置**：把 hover 指向 `dark-2`。active 不用改——Element Plus 的默认值本来就是 `dark-2`，所以**按下与悬停同色**。这是本条目唯一的代价（两个状态没有区分度）；换来的是不发明一个 Element Plus 没有导出的色档（它只导出 `dark-2`，没有 `dark-3`）。

#### 五、各处落点

| 文件 | 改动 |
| --- | --- |
| `variables.scss` | `$rk-primary` 由 `#409eff` 改为 `#2a72c2`；删除短命的 `$rk-primary-ink` |
| `element-override.scss` | 删掉整块失效的 `@forward … with`；改为在 `:root` 覆盖 `--el-color-primary` 及其派生档；新增主按钮 hover 指向 `dark-2` |
| `index.scss` | 移除 `--rk-primary-ink`；`caret-color` 用主色；`::selection` 底色改由 `$rk-primary` 现算（原先是写死的旧蓝 `rgba(64,158,255,.25)`——**写死的那个会被换色落下**） |
| `reset.scss` | 链接色注释与依据更新 |
| `PortalLayout.vue` / `home.vue` / `subject-list.vue` | 6 处 `$rk-primary-ink` 引用回退为 `$rk-primary`；相关注释改为如实描述 |

#### 六、代价与偏离记录

- **这是对 07 §7.9 的第二处偏离**（第一处是 `F11` 的 `$rk-text-3`）。两处性质相同：文档钉死的取值过不了本项目自己的质量底线，所以按底线走并记录在此。
- 全站主色由 Element Plus 默认蓝变为 `#2a72c2`。回填建议：07 §7.9 的 `$rk-primary` 一行改为 `#2a72c2`，并补一段"为什么不用 Element Plus 默认主色"。
- **`F12` 的结论不受影响**：展示字体仍按文档处置（不引外链字体），页面性格仍由结构承担。本条目只动颜色。

#### 七、改后复测（全部实跑）

| 项 | 命令 / 取证 | 结果 |
| --- | --- | --- |
| 三件套 | `pnpm lint` / `type-check` / `build` | 全部 exit 0 |
| 派生色档确实按新主色生成 | 读产物 `index-*.css` | `--el-color-primary:#2a72c2`、`dark-2:rgb(34,91,155)`、`light-3:rgb(106,156,212)`——与 `logs/contrast.js` 的算式逐位相同 |
| 主按钮 hover 规则确实进了产物 | 读产物 | `.el-button--primary:not(.is-text):not(.is-link){--el-button-hover-bg-color: var(--el-color-primary-dark-2)}` |
| 渲染结果 | `node logs/measure.js` | 色带 `dy=1 86x6`、`rgb(42,114,194)`；已印描边 solid `rgb(42,114,194)`、未印 dashed `rgb(220,223,230)`；**20 根书脊 `overflowX=0`** |
| 全站文字对比度 | `node logs/inspect-pages.js`（5 个路由） | **0 处不达标**（首页 81 / 科目 255 / 登录 7 / 注册 9 / 404 3 个文字节点） |
| 设计检测器 | `impeccable detect` 打 4 个渲染后的 URL | 4 个路由全部 `[]`；阳性对照见 §5 |

---

## 4. 文档未覆盖、实现自行决定的点（补记，非出入）

| 项 | 决定 | 理由 |
| --- | --- | --- |
| `utils/request.ts` 与 store/router 的解耦方式 | 不在 request.ts 里 import store 或 router，改为暴露 `setUnauthorizedHandler()` 由 `router/index.ts` 注册 | 直接 import 会形成 `request → stores/user → api/auth → request` 的循环；用动态 `import()` 能绕开但会留下"既静态又动态引入"的构建警告，且让 `utils/` 反向认识业务层，与 §7.2 "api/ 只放接口映射、utils/ 放机制" 的分工相反 |
| `api/` 里不写尚未实现的接口函数 | `api/auth.ts` 只有第 1~4 条；`api/subject.ts` 只有列表 | 写了就是"函数存在、调用必 404"。05 §5.3 第 5~7 条随个人中心页、05 §5.4 第 9~11 条随科目详情页一起开工 |
| `stores/user.ts` 不实现 07 §7.4.1 的三个 action | 同上的三个接口未实现，故不写空实现 | 同上 |
| `Layout` 不引入 CSS 变量以外的主题层 | 只映射 §7.9 表格里的变量 | §7.9 已经钉死了取值与来源（Element Plus 默认主色），本轮没有要新增的颜色 |
| 前端不做 `ElMessage` 的显式 import | 全部交给 `unplugin-auto-import` | 显式 `import { ElMessage } from 'element-plus'` **不会带上它的样式**，表现为"提示能弹但没有样式"，看起来像 CSS 冲突、实际是引入方式问题 |

**首页（书架）的实现决定。** 07 只规定"首页展示科目"，它的视觉世界由本轮的方向契约锁定（决策卡选了 **alt-shelf**、构建路径 **code-led**），契约正文在 `.impeccable/surfaces/web-src-views-portal-home-vue.md`。以下是契约没有覆盖、由实现决定的点：

| 项 | 决定 | 理由 |
| --- | --- | --- |
| "蓝脊"的判据 | 用 `outlineStatus === 'FULL'`，**不用** `contentStatus === 'BULK'` | `contentStatus` 写着 `BULK`（已成规模）的行里，`questionCount` 是 0——两个数字并排会自相矛盾。大纲状态没有这个冲突，且它正是"这门课现在能不能真的学"的第一道门 |
| 书脊内部的固定开销下限 | 色带 6px + 书脚一行 + 4px 内边距 = 22px，`$spine-min-height: 66px` | 见下 |
| 取下来的书要滚进视口 | `toggle` 里 `scrollIntoView({ block: 'nearest' })`，并尊重 `prefers-reduced-motion` | 实测 1280×800 时抽屉落在折线以下（y=775 / 视口 702）。不滚的话点一本书的全部可见后果就是它抬起 14px，用户会以为这个交互没做完 |
| 交互提示写在导语里 | 导语末句"取下一本，看它的考期、大纲与卷面" | 抽屉自己的提示行（`.drawer__hint`）同样在折线以下，把可发现性挂在它上面等于没有 |
| 整个页面只有一处动效 | 只有"把书取下来"（`transform` + `border-color`，220ms 指数缓出），载入骨架刻意不做闪烁 | 载入态不是用户来看的东西，不该和书架争注意力 |
| 书脊的像素高度只由比例决定 | JS 只输出 `--spine-ratio`（`paperTotalScore / 225`），像素值留在 SCSS | 像素值属于样式，写两份就会有一份过期；将来出现第四个满分值时 JS 不用改 |

**关于"脊高 = 卷面满分"这条规则的实测边界（实测依据见 §5）。** 第一版把 `$spine-max-height` 定为 220px、书脚排两行，结果 75 分那本（IPT）的书脊只有 73px，而它的固定开销是 56px——书名列的可用高度被压成 **0**。竖排文字不会消失，它会溢出成 7 列（实测 `name 113x0 cols=7`）再被 `overflow: hidden` 裁掉，看上去像"书名坏了"而不是"高度不够"。

处置是把固定开销压到 22px（色带 10→6px、书脚两行并一行、内边距收紧），并把 `$spine-max-height` 降到 200px，同时用 `$spine-min-height` 把这条约束写在样式里。**当前数据下这条底线不生效**（200 × 75/225 ≈ 66.7px > 66px），所以脊高与满分仍然严格线性、图例的断言完全为真；它防的是将来出现比 75 分更矮的科目。修复后实测 IPT 为 `name 64x39 cols=4 overflowX=0`，20 根书脊全部 `overflowX=0`。

---

## 5. 实测证据汇总（未实测项据实标记）

**实跑并通过**

| 项 | 命令 | 结果 |
| --- | --- | --- |
| 后端编译 | `mvn -o clean package -DskipTests` | 通过 |
| 后端测试 | `mvn -o test` | **17 项全绿**（`rk-common` 3 + `rk-api` 14，含 ADR-02 的 6 项鉴权断言与 8 项参数校验） |
| 后端启动 | `java -jar rk-boot.jar`（环境变量取自 `deploy/.env`） | 8080（应用）+ 8081（管理端点）启动成功 |
| 科目列表真数据 | `GET /api/v1/subjects` | `code=0`；20 个科目（初 4 / 中 11 / 高 5），字段与 05 §5.4 一致 |
| 级别过滤 | `GET /api/v1/subjects?level=MIDDLE` | `JUNIOR` 为空数组、`MIDDLE` 11 条 |
| 匿名访问受保护接口 | `GET /api/v1/users/me` | **HTTP 401** + `{"code":2005,...}` + `traceId` |
| 参数校验 1001 | 非法参数注册 | `code=1001`，`data` 为字段错误列表（username 1 条、password 2 条） |
| 前端构建 | `pnpm build` | 通过，产物分包符合 §7.2.1 的 `manualChunks` 策略 |
| 前端类型检查 | `pnpm type-check`（`vue-tsc --noEmit`） | **零错误** |
| 前端 lint | `pnpm lint` | **零错误** |
| 约定检查 | `grep -rn "from 'axios'" web/src/views web/src/components` | 无结果（符合 §7.11） |
| 开发代理 | `curl http://localhost:5173/api/v1/subjects` | 200，与直连后端结果一致 |
| Redis 缺位的行为 | 注册 / 登录 | 均回 `1000`，与 [06](06-backend.md) §6.5 的约定一致 |
| `paperTotalScore` 上线（`F3` 的 c 方案） | `GET /api/v1/subjects` | `code=0`，20 条**全部非 null**：225 五个 / 150 十四个 / 75 一个（IPT），且 225 的五条 `paperType` 均为 `SENIOR` |
| 满分的来源确实是模板而非换算 | 读 `deploy/sql/init-data.sql` | `SENIOR total=225`、`JUNIOR_MID total=150`，IPT 由 `CASE WHEN s.code='IPT' THEN 75` 覆写（其「应用技术」节 `NOT_SUPPORTED`、不计入满分）；`UNIQUE KEY uk_subject(subject_id)` 保证一个科目至多一份模板 |
| 书架几何（`F3` 的消费端） | `node logs/measure.js`（CDP 读计算值） | 脊高严格线性：`h=200 / 133 / 67` 对应 `225 / 150 / 75`；**20 根书脊全部 `overflowX=0`** |
| IPT 书名的可读性 | 同上 | 修复前 `name 113x0 cols=7`（高度 0、溢出 7 列被裁）；修复后 `name 64x39 cols=4 overflowX=0` |
| 键盘导航 | `node logs/measure-interaction.js` | `SA →(→) SAD →(↓) NE →(→) ST →(End) MAD →(↑) SPM →(Home) SA`，两处夹取（列号 1 保持、列号 10 夹到 4）均正确 |
| 取下一本的完整回路 | 同上 | 点 IPT → `scrollY=162`，抽屉 `top=613 bottom=687`，视口 702 → **fullyVisible: true**；抽屉内容为 `信息处理技术员 / 上、下半年均开考 / 大纲骨架 / 0 题 / 75 分`（与接口一致）；再点一次回到提示态；`Esc` 清空选择且**焦点仍留在书脊上** |
| 桌面优先不重排（01 §1.5） | `node logs/capture.js` 于 1280 与 900 宽 | 900 宽时 `docScrollW=1280 > innerWidth=900`（出现横向滚动）且 20 根书脊宽度仍全为 `88px`、层数不变 → **未重排** |
| 设计检测器 | `impeccable detect --json --viewport 1280x800 <URL>` | 修复前 3 条；修复后 0 条（`[]`）。**但 `[]` 不等于干净**——检测器的取数方式与构建方式有关，见下 |
| `F11` 的对比度数字 | `node -e` 实算 WCAG 相对亮度 | `#909399` 页面底 2.87:1 / 卡片底 3.08:1；`#6b6e76` 4.75:1 / 5.10:1；同屏 `#909399` 文字由 **40 处降为 0 处** |
| **Redis 就位** | `redis-cli config set requirepass` + `config rewrite` | 无口令时 `ping` 现在返回 `NOAUTH Authentication required.`；带口令应答 `PONG`（口令写入服务配置第 1050 行，重启后仍生效） |
| **`F1` 的时间格式端到端** | `node logs/auth-chain.js`（真 HTTP，非 mock） | 注册的 `expireAt` / `createdAt`、登录的 `expireAt`、`/users/me` 的 `lastLoginAt` **全部**为 `yyyy-MM-dd HH:mm:ss`，逐条断言通过 |
| **登录 / 注册 / 登出全链路** | 同上 | 注册 → token → `/users/me` → 登录 → 再 `/users/me` → 登出 → 旧 token 失效 → 重新登录，**全部通过**；`mustChangePassword === false`、`role === USER`、昵称默认取用户名 |
| **`2001` / `2003` / `2004` / `2005` / `1001`** | 同上 | 逐条断言：`2001` 口令错、`2003` 用户已存在、`2005` 未登录 / token 失效、`1001` 字段错误列表；连续 5 次错误口令得到 `2001,2001,2001,2001,2004`，此后正确口令仍为 `2004` |
| **`F14` 的主色与派生档** | `node logs/contrast.js` 实算 + 读产物 `index-*.css` | `#409eff` 2.78/2.59（不过）；`#2a72c2` 4.90/4.57（过）；产物里 `--el-color-primary:#2a72c2`、`dark-2:rgb(34,91,155)`、`light-3:rgb(106,156,212)` 与实算逐位相同 |
| **`F14` 改动后的渲染** | `node logs/measure.js` | 色带仍 `dy=1 86x6`、颜色 `rgb(42,114,194)`；**20 根书脊 `overflowX=0`** |
| **`F14` 改动后的全站对比度** | `node logs/inspect-pages.js`（5 个路由） | **0 处不达标**（355 个文字节点）；0 横向溢出；0 控制台报错 |
| **`F10` 首屏体积（换测量方式）** | `node logs/firstload.js`（CDP 量真实传输字节） | **379.85 KB**（JS 364.64 + CSS 14.35 + 文档 0.86）；预算 350 KB，超 29.85 KB（8.5%） |

**未实测（如实标记）**

> 上一版这张表有五项，**其中四项已随 Redis 就位补测完毕**（见上表：`F1` 端到端、登录/注册/登出、`2001`/`2003`/`2004`/`2005`）。剩下的是下面这些。

| 项 | 为什么没测 | 影响 |
| --- | --- | --- |
| `2002` 一个错误码 | 触发它需要"账号被禁用/锁定"的前置状态，本轮没有构造该状态的数据 | 该分支**未验证**（其余四个错误码已实测） |
| `2005` 的**前端拦截器跳转**行为 | `/users/me` 的 2005 已在 HTTP 层实测；但"拦截器抓到 2005 后跳登录页"发生在浏览器里，没有在浏览器里跑过一次会话过期 | `request.ts` 的 401 分支**只有代码级证据** |
| `web/src/stores/user.ts` 的持久化与路由守卫"已登录"分支 | 同上（需要在浏览器里真的登录一次） | 只有代码级证据 |
| 登录页 / 注册页 / 科目列表页的**视觉核对** | 本轮视觉工作集中在首页（方向契约的落点） | 这三页已**打开并程序化审计**（`logs/inspect-pages.js`：标题、`h1`、正文、横向溢出、逐节点对比度、控制台报错——全为 0 异常），但**没有做过设计意义上的观看**：截图在本机读回是 `[Unsupported Image]`，量得出几何与计算值，看不出"它好不好看" |

**检测器那条 `layout-transition` 的取证（结论已更正，附更正原因）**

上一版把这条记成"**误报**"，依据是"遍历已加载样式表全部规则 —— 含 height/width/padding/margin 的 `transition` 规则 **0 条**"。**那个 0 是仪器坏了，不是页面干净**，本轮已查清并更正：

`logs/probe.js` 里那段遍历写成了

```js
if (r.cssRules) { walk(r.cssRules); continue }   // ← 错
```

Chrome 支持 CSS 嵌套之后，**每一条** `CSSStyleRule` 都带一个 `cssRules`：类型是 `CSSRuleList`，`length` 为 0，而空对象在 JS 里是 **truthy**（实测 `:root` 的 `cssRules` 是 `[object CSSRuleList]`、`length 0`、`!!` 为真）。于是 `continue` 把**每一条**规则都跳过了，规则自身的声明从来没被检查，扫描恒返回 0。

改成 `if (r.cssRules && r.cssRules.length) walk(r.cssRules)`、去掉 `continue`、并改读 `getPropertyValue('transition')` 之后，同一页面上：

| 取证 | 结果 |
| --- | --- |
| 遍历渲染元素的 `transitionProperty` 与 `transitionDuration` | 262 个元素里**动布局属性的 0 个** |
| 遍历已加载样式表的全部规则（修好之后） | 扫到 569 条规则，其中含 height/width/padding/margin 的 `transition` 规则 **3 条**（原文见下），但—— |
| 这些规则**有没有元素在用** | `.collapse-transition` / `.el-collapse-transition-enter-active` / `.el-collapse-transition-leave-active` 各 **0 个元素** → **死规则** |
| 搜本项目源码 | 只有两处 `transition`（`home.vue` 的 `transform`+`border-color`、`prefers-reduced-motion` 的 `transition: none`），**均不涉及布局属性** |
| 搜 `element-plus` 的产物 | 命中的就是上面那 3 条，全部来自它自带的 collapse 过渡，本项目从未使用 collapse 组件 |

那 3 条规则的原文（生产预览实测）：

| 选择器 | `transition` 值 |
| --- | --- |
| `.collapse-transition` | `… height …, … padding-top …` |
| `.el-collapse-transition-leave-active, .el-collapse-transition-enter-active` | `… max-height …, … padding …` |
| `.horizontal-collapse-transition` | `… width …, … padding-left …` |

> 注意第 2 条是**一条规则带两个选择器**。`docs/19` 当时记的是"一条规则、两个选择器"——那是**只找到一条**的意思：它当时是顺着 `padding-bottom` 这个 grep 命中去看的，看到的就是这一条。三条都来自 Element Plus 自带的 collapse 过渡，本项目从未使用 collapse 组件。

**上一版为什么错——两个各自独立的仪器问题**（都在本轮查清，别再重查）：

1. **`probe.js` 的遍历有 bug**（上面那段 `if (r.cssRules)`）。它对应的正是上一版表格里"遍历已加载样式表的全部规则 → 0 条"那一行。
2. **旧记录里那次 grep 的上界太短**（`[^;}]{0,80}`）：`padding-bottom` 出现在规则第 ~145 字符处，超出上界，于是"搜 `element-plus` 产物 → 0 处"那一行也报了 0。它对应的另是一行，与 bug 1 无关。

两条错在**同一张表的不同行**上，所以修好一个不一定修好另一个——这也是本轮把检测器那一节整体重写的原因。

**结论**：那 3 条规则**确实存在**（上一版"0 条"是错的），但**没有任何元素带这些类**，所以它是**死规则**——检测器报得没错，代码也没错，只是"报出来"和"有影响"是两件事。上一版把它写成"误报"是双重错误：既错在"规则不存在"，也错在把检测器的正确报警说成误报。

> **报告修订说明**：`docs/18` 的上一版 §5 与 §7 里"检测器那条是误报""样式表规则 0 条"的表述**据此更正**。改的是本文档，不是检测器，也不是页面。

**两条关于检测器本身、值得下一个人知道的事实**

1. `impeccable detect` 对 `.vue` / `.css` 文件返回 `[]` 是**"没有扫描"而不是"没有问题"**——同一份内容改成 `.html` 后立刻报出 `low-contrast` 与 `cramped-padding`（已实测）。所以 `.vue` 项目必须拿**渲染后的 URL** 作目标。

2. **检测器在"开发服务器"与"生产构建"上会给出不同答案，且差别只来自构建方式。** 本轮用两个内容等价的阳性对照实测（`logs/detector-control.html` 内联样式 / `logs/detector-control-linked.html` + `.css` 外链样式）：

   | 对照 | `low-contrast` | `layout-transition` |
   | --- | :-: | :-: |
   | 内联 `<style>` 版 | ✓ 报出 | ✓ 报出 |
   | 外链 `<link>` 版 | ✓ 报出 | ✗ **未报出** |

   即：`low-contrast` 走的是渲染后元素的**计算样式**，外链 CSS 一样看得见；`layout-transition` 读的是页面里的**内联 `<style>` 声明**，**看不见外链 CSS**。Vite **dev** 把组件 CSS 以**内联 `<style>`** 注入（报得出来），生产构建发出的是 `<link>`（报不出来）。**所以生产预览上的 `[]` 不等于"规则不存在"**——判断规则在不在，要用 `logs/probe.js` 去读 `document.styleSheets`。上一版正是在生产预览上拿到 `[]`，才有了后面一连串误判。

---

## 6. 遗留与待裁决

### 待裁决

| # | 问题 | 选项 | 我的建议 |
| :-: | --- | --- | --- |
| **F10** | 首屏 **379.85 KB** 是否现在压（预算 350 KB，超 8.5%） | a：等页面齐全再测再定／b：现在就拆 Element Plus 引入精度 | **a** |
| **F8** | 无 Redis 时"注册失败但账号已建" | 见 F8 的三种处置 | **本轮 Redis 已就位，"无 Redis"这个前提只在 Redis 挂掉时才成立**；该处置降级为"Redis 故障时的降级策略"，建议随下批一起定 |
| **F11 的回退** | `$rk-text-3` 用达标值 `#6b6e76`（本轮已改），还是回退到 07 §7.9 的 `#909399` | a：保持 `#6b6e76`（推荐）／b：回退文档值，改为逐个用法单独找达标色 | **a**（回退会让那 40 处文字重新落到 2.87:1；逐个改色则要在此后每加一处用法时重新判断） |
| **回填 05 §5.2** | `SubjectVO` 字段表要不要补上 `paperTotalScore` | a：现在就补／b：攒到下批与其它契约改动一起 | **a**（现在代码与文档不一致，正是最容易误读的时候） |
| **回填 07 §7.9（新增）** | `$rk-primary` 回填为 `#2a72c2`（`F14` 已改） | a：现在就补／b：攒到下批 | **a**（同 F11，晚了会有人照 §7.9 的 `#409eff` 把主色改回去） |
| **回填方向契约（新增）** | `.impeccable/surfaces/*.md` 与 `.impeccable/brief-body-home.md` 里**仍写着 `#409eff` 与"不做主题定制"**，与发货构建（`#2a72c2` + 运行期覆盖）不一致 | a：回填成 `#2a72c2`／b：留着，等 07 §7.9 一起回填／c：不动 | **b**（与上一条"回填 07 §7.9"是同一件事的两个落点，合在一起改一次，别分两次） |
| **种子数据自相矛盾（新增）** | `deploy/sql/init-data.sql` 里 3 个科目 `contentStatus='BULK'`（已成规模），但**全部 20 个**科目 `questionCount=0` | a：把 `BULK` 改回 `SAMPLE`（与"0 题"一致）／b：保留 `BULK`，把它定义成"大纲已成规模"而非"题库已成规模"并在 05 §5.4 写清／c：先不动，等真题导入后再看 | **b 或 a**（无论哪个，都要先明确 `contentStatus` 到底描述什么；当前它与 `outlineStatus` 的取值**在数据里完全重合**——都是 3 FULL / 17 SKELETON——两列同义，等于一列白写） |

**已裁决（本轮落地）**：

- **F3** —— 采用 **c**，且字段取整数 `paperTotalScore` 而不是原提案的布尔。依据是首页书架把脊高绑到卷面满分，布尔表达不了 225/150/75 三档。详见 §3 的 F3。
- **F14** —— 委托方裁决采用 **B（只留一个蓝）**：`$rk-primary` 本身改为 `#2a72c2`，全站一个蓝。否决了"填充用 `#409eff`、文字用深一档"的两变量方案（会**同屏出现两个蓝**，且**并没有**解决主按钮 2.78:1 的问题）。详见 §3 的 F14。

### 未决的前置条件（**本轮已解除**）

**Redis 已安装并就位**（`F7` 要求带口令；本轮实测服务配置里已持久化 `requirepass`）。上一版列在 §5 "未实测"里的四项已全部补测通过，`F8` 的前提也随之收窄为"Redis 故障时"。**当前唯一仍阻塞的验证是"浏览器内的会话过期跳转"**（见 §5 未实测表）：需要真的在浏览器里登录一次并让会话过期。

### 与前序文档的关系

| 文档 | 本文档与它的关系 |
| --- | --- |
| [15](15-implementation-deviations.md)`D1~D11` | 未复核、未改动。`D9`/`D10`（README 的状态行与快速开始）里"前端未开始"的措辞**因本轮而变得不实**，但按规则不回改，登记在此 |
| [16](16-p1-code-audit.md)`E1~E8` / [17](17-changes-p1-audit.md)`N1~N7` | 未复核、未改动 |
| [07](07-frontend.md) | 本轮按它的 §7.2/§7.3/§7.4/§7.7/§7.11 落地；出处在 §3 的 `F2`~`F6`。**§7.9 有三处按本文档处置**：`F11`（`$rk-text-3` 取值）、`F12`（展示字体）、`F14`（`$rk-primary` 取值 + Element Plus 主题定制方式）。**§7.10 的预算式子里列了两个当前不存在的 chunk 名**（见 `F10`）。**§7.4.2 的 `configuredSubjects` 写法按 `F3` 改成按 `paperTotalScore` 判断** |
| [05](05-api.md) | §5.2 的 `SubjectVO` 字段表**少一个 `paperTotalScore`**（`F3` 的 c 方案）。本轮以 `SubjectVO.java` 与本文档为准 |
| `PRODUCT.md` | 本轮未改。它的"Evidence on Hand"一节里"`web/` 0 行"这一条**已过期**（现在不是 0 行了），但按"不回改"规则登记在此，不回填 |

---

## 7. 自查

| 断言 | 取证方式 |
| --- | --- |
| `F1` 的冲突真实存在 | 读 `application.yml` 与 05 §5.1 原文；已实跑构建并确认 `JacksonConfig` 被装配（在 8080 启动日志中无异常） |
| `F2` 的两处确实不一致 | 逐字比对 05 §5.3 字段表与 07 §7.11 的代码片段 |
| `F3` 的 getter 确实恒空 | **实打** `GET /api/v1/subjects`，20 条 `paperSummary` 全为 `null` |
| `F4` 的插件确实不兼容 | 读两插件对 ESLint 配置格式的要求；替换后 `pnpm lint` 实跑零错误 |
| `F5` 的 `.d.ts` 确实放不下 | TypeScript 语义（`.d.ts` 不得含实现）；拆分后 `pnpm type-check` 零错误 |
| `F7` 的报错确实发生 | 实跑 `. deploy/.env`，得到 `line 38: -Xms512m: command not found` |
| `F8` 的现象确实发生 | 实跑注册与登录，均得 `1000`；`UserServiceImpl.register` 的事务边界读自源码 |
| `F9` 的绑定确实只在 `::1` | 实跑 `netstat -ano \| grep 5173`，并分别 curl `127.0.0.1` 与 `localhost` |
| `F10` 的数字确实如此 | 改由 `node logs/firstload.js` 量浏览器**真实传输字节**（CDP `Network`，冷缓存、同源、按类型拆分）；并按 chunk 名反查 `el-table` 等确认按需引入生效。**不再**用"构建报告若干 chunk 相加"（相加规则人为可选，见 F10） |
| 本阶段新增的契约 | `web/src/types/api.d.ts` 逐字段对照 05 §5.3 / §5.4；ID 一律 `string`、时间一律 `string` |
| `F3` 的 c 方案确实生效 | **实打** `GET /api/v1/subjects`：20 条 `paperTotalScore` 全非 null；并回读 `init-data.sql` 确认 225/150/75 来自模板而非由 `paperType` 换算 |
| `F11` 的 2.87:1 确实是实测值 | 用 WCAG 相对亮度公式**实算**（不是目测），并把候选值逐个列出；改后同屏 `#909399` 文字由 40 处降为 0 处（CDP 遍历实测） |
| `F11` 的修复没有连带破坏 | 改后重跑 `pnpm type-check`、`pnpm lint`，并重新截图核对层板线与书脚仍可读 |
| 脊高确实严格线性、且底线未生效 | `node logs/measure.js` 实测 `h=200/133/67` 对应 `225/150/75`；比对 `$spine-min-height: 66px` 确认 66.7 > 66 |
| IPT 书名的修复确实成立 | 修复前后各量一次：`113x0 cols=7` → `64x39 cols=4`，且 `overflowX=0`；20 根书脊全部 `overflowX=0` |
| 交互断言确实成立（非"应该会"） | 用 CDP 在真实浏览器里**真的点、真的发按键**：抽屉 `fullyVisible: true`、`Esc` 后焦点仍在书脊上、键盘轨迹逐跳核对 |
| 900 宽不重排确实是实测 | `node logs/capture.js` 在 900 宽下量 `docScrollW(1280) > innerWidth(900)`，同时 20 根书脊宽度仍全为 88px |
| 检测器 `[]` 的含义 | 实测：同一份内容写成 `.vue` 报 `[]`、写成 `.html` 报 2 条 → 证明 `.vue` 的 `[]` 是"未扫描"；故本轮改用渲染后的 URL 作目标 |
| 检测器在 dev 与 prod 上答案不同、且差别只来自构建方式 | 两个内容等价的阳性对照实测：内联 `<style>` 版报 `low-contrast` + `layout-transition`，外链 `<link>` 版**只**报 `low-contrast`。故生产预览的 `[]` **不能**作为"规则不存在"的证据 |
| `layout-transition` 那条**不是误报**（更正） | 修好 `probe.js` 的遍历后实测：规则 **3 条**（Element Plus 的 collapse 过渡）、带这些类的元素 **0 个** → 判定为**死规则**。上一版"0 条"是 `if (r.cssRules)` 的 truthy-空对象 bug 所致 |
| `F14` 的结论确实是实算的 | `logs/contrast.js` 用 WCAG 公式实算 `#409eff` 2.78/2.59、`#2a72c2` 4.90/4.57、各派生档，逐个列出；产物里的 `--el-color-primary` 与派生档与实算**逐位相同** |
| `F14` 的"死代码"确实是实测的（不是读代码推断） | 把 `@forward` 的 base 改成 `#2a72c2` 后重新构建，产物里**仍是** `--el-color-primary:#409eff` → 证明那一段从未参与编译；改为 `:root` 运行期变量后才变过来 |
| `F14` 的改动没有连带破坏 | 三件套重跑 exit 0；`measure.js` 复量色带/书脊不变；`inspect-pages.js` 5 个路由 355 个文字节点 **0 处不达标**、0 溢出、0 报错 |
| **登录链路确实通了（不是"应该会"）** | `node logs/auth-chain.js` 打真接口走完整回路：注册 → `/users/me` → 登录 → 再 `/users/me` → 登出 → 旧 token 401 → 重登，**全部断言通过**；含 `F1` 三种时间字段的端到端格式核对 |
| Redis 的 `requirepass` 确实持久化了 | `config rewrite` 后无口令 `ping` 返回 `NOAUTH Authentication required.`；配置文件名与写入行号已记录 |
