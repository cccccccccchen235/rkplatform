# 15 · P1 实现与设计文档的出入记录

> 本文档是文档链的**第三类记录**。[11](11-review-findings.md) 第一轮体检 → [12](12-changes-applied.md) 第一轮手术 → [13](13-review-round2.md) 第二轮体检 → [14](14-changes-round2.md) 第二轮手术 → **本文档（实现期出入）**。
>
> 分工不同：11/13 记的是**文档内部**的问题，12/14 记的是**文档怎么改**；本文档记的是**代码写出来之后，实现与文档对不上的地方**。四类成因分列：
> ① **文档写错**——照它实现会踩坑（D1 / D2 / D11）；
> ② **文档没写**——实现必须自行决定（§5 补记表）；
> ③ **文档与实现打架**——数字或表述二选一，需裁决（D4 / D9 / D10）；
> ④ **实现偏离文档字面**——但文档本身可用，属分层偏好，**没有缺陷**（D7，已按实测改写，见该条）。
>
> **冲突裁决顺序：`15` > `14` > `13` > `12` > `11`**，但**仅限 §2 逐条列出的条目**；未列及之处沿用 14 的顺序。
>
> 依据 [10](10-roadmap.md) §10.12「文档与代码不一致时，先改文档再改代码」：本文档先把出入摆出来，**改文档还是改代码由每条的「处置」列决定**，未决项集中在 §6。

---

## 1. 范围与结论

**范围**：P1 后端骨架。`server/` 六个 Maven 模块、`GET /api/v1/subjects` 垂直切片、AC-1 环境变量启动校验、`--rk.password-gen` 哈希生成器，以及 [deploy/sql/](../deploy/sql/) 三个 SQL 的**真实执行**验证（在一次性空库上跑，方法与结果见 §7）。

**不含**：前端 `web/`、`deploy/docker-compose*.yml`、`deploy/.env.example`、`scripts/`、CI —— 这些尚未交付，列入 §6 遗留，不算"出入"。

**结论**：共 **11 条出入**，处置分布如下。

| 处置 | 条数 | 编号 |
| --- | --- | --- |
| 文档待回填（**文档有错或不完整**，照抄会踩坑；实现是对的） | 6 | D1 D2 D3 D5 D6 D11 |
| 待裁决 | 4 | D4 D9 D10 D7 |
| 本轮已修（不等他人） | 1 | D8 |

**11 条里没有一条是"实现有缺陷、必须改代码"**：

- **8 条只需改文档** —— D1 D2 D3 D5 D6 D11（实现在这 6 条上是对的），以及 D9 D10（README 的措辞，等你定）；
- **1 条本轮已补** —— D8（`.gitignore` 少 3 行）；
- **2 条两条路都走得通、需选一条** —— D4（连接池数字取 10 还是 20）与 D7（哈希生成器的落点），实现与文档各自都自洽，所以单列为待裁决而不是缺陷。

---

## 2. 出入清单

