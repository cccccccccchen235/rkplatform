package com.ruankao.domain.enums;

/**
 * 分节状态（{@code exam_session_section.status}，02 §2.5.6）。
 *
 * <p>分节**不独立计时**：它的用时完全由所属场次的 {@code started_at} 派生。
 * {@code SKIPPED} 是"本平台无法模拟"（{@code NOT_SUPPORTED}，如 Office 操作题），
 * 创建会话时直接置位、不生成 item、不计分。
 */
public enum SectionStatus {
    PENDING,
    IN_PROGRESS,
    FINISHED,
    SKIPPED
}
