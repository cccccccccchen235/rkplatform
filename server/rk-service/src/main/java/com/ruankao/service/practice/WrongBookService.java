package com.ruankao.service.practice;

import com.ruankao.common.api.PageQuery;
import com.ruankao.common.api.PageResult;
import com.ruankao.domain.vo.WrongQuestionVO;

/**
 * 错题本（06 §6.8、05 §5.7 第 26~28 条）。
 *
 * <p>状态机很小但有三条容易被写错的口径，逐条写在实现类里：
 * <ul>
 *   <li><b>未作答不收录</b>——"没做完"不是"不会做"；</li>
 *   <li><b>连续答对 N 次转 {@code MASTERED}</b>，N 取自 {@code sys_config}（默认 2）；</li>
 *   <li>答错会<b>把已移除的题重新拉回</b> {@code ACTIVE}。</li>
 * </ul>
 */
public interface WrongBookService {

    /**
     * 记录一道题的对错（06 §6.8）。由答题上报与模拟考交卷共同调用。
     *
     * <p>空作答与不判分的题型在这里被直接跳过（不收录、不更新）。
     */
    void onAnswered(Long userId, Long questionId, Long subjectId, JudgeResult result);

    /**
     * 错题列表（05 §5.7 第 26 条）。
     *
     * @param status 默认 {@code ACTIVE}；可选 {@code MASTERED} / {@code REMOVED}
     * @param sortBy {@code wrongCount} 或 {@code lastAnsweredAt}（默认）
     */
    PageResult<WrongQuestionVO> list(Long userId, Long subjectId, String status,
                                     Long outlineId, Long kpId, String sortBy, PageQuery page);

    /** 移出错题本（置 {@code REMOVED}）。不在本子里时抛 1003。 */
    void remove(Long userId, Long questionId);

    /** 恢复到错题本（置 {@code ACTIVE} 且连续答对清零）。不在本子里时抛 1003。 */
    void restore(Long userId, Long questionId);
}