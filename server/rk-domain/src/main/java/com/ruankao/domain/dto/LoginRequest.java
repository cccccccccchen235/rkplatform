package com.ruankao.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求（05 §5.3 第 2 条）。
 *
 * <p><b>刻意不做用户名与密码的格式校验</b>，只判空。理由：登录接口的失败语义是
 * 2001「用户名或密码错误」，不是 1001「参数校验失败」。若把注册时的正则搬到这里，
 * 一个用旧规则注册的老用户（或任何拼错用户名的输入）会收到"参数不合法"，
 * 既泄漏了系统的校验规则，也让用户拿不到"用户名或密码错误"这个正确结论。
 */
public record LoginRequest(

        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password) {
}
