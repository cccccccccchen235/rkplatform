package com.ruankao.domain.vo;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** {@code stat_user_subject} 的读取投影（05 §5.8 第 32 条的科目维度字段）。 */
@Getter
@Setter
@NoArgsConstructor
public class StatUserSubjectRow {

    private Integer answeredCount;

    private Integer correctCount;

    private BigDecimal accuracy;

    private BigDecimal mastery;

    private Integer examCount;

    /** 最高客观题得分；未考过为 {@code null}。 */
    private BigDecimal bestScore;
}
