package com.ruankao.service.user.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.common.security.PasswordHasher;
import com.ruankao.domain.dto.RegisterRequest;
import com.ruankao.domain.entity.SysUser;
import com.ruankao.domain.entity.Subject;
import com.ruankao.domain.vo.UserVO;
import com.ruankao.repository.dao.LoginAttemptDao;
import com.ruankao.repository.mapper.SubjectMapper;
import com.ruankao.repository.mapper.SysUserMapper;
import com.ruankao.service.user.UserConverter;
import com.ruankao.service.user.UserService;
import java.time.LocalDateTime;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    /**
     * 用户名不存在时用来消耗一次 BCrypt 比对的固定哈希（06 §6.5「用户名不存在时仍执行一次比对」）。
     *
     * <p><b>必须是真实产出的 BCrypt 串</b>，由 {@code rk-boot} 的
     * {@code --rk.password-gen='Dummy#Timing2026'} 生成，不是手写的假值：格式非法的哈希会让
     * {@link PasswordHasher#matches} 立刻返回 false 而不做任何计算，那样这条防护就等于不存在，
     * 而它在日志与代码上都看不出来（缺陷表现只是"响应快了 100ms"）。
     *
     * <p>它对应的明文口令是一次性随机串，不参与任何业务，泄露不构成风险。
     */
    private static final String TIMING_DUMMY_HASH =
            "$2a$10$Im1UpVYXaXlLvVh5HxhAc.1EZvOXO258W0LMXvnoJ7ehYLvBnEV5W";

    private final SysUserMapper sysUserMapper;

    private final LoginAttemptDao loginAttemptDao;

    /**
     * 设置当前科目要确认"这个科目真实存在且启用"，所以用户域用到了科目查询。
     *
     * <p>直接注入 {@link SubjectMapper} 而不是 {@code SubjectService}：后者是考生端读取
     * 科目详情/大纲的入口（带缓存与 VO 装配），而这里只需要一次主键查询 + 状态判断。
     * 为了一个布尔判断把整条读取链路拉进来，会让用户域莫名依赖大纲与缓存。
     */
    private final SubjectMapper subjectMapper;

    public UserServiceImpl(SysUserMapper sysUserMapper,
                           LoginAttemptDao loginAttemptDao,
                           SubjectMapper subjectMapper) {
        this.sysUserMapper = sysUserMapper;
        this.loginAttemptDao = loginAttemptDao;
        this.subjectMapper = subjectMapper;
    }

    @Override
    @Transactional
    public UserVO register(RegisterRequest request) {
        String username = request.username().trim();
        if (sysUserMapper.selectByUsername(username) != null) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(PasswordHasher.hash(request.password()));
        user.setNickname(resolveNickname(request.nickname(), username));
        try {
            sysUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 两个请求同时注册同一用户名：唯一键 uk_username 是唯一裁决者。
            // 先查后插只是"多数情况下更快的答案"，这里才是并发下的正确答案。
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        // 回读整行：created_at 等列由 DDL 默认值产生，只有读回来才是真正落库的值，
        // 在 Java 侧自己 now() 会给出一个"看起来对、与库不一致"的时间。
        return getById(user.getId());
    }

    @Override
    public UserVO verifyLogin(String username, String rawPassword) {
        String name = username.trim();

        // 1) 先看是否处于锁定期。放在校验之前，是为了让锁定真的"锁住"：
        //    否则一个被锁的账号只要碰对密码就能进去，锁定形同虚设。
        if (loginAttemptDao.failureCount(name) >= LoginAttemptDao.MAX_ATTEMPTS) {
            throw new BizException(ErrorCode.LOGIN_LOCKED);
        }

        SysUser user = sysUserMapper.selectByUsername(name);
        boolean passwordMatches = user == null
                ? compareAgainstDummy(rawPassword)
                : PasswordHasher.matches(rawPassword, user.getPasswordHash());

        if (user == null || !passwordMatches) {
            if (loginAttemptDao.recordFailure(name) >= LoginAttemptDao.MAX_ATTEMPTS) {
                // 第 5 次失败即回 2004，与 01 §1.5「5 次 / 15 分钟锁定」一致
                throw new BizException(ErrorCode.LOGIN_LOCKED);
            }
            throw new BizException(ErrorCode.LOGIN_FAILED);
        }

        // 2) 状态判定放在密码校验之后：先判状态会让任何人用"这个账号被禁用了"
        //    反推出用户名的存在性。
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        loginAttemptDao.clear(name);
        LocalDateTime now = LocalDateTime.now();
        sysUserMapper.updateLastLoginAt(user.getId(), now);
        user.setLastLoginAt(now);
        return UserConverter.toVO(user);
    }

    @Override
    public UserVO getById(Long userId) {
        return UserConverter.toVO(requireUser(userId));
    }

    @Override
    @Transactional
    public UserVO updateNickname(Long userId, String nickname) {
        SysUser user = requireUser(userId);
        String value = nickname.trim();

        sysUserMapper.updateNickname(userId, value);
        // 回写内存对象再转换：只改一列，没必要为它多打一次 SELECT 往返。
        user.setNickname(value);
        return UserConverter.toVO(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = requireUser(userId);

        // 必须验原密码：这是"改密"与"重置密码"的分界，也是防止"离开工位没锁屏
        // 就被人改掉密码"的唯一一道门。失败回 2006，与登录失败（2001）区分开——
        // 前端要就地提示"原密码错误"，不能让它看起来像登录问题。
        if (!PasswordHasher.matches(oldPassword, user.getPasswordHash())) {
            throw new BizException(ErrorCode.OLD_PASSWORD_WRONG);
        }

        // 明文口令只在这一次调用内存在，落库的是 BCrypt 哈希；本方法不写日志、不带出参。
        sysUserMapper.updatePassword(userId, PasswordHasher.hash(newPassword));
    }

    @Override
    @Transactional
    public UserVO updateCurrentSubject(Long userId, String subjectId) {
        SysUser user = requireUser(userId);
        Long parsed = requireId(subjectId, "科目 ID 非法");

        Subject subject = subjectMapper.selectById(parsed);
        // "不存在"与"已停用"合并成 3001（05 §5.1.2 就是这么定的）：对考生而言
        // 这两件事的结论相同（这个科目不能作为备考科目），分开反而泄漏了内部状态。
        if (subject == null || !"ENABLED".equals(subject.getStatus())) {
            throw new BizException(ErrorCode.SUBJECT_UNAVAILABLE);
        }

        sysUserMapper.updateCurrentSubjectId(userId, parsed);
        user.setCurrentSubjectId(parsed);
        return UserConverter.toVO(user);
    }

    /**
     * 取用户，不存在即 2005（令牌比用户活得久：库被重置、账号被物理删除）。
     *
     * <p>语义上就是"未登录"，回 2005 让前端清 token 并跳登录，
     * 而不是 1003 让前端停在一个空页面上。
     */
    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_LOGGED_IN);
        }
        return user;
    }

    /** 解析必填 ID（字符串 → Long）。非法值报 1001，不静默当成 0（同 PracticeServiceImpl）。 */
    private static Long requireId(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            throw new BizException(ErrorCode.PARAM_INVALID, message);
        }
    }

    /**
     * 用户名不存在时的一次等代价比对。永远返回 {@code false}，只为了让两条分支
     * （存在 / 不存在）在时间上不可区分（06 §6.5）。
     */
    private static boolean compareAgainstDummy(String rawPassword) {
        return PasswordHasher.matches(rawPassword, TIMING_DUMMY_HASH);
    }

    /** 昵称为空或全空白时取用户名（05 §5.3 第 1 条）。 */
    private static String resolveNickname(String nickname, String username) {
        if (nickname == null || nickname.isBlank()) {
            return username;
        }
        return nickname.trim();
    }
}
