package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 卷面模板（{@code exam_paper_template}）。一行对应一个科目（{@code uk_subject(subject_id)}）。
 *
 * <p>只被详情接口 {@code GET /subjects/{id}} 用来装配 {@code paperSummary}（05 §5.4 第 9 条）。
 * 不在本类里做任何 {@code NOT_SUPPORTED} 分节的扣减——那是分节层的信息，
 * 模板行的两个满分列在写库时就已按该口径算好（04 §4.6）。
 *
 * <p>与 {@link Subject} 同规矩：不带 MyBatis 注解，映射在 XML 里。
 */
@Getter
@Setter
@NoArgsConstructor
public class PaperTemplate {

    private Long id;

    private Long subjectId;

    private String name;

    /** JUNIOR_MID / SENIOR。 */
    private String paperType;

    /** 卷面满分 = Σ 各场次 total_max_score（NOT_SUPPORTED 分节不计）。 */
    private Integer totalMaxScore;

    /** 可自动判分满分 = Σ 各场次（仅客观题节）。 */
    private Integer autoScoredMaxScore;

    private String remark;

    /** ENABLED / DISABLED；详情接口只认 ENABLED。 */
    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
