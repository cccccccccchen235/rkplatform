# 17 · P1 审查处置的落地记录（手术）

> 本文档是文档链的**第五类记录**：[11](11-review-findings.md)/[13](13-review-round2.md) 是文档体检，[12](12-changes-applied.md)/[14](14-changes-round2.md) 是文档手术，[15](15-implementation-deviations.md) 是 P1 实现出入，[16](16-p1-code-audit.md) 是**对 P1 代码的独立审查 + 四项裁决方案**，**本文档是那份方案的落地记录**。
>
> **边界**：本文档只记录"**实际怎么落的**"——落在哪个文件、用什么证据验过、哪些做法与 16 的字面不同及原因、哪些没验。**16 保持原样不动**（体检/手术分离的既定规矩）：16 里说错的地方在本文档更正，不改写 16。
>
> **编号约定**：沿用 16 的 `D`（承 15）/`E`（16 新增）编号；本轮**新发现用 `N` 编号**，与 `D`/`E` 区分。`15` 的遗留项仍用 `L` 编号。

---

## 1. 执行范围

[16 §6](16-p1-code-audit.md) 给出的 7 步顺序，**全部执行完毕**：

| 顺序 | 动作 | 状态 | 落地位置 |
| --- | --- | --- | --- |
| 1 | 建 `deploy/.env.example`（E3 + D10） | ✅ | 09 §9.4.1 的 20 个键、同序；凭据类一律留空（`MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` / `REDIS_PASSWORD`），README §5 的「至少填」4 项（含 `RK_TOKEN_TIMEOUT` / `RK_UPLOAD_DIR`）与之对应 |
| 2 | 建 `deploy/docker-compose.infra.yml`（D10） | ✅ | 逐字转写 09 §9.3.2，追加"决策与理由" |
| 3 | 替换 README §5（D10） | ✅ | 步骤 0~5 + 注销重登说明 + PowerShell 版 |
| 4 | 回填 6 篇文档（D4/D5/D6/E2/E4/E5/E6/E7/D11） | ✅ | 见 §2、§3（**含一处落点更正，见 N2**） |
| 5 | 改代码 4 处（D4 池 20 / E1 类级注解 / E2 扫描根 / E4 异常处理 + 测试） | ✅ | 见 §2、§3 |
| 6 | `selectSubjects` 补防御性 `LIMIT`（E5） | ✅ | `LIMIT 500` |
| 7 | `verify.sql` 登记进 04 清单（E6） | ✅ | 并**实测**了它的判读规则，见 N4 |

第 4 步与第 5 步**成对完成**（14 §G9）：池上限 20 既写进了代码，也在 **06 §6.4.1 立了规格**（唯一定义处），09 的三处引用它。

---

## 2. `D` 系列落地

| 编号 | 16 的处置 | 实际落地 |
| --- | --- | --- |
| **D1** | 复核属实，无需改 | 未动（`AuthWhitelist` 两条规则都在，注释写明理由） |
| **D2** | 复核属实，无需改 | 未动（`DocAccessGuardConfig` 返回真 404） |
| **D4** | 池上限取 **20**；06 立规格、09 两处改引用、代码改 20 | ✅ 代码：`maximum-pool-size: 20` / `minimum-idle: 2` / `connection-timeout: 3000` / `pool-name: rk-pool`。文档：规格立在 **06 §6.4.1**（不是 16 写的 §6.5，见 **N2**）；09 的引用**实为三处**（不是两处，见 **N1**），三处全改 |
| **D5** | 05 §5.1.1 补"只适用于业务应答"的作用域 | ✅ 并写清"不套统一响应体的有两类"（401/403 与守卫类），以及"进到 Spring MVC 的路径不存在仍是 `200 + 1003`"——不这么补，就会与 06 §6.3 新补的 404 行自相矛盾 |
| **D6** | 05 §5.2 的 `paperSummary` 补"列表接口恒为 null" | ✅ 并补了理由（首屏不为逐科目拼卷面结构买单） |
| **D7** | 保留现状，只改 09 §9.9 的落点表述 | ✅ 09 §9.9 的代码片段换成真实分派 `if (PasswordGenCli.tryRun(args)) return;`，落点表指向 06 §6.1（**规格的唯一出处**），09 只讲"为什么是这个落点" |
| **D9** | README 行 6 改为"设计已定稿、P1 后端已落地" | 16 §5 已落地；本轮按其产物又改了一处（见 **N5**） |
| **D10** | 先补两件产物再改 README | ✅ 见 §1 第 1~3 步 |
| **D11** | 04 §4.8.3 示例 INSERT 补 `must_change_password`，占位文案对齐脚本 | ✅ 示例改为与 `init-data.sql` 逐字一致（含 `ON DUPLICATE KEY UPDATE` 与"无实义写是刻意的"的理由） |

