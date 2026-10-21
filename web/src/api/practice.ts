// 练习（05 §5.6 第 12~15 条，07 §7.4.3）。
//
// 四条里 `papers` 与 `progress` 是页面级读取/写入，页面上都有自己的错误面板与重试，
// 所以一律由调用方传 `{ silent: true }`，独家提示——否则同一次失败会先弹一条 toast、
// 再在页面里出现一块内容相同的面板。
//
// 两个契约要点写进注释而不是类型里（类型表达不了）：
//   ① 上报体**不得**包含 `isCorrect`——服务端重新判分（05 §5.6 第 13 条）；
//   ② 同一 `questionId` 在 5 秒内重复上报会被服务端当作重放忽略（返回 accepted=0），
//      这是正确行为，不是失败。

import type {
  PracticePaper,
  PracticePaperPayload,
  PracticeProgress,
  PracticeProgressPayload,
  PracticeRecordPayload,
  PracticeRecordResult,
} from '@/types/api'
import { get, post, put, type RequestOptions } from '@/utils/request'

/**
 * 05 §5.6 第 12 条。一次性取回**含答案与解析**的题目列表（`QuestionDetailVO[]`）。
 *
 * 错误码：`3001`（科目不存在/停用）、`1001`（如 `SAMPLE` 且 `count > 5`）、
 * `2005`（游客请求非 `SAMPLE` 模式——本接口 `@SaIgnore`，游客资格在方法体内校验）。
 */
export function fetchPracticePaper(payload: PracticePaperPayload, options?: RequestOptions): Promise<PracticePaper> {
  return post<PracticePaper>('/practice/papers', payload, options)
}

/**
 * 05 §5.6 第 13 条。批量上报答题结果，单次 ≤50 条。
 *
 * 请求体里的 `items` 只带 `{questionId, userAnswer, durationMs}`，**不带 `isCorrect`**：
 * 客户端判分只用于即时反馈，权威结果由服务端复核（07 §7.4.3）。
 */
export function submitPracticeRecords(
  payload: PracticeRecordPayload,
  options?: RequestOptions,
): Promise<PracticeRecordResult> {
  return post<PracticeRecordResult>('/practice/records', payload, options)
}

/**
 * 05 §5.6 第 14 条。返回的是**数组** `PracticeProgress[]`——`mode` 缺省时给出该科目
 * 全部模式的游标，所以顺序练习页需要自己在里面挑 `mode === 'SEQUENCE'` 的那条。
 */
export function fetchPracticeProgress(
  subjectId: string,
  mode?: PracticeProgressPayload['mode'],
  options?: RequestOptions,
): Promise<PracticeProgress[]> {
  return get<PracticeProgress[]>('/practice/progress', { subjectId, mode }, options)
}

/** 05 §5.6 第 15 条。幂等 upsert 顺序练习游标，响应 `data` 为 `null`。 */
export function savePracticeProgress(payload: PracticeProgressPayload, options?: RequestOptions): Promise<void> {
  return put<void>('/practice/progress', payload, options)
}