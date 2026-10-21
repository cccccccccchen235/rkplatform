// 学习统计（05 §5.8 第 32~35 条）。
//
// 四条都是**页面级读取**：统计页自己有错误面板与重试，所以调用方一律传
// `{ silent: true }`，由页面独家提示——否则一次失败会先弹一条 toast、
// 页面里再出现一块内容相同的面板。
//
// `subjectId` 在前端**总是显式传**：服务端缺省取用户的 currentSubjectId，
// 而当前备考科目的写接口（05 §5.3 第 7 条）尚未实现，库里那个字段往往是空的。
// 页面自己维护科目选择并回落到列表第一个科目，比依赖一个改不动的服务端缺省更可靠。

import type { StatOutlineNodeVO, StatOverviewVO, StatTrendPointVO, WeakKpVO } from '@/types/api'
import { get, type RequestOptions } from '@/utils/request'

export interface StatQuery {
  subjectId?: string
}

export interface StatTrendQuery extends StatQuery {
  /** 默认 30，上限 90（05 §5.8 第 33 条）。 */
  days?: number
}

export interface StatWeakKpQuery extends StatQuery {
  /** 默认 10，上限 30。 */
  limit?: number
  /** 默认 0.6（05 §5.8 第 35 条）。 */
  maxMastery?: number
}

/** 05 §5.8 第 32 条。未做过任何题的科目返回零值与先验掌握度，不是错误。 */
export function fetchStatOverview(query: StatQuery = {}, options?: RequestOptions): Promise<StatOverviewVO> {
  return get<StatOverviewVO>('/stats/overview', { ...query }, options)
}

/** 05 §5.8 第 33 条。返回的长度恒等于 `days`，缺失日期已补 0。 */
export function fetchStatTrend(query: StatTrendQuery = {}, options?: RequestOptions): Promise<StatTrendPointVO[]> {
  return get<StatTrendPointVO[]>('/stats/trend', { ...query }, options)
}

/** 05 §5.8 第 34 条。返回**根节点数组**；空数组 = 该科目还没有章节树。 */
export function fetchStatOutline(query: StatQuery = {}, options?: RequestOptions): Promise<StatOutlineNodeVO[]> {
  return get<StatOutlineNodeVO[]>('/stats/outline', { ...query }, options)
}

/** 05 §5.8 第 35 条。按掌握度升序，最多 `limit` 条。 */
export function fetchStatWeakKp(query: StatWeakKpQuery = {}, options?: RequestOptions): Promise<WeakKpVO[]> {
  return get<WeakKpVO[]>('/stats/weak-kp', { ...query }, options)
}
