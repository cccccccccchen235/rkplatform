package com.ruankao.service.config;

import com.ruankao.repository.dao.RepeatWindowDao;
import com.ruankao.repository.mapper.SysConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 运行期可调参数（{@code sys_config}，06 §6.11 / §6.13）。
 *
 * <p>做成"具名访问器"而不是一个泛型 {@code getInt(key, default)}：调用方写不出
 * {@code "wrongbook.master.threshold"} 这种字符串，键名拼错这件事在编译期就不成立。
 * 每个有读取方的键只开一个具名方法；06 §6.11 的其余键在各自功能落地时再补。
 *
 * <p><b>读取失败一律回落默认值并记 WARN</b>，不抛异常：这些是"体验参数"，
 * 不是安全边界。一个读不到配置的请求不该直接失败——尤其是
 * {@code wrongbook.master.threshold}：它决定"连对几次算掌握"，
 * 回落 2 与文档默认一致，行为可预期。
 *
 * <p><b>不加缓存</b>：{@code sys_config} 是主键/唯一键上的一次点查，且调用点是
 * "每次答题上报一次"（不是每道题一次），引入一层缓存的收益抵不上它带来的失效语义。
 */
@Service
public class SysConfigService {

    private static final Logger log = LoggerFactory.getLogger(SysConfigService.class);

    /** {@code sys_config} 键名，与 06 文档一致。 */
    private static final String KEY_WRONGBOOK_MASTER_THRESHOLD = "wrongbook.master.threshold";

    private static final String KEY_QUESTION_REPEAT_WINDOW = "question.repeat.window";

    /** 06 §6.8：连续答对几次算掌握，默认 2。 */
    public static final int DEFAULT_WRONGBOOK_MASTER_THRESHOLD = 2;

    private static final String KEY_MASTERY_HALF_LIFE = "mastery.half.life";

    private static final String KEY_MASTERY_PRIOR = "mastery.prior";

    private static final String KEY_TASK_STAT_REBUILD = "task.enabled.stat.rebuild";

    /** 06 §6.10.1：掌握度时间衰减半衰期（天），默认 30。 */
    public static final int DEFAULT_MASTERY_HALF_LIFE_DAYS = 30;

    /** 06 §6.10.1：无作答历史的先验掌握度，默认 0.5。 */
    public static final double DEFAULT_MASTERY_PRIOR = 0.5;

    private final SysConfigMapper sysConfigMapper;

    public SysConfigService(SysConfigMapper sysConfigMapper) {
        this.sysConfigMapper = sysConfigMapper;
    }

    /** 错题本"连续答对 N 次转 MASTERED"的 N（06 §6.8，默认 2）。 */
    public int wrongbookMasterThreshold() {
        return readPositiveInt(KEY_WRONGBOOK_MASTER_THRESHOLD, DEFAULT_WRONGBOOK_MASTER_THRESHOLD);
    }

    /**
     * 组卷防重复窗口的天数（06 §6.6.4，默认 30）。
     *
     * <p>默认值取自 {@link RepeatWindowDao#DEFAULT_WINDOW_DAYS}，不在这里再写一个字面量 30——
     * 同一个数字有两处定义就会有一处过期。
     */
    public int repeatWindowDays() {
        return readPositiveInt(KEY_QUESTION_REPEAT_WINDOW, RepeatWindowDao.DEFAULT_WINDOW_DAYS);
    }

    /**
     * 掌握度的时间衰减半衰期天数 {@code H}（06 §6.10.1，默认 30）。
     *
     * <p>落到 {@code 0}/{@code 负数} 会让权重公式除零，因此与其它正整数配置一样
     * 由 {@link #readPositiveInt} 拦住并回落默认。
     */
    public int masteryHalfLifeDays() {
        return readPositiveInt(KEY_MASTERY_HALF_LIFE, DEFAULT_MASTERY_HALF_LIFE_DAYS);
    }

    /** 先验掌握度 {@code prior}（06 §6.10.1，默认 0.5）；合法范围 [0,1]。 */
    public double masteryPrior() {
        return readRatio(KEY_MASTERY_PRIOR, DEFAULT_MASTERY_PRIOR);
    }

    /** 统计全量重算定时任务开关（06 §6.13，默认开启）。 */
    public boolean statRebuildEnabled() {
        return readBoolean(KEY_TASK_STAT_REBUILD, true);
    }

    /** 模拟考超时结算开关（06 §6.13 的 {@code task.enabled.exam.timeout}）。 */
    public boolean examTimeoutEnabled() {
        return readBoolean("task.enabled.exam.timeout", true);
    }

    /** 会话级滞留结算开关（06 §6.13 的 {@code task.enabled.session.stale}，对应评审 G1）。 */
    public boolean sessionStaleEnabled() {
        return readBoolean("task.enabled.session.stale", true);
    }

    /**
     * 读一个正整数配置。缺失、非数字、&le;0 一律回落默认值并记 WARN——
     * {@code threshold = 0} 会让每道答对的题都立刻变 MASTERED，属于必须被拦住的脏配置。
     */
    private int readPositiveInt(String key, int defaultValue) {
        String raw;
        try {
            raw = sysConfigMapper.selectValueByKey(key);
        } catch (Exception ex) {
            log.warn("读取 sys_config 失败，回落默认值：key={}, default={}", key, defaultValue, ex);
            return defaultValue;
        }
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value <= 0) {
                log.warn("sys_config 取值非法（须为正整数），回落默认值：key={}, value={}, default={}",
                        key, raw, defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException ex) {
            log.warn("sys_config 取值不是整数，回落默认值：key={}, value={}, default={}",
                    key, raw, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 读一个 [0,1] 的比例配置。缺失、非数字、越界一律回落默认值并记 WARN——
     * {@code prior = 2} 会让掌握度超过 1，属于必须拦住的脏配置。
     */
    private double readRatio(String key, double defaultValue) {
        String raw;
        try {
            raw = sysConfigMapper.selectValueByKey(key);
        } catch (Exception ex) {
            log.warn("读取 sys_config 失败，回落默认值：key={}, default={}", key, defaultValue, ex);
            return defaultValue;
        }
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            if (value < 0 || value > 1) {
                log.warn("sys_config 取值越界（须在 [0,1]），回落默认值：key={}, value={}, default={}",
                        key, raw, defaultValue);
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException ex) {
            log.warn("sys_config 取值不是数字，回落默认值：key={}, value={}, default={}",
                    key, raw, defaultValue);
            return defaultValue;
        }
    }

    /** 读一个布尔开关；只认 {@code true}/{@code false}（忽略大小写），其余回落默认值并记 WARN。 */
    private boolean readBoolean(String key, boolean defaultValue) {
        String raw;
        try {
            raw = sysConfigMapper.selectValueByKey(key);
        } catch (Exception ex) {
            log.warn("读取 sys_config 失败，回落默认值：key={}, default={}", key, defaultValue, ex);
            return defaultValue;
        }
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim();
        if ("true".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized)) {
            return false;
        }
        log.warn("sys_config 取值不是布尔，回落默认值：key={}, value={}, default={}",
                key, raw, defaultValue);
        return defaultValue;
    }
}
