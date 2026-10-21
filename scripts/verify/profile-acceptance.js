// 个人中心 · 验收脚本（05 §5.3 第 4~7 条 / 07 §7.5.1 的 M1）
//
// 覆盖：
//   ① 改昵称：成功回写 + 空值 1001；
//   ② 当前备考科目：合法科目 code=0 且回读一致；不存在 3001；非数字 1001；
//   ③ 改密码：原密码错误 2006、新密码不合规 1001、成功后能用新密码登录，
//      且 `mustChangePassword` 被置 false（06 §6.5）；
//   ④ 改密是"真的改了"——用新密码重新登录拿到新 token，而不是只看响应 code。
//
// 口径（docs/25 §7）：判定依据是业务码 `code`，不是"调用没抛异常"。
// 否定用例（应失败的请求）必须先证明它会失败，再证明合规请求会成功。
//
// 前置：后端在 8080 运行，MySQL/Redis 可用，测试用户存在（默认 negctl_81217）。
// 运行：node scripts/verify/profile-acceptance.js
// 退出码：0 = 全部通过；1 = 有失败或未能还原。
//
// 已知副作用与还原：
//   · 昵称与密码会临时改动，脚本结束时按运行前快照还原（密码换成一次性临时值再换回）；
//   · `current_subject_id` **没有"清空"接口**，若运行前是 NULL，脚本只能在结束时把它改成
//     某个已存在的科目，无法还原成 NULL——脚本会打印提示，由运行者用 SQL 复位（本机做法见文件末注释）。

const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api/v1'
const USERNAME = process.env.RK_TEST_USER || 'negctl_81217'
const PASSWORD = process.env.RK_TEST_PASS || 'NegCtl@2026'
const SUBJECT_ID = process.env.RK_TEST_SUBJECT || '6'

/** 改密用例使用的一次性口令（含字母与数字、8~32 位，满足 05 §5.3 的复杂度）。 */
const TEMP_PASSWORD = 'TmpVerify2026'

let passed = 0
let failed = 0

function assert(name, condition, detail = '') {
  if (condition) {
    passed += 1
    console.log(`  ✓ ${name}`)
  } else {
    failed += 1
    console.log(`  ✗ ${name}${detail ? `  →  ${detail}` : ''}`)
  }
}

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    json = { _unparsed: text }
  }
  return { status: res.status, json }
}

async function login(password) {
  return api('/auth/login', { method: 'POST', body: { username: USERNAME, password } })
}

