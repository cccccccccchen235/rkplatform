package com.ruankao.service.stat;

import com.ruankao.domain.entity.StatUserKp;
import com.ruankao.domain.entity.StatUserOutline;
import com.ruankao.domain.vo.StatAnswerRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 掌握度与统计口径的**纯函数**实现（06 §6.10.1、§6.10.2）。
 *
 * <p>刻意不依赖 Spring / MyBatis：这里是全项目唯一一处"数值对不上时最难查"的逻辑——
 * 它横跨时间衰减、覆盖率、先验三层加权，任何一个指数写反都不会崩，
 * 只会让掌握度悄悄偏一点。把它做成无副作用的静态函数，才能用单测把
 * 06 §6.10.1 的四种场景逐个钉死。
 *
 * <p>两个口径容易混淆，这里写明：
 * <ul>
 *   <li><b>去重口径（最近一次作答）</b>：同一题重复作答只保留最后一次。它是
 *       {@code accuracy} 与 {@code coverage} 的分子的共同来源；一题先对后错必须算错
 *       （否"先对后错"会被算成对，见 06 §6.10.2 的评审 B5）。</li>
 *   <li><b>事件口径</b>：不做去重，用于 {@code stat_daily} 的"那天做了多少题"。
 *       那条路径不经过本类。</li>
 * </ul>
 */
public final class StatAggregator {

    /** 与 {@code stat_user_outline}/{@code stat_user_kp} 的 {@code DECIMAL(5,4)} 对齐。 */
    private static final int SCALE = 4;

    private StatAggregator() {
    }

    /** 去重后的一道题的"最近一次作答"。 */
    public record Answer(Long questionId, Long outlineId, boolean correct, LocalDateTime answeredAt) {
    }

    /** 一个聚合维度的两个结果列。 */
    public record Metrics(BigDecimal accuracy, BigDecimal mastery) {
    }

    /**
     * 每题只保留最近一次作答（06 §6.10.2 的去重口径）。
     *
     * <p>裁决顺序是 {@code (answeredAt, recordId)}：同一秒内的两次作答靠流水主键定胜负。
     * 依赖"输入已按某顺序排好"会让去重正确性隐形地绑在 SQL 的 ORDER BY 上，
     * 所以这里自己做比较，输入顺序无关。
     */
    public static List<Answer> latestPerQuestion(List<StatAnswerRow> rows) {
        Map<Long, StatAnswerRow> latest = new LinkedHashMap<>();
        for (StatAnswerRow row : rows) {
            latest.merge(row.getQuestionId(), row, StatAggregator::later);
        }
        List<Answer> result = new ArrayList<>(latest.size());
        for (StatAnswerRow row : latest.values()) {
            result.add(new Answer(row.getQuestionId(), row.getOutlineId(),
                    Boolean.TRUE.equals(row.getCorrect()), row.getAnsweredAt()));
        }
        return result;
    }

    /** 取两次作答里更晚的一次；时间相同再比流水主键。 */
    private static StatAnswerRow later(StatAnswerRow a, StatAnswerRow b) {
        int byTime = a.getAnsweredAt().compareTo(b.getAnsweredAt());
        if (byTime != 0) {
            return byTime > 0 ? a : b;
        }
        return a.getRecordId() >= b.getRecordId() ? a : b;
    }

    /**
     * 06 §6.10.1 的公式：
     * {@code mastery = coverage × recency + (1 - coverage) × prior}。
     *
     * @param answers        某维度下**已去重**的作答（{@code a = answers.size()}）
     * @param publishedTotal 该维度的已发布题量 {@code n}；&le;0 时视覆盖率为 1（有作答）
     *                       或 0（无作答），避免除零
     * @param halfLifeDays   时间衰减半衰期 {@code H}，来自 {@code mastery.half.life}
     * @param prior          先验 {@code prior}，来自 {@code mastery.prior}
     */
    public static Metrics metrics(Collection<Answer> answers,
                                  int publishedTotal,
                                  LocalDateTime now,
                                  double halfLifeDays,
                                  double prior) {
        int answered = answers.size();
        int correct = 0;
        double weightedSum = 0.0;
        double weightTotal = 0.0;
        double halfLife = halfLifeDays > 0 ? halfLifeDays : 1.0;

        for (Answer answer : answers) {
            if (answer.correct()) {
                correct++;
            }
            double days = Duration.between(answer.answeredAt(), now).toMillis() / 86_400_000.0;
            if (days < 0) {
                days = 0; // 未来时间戳（时钟漂移）按"刚刚作答"处理，不让它权重超过 1
            }
            double weight = Math.pow(0.5, days / halfLife);
            weightTotal += weight;
            if (answer.correct()) {
                weightedSum += weight;
            }
        }

        BigDecimal accuracy = answered == 0
                ? BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(correct)
                        .divide(BigDecimal.valueOf(answered), SCALE, RoundingMode.HALF_UP);

        double coverage;
        if (publishedTotal > 0) {
            coverage = Math.min(1.0, answered / (double) publishedTotal);
        } else {
            coverage = answered > 0 ? 1.0 : 0.0;
        }
        double recency = weightTotal > 0 ? weightedSum / weightTotal : prior;
        double mastery = coverage * recency + (1 - coverage) * prior;

        return new Metrics(accuracy, round4(mastery));
    }

