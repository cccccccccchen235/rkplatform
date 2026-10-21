package com.ruankao.service.stat;

import com.ruankao.domain.vo.ActiveUserSubjectRow;
import com.ruankao.service.config.SysConfigService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 统计夜间全量重算（06 §6.13：每日 03:00）。
 *
 * <p>存在的理由不是"多算一次更保险"，而是掌握度里的<b>时间衰减</b>：
 * 一个用户昨天没答题，他的掌握度今天依然会变。只有按天重算才能让"长期不练会退"
 * 这件事真的发生；增量更新永远只累加、不会让掌握度往下走。
 *
 * <p>循环放在调度器里、逐个调用 {@link StatisticsService#recompute}，而不是服务内部
 * 一个大方法：每个 (user, subject) 一个独立事务，单个用户的数据异常不会拖垮整晚的任务。
 */
@Component
public class StatisticsScheduler {

    private static final Logger log = LoggerFactory.getLogger(StatisticsScheduler.class);

    /** 06 §6.13：活跃用户 = 近 90 天有作答。 */
    private static final int ACTIVE_DAYS = 90;

    private static final int MAX_PAIRS = 20_000;

    private final StatisticsService statisticsService;
    private final SysConfigService sysConfigService;

    public StatisticsScheduler(StatisticsService statisticsService,
                               SysConfigService sysConfigService) {
        this.statisticsService = statisticsService;
        this.sysConfigService = sysConfigService;
    }

    /** 每日 03:00 全量重算活跃用户的章节 / 知识点 / 科目掌握度。 */
    @Scheduled(cron = "0 0 3 * * *")
    public void rebuildActiveStatistics() {
        // 06 §6.13：所有任务都有 task.enabled.* 开关，便于排障时单独关闭
        if (!sysConfigService.statRebuildEnabled()) {
            log.info("统计全量重算已通过 task.enabled.stat.rebuild 关闭，跳过本次");
            return;
        }

        List<ActiveUserSubjectRow> pairs =
                statisticsService.activeUserSubjects(ACTIVE_DAYS, MAX_PAIRS);
        int succeeded = 0;
        int failed = 0;
        for (ActiveUserSubjectRow pair : pairs) {
            try {
                statisticsService.recompute(pair.getUserId(), pair.getSubjectId());
                succeeded++;
            } catch (Exception ex) {
                failed++;
                log.warn("统计重算失败（继续处理其余待办）：userId={}, subjectId={}",
                        pair.getUserId(), pair.getSubjectId(), ex);
            }
        }
        log.info("统计全量重算完成：待办={}, 成功={}, 失败={}", pairs.size(), succeeded, failed);
    }
}