> **为什么 D11 要连 upsert 一起补**：04 §4.8 的表格声明 `init-data.sql` 幂等，而示例里不写 upsert，两者对不上。补上后还有一层必须讲清的：分支**只写 `username`**——如果照"看起来更完整"的写法写成 `password_hash = VALUES(password_hash)`，重跑会把线上真实哈希打回占位串、并把管理员已改过的密码重新标记为"必须改密"。**这个坑不写出来，早晚有人"顺手补全"。**

---

## 3. `E` 系列落地

| 编号 | 缺陷 | 落地 |
| --- | --- | --- |
| **E1** | 鉴权反射测试漏类级注解，且报错文案把人推向"登记白名单"这条真漏洞 | `hasAuthAnnotation` 改为 `AnnotatedElementUtils.hasAnnotation(method, X) \|\| …(method.getDeclaringClass(), X)`；文案第 2 种修法加上前置条件（"仅当无登录时被误加/改请求参数也不会出问题"）与一句警告（登记白名单不是让测试变绿的手段）。新增 `classLevelAnnotationMustBeCovered` 锁定 |
| **E2** | 扫描根硬编码 `com.ruankao.api.controller`，包外控制器完全逃逸 | 扫描根放宽到 `com.ruankao.api`；**同时把包结构约定写进 06 §6.2**（16 指出的关键：不写下来，这道防线就依赖一个测试内部的隐含假设）。新增 `scanRootMustCoverPackagesOutsideController` 锁定"扫描根必须大于等于约定覆盖面" |
| **E3** | 启动失败信息指向一个不存在的文件 | 文件已建（§1 第 1 步），文案无需再改 |
| **E4** | 两条参数校验分支不可达；`@Validated` 全项目零命中 | ① 06 §6.3 新增"参数校验的统一策略"（控制器一律标 `@Validated`、四条分支必须齐备、字段名规则）；② `GlobalExceptionHandler` 补 `HandlerMethodValidationException` 与 `MethodArgumentTypeMismatchException`；③ `SubjectController` 标 `@Validated`；④ 新增 `ParamValidationTest`（8 条）与 `everyControllerMustBeValidated`（反射扫描，防回退）。**详见的排查过程与一处顺序更正见 N3** |
| **E5** | AC-4 措辞与科目列表的设计冲突 | 03 §3.8 的 AC-4 改为"**所有列表查询必须保证结果集有上界**"，并给出"枚举型全量接口可不分页、但 SQL 仍要带防御性 LIMIT"的例外；`selectSubjects` 补 `LIMIT 500` |
| **E6** | `verify.sql` 未登记、`outline-*.sql` 不存在 | 04 §4.8 清单补 `verify.sql` 一行并注明"P2 交付"给 `outline-*.sql`；**判读方式按实测写成两类判据**（见 **N4**） |
| **E7** | `RequiredEnvValidator` 的注释与 09 的"必填"列对不上 | 注释改为"与 09 §9.4 表中标注'是'**且属于应用自身**的条目一致"，写明多出的 3 项是容器级变量、**有意不是 1:1**；`RK_UPLOAD_DIR` 单独说明。顺带修掉两处错误（见 **N6**） |
| **E8** | 10 §10.2 的文档计数过期 | 16 §5 已落地 |

### 3.1 E4 的验证方式（为什么不是"写个测试就完了"）

16 的 E4 明确标注："Spring 6.1 抛 `HandlerMethodValidationException`"是**依据框架版本的知识推断，未实测**。本轮把它变成了实测，过程里有三处只有实跑才暴露的事：

1. **standalone MockMvc 不注册方法级校验器**：`@Min(1)` 传 0 会一路进方法体，异常根本造不出来。所以那两条链路用 **直接构造异常交给处理器**（处理器是所有装配方式下的唯一出口），真实容器中的启用情况改由实机探针覆盖。
2. **构造异常的形状要跟容器一致**：用 `ParameterValidationResult` 造出来的异常会**丢掉消息**（真实容器装配的是 `ParameterErrors`，内部持有 `BindingResult`）。用后者构造才对得上生产路径。
3. **字段名来源**：这一条推翻了本轮之前的判断，见下。

