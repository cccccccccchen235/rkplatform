package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 练习进度（{@code user_practice_progress}），服务于顺序练习的"继续上次练习"（02 §2.5.4）。
 *
 * <p>{@code outline_id} 用 {@code NOT NULL DEFAULT 0} 而非 {@code NULL}，是为了让唯一索引
 * {@code uk_user_subject_type_ref(user_id, subject_id, practice_type, outline_id)} 真正生效：
 * <b>MySQL 唯一索引允许多个 {@code NULL}</b>，若该列为 NULL，同一用户同一练习类型会产生
 * 多条进度记录（04 §4.3 已注明）。因此本字段的 {@code 0} 有确切语义——"{@code 0} 表示不按章节"，
 * 不要把 {@code 0} 当作"未知/空"处理。
 */
@Getter
@Setter
@NoArgsConstructor
public class UserPracticeProgress {

    private Long id;

    private Long userId;

    private Long subjectId;

    /**
     * 练习类型，04 §4.4 限定为 CHAPTER / KP / SEQUENCE / FAVORITE。
     *
     * <p>注意这是 {@link com.ruankao.domain.enums.PracticeMode} 的<b>子集</b>：
     * {@code WRONG_REDO} 与 {@code SAMPLE} 不记进度游标，永远不会出现在本列。
     */
    private String practiceType;

    /** {@code 0} 表示不按章节（唯一索引需要非 NULL）；其余为叶子章节 ID。 */
    private Long outlineId;

    /** 顺序练习游标（下次从 {@code id > last_sequence} 开始取题）。 */
    private Integer lastSequence;

    private Long lastQuestionId;

    private Integer answeredCount;

    private Integer correctCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}