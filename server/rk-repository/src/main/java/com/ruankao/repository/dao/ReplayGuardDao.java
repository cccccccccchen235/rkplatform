package com.ruankao.repository.dao;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 答题上报的重放闸（05 §5.5 第 13 条：同一 {@code questionId} 在 5 秒内重复上报视为重放，忽略）。
 *
 * <p>为什么必须有它：答题上报被设计成**可重试**的（验收项「剪断网络后继续答题，恢复网络后
 * 记录补传成功」）。客户端在超时未收到响应时会重发同一批 items，若服务端照单全收，
 * 一次作答就会在 {@code user_answer_record} 里落下两行——而那张表是<b>唯一的事实流水</b>，
 * 所有统计口径都从它派生。所以幂等不是优化，是正确性要求。
 *
 * <p>用 {@code SET key value NX EX 5} 一条命令完成"判重 + 占位"：返回 {@code true} 表示
 * 这是 5 秒内的第一次上报（放行），{@code false} 表示重放（忽略）。不用
 * "先 GET 再 SET"——那之间有竞态窗口，两次并发重放可能同时通过。
 *
 * <p><b>不可降级</b>：Redis 不可用时<b>让上报失败</b>（异常向上抛，前端拿到 1000）。
 * 与 {@link LoginAttemptDao} 同一取舍：这道闸门守的是"流水不重复"，闸门坏了还放行，
 * 等于把幂等悄悄关掉，而日志里只看得到一次 Redis 抖动。Redis 本来就是本项目的
 * 登录/会话依赖（Sa-Token），这里不额外引入新的可用性约束。
 */
@Repository
public class ReplayGuardDao {

    /** 05 §5.5 第 13 条的重放窗口：5 秒。 */
    private static final Duration WINDOW = Duration.ofSeconds(5);

    private static final String KEY_PREFIX = "rk:replay:answer:";

    private final StringRedisTemplate redisTemplate;

    public ReplayGuardDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试为「某用户答某题」占一个 5 秒窗口。
     *
     * @return {@code true} = 首次上报，应当落库；{@code false} = 5 秒内的重放，应当忽略
     */
    public boolean tryAcquire(Long userId, Long questionId) {
        String key = KEY_PREFIX + userId + ":" + questionId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", WINDOW);
        // setIfAbsent 返回 null 只可能出现在管道/事务场景；这里没有用管道，
        // 但按"拿不准时按重放处理"更安全：宁可少记一条，不可多记一条。
        return Boolean.TRUE.equals(first);
    }
}