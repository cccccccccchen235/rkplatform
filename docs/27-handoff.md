# 27 · 交接文档

> **这份文档是给"接手的人"看的，不是又一份轮次记录。**
>
> `docs/01`~`26` 是**项目自己的设计史与轮次留痕**（按项目的规矩：旧篇永不回改、冲突时新篇优先）。
> 本篇的职责不同：它回答"**现在这个仓库处于什么状态、怎么把它跑起来、下一步该做什么、哪里有坑**"。
> 它**不定义任何契约**——契约的唯一出处仍是 `04`（DDL）与 `05`（接口），本篇只做索引。
>
> 维护约定：本篇描述的是"当前现场"，因此**它可以被更新**（与 `01`~`26` 的"不回改"不同）。
> 换了机器、改了端口、修完了某条欠账之后，请回来改这里，而不是把它当成历史存档。

---

## 0. 30 秒版

| 问题 | 答案 |
| --- | --- |
| 这是什么 | 软考在线学习平台（非商业、无付费）。monorepo：`server/` 后端 + `web/` 前端 + `deploy/` 部署 + `scripts/` 工具 + `docs/` 文档 |
| 做到哪了 | **P0~P4 完成** + **P5 模拟机考完成**（`30` + `31` + `32`）+ **P6 学习统计**（`28`/`29`）+ **考生端个人中心 M1**（`33`，补完 07 路由表最后一个考生端页面）+ **站点图标与管理端入口**（`34`）。P7 部署、P8 收尾未开始 |
| 现在能跑吗 | **能**。后端 8080、nginx 80 托管前端（5173 已停用），题库有 250 道**已发布**的单选题（科目 SD）。✅ **本轮验证时服务是起着的**（后端 8080/8081 + nginx 80；停止/重启见 §3） |
| 最大的风险 | **仓库 0 个提交**——整个项目只是工作区里的未跟踪文件（见 §2.4） |
| 先读哪几篇 | 想动手 → 本篇；想改接口 → `05` + `04`；想知道阶段与验收 → `10`；想知道最近一轮 → `34` |

---

## 1. 文档地图（谁是权威）

**冲突裁决顺序：`34` > `33` > `32` > `31` > `30` > `29` > `28` > `26` > `25` > `24` > `23` > `22`；跨阶段沿用 `17` > `16` > `15` > `14` > `13` > `12` > `11`。**
（`27` 是本篇"当前现场"，只做索引、不定契约，故不进裁决链。）
（`01`~`26` 一律不回改；漏掉的事情由新篇登记，而不是回头改旧篇。）

| 文档 | 它**唯一**负责的事 |
| --- | --- |
| [04-database.md](04-database.md) | **DDL 唯一来源**（27 张表、初始化数据、约束） |
| [05-api.md](05-api.md) | **接口契约唯一来源**（路径、字段名、错误码表 §5.1.2、白名单 §5.13） |
| [06-backend.md](06-backend.md) | 后端分层、组卷算法 §6.6、判分 §6.7、错题本 §6.8、统计口径 §6.10、缓存 §6.11 |
| [07-frontend.md](07-frontend.md) | 前端路由表、store、组件清单、页面专项说明 |
| [10-roadmap.md](10-roadmap.md) | **阶段划分与验收标准**（P0~P8；每阶段的验收勾选项） |
| [02-domain-model.md](02-domain-model.md) | 领域模型、枚举、**版权策略 GR-1~GR-3** |
| [09-deployment.md](09-deployment.md) | 容器化部署设计（**尚未落地**） |
| [26-p4-practice-loop.md](26-p4-practice-loop.md) | 最近一轮：P4 练习闭环 + 首页去噪 + **对 05/06 的 9 条偏离登记** |
| [33-profile-page.md](33-profile-page.md) | 上一轮：考生端个人中心 `/profile`（M1）+ 05 §5.3 第 5~7 条三个接口 + **对 05/06/07 的 7 条偏离登记** |
| [34-icon-and-admin-entry.md](34-icon-and-admin-entry.md) | 最近一轮：书脊图标（favicon + 顶栏品牌图标）+ 管理员导航项「管理端」；纯前端增量，不改 04/05 契约 |
| [32-p5-paper-template-admin.md](32-p5-paper-template-admin.md) | P5 管理端卷面模板配置（05 §5.10 第 51/52 条）+ 服务端五条校验 + **对 05/07 的 7 条偏离登记** |
| [31-p5-exam-frontend.md](31-p5-exam-frontend.md) | P5 考生端三页（模拟考中心 / 答题页 / 成绩详情）+ **对 05/07 的 4 条偏离登记** |
| [25-progress-import-and-fixes.md](25-progress-import-and-fixes.md) | 题库导入、服务启动、`StpInterface` 修复、客户端异常家族修复 |