---

## 4. 执行期的新发现（`N` 系列）

### N1 · 09 里的"20"其实有**三**处，16 只列了两处

16 §2 D4 写"§9.12.2 / §9.12.3 两处写 20"。实际还有第三处：[09 §9.3.1](09-deployment.md) 的**生产 compose** 里 `SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE: "20"`（它是环境变量覆盖，只覆盖池上限一项，另两项仍走 `application.yml`，所以没有行为冲突）。

**处置**：保留这个环境变量（上线后不重建镜像就能调，是运维需要的旋钮），但补上指向 06 §6.4.1 的注释，并写明"改一处就要改另一处、并复核 §9.12.2 的阈值"。**这类"多出来的同一事实"正是 G9 要防的东西，而审查最容易漏——因为它是环境变量名，不叫 `maximum-pool-size`。**

### N2 · D4 的规格落点是 06 §6.4.1，不是 16 写的 §6.5

16 §2 D4 指定"06 §6.5 新立规格"。但 **06 §6.5 是「鉴权与拦截」**——把一个连接池上限写进去，读者只会困惑。

**处置**：规格落在 **06 §6.4.1「数据库连接池上限（本数字的唯一定义处）」**，它是 §6.4「事务与并发」的子节：连接池是并发资源，与同节的并发防护表、隔离级别同处一节才讲得通。09 的三处引用、`application.yml` 的注释**一律指向 §6.4.1**。
**这是对 16 字面要求的有意偏离**（与 14 §2 记录对 13 的偏离同一处理方式）：16 的目标是"在 06 立规格、去掉 09 里的孤本数字"，这一点完全达成；具体节号按文档自身结构就近安置。

### N3 · **`-parameters` 其实是开着的**；字段名必须优先取"契约名"（本轮改动量最大的一条）

本轮早些时候的结论是"本项目未开 `-parameters`，所以 `MethodParameter.getParameterName()` 恒为 null，真正起作用的是 `@RequestParam` 的声明名"。**这个结论是错的。** 实测证据：

```text
$ mvn clean package | grep 'with javac'
Compiling 7 source files with javac [debug parameters release 21] to target\classes
$ javap -v .../SubjectController.class | grep -c MethodParameters
3
```

`-parameters` 由 `spring-boot-starter-parent` 提供（不在本仓库的 `pom.xml` 里，所以按"项目属性"检索会查不到——这正是早先判断出错的原因）。

**接下来是真正要紧的一步**：知道"形参名可解析"还不够，要知道**容器里到底命中哪一级**。两者只在"契约名 ≠ 形参名"时才可分辨，于是做了一次判别探针——把线上那个签名临时改成契约名与形参名不一致：

```java
// 临时改动（已还原）：契约名 level，形参名 lvl
@Size(max = 2) @RequestParam(name = "level", required = false) String lvl
```

```text
$ curl -s '...?level=JUNIOR'
{"code":1001,...,"data":[{"field":"lvl","message":"个数必须在0和2之间"}]}
```

拿到的是 **`lvl`（形参名）**，而客户端发出去的名字是 `level`。**这是一个真实缺陷**：前端按 `field` 定位输入框（05 §5.1.2），而它认识的是自己发出去的名字；返回形参名，前端就找不到那个输入框了。

**处置**：把 `parameterName` 的三级顺序改为 **① `@RequestParam`/`@PathVariable` 声明名（接口契约）→ ② 编译期形参名 → ③ `arg{index}`**。理由不是"哪个更准"，而是**字段名的来源应当是接口契约，而不是构建选项**：契约是对外承诺，`-parameters` 只是编译开关，后者一换父 POM 就可能静默消失。

**新增两条测试钉住这件事**：

- `declaredParamNameMustWinOverJavaParameterName`：夹具 `renamed(@RequestParam(name = "wireName") @Min(1) int javaName)`——两个名字刻意错开，断言 `field = wireName`。
- `javaParameterNamesMustBeCompiledIn`：断言形参名可从 class 文件解析出 `value`。**这条是"编译选项"的守卫**：`-parameters` 一旦丢失不会报错、不会告警，只是所有字段名悄悄退化。

**六篇文档里相应更正**：06 §6.3 的第 3 条（原写"本项目当前未开"，改为"已开启，并由测试守卫"）与新增第 4 条（契约名优先）。

