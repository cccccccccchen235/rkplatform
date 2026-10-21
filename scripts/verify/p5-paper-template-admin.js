// P5 管理端卷面模板配置 · 验收脚本（docs/10-roadmap.md §10.7 的前两条）
//
// 覆盖：
//   ① 管理端可为科目配置卷面模板；autoScoredMaxScore / totalMaxScore 与分节分值之和不符时保存失败；
//   ② 信息处理技术员（IPT）的「应用技术」配置为 NOT_SUPPORTED 时，其分值不计入 totalMaxScore；
//   ③ 题量不足只返回 insufficientQuestions 警告，不阻断保存；
//   ④ GET 的响应可直接作为 PUT 请求体回填（整体覆盖式写入是幂等的，子行不累积）；
//   ⑤ 边界：科目不存在返回 3001；尚未配置模板返回 4001（用 RK_TEMPLATELESS_SUBJECT 指定）。
//
// 口径（本项目的硬规矩，见 docs/25 §7）：**判定依据是业务码 `code`，不是"调用没抛异常"**。
// 否定用例（应失败的保存）必须先证明它会失败，再证明合规的保存会成功。
//
// 前置：
//   1. 后端在 8080 运行，MySQL/Redis 可用；
//   2. 管理员口令从环境变量注入，**不写进本文件**（AC-1：deploy/.env 是凭据的唯一落点）：
//        RK_TEST_ADMIN_USER / RK_TEST_ADMIN_PASSWORD
//      PowerShell 里可从 deploy/.env 逐行注入（文件不是 shell 脚本，别直接 source）：
//        Get-Content deploy/.env | % { if ($_ -match '=') { $kv=$_ -split '=',2;
//          Set-Item "Env:$($kv[0].Trim())" $kv[1].Trim() } }
//
// 副作用（脚本自己负责还原）：
//   · 会临时改 SAD（2）与 IPT（17）的模板，**结束时按运行前快照原样 PUT 回**；
//   · 只写 sys_operation_log（审计），不产生业务流水。
//
// 运行：node scripts/verify/p5-paper-template-admin.js
// 退出码：0 = 全部通过；1 = 有失败（或未能还原）。

const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api/v1'
const ADMIN_USER = process.env.RK_TEST_ADMIN_USER
const ADMIN_PASSWORD = process.env.RK_TEST_ADMIN_PASSWORD

/** 高级科目（两场次），用作主用例。 */
const SENIOR_SUBJECT = process.env.RK_SENIOR_SUBJECT || '2'
/** 信息处理技术员（应用技术节 NOT_SUPPORTED）。 */
const IPT_SUBJECT = process.env.RK_IPT_SUBJECT || '17'
/** 可选：一个当前没有卷面模板的科目 ID，用来验证 4001；未设置则跳过。 */
const TEMPLATELESS_SUBJECT = process.env.RK_TEMPLATELESS_SUBJECT

let passed = 0
let failed = 0

function assert(name, condition, detail = '') {
  if (condition) {
    passed += 1
    console.log(`  ✓ ${name}`)
  } else {
    failed += 1
    console.log(`  ✗ ${name}${detail ? `  →  ${detail}` : ''}`)
  }
}

const clone = (value) => JSON.parse(JSON.stringify(value))

async function api(path, { method = 'GET', body, token } = {}) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  const text = await res.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    json = { _unparsed: text }
  }
  return { status: res.status, json }
}

/** 平铺所有分节，返回 [sittingNo, sectionNo] 序列，用于断言序号连续。 */
function sectionSequence(detail) {
  const seq = []
  for (const sitting of detail.sittings ?? []) {
    for (const section of sitting.sections ?? []) {
      seq.push({ sittingNo: sitting.sittingNo, sectionNo: section.sectionNo })
    }
  }
  return seq
}

/** 按分节重算满分，与 05 §5.10 的口径一致（NOT_SUPPORTED 不计入任何满分）。 */
function recompute(detail) {
  let total = 0
  let auto = 0
  for (const sitting of detail.sittings ?? []) {
    for (const section of sitting.sections ?? []) {
      if (section.supportStatus !== 'SUPPORTED') continue
      const score = Number(section.perQuestionScore) * section.questionCount
      total += score
      if (section.sectionType === 'OBJECTIVE') auto += score
    }
  }
  return { total, auto }
}

async function getTemplate(subjectId, token) {
  const res = await api(`/admin/paper-templates/${subjectId}`, { token })
  return res.json
}

async function putTemplate(subjectId, payload, token) {
  const res = await api(`/admin/paper-templates/${subjectId}`, { method: 'PUT', token, body: payload })
  return res.json
}

