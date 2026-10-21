package com.ruankao.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会话题目快照（{@code exam_session_item}，02 §2.4.3）。
 *
 * <p>题干、选项、正确答案在**组卷时**快照，保证题目被编辑或下架后历史成绩仍可复现；
 * {@code referenceSnapshot}/{@code analysisSnapshot} 在**交卷时**才写入，避免组卷时搬运大字段。
 */
@Getter
@Setter
@NoArgsConstructor
public class ExamSessionItem {

    private Long id;

    private Long sessionId;

    private Integer sectionNo;

    /** 全卷题序，从 1 开始。 */
    private Integer sequence;

    private Long questionId;

    private String questionType;

    private String stemSnapshot;

    /** 选项数组的 JSON（{@code [{"key","content"}]}）；主观题为 {@code null}。 */
    private String optionsSnapshot;

    private String correctAnswer;

    private String referenceSnapshot;

    private String analysisSnapshot;

    private BigDecimal score;

    private String userAnswer;

    private Boolean isCorrect;

    private Integer durationMs;

    private LocalDateTime answeredAt;
}