**另有两个"活文档"，它们不在 `docs/` 链里、且需要随实现更新**：

- `DESIGN.md`（仓库根）—— 视觉世界的权威（impeccable skill 的产物）；`PRODUCT.md` 同理。
- `.impeccable/` —— impeccable skill 的 surface brief、评审截图、设计载荷。
  > 与 `docs/` 的区别：`docs/` 是**设计史**（不回改），这两个是**当前状态的镜像**（必须跟实现一致）。

---

## 2. 现状清单

### 2.1 已建成的东西

| 层 | 内容 |
| --- | --- |
| 后端 | 六模块 Maven（`rk-boot → rk-api → rk-service → rk-repository → rk-domain → rk-common`，enforcer 强制分层）。统一响应体 `R<T>`、`ErrorCode` 枚举、`BizException`、Sa-Token 鉴权、Redis 会话/计数 |
| 已实现接口 | 科目/大纲/知识点（公开）、认证与用户（含 **个人中心 05 §5.3 第 5~7 条**）、管理端内容维护（含 **P5 卷面模板 51/52 两条**）、**P4 练习闭环 11 个端点**、**P5 模拟考 10 个端点**（`/exams/**`）、**P6 学习统计 4 个端点**（`/stats/*`） |
| 前端 | Vue3 + Vite + TS + Pinia + Element Plus。首页书架、科目列表/详情/大纲、登录注册、**个人中心**（昵称/密码/当前科目/退出）、**管理端 4 页**（科目/大纲/知识点/卷面模板）、**练习答题页 + 错题本 + 收藏 + 学习统计页**（ECharts 趋势图 + 章节掌握度树）、**模拟考中心 + 答题页 + 成绩详情页** |
| 数据 | 250 道 SD 单选题（`PUBLISHED`）、452 个知识点、1000 个选项、20 个科目、20 份卷面模板 |
| 工具 | `scripts/generate/`（造题校验 + 导入）、`scripts/verify/`（5 类门禁 + 度量/截图/验收脚本） |

### 2.2 阶段进度（对照 `10 §10.1`）

| 阶段 | 状态 | 备注 |
| :-: | :-: | --- |
| P0 设计定稿 | ✅ | `docs/` 27 篇 |
| P1 骨架跑通 | ✅ | 鉴权链路；`StpInterface` 由 `25` 补齐 |
| P2 内容结构 | 代码/数据 ✅ · **验收 2/8** | `24 §3.2` 的 6 条被阻塞项**至今未重跑**（挡板已移除） |
| P3 题库生产 | **方案 B 完成** | 完整流水线（批次管理 + 审核工作台）未开始 |
| **P4 练习闭环** | **✅** | 6 种组卷模式 / 服务端复核判分 / 错题本 / 收藏 / 反馈 / 进度 |
| P5 模拟机考 | **✅** | `30`：十个接口 + 服务端计时 + 超时兜底 + 接入统计；`31`：模拟考中心 / 答题页 / 成绩详情 + store/组件；`32`：管理端卷面模板配置（05 §5.10 第 51/52 条 + 服务端五条校验） |
| P6 学习统计 | ✅（模考接入待 P5） | `28` 写入层 + `29` 读取接口与统计页；`exam_count`/`best_score` 恒 0/null，待 P5 |
| P7 公网部署 | ⬜ | 设计完整（`09`），一行容器代码都还没写 |
| P8 收尾扩充 | ⬜ | |

### 2.3 未做的欠账（按优先）

