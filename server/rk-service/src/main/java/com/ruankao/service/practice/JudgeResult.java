package com.ruankao.service.practice;

import java.math.BigDecimal;

/**
 * 判分结果（06 §6.7）。
 *
 * @param isCorrect 客观题 {@code true}/{@code false}；主观题与论文恒为 {@code null}——
 *                  "不判分"不是"判错"，落进 {@code user_answer_record.is_correct} 的也是
 *                  {@code NULL}（04 §4.3）。用包装类型而不是 {@code boolean} 就是为了让
 *                  "第三种取值"在类型上无法被忽略。
 * @param score     得分。客观题对 1 分、错 0 分；主观题恒 0（该列 {@code DECIMAL(4,1)}，
 *                  分值精度见 06 §6.7）。练习闭环不用它，模拟考分节汇总才用。
 * @param blank     归一化后是否为**空作答**。这一项必须往外传：06 §6.8 规定未作答的题
 *                  **不入错题本**，而"空"只有在归一化（去空白、转大写）之后才看得出来；
 *                  让调用方自己再实现一遍归一化，正是两处口径开始分叉的地方。
 *
 *                  <p>注意它**不等于** {@code isCorrect == false}：答错与没答都会得 0 分，
 *                  但只有答错才进错题本、才计入错误统计。
 */
public record JudgeResult(Boolean isCorrect, BigDecimal score, boolean blank) {

    /** 客观题单题分值：1.0（06 §6.7「客观题每题 1 分」）。 */
    private static final BigDecimal OBJECTIVE_SCORE = new BigDecimal("1.0");

    private static final BigDecimal ZERO = new BigDecimal("0.0");

    /** 客观题答对。 */
    public static JudgeResult correct() {
        return new JudgeResult(Boolean.TRUE, OBJECTIVE_SCORE, false);
    }

    /** 客观题答错（有作答、但不对）。 */
    public static JudgeResult wrong() {
        return new JudgeResult(Boolean.FALSE, ZERO, false);
    }

    /** 客观题空作答：判 false、得 0 分，但调用方**不得**把它写进错题本（06 §6.8）。 */
    public static JudgeResult blankAnswer() {
        return new JudgeResult(Boolean.FALSE, ZERO, true);
    }

    /**
     * 不判分（主观题、论文，或题型的库值不在枚举内这一异常情况）。
     *
     * <p>{@code blank} 取 {@code false}：调用方据此跳过错题本的是「未作答」，
     * 而这里说的是「这类题本来就不判分」，是两件事。
     */
    public static JudgeResult unscored() {
        return new JudgeResult(null, ZERO, false);
    }

    /** 本题是否产生了对错结论（客观题恒 true，主观题恒 false）。 */
    public boolean scored() {
        return isCorrect != null;
    }
}