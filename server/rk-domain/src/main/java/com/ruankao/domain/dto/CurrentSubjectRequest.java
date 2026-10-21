package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 设置当前备考科目（05 §5.3 第 7 条）。
 *
 * <p>{@code subjectId} 用 {@code String} 而不是 {@code Long}，与 05 §5.1 的 ID 约定一致
 * （前端传字符串，避免 BIGINT 在 JS 里丢精度）；转数字与合法性判定在服务层完成，
 * 同时在那里校验"科目存在且启用"（回 {@code 3001}），不在这里猜。
 */
public record CurrentSubjectRequest(

        @NotBlank(message = "科目 ID 不能为空")
        String subjectId) {
}
