// 首屏体积实测：用 CDP 的 Network 域量**真实传输字节**（encodedDataLength，即压缩后 + 响应头），
// 而不是拿构建报告里各 chunk 的 gzip 值相加。
//
// 为什么不用构建报告：那个数只能算「所有产物之和」或「某几个 chunk 之和」，
// 相加规则是人为选的，换个人会得到另一个数（docs/18 的 F10 之前就吃过这个亏）。
// 这里量的是浏览器在 / 上真正下载了什么，可复现且有唯一解释。
//
// 只统计打到被测站自己的请求（同源），API 调用不算——它们不是首屏代码体积。
const CDP_PORT = process.env.CDP_PORT || 9222
const TARGET_URL = process.env.TARGET_URL || 'http://localhost:4173/'
const ORIGIN = new URL(TARGET_URL).origin

async function main() {
  const page = await (
    await fetch(`http://127.0.0.1:${CDP_PORT}/json/new?${encodeURIComponent('about:blank')}`, { method: 'PUT' })
  ).json()
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
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
    }
  })
  await new Promise((r) => ws.addEventListener('open', r))

  await send('Network.enable')
  await send('Page.enable')
  await send('Emulation.setDeviceMetricsOverride', {
    width: 1280,
    height: 800,
    deviceScaleFactor: 1,
    mobile: false,
  })
  // 冷缓存：确保量到的是完整首屏，而不是 304/内存缓存
  await send('Network.clearBrowserCache')

  const rows = []
  ws.addEventListener('message', (e) => {
    const msg = JSON.parse(e.data)
    if (msg.method !== 'Network.loadingFinished' && msg.method !== 'Network.responseReceived') return
    if (msg.method === 'Network.loadingFinished') {
      const row = rows.find((r) => r.requestId === msg.params.requestId)
      if (row) {
        row.encoded = msg.params.encodedDataLength
        row.decoded = msg.params.decodedBodyLength
      }
      return
    }
    const { requestId, response, type } = msg.params
    if (!response.url.startsWith(ORIGIN)) return
    rows.push({
      requestId,
      type,
      url: response.url.replace(ORIGIN, ''),
      status: response.status,
      mime: response.mimeType,
      encoded: 0,
      decoded: 0,
    })
  })

  await send('Page.navigate', { url: TARGET_URL })
  await new Promise((r) => setTimeout(r, 5000))

  // 页面自身也可能发 XHR（科目列表）。按类型拆开报告，让"代码体积"和"数据"分开。
  const code = rows.filter((r) => ['Document', 'Script', 'Stylesheet', 'Other'].includes(r.type))
  const data = rows.filter((r) => !['Document', 'Script', 'Stylesheet', 'Other'].includes(r.type))

  const sum = (list) => list.reduce((s, r) => s + r.encoded, 0)
  const line = (r) =>
    `  ${String(r.type).padEnd(11)} ${String(r.status).padEnd(4)} ${(r.encoded / 1024).toFixed(2).padStart(8)} KB  ${r.url}`

  console.log('--- 首屏全部同源请求 ---')
  code.sort((a, b) => b.encoded - a.encoded).forEach((r) => console.log(line(r)))
  console.log(`  首屏代码体积（Document+Script+Stylesheet）: ${(sum(code) / 1024).toFixed(2)} KB`)
  if (data.length) {
    console.log('--- 运行时数据（不计入首屏体积） ---')
    data.forEach((r) => console.log(line(r)))
  }
  await send('Page.close')
  ws.close()
}

main().catch((e) => {
  console.error('FAILED', e)
  process.exit(1)
})
