package com.ruankao.repository.cache;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import java.time.Duration;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code rk:cache:*} 系列的读写封装（06 §6.11）。
 *
 * <p><b>可降级</b>——与 {@link com.ruankao.repository.dao.LoginAttemptDao} 相反。
 * 06 §6.11 把两类数据分得很清楚：会话与防爆破<b>不可</b>降级（宁可明确失败），
 * 而本类的读缓存<b>必须</b>降级（Redis 挂了就直接查 MySQL，记 WARN）。
 * 理由：缓存是纯粹的加速层，它的失效不该让考生看不到科目列表。
 *
 * <p>做成一个泛型 DAO 而不是每张缓存一个 {@code XxxCacheDao}：降级逻辑
 * （try-catch + WARN + 回落）是本类唯一有价值的部分，复制 N 份意味着
 * N 处可能忘记吞异常。序列化仍在这一层完成（06 §6.11 的要求），
 * Service 只提供 {@code loader} 与类型引用，不接触 {@code RedisTemplate}。
 *
 * <p>已知取舍：{@link #evict} 失败只记 WARN。若 Redis 恰好在"写库成功、
 * 清缓存失败"的窗口内不可用、之后又恢复，旧值会残留到 TTL 到期（本类默认 1 小时）。
 * 不为此引入版本号机制——那会把每条缓存读都变成两次读，为一个小概率窗口
 * 付出常态成本；WARN 日志里带着 Key，运维侧可见。
 */
@Repository
public class CacheDao {

    private static final Logger log = LoggerFactory.getLogger(CacheDao.class);

    /** 06 §6.11 表里 {@code rk:cache:*} 系列的 TTL 统一为 1 小时。 */
    public static final Duration DEFAULT_TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public CacheDao(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 读穿：先查缓存，未命中（或缓存不可用、或缓存内容已损坏）则调 {@code loader} 回填。
     *
     * @param type   元素类型；树与嵌套 Map 必须用 {@link TypeReference} 才能带住泛型，
     *               写成 {@code Map.class} 会让嵌套的 {@code List<SubjectVO>} 退化成
     *               {@code List<JSONObject>}，序列化时看得过去、反序列化时才炸
     * @param loader 未命中时的回源函数；返回 {@code null} 表示"确实没有"，
     *               此时<b>不写缓存</b>——把 null 缓存起来会需要一套空值 TTL 规则，
     *               而本项目的缓存对象都是"有就是有、没有就是空集合"
     */
    public <T> T getOrLoad(String key, TypeReference<T> type, Duration ttl, Supplier<T> loader) {
        T cached = read(key, type);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        if (value != null) {
            write(key, value, ttl);
        }
        return value;
    }

    /**
     * 删除一个键。管理端改动数据后调用（06 §6.11 的"失效时机"列）。
     *
     * <p>删除失败不抛出：Redis 不可用时读路径本来就会回落 MySQL，
     * 于是"删不掉"与"读不到"同时发生，功能上仍是一致的。
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception ex) {
            log.warn("缓存清除失败（将残留至 TTL 到期）：key={}", key, ex);
        }
    }

    /**
     * 读缓存。任何异常都返回 {@code null}（= 未命中），由调用方回源。
     *
     * <p>反序列化失败也走这条路：缓存里躺着一份读不懂的 JSON（版本变更、人工写入、
     * 半截写入）时，正确行为是当作没缓存并重新回填，而不是把异常抛给用户。
     */
    private <T> T read(String key, TypeReference<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return JSONUtil.toBean(json, type, false);
        } catch (Exception ex) {
            log.warn("缓存读取失败（回落查库）：key={}", key, ex);
            return null;
        }
    }

    /** 写缓存。失败只记 WARN：缓存写不进去不该影响这次请求的结果。 */
    private void write(String key, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), ttl);
        } catch (Exception ex) {
            log.warn("缓存写入失败（不影响本次响应）：key={}", key, ex);
        }
    }
}