| 优先 | 欠账 | 出处 |
| :-: | --- | --- |
| 高 | 重跑 `24 §3.2` 的 6 条 P2 验收 + `2002` | `25 §6.2` |
| 中 | 知识点词表 452 → 受控（委托方选"保持现状"，登记在案） | `25 §2.4` |
| 中 | 三处文档回填：`05 §5.1.1` 补 406 例外、`06 §6.3` 补 5 个异常映射、`06 §6.5`/`09 §9.4` 记 Redis 实为 3.2.100 | `25 §6.1` |
| 中 | 删或留测试用户 `negctl_81217`（它的流水/反馈/错题行还在库里） | `26 §8` |
| 低 | 线上库 `contentStatus` 不一致（SAD / IPMP 是 `BULK` 但 0 题） | `25 §4` |
| 低 | ~~5173 被一个残留 vite 占着~~（本次实测 5173 已空出，dev 落在 5173） | `25 §6.2` |

### 2.4 ⚠️ 最大的风险：仓库 0 个提交

实测（2026-10-03）：

```
$ git -C d:\Project rev-list --count HEAD
fatal: ambiguous argument 'HEAD': unknown revision    # 即：还没有任何提交
$ git -C d:\Project status --short
?? .github/  ?? .gitignore  ?? .impeccable/  ?? .verify-tmp/  ?? DESIGN.md
?? PRODUCT.md  ?? README.md  ?? data/  ?? deploy/  ?? docs/  ?? scripts/  ?? server/  ?? web/
```

分支是 `master`，**全部内容都是未跟踪文件**。这意味着：

- 一次 `git clean -xdf`、一次误删目录，**整个项目（含文档史）就没了**；
- 这个风险不是假设：`21 §3 H1` 记的正是一次"清理日志"会连带销毁验证仪器的事故（`logs/` 被 `.gitignore` 忽略，而当时脚本住在里面）。

**委托方的既定约束**（`PRODUCT.md` / `23 §2`）：项目**在本地完成，暂不考虑上传远程仓库**。
所以这不是"赶紧 push"，而是"**本地至少要有一个提交**"——让工作区的当前状态成为可回溯的基线。
是否要提交由委托方决定；本文只把这个事实摆出来。

---

## 3. 怎么把它跑起来

### 3.1 本机环境事实（踩过的坑都在这里）

| 项 | 值 / 说明 |
| --- | --- |
| OS / Shell | Windows；工具跑在 **PowerShell**（不是 cmd/bash） |
| JDK | 21 |
| Maven | `D:\apache-maven\bin\mvn.cmd`（**不在 PATH**，用全路径） |
| pnpm | `C:\Users\OVER\AppData\Roaming\npm\pnpm.cmd`（**不在 PATH**） |
| bash | `D:\Git\bin\bash.exe`（**不在 PATH**；`.sh` 断言脚本用全路径调） |
| MySQL | 8.x，库名 `ruankao`；口令在 `deploy/.env` 的 `MYSQL_ROOT_PASSWORD` |
| **Redis** | `D:\Redis-x64-3.2.100\redis-cli.exe`。⚠️ **实际是 3.2.100，设计文档写的是 Redis 7**；`--no-auth-warning`、`--scan` 等 5+ 个参数**不可用**。口令在 `deploy/.env` 的 `REDIS_PASSWORD` |
| Chrome | `C:\Program Files\Google\Chrome\Application\chrome.exe`（截图/度量用，见 §4.3） |

**2026-11-20 换机记录（本机 AABB）**：JDK 21 / Maven `D:\apache-maven\bin\mvn.cmd` / Node 22 均在 PATH；pnpm 不在 PATH，已用隔离方式安装到 `C:\Users\AABB\.workbuddy-ai\binaries\node\workspace\node_modules\pnpm\bin\pnpm.cjs`（9.15.9）。MySQL 为 `D:\mysql-8.0.23-winx64\bin\mysql.exe`（本机是空库，已按 §3 重建 `ruankao` 并导入 250 题 + 回填 admin 哈希）；Redis 为 `D:\Redis-x64-3.2.100\redis-cli.exe`，原无 requirepass，已 `CONFIG SET` 一个本地开发口令并写入 `deploy/.env`。部署/启动时曾出现 `server.port` 被解析成随机值 50642 导致启动失败，**启动命令需显式追加 `--server.port=8080 --management.server.port=8081`**；此时 8080/8081 正常监听、health=UP、登录回路通过。Nginx 本次未使用（后于 2026-11-20 补了本地开发配置，见下）。另：首次打开页面时 Vite 会重优化依赖并删除 `web/node_modules/.vite/deps_temp_*`；该批量删除被 WorkBuddy 的安全守卫拦截会导致 dev 进程退出。已在 `web/vite.config.ts` 加 `optimizeDeps.noDiscovery=true` + 全部裸包 `include`，启动时一次性预打包，运行期不再重优化；若手动改依赖后仍出现同类退出，删掉 `web/node_modules/.vite` 重启即可。

