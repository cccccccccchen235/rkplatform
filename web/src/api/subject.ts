// 科目与大纲（05 §5.4，公开接口第 8~11 条）。
//
// 这四条都是**页面级读取**：页面上都有一块自己的错误面板（`el-alert` + 重试按钮），
// 所以调用方一律传 `{ silent: true }`，由页面独家承担提示——否则同一次失败会
// 先弹一条 toast、再在页面里出现一条一模一样的，用户以为出了两个错。

import type { GroupedSubjects, KnowledgePointVO, OutlineNodeVO, SubjectLevel, SubjectVO } from '@/types/api'
import { get, type RequestOptions } from '@/utils/request'

export interface SubjectListQuery {
  /** 可选，只取某一级别。不传则三个级别全返回。 */
  level?: SubjectLevel
  /** 可选，默认 false；true 时只返回已配置卷面模板的科目（可发起模拟考）。 */
  onlyConfigured?: boolean
}

/**
 * 05 §5.4 第 8 条。响应按 `level` 分组，形如 `{ JUNIOR: [...], MIDDLE: [...], SENIOR: [...] }`。
 *
 * 注意：接口保证三个键都存在（可能为空数组），所以页面不需要判空键、
 * 只需判空数组——这个区别决定了"整页空态"与"某级别缺数据"是两种文案。
 */
export function fetchSubjects(query: SubjectListQuery = {}, options?: RequestOptions): Promise<GroupedSubjects> {
  return get<GroupedSubjects>('/subjects', { ...query }, options)
}

/**
 * 05 §5.4 第 9 条。**唯一填充 `paperSummary` 的接口**——列表接口恒为 `null`
 * （05 §5.2 的说明：为 20 个科目各带一棵场次树只让首屏变慢）。
 *
 * 科目不存在**或已停用**都返回 `3001`（05 §5.1.2：两件事共用一个码）。
 * 所以页面无法、也不该区分"没有这个科目"与"这个科目下架了"。
 */
export function fetchSubjectDetail(id: string, options?: RequestOptions): Promise<SubjectVO> {
  return get<SubjectVO>(`/subjects/${id}`, undefined, options)
}

/**
 * 05 §5.4 第 10 条。返回**根节点数组**，根节点的 `parentId` 恒为 `"0"`。
 *
 * 大纲为空的科目返回 `[]`（不是 `null`）——它是"还没有章节"，不是"取不到"。
 */
export function fetchSubjectOutline(id: string, options?: RequestOptions): Promise<OutlineNodeVO[]> {
  return get<OutlineNodeVO[]>(`/subjects/${id}/outline`, undefined, options)
}

/** 05 §5.4 第 11 条。公开接口只返回 `ENABLED` 标签。 */
export function fetchSubjectKnowledgePoints(id: string, options?: RequestOptions): Promise<KnowledgePointVO[]> {
  return get<KnowledgePointVO[]>(`/subjects/${id}/knowledge-points`, undefined, options)
}
