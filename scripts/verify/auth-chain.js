// 鉴权链路端到端实测。上一轮本机无 Redis（Sa-Token 会话不可降级），
// 所以 docs/18 §5 的整张「未实测」表都空着；Redis 就位后由本脚本补上。
//
// 覆盖：注册 / 登录 / 登出 / me / 2001 / 2002(尽力) / 2003 / 2004 / 2005，
// 以及 **F1 的时间格式**（唯一能端到端证明 JacksonConfig 的路径就是这几个接口）。
//
// 口径：只打真实 HTTP，不信代码。每条断言打印 PASS/FAIL，不做"看起来对"的推断。
// 密码是本机测试账号的固定值，与任何真实凭据无关，且**不回显**。

const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api/v1'
const PASS = 'Qa12345678' // 含字母与数字、10 位，满足 RegisterRequest 的校验
const ts = Date.now().toString(36)

let failures = 0
function check(name, cond, detail) {
  const ok = !!cond
  if (!ok) failures++
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== undefined && !ok ? `  → ${JSON.stringify(detail)}` : ''}`)
  return ok
}

async function call(method, path, { body, token } = {}) {
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  // sa-token.token-prefix: Bearer —— 头里必须带前缀，裸 token 会被拒为 2005。
  // 前端 request.ts 也是这么发的（这条约定两端必须一致）。
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  let json = null
  try {
    json = await res.json()
  } catch {
    /* 非 JSON 响应留空 */
  }
  return { http: res.status, json }
}

const DT = /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/ // 05 §5.1 钉死的下发格式

async function main() {
  const user = `qa_${ts}`
  const lockUser = `qa_lock_${ts}`

  console.log('\n=== 0. 匿名访问受保护接口 ===')
  {
    const r = await call('GET', '/users/me')
    check('匿名 GET /users/me → HTTP 401', r.http === 401, r)
    check('匿名 GET /users/me → code 2005', r.json?.code === 2005, r.json)
    check('匿名响应带 traceId', typeof r.json?.traceId === 'string', r.json)
  }

  console.log('\n=== 1. 注册（应有 token + UserVO） ===')
  let token = null
  {
    const r = await call('POST', '/auth/register', { body: { username: user, password: PASS } })
    check('code = 0', r.json?.code === 0, r.json)
    token = r.json?.data?.token
    check('data.token 为非空字符串', typeof token === 'string' && token.length > 0, r.json?.data)
    check('data.expireAt 是 yyyy-MM-dd HH:mm:ss（F1）', DT.test(r.json?.data?.expireAt ?? ''), r.json?.data?.expireAt)
    const u = r.json?.data?.user
    check('user.createdAt 是 yyyy-MM-dd HH:mm:ss（F1）', DT.test(u?.createdAt ?? ''), u?.createdAt)
    check('user.username 回显正确', u?.username === user, u?.username)
    check('user.nickname 缺省取 username', u?.nickname === user, u?.nickname)
    check('user.role = USER', u?.role === 'USER', u?.role)
    check('user.mustChangePassword = false（自助注册）', u?.mustChangePassword === false, u)
    check('user.status 存在', typeof u?.status === 'string', u?.status)
    check('未下发 password 之类敏感字段', u && !('password' in u) && !('passwordHash' in u), Object.keys(u ?? {}))
  }

  console.log('\n=== 2. 带 token 的 me（F1 的第二条路径） ===')
  {
    const r = await call('GET', '/users/me', { token })
    check('code = 0', r.json?.code === 0, r.json)
    check('username 一致', r.json?.data?.username === user, r.json?.data)
    // register 会自动登录（签发 token），但**不打 lastLoginAt 戳**——只有 verifyLogin 打
    // （UserServiceImpl:98）。所以这里期望 null，不是缺陷，只是"注册算不算一次登录"的取向。
    check('注册后 lastLoginAt 仍为 null（register 不打这个戳）', r.json?.data?.lastLoginAt === null, r.json?.data?.lastLoginAt)
    check('id 是字符串（非数字）', typeof r.json?.data?.id === 'string', r.json?.data?.id)
  }

  console.log('\n=== 3. 登录 ===')
  let loginToken = null
  {
    const r = await call('POST', '/auth/login', { body: { username: user, password: PASS } })
    check('code = 0', r.json?.code === 0, r.json)
    loginToken = r.json?.data?.token
    check('登录也签发 token', typeof loginToken === 'string', r.json?.data)
    check('登录响应的 expireAt 格式正确（F1）', DT.test(r.json?.data?.expireAt ?? ''), r.json?.data?.expireAt)
    // 上一轮的缺口：只断言"签发了 token"，没断言"这个 token 能用"。
    // 而 sa-token 的 token-prefix=Bearer —— 头里少写前缀，签发照常成功、用起来全是 2005。
    const m = await call('GET', '/users/me', { token: loginToken })
    check('登录 token 真的能用于 me（不只是"签发了"）', m.json?.code === 0, m.json)
    // F1 的端到端证据：这个字段是 LocalDateTime，只有真的走一遍序列化才证明得了 JacksonConfig。
    check('登录后 lastLoginAt 是 yyyy-MM-dd HH:mm:ss（F1 端到端）', DT.test(m.json?.data?.lastLoginAt ?? ''), m.json?.data?.lastLoginAt)
  }

  console.log('\n=== 4. 2001 用户名或密码错误 ===')
  {
    const r = await call('POST', '/auth/login', { body: { username: user, password: 'WrongPass123' } })
    check('code = 2001', r.json?.code === 2001, r.json)
  }

  console.log('\n=== 5. 2003 用户名已存在 ===')
  {
    const r = await call('POST', '/auth/register', { body: { username: user, password: PASS } })
    check('code = 2003', r.json?.code === 2003, r.json)
  }

  console.log('\n=== 6. 1001 参数校验失败 ===')
  {
    const r = await call('POST', '/auth/register', { body: { username: 'ab', password: 'short' } })
    check('code = 1001', r.json?.code === 1001, r.json)
    check('data 是字段级错误列表', Array.isArray(r.json?.data), r.json?.data)
  }

  console.log('\n=== 7. 2004 登录失败次数过多（注册一个一次性账号来撞） ===')
  {
    const reg = await call('POST', '/auth/register', { body: { username: lockUser, password: PASS } })
    check('一次性账号注册成功', reg.json?.code === 0, reg.json)
    const codes = []
    for (let i = 0; i < 5; i++) {
      const r = await call('POST', '/auth/login', { body: { username: lockUser, password: 'WrongPass123' } })
      codes.push(r.json?.code)
    }
    console.log(`        5 次错误口令的返回序列：${codes.join(', ')}`)
    const sixth = await call('POST', '/auth/login', { body: { username: lockUser, password: PASS } })
    check('第 6 次（正确口令）被拒为 2004', sixth.json?.code === 2004, sixth.json)
  }

  console.log('\n=== 8. 登出后 token 失效，再登录又能用 ===')
  {
    const out = await call('POST', '/auth/logout', { token: loginToken })
    check('登出 code = 0', out.json?.code === 0, out.json)
    const after = await call('GET', '/users/me', { token: loginToken })
    check('登出后原 token → HTTP 401', after.http === 401, after)
    check('登出后原 token → code 2005', after.json?.code === 2005, after.json)
    const again = await call('POST', '/auth/login', { body: { username: user, password: PASS } })
    const m = await call('GET', '/users/me', { token: again.json?.data?.token })
    check('登出后重新登录，新 token 可用', m.json?.code === 0, m.json)
  }

  console.log(`\n${failures === 0 ? '全部通过' : `${failures} 条未通过`}`)
  process.exit(failures === 0 ? 0 : 1)
}

main().catch((e) => {
  console.error('FAILED', e)
  process.exit(2)
})
