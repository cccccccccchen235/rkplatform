package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.LoginRequest;
import com.ruankao.domain.dto.RegisterRequest;
import com.ruankao.domain.vo.AuthResultVO;
import com.ruankao.domain.vo.UserVO;
import com.ruankao.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册 / 登录 / 登出（05 §5.3 第 1~3 条）。
 *
 * <p>注册与登录在 {@link com.ruankao.api.config.AuthWhitelist} 里，故不标注解；
 * 登出需要登录态，标 {@code @SaCheckLogin}。ADR-02 的单元测试要求这两者必居其一。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "认证", description = "05 §5.3：注册、登录、登出")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "注册（成功后即为登录态）",
            description = "响应 data 与登录一致：token / expireAt / user。"
                    + "昵称缺省取用户名；用户名重复返回 2003。")
    public R<AuthResultVO> register(@RequestBody @Valid RegisterRequest request) {
        return R.ok(issueToken(userService.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "登录",
            description = "失败返回 2001；账号禁用 2002；同一用户名 15 分钟内失败 5 次返回 2004。")
    public R<AuthResultVO> login(@RequestBody @Valid LoginRequest request) {
        return R.ok(issueToken(userService.verifyLogin(request.username(), request.password())));
    }

    @PostMapping("/logout")
    @SaCheckLogin
    @Operation(summary = "登出", description = "服务端删除会话，不是前端删掉 token 就算数。")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    /**
     * 签发令牌并组装响应。
     *
     * <p>放在 rk-api 而不是 rk-service：{@code StpUtil} 是 web 层的凭据，业务层只回答
     * "这个人是谁"（见 {@link UserService} 的类注释）。
     */
    private static AuthResultVO issueToken(UserVO user) {
        StpUtil.login(Long.valueOf(user.id()));
        long timeoutSeconds = StpUtil.getTokenTimeout();
        // timeoutSeconds <= 0 表示会话不存在或永不过期（-1/-2），此时不给一个假的到期时刻，
        // 宁可让前端拿到 null 也不给它一个会误导"什么时候该重新登录"的时间。
        LocalDateTime expireAt = timeoutSeconds > 0
                ? LocalDateTime.now().plusSeconds(timeoutSeconds)
                : null;
        return new AuthResultVO(StpUtil.getTokenValue(), expireAt, user);
    }
}
