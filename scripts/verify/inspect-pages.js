// 逐页体检：打开每个路由，读**计算值**而不是看截图。
// 补的是 docs/18 §5 那条缺口——登录 / 注册 / 科目列表三页上一轮从未打开看过。
//
// 每页回答四件事：
//   1. 渲染了吗（标题、h1、关键元素在不在）
//   2. 有横向溢出吗（01 §1.5 桌面优先：该出现滚动条，但不该重排）
//   3. **文字对比度**：遍历可见文字，算它和大面积背景的对比，列出 <4.5:1 的
//   4. 控制台报错了吗
// 对比度那一条是重点：颜色刚被换过（docs/18 的 F14），必须实测而不是相信变量。
const CDP_PORT = process.env.CDP_PORT || 9222
const ORIGIN = process.env.TARGET_ORIGIN || 'http://localhost:4173'
const ROUTES = (process.env.ROUTES || '/login,/register,/subjects,/,/nope').split(',')

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
  await send('Emulation.setDeviceMetricsOverride', { width: 1280, height: 800, deviceScaleFactor: 1, mobile: false })
  return { send, events, ws }
}

// 在页面里跑的一段：渲染事实 + 对比度审计
const AUDIT = `(() => {
  const parse = (s) => {
    const m = /rgba?\\(([^)]+)\\)/.exec(s || '');
    if (!m) return null;
    const p = m[1].split(',').map((v) => parseFloat(v));
    return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
  };
  const lin = (v) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
  const lum = (c) => 0.2126 * lin(c.r) + 0.7152 * lin(c.g) + 0.0722 * lin(c.b);
  const ratio = (a, b) => { const x = lum(a), y = lum(b); const hi = Math.max(x, y), lo = Math.min(x, y); return (hi + 0.05) / (lo + 0.05); };
  // 有效背景：往上找第一个不透明的大面积底色（半透明层按近似忽略）
  const effBg = (el) => {
    let n = el;
    while (n && n !== document.documentElement) {
      const c = parse(getComputedStyle(n).backgroundColor);
      if (c && c.a > 0.95) return c;
      n = n.parentElement;
    }
    return parse(getComputedStyle(document.body).backgroundColor) || { r: 255, g: 255, b: 255, a: 1 };
  };
  const hasOwnText = (el) => [...el.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim().length);

  const findings = [];
  let textEls = 0;
  for (const el of document.querySelectorAll('body *')) {
    if (!hasOwnText(el)) continue;
    const cs = getComputedStyle(el);
    if (cs.visibility === 'hidden' || cs.display === 'none' || parseFloat(cs.opacity) === 0) continue;
    const r = el.getBoundingClientRect();
    if (r.width === 0 || r.height === 0) continue;
    textEls++;
    const fg = parse(cs.color);
    if (!fg) continue;
    const size = parseFloat(cs.fontSize);
    const weight = parseInt(cs.fontWeight, 10) || 400;
    // WCAG：18.66px 以上且加粗、或 24px 以上，算大字号，底线 3:1；否则 4.5:1
    const large = size >= 24 || (size >= 18.66 && weight >= 700);
    const floor = large ? 3 : 4.5;
    const cr = ratio(fg, effBg(el));
    if (cr < floor) {
      findings.push({
        text: el.textContent.trim().slice(0, 26),
        tag: el.tagName.toLowerCase() + (el.className && typeof el.className === 'string' ? '.' + el.className.split(/\\s+/)[0] : ''),
        color: cs.color, size: Math.round(size), ratio: Math.round(cr * 100) / 100, floor,
      });
    }
  }
  return JSON.stringify({
    title: document.title,
    path: location.pathname,
    h1: [...document.querySelectorAll('h1')].map((e) => e.textContent.trim()),
    textEls,
    overflowX: document.documentElement.scrollWidth - innerWidth,
    docScrollW: document.documentElement.scrollWidth,
    innerW: innerWidth,
    bodyText: document.body.innerText.replace(/\\s+/g, ' ').trim().slice(0, 220),
    contrast: findings,
  });
})()`

async function main() {
  const tab = await openTab()
  for (const route of ROUTES) {
    const url = ORIGIN + route
    tab.events.length = 0
    await tab.send('Page.navigate', { url })
    await new Promise((r) => setTimeout(r, 2500))
    const res = await tab.send('Runtime.evaluate', { expression: AUDIT, returnByValue: true })
    const errs = tab.events.filter(
      (e) => e.method === 'Runtime.exceptionThrown' || (e.method === 'Log.entryAdded' && e.params.entry.level === 'error'),
    )
    console.log('\n' + '='.repeat(78))
    if (!res.result?.result?.value) {
      console.log(`${route}  ← 无法求值：${JSON.stringify(res.result?.exceptionDetails?.text || res)}`)
      continue
    }
    const d = JSON.parse(res.result.result.value)
    console.log(`${route}   → ${d.path}   《${d.title}》`)
    console.log('='.repeat(78))
    console.log(`  h1      : ${d.h1.length ? d.h1.join(' | ') : '(无)'}`)
    console.log(`  正文开头: ${d.bodyText}`)
    console.log(`  横向    : docScrollW=${d.docScrollW} innerW=${d.innerW} 溢出=${d.overflowX}px`)
    console.log(`  文字节点: ${d.textEls} 个；对比度不达标: ${d.contrast.length} 个`)
    for (const f of d.contrast) {
      console.log(`    ✗ ${String(f.ratio).padStart(5)}:1 (需 ${f.floor})  ${f.size}px ${f.tag}  "${f.text}"  ${f.color}`)
    }
    console.log(`  控制台错误: ${errs.length}`)
    for (const e of errs.slice(0, 4)) {
      const t = e.params?.entry?.text || e.params?.exceptionDetails?.exception?.description || ''
      console.log(`    ! ${String(t).split('\n')[0].slice(0, 150)}`)
    }
  }
  await tab.send('Page.close')
  tab.ws.close()
}

main().catch((e) => {
  console.error('FAILED', e)
  process.exit(1)
})
