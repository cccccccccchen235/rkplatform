package com.ruankao.domain.enums;

/**
 * 模拟考会话状态（{@code exam_session.status}，02 §2.5.2）。
 *
 * <pre>
 * CREATED ──首次开始场次──→ IN_PROGRESS ──全部场次 FINISHED + 交卷──→ SUBMITTED ──判分──→ SCORED
 *    └──用户主动放弃──→ ABANDONED（不计入成绩统计）
 * </pre>
 */
public enum ExamStatus {
    CREATED,
    IN_PROGRESS,
    SUBMITTED,
    SCORED,
    ABANDONED
}
