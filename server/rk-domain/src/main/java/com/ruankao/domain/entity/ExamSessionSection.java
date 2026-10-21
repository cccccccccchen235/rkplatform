package com.ruankao.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 会话分节（{@code exam_session_section}，02 §2.5.6）。
 *
 * <p>结构字段（{@code durationMin/durationMax/allowEarlySubmit/supportStatus/maxScore}）
 * 创建会话时**从模板逐行快照**，不 JOIN 回模板表——否则管理员修正模板会让历史成绩的
 * 分节归属与"无法模拟"标记随之漂移（06 §6.9.3，对应评审 G2）。
 */
@Getter
@Setter
@NoArgsConstructor
public class ExamSessionSection {

    private Long id;

    private Long sessionId;

    private Integer sittingNo;

    private Integer sectionNo;

    private String sectionName;

    private String sectionType;

    private Integer questionCount;

    private BigDecimal maxScore;

    private BigDecimal objectiveScore;

    private Integer durationMin;

    private Integer durationMax;

    private Boolean allowEarlySubmit;

    private String supportStatus;

    private Integer usedSec;

    private String status;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
