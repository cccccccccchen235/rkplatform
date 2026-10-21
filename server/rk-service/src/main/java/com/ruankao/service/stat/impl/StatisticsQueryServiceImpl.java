package com.ruankao.service.stat.impl;

import com.ruankao.common.api.ErrorCode;
import com.ruankao.common.exception.BizException;
import com.ruankao.domain.entity.OutlineNode;
import com.ruankao.domain.entity.StatUserOutline;
import com.ruankao.domain.vo.StatDailyRow;
import com.ruankao.domain.vo.StatOutlineNodeVO;
import com.ruankao.domain.vo.StatOverviewVO;
import com.ruankao.domain.vo.StatRefCountRow;
import com.ruankao.domain.vo.StatTrendPointVO;
import com.ruankao.domain.vo.StatUserSubjectRow;
import com.ruankao.domain.vo.UserVO;
import com.ruankao.domain.vo.WeakKpRow;
import com.ruankao.domain.vo.WeakKpVO;
import com.ruankao.repository.mapper.OutlineNodeMapper;
import com.ruankao.repository.mapper.StatAggregationMapper;
import com.ruankao.repository.mapper.StatReadMapper;
import com.ruankao.service.config.SysConfigService;
import com.ruankao.service.question.SubjectService;
import com.ruankao.service.stat.StatOutlineTreeBuilder;
import com.ruankao.service.stat.StatisticsQueryService;
import com.ruankao.service.stat.StatisticsService;
import com.ruankao.service.user.UserService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** 学习统计读取实现（05 §5.8）。口径说明见 {@link StatisticsQueryService}。 */
@Service
public class StatisticsQueryServiceImpl implements StatisticsQueryService {

    private static final int SCALE = 4;
    private static final int DEFAULT_TREND_DAYS = 30;
    private static final int MAX_TREND_DAYS = 90;
    private static final int DEFAULT_WEAK_LIMIT = 10;
    private static final int MAX_WEAK_LIMIT = 30;
    private static final double DEFAULT_MAX_MASTERY = 0.6;
    /** 列表查询硬上界（AC-4）：单用户单科目的统计行远小于此。 */
    private static final int MAX_ROWS = 20_000;
    /** 06 §6.10.3 的连续天数遍历上界。 */
    private static final int ACTIVE_DATE_LIMIT = 400;

    private final StatReadMapper statReadMapper;
    private final StatAggregationMapper statAggregationMapper;
    private final OutlineNodeMapper outlineNodeMapper;
    private final StatisticsService statisticsService;
    private final SubjectService subjectService;
    private final UserService userService;
    private final SysConfigService sysConfigService;

    public StatisticsQueryServiceImpl(StatReadMapper statReadMapper,
                                      StatAggregationMapper statAggregationMapper,
                                      OutlineNodeMapper outlineNodeMapper,
                                      StatisticsService statisticsService,
                                      SubjectService subjectService,
                                      UserService userService,
                                      SysConfigService sysConfigService) {
        this.statReadMapper = statReadMapper;
        this.statAggregationMapper = statAggregationMapper;
        this.outlineNodeMapper = outlineNodeMapper;
        this.statisticsService = statisticsService;
        this.subjectService = subjectService;
        this.userService = userService;
        this.sysConfigService = sysConfigService;
    }

    @Override
    public StatOverviewVO overview(Long userId, Long subjectId) {
        Long resolved = resolveSubjectId(userId, subjectId);
        statisticsService.refreshIfStale(userId, resolved);

        StatUserSubjectRow row = statReadMapper.selectUserSubject(userId, resolved);
        int answered = row == null || row.getAnsweredCount() == null ? 0 : row.getAnsweredCount();
        int correct = row == null || row.getCorrectCount() == null ? 0 : row.getCorrectCount();
        BigDecimal accuracy = row == null || row.getAccuracy() == null ? zero() : row.getAccuracy();
        BigDecimal mastery = row == null || row.getMastery() == null
                ? prior() : row.getMastery();
        int examCount = row == null || row.getExamCount() == null ? 0 : row.getExamCount();
        BigDecimal bestScore = row == null ? null : row.getBestScore();

        StatDailyRow today = statReadMapper.selectDailyByDate(userId, LocalDate.now());
        int todayAnswered = today == null || today.getAnsweredCount() == null ? 0 : today.getAnsweredCount();
        int todayCorrect = today == null || today.getCorrectCount() == null ? 0 : today.getCorrectCount();

        return new StatOverviewVO(
                answered,
                correct,
                accuracy,
                statReadMapper.countStudyDays(userId),
                continuousDays(userId),
                mastery,
                examCount,
                bestScore,
                outlineCoverage(userId, resolved),
                todayAnswered,
                ratio(todayCorrect, todayAnswered));
    }

