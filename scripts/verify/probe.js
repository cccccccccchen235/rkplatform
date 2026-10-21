// 把检测器报的 finding 落到具体元素 / 具体样式表规则上——不然改了也不知道改的是谁。
const CDP_PORT = process.env.CDP_PORT || 9222
const TARGET_URL = process.env.TARGET_URL || 'http://localhost:5174/'

async function main() {
  const page = await (
    await fetch(`http://127.0.0.1:${CDP_PORT}/json/new?${encodeURIComponent(TARGET_URL)}`, { method: 'PUT' })
  ).json()
  const ws = new WebSocket(page.webSocketDebuggerUrl)
  let id = 0
  const pending = new Map()
  const send = (m, p) => new Promise((res) => { const i = ++id; pending.set(i, res); ws.send(JSON.stringify({ id: i, method: m, params: p })) })
  ws.addEventListener('message', (e) => {
    const msg = JSON.parse(e.data)
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id) }
  })
  await new Promise((r) => ws.addEventListener('open', r))
  await send('Page.enable')
  await new Promise((r) => setTimeout(r, 7000))

  const evaluate = async (expression) => {
    const res = await send('Runtime.evaluate', { expression, returnByValue: true })
    return res.result.result.value
  }

  const desc = `(el) => el.tagName.toLowerCase() + (typeof el.className === 'string' && el.className.trim() ? '.' + el.className.trim().split(/\\s+/).join('.') : '')`

  // 1) 真的有动画的元素（transition-duration 不为 0），且动的是布局属性
  console.log('== 活元素里动布局属性的 ==')
  console.log(await evaluate(`(() => {
    const hit = [];
    document.querySelectorAll('*').forEach((el) => {
      const cs = getComputedStyle(el);
      const dur = cs.transitionDuration;
      if (!dur || /^0s(, 0s)*$/.test(dur)) return;
      const bad = cs.transitionProperty.split(',').map((s) => s.trim())
        .filter((p) => /^(height|width|padding|margin)/.test(p));
      if (bad.length) hit.push([(${desc})(el), bad.join('|'), dur]);
    });
    const vis = document.querySelectorAll('body *').length;
    return JSON.stringify({ renderedElements: vis, layoutAnimators: hit }, null, 1);
  })()`))

  // 2) 样式表里带 height/padding 过渡的规则——检测器读的是规则，不一定有活元素在用
  //
  // ⚠️ 这里曾经写成 `if (r.cssRules) { walk(r.cssRules); continue }`，于是**永远**报 0：
  // Chrome 支持 CSS 嵌套之后，**每一条** CSSStyleRule 都带一个 cssRules，
  // 类型是 CSSRuleList 但 length 为 0，而空对象在 JS 里是 **truthy**。
  // 实测：`:root` 的 cssRules 是 `[object CSSRuleList]`、length 0、!!truthy —— 于是每条
  // 规则都被 `continue` 跳过，规则自身的声明从来没被检查过。
  // docs/18 §5 曾把这个 0 当成"页面上不存在这样的规则"的证据，那是仪器坏了，不是页面干净。
  // 正确写法是两个条件都要：`r.cssRules && r.cssRules.length`，且**不能 continue**
  // （带嵌套的规则同时有自己的声明和子规则）。
  console.log('\\n== 样式表里的布局过渡规则 ==')
  console.log(await evaluate(`(() => {
    const out = [];
    let seen = 0;
    for (const sheet of document.styleSheets) {
      let rules; try { rules = sheet.cssRules } catch { continue }
      const walk = (list) => {
        for (const r of list) {
          seen++;
          if (r.cssRules && r.cssRules.length) walk(r.cssRules);
          if (r.style && r.style.getPropertyValue('transition')) {
            const t = r.style.getPropertyValue('transition');
            if (/height|padding|margin|width/.test(t)) {
              out.push([r.selectorText && r.selectorText.slice(0, 90), t.slice(0, 90)]);
            }
          }
        }
      };
      walk(rules);
    }
    return JSON.stringify({ rulesSeen: seen, matches: out.length, sample: out.slice(0, 8) }, null, 1);
  })()`))

  // 2b) 这些规则**有没有元素在用**——死规则和真问题在这里分岔
  console.log('\\n== 带这些类的元素有几个 ==')
  console.log(await evaluate(`(() => {
    const sels = ['.collapse-transition', '.el-collapse-transition-enter-active', '.el-collapse-transition-leave-active'];
    const rows = sels.map((s) => [s, document.querySelectorAll(s).length]);
    return JSON.stringify({ rows, verdict: rows.every(([, n]) => n === 0) ? '全部是死规则（0 个元素）' : '有元素在用' }, null, 1);
  })()`))

  // 3) 所有渲染成 #909399 的文字，连同背景色与字号
  console.log('\\n== #909399 的文字 ==')
  console.log(await evaluate(`(() => {
    const WEAK = 'rgb(144, 147, 153)';
    const bgOf = (el) => {
      for (let n = el; n; n = n.parentElement) {
        const c = getComputedStyle(n).backgroundColor;
        if (c && c !== 'rgba(0, 0, 0, 0)' && c !== 'transparent') return c;
      }
      return 'none';
    };
    const out = [];
    document.querySelectorAll('body *').forEach((el) => {
      if (!el.children.length && el.textContent.trim()) {
        const cs = getComputedStyle(el);
        if (cs.color === WEAK) out.push([el.textContent.trim().slice(0, 16), (${desc})(el), cs.fontSize, bgOf(el)]);
      }
    });
    return JSON.stringify({ count: out.length, items: out }, null, 1);
  })()`))

  ws.close()
}

main().catch((e) => { console.error('FAILED', e.message); process.exit(1) })
