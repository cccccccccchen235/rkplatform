// 模拟考（05 §5.6 第 16~25 条）。
//
// 十条接口里，`GET /exams` 与 `GET /exams/{id}/result` 是**页面级读取**（页面各有自己的
// 错误面板与重试），调用方传 `{ silent: true }`；其余六条是作答过程中的写操作，
// 错误语义各不相同，也一律由 store / 页面按 code 接管：
//
//   · `4003` 存在未完成会话 —— 已由 request.ts 默认静默，由模拟考中心弹「继续/放弃」对话框；
//   · `4005` 会话已结束（含超时自动交卷）—— 已默认静默，由答题页转成绩页；
//   · `1005` 状态不允许 —— 例如未开始场次就作答、论文节提前结束，直接展示 message。
//
// 本文件只做「路径 + 参数」映射，不 import axios，不弹提示（07 §7.11）。

import type {
  ExamAnswerPayload,
  ExamAnswerResultVO,
  ExamHistoryVO,
  ExamResultVO,
  ExamSectionAdvanceVO,
  ExamSessionVO,
  ExamSittingFinishVO,
  ExamSittingStartVO,
  PageResult,
} from '@/types/api'
import { get, post, put, type RequestOptions } from '@/utils/request'

/** 05 §5.6 第 16 条。发起模拟考；有未完成会话返回 `4003`。 */
export function createExam(subjectId: string, options?: RequestOptions): Promise<ExamSessionVO> {
  return post<ExamSessionVO>('/exams', { subjectId }, options)
}

export interface ExamHistoryQuery {
  subjectId?: string
  /** 默认 `false`；`IN_PROGRESS` 在任何取值下都不返回（05 §5.6 第 17 条）。 */
  includeAbandoned?: boolean
  pageNum?: number
  pageSize?: number
}

/** 05 §5.6 第 17 条。分页历史成绩。 */
export function fetchExamHistory(
  query: ExamHistoryQuery = {},
  options?: RequestOptions,
): Promise<PageResult<ExamHistoryVO>> {
  return get<PageResult<ExamHistoryVO>>('/exams', { ...query }, options)
}

/** 05 §5.6 第 18 条。会话详情，断点续答入口。 */
export function fetchExamSession(id: string, options?: RequestOptions): Promise<ExamSessionVO> {
  return get<ExamSessionVO>(`/exams/${id}`, undefined, options)
}

/** 05 §5.6 第 19 条。保存作答（不判分）；空 `items` 即心跳与计时校准。 */
export function saveExamAnswers(
  id: string,
  payload: ExamAnswerPayload,
  options?: RequestOptions,
): Promise<ExamAnswerResultVO> {
  return put<ExamAnswerResultVO>(`/exams/${id}/answers`, payload, options)
}

/** 05 §5.6 第 20 条。启动第 N 场次计时（`PENDING → IN_PROGRESS`）。 */
export function startExamSitting(id: string, sittingNo: number, options?: RequestOptions): Promise<ExamSittingStartVO> {
  return post<ExamSittingStartVO>(`/exams/${id}/sittings/${sittingNo}/start`, {}, options)
}

/** 05 §5.6 第 21 条。离开当前分节，进入同场次下一节（无请求体）。 */
export function advanceExamSection(id: string, sectionNo: number, options?: RequestOptions): Promise<ExamSectionAdvanceVO> {
  return post<ExamSectionAdvanceVO>(`/exams/${id}/sections/${sectionNo}/advance`, {}, options)
}

/** 05 §5.6 第 22 条。提前结束整个场次（论文节所含场次不允许）。 */
export function finishExamSitting(id: string, sittingNo: number, options?: RequestOptions): Promise<ExamSittingFinishVO> {
  return post<ExamSittingFinishVO>(`/exams/${id}/sittings/${sittingNo}/finish`, {}, options)
}

/** 05 §5.6 第 23 条。交卷（幂等）。 */
export function submitExam(id: string, options?: RequestOptions): Promise<ExamResultVO> {
  return post<ExamResultVO>(`/exams/${id}/submit`, {}, options)
}

/** 05 §5.6 第 24 条。成绩详情；未交卷返回 `1005`。 */
export function fetchExamResult(id: string, options?: RequestOptions): Promise<ExamResultVO> {
  return get<ExamResultVO>(`/exams/${id}/result`, undefined, options)
}

/** 05 §5.6 第 25 条。放弃会话（不计入模考统计）。 */
export function abandonExam(id: string, options?: RequestOptions): Promise<void> {
  return post<void>(`/exams/${id}/abandon`, {}, options)
}
