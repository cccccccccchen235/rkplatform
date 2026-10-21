package com.ruankao.domain.vo;

import java.math.BigDecimal;

/**
 * 每日趋势的一个点（05 §5.8 第 33 条）。
 *
 * <p>{@code date} 是 {@code yyyy-MM-dd} 字符串而不是日期对象：趋势图的横轴是"日历格子"，
 * 缺失日期必须由服务端补零后按顺序连成一条线（10 §10.8 的验收项）。
 *
 * @param accuracy 该日正确率；无作答的补零点为 0（不是 {@code null}——折线图需要一个数）
 */
public record StatTrendPointVO(
        String date,
        int answeredCount,
        int correctCount,
        BigDecimal accuracy,
        int durationSec) {
}
