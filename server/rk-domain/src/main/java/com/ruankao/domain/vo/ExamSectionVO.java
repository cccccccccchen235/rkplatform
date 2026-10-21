package com.ruankao.domain.vo;

import java.math.BigDecimal;

/**
 * 会话分节（05 §5.6 的 {@code SectionVO}）。
 *
 * <p>{@code perQuestionScore} 由 {@code maxScore / questionCount} 派生：会话分节表只快照了
 * {@code max_score}，再存一个每题分值只会多一个可能与满分不一致的列。
 */
public record ExamSectionVO(
        Integer sittingNo,
        Integer sectionNo,
        String sectionName,
        String sectionType,
        Integer questionCount,
        BigDecimal maxScore,
        BigDecimal perQuestionScore,
        Integer durationMin,
        Integer durationMax,
        Boolean allowEarlySubmit,
        Integer usedSec,
        String supportStatus,
        String status) {
}
