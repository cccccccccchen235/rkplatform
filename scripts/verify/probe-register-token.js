// 定点排查：注册签发的 token 为什么被 /users/me 拒（2005），而登录签发的不被拒。
// 只回答一个问题，不做别的。token 值一律不回显（只打长度与是否同一串）。
const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api/v1'
const PASS = 'Qa12345678'

async function call(method, path, { body, token } = {}) {
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  let json = null
  try { json = await res.json() } catch { /* ignore */ }
  return { http: res.status, json }
}
const brief = (t) => (typeof t === 'string' ? `len=${t.length}` : String(t))

async function me(tag, token) {
  const r = await call('GET', '/users/me', { token })
  console.log(`  ${tag.padEnd(38)} → http=${r.http} code=${r.json?.code}${r.json?.data?.username ? ' user=' + r.json.data.username : ''}`)
  return r
}

async function main() {
  const user = `qa_probe_${Date.now().toString(36)}`

  console.log('\n1) 注册')
  const reg = await call('POST', '/auth/register', { body: { username: user, password: PASS } })
  const regToken = reg.json?.data?.token
  console.log(`   注册 code=${reg.json?.code} token ${brief(regToken)}`)

  console.log('\n2) 注册后立刻用注册 token 调 me')
  await me('register token（立即）', regToken)

  console.log('\n3) 登录（同一账号）')
  const login = await call('POST', '/auth/login', { body: { username: user, password: PASS } })
  const loginToken = login.json?.data?.token
  console.log(`   登录 code=${login.json?.code} token ${brief(loginToken)} 与注册 token 同一串=${loginToken === regToken}`)

  console.log('\n4) 用登录 token 调 me')
  await me('login token', loginToken)

  console.log('\n5) 再回头用注册 token 调 me（看登录是否把它踢掉了）')
  await me('register token（登录之后）', regToken)

  console.log('\n6) 用登录 token 登出，然后两个 token 各再试一次')
  const out = await call('POST', '/auth/logout', { token: loginToken })
  console.log(`   登出 code=${out.json?.code}`)
  await me('login token（登出后）', loginToken)
  await me('register token（登出后）', regToken)
}

main().catch((e) => { console.error('FAILED', e); process.exit(2) })
