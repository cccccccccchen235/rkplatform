package com.ruankao.domain.vo;

import java.time.LocalDateTime;

/**
 * 当前用户信息（05 §5.3 第 4 条）。
 *
 * <p>字段与 05 文档的同名表逐字对应，<b>多一个少一个都算契约变更</b>。
 *
 * <p>两个刻意的类型选择：
 * <ul>
 *   <li>{@code id} / {@code currentSubjectId} 是字符串。主键是 {@code BIGINT UNSIGNED}，
 *       JS 的 {@code Number} 在 2^53 之后丢精度（05 §5.1「ID 类型」）。</li>
 *   <li>时间字段是 {@link LocalDateTime} 而不是预格式化字符串：格式由 05 §5.1 统一规定为
 *       {@code yyyy-MM-dd HH:mm:ss}，在 Jackson 层一次性落实（{@code JacksonConfig}），
 *       而不是让每个 Converter 各写一遍 {@code format(...)}——后者迟早会出现两处格式不一致。</li>
 * </ul>
 *
 * <p>{@code mustChangePassword} 用基本类型：契约要求它恒为布尔值，前端凭它决定是否强制跳改密页
 * （06 §6.5，对应评审 C2），这里不留 null 三态。
 */
public record UserVO(
        String id,
        String username,
        String nickname,
        String role,
        String status,
        String currentSubjectId,
        boolean mustChangePassword,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt) {
}
