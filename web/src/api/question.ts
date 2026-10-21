// 题目反馈（05 §5.9 第 36 条）。
//
// 只有这一条。提交对话框自己把失败讲在对话框顶部（`options` 传 `{ silent: true }`），
// 因为 `1002`（24 小时内对同一题超过 3 次）是一条需要用户读懂才会改行为的提示，
// toast 两秒就消失了。

import type { QuestionFeedbackPayload } from '@/types/api'
import { post, type RequestOptions } from '@/utils/request'

/**
 * 05 §5.9 第 36 条。`content` 5~1000 字；同一用户对同一题 24 小时内最多 3 次，
 * 第 4 次返回 `1002`。
 */
export function submitQuestionFeedback(
  questionId: string,
  payload: QuestionFeedbackPayload,
  options?: RequestOptions,
): Promise<void> {
  return post<void>(`/questions/${questionId}/feedback`, payload, options)
}