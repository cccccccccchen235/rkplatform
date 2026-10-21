package com.ruankao.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 成绩详情（05 §5.6 第 24 条）。
 *
 * <p>{@code items} 是含答案与解析的 {@link QuestionDetailVO}，只在交卷后返回；
 * 考试期间的下发路径是 {@link ExamSessionVO}（那里的 {@link ExamItemVO} 不含答案）。
 */
public record ExamResultVO(
        String sessionId,
        String sessionNo,
        String subjectName,
        BigDecimal objectiveScore,
        Integer autoScoredMaxScore,
        Integer totalMaxScore,
        BigDecimal accuracy,
        Integer usedSec,
        LocalDateTime submittedAt,
        LocalDateTime scoredAt,
        List<ExamResultSittingVO> sittingResults,
        List<ExamResultSectionVO> sectionResults,
        List<QuestionDetailVO> items) {
}
