package com.ruankao.repository.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 用户科目统计（{@code stat_user_subject}，04 §4.6；06 §6.10.2）。
 *
 * <p>两个写入点职责分离：{@link #incrementCounters} 在答题上报事务里累加**事件计数**
 * （累计做题量/正确率），{@link #updateMastery} 由掌握度重算独占写 {@code mastery}。
 * 分开是为了让"重算"只需回填一个字段，不会把增量累出来的计数抹回快照。
 */
@Mapper
public interface StatUserSubjectMapper {

    /**
     * 累加某用户某科目的作答计数。
     *
     * @param masteryPrior 新建行时的掌握度先验（{@code mastery.prior}，默认 0.5）。
     *                     没有作答历史时掌握度必须是先验而不是 0，否则统计页一上来
     *                     就是"我什么都没干就已经失败"（06 §6.10.1）。已有行的
     *                     {@code mastery} 由重算维护，本方法不覆盖。
     */
    int incrementCounters(@Param("userId") Long userId,
                          @Param("subjectId") Long subjectId,
                          @Param("answeredDelta") int answeredDelta,
                          @Param("correctDelta") int correctDelta,
                          @Param("masteryPrior") BigDecimal masteryPrior,
                          @Param("answeredAt") LocalDateTime answeredAt);

    /**
     * 回填科目掌握度（06 §6.10.1：{@code Σ(章节 mastery × 章节题量) / Σ题量}）。
     *
     * <p>只更新 {@code mastery} 一列：计数列由答题上报增量维护，重算不该碰它们，
     * 否则一次夜间全量重算会与正在写入的上报互相覆盖。
     */
    int updateMastery(@Param("userId") Long userId,
                      @Param("subjectId") Long subjectId,
                      @Param("mastery") BigDecimal mastery);

    /**
     * 记一次模拟考成绩（{@code exam_count + 1}、{@code best_score} 取历史最高）。
     *
     * <p>与答题计数分开：模考次数与最高分只在**交卷**时更新一次，而答题计数是每批上报累加的；
     * 混在一个方法里会让"这次到底是不是一次完整的模考"变得含糊。
     * {@code ABANDONED} 的会话不调用本方法（05 §5.6 第 25 条：不计入统计）。
     */
    int recordExam(@Param("userId") Long userId,
                   @Param("subjectId") Long subjectId,
                   @Param("score") BigDecimal score);
}
