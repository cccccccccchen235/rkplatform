#!/usr/bin/env node
/**
 * 题目 JSONL 导入器 · 方案 B（最小导入器）
 *
 * 用法：
 *   node scripts/generate/import-questions.js             # dry-run，只统计不写库（默认）
 *   node scripts/generate/import-questions.js --execute   # 真的写库
 *
 * 与「完整 P3 流水线」的区别（这是有意偏离，不是遗漏）：
 *   · 不走审核工作台，题目**直接以 PUBLISHED 入库**；
 *   · 因此 `reviewed_by` / `reviewed_at` **留 NULL**，并在 `sys_operation_log`
 *     记一条「批量导入，未经逐题审核」——不伪造逐题裁决记录。
 *   后续 P3 补上审核台时，这批题用同一份 schema，不需要重导。
 *
 * 它**只写**这几张表：rk_generation_batch / rk_question / rk_question_option /
 * rk_knowledge_point / rk_question_kp / rk_subject(计数) / rk_outline_node(计数) /
 * sys_operation_log。
 * **不碰 sys_user，不删任何既有数据**；全部包在一个事务里，失败即回滚。
 *
 * 前置：先跑通 validate-questions.js（本脚本会自动调用它，不通过就拒绝执行）。
 */

const fs = require('fs')
const path = require('path')
const { execFileSync } = require('child_process')

const PROJECT = path.resolve(__dirname, '..', '..')
const DATA_DIR = path.join(PROJECT, 'data')
const ENV_FILE = path.join(PROJECT, 'deploy', '.env')
const OUT_SQL = path.join(PROJECT, '.verify-tmp', 'import-SD.sql')

const BATCH_NO = 'EX-SD-001'
const SUBJECT_ID = 6
const MODEL = 'external-ai (user-provided)'
const PROMPT_VERSION = 'question-spec-sd'
const OPERATOR_ID = 1 // admin

const EXECUTE = process.argv.includes('--execute')

// ---------- .env ----------
function readEnv() {
  const env = {}
  for (const line of fs.readFileSync(ENV_FILE, 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)$/)
    if (m) env[m[1]] = m[2].trim()
  }
  return env
}

// ---------- 只读查询：用 mysql CLI，避免引 npm 依赖 ----------
function mysqlQuery(env, sql) {
  const out = execFileSync(
    'mysql',
    ['-h', '127.0.0.1', '-u', 'root', env.MYSQL_DATABASE, '-B', '-N', '-e', sql],
    { encoding: 'utf8', env: { ...process.env, MYSQL_PWD: env.MYSQL_ROOT_PASSWORD } }
  )
  return out.trim()
}