| 编号 | 严重度 | 性质 | 文档处 | 实现处（`server/` 内） | 处置 |
| --- | --- | --- | --- | --- | --- |
| **D1** | 高 | 文档写错：白名单漏基路径 | [05](05-api.md) §5.13 行 761 | `rk-api/…/config/AuthWhitelist.java` | 文档待回填 |
| **D2** | 高 | 文档写错：关开关 ≠ 页面 404 | [09](09-deployment.md) §9.9 行 1037、§9.10 行 1090 | `rk-api/…/config/DocAccessGuardConfig.java` | 文档待回填 |
| **D3** | 中 | 实现新增，文档未列 | [06](06-backend.md) §6.5 行 99~107 | `rk-boot/…/application.yml` | 文档待回填 |
| **D4** | 中 | 数字冲突：连接池上限 | [09](09-deployment.md) §9.12.2 行 1274、§9.12.3 行 1289 | `rk-boot/…/application.yml` | **待裁决** |
| **D5** | 中 | 文档缺作用域限定 | [05](05-api.md) §5.1.1 行 31 | `rk-api/…/handler/GlobalExceptionHandler.java` | 文档待回填 |
| **D6** | 低 | 文档未言明字段是否下发 | [05](05-api.md) §5.2 行 86、§5.4 行 175 | `rk-domain/…/vo/SubjectVO.java` | 文档待回填 |
| **D7** | 低 | 实现未照文档字面落点（**文档可用**，属分层偏好） | [09](09-deployment.md) §9.9 行 1062 | `rk-api/…/cli/PasswordGenCli.java` + `rk-common/…/security/PasswordHasher.java` | **待裁决**（推荐保留 + 回填文档） |
| **D8** | 高 | 产物缺项：`.gitignore` 少 3 行 | [09](09-deployment.md) §9.4.2 行 460 | 仓库根 `.gitignore` | **本轮已修** |
| **D9** | 中 | 表述已不实 | [README](../README.md) 行 6 | —（无实现对应物） | **待裁决** |
| **D10** | 中 | 快速开始照做会失败 | [README](../README.md) §5 行 105~106 | —（同上） | 待修，顺序见 §6 |
| **D11** | 中 | 文档示例与文档自身的注释矛盾 | [04](04-database.md) §4.8.3 行 720 | [deploy/sql/init-data.sql](../deploy/sql/init-data.sql) | 文档待回填 |

---

## 3. 逐条说明（只展开需要展开的；D3 / D5 / D6 的清单行已足够清楚，不重复）

### D1 · 05 §5.13 的白名单漏了 `GET /api/v1/subjects`（高）

**文档怎么写**：§5.13 的白名单只列了 `GET /api/v1/subjects/**`（行 761）。

**为什么是错的**：`/**` 在 Ant 语义下**只匹配子路径，不匹配基路径本身** —— `/api/v1/subjects/**` 匹配 `/api/v1/subjects/17`，不匹配 `/api/v1/subjects`。而科目列表接口**恰恰就是基路径**（§5.4 行 174）。照文档逐字实现，P1 这条唯一的垂直切片会在鉴权层被拦下。

**实现怎么做**：`AuthWhitelist.RULES` 里额外补了一条 `GET /api/v1/subjects`，并在类注释里写明了补它的原因。原有两条 `/{id}/outline`、`/{id}/knowledge-points` 已被 `/**` 覆盖，**保留是为了与 §5.13 逐条对照**，评审时无需来回推导。

**建议**：§5.13 的代码块补一行 `GET /api/v1/subjects`，并加一句 Ant 语义说明。否则下一个人照文档实现会踩同一个坑，而症状（列表页 401）与白名单这个位置隔得很远，不好排查。

### D2 · 「关掉文档开关」不等于 `/doc.html` 返回 404（高）

**文档怎么写**：09 §9.3 行 416 要求 `RK_DOC_ENABLED` 生产必须 `false`；§9.9 行 1037 的上线步骤 `curl -sSI https://exam.example.com/doc.html | head -1  # 期望 404`；§9.10 行 1090 还要在容器内 `curl -sI http://127.0.0.1:8080/doc.html` 复核一次。

**实测**：`knife4j.enable=false` + `springdoc.api-docs.enabled=false` **只关掉了文档的数据接口**（`/v3/api-docs` 变成空），而打包在 jar 里的静态页 `META-INF/resources/doc.html` 与 `/webjars/**` 由 Spring 的资源处理器照常返回 —— **HTTP 200，1903 字节的完整 Knife4j 页面**。

**为什么这是真缺陷而不是洁癖**：09 摆了**两道**防线，只有第一道起作用。

| 防线 | 位置 | 关闭开关后 | 说明 |
| --- | --- | --- | --- |
| 外网 | §9.2 行 545 的 nginx `location ~ ^/(doc\.html\|webjars/\|…) { return 404; }` | ✅ 404 | nginx 层挡住，与后端行为无关（**按配置推断：本机无 nginx，未实测**） |
| 容器内直连 | §9.10 行 1090 `docker compose exec server sh -c "curl -sI …:8080/doc.html"` | ❌ **200** | 绕过 nginx，直接打应用；**已实测** |

