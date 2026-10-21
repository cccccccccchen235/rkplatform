package com.ruankao.service.user;

import com.ruankao.domain.dto.RegisterRequest;
import com.ruankao.domain.vo.UserVO;

/**
 * 账号与当前用户（05 §5.3）。
 *
 * <p><b>令牌不在这里签发。</b>{@code StpUtil} 属于 web 层（sa-token 依赖只存在于 rk-api 的 pom，
 * 06 §6.1 的依赖方向不允许 rk-service 反向依赖它），所以本接口只回答"这个人是谁、凭据对不对"，
 * "给不给他令牌"由 {@code AuthController} 决定。这样切分还有一个好处：
 * 积分制、双因素、登录审计这类将来可能出现的机制，都有地方可放，不必改动业务层。
 */
public interface UserService {

    /**
     * 注册并返回新用户。
     *
     * @throws com.ruankao.common.exception.BizException 2003 用户名已存在；1001 由参数校验先行拦截
     */
    UserVO register(RegisterRequest request);

    /**
     * 校验登录凭据，成功时记录登录时间并清零失败计数。
     *
     * @throws com.ruankao.common.exception.BizException 2001 用户名或密码错误；2002 账号已禁用；2004 失败次数过多
     */
    UserVO verifyLogin(String username, String rawPassword);

    /**
     * 按 ID 取用户信息（{@code GET /users/me}）。
     *
     * @throws com.ruankao.common.exception.BizException 2005 该 ID 已不存在（令牌比用户活得久）
     */
    UserVO getById(Long userId);

    /**
     * 修改昵称（{@code PUT /users/me}）。返回更新后的用户，供前端直接回写 store。
     *
     * @throws com.ruankao.common.exception.BizException 2005 用户已不存在
     */
    UserVO updateNickname(Long userId, String nickname);

    /**
     * 修改密码（{@code PUT /users/me/password}）。成功后 {@code mustChangePassword} 置 false
     * （06 §6.5）。
     *
     * <p><b>与会话失效无关</b>：服务端不在这里踢掉会话，由前端改密成功后主动调
     * {@code POST /auth/logout} 并跳登录（07 §7.5.1 的"改密成功后强制登出"）。
     *
     * @throws com.ruankao.common.exception.BizException 2006 原密码错误；2005 用户已不存在
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 设置当前备考科目（{@code PUT /users/me/current-subject}）。返回更新后的用户。
     *
     * @throws com.ruankao.common.exception.BizException 3001 科目不存在或已停用；
     *                                                   1001 科目 ID 非法；2005 用户已不存在
     */
    UserVO updateCurrentSubject(Long userId, String subjectId);
}