> **16 的 E4 结论"容器会抛 `HandlerMethodValidationException`"经实测成立**；`@Validated` 由 16 标为"零命中"也成立。16 唯一无法回答的是"字段名从哪来"——因为它没启服务。这正是 16 §1 自己声明的边界（"运行期行为未覆盖"），本轮把它补齐了。

### N4 · `verify.sql` 的判读方式**不是**"有行输出即失败"

16 §4 E6 建议的登记文案是"判读方式：**有行输出即失败**"。**实测后不成立**：该脚本 8 个小节里有两类判据——

- **计数类**（1 表数量、2 行数、3 必填列、7 管理员与配置）：脚本**直接打印数字**，靠人比对节内注释的期望值，**必然有输出**。
- **断言类**（2 的缺表检测、4 时长不变式、5 满分口径一致性、6 的非 IPT 不支持分节）：**违规才出行**，空即通过。
- 第 6 节的 IPT 明细更是**有输出才是对的**（期望恰好 1 行 `NOT_SUPPORTED`）。

**处置**：04 §4.8 按实测写成"两类判据"表格，并说明"断言类是默认写法，只在无法用一条 SQL 表达期望值时才退回计数类"。

**实跑证据**（本机 MySQL，全新库上跑 `schema.sql` + `init-data.sql` + `verify.sql`）：

```text
表 27 / 科目 20 / 模板 20 / 场次 25 / 分节 45 / 配置键 15 / 必填列缺失 0,0,0
must_change_password = 1（全新库）
断言类各节（时长不变式、满分口径 5.1~5.4、非 IPT 不支持分节）：无行输出
第 6 节：IPT 恰好 1 行 NOT_SUPPORTED
```

> 顺带解释一处**看似失败**的现象：在本机**旧** `ruankao` 库上跑同一脚本，第 7 节会给出 `must_change_password = 0`。这不是脚本或代码的问题——该库建于 `init-data.sql` 补上这一列**之前**，而 upsert 有意不重置该列（15 的 L2 已记录此事）。**校验脚本的期望值对应的是"全新库"，不是"任何库"**，这一句本轮已写进 04。

### N5 · README 行 6 的"未交付"清单要跟着变

16 的 D9 方案落地的行 6 说"`deploy/` 的 compose 与 Nginx 配置未交付"。本轮建出 `docker-compose.infra.yml` 与 `.env.example` 后，这句话对了一半。

**处置**：改为"**生产** compose、Nginx 与前端 Dockerfile 未交付（本地开发的 `deploy/docker-compose.infra.yml` 与 `deploy/.env.example` 已交付）"，仍指向 15 的 L8/L9。

### N6 · E7 那一行的注释里还有两处错误

除 16 指出的"1:1 对齐"过度声称外，那行注释还说"09 **§9.3** 环境变量表"——而环境变量表实际在 **09 §9.4**（§9.3 是容器编排）。另外它列出的"不在此列"里有 `MYSQL_HOST`、`MYSQL_PORT`，这两个**在 09 §9.4 的表中根本不出现**（compose 里是固定值、本地走 `application.yml` 的默认值）。

**处置**：注释重写为"与 09 §9.4 表中标注'是'且属于应用自身的条目一致"，讲明 13 vs 10 的差就是那 3 个容器级变量；`MYSQL_HOST`/`MYSQL_PORT` 单独一句说清它们不在 09 表里。

### N7 · 06 §6.3 的异常表比代码少了两行

16 §7 的"正面确认"说这张表与代码"逐条对应"。实际代码有 9 个 `@ExceptionHandler`，而表里只列了 5 行——**缺 404（`NoResourceFoundException`/`NoHandlerFoundException` → `1003`）与参数类型转换（`MethodArgumentTypeMismatchException` → `1001`）**。前者是审查前就有的，后者是 E4 本轮补的。

**处置**：表补全为 9 行。**这张表的用处是"照着它写新接口时知道异常会怎么被翻译"，少一行就少一个可依赖的结论。**

---

## 5. 实测证据汇总

