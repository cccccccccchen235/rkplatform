package com.ruankao.service.exam;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 计时口径（06 §6.9.1）——模拟考最致命的一类缺陷（`10 §10.11` R6）的唯一落点。
 *
 * <p>三条铁律，都用纯函数表达：
 * <ol>
 *   <li><b>时间是服务端算的</b>：入参 {@code now} 由调用方注入，本类不认识 {@code LocalDateTime.now()}，
 *       因此单测可以把"本机时间调快 2 小时"直接构造出来，不需要改系统时钟。</li>
 *   <li><b>{@code PENDING} 不倒计时</b>：{@code startedAt == null} 时剩余时长恒等于总时长。
 *       高级科目两场次之间隔着午休，这条保证午休不扣时（评审 A5）。</li>
 *   <li><b>不存增量状态</b>：剩余时长永远由 {@code started_at} 派生，断点续答、换浏览器、
 *       进程重启都不会"重置"或"累积"计时。</li>
 * </ol>
 */
public final class ExamTiming {

    private ExamTiming() {
    }

    /**
     * 场次剩余秒数。{@code startedAt} 为 null（未启动）时返回总时长。
     * 已耗尽时返回 0，不返回负数。
     */
    public static int remainingSec(LocalDateTime startedAt, int totalDurationSec, LocalDateTime now) {
        if (startedAt == null) {
            return totalDurationSec;
        }
        long elapsed = Duration.between(startedAt, now).getSeconds();
        return (int) Math.max(0, totalDurationSec - elapsed);
    }

    /** 某段时间起点到现在的已用秒数；起点为空返回 0。 */
    public static int usedSec(LocalDateTime startedAt, LocalDateTime now) {
        if (startedAt == null) {
            return 0;
        }
        return (int) Math.max(0, Duration.between(startedAt, now).getSeconds());
    }

    /** 场次是否已耗尽（用于惰性结算）：只有进行中且剩余为 0 才算。 */
    public static boolean exhausted(LocalDateTime startedAt, int totalDurationSec, LocalDateTime now) {
        return startedAt != null && remainingSec(startedAt, totalDurationSec, now) <= 0;
    }
}
