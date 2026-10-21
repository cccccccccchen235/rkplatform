// 截图取证：一次会话里出三张——空架、取下一本、窄窗。
// 用 CDP 而不是 chrome --screenshot，因为「取下一本」这个状态需要先点一下。
const fs = require('node:fs')

const CDP_PORT = process.env.CDP_PORT || 9222
const TARGET_URL = process.env.TARGET_URL || 'http://localhost:5174/'
const OUT_DIR = process.env.OUT_DIR || '.impeccable/review'

const SHOTS = [
  { name: 'desktop', width: 1280, height: 800, click: null },
  { name: 'desktop-drawer', width: 1280, height: 800, click: 'IPT' },
  { name: 'narrow-900', width: 900, height: 800, click: null },
]

async function main() {
  const page = await (
    await fetch(`http://127.0.0.1:${CDP_PORT}/json/new?${encodeURIComponent(TARGET_URL)}`, { method: 'PUT' })
  ).json()
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
  const send = (method, params) => new Promise((res) => { const i = ++id; pending.set(i, res); ws.send(JSON.stringify({ id: i, method, params })) })
  ws.addEventListener('message', (e) => {
    const msg = JSON.parse(e.data)
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id) }
  })
  await new Promise((r) => ws.addEventListener('open', r))

  await send('Page.enable')
  await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 800, deviceScaleFactor: 1, mobile: false })
  await new Promise((r) => setTimeout(r, 7000))

  fs.mkdirSync(OUT_DIR, { recursive: true })

  for (const shot of SHOTS) {
    await send('Emulation.setDeviceMetricsOverride', { width: shot.width, height: shot.height, deviceScaleFactor: 1, mobile: false })
    // 换尺寸后先回顶部再点，保证每次都是同一个起始状态
    await send('Runtime.evaluate', { expression: 'scrollTo(0,0)' })
    await new Promise((r) => setTimeout(r, 600))

    if (shot.click) {
      await send('Runtime.evaluate', {
        expression: `[...document.querySelectorAll('.spine')].find((el) => el.querySelector('.spine__code')?.textContent === '${shot.click}').click()`,
      })
      await new Promise((r) => setTimeout(r, 1200)) // 等平滑滚动停稳
    }

    const geometry = await send('Runtime.evaluate', {
      expression: `JSON.stringify({
        w: innerWidth, h: innerHeight,
        docScrollW: document.documentElement.scrollWidth,
        docScrollH: document.documentElement.scrollHeight,
        scrollY: Math.round(scrollY),
        horizontalScroll: document.documentElement.scrollWidth > innerWidth,
        reflowed: (() => {
          // 窄窗不许重排：三层的书脊宽度必须仍是 88，层数不变
          const spines = [...document.querySelectorAll('.spine')];
          return { spineCount: spines.length, widths: [...new Set(spines.map((el) => Math.round(el.getBoundingClientRect().width)))] };
        })(),
      })`,
      returnByValue: true,
    })
    console.log(shot.name, geometry.result.result.value)

    const png = await send('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false })
    fs.writeFileSync(`${OUT_DIR}/${shot.name}.png`, Buffer.from(png.result.data, 'base64'))
  }

  ws.close()
}

main().catch((e) => { console.error('FAILED', e.message); process.exit(1) })
