package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 修改密码（05 §5.3 第 6 条）。
 *
 * <p><b>新密码的复杂度沿用注册口径</b>（8~32 位且同时含字母与数字，05 §5.3 第 1 条）：
 * 如果"改密"比"注册"松，用户注册一个合规口令之后就能把它换成一个不合规的，
 * 那条注册规则等于只在第一天有效。旧密码只判非空——它可能是在规则收紧之前设的，
 * 在这里先按格式拦截，老用户就永远改不了密码。
 *
 * <p>两条约束分开写而不是合成一条正则，理由同 {@link RegisterRequest}：
 * 每条各有各的提示文案，前端要按字段渲染错误列表。
 */
public record PasswordUpdateRequest(

        @NotBlank(message = "原密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 8, max = 32, message = "新密码长度须为 8~32 位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "新密码须同时包含字母与数字")
        String newPassword) {
}
