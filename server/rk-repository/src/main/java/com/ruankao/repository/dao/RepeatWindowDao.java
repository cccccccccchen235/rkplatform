package com.ruankao.repository.dao;

import com.ruankao.repository.cache.CacheKeys;
import com.ruankao.repository.mapper.UserAnswerRecordMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 组卷防重复窗口（06 §6.6.4，{@code rk:repeat:{userId}:{subjectId}}）。
 *
 * <p>成员是该用户在该科目下最近答过的题 ID。用途是让<b>模拟考组卷</b>避开刚做过的题。
 *
 * <p><b>练习组卷不读这个集合</b>：练习要的恰恰是"再练一遍"，若也按窗口排除，
 * 错题重做与收藏练习会立刻取不到题。所以本类在 P4 只有写入方（答题上报成功后 SADD），
 * 读取与回填留给模拟考（P5）——这也解释了为什么它现在只是一个 Set，没有查询接口。
 *
 * <p><b>可降级</b>：06 §6.6.4 明确它是"纯粹的性能优化，丢失只会让防重复短暂失效，
 * 不影响数据正确性"。因此写入失败只记 WARN，不让一次答题上报因为 Redis 抖动而失败。
 * 这与 {@link LoginAttemptDao}（不可降级：计数失败却放行等于关掉爆破防护）是刻意的对照。
 */
@Repository
public class RepeatWindowDao {

    private static final Logger log = LoggerFactory.getLogger(RepeatWindowDao.class);

    /** 06 §6.6.4：{@code sys_config} 的 {@code question.repeat.window} 默认 30 天。 */
    public static final int DEFAULT_WINDOW_DAYS = 30;

    private final StringRedisTemplate redisTemplate;
    private final UserAnswerRecordMapper userAnswerRecordMapper;

    public RepeatWindowDao(StringRedisTemplate redisTemplate,
                           UserAnswerRecordMapper userAnswerRecordMapper) {
        this.redisTemplate = redisTemplate;
        this.userAnswerRecordMapper = userAnswerRecordMapper;
    }

    /**
     * 记下"这些题刚答过"。
     *
     * <p>每次都续期 TTL，等价于一个**滑动**窗口：没答新题才逐渐过期。
     * 这里与 {@link LoginAttemptDao} 的固定窗口（只在首次失败时设 TTL）不同——
     * 那里的固定窗口是为了给出一个可预期的解锁时间（防爆破），
     * 而这里只是"最近答过的一批题"，滑动窗口更贴合它的语义。
     */
    public void add(Long userId, Long subjectId, Collection<Long> questionIds, Duration ttl) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }
        String key = CacheKeys.repeatWindow(userId, subjectId);
        try {
            List<String> members = questionIds.stream().map(String::valueOf).toList();
            redisTemplate.opsForSet().add(key, members.toArray(new String[0]));
            redisTemplate.expire(key, ttl);
        } catch (Exception ex) {
            // 见类注释：这是可丢失数据，写不进去不该影响这次上报的结果。
            log.warn("防重复窗口写入失败（仅影响模拟考去重，不影响本次上报）：key={}", key, ex);
        }
    }

    /**
     * 读窗口内的题 ID（模拟考组卷的读取方，06 §6.6.4）。
     *
     * <p>缓存未命中就从流水**重建**再返回：Set 丢失只是让防重复短暂失效，所以这里的取向是
     * "宁可重建成空集也不报错"。Redis 不可用时返回空集（等于本卷不做防重复），
     * 与类注释的"可降级"一致。
     */
    public Set<Long> members(Long userId, Long subjectId, int windowDays) {
        String key = CacheKeys.repeatWindow(userId, subjectId);
        try {
            Set<String> raw = redisTemplate.opsForSet().members(key);
            if (raw != null && !raw.isEmpty()) {
                Set<Long> ids = new HashSet<>(raw.size() * 2);
                for (String value : raw) {
                    ids.add(Long.valueOf(value));
                }
                return ids;
            }
            Set<Long> rebuilt = new HashSet<>(userAnswerRecordMapper.selectRecentQuestionIds(
                    userId, subjectId, LocalDateTime.now().minusDays(windowDays), REBUILD_LIMIT));
            if (!rebuilt.isEmpty()) {
                add(userId, subjectId, rebuilt, Duration.ofDays(windowDays));
            }
            return rebuilt;
        } catch (Exception ex) {
            log.warn("防重复窗口读取失败（本卷不做去重）：key={}", key, ex);
            return Set.of();
        }
    }

    /** 回填上界：单用户单科目窗口内的去重题数远小于此。 */
    private static final int REBUILD_LIMIT = 2000;
}
