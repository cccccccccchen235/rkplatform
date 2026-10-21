#!/usr/bin/env node
/**
 * 一次性脚本：把 data/SD-*.jsonl 导入「另一台库」。
 *
 * 为什么不直接用 scripts/generate/import-questions.js：
 *   1. 语料里的 outlineId 是开发库的主键，与目标库不一致，必须先按章节名重映射；
 *   2. 官方导入器把 mysql 主机/口令写死在 127.0.0.1，且只生成 SQL 不负责传输。
 *
 * 除了 outlineId 重映射，SQL 生成逻辑（列、转义、知识点 UPSERT、计数刷新）
 * 与官方导入器保持一致。生成的 SQL 由外部 scp 到目标机后执行。
 *
 * 用法：node .verify-tmp/import-sd-remote.js
 */

const fs = require('fs')
const path = require('path')

const HERE = __dirname
const PROJECT = path.resolve(HERE, '..')
const DATA_DIR = path.join(PROJECT, 'data')
const SPEC = path.join(DATA_DIR, 'question-spec-sd.md')
const NODE_MAP = path.join(HERE, 'sd_map.json')
const OUT_SQL = path.join(HERE, 'import-SD-remote.sql')

const BATCH_NO = 'EX-SD-001'
const SUBJECT_ID = 6
const MODEL = 'external-ai (user-provided)'
const PROMPT_VERSION = 'question-spec-sd'
const OPERATOR_ID = 1

// ---------- 章节名 -> 目标库节点 id ----------
const nodes = JSON.parse(fs.readFileSync(NODE_MAP, 'utf8'))
const idByName = new Map()
const dupNames = []
for (const n of nodes) {
  if (idByName.has(n.name)) dupNames.push(n.name)
  idByName.set(n.name, n.id)
}
if (dupNames.length) {
  console.error('目标库同一科目内章节名不唯一，无法按名映射：', dupNames)
  process.exit(1)
}

// ---------- 规格文档 §7：旧 outlineId -> 章节名 ----------
const spec = fs.readFileSync(SPEC, 'utf8')
const start = spec.indexOf('\n## 7.')
const end = spec.indexOf('\n## 8.')
if (start < 0 || end < 0) {
  console.error('规格文档里找不到 §7 区间')
  process.exit(1)
}
const section7 = spec.slice(start, end)
const oldToName = new Map()
for (const line of section7.split('\n')) {
  const m = line.match(/^\|\s*`(\d+)`\s*\|\s*([^|]+?)\s*\|\s*(\d+)\s*\|/)
  if (m) oldToName.set(m[1], m[2])
}

// ---------- 读语料 ----------
const files = fs.readdirSync(DATA_DIR).filter((f) => f.endsWith('.jsonl')).sort()
const rows = []
for (const f of files) {
  fs.readFileSync(path.join(DATA_DIR, f), 'utf8')
    .split('\n')
    .forEach((line, i) => {
      if (line.trim() === '') return
      rows.push({ where: `${f}:${i + 1}`, obj: JSON.parse(line) })
    })
}

// ---------- 重映射 ----------
const oldIds = [...new Set(rows.map((r) => String(r.obj.outlineId)))]
const remap = new Map()
const unmapped = []
for (const oldId of oldIds) {
  const name = oldToName.get(oldId)
  if (!name) {
    unmapped.push(`${oldId}: 规格文档 §7 里没有这个 ID`)
    continue
  }
  const newId = idByName.get(name)
  if (!newId) {
    unmapped.push(`${oldId} (${name}): 目标库找不到同名章节`)
    continue
  }
  remap.set(oldId, newId)
}
if (unmapped.length) {
  console.error('以下章节无法重映射，已中止：')
  unmapped.forEach((u) => console.error('  - ' + u))
  process.exit(1)
}
for (const r of rows) r.outlineId = remap.get(String(r.obj.outlineId))

console.log(`重映射：${oldIds.length} 个旧 ID -> 目标库 ID，全部命中`)
console.log('  例：' + [...remap.entries()].slice(0, 4).map(([a, b]) => `${a}->${b}`).join('  '))

