package com.ruankao.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 错题本列表项（05 §5.7 第 26 条 {@code GET /wrong-questions} 的分页元素）。
 * 字段即契约，改动需同步 05 与本类。
 *
 * <p>与持久层投影 {@link WrongQuestionRowVO} 的区别：本类是<b>接口契约 VO</b>，
 * 所有 ID 为 {@code String}（05 §5.1：ID 以字符串下发，避免超过 2^53 的 JS 精度丢失），
 * 且把 {@code outlineId} 展开成 {@code outlinePath} 名称数组；持久层投影保持 {@code Long}
 * 与列原样，数值到字符串的转换在边界层完成。
 *
 * <p>{@code stemSummary} 是<b>截断后的题干</b>，不是完整题面（{@code LEFT(stem,120)} 口径）：
 * 列表一行只展示一句话，整段 MEDIUMTEXT 级的 Markdown + KaTeX 既撑大响应又要前端自截。
 *
 * <p>{@code lastAnsweredAt} 用 {@link LocalDateTime}：全局 Jackson 配置已按
 * {@code yyyy-MM-dd HH:mm:ss} 序列化，VO 不自行格式化。
 *
 * @param status ACTIVE/MASTERED/REMOVED，见
 *               {@link com.ruankao.domain.enums.WrongQuestionStatus}
 */
public record WrongQuestionVO(String questionId,
                              String stemSummary,
                              List<String> outlinePath,
                              Integer wrongCount,
                              Integer correctCount,
                              Integer consecutiveCorrect,
                              LocalDateTime lastAnsweredAt,
                              String status) {
}