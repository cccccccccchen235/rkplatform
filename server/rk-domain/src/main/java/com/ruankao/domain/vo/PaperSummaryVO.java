package com.ruankao.domain.vo;

import java.util.List;

/**
 * 卷面结构摘要（05 §5.2 {@code SubjectVO.paperSummary}），三层：模板 → 场次 → 分节。
 *
 * <p>{@code totalDuration} 为各场次之和，不是"一场考试的总时长"——高级卷面含两个互不衔接的
 * 场次（连考 240 分钟 + 论文 120 分钟，中间隔午休），所以它表达的是"上机总投入时间"。
 *
 * <p>只暴露作答相关字段：不下发 {@code perQuestionScore}、{@code durationMin/Max}、
 * {@code allowEarlySubmit} 等判分与计时细节，那些是考试进行中的服务端状态，
 * 提前下发等于把判分口径泄漏给客户端。
 */
public record PaperSummaryVO(Integer totalDuration,
                             Integer totalMaxScore,
                             Integer autoScoredMaxScore,
                             List<Sitting> sittings) {

    /**
     * 场次。
     *
     * @param sittingNo         场次序号 1/2
     * @param totalDuration     本场次总时长（分钟）；场次内分节共享
     * @param autoScoredMaxScore 本场次可自动判分满分（仅客观题节）
     */
    public record Sitting(Integer sittingNo,
                          String sittingName,
                          Integer totalDuration,
                          Integer totalMaxScore,
                          Integer autoScoredMaxScore,
                          List<Section> sections) {
    }

    /**
     * 分节。
     *
     * @param supportStatus SUPPORTED / NOT_SUPPORTED；后者表示该分节在本平台不可作答
     *                      （如信息处理技术员的 Office 操作题），前端须置灰而非隐藏，
     *                      让考生知道"这一节存在，只是本平台不做"。
     */
    public record Section(Integer sectionNo,
                          String sectionName,
                          String sectionType,
                          Integer questionCount,
                          String supportStatus) {
    }
}