**2026-11-20 前端改由 nginx 托管**：`pnpm -C web build` 产出 `web/dist`，本地配置 `deploy/nginx/local.conf`（80 端口、SPA `try_files` 回退、`/api/` 与 `/uploads/` 代理到 127.0.0.1:8080），nginx 为 `D:\nginx-1.30.6\nginx.exe`。启动需带 `-p D:\nginx-1.30.6\ -c <local.conf> -g "daemon off;"`，否则 Windows 下子进程会被回收。实测首页/子路由/API 代理/登录回路均 200；Vite 5173 不再使用。这只是本地开发托管，P7 的生产 compose/Nginx 仍未交付。

### 3.2 后端

**端口**：8080（业务，`http://127.0.0.1:8080/api/v1`）、8081（管理端点 `/actuator`）。
**环境变量**：从 `deploy/.env` **逐行**注入（文件不是 shell 脚本，别直接 `source`）。缺必需变量时启动校验会直接失败（AC-1）。

```powershell
# ① 构建（在 server/ 下）
& 'D:\apache-maven\bin\mvn.cmd' -pl rk-boot -am clean package -DskipTests

# ② 停旧进程 —— 必须先停！
#    正在运行的 JVM 持有 .verify-run\rk-boot.jar（缺 FILE_SHARE_DELETE），不停就覆盖不了
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
  Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force }

# ③ 部署 + 启动（工作目录必须是仓库根，jar 里用的是相对路径）
Start-Sleep -Seconds 3
Copy-Item 'server\rk-boot\target\rk-boot.jar' '.verify-run\rk-boot.jar' -Force
Get-Content 'deploy\.env' | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $kv = $_ -split '=', 2
  [Environment]::SetEnvironmentVariable($kv[0].Trim(), $kv[1].Trim(), 'Process')
}
java -jar .verify-run/rk-boot.jar
```

> **为什么从 `.verify-run\rk-boot.jar` 启动、而不是直接跑 `target\` 里的**：
> 门禁 1（`mvn clean package`）要删改 `target\rk-boot.jar`，而运行中的 JVM 会锁住它 →
> 二者互斥。跑副本是 `24` 约定的解法（`scripts/verify/README.md` 的 §"门禁 1 与门禁 5 在本机互斥" 详细写了这件事）。

**启动成功的标志**：日志出现 `Started RkBootApplication`，且 8080/8081 都在监听。
**如果启动就失败**：先看首个 `Caused by`——最常见的是 MySQL/Redis 连不上或 `.env` 注入漏了。

### 3.3 前端

```powershell
& 'C:\Users\OVER\AppData\Roaming\npm\pnpm.cmd' -C web dev
```

**访问 `http://localhost:5173`**：

- **必须用主机名 `localhost`，不能用 IP**（vite 只绑 `::1`，用 `127.0.0.1` 会 ECONNREFUSED）。
- **端口以 vite 实际输出为准**：`vite.config.ts` 声明的端口是 5173；若该端口已被占用，vite 会自动顺延到 5174（历史上曾因一个残留 vite 进程落在 5174，本次实测 5173 已空出）。
- 前端把 `/api` 指向 8080；后端不在时页面会显示各自的错误面板（不是白屏）。

### 3.4 凭据

| 用途 | 来源 |
| --- | --- |
| 管理端（需 `role=ADMIN`） | `deploy/.env` 的 `RK_TEST_ADMIN_USER` / `RK_TEST_ADMIN_PASSWORD` |
| 普通用户（验收脚本默认用它） | `negctl_81217` / `NegCtl@2026`（注册接口的副产物，`26 §8` 记着"删或留由委托方定"） |
| MySQL / Redis | `deploy/.env` 的 `MYSQL_ROOT_PASSWORD` / `REDIS_PASSWORD` |

