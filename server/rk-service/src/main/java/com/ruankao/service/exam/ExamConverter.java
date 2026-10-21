package com.ruankao.service.exam;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruankao.domain.entity.ExamSessionItem;
import com.ruankao.domain.entity.ExamSessionSection;
import com.ruankao.domain.entity.ExamSessionSitting;
import com.ruankao.domain.entity.Question;
import com.ruankao.domain.enums.SectionStatus;
import com.ruankao.domain.enums.SittingStatus;
import com.ruankao.domain.vo.ExamItemVO;
import com.ruankao.domain.vo.ExamResultSectionVO;
import com.ruankao.domain.vo.ExamResultSittingVO;
import com.ruankao.domain.vo.ExamSectionVO;
import com.ruankao.domain.vo.ExamSittingVO;
import com.ruankao.domain.vo.QuestionDetailVO;
import com.ruankao.domain.vo.QuestionImageVO;
import com.ruankao.domain.vo.QuestionOptionVO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 模拟考实体 → VO（纯函数，无数据库访问）。
 *
 * <p>两处派生而非存储的字段，集中在这里：
 * <ul>
 *   <li>场次的 {@code totalMaxScore}/{@code autoScoredMaxScore}：会话场次表不存这两个值，
 *       由本场次分节的 {@code max_score} 求和得到（客观题节另计）。</li>
 *   <li>分节的 {@code usedSec}：进行中按 {@code started_at} 现算，其余用落库值。</li>
 * </ul>
 */
public final class ExamConverter {

    private ExamConverter() {
    }

    public static ExamSectionVO toSectionVO(ExamSessionSection section, LocalDateTime now) {
        int usedSec = ExamTiming.usedSec(section.getStartedAt(), now);
        if (section.getStatus() == null || !SectionStatus.IN_PROGRESS.name().equals(section.getStatus())) {
            usedSec = section.getUsedSec() == null ? 0 : section.getUsedSec();
        }
        return new ExamSectionVO(
                section.getSittingNo(),
                section.getSectionNo(),
                section.getSectionName(),
                section.getSectionType(),
                section.getQuestionCount(),
                section.getMaxScore(),
                perQuestionScore(section),
                section.getDurationMin(),
                section.getDurationMax(),
                section.getAllowEarlySubmit(),
                usedSec,
                section.getSupportStatus(),
                section.getStatus());
    }

    /** 每题分值 = maxScore / questionCount；题量为 0（NOT_SUPPORTED）时为 0。 */
    private static BigDecimal perQuestionScore(ExamSessionSection section) {
        int count = section.getQuestionCount() == null ? 0 : section.getQuestionCount();
        BigDecimal max = section.getMaxScore() == null ? BigDecimal.ZERO : section.getMaxScore();
        if (count <= 0) {
            return BigDecimal.ZERO;
        }
        return max.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    public static ExamSittingVO toSittingVO(ExamSessionSitting sitting,
                                            List<ExamSessionSection> sections,
                                            LocalDateTime now) {
        int total = sitting.getTotalDurationSec() == null ? 0 : sitting.getTotalDurationSec();
        String status = sitting.getStatus() == null ? SittingStatus.PENDING.name() : sitting.getStatus();
        int remaining;
        if (SittingStatus.IN_PROGRESS.name().equals(status)) {
            remaining = ExamTiming.remainingSec(sitting.getStartedAt(), total, now);
        } else if (SittingStatus.FINISHED.name().equals(status)) {
            remaining = 0;
        } else {
            remaining = total;
        }

        int totalMax = 0;
        int autoScoredMax = 0;
        List<ExamSectionVO> sectionVos = new ArrayList<>(sections.size());
        for (ExamSessionSection section : sections) {
            sectionVos.add(toSectionVO(section, now));
            int score = section.getMaxScore() == null ? 0 : section.getMaxScore().intValue();
            totalMax += score;
            if ("OBJECTIVE".equals(section.getSectionType())) {
                autoScoredMax += score;
            }
        }

        return new ExamSittingVO(
                sitting.getSittingNo(),
                sitting.getSittingName(),
                total,
                remaining,
                status,
                sitting.getCurrentSectionNo(),
                totalMax,
                autoScoredMax,
                sitting.getObjectiveScore(),
                sitting.getStartedAt(),
                sitting.getFinishedAt(),
                sectionVos);
    }

    public static ExamItemVO toItemVO(ExamSessionItem item) {
        return new ExamItemVO(
                String.valueOf(item.getId()),
                String.valueOf(item.getQuestionId()),
                item.getSequence(),
                item.getSectionNo(),
                item.getQuestionType(),
                item.getStemSnapshot(),
                // 题干插图没有快照列（04 §4.7 的 exam_session_item 无 images 列），
                // 当前题库也没有带图题；本轮返回空数组并登记该缺口（docs/30）。
                List.<QuestionImageVO>of(),
                parseOptions(item.getOptionsSnapshot()),
                item.getScore(),
                item.getUserAnswer(),
                item.getAnsweredAt(),
                item.getDurationMs());
    }

    /** 选项快照 JSON → 选项列表；{@code null}/空串/脏 JSON 一律返回空列表。 */
    public static List<QuestionOptionVO> parseOptions(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JSONArray array = JSONUtil.parseArray(json);
            List<QuestionOptionVO> options = new ArrayList<>(array.size());
            for (Object element : array) {
                JSONObject object = (JSONObject) element;
                options.add(new QuestionOptionVO(object.getStr("key"), object.getStr("content")));
            }
            return options;
        } catch (Exception ex) {
            return List.of();
        }
    }