async function main() {
  if (!ADMIN_USER || !ADMIN_PASSWORD) {
    console.error('缺少 RK_TEST_ADMIN_USER / RK_TEST_ADMIN_PASSWORD（见文件头的前置说明）')
    process.exit(1)
  }
  console.log(`\n== P5 卷面模板配置验收 · ${BASE} ==\n`)

  const login = await api('/auth/login', {
    method: 'POST',
    body: { username: ADMIN_USER, password: ADMIN_PASSWORD },
  })
  if (login.json?.code !== 0) {
    console.error(`管理员登录失败，无法继续：status=${login.status} body=${JSON.stringify(login.json)}`)
    process.exit(1)
  }
  const token = login.json.data.token
  console.log('管理员登录成功（token 已取得）\n')

  const snapshots = new Map()
  for (const id of [SENIOR_SUBJECT, IPT_SUBJECT]) {
    const res = await getTemplate(id, token)
    if (res?.code !== 0) {
      console.error(`读取科目 ${id} 的模板失败，无法建立还原快照：${JSON.stringify(res)}`)
      process.exit(1)
    }
    snapshots.set(id, clone(res.data))
  }
  console.log('已保存运行前快照（结束时还原）\n')

  let restoreFailed = false
  try {
    // ---------- 1. 读取 ----------
    console.log('[1] GET 读取模板（含场次与分节，全字段）')
    const senior = snapshots.get(SENIOR_SUBJECT)
    assert('code = 0', true)
    assert('下发 sittings 且非空', Array.isArray(senior.sittings) && senior.sittings.length > 0)
    assert('模板级 totalDuration = Σ 场次时长',
      (senior.sittings ?? []).reduce((sum, s) => sum + s.totalDuration, 0) === senior.totalDuration,
      `payload=${JSON.stringify({ totalDuration: senior.totalDuration })}`)
    assert('分节序号跨场次全局连续',
      sectionSequence(senior).every((row, index) => row.sectionNo === index + 1),
      JSON.stringify(sectionSequence(senior)))
    assert('下发判分/计时细节字段（perQuestionScore / durationMin / allowEarlySubmit）',
      senior.sittings.every((s) => s.sections.every((c) =>
        c.perQuestionScore !== undefined && c.durationMin !== undefined && c.allowEarlySubmit !== undefined)))
    assert('读取出的满分与分节重算口径一致',
      Math.abs(recompute(senior).total - senior.totalMaxScore) < 1e-9
        && Math.abs(recompute(senior).auto - senior.autoScoredMaxScore) < 1e-9)

    // ---------- 2. 否定用例：满分口径不符必须保存失败 ----------
    console.log('\n[2] autoScoredMaxScore 与客观题分节分值之和不符 → 1001')
    {
      const payload = clone(senior)
      payload.autoScoredMaxScore += 1
      const res = await putTemplate(SENIOR_SUBJECT, payload, token)
      assert('code = 1001', res?.code === 1001, JSON.stringify(res))
      assert('错误消息点明是 autoScoredMaxScore', String(res?.message ?? '').includes('autoScoredMaxScore'), res?.message)
    }

    console.log('\n[2b] 场次 totalDuration 与分节时长上下限不自洽 → 1001')
    {
      const payload = clone(senior)
      payload.sittings[0].totalDuration = 999
      const res = await putTemplate(SENIOR_SUBJECT, payload, token)
      assert('code = 1001', res?.code === 1001, JSON.stringify(res))
      assert('错误消息点明时长不自洽', String(res?.message ?? '').includes('时长不自洽'), res?.message)
    }

    console.log('\n[2c] 分节序号断号 → 1001')
    {
      const payload = clone(senior)
      payload.sittings[1].sections[0].sectionNo = 4
      const res = await putTemplate(SENIOR_SUBJECT, payload, token)
      assert('code = 1001', res?.code === 1001, JSON.stringify(res))
      assert('错误消息点明分节序号不连续', String(res?.message ?? '').includes('分节序号'), res?.message)
    }

    // ---------- 3. IPT：NOT_SUPPORTED 不计入任何满分口径 ----------
    console.log('\n[3] IPT「应用技术」NOT_SUPPORTED 不计入 totalMaxScore')
    const ipt = snapshots.get(IPT_SUBJECT)
    {
      // 3a 反证：把应用技术改成 SUPPORTED（5×15=75 分）却仍提交 totalMaxScore=75 → 必须 1001。
      // 如果服务端没把"分节分值之和"算进去，这一条会侥幸通过，所以它是"NOT_SUPPORTED 不计分"的漂移探针。
      const payload = clone(ipt)
      const target = payload.sittings[0].sections.find((c) => c.sectionNo === 2)
      target.supportStatus = 'SUPPORTED'
      target.questionCount = 5
      target.perQuestionScore = 15
      const res = await putTemplate(IPT_SUBJECT, payload, token)
      assert('3a 反证：SUPPORTED 后 totalMaxScore 少算 → 1001', res?.code === 1001, JSON.stringify(res))
    }
    {
      // 3b 正例：保持 NOT_SUPPORTED，totalMaxScore = 75（只含基础知识 75 分）。
      const res = await putTemplate(IPT_SUBJECT, clone(ipt), token)
      assert('3b code = 0', res?.code === 0, JSON.stringify(res))
      const after = await getTemplate(IPT_SUBJECT, token)
      assert('3b 回读 totalMaxScore = 75（NOT_SUPPORTED 的 75 分未计入）',
        after?.data?.totalMaxScore === 75, JSON.stringify(after?.data?.totalMaxScore))
      const notSupported = after?.data?.sittings
        ?.flatMap((s) => s.sections)
        ?.find((c) => c.sectionNo === 2)
      assert('3b 应用技术仍为 NOT_SUPPORTED 且分值为 0',
        notSupported?.supportStatus === 'NOT_SUPPORTED'
          && notSupported?.questionCount === 0
          && Number(notSupported?.perQuestionScore) === 0,
        JSON.stringify(notSupported))
    }

    // ---------- 4. 题量不足：警告但不阻断 ----------
    console.log('\n[4] questionCount 超过已发布单选题量 → code=0 + insufficientQuestions')
    {
      const payload = clone(senior)
      const s1 = payload.sittings[0].sections[0]
      s1.questionCount = 999 // 每题 1 分 → 999 分，同步调整满分口径避免撞上 1001
      payload.sittings[0].autoScoredMaxScore = 999
      payload.sittings[0].totalMaxScore = 999 + 75
      payload.autoScoredMaxScore = 999
      payload.totalMaxScore = payload.sittings.reduce((sum, s) => sum + s.totalMaxScore, 0)
      const res = await putTemplate(SENIOR_SUBJECT, payload, token)
      assert('code = 0（题量不足不阻断）', res?.code === 0, JSON.stringify(res))
      const warnings = res?.data?.insufficientQuestions ?? []
      const hit = warnings.find((w) => w.sectionNo === 1)
      assert('insufficientQuestions 命中分节 1', !!hit, JSON.stringify(warnings))
      assert('警告含 requiredCount=999 与 availableCount',
        hit?.requiredCount === 999 && Number.isInteger(hit?.availableCount), JSON.stringify(hit))
    }

    // ---------- 5. 覆盖式写入可回填、可重复（子行不累积） ----------
    console.log('\n[5] GET 响应回填 PUT：幂等且子行不累积')
    {
      const first = await putTemplate(SENIOR_SUBJECT, clone(senior), token)
      assert('回填原快照 code = 0', first?.code === 0, JSON.stringify(first))
      const after1 = await getTemplate(SENIOR_SUBJECT, token)
      assert('回读与原快照逐字节一致', JSON.stringify(after1.data) === JSON.stringify(senior),
        JSON.stringify({ before: senior, after: after1.data }))

      const second = await putTemplate(SENIOR_SUBJECT, clone(after1.data), token)
      assert('再次回填 code = 0', second?.code === 0, JSON.stringify(second))
      const after2 = await getTemplate(SENIOR_SUBJECT, token)
      assert('两次写入后场次/分节数量不变',
        JSON.stringify(after2.data) === JSON.stringify(senior),
        `sittings=${after2.data.sittings?.length}, sections=${sectionSequence(after2.data).length}`)
    }

    // ---------- 6. 边界 ----------
    console.log('\n[6] 边界')
    {
      const res = await api('/admin/paper-templates/999999', { token })
      assert('不存在的科目 → 3001', res.json?.code === 3001, JSON.stringify(res.json))
    }
    if (TEMPLATELESS_SUBJECT) {
      const res = await api(`/admin/paper-templates/${TEMPLATELESS_SUBJECT}`, { token })
      assert('尚未配置模板的科目 → 4001', res.json?.code === 4001, JSON.stringify(res.json))
    } else {
      console.log('  - 跳过 4001（未设置 RK_TEMPLATELESS_SUBJECT）')
    }
  } finally {
    console.log('\n[还原] 按运行前快照 PUT 回 SAD / IPT')
    for (const [id, snapshot] of snapshots) {
      const res = await putTemplate(id, clone(snapshot), token)
      const after = await getTemplate(id, token)
      const ok = res?.code === 0 && JSON.stringify(after.data) === JSON.stringify(snapshot)
      if (!ok) restoreFailed = true
      console.log(`  ${ok ? '✓' : '✗'} 科目 ${id} 已还原`)
    }
  }

  console.log(`\n== 结果：${passed} 通过 / ${failed} 失败 ==\n`)
  process.exit(failed === 0 && !restoreFailed ? 0 : 1)
}

main().catch((error) => {
  console.error('\n脚本异常：', error)
  process.exit(1)
})
