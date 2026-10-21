package com.ruankao.repository.dao;

import com.ruankao.repository.cache.CacheKeys;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 统计待重算标记（{@code rk:stat:stale:{userId}:{subjectId}}，06 §6.10.2、§6.11）。
 *
 * <p>它把"答题上报"与"掌握度重算"解耦：上报只负责把标记点亮（O(1)），
 * 真正的重算发生在<b>有人需要这份数据时</b>（练习组卷的 {@code onlyUnmastered}；
 * P6 的统计读取接口落地后也走同一入口）。这样一次上报不必为一个可能没人看的
 * 统计页付出重算代价，而重算的时机又足够早——在它被读到之前。
 *
 * <p>TTL 7 天（06 §6.11）：一个用户一周没回来，标记过期即可；
 * 若他回来了，夜间全量重算或下一次读取会补齐，标记丢不丢都不影响正确性。
 *
 * <p><b>降级方向是"宁可多算"</b>：{@link #isStale} 在 Redis 不可用时返回 {@code true}
 * 而不是 {@code false}——重算是幂等的，多算一次只是浪费；而当作"不脏"会一直读到旧掌握度。
 * 这与 {@code CacheDao}（读不到就回落查库）同属"降级不能改变结果"的原则，
 * 只是这里的"回落"就是重算。
 */
@Repository
public class StatStaleDao {

    private static final Logger log = LoggerFactory.getLogger(StatStaleDao.class);

    /** 06 §6.11 表：{@code rk:stat:stale:*} 的 TTL。 */
    public static final Duration STALE_TTL = Duration.ofDays(7);

    private static final String MARK = "1";

    private final StringRedisTemplate redisTemplate;

    public StatStaleDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 点亮待重算标记。
     *
     * <p>失败只记 WARN：答题上报已经成功落库，一个统计优化标记写不进去
     * 不该让用户的上报失败。兜底是夜间全量重算（06 §6.13）。
     */
    public void markStale(Long userId, Long subjectId) {
        String key = CacheKeys.statStale(userId, subjectId);
        try {
            redisTemplate.opsForValue().set(key, MARK, STALE_TTL);
        } catch (Exception ex) {
            log.warn("统计待重算标记写入失败（将由夜间全量重算兜底）：key={}", key, ex);
        }
    }

    /**
     * 标记是否存在。Redis 不可用时返回 {@code true}（见类注释：宁可多算）。
     */
    public boolean isStale(Long userId, Long subjectId) {
        String key = CacheKeys.statStale(userId, subjectId);
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception ex) {
            log.warn("统计待重算标记读取失败，按已脏处理（本次将重算）：key={}", key, ex);
            return true;
        }
    }

    /** 重算成功后清除标记。失败只记 WARN：残留的标记只会导致下一次多算一次。 */
    public void clear(Long userId, Long subjectId) {
        String key = CacheKeys.statStale(userId, subjectId);
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("统计待重算标记清除失败（下次将多算一次）：key={}", key, ex);
        }
    }
}
