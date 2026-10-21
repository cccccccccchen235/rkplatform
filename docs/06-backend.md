# 06 · 后端设计

> 模块划分见 [03-architecture.md](03-architecture.md#33-后端模块划分)，表结构见 [04-database.md](04-database.md)，接口契约见 [05-api.md](05-api.md)。

## 6.1 模块与依赖

```
rk-boot ──→ rk-api ──→ rk-service ──→ rk-repository ──→ rk-domain ──→ rk-common
                                        （Spring JDBC / 数据源配置）
```

| 模块 | 允许依赖 | 禁止 |
| --- | --- | --- |
| `rk-common` | 无 Spring Web | 禁止依赖任何其他 `rk-*` 模块 |
| `rk-domain` | `rk-common` | 禁止依赖 MyBatis、Spring Web |
| `rk-repository` | `rk-domain`、MyBatis-Plus、Redis | 禁止包含业务判断 |
| `rk-service` | `rk-repository` | 禁止依赖 `rk-api`；禁止直接使用 `HttpServletRequest` |
| `rk-api` | `rk-service` | 禁止写业务逻辑（只做参数校验、调用、组装响应） |
| `rk-boot` | `rk-api` | 禁止包含业务代码 |

> 决策与理由
> 用 `maven-enforcer-plugin` 的 `banned-dependencies` 规则**在构建期强制**上表约束，而不是靠人自觉。理由：分层架构腐化的起点永远是"就在 Controller 里顺手写一句查询"，构建期失败比 code review 可靠。

**构建产物与一次性命令（09 文档依赖）**

1. `rk-boot/pom.xml` 必须设置 `<finalName>rk-boot</finalName>`，产物固定为 `server/rk-boot/target/rk-boot.jar`（可执行 fat jar）。09 文档的 Dockerfile 与首次上线步骤硬编码了该路径。
2. `rk-boot` 需暴露一个**一次性命令行能力**：`java -jar rk-boot.jar --rk.password-gen='<明文密码>'` 打印 BCrypt 哈希后立即退出（不启动 Web 容器）。用途是生成 `init-data.sql` 中管理员密码的占位哈希。
   > 决策与理由
   > 用应用自带的 BCrypt 实现生成哈希，而不是 `htpasswd -bnBC 10 "" 'pass'`：后者产出 `$2y$` 前缀，而 Java 侧 BCrypt 实现主要按 `$2a$` 分支解析盐值，跨实现前缀不一致会导致登录静默失败。**用同一份实现生成、同一份实现校验，永远不会遇到前缀兼容问题**，这也是把生成器放进后端模块、而不是写成外部脚本的原因。
   >
   > **落点**（本数字/布局的唯一定义处，[09](09-deployment.md) §9.9 只做引用）：`PasswordGenCli` 在 `rk-api`，哈希实现 `PasswordHasher` 在 `rk-common`，`RkBootApplication.main` 首行 `if (PasswordGenCli.tryRun(args)) return;`。为什么生成器不放在 `rk-boot`：`rk-boot` 的分层约束只允许依赖 `rk-api`，把 CLI 放在 `rk-api` 可沿合法路径复用 `rk-common` 的 `PasswordHasher`；若内联回启动模块，要么接受"使用未声明的直接依赖（Hutool 靠传递依赖到达）"，要么在 `rk-boot` 里再写一遍 BCrypt 调用、制造第二个将来会漂移的落点。

## 6.2 分层职责与命名

| 层 | 类名前缀 | 职责 | 禁止 |
| --- | --- | --- | --- |
| Controller | `XxxController` | 参数校验（`@Valid`）、调用 Service、返回 `R<T>` | 业务逻辑、事务、直接操作 Mapper |
| Service | `XxxService` / `XxxServiceImpl` | 业务规则、事务边界、缓存读写 | 依赖 `HttpServletRequest`、返回 `R` |
| Converter | `XxxConverter` | Entity ↔ DTO/VO 转换 | 业务判断 |
| Mapper | `XxxMapper` | 单表 CRUD 与统计 SQL | 业务判断 |
| Repository（可选） | `XxxCacheDao` | Redis 读写封装 | 业务判断 |

**返回类型约定**：Service 只返回 `domain` 模块的 DTO/VO，**不返回 Entity**。理由：Entity 带 `password_hash` 之类字段，一旦流向 Controller 就有泄漏风险。

**异常约定**：Service 抛 `BizException(ErrorCode)`；Controller 与 Service **都不写 try-catch**，统一由 `GlobalExceptionHandler` 转换为 `R`。

**包结构约定（对应 [16](16-p1-code-audit.md) 的 E2）**：`rk-api` 下的控制器**必须放在 `com.ruankao.api` 之内**——考生端 `com.ruankao.api.controller.portal`，管理端 `com.ruankao.api.controller.admin`（03 §3.3.1）。

> 决策与理由
> 这条不是命名品味，而是 ADR-02 那道反射扫描防线的**生效前提**：扫描测试以 `com.ruankao.api` 为根包，包外新起的 `@RestController`（例如 `com.ruankao.api.web`）同样会被 Spring 注册为接口，却**不会被扫到**，测试照样通过——接口就裸奔了。原实现把扫描根写死为 `com.ruankao.api.controller`，却没在任何文档里写下这条约束，等于让防线依赖一个测试内部的隐含假设；约束写在这里，扫描根同时放宽到 `com.ruankao.api`，两边才对齐。

## 6.3 统一响应、异常与校验

```java
// rk-common
public record R<T>(int code, String message, T data, String traceId) {
    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> fail(ErrorCode ec) { ... }
}
```

`ErrorCode` 为枚举，字段：`code`、`message`、`httpStatus`。取值与 [05-api.md](05-api.md#512-错误码表) 的错误码表**一一对应**，新增错误码必须两处同步。

`GlobalExceptionHandler` 处理顺序：

| 异常 | 处理 |
| --- | --- |
| `BizException` | 按其 `ErrorCode` 返回 |
| `MethodArgumentNotValidException` | `@RequestBody @Valid` 失败 → `1001`，`data` 为 `[{field, message}]` |
| `HandlerMethodValidationException` | 方法参数约束校验失败（**Spring Framework 6.1 起的主路径**）→ `1001` |
| `ConstraintViolationException` | 方法参数约束校验失败（旧路径，Spring 6.0 及以前）→ `1001` |
| `MethodArgumentTypeMismatchException` | 参数类型转换失败（`?pageSize=abc`）→ `1001` |
| `NoResourceFoundException` / `NoHandlerFoundException` | 路径不存在 → `1003`（HTTP 状态码仍为 200） |
| `NotLoginException`（Sa-Token） | 返回 `2005`，HTTP 401 |
| `NotRoleException` / `NotPermissionException` | 返回 `1004`，HTTP 403 |
| 兜底 `Exception` | 记 ERROR 日志（含 `traceId` 与堆栈），返回 `1000`，**不回传异常细节** |

`traceId` 由 `TraceIdFilter` 生成（`MDC` 注入 UUID 前 8 字节十六进制），写入日志与响应，贯穿全链路。

**参数校验**：DTO 上用 Jakarta Validation（`@NotBlank`、`@Size`、`@Min`、`@Pattern`、自定义 `@EnumValue`）。**跨字段规则**（如"客观题必须有 `correctAnswer`"）写在 Service 层，因为注解表达不了。

**参数校验的统一策略（对应 [16](16-p1-code-audit.md) 的 E4）**：

1. **控制器类一律标注 `@Validated`**。这是 Spring 6.1 启用"方法参数上的约束注解"的**前提**：没有它，`@RequestParam` / `@PathVariable` 上写的 `@Min`、`@Size` 等**静默不生效**——不报错、不拦截，非法入参一路进 Service。这类失效没有任何运行时症状，只能靠约定与测试守。已由 `ParamValidationTest#everyControllerMustBeValidated` 反射扫描全部 `@RestController` 强制。
2. **上表四条"入参不合法"的分支必须齐备**。缺任何一条，对应请求都会落进兜底分支被报成 `1000 系统内部错误` 并按 ERROR 记一条堆栈，把"用户把 `pageSize` 传成 `abc`"说成"系统挂了"——既误导前端提示，也让真故障淹没在噪音里。
3. **字段名必须是客户端认识的那个名字**。`1001` 的 `data` 是 `[{field, message}]`（[05 §5.1.2](05-api.md#512-错误码表)），前端按 `field` 定位输入框，而它认识的参数名是**自己发出去的那个**。参数名按三级回退取：**`@RequestParam` / `@PathVariable` 显式声明的名字（接口契约）** → 编译期形参名（需 `-parameters` **且** `MethodParameter` 带参数名发现器；`-parameters` 由 spring-boot-starter-parent 提供，**本项目已开启**，由 `ParamValidationTest#javaParameterNamesMustBeCompiledIn` 守卫）→ `arg{index}` 兜底。"取不到就留空"是不可接受的退化：错误会变成"有个参数错了，错在哪个自己找"。
4. **契约名必须优先于形参名**。两者在源码里通常写成同一个（`@RequestParam(name = "level") String level`），但一旦写岔（`@RequestParam(name = "pageSize") int page`），形参名 `page` 对前端就是个陌生字段。这条已实机取证并由 `ParamValidationTest#declaredParamNameMustWinOverJavaParameterName` 锁定。

> 决策与理由
> 为什么把 `HandlerMethodValidationException` 单列而不是复用 `ConstraintViolationException`：**两者不是同一类型**。Spring 6.1 把"控制器方法参数校验失败"统一改抛 `HandlerMethodValidationException`（它是 `ResponseStatusException` 的子类），`ConstraintViolationException` 那条分支在新版本里基本不再被触发。只按旧类型写处理器，等于在框架升级后**静默失去**这条链路——异常照样抛出，只是落到了兜底分支，症状是"接口能用，但非法参数返回的是系统内部错误"。
> 为什么同时接 `MethodArgumentTypeMismatchException`：它与上面同源，都是"入参不合法"而非服务端故障。这是本轮审查发现的相邻缺陷（[16](16-p1-code-audit.md) §4 E4）：一个拼错的查询参数就能制造一条"系统内部错误"日志。

## 6.4 事务与并发

| 场景 | 事务边界 | 说明 |
| --- | --- | --- |
| 答题上报 | `PracticeService.report` 单事务 | 写流水 + 更新错题本 + 更新 `stat_daily`；**统计重算放到事务提交后** |
| 交卷判分 | `ExamService.submit` 单事务 | 更新会话 + 逐题判分 + 更新错题本 + 汇总分节得分 |
| 审核裁决 | `ReviewService.review` 单事务 | 更新题目状态 + 写流转日志 + 更新冗余计数 |
| 组卷 | **不开启事务**（只读） | 组卷是读操作，写入发生在创建会话时 |
| 创建会话 | `ExamService.create` 单事务 | 写 `exam_session` + `exam_session_section` + N 条 `exam_session_item` |

隔离级别：默认 `READ_COMMITTED`（MySQL 默认 `REPEATABLE_READ`，本项目无跨行读一致需求，改低一级减少间隙锁）。

**并发防护**：

| 风险 | 防护 |
| --- | --- |
| 同一用户并发发起两次模拟考 | 应用层先查 `CREATED`/`IN_PROGRESS` 会话；同时对 `rk:exam:creating:{userId}` 加 `SETNX` 短锁（3 秒）防双写 |
| 重复上报答题记录（网络重试） | 5 秒内同一 `(userId, questionId, source)` 视为重放，Redis `SETNX` 去重；流水表本身允许重复行（不同时间作答同一题是合法的） |
| 交卷与超时自动交卷并发 | 交卷操作以 `UPDATE exam_session SET status='SUBMITTED' WHERE id=? AND status IN ('CREATED','IN_PROGRESS')` 的**影响行数**作为唯一裁决依据，影响 0 行则判定已被结算，直接返回既有结果（幂等） |
| 题目并发审核 | `UPDATE rk_question SET status=? WHERE id=? AND status=?` 乐观锁式更新，影响 0 行返回 `3003` |

> 决策与理由
> 不引入分布式锁组件（Redisson）。单实例部署下，`SETNX` 短锁与数据库条件更新已足够；引入 Redisson 只增加一个依赖和一套新的失败模式。**若将来多实例部署，这里必须改为 Redisson**（已在 03 文档 ADR 中标注单机前提）。

### 6.4.1 数据库连接池上限（**本数字的唯一定义处**）

HikariCP 配置（[application.yml](../../server/rk-boot/src/main/resources/application.yml)）：

| 参数 | 值 | 说明 |
| --- | --- | --- |
| `pool-name` | `rk-pool` | 日志与 `SHOW PROCESSLIST` 里可辨识，便于把"连接是谁占的"对到应用 |
| `maximum-pool-size` | **20** | 连接数上限。见下方取值理由 |
| `minimum-idle` | **2** | 空闲压到 2 条：闲时白占着数据库连接没有收益，而重建连接的成本（毫秒级）远低于长期闲置的代价 |
| `connection-timeout` | **3000**（3 秒） | 取不到连接就**快速失败**，不把线程挂在这里 |

**20 的取值理由**：[01 §1.5](01-requirements.md) 的目标是「支撑 50 并发**在线用户**」（单机 2C4G）。注意 50 是**在线人数**，不是"同一毫秒有 50 个查询在跑"——一次请求里真正持有连接的只有 SQL 执行那一段（毫秒级），参数校验、缓存读写、序列化都不占连接。按"任意时刻最多约四成在线用户同时停在 DB 上"这一保守估计取 20；同时它相对原实现的 10 留出了余量，避免 P4/P5 组卷与判分（一次会话要写 N 条 `exam_session_item`）这类偏长的事务把后来的请求排到 `connection-timeout`。

> 决策与理由
> **池大小必须与 `server.tomcat.threads.max` 成对看，不能单独调。** Tomcat 默认 200 个工作线程，是连接上限的 10 倍：线程数远大于连接数时，瓶颈就是池本身，故障表现不是"响应慢"而是**"连接等待超时"**（3 秒后成批失败）。本项目**有意保留这一默认组合**——它的效果是"过载时快速失败并留下清晰日志"，比让 200 个线程一起慢下来更容易定位。若某天要调，两个数字要一起调，并把新的组合写回本表；`09 §9.12.2` 的运维阈值与 §9.12.3 的排查表**引用本节，不各自维护数字**（对应 [16](16-p1-code-audit.md) 的 D4 与 14 §G9）。

## 6.5 鉴权与拦截

Sa-Token 配置：

```yaml
sa-token:
  token-name: Authorization
  token-prefix: Bearer
  timeout: 2592000          # 30 天（秒），由环境变量 RK_TOKEN_TIMEOUT 覆盖
  active-timeout: 604800    # 7 天无操作过期
  is-concurrent: true       # 允许同一账号多端登录
  token-style: uuid
  is-log: false
```

- `rk-api` 提供 `StpInterfaceImpl` 实现，从 `sys_user.role` 返回角色，供 `@SaCheckRole("ADMIN")` 使用。
- 全局拦截器注册：`SaInterceptor` 拦截 `/api/**`，仅做"是否登录"的粗粒度判断；**细粒度权限一律用注解**。
- 白名单（[05-api.md](05-api.md#513-鉴权白名单)）以配置类 `AuthWhitelist` 中的**常量数组**定义，拦截器与单元测试共用同一份常量，避免两处漂移。

**强制要求（对应 ADR-02）**：

```java
@Test
void allControllerMethodsMustDeclareAuth() {
    // 反射扫描 rk-api 下所有 @RestController 的 public 方法
    // 未标注 @SaCheckLogin / @SaCheckRole / @SaIgnore 且不在 AuthWhitelist 中的 → fail
}
```

这条测试是"隐式安全"的防线：**不允许出现"靠全局拦截器兜底"的接口**。

密码与登录安全：

| 项 | 实现 |
| --- | --- |
| 密码存储 | BCrypt，`strength = 10`（约 100ms/次，单机可接受） |
| 登录失败计数 | `rk:login:fail:{username}`，`INCR` + `EXPIRE 900`，达 5 次返回 `2004`；登录成功 `DEL` |
| 用户名不存在时 | 仍执行一次 BCrypt 比对（用固定假哈希），**保证响应时间不泄漏"用户是否存在"** |
| 登出 | `StpUtil.logout()`，服务端删除会话 |
| 管理员重置密码 | 随机 12 位密码，返回一次明文并强制首次登录改密 |
| 首次登录强制改密 | 策略总开关取 `sys_config` 的 `admin.must.change.password`（默认 `true`）；管理员重置密码时置该用户 `mustChangePassword = true`，用户改密成功（`PUT /users/me/password`）后置 `false`。对外由 `UserVO.mustChangePassword` 暴露（登录响应与 `GET /users/me`），**不新增专用错误码**，前端凭该字段跳改密页（对应评审 C2）；持久化列为 `sys_user.must_change_password`（已存在于 04，对应评审 G8） |

## 6.6 组卷算法

组卷是练习与模拟考的公共能力，实现于 `QuestionPickService`。

### 6.6.1 输入与输出

```java
public record PickRequest(Long userId, Long subjectId, String questionType,
                          int count, Integer difficulty, PickMode mode,
                          Long outlineId, Long kpId, Integer fromSequence) {}

public record PickResult(List<Long> questionIds, int actualCount,
                         int repeatedCount, List<Long> uncoveredOutlineIds) {}
```

### 6.6.2 候选池构造（模拟考·按章节配额）

**Step A｜章节配额分配（最大余数法）**

1. 查该科目全部**叶子章节**及其已发布题量：`rk_outline_node.question_count`（冗余列，免 join）。
2. 只保留 `question_count > 0` 的章节，得集合 `S`。
3. 若 `count >= |S|`：先给每章保底 1 题；剩余 `count - |S|` 题按题量占比用最大余数法分配。
   若 `count < |S|`：按题量占比用最大余数法随机取 `count` 个章节，各 1 题（**尽力覆盖，不保证全章节**）。
4. 输出 `Map<outlineId, quota>`。

**Step B｜章节内抽题**

```sql
-- 单章节抽题（章节内题量通常 < 200，ORDER BY RAND() 完全可接受）
SELECT q.id
FROM rk_question q
WHERE q.subject_id = #{subjectId}
  AND q.outline_id = #{outlineId}
  AND q.status = 'PUBLISHED'
  AND q.question_type = #{questionType}
  AND q.difficulty BETWEEN #{minDifficulty} AND #{maxDifficulty}
  AND q.id NOT IN (${excludeIds})      -- 防重复窗口内的题，见 6.6.4
ORDER BY RAND()
LIMIT #{quota};
```

难度分档（`difficulty` **1 最易、5 最难**，与 [02-domain-model.md](02-domain-model.md#222-题目内容结构约定) 的字段定义一致）：

| 档位 | `difficulty` 取值 | 目标占比 |
| --- | --- | --- |
| 基础 | 1~2 | 30% |
| 进阶 | 3 | 50% |
| 挑战 | 4~5 | 20% |

按占比拆配额后**逐档抽取**；某档题量不足时，缺口由相邻档位补足（先进阶，再基础，最后挑战）。

**Step C｜降级与兜底（必须严格按序）**

| 级别 | 触发条件 | 动作 |
| --- | --- | --- |
| L1 | 某章节配额内可选（含排除集过滤后）不足 | 相邻档位补足 |
| L2 | 仍不足 | **放宽防重复**：按 `last_answered_at` 升序（最早做过的先复用）补足，`repeatedCount` 累加 |
| L3 | 仍不足 | 跨章节补足：按其他章节题量降序取，优先取未达配额的章节 |
| L4 | 仍不足 | 缩减题量，`actualCount < count`，通过 `uncoveredOutlineIds` 返回未覆盖章节；前端提示"题库存量不足，本次试卷共 N 题" |

> 决策与理由
> 降级顺序体现优先级：**不重复作答 > 章节覆盖 > 难度配比 > 题量达标**。用户对"抽到刚做过的题"的容忍度最低，因此 L2 放在"跨章节"之前；而"题量"最后让步，因为一份 70 题的模拟卷仍可用，一份章节全错位的卷子没有练习价值。
> 题量上限校验在**创建会话之前**完成：若该科目已发布客观题总量 < 模板要求题数，直接返回 `4002`，不生成残缺试卷（避免产生无法解释的低分成绩）。

### 6.6.3 候选池构造（练习）

练习模式不需要章节配额，逻辑更简单：

| mode | 取题条件 |
| --- | --- |
| `CHAPTER` | `outline_id = refId`（含子章节：`outline_id IN (该节点及其子孙)`） |
| `KP` | `JOIN rk_question_kp ON kp_id = refId` |
| `SEQUENCE` | 按 `id ASC` 且 `id > fromSequence` 顺序取，不随机 |
| `FAVORITE` | `JOIN user_favorite` |
| `WRONG_REDO` | `JOIN user_wrong_question WHERE status = 'ACTIVE'`，按 `wrong_count DESC` 优先 |
| `SAMPLE` | 全科目随机，`count ≤ 5`，供游客 |

练习模式**不做难度配比**（用户按自己的意愿挑章节，硬性配比反而干扰），但 `onlyUnmastered = true` 时排除 `stat_user_kp.mastery >= 0.6` 的知识点下的题。

### 6.6.4 防重复窗口

- 窗口值：`sys_config` 的 `question.repeat.window`（默认 30 天）。
- 存储：Redis `SET` —— `rk:repeat:{userId}:{subjectId}`，成员为题 ID，`TTL = 窗口天数`。
  缓存未命中时从 `user_answer_record` 重建：

```sql
SELECT DISTINCT question_id FROM user_answer_record
WHERE user_id = ? AND subject_id = ? AND answered_at >= DATE_SUB(NOW(), INTERVAL 30 DAY);
```

- 写入时机：`PracticeService.report` 与 `ExamService.submit` 成功后 `SADD`。
- **重建是幂等且可丢失的**：这个 Set 是纯粹的性能优化，丢失只会让防重复短暂失效，不影响数据正确性。**因此不做持久化保证。**

> 决策与理由
> 用 Redis Set 而不是每次都写 SQL 子查询。理由：`IN (SELECT ...)` 在 400 万行流水表上会随用户活跃度增长而变慢，而单个用户的窗口内题量有上界（≤ 该科目题量，通常 ≤ 300），Set 的 `SMEMBERS` 是 O(N) 但 N 很小。且它天生可丢可重建，不构成一致性负担。

### 6.6.5 随机取数的规模阈值

| 单次候选池规模 | 策略 |
| --- | --- |
| < 2000 | `ORDER BY RAND() LIMIT n`。理由：MySQL 的 `RAND()` 会对候选集排序，2000 行以内的排序开销可忽略 |
| ≥ 2000 | **随机偏移多轮取样**：取 `MIN(id)`/`MAX(id)`，随机生成 3×n 个候选 id，按 `id >= candidate ORDER BY id LIMIT 1` 采样，去重后取够 n 个 |

当前阶段所有科目单章节池均 < 2000，实现上先只落 `ORDER BY RAND()` 分支，但**接口按策略可切换设计**（`PickStrategy` 接口 + 两个实现），避免将来题量增长时改业务代码。

## 6.7 判分逻辑

`JudgeService.judge(question, userAnswer)` → `JudgeResult(boolean isCorrect, BigDecimal score)`。

```java
switch (question.getQuestionType()) {
    case SINGLE_CHOICE, TRUE_FALSE ->
        isCorrect = normalize(userAnswer).equals(question.getCorrectAnswer());
    case MULTI_CHOICE -> {
        // 拆成字符集合比较，顺序无关；完全相等才得分，不给部分分
        isCorrect = sortedChars(userAnswer).equals(sortedChars(question.getCorrectAnswer()));
    }
    case SUBJECTIVE, ESSAY ->
        // 不判分：isCorrect = null，score = 0
        return JudgeResult.unscored();
}
```

| 规则 | 说明 |
| --- | --- |
| 归一化 | 去首尾空白、统一大写；空串视为未作答 |
| 未作答 | `isCorrect = false`，得 0 分（但不写入错题本，见 6.8） |
| 多选不给部分分 | 与官方一致：多选题漏选、多选均不得分 |
| 主观题 | `is_correct` 落库为 `NULL`，`score = 0`，**不参与任何分数计算** |
| 论文 | 同上；字数统计由前端完成，服务端**不做字数校验**（避免与真实考试规则产生认知偏差），仅校验长度上限 20000 字符 |
| 分数精度 | `DECIMAL(4,1)`；客观题每题 1 分，主观题每题按模板分值但记 0 分 |

**成绩汇总**：

```java
objectiveScore = sum(items where questionType in 客观题 and isCorrect)
sectionScores  = 按 sectionNo 分组求和
accuracy       = correctCount / objectiveCount      // 分母不含主观题
```

## 6.8 错题本

触发点：`PracticeService.report` 与 `ExamService.submit` 均为每题调用 `WrongBookService.onAnswered(userId, questionId, isCorrect)`。

| 事件 | 动作 |
| --- | --- |
| 答错（`isCorrect = false`） | `wrong_count + 1`、`consecutive_correct = 0`、`last_answered_at = now`、`status = 'ACTIVE'`（**若原为 `REMOVED` 则恢复为 `ACTIVE`**） |
| 答对 | `correct_count + 1`、`consecutive_correct + 1`、`last_answered_at = now`；若 `consecutive_correct >= 2` 则 `status = 'MASTERED'` |
| 未作答（空答案） | **不收录、不更新**。理由：未作答是"没做完"而非"不会做"，收录会污染错题本的语义 |
| 用户手动移除 | `status = 'REMOVED'` |
| 用户手动恢复 | `status = 'ACTIVE'`，`consecutive_correct = 0` |

实现为 `INSERT ... ON DUPLICATE KEY UPDATE`（依赖 `uk_user_question`），一次 SQL 完成 upsert，避免读改写竞态。

移出规则为何是"连续答对 2 次"：只对 1 次就移出会让用户抓不住重点，要求 3 次以上会让错题本永不缩减。2 次是**可配置项**（`sys_config` 的 `wrongbook.master.threshold`，默认 2），便于按实际体验调整。

## 6.9 模拟考会话与计时

### 6.9.1 计时模型（关键设计）

官方机考的真实规则是：**同一场次内总作答时长共享**，分节只约束"最早可以离开本节的时间"与"最晚可以停留的时间"；**场次之间不自动衔接**（对应评审 A5，权威定义见 [02-domain-model.md](02-domain-model.md#24-卷面模型场次--分节两层结构) §2.4 与 §2.5.5）。

计时粒度由"全卷一条倒计时"改为**场次级**：每个场次各有一条独立倒计时与独立 `started_at`。

| 概念 | 计算 |
| --- | --- |
| 计时真值来源 | `exam_session_sitting.started_at`（唯一权威，每次读取时重算） |
| 场次剩余时长 | `remainingSec = max(0, sitting.total_duration_sec - (now - sitting.started_at))`，**时间全部取服务端**；**仅当该场次 `status = IN_PROGRESS` 时计时**，`PENDING`（`started_at` 为空）不倒计时 |
| 本节已用 | `usedSec = now - section.started_at`（服务端时间） |
| 场次内共享 | 同一场次的所有分节共享 `sitting.total_duration_sec` |
| 最早可离开本节 | `usedSec >= section.duration_min` |
| 最晚必须离开本节 | `usedSec >= section.duration_max`（达标即强制离开本节，进入下一节或结束本场次） |
| 论文节 | `allowEarlySubmit = false`：不接受"结束本节"动作，必须等本场次时长耗尽或整体交卷 |
| 场次衔接 | 场次一结束后场次二为 `PENDING`，**不自动开始**；用户调 `POST /exams/{id}/sittings/{sittingNo}/start` 才开始计时 |
| 长时钟漂移 | 不依赖客户端时间戳；`PUT /exams/{id}/answers` 返回服务端 `remainingSec`，前端仅用于展示与校准 |

> 计时权威（对应评审 A1）
> `exam_session.remaining_sec` 与 `exam_session_sitting.remaining_sec` 都是**展示用缓存**，真值一律由 `exam_session_sitting.started_at` 派生并每次重算；Redis 只承载会话状态与短锁（如 `rk:exam:creating:{userId}`），**不承载计时增量**。客户端时间戳不可信（约束 C4）。此处与 04 的 DDL 字段注释保持一致。
>
> **两个 `remaining_sec` 的写入时机与读取优先级（对应评审 G4）**：`exam_session.remaining_sec` 是**当前场次那个值的镜像**，只在场次开始 / 作答写入 / 场次结束时随 `exam_session_sitting` 一并同步更新，**不独立演进**；**任何读取一律以 `exam_session_sitting` 为准**，`exam_session` 上的那份仅为列表等场景的便捷字段。这样即使镜像因异常中断而落后，也不会污染判定——因为它从不参与判定。

> 决策与理由（对应评审 A5）
> 早期模型把一份试卷当成"一个会话 + 一条连续倒计时"，无法表达高级科目的两场次结构。高级的两场次之间**隔着午休**（上午连考 240 分钟、下午论文独立 120 分钟），若按一条连续倒计时计算，考生在午休时会被凭空扣掉论文的时间。因此每个场次各有独立的 `started_at` 与倒计时，**场次之间不自动衔接，场次二必须由用户主动开始**。初级/中级只有 1 个场次（连考 240 分钟共享），不涉及该分支。
>
> 分节上原有的「结转」相关字段已从 DDL 删除（对应评审 B11）：在"场次内总时长共享"的模型下，节余时长**自动可用、无需申请结转**，该字段既非判定依据也无法配置。若仍需分析"每节实际可用时间"，可由 `sitting.total_duration_sec - Σ(同场次更早分节的 used_sec)` 派生，**无需额外存储**。

### 6.9.2 状态流转与超时结算

```
CREATED ──首次开始场次（sittings/{sittingNo}/start）──→ IN_PROGRESS ──交卷/超时──→ SUBMITTED ──判分──→ SCORED
   │                                                              
   └──用户主动放弃──→ ABANDONED（不计入成绩统计）
```

会话状态不变（`CREATED/IN_PROGRESS/SUBMITTED/SCORED/ABANDONED`）：一个会话下可挂多个场次（`exam_session_sitting`），会话在全部场次 `FINISHED` 之后才 `SUBMITTED`。

**超时结算按场次进行**，采用"惰性 + 兜底"双保险：

1. **惰性**：任何读写会话/场次的请求入口（`GET /exams/{id}`、`PUT /answers`、`POST /exams/{id}/sittings/{sittingNo}/start`、`POST /exams/{id}/sittings/{sittingNo}/finish`、`POST /exams/{id}/sections/{sectionNo}/advance`、`POST /submit`）统一调用 `ExamSessionGuard.ensureNotExpired(session)`；若当前 `IN_PROGRESS` 场次的 `remainingSec == 0`，立即结束该场次 —— **有下一场次则置为 `PENDING`（等待用户主动开始），已是最后一场次则触发整卷交卷**，并返回 `4005` 或结算结果。
2. **兜底**：定时任务每 5 分钟扫描 `exam_session_sitting` 中 `status = 'IN_PROGRESS' AND started_at < NOW() - INTERVAL (total_duration_sec + 60) SECOND` 的场次，按第 1 条同一逻辑结束该场次。加 60 秒容忍是为了避开与请求线程的竞争。

**断点续答**：`GET /exams/{id}` 返回 `currentSittingNo`、各场次的 `status`/`remainingSec`、`currentSectionNo`、各分节的 `status`/`usedSec`，以及已保存的 `userAnswer`。前端刷新或换设备后据此恢复到正确场次 —— **若当前场次为 `PENDING`，前端应展示"开始本场次"按钮而不是直接倒计时**（场次之间不自动衔接）。

### 6.9.3 创建会话的组卷上限校验

创建前检查：

| 检查 | 失败返回 |
| --- | --- |
| 科目存在且启用 | `3001` |
| 存在 `ENABLED` 的卷面模板 | `4001` |
| 模板至少有一个 `supportStatus = 'SUPPORTED'` 且 `questionType` 为客观题的分节（否则无可判分内容） | `4002` |
| 各分节所需题量 ≤ 该科目对应题型的已发布题量，**否则按 6.6.2 Step C 的 L4 缩减并在响应中提示** | 不阻断 |
| 无 `CREATED`/`IN_PROGRESS` 的会话 | `4003` |

**快照写入（对应评审 G2）**：创建会话时，`exam_session_section` 的每一行都要**从 `exam_paper_section` 复制** `sitting_no`、`support_status`、`duration_min`、`duration_max`、`allow_early_submit`、`max_score` 等结构字段，**不允许通过 `exam_session.template_id` JOIN 回模板表取值**。理由：模板是可被管理员修正的（ADR-04 明确把"允许修正配置错误"列为模板驱动的收益之一），一旦修正就会让历史成绩的场次归属与"无法模拟"标记随之漂移，破坏快照的可复现性（04 中 `exam_session_item` 的快照注释同此目的）。

`NOT_SUPPORTED` 分节（信息处理技术员的「应用技术」）在创建会话时**生成对应的 `exam_session_section` 记录但不生成 item**，其 `status` 置为 `SKIPPED`，成绩页原样展示"本平台无法模拟"。

## 6.10 掌握度与统计计算

### 6.10.1 掌握度公式

对某个聚合维度（章节 / 知识点），设：

- `a` = 该维度下用户已作答的**去重题数**
- `n` = 该维度下已发布题量
- `c_i` = 第 `i` 道已作答题的最近一次作答结果（答对 1 / 答错 0）
- `Δd_i` = 距最近一次作答的天数
- `w_i = 0.5 ^ (Δd_i / H)`，`H` = 半衰期，取自 `sys_config` 的 `mastery.half.life`（默认 30 天）
- `prior` = 取自 `mastery.prior`（默认 0.5）

```
coverage = min(1, a / n)
recency  = Σ(w_i × c_i) / Σ(w_i)          // 加权正确率，随时间衰减
mastery  = coverage × recency + (1 - coverage) × prior
```

| 场景 | 结果 |
| --- | --- |
| 完全没做过（`a = 0`） | `mastery = prior = 0.5` |
| 做过 1 遍全错 | `coverage = 1`，`recency = 0` → `mastery = 0` |
| 做完且全对 | `mastery = 1` |
| 做了一半且全对 | `coverage = 0.5` → `mastery = 0.75` |

**科目掌握度** = `Σ(章节 mastery × 章节题量) / Σ(章节题量)`（按题量加权，避免题量极小的章节过度影响）。

> 决策与理由
> 用"覆盖率 + 时间衰减加权正确率"而不是简单的"正确率"。理由有三：① 简单正确率无法区分"做过 1 题答对"和"做过 50 题答对 40"，前者不该被判定为掌握；② 没有时间衰减，半年前做对的题会让掌握度虚高；③ 未做过的部分必须有先验值，否则新用户打开统计页全是 0 分，体验上是"我什么都没干就已经失败了"。三个问题分别由 `coverage`、`w_i`、`prior` 解决。
> **不使用遗忘曲线（SM-2 / Ebbinghaus）**：它属于"自适应推荐算法"，已被 01 文档列为范围外（OOS），且需要按题维护复习间隔状态，复杂度远超收益。

### 6.10.2 统计聚合策略（懒重算 + 夜间全量）

```
答题上报（事务内）：
  └─ 增量更新 stat_user_subject / stat_daily 的计数类字段（answered_count/correct_count/accuracy）
     （这些字段可以纯增量维护，无衰减，不需要重算）
  └─ 事务提交后：SET rk:stat:stale:{userId}:{subjectId} = 1（TTL 7 天）

读取统计接口时：
  └─ 若 stale 标记存在 → 重算该用户该科目的 mastery 与 stat_user_outline / stat_user_kp
     → 写回 → DEL stale

定时任务（每日 03:00）：
  └─ 全量重算活跃用户的 stat_user_outline / stat_user_kp / stat_user_subject.mastery
     （兜底：修正因异常中断导致的 stale 标记丢失）
```

重算 SQL（章节维度）：

```sql
-- 先用窗口函数取每题「最后一次作答」（去重口径的唯一实现），再在其上聚合
INSERT INTO stat_user_outline (user_id, subject_id, outline_id, answered_count, correct_count, accuracy, mastery)
SELECT t.user_id, t.subject_id, t.outline_id,
       COUNT(*)                                            AS answered_count,
       SUM(CASE WHEN t.is_correct = 1 THEN 1 ELSE 0 END)   AS correct_count,
       0, 0
FROM (
    SELECT r.user_id, r.subject_id, r.outline_id, r.is_correct,
           ROW_NUMBER() OVER (PARTITION BY r.question_id ORDER BY r.answered_at DESC, r.id DESC) AS rn
    FROM user_answer_record r
    WHERE r.user_id = ? AND r.subject_id = ? AND r.is_correct IS NOT NULL
) t
WHERE t.rn = 1
GROUP BY t.outline_id;
-- mastery 由 Java 侧按 6.10.1 公式计算（含时间衰减），不在 SQL 里算：
-- 时间衰减权重涉及逐行计算，用 MySQL 写会非常难读且难测；
-- 单用户单科目记录数在千级以内，取回内存计算更清晰，且这部分逻辑必须可单测。
```

`accuracy` 定义：**最近一次作答**口径（同一题重复作答只算最后一次），与移出错题本的"连续答对"逻辑保持同一语义。`COUNT(DISTINCT question_id)` 与未去重的 `SUM(is_correct)` 混用会让"一题先错后对"时的 `correct_count` / `accuracy` 系统性偏高，故必须走上面的窗口函数口径（对应评审 B5）。**该口径必须有单测覆盖（一题先错后对，`accuracy` 应为 0）。**

### 6.10.3 连续学习天数

```sql
-- 从今天往前找连续有记录的最大天数
SELECT stat_date FROM stat_daily
WHERE user_id = ? AND answered_count > 0
ORDER BY stat_date DESC LIMIT 400;
-- 由 Java 侧遍历：允许"今天还没学"（从昨天起算），中断即停止
```

不做首尾日期差、不引入日历表。

## 6.11 缓存设计

| Key | 类型 | TTL | 内容 | 失效时机 |
| --- | --- | --- | --- | --- |
| `rk:login:fail:{username}` | String | 900s | 登录失败次数 | 登录成功时 DEL |
| `rk:repeat:{userId}:{subjectId}` | Set | 30d | 防重复窗口内的题 ID | 无需失效，随作答增长 |
| `rk:cache:subject:list` | String(JSON) | 1h | 科目列表 | 管理端改动科目时 DEL |
| `rk:cache:outline:{subjectId}` | String(JSON) | 1h | 大纲树 | 管理端改动章节时 DEL |
| `rk:cache:paper:template:{subjectId}` | String(JSON) | 1h | 卷面模板 + 分节 | 管理端改动模板时 DEL |
| `rk:stat:stale:{userId}:{subjectId}` | String | 7d | 统计待重算标记 | 重算完成时 DEL |
| `rk:limit:feedback:{userId}:{questionId}` | String | 24h | 反馈提交次数 | 不主动失效 |
| `rk:limit:register:{ip}` | String | 3600s | 同 IP 注册次数（10 次/小时，防批量注册） | 不主动失效，靠 TTL |
| `rk:limit:upload:{userId}` | String | 3600s | 同管理员图片上传次数（60 次/小时，防刷量） | 不主动失效，靠 TTL |
| `rk:exam:creating:{userId}` | String | 3s | 创建会话短锁 | 自动过期 |

> 限流键的 TTL 与阈值与 [09-deployment.md](09-deployment.md) §9.13.6 保持一致（注册 10 次/小时、上传 60 次/小时，均 3600s）；它们此前只在 09 出现，未在本表登记，违反 §6.15 的检查项，现补入（对应评审 C7）。

**缓存读写封装**：`rk-repository` 的 `XxxCacheDao` 负责序列化与空值处理；Service 只调 DAO 的 `getOrLoad(key, loader)` 风格方法，不直接接触 `RedisTemplate`。

**缓存降级**：`rk:cache:*` 系列在 Redis 不可用时**直接回落查 MySQL**（try-catch 包裹缓存层，记 WARN 日志），不影响功能。会话类数据（Sa-Token）不可降级，Redis 不可用时登录直接失败并返回 `1000`，这是有意的设计选择——**宁可明确失败，也不要静默降级到一个不一致的鉴权状态**。

## 6.12 审核工作台的状态机实现

状态迁移集中在 `QuestionStateMachine`，用**显式迁移表**而非散落的 `if`：

```java
private static final Map<QuestionStatus, Map<ReviewAction, QuestionStatus>> TRANSITIONS = Map.of(
    DRAFT,          Map.of(SUBMIT, PENDING_REVIEW),
    PENDING_REVIEW, Map.of(APPROVE, APPROVED, REJECT, REJECTED),
    REJECTED,       Map.of(REGENERATE, DRAFT, SUBMIT, PENDING_REVIEW),
    APPROVED,       Map.of(PUBLISH, PUBLISHED, EDIT, DRAFT),
    PUBLISHED,      Map.of(OFFLINE, OFFLINE, EDIT, DRAFT),   // 编辑已发布题目需先转为 DRAFT
    OFFLINE,        Map.of(PUBLISH, PUBLISHED, EDIT, DRAFT)
);
```

每次迁移：① 校验非法迁移（返回 `3003`）；② 更新 `rk_question.status`；③ 写 `rk_question_review_log`（`action` 枚举取值以 [04-database.md](04-database.md) 的 DDL 注释为准，对应评审 D2）；④ **凡"进入或离开 `PUBLISHED`"的迁移都必须同步三处冗余计数**（`rk_subject`、`rk_outline_node`、`rk_knowledge_point` 的 `question_count`），触发迁移逐条为 —— **进入**：`APPROVED → PUBLISHED`、`OFFLINE → PUBLISHED`；**离开**：`PUBLISHED → OFFLINE`、`PUBLISHED → DRAFT`。为避免增量漏减，所有落到 `DRAFT` 的 EDIT 迁移（`APPROVED → DRAFT`、`OFFLINE → DRAFT`、`PUBLISHED → DRAFT`）**统一按"重算当前科目三处计数"处理**，这样计数不会偏高整整一个周期，也不会让 §6.13 每周一 04:00 的计数校验持续告警（对应评审 B1、B12）。

**批量裁决**（`/admin/questions/batch-review`）：逐条走同一状态机，**不做整体事务**（一条失败不影响其他），返回 `{success: n, failed: [{id, code, message}]}`。上限 100 条/次，防止长事务。

**退回重生成**：`REJECTED → DRAFT` 后，对应的 `rk_generation_batch.rejected_count` 已经 +1；管理员可在批次详情页发起新批次针对同一批考点重新生成。

## 6.13 定时任务

单实例部署，直接使用 Spring `@Scheduled`，**不引入分布式调度组件**。

| 任务 | 频率 | 内容 |
| --- | --- | --- |
| 超时会话结算 | 每 5 分钟 | 按场次扫描 `exam_session_sitting`，见 6.9.2 兜底 |
| **会话级滞留结算** | 每 30 分钟 | **对应评审 G1**。会话 `status = 'IN_PROGRESS'` 且当前场次为 `PENDING`，且**上一场次的 `finished_at` 早于 `NOW() - exam.session.stale.hours` 小时**（默认 6）→ 按 05 用例 23 已定的语义**自动交卷**：未作答的场次一并置 `FINISHED`，成绩页标 `answered = false`。**判据必须用 `finished_at` 而非 `updated_at`**——`GET /exams/{id}` 是读操作不写库，用活动时间会漏判。选"自动交卷"而非"自动放弃"：保留场次一已考出的成绩，且完全复用既有语义，不引入新状态 |
| 统计全量重算 | 每日 03:00 | 活跃用户（近 90 天有作答）的 `mastery` 与 `stat_user_*` |
| 冗余计数校验 | 每周一 04:00 | 对比 `rk_subject.question_count` 与实际 `PUBLISHED` 题量，不一致则以实际为准修正，并记 WARN 日志 |
| 过期数据清理 | 每日 04:30 | 清理 90 天前的 `ABANDONED` 会话及其 item（成绩统计不受影响）。**注意**：`ABANDONED` 只在用户主动点放弃时产生，自动路径不产生——因此本任务此前近乎空转，G1 落地后自动路径仍走 `SUBMITTED`，本任务的价值主要是清理手工放弃的会话 |
| 生成批次一致性巡检 | 每日 05:00 | 见 [08-ai-pipeline.md](08-ai-pipeline.md) §8.9.2 第 10 项：按 `generation_batch_id` 分组校验批次产出与审核计数的一致性，**只告警、不自动修正**，不一致即记 WARN 日志待人工介入（对应评审 B15） |

所有任务通过 `sys_config` 的 `task.enabled.*` 开关控制，便于排查时单独关闭；每个任务对应一个开关键（「生成批次一致性巡检」对 `task.enabled.batch.consistency`、「会话级滞留结算」对 `task.enabled.session.stale`，阈值对 `exam.session.stale.hours`，均在 04 §4.8.3 登记）。

> **为什么必须有「会话级滞留结算」（对应评审 G1）**
> A5 引入场次模型 + B9 收紧历史列表过滤，两者各自正确，叠加后产生一个缺陷：高级科目考生考完上午连考后不再回来（午休后忘记继续）时，场次一到期置 `FINISHED`、场次二停在 `PENDING`，**而会话仍是 `IN_PROGRESS` 且没有任何机制会再推进它**。后果是三重的：① 新建模拟考被 `4003` 拦住；② 该会话不在历史列表里（默认只返回 `SUBMITTED`/`SCORED`，且 `includeAbandoned` 也不含 `IN_PROGRESS`），用户没有 UI 入口回去处理；③ 场次一的成绩不判分（判分发生在交卷时），用户白考一场。
> 影响面**恰好是高级科目**——也就是 A5 要服务的那批用户；初级/中级只有一个场次，`remainingSec == 0` 时直接触发整卷交卷，不受影响。

## 6.14 代码规范与测试要求

### 规范

| 项 | 规则 |
| --- | --- |
| 类长度 | Service 实现类超过 400 行时拆分（按职责拆，不按 CRUD 拆） |
| 方法长度 | 超过 60 行必须拆分 |
| 注释 | 只在"为什么这么做"上写注释，不写"做了什么"；所有含降级/兜底/绕过逻辑的代码**必须**有注释说明触发条件 |
| 日志 | 入口 INFO（含关键参数 ID）、异常 ERROR（含堆栈）、循环内禁止打日志；**禁止打印密码、token、用户答案原文** |
| SQL | 禁止在 XML 中拼 `${}` 变量（防注入）；列表查询必须带 `LIMIT` |
| 常量 | 枚举值、错误码、缓存 Key 前缀集中定义，禁止散落字符串字面量 |

### 测试要求（必须覆盖，其余按需）

| 用例 | 类型 | 要点 |
| --- | --- | --- |
| 组卷章节配额分配 | 纯单测 | 最大余数法；`count < 章节数` 与 `count > 总题量` 两个边界 |
| 组卷四级降级 | 纯单测 | 逐级触发，断言 `repeatedCount`、`actualCount`、`uncoveredOutlineIds` |
| 判分 | 纯单测 | 单选、多选（漏选/多选/顺序不同）、判断、主观题（`isCorrect` 为 `null`） |
| 掌握度公式 | 纯单测 | 6.10.1 表格中的四种场景逐一断言（含浮点容差） |
| 计时与超时 | 纯单测（注入 `Clock`） | `remainingSec` 计算、跨节条件、论文节不可提前结束 |
| 状态机迁移 | 纯单测 | `TRANSITIONS` 表中每条合法边 + 至少 3 条非法边 |
| 鉴权注解覆盖 | 单测 | 6.5 的反射扫描测试 |
| 生成输入护栏 | 单测 | Prompt 断言命中真题特征词时抛异常 |
| 答题上报幂等 | `@SpringBootTest` | 5 秒内重复上报只生效一次 |

**测试基础设施约定**：纯逻辑单测**不依赖容器**（不启 Spring、不连 MySQL），通过构造器注入 `Clock` 与 Mapper 接口的 mock 实现；仅上表最后一行的集成测试需本机 MySQL + Redis。**不引入 Testcontainers**：单机环境下它带来的启动开销与镜像拉取负担超过收益（若将来上 CI 频繁跑集成测试再评估）。

> 决策与理由
> 覆盖率不设硬指标（01 文档已声明）。理由：为了达到某个百分比而给 getter/setter、Converter 写测试，只会增加维护成本。**上表列出的用例是"算法核心 + 安全边界"，它们必须全绿；其余按需。**

## 6.15 后端开发检查单

- [ ] 新增的枚举值与 `04-database.md`、`02-domain-model.md` 一致，且 `rk-common` 中有对应 Java 枚举
- [ ] 新增接口已登记到 `05-api.md`，错误码已在 `ErrorCode` 与文档两处同步
- [ ] 新增 Controller 方法已标注鉴权注解（`AuthWhitelist` 例外需说明理由）
- [ ] 涉及用户数据的查询都带 `user_id` 条件（约束 AC-3）
- [ ] 高危操作已写 `sys_operation_log`（约束 AC-2）
- [ ] 新增缓存 Key 已在 6.11 登记，并在写操作处补上失效逻辑
- [ ] 涉及状态流转的改动已更新 `QuestionStateMachine.TRANSITIONS` 或 `exam_session` 流转校验
- [ ] 新增降级/兜底分支已有注释说明触发条件与预期行为