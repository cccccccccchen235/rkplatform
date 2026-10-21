package com.ruankao.service.stat;

import com.ruankao.domain.vo.ActiveUserSubjectRow;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 学习统计的写入与聚合（06 §6.4、§6.10.2、§6.13）。
 *
 * <p>读取接口（05 §5.8 的总览 / 趋势 / 章节 / 薄弱点）尚未落地；本接口只负责
 * 把 {@code stat_*} 表维护成"随时可读"的状态，也就是 P4 欠的那笔
 * {@code onlyUnmastered} 数据债（26 §7 偏离 1）。
 *
 * <p>职责边界：<b>计数靠增量、掌握度靠重算</b>。前者无衰减、可在线累加；
 * 后者依赖"距今天数"，今天算对明天就可能变，只能按需重算。
 */
public interface StatisticsService {

    /**
     * 在答题上报的**同一事务**内累加计数类字段（{@code stat_daily}、
     * {@code stat_user_subject} 的 answered/correct/accuracy）。
     */
    void incrementCounters(Long userId,
                           Long subjectId,
                           int answeredDelta,
                           int correctDelta,
                           int durationSecDelta,
                           LocalDateTime answeredAt);

    /**
     * 点亮 {@code rk:stat:stale:{userId}:{subjectId}}，等下一次读取再重算。
     * 由答题上报在**事务提交后**调用。
     */
    void markStale(Long userId, Long subjectId);

    /**
     * 需要掌握度画像时调用：标记存在则重算该用户该科目，完成后清除标记。
     *
     * <p>当前唯一调用方是练习组卷的 {@code onlyUnmastered}（它直接读
     * {@code stat_user_kp.mastery}）；P6 的统计读取接口落地后应复用本入口。
     */
    void refreshIfStale(Long userId, Long subjectId);

    /** 无条件重算某用户某科目（夜间全量重算的单个待办）。 */
    void recompute(Long userId, Long subjectId);

    /**
     * 记一次模拟考成绩（05 §5.6 第 23 条）：{@code exam_count + 1}，{@code best_score} 取最高。
     *
     * <p>与 {@link #incrementCounters} 分开：那个是"每批答题"的事件计数，这个是"一次完整模考"
     * 的结果；{@code ABANDONED} 的会话不调用本方法（05 §5.6 第 25 条）。
     */
    void recordExam(Long userId, Long subjectId, java.math.BigDecimal objectiveScore);

    /** 近 {@code days} 天有作答的用户 × 其全部作答科目（夜间全量重算的待办清单）。 */
    List<ActiveUserSubjectRow> activeUserSubjects(int days, int limit);
}
