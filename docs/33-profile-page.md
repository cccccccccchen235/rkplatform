# 33 · 考生端个人中心（M1）

> 本轮做 07 §7.3.1 / §7.5.1 的 `/profile`「个人中心」= 01 的 M1（F-101~105），
> 含 05 §5.3 第 **5~7** 条三个此前缺失的接口（改昵称 / 改密码 / 设置当前备考科目）。
>
> 这是 P1~P6 之外唯一还挂在 07 路由表上、却既没有页面也没有接口的考生端页面；
> 补完后考生端 07 §7.3.1 的页面全部落地，**可以进入部署准备**。

## 1. 交付了什么

| 层 | 内容 |
| --- | --- |
| 接口 | `PUT /api/v1/users/me`（改昵称）、`PUT /users/me/password`（改密码）、`PUT /users/me/current-subject`（设置当前备考科目） |
| 后端 | DTO `NicknameUpdateRequest` / `PasswordUpdateRequest` / `CurrentSubjectRequest`；`UserService(+Impl)` 三个方法；`SysUserMapper(+XML)` 三条 UPDATE；`UserController` 三个方法 |
| 前端 | `views/portal/profile.vue`（基本信息 / 改昵称 / 改密码 / 当前科目 / 退出登录）；`stores/user.ts` 补 `updateNickname` / `updatePassword` / `setCurrentSubject`；`api/auth.ts` 补三个函数；`types/api.d.ts` 补三个请求体类型；`types/labels.ts` 补 `USER_ROLE_LABEL` / `USER_STATUS_LABEL` |
| 路由与导航 | `/profile` 路由（`requiresAuth`，考生端布局）；顶栏新增「我的」；**顶栏科目下拉从"仅提示"改为真正调用第 7 条接口** |
| 守卫 | `mustChangePassword = true` 时强制跳 `/profile` 并提示（06 §6.5 / 05 §5.3 第 4 条要求的行为） |
| 验证 | `scripts/verify/profile-acceptance.js`：17 条断言，含原密码错误、新密码不合规、新密码真能登录、`mustChangePassword` 置 false，并自动还原昵称与密码 |

## 2. 关键口径

- **改密成功后强制登出**（07 §7.5.1）：服务端**不踢会话**，由前端在改密成功后显式调
  `POST /auth/logout`（`stores/user.ts` 的 `updatePassword` 内部完成），再跳登录页。
  顺序不能反——先清本地会让服务端会话一直活到超时。
- **`mustChangePassword` 与改密是同一条 UPDATE**：`updatePassword` 把
  `password_hash` 与 `must_change_password = 0` 一起写（06 §6.5），不存在"密码改了、标记还在"的中间态。
- **新密码复杂度沿用注册口径**（8~32 位且含字母与数字）：改密比注册松，就等于注册规则只在第一天有效。
  旧密码只判非空——它可能是在规则收紧之前设的。
- **当前科目必须存在且启用**，否则 `3001`；ID 非数字 `1001`。校验放在服务层（要查库），
  DTO 只判非空。
- **退出登录二次确认**（07 §7.5.1）：退出不可撤销，点错一次要重登一遍。
- 顶栏科目下拉用 `:model-value` 单向绑定：请求失败时显示值自动回弹，不会留下
  "界面上切了、库里没变"的假象。

## 3. 偏离登记

