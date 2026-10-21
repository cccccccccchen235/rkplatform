// 管理端卷面模板配置（05 §5.10 第 51/52 条）。
//
// 与 content.ts 分开成两个文件：这不是"内容维护"三件套（科目/大纲/知识点）的一部分，
// 而是与模拟考同域的一份配置。按业务域切文件，读代码的人不必先翻完 50 条接口
// 才能找到第 51/52 条。

import type {
  PaperTemplateDetailVO,
  PaperTemplateSavePayload,
  PaperTemplateSaveResultVO,
} from '@/types/api'
import { get, put, type RequestOptions } from '@/utils/request'

/**
 * 05 §5.10 第 51 条。**下发全字段**（含 `perQuestionScore` / `durationMin/Max` /
 * `allowEarlySubmit`），与考生端的 `paperSummary` 刻意裁剪过的字段集不同。
 *
 * 科目不存在返回 `3001`；尚未配置模板返回 `4001`——这个页面本来就是来配置它的，
 * 所以 `4001` 是"可以开始填"的正常态，不是故障（页面据此进入新建引导）。
 */
export function fetchAdminPaperTemplate(
  subjectId: string,
  options?: RequestOptions,
): Promise<PaperTemplateDetailVO> {
  return get<PaperTemplateDetailVO>(`/admin/paper-templates/${subjectId}`, undefined, options)
}

/**
 * 05 §5.10 第 52 条。整体覆盖式提交：一次把模板、场次、分节全部换掉，不是增量。
 *
 * 返回体里的 `insufficientQuestions` 是**成功但仍需提示**的情况（配置题数 > 已发布题量），
 * 调用方必须把它展示出来——否则用户以为"保存了个能考 75 题的卷子"，
 * 实际组卷时只会按现有题量出题。
 */
export function saveAdminPaperTemplate(
  subjectId: string,
  payload: PaperTemplateSavePayload,
  options?: RequestOptions,
): Promise<PaperTemplateSaveResultVO> {
  return put<PaperTemplateSaveResultVO>(`/admin/paper-templates/${subjectId}`, payload, options)
}