即：**§9.10 行 1090 是一条永远过不了的上线核查项**。它挡的正是"同网络的其他容器"和"端口被误映射"这两类场景，而这两类恰恰是纵深防御存在的理由。

**实现怎么做**：新增 `DocAccessGuardConfig`（`rk-api/…/config/`），条件装配在 `springdoc.api-docs.enabled=false` 时注册一个拦截器，对 `/doc.html`、`/doc.html/**`、`/webjars/**`、`/swagger-ui/**`、`/swagger-ui.html` 直接返回**真实 HTTP 404**（不是统一响应体——这不是业务应答，见 D5）。文档开着时这个类根本不加载，两向都已实测（§7）。

**建议**：06 §6.5 或 09 §9.10 补记这个类。否则下一个人看到"文档开关已关"就以为够了，而这条核查项会在上线当天以"检查不通过"的形式暴露，那时已经来不及查。

### D4 · 连接池上限：实现 10，文档 20（中 · 待裁决）

**文档怎么写**：09 §9.12.2 行 1274 的监控阈值写「`Threads_connected` > 200（上限 300）」并在括注里写「Hikari 池上限 20」；§9.12.3 行 1289 的排障动作写「把 Hikari 池调回 20」。

**实现怎么做**：`application.yml` 的 `hikari.maximum-pool-size: 10`，注释理由是单实例够用。

**问题**：**06 里没有任何连接池规格** —— 全文检索 `maximum-pool-size`、`Hikari` 在 [06](06-backend.md) 中零命中。也就是说 `20` 这个数字在文档里**没有权威出处，只存在于 09 的两处运维表格**里，而实现另写了一个 `10`。这正是 [14](14-changes-round2.md) §G9 要消灭的形态——「同一个数字被两处各写一遍，然后两边各自演进」——只不过这次是文档与代码各写一遍。

**待裁决**：两个方向都成立，请择一。

| 方案 | 动作 | 代价 |
| --- | --- | --- |
| **a（推荐）** | 06 §6.5 定为唯一定义处并取 **20**，实现改 20，09 的两处改为引用 06 | 改 1 行代码 + 1 处文档；运维按 20 核对时不会看到"实现与手册不符" |
| b | 06 §6.5 取值 **10**，09 两处改 10 | 改 2 处文档 |

推荐 a 的理由仅有一条：09 是**运维唯一事实来源**（其开篇行 4 明写「本文档的……是运维唯一事实来源；与其他文档冲突时以本文件为准」），按它写的数字去核对是运维的正常动作。但注意这条规则只解决文档之间，不解决文档与实现之间——所以无论选哪个，06 都必须补上这个规格。

### D7 · 哈希生成器的落点：实现没照文档写（低 · 分层偏好，不是缺陷）

**文档怎么写**：§9.9 行 1062 的决策写着「**用后端自带的实现生成、同一实现校验**……这也是把生成器**放进 `rk-boot`** 而非外部脚本的原因」。

**先说结论：文档的字面写法是可用的，这里没有缺陷。** 两条我核对过的事实：

1. 把 `cn.hutool.crypto.digest.BCrypt` 直接写进 `RkBootApplication.main` **能编译**——`rk-boot` 虽然没直接声明 Hutool，但依赖链 `rk-boot → rk-api → rk-service → rk-repository → rk-domain → rk-common` 全是 compile 作用域，Hutool 以 compile 作用域传递到 rk-boot 上，实测 `dependency:tree` 可见 `cn.hutool:hutool-all:jar:5.8.47:compile`（§7 有记录）。
2. 只要生成侧与校验侧都调 Hutool 的 BCrypt，文档担心的 `$2y$` / `$2a$` 前缀问题就不会出现——那问题是"用 `htpasswd` 等**外部工具**生成"引起的，而文档要的正是不要那样做。

**那实现为什么不照字面写**：纯粹是分层偏好，两条理由。

