package com.ruankao.domain.entity;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户知识点统计（{@code stat_user_kp}，04 §4.6；口径见 06 §6.10）。
 *
 * <p>它是"薄弱知识点"画像的数据来源，也是练习组卷 {@code onlyUnmastered} 过滤器
 * 唯一读的表（05 §5.5 第 12 条：{@code mastery >= 0.6} 视为已掌握）。
 * <b>表里没有行 = 没有掌握度画像</b>，过滤器就会退化成空操作——这正是
 * P4 阶段 {@code onlyUnmastered} 不生效的原因（26 §7 偏离 1）。
 */
@Getter
@Setter
@NoArgsConstructor
public class StatUserKp {

    private Long id;

    private Long userId;

    private Long subjectId;

    private Long kpId;

    /** 该知识点下用户已作答的**去重**题数（一道题挂多个标签时每个标签各计一次）。 */
    private Integer answeredCount;

    private Integer correctCount;

    private BigDecimal accuracy;

    /** 06 §6.10.1 的掌握度，保留 4 位小数。 */
    private BigDecimal mastery;
}
