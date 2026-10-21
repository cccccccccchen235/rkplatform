package com.ruankao.domain.entity;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 卷面分节（{@code exam_paper_section}）。
 *
 * <p>本类承载的是**完整**的分节行（含判分与计时细节），但
 * {@code PaperSummaryVO.Section} 只下发作答相关字段——见
 * {@link com.ruankao.domain.vo.PaperSummaryVO} 的说明：{@code perQuestionScore}、
 * {@code durationMin/Max}、{@code allowEarlySubmit} 是考试进行中的服务端状态，
 * 提前下发等于把判分口径泄漏给客户端。读取时取全列、下发时裁剪，
 * 免得将来判分需要这些字段时再去改 SQL。
 */
@Getter
@Setter
@NoArgsConstructor
public class PaperSection {

    private Long id;

    private Long templateId;

    /** 所属场次（1/2），对应 {@code exam_paper_sitting.sitting_no}。 */
    private Integer sittingNo;

    /** 全局连续递增（跨场次连续，如高级为 1/2/3）。 */
    private Integer sectionNo;

    private String sectionName;

    /** OBJECTIVE / SUBJECTIVE / ESSAY。 */
    private String sectionType;

    private String questionType;

    private Integer questionCount;

    private BigDecimal perQuestionScore;

    /** 本节最早可离开的时间点（分钟）。 */
    private Integer durationMin;

    /** 本节最晚必须离开的时间点（分钟）。 */
    private Integer durationMax;

    private Boolean allowEarlySubmit;

    /** SUPPORTED / NOT_SUPPORTED（Office 操作题）。 */
    private String supportStatus;

    private Integer sortNo;
}
