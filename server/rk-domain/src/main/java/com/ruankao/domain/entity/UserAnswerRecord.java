package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 答题记录（{@code user_answer_record}），本库最大的表，也是<b>唯一的事实流水</b>：
 * 所有统计口径都由本表派生，{@code stat_*} 表只是它的聚合缓存（04 §4.3、06 §6.10）。
 *
 * <p>写入前注意两条业务约束：
 * <ul>
 *   <li>{@code outline_id} 是<b>冗余的叶子章节</b>，为的是按章节聚合统计时免回表
 *       {@code rk_question}（04 §4.3），写入时必须与题目当前归属一致；</li>
 *   <li>{@code duration_ms} 单题超过 10 分钟按 10 分钟截断，防挂机拉高均值（04 §4.3）。</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class UserAnswerRecord {

    private Long id;

    private Long userId;

    private Long subjectId;

    /** 冗余叶子章节；按章节聚合统计时免回表。 */
    private Long outlineId;

    private Long questionId;

    /** 来源，取值见 {@link com.ruankao.domain.enums.PracticeSource}（04 §4.3）。 */
    private String source;

    /** 仅 {@code source = EXAM} 时有值，对应 {@code exam_session.id}。 */
    private Long sessionId;

    /** 客观题为选项 key；主观题为空（不判分）。 */
    private String userAnswer;

    /** <b>主观题固定为 {@code NULL}</b>（04 §4.3）：SUBJECTIVE/ESSAY 不判分，故没有对错可言。 */
    private Boolean isCorrect;

    /** 单题作答耗时（毫秒），>10 分钟按 10 分钟截断。 */
    private Integer durationMs;

    private LocalDateTime answeredAt;

    private LocalDateTime createdAt;
}