| 理由 | 说明 |
| --- | --- |
| 依赖卫生 | 用传递依赖等于使用**未声明的直接依赖**（`mvn dependency:analyze` 会报 used-undeclared），且链条上任何一环不再暴露 Hutool 时（rk-common 换实现、改 `provided` 等），rk-boot 会因为与自己无关的原因坏掉 |
| 单一实现集中 | `strength`、算法、哈希格式将来若要调整，只有 `PasswordHasher` 一处要改；生成器与校验器分别在两个模块各写一遍 BCrypt 调用，则是两个将来可能各自漂移的落点 |

**代价**：偏离了文档的字面落点，需要文档回填（否则下一个人读 09 §9.9 会去找一个不存在的 rk-boot 内实现）。

**若你更偏好忠于文档字面**：把 `PasswordGenCli` 的逻辑内联回 `RkBootApplication.main`、`PasswordHasher` 留在 rk-common 供两侧共用即可，改动很小；代价是接受一个未声明的直接依赖。**这条可以保留现状，也可以改回去，取决于你看重哪一边。**

**实现怎么做**：把哈希实现下沉到**生成与校验都能看到的最低层**。

| 构件 | 位置 | 职责 |
| --- | --- | --- |
| `PasswordHasher` | `rk-common/…/security/` | BCrypt strength=10，`hash()` 与 `matches()` **同一份实现** |
| `PasswordGenCli` | `rk-api/…/cli/` | 解析 `--rk.password-gen='<明文>'`，打印哈希后返回 true |
| `RkBootApplication.main` | `rk-boot/…/` | 首行 `if (PasswordGenCli.tryRun(args)) return;`，**不启动 Spring** |

对外行为与文档完全一致（§7 有实测），**只是"放进 rk-boot"这半句要改成"由 rk-boot 调用"**。文档保留其原有的理由（同一实现生成与校验），把落点指到 `PasswordGenCli` 即可。

### D11 · 04 §4.8.3 的示例 INSERT 缺 `must_change_password`（中）

**文档自相矛盾**：[04](04-database.md) 行 66 的列注释写着 `NOT NULL DEFAULT 0`、「管理员重置密码后置 1」；[14](14-changes-round2.md) §5 的遗留 L2 写着「管理员初始记录须以 `must_change_password = 1` 插入」。但 §4.8.3 行 720 的**示例 INSERT 只给了 5 列**：

```sql
VALUES ('admin', '<BCrypt占位，部署时由脚本生成>', '管理员', 'ADMIN', 'ACTIVE');   -- 没有 must_change_password
```

因为列默认值是 `0`，**照这个示例执行得到的恰恰是 `0`**，与同一份文档的 L2 要求相反。

**实现怎么做**：[deploy/sql/init-data.sql](../deploy/sql/init-data.sql) 补上了第 6 列并显式写 `1`（空库验证通过，见 §7）。占位串文案也与 04 行 720 不同（SQL 里是 `<BCrypt占位，待 rk-boot 的哈希生成器产出>`）——同一个占位串两处各写一遍，是 G9 的同一形态，建议以 SQL 为准回填 04。

**这条还解释了本机库的一个异常现象**：活库 `ruankao` 的 admin 行是 `must_change_password = 0`。成因是它由**更早版本**的 `init-data.sql`（照 04 行 720 的 5 列示例）插入，而现行的 upsert 分支只写 `username`、**有意不覆盖该列**（否则管理员改密后每重跑一次初始化又会被强制改密）。所以**重跑 `init-data.sql` 永远修不回 1** —— 这是设计使然，不是缺陷；但它意味着**在本机这个已有 admin 行的库上无法验证 L2**，L2 只能在空库上验证（§7 的做法）。

---

## 4. 本轮已闭环的动作

| 编号 | 动作 | 依据 |
| --- | --- | --- |
| **D8** | `.gitignore` 补 3 行：`deploy/data/`、`backup/`、`*.hprof` | 09 §9.4.2 行 460 的"必须包含"清单，修前 7 项只有 4 项命中 |
| **D2** | 新增 `DocAccessGuardConfig`，并双向实测 | 09 §9.9 行 1037 / §9.10 行 1090 |

