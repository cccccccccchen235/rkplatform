package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 错题本条目（{@code user_wrong_question}）。
 *
 * <p>收录与移出规则（04 §4.3、06 §6.8）：答错即收录（{@code wrong_count + 1}，
 * {@code consecutive_correct} 归零，{@code status} 置 {@code ACTIVE}，若原为 {@code REMOVED}
 * 则恢复）；答对令 {@code consecutive_correct + 1}，<b>连续答对达阈值</b>（默认 2，
 * 可配置 {@code wrongbook.master.threshold}）后置 {@code MASTERED}——移出可用列表但保留数据；
 * 用户也可手动置 {@code REMOVED}。
 *
 * <p>{@code subject_id} 是冗余列，避免错题列表回表 {@code rk_question}（04 §4.3）；
 * {@code uk_user_question(user_id, question_id)} 保证同一用户对同一题至多一条记录。
 */
@Getter
@Setter
@NoArgsConstructor
public class UserWrongQuestion {

    private Long id;

    private Long userId;

    private Long questionId;

    /** 冗余，避免列表查询回表 {@code rk_question}。 */
    private Long subjectId;

    private Integer wrongCount;

    private Integer correctCount;

    /** 连续答对计数；达阈值（默认 2）触发 {@code MASTERED}。不是"累计答对次数"。 */
    private Integer consecutiveCorrect;

    private LocalDateTime lastAnsweredAt;

    /** ACTIVE / MASTERED / REMOVED，见 {@link com.ruankao.domain.enums.WrongQuestionStatus}。 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}