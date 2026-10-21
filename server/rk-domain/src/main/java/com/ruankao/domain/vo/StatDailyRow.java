package com.ruankao.domain.vo;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 一天的聚合行：既用于 {@code stat_daily} 的单日点查，也用于从流水现算的趋势分组。
 *
 * <p>用可变类而非 {@code record}：MyBatis 自动映射走 setter（见 {@link QuestionKpRowVO} 的说明）。
 */
@Getter
@Setter
@NoArgsConstructor
public class StatDailyRow {

    private LocalDate statDate;

    private Integer answeredCount;

    private Integer correctCount;

    private Integer durationSec;
}