D8 值得单独说一句：`backup/` 与 `deploy/data/` 会装数据库转储与上传文件，`*.hprof` 是**堆转储——里面可能有内存中的明文口令与会话令牌**。09 §9.4.2 的决策说的是「必须在第一次 `git add` 之前就写好这条规则」，写这份文档时仓库**仍是 0 次提交**（§7 已核），所以这次补齐没有造成任何泄漏，但窗口期已经用掉了一次。

---

## 5. 文档未覆盖、实现自行决定的点（补记，非出入）

这些地方文档**没有规定**，不算"出入"，但实现做了选择，记在这里以免后人以为是随手写的。

| 项 | 文档状况 | 实现的选择 | 理由 |
| --- | --- | --- | --- |
| Mapper 扫描位置 | 03 §3.3.1 只写「rk-repository 放 mapper（MyBatis-Plus）、XML」 | 在 `rk-repository` 内加 `MapperScanConfig` | `@MapperScan` 若加在启动类上，等于用**字符串**把 rk-boot 绑回持久层，绕过了 enforcer 的分层检查；而 MyBatis-Plus 的自动扫描只覆盖主类所在包（`com.ruankao.boot`），扫不到 mapper |
| JDBC 驱动声明 | 03 §3.3.1 的层表未提驱动 | `com.mysql:mysql-connector-j`（runtime）声明在 `rk-repository` | 驱动是持久层实现细节；版本由 Boot 统一管理 |
| AC-1 校验的机制 | 09 §9.4/§9.12.3 只要求"必填变量为空则启动失败" | `EnvironmentPostProcessor`，**一次列全所有缺失键**再抛异常 | 报第一个就停，会让运维"补一个、再启动、再报下一个"来回折腾 |
| BCrypt 的实现选型 | 09 行 1062 写的是「Hutool / Spring Security 的 BCrypt」 | 取 Hutool | rk-common 是全项目最底层，引 spring-security-crypto 会连带 Spring 系依赖与日志桥接，为一个纯函数不值得 |
| 未匹配路径的行为 | 05 未规定 | 兜底成 `code 1003`（HTTP 200） | 不兜的话会落进"系统内部错误 1000"并记 ERROR 堆栈——用户打错一个字母就制造一条"系统内部错误"，真故障会被淹没 |
| 关机与 Tomcat 日志 | 09 未规定 | `shutdown: graceful` + `timeout-per-shutdown-phase: 30s`；APR 的两条 ERROR 定点静音 | 前者配合 09 的容器停止流程；后者：Tomcat Native 版本低于内嵌 Tomcat 要求，会自动退回 NIO 连接器，但"每次启动都有、且可以忽略"的 ERROR 会让运维学会忽略 ERROR |

---

## 6. 遗留与待裁决

### 待裁决

| # | 事项 | 选项 | 推荐 |
| --- | --- | --- | --- |
| **D4** | Hikari 池上限 10 / 20 | a：实现改 20 并在 06 立规格（09 改引用）<br>b：09 两处改 10 | **a** |
| **D7** | 生成器落点：保留现状 / 回退到文档字面 | a：保留（`PasswordGenCli` 在 rk-api + `PasswordHasher` 在 rk-common），回填 09 §9.9 的落点<br>b：内联回 `RkBootApplication.main`，接受一个未声明的直接依赖 | **a** |
| **D9** | README 行 6「当前状态：设计阶段……**代码尚未开始编写**」已不实 | a：改为「设计完成；P1 后端骨架已落地（六个模块 + `GET /api/v1/subjects` 垂直切片），前端未开始」<br>b：等你认为可以对外这么写时再改 | **a**（这行是仓库最显眼的一句话，也是唯一会让访客判断错状态的） |
| **D10** | README §5 快速开始按它做会失败 | 见下 | 与前端同批修 |

D10 的具体三处：

1. 行 106 `cd server && mvn -pl rk-boot -am spring-boot:run` —— **必然失败于 AC-1 校验**：10 个必填环境变量无默认值，README 既没提 `deploy/.env` 也没给单次注入的写法。
2. 行 105「Knife4j 文档在 /doc.html」—— `RK_DOC_ENABLED` 默认 `false`，实测 **404**；要看文档须显式 `RK_DOC_ENABLED=true`。
3. 全节未提 `--rk.password-gen` + 一次性 `UPDATE` 这一步 —— 照 README 走完，admin 仍是占位哈希，**登录必然失败**。

