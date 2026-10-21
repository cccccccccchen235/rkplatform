package com.ruankao.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话题目（05 §5.6 第 18 条的 {@code ExamItemVO}）。
 *
 * <p>不含 {@code correctAnswer}/{@code referenceAnswer}/{@code analysis}：考试期间下发答案
 * 等于把卷子送出去；含答案的是交卷后的成绩接口（{@link QuestionDetailVO}）。
 */
public record ExamItemVO(
        String itemId,
        String questionId,
        Integer sequence,
        Integer sectionNo,
        String questionType,
        String stem,
        List<QuestionImageVO> images,
        List<QuestionOptionVO> options,
        BigDecimal score,
        String userAnswer,
        LocalDateTime answeredAt,
        Integer durationMs) {
}
