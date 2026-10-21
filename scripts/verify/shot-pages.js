// 逐页截图（23 §4.1 第 4 项：三页的**设计观看**）。
//
// 为什么另写一个：`capture.js` 是"一个页面的三张几何快照"（桌面/抽屉/900px），
// 而这一项要的是"多页人眼过一遍"。上一轮的截图读回来是 `[Unsupported Image]`，
// 所以这条一直停在"程序化审计 0 异常、但没人真的看过"。本脚本把 PNG 落到
// `.impeccable/review/`（与 capture.js 同一落点），再用 Read 工具逐张读——
// Read 认 PNG，这是这次换掉的那个"截图方式"。
//
// 运行：TARGET_ORIGIN=http://localhost:5174 node scripts/verify/shot-pages.js
//   ROUTES 可覆盖，格式 `名字=路径,名字=路径`；默认见下。
//   FULL=1 时用整页截图（长页面，如大纲树），否则只拍首屏 1280x900。

const fs = require('node:fs')
const path = require('node:path')

const CDP_PORT = process.env.CDP_PORT || 9222
const ORIGIN = process.env.TARGET_ORIGIN || 'http://localhost:4173'
const FULL = process.env.FULL === '1'
const OUT_DIR = process.env.OUT_DIR || path.join(process.cwd(), '.impeccable', 'review')

const ROUTES = (
  process.env.ROUTES ||
  'login=/login,register=/register,subject-list=/subjects,subject-detail=/subjects/2,subject-outline=/subjects/2/outline'
)
  .split(',')
  .map((s) => {
    const i = s.indexOf('=')
    return { name: s.slice(0, i), route: s.slice(i + 1) }
  })

async function openTab() {
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
    const m = JSON.parse(e.data)
    if (m.id && pending.has(m.id)) {
      pending.get(m.id)(m)
      pending.delete(m.id)
    }
  })
  await new Promise((r) => ws.addEventListener('open', r))
  await send('Page.enable')
  await send('Runtime.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: FULL ? 900 : 900, deviceScaleFactor: 1, mobile: false })
  return { send, ws }
}

async function main() {
  fs.mkdirSync(OUT_DIR, { recursive: true })
  const tab = await openTab()
  for (const { name, route } of ROUTES) {
    await tab.send('Page.navigate', { url: ORIGIN + route })
    await new Promise((r) => setTimeout(r, 2600))
    const shot = await tab.send('Page.captureScreenshot', {
      format: 'png',
      captureBeyondViewport: FULL,
    })
    const file = path.join(OUT_DIR, `shot-${name}.png`)
    fs.writeFileSync(file, Buffer.from(shot.result.data, 'base64'))
    const bytes = fs.statSync(file).size
    console.log(`${name.padEnd(16)} ${route.padEnd(24)} -> ${file}  (${Math.round(bytes / 1024)} KB)`)
  }
  await tab.send('Page.close')
  tab.ws.close()
}

main().catch((e) => {
  console.error('FAILED', e)
  process.exit(1)
})
