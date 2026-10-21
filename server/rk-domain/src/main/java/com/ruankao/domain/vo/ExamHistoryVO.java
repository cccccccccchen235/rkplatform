package com.ruankao.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 历史成绩列表项（05 §5.6 第 17 条）。 */
public record ExamHistoryVO(
        String sessionId,
        String sessionNo,
        String subjectId,
        String subjectName,
        String status,
        BigDecimal objectiveScore,
        Integer autoScoredMaxScore,
        BigDecimal accuracy,
        Integer usedSec,
        LocalDateTime submittedAt) {
}
