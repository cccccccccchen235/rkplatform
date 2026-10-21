package com.ruankao.repository.mapper;

import com.ruankao.domain.entity.StatUserOutline;
import com.ruankao.domain.vo.StatDailyRow;
import com.ruankao.domain.vo.StatUserSubjectRow;
import com.ruankao.domain.vo.WeakKpRow;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 统计读取接口（05 §5.8）的查询。
 *
 * <p>与 {@link StatAggregationMapper} 的分工：那个是"Aggregator 该从哪里取原始事实"
 * （重算的输入），这个是"页面要看什么"（重算的输出与派生量）。两者都读，但改动的理由不同——
 * 重算口径变了改前者，页面要新字段改后者。
 *
 * <p>结果一律来自 {@code stat_*} 聚合缓存，只有趋势例外：它从流水现算。原因是
 * {@code stat_daily} 没有 {@code subject_id} 列（04 §4.6），而 05 §5.8 的趋势带
 * {@code subjectId?} 参数——要按科目过滤，只能回到流水分组。这一处偏离记在 {@code 28}。
 */
@Mapper
public interface StatReadMapper {

    /** 某用户某科目的科目级统计行；从未作答返回 {@code null}（由服务层落回零值与先验）。 */
    StatUserSubjectRow selectUserSubject(@Param("userId") Long userId,
                                         @Param("subjectId") Long subjectId);

    /**
     * 某科目在 {@code [sinceDate, 今天]} 内逐日的**事件**计数（不按题目去重）。
     *
     * <p>只统计客观题（{@code is_correct IS NOT NULL}）：主观题不判分，混进来会把
     * 分母做大、正确率算低。
     */
    List<StatDailyRow> selectDailyTrend(@Param("userId") Long userId,
                                        @Param("subjectId") Long subjectId,
                                        @Param("sinceDate") LocalDate sinceDate,
                                        @Param("limit") int limit);

    /** 累计有作答的天数（06 §6.10.3）。 */
    int countStudyDays(@Param("userId") Long userId);

    /**
     * 有作答的日期，从近到远；供连续学习天数遍历（06 §6.10.3 的 400 天上界）。
     */
    List<LocalDate> selectActiveDates(@Param("userId") Long userId,
                                      @Param("limit") int limit);

    /** 指定日期的每日统计；当天没作答返回 {@code null}。 */
    StatDailyRow selectDailyByDate(@Param("userId") Long userId,
                                   @Param("statDate") LocalDate statDate);

    /** 某用户某科目的章节统计行（只有练过的章节才有行）。 */
    List<StatUserOutline> selectOutlineStatRows(@Param("userId") Long userId,
                                                @Param("subjectId") Long subjectId,
                                                @Param("limit") int limit);

    /**
     * 薄弱知识点：{@code mastery < maxMastery}，按掌握度升序（05 §5.8 第 35 条）。
     *
     * <p>不过滤知识点 {@code status}：停用的标签也是"用户没掌握的考点"，
     * 从薄弱点里剔除它会让用户看不到自己真实的短板（与 {@code StatQuestionKpRow} 同一取舍）。
     */
    List<WeakKpRow> selectWeakKp(@Param("userId") Long userId,
                                 @Param("subjectId") Long subjectId,
                                 @Param("maxMastery") java.math.BigDecimal maxMastery,
                                 @Param("limit") int limit);
}