建议与前端开工同批改，因为届时行 108 的 `cd web && pnpm dev` 也才真正可执行。

### 遗留（承接 14 §5，更新后）

| # | 遗留 | 状态 |
| --- | --- | --- |
| L1 | 20 个科目的完整 INSERT | **已闭环**（空库实测 20 科目 / 20 模板 / 25 场次 / 45 分节，§7） |
| L2 | 管理员初始记录以 `must_change_password = 1` 插入 | **已闭环**（空库实测 `must_change = 1`，§7）；本机旧库仍是 0，成因见 D11 |
| L3 | `init-data.sql` 必须填全 4 个时长列 | **已闭环**（空库实测"未填时长的分节 = 0"，§7） |
| L4 | 3 个 FULL 科目的章节树 SQL | P2 |
| L5 | 软件设计师首批 250 题 | P3 |
| L6 | 滞留结算的判据用跨午休场景验证 | P5 |
| L7 | 「表数量 CI 断言」需在 P1 建 CI 时实现 | **未开始**（CI 未建，见下） |
| **L8** | `deploy/.env.example`、`deploy/docker-compose.yml`、`docker-compose.infra.yml`、`nginx/conf.d/default.conf`、`scripts/`、`.github/workflows/` | **未交付**（`deploy/` 目前只有 `sql/`；09 §9.4.2 的 `.gitignore` 否定项 `!deploy/.env.example` 指向的文件还不存在） |
| **L9** | 前端 `web/` 未开始 | **未开始**（按 14 与既定约定，动 UI 前先产出 PRODUCT.md） |

---

## 7. 自查（每项均由检索或实跑得出，不按印象勾选）

**构建与测试**

| 核对项 | 期望 | 结果 |
| --- | --- | --- |
| `mvn clean package` | BUILD SUCCESS | ✅ 六模块全 SUCCESS |
| 测试总数 | 6（rk-common 3 + rk-api 3） | ✅ `Tests run: 3` × 2 组，`Failures: 0` |
| 分层约束真的会拦人 | 临时给 rk-api 加 `com.ruankao:rk-repository` 后构建失败 | ✅ `Rule 0: BannedDependencies failed`（**已回滚**） |
| AC-1：不提供环境变量 | 启动失败且**一次列全** 10 个缺失键 | ✅ 退出码 1，Tomcat 未启动 |
| `--rk.password-gen='ChangeMe@2026'` | `$2a$10$` 前缀、**60 字符**（对应 09 §9.9 第 7 步 `hash_len` 必须 60） | ✅ 60 字符，零 stderr，未启 Spring、不需要环境变量 |
| 密码哈希契约 | `$2a$` 前缀 / 60 字符 / 占位串比对返回 false 不抛异常 | ✅ `PasswordHasherTest` 3/3 |
| D7 的落点论断 | Hutool 是否传递到 rk-boot 的 compile classpath | ✅ **是**（`dependency:tree` 显示 `cn.hutool:hutool-all:5.8.47:compile`）——所以"内联进 rk-boot 编译不过"是**错的**，D7 已按实测改写 |

**接口（应用真跑，port 18080 / mgmt 8081）**