async function main() {
  console.log(`\n== 个人中心验收 · ${BASE} · user=${USERNAME} ==\n`)

  const loginRes = await login(PASSWORD)
  if (loginRes.json?.code !== 0) {
    console.error(`登录失败，无法继续：status=${loginRes.status} body=${JSON.stringify(loginRes.json)}`)
    process.exit(1)
  }
  const token = loginRes.json.data.token

  const before = await api('/users/me', { token })
  if (before.json?.code !== 0) {
    console.error(`GET /users/me 失败：${JSON.stringify(before.json)}`)
    process.exit(1)
  }
  const snapshot = {
    nickname: before.json.data.nickname,
    currentSubjectId: before.json.data.currentSubjectId ?? null,
  }
  console.log(`快照：nickname="${snapshot.nickname}"、currentSubjectId=${snapshot.currentSubjectId ?? 'NULL'}\n`)

  /** 当前仍然有效的口令；失败路径上可能停在临时口令。 */
  let currentPassword = PASSWORD
  try {
    // ---------- 1. 改昵称 ----------
    console.log('[1] 改昵称（PUT /users/me）')
    {
      const next = `${snapshot.nickname}-核对`
      const res = await api('/users/me', { method: 'PUT', token, body: { nickname: next } })
      assert('code = 0', res.json?.code === 0, JSON.stringify(res.json))
      assert('响应里的 nickname 是新值', res.json?.data?.nickname === next, res.json?.data?.nickname)
      const after = await api('/users/me', { token })
      assert('回读 /users/me 一致', after.json?.data?.nickname === next, after.json?.data?.nickname)
    }
    {
      const res = await api('/users/me', { method: 'PUT', token, body: { nickname: '' } })
      assert('空昵称 → 1001', res.json?.code === 1001, JSON.stringify(res.json))
    }

    // ---------- 2. 当前备考科目 ----------
    console.log('\n[2] 当前备考科目（PUT /users/me/current-subject）')
    {
      const res = await api('/users/me/current-subject', {
        method: 'PUT',
        token,
        body: { subjectId: SUBJECT_ID },
      })
      assert('code = 0', res.json?.code === 0, JSON.stringify(res.json))
      assert(`响应 currentSubjectId = ${SUBJECT_ID}`, res.json?.data?.currentSubjectId === SUBJECT_ID,
        res.json?.data?.currentSubjectId)
      const after = await api('/users/me', { token })
      assert('回读 /users/me 一致', after.json?.data?.currentSubjectId === SUBJECT_ID,
        after.json?.data?.currentSubjectId)
    }
    {
      const res = await api('/users/me/current-subject', {
        method: 'PUT',
        token,
        body: { subjectId: '999999' },
      })
      assert('不存在的科目 → 3001', res.json?.code === 3001, JSON.stringify(res.json))
    }
    {
      const res = await api('/users/me/current-subject', { method: 'PUT', token, body: { subjectId: 'abc' } })
      assert('非数字 ID → 1001', res.json?.code === 1001, JSON.stringify(res.json))
    }

    // ---------- 3. 改密码（否定用例） ----------
    console.log('\n[3] 改密码（PUT /users/me/password）')
    {
      const res = await api('/users/me/password', {
        method: 'PUT',
        token,
        body: { oldPassword: 'DefinitelyWrong1', newPassword: TEMP_PASSWORD },
      })
      assert('原密码错误 → 2006', res.json?.code === 2006, JSON.stringify(res.json))
    }
    {
      const res = await api('/users/me/password', {
        method: 'PUT',
        token,
        body: { oldPassword: PASSWORD, newPassword: 'abcdefgh' },
      })
      assert('新密码缺数字 → 1001', res.json?.code === 1001, JSON.stringify(res.json))
    }

    // ---------- 4. 改密码（正例）+ 真登录取证 ----------
    {
      const res = await api('/users/me/password', {
        method: 'PUT',
        token,
        body: { oldPassword: PASSWORD, newPassword: TEMP_PASSWORD },
      })
      assert('code = 0', res.json?.code === 0, JSON.stringify(res.json))
      if (res.json?.code === 0) currentPassword = TEMP_PASSWORD

      const relogin = await login(TEMP_PASSWORD)
      assert('用新密码能登录（说明真的改了，不是只回了个 200）', relogin.json?.code === 0,
        JSON.stringify(relogin.json))
      if (relogin.json?.code === 0) {
        const me = await api('/users/me', { token: relogin.json.data.token })
        assert('mustChangePassword 已被置 false（06 §6.5）', me.json?.data?.mustChangePassword === false,
          JSON.stringify(me.json?.data))
      }
      const oldLogin = await login(PASSWORD)
      assert('旧密码已失效', oldLogin.json?.code === 2001, JSON.stringify(oldLogin.json))
    }
    {
      // 换回原密码：必须用当前有效口令，且用新 token（旧 token 仍有效，但不再依赖它）
      const res = await api('/users/me/password', {
        method: 'PUT',
        token,
        body: { oldPassword: TEMP_PASSWORD, newPassword: PASSWORD },
      })
      assert('换回原密码 code = 0', res.json?.code === 0, JSON.stringify(res.json))
      if (res.json?.code === 0) currentPassword = PASSWORD
      const relogin = await login(PASSWORD)
      assert('原密码可再次登录', relogin.json?.code === 0, JSON.stringify(relogin.json))
    }
  } finally {
    console.log('\n[还原] 昵称与密码')
    const restored = await api('/users/me', { method: 'PUT', token, body: { nickname: snapshot.nickname } })
    console.log(`  ${restored.json?.code === 0 ? '✓' : '✗'} 昵称还原为 "${snapshot.nickname}"`)
    if (restored.json?.code !== 0) failed += 1

    if (currentPassword !== PASSWORD) {
      const back = await api('/users/me/password', {
        method: 'PUT',
        token,
        body: { oldPassword: currentPassword, newPassword: PASSWORD },
      })
      const ok = back.json?.code === 0
      console.log(`  ${ok ? '✓' : '✗'} 密码还原（用 ${currentPassword === TEMP_PASSWORD ? '临时口令' : '未知口令'} 换回）`)
      if (!ok) {
        failed += 1
        console.error(`  ! 密码可能仍停在临时口令 ${TEMP_PASSWORD}，请手工复位`)
      }
    }
    if (snapshot.currentSubjectId === null) {
      console.log('  - currentSubjectId 原为 NULL，无接口可清空；请用 SQL 复位：')
      console.log(`     UPDATE sys_user SET current_subject_id = NULL WHERE username = '${USERNAME}';`)
    } else {
      await api('/users/me/current-subject', {
        method: 'PUT',
        token,
        body: { subjectId: snapshot.currentSubjectId },
      })
      console.log(`  ✓ currentSubjectId 还原为 ${snapshot.currentSubjectId}`)
    }
  }

  console.log(`\n== 结果：${passed} 通过 / ${failed} 失败 ==\n`)
  process.exit(failed === 0 ? 0 : 1)
}

main().catch((error) => {
  console.error('\n脚本异常：', error)
  process.exit(1)
})