> **不要把这些值写进文档或代码**——`deploy/.env` 是它们唯一的落点（AC-1：不硬编码密钥）。

---

## 4. 怎么验证（仪器与判据）

### 4.1 一键门禁

```bat
scripts\verify\run-all.cmd            :: 门禁 1~4（+5，若后端在线）
scripts\verify\run-all.cmd -Gate5     :: 只跑门禁 5（后端须在线）
```

| # | 门禁 | 覆盖 |
| :-: | --- | --- |
| 1 | `mvn -q clean package`（`server/`） | 后端编译 + 单测 + enforcer 分层约束 |
| 2 | `pnpm lint && type-check && build`（`web/`） | 前端三件套 |
| 3 | `check-numeric-consistency.sh` | 表数量 / 科目数的 13 处复写点 |
| 4 | `check-no-corpus-files.sh` | GR-1：`scripts/generate/` 下不得有真题语料 |
| 5 | `node scripts/verify/auth-chain.js` | 真 HTTP 鉴权回路（需 MySQL + Redis + 后端在线） |

**退出码 0 只在"本次要跑的门禁全部实跑且通过"时给出**；有"未验证"项也返回非 0。

### 4.2 P4 验收

```powershell
node scripts/verify/p4-acceptance.js     # 46 条断言，覆盖 10 §10.6 的 10 条验收
```

⚠️ **跑前必须清一次反馈限流键**，否则第 1 次反馈就会拿到 `1002`（限流是"同题同用户 24h 内 3 次"，上一轮的计数还在）：

```powershell
$pw = ((Get-Content 'deploy\.env' | Where-Object { $_ -match '^REDIS_PASSWORD=' }) -split '=',2)[1].Trim()
& 'D:\Redis-x64-3.2.100\redis-cli.exe' -a $pw KEYS 'rk:limit:feedback:*' |
  Where-Object { $_ -notmatch 'Warning' } | ForEach-Object { & 'D:\Redis-x64-3.2.100\redis-cli.exe' -a $pw DEL $_ }
```

**副作用（可接受）**：会在 `user_answer_record` / `user_wrong_question` / `rk_question_feedback` /
`user_favorite` / `user_practice_progress` 里留下测试用户的数据。生产前用 `deploy/sql` 重建即可。

### 4.3 渲染取证（截图 / 度量）

三个脚本都靠 **CDP（Chrome DevTools Protocol，端口 9222）**，而不是"看一眼觉得差不多"：

```powershell
# Chrome 必须与 node 脚本在**同一条命令里**启动 —— 进程随 shell 结束而结束
$cp = Start-Process -FilePath 'C:\Program Files\Google\Chrome\Application\chrome.exe' `
  -ArgumentList '--headless=new','--remote-debugging-port=9222','--remote-debugging-address=127.0.0.1',
  '--user-data-dir=D:\Project\.verify-tmp\chrome-x','--no-first-run','--disable-gpu','--window-size=1280,900','about:blank' -PassThru