// ---------- SQL 生成（与官方导入器同构） ----------
const q = (s) => "'" + String(s).replace(/\\/g, '\\\\').replace(/'/g, "\\'") + "'"
const L = []

L.push('-- 由 .verify-tmp/import-sd-remote.js 生成：官方导入器的 SQL 生成逻辑 + outlineId 按章节名重映射')
L.push('-- 未经逐题审核，直接 PUBLISHED；reviewed_by 留 NULL，理由见 sys_operation_log')
L.push('SET NAMES utf8mb4;')
L.push('START TRANSACTION;')
L.push('')
L.push('INSERT INTO rk_generation_batch')
L.push('  (batch_no, subject_id, outline_ids, question_type, target_count, model, prompt_version,')
L.push('   status, produced_count, approved_count, rejected_count, operator_id, finished_at)')
L.push(
  `VALUES (${q(BATCH_NO)}, ${SUBJECT_ID}, ${q(JSON.stringify([...new Set(rows.map((r) => r.outlineId))]))}, 'SINGLE_CHOICE', ` +
    `${rows.length}, ${q(MODEL)}, ${q(PROMPT_VERSION)}, 'COMPLETED', ${rows.length}, ${rows.length}, 0, ${OPERATOR_ID}, NOW());`
)
L.push('SET @batch := LAST_INSERT_ID();')
L.push('')

const kpNames = [...new Set(rows.flatMap((r) => r.obj.knowledgePoints.map((k) => k.name)))]
const kpIndex = new Map(kpNames.map((n, i) => [n, i]))
L.push('-- 知识点（按名称匹配；不存在的新建）')
kpNames.forEach((name, i) => {
  L.push(
    `INSERT INTO rk_knowledge_point (subject_id, name) VALUES (${SUBJECT_ID}, ${q(name)}) ` +
      `ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id);`
  )
  L.push(`SET @kp_${i} := LAST_INSERT_ID();`)
})
L.push('')

rows.forEach((r, idx) => {
  const o = r.obj
  const n = String(idx + 1).padStart(4, '0')
  L.push(`-- ${r.where}  ${o.clientKey || ''}  outline ${o.outlineId} -> ${r.outlineId}`)
  L.push('INSERT INTO rk_question')
  L.push('  (subject_id, outline_id, question_type, stem, stem_format, correct_answer, analysis,')
  L.push('   difficulty, source_type, source_ref, source_license, generation_batch_id, status,')
  L.push('   created_by, published_at)')
  L.push(
    `VALUES (${SUBJECT_ID}, ${r.outlineId}, 'SINGLE_CHOICE', ${q(o.stem)}, 'MARKDOWN', ` +
      `${q(o.correctAnswer)}, ${q(o.analysis)}, ${o.difficulty}, 'AI_GENERATED', ${q(o.sourceRef)}, ` +
      `'SELF', @batch, 'PUBLISHED', ${OPERATOR_ID}, NOW());`
  )
  L.push(`SET @q${n} := LAST_INSERT_ID();`)
  const opt = o.options.map((op, i) => `(@q${n}, ${q(op.key)}, ${q(op.content)}, ${i})`).join(', ')
  L.push(`INSERT INTO rk_question_option (question_id, option_key, content, sort_no) VALUES ${opt};`)
  for (const kp of o.knowledgePoints) {
    L.push(`INSERT INTO rk_question_kp (question_id, kp_id) VALUES (@q${n}, @kp_${kpIndex.get(kp.name)});`)
  }
  L.push('')
})

L.push('-- 审计：如实记录方案 B 的偏离')
L.push(
  `INSERT INTO sys_operation_log (user_id, module, action, target_type, target_id, detail) VALUES ` +
    `(${OPERATOR_ID}, 'QUESTION', 'CREATE', 'generation_batch', @batch, ` +
    `${q(`方案 B 最小导入器：${rows.length} 题由 ${BATCH_NO} 批量导入并直接置为 PUBLISHED，未经逐题审核（reviewed_by 留 NULL）。`)});`
)
L.push('')
L.push('COMMIT;')

// ---------- 计数刷新 ----------
const M = []
M.push('')
M.push('-- 冗余计数刷新（不进事务：纯派生数据，重算一遍即可）')
M.push(
  `UPDATE rk_subject SET question_count = ` +
    `(SELECT COUNT(*) FROM rk_question WHERE subject_id = ${SUBJECT_ID} AND status = 'PUBLISHED') ` +
    `WHERE id = ${SUBJECT_ID};`
)
const childrenOf = {}
for (const n of nodes) (childrenOf[n.parent] = childrenOf[n.parent] || []).push(n.id)
const perNode = {}
for (const r of rows) perNode[r.outlineId] = (perNode[r.outlineId] || 0) + 1
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
  M.push(`UPDATE rk_outline_node SET question_count = ${subtree[n.id]} WHERE id = ${n.id};`)
}

fs.writeFileSync(OUT_SQL, L.join('\n') + M.join('\n'), 'utf8')
console.log(`已写出 ${OUT_SQL}`)
console.log(`  题目 ${rows.length} / 选项 ${rows.length * 4} / 知识点 ${kpNames.length} / 章节计数刷新 ${nodes.length} 行`)
