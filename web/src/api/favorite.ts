// 收藏（05 §5.7 第 29~31 条）。
//
// 收藏与取消收藏都是**幂等**的（05 §5.7）：对已收藏的题再 `POST`、对未收藏的题
// 再 `DELETE` 都不会报错。前端据此可以做乐观更新——失败时回滚即可，不必先查询当前状态。

import type { PageResult, QuestionVO } from '@/types/api'
import { del, get, post, type RequestOptions } from '@/utils/request'

export interface FavoriteQuery {
  subjectId?: string
  pageNum?: number
  pageSize?: number
}

/** 05 §5.7 第 29 条。请求体是 `{ questionId }`；重复收藏不报错。 */
export function addFavorite(questionId: string): Promise<void> {
  return post<void>('/favorites', { questionId })
}

/** 05 §5.7 第 30 条。取消收藏；未收藏时也不报错。 */
export function removeFavorite(questionId: string): Promise<void> {
  return del<void>(`/favorites/${questionId}`)
}

/** 05 §5.7 第 31 条。分页返回 `QuestionVO`（不含答案）。 */
export function fetchFavorites(query: FavoriteQuery, options?: RequestOptions): Promise<PageResult<QuestionVO>> {
  return get<PageResult<QuestionVO>>('/favorites', { ...query }, options)
}