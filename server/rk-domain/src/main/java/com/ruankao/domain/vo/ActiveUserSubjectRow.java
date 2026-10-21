package com.ruankao.domain.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 夜间统计全量重算的待办投影：一个"活跃用户 × 科目"组合（06 §6.13）。
 *
 * <p>粒度是 (user, subject) 而不是 user：重算的写入单位本来就是
 * {@code (user_id, subject_id)}，拆到 for 循环里再展开一次科目只会多一次查询。
 */
@Getter
@Setter
@NoArgsConstructor
public class ActiveUserSubjectRow {

    private Long userId;

    private Long subjectId;
}
