package com.ruankao.repository.mapper;

import java.time.LocalDate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 每日活跃统计（{@code stat_daily}，04 §4.6；06 §6.10.2）。
 *
 * <p>只有<b>增量</b>写入：趋势图要的是"那天做了多少题"，是事件计数，
 * 没有"最近一次作答"的去重语义，因此同一题当天做两次就记两次——
 * 这与 {@code stat_user_outline} 的口径刻意不同（06 §6.10.2）。
 */
@Mapper
public interface StatDailyMapper {

    /**
     * 累加某人某天的一组计数。首次写入当天自动建行。
     *
     * @param durationSecDelta 单题耗时累加后取整秒；单题超过 10 分钟已在调用方截断
     *                         （04 §4.3），否则"挂机一下午"会把日均耗时拉到无意义的数量级
     */
    int increment(@Param("userId") Long userId,
                  @Param("statDate") LocalDate statDate,
                  @Param("answeredDelta") int answeredDelta,
                  @Param("correctDelta") int correctDelta,
                  @Param("durationSecDelta") int durationSecDelta);
}
