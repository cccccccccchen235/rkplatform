#!/usr/bin/env node
/**
 * 题目 JSONL 校验器（P3 · 方案 B 的入库前置）
 *
 * 用法：
 *   node scripts/generate/validate-questions.js [目录，默认 data/]
 *
 * 它做两件事：
 *   ① 逐行校验 13 个必填字段、常量值、选项结构、长度、黑名单（对应 08 §8.6 的丢弃规则）
 *   ② 统计逐章配额、难度分布、批内重复（对应 data/question-spec-sd.md 的第 7、6 节）
 *
 * 退出码：0 = 全部通过；1 = 有失败行
 *
 * 注意：本脚本不做数据库访问。outlineId 与配额是从规格文档里取的常量，
 * 与库的对应关系由导入器在入库时再核一次。
 */

const fs = require('fs')
const path = require('path')

const TARGET_SUBJECT = '6' // 软件设计师
const ALLOWED_DIFFICULTY = [1, 2, 3, 4, 5]

/** 规格文档第 7 节的逐章配额（55 个叶子章节，合计 250） */
const QUOTA = {
  // 7.1 计算机科学基础知识
  16: 5, 17: 5, 18: 5, 19: 5,
  // 7.2 计算机系统知识
  81: 5, 80: 5, 79: 5, 85: 5, 84: 5, 83: 5, 82: 5, 22: 5, 23: 5,
  // 7.3 系统开发和运行知识
  24: 5, 25: 5, 26: 5, 27: 5, 28: 5, 29: 5, 30: 5,
  // 7.4 软件工程应用
  53: 5, 54: 5, 55: 5, 56: 5, 57: 5, 58: 5, 59: 5, 60: 5, 61: 5,
  // 7.5 外部设计
  34: 4, 35: 4, 36: 4, 37: 4, 38: 4, 39: 4,
  // 7.6 内部设计
  40: 4, 41: 4, 42: 4, 43: 4, 44: 4, 45: 4,
  // 7.7 系统实施
  49: 4, 50: 4, 51: 4, 52: 4,
  // 7.8 程序设计
  46: 4, 47: 4, 48: 4,
  // 7.9 标准化、信息化和知识产权
  31: 4, 32: 4, 33: 4,
  // 7.10 单章成节
  4: 6, 5: 6, 10: 5,
}

const REQUIRED_FIELDS = [
  'subjectId', 'outlineId', 'questionType', 'stem', 'stemFormat',
  'options', 'correctAnswer', 'analysis', 'difficulty',
  'knowledgePoints', 'sourceType', 'sourceLicense', 'sourceRef',
]

/** 08 §8.6 的黑名单：年份 / 考次 / 真题类字样。命中即丢弃。 */
const BLACKLIST = [
  { re: /(19|20)\d{2}\s*年/, label: '出现年份' },
  { re: /上半年|下半年|第\s*\d+\s*次考试|考次/, label: '出现考次' },
  { re: /真题|原题|试题|历年/, label: '出现"真题/原题/试题/历年"字样' },
]

/** 选项不得全部是这类凑数项（08 §8.6 干扰项合理性） */
const LAZY_OPTION = /^(以上都[对正确]|以上都不[对正确]|全部正确|都不正确|无法确定)$/

