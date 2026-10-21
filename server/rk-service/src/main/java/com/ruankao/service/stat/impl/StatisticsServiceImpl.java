package com.ruankao.service.stat.impl;

import com.ruankao.domain.entity.StatUserKp;
import com.ruankao.domain.entity.StatUserOutline;
import com.ruankao.domain.vo.ActiveUserSubjectRow;
import com.ruankao.domain.vo.StatAnswerRow;
import com.ruankao.domain.vo.StatQuestionKpRow;
import com.ruankao.domain.vo.StatRefCountRow;
import com.ruankao.repository.dao.StatStaleDao;
import com.ruankao.repository.mapper.StatAggregationMapper;
import com.ruankao.repository.mapper.StatDailyMapper;
import com.ruankao.repository.mapper.StatUserKpMapper;
import com.ruankao.repository.mapper.StatUserOutlineMapper;
import com.ruankao.repository.mapper.StatUserSubjectMapper;
import com.ruankao.service.config.SysConfigService;
import com.ruankao.service.stat.StatAggregator;
import com.ruankao.service.stat.StatisticsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 学习统计写入实现（06 §6.4、§6.10.2、§6.13）。说明见 {@link StatisticsService}。 */
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    /**
     * 单用户单科目重算读回流水行的硬上界（AC-4）。
     * 本项目单用户在单科目的作答在数千量级（04 §4.3 的量级估算），20000 远超实际；
     * 设上限只是为了让"异常用户把整表读进内存"这件事有一个确定的失败面。
     */
    private static final int MAX_ANSWER_ROWS = 20_000;

    /** 夜间全量重算单次扫描的 (user, subject) 硬上界（AC-4）。 */
    private static final int MAX_ACTIVE_PAIRS = 20_000;

    private final StatDailyMapper statDailyMapper;
    private final StatUserSubjectMapper statUserSubjectMapper;
    private final StatUserOutlineMapper statUserOutlineMapper;
    private final StatUserKpMapper statUserKpMapper;
    private final StatAggregationMapper statAggregationMapper;
    private final StatStaleDao statStaleDao;
    private final SysConfigService sysConfigService;

    public StatisticsServiceImpl(StatDailyMapper statDailyMapper,
                                 StatUserSubjectMapper statUserSubjectMapper,
                                 StatUserOutlineMapper statUserOutlineMapper,
                                 StatUserKpMapper statUserKpMapper,
                                 StatAggregationMapper statAggregationMapper,
                                 StatStaleDao statStaleDao,
                                 SysConfigService sysConfigService) {
        this.statDailyMapper = statDailyMapper;
        this.statUserSubjectMapper = statUserSubjectMapper;
        this.statUserOutlineMapper = statUserOutlineMapper;
        this.statUserKpMapper = statUserKpMapper;
        this.statAggregationMapper = statAggregationMapper;
        this.statStaleDao = statStaleDao;
        this.sysConfigService = sysConfigService;
    }

    @Override
    @Transactional
    public void incrementCounters(Long userId,
                                  Long subjectId,
                                  int answeredDelta,
                                  int correctDelta,
                                  int durationSecDelta,
                                  LocalDateTime answeredAt) {
        if (answeredDelta <= 0) {
            return;
        }
        statDailyMapper.increment(userId, answeredAt.toLocalDate(),
                answeredDelta, correctDelta, durationSecDelta);
        statUserSubjectMapper.incrementCounters(userId, subjectId,
                answeredDelta, correctDelta,
                BigDecimal.valueOf(sysConfigService.masteryPrior()), answeredAt);
    }

    @Override
    public void markStale(Long userId, Long subjectId) {
        statStaleDao.markStale(userId, subjectId);
    }

    @Override
    @Transactional
    public void refreshIfStale(Long userId, Long subjectId) {
        if (!statStaleDao.isStale(userId, subjectId)) {
            return;
        }
        recomputeInternal(userId, subjectId);
        clearStaleAfterCommit(userId, subjectId);
    }

    @Override
    @Transactional
    public void recompute(Long userId, Long subjectId) {
        recomputeInternal(userId, subjectId);
        // 夜间全量重算结束后数据已是最新，残留的脏标记可以直接丢掉
        clearStaleAfterCommit(userId, subjectId);
    }

    @Override
    @Transactional
    public void recordExam(Long userId, Long subjectId, BigDecimal objectiveScore) {
        statUserSubjectMapper.recordExam(userId, subjectId, objectiveScore);
    }

    @Override
    public List<ActiveUserSubjectRow> activeUserSubjects(int days, int limit) {
        return statAggregationMapper.selectActiveUserSubjects(days, Math.min(limit, MAX_ACTIVE_PAIRS));
    }

    /**
     * 重算某用户某科目的章节 / 知识点掌握度，并回填科目掌握度（06 §6.10.2）。
     *
     * <p>写的是"重算结果"而不是增量：每次都由流水重新导出全部数字，
     * 所以 upsert 用整体覆盖语义。四步顺序与依赖关系：
     * 原始流水 → 去重取最近一次 → 章节+知识点两个维度 → 科目加权汇总。
     */
    private void recomputeInternal(Long userId, Long subjectId) {
        LocalDateTime now = LocalDateTime.now();
        double halfLifeDays = sysConfigService.masteryHalfLifeDays();
        double prior = sysConfigService.masteryPrior();

        List<StatAnswerRow> raw =
                statAggregationMapper.selectAnswerRows(userId, subjectId, MAX_ANSWER_ROWS);
        List<StatAggregator.Answer> latest = StatAggregator.latestPerQuestion(raw);

        Map<Long, Integer> outlineCounts =
                toCountMap(statAggregationMapper.selectOutlinePublishedCounts(subjectId));
        List<StatUserOutline> outlineRows = StatAggregator.outlineRows(
                userId, subjectId, latest, outlineCounts, now, halfLifeDays, prior);
        if (!outlineRows.isEmpty()) {
            statUserOutlineMapper.upsertBatch(outlineRows);
        }

        Map<Long, List<Long>> kpByQuestion = new LinkedHashMap<>();
        for (StatQuestionKpRow link : statAggregationMapper.selectQuestionKpLinks(
                userId, subjectId, MAX_ANSWER_ROWS)) {
            kpByQuestion.computeIfAbsent(link.getQuestionId(), k -> new ArrayList<>())
                    .add(link.getKpId());
        }
        Map<Long, Integer> kpCounts =
                toCountMap(statAggregationMapper.selectKpPublishedCounts(subjectId));
        List<StatUserKp> kpRows = StatAggregator.kpRows(
                userId, subjectId, latest, kpByQuestion, kpCounts, now, halfLifeDays, prior);
        if (!kpRows.isEmpty()) {
            statUserKpMapper.upsertBatch(kpRows);
        }

        Map<Long, BigDecimal> outlineMastery = new LinkedHashMap<>();
        for (StatUserOutline row : outlineRows) {
            outlineMastery.put(row.getOutlineId(), row.getMastery());
        }
        BigDecimal subjectMastery = StatAggregator.subjectMastery(outlineMastery, outlineCounts, prior);
        statUserSubjectMapper.updateMastery(userId, subjectId, subjectMastery);

        log.debug("统计重算完成：userId={}, subjectId={}, 章节行={}, 知识点行={}, 科目掌握度={}",
                userId, subjectId, outlineRows.size(), kpRows.size(), subjectMastery);
    }

    /**
     * 重算结果**提交后**再清标记。
     *
     * <p>顺序不能反：若先清标记、事务随后回滚，脏标记就丢了，而表还是旧值——
     * 表现是"掌握度永远停在上一版，直到夜间重算"。提交后再清，最坏情况只是多算一次。
     */
    private void clearStaleAfterCommit(Long userId, Long subjectId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    statStaleDao.clear(userId, subjectId);
                }
            });
        } else {
            statStaleDao.clear(userId, subjectId);
        }
    }

    private static Map<Long, Integer> toCountMap(List<StatRefCountRow> rows) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (StatRefCountRow row : rows) {
            counts.put(row.getRefId(), row.getQuestionCount() == null ? 0 : row.getQuestionCount());
        }
        return counts;
    }
}
