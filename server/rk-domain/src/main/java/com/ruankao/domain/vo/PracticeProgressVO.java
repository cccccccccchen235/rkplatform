package com.ruankao.domain.vo;

import java.time.LocalDateTime;

/**
 * 练习进度（05 §5.5 第 14 条）。字段即契约，改动需同步 05 与本类。
 *
 * <p>{@code lastSequence}/{@code lastQuestionId} 是"继续上次"的定位游标；
 * {@code answeredCount}/{@code totalCount} 用于进度条（分母为本次范围的总题数）。
 * 所有 ID 为 {@code String}（05 §5.1：ID 以字符串下发，避免超过 2^53 的 JS 精度丢失）。
 *
 * <p>{@code updatedAt} 用 {@link LocalDateTime}：全局 Jackson 配置已按
 * {@code yyyy-MM-dd HH:mm:ss} 序列化，VO 不自行格式化、也不改用字符串。
 */
public record PracticeProgressVO(String subjectId,
                                 String mode,
                                 String refId,
                                 Integer lastSequence,
                                 String lastQuestionId,
                                 Integer totalCount,
                                 Integer answeredCount,
                                 LocalDateTime updatedAt) {
}