for ($i=0; $i -lt 20; $i++) { try { Invoke-RestMethod 'http://127.0.0.1:9222/json/version' -TimeoutSec 2 | Out-Null; break } catch { Start-Sleep -Milliseconds 600 } }
node scripts/verify/measure.js     # 书架几何（读计算值）
node scripts/verify/capture.js     # 三张几何快照 → .impeccable/review/
```

> `Start-Process` 起 Chrome 时若报 `TRAE Sandbox Error ... Recent\CustomDestinations\...`，
> 那是 Chrome 自己在写系统目录，**与本任务无关**，忽略即可（脚本仍会正常退出 0）。

### 4.4 判据规矩（**这是本项目最容易栽的地方**）

1. **判定依据必须是业务码 `code`，不是"调用没抛异常"。**
   `25 §7` 第 5 条记的就是一次真实事故：测试脚本按"没抛异常"打 ✅，于是把 `1000` 打成了通过。
2. **任何断言/门禁都必须先用一次"人为制造漂移"证明它会失败。**
   `26 §5.1` 的做法：把多选的集合比较故意改回顺序敏感 → 两条测试如期失败 → 改回后恢复全绿。
   否则"全绿"只证明测试跑了，不证明它在看。
3. **"数值或结论对不上预期"时先问三句**：这是谁在表示它？（字节/字符？批处理转义？日志大小写？）
   我的判定依据是什么？（退出码？HTTP 状态？还是 `code`？）这个方法会不会恒真/恒假？
4. **不要相信"文档自称实测通过"**——本项目有"文档写了结论但结论有误"的历史，核实时要独立复现。

---

## 5. 这个项目反复踩的坑（写给下一位）

技术栈横跨 **JS / Java / SQL / shell / 日志 / MyBatis-OGNL** 六种表示层，每一层都有自己的
"表示 → 存储/传输"转换。已累计 **十几次**同族问题，`25 §7` 与 `26 §5` 有完整台账。高频的几类：

| 类 | 实例 |
| --- | --- |
| **度量单位错** | 用 `LENGTH()`（**字节**）去比 60（**字符**），于是断言"这不是 bcrypt" |
| **表示 ≠ 存储** | `mysql -B` 批处理模式**转义**了反斜杠，被当成库里真的存了双反斜杠（要靠 `UNHEX()` 字节级比对才避开） |
| **检索方式错** | `Select-String` **默认不区分大小写** → 匹配到 `ErrorReportValve`，把栈尾当栈首；用区分大小写的模式检索 `lastLoginAt` → 假阴性 |
| **表示层边界** | javadoc 里的 `*/` 会**提前终止注释块**；PowerShell **双引号套双引号**会让脚本解析失败；`-Dsurefire.failIfNoSpecifiedTests` 里的**点**会被 PowerShell 当参数分隔 |
| **ORM 参数语义** | MyBatis 的 `ParamMap` 对**未声明的键**会抛 `BindingException`（不是返回 null）——所以共享 SQL 片段引用的每个参数都必须在方法签名里声明 |
| **SQL 求值顺序** | MySQL 的 `SET` 子句**从左到右求值，后面的赋值读到前面已更新的值**。错题本把 `status` 的赋值排在 `consecutive_correct` 之后，`consecutive_correct + 1` 就会读到自增后的数、阈值提前一次触发（`26 §2.2` 已修并注明） |
| **环境被污染** | 用**被注入过的 shell** 去验证用户的 PATH，得出"mvn 正常"的假结论 |

---

## 6. 代码地图（想改哪里看哪里）

### 后端（`server/`）

| 位置 | 放什么 |
| --- | --- |
| `rk-domain/…/entity` | 实体。**纯 POJO，不带任何 MyBatis 注解**（`06 §6.1` 禁止 rk-domain 依赖 MyBatis） |
| `rk-domain/…/{dto,vo,enums}` | 请求 DTO（可变 Lombok 类 + jakarta 校验）、响应 VO（**record**）、枚举 |
| `rk-repository/…/mapper` + `resources/mapper/*.xml` | Mapper 接口 + **手写 SQL**。列名显式写出、列表查询一律有 `LIMIT` 上界（AC-4） |
| `rk-repository/…/{cache,dao}` | `CacheDao`（**可降级**的读缓存）、`LoginAttemptDao`/`ReplayGuardDao`/`FeedbackLimitDao`（**不可降级**的 Redis 计数器） |
| `rk-service/…/<域>/{XxxService, impl/XxxServiceImpl}` | 业务。构造器注入；**Controller 与 Service 都不写 try-catch**——业务失败一律 `throw new BizException(ErrorCode.X)` |
| `rk-service/…/stat/` | 统计写入与聚合（`28`）：`StatAggregator`（纯函数口径）、`StatisticsService`、`StatisticsScheduler`（每日 03:00）。`stat_*` 只是流水表的聚合缓存，事实永远在 `user_answer_record` |
| `rk-api/…/controller/{portal,admin}` | 控制器，返回 `R.ok(...)`。路径与字段名以 `05` 为准 |
| `rk-api/…/config/AuthWhitelist` | **白名单唯一事实来源**。⚠️ `/api/v1/practice/papers` **刻意不在里面**（评审 B6 的安全洞，见 `05 §5.13`） |

**三条硬约束（不可交易）**：AC-3 用户数据访问必须带 `user_id`；AC-5 只有 `PUBLISHED` 的题可入卷；
GR-1~GR-3 版权护栏（不复制/不改编真题原文，题目文案不出现"真题"字样）。

### 前端（`web/`）

| 位置 | 放什么 |
| --- | --- |
| `src/utils/request.ts` | axios 封装（**机制层，不认识任何业务**）。响应拦截器把 `R<T>` **拆成 `data`** 后 resolve |
| `src/api/*.ts` | 薄映射：只做"路径 + 参数"。页面级读取一律传 `{ silent: true }`，由页面自己的错误面板独家提示 |
| `src/stores/` | Pinia |
| `src/types/api.d.ts` | **手写维护**的接口类型（不从 Knife4j 生成，理由见 `07 §7.11`） |
| `src/views/portal/`、`src/views/admin/` | 页面 |
| `src/styles/` | `$rk-*` 变量与全局类（`rk-container` / `rk-page-title`） |

**既定事实**：桌面优先（`body` 最小宽度 1280px，窄了出横向滚动、不重排）；仅简体中文；无深色模式；
不用 Tailwind（与 Element Plus 变量体系冲突）；全站**一处** `box-shadow` 都没有。

---

## 7. 建议的下一步

| 顺序 | 事项 | 为什么是它 |
| :-: | --- | --- |
| 1 | **重跑 `24 §3.2` 的 6 条 P2 验收 + `2002`** | 挡板（管理端 403）已移除，这 6 条可以在一次会话内跑完；它们卡了整整一个阶段 |
| 2 | ~~P5 管理端卷面模板配置~~（已由 `32` 完成） | `/admin/paper-templates/{subjectId}` 读写 + 五条校验 + `/admin/paper-templates` 页；含 24 条断言的验收脚本 |
| 3 | **P7 公网部署**（委托方处理） | `deploy/` 目前只有本地 infra compose；生产 compose / Nginx / 两个 Dockerfile / CI 都还没有（09 §9.3~§9.9）。**动 deploy 前先做本地首次提交**（§2.4） |
| 4 | 浏览器渲染取证（考生端四页 + 个人中心 + 卷面模板页） | 本机策略禁止启动 Chrome（`31`/`32`/`33` 的同一未验证项）；换环境后可跑 `capture.js` / `shot-pages.js` |
| 5 | 三处文档回填 + 清理测试数据 | 见 §2.3 / §2.4 |

---

## 8. 一页速查

```powershell
# 路径
$mvn   = 'D:\apache-maven\bin\mvn.cmd'
$pnpm  = 'C:\Users\OVER\AppData\Roaming\npm\pnpm.cmd'
$bash  = 'D:\Git\bin\bash.exe'
$redis = 'D:\Redis-x64-3.2.100\redis-cli.exe'
$chrome= 'C:\Program Files\Google\Chrome\Application\chrome.exe'

# 后端（在 server/）：构建 → 停旧 → 拷副本 → 注入 .env → 启动
& $mvn -pl rk-boot -am clean package -DskipTests
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
  Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id $_ -Force }
Copy-Item 'server\rk-boot\target\rk-boot.jar' '.verify-run\rk-boot.jar' -Force
Get-Content 'deploy\.env' | ForEach-Object { if ($_ -notmatch '^\s*#' -and $_ -match '=') {
  $kv = $_ -split '=',2; [Environment]::SetEnvironmentVariable($kv[0].Trim(),$kv[1].Trim(),'Process') } }
java -jar .verify-run/rk-boot.jar

# 前端（在 web/）：http://localhost:5173  —— 必须用主机名，不能用 IP（端口被占则顺延到 5174）
& $pnpm -C web dev

# 验证
& $mvn -pl rk-api -am test          # 后端单测（含 AuthAnnotationTest）
& $pnpm -C web lint; & $pnpm -C web type-check; & $pnpm -C web build
& $bash scripts/verify/check-numeric-consistency.sh
node scripts/verify/p4-acceptance.js          # 跑前先清 rk:limit:feedback:*
scripts\verify\run-all.cmd                    # 一键门禁（门禁 1 与 5 在本机互斥）

# 数据库速查
$pw = ((Get-Content 'deploy\.env' | Where-Object { $_ -match '^MYSQL_ROOT_PASSWORD=' }) -split '=',2)[1].Trim()
mysql -u root "-p$pw" -B ruankao -e "SELECT COUNT(*) FROM rk_question WHERE status='PUBLISHED';"
```

---

## 9. 变更记录（本篇是"当前现场"，因此允许更新）

| 日期 | 改了什么 |
| --- | --- |
| 2026-10-03 | 建立本篇。记录：P0~P4 完成、服务已停（按 §3 启动）、**仓库 0 提交**、`stat_*` 欠账、判据与踩坑台账 |
| 2026-10-05 | 统计写入层落地（`28`）：`stat_*` 增量更新 + 掌握度重算 + 夜间全量重算；`onlyUnmastered` 从空操作变为生效。P6 读取接口/前端仍 ⬜ |
| 2026-10-05 | P6 读取接口 + 考生端统计页落地（`29`）：`/stats/overview|trend|outline|weak-kp` 四条 + `/stats` 页（ECharts 趋势、章节掌握度树、薄弱知识点）。仅剩"模考接入统计"待 P5 |
| 2026-10-05 | P5 模拟机考后端闭环落地（`30`）：十个接口 + Java 组卷（配额/难度/L1~L4）+ 服务端计时 + 断点续答 + 交卷判分 + 超时兜底 + 接入统计。考生端三页与管理端卷面模板配置仍 ⬜ |
| 2026-10-05 | P5 考生端三页落地（`31`）：模拟考中心 / 答题页（答题卡 + 倒计时 + 分节切换 + 论文草稿）/ 成绩详情；`stores/exam.ts` + `api/exam.ts` + 4 个 `components/exam/*`。管理端卷面模板配置仍 ⬜；CDP 截图因环境禁止启动 Chrome 未做 |
| 2026-10-05 | P5 管理端卷面模板配置落地（`32`）：`GET/PUT /admin/paper-templates/{subjectId}` + 服务端五条校验（满分口径不符即 `1001`）+ `/admin/paper-templates` 页（蓝图填充 / 只读派生序号与满分 / 即时校验）；验收脚本 24 条断言全通过。**P5 至此完成**；CDP 截图仍未做（Chrome 被策略拦） |
| 2026-10-05 | 考生端个人中心落地（`33`）：`PUT /users/me` / `/users/me/password` / `/users/me/current-subject` 三个接口 + `/profile` 页（基本信息 / 改昵称 / 改密码 / 当前科目 / 退出）+ 顶栏「我的」与科目下拉接通 + `mustChangePassword` 强制改密分支。验收脚本 17 条断言全通过；**07 路由表的考生端页面至此全部落地**，下一步进入 P7 部署 |
| 2026-11-20 | 换机后完成本地启动：由空库重建 `ruankao`（schema + init-data + 三份大纲 + verify 全绿）、导入 250 题、回填 admin 哈希；生成 `deploy/.env` 并给 Redis 设本地口令；Maven 构建后端 + 隔离 pnpm 安装前端依赖。后端需显式 `--server.port=8080 --management.server.port=8081` 启动；8080/8081/5173 正常，登录回路与前端代理均通过（详见 §3.1）。同日前端改由 nginx 托管：`web/dist` + `deploy/nginx/local.conf`（80 端口），5173 不再使用 |
| 2026-11-20 | 前端改用 nginx：新增 `deploy/nginx/local.conf`，`pnpm -C web build` 产出 dist，nginx 80 托管静态资源并代理 `/api`、`/uploads` 到后端 8080；实测首页、SPA 子路由、API 代理、管理员登录与 `/users/me` 全通过 |
| 2026-11-20 | 站点图标与管理端入口（`34`）：新增 `web/public/favicon.{svg,ico}` 与 `BrandMark.vue`；考生端顶栏与管理后台侧栏品牌区都加图标（后台折叠态只留图标），导航末尾加仅管理员可见的「管理端」链接跳 `/admin`。两轮内容合并计入 `34`，未另起文档；type-check / lint / build 通过，nginx 上 favicon 与页面均 200；未做浏览器渲染取证 |