    @Override
    public List<StatTrendPointVO> trend(Long userId, Long subjectId, Integer days) {
        Long resolved = resolveSubjectId(userId, subjectId);
        statisticsService.refreshIfStale(userId, resolved);

        int span = clamp(days, DEFAULT_TREND_DAYS, 1, MAX_TREND_DAYS);
        LocalDate today = LocalDate.now();
        LocalDate since = today.minusDays(span - 1L);

        Map<LocalDate, StatDailyRow> byDate = new LinkedHashMap<>();
        for (StatDailyRow row : statReadMapper.selectDailyTrend(userId, resolved, since, span + 1)) {
            byDate.put(row.getStatDate(), row);
        }

        List<StatTrendPointVO> points = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            LocalDate date = since.plusDays(i);
            StatDailyRow row = byDate.get(date);
            int answered = row == null || row.getAnsweredCount() == null ? 0 : row.getAnsweredCount();
            int correct = row == null || row.getCorrectCount() == null ? 0 : row.getCorrectCount();
            int duration = row == null || row.getDurationSec() == null ? 0 : row.getDurationSec();
            points.add(new StatTrendPointVO(date.toString(), answered, correct,
                    ratio(correct, answered), duration));
        }
        return points;
    }

    @Override
    public List<StatOutlineNodeVO> outline(Long userId, Long subjectId) {
        Long resolved = resolveSubjectId(userId, subjectId);
        statisticsService.refreshIfStale(userId, resolved);

        List<OutlineNode> nodes = outlineNodeMapper.selectBySubject(resolved, false);
        if (nodes.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> publishedCounts =
                toCountMap(statAggregationMapper.selectOutlinePublishedCounts(resolved));
        Map<Long, StatUserOutline> statRows = new LinkedHashMap<>();
        for (StatUserOutline row : statReadMapper.selectOutlineStatRows(userId, resolved, MAX_ROWS)) {
            statRows.put(row.getOutlineId(), row);
        }
        return StatOutlineTreeBuilder.build(nodes, publishedCounts, statRows, sysConfigService.masteryPrior());
    }

    @Override
    public List<WeakKpVO> weakKp(Long userId, Long subjectId, Integer limit, Double maxMastery) {
        Long resolved = resolveSubjectId(userId, subjectId);
        statisticsService.refreshIfStale(userId, resolved);

        int size = clamp(limit, DEFAULT_WEAK_LIMIT, 1, MAX_WEAK_LIMIT);
        double boundary = clampRatio(maxMastery, DEFAULT_MAX_MASTERY);
        List<WeakKpRow> rows = statReadMapper.selectWeakKp(userId, resolved,
                BigDecimal.valueOf(boundary), size);

        List<WeakKpVO> result = new ArrayList<>(rows.size());
        for (WeakKpRow row : rows) {
            result.add(new WeakKpVO(
                    String.valueOf(row.getKpId()),
                    row.getKpName(),
                    row.getAnsweredCount(),
                    row.getAccuracy(),
                    row.getMastery()));
        }
        return result;
    }

    private Long resolveSubjectId(Long userId, Long subjectId) {
        Long resolved = subjectId;
        if (resolved == null) {
            UserVO user = userService.getById(userId);
            resolved = parseId(user.currentSubjectId());
        }
        if (resolved == null) {
            throw new BizException(ErrorCode.NO_STATISTICS, "尚未选择备考科目，暂无统计数据");
        }
        // 不存在/已停用统一 3001（05 §5.1.2）；与练习、错题本同一口径
        subjectService.getDetail(resolved);
        return resolved;
    }

    /** 06 §6.10.3：允许"今天还没学"（从昨天起算），中断即停止。 */
    private int continuousDays(Long userId) {
        Set<LocalDate> active = new HashSet<>(statReadMapper.selectActiveDates(userId, ACTIVE_DATE_LIMIT));
        LocalDate cursor = LocalDate.now();
        if (!active.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int days = 0;
        while (active.contains(cursor)) {
            days++;
            cursor = cursor.minusDays(1);
        }
        return days;
    }

    private BigDecimal outlineCoverage(Long userId, Long subjectId) {
        List<OutlineNode> nodes = outlineNodeMapper.selectBySubject(subjectId, false);
        if (nodes.isEmpty()) {
            return zero();
        }
        Set<Long> parentIds = new HashSet<>();
        for (OutlineNode node : nodes) {
            if (node.getParentId() != null && node.getParentId() != 0L) {
                parentIds.add(node.getParentId());
            }
        }
        List<Long> leaves = new ArrayList<>();
        for (OutlineNode node : nodes) {
            if (!parentIds.contains(node.getId())) {
                leaves.add(node.getId());
            }
        }
        if (leaves.isEmpty()) {
            return zero();
        }
        Set<Long> practiced = new HashSet<>();
        for (StatUserOutline row : statReadMapper.selectOutlineStatRows(userId, subjectId, MAX_ROWS)) {
            if (row.getAnsweredCount() != null && row.getAnsweredCount() > 0) {
                practiced.add(row.getOutlineId());
            }
        }
        long hit = leaves.stream().filter(practiced::contains).count();
        return round4(hit / (double) leaves.size());
    }

    private BigDecimal prior() {
        return BigDecimal.valueOf(sysConfigService.masteryPrior()).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static Map<Long, Integer> toCountMap(List<StatRefCountRow> rows) {
        Map<Long, Integer> counts = new LinkedHashMap<>();
        for (StatRefCountRow row : rows) {
            counts.put(row.getRefId(), row.getQuestionCount() == null ? 0 : row.getQuestionCount());
        }
        return counts;
    }

    private static BigDecimal ratio(int correct, int answered) {
        if (answered <= 0) {
            return zero();
        }
        return BigDecimal.valueOf(correct)
                .divide(BigDecimal.valueOf(answered), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal round4(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static int clamp(Integer raw, int defaultValue, int min, int max) {
        int value = raw == null ? defaultValue : raw;
        return Math.max(min, Math.min(max, value));
    }

    private static double clampRatio(Double raw, double defaultValue) {
        double value = raw == null ? defaultValue : raw;
        if (Double.isNaN(value)) {
            return defaultValue;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static Long parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
