# `scripts/verify/` · 验证与取证脚本

本目录是**验证仪器**的落点，不是离线业务工具（造题 / 导入的落点是 `scripts/generate/`，见 README §3 与 [08 §8.4.4](../../docs/08-ai-pipeline.md)）。

> **为什么从 `logs/` 搬到这里**（对应 [21 §3 H1](../../docs/21-p2-audit-round.md)）：这批脚本原先住在 `logs/`，而 `.gitignore` 第 51 行忽略 `logs/`、第 50 行忽略 `*.log`。后果是「清理日志」与「销毁验证仪器」是**同一个动作**——`git clean -xdf` 或一次例行清日志就会把它们删掉，而 [20](../../docs/20-p2-verification-round.md) 把三页的回测结果、`F10` 的首屏数字、检测器结论全挂在这批脚本上。搬到本目录后它们不再被忽略，可以进版本控制。

## 一键门禁（先看这一节）

`run-all.cmd` 是**本地质量门禁的入口**，串起五类断言（[23 §4.2](../../docs/23-next-round-plan.md)）：

| # | 命令 | 覆盖 |
| :-: | --- | --- |
| 1 | `mvn -q clean package`（`server/`） | 后端编译 + 单测 + **enforcer 分层约束** |
| 2 | `pnpm lint && pnpm type-check && pnpm build`（`web/`） | 前端三件套 |
| 3 | `check-numeric-consistency.sh` | 表数量 / 科目数的 13 处复写点 |
| 4 | `check-no-corpus-files.sh` | GR-1：`scripts/generate/` 下无真题语料 |
| 5 | `node scripts/verify/auth-chain.js` | 真 HTTP 鉴权回路（需 MySQL + Redis + 后端在线） |

```bat
scripts\verify\run-all.cmd            :: 门禁 1~4（+5，若后端恰在线）
scripts\verify\run-all.cmd -Gate5     :: 只跑门禁 5（后端须在线）
```

**入口自己解析 `bash` / `mvn` / `java` 的全路径**（`D:\Git\bin` → `D:\Git\usr\bin` → Program Files → PATH），
**不要求使用者先配 PATH**——否则它与 [22](../../docs/22-infra-round.md) 的文档犯同一个错（[23 §2.1](../../docs/23-next-round-plan.md)）。

### ⚠️ 门禁 1 与门禁 5 在本机互斥

门禁 1 要删/改 `server/rk-boot/target/rk-boot.jar`；正在运行的后端 JVM 以**缺 `FILE_SHARE_DELETE`**
的方式持有该 jar（实测：改名失败、追加写成功）。因此后端在线时 `clean` 与 `package` **都会失败**，
而门禁 5 又要求后端在线。**入口不静默降级**，而是把互斥关系直接说清并给出两种跑法：

- **a** 停掉后端 → 跑本入口（门禁 1~4 绿）→ 启动后端 → `run-all.cmd -Gate5`；
- **b** 让后端从 `target/` 之外的 jar 副本启动，本入口即可一次全绿（约定见 [24](../../docs/24-*.md)）。

**退出码**：`0` 仅当本次要跑的门禁**全部实跑且通过**。有"未验证"项时也返回非 0——避免把
"跑了一半"当成全绿（正是 [23 §7](../../docs/23-next-round-plan.md) 要防的半截证据）。

> **实现分两个文件**：`run-all.cmd` 是纯 ASCII 的瘦启动器，逻辑与中文输出在 `run-all.ps1`（UTF-8 **带 BOM**）。
> 原因：`cmd.exe` 按控制台代码页（本机 GBK）读 `.cmd`，UTF-8 的中文注释会被拆成杂散命令而报错——这是实测踩到的坑。

## 脚本清单

| 文件 | 测什么 | 前置 |
| --- | --- | --- |
| `run-all.cmd` / `run-all.ps1` | 上面五类门禁的一键入口（**先看上一节**） | 见各门禁 |
| `firstload.js` | 首屏**同源传输字节**（CDP `Network.encodedDataLength`，冷缓存，按 Document/Script/Stylesheet 分类） | 生产预览在 `localhost:4173`；headless Chrome 的 CDP 在 `9222` |
| `probe.js` | 读 `document.styleSheets` 的全部规则，找布局动画 / 浅色文本 | 同上 |
| `contrast.js` | 逐元素算 WCAG 对比度 | 同上 |
| `inspect-pages.js` | 逐页程序化审计（元素、宽度、横向滚动） | 同上 |
| `capture.js` | 三张几何快照（桌面 / 抽屉 / 900px 窄屏），落在 `.impeccable/review/` | 同上 |
| `measure.js` / `measure-interaction.js` | 关键路径的耗时与交互行为 | 后端在 `8080/8081` |
| `auth-chain.js` | 注册 → 登录 → `/users/me` → 登出全链路，含 `2001/2003/2004/2005/1001` 与 `F1` 时间格式断言 | 后端在 `8080/8081`，Redis 必须带口令在跑 |
| `probe-register-token.js` | 单独探注册返回的 token 形态 | 同上 |
| `detector-control.html` / `detector-control-linked.{html,css}` | `impeccable detect` 的**永久阳性对照**：证明 `low-contrast` 看得见外链 CSS 而 `layout-transition` 看不见 | 两个 html 与那个 css 放进被测站点的根，再用同一命令各扫一次 |

## 对照的用法（这两组**不要删**）

把三个文件复制进被测站点的静态根（本地即 `web/dist/`），然后用**同一条命令**分别扫：

```bash
impeccable detect --json --viewport 1280x800 http://localhost:4173/detector-control.html
impeccable detect --json --viewport 1280x800 http://localhost:4173/detector-control-linked.html
```

期望：前者两项都报，后者只报 `low-contrast`。这个差异本身就是要保住的证据——它说明**生产预览上的 `[]` 不等于规则不存在**，判断规则在不在要用 `probe.js`。

## 迁移记录

2026-10-02 从 `logs/` 迁入（[21 §5.5](../../docs/21-p2-audit-round.md) 第 1、3 步）。因此 **`docs/18`、`docs/19`、`docs/20`、`docs/21` 里写的 `logs/xxx.js` 路径均已过期**，实际位置即本目录。那四份按「旧文档一律不回改」保持原样，改动记在 [22](../../docs/22-infra-round.md)。