    /**
     * 章节维度统计行：{@code (user, subject, outline)} 整体重算的结果。
     *
     * <p>只为"有作答的章节"出行；没做过的章节由读取端用先验 0.5 兜底，
     * 这样表的大小与用户实际练过的范围成正比，而不是"用户数 × 章节数"。
     */
    public static List<StatUserOutline> outlineRows(Long userId,
                                                    Long subjectId,
                                                    List<Answer> latest,
                                                    Map<Long, Integer> publishedCounts,
                                                    LocalDateTime now,
                                                    double halfLifeDays,
                                                    double prior) {
        Map<Long, List<Answer>> byOutline = new LinkedHashMap<>();
        for (Answer answer : latest) {
            byOutline.computeIfAbsent(answer.outlineId(), k -> new ArrayList<>()).add(answer);
        }

        List<StatUserOutline> rows = new ArrayList<>(byOutline.size());
        for (Map.Entry<Long, List<Answer>> entry : byOutline.entrySet()) {
            List<Answer> group = entry.getValue();
            Metrics metrics = metrics(group, publishedCounts.getOrDefault(entry.getKey(), 0),
                    now, halfLifeDays, prior);

            StatUserOutline row = new StatUserOutline();
            row.setUserId(userId);
            row.setSubjectId(subjectId);
            row.setOutlineId(entry.getKey());
            row.setAnsweredCount(group.size());
            row.setCorrectCount((int) group.stream().filter(Answer::correct).count());
            row.setAccuracy(metrics.accuracy());
            row.setMastery(metrics.mastery());
            row.setLastAnsweredAt(group.stream()
                    .map(Answer::answeredAt)
                    .max(Comparator.naturalOrder())
                    .orElse(null));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 知识点维度统计行。一道题的最近一次结果会分发给它挂的**每一个**标签——
     * 这与 {@code onlyUnmastered} 的"任一标签已掌握就排除整题"口径配套：
     * 标签的已掌握与否必须由挂在它下面的题来回答，不能因一题多标签而漏算。
     *
     * @param kpByQuestion 题目 → 知识点集合
     */
    public static List<StatUserKp> kpRows(Long userId,
                                          Long subjectId,
                                          List<Answer> latest,
                                          Map<Long, List<Long>> kpByQuestion,
                                          Map<Long, Integer> publishedCounts,
                                          LocalDateTime now,
                                          double halfLifeDays,
                                          double prior) {
        Map<Long, List<Answer>> byKp = new LinkedHashMap<>();
        for (Answer answer : latest) {
            for (Long kpId : kpByQuestion.getOrDefault(answer.questionId(), List.of())) {
                byKp.computeIfAbsent(kpId, k -> new ArrayList<>()).add(answer);
            }
        }

        List<StatUserKp> rows = new ArrayList<>(byKp.size());
        for (Map.Entry<Long, List<Answer>> entry : byKp.entrySet()) {
            List<Answer> group = entry.getValue();
            Metrics metrics = metrics(group, publishedCounts.getOrDefault(entry.getKey(), 0),
                    now, halfLifeDays, prior);

            StatUserKp row = new StatUserKp();
            row.setUserId(userId);
            row.setSubjectId(subjectId);
            row.setKpId(entry.getKey());
            row.setAnsweredCount(group.size());
            row.setCorrectCount((int) group.stream().filter(Answer::correct).count());
            row.setAccuracy(metrics.accuracy());
            row.setMastery(metrics.mastery());
            rows.add(row);
        }
        return rows;
    }

    /**
     * 科目掌握度（06 §6.10.1）：{@code Σ(章节 mastery × 章节题量) / Σ章节题量}。
     *
     * <p>权重分母是**全部**有已发布题的章节，缺失的（没练过的）章节用先验补：
     * 只按练过的章节加权，会让"只做了一章"的用户拿到接近满分的科目掌握度，
     * 而这正是覆盖率要防的事。
     *
     * @param outlineMastery 已算出的章节掌握度；没有行的章节按先验处理
     * @param outlineCounts  章节 → 已发布题量
     */
    public static BigDecimal subjectMastery(Map<Long, BigDecimal> outlineMastery,
                                            Map<Long, Integer> outlineCounts,
                                            double prior) {
        double weighted = 0.0;
        long weightTotal = 0;
        for (Map.Entry<Long, Integer> entry : outlineCounts.entrySet()) {
            int count = entry.getValue() == null ? 0 : entry.getValue();
            if (count <= 0) {
                continue;
            }
            BigDecimal mastery = outlineMastery.getOrDefault(entry.getKey(), round4(prior));
            weighted += mastery.doubleValue() * count;
            weightTotal += count;
        }
        if (weightTotal == 0) {
            return round4(prior);
        }
        return round4(weighted / weightTotal);
    }

    private static BigDecimal round4(double value) {
        return BigDecimal.valueOf(value).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
