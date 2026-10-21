// 浏览器层的登录态取证（23 §4.1 的第 2、3 项）。
//
// 补的缺口是【半截证据】：`auth-chain.js` 只在 HTTP 层打了 2005/2001/2003/2004，
// 而"浏览器里会话真的过期之后，拦截器会不会清 token、会不会跳登录、有没有提示"
// 从来没有在浏览器里做过——那两个发生在 `stores/user.ts` 与 `utils/request.ts` 里，
// curl 看不见。本脚本用真 CDP 浏览器跑真前端（真 HTTP 到真后端）。
//
// 覆盖：
//   A 未登录访问 /admin/subjects → 跳登录页 + **不请求任何管理端模块**（07 §7.12）
//   B token 持久化：写 rk:token 后整页加载 → 守卫 fetchMe 恢复登录态（顶栏昵称/退出）
//   C 已登录但非管理员访问 /admin/subjects → 回首页 + 提示 + 仍未请求管理端模块
//   D 会话过期（服务端删会话、浏览器留着旧 token）→ 2005 清 token + 提示 + 跳登录
//   D2 同一过期态下停在公开页整页加载 → 只观测落点，不断言（落点有两种可能的竞态）
//
// 口径与 auth-chain.js 一致：只打真实 HTTP + 读真实 DOM，不做"看起来对"的推断。
// 密码沿用 auth-chain.js 的本机测试固定值，与任何真实凭据无关，且不回显。
//
// 运行：node scripts/verify/browser-auth.js
//   TARGET_ORIGIN 默认 http://localhost:4173（与同目录其他脚本一致）；
//   ⚠️ 必须用**主机名** localhost，不要写 127.0.0.1——vite dev server 只绑 ::1，
//      用 IP 会得到 ECONNREFUSED（实测）。
//   API_BASE 默认直连后端 8080，Node 侧不走前端代理（前端 origin 是谁都行）。

const CDP_PORT = process.env.CDP_PORT || 9222
const ORIGIN = process.env.TARGET_ORIGIN || 'http://localhost:4173'
const API = process.env.API_BASE || 'http://127.0.0.1:8080/api/v1'
const PASS = 'Qa12345678'
const ts = Date.now().toString(36)
const USER = `qa_br_${ts}`

let failures = 0
function check(name, cond, detail) {
  const ok = !!cond
  if (!ok) failures++
  console.log(`  ${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== undefined && !ok ? `  → ${JSON.stringify(detail)}` : ''}`)
  return ok
}
function note(label, value) {
  console.log(`  ····  ${label}: ${JSON.stringify(value)}`)
}

// ---------------- Node 侧 HTTP（注册 / 删会话）----------------

