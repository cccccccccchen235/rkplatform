package com.ruankao.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会话场次（{@code exam_session_sitting}，02 §2.5.5）。
 *
 * <p><b>计时真值的唯一来源是本行的 {@code started_at}</b>：{@code remaining_sec} 只是缓存，
 * 每次读取按 {@code total_duration_sec - (now - started_at)} 重算。
 */
@Getter
@Setter
@NoArgsConstructor
public class ExamSessionSitting {

    private Long id;

    private Long sessionId;

    private Integer sittingNo;

    private String sittingName;

    private Integer totalDurationSec;

    private Integer remainingSec;

    private String status;

    private Integer currentSectionNo;

    private BigDecimal objectiveScore;

    /** PENDING 时为 {@code null}（未启动）。 */
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
