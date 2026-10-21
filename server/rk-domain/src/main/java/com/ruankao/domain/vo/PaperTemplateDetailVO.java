package com.ruankao.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理端卷面模板详情（05 §5.10 第 51 条）。
 *
 * <p>与考生端 {@link PaperSummaryVO} 的关键差别是<b>下发全字段</b>：
 * {@code perQuestionScore}、{@code durationMin/Max}、{@code allowEarlySubmit} 这些
 * 在考生端刻意裁掉的判分与计时细节，正是管理端要编辑的对象。
 *
 * <p>字段与 {@link com.ruankao.domain.dto.PaperTemplateSaveRequest} 逐字对应，
 * 所以本响应可以直接回填成保存请求体。
 */
public record PaperTemplateDetailVO(

        Long subjectId,

        String name,

        /** {@code JUNIOR_MID} / {@code SENIOR}。 */
        String paperType,

        /** 各场次时长之和（分钟）。服务端加出来，不是某一列的值。 */
        Integer totalDuration,

        Integer totalMaxScore,

        Integer autoScoredMaxScore,

        List<Sitting> sittings) {

    public record Sitting(

            Integer sittingNo,
            String sittingName,
            /** 本场次总时长（分钟）；场次内分节共享。 */
            Integer totalDuration,
            Integer totalMaxScore,
            Integer autoScoredMaxScore,
            Integer sortNo,
            List<Section> sections) {
    }

    public record Section(

            /** 跨场次全局连续（如高级为 1/2/3）。 */
            Integer sectionNo,
            String sectionName,
            /** OBJECTIVE / SUBJECTIVE / ESSAY。 */
            String sectionType,
            String questionType,
            Integer questionCount,
            BigDecimal perQuestionScore,
            /** 本节最早可离开的时间点（分钟）。 */
            Integer durationMin,
            /** 本节最晚可离开的时间点（分钟）。 */
            Integer durationMax,
            Boolean allowEarlySubmit,
            /** SUPPORTED / NOT_SUPPORTED。 */
            String supportStatus,
            Integer sortNo) {
    }
}
