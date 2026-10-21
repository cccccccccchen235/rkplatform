package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 题目反馈与版权投诉（{@code rk_question_feedback}）。
 *
 * <p>类型取值见 {@link com.ruankao.domain.enums.FeedbackType}；其中 {@code COPYRIGHT}
 * 在管理端为最高优先级，处理动作是下架题目（{@code PUBLISHED → OFFLINE}），
 * 下架后已生成的试卷快照保留、不再进新组卷（02 §2.6.3、04 §4.4）。
 *
 * <p>{@code status} 为 PENDING / PROCESSED / IGNORED（DDL 注释）；它是纯工作流字段，
 * 未枚举化，处理时按 DDL 字面量写入即可。
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionFeedback {

    private Long id;

    private Long questionId;

    /** 提交反馈的用户（AC-3：用户数据访问必须携带 user_id）。 */
    private Long userId;

    /** TYPO / WRONG_ANSWER / UNCLEAR_ANALYSIS / COPYRIGHT / OTHER，见 {@link com.ruankao.domain.enums.FeedbackType}。 */
    private String type;

    private String content;

    /** PENDING / PROCESSED / IGNORED。 */
    private String status;

    /** 处理人（管理员）用户 ID；未处理时为 {@code null}。 */
    private Long handlerId;

    private String handleRemark;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;
}