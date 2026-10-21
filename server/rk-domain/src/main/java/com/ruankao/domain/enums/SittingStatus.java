package com.ruankao.domain.enums;

/**
 * 场次状态（{@code exam_session_sitting.status}，02 §2.5.5）。
 *
 * <p>{@code PENDING} 的关键含义是"<b>不倒计时</b>"——高级科目两场次之间隔着午休，
 * 场次一结束后场次二停在 {@code PENDING}，等用户主动点"开始"才写 {@code started_at}
 * （对应评审 A5）。
 */
public enum SittingStatus {
    PENDING,
    IN_PROGRESS,
    FINISHED
}
