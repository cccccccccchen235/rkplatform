package com.ruankao.domain.vo;

import java.math.BigDecimal;

/** 成绩详情的分节明细（05 §5.6 第 24 条的 {@code sectionResults[]}）。 */
public record ExamResultSectionVO(
        Integer sittingNo,
        Integer sectionNo,
        String sectionName,
        String sectionType,
        BigDecimal maxScore,
        BigDecimal objectiveScore,
        String supportStatus,
        boolean scored) {
}
