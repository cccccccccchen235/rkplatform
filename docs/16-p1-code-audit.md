# 16 · P1 代码审查与四项裁决的处置

> 本文档是文档链的**第四类记录**：11/13 是文档体检、12/14 是文档手术、[15](15-implementation-deviations.md) 是实现出入，**本文档是「对 P1 代码的独立审查 + 四项待裁决的处置方案」**。
>
> **边界（应要求）**：本文档**只审查、只出方案，不改 `server/` 内任何代码**。所有针对代码的修改都写成"可直接套用的补丁"，等你放行再落。
>
> **编号约定**：引用 15 已有的出入沿用其 `D` 编号；本文档**新发现用 `E` 编号**，避免与 15 的 `D1~D11` 混淆。
>
> **冲突裁决顺序**：本文档的结论与 15 并列；`E` 系列为新增，`D` 系列的复核结论与 15 一致。

---

## 1. 审查方法与范围

| 项 | 内容 |
| --- | --- |
| 基准 | [15](15-implementation-deviations.md) 的 11 条出入 + [03](03-architecture.md)（分层/AC-1~AC-5/ADR-02）+ [04](04-database.md)（DDL 唯一来源）+ [05](05-api.md)（接口契约）+ [06](06-backend.md)（分层/异常/测试要求）+ [09](09-deployment.md)（环境变量/运维） |
| 覆盖 | `server/` 六个模块的源码与配置、`deploy/sql/` 三个脚本、仓库根 `.gitignore`、`README.md` |
| 取证方式 | **全部由我逐文件读取原文 + 检索复核**。每一条结论都标明是"读到实证"还是"推断" |
| 未覆盖 | `web/`（未开工）、CI workflow（未交付）、运行期行为（未启服务，故 E4 只做到静态判定） |

> ⚠️ **方法说明**：本轮我先派了 3 个并行代理分片审计，但它们返回的都是"相关代码片段清单"，**没有给出任何判断**，无法作为审查结论。因此本文档**不采信那三份汇报的任何内容**，全部结论由我重新读代码取证。这也与 [13](13-review-round2.md) §13.6.1 的教训一致：**拿到结论的方式不可复现，结论就不算数。**

---

## 2. 四项待裁决的处置方案

图中四项与 15 §6 的待裁决项一致。以下为**可直接套用的处置**。

### D4 · Hikari 池上限：实现 10 / 文档 20 → **取 20**

