package com.ruankao.service.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 计时口径的单测（06 §6.9.1）。这是 {@code 10 §10.11} R6（"计时正确性 bug 是最致命的缺陷"）
 * 的直接对策：把"本机时间调快 2 小时"这件事构造成一个入参，不需要改系统时钟。
 */
class ExamTimingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 9, 0);
    private static final int TOTAL = 240 * 60;

    @Test
    @DisplayName("PENDING（started_at 为空）不倒计时")
    void pendingDoesNotCountDown() {
        assertEquals(TOTAL, ExamTiming.remainingSec(null, TOTAL, NOW));
        assertFalse(ExamTiming.exhausted(null, TOTAL, NOW));
    }

    @Test
    @DisplayName("剩余时长由服务端 started_at 派生")
    void remainingDerivesFromServerTime() {
        LocalDateTime startedAt = NOW.minusMinutes(30);
        assertEquals(TOTAL - 30 * 60, ExamTiming.remainingSec(startedAt, TOTAL, NOW));
    }

    @Test
    @DisplayName("客户端时间无关：把 now 调快 2 小时，剩余时长照减 2 小时")
    void clientClockPlaysNoRole() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 10, 5, 8, 0);
        // 服务端此刻是 9:00 → 已过 1 小时
        assertEquals(60 * 60, ExamTiming.remainingSec(startedAt, 2 * 60 * 60, NOW));
        // 服务端此刻是 11:00 → 已过 3 小时 → 耗尽
        assertEquals(0, ExamTiming.remainingSec(startedAt, 2 * 60 * 60, NOW.plusHours(2)));
    }

    @Test
    @DisplayName("已耗尽返回 0 且判定为超时，不返回负数")
    void exhaustedIsZeroNotNegative() {
        LocalDateTime startedAt = NOW.minusSeconds(TOTAL + 5);
        assertEquals(0, ExamTiming.remainingSec(startedAt, TOTAL, NOW));
        assertTrue(ExamTiming.exhausted(startedAt, TOTAL, NOW));
    }

    @Test
    @DisplayName("已用时长：起点为空返回 0")
    void usedSecFromStart() {
        assertEquals(0, ExamTiming.usedSec(null, NOW));
        assertEquals(90, ExamTiming.usedSec(NOW.minusSeconds(90), NOW));
    }
}
