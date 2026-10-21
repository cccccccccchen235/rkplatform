package com.ruankao.service.stat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ruankao.domain.entity.StatUserKp;
import com.ruankao.domain.entity.StatUserOutline;
import com.ruankao.domain.vo.StatAnswerRow;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 掌握度口径的单测（06 §6.10.1、§6.10.2）。
 *
 * <p>06 §6.10.2 要求"一题重复作答只算最近一次"这条口径必须有单测覆盖
 * （原文写的场景是"一题先错后对，accuracy 应为 0"，但按它自己定义的
 * "取最近一次"应当为 1；文档该处自相矛盾，见本轮文档的偏离登记）。
 * 这里把两个方向都钉死，无论文档笔误指向哪边，实现的口径都不会漂。
 */
class StatAggregatorTest {

    private static final double HALF_LIFE_DAYS = 30.0;
    private static final double PRIOR = 0.5;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 12, 0);

    @Test
    @DisplayName("06 §6.10.1 场景一：完全没做过 → mastery = prior = 0.5")
    void notAnsweredFallsBackToPrior() {
        StatAggregator.Metrics metrics =
                StatAggregator.metrics(List.of(), 10, NOW, HALF_LIFE_DAYS, PRIOR);

        assertEquals(new BigDecimal("0.0000"), metrics.accuracy());
        assertEquals(new BigDecimal("0.5000"), metrics.mastery());
    }

    @Test
    @DisplayName("06 §6.10.1 场景二：做过一遍全错 → mastery = 0")
    void fullyWrongIsZero() {
        List<StatAggregator.Answer> answers = List.of(
                answer(1L, false, NOW),
                answer(2L, false, NOW),
                answer(3L, false, NOW));

        StatAggregator.Metrics metrics =
                StatAggregator.metrics(answers, 3, NOW, HALF_LIFE_DAYS, PRIOR);

        assertEquals(new BigDecimal("0.0000"), metrics.accuracy());
        assertEquals(new BigDecimal("0.0000"), metrics.mastery());
    }

    @Test
    @DisplayName("06 §6.10.1 场景三：做完且全对 → mastery = 1")
    void fullyCorrectIsOne() {
        List<StatAggregator.Answer> answers = List.of(
                answer(1L, true, NOW),
                answer(2L, true, NOW),
                answer(3L, true, NOW));

        StatAggregator.Metrics metrics =
                StatAggregator.metrics(answers, 3, NOW, HALF_LIFE_DAYS, PRIOR);

        assertEquals(new BigDecimal("1.0000"), metrics.mastery());
    }

    @Test
    @DisplayName("06 §6.10.1 场景四：做了一半且全对 → mastery = 0.75")
    void halfCoverageCorrectIsZeroPointSevenFive() {
        List<StatAggregator.Answer> answers = List.of(
                answer(1L, true, NOW),
                answer(2L, true, NOW));

        StatAggregator.Metrics metrics =
                StatAggregator.metrics(answers, 4, NOW, HALF_LIFE_DAYS, PRIOR);

        assertEquals(new BigDecimal("0.7500"), metrics.mastery());
    }

    @Test
    @DisplayName("去重口径：一题先对后错，最近一次为错 → accuracy = 0")
    void laterWrongAnswerWins() {
        List<StatAnswerRow> rows = List.of(
                row(1L, 7L, 1L, true, NOW.minusMinutes(2)),
                row(1L, 7L, 2L, false, NOW.minusMinutes(1)));

        StatAggregator.Metrics metrics = StatAggregator.metrics(
                StatAggregator.latestPerQuestion(rows), 1, NOW, HALF_LIFE_DAYS, PRIOR);

        assertEquals(1, StatAggregator.latestPerQuestion(rows).size());
        assertEquals(new BigDecimal("0.0000"), metrics.accuracy());
        assertEquals(new BigDecimal("0.0000"), metrics.mastery());
    }

    @Test
    @DisplayName("去重口径：一题先错后对，最近一次为对 → accuracy = 1")
    void laterCorrectAnswerWins() {
        List<StatAnswerRow> rows = List.of(
                row(1L, 7L, 1L, false, NOW.minusMinutes(2)),
                row(1L, 7L, 2L, true, NOW.minusMinutes(1)));

        StatAggregator.Metrics metrics = StatAggregator.metrics(
                StatAggregator.latestPerQuestion(rows), 1, NOW, HALF_LIFE_DAYS, PRIOR);

        assertEquals(new BigDecimal("1.0000"), metrics.accuracy());
        assertEquals(new BigDecimal("1.0000"), metrics.mastery());
    }

    @Test
    @DisplayName("去重裁决：同一时间戳用流水主键定胜负")
    void sameTimestampIsBrokenByRecordId() {
        List<StatAnswerRow> rows = List.of(
                row(1L, 7L, 10L, true, NOW),
                row(1L, 7L, 11L, false, NOW));

        assertEquals(false, StatAggregator.latestPerQuestion(rows).get(0).correct());
    }

    @Test
    @DisplayName("时间衰减：半衰期前的对题权重减半")
    void decayHalvesWeightPerHalfLife() {
        List<StatAggregator.Answer> answers = List.of(
                answer(1L, false, NOW),
                answer(2L, true, NOW.minusDays(30)));

        StatAggregator.Metrics metrics =
                StatAggregator.metrics(answers, 2, NOW, HALF_LIFE_DAYS, PRIOR);

        // recency = (0×1 + 1×0.5) / (1 + 0.5) = 1/3；覆盖率 1 → mastery = 1/3
        assertEquals(new BigDecimal("0.3333"), metrics.mastery());
    }

    @Test
    @DisplayName("科目掌握度按章节题量加权，未练过的章节取先验")
    void subjectMasteryIsWeightedByQuestionCounts() {
        Map<Long, BigDecimal> outlineMastery = new LinkedHashMap<>();
        outlineMastery.put(1L, new BigDecimal("1.0000"));

        Map<Long, Integer> outlineCounts = new LinkedHashMap<>();
        outlineCounts.put(1L, 10);
        outlineCounts.put(2L, 90);

        // (1×10 + 0.5×90) / 100 = 0.55
        assertEquals(new BigDecimal("0.5500"),
                StatAggregator.subjectMastery(outlineMastery, outlineCounts, PRIOR));
    }

    @Test
    @DisplayName("章节行与知识点行只覆盖有作答的维度")
    void rowsOnlyCoverAnsweredDimensions() {
        List<StatAggregator.Answer> latest = List.of(
                answer(1L, true, NOW),
                answer(2L, false, NOW));

        List<StatUserOutline> outlineRows = StatAggregator.outlineRows(
                9L, 3L, latest, Map.of(7L, 2), NOW, HALF_LIFE_DAYS, PRIOR);
        assertEquals(1, outlineRows.size());
        assertEquals(2, outlineRows.get(0).getAnsweredCount());
        assertEquals(1, outlineRows.get(0).getCorrectCount());

        Map<Long, List<Long>> kpByQuestion = new LinkedHashMap<>();
        kpByQuestion.put(1L, List.of(100L, 200L));
        kpByQuestion.put(2L, List.of(100L));
        List<StatUserKp> kpRows = StatAggregator.kpRows(
                9L, 3L, latest, kpByQuestion, Map.of(100L, 2, 200L, 1),
                NOW, HALF_LIFE_DAYS, PRIOR);

        // kp 100 挂两道题（一对一错），kp 200 只挂答对的那道
        assertEquals(2, kpRows.size());
        StatUserKp kp100 = kpRows.stream().filter(r -> r.getKpId().equals(100L)).findFirst().orElseThrow();
        assertEquals(2, kp100.getAnsweredCount());
        assertEquals(1, kp100.getCorrectCount());
        StatUserKp kp200 = kpRows.stream().filter(r -> r.getKpId().equals(200L)).findFirst().orElseThrow();
        assertEquals(1, kp200.getAnsweredCount());
        assertEquals(1, kp200.getCorrectCount());
    }

    private static StatAggregator.Answer answer(Long questionId, boolean correct, LocalDateTime at) {
        return new StatAggregator.Answer(questionId, 7L, correct, at);
    }

    private static StatAnswerRow row(Long questionId, Long outlineId, Long recordId,
                                     boolean correct, LocalDateTime at) {
        StatAnswerRow row = new StatAnswerRow();
        row.setRecordId(recordId);
        row.setQuestionId(questionId);
        row.setOutlineId(outlineId);
        row.setCorrect(correct);
        row.setAnsweredAt(at);
        return row;
    }
}
