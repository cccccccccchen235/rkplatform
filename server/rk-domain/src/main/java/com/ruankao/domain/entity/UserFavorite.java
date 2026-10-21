package com.ruankao.domain.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 题目收藏（{@code user_favorite}）。
 *
 * <p>{@code uk_user_question(user_id, question_id)} 保证不重复收藏；
 * {@code idx_user_subject(user_id, subject_id, created_at)} 支撑"按科目列出收藏、按时间倒序"，
 * 即收藏练习（{@link com.ruankao.domain.enums.PracticeMode#FAVORITE}）的取题入口（04 §4.3）。
 */
@Getter
@Setter
@NoArgsConstructor
public class UserFavorite {

    private Long id;

    private Long userId;

    private Long questionId;

    private Long subjectId;

    private LocalDateTime createdAt;
}