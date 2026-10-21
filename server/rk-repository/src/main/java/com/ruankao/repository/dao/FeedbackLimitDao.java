package com.ruankao.repository.dao;

import com.ruankao.repository.cache.CacheKeys;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 题目反馈限流计数器（05 §5.9：同题同用户 24 小时内最多 3 次，超出返回 {@code 1002}）。
 *
 * <p>与 {@link LoginAttemptDao} 是同一个"固定窗口计数器"的形态：只在第一次自增时设 TTL，
 * 而不是每次续期。理由相同——固定窗口给出的是一个**可预期**的重置时刻
 * （"24 小时后可以再提一次"），而滑动窗口会让一个不断提交的用户永远打不开。
 *
 * <p><b>不可降级</b>：本类不吞异常、不回落。限流器不可用时放行，等于把这条规则
 * 悄悄关掉，而日志里只看得到一次 Redis 抖动；与其如此，不如让这次反馈以
 * {@code 1000} 明确失败——反馈是低频动作，明确失败对用户是可接受的，
 * 静默地不生效则会让"24 小时 3 次"变成一句谎话。
 */
@Repository
public class FeedbackLimitDao {

    /** 05 §5.9：同一用户对同一题 24 小时内最多提交 3 次。 */
    public static final int MAX_PER_WINDOW = 3;

    private static final Duration WINDOW = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    public FeedbackLimitDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记一次提交，返回窗口内的累计次数（含本次）。
     *
     * <p>调用方在写入反馈<b>之前</b>调用它，并据此判断是否超过 {@link #MAX_PER_WINDOW}——
     * 先计数再写库，是为了让超限的那次提交根本不产生一行反馈记录。
     */
    public long recordSubmit(Long userId, Long questionId) {
        String key = CacheKeys.feedbackLimit(userId, questionId);
        Long count = redisTemplate.opsForValue().increment(key);
        long attempts = count == null ? 1L : count;
        if (attempts == 1L) {
            redisTemplate.expire(key, WINDOW);
        }
        return attempts;
    }
}