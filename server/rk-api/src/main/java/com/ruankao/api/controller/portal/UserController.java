package com.ruankao.api.controller.portal;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.ruankao.common.api.R;
import com.ruankao.domain.dto.CurrentSubjectRequest;
import com.ruankao.domain.dto.NicknameUpdateRequest;
import com.ruankao.domain.dto.PasswordUpdateRequest;
import com.ruankao.domain.vo.UserVO;
import com.ruankao.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户与个人中心（05 §5.3 第 4~7 条）。
 *
 * <p>四个方法都标 {@code @SaCheckLogin}：它们操作的都是"我自己"的数据。
 * 改密成功后<b>服务端不踢会话</b>，由前端主动登出（07 §7.5.1）——这与
 * {@code POST /auth/logout} 的职责分工一致，两个动作各自只做一件事。
 *
 * <p>用户 ID 一律取服务端会话里的值（03 §3.8 AC-3：访问用户数据必须携带 user_id，
 * 且这个 ID 不能来自请求参数），所以本控制器没有任何接收用户 ID 的入参。
 */
@RestController
@RequestMapping("/api/v1/users")
@Validated
@Tag(name = "用户", description = "05 §5.3：当前用户信息与个人中心")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @SaCheckLogin
    @Operation(summary = "当前用户信息", description = "登录态失效返回 2005 + HTTP 401。")
    public R<UserVO> me() {
        return R.ok(userService.getById(StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/me")
    @SaCheckLogin
    @Operation(summary = "修改昵称", description = "返回更新后的 UserVO，供前端直接回写 store。")
    public R<UserVO> updateNickname(@Valid @RequestBody NicknameUpdateRequest request) {
        return R.ok(userService.updateNickname(StpUtil.getLoginIdAsLong(), request.nickname()));
    }

    /**
     * 改密码（05 §5.3 第 6 条）。
     *
     * <p>原密码错误回 {@code 2006}，由页面就地提示；新密码复杂度沿用注册口径
     * （8~32 位含字母与数字）。成功后服务端把 {@code mustChangePassword} 置 false（06 §6.5）。
     */
    @PutMapping("/me/password")
    @SaCheckLogin
    @Operation(summary = "修改密码",
            description = "原密码错误返回 2006；成功后 mustChangePassword 置 false。"
                    + "服务端不踢会话，前端应在成功后调 /auth/logout 并跳登录。")
    public R<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(
                StpUtil.getLoginIdAsLong(), request.oldPassword(), request.newPassword());
        return R.ok();
    }

    @PutMapping("/me/current-subject")
    @SaCheckLogin
    @Operation(summary = "设置当前备考科目",
            description = "科目不存在或已停用返回 3001；返回更新后的 UserVO。")
    public R<UserVO> updateCurrentSubject(@Valid @RequestBody CurrentSubjectRequest request) {
        return R.ok(userService.updateCurrentSubject(StpUtil.getLoginIdAsLong(), request.subjectId()));
    }
}
