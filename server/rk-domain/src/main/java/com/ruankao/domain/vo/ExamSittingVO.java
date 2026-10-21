package com.ruankao.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话场次（05 §5.6 的 {@code SittingVO}）。
 *
 * <p>{@code PENDING} 时 {@code remainingSec == totalDurationSec} 且 {@code startedAt == null}，
 * 前端据此显示"开始本场次"按钮而不是直接倒计时（场次之间不自动衔接，评审 A5）。
 */
public record ExamSittingVO(
        Integer sittingNo,
        String sittingName,
        Integer totalDurationSec,
        Integer remainingSec,
        String status,
        Integer currentSectionNo,
        Integer totalMaxScore,
        Integer autoScoredMaxScore,
        BigDecimal objectiveScore,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<ExamSectionVO> sections) {
}
