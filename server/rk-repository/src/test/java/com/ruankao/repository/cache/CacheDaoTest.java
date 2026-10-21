package com.ruankao.repository.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import cn.hutool.core.lang.TypeReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * {@link CacheDao} 的<b>降级分支</b>（06 §6.11：{@code rk:cache:*} 必须在 Redis 不可用时回落 MySQL）。
 *
 * <p>为什么用"操作时抛异常的模板"而不是真连 Redis：这条断言的内容就是"Redis 挂了会怎样"，
 * 连上真 Redis 恰好让这个分支永不执行。本机 Redis 是活的（用 redis-server 手工起的），
 * 停掉它既影响 Sa-Token 会话、又会被下一个把 Redis 起回来的人悄悄"修好"这条测试
 * ——一个依赖外部进程状态的测试迟早会变成一条永远为真的断言。
 *
 * <p>不用 Mockito：{@link StringRedisTemplate#opsForValue()} 是普通方法，
 * 匿名子类覆写即可，为此多引一个 mock 框架不值得。
 */
class CacheDaoTest {

    /** 任何 Redis 操作都失败——模拟"Redis 没起"或"网络不通"。 */
    private static StringRedisTemplate unreachableRedis() {
        return new StringRedisTemplate() {
            @Override
            public ValueOperations<String, String> opsForValue() {
                throw new RuntimeException("模拟 Redis 连接失败（Connection refused）");
            }
        };
    }

    @Test
    @DisplayName("Redis 读失败时不抛异常，直接回源，并把 loader 的结果原样返回")
    void readFailureFallsBackToLoader() {
        CacheDao dao = new CacheDao(unreachableRedis());
        AtomicInteger loadCount = new AtomicInteger();
        String loaded = "来自 MySQL 的科目列表";

        String value = dao.getOrLoad(
                CacheKeys.SUBJECT_LIST,
                new TypeReference<String>() {
                },
                CacheDao.DEFAULT_TTL,
                () -> {
                    loadCount.incrementAndGet();
                    return loaded;
                });

        assertSame(loaded, value, "Redis 挂了必须回落 loader，而不是把异常抛给考生");
        assertEquals(1, loadCount.get(), "回源应当且只应当发生一次");
    }

    @Test
    @DisplayName("Redis 写失败只记 WARN，不影响本次返回值")
    void writeFailureDoesNotAffectResult() {
        CacheDao dao = new CacheDao(unreachableRedis());

        // 读失败 → 回源 → 回填时写又失败。两次异常都被吞掉，调用方无感。
        Map<String, List<String>> loaded = Map.of("SENIOR", List.of("系统架构设计师"));
        Map<String, List<String>> value = dao.getOrLoad(
                CacheKeys.SUBJECT_LIST,
                new TypeReference<Map<String, List<String>>>() {
                },
                CacheDao.DEFAULT_TTL,
                () -> loaded);

        assertEquals(loaded, value);
    }

    @Test
    @DisplayName("Redis 不可用时 evict 不抛异常（读路径本来就会回落，删不掉与读不到是一致的）")
    void evictSwallowsFailure() {
        CacheDao dao = new CacheDao(unreachableRedis());

        // 管理端改完数据就会调它；若这里把异常抛出去，
        // 表现是"改科目成功但接口报 500"，而实际数据已经改好了。
        dao.evict(CacheKeys.SUBJECT_LIST);
        dao.evict(CacheKeys.outline(2L));
    }

    @Test
    @DisplayName("缓存不可用时 loader 返回 null 也不抛（null 表示确实没有，不写缓存）")
    void nullLoaderResultIsNotWritten() {
        CacheDao dao = new CacheDao(unreachableRedis());

        String value = dao.getOrLoad(
                CacheKeys.outline(99L),
                new TypeReference<String>() {
                },
                CacheDao.DEFAULT_TTL,
                () -> null);

        assertNull(value);
    }

    @Test
    @DisplayName("Key 前缀与 06 §6.11 的表一致；章节 Key 按 subjectId 分桶")
    void cacheKeysMatchSpec() {
        assertEquals("rk:cache:subject:list", CacheKeys.SUBJECT_LIST);
        assertEquals("rk:cache:outline:2", CacheKeys.outline(2L));
        assertEquals("rk:cache:outline:6", CacheKeys.outline(6L));
        assertEquals(1L, CacheDao.DEFAULT_TTL.toHours(), "06 §6.11：rk:cache:* 统一 1 小时");
    }
}