// ---------- SQL 字符串转义 ----------
// 服务器的 sql_mode 不含 NO_BACKSLASH_ESCAPES，故反斜杠是转义符。
// 顺序要紧：先转义反斜杠，再转义单引号。
const q = (s) => "'" + String(s).replace(/\\/g, '\\\\').replace(/'/g, "\\'") + "'"

function buildSql(env, rows) {
  const L = []
  const outlineIds = [...new Set(rows.map((r) => r.outlineId))]

  L.push('-- 由 scripts/generate/import-questions.js 生成（方案 B）')
  L.push('-- 未经逐题审核，直接 PUBLISHED；reviewed_by 留 NULL，理由见 sys_operation_log')
  L.push('SET NAMES utf8mb4;')
  L.push('START TRANSACTION;')
  L.push('')

  // 批次
  L.push('INSERT INTO rk_generation_batch')
  L.push('  (batch_no, subject_id, outline_ids, question_type, target_count, model, prompt_version,')
  L.push('   status, produced_count, approved_count, rejected_count, operator_id, finished_at)')
  L.push(
    `VALUES (${q(BATCH_NO)}, ${SUBJECT_ID}, ${q(JSON.stringify(outlineIds))}, 'SINGLE_CHOICE', ` +
      `${rows.length}, ${q(MODEL)}, ${q(PROMPT_VERSION)}, 'COMPLETED', ${rows.length}, ${rows.length}, 0, ${OPERATOR_ID}, NOW());`
  )
  L.push('SET @batch := LAST_INSERT_ID();')
  L.push('')

  // 知识点变量名用**序号**而不是名字的切片：中文一字 3 字节，取前 N 字节会让
  // "分页存储管理" 与 "分页存储结构" 撞成同一个变量名，导致知识点关联错挂。
  const kpNames = [...new Set(rows.flatMap((r) => r.obj.knowledgePoints.map((k) => k.name)))]
  const kpIndex = new Map(kpNames.map((n, i) => [n, i]))

  // 知识点：按名称匹配，缺的新建（uk_subject_name 保证幂等；LAST_INSERT_ID(id) 让两种情况都拿到 id）
  L.push('-- 知识点（按名称匹配；不存在的新建）')
  kpNames.forEach((name, i) => {
    L.push(
      `INSERT INTO rk_knowledge_point (subject_id, name) VALUES (${SUBJECT_ID}, ${q(name)}) ` +
        `ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);`
    )
    L.push(`SET @kp_${i} := LAST_INSERT_ID();`)
  })
  L.push('')

  // 题目
  rows.forEach((r, idx) => {
    const o = r.obj
    const n = String(idx + 1).padStart(4, '0')
    L.push(`-- ${r.where}  ${o.clientKey || ''}`)
    L.push('INSERT INTO rk_question')
    L.push('  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,')
    L.push('   difficulty, source_type, source_ref, source_license, generation_batch_id, status,')
    L.push('   created_by, published_at)')
    L.push(
      `VALUES (${SUBJECT_ID}, ${Number(o.outlineId)}, 'SINGLE_CHOICE', ${q(o.stem)}, 'MARKDOWN', ` +
        `${q(o.correctAnswer)}, ${q(o.analysis)}, ${o.difficulty}, 'AI_GENERATED', ${q(o.sourceRef)}, ` +
        `'SELF', @batch, 'PUBLISHED', ${OPERATOR_ID}, NOW());`
    )
    L.push(`SET @q${n} := LAST_INSERT_ID();`)

    const opt = o.options
      .map((op, i) => `(@q${n}, ${q(op.key)}, ${q(op.content)}, ${i})`)
      .join(', ')
    L.push(`INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES ${opt};`)

    for (const kp of o.knowledgePoints) {
      L.push(`INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q${n}, @kp_${kpIndex.get(kp.name)});`)
    }
    L.push('')
  })

  // 审计：如实记录方案 B 的偏离。
  // module/action 取 04 的列注释里列出的取值（QUESTION / CREATE），不自造枚举；
  // "批量导入、未经审核"这层信息放在 detail 与 target_type 里。
  L.push('-- 审计：如实记录方案 B 的偏离')
  L.push(
    `INSERT INTO sys_operation_log (user_id, module, action, target_type, target_id, detail) VALUES ` +
      `(${OPERATOR_ID}, 'QUESTION', 'CREATE', 'generation_batch', @batch, ` +
      `${q(`方案 B 最小导入器：${rows.length} 题由 ${BATCH_NO} 批量导入并直接置为 PUBLISHED，未经逐题审核（reviewed_by 留 NULL）。`)});`
  )
  L.push('')
  L.push('COMMIT;')
  return L.join('\n') + '\n'
}

// ---------- 导入后的计数刷新 ----------
function buildCountSql(env, rows) {
  const L = []
  L.push('')
  L.push('-- 冗余计数刷新（不进事务：纯派生数据，重算一遍即可）')
  L.push(
    `UPDATE rk_subject SET question_count = ` +
      `(SELECT COUNT(*) FROM rk_question WHERE subject_id = ${SUBJECT_ID} AND status = 'PUBLISHED') ` +
      `WHERE id = ${SUBJECT_ID};`
  )

  // 章节计数 = 该节点子树下全部已发布题量（rk_outline_node.question_count 的注释口径）
  const treeRaw = mysqlQuery(
    env,
    `SELECT id, parent_id FROM rk_outline_node WHERE subject_id = ${SUBJECT_ID} ORDER BY id`
  )
  const nodes = treeRaw
    .split('\n')
    .filter(Boolean)
    .map((l) => {
      const [id, parent] = l.split('\t')
      return { id: Number(id), parent: Number(parent) }
    })
  const childrenOf = {}
  for (const n of nodes) (childrenOf[n.parent] = childrenOf[n.parent] || []).push(n.id)

  const perNode = {}
  for (const r of rows) perNode[Number(r.outlineId)] = (perNode[Number(r.outlineId)] || 0) + 1

  // 自底向上汇总
  const subtree = {}
  const walk = (id) => {
    if (subtree[id] !== undefined) return subtree[id]
    let sum = perNode[id] || 0
    for (const c of childrenOf[id] || []) sum += walk(c)
    subtree[id] = sum
    return sum
  }
  for (const n of nodes) walk(n.id)

  for (const n of nodes) {
    L.push(`UPDATE rk_outline_node SET question_count = ${subtree[n.id]} WHERE id = ${n.id};`)
  }
  return L.join('\n') + '\n'
}

// ---------- 主流程 ----------
function main() {
  const env = readEnv()
  const mysqlOpts = { encoding: 'utf8', env: { ...process.env, MYSQL_PWD: env.MYSQL_ROOT_PASSWORD } }

  console.log('='.repeat(64))
  console.log(`题目导入器（方案 B）· ${EXECUTE ? '【执行模式】' : '【dry-run，不写库】'}`)
  console.log('='.repeat(64))

  // ① 门禁：必须先过校验器
  console.log('\n[1/5] 跑校验器…')
  try {
    execFileSync('node', [path.join(__dirname, 'validate-questions.js'), DATA_DIR], {
      stdio: 'pipe',
      encoding: 'utf8',
    })
    console.log('      ✓ 校验通过')
  } catch (e) {
    console.error('      ✗ 校验未通过，拒绝导入。先修数据：')
    console.error((e.stdout || '') + (e.stderr || ''))
    process.exit(1)
  }

  // ② 读数据
  console.log('\n[2/5] 读入 JSONL…')
  const files = fs.readdirSync(DATA_DIR).filter((f) => f.endsWith('.jsonl')).sort()
  const rows = []
  for (const f of files) {
    fs.readFileSync(path.join(DATA_DIR, f), 'utf8')
      .split('\n')
      .forEach((line, i) => {
        if (line.trim() === '') return
        rows.push({ where: `${f}:${i + 1}`, outlineId: null, obj: JSON.parse(line) })
      })
  }
  rows.forEach((r) => (r.outlineId = r.obj.outlineId))
  console.log(`      ${files.length} 个文件，${rows.length} 题`)

  // ③ 库侧前置检查（只读）
  console.log('\n[3/5] 库侧前置检查…')
  const dupBatch = mysqlQuery(env, `SELECT COUNT(*) FROM rk_generation_batch WHERE batch_no = '${BATCH_NO}'`)
  if (Number(dupBatch) > 0) {
    console.error(`      ✗ batch_no='${BATCH_NO}' 已存在（${dupBatch} 条）——重复导入会产生重复题目，已拒绝。`)
    console.error('        若要重导：先手工清掉该批次及其题目，或换一个 batch_no。')
    process.exit(1)
  }
  console.log(`      ✓ batch_no='${BATCH_NO}' 未被占用`)

  const existingQ = mysqlQuery(env, 'SELECT COUNT(*) FROM rk_question')
  const existingKp = mysqlQuery(env, `SELECT COUNT(*) FROM rk_knowledge_point WHERE subject_id = ${SUBJECT_ID}`)
  const kpWanted = new Set(rows.flatMap((r) => r.obj.knowledgePoints.map((k) => k.name))).size
  console.log(`      · 现有题目总数 ${existingQ}（本批将新增 ${rows.length}）`)
  console.log(`      · 现有 SD 知识点 ${existingKp}；本批涉及 ${kpWanted} 个名称 → 需按名称匹配，缺的将新建`)

  const adminOk = mysqlQuery(env, `SELECT COUNT(*) FROM sys_user WHERE id = ${OPERATOR_ID} AND role='ADMIN'`)
  if (Number(adminOk) !== 1) {
    console.error(`      ✗ created_by/operator_id=${OPERATOR_ID} 不是有效 ADMIN，拒绝`)
    process.exit(1)
  }
  console.log(`      ✓ operator_id=${OPERATOR_ID} 是 ADMIN`)

  // ④ 生成 SQL
  console.log('\n[4/5] 生成 SQL…')
  const sql = buildSql(env, rows)
  const countSql = buildCountSql(env, rows)
  fs.mkdirSync(path.dirname(OUT_SQL), { recursive: true })
  fs.writeFileSync(OUT_SQL, sql + countSql, 'utf8')
  const stmts = (sql + countSql).split('\n').filter((l) => /^(INSERT|UPDATE|SET|START|COMMIT)/i.test(l.trim())).length
  console.log(`      · 写入 ${path.relative(PROJECT, OUT_SQL)}（${(sql + countSql).length} 字节，约 ${stmts} 条语句）`)
  console.log(`      · 涉及表：rk_generation_batch(1) rk_question(${rows.length}) ` +
    `rk_question_option(${rows.length * 4}) rk_knowledge_point(≤${kpWanted}) ` +
    `rk_question_kp(${rows.reduce((a, r) => a + r.obj.knowledgePoints.length, 0)}) ` +
    `rk_subject(1) rk_outline_node(66) sys_operation_log(1)`)

  if (!EXECUTE) {
    console.log('\n[5/5] dry-run 结束，未写库。')
    console.log(`      要执行：node scripts/generate/import-questions.js --execute`)
    console.log(`      也可以让 mysql 自己跑这份 SQL：mysql -h127.0.0.1 -uroot -p ruankao < ${path.relative(PROJECT, OUT_SQL)}`)
    return
  }

  // ⑤ 执行
  console.log('\n[5/5] 执行写库…')
  try {
    execFileSync('mysql', ['-h', '127.0.0.1', '-u', 'root', env.MYSQL_DATABASE, '--default-character-set=utf8mb4'], {
      input: sql,
      ...mysqlOpts,
      stdio: ['pipe', 'pipe', 'pipe'],
    })
  } catch (e) {
    console.error('      ✗ 事务失败，已回滚（事务内没有任何提交）。mysql 输出：')
    console.error((e.stderr || '') + (e.stdout || ''))
    process.exit(1)
  }
  console.log('      ✓ 事务提交成功')

  try {
    execFileSync('mysql', ['-h', '127.0.0.1', '-u', 'root', env.MYSQL_DATABASE, '--default-character-set=utf8mb4'], {
      input: countSql,
      ...mysqlOpts,
      stdio: ['pipe', 'pipe', 'pipe'],
    })
  } catch (e) {
    console.error('      ⚠ 计数刷新失败（题目已入库，只是冗余计数列没更新）：')
    console.error((e.stderr || '') + '')
    process.exit(1)
  }
  console.log('      ✓ 冗余计数已刷新')

  // 缓存：当前库为空，仍防御性地清一次 rk:cache:*
  const REDIS_CLI = 'D:\\Redis-x64-3.2.100\\redis-cli.exe'
  if (fs.existsSync(REDIS_CLI) && env.REDIS_PASSWORD) {
    try {
      const n = execFileSync(
        REDIS_CLI,
        ['-h', '127.0.0.1', '-p', '6379', '-a', env.REDIS_PASSWORD, 'EVAL',
          "local k=redis.call('keys','rk:cache:*') if #k>0 then return redis.call('del',unpack(k)) end return 0", '0'],
        { encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] }
      ).trim()
      console.log(`      ✓ 已清 rk:cache:* （删除 ${n} 个键）`)
    } catch (e) {
      console.log('      ⚠ 清缓存失败（不影响数据，重启后端前缓存里也可能本就没有相关键）')
    }
  }

  console.log('\n完成。下一步可查：')
  console.log(`  SELECT COUNT(*) FROM rk_question WHERE status='PUBLISHED';`)
  console.log(`  SELECT question_count FROM rk_subject WHERE id=${SUBJECT_ID};`)
}

main()