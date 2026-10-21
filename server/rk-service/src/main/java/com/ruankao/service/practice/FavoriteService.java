package com.ruankao.service.practice;

import com.ruankao.common.api.PageQuery;
import com.ruankao.common.api.PageResult;
import com.ruankao.domain.vo.QuestionVO;

/**
 * 题目收藏（05 §5.7 第 29~31 条）。
 *
 * <p><b>收藏与取消都是幂等的</b>，不存在的收藏关系不报错：前端对收藏做乐观更新
 * （07 §7.4.3），用户双击、或断网重试都会发出重复的 POST/DELETE，为这类重复
 * 弹一个错误提示只会让人以为功能坏了。这与错题本的"移出"刻意不同——
 * 那里是"这条不在你的错题本里"这一状态不一致值得被看见，而收藏只是一个开关。
 */
public interface FavoriteService {

    /** 收藏（幂等）。题目不存在抛 3002。 */
    void add(Long userId, Long questionId);

    /** 取消收藏（幂等）。 */
    void remove(Long userId, Long questionId);

    /** 收藏列表（05 §5.7 第 31 条）：按收藏时间倒序分页，返回不含答案的 {@link QuestionVO}。 */
    PageResult<QuestionVO> list(Long userId, Long subjectId, PageQuery page);
}