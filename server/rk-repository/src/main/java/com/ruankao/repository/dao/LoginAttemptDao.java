package com.ruankao.repository.dao;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 登录失败计数器（06 §6.5：{@code rk:login:fail:{username}}）。
 *
 * <p>放在 rk-repository 的 {@code dao} 包：这是"存取一份易失状态"，
 * 与 Mapper 一样属于持久层职责，不是业务规则（规则——几次算多、锁多久——在
 * rk-service 的判断里，这里只负责读写）。
 *
 * <p><b>不可降级</b>：06 §6.5 明确"会话类数据在 Redis 不可用时登录直接失败"。
 * 本类不吞异常也不回落——计数失败却放行，等于把爆破防护关掉而日志里看不出来。
 */
@Repository
public class LoginAttemptDao {

    /** 达到该次数即锁定（06 §6.5、01 §1.5：5 次 / 15 分钟）。 */
    public static final int MAX_ATTEMPTS = 5;

    private static final String KEY_PREFIX = "rk:login:fail:";

    private static final Duration LOCK_TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 取当前失败次数。不存在或值不可解析时返回 0。
     *
     * <p>解析失败当作 0 而不是抛异常：计数键是可易失数据，被人手工写成
     * {@code rk:login:fail:admin=abc} 时，正确行为是"当作没有失败记录"，
     * 而不是让全站登录挂掉。
     */
    public int failureCount(String username) {
        String raw = redisTemplate.opsForValue().get(key(username));
        if (raw == null) {
            return 0;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    /**
     * 记一次失败，返回累计次数。
     *
     * <p>TTL 只在第一次失败（计数为 1）时设置，而不是每次失败都续期：
     * 后者会让窗口变成"滑动"的——一个每 14 分钟错一次的脚本能让账号永久锁定，
     * 而 01 §1.5 的口径是固定的"5 次 / 15 分钟"。代价是攻击者可以每 15 分钟重置一次预算，
     * 这是固定窗口方案的固有代价，换来的是一条可预期的解锁时间。
     */
    public long recordFailure(String username) {
        String key = key(username);
        Long count = redisTemplate.opsForValue().increment(key);
        long attempts = count == null ? 1L : count;
        if (attempts == 1L) {
            redisTemplate.expire(key, LOCK_TTL);
        }
        return attempts;
    }

    /** 登录成功后清零（06 §6.5）。 */
    public void clear(String username) {
        redisTemplate.delete(key(username));
    }

    private static String key(String username) {
        return KEY_PREFIX + username;
    }
}