**实证**：[application.yml:40](../../server/rk-boot/src/main/resources/application.yml#L40) `maximum-pool-size: 10`、[L41](../../server/rk-boot/src/main/resources/application.yml#L41) `minimum-idle: 2`；[09 §9.12.2](09-deployment.md) 与 §9.12.3 两处写 20。**06 中 `maximum-pool-size` / `Hikari` 零命中**（15 的判断属实）。

**处置（三处一起改，缺一不可）**

| # | 文件 | 动作 |
| --- | --- | --- |
| 1 | [06](06-backend.md) §6.5 | **新立规格**：新增一行「连接池上限：`maximum-pool-size = 20`、`minimum-idle = 2`、`connection-timeout = 3000ms`；20 的取值理由：目标并发 50（01 §1.5）下留一倍余量，且与 Tomcat 默认 200 线程不成瓶颈」。**这是本数字的唯一定义处** |
| 2 | [09](09-deployment.md) §9.12.2 / §9.12.3 | 两处由"写数字"改为"**引用 06 §6.5**"（按 14 的 G9 原则：只有一个出处） |
| 3 | 代码 | `application.yml:40` 的 `10` → `20`，注释改为「上限 20（06 §6.5）；空闲压到 2 条避免闲时白占连接」 |

**为什么取 20 而不是 10**：09 是**运维唯一事实来源**（其开篇自明），运维在故障时按它核对是正常动作；若实现与手册不一致，会先浪费一轮"到底哪个是对的"。另外 P4/P5 会引入组卷随机取题与统计聚合，连接占用时间比 P1 长，10 在 `connection-timeout: 3000` 下更容易变成 500。

> **顺带补一条 06 应写但没写的**：池大小必须**与 Tomcat 的 `server.tomcat.threads.max` 一起定**。池是 20、线程 200 时，池就是瓶颈，表现为"连接等待超时"；两个数字要成对出现，否则调优时不知道该调哪个。当前用默认（200 线程 / 20 连接）是合理的，但**应当写明这是有意的**。

### D7 · 哈希生成器落点：保留现状 → **回填 09**

**实证**：`main` 首行短路已实现（[RkBootApplication.java:19-21](../../server/rk-boot/src/main/java/com/ruankao/boot/RkBootApplication.java#L19-L21)）；CLI 在 [rk-api/…/cli/PasswordGenCli.java](../../server/rk-api/src/main/java/com/ruankao/api/cli/PasswordGenCli.java)，哈希实现在 [rk-common/…/security/PasswordHasher.java](../../server/rk-common/src/main/java/com/ruankao/common/security/PasswordHasher.java)。**对外行为与 09 §9.9 完全一致**（15 §7 已实测 60 字符、零 stderr、不启 Spring）。

**处置**：**保留现状**，只改 09 §9.9 的落点表述。原文是「把生成器**放进 `rk-boot`** 而非外部脚本」，改为：

> ……这也是把生成器**放进后端模块、由 `rk-boot` 的 `main` 分派**而非交给外部脚本的原因。具体落点：`PasswordGenCli` 在 `rk-api`（`rk-boot` 的分层约束只允许依赖 `rk-api`，放在这里可沿合法路径复用 `rk-common` 的 `PasswordHasher`，不必为一次性 CLI 放宽启动模块的依赖约束），哈希实现 `PasswordHasher` 在 `rk-common`，`RkBootApplication.main` 首行 `if (PasswordGenCli.tryRun(args)) return;`。

**为什么保留现状**：这不只是"依赖卫生"的偏好。`PasswordHasher` 在 `rk-common` 意味着**生成侧与校验侧共用同一份实现** —— 这正是 09 原本要防的 `$2y$` / `$2a$` 前缀哑弹。若把生成逻辑内联回 `rk-boot`，要么接受"使用未声明的直接依赖（Hutool 靠传递依赖到达）"，要么在 rk-boot 里再写一遍 BCrypt 调用、制造第二个将来会漂移的落点。**现状在两方面都更优，改回去只为忠于字面是不划算的。**

### D9 · README 行 6「代码尚未开始编写」已不实 → **改为 a**

**实证**：[README.md:6](../README.md) 现文「**当前状态：设计阶段。本文档集是开发施工图，代码尚未开始编写。**」；而 `server/` 六模块已落地、能从 `GET /api/v1/subjects` 取到 20 个科目（15 §7 实测）。

**处置**：替换为

> **当前状态：设计已定稿（`docs/01`~`10`），P1 后端骨架已落地**——六个 Maven 模块、`GET /api/v1/subjects` 垂直切片、AC-1 环境变量启动校验、`--rk.password-gen` 哈希生成器、27 张表的建库与初始化脚本均已实测通过。**前端 `web/` 未开始**，`deploy/` 的 compose 与 Nginx 配置未交付（见 [15](docs/15-implementation-deviations.md) 的 L8/L9）。

**为什么是 a**：这一行是仓库最显眼的一句话，也是访客判断项目状态**唯一**的依据。现在它把"已完成 P1 后端"说成"一行代码都没有"，是**双向**误导：既不实，又让人低估已有成果。

### D10 · README §5 快速开始照做会失败 → **先补两件产物，再改文档**

**实证（三处失败，逐条确认）**

| # | 现文 | 失败点 | 实证 |
| --- | --- | --- | --- |
| 1 | [README:107](../README.md) `cd server && mvn -pl rk-boot -am spring-boot:run` | **必然死于 AC-1 校验**：10 个必填环境变量无默认值 | [RequiredEnvValidator.java:30-40](../../server/rk-boot/src/main/java/com/ruankao/boot/config/RequiredEnvValidator.java#L30-L40) 恰好 10 个键；README 既没提 `deploy/.env` 也没给单次注入写法 |
| 2 | [README:106](../README.md)「Knife4j 文档在 `/doc.html`」 | **404**：`RK_DOC_ENABLED` 默认 `false` | [application.yml:92-99](../../server/rk-boot/src/main/resources/application.yml#L92-L99)；且 `DocAccessGuardConfig` 会把静态页也拦成真 404 |
| 3 | 全节未提 `--rk.password-gen` + 一次性 `UPDATE` | **`admin` 登录必然失败**：库里是占位串 | [init-data.sql:194](../../deploy/sql/init-data.sql) 的 `password_hash` 是占位文案 |

**处置：分两步，顺序不能反**

**第 1 步（前置产物，属 P1 漏项而非新需求）** — 这两件本来就在 [10 §10.3](10-roadmap.md) 的 P1 交付物清单里（第 2、3 条），只是没做：

| 产物 | 内容 | 依据 |
| --- | --- | --- |
| `deploy/.env.example` | 09 §9.4 的变量清单，值留空或写示例值；**必须存在**，因为 `.gitignore` 已写了否定项 `!deploy/.env.example`，而该文件当前不存在 | 09 §9.4.2 |
| `deploy/docker-compose.infra.yml` | 本地只起 MySQL + Redis，**映射 3306/6379**（与生产故意相反，理由写在文件注释里） | 09 §9.3.2 |

**第 2 步** — 用下面这份替换 README §5（每一步都对应真实存在的产物）：

````markdown
### 前置依赖

JDK 21、Maven 3.9+、Node 20+、pnpm 9+、Docker（或本地 MySQL 8 + Redis 7）。

> **装完工具链后必须注销重登或重启**（含完全退出编辑器再打开）。新开的终端/集成终端
> 继承的是父进程启动时的环境快照，否则会出现"PATH 配好了但 `mvn` 命令找不到"的假故障。

### 本地启动

```bash
# 0) 准备环境变量（AC-1：不在代码里写默认值，缺任一项会拒绝启动并一次列全缺失键）
cp deploy/.env.example deploy/.env
$EDITOR deploy/.env        # 至少填 MYSQL_PASSWORD / REDIS_PASSWORD / RK_TOKEN_TIMEOUT / RK_UPLOAD_DIR

# 1) 起依赖（MySQL + Redis）
docker compose -f deploy/docker-compose.infra.yml up -d

# 2) 建库 + 初始化 + 校验（校验脚本"违规即出行为空表"，比打印 OK 更难自欺）
mysql -h127.0.0.1 -uroot -p < deploy/sql/schema.sql
mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/init-data.sql
mysql -h127.0.0.1 -uroot -p ruankao < deploy/sql/verify.sql

# 3) 产出并回填管理员密码哈希（不做这步 admin 登录必然失败：库里是占位串）
cd server && mvn -q -pl rk-boot -am package -DskipTests && cd ..
java -jar server/rk-boot/target/rk-boot.jar --rk.password-gen='你的口令'
mysql -h127.0.0.1 -uroot -p ruankao -e \
  "UPDATE sys_user SET password_hash='<上一步输出的 60 字符哈希>' WHERE username='admin';"

# 4) 起后端（默认 8080；需先导入环境变量）
set -a && source deploy/.env && set +a
cd server && mvn -pl rk-boot -am spring-boot:run

# 5) 起前端（P1 未交付，见 docs/15 的 L9）
# cd web && pnpm install && pnpm dev
```

> - 接口文档默认**关闭**（`RK_DOC_ENABLED=false`，生产要求如此，且关掉后 `/doc.html` 返回真 404）。
>   本地要看文档：启动前 `export RK_DOC_ENABLED=true`，再访问 `http://127.0.0.1:8080/doc.html`。
> - **Windows 上用 PowerShell 而非 bash**（`set -a && source` 是 bash 语法）：
>   ```powershell
>   Get-Content deploy/.env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
>       $kv = $_ -split '=', 2; Set-Item "env:$($kv[0].Trim())" $kv[1].Trim() }
>   ```
>   或者干脆在 WSL 里跑后端（Docker 也在那儿）。
````

> **关于第 3 步的 `-DskipTests`**：P1 的测试是纯反射与纯逻辑单测（不依赖容器），其实可以照跑；这里加 `-DskipTests` 只为让"只为取一个哈希"的路径更快。**CI 里绝不允许出现这个标志**（否则等于没有测试）。

---

## 3. 15 已有条目的复核（只列我读到实证的）

| 编号 | 15 的说法 | 代码实证 | 判定 |
| --- | --- | --- | --- |
| **D1** | 白名单漏了基路径 `/api/v1/subjects` | [AuthWhitelist.java:51-54](../../server/rk-api/src/main/java/com/ruankao/api/config/AuthWhitelist.java#L51-L54)：既有 `GET /api/v1/subjects`（补的），也有 `/api/v1/subjects/**`，且类注释写明原因 | ✅ **属实**，实现是对的 |
| **D2** | 关开关 ≠ 页面 404，实现新增守卫 | [DocAccessGuardConfig.java:33](../../server/rk-api/src/main/java/com/ruankao/api/config/DocAccessGuardConfig.java#L33) 条件装配在 `springdoc.api-docs.enabled=false`；[L41](../../server/rk-api/src/main/java/com/ruankao/api/config/DocAccessGuardConfig.java#L41) 返回 `SC_NOT_FOUND`（真 404，非统一响应体） | ✅ **属实** |
| **D5** | 05 §5.1.1 缺"只适用于业务应答"的作用域限定 | [GlobalExceptionHandler.java:29-30](../../server/rk-api/src/main/java/com/ruankao/api/handler/GlobalExceptionHandler.java#L29-L30) 自述"只有 401/403 例外，其余一律 200"；而 D2 的守卫返回真 404 —— **实现里实际有三类非 200** | ✅ **属实**，建议 05 §5.1.1 补："以上只适用于**业务应答**；非业务应答（文档页守卫、静态资源、未来 Nginx 层）返回真实状态码" |
| **D6** | 05 未言明 `paperSummary` 是否下发 | [SubjectVO.java:10-12](../../server/rk-domain/src/main/java/com/ruankao/domain/vo/SubjectVO.java#L10-L12) 注释：「**列表接口返回 null**，仅 `GET /subjects/{id}` 填充（05 §5.4 第 9 条把"含 paperSummary"列为详情接口的特性）」；[SubjectController.java:45-46](../../server/rk-api/src/main/java/com/ruankao/api/controller/portal/SubjectController.java#L45-L46) 的 `@Operation` 也写明"列表不含 paperSummary" | ✅ **属实**，建议 05 §5.2 的 `paperSummary` 字段说明补一句"**列表接口恒为 null**" |
| **D11** | 04 §4.8.3 示例 INSERT 缺 `must_change_password`，照示例会得到 0 | [init-data.sql:194](../../deploy/sql/init-data.sql) 明确 6 列含 `must_change_password`；[L188-189](../../deploy/sql/init-data.sql) 注释写明"重复执行**不会**重置回 1" | ✅ **属实**，实现是对的 |

**另外核对（15 未列，但属同一类）：** 15 §5 说"未匹配路径兜底成 `1003`（HTTP 200）"—— 已确认：[GlobalExceptionHandler.java:88-93](../../server/rk-api/src/main/java/com/ruankao/api/handler/GlobalExceptionHandler.java#L88-L93) 同时接住 `NoResourceFoundException` 与 `NoHandlerFoundException`，记 DEBUG 而非 ERROR。**这是对的**：否则任何路径扫描都会制造一条"系统内部错误"，真故障会被淹没。

---

## 4. 15 未记录的新发现（E1~E8）

### E1 · 鉴权反射测试漏掉**类级**注解，且报错文案把人推向真漏洞（🔴 高）

**位置**：[AuthAnnotationTest.java:123-125](../../server/rk-api/src/test/java/com/ruankao/api/config/AuthAnnotationTest.java#L123-L125) 与 [L70-82](../../server/rk-api/src/test/java/com/ruankao/api/config/AuthAnnotationTest.java#L70-L82)

**事实**：判定函数是 `element.isAnnotationPresent(annotation)`，只作用于 **Method 上**；Java 反射的 `isAnnotationPresent` **不会**向上读取声明类的注解。而该测试的报错文案写着两种合法修法，第一种是「在方法**（或类）**上加 `@SaCheckLogin` 或 `@SaCheckRole("ADMIN")`」。

**对照**：ADR-02（03 §3.6）要求"不允许依赖全局拦截器兜底的隐式安全"；Sa-Token 的注解校验器**支持类级注解**（类上标 `@SaCheckRole("ADMIN")` 是管理端控制器的惯用写法）。

**后果（两个方向，第二个更坏）**：
1. 有人按文案在**类上**标注 → 测试仍报违规 → 白白排查一轮；
2. 更坏的：文案把「**在 `AuthWhitelist.RULES` 中登记**」列为第二种合法修法。一个被误报的开发者很可能选这条捷径 —— 而**把管理端路径写进白名单就是真漏洞**。

**是否确认**：✅ 读到实证（判定函数与文案都在代码里）。

**建议**：把判定改为能识别类级与元注解的形式，例如对 `AnnotatedElementUtils.hasAnnotation(method, X.class) || AnnotatedElementUtils.hasAnnotation(method.getDeclaringClass(), X.class)` 逐个注解求或；同时**把报错文案里的第二种修法收紧**，明确写"仅当该接口**确实应当完全公开**时"才允许登记白名单，并提示"登记前先问：无登录时误加/改请求参数会不会出问题"。

### E2 · 鉴权反射测试的扫描根是硬编码包名，包外控制器完全逃逸（🟠 中）

**位置**：[AuthAnnotationTest.java:39](../../server/rk-api/src/test/java/com/ruankao/api/config/AuthAnnotationTest.java#L39) `CONTROLLER_BASE_PACKAGE = "com.ruankao.api.controller"`

**事实**：扫描根写死为 `com.ruankao.api.controller`。任何在此包**之外**新建的 `@RestController`（例如 `com.ruankao.api.web`、`com.ruankao.admin`）都不会被扫到，测试照样通过。

**对照**：ADR-02 与 06 §6.5 要求这条测试作为"隐式安全"的防线；03 §3.4/§3.3.1 只约定"`controller`，portal / admin 分包"，**没有**规定控制器必须放在该包下。也就是说：**约束是测试实现的隐含假设，文档没背书。**

**后果**：新同事（或 AI）在 `rk-api` 下另起一个包写管理端控制器、忘了标注解 —— 编译通过、测试通过、接口裸奔，直到被人扫到。这正是 ADR-02 要堵的场景。

**是否确认**：✅ 读到实证。

**建议**：扫描根放宽到 `com.ruankao.api`（或干脆全项目 `com.ruankao`，多扫几个包的成本可忽略）；并**补一条自测**：临时在包外放一个未标注的 `@RestController`，断言测试**必须失败**（否则这道防线是否真的连着电，没人知道）。同时把"控制器必须放在 `com.ruankao.api.controller` 下"写进 06 §6.2。

### E3 · 启动失败信息引导用户去看一个**不存在**的文件（🟠 中）

**位置**：[RequiredEnvValidator.java:61](../../server/rk-boot/src/main/java/com/ruankao/boot/config/RequiredEnvValidator.java#L61)

**事实**：缺失变量时的报错文案含「请参考 **`deploy/.env.example`** 生成 `deploy/.env`」。而 `deploy/` 目录下**只有 `sql/`** —— `deploy/.env.example` 不存在（15 的 L8 也记着它未交付）；`.gitignore` 里还留着一个指向不存在文件的否定项 `!deploy/.env.example`。

**对照**：09 §9.4.2 要求 `.env` 与 `.env.example` 成对出现；10 §10.3 把 `deploy/.env.example` 列为 P1 交付物。

**后果**：**新开发者遇到的第一条错误信息就指向一个打不开的文件。** 而这条信息是 AC-1 校验的**唯一**引导，用户此时还没读过任何文档。

**是否确认**：✅ 读到实证（代码文案 + `deploy/` 实际目录）。

**建议**：与 D10 第 1 步同批创建 `deploy/.env.example`（这本来就是 P1 漏项）。若短期不创建，则把文案改为"见 09 §9.4 的环境变量清单"。

### E4 · 两条参数校验分支目前**不可达**；`@Validated` 全项目零命中（🟠 中）

**位置**：[GlobalExceptionHandler.java:52-71](../../server/rk-api/src/main/java/com/ruankao/api/handler/GlobalExceptionHandler.java#L52-L71)

**事实（已检索确认）**：
- `MethodArgumentNotValidException` 只在 `@RequestBody @Valid` 校验失败时抛出。P1 唯一接口是 `GET /api/v1/subjects`（无请求体）→ **该分支不可达**。
- `ConstraintViolationException` 分支的注释明确举例 `@PathVariable @Min(1)`。但**全项目检索 `@Validated` / `@Valid` 零命中** → 控制器上没有任何方法级校验入口 → **该分支同样不可达**；其直接含义是：**`@RequestParam` / `@PathVariable` 上即使写了约束注解也不会执行**（静默失效，不是报错）。

**对照**：06 §6.3 的异常处理顺序表把"参数校验失败 → 1001 + 字段级错误列表"列为主路径；05 §5.1 要求 `pageSize` 上限 100（越界夹到 100），这类边界正是靠参数校验实现的。

**后果**：P2 开始出现 `@RequestParam pageNum/pageSize`、`@PathVariable` ID 一类接口时，两条路都不妙：
1. 不标 `@Validated` → 约束注解**静默不生效**，非法入参一路进 Service；
2. 标了 `@Validated` → Spring Framework 6.1（Boot 3.3）在方法级校验失败时抛的是 **`HandlerMethodValidationException`**，与 `ConstraintViolationException` **不是同一类型**，会落进兜底的 `handleException` → 返回 `1000 系统内部错误` 并记 ERROR 堆栈（正是 [L73-87](../../server/rk-api/src/main/java/com/ruankao/api/handler/GlobalExceptionHandler.java#L73-L87) 那段注释说要避免的效果）。

**是否确认**：⚠️ **部分确认**。"零命中 `@Validated`/`@Valid`"与"两条分支当前不可达"是 ✅ **读到实证**；"Spring 6.1 抛 `HandlerMethodValidationException`"是 **依据框架版本的知识推断，未实测**。

**建议**：① 在 06 §6.3 明确**参数校验的统一策略**（推荐：控制器类统一标 `@Validated`，并补 `HandlerMethodValidationException` 的处理器，映射为 1001 + 字段级列表）；② 补一条测试：给某个 `@RequestParam` 加 `@Min`，传非法值，断言返回 1001 而不是 1000 —— **这条测试同时验证了"校验真的在执行"与"异常真的被接住"**。

### E5 · AC-4 的措辞与科目列表接口的设计冲突（🔵 低）

**位置**：[SubjectMapper.xml:35-55](../../server/rk-repository/src/main/resources/mapper/SubjectMapper.xml#L35-L55)（`selectSubjects` 无 `LIMIT`）

**事实**：该查询无 `LIMIT`，而 03 §3.8 的 **AC-4 写的是"所有列表接口必须分页，禁止无 limit 查询"**。但按 05 §5.4 第 8 条，科目列表就是**不分页的全量返回**（按 level 分三组），且其规模天然有上界（20 个科目）。

**对照**：实现与 05 **一致**，冲突在 AC-4 的措辞本身 —— 它把"分页"当成了目的，而真正的目的是"结果集必须有上界"。

**后果**：将来有人拿 AC-4 逐字核对，会把这条正确的查询判为违规并"修"成带分页的接口，从而与 05 的契约打架。

**是否确认**：✅ 读到实证（SQL 与 AC-4 原文）。

**建议**：AC-4 改为「**所有列表查询必须保证结果集有上界**：常规列表一律分页；枚举型全量接口（如科目列表，上界 = 科目总数）可不分页，但 SQL 仍应带一条防御性 `LIMIT`」。同时给 `selectSubjects` 补 `LIMIT 500` 之类的硬上界 —— 这是零成本的兜底。

### E6 · `deploy/sql/verify.sql` 存在但**未在任何文档登记**，而清单里的 `outline-*.sql` 至今不存在（🔵 低）

**事实**：`deploy/sql/` 实际有 3 个文件：`schema.sql`、`init-data.sql`、**`verify.sql`**。而 [04 §4.8](04-database.md) 的文件清单写的是 `schema.sql` / `init-data.sql` / `outline-*.sql` —— **`verify.sql` 没被登记，`outline-*.sql` 不存在**。

**`verify.sql` 本身质量不错**：头部写明用法，且采用"违规即出行为空表"的设计（比打印 OK 更不容易自欺），还逐表列了 27 张表做缺表检测。（15 的 L4 已记 `outline-*.sql` 属 P2。）

**后果**：一个有用的验收脚本没有文档入口，下一个人不知道它有、也不知道该在什么时机跑（它对应 10 §10.3 的 P1 验收断言）。

**是否确认**：✅ 读到实证（文件内容 + 04 清单）。

**建议**：04 §4.8 的清单补 `verify.sql` 一行（用途：建库后断言；执行方式：`mysql ... < deploy/sql/verify.sql`；判读方式：**有行输出即失败**），并把 `outline-*.sql` 标注为"P2 交付"。

### E7 · `RequiredEnvValidator` 的注释与 09 的"必填"列不完全对得上（🔵 低）

**位置**：[RequiredEnvValidator.java:22-29](../../server/rk-boot/src/main/java/com/ruankao/boot/config/RequiredEnvValidator.java#L22-L29)

**事实**：注释称这 10 个键"与 09 §9.3 环境变量表中标注'是'的条目一致"。而 09 §9.4 的表中标"是"的有 **13** 项，多出的 `MYSQL_ROOT_PASSWORD`、`TZ`、`JAVA_OPTS` 属**容器级**变量（供 mysql 容器与 `docker run` 用），应用自身确实不需要。

**对照**：**实现是对的**（不该把容器级变量设为应用启动的硬门槛，否则本地开发被无谓拦住），问题只在注释的表述会让人以为应当 1:1 对齐。

**是否确认**：✅ 读到实证（10 个键的清单 + 09 表格的"是"列）。

**建议**：注释改为"与 09 表中标注'是'**且属于应用自身**的条目一致；容器级变量（`MYSQL_ROOT_PASSWORD` / `TZ` / `JAVA_OPTS`）由 compose 提供，不在此列"，并把 `RK_UPLOAD_DIR` 单独说明（P1 尚无上传功能，但它被有意设为必填，以免上线时才暴露缺配置）。

### E8 · `10 §10.2` 的文档计数又过期了（🔵 低，G9 的同一形态）

**事实**：[10 §10.2](10-roadmap.md) 的 P0 交付物写「`docs/01` ~ `docs/14`」，而 `docs/` 下已有 `15`（以及本文档 `16`）。

**这恰好是 14 §2 的 G6 与 G9 讨论过的同一形态**：同一事实（文档篇数）被复写，然后各自演进。

**建议**：把 §10.2 的交付物改为**不写死数字**的表述 —— 「`README.md` + `docs/` 下全部文档（**以 README 的文档索引为准**）」。理由：P0 验收① 已经在 14 那轮改成了自维护表述，§10.2 仍写死数字与它不一致；这一处**已在本轮落地**（见表末说明）。

---

## 5. 本轮对文档的改动（不含 `server/` 代码）

按你的要求，**`server/` 内一个字都没动**。以下是本轮为让文档与实现对齐而**已经落地**的文档改动：

| # | 文件 | 改动 |
| --- | --- | --- |
| 1 | [README.md](../README.md) | 文档索引补入 `docs/16`；行 6 的状态改为 D9 的 `a` 方案 |
| 2 | [10-roadmap.md](10-roadmap.md) §10.2 | 交付物改为不写死篇数的表述（E8） |
| 3 | 本文档 | 新增 |

**其余 D/E 条目一律未落地**，等你放行后再按 §6 的顺序执行。

---

## 6. 建议的执行顺序

| 顺序 | 动作 | 依据 | 是否动代码 |
| --- | --- | --- | --- |
| 1 | 建 `deploy/.env.example`（E3 + D10 第 1 步） | 09 §9.4.2 / 10 §10.3 | 否 |
| 2 | 建 `deploy/docker-compose.infra.yml`（D10 第 1 步） | 09 §9.3.2 | 否 |
| 3 | 按 §2 D10 第 2 步替换 README §5（含 PowerShell 版） | — | 否 |
| 4 | 回填 6 篇文档：**06**（D4 立规格 + E4 参数校验策略 + E2 控制器包约定）、**09**（D4 两处改引用 + D7 落点 + E3 若不改代码时的文案指引）、**05**（D5 §5.1.1 作用域 + D6 §5.2 列表恒 null）、**04**（D11 §4.8.3 示例 + E6 文件清单）、**03**（E5 AC-4 措辞） | 15/16 各条 | 否 |
| 5 | 改代码 4 处：D4 池 20、E1 类级注解、E2 扫描根、E4 异常处理 + 补测试 | 15/16 各条 | **是** |
| 6 | 给 `selectSubjects` 补防御性 `LIMIT`（E5） | — | **是** |
| 7 | 补 `deploy/sql/verify.sql` 到 04 清单（E6 的文档侧） | — | 否 |

**第 4 步与第 5 步必须成对**：14 §G9 立的规矩是"同一事实只有一个出处"。若只改代码把池设成 20、而 06 不立规格，那个数字就又一次"只活在 09 的运维表里"。

---

## 7. 正面确认（同样读到实证，非"应该没问题"）

审的同时也确认了一批做法是对的，此处只列结论与证据位置，避免后人重复排查：

| 项 | 证据 |
| --- | --- |
| `schema.sql` **恰好 27 张表**，与 04 一致；**无物理外键、无 MySQL `ENUM`**；27 张全部 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`；首部有 `CREATE DATABASE ... utf8mb4 ... utf8mb4_0900_ai_ci` + `USE` | [schema.sql:21-22](../../deploy/sql/schema.sql)、27 处 `CREATE TABLE` |
| **错误码与 05 §5.1.2 逐行一致**（0 / 1000-1005 / 2001-2006 / 3001-3005 / 4001-4007 / 5001），且 `httpStatus` 只有 1004→403、2005→401 两个非 200 | [ErrorCode.java:27-62](../../server/rk-common/src/main/java/com/ruankao/common/api/ErrorCode.java#L27-L62) |
| `ErrorCode` 有**启动期重复码值自检**（静态块抛 `IllegalStateException`）与 `fromCode` 未知码兜底 | [ErrorCode.java:66-76](../../server/rk-common/src/main/java/com/ruankao/common/api/ErrorCode.java#L66-L76)、[L101-103](../../server/rk-common/src/main/java/com/ruankao/common/api/ErrorCode.java#L101-L103) |
| `SubjectMapper.xml` **无 `${}` 拼接**，列名全部显式写出（不用 `SELECT *`），表名/列名与 04 一致 | [SubjectMapper.xml:12-55](../../server/rk-repository/src/main/resources/mapper/SubjectMapper.xml#L12-L55) |
| `init-data.sql` 6 处 upsert，科目/模板/场次/分节/管理员/配置；管理员 6 列含 `must_change_password=1`，且 upsert **有意不覆盖**该列 | [init-data.sql:31/70/95/133/194/204](../../deploy/sql/init-data.sql) |
| `GlobalExceptionHandler` 的 5 条业务分支与 06 §6.3 的处理顺序表**逐条对应**，兜底**不回传异常细节**、只给 traceId | [GlobalExceptionHandler.java:38-122](../../server/rk-api/src/main/java/com/ruankao/api/handler/GlobalExceptionHandler.java#L38-L122) |
| `PasswordHasher.matches` 对非法哈希串**返回 false 而非抛异常**，使"库里还是占位串"这一可预期状态不会变成 500 | [PasswordHasher.java:41-50](../../server/rk-common/src/main/java/com/ruankao/common/security/PasswordHasher.java#L41-L50) |
| `PasswordGenCli` 三条设计约束都落实（不启 Spring、不回显明文、落点选在 rk-api 而非放宽 rk-boot 的依赖约束） | [PasswordGenCli.java:15-27](../../server/rk-api/src/main/java/com/ruankao/api/cli/PasswordGenCli.java#L15-L27) |
| `AuthAnnotationTest` 里两条"防倒退"断言是有效的：`practice/papers` 不得进白名单（B6）、白名单必须按 HTTP 方法匹配（`POST /subjects` 不放行） | [AuthAnnotationTest.java:85-105](../../server/rk-api/src/test/java/com/ruankao/api/config/AuthAnnotationTest.java#L85-L105) |

---

## 8. 文档链现状

| 序号 | 文件 | 角色 | 状态 |
| --- | --- | --- | --- |
| 11 | [11-review-findings.md](11-review-findings.md) | 第一轮体检 | 留痕，不改 |
| 12 | [12-changes-applied.md](12-changes-applied.md) | 第一轮手术 | 已含 G7 口径更正 |
| 13 | [13-review-round2.md](13-review-round2.md) | 第二轮体检 | 留痕，不改 |
| 14 | [14-changes-round2.md](14-changes-round2.md) | 第二轮手术 | G1~G9 全部落地 |
| 15 | [15-implementation-deviations.md](15-implementation-deviations.md) | P1 实现出入（11 条） | 6 待回填 / 4 待裁决 / 1 已修 |
| **16** | **本文件** | **P1 代码独立审查 + 四项裁决处置** | **四项裁决已定方案；新增 E1~E8** |

**冲突裁决顺序**：`16` 的 `E` 系列为新增；`D` 系列沿用 `15` 的结论；`15` > `14` > `13` > `12` > `11`。

**后续约定**：沿用"体检/手术分离"，且**实现期的审查另立本类文档**。下一轮（P2 完成后）另出 `17`，不修改本篇。