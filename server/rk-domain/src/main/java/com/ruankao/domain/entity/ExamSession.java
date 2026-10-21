package com.ruankao.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 模拟考会话（{@code exam_session}，04 §4.7 前身；状态机见 02 §2.5.2）。
 *
 * <p>{@code remainingSec} 是**展示用缓存**（当前场次那个值的镜像），真值由
 * {@code exam_session_sitting.started_at} 每次重算——判定从不读它（06 §6.9.1，对应评审 A1/G4）。
 */
@Getter
@Setter
@NoArgsConstructor
public class ExamSession {

    private Long id;

    private String sessionNo;

    private Long userId;

    private Long subjectId;

    private Long templateId;

    private String status;

    /** 全场次总时长之和（秒，展示用）。 */
    private Integer totalDurationSec;

    /** 当前场次剩余秒数的镜像；读取一律以场次表为准。 */
    private Integer remainingSec;

    private Integer currentSittingNo;

    private Integer currentSectionNo;

    private Integer questionCount;

    private BigDecimal objectiveScore;

    private Integer autoScoredMaxScore;

    private Integer totalMaxScore;

    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    private LocalDateTime scoredAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