| 项 | 命令 / 方式 | 结果 |
| --- | --- | --- |
| 全量构建 + 测试 | `mvn clean package` | **BUILD SUCCESS**，17 tests，0 failures（`rk-common` 3 / `AuthAnnotationTest` 6 / `ParamValidationTest` 8） |
| 分层约束（AC 的 enforcer 断言） | 同一构建中的 `bannedDependencies` | 通过（六模块依赖方向未被破坏） |
| `-parameters` | `javap -v` 数 `MethodParameters` / `mvn` 的 `with javac [...]` 行 | 属性存在；编译行含 `parameters` |
| 字段名取哪一级 | 临时把契约名/形参名错开（`level` vs `lvl`），实机请求 | 返回 `lvl` → **证明命中形参名**，故调整顺序（N3） |
| 调整后的字段名 | 恢复真实签名，实机请求非法值 | `{"field":"level","message":"个数必须在0和4之间"}` |
| 类型转换失败 | `?onlyConfigured=abc` | `1001` + `{"field":"onlyConfigured","message":"参数类型不正确"}` |
| **D4 池上限的运行期读数** | 以 `--logging.level.com.zaxxer.hikari=DEBUG` 启包，读 Hikari 自打的配置块 | `poolName="rk-pool"` / `maximumPoolSize=20` / `minimumIdle=2` / `connectionTimeout=3000` |
| 基线未回归 | 实机探针 | `?level=junior` → `code 0` + JUNIOR 组；无参数 → **20** 个科目（`LIMIT 500` 不截断）；`/api/v1/users/me` 未登录 → HTTP **401** + `2005`；`/doc.html` → **404**；启动期 ERROR 行数 **0** |
| 建库脚本 | 全新库跑 `schema.sql` + `init-data.sql` + `verify.sql` | 27 / 20 / 20 / 25 / 45 / 15、`must_change_password = 1`、断言类各节无输出（N4）；**用完即删，未触碰 `library_db`/`mydb`/`testdb`** |

### 5.1 未实测（据实标记）

| 项 | 原因 |
| --- | --- |
| `docker-compose.infra.yml` 能否真的起来 | 本机**无 Docker**。它是 09 §9.3.2 的逐字转写，未验证 compose 版本兼容性与镜像拉取 |
| 生产 `docker-compose.yml` / Nginx / 前端 Dockerfile | 未交付（15 的 L8/L9） |
| `web/` 前端 | **未开工**（本轮范围外，按要求等放行） |
| CI workflow | 未建（15 的 L7） |
| 池上限在**压力**下的表现 | 只验了配置值，未做并发压测。20 这个数字的依据是 01 §1.5 的目标并发在线人数与"请求持连接的窗口只占处理时间一小部分"，属**设计推断**，非压测结论 |
| `outline-*.sql` | P2 交付，P1 上不存在（E6 已登记） |

---

## 6. 结论

- 16 的 **8 条 `E`** 与**四项裁决（D4/D7/D9/D10）**、以及复核属实的 **D5/D6/D11** 全部落地；`D1`/`D2` 复核通过无需改动。
- 执行期另发现 **7 条（N1~N7）**，其中 **N3 是唯一一条改动了生产代码行为的**（字段名取值顺序），且它源自**对先前一个错误判断的实测推翻**——这也是本文档存在的意义：16 明确声明了"运行期行为未覆盖"，而运行期一跑，结论就变了一处。
- 遗留不变：`L4`（章节树 SQL）→ P2，`L5`（首批 250 题）→ P3，`L6`（滞留结算跨午休验证）→ P5，`L7`（CI）未开始，`L8`/`L9`（生产 compose / Nginx）未交付。
- **前端 `web/` 未开始**，按约定等放行。

### 6.1 文档链现状

| 序号 | 文件 | 角色 | 状态 |
| --- | --- | --- | --- |
| 11 / 12 | [11](11-review-findings.md) / [12](12-changes-applied.md) | 第一轮体检 / 手术 | 留痕，不改 |
| 13 / 14 | [13](13-review-round2.md) / [14](14-changes-round2.md) | 第二轮体检 / 手术 | 留痕，不改 |
| 15 | [15-implementation-deviations.md](15-implementation-deviations.md) | P1 实现出入（11 条） | 6 待回填 / 4 待裁决 / 1 已修 |
| 16 | [16-p1-code-audit.md](16-p1-code-audit.md) | P1 代码独立审查 + 四项裁决方案（E1~E8） | **保持原样**（本文档更正 N2 / N4 / N7 所指之处） |
| **17** | **本文件** | **P1 审查处置的落地记录**（D/E 落地 + N1~N7 + 实测证据） | 本轮 |

**冲突裁决顺序**：`17` > `16` > `15` > `14` > `13` > `12` > `11`（新者优先；16 保持原样，其字面与本文档冲突处按本文档执行）。

**后续约定**：实现期的审查另立文档。下一轮（P2 完成后）另出 `18`，不修改本篇与 16。
