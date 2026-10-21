package com.ruankao.domain.vo;

import java.math.BigDecimal;

/**
 * 学习统计总览（05 §5.8 第 32 条）。
 *
 * <p>字段分成两组口径，前端不要把它们当成同一维度：
 * <ul>
 *   <li><b>科目维度</b>（{@code subjectId} 选中那门课）：{@code answeredCount} /
 *       {@code correctCount} / {@code accuracy} / {@code mastery} / {@code outlineCoverage} /
 *       {@code examCount} / {@code bestScore}。</li>
 *   <li><b>用户维度</b>（跨科目）：{@code studyDays} / {@code continuousDays} /
 *       {@code todayAnswered} / {@code todayAccuracy}。连续学习天数是"我这个人"的属性，
 *       按科目拆开就没有意义（06 §6.10.3）。</li>
 * </ul>
 *
 * @param mastery         科目掌握度；无作答时是<b>先验 0.5</b>而不是 0（06 §6.10.1）
 * @param bestScore       最高客观题得分；尚未考过模拟考时为 {@code null}
 * @param outlineCoverage 已练习叶子章节数 / 总叶子章节数
 */
public record StatOverviewVO(
        int answeredCount,
        int correctCount,
        BigDecimal accuracy,
        int studyDays,
        int continuousDays,
        BigDecimal mastery,
        int examCount,
        BigDecimal bestScore,
        BigDecimal outlineCoverage,
        int todayAnswered,
        BigDecimal todayAccuracy) {
}
