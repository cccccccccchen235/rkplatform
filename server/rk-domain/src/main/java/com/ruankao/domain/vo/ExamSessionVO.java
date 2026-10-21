package com.ruankao.domain.vo;

import java.util.List;

/**
 * 模拟考会话详情（05 §5.6 第 16/18 条）。断点续答靠它恢复全部已答内容与剩余时间。
 *
 * <p>{@code remainingSec}/{@code currentSectionNo} 是**当前场次**的值；真值以
 * {@code sittings[]} 为准（05 §5.6 的字段说明）。
 */
public record ExamSessionVO(
        String sessionId,
        String sessionNo,
        String subjectId,
        String subjectName,
        String status,
        Integer currentSittingNo,
        Integer totalDurationSec,
        Integer remainingSec,
        Integer currentSectionNo,
        Integer questionCount,
        Integer autoScoredMaxScore,
        Integer totalMaxScore,
        List<ExamSittingVO> sittings,
        List<ExamSectionVO> sections,
        List<ExamItemVO> items) {
}
