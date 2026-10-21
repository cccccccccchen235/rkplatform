// WCAG 对比度实算表。取代上一轮那条被中止的 `node -e` 一次性命令——
// 那个数要写进 docs/18，必须有脚本留痕、能被下一个人复跑。
//
// 为什么单独一件事：主色这一个决定（docs/19 第 2 节（2））要拿数字裁决，
// 而数字涉及 Element Plus 的派生色档（light-3 / dark-2），不是只算主色自己。
//
// Element Plus 的派生规则（theme-chalk/src/common/var.scss）：
//   light-i = mix(白, 主色, i*10%)     ← 权重给白
//   dark-i  = mix(黑, 主色, i*10%)     ← 权重给黑
// 所以 hover 用的 light-3 = 30% 白 + 70% 主色。
// 这条规则的正确性由 `--el-color-primary-light-3` 在产物 CSS 里的实际值交叉验证（见文件末尾）。

function srgbToLinear(v) {
  const c = v / 255
  return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4)
}
function luminance([r, g, b]) {
  return 0.2126 * srgbToLinear(r) + 0.7152 * srgbToLinear(g) + 0.0722 * srgbToLinear(b)
}
function ratio(a, b) {
  const la = luminance(a)
  const lb = luminance(b)
  const hi = Math.max(la, lb)
  const lo = Math.min(la, lb)
  return (hi + 0.05) / (lo + 0.05)
}
function hex(h) {
  const s = h.replace('#', '')
  return [0, 2, 4].map((i) => parseInt(s.slice(i, i + 2), 16))
}
function toHex([r, g, b]) {
  return '#' + [r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('')
}
// w = 第一个颜色的权重
function mix(a, b, w) {
  return a.map((v, i) => Math.round(v * w + b[i] * (1 - w)))
}

const WHITE = hex('#ffffff')
const PAGE = hex('#f5f7fa')
const BLACK = hex('#000000')

const RED = hex('#f56c6c') // 用于标出不过线的行

const fmt = (n) => n.toFixed(2)
const mark = (n, floor) => (n >= floor ? '  ok ' : n >= floor - 0.4 ? ' ~    ' : ' FAIL')

// 表格一列：一个色在某底色上的对比度，并对照该用途的下限
function row(label, color, bg, floor, bgName) {
  const r = ratio(hex(color), bg)
  console.log(
    `  ${label.padEnd(34)} ${color} × ${bgName.padEnd(8)} = ${fmt(r).padStart(6)}:1  ${mark(r, floor)} (需 ≥${floor})`,
  )
  return r
}

const CANDIDATES = {
  'A · 文档钉死值 $rk-primary': '#409eff',
  'B · 建议新主色': '#2a72c2',
  '（曾考虑，未采纳）': '#2f79c4',
}

console.log('='.repeat(78))
console.log('一、主色候选：作为「白底文字 / 细线」能否达标（craft floor: 文字 ≥4.5:1、图形 ≥3:1）')
console.log('='.repeat(78))
for (const [name, c] of Object.entries(CANDIDATES)) {
  console.log(`\n${name}  ${c}`)
  row('白底文字对比度', c, WHITE, 4.5, '白底')
  row('页面底文字对比度', c, PAGE, 4.5, '页面底')
}

console.log('\n' + '='.repeat(78))
console.log('二、Element Plus 派生色档（按钮填充面）：白字压在各档上的对比度')
console.log('   hover 底默认是 light-3（30% 白），active 是 dark-2（20% 黑）')
console.log('='.repeat(78))

for (const base of ['#409eff', '#2a72c2']) {
  const b = hex(base)
  const tiers = {
    base: b,
    'light-3（hover 默认）': mix(WHITE, b, 0.3),
    'light-5（disabled 常取）': mix(WHITE, b, 0.5),
    'light-9（plain 底）': mix(WHITE, b, 0.9),
    'dark-2（active 默认）': mix(BLACK, b, 0.2),
  }
  console.log(`\n主色 ${base}`)
  for (const [name, rgb] of Object.entries(tiers)) {
    const r = ratio(WHITE, rgb)
    console.log(
      `  ${name.padEnd(26)} ${toHex(rgb)}  白字 = ${fmt(r).padStart(6)}:1  ${mark(r, 4.5)}`,
    )
  }
}

console.log('\n' + '='.repeat(78))
console.log('三、若 B 落地，作为「白底文字」的复核（应与 $rk-primary-ink 一致或更好）')
console.log('='.repeat(78))
row('$rk-primary-ink 现值', '#2a72c2', WHITE, 4.5, '白底')
row('$rk-primary-ink 现值', '#2a72c2', PAGE, 4.5, '页面底')

console.log('\n' + '='.repeat(78))
console.log('四、交叉验证：产物 CSS 里的 --el-color-primary-light-3 / dark-2 实际值')
console.log('   （构建后从 dist/assets/vendor-element-*.css 读出，应与上表第二列一致）')
console.log('='.repeat(78))
console.log('  命令： grep -ho "el-color-primary-\\(light-3\\|dark-2\\):#[0-9a-f]*" web/dist/assets/vendor-element-*.css')

// ============================================================================
// 五、语义色三档（2026-10-02 新增）
//
// 背景：/subjects/999 的逐页审计实测到 4 处不达标，全在 Element Plus 的语义色上——
//   rgb(245,108,108) 的 el-alert__title 在白底 2.85:1、在自家浅底 #fef0f0 上 2.61:1。
// 那不是某一个页面的问题：07 §7.9 照抄的四个 Element Plus 默认值都过不了本项目
// 自己定的 craft floor，而它们同时扮演**两种角色**：
//   ① 文字：alert 标题、el-tag 的字、el-message 的字
//   ② 填充：el-button type="danger" 的实心底、错误态的边框
// 所以判据必须两边都算。对比度是对称的，于是两个方向是同一个不等式：
//   1.05 / (L + 0.05) ≥ 4.5  →  L ≤ 0.1833
// 而文字压在自家 -light-9 浅底（L≈0.85）上更严：L ≤ 0.159。
// 现值按 L ≤ 0.13 取，给下一档微调留了余量（与 $rk-primary 取 #2a72c2 同一理由）。
//
// 这一节**带断言**：任何人把语义色改回浅色，本脚本会以非 0 退出码失败。
// 旧值那三行只印不判——它们是"为什么必须改"的取证，不是期望。
// ============================================================================

const SEMANTIC = [
  { name: 'danger（el-button / el-tag 取它）', doc: '#f56c6c', now: '#b03030' },
  { name: 'error（el-alert / el-message 取它，与 danger 同值）', doc: '#f56c6c', now: '#b03030' },
  { name: 'warning', doc: '#e6a23c', now: '#8a5a10' },
  { name: 'success', doc: '#67c23a', now: '#2a6e2e' },
  { name: 'info', doc: '#909399', now: '#606469' },
]

/** 一个语义色在两种角色上的三项判据。`assert` 为真时把失败计入退出码。 */
function semanticRow(label, c, assert) {
  const base = hex(c)
  const tint = toHex(mix(WHITE, base, 0.9)) // = Element Plus 的 -light-9 派生档
  const checks = [
    ['文字 on 自家浅底(-light-9)', c, tint, 4.5],
    ['文字 on 白底             ', c, '#ffffff', 4.5],
    ['白字 on 填充面           ', '#ffffff', c, 4.5],
  ]
  let bad = 0
  for (const [what, fg, bg, floor] of checks) {
    const r = ratio(hex(fg), hex(bg))
    const ok = r >= floor
    if (!ok) bad += 1
    console.log(`  ${label.padEnd(5)} ${what} ${fg} × ${bg} = ${fmt(r).padStart(6)}:1 ${ok ? '  ok ' : ' FAIL'}`)
  }
  return assert ? bad : 0
}

console.log('\n' + '='.repeat(78))
console.log('五、语义色三档：文字与填充两种角色必须同时达标（floor 4.5:1）')
console.log('='.repeat(78))
let semanticFailures = 0
for (const s of SEMANTIC) {
  console.log(`\n${s.name}`)
  // 旧值只印不判：它们是"为什么必须改"的取证，不是期望值。
  semanticRow('旧值', s.doc, false)
  semanticFailures += semanticRow('现值', s.now, true)
}

console.log('\n' + '='.repeat(78))
console.log('六、交叉验证：产物 CSS 里的语义色实际值')
console.log('   --el-color-* 由 element-override.scss 的 :root 覆盖，落在**本项目**的 CSS 里')
console.log('   （不是 vendor-element，那里仍是 Element Plus 的默认值，被后加载的这份盖掉）')
console.log('='.repeat(78))
console.log('  命令： grep -ho "el-color-\\(danger\\|error\\|warning\\|success\\|info\\): [^;]*" web/dist/assets/index-*.css | sort -u')
console.log('  期望： danger 与 error 同值（#b03030），五个值都与上面「现值」一致。')

if (semanticFailures > 0) {
  console.log(`\n语义色断言失败 ${semanticFailures} 项。`)
  process.exitCode = 1
}
