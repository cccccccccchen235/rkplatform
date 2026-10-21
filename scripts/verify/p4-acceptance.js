// P4 练习闭环 · 验收脚本（docs/10-roadmap.md §10.6 的 10 条）
//
// 口径（本项目的硬规矩，见 docs/25 §7）：**判定依据是业务码 `code`，不是"调用没抛异常"**。
// 每个断言都同时检查 code 与具体字段，避免"跑通了但结果是错的"。
//
// 前置：
//   1. 后端在 8080 运行，MySQL/Redis 可用；题库已导入（subject 6 有 250 题）。
//   2. Redis 里 `rk:limit:feedback:*` 与 `rk:replay:answer:*` 对本脚本用的测试用户是干净的
//      —— 反馈限流是"同题同用户 24h 内 3 次"，上一轮的计数会让本轮第 1 次就拿到 1002。
//      运行前清一次：  redis-cli -a <pw> --scan --pattern 'rk:limit:feedback:*' | ... DEL
//      （脚本不代劳：Redis 口令属于环境，不该写进验证仪器。）
//   3. 测试用户存在且可用（默认 negctl_81217）。
//
// 已知副作用（会留在库里，可接受）：
//   · 有答题记录、错题本行、收藏行、反馈行；
//   · 顺序练习的进度游标被推进。
//
// 运行：node scripts/verify/p4-acceptance.js
// 退出码：0 = 全部通过；1 = 有失败。

const BASE = process.env.API_BASE || 'http://127.0.0.1:8080/api/v1'
const USERNAME = process.env.RK_TEST_USER || 'negctl_81217'
const PASSWORD = process.env.RK_TEST_PASS || 'NegCtl@2026'
const SUBJECT_ID = process.env.RK_TEST_SUBJECT || '6'

/** 重放窗口是 5 秒，等待必须严格大于它，否则会把"去重"误判成"错题本没更新"。 */
const REPLAY_WAIT_MS = 6000

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

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

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

/** 在大纲树里找第一个"有题"的叶子章节。 */
function firstLeafWithQuestions(nodes, found = { id: null }) {
  for (const node of nodes) {
    if (node.children && node.children.length > 0) {
      firstLeafWithQuestions(node.children, found)
    } else if (found.id === null && node.questionCount > 0) {
      found.id = node.id
    }
    if (found.id !== null) return found
  }
  return found
}

function firstWrongOption(options, correct) {
  const wrong = options.map((o) => o.key).filter((k) => k !== correct)
  return wrong.length > 0 ? wrong[0] : null
}