    /**
     * 成绩回顾的一道题：**题干/选项/答案/解析取快照**（保证题目被改动后仍可复现），
     * 只有元数据（难度、评分要点、章节）来自活题，取不到时留空。
     */
    public static QuestionDetailVO toResultItem(ExamSessionItem item, Question live) {
        return new QuestionDetailVO(
                String.valueOf(item.getQuestionId()),
                live == null || live.getSubjectId() == null ? null : String.valueOf(live.getSubjectId()),
                live == null || live.getOutlineId() == null ? null : String.valueOf(live.getOutlineId()),
                List.of(),
                item.getQuestionType(),
                item.getStemSnapshot(),
                List.<QuestionImageVO>of(),
                live == null ? null : live.getDifficulty(),
                parseOptions(item.getOptionsSnapshot()),
                List.of(),
                false,
                item.getCorrectAnswer(),
                item.getReferenceSnapshot(),
                live == null ? null : parseScorePoints(live.getScorePoints()),
                item.getAnalysisSnapshot(),
                item.getIsCorrect(),
                item.getUserAnswer());
    }

    private static List<String> parseScorePoints(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JSONArray array = JSONUtil.parseArray(json);
            List<String> points = new ArrayList<>(array.size());
            for (Object element : array) {
                points.add(String.valueOf(element));
            }
            return points;
        } catch (Exception ex) {
            return null;
        }
    }

    public static ExamResultSectionVO toResultSection(ExamSessionSection section) {
        boolean scored = "OBJECTIVE".equals(section.getSectionType())
                && "SUPPORTED".equals(section.getSupportStatus());
        return new ExamResultSectionVO(
                section.getSittingNo(),
                section.getSectionNo(),
                section.getSectionName(),
                section.getSectionType(),
                section.getMaxScore(),
                section.getObjectiveScore(),
                section.getSupportStatus(),
                scored);
    }

    public static ExamResultSittingVO toResultSitting(ExamSessionSitting sitting,
                                                      List<ExamSessionSection> sections) {
        int totalMax = 0;
        int autoScoredMax = 0;
        int usedSec = 0;
        boolean answered = sitting.getStartedAt() != null;
        for (ExamSessionSection section : sections) {
            int score = section.getMaxScore() == null ? 0 : section.getMaxScore().intValue();
            totalMax += score;
            if ("OBJECTIVE".equals(section.getSectionType())) {
                autoScoredMax += score;
            }
            usedSec += section.getUsedSec() == null ? 0 : section.getUsedSec();
        }
        return new ExamResultSittingVO(
                sitting.getSittingNo(),
                sitting.getSittingName(),
                sitting.getTotalDurationSec(),
                usedSec,
                sitting.getObjectiveScore(),
                autoScoredMax,
                totalMax,
                sitting.getStatus(),
                answered);
    }
}
