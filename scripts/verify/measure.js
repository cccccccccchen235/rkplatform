// 用 CDP 量真实几何：书脊的位置、尺寸、书名列数。
// 刻意不做成"看一眼截图觉得差不多"——craft floor 要求读计算值。
const CDP_PORT = process.env.CDP_PORT || 9222
const TARGET_URL = process.env.TARGET_URL || 'http://localhost:5174/'

async function main() {
  const list = await (await fetch(`http://127.0.0.1:${CDP_PORT}/json/new?${encodeURIComponent(TARGET_URL)}`, { method: 'PUT' })).json()
  const ws = new WebSocket(list.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
  const send = (method, params) => new Promise((res) => { const i = ++id; pending.set(i, res); ws.send(JSON.stringify({ id: i, method, params })) })

  ws.addEventListener('message', (e) => {
    const msg = JSON.parse(e.data)
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id) }
  })
  await new Promise((r) => ws.addEventListener('open', r))

  await send('Page.enable')
  await new Promise((r) => setTimeout(r, 7000))

  const expr = `(() => {
    const out = { viewport: { w: innerWidth, h: innerHeight }, docScrollW: document.documentElement.scrollWidth, tiers: [] };
    document.querySelectorAll('.shelf__tier').forEach((tier) => {
      const board = tier.querySelector('.shelf__board');
      const label = tier.querySelector('.shelf__tier-label');
      const spines = [...tier.querySelectorAll('.spine')].map((el) => {
        const r = el.getBoundingClientRect();
        const nameEl = el.querySelector('.spine__name-text');
        const nr = nameEl ? nameEl.getBoundingClientRect() : null;
        const cs = nameEl ? getComputedStyle(nameEl) : null;
        const codeEl = el.querySelector('.spine__code');
        return {
          code: el.querySelector('.spine__code')?.textContent ?? '?',
          codeColor: codeEl ? getComputedStyle(codeEl).color : null,
          borderColor: getComputedStyle(el).borderTopColor,
          borderStyle: getComputedStyle(el).borderTopStyle,
          x: Math.round(r.x), y: Math.round(r.y), w: Math.round(r.width), h: Math.round(r.height),
          nameW: nr ? Math.round(nr.width) : null,
          nameH: nr ? Math.round(nr.height) : null,
          nameScrollH: nameEl ? nameEl.scrollHeight : null,
          cols: nr && cs ? Math.round(nr.width / (parseFloat(cs.fontSize) * 1.15)) : null,
          overflowX: el.scrollWidth > el.clientWidth ? el.scrollWidth - el.clientWidth : 0,
        };
      });
      out.tiers.push({ label: label?.textContent?.trim(), boardY: board ? Math.round(board.getBoundingClientRect().y) : null, spines });
    });
    out.drawer = (() => { const d = document.querySelector('.drawer'); if (!d) return null; const r = d.getBoundingClientRect(); return { y: Math.round(r.y), h: Math.round(r.height) } })();
    out.legend = (() => { const d = document.querySelector('.legend'); if (!d) return null; const r = d.getBoundingClientRect(); return { y: Math.round(r.y), h: Math.round(r.height), bottom: Math.round(r.bottom) } })();
    return JSON.stringify(out);
  })()`

  const res = await send('Runtime.evaluate', { expression: expr, returnByValue: true })
  const data = JSON.parse(res.result.result.value)

  console.log('viewport', data.viewport, 'docScrollW', data.docScrollW)
  for (const tier of data.tiers) {
    console.log(`\n== ${tier.label} (board y=${tier.boardY}) ==`)
    for (const s of tier.spines) {
      console.log(
        `  ${String(s.code).padEnd(5)} x=${String(s.x).padStart(5)} y=${String(s.y).padStart(4)} w=${s.w} h=${String(s.h).padStart(3)}` +
          ` | name ${s.nameW}x${s.nameH} cols=${s.cols} scrollH=${s.nameScrollH} overflowX=${s.overflowX}`,
      )
      console.log(
        `        border ${s.borderStyle} ${s.borderColor} | codeColor ${s.codeColor}`,
      )
    }
  }
  // 书脊统一高度：把 h 收成一个集合打印。集合里只剩一个值才叫"统一"。
  const heights = [...new Set(data.tiers.flatMap((t) => t.spines.map((s) => s.h)))].sort((a, b) => a - b)
  const widths = [...new Set(data.tiers.flatMap((t) => t.spines.map((s) => s.w)))].sort((a, b) => a - b)
  const overflow = data.tiers.flatMap((t) => t.spines).filter((s) => s.overflowX > 0)
  console.log('\n[断言] 脊高集合', heights, '(应只剩 1 个值)')
  console.log('[断言] 脊宽集合', widths)
  console.log('[断言] 横向溢出书脊', overflow.length, overflow.map((s) => s.code))

  console.log('\ndrawer', data.drawer, '\nlegend', data.legend)
  ws.close()
}

main().catch((e) => { console.error('FAILED', e.message); process.exit(1) })
