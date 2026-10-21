package com.ruankao.domain.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统计重算的事实投影：用户在某科目下的一条**原始作答流水**。
 *
 * <p>刻意保留 {@code recordId}：去重"取最近一次"的并列裁决（同一秒内两次作答）要靠
 * {@code (answered_at, id)} 才能稳定，只比时间戳会让顺序不确定（06 §6.10.2 的窗口函数
 * 也是 {@code ORDER BY answered_at DESC, id DESC}）。
 *
 * <p>用可变类而非 {@code record}：MyBatis 自动映射走 setter，本项目未开启
 * {@code argNameBasedConstructorAutoMapping}（见 {@link QuestionKpRowVO} 的说明）。
 */
@Getter
@Setter
@NoArgsConstructor
public class StatAnswerRow {

    /** 流水主键，用于同一时间戳下的稳定裁决。 */
    private Long recordId;

    private Long questionId;

    /** 冗余叶子章节（{@code user_answer_record.outline_id}）。 */
    private Long outlineId;

    /** 是否答对；重算只取客观题（{@code is_correct IS NOT NULL}），故恒非空。 */
    private Boolean correct;

    private LocalDateTime answeredAt;
}