| # | 偏离 | 理由 | 影响 |
| :-: | --- | --- | --- |
| 1 | 05 §5.3 只给了第 5~7 条的**路径与方法**，没有请求体字段表。本实现定形为 `{nickname}`、`{oldPassword, newPassword}`、`{subjectId}` | 字段名与 `UserVO`/注册请求保持一致，不新造词 | 若将来 05 补定义且命名不同，改这三处 |
| 2 | 第 5/7 条的**响应** 05 未定义。本实现返回更新后的 `UserVO`（第 6 条返回 `null`） | 前端拿到即可回写 store，省一次 `GET /users/me` 往返；不回 `UserVO` 的话 store 只能本地拼，容易与库不一致 | 属本实现自定契约 |
| 3 | 第 5~7 条的**错误码** 05 未定义。本实现复用 `1001`（参数非法）、`2005`（用户不存在）、`2006`（原密码错误）、`3001`（科目不存在或停用） | 全部是 05 §5.1.2 已有码，不新增 | 无 |
| 4 | 当前科目**不能清空**（没有"取消选择"语义） | 05 §5.3 第 7 条写的是"设置当前备考科目"；支持清空需要新增一个"可空"的语义与 DTO | 用户若想回到"未选择"状态，当前无入口 |
| 5 | 守卫里新增 `mustChangePassword` 强制跳转分支。07 §7.3.3 的守卫伪代码**没有**这一步 | 05 §5.3 第 4 条与 06 §6.5 都要求"前端凭该字段强制跳转改密页"，不实现则该字段形同虚设 | 管理员重置密码后，用户第一次进任何页面都会被送到 `/profile` |
| 6 | 改密**不踢其它设备上的会话** | 05/06 未要求；踢会话需要 Sa-Token 按 userId 注销全部 token，属于安全增强而非契约行为 | 用户改密后，其它已登录设备上的旧 token 仍有效直到超时 |
| 7 | 本页的**浏览器渲染取证未做** | 本机策略禁止启动 Chrome（同 `31`/`32`） | type-check / build / 真 HTTP 不能证明渲染无异常 |

## 4. 验证

| 项 | 结果 |
| --- | --- |
| `mvn -pl rk-api -am test` | 通过（含 `AuthAnnotationTest` 的"四个方法必须显式声明鉴权注解"、`ParamValidationTest`） |
| `pnpm -C web type-check` / `lint` / `build` | 通过 |
| `node scripts/verify/profile-acceptance.js` | **17 通过 / 0 失败** |
| ↳ 改昵称 | `code=0` 且响应与回读一致；空昵称 `1001` |
| ↳ 当前科目 | 合法 `6` → `code=0` 且回读一致；`999999` → `3001`；`abc` → `1001` |
| ↳ 改密码 | 原密码错 `2006`；新密码缺数字 `1001`；改成功后**用新密码重新登录**取得新 token；旧密码登录 `2001`；再换回原密码后原密码可登录 |
| ↳ `mustChangePassword` | 用 SQL 置 1 → 调 `PUT /users/me/password` → 库中变 0（06 §6.5 落地取证） |
| ↳ 还原 | 昵称、`current_subject_id`（SQL 置回 NULL）、`must_change_password`（0）、原密码登录——均复原 |
| 仓库门禁 | `check-numeric-consistency.sh` 13/13、`check-no-corpus-files.sh` 通过 |
| **未做** | 浏览器渲染取证（Chrome 被策略拦） |

## 5. 文件

后端新增：`rk-domain` 的 `dto/NicknameUpdateRequest`、`dto/PasswordUpdateRequest`、`dto/CurrentSubjectRequest`。

后端修改：`rk-repository` 的 `SysUserMapper(+XML)`（3 条 UPDATE）；
`rk-service` 的 `UserService(+Impl)`（新增 3 个方法 + `SubjectMapper` 依赖 + ID 解析）；
`rk-api` 的 `UserController`（新增 3 个 PUT）。

前端新增：`src/views/portal/profile.vue`。

前端修改：`src/api/auth.ts`、`src/stores/user.ts`、`src/types/api.d.ts`、`src/types/labels.ts`、
`src/router/portal.routes.ts`、`src/router/index.ts`（强制改密分支）、`src/layouts/PortalLayout.vue`（「我的」+ 科目下拉接真接口）。

工具新增：`scripts/verify/profile-acceptance.js`。

## 6. 下一步

1. **P7 公网部署**（委托方处理）：`deploy/docker-compose.yml`、`deploy/nginx/`、两个 Dockerfile、
   `.env.example`、CI workflow、09 §9.9 的首次上线步骤与 §9.10 检查单。建议先做**本地首次 git 提交**
   （27 §2.4 的最大风险），再开始动 `deploy/`。
2. 浏览器渲染取证（考生端四页 + 个人中心 + 卷面模板页；需能启动 Chrome 的环境）。
3. 管理端其余 8 页 + P3 题库生产流水线（05 §5.11 第 55~67 条、§5.12）。