async function main() {
  console.log(`\n== P4 验收 · ${BASE} · subject=${SUBJECT_ID} user=${USERNAME} ==\n`)

  // ---------- 登录 ----------
  const login = await api('/auth/login', {
    method: 'POST',
    body: { username: USERNAME, password: PASSWORD },
  })
  if (login.json?.code !== 0) {
    console.error(`登录失败，无法继续：status=${login.status} body=${JSON.stringify(login.json)}`)
    process.exit(1)
  }
  const token = login.json.data.token
  console.log(`登录成功（token 已取得）\n`)

  const outline = await api(`/subjects/${SUBJECT_ID}/outline`)
  const leaf = firstLeafWithQuestions(outline.json.data)
  const leafId = leaf.id

  // ---------- 验收 1：五种模式均可用 ----------
  console.log('[1] 五种练习模式均可用')
  const chapterPaper = await api('/practice/papers', {
    method: 'POST',
    token,
    body: { subjectId: SUBJECT_ID, mode: 'CHAPTER', refId: leafId, count: 10 },
  })
  assert('CHAPTER 组卷 code=0', chapterPaper.json?.code === 0, JSON.stringify(chapterPaper.json))
  const questions = chapterPaper.json?.data?.questions ?? []
  assert('CHAPTER 取到题目', questions.length > 0, `total=${chapterPaper.json?.data?.total}`)

  // 后续几个用例都要"独立的一道题"（重放窗口按 (user, question) 计 5 秒，
  // 同一题在 5 秒内二次上报会被合规地丢掉，复用它会把产品行为误判成缺陷）。
  // 章节池只有 5 题，不够分，所以另取一份 50 题的顺序卷作为题库池。
  const poolPaper = await api('/practice/papers', {
    method: 'POST',
    token,
    body: { subjectId: SUBJECT_ID, mode: 'SEQUENCE', fromSequence: 0, count: 50 },
  })
  const pool = poolPaper.json?.data?.questions ?? []
  assert('题库池取到 >= 10 道题（供各用例独立取题）', pool.length >= 10, `pool=${pool.length}`)

  const kpId = questions[0]?.knowledgePoints?.[0]?.id
  const modes = [
    ['KP', { refId: kpId }],
    ['SEQUENCE', { fromSequence: 0 }],
    ['FAVORITE', {}],
    ['WRONG_REDO', {}],
    ['SAMPLE', { count: 5 }],
  ]
  for (const [mode, extra] of modes) {
    if (mode === 'KP' && !kpId) {
      assert('KP 组卷 code=0（跳过：样例题未挂知识点）', false, '取不到 kpId')
      continue
    }
    const res = await api('/practice/papers', {
      method: 'POST',
      token,
      body: { subjectId: SUBJECT_ID, mode, count: 5, ...extra },
    })
    assert(`${mode} 组卷 code=0`, res.json?.code === 0, JSON.stringify(res.json))
  }

  // ---------- 验收 2：即时对错与解析（答案随卷下发） ----------
  console.log('\n[2] 取卷即带答案与解析（前端可即时判分）')
  const q0 = questions[0]
  assert('题目含 correctAnswer', typeof q0?.correctAnswer === 'string' && q0.correctAnswer.length > 0)
  assert('题目含 options（客观题）', Array.isArray(q0?.options) && q0.options.length >= 2)
  assert('题目含 isFavorited 且为布尔（非 null）', typeof q0?.isFavorited === 'boolean')
  assert('题目含 outlinePath 章节路径', Array.isArray(q0?.outlinePath) && q0.outlinePath.length > 0)
  assert('题目含 knowledgePoints 数组', Array.isArray(q0?.knowledgePoints))

  // ---------- 验收 3/4：服务端复核（AC-3） + 记录落库 ----------
  console.log('\n[3] 服务端复核：客户端谎报 isCorrect=true 也不生效')
  const target = pool[0]
  const wrongKey = firstWrongOption(target.options, target.correctAnswer)
  // 关键：请求体里多塞一个 isCorrect: true —— 契约里没有这个字段，看服务端会不会被它带偏
  const tampered = await api('/practice/records', {
    method: 'POST',
    token,
    body: {
      subjectId: SUBJECT_ID,
      source: 'PRACTICE_CHAPTER',
      items: [
        {
          questionId: target.id,
          userAnswer: wrongKey,
          durationMs: 1200,
          isCorrect: true, // 谎言
        },
      ],
    },
  })
  assert('上报 code=0', tampered.json?.code === 0, JSON.stringify(tampered.json))
  assert('accepted=1', tampered.json?.data?.accepted === 1, JSON.stringify(tampered.json?.data))
  assert(
    '服务端复核后 correctCount=0（谎报无效）',
    tampered.json?.data?.correctCount === 0,
    `correctCount=${tampered.json?.data?.correctCount}`,
  )

  // 答错必然进错题本 —— 这是"库里 is_correct 落的是 false"的可观察推论
  const wrongList = await api(`/wrong-questions?subjectId=${SUBJECT_ID}&status=ACTIVE&pageSize=100`, { token })
  const inWrongBook = (wrongList.json?.data?.list ?? []).find((r) => r.questionId === target.id)
  assert('答错的题进入错题本（ACTIVE）', Boolean(inWrongBook), JSON.stringify(wrongList.json?.data))
  assert('wrongCount >= 1', (inWrongBook?.wrongCount ?? 0) >= 1, `wrongCount=${inWrongBook?.wrongCount}`)
  assert('错题行含章节路径 outlinePath', Array.isArray(inWrongBook?.outlinePath))

  console.log('\n[4] 答题记录落库：行数与实际上报数一致')
  const freshItems = pool.slice(1, 4).map((q) => ({
    questionId: q.id,
    userAnswer: q.correctAnswer,
    durationMs: 800,
  }))
  const batch = await api('/practice/records', {
    method: 'POST',
    token,
    body: { subjectId: SUBJECT_ID, source: 'PRACTICE_CHAPTER', items: freshItems },
  })
  assert('3 条全部 accepted', batch.json?.data?.accepted === 3, JSON.stringify(batch.json?.data))
  assert('correctCount=3（都答对）', batch.json?.data?.correctCount === 3, JSON.stringify(batch.json?.data))

  // ---------- 验收 5：连续答对 2 次转 MASTERED ----------
  console.log('\n[5] 错题本：同一题连续答对 2 次 → MASTERED')
  const redoBody = (answer) => ({
    subjectId: SUBJECT_ID,
    source: 'WRONG_REDO',
    items: [{ questionId: target.id, userAnswer: answer, durationMs: 500 }],
  })
  // [3] 刚用同一道题上报过（故意答错），必须越过 5 秒重放窗口，本次答对才不会被当成重放
  await sleep(REPLAY_WAIT_MS)
  const first = await api('/practice/records', { method: 'POST', token, body: redoBody(target.correctAnswer) })
  assert('第 1 次答对 accepted=1', first.json?.data?.accepted === 1, JSON.stringify(first.json?.data))
  let active = await api(`/wrong-questions?subjectId=${SUBJECT_ID}&status=ACTIVE&pageSize=100`, { token })
  assert(
    '第 1 次答对后仍在 ACTIVE（阈值 2 未到）',
    Boolean((active.json?.data?.list ?? []).find((r) => r.questionId === target.id)),
  )

  await sleep(REPLAY_WAIT_MS) // 必须越过 5 秒重放窗口，否则第 2 次会被当成重放忽略

  const second = await api('/practice/records', { method: 'POST', token, body: redoBody(target.correctAnswer) })
  assert('第 2 次答对 accepted=1（未被重放闸吞掉）', second.json?.data?.accepted === 1, JSON.stringify(second.json?.data))
  const mastered = await api(`/wrong-questions?subjectId=${SUBJECT_ID}&status=MASTERED&pageSize=100`, { token })
  assert(
    '第 2 次答对后状态变为 MASTERED',
    Boolean((mastered.json?.data?.list ?? []).find((r) => r.questionId === target.id)),
    JSON.stringify(mastered.json?.data?.list?.map((r) => r.questionId)),
  )

  // ---------- 验收 6：未作答不入错题本 ----------
  console.log('\n[6] 未作答的题不入错题本')
  const blankTarget = pool[4]
  const blankReport = await api('/practice/records', {
    method: 'POST',
    token,
    body: {
      subjectId: SUBJECT_ID,
      source: 'PRACTICE_CHAPTER',
      items: [{ questionId: blankTarget.id, userAnswer: '', durationMs: 300 }],
    },
  })
  assert('空作答上报 accepted=1（流水仍记录）', blankReport.json?.data?.accepted === 1, JSON.stringify(blankReport.json?.data))
  const afterBlank = await api(`/wrong-questions?subjectId=${SUBJECT_ID}&status=ACTIVE&pageSize=100`, { token })
  assert(
    '空作答的题不在错题本里',
    !(afterBlank.json?.data?.list ?? []).some((r) => r.questionId === blankTarget.id),
  )

  // ---------- 验收 7：5 秒内重复提交只生效一次 ----------
  console.log('\n[7] 5 秒内重复提交只生效一次（幂等/重放）')
  const replayTarget = pool[5]
  const replayBody = {
    subjectId: SUBJECT_ID,
    source: 'PRACTICE_CHAPTER',
    items: [{ questionId: replayTarget.id, userAnswer: replayTarget.correctAnswer, durationMs: 400 }],
  }
  const r1 = await api('/practice/records', { method: 'POST', token, body: replayBody })
  const r2 = await api('/practice/records', { method: 'POST', token, body: replayBody })
  assert('第 1 次 accepted=1', r1.json?.data?.accepted === 1, JSON.stringify(r1.json?.data))
  assert('立刻重发 accepted=0（被识别为重放）', r2.json?.data?.accepted === 0, JSON.stringify(r2.json?.data))

  // ---------- 验收 8：顺序练习"继续上次" ----------
  console.log('\n[8] 顺序练习：进度可保存、可续做')
  const cursor = { subjectId: SUBJECT_ID, mode: 'SEQUENCE', lastSequence: 42, lastQuestionId: null }
  const saveProgress = await api('/practice/progress', { method: 'PUT', token, body: cursor })
  assert('PUT progress code=0', saveProgress.json?.code === 0, JSON.stringify(saveProgress.json))
  const readProgress = await api(`/practice/progress?subjectId=${SUBJECT_ID}&mode=SEQUENCE`, { token })
  const seqRow = (readProgress.json?.data ?? [])[0]
  assert('GET progress 回读 lastSequence=42', seqRow?.lastSequence === 42, JSON.stringify(readProgress.json?.data))
  assert('GET progress 含 totalCount（候选池分母）', typeof seqRow?.totalCount === 'number', JSON.stringify(seqRow))

  const seqPaper = await api('/practice/papers', {
    method: 'POST',
    token,
    body: { subjectId: SUBJECT_ID, mode: 'SEQUENCE', fromSequence: 42, count: 5 },
  })
  const seqQuestions = seqPaper.json?.data?.questions ?? []
  assert('SEQUENCE 从游标之后取题', seqQuestions.length > 0, JSON.stringify(seqPaper.json?.data))
  assert(
    '所有题 id 严格大于 fromSequence=42',
    seqQuestions.every((q) => Number(q.id) > 42),
    JSON.stringify(seqQuestions.map((q) => q.id)),
  )
  assert('SEQUENCE 返回 nextSequence', typeof seqPaper.json?.data?.nextSequence === 'number')

  // ---------- 验收 9：游客样例题边界 ----------
  console.log('\n[9] 游客：SAMPLE ≤5 可用，超量 1001，非 SAMPLE 2005')
  const guestOk = await api('/practice/papers', {
    method: 'POST',
    body: { subjectId: SUBJECT_ID, mode: 'SAMPLE', count: 5 },
  })
  assert('游客 SAMPLE count=5 → code=0', guestOk.json?.code === 0, JSON.stringify(guestOk.json))
  assert('游客取到题目（≤5）', (guestOk.json?.data?.total ?? 0) <= 5, `total=${guestOk.json?.data?.total}`)

  const guestOver = await api('/practice/papers', {
    method: 'POST',
    body: { subjectId: SUBJECT_ID, mode: 'SAMPLE', count: 10 },
  })
  assert('游客 SAMPLE count=10 → 1001', guestOver.json?.code === 1001, JSON.stringify(guestOver.json))

  const guestFull = await api('/practice/papers', {
    method: 'POST',
    body: { subjectId: SUBJECT_ID, mode: 'FULL', count: 5 },
  })
  assert('游客 mode=FULL → 2005', guestFull.json?.code === 2005, JSON.stringify(guestFull.json))

  // ---------- 验收 10：反馈限流 ----------
  console.log('\n[10] 题目反馈：24h 内同题同用户第 4 次返回 1002')
  const feedbackTarget = pool[6]
  const codes = []
  for (let i = 1; i <= 4; i += 1) {
    const res = await api(`/questions/${feedbackTarget.id}/feedback`, {
      method: 'POST',
      token,
      body: { type: 'TYPO', content: `验收脚本第 ${i} 次提交（内容需 5~1000 字）` },
    })
    codes.push(res.json?.code)
  }
  assert('前 3 次 code=0', codes.slice(0, 3).every((c) => c === 0), JSON.stringify(codes))
  assert('第 4 次 code=1002', codes[3] === 1002, JSON.stringify(codes))

  // ---------- 附加：收藏 ----------
  console.log('\n[+] 收藏：幂等增删')
  const favTarget = pool[7].id
  const favAdd = await api('/favorites', { method: 'POST', token, body: { questionId: String(favTarget) } })
  assert('收藏 code=0', favAdd.json?.code === 0, JSON.stringify(favAdd.json))
  const favAddAgain = await api('/favorites', { method: 'POST', token, body: { questionId: String(favTarget) } })
  assert('重复收藏仍 code=0（幂等）', favAddAgain.json?.code === 0, JSON.stringify(favAddAgain.json))
  const favList = await api(`/favorites?subjectId=${SUBJECT_ID}&pageSize=100`, { token })
  assert(
    '收藏列表包含该题',
    (favList.json?.data?.list ?? []).some((q) => q.id === String(favTarget)),
    JSON.stringify(favList.json?.data?.total),
  )
  const favRemove = await api(`/favorites/${favTarget}`, { method: 'DELETE', token })
  assert('取消收藏 code=0', favRemove.json?.code === 0, JSON.stringify(favRemove.json))
  const favRemoveAgain = await api(`/favorites/${favTarget}`, { method: 'DELETE', token })
  assert('重复取消仍 code=0（幂等）', favRemoveAgain.json?.code === 0, JSON.stringify(favRemoveAgain.json))

  // ---------- 汇总 ----------
  console.log(`\n== 合计：${passed} 通过 / ${failed} 失败 ==\n`)
  process.exit(failed === 0 ? 0 : 1)
}

main().catch((error) => {
  console.error('脚本异常终止：', error)
  process.exit(1)
})