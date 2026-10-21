package com.ruankao.domain.vo;

import java.math.BigDecimal;

/**
 * 成绩详情的场次汇总（05 §5.6 第 24 条）。
 *
 * @param answered 该场次是否有作答；false = 用户放弃本场次（如不写论文直接交卷）
 */
public record ExamResultSittingVO(
        Integer sittingNo,
        String sittingName,
        Integer totalDurationSec,
        Integer usedSec,
        BigDecimal objectiveScore,
        Integer autoScoredMaxScore,
        Integer totalMaxScore,
        String status,
        boolean answered) {
}
