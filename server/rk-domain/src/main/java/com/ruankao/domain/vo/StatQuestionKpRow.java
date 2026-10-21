package com.ruankao.domain.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统计重算用的题目 → 知识点关联投影（{@code rk_question_kp}）。
 *
 * <p>与 {@link QuestionKpRowVO} 的区别：那个还要 {@code kp_name} 且只取
 * {@code status='ENABLED'} 的标签（考生端可见性）；统计口径按**题库事实**聚合，
 * 一个已停用的标签同样是这道题的考点，过滤掉会让同一道题在两个维度里对不上。
 */
@Getter
@Setter
@NoArgsConstructor
public class StatQuestionKpRow {

    private Long questionId;

    private Long kpId;
}