async function api(method, path, { body, token } = {}) {
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(API + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  let json = null
  try {
    json = await res.json()
  } catch {
    /* 非 JSON 留空 */
  }
  return { http: res.status, json }
}

// ---------------- CDP ----------------

async function openTab() {
  const page = await (
    await fetch(`http://127.0.0.1:${CDP_PORT}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })
  ).json()
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
  const events = []
  const send = (method, params) =>
    new Promise((res) => {
      const i = ++id
      pending.set(i, res)
      ws.send(JSON.stringify({ id: i, method, params }))
    })
  ws.addEventListener('message', (e) => {
    const msg = JSON.parse(e.data)
    if (msg.id && pending.has(msg.id)) {
      pending.get(msg.id)(msg)
      pending.delete(msg.id)
    } else if (msg.method) {
      events.push(msg)
    }
  })
  await new Promise((r) => ws.addEventListener('open', r))
  await send('Runtime.enable')
  await send('Log.enable')
  await send('Page.enable')
  await send('Network.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 800, deviceScaleFactor: 1, mobile: false })
  return { send, events, ws }
}

async function evaluate(tab, expression) {
  const r = await tab.send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
  if (r.result?.exceptionDetails) {
    throw new Error(r.result.exceptionDetails.exception?.description || r.result.exceptionDetails.text)
  }
  return r.result?.result?.value
}

async function goto(tab, url, waitMs = 2600) {
  tab.events.length = 0
  await tab.send('Page.navigate', { url })
  await new Promise((r) => setTimeout(r, waitMs))
}

async function setToken(tab, token) {
  // 先确保在目标 origin 上（about:blank 没有 localStorage 访问权）
  const script = token
    ? `localStorage.setItem('rk:token', JSON.stringify(${JSON.stringify(token)})); 'set'`
    : `Object.keys(localStorage).filter((k) => k.startsWith('rk:')).forEach((k) => localStorage.removeItem(k)); 'cleared'`
  return evaluate(tab, script)
}

const nap = (ms) => new Promise((r) => setTimeout(r, ms))

const bodyText = (tab) =>
  evaluate(tab, `document.body.innerText.replace(/\\s+/g, ' ').trim().slice(0, 400)`)

const locOf = (tab) => evaluate(tab, `location.pathname + location.search`)

/**
 * 读 ElMessage 的 toast 文本。
 *
 * ⚠️ **不能**用 body.innerText 判断提示是否出现（实测踩过）：ElMessage 的容器挂在
 * app 根节点**之后**，而正文截断取的是开头——提示落在第 400 字符以外，读不到，
 * 于是"明明弹了"却断言失败。更坏的一种是**假阳性**：登录页自己的文案里就有
 * 「未登录或登录已过期」，拿 /过期/ 去 match 正文永远为真，即使根本没弹提示。
 * 所以提示一律按 `.el-message` 元素读，按元素断言。
 * 另外它是 3 秒自动消失的——采样要赶在消失之前。
 */
const toastText = (tab) =>
  evaluate(tab, `[...document.querySelectorAll('.el-message')].map((e) => e.textContent).join('|')`)

/** 本次导航里请求过的、属于管理端的模块（dev 按源码路径下发；生产是 admin chunk，同样命中）。 */
function adminFetches(tab) {
  return tab.events
    .filter((e) => e.method === 'Network.requestWillBeSent')
    .map((e) => e.params.request.url)
    .filter((u) => /views\/admin\/|layouts\/AdminLayout|views%2Fadmin%2F|AdminLayout-/.test(u))
}

function pageErrors(tab) {
  return tab.events.filter(
    (e) => e.method === 'Runtime.exceptionThrown' || (e.method === 'Log.entryAdded' && e.params.entry.level === 'error'),
  )
}

// ---------------- 主流程 ----------------

async function main() {
  console.log(`目标前端：${ORIGIN}   后端 API：${API}`)
  const tab = await openTab()

  // ---- A：未登录访问管理端 ----
  console.log('\n=== A. 未登录访问 /admin/subjects ===')
  {
    await goto(tab, ORIGIN + '/login', 1500)
    await setToken(tab, null)
    await goto(tab, ORIGIN + '/admin/subjects')
    const loc = await locOf(tab)
    check('落在登录页', String(loc).startsWith('/login'), loc)
    check('redirect 带回原地址 /admin/subjects', decodeURIComponent(String(loc)).includes('/admin/subjects'), loc)
    const admin = adminFetches(tab)
    check('未请求任何管理端模块（07 §7.12）', admin.length === 0, admin.slice(0, 5))
  }

  // ---- B：token 持久化 ----
  console.log('\n=== B. stores/user.ts 的 token 持久化 ===')
  let token = null
  {
    const reg = await api('POST', '/auth/register', { body: { username: USER, password: PASS } })
    check('注册拿到 token（HTTP 层前置）', reg.json?.code === 0 && typeof reg.json?.data?.token === 'string', reg.json)
    token = reg.json?.data?.token

    await setToken(tab, token)
    await goto(tab, ORIGIN + '/')
    const home = await bodyText(tab)
    check('整页加载后顶栏显示昵称（= 守卫 fetchMe 成功恢复了登录态）', home.includes(USER))
    check('顶栏出现「退出」（不是「登录/注册」）', home.includes('退出') && !home.includes('注册'))

    // 再整页刷新到另一条路由，证明持久化不是内存里的假象
    await goto(tab, ORIGIN + '/subjects')
    const list = await bodyText(tab)
    check('切到另一条路由后仍是登录态', list.includes(USER))
    note('科目页正文开头', list.slice(0, 80))
  }

  // ---- C：非管理员的角色守卫 ----
  console.log('\n=== C. 普通用户访问 /admin/subjects（requiresAdmin 守卫）===')
  {
    await goto(tab, ORIGIN + '/admin/subjects', 1200)
    const toast = await toastText(tab)
    check('弹出「无权限访问该页面」（按 .el-message 断言）', toast.includes('无权限访问该页面'), toast)
    await nap(1200)
    const loc = await locOf(tab)
    check('被送回首页', String(loc).split('?')[0] === '/', loc)
    const admin = adminFetches(tab)
    check('仍未请求任何管理端模块', admin.length === 0, admin.slice(0, 5))
  }

  // ---- D：会话过期（服务端删会话，浏览器留着旧 token）----
  console.log('\n=== D. 浏览器内 2005：会话真的过期 ===')
  {
    const out = await api('POST', '/auth/logout', { token })
    check('服务端已删除该会话（HTTP 层前置）', out.json?.code === 0, out.json)
    // 关键：浏览器**不知道**这件事，localStorage 里的 token 还在
    const still = await evaluate(tab, `localStorage.getItem('rk:token')`)
    check('浏览器仍留着旧 token（这就是"过期"这个场景）', !!still)

    // 整页加载一个需要登录的路由 → 守卫 fetchMe → 401/2005
    // 先在 toast 还活着的时候取样（3 秒自动消失），再等跳转落定。
    await goto(tab, ORIGIN + '/admin/subjects', 1400)
    const toast = await toastText(tab)
    check('弹出会话过期提示（按 .el-message 断言，不拿正文凑）', /过期|登录/.test(toast), toast)
    await nap(1400)
    const loc = await locOf(tab)
    check('被送到登录页', String(loc).startsWith('/login'), loc)
    check('redirect 带回原地址', decodeURIComponent(String(loc)).includes('/admin/subjects'), loc)
    const after = await evaluate(tab, `localStorage.getItem('rk:token')`)
    check('本地 token 已被清除', after === null, after)
    check('导航全程未请求管理端模块', adminFetches(tab).length === 0, adminFetches(tab).slice(0, 5))
  }

  // ---- D2：同一过期态下停在**公开页**整页加载（只观测）----
  console.log('\n=== D2. 会话过期后停在公开页整页加载（观测，不断言）===')
  {
    const reg = await api('POST', '/auth/register', { body: { username: `qa_br2_${ts}`, password: PASS } })
    const t2 = reg.json?.data?.token
    await setToken(tab, t2)
    await goto(tab, ORIGIN + '/', 1500)
    check('先确认这次是登录态', (await bodyText(tab)).includes(`qa_br2_${ts}`), null)
    const killed = await api('POST', '/auth/logout', { token: t2 })
    check('服务端已删除该会话', killed.json?.code === 0, killed.json)
    await goto(tab, ORIGIN + '/', 1400)
    note('toast', await toastText(tab))
    await nap(1400)
    note('落点', await locOf(tab))
    note('本地 token', await evaluate(tab, `localStorage.getItem('rk:token')`))
    note('正文开头', (await bodyText(tab)).slice(0, 100))
  }

  // 期望里含一条 401：它们正是"会话已删除、token 还在用"的传输层表达（05 §5.1.1），
  // 是这条链路的正常产物，不是缺陷——但数量要看着，多出来的才是真异常。
  const errs = pageErrors(tab)
  console.log(`\n页面级报错（Runtime.exceptionThrown / console.error）：${errs.length}`)
  for (const e of errs.slice(0, 5)) {
    const t = e.params?.entry?.text || e.params?.exceptionDetails?.exception?.description || ''
    console.log(`    ! ${String(t).split('\n')[0].slice(0, 150)}`)
  }

  await tab.send('Page.close')
  tab.ws.close()
  console.log(`\n${failures === 0 ? '全部通过' : `${failures} 条未通过`}`)
  process.exit(failures === 0 ? 0 : 1)
}

main().catch((e) => {
  console.error('FAILED', e)
  process.exit(2)
})
