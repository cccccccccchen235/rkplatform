package com.ruankao.domain.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 错题本列表项（05 §5.7 第 26 条 {@code GET /wrong-questions} 的分页元素）。
 *
 * <p>字段与 05 §5.7 契约逐字对应，<b>多一个少一个都算契约变更</b>。
 *
 * <p>{@code stemSummary} 是<b>截断后的题干摘要</b>，不是完整 {@code stem}：列表一行只展示
 * 一句话，题干里可能是 MEDIUMTEXT 级的 Markdown + KaTeX，整段取出既撑大响应，
 * 又要前端自己截断。截断在 SQL 里用 {@code LEFT(q.stem, 120)} 一次完成（见 XML），
 * 服务端不再二次处理，前端也不负责。
 *
 * <p>这里的 {@code questionId} / {@code outlineId} 是 {@code Long}，而引用它们的
 * Controller 层 VO 会按 05 §5.1「ID 类型」把它们转成字符串下发——主键是
 * {@code BIGINT UNSIGNED}，超过 2^53 前端 JS 会静默丢精度，这道转换留在边界层做，
 * 持久层保持数值原样。
 *
 * <p>用 Lombok 可变类而非 record：本对象由 MyBatis 按列名逐字段回填，
 * 无参构造 + setter 是最直接的映射路径（同 rk-domain 的实体风格）。
 */
@Getter
@Setter
@NoArgsConstructor
public class WrongQuestionRowVO {

    /** 题目 ID，对应 {@code user_wrong_question.question_id}。 */
    private Long questionId;

    /** 叶子章节 ID，取自冗余列 {@code rk_question.outline_id}，组件路径树时用。 */
    private Long outlineId;

    /** 题干摘要（{@code LEFT(stem, 120)}），非完整题面。 */
    private String stemSummary;

    /** 累计答错次数；答错即 +1。 */
    private Integer wrongCount;

    /** 累计答对次数；答对即 +1（与 {@code consecutiveCorrect} 不是一回事）。 */
    private Integer correctCount;

    /** 连续答对计数；达阈值（{@code wrongbook.master.threshold}，默认 2）置 MASTERED。 */
    private Integer consecutiveCorrect;

    /** 最近一次作答时间（答对或答错都会刷新），默认排序字段。 */
    private LocalDateTime lastAnsweredAt;

    /** ACTIVE / MASTERED / REMOVED，见 {@link com.ruankao.domain.enums.WrongQuestionStatus}。 */
    private String status;
}