package com.ruankao.api.config;

import cn.dev33.satoken.stp.StpInterface;
import com.ruankao.domain.vo.UserVO;
import com.ruankao.service.user.UserService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 角色解析：Sa-Token 的 {@link StpInterface} 实现（06 §6.5 第 148 行点名要求本类）。
 *
 * <p><b>为什么必须有这个类。</b>{@code @SaCheckRole("ADMIN")} 只表达"要求什么角色"，
 * 而"这个 loginId 到底有哪些角色"必须由应用回答——Sa-Token 自身没有任何角色来源。
 * 缺了这个实现，角色列表恒为空，三个管理端控制器（{@code AdminSubjectController} /
 * {@code AdminOutlineController} / {@code AdminKnowledgePointController}）
 * 对<b>任何人</b>都返回 {@code 403 + 1004}，<b>包括角色为 ADMIN 的用户</b>。
 *
 * <p>落点在 rk-api，理由与 {@code UserService} 把"签发令牌"留给 web 层一致：
 * {@link StpInterface} 是 Sa-Token 的 SPI，而 sa-token 依赖只声明在 rk-api
 * （06 §6.1 的依赖方向不允许 rk-service 反向依赖它）。
 *
 * <p>loginId 的取值：{@code AuthController} 用 {@code StpUtil.login(Long.valueOf(user.id()))}，
 * 所以 loginId 就是 {@code sys_user.id}。
 *
 * <p><b>已知边界（文档未规定，如实登记、未擅自扩展）</b>：本实现只看 {@code sys_user.role}。
 * 一个已被 {@code DISABLED} 但令牌尚未过期的用户，在其令牌有效期内仍能通过角色校验；
 * 登录时的 {@code 2002} 只挡住"重新登录"。若要让禁用立即生效，需要在令牌超时之外
 * 再加一层状态校验或主动踢下线。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    private static final Logger log = LoggerFactory.getLogger(StpInterfaceImpl.class);

    private final UserService userService;

    public StpInterfaceImpl(UserService userService) {
        this.userService = userService;
    }

    /**
     * 本项目用"角色制"，没有细粒度权限点（05 §5.13 的接口全部用 {@code @SaCheckRole}），
     * 故权限列表恒为空。将来若引入权限点，在这里接一张表即可，不动控制器。
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    /**
     * 取用户的角色。
     *
     * <p><b>失败一律返回空列表（fail-closed）</b>：无角色 → {@code 403}，不会因为解析异常而放行。
     * 取不到用户（令牌比用户活得久）时<b>不抛异常</b>——把一个 {@code 2005} 混进鉴权判定里，
     * 会让"角色不足"与"会话失效"两种情况的响应变得不可预测。
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = toUserId(loginId);
        if (userId == null) {
            return List.of();
        }
        try {
            UserVO user = userService.getById(userId);
            String role = user == null ? null : user.role();
            return (role == null || role.isBlank()) ? List.of() : List.of(role);
        } catch (RuntimeException ex) {
            // 记 DEBUG：这是可预期的控制流（用户已不存在），不该在 INFO 级别制造噪音。
            if (log.isDebugEnabled()) {
                log.debug("角色解析失败，按无角色处理 loginId={}", userId, ex);
            }
            return List.of();
        }
    }

    /** loginId 在本项目是 Long；容错处理字符串形态，避免将来换登录方式时静默失效。 */
    private Long toUserId(Object loginId) {
        if (loginId instanceof Long value) {
            return value;
        }
        try {
            return loginId == null ? null : Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}