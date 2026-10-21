// 交互取证：点书脊之后，抽屉到底有没有出现在视口里。
// 这条断言只有在浏览器里跑过才算数——"scrollIntoView 应该会滚"不是证据。
const CDP_PORT = process.env.CDP_PORT || 9222
const TARGET_URL = process.env.TARGET_URL || 'http://localhost:5174/'

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
  await new Promise((r) => setTimeout(r, 7000))

  const evaluate = async (expression) => {
    const res = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true })
    if (res.result?.exceptionDetails) throw new Error(JSON.stringify(res.result.exceptionDetails))
    return res.result.result.value
  }

  // ---- 1. 点最矮的那本（IPT，书名最长、书脊最短） ----
  const clicked = await evaluate(`(() => {
    const spine = [...document.querySelectorAll('.spine')].find((el) => el.querySelector('.spine__code')?.textContent === 'IPT');
    spine.click();
    return true;
  })()`)
  await new Promise((r) => setTimeout(r, 900)) // 等平滑滚动走完

  const afterClick = await evaluate(`(() => {
    const d = document.querySelector('.drawer');
    const r = d.getBoundingClientRect();
    return JSON.stringify({
      clicked: ${clicked},
      scrollY: Math.round(scrollY),
      drawerTop: Math.round(r.top), drawerBottom: Math.round(r.bottom), drawerH: Math.round(r.height),
      viewportH: innerHeight,
      fullyVisible: r.top >= 0 && r.bottom <= innerHeight,
      name: d.querySelector('.drawer__name')?.textContent ?? null,
      facts: [...d.querySelectorAll('.drawer__facts dd')].map((el) => el.textContent.trim()),
      pressed: [...document.querySelectorAll('.spine')].filter((el) => el.getAttribute('aria-pressed') === 'true').map((el) => el.querySelector('.spine__code')?.textContent),
    });
  })()`)
  console.log('点 IPT 之后:', afterClick)

  // ---- 2. 再点一次应该插回去，并且把抽屉收回提示态 ----
  await evaluate(`(() => {
    const spine = [...document.querySelectorAll('.spine')].find((el) => el.querySelector('.spine__code')?.textContent === 'IPT');
    spine.click();
    return true;
  })()`)
  await new Promise((r) => setTimeout(r, 400))
  console.log('再点一次:', await evaluate(`JSON.stringify({
    hint: document.querySelector('.drawer__hint')?.textContent ?? null,
    pressed: [...document.querySelectorAll('.spine')].filter((el) => el.getAttribute('aria-pressed') === 'true').length,
  })`))

  // ---- 3. 键盘：从第一根书脊按 ArrowDown / End / Escape ----
  //
  // 每条按键都发给 document.activeElement——真实浏览器里拿到按键的是**当前聚焦的**
  // 那根书脊，而 @keydown 是逐脊绑定的（处理器带着自己那份 tierIndex/index）。
  // 全发给同一根，测出来的只是那一根的转移，等于没测。
  const kb = await evaluate(`(() => {
    const active = () => document.activeElement?.querySelector?.('.spine__code')?.textContent ?? document.activeElement?.tagName;
    const fire = (key) => document.activeElement.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
    const pressed = () => [...document.querySelectorAll('.spine')].filter((el) => el.getAttribute('aria-pressed') === 'true').map((el) => el.querySelector('.spine__code')?.textContent).join(',') || 'none';
    const trail = [];
    document.querySelector('.spine').focus();
    trail.push(active());
    fire('ArrowRight'); trail.push(active());   // 高级 SA -> SAD
    fire('ArrowDown');  trail.push(active());   // 落进中级，列号保持 -> NE
    fire('ArrowRight'); trail.push(active());   // 中级 NE -> ST
    fire('End');        trail.push(active());   // 中级最后一本 -> MAD
    fire('ArrowUp');    trail.push(active());   // 回到高级，列号被夹到最后一本 -> SPM
    fire('Home');       trail.push(active());   // 高级第一本 -> SA
    return trail.join(' -> ');
  })()`)
  console.log('键盘轨迹:', kb)

  // ---- 4. Esc 把书插回去，且焦点留在架上 ----
  // 每一步之间必须等一次宏任务：Vue 在微任务里刷 DOM，
  // 同一个 tick 里读 aria-pressed 读到的永远是上一步的状态。
  await evaluate(`document.activeElement.click()`)
  await new Promise((r) => setTimeout(r, 300))
  const pressedAfterClick = await evaluate(`JSON.stringify({
    pressed: [...document.querySelectorAll('.spine')].filter((el) => el.getAttribute('aria-pressed') === 'true').map((el) => el.querySelector('.spine__code')?.textContent),
    drawerName: document.querySelector('.drawer__name')?.textContent ?? null,
    focusOnSpine: document.activeElement?.classList.contains('spine') ?? false,
  })`)
  await evaluate(`document.activeElement.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }))`)
  await new Promise((r) => setTimeout(r, 300))
  const pressedAfterEsc = await evaluate(`JSON.stringify({
    pressedCount: [...document.querySelectorAll('.spine')].filter((el) => el.getAttribute('aria-pressed') === 'true').length,
    hintBack: Boolean(document.querySelector('.drawer__hint')),
    focusStillOnSpine: document.activeElement?.classList.contains('spine') ?? false,
  })`)
  console.log('按下后:', pressedAfterClick)
  console.log('Esc 后:', pressedAfterEsc)

  ws.close()
}

main().catch((e) => { console.error('FAILED', e.message); process.exit(1) })
