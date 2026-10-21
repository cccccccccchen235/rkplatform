package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（05 §5.3 第 1 条）。
 *
 * <p>校验规则与 05 文档的表逐条对应，同时与前端注册页的提示文案一致（07 §7.5.1）：
 * 用户名 {@code ^[a-zA-Z0-9_]{4,32}$}，密码 8~32 位且同时含字母与数字，昵称可空（缺省取用户名）。
 *
 * <p>密码的两条约束分开写而不是合成一条正则：两条规则各有各的提示文案，
 * 前端要按字段渲染错误列表，合成一条会让"长度不对"与"缺数字"变成同一句话。
 */
public record RegisterRequest(

        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{4,32}$", message = "用户名须为 4~32 位字母、数字或下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 32, message = "密码长度须为 8~32 位")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "密码须同时包含字母与数字")
        String password,

        @Size(max = 32, message = "昵称最长 32 位")
        String nickname) {
}
