package com.ruankao.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户章节统计（{@code stat_user_outline}，04 §4.6；口径见 06 §6.10）。
 *
 * <p>它是 {@code user_answer_record} 的**聚合缓存**：任何一个数字都能由流水重算，
 * 本表只是把"打开统计页"从一次全表扫描降成一次点查。因此这里承载的是
 * <b>重算结果</b>，不是事实——事实永远在流水表里。
 *
 * <p>{@code answeredCount} / {@code correctCount} / {@code accuracy} 是
 * <b>最近一次作答</b>口径（同一题重复作答只算最后一次，06 §6.10.2），
 * 与 {@code mastery} 的 {@code coverage = a / n} 共用同一个 {@code a}。
 */
@Getter
@Setter
@NoArgsConstructor
public class StatUserOutline {

    private Long id;

    private Long userId;

    private Long subjectId;

    /** 叶子章节 ID；{@code rk_question.outline_id} 与 {@code user_answer_record.outline_id} 都指向它。 */
    private Long outlineId;

    /** 该章节下用户已作答的**去重**题数（最近一次口径的 {@code a}）。 */
    private Integer answeredCount;

    /** 去重后最近一次答对的题数。 */
    private Integer correctCount;

    /** {@code correctCount / answeredCount}，无作答时为 0。 */
    private BigDecimal accuracy;

    /** 06 §6.10.1 的掌握度，保留 4 位小数。 */
    private BigDecimal mastery;

    /** 该章节下最后一次作答时间；无作答为 {@code null}。 */
    private LocalDateTime lastAnsweredAt;
}