| 核对项 | 期望 | 结果 |
| --- | --- | --- |
| `GET /api/v1/subjects` 顶层字段 | **恰好 4 个**（05 §5.1.1） | ✅ code / message / data / traceId |
| 科目数与分组 | 20，且 JUNIOR / MIDDLE / SENIOR 三键**恒在** | ✅ 4 / 11 / 5 = 20；`level=NOPE` 时三组空数组仍在 |
| `id` 下发型别 | 字符串（BIGINT UNSIGNED） | ✅ `"id":"17"` |
| `?level=junior` 大小写 | 归一化后与 `JUNIOR` 同结果 | ✅ 4 条 |
| `paperSummary` 在列表接口 | 恒 null（见 D6） | ✅ 20 处 null |
| 未登录 `GET /api/v1/users/me` | HTTP 401 + `2005` | ✅ |
| 未匹配路径 `/nope` | HTTP 200 + `1003`，且**不记 ERROR** | ✅ 修复前是 1000 + ERROR 堆栈 |
| `/doc.html` · `RK_DOC_ENABLED=false` | HTTP 404 | ✅ 1903 字节的页面被拦下 |
| `/doc.html` + `/v3/api-docs` · `=true` | 200 且**返回真实文档** | ✅ 200 / 200，OpenAPI JSON 3269 字节（证明守卫是条件装配，没打死开发期的文档） |
| 启动日志 ERROR 条数 | 0 | ✅ APR 静音后为 0 |

**数据库（在一次性空库 `rk_l2check` 上跑，跑完即 DROP —— 未触碰 `ruankao` 与既有库）**

| 核对项 | 期望 | 结果 |
| --- | --- | --- |
| `schema.sql` 建表 | 27 张（04 §4.8.1 / 03 / 09 / 10 一致） | ✅ 27 |
| 字符集 | `utf8mb4` / `utf8mb4_0900_ai_ci` | ✅ 与 09 §9.9 第 6 步的断言一致 |
| 初始化数据 | 20 科目 / 20 模板 | ✅ 20 / 20（场次 25、分节 45） |
| **L2**：管理员 `must_change_password` | 1 | ✅ 1（**空库**插入；本机旧库为 0，成因见 D11） |
| **L3**：时长列填全 | 未填的分节数 = 0 | ✅ 0（4 个 `NOT NULL` 列全填，建库未报错） |
| 幂等：重跑 `init-data.sql` | 不报错、行数不变 | ✅ 科目 20 / 分节 45，且 `must_change` 未被改回 |
| 清理 | 空库已删除，既有库原样 | ✅ `SHOW DATABASES` 仅剩 `library_db` / `mydb` / `testdb` / `ruankao` 等原库 |
| 活库 `ruankao` | 27 表 / 20 科目 / 20 模板 / 15 配置键 | ✅ 与 04 §4.8 一致 |

**文档链与仓库**

| 核对项 | 期望 | 结果 |
| --- | --- | --- |
| README 索引列出 `docs/15` | 存在，链接可达 | ✅（本次一并补入） |
| `15` → `11`~`14` 的引用 | 全部可达、无坏链 | ✅ 文中 13 条链接全部命中真实文件 |
| `11`~`14` → `15` 的反向引用 | **不引用**（预期如此） | ✅ 四篇均为留痕文档，按既定约定不修改 |
| `.gitignore` 覆盖 09 §9.4.2 的 7 项 | 7/7 | 修前 4/7 → ✅ 修后 7/7 |
| `.env` 从未进入 Git 历史 | 0 处 | ✅ `git log --all --name-only` 无匹配；**提交数仍为 0** |

---

## 8. 文档链现状

| 序号 | 文件 | 角色 | 状态 |
| --- | --- | --- | --- |
| 11 | [11-review-findings.md](11-review-findings.md) | 第一轮体检（38 条 + 6 项裁决） | 留痕，不改 |
| 12 | [12-changes-applied.md](12-changes-applied.md) | 第一轮手术记录 | 已含 G7 口径更正 |
| 13 | [13-review-round2.md](13-review-round2.md) | 第二轮体检（44 条复跑 + G1~G9） | 留痕，不改 |
| 14 | [14-changes-round2.md](14-changes-round2.md) | 第二轮手术记录 | G1~G9 全部落地 |
| **15** | **本文件** | **P1 实现与文档的出入记录** | **11 条：6 待回填 / 4 待裁决 / 1 已修** |

**冲突裁决顺序：`15` > `14` > `13` > `12` > `11`，仅限本文档 §2 列出的条目。**

后续轮次沿用"体检 / 手术"分离的既定约定；**实现期的出入另立本类文档**，每完成一个阶段（P2、P3……）各出一篇，不修改前一篇。