function stripMarkdown(s) {
  return String(s)
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/[*_`#>~]/g, '')
    .replace(/\s+/g, '')
}

/** 归一化题干用于重复检测：去标记、去标点、转小写、KaTeX 命令降级 */
function normalizeStem(s) {
  return String(s)
    .toLowerCase()
    .replace(/\\[a-z]+/g, '')
    .replace(/[^\u4e00-\u9fa5a-z0-9]/g, '')
}

function trigrams(s) {
  const out = new Set()
  for (let i = 0; i + 3 <= s.length; i++) out.add(s.slice(i, i + 3))
  if (s.length > 0 && s.length < 3) out.add(s)
  return out
}

function dice(a, b) {
  if (a.size === 0 || b.size === 0) return 0
  let inter = 0
  for (const t of a) if (b.has(t)) inter++
  return (2 * inter) / (a.size + b.size)
}

function validateOne(obj, ctx) {
  const errs = []

  // --- 必填字段 ---
  for (const f of REQUIRED_FIELDS) {
    if (obj[f] === undefined || obj[f] === null || obj[f] === '') {
      errs.push(`缺少必填字段 ${f}`)
    }
  }
  if (errs.length) return errs

  // --- 常量值 ---
  if (obj.subjectId !== TARGET_SUBJECT) errs.push(`subjectId 应为 "${TARGET_SUBJECT}"，实际 "${obj.subjectId}"`)
  if (obj.questionType !== 'SINGLE_CHOICE') errs.push(`questionType 应为 SINGLE_CHOICE，实际 "${obj.questionType}"`)
  if (obj.stemFormat !== 'MARKDOWN') errs.push(`stemFormat 应为 MARKDOWN，实际 "${obj.stemFormat}"`)
  if (obj.sourceType !== 'AI_GENERATED') errs.push(`sourceType 应为 AI_GENERATED，实际 "${obj.sourceType}"`)
  if (obj.sourceLicense !== 'SELF') errs.push(`sourceLicense 应为 SELF，实际 "${obj.sourceLicense}"`)

  // --- 章节归属 ---
  if (!Object.prototype.hasOwnProperty.call(QUOTA, obj.outlineId)) {
    errs.push(`outlineId "${obj.outlineId}" 不在规格第 7 节的 55 个章节内`)
  } else {
    ctx.quotaSeen[obj.outlineId] = (ctx.quotaSeen[obj.outlineId] || 0) + 1
  }

  // --- 选项 ---
  if (!Array.isArray(obj.options)) {
    errs.push('options 不是数组')
  } else {
    if (obj.options.length !== 4) errs.push(`options 应为 4 个，实际 ${obj.options.length} 个`)
    const keys = obj.options.map((o) => o && o.key)
    if (new Set(keys).size !== keys.length) errs.push('options 的 key 有重复')
    for (const o of obj.options) {
      if (!o || typeof o.key !== 'string' || !/^[A-F]$/.test(o.key)) errs.push(`选项 key 非法：${JSON.stringify(o && o.key)}`)
      if (!o || typeof o.content !== 'string' || o.content.trim() === '') errs.push(`选项 ${o && o.key} 内容为空`)
    }
    const lazy = obj.options.filter((o) => o && typeof o.content === 'string' && LAZY_OPTION.test(o.content.trim()))
    if (lazy.length >= 2) errs.push(`干扰项凑数：${lazy.length} 个"以上都对/都不对"类选项`)

    if (typeof obj.correctAnswer !== 'string' || !keys.includes(obj.correctAnswer)) {
      errs.push(`correctAnswer "${obj.correctAnswer}" 不是任一选项的 key`)
    }
  }

  // --- 答案格式：单选必须单个字母 ---
  if (typeof obj.correctAnswer === 'string' && obj.correctAnswer.length !== 1) {
    errs.push(`SINGLE_CHOICE 的 correctAnswer 应为单个字母，实际 "${obj.correctAnswer}"`)
  }

  // --- 题干长度 ---
  const visible = stripMarkdown(obj.stem)
  if (visible.length < 10) errs.push(`题干过短（去标记后 ${visible.length} 字，下限 10）`)
  if (visible.length > 600) errs.push(`题干过长（去标记后 ${visible.length} 字，上限 600）`)

  // --- 难度 ---
  if (!Number.isInteger(obj.difficulty) || !ALLOWED_DIFFICULTY.includes(obj.difficulty)) {
    errs.push(`difficulty 应为 1~5 的整数，实际 ${JSON.stringify(obj.difficulty)}`)
  }

  // --- 知识点 ---
  if (!Array.isArray(obj.knowledgePoints)) {
    errs.push('knowledgePoints 不是数组')
  } else {
    if (obj.knowledgePoints.length < 1 || obj.knowledgePoints.length > 5) {
      errs.push(`knowledgePoints 应为 1~5 个，实际 ${obj.knowledgePoints.length} 个`)
    }
    for (const kp of obj.knowledgePoints) {
      if (!kp || typeof kp.name !== 'string' || kp.name.trim() === '') errs.push('knowledgePoints 有空名称项')
    }
  }

  // --- 解析必须有推理（粗判：长度 + 不能只写"故选X"） ---
  const analysisVisible = stripMarkdown(obj.analysis)
  if (analysisVisible.length < 15) errs.push(`解析过短（去标记后 ${analysisVisible.length} 字），需含推理过程`)

  // --- KaTeX / 图片 ---
  for (const [field, val] of [['stem', obj.stem], ['analysis', obj.analysis]]) {
    const dollars = (String(val).match(/(?<!\\)\$/g) || []).length
    if (dollars % 2 !== 0) errs.push(`${field} 的 $ 不成对（${dollars} 个）`)
    if (/!\[[^\]]*\]\(/.test(String(val))) errs.push(`${field} 含图片引用（本批禁止用图）`)
  }

  // --- 黑名单 ---
  const haystack = [obj.stem, obj.analysis, ...(obj.options || []).map((o) => o && o.content)].join('\n')
  for (const { re, label } of BLACKLIST) {
    const m = haystack.match(re)
    if (m) errs.push(`${label}：命中「${m[0]}」`)
  }

  return errs
}

function main() {
  const dir = process.argv[2] || 'data'
  const files = fs
    .readdirSync(dir)
    .filter((f) => f.endsWith('.jsonl'))
    .sort()

  if (files.length === 0) {
    console.error(`✗ ${dir} 下没有 .jsonl 文件`)
    process.exit(1)
  }

  const ctx = { quotaSeen: {} }
  const difficulty = {}
  const failures = []
  const rows = []
  let total = 0

  for (const file of files) {
    const lines = fs.readFileSync(path.join(dir, file), 'utf8').split('\n')
    lines.forEach((line, i) => {
      if (line.trim() === '') return
      const where = `${file}:${i + 1}`
      total++
      let obj
      try {
        obj = JSON.parse(line)
      } catch (e) {
        failures.push({ where, errs: [`JSON 不合法：${e.message}`] })
        return
      }
      const errs = validateOne(obj, ctx)
      if (errs.length) failures.push({ where, errs })
      difficulty[obj.difficulty] = (difficulty[obj.difficulty] || 0) + 1
      rows.push({ where, outlineId: obj.outlineId, stem: normalizeStem(obj.stem) })
    })
  }

  // --- 批内重复检测 ---
  const dupes = []
  const grams = rows.map((r) => trigrams(r.stem))
  for (let i = 0; i < rows.length; i++) {
    for (let j = i + 1; j < rows.length; j++) {
      if (rows[i].outlineId !== rows[j].outlineId) continue // 08 规定同科目同章节内比对
      const sim = dice(grams[i], grams[j])
      if (sim >= 0.9) dupes.push(`${rows[i].where} ⇄ ${rows[j].where}  相似度 ${sim.toFixed(3)}`)
    }
  }

  // --- 逐章配额 ---
  const quotaIssues = []
  total = 0
  for (const [id, want] of Object.entries(QUOTA)) {
    const got = ctx.quotaSeen[id] || 0
    total += got
    if (got !== want) quotaIssues.push(`章节 ${id}：期望 ${want} 题，实际 ${got} 题`)
  }
  const strayCount = Object.keys(ctx.quotaSeen).length - Object.keys(QUOTA).length

  // --- 输出 ---
  console.log('='.repeat(64))
  console.log(`题目校验 · 目录 ${dir} · 文件 ${files.length} 个 · 共 ${rows.length} 题`)
  console.log('='.repeat(64))

  console.log(`\n-- 逐章配额（期望 250 题）--`)
  console.log(total === 250 ? `✓ 合计 ${total} 题` : `✗ 合计 ${total} 题，与期望 250 不符`)
  quotaIssues.forEach((s) => console.log(`  ✗ ${s}`))

  console.log(`\n-- 难度分布（期望 基础30% 进阶50% 挑战20%）--`)
  const base = (difficulty[1] || 0) + (difficulty[2] || 0)
  const mid = difficulty[3] || 0
  const hard = (difficulty[4] || 0) + (difficulty[5] || 0)
  const pct = (n) => (rows.length ? ((n / rows.length) * 100).toFixed(1) : '0.0')
  console.log(`  基础(1~2) ${base} 题 ${pct(base)}%   进阶(3) ${mid} 题 ${pct(mid)}%   挑战(4~5) ${hard} 题 ${pct(hard)}%`)
  console.log(`  逐档明细：${JSON.stringify(difficulty)}`)

  console.log(`\n-- 批内重复（同章节相似度 ≥0.9）--`)
  console.log(dupes.length === 0 ? '✓ 无' : dupes.map((d) => `  ✗ ${d}`).join('\n'))

  console.log(`\n-- 逐行字段校验 --`)
  if (failures.length === 0) {
    console.log('✓ 全部通过')
  } else {
    console.log(`✗ ${failures.length} 行有问题：\n`)
    for (const f of failures) {
      console.log(`  ${f.where}`)
      f.errs.forEach((e) => console.log(`      · ${e}`))
    }
  }

  const ok = failures.length === 0 && dupes.length === 0 && quotaIssues.length === 0 && total === 250
  console.log(`\n结果：${ok ? '全绿' : '有失败项'}`)
  process.exit(ok ? 0 : 1)
}

main()