// 错题本（05 §5.7 第 26~28 条）。
//
// 三条都不接 `options`：列表页的读取失败由页面自己的错误面板呈现（所以调 `fetch` 时
// 由页面传 `{ silent: true }`），两个写操作挂在列表行上、失败只由 toast 讲——
// 行上没有第二个能挂错误的地方。这与 admin/content.ts 顶部那条约定同源。

import type { PageResult, WrongQuestionStatus, WrongQuestionVO } from '@/types/api'
import { del, get, post, type RequestOptions } from '@/utils/request'

export interface WrongQuestionQuery {
  /** **必填**（05 §5.7 第 26 条）。未选科目时页面不发请求，不靠服务端兜底。 */
  subjectId: string
  /** 默认 `ACTIVE`。 */
  status?: WrongQuestionStatus
  outlineId?: string
  kpId?: string
  /** 默认 `lastAnsweredAt`。 */
  sortBy?: 'wrongCount' | 'lastAnsweredAt'
  pageNum?: number
  pageSize?: number
}

/**
 * 05 §5.7 第 26 条。分页返回错题条目。
 *
 * `status` 三档（`ACTIVE` / `MASTERED` / `REMOVED`）是**互斥的筛选**而不是层级：
 * 页面用 el-tabs 切换时每次只发一个值，不做"同时看已掌握与已移出"的合并查询。
 */
export function fetchWrongQuestions(
  query: WrongQuestionQuery,
  options?: RequestOptions,
): Promise<PageResult<WrongQuestionVO>> {
  return get<PageResult<WrongQuestionVO>>('/wrong-questions', { ...query }, options)
}

/**
 * 05 §5.7 第 27 条。移出错题本 = 置 `REMOVED`（不删记录，可恢复）。
 * 题目不在本子里时服务端返回 `1003`。
 */
export function removeWrongQuestion(questionId: string): Promise<void> {
  return del<void>(`/wrong-questions/${questionId}`)
}

/** 05 §5.7 第 28 条。把 `REMOVED` 的记录恢复为 `ACTIVE`。 */
export function restoreWrongQuestion(questionId: string): Promise<void> {
  return post<void>(`/wrong-questions/${questionId}/restore`)
}